package one.dastec.javaw.components;

import one.dastec.javaw.JavacToWasm;
import one.dastec.javaw.components.compiler.ComponentExtensionAccess;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** M5 events: ComponentEvent records and options, @Fires, listen, and typed @event bindings across Java components. */
class EventsTest {
    static final String PROGRAM = """
        import one.dastec.javaw.components.*;
        import one.dastec.javaw.dom.Element;
        import one.dastec.javaw.dom.Event;

        enum Level { LOW, VERY_HIGH }

        @CustomElement("x-todo")
        class Todo extends WebComponent {
            public record RemainingChanged(int remaining, String label, Level level) implements ComponentEvent {}
            @EventName("todo-cleared") @EventOptions(bubbles = false)
            public record Cleared(boolean all) implements ComponentEvent {}
            @EventOptions(cancelable = true)
            public record CloseRequestEvent(long id, char key, double ratio, Integer missing, Inner inner) implements ComponentEvent {}
            public record Inner(String text, Element where) implements ComponentEvent {
                public Inner {
                    if (text == null) text = "none";
                }
            }
            @Property private int remaining = 3;
            public static Object lastSent;
            public static int vetoed;
            public static int vetoedCount() { return vetoed; }

            @Override protected Template render() {
                return html(\"""
                    <button id="dec" @click=${this::dec}>-</button>
                    <button id="clear" @click=${() -> dispatch(new Cleared(true))}>clear</button>
                    <button id="close" @click=${this::close}>close</button>
                    \""");
            }
            private void dec() {
                remaining--;
                RemainingChanged e = new RemainingChanged(remaining, remaining == 0 ? null : "left", remaining > 1 ? Level.VERY_HIGH : Level.LOW);
                lastSent = e;
                dispatch(e);
            }
            private void close() {
                if (!dispatch(new CloseRequestEvent(9007199254740993L, 'k', 0.5, null, new Inner(null, host())))) vetoed++;
            }
        }

        @CustomElement("x-board")
        class Board extends WebComponent {
            @State private int total = -1;
            @State private String label = "";
            @State private int clears;
            @State private int closes;
            @State private boolean same;
            @State private String text = "";

            @Override protected Template render() {
                return html(\"""
                    <x-todo @remaining-changed=${e -> { total = e.remaining(); label = e.label() == null ? "null" : e.label(); same = e == Todo.lastSent; }}
                            @todo-cleared=${() -> clears++}
                            @close-request=${this::onClose}></x-todo>
                    <p>${total} ${label} ${same} ${clears} ${closes} ${text}</p>
                    \""");
            }
            private void onClose(Todo.CloseRequestEvent e) {
                closes++;
                text = e.inner().text() + ":" + e.key() + ":" + e.id() + ":" + e.ratio();
            }
        }

        @CustomElement("x-watch")
        @Fires(Todo.Cleared.class)
        class Watch extends WebComponent {
            @State private int resized;
            @State private int keys;
            public Watch() {
                listen(window(), "resize", () -> resized++);
                listen(document(), "keydown", (Event e) -> { if (e.type().equals("keydown")) keys++; });
            }
            @Override protected Template render() {
                return html("<p>${resized} ${keys}</p>");
            }
        }
        """;

    @Test
    void eventRecordsReachJsAndTypedJavaListeners() throws Exception {
        byte[] wasm = JavacToWasm.compile("App.java", PROGRAM, false).getWasm();
        CustomElementTest.run(wasm, "", """
            const tick = async () => { await settle(); await settle(); };
            const board = document.createElement('x-board');
            document.body.appendChild(board);
            await tick();
            const todo = board.shadowRoot.querySelector('x-todo');
            await tick();
            const seen = [];
            document.addEventListener('remaining-changed', ev => seen.push(ev));
            document.addEventListener('todo-cleared', ev => seen.push(ev));
            document.addEventListener('close-request', ev => { seen.push(ev); ev.preventDefault(); });
            const p = () => board.shadowRoot.querySelector('p').textContent;
            const button = id => todo.shadowRoot.getElementById(id);

            button('dec').click(); await tick();
            assert.equal(p(), '2 left true 0 0 ', 'typed listener receives the dispatched record instance');
            assert.equal(seen.length, 1, 'bubbles and composed by default');
            const ev = seen[0];
            assert.equal(ev.type, 'remaining-changed');
            assert.ok(ev instanceof window.CustomEvent);
            assert.deepEqual([ev.bubbles, ev.composed, ev.cancelable], [true, true, false]);
            assert.deepEqual({ ...ev.detail }, { remaining: 2, label: 'left', level: 'very-high' });
            // happy-dom does not retarget event.target at the document; a browser reports the board.
            assert.ok(ev.target === board || ev.target === todo, 'dispatched from the host');

            button('dec').click(); await tick();
            button('dec').click(); await tick();
            assert.equal(p(), '0 null true 0 0 ');
            assert.deepEqual({ ...seen[2].detail }, { remaining: 0, label: null, level: 'low' });

            button('clear').click(); await tick();
            assert.equal(p(), '0 null true 1 0 ', '@EventName and a zero-parameter listener');
            assert.equal(seen.length, 3, 'bubbles = false does not reach the document');

            button('close').click(); await tick();
            assert.equal(seen.length, 4);
            const close = seen[3];
            assert.equal(close.cancelable, true);
            assert.equal(close.detail.id, 9007199254740993n, 'long is a bigint');
            assert.equal(close.detail.key, 'k');
            assert.equal(close.detail.ratio, 0.5);
            assert.equal(close.detail.missing, null);
            assert.equal(close.detail.inner.text, 'none', 'compact constructor ran');
            assert.ok(close.detail.inner.where === todo, 'Element components are the element');
            assert.equal(p(), '0 null true 1 1 none:k:9007199254740993:0.5');
            assert.equal(e.vetoedCount(), 1, 'dispatch returns false for a prevented cancelable event');

            // A CustomEvent of the same name dispatched from JS carries no record: the typed Java listener ignores it.
            todo.dispatchEvent(new window.CustomEvent('remaining-changed', { detail: { remaining: 99 }, bubbles: true, composed: true }));
            await tick();
            assert.equal(p().split(' ')[0], '0');
            assert.equal(seen[seen.length - 1].detail.remaining, 99, 'the JS listener still sees it');
            assert.equal(errors.length, 0, String(errors[0] && errors[0].stack));
            """);
    }

    @Test
    void listenFollowsConnectionAndDoesNotPinRemovedComponents() throws Exception {
        byte[] wasm = JavacToWasm.compile("App.java", PROGRAM, false).getWasm();
        CustomElementTest.run(wasm, "", """
            const tick = async () => { await settle(); await settle(); };
            const watch = document.createElement('x-watch');
            const text = () => watch.shadowRoot.querySelector('p').textContent;
            window.dispatchEvent(new window.Event('resize'));
            document.body.appendChild(watch);
            await tick();
            assert.equal(text(), '0 0', 'listen in the constructor adds nothing before the first connection');
            window.dispatchEvent(new window.Event('resize'));
            document.dispatchEvent(new window.KeyboardEvent('keydown', { key: 'a' }));
            document.dispatchEvent(new window.KeyboardEvent('keydown', { key: 'b' }));
            await tick();
            assert.equal(text(), '1 2', 'window and document listeners while connected');
            watch.remove();
            window.dispatchEvent(new window.Event('resize'));
            document.dispatchEvent(new window.KeyboardEvent('keydown', { key: 'c' }));
            document.body.appendChild(watch);
            await tick();
            assert.equal(text(), '1 2', 'removed on disconnect');
            window.dispatchEvent(new window.Event('resize'));
            await tick();
            assert.equal(text(), '2 2', 'added again on reconnect, once');

            // Removed components that listen on window and document are finalized.
            watch.remove();
            const created = r.getHandleStats().scopes.created;
            const before = r.getHandleStats().scopes.finalized;
            const one = async () => {
                const el = document.createElement('x-watch');
                document.body.appendChild(el);
                await tick();
                window.dispatchEvent(new window.Event('resize'));
                el.remove();
            };
            for (let i = 0; i < 100; i++) await one();
            const made = r.getHandleStats().scopes.created - created;
            assert.equal(made, 100);
            for (let i = 0; i < 100 && r.getHandleStats().scopes.finalized - before < made; i++) { global.gc(); await settle(); await settle(); }
            assert.equal(r.getHandleStats().scopes.finalized - before, made, 'listen does not keep removed components alive');
            assert.equal(errors.length, 0, String(errors[0] && errors[0].stack));
            """);
    }

    // The same records on the JVM: the lowered event classes must give the same text and equality.
    enum Tone { LOW, HIGH }
    record Point(int x, double y, String name, Tone tone, boolean on, long big, char c, Integer boxed) implements ComponentEvent {}
    record Wrap(Point point, String label) implements ComponentEvent {
        Wrap {
            if (label == null) label = "none";
        }
        String shout() { return label.toUpperCase(); }
    }

    @Test
    void loweredEventRecordsMatchJvmRecords() throws Exception {
        Point p = new Point(3, -0.5, "a", Tone.HIGH, true, 1L << 40, 'z', null);
        Wrap w = new Wrap(p, null);
        String expected = p + "|" + w + "|" + w.shout() + "|" + p.equals(new Point(3, -0.5, "a", Tone.HIGH, true, 1L << 40, 'z', null))
                + "|" + p.equals(new Point(3, 0.5, "a", Tone.HIGH, true, 1L << 40, 'z', null))
                + "|" + new Point(0, 0.0 / 0.0, null, null, false, 0L, 'a', Integer.valueOf(1)).equals(new Point(0, 0.0 / 0.0, null, null, false, 0L, 'a', Integer.valueOf(1)))
                + "|" + new Point(0, 0.0, null, null, false, 0L, 'a', Integer.valueOf(1)).equals(new Point(0, -0.0, null, null, false, 0L, 'a', Integer.valueOf(1)))
                + "|" + w.equals(new Wrap(p, "none")) + "|" + w.equals("x")
                + "|" + (p.hashCode() == new Point(3, -0.5, "a", Tone.HIGH, true, 1L << 40, 'z', null).hashCode());
        byte[] wasm = JavacToWasm.compile("App.java", """
            import one.dastec.javaw.components.*;

            enum Tone { LOW, HIGH }
            record Point(int x, double y, String name, Tone tone, boolean on, long big, char c, Integer boxed) implements ComponentEvent {}
            record Wrap(Point point, String label) implements ComponentEvent {
                Wrap {
                    if (label == null) label = "none";
                }
                String shout() { return label.toUpperCase(); }
            }

            @CustomElement("x-holder")
            class Holder extends WebComponent {
                public static int test() {
                    Point p = new Point(3, -0.5, "a", Tone.HIGH, true, 1L << 40, 'z', null);
                    Wrap w = new Wrap(p, null);
                    String text = p + "|" + w + "|" + w.shout() + "|" + p.equals(new Point(3, -0.5, "a", Tone.HIGH, true, 1L << 40, 'z', null))
                            + "|" + p.equals(new Point(3, 0.5, "a", Tone.HIGH, true, 1L << 40, 'z', null))
                            + "|" + new Point(0, 0.0 / 0.0, null, null, false, 0L, 'a', Integer.valueOf(1)).equals(new Point(0, 0.0 / 0.0, null, null, false, 0L, 'a', Integer.valueOf(1)))
                            + "|" + new Point(0, 0.0, null, null, false, 0L, 'a', Integer.valueOf(1)).equals(new Point(0, -0.0, null, null, false, 0L, 'a', Integer.valueOf(1)))
                            + "|" + w.equals(new Wrap(p, "none")) + "|" + w.equals("x")
                            + "|" + (p.hashCode() == new Point(3, -0.5, "a", Tone.HIGH, true, 1L << 40, 'z', null).hashCode());
                    return text.equals(%s) ? 1 : -text.length();
                }
            }
            """.formatted(ComponentEventsJvm.quote(expected)), false).getWasm();
        CustomElementTest.run(wasm, "", "assert.equal(e.test(), 1, " + ComponentEventsJvm.quote(expected) + ");");
    }

    @Test
    void eventMisuseIsDiagnosed() {
        String c = "@CustomElement(\"x-a\") class A extends WebComponent { ";
        reject("class E implements ComponentEvent {}", "ComponentEvent must be implemented by a record: E");
        reject("record R<T>(T t) implements ComponentEvent {}", "must not be generic");
        reject("record R(java.util.List<String> items) implements ComponentEvent {}", "Component 'items' of event record R has type",
                "does not support yet");
        reject("@EventName(\"a b\") record R(int n) implements ComponentEvent {}", "App.java:", "non-empty and without whitespace");
        reject("@EventName(\"x\") record R(int n) {}", "belong on a record that implements ComponentEvent");
        reject("@EventOptions(bubbles = 1) record R(int n) implements ComponentEvent {}", "must be a boolean literal");
        reject("@Fires(String.class) " + c + "}", "@Fires lists records of this module that implement ComponentEvent");
        reject("record One(int n) implements ComponentEvent {}\n@EventName(\"one\") record Two(int n) implements ComponentEvent {}\n"
                + "@Fires({One.class, Two.class}) " + c + "}", "A declares two events named 'one': One and Two");
    }

    @Test
    void eventNamesAreKebabCaseWithoutEventSuffix() {
        assertEquals("remaining-changed", ComponentExtensionAccess.eventName("RemainingChanged"));
        assertEquals("item-selected", ComponentExtensionAccess.eventName("ItemSelectedEvent"));
        assertEquals("url-changed", ComponentExtensionAccess.eventName("URLChanged"));
        assertEquals("step2-done", ComponentExtensionAccess.eventName("Step2Done"));
        assertEquals("event", ComponentExtensionAccess.eventName("Event"));
        assertEquals("close", ComponentExtensionAccess.eventName("Close"));
    }

    private static void reject(String body, String... fragments) {
        var error = assertThrows(IllegalArgumentException.class, () -> JavacToWasm.compile("App.java",
                "import one.dastec.javaw.components.*;\n" + body, false));
        for (String fragment : fragments) assertTrue(error.getMessage().contains(fragment), error.getMessage());
    }

    /** Java string literal helpers for the JVM comparison. */
    static final class ComponentEventsJvm {
        static String quote(String s) {
            StringBuilder out = new StringBuilder("\"");
            for (char c : s.toCharArray()) out.append(c == '"' || c == '\\' ? "\\" + c : String.valueOf(c));
            return out.append('"').toString();
        }
    }
}
