package com.example.javaw;

import com.github.javaparser.StaticJavaParser;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class OverloadResolutionTest {
    private ClassRegistry registry(String source) {
        return ClassRegistry.build(StaticJavaParser.parse(source));
    }

    @Test void selectsByTypesRatherThanDeclarationOrder() {
        ClassRegistry registry = registry("class Choice { Choice(double x) {} Choice(long x) {} Choice(int x) {} Choice(char x) {} }");
        var candidates = registry.getClass("Choice").getConstructors();
        for (String type : List.of("int", "long", "double", "char"))
            assertEquals(List.of(type), registry.resolveOverload(candidates, List.of(type)).getParamTypeNames());
        assertEquals(List.of("int"), registry.resolveOverload(candidates, List.of("byte")).getParamTypeNames());
        assertThrows(IllegalArgumentException.class, () -> registry.resolveOverload(candidates, List.of("boolean")));
        assertThrows(IllegalArgumentException.class, () -> registry.resolveOverload(candidates, List.of()));
    }

    @Test void usesReferenceSpecificityAndRejectsAmbiguity() {
        ClassRegistry registry = registry("class Parent {} class Child extends Parent {} class Choice { Choice(Object x) {} Choice(Parent x) {} Choice(Child x) {} }");
        assertEquals(List.of("Child"), registry.resolveOverload(registry.getClass("Choice").getConstructors(), List.of("null")).getParamTypeNames());
        registry = registry("class Choice { Choice(String x) {} Choice(Integer x) {} }");
        ClassRegistry ambiguous = registry;
        assertTrue(assertThrows(IllegalArgumentException.class, () -> ambiguous.resolveOverload(
                ambiguous.getClass("Choice").getConstructors(), List.of("null"))).getMessage().contains("Ambiguous overload"));
    }

    @Test void reportsConstructorErrorsAtSourceLocation() {
        var error = assertThrows(IllegalArgumentException.class, () -> JavacToWasm.compile("Bad.java",
                "class Choice { Choice(String x) {} Choice(Integer x) {} } class Bad { public static int test() { new Choice(null); return 0; } }", false));
        assertTrue(error.getMessage().contains("Bad.java:1:"), error.getMessage());
        assertTrue(error.getMessage().contains("Ambiguous overload"), error.getMessage());
    }

    @Test void executesTypedConstructorsAndConstructorChaining() throws Exception {
        byte[] wasm = JavacToWasm.compile("ConstructorApp.java", """
            class Value {
                double amount;
                Value(double value) { this.amount = value + 0.5; }
                Value(int value) { this.amount = value + 10.0; }
                Value() { this(2.25); }
                public double read() { return amount; }
            }
            class Child extends Value { Child() { super(3.25); } }
            class ConstructorApp {
                public static double integer() { var v = new Value(2); return v.read(); }
                public static double decimal() { var v = new Value(2.25); return v.read(); }
                public static double chained() { var v = new Value(); return v.read(); }
                public static double inherited() { var v = new Child(); return v.read(); }
            }
            """, false).getWasm();
        assertNotNull(wasm);
        String script = """
            const assert = require('node:assert/strict');
            const {instantiateJavaw} = require(process.argv[1]);
            instantiateJavaw(new WebAssembly.Module(require('node:fs').readFileSync(0)))
              .then(({instance: {exports: e}}) => {
                assert.equal(e.integer(), 12);
                assert.equal(e.decimal(), 2.75);
                assert.equal(e.chained(), 2.75);
                assert.equal(e.inherited(), 3.75);
              }).catch(error => { console.error(error); process.exitCode = 1; });
            """;
        Process node = new ProcessBuilder("node", "-e", script,
                Path.of("src/main/resources/javaw-runtime.js").toAbsolutePath().toString()).redirectErrorStream(true).start();
        try {
            try (var input = node.getOutputStream()) { input.write(wasm); }
            assertTrue(node.waitFor(20, TimeUnit.SECONDS), "Node execution timed out");
            assertEquals(0, node.exitValue(), new String(node.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        } finally { if (node.isAlive()) node.destroyForcibly(); }
    }
}
