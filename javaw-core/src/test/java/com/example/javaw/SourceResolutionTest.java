package com.example.javaw;

import com.github.javaparser.StaticJavaParser;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class SourceResolutionTest {
    @Test void executesBinaryConditionalAndLexicallyScopedLocals() throws Exception {
        String source = """
            package sample;
            class Box {
                public int value;
                Box(int value) { this.value = value; }
                public int read() { return this.value; }
            }
            class Factory { public static Box box() { return new Box(4); } }
            class BaseScope { public int inherited; BaseScope() { this.inherited = 5; } }
            class ResolutionApp extends BaseScope {
                public int checkScope() {
                    int result = 0;
                    { var inherited = 2; result += inherited; }
                    return result + inherited;
                }
                public static int inheritedScope() { var app = new ResolutionApp(); return app.checkScope(); }
                static int x = 9;
                static int visits = 0;
                public static boolean visited() { visits++; return true; }
                public static int shortCircuit() {
                    var left = false && visited();
                    var right = true || visited();
                    if (left || !right) return -1;
                    return visits;
                }
                public static double binary() { var box = Factory.box(); var value = box.value + 0.25; return value; }
                public static long wide() { var value = 9007199254740993L + 2; return value; }
                public static int comparison() { var value = 3L < 4L; if (value) return 1; return 0; }
                public static double foreachDecimals() {
                    double result = 0.0;
                    for (var value : new double[]{0.25, 1.5}) result += value;
                    return result;
                }
                public static double division() { var value = 1 / 2; return value; }
                public static int shifts() { var value = 1 << 33L; return value; }
                public static double conditional(boolean flag) { var value = flag ? 2 : 3.75; return value; }
                public static double reverse(boolean flag) { var value = flag ? 1.25 : 4; return value; }
                public static long longConditional(boolean flag) { var value = flag ? 2 : 9007199254740993L; return value; }
                public static int referenceConditional(boolean flag) {
                    var value = flag ? new Box(8) : null;
                    if (value == null) return -1;
                    return value.read();
                }
                public static int qualified() {
                    sample.Box box = sample.Factory.box();
                    var second = new sample.Box(box.read() + 2);
                    java.lang.String label = "ok";
                    return second.read() + label.length();
                }
                public static int scopes() {
                    int result = 0;
                    { var x = 1; result += x; }
                    { var x = 2.5; if (x > 2.0) result += 2; }
                    result += x;
                    for (int i = 0; i < 2; i++) result++;
                    for (long i = 0L; i < 2L; i = i + 1L) result++;
                    try { throw new RuntimeException("first"); }
                    catch (RuntimeException error) { var local = 1; result += local; }
                    try { throw new RuntimeException("second"); }
                    catch (RuntimeException error) { var local = 2; result += local; }
                    for (var item : new int[]{1, 2}) result += item;
                    return result;
                }
            }
            """;
        byte[] wasm = JavacToWasm.compile("ResolutionApp.java", source, false).getWasm();
        execute(wasm, """
            assert.equal(e.binary(), 4.25);
            assert.equal(e.inheritedScope(), 7);
            assert.equal(e.wide(), 9007199254740995n);
            assert.equal(e.comparison(), 1);
            assert.equal(e.division(), 0);
            assert.equal(e.foreachDecimals(), 1.75);
            assert.equal(e.shifts(), 2);
            assert.equal(e.conditional(1), 2);
            assert.equal(e.conditional(0), 3.75);
            assert.equal(e.reverse(1), 1.25);
            assert.equal(e.reverse(0), 4);
            assert.equal(e.longConditional(0), 9007199254740993n);
            assert.equal(e.longConditional(1), 2n);
            assert.equal(e.referenceConditional(1), 8);
            assert.equal(e.referenceConditional(0), -1);
            assert.equal(e.shortCircuit(), 0);
            assert.equal(e.qualified(), 8);
            assert.equal(e.scopes(), 22);
            """);
    }

    @Test void resolvesQualifiedTypesAcrossSourceFiles() throws Exception {
        var model = StaticJavaParser.parse("package model; public class Value { public int n; public Value(int n) { this.n=n; } public int read() { return n; } }");
        var app = StaticJavaParser.parse("package app; class App { public static int run() { var value = new model.Value(17); return value.read(); } }");
        String before = app.toString();
        String wat = JavacToWasm.generateWat(List.of(model, app));
        assertEquals(before, app.toString());
        execute(JavacToWasm.assembleWasm(wat, false), "assert.equal(e.run(), 17);");
    }

    @Test void rejectsUnresolvedInferenceWithSourceLocationAndRemedy() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> JavacToWasm.compile("Unknown.java",
                "class Unknown { public static int run() { var value = missing(); return 0; } }", false));
        assertTrue(error.getMessage().contains("Unknown.java:1:"), error.getMessage());
        assertTrue(error.getMessage().contains("Cannot infer type of local 'value'"));
        assertTrue(error.getMessage().contains("explicit supported type"));
    }

    @Test void rejectsLocalsOutsideTheirLexicalScope() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> JavacToWasm.compile("Scope.java",
                "class Scope { static int run() { { var hidden = 1; } var value = hidden; return value; } }", false));
        assertTrue(error.getMessage().contains("Scope.java:1:"));
        assertTrue(error.getMessage().contains("not in scope"));
    }

    @Test void rejectsSelfReferencesEvenWhenAFieldHasTheSameName() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> JavacToWasm.compile("Self.java",
                "class Self { static int value = 1; static int run() { var value = value; return value; } }", false));
        assertTrue(error.getMessage().contains("before initialization"));
    }

    @Test void diagnosesConflictingModuleTypeNames() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> JavacToWasm.generateWat(List.of(
                StaticJavaParser.parse("package one; class Value {}"), StaticJavaParser.parse("package two; class Value {}"))));
        assertTrue(error.getMessage().contains("Duplicate simple type name 'Value'"));
    }

    private void execute(byte[] wasm, String checks) throws Exception {
        assertNotNull(wasm, "Source resolution must produce a valid Wasm binary");
        String script = "const assert = require('node:assert/strict'); const { instantiateJavaw } = require(process.argv[1]); "
                + "instantiateJavaw(new WebAssembly.Module(require('node:fs').readFileSync(0))).then(({instance:{exports:e}}) => {"
                + checks + "}).catch(error => { console.error(error); process.exitCode=1; });";
        Process node = new ProcessBuilder("node", "-e", script, Path.of("src/main/resources/javaw-runtime.js").toAbsolutePath().toString())
                .redirectErrorStream(true).start();
        try {
            try (var input = node.getOutputStream()) { input.write(wasm); }
            assertTrue(node.waitFor(20, TimeUnit.SECONDS), "Node execution timed out");
            assertEquals(0, node.exitValue(), new String(node.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        } finally { if (node.isAlive()) node.destroyForcibly(); }
    }
}
