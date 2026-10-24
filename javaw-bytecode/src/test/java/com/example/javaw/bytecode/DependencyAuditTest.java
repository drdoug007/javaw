package com.example.javaw.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.*;

import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

import static org.junit.jupiter.api.Assertions.*;

class DependencyAuditTest {
    @TempDir Path directory;

    private byte[] simple(String name, String dependency) {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, name, null, "java/lang/Object", null);
        writer.visitField(Opcodes.ACC_PUBLIC, "value", "L" + dependency + ";", null, null).visitEnd();
        writer.visitEnd(); return writer.toByteArray();
    }

    private byte[] root() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "Root", null, "java/lang/Object", null);
        writer.visitField(Opcodes.ACC_VOLATILE, "state", "I", null, null).visitEnd();
        writer.visitField(0, "generic", "Lexternal/Outer$Inner;",
                "Lexternal/Outer<Ljava/lang/String;>.Inner<Ljava/lang/Integer;>;", null).visitEnd();
        writer.visitAnnotation("Lexternal/Marker;", true).visitEnd();
        writer.visitMethod(Opcodes.ACC_NATIVE, "nativeCall", "()V", null, null).visitEnd();
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_STATIC | Opcodes.ACC_SYNCHRONIZED,
                "<clinit>", "()V", null, new String[]{"external/DeclaredException"});
        MethodVisitor annotated = writer.visitMethod(Opcodes.ACC_STATIC | Opcodes.ACC_NATIVE,
                "annotated", "(Ljava/lang/String;)V", null, null);
        annotated.visitParameterAnnotation(0, "Lexternal/Parameter;", true).visitEnd(); annotated.visitEnd();
        method.visitCode();
        Label start = new Label(), end = new Label(), handler = new Label();
        method.visitTryCatchBlock(start, end, handler, "external/CaughtException");
        method.visitLabel(start);
        method.visitInsn(Opcodes.ICONST_1); method.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_INT); method.visitInsn(Opcodes.POP);
        method.visitInsn(Opcodes.ICONST_1); method.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object"); method.visitInsn(Opcodes.POP);
        method.visitInsn(Opcodes.ICONST_1); method.visitInsn(Opcodes.ICONST_2);
        method.visitMultiANewArrayInsn("[[I", 2); method.visitInsn(Opcodes.POP);
        method.visitLdcInsn("example"); method.visitInsn(Opcodes.POP);
        method.visitLdcInsn(Type.getObjectType("Leaf")); method.visitInsn(Opcodes.POP);
        Handle bootstrap = new Handle(Opcodes.H_INVOKESTATIC, "external/Bootstrap", "link", "()Ljava/lang/invoke/CallSite;", false);
        method.visitInvokeDynamicInsn("call", "()V", bootstrap, Type.getObjectType("external/BootstrapArgument"));
        method.visitLdcInsn(new ConstantDynamic("value", "Lexternal/Dynamic;", bootstrap,
                new Handle(Opcodes.H_INVOKESTATIC, "external/Target", "work", "()V", false)));
        method.visitInsn(Opcodes.POP);
        method.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
        method.visitInsn(Opcodes.POP);
        method.visitLabel(end); method.visitInsn(Opcodes.RETURN);
        method.visitLabel(handler); method.visitInsn(Opcodes.ATHROW);
        method.visitMaxs(4, 0); method.visitEnd(); writer.visitEnd(); return writer.toByteArray();
    }

    private Path jar(String name, boolean multiRelease, Map<String, byte[]> entries) throws Exception {
        Manifest manifest = new Manifest(); manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        if (multiRelease) manifest.getMainAttributes().putValue("Multi-Release", "true");
        Path path = directory.resolve(name);
        try (var jar = new JarOutputStream(Files.newOutputStream(path), manifest)) {
            for (var entry : entries.entrySet()) {
                jar.putNextEntry(new JarEntry(entry.getKey())); jar.write(entry.getValue()); jar.closeEntry();
            }
        }
        return path;
    }

    @Test void inventoriesEntireJarAndSelectsTargetedMultiReleaseClassesDeterministically() throws Exception {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("Root.class", simple("Root", "BaseOnly"));
        entries.put("Leaf.class", simple("Leaf", "external/LeafDependency"));
        entries.put("Unused.class", simple("Unused", "external/UnusedDependency"));
        entries.put("META-INF/versions/17/Root.class", root());
        entries.put("META-INF/versions/21/Root.class", simple("Root", "FutureOnly"));
        entries.put("META-INF/services/example.Service", "Leaf # provider\n".getBytes());
        entries.put("META-INF/spring/example.imports", "Root\n".getBytes());
        Path jar = jar("fixture.jar", true, entries), first = directory.resolve("first"), second = directory.resolve("second");
        for (Path output : List.of(first, second)) {
            DependencyAudit audit = new DependencyAudit(); audit.scan("test:fixture:1", jar, 17);
            audit.write(output, List.of("Root"), 17);
        }
        try (var paths = Files.list(first)) {
            for (Path path : paths.toList()) assertEquals(Files.readString(path), Files.readString(second.resolve(path.getFileName())));
        }
        String summary = Files.readString(first.resolve("summary.tsv"));
        assertTrue(summary.contains("classDefinitions\t3\n"), summary);
        String classes = Files.readString(first.resolve("classes.tsv"));
        assertTrue(classes.contains("Leaf\ttest:fixture:1\troot-reference-closure"), classes);
        assertTrue(classes.contains("Unused\ttest:fixture:1\tnot-in-static-closure"), classes);
        String types = Files.readString(first.resolve("external-types.tsv"));
        for (String name : List.of("external/Parameter", "external/CaughtException", "external/DeclaredException",
                "external/BootstrapArgument", "external/Dynamic", "external/Target", "external/Outer$Inner"))
            assertTrue(types.contains(name + "\tRoot\ttrue"), name);
        assertFalse(types.contains("FutureOnly")); assertFalse(types.contains("BaseOnly"));
        assertTrue(types.contains("external/UnusedDependency\tUnused\tfalse"));
        String features = Files.readString(first.resolve("requirements.tsv"));
        for (String category : List.of("native", "volatile", "synchronized", "class-initializer", "exception-handler",
                "invokedynamic-bootstrap", "constant-dynamic-bootstrap", "reflection", "string-constant"))
            assertTrue(features.contains("\t" + category + "\t"), category);
        assertFalse(Files.readString(first.resolve("unsupported-opcodes.tsv")).contains("\t188\tNEWARRAY\t"));
        assertFalse(Files.readString(first.resolve("unsupported-opcodes.tsv")).contains("\t189\tANEWARRAY\t"));
        assertFalse(Files.readString(first.resolve("unsupported-opcodes.tsv")).contains("MULTIANEWARRAY"));
        assertTrue(Files.readString(first.resolve("services.tsv")).contains("META-INF/services/example.Service\tLeaf"));
        assertTrue(Files.readString(first.resolve("resources.tsv")).contains("META-INF/spring/example.imports\tspring"));
        assertFalse(features.contains(directory.toString()));
    }

    @Test void separatesAndVerifiesNormalizedSubroutinesFromUnsupportedInstructions() throws Exception {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC, "Legacy", null, "java/lang/Object", null);
        MethodVisitor method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "run", "()V", null, null);
        method.visitCode(); Label sub = new Label(); method.visitJumpInsn(Opcodes.JSR, sub);
        method.visitInsn(Opcodes.RETURN); method.visitLabel(sub); method.visitVarInsn(Opcodes.ASTORE, 0);
        method.visitVarInsn(Opcodes.RET, 0); method.visitMaxs(1, 1); method.visitEnd(); writer.visitEnd();
        DependencyAudit audit = new DependencyAudit();
        audit.scan("legacy", jar("legacy.jar", false, Map.of("Legacy.class", writer.toByteArray())), 17);
        Path output = directory.resolve("legacy-report"); audit.write(output, List.of("Legacy"), 17);
        assertTrue(Files.readString(output.resolve("normalized-opcodes.tsv")).contains("\t168\tJSR\t1\t"));
        assertTrue(Files.readString(output.resolve("normalized-opcodes.tsv")).contains("\t169\tRET\t1\t"));
        assertFalse(Files.readString(output.resolve("unsupported-opcodes.tsv")).contains("JSR"));
        assertTrue(Files.readString(output.resolve("requirements.tsv")).contains("normalized-subroutine-method"));
    }

    @Test void reportsDuplicatesAndRejectsMissingRootsWithoutTreatingNonMultiReleaseEntriesAsClasses() throws Exception {
        Path jar = jar("ordinary.jar", false, Map.of("Root.class", simple("Root", "BaseOnly"),
                "META-INF/versions/17/Root.class", simple("Root", "ShouldBeIgnored")));
        DependencyAudit audit = new DependencyAudit(); audit.scan("a", jar, 17); audit.scan("b", jar, 17);
        assertThrows(IllegalArgumentException.class, () -> audit.write(directory.resolve("bad"), List.of("Absent"), 17));
        Path output = directory.resolve("output"); audit.write(output, List.of("Root"), 17);
        assertTrue(Files.readString(output.resolve("duplicate-classes.tsv")).contains("Root\ta\tb"));
        assertFalse(Files.readString(output.resolve("external-types.tsv")).contains("ShouldBeIgnored"));
    }
}
