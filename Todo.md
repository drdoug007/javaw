# Java to WebAssembly (`javaw`) Improvement Roadmap

This roadmap organizes planned features by their utility and return on investment, prioritizing high-impact language constructs and web runtime capabilities.

---

## 1. Core Control Flow & Language Constructs (Short-Term / High Utility)

- [x] **Standard `for` loop lowering (`ForStmt`)**
  - Translate Java `for (init; condition; update) { ... }` into WebAssembly `block` / `loop` / `br_if` structures with proper iteration semantics.
  - Implemented support for `break`, `continue`, and compound assignments (`+=`, `-=`, `*=`, `/=`, etc.).
- [x] **Enhanced `for-each` loop lowering (`ForEachStmt`)**
  - Support iteration over primitive arrays (`int[]`, `double[]`, etc.) and iterable structures.
  - Linear memory array layout `[length: i32, element_0, element_1, ...]`.
  - Array instantiation (`new int[n]`, `new int[]{...}`), array indexing (`arr[i]`), element updates (`arr[i] = val`, `arr[i] += val`), and length property (`arr.length`).
  - Loop control with `break` and `continue` support.
- [x] **`switch` statements & expressions (`SwitchStmt`, `SwitchExpr`)**
  - Lower switch statements to nested WebAssembly blocks with conditional branching dispatch and fallthrough support.
  - Support `break` (switch exit), `yield` (switch expression value return), and enclosing loop `continue` forwarding.
- [x] **Enhanced `break` and `continue` with labels**
  - Map labeled break/continue statements to outer WebAssembly block/loop depths.
  - Support labeled `for`, `while`, `for-each` loops, `switch` statements, and standalone labeled code blocks.
- [x] **WebAssembly Exception Handling (`try-catch-finally`, `throw`)**
  - Lower Java `try-catch-finally` blocks and `throw` statements directly to standard WebAssembly exception handling (`(tag $Exception ...)`, `(try ... (catch $Exception ...) (catch_all ...))`, `throw`, `rethrow`).
  - Pre-registered built-in exception hierarchy (`Throwable`, `Exception`, `RuntimeException`, `IllegalArgumentException`, `IllegalStateException`, `NullPointerException`) and custom exception class support.
  - Multi-catch type dispatch with dynamic inheritance subtype checking and fallback rethrowing.
  - Zero-cost exception propagation without JavaScript bridge overhead.
- [x] **Multi-Value Returns (`(result ...)`) & Record Support**
  - Support multi-value returns on the WebAssembly evaluation stack for Java records, tuples, and multi-component returns without heap allocation overhead.
  - Full AST lowering for `RecordDeclaration`, component accessor methods (`p.x()`, `p.first()`), instance/static record methods, and parameter passing.
  - Multi-value stack packing for record instantiations (`new Point(x, y)`), returns (`return new Point(x, y)`), and multi-value local variable unpacking (`Point p = makePoint(...)`).

---

## 2. Array Support & Linear Memory Layout

- [x] **Primitive Array Allocation & Access (`int[]`, `double[]`, `boolean[]`)**
  - Linear memory layout: `[length: i32, element_0, element_1, ...]`.
  - Array instantiation (`new int[n]`, `new int[]{...}`), element loading (`i32.load`, `f64.load`), element storing (`i32.store`, `f64.store`).
- [x] **Array Length Property (`arr.length`)**
  - Read array length directly from array header in linear memory.
- [x] **Bulk Memory Operations (`memory.copy`, `memory.fill`)**
  - Lower `System.arraycopy(...)` and `Arrays.fill(...)` directly to WebAssembly `memory.copy` and `memory.fill` instructions for high-throughput block data transfers.
  - Implemented strongly-typed array utilities facade (`com.example.javaw.util.Arrays`) with `fill`, `copyOf`, `copyOfRange`, and `equals`.
  - Accelerated dynamic linear memory allocation (`$__heap_alloc`), dynamic string concatenation (`$__str_concat`), substring extraction (`$__str_substring`), and `StringBuilder` buffer expansions with block memory copies and fills.
- [x] **Multi-Memory Segregation & Isolation**
  - Leverage WebAssembly Multi-Memory proposal to segregate memory into discrete spaces: read-only static string pool (`$strings`), dynamic heap (`$heap`), and thread stack (`$stack`).
  - Implemented strongly-typed WebAssembly memory facade (`com.example.javaw.memory.Memory`) supporting `Memory.HEAP`, `Memory.STRINGS`, `Memory.STACK`, `Memory.DEFAULT`, `Memory.of(name)`.
  - Full AST lowering for memory operations: typed loads and stores (`loadI32`, `storeI32`, `loadI64`, `storeI64`, `loadF32`, `storeF32`, `loadF64`, `storeF64`, `loadI8`, `storeI8`, `loadI16`, `storeI16`), memory growth (`grow`), memory sizing (`size`), block filling (`fill`), and cross-memory bulk copying (`copy`, `copyFrom`).
  - Updated assembler and optimizer flags (`wat2wasm --enable-multi-memory` and `wasm-opt --enable-multimemory`).
  - Extended browser/Node JavaScript runtime bridge (`javaw-runtime.js`) with multi-memory accessors (`getMemory`, `getHeapMemory`, `getStringsMemory`, `getStackMemory`) and multiple memory string decoding.

---

## 3. Dynamic Strings & String Manipulation

- [x] **Dynamic String Concatenation (`+` operator with strings & primitives)**
  - Implement runtime string concatenation and heap buffer allocation.
- [x] **Dynamic `StringBuilder` Facade**
  - Support append operations and dynamic string building (`append(String)`, `append(int)`, `append(char)`, `append(boolean)`, `toString()`, `length()`, `charAt()`, `setCharAt()`).
- [x] **Core String Methods**
  - `length()`, `charAt(index)`, `substring(start, end)`, `equals(other)`, `indexOf(str)`, `concat(str)`, `isEmpty()`.

---

## 4. Object-Oriented Programming (OOP) & Memory Allocation

- [x] **Dynamic Heap Allocation (`new MyClass(...)`)**
  - Dynamic linear memory bump allocation (`call $__heap_alloc`) with 8-byte alignment.
  - Struct byte layout calculation, object headers storing vtable indices, and `$this` pointer passing to instance methods.
  - Full constructor lowering, field loading and storing (`this.field`, `obj.field`, `obj.field = val`, compound assignments `obj.field += val`).
- [x] **Virtual Method Dispatch (Polymorphism)**
  - Dynamic dispatch via WebAssembly function tables (`(table (export "__vtable") ... funcref)`) and `call_indirect`.
  - Type signatures `(type $sig_... (func ...))` and vtable base calculation supporting multi-level inheritance hierarchies.
  - `super(...)` constructor chaining, `super.method(...)` calls, and runtime `instanceof` (`InstanceOfExpr`) type checking.
- [x] **Object Arrays & References**
  - Linear memory reference arrays (`MyClass[]`) with indexing and iteration support.
- [ ] **WasmGC Target Proposal**
  - Emit native WebAssembly Garbage Collection types (`(type $Class (struct ...))`, `struct.new`, `struct.get`).
  - *Note*: Evaluated as an alternative standalone backend (`WasmGcVisitor`) rather than in-line linear memory branching to avoid visitor fragmentation, preserve universal linear memory compatibility, and isolate GC struct type hierarchies.

---

## 5. Extended Web & Browser Platform APIs

- [x] **DOM Facade Extensions**
  - `DOM.querySelector(selector)` / `DOM.querySelectorAll(selector)` for CSS querying and element array collections.
  - `element.remove()`, `element.removeChild(child)`, `element.getParent()` for DOM tree navigation and mutation.
  - `element.getValue()`, `element.setValue(val)`, `element.isChecked()`, `element.setChecked(boolean)` for forms and input controls.
  - `element.setStyle(name, val)`, `element.getStyle(name)`, `element.getAttribute(name)`, `element.getInnerText()`.
  - `element.addClass(name)`, `element.removeClass(name)`, `element.hasClass(name)`, `element.toggleClass(name)`, and `element.getClassList()` (`ClassList` facade).
- [x] **HTML5 Canvas 2D & WebGL Graphics Facades**
  - 2D context drawing primitives (`fillRect`, `strokeRect`, `clearRect`, `beginPath`, `closePath`, `moveTo`, `lineTo`, `arc`, `rect`, `ellipse`, `fill`, `stroke`, `fillText`, `strokeText`, `drawImage`, `save`, `restore`, `translate`, `rotate`, `scale`, `resetTransform`).
  - Context styling & properties (`fillStyle`, `strokeStyle`, `lineWidth`, `font`, `textAlign`, `globalAlpha`).
  - Canvas dimension controls (`getWidth()`, `setWidth(w)`, `getHeight()`, `setHeight(h)`) and context retrieval (`element.getContext2D()`, `DOM.getCanvasContext2D(id)`).
  - Basic WebGL context (`WebGLRenderingContext`) with `clearColor`, `clear`, `viewport`, `enable`, `disable`.
- [x] **Asynchronous Browser Interop (Promises & Timers)**
  - Timers: `Window.setTimeout(handler, ms)`, `Window.clearTimeout(id)`, `Window.setInterval(handler, ms)`, `Window.clearInterval(id)`.
  - Animation frame: `Window.requestAnimationFrame(handler)`, `Window.cancelAnimationFrame(id)`.
  - HTTP Fetch & Promises: `Window.fetch(url, callbackHandler)`.
  - Window dialogs: `Window.alert(msg)`, `Window.confirm(msg)`, `Window.prompt(msg)`.
- [x] **Generic JavaScript Dynamic Interop (`JSObject`)**
  - Dynamic evaluation and method calling against arbitrary JavaScript objects (`JSObject.eval`, `JSObject.global()`, `JSObject.createObject()`, `JSObject.createArray()`).
  - Property accessors/mutators (`get`, `getString`, `getInt`, `getDouble`, `getBoolean`, `set`).
  - Dynamic method invocations (`call`, `callString`, `callInt`, `callDouble`, `callBoolean`).
  - Dynamic conversions (`asString`, `asInt`, `asDouble`, `asBoolean`).
  - Array operations (`isArray`, `length`, `getIndex`, `setIndex`, `push`).

---

## 6. Non-Browser Standards (WASI & Concurrency) & High Performance

- [x] **WASI Target (`wasi_snapshot_preview1`)**
  - Standard I/O output: `System.out.println`, `System.out.print`, `System.err.println`, `System.err.print`, and `System.exit(code)` lowered to WASI `fd_write` and `proc_exit`.
  - Typed output support for strings, integers, floating point (`f64`), booleans, and characters with automated memory buffering.
  - Strongly-typed `WASI` facade (`com.example.javaw.wasi.WASI`) with `print`, `println`, `eprint`, `eprintln`, `exit`, `fdWrite`, and `procExit`.
  - Built-in WASI runtime layer in `javaw-runtime.js` supporting `fd_write`, `fd_read`, `proc_exit`, `args_sizes_get`, `args_get`, `environ_sizes_get`, `environ_get`, `clock_time_get`, and `random_get`.
- [x] **Extended WASI & Virtual File System (VFS)**
  - Expand `WASI` facade with file descriptor routines (`readString`, `writeString`, `appendString`, `fileExists`, `fileLength`, `deleteFile`, `createDirectory`, `open`, `close`, `seek`) and clock/random sources (`currentTimeMillis`, `randomInt`).
  - Object-oriented file abstraction (`com.example.javaw.wasi.WASIFile`) supporting `readString()`, `writeString(content)`, `appendString(content)`, `exists()`, `length()`, `delete()`, `mkdir()`.
  - High-performance in-memory and sandboxed virtual file system within `javaw-runtime.js` for browser and Node.js file operations (`path_open`, `fd_read`, `fd_write`, `fd_close`, `fd_seek`, `fd_filestat_get`, `path_filestat_get`, `path_create_directory`, `path_unlink_file`).
- [x] **WebAssembly Threads & Shared Memory**
  - WebAssembly shared memory declaration `(memory (export "memory") 2 20 shared)` for multithreaded runtime support.
  - Strongly-typed `AtomicInteger` facade (`com.example.javaw.concurrent.AtomicInteger`) supporting `get`, `set`, `getAndSet`, `compareAndSet`, `getAndAdd`, `addAndGet`, `incrementAndGet`, `decrementAndGet`, and bitwise atomic operations (`i32.atomic.load`, `i32.atomic.store`, `i32.atomic.rmw.*`).
  - Low-level array atomic primitives (`com.example.javaw.concurrent.Atomics`) supporting `load`, `store`, `add`, `sub`, `and`, `or`, `xor`, `exchange`, `compareExchange`, `wait` (`memory.atomic.wait32`), and `notify` (`memory.atomic.notify`).
  - Web Worker and thread management facade (`com.example.javaw.concurrent.WasmThread`) supporting `spawn(entryPoint, arg)`, `sleep(ms)`, `currentThreadId()`, and `hardwareConcurrency()`.
  - Host runtime bridge in `javaw-runtime.js` for thread spawning, sleep via atomics, and concurrency detection.
- [x] **Fixed-Width SIMD (`v128`) Vectorization Facades**
  - Implement strongly typed SIMD vector facades (`Float32x4`, `Int32x4`, `Vector128`) mapping to WebAssembly 128-bit vector instructions for hardware-accelerated math, physics, and graphics.
  - Native 128-bit `v128` WebAssembly value type mapping for parameters, local variables, and return values without heap allocation overhead.
  - Full arithmetic, bitwise, math, lane extraction/replacement, dot product, reductions (`sum()`), and vector array load/store operations (`v128.load`, `v128.store`).
  - Integrated `--enable-simd` in Binaryen `wasm-opt -O3` optimization pipeline.

---

## 7. Tooling & Optimization Pipeline

- [x] **Binaryen Optimization Pass (`wasm-opt`)**
  - Automated dead-code elimination, inlining, and binary size reduction via integrated `wasm-opt -O3 --enable-threads` pipeline.
- [x] **Source Maps & DWARF Debugging**
  - Generate standard Source Map v3 (`.wasm.map` / `.wat.map`) mapping WebAssembly instructions to original Java source files and line/column numbers.
  - Implemented `SourceMapGenerator` and `SourceMap` with Base64 VLQ encoding/decoding, sources content embedding, and AST range tracking across statements and expressions.
  - Added WebAssembly `sourceMappingURL` custom section embedding (`(@custom "sourceMappingURL" ...)` and binary section injection) for browser Chrome/Firefox DevTools debugging.
  - Added CLI flag `--source-map` and in-memory compilation result accessors (`CompilationResult.getSourceMap()`, `getSourceMapJson()`, `getSourceMapDataUri()`).
- [x] **Multi-File Compilation & Project Linking**
  - Compile multiple Java source files or entire directories into a unified WebAssembly module.
- [x] **In-Memory Java Compiler API & Embedded REPL**
  - Expose programmatic in-memory compilation (`JavacToWasm.compile(String javaSource)`) returning compiled WAT strings and byte arrays for embedded browser editors, dynamic code generation, and direct unit testing.

---

## 8. GraalVM Web Image-Inspired Interoperability & Application Tooling

- [x] **`@JS` Inline JavaScript Annotation & Code Generation**
  - Allow Java methods (declared `native`) to define their implementation using inline JavaScript snippets via `@JS("console.log(message);")` or `@JS("return a + b;")`.
  - Automatically extract snippet code during AST traversal and synthesize host bridge functions in `javaw-runtime.js` without manual glue code.
  - Custom section `javaw_js_snippets` metadata serialization in WebAssembly modules (`.wat` and `.wasm`) with automated runtime instantiation and binding.
- [x] **Automatic Type Coercion (`@JS.Coerce`)**
  - Automatically marshal primitive and reference types across the WASM-JS boundary (Java `int`, `double`, `boolean`, `String` &harr; JavaScript `number`, `boolean`, `string`) when annotated with `@JS.Coerce`.
  - Automatic conversion for array types (`int[]`, `double[]`, `String[]`) into native JavaScript `Array` instances for parameters and reconstruction from returned JS arrays.
  - Automatic JSON serialization when returning JavaScript objects to Java `String`.
- [x] **JavaScript Value Hierarchy Facades (`JSValue`, `JSNumber`, `JSBoolean`, `JSString`, `JSBigInt`, `JSSymbol`, `JSUndefined`, `JSError`, `JSNull`)**
  - Introduce explicit typed wrappers for JavaScript runtime types allowing fine-grained control over JS value manipulation, prototype access, and type checks (`typeof`, `instanceof`).
  - Implemented typed facades `JSValue`, `JSNumber`, `JSBoolean`, `JSString`, `JSBigInt`, `JSSymbol`, `JSUndefined`, `JSError`, `JSNull`, and updated `JSObject` to inherit from `JSValue`.
  - Added AST lowering and host runtime bridge handlers for type queries (`isNumber`, `isString`, `isBoolean`, `isBigInt`, `isSymbol`, `isUndefined`, `isNull`, `isObject`, `isFunction`, `isError`), `getTypeof()`, `instanceOf(className)`, `strictEquals(other)`, and typed extraction methods (`doubleValue`, `intValue`, `isNaN`, `isFinite`, `isInteger`, `getDescription`, `getMessage`, `getName`, `getStack`).
- [x] **Global JavaScript Class & Object Binding (`@JS.Import`)**
  - Annotate Java classes/interfaces with `@JS.Import("Date")`, `@JS.Import("Math")`, `@JS.Import("JSON")`, `@JS.Import("console")`, `@JS.Import("localStorage")` to map standard browser and JavaScript APIs directly to typed Java facades.
  - Implemented automatic snippet synthesizer for static and instance methods in `@JS.Import` classes with constructor instantiations (`new Date(...)`), direct method calls (`Math.hypot(...)`), and instance invocations (`this.getTime()`).
  - Added built-in standard facades `com.example.javaw.js.JSDate`, `JSMath`, `JSJSON`, `JSConsole`, `JSStorage`.
  - Full AST lowering and host runtime code synthesis in `javaw-runtime.js` supporting argument conversions, string decoding, handle registration, and native return marshalling.
- [x] **Exporting Java Classes to JavaScript Scope (`@JS.Export`)**
  - Annotate Java classes with `@JS.Export` (or `@JS.Export("CustomName")`) to register constructor functions and prototypes in JavaScript (`globalThis.MyJavaClass = ...`), allowing JavaScript to instantiate Java classes and call methods directly.
  - Implemented `JsExportModel` and metadata collection in `JsAnnotationHelper` serializing class layouts, constructor functions, and instance/static method signatures into custom section `javaw_js_exports`.
  - Added automatic JavaScript constructor function generation in `javaw-runtime.js` invoking `$__heap_alloc`, vtable initializations, and `$ClassName__init` constructors with transparent argument marshaling and return value unmarshaling.
  - Full support for prototype instance methods, static class methods, string parameters/returns, arrays, object handles, and field access across the WASM-JS runtime boundary.
- [x] **JavaScript Asset Bundling & Resource Embedding (`@JS.Code`, `@JS.Code.Include`, `@JS.Resource`, `JSResource`)**
  - Embed external JavaScript scripts and static web resources directly into the compiled WebAssembly bundle or runtime harness.
  - Implemented `@JS.Code("...")` for inline script blocks and `@JS.Code.Include({"file1.js", "file2.js"})` for bundling external JavaScript files during compilation.
  - Implemented `@JS.Resource(value = "...", path = "...")` for embedding static assets (CSS, JSON, HTML, text) into custom WebAssembly sections (`javaw_js_code` and `javaw_js_resources`).
  - Added strongly-typed Java facade `com.example.javaw.js.JSResource` supporting asset extraction (`getAsString`, `hasResource`, `getLength`, `listResources`), dynamic style/script injection (`injectStyle`, `injectStyleSheet`, `injectScript`, `injectScriptTag`), and automatic mounting into the WASI virtual file system (`mountAll`).
  - Extended `javaw-runtime.js` to automatically extract bundled scripts, execute them upon module instantiation, and register embedded assets in memory and VFS.
- [x] **Interactive In-Browser Terminal & REPL Integration (WASI & xterm.js)**
  - Provide an out-of-the-box browser terminal runner harness (powered by xterm.js or ANSI terminal emulation) connected to WASI standard I/O streams (`stdin`, `stdout`, `stderr`), enabling interactive Java CLI applications (such as shells, interpreters, and compilers) to run interactively in the browser.
  - Implemented strongly-typed Java facades `com.example.javaw.wasi.WASITerminal` and `com.example.javaw.terminal.Terminal` with full ANSI color palette constants (`BLACK`, `RED`, `GREEN`, `YELLOW`, `BLUE`, `MAGENTA`, `CYAN`, `WHITE`, `BRIGHT_*`), text attributes (`BOLD`, `DIM`, `ITALIC`, `UNDERLINE`, `REVERSE`), cursor positioning (`setCursor`), dimensions (`getColumns`, `getRows`), screen clearing (`clear`), and interactive line/char reading (`readLine`, `readChar`).
  - Implemented WebAssembly runtime routines (`$__wasi_read_char`, `$__wasi_read_line`) wired to WASI `fd_read` (`fd 0` stdin queue) with buffer memory management and dynamic string creation.
  - Built `JavawTerminal` in `javaw-runtime.js` supporting both xterm.js and a zero-dependency DOM terminal with full ANSI escape rendering, customizable prompt (`javaw> `), command history (Up/Down arrow navigation), Ctrl+L/Ctrl+C keyboard shortcuts, custom command dispatch (`help`, `clear`, `history`, `ls`, `js`, `run <wasmExport>`), and interactive REPL integration.

---

## 9. Maven Multi-Module Architecture & Build Tooling

- [x] **Multi-Module Project Architecture (`javaw-core`, `javaw-maven-plugin`, `examples`)**
  - Convert root project into an aggregator parent POM (`pom.xml` with `<packaging>pom</packaging>`) and centralized `<dependencyManagement>`.
  - Extract compiler engine, AST lowering visitors, runtime generator, source maps, and standard facades into `javaw-core`.
  - Isolate Maven plugin dependencies (`maven-plugin-api`, `maven-plugin-annotations`, `plexus-utils`) from the core compiler library.
- [x] **`javaw-maven-plugin` Mojo Implementation**
  - Implement `CompileMojo` bound to the standard lifecycle (`compile` / `process-classes` phase, goal: `javaw:compile`).
  - Expose configurable Mojo parameters:
    - `sourceDirectory`: Java source folder for WebAssembly compilation (defaults to `src/main/java` or `src/main/javaw`).
    - `outputDirectory`: Target directory for generated `.wat`, `.wasm`, `.wasm.map`, and runtime assets (defaults to `${project.build.directory}/generated-wasm`).
    - `optimizationLevel`: Binaryen `wasm-opt` optimization flag (`-O0`, `-O1`, `-O2`, `-O3`, `-Oz`).
    - `generateSourceMaps`: Boolean flag controlling Source Map v3 (`.wasm.map`) generation.
    - `enableThreads`: Boolean flag to enable shared memory and WebAssembly thread atomics.
    - `copyRuntimeScript`: Automatically copy `javaw-runtime.js` alongside compiled WASM artifacts.
  - Automate `.wat`, `.wasm`, and `.wasm.map` compilation without manual CLI invocation.
- [x] **Examples Child Module & Lifecycle Dogfooding**
  - Convert `examples/` directory into a child module (`examples/pom.xml`) that consumes `javaw-core` and executes `javaw-maven-plugin` during build lifecycle.
  - Validate all interactive demo applications (Todo, Canvas Game of Life, SIMD Particles, JS Interop, WASI Terminal, Records, Atomics) compile and pass tests automatically during standard `mvn clean package`.

---

## 10. `java.lang` Standard Library Support & Core Language Idioms

- [x] **Phase 1: Hardware Math & Primitive Wrapper Classes**
  - Complete `java.lang.Math` and `StrictMath` hardware operations mapped directly to WebAssembly FPU instructions (`Math.floor`, `Math.ceil`, `Math.trunc`, `Math.rint`, `Math.round`, `Math.abs`, `Math.sqrt`, `Math.min`, `Math.max`, `Math.copySign`, `Math.signum`, `Math.toDegrees`, `Math.toRadians`).
  - Bridge transcendental math functions and utilities (`sin`, `cos`, `tan`, `asin`, `acos`, `atan`, `atan2`, `exp`, `log`, `log10`, `log1p`, `pow`, `hypot`, `cbrt`, `sinh`, `cosh`, `tanh`, `Math.random()`, `Math.PI`, `Math.E`) via host runtime / standard math imports.
  - Primitive wrapper classes (`Integer`, `Long`, `Double`, `Float`, `Boolean`, `Character`, `Byte`, `Short`, `Number`) with constant fields (`MAX_VALUE`, `MIN_VALUE`, `BYTES`, `SIZE`, etc.).
  - Wrapper parsing, string conversion, and validation methods (`Integer.parseInt(s)`, `Integer.toString(i)`, `Integer.toHexString(i)`, `Integer.toBinaryString(i)`, `Integer.toOctalString(i)`, `Long.parseLong(s)`, `Long.toString(l)`, `Double.parseDouble(s)`, `Double.isNaN(d)`, `Double.isInfinite(d)`, `Double.isFinite(d)`, `Double.doubleToLongBits`, `Double.longBitsToDouble`, `Float.parseFloat(s)`, `Float.floatToIntBits`, `Float.intBitsToFloat`, `Boolean.parseBoolean(s)`, `Character.isDigit`, `Character.isLetter`, `Character.toUpperCase`, `Character.toLowerCase`, `Character.digit`, `Character.forDigit`).
  - Bitwise manipulation intrinsics mapped to WebAssembly instructions (`Integer.bitCount` -> `i32.popcnt`, `Integer.numberOfLeadingZeros` -> `i32.clz`, `Integer.numberOfTrailingZeros` -> `i32.ctz`, `Integer.reverseBytes`, `Integer.reverse`, `Integer.rotateLeft` -> `i32.rotl`, `Integer.rotateRight` -> `i32.rotr`, `Long.bitCount` -> `i64.popcnt`, `Long.numberOfLeadingZeros` -> `i64.clz`, `Long.numberOfTrailingZeros` -> `i64.ctz`, `Long.reverseBytes`, `Long.reverse`, `Long.rotateLeft` -> `i64.rotl`, `Long.rotateRight` -> `i64.rotr`).

- [x] **Phase 2: Root `Object` Virtual Contracts & Language `enum`**
  - Universal root `Object` virtual method table integration for all classes: slot 0 (`toString()`), slot 1 (`equals(Object)`), slot 2 (`hashCode()`).
  - Heap-address based default `hashCode()` and shallow memory cloning (`Object.clone()` lowered to `memory.copy`).
  - Java `enum` language construct lowering (`enum MyEnum { A, B, C }`) with static singleton class instances, `name()`, `ordinal()`, and `values()` array generation.
  - Support `switch` on enum instances lowered to branch tables (`br_table`) or conditional branching matching ordinal values.

- [x] **Phase 3: Expanded `String`, `CharSequence`, and Standard `StringBuilder`**
  - Promote `com.example.javaw.util.StringBuilder` to standard `java.lang.StringBuilder` in core package resolution with expanded methods (`insert`, `delete`, `deleteCharAt`, `reverse`, `replace`).
  - Implement `java.lang.CharSequence` interface.
  - Expand `String` standard methods (`trim()`, `strip()`, `toLowerCase()`, `toUpperCase()`, `replace(target, replacement)`, `contains(seq)`, `startsWith(prefix)`, `endsWith(suffix)`, `toCharArray()`, `getBytes()`, `compareTo(other)`, `equalsIgnoreCase(other)`, `repeat(count)`).

- [x] **Phase 4: Standard `System`, Clock & Environment**
  - Connect `System.currentTimeMillis()` and `System.nanoTime()` to WASI clock / browser performance timers.
  - Implement `System.identityHashCode(obj)`, `System.lineSeparator()`.
  - Bridge `System.getenv(key)` and `System.getProperty(key)` to WASI environment vectors or runtime JavaScript configs.

- [x] **Phase 5: Resource Management & Core Interfaces**
  - Implement `java.lang.AutoCloseable` interface.
  - Lower `try-with-resources` statements (`try (AutoCloseable res = ...) { ... }`) to WebAssembly `try-catch_all` blocks with guaranteed `.close()` cleanup.
  - Support standard interface dispatch for `java.lang.Comparable<T>` and `java.lang.Iterable<T>` in polymorphic call sites.

- [x] **Phase 6: Standard Thread/Runnable & Static Class Introspection**
  - Wrap `WasmThread` into standard `java.lang.Thread` and `java.lang.Runnable` constructs (`Thread.sleep(ms)`, `Thread.currentThread()`).
  - Static linear memory `java.lang.Class` descriptor tables supporting `obj.getClass().getName()`, `Class.getSimpleName()`, and `Class.isInstance(obj)`.
