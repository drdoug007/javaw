package com.example.javaw.bytecode.cache;

import com.example.javaw.bytecode.BytecodeCompiler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class LocalM2CacheManager {
    private final File repositoryRoot;

    public LocalM2CacheManager() {
        this(new File(System.getProperty("user.home"), ".m2/repository"));
    }

    public LocalM2CacheManager(File repositoryRoot) {
        this.repositoryRoot = repositoryRoot;
    }

    public File getRepositoryRoot() {
        return repositoryRoot;
    }

    public File getArtifactDirectory(String groupId, String artifactId, String version) {
        String groupPath = groupId.replace('.', File.separatorChar);
        return new File(repositoryRoot, groupPath + File.separator + artifactId + File.separator + version);
    }

    public File getCachedWatFile(String groupId, String artifactId, String version) {
        File dir = getArtifactDirectory(groupId, artifactId, version);
        return new File(dir, artifactId + "-" + version + "-javaw.wat");
    }

    public File getCachedMetadataFile(String groupId, String artifactId, String version) {
        File dir = getArtifactDirectory(groupId, artifactId, version);
        return new File(dir, artifactId + "-" + version + "-javaw.json");
    }

    public boolean isCacheValid(String groupId, String artifactId, String version, File jarFile) {
        File watFile = getCachedWatFile(groupId, artifactId, version);
        File metaFile = getCachedMetadataFile(groupId, artifactId, version);

        if (!watFile.exists() || !metaFile.exists()) {
            return false;
        }

        try {
            String json = new String(Files.readAllBytes(metaFile.toPath()), StandardCharsets.UTF_8);
            CacheMetadata meta = CacheMetadata.fromJson(json);
            String currentHash = JarHashUtil.computeSha256(jarFile);
            return currentHash.equalsIgnoreCase(meta.getJarSha256());
        } catch (Exception e) {
            return false;
        }
    }

    public CacheMetadata getOrCompile(String groupId, String artifactId, String version, File jarFile) throws IOException {
        File dir = getArtifactDirectory(groupId, artifactId, version);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File watFile = getCachedWatFile(groupId, artifactId, version);
        File metaFile = getCachedMetadataFile(groupId, artifactId, version);

        if (isCacheValid(groupId, artifactId, version, jarFile)) {
            String json = new String(Files.readAllBytes(metaFile.toPath()), StandardCharsets.UTF_8);
            return CacheMetadata.fromJson(json);
        }

        // Cache miss: Compile bytecode to WAT
        String jarSha256 = JarHashUtil.computeSha256(jarFile);
        String compiledWat = BytecodeCompiler.compileJar(jarFile);

        // Write WAT
        Files.write(watFile.toPath(), compiledWat.getBytes(StandardCharsets.UTF_8));

        // Create and write metadata
        CacheMetadata meta = new CacheMetadata(groupId, artifactId, version, jarSha256);
        Files.write(metaFile.toPath(), meta.toJson().getBytes(StandardCharsets.UTF_8));

        return meta;
    }
}
