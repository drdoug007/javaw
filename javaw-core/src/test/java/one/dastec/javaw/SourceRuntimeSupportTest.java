package one.dastec.javaw;

import com.github.javaparser.StaticJavaParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SourceRuntimeSupportTest {
    @Test void stringReceiversHashCompareAndPrintWithoutAVtable() throws Exception {
        // Strings carry no object header, so these calls once trapped with "table index is out of bounds".
        byte[] wasm = JavacToWasm.compile("Strings.java", """
            class Box { int v; Box(int v) { this.v = v; }
                public int hashCode() { return v * 3; }
                public boolean equals(Object o) { if (!(o instanceof Box)) return false; Box b = (Box) o; return b.v == v; }
                public String toString() { return "Box" + v; } }
            class Strings {
                public static int literal() { return "numbers".hashCode(); }
                public static int local() { String s = "ab"; s = s + "cd"; return s.hashCode(); }
                public static int empty() { return "".hashCode(); }
                public static int nullString() { String s = null; try { return s.hashCode(); } catch (NullPointerException e) { return 7; } }
                public static int objectHash() { Object o = "numbers"; return o.hashCode(); }
                public static int objectEquals() { Object o = "numbers"; return (o.equals("numbers") ? 1 : 0) + (o.equals(new Box(1)) ? 2 : 0); }
                public static int objectString() { Object o = "numbers"; return o.toString().length(); }
                public static int sequenceHash() { CharSequence c = "numbers"; return c.hashCode(); }
                public static int objects() { Object o = new Box(5); return o.hashCode() + (o.equals(new Box(5)) ? 100 : 0) + (o.equals("x") ? 1000 : 0) + o.toString().length() * 10000; }
                public static int nullObject() { Object o = null; try { return o.hashCode(); } catch (NullPointerException e) { return 9; } }
            }
            """, false).getWasm();
        OverloadResolutionTest.execute(wasm, """
            assert.equal(e.literal(), %d);
            assert.equal(e.local(), %d);
            assert.equal(e.empty(), 0);
            assert.equal(e.nullString(), 7);
            assert.equal(e.objectHash(), %d);
            assert.equal(e.objectEquals(), 1);
            assert.equal(e.objectString(), 7);
            assert.equal(e.sequenceHash(), %d);
            assert.equal(e.objects(), 40115);
            assert.equal(e.nullObject(), 9);
            """.formatted("numbers".hashCode(), "abcd".hashCode(), "numbers".hashCode(), "numbers".hashCode()));
    }

    @Test void bindsInstanceofPatternsAndRecognizesStringTargets() throws Exception {
        // Patterns once dropped their binding and emitted invalid WAT; string type tests were always false.
        byte[] wasm = JavacToWasm.compile("Patterns.java", """
            class Box { int v; Box(int v) { this.v = v; } public boolean equals(Object o) { return o instanceof Box b && b.v == v; } }
            class Patterns {
                public static int equalBoxes() { return (new Box(3).equals(new Box(3)) ? 1 : 0) + (new Box(3).equals(new Box(4)) ? 2 : 0) + (new Box(3).equals("x") ? 4 : 0); }
                public static int stringBinding() { Object o = "hello"; if (o instanceof String s) return s.length(); return -1; }
                public static int negatedBinding() { Object o = new Box(9); if (!(o instanceof Box b)) return -1; return b.v; }
                public static int stringTargets() { Object s = "x", box = new Box(1), none = null;
                    return (s instanceof String ? 1 : 0) + (s instanceof CharSequence ? 2 : 0) + (s instanceof Object ? 4 : 0)
                        + (box instanceof String ? 8 : 0) + (box instanceof Object ? 16 : 0) + (none instanceof Object ? 32 : 0) + (none instanceof String ? 64 : 0); }
                public static int runtimeException() { try { Object o = null; return o.hashCode(); }
                    catch (NullPointerException e) { Object x = e; return (x instanceof String ? 1 : 0) + (x instanceof NullPointerException ? 2 : 0); } }
                public static int printedObject() { Object o = "abc"; return String.valueOf(o).length(); }
            }
            """, false).getWasm();
        OverloadResolutionTest.execute(wasm, """
            assert.equal(e.equalBoxes(), 1);
            assert.equal(e.stringBinding(), 5);
            assert.equal(e.negatedBinding(), 9);
            assert.equal(e.stringTargets(), 23);
            assert.equal(e.runtimeException(), 2);
            assert.equal(e.printedObject(), 3);
            """);
    }

    @Test void rejectsConstructsThatWouldOtherwiseBeSilentlyOmitted() {
        String[][] cases = {
            {"class C { public static int f() { return zz + 1; } }", "C.java:1:", "Unresolved name 'zz'"},
            {"class C { public static int f() { Object o = new int[2]; return o instanceof int[] ? 1 : 0; } }", "C.java:1:", "Unsupported instanceof array type int[]"},
            {"class A {} class B {}\nclass C { public static int f(Object o) { if (o instanceof A x) return 1; if (o instanceof B x) return 2; return 0; } }",
                "C.java:2:", "Unsupported reuse of pattern variable 'x'"},
        };
        for (String[] c : cases) {
            var error = assertThrows(IllegalArgumentException.class, () -> JavacToWasm.compile("C.java", c[0], false), c[0]);
            assertTrue(error.getMessage().contains(c[1]) && error.getMessage().contains(c[2]), error.getMessage());
        }
    }

    @Test void assertionsAreLazyAndErrorsDoNotMatchException() throws Exception {
        byte[] wasm = JavacToWasm.compile("Assertions.java", """
            class Assertions {
                static int checks; static int messages;
                static boolean check() { checks++; return false; }
                static int detail() { messages++; return 42; }
                public static int test() {
                    try { assert check() : detail(); }
                    catch (AssertionError failure) { if (!failure.getMessage().equals("42")) return -1; }
                    return checks * 10 + messages;
                }
                public static int trueCondition() { assert true : detail(); return messages; }
                public static int union() {
                    try { throw new IllegalStateException(); }
                    catch (IllegalArgumentException | IllegalStateException expected) { return 8; }
                }
                public static int hierarchy() {
                    try { try { assert false; } catch (Exception wrong) { return -1; } }
                    catch (Error expected) { return 7; }
                    return 0;
                }
            }
            """, false).getWasm();
        OverloadResolutionTest.execute(wasm, """
            assert.equal(e.test(), 0);
            e.__javaw_set_assertions(1);
            assert.equal(e.test(), 11);
            assert.equal(e.trueCondition(), 1);
            assert.equal(e.hierarchy(), 7);
            assert.equal(e.union(), 8);
            e.__javaw_set_assertions(0);
            assert.equal(e.test(), 11);
            """);
    }

    @Test void executesStaticNestedClasses() throws Exception {
        byte[] wasm = JavacToWasm.compile("Outer.java", """
            class Outer {
                static class Nested {
                    int value; Nested(int n) { value=n; }
                    public int read() { return value; }
                }
                public static int nested() { var n=new Nested(42); return n.read(); }
            }
            """,false).getWasm();
        OverloadResolutionTest.execute(wasm,"assert.equal(e.nested(),42);");
        var duplicate=StaticJavaParser.parse("class A { static class Item {} } class B { static class Item {} }");
        assertTrue(assertThrows(IllegalArgumentException.class,()->SourceRuntimeSupport.prepare(duplicate))
                .getMessage().contains("duplicate flattened type name"));
    }

    @Test void rejectsUnsupportedConstructsWithLocationAndRemediation() {
        for (String construct : new String[]{"void f() { synchronized(this) {} }", "synchronized void f() {}",
                "void f() { do {} while(false); }", "volatile int x;", "native void f();", "void f() { Object x = new Object() {}; }", "class Inner {}",
                "void f() { class Local {} }"}) {
            var cu = StaticJavaParser.parse("class Audit {\n" + construct + "\n}");
            JavaSourcePreparation.markSource(cu, "Audit.java");
            var error = assertThrows(IllegalArgumentException.class, () -> SourceRuntimeSupport.prepare(cu));
            assertTrue(error.getMessage().startsWith("Audit.java:2:"), error.getMessage());
            assertTrue(error.getMessage().contains("Unsupported"));
            assertTrue(error.getMessage().contains("; use") || error.getMessage().contains(": move") || error.getMessage().contains(": provide") || error.getMessage().contains(": rewrite"));
        }
    }
}
