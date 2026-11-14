package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

import static org.junit.jupiter.api.Assertions.*;

/** The Phase 7 exit-gate harness: blocker attribution, dependent exclusion, and the measured admitted subset. */
class BytecodeExitGateTest implements Opcodes {
    @TempDir Path directory;

    private static byte[] type(String name, String superName, String body) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(V17, ACC_PUBLIC | ACC_SUPER, name, null, superName, null);
        writer.visitSource(name + ".java", null);
        MethodVisitor init = writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode(); init.visitVarInsn(ALOAD, 0); init.visitMethodInsn(INVOKESPECIAL, superName, "<init>", "()V", false);
        init.visitInsn(RETURN); init.visitMaxs(0, 0); init.visitEnd();
        MethodVisitor run = writer.visitMethod(ACC_PUBLIC | ACC_STATIC, "run", "()I", null, null);
        run.visitCode();
        if (body != null) { run.visitTypeInsn(NEW, body); run.visitInsn(DUP); run.visitMethodInsn(INVOKESPECIAL, body, "<init>", "()V", false); run.visitInsn(POP); }
        run.visitInsn(ICONST_5); run.visitInsn(IRETURN); run.visitMaxs(0, 0); run.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    @Test void excludesTheBlockerAndItsDependentsThenMeasuresTheRest() throws Exception {
        Path jar = directory.resolve("library.jar"), output = directory.resolve("gate");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
            // Blocked extends a JDK type the bundled runtime does not link; User refers to it; Free is independent.
            for (var entry : Map.of("lib/Blocked.class", type("lib/Blocked", "java/net/Socket", null),
                    "lib/User.class", type("lib/User", "java/lang/Object", "lib/Blocked"),
                    "lib/Free.class", type("lib/Free", "java/lang/Object", null)).entrySet()) {
                out.putNextEntry(new JarEntry(entry.getKey())); out.write(entry.getValue()); out.closeEntry();
            }
        }
        assertTrue(BytecodeExitGate.run(jar, output, new BytecodeExitGate.Budget(600, 64 * 1024, 1024, 1024)));
        String report = Files.readString(output.resolve("report.md")), blockers = Files.readString(output.resolve("blockers.tsv"));
        assertTrue(report.contains("JAR classes (after Multi-Release selection for Java 17): 3. Root blockers: 1. Excluded dependents: 1. Admitted: 1."), report);
        assertTrue(report.contains("`lib/Blocked`") && report.contains("java/net/Socket"), report);
        assertTrue(blockers.contains("lib/User\tlib/Blocked\t"), blockers);
        assertTrue(Files.size(output.resolve("module.wasm")) > 0);
        assertTrue(Files.readString(output.resolve("module.wat")).contains("(export \"lib.Free.run\")"));
        // A budget the measurement exceeds fails the gate.
        assertFalse(BytecodeExitGate.run(jar, directory.resolve("tight"), new BytecodeExitGate.Budget(600, 64 * 1024, 0, 1024)));
    }

    @Test void failsWhenNothingIsLeftToMeasure() throws Exception {
        Path jar = directory.resolve("blocked.jar"), output = directory.resolve("none");
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar))) {
            out.putNextEntry(new JarEntry("lib/Blocked.class")); out.write(type("lib/Blocked", "java/net/Socket", null)); out.closeEntry();
        }
        assertFalse(BytecodeExitGate.run(jar, output, null));
        assertTrue(Files.readString(output.resolve("report.md")).contains("Nothing was measured"));
    }

    @Test void attributesGeneratedCompanionsToTheirClass() {
        ClassNode free = new ClassNode(); free.name = "lib/Free"; free.superName = "java/lang/Object";
        Map<String, ClassNode> jar = Map.of("lib/Free", free);
        assertEquals(Set.of("lib/Free"), BytecodeExitGate.attribute(
                "Free.java:line unavailable: lib.Free$$javaw$reflection.methods()[Ljava/lang/reflect/Method;: unsupported dynamic bootstrap; link x/Y",
                jar, Set.of()));
        // A class named only inside a descriptor is still found.
        assertEquals(Set.of("lib/Free"), BytecodeExitGate.attribute(
                "X.class:line unavailable: javaw.runtime.X.m(Llib/Free;)V: unsupported method handle javaw/runtime/X.m([Llib/Free;)V; link it",
                jar, Set.of()));
        // java/lang/Object is supplied by the compiler, so naming it blames no class.
        assertEquals(Set.of(), BytecodeExitGate.attribute(
                "X.class:line unavailable: javaw.runtime.X.m()V: unsupported method handle java/lang/Object.getClass()Ljava/lang/Class;; link y",
                jar, Set.of("java/lang/Class")));
    }
}
