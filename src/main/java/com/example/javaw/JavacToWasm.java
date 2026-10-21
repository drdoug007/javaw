package com.example.javaw;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JavacToWasm {

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: javaw <JavaFile.java>");
            System.exit(1);
        }

        String javaFilePath = args[0];
        Path path = Paths.get(javaFilePath).toAbsolutePath().normalize();
        String stem = path.getFileName().toString().replace(".java", "");
        Path watFilePath = path.getParent().resolve(stem + ".wat");
        Path wasmFilePath = path.getParent().resolve(stem + ".wasm");

        try {
            System.out.println("Compiling: " + path);
            System.out.println("Target WAT: " + watFilePath);

            CompilationUnit cu = StaticJavaParser.parse(path);
            String watContent = generateWat(cu);

            try (FileWriter writer = new FileWriter(watFilePath.toFile())) {
                writer.write(watContent);
            }

            System.out.println("WAT generation successful.");

            // Attempt to compile to binary WASM if wat2wasm is available
            try {
                Process process = new ProcessBuilder("wat2wasm", watFilePath.toString(), "-o", wasmFilePath.toString())
                        .redirectErrorStream(true)
                        .start();
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    System.out.println("Binary WASM generated: " + wasmFilePath);
                } else {
                    System.err.println("wat2wasm compilation exited with code: " + exitCode);
                }
            } catch (IOException e) {
                // wat2wasm not installed or not in PATH
                System.out.println("wat2wasm not available in PATH; skipping binary .wasm compilation.");
            }

            System.out.println("Compilation successful.");
        } catch (Exception e) {
            System.err.println("Error during compilation: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static class GlobalVar {
        public final String name;
        public final String type;
        public final String initVal;

        public GlobalVar(String name, String type, String initVal) {
            this.name = name;
            this.type = type;
            this.initVal = initVal;
        }
    }

    public static Map<String, GlobalVar> collectGlobalVars(CompilationUnit cu) {
        Map<String, GlobalVar> globals = new LinkedHashMap<>();
        List<FieldDeclaration> fields = cu.findAll(FieldDeclaration.class);
        for (FieldDeclaration fd : fields) {
            for (VariableDeclarator vd : fd.getVariables()) {
                String vName = vd.getNameAsString();
                String vType = mapType(vd.getType());
                if (vType == null) vType = "i32";
                String initVal = "0";
                if (vd.getInitializer().isPresent()) {
                    Expression initExpr = vd.getInitializer().get();
                    if (initExpr.isIntegerLiteralExpr()) {
                        initVal = initExpr.asIntegerLiteralExpr().getValue();
                    } else if (initExpr.isDoubleLiteralExpr()) {
                        initVal = initExpr.asDoubleLiteralExpr().getValue();
                    } else if (initExpr.isBooleanLiteralExpr()) {
                        initVal = initExpr.asBooleanLiteralExpr().getValue() ? "1" : "0";
                    }
                }
                globals.put(vName, new GlobalVar(vName, vType, initVal));
            }
        }
        return globals;
    }

    public static class StringConstant {
        public final int offset;
        public final int length;
        public final byte[] bytes;

        public StringConstant(int offset, int length, byte[] bytes) {
            this.offset = offset;
            this.length = length;
            this.bytes = bytes;
        }
    }

    public static Map<String, StringConstant> collectStringLiterals(CompilationUnit cu) {
        Map<String, StringConstant> pool = new LinkedHashMap<>();
        int currentOffset = 4096; // 0x1000
        List<StringLiteralExpr> literals = cu.findAll(StringLiteralExpr.class);
        for (StringLiteralExpr lit : literals) {
            String value = lit.getValue();
            if (!pool.containsKey(value)) {
                byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
                pool.put(value, new StringConstant(currentOffset, bytes.length, bytes));
                currentOffset += bytes.length + 1; // null-terminated spacing
            }
        }
        return pool;
    }

    public static String escapeForWat(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            int ub = b & 0xFF;
            if (ub >= 32 && ub <= 126 && ub != '"' && ub != '\\') {
                sb.append((char) ub);
            } else {
                sb.append(String.format("\\%02x", ub));
            }
        }
        sb.append("\\00");
        return sb.toString();
    }

    public static Set<String> collectDomImports(CompilationUnit cu) {
        Set<String> imports = new LinkedHashSet<>();
        List<MethodCallExpr> calls = cu.findAll(MethodCallExpr.class);
        for (MethodCallExpr call : calls) {
            String mName = call.getNameAsString();
            if (isDomStaticMethod(mName) || isDomInstanceMethod(mName)) {
                imports.add(mName);
            }
        }
        return imports;
    }

    public static boolean isDomStaticMethod(String mName) {
        return mName.equals("getElementById") ||
                mName.equals("createElement") ||
                mName.equals("getBody");
    }

    public static boolean isDomInstanceMethod(String mName) {
        return mName.equals("setInnerText") ||
                mName.equals("setAttribute") ||
                mName.equals("appendChild") ||
                mName.equals("addEventListener");
    }

    public static String generateWat(CompilationUnit cu) {
        StringBuilder watOutput = new StringBuilder();
        watOutput.append("(module\n");

        Set<String> domImports = collectDomImports(cu);
        if (domImports.contains("getElementById")) {
            watOutput.append("  (import \"dom\" \"getElementById\" (func $dom_getElementById (param i32 i32) (result i32)))\n");
        }
        if (domImports.contains("createElement")) {
            watOutput.append("  (import \"dom\" \"createElement\" (func $dom_createElement (param i32 i32) (result i32)))\n");
        }
        if (domImports.contains("getBody")) {
            watOutput.append("  (import \"dom\" \"getBody\" (func $dom_getBody (result i32)))\n");
        }
        if (domImports.contains("setInnerText")) {
            watOutput.append("  (import \"dom\" \"setInnerText\" (func $dom_setInnerText (param i32 i32 i32)))\n");
        }
        if (domImports.contains("setAttribute")) {
            watOutput.append("  (import \"dom\" \"setAttribute\" (func $dom_setAttribute (param i32 i32 i32 i32 i32)))\n");
        }
        if (domImports.contains("appendChild")) {
            watOutput.append("  (import \"dom\" \"appendChild\" (func $dom_appendChild (param i32 i32)))\n");
        }
        if (domImports.contains("addEventListener")) {
            watOutput.append("  (import \"dom\" \"addEventListener\" (func $dom_addEventListener (param i32 i32 i32 i32 i32)))\n");
        }

        watOutput.append("  (memory (export \"memory\") 2)\n");

        Map<String, GlobalVar> globalVars = collectGlobalVars(cu);
        for (GlobalVar gv : globalVars.values()) {
            watOutput.append("  (global $").append(gv.name).append(" (mut ").append(gv.type).append(") (")
                    .append(gv.type).append(".const ").append(gv.initVal).append("))\n");
        }

        Map<String, StringConstant> stringPool = collectStringLiterals(cu);
        int heapStart = 65536; // Default to 64KB (1 WebAssembly page)
        for (Map.Entry<String, StringConstant> entry : stringPool.entrySet()) {
            StringConstant sc = entry.getValue();
            watOutput.append("  (data (i32.const ").append(sc.offset).append(") \"")
                    .append(escapeForWat(sc.bytes)).append("\")\n");
            int end = sc.offset + sc.length + 16;
            if (end > heapStart) {
                heapStart = ((end + 65535) / 65536) * 65536;
            }
        }
        watOutput.append("  (global $__heap_ptr (mut i32) (i32.const ").append(heapStart).append("))\n");

        cu.accept(new WasmGeneratorVisitor(stringPool, globalVars), watOutput);
        watOutput.append(")\n");
        return watOutput.toString();
    }

    /**
     * Map Java types to WebAssembly value types.
     */
    public static String mapType(String typeStr) {
        if (typeStr == null) {
            return "i32";
        }
        if (typeStr.endsWith("[]")) {
            return "i32";
        }
        return switch (typeStr) {
            case "void" -> null;
            case "int", "boolean", "byte", "short", "char" -> "i32";
            case "long" -> "i64";
            case "float" -> "f32";
            case "double" -> "f64";
            default -> "i32";
        };
    }

    public static String mapType(Type type) {
        return mapType(type.asString());
    }

    private static class LoopContext {
        final String breakLabel;
        final String continueLabel;

        LoopContext(String breakLabel, String continueLabel) {
            this.breakLabel = breakLabel;
            this.continueLabel = continueLabel;
        }
    }

    /**
     * A visitor that translates Java AST elements into valid WebAssembly Text format (WAT).
     */
    private static class WasmGeneratorVisitor extends VoidVisitorAdapter<StringBuilder> {
        private final Map<String, StringConstant> stringPool;
        private final Map<String, GlobalVar> globalVars;
        private final Deque<LoopContext> loopStack = new ArrayDeque<>();
        private int loopCounter = 0;
        private int forEachCounter = 0;

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
            String returnWasmType = mapType(n.getType());

            Map<String, String> localTypes = new HashMap<>();
            Map<String, String> localElementTypes = new HashMap<>();
            forEachCounter = 0;

            collector.append("  (func $").append(methodName)
                    .append(" (export \"").append(methodName).append("\")");

            // Process parameters
            for (Parameter p : n.getParameters()) {
                String pType = mapType(p.getType());
                localTypes.put(p.getNameAsString(), pType);
                if (p.getType().isArrayType()) {
                    localElementTypes.put(p.getNameAsString(), mapType(p.getType().asArrayType().getComponentType()));
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
                    String vType = mapType(vd.getType());
                    localTypes.put(vName, vType);
                    if (vd.getType().isArrayType()) {
                        localElementTypes.put(vName, mapType(vd.getType().asArrayType().getComponentType()));
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

                // Translate statements
                for (Statement stmt : body.getStatements()) {
                    translateStatement(stmt, collector, localTypes, localElementTypes, returnWasmType);
                }
            }

            collector.append("  )\n");
        }

        private void translateStatement(Statement stmt, StringBuilder collector, Map<String, String> localTypes, Map<String, String> localElementTypes, String returnWasmType) {
            if (stmt.isReturnStmt()) {
                ReturnStmt ret = stmt.asReturnStmt();
                if (ret.getExpression().isPresent()) {
                    translateExpression(ret.getExpression().get(), collector, localTypes, localElementTypes, returnWasmType);
                }
            } else if (stmt.isExpressionStmt()) {
                ExpressionStmt exprStmt = stmt.asExpressionStmt();
                translateExpression(exprStmt.getExpression(), collector, localTypes, localElementTypes, returnWasmType);
            } else if (stmt.isIfStmt()) {
                IfStmt ifStmt = stmt.asIfStmt();
                translateExpression(ifStmt.getCondition(), collector, localTypes, localElementTypes, "i32");
                collector.append("    (if\n");
                collector.append("      (then\n");
                translateStatement(ifStmt.getThenStmt(), collector, localTypes, localElementTypes, returnWasmType);
                collector.append("      )\n");
                if (ifStmt.getElseStmt().isPresent()) {
                    collector.append("      (else\n");
                    translateStatement(ifStmt.getElseStmt().get(), collector, localTypes, localElementTypes, returnWasmType);
                    collector.append("      )\n");
                }
                collector.append("    )\n");
            } else if (stmt.isWhileStmt()) {
                WhileStmt whileStmt = stmt.asWhileStmt();
                int loopId = ++loopCounter;
                String breakLabel = "break_while_" + loopId;
                String loopLabel = "loop_while_" + loopId;

                loopStack.push(new LoopContext(breakLabel, loopLabel));

                collector.append("    (block $").append(breakLabel).append("\n");
                collector.append("      (loop $").append(loopLabel).append("\n");
                translateExpression(whileStmt.getCondition(), collector, localTypes, localElementTypes, "i32");
                collector.append("        i32.eqz\n");
                collector.append("        br_if $").append(breakLabel).append("\n");
                translateStatement(whileStmt.getBody(), collector, localTypes, localElementTypes, returnWasmType);
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

                // 1. Initialization expressions
                for (Expression initExpr : forStmt.getInitialization()) {
                    translateExpression(initExpr, collector, localTypes, localElementTypes, null);
                }

                loopStack.push(new LoopContext(breakLabel, continueLabel));

                collector.append("    (block $").append(breakLabel).append("\n");
                collector.append("      (loop $").append(loopLabel).append("\n");

                // 2. Condition check
                if (forStmt.getCompare().isPresent()) {
                    translateExpression(forStmt.getCompare().get(), collector, localTypes, localElementTypes, "i32");
                    collector.append("        i32.eqz\n");
                    collector.append("        br_if $").append(breakLabel).append("\n");
                }

                // 3. Body with continue target
                collector.append("        (block $").append(continueLabel).append("\n");
                translateStatement(forStmt.getBody(), collector, localTypes, localElementTypes, returnWasmType);
                collector.append("        )\n");

                // 4. Update expressions
                for (Expression updateExpr : forStmt.getUpdate()) {
                    translateExpression(updateExpr, collector, localTypes, localElementTypes, null);
                }

                collector.append("        br $").append(loopLabel).append("\n");
                collector.append("      )\n");
                collector.append("    )\n");

                loopStack.pop();
            } else if (stmt.isForEachStmt()) {
                ForEachStmt forEach = stmt.asForEachStmt();
                int forEachId = ++forEachCounter;
                int loopId = ++loopCounter;
                String breakLabel = "break_foreach_" + loopId;
                String loopLabel = "loop_foreach_" + loopId;
                String continueLabel = "continue_foreach_" + loopId;

                String ptrVar = "__arr_ptr_" + forEachId;
                String lenVar = "__arr_len_" + forEachId;
                String idxVar = "__arr_idx_" + forEachId;

                VariableDeclarator varDecl = forEach.getVariable().getVariable(0);
                String elemVar = varDecl.getNameAsString();
                String elemType = mapType(varDecl.getType());
                int headerOffset = (elemType.equals("f64") || elemType.equals("i64")) ? 8 : 4;
                int elemSize = (elemType.equals("f64") || elemType.equals("i64")) ? 8 : 4;

                // 1. Evaluate iterable expression and store into ptrVar
                translateExpression(forEach.getIterable(), collector, localTypes, localElementTypes, "i32");
                collector.append("    local.set $").append(ptrVar).append("\n");

                // 2. Load array length from ptrVar
                collector.append("    local.get $").append(ptrVar).append("\n");
                collector.append("    i32.load\n");
                collector.append("    local.set $").append(lenVar).append("\n");

                // 3. Initialize index = 0
                collector.append("    i32.const 0\n");
                collector.append("    local.set $").append(idxVar).append("\n");

                loopStack.push(new LoopContext(breakLabel, continueLabel));

                collector.append("    (block $").append(breakLabel).append("\n");
                collector.append("      (loop $").append(loopLabel).append("\n");

                // 4. Condition check: idx >= len -> break
                collector.append("        local.get $").append(idxVar).append("\n");
                collector.append("        local.get $").append(lenVar).append("\n");
                collector.append("        i32.ge_s\n");
                collector.append("        br_if $").append(breakLabel).append("\n");

                // 5. Load element at idx into elemVar
                collector.append("        local.get $").append(ptrVar).append("\n");
                collector.append("        local.get $").append(idxVar).append("\n");
                collector.append("        i32.const ").append(elemSize).append("\n");
                collector.append("        i32.mul\n");
                collector.append("        i32.add\n");
                collector.append("        ").append(getLoadInstruction(elemType)).append(" offset=").append(headerOffset).append("\n");
                collector.append("        local.set $").append(elemVar).append("\n");

                // 6. Body with continue target
                collector.append("        (block $").append(continueLabel).append("\n");
                translateStatement(forEach.getBody(), collector, localTypes, localElementTypes, returnWasmType);
                collector.append("        )\n");

                // 7. Update index: idx++
                collector.append("        local.get $").append(idxVar).append("\n");
                collector.append("        i32.const 1\n");
                collector.append("        i32.add\n");
                collector.append("        local.set $").append(idxVar).append("\n");
                collector.append("        br $").append(loopLabel).append("\n");
                collector.append("      )\n");
                collector.append("    )\n");

                loopStack.pop();
            } else if (stmt.isBreakStmt()) {
                if (!loopStack.isEmpty()) {
                    collector.append("    br $").append(loopStack.peek().breakLabel).append("\n");
                }
            } else if (stmt.isContinueStmt()) {
                if (!loopStack.isEmpty()) {
                    collector.append("    br $").append(loopStack.peek().continueLabel).append("\n");
                }
            } else if (stmt.isBlockStmt()) {
                for (Statement s : stmt.asBlockStmt().getStatements()) {
                    translateStatement(s, collector, localTypes, localElementTypes, returnWasmType);
                }
            }
        }

        private void translateExpression(Expression expr, StringBuilder collector, Map<String, String> localTypes, Map<String, String> localElementTypes, String expectedType) {
            if (expr.isBinaryExpr()) {
                BinaryExpr bin = expr.asBinaryExpr();
                String opType = inferType(bin, localTypes, localElementTypes, expectedType);
                translateExpression(bin.getLeft(), collector, localTypes, localElementTypes, opType);
                translateExpression(bin.getRight(), collector, localTypes, localElementTypes, opType);
                collector.append("    ").append(getWasmBinaryOp(bin.getOperator(), opType)).append("\n");
            } else if (expr.isUnaryExpr()) {
                UnaryExpr u = expr.asUnaryExpr();
                String uType = inferType(u.getExpression(), localTypes, localElementTypes, expectedType);
                if (u.getOperator() == UnaryExpr.Operator.MINUS) {
                    if ("f64".equals(uType)) {
                        translateExpression(u.getExpression(), collector, localTypes, localElementTypes, uType);
                        collector.append("    f64.neg\n");
                    } else if ("f32".equals(uType)) {
                        translateExpression(u.getExpression(), collector, localTypes, localElementTypes, uType);
                        collector.append("    f32.neg\n");
                    } else if ("i64".equals(uType)) {
                        collector.append("    i64.const 0\n");
                        translateExpression(u.getExpression(), collector, localTypes, localElementTypes, uType);
                        collector.append("    i64.sub\n");
                    } else {
                        collector.append("    i32.const 0\n");
                        translateExpression(u.getExpression(), collector, localTypes, localElementTypes, uType);
                        collector.append("    i32.sub\n");
                    }
                } else if (u.getOperator() == UnaryExpr.Operator.LOGICAL_COMPLEMENT) {
                    translateExpression(u.getExpression(), collector, localTypes, localElementTypes, "i32");
                    collector.append("    i32.eqz\n");
                } else if (u.getOperator() == UnaryExpr.Operator.BITWISE_COMPLEMENT) {
                    translateExpression(u.getExpression(), collector, localTypes, localElementTypes, uType);
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
                    translateExpression(u.getExpression(), collector, localTypes, localElementTypes, expectedType);
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
                    collector.append("    i32.const ").append(sc.length).append("\n");
                } else {
                    collector.append("    i32.const 0\n    i32.const 0\n");
                }
            } else if (expr.isNameExpr()) {
                String var = expr.asNameExpr().getNameAsString();
                boolean isGlobal = globalVars != null && globalVars.containsKey(var) && !localTypes.containsKey(var);
                collector.append(isGlobal ? "    global.get $" : "    local.get $").append(var).append("\n");
            } else if (expr.isEnclosedExpr()) {
                translateExpression(expr.asEnclosedExpr().getInner(), collector, localTypes, localElementTypes, expectedType);
            } else if (expr.isArrayCreationExpr()) {
                ArrayCreationExpr ace = expr.asArrayCreationExpr();
                String elemType = mapType(ace.getElementType());
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
                        translateExpression(init.getValues().get(i), collector, localTypes, localElementTypes, elemType);
                        int offset = headerOffset + i * elemSize;
                        collector.append("    ").append(getStoreInstruction(elemType)).append(" offset=").append(offset).append("\n");
                    }
                    collector.append("    local.get $__arr_alloc_ptr\n");
                } else if (!ace.getLevels().isEmpty() && ace.getLevels().get(0).getDimension().isPresent()) {
                    Expression dimExpr = ace.getLevels().get(0).getDimension().get();
                    translateExpression(dimExpr, collector, localTypes, localElementTypes, "i32");
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
                    translateExpression(init.getValues().get(i), collector, localTypes, localElementTypes, elemType);
                    int offset = headerOffset + i * elemSize;
                    collector.append("    ").append(getStoreInstruction(elemType)).append(" offset=").append(offset).append("\n");
                }
                collector.append("    local.get $__arr_alloc_ptr\n");
            } else if (expr.isArrayAccessExpr()) {
                ArrayAccessExpr aae = expr.asArrayAccessExpr();
                String elemType = getArrayElementType(aae, localElementTypes, expectedType);
                int headerOffset = (elemType.equals("f64") || elemType.equals("i64")) ? 8 : 4;
                int elemSize = (elemType.equals("f64") || elemType.equals("i64")) ? 8 : 4;

                translateExpression(aae.getName(), collector, localTypes, localElementTypes, "i32");
                translateExpression(aae.getIndex(), collector, localTypes, localElementTypes, "i32");
                collector.append("    i32.const ").append(elemSize).append("\n");
                collector.append("    i32.mul\n");
                collector.append("    i32.add\n");
                collector.append("    ").append(getLoadInstruction(elemType)).append(" offset=").append(headerOffset).append("\n");
            } else if (expr.isFieldAccessExpr()) {
                FieldAccessExpr fae = expr.asFieldAccessExpr();
                if (fae.getNameAsString().equals("length")) {
                    translateExpression(fae.getScope(), collector, localTypes, localElementTypes, "i32");
                    collector.append("    i32.load\n");
                }
            } else if (expr.isAssignExpr()) {
                AssignExpr assign = expr.asAssignExpr();
                AssignExpr.Operator op = assign.getOperator();
                if (assign.getTarget().isNameExpr()) {
                    String target = assign.getTarget().asNameExpr().getNameAsString();
                    boolean isGlobal = globalVars != null && globalVars.containsKey(target) && !localTypes.containsKey(target);
                    String targetType = isGlobal ? globalVars.get(target).type : localTypes.getOrDefault(target, "i32");

                    if (op == AssignExpr.Operator.ASSIGN) {
                        translateExpression(assign.getValue(), collector, localTypes, localElementTypes, targetType);
                        collector.append(isGlobal ? "    global.set $" : "    local.set $").append(target).append("\n");
                    } else {
                        // Compound assignment (e.g. +=, -=, *=, /=)
                        collector.append(isGlobal ? "    global.get $" : "    local.get $").append(target).append("\n");
                        translateExpression(assign.getValue(), collector, localTypes, localElementTypes, targetType);
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
                        translateExpression(aae.getName(), collector, localTypes, localElementTypes, "i32");
                        translateExpression(aae.getIndex(), collector, localTypes, localElementTypes, "i32");
                        collector.append("    i32.const ").append(elemSize).append("\n");
                        collector.append("    i32.mul\n");
                        collector.append("    i32.add\n");
                        translateExpression(assign.getValue(), collector, localTypes, localElementTypes, elemType);
                        collector.append("    ").append(getStoreInstruction(elemType)).append(" offset=").append(headerOffset).append("\n");
                    } else {
                        // Compound assignment on array element: arr[i] += val
                        translateExpression(aae.getName(), collector, localTypes, localElementTypes, "i32");
                        translateExpression(aae.getIndex(), collector, localTypes, localElementTypes, "i32");
                        collector.append("    i32.const ").append(elemSize).append("\n");
                        collector.append("    i32.mul\n");
                        collector.append("    i32.add\n");
                        collector.append("    local.set $__arr_alloc_ptr\n");

                        collector.append("    local.get $__arr_alloc_ptr\n");
                        collector.append("    local.get $__arr_alloc_ptr\n");
                        collector.append("    ").append(getLoadInstruction(elemType)).append(" offset=").append(headerOffset).append("\n");
                        translateExpression(assign.getValue(), collector, localTypes, localElementTypes, elemType);
                        BinaryExpr.Operator binOp = mapAssignOpToBinaryOp(op);
                        if (binOp != null) {
                            collector.append("    ").append(getWasmBinaryOp(binOp, elemType)).append("\n");
                        }
                        collector.append("    ").append(getStoreInstruction(elemType)).append(" offset=").append(headerOffset).append("\n");
                    }
                } else {
                    translateExpression(assign.getValue(), collector, localTypes, localElementTypes, expectedType);
                }
            } else if (expr.isVariableDeclarationExpr()) {
                VariableDeclarationExpr vde = expr.asVariableDeclarationExpr();
                for (VariableDeclarator vd : vde.getVariables()) {
                    if (vd.getInitializer().isPresent()) {
                        String vType = mapType(vd.getType());
                        String elemType = vd.getType().isArrayType() ? mapType(vd.getType().asArrayType().getComponentType()) : null;
                        translateExpression(vd.getInitializer().get(), collector, localTypes, localElementTypes, elemType != null ? elemType : vType);
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
                            translateExpression(arg, collector, localTypes, localElementTypes, null);
                        }
                        collector.append("    call $dom_").append(mName).append("\n");
                    } else if (isDomInstanceMethod(mName)) {
                        translateExpression(scope, collector, localTypes, localElementTypes, "i32");
                        for (Expression arg : call.getArguments()) {
                            translateExpression(arg, collector, localTypes, localElementTypes, null);
                        }
                        collector.append("    call $dom_").append(mName).append("\n");
                    } else {
                        translateExpression(scope, collector, localTypes, localElementTypes, null);
                        for (Expression arg : call.getArguments()) {
                            translateExpression(arg, collector, localTypes, localElementTypes, null);
                        }
                        collector.append("    call $").append(mName).append("\n");
                    }
                } else {
                    if (isDomStaticMethod(mName) || isDomInstanceMethod(mName)) {
                        for (Expression arg : call.getArguments()) {
                            translateExpression(arg, collector, localTypes, localElementTypes, null);
                        }
                        collector.append("    call $dom_").append(mName).append("\n");
                    } else {
                        for (Expression arg : call.getArguments()) {
                            translateExpression(arg, collector, localTypes, localElementTypes, null);
                        }
                        collector.append("    call $").append(mName).append("\n");
                    }
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

        private String inferType(Expression expr, Map<String, String> localTypes, Map<String, String> localElementTypes, String fallback) {
            if (expr.isDoubleLiteralExpr()) {
                return "f64";
            }
            if (expr.isLongLiteralExpr()) {
                return "i64";
            }
            if (expr.isIntegerLiteralExpr() || expr.isBooleanLiteralExpr() || expr.isCharLiteralExpr()) {
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
            if (expr.isBinaryExpr()) {
                BinaryExpr b = expr.asBinaryExpr();
                String left = inferType(b.getLeft(), localTypes, localElementTypes, null);
                if ("f64".equals(left)) return "f64";
                if ("i64".equals(left)) return "i64";
                if ("f32".equals(left)) return "f32";
                String right = inferType(b.getRight(), localTypes, localElementTypes, null);
                if ("f64".equals(right)) return "f64";
                if ("i64".equals(right)) return "i64";
                if ("f32".equals(right)) return "f32";
            }
            if (expr.isEnclosedExpr()) {
                return inferType(expr.asEnclosedExpr().getInner(), localTypes, localElementTypes, fallback);
            }
            return (fallback != null) ? fallback : "i32";
        }

        private String getWasmBinaryOp(BinaryExpr.Operator op, String type) {
            String prefix = (type != null && !type.isEmpty()) ? type : "i32";
            boolean isFloat = prefix.equals("f32") || prefix.equals("f64");
            return switch (op) {
                case PLUS -> prefix + ".add";
                case MINUS -> prefix + ".sub";
                case MULTIPLY -> prefix + ".mul";
                case DIVIDE -> isFloat ? (prefix + ".div") : (prefix + ".div_s");
                case REMAINDER -> prefix + ".rem_s";
                case EQUALS -> prefix + ".eq";
                case NOT_EQUALS -> prefix + ".ne";
                case LESS -> isFloat ? (prefix + ".lt") : (prefix + ".lt_s");
                case LESS_EQUALS -> isFloat ? (prefix + ".le") : (prefix + ".le_s");
                case GREATER -> isFloat ? (prefix + ".gt") : (prefix + ".gt_s");
                case GREATER_EQUALS -> isFloat ? (prefix + ".ge") : (prefix + ".ge_s");
                case BINARY_AND, AND -> prefix + ".and";
                case BINARY_OR, OR -> prefix + ".or";
                case XOR -> prefix + ".xor";
                case LEFT_SHIFT -> prefix + ".shl";
                case SIGNED_RIGHT_SHIFT -> prefix + ".shr_s";
                case UNSIGNED_RIGHT_SHIFT -> prefix + ".shr_u";
                default -> prefix + ".add";
            };
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
    }
}
