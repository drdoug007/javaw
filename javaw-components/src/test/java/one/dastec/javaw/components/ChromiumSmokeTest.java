package one.dastec.javaw.components;

import com.sun.net.httpserver.HttpServer;
import one.dastec.javaw.JavacToWasm;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * M6 opt-in Chromium smoke test: what happy-dom cannot show. Upgrade-time attribute reactions, adoptedCallback,
 * composed events retargeted across two shadow roots and shared adoptedStyleSheets, in a real browser.
 *
 * <p>Skipped unless {@code -Djavaw.chrome=<binary>} (or the environment variable {@code JAVAW_CHROME}) names a
 * local Chrome or Chromium executable, e.g. a Playwright {@code chrome-headless-shell}. Nothing is downloaded. The
 * test serves the compiled module from a loopback HTTP server with COOP/COEP headers (javaw memory is shared, so the
 * page must be cross-origin isolated), runs Chrome headless and receives the page's results by POST.
 */
class ChromiumSmokeTest {
    static final String PROGRAM = """
        import one.dastec.javaw.components.*;

        @CustomElement(value = "smoke-probe", observedAttributes = {"mode"})
        class Probe extends WebComponent {
            static final Css STYLES = css(":host { display: block; color: rgb(1, 2, 3); } p { color: rgb(4, 5, 6); }");
            @Property private String log = "";
            @Override protected void attributeChanged(String name, String oldValue, String newValue) {
                log = log + "attr:" + name + "=" + newValue + ";";
            }
            @Override protected void connected() { log = log + "connected;"; }
            @Override protected void disconnected() { log = log + "disconnected;"; }
            @Override protected void adopted() { log = log + "adopted;"; }
            @Override protected Template render() { return html("<p>probe</p><smoke-inner id=\\"inner\\"></smoke-inner>"); }
        }

        @CustomElement("smoke-inner")
        class Inner extends WebComponent {
            static final Css STYLES = css("button { color: rgb(7, 8, 9); }");
            public record Ping(int n) implements ComponentEvent {}
            @EventOptions(composed = false) public record Local(int n) implements ComponentEvent {}
            @Override protected Template render() { return html("<button id=\\"go\\" @click=${this::go}>go</button>"); }
            private void go() { dispatch(new Ping(7)); dispatch(new Local(8)); }
        }
        """;

    static final String PAGE = """
        <!doctype html>
        <meta charset="utf-8">
        <body>
        <smoke-probe id="early" mode="parsed"></smoke-probe>
        <smoke-probe id="second"></smoke-probe>
        <script type="module">
        const r = {};
        const tick = () => new Promise(resolve => setTimeout(resolve, 0));
        try {
          r.isolated = self.crossOriginIsolated;
          const early = document.getElementById('early');
          r.beforeDefine = customElements.get('smoke-probe') === undefined && !(early.shadowRoot);
          const mod = await import('./Smoke.components.js');
          await mod.load();
          await mod.whenDefined();
          await tick();
          r.upgradeLog = early.log;
          early.setAttribute('mode', 'later');
          r.afterSet = early.log;

          const sr = early.shadowRoot;
          r.sheets = sr.adoptedStyleSheets.length;
          r.styleTags = sr.querySelectorAll('style').length;
          r.hostColor = getComputedStyle(early).color;
          r.pColor = getComputedStyle(sr.querySelector('p')).color;
          const inner = sr.getElementById('inner');
          await tick();
          r.innerSheets = inner.shadowRoot.adoptedStyleSheets.length;
          r.buttonColor = getComputedStyle(inner.shadowRoot.getElementById('go')).color;
          const second = document.getElementById('second');
          r.sharedSheet = second.shadowRoot.adoptedStyleSheets[0] === sr.adoptedStyleSheets[0];

          const seen = [];
          document.addEventListener('ping', e => seen.push(['document', e.target.id, e.composed, e.composedPath()[0].id, e.detail.n]));
          sr.addEventListener('ping', e => seen.push(['outer-root', e.target.id]));
          document.addEventListener('local', e => seen.push(['document-local']));
          inner.addEventListener('local', e => seen.push(['inner-local', e.target.id, e.composed, e.detail.n]));
          inner.shadowRoot.getElementById('go').click();
          r.events = seen;

          const other = document.implementation.createHTMLDocument('other');
          other.body.appendChild(second);
          r.adoptLog = second.log;
          r.adoptedOwner = second.ownerDocument === other;
        } catch (error) {
          r.error = String(error && error.stack || error);
        }
        await fetch('/result', { method: 'POST', body: JSON.stringify(r) });
        </script>
        """;

    static String chrome() {
        String path = System.getProperty("javaw.chrome", System.getenv("JAVAW_CHROME"));
        if (path == null || path.isBlank()) return null;
        // A configured but unusable path is a setup error, not an opt-out: fail instead of skipping silently.
        if (!Files.isRegularFile(Path.of(path)) || !Files.isExecutable(Path.of(path)))
            throw new AssertionError("javaw.chrome/JAVAW_CHROME=" + path + " is not an executable file"
                + " (for a macOS .app bundle use the binary under Contents/MacOS/)");
        return path;
    }

    @Test
    void realBrowserLifecycleEventsAndStyles() throws Exception {
        String chrome = chrome();
        Assumptions.assumeTrue(chrome != null, "set -Djavaw.chrome=<Chrome or chrome-headless-shell binary> to run the Chromium smoke test");
        Path dir = Files.createDirectories(Path.of("target/chromium-smoke").toAbsolutePath());
        var result = JavacToWasm.compile("Smoke.java", PROGRAM, false);
        assertNotNull(result.getWasm());
        Files.write(dir.resolve("Smoke.wasm"), result.getWasm());
        Files.writeString(dir.resolve("Smoke.components.js"), result.getArtifacts().get("Smoke.components.js"));
        Files.copy(Path.of("../javaw-core/src/main/resources/javaw-runtime.js"), dir.resolve("javaw-runtime.js"),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        Files.writeString(dir.resolve("index.html"), PAGE);

        String json = runPage(chrome, dir, "index.html");
        System.out.println("[chromium-smoke] " + json);
        assertFalse(json.contains("\"error\""), json);
        assertTrue(json.contains("\"isolated\":true"), json);
        assertTrue(json.contains("\"beforeDefine\":true"), json);
        // Upgrade: the parsed attribute is reported before connectedCallback, which happy-dom skips.
        assertTrue(json.contains("\"upgradeLog\":\"attr:mode=parsed;connected;\""), json);
        assertTrue(json.contains("\"afterSet\":\"attr:mode=parsed;connected;attr:mode=later;\""), json);
        // Styles: constructable sheets, no <style> fallback, applied to host and shadow content, one sheet per class.
        assertTrue(json.contains("\"sheets\":1"), json);
        assertTrue(json.contains("\"styleTags\":0"), json);
        assertTrue(json.contains("\"hostColor\":\"rgb(1, 2, 3)\""), json);
        assertTrue(json.contains("\"pColor\":\"rgb(4, 5, 6)\""), json);
        assertTrue(json.contains("\"innerSheets\":1"), json);
        assertTrue(json.contains("\"buttonColor\":\"rgb(7, 8, 9)\""), json);
        assertTrue(json.contains("\"sharedSheet\":true"), json);
        // Events: dispatch fires on the inner host. The composed event is seen as <smoke-inner> in the outer shadow
        // root and retargeted to the outer host at the document; the non-composed one never leaves the outer root.
        assertTrue(json.contains("\"events\":[[\"outer-root\",\"inner\"],[\"document\",\"early\",true,\"inner\",7],[\"inner-local\",\"inner\",false,8]]"), json);
        // adoptedCallback: moving into another document runs disconnected, adopted, then connected in the new document.
        assertTrue(json.contains("\"adoptedOwner\":true"), json);
        assertTrue(json.contains("\"adoptLog\":\"connected;disconnected;adopted;connected;\""), json);
    }

    /** The size-budget examples, javaw-counter and todo-list, each in its own module, run together on one page. */
    @Test
    void examplesRunInChromium() throws Exception {
        String chrome = chrome();
        Assumptions.assumeTrue(chrome != null, "set -Djavaw.chrome=<Chrome or chrome-headless-shell binary> to run the Chromium smoke test");
        Path dir = Files.createDirectories(Path.of("target/chromium-examples").toAbsolutePath());
        SizeBudgetTest.compile("JavawCounter", dir);
        SizeBudgetTest.compile("TodoList", dir);
        Files.writeString(dir.resolve("index.html"), """
            <!doctype html>
            <meta charset="utf-8">
            <javaw-counter label="Clicks"></javaw-counter>
            <todo-list heading="Shopping"></todo-list>
            <script type="module">
            const r = {};
            const tick = () => new Promise(resolve => setTimeout(resolve, 0));
            try {
              const [counter, todo] = await Promise.all([import('./JavawCounter.components.js'), import('./TodoList.components.js')]);
              await Promise.all([counter.load(), todo.load()]);
              await tick();
              const c = document.querySelector('javaw-counter');
              const counts = [];
              c.addEventListener('count-changed', e => counts.push(e.detail.count));
              c.shadowRoot.querySelector('button').click();
              c.shadowRoot.querySelector('button').click();
              await tick();
              r.counter = c.shadowRoot.querySelector('button').textContent;
              r.countAttr = c.getAttribute('count');
              r.counts = counts;
              const t = document.querySelector('todo-list');
              const remaining = [];
              t.addEventListener('remaining-changed', e => remaining.push(e.detail.remaining));
              const input = t.shadowRoot.querySelector('input');
              for (const text of ['milk', 'eggs']) {
                input.value = text;
                input.dispatchEvent(new Event('input'));
                await tick();
                t.shadowRoot.querySelector('button').click();
                await tick();
              }
              t.shadowRoot.querySelector('li').click();
              await tick();
              r.heading = t.shadowRoot.querySelector('h2').textContent;
              r.items = [...t.shadowRoot.querySelectorAll('li')].map(li => li.textContent + (li.className ? '/' + li.className : ''));
              r.left = t.shadowRoot.querySelector('p').textContent;
              r.remaining = remaining;
              r.disabled = t.shadowRoot.querySelector('button').disabled;
            } catch (error) {
              r.error = String(error && error.stack || error);
            }
            await fetch('/result', { method: 'POST', body: JSON.stringify(r) });
            </script>
            """);
        String json = runPage(chrome, dir, "index.html");
        System.out.println("[chromium-examples] " + json);
        assertEquals("{\"counter\":\"Clicks: 2\",\"countAttr\":\"2\",\"counts\":[1,2],\"heading\":\"Shopping\","
                + "\"items\":[\"milk/done\",\"eggs\"],\"left\":\"1 left\",\"remaining\":[1,2,1],\"disabled\":true}", json);
    }

    /** Serves {@code dir} with COOP/COEP headers, opens {@code page} in headless Chrome and returns the POSTed body. */
    static String runPage(String chrome, Path dir, String page) throws Exception {
        CompletableFuture<String> posted = new CompletableFuture<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            try (exchange) {
                String path = exchange.getRequestURI().getPath();
                exchange.getResponseHeaders().add("Cross-Origin-Opener-Policy", "same-origin");
                exchange.getResponseHeaders().add("Cross-Origin-Embedder-Policy", "require-corp");
                if ("POST".equals(exchange.getRequestMethod()) && path.equals("/result")) {
                    posted.complete(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                    exchange.sendResponseHeaders(204, -1);
                    return;
                }
                Path file = dir.resolve(path.substring(1)).normalize();
                if (!file.startsWith(dir) || !Files.isRegularFile(file)) { exchange.sendResponseHeaders(404, -1); return; }
                String type = path.endsWith(".html") ? "text/html" : path.endsWith(".js") ? "text/javascript"
                        : path.endsWith(".wasm") ? "application/wasm" : "application/octet-stream";
                exchange.getResponseHeaders().add("Content-Type", type);
                byte[] body = Files.readAllBytes(file);
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            } catch (IOException ignored) {
            }
        });
        server.start();
        Path profile = Files.createTempDirectory(Path.of("target").toAbsolutePath(), "chrome-profile");
        List<String> command = new ArrayList<>(List.of(chrome, "--headless", "--disable-gpu", "--no-first-run",
                "--no-default-browser-check", "--disable-extensions", "--user-data-dir=" + profile,
                "--remote-debugging-port=0", "http://127.0.0.1:" + server.getAddress().getPort() + "/" + page));
        Process process = new ProcessBuilder(command).redirectErrorStream(true)
                .redirectOutput(dir.resolve("chrome.log").toFile()).start();
        try {
            return posted.get(90, TimeUnit.SECONDS);
        } finally {
            process.destroy();
            if (!process.waitFor(10, TimeUnit.SECONDS)) process.destroyForcibly();
            server.stop(0);
        }
    }
}
