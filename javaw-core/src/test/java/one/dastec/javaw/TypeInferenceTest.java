package one.dastec.javaw;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class TypeInferenceTest {
    @Test void executesInferredLocalsAndInheritedFactoryResults() throws Exception {
        String source = """
            class ParentValue {
                public int marker;
                public double amount() { return 1.25; }
                public void change(int marker) { this.marker = marker; }
            }
            class ChildValue extends ParentValue { }
            class Factory {
                public static ChildValue make() { return new ChildValue(); }
            }
            class InferenceApp {
                public static double references() {
                    var value = Factory.make();
                    var alias = value;
                    alias.change(7);
                    var amount = alias.amount();
                    if (alias.marker != 7) return 0.0;
                    return amount;
                }
                public static double decimals() { var value = -1.25; return value; }
                public static float floats() { var value = 2.5f; return value; }
                public static long wide() { var value = 9007199254740993L; return value; }
                public static int strings() { var value = "hello"; return value.length(); }
                public static int flags() { var value = !false; if (value) return 1; return 0; }
                public static double arrays() { var values = new double[]{1.25, 3.5}; return values[1]; }
            }
            """;
        byte[] wasm = JavacToWasm.compile("InferenceApp.java", source, false).getWasm();
        assertNotNull(wasm, "Inferred local storage and method calls must assemble");
        String script = """
            const assert = require('node:assert/strict');
            const bytes = require('node:fs').readFileSync(0);
            const { instantiateJavaw } = require(process.argv[1]);
            instantiateJavaw(new WebAssembly.Module(bytes)).then(({instance: {exports: e}}) => {
                assert.equal(e.references(), 1.25);
                assert.equal(e.decimals(), -1.25);
                assert.equal(e.floats(), 2.5);
                assert.equal(e.wide(), 9007199254740993n);
                assert.equal(e.strings(), 5);
                assert.equal(e.flags(), 1);
                assert.equal(e.arrays(), 3.5);
            }).catch(error => { console.error(error); process.exitCode = 1; });
            """;
        Process node = new ProcessBuilder("node", "-e", script, java.nio.file.Path.of("src/main/resources/javaw-runtime.js").toAbsolutePath().toString()).redirectErrorStream(true).start();
        try {
            try (var input = node.getOutputStream()) { input.write(wasm); }
            assertTrue(node.waitFor(20, TimeUnit.SECONDS), "Node execution timed out");
            String output = new String(node.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(0, node.exitValue(), output);
        } finally { if (node.isAlive()) node.destroyForcibly(); }
    }
}
