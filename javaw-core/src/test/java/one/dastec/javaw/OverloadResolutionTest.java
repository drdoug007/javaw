package one.dastec.javaw;

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

    @Test void executesRecordEnumAndFacadeNamedOverloads() throws Exception {
        byte[] wasm = JavacToWasm.compile("KindsApp.java", """
            record Scale(int factor) {
                public int length(int x) { return factor * x; }
                public double length(double x) { return factor * x; }
                public double twice() { return length(2.5); }
                public static int of(int x) { return x + 1; }
                public static double of(double x) { return x + 0.5; }
            }
            enum Mode { ONE(2.5), TWO(2);
                double initial;
                Mode(int x) { initial = x + 10.0; }
                Mode(double x) { initial = x; }
                public double initial() { return initial; }
                public int length(int x) { return x + 1; }
                public double length(double x) { return x + 0.5; }
            }
            class FirstHelper { public static int same(int x) { return x + 1; } }
            class SecondHelper { public static int same(int x) { return x + 2; } }
            class KindsApp {
                public static double records() { var scale = new Scale(3); return scale.length(2.5) + scale.length(2) + scale.twice(); }
                public static double statics() { return Scale.of(1.5) + Scale.of(2); }
                public static double enums() { return Mode.ONE.length(2.5) + Mode.ONE.length(2); }
                public static int collision() { return FirstHelper.same(1) + SecondHelper.same(1); }
                public static double enumConstructors() { return Mode.ONE.initial() + Mode.TWO.initial(); }
            }
            """, false).getWasm();
        execute(wasm, """
            e.__init_enums();
            assert.equal(e.records(), 21);
            assert.equal(e.statics(), 5);
            assert.equal(e.enums(), 6);
            assert.equal(e.collision(), 5);
            assert.equal(e.enumConstructors(), 14.5);
            """);
    }

    @Test void rejectsPrivateAndStaticContextViolations() {
        for (String source : List.of(
                "class Hidden { private static int pick(int x) { return x; } } class Bad { public static int test() { return Hidden.pick(1); } }",
                "class Bad { public int pick(int x) { return x; } public static int test() { return pick(1); } }",
                "class Bad { public int pick(int x) { return x; } public static int test() { return Bad.pick(1); } }",
                "class Hidden { private Hidden(int x) {} } class Bad { public static int test() { new Hidden(1); return 0; } }")) {
            var error = assertThrows(IllegalArgumentException.class, () -> JavacToWasm.compile("Access.java", source, false));
            assertTrue(error.getMessage().contains("Access.java:"), error.getMessage());
            assertTrue(error.getMessage().contains("No applicable overload"), error.getMessage());
        }
    }

    @Test void nestedVirtualCallsKeepTheirReceivers() throws Exception {
        byte[] wasm = JavacToWasm.compile("NestedApp.java", """
            class Counter { int base; Counter(int x) { base = x; } public int pick(int x) { return base + x; } }
            class NestedApp { public static int test() { var a = new Counter(10); var b = new Counter(100); return a.pick(b.pick(2)); } }
            """, false).getWasm();
        execute(wasm, "assert.equal(e.test(), 112);");
    }

    @Test void executesOverloadedMethodsAndVirtualDispatch() throws Exception {
        byte[] wasm = JavacToWasm.compile("MethodsApp.java", """
            class ParentMethods {
                int marker;
                public double pick(double x) { return x + 0.5; }
                public int pick(int x) { return x + 1; }
                public double inheritedCall() { return pick(2.5); }
            }
            class ChildMethods extends ParentMethods {
                public double pick(double x) { return x + 10.5; }
                public double parentCall() { return super.pick(2.5); }
            }
            class MethodsApp {
                public static double choose(double x) { return x + 0.25; }
                public static long choose(long x) { return x + 1L; }
                public static int choose(int x, int y) { return x + y; }
                public static double inferred() { var x = choose(2.5); return x; }
                public static long wide() { var x = MethodsApp.choose(9007199254740993L); return x; }
                public static int arity() { return choose(2, 3); }
                public static double dispatch() { ParentMethods x = new ChildMethods(); return x.pick(2.5); }
                public static int inheritedOverload() { var x = new ChildMethods(); return x.pick(2); }
                public static double implicitDispatch() { var x = new ChildMethods(); return x.inheritedCall(); }
                public static double directSuper() { var x = new ChildMethods(); return x.parentCall(); }
            }
            """, false).getWasm();
        execute(wasm, """
            assert.equal(e.inferred(), 2.75);
            assert.equal(e.wide(), 9007199254740994n);
            assert.equal(e.arity(), 5);
            assert.equal(e.dispatch(), 13);
            assert.equal(e.inheritedOverload(), 3);
            assert.equal(e.implicitDispatch(), 13);
            assert.equal(e.directSuper(), 3);
            """);
    }

    @Test void rejectsAmbiguousAndInapplicableMethodCalls() {
        for (String argument : List.of("null", "true")) {
            var error = assertThrows(IllegalArgumentException.class, () -> JavacToWasm.compile("BadMethod.java",
                    "class BadMethod { static int pick(String x) { return 1; } static int pick(Integer x) { return 2; } public static int test() { return pick("
                            + argument + "); } }", false));
            assertTrue(error.getMessage().contains("BadMethod.java:1:"), error.getMessage());
            assertTrue(error.getMessage().contains(argument.equals("null") ? "Ambiguous overload" : "No applicable overload"), error.getMessage());
        }
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
        execute(wasm, """
            assert.equal(e.integer(), 12);
            assert.equal(e.decimal(), 2.75);
            assert.equal(e.chained(), 2.75);
            assert.equal(e.inherited(), 3.75);
            """);
    }

    static void execute(byte[] wasm, String assertions) throws Exception {
        assertNotNull(wasm);
        String script = """
            const assert = require('node:assert/strict');
            const {instantiateJavaw} = require(process.argv[1]);
            instantiateJavaw(new WebAssembly.Module(require('node:fs').readFileSync(0)))
              .then(({instance: {exports: e}}) => {
            """ + assertions + """
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
