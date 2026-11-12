package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.*;

import javax.tools.ToolProvider;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;
import java.util.jar.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JAR entry order has no JVM meaning, so {@link BytecodeCompiler} links JAR classes in class-name, then entry-name
 * order. These checks compile one fixture in several entry orders and require identical output, which is why the
 * executable tests compile each fixture JAR in a single order.
 */
class BytecodeJarOrderTest implements Opcodes {
    @TempDir Path directory;

    private List<Path> fixture() throws Exception {
        Path source = directory.resolve("src"), classes = directory.resolve("classes");
        Files.createDirectories(source.resolve("pkg"));
        Files.writeString(source.resolve("Main.java"), """
            public class Main {
                interface Shape { int area(); }
                static final class Square implements Shape { final int side; Square(int side) { this.side = side; } public int area() { return side * side; } }
                public static int run(int n) {
                    Shape twice = new Shape() { public int area() { return 2 * n; } };
                    return new Square(n).area() + twice.area() + pkg.Helper.seed() + pkg.Helper.twice(n);
                }
            }
            """);
        Files.writeString(source.resolve("pkg/Helper.java"), """
            package pkg;
            public class Helper { public static int seed() { return 7; } public static int twice(int n) { return n + n; } }
            """);
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, "-g", "--release", "17", "-d",
                classes.toString(), source.resolve("Main.java").toString(), source.resolve("pkg/Helper.java").toString()));
        try (var paths = Files.walk(classes)) {
            List<Path> files = new ArrayList<>(paths.filter(path -> path.toString().endsWith(".class")).sorted().toList());
            assertEquals(5, files.size(), files.toString()); // Main, Main$1, Main$Shape, Main$Square, pkg/Helper
            return files;
        }
    }

    private byte[] jar(List<Path> files) throws IOException {
        Path classes = directory.resolve("classes");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JarOutputStream jar = new JarOutputStream(bytes)) {
            for (Path file : files) {
                jar.putNextEntry(new JarEntry(classes.relativize(file).toString().replace(File.separatorChar, '/')));
                jar.write(Files.readAllBytes(file)); jar.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    /** Path order (nested classes before their outer class), reversed, and two rotations. */
    private static List<List<Path>> orders(List<Path> files) {
        List<List<Path>> orders = new ArrayList<>();
        orders.add(files);
        List<Path> reversed = new ArrayList<>(files); Collections.reverse(reversed); orders.add(reversed);
        for (int distance : new int[]{2, 3}) { List<Path> rotated = new ArrayList<>(files); Collections.rotate(rotated, distance); orders.add(rotated); }
        return orders;
    }

    private void assertOrderIndependent(Function<byte[], String> compile) throws Exception {
        List<Path> files = fixture();
        String first = null;
        for (List<Path> order : orders(files)) {
            String wat = compile.apply(jar(order));
            if (first == null) first = wat;
            else assertEquals(first, wat, "WAT differs for entry order " + order.stream().map(Path::getFileName).toList());
        }
        assertTrue(first.contains("Main.run"), "fixture export missing");
    }

    private static String unchecked(IOExceptionCall call) {
        try { return call.get(); } catch (IOException error) { throw new UncheckedIOException(error); }
    }
    private interface IOExceptionCall { String get() throws IOException; }

    @Test void linksJarClassesIdenticallyInEveryEntryOrderWithoutTheRuntime() throws Exception {
        assertOrderIndependent(jar -> unchecked(() -> BytecodeCompiler.compileJar(new ByteArrayInputStream(jar))));
    }

    @Test void linksJarClassesIdenticallyInEveryEntryOrderWithTheBundledRuntime() throws Exception {
        assertOrderIndependent(jar -> unchecked(() -> BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar))));
    }

    private static byte[] duplicate(String sourceFile) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(V17, ACC_PUBLIC | ACC_SUPER, "Duplicate", null, "java/lang/Object", null);
        writer.visitSource(sourceFile, null);
        MethodVisitor run = writer.visitMethod(ACC_PUBLIC | ACC_STATIC, "run", "()I", null, null);
        run.visitCode(); run.visitInsn(ICONST_1); run.visitInsn(IRETURN); run.visitMaxs(0, 0); run.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    @Test void namesTheSameDuplicateClassDeclarationInEitherEntryOrder() throws Exception {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("a/Duplicate.class", duplicate("FirstCopy.java"));
        entries.put("b/Duplicate.class", duplicate("SecondCopy.java"));
        List<String> names = new ArrayList<>(entries.keySet());
        for (boolean reverse : new boolean[]{false, true}) {
            if (reverse) Collections.reverse(names);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (JarOutputStream jar = new JarOutputStream(bytes)) {
                for (String name : names) { jar.putNextEntry(new JarEntry(name)); jar.write(entries.get(name)); jar.closeEntry(); }
            }
            String message = assertThrows(IllegalArgumentException.class,
                    () -> BytecodeCompiler.compileJar(new ByteArrayInputStream(bytes.toByteArray()))).getMessage();
            // Entries sort by class name, then entry name, so b/Duplicate.class is always the redefinition.
            assertTrue(message.startsWith("SecondCopy.java:"), message);
            assertTrue(message.contains("duplicate class declaration"), message);
        }
    }
}
