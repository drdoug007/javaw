package one.dastec.javaw.components;

import one.dastec.javaw.JavacToWasm;
import one.dastec.javaw.components.compiler.ComponentExtensionAccess;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Compile-time diagnostics for @CustomElement classes and @Query fields. */
class ComponentDiagnosticsTest {
    private static final String IMPORTS = """
        import one.dastec.javaw.components.CustomElement;
        import one.dastec.javaw.components.Query;
        import one.dastec.javaw.components.WebComponent;
        import one.dastec.javaw.dom.Element;
        """;

    @Test
    void tagNamesFollowTheCustomElementNameRules() {
        assertNull(ComponentExtensionAccess.tagProblem("todo-list"));
        assertNull(ComponentExtensionAccess.tagProblem("x-1.2_b"));
        assertNull(ComponentExtensionAccess.tagProblem("math-α"));
        assertTrue(ComponentExtensionAccess.tagProblem("todolist").contains("hyphen"));
        assertTrue(ComponentExtensionAccess.tagProblem("Todo-list").contains("lowercase letter"));
        assertTrue(ComponentExtensionAccess.tagProblem("todo-List").contains("uppercase"));
        assertTrue(ComponentExtensionAccess.tagProblem("1-x").contains("lowercase letter"));
        assertTrue(ComponentExtensionAccess.tagProblem("font-face").contains("reserved"));
        assertTrue(ComponentExtensionAccess.tagProblem("my element-x").contains("U+0020"));
    }

    @Test
    void invalidComponentsAreRejectedWithPositions() {
        reject("@CustomElement(\"counter\") class C extends WebComponent {}", "App.java:6:", "must contain a hyphen");
        reject("@CustomElement(\"x-a\") class A extends WebComponent {}\n@CustomElement(\"x-a\") class B extends WebComponent {}",
                "Custom element <x-a> is already defined by A");
        reject("@CustomElement(\"x-a\") class A {}", "must extend WebComponent");
        reject("@CustomElement(\"x-a\") abstract class A extends WebComponent {}", "must not be abstract");
        reject("@CustomElement(\"x-a\") class A extends WebComponent { public A(int n) {} }", "no-argument constructor");
        reject("@CustomElement(value = \"x-a\", observedAttributes = {\"Start\"}) class A extends WebComponent {}", "lowercase");
        reject("@CustomElement(\"x-a\") class A extends WebComponent { @Query(\"b\") private Element b; void f() { b = null; } }",
                "@Query field 'b' is read-only");
        reject("@CustomElement(\"x-a\") class A extends WebComponent { @Query(\"b\") Element b; }", "must be a private instance field");
        reject("@CustomElement(\"x-a\") class A extends WebComponent { @Query(\"b\") private String b; }", "must have type Element");
        reject("class WebComponent {}", "reserved");
        reject("class Outer { @CustomElement(\"x-a\") class A extends WebComponent {} }", "App.java:6:",
                "Unsupported non-static inner/local type");
        reject("class Outer { static void f() { @CustomElement(\"x-a\") class A extends WebComponent {} } }", "Unsupported non-static inner/local type");
        reject("class P { @CustomElement(\"x-a\") static class Row extends WebComponent {} }\n"
                + "class Q { @CustomElement(\"x-b\") static class Row extends WebComponent {} }",
                "Unsupported duplicate flattened type name 'Row'");
        reject("@CustomElement(\"x-a\") class A extends WebComponent { @CustomElement(\"x-b\") static class B extends WebComponent {} }",
                "must not be nested in the component class A");
        reject("abstract class Base extends WebComponent { @CustomElement(\"x-b\") static class B extends WebComponent {} }",
                "must not be nested in the component class Base");
    }

    @Test
    void constructionOtherThanNewIsRejected() {
        reject("@CustomElement(\"x-a\") class A extends WebComponent {}\nclass App { static java.util.function.Supplier<A> s = A::new; }",
                "not a constructor reference");
        reject("@CustomElement(\"x-a\") class A extends WebComponent {}\nclass Sub extends A {}\nclass App { static void f() { new Sub(); } }",
                "Sub extends WebComponent but has no @CustomElement");
    }

    @Test
    void queryReadsFollowJavaScoping() {
        String rewritten = ComponentExtensionAccess.rewrite(IMPORTS + """
            @CustomElement("x-a") class A extends WebComponent {
                @Query("button") private Element button;
                void before() { button.setInnerText("a"); Element button = null; button.remove(); }
                void loop(Element[] items) { button.remove(); for (Element button : items) button.remove(); }
                void caught() { try { button.remove(); } catch (RuntimeException button) { button.getMessage(); } }
                void lambda() { java.util.function.Consumer<Element> c = button -> button.remove(); c.accept(this.button); }
            }
            """);
        assertEquals(3, count(rewritten, "this.renderRoot().querySelector(\"button\").remove()")
                + count(rewritten, "this.renderRoot().querySelector(\"button\").setInnerText(\"a\")"), rewritten);
        assertTrue(rewritten.contains("c.accept(this.renderRoot().querySelector(\"button\"))"), rewritten);
        assertTrue(rewritten.contains("button.getMessage()"), "a catch parameter shadows the field: " + rewritten);
        assertTrue(rewritten.contains("Element button = null;\n        button.remove();"), "a later local shadows only after its declaration: " + rewritten);
        assertTrue(rewritten.contains("for (Element button : items) button.remove();"), rewritten);
        assertTrue(rewritten.contains("button -> button.remove()"), rewritten);

        reject("@CustomElement(\"x-a\") class A extends WebComponent { @Query(\"b\") private Element b; void f(Object o) { if (o instanceof Element b) b.remove(); } }",
                "Pattern variable 'b'");
        reject("@CustomElement(\"x-a\") class A extends WebComponent { @Query(\"b\") private Element b; Object o; { if (o instanceof Element b) b.remove(); } }",
                "Pattern variable 'b'");
        String more = ComponentExtensionAccess.rewrite(IMPORTS + """
            @CustomElement("x-a") class A extends WebComponent {
                @Query("q") private Element q;
                void same() { Element q = null, b = q; }
                void resources() throws Exception { try (AutoCloseable q = () -> {}; AutoCloseable b = q) { } }
                void groups(int k, Element x) { switch (k) { case 1: Element q = x; break; case 2: q = x; q.remove(); break; } }
            }
            """);
        assertFalse(more.contains("querySelector"), "every q above is a local: " + more);
        reject("@CustomElement(\"x-a\") class Gen<T> extends WebComponent {}", "must not be generic");
        reject("abstract class Base<T> extends WebComponent {}\n@CustomElement(\"x-a\") class A extends Base<String> {}\n"
                + "class App { static java.util.function.Supplier<Base<String>> s = Base<String>::new; }", "not a constructor reference");
        reject("@CustomElement(\"x-a\") class A extends WebComponent { @Query(\"b\") private Element b; void f(A other) { other.b.remove(); } }",
                "can only be read as 'b' or 'this.b'");
        reject("@CustomElement(\"x-a\") class A extends WebComponent { @Query(\"b\") private Element b; static class N { void f() { b.remove(); } } }",
                "cannot be read from a nested class");
    }

    @Test
    void unrelatedTypesNamedWebComponentAreLeftAlone() {
        assertNotNull(JavacToWasm.compile("App.java", "class WebComponent { public static int f() { return 1; } }", false).getWasm());
        assertNotNull(JavacToWasm.compile("App.java", "@interface CustomElement { String value(); }\n@CustomElement(\"x\") class Thing { }", false).getWasm());
    }

    @Test
    void qualifiedApiNamesWorkWithoutImports() {
        assertNotNull(JavacToWasm.compile("App.java", """
            @one.dastec.javaw.components.CustomElement("x-e")
            class E extends one.dastec.javaw.components.WebComponent {
                @Override protected void connected() { host().setAttribute("a", "b"); }
            }
            """, false).getWasm());
    }

    private static int count(String text, String fragment) {
        int n = 0;
        for (int i = text.indexOf(fragment); i >= 0; i = text.indexOf(fragment, i + 1)) n++;
        return n;
    }

    @Test
    void missingExtensionIsReportedNotMiscompiled() {
        // With the extension on the classpath this compiles; the core-only path is covered in javaw-core.
        assertNotNull(JavacToWasm.compile("App.java", IMPORTS + "@CustomElement(\"x-ok\") class Ok extends WebComponent {}", false).getWasm());
    }

    private static void reject(String body, String... fragments) {
        var error = assertThrows(IllegalArgumentException.class, () -> JavacToWasm.compile("App.java", IMPORTS + "\n" + body, false));
        for (String fragment : fragments) assertTrue(error.getMessage().contains(fragment), error.getMessage());
    }
}
