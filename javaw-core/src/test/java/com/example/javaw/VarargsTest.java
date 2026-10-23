package com.example.javaw;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class VarargsTest {
    @Test void executesVarargsAfterFixedArityPhases() throws Exception {
        byte[] wasm = JavacToWasm.compile("VarargsApp.java", """
            class Many { int total; Many(int... values) { for (int value : values) total += value; } public int result() { return total; } }
            class VarargsApp {
                public static int sum(int... values) { int total = 0; for (int value : values) total += value; return total; }
                public static int count(Object... values) { return values.length; }
                public static int pick(int x) { return 1; }
                public static int pick(Integer... x) { return 2; }
                public static int narrow(Object... values) { return 1; }
                public static int narrow(String... values) { return 2; }
                public static int specificity() { return narrow(); }
                public static int empty() { return sum(); }
                public static int packed() { return sum(1, 2, 3); }
                public static int array() { return sum(new int[]{4, 5}); }
                public static int boxed() { return count(1, 2.5, "ok"); }
                public static int strict() { return pick(1); }
                public static int constructor() { var x = new Many(1, 2, 3); return x.result(); }
            }
            """, false).getWasm();
        OverloadResolutionTest.execute(wasm, """
            assert.equal(e.specificity(), 2); assert.equal(e.empty(), 0); assert.equal(e.packed(), 6);
            assert.equal(e.array(), 9); assert.equal(e.boxed(), 3);
            assert.equal(e.strict(), 1); assert.equal(e.constructor(), 6);
            """);
    }
    @Test void diagnosesAmbiguousVarargs() {
        var error = assertThrows(IllegalArgumentException.class, () -> JavacToWasm.compile("Ambiguous.java",
            "class Ambiguous { static int pick(String... x) { return 1; } static int pick(Integer... x) { return 2; } public static int test() { return pick(); } }", false));
        assertTrue(error.getMessage().contains("Ambiguous overload"), error.getMessage());
        assertTrue(error.getMessage().contains("Ambiguous.java:"), error.getMessage());
    }
}
