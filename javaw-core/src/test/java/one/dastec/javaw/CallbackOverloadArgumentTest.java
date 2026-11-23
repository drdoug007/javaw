package one.dastec.javaw;

import org.junit.jupiter.api.Test;

/**
 * Overloads with the same callback shape that differ in another parameter's type
 * ({@code listen(JSObject, String, Runnable)} and {@code listen(Element, String, Runnable)}) were an
 * "Ambiguous callback target" error; the other arguments' static types now select one. Same results as
 * the JVM.
 */
class CallbackOverloadArgumentTest {
    interface Act { void run(); }
    static final class Box { int n; }
    static final class Tag { int n; }
    static int pick(Box target, Act act) { act.run(); return 1; }
    static int pick(Tag target, Act act) { act.run(); return 20; }
    static int runs;
    static int jvm() {
        Box box = new Box();
        return pick(box, () -> runs++) + pick(new Tag(), () -> runs += 100) + runs;
    }

    @Test
    void otherArgumentsSelectTheOverload() throws Exception {
        runs = 0;
        int expected = jvm();
        String program = """
            interface Act { public void run(); }
            class Box { int n; Box() {} }
            class Tag { int n; Tag() {} }
            class App {
                static int runs;
                static int pick(Box target, Act act) { act.run(); return 1; }
                static int pick(Tag target, Act act) { act.run(); return 20; }
                public static int test() {
                    Box box = new Box();
                    return pick(box, () -> runs++) + pick(new Tag(), () -> runs += 100) + runs;
                }
            }
            """;
        byte[] wasm = JavacToWasm.compile("App.java", program, false).getWasm();
        OverloadResolutionTest.execute(wasm, "assert.equal(e.test(), " + expected + ");");
    }
}
