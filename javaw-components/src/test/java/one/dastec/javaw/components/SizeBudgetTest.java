package one.dastec.javaw.components;

import one.dastec.javaw.JavacToWasm;
import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M6 size budget: compiles examples/components/{JavawCounter,TodoList}.java at -O0 (shared memory, as every javaw module) and
 * fails when the .wasm, the generated components ES module or the shared javaw-runtime.js exceed the
 * byte budgets recorded in examples/components/size-budget.properties. Raise a budget only on purpose,
 * and record the new measured size in docs/web-components.md.
 */
class SizeBudgetTest {
    static final Path EXAMPLES = Path.of("../examples/components").toAbsolutePath().normalize();

    @Test
    void counterStaysWithinItsSizeBudget() throws Exception { check("JavawCounter"); }

    @Test
    void todoListStaysWithinItsSizeBudget() throws Exception { check("TodoList"); }

    private static void check(String name) throws Exception {
        Properties budget = new Properties();
        try (Reader r = Files.newBufferedReader(EXAMPLES.resolve("size-budget.properties"))) { budget.load(r); }
        Path out = Path.of("target/size-budget", name).toAbsolutePath();
        Path wasm = compile(name, out);
        long wasmBytes = Files.size(wasm);
        long moduleBytes = Files.size(out.resolve(name + ".components.js"));
        long runtimeBytes = Files.size(Path.of("../javaw-core/src/main/resources/javaw-runtime.js"));
        String measured = name + ": wasm=" + wasmBytes + " components.js=" + moduleBytes + " javaw-runtime.js=" + runtimeBytes;
        System.out.println("[size-budget] " + measured);
        assertWithin(budget, name + ".wasm", wasmBytes, measured);
        assertWithin(budget, name + ".components.js", moduleBytes, measured);
        assertWithin(budget, "javaw-runtime.js", runtimeBytes, measured);
    }

    /** Compiles an example into {@code out} (wasm, artifacts) and copies the runtime and page next to it. */
    static Path compile(String name, Path out) throws Exception {
        Files.createDirectories(out);
        Path wasm = out.resolve(name + ".wasm");
        assertTrue(JavacToWasm.compileFiles(List.of(EXAMPLES.resolve(name + ".java")), out.resolve("wat/" + name + ".wat"), wasm,
                null, null, "-O0", false, true), name + " compiles");
        Files.copy(Path.of("../javaw-core/src/main/resources/javaw-runtime.js"), out.resolve("javaw-runtime.js"),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return wasm;
    }

    private static void assertWithin(Properties budget, String key, long bytes, String measured) {
        String limit = budget.getProperty(key);
        assertNotNull(limit, "no budget recorded for " + key);
        assertTrue(bytes <= Long.parseLong(limit.trim()), key + " is " + bytes + " bytes, over its budget of " + limit + " (" + measured + ")");
    }
}
