package com.example.javaw;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.type.Type;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Main compiler entry point and coordinator for compiling Java source code into WebAssembly.
 */
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

    public static Map<String, StringConstant> collectStringLiterals(CompilationUnit cu) {
        Map<String, StringConstant> pool = new LinkedHashMap<>();
        int currentOffset = 4096; // 0x1000

        List<StringLiteralExpr> literals = cu.findAll(StringLiteralExpr.class);
        for (StringLiteralExpr lit : literals) {
            String val = lit.getValue();
            if (!pool.containsKey(val)) {
                byte[] bytes = val.getBytes(StandardCharsets.UTF_8);
                pool.put(val, new StringConstant(currentOffset, bytes.length, bytes));
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
        return DomHelper.collectDomImports(cu);
    }

    public static boolean isDomStaticMethod(String mName) {
        return DomHelper.isDomStaticMethod(mName);
    }

    public static boolean isDomInstanceMethod(String mName) {
        return DomHelper.isDomInstanceMethod(mName);
    }

    public static String generateWat(CompilationUnit cu) {
        StringBuilder watOutput = new StringBuilder();
        watOutput.append("(module\n");

        Set<String> domImports = DomHelper.collectDomImports(cu);
        DomHelper.emitDomImports(domImports, watOutput);

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

        int trueOffset = stringPool.containsKey("true") ? stringPool.get("true").offset : 0;
        int falseOffset = stringPool.containsKey("false") ? stringPool.get("false").offset : 0;

        WasmRuntime.emitRuntimeFunctions(watOutput, trueOffset, falseOffset);

        cu.accept(new WasmGeneratorVisitor(stringPool, globalVars), watOutput);
        watOutput.append(")\n");
        return watOutput.toString();
    }

    public static String mapType(String typeStr) {
        return TypeMapper.mapType(typeStr);
    }

    public static String mapType(Type type) {
        return TypeMapper.mapType(type);
    }
}
