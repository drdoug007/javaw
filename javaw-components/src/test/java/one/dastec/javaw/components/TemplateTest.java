package one.dastec.javaw.components;

import one.dastec.javaw.JavacToWasm;
import org.junit.jupiter.api.Test;

/** M3: html(...) templates rendered by the Wasm-side renderer into the shadow root. */
class TemplateTest {
    static final String PROGRAM = """
        import one.dastec.javaw.components.Changes;
        import one.dastec.javaw.components.CustomElement;
        import one.dastec.javaw.components.Property;
        import one.dastec.javaw.components.State;
        import one.dastec.javaw.components.Template;
        import one.dastec.javaw.components.WebComponent;
        import one.dastec.javaw.dom.Element;

        @CustomElement("x-counter")
        class Counter extends WebComponent {
            @Property private String label = "Clicks";
            @Property private int count;
            @Property private boolean wide;
            @State private String note = "";
            @Property private boolean showDetail;
            @Property private String kind = "a";

            @Override protected Template render() {
                return html(\"""
                    <h2 class="title ${wide ? "wide" : "narrow"} x" data-count=${count}>${label}: ${count}</h2>
                    <button ?disabled=${count >= 3} @click=${() -> count++}>+</button>
                    <input .value=${note} @input=${e -> note = e.target().getValue()}>
                    <p title=${note.isEmpty() ? null : note}>$${count}.00 &amp; ${'!'}</p>
                    <div>${showDetail ? detail() : null}</div>
                    <span>${kind.equals("a") ? html("<b>A ${count}</b>") : html("<i>B</i>")}</span>
                    <table><tr><td>${count * 2}</td></tr></table>
                    \""");
            }

            private Template detail() {
                return html("<em data-n=${count}>detail ${label}</em>");
            }

        }
        """;

    @Test
    void templatesRenderAndUpdateInPlace() throws Exception {
        byte[] wasm = JavacToWasm.compile("App.java", PROGRAM, false).getWasm();
        CustomElementTest.run(wasm, "", """
            const tick = async () => { await settle(); await settle(); };
            const el = document.createElement('x-counter');
            document.body.appendChild(el);
            await tick();
            const root = el.shadowRoot;
            const html = () => root.innerHTML.replace(/<!--[^>]*-->/g, '').replace(/\\s+/g, ' ').trim();
            const h2 = root.querySelector('h2');
            assert.equal(h2.textContent, 'Clicks: 0');
            assert.equal(h2.getAttribute('class'), 'title narrow x', 'multi-part attribute');
            assert.equal(h2.getAttribute('data-count'), '0', 'unquoted attribute binding');
            assert.equal(root.querySelector('button').hasAttribute('disabled'), false);
            assert.equal(root.querySelector('p').hasAttribute('title'), false, 'null removes the attribute');
            assert.equal(root.querySelector('p').textContent, '$0.00 & !', 'lone $ is text; entities decode; char child');
            assert.equal(root.querySelector('div').textContent, '', 'null child renders nothing');
            assert.equal(root.querySelector('span').innerHTML.replace(/<!--[^>]*-->/g, ''), '<b>A 0</b>', 'nested one-line template');
            assert.equal(root.querySelector('td').textContent, '0', 'child part inside a table cell');

            const button = root.querySelector('button');
            const b = root.querySelector('b');
            const textNode = [...h2.childNodes].find(n => n.nodeType === 3 && n.data === '0');
            button.click(); await tick();
            assert.equal(h2.textContent, 'Clicks: 1', 'event listener writes a reactive field');
            assert.equal(el.count, 1);
            assert.equal(root.querySelector('button'), button, 'DOM is updated in place');
            assert.equal(root.querySelector('b'), b, 'same nested template is updated in place');
            assert.equal(b.textContent, 'A 1');
            assert.ok(textNode.isConnected && textNode.data === '1', 'text nodes are reused');

            el.count = 3; await tick();
            assert.equal(button.hasAttribute('disabled'), true, 'boolean attribute');
            assert.equal(root.querySelector('td').textContent, '6');

            const input = root.querySelector('input');
            input.value = 'hello';
            input.dispatchEvent(new window.Event('input'));
            await tick();
            assert.equal(root.querySelector('p').getAttribute('title'), 'hello', 'one-parameter listener reads the event');
            assert.equal(input.value, 'hello', 'property binding');

            // An update in which nothing changed makes no host calls.
            const names = ['wcChildText', 'wcChildTemplate', 'wcAttr', 'wcBool', 'wcProp', 'wcEvent', 'wcCreateInstance', 'wcRelease'];
            let calls = 0;
            for (const n of names) { const f = r[n]; r[n] = function (...a) { calls++; return f.apply(this, a); }; }
            el.count = 3;
            el.label = 'X'; el.label = 'Clicks';
            await tick();
            assert.equal(calls, 0, 'unchanged update makes no host calls');
            el.count = 4; await tick();
            assert.equal(calls, 5, 'only the changed bindings cross: h2 data-count and text, the p price, the nested <b> text, td');

            el.wide = true; await tick();
            assert.equal(h2.getAttribute('class'), 'title wide x');

            // Switching templates replaces the content and releases the old instance.
            const live = r.wcInstances.size;
            el.kind = 'b'; await tick();
            assert.equal(root.querySelector('span').innerHTML.replace(/<!--[^>]*-->/g, ''), '<i>B</i>');
            assert.equal(r.wcInstances.size, live, 'old nested instance released, new one created');
            el.showDetail = true; await tick();
            assert.equal(root.querySelector('em').textContent, 'detail Clicks');
            assert.equal(root.querySelector('em').getAttribute('data-n'), '4');
            el.showDetail = false; await tick();
            assert.equal(root.querySelector('em'), null);
            assert.equal(r.wcInstances.size, live, 'detail instance released');
            assert.equal(errors.length, 0, String(errors[0] && errors[0].stack));
            """);
    }

    @Test
    void removedTemplatedComponentsAreFinalized() throws Exception {
        byte[] wasm = JavacToWasm.compile("App.java", PROGRAM, false).getWasm();
        CustomElementTest.run(wasm, "", """
            const tick = async () => { await settle(); await settle(); };
            const created = r.getHandleStats().scopes.created;
            const instancesBefore = r.wcInstances ? r.wcInstances.size : 0;
            const idsBefore = r.wcNextInstance || 0;
            // Taken first: components can be finalized while the loop still runs.
            const before = r.getHandleStats().scopes.finalized;
            // In its own function, so no suspended frame of this one keeps the last element alive.
            const one = async () => {
                const el = document.createElement('x-counter');
                document.body.appendChild(el);
                await tick();
                el.shadowRoot.querySelector('button').click();
                el.showDetail = true;
                await tick();
                el.remove();
            };
            for (let i = 0; i < 200; i++) await one();
            const made = r.getHandleStats().scopes.created - created;
            assert.equal(made, 200);
            assert.ok(r.wcNextInstance - idsBefore >= 200 * 4, 'root, nested and detail instances were created: ' + (r.wcNextInstance - idsBefore));
            for (let i = 0; i < 100 && r.getHandleStats().scopes.finalized - before < made; i++) { global.gc(); await settle(); await settle(); }
            assert.equal(r.getHandleStats().scopes.finalized - before, made, 'removed components with templates and event parts are finalized');
            assert.equal(r.wcInstances.size, instancesBefore, 'their template instance ids are dropped with the scope');
            assert.equal(errors.length, 0, String(errors[0] && errors[0].stack));
            """);
    }

    @Test
    void eventBindingsAreTypedByEventName() throws Exception {
        byte[] wasm = JavacToWasm.compile("App.java", """
            import one.dastec.javaw.components.CustomElement;
            import one.dastec.javaw.components.Property;
            import one.dastec.javaw.components.Template;
            import one.dastec.javaw.components.WebComponent;
            import one.dastec.javaw.dom.Event;
            import one.dastec.javaw.dom.KeyboardEvent;
            import one.dastec.javaw.dom.PointerEvent;

            @CustomElement("x-typed")
            class Typed extends WebComponent {
                @Property private String log = "";
                @Override protected Template render() {
                    return html(\"""
                        <div id="d" tabindex="0"
                             @click=${e -> log = log + "click:" + e.clientX() + "," + e.button() + "," + e.shiftKey() + ";"}
                             @keydown=${e -> log = log + "key:" + e.key() + "," + e.code() + "," + e.ctrlKey() + ";"}
                             @pointerdown=${this::down}
                             @focus=${e -> log = log + "focus:" + (e.relatedTarget() == null) + ";"}
                             @custom=${this::other}>${log}</div>
                        \""");
                }
                private void down(PointerEvent e) { log = log + "pointer:" + e.pointerType() + "," + e.clientY() + ";"; }
                private void other(Event e) { log = log + "event:" + e.type() + ";"; }
            }
            """, false).getWasm();
        CustomElementTest.run(wasm, "", """
            const tick = async () => { await settle(); await settle(); };
            const el = document.createElement('x-typed');
            document.body.appendChild(el);
            await tick();
            const d = el.shadowRoot.getElementById('d');
            d.dispatchEvent(new window.MouseEvent('click', { clientX: 12.5, button: 2, shiftKey: true }));
            d.dispatchEvent(new window.KeyboardEvent('keydown', { key: 'Enter', code: 'Enter', ctrlKey: true }));
            d.dispatchEvent(new window.PointerEvent('pointerdown', { pointerType: 'pen', clientY: 3 }));
            d.dispatchEvent(new window.FocusEvent('focus'));
            d.dispatchEvent(new window.Event('custom'));
            await tick();
            assert.equal(el.log, 'click:12.5,2,true;key:Enter,Enter,true;pointer:pen,3.0;focus:true;event:custom;');
            assert.equal(d.textContent, el.log);
            assert.equal(errors.length, 0, String(errors[0] && errors[0].stack));
            """);
    }
}
