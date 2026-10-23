package com.example.javaw;

import com.example.javaw.js.JS;
import com.example.javaw.js.JsAnnotationHelper;
import com.example.javaw.js.JsSnippetModel;
import com.example.javaw.sourcemap.SourceMap;
import com.example.javaw.sourcemap.SourceMapGenerator;
import com.example.javaw.sourcemap.SourceMapTracker;
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
import java.nio.file.Files;
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
            System.err.println("Usage: javaw <JavaFile.java | dir> [extra files...] [-O0|--no-opt] [-g|--source-map]");
            System.exit(1);
        }

        List<Path> javaFiles = new ArrayList<>();
        boolean optimize = true;
        boolean generateSourceMap = true;

        for (String arg : args) {
            if (arg.equals("-O0") || arg.equals("--no-opt")) {
                optimize = false;
                continue;
            }
            if (arg.equals("-g") || arg.equals("--source-map")) {
                generateSourceMap = true;
                continue;
            }
            if (arg.equals("--no-source-map")) {
                generateSourceMap = false;
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
        Path watMapFilePath = firstPath.getParent().resolve(stem + ".wat.map");
        Path wasmMapFilePath = firstPath.getParent().resolve(stem + ".wasm.map");

        String optLevel = optimize ? "-O3" : "-O0";
        boolean success = compileFiles(javaFiles, watFilePath, wasmFilePath, watMapFilePath, wasmMapFilePath, optLevel, generateSourceMap, true);
        if (!success) {
            System.exit(1);
        }
    }

    public static boolean compileFiles(List<Path> javaFiles, Path watFilePath, Path wasmFilePath, Path watMapFilePath, Path wasmMapFilePath, String optimizationLevel, boolean generateSourceMap, boolean enableThreads) {
        if (javaFiles == null || javaFiles.isEmpty()) {
            return false;
        }

        try {
            System.out.println("Compiling " + javaFiles.size() + " Java source file(s)...");
            List<CompilationUnit> cus = new ArrayList<>();
            String stem = watFilePath.getFileName().toString().replace(".wat", "");
            SourceMapGenerator sourceMapGen = generateSourceMap ? new SourceMapGenerator(stem + ".wat") : null;

            for (Path path : javaFiles) {
                CompilationUnit cu = StaticJavaParser.parse(path);
                cus.add(cu);
                if (sourceMapGen != null) {
                    try {
                        String sourceContent = Files.readString(path);
                        sourceMapGen.addSource(path.getFileName().toString(), sourceContent);
                    } catch (Exception ignored) {
                        sourceMapGen.addSource(path.getFileName().toString(), null);
                    }
                }
            }

            String watContent = generateWat(cus, sourceMapGen);
            if (watFilePath.getParent() != null) {
                Files.createDirectories(watFilePath.getParent());
            }
            try (FileWriter writer = new FileWriter(watFilePath.toFile())) {
                writer.write(watContent);
            }
            System.out.println("WAT generation successful: " + watFilePath);

            if (sourceMapGen != null) {
                String sourceMapJson = sourceMapGen.generateJson();
                if (watMapFilePath != null) {
                    if (watMapFilePath.getParent() != null) Files.createDirectories(watMapFilePath.getParent());
                    Files.writeString(watMapFilePath, sourceMapJson);
                }
                if (wasmMapFilePath != null) {
                    if (wasmMapFilePath.getParent() != null) Files.createDirectories(wasmMapFilePath.getParent());
                    Files.writeString(wasmMapFilePath, sourceMapJson);
                    System.out.println("Source Map generated: " + wasmMapFilePath);
                }
            }

            // Attempt to compile to binary WASM if wat2wasm is available
            boolean wasmGenerated = false;
            if (wasmFilePath != null) {
                if (wasmFilePath.getParent() != null) {
                    Files.createDirectories(wasmFilePath.getParent());
                }
                List<String> wat2wasmCmd = new ArrayList<>();
                wat2wasmCmd.add("wat2wasm");
                if (enableThreads) {
                    wat2wasmCmd.add("--enable-threads");
                }
                wat2wasmCmd.add("--enable-exceptions");
                wat2wasmCmd.add("--enable-multi-memory");
                wat2wasmCmd.add("--enable-annotations");
                wat2wasmCmd.add(watFilePath.toString());
                wat2wasmCmd.add("-o");
                wat2wasmCmd.add(wasmFilePath.toString());

                try {
                    Process process = new ProcessBuilder(wat2wasmCmd)
                            .redirectErrorStream(true)
                            .start();
                    int exitCode = process.waitFor();
                    if (exitCode == 0) {
                        System.out.println("Binary WASM generated: " + wasmFilePath);
                        wasmGenerated = true;

                        // Ensure sourceMappingURL is embedded in the binary
                        if (generateSourceMap && wasmMapFilePath != null) {
                            byte[] wasmBytes = Files.readAllBytes(wasmFilePath);
                            byte[] embedded = SourceMapGenerator.embedSourceMapInWasm(wasmBytes, wasmMapFilePath.getFileName().toString());
                            if (embedded.length != wasmBytes.length) {
                                Files.write(wasmFilePath, embedded);
                            }
                        }
                    } else {
                        String procOut = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                        System.err.println("wat2wasm compilation exited with code: " + exitCode);
                        if (!procOut.isBlank()) {
                            System.err.println(procOut);
                        }
                        return false;
                    }
                } catch (IOException e) {
                    // wat2wasm not installed or not in PATH
                    System.out.println("wat2wasm not available in PATH; skipping binary .wasm compilation.");
                }

                // Attempt to optimize with wasm-opt if available
                boolean shouldOptimize = optimizationLevel != null && !optimizationLevel.equals("-O0") && !optimizationLevel.equals("--no-opt");
                if (wasmGenerated && shouldOptimize) {
                    try {
                        List<String> optCmd = new ArrayList<>();
                        optCmd.add("wasm-opt");
                        optCmd.add(optimizationLevel);
                        optCmd.add("--enable-simd");
                        if (enableThreads) {
                            optCmd.add("--enable-threads");
                        }
                        optCmd.add("--enable-exception-handling");
                        optCmd.add("--enable-multivalue");
                        optCmd.add("--enable-bulk-memory");
                        optCmd.add("--enable-multimemory");
                        optCmd.add(wasmFilePath.toString());
                        optCmd.add("-o");
                        optCmd.add(wasmFilePath.toString());

                        Process optProcess = new ProcessBuilder(optCmd)
                                .redirectErrorStream(true)
                                .start();
                        int optExit = optProcess.waitFor();
                        if (optExit == 0) {
                            System.out.println("Binary WASM optimized with wasm-opt (" + optimizationLevel + "): " + wasmFilePath);
                        }
                    } catch (IOException e) {
                        // wasm-opt not installed, skipping optimization
                    }
                }
            }

            System.out.println("Compilation successful.");
            return true;
        } catch (Exception e) {
            System.err.println("Error during compilation: " + e.getMessage());
            e.printStackTrace();
            return false;
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
                if (vd.getInitializer().isPresent() && !BoxingSupport.boxed(vd.getTypeAsString())) {
                    Expression initExpr = vd.getInitializer().get();
                    if (initExpr.isIntegerLiteralExpr()) {
                        initVal = initExpr.asIntegerLiteralExpr().getValue();
                    } else if (initExpr.isDoubleLiteralExpr()) {
                        initVal = cleanFloatStr(initExpr.asDoubleLiteralExpr().getValue());
                    } else if (initExpr.isBooleanLiteralExpr()) {
                        initVal = initExpr.asBooleanLiteralExpr().getValue() ? "1" : "0";
                    } else {
                        initVal = vType.equals("f64") || vType.equals("f32") ? "0.0" : "0";
                    }
                }
                globals.put(vName, new GlobalVar(vName, vType, initVal, vd.getTypeAsString()));
            }
        }

        List<com.github.javaparser.ast.body.EnumDeclaration> enumDecls = cu.findAll(com.github.javaparser.ast.body.EnumDeclaration.class);
        for (com.github.javaparser.ast.body.EnumDeclaration ed : enumDecls) {
            String enumName = ed.getNameAsString();
            for (com.github.javaparser.ast.body.EnumConstantDeclaration ecd : ed.getEntries()) {
                String gName = enumName + "_" + ecd.getNameAsString();
                globals.put(gName, new GlobalVar(gName, "i32", "0", enumName));
            }
        }
        return globals;
    }

    public static String cleanFloatStr(String val) {
        if (val == null) return "0.0";
        val = val.trim();
        while (val.endsWith("f") || val.endsWith("F") || val.endsWith("d") || val.endsWith("D")) {
            val = val.substring(0, val.length() - 1);
        }
        if (!val.contains(".")) {
            val += ".0";
        }
        return val;
    }

    public static String unescapeString(String val) {
        if (val == null || !val.contains("\\")) return val;
        StringBuilder sb = new StringBuilder();
        int len = val.length();
        for (int i = 0; i < len; i++) {
            char c = val.charAt(i);
            if (c == '\\' && i + 1 < len) {
                char next = val.charAt(i + 1);
                switch (next) {
                    case 'n' -> { sb.append('\n'); i++; }
                    case 't' -> { sb.append('\t'); i++; }
                    case 'r' -> { sb.append('\r'); i++; }
                    case 'b' -> { sb.append('\b'); i++; }
                    case 'f' -> { sb.append('\f'); i++; }
                    case '"' -> { sb.append('"'); i++; }
                    case '\'' -> { sb.append('\''); i++; }
                    case '\\' -> { sb.append('\\'); i++; }
                    case '0' -> { sb.append('\0'); i++; }
                    case 'u' -> {
                        if (i + 5 < len) {
                            try {
                                int unicode = Integer.parseInt(val.substring(i + 2, i + 6), 16);
                                sb.append((char) unicode);
                                i += 5;
                            } catch (NumberFormatException e) {
                                sb.append(c);
                            }
                        } else {
                            sb.append(c);
                        }
                    }
                    default -> sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static int unescapeChar(String val) {
        if (val == null || val.isEmpty()) return 0;
        if (val.startsWith("\\") && val.length() >= 2) {
            char next = val.charAt(1);
            return switch (next) {
                case 'n' -> '\n';
                case 't' -> '\t';
                case 'r' -> '\r';
                case 'b' -> '\b';
                case 'f' -> '\f';
                case '\'' -> '\'';
                case '"' -> '"';
                case '\\' -> '\\';
                case '0' -> 0;
                case 'u' -> val.length() >= 6 ? Integer.parseInt(val.substring(2, 6), 16) : val.charAt(0);
                default -> next;
            };
        }
        return val.charAt(0);
    }

    public static Map<String, StringConstant> collectStringLiterals(CompilationUnit cu) {
        Map<String, StringConstant> pool = new LinkedHashMap<>();
        int currentOffset = 4096; // 0x1000

        List<StringLiteralExpr> literals = cu.findAll(StringLiteralExpr.class);
        for (StringLiteralExpr lit : literals) {
            String val = unescapeString(lit.getValue());
            if (!pool.containsKey(val)) {
                byte[] bytes = val.getBytes(StandardCharsets.UTF_8);
                pool.put(val, new StringConstant(currentOffset, bytes.length, bytes));
                currentOffset += bytes.length + 1; // null-terminated spacing
            }
        }

        List<com.github.javaparser.ast.body.EnumDeclaration> enumDecls = cu.findAll(com.github.javaparser.ast.body.EnumDeclaration.class);
        for (com.github.javaparser.ast.body.EnumDeclaration ed : enumDecls) {
            String enumName = ed.getNameAsString();
            if (!pool.containsKey(enumName)) {
                byte[] bytes = enumName.getBytes(StandardCharsets.UTF_8);
                pool.put(enumName, new StringConstant(currentOffset, bytes.length, bytes));
                currentOffset += bytes.length + 1;
            }
            for (com.github.javaparser.ast.body.EnumConstantDeclaration ecd : ed.getEntries()) {
                String val = ecd.getNameAsString();
                if (!pool.containsKey(val)) {
                    byte[] bytes = val.getBytes(StandardCharsets.UTF_8);
                    pool.put(val, new StringConstant(currentOffset, bytes.length, bytes));
                    currentOffset += bytes.length + 1;
                }
            }
        }

        List<com.github.javaparser.ast.body.ClassOrInterfaceDeclaration> classDecls = cu.findAll(com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class);
        for (com.github.javaparser.ast.body.ClassOrInterfaceDeclaration cd : classDecls) {
            String cName = cd.getNameAsString();
            if (!pool.containsKey(cName)) {
                byte[] bytes = cName.getBytes(StandardCharsets.UTF_8);
                pool.put(cName, new StringConstant(currentOffset, bytes.length, bytes));
                currentOffset += bytes.length + 1;
            }
        }

        String[] defaultStrings = {"null", "@", "true", "false", "class ", "interface ", "Object", "String", "Class", "Thread", "Runnable", "Enum", "int", "long", "double", "float", "boolean", "char", "byte", "short", "void"};
        for (String ds : defaultStrings) {
            if (!pool.containsKey(ds)) {
                byte[] bytes = ds.getBytes(StandardCharsets.UTF_8);
                pool.put(ds, new StringConstant(currentOffset, bytes.length, bytes));
                currentOffset += bytes.length + 1;
            }
        }

        List<com.github.javaparser.ast.expr.FieldAccessExpr> fieldAccesses = cu.findAll(com.github.javaparser.ast.expr.FieldAccessExpr.class);
        for (com.github.javaparser.ast.expr.FieldAccessExpr fae : fieldAccesses) {
            String scope = fae.getScope().toString();
            if (scope.equals("WASITerminal") || scope.endsWith(".WASITerminal") || scope.equals("Terminal") || scope.endsWith(".Terminal")) {
                String ansi = switch (fae.getNameAsString()) {
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
                if (!ansi.isEmpty() && !pool.containsKey(ansi)) {
                    byte[] bytes = ansi.getBytes(StandardCharsets.UTF_8);
                    pool.put(ansi, new StringConstant(currentOffset, bytes.length, bytes));
                    currentOffset += bytes.length + 1;
                }
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
        return code.contains("AtomicInteger") || code.contains("Atomics") || code.contains("WasmThread") || code.contains("concurrent") || code.contains("Thread") || code.contains("Runnable");
    }

    public static String generateWat(List<CompilationUnit> cus) {
        return generateWat(cus, null);
    }

    public static String generateWat(List<CompilationUnit> cus, SourceMapGenerator sourceMapGen) {
        if (cus.isEmpty()) {
            return "(module)\n";
        }
        if (cus.size() == 1) {
            String sourceName = cus.get(0).getStorage().map(s -> s.getPath().getFileName().toString()).orElse("Source.java");
            return generateWat(cus.get(0), sourceMapGen, sourceName);
        }
        Map<String, String> typeNames = JavaSourcePreparation.typeNames(cus);
        CompilationUnit merged = new CompilationUnit();
        for (CompilationUnit original : cus) {
            CompilationUnit cu = original.clone();
            JavaSourcePreparation.markSource(cu, cu.getStorage().map(s -> s.getPath().getFileName().toString()).orElse("Source.java"));
            JavaSourcePreparation.normalizeTypes(cu, typeNames);
            cu.getImports().forEach(i -> {
                if (!merged.getImports().contains(i)) {
                    merged.addImport(i.clone());
                }
            });
            cu.getTypes().forEach(t -> merged.getTypes().add(t.clone()));
        }
        String sourceName = cus.get(0).getStorage().map(s -> s.getPath().getFileName().toString()).orElse("Source.java");
        return generateWat(merged, sourceMapGen, sourceName);
    }

    public static String generateWat(CompilationUnit cu) {
        return generateWat(cu, null, "Source.java");
    }

    public static String generateWat(CompilationUnit cu, SourceMapGenerator sourceMapGen, String sourceName) {
        StringBuilder watOutput = new StringBuilder();
        watOutput.append("(module\n");

        cu = cu.clone();
        JavaSourcePreparation.markSource(cu, sourceName);
        JavaSourcePreparation.normalizeTypes(cu, JavaSourcePreparation.typeNames(List.of(cu)));
        SourceRuntimeSupport.prepare(cu);
        ReflectionSupport reflection = ReflectionSupport.prepare(cu);
        SpringWebSupport springWeb = SpringWebSupport.lower(cu);
        DecimalSupport.addTypes(cu);
        LibrarySupport.addTypes(cu);
        IoSupport.addTypes(cu);
        CollectionSupport.addTypes(cu);
        StreamSupport.addTypes(cu);
        PrimitiveStreamSupport.addTypes(cu);
        CollectorSupport.addTypes(cu);
        JavaSourcePreparation.normalizeTypes(cu, JavaSourcePreparation.typeNames(List.of(cu)));
        BoxingSupport.addWrappers(cu);
        if (reflection != null) reflection.finish(cu);
        JavaSourcePreparation.normalizeVarargs(cu);
        JavaSourcePreparation.resolveLocalScopes(cu);
        LambdaSupport.lower(cu);
        LambdaSupport.lowerLoops(cu);
        JavaSourcePreparation.resolveLocalScopes(cu);
        ClassRegistry classRegistry = ClassRegistry.build(cu);
        if (reflection != null) reflection.register(classRegistry);
        GenericTypes.validateSourceTypes(cu, classRegistry);
        springWeb.emitMetadata(classRegistry, watOutput);

        if (sourceMapGen != null && sourceMapGen.getFile() != null && !sourceMapGen.getFile().isEmpty()) {
            String mapUrl = sourceMapGen.getFile().endsWith(".wasm") ?
                    sourceMapGen.getFile() + ".map" :
                    sourceMapGen.getFile().replace(".wat", ".wasm") + ".map";
            watOutput.append("  (@custom \"sourceMappingURL\" \"")
                    .append(SourceMapGenerator.escapeCustomSectionPayload(mapUrl))
                    .append("\")\n");
        }

        Set<String> domImports = DomHelper.collectDomImports(cu);
        DomHelper.emitDomImports(domImports, watOutput);

        Map<String, JsSnippetModel> jsSnippets = JsAnnotationHelper.collectJsSnippets(cu);
        if (!jsSnippets.isEmpty()) {
            String snippetsJson = JsAnnotationHelper.snippetsToJson(jsSnippets);
            watOutput.append("  (@custom \"javaw_js_snippets\" \"")
                    .append(SourceMapGenerator.escapeRawCustomSectionPayload(snippetsJson))
                    .append("\")\n");
            for (JsSnippetModel snippet : jsSnippets.values()) {
                watOutput.append("  ").append(snippet.getWasmImportSignature()).append("\n");
            }
        }

        Map<String, com.example.javaw.js.JsExportModel> jsExports = JsAnnotationHelper.collectJsExports(cu, classRegistry);
        if (!jsExports.isEmpty()) {
            String exportsJson = JsAnnotationHelper.exportsToJson(jsExports);
            watOutput.append("  (@custom \"javaw_js_exports\" \"")
                    .append(SourceMapGenerator.escapeRawCustomSectionPayload(exportsJson))
                    .append("\")\n");
        }

        List<String> jsCodeBlocks = JsAnnotationHelper.collectJsCodeBlocks(cu);
        if (!jsCodeBlocks.isEmpty()) {
            String codeJson = JsAnnotationHelper.codeBlocksToJson(jsCodeBlocks);
            watOutput.append("  (@custom \"javaw_js_code\" \"")
                    .append(SourceMapGenerator.escapeRawCustomSectionPayload(codeJson))
                    .append("\")\n");
        }

        Map<String, String> jsResources = JsAnnotationHelper.collectJsResources(cu);
        if (!jsResources.isEmpty()) {
            String resourcesJson = JsAnnotationHelper.resourcesToJson(jsResources);
            watOutput.append("  (@custom \"javaw_js_resources\" \"")
                    .append(SourceMapGenerator.escapeRawCustomSectionPayload(resourcesJson))
                    .append("\")\n");
        }

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
            String initInstruction = switch (gv.type) {
                case "v128" -> "(v128.const i32x4 0 0 0 0)";
                case "f64" -> "(f64.const " + cleanFloatStr(gv.initVal) + ")";
                case "f32" -> "(f32.const " + cleanFloatStr(gv.initVal) + ")";
                case "i64" -> "(i64.const " + gv.initVal + ")";
                default -> "(i32.const " + gv.initVal + ")";
            };
            watOutput.append("  (global $").append(gv.name).append(" (mut ").append(gv.type).append(") ")
                    .append(initInstruction).append(")\n");
        }

        Map<String, StringConstant> stringPool = collectStringLiterals(cu);
        int heapStart = 65536; // Single-memory and multi-memory isolate heap starting at page 1+
        for (Map.Entry<String, StringConstant> entry : stringPool.entrySet()) {
            StringConstant sc = entry.getValue();
            if (multiMem) {
                watOutput.append("  (data (memory $strings) (i32.const ").append(sc.offset).append(") \"")
                        .append(escapeForWat(sc.bytes)).append("\")\n");
            } else {
                watOutput.append("  (data (i32.const ").append(sc.offset).append(") \"")
                        .append(escapeForWat(sc.bytes)).append("\")\n");
            }
            int end = sc.offset + sc.length + 16;
            if (end > heapStart) {
                heapStart = ((end + 65535) / 65536) * 65536;
            }
        }
        watOutput.append("  (global $__heap_base i32 (i32.const ").append(heapStart).append("))\n");
        watOutput.append("  (global $__heap_ptr (mut i32) (i32.const ").append(heapStart).append("))\n");
        for (String cName : classRegistry.getClasses().keySet()) {
            watOutput.append("  (global $__class_").append(ClassRegistry.classSymbol(cName)).append(" (mut i32) (i32.const 0))\n");
        }

        classRegistry.emitTypeSignaturesAndTable(watOutput);

        int trueOffset = stringPool.containsKey("true") ? stringPool.get("true").offset : 0;
        int falseOffset = stringPool.containsKey("false") ? stringPool.get("false").offset : 0;

        WasmRuntime.emitRuntimeFunctions(watOutput, trueOffset, falseOffset, !wasiImports.isEmpty());

        SourceMapTracker tracker = sourceMapGen != null ? new SourceMapTracker(sourceMapGen, sourceName) : null;
        cu.accept(new WasmGeneratorVisitor(stringPool, globalVars, classRegistry, tracker, jsSnippets), watOutput);
        String initializer=cu.findAll(com.github.javaparser.ast.body.EnumDeclaration.class).isEmpty() ? "$__init_classes" : "$__init_enums";
        if(cu.getClassByName("BigDecimal").isPresent()) {
            watOutput.append("  (func $__javaw_initialize\n    call ").append(initializer)
                .append("\n    call ").append(classRegistry.findMethod("BigInteger","__javaw_initialize").getFunctionName())
                .append("\n    call ").append(classRegistry.findMethod("BigDecimal","__javaw_initialize").getFunctionName())
                .append(")\n  (start $__javaw_initialize)\n");
        } else watOutput.append("  (start ").append(initializer).append(")\n");
        ManagedHeap.emitCollector(watOutput);
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
        CompilationUnit cu;
        try { cu = StaticJavaParser.parse(javaSource); }
        catch (com.github.javaparser.ParseProblemException error) { throw new IllegalArgumentException(fileName + ": " + error.getMessage(), error); }
        String stem = fileName.endsWith(".java") ? fileName.substring(0, fileName.length() - 5) : fileName;
        SourceMapGenerator sourceMapGen = new SourceMapGenerator(stem + ".wat");
        sourceMapGen.addSource(fileName, javaSource);

        String wat = generateWat(cu, sourceMapGen, fileName);
        byte[] wasm = assembleWasm(wat, optimize);
        Map<String, JsSnippetModel> jsSnippets = JsAnnotationHelper.collectJsSnippets(cu);
        ClassRegistry classRegistry = ClassRegistry.build(cu);
        Map<String, com.example.javaw.js.JsExportModel> jsExports = JsAnnotationHelper.collectJsExports(cu, classRegistry);
        List<String> jsCodeBlocks = JsAnnotationHelper.collectJsCodeBlocks(cu);
        Map<String, String> jsResources = JsAnnotationHelper.collectJsResources(cu);
        return new CompilationResult(wat, wasm, sourceMapGen, jsSnippets, jsExports, jsCodeBlocks, jsResources);
    }

    public static byte[] assembleWasm(String watContent, boolean optimize) {
        try {
            java.nio.file.Path tempWat = java.nio.file.Files.createTempFile("javaw_", ".wat");
            java.nio.file.Path tempWasm = java.nio.file.Files.createTempFile("javaw_", ".wasm");
            java.nio.file.Files.writeString(tempWat, watContent);
            try {
                Process process = new ProcessBuilder("wat2wasm", "--enable-threads", "--enable-exceptions", "--enable-multi-memory", "--enable-annotations", tempWat.toString(), "-o", tempWasm.toString())
                        .redirectErrorStream(true)
                        .start();
                String errOutput = new String(process.getInputStream().readAllBytes());
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    System.err.println("wat2wasm error in Phase6: " + errOutput);
                }
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
        private final SourceMapGenerator sourceMapGenerator;
        private final Map<String, JsSnippetModel> jsSnippets;
        private final Map<String, com.example.javaw.js.JsExportModel> jsExports;
        private final List<String> jsCodeBlocks;
        private final Map<String, String> jsResources;

        public CompilationResult(String wat, byte[] wasm) {
            this(wat, wasm, null, java.util.Collections.emptyMap(), java.util.Collections.emptyMap(), java.util.Collections.emptyList(), java.util.Collections.emptyMap());
        }

        public CompilationResult(String wat, byte[] wasm, SourceMapGenerator sourceMapGenerator) {
            this(wat, wasm, sourceMapGenerator, java.util.Collections.emptyMap(), java.util.Collections.emptyMap(), java.util.Collections.emptyList(), java.util.Collections.emptyMap());
        }

        public CompilationResult(String wat, byte[] wasm, SourceMapGenerator sourceMapGenerator, Map<String, JsSnippetModel> jsSnippets) {
            this(wat, wasm, sourceMapGenerator, jsSnippets, java.util.Collections.emptyMap(), java.util.Collections.emptyList(), java.util.Collections.emptyMap());
        }

        public CompilationResult(String wat, byte[] wasm, SourceMapGenerator sourceMapGenerator,
                                 Map<String, JsSnippetModel> jsSnippets,
                                 Map<String, com.example.javaw.js.JsExportModel> jsExports) {
            this(wat, wasm, sourceMapGenerator, jsSnippets, jsExports, java.util.Collections.emptyList(), java.util.Collections.emptyMap());
        }

        public CompilationResult(String wat, byte[] wasm, SourceMapGenerator sourceMapGenerator,
                                 Map<String, JsSnippetModel> jsSnippets,
                                 Map<String, com.example.javaw.js.JsExportModel> jsExports,
                                 List<String> jsCodeBlocks,
                                 Map<String, String> jsResources) {
            this.wat = wat;
            this.wasm = wasm;
            this.sourceMapGenerator = sourceMapGenerator;
            this.jsSnippets = jsSnippets != null ? jsSnippets : java.util.Collections.emptyMap();
            this.jsExports = jsExports != null ? jsExports : java.util.Collections.emptyMap();
            this.jsCodeBlocks = jsCodeBlocks != null ? jsCodeBlocks : java.util.Collections.emptyList();
            this.jsResources = jsResources != null ? jsResources : java.util.Collections.emptyMap();
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

        public SourceMapGenerator getSourceMapGenerator() {
            return sourceMapGenerator;
        }

        public SourceMap getSourceMap() {
            if (sourceMapGenerator == null) return null;
            return SourceMap.parse(sourceMapGenerator.generateJson());
        }

        public String getSourceMapJson() {
            return sourceMapGenerator != null ? sourceMapGenerator.generateJson() : null;
        }

        public String getSourceMapDataUri() {
            return sourceMapGenerator != null ? sourceMapGenerator.generateBase64DataUri() : null;
        }

        public Map<String, JsSnippetModel> getJsSnippets() {
            return jsSnippets;
        }

        public String getJsSnippetsJson() {
            return JsAnnotationHelper.snippetsToJson(jsSnippets);
        }

        public Map<String, com.example.javaw.js.JsExportModel> getJsExports() {
            return jsExports;
        }

        public String getJsExportsJson() {
            return JsAnnotationHelper.exportsToJson(jsExports);
        }

        public List<String> getJsCodeBlocks() {
            return jsCodeBlocks;
        }

        public String getJsCodeBlocksJson() {
            return JsAnnotationHelper.codeBlocksToJson(jsCodeBlocks);
        }

        public Map<String, String> getJsResources() {
            return jsResources;
        }

        public String getJsResourcesJson() {
            return JsAnnotationHelper.resourcesToJson(jsResources);
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
