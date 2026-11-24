package one.dastec.javaw.components;

import one.dastec.javaw.JavacToWasm;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** M3: template positions the tokenizer rejects, and type errors, reported at the binding's line and column. */
class TemplateDiagnosticsTest {
    private static final String HEAD = """
        import one.dastec.javaw.components.CustomElement;
        import one.dastec.javaw.components.Template;
        import one.dastec.javaw.components.WebComponent;
        @CustomElement("x-a")
        class A extends WebComponent {
            private String s = "";
            private int n;
        """;  // render() is declared on line 8

    /** A render() on line 8 whose text block content starts on line 9. */
    private static String block(String content) {
        return HEAD + "    protected Template render() { return html(\"\"\"\n" + content + "\n        \"\"\"); }\n}\n";
    }

    @Test
    void rejectedPositionsAreReportedAtTheBinding() {
        reject(block("        <p ${s + 1}></p>"), "App.java:9:12:", "element position");
        reject(block("        <p data-${s}=x></p>"), "App.java:9:17:", "attribute names");
        reject(block("        <${s}></p>"), "App.java:9:10:", "tag names");
        reject(block("        <p></${s}>"), "App.java:9:14:", "tag names");
        reject(block("        <!-- ${s} -->"), "App.java:9:14:", "comments");
        reject(block("        <style>p { color: ${s}; }</style>"), "App.java:9:27:", "<style>");
        reject(block("        <script>${s}</script>"), "App.java:9:17:", "<script>");
        reject(block("        <textarea>${s}</textarea>"), "App.java:9:19:", "<textarea>");
        reject(block("        <title>${s}</title>"), "App.java:9:16:", "<title>");
        reject(block("        <template>${s}</template>"), "App.java:9:19:", "<template>");
        reject(block("        <p class=a${s}></p>"), "App.java:9:19:", "unquoted");
        reject(block("        <p class=${s}a></p>"), "App.java:9:18:", "unquoted");
        reject(block("        <p ?hidden=\"x${n > 0}\"></p>"), "App.java:9:22:", "exactly one binding");
        reject(block("        <p @click=${null} .x=${s}${s}></p>"), "App.java:9:34:", "unquoted");
        reject(block("        <p ?hidden=${null}></p>"), "App.java:9:20:", "not null");
        reject(block("        <p>\n        ${s"), "App.java:10:9:", "Unterminated template binding");
        reject(block("        <p>${}</p>"), "App.java:9:12:", "Empty template binding");
        reject(block("        <p>${s +}</p>"), "App.java:9:12:", "Invalid binding expression");
        reject(block("        <p class=\"x"), "App.java:8:47:", "ends inside a tag");
        reject(block("        <p>wc$1</p>"), "App.java:8:47:", "reserved");
        reject(HEAD + "    protected Template render() { String t = \"<p></p>\"; return html(t); }\n}\n", "App.java:8:69:", "string literal or text block");
        reject(block("        <textarea/>${s}</textarea>"), "App.java:9:20:", "<textarea>");
        reject(block("        <xmp>${s}</xmp>"), "App.java:9:14:", "<xmp>");
        reject(block("        <iframe>${s}</iframe>"), "App.java:9:17:", "<iframe>");
        reject(block("        <!-- a --!><p ${\"x\"}></p>"), "App.java:9:23:", "element position");
        // U+2003 is not HTML whitespace, so it stays part of the tag name.
        reject(block("        <div\u2003${s}></div>"), "App.java:9:14:", "tag names");
        // Whitespace after the opening delimiter is not content and does not shift lines.
        reject(HEAD + "    protected Template render() { return html(\"\"\"   \n        <p>${s +}</p>\n        \"\"\"); }\n}\n",
                "App.java:9:12:", "Invalid binding expression");
        // One-line literals: columns follow the literal on its line.
        reject(HEAD + "    protected Template render() { return html(\"<p ${() -> s}>\"); }\n}\n", "App.java:8:51:", "element position");
        // A binding that spans lines is reported where it starts.
        reject(block("        <p>${s.equals(\"a\")\n            ? 1 : }</p>"), "App.java:9:12:", "Invalid binding expression");
    }

    @Test
    void bindingTypeErrorsPointAtTheExpression() {
        // A boolean attribute needs a boolean: the core compiler rejects Parts.bool(String) at the binding.
        var error = assertThrows(IllegalArgumentException.class,
                () -> JavacToWasm.compile("App.java", block("        <p>ok</p>\n        <p ?hidden=${s}></p>"), false));
        assertTrue(error.getMessage().contains("App.java:10:22: No applicable overload"), error.getMessage());
    }

    private static void reject(String source, String position, String fragment) {
        var error = assertThrows(IllegalArgumentException.class, () -> JavacToWasm.compile("App.java", source, false), source);
        assertTrue(error.getMessage().startsWith(position) && error.getMessage().contains(fragment),
                "expected " + position + " ... " + fragment + " but got: " + error.getMessage() + "\n" + source);
    }

    @Test
    void propertyBindingsOnJavaComponentsAreChecked() {
        String source = """
            import one.dastec.javaw.components.CustomElement;
            import one.dastec.javaw.components.Property;
            import one.dastec.javaw.components.Template;
            import one.dastec.javaw.components.WebComponent;
            @CustomElement("x-kid") class Kid extends WebComponent { @Property private int count; @Property private String label; }
            @CustomElement("x-mom") class Mom extends WebComponent {
                protected Template render() { return html("<x-kid .%s=${%s}></x-kid>"); }
            }
            """;
        reject(source.formatted("size", "1"), "App.java:7:61:", "<x-kid> (Kid) has no @Property 'size'; its properties are count, label");
        var error = assertThrows(IllegalArgumentException.class, () -> JavacToWasm.compile("App.java", source.formatted("count", "true"), false));
        assertTrue(error.getMessage().contains("App.java:7:64:"), "a mistyped value is reported at the expression: " + error.getMessage());
    }

    @Test
    void ownMethodsNamedLikeDirectivesAreNotDirectives() {
        // A component's own ref/classMap/styleMap shadows the inherited directive, as in Java.
        String own = HEAD + """
                String ref(int id) { return "#" + id; }
                String classMap(String c) { return c; }
                String styleMap(String c) { return c; }
                protected Template render() { return html("<a href=${ref(n)} title=${classMap(s)}>${styleMap(s)}</a>"); }
            }
            """;
        assertDoesNotThrow(() -> JavacToWasm.compile("App.java", own, false));
        // The inherited directive is still checked.
        reject(block("        <a href=${ref(null)}></a>"), "App.java:9:17:", "element position");
    }
}
