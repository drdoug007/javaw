package one.dastec.javaw.components;

import one.dastec.javaw.JavacToWasm;
import one.dastec.javaw.components.compiler.ComponentExtensionAccess;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** M5 styles: style-field discovery, shared adoptedStyleSheets, the <style> fallback and diagnostics. */
class StylesTest {
    static final String PROGRAM = """
        import one.dastec.javaw.components.Css;
        import one.dastec.javaw.components.CustomElement;
        import one.dastec.javaw.components.Template;
        import one.dastec.javaw.components.WebComponent;
        import static one.dastec.javaw.components.Templates.css;

        final class Theme {
            static final Css SHARED = css(".shared { color: teal; }");
            static final Css ALIAS = SHARED;
        }

        abstract class Themed extends WebComponent {
            static final Css BASE = css(":host { display: block; }");
        }

        @CustomElement("x-card")
        class Card extends Themed {
            static final Css CARD = WebComponent.css(\"""
                p {
                  margin: 0;
                }
                p::before { content: "\\\\2022"; }
                \""");
            // Already in the list through Themed: kept at its last position, as Lit does.
            private static final Css AGAIN = BASE;
            public static final Css SHARED = Theme.ALIAS;
            @Override protected Template render() { return html("<p>${CARD.cssText()}</p>"); }
        }

        @CustomElement("x-wide-card")
        class WideCard extends Card {
            protected static final Css WIDE = css("p { width: 100%; }");
        }

        @CustomElement("x-bare")
        class Bare extends WebComponent {
            @Override protected Template render() { return html("<p>bare</p>"); }
        }
        """;

    @Test
    void styleFieldsAreDiscoveredInOrder() {
        Map<String, String> sections = new LinkedHashMap<>();
        ComponentExtensionAccess.rewriteUserTypes(PROGRAM, sections);
        String components = sections.get("javaw_wc_components");
        // Call sites: 0 Theme.SHARED, 1 Themed.BASE, 2 Card.CARD, 3 WideCard.WIDE.
        assertTrue(components.contains("\"tag\":\"x-card\"") && components.contains("\"styles\":[2, 1, 0]"), components);
        assertTrue(components.contains("\"styles\":[2, 1, 0, 3]"), components);
        assertTrue(components.contains("\"styles\":[]"), components);
        String styles = sections.get("javaw_wc_styles");
        assertTrue(styles.contains("{\"id\":2,\"css\":\"p {\\u000a  margin: 0;\\u000a}\\u000ap::before { content: \\\"\\\\2022\\\"; }\\u000a\"}"), styles);
    }

    @Test
    void sheetsAreSharedAndAdoptedSuperclassFirst() throws Exception {
        byte[] wasm = JavacToWasm.compile("App.java", PROGRAM, false).getWasm();
        CustomElementTest.run(wasm, "", """
            const tick = async () => { await settle(); await settle(); await settle(); };
            const a = document.createElement('x-card'), b = document.createElement('x-card');
            const w = document.createElement('x-wide-card'), bare = document.createElement('x-bare');
            document.body.append(a, b, w, bare);
            await tick();
            const texts = el => el.shadowRoot.adoptedStyleSheets.map(s => [...s.cssRules].map(r => r.cssText).join(' '));
            assert.equal(a.shadowRoot.adoptedStyleSheets.length, 3);
            assert.ok(texts(a)[0].includes('margin'), texts(a).join(' / '));
            assert.ok(texts(a)[1].includes(':host'), texts(a).join(' / '));
            assert.ok(texts(a)[2].includes('teal'), texts(a).join(' / '));
            for (let i = 0; i < 3; i++) {
              assert.equal(a.shadowRoot.adoptedStyleSheets[i], b.shadowRoot.adoptedStyleSheets[i], 'one sheet per css(...) call site');
              assert.equal(a.shadowRoot.adoptedStyleSheets[i], w.shadowRoot.adoptedStyleSheets[i], 'inherited sheets are the same objects');
            }
            assert.equal(w.shadowRoot.adoptedStyleSheets.length, 4);
            assert.ok(texts(w)[3].includes('width'));
            assert.equal(bare.shadowRoot.adoptedStyleSheets.length, 0);
            assert.equal(a.shadowRoot.querySelectorAll('style').length, 0, 'no <style> elements with constructable sheets');
            assert.equal(a.shadowRoot.querySelector('p').textContent, 'p {\\n  margin: 0;\\n}\\np::before { content: "\\\\2022"; }\\n',
              'Css.cssText() is the processed text block');
            // Reconnecting does not adopt the sheets again.
            a.remove(); document.body.appendChild(a); await tick();
            assert.equal(a.shadowRoot.adoptedStyleSheets.length, 3);
            """);
    }

    @Test
    void withoutConstructableSheetsEachRootGetsStyleElements() throws Exception {
        byte[] wasm = JavacToWasm.compile("App.java", PROGRAM, false).getWasm();
        CustomElementTest.run(wasm, "window.CSSStyleSheet = undefined;", """
            const tick = async () => { await settle(); await settle(); await settle(); };
            const a = document.createElement('x-wide-card');
            document.body.append(a);
            await tick();
            const styles = [...a.shadowRoot.querySelectorAll('style')].map(s => s.textContent);
            assert.equal(styles.length, 4);
            assert.ok(styles[0].includes('margin') && styles[1].includes(':host') && styles[2].includes('teal') && styles[3].includes('width'), styles.join(' / '));
            assert.equal(a.shadowRoot.querySelector('p').textContent.includes('margin'), true, 'the template still renders');
            a.remove(); document.body.appendChild(a); await tick();
            assert.equal(a.shadowRoot.querySelectorAll('style').length, 4, 'styles are added once per shadow root');
            """);
    }

    private static final String IMPORTS = """
        import one.dastec.javaw.components.Css;
        import one.dastec.javaw.components.CustomElement;
        import one.dastec.javaw.components.WebComponent;
        """;

    @Test
    void misuseIsDiagnosed() {
        reject("@CustomElement(\"x-a\") class A extends WebComponent { static final Css S = css(\"a\" + \"b\"); }", "requires a string literal");
        reject("@CustomElement(\"x-a\") class A extends WebComponent { static String t = \"p{}\"; static final Css S = css(t); }", "no unsafeCSS");
        reject("@CustomElement(\"x-a\") class A extends WebComponent { static final Css S = css(\"p { color: ${c}; }\"); }", "bindings");
        reject("@CustomElement(\"x-a\") class A extends WebComponent { static final Css S = css(); }", "exactly one");
        reject("@CustomElement(\"x-a\") class A extends WebComponent { static Css S = css(\"p{}\"); }", "Style field 'S' of A must be static final");
        reject("@CustomElement(\"x-a\") class A extends WebComponent { final Css s = css(\"p{}\"); }", "must be static final");
        reject("@CustomElement(\"x-a\") class A extends WebComponent { static final Css S = make(); static Css make() { return css(\"p{}\"); } }",
                "App.java:5:", "must be initialized with css(\"...\") or a reference");
        reject("@CustomElement(\"x-a\") class A extends WebComponent { static final Css S = true ? css(\"a{}\") : css(\"b{}\"); }",
                "must be initialized with css");
        reject("@CustomElement(\"x-a\") class A extends WebComponent { static final Css[] S = { css(\"p{}\") }; }", "declare one field per style sheet");
        reject("abstract class B extends WebComponent { static final Css S; static { S = css(\"p{}\"); } }\n"
                + "@CustomElement(\"x-a\") class A extends B {}", "needs an initializer");
        reject("interface T { Css X = WebComponent.css(\"p{}\"); }\n"
                + "@CustomElement(\"x-a\") class A extends WebComponent { static final Css S = T.X; }",
                "Style field 'S' refers to T.X: style fields of interfaces are not supported");
        reject("class Css {}", "reserved");
    }

    @Test
    void cssWorksOutsideStyleFields() throws Exception {
        // css(...) values outside style fields are plain values: valid, but not adopted.
        byte[] wasm = JavacToWasm.compile("App.java", IMPORTS + """
            import one.dastec.javaw.components.Template;
            @CustomElement("x-a") class A extends WebComponent {
                @Override protected Template render() { Css local = css("b { }"); return html("<p>${local.toString()}</p>"); }
            }
            """, false).getWasm();
        CustomElementTest.run(wasm, "", """
            const a = document.createElement('x-a');
            document.body.append(a);
            await settle(); await settle(); await settle();
            assert.equal(a.shadowRoot.querySelector('p').textContent, 'b { }');
            assert.equal(a.shadowRoot.adoptedStyleSheets.length, 0);
            """);
    }

    private static void reject(String body, String... fragments) {
        var error = assertThrows(IllegalArgumentException.class, () -> JavacToWasm.compile("App.java", IMPORTS + "\n" + body, false));
        for (String fragment : fragments) assertTrue(error.getMessage().contains(fragment), error.getMessage());
    }
}
