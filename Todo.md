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

---

## 2. Array Support & Linear Memory Layout

- [x] **Primitive Array Allocation & Access (`int[]`, `double[]`, `boolean[]`)**
  - Linear memory layout: `[length: i32, element_0, element_1, ...]`.
  - Array instantiation (`new int[n]`, `new int[]{...}`), element loading (`i32.load`, `f64.load`), element storing (`i32.store`, `f64.store`).
- [x] **Array Length Property (`arr.length`)**
  - Read array length directly from array header in linear memory.

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

## 6. Non-Browser Standards (WASI & Concurrency)

- [x] **WASI Target (`wasi_snapshot_preview1`)**
  - Standard I/O output: `System.out.println`, `System.out.print`, `System.err.println`, `System.err.print`, and `System.exit(code)` lowered to WASI `fd_write` and `proc_exit`.
  - Typed output support for strings, integers, floating point (`f64`), booleans, and characters with automated memory buffering.
  - Strongly-typed `WASI` facade (`com.example.javaw.wasi.WASI`) with `print`, `println`, `eprint`, `eprintln`, `exit`, `fdWrite`, and `procExit`.
  - Built-in WASI runtime layer in `javaw-runtime.js` supporting `fd_write`, `fd_read`, `proc_exit`, `args_sizes_get`, `args_get`, `environ_sizes_get`, `environ_get`, `clock_time_get`, and `random_get`.
- [x] **WebAssembly Threads & Shared Memory**
  - WebAssembly shared memory declaration `(memory (export "memory") 2 20 shared)` for multithreaded runtime support.
  - Strongly-typed `AtomicInteger` facade (`com.example.javaw.concurrent.AtomicInteger`) supporting `get`, `set`, `getAndSet`, `compareAndSet`, `getAndAdd`, `addAndGet`, `incrementAndGet`, `decrementAndGet`, and bitwise atomic operations (`i32.atomic.load`, `i32.atomic.store`, `i32.atomic.rmw.*`).
  - Low-level array atomic primitives (`com.example.javaw.concurrent.Atomics`) supporting `load`, `store`, `add`, `sub`, `and`, `or`, `xor`, `exchange`, `compareExchange`, `wait` (`memory.atomic.wait32`), and `notify` (`memory.atomic.notify`).
  - Web Worker and thread management facade (`com.example.javaw.concurrent.WasmThread`) supporting `spawn(entryPoint, arg)`, `sleep(ms)`, `currentThreadId()`, and `hardwareConcurrency()`.
  - Host runtime bridge in `javaw-runtime.js` for thread spawning, sleep via atomics, and concurrency detection.

---

## 7. Tooling & Optimization Pipeline

- [x] **Binaryen Optimization Pass (`wasm-opt`)**
  - Automated dead-code elimination, inlining, and binary size reduction via integrated `wasm-opt -O3 --enable-threads` pipeline.
- [ ] **Source Maps & DWARF Debugging**
  - Generate source maps mapping WebAssembly bytecode to Java source lines.
- [x] **Multi-File Compilation & Project Linking**
  - Compile multiple Java source files or entire directories into a unified WebAssembly module.
