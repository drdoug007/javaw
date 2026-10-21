package com.example.javaw;

import com.example.javaw.js.JsAnnotationHelper;
import com.example.javaw.sourcemap.SourceMapTracker;
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
    private final Deque<LoopContext> loopStack = new ArrayDeque<>();
    private ClassModel currentClass;
    private RecordModel currentRecord;
    private int loopCounter = 0;
    private int forEachCounter = 0;
    private int switchCounter = 0;
    private int switchExprCounter = 0;

    public WasmGeneratorVisitor(Map<String, StringConstant> stringPool, Map<String, GlobalVar> globalVars, ClassRegistry classRegistry, SourceMapTracker sourceMapTracker) {
        this.stringPool = stringPool;
        this.globalVars = globalVars;
        this.classRegistry = classRegistry;
        this.sourceMapTracker = sourceMapTracker;
    }

    public WasmGeneratorVisitor(Map<String, StringConstant> stringPool, Map<String, GlobalVar> globalVars, ClassRegistry classRegistry) {
        this(stringPool, globalVars, classRegistry, null);
    }

    public WasmGeneratorVisitor(Map<String, StringConstant> stringPool, Map<String, GlobalVar> globalVars) {
        this(stringPool, globalVars, null, null);
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, StringBuilder collector) {
        String className = n.getNameAsString();
        currentClass = classRegistry != null ? classRegistry.getClass(className) : null;

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
        String className = n.getNameAsString();
        Map<String, String> localTypes = new HashMap<>();
        Map<String, String> localElementTypes = new HashMap<>();
        Map<String, String> localJavaTypes = new HashMap<>();
        forEachCounter = 0;
        switchCounter = 0;
        switchExprCounter = 0;

        String funcName = "$" + className + "__init";
        if (sourceMapTracker != null) {
            sourceMapTracker.track(collector, n, className + "__init");
        }
        collector.append("  (func ").append(funcName)
                .append(" (export \"").append(className).append("__init\")")
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
        String exportName = recordName + "_" + methodName;

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
        boolean isOopInstanceMethod = currentClass != null && currentClass.isOopClass() && !isStatic;
        String funcName;
        String exportName;

        if (isOopInstanceMethod) {
            funcName = "$" + currentClass.getName() + "_" + methodName;
            exportName = currentClass.getName() + "_" + methodName;
        } else {
            funcName = "$" + methodName;
            exportName = methodName;
        }

        if (sourceMapTracker != null) {
            sourceMapTracker.track(collector, n, exportName);
        }

        collector.append("  (func ").append(funcName)
                .append(" (export \"").append(exportName).append("\")");

        if (isOopInstanceMethod) {
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
        Optional<AnnotationExpr> jsAnnoOpt = JsAnnotationHelper.getJsAnnotation(n);
        boolean isJsInline = jsAnnoOpt.isPresent();

        if (isJsInline) {
            String className = currentClass != null ? currentClass.getName() : "Global";
            String importName = "js_inline_" + className + "_" + methodName;
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
                String vType = TypeMapper.mapType(vd.getType());
                if (vType == null) vType = "i32";
                localTypes.put(vName, vType);
                localJavaTypes.put(vName, vd.getType().asString());
                if (vd.getType().isArrayType()) {
                    localElementTypes.put(vName, TypeMapper.mapType(vd.getType().asArrayType().getComponentType()));
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

        List<TryStmt> tryStmts = body.findAll(TryStmt.class);
        for (TryStmt ts : tryStmts) {
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
            for (Expression arg : ecis.getArguments()) {
                translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            }
            int argCount = ecis.getArguments().size();
            String targetClass = ecis.isThis() ? currentClass.getName() : currentClass.getSuperClassName();
            String initFunc = "$" + targetClass + "__init";
            if (classRegistry != null && classRegistry.hasClass(targetClass)) {
                ClassModel targetCm = classRegistry.getClass(targetClass);
                for (MethodModel cons : targetCm.getConstructors()) {
                    if (cons.getParamNames().size() == argCount) {
                        initFunc = cons.getFunctionName();
                        break;
                    }
                }
            }
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
            String elemType = TypeMapper.mapType(forEach.getVariable().getVariables().get(0).getType());
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
            if (bin.getOperator() == BinaryExpr.Operator.PLUS && (isStringType(bin.getLeft(), localJavaTypes) || isStringType(bin.getRight(), localJavaTypes))) {
                translateStringToStack(bin.getLeft(), collector, localTypes, localElementTypes, localJavaTypes);
                translateStringToStack(bin.getRight(), collector, localTypes, localElementTypes, localJavaTypes);
                collector.append("    call $__str_concat\n");
                return;
            }
            String opType = inferType(bin, localTypes, localElementTypes, localJavaTypes, expectedType);
            translateExpression(bin.getLeft(), collector, localTypes, localElementTypes, localJavaTypes, opType);
            translateExpression(bin.getRight(), collector, localTypes, localElementTypes, localJavaTypes, opType);
            collector.append("    ").append(getWasmBinaryOp(bin.getOperator(), opType)).append("\n");
            if ("f64".equals(expectedType) && "f32".equals(opType)) {
                collector.append("    f64.promote_f32\n");
            } else if ("f32".equals(expectedType) && "f64".equals(opType)) {
                collector.append("    f32.demote_f64\n");
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
                    collector.append(isGlobal ? "    global.get $" : "    local.get $").append(var).append("\n");
                    collector.append("    i32.const 1\n");
                    collector.append("    i32.add\n");
                    collector.append(isGlobal ? "    global.set $" : "    local.set $").append(var).append("\n");
                }
            } else if (u.getOperator() == UnaryExpr.Operator.POSTFIX_DECREMENT || u.getOperator() == UnaryExpr.Operator.PREFIX_DECREMENT) {
                if (u.getExpression().isNameExpr()) {
                    String var = u.getExpression().asNameExpr().getNameAsString();
                    boolean isGlobal = globalVars != null && globalVars.containsKey(var) && !localTypes.containsKey(var);
                    collector.append(isGlobal ? "    global.get $" : "    local.get $").append(var).append("\n");
                    collector.append("    i32.const 1\n");
                    collector.append("    i32.sub\n");
                    collector.append(isGlobal ? "    global.set $" : "    local.set $").append(var).append("\n");
                }
            } else {
                translateExpression(u.getExpression(), collector, localTypes, localElementTypes, localJavaTypes, expectedType);
            }
        } else if (expr.isIntegerLiteralExpr()) {
            if ("f64".equals(expectedType)) {
                collector.append("    f64.const ").append(expr.asIntegerLiteralExpr().getValue()).append("\n");
            } else if ("f32".equals(expectedType)) {
                collector.append("    f32.const ").append(expr.asIntegerLiteralExpr().getValue()).append(".0\n");
            } else {
                collector.append("    i32.const ").append(expr.asIntegerLiteralExpr().getValue()).append("\n");
            }
        } else if (expr.isLongLiteralExpr()) {
            String val = expr.asLongLiteralExpr().getValue().replaceAll("[lL]", "");
            collector.append("    i64.const ").append(val).append("\n");
        } else if (expr.isDoubleLiteralExpr()) {
            String dVal = expr.asDoubleLiteralExpr().getValue().replaceAll("[fFdD]", "");
            if ("f32".equals(expectedType)) {
                collector.append("    f32.const ").append(dVal).append("\n");
            } else {
                collector.append("    f64.const ").append(dVal).append("\n");
            }
        } else if (expr.isBooleanLiteralExpr()) {
            collector.append("    i32.const ").append(expr.asBooleanLiteralExpr().getValue() ? "1" : "0").append("\n");
        } else if (expr.isCharLiteralExpr()) {
            collector.append("    i32.const ").append((int) expr.asCharLiteralExpr().getValue().charAt(0)).append("\n");
        } else if (expr.isStringLiteralExpr()) {
            String val = expr.asStringLiteralExpr().getValue();
            StringConstant sc = stringPool != null ? stringPool.get(val) : null;
            if (sc != null) {
                collector.append("    i32.const ").append(sc.offset).append("\n");
            } else {
                collector.append("    i32.const 0\n");
            }
        } else if (expr.isNullLiteralExpr()) {
            collector.append("    i32.const 0\n");
        } else if (expr.isConditionalExpr()) {
            ConditionalExpr cond = expr.asConditionalExpr();
            String resType = inferType(cond.getThenExpr(), localTypes, localElementTypes, localJavaTypes, expectedType);
            if (resType == null) resType = "i32";
            translateExpression(cond.getCondition(), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    if (result ").append(resType).append(")\n");
            translateExpression(cond.getThenExpr(), collector, localTypes, localElementTypes, localJavaTypes, resType);
            collector.append("    else\n");
            translateExpression(cond.getElseExpr(), collector, localTypes, localElementTypes, localJavaTypes, resType);
            collector.append("    end\n");
        } else if (expr.isThisExpr() || expr.isSuperExpr()) {
            collector.append("    local.get $this\n");
        } else if (expr.isCastExpr()) {
            translateExpression(expr.asCastExpr().getExpression(), collector, localTypes, localElementTypes, localJavaTypes, expectedType);
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
            if (localJavaTypes.containsKey(var) && classRegistry != null && classRegistry.isRecord(localJavaTypes.get(var))) {
                RecordModel rm = classRegistry.getRecord(localJavaTypes.get(var));
                for (FieldModel c : rm.getComponents()) {
                    collector.append("    local.get $").append(var).append("$").append(c.getName()).append("\n");
                }
            } else if (localTypes.containsKey(var)) {
                collector.append("    local.get $").append(var).append("\n");
                if ("f64".equals(expectedType) && "i32".equals(localTypes.get(var))) {
                    collector.append("    f64.convert_i32_s\n");
                } else if ("f32".equals(expectedType) && "f64".equals(localTypes.get(var))) {
                    collector.append("    f32.demote_f64\n");
                } else if ("f32".equals(expectedType) && "i32".equals(localTypes.get(var))) {
                    collector.append("    f32.convert_i32_s\n");
                }
            } else if (localTypes.containsKey("this$" + var)) {
                collector.append("    local.get $this$").append(var).append("\n");
                if ("f64".equals(expectedType) && "i32".equals(localTypes.get("this$" + var))) {
                    collector.append("    f64.convert_i32_s\n");
                } else if ("f32".equals(expectedType) && "f64".equals(localTypes.get("this$" + var))) {
                    collector.append("    f32.demote_f64\n");
                } else if ("f32".equals(expectedType) && "i32".equals(localTypes.get("this$" + var))) {
                    collector.append("    f32.convert_i32_s\n");
                }
            } else if (currentClass != null && currentClass.getField(var) != null && !currentClass.getField(var).isStatic()) {
                FieldModel fm = currentClass.getField(var);
                collector.append("    local.get $this\n");
                collector.append("    ").append(getLoadInstruction(fm.getWasmType())).append(" offset=").append(fm.getOffset()).append("\n");
                if ("f64".equals(expectedType) && "i32".equals(fm.getWasmType())) {
                    collector.append("    f64.convert_i32_s\n");
                } else if ("f32".equals(expectedType) && "f64".equals(fm.getWasmType())) {
                    collector.append("    f32.demote_f64\n");
                } else if ("f32".equals(expectedType) && "i32".equals(fm.getWasmType())) {
                    collector.append("    f32.convert_i32_s\n");
                }
            } else if (globalVars != null && globalVars.containsKey(var)) {
                collector.append("    global.get $").append(var).append("\n");
                if ("f64".equals(expectedType) && "i32".equals(globalVars.get(var).type)) {
                    collector.append("    f64.convert_i32_s\n");
                } else if ("f32".equals(expectedType) && "f64".equals(globalVars.get(var).type)) {
                    collector.append("    f32.demote_f64\n");
                } else if ("f32".equals(expectedType) && "i32".equals(globalVars.get(var).type)) {
                    collector.append("    f32.convert_i32_s\n");
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
                for (Expression arg : oce.getArguments()) {
                    translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                }
                int argCount = oce.getArguments().size();
                String initFunc = "$" + typeName + "__init";
                for (MethodModel cons : cm.getConstructors()) {
                    if (cons.getParamNames().size() == argCount) {
                        initFunc = cons.getFunctionName();
                        break;
                    }
                }
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
                    } else {
                        translateStatement(s, collector, localTypes, localElementTypes, localJavaTypes, exprType);
                    }
                }
            }
            collector.append("    )\n");
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
                String vTypeName = vd.getType().asString();
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
                    String vType = TypeMapper.mapType(vd.getType());
                    String elemType = vd.getType().isArrayType() ? TypeMapper.mapType(vd.getType().asArrayType().getComponentType()) : null;
                    translateExpression(vd.getInitializer().get(), collector, localTypes, localElementTypes, localJavaTypes, elemType != null ? elemType : vType);
                    collector.append("    local.set $").append(vd.getNameAsString()).append("\n");
                }
            }
        } else if (expr.isMethodCallExpr()) {
            MethodCallExpr call = expr.asMethodCallExpr();
            String mName = call.getNameAsString();
            if (isSystemOrWasiCall(call, localJavaTypes)) {
                translateSystemOrWasiCall(call, collector, localTypes, localElementTypes, localJavaTypes);
            } else if (isArrayUtilsCall(call)) {
                translateArrayUtilsCall(call, collector, localTypes, localElementTypes, localJavaTypes);
            } else if (isMathCall(call)) {
                translateMathCall(call, collector, localTypes, localElementTypes, localJavaTypes);
            } else if (isSimdCall(call, localJavaTypes)) {
                translateSimdCall(call, collector, localTypes, localElementTypes, localJavaTypes, expectedType);
            } else if (isMemoryCall(call, localJavaTypes)) {
                translateMemoryCall(call, collector, localTypes, localElementTypes, localJavaTypes, expectedType);
            } else if (call.getScope().isPresent()) {
                Expression scope = call.getScope().get();
                String scopeStr = scope.toString();
                String scopeJavaType = getExpressionJavaType(scope, localJavaTypes);
                if (scopeStr.equals("DOM") || scopeStr.endsWith(".DOM") || scopeStr.equals("Window") || scopeStr.endsWith(".Window") || scopeStr.equals("JSObject") || scopeStr.endsWith(".JSObject")) {
                    translateDomOrWindowStaticCall(call, collector, localTypes, localElementTypes, localJavaTypes);
                } else if (scopeStr.equals("Atomics") || scopeStr.endsWith(".Atomics")) {
                    translateAtomicsStaticCall(call, collector, localTypes, localElementTypes, localJavaTypes);
                } else if (scopeStr.equals("WasmThread") || scopeStr.endsWith(".WasmThread")) {
                    translateWasmThreadStaticCall(call, collector, localTypes, localElementTypes, localJavaTypes);
                } else if ("AtomicInteger".equals(scopeJavaType) || "AtomicInteger".equals(scopeStr) || (scopeJavaType != null && scopeJavaType.endsWith(".AtomicInteger"))) {
                    translateAtomicIntegerInstanceCall(call, collector, localTypes, localElementTypes, localJavaTypes);
                } else if ("JSObject".equals(getExpressionJavaType(scope, localJavaTypes))) {
                    translateJsInstanceCall(call, collector, localTypes, localElementTypes, localJavaTypes);
                } else if (DomHelper.isDomInstanceMethod(mName)) {
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
                        translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
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
                    if ("JSObject".equals(getExpressionJavaType(scope, localJavaTypes))) {
                        translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        collector.append("    call $dom_js_length\n");
                    } else if (scope.isNameExpr() && localJavaTypes.containsKey(scope.asNameExpr().getNameAsString()) && (localJavaTypes.get(scope.asNameExpr().getNameAsString()).equals("StringBuilder") || localJavaTypes.get(scope.asNameExpr().getNameAsString()).endsWith(".StringBuilder"))) {
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
                    if (scope.isNameExpr() && localJavaTypes.containsKey(scope.asNameExpr().getNameAsString()) && (localJavaTypes.get(scope.asNameExpr().getNameAsString()).equals("StringBuilder") || localJavaTypes.get(scope.asNameExpr().getNameAsString()).endsWith(".StringBuilder"))) {
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
                } else if (mName.equals("substring")) {
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
                } else if (mName.equals("equals")) {
                    translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "String");
                    translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "String");
                    collector.append("    call $__str_equals\n");
                } else if (mName.equals("indexOf")) {
                    translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "String");
                    Expression arg = call.getArgument(0);
                    if (isStringType(arg, localJavaTypes)) {
                        translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "String");
                    } else {
                        translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        collector.append("    call $__str_from_char\n");
                    }
                    collector.append("    call $__str_indexOf\n");
                } else if (mName.equals("concat")) {
                    translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "String");
                    translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "String");
                    collector.append("    call $__str_concat\n");
                } else if (mName.equals("isEmpty")) {
                    translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "String");
                    collector.append("    call $__str_length\n");
                    collector.append("    i32.eqz\n");
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
                    } else {
                        translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        collector.append("    call $__sb_append_i32\n");
                    }
                } else if (mName.equals("toString")) {
                    translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    collector.append("    call $__sb_toString\n");
                } else if (scope.isSuperExpr()) {
                    String superClassName = currentClass != null ? currentClass.getSuperClassName() : null;
                    collector.append("    local.get $this\n");
                    for (Expression arg : call.getArguments()) {
                        translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, null);
                    }
                    collector.append("    call $").append(superClassName).append("_").append(mName).append("\n");
                } else {
                    String targetType = getExpressionJavaType(scope, localJavaTypes);
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
                        MethodModel rmm = rm.getMethodByName(mName);
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
                        MethodModel rmm = rm.getMethodByName(mName);
                        for (int i = 0; i < call.getArguments().size(); i++) {
                            String pWasm = (rmm != null && i < rmm.getParamWasmTypes().size()) ? rmm.getParamWasmTypes().get(i) : "i32";
                            translateExpression(call.getArgument(i), collector, localTypes, localElementTypes, localJavaTypes, pWasm);
                        }
                        collector.append("    call $").append(recName).append("_").append(mName).append("\n");
                        return;
                    }
                    ClassModel targetCm = (targetType != null && classRegistry != null) ? classRegistry.getClass(targetType) : null;
                    MethodModel targetMm = targetCm != null ? targetCm.getMethodByName(mName) : null;

                    if (targetMm != null && targetMm.isVirtual() && targetCm != null && targetCm.isOopClass()) {
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
                        collector.append("    call_indirect (type ").append(targetMm.getTypeSignatureName()).append(")\n");
                    } else if (targetMm != null) {
                        if (!targetMm.isStatic() && targetCm != null && targetCm.isOopClass()) {
                            translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                        }
                        for (int i = 0; i < call.getArguments().size(); i++) {
                            String pWasm = i < targetMm.getParamWasmTypes().size() ? targetMm.getParamWasmTypes().get(i) : "i32";
                            translateExpression(call.getArgument(i), collector, localTypes, localElementTypes, localJavaTypes, pWasm);
                        }
                        collector.append("    call ").append(targetMm.getFunctionName()).append("\n");
                    } else {
                        translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, null);
                        for (Expression arg : call.getArguments()) {
                            translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, null);
                        }
                        collector.append("    call $").append(mName).append("\n");
                    }
                }
            } else {
                if (currentRecord != null && currentRecord.getComponent(mName) != null) {
                    collector.append("    local.get $this$").append(mName).append("\n");
                } else if (DomHelper.isDomStaticMethod(mName)) {
                    translateDomOrWindowStaticCall(call, collector, localTypes, localElementTypes, localJavaTypes);
                } else if (DomHelper.isDomInstanceMethod(mName)) {
                    for (Expression arg : call.getArguments()) {
                        translateDomStringArg(arg, collector, localTypes, localElementTypes, localJavaTypes);
                    }
                    collector.append("    call $dom_").append(mName).append("\n");
                } else if (currentClass != null && currentClass.getMethodByName(mName) != null) {
                    MethodModel mm = currentClass.getMethodByName(mName);
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
                        if (!mm.isStatic() && currentClass.isOopClass()) {
                            collector.append("    local.get $this\n");
                        }
                        for (int i = 0; i < call.getArguments().size(); i++) {
                            String pWasm = i < mm.getParamWasmTypes().size() ? mm.getParamWasmTypes().get(i) : "i32";
                            translateExpression(call.getArgument(i), collector, localTypes, localElementTypes, localJavaTypes, pWasm);
                        }
                        collector.append("    call ").append(mm.getFunctionName()).append("\n");
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

    private String getExpressionJavaType(Expression expr, Map<String, String> localJavaTypes) {
        if (expr.isNameExpr()) {
            return localJavaTypes.get(expr.asNameExpr().getNameAsString());
        }
        if (expr.isThisExpr()) {
            return currentClass != null ? currentClass.getName() : (currentRecord != null ? currentRecord.getName() : null);
        }
        if (expr.isObjectCreationExpr()) {
            return expr.asObjectCreationExpr().getTypeAsString();
        }
        if (expr.isMethodCallExpr()) {
            MethodCallExpr call = expr.asMethodCallExpr();
            String mName = call.getNameAsString();
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
                    mName.equals("readString") || mName.equals("getPath")) {
                return "String";
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
                    mName.equals("seek") || mName.equals("randomInt") || mName.equals("randomGet")) {
                return "int";
            }
            if (mName.equals("getLineWidth") || mName.equals("getGlobalAlpha") ||
                    mName.equals("getDouble") || mName.equals("callDouble") || mName.equals("asDouble") ||
                    mName.equals("currentTimeMillis") || mName.equals("clockTimeGet")) {
                return "double";
            }
            if (mName.equals("prompt")) {
                return "String";
            }
            if (mName.equals("isChecked") || mName.equals("hasClass") || mName.equals("contains") ||
                    mName.equals("getBoolean") || mName.equals("callBoolean") || mName.equals("asBoolean") || mName.equals("isArray") ||
                    mName.equals("writeString") || mName.equals("appendString") || mName.equals("fileExists") ||
                    mName.equals("exists") || mName.equals("deleteFile") || mName.equals("removeFile") ||
                    mName.equals("delete") || mName.equals("createDirectory") || mName.equals("mkdir")) {
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
                    MethodModel mm = rm.getMethodByName(mName);
                    if (mm != null) return mm.getReturnTypeName();
                } else if (scopeType != null && classRegistry != null && classRegistry.hasClass(scopeType)) {
                    MethodModel mm = classRegistry.getClass(scopeType).getMethodByName(mName);
                    if (mm != null) return mm.getReturnTypeName();
                }
            } else if (currentClass != null) {
                MethodModel mm = currentClass.getMethodByName(mName);
                if (mm != null) return mm.getReturnTypeName();
            } else if (currentRecord != null) {
                if (currentRecord.getComponent(mName) != null) {
                    return currentRecord.getComponent(mName).getTypeName();
                }
                MethodModel mm = currentRecord.getMethodByName(mName);
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
                    MethodModel mm = classRegistry.getClass(targetType).getMethodByName(mName);
                    if (mm != null && mm.getReturnWasmType() != null) return true;
                }
            } else if (currentClass != null) {
                MethodModel mm = currentClass.getMethodByName(mName);
                if (mm != null && mm.getReturnWasmType() != null) return true;
            }
        }
        if (expr.isObjectCreationExpr()) {
            return true;
        }
        return false;
    }

    private boolean isStringType(Expression expr, Map<String, String> localJavaTypes) {
        if (expr.isStringLiteralExpr()) return true;
        if (expr.isNameExpr()) {
            String name = expr.asNameExpr().getNameAsString();
            String jt = localJavaTypes.get(name);
            if ("String".equals(jt) || "java.lang.String".equals(jt)) return true;
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
                    mName.equals("getInnerText") || mName.equals("getAttribute") ||
                    mName.equals("getValue") || mName.equals("getStyle") ||
                    mName.equals("getFillStyle") || mName.equals("getStrokeStyle") ||
                    mName.equals("getFont") || mName.equals("getTextAlign") ||
                    mName.equals("getString") || mName.equals("callString") || mName.equals("asString") ||
                    mName.equals("prompt") || "String".equals(getExpressionJavaType(expr, localJavaTypes))) return true;
        }
        if (expr.isEnclosedExpr()) {
            return isStringType(expr.asEnclosedExpr().getInner(), localJavaTypes);
        }
        return false;
    }

    private void translateStringToStack(Expression expr, StringBuilder collector, Map<String, String> localTypes, Map<String, String> localElementTypes, Map<String, String> localJavaTypes) {
        if (isStringType(expr, localJavaTypes)) {
            translateExpression(expr, collector, localTypes, localElementTypes, localJavaTypes, "String");
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

    private boolean isSystemOrWasiCall(MethodCallExpr call, Map<String, String> localJavaTypes) {
        if (!call.getScope().isPresent()) return false;
        String scopeStr = call.getScope().get().toString();
        String mName = call.getNameAsString();
        if (scopeStr.equals("System.out") || scopeStr.equals("System.err")) {
            return mName.equals("println") || mName.equals("print");
        }
        if (scopeStr.equals("System")) {
            return mName.equals("exit");
        }
        if (scopeStr.equals("WASI") || scopeStr.endsWith(".WASI") || scopeStr.equals("WASIFile") || scopeStr.endsWith(".WASIFile")) {
            return true;
        }
        String scopeJavaType = getExpressionJavaType(call.getScope().get(), localJavaTypes);
        if ("WASIFile".equals(scopeJavaType)) {
            return true;
        }
        return false;
    }

    private void translateSystemOrWasiCall(MethodCallExpr call, StringBuilder collector, Map<String, String> localTypes, Map<String, String> localElementTypes, Map<String, String> localJavaTypes) {
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

        if (mName.equals("currentTimeMillis") || mName.equals("clockTimeGet")) {
            collector.append("    call $__wasi_clock_time_ms\n");
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
        return scopeStr.equals("Math") || scopeStr.equals("java.lang.Math");
    }

    private void translateMathCall(MethodCallExpr call, StringBuilder collector, Map<String, String> localTypes, Map<String, String> localElementTypes, Map<String, String> localJavaTypes) {
        String mName = call.getNameAsString();
        if (mName.equals("min") || mName.equals("max")) {
            Expression arg0 = call.getArgument(0);
            Expression arg1 = call.getArgument(1);
            String type0 = inferType(arg0, localTypes, localElementTypes, localJavaTypes, null);
            String type1 = inferType(arg1, localTypes, localElementTypes, localJavaTypes, null);
            boolean isF64 = "f64".equals(type0) || "f64".equals(type1) || arg0.isDoubleLiteralExpr() || arg1.isDoubleLiteralExpr();
            if (isF64) {
                translateExpression(arg0, collector, localTypes, localElementTypes, localJavaTypes, "f64");
                translateExpression(arg1, collector, localTypes, localElementTypes, localJavaTypes, "f64");
                collector.append("    f64.").append(mName).append("\n");
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
        if (mName.equals("sqrt") || mName.equals("floor") || mName.equals("ceil")) {
            translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "f64");
            collector.append("    f64.").append(mName).append("\n");
            return;
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

    private void translateWasmThreadStaticCall(MethodCallExpr call, StringBuilder collector, Map<String, String> localTypes, Map<String, String> localElementTypes, Map<String, String> localJavaTypes) {
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
        } else if (mName.equals("hardwareConcurrency")) {
            collector.append("    call $dom_thread_hardware_concurrency\n");
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

        if (mName.equals("get")) {
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
        if (expr.isIntegerLiteralExpr() || expr.isBooleanLiteralExpr() || expr.isCharLiteralExpr()) return "i32";
        if (expr.isStringLiteralExpr()) return "i32";
        if (expr.isNameExpr()) {
            String name = expr.asNameExpr().getNameAsString();
            if (localTypes.containsKey(name)) return localTypes.get(name);
            if (globalVars != null && globalVars.containsKey(name)) return globalVars.get(name).type;
            if (currentClass != null && currentClass.getField(name) != null) return currentClass.getField(name).getWasmType();
        }
        if (expr.isBinaryExpr()) {
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
        if (expr.isMethodCallExpr()) {
            String javaType = getExpressionJavaType(expr, localJavaTypes);
            if ("Float32x4".equals(javaType) || "Int32x4".equals(javaType) || "Vector128".equals(javaType)) return "v128";
            if ("double".equals(javaType)) return "f64";
            if ("float".equals(javaType)) return "f32";
            if ("String".equals(javaType)) return "String";
            return "i32";
        }
        if (expr.isFieldAccessExpr()) {
            FieldAccessExpr fae = expr.asFieldAccessExpr();
            if (fae.getNameAsString().equals("length")) return "i32";
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
}
