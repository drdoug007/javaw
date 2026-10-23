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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
            Process process = new ProcessBuilder("wat2wasm", "--enable-threads", "--enable-exceptions", appWatPath.toString(), "-o", appWasmPath.toString())
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

        Process process = new ProcessBuilder("wat2wasm", "--enable-threads", "--enable-exceptions", tempWat.getAbsolutePath(), "-o", tempWasm.getAbsolutePath())
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

        Process process = new ProcessBuilder("wat2wasm", "--enable-threads", "--enable-exceptions", tempWat.getAbsolutePath(), "-o", tempWasm.getAbsolutePath())
                .redirectErrorStream(true)
                .start();
        process.waitFor();

        byte[] wasmBytes = Files.readAllBytes(tempWasm.toPath());
        String base64Wasm = java.util.Base64.getEncoder().encodeToString(wasmBytes);

        Page page = browser.newPage();
        page.onConsoleMessage(msg -> System.out.println("PAGE LOG: " + msg.text()));
        page.onPageError(err -> System.out.println("PAGE ERR: " + err));
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

    @Test
    public void testBrowserAsyncTimersAndFetch() throws Exception {
        String asyncJavaSource = "package com.example.javaw;\n" +
                "import com.example.javaw.dom.DOM;\n" +
                "import com.example.javaw.dom.Element;\n" +
                "import com.example.javaw.dom.Window;\n" +
                "public class AsyncBrowserApp {\n" +
                "    public static void start() {\n" +
                "        Window.setTimeout(\"onTimerTriggered\", 50);\n" +
                "        Window.fetch(\"http://127.0.0.1:" + port + "/src/test/resources/data.json\", \"onFetchSuccess\");\n" +
                "    }\n" +
                "    public static void onTimerTriggered() {\n" +
                "        Element status = DOM.getElementById(\"async-status\");\n" +
                "        status.setInnerText(\"Timer Activated!\");\n" +
                "    }\n" +
                "    public static void onFetchSuccess(String data) {\n" +
                "        Element dataEl = DOM.getElementById(\"async-data\");\n" +
                "        dataEl.setInnerText(data);\n" +
                "    }\n" +
                "}\n";

        // Ensure test data file exists
        Path testDataDir = Paths.get("src/test/resources");
        Files.createDirectories(testDataDir);
        Files.writeString(testDataDir.resolve("data.json"), "{\"message\":\"Async WASM Works!\"}");

        CompilationUnit cu = StaticJavaParser.parse(asyncJavaSource);
        String watContent = JavacToWasm.generateWat(cu);
        File tempWat = File.createTempFile("async_app", ".wat");
        File tempWasm = File.createTempFile("async_app", ".wasm");

        try (FileWriter writer = new FileWriter(tempWat)) {
            writer.write(watContent);
        }

        Process process = new ProcessBuilder("wat2wasm", "--enable-threads", "--enable-exceptions", tempWat.getAbsolutePath(), "-o", tempWasm.getAbsolutePath())
                .redirectErrorStream(true)
                .start();
        process.waitFor();

        byte[] wasmBytes = Files.readAllBytes(tempWasm.toPath());
        String base64Wasm = java.util.Base64.getEncoder().encodeToString(wasmBytes);

        Page page = browser.newPage();
        String htmlContent = "<!DOCTYPE html><html><body>" +
                "<div id=\"async-status\">Initial</div>" +
                "<div id=\"async-data\">No Data</div>" +
                "<script src=\"http://127.0.0.1:" + port + "/src/main/resources/javaw-runtime.js\"></script>" +
                "<script>" +
                "  async function execute() {" +
                "    const binary = Uint8Array.from(atob('" + base64Wasm + "'), c => c.charCodeAt(0));" +
                "    const { exports } = await Javaw.instantiateJavaw(binary);" +
                "    exports.start();" +
                "  }" +
                "  window.onload = execute;" +
                "</script>" +
                "</body></html>";

        page.setContent(htmlContent);

        // Wait for timer update
        page.waitForFunction("() => document.getElementById('async-status').innerText === 'Timer Activated!'");
        // Wait for fetch update
        page.waitForFunction("() => document.getElementById('async-data').innerText.includes('Async WASM Works!')");

        page.close();
        tempWat.delete();
        tempWasm.delete();
    }

    @Test
    public void testBrowserJSObjectDynamicInterop() throws Exception {
        String jsInteropJavaSource = "package com.example.javaw;\n" +
                "import com.example.javaw.dom.DOM;\n" +
                "import com.example.javaw.dom.Element;\n" +
                "import com.example.javaw.dom.JSObject;\n" +
                "public class JsInteropBrowserApp {\n" +
                "    public static void start() {\n" +
                "        JSObject custom = JSObject.eval(\"({ name: 'JavaWasm', version: '2.0', active: true })\");\n" +
                "        JSObject arr = JSObject.createArray();\n" +
                "        arr.push(\"Alpha\");\n" +
                "        arr.push(\"Beta\");\n" +
                "        Element statusEl = DOM.getElementById(\"js-status\");\n" +
                "        statusEl.setInnerText(custom.getString(\"name\") + \" \" + custom.getString(\"version\"));\n" +
                "        Element arrayEl = DOM.getElementById(\"js-array\");\n" +
                "        arrayEl.setInnerText(arr.getIndex(0).asString() + \" and \" + arr.getIndex(1).asString());\n" +
                "        JSObject math = JSObject.global().get(\"Math\");\n" +
                "        Element calcEl = DOM.getElementById(\"js-calc\");\n" +
                "        calcEl.setInnerText(\"Calculated: \" + math.callInt(\"max\", 500));\n" +
                "    }\n" +
                "}\n";

        CompilationUnit cu = StaticJavaParser.parse(jsInteropJavaSource);
        String watContent = JavacToWasm.generateWat(cu);
        File tempWat = File.createTempFile("js_interop_app", ".wat");
        File tempWasm = File.createTempFile("js_interop_app", ".wasm");

        try (FileWriter writer = new FileWriter(tempWat)) {
            writer.write(watContent);
        }

        Process process = new ProcessBuilder("wat2wasm", "--enable-threads", "--enable-exceptions", tempWat.getAbsolutePath(), "-o", tempWasm.getAbsolutePath())
                .redirectErrorStream(true)
                .start();
        process.waitFor();

        byte[] wasmBytes = Files.readAllBytes(tempWasm.toPath());
        String base64Wasm = java.util.Base64.getEncoder().encodeToString(wasmBytes);

        Page page = browser.newPage();
        String htmlContent = "<!DOCTYPE html><html><body>" +
                "<div id=\"js-status\">Initial</div>" +
                "<div id=\"js-array\">Empty</div>" +
                "<div id=\"js-calc\">Zero</div>" +
                "<script src=\"http://127.0.0.1:" + port + "/src/main/resources/javaw-runtime.js\"></script>" +
                "<script>" +
                "  async function execute() {" +
                "    const binary = Uint8Array.from(atob('" + base64Wasm + "'), c => c.charCodeAt(0));" +
                "    const { exports } = await Javaw.instantiateJavaw(binary);" +
                "    exports.start();" +
                "  }" +
                "  window.onload = execute;" +
                "</script>" +
                "</body></html>";

        page.setContent(htmlContent);

        page.waitForFunction("() => document.getElementById('js-status').innerText === 'JavaWasm 2.0'");
        page.waitForFunction("() => document.getElementById('js-array').innerText === 'Alpha and Beta'");
        page.waitForFunction("() => document.getElementById('js-calc').innerText === 'Calculated: 500'");

        page.close();
        tempWat.delete();
        tempWasm.delete();
    }

    @Test
    public void testBrowserInteractiveTerminalAndRepl() {
        Page page = browser.newPage();
        page.navigate("http://127.0.0.1:" + port + "/demo/index.html");

        // Wait for terminal wrapper
        page.waitForSelector(".javaw-terminal-wrapper");
        assertEquals("javaw> ", page.textContent(".javaw-terminal-prompt"));

        // Type 'help' and submit
        page.fill(".javaw-terminal-input", "help");
        page.press(".javaw-terminal-input", "Enter");

        page.waitForFunction("() => document.querySelector('.javaw-terminal-screen').innerText.includes('Display this help message')");
        String screenText = page.textContent(".javaw-terminal-screen");
        assertTrue(screenText.contains("javaw WebAssembly Terminal & REPL"));
        assertTrue(screenText.contains("help"));

        // Type 'js 123 + 456' and submit
        page.fill(".javaw-terminal-input", "js 123 + 456");
        page.press(".javaw-terminal-input", "Enter");
        page.waitForFunction("() => document.querySelector('.javaw-terminal-screen').innerText.includes('579')");

        // Type 'history' and submit
        page.fill(".javaw-terminal-input", "history");
        page.press(".javaw-terminal-input", "Enter");
        page.waitForFunction("() => document.querySelector('.javaw-terminal-screen').innerText.includes('1: help')");

        page.close();
    }

    @Test
    public void testLog4j2DemoInBrowser() throws Exception {
        // Compile Log4j2Demo if needed
        Path log4jJavaPath = Paths.get("examples/log4j/Log4j2Demo.java").toAbsolutePath().normalize();
        if (!Files.exists(log4jJavaPath)) {
            log4jJavaPath = Paths.get("../examples/log4j/Log4j2Demo.java").toAbsolutePath().normalize();
        }
        if (Files.exists(log4jJavaPath)) {
            CompilationUnit cu = StaticJavaParser.parse(log4jJavaPath);
            String wat = JavacToWasm.generateWat(cu);
            Path watPath = log4jJavaPath.getParent().resolve("Log4j2Demo.wat");
            Path wasmPath = log4jJavaPath.getParent().resolve("Log4j2Demo.wasm");
            Files.writeString(watPath, wat);
            try {
                Process process = new ProcessBuilder("wat2wasm", "--enable-threads", "--enable-exceptions", watPath.toString(), "-o", wasmPath.toString())
                        .redirectErrorStream(true)
                        .start();
                process.waitFor();
            } catch (IOException ignored) {}
        }

        Page page = browser.newPage();
        java.util.List<String> consoleErrors = new java.util.ArrayList<>();
        page.onConsoleMessage(msg -> {
            if ("error".equals(msg.type())) {
                consoleErrors.add(msg.text());
            }
        });
        page.onPageError(err -> consoleErrors.add(err));

        page.navigate("http://127.0.0.1:" + port + "/examples/log4j/index.html");

        // Wait for Log4j2 UI to mount into DOM
        page.waitForSelector("#log4j-app h3");
        assertEquals("Log4j2 Enterprise Logging Engine", page.textContent("#log4j-app h3"));

        // Verify initial console errors list is empty (no SharedArrayBuffer decode error)
        assertTrue(consoleErrors.stream().noneMatch(e -> e.contains("ArrayBufferView") || e.contains("TextDecoder")),
                "Must not throw TextDecoder / ArrayBufferView shared memory error: " + consoleErrors);

        // Click toolbar actions
        page.click("button:has-text('Run Full Lifecycle')");
        page.click("button:has-text('Auth Event')");
        page.click("button:has-text('DB Metrics')");
        page.click("button:has-text('Simulate Warning')");
        page.click("button:has-text('Simulate Error')");

        // Assert log entries are rendered into the DOM
        page.waitForFunction("() => document.getElementById('log4j-app').innerText.includes('Log4j2')");
        String logText = page.textContent("#log4j-app");
        assertTrue(logText.contains("Log4j2 Enterprise Logging Engine"));

        // Verify no TextDecoder errors occurred during simulation execution
        assertTrue(consoleErrors.stream().noneMatch(e -> e.contains("ArrayBufferView") || e.contains("TextDecoder")),
                "No TextDecoder shared buffer errors during simulation: " + consoleErrors);

        page.close();
    }

    @Test
    public void testTodoAppInBrowser() throws Exception {
        Path todoJavaPath = Paths.get("examples/todo/TodoApp.java").toAbsolutePath().normalize();
        if (!Files.exists(todoJavaPath)) {
            todoJavaPath = Paths.get("../examples/todo/TodoApp.java").toAbsolutePath().normalize();
        }
        if (Files.exists(todoJavaPath)) {
            CompilationUnit cu = StaticJavaParser.parse(todoJavaPath);
            String wat = JavacToWasm.generateWat(cu);
            Path watPath = todoJavaPath.getParent().resolve("TodoApp.wat");
            Path wasmPath = todoJavaPath.getParent().resolve("TodoApp.wasm");
            Files.writeString(watPath, wat);
            try {
                Process process = new ProcessBuilder("wat2wasm", "--enable-threads", "--enable-exceptions", watPath.toString(), "-o", wasmPath.toString())
                        .redirectErrorStream(true)
                        .start();
                process.waitFor();
            } catch (IOException ignored) {}
        }

        Page page = browser.newPage();
        java.util.List<String> consoleErrors = new java.util.ArrayList<>();
        page.onConsoleMessage(msg -> {
            if ("error".equals(msg.type())) consoleErrors.add(msg.text());
        });
        page.onPageError(consoleErrors::add);

        page.navigate("http://127.0.0.1:" + port + "/examples/todo/index.html");

        page.waitForSelector("#todo-app h2");
        assertEquals("Java WebAssembly Todo Application", page.textContent("#todo-app h2"));

        assertTrue(consoleErrors.stream().noneMatch(e -> e.contains("ArrayBufferView") || e.contains("TextDecoder")),
                "TodoApp must not throw TextDecoder error: " + consoleErrors);

        page.close();
    }

    @Test
    public void testBytecodeAlgorithmsDemoInBrowser() throws Exception {
        Path bytecodeJavaPath = Paths.get("examples/bytecode/BytecodeAlgorithmsDemo.java").toAbsolutePath().normalize();
        if (!Files.exists(bytecodeJavaPath)) {
            bytecodeJavaPath = Paths.get("../examples/bytecode/BytecodeAlgorithmsDemo.java").toAbsolutePath().normalize();
        }
        if (Files.exists(bytecodeJavaPath)) {
            CompilationUnit cu = StaticJavaParser.parse(bytecodeJavaPath);
            String wat = JavacToWasm.generateWat(cu);
            Path watPath = bytecodeJavaPath.getParent().resolve("BytecodeAlgorithmsDemo.wat");
            Path wasmPath = bytecodeJavaPath.getParent().resolve("BytecodeAlgorithmsDemo.wasm");
            Files.writeString(watPath, wat);
            try {
                Process process = new ProcessBuilder("wat2wasm", "--enable-threads", "--enable-exceptions", watPath.toString(), "-o", wasmPath.toString())
                        .redirectErrorStream(true)
                        .start();
                process.waitFor();
            } catch (IOException ignored) {}
        }

        Page page = browser.newPage();
        java.util.List<String> consoleErrors = new java.util.ArrayList<>();
        page.onConsoleMessage(msg -> {
            if ("error".equals(msg.type())) consoleErrors.add(msg.text());
        });
        page.onPageError(consoleErrors::add);

        page.navigate("http://127.0.0.1:" + port + "/examples/bytecode/index.html");

        page.waitForSelector("#bytecode-app h3");
        assertEquals("Bytecode & Relooper Algorithmic Engine", page.textContent("#bytecode-app h3"));

        assertTrue(consoleErrors.stream().noneMatch(e -> e.contains("ArrayBufferView") || e.contains("TextDecoder")),
                "BytecodeAlgorithmsDemo must not throw TextDecoder error: " + consoleErrors);

        page.close();
    }

    @Test
    public void testExamplesShowcaseIndexInBrowser() throws Exception {
        Page page = browser.newPage();
        java.util.List<String> consoleErrors = new java.util.ArrayList<>();
        page.onConsoleMessage(msg -> {
            if (msg.text().contains("JAVAW") || msg.text().contains("GameOfLife") || msg.text().contains("step") || msg.text().contains("gen-info")) {
                System.out.println("[BROWSER LOG] " + msg.type() + ": " + msg.text());
            }
            if ("error".equals(msg.type())) consoleErrors.add(msg.text());
        });
        page.onPageError(consoleErrors::add);

        page.navigate("http://127.0.0.1:" + port + "/examples/index.html");

        // 1. Todo App tab (default active)
        page.waitForSelector("#todo-app h2");
        assertEquals("Java WebAssembly Todo Application", page.textContent("#todo-app h2"));

        // 2. Canvas Game of Life tab
        page.click("button:has-text('2. Canvas Game of Life')");
        page.waitForSelector("#game-container h2");
        assertEquals("Conway's Game of Life (HTML5 Canvas 2D)", page.textContent("#game-container h2"));
        page.waitForSelector("#gen-info");
        assertEquals("Generation: 0", page.textContent("#gen-info"));
        
        System.out.println("Buttons in game-container: " + page.evaluate("() => Array.from(document.querySelectorAll('#game-container button')).map(b => b.innerText + ' (' + b.outerHTML + ')')"));
        
        Object clickResult = page.evaluate("() => {\n" +
                "  const btn = Array.from(document.querySelectorAll('#game-container button')).find(b => b.innerText.includes('Next Step'));\n" +
                "  if (!btn) return 'BUTTON NOT FOUND';\n" +
                "  console.log('Dispatching click on btn:', btn);\n" +
                "  btn.click();\n" +
                "  return 'CLICKED, gen is: ' + (document.getElementById('gen-info') ? document.getElementById('gen-info').innerText : 'no gen-info');\n" +
                "}");
        System.out.println("Click evaluation result: " + clickResult);

        // 3. 128-Bit SIMD Physics tab
        page.click("button:has-text('3. 128-Bit SIMD Physics')");
        page.waitForSelector("#simd-container h2");
        assertEquals("128-Bit SIMD Vectorized Particle Engine", page.textContent("#simd-container h2"));
        page.waitForSelector("#simd-status");
        page.click("button:has-text('SIMD Update Tick')");

        // 4. JavaScript Interop tab
        page.click("button:has-text('4. JavaScript Interop')");
        page.waitForSelector("#js-interop-app h2");
        assertEquals("GraalVM Web Image-Style JavaScript Interop", page.textContent("#js-interop-app h2"));
        page.waitForFunction("() => document.getElementById('js-interop-app') && document.getElementById('js-interop-app').innerText.includes('JSON Stringify demo:')");
        String jsInteropText = page.textContent("#js-interop-app");
        assertTrue(jsInteropText.contains("JSON Stringify demo: {\"engine\":\"javaw\",\"version\":\"1.0\"}"),
                "JSON Stringify demo must serialize object properly: " + jsInteropText);
        assertFalse(jsInteropText.contains("JSON Stringify demo: null"),
                "JSON Stringify demo must not be null: " + jsInteropText);

        // 5. WASI Terminal tab
        page.click("button:has-text('5. WASI Terminal')");
        page.waitForSelector("#terminal-container .javaw-terminal-wrapper");
        page.waitForFunction("() => document.getElementById('terminal-container') && document.getElementById('terminal-container').innerText.includes('Terminal')");
        String terminalText = page.textContent("#terminal-container");
        assertTrue(terminalText.contains("Terminal") || terminalText.contains("WASI"),
                "Terminal container must contain terminal UI: " + terminalText);

        // 6. Bytecode & Relooper tab
        page.click("button:has-text('6. Bytecode & Relooper')");
        page.waitForSelector("#bytecode-app h3");
        assertEquals("Bytecode & Relooper Algorithmic Engine", page.textContent("#bytecode-app h3"));

        // 7. Log4j2 Logging tab
        page.click("button:has-text('7. Log4j2 Logging')");
        page.waitForSelector("#log4j-app h3");
        assertEquals("Log4j2 Enterprise Logging Engine", page.textContent("#log4j-app h3"));

        // Verify zero memory out of bounds or TextDecoder exceptions across all tabs
        assertTrue(consoleErrors.stream().noneMatch(e -> e.contains("out of bounds") || e.contains("ArrayBufferView") || e.contains("TextDecoder")),
                "Must not throw memory out of bounds or TextDecoder errors across all tabs: " + consoleErrors);

        page.close();
    }

    @Test
    public void testGameOfLifeButtonClickInBrowser() throws Exception {
        Page page = browser.newPage();
        java.util.List<String> consoleErrors = new java.util.ArrayList<>();
        page.onConsoleMessage(msg -> {
            System.out.println("[GAME TEST LOG] " + msg.type() + ": " + msg.text());
            if ("error".equals(msg.type())) consoleErrors.add(msg.text());
        });
        page.onPageError(consoleErrors::add);

        page.navigate("http://127.0.0.1:" + port + "/examples/index.html");
        page.click("button:has-text('2. Canvas Game of Life')");
        page.waitForSelector("#gen-info");
        assertEquals("Generation: 0", page.textContent("#gen-info"));

        page.click("button:has-text('Next Step')");
        page.waitForFunction("() => document.getElementById('gen-info') && document.getElementById('gen-info').innerText === 'Generation: 1'");
        assertEquals("Generation: 1", page.textContent("#gen-info"));

        page.click("button:has-text('Next Step')");
        page.waitForFunction("() => document.getElementById('gen-info') && document.getElementById('gen-info').innerText === 'Generation: 2'");
        assertEquals("Generation: 2", page.textContent("#gen-info"));

        page.click("button:has-text('Clear')");
        page.waitForFunction("() => document.getElementById('gen-info') && document.getElementById('gen-info').innerText === 'Generation: 0'");
        assertEquals("Generation: 0", page.textContent("#gen-info"));

        assertTrue(consoleErrors.isEmpty(), "No console errors allowed: " + consoleErrors);
        page.close();
    }

    @Test
    public void testSimdParticlesButtonClickInBrowser() throws Exception {
        Page page = browser.newPage();
        java.util.List<String> consoleErrors = new java.util.ArrayList<>();
        page.onConsoleMessage(msg -> {
            System.out.println("[SIMD TEST LOG] " + msg.type() + ": " + msg.text());
            if ("error".equals(msg.type())) consoleErrors.add(msg.text());
        });
        page.onPageError(consoleErrors::add);

        page.navigate("http://127.0.0.1:" + port + "/examples/index.html");
        page.click("button:has-text('3. 128-Bit SIMD Physics')");
        page.waitForSelector("#simd-status");
        assertEquals("Processed 4 particles simultaneously via v128 SIMD", page.textContent("#simd-status"));

        page.click("button:has-text('SIMD Update Tick')");
        page.waitForFunction("() => document.getElementById('simd-status') && document.getElementById('simd-status').innerText.includes('Processed 4 particles simultaneously')");

        assertTrue(consoleErrors.isEmpty(), "No console errors allowed: " + consoleErrors);
        page.close();
    }

    @Test
    public void testWasiShellDemoStandaloneInBrowser() throws Exception {
        Page page = browser.newPage();
        java.util.List<String> consoleErrors = new java.util.ArrayList<>();
        page.onConsoleMessage(msg -> {
            if ("error".equals(msg.type())) consoleErrors.add(msg.text());
        });
        page.onPageError(consoleErrors::add);

        page.navigate("http://127.0.0.1:" + port + "/examples/terminal/index.html");

        page.waitForSelector("#terminal-container .javaw-terminal-wrapper");
        page.waitForFunction("() => document.getElementById('terminal-container') && document.getElementById('terminal-container').innerText.includes('Terminal')");
        String terminalText = page.textContent("#terminal-container");
        assertTrue(terminalText.contains("Terminal") || terminalText.contains("WASI"),
                "Standalone terminal container must contain terminal UI: " + terminalText);

        assertTrue(consoleErrors.stream().noneMatch(e -> e.contains("out of bounds") || e.contains("RuntimeError")),
                "WasiShellDemo must not throw memory out of bounds: " + consoleErrors);

        page.close();
    }

    private static class FileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            Path filePath = Paths.get(path).toAbsolutePath().normalize();

            if (path.endsWith("javaw-runtime.js")) {
                Path r1 = Paths.get("src/main/resources/javaw-runtime.js").toAbsolutePath().normalize();
                Path r2 = Paths.get("../javaw-core/src/main/resources/javaw-runtime.js").toAbsolutePath().normalize();
                if (Files.exists(r1)) {
                    filePath = r1;
                } else if (Files.exists(r2)) {
                    filePath = r2;
                }
            } else if (!Files.exists(filePath)) {
                Path parentCheck = Paths.get("..", path).toAbsolutePath().normalize();
                if (Files.exists(parentCheck)) {
                    filePath = parentCheck;
                }
            }

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
            } else if (path.endsWith(".json")) {
                contentType = "application/json; charset=UTF-8";
            }

            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Cross-Origin-Opener-Policy", "same-origin");
            exchange.getResponseHeaders().set("Cross-Origin-Embedder-Policy", "require-corp");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}
