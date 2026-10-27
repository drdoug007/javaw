package one.dastec.javaw.plugin;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CompileMojoTest {

    @Test
    public void testMojoExecutionOnSampleJavaSource() throws Exception {
        Path tempSrc = Files.createTempDirectory("javaw_mojo_src");
        Path tempOut = Files.createTempDirectory("javaw_mojo_out");

        Path sampleJava = tempSrc.resolve("SampleMojoApp.java");
        Files.writeString(sampleJava, "package one.dastec.sample;\n" +
                "public class SampleMojoApp {\n" +
                "    public static int add(int a, int b) { return a + b; }\n" +
                "}\n");

        CompileMojo mojo = new CompileMojo();

        setField(mojo, "sourceDirectory", tempSrc.toFile());
        setField(mojo, "outputDirectory", tempOut.toFile());
        setField(mojo, "optimizationLevel", "-O3");
        setField(mojo, "generateSourceMaps", true);
        setField(mojo, "enableThreads", true);
        setField(mojo, "copyRuntimeScript", true);
        setField(mojo, "batchMode", false);
        setField(mojo, "skip", false);

        mojo.execute();

        Path watFile = tempOut.resolve("SampleMojoApp.wat");
        assertTrue(Files.exists(watFile), "SampleMojoApp.wat should be generated");
        String watContent = Files.readString(watFile);
        assertTrue(watContent.contains("(func $add (export \"add\")"), "WAT must contain add function");

        Path mapFile = tempOut.resolve("SampleMojoApp.wasm.map");
        assertTrue(Files.exists(mapFile), "Source map should be generated");

        Path runtimeFile = tempOut.resolve("javaw-runtime.js");
        assertTrue(Files.exists(runtimeFile), "javaw-runtime.js should be copied");
    }

    @Test
    public void testMojoExecutionWithDependencyLinking() throws Exception {
        Path tempSrc = Files.createTempDirectory("javaw_mojo_src_dep");
        Path tempOut = Files.createTempDirectory("javaw_mojo_out_dep");
        Path tempDepJar = Files.createTempFile("dep-calc", ".jar");

        // Create dependency JAR with calculator class
        org.objectweb.asm.ClassWriter cw = new org.objectweb.asm.ClassWriter(org.objectweb.asm.ClassWriter.COMPUTE_FRAMES | org.objectweb.asm.ClassWriter.COMPUTE_MAXS);
        cw.visit(org.objectweb.asm.Opcodes.V17, org.objectweb.asm.Opcodes.ACC_PUBLIC, "one/dastec/math/Calculator", null, "java/lang/Object", null);
        org.objectweb.asm.MethodVisitor mv = cw.visitMethod(org.objectweb.asm.Opcodes.ACC_PUBLIC | org.objectweb.asm.Opcodes.ACC_STATIC, "square", "(I)I", null, null);
        mv.visitCode();
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ILOAD, 0);
        mv.visitVarInsn(org.objectweb.asm.Opcodes.ILOAD, 0);
        mv.visitInsn(org.objectweb.asm.Opcodes.IMUL);
        mv.visitInsn(org.objectweb.asm.Opcodes.IRETURN);
        mv.visitMaxs(2, 1);
        mv.visitEnd();
        cw.visitEnd();

        try (java.util.jar.JarOutputStream jos = new java.util.jar.JarOutputStream(Files.newOutputStream(tempDepJar))) {
            jos.putNextEntry(new java.util.jar.JarEntry("one/dastec/math/Calculator.class"));
            jos.write(cw.toByteArray());
            jos.closeEntry();
        }

        Path sampleJava = tempSrc.resolve("MainApp.java");
        Files.writeString(sampleJava, "package one.dastec.app;\n" +
                "public class MainApp {\n" +
                "    public static int run(int x) { return x + 1; }\n" +
                "}\n");

        CompileMojo mojo = new CompileMojo();
        org.apache.maven.project.MavenProject mockProject = new org.apache.maven.project.MavenProject();
        mockProject.setArtifactId("demo-app");

        org.apache.maven.artifact.DefaultArtifact depArtifact = new org.apache.maven.artifact.DefaultArtifact(
                "one.dastec.math", "math-calc", "1.0.0", "compile", "jar", null,
                new org.apache.maven.artifact.handler.DefaultArtifactHandler("jar")
        );
        depArtifact.setFile(tempDepJar.toFile());
        mockProject.setArtifacts(java.util.Collections.singleton(depArtifact));

        setField(mojo, "project", mockProject);
        setField(mojo, "sourceDirectory", tempSrc.toFile());
        setField(mojo, "outputDirectory", tempOut.toFile());
        setField(mojo, "optimizationLevel", "-O3");
        setField(mojo, "generateSourceMaps", false);
        setField(mojo, "enableThreads", true);
        setField(mojo, "copyRuntimeScript", true);
        setField(mojo, "batchMode", false);
        setField(mojo, "skip", false);

        mojo.execute();

        Path watFile = tempOut.resolve("MainApp.wat");
        assertTrue(Files.exists(watFile), "MainApp.wat should be generated");
        String watContent = Files.readString(watFile);
        assertTrue(watContent.contains("(export \"one.dastec.math.Calculator.square\")"), "Linked WAT must contain dependency function");
        assertTrue(watContent.contains("$run"), "Linked WAT must contain application function");
    }

    private static void setField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}
