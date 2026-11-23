package one.dastec.javaw.components;

import one.dastec.javaw.JavacToWasm;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** M4: classMap, styleMap and ref (element parts), with differential DOM checks against lit-html's directives. */
class TemplateDirectiveTest {
    static final String PROGRAM = """
        import java.util.ArrayList;
        import java.util.HashMap;
        import java.util.List;
        import java.util.Map;
        import one.dastec.javaw.components.Changes;
        import one.dastec.javaw.components.CustomElement;
        import one.dastec.javaw.components.Property;
        import one.dastec.javaw.components.Ref;
        import one.dastec.javaw.components.Template;
        import one.dastec.javaw.components.WebComponent;
        import one.dastec.javaw.dom.Element;

        @CustomElement("x-dir")
        class Directives extends WebComponent {
            /** "a,!b" is {a: true, b: false}. */
            @Property private String classes = "";
            /** "color=red|--gap=|font-size=2px" ; an empty value is null. */
            @Property private String styles = "";
            @Property private boolean swap;
            @Property private boolean nested = true;
            @Property private String refs = "";
            /** Only triggers an update. */
            @Property private int bump;
            private final Ref<Element> input = new Ref<Element>();
            private final Ref<Element> other = new Ref<Element>();
            private final Ref<Element> inner = new Ref<Element>();
            @Override protected Template render() {
                return html(\"""
                    <p class="static ${classMap(classValues())}" style="${styleMap(styleValues())}">x</p>
                    <input ${ref(swap ? other : input)} value="v" data-k="input">
                    <div>${nested ? html("<span data-k=span ${ref(inner)}>s</span>") : null}</div>
                    \""");
            }
            @Override protected void updated(Changes changes) {
                refs = describe(input) + "," + describe(other) + "," + describe(inner);
            }
            private static String describe(Ref<Element> ref) {
                Element e = ref.value();
                return e == null ? "null" : e.getAttribute("data-k");
            }
            private Map<String, Boolean> classValues() {
                Map<String, Boolean> map = new HashMap<String, Boolean>();
                for (String part : split(classes, ',')) {
                    if (part.startsWith("!")) map.put(part.substring(1), Boolean.FALSE); else map.put(part, Boolean.TRUE);
                }
                return map;
            }
            private Map<String, String> styleValues() {
                Map<String, String> map = new HashMap<String, String>();
                for (String part : split(styles, '|')) {
                    int eq = part.indexOf("=");
                    String value = part.substring(eq + 1);
                    map.put(part.substring(0, eq), value.isEmpty() ? null : value);
                }
                return map;
            }
            private static List<String> split(String csv, char separator) {
                List<String> out = new ArrayList<String>();
                String current = "";
                for (int i = 0; i < csv.length(); i++) {
                    char c = csv.charAt(i);
                    if (c == separator) { out.add(current); current = ""; } else current = current + c;
                }
                if (!csv.isEmpty()) out.add(current);
                return out;
            }
        }
        """;

    @Test
    void classMapStyleMapAndRefMatchLitHtml() throws Exception {
        byte[] wasm = JavacToWasm.compile("App.java", PROGRAM, false).getWasm();
        Path lit = Path.of("../test-vendor/node/node_modules/lit-html").toAbsolutePath().normalize();
        CustomElementTest.run(wasm, "", """
            const tick = async () => { await settle(); await settle(); await settle(); };
            globalThis.window = window; globalThis.document = document;
            const litBase = require('node:url').pathToFileURL(%s).href;
            const lit = await import(litBase + '/lit-html.js');
            const { classMap } = await import(litBase + '/directives/class-map.js');
            const { styleMap } = await import(litBase + '/directives/style-map.js');
            const { ref, createRef } = await import(litBase + '/directives/ref.js');
            const el = document.createElement('x-dir');
            document.body.appendChild(el);
            await tick();
            const container = document.createElement('div');
            document.body.appendChild(container);
            const classesOf = csv => Object.fromEntries((csv ? csv.split(',') : []).map(p => p.startsWith('!') ? [p.slice(1), false] : [p, true]));
            const stylesOf = csv => Object.fromEntries((csv ? csv.split('|') : []).map(p => { const i = p.indexOf('='); const v = p.slice(i + 1); return [p.slice(0, i), v === '' ? null : v]; }));
            const litRender = (classes, styles) => lit.render(lit.html`<p class="static ${classMap(classesOf(classes))}" style="${styleMap(stylesOf(styles))}">x</p>`, container);
            // Java maps iterate in hash order and lit-html objects in insertion order, so token and declaration order is normalized.
            const norm = p => {
              const cls = (p.getAttribute('class') || '').split(/\\s+/).filter(Boolean).sort().join(' ');
              const sty = (p.getAttribute('style') || '').split(';').map(s => s.trim()).filter(Boolean).sort().join(';');
              return cls + ' | ' + sty + ' | ' + p.hasAttribute('class') + p.hasAttribute('style');
            };
            const steps = [
              ['', ''],
              ['a,b', 'color=red'],
              ['a,!b,c', 'color=blue|--gap=4px|fontSize=12px'],
              ['static,c', 'color=|--gap=8px|fontSize=12px'],
              ['!static,d', 'backgroundColor=green !important|webkitTransform=none'],
              ['', '--gap=1px'],
              ['b,a', 'font-size=3px|color=red !important'],
              ['b,a', 'font-size=3px|color=red !important'],
            ];
            const jp = () => el.shadowRoot.querySelector('p');
            for (const [classes, styles] of steps) {
              el.classes = classes; el.styles = styles;
              await tick();
              litRender(classes, styles);
              assert.equal(norm(jp()), norm(container.querySelector('p')), 'same DOM as lit-html for ' + classes + ' / ' + styles);
            }
            assert.ok(jp().classList.contains('static'), 'the static class stays');
            // One key each, so the first render text is compared exactly.
            const fresh = document.createElement('x-dir');
            fresh.classes = 'only'; fresh.styles = 'marginTop=1px';
            document.body.appendChild(fresh);
            await tick();
            const c2 = document.createElement('div');
            lit.render(lit.html`<p class="static ${classMap({ only: true })}" style="${styleMap({ marginTop: '1px' })}">x</p>`, c2);
            assert.equal(fresh.shadowRoot.querySelector('p').outerHTML, c2.querySelector('p').outerHTML);

            // Classes added by other code are kept, and an unchanged update makes no host calls.
            jp().classList.add('ext');
            el.classes = 'a'; await tick();
            assert.ok(jp().classList.contains('ext'), 'classMap keeps classes it did not add');
            const names = ['wcClassMap', 'wcClassToggle', 'wcStyleMap', 'wcStyleSet', 'wcStyleRemove', 'wcElement', 'wcAttr'];
            let calls = 0;
            for (const n of names) { const f = r[n]; r[n] = function (...a) { calls++; return f.apply(this, a); }; }
            el.bump = 1; await tick();
            assert.equal(calls, 0, 'unchanged maps and refs make no host calls');
            el.classes = 'a,z'; await tick();
            assert.equal(calls, 1, 'one added class is one host call');

            // ref assigns the element (a scoped handle), moves to the new Ref and clears with its template.
            const input = el.shadowRoot.querySelector('input');
            assert.equal(el.refs, 'input,null,span');
            el.swap = true; await tick();
            assert.equal(el.refs, 'null,input,span', 'the old Ref is cleared when the binding gets another Ref');
            el.nested = false; await tick();
            assert.equal(el.refs, 'null,input,null', 'removing the template clears its Ref');
            el.nested = true; await tick();
            assert.equal(el.refs, 'null,input,span');
            assert.equal(el.shadowRoot.querySelector('input'), input);
            assert.equal(errors.length, 0, String(errors[0] && errors[0].stack));

            // The same ref steps with lit-html.
            const a = createRef(), b = createRef(), inner = createRef();
            const refRender = (swap, nested) => lit.render(lit.html`<input ${ref(swap ? b : a)} value="v" data-k="input"><div>${nested ? lit.html`<span data-k=span ${ref(inner)}>s</span>` : null}</div>`, c2);
            const tags = () => [a, b, inner].map(x => x.value ? x.value.localName : 'null').join(',');
            refRender(false, true); assert.equal(tags(), 'input,null,span');
            refRender(true, true); assert.equal(tags(), 'null,input,span');
            refRender(true, false); assert.equal(tags(), 'null,input,null');
            refRender(true, true); assert.equal(tags(), 'null,input,span');
            """.formatted(TemplateListTest.js(lit.toString())));
    }

    @Test
    void misplacedDirectivesAreRejected() {
        String head = """
            import java.util.HashMap;
            import java.util.Map;
            import one.dastec.javaw.components.CustomElement;
            import one.dastec.javaw.components.Ref;
            import one.dastec.javaw.components.Template;
            import one.dastec.javaw.components.WebComponent;
            import one.dastec.javaw.dom.Element;
            @CustomElement("x-a")
            class A extends WebComponent {
                private Map<String, Boolean> m = new HashMap<String, Boolean>();
                private Map<String, String> st = new HashMap<String, String>();
                private Ref<Element> r = new Ref<Element>();
                private String s = "";
            """; // render() is on line 14
        java.util.function.Function<String, String> render = t -> head + "    protected Template render() { return html(\"" + t + "\"); }\n}\n";
        reject(render.apply("<p title=${classMap(m)}></p>"), "App.java:14:57:", "only binding of a class attribute");
        reject(render.apply("<p class='a ${classMap(m)} ${s}'></p>"), "App.java:14:60:", "only binding of a class attribute");
        reject(render.apply("<p>${styleMap(st)}</p>"), "App.java:14:51:", "only binding of a style attribute");
        reject(render.apply("<p class=${styleMap(st)}></p>"), "App.java:14:57:", "only binding of a style attribute");
        reject(render.apply("<p>${ref(r)}</p>"), "App.java:14:51:", "element position");
        reject(render.apply("<p title=${ref(r)}></p>"), "App.java:14:57:", "element position");
        reject(render.apply("<p ${1 + 1}></p>"), "App.java:14:51:", "Only ref(...)");
        // Other expressions in element position are checked by type.
        var error = assertThrows(IllegalArgumentException.class, () -> JavacToWasm.compile("App.java", render.apply("<p ${s}></p>"), false));
        assertTrue(error.getMessage().contains("App.java:14:53: No applicable overload"), error.getMessage());
        // A Ref needs an Element type.
        assertThrows(IllegalArgumentException.class, () -> JavacToWasm.compile("App.java",
                head.replace("private Ref<Element> r", "private Ref<String> r") + "}\n", false));
    }

    @Test
    void misplacedMapsThroughVariablesAreReportedAtRuntime() throws Exception {
        byte[] wasm = JavacToWasm.compile("App.java", """
            import java.util.HashMap;
            import java.util.Map;
            import one.dastec.javaw.components.ClassMap;
            import one.dastec.javaw.components.CustomElement;
            import one.dastec.javaw.components.Template;
            import one.dastec.javaw.components.WebComponent;
            @CustomElement("x-bad")
            class Bad extends WebComponent {
                @Override protected Template render() {
                    Map<String, Boolean> m = new HashMap<String, Boolean>();
                    m.put("a", Boolean.TRUE);
                    ClassMap c = classMap(m);
                    return html("<p title=${c}></p>");
                }
            }
            """, false).getWasm();
        CustomElementTest.run(wasm, "", """
            document.body.appendChild(document.createElement('x-bad'));
            await settle(); await settle();
            assert.equal(errors.length, 1);
            assert.match(String(errors[0] && (errors[0].stack || errors[0])), /classMap\\(\\)` can only be used in the `class` attribute/);
            """);
    }

    private static void reject(String source, String position, String fragment) {
        var error = assertThrows(IllegalArgumentException.class, () -> JavacToWasm.compile("App.java", source, false), source);
        assertTrue(error.getMessage().startsWith(position) && error.getMessage().contains(fragment),
                "expected " + position + " ... " + fragment + " but got: " + error.getMessage() + "\n" + source);
    }
}
