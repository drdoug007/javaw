package one.dastec.javaw.components;

import one.dastec.javaw.JavacToWasm;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** M1: @CustomElement classes run as native custom elements in happy-dom (test-vendor/node). */
class CustomElementTest {
    static final String COUNTER = """
        import one.dastec.javaw.components.CustomElement;
        import one.dastec.javaw.components.Query;
        import one.dastec.javaw.components.WebComponent;
        import one.dastec.javaw.dom.DOM;
        import one.dastec.javaw.dom.Element;

        @CustomElement(value = "javaw-counter", observedAttributes = {"start"})
        class Counter extends WebComponent {
            private int count;
            private String log = "";
            private boolean rendered;
            @Query("button") private Element button;
            public Counter() { log = "ctor"; }
            @Override protected void connected() {
                log = log + ",connected";
                if (!rendered) {
                    rendered = true;
                    Element b = createElement("button");
                    b.addEventListener("click", () -> { count++; show(); });
                    renderRoot().appendChild(b);
                }
                show();
            }
            @Override protected void disconnected() { log = log + ",disconnected"; host().setAttribute("data-log", log); }
            @Override protected void attributeChanged(String name, String oldValue, String newValue) {
                log = log + ",attr:" + name + "=" + newValue;
                if (newValue != null) count = Integer.parseInt(newValue);
                if (rendered) show();
            }
            private void show() {
                button.setInnerText("Count: " + count);
                host().setAttribute("data-log", log);
            }
            public int count() { return count; }
        }

        @CustomElement("javaw-broken")
        class Broken extends WebComponent {
            @Override protected void connected() { throw new IllegalStateException("broken"); }
        }

        class App {
            public static Counter made;
            public static void makeOne() { made = new Counter(); DOM.getBody().appendChild(made.host()); }
            public static int madeCount() { return made.count(); }
        }
        """;

    @Test
    void componentsRunAsCustomElements() throws Exception {
        byte[] wasm = JavacToWasm.compile("App.java", COUNTER, false).getWasm();
        run(wasm, """
            document.body.innerHTML = '<javaw-counter id="early" start="5"></javaw-counter>';
            """, """
            const text = el => el.shadowRoot.querySelector('button').textContent;
            const early = document.getElementById('early');
            assert.ok(early.shadowRoot, 'an element in the document before define is upgraded');
            assert.equal(early.getAttribute('data-log'), 'ctor,connected', 'happy-dom skips initial attribute reactions on upgrade');

            document.body.insertAdjacentHTML('beforeend', '<javaw-counter id="a" start="3"></javaw-counter><javaw-counter id="b"></javaw-counter>');
            const a = document.getElementById('a'), b = document.getElementById('b');
            assert.equal(a.getAttribute('data-log'), 'ctor,attr:start=3,connected', 'lifecycle order for a parsed element');
            assert.equal(text(a), 'Count: 3');
            assert.equal(text(b), 'Count: 0');
            a.shadowRoot.querySelector('button').click();
            assert.equal(text(a), 'Count: 4', 'the click listener runs on its own component');
            assert.equal(text(b), 'Count: 0');

            a.setAttribute('start', '10');
            assert.equal(text(a), 'Count: 10', 'observed attribute changes reach attributeChanged');
            a.setAttribute('ignored', 'x');
            a.removeAttribute('start');
            assert.equal(text(a), 'Count: 10');
            assert.ok(a.getAttribute('data-log').endsWith(',attr:start=10,attr:start=null'), 'removal passes null: ' + a.getAttribute('data-log'));

            const root = a.shadowRoot, button = root.querySelector('button');
            a.remove();
            assert.ok(a.getAttribute('data-log').endsWith(',disconnected'));
            document.body.appendChild(a);
            assert.ok(a.getAttribute('data-log').endsWith(',disconnected,connected'));
            assert.equal(a.shadowRoot, root, 'reconnecting keeps the shadow root');
            assert.equal(root.querySelector('button'), button, 'and the rendered DOM');

            e.makeOne();
            const made = document.body.lastElementChild;
            assert.equal(made.tagName, 'JAVAW-COUNTER', 'new Counter() in Java creates the element');
            made.shadowRoot.querySelector('button').click();
            assert.equal(e.madeCount(), 1, 'and Java holds the same object the element dispatches to');

            document.body.insertAdjacentHTML('beforeend', '<javaw-broken></javaw-broken><javaw-counter id="after"></javaw-counter>');
            assert.equal(errors.length, 1, 'an exception in a lifecycle hook is reported');
            assert.equal(text(document.getElementById('after')), 'Count: 0', 'and other components keep working');
            assert.equal(r.getHandleStats().global, globalHandles(), 'component nodes never enter the global handle table');
            """);
    }

    @Test
    void removedComponentsAreReclaimed() throws Exception {
        byte[] wasm = JavacToWasm.compile("App.java", COUNTER, false).getWasm();
        run(wasm, "", """
            e.__gc_collect();
            const baseline = e.__gc_live_bytes();
            const globals = r.getHandleStats().global;
            const cycle = n => {
              for (let i = 0; i < n; i++) {
                const el = document.createElement('javaw-counter');
                el.setAttribute('start', String(i));
                document.body.appendChild(el);
                el.shadowRoot.querySelector('button').click();
                el.remove();
              }
            };
            return (async () => {
              cycle(300);
              const created = r.getHandleStats().scopes.created;
              for (let i = 0; i < 100 && r.getHandleStats().scopes.finalized < created; i++) { global.gc(); await settle(); await settle(); }
              assert.equal(r.getHandleStats().scopes.finalized, created, 'every removed component was finalized');
              assert.equal(r.getHandleStats().scoped, 0);
              assert.equal(r.getHandleStats().global, globals, 'the global handle table stays level');
              e.__gc_collect();
              assert.equal(e.__gc_live_bytes(), baseline, 'component objects and closures are released');
            })();
            """);
    }

    @Test
    void constructorExceptionsReachJavaCatchBlocks() throws Exception {
        byte[] wasm = JavacToWasm.compile("App.java", """
            import one.dastec.javaw.components.CustomElement;
            import one.dastec.javaw.components.WebComponent;
            @CustomElement("x-thrower")
            class Thrower extends WebComponent { public Thrower() { throw new IllegalStateException("no"); } }
            class App {
                public static int tryMake() {
                    try { new Thrower(); return 0; }
                    catch (IllegalStateException e) { return 1; }
                }
            }
            """, false).getWasm();
        run(wasm, "", """
            assert.equal(e.tryMake(), 1, 'a constructor exception is catchable in Java');
            // Browsers report a custom element constructor exception inside createElement and return an
            // HTMLUnknownElement instead of rethrowing (HTML "create an element"); happy-dom rethrows.
            const original = document.createElement.bind(document);
            document.createElement = tag => { try { return original(tag); } catch (error) { reportError(error); return original('span'); } };
            const reported = errors.length;
            assert.equal(e.tryMake(), 1, 'the original exception still reaches Java with browser createElement behaviour');
            assert.equal(errors.length, reported + 1);
            """);
    }

    static final String EXTRA = """
        import one.dastec.javaw.components.CustomElement;
        import one.dastec.javaw.components.Query;
        import one.dastec.javaw.components.WebComponent;
        import one.dastec.javaw.dom.DOM;
        import one.dastec.javaw.dom.Element;

        @CustomElement("x-portal")
        class Portal extends WebComponent {
            @Query("input[name=\\"q\\"]") private Element input;
            @Override protected void connected() {
                Element tip = createElement("div");
                tip.setAttribute("class", "tip");
                tip.addEventListener("click", () -> host().setAttribute("data-tip", "1"));
                DOM.getBody().appendChild(tip);
                Element field = createElement("input");
                field.setAttribute("name", "q");
                renderRoot().appendChild(field);
                renderRoot().appendChild(createElement("input"));
                Element[] all = renderRoot().querySelectorAll("input");
                host().setAttribute("data-inputs", "" + all.length);
                input.setAttribute("data-found", "1");
                Element self = host().querySelector("span");
                host().setAttribute("data-light", self == null ? "none" : "span");
            }
        }

        @CustomElement("x-plain")
        class Plain extends WebComponent { }

        class App {
            public static Plain kept;
            public static void make() { kept = new Plain(); new Plain(); }
            public static int attach() {
                try { DOM.getBody().appendChild(kept.host()); return 1; }
                catch (IllegalStateException e) { return 0; }
            }
        }
        """;

    @Test
    void queriesPortalsAndJavaHeldComponents() throws Exception {
        byte[] wasm = JavacToWasm.compile("App.java", EXTRA, false).getWasm();
        run(wasm, "", """
            document.body.insertAdjacentHTML('beforeend', '<x-portal id="p"><span></span></x-portal>');
            const p = document.getElementById('p');
            assert.equal(p.getAttribute('data-inputs'), '2', 'renderRoot().querySelectorAll');
            assert.equal(p.shadowRoot.querySelector('input[name="q"]').getAttribute('data-found'), '1', '@Query with a quoted selector');
            assert.equal(p.getAttribute('data-light'), 'span', 'host().querySelector searches the light DOM');
            document.querySelector('.tip').click();
            assert.equal(p.getAttribute('data-tip'), '1', 'a scoped node outside the shadow tree keeps working while the component lives');

            e.make();   // `new Plain();` as a statement also compiles
            for (let i = 0; i < 10; i++) { global.gc(); await settle(); }
            assert.equal(e.attach(), 1, 'a component held only by Java survives until it is first connected');

            const created = r.getHandleStats().scopes.created;
            const makePortals = n => { for (let i = 0; i < n; i++) { const el = document.createElement('x-portal'); document.body.appendChild(el); el.remove(); } };
            makePortals(50);   // each leaves its tooltip node in the body
            const portals = r.getHandleStats().scopes.created - created;
            const before = r.getHandleStats().scopes.finalized;
            for (let i = 0; i < 100 && r.getHandleStats().scopes.finalized - before < portals; i++) { global.gc(); await settle(); await settle(); }
            assert.equal(r.getHandleStats().scopes.finalized - before, portals, 'nodes left in the body do not pin their removed components');
            assert.equal(document.querySelectorAll('.tip').length, 51, 'the tooltip nodes are still in the document');
            """);
    }

    @Test
    void staticNestedComponentsWorkLikeTopLevelOnes() throws Exception {
        byte[] wasm = JavacToWasm.compile("App.java", """
            import one.dastec.javaw.components.ComponentEvent;
            import one.dastec.javaw.components.CustomElement;
            import one.dastec.javaw.components.Property;
            import one.dastec.javaw.components.Query;
            import one.dastec.javaw.components.Template;
            import one.dastec.javaw.components.WebComponent;
            import one.dastec.javaw.dom.DOM;
            import one.dastec.javaw.dom.Element;

            class Widgets {
                static final String PREFIX = "n";
                record Picked(int count) implements ComponentEvent {}

                @CustomElement("x-nested-counter")
                static class Counter extends WebComponent {
                    @Property(reflect = true) private int count;
                    @Query("button") private Element button;
                    @Override protected Template render() {
                        return html("<button @click=${() -> { count++; dispatch(new Picked(count)); }}>${PREFIX}${count}</button>");
                    }
                    String label() { return button.getInnerText(); }
                }

                @CustomElement("x-nested-panel")
                public static class Panel extends WebComponent {
                    @Override protected Template render() {
                        return html("<x-nested-counter .count=${5} @picked=${this::onPicked}></x-nested-counter>");
                    }
                    private void onPicked(Picked p) { host().setAttribute("data-picked", "" + p.count()); }
                }
            }

            class App {
                static Widgets.Counter made;
                public static void make() {
                    made = new Widgets.Counter();
                    DOM.getBody().appendChild(made.host());
                }
                public static int madeIsCounter() { return made.host().getAttribute("count") != null ? 1 : 0; }
            }
            """, false).getWasm();
        run(wasm, "", """
            const tick = async () => { await settle(); await settle(); };
            const panel = document.createElement('x-nested-panel');
            document.body.appendChild(panel);
            await tick();
            const counter = panel.shadowRoot.querySelector('x-nested-counter');
            assert.equal(counter.constructor, window.customElements.get('x-nested-counter'), 'the nested class is registered');
            assert.equal(counter.count, 5, 'a typed .property binding reaches the nested component');
            assert.equal(counter.getAttribute('count'), '5', 'reflection works');
            counter.shadowRoot.querySelector('button').click();
            await tick();
            assert.equal(counter.shadowRoot.querySelector('button').textContent, 'n6', 'the nested class reads its enclosing class constant');
            assert.equal(panel.getAttribute('data-picked'), '6', 'a typed @event binding receives the nested event record');
            e.make();
            await tick();
            assert.equal(e.madeIsCounter(), 1, 'new Outer.Nested() creates the element through the registry');
            assert.equal(document.body.lastElementChild.localName, 'x-nested-counter');
            assert.equal(errors.length, 0, String(errors[0] && errors[0].stack));
            """);
    }

    @Test
    void oneFailingDefinitionDoesNotStopTheOthers() throws Exception {
        byte[] wasm = JavacToWasm.compile("App.java", """
            import one.dastec.javaw.components.CustomElement;
            import one.dastec.javaw.components.WebComponent;
            @CustomElement("x-thrower")
            class Thrower extends WebComponent { public Thrower() { throw new IllegalStateException("no"); } }
            @CustomElement("x-ok")
            class Ok extends WebComponent { @Override protected void connected() { host().setAttribute("data-ok", "1"); } }
            """, false).getWasm();
        run(wasm, "document.body.innerHTML = '<x-thrower></x-thrower><x-ok id=\"ok\"></x-ok>';", """
            assert.ok(window.customElements.get('x-ok'), 'later definitions still run');
            assert.equal(document.getElementById('ok').getAttribute('data-ok'), '1');
            assert.equal(errors.length, 1, 'the failing upgrade is reported');
            assert.equal(r.pendingComponentError, undefined);
            """);
    }

    /** Runs {@code assertions} after instantiation in a happy-dom window; {@code before} runs before. */
    static void run(byte[] wasm, String before, String assertions) throws Exception {
        assertNotNull(wasm);
        Path vendor = Path.of("../test-vendor/node/node_modules/happy-dom/lib/index.js").toAbsolutePath().normalize();
        Path runtime = Path.of("../javaw-core/src/main/resources/javaw-runtime.js").toAbsolutePath().normalize();
        String script = """
            const assert = require('node:assert/strict');
            const { pathToFileURL } = require('node:url');
            const settle = () => new Promise(resolve => setImmediate(resolve));
            (async () => {
              const { Window } = await import(pathToFileURL(process.argv[1]).href);
              const window = new Window();
              const document = window.document;
              const errors = [];
              globalThis.reportError = error => errors.push(error);
            """ + before + """
              const { instantiateJavaw } = require(process.argv[2]);
              const { instance: { exports: e }, runtime: r } =
                  await instantiateJavaw(new WebAssembly.Module(require('node:fs').readFileSync(0)), {}, document);
              const initialGlobals = r.getHandleStats().global;
              const globalHandles = () => initialGlobals + 1; // DOM.getBody() from makeOne
              await (async () => {
            """ + assertions + """
              })();
              await window.happyDOM.abort();
            })().catch(error => { console.error(error); process.exitCode = 1; });
            """;
        Process node = new ProcessBuilder("node", "--expose-gc", "-e", script, vendor.toString(), runtime.toString())
                .redirectErrorStream(true).start();
        try {
            try (var input = node.getOutputStream()) { input.write(wasm); }
            assertTrue(node.waitFor(90, TimeUnit.SECONDS), "Node execution timed out");
            assertEquals(0, node.exitValue(), new String(node.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        } finally {
            if (node.isAlive()) node.destroyForcibly();
        }
    }
}
