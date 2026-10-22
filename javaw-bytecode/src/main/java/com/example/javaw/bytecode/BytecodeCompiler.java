package com.example.javaw.bytecode;

import org.objectweb.asm.ClassReader;
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
                ClassReader cr = new ClassReader(baos.toByteArray());
                ClassNode cn = new ClassNode();
                cr.accept(cn, 0);
                classes.add(cn);
            }
        }

        StringBuilder combinedWat = new StringBuilder();
        combinedWat.append("(module\n");
        combinedWat.append("    (import \"env\" \"memory\" (memory 2 20))\n");
        combinedWat.append("    (import \"env\" \"__heap_alloc\" (func $__heap_alloc (param i32) (result i32)))\n");

        for (ClassNode cn : classes) {
            ClassBytecodeCompiler compiler = new ClassBytecodeCompiler(cn);
            String classWat = compiler.compileToWat();
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

        combinedWat.append(")\n");
        return combinedWat.toString();
    }

    public static String compileJar(File jarFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(jarFile)) {
            return compileJar(fis);
        }
    }
}
