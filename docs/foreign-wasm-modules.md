# Calling Go and Rust WebAssembly modules from Java

Java compiled by the source compiler (`javaw-core`) can call functions exported by WebAssembly modules built with other toolchains, such as Go or Rust. The host instantiates the other module first and exposes its exports as a JavaScript global. A Java class annotated with `@JS.Import("<global>")` then declares the functions as `native` methods.

The bytecode compiler (`javaw-bytecode`) does not support this yet; see [Bytecode modules](#bytecode-modules).

## How it works

```java
@JS.Import("golib")
class GoLib {
    public static native int add(int a, int b);
    public static native long fib(int n);
}
```

For each `native` method, `javaw-core` emits a host import, `(import "dom" "js_inline_GoLib_add" ...)`. It also records a JavaScript snippet in the `javaw_js_snippets` custom section; here `return golib.add(a, b);`. `javaw-runtime.js` turns each snippet into an import function when `instantiateJavaw` loads the module. So a call goes Java → JS glue → the other module's export.

The host side:

```js
const { WASI } = require('node:wasi');
const wasi = new WASI({ version: 'preview1' });
const go = await WebAssembly.instantiate(goBytes, wasi.getImportObject());
wasi.initialize(go.instance);                 // Go reactor: runs _initialize once
globalThis.golib = go.instance.exports;
globalThis.rustlib = (await WebAssembly.instantiate(rustBytes, {})).instance.exports;
const java = (await instantiateJavaw(javaBytes)).instance.exports;
```

In a browser, use a WASI shim (or a module with no WASI imports) and set the global on `window` before `instantiateJavaw`.

## Building the other modules

- **Go 1.24 or later:** export functions with `//go:wasmexport name` and build a WASI reactor with `GOOS=wasip1 GOARCH=wasm go build -buildmode=c-shared`. The module imports `wasi_snapshot_preview1` and exports `_initialize`, `memory` and the functions. Programs built with `GOOS=js` (`wasm_exec.js`, `syscall/js`) are a different model: they run a long-lived `main`. They can still serve Java if they register functions as JavaScript globals.
- **Rust:** build a `cdylib` for `wasm32-unknown-unknown` with `#[no_mangle] pub extern "C"` functions. Such a module has no imports. A wasm-bindgen package should work the same way, through its generated JavaScript object, but that has not been tested.

## What crosses the boundary

- **Numbers pass directly:** `int`, `long` (a JavaScript `BigInt`), `float`, `double` and `boolean`.
- **Objects cannot be shared.** Each module has its own linear memory, and a Java reference is an address in the Java module's memory. Go or Rust code cannot read it, and the Java heap cannot hold their values.
- **Strings and arrays must be copied by JS glue.** A `String` parameter of a `@JS` method arrives in the snippet as a JavaScript string. To pass it on, the other module has to export an allocator. The snippet copies the encoded bytes into that module's memory and passes the pointer and length:

  ```java
  @JS("const bytes = new TextEncoder().encode(text); const pointer = rustlib.alloc(bytes.length); new Uint8Array(rustlib.memory.buffer, pointer, bytes.length).set(bytes); try { return rustlib.count_vowels(pointer, bytes.length); } finally { rustlib.dealloc(pointer, bytes.length); }")
  public static native int countVowels(String text);
  ```

  Results flow back the same way, in reverse.

Limits:
- Every call goes through a JavaScript function. There is no direct Wasm-to-Wasm import yet.
- A `@JS` value must be a single string literal. `javaw-core` embeds a concatenated value (`"a" + "b"`) as its Java source text, so the snippet silently returns `undefined`.
- `javaw-core` binds each import class to one global, and resolves it when the call runs.

## Example and evidence

[`examples/foreign-wasm`](../examples/foreign-wasm) contains a Go library, a Rust library, a Java program that calls both, and a Node runner. `build-and-run.sh` writes every generated module to `examples/foreign-wasm/target/`.

It needs:
- Go 1.24 or later;
- Rust with `rustup target add wasm32-unknown-unknown`;
- wabt and Node;
- the `javaw-core` jar (`mvn -pl javaw-core package -DskipTests`).

Run on 2026-11-12 (Go 1.27.3, Rust 1.99.1, Node 24.14, wabt 1.0.42, GraalVM CE 21.0.2), it printed:

```
goSum(40, 2)       = 84
goFibonacci(90)    = 2880067194370816121
rustProduct(6, 7)  = 55
rustHypot(3, 4)    = 5
rustVowels()       = 11
all foreign-module calls match
```

Each line is checked against its expected value:
- `goSum` and `goFibonacci` call Go;
- `rustProduct` calls Rust and Go in one Java method;
- `rustHypot` passes `double` values;
- `rustVowels` copies a Java string into Rust's memory.

The example is not part of the Maven test suites, because it needs Go and Rust on the test machine. The `examples` build compiles `ForeignWasm.java` with the other demos, but does not run it; `examples/pom.xml` excludes the copy that `build-and-run.sh` places in `foreign-wasm/target/`.

## Bytecode modules

`javaw-bytecode` rejects `native` methods during admission, for example:

```
Native.java:line unavailable: Native.add(II)I: unsupported native/synchronized method; provide a supported source or explicit host adapter
```

Its modules have no annotation-driven host imports. The only imports are `env.memory` and, with `Memory.hostAllocator()`, `env.__heap_alloc`. Supporting foreign modules there would need an explicit contract:
- a way to map a static `native` method with only numeric parameters to a named Wasm import, which a host could satisfy directly with another module's export, without JS;
- diagnostics for every parameter type that cannot cross;
- executable tests.

This is recorded as open work in `Todo.md`, next to the source/bytecode interoperability contract.
