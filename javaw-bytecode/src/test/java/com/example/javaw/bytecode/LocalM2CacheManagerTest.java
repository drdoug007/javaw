package com.example.javaw.bytecode;

import com.example.javaw.bytecode.cache.CacheMetadata;
import com.example.javaw.bytecode.cache.JarHashUtil;
import com.example.javaw.bytecode.cache.LocalM2CacheManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class LocalM2CacheManagerTest {

    @TempDir
    Path tempDir;

    private File createSampleJar(File jarFile, int constantValue) throws IOException {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "com/example/util/Helper", null, "java/lang/Object", null);

        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "getValue", "()I", null, null);
        mv.visitCode();
        mv.visitLdcInsn(constantValue);
        mv.visitInsn(Opcodes.IRETURN);
        mv.visitMaxs(1, 0);
        mv.visitEnd();
        cw.visitEnd();

        byte[] classBytes = cw.toByteArray();

        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile))) {
            JarEntry entry = new JarEntry("com/example/util/Helper.class");
            jos.putNextEntry(entry);
            jos.write(classBytes);
            jos.closeEntry();
        }

        return jarFile;
    }

    @Test
    public void testCacheMissHitAndInvalidation() throws Exception {
        File repoRoot = tempDir.resolve("m2-repo").toFile();
        LocalM2CacheManager cacheManager = new LocalM2CacheManager(repoRoot);

        File jarFile = tempDir.resolve("helper-1.0.0.jar").toFile();
        createSampleJar(jarFile, 42);

        String groupId = "com.example.util";
        String artifactId = "helper";
        String version = "1.0.0";

        // 1. Initial state: cache miss
        assertFalse(cacheManager.isCacheValid(groupId, artifactId, version, jarFile));

        // 2. Compile and save to cache
        CacheMetadata meta1 = cacheManager.getOrCompile(groupId, artifactId, version, jarFile);
        assertNotNull(meta1);
        assertEquals(groupId, meta1.getGroupId());
        assertEquals(artifactId, meta1.getArtifactId());
        assertEquals(version, meta1.getVersion());

        File watFile = cacheManager.getCachedWatFile(groupId, artifactId, version);
        File jsonFile = cacheManager.getCachedMetadataFile(groupId, artifactId, version);
        assertTrue(watFile.exists());
        assertTrue(jsonFile.exists());

        // 3. Second run: cache hit
        assertTrue(cacheManager.isCacheValid(groupId, artifactId, version, jarFile));
        CacheMetadata meta2 = cacheManager.getOrCompile(groupId, artifactId, version, jarFile);
        assertNotNull(meta2);
        assertEquals(meta1.getJarSha256(), meta2.getJarSha256());

        // Compiler semantics must invalidate otherwise identical artifact bytes.
        meta2.setCompilerVersion("1.0-SNAPSHOT-class-access-v11");
        java.nio.file.Files.writeString(jsonFile.toPath(), meta2.toJson());
        assertFalse(cacheManager.isCacheValid(groupId, artifactId, version, jarFile));
        cacheManager.getOrCompile(groupId, artifactId, version, jarFile);
        assertTrue(cacheManager.isCacheValid(groupId, artifactId, version, jarFile));

        // 4. Invalidation: Modify JAR content
        createSampleJar(jarFile, 100); // changes SHA-256
        assertFalse(cacheManager.isCacheValid(groupId, artifactId, version, jarFile));

        // 5. Recompilation on mismatch
        CacheMetadata meta3 = cacheManager.getOrCompile(groupId, artifactId, version, jarFile);
        assertNotNull(meta3);
        assertNotEquals(meta1.getJarSha256(), meta3.getJarSha256());
        assertEquals(JarHashUtil.computeSha256(jarFile), meta3.getJarSha256());
    }
}
