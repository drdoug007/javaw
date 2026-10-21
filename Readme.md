# javaw (Java to WebAssembly Compiler)

A lightweight, high-performance Java-to-WebAssembly compiler that parses Java source files (`.java`) and compiles them directly into standard WebAssembly Text format (`.wat`) and optimized binary modules (`.wasm`). `javaw` delivers an efficient, standalone runtime pipeline supporting browser Document Object Model (DOM) manipulation, HTML5 Canvas 2D / WebGL graphics, WebAssembly Threads and shared memory atomics, WASI standard I/O, asynchronous timers/fetch, generic JavaScript dynamic interop, and complete object-oriented programming.

---

## Overview

`javaw` provides an efficient pipeline to compile Java code into WebAssembly for native browser execution, Node.js runtimes, and WASI host environments. Using [JavaParser](https://javaparser.org/) for AST traversal, the compiler analyzes Java classes, methods, expressions, and statements, lowering them directly into WebAssembly stack instructions without requiring intermediate JVM bytecode compilation or heavyweight runtime interpreters.

With built-in linear memory allocation, static string constant pooling, dynamic heap management, multithreading shared memory, and a zero-dependency host runtime bridge (`javaw-runtime.js`), developers can author interactive web applications, high-performance computational workloads, and graphical user interfaces natively in Java.

---

## Features

- **AST-Based Lowering & Modular Compiler Architecture**:
  - Directly parses Java source files using JavaParser and translates statements and expressions to WebAssembly instructions.
  - Modular architecture separated into dedicated components: `JavacToWasm`, `WasmGeneratorVisitor`, `WasmRuntime`, `DomHelper`, `WasiHelper`, `TypeMapper`, `ClassRegistry`, `ClassModel`, `FieldModel`, `MethodModel`, `GlobalVar`, `StringConstant`, and `LoopContext`.
- **Multi-File Java Compilation & Project Linking**:
  - Compiles multiple Java source files or entire project directories into a unified, linked WebAssembly module.
  - Automatically merges AST compilation units, resolves cross-file type references, and aggregates class hierarchies.
- **Binaryen Optimization Pipeline (`wasm-opt`)**:
  - Integrates automated `wasm-opt -O3 --enable-threads` optimization passes directly into the build pipeline for dead-code elimination, inlining, and binary size reduction.
  - Supports `--no-opt` / `-O0` flags to bypass optimization passes during development.
- **WebAssembly Threads, Shared Memory & Atomic Operations**:
  - Automatically emits WebAssembly shared memory declarations `(memory (export "memory") 2 20 shared)` and enables thread extensions in `wat2wasm`.
  - **`AtomicInteger`** (`com.example.javaw.concurrent.AtomicInteger`): High-level atomic integer operations (`get`, `set`, `getAndSet`, `compareAndSet`, `getAndAdd`, `addAndGet`, `incrementAndGet`, `decrementAndGet`, and bitwise atomics `getAndAnd`, `getAndOr`, `getAndXor`) lowered to `i32.atomic.load`, `i32.atomic.store`, and `i32.atomic.rmw.*`.
  - **`Atomics`** (`com.example.javaw.concurrent.Atomics`): Low-level array atomic primitives (`load`, `store`, `add`, `sub`, `and`, `or`, `xor`, `exchange`, `compareExchange`, `wait` via `memory.atomic.wait32`, and `notify` via `memory.atomic.notify`).
  - **`WasmThread`** (`com.example.javaw.concurrent.WasmThread`): Web Worker thread management, entry point dispatching (`spawn`), thread sleep (`sleep`), thread ID discovery (`currentThreadId`), and CPU core detection (`hardwareConcurrency`).
- **WASI Target Support (`wasi_snapshot_preview1`)**:
  - Standard Java I/O stream lowering: `System.out.println`, `System.out.print`, `System.err.println`, `System.err.print`, and `System.exit(code)` lowered to WASI `fd_write` and `proc_exit`.
  - Automated linear memory string buffering and typed output conversion for strings, integers, floats, doubles, booleans, and characters.
  - Strongly typed `WASI` facade (`com.example.javaw.wasi.WASI`) with `print`, `println`, `eprint`, `eprintln`, `exit`, `fdWrite`, and `procExit`.
  - Built-in WASI runtime layer in `javaw-runtime.js` supporting standard file descriptors, clock timing, random byte generation, and process argument/environment queries.
- **Generic JavaScript Dynamic Interop (`JSObject`)**:
  - `com.example.javaw.dom.JSObject`: Dynamic evaluation and interaction with arbitrary browser JavaScript objects without pre-declared static imports.
  - Global scope access (`JSObject.global()`, `JSObject.getWindow()`) and dynamic expression execution (`JSObject.eval(code)`).
  - Object & array instantiation (`createObject()`, `createArray()`).
  - Typed property accessors & mutators (`get`, `getString`, `getInt`, `getDouble`, `getBoolean`, `set`).
  - Dynamic method invocations (`call`, `callString`, `callInt`, `callDouble`, `callBoolean`).
  - Type conversions (`asString`, `asInt`, `asDouble`, `asBoolean`) and array indexing operations (`isArray`, `length`, `getIndex`, `setIndex`, `push`).
- **Asynchronous Browser Interop & Window APIs (`Window`)**:
  - `com.example.javaw.dom.Window`: Browser window facade for timers and asynchronous browser events.
  - Timers: `Window.setTimeout(handler, ms)`, `Window.clearTimeout(id)`, `Window.setInterval(handler, ms)`, `Window.clearInterval(id)`.
  - Animation loop scheduling: `Window.requestAnimationFrame(handler)`, `Window.cancelAnimationFrame(id)`.
  - Asynchronous HTTP Fetch: `Window.fetch(url, callbackHandler)` with automated response string decoding.
  - Browser dialogs: `Window.alert(msg)`, `Window.confirm(msg)`, `Window.prompt(msg)`.
- **Object-Oriented Programming (OOP) & Dynamic Memory Allocation**:
  - Dynamic linear memory bump allocation (`$__heap_alloc`) with 8-byte alignment and zero-initialization for objects and data structures.
  - Struct byte layout modeling, instance fields (`this.field`, `obj.field = val`, compound updates), and automatic `$this` parameter passing.
  - Full constructor lowering (`$ClassName__init`), constructor overloading, and `super(...)` constructor chaining.
  - Polymorphism and dynamic method dispatch via WebAssembly function tables (`(table (export "__vtable") ... funcref)`) and `call_indirect`.
  - Multi-level class inheritance, `super.method(...)` calls, and runtime `instanceof` type checking.
  - Object reference arrays (`MyClass[]`) with dynamic indexing and iteration support.
- **WebAssembly Linear Memory & Dynamic Heap**:
  - Automatically exports linear memory (`(memory (export "memory") 2)` or shared memory for threads).
  - Pools UTF-8 encoded String literals at compile-time into static WebAssembly data segments `(data (i32.const offset) "bytes\00")`.
  - Dynamic linear memory bump allocation (`global $__heap_ptr`) for runtime arrays and string manipulation.
  - Passes strings across the host boundary via efficient pointer and length pairs (`(ptr: i32, len: i32)`).
- **Primitive Array Support**:
  - Dynamic memory layout: `[length: i32, element_0, element_1, ...]`.
  - Dynamic array instantiation (`new int[n]`, `new int[]{...}`).
  - Element indexing and access (`arr[i]`), element updates (`arr[i] = val`, `arr[i] += val`), and array length property (`arr.length`).
- **Dynamic Strings & StringBuilder**:
  - Runtime string concatenation via `+` and `+=` operators (supporting strings, integers, characters, and booleans).
  - Built-in string methods: `length()`, `charAt(index)`, `substring(start, end)`, `equals(other)`, `indexOf(str)`, `concat(str)`, and `isEmpty()`.
  - Dynamic `StringBuilder` (`com.example.javaw.util.StringBuilder`) with chained `append(...)`, `setCharAt(...)`, and `toString()`.
- **Comprehensive Control Flow**:
  - `if-then-else` conditional branching.
  - `while` loops, standard `for` loops, and enhanced `for-each` loops over arrays.
  - `switch` statements and `switch` expressions (with `yield`, case matching, and fallthrough).
  - Labeled `break` and `continue` statements for targeted outer loop/block exits.
- **Rich DOM & UI Manipulation Facades**:
  - `com.example.javaw.dom.DOM`: Static methods such as `getElementById`, `createElement`, `getBody`, `querySelector`, `querySelectorAll`, and `getCanvasContext2D`.
  - `com.example.javaw.dom.Element`: DOM tree mutations (`appendChild`, `removeChild`, `remove`, `getParent`), attribute & style controls (`setAttribute`, `getAttribute`, `setStyle`, `getStyle`), text & form values (`setInnerText`, `getInnerText`, `getValue`, `setValue`, `isChecked`, `setChecked`), and event handling (`addEventListener`).
  - `com.example.javaw.dom.ClassList`: Dynamic CSS class token list management (`addClass`, `removeClass`, `hasClass`, `toggleClass`, `getClassList`).
- **HTML5 Canvas 2D & WebGL Graphics**:
  - `com.example.javaw.dom.CanvasRenderingContext2D`: Full 2D drawing pipeline including path creation (`beginPath`, `closePath`, `moveTo`, `lineTo`, `arc`, `rect`, `ellipse`), shape drawing (`fillRect`, `strokeRect`, `clearRect`, `fill`, `stroke`), text rendering (`fillText`, `strokeText`), image/canvas blitting (`drawImage`), styling (`fillStyle`, `strokeStyle`, `lineWidth`, `font`, `textAlign`, `globalAlpha`), and matrix transforms (`save`, `restore`, `translate`, `rotate`, `scale`, `resetTransform`).
  - `com.example.javaw.dom.WebGLRenderingContext`: Hardware-accelerated 3D graphics context (`clearColor`, `clear`, `viewport`, `enable`, `disable`, and buffer mask / capability constants).
  - Canvas dimension controls on `Element` (`getWidth`, `setWidth`, `getHeight`, `setHeight`) and direct context accessors (`element.getContext2D()`, `element.getWebGLContext()`).
- **Automatic Host Import Generation**: Automatically detects DOM, Canvas 2D, WebGL, Window, Atomics, and WASI method invocations and emits corresponding `(import "dom" ...)` and `(import "wasi_snapshot_preview1" ...)` declarations.
- **Lightweight Browser Runtime Bridge (`javaw-runtime.js`)**:
  - Minimal footprint (< 15KB) with zero external dependencies.
  - 32-bit integer handle lookup table (`Map<number, HTMLElement>`) for safe referencing of DOM elements from WebAssembly.
  - Zero-copy UTF-8 text decoding from WebAssembly linear memory (`instance.exports.memory.buffer`).
  - Bi-directional event dispatching mapping browser DOM events to exported WebAssembly handler functions.
  - Built-in Web Worker spawning, timer queues, and WASI standard streams.
- **Arithmetic & Primitive Types**:
  - Primitive and return type mapping (`void`, `int`, `boolean`, `char`, `long`, `float`, `double`).
  - Arithmetic and bitwise binary operators (`+`, `-`, `*`, `/`, `%`, `&`, `|`, `^`, `<<`, `>>`, `>>>`).
  - Unary operators (negation, logical/bitwise complement, prefix/postfix increments and decrements).
  - Local variables and static global field variables.
- **Binary WASM Assembly & Optimization**: Automatically invokes `wat2wasm` and `wasm-opt` (when present on `PATH`) to produce deployable, optimized `.wasm` binaries.
- **Standalone Fat JAR**: Packaged via Maven Shade plugin for frictionless CLI use.

---

## Project Structure

```
.
├── demo
│   └── index.html                     # Browser demo harness running compiled Java WASM
├── pom.xml                            # Maven project configuration and shade packaging
├── Readme.md                          # Project documentation
├── Todo.md                            # Feature roadmap and implementation progress
├── src
│   ├── main
│   │   ├── java/com/example/javaw
│   │   │   ├── JavacToWasm.java       # Compiler CLI coordinator and entry point
│   │   │   ├── WasmGeneratorVisitor.java # AST visitor lowering Java statements & expressions
│   │   │   ├── WasmRuntime.java       # WebAssembly linear memory runtime routines (heap, strings, arrays)
│   │   │   ├── DomHelper.java         # DOM API import collector and emitter
│   │   │   ├── WasiHelper.java        # WASI snapshot preview 1 import collector and emitter
│   │   │   ├── TypeMapper.java        # Java to WebAssembly type mapping
│   │   │   ├── ClassRegistry.java     # Class layout, vtable calculation & dispatch manager
│   │   │   ├── ClassModel.java        # Class metadata model
│   │   │   ├── FieldModel.java        # Field layout & byte offset model
│   │   │   ├── MethodModel.java       # Method signature & vtable slot model
│   │   │   ├── StringConstant.java    # String literal pool model
│   │   │   ├── GlobalVar.java         # Global field variable model
│   │   │   ├── LoopContext.java       # Loop label and jump context tracker
│   │   │   ├── concurrent
│   │   │   │   ├── AtomicInteger.java # WebAssembly atomic integer facade
│   │   │   │   ├── Atomics.java       # Low-level array atomics and wait/notify facade
│   │   │   │   └── WasmThread.java    # Web Worker thread management facade
│   │   │   ├── dom
│   │   │   │   ├── DOM.java           # Java DOM API facade
│   │   │   │   ├── Element.java       # Java DOM Element facade
│   │   │   │   ├── ClassList.java     # Java DOM ClassList facade
│   │   │   │   ├── CanvasRenderingContext2D.java # HTML5 Canvas 2D graphics facade
│   │   │   │   ├── WebGLRenderingContext.java    # HTML5 WebGL graphics facade
│   │   │   │   ├── Window.java        # Browser Window, timers & async fetch facade
│   │   │   │   └── JSObject.java      # Generic dynamic JavaScript interop facade
│   │   │   ├── util
│   │   │   │   └── StringBuilder.java # Dynamic string builder facade
│   │   │   └── wasi
│   │   │       └── WASI.java          # WASI snapshot preview 1 system facade
│   │   └── resources
│   │       └── javaw-runtime.js       # JavaScript WebAssembly host runtime bridge
│   └── test
│       ├── java/com/example/javaw
│       │   ├── App.java               # Java DOM & interactive event demo application
│       │   ├── AppBrowserTest.java    # Playwright headless browser integration test suite
│       │   ├── JavacToWasmTest.java   # JUnit 5 compiler unit test suite
│       │   └── TestClass.java         # Comprehensive Java test suite (loops, arrays, OOP, WASI, threads)
│       └── node-tests
│           ├── package.json           # Node.js test configuration
│           └── test.js                # End-to-end WebAssembly execution & DOM test suite
```

---

## Prerequisites

- **Java Development Kit (JDK)**: Version 17 or higher
- **Apache Maven**: Version 3.6 or higher
- **WABT (`wat2wasm`)** (optional): For compiling `.wat` text to `.wasm` binary modules directly from the CLI
- **Binaryen (`wasm-opt`)** (optional): For optimizing `.wasm` binary size and execution speed with `-O3`
- **Node.js** (optional): For executing the automated end-to-end test suite

---

## Building the Compiler

Build the standalone executable JAR using Maven:

```bash
mvn clean package
```

The resulting shaded executable JAR is located at:
```
target/javaw-1.0-SNAPSHOT.jar
```

---

## Usage

### 1. Compiling Java to WebAssembly

#### Single File Compilation
Run the compiler against any Java source file:

```bash
java -jar target/javaw-1.0-SNAPSHOT.jar <path-to-java-file.java>
```

#### Multi-File and Directory Compilation
Compile multiple Java source files or an entire directory into a unified WebAssembly module:

```bash
# Multiple explicit files
java -jar target/javaw-1.0-SNAPSHOT.jar ServiceA.java ServiceB.java App.java

# Entire source directory
java -jar target/javaw-1.0-SNAPSHOT.jar src/main/java/com/example/app
```

#### Disabling Optimization
To skip the `wasm-opt` optimization pass during development:

```bash
java -jar target/javaw-1.0-SNAPSHOT.jar --no-opt src/test/java/com/example/javaw/App.java
```

This generates:
- `<filename>.wat`: WebAssembly Text representation.
- `<filename>.wasm`: Compiled and optimized binary WebAssembly (if `wat2wasm` and `wasm-opt` are available on `PATH`).

---

### 2. Examples

#### Example A: Pure Computation (`TestClass.java`)

```java
package com.example.javaw;

public class TestClass {
    public int addNumbers(int a, int b) {
        return a + b;
    }

    public double getPi() {
        return 3.14159;
    }
}
```

Compile the file:
```bash
java -jar target/javaw-1.0-SNAPSHOT.jar src/test/java/com/example/javaw/TestClass.java
```

---

#### Example B: Dynamic Strings, Arrays & `StringBuilder`

```java
package com.example.javaw;

import com.example.javaw.util.StringBuilder;

public class StringArrayDemo {
    public String buildMessage(int[] values) {
        StringBuilder sb = new StringBuilder("Processed items: ");
        int sum = 0;
        for (int v : values) {
            sum += v;
            sb.append(v).append(" ");
        }
        sb.append("| Total: ").append(sum);
        return sb.toString();
    }
}
```

---

#### Example C: Object-Oriented Programming & Polymorphism

```java
package com.example.javaw;

abstract class Shape {
    protected int x;
    protected int y;

    public Shape(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public abstract int area();
}

class Rectangle extends Shape {
    private int width;
    private int height;

    public Rectangle(int x, int y, int width, int height) {
        super(x, y);
        this.width = width;
        this.height = height;
    }

    @Override
    public int area() {
        return this.width * this.height;
    }
}

public class GeometryApp {
    public int calculateArea(Shape shape) {
        return shape.area(); // Dynamic dispatch via vtable call_indirect
    }

    public boolean isRectangle(Shape shape) {
        return shape instanceof Rectangle; // Runtime instanceof checking
    }
}
```

---

#### Example D: Interactive DOM & Browser Event Handling (`App.java`)

```java
package com.example.javaw;

import com.example.javaw.dom.DOM;
import com.example.javaw.dom.Element;

public class App {
    private static int counter = 0;

    public static void start() {
        Element app = DOM.getElementById("app");

        Element title = DOM.createElement("h1");
        title.setInnerText("Interactive Java WebAssembly");
        app.appendChild(title);

        Element countDisplay = DOM.createElement("p");
        countDisplay.setAttribute("id", "counter-text");
        countDisplay.setInnerText("Current Count: 0");
        app.appendChild(countDisplay);

        Element btn = DOM.createElement("button");
        btn.setInnerText("Increment Count");
        btn.addEventListener("click", "handleIncrement");
        app.appendChild(btn);
    }

    public static void handleIncrement() {
        counter++;
        Element countDisplay = DOM.getElementById("counter-text");
        countDisplay.setInnerText("Current Count: " + counter);
    }
}
```

---

#### Example E: HTML5 Canvas 2D Graphics

```java
package com.example.javaw;

import com.example.javaw.dom.DOM;
import com.example.javaw.dom.Element;
import com.example.javaw.dom.CanvasRenderingContext2D;

public class CanvasDemo {
    public void render() {
        Element canvas = DOM.getElementById("main-canvas");
        canvas.setWidth(400);
        canvas.setHeight(300);

        CanvasRenderingContext2D ctx = canvas.getContext2D();

        // Background
        ctx.setFillStyle("#1e1e2e");
        ctx.fillRect(0, 0, 400, 300);

        // Draw circle
        ctx.beginPath();
        ctx.arc(200, 150, 60, 0, 6.283185307179586);
        ctx.setFillStyle("#f38ba8");
        ctx.fill();

        // Draw text
        ctx.setFont("bold 20px sans-serif");
        ctx.setFillStyle("#cdd6f4");
        ctx.setTextAlign("center");
        ctx.fillText("Rendered with Java WASM", 200, 155);
    }
}
```

---

#### Example F: WebAssembly Threads & Atomic Operations

```java
package com.example.javaw;

import com.example.javaw.concurrent.AtomicInteger;
import com.example.javaw.concurrent.Atomics;
import com.example.javaw.concurrent.WasmThread;

public class ThreadDemo {
    public static void runWorker() {
        // High-level atomic counter operations
        AtomicInteger counter = new AtomicInteger(0);
        int nextVal = counter.incrementAndGet(); // 1
        boolean updated = counter.compareAndSet(1, 100); // true (atomic CAS)

        // Low-level array atomics
        int[] sharedArr = new int[]{10, 20, 30};
        Atomics.add(sharedArr, 0, 5); // atomically adds 5 to sharedArr[0]

        // Thread and worker management
        int cpus = WasmThread.hardwareConcurrency();
        WasmThread.spawn("workerTask", 42);
        WasmThread.sleep(100);
    }

    public static void workerTask(int payload) {
        // Background thread routine
    }
}
```

---

#### Example G: WASI Target & Standard I/O

```java
package com.example.javaw;

import com.example.javaw.wasi.WASI;

public class WasiApp {
    public static void main(String[] args) {
        // Standard Java I/O lowered to WASI fd_write
        System.out.println("Executing on WASI Runtime!");
        System.out.print("Computation result: ");
        System.out.println(42 * 10);

        // Strongly typed WASI facade
        WASI.eprintln("Warning: Standard error stream output");
        WASI.exit(0);
    }
}
```

---

#### Example H: Generic Dynamic JavaScript Interop & Async APIs

```java
package com.example.javaw;

import com.example.javaw.dom.JSObject;
import com.example.javaw.dom.Window;

public class DynamicJsDemo {
    public void setup() {
        // Dynamic JavaScript evaluation and global interaction
        JSObject global = JSObject.global();
        JSObject console = global.get("console");
        console.call("log", "Dynamic logging from Java!");

        // Dynamic JSON parsing
        JSObject json = JSObject.eval("JSON.parse('{\"status\":\"ok\",\"code\":200}')");
        int code = json.getInt("code");

        // Asynchronous Timers & Fetch
        Window.setTimeout("onTimerFired", 1000);
        Window.fetch("https://api.example.com/data", "onFetchComplete");
    }

    public static void onTimerFired() {
        Window.alert("Timer completed!");
    }

    public static void onFetchComplete(String responseText) {
        System.out.println("Received: " + responseText);
    }
}
```

---

### 3. Running in the Browser

Include `javaw-runtime.js` and instantiate the WebAssembly module:

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Java WASM App</title>
</head>
<body>
    <div id="app"></div>

    <script src="src/main/resources/javaw-runtime.js"></script>
    <script>
        async function run() {
            const response = await fetch('src/test/java/com/example/javaw/App.wasm');
            const wasmBytes = await response.arrayBuffer();
            const { exports } = await Javaw.instantiateJavaw(wasmBytes);
            
            // Execute the Java entry point
            if (exports.start) {
                exports.start();
            }
        }
        window.addEventListener('DOMContentLoaded', run);
    </script>
</body>
</html>
```

---

## Testing

### Java Unit & Playwright Browser Tests (JUnit 5 + Playwright)

Run the full Java test suite, including compiler AST unit tests and headless Chromium browser tests:

```bash
mvn test
```

The browser test suite (`AppBrowserTest.java`) launches a headless browser using Microsoft Playwright, loads the compiled Java WebAssembly application, and verifies live DOM rendering, styling, and event dispatching.

### End-to-End Node.js & DOM Integration Tests

Run the Node.js test suite to verify binary execution, string decoding, threads/atomics, dynamic JavaScript interop, WASI streams, and interactive DOM mutations:

```bash
npm test --prefix src/test/node-tests
```
