package one.dastec.javaw;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Instance-bound DOM listeners: Java closures registered with Element.addEventListener keep their receivers. */
class DomCallbackTest {
    private static final String PROGRAM = """
        import one.dastec.javaw.dom.DOM;
        import one.dastec.javaw.dom.Element;
        import one.dastec.javaw.dom.Event;
        import one.dastec.javaw.dom.EventListener;

        class Counter {
            private int count;
            private String name;
            private String lastType;
            private String lastTarget;
            private Element button;
            private EventListener<Event> listener;
            public Counter(String name) { this.name = name; }
            public void mount(Element root) {
                button = DOM.createElement("button");
                button.setAttribute("id", name);
                listener = e -> {
                    count++;
                    lastType = e.type();
                    lastTarget = e.target().getAttribute("id");
                    e.preventDefault();
                };
                button.addEventListener("click", listener);
                button.addEventListener("click", listener);
                button.addEventListener("reset", this::reset);
                root.appendChild(button);
            }
            public void unmount() { button.removeEventListener("click", listener); }
            private void reset() { count = 0; }
            public int count() { return count; }
            public String lastType() { return lastType; }
            public String lastTarget() { return lastTarget; }
        }

        class App {
            public static Counter a;
            public static Counter b;
            public static int anonymous;
            public static void start() {
                Element body = DOM.getBody();
                a = new Counter("a");
                b = new Counter("b");
                a.mount(body);
                b.mount(body);
            }
            public static void mountUnreferenced() {
                Element button = DOM.createElement("button");
                button.setAttribute("id", "c");
                int step = 5;
                button.addEventListener("click", () -> anonymous += step);
                DOM.getBody().appendChild(button);
            }
            public static void mountFailing() {
                Element button = DOM.createElement("button");
                button.setAttribute("id", "fail");
                button.addEventListener("click", (Event e) -> { throw new IllegalStateException("boom"); });
                button.addEventListener("click", () -> anonymous += 100);
                DOM.getBody().appendChild(button);
            }
            public static int countA() { return a.count(); }
            public static int countB() { return b.count(); }
            public static boolean typeAIs(int which) { return (which == 0 ? "click" : "reset").equals(a.lastType()); }
            public static boolean targetBIsB() { return "b".equals(b.lastTarget()); }
            public static void unmountA() { a.unmount(); }
            public static int anonymous() { return anonymous; }
        }
        """;

    private static final String FAKE_DOM = """
        class FakeElement extends EventTarget {
          constructor(tag) { super(); this.tagName = tag.toUpperCase(); this.nodeType = 1; this.attributes = {}; this.children = []; this.parentNode = null; this.textContent = ''; }
          setAttribute(k, v) { this.attributes[k] = String(v); if (k === 'id') this.id = String(v); }
          getAttribute(k) { return k in this.attributes ? this.attributes[k] : null; }
          appendChild(c) { this.children.push(c); c.parentNode = this; return c; }
          removeChild(c) { this.children = this.children.filter(x => x !== c); c.parentNode = null; return c; }
        }
        const body = new FakeElement('body');
        const find = (el, id) => el.id === id ? el : el.children.map(c => find(c, id)).find(Boolean) || null;
        globalThis.document = { body, documentElement: body, createElement: t => new FakeElement(t), getElementById: id => find(body, id) };
        const errors = [];
        globalThis.reportError = error => errors.push(error);
        const click = id => { const ev = new Event('click', { cancelable: true }); document.getElementById(id).dispatchEvent(ev); return ev; };
        """;

    @Test
    void listenersKeepTheirReceiversAndAreRetainedUntilRemoved() throws Exception {
        byte[] wasm = JavacToWasm.compile("App.java", PROGRAM, false).getWasm();
        run(wasm, """
            e.start();
            const first = click('a'); click('a'); click('b');
            assert.equal(e.countA(), 2, 'the duplicate add of the same listener object is ignored');
            assert.equal(e.countB(), 1);
            assert.ok(first.defaultPrevented, 'Event.preventDefault reaches the JS event');
            document.getElementById('a').dispatchEvent(new Event('reset'));
            assert.equal(e.countA(), 0, 'a bound method reference runs on its own receiver');
            assert.equal(e.countB(), 1);

            e.mountUnreferenced();
            e.__gc_collect();
            click('c'); click('c');
            assert.equal(e.anonymous(), 10, 'a closure reachable only from JS survives collection');

            const handles = r.handleMap.size;
            for (let i = 0; i < 50; i++) click('b');
            assert.equal(e.countB(), 51);
            assert.equal(r.handleMap.size, handles, 'event handles are released after each dispatch');

            e.unmountA(); e.__gc_collect();
            click('a');
            assert.equal(e.countA(), 0, 'a removed listener is no longer called');

            e.mountFailing();
            click('fail');
            assert.equal(errors.length, 1, 'a Java exception in a listener is reported');
            assert.equal(e.anonymous(), 110, 'other listeners on the same event still run');
            """);
    }

    @Test
    void eventAccessorsDescribeTheDispatch() throws Exception {
        byte[] wasm = JavacToWasm.compile("App.java", PROGRAM, false).getWasm();
        run(wasm, """
            e.start();
            click('b');
            assert.equal(e.targetBIsB(), 1, 'Event.target returns the dispatching element');
            click('a');
            assert.equal(e.typeAIs(0), 1, 'Event.type returns the event type');
            """);
    }

    @Test
    void ownTypesNamedLikeTypedEventsAreKept() throws Exception {
        // The typed DOM events (MouseEvent and others) are added only when the program does not declare its own.
        byte[] wasm = JavacToWasm.compile("App.java", """
            import one.dastec.javaw.dom.DOM;
            import one.dastec.javaw.dom.Element;
            import one.dastec.javaw.dom.Event;
            import one.dastec.javaw.dom.EventListener;
            class MouseEvent { int x = 7; }
            class App {
                public static int seen;
                public static int start() {
                    Element button = DOM.createElement("button");
                    EventListener<Event> listener = e -> seen++;
                    button.addEventListener("click", listener);
                    return new MouseEvent().x;
                }
            }
            """, false).getWasm();
        run(wasm, "assert.equal(e.start(), 7);");
    }

    @Test
    void typedListenersReceiveTheirDeclaredEventClass() throws Exception {
        // The listener's declared event type, not the event name, selects the wrapper (wildcard dom import).
        byte[] wasm = JavacToWasm.compile("App.java", """
            import one.dastec.javaw.dom.*;
            class Pad {
                int pointerId;
                String pointerKind = "";
                void onPointer(PointerEvent e) { pointerId = e.pointerId(); pointerKind = e.pointerType(); }
            }
            class App {
                public static int x;
                public static String key = "";
                public static String typeSeen = "";
                public static String detail = "";
                public static int keys;
                public static Pad pad = new Pad();
                static Element button;
                static EventListener<KeyboardEvent> onKey;
                public static void start() {
                    button = DOM.createElement("button");
                    button.setAttribute("id", "b");
                    button.addEventListener("click", (MouseEvent e) -> x = (int) e.clientX() + (e.shiftKey() ? 1000 : 0));
                    onKey = e -> { keys++; key = e.key(); };
                    button.addEventListener("keydown", onKey);
                    Pad target = pad;
                    button.addEventListener("pointerdown", target::onPointer);
                    button.addEventListener("input", e -> typeSeen = e.type());
                    button.addEventListener("picked", (CustomEvent e) -> detail = e.detail().getString("name"));
                    DOM.getBody().appendChild(button);
                }
                public static void removeKey() { button.removeEventListener("keydown", onKey); }
                public static int x() { return x; }
                public static int keys() { return keys; }
                public static boolean keyIs(int which) { return (which == 0 ? "Enter" : "a").equals(key); }
                public static boolean inputSeen() { return "input".equals(typeSeen); }
                public static boolean detailIsAda() { return "Ada".equals(detail); }
                public static int pointerId() { return pad.pointerId; }
                public static boolean pen() { return "pen".equals(pad.pointerKind); }
            }
            """, false).getWasm();
        run(wasm, """
            const fire = (type, props) => { const ev = new Event(type); Object.assign(ev, props); document.getElementById('b').dispatchEvent(ev); };
            e.start();
            fire('click', { clientX: 41.7, shiftKey: true });
            assert.equal(e.x(), 1041, 'a MouseEvent listener reads mouse properties');
            fire('keydown', { key: 'Enter' });
            assert.equal(e.keyIs(0), 1, 'a stored EventListener<KeyboardEvent> reads the key');
            fire('pointerdown', { pointerId: 9, pointerType: 'pen' });
            assert.equal(e.pointerId(), 9, 'a bound method reference taking PointerEvent receives one');
            assert.equal(e.pen(), 1);
            fire('input', {});
            assert.equal(e.inputSeen(), 1, 'an implicit lambda receives Event');
            fire('picked', { detail: { name: 'Ada' } });
            assert.equal(e.detailIsAda(), 1, 'a CustomEvent listener on a custom name reads detail');
            const handles = r.handleMap.size;
            for (let i = 0; i < 20; i++) fire('keydown', { key: 'a' });
            assert.equal(r.handleMap.size, handles, 'typed event handles are released after each dispatch');
            assert.equal(e.keyIs(1), 1);
            e.removeKey();
            fire('keydown', { key: 'Enter' });
            assert.equal(e.keys(), 21, 'a typed listener is removed by the same object');
            assert.equal(errors.length, 0, String(errors[0]));
            """);
    }

    @Test
    void typedListenersAreCheckedAgainstLiteralEventNames() {
        var mismatch = assertThrows(RuntimeException.class, () -> JavacToWasm.compile("App.java", """
            import one.dastec.javaw.dom.*;
            class App {
                public static void start() { DOM.getBody().addEventListener("keydown", (MouseEvent e) -> {}); }
            }
            """, false));
        assertTrue(mismatch.getMessage().contains("'keydown' events are KeyboardEvents"), mismatch.getMessage());
        var stored = assertThrows(RuntimeException.class, () -> JavacToWasm.compile("App.java", """
            import one.dastec.javaw.dom.*;
            class App {
                public static void start() {
                    EventListener<FocusEvent> l = e -> {};
                    DOM.getBody().addEventListener("click", l);
                }
            }
            """, false));
        assertTrue(stored.getMessage().contains("'click' events are MouseEvents"), stored.getMessage());
        var notEvent = assertThrows(RuntimeException.class, () -> JavacToWasm.compile("App.java", """
            import one.dastec.javaw.dom.*;
            class App {
                public static void start() { DOM.getBody().addEventListener("click", (String e) -> {}); }
            }
            """, false));
        assertTrue(notEvent.getMessage().contains("must be Event or a typed DOM event"), notEvent.getMessage());
        var wildcard = assertThrows(RuntimeException.class, () -> JavacToWasm.compile("App.java", """
            import one.dastec.javaw.dom.*;
            class App {
                public static void start() {
                    EventListener<MouseEvent> h = e -> {};
                    EventListener<? super MouseEvent> l = h;
                    DOM.getBody().addEventListener("click", l);
                }
            }
            """, false));
        assertTrue(wildcard.getMessage().contains("has a wildcard"), wildcard.getMessage());
        var shadowed = assertThrows(RuntimeException.class, () -> JavacToWasm.compile("App.java", """
            import one.dastec.javaw.dom.*;
            class MouseEvent extends Event { MouseEvent(JSObject js) { super(js); } }
            class App {
                public static void start() { DOM.getBody().addEventListener("click", (MouseEvent e) -> {}); }
            }
            """, false));
        assertTrue(shadowed.getMessage().contains("shadows one.dastec.javaw.dom.MouseEvent"), shadowed.getMessage());
    }

    @Test
    void removedListenersAreReleasedForCollection() throws Exception {
        byte[] wasm = JavacToWasm.compile("App.java", """
            import one.dastec.javaw.dom.DOM;
            import one.dastec.javaw.dom.Element;
            import one.dastec.javaw.dom.Event;
            import one.dastec.javaw.dom.EventListener;
            class App {
                public static int total;
                public static void cycle() {
                    Element button = DOM.createElement("button");
                    int[] payload = new int[512];
                    EventListener<Event> listener = e -> total += payload.length;
                    button.addEventListener("click", listener);
                    button.removeEventListener("click", listener);
                }
            }
            """, false).getWasm();
        run(wasm, """
            e.__gc_collect(); const baseline = e.__gc_live_bytes();
            for (let i = 0; i < 20; i++) { e.cycle(); e.__gc_collect(); }
            assert.equal(e.__gc_live_bytes(), baseline, 'remove releases the retained closure');
            """);
    }

    @Test
    void ambiguousMethodReferenceIsDiagnosed() {
        var error = assertThrows(RuntimeException.class, () -> JavacToWasm.compile("App.java", """
            import one.dastec.javaw.dom.DOM;
            import one.dastec.javaw.dom.Element;
            import one.dastec.javaw.dom.Event;
            class App {
                static void handle() {}
                static void handle(Event e) {}
                public static void start() { DOM.getBody().addEventListener("click", App::handle); }
            }
            """, false));
        assertTrue(error.getMessage().contains("Ambiguous DOM listener"), error.getMessage());
    }

    @Test
    void runnableValuesAreDiagnosed() {
        var error = assertThrows(RuntimeException.class, () -> JavacToWasm.compile("App.java", """
            import one.dastec.javaw.dom.DOM;
            class Job implements Runnable { public void run() {} }
            class App {
                public static void start() { DOM.getBody().addEventListener("click", new Job()); }
            }
            """, false));
        assertTrue(error.getMessage().contains("Runnable values are not supported as DOM listeners"), error.getMessage());
    }

    @Test
    void userTypeNamedEventConflictsWithListenerSupport() {
        var error = assertThrows(RuntimeException.class, () -> JavacToWasm.compile("App.java", """
            import one.dastec.javaw.dom.DOM;
            class Event { }
            class App {
                public static void start() { DOM.getBody().addEventListener("click", () -> {}); }
            }
            """, false));
        assertTrue(error.getMessage().contains("conflicts with one.dastec.javaw.dom.Event"), error.getMessage());
    }

    @Test
    void programsWithTheirOwnEventTypesAndListenerMethodsAreUnaffected() throws Exception {
        // A nested user type named Event, with no DOM listeners: nothing is injected or replaced.
        OverloadResolutionTest.execute(JavacToWasm.compile("Main.java", """
            public class Main {
                static class Event { int id; Event(int id) { this.id = id; } }
                public static int test() { return new Event(3).id; }
            }
            """, false).getWasm(), "assert.equal(e.test(), 3);");
        // A user class named Event alongside the existing handler-name form.
        assertNotNull(JavacToWasm.compile("App.java", """
            import one.dastec.javaw.dom.DOM;
            class Event { }
            class App {
                public static void start() { String h = "onClick"; DOM.getBody().addEventListener("click", h); }
                public static void onClick() { }
            }
            """, false).getWasm());
        // Source classes with their own add/removeEventListener keep ordinary Java dispatch,
        // including receivers the lambda pass cannot type directly (array elements).
        OverloadResolutionTest.execute(JavacToWasm.compile("App.java", """
            interface Sink { public int accept(int value); }
            class Event { }
            class Bus {
                private Sink sink;
                public void addEventListener(String type, Sink listener) { sink = listener; }
                public void removeEventListener(String type, Sink listener) { if (sink == listener) sink = null; }
                public int fire(int value) { return sink == null ? -1 : sink.accept(value); }
                public void clear() { removeEventListener("x", sink); }
            }
            class App {
                public static int test() {
                    Bus bus = new Bus();
                    bus.addEventListener("x", v -> v * 2);
                    if (bus.fire(4) != 8) return -1;
                    Bus[] buses = { new Bus() };
                    buses[0].addEventListener("x", v -> v + 100);
                    if (buses[0].fire(1) != 101) return -2;
                    bus.clear();
                    if (bus.fire(4) != -1) return -3;
                    return 1;
                }
            }
            """, false).getWasm(), "assert.equal(e.test(), 1);");
    }

    private static void run(byte[] wasm, String assertions) throws Exception {
        assertNotNull(wasm);
        String script = "const assert = require('node:assert/strict');\n" + FAKE_DOM + """
            const {instantiateJavaw} = require(process.argv[1]);
            instantiateJavaw(new WebAssembly.Module(require('node:fs').readFileSync(0)))
              .then(({instance: {exports: e}, runtime: r}) => {
            """ + assertions + """
              }).catch(error => { console.error(error); process.exitCode = 1; });
            """;
        Process node = new ProcessBuilder("node", "-e", script,
                Path.of("src/main/resources/javaw-runtime.js").toAbsolutePath().toString()).redirectErrorStream(true).start();
        try {
            try (var input = node.getOutputStream()) { input.write(wasm); }
            assertTrue(node.waitFor(20, TimeUnit.SECONDS), "Node execution timed out");
            assertEquals(0, node.exitValue(), new String(node.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        } finally {
            if (node.isAlive()) node.destroyForcibly();
        }
    }
}
