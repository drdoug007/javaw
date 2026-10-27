package one.dastec.javaw;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class GenericInferenceTest {
    @Test void executesInferredGenericMethodsClassesAndBounds() throws Exception {
        byte[] wasm = JavacToWasm.compile("GenericApp.java", """
            class Holder<T> {
                public T value;
                Holder(T value) { this.value = value; }
                public T get() { return value; }
                public void set(T value) { this.value = value; }
            }
            class StringHolder extends Holder<String> { StringHolder(String value) { super(value); } public void set(String value) { this.value = value + "!"; } }
            class GenericApp {
                public static <T> T identity(T value) { return value; }
                public static <T extends Number> double amount(T value) { return value.doubleValue(); }
                public static <T> T read(Holder<T> holder) { return holder.get(); }
                public static int tie(String value) { return 1; }
                public static <T> int tie(T value) { return 2; }
                public static <T extends Number> int boundsTie(T value) { return 1; }
                public static <T> int boundsTie(T value) { return 2; }
                public static int overloadTies() { return tie("ok") + boundsTie(2); }
                public static int wildcardReads() { Holder<? extends Number> h = new Holder<Integer>(4); var value = h.get(); return value.intValue(); }
                public static int wildcardWrites() { Holder<? super Integer> h = new Holder<Number>(2); h.set(3); return ((Number)h.get()).intValue(); }
                public static int genericOverride() { Holder<String> h = new StringHolder("a"); h.set("b"); return h.get().length(); }
                public static <T> T first(T... values) { return values[0]; }
                public static int inferred() { var x = identity("hello"); return x.length(); }
                public static long boxed() { var x = identity(9007199254740993L); return x + 1L; }
                public static double bounded() { return amount(2.5); }
                public static int explicit() { var x = GenericApp.<String>identity("ok"); return x.length(); }
                public static int classes() { var box = new Holder<>("first"); box.set("other"); var x = box.get(); return x.length(); }
                public static int targeted() { Holder<String> box = new Holder<>(null); box.value = "yes"; return box.value.length(); }
                public static int inherited() { var box = new StringHolder("works"); var x = box.get(); return x.length(); }
                public static int nestedInference() { Holder<String> box = new Holder<>("nested"); var value = read(box); return value.length(); }
                public static long genericWideVarargs() { var x = first(9007199254740993L, 2L); return x + 1L; }
                public static int genericVarargs() { var x = first("one", "two"); return x.length(); }
            }
            """, false).getWasm();
        OverloadResolutionTest.execute(wasm, """
            assert.equal(e.overloadTies(), 2);
            assert.equal(e.wildcardReads(), 4); assert.equal(e.wildcardWrites(), 3);
            assert.equal(e.genericOverride(), 2);
            assert.equal(e.inferred(), 5); assert.equal(e.boxed(), 9007199254740994n);
            assert.equal(e.bounded(), 2.5); assert.equal(e.explicit(), 2);
            assert.equal(e.classes(), 5); assert.equal(e.targeted(), 3);
            assert.equal(e.inherited(), 5); assert.equal(e.nestedInference(), 6);
            assert.equal(e.genericVarargs(), 3); assert.equal(e.genericWideVarargs(), 9007199254740994n);
            """);
    }
    @Test void rejectsBoundViolationsAndWrongTypeArguments() {
        for (String call : new String[]{"amount(\"bad\")", "GenericApp.<String>amount(\"bad\")"}) {
            var error = assertThrows(IllegalArgumentException.class, () -> JavacToWasm.compile("Bounds.java",
                "class GenericApp { static <T extends Number> double amount(T x) { return x.doubleValue(); } public static double test() { return " + call + "; } }", false));
            assertTrue(error.getMessage().contains("bound"), error.getMessage());
            assertTrue(error.getMessage().contains("Bounds.java:"), error.getMessage());
        }
    }
    @Test void rejectsInvalidParameterizedAssignmentsAndCaptureWrites() {
        for (String body : new String[]{
                "Holder<String> x = new Holder<Integer>(1); return 0;",
                "Holder<? extends Number> x = new Holder<Integer>(1); x.set(2); return 0;",
                "Holder<? extends Number> x = new Holder<Integer>(1); x.value = 2; return 0;",
                "Holder<int> x = null; return 0;"}) {
            var error = assertThrows(IllegalArgumentException.class, () -> JavacToWasm.compile("InvalidGeneric.java",
                "class Holder<T> { T value; Holder(T x) { value=x; } public void set(T x) { value=x; } } class InvalidGeneric { public static int test() { " + body + " } }", false));
            assertTrue(error.getMessage().contains("InvalidGeneric.java:"), error.getMessage());
        }
    }
    @Test void rejectsNonReifiableTypeVariableConstruction() {
        var error = assertThrows(IllegalArgumentException.class, () -> JavacToWasm.compile("TypeVariable.java",
            "class TypeVariable { public static <T> T make() { return new T(); } }", false));
        assertTrue(error.getMessage().contains("type variable"), error.getMessage());
        assertTrue(error.getMessage().contains("TypeVariable.java:"), error.getMessage());
    }
}
