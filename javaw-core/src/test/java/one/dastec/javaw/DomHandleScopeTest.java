package one.dastec.javaw;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Scoped element handles, scope release and finalization, and retained exported-class wrappers. */
class DomHandleScopeTest {
    private static final String FAKE_DOM = """
        class FakeNode extends EventTarget {
          constructor() { super(); this.children = []; this.parentNode = null; this.attributes = {}; }
          appendChild(c) { this.children.push(c); c.parentNode = this; return c; }
          removeChild(c) { this.children = this.children.filter(x => x !== c); c.parentNode = null; return c; }
          getRootNode() { let n = this; while (n.parentNode) n = n.parentNode; return n; }
          setAttribute(k, v) { this.attributes[k] = String(v); }
          getAttribute(k) { return k in this.attributes ? this.attributes[k] : null; }
        }
        class FakeElement extends FakeNode { constructor(tag) { super(); this.tagName = tag.toUpperCase(); this.nodeType = 1; } }
        class FakeShadowRoot extends FakeNode { constructor(host) { super(); this.host = host; this.nodeType = 11; host.shadowRoot = this; } }
        const body = new FakeElement('body');
        globalThis.document = { body, documentElement: body, createElement: t => new FakeElement(t), getElementById: () => null };
        const settle = () => new Promise(resolve => setImmediate(resolve));
        """;

    @Test
    void scopedHandlesResolveReleaseAndAreReclaimedWithTheirHost() throws Exception {
        byte[] wasm = JavacToWasm.compile("App.java", """
            import one.dastec.javaw.dom.Element;
            import one.dastec.javaw.dom.Event;
            class Peer {
                public int[] payload;
                public int clicks;
                public Peer() { payload = new int[256]; }
            }
            class App {
                public static int clicks;
                public static Element stash;
                public static Peer newPeer() { return new Peer(); }
                public static void wire(Element host, Element child, Peer peer) {
                    child.setAttribute("data-wired", "1");
                    host.addEventListener("click", () -> peer.clicks++);
                    child.addEventListener("click", (Event e) -> { clicks++; e.target().setAttribute("seen", "1"); });
                }
                public static void keep(Element child) { stash = child; }
                public static int useStash() {
                    try { stash.setAttribute("late", "1"); return 0; }
                    catch (IllegalStateException e) { return 1; }
                }
                public static int clickCount() { return clicks; }
                public static int peerClicks(Peer peer) { return peer.clicks; }
            }
            """, false).getWasm();
        run(wasm, """
            e.__gc_collect();
            const baseline = e.__gc_live_bytes();
            const globalBase = r.getHandleStats().global;
            const make = () => {
              const host = new FakeElement('x-host');
              const root = new FakeShadowRoot(host);
              const child = new FakeElement('span');
              root.appendChild(child);
              body.appendChild(host);
              const peer = e.newPeer();
              const scope = r.createHandleScope(host, { peer, root });
              const hostHandle = r.getOrCreateHandle(host), childHandle = r.getOrCreateHandle(child);
              assert.ok(hostHandle < 0 && childHandle < 0, 'the host and shadow-tree nodes get scoped handles');
              assert.equal(r.getOrCreateHandle(child), childHandle, 'a node keeps its scoped handle');
              e.wire(hostHandle, childHandle, peer);
              return { host, child, scope, peer, childHandle };
            };

            const one = make();
            assert.equal(one.child.attributes['data-wired'], '1', 'DOM imports resolve scoped handles');
            e.__gc_collect();
            one.host.dispatchEvent(new Event('click'));
            one.child.dispatchEvent(new Event('click'));
            assert.equal(e.peerClicks(one.peer), 1, 'the scope retains the peer and listener closures across collection');
            assert.equal(e.clickCount(), 1);
            assert.equal(one.child.attributes.seen, '1', 'Event.target() inside the scope is a scoped handle');
            assert.equal(r.getHandleStats().global, globalBase, 'no scoped node or event entered the global table');

            e.keep(one.childHandle);
            r.releaseHandleScope(one.scope);
            assert.equal(e.useStash(), 1, 'a released scoped handle throws IllegalStateException in Java');
            one.child.dispatchEvent(new Event('click'));
            assert.equal(e.clickCount(), 1, 'release detaches listeners registered in the scope');
            assert.equal(r.getHandleStats().scopes.released, 1);
            body.removeChild(one.host);
            e.__gc_collect();
            assert.equal(e.__gc_live_bytes(), baseline, 'release frees the peer and its closures');

            return (async () => {
              const cycles = 1000;
              let made = 0;
              const batch = n => { for (let i = 0; i < n; i++) { const m = make(); m.child.dispatchEvent(new Event('click')); body.removeChild(m.host); made++; } };
              batch(cycles);
              for (let i = 0; i < 100 && r.getHandleStats().scopes.finalized < made; i++) { global.gc(); await settle(); await settle(); }
              assert.equal(r.getHandleStats().scopes.finalized, made, 'every unreachable host scope was finalized');
              assert.equal(r.getHandleStats().scoped, 0, 'finalization removed every scoped handle');
              e.__gc_collect();
              assert.equal(e.__gc_live_bytes(), baseline, 'finalization released every peer and closure');
              const heap = e.__gc_heap_bytes();
              batch(cycles);
              for (let i = 0; i < 100 && r.getHandleStats().scopes.finalized < made; i++) { global.gc(); await settle(); await settle(); }
              assert.equal(r.getHandleStats().scopes.finalized, made);
              e.__gc_collect();
              assert.equal(e.__gc_live_bytes(), baseline);
              assert.equal(e.__gc_heap_bytes(), heap, 'the heap high-water mark stays level after warm-up');
              assert.equal(r.getHandleStats().global, globalBase, 'the global handle table stays level');
            })();
            """);
    }

    @Test
    void listenerOwnershipFollowsTheNodeAcrossHandlesAndScopes() throws Exception {
        byte[] wasm = JavacToWasm.compile("App.java", """
            import one.dastec.javaw.dom.Element;
            import one.dastec.javaw.dom.Event;
            import one.dastec.javaw.dom.EventListener;
            class App {
                public static int pings;
                public static EventListener<Event> stored;
                public static void prepare() { int[] payload = new int[256]; stored = e -> pings += 1 + payload.length * 0; }
                public static void addStored(Element el) { el.addEventListener("ping", stored); }
                public static void removeStored(Element el) { el.removeEventListener("ping", stored); }
                public static int pingCount() { return pings; }
            }
            """, false).getWasm();
        run(wasm, """
            e.prepare();
            const ping = el => el.dispatchEvent(new Event('ping'));
            // Re-adding the same listener after its scope was released and the host got a new scope.
            const host = new FakeElement('x-host');
            const first = r.createHandleScope(host, { root: new FakeShadowRoot(host) });
            e.addStored(r.getOrCreateHandle(host));
            r.releaseHandleScope(first);
            r.createHandleScope(host, { root: host.shadowRoot });
            const again = r.getOrCreateHandle(host);
            e.addStored(again);
            ping(host);
            assert.equal(e.pingCount(), 1, 'a listener re-added in a new scope is attached');
            e.removeStored(again);
            ping(host);
            assert.equal(e.pingCount(), 1, 'and removed again without a double release');

            // Added through a scoped handle, removed through the node's older global handle.
            const other = new FakeElement('x-other');
            const globalHandle = r.getOrCreateHandle(other);
            assert.ok(globalHandle > 0);
            const scope = r.createHandleScope(other, { root: new FakeShadowRoot(other) });
            const scoped = r.getOrCreateHandle(other);
            assert.ok(scoped < 0);
            e.addStored(scoped);
            e.removeStored(globalHandle);
            ping(other);
            assert.equal(e.pingCount(), 1, 'removal through either handle detaches the listener');
            r.releaseHandleScope(scope); // would trap on a second release of the closure

            // A nested host whose own scope was released belongs to the parent's scope.
            const parent = new FakeElement('x-parent');
            const parentRoot = new FakeShadowRoot(parent);
            r.createHandleScope(parent, { root: parentRoot });
            const child = new FakeElement('x-child');
            parentRoot.appendChild(child);
            r.releaseHandleScope(r.createHandleScope(child, { root: new FakeShadowRoot(child) }));
            const globals = r.getHandleStats().global;
            assert.ok(r.getOrCreateHandle(child) < 0, 'the released child host gets a parent-scoped handle');
            assert.equal(r.getHandleStats().global, globals, 'and does not enter the global table');
            """);
    }

    @Test
    void exportedWrappersKeepTheirObjectsAliveUntilFinalized() throws Exception {
        byte[] wasm = JavacToWasm.compile("App.java", """
            import one.dastec.javaw.js.JS;
            @JS.Export("Box")
            class Box {
                public int[] data;
                public int value;
                public Box(int value) { this.value = value; data = new int[256]; data[255] = value; }
                public int read() { return value + data[255]; }
            }
            class App {
                public static int churn(int n) { int total = 0; for (int i = 0; i < n; i++) { int[] t = new int[300]; t[0] = i; total += t[0]; } return total; }
            }
            """, false).getWasm();
        run(wasm, """
            e.__gc_collect();
            const baseline = e.__gc_live_bytes();
            let box = new globalThis.Box(7);
            e.__gc_collect();
            e.churn(50);
            assert.equal(box.read(), 14, 'a JS wrapper keeps its Java object alive across collection');
            assert.equal(r.getHandleStats().global, 0, 'wrappers are not entered in the strong global table');
            box = null;
            return (async () => {
              for (let i = 0; i < 100 && r.wrapperStats.finalized < 1; i++) { global.gc(); await settle(); }
              assert.equal(r.wrapperStats.finalized, 1, 'the wrapper was finalized');
              e.__gc_collect();
              assert.equal(e.__gc_live_bytes(), baseline, 'finalization released the retained object');
            })();
            """);
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
        Process node = new ProcessBuilder("node", "--expose-gc", "-e", script,
                Path.of("src/main/resources/javaw-runtime.js").toAbsolutePath().toString()).redirectErrorStream(true).start();
        try {
            try (var input = node.getOutputStream()) { input.write(wasm); }
            assertTrue(node.waitFor(60, TimeUnit.SECONDS), "Node execution timed out");
            assertEquals(0, node.exitValue(), new String(node.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        } finally {
            if (node.isAlive()) node.destroyForcibly();
        }
    }
}
