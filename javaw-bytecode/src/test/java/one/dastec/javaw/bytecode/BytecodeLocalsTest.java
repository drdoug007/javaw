package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.ToolProvider;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lowered functions declare only the mapper locals their bodies name. Generated methods used to declare 512 stack
 * words in four types each (2,048 locals), and every method at least 272.
 */
class BytecodeLocalsTest {
    @TempDir Path directory;

    @Test void keepsOnlyDeclarationsTheBodyNames() {
        StringBuilder wat = new StringBuilder("    (func $f (result i32)\n");
        int start = wat.length();
        for (String local : List.of("$l_0 i32", "$l_0_i64 i64", "$l_1 i32", "$stk_0 i32", "$stk_0_f64 f64", "$stk_10 i32", "$__shuffle_i32_0 i32"))
            wat.append("        (local ").append(local).append(")\n");
        int end = wat.length();
        wat.append("        (local $__state i32)\n        local.get $p0\n        local.set $l_0\n        local.get $stk_1\n")
                .append("        (local.tee $stk_0_f64)\n        call $l_1_helper\n        unreachable\n");
        ClassBytecodeCompiler.pruneLocals(wat, start, end);
        assertEquals("    (func $f (result i32)\n        (local $l_0 i32)\n        (local $stk_0_f64 f64)\n"
                + "        (local $__state i32)\n        local.get $p0\n        local.set $l_0\n        local.get $stk_1\n"
                + "        (local.tee $stk_0_f64)\n        call $l_1_helper\n        unreachable\n", wat.toString());
    }

    @Test void generatedAndOrdinaryMethodsDeclareFewLocals() throws Exception {
        Path source = directory.resolve("Lambdas.java"), classes = Files.createDirectories(directory.resolve("classes"));
        Files.writeString(source, """
            import java.util.function.*;
            public class Lambdas {
                public static int run(int n) {
                    IntUnaryOperator twice = x -> x * 2;
                    Supplier<String> text = () -> "n=" + n;
                    return twice.applyAsInt(n) + text.get().length();
                }
            }
            """);
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, "--release", "17", "-d", classes.toString(), source.toString()));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JarOutputStream jar = new JarOutputStream(bytes); var files = Files.walk(classes)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".class")).sorted().toList()) {
                jar.putNextEntry(new JarEntry(classes.relativize(file).toString().replace(File.separatorChar, '/')));
                jar.write(Files.readAllBytes(file)); jar.closeEntry();
            }
        }
        String wat = BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(bytes.toByteArray()));
        int most = 0, functions = 0, current = 0;
        for (String line : wat.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("(func ")) { most = Math.max(most, current); current = 0; functions++; }
            else if (trimmed.startsWith("(local $stk_") || trimmed.startsWith("(local $l_")) current++;
        }
        most = Math.max(most, current);
        assertTrue(functions > 10, "functions " + functions);
        assertTrue(most < 120, "a function declares " + most + " mapper locals");
        WasmTestSupport.execute(directory, wat, "assert.equal(e['Lambdas.run'](21),46);");
    }
}
