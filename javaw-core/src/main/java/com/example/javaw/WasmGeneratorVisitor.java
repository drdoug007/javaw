package com.example.javaw;

import com.example.javaw.js.JsAnnotationHelper;
import com.example.javaw.sourcemap.SourceMapTracker;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.*;

/**
 * A visitor that translates Java AST elements into valid WebAssembly Text format (WAT),
 * supporting full object-oriented programming, dynamic heap allocation, inheritance,
 * field accesses, and virtual method dispatch via vtables and call_indirect.
 */
public class WasmGeneratorVisitor extends VoidVisitorAdapter<StringBuilder> {
    private final Map<String, StringConstant> stringPool;
    private final Map<String, GlobalVar> globalVars;
    private final ClassRegistry classRegistry;
    private final SourceMapTracker sourceMapTracker;
    private final Map<String, com.example.javaw.js.JsSnippetModel> jsSnippets;
    private final Deque<LoopContext> loopStack = new ArrayDeque<>();
    private final Deque<String> switchEnumStack = new ArrayDeque<>();
    private final Map<String, Integer> methodCounts = new HashMap<>();
    private final Map<String, Integer> constructorCounts = new HashMap<>();
    private final Map<MethodCallExpr, String> receiverLocals = new IdentityHashMap<>();
    private CallableDeclaration<?> currentCallable;
    private ClassModel currentClass;
    private RecordModel currentRecord;
    private int loopCounter = 0;
    private int forEachCounter = 0;
    private int switchCounter = 0;
    private int switchExprCounter = 0;

    public WasmGeneratorVisitor(Map<String, StringConstant> stringPool, Map<String, GlobalVar> globalVars, ClassRegistry classRegistry, SourceMapTracker sourceMapTracker, Map<String, com.example.javaw.js.JsSnippetModel> jsSnippets) {
        this.stringPool = stringPool;
        this.globalVars = globalVars;
        this.classRegistry = classRegistry;
        this.sourceMapTracker = sourceMapTracker;
        this.jsSnippets = jsSnippets != null ? jsSnippets : Collections.emptyMap();
    }

    public WasmGeneratorVisitor(Map<String, StringConstant> stringPool, Map<String, GlobalVar> globalVars, ClassRegistry classRegistry, SourceMapTracker sourceMapTracker) {
        this(stringPool, globalVars, classRegistry, sourceMapTracker, Collections.emptyMap());
    }

    public WasmGeneratorVisitor(Map<String, StringConstant> stringPool, Map<String, GlobalVar> globalVars, ClassRegistry classRegistry) {
        this(stringPool, globalVars, classRegistry, null, Collections.emptyMap());
    }

    public WasmGeneratorVisitor(Map<String, StringConstant> stringPool, Map<String, GlobalVar> globalVars) {
        this(stringPool, globalVars, null, null, Collections.emptyMap());
    }

    @Override
    public void visit(CompilationUnit n, StringBuilder collector) {
        super.visit(n, collector);
        emitDefaultRootVirtualMethods(collector);
        emitClassInitializersAndHelpers(n, collector);
        emitEnumInitializersAndHelpers(n, collector);
    }

    @Override
    public void visit(EnumDeclaration n, StringBuilder collector) {
        String enumName = n.getNameAsString();
        currentClass = classRegistry != null ? classRegistry.getClass(enumName) : null;
        methodCounts.clear();

        if (sourceMapTracker != null) {
            sourceMapTracker.track(collector, n, enumName);
        }

        collector.append("  ;; Enum: ").append(enumName).append("\n");

        if (n.getConstructors().isEmpty()) {
            collector.append("  (func $").append(enumName).append("__init (export \"")
                    .append(enumName).append("__init\") (param $this i32)\n");
            collector.append("  )\n");
        }

        for (ConstructorDeclaration cd : n.getConstructors()) {
            cd.accept(this, collector);
        }

        for (MethodDeclaration md : n.getMethods()) {
            md.accept(this, collector);
        }

        currentClass = null;
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, StringBuilder collector) {
        String className = n.getNameAsString();
        currentClass = classRegistry != null ? classRegistry.getClass(className) : null;
        methodCounts.clear();
        constructorCounts.clear();

        if (sourceMapTracker != null) {
            sourceMapTracker.track(collector, n, className);
        }

        collector.append("  ;; Class/Interface: ").append(className).append("\n");

        // If class is an OOP class and has no explicit constructor, emit default constructor
        if (currentClass != null && currentClass.isOopClass() && n.getConstructors().isEmpty() && !n.isInterface()) {
            collector.append("  (func $").append(className).append("__init (export \"")
                    .append(className).append("__init\") (param $this i32)\n");
            if (currentClass.getSuperClassName() != null && classRegistry != null && classRegistry.hasClass(currentClass.getSuperClassName())) {
                collector.append("    local.get $this\n");
                collector.append("    call $").append(currentClass.getSuperClassName()).append("__init\n");
            }
            collector.append("  )\n");
        }

        super.visit(n, collector);
    }

    @Override
    public void visit(ConstructorDeclaration n, StringBuilder collector) {
        currentCallable = n;
        String className = n.getNameAsString();
        Map<String, String> localTypes = new HashMap<>();
        Map<String, String> localElementTypes = new HashMap<>();
        Map<String, String> localJavaTypes = new HashMap<>();
        forEachCounter = 0;
        switchCounter = 0;
        switchExprCounter = 0;

        int count = constructorCounts.getOrDefault(className, 0);
        constructorCounts.put(className, count + 1);
        String funcName = count == 0 ? "$" + className + "__init" : "$" + className + "__init_" + count;
        String exportName = count == 0 ? className + "__init" : className + "__init_" + count;

        if (sourceMapTracker != null) {
            sourceMapTracker.track(collector, n, exportName);
        }
        collector.append("  (func ").append(funcName)
                .append(" (export \"").append(exportName).append("\")")
                .append(" (param $this i32)");

        localTypes.put("this", "i32");
        localJavaTypes.put("this", className);

        // Process constructor parameters
        for (Parameter p : n.getParameters()) {
            String pType = TypeMapper.mapType(p.getType());
            if (pType == null) pType = "i32";
            localTypes.put(p.getNameAsString(), pType);
            localJavaTypes.put(p.getNameAsString(), p.getType().asString());
            if (p.getType().isArrayType()) {
                localElementTypes.put(p.getNameAsString(), TypeMapper.mapType(p.getType().asArrayType().getComponentType()));
            }
            collector.append(" (param $").append(p.getNameAsString()).append(" ").append(pType).append(")");
        }
        collector.append("\n");

        BlockStmt body = n.getBody();
        emitLocalDeclarationsAndHelpers(body, collector, localTypes, localElementTypes, localJavaTypes);

        // Check if explicit super(...) or this(...) exists; if not and superclass exists, invoke super constructor
        boolean hasExplicitCall = !body.getStatements().isEmpty() && body.getStatement(0).isExplicitConstructorInvocationStmt();
        if (!hasExplicitCall && currentClass != null && currentClass.getSuperClassName() != null
                && classRegistry != null && classRegistry.hasClass(currentClass.getSuperClassName())) {
            collector.append("    local.get $this\n");
            collector.append("    call $").append(currentClass.getSuperClassName()).append("__init\n");
        }

        for (Statement stmt : body.getStatements()) {
            translateStatement(stmt, collector, localTypes, localElementTypes, localJavaTypes, null);
        }

        collector.append("  )\n");
    }

    @Override
    public void visit(RecordDeclaration n, StringBuilder collector) {
        String recordName = n.getNameAsString();
        currentRecord = classRegistry != null ? classRegistry.getRecord(recordName) : null;

        if (sourceMapTracker != null) {
            sourceMapTracker.track(collector, n, recordName);
        }

        collector.append("  ;; Record: ").append(recordName).append("\n");

        for (MethodDeclaration md : n.getMethods()) {
            visitRecordMethod(md, collector, recordName);
        }

        currentRecord = null;
    }

    private void visitRecordMethod(MethodDeclaration n, StringBuilder collector, String recordName) {
        currentCallable = n;
        String methodName = n.getNameAsString();
        String returnWasmType = TypeMapper.mapType(n.getType());
        String returnTypeName = n.getType().asString();
        RecordModel returnRecord = classRegistry != null ? classRegistry.getRecord(returnTypeName) : null;

        Map<String, String> localTypes = new HashMap<>();
        Map<String, String> localElementTypes = new HashMap<>();
        Map<String, String> localJavaTypes = new HashMap<>();
        forEachCounter = 0;
        switchCounter = 0;
        switchExprCounter = 0;

        boolean isStatic = n.isStatic();
        String funcName = "$" + recordName + "_" + methodName;
        MethodModel declaration = currentRecord.getMethods().stream().filter(m -> m.getSignatureKey().equals(
                methodName + "(" + n.getParameters().stream().map(p -> p.getType().asString())
                        .collect(java.util.stream.Collectors.joining(",")) + ")")).findFirst().orElseThrow();
        funcName = declaration.getFunctionName();
        String exportName = funcName.substring(1);

        if (sourceMapTracker != null) {
            sourceMapTracker.track(collector, n, exportName);
        }

        collector.append("  (func ").append(funcName)
                .append(" (export \"").append(exportName).append("\")");

        if (!isStatic && currentRecord != null) {
            localJavaTypes.put("this", recordName);
            for (FieldModel c : currentRecord.getComponents()) {
                String thisParam = "this$" + c.getName();
                localTypes.put(thisParam, c.getWasmType());
                localJavaTypes.put(thisParam, c.getTypeName());
                collector.append(" (param $").append(thisParam).append(" ").append(c.getWasmType()).append(")");
            }
        }

        // Process parameters
        for (Parameter p : n.getParameters()) {
            String pTypeName = p.getType().asString();
            if (classRegistry != null && classRegistry.isRecord(pTypeName)) {
                RecordModel pRecord = classRegistry.getRecord(pTypeName);
                localJavaTypes.put(p.getNameAsString(), pTypeName);
                for (FieldModel c : pRecord.getComponents()) {
                    String paramLocal = p.getNameAsString() + "$" + c.getName();
                    localTypes.put(paramLocal, c.getWasmType());
                    localJavaTypes.put(paramLocal, c.getTypeName());
                    collector.append(" (param $").append(paramLocal).append(" ").append(c.getWasmType()).append(")");
                }
            } else {
                String pType = TypeMapper.mapType(p.getType());
                if (pType == null) pType = "i32";
                localTypes.put(p.getNameAsString(), pType);
                localJavaTypes.put(p.getNameAsString(), p.getType().asString());
                if (p.getType().isArrayType()) {
                    localElementTypes.put(p.getNameAsString(), TypeMapper.mapType(p.getType().asArrayType().getComponentType()));
                }
                collector.append(" (param $").append(p.getNameAsString()).append(" ").append(pType).append(")");
            }
        }

        // Return type
        if (returnRecord != null) {
            collector.append(" (result ").append(returnRecord.getWasmResultSignature()).append(")");
        } else if (returnWasmType != null) {
            collector.append(" (result ").append(returnWasmType).append(")");
        }
        collector.append("\n");

        if (n.getBody().isPresent()) {
            BlockStmt body = n.getBody().get();
            emitLocalDeclarationsAndHelpers(body, collector, localTypes, localElementTypes, localJavaTypes);

            for (Statement stmt : body.getStatements()) {
                translateStatement(stmt, collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
            }
            if (returnRecord != null) {
                if (body.getStatements().isEmpty() || !body.getStatements().get(body.getStatements().size() - 1).isReturnStmt()) {
                    for (FieldModel c : returnRecord.getComponents()) {
                        collector.append("    ").append(getWasmZeroConst(c.getWasmType())).append("\n");
                    }
                }
            } else if (returnWasmType != null) {
                if (body.getStatements().isEmpty() || !body.getStatements().get(body.getStatements().size() - 1).isReturnStmt()) {
                    collector.append("    ").append(getWasmZeroConst(returnWasmType)).append("\n");
                }
            }
        }

        collector.append("  )\n");
    }

    @Override
    public void visit(MethodDeclaration n, StringBuilder collector) {
        currentCallable = n;
        if (n.getParentNode().isPresent() && n.getParentNode().get() instanceof RecordDeclaration) {
            return;
        }
        ClassOrInterfaceDeclaration parentClassDecl = null;
        if (n.getParentNode().isPresent() && n.getParentNode().get() instanceof ClassOrInterfaceDeclaration) {
            parentClassDecl = (ClassOrInterfaceDeclaration) n.getParentNode().get();
        }
        String className = parentClassDecl != null ? parentClassDecl.getNameAsString() : (currentClass != null ? currentClass.getName() : "Global");
        ClassModel classModel = classRegistry != null ? classRegistry.getClass(className) : currentClass;
        currentClass = classModel;

        String methodName = n.getNameAsString();
        String baseKey = className + "_" + methodName;
        int count = methodCounts.getOrDefault(baseKey, 0);
        methodCounts.put(baseKey, count + 1);
        String importName = count == 0 ? "js_inline_" + baseKey : "js_inline_" + baseKey + "_" + count;

        Optional<AnnotationExpr> jsAnnoOpt = JsAnnotationHelper.getJsAnnotation(n);
        boolean isJsInline = jsAnnoOpt.isPresent() || (jsSnippets != null && jsSnippets.containsKey(importName));

        // Skip abstract or interface methods without bodies and without JS/native annotations
        if (!n.getBody().isPresent() && !isJsInline && !n.isNative()) {
            return;
        }

        String returnWasmType = TypeMapper.mapType(n.getType());
        String returnTypeName = n.getType().asString();
        RecordModel returnRecord = classRegistry != null ? classRegistry.getRecord(returnTypeName) : null;

        Map<String, String> localTypes = new HashMap<>();
        Map<String, String> localElementTypes = new HashMap<>();
        Map<String, String> localJavaTypes = new HashMap<>();
        forEachCounter = 0;
        switchCounter = 0;
        switchExprCounter = 0;

        boolean isStatic = n.isStatic();
        boolean isInstanceMethod = currentClass != null && !isStatic;
        String funcName;
        String exportName;

        if (isInstanceMethod && (currentClass.isOopClass() || n.isNative() || currentClass.isInterface() || n.getAnnotations().stream().anyMatch(a -> a.getNameAsString().contains("Import") || a.getNameAsString().contains("JS")))) {
            funcName = "$" + currentClass.getName() + "_" + methodName;
            exportName = currentClass.getName() + "_" + methodName;
        } else {
            funcName = "$" + methodName;
            exportName = methodName;
        }

        if (currentClass != null) {
            String key = methodName + "(" + n.getParameters().stream().map(p -> p.getType().asString())
                    .collect(java.util.stream.Collectors.joining(",")) + ")";
            MethodModel declaration = currentClass.getMethod(key);
            if (declaration != null) {
                funcName = declaration.getFunctionName();
                exportName = funcName.substring(1);
            }
        }

        if (sourceMapTracker != null) {
            sourceMapTracker.track(collector, n, exportName);
        }

        collector.append("  (func ").append(funcName)
                .append(" (export \"").append(exportName).append("\")");

        if (isInstanceMethod && (currentClass.isOopClass() || n.isNative() || currentClass.isInterface() || n.getAnnotations().stream().anyMatch(a -> a.getNameAsString().contains("Import") || a.getNameAsString().contains("JS")))) {
            collector.append(" (param $this i32)");
            localTypes.put("this", "i32");
            localJavaTypes.put("this", currentClass.getName());
        }

        // Process parameters
        for (Parameter p : n.getParameters()) {
            String pTypeName = p.getType().asString();
            if (classRegistry != null && classRegistry.isRecord(pTypeName)) {
                RecordModel pRecord = classRegistry.getRecord(pTypeName);
                localJavaTypes.put(p.getNameAsString(), pTypeName);
                for (FieldModel c : pRecord.getComponents()) {
                    String paramLocal = p.getNameAsString() + "$" + c.getName();
                    localTypes.put(paramLocal, c.getWasmType());
                    localJavaTypes.put(paramLocal, c.getTypeName());
                    collector.append(" (param $").append(paramLocal).append(" ").append(c.getWasmType()).append(")");
                }
            } else {
                String pType = TypeMapper.mapType(p.getType());
                if (pType == null) pType = "i32";
                localTypes.put(p.getNameAsString(), pType);
                localJavaTypes.put(p.getNameAsString(), p.getType().asString());
                if (p.getType().isArrayType()) {
                    localElementTypes.put(p.getNameAsString(), TypeMapper.mapType(p.getType().asArrayType().getComponentType()));
                }
                collector.append(" (param $").append(p.getNameAsString()).append(" ").append(pType).append(")");
            }
        }

        // Return type
        if (returnRecord != null) {
            collector.append(" (result ").append(returnRecord.getWasmResultSignature()).append(")");
        } else if (returnWasmType != null) {
            collector.append(" (result ").append(returnWasmType).append(")");
        }
        collector.append("\n");

        // Body
        if (isJsInline) {
            if (!isStatic && currentClass != null) {
                collector.append("    local.get $this\n");
            }
            for (Parameter p : n.getParameters()) {
                String pName = p.getNameAsString();
                String pType = p.getType().asString();
                if ("String".equals(pType) || "java.lang.String".equals(pType)) {
                    collector.append("    local.get $").append(pName).append("\n");
                    collector.append("    local.get $").append(pName).append("\n");
                    collector.append("    call $__str_length\n");
                } else {
                    collector.append("    local.get $").append(pName).append("\n");
                }
            }
            collector.append("    call $dom_").append(importName).append("\n");
        } else if (n.getBody().isPresent()) {
            BlockStmt body = n.getBody().get();
            emitLocalDeclarationsAndHelpers(body, collector, localTypes, localElementTypes, localJavaTypes);

            for (Statement stmt : body.getStatements()) {
                translateStatement(stmt, collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
            }
            if (returnRecord != null) {
                if (body.getStatements().isEmpty() || !body.getStatements().get(body.getStatements().size() - 1).isReturnStmt()) {
                    for (FieldModel c : returnRecord.getComponents()) {
                        collector.append("    ").append(getWasmZeroConst(c.getWasmType())).append("\n");
                    }
                }
            } else if (returnWasmType != null) {
                if (body.getStatements().isEmpty() || !body.getStatements().get(body.getStatements().size() - 1).isReturnStmt()) {
                    if ("f64".equals(returnWasmType)) {
                        collector.append("    f64.const 0\n");
                    } else if ("i64".equals(returnWasmType)) {
                        collector.append("    i64.const 0\n");
                    } else {
                        collector.append("    i32.const 0\n");
                    }
                }
            }
        }

        collector.append("  )\n");
    }

    private void emitLocalDeclarationsAndHelpers(BlockStmt body, StringBuilder collector,
                                                 Map<String, String> localTypes,
                                                 Map<String, String> localElementTypes,
                                                 Map<String, String> localJavaTypes) {
        Set<String> declaredLocals = new HashSet<>(localTypes.keySet());
        List<VariableDeclarator> varDecls = body.findAll(VariableDeclarator.class);
        for (VariableDeclarator vd : varDecls) {
            String vName = vd.getNameAsString();
            String vTypeName = vd.getType().asString();
            if (vd.getType().isVarType()) {
                String inferredType = vd.getInitializer().map(e -> getExpressionJavaType(e, localJavaTypes)).orElse(null);
                if (inferredType == null && vd.getParentNode().flatMap(Node::getParentNode).filter(n -> n instanceof ForEachStmt).isPresent()) {
                    ForEachStmt loop = (ForEachStmt) vd.getParentNode().flatMap(Node::getParentNode).get();
                    String arrayType = getExpressionJavaType(loop.getIterable(), localJavaTypes);
                    if (arrayType != null && arrayType.endsWith("[]")) inferredType = arrayType.substring(0, arrayType.length() - 2);
                }
                if (inferredType == null || Set.of("null", "void", "var").contains(inferredType))
                    throw JavaSourcePreparation.error(vd, "Cannot infer type of local '" + vd.getNameAsString()
                            + "' from its initializer; use an explicit supported type or implement the called API");
                vTypeName = inferredType;
            }
            if (classRegistry != null && classRegistry.isRecord(vTypeName)) {
                RecordModel rm = classRegistry.getRecord(vTypeName);
                localJavaTypes.put(vName, vTypeName);
                for (FieldModel c : rm.getComponents()) {
                    String compLocal = vName + "$" + c.getName();
                    localTypes.put(compLocal, c.getWasmType());
                    localJavaTypes.put(compLocal, c.getTypeName());
                    if (!declaredLocals.contains(compLocal)) {
                        declaredLocals.add(compLocal);
                        collector.append("    (local $").append(compLocal).append(" ").append(c.getWasmType()).append(")\n");
                    }
                }
            } else {
                String vType = TypeMapper.mapType(vTypeName);
                if (vType == null) vType = "i32";
                localTypes.put(vName, vType);
                localJavaTypes.put(vName, vTypeName);
                if (vTypeName.endsWith("[]")) {
                    localElementTypes.put(vName, TypeMapper.mapType(vTypeName.substring(0, vTypeName.length() - 2)));
                }
                if (!declaredLocals.contains(vName)) {
                    declaredLocals.add(vName);
                    collector.append("    (local $").append(vName).append(" ").append(vType).append(")\n");
                }
            }
        }

        List<ForEachStmt> forEachStmts = body.findAll(ForEachStmt.class);
        for (int i = 1; i <= forEachStmts.size(); i++) {
            collector.append("    (local $__arr_ptr_").append(i).append(" i32)\n");
            collector.append("    (local $__arr_len_").append(i).append(" i32)\n");
            collector.append("    (local $__arr_idx_").append(i).append(" i32)\n");
        }
        collector.append("    (local $__arr_alloc_ptr i32)\n");
        collector.append("    (local $__arr_alloc_len i32)\n");

        List<SwitchStmt> switchStmts = body.findAll(SwitchStmt.class);
        for (int i = 1; i <= switchStmts.size(); i++) {
            collector.append("    (local $__switch_val_").append(i).append(" i32)\n");
        }
        List<SwitchExpr> switchExprs = body.findAll(SwitchExpr.class);
        for (int i = 1; i <= switchExprs.size(); i++) {
            collector.append("    (local $__switchexpr_val_").append(i).append(" i32)\n");
        }

        collector.append("    (local $__str_arg_ptr i32)\n");
        collector.append("    (local $__str_tmp i32)\n");
        collector.append("    (local $__str_sub_start i32)\n");
        collector.append("    (local $__sb_tmp i32)\n");
        collector.append("    (local $__obj_alloc_ptr i32)\n");
        collector.append("    (local $__obj_tmp i32)\n");
        collector.append("    (local $__vtable_tmp i32)\n");
        collector.append("    (local $__atomic_tmp i32)\n");
        collector.append("    (local $__atomic_val i32)\n");
        collector.append("    (local $__simd_tmp v128)\n");
        collector.append("    (local $__f64_tmp f64)\n");
        collector.append("    (local $__f32_tmp f32)\n");
        collector.append("    (local $__i64_tmp i64)\n");

        List<TryStmt> tryStmts = body.findAll(TryStmt.class);
        for (TryStmt ts : tryStmts) {
            for (Expression res : ts.getResources()) {
                if (res.isVariableDeclarationExpr()) {
                    for (VariableDeclarator vd : res.asVariableDeclarationExpr().getVariables()) {
                        String vName = vd.getNameAsString();
                        String vType = TypeMapper.mapType(vd.getType());
                        if (vType == null) vType = "i32";
                        localTypes.put(vName, vType);
                        localJavaTypes.put(vName, vd.getType().asString());
                        if (!declaredLocals.contains(vName)) {
                            declaredLocals.add(vName);
                            collector.append("    (local $").append(vName).append(" ").append(vType).append(")\n");
                        }
                    }
                }
            }
            for (CatchClause cc : ts.getCatchClauses()) {
                String pName = cc.getParameter().getNameAsString();
                String pType = cc.getParameter().getType().asString();
                localTypes.put(pName, "i32");
                localJavaTypes.put(pName, pType);
                if (!declaredLocals.contains(pName)) {
                    declaredLocals.add(pName);
                    collector.append("    (local $").append(pName).append(" i32)\n");
                }
            }
        }
        for (MethodCallExpr call : body.findAll(MethodCallExpr.class)) {
            String local = "__receiver$" + receiverLocals.size();
            receiverLocals.put(call, local);
            collector.append("    (local $").append(local).append(" i32)\n");
        }
        collector.append("    (local $__caught_exc i32)\n");
    }

    private void translateStatement(Statement stmt, StringBuilder collector, Map<String, String> localTypes, Map<String, String> localElementTypes, Map<String, String> localJavaTypes, String returnWasmType) {
        translateStatement(stmt, collector, localTypes, localElementTypes, localJavaTypes, returnWasmType, Collections.emptyList());
    }

    private void translateStatement(Statement stmt, StringBuilder collector, Map<String, String> localTypes, Map<String, String> localElementTypes, Map<String, String> localJavaTypes, String returnWasmType, List<String> pendingLabels) {
        if (sourceMapTracker != null && !stmt.isBlockStmt()) {
            sourceMapTracker.track(collector, stmt);
        }
        if (stmt.isExplicitConstructorInvocationStmt()) {
            ExplicitConstructorInvocationStmt ecis = stmt.asExplicitConstructorInvocationStmt();
            collector.append("    local.get $this\n");
            String targetClass = ecis.isThis() ? currentClass.getName() : currentClass.getSuperClassName();
            String initFunc = emitConstructorArguments(targetClass, ecis.getArguments(), ecis,
                    collector, localTypes, localElementTypes, localJavaTypes);
            collector.append("    call ").append(initFunc).append("\n");
        } else if (stmt.isReturnStmt()) {
            ReturnStmt ret = stmt.asReturnStmt();
            if (ret.getExpression().isPresent()) {
                Expression retExpr = ret.getExpression().get();
                if (retExpr.isObjectCreationExpr() && classRegistry != null && classRegistry.isRecord(retExpr.asObjectCreationExpr().getTypeAsString())) {
                    ObjectCreationExpr oce = retExpr.asObjectCreationExpr();
                    RecordModel rm = classRegistry.getRecord(oce.getTypeAsString());
                    for (int i = 0; i < oce.getArguments().size(); i++) {
                        String compWasm = i < rm.getComponents().size() ? rm.getComponents().get(i).getWasmType() : "i32";
                        translateExpression(oce.getArgument(i), collector, localTypes, localElementTypes, localJavaTypes, compWasm);
                    }
                } else if (retExpr.isNameExpr() && localJavaTypes.containsKey(retExpr.asNameExpr().getNameAsString())
                        && classRegistry != null && classRegistry.isRecord(localJavaTypes.get(retExpr.asNameExpr().getNameAsString()))) {
                    String varName = retExpr.asNameExpr().getNameAsString();
                    RecordModel rm = classRegistry.getRecord(localJavaTypes.get(varName));
                    for (FieldModel c : rm.getComponents()) {
                        collector.append("    local.get $").append(varName).append("$").append(c.getName()).append("\n");
                    }
                } else {
                    translateExpression(retExpr, collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
                }
            }
            collector.append("    return\n");
        } else if (stmt.isExpressionStmt()) {
            ExpressionStmt exprStmt = stmt.asExpressionStmt();
            translateExpression(exprStmt.getExpression(), collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
            if (expressionLeavesValueOnStack(exprStmt.getExpression(), localJavaTypes)) {
                collector.append("    drop\n");
            }
        } else if (stmt.isIfStmt()) {
            IfStmt ifStmt = stmt.asIfStmt();
            translateExpression(ifStmt.getCondition(), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    if\n");
            translateStatement(ifStmt.getThenStmt(), collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
            if (ifStmt.getElseStmt().isPresent()) {
                collector.append("    else\n");
                translateStatement(ifStmt.getElseStmt().get(), collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
            }
            collector.append("    end\n");
        } else if (stmt.isWhileStmt()) {
            WhileStmt whileStmt = stmt.asWhileStmt();
            int loopId = ++loopCounter;
            String breakLabel = "break_while_" + loopId;
            String loopLabel = "loop_while_" + loopId;
            loopStack.push(new LoopContext(pendingLabels, breakLabel, loopLabel));

            collector.append("    (block $").append(breakLabel).append("\n");
            collector.append("      (loop $").append(loopLabel).append("\n");
            translateExpression(whileStmt.getCondition(), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("        i32.eqz\n");
            collector.append("        br_if $").append(breakLabel).append("\n");
            translateStatement(whileStmt.getBody(), collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
            collector.append("        br $").append(loopLabel).append("\n");
            collector.append("      )\n");
            collector.append("    )\n");

            loopStack.pop();
        } else if (stmt.isForStmt()) {
            ForStmt forStmt = stmt.asForStmt();
            int loopId = ++loopCounter;
            String breakLabel = "break_for_" + loopId;
            String loopLabel = "loop_for_" + loopId;
            String continueLabel = "continue_for_" + loopId;
            loopStack.push(new LoopContext(pendingLabels, breakLabel, continueLabel));

            for (Expression init : forStmt.getInitialization()) {
                translateExpression(init, collector, localTypes, localElementTypes, localJavaTypes, null);
                if (expressionLeavesValueOnStack(init, localJavaTypes)) {
                    collector.append("    drop\n");
                }
            }

            collector.append("    (block $").append(breakLabel).append("\n");
            collector.append("      (loop $").append(loopLabel).append("\n");
            if (forStmt.getCompare().isPresent()) {
                translateExpression(forStmt.getCompare().get(), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("        i32.eqz\n");
                collector.append("        br_if $").append(breakLabel).append("\n");
            }
            collector.append("        (block $").append(continueLabel).append("\n");
            translateStatement(forStmt.getBody(), collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
            collector.append("        )\n");
            for (Expression update : forStmt.getUpdate()) {
                translateExpression(update, collector, localTypes, localElementTypes, localJavaTypes, null);
                if (expressionLeavesValueOnStack(update, localJavaTypes)) {
                    collector.append("    drop\n");
                }
            }
            collector.append("        br $").append(loopLabel).append("\n");
            collector.append("      )\n");
            collector.append("    )\n");

            loopStack.pop();
        } else if (stmt.isForEachStmt()) {
            ForEachStmt forEach = stmt.asForEachStmt();
            int forEachNum = ++forEachCounter;
            int loopId = ++loopCounter;
            String breakLabel = "break_foreach_" + loopId;
            String loopLabel = "loop_foreach_" + loopId;
            String continueLabel = "continue_foreach_" + loopId;
            loopStack.push(new LoopContext(pendingLabels, breakLabel, continueLabel));

            String ptrVar = "__arr_ptr_" + forEachNum;
            String lenVar = "__arr_len_" + forEachNum;
            String idxVar = "__arr_idx_" + forEachNum;
            String itemVar = forEach.getVariable().getVariables().get(0).getNameAsString();
            String elemType = localTypes.getOrDefault(itemVar, TypeMapper.mapType(forEach.getVariable().getVariables().get(0).getType()));
            if (elemType == null) elemType = "i32";
            int headerOffset = (elemType.equals("f64") || elemType.equals("i64")) ? 8 : 4;
            int elemSize = (elemType.equals("f64") || elemType.equals("i64")) ? 8 : 4;

            translateExpression(forEach.getIterable(), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    local.set $").append(ptrVar).append("\n");
            collector.append("    local.get $").append(ptrVar).append("\n");
            collector.append("    i32.load\n");
            collector.append("    local.set $").append(lenVar).append("\n");
            collector.append("    i32.const 0\n");
            collector.append("    local.set $").append(idxVar).append("\n");

            collector.append("    (block $").append(breakLabel).append("\n");
            collector.append("      (loop $").append(loopLabel).append("\n");
            collector.append("        local.get $").append(idxVar).append("\n");
            collector.append("        local.get $").append(lenVar).append("\n");
            collector.append("        i32.ge_s\n");
            collector.append("        br_if $").append(breakLabel).append("\n");

            collector.append("        local.get $").append(ptrVar).append("\n");
            collector.append("        local.get $").append(idxVar).append("\n");
            collector.append("        i32.const ").append(elemSize).append("\n");
            collector.append("        i32.mul\n");
            collector.append("        i32.add\n");
            collector.append("        ").append(getLoadInstruction(elemType)).append(" offset=").append(headerOffset).append("\n");
            collector.append("        local.set $").append(itemVar).append("\n");

            collector.append("        (block $").append(continueLabel).append("\n");
            translateStatement(forEach.getBody(), collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
            collector.append("        )\n");

            collector.append("        local.get $").append(idxVar).append("\n");
            collector.append("        i32.const 1\n");
            collector.append("        i32.add\n");
            collector.append("        local.set $").append(idxVar).append("\n");
            collector.append("        br $").append(loopLabel).append("\n");
            collector.append("      )\n");
            collector.append("    )\n");

            loopStack.pop();
        } else if (stmt.isSwitchStmt()) {
            SwitchStmt switchStmt = stmt.asSwitchStmt();
            int switchNum = ++switchCounter;
            int switchId = ++loopCounter;
            String breakLabel = "break_switch_" + switchId;
            String valVar = "__switch_val_" + switchNum;

            String enclosingLoopContinue = null;
            for (LoopContext ctx : loopStack) {
                if (ctx.continueLabel != null) {
                    enclosingLoopContinue = ctx.continueLabel;
                    break;
                }
            }
            loopStack.push(new LoopContext(pendingLabels, breakLabel, enclosingLoopContinue));

            String selectorType = getExpressionJavaType(switchStmt.getSelector(), localJavaTypes);
            ClassModel enumModel = (selectorType != null && classRegistry != null) ? classRegistry.getClass(selectorType) : null;
            if (enumModel != null && !enumModel.isEnum()) {
                enumModel = null;
            }
            switchEnumStack.push(enumModel != null ? enumModel.getName() : "");

            try {
                translateExpression(switchStmt.getSelector(), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    local.set $").append(valVar).append("\n");

                collector.append("    (block $").append(breakLabel).append("\n");
                List<SwitchEntry> entries = switchStmt.getEntries();
                int numEntries = entries.size();
                for (int i = numEntries - 1; i >= 0; i--) {
                    collector.append("      (block $case_").append(switchId).append("_").append(i).append("\n");
                }
                for (int i = 0; i < numEntries; i++) {
                    SwitchEntry entry = entries.get(i);
                    if (!entry.getLabels().isEmpty()) {
                        for (Expression lbl : entry.getLabels()) {
                            collector.append("        local.get $").append(valVar).append("\n");
                            translateExpression(lbl, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                            collector.append("        i32.eq\n");
                            collector.append("        br_if $case_").append(switchId).append("_").append(i).append("\n");
                        }
                    } else {
                        collector.append("        br $case_").append(switchId).append("_").append(i).append("\n");
                    }
                }
                collector.append("        br $").append(breakLabel).append("\n");

                for (int i = 0; i < numEntries; i++) {
                    SwitchEntry entry = entries.get(i);
                    collector.append("      ) ;; end block $case_").append(switchId).append("_").append(i).append("\n");
                    for (Statement s : entry.getStatements()) {
                        translateStatement(s, collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
                    }
                }
                collector.append("    )\n");
            } finally {
                switchEnumStack.pop();
            }

            loopStack.pop();
        } else if (stmt.isBreakStmt()) {
            BreakStmt brk = stmt.asBreakStmt();
            String targetLabel = null;
            if (brk.getLabel().isPresent()) {
                String lblName = brk.getLabel().get().getIdentifier();
                for (LoopContext ctx : loopStack) {
                    if (ctx.hasLabel(lblName)) {
                        targetLabel = ctx.breakLabel;
                        break;
                    }
                }
            } else if (!loopStack.isEmpty()) {
                targetLabel = loopStack.peek().breakLabel;
            }
            if (targetLabel != null) {
                collector.append("    br $").append(targetLabel).append("\n");
            }
        } else if (stmt.isContinueStmt()) {
            ContinueStmt cont = stmt.asContinueStmt();
            String targetLabel = null;
            if (cont.getLabel().isPresent()) {
                String lblName = cont.getLabel().get().getIdentifier();
                for (LoopContext ctx : loopStack) {
                    if (ctx.hasLabel(lblName) && ctx.continueLabel != null) {
                        targetLabel = ctx.continueLabel;
                        break;
                    }
                }
            } else {
                for (LoopContext ctx : loopStack) {
                    if (ctx.continueLabel != null) {
                        targetLabel = ctx.continueLabel;
                        break;
                    }
                }
            }
            if (targetLabel != null) {
                collector.append("    br $").append(targetLabel).append("\n");
            }
        } else if (stmt.isLabeledStmt()) {
            LabeledStmt labeledStmt = stmt.asLabeledStmt();
            String labelName = labeledStmt.getLabel().getIdentifier();
            List<String> newPending = new ArrayList<>(pendingLabels);
            newPending.add(labelName);

            Statement inner = labeledStmt.getStatement();
            if (inner.isForStmt() || inner.isForEachStmt() || inner.isWhileStmt() || inner.isSwitchStmt()) {
                translateStatement(inner, collector, localTypes, localElementTypes, localJavaTypes, returnWasmType, newPending);
            } else {
                int blockId = ++loopCounter;
                String blockLabel = "break_label_" + blockId;
                loopStack.push(new LoopContext(newPending, blockLabel, null));
                collector.append("    (block $").append(blockLabel).append("\n");
                translateStatement(inner, collector, localTypes, localElementTypes, localJavaTypes, returnWasmType, Collections.emptyList());
                collector.append("    )\n");
                loopStack.pop();
            }
        } else if (stmt.isBlockStmt()) {
            for (Statement s : stmt.asBlockStmt().getStatements()) {
                translateStatement(s, collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
            }
        } else if (stmt.isThrowStmt()) {
            ThrowStmt throwStmt = stmt.asThrowStmt();
            translateExpression(throwStmt.getExpression(), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    throw $Exception\n");
        } else if (stmt.isTryStmt()) {
            translateTryStatement(stmt.asTryStmt(), collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
        }
    }

    private void translateTryStatement(TryStmt tryStmt, StringBuilder collector, Map<String, String> localTypes, Map<String, String> localElementTypes, Map<String, String> localJavaTypes, String returnWasmType) {
        boolean hasFinally = tryStmt.getFinallyBlock().isPresent();
        boolean hasCatch = !tryStmt.getCatchClauses().isEmpty();
        boolean hasResources = !tryStmt.getResources().isEmpty();

        if (hasResources) {
            if (hasCatch && !hasFinally) {
                collector.append("    (try\n");
                collector.append("      (do\n");
                translateTryBlockWithResources(tryStmt.getResources(), 0, tryStmt.getTryBlock(), collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
                collector.append("      )\n");
                collector.append("      (catch $Exception\n");
                collector.append("        local.set $__caught_exc\n");
                translateCatchClauses(tryStmt.getCatchClauses(), collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
                collector.append("      )\n");
                collector.append("    )\n");
            } else if (!hasCatch && hasFinally) {
                collector.append("    (try\n");
                collector.append("      (do\n");
                translateTryBlockWithResources(tryStmt.getResources(), 0, tryStmt.getTryBlock(), collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
                collector.append("      )\n");
                collector.append("      (catch_all\n");
                for (Statement s : tryStmt.getFinallyBlock().get().getStatements()) {
                    translateStatement(s, collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
                }
                collector.append("        (rethrow 0)\n");
                collector.append("      )\n");
                collector.append("    )\n");
                for (Statement s : tryStmt.getFinallyBlock().get().getStatements()) {
                    translateStatement(s, collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
                }
            } else if (hasCatch && hasFinally) {
                collector.append("    (try\n");
                collector.append("      (do\n");
                collector.append("        (try\n");
                collector.append("          (do\n");
                translateTryBlockWithResources(tryStmt.getResources(), 0, tryStmt.getTryBlock(), collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
                collector.append("          )\n");
                collector.append("          (catch $Exception\n");
                collector.append("            local.set $__caught_exc\n");
                translateCatchClauses(tryStmt.getCatchClauses(), collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
                collector.append("          )\n");
                collector.append("        )\n");
                collector.append("      )\n");
                collector.append("      (catch_all\n");
                for (Statement s : tryStmt.getFinallyBlock().get().getStatements()) {
                    translateStatement(s, collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
                }
                collector.append("        (rethrow 0)\n");
                collector.append("      )\n");
                collector.append("    )\n");
                for (Statement s : tryStmt.getFinallyBlock().get().getStatements()) {
                    translateStatement(s, collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
                }
            } else {
                translateTryBlockWithResources(tryStmt.getResources(), 0, tryStmt.getTryBlock(), collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
            }
            return;
        }

        if (hasCatch && !hasFinally) {
            collector.append("    (try\n");
            collector.append("      (do\n");
            for (Statement s : tryStmt.getTryBlock().getStatements()) {
                translateStatement(s, collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
            }
            collector.append("      )\n");
            collector.append("      (catch $Exception\n");
            collector.append("        local.set $__caught_exc\n");
            translateCatchClauses(tryStmt.getCatchClauses(), collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
            collector.append("      )\n");
            collector.append("    )\n");
        } else if (!hasCatch && hasFinally) {
            collector.append("    (try\n");
            collector.append("      (do\n");
            for (Statement s : tryStmt.getTryBlock().getStatements()) {
                translateStatement(s, collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
            }
            collector.append("      )\n");
            collector.append("      (catch_all\n");
            for (Statement s : tryStmt.getFinallyBlock().get().getStatements()) {
                translateStatement(s, collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
            }
            collector.append("        (rethrow 0)\n");
            collector.append("      )\n");
            collector.append("    )\n");
            for (Statement s : tryStmt.getFinallyBlock().get().getStatements()) {
                translateStatement(s, collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
            }
        } else if (hasCatch && hasFinally) {
            collector.append("    (try\n");
            collector.append("      (do\n");
            collector.append("        (try\n");
            collector.append("          (do\n");
            for (Statement s : tryStmt.getTryBlock().getStatements()) {
                translateStatement(s, collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
            }
            collector.append("          )\n");
            collector.append("          (catch $Exception\n");
            collector.append("            local.set $__caught_exc\n");
            translateCatchClauses(tryStmt.getCatchClauses(), collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
            collector.append("          )\n");
            collector.append("        )\n");
            collector.append("      )\n");
            collector.append("      (catch_all\n");
            for (Statement s : tryStmt.getFinallyBlock().get().getStatements()) {
                translateStatement(s, collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
            }
            collector.append("        (rethrow 0)\n");
            collector.append("      )\n");
            collector.append("    )\n");
            for (Statement s : tryStmt.getFinallyBlock().get().getStatements()) {
                translateStatement(s, collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
            }
        }
    }

    private void translateTryBlockWithResources(List<Expression> resources, int index, BlockStmt tryBlock, StringBuilder collector, Map<String, String> localTypes, Map<String, String> localElementTypes, Map<String, String> localJavaTypes, String returnWasmType) {
        if (index >= resources.size()) {
            for (Statement s : tryBlock.getStatements()) {
                translateStatement(s, collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
            }
            return;
        }

        Expression resExpr = resources.get(index);
        String varName = null;
        if (resExpr.isVariableDeclarationExpr()) {
            VariableDeclarationExpr vde = resExpr.asVariableDeclarationExpr();
            for (VariableDeclarator vd : vde.getVariables()) {
                varName = vd.getNameAsString();
                if (vd.getInitializer().isPresent()) {
                    translateExpression(vd.getInitializer().get(), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    collector.append("    local.set $").append(varName).append("\n");
                }
            }
        } else if (resExpr.isNameExpr()) {
            varName = resExpr.asNameExpr().getNameAsString();
        }

        final String finalVarName = varName;
        collector.append("    (try\n");
        collector.append("      (do\n");
        translateTryBlockWithResources(resources, index + 1, tryBlock, collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
        collector.append("      )\n");
        collector.append("      (catch_all\n");
        if (finalVarName != null) {
            emitResourceClose(finalVarName, collector);
        }
        collector.append("        (rethrow 0)\n");
        collector.append("      )\n");
        collector.append("    )\n");
        if (finalVarName != null) {
            emitResourceClose(finalVarName, collector);
        }
    }

    private void emitResourceClose(String varName, StringBuilder collector) {
        String sigName = classRegistry != null ? classRegistry.getOrCreateTypeSignature("(func (param i32))") : "$t_sig_void_close";
        collector.append("        local.get $").append(varName).append("\n");
        collector.append("        if\n");
        collector.append("          local.get $").append(varName).append("\n");
        collector.append("          local.get $").append(varName).append("\n");
        collector.append("          i32.load offset=0\n");
        collector.append("          i32.const 3\n");
        collector.append("          i32.add\n");
        collector.append("          call_indirect (type ").append(sigName).append(")\n");
        collector.append("        end\n");
    }

    private void translateCatchClauses(List<CatchClause> clauses, StringBuilder collector, Map<String, String> localTypes, Map<String, String> localElementTypes, Map<String, String> localJavaTypes, String returnWasmType) {
        if (clauses.isEmpty()) return;

        if (clauses.size() == 1) {
            CatchClause cc = clauses.get(0);
            String pName = cc.getParameter().getNameAsString();
            collector.append("        local.get $__caught_exc\n");
            collector.append("        local.set $").append(pName).append("\n");
            for (Statement s : cc.getBody().getStatements()) {
                translateStatement(s, collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
            }
            return;
        }

        int unclosedElseCount = 0;
        for (int i = 0; i < clauses.size(); i++) {
            CatchClause cc = clauses.get(i);
            String pName = cc.getParameter().getNameAsString();
            String pType = cc.getParameter().getType().asString();
            boolean isLast = (i == clauses.size() - 1);
            boolean isCatchAllType = pType.equals("Exception") || pType.equals("Throwable") || pType.equals("Object");

            if (isLast && isCatchAllType) {
                collector.append("        local.get $__caught_exc\n");
                collector.append("        local.set $").append(pName).append("\n");
                for (Statement s : cc.getBody().getStatements()) {
                    translateStatement(s, collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
                }
            } else {
                collector.append("        local.get $__caught_exc\n");
                collector.append("        i32.eqz\n");
                collector.append("        if (result i32)\n");
                collector.append("          i32.const 0\n");
                collector.append("        else\n");
                collector.append("          local.get $__caught_exc\n");
                collector.append("          i32.load offset=0\n");
                collector.append("          local.set $__vtable_tmp\n");
                collector.append("          i32.const 0\n");
                if (classRegistry != null) {
                    for (ClassModel cm : classRegistry.getClasses().values()) {
                        if (classRegistry.isSubclassOf(cm.getName(), pType)) {
                            collector.append("          local.get $__vtable_tmp\n");
                            collector.append("          i32.const ").append(cm.getVtableBase()).append("\n");
                            collector.append("          i32.eq\n");
                            collector.append("          i32.or\n");
                        }
                    }
                }
                collector.append("        end\n");
                collector.append("        if\n");
                collector.append("          local.get $__caught_exc\n");
                collector.append("          local.set $").append(pName).append("\n");
                for (Statement s : cc.getBody().getStatements()) {
                    translateStatement(s, collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
                }
                collector.append("        else\n");
                unclosedElseCount++;
                if (isLast) {
                    collector.append("          local.get $__caught_exc\n");
                    collector.append("          throw $Exception\n");
                }
            }
        }

        while (unclosedElseCount > 0) {
            collector.append("        end\n");
            unclosedElseCount--;
        }
    }

    private void translateExpression(Expression expr, StringBuilder collector, Map<String, String> localTypes, Map<String, String> localElementTypes, Map<String, String> localJavaTypes, String expectedType) {
        if (expr.isBinaryExpr()) {
            BinaryExpr bin = expr.asBinaryExpr();
            if (bin.getOperator() == BinaryExpr.Operator.AND || bin.getOperator() == BinaryExpr.Operator.OR) {
                translateExpression(bin.getLeft(), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    if (result i32)\n");
                if (bin.getOperator() == BinaryExpr.Operator.OR) collector.append("    i32.const 1\n");
                else translateExpression(bin.getRight(), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    else\n");
                if (bin.getOperator() == BinaryExpr.Operator.AND) collector.append("    i32.const 0\n");
                else translateExpression(bin.getRight(), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    end\n");
                return;
            }
            if (bin.getOperator() == BinaryExpr.Operator.PLUS && (isStringType(bin.getLeft(), localJavaTypes) || isStringType(bin.getRight(), localJavaTypes))) {
                translateStringToStack(bin.getLeft(), collector, localTypes, localElementTypes, localJavaTypes);
                translateStringToStack(bin.getRight(), collector, localTypes, localElementTypes, localJavaTypes);
                collector.append("    call $__str_concat\n");
                return;
            }
            String leftType = inferType(bin.getLeft(), localTypes, localElementTypes, localJavaTypes, null);
            String rightType = inferType(bin.getRight(), localTypes, localElementTypes, localJavaTypes, null);
            String opType = "i32";
            if (bin.getOperator() == BinaryExpr.Operator.AND || bin.getOperator() == BinaryExpr.Operator.OR) {
                opType = "i32";
            } else if ("f64".equals(leftType) || "f64".equals(rightType) || "f64".equals(opType) || ("f32".equals(leftType) && ("f64".equals(rightType) || bin.getRight().isDoubleLiteralExpr())) || ("f32".equals(rightType) && ("f64".equals(leftType) || bin.getLeft().isDoubleLiteralExpr()))) {
                opType = "f64";
            } else if ("f32".equals(leftType) || "f32".equals(rightType)) {
                opType = "f32";
            } else if ("i64".equals(leftType) || "i64".equals(rightType) || "i64".equals(opType)) {
                opType = "i64";
            }
            if (bin.getOperator() == BinaryExpr.Operator.LEFT_SHIFT || bin.getOperator() == BinaryExpr.Operator.SIGNED_RIGHT_SHIFT
                    || bin.getOperator() == BinaryExpr.Operator.UNSIGNED_RIGHT_SHIFT) opType = "i64".equals(leftType) ? "i64" : "i32";
            boolean isComparison = bin.getOperator() == BinaryExpr.Operator.EQUALS ||
                    bin.getOperator() == BinaryExpr.Operator.NOT_EQUALS ||
                    bin.getOperator() == BinaryExpr.Operator.LESS ||
                    bin.getOperator() == BinaryExpr.Operator.LESS_EQUALS ||
                    bin.getOperator() == BinaryExpr.Operator.GREATER ||
                    bin.getOperator() == BinaryExpr.Operator.GREATER_EQUALS;
            String resultType = isComparison ? "i32" : opType;
            translateExpression(bin.getLeft(), collector, localTypes, localElementTypes, localJavaTypes, opType);
            translateExpression(bin.getRight(), collector, localTypes, localElementTypes, localJavaTypes, opType);
            collector.append("    ").append(getWasmBinaryOp(bin.getOperator(), opType)).append("\n");
            if ("i64".equals(resultType) && ("f64".equals(expectedType) || "f32".equals(expectedType))) {
                emitNumericWidening(resultType, expectedType, collector);
            } else if ("f64".equals(expectedType) && "f32".equals(resultType)) {
                collector.append("    f64.promote_f32\n");
            } else if ("f32".equals(expectedType) && "f64".equals(resultType)) {
                collector.append("    f32.demote_f64\n");
            } else if ("f64".equals(expectedType) && "i32".equals(resultType)) {
                collector.append("    f64.convert_i32_s\n");
            } else if ("f32".equals(expectedType) && "i32".equals(resultType)) {
                collector.append("    f32.convert_i32_s\n");
            } else if ("i64".equals(expectedType) && "i32".equals(resultType)) {
                collector.append("    i64.extend_i32_s\n");
            } else if ("i32".equals(expectedType) && "i64".equals(resultType)) {
                collector.append("    i32.wrap_i64\n");
            }
        } else if (expr.isUnaryExpr()) {
            UnaryExpr u = expr.asUnaryExpr();
            String uType = inferType(u.getExpression(), localTypes, localElementTypes, localJavaTypes, expectedType);
            if (u.getOperator() == UnaryExpr.Operator.MINUS) {
                if ("f64".equals(uType)) {
                    translateExpression(u.getExpression(), collector, localTypes, localElementTypes, localJavaTypes, uType);
                    collector.append("    f64.neg\n");
                } else if ("f32".equals(uType)) {
                    translateExpression(u.getExpression(), collector, localTypes, localElementTypes, localJavaTypes, uType);
                    collector.append("    f32.neg\n");
                } else if ("i64".equals(uType)) {
                    collector.append("    i64.const 0\n");
                    translateExpression(u.getExpression(), collector, localTypes, localElementTypes, localJavaTypes, uType);
                    collector.append("    i64.sub\n");
                } else {
                    collector.append("    i32.const 0\n");
                    translateExpression(u.getExpression(), collector, localTypes, localElementTypes, localJavaTypes, uType);
                    collector.append("    i32.sub\n");
                }
            } else if (u.getOperator() == UnaryExpr.Operator.LOGICAL_COMPLEMENT) {
                translateExpression(u.getExpression(), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    i32.eqz\n");
            } else if (u.getOperator() == UnaryExpr.Operator.BITWISE_COMPLEMENT) {
                translateExpression(u.getExpression(), collector, localTypes, localElementTypes, localJavaTypes, uType);
                if ("i64".equals(uType)) {
                    collector.append("    i64.const -1\n    i64.xor\n");
                } else {
                    collector.append("    i32.const -1\n    i32.xor\n");
                }
            } else if (u.getOperator() == UnaryExpr.Operator.POSTFIX_INCREMENT || u.getOperator() == UnaryExpr.Operator.PREFIX_INCREMENT) {
                if (u.getExpression().isNameExpr()) {
                    String var = u.getExpression().asNameExpr().getNameAsString();
                    boolean isGlobal = globalVars != null && globalVars.containsKey(var) && !localTypes.containsKey(var);
                    FieldModel fm = (currentClass != null && !localTypes.containsKey(var)) ? currentClass.getField(var) : null;
                    if (fm != null && !fm.isStatic()) {
                        collector.append("    local.get $this\n");
                        collector.append("    local.get $this\n");
                        collector.append("    ").append(fm.getWasmType()).append(".load offset=").append(fm.getOffset()).append("\n");
                        collector.append("    i32.const 1\n");
                        collector.append("    i32.add\n");
                        collector.append("    ").append(fm.getWasmType()).append(".store offset=").append(fm.getOffset()).append("\n");
                    } else {
                        collector.append(isGlobal ? "    global.get $" : "    local.get $").append(var).append("\n");
                        collector.append("    i32.const 1\n");
                        collector.append("    i32.add\n");
                        collector.append(isGlobal ? "    global.set $" : "    local.set $").append(var).append("\n");
                    }
                } else if (u.getExpression().isFieldAccessExpr()) {
                    FieldAccessExpr fae = u.getExpression().asFieldAccessExpr();
                    String fName = fae.getNameAsString();
                    String targetType = getExpressionJavaType(fae.getScope(), localJavaTypes);
                    FieldModel fm = null;
                    if (targetType != null && classRegistry != null && classRegistry.hasClass(targetType)) {
                        fm = classRegistry.getClass(targetType).getField(fName);
                    } else if (currentClass != null && currentClass.getField(fName) != null) {
                        fm = currentClass.getField(fName);
                    }
                    if (fm != null && !fm.isStatic()) {
                        translateExpression(fae.getScope(), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        collector.append("    local.set $__obj_tmp\n");
                        collector.append("    local.get $__obj_tmp\n");
                        collector.append("    local.get $__obj_tmp\n");
                        collector.append("    ").append(fm.getWasmType()).append(".load offset=").append(fm.getOffset()).append("\n");
                        collector.append("    i32.const 1\n");
                        collector.append("    i32.add\n");
                        collector.append("    ").append(fm.getWasmType()).append(".store offset=").append(fm.getOffset()).append("\n");
                    }
                }
            } else if (u.getOperator() == UnaryExpr.Operator.POSTFIX_DECREMENT || u.getOperator() == UnaryExpr.Operator.PREFIX_DECREMENT) {
                if (u.getExpression().isNameExpr()) {
                    String var = u.getExpression().asNameExpr().getNameAsString();
                    boolean isGlobal = globalVars != null && globalVars.containsKey(var) && !localTypes.containsKey(var);
                    FieldModel fm = (currentClass != null && !localTypes.containsKey(var)) ? currentClass.getField(var) : null;
                    if (fm != null && !fm.isStatic()) {
                        collector.append("    local.get $this\n");
                        collector.append("    local.get $this\n");
                        collector.append("    ").append(fm.getWasmType()).append(".load offset=").append(fm.getOffset()).append("\n");
                        collector.append("    i32.const 1\n");
                        collector.append("    i32.sub\n");
                        collector.append("    ").append(fm.getWasmType()).append(".store offset=").append(fm.getOffset()).append("\n");
                    } else {
                        collector.append(isGlobal ? "    global.get $" : "    local.get $").append(var).append("\n");
                        collector.append("    i32.const 1\n");
                        collector.append("    i32.sub\n");
                        collector.append(isGlobal ? "    global.set $" : "    local.set $").append(var).append("\n");
                    }
                } else if (u.getExpression().isFieldAccessExpr()) {
                    FieldAccessExpr fae = u.getExpression().asFieldAccessExpr();
                    String fName = fae.getNameAsString();
                    String targetType = getExpressionJavaType(fae.getScope(), localJavaTypes);
                    FieldModel fm = null;
                    if (targetType != null && classRegistry != null && classRegistry.hasClass(targetType)) {
                        fm = classRegistry.getClass(targetType).getField(fName);
                    } else if (currentClass != null && currentClass.getField(fName) != null) {
                        fm = currentClass.getField(fName);
                    }
                    if (fm != null && !fm.isStatic()) {
                        translateExpression(fae.getScope(), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        collector.append("    local.set $__obj_tmp\n");
                        collector.append("    local.get $__obj_tmp\n");
                        collector.append("    local.get $__obj_tmp\n");
                        collector.append("    ").append(fm.getWasmType()).append(".load offset=").append(fm.getOffset()).append("\n");
                        collector.append("    i32.const 1\n");
                        collector.append("    i32.sub\n");
                        collector.append("    ").append(fm.getWasmType()).append(".store offset=").append(fm.getOffset()).append("\n");
                    }
                }
            } else {
                translateExpression(u.getExpression(), collector, localTypes, localElementTypes, localJavaTypes, expectedType);
            }
        } else if (expr.isIntegerLiteralExpr()) {
            if ("f64".equals(expectedType)) {
                String val = expr.asIntegerLiteralExpr().getValue();
                if (!val.contains(".")) val += ".0";
                collector.append("    f64.const ").append(val).append("\n");
            } else if ("f32".equals(expectedType)) {
                String val = expr.asIntegerLiteralExpr().getValue();
                if (!val.contains(".")) val += ".0";
                collector.append("    f32.const ").append(val).append("\n");
            } else if ("i64".equals(expectedType)) {
                collector.append("    i64.const ").append(expr.asIntegerLiteralExpr().getValue()).append("\n");
            } else {
                collector.append("    i32.const ").append(expr.asIntegerLiteralExpr().getValue()).append("\n");
            }
        } else if (expr.isLongLiteralExpr()) {
            String val = expr.asLongLiteralExpr().getValue().replaceAll("[lL]", "");
            collector.append("    i64.const ").append(val).append("\n");
            if ("i32".equals(expectedType)) collector.append("    i32.wrap_i64\n");
            else emitNumericWidening("i64", expectedType, collector);
        } else if (expr.isDoubleLiteralExpr()) {
            String raw = expr.asDoubleLiteralExpr().getValue();
            boolean isExplicitFloat = raw.endsWith("f") || raw.endsWith("F");
            String dVal = raw.replaceAll("[fFdD]", "");
            if (!dVal.contains(".")) dVal += ".0";
            if ("f32".equals(expectedType) || (isExplicitFloat && expectedType == null)) {
                collector.append("    f32.const ").append(dVal).append("\n");
            } else {
                collector.append("    f64.const ").append(dVal).append("\n");
            }
        } else if (expr.isBooleanLiteralExpr()) {
            collector.append("    i32.const ").append(expr.asBooleanLiteralExpr().getValue() ? "1" : "0").append("\n");
        } else if (expr.isCharLiteralExpr()) {
            collector.append("    i32.const ").append(JavacToWasm.unescapeChar(expr.asCharLiteralExpr().getValue())).append("\n");
        } else if (expr.isStringLiteralExpr()) {
            String val = JavacToWasm.unescapeString(expr.asStringLiteralExpr().getValue());
            StringConstant sc = stringPool != null ? stringPool.get(val) : null;
            if (sc != null) {
                collector.append("    i32.const ").append(sc.offset).append("\n");
            } else {
                collector.append("    i32.const 0\n");
            }
        } else if (expr.isNullLiteralExpr()) {
            collector.append("    i32.const 0\n");
        } else if (expr.isClassExpr()) {
            ClassExpr ce = expr.asClassExpr();
            String typeName = ce.getType().asString();
            if (typeName.contains("<")) {
                typeName = typeName.substring(0, typeName.indexOf('<')).trim();
            }
            collector.append("    global.get $__class_").append(typeName).append("\n");
        } else if (expr.isConditionalExpr()) {
            ConditionalExpr cond = expr.asConditionalExpr();
            String resType = TypeMapper.mapType(getExpressionJavaType(cond, localJavaTypes));
            if (resType == null || (!resType.equals("i64") && !resType.equals("f32") && !resType.equals("f64") && !resType.equals("v128"))) {
                resType = "i32";
            }
            translateExpression(cond.getCondition(), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    if (result ").append(resType).append(")\n");
            translateExpression(cond.getThenExpr(), collector, localTypes, localElementTypes, localJavaTypes, resType);
            collector.append("    else\n");
            translateExpression(cond.getElseExpr(), collector, localTypes, localElementTypes, localJavaTypes, resType);
            collector.append("    end\n");
            emitNumericWidening(resType, expectedType, collector);
        } else if (expr.isThisExpr() || expr.isSuperExpr()) {
            collector.append("    local.get $this\n");
        } else if (expr.isCastExpr()) {
            CastExpr ce = expr.asCastExpr();
            String castTarget = TypeMapper.mapType(ce.getType());
            String innerType = inferType(ce.getExpression(), localTypes, localElementTypes, localJavaTypes, null);
            translateExpression(ce.getExpression(), collector, localTypes, localElementTypes, localJavaTypes, innerType != null ? innerType : castTarget);
            if ("i32".equals(castTarget)) {
                if ("f64".equals(innerType)) {
                    collector.append("    i32.trunc_f64_s\n");
                } else if ("f32".equals(innerType)) {
                    collector.append("    i32.trunc_f32_s\n");
                } else if ("i64".equals(innerType)) {
                    collector.append("    i32.wrap_i64\n");
                }
            } else if ("i64".equals(castTarget)) {
                if ("i32".equals(innerType)) {
                    collector.append("    i64.extend_i32_s\n");
                } else if ("f64".equals(innerType)) {
                    collector.append("    i64.trunc_f64_s\n");
                } else if ("f32".equals(innerType)) {
                    collector.append("    i64.trunc_f32_s\n");
                }
            } else if ("f64".equals(castTarget)) {
                if ("i32".equals(innerType)) {
                    collector.append("    f64.convert_i32_s\n");
                } else if ("i64".equals(innerType)) {
                    collector.append("    f64.convert_i64_s\n");
                } else if ("f32".equals(innerType)) {
                    collector.append("    f64.promote_f32\n");
                }
            } else if ("f32".equals(castTarget)) {
                if ("i32".equals(innerType)) {
                    collector.append("    f32.convert_i32_s\n");
                } else if ("i64".equals(innerType)) {
                    collector.append("    f32.convert_i64_s\n");
                } else if ("f64".equals(innerType)) {
                    collector.append("    f32.demote_f64\n");
                }
            }
            if (expectedType != null && !expectedType.equals(castTarget)) {
                if ("f64".equals(expectedType) && "i32".equals(castTarget)) {
                    collector.append("    f64.convert_i32_s\n");
                } else if ("f64".equals(expectedType) && "f32".equals(castTarget)) {
                    collector.append("    f64.promote_f32\n");
                } else if ("f64".equals(expectedType) && "i64".equals(castTarget)) {
                    collector.append("    f64.convert_i64_s\n");
                } else if ("f32".equals(expectedType) && "i32".equals(castTarget)) {
                    collector.append("    f32.convert_i32_s\n");
                } else if ("f32".equals(expectedType) && "f64".equals(castTarget)) {
                    collector.append("    f32.demote_f64\n");
                } else if ("f32".equals(expectedType) && "i64".equals(castTarget)) {
                    collector.append("    f32.convert_i64_s\n");
                } else if ("i32".equals(expectedType) && "f64".equals(castTarget)) {
                    collector.append("    i32.trunc_f64_s\n");
                } else if ("i32".equals(expectedType) && "f32".equals(castTarget)) {
                    collector.append("    i32.trunc_f32_s\n");
                } else if ("i32".equals(expectedType) && "i64".equals(castTarget)) {
                    collector.append("    i32.wrap_i64\n");
                } else if ("i64".equals(expectedType) && "i32".equals(castTarget)) {
                    collector.append("    i64.extend_i32_s\n");
                }
            }
        } else if (expr.isInstanceOfExpr()) {
            InstanceOfExpr ioe = expr.asInstanceOfExpr();
            String targetClassName = ioe.getType().asString();
            translateExpression(ioe.getExpression(), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    local.set $__obj_tmp\n");
            collector.append("    local.get $__obj_tmp\n");
            collector.append("    i32.eqz\n");
            collector.append("    if (result i32)\n");
            collector.append("      i32.const 0\n");
            collector.append("    else\n");
            collector.append("      local.get $__obj_tmp\n");
            collector.append("      i32.load offset=0\n");
            collector.append("      local.set $__vtable_tmp\n");

            List<ClassModel> matchingClasses = new ArrayList<>();
            if (classRegistry != null) {
                for (ClassModel cm : classRegistry.getClasses().values()) {
                    if (classRegistry.isSubclassOf(cm.getName(), targetClassName)) {
                        matchingClasses.add(cm);
                    }
                }
            }

            if (matchingClasses.isEmpty()) {
                collector.append("      i32.const 0\n");
            } else {
                collector.append("      i32.const 0\n");
                for (ClassModel mc : matchingClasses) {
                    collector.append("      local.get $__vtable_tmp\n");
                    collector.append("      i32.const ").append(mc.getVtableBase()).append("\n");
                    collector.append("      i32.eq\n");
                    collector.append("      i32.or\n");
                }
            }
            collector.append("    end\n");
        } else if (expr.isNameExpr()) {
            String var = expr.asNameExpr().getNameAsString();
            String activeEnum = switchEnumStack.peek();
            if (activeEnum != null && !activeEnum.isEmpty() && classRegistry != null) {
                ClassModel enumCm = classRegistry.getClass(activeEnum);
                if (enumCm != null && enumCm.getEnumConstants().contains(var)) {
                    collector.append("    global.get $").append(activeEnum).append("_").append(var).append("\n");
                    return;
                }
            }
            if (currentClass != null && currentClass.isEnum() && currentClass.getEnumConstants().contains(var)) {
                collector.append("    global.get $").append(currentClass.getName()).append("_").append(var).append("\n");
                return;
            }
            if (localJavaTypes.containsKey(var) && classRegistry != null && classRegistry.isRecord(localJavaTypes.get(var))) {
                RecordModel rm = classRegistry.getRecord(localJavaTypes.get(var));
                for (FieldModel c : rm.getComponents()) {
                    collector.append("    local.get $").append(var).append("$").append(c.getName()).append("\n");
                }
            } else if (localTypes.containsKey(var)) {
                collector.append("    local.get $").append(var).append("\n");
                if ("f64".equals(expectedType) && "i32".equals(localTypes.get(var))) {
                    collector.append("    f64.convert_i32_s\n");
                } else if ("f64".equals(expectedType) && "f32".equals(localTypes.get(var))) {
                    collector.append("    f64.promote_f32\n");
                } else if ("f64".equals(expectedType) && "i64".equals(localTypes.get(var))) {
                    collector.append("    f64.convert_i64_s\n");
                } else if ("f32".equals(expectedType) && "f64".equals(localTypes.get(var))) {
                    collector.append("    f32.demote_f64\n");
                } else if ("f32".equals(expectedType) && "i32".equals(localTypes.get(var))) {
                    collector.append("    f32.convert_i32_s\n");
                } else if ("f32".equals(expectedType) && "i64".equals(localTypes.get(var))) {
                    collector.append("    f32.convert_i64_s\n");
                } else if ("i64".equals(expectedType) && "i32".equals(localTypes.get(var))) {
                    collector.append("    i64.extend_i32_s\n");
                } else if ("i32".equals(expectedType) && "i64".equals(localTypes.get(var))) {
                    collector.append("    i32.wrap_i64\n");
                }
            } else if (localTypes.containsKey("this$" + var)) {
                collector.append("    local.get $this$").append(var).append("\n");
                if ("f64".equals(expectedType) && "i32".equals(localTypes.get("this$" + var))) {
                    collector.append("    f64.convert_i32_s\n");
                } else if ("f64".equals(expectedType) && "f32".equals(localTypes.get("this$" + var))) {
                    collector.append("    f64.promote_f32\n");
                } else if ("f64".equals(expectedType) && "i64".equals(localTypes.get("this$" + var))) {
                    collector.append("    f64.convert_i64_s\n");
                } else if ("f32".equals(expectedType) && "f64".equals(localTypes.get("this$" + var))) {
                    collector.append("    f32.demote_f64\n");
                } else if ("f32".equals(expectedType) && "i32".equals(localTypes.get("this$" + var))) {
                    collector.append("    f32.convert_i32_s\n");
                } else if ("f32".equals(expectedType) && "i64".equals(localTypes.get("this$" + var))) {
                    collector.append("    f32.convert_i64_s\n");
                } else if ("i64".equals(expectedType) && "i32".equals(localTypes.get("this$" + var))) {
                    collector.append("    i64.extend_i32_s\n");
                } else if ("i32".equals(expectedType) && "i64".equals(localTypes.get("this$" + var))) {
                    collector.append("    i32.wrap_i64\n");
                }
            } else if (currentClass != null && currentClass.getField(var) != null && !currentClass.getField(var).isStatic()) {
                FieldModel fm = currentClass.getField(var);
                collector.append("    local.get $this\n");
                collector.append("    ").append(getLoadInstruction(fm.getWasmType())).append(" offset=").append(fm.getOffset()).append("\n");
                if ("f64".equals(expectedType) && "i32".equals(fm.getWasmType())) {
                    collector.append("    f64.convert_i32_s\n");
                } else if ("f64".equals(expectedType) && "f32".equals(fm.getWasmType())) {
                    collector.append("    f64.promote_f32\n");
                } else if ("f64".equals(expectedType) && "i64".equals(fm.getWasmType())) {
                    collector.append("    f64.convert_i64_s\n");
                } else if ("f32".equals(expectedType) && "f64".equals(fm.getWasmType())) {
                    collector.append("    f32.demote_f64\n");
                } else if ("f32".equals(expectedType) && "i32".equals(fm.getWasmType())) {
                    collector.append("    f32.convert_i32_s\n");
                } else if ("f32".equals(expectedType) && "i64".equals(fm.getWasmType())) {
                    collector.append("    f32.convert_i64_s\n");
                } else if ("i64".equals(expectedType) && "i32".equals(fm.getWasmType())) {
                    collector.append("    i64.extend_i32_s\n");
                } else if ("i32".equals(expectedType) && "i64".equals(fm.getWasmType())) {
                    collector.append("    i32.wrap_i64\n");
                }
            } else if (globalVars != null && globalVars.containsKey(var)) {
                collector.append("    global.get $").append(var).append("\n");
                if ("f64".equals(expectedType) && "i32".equals(globalVars.get(var).type)) {
                    collector.append("    f64.convert_i32_s\n");
                } else if ("f64".equals(expectedType) && "f32".equals(globalVars.get(var).type)) {
                    collector.append("    f64.promote_f32\n");
                } else if ("f64".equals(expectedType) && "i64".equals(globalVars.get(var).type)) {
                    collector.append("    f64.convert_i64_s\n");
                } else if ("f32".equals(expectedType) && "f64".equals(globalVars.get(var).type)) {
                    collector.append("    f32.demote_f64\n");
                } else if ("f32".equals(expectedType) && "i32".equals(globalVars.get(var).type)) {
                    collector.append("    f32.convert_i32_s\n");
                } else if ("f32".equals(expectedType) && "i64".equals(globalVars.get(var).type)) {
                    collector.append("    f32.convert_i64_s\n");
                } else if ("i64".equals(expectedType) && "i32".equals(globalVars.get(var).type)) {
                    collector.append("    i64.extend_i32_s\n");
                } else if ("i32".equals(expectedType) && "i64".equals(globalVars.get(var).type)) {
                    collector.append("    i32.wrap_i64\n");
                }
            }
        } else if (expr.isEnclosedExpr()) {
            translateExpression(expr.asEnclosedExpr().getInner(), collector, localTypes, localElementTypes, localJavaTypes, expectedType);
        } else if (expr.isObjectCreationExpr()) {
            ObjectCreationExpr oce = expr.asObjectCreationExpr();
            String typeName = oce.getTypeAsString();
            if (typeName.equals("StringBuilder") || typeName.endsWith(".StringBuilder")) {
                if (oce.getArguments().isEmpty()) {
                    collector.append("    i32.const 16\n");
                    collector.append("    call $__sb_new\n");
                } else if (oce.getArguments().size() == 1) {
                    Expression arg = oce.getArgument(0);
                    if (isStringType(arg, localJavaTypes)) {
                        collector.append("    i32.const 16\n");
                        collector.append("    call $__sb_new\n");
                        collector.append("    local.set $__sb_tmp\n");
                        collector.append("    local.get $__sb_tmp\n");
                        translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "String");
                        collector.append("    call $__sb_append_str\n");
                        collector.append("    drop\n");
                        collector.append("    local.get $__sb_tmp\n");
                    } else {
                        translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        collector.append("    call $__sb_new\n");
                    }
                }
            } else if (typeName.equals("AtomicInteger") || typeName.endsWith(".AtomicInteger")) {
                collector.append("    i32.const 8\n");
                collector.append("    call $__heap_alloc\n");
                if (oce.getArguments().size() > 0) {
                    collector.append("    local.set $__atomic_tmp\n");
                    collector.append("    local.get $__atomic_tmp\n");
                    translateExpression(oce.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    collector.append("    i32.atomic.store offset=4\n");
                    collector.append("    local.get $__atomic_tmp\n");
                }
            } else if (classRegistry != null && classRegistry.isRecord(typeName)) {
                RecordModel rm = classRegistry.getRecord(typeName);
                for (int i = 0; i < oce.getArguments().size(); i++) {
                    String pWasm = i < rm.getComponents().size() ? rm.getComponents().get(i).getWasmType() : "i32";
                    translateExpression(oce.getArgument(i), collector, localTypes, localElementTypes, localJavaTypes, pWasm);
                }
            } else if (classRegistry != null && classRegistry.hasClass(typeName)) {
                ClassModel cm = classRegistry.getClass(typeName);
                collector.append("    i32.const ").append(cm.getStructSize()).append("\n");
                collector.append("    call $__heap_alloc\n");
                collector.append("    local.set $__obj_alloc_ptr\n");

                if (!cm.getVtableSlots().isEmpty()) {
                    collector.append("    local.get $__obj_alloc_ptr\n");
                    collector.append("    i32.const ").append(cm.getVtableBase()).append("\n");
                    collector.append("    i32.store offset=0\n");
                }

                collector.append("    local.get $__obj_alloc_ptr\n");
                String initFunc = emitConstructorArguments(typeName, oce.getArguments(), oce,
                        collector, localTypes, localElementTypes, localJavaTypes);
                collector.append("    call ").append(initFunc).append("\n");
                collector.append("    local.get $__obj_alloc_ptr\n");
            }
        } else if (expr.isArrayCreationExpr()) {
            ArrayCreationExpr ace = expr.asArrayCreationExpr();
            String elemType = TypeMapper.mapType(ace.getElementType());
            int headerOffset = (elemType.equals("f64") || elemType.equals("i64")) ? 8 : 4;
            int elemSize = (elemType.equals("f64") || elemType.equals("i64")) ? 8 : 4;

            if (ace.getInitializer().isPresent()) {
                ArrayInitializerExpr init = ace.getInitializer().get();
                int n = init.getValues().size();
                int totalBytes = headerOffset + n * elemSize;
                int alignedBytes = (totalBytes + 7) & ~7;

                collector.append("    global.get $__heap_ptr\n");
                collector.append("    local.set $__arr_alloc_ptr\n");
                collector.append("    global.get $__heap_ptr\n");
                collector.append("    i32.const ").append(alignedBytes).append("\n");
                collector.append("    i32.add\n");
                collector.append("    global.set $__heap_ptr\n");

                collector.append("    local.get $__arr_alloc_ptr\n");
                collector.append("    i32.const ").append(n).append("\n");
                collector.append("    i32.store\n");

                for (int i = 0; i < n; i++) {
                    collector.append("    local.get $__arr_alloc_ptr\n");
                    translateExpression(init.getValues().get(i), collector, localTypes, localElementTypes, localJavaTypes, elemType);
                    int offset = headerOffset + i * elemSize;
                    collector.append("    ").append(getStoreInstruction(elemType)).append(" offset=").append(offset).append("\n");
                }
                collector.append("    local.get $__arr_alloc_ptr\n");
            } else if (!ace.getLevels().isEmpty() && ace.getLevels().get(0).getDimension().isPresent()) {
                Expression dimExpr = ace.getLevels().get(0).getDimension().get();
                translateExpression(dimExpr, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    local.set $__arr_alloc_len\n");
                collector.append("    global.get $__heap_ptr\n");
                collector.append("    local.set $__arr_alloc_ptr\n");

                collector.append("    local.get $__arr_alloc_ptr\n");
                collector.append("    local.get $__arr_alloc_len\n");
                collector.append("    i32.store\n");

                collector.append("    global.get $__heap_ptr\n");
                collector.append("    i32.const ").append(headerOffset + 7).append("\n");
                collector.append("    local.get $__arr_alloc_len\n");
                collector.append("    i32.const ").append(elemSize).append("\n");
                collector.append("    i32.mul\n");
                collector.append("    i32.add\n");
                collector.append("    i32.const -8\n");
                collector.append("    i32.and\n");
                collector.append("    i32.add\n");
                collector.append("    global.set $__heap_ptr\n");

                collector.append("    local.get $__arr_alloc_ptr\n");
            }
        } else if (expr.isArrayInitializerExpr()) {
            ArrayInitializerExpr init = expr.asArrayInitializerExpr();
            String elemType = (expectedType != null && !expectedType.equals("i32")) ? expectedType : "i32";
            int headerOffset = (elemType.equals("f64") || elemType.equals("i64")) ? 8 : 4;
            int elemSize = (elemType.equals("f64") || elemType.equals("i64")) ? 8 : 4;

            int n = init.getValues().size();
            int totalBytes = headerOffset + n * elemSize;
            int alignedBytes = (totalBytes + 7) & ~7;

            collector.append("    global.get $__heap_ptr\n");
            collector.append("    local.set $__arr_alloc_ptr\n");
            collector.append("    global.get $__heap_ptr\n");
            collector.append("    i32.const ").append(alignedBytes).append("\n");
            collector.append("    i32.add\n");
            collector.append("    global.set $__heap_ptr\n");

            collector.append("    local.get $__arr_alloc_ptr\n");
            collector.append("    i32.const ").append(n).append("\n");
            collector.append("    i32.store\n");

            for (int i = 0; i < n; i++) {
                collector.append("    local.get $__arr_alloc_ptr\n");
                translateExpression(init.getValues().get(i), collector, localTypes, localElementTypes, localJavaTypes, elemType);
                int offset = headerOffset + i * elemSize;
                collector.append("    ").append(getStoreInstruction(elemType)).append(" offset=").append(offset).append("\n");
            }
            collector.append("    local.get $__arr_alloc_ptr\n");
        } else if (expr.isArrayAccessExpr()) {
            ArrayAccessExpr aae = expr.asArrayAccessExpr();
            String elemType = getArrayElementType(aae, localElementTypes, expectedType);
            int headerOffset = (elemType.equals("f64") || elemType.equals("i64")) ? 8 : 4;
            int elemSize = (elemType.equals("f64") || elemType.equals("i64")) ? 8 : 4;

            translateExpression(aae.getName(), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            translateExpression(aae.getIndex(), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    i32.const ").append(elemSize).append("\n");
            collector.append("    i32.mul\n");
            collector.append("    i32.add\n");
            collector.append("    ").append(getLoadInstruction(elemType)).append(" offset=").append(headerOffset).append("\n");
        } else if (expr.isFieldAccessExpr()) {
            FieldAccessExpr fae = expr.asFieldAccessExpr();
            String fName = fae.getNameAsString();
            if (fName.equals("length")) {
                translateExpression(fae.getScope(), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    i32.load\n");
            } else if (fae.getScope().isNameExpr() && classRegistry != null && classRegistry.hasClass(fae.getScope().asNameExpr().getNameAsString()) && classRegistry.getClass(fae.getScope().asNameExpr().getNameAsString()).isEnum() && classRegistry.getClass(fae.getScope().asNameExpr().getNameAsString()).getEnumConstants().contains(fName)) {
                String enumName = fae.getScope().asNameExpr().getNameAsString();
                collector.append("    global.get $").append(enumName).append("_").append(fName).append("\n");
            } else if (fae.getScope().isNameExpr() && isStandardConstantScope(fae.getScope().asNameExpr().getNameAsString())) {
                String scopeName = fae.getScope().asNameExpr().getNameAsString();
                String shortScope = scopeName.contains(".") ? scopeName.substring(scopeName.lastIndexOf('.') + 1) : scopeName;
                emitStandardConstant(shortScope, fName, collector);
            } else if (fae.getScope().isNameExpr() && (fae.getScope().asNameExpr().getNameAsString().equals("Memory") || fae.getScope().asNameExpr().getNameAsString().endsWith(".Memory"))) {
                int val = switch (fName) {
                    case "HEAP" -> 1;
                    case "STACK" -> 2;
                    default -> 0; // DEFAULT or STRINGS
                };
                collector.append("    i32.const ").append(val).append("\n");
            } else if (fae.getScope().isNameExpr() && fae.getScope().asNameExpr().getNameAsString().equals("WebGLRenderingContext")) {
                int val = switch (fName) {
                    case "COLOR_BUFFER_BIT" -> 0x00004000;
                    case "DEPTH_BUFFER_BIT" -> 0x00000100;
                    case "STENCIL_BUFFER_BIT" -> 0x00000400;
                    case "DEPTH_TEST" -> 0x0B71;
                    case "BLEND" -> 0x0BE2;
                    case "CULL_FACE" -> 0x0B44;
                    default -> 0;
                };
                collector.append("    i32.const ").append(val).append("\n");
            } else if (fae.getScope().isNameExpr() && (fae.getScope().asNameExpr().getNameAsString().equals("Level") || fae.getScope().asNameExpr().getNameAsString().endsWith(".Level"))) {
                int val = switch (fName) {
                    case "OFF" -> 0;
                    case "FATAL" -> 100;
                    case "ERROR" -> 200;
                    case "WARN" -> 300;
                    case "INFO" -> 400;
                    case "DEBUG" -> 500;
                    case "TRACE" -> 600;
                    case "ALL" -> Integer.MAX_VALUE;
                    default -> 400;
                };
                collector.append("    i32.const ").append(val).append("\n");
            } else if (fae.getScope().isNameExpr() && (fae.getScope().asNameExpr().getNameAsString().equals("WASITerminal") || fae.getScope().asNameExpr().getNameAsString().endsWith(".WASITerminal") || fae.getScope().asNameExpr().getNameAsString().equals("Terminal") || fae.getScope().asNameExpr().getNameAsString().endsWith(".Terminal"))) {
                String ansi = switch (fName) {
                    case "RESET" -> "\u001B[0m";
                    case "BLACK" -> "\u001B[30m";
                    case "RED" -> "\u001B[31m";
                    case "GREEN" -> "\u001B[32m";
                    case "YELLOW" -> "\u001B[33m";
                    case "BLUE" -> "\u001B[34m";
                    case "MAGENTA" -> "\u001B[35m";
                    case "CYAN" -> "\u001B[36m";
                    case "WHITE" -> "\u001B[37m";
                    case "BRIGHT_BLACK" -> "\u001B[90m";
                    case "BRIGHT_RED" -> "\u001B[91m";
                    case "BRIGHT_GREEN" -> "\u001B[92m";
                    case "BRIGHT_YELLOW" -> "\u001B[93m";
                    case "BRIGHT_BLUE" -> "\u001B[94m";
                    case "BRIGHT_MAGENTA" -> "\u001B[95m";
                    case "BRIGHT_CYAN" -> "\u001B[96m";
                    case "BRIGHT_WHITE" -> "\u001B[97m";
                    case "BOLD" -> "\u001B[1m";
                    case "DIM" -> "\u001B[2m";
                    case "ITALIC" -> "\u001B[3m";
                    case "UNDERLINE" -> "\u001B[4m";
                    case "REVERSE" -> "\u001B[7m";
                    default -> "";
                };
                StringConstant sc = stringPool != null ? stringPool.get(ansi) : null;
                int offset = sc != null ? sc.offset : 0;
                collector.append("    i32.const ").append(offset).append("\n");
            } else {
                String targetType = getExpressionJavaType(fae.getScope(), localJavaTypes);
                if (targetType != null && classRegistry != null && classRegistry.isRecord(targetType)) {
                    if (fae.getScope().isThisExpr()) {
                        collector.append("    local.get $this$").append(fName).append("\n");
                    } else if (fae.getScope().isNameExpr()) {
                        String scopeVar = fae.getScope().asNameExpr().getNameAsString();
                        collector.append("    local.get $").append(scopeVar).append("$").append(fName).append("\n");
                    }
                } else {
                    FieldModel fm = null;
                    if (targetType != null && classRegistry != null && classRegistry.hasClass(targetType)) {
                        fm = classRegistry.getClass(targetType).getField(fName);
                    } else if (currentClass != null && currentClass.getField(fName) != null) {
                        fm = currentClass.getField(fName);
                    }
                    if (fm != null) {
                        translateExpression(fae.getScope(), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        collector.append("    ").append(getLoadInstruction(fm.getWasmType())).append(" offset=").append(fm.getOffset()).append("\n");
                        emitNumericWidening(fm.getWasmType(), expectedType, collector);
                    }
                }
            }
        } else if (expr.isSwitchExpr()) {
            SwitchExpr switchExpr = expr.asSwitchExpr();
            int switchNum = ++switchExprCounter;
            int switchId = ++loopCounter;
            String breakLabel = "break_switchexpr_" + switchId;
            String valVar = "__switchexpr_val_" + switchNum;
            String exprType = inferType(switchExpr, localTypes, localElementTypes, localJavaTypes, expectedType);

            String selectorType = getExpressionJavaType(switchExpr.getSelector(), localJavaTypes);
            ClassModel enumModel = (selectorType != null && classRegistry != null) ? classRegistry.getClass(selectorType) : null;
            if (enumModel != null && !enumModel.isEnum()) {
                enumModel = null;
            }
            switchEnumStack.push(enumModel != null ? enumModel.getName() : "");

            try {
                translateExpression(switchExpr.getSelector(), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    local.set $").append(valVar).append("\n");

                collector.append("    (block $").append(breakLabel).append(" (result ").append(exprType).append(")\n");
                List<SwitchEntry> entries = switchExpr.getEntries();
                int numEntries = entries.size();
                for (int i = numEntries - 1; i >= 0; i--) {
                    collector.append("      (block $case_expr_").append(switchId).append("_").append(i).append("\n");
                }
                for (int i = 0; i < numEntries; i++) {
                    SwitchEntry entry = entries.get(i);
                    if (!entry.getLabels().isEmpty()) {
                        for (Expression lbl : entry.getLabels()) {
                            collector.append("        local.get $").append(valVar).append("\n");
                            translateExpression(lbl, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                            collector.append("        i32.eq\n");
                            collector.append("        br_if $case_expr_").append(switchId).append("_").append(i).append("\n");
                        }
                    } else {
                        collector.append("        br $case_expr_").append(switchId).append("_").append(i).append("\n");
                    }
                }
                collector.append("        ").append(getWasmZeroConst(exprType)).append("\n");
                collector.append("        br $").append(breakLabel).append("\n");

                for (int i = 0; i < numEntries; i++) {
                    SwitchEntry entry = entries.get(i);
                    collector.append("      ) ;; end block $case_expr_").append(switchId).append("_").append(i).append("\n");
                    for (Statement s : entry.getStatements()) {
                        if (s.isYieldStmt()) {
                            YieldStmt ys = s.asYieldStmt();
                            translateExpression(ys.getExpression(), collector, localTypes, localElementTypes, localJavaTypes, exprType);
                            collector.append("        br $").append(breakLabel).append("\n");
                        } else if (s.isExpressionStmt()) {
                            translateExpression(s.asExpressionStmt().getExpression(), collector, localTypes, localElementTypes, localJavaTypes, exprType);
                            collector.append("        br $").append(breakLabel).append("\n");
                        } else {
                            translateStatement(s, collector, localTypes, localElementTypes, localJavaTypes, exprType);
                        }
                    }
                }
                collector.append("    )\n");
            } finally {
                switchEnumStack.pop();
            }
        } else if (expr.isAssignExpr()) {
            AssignExpr assign = expr.asAssignExpr();
            AssignExpr.Operator op = assign.getOperator();
            if (assign.getTarget().isNameExpr()) {
                String target = assign.getTarget().asNameExpr().getNameAsString();
                boolean isGlobal = globalVars != null && globalVars.containsKey(target) && !localTypes.containsKey(target);
                boolean isInstanceField = currentClass != null && currentClass.getField(target) != null && !localTypes.containsKey(target) && !isGlobal;

                if (localJavaTypes.containsKey(target) && classRegistry != null && classRegistry.isRecord(localJavaTypes.get(target))) {
                    RecordModel rm = classRegistry.getRecord(localJavaTypes.get(target));
                    if (assign.getValue().isObjectCreationExpr()) {
                        ObjectCreationExpr oce = assign.getValue().asObjectCreationExpr();
                        for (int i = 0; i < oce.getArguments().size(); i++) {
                            String compWasm = i < rm.getComponents().size() ? rm.getComponents().get(i).getWasmType() : "i32";
                            translateExpression(oce.getArgument(i), collector, localTypes, localElementTypes, localJavaTypes, compWasm);
                            String compName = i < rm.getComponents().size() ? rm.getComponents().get(i).getName() : ("comp" + i);
                            collector.append("    local.set $").append(target).append("$").append(compName).append("\n");
                        }
                    } else if (assign.getValue().isNameExpr() && localJavaTypes.containsKey(assign.getValue().asNameExpr().getNameAsString())) {
                        String otherVar = assign.getValue().asNameExpr().getNameAsString();
                        for (FieldModel c : rm.getComponents()) {
                            collector.append("    local.get $").append(otherVar).append("$").append(c.getName()).append("\n");
                            collector.append("    local.set $").append(target).append("$").append(c.getName()).append("\n");
                        }
                    } else {
                        translateExpression(assign.getValue(), collector, localTypes, localElementTypes, localJavaTypes, null);
                        List<FieldModel> comps = rm.getComponents();
                        for (int i = comps.size() - 1; i >= 0; i--) {
                            collector.append("    local.set $").append(target).append("$").append(comps.get(i).getName()).append("\n");
                        }
                    }
                } else if (isInstanceField) {
                    FieldModel fm = currentClass.getField(target);
                    if (op == AssignExpr.Operator.ASSIGN) {
                        collector.append("    local.get $this\n");
                        translateExpression(assign.getValue(), collector, localTypes, localElementTypes, localJavaTypes, fm.getWasmType());
                        collector.append("    ").append(getStoreInstruction(fm.getWasmType())).append(" offset=").append(fm.getOffset()).append("\n");
                    } else {
                        collector.append("    local.get $this\n");
                        collector.append("    local.get $this\n");
                        collector.append("    ").append(getLoadInstruction(fm.getWasmType())).append(" offset=").append(fm.getOffset()).append("\n");
                        translateExpression(assign.getValue(), collector, localTypes, localElementTypes, localJavaTypes, fm.getWasmType());
                        BinaryExpr.Operator binOp = mapAssignOpToBinaryOp(op);
                        if (binOp != null) {
                            collector.append("    ").append(getWasmBinaryOp(binOp, fm.getWasmType())).append("\n");
                        }
                        collector.append("    ").append(getStoreInstruction(fm.getWasmType())).append(" offset=").append(fm.getOffset()).append("\n");
                    }
                } else {
                    String targetType = localTypes.getOrDefault(target, isGlobal ? globalVars.get(target).type : "i32");
                    if (op == AssignExpr.Operator.ASSIGN) {
                        translateExpression(assign.getValue(), collector, localTypes, localElementTypes, localJavaTypes, targetType);
                        collector.append(isGlobal ? "    global.set $" : "    local.set $").append(target).append("\n");
                    } else if (op == AssignExpr.Operator.PLUS && isStringType(assign.getValue(), localJavaTypes)) {
                        collector.append(isGlobal ? "    global.get $" : "    local.get $").append(target).append("\n");
                        translateStringToStack(assign.getValue(), collector, localTypes, localElementTypes, localJavaTypes);
                        collector.append("    call $__str_concat\n");
                        collector.append(isGlobal ? "    global.set $" : "    local.set $").append(target).append("\n");
                    } else {
                        collector.append(isGlobal ? "    global.get $" : "    local.get $").append(target).append("\n");
                        translateExpression(assign.getValue(), collector, localTypes, localElementTypes, localJavaTypes, targetType);
                        BinaryExpr.Operator binOp = mapAssignOpToBinaryOp(op);
                        if (binOp != null) {
                            collector.append("    ").append(getWasmBinaryOp(binOp, targetType)).append("\n");
                        }
                        collector.append(isGlobal ? "    global.set $" : "    local.set $").append(target).append("\n");
                    }
                }
            } else if (assign.getTarget().isFieldAccessExpr()) {
                FieldAccessExpr fae = assign.getTarget().asFieldAccessExpr();
                String fName = fae.getNameAsString();
                String targetType = getExpressionJavaType(fae.getScope(), localJavaTypes);
                FieldModel fm = null;
                if (targetType != null && classRegistry != null && classRegistry.hasClass(targetType)) {
                    fm = classRegistry.getClass(targetType).getField(fName);
                } else if (currentClass != null && currentClass.getField(fName) != null) {
                    fm = currentClass.getField(fName);
                }
                if (fm != null) {
                    if (op == AssignExpr.Operator.ASSIGN) {
                        translateExpression(fae.getScope(), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        translateExpression(assign.getValue(), collector, localTypes, localElementTypes, localJavaTypes, fm.getWasmType());
                        collector.append("    ").append(getStoreInstruction(fm.getWasmType())).append(" offset=").append(fm.getOffset()).append("\n");
                    } else {
                        translateExpression(fae.getScope(), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        collector.append("    local.set $__obj_tmp\n");
                        collector.append("    local.get $__obj_tmp\n");
                        collector.append("    local.get $__obj_tmp\n");
                        collector.append("    ").append(getLoadInstruction(fm.getWasmType())).append(" offset=").append(fm.getOffset()).append("\n");
                        translateExpression(assign.getValue(), collector, localTypes, localElementTypes, localJavaTypes, fm.getWasmType());
                        BinaryExpr.Operator binOp = mapAssignOpToBinaryOp(op);
                        if (binOp != null) {
                            collector.append("    ").append(getWasmBinaryOp(binOp, fm.getWasmType())).append("\n");
                        }
                        collector.append("    ").append(getStoreInstruction(fm.getWasmType())).append(" offset=").append(fm.getOffset()).append("\n");
                    }
                }
            } else if (assign.getTarget().isArrayAccessExpr()) {
                ArrayAccessExpr aae = assign.getTarget().asArrayAccessExpr();
                String elemType = getArrayElementType(aae, localElementTypes, expectedType);
                int headerOffset = (elemType.equals("f64") || elemType.equals("i64")) ? 8 : 4;
                int elemSize = (elemType.equals("f64") || elemType.equals("i64")) ? 8 : 4;

                if (op == AssignExpr.Operator.ASSIGN) {
                    translateExpression(aae.getName(), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    translateExpression(aae.getIndex(), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    collector.append("    i32.const ").append(elemSize).append("\n");
                    collector.append("    i32.mul\n");
                    collector.append("    i32.add\n");
                    translateExpression(assign.getValue(), collector, localTypes, localElementTypes, localJavaTypes, elemType);
                    collector.append("    ").append(getStoreInstruction(elemType)).append(" offset=").append(headerOffset).append("\n");
                } else {
                    translateExpression(aae.getName(), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    translateExpression(aae.getIndex(), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    collector.append("    i32.const ").append(elemSize).append("\n");
                    collector.append("    i32.mul\n");
                    collector.append("    i32.add\n");
                    collector.append("    local.set $__arr_alloc_ptr\n");

                    collector.append("    local.get $__arr_alloc_ptr\n");
                    collector.append("    local.get $__arr_alloc_ptr\n");
                    collector.append("    ").append(getLoadInstruction(elemType)).append(" offset=").append(headerOffset).append("\n");
                    translateExpression(assign.getValue(), collector, localTypes, localElementTypes, localJavaTypes, elemType);
                    BinaryExpr.Operator binOp = mapAssignOpToBinaryOp(op);
                    if (binOp != null) {
                        collector.append("    ").append(getWasmBinaryOp(binOp, elemType)).append("\n");
                    }
                    collector.append("    ").append(getStoreInstruction(elemType)).append(" offset=").append(headerOffset).append("\n");
                }
            } else {
                translateExpression(assign.getValue(), collector, localTypes, localElementTypes, localJavaTypes, expectedType);
            }
        } else if (expr.isVariableDeclarationExpr()) {
            VariableDeclarationExpr vde = expr.asVariableDeclarationExpr();
            for (VariableDeclarator vd : vde.getVariables()) {
                String vTypeName = localJavaTypes.getOrDefault(vd.getNameAsString(), vd.getType().asString());
                if (classRegistry != null && classRegistry.isRecord(vTypeName)) {
                    RecordModel rm = classRegistry.getRecord(vTypeName);
                    if (vd.getInitializer().isPresent()) {
                        Expression init = vd.getInitializer().get();
                        if (init.isObjectCreationExpr()) {
                            ObjectCreationExpr oce = init.asObjectCreationExpr();
                            for (int i = 0; i < oce.getArguments().size(); i++) {
                                String compWasm = i < rm.getComponents().size() ? rm.getComponents().get(i).getWasmType() : "i32";
                                translateExpression(oce.getArgument(i), collector, localTypes, localElementTypes, localJavaTypes, compWasm);
                                String compName = i < rm.getComponents().size() ? rm.getComponents().get(i).getName() : ("comp" + i);
                                collector.append("    local.set $").append(vd.getNameAsString()).append("$").append(compName).append("\n");
                            }
                        } else if (init.isNameExpr() && localJavaTypes.containsKey(init.asNameExpr().getNameAsString())) {
                            String otherVar = init.asNameExpr().getNameAsString();
                            for (FieldModel c : rm.getComponents()) {
                                collector.append("    local.get $").append(otherVar).append("$").append(c.getName()).append("\n");
                                collector.append("    local.set $").append(vd.getNameAsString()).append("$").append(c.getName()).append("\n");
                            }
                        } else {
                            translateExpression(init, collector, localTypes, localElementTypes, localJavaTypes, null);
                            List<FieldModel> comps = rm.getComponents();
                            for (int i = comps.size() - 1; i >= 0; i--) {
                                collector.append("    local.set $").append(vd.getNameAsString()).append("$").append(comps.get(i).getName()).append("\n");
                            }
                        }
                    }
                } else if (vd.getInitializer().isPresent()) {
                    String vType = localTypes.getOrDefault(vd.getNameAsString(), TypeMapper.mapType(vd.getType()));
                    String elemType = localElementTypes.get(vd.getNameAsString());
                    translateExpression(vd.getInitializer().get(), collector, localTypes, localElementTypes, localJavaTypes, elemType != null ? elemType : vType);
                    collector.append("    local.set $").append(vd.getNameAsString()).append("\n");
                }
            }
        } else if (expr.isMethodCallExpr()) {
            MethodCallExpr call = expr.asMethodCallExpr();
            String mName = call.getNameAsString();
            MethodModel sourceMethod = sourceMethod(call, localJavaTypes);
            if (sourceMethod != null) {
                emitSourceMethod(call, sourceMethod, collector, localTypes, localElementTypes, localJavaTypes, expectedType);
                return;
            }
            if (isSystemOrWasiCall(call, localJavaTypes)) {
                translateSystemOrWasiCall(call, collector, localTypes, localElementTypes, localJavaTypes, expectedType);
            } else if (isLog4jCall(call, localJavaTypes)) {
                translateLog4jCall(call, collector, localTypes, localElementTypes, localJavaTypes, expectedType);
            } else if (isArrayUtilsCall(call)) {
                translateArrayUtilsCall(call, collector, localTypes, localElementTypes, localJavaTypes);
            } else if (isMathCall(call)) {
                translateMathCall(call, collector, localTypes, localElementTypes, localJavaTypes);
            } else if (isWrapperCall(call, localJavaTypes)) {
                translateWrapperCall(call, collector, localTypes, localElementTypes, localJavaTypes, expectedType);
            } else if (isSimdCall(call, localJavaTypes)) {
                translateSimdCall(call, collector, localTypes, localElementTypes, localJavaTypes, expectedType);
            } else if (isMemoryCall(call, localJavaTypes)) {
                translateMemoryCall(call, collector, localTypes, localElementTypes, localJavaTypes, expectedType);
            } else if (call.getScope().isPresent()) {
                Expression scope = call.getScope().get();
                String scopeStr = scope.toString();
                String scopeJavaType = getExpressionJavaType(scope, localJavaTypes);
                String shortScope = scopeStr.contains(".") ? scopeStr.substring(scopeStr.lastIndexOf('.') + 1) : scopeStr;
                String shortType = (scopeJavaType != null && scopeJavaType.contains(".")) ? scopeJavaType.substring(scopeJavaType.lastIndexOf('.') + 1) : scopeJavaType;
                String snippetKey = (jsSnippets != null && jsSnippets.containsKey("js_inline_" + shortScope + "_" + mName)) ?
                        "js_inline_" + shortScope + "_" + mName :
                        (shortType != null && jsSnippets != null && jsSnippets.containsKey("js_inline_" + shortType + "_" + mName)) ?
                                "js_inline_" + shortType + "_" + mName : null;

                if (snippetKey != null) {
                    var snippetModel = jsSnippets.get(snippetKey);
                    if (!snippetModel.isStatic()) {
                        translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    }
                    for (int i = 0; i < call.getArguments().size(); i++) {
                        int pIdx = snippetModel.isStatic() ? i : i + 1;
                        String pWasm = pIdx < snippetModel.getParameters().size() ? snippetModel.getParameters().get(pIdx).getWasmType() : "i32";
                        String pJava = pIdx < snippetModel.getParameters().size() ? snippetModel.getParameters().get(pIdx).getJavaType() : "";
                        Expression arg = call.getArgument(i);
                        if ("String".equals(pJava) || "java.lang.String".equals(pJava)) {
                            translateDomStringArg(arg, collector, localTypes, localElementTypes, localJavaTypes);
                        } else {
                            translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, pWasm);
                        }
                    }
                    collector.append("    call $dom_").append(snippetKey).append("\n");
                } else if (scopeStr.equals("DOM") || scopeStr.endsWith(".DOM") || scopeStr.equals("Window") || scopeStr.endsWith(".Window") || scopeStr.equals("JSObject") || scopeStr.endsWith(".JSObject") || scopeStr.equals("JSResource") || scopeStr.endsWith(".JSResource") || DomHelper.isJsValueClass(scopeStr)) {
                    translateDomOrWindowStaticCall(call, collector, localTypes, localElementTypes, localJavaTypes);
                } else if (scopeStr.equals("Atomics") || scopeStr.endsWith(".Atomics")) {
                    translateAtomicsStaticCall(call, collector, localTypes, localElementTypes, localJavaTypes);
                } else if (scopeStr.equals("WasmThread") || scopeStr.endsWith(".WasmThread") || scopeStr.equals("Thread") || scopeStr.endsWith(".Thread")) {
                    translateThreadStaticCall(call, collector, localTypes, localElementTypes, localJavaTypes);
                } else if (scopeStr.equals("Class") || scopeStr.endsWith(".Class")) {
                    translateClassStaticCall(call, collector, localTypes, localElementTypes, localJavaTypes);
                } else if ("AtomicInteger".equals(scopeJavaType) || "AtomicInteger".equals(scopeStr) || (scopeJavaType != null && scopeJavaType.endsWith(".AtomicInteger"))) {
                    translateAtomicIntegerInstanceCall(call, collector, localTypes, localElementTypes, localJavaTypes);
                } else if (DomHelper.isJsValueClass(scopeJavaType) || DomHelper.isJsValueClass(scopeStr)) {
                    translateJsInstanceCall(call, collector, localTypes, localElementTypes, localJavaTypes);
                } else if (DomHelper.isDomInstanceMethod(mName) && !(scope.isNameExpr() && classRegistry != null && classRegistry.hasClass(scope.asNameExpr().getNameAsString())) && (scopeJavaType == null || classRegistry == null || !classRegistry.hasClass(scopeJavaType) || classRegistry.getClass(scopeJavaType).getMethodByName(mName) == null)) {
                    translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    if (mName.equals("getClassList")) {
                        // Leaves element handle on stack as ClassList handle
                    } else if (mName.equals("getContext2D")) {
                        collector.append("    call $dom_getContext2D\n");
                    } else if (mName.equals("getWebGLContext")) {
                        collector.append("    call $dom_getWebGLContext\n");
                    } else if (mName.equals("getContext")) {
                        translateDomStringArg(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes);
                        collector.append("    call $dom_getContext\n");
                    } else if (mName.equals("appendChild") || mName.equals("removeChild")) {
                        translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        collector.append("    call $dom_").append(mName).append("\n");
                    } else if (mName.equals("remove") || mName.equals("getInnerText") || mName.equals("getParent") ||
                            mName.equals("getValue") || mName.equals("isChecked") || mName.equals("getWidth") ||
                            mName.equals("getHeight") || mName.equals("getFillStyle") || mName.equals("getStrokeStyle") ||
                            mName.equals("getLineWidth") || mName.equals("getFont") || mName.equals("getTextAlign") ||
                            mName.equals("getGlobalAlpha") || mName.equals("beginPath") || mName.equals("closePath") ||
                            mName.equals("fill") || mName.equals("stroke") || mName.equals("save") ||
                            mName.equals("restore") || mName.equals("resetTransform")) {
                        collector.append("    call $dom_").append(mName).append("\n");
                    } else if (mName.equals("setChecked") || mName.equals("setWidth") || mName.equals("setHeight") ||
                            mName.equals("clear") || mName.equals("enable") || mName.equals("disable")) {
                        if (!call.getArguments().isEmpty()) {
                            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        } else {
                            collector.append("    i32.const 0\n");
                        }
                        if (mName.equals("clear") || mName.equals("enable") || mName.equals("disable")) {
                            collector.append("    call $dom_webgl_").append(mName).append("\n");
                        } else {
                            collector.append("    call $dom_").append(mName).append("\n");
                        }
                    } else if (mName.equals("fillRect") || mName.equals("strokeRect") || mName.equals("clearRect") || mName.equals("rect")) {
                        for (Expression arg : call.getArguments()) {
                            translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "f64");
                        }
                        collector.append("    call $dom_").append(mName).append("\n");
                    } else if (mName.equals("moveTo") || mName.equals("lineTo") || mName.equals("translate") || mName.equals("scale")) {
                        translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "f64");
                        translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "f64");
                        collector.append("    call $dom_").append(mName).append("\n");
                    } else if (mName.equals("rotate") || mName.equals("setLineWidth") || mName.equals("setGlobalAlpha")) {
                        translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "f64");
                        collector.append("    call $dom_").append(mName).append("\n");
                    } else if (mName.equals("arc")) {
                        translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "f64");
                        translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "f64");
                        translateExpression(call.getArgument(2), collector, localTypes, localElementTypes, localJavaTypes, "f64");
                        translateExpression(call.getArgument(3), collector, localTypes, localElementTypes, localJavaTypes, "f64");
                        translateExpression(call.getArgument(4), collector, localTypes, localElementTypes, localJavaTypes, "f64");
                        if (call.getArguments().size() > 5) {
                            translateExpression(call.getArgument(5), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        } else {
                            collector.append("    i32.const 0\n");
                        }
                        collector.append("    call $dom_arc\n");
                    } else if (mName.equals("ellipse")) {
                        for (Expression arg : call.getArguments()) {
                            translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "f64");
                        }
                        collector.append("    call $dom_ellipse\n");
                    } else if (mName.equals("fillText") || mName.equals("strokeText")) {
                        translateDomStringArg(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes);
                        translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "f64");
                        translateExpression(call.getArgument(2), collector, localTypes, localElementTypes, localJavaTypes, "f64");
                        collector.append("    call $dom_").append(mName).append("\n");
                    } else if (mName.equals("setFillStyle") || mName.equals("setStrokeStyle") || mName.equals("setFont") || mName.equals("setTextAlign")) {
                        translateDomStringArg(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes);
                        collector.append("    call $dom_").append(mName).append("\n");
                    } else if (mName.equals("drawImage")) {
                        translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        if (call.getArguments().size() == 3) {
                            translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "f64");
                            translateExpression(call.getArgument(2), collector, localTypes, localElementTypes, localJavaTypes, "f64");
                            collector.append("    f64.const 0.0\n    f64.const 0.0\n");
                            collector.append("    call $dom_drawImage\n");
                        } else if (call.getArguments().size() == 5) {
                            translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "f64");
                            translateExpression(call.getArgument(2), collector, localTypes, localElementTypes, localJavaTypes, "f64");
                            translateExpression(call.getArgument(3), collector, localTypes, localElementTypes, localJavaTypes, "f64");
                            translateExpression(call.getArgument(4), collector, localTypes, localElementTypes, localJavaTypes, "f64");
                            collector.append("    call $dom_drawImage\n");
                        } else if (call.getArguments().size() >= 9) {
                            for (int i = 1; i <= 8; i++) {
                                translateExpression(call.getArgument(i), collector, localTypes, localElementTypes, localJavaTypes, "f64");
                            }
                            collector.append("    call $dom_drawImageFull\n");
                        }
                    } else if (mName.equals("clearColor")) {
                        for (Expression arg : call.getArguments()) {
                            translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "f64");
                        }
                        collector.append("    call $dom_webgl_clearColor\n");
                    } else if (mName.equals("viewport")) {
                        for (Expression arg : call.getArguments()) {
                            translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        }
                        collector.append("    call $dom_webgl_viewport\n");
                    } else {
                        for (Expression arg : call.getArguments()) {
                            translateDomStringArg(arg, collector, localTypes, localElementTypes, localJavaTypes);
                        }
                        collector.append("    call $dom_").append(mName).append("\n");
                    }
                } else if ((mName.equals("add") || mName.equals("remove") || mName.equals("contains") || mName.equals("toggle")) &&
                        (scope.toString().contains("classList") || scope.toString().contains("ClassList") || "ClassList".equals(getExpressionJavaType(scope, localJavaTypes)))) {
                    translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    for (Expression arg : call.getArguments()) {
                        translateDomStringArg(arg, collector, localTypes, localElementTypes, localJavaTypes);
                    }
                    if (mName.equals("add")) collector.append("    call $dom_addClass\n");
                    else if (mName.equals("remove")) collector.append("    call $dom_removeClass\n");
                    else if (mName.equals("contains")) collector.append("    call $dom_hasClass\n");
                    else if (mName.equals("toggle")) collector.append("    call $dom_toggleClass\n");
                } else if (mName.equals("length")) {
                    String scopeJType = getExpressionJavaType(scope, localJavaTypes);
                    if ("JSObject".equals(scopeJType) || "JSString".equals(scopeJType) || "JSValue".equals(scopeJType)) {
                        translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        collector.append("    call $dom_js_length\n");
                    } else if (isStringBuilderType(scope, localJavaTypes)) {
                        translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        collector.append("    call $__sb_length\n");
                    } else if (scope.isNameExpr() && localElementTypes.containsKey(scope.asNameExpr().getNameAsString())) {
                        translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        collector.append("    i32.load\n");
                    } else {
                        translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "String");
                        collector.append("    call $__str_length\n");
                    }
                } else if (mName.equals("charAt")) {
                    if (isStringBuilderType(scope, localJavaTypes)) {
                        translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        collector.append("    call $__sb_charAt\n");
                    } else {
                        translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "String");
                        translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        collector.append("    call $__str_charAt\n");
                    }
                } else if (mName.equals("setCharAt")) {
                    translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    collector.append("    call $__sb_setCharAt\n");
                } else if (mName.equals("substring") || mName.equals("subSequence")) {
                    if (isStringBuilderType(scope, localJavaTypes)) {
                        translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        if (call.getArguments().size() > 1) {
                            translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        } else {
                            collector.append("    local.set $__str_sub_start\n");
                            translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                            collector.append("    local.set $__sb_tmp\n");
                            collector.append("    local.get $__sb_tmp\n");
                            collector.append("    local.get $__str_sub_start\n");
                            collector.append("    local.get $__sb_tmp\n");
                            collector.append("    call $__sb_length\n");
                        }
                        collector.append("    call $__sb_substring\n");
                    } else {
                        translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "String");
                        translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        if (call.getArguments().size() > 1) {
                            translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                            collector.append("    call $__str_substring\n");
                        } else {
                            collector.append("    local.set $__str_sub_start\n");
                            translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "String");
                            collector.append("    local.set $__str_tmp\n");
                            collector.append("    local.get $__str_tmp\n");
                            collector.append("    local.get $__str_sub_start\n");
                            collector.append("    local.get $__str_tmp\n");
                            collector.append("    call $__str_length\n");
                            collector.append("    call $__str_substring\n");
                        }
                    }
                } else if (mName.equals("equals") && isStringType(scope, localJavaTypes)) {
                    translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "String");
                    translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "String");
                    collector.append("    call $__str_equals\n");
                } else if (mName.equals("equalsIgnoreCase")) {
                    translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "String");
                    translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "String");
                    collector.append("    call $__str_equalsIgnoreCase\n");
                } else if (mName.equals("compareTo") && isStringType(scope, localJavaTypes)) {
                    translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "String");
                    translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "String");
                    collector.append("    call $__str_compareTo\n");
                } else if (mName.equals("compareToIgnoreCase")) {
                    translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "String");
                    collector.append("    call $__str_toLowerCase\n");
                    translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "String");
                    collector.append("    call $__str_toLowerCase\n");
                    collector.append("    call $__str_compareTo\n");
                } else if (mName.equals("startsWith")) {
                    translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "String");
                    translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "String");
                    collector.append("    call $__str_startsWith\n");
                } else if (mName.equals("endsWith")) {
                    translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "String");
                    translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "String");
                    collector.append("    call $__str_endsWith\n");
                } else if (mName.equals("contains")) {
                    translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "String");
                    translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "String");
                    collector.append("    call $__str_contains\n");
                } else if (mName.equals("indexOf")) {
                    if (isStringBuilderType(scope, localJavaTypes)) {
                        translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "String");
                        collector.append("    call $__sb_indexOf\n");
                    } else {
                        translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "String");
                        Expression arg = call.getArgument(0);
                        if (isStringType(arg, localJavaTypes)) {
                            translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "String");
                        } else {
                            translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                            collector.append("    call $__str_from_char\n");
                        }
                        collector.append("    call $__str_indexOf\n");
                    }
                } else if (mName.equals("concat")) {
                    translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "String");
                    translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "String");
                    collector.append("    call $__str_concat\n");
                } else if (mName.equals("isEmpty")) {
                    translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "String");
                    collector.append("    call $__str_length\n");
                    collector.append("    i32.eqz\n");
                } else if (mName.equals("isBlank")) {
                    translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "String");
                    collector.append("    call $__str_trim\n");
                    collector.append("    call $__str_length\n");
                    collector.append("    i32.eqz\n");
                } else if (mName.equals("trim")) {
                    translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "String");
                    collector.append("    call $__str_trim\n");
                } else if (mName.equals("strip") || mName.equals("stripLeading") || mName.equals("stripTrailing")) {
                    translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "String");
                    collector.append("    call $__str_strip\n");
                } else if (mName.equals("toLowerCase")) {
                    translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "String");
                    collector.append("    call $__str_toLowerCase\n");
                } else if (mName.equals("toUpperCase")) {
                    translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "String");
                    collector.append("    call $__str_toUpperCase\n");
                } else if (mName.equals("toCharArray")) {
                    translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "String");
                    collector.append("    call $__str_toCharArray\n");
                } else if (mName.equals("getBytes")) {
                    translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "String");
                    collector.append("    call $__str_getBytes\n");
                } else if (mName.equals("repeat")) {
                    translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "String");
                    translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    collector.append("    call $__str_repeat\n");
                } else if (mName.equals("replace")) {
                    if (isStringBuilderType(scope, localJavaTypes)) {
                        translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        translateExpression(call.getArgument(2), collector, localTypes, localElementTypes, localJavaTypes, "String");
                        collector.append("    call $__sb_replace\n");
                    } else {
                        translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "String");
                        Expression arg0 = call.getArgument(0);
                        Expression arg1 = call.getArgument(1);
                        if (arg0.isCharLiteralExpr() || (arg0.isNameExpr() && "char".equals(localJavaTypes.get(arg0.asNameExpr().getNameAsString())))) {
                            translateExpression(arg0, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                            translateExpression(arg1, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                            collector.append("    call $__str_replace_char\n");
                        } else {
                            translateExpression(arg0, collector, localTypes, localElementTypes, localJavaTypes, "String");
                            translateExpression(arg1, collector, localTypes, localElementTypes, localJavaTypes, "String");
                            collector.append("    call $__str_replace_str\n");
                        }
                    }
                } else if (mName.equals("insert") && isStringBuilderType(scope, localJavaTypes)) {
                    translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    Expression arg = call.getArgument(1);
                    if (isStringType(arg, localJavaTypes)) {
                        translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "String");
                        collector.append("    call $__sb_insert_str\n");
                    } else if (arg.isCharLiteralExpr() || (arg.isNameExpr() && "char".equals(localJavaTypes.get(arg.asNameExpr().getNameAsString())))) {
                        translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        collector.append("    call $__sb_insert_char\n");
                    } else if (arg.isBooleanLiteralExpr() || (arg.isNameExpr() && "boolean".equals(localJavaTypes.get(arg.asNameExpr().getNameAsString())))) {
                        translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        collector.append("    call $__sb_insert_bool\n");
                    } else if (arg.isDoubleLiteralExpr() || (arg.isNameExpr() && ("double".equals(localJavaTypes.get(arg.asNameExpr().getNameAsString())) || "float".equals(localJavaTypes.get(arg.asNameExpr().getNameAsString()))))) {
                        translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "f64");
                        collector.append("    call $__sb_insert_f64\n");
                    } else {
                        translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        collector.append("    call $__sb_insert_i32\n");
                    }
                } else if (mName.equals("delete") && isStringBuilderType(scope, localJavaTypes)) {
                    translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    collector.append("    call $__sb_delete\n");
                } else if (mName.equals("deleteCharAt") && isStringBuilderType(scope, localJavaTypes)) {
                    translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    collector.append("    call $__sb_deleteCharAt\n");
                } else if (mName.equals("reverse") && isStringBuilderType(scope, localJavaTypes)) {
                    translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    collector.append("    call $__sb_reverse\n");
                } else if (mName.equals("append")) {
                    translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    Expression arg = call.getArgument(0);
                    if (isStringType(arg, localJavaTypes)) {
                        translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "String");
                        collector.append("    call $__sb_append_str\n");
                    } else if (arg.isCharLiteralExpr() || (arg.isNameExpr() && "char".equals(localJavaTypes.get(arg.asNameExpr().getNameAsString())))) {
                        translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        collector.append("    call $__sb_append_char\n");
                    } else if (arg.isBooleanLiteralExpr() || (arg.isNameExpr() && "boolean".equals(localJavaTypes.get(arg.asNameExpr().getNameAsString())))) {
                        translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        collector.append("    call $__sb_append_bool\n");
                    } else if (arg.isDoubleLiteralExpr() || (arg.isNameExpr() && ("double".equals(localJavaTypes.get(arg.asNameExpr().getNameAsString())) || "float".equals(localJavaTypes.get(arg.asNameExpr().getNameAsString()))))) {
                        translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "f64");
                        collector.append("    call $__sb_append_f64\n");
                    } else {
                        translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        collector.append("    call $__sb_append_i32\n");
                    }
                } else if (mName.equals("toString") && isStringBuilderType(scope, localJavaTypes)) {
                    translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    collector.append("    call $__sb_toString\n");
                } else if (mName.equals("toString") && isStringType(scope, localJavaTypes)) {
                    translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "String");
                } else if (mName.equals("valueOf") && scope.isNameExpr() && ("String".equals(scope.asNameExpr().getNameAsString()) || "java.lang.String".equals(scope.asNameExpr().getNameAsString()))) {
                    Expression arg0 = call.getArgument(0);
                    String argType = getExpressionJavaType(arg0, localJavaTypes);
                    if (isStringType(arg0, localJavaTypes)) {
                        translateExpression(arg0, collector, localTypes, localElementTypes, localJavaTypes, "String");
                    } else if ("char[]".equals(argType) || (arg0.isArrayCreationExpr() && "char".equals(arg0.asArrayCreationExpr().getElementType().asString()))) {
                        translateExpression(arg0, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        collector.append("    call $__str_from_char_array\n");
                    } else if (arg0.isCharLiteralExpr() || "char".equals(argType)) {
                        translateExpression(arg0, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        collector.append("    call $__str_from_char\n");
                    } else if (arg0.isBooleanLiteralExpr() || "boolean".equals(argType)) {
                        translateExpression(arg0, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        collector.append("    call $__str_from_bool\n");
                    } else if (arg0.isDoubleLiteralExpr() || "double".equals(argType) || "float".equals(argType)) {
                        translateExpression(arg0, collector, localTypes, localElementTypes, localJavaTypes, "f64");
                        collector.append("    call $__str_from_f64\n");
                    } else if (arg0.isIntegerLiteralExpr() || "int".equals(argType) || "byte".equals(argType) || "short".equals(argType) || "long".equals(argType)) {
                        translateExpression(arg0, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        collector.append("    call $__str_from_i32\n");
                    } else {
                        // Object toString via vtable slot 0
                        translateExpression(arg0, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        collector.append("    local.set $__obj_tmp\n");
                        collector.append("    local.get $__obj_tmp\n");
                        collector.append("    local.get $__obj_tmp\n");
                        collector.append("    i32.load offset=0\n");
                        collector.append("    call_indirect (type $t_sig_i32_to_i32)\n");
                    }
                } else if (mName.equals("clone")) {
                    String targetType = getExpressionJavaType(scope, localJavaTypes);
                    ClassModel targetCm = (targetType != null && classRegistry != null) ? classRegistry.getClass(targetType) : null;
                    int size = targetCm != null ? targetCm.getStructSize() : 32;
                    translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    collector.append("    i32.const ").append(size).append("\n");
                    collector.append("    call $Object_clone\n");
                } else if (scope.isSuperExpr()) {
                    String superClassName = currentClass != null ? currentClass.getSuperClassName() : null;
                    MethodModel method = resolveMethodCall(superClassName, call, localJavaTypes);
                    if (!method.isStatic()) collector.append("    local.get $this\n");
                    for (int i = 0; i < call.getArguments().size(); i++) {
                        translateExpression(call.getArgument(i), collector, localTypes, localElementTypes,
                                localJavaTypes, method.getParamWasmTypes().get(i));
                    }
                    collector.append("    call ").append(method.getFunctionName()).append("\n");
                } else {
                    String targetType = getExpressionJavaType(scope, localJavaTypes);
                    if (scope.isNameExpr() && classRegistry != null && classRegistry.hasClass(scope.asNameExpr().getNameAsString())) {
                        ClassModel scopeCm = classRegistry.getClass(scope.asNameExpr().getNameAsString());
                        if (scopeCm.isEnum()) {
                            if ("values".equals(mName)) {
                                collector.append("    call $").append(scopeCm.getName()).append("_values\n");
                                return;
                            } else if ("valueOf".equals(mName) && call.getArguments().size() == 1) {
                                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "String");
                                collector.append("    call $").append(scopeCm.getName()).append("_valueOf\n");
                                return;
                            }
                        }
                    }
                    if (targetType != null && classRegistry != null && classRegistry.isRecord(targetType)) {
                        RecordModel rm = classRegistry.getRecord(targetType);
                        if (rm.getComponent(mName) != null) {
                            if (scope.isThisExpr()) {
                                collector.append("    local.get $this$").append(mName).append("\n");
                            } else if (scope.isNameExpr()) {
                                String scopeVar = scope.asNameExpr().getNameAsString();
                                collector.append("    local.get $").append(scopeVar).append("$").append(mName).append("\n");
                            }
                            return;
                        }
                        MethodModel rmm = resolveMethodCall(rm.getName(), call, localJavaTypes);
                        if (rmm != null) {
                            if (!rmm.isStatic()) {
                                if (scope.isThisExpr()) {
                                    for (FieldModel c : rm.getComponents()) {
                                        collector.append("    local.get $this$").append(c.getName()).append("\n");
                                    }
                                } else if (scope.isNameExpr()) {
                                    String scopeVar = scope.asNameExpr().getNameAsString();
                                    for (FieldModel c : rm.getComponents()) {
                                        collector.append("    local.get $").append(scopeVar).append("$").append(c.getName()).append("\n");
                                    }
                                }
                            }
                            for (int i = 0; i < call.getArguments().size(); i++) {
                                String pWasm = i < rmm.getParamWasmTypes().size() ? rmm.getParamWasmTypes().get(i) : "i32";
                                translateExpression(call.getArgument(i), collector, localTypes, localElementTypes, localJavaTypes, pWasm);
                            }
                            collector.append("    call ").append(rmm.getFunctionName()).append("\n");
                            return;
                        }
                    } else if (scope.isNameExpr() && classRegistry != null && classRegistry.isRecord(scope.asNameExpr().getNameAsString())) {
                        String recName = scope.asNameExpr().getNameAsString();
                        RecordModel rm = classRegistry.getRecord(recName);
                        MethodModel rmm = resolveMethodCall(rm.getName(), call, localJavaTypes);
                        for (int i = 0; i < call.getArguments().size(); i++) {
                            String pWasm = (rmm != null && i < rmm.getParamWasmTypes().size()) ? rmm.getParamWasmTypes().get(i) : "i32";
                            translateExpression(call.getArgument(i), collector, localTypes, localElementTypes, localJavaTypes, pWasm);
                        }
                        collector.append("    call ").append(rmm.getFunctionName()).append("\n");
                        return;
                    }
                    ClassModel targetCm = (targetType != null && classRegistry != null) ? classRegistry.getClass(targetType) : null;
                    MethodModel targetMm = (targetType != null && classRegistry != null) ? resolveMethodCall(targetType, call, localJavaTypes) : (targetCm != null ? targetCm.getMethodByName(mName) : null);
                    if (targetMm == null && ("toString".equals(mName) || "equals".equals(mName) || "hashCode".equals(mName) ||
                            "close".equals(mName) || "compareTo".equals(mName) || "iterator".equals(mName) || "hasNext".equals(mName) || "next".equals(mName)) && classRegistry != null) {
                        targetMm = classRegistry.findMethod("Object", mName);
                    }

                    if (targetMm != null && targetMm.isVirtual() && targetMm.getVirtualSlotIndex() >= 0 && (targetCm == null || targetCm.isOopClass())) {
                        String sigName = targetMm.getTypeSignatureName();
                        if (sigName == null && classRegistry != null) {
                            sigName = classRegistry.getOrCreateTypeSignature(targetMm.buildWasmTypeSignature());
                        }
                        // Dynamic dispatch via vtable and call_indirect
                        translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        collector.append("    local.set $__obj_tmp\n");
                        collector.append("    local.get $__obj_tmp\n");
                        for (int i = 0; i < call.getArguments().size(); i++) {
                            String pWasm = i < targetMm.getParamWasmTypes().size() ? targetMm.getParamWasmTypes().get(i) : "i32";
                            translateExpression(call.getArgument(i), collector, localTypes, localElementTypes, localJavaTypes, pWasm);
                        }
                        collector.append("    local.get $__obj_tmp\n");
                        collector.append("    i32.load offset=0\n");
                        collector.append("    i32.const ").append(targetMm.getVirtualSlotIndex()).append("\n");
                        collector.append("    i32.add\n");
                        collector.append("    call_indirect (type ").append(sigName).append(")\n");
                    } else if (targetMm != null) {
                        if (!targetMm.isStatic()) {
                            translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        }
                        for (int i = 0; i < call.getArguments().size(); i++) {
                            String pWasm = i < targetMm.getParamWasmTypes().size() ? targetMm.getParamWasmTypes().get(i) : "i32";
                            translateExpression(call.getArgument(i), collector, localTypes, localElementTypes, localJavaTypes, pWasm);
                        }
                        collector.append("    call ").append(targetMm.getFunctionName()).append("\n");
                    } else {
                        String snippetImport = null;
                        if (targetType != null && jsSnippets != null) {
                            String shortTarget = targetType.contains(".") ? targetType.substring(targetType.lastIndexOf('.') + 1) : targetType;
                            String key = "js_inline_" + shortTarget + "_" + mName;
                            if (jsSnippets.containsKey(key)) {
                                snippetImport = key;
                            }
                        }
                        if (snippetImport != null) {
                            var snippetModel = jsSnippets.get(snippetImport);
                            if (!snippetModel.isStatic()) {
                                translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                            }
                            for (int i = 0; i < call.getArguments().size(); i++) {
                                int pIdx = snippetModel.isStatic() ? i : i + 1;
                                String pWasm = pIdx < snippetModel.getParameters().size() ? snippetModel.getParameters().get(pIdx).getWasmType() : "i32";
                                String pJava = pIdx < snippetModel.getParameters().size() ? snippetModel.getParameters().get(pIdx).getJavaType() : "";
                                Expression arg = call.getArgument(i);
                                if ("String".equals(pJava) || "java.lang.String".equals(pJava)) {
                                    translateDomStringArg(arg, collector, localTypes, localElementTypes, localJavaTypes);
                                } else {
                                    translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, pWasm);
                                }
                            }
                            collector.append("    call $dom_").append(snippetImport).append("\n");
                        } else {
                            translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, null);
                            for (Expression arg : call.getArguments()) {
                                translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, null);
                            }
                            collector.append("    call $").append(mName).append("\n");
                        }
                    }
                }
            } else {
                if (currentRecord != null && currentRecord.getComponent(mName) != null) {
                    collector.append("    local.get $this$").append(mName).append("\n");
                } else if (currentRecord != null && !classRegistry.methodCandidates(currentRecord.getName(), mName).isEmpty()) {
                    MethodModel method = resolveMethodCall(currentRecord.getName(), call, localJavaTypes);
                    if (!method.isStatic()) for (FieldModel component : currentRecord.getComponents())
                        collector.append("    local.get $this$").append(component.getName()).append("\n");
                    for (int i = 0; i < call.getArguments().size(); i++)
                        translateExpression(call.getArgument(i), collector, localTypes, localElementTypes,
                                localJavaTypes, method.getParamWasmTypes().get(i));
                    collector.append("    call ").append(method.getFunctionName()).append("\n");
                } else if (DomHelper.isDomStaticMethod(mName) || DomHelper.isJsStaticMethod(mName, call)) {
                    translateDomOrWindowStaticCall(call, collector, localTypes, localElementTypes, localJavaTypes);
                } else if (DomHelper.isDomInstanceMethod(mName)) {
                    for (Expression arg : call.getArguments()) {
                        translateDomStringArg(arg, collector, localTypes, localElementTypes, localJavaTypes);
                    }
                    collector.append("    call $dom_").append(mName).append("\n");
                } else if (currentClass != null && !classRegistry.methodCandidates(currentClass.getName(), mName).isEmpty()) {
                    MethodModel mm = resolveMethodCall(currentClass.getName(), call, localJavaTypes);
                    if (mm.isVirtual() && currentClass.isOopClass()) {
                        collector.append("    local.get $this\n");
                        for (int i = 0; i < call.getArguments().size(); i++) {
                            String pWasm = i < mm.getParamWasmTypes().size() ? mm.getParamWasmTypes().get(i) : "i32";
                            translateExpression(call.getArgument(i), collector, localTypes, localElementTypes, localJavaTypes, pWasm);
                        }
                        collector.append("    local.get $this\n");
                        collector.append("    i32.load offset=0\n");
                        collector.append("    i32.const ").append(mm.getVirtualSlotIndex()).append("\n");
                        collector.append("    i32.add\n");
                        collector.append("    call_indirect (type ").append(mm.getTypeSignatureName()).append(")\n");
                    } else {
                        if (!mm.isStatic() && (currentClass != null && currentClass.isOopClass())) {
                            collector.append("    local.get $this\n");
                        }
                        for (int i = 0; i < call.getArguments().size(); i++) {
                            String pWasm = i < mm.getParamWasmTypes().size() ? mm.getParamWasmTypes().get(i) : "i32";
                            translateExpression(call.getArgument(i), collector, localTypes, localElementTypes, localJavaTypes, pWasm);
                        }
                        collector.append("    call ").append(mm.getFunctionName()).append("\n");
                    }
                } else if (("toString".equals(mName) || "equals".equals(mName) || "hashCode".equals(mName) ||
                        "close".equals(mName) || "compareTo".equals(mName) || "iterator".equals(mName) || "hasNext".equals(mName) || "next".equals(mName)) && currentClass != null && currentClass.isOopClass() && classRegistry != null) {
                    MethodModel mm = classRegistry.findMethod(currentClass.getName(), mName);
                    if (mm != null && mm.isVirtual()) {
                        collector.append("    local.get $this\n");
                        for (int i = 0; i < call.getArguments().size(); i++) {
                            String pWasm = i < mm.getParamWasmTypes().size() ? mm.getParamWasmTypes().get(i) : "i32";
                            translateExpression(call.getArgument(i), collector, localTypes, localElementTypes, localJavaTypes, pWasm);
                        }
                        collector.append("    local.get $this\n");
                        collector.append("    i32.load offset=0\n");
                        collector.append("    i32.const ").append(mm.getVirtualSlotIndex()).append("\n");
                        collector.append("    i32.add\n");
                        collector.append("    call_indirect (type ").append(mm.getTypeSignatureName()).append(")\n");
                    } else {
                        for (Expression arg : call.getArguments()) {
                            translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, null);
                        }
                        collector.append("    call $").append(mName).append("\n");
                    }
                } else {
                    for (Expression arg : call.getArguments()) {
                        translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, null);
                    }
                    collector.append("    call $").append(mName).append("\n");
                }
            }
        }
    }

    private void emitNumericWidening(String from, String to, StringBuilder out) {
        if (to == null || to.equals(from)) return;
        String op = null;
        if ("i32".equals(from) && "i64".equals(to)) op = "i64.extend_i32_s";
        if ("i32".equals(from) && "f32".equals(to)) op = "f32.convert_i32_s";
        if ("i32".equals(from) && "f64".equals(to)) op = "f64.convert_i32_s";
        if ("i64".equals(from) && "f32".equals(to)) op = "f32.convert_i64_s";
        if ("i64".equals(from) && "f64".equals(to)) op = "f64.convert_i64_s";
        if ("f32".equals(from) && "f64".equals(to)) op = "f64.promote_f32";
        if (op != null) out.append("    ").append(op).append("\n");
    }

    private String promotedNumeric(String left, String right) {
        Set<String> numeric = Set.of("byte", "short", "char", "int", "long", "float", "double");
        if (left == null || right == null || !numeric.contains(left) || !numeric.contains(right)) return null;
        for (String type : List.of("double", "float", "long")) if (type.equals(left) || type.equals(right)) return type;
        return "int";
    }

    private String conditionalType(ConditionalExpr expression, String left, String right) {
        if (left == null || right == null) return null;
        if (left.equals(right)) return left;
        String numeric = promotedNumeric(left, right);
        if (numeric != null) {
            if (Set.of("byte", "short").contains(left) && Set.of("byte", "short").contains(right)) return "short";
            if (fitsConditionalConstant(left, expression.getElseExpr(), right)) return left;
            if (fitsConditionalConstant(right, expression.getThenExpr(), left)) return right;
            return numeric;
        }
        Set<String> primitive = Set.of("byte", "short", "char", "int", "long", "float", "double", "boolean");
        if (primitive.contains(left) || primitive.contains(right)) return null;
        if (left.equals("null")) return right;
        if (right.equals("null")) return left;
        Set<String> ancestors = new HashSet<>();
        String cursor = left;
        while (cursor != null && ancestors.add(cursor)) {
            ClassModel model = classRegistry == null ? null : classRegistry.getClass(cursor);
            cursor = model == null ? null : model.getSuperClassName();
        }
        cursor = right;
        Set<String> visited = new HashSet<>();
        while (cursor != null && visited.add(cursor)) {
            if (ancestors.contains(cursor)) return cursor;
            ClassModel model = classRegistry == null ? null : classRegistry.getClass(cursor);
            cursor = model == null ? null : model.getSuperClassName();
        }
        return "Object";
    }

    private boolean fitsConditionalConstant(String narrow, Expression expression, String other) {
        if (!"int".equals(other) || !Set.of("byte", "short", "char").contains(narrow)) return false;
        try {
            String literal = expression.toString().replace("_", "").replace(" ", "");
            long value = Long.decode(literal);
            return switch (narrow) {
                case "byte" -> value >= -128 && value <= 127;
                case "short" -> value >= -32768 && value <= 32767;
                default -> value >= 0 && value <= 65535;
            };
        } catch (NumberFormatException ignored) { return false; }
    }

    private MethodModel sourceMethod(MethodCallExpr call, Map<String, String> locals) {
        if (classRegistry == null) return null;
        String owner = call.getScope().isPresent() ? getExpressionJavaType(call.getScope().get(), locals)
                : currentRecord != null ? currentRecord.getName() : currentClass != null ? currentClass.getName() : null;
        if (classRegistry.methodCandidates(owner, call.getNameAsString()).stream().noneMatch(m -> m.getDeclaration() != null)) return null;
        return resolveMethodCall(owner, call, locals);
    }

    private void emitSourceMethod(MethodCallExpr call, MethodModel method, StringBuilder out,
            Map<String, String> locals, Map<String, String> elements, Map<String, String> javaTypes, String expected) {
        Expression receiver = call.getScope().orElse(new ThisExpr());
        RecordModel record = classRegistry.getRecord(method.getClassName());
        boolean dynamic = !method.isStatic() && record == null && method.isVirtual()
                && method.getVirtualSlotIndex() >= 0 && !receiver.isSuperExpr()
                && classRegistry.getClass(method.getClassName()).isOopClass();
        String temp = receiverLocals.get(call);
        if (!method.isStatic()) {
            if (record != null) {
                String prefix = receiver.isThisExpr() ? "this" : receiver.isNameExpr() ? receiver.asNameExpr().getNameAsString() : null;
                if (prefix == null) throw JavaSourcePreparation.error(call, "Store the record receiver in a local before invoking a custom method");
                for (FieldModel c : record.getComponents()) out.append("    local.get $").append(prefix).append("$").append(c.getName()).append("\n");
            } else if (classRegistry.getClass(method.getClassName()).isOopClass()) {
                translateExpression(receiver, out, locals, elements, javaTypes, "i32");
                if (dynamic) {
                    out.append("    local.tee $").append(temp).append("\n");
                }
            }
        } else if (call.getScope().isPresent() && !(receiver.isNameExpr() && !javaTypes.containsKey(receiver.asNameExpr().getNameAsString())
                && (classRegistry.hasClass(receiver.toString()) || classRegistry.isRecord(receiver.toString())))) {
            // Java evaluates an expression used to qualify a static invocation.
            translateExpression(receiver, out, locals, elements, javaTypes, "i32");
            out.append("    drop\n");
        }
        for (int i = 0; i < call.getArguments().size(); i++)
            translateExpression(call.getArgument(i), out, locals, elements, javaTypes, method.getParamWasmTypes().get(i));
        if (dynamic) {
            out.append("    local.get $").append(temp).append("\n    i32.load offset=0\n    i32.const ")
                    .append(method.getVirtualSlotIndex()).append("\n    i32.add\n    call_indirect (type ")
                    .append(method.getTypeSignatureName()).append(")\n");
        } else out.append("    call ").append(method.getFunctionName()).append("\n");
        if (method.getReturnWasmType() != null) emitNumericWidening(method.getReturnWasmType(), expected, out);
    }

    private MethodModel resolveMethodCall(String owner, MethodCallExpr call, Map<String, String> localJavaTypes) {
        List<MethodModel> candidates = classRegistry.methodCandidates(owner, call.getNameAsString());
        if (candidates.isEmpty()) return classRegistry.findMethod(owner, call.getNameAsString());
        // Class-qualified invocation cannot select an instance method.
        if (call.getScope().filter(e -> e.isNameExpr() && !localJavaTypes.containsKey(e.asNameExpr().getNameAsString())
                && (classRegistry.hasClass(e.asNameExpr().getNameAsString()) || classRegistry.isRecord(e.asNameExpr().getNameAsString()))).isPresent())
            candidates = candidates.stream().filter(MethodModel::isStatic).toList();
        String caller = currentClass != null ? currentClass.getName() : currentRecord != null ? currentRecord.getName() : "";
        boolean staticContext = currentCallable instanceof MethodDeclaration method && method.isStatic();
        candidates = candidates.stream().filter(m -> accessible(m, caller, owner, call))
                .filter(m -> call.getScope().isPresent() || !staticContext || m.isStatic()).toList();
        List<String> arguments = call.getArguments().stream().map(a -> a.isNullLiteralExpr() ? "null"
                : getExpressionJavaType(a, localJavaTypes)).toList();
        try { return classRegistry.resolveOverload(candidates, arguments); }
        catch (IllegalArgumentException error) { throw JavaSourcePreparation.error(call, error.getMessage()); }
    }

    private boolean accessible(MethodModel method, String caller, String receiver, Node source) {
        CallableDeclaration<?> declaration = method.getDeclaration();
        if (declaration == null || declaration.isPublic()) return true;
        if (declaration.findAncestor(ClassOrInterfaceDeclaration.class).map(ClassOrInterfaceDeclaration::isInterface).orElse(false)
                && !declaration.isPrivate()) return true;
        if (declaration.isPrivate()) return method.getClassName().equals(caller);
        String declaredPackage = declaration.findAncestor(TypeDeclaration.class)
                .filter(t -> t.containsData(JavaSourcePreparation.PACKAGE)).map(t -> t.getData(JavaSourcePreparation.PACKAGE)).orElse("");
        String callerPackage = currentCallable == null ? "" : currentCallable.findAncestor(TypeDeclaration.class)
                .filter(t -> t.containsData(JavaSourcePreparation.PACKAGE)).map(t -> t.getData(JavaSourcePreparation.PACKAGE)).orElse("");
        if (declaredPackage.equals(callerPackage)) return true;
        if (!declaration.isProtected()) return false;
        ClassModel model = classRegistry.getClass(caller);
        while (model != null) {
            if (model.getName().equals(method.getClassName())) {
                if (source instanceof MethodCallExpr call && call.getScope().isPresent()
                        && !call.getScope().get().isSuperExpr()) {
                    ClassModel target = classRegistry.getClass(receiver);
                    while (target != null) {
                        if (target.getName().equals(caller)) return true;
                        target = classRegistry.getClass(target.getSuperClassName());
                    }
                    return false;
                }
                return true;
            }
            model = classRegistry.getClass(model.getSuperClassName());
        }
        return false;
    }

    private String emitConstructorArguments(String className, NodeList<Expression> arguments, Node source,
            StringBuilder collector, Map<String, String> localTypes, Map<String, String> localElementTypes,
            Map<String, String> localJavaTypes) {
        ClassModel model = classRegistry == null ? null : classRegistry.getClass(className);
        MethodModel selected = null;
        if (model != null && (!model.getConstructors().isEmpty() || !arguments.isEmpty())) {
            List<String> types = arguments.stream().map(a -> a.isNullLiteralExpr() ? "null"
                    : getExpressionJavaType(a, localJavaTypes)).toList();
            try { selected = classRegistry.resolveOverload(model.getConstructors().stream()
                    .filter(m -> accessible(m, currentClass != null ? currentClass.getName()
                            : currentRecord != null ? currentRecord.getName() : "", className, source)).toList(), types); }
            catch (IllegalArgumentException error) { throw JavaSourcePreparation.error(source, error.getMessage()); }
        }
        for (int i = 0; i < arguments.size(); i++) {
            String wasmType = selected == null ? "i32" : selected.getParamWasmTypes().get(i);
            translateExpression(arguments.get(i), collector, localTypes, localElementTypes, localJavaTypes, wasmType);
        }
        return selected == null ? "$" + className + "__init" : selected.getFunctionName();
    }

    private String getExpressionJavaType(Expression expr, Map<String, String> localJavaTypes) {
        if (expr.containsData(JavaSourcePreparation.LOWERED_JAVA_TYPE))
            return expr.getData(JavaSourcePreparation.LOWERED_JAVA_TYPE);
        if (expr.isNullLiteralExpr()) return "null";
        if (expr.isConditionalExpr()) {
            ConditionalExpr conditional = expr.asConditionalExpr();
            String left = getExpressionJavaType(conditional.getThenExpr(), localJavaTypes);
            String right = getExpressionJavaType(conditional.getElseExpr(), localJavaTypes);
            return conditionalType(conditional, left, right);
        }
        if (expr.isBinaryExpr()) {
            BinaryExpr binary = expr.asBinaryExpr();
            String left = getExpressionJavaType(binary.getLeft(), localJavaTypes);
            String right = getExpressionJavaType(binary.getRight(), localJavaTypes);
            return switch (binary.getOperator()) {
                case EQUALS, NOT_EQUALS, LESS, LESS_EQUALS, GREATER, GREATER_EQUALS, AND, OR -> "boolean";
                case PLUS -> "String".equals(left) || "String".equals(right) ? "String" : promotedNumeric(left, right);
                case LEFT_SHIFT, SIGNED_RIGHT_SHIFT, UNSIGNED_RIGHT_SHIFT -> "long".equals(left) ? "long" : "int";
                case BINARY_AND, BINARY_OR, XOR -> "boolean".equals(left) && "boolean".equals(right) ? "boolean" : promotedNumeric(left, right);
                default -> promotedNumeric(left, right);
            };
        }
        if (expr.isStringLiteralExpr() || expr.isTextBlockLiteralExpr()) return "String";
        if (expr.isBooleanLiteralExpr()) return "boolean";
        if (expr.isCharLiteralExpr()) return "char";
        if (expr.isIntegerLiteralExpr()) return "int";
        if (expr.isLongLiteralExpr()) return "long";
        if (expr.isDoubleLiteralExpr()) {
            String value = expr.asDoubleLiteralExpr().getValue();
            return value.endsWith("f") || value.endsWith("F") ? "float" : "double";
        }
        if (expr.isUnaryExpr()) {
            UnaryExpr unary = expr.asUnaryExpr();
            if (unary.getOperator() == UnaryExpr.Operator.LOGICAL_COMPLEMENT) return "boolean";
            String type = getExpressionJavaType(unary.getExpression(), localJavaTypes);
            return Set.of("byte", "short", "char").contains(type == null ? "" : type) ? "int" : type;
        }
        if (expr.isArrayCreationExpr()) {
            ArrayCreationExpr array = expr.asArrayCreationExpr();
            return array.getElementType().asString() + "[]".repeat(array.getLevels().size());
        }
        if (expr.isNameExpr()) {
            String name = expr.asNameExpr().getNameAsString();
            if (localJavaTypes != null && localJavaTypes.containsKey(name)) {
                return localJavaTypes.get(name);
            }
            if (globalVars != null && globalVars.containsKey(name)) {
                return globalVars.get(name).getJavaType();
            }
            if (currentClass != null && currentClass.hasField(name)) {
                return currentClass.getField(name).getJavaType();
            }
            if (classRegistry != null && (classRegistry.hasClass(name) || classRegistry.isRecord(name))) return name;
            return null;
        }
        if (expr.isSuperExpr()) return currentClass == null ? null : currentClass.getSuperClassName();
        if (expr.isThisExpr()) {
            if (currentRecord != null) return currentRecord.getName();
            if (localJavaTypes != null && localJavaTypes.containsKey("this")) return localJavaTypes.get("this");
            return currentClass != null ? currentClass.getName() : null;
        }
        if (expr.isFieldAccessExpr()) {
            FieldAccessExpr fae = expr.asFieldAccessExpr();
            String fName = fae.getNameAsString();
            if (fae.getScope().isNameExpr() && classRegistry != null && classRegistry.hasClass(fae.getScope().asNameExpr().getNameAsString())) {
                ClassModel cm = classRegistry.getClass(fae.getScope().asNameExpr().getNameAsString());
                if (cm.isEnum() && cm.getEnumConstants().contains(fName)) {
                    return cm.getName();
                }
            }
            String targetType = getExpressionJavaType(fae.getScope(), localJavaTypes);
            if (targetType != null && classRegistry != null && classRegistry.hasClass(targetType)) {
                FieldModel fm = classRegistry.getClass(targetType).getField(fName);
                if (fm != null) return fm.getJavaType();
            }
            if (currentClass != null && currentClass.hasField(fName)) {
                FieldModel fm = currentClass.getField(fName);
                if (fm != null) return fm.getJavaType();
            }
        }
        if (expr.isClassExpr()) {
            return "Class";
        }
        if (expr.isCastExpr()) {
            return expr.asCastExpr().getType().asString();
        }
        if (expr.isEnclosedExpr()) {
            return getExpressionJavaType(expr.asEnclosedExpr().getInner(), localJavaTypes);
        }
        if (expr.isObjectCreationExpr()) {
            return expr.asObjectCreationExpr().getTypeAsString();
        }
        if (expr.isMethodCallExpr()) {
            MethodCallExpr call = expr.asMethodCallExpr();
            String mName = call.getNameAsString();
            MethodModel sourceMethod = sourceMethod(call, localJavaTypes);
            if (sourceMethod != null) return sourceMethod.getReturnTypeName();
            if (mName.equals("getClass")) {
                return "Class";
            }
            if (mName.equals("currentThread")) {
                return "Thread";
            }
            if (call.getScope().isPresent()) {
                String scopeType = getExpressionJavaType(call.getScope().get(), localJavaTypes);
                String scopeStr = call.getScope().get().toString();
                if ("Class".equals(scopeStr) || "Class".equals(scopeType) || (scopeType != null && scopeType.startsWith("Class<")) || (scopeType != null && scopeType.endsWith(".Class"))) {
                    if (mName.equals("getName") || mName.equals("getSimpleName") || mName.equals("toString")) return "String";
                    if (mName.equals("isInterface") || mName.equals("isEnum") || mName.equals("isArray") || mName.equals("isInstance") || mName.equals("equals")) return "boolean";
                    if (mName.equals("getId") || mName.equals("hashCode")) return "int";
                    if (mName.equals("getSuperclass") || mName.equals("forName")) return "Class";
                }
                if ("Thread".equals(scopeStr) || "Thread".equals(scopeType) || (scopeType != null && scopeType.endsWith(".Thread"))) {
                    if (mName.equals("getId") || mName.equals("activeCount")) return "int";
                    if (mName.equals("getName")) return "String";
                    if (mName.equals("isAlive")) return "boolean";
                    if (mName.equals("currentThread")) return "Thread";
                    if (mName.equals("start") || mName.equals("run") || mName.equals("setName") || mName.equals("sleep") || mName.equals("yield") || mName.equals("onSpinWait")) return "void";
                }
            }
            if (isMathCall(call)) {
                if (mName.equals("round")) {
                    if (call.getArguments().size() > 0 && "float".equals(getExpressionJavaType(call.getArgument(0), localJavaTypes))) {
                        return "int";
                    }
                    return "long";
                }
                if (mName.equals("min") || mName.equals("max") || mName.equals("abs") || mName.equals("signum")) {
                    if (call.getArguments().size() > 0) {
                        String t = getExpressionJavaType(call.getArgument(0), localJavaTypes);
                        return t != null ? t : "double";
                    }
                }
                return "double";
            }
            if (isWrapperCall(call, localJavaTypes)) {
                if (mName.equals("parseLong") || mName.equals("longValue") || mName.equals("doubleToLongBits") || mName.equals("doubleToRawLongBits")) return "long";
                if (mName.equals("parseDouble") || mName.equals("doubleValue") || mName.equals("longBitsToDouble")) return "double";
                if (mName.equals("parseFloat") || mName.equals("floatValue") || mName.equals("intBitsToFloat")) return "float";
                if (mName.equals("parseBoolean") || mName.equals("booleanValue") || mName.equals("isNaN") || mName.equals("isInfinite") || mName.equals("isFinite") || mName.equals("isDigit") || mName.equals("isLetter") || mName.equals("isLetterOrDigit") || mName.equals("isWhitespace") || mName.equals("isUpperCase") || mName.equals("isLowerCase")) return "boolean";
                if (mName.equals("toUpperCase") || mName.equals("toLowerCase") || mName.equals("charValue") || mName.equals("forDigit")) return "char";
                if (mName.equals("byteValue")) return "byte";
                if (mName.equals("shortValue")) return "short";
                if (mName.equals("toString") || mName.equals("toHexString") || mName.equals("toOctalString") || mName.equals("toBinaryString")) return "String";
                if (mName.equals("valueOf") && call.getArguments().size() > 0) {
                    Expression arg0 = call.getArgument(0);
                    if (!isStringType(arg0, localJavaTypes)) {
                        String t = getExpressionJavaType(arg0, localJavaTypes);
                        return t != null ? t : "int";
                    }
                }
                return "int";
            }
            if (isStringBuilderType(call.getScope().orElse(null), localJavaTypes)) {
                if (mName.equals("append") || mName.equals("insert") || mName.equals("delete") ||
                    mName.equals("deleteCharAt") || mName.equals("reverse") || mName.equals("replace")) {
                    return "StringBuilder";
                }
                if (mName.equals("length") || mName.equals("indexOf")) return "int";
                if (mName.equals("charAt")) return "char";
                if (mName.equals("subSequence")) return "CharSequence";
                if (mName.equals("substring") || mName.equals("toString")) return "String";
            }
            if (isStringType(call.getScope().orElse(null), localJavaTypes)) {
                if (mName.equals("toString") || mName.equals("substring") || mName.equals("concat") ||
                    mName.equals("trim") || mName.equals("strip") || mName.equals("stripLeading") || mName.equals("stripTrailing") ||
                    mName.equals("toLowerCase") || mName.equals("toUpperCase") || mName.equals("repeat") || mName.equals("replace") ||
                    mName.equals("valueOf")) return "String";
                if (mName.equals("toCharArray")) return "char[]";
                if (mName.equals("getBytes")) return "byte[]";
                if (mName.equals("subSequence")) return "CharSequence";
                if (mName.equals("compareTo") || mName.equals("compareToIgnoreCase") || mName.equals("indexOf") || mName.equals("length")) return "int";
                if (mName.equals("contains") || mName.equals("startsWith") || mName.equals("endsWith") || mName.equals("equalsIgnoreCase") || mName.equals("isEmpty") || mName.equals("isBlank") || mName.equals("equals")) return "boolean";
                if (mName.equals("charAt")) return "char";
            }
            if (call.getScope().isPresent() && call.getScope().get().isNameExpr()) {
                String scName = call.getScope().get().asNameExpr().getNameAsString();
                if ("String".equals(scName) || "java.lang.String".equals(scName)) {
                    if (mName.equals("valueOf") || mName.equals("join") || mName.equals("format")) return "String";
                }
            }
            if (mName.equals("getElementById") || mName.equals("createElement") || mName.equals("getBody") ||
                    mName.equals("querySelector") || mName.equals("getParent")) {
                return "Element";
            }
            if (mName.equals("querySelectorAll")) {
                return "Element[]";
            }
            if (mName.equals("getClassList")) {
                return "ClassList";
            }
            if (mName.equals("getContext2D") || mName.equals("getContext") || mName.equals("getCanvasContext2D")) {
                return "CanvasRenderingContext2D";
            }
            if (mName.equals("getWebGLContext")) {
                return "WebGLRenderingContext";
            }
            if (mName.equals("getInnerText") || mName.equals("getAttribute") || mName.equals("getValue") ||
                    mName.equals("getStyle") || mName.equals("getFillStyle") || mName.equals("getStrokeStyle") ||
                    mName.equals("getFont") || mName.equals("getTextAlign") ||
                    mName.equals("getString") || mName.equals("callString") || mName.equals("asString") ||
                    mName.equals("readString") || mName.equals("getPath") || mName.equals("getAsString") ||
                    mName.equals("readLine")) {
                return "String";
            }
            if (mName.equals("listResources")) {
                return "String[]";
            }
            if (mName.equals("create") || mName.equals("ofInstance")) {
                if (call.getScope().isPresent()) {
                    String scopeStr = call.getScope().get().toString();
                    if (scopeStr.equals("JSDate") || scopeStr.endsWith(".JSDate")) return "JSDate";
                }
            }
            if (mName.equals("getTime") || mName.equals("now") || mName.equals("parse") || mName.equals("UTC")) {
                if (call.getScope().isPresent()) {
                    String scopeType = getExpressionJavaType(call.getScope().get(), localJavaTypes);
                    String scopeStr = call.getScope().get().toString();
                    if ("JSDate".equals(scopeStr) || "JSDate".equals(scopeType) || (scopeType != null && scopeType.endsWith(".JSDate"))) {
                        return "double";
                    }
                }
            }
            if (mName.equals("hypot") || mName.equals("sin") || mName.equals("cos") || mName.equals("tan") ||
                    mName.equals("asin") || mName.equals("acos") || mName.equals("atan") || mName.equals("atan2") ||
                    mName.equals("sinh") || mName.equals("cosh") || mName.equals("tanh") || mName.equals("sqrt") ||
                    mName.equals("cbrt") || mName.equals("exp") || mName.equals("expm1") || mName.equals("log") ||
                    mName.equals("log10") || mName.equals("log2") || mName.equals("log1p") || mName.equals("pow") ||
                    mName.equals("floor") || mName.equals("ceil") || mName.equals("round") || mName.equals("trunc") ||
                    mName.equals("abs") || mName.equals("sign") || mName.equals("min") || mName.equals("max") ||
                    mName.equals("random") || mName.equals("PI") || mName.equals("E")) {
                if (call.getScope().isPresent()) {
                    String scopeStr = call.getScope().get().toString();
                    if (scopeStr.equals("JSMath") || scopeStr.endsWith(".JSMath")) return "double";
                }
            }
            if (mName.equals("stringify")) {
                if (call.getScope().isPresent()) {
                    String scopeStr = call.getScope().get().toString();
                    if (scopeStr.equals("JSJSON") || scopeStr.endsWith(".JSJSON")) return "String";
                }
            }
            if (mName.equals("parse")) {
                if (call.getScope().isPresent()) {
                    String scopeStr = call.getScope().get().toString();
                    if (scopeStr.equals("JSJSON") || scopeStr.endsWith(".JSJSON")) return "JSValue";
                }
            }
            if (mName.equals("global") || mName.equals("getWindow") || mName.equals("eval") ||
                    mName.equals("createObject") || mName.equals("createArray") || mName.equals("get") ||
                    mName.equals("call") || mName.equals("getIndex")) {
                return "JSObject";
            }
            if (mName.equals("getWidth") || mName.equals("getHeight") ||
                    mName.equals("setTimeout") || mName.equals("setInterval") || mName.equals("requestAnimationFrame") ||
                    mName.equals("getInt") || mName.equals("callInt") || mName.equals("asInt") ||
                    mName.equals("fileLength") || mName.equals("fileSize") || mName.equals("open") || mName.equals("close") ||
                    mName.equals("seek") || mName.equals("randomInt") || mName.equals("randomGet") || mName.equals("getLength") ||
                    mName.equals("readChar") || mName.equals("getColumns") || mName.equals("getRows") || mName.equals("identityHashCode")) {
                return "int";
            }
            if (mName.equals("nanoTime")) {
                return "long";
            }
            if (mName.equals("currentTimeMillis")) {
                if (call.getScope().isPresent()) {
                    String scopeStr = call.getScope().get().toString();
                    if (scopeStr.equals("WASI") || scopeStr.endsWith(".WASI")) {
                        return "double";
                    }
                }
                return "long";
            }
            if (mName.equals("getLineWidth") || mName.equals("getGlobalAlpha") ||
                    mName.equals("getDouble") || mName.equals("callDouble") || mName.equals("asDouble") ||
                    mName.equals("clockTimeGet")) {
                return "double";
            }
            if (mName.equals("prompt") || mName.equals("lineSeparator") || mName.equals("getenv") || mName.equals("getProperty")) {
                return "String";
            }
            if (mName.equals("isChecked") || mName.equals("hasClass") || mName.equals("contains") ||
                    mName.equals("getBoolean") || mName.equals("callBoolean") || mName.equals("asBoolean") || mName.equals("isArray") ||
                    mName.equals("writeString") || mName.equals("appendString") || mName.equals("fileExists") ||
                    mName.equals("exists") || mName.equals("deleteFile") || mName.equals("removeFile") ||
                    mName.equals("delete") || mName.equals("createDirectory") || mName.equals("mkdir") || mName.equals("hasResource")) {
                return "boolean";
            }
            if (mName.equals("of") || mName.equals("splat") || mName.equals("zero") ||
                    mName.equals("add") || mName.equals("sub") || mName.equals("mul") || mName.equals("div") ||
                    mName.equals("min") || mName.equals("max") || mName.equals("sqrt") || mName.equals("abs") ||
                    mName.equals("neg") || mName.equals("withLane") || mName.equals("replaceLane") ||
                    mName.equals("withX") || mName.equals("withY") || mName.equals("withZ") || mName.equals("withW") ||
                    mName.equals("and") || mName.equals("or") || mName.equals("xor") || mName.equals("not") ||
                    mName.equals("shiftLeft") || mName.equals("shiftRight") || mName.equals("shl") || mName.equals("shr")) {
                if (call.getScope().isPresent()) {
                    String scopeStr = call.getScope().get().toString();
                    String scopeType = getExpressionJavaType(call.getScope().get(), localJavaTypes);
                    if ("Float32x4".equals(scopeStr) || "Float32x4".equals(scopeType) || (scopeType != null && scopeType.endsWith(".Float32x4"))) {
                        return "Float32x4";
                    }
                    if ("Int32x4".equals(scopeStr) || "Int32x4".equals(scopeType) || (scopeType != null && scopeType.endsWith(".Int32x4"))) {
                        return "Int32x4";
                    }
                }
            }
            if (mName.equals("getX") || mName.equals("getY") || mName.equals("getZ") || mName.equals("getW") ||
                    mName.equals("get") || mName.equals("extractLane") || mName.equals("dot") || mName.equals("sum")) {
                if (call.getScope().isPresent()) {
                    String scopeType = getExpressionJavaType(call.getScope().get(), localJavaTypes);
                    String scopeStr = call.getScope().get().toString();
                    if ("Float32x4".equals(scopeStr) || "Float32x4".equals(scopeType) || (scopeType != null && scopeType.endsWith(".Float32x4"))) {
                        return "float";
                    }
                    if ("Int32x4".equals(scopeStr) || "Int32x4".equals(scopeType) || (scopeType != null && scopeType.endsWith(".Int32x4"))) {
                        return "int";
                    }
                }
            }
            if (mName.equals("load")) {
                if (call.getScope().isPresent()) {
                    String scopeStr = call.getScope().get().toString();
                    if (scopeStr.equals("Float32x4") || scopeStr.endsWith(".Float32x4")) return "Float32x4";
                    if (scopeStr.equals("Int32x4") || scopeStr.endsWith(".Int32x4")) return "Int32x4";
                }
            }
            if (mName.equals("loadI32") || mName.equals("loadU8") || mName.equals("loadI8") ||
                    mName.equals("loadU16") || mName.equals("loadI16") || mName.equals("size") ||
                    mName.equals("grow") || mName.equals("getIndex")) {
                if (call.getScope().isPresent()) {
                    String scopeStr = call.getScope().get().toString();
                    String scopeType = getExpressionJavaType(call.getScope().get(), localJavaTypes);
                    if ("Memory".equals(scopeStr) || "Memory".equals(scopeType) || (scopeType != null && scopeType.endsWith(".Memory"))) {
                        return "int";
                    }
                }
            }
            if (mName.equals("loadI64")) {
                if (call.getScope().isPresent()) {
                    String scopeStr = call.getScope().get().toString();
                    String scopeType = getExpressionJavaType(call.getScope().get(), localJavaTypes);
                    if ("Memory".equals(scopeStr) || "Memory".equals(scopeType) || (scopeType != null && scopeType.endsWith(".Memory"))) {
                        return "long";
                    }
                }
            }
            if (mName.equals("loadF32")) {
                if (call.getScope().isPresent()) {
                    String scopeStr = call.getScope().get().toString();
                    String scopeType = getExpressionJavaType(call.getScope().get(), localJavaTypes);
                    if ("Memory".equals(scopeStr) || "Memory".equals(scopeType) || (scopeType != null && scopeType.endsWith(".Memory"))) {
                        return "float";
                    }
                }
            }
            if (mName.equals("loadF64")) {
                if (call.getScope().isPresent()) {
                    String scopeStr = call.getScope().get().toString();
                    String scopeType = getExpressionJavaType(call.getScope().get(), localJavaTypes);
                    if ("Memory".equals(scopeStr) || "Memory".equals(scopeType) || (scopeType != null && scopeType.endsWith(".Memory"))) {
                        return "double";
                    }
                }
            }
            if (mName.equals("named") || (mName.equals("of") && call.getScope().isPresent() && call.getScope().get().toString().contains("Memory"))) {
                return "Memory";
            }
            if (mName.equals("getName") && call.getScope().isPresent()) {
                String scopeType = getExpressionJavaType(call.getScope().get(), localJavaTypes);
                if ("Memory".equals(scopeType) || (scopeType != null && scopeType.endsWith(".Memory"))) {
                    return "String";
                }
            }
            if (call.getScope().isPresent()) {
                String scopeType = getExpressionJavaType(call.getScope().get(), localJavaTypes);
                if (scopeType != null && classRegistry != null && classRegistry.isRecord(scopeType)) {
                    RecordModel rm = classRegistry.getRecord(scopeType);
                    if (rm.getComponent(mName) != null) {
                        return rm.getComponent(mName).getTypeName();
                    }
                    MethodModel mm = resolveMethodCall(rm.getName(), call, localJavaTypes);
                    if (mm != null) return mm.getReturnTypeName();
                } else if (scopeType != null && classRegistry != null && classRegistry.hasClass(scopeType)) {
                    MethodModel mm = resolveMethodCall(scopeType, call, localJavaTypes);
                    if (mm != null) return mm.getReturnTypeName();
                }
            } else if (currentClass != null) {
                MethodModel mm = classRegistry != null ? resolveMethodCall(currentClass.getName(), call, localJavaTypes)
                        : currentClass.getMethodByName(mName);
                if (mm != null) return mm.getReturnTypeName();
            } else if (currentRecord != null) {
                if (currentRecord.getComponent(mName) != null) {
                    return currentRecord.getComponent(mName).getTypeName();
                }
                MethodModel mm = resolveMethodCall(currentRecord.getName(), call, localJavaTypes);
                if (mm != null) return mm.getReturnTypeName();
            }
        }
        if (expr.isArrayAccessExpr()) {
            ArrayAccessExpr aae = expr.asArrayAccessExpr();
            String arrType = getExpressionJavaType(aae.getName(), localJavaTypes);
            if (arrType != null && arrType.endsWith("[]")) {
                return arrType.substring(0, arrType.length() - 2);
            }
        }
        if (expr.isFieldAccessExpr()) {
            FieldAccessExpr fae = expr.asFieldAccessExpr();
            String scopeType = getExpressionJavaType(fae.getScope(), localJavaTypes);
            if (scopeType != null && classRegistry != null && classRegistry.isRecord(scopeType)) {
                FieldModel fm = classRegistry.getRecord(scopeType).getComponent(fae.getNameAsString());
                if (fm != null) return fm.getTypeName();
            } else if (scopeType != null && classRegistry != null && classRegistry.hasClass(scopeType)) {
                FieldModel fm = classRegistry.getClass(scopeType).getField(fae.getNameAsString());
                if (fm != null) return fm.getTypeName();
            }
        }
        return null;
    }

    private void translateDomStringArg(Expression arg, StringBuilder collector, Map<String, String> localTypes, Map<String, String> localElementTypes, Map<String, String> localJavaTypes) {
        if (arg.isStringLiteralExpr()) {
            String val = arg.asStringLiteralExpr().getValue();
            StringConstant sc = stringPool != null ? stringPool.get(val) : null;
            if (sc != null) {
                collector.append("    i32.const ").append(sc.offset).append("\n");
                collector.append("    i32.const ").append(sc.length).append("\n");
            } else {
                collector.append("    i32.const 0\n    i32.const 0\n");
            }
        } else if (arg.isFieldAccessExpr() && (arg.asFieldAccessExpr().getScope().toString().contains("Terminal") || arg.asFieldAccessExpr().getScope().toString().contains("WASITerminal"))) {
            String fName = arg.asFieldAccessExpr().getNameAsString();
            String ansi = switch (fName) {
                case "RESET" -> "\u001B[0m";
                case "BLACK" -> "\u001B[30m";
                case "RED" -> "\u001B[31m";
                case "GREEN" -> "\u001B[32m";
                case "YELLOW" -> "\u001B[33m";
                case "BLUE" -> "\u001B[34m";
                case "MAGENTA" -> "\u001B[35m";
                case "CYAN" -> "\u001B[36m";
                case "WHITE" -> "\u001B[37m";
                case "BRIGHT_BLACK" -> "\u001B[90m";
                case "BRIGHT_RED" -> "\u001B[91m";
                case "BRIGHT_GREEN" -> "\u001B[92m";
                case "BRIGHT_YELLOW" -> "\u001B[93m";
                case "BRIGHT_BLUE" -> "\u001B[94m";
                case "BRIGHT_MAGENTA" -> "\u001B[95m";
                case "BRIGHT_CYAN" -> "\u001B[96m";
                case "BRIGHT_WHITE" -> "\u001B[97m";
                case "BOLD" -> "\u001B[1m";
                case "DIM" -> "\u001B[2m";
                case "ITALIC" -> "\u001B[3m";
                case "UNDERLINE" -> "\u001B[4m";
                case "REVERSE" -> "\u001B[7m";
                default -> "";
            };
            StringConstant sc = stringPool != null ? stringPool.get(ansi) : null;
            if (sc != null) {
                collector.append("    i32.const ").append(sc.offset).append("\n");
                collector.append("    i32.const ").append(sc.length).append("\n");
            } else {
                collector.append("    i32.const 0\n    i32.const 0\n");
            }
        } else {
            translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "String");
            collector.append("    local.set $__str_arg_ptr\n");
            collector.append("    local.get $__str_arg_ptr\n");
            collector.append("    local.get $__str_arg_ptr\n");
            collector.append("    call $__str_length\n");
        }
    }

    private boolean expressionLeavesValueOnStack(Expression expr, Map<String, String> localJavaTypes) {
        if (expr.isMethodCallExpr()) {
            MethodCallExpr call = expr.asMethodCallExpr();
            MethodModel source = sourceMethod(call, localJavaTypes);
            if (source != null) return source.getReturnWasmType() != null;
            String mName = call.getNameAsString();
            if (mName.equals("append") || mName.equals("toString") || mName.equals("substring") ||
                    mName.equals("concat") || mName.equals("length") || mName.equals("charAt") ||
                    mName.equals("indexOf") || mName.equals("equals") || mName.equals("isEmpty") ||
                    mName.equals("getElementById") || mName.equals("createElement") || mName.equals("getBody") ||
                    mName.equals("querySelector") || mName.equals("querySelectorAll") ||
                    mName.equals("getContext2D") || mName.equals("getContext") || mName.equals("getCanvasContext2D") ||
                    mName.equals("getWebGLContext") || mName.equals("getWidth") || mName.equals("getHeight") ||
                    mName.equals("getFillStyle") || mName.equals("getStrokeStyle") || mName.equals("getLineWidth") ||
                    mName.equals("getFont") || mName.equals("getTextAlign") || mName.equals("getGlobalAlpha") ||
                    mName.equals("getInnerText") || mName.equals("getAttribute") || mName.equals("getParent") ||
                    mName.equals("getValue") || mName.equals("isChecked") || mName.equals("getStyle") ||
                    mName.equals("hasClass") || mName.equals("getClassList") ||
                    mName.equals("contains") || mName.equals("setTimeout") || mName.equals("setInterval") ||
                    mName.equals("requestAnimationFrame") || mName.equals("confirm") || mName.equals("prompt") ||
                    mName.equals("get") || mName.equals("getAndSet") || mName.equals("compareAndSet") ||
                    mName.equals("getAndAdd") || mName.equals("addAndGet") || mName.equals("getAndIncrement") ||
                    mName.equals("incrementAndGet") || mName.equals("getAndDecrement") || mName.equals("decrementAndGet") ||
                    mName.equals("getAndAnd") || mName.equals("getAndOr") || mName.equals("getAndXor") ||
                    mName.equals("load") || mName.equals("exchange") || mName.equals("compareExchange") ||
                    mName.equals("wait") || mName.equals("notify") || mName.equals("spawn") ||
                    mName.equals("currentThreadId") || mName.equals("hardwareConcurrency") ||
                    mName.equals("copyOf") || mName.equals("copyOfRange") ||
                    mName.equals("min") || mName.equals("max") || mName.equals("abs") ||
                    mName.equals("sqrt") || mName.equals("floor") || mName.equals("ceil") ||
                    mName.equals("readString") || mName.equals("writeString") || mName.equals("appendString") ||
                    mName.equals("fileExists") || mName.equals("exists") || mName.equals("fileLength") ||
                    mName.equals("fileSize") || mName.equals("length") || mName.equals("deleteFile") ||
                    mName.equals("removeFile") || mName.equals("delete") || mName.equals("createDirectory") ||
                    mName.equals("mkdir") || mName.equals("open") || mName.equals("close") ||
                    mName.equals("seek") || mName.equals("currentTimeMillis") || mName.equals("clockTimeGet") ||
                    mName.equals("randomInt") || mName.equals("randomGet") || mName.equals("getPath") ||
                    mName.equals("of") || mName.equals("splat") || mName.equals("zero") || mName.equals("dot") ||
                    mName.equals("sum") || mName.equals("withLane") || mName.equals("replaceLane") ||
                    mName.equals("withX") || mName.equals("withY") || mName.equals("withZ") || mName.equals("withW") ||
                    mName.equals("shiftLeft") || mName.equals("shiftRight") || mName.equals("shl") || mName.equals("shr") ||
                    mName.equals("not") || mName.equals("and") || mName.equals("or") || mName.equals("xor") ||
                    mName.equals("getX") || mName.equals("getY") || mName.equals("getZ") || mName.equals("getW") ||
                    mName.equals("extractLane") || mName.equals("add") || mName.equals("sub") ||
                    mName.equals("mul") || mName.equals("div") || mName.equals("neg") ||
                    mName.equals("loadI32") || mName.equals("loadI64") || mName.equals("loadF32") ||
                    mName.equals("loadF64") || mName.equals("loadI8") || mName.equals("loadU8") ||
                    mName.equals("loadI16") || mName.equals("loadU16") || mName.equals("size") ||
                    mName.equals("grow") || mName.equals("getName") || mName.equals("getIndex")) {
                return true;
            }
            if (call.getScope().isPresent()) {
                String targetType = getExpressionJavaType(call.getScope().get(), localJavaTypes);
                if (targetType != null && classRegistry != null && classRegistry.hasClass(targetType)) {
                    MethodModel mm = resolveMethodCall(targetType, call, localJavaTypes);
                    if (mm != null && mm.getReturnWasmType() != null) return true;
                }
            } else if (currentClass != null) {
                MethodModel mm = resolveMethodCall(currentClass.getName(), call, localJavaTypes);
                if (mm != null && mm.getReturnWasmType() != null) return true;
            }
        }
        if (expr.isObjectCreationExpr()) {
            return true;
        }
        return false;
    }

    private boolean isStringType(Expression expr, Map<String, String> localJavaTypes) {
        if (expr == null) return false;
        String jType = getExpressionJavaType(expr, localJavaTypes);
        if ("String".equals(jType) || "java.lang.String".equals(jType) || "CharSequence".equals(jType) || "java.lang.CharSequence".equals(jType)) return true;
        if (expr.isStringLiteralExpr()) return true;
        if (expr.isNameExpr()) {
            String name = expr.asNameExpr().getNameAsString();
            String jt = localJavaTypes != null ? localJavaTypes.get(name) : null;
            if ("String".equals(jt) || "java.lang.String".equals(jt) || "CharSequence".equals(jt) || "java.lang.CharSequence".equals(jt)) return true;
        }
        if (expr.isBinaryExpr()) {
            BinaryExpr b = expr.asBinaryExpr();
            if (b.getOperator() == BinaryExpr.Operator.PLUS) {
                return isStringType(b.getLeft(), localJavaTypes) || isStringType(b.getRight(), localJavaTypes);
            }
        }
        if (expr.isMethodCallExpr()) {
            MethodCallExpr m = expr.asMethodCallExpr();
            String mName = m.getNameAsString();
            if (mName.equals("toString") || mName.equals("substring") || mName.equals("concat") ||
                    mName.equals("trim") || mName.equals("strip") || mName.equals("stripLeading") || mName.equals("stripTrailing") ||
                    mName.equals("toLowerCase") || mName.equals("toUpperCase") || mName.equals("repeat") || mName.equals("replace") ||
                    mName.equals("name") ||
                    mName.equals("getInnerText") || mName.equals("getAttribute") ||
                    mName.equals("getValue") || mName.equals("getStyle") ||
                    mName.equals("getFillStyle") || mName.equals("getStrokeStyle") ||
                    mName.equals("getFont") || mName.equals("getTextAlign") ||
                    mName.equals("getString") || mName.equals("callString") || mName.equals("asString") ||
                    mName.equals("readLine") || mName.equals("lineSeparator") || mName.equals("getenv") || mName.equals("getProperty") ||
                    mName.equals("prompt") || "String".equals(getExpressionJavaType(expr, localJavaTypes))) return true;
        }
        if (expr.isFieldAccessExpr()) {
            FieldAccessExpr fae = expr.asFieldAccessExpr();
            String scope = fae.getScope().toString();
            if (scope.equals("WASITerminal") || scope.endsWith(".WASITerminal") || scope.equals("Terminal") || scope.endsWith(".Terminal")) {
                return true;
            }
        }
        if (expr.isEnclosedExpr()) {
            return isStringType(expr.asEnclosedExpr().getInner(), localJavaTypes);
        }
        return false;
    }

    private boolean isStringBuilderType(Expression expr, Map<String, String> localJavaTypes) {
        if (expr == null) return false;
        if (expr.isObjectCreationExpr()) {
            String t = expr.asObjectCreationExpr().getTypeAsString();
            return t.equals("StringBuilder") || t.endsWith(".StringBuilder");
        }
        if (expr.isNameExpr()) {
            String var = expr.asNameExpr().getNameAsString();
            String type = localJavaTypes != null ? localJavaTypes.get(var) : null;
            return "StringBuilder".equals(type) || (type != null && type.endsWith(".StringBuilder"));
        }
        if (expr.isMethodCallExpr()) {
            MethodCallExpr call = expr.asMethodCallExpr();
            String mName = call.getNameAsString();
            if (mName.equals("append") || mName.equals("insert") || mName.equals("delete") ||
                mName.equals("deleteCharAt") || mName.equals("reverse") || mName.equals("replace")) {
                Expression scope = call.getScope().orElse(null);
                if (scope != null && isStringBuilderType(scope, localJavaTypes)) return true;
            }
        }
        String type = getExpressionJavaType(expr, localJavaTypes);
        return "StringBuilder".equals(type) || (type != null && type.endsWith(".StringBuilder"));
    }

    private void translateStringToStack(Expression expr, StringBuilder collector, Map<String, String> localTypes, Map<String, String> localElementTypes, Map<String, String> localJavaTypes) {
        String exprType = inferType(expr, localTypes, localElementTypes, localJavaTypes, null);
        if (isStringType(expr, localJavaTypes)) {
            translateExpression(expr, collector, localTypes, localElementTypes, localJavaTypes, "String");
        } else if ("f64".equals(exprType) || expr.isDoubleLiteralExpr()) {
            translateExpression(expr, collector, localTypes, localElementTypes, localJavaTypes, "f64");
            collector.append("    call $__str_from_f64\n");
        } else if ("f32".equals(exprType)) {
            translateExpression(expr, collector, localTypes, localElementTypes, localJavaTypes, "f32");
            collector.append("    f64.promote_f32\n");
            collector.append("    call $__str_from_f64\n");
        } else if (expr.isCharLiteralExpr() || (expr.isNameExpr() && "char".equals(localJavaTypes.get(expr.asNameExpr().getNameAsString())))) {
            translateExpression(expr, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    call $__str_from_char\n");
        } else if (expr.isBooleanLiteralExpr() || (expr.isNameExpr() && "boolean".equals(localJavaTypes.get(expr.asNameExpr().getNameAsString())))) {
            translateExpression(expr, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    call $__str_from_bool\n");
        } else {
            translateExpression(expr, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    call $__str_from_i32\n");
        }
    }

    private boolean isLog4jCall(MethodCallExpr call, Map<String, String> localJavaTypes) {
        if (!call.getScope().isPresent()) return false;
        String scopeStr = call.getScope().get().toString();
        String mName = call.getNameAsString();
        if (scopeStr.equals("LogManager") || scopeStr.endsWith(".LogManager")) {
            return true;
        }
        if (scopeStr.equals("ThreadContext") || scopeStr.endsWith(".ThreadContext")) {
            return true;
        }
        if (scopeStr.equals("Level") || scopeStr.endsWith(".Level")) {
            return mName.equals("getLevel") || mName.equals("valueOf") || mName.equals("name") || mName.equals("intLevel");
        }
        if (scopeStr.equals("SimpleLogger") || scopeStr.endsWith(".SimpleLogger")) {
            return true;
        }
        String scopeJavaType = getExpressionJavaType(call.getScope().get(), localJavaTypes);
        if ("Logger".equals(scopeJavaType) || "SimpleLogger".equals(scopeJavaType) || (scopeJavaType != null && scopeJavaType.endsWith(".Logger")) ||
            scopeStr.toLowerCase().contains("logger") || scopeStr.toLowerCase().contains("log")) {
            return true;
        }
        return false;
    }

    private void translateLog4jCall(MethodCallExpr call, StringBuilder collector, Map<String, String> localTypes, Map<String, String> localElementTypes, Map<String, String> localJavaTypes, String expectedType) {
        String mName = call.getNameAsString();
        Expression scope = call.getScope().orElse(null);
        String scopeStr = scope != null ? scope.toString() : "";

        if (scopeStr.equals("LogManager") || scopeStr.endsWith(".LogManager")) {
            if (mName.equals("getLogger") || mName.equals("getRootLogger") || mName.equals("getFormatterLogger")) {
                if (call.getArguments().size() > 0) {
                    translateDomStringArg(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes);
                } else {
                    collector.append("    i32.const 0\n");
                }
                return;
            }
            if (mName.equals("getRootLevel")) {
                collector.append("    i32.const 400\n");
                return;
            }
            if (mName.equals("setRootLevel")) {
                return;
            }
        }

        if (scopeStr.equals("ThreadContext") || scopeStr.endsWith(".ThreadContext")) {
            if (mName.equals("get") || mName.equals("containsKey") || mName.equals("formatContext")) {
                collector.append("    i32.const 0\n");
                return;
            }
            return;
        }

        if (scopeStr.equals("SimpleLogger") || scopeStr.endsWith(".SimpleLogger")) {
            if (mName.equals("clearHistory") || mName.equals("addListener") || mName.equals("removeListener")) {
                return;
            }
            if (mName.equals("getLogHistory")) {
                collector.append("    i32.const 0\n");
                return;
            }
        }

        if (scopeStr.equals("Level") || scopeStr.endsWith(".Level")) {
            if (mName.equals("intLevel")) {
                collector.append("    i32.const 400\n");
                return;
            }
            if (mName.equals("name") || mName.equals("toString")) {
                translateDomStringArg(new com.github.javaparser.ast.expr.StringLiteralExpr("INFO"), collector, localTypes, localElementTypes, localJavaTypes);
                return;
            }
            if (mName.equals("getLevel") || mName.equals("valueOf")) {
                collector.append("    i32.const 400\n");
                return;
            }
        }

        if (mName.equals("isInfoEnabled") || mName.equals("isDebugEnabled") || mName.equals("isWarnEnabled") ||
            mName.equals("isErrorEnabled") || mName.equals("isTraceEnabled") || mName.equals("isFatalEnabled") ||
            mName.equals("isEnabled")) {
            collector.append("    i32.const 1\n");
            return;
        }

        if (mName.equals("getName")) {
            translateDomStringArg(new com.github.javaparser.ast.expr.StringLiteralExpr("logger"), collector, localTypes, localElementTypes, localJavaTypes);
            return;
        }

        if (mName.equals("getLevel")) {
            collector.append("    i32.const 400\n");
            return;
        }

        if (mName.equals("setLevel")) {
            return;
        }

        if (mName.equals("info") || mName.equals("warn") || mName.equals("error") ||
            mName.equals("debug") || mName.equals("trace") || mName.equals("fatal") || mName.equals("log")) {

            String levelName = mName.toUpperCase();
            int startArg = 0;
            if (mName.equals("log") && call.getArguments().size() > 0) {
                startArg = 1;
                levelName = "LOG";
            }
            int fd = (mName.equals("error") || mName.equals("fatal")) ? 2 : 1;

            collector.append("    i32.const ").append(fd).append("\n");
            translateDomStringArg(new com.github.javaparser.ast.expr.StringLiteralExpr("[" + levelName + "] "), collector, localTypes, localElementTypes, localJavaTypes);
            collector.append("    i32.const 0\n");
            collector.append("    call $__wasi_print_str\n");

            int numArgs = call.getArguments().size();
            if (numArgs <= startArg) {
                collector.append("    i32.const ").append(fd).append("\n");
                collector.append("    i32.const 0\n");
                collector.append("    i32.const 1\n");
                collector.append("    call $__wasi_print_str\n");
                return;
            }

            if (numArgs == startArg + 1) {
                translateWasiPrintArgument(call.getArgument(startArg), fd, true, collector, localTypes, localElementTypes, localJavaTypes);
            } else if (call.getArgument(startArg).isStringLiteralExpr() && call.getArgument(startArg).asStringLiteralExpr().getValue().contains("{}")) {
                String pattern = call.getArgument(startArg).asStringLiteralExpr().getValue();
                String[] pieces = pattern.split("\\{\\}", -1);
                int argIndex = startArg + 1;
                for (int i = 0; i < pieces.length; i++) {
                    if (!pieces[i].isEmpty()) {
                        collector.append("    i32.const ").append(fd).append("\n");
                        translateDomStringArg(new com.github.javaparser.ast.expr.StringLiteralExpr(pieces[i]), collector, localTypes, localElementTypes, localJavaTypes);
                        boolean isEnd = (i == pieces.length - 1 && argIndex >= numArgs);
                        collector.append("    i32.const ").append(isEnd ? 1 : 0).append("\n");
                        collector.append("    call $__wasi_print_str\n");
                    }
                    if (argIndex < numArgs && i < pieces.length - 1) {
                        Expression nextArg = call.getArgument(argIndex++);
                        boolean isEnd = (i == pieces.length - 2 && pieces[pieces.length - 1].isEmpty() && argIndex >= numArgs);
                        translateWasiPrintArgument(nextArg, fd, isEnd, collector, localTypes, localElementTypes, localJavaTypes);
                    }
                }
                while (argIndex < numArgs) {
                    collector.append("    i32.const ").append(fd).append("\n");
                    translateDomStringArg(new com.github.javaparser.ast.expr.StringLiteralExpr(" "), collector, localTypes, localElementTypes, localJavaTypes);
                    collector.append("    i32.const 0\n");
                    collector.append("    call $__wasi_print_str\n");

                    boolean isEnd = (argIndex == numArgs - 1);
                    translateWasiPrintArgument(call.getArgument(argIndex++), fd, isEnd, collector, localTypes, localElementTypes, localJavaTypes);
                }
            } else {
                translateWasiPrintArgument(call.getArgument(startArg), fd, false, collector, localTypes, localElementTypes, localJavaTypes);

                for (int i = startArg + 1; i < numArgs; i++) {
                    collector.append("    i32.const ").append(fd).append("\n");
                    translateDomStringArg(new com.github.javaparser.ast.expr.StringLiteralExpr(" "), collector, localTypes, localElementTypes, localJavaTypes);
                    collector.append("    i32.const 0\n");
                    collector.append("    call $__wasi_print_str\n");

                    translateWasiPrintArgument(call.getArgument(i), fd, (i == numArgs - 1), collector, localTypes, localElementTypes, localJavaTypes);
                }
            }
            return;
        }
    }

    private void translateWasiPrintArgument(Expression arg, int fd, boolean isNewline, StringBuilder collector, Map<String, String> localTypes, Map<String, String> localElementTypes, Map<String, String> localJavaTypes) {
        String argType = inferType(arg, localTypes, localElementTypes, localJavaTypes, null);
        String argJavaType = getExpressionJavaType(arg, localJavaTypes);

        collector.append("    i32.const ").append(fd).append("\n");
        if (isStringType(arg, localJavaTypes) || "String".equals(argJavaType) || "String".equals(argType)) {
            translateDomStringArg(arg, collector, localTypes, localElementTypes, localJavaTypes);
            collector.append("    i32.const ").append(isNewline ? 1 : 0).append("\n");
            collector.append("    call $__wasi_print_str\n");
        } else if ("f64".equals(argType) || "double".equals(argJavaType) || "float".equals(argJavaType) || arg.isDoubleLiteralExpr()) {
            translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "f64");
            collector.append("    i32.const ").append(isNewline ? 1 : 0).append("\n");
            collector.append("    call $__wasi_print_double\n");
        } else if ("boolean".equals(argJavaType) || (arg.isBooleanLiteralExpr())) {
            translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    i32.const ").append(isNewline ? 1 : 0).append("\n");
            collector.append("    call $__wasi_print_bool\n");
        } else if ("char".equals(argJavaType) || (arg.isCharLiteralExpr())) {
            translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    i32.const ").append(isNewline ? 1 : 0).append("\n");
            collector.append("    call $__wasi_print_char\n");
        } else {
            translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    i32.const ").append(isNewline ? 1 : 0).append("\n");
            collector.append("    call $__wasi_print_int\n");
        }
    }

    private boolean isSystemOrWasiCall(MethodCallExpr call, Map<String, String> localJavaTypes) {
        if (!call.getScope().isPresent()) return false;
        String scopeStr = call.getScope().get().toString();
        String mName = call.getNameAsString();
        if (scopeStr.equals("System.out") || scopeStr.equals("System.err")) {
            return mName.equals("println") || mName.equals("print");
        }
        if (scopeStr.equals("System") || scopeStr.equals("java.lang.System")) {
            return mName.equals("exit") || mName.equals("currentTimeMillis") || mName.equals("nanoTime") ||
                    mName.equals("identityHashCode") || mName.equals("lineSeparator") ||
                    mName.equals("getenv") || mName.equals("getProperty");
        }
        if (scopeStr.equals("WASI") || scopeStr.endsWith(".WASI") ||
            scopeStr.equals("WASIFile") || scopeStr.endsWith(".WASIFile") ||
            scopeStr.equals("WASITerminal") || scopeStr.endsWith(".WASITerminal") ||
            scopeStr.equals("Terminal") || scopeStr.endsWith(".Terminal")) {
            return true;
        }
        String scopeJavaType = getExpressionJavaType(call.getScope().get(), localJavaTypes);
        if ("WASIFile".equals(scopeJavaType)) {
            return true;
        }
        return false;
    }

    private void translateSystemOrWasiCall(MethodCallExpr call, StringBuilder collector, Map<String, String> localTypes, Map<String, String> localElementTypes, Map<String, String> localJavaTypes, String expectedType) {
        String mName = call.getNameAsString();
        Expression scope = call.getScope().orElse(null);
        String scopeStr = scope != null ? scope.toString() : "";
        String scopeJavaType = scope != null ? getExpressionJavaType(scope, localJavaTypes) : null;
        boolean isWasiFileInstance = "WASIFile".equals(scopeJavaType);

        if (mName.equals("exit") || mName.equals("procExit")) {
            if (call.getArguments().size() > 0) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            } else {
                collector.append("    i32.const 0\n");
            }
            collector.append("    call $__wasi_proc_exit\n");
            return;
        }

        if (mName.equals("identityHashCode")) {
            if (call.getArguments().size() > 0) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            } else {
                collector.append("    i32.const 0\n");
            }
            collector.append("    call $__system_identity_hash_code\n");
            return;
        }

        if (mName.equals("lineSeparator")) {
            collector.append("    call $__system_line_separator\n");
            return;
        }

        if (mName.equals("getenv")) {
            if (call.getArguments().size() > 0) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "String");
                collector.append("    call $__system_getenv\n");
            } else {
                collector.append("    i32.const 0\n");
            }
            return;
        }

        if (mName.equals("getProperty")) {
            if (call.getArguments().size() == 1) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "String");
                collector.append("    call $__system_get_property\n");
            } else if (call.getArguments().size() >= 2) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "String");
                translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "String");
                collector.append("    call $__system_get_property_default\n");
            } else {
                collector.append("    i32.const 0\n");
            }
            return;
        }

        if (mName.equals("fdWrite")) {
            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            translateDomStringArg(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes);
            collector.append("    i32.const 0\n");
            collector.append("    call $__wasi_print_str\n");
            return;
        }

        if (mName.equals("readString")) {
            if (isWasiFileInstance) {
                translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    i32.load offset=4\n");
            } else {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "String");
            }
            collector.append("    call $__wasi_read_file_str\n");
            return;
        }

        if (mName.equals("writeString")) {
            if (isWasiFileInstance) {
                translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    i32.load offset=4\n");
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "String");
            } else {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "String");
                translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "String");
            }
            collector.append("    i32.const 0\n");
            collector.append("    call $__wasi_write_file_str\n");
            return;
        }

        if (mName.equals("appendString")) {
            if (isWasiFileInstance) {
                translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    i32.load offset=4\n");
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "String");
            } else {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "String");
                translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "String");
            }
            collector.append("    i32.const 1\n");
            collector.append("    call $__wasi_write_file_str\n");
            return;
        }

        if (mName.equals("fileExists") || mName.equals("exists")) {
            if (isWasiFileInstance) {
                translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    i32.load offset=4\n");
            } else {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "String");
            }
            collector.append("    call $__wasi_file_exists\n");
            return;
        }

        if (mName.equals("fileLength") || mName.equals("fileSize") || mName.equals("length")) {
            if (isWasiFileInstance) {
                translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    i32.load offset=4\n");
            } else {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "String");
            }
            collector.append("    call $__wasi_file_length\n");
            return;
        }

        if (mName.equals("deleteFile") || mName.equals("removeFile") || mName.equals("delete")) {
            if (isWasiFileInstance) {
                translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    i32.load offset=4\n");
            } else {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "String");
            }
            collector.append("    call $__wasi_file_delete\n");
            return;
        }

        if (mName.equals("createDirectory") || mName.equals("mkdir")) {
            if (isWasiFileInstance) {
                translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    i32.load offset=4\n");
            } else {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "String");
            }
            collector.append("    call $__wasi_create_dir\n");
            return;
        }

        if (mName.equals("getPath") && isWasiFileInstance) {
            translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    i32.load offset=4\n");
            return;
        }

        if (mName.equals("currentTimeMillis")) {
            if ("f64".equals(expectedType) || "double".equals(expectedType) || scopeStr.equals("WASI") || scopeStr.endsWith(".WASI")) {
                collector.append("    call $__wasi_clock_time_ms\n");
            } else {
                collector.append("    call $__wasi_clock_time_ms_i64\n");
            }
            return;
        }

        if (mName.equals("clockTimeGet")) {
            collector.append("    call $__wasi_clock_time_ms\n");
            return;
        }

        if (mName.equals("nanoTime")) {
            collector.append("    call $__wasi_clock_time_ns\n");
            return;
        }

        if (mName.equals("randomInt") || mName.equals("randomGet")) {
            collector.append("    call $__wasi_random_int\n");
            return;
        }

        if (mName.equals("open")) {
            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "String");
            translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    call $__wasi_path_open_helper\n");
            return;
        }

        if (mName.equals("close")) {
            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    call $__wasi_fd_close\n");
            return;
        }

        if (mName.equals("seek")) {
            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    i64.extend_i32_s\n");
            translateExpression(call.getArgument(2), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    i32.const 24\n");
            collector.append("    call $__wasi_fd_seek\n");
            collector.append("    drop\n");
            collector.append("    i32.const 24\n");
            collector.append("    i32.load\n");
            return;
        }

        if (mName.equals("readLine")) {
            if (call.getArguments().size() > 0) {
                translateDomStringArg(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes);
            } else {
                collector.append("    i32.const 0\n");
            }
            collector.append("    call $__wasi_read_line\n");
            return;
        }

        if (mName.equals("readChar")) {
            collector.append("    call $__wasi_read_char\n");
            return;
        }

        if (mName.equals("clear") && (scopeStr.contains("Terminal") || scopeStr.equals("Terminal") || scopeStr.equals("WASITerminal"))) {
            collector.append("    call $dom_terminal_clear\n");
            return;
        }

        if (mName.equals("setColor") && (scopeStr.contains("Terminal") || scopeStr.equals("Terminal") || scopeStr.equals("WASITerminal"))) {
            collector.append("    i32.const 1\n");
            translateDomStringArg(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes);
            collector.append("    i32.const 0\n");
            collector.append("    call $__wasi_print_str\n");
            return;
        }

        if ((mName.equals("resetColor") || mName.equals("reset")) && (scopeStr.contains("Terminal") || scopeStr.equals("Terminal") || scopeStr.equals("WASITerminal"))) {
            collector.append("    i32.const 1\n");
            translateDomStringArg(new com.github.javaparser.ast.expr.StringLiteralExpr("\u001b[0m"), collector, localTypes, localElementTypes, localJavaTypes);
            collector.append("    i32.const 0\n");
            collector.append("    call $__wasi_print_str\n");
            return;
        }

        if (mName.equals("getColumns")) {
            collector.append("    call $dom_terminal_get_columns\n");
            return;
        }

        if (mName.equals("getRows")) {
            collector.append("    call $dom_terminal_get_rows\n");
            return;
        }

        if (mName.equals("setCursor") && (scopeStr.contains("Terminal") || scopeStr.equals("Terminal") || scopeStr.equals("WASITerminal"))) {
            collector.append("    i32.const 1\n");
            translateDomStringArg(new com.github.javaparser.ast.expr.StringLiteralExpr("\u001b["), collector, localTypes, localElementTypes, localJavaTypes);
            collector.append("    i32.const 0\n");
            collector.append("    call $__wasi_print_str\n");

            collector.append("    i32.const 1\n");
            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    i32.const 0\n");
            collector.append("    call $__wasi_print_int\n");

            collector.append("    i32.const 1\n");
            translateDomStringArg(new com.github.javaparser.ast.expr.StringLiteralExpr(";"), collector, localTypes, localElementTypes, localJavaTypes);
            collector.append("    i32.const 0\n");
            collector.append("    call $__wasi_print_str\n");

            collector.append("    i32.const 1\n");
            translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    i32.const 0\n");
            collector.append("    call $__wasi_print_int\n");

            collector.append("    i32.const 1\n");
            translateDomStringArg(new com.github.javaparser.ast.expr.StringLiteralExpr("H"), collector, localTypes, localElementTypes, localJavaTypes);
            collector.append("    i32.const 0\n");
            collector.append("    call $__wasi_print_str\n");
            return;
        }

        int fd = (scopeStr.contains("err") || mName.startsWith("eprint")) ? 2 : 1;
        boolean isNewline = mName.endsWith("println") || mName.equals("eprintln");

        if (call.getArguments().isEmpty()) {
            collector.append("    i32.const ").append(fd).append("\n");
            collector.append("    call $__wasi_println_empty\n");
            return;
        }

        Expression arg = call.getArgument(0);
        String argType = inferType(arg, localTypes, localElementTypes, localJavaTypes, null);
        String argJavaType = getExpressionJavaType(arg, localJavaTypes);

        if (isStringType(arg, localJavaTypes) || "String".equals(argJavaType) || "String".equals(argType)) {
            collector.append("    i32.const ").append(fd).append("\n");
            translateDomStringArg(arg, collector, localTypes, localElementTypes, localJavaTypes);
            collector.append("    i32.const ").append(isNewline ? 1 : 0).append("\n");
            collector.append("    call $__wasi_print_str\n");
        } else if ("f64".equals(argType) || "double".equals(argJavaType) || "float".equals(argJavaType) || arg.isDoubleLiteralExpr()) {
            collector.append("    i32.const ").append(fd).append("\n");
            translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "f64");
            collector.append("    i32.const ").append(isNewline ? 1 : 0).append("\n");
            collector.append("    call $__wasi_print_double\n");
        } else if ("boolean".equals(argJavaType) || (arg.isBooleanLiteralExpr())) {
            collector.append("    i32.const ").append(fd).append("\n");
            translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    i32.const ").append(isNewline ? 1 : 0).append("\n");
            collector.append("    call $__wasi_print_bool\n");
        } else if ("char".equals(argJavaType) || (arg.isCharLiteralExpr())) {
            collector.append("    i32.const ").append(fd).append("\n");
            translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    i32.const ").append(isNewline ? 1 : 0).append("\n");
            collector.append("    call $__wasi_print_char\n");
        } else {
            collector.append("    i32.const ").append(fd).append("\n");
            translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    i32.const ").append(isNewline ? 1 : 0).append("\n");
            collector.append("    call $__wasi_print_int\n");
        }
    }

    private boolean isArrayUtilsCall(MethodCallExpr call) {
        if (!call.getScope().isPresent()) return false;
        String scopeStr = call.getScope().get().toString();
        String mName = call.getNameAsString();
        if ((scopeStr.equals("System") || scopeStr.endsWith(".System")) && mName.equals("arraycopy")) {
            return true;
        }
        if (scopeStr.equals("Arrays") || scopeStr.endsWith(".Arrays") || scopeStr.equals("java.util.Arrays")) {
            return mName.equals("fill") || mName.equals("copyOf") || mName.equals("copyOfRange") || mName.equals("equals");
        }
        return false;
    }

    private void translateArrayUtilsCall(MethodCallExpr call, StringBuilder collector, Map<String, String> localTypes, Map<String, String> localElementTypes, Map<String, String> localJavaTypes) {
        String mName = call.getNameAsString();
        if (mName.equals("arraycopy")) {
            Expression src = call.getArgument(0);
            Expression srcPos = call.getArgument(1);
            Expression dest = call.getArgument(2);
            Expression destPos = call.getArgument(3);
            Expression length = call.getArgument(4);

            String srcElem = getArrayElementType(src, localElementTypes, localJavaTypes);
            String destElem = getArrayElementType(dest, localElementTypes, localJavaTypes);
            boolean isF64 = "f64".equals(srcElem) || "f64".equals(destElem) || "double".equals(srcElem) || "double".equals(destElem);

            int headerOffset = isF64 ? 8 : 4;
            int elemShift = isF64 ? 3 : 2;

            // 1. dest address
            translateExpression(dest, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    i32.const ").append(headerOffset).append("\n    i32.add\n");
            translateExpression(destPos, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    i32.const ").append(elemShift).append("\n    i32.shl\n    i32.add\n");

            // 2. src address
            translateExpression(src, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    i32.const ").append(headerOffset).append("\n    i32.add\n");
            translateExpression(srcPos, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    i32.const ").append(elemShift).append("\n    i32.shl\n    i32.add\n");

            // 3. byte count
            translateExpression(length, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    i32.const ").append(elemShift).append("\n    i32.shl\n");

            // memory.copy instruction
            collector.append("    memory.copy\n");
            return;
        }

        if (mName.equals("fill")) {
            Expression arr = call.getArgument(0);
            String arrElem = getArrayElementType(arr, localElementTypes, localJavaTypes);
            boolean isF64 = "f64".equals(arrElem) || "double".equals(arrElem);

            if (call.getArguments().size() == 2) {
                Expression val = call.getArgument(1);
                translateExpression(arr, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    i32.const 0\n");
                translateExpression(arr, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    i32.load\n");
                translateExpression(val, collector, localTypes, localElementTypes, localJavaTypes, isF64 ? "f64" : "i32");
                collector.append("    call ").append(isF64 ? "$__array_fill_f64" : "$__array_fill_i32").append("\n");
            } else if (call.getArguments().size() == 4) {
                Expression from = call.getArgument(1);
                Expression to = call.getArgument(2);
                Expression val = call.getArgument(3);
                translateExpression(arr, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                translateExpression(from, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                translateExpression(to, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                translateExpression(val, collector, localTypes, localElementTypes, localJavaTypes, isF64 ? "f64" : "i32");
                collector.append("    call ").append(isF64 ? "$__array_fill_f64" : "$__array_fill_i32").append("\n");
            }
            return;
        }

        if (mName.equals("copyOf")) {
            Expression arr = call.getArgument(0);
            Expression newLen = call.getArgument(1);
            String arrElem = getArrayElementType(arr, localElementTypes, localJavaTypes);
            boolean isF64 = "f64".equals(arrElem) || "double".equals(arrElem);

            translateExpression(arr, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            translateExpression(newLen, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    call ").append(isF64 ? "$__array_copyOf_f64" : "$__array_copyOf_i32").append("\n");
            return;
        }

        if (mName.equals("copyOfRange")) {
            Expression arr = call.getArgument(0);
            Expression from = call.getArgument(1);
            Expression to = call.getArgument(2);
            String arrElem = getArrayElementType(arr, localElementTypes, localJavaTypes);
            boolean isF64 = "f64".equals(arrElem) || "double".equals(arrElem);

            translateExpression(arr, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            translateExpression(from, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            translateExpression(to, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    call ").append(isF64 ? "$__array_copyOfRange_f64" : "$__array_copyOfRange_i32").append("\n");
            return;
        }

        if (mName.equals("equals")) {
            Expression a = call.getArgument(0);
            Expression b = call.getArgument(1);
            String aElem = getArrayElementType(a, localElementTypes, localJavaTypes);
            boolean isF64 = "f64".equals(aElem) || "double".equals(aElem);

            translateExpression(a, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            translateExpression(b, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    call ").append(isF64 ? "$__array_equals_f64" : "$__array_equals_i32").append("\n");
            return;
        }
    }

    private String getArrayElementType(Expression expr, Map<String, String> localElementTypes, Map<String, String> localJavaTypes) {
        if (expr.isNameExpr()) {
            String name = expr.asNameExpr().getNameAsString();
            if (localElementTypes.containsKey(name)) {
                return localElementTypes.get(name);
            }
            String javaType = localJavaTypes.get(name);
            if (javaType != null && (javaType.contains("double") || javaType.contains("Double"))) {
                return "f64";
            }
        }
        return "i32";
    }

    private boolean isMathCall(MethodCallExpr call) {
        if (!call.getScope().isPresent()) return false;
        String scopeStr = call.getScope().get().toString();
        return scopeStr.equals("Math") || scopeStr.equals("java.lang.Math") ||
               scopeStr.equals("StrictMath") || scopeStr.equals("java.lang.StrictMath");
    }

    private void translateMathCall(MethodCallExpr call, StringBuilder collector, Map<String, String> localTypes, Map<String, String> localElementTypes, Map<String, String> localJavaTypes) {
        String mName = call.getNameAsString();
        if (mName.equals("min") || mName.equals("max")) {
            Expression arg0 = call.getArgument(0);
            Expression arg1 = call.getArgument(1);
            String type0 = inferType(arg0, localTypes, localElementTypes, localJavaTypes, null);
            String type1 = inferType(arg1, localTypes, localElementTypes, localJavaTypes, null);
            boolean isF64 = "f64".equals(type0) || "f64".equals(type1) || arg0.isDoubleLiteralExpr() || arg1.isDoubleLiteralExpr();
            boolean isF32 = "f32".equals(type0) || "f32".equals(type1);
            boolean isI64 = "i64".equals(type0) || "i64".equals(type1) || arg0.isLongLiteralExpr() || arg1.isLongLiteralExpr();
            if (isF64) {
                translateExpression(arg0, collector, localTypes, localElementTypes, localJavaTypes, "f64");
                translateExpression(arg1, collector, localTypes, localElementTypes, localJavaTypes, "f64");
                collector.append("    f64.").append(mName).append("\n");
            } else if (isF32) {
                translateExpression(arg0, collector, localTypes, localElementTypes, localJavaTypes, "f32");
                translateExpression(arg1, collector, localTypes, localElementTypes, localJavaTypes, "f32");
                collector.append("    f32.").append(mName).append("\n");
            } else if (isI64) {
                translateExpression(arg0, collector, localTypes, localElementTypes, localJavaTypes, "i64");
                translateExpression(arg1, collector, localTypes, localElementTypes, localJavaTypes, "i64");
                collector.append("    call ").append(mName.equals("min") ? "$__i64_min" : "$__i64_max").append("\n");
            } else {
                translateExpression(arg0, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    local.set $__str_arg_ptr\n");
                translateExpression(arg1, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    local.set $__str_sub_start\n");
                collector.append("    local.get $__str_arg_ptr\n");
                collector.append("    local.get $__str_sub_start\n");
                collector.append("    local.get $__str_arg_ptr\n");
                collector.append("    local.get $__str_sub_start\n");
                if (mName.equals("min")) {
                    collector.append("    i32.lt_s\n    select\n");
                } else {
                    collector.append("    i32.gt_s\n    select\n");
                }
            }
            return;
        }
        if (mName.equals("abs")) {
            Expression arg0 = call.getArgument(0);
            String type0 = inferType(arg0, localTypes, localElementTypes, localJavaTypes, null);
            if ("f64".equals(type0) || arg0.isDoubleLiteralExpr()) {
                translateExpression(arg0, collector, localTypes, localElementTypes, localJavaTypes, "f64");
                collector.append("    f64.abs\n");
            } else if ("f32".equals(type0)) {
                translateExpression(arg0, collector, localTypes, localElementTypes, localJavaTypes, "f32");
                collector.append("    f32.abs\n");
            } else if ("i64".equals(type0) || arg0.isLongLiteralExpr()) {
                translateExpression(arg0, collector, localTypes, localElementTypes, localJavaTypes, "i64");
                collector.append("    local.set $__i64_tmp\n");
                collector.append("    i64.const 0\n");
                collector.append("    local.get $__i64_tmp\n");
                collector.append("    i64.sub\n");
                collector.append("    local.get $__i64_tmp\n");
                collector.append("    local.get $__i64_tmp\n");
                collector.append("    i64.const 0\n");
                collector.append("    i64.lt_s\n");
                collector.append("    select\n");
            } else {
                translateExpression(arg0, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    local.set $__str_arg_ptr\n");
                collector.append("    i32.const 0\n");
                collector.append("    local.get $__str_arg_ptr\n");
                collector.append("    i32.sub\n");
                collector.append("    local.get $__str_arg_ptr\n");
                collector.append("    local.get $__str_arg_ptr\n");
                collector.append("    i32.const 0\n");
                collector.append("    i32.lt_s\n");
                collector.append("    select\n");
            }
            return;
        }
        if (mName.equals("sqrt") || mName.equals("floor") || mName.equals("ceil") || mName.equals("trunc")) {
            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "f64");
            collector.append("    f64.").append(mName).append("\n");
            return;
        }
        if (mName.equals("rint")) {
            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "f64");
            collector.append("    f64.nearest\n");
            return;
        }
        if (mName.equals("copySign")) {
            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "f64");
            translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "f64");
            collector.append("    f64.copysign\n");
            return;
        }
        if (mName.equals("round")) {
            Expression arg0 = call.getArgument(0);
            String type0 = inferType(arg0, localTypes, localElementTypes, localJavaTypes, null);
            if ("f32".equals(type0)) {
                translateExpression(arg0, collector, localTypes, localElementTypes, localJavaTypes, "f32");
                collector.append("    f32.const 0.5\n    f32.add\n    f32.floor\n    i32.trunc_f32_s\n");
            } else {
                translateExpression(arg0, collector, localTypes, localElementTypes, localJavaTypes, "f64");
                collector.append("    f64.const 0.5\n    f64.add\n    f64.floor\n    i64.trunc_f64_s\n");
            }
            return;
        }
        if (mName.equals("signum")) {
            Expression arg0 = call.getArgument(0);
            String type0 = inferType(arg0, localTypes, localElementTypes, localJavaTypes, null);
            if ("f32".equals(type0)) {
                translateExpression(arg0, collector, localTypes, localElementTypes, localJavaTypes, "f32");
                collector.append("    local.set $__f32_tmp\n");
                collector.append("    local.get $__f32_tmp\n");
                collector.append("    local.get $__f32_tmp\n");
                collector.append("    f32.const 0.0\n");
                collector.append("    f32.eq\n");
                collector.append("    local.get $__f32_tmp\n");
                collector.append("    local.get $__f32_tmp\n");
                collector.append("    f32.ne\n");
                collector.append("    i32.or\n");
                collector.append("    if (result f32)\n      local.get $__f32_tmp\n    else\n      f32.const -1.0\n      f32.const 1.0\n      local.get $__f32_tmp\n      f32.const 0.0\n      f32.lt\n      select\n    end\n");
            } else {
                translateExpression(arg0, collector, localTypes, localElementTypes, localJavaTypes, "f64");
                collector.append("    local.set $__f64_tmp\n");
                collector.append("    local.get $__f64_tmp\n");
                collector.append("    local.get $__f64_tmp\n");
                collector.append("    f64.const 0.0\n");
                collector.append("    f64.eq\n");
                collector.append("    local.get $__f64_tmp\n");
                collector.append("    local.get $__f64_tmp\n");
                collector.append("    f64.ne\n");
                collector.append("    i32.or\n");
                collector.append("    if (result f64)\n      local.get $__f64_tmp\n    else\n      f64.const -1.0\n      f64.const 1.0\n      local.get $__f64_tmp\n      f64.const 0.0\n      f64.lt\n      select\n    end\n");
            }
            return;
        }
        if (mName.equals("toDegrees")) {
            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "f64");
            collector.append("    f64.const 180.0\n    f64.mul\n    f64.const 3.141592653589793\n    f64.div\n");
            return;
        }
        if (mName.equals("toRadians")) {
            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "f64");
            collector.append("    f64.const 3.141592653589793\n    f64.mul\n    f64.const 180.0\n    f64.div\n");
            return;
        }
        if (mName.equals("random")) {
            collector.append("    call $dom_math_random\n");
            return;
        }
        if (mName.equals("sin") || mName.equals("cos") || mName.equals("tan") ||
            mName.equals("asin") || mName.equals("acos") || mName.equals("atan") ||
            mName.equals("exp") || mName.equals("log") || mName.equals("log10") ||
            mName.equals("log1p") || mName.equals("cbrt") || mName.equals("sinh") ||
            mName.equals("cosh") || mName.equals("tanh")) {
            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "f64");
            collector.append("    call $dom_math_").append(mName).append("\n");
            return;
        }
        if (mName.equals("atan2") || mName.equals("pow") || mName.equals("hypot")) {
            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "f64");
            translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "f64");
            collector.append("    call $dom_math_").append(mName).append("\n");
            return;
        }
    }

    private boolean isStandardConstantScope(String scope) {
        String s = scope.contains(".") ? scope.substring(scope.lastIndexOf('.') + 1) : scope;
        return s.equals("Math") || s.equals("StrictMath") || s.equals("Integer") || s.equals("Long") ||
               s.equals("Double") || s.equals("Float") || s.equals("Byte") || s.equals("Short") ||
               s.equals("Character") || s.equals("Boolean");
    }

    private void emitStandardConstant(String scope, String fName, StringBuilder collector) {
        if (scope.equals("Math") || scope.equals("StrictMath")) {
            if (fName.equals("PI")) collector.append("    f64.const 3.141592653589793\n");
            else if (fName.equals("E")) collector.append("    f64.const 2.718281828459045\n");
            else collector.append("    f64.const 0.0\n");
        } else if (scope.equals("Integer")) {
            if (fName.equals("MAX_VALUE")) collector.append("    i32.const 2147483647\n");
            else if (fName.equals("MIN_VALUE")) collector.append("    i32.const -2147483648\n");
            else if (fName.equals("BYTES")) collector.append("    i32.const 4\n");
            else if (fName.equals("SIZE")) collector.append("    i32.const 32\n");
            else collector.append("    i32.const 0\n");
        } else if (scope.equals("Long")) {
            if (fName.equals("MAX_VALUE")) collector.append("    i64.const 9223372036854775807\n");
            else if (fName.equals("MIN_VALUE")) collector.append("    i64.const -9223372036854775808\n");
            else if (fName.equals("BYTES")) collector.append("    i32.const 8\n");
            else if (fName.equals("SIZE")) collector.append("    i32.const 64\n");
            else collector.append("    i64.const 0\n");
        } else if (scope.equals("Double")) {
            if (fName.equals("MAX_VALUE")) collector.append("    f64.const 1.7976931348623157e+308\n");
            else if (fName.equals("MIN_VALUE")) collector.append("    f64.const 4.9e-324\n");
            else if (fName.equals("NaN")) collector.append("    f64.const nan\n");
            else if (fName.equals("POSITIVE_INFINITY")) collector.append("    f64.const inf\n");
            else if (fName.equals("NEGATIVE_INFINITY")) collector.append("    f64.const -inf\n");
            else if (fName.equals("BYTES")) collector.append("    i32.const 8\n");
            else if (fName.equals("SIZE")) collector.append("    i32.const 64\n");
            else collector.append("    f64.const 0.0\n");
        } else if (scope.equals("Float")) {
            if (fName.equals("MAX_VALUE")) collector.append("    f32.const 3.4028235e+38\n");
            else if (fName.equals("MIN_VALUE")) collector.append("    f32.const 1.4e-45\n");
            else if (fName.equals("NaN")) collector.append("    f32.const nan\n");
            else if (fName.equals("POSITIVE_INFINITY")) collector.append("    f32.const inf\n");
            else if (fName.equals("NEGATIVE_INFINITY")) collector.append("    f32.const -inf\n");
            else if (fName.equals("BYTES")) collector.append("    i32.const 4\n");
            else if (fName.equals("SIZE")) collector.append("    i32.const 32\n");
            else collector.append("    f32.const 0.0\n");
        } else if (scope.equals("Byte")) {
            if (fName.equals("MAX_VALUE")) collector.append("    i32.const 127\n");
            else if (fName.equals("MIN_VALUE")) collector.append("    i32.const -128\n");
            else if (fName.equals("BYTES")) collector.append("    i32.const 1\n");
            else if (fName.equals("SIZE")) collector.append("    i32.const 8\n");
            else collector.append("    i32.const 0\n");
        } else if (scope.equals("Short")) {
            if (fName.equals("MAX_VALUE")) collector.append("    i32.const 32767\n");
            else if (fName.equals("MIN_VALUE")) collector.append("    i32.const -32768\n");
            else if (fName.equals("BYTES")) collector.append("    i32.const 2\n");
            else if (fName.equals("SIZE")) collector.append("    i32.const 16\n");
            else collector.append("    i32.const 0\n");
        } else if (scope.equals("Character")) {
            if (fName.equals("MAX_VALUE")) collector.append("    i32.const 65535\n");
            else if (fName.equals("MIN_VALUE")) collector.append("    i32.const 0\n");
            else if (fName.equals("BYTES")) collector.append("    i32.const 2\n");
            else if (fName.equals("SIZE")) collector.append("    i32.const 16\n");
            else if (fName.equals("MIN_RADIX")) collector.append("    i32.const 2\n");
            else if (fName.equals("MAX_RADIX")) collector.append("    i32.const 36\n");
            else collector.append("    i32.const 0\n");
        } else if (scope.equals("Boolean")) {
            if (fName.equals("TRUE")) collector.append("    i32.const 1\n");
            else if (fName.equals("FALSE")) collector.append("    i32.const 0\n");
            else collector.append("    i32.const 0\n");
        }
    }

    private boolean isWrapperCall(MethodCallExpr call, Map<String, String> localJavaTypes) {
        if (!call.getScope().isPresent()) return false;
        String scopeStr = call.getScope().get().toString();
        String scopeType = getExpressionJavaType(call.getScope().get(), localJavaTypes);
        String s = scopeStr.contains(".") ? scopeStr.substring(scopeStr.lastIndexOf('.') + 1) : scopeStr;
        String st = (scopeType != null && scopeType.contains(".")) ? scopeType.substring(scopeType.lastIndexOf('.') + 1) : scopeType;
        return isWrapperClassName(s) || isWrapperClassName(st);
    }

    private boolean isWrapperClassName(String name) {
        if (name == null) return false;
        return name.equals("Integer") || name.equals("Long") || name.equals("Double") ||
               name.equals("Float") || name.equals("Boolean") || name.equals("Character") ||
               name.equals("Byte") || name.equals("Short") || name.equals("Number");
    }

    private void translateWrapperCall(MethodCallExpr call, StringBuilder collector, Map<String, String> localTypes, Map<String, String> localElementTypes, Map<String, String> localJavaTypes, String expectedType) {
        String mName = call.getNameAsString();
        Expression scope = call.getScope().orElse(null);
        String scopeStr = scope != null ? scope.toString() : "";
        String scopeJavaType = scope != null ? getExpressionJavaType(scope, localJavaTypes) : null;
        String shortScope = scopeStr.contains(".") ? scopeStr.substring(scopeStr.lastIndexOf('.') + 1) : scopeStr;
        String shortType = (scopeJavaType != null && scopeJavaType.contains(".")) ? scopeJavaType.substring(scopeJavaType.lastIndexOf('.') + 1) : scopeJavaType;
        String targetClass = isWrapperClassName(shortScope) ? shortScope : shortType;

        // Instance methods on wrapper objects
        if (mName.equals("intValue") || mName.equals("byteValue") || mName.equals("shortValue")) {
            if ("Long".equals(targetClass)) {
                translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i64");
                collector.append("    i32.wrap_i64\n");
            } else if ("Double".equals(targetClass)) {
                translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "f64");
                collector.append("    i32.trunc_f64_s\n");
            } else if ("Float".equals(targetClass)) {
                translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "f32");
                collector.append("    i32.trunc_f32_s\n");
            } else {
                translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            }
            return;
        }
        if (mName.equals("longValue")) {
            if ("Double".equals(targetClass)) {
                translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "f64");
                collector.append("    i64.trunc_f64_s\n");
            } else if ("Float".equals(targetClass)) {
                translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "f32");
                collector.append("    i64.trunc_f32_s\n");
            } else if ("Long".equals(targetClass)) {
                translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i64");
            } else {
                translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    i64.extend_i32_s\n");
            }
            return;
        }
        if (mName.equals("doubleValue")) {
            if ("Double".equals(targetClass)) {
                translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "f64");
            } else if ("Float".equals(targetClass)) {
                translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "f32");
                collector.append("    f64.promote_f32\n");
            } else if ("Long".equals(targetClass)) {
                translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i64");
                collector.append("    f64.convert_i64_s\n");
            } else {
                translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    f64.convert_i32_s\n");
            }
            return;
        }
        if (mName.equals("floatValue")) {
            if ("Float".equals(targetClass)) {
                translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "f32");
            } else if ("Double".equals(targetClass)) {
                translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "f64");
                collector.append("    f32.demote_f64\n");
            } else if ("Long".equals(targetClass)) {
                translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i64");
                collector.append("    f32.convert_i64_s\n");
            } else {
                translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    f32.convert_i32_s\n");
            }
            return;
        }
        if (mName.equals("booleanValue") || mName.equals("charValue")) {
            translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            return;
        }

        // Static methods
        if ("Integer".equals(targetClass) || "Byte".equals(targetClass) || "Short".equals(targetClass)) {
            if (mName.equals("parseInt") || mName.equals("parseByte") || mName.equals("parseShort")) {
                if (call.getArguments().size() == 1) {
                    translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "String");
                    collector.append("    call $__str_parse_i32\n");
                } else {
                    translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "String");
                    translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    collector.append("    call $__str_parse_i32_radix\n");
                }
                return;
            }
            if (mName.equals("valueOf")) {
                Expression arg0 = call.getArgument(0);
                if (isStringType(arg0, localJavaTypes)) {
                    if (call.getArguments().size() == 1) {
                        translateExpression(arg0, collector, localTypes, localElementTypes, localJavaTypes, "String");
                        collector.append("    call $__str_parse_i32\n");
                    } else {
                        translateExpression(arg0, collector, localTypes, localElementTypes, localJavaTypes, "String");
                        translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        collector.append("    call $__str_parse_i32_radix\n");
                    }
                } else {
                    translateExpression(arg0, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                }
                return;
            }
            if (mName.equals("toString")) {
                if (call.getArguments().size() == 1) {
                    translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    collector.append("    call $__str_from_i32\n");
                } else if (call.getArguments().size() == 2) {
                    translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    collector.append("    call $__str_from_i32_radix\n");
                }
                return;
            }
            if (mName.equals("toHexString")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    call $__str_to_hex_i32\n");
                return;
            }
            if (mName.equals("toOctalString")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    call $__str_to_oct_i32\n");
                return;
            }
            if (mName.equals("toBinaryString")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    call $__str_to_bin_i32\n");
                return;
            }
            if (mName.equals("bitCount")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    i32.popcnt\n");
                return;
            }
            if (mName.equals("numberOfLeadingZeros")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    i32.clz\n");
                return;
            }
            if (mName.equals("numberOfTrailingZeros")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    i32.ctz\n");
                return;
            }
            if (mName.equals("reverseBytes")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    call $__i32_reverse_bytes\n");
                return;
            }
            if (mName.equals("reverse")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    call $__i32_reverse_bits\n");
                return;
            }
            if (mName.equals("rotateLeft")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    i32.rotl\n");
                return;
            }
            if (mName.equals("rotateRight")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    i32.rotr\n");
                return;
            }
            if (mName.equals("compare")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    local.set $__str_sub_start\n");
                collector.append("    local.set $__str_arg_ptr\n");
                collector.append("    local.get $__str_arg_ptr\n");
                collector.append("    local.get $__str_sub_start\n");
                collector.append("    i32.eq\n");
                collector.append("    if (result i32)\n      i32.const 0\n    else\n      i32.const -1\n      i32.const 1\n      local.get $__str_arg_ptr\n      local.get $__str_sub_start\n      i32.lt_s\n      select\n    end\n");
                return;
            }
            if (mName.equals("compareUnsigned")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    local.set $__str_sub_start\n");
                collector.append("    local.set $__str_arg_ptr\n");
                collector.append("    local.get $__str_arg_ptr\n");
                collector.append("    local.get $__str_sub_start\n");
                collector.append("    i32.eq\n");
                collector.append("    if (result i32)\n      i32.const 0\n    else\n      i32.const -1\n      i32.const 1\n      local.get $__str_arg_ptr\n      local.get $__str_sub_start\n      i32.lt_u\n      select\n    end\n");
                return;
            }
            if (mName.equals("signum")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    local.set $__str_arg_ptr\n");
                collector.append("    local.get $__str_arg_ptr\n");
                collector.append("    i32.const 0\n");
                collector.append("    i32.eq\n");
                collector.append("    if (result i32)\n      i32.const 0\n    else\n      i32.const -1\n      i32.const 1\n      local.get $__str_arg_ptr\n      i32.const 0\n      i32.lt_s\n      select\n    end\n");
                return;
            }
            if (mName.equals("min") || mName.equals("max")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    local.set $__str_arg_ptr\n");
                translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    local.set $__str_sub_start\n");
                collector.append("    local.get $__str_arg_ptr\n");
                collector.append("    local.get $__str_sub_start\n");
                collector.append("    local.get $__str_arg_ptr\n");
                collector.append("    local.get $__str_sub_start\n");
                if (mName.equals("min")) {
                    collector.append("    i32.lt_s\n    select\n");
                } else {
                    collector.append("    i32.gt_s\n    select\n");
                }
                return;
            }
            if (mName.equals("highestOneBit")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    local.set $__str_arg_ptr\n");
                collector.append("    local.get $__str_arg_ptr\n");
                collector.append("    i32.eqz\n");
                collector.append("    if (result i32)\n      i32.const 0\n    else\n      i32.const 1\n      i32.const 31\n      local.get $__str_arg_ptr\n      i32.clz\n      i32.sub\n      i32.shl\n    end\n");
                return;
            }
            if (mName.equals("lowestOneBit")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    local.set $__str_arg_ptr\n");
                collector.append("    local.get $__str_arg_ptr\n");
                collector.append("    i32.const 0\n");
                collector.append("    local.get $__str_arg_ptr\n");
                collector.append("    i32.sub\n");
                collector.append("    i32.and\n");
                return;
            }
        }

        if ("Long".equals(targetClass)) {
            if (mName.equals("parseLong")) {
                if (call.getArguments().size() == 1) {
                    translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "String");
                    collector.append("    call $__str_parse_i64\n");
                }
                return;
            }
            if (mName.equals("valueOf")) {
                Expression arg0 = call.getArgument(0);
                if (isStringType(arg0, localJavaTypes)) {
                    translateExpression(arg0, collector, localTypes, localElementTypes, localJavaTypes, "String");
                    collector.append("    call $__str_parse_i64\n");
                } else {
                    translateExpression(arg0, collector, localTypes, localElementTypes, localJavaTypes, "i64");
                }
                return;
            }
            if (mName.equals("toString")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i64");
                collector.append("    call $__str_from_i64\n");
                return;
            }
            if (mName.equals("toHexString")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i64");
                collector.append("    call $__str_to_hex_i64\n");
                return;
            }
            if (mName.equals("toOctalString")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i64");
                collector.append("    call $__str_to_oct_i64\n");
                return;
            }
            if (mName.equals("toBinaryString")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i64");
                collector.append("    call $__str_to_bin_i64\n");
                return;
            }
            if (mName.equals("bitCount")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i64");
                collector.append("    i64.popcnt\n");
                collector.append("    i32.wrap_i64\n");
                return;
            }
            if (mName.equals("numberOfLeadingZeros")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i64");
                collector.append("    i64.clz\n");
                collector.append("    i32.wrap_i64\n");
                return;
            }
            if (mName.equals("numberOfTrailingZeros")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i64");
                collector.append("    i64.ctz\n");
                collector.append("    i32.wrap_i64\n");
                return;
            }
            if (mName.equals("reverseBytes")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i64");
                collector.append("    call $__i64_reverse_bytes\n");
                return;
            }
            if (mName.equals("reverse")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i64");
                collector.append("    call $__i64_reverse_bits\n");
                return;
            }
            if (mName.equals("rotateLeft")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i64");
                translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    i64.extend_i32_u\n");
                collector.append("    i64.rotl\n");
                return;
            }
            if (mName.equals("rotateRight")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i64");
                translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    i64.extend_i32_u\n");
                collector.append("    i64.rotr\n");
                return;
            }
            if (mName.equals("compare")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i64");
                translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "i64");
                collector.append("    call $__i64_compare\n");
                return;
            }
            if (mName.equals("compareUnsigned")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i64");
                translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "i64");
                collector.append("    call $__i64_compare_u\n");
                return;
            }
            if (mName.equals("signum")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i64");
                collector.append("    call $__i64_signum\n");
                return;
            }
            if (mName.equals("min")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i64");
                translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "i64");
                collector.append("    call $__i64_min\n");
                return;
            }
            if (mName.equals("max")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i64");
                translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "i64");
                collector.append("    call $__i64_max\n");
                return;
            }
        }

        if ("Double".equals(targetClass)) {
            if (mName.equals("parseDouble")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "String");
                collector.append("    call $__str_parse_f64\n");
                return;
            }
            if (mName.equals("valueOf")) {
                Expression arg0 = call.getArgument(0);
                if (isStringType(arg0, localJavaTypes)) {
                    translateExpression(arg0, collector, localTypes, localElementTypes, localJavaTypes, "String");
                    collector.append("    call $__str_parse_f64\n");
                } else {
                    translateExpression(arg0, collector, localTypes, localElementTypes, localJavaTypes, "f64");
                }
                return;
            }
            if (mName.equals("toString")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "f64");
                collector.append("    call $__str_from_f64\n");
                return;
            }
            if (mName.equals("isNaN")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "f64");
                collector.append("    local.set $__f64_tmp\n    local.get $__f64_tmp\n    local.get $__f64_tmp\n    f64.ne\n");
                return;
            }
            if (mName.equals("isInfinite")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "f64");
                collector.append("    local.set $__f64_tmp\n    local.get $__f64_tmp\n    f64.const inf\n    f64.eq\n    local.get $__f64_tmp\n    f64.const -inf\n    f64.eq\n    i32.or\n");
                return;
            }
            if (mName.equals("isFinite")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "f64");
                collector.append("    local.set $__f64_tmp\n    local.get $__f64_tmp\n    local.get $__f64_tmp\n    f64.ne\n    local.get $__f64_tmp\n    f64.const inf\n    f64.eq\n    i32.or\n    local.get $__f64_tmp\n    f64.const -inf\n    f64.eq\n    i32.or\n    i32.eqz\n");
                return;
            }
            if (mName.equals("compare")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "f64");
                translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "f64");
                collector.append("    call $__f64_compare\n");
                return;
            }
            if (mName.equals("doubleToLongBits") || mName.equals("doubleToRawLongBits")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "f64");
                collector.append("    i64.reinterpret_f64\n");
                return;
            }
            if (mName.equals("longBitsToDouble")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i64");
                collector.append("    f64.reinterpret_i64\n");
                return;
            }
            if (mName.equals("min") || mName.equals("max")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "f64");
                translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "f64");
                collector.append("    f64.").append(mName).append("\n");
                return;
            }
        }

        if ("Float".equals(targetClass)) {
            if (mName.equals("parseFloat")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "String");
                collector.append("    call $__str_parse_f64\n");
                collector.append("    f32.demote_f64\n");
                return;
            }
            if (mName.equals("valueOf")) {
                Expression arg0 = call.getArgument(0);
                if (isStringType(arg0, localJavaTypes)) {
                    translateExpression(arg0, collector, localTypes, localElementTypes, localJavaTypes, "String");
                    collector.append("    call $__str_parse_f64\n");
                    collector.append("    f32.demote_f64\n");
                } else {
                    translateExpression(arg0, collector, localTypes, localElementTypes, localJavaTypes, "f32");
                }
                return;
            }
            if (mName.equals("toString")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "f32");
                collector.append("    f64.promote_f32\n");
                collector.append("    call $__str_from_f64\n");
                return;
            }
            if (mName.equals("isNaN")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "f32");
                collector.append("    local.set $__f32_tmp\n    local.get $__f32_tmp\n    local.get $__f32_tmp\n    f32.ne\n");
                return;
            }
            if (mName.equals("isInfinite")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "f32");
                collector.append("    local.set $__f32_tmp\n    local.get $__f32_tmp\n    f32.const inf\n    f32.eq\n    local.get $__f32_tmp\n    f32.const -inf\n    f32.eq\n    i32.or\n");
                return;
            }
            if (mName.equals("isFinite")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "f32");
                collector.append("    local.set $__f32_tmp\n    local.get $__f32_tmp\n    local.get $__f32_tmp\n    f32.ne\n    local.get $__f32_tmp\n    f32.const inf\n    f32.eq\n    i32.or\n    local.get $__f32_tmp\n    f32.const -inf\n    f32.eq\n    i32.or\n    i32.eqz\n");
                return;
            }
            if (mName.equals("floatToIntBits") || mName.equals("floatToRawIntBits")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "f32");
                collector.append("    i32.reinterpret_f32\n");
                return;
            }
            if (mName.equals("intBitsToFloat")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    f32.reinterpret_i32\n");
                return;
            }
            if (mName.equals("min") || mName.equals("max")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "f32");
                translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "f32");
                collector.append("    f32.").append(mName).append("\n");
                return;
            }
        }

        if ("Boolean".equals(targetClass)) {
            if (mName.equals("parseBoolean")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "String");
                collector.append("    call $__str_parse_bool\n");
                return;
            }
            if (mName.equals("valueOf")) {
                Expression arg0 = call.getArgument(0);
                if (isStringType(arg0, localJavaTypes)) {
                    translateExpression(arg0, collector, localTypes, localElementTypes, localJavaTypes, "String");
                    collector.append("    call $__str_parse_bool\n");
                } else {
                    translateExpression(arg0, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                }
                return;
            }
            if (mName.equals("toString")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    call $__str_from_bool\n");
                return;
            }
            if (mName.equals("compare")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    i32.sub\n");
                return;
            }
            if (mName.equals("logicalAnd")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    i32.and\n");
                return;
            }
            if (mName.equals("logicalOr")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    i32.or\n");
                return;
            }
            if (mName.equals("logicalXor")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    i32.xor\n");
                return;
            }
        }

        if ("Character".equals(targetClass)) {
            if (mName.equals("isDigit")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    call $__char_is_digit\n");
                return;
            }
            if (mName.equals("isLetter")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    call $__char_is_letter\n");
                return;
            }
            if (mName.equals("isLetterOrDigit")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    call $__char_is_letter_or_digit\n");
                return;
            }
            if (mName.equals("isWhitespace")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    call $__char_is_whitespace\n");
                return;
            }
            if (mName.equals("isUpperCase") || mName.equals("isUpper")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    call $__char_is_upper\n");
                return;
            }
            if (mName.equals("isLowerCase") || mName.equals("isLower")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    call $__char_is_lower\n");
                return;
            }
            if (mName.equals("toUpperCase")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    call $__char_to_upper\n");
                return;
            }
            if (mName.equals("toLowerCase")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    call $__char_to_lower\n");
                return;
            }
            if (mName.equals("digit")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    call $__char_digit\n");
                return;
            }
            if (mName.equals("forDigit")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    call $__char_for_digit\n");
                return;
            }
            if (mName.equals("compare")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    i32.sub\n");
                return;
            }
            if (mName.equals("toString")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    call $__str_from_char\n");
                return;
            }
            if (mName.equals("valueOf")) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                return;
            }
        }
    }

    private void translateDomOrWindowStaticCall(MethodCallExpr call, StringBuilder collector, Map<String, String> localTypes, Map<String, String> localElementTypes, Map<String, String> localJavaTypes) {
        String mName = call.getNameAsString();
        if (mName.equals("global") || mName.equals("getWindow")) {
            collector.append("    call $dom_js_global\n");
        } else if (mName.equals("eval")) {
            translateDomStringArg(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes);
            collector.append("    call $dom_js_eval\n");
        } else if (mName.equals("createObject")) {
            collector.append("    call $dom_js_createObject\n");
        } else if (mName.equals("createArray")) {
            collector.append("    call $dom_js_createArray\n");
        } else if (mName.equals("undefined")) {
            collector.append("    call $dom_js_undefined\n");
        } else if (mName.equals("nullValue")) {
            collector.append("    call $dom_js_null\n");
        } else if (mName.equals("getInstance")) {
            String scope = call.getScope().map(Object::toString).orElse("");
            if (scope.contains("Null")) {
                collector.append("    call $dom_js_null\n");
            } else {
                collector.append("    call $dom_js_undefined\n");
            }
        } else if (mName.equals("forSymbol")) {
            translateDomStringArg(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes);
            collector.append("    call $dom_js_symbol_for\n");
        } else if (mName.equals("typeError")) {
            translateDomStringArg(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes);
            collector.append("    call $dom_js_type_error_of\n");
        } else if (mName.equals("rangeError")) {
            translateDomStringArg(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes);
            collector.append("    call $dom_js_range_error_of\n");
        } else if (mName.equals("syntaxError")) {
            translateDomStringArg(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes);
            collector.append("    call $dom_js_syntax_error_of\n");
        } else if (mName.equals("fromHandle")) {
            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
        } else if (mName.equals("of")) {
            String scope = call.getScope().map(Object::toString).orElse("");
            Expression arg = call.getArgument(0);
            String argWasmType = inferType(arg, localTypes, localElementTypes, localJavaTypes, null);
            String argJavaType = getExpressionJavaType(arg, localJavaTypes);
            if (scope.contains("Number")) {
                if ("f64".equals(argWasmType) || "double".equals(argJavaType) || arg.isDoubleLiteralExpr()) {
                    translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "f64");
                    collector.append("    call $dom_js_number_of_double\n");
                } else {
                    translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    collector.append("    call $dom_js_number_of_int\n");
                }
            } else if (scope.contains("Boolean")) {
                translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    call $dom_js_boolean_of\n");
            } else if (scope.contains("String")) {
                translateDomStringArg(arg, collector, localTypes, localElementTypes, localJavaTypes);
                collector.append("    call $dom_js_string_of\n");
            } else if (scope.contains("BigInt")) {
                if (arg.isStringLiteralExpr() || "String".equals(argJavaType)) {
                    translateDomStringArg(arg, collector, localTypes, localElementTypes, localJavaTypes);
                    collector.append("    call $dom_js_bigint_of_str\n");
                } else {
                    translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "i64");
                    collector.append("    call $dom_js_bigint_of_i64\n");
                }
            } else if (scope.contains("Symbol")) {
                translateDomStringArg(arg, collector, localTypes, localElementTypes, localJavaTypes);
                collector.append("    call $dom_js_symbol_of\n");
            } else if (scope.contains("Error")) {
                translateDomStringArg(arg, collector, localTypes, localElementTypes, localJavaTypes);
                collector.append("    call $dom_js_error_of\n");
            } else {
                if (arg.isStringLiteralExpr() || "String".equals(argJavaType)) {
                    translateDomStringArg(arg, collector, localTypes, localElementTypes, localJavaTypes);
                    collector.append("    call $dom_js_string_of\n");
                } else if ("f64".equals(argWasmType) || "double".equals(argJavaType) || arg.isDoubleLiteralExpr()) {
                    translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "f64");
                    collector.append("    call $dom_js_number_of_double\n");
                } else if ("boolean".equals(argJavaType) || arg.isBooleanLiteralExpr()) {
                    translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    collector.append("    call $dom_js_boolean_of\n");
                } else if ("i64".equals(argWasmType) || "long".equals(argJavaType) || arg.isLongLiteralExpr()) {
                    translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "i64");
                    collector.append("    call $dom_js_bigint_of_i64\n");
                } else {
                    translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    collector.append("    call $dom_js_number_of_int\n");
                }
            }
        } else if (mName.equals("setTimeout") || mName.equals("setInterval")) {
            translateDomStringArg(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes);
            translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    call $dom_").append(mName).append("\n");
        } else if (mName.equals("clearTimeout") || mName.equals("clearInterval") || mName.equals("cancelAnimationFrame")) {
            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    call $dom_").append(mName).append("\n");
        } else if (mName.equals("requestAnimationFrame")) {
            translateDomStringArg(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes);
            collector.append("    call $dom_requestAnimationFrame\n");
        } else if (mName.equals("fetch")) {
            translateDomStringArg(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes);
            translateDomStringArg(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes);
            collector.append("    call $dom_fetch\n");
        } else if (mName.equals("alert") || mName.equals("confirm") || mName.equals("prompt")) {
            translateDomStringArg(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes);
            collector.append("    call $dom_").append(mName).append("\n");
        } else if (mName.equals("getAsString")) {
            translateDomStringArg(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes);
            collector.append("    call $dom_js_resource_get_string\n");
        } else if (mName.equals("hasResource")) {
            translateDomStringArg(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes);
            collector.append("    call $dom_js_resource_has\n");
        } else if (mName.equals("getLength")) {
            translateDomStringArg(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes);
            collector.append("    call $dom_js_resource_get_length\n");
        } else if (mName.equals("injectStyle")) {
            translateDomStringArg(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes);
            collector.append("    call $dom_js_resource_inject_style\n");
        } else if (mName.equals("injectStyleSheet")) {
            translateDomStringArg(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes);
            collector.append("    call $dom_js_resource_inject_stylesheet\n");
        } else if (mName.equals("injectScript")) {
            translateDomStringArg(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes);
            collector.append("    call $dom_js_resource_inject_script\n");
        } else if (mName.equals("injectScriptTag")) {
            translateDomStringArg(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes);
            collector.append("    call $dom_js_resource_inject_script_tag\n");
        } else if (mName.equals("listResources")) {
            collector.append("    call $dom_js_resource_list\n");
        } else if (mName.equals("mountAll")) {
            collector.append("    call $dom_js_resource_mount_all\n");
        } else {
            for (Expression arg : call.getArguments()) {
                translateDomStringArg(arg, collector, localTypes, localElementTypes, localJavaTypes);
            }
            collector.append("    call $dom_").append(mName).append("\n");
        }
    }

    private void translateAtomicsStaticCall(MethodCallExpr call, StringBuilder collector, Map<String, String> localTypes, Map<String, String> localElementTypes, Map<String, String> localJavaTypes) {
        String mName = call.getNameAsString();
        // Atomics methods take int[] array as arg 0, index as arg 1
        // Base address calculation: arr_ptr + 4 + (idx * 4)
        translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
        collector.append("    i32.const 4\n");
        collector.append("    i32.add\n");
        translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "i32");
        collector.append("    i32.const 2\n");
        collector.append("    i32.shl\n");
        collector.append("    i32.add\n");

        if (mName.equals("load")) {
            collector.append("    i32.atomic.load\n");
        } else if (mName.equals("store")) {
            translateExpression(call.getArgument(2), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    i32.atomic.store\n");
        } else if (mName.equals("add")) {
            translateExpression(call.getArgument(2), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    i32.atomic.rmw.add\n");
        } else if (mName.equals("sub")) {
            translateExpression(call.getArgument(2), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    i32.atomic.rmw.sub\n");
        } else if (mName.equals("and")) {
            translateExpression(call.getArgument(2), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    i32.atomic.rmw.and\n");
        } else if (mName.equals("or")) {
            translateExpression(call.getArgument(2), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    i32.atomic.rmw.or\n");
        } else if (mName.equals("xor")) {
            translateExpression(call.getArgument(2), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    i32.atomic.rmw.xor\n");
        } else if (mName.equals("exchange")) {
            translateExpression(call.getArgument(2), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    i32.atomic.rmw.xchg\n");
        } else if (mName.equals("compareExchange")) {
            translateExpression(call.getArgument(2), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            translateExpression(call.getArgument(3), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    i32.atomic.rmw.cmpxchg\n");
        } else if (mName.equals("wait")) {
            translateExpression(call.getArgument(2), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            if (call.getArguments().size() > 3) {
                translateExpression(call.getArgument(3), collector, localTypes, localElementTypes, localJavaTypes, "i64");
            } else {
                collector.append("    i64.const -1\n");
            }
            collector.append("    memory.atomic.wait32\n");
        } else if (mName.equals("notify")) {
            if (call.getArguments().size() > 2) {
                translateExpression(call.getArgument(2), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            } else {
                collector.append("    i32.const 1\n");
            }
            collector.append("    memory.atomic.notify\n");
        }
    }

    private void translateThreadStaticCall(MethodCallExpr call, StringBuilder collector, Map<String, String> localTypes, Map<String, String> localElementTypes, Map<String, String> localJavaTypes) {
        String mName = call.getNameAsString();
        if (mName.equals("spawn")) {
            translateDomStringArg(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes);
            translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    call $dom_thread_spawn\n");
        } else if (mName.equals("sleep")) {
            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    call $dom_thread_sleep\n");
        } else if (mName.equals("currentThreadId")) {
            collector.append("    call $dom_thread_current_id\n");
        } else if (mName.equals("hardwareConcurrency") || mName.equals("activeCount")) {
            collector.append("    call $dom_thread_hardware_concurrency\n");
        } else if (mName.equals("currentThread")) {
            collector.append("    call $Thread_currentThread\n");
        } else if (mName.equals("yield") || mName.equals("onSpinWait")) {
            // no-op
        }
    }

    private void translateClassStaticCall(MethodCallExpr call, StringBuilder collector, Map<String, String> localTypes, Map<String, String> localElementTypes, Map<String, String> localJavaTypes) {
        String mName = call.getNameAsString();
        if (mName.equals("forName")) {
            translateDomStringArg(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes);
            collector.append("    call $Class_forName\n");
        }
    }

    private void translateAtomicIntegerInstanceCall(MethodCallExpr call, StringBuilder collector, Map<String, String> localTypes, Map<String, String> localElementTypes, Map<String, String> localJavaTypes) {
        String mName = call.getNameAsString();
        Expression scope = call.getScope().get();

        if (mName.equals("get")) {
            translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    i32.atomic.load offset=4\n");
        } else if (mName.equals("set")) {
            translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    i32.atomic.store offset=4\n");
        } else if (mName.equals("getAndSet")) {
            translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    i32.atomic.rmw.xchg offset=4\n");
        } else if (mName.equals("compareAndSet")) {
            translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    local.set $__atomic_tmp\n");
            collector.append("    local.get $__atomic_tmp\n");
            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    local.set $__atomic_val\n");
            collector.append("    local.get $__atomic_val\n");
            translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    i32.atomic.rmw.cmpxchg offset=4\n");
            collector.append("    local.get $__atomic_val\n");
            collector.append("    i32.eq\n");
        } else if (mName.equals("getAndAdd")) {
            translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    i32.atomic.rmw.add offset=4\n");
        } else if (mName.equals("addAndGet")) {
            translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    local.set $__atomic_tmp\n");
            collector.append("    local.get $__atomic_tmp\n");
            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    local.set $__atomic_val\n");
            collector.append("    local.get $__atomic_val\n");
            collector.append("    i32.atomic.rmw.add offset=4\n");
            collector.append("    local.get $__atomic_val\n");
            collector.append("    i32.add\n");
        } else if (mName.equals("getAndIncrement")) {
            translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    i32.const 1\n");
            collector.append("    i32.atomic.rmw.add offset=4\n");
        } else if (mName.equals("incrementAndGet")) {
            translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    local.set $__atomic_tmp\n");
            collector.append("    local.get $__atomic_tmp\n");
            collector.append("    i32.const 1\n");
            collector.append("    i32.atomic.rmw.add offset=4\n");
            collector.append("    i32.const 1\n");
            collector.append("    i32.add\n");
        } else if (mName.equals("getAndDecrement")) {
            translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    i32.const -1\n");
            collector.append("    i32.atomic.rmw.add offset=4\n");
        } else if (mName.equals("decrementAndGet")) {
            translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    local.set $__atomic_tmp\n");
            collector.append("    local.get $__atomic_tmp\n");
            collector.append("    i32.const -1\n");
            collector.append("    i32.atomic.rmw.add offset=4\n");
            collector.append("    i32.const 1\n");
            collector.append("    i32.sub\n");
        } else if (mName.equals("getAndAnd")) {
            translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    i32.atomic.rmw.and offset=4\n");
        } else if (mName.equals("getAndOr")) {
            translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    i32.atomic.rmw.or offset=4\n");
        } else if (mName.equals("getAndXor")) {
            translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    i32.atomic.rmw.xor offset=4\n");
        }
    }

    private void translateJsInstanceCall(MethodCallExpr call, StringBuilder collector, Map<String, String> localTypes, Map<String, String> localElementTypes, Map<String, String> localJavaTypes) {
        Expression scope = call.getScope().get();
        String mName = call.getNameAsString();
        translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");

        if (mName.equals("getTypeof")) {
            collector.append("    call $dom_js_typeof\n");
        } else if (mName.equals("isNumber")) {
            collector.append("    call $dom_js_isNumber\n");
        } else if (mName.equals("isBoolean")) {
            collector.append("    call $dom_js_isBoolean\n");
        } else if (mName.equals("isString")) {
            collector.append("    call $dom_js_isString\n");
        } else if (mName.equals("isBigInt")) {
            collector.append("    call $dom_js_isBigInt\n");
        } else if (mName.equals("isSymbol")) {
            collector.append("    call $dom_js_isSymbol\n");
        } else if (mName.equals("isUndefined")) {
            collector.append("    call $dom_js_isUndefined\n");
        } else if (mName.equals("isNull")) {
            collector.append("    call $dom_js_isNull\n");
        } else if (mName.equals("isObject")) {
            collector.append("    call $dom_js_isObject\n");
        } else if (mName.equals("isFunction")) {
            collector.append("    call $dom_js_isFunction\n");
        } else if (mName.equals("isError")) {
            collector.append("    call $dom_js_isError\n");
        } else if (mName.equals("instanceOf")) {
            translateDomStringArg(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes);
            collector.append("    call $dom_js_instanceof\n");
        } else if (mName.equals("strictEquals")) {
            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    call $dom_js_strictEquals\n");
        } else if (mName.equals("isNaN")) {
            collector.append("    call $dom_js_isNaN\n");
        } else if (mName.equals("isFinite")) {
            collector.append("    call $dom_js_isFinite\n");
        } else if (mName.equals("isInteger")) {
            collector.append("    call $dom_js_isInteger\n");
        } else if (mName.equals("doubleValue")) {
            collector.append("    call $dom_js_asDouble\n");
        } else if (mName.equals("intValue")) {
            collector.append("    call $dom_js_asInt\n");
        } else if (mName.equals("longValue")) {
            collector.append("    call $dom_js_asInt\n");
        } else if (mName.equals("booleanValue") || mName.equals("asBooleanPrimitive")) {
            collector.append("    call $dom_js_asBoolean\n");
        } else if (mName.equals("stringValue")) {
            collector.append("    call $dom_js_asString\n");
        } else if (mName.equals("getDescription")) {
            collector.append("    call $dom_js_symbolDescription\n");
        } else if (mName.equals("getMessage")) {
            collector.append("    call $dom_js_errorMessage\n");
        } else if (mName.equals("getName")) {
            collector.append("    call $dom_js_errorName\n");
        } else if (mName.equals("getStack")) {
            collector.append("    call $dom_js_errorStack\n");
        } else if (mName.equals("asNumber") || mName.equals("asBoolean") || mName.equals("asStringValue") ||
                   mName.equals("asBigInt") || mName.equals("asSymbol") || mName.equals("asObject") ||
                   mName.equals("asError")) {
            // handle is already on stack
        } else if (mName.equals("get")) {
            translateDomStringArg(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes);
            collector.append("    call $dom_js_get\n");
        } else if (mName.equals("getString")) {
            translateDomStringArg(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes);
            collector.append("    call $dom_js_getString\n");
        } else if (mName.equals("getInt")) {
            translateDomStringArg(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes);
            collector.append("    call $dom_js_getInt\n");
        } else if (mName.equals("getDouble")) {
            translateDomStringArg(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes);
            collector.append("    call $dom_js_getDouble\n");
        } else if (mName.equals("getBoolean")) {
            translateDomStringArg(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes);
            collector.append("    call $dom_js_getBoolean\n");
        } else if (mName.equals("set")) {
            translateDomStringArg(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes);
            Expression val = call.getArgument(1);
            String valType = inferType(val, localTypes, localElementTypes, localJavaTypes, null);
            String javaType = getExpressionJavaType(val, localJavaTypes);
            if (val.isStringLiteralExpr() || "String".equals(javaType) || "String".equals(valType)) {
                translateDomStringArg(val, collector, localTypes, localElementTypes, localJavaTypes);
                collector.append("    call $dom_js_setString\n");
            } else if ("f64".equals(valType) || "double".equals(javaType) || val.isDoubleLiteralExpr()) {
                translateExpression(val, collector, localTypes, localElementTypes, localJavaTypes, "f64");
                collector.append("    call $dom_js_setDouble\n");
            } else if ("boolean".equals(javaType) || val.isBooleanLiteralExpr()) {
                translateExpression(val, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    call $dom_js_setBoolean\n");
            } else if ("JSObject".equals(javaType) || "Element".equals(javaType)) {
                translateExpression(val, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    call $dom_js_setObject\n");
            } else {
                translateExpression(val, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    call $dom_js_setInt\n");
            }
        } else if (mName.equals("call")) {
            translateDomStringArg(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes);
            int argCount = call.getArguments().size();
            if (argCount == 1) {
                collector.append("    call $dom_js_call0\n");
            } else if (argCount == 2) {
                Expression a1 = call.getArgument(1);
                String a1WasmType = inferType(a1, localTypes, localElementTypes, localJavaTypes, null);
                String a1JavaType = getExpressionJavaType(a1, localJavaTypes);
                if (a1.isStringLiteralExpr() || "String".equals(a1JavaType) || "String".equals(a1WasmType)) {
                    translateDomStringArg(a1, collector, localTypes, localElementTypes, localJavaTypes);
                    collector.append("    call $dom_js_call1_str\n");
                } else if ("f64".equals(a1WasmType) || "double".equals(a1JavaType) || a1.isDoubleLiteralExpr()) {
                    translateExpression(a1, collector, localTypes, localElementTypes, localJavaTypes, "f64");
                    collector.append("    call $dom_js_call1_double\n");
                } else if ("boolean".equals(a1JavaType) || a1.isBooleanLiteralExpr()) {
                    translateExpression(a1, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    collector.append("    call $dom_js_call1_bool\n");
                } else if ("JSObject".equals(a1JavaType) || "Element".equals(a1JavaType)) {
                    translateExpression(a1, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    collector.append("    call $dom_js_call1_obj\n");
                } else {
                    translateExpression(a1, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    collector.append("    call $dom_js_call1_int\n");
                }
            } else if (argCount == 3) {
                Expression a1 = call.getArgument(1);
                Expression a2 = call.getArgument(2);
                String a1JavaType = getExpressionJavaType(a1, localJavaTypes);
                String a2JavaType = getExpressionJavaType(a2, localJavaTypes);
                boolean a1Str = a1.isStringLiteralExpr() || "String".equals(a1JavaType);
                boolean a2Str = a2.isStringLiteralExpr() || "String".equals(a2JavaType);
                if (a1Str && a2Str) {
                    translateDomStringArg(a1, collector, localTypes, localElementTypes, localJavaTypes);
                    translateDomStringArg(a2, collector, localTypes, localElementTypes, localJavaTypes);
                    collector.append("    call $dom_js_call2_str_str\n");
                } else if (a1Str && ("JSObject".equals(a2JavaType) || "Element".equals(a2JavaType))) {
                    translateDomStringArg(a1, collector, localTypes, localElementTypes, localJavaTypes);
                    translateExpression(a2, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    collector.append("    call $dom_js_call2_str_obj\n");
                } else if (a1Str) {
                    translateDomStringArg(a1, collector, localTypes, localElementTypes, localJavaTypes);
                    translateExpression(a2, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    collector.append("    call $dom_js_call2_str_int\n");
                } else {
                    translateExpression(a1, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    translateExpression(a2, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    collector.append("    call $dom_js_call2_obj_obj\n");
                }
            }
        } else if (mName.equals("callString")) {
            translateDomStringArg(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes);
            if (call.getArguments().size() > 1) {
                translateDomStringArg(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes);
                collector.append("    call $dom_js_callString1_str\n");
            } else {
                collector.append("    call $dom_js_callString0\n");
            }
        } else if (mName.equals("callInt")) {
            translateDomStringArg(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes);
            if (call.getArguments().size() > 1) {
                translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    call $dom_js_callInt1_int\n");
            } else {
                collector.append("    call $dom_js_callInt0\n");
            }
        } else if (mName.equals("callDouble")) {
            translateDomStringArg(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes);
            collector.append("    call $dom_js_callDouble0\n");
        } else if (mName.equals("callBoolean")) {
            translateDomStringArg(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes);
            collector.append("    call $dom_js_callBoolean0\n");
        } else if (mName.equals("asString")) {
            collector.append("    call $dom_js_asString\n");
        } else if (mName.equals("asInt")) {
            collector.append("    call $dom_js_asInt\n");
        } else if (mName.equals("asDouble")) {
            collector.append("    call $dom_js_asDouble\n");
        } else if (mName.equals("asBoolean")) {
            collector.append("    call $dom_js_asBoolean\n");
        } else if (mName.equals("isArray")) {
            collector.append("    call $dom_js_isArray\n");
        } else if (mName.equals("length")) {
            collector.append("    call $dom_js_length\n");
        } else if (mName.equals("getIndex")) {
            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    call $dom_js_getIndex\n");
        } else if (mName.equals("setIndex")) {
            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            Expression val = call.getArgument(1);
            String valType = inferType(val, localTypes, localElementTypes, localJavaTypes, null);
            String javaType = getExpressionJavaType(val, localJavaTypes);
            if (val.isStringLiteralExpr() || "String".equals(javaType) || "String".equals(valType)) {
                translateDomStringArg(val, collector, localTypes, localElementTypes, localJavaTypes);
                collector.append("    call $dom_js_setIndexStr\n");
            } else if ("f64".equals(valType) || "double".equals(javaType) || val.isDoubleLiteralExpr()) {
                translateExpression(val, collector, localTypes, localElementTypes, localJavaTypes, "f64");
                collector.append("    call $dom_js_setIndexDouble\n");
            } else if ("boolean".equals(javaType) || val.isBooleanLiteralExpr()) {
                translateExpression(val, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    call $dom_js_setIndexBoolean\n");
            } else if ("JSObject".equals(javaType) || "Element".equals(javaType)) {
                translateExpression(val, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    call $dom_js_setIndexObj\n");
            } else {
                translateExpression(val, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    call $dom_js_setIndexInt\n");
            }
        } else if (mName.equals("push")) {
            Expression val = call.getArgument(0);
            String valType = inferType(val, localTypes, localElementTypes, localJavaTypes, null);
            String javaType = getExpressionJavaType(val, localJavaTypes);
            if (val.isStringLiteralExpr() || "String".equals(javaType) || "String".equals(valType)) {
                translateDomStringArg(val, collector, localTypes, localElementTypes, localJavaTypes);
                collector.append("    call $dom_js_pushStr\n");
            } else if ("f64".equals(valType) || "double".equals(javaType) || val.isDoubleLiteralExpr()) {
                translateExpression(val, collector, localTypes, localElementTypes, localJavaTypes, "f64");
                collector.append("    call $dom_js_pushDouble\n");
            } else if ("boolean".equals(javaType) || val.isBooleanLiteralExpr()) {
                translateExpression(val, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    call $dom_js_pushBoolean\n");
            } else if ("JSObject".equals(javaType) || "Element".equals(javaType)) {
                translateExpression(val, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    call $dom_js_pushObj\n");
            } else {
                translateExpression(val, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    call $dom_js_pushInt\n");
            }
        }
    }

    private String getArrayElementType(ArrayAccessExpr aae, Map<String, String> localElementTypes, String fallback) {
        if (aae.getName().isNameExpr()) {
            String name = aae.getName().asNameExpr().getNameAsString();
            if (localElementTypes.containsKey(name)) {
                return localElementTypes.get(name);
            }
        }
        return (fallback != null && !fallback.equals("i32")) ? fallback : "i32";
    }

    private String getLoadInstruction(String elemType) {
        if (elemType == null) return "i32.load";
        return switch (elemType) {
            case "v128" -> "v128.load";
            case "f64" -> "f64.load";
            case "f32" -> "f32.load";
            case "i64" -> "i64.load";
            default -> "i32.load";
        };
    }

    private String getStoreInstruction(String elemType) {
        if (elemType == null) return "i32.store";
        return switch (elemType) {
            case "v128" -> "v128.store";
            case "f64" -> "f64.store";
            case "f32" -> "f32.store";
            case "i64" -> "i64.store";
            default -> "i32.store";
        };
    }

    private String getWasmZeroConst(String type) {
        if (type == null) return "i32.const 0";
        return switch (type) {
            case "v128" -> "v128.const i32x4 0 0 0 0";
            case "f64" -> "f64.const 0.0";
            case "f32" -> "f32.const 0.0";
            case "i64" -> "i64.const 0";
            default -> "i32.const 0";
        };
    }

    private String inferType(Expression expr, Map<String, String> localTypes, Map<String, String> localElementTypes, Map<String, String> localJavaTypes, String fallback) {
        if (expr.isEnclosedExpr()) {
            return inferType(expr.asEnclosedExpr().getInner(), localTypes, localElementTypes, localJavaTypes, fallback);
        }
        if (expr.isConditionalExpr()) {
            return inferType(expr.asConditionalExpr().getThenExpr(), localTypes, localElementTypes, localJavaTypes, fallback);
        }
        if (expr.isDoubleLiteralExpr()) return "f64";
        if (expr.isLongLiteralExpr()) return "i64";
        if (expr.isIntegerLiteralExpr() || expr.isBooleanLiteralExpr() || expr.isCharLiteralExpr() || expr.isClassExpr()) return "i32";
        if (expr.isStringLiteralExpr()) return "i32";
        if (expr.isNameExpr()) {
            String name = expr.asNameExpr().getNameAsString();
            if (localTypes.containsKey(name)) return localTypes.get(name);
            if (globalVars != null && globalVars.containsKey(name)) return globalVars.get(name).type;
            if (currentClass != null && currentClass.getField(name) != null) return currentClass.getField(name).getWasmType();
        }
        if (expr.isBinaryExpr()) {
            String inferred = getExpressionJavaType(expr, localJavaTypes);
            if (inferred != null) return "String".equals(inferred) ? "String" : TypeMapper.mapType(inferred);
            BinaryExpr b = expr.asBinaryExpr();
            String lt = inferType(b.getLeft(), localTypes, localElementTypes, localJavaTypes, null);
            String rt = inferType(b.getRight(), localTypes, localElementTypes, localJavaTypes, null);
            if ("f64".equals(lt) || "f64".equals(rt)) return "f64";
            if ("f32".equals(lt) || "f32".equals(rt)) return "f32";
            if ("i64".equals(lt) || "i64".equals(rt)) return "i64";
            return "i32";
        }
        if (expr.isArrayAccessExpr()) {
            return getArrayElementType(expr.asArrayAccessExpr(), localElementTypes, fallback);
        }
        if (expr.isConditionalExpr()) return TypeMapper.mapType(getExpressionJavaType(expr, localJavaTypes));
        if (expr.isEnclosedExpr()) return inferType(expr.asEnclosedExpr().getInner(), localTypes, localElementTypes, localJavaTypes, fallback);
        if (expr.isMethodCallExpr()) {
            MethodCallExpr call = expr.asMethodCallExpr();
            MethodModel source = sourceMethod(call, localJavaTypes);
            if (source != null) return source.getReturnWasmType();
            String mName = call.getNameAsString();
            if (isMathCall(call)) {
                if (mName.equals("round")) {
                    if (call.getArguments().size() > 0 && "f32".equals(inferType(call.getArgument(0), localTypes, localElementTypes, localJavaTypes, null))) {
                        return "i32";
                    }
                    return "i64";
                }
                if (mName.equals("min") || mName.equals("max") || mName.equals("abs") || mName.equals("signum")) {
                    if (call.getArguments().size() > 0) {
                        return inferType(call.getArgument(0), localTypes, localElementTypes, localJavaTypes, "f64");
                    }
                }
                return "f64";
            }
            if (isWrapperCall(call, localJavaTypes)) {
                if (mName.equals("parseLong") || mName.equals("longValue") || mName.equals("doubleToLongBits") || mName.equals("doubleToRawLongBits")) return "i64";
                if (mName.equals("parseDouble") || mName.equals("doubleValue") || mName.equals("longBitsToDouble")) return "f64";
                if (mName.equals("parseFloat") || mName.equals("floatValue") || mName.equals("intBitsToFloat")) return "f32";
                if (mName.equals("toString") || mName.equals("toHexString") || mName.equals("toOctalString") || mName.equals("toBinaryString")) return "String";
                if (mName.equals("valueOf") && call.getArguments().size() > 0) {
                    Expression arg0 = call.getArgument(0);
                    if (!isStringType(arg0, localJavaTypes)) {
                        return inferType(arg0, localTypes, localElementTypes, localJavaTypes, "i32");
                    }
                }
                return "i32";
            }
            String javaType = getExpressionJavaType(expr, localJavaTypes);
            if ("Float32x4".equals(javaType) || "Int32x4".equals(javaType) || "Vector128".equals(javaType)) return "v128";
            if ("double".equals(javaType)) return "f64";
            if ("float".equals(javaType)) return "f32";
            if ("long".equals(javaType)) return "i64";
            if ("String".equals(javaType)) return "String";
            return "i32";
        }
        if (expr.isFieldAccessExpr()) {
            FieldAccessExpr fae = expr.asFieldAccessExpr();
            if (fae.getNameAsString().equals("length")) return "i32";
            if (fae.getScope().isNameExpr() && isStandardConstantScope(fae.getScope().asNameExpr().getNameAsString())) {
                String scopeName = fae.getScope().asNameExpr().getNameAsString();
                String shortScope = scopeName.contains(".") ? scopeName.substring(scopeName.lastIndexOf('.') + 1) : scopeName;
                if ("Math".equals(shortScope) || "StrictMath".equals(shortScope) || "Double".equals(shortScope)) {
                    if ("BYTES".equals(fae.getNameAsString()) || "SIZE".equals(fae.getNameAsString())) return "i32";
                    return "f64";
                }
                if ("Float".equals(shortScope)) {
                    if ("BYTES".equals(fae.getNameAsString()) || "SIZE".equals(fae.getNameAsString())) return "i32";
                    return "f32";
                }
                if ("Long".equals(shortScope)) {
                    if ("BYTES".equals(fae.getNameAsString()) || "SIZE".equals(fae.getNameAsString())) return "i32";
                    return "i64";
                }
                return "i32";
            }
            String targetType = getExpressionJavaType(fae.getScope(), localJavaTypes);
            if (targetType != null && classRegistry != null && classRegistry.isRecord(targetType)) {
                FieldModel fm = classRegistry.getRecord(targetType).getComponent(fae.getNameAsString());
                if (fm != null) return fm.getWasmType();
            } else if (targetType != null && classRegistry != null && classRegistry.hasClass(targetType)) {
                FieldModel fm = classRegistry.getClass(targetType).getField(fae.getNameAsString());
                if (fm != null) return fm.getWasmType();
            }
            return "i32";
        }
        if (expr.isObjectCreationExpr()) {
            return "i32";
        }
        return fallback != null ? fallback : "i32";
    }

    private BinaryExpr.Operator mapAssignOpToBinaryOp(AssignExpr.Operator op) {
        return switch (op) {
            case PLUS -> BinaryExpr.Operator.PLUS;
            case MINUS -> BinaryExpr.Operator.MINUS;
            case MULTIPLY -> BinaryExpr.Operator.MULTIPLY;
            case DIVIDE -> BinaryExpr.Operator.DIVIDE;
            case REMAINDER -> BinaryExpr.Operator.REMAINDER;
            case BINARY_AND -> BinaryExpr.Operator.BINARY_AND;
            case BINARY_OR -> BinaryExpr.Operator.BINARY_OR;
            case XOR -> BinaryExpr.Operator.XOR;
            case LEFT_SHIFT -> BinaryExpr.Operator.LEFT_SHIFT;
            case SIGNED_RIGHT_SHIFT -> BinaryExpr.Operator.SIGNED_RIGHT_SHIFT;
            case UNSIGNED_RIGHT_SHIFT -> BinaryExpr.Operator.UNSIGNED_RIGHT_SHIFT;
            default -> null;
        };
    }

    private String getWasmBinaryOp(BinaryExpr.Operator op, String type) {
        boolean isF64 = "f64".equals(type);
        boolean isF32 = "f32".equals(type);
        boolean isI64 = "i64".equals(type);
        String prefix = isF64 ? "f64" : isF32 ? "f32" : isI64 ? "i64" : "i32";

        return switch (op) {
            case PLUS -> prefix + ".add";
            case MINUS -> prefix + ".sub";
            case MULTIPLY -> prefix + ".mul";
            case DIVIDE -> (isF64 || isF32) ? prefix + ".div" : prefix + ".div_s";
            case REMAINDER -> (isF64 || isF32) ? prefix + ".rem" : prefix + ".rem_s";
            case EQUALS -> (isF64 || isF32) ? prefix + ".eq" : prefix + ".eq";
            case NOT_EQUALS -> (isF64 || isF32) ? prefix + ".ne" : prefix + ".ne";
            case LESS -> (isF64 || isF32) ? prefix + ".lt" : prefix + ".lt_s";
            case LESS_EQUALS -> (isF64 || isF32) ? prefix + ".le" : prefix + ".le_s";
            case GREATER -> (isF64 || isF32) ? prefix + ".gt" : prefix + ".gt_s";
            case GREATER_EQUALS -> (isF64 || isF32) ? prefix + ".ge" : prefix + ".ge_s";
            case BINARY_AND, AND -> prefix + ".and";
            case BINARY_OR, OR -> prefix + ".or";
            case XOR -> prefix + ".xor";
            case LEFT_SHIFT -> prefix + ".shl";
            case SIGNED_RIGHT_SHIFT -> prefix + ".shr_s";
            case UNSIGNED_RIGHT_SHIFT -> prefix + ".shr_u";
            default -> prefix + ".add";
        };
    }

    private boolean isSimdCall(MethodCallExpr call, Map<String, String> localJavaTypes) {
        if (!call.getScope().isPresent()) return false;
        Expression scope = call.getScope().get();
        String scopeStr = scope.toString();
        String scopeJavaType = getExpressionJavaType(scope, localJavaTypes);

        if (scopeStr.equals("Float32x4") || scopeStr.endsWith(".Float32x4") ||
                scopeStr.equals("Int32x4") || scopeStr.endsWith(".Int32x4") ||
                scopeStr.equals("Vector128") || scopeStr.endsWith(".Vector128")) {
            return true;
        }
        if ("Float32x4".equals(scopeJavaType) || (scopeJavaType != null && scopeJavaType.endsWith(".Float32x4")) ||
                "Int32x4".equals(scopeJavaType) || (scopeJavaType != null && scopeJavaType.endsWith(".Int32x4"))) {
            return true;
        }
        return false;
    }

    private void translateSimdCall(MethodCallExpr call, StringBuilder collector, Map<String, String> localTypes, Map<String, String> localElementTypes, Map<String, String> localJavaTypes, String expectedType) {
        Expression scope = call.getScope().get();
        String scopeStr = scope.toString();
        String scopeJavaType = getExpressionJavaType(scope, localJavaTypes);
        boolean isFloat = scopeStr.equals("Float32x4") || scopeStr.endsWith(".Float32x4") ||
                "Float32x4".equals(scopeJavaType) || (scopeJavaType != null && scopeJavaType.endsWith(".Float32x4"));
        String mName = call.getNameAsString();

        if (mName.equals("of")) {
            if (call.getArguments().size() == 4) {
                boolean allConst = true;
                for (Expression arg : call.getArguments()) {
                    if (!(arg.isIntegerLiteralExpr() || arg.isDoubleLiteralExpr() ||
                            (arg.isUnaryExpr() && arg.asUnaryExpr().getOperator() == UnaryExpr.Operator.MINUS &&
                                    (arg.asUnaryExpr().getExpression().isIntegerLiteralExpr() || arg.asUnaryExpr().getExpression().isDoubleLiteralExpr())))) {
                        allConst = false;
                        break;
                    }
                }
                if (allConst) {
                    if (isFloat) {
                        collector.append("    v128.const f32x4 ")
                                .append(getNumericLiteralString(call.getArgument(0), true))
                                .append(" ")
                                .append(getNumericLiteralString(call.getArgument(1), true))
                                .append(" ")
                                .append(getNumericLiteralString(call.getArgument(2), true))
                                .append(" ")
                                .append(getNumericLiteralString(call.getArgument(3), true))
                                .append("\n");
                    } else {
                        collector.append("    v128.const i32x4 ")
                                .append(getNumericLiteralString(call.getArgument(0), false))
                                .append(" ")
                                .append(getNumericLiteralString(call.getArgument(1), false))
                                .append(" ")
                                .append(getNumericLiteralString(call.getArgument(2), false))
                                .append(" ")
                                .append(getNumericLiteralString(call.getArgument(3), false))
                                .append("\n");
                    }
                    return;
                }
                if (isFloat) {
                    translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "f32");
                    collector.append("    f32x4.splat\n");
                    translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "f32");
                    collector.append("    f32x4.replace_lane 1\n");
                    translateExpression(call.getArgument(2), collector, localTypes, localElementTypes, localJavaTypes, "f32");
                    collector.append("    f32x4.replace_lane 2\n");
                    translateExpression(call.getArgument(3), collector, localTypes, localElementTypes, localJavaTypes, "f32");
                    collector.append("    f32x4.replace_lane 3\n");
                } else {
                    translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    collector.append("    i32x4.splat\n");
                    translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    collector.append("    i32x4.replace_lane 1\n");
                    translateExpression(call.getArgument(2), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    collector.append("    i32x4.replace_lane 2\n");
                    translateExpression(call.getArgument(3), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    collector.append("    i32x4.replace_lane 3\n");
                }
            }
        } else if (mName.equals("splat")) {
            if (isFloat) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "f32");
                collector.append("    f32x4.splat\n");
            } else {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    i32x4.splat\n");
            }
        } else if (mName.equals("zero")) {
            if (isFloat) {
                collector.append("    v128.const f32x4 0.0 0.0 0.0 0.0\n");
            } else {
                collector.append("    v128.const i32x4 0 0 0 0\n");
            }
        } else if (mName.equals("add")) {
            translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "v128");
            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "v128");
            collector.append(isFloat ? "    f32x4.add\n" : "    i32x4.add\n");
        } else if (mName.equals("sub")) {
            translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "v128");
            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "v128");
            collector.append(isFloat ? "    f32x4.sub\n" : "    i32x4.sub\n");
        } else if (mName.equals("mul")) {
            translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "v128");
            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "v128");
            collector.append(isFloat ? "    f32x4.mul\n" : "    i32x4.mul\n");
        } else if (mName.equals("div")) {
            translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "v128");
            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "v128");
            collector.append("    f32x4.div\n");
        } else if (mName.equals("min")) {
            translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "v128");
            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "v128");
            collector.append(isFloat ? "    f32x4.min\n" : "    i32x4.min_s\n");
        } else if (mName.equals("max")) {
            translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "v128");
            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "v128");
            collector.append(isFloat ? "    f32x4.max\n" : "    i32x4.max_s\n");
        } else if (mName.equals("sqrt")) {
            translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "v128");
            collector.append("    f32x4.sqrt\n");
        } else if (mName.equals("abs")) {
            translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "v128");
            collector.append(isFloat ? "    f32x4.abs\n" : "    i32x4.abs\n");
        } else if (mName.equals("neg")) {
            translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "v128");
            collector.append(isFloat ? "    f32x4.neg\n" : "    i32x4.neg\n");
        } else if (mName.equals("and")) {
            translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "v128");
            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "v128");
            collector.append("    v128.and\n");
        } else if (mName.equals("or")) {
            translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "v128");
            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "v128");
            collector.append("    v128.or\n");
        } else if (mName.equals("xor")) {
            translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "v128");
            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "v128");
            collector.append("    v128.xor\n");
        } else if (mName.equals("not")) {
            translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "v128");
            collector.append("    v128.not\n");
        } else if (mName.equals("shiftLeft") || mName.equals("shl")) {
            translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "v128");
            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    i32x4.shl\n");
        } else if (mName.equals("shiftRight") || mName.equals("shr")) {
            translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "v128");
            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    i32x4.shr_s\n");
        } else if (mName.equals("getX") || mName.equals("getY") || mName.equals("getZ") || mName.equals("getW") ||
                mName.equals("get") || mName.equals("extractLane")) {
            int lane = 0;
            if (mName.equals("getY")) lane = 1;
            else if (mName.equals("getZ")) lane = 2;
            else if (mName.equals("getW")) lane = 3;
            else if (call.getArguments().size() > 0 && call.getArgument(0).isIntegerLiteralExpr()) {
                lane = call.getArgument(0).asIntegerLiteralExpr().asNumber().intValue();
            }
            translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "v128");
            if (isFloat) {
                collector.append("    f32x4.extract_lane ").append(lane).append("\n");
                if ("f64".equals(expectedType) || "double".equals(expectedType)) {
                    collector.append("    f64.promote_f32\n");
                }
            } else {
                collector.append("    i32x4.extract_lane ").append(lane).append("\n");
            }
        } else if (mName.equals("withLane") || mName.equals("replaceLane") ||
                mName.equals("withX") || mName.equals("withY") || mName.equals("withZ") || mName.equals("withW")) {
            int lane = 0;
            Expression valExpr = null;
            if (mName.equals("withX")) { lane = 0; valExpr = call.getArgument(0); }
            else if (mName.equals("withY")) { lane = 1; valExpr = call.getArgument(0); }
            else if (mName.equals("withZ")) { lane = 2; valExpr = call.getArgument(0); }
            else if (mName.equals("withW")) { lane = 3; valExpr = call.getArgument(0); }
            else if (call.getArguments().size() == 2) {
                if (call.getArgument(0).isIntegerLiteralExpr()) {
                    lane = call.getArgument(0).asIntegerLiteralExpr().asNumber().intValue();
                }
                valExpr = call.getArgument(1);
            }
            translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "v128");
            if (isFloat) {
                translateExpression(valExpr, collector, localTypes, localElementTypes, localJavaTypes, "f32");
                collector.append("    f32x4.replace_lane ").append(lane).append("\n");
            } else {
                translateExpression(valExpr, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    i32x4.replace_lane ").append(lane).append("\n");
            }
        } else if (mName.equals("load")) {
            if (call.getArguments().size() == 2) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    i32.const 4\n    i32.add\n");
                translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    i32.const 4\n    i32.mul\n    i32.add\n");
                collector.append("    v128.load\n");
            } else if (call.getArguments().size() == 1) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    v128.load\n");
            }
        } else if (mName.equals("store")) {
            if (call.getArguments().size() == 2) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    i32.const 4\n    i32.add\n");
                translateExpression(call.getArgument(1), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    i32.const 4\n    i32.mul\n    i32.add\n");
                translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "v128");
                collector.append("    v128.store\n");
            } else if (call.getArguments().size() == 1) {
                translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "v128");
                collector.append("    v128.store\n");
            }
        } else if (mName.equals("dot")) {
            translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "v128");
            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "v128");
            collector.append("    f32x4.mul\n");
            collector.append("    local.set $__simd_tmp\n");
            collector.append("    local.get $__simd_tmp\n    f32x4.extract_lane 0\n");
            collector.append("    local.get $__simd_tmp\n    f32x4.extract_lane 1\n    f32.add\n");
            collector.append("    local.get $__simd_tmp\n    f32x4.extract_lane 2\n    f32.add\n");
            collector.append("    local.get $__simd_tmp\n    f32x4.extract_lane 3\n    f32.add\n");
            if ("f64".equals(expectedType) || "double".equals(expectedType)) {
                collector.append("    f64.promote_f32\n");
            }
        } else if (mName.equals("sum")) {
            translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "v128");
            collector.append("    local.set $__simd_tmp\n");
            if (isFloat) {
                collector.append("    local.get $__simd_tmp\n    f32x4.extract_lane 0\n");
                collector.append("    local.get $__simd_tmp\n    f32x4.extract_lane 1\n    f32.add\n");
                collector.append("    local.get $__simd_tmp\n    f32x4.extract_lane 2\n    f32.add\n");
                collector.append("    local.get $__simd_tmp\n    f32x4.extract_lane 3\n    f32.add\n");
                if ("f64".equals(expectedType) || "double".equals(expectedType)) {
                    collector.append("    f64.promote_f32\n");
                }
            } else {
                collector.append("    local.get $__simd_tmp\n    i32x4.extract_lane 0\n");
                collector.append("    local.get $__simd_tmp\n    i32x4.extract_lane 1\n    i32.add\n");
                collector.append("    local.get $__simd_tmp\n    i32x4.extract_lane 2\n    i32.add\n");
                collector.append("    local.get $__simd_tmp\n    i32x4.extract_lane 3\n    i32.add\n");
            }
        }
    }

    private String getNumericLiteralString(Expression expr, boolean isFloat) {
        if (expr.isDoubleLiteralExpr()) {
            String val = expr.asDoubleLiteralExpr().getValue().replaceAll("[fFdD]", "");
            if (isFloat && !val.contains(".")) val += ".0";
            return val;
        }
        if (expr.isIntegerLiteralExpr()) {
            String val = expr.asIntegerLiteralExpr().getValue().replaceAll("[lL_]", "");
            if (isFloat) val += ".0";
            return val;
        }
        if (expr.isUnaryExpr() && expr.asUnaryExpr().getOperator() == UnaryExpr.Operator.MINUS) {
            return "-" + getNumericLiteralString(expr.asUnaryExpr().getExpression(), isFloat);
        }
        return isFloat ? "0.0" : "0";
    }

    private boolean isMemoryCall(MethodCallExpr call, Map<String, String> localJavaTypes) {
        if (!call.getScope().isPresent()) return false;
        Expression scope = call.getScope().get();
        String scopeStr = scope.toString();
        String scopeJavaType = getExpressionJavaType(scope, localJavaTypes);

        if (scopeStr.equals("Memory") || scopeStr.endsWith(".Memory") ||
                "Memory".equals(scopeJavaType) || (scopeJavaType != null && scopeJavaType.endsWith(".Memory"))) {
            return true;
        }
        return false;
    }

    private String resolveMemoryIdentifier(Expression expr, Map<String, String> localJavaTypes) {
        if (expr == null) return "";
        String s = expr.toString();
        if (s.equals("Memory.HEAP") || s.endsWith(".HEAP")) return "$heap";
        if (s.equals("Memory.STRINGS") || s.endsWith(".STRINGS")) return "$strings";
        if (s.equals("Memory.STACK") || s.endsWith(".STACK")) return "$stack";
        if (s.equals("Memory.DEFAULT") || s.endsWith(".DEFAULT")) return "";
        if (expr.isStringLiteralExpr()) {
            String val = expr.asStringLiteralExpr().getValue();
            if (val.equalsIgnoreCase("heap")) return "$heap";
            if (val.equalsIgnoreCase("strings")) return "$strings";
            if (val.equalsIgnoreCase("stack")) return "$stack";
            if (val.equalsIgnoreCase("memory") || val.isEmpty()) return "";
            return "$" + val;
        }
        if (expr.isMethodCallExpr()) {
            MethodCallExpr m = expr.asMethodCallExpr();
            if ((m.getNameAsString().equals("of") || m.getNameAsString().equals("named")) && m.getArguments().size() > 0) {
                return resolveMemoryIdentifier(m.getArgument(0), localJavaTypes);
            }
        }
        if (expr.isObjectCreationExpr()) {
            var oce = expr.asObjectCreationExpr();
            if (oce.getArguments().size() > 0) {
                return resolveMemoryIdentifier(oce.getArgument(0), localJavaTypes);
            }
        }
        if (expr.isNameExpr()) {
            String varName = expr.asNameExpr().getNameAsString();
            if (varName.toLowerCase().contains("heap")) return "$heap";
            if (varName.toLowerCase().contains("string")) return "$strings";
            if (varName.toLowerCase().contains("stack")) return "$stack";
        }
        return "";
    }

    private void translateMemoryCall(MethodCallExpr call, StringBuilder collector, Map<String, String> localTypes, Map<String, String> localElementTypes, Map<String, String> localJavaTypes, String expectedType) {
        Expression scope = call.getScope().get();
        String mName = call.getNameAsString();
        String memId = resolveMemoryIdentifier(scope, localJavaTypes);

        if (mName.equals("size")) {
            if (call.getArguments().size() > 0) {
                memId = resolveMemoryIdentifier(call.getArgument(0), localJavaTypes);
            }
            collector.append(memId.isEmpty() ? "    memory.size\n" : "    memory.size " + memId + "\n");
        } else if (mName.equals("grow")) {
            Expression pagesArg;
            if (call.getArguments().size() == 2) {
                memId = resolveMemoryIdentifier(call.getArgument(0), localJavaTypes);
                pagesArg = call.getArgument(1);
            } else {
                pagesArg = call.getArgument(0);
            }
            translateExpression(pagesArg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append(memId.isEmpty() ? "    memory.grow\n" : "    memory.grow " + memId + "\n");
        } else if (mName.equals("copy")) {
            if (call.getArguments().size() == 5) {
                String dstMem = resolveMemoryIdentifier(call.getArgument(0), localJavaTypes);
                Expression dstOffset = call.getArgument(1);
                String srcMem = resolveMemoryIdentifier(call.getArgument(2), localJavaTypes);
                Expression srcOffset = call.getArgument(3);
                Expression byteCount = call.getArgument(4);

                translateExpression(dstOffset, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                translateExpression(srcOffset, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                translateExpression(byteCount, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                if (dstMem.isEmpty() && srcMem.isEmpty()) {
                    collector.append("    memory.copy\n");
                } else {
                    collector.append("    memory.copy ").append(dstMem.isEmpty() ? "$strings" : dstMem)
                            .append(" ").append(srcMem.isEmpty() ? "$strings" : srcMem).append("\n");
                }
            } else if (call.getArguments().size() == 4) {
                String dstMem = memId;
                Expression dstOffset = call.getArgument(0);
                String srcMem = resolveMemoryIdentifier(call.getArgument(1), localJavaTypes);
                Expression srcOffset = call.getArgument(2);
                Expression byteCount = call.getArgument(3);

                translateExpression(dstOffset, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                translateExpression(srcOffset, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                translateExpression(byteCount, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                if (dstMem.isEmpty() && srcMem.isEmpty()) {
                    collector.append("    memory.copy\n");
                } else {
                    collector.append("    memory.copy ").append(dstMem.isEmpty() ? "$strings" : dstMem)
                            .append(" ").append(srcMem.isEmpty() ? "$strings" : srcMem).append("\n");
                }
            }
        } else if (mName.equals("copyFrom")) {
            String dstMem = memId;
            Expression dstOffset = call.getArgument(0);
            Expression srcOffset = call.getArgument(1);
            Expression byteCount = call.getArgument(2);

            translateExpression(dstOffset, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            translateExpression(srcOffset, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            translateExpression(byteCount, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            if (dstMem.isEmpty()) {
                collector.append("    memory.copy\n");
            } else {
                collector.append("    memory.copy ").append(dstMem).append(" ").append(dstMem).append("\n");
            }
        } else if (mName.equals("fill")) {
            Expression offsetArg, valArg, countArg;
            if (call.getArguments().size() == 4) {
                memId = resolveMemoryIdentifier(call.getArgument(0), localJavaTypes);
                offsetArg = call.getArgument(1);
                valArg = call.getArgument(2);
                countArg = call.getArgument(3);
            } else {
                offsetArg = call.getArgument(0);
                valArg = call.getArgument(1);
                countArg = call.getArgument(2);
            }
            translateExpression(offsetArg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            translateExpression(valArg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            translateExpression(countArg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append(memId.isEmpty() ? "    memory.fill\n" : "    memory.fill " + memId + "\n");
        } else if (mName.equals("loadI32")) {
            Expression offsetArg;
            if (call.getArguments().size() == 2) {
                memId = resolveMemoryIdentifier(call.getArgument(0), localJavaTypes);
                offsetArg = call.getArgument(1);
            } else {
                offsetArg = call.getArgument(0);
            }
            translateExpression(offsetArg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append(memId.isEmpty() ? "    i32.load\n" : "    i32.load " + memId + "\n");
        } else if (mName.equals("storeI32")) {
            Expression offsetArg, valArg;
            if (call.getArguments().size() == 3) {
                memId = resolveMemoryIdentifier(call.getArgument(0), localJavaTypes);
                offsetArg = call.getArgument(1);
                valArg = call.getArgument(2);
            } else {
                offsetArg = call.getArgument(0);
                valArg = call.getArgument(1);
            }
            translateExpression(offsetArg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            translateExpression(valArg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append(memId.isEmpty() ? "    i32.store\n" : "    i32.store " + memId + "\n");
        } else if (mName.equals("loadF64")) {
            Expression offsetArg;
            if (call.getArguments().size() == 2) {
                memId = resolveMemoryIdentifier(call.getArgument(0), localJavaTypes);
                offsetArg = call.getArgument(1);
            } else {
                offsetArg = call.getArgument(0);
            }
            translateExpression(offsetArg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append(memId.isEmpty() ? "    f64.load\n" : "    f64.load " + memId + "\n");
        } else if (mName.equals("storeF64")) {
            Expression offsetArg, valArg;
            if (call.getArguments().size() == 3) {
                memId = resolveMemoryIdentifier(call.getArgument(0), localJavaTypes);
                offsetArg = call.getArgument(1);
                valArg = call.getArgument(2);
            } else {
                offsetArg = call.getArgument(0);
                valArg = call.getArgument(1);
            }
            translateExpression(offsetArg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            translateExpression(valArg, collector, localTypes, localElementTypes, localJavaTypes, "f64");
            collector.append(memId.isEmpty() ? "    f64.store\n" : "    f64.store " + memId + "\n");
        } else if (mName.equals("loadF32")) {
            Expression offsetArg;
            if (call.getArguments().size() == 2) {
                memId = resolveMemoryIdentifier(call.getArgument(0), localJavaTypes);
                offsetArg = call.getArgument(1);
            } else {
                offsetArg = call.getArgument(0);
            }
            translateExpression(offsetArg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append(memId.isEmpty() ? "    f32.load\n" : "    f32.load " + memId + "\n");
        } else if (mName.equals("storeF32")) {
            Expression offsetArg, valArg;
            if (call.getArguments().size() == 3) {
                memId = resolveMemoryIdentifier(call.getArgument(0), localJavaTypes);
                offsetArg = call.getArgument(1);
                valArg = call.getArgument(2);
            } else {
                offsetArg = call.getArgument(0);
                valArg = call.getArgument(1);
            }
            translateExpression(offsetArg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            translateExpression(valArg, collector, localTypes, localElementTypes, localJavaTypes, "f32");
            collector.append(memId.isEmpty() ? "    f32.store\n" : "    f32.store " + memId + "\n");
        } else if (mName.equals("loadI64")) {
            Expression offsetArg;
            if (call.getArguments().size() == 2) {
                memId = resolveMemoryIdentifier(call.getArgument(0), localJavaTypes);
                offsetArg = call.getArgument(1);
            } else {
                offsetArg = call.getArgument(0);
            }
            translateExpression(offsetArg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append(memId.isEmpty() ? "    i64.load\n" : "    i64.load " + memId + "\n");
        } else if (mName.equals("storeI64")) {
            Expression offsetArg, valArg;
            if (call.getArguments().size() == 3) {
                memId = resolveMemoryIdentifier(call.getArgument(0), localJavaTypes);
                offsetArg = call.getArgument(1);
                valArg = call.getArgument(2);
            } else {
                offsetArg = call.getArgument(0);
                valArg = call.getArgument(1);
            }
            translateExpression(offsetArg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            translateExpression(valArg, collector, localTypes, localElementTypes, localJavaTypes, "i64");
            collector.append(memId.isEmpty() ? "    i64.store\n" : "    i64.store " + memId + "\n");
        } else if (mName.equals("loadI8")) {
            Expression offsetArg = call.getArgument(call.getArguments().size() - 1);
            if (call.getArguments().size() == 2) memId = resolveMemoryIdentifier(call.getArgument(0), localJavaTypes);
            translateExpression(offsetArg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append(memId.isEmpty() ? "    i32.load8_s\n" : "    i32.load8_s " + memId + "\n");
        } else if (mName.equals("loadU8")) {
            Expression offsetArg = call.getArgument(call.getArguments().size() - 1);
            if (call.getArguments().size() == 2) memId = resolveMemoryIdentifier(call.getArgument(0), localJavaTypes);
            translateExpression(offsetArg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append(memId.isEmpty() ? "    i32.load8_u\n" : "    i32.load8_u " + memId + "\n");
        } else if (mName.equals("storeI8")) {
            Expression offsetArg = call.getArgument(call.getArguments().size() - 2);
            Expression valArg = call.getArgument(call.getArguments().size() - 1);
            if (call.getArguments().size() == 3) memId = resolveMemoryIdentifier(call.getArgument(0), localJavaTypes);
            translateExpression(offsetArg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            translateExpression(valArg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append(memId.isEmpty() ? "    i32.store8\n" : "    i32.store8 " + memId + "\n");
        } else if (mName.equals("loadI16")) {
            Expression offsetArg = call.getArgument(call.getArguments().size() - 1);
            if (call.getArguments().size() == 2) memId = resolveMemoryIdentifier(call.getArgument(0), localJavaTypes);
            translateExpression(offsetArg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append(memId.isEmpty() ? "    i32.load16_s\n" : "    i32.load16_s " + memId + "\n");
        } else if (mName.equals("loadU16")) {
            Expression offsetArg = call.getArgument(call.getArguments().size() - 1);
            if (call.getArguments().size() == 2) memId = resolveMemoryIdentifier(call.getArgument(0), localJavaTypes);
            translateExpression(offsetArg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append(memId.isEmpty() ? "    i32.load16_u\n" : "    i32.load16_u " + memId + "\n");
        } else if (mName.equals("storeI16")) {
            Expression offsetArg = call.getArgument(call.getArguments().size() - 2);
            Expression valArg = call.getArgument(call.getArguments().size() - 1);
            if (call.getArguments().size() == 3) memId = resolveMemoryIdentifier(call.getArgument(0), localJavaTypes);
            translateExpression(offsetArg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            translateExpression(valArg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append(memId.isEmpty() ? "    i32.store16\n" : "    i32.store16 " + memId + "\n");
        } else if (mName.equals("of") || mName.equals("named")) {
            int idx = 0;
            if (memId.equals("$heap")) idx = 1;
            else if (memId.equals("$stack")) idx = 2;
            collector.append("    i32.const ").append(idx).append("\n");
        }
    }

    private void emitDefaultRootVirtualMethods(StringBuilder collector) {
        if (classRegistry == null) return;
        for (ClassModel cm : classRegistry.getClasses().values()) {
            if (cm.isInterface() || !cm.isOopClass()) continue;

            // Slot 0: toString
            MethodModel toStr = cm.getMethodByName("toString");
            if (toStr != null && toStr.isSynthetic()) {
                collector.append("  ;; Default toString() for ").append(cm.getName()).append("\n");
                collector.append("  (func $").append(cm.getName()).append("_toString (export \"").append(cm.getName()).append("_toString\") (param $this i32) (result i32)\n");
                if (cm.isEnum()) {
                    collector.append("    local.get $this\n");
                    collector.append("    i32.load offset=4\n");
                } else {
                    collector.append("    local.get $this\n");
                    collector.append("    call $__str_from_i32\n");
                }
                collector.append("  )\n");
            }

            // Slot 1: equals
            MethodModel eq = cm.getMethodByName("equals");
            if (eq != null && eq.isSynthetic()) {
                collector.append("  ;; Default equals() for ").append(cm.getName()).append("\n");
                collector.append("  (func $").append(cm.getName()).append("_equals (export \"").append(cm.getName()).append("_equals\") (param $this i32) (param $other i32) (result i32)\n");
                collector.append("    local.get $this\n");
                collector.append("    local.get $other\n");
                collector.append("    i32.eq\n");
                collector.append("  )\n");
            }

            // Slot 2: hashCode
            MethodModel hc = cm.getMethodByName("hashCode");
            if (hc != null && hc.isSynthetic()) {
                collector.append("  ;; Default hashCode() for ").append(cm.getName()).append("\n");
                collector.append("  (func $").append(cm.getName()).append("_hashCode (export \"").append(cm.getName()).append("_hashCode\") (param $this i32) (result i32)\n");
                collector.append("    local.get $this\n");
                collector.append("  )\n");
            }

            // Slot 8: run
            MethodModel runMm = cm.getMethodByName("run");
            if (runMm != null && runMm.isSynthetic() && !cm.getName().equals("Thread")) {
                collector.append("  ;; Default run() for ").append(cm.getName()).append("\n");
                collector.append("  (func $").append(cm.getName()).append("_run (export \"").append(cm.getName()).append("_run\") (param $this i32)\n");
                collector.append("  )\n");
            }

            // Slot 9: getClass
            MethodModel getCls = cm.getMethodByName("getClass");
            if (getCls != null && getCls.isSynthetic()) {
                collector.append("  ;; Default getClass() for ").append(cm.getName()).append("\n");
                collector.append("  (func $").append(cm.getName()).append("_getClass (export \"").append(cm.getName()).append("_getClass\") (param $this i32) (result i32)\n");
                collector.append("    global.get $__class_").append(cm.getName()).append("\n");
                collector.append("  )\n");
            }
        }
    }

    private void emitEnumInitializersAndHelpers(CompilationUnit cu, StringBuilder collector) {
        if (classRegistry == null) return;
        List<EnumDeclaration> enumDecls = cu.findAll(EnumDeclaration.class);
        if (enumDecls.isEmpty()) {
            collector.append("  (func $__init_enums (export \"__init_enums\")\n");
            collector.append("  )\n");
            return;
        }

        for (EnumDeclaration ed : enumDecls) {
            String enumName = ed.getNameAsString();
            ClassModel cm = classRegistry.getClass(enumName);
            if (cm == null) continue;

            collector.append("  ;; Standard enum methods for ").append(enumName).append("\n");
            collector.append("  (func $").append(enumName).append("_name (export \"").append(enumName).append("_name\") (param $this i32) (result i32)\n");
            collector.append("    local.get $this\n");
            collector.append("    i32.load offset=4\n");
            collector.append("  )\n");

            collector.append("  (func $").append(enumName).append("_ordinal (export \"").append(enumName).append("_ordinal\") (param $this i32) (result i32)\n");
            collector.append("    local.get $this\n");
            collector.append("    i32.load offset=8\n");
            collector.append("  )\n");

            collector.append("  (func $").append(enumName).append("_compareTo (export \"").append(enumName).append("_compareTo\") (param $this i32) (param $other i32) (result i32)\n");
            collector.append("    local.get $this\n");
            collector.append("    i32.load offset=8\n");
            collector.append("    local.get $other\n");
            collector.append("    i32.load offset=8\n");
            collector.append("    i32.sub\n");
            collector.append("  )\n");

            int count = cm.getEnumConstants().size();
            collector.append("  (func $").append(enumName).append("_values (export \"").append(enumName).append("_values\") (result i32)\n");
            collector.append("    (local $arr i32)\n");
            collector.append("    i32.const ").append(4 + count * 4).append("\n");
            collector.append("    call $__heap_alloc\n");
            collector.append("    local.set $arr\n");
            collector.append("    local.get $arr\n");
            collector.append("    i32.const ").append(count).append("\n");
            collector.append("    i32.store\n");
            for (int i = 0; i < count; i++) {
                String cName = cm.getEnumConstants().get(i);
                collector.append("    local.get $arr\n");
                collector.append("    global.get $").append(enumName).append("_").append(cName).append("\n");
                collector.append("    i32.store offset=").append(4 + i * 4).append("\n");
            }
            collector.append("    local.get $arr\n");
            collector.append("  )\n");

            collector.append("  (func $").append(enumName).append("_valueOf (export \"").append(enumName).append("_valueOf\") (param $name i32) (result i32)\n");
            for (int i = 0; i < count; i++) {
                String cName = cm.getEnumConstants().get(i);
                StringConstant sc = stringPool != null ? stringPool.get(cName) : null;
                int scOffset = sc != null ? sc.offset : 0;
                collector.append("    local.get $name\n");
                collector.append("    i32.const ").append(scOffset).append("\n");
                collector.append("    call $__str_equals\n");
                collector.append("    if\n");
                collector.append("      global.get $").append(enumName).append("_").append(cName).append("\n");
                collector.append("      return\n");
                collector.append("    end\n");
            }
            collector.append("    i32.const 0\n");
            collector.append("  )\n");
        }

        collector.append("  ;; Enum Singleton Initializer\n");
        collector.append("  (func $__init_enums (export \"__init_enums\")\n");
        collector.append("    (local $inst i32)\n");
        collector.append("    call $__init_classes\n");
        for (EnumDeclaration ed : enumDecls) {
            String enumName = ed.getNameAsString();
            ClassModel cm = classRegistry.getClass(enumName);
            if (cm == null) continue;
            int structSize = Math.max(16, cm.getStructSize());
            int vtableBase = cm.getVtableBase();

            for (int i = 0; i < cm.getEnumConstants().size(); i++) {
                String cName = cm.getEnumConstants().get(i);
                StringConstant sc = stringPool != null ? stringPool.get(cName) : null;
                int nameOffset = sc != null ? sc.offset : 0;

                collector.append("    i32.const ").append(structSize).append("\n");
                collector.append("    call $__heap_alloc\n");
                collector.append("    local.set $inst\n");
                collector.append("    local.get $inst\n");
                collector.append("    i32.const ").append(vtableBase).append("\n");
                collector.append("    i32.store offset=0\n");
                collector.append("    local.get $inst\n");
                collector.append("    i32.const ").append(nameOffset).append("\n");
                collector.append("    i32.store offset=4\n");
                collector.append("    local.get $inst\n");
                collector.append("    i32.const ").append(i).append("\n");
                collector.append("    i32.store offset=8\n");

                List<Expression> args = cm.getEnumConstantArgs().getOrDefault(cName, List.of());
                if (!cm.getConstructors().isEmpty()) {
                    MethodModel constructor;
                    try { constructor = classRegistry.resolveOverload(cm.getConstructors(), args.stream()
                            .map(arg -> getExpressionJavaType(arg, Collections.emptyMap())).toList()); }
                    catch (IllegalArgumentException error) { throw JavaSourcePreparation.error(ed, error.getMessage()); }
                    collector.append("    local.get $inst\n");
                    for (int argument = 0; argument < args.size(); argument++)
                        translateExpression(args.get(argument), collector, Collections.emptyMap(), Collections.emptyMap(),
                                Collections.emptyMap(), constructor.getParamWasmTypes().get(argument));
                    collector.append("    call ").append(constructor.getFunctionName()).append("\n");
                }

                collector.append("    local.get $inst\n");
                collector.append("    global.set $").append(enumName).append("_").append(cName).append("\n");
            }
        }
        collector.append("  )\n");
    }

    private void emitClassInitializersAndHelpers(CompilationUnit cu, StringBuilder collector) {
        if (classRegistry == null) return;

        // Class methods: $Class_getName, $Class_getSimpleName, $Class_getId, $Class_isInterface, $Class_isEnum, $Class_isArray, $Class_getSuperclass, $Class_isInstance, $Class_forName
        collector.append("  ;; --- Class Reflection Methods ---\n");
        collector.append("  (func $Class_getName (export \"Class_getName\") (param $this i32) (result i32)\n");
        collector.append("    local.get $this\n");
        collector.append("    i32.load offset=4\n");
        collector.append("  )\n");
        collector.append("  (func $Class_getSimpleName (export \"Class_getSimpleName\") (param $this i32) (result i32)\n");
        collector.append("    local.get $this\n");
        collector.append("    i32.load offset=8\n");
        collector.append("  )\n");
        collector.append("  (func $Class_getId (export \"Class_getId\") (param $this i32) (result i32)\n");
        collector.append("    local.get $this\n");
        collector.append("    i32.load offset=12\n");
        collector.append("  )\n");
        collector.append("  (func $Class_isInterface (export \"Class_isInterface\") (param $this i32) (result i32)\n");
        collector.append("    local.get $this\n");
        collector.append("    i32.load offset=16\n");
        collector.append("  )\n");
        collector.append("  (func $Class_isEnum (export \"Class_isEnum\") (param $this i32) (result i32)\n");
        collector.append("    local.get $this\n");
        collector.append("    i32.load offset=20\n");
        collector.append("  )\n");
        collector.append("  (func $Class_isArray (export \"Class_isArray\") (param $this i32) (result i32)\n");
        collector.append("    local.get $this\n");
        collector.append("    i32.load offset=4\n");
        collector.append("    i32.load8_u\n");
        collector.append("    i32.const 91\n"); // '['
        collector.append("    i32.eq\n");
        collector.append("  )\n");
        collector.append("  (func $Class_getSuperclass (export \"Class_getSuperclass\") (param $this i32) (result i32)\n");
        collector.append("    local.get $this\n");
        collector.append("    i32.load offset=24\n");
        collector.append("  )\n");
        collector.append("  (func $Class_isInstance (export \"Class_isInstance\") (param $this i32) (param $obj i32) (result i32)\n");
        collector.append("    (local $objCls i32)\n");
        collector.append("    local.get $obj\n");
        collector.append("    i32.eqz\n");
        collector.append("    if\n");
        collector.append("      i32.const 0\n");
        collector.append("      return\n");
        collector.append("    end\n");
        collector.append("    local.get $obj\n");
        collector.append("    i32.load offset=0\n");
        collector.append("    call $__get_class_from_vtable\n");
        collector.append("    local.set $objCls\n");
        collector.append("    (block $done\n");
        collector.append("      (loop $check\n");
        collector.append("        local.get $objCls\n");
        collector.append("        i32.eqz\n");
        collector.append("        br_if $done\n");
        collector.append("        local.get $objCls\n");
        collector.append("        local.get $this\n");
        collector.append("        i32.eq\n");
        collector.append("        if\n");
        collector.append("          i32.const 1\n");
        collector.append("          return\n");
        collector.append("        end\n");
        collector.append("        local.get $objCls\n");
        collector.append("        i32.load offset=24\n");
        collector.append("        local.set $objCls\n");
        collector.append("        br $check\n");
        collector.append("      )\n");
        collector.append("    )\n");
        collector.append("    i32.const 0\n");
        collector.append("  )\n");

        collector.append("  (func $Class_forName (export \"Class_forName\") (param $name i32) (result i32)\n");
        for (ClassModel cm : classRegistry.getClasses().values()) {
            String cName = cm.getName();
            StringConstant sc = stringPool != null ? stringPool.get(cName) : null;
            int scOffset = sc != null ? sc.offset : 0;
            collector.append("    local.get $name\n");
            collector.append("    i32.const ").append(scOffset).append("\n");
            collector.append("    call $__str_equals\n");
            collector.append("    if\n");
            collector.append("      global.get $__class_").append(cName).append("\n");
            collector.append("      return\n");
            collector.append("    end\n");
        }
        collector.append("    i32.const 0\n");
        collector.append("  )\n");

        // Helper: $__get_class_from_vtable
        collector.append("  (func $__get_class_from_vtable (param $vtableBase i32) (result i32)\n");
        for (ClassModel cm : classRegistry.getClasses().values()) {
            if (cm.isOopClass()) {
                collector.append("    local.get $vtableBase\n");
                collector.append("    i32.const ").append(cm.getVtableBase()).append("\n");
                collector.append("    i32.eq\n");
                collector.append("    if\n");
                collector.append("      global.get $__class_").append(cm.getName()).append("\n");
                collector.append("      return\n");
                collector.append("    end\n");
            }
        }
        collector.append("    i32.const 0\n");
        collector.append("  )\n");

        // Thread constructors and methods: $Thread__init, $Thread__init_1, $Thread__init_2, $Thread_start, $Thread_run, $Thread_getId, $Thread_getName, $Thread_setName, $Thread_isAlive, $Thread_currentThread
        collector.append("  ;; --- Standard Thread Methods ---\n");
        collector.append("  (func $Thread__init (export \"Thread__init\") (param $this i32)\n");
        collector.append("    local.get $this\n");
        collector.append("    i32.const 0\n");
        collector.append("    i32.const 0\n");
        collector.append("    call $Thread__init_2\n");
        collector.append("  )\n");

        collector.append("  (func $Thread__init_1 (export \"Thread__init_1\") (param $this i32) (param $target i32)\n");
        collector.append("    local.get $this\n");
        collector.append("    local.get $target\n");
        collector.append("    i32.const 0\n");
        collector.append("    call $Thread__init_2\n");
        collector.append("  )\n");

        collector.append("  (func $Thread__init_2 (export \"Thread__init_2\") (param $this i32) (param $target i32) (param $name i32)\n");
        collector.append("    local.get $this\n");
        collector.append("    local.get $target\n");
        collector.append("    i32.store offset=4\n");
        collector.append("    local.get $this\n");
        collector.append("    local.get $name\n");
        collector.append("    i32.store offset=8\n");
        collector.append("    local.get $this\n");
        collector.append("    i32.const 1\n");
        collector.append("    i32.store offset=12\n");
        collector.append("    local.get $this\n");
        collector.append("    i32.const 0\n");
        collector.append("    i32.store offset=16\n");
        collector.append("  )\n");

        collector.append("  (func $Thread_start (export \"Thread_start\") (param $this i32)\n");
        collector.append("    local.get $this\n");
        collector.append("    i32.const 1\n");
        collector.append("    i32.store offset=16\n");
        collector.append("    local.get $this\n");
        collector.append("    i32.load offset=4\n");
        collector.append("    if\n");
        collector.append("      local.get $this\n");
        collector.append("      i32.load offset=4\n");
        collector.append("      local.get $this\n");
        collector.append("      i32.load offset=4\n");
        collector.append("      i32.load offset=0\n");
        collector.append("      i32.const 8\n");
        collector.append("      i32.add\n");
        collector.append("      call_indirect (type ").append(classRegistry.getOrCreateTypeSignature("(func (param i32))")).append(")\n");
        collector.append("    else\n");
        collector.append("      local.get $this\n");
        collector.append("      local.get $this\n");
        collector.append("      i32.load offset=0\n");
        collector.append("      i32.const 8\n");
        collector.append("      i32.add\n");
        collector.append("      call_indirect (type ").append(classRegistry.getOrCreateTypeSignature("(func (param i32))")).append(")\n");
        collector.append("    end\n");
        collector.append("  )\n");

        collector.append("  (func $Thread_run (export \"Thread_run\") (param $this i32)\n");
        collector.append("    local.get $this\n");
        collector.append("    i32.load offset=4\n");
        collector.append("    if\n");
        collector.append("      local.get $this\n");
        collector.append("      i32.load offset=4\n");
        collector.append("      local.get $this\n");
        collector.append("      i32.load offset=4\n");
        collector.append("      i32.load offset=0\n");
        collector.append("      i32.const 8\n");
        collector.append("      i32.add\n");
        collector.append("      call_indirect (type ").append(classRegistry.getOrCreateTypeSignature("(func (param i32))")).append(")\n");
        collector.append("    end\n");
        collector.append("  )\n");

        collector.append("  (func $Thread_getId (export \"Thread_getId\") (param $this i32) (result i32)\n");
        collector.append("    local.get $this\n");
        collector.append("    i32.load offset=12\n");
        collector.append("  )\n");

        collector.append("  (func $Thread_getName (export \"Thread_getName\") (param $this i32) (result i32)\n");
        collector.append("    local.get $this\n");
        collector.append("    i32.load offset=8\n");
        collector.append("  )\n");

        collector.append("  (func $Thread_setName (export \"Thread_setName\") (param $this i32) (param $name i32)\n");
        collector.append("    local.get $this\n");
        collector.append("    local.get $name\n");
        collector.append("    i32.store offset=8\n");
        collector.append("  )\n");

        collector.append("  (func $Thread_isAlive (export \"Thread_isAlive\") (param $this i32) (result i32)\n");
        collector.append("    local.get $this\n");
        collector.append("    i32.load offset=16\n");
        collector.append("  )\n");

        collector.append("  (func $Thread_currentThread (export \"Thread_currentThread\") (result i32)\n");
        collector.append("    (local $t i32)\n");
        ClassModel threadCm = classRegistry.getClass("Thread");
        int threadVtable = threadCm != null ? threadCm.getVtableBase() : 0;
        StringConstant mainSc = stringPool != null ? stringPool.get("main") : null;
        int mainOffset = mainSc != null ? mainSc.offset : 0;
        collector.append("    i32.const 24\n");
        collector.append("    call $__heap_alloc\n");
        collector.append("    local.set $t\n");
        collector.append("    local.get $t\n");
        collector.append("    i32.const ").append(threadVtable).append("\n");
        collector.append("    i32.store offset=0\n");
        collector.append("    local.get $t\n");
        collector.append("    i32.const 0\n"); // target = null
        collector.append("    i32.store offset=4\n");
        collector.append("    local.get $t\n");
        collector.append("    i32.const ").append(mainOffset).append("\n"); // name = "main"
        collector.append("    i32.store offset=8\n");
        collector.append("    local.get $t\n");
        collector.append("    i32.const 1\n"); // id
        collector.append("    i32.store offset=12\n");
        collector.append("    local.get $t\n");
        collector.append("    i32.const 1\n"); // alive = true
        collector.append("    i32.store offset=16\n");
        collector.append("    local.get $t\n");
        collector.append("  )\n");

        // Class Descriptors Initializer ($__init_classes)
        ClassModel classCm = classRegistry.getClass("Class");
        int classVtable = classCm != null ? classCm.getVtableBase() : 0;
        collector.append("  ;; Class Descriptors Initializer\n");
        collector.append("  (func $__init_classes (export \"__init_classes\")\n");
        collector.append("    (local $inst i32)\n");

        for (ClassModel cm : classRegistry.getClasses().values()) {
            String cName = cm.getName();
            String simpleName = cName.contains(".") ? cName.substring(cName.lastIndexOf('.') + 1) : cName;
            StringConstant nameSc = stringPool != null ? stringPool.get(cName) : null;
            int nameOffset = nameSc != null ? nameSc.offset : 0;
            StringConstant simpleSc = stringPool != null ? stringPool.get(simpleName) : null;
            int simpleOffset = simpleSc != null ? simpleSc.offset : 0;

            collector.append("    i32.const 32\n");
            collector.append("    call $__heap_alloc\n");
            collector.append("    local.set $inst\n");
            collector.append("    local.get $inst\n");
            collector.append("    i32.const ").append(classVtable).append("\n");
            collector.append("    i32.store offset=0\n");
            collector.append("    local.get $inst\n");
            collector.append("    i32.const ").append(nameOffset).append("\n");
            collector.append("    i32.store offset=4\n");
            collector.append("    local.get $inst\n");
            collector.append("    i32.const ").append(simpleOffset).append("\n");
            collector.append("    i32.store offset=8\n");
            collector.append("    local.get $inst\n");
            collector.append("    i32.const ").append(cm.getClassId()).append("\n");
            collector.append("    i32.store offset=12\n");
            collector.append("    local.get $inst\n");
            collector.append("    i32.const ").append(cm.isInterface() ? 1 : 0).append("\n");
            collector.append("    i32.store offset=16\n");
            collector.append("    local.get $inst\n");
            collector.append("    i32.const ").append(cm.isEnum() ? 1 : 0).append("\n");
            collector.append("    i32.store offset=20\n");
            collector.append("    local.get $inst\n");
            collector.append("    i32.const 0\n");
            collector.append("    i32.store offset=24\n");
            collector.append("    local.get $inst\n");
            collector.append("    global.set $__class_").append(cName).append("\n");
        }

        // Second pass: set superclass pointers
        for (ClassModel cm : classRegistry.getClasses().values()) {
            if (cm.getSuperClassName() != null && classRegistry.hasClass(cm.getSuperClassName())) {
                collector.append("    global.get $__class_").append(cm.getName()).append("\n");
                collector.append("    global.get $__class_").append(cm.getSuperClassName()).append("\n");
                collector.append("    i32.store offset=24\n");
            }
        }
        collector.append("  )\n");
    }
}
