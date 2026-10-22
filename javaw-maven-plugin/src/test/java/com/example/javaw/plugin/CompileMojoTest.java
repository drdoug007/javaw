package com.example.javaw.plugin;

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
        Files.writeString(sampleJava, "package com.example.sample;\n" +
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

    private static void setField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }
}
