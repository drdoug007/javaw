package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A Multi-Release JAR is linked as a Java {@value BytecodeCompiler#RELEASE} runtime reads it, which is also how
 * DependencyAudit selects entries: each class from its highest {@code META-INF/versions/N} copy with
 * 9 <= N <= 17, else the base entry. Module descriptors are skipped at any version, and a JAR without
 * {@code Multi-Release: true} ignores its versioned entries.
 */
class BytecodeMultiReleaseJarTest implements Opcodes {
    @TempDir Path directory;

    /** {@code Pick.run()} returning {@code value}. */
    private static byte[] pick(int value) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        writer.visit(V17, ACC_PUBLIC | ACC_SUPER, "Pick", null, "java/lang/Object", null);
        MethodVisitor run = writer.visitMethod(ACC_PUBLIC | ACC_STATIC, "run", "()I", null, null);
        run.visitCode(); run.visitIntInsn(SIPUSH, value); run.visitInsn(IRETURN); run.visitMaxs(0, 0); run.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] moduleInfo() {
        ClassWriter writer = new ClassWriter(0);
        writer.visit(V17, ACC_MODULE, "module-info", null, null, null);
        writer.visitModule("pick", 0, null).visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static byte[] jar(boolean multiRelease, Map<String, byte[]> entries) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        if (multiRelease) manifest.getMainAttributes().put(new Attributes.Name("Multi-Release"), "true");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JarOutputStream jar = new JarOutputStream(bytes, manifest)) {
            for (var entry : entries.entrySet()) { jar.putNextEntry(new JarEntry(entry.getKey())); jar.write(entry.getValue()); jar.closeEntry(); }
        }
        return bytes.toByteArray();
    }

    private static Map<String, byte[]> versions() {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        // Higher versions first, so stream order cannot be what selects the copy.
        entries.put("META-INF/versions/21/Pick.class", pick(21));
        entries.put("META-INF/versions/17/Pick.class", pick(17));
        entries.put("META-INF/versions/11/Pick.class", pick(11));
        entries.put("META-INF/versions/9/module-info.class", moduleInfo());
        entries.put("module-info.class", moduleInfo());
        entries.put("META-INF/versions/x/Pick.class", pick(99));
        entries.put("Pick.class", pick(1));
        return entries;
    }

    private static int returned(List<org.objectweb.asm.tree.ClassNode> classes) {
        assertEquals(1, classes.size(), classes.stream().map(c -> c.name).toList().toString());
        var instruction = classes.get(0).methods.stream().filter(m -> m.name.equals("run")).findFirst().orElseThrow().instructions.getFirst();
        return ((org.objectweb.asm.tree.IntInsnNode) instruction).operand;
    }

    @Test void selectsTheJava17CopyAndSkipsModuleDescriptors() throws Exception {
        byte[] jar = jar(true, versions());
        assertEquals(17, returned(BytecodeCompiler.readJar(new ByteArrayInputStream(jar))));
        String wat = BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar));
        WasmTestSupport.execute(directory, wat, "assert.equal(e['Pick.run'](),17);");
    }

    @Test void findsAManifestThatIsNotTheFirstEntry() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(bytes)) {
            for (var entry : versions().entrySet()) { zip.putNextEntry(new java.util.zip.ZipEntry(entry.getKey())); zip.write(entry.getValue()); zip.closeEntry(); }
            zip.putNextEntry(new java.util.zip.ZipEntry("META-INF/MANIFEST.MF"));
            zip.write("Manifest-Version: 1.0\r\nMulti-Release: true\r\n\r\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        assertEquals(17, returned(BytecodeCompiler.readJar(new ByteArrayInputStream(bytes.toByteArray()))));
    }

    @Test void acceptsOnlyCanonicalVersionDirectories() throws Exception {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        for (String version : List.of("011", "+12", "13x", "1_4"))
            entries.put("META-INF/versions/" + version + "/Pick.class", pick(Integer.parseInt(version.replaceAll("\\D", ""))));
        entries.put("Pick.class", pick(1));
        assertEquals(1, returned(BytecodeCompiler.readJar(new ByteArrayInputStream(jar(true, entries)))));
        assertEquals(16, BytecodeCompiler.versionDirectory("META-INF/versions/16/a/B.class"));
        for (String name : List.of("META-INF/versions/016/B.class", "META-INF/versions/16", "META-INF/versions/16/", "META-INF/versions/٣/B.class"))
            assertEquals(-1, BytecodeCompiler.versionDirectory(name), name);
    }

    @Test void ignoresVersionedEntriesWithoutTheManifestAttribute() throws Exception {
        assertEquals(1, returned(BytecodeCompiler.readJar(new ByteArrayInputStream(jar(false, versions())))));
    }

    @Test void fallsBackToTheBaseEntryWhenOnlyNewerVersionsExist() throws Exception {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("META-INF/versions/21/Pick.class", pick(21));
        entries.put("Pick.class", pick(1));
        assertEquals(1, returned(BytecodeCompiler.readJar(new ByteArrayInputStream(jar(true, entries)))));
    }

    @Test void duplicatesAtTheSelectedVersionStillReachTheDuplicateCheck() throws Exception {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("META-INF/versions/17/Pick.class", pick(17));
        entries.put("META-INF/versions/17/other/Pick.class", pick(18)); // a different path declaring the same class
        entries.put("Pick.class", pick(1));
        var failure = assertThrows(IllegalArgumentException.class,
                () -> BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(true, entries))));
        assertTrue(failure.getMessage().contains("duplicate class declaration"), failure.getMessage());
    }
}
