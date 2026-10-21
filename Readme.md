# javaw (Java to WebAssembly Compiler)

A lightweight, high-performance Java-to-WebAssembly compiler that parses Java source files (`.java`) and compiles them into standard WebAssembly Text format (`.wat`) and binary modules (`.wasm`). `javaw` includes full support for browser Document Object Model (DOM) manipulation, static string pooling, and bi-directional event handling.

---

## Overview

`javaw` provides an efficient pipeline to compile Java code into WebAssembly for native browser execution. Using [JavaParser](https://javaparser.org/) for AST traversal, the compiler analyzes Java classes, methods, expressions, and statements, lowering them into WebAssembly stack instructions without requiring intermediate JVM bytecode compilation.

With built-in linear memory allocation, static string constant pooling, and a zero-dependency host runtime bridge (`javaw-runtime.js`), developers can author interactive web applications directly in Java.

---

## Features

- **AST-Based Lowering & Modular Compiler Architecture**: Directly parses Java source files using JavaParser and translates statements and expressions to WebAssembly instructions, separated into dedicated functional modules (`JavacToWasm`, `WasmGeneratorVisitor`, `WasmRuntime`, `DomHelper`, `TypeMapper`).
- **WebAssembly Linear Memory & Dynamic Heap**:
  - Automatically exports linear memory `(memory (export "memory") 2)`.
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
- **Strongly-Typed Java DOM Facade**:
  - `com.example.javaw.dom.DOM`: Static methods such as `getElementById`, `createElement`, and `getBody`.
  - `com.example.javaw.dom.Element`: Instance methods including `setInnerText`, `setAttribute`, `appendChild`, and `addEventListener`.
- **Automatic Host Import Generation**: Automatically detects DOM method invocations and emits corresponding `(import "dom" ...)` declarations.
- **Lightweight Browser Runtime Bridge (`javaw-runtime.js`)**:
  - Minimal footprint (< 15KB) with zero external dependencies.
  - 32-bit integer handle lookup table (`Map<number, HTMLElement>`) for safe referencing of DOM elements from WebAssembly.
  - Zero-copy UTF-8 text decoding from WebAssembly linear memory (`instance.exports.memory.buffer`).
  - Bi-directional event dispatching mapping browser DOM events to exported WebAssembly handler functions.
- **Arithmetic & Primitive Types**:
  - Primitive and return type mapping (`void`, `int`, `boolean`, `char`, `long`, `float`, `double`).
  - Arithmetic and bitwise binary operators (`+`, `-`, `*`, `/`, `%`, `&`, `|`, `^`, `<<`, `>>`, `>>>`).
  - Unary operators (negation, logical/bitwise complement, prefix/postfix increments and decrements).
  - Local variables and static global field variables.
- **Binary WASM Assembly**: Automatically invokes `wat2wasm` (when present on `PATH`) to produce deployable `.wasm` binaries.
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
│   │   │   ├── WasmRuntime.java       # WebAssembly linear memory helper routines (strings, arrays)
│   │   │   ├── DomHelper.java         # DOM API import collector and emitter
│   │   │   ├── TypeMapper.java        # Java to WebAssembly type mapping
│   │   │   ├── StringConstant.java    # String literal pool model
│   │   │   ├── GlobalVar.java         # Global field variable model
│   │   │   ├── LoopContext.java       # Loop label and jump context tracker
│   │   │   ├── dom
│   │   │   │   ├── DOM.java           # Java DOM API facade
│   │   │   │   └── Element.java       # Java DOM Element facade
│   │   │   └── util
│   │   │       └── StringBuilder.java # Dynamic string builder facade
│   │   └── resources
│   │       └── javaw-runtime.js       # JavaScript WebAssembly host runtime bridge
│   └── test
│       ├── java/com/example/javaw
│       │   ├── App.java               # Java DOM & interactive event demo application
│       │   ├── AppBrowserTest.java    # Playwright headless browser integration test suite
│       │   ├── JavacToWasmTest.java   # JUnit 5 compiler unit test suite
│       │   └── TestClass.java         # Comprehensive Java test suite (loops, arrays, strings)
│       └── node-tests
│           ├── package.json           # Node.js test configuration
│           └── test.js                # End-to-end WebAssembly execution & DOM test suite
```

---

## Prerequisites

- **Java Development Kit (JDK)**: Version 17 or higher
- **Apache Maven**: Version 3.6 or higher
- **WABT (`wat2wasm`)** (optional): For compiling `.wat` text to `.wasm` binary files directly from the CLI
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

Run the compiler against any Java source file:

```bash
java -jar target/javaw-1.0-SNAPSHOT.jar <path-to-java-file.java>
```

This generates:
- `<filename>.wat`: WebAssembly Text representation.
- `<filename>.wasm`: Compiled binary WebAssembly (if `wat2wasm` is available on `PATH`).

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

Emitted WAT output:
```wat
(module
  (memory (export "memory") 1)
  ;; Class/Interface: TestClass
  (func $addNumbers (export "addNumbers") (param $a i32) (param $b i32) (result i32)
    local.get $a
    local.get $b
    i32.add
  )
  (func $getPi (export "getPi") (result f64)
    f64.const 3.14159
  )
)
```

---

#### Example B: Interactive Browser DOM Application (`App.java`)

```java
package com.example.javaw;

import com.example.javaw.dom.DOM;
import com.example.javaw.dom.Element;

public class App {
    public static int counter = 0;

    public void start() {
        Element app = DOM.getElementById("app");

        Element heading = DOM.createElement("h1");
        heading.setAttribute("class", "app-title");
        heading.setInnerText("Java to WebAssembly DOM App");
        app.appendChild(heading);

        Element status = DOM.createElement("p");
        status.setAttribute("id", "status-text");
        status.setInnerText("Count: 0");
        app.appendChild(status);

        Element btn = DOM.createElement("button");
        btn.setAttribute("id", "btn-increment");
        btn.setInnerText("Increment Counter");
        btn.addEventListener("click", "increment");
        app.appendChild(btn);
    }

    public void increment() {
        counter = counter + 1;
        Element status = DOM.getElementById("status-text");
        if (counter == 1) {
            status.setInnerText("Count: 1 click");
        } else if (counter == 2) {
            status.setInnerText("Count: 2 clicks");
        } else {
            status.setInnerText("Count: multiple clicks");
        }
    }
}
```

---

#### Example C: Arrays, Control Flow & Dynamic Strings

```java
package com.example.javaw;

import com.example.javaw.util.StringBuilder;

public class DataProcessor {
    public int sumArray(int[] numbers) {
        int sum = 0;
        for (int n : numbers) {
            sum += n;
        }
        return sum;
    }

    public String buildReport(String category, int[] scores) {
        StringBuilder sb = new StringBuilder("Category: ");
        sb.append(category);
        sb.append(", Total: ");
        
        int total = 0;
        for (int score : scores) {
            total += score;
        }
        sb.append(total);
        return sb.toString();
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
    <title>Java WASM DOM App</title>
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

Run the Node.js test suite to verify binary execution, string decoding, and interactive DOM mutations:

```bash
npm test --prefix src/test/node-tests
```
