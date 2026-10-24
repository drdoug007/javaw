# javaw (Java to WebAssembly Compiler)

A lightweight, high-performance Java-to-WebAssembly compiler that parses Java source files (`.java`) and compiles them directly into standard WebAssembly Text format (`.wat`) and optimized binary modules (`.wasm`). `javaw` delivers an efficient, standalone runtime pipeline supporting browser Document Object Model (DOM) manipulation, HTML5 Canvas 2D / WebGL graphics, WebAssembly Threads and shared memory atomics, WASI standard I/O, asynchronous timers/fetch, generic JavaScript dynamic interop, standard WebAssembly Exception Handling (`try-catch-finally`, `throw`), and selected object-oriented features.

---

## Overview

`javaw` provides an efficient pipeline to compile Java code into WebAssembly for native browser execution, Node.js runtimes, and WASI host environments. Using [JavaParser](https://javaparser.org/) for AST traversal, the compiler analyzes Java classes, methods, expressions, and statements, lowering them directly into WebAssembly stack instructions without requiring intermediate JVM bytecode compilation or heavyweight runtime interpreters.

With built-in linear memory allocation, static string constant pooling, dynamic heap management, multithreading shared memory, and a zero-dependency host runtime bridge (`javaw-runtime.js`), developers can author interactive web applications, high-performance computational workloads, and graphical user interfaces natively in Java.

---

## Java compatibility and current limits

`javaw` implements a Java subset with selected JDK facades and ahead-of-time API translations. Parsing Java 17 syntax, compiling a sample, or caching a dependency does not establish full language, JDK, JVM, or third-party library compatibility. A checked roadmap item describes the implemented subset.

| Area | Current limit |
| :--- | :--- |
| Collections | Source-backed `ArrayList`, bucketed/resizing `HashMap`, `HashSet`, `List`/`Set`/`Map`/`Collection`/`Iterable`/`Iterator` interfaces, enhanced for, live map key/value/entry views, backed nested sublists, bulk mutations, and iterator removal. Complete JDK collection coverage remains pending. |
| Streams | Lazy sequential reference pipelines and common terminals execute in Wasm, including short-circuiting and infinite sources. Stable natural/custom sorting and prefix operations are supported; core IntStream/LongStream/DoubleStream APIs are available; standard Collector contracts and core factories are supported; parallel execution and additional JDK factory families remain pending. Spring repository lists retain their reference-array adapter. |
| Lambdas | Reusable closure objects capture effectively-final locals and lexical `this`; functional interfaces and static/bound/unbound/constructor method references use Wasm dispatch. Inherited abstract contracts and default methods are supported, including Function/Predicate/Consumer composition. Explicitly typed callback locals resolve ambiguous targets; complete JLS target typing remains pending. |
| Optional | Source-backed `empty`, `of`, `ofNullable`, presence checks, `get`, eager/lazy defaults, exception suppliers, `map`, `flatMap`, `filter`, and `ifPresent` execute in Wasm. Functional callbacks use interface dispatch; empty/null factories may require explicit type witnesses (`Optional.<String>empty()`). |
| Decimal arithmetic | Source-backed arbitrary-precision BigInteger/BigDecimal arithmetic, scale, all rounding modes, exact conversions, and exact decimal JSON. Dynamic double factories and MathContext remain pending. |
| Memory management | Conservative tracing at explicit host safepoints reclaims objects and cycles; counted host retains preserve escaped values. See [the memory-management design](docs/memory-management.md). |
| Types and methods | Local `var` inference covers supported literals, binary/conditional expressions, arrays, aliases, and inherited/static method returns. Block/loop/catch scopes preserve distinct locals. Qualified module types are resolved when simple names are unique. Source overload selection, nullable boxing, and reference generic inference are supported for the tested subset. Complete JLS conversion and inference conformance remains pending. |
| Reflection | Linked source constructors, fields, methods, invocation, runtime annotations, and Class.forName lookup execute in Wasm. Array arguments and field writes check runtime component types; Enum constants, record members/components, and repeatable/parameter annotation queries are supported; library metadata and full JVM reflection remain pending. See [the reflection contract](docs/reflection.md). |
| Concurrency | Wasm threads and atomics cover a subset; monitors, wait/notify, and complete Java Memory Model semantics remain pending. |
| JDK APIs | I/O, NIO, networking, date/time, regex, and serialization lack comprehensive compatibility. WASI/JavaScript facades provide selected operations. |
| Dependencies | The bytecode backend admits a typed numeric/reference subset with closed-world declaration/linkage checks, executable JVM/Wasm comparisons and compiler-revision cache invalidation. Actual ASM/JUnit JARs are tested rejections. See [the opcode matrix and compatibility suite](docs/compatibility.md); arbitrary Maven JAR compatibility remains pending. |
| Spring and persistence | Constructor injection, derived equality queries, and synchronous default REQUIRED transactions execute in the in-memory Spring subset. General Spring beans/proxies, advanced transaction policies, Hibernate, JDBC, and the actual H2 engine remain pending. |
| Diagnostics | Unsupported synchronized/volatile, non-static inner/local/anonymous classes, and native methods without JS bindings are rejected before lowering. Unresolved local inference, self-references, scope violations, and conflicting module type names receive source-location diagnostics. Other unsupported features can still surface as unresolved symbols or invalid WAT. |

Source-class constructor overloads use argument types and arity, including primitive/reference widening and `this(...)`/`super(...)` chains. Ambiguous or inapplicable constructor calls report source locations. Source-class method calls and inferred return types use the same selector, including inherited overloads and virtual dispatch. Overloaded source-class methods receive distinct Wasm symbols and exports. Custom record/enum overloads and source access/static-context checks use the same selection path. Nested virtual calls retain distinct receivers. Constructor and method regressions execute Wasm in `OverloadResolutionTest`.

Nullable primitive wrappers use linear-memory objects with small-value caches, value equality/hashing, and null-unboxing exceptions. Assignments, arguments, returns, arrays, and fields apply boxing/unboxing; overload selection tries strict conversions before boxing/unboxing. Primitive/reference varargs are packed after fixed-arity invocation phases, including constructor calls and element boxing.

Source generic inference covers method/class variables, explicit and diamond arguments, bounds, inherited substitutions, simple wildcard reads/writes, generic varargs, and virtual overrides. Invalid bounds and parameterized assignments receive source diagnostics. Empty/null generic factories use declaration, assignment, return, and conditional target contexts; wildcard functional-interface lambda targets are grounded before closure generation. Full intersection/capture and nested or overload-sensitive target-typed poly-expression inference remain pending; inferred intersections require an explicit common type argument.

Source-resolution regressions execute compiled Wasm in Node; see `javaw-core/src/test/java/com/example/javaw/SourceResolutionTest.java`. Qualified references to source classes work across batch files; multiple classes with the same simple name in one module are explicitly rejected.

Generic, wrapper, and varargs regressions execute Wasm in `GenericInferenceTest`, `BoxingSupportTest`, and `VarargsTest`.

Primitive numeric regressions in `NumericConformanceTest` execute typed mutations, compound assignment promotion/narrowing, constant-expression ternaries, saturating float-to-integer casts, division edge cases, and floating remainder entirely in Wasm.

Collections accept null elements, keys, and values. Lists retain insertion order; hash collections promise no iteration order. String keys compare by UTF-8 content; boxed values and source objects use virtual `equals`/`hashCode` (keys must obey the Java equality/hash contract). Structural mutation invalidates iterator `next()`; exhausted iteration and invalid list indexes throw standard exceptions. Iterator removal checks mutation state and structural changes. Bulk add/remove/retain, predicate removal, indexed insertion, list replacement/sorting, backed nested sublists, live map values/entries, entry setValue, and map compute/merge defaults are supported. Structural changes through a nested sublist update its ancestors and invalidate unrelated views. Managed allocation headers distinguish source objects from untagged runtime strings. Objects, arrays, strings, and temporary buffers participate in explicit host-boundary conservative collection; unreachable cycles are reclaimed and cleared blocks are reused.

Sequential streams support `Stream.of/empty/generate/iterate`, collection `stream()`, `map/filter/flatMap/peek/distinct/skip/limit/sorted/takeWhile/dropWhile`, `count/findFirst/findAny`, matching, reductions, `min/max`, `forEach/forEachOrdered`, `toArray`, and unmodifiable `toList`. `collect(Collectors.toList/toSet)` returns mutable collections; explicit witnesses such as `Collectors.<Integer>toList()` may be required. Intermediate stages are lazy; terminals consume each pipeline once, and short-circuiting limits upstream callback execution. Collection sources bind their iterator on traversal. `parallel()` throws `UnsupportedOperationException`; Standard `Collector` contracts, `Collector.of`, joining/counting/summing/reducing, and downstream mapping/filtering/finishing are supported; additional factory families and close handlers remain pending. Primitive pipelines share boxed storage while preserving primitive Wasm arithmetic, exact long values, conversions, ranges, and optional numeric results; complete primitive statistics APIs and specialized storage remain follow-ups. For generic callback chains that need more target inference, assign callbacks to explicitly typed functional-interface locals.

Phase 1 source-resolution foundations and source-backed Optional/closures are implemented for the tested subset. Phase 2 now includes a tested collection and sequential-stream subset. Phase 3 adds arbitrary-precision decimal arithmetic and host-boundary memory reclamation (see the ownership contract below). See [the compatibility roadmap](Todo.md#13-java-compatibility-roadmap) for acceptance criteria and remaining work.

## Features

- **Bytecode-to-WebAssembly Compiler & Local Repository Dependency Caching (`javaw-bytecode`)**:
  - Direct JVM `.class` bytecode parsing via OW2 ASM (`ClassReader`, `ClassNode`, `MethodNode`) enabling consumption of third-party Maven JAR dependencies without requiring source code or `-sources.jar` artifacts.
  - Control Flow Graph analysis and **Relooper algorithm** converting arbitrary JVM branching structures (`goto`, conditional jumps `if*`, `tableswitch`, `lookupswitch`, irreducible loops) into WebAssembly structured control flow (`block`, `loop`, `br`, `br_if`, `br_table`).
  - Operand stack simulation and lowering (`StackToLocalMapper`, `BytecodeInstructionVisitor`) mapping JVM stack depths to WebAssembly local slots for stack manipulations (`dup`, `swap`, `pop`) and instruction translations (arithmetic, bitwise, memory loads/stores, object allocations, field access, static/virtual method calls).
  - Local `.m2` repository cache manager (`LocalM2CacheManager`) storing precompiled WebAssembly artifacts (`${artifactId}-${version}-javaw.wasm` and `.javaw-metadata.json`) directly in `~/.m2/repository/` for near-instantaneous $O(1)$ cache hits on subsequent builds.
  - Deterministic SHA-256 content hashing (`JarHashUtil`) verifying JAR checksums before cache reuse, guaranteeing reliable cache invalidation for updated release or SNAPSHOT dependencies.
  - Seamless Maven build integration: `javaw-maven-plugin:compile` inspects project dependencies, compiles external JAR dependencies on cache miss, caches them in `.m2`, and statically links dependency functions and exports into the application bundle via `WasmLinker`.
- **Log4j2 Enterprise Logging Engine (`org.apache.logging.log4j`)**:
  - Standard Log4j2 API compatibility (`Level`, `Logger`, `LogManager`, `ThreadContext`, `SimpleLogger`) compiling directly into WebAssembly without heavy server-side dependency overhead.
  - Multi-logger category management via `LogManager.getLogger(...)` with runtime level thresholding (`FATAL`, `ERROR`, `WARN`, `INFO`, `DEBUG`, `TRACE`).
  - ThreadContext (Mapped Diagnostic Context / MDC) diagnostics for request correlation, tracing metadata, and structured log context.
  - Parameterized message formatting with `{}` placeholder substitution lowered directly to high-throughput WebAssembly string and stream output.
  - Pluggable log listener architecture (`SimpleLogger.addListener(...)`) allowing WebAssembly UI dashboards, DOM streams, and WASI output consoles to capture and render log streams in real-time.
- **Maven Multi-Module Architecture & Build Tooling (`javaw-maven-plugin`)**:
  - Modular reactor architecture comprising `javaw-core` (embeddable compiler engine, runtime helpers, facades, and AST visitors), `javaw-bytecode` (bytecode parser, Relooper CFG engine, stack lowering, linker, and `.m2` cache manager), `javaw-maven-plugin` (dedicated Maven Mojo automating WebAssembly compilation and dependency linking), and `examples` (interactive consumer module).
  - Dedicated Maven Mojo `javaw:compile` bound to the `process-classes` lifecycle phase with full parameter configuration (`sourceDirectory`, `outputDirectory`, `optimizationLevel`, `generateSourceMaps`, `enableThreads`, `copyRuntimeScript`, `batchMode`, `moduleName`, `includes`, `excludes`).
  - Automated compilation of `.wat`, `.wasm`, `.wasm.map`, and runtime scripts (`javaw-runtime.js`) during standard `mvn compile` or `mvn package`.
- **`java.lang` Standard Library Support & Core Language Idioms**:
  - **Hardware Math & Primitive Wrapper Classes**: Direct WebAssembly FPU instruction lowering for `java.lang.Math` and `StrictMath` (`floor`, `ceil`, `trunc`, `rint`, `round`, `abs`, `sqrt`, `min`, `max`, `copySign`, `signum`, `toDegrees`, `toRadians`) and host/runtime transcendental math imports (`sin`, `cos`, `tan`, `asin`, `acos`, `atan`, `atan2`, `exp`, `log`, `log10`, `pow`, `hypot`, `cbrt`, `sinh`, `cosh`, `tanh`, `random`, `PI`, `E`). Complete primitive wrapper classes (`Integer`, `Long`, `Double`, `Float`, `Boolean`, `Character`, `Byte`, `Short`, `Number`) with compile-time constants (`MAX_VALUE`, `MIN_VALUE`, `BYTES`, `SIZE`), string parsing & radix formatting (`parseInt`, `parseLong`, `parseDouble`, `toHexString`, `toBinaryString`, `toOctalString`), bit conversions (`doubleToLongBits`, `floatToIntBits`), and hardware-accelerated bitwise intrinsics (`bitCount` &rarr; `popcnt`, `numberOfLeadingZeros` &rarr; `clz`, `numberOfTrailingZeros` &rarr; `ctz`, `reverseBytes`, `rotateLeft` &rarr; `rotl`, `rotateRight` &rarr; `rotr`).
  - **Root `Object` Virtual Contracts & Language `enum`**: Universal vtable integration for all classes and interfaces (slot 0: `toString()`, slot 1: `equals(Object)`, slot 2: `hashCode()`), shallow object cloning (`Object.clone()` lowered to `memory.copy`), heap-address `hashCode()`, and first-class Java `enum` lowering (`ordinal()`, `name()`, `values()`, `valueOf()`, and `switch` matching on enum constants).
  - **Expanded `String`, `CharSequence` & Standard `StringBuilder`**: Standard package resolution for `java.lang.StringBuilder` (with `insert`, `delete`, `deleteCharAt`, `reverse`, `replace`, `substring`, `indexOf`) and `java.lang.CharSequence`. Expanded `String` methods: `trim()`, `strip()`, `toLowerCase()`, `toUpperCase()`, `replace(target, rep)`, `contains(seq)`, `startsWith(prefix)`, `endsWith(suffix)`, `toCharArray()`, `getBytes()`, `compareTo(other)`, `equalsIgnoreCase(other)`, `repeat(n)`, and `String.valueOf(...)`.
  - **Standard `System`, High-Resolution Clock & Environment**: High-precision timers `System.currentTimeMillis()` and `System.nanoTime()` connected to WASI/browser performance timers, `System.identityHashCode(obj)`, `System.lineSeparator()`, and environment/property lookups (`System.getenv(key)`, `System.getProperty(key)`).
  - **Resource Management & Core Interfaces (`AutoCloseable`, `Comparable`, `Iterable`)**: Standard interfaces `java.lang.AutoCloseable`, `java.lang.Comparable<T>`, `java.lang.Iterable<T>`, and `java.util.Iterator<E>` with universal vtable slots (slots 3–7) and polymorphic dispatch. Full `try-with-resources` (`try (AutoCloseable res = ...) { ... }`) lowering to nested WebAssembly `try-catch_all` blocks with guaranteed `.close()` cleanup.
  - **Linked source reflection**: Generated constructor/field/method descriptors support invocation, primitive widening, access overrides, and runtime annotation inspection. [ReflectionSupportTest](javaw-core/src/test/java/com/example/javaw/ReflectionSupportTest.java) executes the supported operations and boundaries in WebAssembly. See [reflection limitations](docs/reflection.md), including library member metadata, enum constant-specific bodies, untyped host buffers, and dynamic loading.
  - **Standard Thread/Runnable & Static Class Reflection Tables (`Thread`, `Runnable`, `Class<T>`)**: Standard `java.lang.Thread` and `java.lang.Runnable` wrapping WebAssembly thread primitives (`start()`, `run()`, `sleep(ms)`, `currentThread()`, `getId()`, `getName()`, `setName()`, `isAlive()`, `yield()`, `onSpinWait()`, `activeCount()`). Universal vtable slots (slot 8: `run()`, slot 9: `getClass()`), class literal expressions (`MyClass.class`), and static linear memory `java.lang.Class` descriptor tables supporting `obj.getClass()`, `Class.getName()`, `Class.getSimpleName()`, `Class.isInterface()`, `Class.isEnum()`, `Class.isArray()`, `Class.isInstance()`, and `Class.forName()`.
- **Interactive In-Browser WASI Terminal & REPL Integration (WASI & ANSI)**:
  - Strongly-typed Java facades `com.example.javaw.wasi.WASITerminal` and `com.example.javaw.terminal.Terminal` providing standard ANSI color palette constants (`BLACK`, `RED`, `GREEN`, `YELLOW`, `BLUE`, `MAGENTA`, `CYAN`, `WHITE`, and `BRIGHT_*` variants), text attributes (`BOLD`, `DIM`, `ITALIC`, `UNDERLINE`, `REVERSE`), cursor positioning (`setCursor`), screen clearing (`clear`), and dimensions (`getColumns`, `getRows`).
  - Interactive line and character reading (`readLine()`, `readLine(prompt)`, `readChar()`) wired directly to WASI standard input (`fd 0` stdin queue) with linear memory string decoding.
  - Runtime terminal component `JavawTerminal` in `javaw-runtime.js` supporting both xterm.js and a zero-dependency DOM terminal with full ANSI escape rendering, customizable prompt (`javaw> `), command history (Up/Down arrow navigation), keyboard shortcuts (Ctrl+L clear, Ctrl+C interrupt), and custom command dispatch (`help`, `clear`, `history`, `ls`, `js`, `run <wasmExport>`).
- **Exporting Java Classes to JavaScript Scope (`@JS.Export`)**:
  - Annotate Java classes with `@JS.Export` (or `@JS.Export("CustomName")`) to expose them directly to JavaScript environments (`globalThis.MyJavaClass = ...`), allowing JavaScript code to instantiate Java classes natively using `new MyJavaClass(...)`.
  - Automatically synthesizes class metadata, constructor signatures, vtable bases, and method definitions into WebAssembly custom section `javaw_js_exports`.
  - The runtime bridge (`javaw-runtime.js`) dynamically constructs JavaScript constructor functions and prototypes that allocate linear memory with `$__heap_alloc`, initialize object headers and vtables, and marshal arguments and return values across the WASM-JS runtime boundary.
  - Supports instance methods (`this`), static methods, overloaded constructors, primitive/string/array parameters, object handles, and field modifications.
- **JavaScript Asset Bundling & Resource Embedding (`@JS.Code`, `@JS.Code.Include`, `@JS.Resource`, `JSResource`)**:
  - Embed arbitrary JavaScript script blocks directly into Java classes using `@JS.Code("...")` or bundle external JavaScript files using `@JS.Code.Include({"script1.js", "script2.js"})`.
  - Embed static web assets (CSS stylesheets, JSON configurations, HTML templates, text files) using `@JS.Resource(value = "...", path = "...")` serialized into custom WebAssembly sections `javaw_js_code` and `javaw_js_resources`.
  - The runtime bridge (`javaw-runtime.js`) automatically executes bundled JavaScript scripts upon module instantiation in the global scope and registers embedded resources into memory and VFS.
  - Strongly-typed `JSResource` facade (`com.example.javaw.js.JSResource`) provides methods to query assets (`getAsString`, `hasResource`, `getLength`, `listResources`), inject styles into `<head>` (`injectStyle`, `injectStyleSheet`), execute script snippets (`injectScript`, `injectScriptTag`), and mount resources into WASI VFS (`mountAll`).
- **`@JS` Inline JavaScript Annotations & Host Code Generation**:
  - Allows Java methods (declared `native`) to define inline JavaScript logic directly using `@JS("return Math.hypot(a, b);")`, `@JS("console.log(message);")`, or `@JS("return 'Hello ' + name;")`.
  - Automatic type marshalling and coercion via `@JS.Coerce` for Java primitives, strings, and arrays (`int[]`, `double[]`, `String[]` &harr; JavaScript `Array`) and automatic JSON serialization for JavaScript objects.
  - Automatically extracts snippet code during AST analysis, emits corresponding host import signatures (`(import "dom" "js_inline_..." ...)`), and serializes metadata into WebAssembly custom sections (`(@custom "javaw_js_snippets" ...)` and binary `.wasm` sections).
  - The runtime bridge (`javaw-runtime.js`) automatically deserializes inline snippets and synthesizes callable host functions with zero-copy string decoding and argument conversions.
- **Global JavaScript Class & Object Binding (`@JS.Import`)**:
  - Annotate Java classes or interfaces with `@JS.Import("Date")`, `@JS.Import("Math")`, `@JS.Import("JSON")`, `@JS.Import("console")`, `@JS.Import("localStorage")` to bind standard browser APIs directly to typed Java facades.
  - Automatically synthesizes host bridge snippets for constructor instantiations (`new GlobalName(...)`), direct static method invocations (`GlobalName.method(...)`), constants (`Math.PI`), and instance methods (`this.method(...)`).
  - Includes pre-built standard facades in `com.example.javaw.js`: `JSDate`, `JSMath`, `JSJSON`, `JSConsole`, and `JSStorage`.
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
├── pom.xml                                # Aggregator parent POM with centralized dependencyManagement
├── Readme.md                              # Project documentation
├── Todo.md                                # Feature roadmap and implementation progress
├── demo
│   └── index.html                         # Browser demo harness running compiled Java WASM
├── javaw-core                             # Core compiler engine, AST visitors, runtime & facades
│   ├── pom.xml                            # Core module POM (shaded standalone CLI executable)
│   ├── src
│   │   ├── main
│   │   │   ├── java/com/example/javaw
│   │   │   │   ├── JavacToWasm.java       # Compiler coordinator & programmatic compilation API
│   │   │   │   ├── WasmGeneratorVisitor.java # AST visitor lowering Java statements & expressions
│   │   │   │   ├── WasmRuntime.java       # WebAssembly linear memory runtime routines (heap, strings, arrays)
│   │   │   │   ├── DomHelper.java         # DOM API import collector and emitter
│   │   │   │   ├── WasiHelper.java        # WASI snapshot preview 1 import collector and emitter
│   │   │   │   ├── TypeMapper.java        # Java to WebAssembly type mapping
│   │   │   │   ├── ClassRegistry.java     # Class layout, vtable calculation & dispatch manager
│   │   │   │   ├── ClassModel.java        # Class metadata model
│   │   │   │   ├── RecordModel.java       # Record components & multi-value signature model
│   │   │   │   ├── FieldModel.java        # Field layout & byte offset model
│   │   │   │   ├── MethodModel.java       # Method signature & vtable slot model
│   │   │   │   ├── StringConstant.java    # String literal pool model
│   │   │   │   ├── GlobalVar.java         # Global field variable model
│   │   │   │   ├── LoopContext.java       # Loop label and jump context tracker
│   │   │   │   ├── concurrent
│   │   │   │   │   ├── AtomicInteger.java # WebAssembly atomic integer facade
│   │   │   │   │   ├── Atomics.java       # Low-level array atomics and wait/notify facade
│   │   │   │   │   └── WasmThread.java    # Web Worker thread management facade
│   │   │   │   ├── dom
│   │   │   │   │   ├── DOM.java           # Java DOM API facade
│   │   │   │   │   ├── Element.java       # Java DOM Element facade
│   │   │   │   │   ├── ClassList.java     # Java DOM ClassList facade
│   │   │   │   │   ├── CanvasRenderingContext2D.java # HTML5 Canvas 2D graphics facade
│   │   │   │   │   ├── WebGLRenderingContext.java    # HTML5 WebGL graphics facade
│   │   │   │   │   ├── Window.java        # Browser Window, timers & async fetch facade
│   │   │   │   │   ├── JSObject.java      # Generic dynamic JavaScript interop facade
│   │   │   │   │   ├── JSValue.java       # Base JavaScript value representation
│   │   │   │   │   ├── JSNumber.java      # JavaScript Number wrapper
│   │   │   │   │   ├── JSBoolean.java     # JavaScript Boolean wrapper
│   │   │   │   │   ├── JSString.java      # JavaScript String wrapper
│   │   │   │   │   ├── JSBigInt.java      # JavaScript BigInt wrapper
│   │   │   │   │   ├── JSSymbol.java      # JavaScript Symbol wrapper
│   │   │   │   │   ├── JSUndefined.java   # JavaScript Undefined wrapper
│   │   │   │   │   ├── JSError.java       # JavaScript Error hierarchy wrapper
│   │   │   │   │   └── JSNull.java        # JavaScript Null wrapper
│   │   │   │   ├── js
│   │   │   │   │   ├── JS.java                # @JS annotation and sub-annotation definitions
│   │   │   │   │   ├── JSResource.java        # Static asset & embedded resource facade (@JS.Resource)
│   │   │   │   │   ├── JSDate.java            # Standard JavaScript Date facade (@JS.Import)
│   │   │   │   │   ├── JSMath.java            # Standard JavaScript Math facade (@JS.Import)
│   │   │   │   │   ├── JSJSON.java            # Standard JavaScript JSON facade (@JS.Import)
│   │   │   │   │   ├── JSConsole.java         # Standard JavaScript console facade (@JS.Import)
│   │   │   │   │   ├── JSStorage.java         # Standard JavaScript Web Storage facade (@JS.Import)
│   │   │   │   │   ├── JsSnippetModel.java    # Model for extracted @JS inline JavaScript snippets
│   │   │   │   │   ├── JsExportModel.java     # Model for exported Java classes (@JS.Export)
│   │   │   │   │   └── JsAnnotationHelper.java # AST visitor and annotation discovery helper
│   │   │   │   ├── lang
│   │   │   │   │   ├── AutoCloseable.java     # Standard AutoCloseable interface
│   │   │   │   │   ├── Class.java             # Static class metadata & reflection descriptor facade
│   │   │   │   │   ├── Comparable.java        # Standard Comparable interface
│   │   │   │   │   ├── Iterable.java          # Standard Iterable interface
│   │   │   │   │   ├── Runnable.java          # Standard Runnable interface
│   │   │   │   │   └── Thread.java            # Standard Thread facade wrapping WebAssembly concurrency
│   │   │   │   ├── memory
│   │   │   │   │   └── Memory.java            # WebAssembly Multi-Memory segregation & operations facade
│   │   │   │   ├── simd
│   │   │   │   │   ├── Float32x4.java         # 128-bit float vector facade (f32x4)
│   │   │   │   │   ├── Int32x4.java           # 128-bit integer vector facade (i32x4)
│   │   │   │   │   └── Vector128.java         # 128-bit SIMD constants and utilities
│   │   │   │   ├── sourcemap
│   │   │   │   │   ├── Base64VLQ.java         # Base64 Variable-Length Quantity (VLQ) encoder/decoder
│   │   │   │   │   ├── MappingEntry.java      # Generated to original source coordinate mapping model
│   │   │   │   │   ├── SourceMap.java         # Source Map v3 parser and consumer
│   │   │   │   │   ├── SourceMapGenerator.java # Source Map v3 generator and custom section injector
│   │   │   │   │   └── SourceMapTracker.java   # AST source position tracking visitor helper
│   │   │   │   ├── terminal
│   │   │   │   │   └── Terminal.java          # Terminal facade for interactive console & ANSI styling
│   │   │   │   ├── util
│   │   │   │   │   ├── Arrays.java            # High-performance array utilities & bulk memory operations
│   │   │   │   │   ├── Iterator.java          # Standard Iterator interface
│   │   │   │   │   └── StringBuilder.java     # Dynamic string builder facade
│   │   │   │   └── wasi
│   │   │   │       ├── WASI.java          # WASI snapshot preview 1 system & VFS facade
│   │   │   │       ├── WASIFile.java      # Object-oriented WASI Virtual File System abstraction
│   │   │   │       └── WASITerminal.java  # Interactive WASI terminal, ANSI palette & stdin stream facade
│   │   │   └── resources
│   │   │       └── javaw-runtime.js       # JavaScript WebAssembly host runtime bridge
│   │   └── test
│   │       ├── java/com/example/javaw
│   │       │   ├── App.java               # Java DOM & interactive event demo application
│   │       │   ├── AppBrowserTest.java    # Playwright headless browser integration test suite
│   │       │   ├── JavacToWasmTest.java   # JUnit 5 compiler unit test suite
│   │       │   └── TestClass.java         # Comprehensive Java test suite (loops, arrays, OOP, WASI, threads)
│   │       └── node-tests
│   │           ├── package.json           # Node.js test configuration
│   │           └── test.js                # End-to-end WebAssembly execution & DOM test suite
├── javaw-bytecode                     # Bytecode compiler & local repository cache module
│   ├── pom.xml                        # ASM dependencies (asm, asm-tree, asm-analysis, asm-commons)
│   └── src
│       ├── main/java/com/example/javaw/bytecode
│       │   ├── BytecodeCompiler.java       # Bytecode and JAR compilation orchestrator
│       │   ├── ClassBytecodeCompiler.java  # Individual .class file bytecode translator
│       │   ├── TypeHelper.java             # JVM descriptor to WebAssembly type mapper
│       │   ├── cache
│       │   │   ├── CacheMetadata.java      # JSON metadata contract for cached artifacts
│       │   │   ├── JarHashUtil.java        # SHA-256 JAR content hashing engine
│       │   │   └── LocalM2CacheManager.java # ~/.m2/repository cache store and loader
│       │   ├── cfg
│       │   │   ├── BasicBlock.java         # Basic block node with instruction sequence
│       │   │   ├── ControlFlowGraph.java   # Control Flow Graph builder and edge analyzer
│       │   │   └── Relooper.java           # Relooper structured control flow algorithm
│       │   ├── linker
│       │   │   └── WasmLinker.java         # Static WebAssembly symbol and WAT module linker
│       │   └── lowering
│       │       ├── BytecodeInstructionVisitor.java # JVM bytecode to WebAssembly instruction lowering
│       │       └── StackToLocalMapper.java # JVM operand stack to WebAssembly local variable mapper
│       └── test/java/com/example/javaw/bytecode
│           ├── BytecodeCompilerTest.java   # Synthetic class compilation and execution tests
│           ├── LocalM2CacheManagerTest.java # Cache hit/miss and SHA-256 invalidation tests
│           └── RelooperTest.java           # Relooper control flow structuring unit tests
├── javaw-maven-plugin                     # Maven Plugin module automating Java-to-WASM compilation
│   ├── pom.xml                            # Plugin packaging and maven-plugin-tools descriptor configuration
│   └── src
│       ├── main/java/com/example/javaw/plugin
│       │   └── CompileMojo.java           # Mojo for goal javaw:compile (process-classes phase)
│       └── test/java/com/example/javaw/plugin
│           └── CompileMojoTest.java       # Unit tests for Mojo configuration and defaults
└── examples                               # Interactive demo child module built via javaw-maven-plugin
    ├── pom.xml                            # Examples POM consuming javaw-core and running javaw-maven-plugin
    ├── bytecode
    │   ├── BytecodeAlgorithmsDemo.java    # Relooper control flow structuring & algorithmic routines
    │   └── index.html                     # Standalone bytecode algorithms web runner
    ├── canvas
    │   └── GameOfLife.java                # Conway's Game of Life on HTML5 Canvas 2D
    ├── jsinterop
    │   └── JsInteropShowcase.java         # @JS inline JS, @JS.Coerce, JSDate/JSMath/JSJSON, @JS.Export
    ├── records
    │   └── RecordMathEngine.java          # Java 17 records, multi-value returns & custom exceptions
    ├── simd
    │   └── SimdParticles.java             # 128-bit SIMD vector physics simulation (Float32x4)
    ├── terminal
    │   └── WasiShellDemo.java             # Interactive WASI terminal & virtual file system shell
    ├── threads
    │   └── WorkerAtomicsDemo.java         # WebAssembly shared memory & atomic operations
    ├── todo
    │   ├── TodoApp.java                   # Interactive Todo MVC DOM application
    │   └── index.html                     # Standalone Todo application web runner
    └── index.html                         # Multi-tab interactive browser feature showcase
```

---

## Prerequisites

- **Java Development Kit (JDK)**: Version 17 or higher
- **Apache Maven**: Version 3.6 or higher
- **WABT (`wat2wasm`)** (optional): For compiling `.wat` text to `.wasm` binary modules directly from the CLI or Maven plugin
- **Binaryen (`wasm-opt`)** (optional): For optimizing `.wasm` binary size and execution speed with `-O3`
- **Node.js** (optional): For executing the automated end-to-end test suite

---

## Building the Project

Build all reactor modules (`javaw-core`, `javaw-bytecode`, `javaw-maven-plugin`, and `examples`) using Maven:

```bash
mvn clean package
```

The resulting artifacts:
- **Standalone Fat JAR**: `javaw-core/target/javaw-core-1.0-SNAPSHOT.jar`
- **Bytecode Compiler & Cache Engine**: `javaw-bytecode/target/javaw-bytecode-1.0-SNAPSHOT.jar`
- **Maven Plugin**: `javaw-maven-plugin/target/javaw-maven-plugin-1.0-SNAPSHOT.jar`
- **Compiled WebAssembly Examples**: `examples/target/generated-wasm/`

---

## Maven Plugin Integration (`javaw-maven-plugin`)

The `javaw-maven-plugin` enables zero-friction, automated compilation of Java source code and external Maven dependencies to WebAssembly directly within standard Maven workflows.

### 1. Add Plugin to `pom.xml`

```xml
<project>
    ...
    <dependencies>
        <!-- Dependency for Java DOM, JS interop, WASI, and SIMD facades -->
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>javaw-core</artifactId>
            <version>1.0-SNAPSHOT</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>com.example</groupId>
                <artifactId>javaw-maven-plugin</artifactId>
                <version>1.0-SNAPSHOT</version>
                <executions>
                    <execution>
                        <id>compile-wasm</id>
                        <phase>process-classes</phase>
                        <goals>
                            <goal>compile</goal>
                        </goals>
                        <configuration>
                            <sourceDirectory>${project.basedir}/src/main/java</sourceDirectory>
                            <outputDirectory>${project.build.directory}/generated-wasm</outputDirectory>
                            <optimizationLevel>-O3</optimizationLevel>
                            <generateSourceMaps>true</generateSourceMaps>
                            <enableThreads>true</enableThreads>
                            <copyRuntimeScript>true</copyRuntimeScript>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

### 2. Third-Party Dependency Compilation & Local Repository (`~/.m2`) Caching

When compiling a Maven project that references external dependencies (such as utility JARs or data structures), `javaw-maven-plugin` coordinates with `javaw-bytecode`:
1. **Dependency Inspection**: Scans `project.getArtifacts()` for external compile/runtime JARs on the classpath.
2. **Deterministic SHA-256 Cache Check**: Computes a SHA-256 hash of each dependency JAR and checks the local Maven repository (`~/.m2/repository/{groupId}/{artifactId}/{version}/{artifactId}-{version}-javaw.wasm` and companion `.javaw-metadata.json`).
3. **Bytecode Compilation on Cache Miss**:
   - Parses `.class` bytecode via OW2 ASM.
   - Restructures unstructured JVM jumps (`goto`, `ifeq`, `tableswitch`, etc.) into WebAssembly structured control flow (`block`, `loop`, `br`, `br_if`) using the **Relooper algorithm**.
   - Lowers JVM stack instructions and allocates WebAssembly locals.
   - Saves the precompiled `.wasm` artifact and metadata JSON directly into `~/.m2/repository`.
4. **Instant $O(1)$ Cache Hit**: Reuses previously compiled `.wasm` artifacts on subsequent builds without reparsing or recompiling bytecode.
5. **Static Linking**: Merges application WebAssembly modules with dependency WebAssembly units into the final application bundle using `WasmLinker`.

### 3. Plugin Configuration Parameters

| Parameter | Type | Default Value | Description |
| :--- | :--- | :--- | :--- |
| `sourceDirectory` | `File` | `${project.basedir}/src/main/java` | Source folder containing Java files to compile to WebAssembly |
| `outputDirectory` | `File` | `${project.build.directory}/generated-wasm` | Destination folder for emitted `.wat`, `.wasm`, `.wasm.map`, and runtime assets |
| `optimizationLevel` | `String` | `-O3` | Binaryen `wasm-opt` optimization flag (`-O0`, `-O1`, `-O2`, `-O3`, `-Oz`, or `--no-opt`) |
| `generateSourceMaps` | `boolean` | `true` | Enables generation of Source Map v3 (`.wasm.map` and `.wat.map`) debugging sections |
| `enableThreads` | `boolean` | `true` | Enables shared memory and WebAssembly atomic instruction lowering |
| `copyRuntimeScript` | `boolean` | `true` | Automatically copies `javaw-runtime.js` into `outputDirectory` for immediate browser serving |
| `batchMode` | `boolean` | `false` | If `true`, compiles all matching Java source files into a single unified WASM module |
| `moduleName` | `String` | `${project.artifactId}` | Custom module stem name when compiling in `batchMode` |
| `includes` | `List<String>` | `null` | Glob patterns for source files to include (e.g. `**/*.java`) |
| `excludes` | `List<String>` | `null` | Glob patterns for source files to exclude |

---

## CLI Usage

### 1. Compiling Java to WebAssembly via CLI

#### Single File Compilation
Run the standalone compiler against any Java source file:

```bash
java -jar javaw-core/target/javaw-core-1.0-SNAPSHOT.jar <path-to-java-file.java>
```

#### Multi-File and Directory Compilation
Compile multiple Java source files or an entire directory into a unified WebAssembly module:

```bash
# Multiple explicit files
java -jar javaw-core/target/javaw-core-1.0-SNAPSHOT.jar ServiceA.java ServiceB.java App.java

# Entire source directory
java -jar javaw-core/target/javaw-core-1.0-SNAPSHOT.jar src/main/java/com/example/app
```

#### Disabling Optimization
To skip the `wasm-opt` optimization pass during development:

```bash
java -jar javaw-core/target/javaw-core-1.0-SNAPSHOT.jar --no-opt src/test/java/com/example/javaw/App.java
```

#### Generating Source Maps
To generate standard Source Map v3 files (`.wat.map` and `.wasm.map`) with embedded source locations for DevTools step-through debugging:

```bash
java -jar javaw-core/target/javaw-core-1.0-SNAPSHOT.jar --source-map src/test/java/com/example/javaw/App.java
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

#### Example S: Global JavaScript Class & Object Binding (`@JS.Import`)

Bind standard browser and JavaScript global objects (`Date`, `Math`, `JSON`, `console`, `localStorage`) directly into strongly-typed Java facades:

```java
package com.example.javaw;

import com.example.javaw.dom.JSObject;
import com.example.javaw.dom.JSValue;
import com.example.javaw.js.JS;
import com.example.javaw.js.JSConsole;
import com.example.javaw.js.JSDate;
import com.example.javaw.js.JSJSON;
import com.example.javaw.js.JSMath;
import com.example.javaw.js.JSStorage;

// Custom browser API binding
@JS.Import("WebSocket")
class JSWebSocket {
    public static native JSWebSocket create(String url);
    public native void send(String data);
    public native void close();
}

public class JsImportDemo {
    public static void run() {
        // Standard Math operations
        double dist = JSMath.hypot(3.0, 4.0); // 5.0
        double pi = JSMath.PI();              // 3.141592653589793

        // Standard Date operations
        JSDate now = JSDate.create();
        int year = now.getFullYear();
        double time = now.getTime();
        String iso = now.toISOString();

        // JSON serialization & parsing
        JSObject config = JSObject.createObject();
        config.set("app", "javaw");
        config.set("version", 2026);
        String jsonStr = JSJSON.stringify(config); // '{"app":"javaw","version":2026}'
        JSValue parsed = JSJSON.parse(jsonStr);

        // Web Storage (localStorage)
        JSStorage.setItem("user-token", "abc-123-xyz");
        String token = JSStorage.getItem("user-token");

        // Console logging
        JSConsole.log("Application started successfully in year " + year);
    }
}
```

---

#### Example T: Exporting Java Classes to JavaScript Scope (`@JS.Export`)

Annotate Java classes with `@JS.Export` to allow JavaScript code to instantiate Java classes natively, pass arguments to constructors, and invoke instance or static methods:

```java
package com.example.javaw;

import com.example.javaw.js.JS;

@JS.Export("Counter")
public class ExportedCounter {
    private int count;

    public ExportedCounter() {
        this.count = 0;
    }

    public ExportedCounter(int initial) {
        this.count = initial;
    }

    public int getCount() {
        return this.count;
    }

    public int increment() {
        this.count++;
        return this.count;
    }

    public int add(int delta) {
        this.count += delta;
        return this.count;
    }

    public String format(String prefix) {
        return prefix + ": " + this.count;
    }

    public static int multiply(int a, int b) {
        return a * b;
    }
}
```

In JavaScript, once instantiated with `javaw-runtime.js`, `Counter` is available globally or on `runtime.classes`:

```javascript
// Instantiating Java class directly in JavaScript
const counter = new Counter(10);
console.log(counter.getCount());      // 10
console.log(counter.increment());     // 11
console.log(counter.add(5));          // 16
console.log(counter.format("Score")); // "Score: 16"

// Calling exported static methods
console.log(Counter.multiply(6, 7));  // 42
```

---

#### Example U: JavaScript Asset Bundling & Resource Embedding (`@JS.Code`, `@JS.Resource`, `JSResource`)

Embed JavaScript helper functions, stylesheet assets, and JSON configurations into compiled WebAssembly modules and interact with them using `JSResource`:

```java
package com.example.javaw;

import com.example.javaw.js.JS;
import com.example.javaw.js.JSResource;

// Bundle JavaScript script executed upon WebAssembly module initialization
@JS.Code("function initChartLibrary() { console.log('Chart library initialized'); }")
// Embed static CSS stylesheet resource
@JS.Resource(value = "body { margin: 0; font-family: sans-serif; } .card { padding: 16px; }", path = "app.css")
// Embed configuration JSON
@JS.Resource(value = "{\"title\":\"Javaw App\",\"theme\":\"dark\",\"version\":2}", path = "config.json")
public class AssetDemo {

    public static void setup() {
        // Query embedded static resources
        if (JSResource.hasResource("config.json")) {
            String configJson = JSResource.getAsString("config.json");
            int len = JSResource.getLength("config.json");
        }

        // Dynamically inject stylesheet into browser <head>
        JSResource.injectStyleSheet("app.css");

        // Inject and execute dynamic script
        JSResource.injectScript("initChartLibrary();");

        // List all bundled assets
        String[] assets = JSResource.listResources();

        // Mount all bundled resources into the WASI virtual file system (VFS)
        JSResource.mountAll();
    }
}
```

---

#### Example V: Interactive In-Browser WASI Terminal & REPL Integration (`WASITerminal`, `Terminal`, `JavawTerminal`)

Build interactive command-line interfaces, diagnostic shells, or REPLs that run directly inside the browser using standard I/O streams and ANSI escape styling:

```java
package com.example.javaw;

import com.example.javaw.terminal.Terminal;

public class ShellApp {

    public static void runShell() {
        // Clear terminal screen and show banner
        Terminal.clear();
        Terminal.println(Terminal.BRIGHT_CYAN + Terminal.BOLD + "=== Javaw Interactive Browser Shell ===" + Terminal.RESET);
        Terminal.println(Terminal.DIM + "Type 'help' for commands or 'exit' to quit." + Terminal.RESET);

        boolean running = true;
        while (running) {
            // Read input with prompt
            String line = Terminal.readLine(Terminal.BRIGHT_GREEN + "javaw> " + Terminal.RESET);
            if (line == null || line.equals("exit")) {
                running = false;
                Terminal.println(Terminal.YELLOW + "Session terminated." + Terminal.RESET);
            } else if (line.equals("help")) {
                Terminal.println(Terminal.BOLD + "Available Commands:" + Terminal.RESET);
                Terminal.println("  " + Terminal.CYAN + "clear" + Terminal.RESET + "   - Clear the terminal screen");
                Terminal.println("  " + Terminal.CYAN + "info" + Terminal.RESET + "    - Show terminal dimensions");
                Terminal.println("  " + Terminal.CYAN + "exit" + Terminal.RESET + "    - Quit the shell session");
            } else if (line.equals("clear")) {
                Terminal.clear();
            } else if (line.equals("info")) {
                Terminal.println("Columns: " + Terminal.getColumns() + ", Rows: " + Terminal.getRows());
            } else if (!line.isEmpty()) {
                Terminal.println(Terminal.RED + "Unknown command: " + line + Terminal.RESET);
            }
        }
    }
}
```

Attach `JavawTerminal` in JavaScript to an xterm.js instance or zero-dependency DOM element:

```javascript
// Attach interactive terminal to a DOM element
const terminal = runtime.createTerminal('#terminal-container', {
    prompt: 'javaw> ',
    cols: 80,
    rows: 24
});

// Run the Java WebAssembly shell routine
runtime.instance.exports.runShell();
```

---

#### Example W: `java.lang` Standard Library, Enums, Try-With-Resources & Reflection

Author idiomatic Java code using standard mathematical functions, wrapper utilities, enums with `switch` statements, `try-with-resources` automatic cleanup, multithreaded tasks, and static class reflection:

```java
package com.example.javaw;

import com.example.javaw.lang.AutoCloseable;
import com.example.javaw.lang.Class;
import com.example.javaw.lang.Runnable;
import com.example.javaw.lang.Thread;

enum Status {
    PENDING,
    ACTIVE,
    COMPLETED
}

class Transaction implements AutoCloseable {
    private String name;
    private boolean closed;

    public Transaction(String name) {
        this.name = name;
        this.closed = false;
    }

    public void commit() {
        System.out.println("Transaction committed: " + this.name);
    }

    @Override
    public void close() {
        this.closed = true;
        System.out.println("Transaction closed safely.");
    }
}

class BackgroundWorker implements Runnable {
    public int processed = 0;

    @Override
    public void run() {
        this.processed += 100;
    }
}

public class StandardLibraryDemo {

    public static void demonstrateFeatures() {
        // 1. Math and Hardware FPU Operations
        double root = Math.sqrt(144.0);                  // f64.sqrt
        double rounded = Math.floor(45.8);               // f64.floor
        int maxVal = Math.max(10, 20);                   // Direct comparison
        double angleRad = Math.toRadians(180.0);         // PI

        // 2. Primitive Wrappers & Bitwise Intrinsics
        int parsed = Integer.parseInt("1024");
        String hex = Integer.toHexString(255);           // "ff"
        int popCount = Integer.bitCount(0b1011001);       // i32.popcnt -> 4
        int leadingZeros = Integer.numberOfLeadingZeros(16); // i32.clz

        // 3. String Methods & Dynamic StringBuilder
        String text = "   Java to WebAssembly   ".trim().toUpperCase();
        boolean containsWasm = text.contains("WEBASSEMBLY");
        String replaced = text.replace("JAVA", "JAVAW");

        // 4. Java Enums & Switch Expressions
        Status currentStatus = Status.ACTIVE;
        String statusLabel;
        switch (currentStatus) {
            case PENDING -> statusLabel = "Waiting";
            case ACTIVE -> statusLabel = "In Progress";
            case COMPLETED -> statusLabel = "Done";
            default -> statusLabel = "Unknown";
        }

        // 5. Try-With-Resources (Guaranteed .close() Invocation)
        try (Transaction tx = new Transaction("Tx-01")) {
            tx.commit();
        }

        // 6. Thread & Runnable Task Execution
        BackgroundWorker worker = new BackgroundWorker();
        Thread thread = new Thread(worker, "worker-thread");
        thread.start();
        Thread.yield();

        // 7. Static Class Reflection & Introspection
        Class<?> workerClass = worker.getClass();
        String className = workerClass.getName();         // "BackgroundWorker"
        boolean isInterface = workerClass.isInterface();  // false
        boolean isInstance = workerClass.isInstance(worker); // true
    }
}
```

---

#### Example X: Log4j2 Enterprise Logging & Diagnostic Context (`org.apache.logging.log4j`)

Use standard Apache Log4j2 APIs for category-based logging, dynamic severity thresholding, ThreadContext (MDC) tracing, and parameterized message formatting in WebAssembly:

```java
package com.example.javaw;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

public class Log4j2App {

    private static final Logger authLogger = LogManager.getLogger("AuthService");
    private static final Logger dbLogger = LogManager.getLogger("DatabasePool");

    public static void runLoggingDemo() {
        // 1. ThreadContext (MDC) Request Correlation
        ThreadContext.put("env", "production");
        ThreadContext.put("traceId", "req-98214");

        // 2. Standard Category Logging with Parameterized Formatting
        authLogger.info("User {} authenticated successfully with roles {}", "doug_admin", "[ADMIN, OPERATOR]");

        // 3. Dynamic Level Adjustment & Conditional Filtering
        dbLogger.setLevel(Level.DEBUG);
        dbLogger.debug("Allocated connection handle: {}", "conn-42");

        // 4. Warning & Error Diagnostics
        if (dbLogger.isWarnEnabled()) {
            dbLogger.warn("Query latency high: {} ms for query [{}]", 480, "SELECT * FROM orders");
        }
        dbLogger.error("Connection reset by peer on node {}", "cluster-eu-01");

        // 5. Cleanup Diagnostic Context
        ThreadContext.clearMap();
    }
}
```

---

### 3. Example Applications Suite (`examples/`)

The repository includes a suite of standalone, fully functional example applications in the `examples/` directory showcasing each major capability of `javaw`:

| Example Application | Directory / File | Key Features Demonstrated |
| :--- | :--- | :--- |
| **Spring API CRUD subset** | `examples/springboot-h2/` | Constructor-injected controllers, generated integer IDs, in-memory Wasm repositories, annotation-based routes, Node.js HTTP/JSON bridge |
| **Log4j2 Enterprise Logging** | `examples/log4j/Log4j2Demo.java` | Standard Log4j2 API (`LogManager`, `Logger`, `Level`, `ThreadContext` MDC), parameter formatting, live UI log streaming |
| **Bytecode & Relooper Algorithms** | `examples/bytecode/BytecodeAlgorithmsDemo.java` | Complex control flow structuring (Euclidean GCD, Fibonacci, Collatz, Prime Sieve, State Machine Lexer) |
| **Interactive Todo MVC** | `examples/todo/TodoApp.java` | DOM tree creation, `Element.appendChild`, `removeChild`, input forms, event listeners, dynamic strings, active item counts |
| **Conway's Game of Life** | `examples/canvas/GameOfLife.java` | HTML5 Canvas 2D (`CanvasRenderingContext2D`), grid state in linear memory, animation loops, color styling |
| **128-Bit SIMD Physics** | `examples/simd/SimdParticles.java` | Fixed-width SIMD (`Float32x4`, `Vector128`), parallel coordinate integration (`v128`), boundary reflection |
| **JavaScript Interop Showcase** | `examples/jsinterop/JsInteropShowcase.java` | `@JS` inline JavaScript snippets, `@JS.Coerce` conversions, `JSDate`, `JSMath`, `JSJSON`, `JSStorage`, `@JS.Export` |
| **WASI Terminal & Shell** | `examples/terminal/WasiShellDemo.java` | In-browser `WASITerminal` / `Terminal`, ANSI 16-color styling, interactive `readLine` prompt, `WASIFile` virtual file system |
| **Record Math Engine** | `examples/records/RecordMathEngine.java` | Java 17 `record` types, zero-overhead multi-value stack returns, custom exception hierarchies, multi-catch dispatch |
| **Shared Memory Atomics** | `examples/threads/WorkerAtomicsDemo.java` | WebAssembly shared memory (`shared`), `AtomicInteger`, `Atomics` array operations, hardware concurrency detection |

#### Running the Interactive Feature Showcase

An interactive multi-tab web showcase is provided at `examples/index.html`. You can compile all examples and run the showcase locally using Maven and any local HTTP server:

```bash
# Option 1: Compile all examples automatically via Maven Multi-Module build
mvn clean package

# Option 2: Compile a single example explicitly using the standalone compiler CLI
java -jar javaw-core/target/javaw-core-1.0-SNAPSHOT.jar examples/todo/TodoApp.java

# Open the showcase in your browser
python3 -m http.server 8000
# Navigate to: http://localhost:8000/examples/index.html
```

---

### 4. Spring API CRUD subset on WebAssembly (`examples/springboot-h2`)

The compiler lowers the unchanged `SpringBootApp.java`, `ProductRepository.java`, and `Product.java` into a unified Wasm module. `SpringApplication.run` creates singleton repositories, constructor-injected controllers, and linked `@Service`/`@Component` objects. Dependencies may be concrete classes or uniquely implemented source interfaces, resolved transitively before their consumers. Controller method bodies execute in Wasm, including repository operations, generated IDs, and lazy `orElseThrow` exception suppliers.

This is ahead-of-time support for a subset of Spring APIs. It does **not** run Spring Boot, Hibernate, or the H2 SQL engine. The Maven dependencies provide the Java API types; `linkDependencies=false` keeps their JVM implementations out of the Wasm module. Repository rows use a linked list in Wasm linear memory, with linear scans for lookup. There is no SQL, JDBC transaction manager, disk persistence, or general Spring container.

The compiler embeds `javaw_spring_web` metadata describing routes, parameters, and entity field offsets. `spring-web.js` uses it to pass HTTP arguments to Wasm controller instances, serialize returned entities/arrays, and translate `ResponseStatusException` into HTTP errors. Node does not store the repository rows.

Supported patterns in this example:

- One constructor per managed controller, startup application, service, or component (or an implicit default constructor), with dependencies on other linked managed beans or `CrudRepository<Entity, Integer>` interfaces.
- An entity with one generated `int`/`Integer` ID, and `String`, `BigDecimal`, `int`, or `Integer` fields.
- `findAll`, `findById`, `save`, and `deleteById`, plus derived equality queries named `findBy...`, `countBy...`, and `existsBy...`. Queries support direct `String`, `int`, `Integer`, and `BigDecimal` fields, `And`/`Or` predicates, and `Is`/`Equals` aliases; unsupported query forms fail compilation.
- `findById(...).orElseThrow(() -> new ResponseStatusException(...))` with an expression supplier using method parameters.
- Single string `@RequestMapping`, `@GetMapping`, `@PostMapping`, `@PutMapping`, and `@DeleteMapping` paths; integer `@PathVariable` arguments and JSON object fields for other arguments.
- `HttpStatus.NOT_FOUND`, `BAD_REQUEST`, `CONFLICT`, and `INTERNAL_SERVER_ERROR`.

Derived queries execute linear scans over the live Wasm rows. Multi-result queries preserve repository order and return fresh arrays of entity references; declared `List<Entity>` results use the existing array lowering. Single-result queries return the entity (or null), or a source-backed `Optional<Entity>`, and reject multiple matches with `IncorrectResultSizeDataAccessException`. Null parameters match null fields; decimal equality compares numeric value without losing precision. `SpringRepositoryQueryTest` executes this contract, and `SpringWebSupportTest` validates query-backed controller metadata. See [supported query syntax and limits](docs/spring-repository-queries.md).

Marker `@Transactional` on public instance methods or managed classes now provides synchronous default REQUIRED transactions. Outer boundaries snapshot all linked repository rows and entity fields; unchecked failures restore membership, row order, and fields on the original objects, while checked exceptions commit by default. Joined failures mark rollback-only and produce `UnexpectedRollbackException` if swallowed by the outer method. Generated IDs remain monotonic, so rolled-back inserts consume IDs. Advanced options and concurrent isolation remain unsupported. `SpringTransactionTest` verifies this contract and snapshot cleanup; see [transaction semantics and limits](docs/spring-transactions.md).

`BigDecimal` values are arbitrary-precision Wasm objects with scale and rounding semantics. The HTTP bridge preserves decimal JSON lexemes and emits exact JSON numbers; JavaScript clients must preserve that text or use strings to avoid client-side rounding. Storage lasts for one Wasm instance. The bridge collects unreachable rows and request temporaries after responses are copied into JavaScript; retained repository rows remain roots.

Missing, ambiguous, and cyclic constructor dependencies fail compilation with source diagnostics. Bootstrap creates each bean once per Wasm instance; repeated starts preserve the graph and do not repeat runners. Reentrant starts and retries after a constructor/runner failure throw `IllegalStateException`. Field/setter injection, qualifiers, scopes, general factory beans, lifecycle callbacks, and concurrent startup are outside this subset. See [the injection contract and executable tests](docs/spring-injection.md).

The application runs its `@Bean ApplicationRunner` lambda once at startup, inserting three products. The application class receives the same repository singleton as the controller. PUT invokes the entity setters and saves the changed product. These behaviors follow the Java source; inferred `var` locals retain their entity type for method dispatch. POST returns 201, successful DELETE returns 204 with no body, and missing products return 404 with the Java exception's reason.

```bash
# Build the compiler/plugin and this example together, avoiding stale installed plugins.
mvn -pl javaw-maven-plugin,examples/springboot-h2 -am package -DskipTests

# Start the API and dashboard on port 8080.
npm start --prefix examples/springboot-h2

# Run HTTP, repository, error handling, and instance-isolation checks.
npm test --prefix examples/springboot-h2
```

```bash
curl http://localhost:8080/api/products
curl -X POST http://localhost:8080/api/products -H 'Content-Type: application/json' -d '{"name":"Keyboard","price":129.95,"stock":40}'
curl http://localhost:8080/api/products/1
curl -X DELETE http://localhost:8080/api/products/1
```

---

### 5. Running in the Browser

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

    <script src="javaw-core/src/main/resources/javaw-runtime.js"></script>
    <script>
        async function run() {
            const response = await fetch('javaw-core/src/test/java/com/example/javaw/App.wasm');
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

Run the full Java test suite across all modules, including compiler AST unit tests, Mojo plugin tests, and headless Chromium browser tests:

```bash
mvn test
```

The browser test suite (`AppBrowserTest.java`) launches a headless browser using Microsoft Playwright, loads the compiled Java WebAssembly application, and verifies live DOM rendering, styling, and event dispatching.

### End-to-End Node.js & DOM Integration Tests

Run the Node.js test suite to verify binary execution, string decoding, threads/atomics, WebAssembly exception handling, dynamic JavaScript interop, WASI streams, and interactive DOM mutations:

```bash
npm test --prefix javaw-core/src/test/node-tests
```


### Phase 3 decimal support

`BigInteger` and `BigDecimal` execute source-backed arbitrary-precision arithmetic in Wasm. Decimal values retain an integer coefficient and signed scale; addition, subtraction, multiplication, exact/rounded division, scale changes, all eight `RoundingMode` values, comparison, trailing-zero stripping, powers, and exact integer conversions are covered by executable regressions. String, int, long, unscaled-integer, and exact binary-double constructors are supported. `BigDecimal.valueOf` supports long factories and compile-time double constants; dynamic double factories, `MathContext`, and complete JDK math coverage remain pending.

The Spring bridge parses decimal JSON number lexemes directly into Wasm decimal objects and emits their exact text as JSON numbers, including digits beyond JavaScript precision. Send decimal strings when your client already uses JavaScript numbers; `JSON.parse`/ordinary browser number displays can still round a correctly transmitted value. HTTP decimal exponents are limited to ±100000. `DecimalSupportTest` compares rounding against the JDK, and `examples/springboot-h2/test.js` checks exact HTTP round-trips.


### Host-boundary heap collection and ownership

All compiler-managed allocations use a non-moving block allocator in the default linear memory. It grows memory with checked sizes, reuses cleared free blocks, splits large blocks, and coalesces adjacent free blocks after collection. Wasm memory pages cannot shrink; reuse bounds growth for stable workloads. Payload headers track object identity without a separately allocated identity registry. StringBuilder handles remain stable while their separate buffers grow.

Call `runtime.collectGarbage()` (or `instance.exports.__gc_collect()`) only between Wasm calls, after response/return values have been copied or retained, with no active Wasm frame, reentrant import, or concurrent worker. Collection is never triggered inside allocation: locals and operand-stack values are not enumerated. The Spring handler establishes this boundary after each request, including exceptions.

Static i32 globals, class descriptors, enums, wrapper caches, and explicitly retained host pointers are roots. Reachable payloads are scanned conservatively for pointers, including interior pointers; this reaches fields, arrays, collections, captured closures, decimal coefficients, and builder buffers, and reclaims unreachable cycles. Integer bit patterns can conservatively retain extra blocks. There is no compaction, finalization, weak-reference processing, concurrent collector, or automatic collection for arbitrary applications. Directly managed secondary memories and raw addresses are outside this allocator.

A raw returned pointer is borrowed until the next collection. Hosts that store it across collections must call `runtime.retain(pointer)` and pair each retain with `runtime.release(pointer)` when finished. This includes object pointers kept in generated JS wrappers (`wrapper.__ptr`), asynchronous callbacks, and custom host handles. Strings copied into JS no longer require a root. Ordinary JS numeric handles are not Wasm roots. Retains are counted, accept interior pointers, and do not move objects; a pointer must not be used after its final release and a collection. Static string-pool pointers do not require retention. `runtime.getHeapStats()` reports live payload bytes, allocator high-water bytes, and memory bytes.

`ManagedHeapTest` checks roots, captured arrays, cycles, host ownership, interior pointers, zeroed reuse, builder growth, exception cleanup, and a stable 500-cycle heap. The Spring test verifies exact decimals and a stable 300-cycle CRUD heap after lazy cache warm-up. See the [memory-management design](docs/memory-management.md) for the choice of collector and the WasmGC evaluation.

### Assertions and source runtime boundaries

Java assertions compile to Wasm and are disabled by default. Modules containing `assert` export `__javaw_set_assertions(boolean)`; hosts can call `instance.exports.__javaw_set_assertions(1)` before invoking application exports. Disabled assertions evaluate neither condition nor detail; enabled failures throw `AssertionError extends Error`, with a stringified detail. Typed catch dispatch preserves the Error/Exception distinction, including single and union catches. Per-class/package assertion policies and AssertionError cause chaining are not implemented.

The compiler rejects synchronized blocks/methods, volatile fields, non-static inner/local/anonymous classes, and native methods without `@JS`/`@JS.Import` implementations with source locations and remediation. Move non-static inner implementations to top-level classes. Explicit atomic facades do not provide a general Java Memory Model or worker-safe heap. These boundaries are covered by [SourceRuntimeSupportTest](javaw-core/src/test/java/com/example/javaw/SourceRuntimeSupportTest.java).

Host-independent `java.io` byte streams now include InputStream/OutputStream bulk operations and ByteArrayInputStream/ByteArrayOutputStream storage, growth, read/write, unsigned reads, skip, available, mark/reset, copy isolation, and no-op close. [IoSupportTest](javaw-core/src/test/java/com/example/javaw/IoSupportTest.java) executes this subset in Wasm. Charset conversion, file/socket streams, NIO, serialization, and concurrent stream access remain pending; the byte-stream implementation does not provide Java monitor semantics.

Host-independent character streams now include `Reader`/`Writer`, `StringReader`/`StringWriter`, `CharArrayReader`/`CharArrayWriter`, and `BufferedReader`/`BufferedWriter`, with `Closeable`, `Flushable`, and writer `Appendable` dispatch. `IoSupportTest` executes buffered CR/LF/CRLF line reading, repeated EOF and mark/reset, backward StringReader skipping, growth and copy isolation, close cleanup after write failures, and bounds/null errors in Wasm. The compiler also distinguishes ordinary object fields named `length` from array headers. String conversion is currently limited by the byte-oriented string runtime; full UTF-16 semantics, `StringBuffer`/`StringWriter.getBuffer()`, charset bridges, newer stream convenience APIs, and thread-safe stream operations remain pending. See [the character-stream contract](docs/character-streams.md).

The [bytecode compatibility matrix](docs/compatibility.md) lists admitted opcodes, required imports, source/bytecode backend differences, tested dependency artifacts, and open Phase 4/5 work. Unsupported bytecode receives file/line (when available), class/method descriptor, construct, and remediation before WAT is returned. The backend rejects unlinked calls, invokedynamic, unsupported constants, native/monitor code, unlinked catch types and unsupported Throwable APIs rather than emitting partial functions. Unsupported source statements also fail explicitly; `do-while` currently requires rewriting to supported loop control flow.

Static member classes with unique simple names retain the existing flattened module representation; nested names with the same simple name receive an explicit diagnostic. `SourceRuntimeSupportTest` executes static nested construction and dispatch. Enclosing-instance capture remains unsupported.


### Experimental WasmGC backend

`JavacToWasm.generateGcWat(source)` selects the standalone `WasmGcVisitor` prototype. It emits native GC structs and typed references, including constructor allocation, mutable reference/primitive fields, cyclic graphs, and exported methods. `WasmGcVisitorTest` assembles with Binaryen and executes in Node, including passing an opaque GC object through JavaScript back to Wasm. The default compiler continues to use linear memory.

This is a restricted backend: inheritance, virtual calls, arrays, libraries, exceptions, and control flow are pending. Unsupported syntax receives a location and guidance to use the default backend. See [supported syntax, assembly instructions, and host ownership](docs/wasmgc.md). Validation: `mvn -pl javaw-core -Dtest=WasmGcVisitorTest,ManagedHeapTest test`.

### Progress toward actual H2 and Spring/Hibernate execution

The bytecode backend now executes `TABLESWITCH` and `LOOKUPSWITCH` through selector comparisons and state dispatch. Executable javac-to-Wasm regressions cover dense/sparse keys, signed integer extremes, default and shared targets, fallthrough, joins with existing operand-stack values, and switches inside loops. Dispatch currently uses a linear comparison chain.

This completes a prerequisite of the next open Todo item. Actual H2 SQL and full Spring/Hibernate execution remain unimplemented: object/virtual dispatch, typed bytecode, exceptions, class initialization, and JDK/framework linkage still need substantial work. The Spring example continues to use its documented in-memory API subset. Validation: `mvn -o -pl javaw-bytecode -am -Dtest=BytecodeCompatibilityTest,BytecodeCompilerTest,RelooperTest -Dsurefire.failIfNoSpecifiedTests=false test` (10 tests passed).

The next completed bytecode prerequisite adds linked `NEW`/`GETFIELD`/`PUTFIELD` for concrete classes extending `Object` directly. Allocation uses the actual field layout size and clears dirty host memory to Java defaults; constructor calls initialize fields, references preserve aliases/cycles, and null field access traps. Executable tests cover objects larger than the former provisional 64-byte allocation and consistent cross-class layouts in a JAR. Inherited layouts, virtual dispatch, catchable null exceptions, and actual H2/framework execution remain pending. See [the bytecode object contract](docs/compatibility.md#bytecode-object-allocation-contract). Validation: `mvn -o -pl javaw-bytecode -am -Dtest='com.example.javaw.bytecode.*Test' -Dsurefire.failIfNoSpecifiedTests=false test` (12 tests passed).

Inherited object layouts are now supported by the bytecode backend. A subclass retains its superclass payload as a prefix and appends its own fields, so superclass constructors and parent-typed aliases use consistent storage. Hidden fields resolve by symbolic owner, name, and descriptor, including inherited field references naming a subclass. Layout construction supports abstract intermediate classes and is independent of JAR entry order; missing/cyclic superclass chains fail with diagnostics. Virtual/interface dispatch, typed numeric bytecode, exceptions, class initialization, and full H2/Spring/Hibernate execution remain open. Validation: `mvn -o -pl javaw-bytecode -am -Dtest='com.example.javaw.bytecode.*Test' -Dsurefire.failIfNoSpecifiedTests=false test` (14 tests passed).

The next completed bytecode prerequisite adds closed-world public virtual/interface calls. Objects carry a module-local class ID; dispatch selects the most-derived linked class implementation, including inherited methods and calls through interface references. Wasm regressions cover constructor-time dispatch, direct super calls, nested arguments, void/reference results, and null/unrelated receiver traps. Non-public virtual calls receive diagnostics; public interface default support is described below. Full JVM dispatch semantics and H2/Spring/Hibernate execution remain open. See [the dispatch contract](docs/compatibility.md#bytecode-dynamic-dispatch-contract). Validation: `mvn -o -pl javaw-bytecode -am -Dtest='com.example.javaw.bytecode.*Test' -Dsurefire.failIfNoSpecifiedTests=false test` (16 tests passed).

Public interface default methods now execute in the bytecode backend. Dispatch selects the most-specific default through interface diamonds, gives class declarations precedence, and honors abstract redeclarations. Direct and inherited `Interface.super` calls bind to their resolved default. Executable Wasm tests cover nested/default-to-abstract calls, void/reference results, superclass overrides, and call-site diagnostics for conflicting defaults or missing implementations. Non-public virtual calls and the remaining H2/runtime foundations stay open. See [the dispatch contract](docs/compatibility.md#bytecode-dynamic-dispatch-contract). Validation: `mvn -o -pl javaw-bytecode -am -Dtest='com.example.javaw.bytecode.*Test' -Dsurefire.failIfNoSpecifiedTests=false test` (18 tests passed).

Private bytecode class/interface helpers now execute with their declaring-method binding, so same-named subclass methods cannot redirect them. Calls support private overloads, void/reference results, and validated nestmate access, including private constructors and static helpers. Same-class calls need no nest metadata; nestmate calls require a linked host, reciprocal same-package NestHost/NestMembers metadata, and receive call-site diagnostics when invalid. Wasm regressions also execute Java 8 private INVOKESPECIAL bytecode and verify null/argument behavior. Protected/package virtual calls and the remaining H2/runtime foundations stay open. See [the private-method contract](docs/compatibility.md#private-bytecode-methods-and-nests). Validation: `mvn -o -pl javaw-bytecode -am -Dtest='com.example.javaw.bytecode.*Test' -Dsurefire.failIfNoSpecifiedTests=false test` (23 tests passed).

Protected and package-access virtual methods now execute in the bytecode backend with caller access checks. Package-private methods retain their own dispatch slots across package boundaries, while same-package overrides that widen access permit later cross-package overrides. Protected calls outside the declaring package restrict receivers to the caller class or subclasses. Multi-package Wasm regressions match JVM execution; additional tests reject inaccessible callers and trap base/sibling receivers. Full JVM linkage/access verification and the remaining H2/runtime foundations stay open. See [the access and override contract](docs/compatibility.md#protected-and-package-access-virtual-methods). Validation: `mvn -o -pl javaw-bytecode -am -Dtest='com.example.javaw.bytecode.*Test' -Dsurefire.failIfNoSpecifiedTests=false test` (26 tests passed).

Long bytecode now executes with typed i64 locals and operand-stack values, mixed int/long parameter positions, inherited/hidden long fields, constants, arithmetic, shifts, comparisons, conversions, and category-two stack operations. Wasm regressions cover signed extremes, division overflow, branch joins, slot reuse, field increments, and long default/virtual calls; field/call results match JVM execution. The cache revision was bumped for the changed layout. Division by zero still traps; the remaining H2/runtime foundations stay open. See [the long-bytecode contract](docs/compatibility.md#long-bytecode-execution). Validation: `mvn -o -pl javaw-bytecode -am -Dtest='com.example.javaw.bytecode.*Test' -Dsurefire.failIfNoSpecifiedTests=false test` (29 tests passed).

Core float/double bytecode now executes with typed locals/stack values, numeric constants/fields, IEEE arithmetic, NaN-aware comparisons, and all primitive numeric casts. Saturating casts implement Java overflow/NaN behavior; tests cover signed zero, infinities, subnormals, joins, slot reuse, and mixed-width calls. JVM comparisons verify inherited fields/default dispatch and a long-to-float double-rounding edge case. This step introduced cache revision `numeric-typed-v4`; floating remainder support and its newer revision are recorded below. The remaining H2/runtime foundations stay open. See [the floating-point contract](docs/compatibility.md#floating-point-bytecode-execution). Validation: `mvn -o -pl javaw-bytecode -am -Dtest='com.example.javaw.bytecode.*Test' -Dsurefire.failIfNoSpecifiedTests=false test` (32 tests passed).

Floating remainder bytecode (`FREM`/`DREM`) now uses the same portable Wasm helper as the source backend, emitted once per linked module without a host math import. JVM comparisons cover 2,848 raw-bit vectors, including signed zero, NaNs, infinities, subnormals, and extreme exponent differences; regressions also execute conditional joins, nested remainders, and linked JAR calls. The cache revision is now `numeric-remainder-v5`. Full H2/Spring/Hibernate runtime support remains open. Validation: `mvn -o -pl javaw-bytecode -am -Dtest='NumericConformanceTest,com.example.javaw.bytecode.*Test' -Dsurefire.failIfNoSpecifiedTests=false test` passed (34 bytecode tests plus source numeric conformance tests).

Bytecode `CHECKCAST` and `INSTANCEOF` now test linked class/interface hierarchies against module-local runtime class IDs, including inherited interfaces and targets without concrete implementations. Null casts preserve null; `instanceof null` returns false. Successful casts preserve the reference, and failed casts trap until bytecode exception mapping is implemented. Array targets, unlinked/inaccessible targets, and incomplete/cyclic hierarchies receive admission diagnostics. The cache revision is now `type-tests-v6`. Full H2/Spring/Hibernate runtime support remains open. Validation: `mvn -o -pl javaw-bytecode -am -Dtest='com.example.javaw.bytecode.*Test' -Dsurefire.failIfNoSpecifiedTests=false test` (36 tests passed).

Bytecode field accesses now resolve declarations by name and descriptor through superinterfaces and superclasses, including inherited static fields backed by the declaring class’s global. Admission checks symbolic-owner accessibility, public/package/protected/private nest access, static/instance opcode kind, and final-write location. Cross-package protected instance reads/writes guard receiver class IDs; excluded receivers trap. Tests execute inherited long static storage, interface-first field resolution, hidden fields, private nestmates, and constructor final writes. The cache revision is now `field-linkage-v7`; full JVM verification, class initialization, and catchable linkage exceptions remain open. Validation: `mvn -o -pl javaw-bytecode -am -Dtest='com.example.javaw.bytecode.*Test' -Dsurefire.failIfNoSpecifiedTests=false test` (39 tests passed).

Bytecode `INVOKESTATIC` now resolves inherited class methods to their declaring Wasm function, preserving descriptor matching and static hiding. Shared admission/lowering checks enforce symbolic-owner accessibility, public/package/protected/private nest access, static declaration kind, and class/interface reference kind; interface static methods remain callable only through their declaring interface. Tests compare JVM results for inherited mixed-width calls, hiding, protected/package access, direct interface calls, and nested argument side effects, and diagnose invalid targets and missing/cyclic lookup paths. The cache revision is now `static-linkage-v8`. Automatic class initialization, general special-call resolution, exceptions, and the full H2 runtime remain open. Validation: `mvn -o -pl javaw-bytecode -am -Dtest='com.example.javaw.bytecode.*Test' -Dsurefire.failIfNoSpecifiedTests=false test` (41 tests passed).

Class bytecode `INVOKESPECIAL` now shares declaration resolution between admission and lowering: constructors require an exact declaration, inherited methods/defaults resolve through superclass hierarchies, and ACC_SUPER calls select from the direct superclass even when the symbolic reference names a grandparent. Owner/member access checks reject invalid calls before WAT generation. JVM comparisons cover superclass selection, inherited mixed-width calls, and defaults inherited by a superclass; existing private/nestmate constructor and Java 8 private-helper regressions remain passing. The cache revision is now `special-linkage-v9`. Full receiver/uninitialized-object verification, class initialization, exceptions, and the full H2 runtime remain open; interface-special caller checks follow below. Validation: `mvn -o -pl javaw-bytecode -am -Dtest='com.example.javaw.bytecode.*Test' -Dsurefire.failIfNoSpecifiedTests=false test` (43 tests passed).

Interface bytecode `INVOKESPECIAL` now requires an accessible current-interface or direct-superinterface owner and a complete acyclic target hierarchy. Indirect/unrelated owners, wrong reference kinds, initializer names, and unsupported targets receive call-site diagnostics. JVM comparisons execute inherited defaults with mixed numeric arguments, void/reference results, same-interface private helpers, and validated private nest calls through a direct superinterface. The cache revision is now `interface-special-v10`. Full receiver verification, class initialization, exception mapping, and the full H2 runtime remain open. Validation: `mvn -o -pl javaw-bytecode -am -Dtest='com.example.javaw.bytecode.*Test' -Dsurefire.failIfNoSpecifiedTests=false test` (46 tests passed).

Bytecode symbolic-owner accessibility now uses one public/package check across allocation, fields, casts/type tests, and every invocation path. `NEW` and virtual/interface calls reject inaccessible owners; dynamic calls also reject mismatched class/interface reference flags and initializer names. JVM comparisons verify same-package access to hidden types and inherited public methods/defaults called through accessible symbolic owners, including resolution without javac’s accessibility bridge. The cache revision is now `class-access-v11`. Full receiver/uninitialized-object verification, class initialization, exception mapping, and the full H2 runtime remain open; hierarchy declaration checks follow below. Validation: `mvn -o -pl javaw-bytecode -am -Dtest='com.example.javaw.bytecode.*Test' -Dsurefire.failIfNoSpecifiedTests=false test` (49 tests passed).

Bytecode hierarchy declarations are now validated before method lowering, including classes with no executable methods. Checks reject missing/cyclic parents, wrong class/interface parent kinds, invalid interface superclasses, duplicate direct interfaces, final superclasses, and inaccessible parents. Access is checked from each declaring child, preserving legal public parents with hidden same-package ancestors. Executable regressions cover shared interface diamonds, abstract parents, final leaves, and reversed JAR order; declaration diagnostics report the offending class and available source file. The cache revision is now `hierarchy-v12`. Complete override/reference/uninitialized-object/module verification, class initialization, exceptions, and the full H2 runtime remain open. Validation: `mvn -o -pl javaw-bytecode -am -Dtest='com.example.javaw.bytecode.*Test' -Dsurefire.failIfNoSpecifiedTests=false test` (52 tests passed).

Bytecode non-private instance overrides are now checked before lowering, even when no virtual call reaches the method. Validation reuses dispatch’s package-aware override slots to reject final-method overrides and public/protected visibility reductions, including intermediate widening bridges. JVM comparisons preserve legal same-named methods in distinct package-private/private slots and final leaf dispatch in both JAR orders. The cache revision is now `overrides-v13`. Complete method/access-flag/interface/reference verification, class initialization, exception mapping, and the full H2 runtime remain open. Validation: `mvn -o -pl javaw-bytecode -am -Dtest='com.example.javaw.bytecode.*Test' -Dsurefire.failIfNoSpecifiedTests=false test` (54 tests passed).

The actual H2/Spring/Hibernate task in [Todo.md](Todo.md#12-spring-api-subset-on-webassembly) now includes a consolidated remaining roadmap: target/version pinning and dependency audit; JVM verification, arrays, exceptions and initialization; runtime/JDK/reflection/dynamic linkage; host facilities, memory and concurrency; and real H2, Hibernate, Spring integration with an end-to-end compatibility gate. These are planned workstreams, not newly implemented runtime features. Library-specific blockers will be refined from the pinned dependency audit and execution results.

The [actual-library runtime target](docs/runtime-target.md) now pins Spring Boot/Data JPA 3.2.4, Spring Framework 6.1.5, Hibernate 6.4.4.Final and H2 2.2.224, with an explicitly managed, SHA-256-verified 63-JAR closure. `runtime-target` runs two passing JVM reference tests against actual H2/JDBC and Spring/Hibernate/JPA, including transactions, dirty checking and file reopen. Validation: `python3 runtime-target/verify-lock.py` and `mvn -o -pl runtime-target test`. This completes target pinning; actual-library Wasm execution and the remaining prerequisites are still open.

The [pinned dependency audit](docs/runtime-audit.md) now inventories every Java-17-selected class in the 63-JAR runtime target: 26,625 classes, 734 resources and 28 service providers. `python3 runtime-target/audit.py --check-baseline` verifies the lock and produces opcode, member, bootstrap, native/platform, metadata/resource and external-type reports, plus an explicitly limited static class-reference estimate. The audit identifies concrete remaining blockers, including legacy Byte Buddy subroutines; it does not claim dynamic reachability or Wasm framework execution. Validation: 56 bytecode tests passed, including deterministic multi-release audit fixtures; the full target audit completed successfully.

Legacy bytecode JSR/RET is now inlined before admission and frame analysis for class, linked-JAR, and direct ClassNode inputs. JVM/Wasm comparisons execute shared/nested subroutines, wide RET slots, and preserved long stack values; recursive subroutines are diagnosed, and handler/source metadata survives normalization. The pinned audit verifies all 61 affected Byte Buddy methods and reports 162 JSR/77 RET occurrences separately as normalized instructions; remaining library blockers stay explicit. Validation: 59 bytecode tests pass and the full 63-JAR audit completes. Cache revision: `subroutines-v14`.

Bytecode frame inference now uses a reference-aware verifier backed only by linked class metadata. It rejects invalid primitive/local/reference uses, unrelated class receivers, incompatible field values and arguments/results, and invalid class-reference merges; null/array descriptor assignability is modelled without compiler-host class loading. Existing JVM/Wasm regressions still pass, including legacy subroutine normalization. Explicit stack-map conformance, constructor/uninitialized-object verification, and array execution remain open; this is the inferred-type portion of the verifier prerequisite. Cache revision: `reference-inference-v15`.

Bytecode declaration validation now rejects duplicate name/descriptor members, conflicting visibility, incompatible final/abstract and final/volatile flags, invalid interface declarations/version rules, invalid initializer signatures/flags, and malformed descriptors before lowering, including unused linked declarations. Static globals use the complete encoded owner/name/descriptor identity; a JVM/Wasm comparison verifies independent same-named int/long fields. Validation: 66 bytecode tests pass and the pinned 63-JAR audit baseline is refreshed. Cache revision: `declarations-v16`. Full declaration/linkage validation and the other actual H2/Spring/Hibernate prerequisites remain open.

Bytecode hierarchy admission now enforces sealed superclass/superinterface permits lists and rejects final classes with `PermittedSubclasses`, including unused linked declarations. Attribute version checks accept Java 17+ and Java 15/16 preview metadata. JVM/Wasm comparisons execute sealed interfaces, sealed abstract parents, non-sealed branches and final descendants in either JAR order; unlinked permit-list entries do not force loading. Validation: 69 bytecode tests pass and the 63-JAR audit baseline changes only its compiler revision (`sealed-hierarchy-v17`). Module/package placement and the remaining declaration/linkage and actual H2/Spring/Hibernate runtime work remain open. Repository guidance is recorded in [AGENTS.md](AGENTS.md).

Bytecode member declaration checks now reject malformed field/method names and signatures exceeding the JVM's 255 parameter slots before lowering, including unused linked declarations. Counting includes two slots for long/double, one for arrays, and one for an instance receiver. JVM/Wasm comparisons execute boundary-width static and instance calls; admission tests preserve legal angle brackets in field names. Validation: 72 bytecode tests pass; the pinned 63-JAR audit inventory is unchanged apart from cache revision `member-names-slots-v18`. The larger declaration/linkage milestone and actual H2/Spring/Hibernate runtime remain open.

Bytecode declaration admission now checks `ConstantValue` attribute types against field descriptors, including unused linked declarations and instance fields. Malformed attributes receive a declaration diagnostic before WAT generation; correctly typed string constants retain the explicit unsupported-runtime diagnostic. JVM/Wasm regressions cover numeric constants (integer limits, signed zero, NaN and infinities) and confirm that instance constants leave allocation defaults unchanged. Validation: 75 bytecode tests pass; the 63-JAR dependency audit changes only its compiler revision (`constant-values-v19`). Automatic class initialization and the broader declaration/linkage and actual-library runtime milestones remain open.

The bytecode declaration/linkage roadmap item is now complete for the [admitted closed-world backend](docs/compatibility.md#closed-world-declaration-and-linkage-contract). Admission checks all linked declarations, hierarchies and overrides even without call sites; resolves descriptor types from linked metadata; checks interface/nest/version/Code rules, final Object methods and unnamed-module sealed packages; and rejects duplicate class identities and invocation-kind mismatches. JVM-legal inherited static/instance slots remain distinct. Special calls validate interface-reference versions, caller-compatible receivers and the exact Object-constructor intrinsic. Encoded method/global/layout identities and escaped exports preserve unusual legal JVM names, including punctuation and private surrogate names; exports requiring invalid UTF-8 receive diagnostics. Validation: 90 bytecode tests pass, preserving package/nest/override regressions; `python3 runtime-target/audit.py --check-baseline` passes with unchanged 63-JAR inventory counts and cache revision `declaration-linkage-v20`. Stack-map and constructor/uninitialized-object verification, named modules/loaders, initialization, exceptions and actual H2/Spring/Hibernate execution remain open.


Stack-map conformance is now enforced for admitted initialized numeric/reference bytecode. Compressed and expanded frames constrain incoming values and subsequent verification; wider reference declarations and discarded locals cannot be bypassed by inferred types. Modern branch/switch targets and code after non-fallthrough instructions require frames, and framed unreachable code is checked. Category-two locals and stack-word limits, linked verification types, and malformed frame shapes receive diagnostics. Validation: 101 bytecode tests, including JVM acceptance/rejection comparisons and existing JVM/Wasm execution; the 63-JAR audit inventory is unchanged. Cache revision: `stack-maps-v21`. At this revision, constructor/uninitialized-object flows were still open; their completion follows below. The reference-verification milestone remains open for unsupported array operations; TOP operand-stack support follows below. See the [stack-map contract](docs/compatibility.md#stack-map-conformance).


Constructor/uninitialized-object verification is now complete for admitted handler-free bytecode. The verifier tracks uninitialized-this and each NEW allocation through local/stack aliases and stack maps, initializes all aliases after legal this/super/exact-class constructor calls, and requires initialization on every constructor return path even if local zero was overwritten. It rejects premature object use/escape, repeated or wrong constructor calls, incompatible allocation identities, forged frame tokens, and cross-package protected constructor calls on NEW receivers. JVM-legal pre-super writes to the current class's fields and reference null/identity comparisons remain supported. Validation: 114 bytecode tests pass, including JVM verifier comparisons and JVM/Wasm execution of constructor chaining, nested allocations and pre-super field writes. The 63-JAR audit inventory is unchanged; cache revision: `constructor-flows-v22`. TOP operand-stack support follows below. Exception-handler flows, array execution and catchable linkage failures remain open; raw exported constructors assume valid module-local host storage. See the [constructor contract](docs/compatibility.md#constructor-and-uninitialized-object-verification).


TOP operand-stack declarations are now supported, including erasing long/double values into two unusable words. Wasm stack locals use JVM word positions so live values above erased words retain their values across joins. Verification permits work above TOP and frame discard on return, and rejects typed reads, stack manipulation touching TOP, invalid refinement/partial erasure, and conflicting inferred stack types. Validation: 126 bytecode tests pass, including JVM verifier and JVM/Wasm comparisons; the 63-JAR audit inventory is unchanged. Cache revision: `top-stack-words-v23`. Array operations and exception-handler edges remain open parts of the verifier/runtime roadmap. See the [TOP stack contract](docs/compatibility.md#top-operand-stack-words).


Revalidated the completed legacy JSR/RET normalization task (implementation commit `3f89072`, introduced in `subroutines-v14`) against the current `top-stack-words-v23` verifier. All 126 bytecode tests pass, including shared/nested subroutine JVM/Wasm comparisons, RET local 300, long-stack preservation, handler/source metadata preservation and recursion diagnostics. The unchanged 63-JAR audit baseline passes and confirms normalization of all 61 affected Byte Buddy 1.14.11 methods (162 JSR / 77 RET occurrences). No compiler changes were needed; exception handling and other Byte Buddy runtime blockers remain open.

Primitive bytecode arrays now execute for all eight primitive types, with packed storage, zero initialization, Java narrow conversions, length, checked element access and runtime casts/type tests. Null, bounds, negative-size and allocation failures trap until catchable bytecode exceptions are implemented. Validation: 136 bytecode tests pass, including JVM/Wasm comparisons and malformed-bytecode verification; the reviewed 63-JAR audit removes the 16 newly admitted opcodes and preserves other inventory counts. Cache revision: `primitive-arrays-v24`. Reference arrays, multidimensional allocation and clone/arraycopy remain open, along with actual H2/Spring/Hibernate execution. See [the primitive-array contract](docs/compatibility.md#primitive-bytecode-arrays).

One-dimensional bytecode reference arrays now support linked class/interface components and Object, with allocation, zero-initialized references, loads/stores, covariant casts/type tests and runtime store checks against the actual array component. Object[] can hold primitive and reference arrays. Invalid stores, nulls, bounds and size failures currently trap. Validation: 140 bytecode tests pass, including JVM/Wasm comparisons in both JAR orders and malformed JVM fixtures; the reviewed 63-JAR audit removes the three newly admitted opcodes with other inventory counts unchanged. Cache revision: `reference-arrays-v25`. Nested array components, multidimensional allocation, clone/arraycopy and catchable exceptions remain open. See [the reference-array contract](docs/compatibility.md#reference-bytecode-arrays).

Nested bytecode arrays and full/partial MULTIANEWARRAY allocation now execute with recursive covariance, runtime store checks, distinct zeroed child arrays and negative-size checks across every supplied dimension before allocation, including dimensions after a zero. Validation: 144 bytecode tests pass, covering JVM/Wasm comparisons, malformed allocations and the rank-255 boundary; the reviewed 63-JAR audit removes the three MULTIANEWARRAY occurrences with other inventory counts unchanged. Cache revision: `multidimensional-arrays-v26`. Clone/arraycopy and catchable exceptions remain open; actual H2/Spring/Hibernate execution is still pending. See [the nested-array contract](docs/compatibility.md#nested-and-multidimensional-bytecode-arrays).

Bytecode arrays now support shallow clone and exact System.arraycopy calls: primitive/reference/nested arrays, independent clone storage, shared referenced elements, overlap-safe transfers, primitive-kind checks and checked reference copies that preserve the successful prefix on failure. Explicitly linked System definitions retain normal linkage. Validation: 150 bytecode tests pass, including JVM/Wasm comparisons and malformed call/operand rejection; the reviewed 63-JAR audit inventory is unchanged. Cache revision: `array-transfers-v27`. Failures currently trap; catchable array exceptions, calls through Object.clone and other JDK array utilities remain open. See [the array-transfer contract](docs/compatibility.md#bytecode-array-clone-and-arraycopy).

The `handler-verification-v28` prerequisite added exception-table and exceptional-frame verification. It checks protected ranges/targets, linked Throwable catch hierarchies, required handler stack maps and input-local merges, replaces the operand stack with one exception, and invalidates failed NEW-constructor aliases. Validation: 158 bytecode tests pass, including JVM comparisons for catch/multi-catch/finally/rethrow shapes and malformed handler metadata/frames; the reviewed 63-JAR audit inventory is unchanged. Cache revision: `handler-verification-v28`. Explicit exception execution follows below; catchable implicit failures and handlers covering this/super initialization remain open. See [the handler contract](docs/compatibility.md#exception-handler-verification-prerequisite).

Explicit bytecode exceptions now execute through a module-local Wasm tag: ATHROW, ordered typed/catch-all handlers, identity-preserving rethrow, finally flow and cross-method unwinding, including interface calls and failed NEW constructors. Validation: 161 bytecode tests pass with JVM/Wasm comparisons; the reviewed 63-JAR audit removes ATHROW from unsupported opcodes with other inventory counts unchanged. Cache revision: `explicit-exceptions-v29`. This subset uses explicitly linked Throwable definitions and requires Wasm exception handling (`wat2wasm --enable-exceptions`). Null throws, implicit failures and host errors bypass these handlers; full Throwable APIs, suppression/try-with-resources and handlers across this/super initialization remain open. See [the exception runtime contract](docs/compatibility.md#explicit-bytecode-exception-execution).


Integer bytecode division/remainder now throws catchable exceptions when the module explicitly links a public concrete ArithmeticException with a public bytecode no-arg constructor and Throwable ancestry. The runtime allocates fresh zeroed objects, invokes their constructors, and preserves typed handlers, finally and cross-method unwinding. Modules without that definition retain traps. Validation: 163 bytecode tests pass, including JVM/Wasm comparisons for all four int/long operations, overflow, object identity/constructor effects and invalid runtime definitions. The reviewed 63-JAR audit changes only cache revision `arithmetic-exceptions-v30`. Null, cast, array, allocation and linkage exception mappings, full Throwable APIs and actual H2/Spring/Hibernate execution remain open. See [the arithmetic exception contract](docs/compatibility.md#implicit-bytecode-arithmetic-exceptions).


Bytecode null ATHROW, null instance field/call receivers and failed CHECKCAST operations now throw catchable Java exceptions when NullPointerException/ClassCastException runtime definitions are explicitly linked. Each mapping is independent and uses fresh zeroed objects plus the linked no-arg constructor. JVM/Wasm comparisons preserve argument evaluation, null casts, instanceof, class/interface/array type tests, typed catches, finally and cross-method unwinding in both JAR orders. Validation: 166 bytecode tests pass, including legacy null INVOKESPECIAL receivers, malformed runtime definitions and allocator failures. The reviewed 63-JAR audit changes only cache revision `reference-exceptions-v31`. Array access/arraycopy null checks, bounds/store/size failures, allocation/linkage mappings, full Throwable APIs and actual H2/Spring/Hibernate execution remain open. See [the reference exception contract](docs/compatibility.md#implicit-bytecode-reference-exceptions).


Array and allocation exception mappings now execute with explicitly linked runtime definitions: array null/bounds/store/negative-size failures, checked arraycopy prefixes, multidimensional checks, allocation overflow and null allocator results. An emergency OutOfMemoryError resides in a reserved memory prefix, exported as `__exception_heap_base`; hosts must allocate beyond it. Validation: 167 bytecode tests and the reviewed 63-JAR audit pass (revision `array-exceptions-v32`, inventory unchanged). Throwable APIs, constructor handlers and runtime linkage behavior continue under the exception milestone. See [the failure contract](docs/compatibility.md#bytecode-array-and-allocation-exceptions).


Constructor exception handlers now admit JVM-legal protected this/super calls. Failed receiver aliases become unusable while the pending initialization flag prevents handlers from returning an uninitialized object. JVM/Wasm tests execute a failed super constructor with aliased this and rethrow in both JAR orders; verifier comparisons reject invalid handler returns and alias reuse. All 167 bytecode tests and the reviewed 63-JAR audit pass; revision `constructor-handlers-v33`.


The explicit `BytecodeCompiler.compileJarWithExceptionRuntime` and `compileClassWithExceptionRuntime` APIs now supply an isolated Throwable runtime: message/cause constructors, initCause, ordered suppression, defensive copies, disabled suppression/traces, stack capture/refill/set and StackTraceElement metadata. JVM/Wasm comparisons execute javac try-with-resources with body/close/constructor failures and multiple/null resources. A UTF-16 String subset serves messages and trace metadata; public overloads use descriptor-qualified exports. A bounded 256-frame shadow stack captures application frames without allocating on call/unwind and throws an emergency StackOverflowError at its limit. Validation: 168 bytecode tests and the reviewed 63-JAR audit pass (revision `throwable-runtime-v34`, inventory unchanged). Runtime linkage remains the last exception-milestone part; complete String/IO/serialization, threads and class initialization remain separate. See [the runtime API and host contract](docs/compatibility.md#bundled-bytecode-exception-runtime).
