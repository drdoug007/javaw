package one.dastec.javaw.components;

import one.dastec.javaw.JavacToWasm;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

/** M4: Iterable children, keyed repeat (DOM identity across moves, duplicate keys), when and nothing. */
class TemplateListTest {
    static final String PROGRAM = """
        import java.util.ArrayList;
        import java.util.List;
        import one.dastec.javaw.components.CustomElement;
        import one.dastec.javaw.components.Property;
        import one.dastec.javaw.components.State;
        import one.dastec.javaw.components.Template;
        import one.dastec.javaw.components.WebComponent;

        final class Item {
            private final int id;
            private final String text;
            Item(int id, String text) { this.id = id; this.text = text; }
            int id() { return id; }
            String text() { return text; }
        }

        @CustomElement("x-list")
        class Lists extends WebComponent {
            @Property private String order = "";
            @Property private String mode = "list";
            @Property private boolean flag;
            @State private List<String> tags = new ArrayList<String>();
            @Property private boolean odd;
            @Override protected Template render() {
                return html(\"""
                    <ul>${mode.equals("list") ? repeat(items(), Item::id, this::row) : mode.equals("text") ? (Object) "plain" : null}</ul>
                    <ol>${tags}</ol>
                    <p title=${flag ? "on" : nothing()}>${when(flag, () -> html("<b>yes</b>"), () -> html("<i>no</i>"))}</p>
                    <div>${when(flag, () -> html("<em>only</em>"))}</div>
                    <section>${odd ? repeat(oddKeys(), k -> k, k -> html("<s>${label(k)}</s>")) : null}</section>
                    \""");
            }
            private Template row(Item item) { return html("<li data-id=${item.id()}>${item.text()}</li>"); }
            /** order is a comma-separated list of ids; the text of an item is t + id. */
            private List<Item> items() {
                List<Item> out = new ArrayList<Item>();
                for (String part : parts(order)) { int id = Integer.parseInt(part); out.add(new Item(id, "t" + id)); }
                return out;
            }
            private static String label(String k) { return k == null ? "null" : k; }
            private List<String> oddKeys() {
                List<String> keys = new ArrayList<String>();
                keys.add(null);
                keys.add("__wc$null-key");
                keys.add("b");
                return keys;
            }
            public void setTags(String csv) {
                tags = parts(csv);
            }
            /** Comma-separated parts (String.split and indexOf(String, int) are not supported by the source compiler yet). */
            private static List<String> parts(String csv) {
                List<String> out = new ArrayList<String>();
                String current = "";
                for (int i = 0; i < csv.length(); i++) {
                    char c = csv.charAt(i);
                    if (c == ',') { out.add(current); current = ""; } else current = current + c;
                }
                if (!csv.isEmpty()) out.add(current);
                return out;
            }
        }
        """;

    @Test
    void keyedListsMoveTheirDomAndMatchLitHtml() throws Exception {
        byte[] wasm = JavacToWasm.compile("App.java", PROGRAM, false).getWasm();
        Path lit = Path.of("../test-vendor/node/node_modules/lit-html").toAbsolutePath().normalize();
        CustomElementTest.run(wasm, "", """
            const tick = async () => { await settle(); await settle(); };
            globalThis.window = window; globalThis.document = document;
            const litBase = require('node:url').pathToFileURL(%s).href;
            const lit = await import(litBase + '/lit-html.js');
            const { repeat } = await import(litBase + '/directives/repeat.js');
            const clean = h => h.replace(/<!--[^]*?-->/g, '');
            const el = document.createElement('x-list');
            document.body.appendChild(el);
            const ul = () => el.shadowRoot.querySelector('ul');
            const ids = () => [...ul().querySelectorAll('li')].map(li => li.dataset.id).join(',');
            const container = document.createElement('div');
            document.body.appendChild(container);
            const litRender = order => lit.render(lit.html`<ul>${repeat(order ? order.split(',').map(Number) : [], id => id,
                id => lit.html`<li data-id=${id}>${'t' + id}</li>`)}</ul>`, container);
            const steps = ['', '1,2,3,4,5', '5,4,3,2,1', '5,1,4,2,3', '3', '3,9,8', '8,3', '1,2,3,4,5,6,7,8', '2,1,4,3,6,5,8,7',
                           '8,7,6,5,4,3,2,1', '1,8', '', '4,2'];
            let previous = new Map();
            for (const order of steps) {
                el.order = order;
                await tick();
                litRender(order);
                assert.equal(ids(), order, 'order ' + order);
                assert.equal(clean(ul().outerHTML), clean(container.querySelector('ul').outerHTML), 'same DOM as lit-html for ' + order);
                // Items whose key survives keep their <li> node.
                const now = new Map([...ul().querySelectorAll('li')].map(li => [li.dataset.id, li]));
                for (const [id, li] of now) if (previous.has(id)) assert.equal(li, previous.get(id), 'item ' + id + ' keeps its DOM at ' + order);
                previous = now;
            }

            assert.equal(errors.length, 0, String(errors[0] && errors[0].stack));

            // Switching the binding between a list, text and nothing replaces the content.
            el.order = '1,2'; await tick();
            el.mode = 'text'; await tick();
            assert.equal(clean(ul().innerHTML), 'plain');
            el.mode = 'none'; await tick();
            assert.equal(clean(ul().innerHTML), '');
            el.mode = 'list'; await tick();
            assert.equal(ids(), '1,2');

            // when and nothing.
            const p = el.shadowRoot.querySelector('p');
            assert.equal(clean(p.innerHTML), '<i>no</i>');
            assert.equal(p.hasAttribute('title'), false, 'nothing() removes the attribute');
            assert.equal(clean(el.shadowRoot.querySelector('div').innerHTML), '', 'when without a false case renders nothing');
            el.flag = true; await tick();
            assert.equal(clean(p.innerHTML), '<b>yes</b>');
            assert.equal(p.getAttribute('title'), 'on');
            assert.equal(clean(el.shadowRoot.querySelector('div').innerHTML), '<em>only</em>');

            // Duplicate keys are reported; the later item gets its own DOM.
            el.order = '1,2,1'; await tick();
            assert.equal(ids(), '1,2,1');
            assert.equal(errors.length, 1, 'the duplicate key is reported');
            assert.match(String(errors[0]), /duplicate key 1/);
            errors.length = 0;
            el.order = '2,1'; await tick();
            assert.equal(ids(), '2,1');
            assert.equal(errors.length, 0, String(errors[0] && errors[0].stack));

            // A null key is its own key, distinct from any string.
            el.odd = true; await tick();
            const section = () => clean(el.shadowRoot.querySelector('section').innerHTML);
            assert.equal(section(), '<s>null</s><s>__wc$null-key</s><s>b</s>');
            const kept = el.shadowRoot.querySelector('section s:nth-child(2)') || el.shadowRoot.querySelectorAll('section s')[1];
            el.flag = !el.flag; await tick();
            assert.equal(el.shadowRoot.querySelectorAll('section s')[1], kept, 'no duplicate: the item keeps its DOM');
            assert.equal(errors.length, 0, String(errors[0] && errors[0].stack));
            el.odd = false; el.flag = true; await tick();

            // Removed items release their instances.
            const live = r.wcInstances.size;
            el.order = '1,2,3,4,5,6'; await tick();
            el.order = ''; await tick();
            assert.equal(r.wcInstances.size, live - 4, 'the two items and their templates are released when removed');
            """.formatted(js(lit.toString())));
    }

    @Test
    void iterableChildrenAreMatchedByIndex() throws Exception {
        byte[] wasm = JavacToWasm.compile("App.java", """
            import java.util.ArrayList;
            import java.util.List;
            import one.dastec.javaw.components.CustomElement;
            import one.dastec.javaw.components.Property;
            import one.dastec.javaw.components.Template;
            import one.dastec.javaw.components.WebComponent;

            @CustomElement("x-tags")
            class Tags extends WebComponent {
                @Property private int n;
                @Override protected Template render() {
                    List<Object> items = new ArrayList<Object>();
                    for (int i = 0; i < n; i++) items.add(i % 2 == 0 ? (Object) html("<b>${i}</b>") : (Object) ("t" + i));
                    List<List<Integer>> grid = new ArrayList<List<Integer>>();
                    for (int r = 0; r < 2; r++) { List<Integer> row = new ArrayList<Integer>(); for (int c = 0; c < n; c++) row.add(r * 10 + c); grid.add(row); }
                    return html("<p>${items}</p><div>${grid}</div>");
                }
            }
            """, false).getWasm();
        CustomElementTest.run(wasm, "", """
            const tick = async () => { await settle(); await settle(); };
            const clean = h => h.replace(/<!--[^]*?-->/g, '');
            const el = document.createElement('x-tags');
            document.body.appendChild(el);
            el.n = 3; await tick();
            const p = el.shadowRoot.querySelector('p');
            assert.equal(clean(p.innerHTML), '<b>0</b>t1<b>2</b>');
            const first = p.querySelector('b');
            el.n = 5; await tick();
            assert.equal(clean(p.innerHTML), '<b>0</b>t1<b>2</b>t3<b>4</b>');
            assert.equal(p.querySelector('b'), first, 'items are reused by index');
            assert.equal(clean(el.shadowRoot.querySelector('div').innerHTML), '01234' + '1011121314', 'nested lists');
            el.n = 1; await tick();
            assert.equal(clean(p.innerHTML), '<b>0</b>');
            assert.equal(p.querySelector('b'), first);
            assert.equal(errors.length, 0, String(errors[0] && errors[0].stack));
            """);
    }
    @Test
    void rerenderingReplacedDomKeepsTheScopeLevel() throws Exception {
        // Scope growth (M1): DOM the renderer creates and removes is never handed to Java, so replacing every
        // row with new keys, listeners and refs must not grow the component's handle scope.
        byte[] wasm = JavacToWasm.compile("App.java", """
            import java.util.ArrayList;
            import java.util.List;
            import one.dastec.javaw.components.CustomElement;
            import one.dastec.javaw.components.Property;
            import one.dastec.javaw.components.Ref;
            import one.dastec.javaw.components.Template;
            import one.dastec.javaw.components.WebComponent;
            import one.dastec.javaw.dom.Element;

            @CustomElement("x-churn")
            class Churn extends WebComponent {
                @Property private int round;
                @Property private int clicks;
                @Property private boolean refs;
                private final Ref<Element> last = new Ref<Element>();
                @Override protected Template render() {
                    List<Integer> keys = new ArrayList<Integer>();
                    for (int i = 0; i < 5; i++) keys.add(round * 5 + i);
                    return html(\"""
                        <ul>${repeat(keys, k -> k, k -> html("<li ${refs ? ref(last) : null} @click=${() -> clicks++}>${k}</li>"))}</ul>
                        <p>${round % 2 == 0 ? html("<b>${round}</b>") : html("<i>${round}</i>")}</p>
                        \""");
                }
            }
            """, false).getWasm();
        CustomElementTest.run(wasm, "", """
            const tick = async () => { await settle(); await settle(); };
            const el = document.createElement('x-churn');
            document.body.appendChild(el);
            el.round = 1; await tick();
            const scope = el.__javawScope;
            assert.ok(scope && !scope.released, 'the component has a live scope');
            const level = () => [scope.held.handles.size, scope.nodes.size, scope.held.closures.size];
            const before = level();
            // The runtime collects after flushes (M2 collector safepoint); without it the heap is exhausted
            // after about 150 rounds.
            const churn = async (from, to) => {
                for (let round = from; round <= to; round++) {
                    el.round = round; await tick();
                    el.shadowRoot.querySelector('li').click();
                }
            };
            await churn(2, 201);
            assert.equal(el.shadowRoot.querySelectorAll('li').length, 5);
            assert.equal(el.shadowRoot.querySelector('li').textContent, '1005');
            assert.equal(el.clicks, 200, 'listeners of the current rows run');
            assert.deepEqual(level(), before, 'replaced rows add no scoped handles, nodes or retained closures');
            assert.ok(r.componentCollections > 0, 'the runtime collected after flushes');
            // Heap high-water stays bounded: 800 more rounds do not raise it once warmed up.
            await churn(202, 401);
            const highWater = r.getHeapStats().heapBytes;
            await churn(402, 1001);
            assert.equal(r.getHeapStats().heapBytes, highWater, 'heap high-water bytes stay level over 600 rounds');
            // A ref hands its element to Java, and the scope keeps it until the component is released.
            el.refs = true; await tick();
            const withRefs = scope.held.handles.size;
            await churn(1002, 1011);
            assert.equal(scope.held.handles.size - withRefs, 50, 'each ref-bound row keeps one scoped handle');
            r.componentCollection = 'always';
            let count = r.componentCollections;
            await churn(1012, 1014);
            assert.ok(r.componentCollections - count >= 3, "'always' collects after every flush (rounds and clicks each flush)");
            r.componentCollection = 'off';
            count = r.componentCollections;
            await churn(1015, 1030);
            assert.equal(r.componentCollections, count, "'off' never collects");
            assert.equal(errors.length, 0, String(errors[0] && errors[0].stack));
            """);
    }

    static String js(String s) {
        return '"' + s.replace("\\\\", "\\\\\\\\").replace("\"", "\\\\\"") + '"';
    }
}
