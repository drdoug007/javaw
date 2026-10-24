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

    public static String compileJar(InputStream jarInputStream) throws IOException {
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

        StringBuilder combinedWat = new StringBuilder();
        combinedWat.append("(module\n");
        combinedWat.append("    (import \"env\" \"memory\" (memory 2 20))\n");
        combinedWat.append("    (import \"env\" \"__heap_alloc\" (func $__heap_alloc (param i32) (result i32)))\n");

        if (classes.stream().anyMatch(ClassBytecodeCompiler::usesFloatingRemainder))
            combinedWat.append(FloatingPointSupport.remainderFunctionsWat());

        if (classes.stream().anyMatch(BytecodeArrays::usesRuntime))
            combinedWat.append(BytecodeArrays.runtimeWat());

        Map<String, ClassNode> linked = new LinkedHashMap<>();
        for (ClassNode cn : classes) {
            if (linked.putIfAbsent(cn.name, cn) != null)
                throw BytecodeCompatibility.failure(cn, null, null, "duplicate class declaration",
                        "provide exactly one definition of " + cn.name + " in the linked JAR");
        }
        BytecodeDispatch runtimeDispatch = new BytecodeDispatch(linked);
        boolean arithmetic = runtimeDispatch.hasArithmeticException() && classes.stream().anyMatch(BytecodeExceptions::usesArithmetic);
        if (arithmetic || classes.stream().anyMatch(BytecodeExceptions::usesRuntime)) combinedWat.append(BytecodeExceptions.TAG_WAT);
        if (arithmetic) combinedWat.append(runtimeDispatch.arithmeticExceptionRuntimeWat());
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
        if (args.length == 0 || "--help".equals(args[0]) || "-h".equals(args[0])) {
            System.out.println("Usage: java com.example.javaw.bytecode.BytecodeCompiler <jar-or-class-file> [output-wat-file]");
            return;
        }
        try {
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
                wat = compileJar(file);
            } else if (file.getName().endsWith(".class")) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    wat = compileClass(fis);
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
