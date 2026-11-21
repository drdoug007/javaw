package one.dastec.javaw.components;

import one.dastec.javaw.JavacToWasm;
import one.dastec.javaw.components.compiler.ComponentExtensionAccess;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * M3 differential test: the same static strings and value sequences rendered by lit-html 3.3.2 and by
 * the javaw renderer, compared after every step (docs/web-components.md, "Validation strategy").
 * Java null in an attribute part is passed to lit-html as {@code nothing}, the documented difference.
 */
class TemplateDifferentialTest {
    // The raw text-block content of the outer template (between the delimiters, with its indentation).
    private static final String OUTER = """
                    <section class="s ${a} ${b}" data-n=${n} ?hidden=${on}>
                      <h1 title=${b}>${a}</h1>
                      <ul><li>${n}</li><li>${b}</li></ul>
                      <input .value=${a}>
                      <div>${mode.equals("x") ? html("<b class=${a}>${n}</b>") : mode.equals("y") ? html("<i>${b}</i> tail") : null}</div>
                      <table><tr><td>${a}</td></tr></table>
                      <p>&lt;${a}&gt; &amp; ${n}</p>
                    </section>
                    """;
    private static final String INNER_X = "<b class=${a}>${n}</b>";
    private static final String INNER_Y = "<i>${b}</i> tail";

    @Test
    void rendersLikeLitHtml() throws Exception {
        String source = """
            import one.dastec.javaw.components.CustomElement;
            import one.dastec.javaw.components.Property;
            import one.dastec.javaw.components.Template;
            import one.dastec.javaw.components.WebComponent;

            @CustomElement("x-diff")
            class Diff extends WebComponent {
                @Property private String a = "";
                @Property private String b;
                @Property private int n;
                @Property private boolean on;
                @Property private String mode = "x";
                @Override protected Template render() {
                    return html(\"""
            """ + OUTER + """
                        \""");
                }
            }
            """;
        byte[] wasm = JavacToWasm.compile("App.java", source, false).getWasm();
        Path lit = Path.of("../test-vendor/node/node_modules/lit-html/lit-html.js").toAbsolutePath().normalize();
        CustomElementTest.run(wasm, "", """
            const tick = async () => { await settle(); await settle(); };
            globalThis.window = window; globalThis.document = document;
            const lit = await import(require('node:url').pathToFileURL(%s).href);
            const strings = a => Object.freeze(Object.assign(a.slice(), { raw: Object.freeze(a.slice()) }));
            const OUTER = strings(%s), X = strings(%s), Y = strings(%s);
            const orNothing = v => v === null ? lit.nothing : v;
            // Value order follows the bindings in OUTER; attribute nulls become nothing.
            const litTemplate = s => lit.html(OUTER, s.a, orNothing(s.b), s.n, s.on, orNothing(s.b), s.a, s.n, s.b, s.a,
                s.mode === 'x' ? lit.html(X, s.a, s.n) : s.mode === 'y' ? lit.html(Y, s.b) : null, s.a, s.a, s.n);
            const clean = html => html.replace(/<!--[^]*?-->/g, '');
            const el = document.createElement('x-diff');
            document.body.appendChild(el);
            const container = document.createElement('div');
            document.body.appendChild(container);
            const states = [
                { a: '', b: null, n: 0, on: false, mode: 'x' },
                { a: 'hi', b: '<&"\\'>', n: 1, on: true, mode: 'x' },
                { a: 'hi', b: null, n: 2, on: true, mode: 'y' },
                { a: 'a b', b: 'z', n: -3, on: false, mode: 'none' },
                { a: '', b: '', n: 4, on: false, mode: 'x' },
                { a: 'x', b: 'y', n: 2147483647, on: true, mode: 'y' },
                { a: 'x', b: 'y', n: 2147483647, on: true, mode: 'y' },
                { a: 'ünï ☃', b: null, n: 0, on: false, mode: 'x' },
                { a: '', b: 'back', n: 7, on: false, mode: 'none' },
                { a: 'last', b: 'q', n: 8, on: true, mode: 'x' },
            ];
            for (let i = 0; i < states.length; i++) {
                const s = states[i];
                if (i > 0) Object.assign(el, s);
                await tick();
                lit.render(litTemplate(s), container);
                const ours = clean(el.shadowRoot.innerHTML), theirs = clean(container.innerHTML);
                assert.equal(ours, theirs, 'step ' + i + ' ' + JSON.stringify(s));
                assert.equal(el.shadowRoot.querySelector('input').value, container.querySelector('input').value, 'step ' + i + ' .value');
            }
            assert.equal(errors.length, 0, String(errors[0] && errors[0].stack));
            """.formatted(js(lit.toString()), array(OUTER, true), array(INNER_X, false), array(INNER_Y, false)));
    }

    private static String array(String raw, boolean textBlock) {
        List<String> statics = ComponentExtensionAccess.statics(raw, textBlock);
        return statics.stream().map(TemplateDifferentialTest::js).collect(Collectors.joining(",", "[", "]"));
    }

    private static String js(String s) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            if (c == '"' || c == '\\') out.append('\\').append(c);
            else if (c < 0x20 || c > 0x7e) out.append(String.format("\\u%04x", (int) c));
            else out.append(c);
        }
        return out.append('"').toString();
    }
}
