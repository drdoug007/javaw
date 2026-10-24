# Experimental native WasmGC target

The roadmap's WasmGC proposal is implemented as a standalone prototype. `JavacToWasm.generateGcWat(String source)` returns WAT from `WasmGcVisitor`; it does not enter the linear-memory visitor or link its runtime. The default compile APIs and CLI retain their existing target. This separation lets the prototype use native references throughout instead of mixing pointers with GC objects.

```java
String wat = JavacToWasm.generateGcWat("""
    class Box {
        int value;
        Box(int value) { this.value = value; }
        public static Box make(int value) { return new Box(value); }
        public int read() { return this.value; }
    }
    """);
java.nio.file.Files.writeString(java.nio.file.Path.of("box.wat"), wat);
```

Assemble with a modern Binaryen (tested with version 130), then execute with a WasmGC-capable runtime (tested with Node 24):

```sh
wasm-as box.wat --all-features -o box.wasm
node -e 'const fs=require("node:fs"); const e=new WebAssembly.Instance(new WebAssembly.Module(fs.readFileSync("box.wasm"))).exports; const box=e["Box.make"](42); console.log(e["Box.read"](box));'
```

The existing WABT assembly pipeline is not selected by this API. Toolchain failures must be treated as failures by the caller; the API only generates WAT.

## Supported prototype contract

- Top-level non-generic ordinary classes with unique names; no inheritance or interfaces.
- Native struct fields with primitive storage or nullable references to linked classes. Types share one recursive group, supporting self/mutually referenced graphs. Fields default to zero/null; integer and boolean literal initializers are supported.
- One constructor per class, or an implicit default constructor. Allocation evaluates arguments, creates a default-initialized struct with `struct.new`, and executes the constructor body against that object.
- Methods exported as `Class.method`; instance exports take an explicit typed receiver. Static methods, parameters, explicit typed locals, returns, and simple assignment statements are supported.
- `new`, field reads/writes (including chained references), integer/boolean literals, parentheses, and int addition/subtraction/multiplication.
- Unsupported AST constructs produce an exception with source line/column and a suggestion to use the default backend. This is not a Java semantic checker; invalid types/arity and return paths can additionally fail assembler validation. Supply valid Java source within the subset.

There are no static fields, overloaded methods/constructors, method-call expressions, arrays, strings, library linkage, branching/loops, exceptions, annotations, synchronization, reflection, generics, or class initialization runtime. Java null exceptions are not implemented: dereferencing a null GC reference traps. Access control and Java numeric narrowing are not enforced by this prototype. Public and nonpublic methods are exported alike for experimentation. These are future compiler work, not claims of general Java compatibility.

## Ownership and evidence

Objects are engine-managed GC references. Wasm locals and fields retain references during execution; host-held opaque GC values retain objects between calls. Hosts pass these values back to typed exports directly. They are not linear-memory addresses and must not be passed to the existing retain/release/heap-stat APIs or Javaw JavaScript wrapper bridge. There is no linear memory or manual collector in these modules.

`WasmGcVisitorTest` assembles and executes constructor arguments, integer fields, reference fields, cycles, nested mutation, arithmetic, and GC values crossing the host boundary. It also checks rejection locations for unsupported constructs. `ManagedHeapTest` separately guards the default allocator. These tests require Binaryen and Node on PATH and fail if assembly/execution fails; they do not skip missing toolchains.
