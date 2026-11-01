package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.*;
import javax.tools.ToolProvider;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.jar.*;

import static org.junit.jupiter.api.Assertions.*;

class BytecodeReachabilityTest implements Opcodes {
    @TempDir Path directory;

    private static final String SOURCE = """
            public class Reach {
                interface Shape { int area(); }
                static final class Square implements Shape { public int area() { return 4; } }
                static final class Circle implements Shape { public int area() { return 3; } }
                static int total(Shape shape) { return shape.area(); }
                public static int shapes() { return total(new Square()) * 10 + total(new Circle()); }
                // Bundled-runtime receivers are not exported. LinkedList is instantiated only after count() is first
                // lowered, which widens its dispatch; TreeSet is never instantiated.
                static int count(java.util.Collection<?> values) { return values.size(); }
                static int later() { java.util.LinkedList<String> values = new java.util.LinkedList<>(); values.add("x"); values.add("y"); return count(values); }
                public static int collections() { java.util.ArrayList<String> values = new java.util.ArrayList<>(); values.add("x"); return count(values) * 10 + later(); }
                // String exists only through a runtime text constant, never through NEW.
                public static String text() { Object value = "abc"; return value.toString() + value.hashCode(); }
                // The NullPointerException is allocated by an implicit runtime helper.
                public static String implicit() {
                    try { Object value = null; return "none" + value.hashCode(); }
                    catch (NullPointerException error) { return error.getClass().getName() + ":" + error.getStackTrace()[0].getMethodName(); }
                }
            }
            """;

    @Test void lowersOnlyInstantiatedReceiversAndMatchesTheJvm() throws Exception {
        byte[] jar = compile();
        String wat = BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar));
        assertTrue(wat.contains("(func " + TypeHelper.getMethodFunctionName("java/util/LinkedList", "size", "()I")), "late-instantiated receiver is dispatched");
        assertFalse(wat.contains(TypeHelper.getMethodFunctionName("java/util/TreeSet", "size", "()I")), "uninstantiated receiver is not lowered");
        assertFalse(wat.contains(TypeHelper.getGlobalFieldName("java/util/TreeSet", "PRESENT", "Ljava/lang/Object;")), "unreferenced static state is pruned");
        assertTrue(wat.contains("(export \"Reach.shapes\")"));
        try (URLClassLoader loader = new URLClassLoader(new URL[]{directory.toUri().toURL()}, null)) {
            Class<?> type = loader.loadClass("Reach");
            StringBuilder assertions = new StringBuilder();
            for (String name : List.of("shapes", "collections"))
                assertions.append("assert.equal(e['Reach.").append(name).append("'](),").append(type.getMethod(name).invoke(null)).append(");\n");
            for (String name : List.of("text", "implicit"))
                assertions.append("assert.equal(text(e['Reach.").append(name).append("']()),").append(js((String) type.getMethod(name).invoke(null))).append(");\n");
            execute(wat, assertions.toString());
        }
    }

    @Test void stillRejectsUnusedMethodsWithInvalidOperandStacks() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(V17, ACC_PUBLIC, "UnusedStack", null, "java/lang/Object", null);
        writer.visitSource("UnusedStack.java", null);
        MethodVisitor used = writer.visitMethod(ACC_PUBLIC | ACC_STATIC, "run", "()I", null, null);
        used.visitCode(); used.visitInsn(ICONST_1); used.visitInsn(IRETURN); used.visitMaxs(1, 0); used.visitEnd();
        MethodVisitor unused = writer.visitMethod(ACC_PRIVATE | ACC_STATIC, "unused", "()V", null, null);
        unused.visitCode(); Label line = new Label(); unused.visitLabel(line); unused.visitLineNumber(17, line);
        unused.visitInsn(POP); unused.visitInsn(RETURN); unused.visitMaxs(1, 0); unused.visitEnd();
        writer.visitEnd();
        var failure = assertThrows(IllegalArgumentException.class, () -> BytecodeCompiler.compileClassWithExceptionRuntime(writer.toByteArray()));
        assertTrue(failure.getMessage().contains("UnusedStack.java:17") && failure.getMessage().contains("invalid operand stack"), failure.getMessage());
    }

    private byte[] compile() throws Exception {
        Path file = directory.resolve("Reach.java");
        Files.writeString(file, SOURCE);
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, "-g", "--release", "17", "-d", directory.toString(), file.toString()));
        List<Path> files;
        try (var paths = Files.walk(directory)) { files = paths.filter(path -> path.toString().endsWith(".class")).sorted().toList(); }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JarOutputStream jar = new JarOutputStream(bytes)) {
            for (Path path : files) { jar.putNextEntry(new JarEntry(directory.relativize(path).toString())); jar.write(Files.readAllBytes(path)); jar.closeEntry(); }
        }
        return bytes.toByteArray();
    }

    private String js(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : value.toCharArray()) out.append(String.format("\\u%04x", (int) c));
        return out.append('"').toString();
    }

    private void run(String... arguments) throws Exception {
        Path log = directory.resolve("command.log");
        Process process = new ProcessBuilder(arguments).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if (!process.waitFor(60, TimeUnit.SECONDS)) { process.destroyForcibly(); fail("Command timed out"); }
        assertEquals(0, process.exitValue(), Files.readString(log));
    }

    private void execute(String wat, String assertions) throws Exception {
        Path input = directory.resolve("reach.wat"), output = directory.resolve("reach.wasm"), script = directory.resolve("run.cjs");
        Files.writeString(input, wat);
        run("wat2wasm", "--enable-exceptions", input.toString(), "-o", output.toString());
        Files.writeString(script, """
            const fs=require('node:fs'),assert=require('node:assert/strict');
            const memory=new WebAssembly.Memory({initial:2});let heap=1024;
            const e=new WebAssembly.Instance(new WebAssembly.Module(fs.readFileSync(process.argv[2])),{env:{memory,__heap_alloc:size=>{
                const address=heap;heap+=size;while(heap>memory.buffer.byteLength)memory.grow(1);return address;}}}).exports;
            heap=Math.max(heap,e.__exception_heap_base?.value??0);
            function text(pointer){if(!pointer)return null;const view=new DataView(memory.buffer);
                const chars=view.getInt32(pointer+4,true),length=view.getInt32(chars+4,true);
                let result='';for(let i=0;i<length;i++)result+=String.fromCharCode(view.getUint16(chars+8+2*i,true));return result;}
            """ + assertions);
        run("node", script.toString(), output.toString());
    }
}
