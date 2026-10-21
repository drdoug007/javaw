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
- [ ] **`switch` statements & expressions (`SwitchStmt`)**
  - Lower dense integer switches to WebAssembly `br_table` jump tables and sparse cases to binary search branching.
- [ ] **Enhanced `break` and `continue` with labels**
  - Map labeled break/continue statements to outer WebAssembly block/loop depths.

---

## 2. Array Support & Linear Memory Layout

- [ ] **Primitive Array Allocation & Access (`int[]`, `double[]`, `boolean[]`)**
  - Linear memory layout: `[length: i32, element_0, element_1, ...]`.
  - Array instantiation (`new int[n]`), element loading (`i32.load`), element storing (`i32.store`).
  - Automatic runtime bounds checking.
- [ ] **Array Length Property (`arr.length`)**
  - Read array length directly from array header in linear memory.

---

## 3. Dynamic Strings & String Manipulation

- [ ] **Dynamic String Concatenation (`+` operator with strings & primitives)**
  - Implement runtime string concatenation and heap buffer allocation.
- [ ] **Dynamic `StringBuilder` Facade**
  - Support append operations and dynamic string building.
- [ ] **Core String Methods**
  - `length()`, `charAt(index)`, `substring(start, end)`, `equals(other)`.

---

## 4. Object-Oriented Programming (OOP) & Memory Allocation

- [ ] **Dynamic Heap Allocation (`new MyClass(...)`)**
  - Bump/free-list linear memory allocator starting at dynamic heap offset (`0x2000`).
  - Struct byte layout calculation, object headers, and `$this` pointer passing to instance methods.
- [ ] **Virtual Method Dispatch (Polymorphism)**
  - Dynamic dispatch via WebAssembly function tables (`table`) and `call_indirect`.
- [ ] **WasmGC Target Proposal**
  - Emit native WebAssembly Garbage Collection types (`(type $Class (struct ...))`, `struct.new`, `struct.get`).

---

## 5. Extended Web & Browser Platform APIs

- [ ] **DOM Facade Extensions**
  - `DOM.querySelector(selector)` / `DOM.querySelectorAll(selector)`.
  - `element.remove()`, `element.removeChild(child)`, `element.getParent()`.
  - `element.getValue()`, `element.setValue(val)`, `element.isChecked()`.
  - `element.getClassList().add(...)` / `remove(...)`.
- [ ] **HTML5 Canvas 2D & WebGL Graphics Facades**
  - 2D context drawing primitives (`fillRect`, `beginPath`, `arc`, `drawImage`).
- [ ] **Asynchronous Browser Interop (Promises & Timers)**
  - `fetch(url)`, `setTimeout`, `setInterval`, `requestAnimationFrame`.
- [ ] **Generic JavaScript Dynamic Interop (`JSObject`)**
  - Dynamic evaluation and method calling against arbitrary JavaScript objects.

---

## 6. Non-Browser Standards (WASI & Concurrency)

- [ ] **WASI Target (`wasi_snapshot_preview1`)**
  - Support `System.out.println`, file I/O, and CLI arguments for serverless runtimes.
- [ ] **WebAssembly Threads & Shared Memory**
  - Multi-threaded execution and atomic operations (`atomic.rmw.*`).

---

## 7. Tooling & Optimization Pipeline

- [ ] **Binaryen Optimization Pass (`wasm-opt`)**
  - Automated dead-code elimination, inlining, and binary size reduction.
- [ ] **Source Maps & DWARF Debugging**
  - Generate source maps mapping WebAssembly bytecode to Java source lines.
- [ ] **Multi-File Compilation & Project Linking**
  - Compile multi-file Java packages into single WebAssembly modules.
