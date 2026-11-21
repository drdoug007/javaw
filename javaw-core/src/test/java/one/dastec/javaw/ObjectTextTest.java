package one.dastec.javaw;

import org.junit.jupiter.api.Test;

/**
 * Text and equality of references held as Object: boxes, enums, user objects and null in string
 * concatenation, String.valueOf and toString; primitive arguments to equals(Object); primitives
 * returned through an Object-returning functional interface. Each check matches the JVM.
 */
class ObjectTextTest {
    @Test
    void referencesConvertAndCompareLikeTheJvm() throws Exception {
        byte[] wasm = JavacToWasm.compile("App.java", """
            interface KeyFunction<T> { Object key(T item); }
            final class Todo { private final int id; Todo(int id) { this.id = id; } int id() { return id; } public String toString() { return "T" + id; } }
            enum Tone { LOW, HIGH }
            class App {
                static Object f(int x) { return x; }
                public static int test() {
                    int bad = 0;
                    Object r = f(3);
                    if (!r.equals(Integer.valueOf(3))) bad += 1;
                    if (!("" + r).equals("3")) bad += 2;
                    if (!String.valueOf(r).equals("3")) bad += 4;
                    if (!r.toString().equals("3")) bad += 8;
                    KeyFunction<Todo> l = t -> t.id();
                    Object k = l.key(new Todo(6));
                    if (!k.equals(6)) bad += 16;
                    Object o = 7;
                    if (!o.equals(7)) bad += 32;
                    Object d = 2.5;
                    if (!("" + d).equals("2.5")) bad += 64;
                    Object t = new Todo(4);
                    if (!("x" + t).equals("xT4")) bad += 128;
                    Todo none = null;
                    if (!("x" + none).equals("xnull")) bad += 256;
                    if (!("" + Tone.HIGH).equals("HIGH")) bad += 512;
                    Object c = 'c';
                    if (!("" + c).equals("c")) bad += 1024;
                    Object b = true;
                    if (!("" + b).equals("true")) bad += 2048;
        if (!Integer.toString(-5).equals("-5") || !Long.toString(1L << 40).equals("1099511627776")) bad += 4096;
        if (!Boolean.toString(true).equals("true") || !Character.toString('q').equals("q")) bad += 8192;
        if (!Double.toString(0.1).equals("0.1") || !Float.toString(1.1f).equals("1.1")) bad += 16384;
                    return bad == 0 ? 1 : -bad;
                }
            }
            """, false).getWasm();
        OverloadResolutionTest.execute(wasm, "assert.equal(e.test(), 1);");
    }

    @Test
    void instanceofFollowsSuperinterfaces() throws Exception {
        byte[] wasm = JavacToWasm.compile("App.java", """
            import java.util.ArrayList;
            import java.util.List;
            class App {
                public static int test() {
                    Object v = new ArrayList<Integer>();
                    int bad = 0;
                    if (!(v instanceof Iterable<?>)) bad += 1;
                    if (!(v instanceof Iterable<?> it)) bad += 2;
                    if (!(v instanceof List<?>)) bad += 4;
                    if (!(v instanceof ArrayList<?>)) bad += 8;
                    Object s = "x";
                    if (s instanceof Iterable<?>) bad += 16;
                    return bad == 0 ? 1 : -bad;
                }
            }
            interface A {}
            interface B {}
            interface C extends A, B {}
            class Impl implements C {}
            class App2 {
                public static int test2() {
                    Object o = new Impl();
                    int bad = 0;
                    if (!(o instanceof A)) bad += 1;
                    if (!(o instanceof B)) bad += 2;
                    if (!(o instanceof C)) bad += 4;
                    return bad == 0 ? 1 : -bad;
                }
            }
            """, false).getWasm();
        OverloadResolutionTest.execute(wasm, "assert.equal(e.test(), 1); assert.equal(e.test2(), 1);");
    }
}
