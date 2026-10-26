package dastec.one.javaw.bytecode.cache;

import dastec.one.javaw.bytecode.BytecodeCompiler;

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
            return CacheMetadata.COMPILER_VERSION.equals(meta.getCompilerVersion())
                    && currentHash.equalsIgnoreCase(meta.getJarSha256());
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

    public static void main(String[] args) {
        if (args.length == 0 || "--help".equals(args[0]) || "-h".equals(args[0])) {
            System.out.println("Usage: java dastec.one.javaw.bytecode.cache.LocalM2CacheManager <groupId:artifactId:version> [jar-file]");
            System.out.println("   or: java dastec.one.javaw.bytecode.cache.LocalM2CacheManager <groupId> <artifactId> <version> [jar-file]");
            return;
        }
        try {
            String groupId;
            String artifactId;
            String version;
            String jarPath = null;

            if (args[0].contains(":")) {
                String[] parts = args[0].split(":");
                if (parts.length < 3) {
                    System.err.println("Invalid GAV coordinate: " + args[0] + " (expected groupId:artifactId:version)");
                    System.exit(1);
                    return;
                }
                groupId = parts[0];
                artifactId = parts[1];
                version = parts[2];
                if (args.length > 1) {
                    jarPath = args[1];
                }
            } else if (args.length >= 3) {
                groupId = args[0];
                artifactId = args[1];
                version = args[2];
                if (args.length > 3) {
                    jarPath = args[3];
                }
            } else {
                System.err.println("Invalid arguments. Use --help for usage instructions.");
                System.exit(1);
                return;
            }

            LocalM2CacheManager cacheManager = new LocalM2CacheManager();
            File jarFile;
            if (jarPath != null) {
                if (jarPath.startsWith("~" + File.separator) || jarPath.equals("~")) {
                    jarPath = System.getProperty("user.home") + jarPath.substring(1);
                }
                jarFile = new File(jarPath);
            } else {
                File dir = cacheManager.getArtifactDirectory(groupId, artifactId, version);
                jarFile = new File(dir, artifactId + "-" + version + ".jar");
            }

            if (!jarFile.exists()) {
                System.err.println("JAR file not found: " + jarFile.getAbsolutePath());
                System.err.println("Tip: Run 'mvn dependency:get -Dartifact=" + groupId + ":" + artifactId + ":" + version + "' first.");
                System.exit(1);
                return;
            }

            System.out.println("Compiling and caching " + groupId + ":" + artifactId + ":" + version + " to ~/.m2/repository...");
            CacheMetadata metadata = cacheManager.getOrCompile(groupId, artifactId, version, jarFile);
            File watFile = cacheManager.getCachedWatFile(groupId, artifactId, version);
            File metaFile = cacheManager.getCachedMetadataFile(groupId, artifactId, version);

            System.out.println("Successfully installed into local repository cache:");
            System.out.println("  WAT:      " + watFile.getAbsolutePath());
            System.out.println("  Metadata: " + metaFile.getAbsolutePath());
            System.out.println("  SHA-256:  " + metadata.getJarSha256());
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
