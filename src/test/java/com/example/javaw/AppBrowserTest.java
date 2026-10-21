package com.example.javaw;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AppBrowserTest {

    private static HttpServer server;
    private static int port;
    private static Playwright playwright;
    private static Browser browser;

    @BeforeAll
    public static void setUp() throws Exception {
        // 1. Ensure App.java is compiled to App.wat and App.wasm
        Path appJavaPath = Paths.get("src/test/java/com/example/javaw/App.java").toAbsolutePath().normalize();
        CompilationUnit cu = StaticJavaParser.parse(appJavaPath);
        String watContent = JavacToWasm.generateWat(cu);
        Path appWatPath = Paths.get("src/test/java/com/example/javaw/App.wat").toAbsolutePath().normalize();
        try (FileWriter writer = new FileWriter(appWatPath.toFile())) {
            writer.write(watContent);
        }

        Path appWasmPath = Paths.get("src/test/java/com/example/javaw/App.wasm").toAbsolutePath().normalize();
        try {
            Process process = new ProcessBuilder("wat2wasm", appWatPath.toString(), "-o", appWasmPath.toString())
                    .redirectErrorStream(true)
                    .start();
            process.waitFor();
        } catch (IOException ignored) {
        }

        // 2. Start lightweight local HTTP server to serve demo files
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", new FileHandler());
        server.start();
        port = server.getAddress().getPort();

        // 3. Initialize Playwright and Browser
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }

    @AfterAll
    public static void tearDown() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    public void testAppDomInteractionsInBrowser() {
        Page page = browser.newPage();
        page.navigate("http://127.0.0.1:" + port + "/demo/index.html");

        // Wait for Java WASM App to initialize DOM elements
        page.waitForSelector("#status-text");

        // Verify Page Title
        assertEquals("Java WebAssembly DOM Application", page.title());

        // Verify Dynamic Heading created by Java App
        String headingText = page.textContent(".app-title");
        assertEquals("Java to WebAssembly DOM App", headingText);

        // Verify Initial Status text
        String initialStatus = page.textContent("#status-text");
        assertEquals("Count: 0", initialStatus);

        // Verify Increment Button text
        String buttonText = page.textContent("#btn-increment");
        assertEquals("Increment Counter", buttonText);

        // Verify Input value and Checkbox state
        assertEquals("Initial text", page.inputValue("#user-input"));
        assertTrue(page.isChecked("#user-check"));
        assertEquals(3, page.locator(".item-tag").count());

        // Click 1: Expect "Count: 1 click"
        page.click("#btn-increment");
        page.waitForFunction("() => document.getElementById('status-text').innerText === 'Count: 1 click'");
        assertEquals("Count: 1 click", page.textContent("#status-text"));
        assertTrue(page.locator("#status-text").getAttribute("class").contains("active"));

        // Click 2: Expect "Count: 2 clicks"
        page.click("#btn-increment");
        page.waitForFunction("() => document.getElementById('status-text').innerText === 'Count: 2 clicks'");
        assertEquals("Count: 2 clicks", page.textContent("#status-text"));

        // Click 3: Expect "Count: multiple clicks"
        page.click("#btn-increment");
        page.waitForFunction("() => document.getElementById('status-text').innerText === 'Count: multiple clicks'");
        assertEquals("Count: multiple clicks", page.textContent("#status-text"));
        assertTrue(page.locator("#status-text").getAttribute("class").contains("active"));

        page.close();
    }

    @Test
    public void testDynamicJavaWasmAppInBrowser() throws Exception {
        // Compile a dynamic Java source with loops and DOM changes
        String dynamicJavaSource = "package com.example.javaw;\n" +
                "import com.example.javaw.dom.DOM;\n" +
                "import com.example.javaw.dom.Element;\n" +
                "public class DynamicApp {\n" +
                "    public void run() {\n" +
                "        Element container = DOM.getElementById(\"test-container\");\n" +
                "        for (int i = 1; i <= 3; i++) {\n" +
                "            Element item = DOM.createElement(\"div\");\n" +
                "            item.setAttribute(\"class\", \"item-node\");\n" +
                "            item.setInnerText(\"Item Node\");\n" +
                "            container.appendChild(item);\n" +
                "        }\n" +
                "    }\n" +
                "}\n";

        CompilationUnit cu = StaticJavaParser.parse(dynamicJavaSource);
        String watContent = JavacToWasm.generateWat(cu);
        File tempWat = File.createTempFile("dynamic_app", ".wat");
        File tempWasm = File.createTempFile("dynamic_app", ".wasm");

        try (FileWriter writer = new FileWriter(tempWat)) {
            writer.write(watContent);
        }

        Process process = new ProcessBuilder("wat2wasm", tempWat.getAbsolutePath(), "-o", tempWasm.getAbsolutePath())
                .redirectErrorStream(true)
                .start();
        process.waitFor();

        byte[] wasmBytes = Files.readAllBytes(tempWasm.toPath());
        String base64Wasm = java.util.Base64.getEncoder().encodeToString(wasmBytes);

        Page page = browser.newPage();
        // Load runtime and instantiate base64 wasm directly
        String htmlContent = "<!DOCTYPE html><html><body>" +
                "<div id=\"test-container\"></div>" +
                "<script src=\"http://127.0.0.1:" + port + "/src/main/resources/javaw-runtime.js\"></script>" +
                "<script>" +
                "  async function execute() {" +
                "    const binary = Uint8Array.from(atob('" + base64Wasm + "'), c => c.charCodeAt(0));" +
                "    const { exports } = await Javaw.instantiateJavaw(binary);" +
                "    exports.run();" +
                "  }" +
                "  window.onload = execute;" +
                "</script></body></html>";

        page.setContent(htmlContent);
        page.waitForSelector(".item-node");

        int count = page.locator(".item-node").count();
        assertEquals(3, count);

        page.close();
        tempWat.delete();
        tempWasm.delete();
    }

    @Test
    public void testCanvasDrawingInBrowser() throws Exception {
        String canvasJavaSource = "package com.example.javaw;\n" +
                "import com.example.javaw.dom.DOM;\n" +
                "import com.example.javaw.dom.Element;\n" +
                "import com.example.javaw.dom.CanvasRenderingContext2D;\n" +
                "public class CanvasApp {\n" +
                "    public void draw() {\n" +
                "        Element canvas = DOM.getElementById(\"browser-canvas\");\n" +
                "        canvas.setWidth(300);\n" +
                "        canvas.setHeight(150);\n" +
                "        CanvasRenderingContext2D ctx = canvas.getContext2D();\n" +
                "        ctx.setFillStyle(\"#00ff00\");\n" +
                "        ctx.fillRect(0, 0, 300, 150);\n" +
                "        ctx.setFillStyle(\"#0000ff\");\n" +
                "        ctx.beginPath();\n" +
                "        ctx.arc(150, 75, 40, 0, 6.28);\n" +
                "        ctx.fill();\n" +
                "    }\n" +
                "}\n";

        CompilationUnit cu = StaticJavaParser.parse(canvasJavaSource);
        String watContent = JavacToWasm.generateWat(cu);
        File tempWat = File.createTempFile("canvas_app", ".wat");
        File tempWasm = File.createTempFile("canvas_app", ".wasm");

        try (FileWriter writer = new FileWriter(tempWat)) {
            writer.write(watContent);
        }

        Process process = new ProcessBuilder("wat2wasm", tempWat.getAbsolutePath(), "-o", tempWasm.getAbsolutePath())
                .redirectErrorStream(true)
                .start();
        process.waitFor();

        byte[] wasmBytes = Files.readAllBytes(tempWasm.toPath());
        String base64Wasm = java.util.Base64.getEncoder().encodeToString(wasmBytes);

        Page page = browser.newPage();
        String htmlContent = "<!DOCTYPE html><html><body>" +
                "<canvas id=\"browser-canvas\"></canvas>" +
                "<script src=\"http://127.0.0.1:" + port + "/src/main/resources/javaw-runtime.js\"></script>" +
                "<script>" +
                "  async function execute() {" +
                "    const binary = Uint8Array.from(atob('" + base64Wasm + "'), c => c.charCodeAt(0));" +
                "    const { exports } = await Javaw.instantiateJavaw(binary);" +
                "    exports.draw();" +
                "    window.__canvasReady = true;" +
                "  }" +
                "  window.onload = execute;" +
                "</script></body></html>";

        page.setContent(htmlContent);
        page.waitForFunction("() => window.__canvasReady === true");

        Object widthObj = page.evaluate("() => document.getElementById('browser-canvas').width");
        assertEquals(300, ((Number) widthObj).intValue());

        Object heightObj = page.evaluate("() => document.getElementById('browser-canvas').height");
        assertEquals(150, ((Number) heightObj).intValue());

        // Verify drawn pixel at (150, 75) is blue (#0000ff -> [0, 0, 255, 255])
        Object pixelObj = page.evaluate("() => {" +
                "  const ctx = document.getElementById('browser-canvas').getContext('2d');" +
                "  const p = ctx.getImageData(150, 75, 1, 1).data;" +
                "  return Array.from(p);" +
                "}");
        assertTrue(pixelObj.toString().contains("255"));

        page.close();
        tempWat.delete();
        tempWasm.delete();
    }

    private static class FileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            Path filePath = Paths.get(path).toAbsolutePath().normalize();

            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            byte[] bytes = Files.readAllBytes(filePath);
            String contentType = "text/plain";
            if (path.endsWith(".html")) {
                contentType = "text/html; charset=UTF-8";
            } else if (path.endsWith(".js")) {
                contentType = "application/javascript; charset=UTF-8";
            } else if (path.endsWith(".wasm")) {
                contentType = "application/wasm";
            } else if (path.endsWith(".css")) {
                contentType = "text/css; charset=UTF-8";
            }

            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}
