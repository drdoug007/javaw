package com.example.javaw;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.*;

/**
 * A visitor that translates Java AST elements into valid WebAssembly Text format (WAT).
 */
public class WasmGeneratorVisitor extends VoidVisitorAdapter<StringBuilder> {
    private final Map<String, StringConstant> stringPool;
    private final Map<String, GlobalVar> globalVars;
    private final Deque<LoopContext> loopStack = new ArrayDeque<>();
    private int loopCounter = 0;
    private int forEachCounter = 0;
    private int switchCounter = 0;
    private int switchExprCounter = 0;

    public WasmGeneratorVisitor(Map<String, StringConstant> stringPool, Map<String, GlobalVar> globalVars) {
        this.stringPool = stringPool;
        this.globalVars = globalVars;
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, StringBuilder collector) {
        collector.append("  ;; Class/Interface: ").append(n.getNameAsString()).append("\n");
        super.visit(n, collector);
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

        collector.append("  (func $").append(methodName)
                .append(" (export \"").append(methodName).append("\")");

        // Process parameters
        for (Parameter p : n.getParameters()) {
            String pType = TypeMapper.mapType(p.getType());
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

            // Declare local variables at function start (avoiding duplicates)
            Set<String> declaredLocals = new HashSet<>(localTypes.keySet());
            List<VariableDeclarator> varDecls = body.findAll(VariableDeclarator.class);
            for (VariableDeclarator vd : varDecls) {
                String vName = vd.getNameAsString();
                String vType = TypeMapper.mapType(vd.getType());
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

            // Helper locals for for-each loops and array operations
            List<ForEachStmt> forEachStmts = body.findAll(ForEachStmt.class);
            for (int i = 1; i <= forEachStmts.size(); i++) {
                collector.append("    (local $__arr_ptr_").append(i).append(" i32)\n");
                collector.append("    (local $__arr_len_").append(i).append(" i32)\n");
                collector.append("    (local $__arr_idx_").append(i).append(" i32)\n");
            }
            collector.append("    (local $__arr_alloc_ptr i32)\n");
            collector.append("    (local $__arr_alloc_len i32)\n");

            // Helper locals for switch statements and expressions
            List<SwitchStmt> switchStmts = body.findAll(SwitchStmt.class);
            for (int i = 1; i <= switchStmts.size(); i++) {
                collector.append("    (local $__switch_val_").append(i).append(" i32)\n");
            }
            List<SwitchExpr> switchExprs = body.findAll(SwitchExpr.class);
            for (int i = 1; i <= switchExprs.size(); i++) {
                collector.append("    (local $__switchexpr_val_").append(i).append(" i32)\n");
            }

            // Helper locals for string and stringbuilder operations
            collector.append("    (local $__str_arg_ptr i32)\n");
            collector.append("    (local $__str_tmp i32)\n");
            collector.append("    (local $__str_sub_start i32)\n");
            collector.append("    (local $__sb_tmp i32)\n");

            // Translate statements
            for (Statement stmt : body.getStatements()) {
                translateStatement(stmt, collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
            }
        }

        collector.append("  )\n");
    }

    private void translateStatement(Statement stmt, StringBuilder collector, Map<String, String> localTypes, Map<String, String> localElementTypes, Map<String, String> localJavaTypes, String returnWasmType) {
        translateStatement(stmt, collector, localTypes, localElementTypes, localJavaTypes, returnWasmType, Collections.emptyList());
    }

    private void translateStatement(Statement stmt, StringBuilder collector, Map<String, String> localTypes, Map<String, String> localElementTypes, Map<String, String> localJavaTypes, String returnWasmType, List<String> pendingLabels) {
        if (stmt.isReturnStmt()) {
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
        } else if (stmt.isLabeledStmt()) {
            LabeledStmt labeledStmt = stmt.asLabeledStmt();
            List<String> labels = new ArrayList<>(pendingLabels);
            labels.add(labeledStmt.getLabel().asString());
            Statement childStmt = labeledStmt.getStatement();
            if (childStmt.isForStmt() || childStmt.isWhileStmt() || childStmt.isForEachStmt() || childStmt.isSwitchStmt() || childStmt.isLabeledStmt()) {
                translateStatement(childStmt, collector, localTypes, localElementTypes, localJavaTypes, returnWasmType, labels);
            } else {
                int blockId = ++loopCounter;
                String breakLabel = "break_label_" + blockId;
                loopStack.push(new LoopContext(labels, breakLabel, null));
                collector.append("    (block $").append(breakLabel).append("\n");
                translateStatement(childStmt, collector, localTypes, localElementTypes, localJavaTypes, returnWasmType, Collections.emptyList());
                collector.append("    )\n");
                loopStack.pop();
            }
        } else if (stmt.isIfStmt()) {
            IfStmt ifStmt = stmt.asIfStmt();
            translateExpression(ifStmt.getCondition(), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    (if\n");
            collector.append("      (then\n");
            translateStatement(ifStmt.getThenStmt(), collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
            collector.append("      )\n");
            if (ifStmt.getElseStmt().isPresent()) {
                collector.append("      (else\n");
                translateStatement(ifStmt.getElseStmt().get(), collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
                collector.append("      )\n");
            }
            collector.append("    )\n");
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

            // 1. Initialization
            for (Expression initExpr : forStmt.getInitialization()) {
                translateExpression(initExpr, collector, localTypes, localElementTypes, localJavaTypes, null);
            }

            loopStack.push(new LoopContext(pendingLabels, breakLabel, continueLabel));

            collector.append("    (block $").append(breakLabel).append("\n");
            collector.append("      (loop $").append(loopLabel).append("\n");

            // 2. Condition check
            if (forStmt.getCompare().isPresent()) {
                translateExpression(forStmt.getCompare().get(), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("        i32.eqz\n");
                collector.append("        br_if $").append(breakLabel).append("\n");
            }

            // 3. Loop Body inside continue block
            collector.append("        (block $").append(continueLabel).append("\n");
            translateStatement(forStmt.getBody(), collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
            collector.append("        )\n");

            // 4. Update expressions
            for (Expression updateExpr : forStmt.getUpdate()) {
                translateExpression(updateExpr, collector, localTypes, localElementTypes, localJavaTypes, null);
            }

            // 5. Jump back to loop start
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

            String ptrVar = "__arr_ptr_" + forEachNum;
            String lenVar = "__arr_len_" + forEachNum;
            String idxVar = "__arr_idx_" + forEachNum;

            VariableDeclarator varDecl = forEach.getVariable().getVariable(0);
            String elemVar = varDecl.getNameAsString();
            String elemType = TypeMapper.mapType(varDecl.getType());
            localJavaTypes.put(elemVar, varDecl.getType().asString());
            int headerOffset = (elemType.equals("f64") || elemType.equals("i64")) ? 8 : 4;
            int elemSize = (elemType.equals("f64") || elemType.equals("i64")) ? 8 : 4;

            // 1. Evaluate array expression and store ptr and len
            translateExpression(forEach.getIterable(), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    local.set $").append(ptrVar).append("\n");
            collector.append("    local.get $").append(ptrVar).append("\n");
            collector.append("    i32.load\n");
            collector.append("    local.set $").append(lenVar).append("\n");
            collector.append("    i32.const 0\n");
            collector.append("    local.set $").append(idxVar).append("\n");

            loopStack.push(new LoopContext(pendingLabels, breakLabel, continueLabel));

            collector.append("    (block $").append(breakLabel).append("\n");
            collector.append("      (loop $").append(loopLabel).append("\n");

            // Condition check: idx >= len -> break
            collector.append("        local.get $").append(idxVar).append("\n");
            collector.append("        local.get $").append(lenVar).append("\n");
            collector.append("        i32.ge_s\n");
            collector.append("        br_if $").append(breakLabel).append("\n");

            // Load element into elemVar: elemVar = arr[idx]
            collector.append("        local.get $").append(ptrVar).append("\n");
            collector.append("        local.get $").append(idxVar).append("\n");
            collector.append("        i32.const ").append(elemSize).append("\n");
            collector.append("        i32.mul\n");
            collector.append("        i32.add\n");
            collector.append("        ").append(getLoadInstruction(elemType)).append(" offset=").append(headerOffset).append("\n");
            collector.append("        local.set $").append(elemVar).append("\n");

            // Loop body inside continue block
            collector.append("        (block $").append(continueLabel).append("\n");
            translateStatement(forEach.getBody(), collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
            collector.append("        )\n");

            // Increment index: idx++
            collector.append("        local.get $").append(idxVar).append("\n");
            collector.append("        i32.const 1\n");
            collector.append("        i32.add\n");
            collector.append("        local.set $").append(idxVar).append("\n");

            // Repeat loop
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

            // Evaluate selector expression and store in switch value variable
            translateExpression(switchStmt.getSelector(), collector, localTypes, localElementTypes, localJavaTypes, "i32");
            collector.append("    local.set $").append(valVar).append("\n");

            String enclosingContinueLabel = loopStack.isEmpty() ? null : loopStack.peek().continueLabel;
            loopStack.push(new LoopContext(pendingLabels, breakLabel, enclosingContinueLabel));

            collector.append("    (block $").append(breakLabel).append("\n");

            List<SwitchEntry> entries = switchStmt.getEntries();
            int numEntries = entries.size();

            int defaultIndex = -1;
            for (int i = 0; i < numEntries; i++) {
                if (entries.get(i).getLabels().isEmpty()) {
                    defaultIndex = i;
                    break;
                }
            }

            for (int i = numEntries - 1; i >= 0; i--) {
                collector.append("      (block $case_").append(switchId).append("_").append(i).append("\n");
            }

            collector.append("        ;; Dispatch logic for switch cases\n");
            for (int i = 0; i < numEntries; i++) {
                SwitchEntry entry = entries.get(i);
                for (Expression label : entry.getLabels()) {
                    collector.append("        local.get $").append(valVar).append("\n");
                    translateExpression(label, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    collector.append("        i32.eq\n");
                    collector.append("        br_if $case_").append(switchId).append("_").append(i).append("\n");
                }
            }

            if (defaultIndex >= 0) {
                collector.append("        br $case_").append(switchId).append("_").append(defaultIndex).append("\n");
            } else {
                collector.append("        br $").append(breakLabel).append("\n");
            }

            for (int i = 0; i < numEntries; i++) {
                collector.append("      )\n");
                SwitchEntry entry = entries.get(i);
                for (Statement s : entry.getStatements()) {
                    translateStatement(s, collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
                }
            }

            collector.append("    )\n");

            loopStack.pop();
        } else if (stmt.isYieldStmt()) {
            YieldStmt yieldStmt = stmt.asYieldStmt();
            translateExpression(yieldStmt.getExpression(), collector, localTypes, localElementTypes, localJavaTypes, returnWasmType);
            if (!loopStack.isEmpty()) {
                collector.append("    br $").append(loopStack.peek().breakLabel).append("\n");
            }
        } else if (stmt.isBreakStmt()) {
            BreakStmt breakStmt = stmt.asBreakStmt();
            if (breakStmt.getLabel().isPresent()) {
                String targetLabel = breakStmt.getLabel().get().asString();
                for (LoopContext ctx : loopStack) {
                    if (ctx.javaLabels.contains(targetLabel)) {
                        collector.append("    br $").append(ctx.breakLabel).append("\n");
                        return;
                    }
                }
            }
            if (!loopStack.isEmpty()) {
                collector.append("    br $").append(loopStack.peek().breakLabel).append("\n");
            }
        } else if (stmt.isContinueStmt()) {
            ContinueStmt contStmt = stmt.asContinueStmt();
            if (contStmt.getLabel().isPresent()) {
                String targetLabel = contStmt.getLabel().get().asString();
                for (LoopContext ctx : loopStack) {
                    if (ctx.javaLabels.contains(targetLabel)) {
                        if (ctx.continueLabel != null) {
                            collector.append("    br $").append(ctx.continueLabel).append("\n");
                        }
                        return;
                    }
                }
            }
            if (!loopStack.isEmpty() && loopStack.peek().continueLabel != null) {
                collector.append("    br $").append(loopStack.peek().continueLabel).append("\n");
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
        } else if (expr.isNameExpr()) {
            String var = expr.asNameExpr().getNameAsString();
            boolean isGlobal = globalVars != null && globalVars.containsKey(var) && !localTypes.containsKey(var);
            collector.append(isGlobal ? "    global.get $" : "    local.get $").append(var).append("\n");
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
            if (fae.getNameAsString().equals("length")) {
                translateExpression(fae.getScope(), collector, localTypes, localElementTypes, localJavaTypes, "i32");
                collector.append("    i32.load\n");
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

            String enclosingContinueLabel = loopStack.isEmpty() ? null : loopStack.peek().continueLabel;
            loopStack.push(new LoopContext(breakLabel, enclosingContinueLabel));

            collector.append("    (block $").append(breakLabel).append(" (result ").append(exprType).append(")\n");

            List<SwitchEntry> entries = switchExpr.getEntries();
            int numEntries = entries.size();

            int defaultIndex = -1;
            for (int i = 0; i < numEntries; i++) {
                if (entries.get(i).getLabels().isEmpty()) {
                    defaultIndex = i;
                    break;
                }
            }

            for (int i = numEntries - 1; i >= 0; i--) {
                collector.append("      (block $case_").append(switchId).append("_").append(i).append("\n");
            }

            collector.append("        ;; Dispatch logic for switch expression\n");
            for (int i = 0; i < numEntries; i++) {
                SwitchEntry entry = entries.get(i);
                for (Expression label : entry.getLabels()) {
                    collector.append("        local.get $").append(valVar).append("\n");
                    translateExpression(label, collector, localTypes, localElementTypes, localJavaTypes, "i32");
                    collector.append("        i32.eq\n");
                    collector.append("        br_if $case_").append(switchId).append("_").append(i).append("\n");
                }
            }

            if (defaultIndex >= 0) {
                collector.append("        br $case_").append(switchId).append("_").append(defaultIndex).append("\n");
            } else {
                collector.append("        ").append(exprType).append(".const 0\n");
                collector.append("        br $").append(breakLabel).append("\n");
            }

            for (int i = 0; i < numEntries; i++) {
                collector.append("      )\n");
                SwitchEntry entry = entries.get(i);
                for (Statement s : entry.getStatements()) {
                    translateStatement(s, collector, localTypes, localElementTypes, localJavaTypes, exprType);
                }
            }

            collector.append("    )\n");

            loopStack.pop();
        } else if (expr.isAssignExpr()) {
            AssignExpr assign = expr.asAssignExpr();
            AssignExpr.Operator op = assign.getOperator();
            if (assign.getTarget().isNameExpr()) {
                String target = assign.getTarget().asNameExpr().getNameAsString();
                boolean isGlobal = globalVars != null && globalVars.containsKey(target) && !localTypes.containsKey(target);
                String targetType = isGlobal ? globalVars.get(target).type : localTypes.getOrDefault(target, "i32");

                if (op == AssignExpr.Operator.ASSIGN) {
                    translateExpression(assign.getValue(), collector, localTypes, localElementTypes, localJavaTypes, targetType);
                    collector.append(isGlobal ? "    global.set $" : "    local.set $").append(target).append("\n");
                } else if (op == AssignExpr.Operator.PLUS && (isStringType(assign.getTarget(), localJavaTypes) || isStringType(assign.getValue(), localJavaTypes))) {
                    collector.append(isGlobal ? "    global.get $" : "    local.get $").append(target).append("\n");
                    translateStringToStack(assign.getValue(), collector, localTypes, localElementTypes, localJavaTypes);
                    collector.append("    call $__str_concat\n");
                    collector.append(isGlobal ? "    global.set $" : "    local.set $").append(target).append("\n");
                } else {
                    // Compound assignment (e.g. +=, -=, *=, /=)
                    collector.append(isGlobal ? "    global.get $" : "    local.get $").append(target).append("\n");
                    translateExpression(assign.getValue(), collector, localTypes, localElementTypes, localJavaTypes, targetType);
                    BinaryExpr.Operator binOp = mapAssignOpToBinaryOp(op);
                    if (binOp != null) {
                        collector.append("    ").append(getWasmBinaryOp(binOp, targetType)).append("\n");
                    }
                    collector.append(isGlobal ? "    global.set $" : "    local.set $").append(target).append("\n");
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
                    // Compound assignment on array element: arr[i] += val
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
                } else {
                    translateExpression(scope, collector, localTypes, localElementTypes, localJavaTypes, null);
                    for (Expression arg : call.getArguments()) {
                        translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, null);
                    }
                    collector.append("    call $").append(mName).append("\n");
                }
            } else {
                if (DomHelper.isDomStaticMethod(mName) || DomHelper.isDomInstanceMethod(mName)) {
                    for (Expression arg : call.getArguments()) {
                        translateDomStringArg(arg, collector, localTypes, localElementTypes, localJavaTypes);
                    }
                    collector.append("    call $dom_").append(mName).append("\n");
                } else {
                    for (Expression arg : call.getArguments()) {
                        translateExpression(arg, collector, localTypes, localElementTypes, localJavaTypes, null);
                    }
                    collector.append("    call $").append(mName).append("\n");
                }
            }
        }
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
        } else {
            String type = inferType(expr, localTypes, localElementTypes, localJavaTypes, "i32");
            if (expr.isCharLiteralExpr() || (expr.isNameExpr() && "char".equals(localJavaTypes.get(expr.asNameExpr().getNameAsString())))) {
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
    }

    private String getArrayElementType(ArrayAccessExpr aae, Map<String, String> localElementTypes, String fallback) {
        if (aae.getName().isNameExpr()) {
            String arrName = aae.getName().asNameExpr().getNameAsString();
            String elemType = localElementTypes.get(arrName);
            if (elemType != null) {
                return elemType;
            }
        }
        return (fallback != null && !fallback.equals("i32")) ? fallback : "i32";
    }

    private static String getLoadInstruction(String type) {
        if ("f64".equals(type)) return "f64.load";
        if ("f32".equals(type)) return "f32.load";
        if ("i64".equals(type)) return "i64.load";
        return "i32.load";
    }

    private static String getStoreInstruction(String type) {
        if ("f64".equals(type)) return "f64.store";
        if ("f32".equals(type)) return "f32.store";
        if ("i64".equals(type)) return "i64.store";
        return "i32.store";
    }

    private String inferType(Expression expr, Map<String, String> localTypes, Map<String, String> localElementTypes, Map<String, String> localJavaTypes, String fallback) {
        if (expr.isDoubleLiteralExpr()) {
            return "f64";
        }
        if (expr.isLongLiteralExpr()) {
            return "i64";
        }
        if (expr.isIntegerLiteralExpr() || expr.isBooleanLiteralExpr() || expr.isCharLiteralExpr() || expr.isStringLiteralExpr()) {
            return "i32";
        }
        if (expr.isNameExpr()) {
            String name = expr.asNameExpr().getNameAsString();
            String t = localTypes.get(name);
            if (t != null) {
                return t;
            }
            if (globalVars != null && globalVars.containsKey(name)) {
                return globalVars.get(name).type;
            }
        }
        if (expr.isArrayAccessExpr()) {
            return getArrayElementType(expr.asArrayAccessExpr(), localElementTypes, fallback);
        }
        if (expr.isFieldAccessExpr() && expr.asFieldAccessExpr().getNameAsString().equals("length")) {
            return "i32";
        }
        if (expr.isArrayCreationExpr()) {
            return "i32";
        }
        if (expr.isObjectCreationExpr()) {
            return "i32";
        }
        if (expr.isSwitchExpr()) {
            SwitchExpr se = expr.asSwitchExpr();
            for (SwitchEntry entry : se.getEntries()) {
                for (Statement s : entry.getStatements()) {
                    if (s.isYieldStmt()) {
                        return inferType(s.asYieldStmt().getExpression(), localTypes, localElementTypes, localJavaTypes, fallback);
                    }
                    if (s.isExpressionStmt()) {
                        return inferType(s.asExpressionStmt().getExpression(), localTypes, localElementTypes, localJavaTypes, fallback);
                    }
                }
            }
        }
        if (expr.isBinaryExpr()) {
            BinaryExpr b = expr.asBinaryExpr();
            if (isComparisonOp(b.getOperator())) {
                return "i32";
            }
            if (b.getOperator() == BinaryExpr.Operator.PLUS && (isStringType(b.getLeft(), localJavaTypes) || isStringType(b.getRight(), localJavaTypes))) {
                return "i32";
            }
            String lt = inferType(b.getLeft(), localTypes, localElementTypes, localJavaTypes, null);
            if (lt != null) return lt;
            String rt = inferType(b.getRight(), localTypes, localElementTypes, localJavaTypes, null);
            if (rt != null) return rt;
        }
        if (expr.isUnaryExpr()) {
            return inferType(expr.asUnaryExpr().getExpression(), localTypes, localElementTypes, localJavaTypes, fallback);
        }
        if (expr.isEnclosedExpr()) {
            return inferType(expr.asEnclosedExpr().getInner(), localTypes, localElementTypes, localJavaTypes, fallback);
        }
        return (fallback != null) ? fallback : "i32";
    }

    private static boolean isComparisonOp(BinaryExpr.Operator op) {
        return op == BinaryExpr.Operator.EQUALS ||
                op == BinaryExpr.Operator.NOT_EQUALS ||
                op == BinaryExpr.Operator.LESS ||
                op == BinaryExpr.Operator.LESS_EQUALS ||
                op == BinaryExpr.Operator.GREATER ||
                op == BinaryExpr.Operator.GREATER_EQUALS ||
                op == BinaryExpr.Operator.AND ||
                op == BinaryExpr.Operator.OR;
    }

    private static BinaryExpr.Operator mapAssignOpToBinaryOp(AssignExpr.Operator op) {
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

    private static String getWasmBinaryOp(BinaryExpr.Operator op, String type) {
        String prefix = (type != null && !type.isEmpty()) ? type : "i32";
        return switch (op) {
            case PLUS -> prefix + ".add";
            case MINUS -> prefix + ".sub";
            case MULTIPLY -> prefix + ".mul";
            case DIVIDE -> prefix.startsWith("i") ? prefix + ".div_s" : prefix + ".div";
            case REMAINDER -> prefix.startsWith("i") ? prefix + ".rem_s" : "i32.rem_s";
            case EQUALS -> prefix + ".eq";
            case NOT_EQUALS -> prefix + ".ne";
            case LESS -> prefix.startsWith("i") ? prefix + ".lt_s" : prefix + ".lt";
            case LESS_EQUALS -> prefix.startsWith("i") ? prefix + ".le_s" : prefix + ".le";
            case GREATER -> prefix.startsWith("i") ? prefix + ".gt_s" : prefix + ".gt";
            case GREATER_EQUALS -> prefix.startsWith("i") ? prefix + ".ge_s" : prefix + ".ge";
            case AND -> "i32.and";
            case OR -> "i32.or";
            case BINARY_AND -> prefix + ".and";
            case BINARY_OR -> prefix + ".or";
            case XOR -> prefix + ".xor";
            case LEFT_SHIFT -> prefix + ".shl";
            case SIGNED_RIGHT_SHIFT -> prefix + ".shr_s";
            case UNSIGNED_RIGHT_SHIFT -> prefix + ".shr_u";
        };
    }
}
