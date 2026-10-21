package com.example.javaw;

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
    private final Deque<LoopContext> loopStack = new ArrayDeque<>();
    private ClassModel currentClass;
    private int loopCounter = 0;
    private int forEachCounter = 0;
    private int switchCounter = 0;
    private int switchExprCounter = 0;

    public WasmGeneratorVisitor(Map<String, StringConstant> stringPool, Map<String, GlobalVar> globalVars, ClassRegistry classRegistry) {
        this.stringPool = stringPool;
        this.globalVars = globalVars;
        this.classRegistry = classRegistry;
    }

    public WasmGeneratorVisitor(Map<String, StringConstant> stringPool, Map<String, GlobalVar> globalVars) {
        this(stringPool, globalVars, null);
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, StringBuilder collector) {
        String className = n.getNameAsString();
        currentClass = classRegistry != null ? classRegistry.getClass(className) : null;

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
    public void visit(MethodDeclaration n, StringBuilder collector) {
        String methodName = n.getNameAsString();
        String returnWasmType = TypeMapper.mapType(n.getType());

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

        collector.append("  (func ").append(funcName)
                .append(" (export \"").append(exportName).append("\")");

        if (isOopInstanceMethod) {
            collector.append(" (param $this i32)");
            localTypes.put("this", "i32");
            localJavaTypes.put("this", currentClass.getName());
        }

        // Process parameters
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

        // Return type
        if (returnWasmType != null) {
            collector.append(" (result ").append(returnWasmType).append(")");
        }
        collector.append("\n");

        // Body
        if (n.getBody().isPresent()) {
            BlockStmt body = n.getBody().get();
            emitLocalDeclarationsAndHelpers(body, collector, localTypes, localElementTypes, localJavaTypes);

            for (Statement stmt : body.getStatements()) {
                translateStatement(stmt, collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
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
    }

    private void translateStatement(Statement stmt, StringBuilder collector, Map<String, String> localTypes, Map<String, String> localElementTypes, Map<String, String> localJavaTypes, String returnWasmType) {
        translateStatement(stmt, collector, localTypes, localElementTypes, localJavaTypes, returnWasmType, Collections.emptyList());
    }

    private void translateStatement(Statement stmt, StringBuilder collector, Map<String, String> localTypes, Map<String, String> localElementTypes, Map<String, String> localJavaTypes, String returnWasmType, List<String> pendingLabels) {
        if (stmt.isExplicitConstructorInvocationStmt()) {
            ExplicitConstructorInvocationStmt ecis = stmt.asExplicitConstructorInvocationStmt();
            collector.append("    local.get $this\n");
            for (Expression arg : ecis.getArguments()) {
                translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, "i32");
            }
            if (ecis.isThis()) {
                collector.append("    call $").append(currentClass.getName()).append("__init\n");
            } else if (currentClass != null && currentClass.getSuperClassName() != null) {
                collector.append("    call $").append(currentClass.getSuperClassName()).append("__init\n");
            }
        } else if (stmt.isReturnStmt()) {
            ReturnStmt ret = stmt.asReturnStmt();
            if (ret.getExpression().isPresent()) {
                translateExpression(ret.getExpression().get(), collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
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
            collector.append("    i32.const ").append(expr.asIntegerLiteralExpr().getValue()).append("\n");
        } else if (expr.isLongLiteralExpr()) {
            String val = expr.asLongLiteralExpr().getValue().replaceAll("[lL]", "");
            collector.append("    i64.const ").append(val).append("\n");
        } else if (expr.isDoubleLiteralExpr()) {
            collector.append("    f64.const ").append(expr.asDoubleLiteralExpr().getValue()).append("\n");
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
            if (localTypes.containsKey(var)) {
                collector.append("    local.get $").append(var).append("\n");
            } else if (currentClass != null && currentClass.getField(var) != null && !currentClass.getField(var).isStatic()) {
                FieldModel fm = currentClass.getField(var);
                collector.append("    local.get $this\n");
                collector.append("    ").append(getLoadInstruction(fm.getWasmType())).append(" offset=").append(fm.getOffset()).append("\n");
            } else if (globalVars != null && globalVars.containsKey(var)) {
                collector.append("    global.get $").append(var).append("\n");
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
                collector.append("    call $").append(typeName).append("__init\n");
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
            } else {
                String targetType = getExpressionJavaType(fae.getScope(), localJavaTypes);
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

                if (isInstanceField) {
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
                if (vd.getInitializer().isPresent()) {
                    String vType = TypeMapper.mapType(vd.getType());
                    String elemType = vd.getType().isArrayType() ? TypeMapper.mapType(vd.getType().asArrayType().getComponentType()) : null;
                    translateExpression(vd.getInitializer().get(), collector, localTypes, localElementTypes, localJavaTypes, elemType != null ? elemType : vType);
                    collector.append("    local.set $").append(vd.getNameAsString()).append("\n");
                }
            }
        } else if (expr.isMethodCallExpr()) {
            MethodCallExpr call = expr.asMethodCallExpr();
            String mName = call.getNameAsString();
            if (call.getScope().isPresent()) {
                Expression scope = call.getScope().get();
                if (scope.isNameExpr() && scope.asNameExpr().getNameAsString().equals("DOM")) {
                    for (Expression arg : call.getArguments()) {
                        translateDomStringArg(arg, collector, localTypes, localElementTypes, localJavaTypes);
                    }
                    collector.append("    call $dom_").append(mName).append("\n");
                } else if (DomHelper.isDomInstanceMethod(mName)) {
                    translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    if (mName.equals("appendChild")) {
                        translateExpression(call.getArgument(0), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    } else {
                        for (Expression arg : call.getArguments()) {
                            translateDomStringArg(arg, collector, localTypes, localElementTypes, localJavaTypes);
                        }
                    }
                    collector.append("    call $dom_").append(mName).append("\n");
                } else if (mName.equals("length")) {
                    if (scope.isNameExpr() && localJavaTypes.containsKey(scope.asNameExpr().getNameAsString()) && (localJavaTypes.get(scope.asNameExpr().getNameAsString()).equals("StringBuilder") || localJavaTypes.get(scope.asNameExpr().getNameAsString()).endsWith(".StringBuilder"))) {
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
                    ClassModel targetCm = (targetType != null && classRegistry != null) ? classRegistry.getClass(targetType) : null;
                    MethodModel targetMm = targetCm != null ? targetCm.getMethodByName(mName) : null;

                    if (targetMm != null && targetMm.isVirtual()) {
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
                        if (!targetMm.isStatic()) {
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
                if (DomHelper.isDomStaticMethod(mName) || DomHelper.isDomInstanceMethod(mName)) {
                    for (Expression arg : call.getArguments()) {
                        translateDomStringArg(arg, collector, localTypes, localElementTypes, localJavaTypes);
                    }
                    collector.append("    call $dom_").append(mName).append("\n");
                } else if (currentClass != null && currentClass.getMethodByName(mName) != null) {
                    MethodModel mm = currentClass.getMethodByName(mName);
                    if (mm.isVirtual()) {
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
                        if (!mm.isStatic()) {
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
            return currentClass != null ? currentClass.getName() : null;
        }
        if (expr.isObjectCreationExpr()) {
            return expr.asObjectCreationExpr().getTypeAsString();
        }
        if (expr.isMethodCallExpr()) {
            MethodCallExpr call = expr.asMethodCallExpr();
            String mName = call.getNameAsString();
            if (call.getScope().isPresent()) {
                String scopeType = getExpressionJavaType(call.getScope().get(), localJavaTypes);
                if (scopeType != null && classRegistry != null && classRegistry.hasClass(scopeType)) {
                    MethodModel mm = classRegistry.getClass(scopeType).getMethodByName(mName);
                    if (mm != null) return mm.getReturnTypeName();
                }
            } else if (currentClass != null) {
                MethodModel mm = currentClass.getMethodByName(mName);
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
            if (scopeType != null && classRegistry != null && classRegistry.hasClass(scopeType)) {
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
                    mName.equals("getElementById") || mName.equals("createElement") || mName.equals("getBody")) {
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
            if (mName.equals("toString") || mName.equals("substring") || mName.equals("concat")) return true;
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
            case "f64" -> "f64.load";
            case "f32" -> "f32.load";
            case "i64" -> "i64.load";
            default -> "i32.load";
        };
    }

    private String getStoreInstruction(String elemType) {
        if (elemType == null) return "i32.store";
        return switch (elemType) {
            case "f64" -> "f64.store";
            case "f32" -> "f32.store";
            case "i64" -> "i64.store";
            default -> "i32.store";
        };
    }

    private String getWasmZeroConst(String type) {
        if (type == null) return "i32.const 0";
        return switch (type) {
            case "f64" -> "f64.const 0.0";
            case "f32" -> "f32.const 0.0";
            case "i64" -> "i64.const 0";
            default -> "i32.const 0";
        };
    }

    private String inferType(Expression expr, Map<String, String> localTypes, Map<String, String> localElementTypes, Map<String, String> localJavaTypes, String fallback) {
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
        if (expr.isFieldAccessExpr()) {
            FieldAccessExpr fae = expr.asFieldAccessExpr();
            if (fae.getNameAsString().equals("length")) return "i32";
            String targetType = getExpressionJavaType(fae.getScope(), localJavaTypes);
            if (targetType != null && classRegistry != null && classRegistry.hasClass(targetType)) {
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
}
