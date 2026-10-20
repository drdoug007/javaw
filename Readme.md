# javaw (Java to WebAssembly Compiler)

A lightweight Java-to-WebAssembly translator that parses Java source files (`.java`) and compiles them into WebAssembly Text format (`.wat`).

---

## Overview

`javaw` provides an efficient utility to convert Java source code structures into WebAssembly. Utilizing [JavaParser](https://javaparser.org/) for AST (Abstract Syntax Tree) parsing, the tool extracts class declarations and method signatures, translating them into standard WebAssembly Text (`.wat`) modules.

## Features

- **AST-Based Parsing**: Parses Java source code using JavaParser without requiring bytecode compilation first.
- **Type Mapping**: Automatically maps Java primitive and return types to WebAssembly types:
  - `void` &rarr; No return value
  - `int` &rarr; `i32`
  - `double` &rarr; `f64`
  - Default fallback &rarr; `i32`
- **Automatic Output Generation**: Generates a `.wat` file in the same directory as the source Java file with matching base filename.
- **Standalone Fat JAR**: Packaged via Maven Shade plugin for standalone command-line execution.

---

## Project Structure

```
├── pom.xml                               # Maven project configuration and shade packaging
├── src
│   ├── main
│   │   └── java/com/example/javaw
│   │       └── JavacToWasm.java          # Core CLI application and AST visitor implementation
│   └── test
│       ├── java/com/example/javaw
│       │   └── TestClass.java            # Sample Java input for testing translation
│       └── node-tests
│           ├── package.json              # Node.js test configuration
│           └── test.js                   # End-to-end integration test runner
```

---

## Prerequisites

- **Java Development Kit (JDK)**: Version 17 or higher
- **Apache Maven**: Version 3.6 or higher
- **Node.js** (optional): For running integration test suite

---

## Building the Application

Build the standalone executable JAR using Maven:

```bash
mvn clean package
```

The resulting executable JAR will be located at:
```
target/javaw-1.0-SNAPSHOT.jar
```

---

## Usage

Run the compiled JAR and provide the path to a `.java` source file:

```bash
java -jar target/javaw-1.0-SNAPSHOT.jar <path-to-java-file.java>
```

### Example

Given a Java file `TestClass.java`:

```java
package com.example.javaw;

public class TestClass {
    public void sayHello() {
    }

    public int addNumbers(int a, int b) {
        return a + b;
    }

    public double getPi() {
        return 3.14159;
    }
}
```

Run the compiler:

```bash
java -jar target/javaw-1.0-SNAPSHOT.jar src/test/java/com/example/javaw/TestClass.java
```

This generates `src/test/java/com/example/javaw/TestClass.wat`:

```wat
(module
  (func $sayHello )
    ;; Method Body for sayHello
    end
  )
  (func $addNumbers -> i32 
    ;; Method Body for addNumbers
    end
  )
  (func $getPi -> f64 
    ;; Method Body for getPi
    end
  )
  ;; Class/Interface: TestClass
)
```

---

## Testing

### Integration Tests (Node.js)

Run the automated Node.js test script to verify compilation and generated WAT structures:

```bash
cd src/test/node-tests
npm test
```
