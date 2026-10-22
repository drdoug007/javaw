package com.example.javaw.plugin;

import com.example.javaw.JavacToWasm;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Maven Mojo that compiles Java source files into WebAssembly Text (.wat),
 * binary modules (.wasm), and Source Maps (.wasm.map) using javaw.
 */
@Mojo(name = "compile", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class CompileMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * Source directory containing Java files to compile to WebAssembly.
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/java", property = "javaw.sourceDirectory")
    private File sourceDirectory;

    /**
     * Target directory for generated WebAssembly files (.wat, .wasm, .wasm.map).
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-wasm", property = "javaw.outputDirectory")
    private File outputDirectory;

    /**
     * Binaryen wasm-opt optimization level (-O0, -O1, -O2, -O3, -Oz).
     */
    @Parameter(defaultValue = "-O3", property = "javaw.optimizationLevel")
    private String optimizationLevel;

    /**
     * Whether to generate Source Map v3 (.wasm.map) debugging sections.
     */
    @Parameter(defaultValue = "true", property = "javaw.generateSourceMaps")
    private boolean generateSourceMaps;

    /**
     * Whether to enable WebAssembly threads and shared memory atomics.
     */
    @Parameter(defaultValue = "true", property = "javaw.enableThreads")
    private boolean enableThreads;

    /**
     * Whether to automatically copy javaw-runtime.js into output directory.
     */
    @Parameter(defaultValue = "true", property = "javaw.copyRuntimeScript")
    private boolean copyRuntimeScript;

    /**
     * If true, compiles all matching Java source files into a single unified WASM module.
     */
    @Parameter(defaultValue = "false", property = "javaw.batchMode")
    private boolean batchMode;

    /**
     * Output module stem name when compiling in batch mode. Defaults to project artifactId.
     */
    @Parameter(property = "javaw.moduleName")
    private String moduleName;

    /**
     * Optional include patterns (e.g. "**\/*.java").
     */
    @Parameter(property = "javaw.includes")
    private List<String> includes;

    /**
     * Optional exclude patterns (e.g. "**\/Test*.java").
     */
    @Parameter(property = "javaw.excludes")
    private List<String> excludes;

    /**
     * Skip execution of this plugin goal.
     */
    @Parameter(defaultValue = "false", property = "javaw.skip")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping javaw WebAssembly compilation (javaw.skip=true).");
            return;
        }

        if (sourceDirectory == null || !sourceDirectory.exists() || !sourceDirectory.isDirectory()) {
            getLog().info("Source directory does not exist or is not a directory: " + sourceDirectory);
            return;
        }

        List<Path> javaFiles = findJavaFiles(sourceDirectory.toPath());
        if (javaFiles.isEmpty()) {
            getLog().info("No matching Java files found in " + sourceDirectory);
            return;
        }

        Path outDirPath = outputDirectory.toPath();
        try {
            Files.createDirectories(outDirPath);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to create output directory: " + outDirPath, e);
        }

        getLog().info("Compiling " + javaFiles.size() + " Java source file(s) from " + sourceDirectory + " to WebAssembly in " + outDirPath);

        if (batchMode) {
            compileBatch(javaFiles, outDirPath);
        } else {
            compileIndividual(javaFiles, outDirPath);
        }

        if (copyRuntimeScript) {
            copyRuntimeScriptTo(outDirPath);
        }
    }

    private List<Path> findJavaFiles(Path rootDir) throws MojoExecutionException {
        List<Path> result = new ArrayList<>();
        List<PathMatcher> includeMatchers = createMatchers(includes);
        List<PathMatcher> excludeMatchers = createMatchers(excludes);

        try (Stream<Path> stream = Files.walk(rootDir)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(p -> {
                        Path rel = rootDir.relativize(p);
                        if (!includeMatchers.isEmpty()) {
                            boolean matched = includeMatchers.stream().anyMatch(m -> m.matches(rel));
                            if (!matched) return;
                        }
                        if (!excludeMatchers.isEmpty()) {
                            boolean excluded = excludeMatchers.stream().anyMatch(m -> m.matches(rel));
                            if (excluded) return;
                        }
                        result.add(p);
                    });
        } catch (IOException e) {
            throw new MojoExecutionException("Error scanning source directory: " + rootDir, e);
        }
        return result;
    }

    private List<PathMatcher> createMatchers(List<String> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return Collections.emptyList();
        }
        List<PathMatcher> matchers = new ArrayList<>();
        for (String pattern : patterns) {
            if (pattern != null && !pattern.isBlank()) {
                matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + pattern.trim()));
            }
        }
        return matchers;
    }

    private void compileIndividual(List<Path> javaFiles, Path outDirPath) throws MojoFailureException {
        Path srcRoot = sourceDirectory.toPath().toAbsolutePath().normalize();
        int successCount = 0;

        for (Path javaFile : javaFiles) {
            Path absJavaFile = javaFile.toAbsolutePath().normalize();
            Path relPath = srcRoot.relativize(absJavaFile);
            Path targetSubDir = outDirPath;
            if (relPath.getParent() != null) {
                targetSubDir = outDirPath.resolve(relPath.getParent());
            }

            String fileName = javaFile.getFileName().toString();
            String stem = fileName.endsWith(".java") ? fileName.substring(0, fileName.length() - 5) : fileName;

            Path watPath = targetSubDir.resolve(stem + ".wat");
            Path wasmPath = targetSubDir.resolve(stem + ".wasm");
            Path watMapPath = targetSubDir.resolve(stem + ".wat.map");
            Path wasmMapPath = targetSubDir.resolve(stem + ".wasm.map");

            getLog().info("Compiling: " + relPath + " -> " + wasmPath);
            boolean success = JavacToWasm.compileFiles(
                    Collections.singletonList(absJavaFile),
                    watPath,
                    wasmPath,
                    watMapPath,
                    wasmMapPath,
                    optimizationLevel,
                    generateSourceMaps,
                    enableThreads
            );

            if (!success) {
                throw new MojoFailureException("Compilation failed for " + relPath);
            }
            successCount++;
        }
        getLog().info("Successfully compiled " + successCount + " WebAssembly module(s).");
    }

    private void compileBatch(List<Path> javaFiles, Path outDirPath) throws MojoFailureException {
        String stem = moduleName != null && !moduleName.isBlank()
                ? moduleName.trim()
                : (project != null ? project.getArtifactId() : "app");

        Path watPath = outDirPath.resolve(stem + ".wat");
        Path wasmPath = outDirPath.resolve(stem + ".wasm");
        Path watMapPath = outDirPath.resolve(stem + ".wat.map");
        Path wasmMapPath = outDirPath.resolve(stem + ".wasm.map");

        getLog().info("Compiling " + javaFiles.size() + " files into unified module: " + wasmPath);
        boolean success = JavacToWasm.compileFiles(
                javaFiles,
                watPath,
                wasmPath,
                watMapPath,
                wasmMapPath,
                optimizationLevel,
                generateSourceMaps,
                enableThreads
        );

        if (!success) {
            throw new MojoFailureException("Batch WebAssembly compilation failed.");
        }
        getLog().info("Successfully compiled unified WebAssembly module: " + wasmPath);
    }

    private void copyRuntimeScriptTo(Path outDirPath) {
        try (InputStream is = JavacToWasm.class.getResourceAsStream("/javaw-runtime.js")) {
            if (is != null) {
                Path dest = outDirPath.resolve("javaw-runtime.js");
                Files.copy(is, dest, StandardCopyOption.REPLACE_EXISTING);
                getLog().info("Copied javaw-runtime.js to " + dest);
            } else {
                getLog().warn("javaw-runtime.js resource not found on classpath.");
            }
        } catch (IOException e) {
            getLog().warn("Failed to copy javaw-runtime.js: " + e.getMessage());
        }
    }
}
