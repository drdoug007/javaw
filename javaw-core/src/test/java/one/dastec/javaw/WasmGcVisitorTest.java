package one.dastec.javaw;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class WasmGcVisitorTest {
    @TempDir Path dir;

    @Test void nativeObjectsExecuteAndCrossHostBoundary() throws Exception {
        String wat = JavacToWasm.generateGcWat("""
            class Box {
                int value;
                Box next;
                Box(int value) { this.value = value; }
                public int read() { return this.value; }
                public static Box make(int value) { return new Box(value); }
                public static int test() {
                    Box a = new Box(10);
                    Box b = new Box(32);
                    a.next = b;
                    b.next = a;
                    a.next.value = a.value + b.value;
                    return b.value;
                }
            }
            """);
        assertTrue(wat.contains("struct.new $Box"));
        assertTrue(wat.contains("struct.get $Box"));
        assertTrue(wat.contains("struct.set $Box"));
        assertFalse(wat.contains("memory"));
        Path input = dir.resolve("gc.wat"), binary = dir.resolve("gc.wasm");
        Files.writeString(input, wat);
        run("wasm-as", input.toString(), "--all-features", "-o", binary.toString());
        run("node", "-e", "const fs=require('node:fs'); const assert=require('node:assert/strict');"
                + "const e=new WebAssembly.Instance(new WebAssembly.Module(fs.readFileSync(process.argv[1]))).exports;"
                + "assert.equal(e['Box.test'](),42); const b=e['Box.make'](71);"
                + "assert.equal(e['Box.read'](b),71);", binary.toString());
    }

    @Test void rejectsUnsupportedFeaturesWithLocations() {
        for (String source : new String[]{"class A extends B {}", "class A { static int x; }",
                "class A { int f() { while(true) {} } }", "class A { String s; }",
                "class A { A(){} A(int x){} }"}) {
            var error = assertThrows(IllegalArgumentException.class, () -> JavacToWasm.generateGcWat(source));
            assertTrue(error.getMessage().contains(" at "));
            assertTrue(error.getMessage().contains("linear-memory backend"));
        }
    }

    private void run(String... args) throws Exception {
        var process = new ProcessBuilder(args).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes());
        assertEquals(0, process.waitFor(), output);
    }
}
