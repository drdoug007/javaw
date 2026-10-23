package com.example.javaw;

import com.github.javaparser.StaticJavaParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SourceRuntimeSupportTest {
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
