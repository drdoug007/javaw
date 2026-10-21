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
    static {
        StaticJavaParser.getConfiguration().setLanguageLevel(com.github.javaparser.ParserConfiguration.LanguageLevel.JAVA_17);
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: javaw <JavaFile.java | dir> [extra files...]");
            System.exit(1);
        }

        List<Path> javaFiles = new ArrayList<>();
        boolean optimize = true;
        for (String arg : args) {
            if (arg.equals("-O0") || arg.equals("--no-opt")) {
                optimize = false;
                continue;
            }
            Path p = Paths.get(arg).toAbsolutePath().normalize();
            if (java.nio.file.Files.isDirectory(p)) {
                try (java.util.stream.Stream<Path> stream = java.nio.file.Files.walk(p)) {
                    stream.filter(f -> f.toString().endsWith(".java")).forEach(javaFiles::add);
                } catch (IOException e) {
                    System.err.println("Error reading directory " + p + ": " + e.getMessage());
                }
            } else if (p.toString().endsWith(".java")) {
                javaFiles.add(p);
            }
        }

        if (javaFiles.isEmpty()) {
            System.err.println("No .java files found to compile.");
            System.exit(1);
        }

        Path firstPath = javaFiles.get(0);
        String stem = firstPath.getFileName().toString().replace(".java", "");
        Path watFilePath = firstPath.getParent().resolve(stem + ".wat");
        Path wasmFilePath = firstPath.getParent().resolve(stem + ".wasm");

        try {
            System.out.println("Compiling " + javaFiles.size() + " Java source file(s)...");
            List<CompilationUnit> cus = new ArrayList<>();
            for (Path path : javaFiles) {
                cus.add(StaticJavaParser.parse(path));
            }

            String watContent = generateWat(cus);
            try (FileWriter writer = new FileWriter(watFilePath.toFile())) {
                writer.write(watContent);
            }
            System.out.println("WAT generation successful: " + watFilePath);

            // Attempt to compile to binary WASM if wat2wasm is available
            boolean wasmGenerated = false;
            try {
                Process process = new ProcessBuilder("wat2wasm", "--enable-threads", "--enable-exceptions", "--enable-multi-memory", watFilePath.toString(), "-o", wasmFilePath.toString())
                        .redirectErrorStream(true)
                        .start();
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    System.out.println("Binary WASM generated: " + wasmFilePath);
                    wasmGenerated = true;
                } else {
                    System.err.println("wat2wasm compilation exited with code: " + exitCode);
                }
            } catch (IOException e) {
                // wat2wasm not installed or not in PATH
                System.out.println("wat2wasm not available in PATH; skipping binary .wasm compilation.");
            }

            // Attempt to optimize with wasm-opt if available
            if (wasmGenerated && optimize) {
                try {
                    Process optProcess = new ProcessBuilder("wasm-opt", "-O3", "--enable-simd", "--enable-threads", "--enable-exception-handling", "--enable-multivalue", "--enable-bulk-memory", "--enable-multimemory", wasmFilePath.toString(), "-o", wasmFilePath.toString())
                            .redirectErrorStream(true)
                            .start();
                    int optExit = optProcess.waitFor();
                    if (optExit == 0) {
                        System.out.println("Binary WASM optimized with wasm-opt (-O3): " + wasmFilePath);
                    }
                } catch (IOException e) {
                    // wasm-opt not installed, skipping optimization
                }
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
            if (!fd.isStatic()) continue;
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

    public static boolean hasThreadsOrAtomics(CompilationUnit cu) {
        String code = cu.toString();
        return code.contains("AtomicInteger") || code.contains("Atomics") || code.contains("WasmThread") || code.contains("concurrent");
    }

    public static String generateWat(List<CompilationUnit> cus) {
        if (cus.isEmpty()) {
            return "(module)\n";
        }
        if (cus.size() == 1) {
            return generateWat(cus.get(0));
        }
        CompilationUnit merged = new CompilationUnit();
        for (CompilationUnit cu : cus) {
            cu.getImports().forEach(i -> {
                if (!merged.getImports().contains(i)) {
                    merged.addImport(i.clone());
                }
            });
            cu.getTypes().forEach(t -> merged.addType(t.clone()));
        }
        return generateWat(merged);
    }

    public static String generateWat(CompilationUnit cu) {
        StringBuilder watOutput = new StringBuilder();
        watOutput.append("(module\n");

        Set<String> domImports = DomHelper.collectDomImports(cu);
        DomHelper.emitDomImports(domImports, watOutput);

        Set<String> wasiImports = WasiHelper.collectWasiImports(cu);
        WasiHelper.emitWasiImports(wasiImports, watOutput);

        watOutput.append("  (tag $Exception (export \"Exception\") (param i32))\n");

        boolean multiMem = hasMultiMemory(cu);
        boolean shared = hasThreadsOrAtomics(cu);

        if (multiMem) {
            watOutput.append("  (memory $strings 2)\n");
            watOutput.append("  (export \"memory\" (memory $strings))\n");
            watOutput.append("  (export \"strings\" (memory $strings))\n");
            if (shared) {
                watOutput.append("  (memory $heap 2 20 shared)\n");
                watOutput.append("  (export \"heap\" (memory $heap))\n");
                watOutput.append("  (memory $stack 2 20 shared)\n");
                watOutput.append("  (export \"stack\" (memory $stack))\n");
            } else {
                watOutput.append("  (memory $heap 2)\n");
                watOutput.append("  (export \"heap\" (memory $heap))\n");
                watOutput.append("  (memory $stack 2)\n");
                watOutput.append("  (export \"stack\" (memory $stack))\n");
            }
        } else {
            if (shared) {
                watOutput.append("  (memory (export \"memory\") 2 20 shared)\n");
            } else {
                watOutput.append("  (memory (export \"memory\") 2)\n");
            }
        }

        Map<String, GlobalVar> globalVars = collectGlobalVars(cu);
        for (GlobalVar gv : globalVars.values()) {
            watOutput.append("  (global $").append(gv.name).append(" (mut ").append(gv.type).append(") (")
                    .append(gv.type).append(".const ").append(gv.initVal).append("))\n");
        }

        Map<String, StringConstant> stringPool = collectStringLiterals(cu);
        int heapStart = multiMem ? 0 : 65536; // Multi-memory isolates heap starting at 0; single-memory uses page 1+
        for (Map.Entry<String, StringConstant> entry : stringPool.entrySet()) {
            StringConstant sc = entry.getValue();
            if (multiMem) {
                watOutput.append("  (data (memory $strings) (i32.const ").append(sc.offset).append(") \"")
                        .append(escapeForWat(sc.bytes)).append("\")\n");
            } else {
                watOutput.append("  (data (i32.const ").append(sc.offset).append(") \"")
                        .append(escapeForWat(sc.bytes)).append("\")\n");
                int end = sc.offset + sc.length + 16;
                if (end > heapStart) {
                    heapStart = ((end + 65535) / 65536) * 65536;
                }
            }
        }
        watOutput.append("  (global $__heap_ptr (mut i32) (i32.const ").append(heapStart).append("))\n");

        ClassRegistry classRegistry = ClassRegistry.build(cu);
        classRegistry.emitTypeSignaturesAndTable(watOutput);

        int trueOffset = stringPool.containsKey("true") ? stringPool.get("true").offset : 0;
        int falseOffset = stringPool.containsKey("false") ? stringPool.get("false").offset : 0;

        WasmRuntime.emitRuntimeFunctions(watOutput, trueOffset, falseOffset, !wasiImports.isEmpty());

        cu.accept(new WasmGeneratorVisitor(stringPool, globalVars, classRegistry), watOutput);
        watOutput.append(")\n");
        return watOutput.toString();
    }

    public static CompilationResult compile(String javaSource) {
        return compile("Source.java", javaSource, true);
    }

    public static CompilationResult compile(String javaSource, boolean optimize) {
        return compile("Source.java", javaSource, optimize);
    }

    public static CompilationResult compile(String fileName, String javaSource, boolean optimize) {
        CompilationUnit cu = StaticJavaParser.parse(javaSource);
        String wat = generateWat(cu);
        byte[] wasm = assembleWasm(wat, optimize);
        return new CompilationResult(wat, wasm);
    }

    public static byte[] assembleWasm(String watContent, boolean optimize) {
        try {
            java.nio.file.Path tempWat = java.nio.file.Files.createTempFile("javaw_", ".wat");
            java.nio.file.Path tempWasm = java.nio.file.Files.createTempFile("javaw_", ".wasm");
            java.nio.file.Files.writeString(tempWat, watContent);
            try {
                Process process = new ProcessBuilder("wat2wasm", "--enable-threads", "--enable-exceptions", "--enable-multi-memory", tempWat.toString(), "-o", tempWasm.toString())
                        .redirectErrorStream(true)
                        .start();
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    if (optimize) {
                        try {
                            Process optProcess = new ProcessBuilder("wasm-opt", "-O3", "--enable-simd", "--enable-threads", "--enable-exception-handling", "--enable-multivalue", "--enable-bulk-memory", "--enable-multimemory", tempWasm.toString(), "-o", tempWasm.toString())
                                    .redirectErrorStream(true)
                                    .start();
                            optProcess.waitFor();
                        } catch (Exception ignored) {
                        }
                    }
                    return java.nio.file.Files.readAllBytes(tempWasm);
                }
            } finally {
                java.nio.file.Files.deleteIfExists(tempWat);
                java.nio.file.Files.deleteIfExists(tempWasm);
            }
        } catch (Exception e) {
            // wat2wasm not available or error
        }
        return null;
    }

    public static class CompilationResult {
        private final String wat;
        private final byte[] wasm;

        public CompilationResult(String wat, byte[] wasm) {
            this.wat = wat;
            this.wasm = wasm;
        }

        public String getWat() {
            return wat;
        }

        public byte[] getWasm() {
            return wasm;
        }

        public boolean isWasmGenerated() {
            return wasm != null && wasm.length > 0;
        }
    }

    public static boolean hasMultiMemory(CompilationUnit cu) {
        if (cu.getImports().stream().anyMatch(i -> i.getNameAsString().startsWith("com.example.javaw.memory"))) {
            return true;
        }
        return cu.findAll(com.github.javaparser.ast.expr.MethodCallExpr.class).stream()
                .anyMatch(m -> m.getScope().map(s -> s.toString().equals("Memory") || s.toString().endsWith(".Memory")).orElse(false))
                || cu.findAll(com.github.javaparser.ast.expr.FieldAccessExpr.class).stream()
                .anyMatch(f -> f.getScope().toString().equals("Memory") || f.getScope().toString().endsWith(".Memory"));
    }

    public static String mapType(String typeStr) {
        return TypeMapper.mapType(typeStr);
    }

    public static String mapType(Type type) {
        return TypeMapper.mapType(type);
    }
}
