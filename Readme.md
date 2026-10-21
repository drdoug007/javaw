# javaw (Java to WebAssembly Compiler)

A lightweight, high-performance Java-to-WebAssembly compiler that parses Java source files (`.java`) and compiles them directly into standard WebAssembly Text format (`.wat`) and optimized binary modules (`.wasm`). `javaw` delivers an efficient, standalone runtime pipeline supporting browser Document Object Model (DOM) manipulation, HTML5 Canvas 2D / WebGL graphics, WebAssembly Threads and shared memory atomics, WASI standard I/O, asynchronous timers/fetch, generic JavaScript dynamic interop, standard WebAssembly Exception Handling (`try-catch-finally`, `throw`), and complete object-oriented programming.

---

## Overview

`javaw` provides an efficient pipeline to compile Java code into WebAssembly for native browser execution, Node.js runtimes, and WASI host environments. Using [JavaParser](https://javaparser.org/) for AST traversal, the compiler analyzes Java classes, methods, expressions, and statements, lowering them directly into WebAssembly stack instructions without requiring intermediate JVM bytecode compilation or heavyweight runtime interpreters.

With built-in linear memory allocation, static string constant pooling, dynamic heap management, multithreading shared memory, and a zero-dependency host runtime bridge (`javaw-runtime.js`), developers can author interactive web applications, high-performance computational workloads, and graphical user interfaces natively in Java.

---

## Features

- **`@JS` Inline JavaScript Annotations & Host Code Generation**:
  - Allows Java methods (declared `native`) to define inline JavaScript logic directly using `@JS("return Math.hypot(a, b);")`, `@JS("console.log(message);")`, or `@JS("return 'Hello ' + name;")`.
  - Automatic type marshalling and coercion via `@JS.Coerce` for Java primitives, strings, and arrays (`int[]`, `double[]`, `String[]` &harr; JavaScript `Array`) and automatic JSON serialization for JavaScript objects.
  - Automatically extracts snippet code during AST analysis, emits corresponding host import signatures (`(import "dom" "js_inline_..." ...)`), and serializes metadata into WebAssembly custom sections (`(@custom "javaw_js_snippets" ...)` and binary `.wasm` sections).
  - The runtime bridge (`javaw-runtime.js`) automatically deserializes inline snippets and synthesizes callable host functions with zero-copy string decoding and argument conversions.
- **JavaScript Value Hierarchy Facades (`JSValue`, `JSNumber`, `JSBoolean`, `JSString`, `JSBigInt`, `JSSymbol`, `JSUndefined`, `JSError`, `JSNull`)**:
  - Provides a complete, strongly-typed object-oriented hierarchy representing JavaScript runtime values with zero overhead.
  - Base class `JSValue` exposes type reflection methods (`getTypeof()`, `isNumber()`, `isString()`, `isBoolean()`, `isBigInt()`, `isSymbol()`, `isUndefined()`, `isNull()`, `isObject()`, `isFunction()`, `isError()`), prototype testing (`instanceOf(className)`), and strict identity checks (`strictEquals(other)`).
  - Explicit typed wrappers `JSNumber` (`doubleValue()`, `intValue()`, `isNaN()`, `isFinite()`, `isInteger()`), `JSBoolean` (`booleanValue()`), `JSString` (`stringValue()`), `JSBigInt` (`longValue()`), `JSSymbol` (`getDescription()`), `JSError` (`getMessage()`, `getName()`, `getStack()`, with `TypeError`, `RangeError`, `SyntaxError` factories), `JSUndefined`, and `JSNull`.
  - `JSObject` extends `JSValue`, unifying arbitrary JavaScript object and array manipulation into a consistent type hierarchy.
- **AST-Based Lowering & Modular Compiler Architecture**:
  - Directly parses Java source files using JavaParser and translates statements and expressions to WebAssembly instructions.
  - Modular architecture separated into dedicated components: `JavacToWasm`, `WasmGeneratorVisitor`, `WasmRuntime`, `DomHelper`, `WasiHelper`, `TypeMapper`, `ClassRegistry`, `ClassModel`, `FieldModel`, `MethodModel`, `GlobalVar`, `StringConstant`, and `LoopContext`.
- **Multi-File Java Compilation & Project Linking**:
  - Compiles multiple Java source files or entire project directories into a unified, linked WebAssembly module.
  - Automatically merges AST compilation units, resolves cross-file type references, and aggregates class hierarchies.
- **Source Maps & Browser DevTools Debugging (Source Map v3)**:
  - Generates standard Source Map v3 files (`.wasm.map` and `.wat.map`) mapping WebAssembly instructions directly back to original Java source lines and columns.
  - Full Base64 VLQ encoding and decoding, embedded source contents, and AST location tracking across classes, constructors, methods, fields, and statements.
  - Automatically injects `sourceMappingURL` custom sections into generated WAT (`(@custom "sourceMappingURL" ...)`) and binary `.wasm` files for step-through debugging inside Chrome and Firefox DevTools.
  - CLI flag `--source-map` and in-memory compilation result accessors (`CompilationResult.getSourceMap()`, `getSourceMapJson()`, `getSourceMapDataUri()`).
- **WebAssembly Multi-Value Returns & Java Record Support**:
  - Leverages WebAssembly multi-value return capability (`(result i32 i32 ...)`) to return multiple values on the evaluation stack without heap allocation overhead.
  - Full AST lowering for Java `record` declarations (`RecordDeclaration`), component accessor methods (`p.x()`, `p.first()`), instance/static record methods, and constructor/parameter passing.
  - Multi-value stack packing for record instantiations (`new Point(x, y)`), returns (`return new Point(...)`), and multi-value local variable unpacking (`Point p = makePoint(...)`).
- **Bulk Memory Operations & Fast Array Utilities (`memory.copy`, `memory.fill`)**:
  - Direct translation of `System.arraycopy(...)` and `Arrays.fill(...)` to WebAssembly `memory.copy` and `memory.fill` instructions for high-throughput block data movement and initialization.
  - Strongly-typed array utility facade (`com.example.javaw.util.Arrays`) supporting `fill`, `copyOf`, `copyOfRange`, and `equals` for both `int[]` and `double[]`.
  - Accelerated dynamic heap zeroing (`$__heap_alloc`), dynamic string concatenation (`$__str_concat`), substring slicing (`$__str_substring`), and `StringBuilder` buffer expansion using zero-overhead memory copies.
- **Binaryen Optimization Pipeline (`wasm-opt`)**:
  - Integrates automated `wasm-opt -O3 --enable-multimemory --enable-simd --enable-threads --enable-exception-handling --enable-multivalue --enable-bulk-memory` optimization passes directly into the build pipeline for dead-code elimination, inlining, and binary size reduction.
  - Supports `--no-opt` / `-O0` flags to bypass optimization passes during development.
- **WebAssembly Multi-Memory Segregation & Isolation (`Memory`)**:
  - Leverages the WebAssembly Multi-Memory proposal to segregate memory into discrete spaces: read-only static string pool (`$strings`), dynamic heap allocation space (`$heap`), and execution thread stack (`$stack`).
  - Strongly-typed `Memory` facade (`com.example.javaw.memory.Memory`) supporting `Memory.HEAP`, `Memory.STRINGS`, `Memory.STACK`, `Memory.DEFAULT`, and `Memory.of(name)`.
  - Full AST lowering for memory instructions: typed loads and stores (`loadI32`, `storeI32`, `loadI64`, `storeI64`, `loadF32`, `storeF32`, `loadF64`, `storeF64`, `loadI8`, `storeI8`, `loadI16`, `storeI16`), memory sizing (`size()`), memory growth (`grow(pages)`), block filling (`fill`), and bulk copying across discrete memories (`copy`, `copyFrom`).
  - Automatically configures toolchain flags (`wat2wasm --enable-multi-memory` and `wasm-opt --enable-multimemory`).
  - JavaScript host runtime support in `javaw-runtime.js` (`getMemory`, `getHeapMemory`, `getStringsMemory`, `getStackMemory`) for multi-memory buffer access and UTF-8 string decoding across memories.
- **Fixed-Width SIMD (`v128`) Vectorization Facades**:
  - Strongly typed 128-bit vector facades (`com.example.javaw.simd.Float32x4`, `Int32x4`, `Vector128`) mapping directly to native WebAssembly `v128` vector value types without heap allocation overhead.
  - Full support for vector math (`add`, `sub`, `mul`, `div`, `min`, `max`, `sqrt`, `abs`, `neg`), bitwise operations (`and`, `or`, `xor`, `not`, `shl`, `shr`), and scalar reductions (`dot`, `sum`).
  - Lane extraction and replacement (`getX`..`getW`, `withX`..`withW`, `extractLane`, `replaceLane`), vector splatting (`Float32x4.splat`, `Int32x4.splat`), and bulk vector memory operations (`load`, `store`).
  - Native lowering to WebAssembly instructions (`f32x4.add`, `f32x4.mul`, `f32x4.sqrt`, `i32x4.add`, `i32x4.shl`, `v128.and`, `v128.load`, `v128.store`).
- **WebAssembly Exception Handling (`try-catch-finally`, `throw`)**:
  - Direct translation of Java `try`, `catch`, `finally`, and `throw` statements to standard WebAssembly exception handling instructions (`(tag $Exception ...)`, `(try ... (catch $Exception ...) (catch_all ...))`, `throw $Exception`, `rethrow`).
  - Pre-registered built-in exception hierarchy (`Throwable`, `Exception`, `RuntimeException`, `IllegalArgumentException`, `IllegalStateException`, `NullPointerException`) providing standard constructor overloads and `.getMessage()`.
  - Custom user-defined exception classes with inheritance and custom fields/methods.
  - Polymorphic multi-catch blocks with dynamic subtype checking against the class registry vtable and fallback automatic rethrowing.
  - Guaranteed `finally` block execution for normal returns, caught exceptions, and rethrown errors.
- **WebAssembly Threads, Shared Memory & Atomic Operations**:
  - Automatically emits WebAssembly shared memory declarations `(memory (export "memory") 2 20 shared)` and enables thread extensions in `wat2wasm`.
  - **`AtomicInteger`** (`com.example.javaw.concurrent.AtomicInteger`): High-level atomic integer operations (`get`, `set`, `getAndSet`, `compareAndSet`, `getAndAdd`, `addAndGet`, `incrementAndGet`, `decrementAndGet`, and bitwise atomics `getAndAnd`, `getAndOr`, `getAndXor`) lowered to `i32.atomic.load`, `i32.atomic.store`, and `i32.atomic.rmw.*`.
  - **`Atomics`** (`com.example.javaw.concurrent.Atomics`): Low-level array atomic primitives (`load`, `store`, `add`, `sub`, `and`, `or`, `xor`, `exchange`, `compareExchange`, `wait` via `memory.atomic.wait32`, and `notify` via `memory.atomic.notify`).
  - **`WasmThread`** (`com.example.javaw.concurrent.WasmThread`): Web Worker thread management, entry point dispatching (`spawn`), thread sleep (`sleep`), thread ID discovery (`currentThreadId`), and CPU core detection (`hardwareConcurrency`).
- **WASI Target Support & Virtual File System (VFS)**:
  - Standard Java I/O stream lowering: `System.out.println`, `System.out.print`, `System.err.println`, `System.err.print`, and `System.exit(code)` lowered to WASI `fd_write` and `proc_exit`.
  - Automated linear memory string buffering and typed output conversion for strings, integers, floats, doubles, booleans, and characters.
  - Strongly typed `WASI` facade (`com.example.javaw.wasi.WASI`) with `print`, `println`, `eprint`, `eprintln`, `exit`, `fdWrite`, `procExit`, `readString`, `writeString`, `appendString`, `fileExists`, `fileLength`, `deleteFile`, `createDirectory`, `open`, `close`, `seek`, `currentTimeMillis`, and `randomInt`.
  - Object-oriented file abstraction (`com.example.javaw.wasi.WASIFile`) supporting `readString()`, `writeString(content)`, `appendString(content)`, `exists()`, `length()`, `delete()`, and `mkdir()`.
  - Built-in Virtual File System (VFS) in `javaw-runtime.js` supporting file descriptor tables, in-memory mounting (`runtime.mount`), virtual sandboxing, file queries, clocks, and random sources across browser and Node.js environments.
- **In-Memory Java Compiler API & Embedded REPL**:
  - Programmatic in-memory compilation via `JavacToWasm.compile(String javaSource)` returning compiled WAT strings and executable WASM byte arrays (`byte[]`) without requiring disk I/O.
  - Enables embedding Java compilation directly inside web IDEs, REPLs, and automated test runners.
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
│   │   │   ├── RecordModel.java       # Record components & multi-value signature model
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
│   │   │   │   ├── JSObject.java      # Generic dynamic JavaScript interop facade
│   │   │   │   ├── JSValue.java       # Base JavaScript value representation
│   │   │   │   ├── JSNumber.java      # JavaScript Number wrapper
│   │   │   │   ├── JSBoolean.java     # JavaScript Boolean wrapper
│   │   │   │   ├── JSString.java      # JavaScript String wrapper
│   │   │   │   ├── JSBigInt.java      # JavaScript BigInt wrapper
│   │   │   │   ├── JSSymbol.java      # JavaScript Symbol wrapper
│   │   │   │   ├── JSUndefined.java   # JavaScript Undefined wrapper
│   │   │   │   ├── JSError.java       # JavaScript Error hierarchy wrapper
│   │   │   │   └── JSNull.java        # JavaScript Null wrapper
│   │   │   ├── js
│   │   │   │   ├── JS.java                # @JS annotation and sub-annotation definitions
│   │   │   │   ├── JsSnippetModel.java    # Model for extracted @JS inline JavaScript snippets
│   │   │   │   └── JsAnnotationHelper.java # AST visitor and annotation discovery helper
│   │   │   ├── memory
│   │   │   │   └── Memory.java        # WebAssembly Multi-Memory segregation & operations facade
│   │   │   ├── simd
│   │   │   │   ├── Float32x4.java     # 128-bit float vector facade (f32x4)
│   │   │   │   ├── Int32x4.java       # 128-bit integer vector facade (i32x4)
│   │   │   │   └── Vector128.java     # 128-bit SIMD constants and utilities
│   │   │   ├── sourcemap
│   │   │   │   ├── Base64VLQ.java     # Base64 Variable-Length Quantity (VLQ) encoder/decoder
│   │   │   │   ├── MappingEntry.java  # Generated to original source coordinate mapping model
│   │   │   │   ├── SourceMap.java     # Source Map v3 parser and consumer
│   │   │   │   ├── SourceMapGenerator.java # Source Map v3 generator and custom section injector
│   │   │   │   └── SourceMapTracker.java   # AST source position tracking visitor helper
│   │   │   ├── util
│   │   │   │   ├── Arrays.java        # High-performance array utilities & bulk memory operations
│   │   │   │   └── StringBuilder.java # Dynamic string builder facade
│   │   │   └── wasi
│   │   │       ├── WASI.java          # WASI snapshot preview 1 system & VFS facade
│   │   │       └── WASIFile.java      # Object-oriented WASI Virtual File System abstraction
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

#### Generating Source Maps
To generate standard Source Map v3 files (`.wat.map` and `.wasm.map`) with embedded source locations for DevTools step-through debugging:

```bash
java -jar target/javaw-1.0-SNAPSHOT.jar --source-map src/test/java/com/example/javaw/App.java
```

This generates:
- `<filename>.wat`: WebAssembly Text representation.
- `<filename>.wat.map`: Source Map v3 for the text format.
- `<filename>.wasm`: Compiled and optimized binary WebAssembly with embedded `sourceMappingURL` custom section.
- `<filename>.wasm.map`: Source Map v3 for the binary module.

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

#### Example I: WebAssembly Exception Handling (`try-catch-finally`, `throw`, `multi-catch`)

```java
package com.example.javaw;

class ValidationException extends Exception {
    private int errorCode;

    public ValidationException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return this.errorCode;
    }
}

public class ExceptionDemo {
    public static int processInput(int value) {
        int status = 0;
        try {
            if (value < 0) {
                throw new ValidationException("Negative input not allowed", 400);
            } else if (value == 0) {
                throw new IllegalArgumentException("Zero input invalid");
            }
            status = 100 / value;
        } catch (ValidationException ve) {
            // Polymorphic multi-catch matching custom exception
            System.out.println(ve.getMessage());
            status = ve.getErrorCode();
        } catch (IllegalArgumentException iae) {
            // Multi-catch matching standard runtime exception
            System.out.println(iae.getMessage());
            status = -1;
        } catch (Exception e) {
            // Fallback catch
            status = -999;
        } finally {
            // Guaranteed cleanup logic
            System.out.println("Processing attempt completed");
        }
        return status;
    }
}
```

---

#### Example J: WebAssembly Multi-Value Returns & Java Records

```java
package com.example.javaw;

// Record definitions compiled to WebAssembly multi-value signatures
record Point(int x, int y) {}

record DivisionResult(int quotient, int remainder) {
    public int total() {
        return quotient() + remainder();
    }
}

public class RecordDemo {
    // Returns (result i32 i32) on the WebAssembly evaluation stack
    public static Point createPoint(int x, int y) {
        return new Point(x, y);
    }

    public static DivisionResult divide(int dividend, int divisor) {
        return new DivisionResult(dividend / divisor, dividend % divisor);
    }

    public static int process() {
        // Multi-value stack unpacking into local variables
        Point pt = createPoint(15, 25);
        int sum = pt.x() + pt.y();

        DivisionResult div = divide(50, 6);
        return sum + div.total(); // 40 + (8 + 2) = 50
    }
}
```

---

#### Example K: Bulk Memory Operations & Fast Array Utilities (`System.arraycopy`, `Arrays.*`)

```java
package com.example.javaw;

import java.util.Arrays;

public class BulkMemoryDemo {
    public static int processArrays() {
        int[] src = new int[]{10, 20, 30, 40, 50};
        int[] dest = new int[5];

        // Direct lowering to WebAssembly memory.copy
        System.arraycopy(src, 1, dest, 2, 3); // dest: [0, 0, 20, 30, 40]

        // Fast block fills lowered to memory.fill
        int[] buffer = new int[10];
        Arrays.fill(buffer, 7);         // [7, 7, 7, ...]
        Arrays.fill(buffer, 2, 6, 99);   // Sub-range fill

        // Zero-allocation array cloning & sub-arrays
        int[] copy = Arrays.copyOf(src, 8); // [10, 20, 30, 40, 50, 0, 0, 0]
        int[] slice = Arrays.copyOfRange(src, 1, 4); // [20, 30, 40]

        boolean identical = Arrays.equals(slice, new int[]{20, 30, 40});
        return identical ? dest[2] + copy[0] : 0; // 20 + 10 = 30
    }
}
```

---

#### Example L: Extended WASI & Virtual File System (VFS)

```java
package com.example.javaw;

import com.example.javaw.wasi.WASI;
import com.example.javaw.wasi.WASIFile;

public class VfsDemo {
    public static String handleFiles() {
        // Direct static WASI file system manipulation
        WASI.createDirectory("/app");
        WASI.writeString("/app/config.json", "{\"mode\":\"production\",\"port\":8080}");
        
        // Append additional content
        WASI.appendString("/app/log.txt", "Server started at timestamp: " + WASI.currentTimeMillis());

        // Object-oriented file operations via WASIFile
        WASIFile configFile = new WASIFile("/app/config.json");
        if (configFile.exists()) {
            return configFile.readString();
        }
        return "Not found";
    }
}
```

---

#### Example M: Programmatic In-Memory Compilation API

```java
import com.example.javaw.JavacToWasm;

public class CompilerHost {
    public static void main(String[] args) {
        String javaSource = "package com.example;\n" +
                "public class MathLib {\n" +
                "    public int square(int x) { return x * x; }\n" +
                "}";

        // In-memory compilation without file I/O
        JavacToWasm.CompilationResult result = JavacToWasm.compile(javaSource);
        
        System.out.println("Generated WAT:\n" + result.getWat());
        System.out.println("WASM Byte Count: " + result.getWasm().length);
    }
}
```

---

#### Example N: Fixed-Width SIMD Vectorization (128-bit)

```java
package com.example.javaw;

import com.example.javaw.simd.Float32x4;
import com.example.javaw.simd.Int32x4;

public class SimdDemo {
    public static float calculateVectorMath() {
        // 128-bit floating-point vector arithmetic (f32x4)
        Float32x4 a = Float32x4.of(1.0f, 2.0f, 3.0f, 4.0f);
        Float32x4 b = Float32x4.of(5.0f, 6.0f, 7.0f, 8.0f);
        
        // Native WebAssembly f32x4.add and f32x4.mul instructions
        Float32x4 sum = a.add(b); // (6.0, 8.0, 10.0, 12.0)
        Float32x4 scaled = sum.mul(Float32x4.splat(2.0f));
        
        // Dot product and vector reduction
        float dotProduct = a.dot(b); // 1*5 + 2*6 + 3*7 + 4*8 = 70
        return scaled.sum() + dotProduct;
    }

    public static int processIntArray(int[] data) {
        // Fast 128-bit bulk load from array into vector register (v128.load)
        Int32x4 vec = Int32x4.load(data, 0);
        
        // Bitwise operations and vector shifts (i32x4.shl)
        Int32x4 shifted = vec.shiftLeft(2);
        
        // Store back to array in a single 128-bit operation (v128.store)
        shifted.store(data, 0);
        return shifted.sum();
    }
}
```

---

#### Example O: WebAssembly Multi-Memory Segregation & Isolation (`Memory`)

```java
package com.example.javaw;

import com.example.javaw.memory.Memory;

public class MultiMemoryDemo {
    public static int processIsolatedMemories(int baseValue) {
        // Direct access to segregated linear memories
        Memory heap = Memory.HEAP;
        Memory strings = Memory.STRINGS;
        
        // Memory sizing and growth
        int initialPages = heap.size();
        heap.grow(1); // Grow heap memory dynamically
        
        // Store and load values in isolated heap space
        heap.storeI32(0, baseValue);
        heap.storeI32(4, baseValue * 2);
        
        // Bulk copy bytes across discrete memories (strings -> heap)
        // Lowered to: memory.copy $heap $strings
        Memory.copy(heap, 100, strings, 0, 16);
        
        int firstCopiedByte = heap.loadU8(100);
        return heap.loadI32(0) + heap.loadI32(4) + firstCopiedByte;
    }
}
```

---

#### Example P: Source Maps & Browser DevTools Debugging

Generate Source Map v3 mappings programmatically or via CLI for step-through debugging inside browser Developer Tools:

```java
package com.example.javaw;

import com.example.javaw.sourcemap.SourceMap;
import com.example.javaw.sourcemap.MappingEntry;

public class SourceMapDemo {
    public static void main(String[] args) {
        String javaSource = """
            package com.example.app;
            public class App {
                public static int calculate(int x) {
                    int factor = 5;
                    return x * factor + 10;
                }
            }
            """;

        // In-memory compilation with Source Map generation enabled
        JavacToWasm.CompilationResult result = JavacToWasm.compile(javaSource, true, true);
        
        // Retrieve generated WAT, optimized WASM binary, and Source Map JSON
        String wat = result.getWat();
        byte[] wasm = result.getWasm();
        String sourceMapJson = result.getSourceMapJson();
        String dataUri = result.getSourceMapDataUri();

        // Inspect and parse source map entries
        SourceMap map = SourceMap.fromJson(sourceMapJson);
        System.out.println("Sources: " + map.getSources());
        
        // Find Java source position corresponding to a generated WebAssembly line
        MappingEntry entry = map.findMappingForGenerated(4, 0);
        if (entry != null) {
            System.out.println("Mapped to Java line: " + (entry.getOriginalLine() + 1));
        }
    }
}
```

---

#### Example Q: @JS Inline JavaScript Annotations & Host Bridge

Use `@JS` and `@JS.Coerce` annotations to execute inline JavaScript expressions directly from Java `native` methods across the WebAssembly-to-JavaScript boundary:

```java
package com.example.javaw;

import com.example.javaw.js.JS;

public class InlineJsDemo {
    // Mathematical and browser operations written in inline JavaScript
    @JS("return Math.hypot(a, b);")
    public static native double calculateHypotenuse(double a, double b);

    @JS("console.log('[JavaW]: ' + message);")
    public static native void logToConsole(String message);

    @JS("return 'Hello ' + name + '!';")
    public static native String formatGreeting(String name);

    @JS("return window.innerWidth > 768;")
    public static native boolean isDesktopViewport();

    // Automatic type coercion for arrays and JSON data
    @JS("return arr.reduce((acc, x) => acc + x, 0);")
    public static native int sumArray(@JS.Coerce int[] arr);

    @JS("return arr.map(x => x * 2);")
    @JS.Coerce
    public static native int[] doubleArray(@JS.Coerce int[] arr);

    @JS("return { title: 'WASM in Action', year: 2026 };")
    @JS.Coerce
    public static native String getBookMetadataJson();

    public static void run() {
        logToConsole("Initializing application...");
        double dist = calculateHypotenuse(3.0, 4.0); // Returns 5.0
        String greeting = formatGreeting("WebAssembly Developer");
        
        if (isDesktopViewport()) {
            logToConsole(greeting + " (Hypotenuse: " + dist + ")");
        }

        int[] nums = new int[]{10, 20, 30};
        int total = sumArray(nums); // Returns 60
        int[] doubled = doubleArray(nums); // Returns [20, 40, 60]
        String json = getBookMetadataJson(); // Returns JSON string
    }
}
```

---

#### Example R: JavaScript Value Hierarchy & Dynamic Typing

Manipulate arbitrary JavaScript values with strong type safety, explicit runtime reflections, and error inspection:

```java
package com.example.javaw;

import com.example.javaw.dom.JSBigInt;
import com.example.javaw.dom.JSBoolean;
import com.example.javaw.dom.JSError;
import com.example.javaw.dom.JSNull;
import com.example.javaw.dom.JSNumber;
import com.example.javaw.dom.JSObject;
import com.example.javaw.dom.JSString;
import com.example.javaw.dom.JSSymbol;
import com.example.javaw.dom.JSUndefined;
import com.example.javaw.dom.JSValue;

public class JsValueHierarchyDemo {
    public static void run() {
        // Create typed JavaScript values
        JSNumber num = JSNumber.of(42.5);
        JSString str = JSString.of("Hello WebAssembly");
        JSBoolean bool = JSBoolean.of(true);
        JSBigInt bigInt = JSBigInt.of(9007199254740991L);
        JSSymbol sym = JSSymbol.forSymbol("shared-key");
        JSUndefined undef = JSUndefined.getInstance();
        JSNull nullVal = JSNull.getInstance();
        JSError err = JSError.typeError("Invalid configuration detected");

        // Dynamic type reflection
        JSValue val = num;
        if (val.isNumber()) {
            double d = val.asNumber().doubleValue();
            boolean isInt = val.asNumber().isInteger();
        }

        // Prototype and strict equality checks
        JSObject global = JSObject.global();
        JSValue doc = global.get("document");
        if (doc.instanceOf("HTMLDocument")) {
            System.out.println("Running in browser environment");
        }

        if (err.isError()) {
            String errorName = err.getName();       // "TypeError"
            String message = err.getMessage();       // "Invalid configuration detected"
        }
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

Run the Node.js test suite to verify binary execution, string decoding, threads/atomics, WebAssembly exception handling, dynamic JavaScript interop, WASI streams, and interactive DOM mutations:

```bash
npm test --prefix src/test/node-tests
```
