package com.example.javaw.bytecode;

import com.example.javaw.FloatingPointSupport;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class BytecodeCompiler {

    public static String compileClass(byte[] classBytes) {
        ClassBytecodeCompiler compiler = ClassBytecodeCompiler.fromBytes(classBytes);
        return compiler.compileToWat();
    }

    public static String compileClass(InputStream classInputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int r;
        while ((r = classInputStream.read(buf)) != -1) {
            baos.write(buf, 0, r);
        }
        return compileClass(baos.toByteArray());
    }

    public static String compileJarWithExceptionRuntime(InputStream input) throws IOException {
        return compileJar(input, true,List.of());
    }

    public static String compileJarWithExceptionRuntime(InputStream input,List<List<String>> proxyShapes) throws IOException {
        return compileJar(input,true,proxyShapes);
    }

    public static String compileClassWithExceptionRuntime(byte[] bytes) throws IOException {
        return compileClasses(new ArrayList<>(List.of(BytecodeInput.read(bytes))), true);
    }

    public static String compileClassWithExceptionRuntime(byte[] bytes,List<List<String>> proxyShapes) throws IOException {
        return compileClasses(new ArrayList<>(List.of(BytecodeInput.read(bytes))),true,proxyShapes);
    }

    public static String compileJar(InputStream jarInputStream) throws IOException { return compileJar(jarInputStream, false); }

    private static String compileJar(InputStream jarInputStream, boolean exceptionRuntime) throws IOException {return compileJar(jarInputStream,exceptionRuntime,List.of());}
    private static String compileJar(InputStream jarInputStream, boolean exceptionRuntime,List<List<String>> proxyShapes) throws IOException {
        JarInputStream jis = new JarInputStream(jarInputStream);
        JarEntry entry;
        List<ClassNode> classes = new ArrayList<>();

        while ((entry = jis.getNextJarEntry()) != null) {
            if (entry.getName().endsWith(".class") && !entry.getName().equals("module-info.class")) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int r;
                while ((r = jis.read(buf)) != -1) {
                    baos.write(buf, 0, r);
                }
                ClassNode cn = BytecodeInput.read(baos.toByteArray());
                classes.add(cn);
            }
        }

        return compileClasses(classes, exceptionRuntime,proxyShapes);
    }

    private static String compileClasses(List<ClassNode> classes, boolean exceptionRuntime) throws IOException {return compileClasses(classes,exceptionRuntime,List.of());}
    private static String compileClasses(List<ClassNode> classes, boolean exceptionRuntime,List<List<String>> proxyShapes) throws IOException {
        if (exceptionRuntime) {
            Map<String, ClassNode> runtime = BytecodeExceptionRuntime.definitions();
            for (ClassNode type : classes)
                if (runtime.containsKey(type.name))
                    throw BytecodeCompatibility.failure(type, null, null, "exception runtime redefinition",
                            "use the isolated bundled runtime or explicitly linked definitions, not both");
            classes.addAll(runtime.values());
        }
        if (exceptionRuntime) {
            BytecodeReflection.normalize(classes);
            BytecodeProxies.normalize(classes,proxyShapes);
            BytecodeLookups.normalize(classes);
            BytecodeHandles.normalize(classes);
            BytecodeDynamic.normalize(classes);
            BytecodeInvokes.normalize(classes);
        }
        StringBuilder combinedWat = new StringBuilder();
        combinedWat.append("(module\n");
        combinedWat.append("    (import \"env\" \"memory\" (memory 2 20))\n");
        combinedWat.append("    (import \"env\" \"__heap_alloc\" (func $__heap_alloc (param i32) (result i32)))\n");

        if (classes.stream().anyMatch(ClassBytecodeCompiler::usesFloatingRemainder))
            combinedWat.append(FloatingPointSupport.remainderFunctionsWat());


        Map<String, ClassNode> linked = new LinkedHashMap<>();
        for (ClassNode cn : classes) {
            if (linked.putIfAbsent(cn.name, cn) != null)
                throw BytecodeCompatibility.failure(cn, null, null, "duplicate class declaration",
                        "provide exactly one definition of " + cn.name + " in the linked JAR");
        }
        BytecodeDispatch runtimeDispatch = new BytecodeDispatch(linked);
        if (classes.stream().anyMatch(BytecodeArrays::usesRuntime)) combinedWat.append(BytecodeArrays.runtimeWat(runtimeDispatch));
        Set<BytecodeExceptions.Implicit> implicit = EnumSet.noneOf(BytecodeExceptions.Implicit.class);
        for (ClassNode type : classes) implicit.addAll(BytecodeExceptions.implicitRuntime(type, runtimeDispatch));
        if (!implicit.isEmpty() || classes.stream().anyMatch(BytecodeExceptions::usesRuntime)) combinedWat.append(BytecodeExceptions.TAG_WAT);
        for (var kind : implicit) combinedWat.append(runtimeDispatch.implicitExceptionRuntimeWat(kind));
        if(exceptionRuntime)combinedWat.append("""
            (func $__double_bits (param f64) (result i64) local.get 0 i64.reinterpret_f64)
            (func $__float_bits (param f32) (result i32) local.get 0 i32.reinterpret_f32)
            (func $__double_value (param i64) (result f64) local.get 0 f64.reinterpret_i64)
            (func $__float_value (param i32) (result f32) local.get 0 f32.reinterpret_i32)
            """);
        if (exceptionRuntime) combinedWat.append(BytecodeObjects.runtimeWat(linked,runtimeDispatch)).append(BytecodeClasses.runtimeWat(linked,runtimeDispatch));
        combinedWat.append(BytecodeStrings.runtimeWat(linked, runtimeDispatch));
        combinedWat.append(BytecodeTraces.runtimeWat(linked, runtimeDispatch));
        for (ClassNode cn : classes) {
            ClassBytecodeCompiler compiler = new ClassBytecodeCompiler(cn);
            String classWat = compiler.compileToWat(linked, false);
            // Strip outer (module ... )
            int start = classWat.indexOf("(import \"env\" \"__heap_alloc\" (func $__heap_alloc (param i32) (result i32)))");
            if (start != -1) {
                start += "(import \"env\" \"__heap_alloc\" (func $__heap_alloc (param i32) (result i32)))\n".length();
            } else {
                start = classWat.indexOf("(module\n") + "(module\n".length();
            }
            int end = classWat.lastIndexOf(")");
            if (start < end) {
                combinedWat.append(classWat.substring(start, end));
            }
        }

        if (classes.stream().anyMatch(BytecodeArrays::usesRuntime))
            combinedWat.append(new BytecodeDispatch(linked).referenceArrayRuntimeWat())
                    .append(new BytecodeDispatch(linked).multiArrayRuntimeWat());
        combinedWat.append(")\n");
        return combinedWat.toString();
    }

    public static String compileJar(File jarFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(jarFile)) {
            return compileJar(fis);
        }
    }

    public static void main(String[] args) {
        boolean exceptionRuntime=false;List<List<String>> proxyShapes=new ArrayList<>();int option=0;
        while(option<args.length) {
            if(args[option].equals("--exception-runtime"))exceptionRuntime=true;
            else if(args[option].startsWith("--aot-proxy="))proxyShapes.add(List.of(args[option].substring("--aot-proxy=".length()).split(",",-1)));
            else break;
            option++;
        }
        args=Arrays.copyOfRange(args,option,args.length);
        if (args.length == 0 || "--help".equals(args[0]) || "-h".equals(args[0])) {
            System.out.println("Usage: java com.example.javaw.bytecode.BytecodeCompiler [--exception-runtime] [--aot-proxy=Interface,Other] <jar-or-class-file> [output-wat-file]");
            return;
        }
        try {
            if(!exceptionRuntime && !proxyShapes.isEmpty())throw new IllegalArgumentException("AOT proxies require --exception-runtime");
            String path = args[0];
            if (path.startsWith("~" + File.separator) || path.equals("~")) {
                path = System.getProperty("user.home") + path.substring(1);
            }
            File file = new File(path);
            if (!file.exists()) {
                System.err.println("File not found: " + file.getAbsolutePath());
                System.exit(1);
            }
            String wat;
            if (file.getName().endsWith(".jar")) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    wat = compileJar(fis, exceptionRuntime,proxyShapes);
                }
            } else if (file.getName().endsWith(".class")) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    wat = exceptionRuntime ? compileClassWithExceptionRuntime(fis.readAllBytes(),proxyShapes) : compileClass(fis);
                }
            } else {
                System.err.println("Unsupported file type: " + file.getName());
                System.exit(1);
                return;
            }
            if (args.length > 1) {
                String outPath = args[1];
                if (outPath.startsWith("~" + File.separator) || outPath.equals("~")) {
                    outPath = System.getProperty("user.home") + outPath.substring(1);
                }
                File outFile = new File(outPath);
                java.nio.file.Files.write(outFile.toPath(), wat.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                System.out.println("Wrote compiled WAT to " + outFile.getAbsolutePath());
            } else {
                System.out.println(wat);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
