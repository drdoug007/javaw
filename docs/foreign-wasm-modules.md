# Calling Go and Rust WebAssembly modules from Java

Java compiled by the source compiler (`javaw-core`) can call functions exported by WebAssembly modules built with other toolchains, such as Go or Rust. The host instantiates the other module first and exposes its exports as a JavaScript global. A Java class annotated with `@JS.Import("<global>")` then declares the functions as `native` methods.

The bytecode compiler (`javaw-bytecode`) takes a different route: it binds `static native` methods to Wasm imports, and the host passes the other module's exports to those imports directly, with no JavaScript in between. See [Bytecode modules](#bytecode-modules).

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
- Every call goes through a JavaScript function. Bytecode modules can import the other module's functions directly; see [Bytecode modules](#bytecode-modules).
- A `@JS` value must be a single string literal. `javaw-core` embeds a concatenated value (`"a" + "b"`) as its Java source text, so the snippet silently returns `undefined`.
- `javaw-core` binds each import class to one global, and resolves it when the call runs.

## Example and evidence

[`examples/foreign-wasm`](../examples/foreign-wasm) contains a Go library, a Rust library, a Java program that calls both, and a Node runner. `build-and-run.sh` writes every generated module to `examples/foreign-wasm/target/`.

It needs:
- Go 1.24 or later;
- Rust with `rustup target add wasm32-unknown-unknown`;
- wabt and Node;
- the `javaw-core` jar (`mvn -pl javaw-core package -DskipTests`);
- for the bytecode part, a JDK and compiled `javaw-bytecode` classes (`mvn -pl javaw-bytecode -am compile`).

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

The script then runs the bytecode version; see [Bytecode modules](#bytecode-modules) for its output. The example is not part of the Maven test suites, because it needs Go and Rust on the test machine. The `examples` build compiles `ForeignWasm.java` with the other demos, but does not run it. `examples/pom.xml` excludes the copy that `build-and-run.sh` places in `foreign-wasm/target/`, and `foreign-wasm/bytecode/`, whose plain native methods are for `javaw-bytecode`.

## Bytecode modules

`javaw-bytecode` binds static `native` methods to named Wasm imports (compiler revision `native-imports-v260`). The binding is explicit: an unbound native method is still rejected. A binding is never inferred from the class file, so it also works for third-party bytecode that cannot be annotated.

```java
package foreign;
final class GoLib {
    static native int add(int a, int b);
    static native long fib(int n);
}
```

```
java one.dastec.javaw.bytecode.BytecodeCompiler --exception-runtime \
    --native-import='foreign.GoLib.add(II)I=golib:add' \
    --native-import='foreign.GoLib.fib(I)J=golib:fib' app.jar app.wat
```

The API equivalent is `BytecodeCompiler.compileJarWithExceptionRuntime(input, new NativeImport("foreign/GoLib", "add", "(II)I", "golib", "add"), ...)`, or the overload that also takes proxy shapes, retained functions and `Memory`. `NativeImport.parse` reads the command-line form. The class name may be binary (dots, with `$` for nested classes, e.g. `pkg.Outer$Inner`) or internal (slashes). Without `:field`, the field is the method name. Module names cannot contain `:`, and class and method names cannot contain `(` or `=`, so `toString()` is always the command-line form.

Each binding adds one import, for example `(import "golib" "add" (func $__native_... (param i32 i32) (result i32)))`, and the host passes the other module's export object as that namespace:

```js
const go = await WebAssembly.instantiate(goBytes, wasi.getImportObject());
wasi.initialize(go.instance);
const rust = await WebAssembly.instantiate(rustBytes, {});
const java = new WebAssembly.Instance(javaModule,
    { env: { memory }, golib: go.instance.exports, rustlib: rust.instance.exports }).exports;
```

Because the import is the other module's exported function itself, a Java call is a direct Wasm-to-Wasm call.

Contract:
- **Which methods can be bound.** The method must be a `static native` method declared by an input class of the compilation. Bundled-runtime classes cannot be bound, and only bundled-runtime builds (`--exception-runtime`) accept bindings.
- **What a binding is checked for.** Every binding is checked before admission. Its method must exist and be static and native, and its parameter and result types must be primitive (`boolean`, `byte`, `char`, `short`, `int`, `long`, `float`, `double`, or a `void` result). Each method may be bound once, and each `module`/`field` pair names one method. `env.memory`, and `env.__heap_alloc` with `Memory.hostAllocator()`, are the module's own imports and cannot be bound. A failure names the method and, for a type that cannot cross, the parameter position and Java type, for example `parameter 1 has type java.lang.String, which cannot cross a Wasm import`. Instance methods are rejected, because a receiver is an address in the Java module's memory.
- **Unbound and synchronized natives.** An unbound native is still rejected by admission, and a bound `synchronized` native is rejected as a synchronized method. Input bytecode cannot bind itself: a class-file attribute is never read as a binding.
- **Types at the boundary.** `long` crosses as `i64`, `float` and `double` as `f32` and `f64`, and the other types as `i32`. Verified bytecode may pass any `int` for a sub-int parameter, and the foreign function may return any `i32`. In both directions, a `boolean`, `byte`, `char` or `short` is read as a JNI implementation reads `jboolean`, `jbyte`, `jchar` and `jshort`: `boolean` is 1 when the low 8 bits are nonzero and 0 otherwise, `byte` and `short` sign-extend the low 8 or 16 bits, and `char` keeps the low 16 bits. So the foreign function always receives a canonical Java value.
- **Wrapper and class initialization.** The method compiles to a wrapper function. Like any static method, the wrapper first initializes its class (JVMS §5.5), then calls the import. It is exported when public, and it is reached through ordinary calls, method references and lambdas.
- **Imports are fixed by the bindings.** The module imports every binding, even one that no reachable code calls, so the host's import object is determined by the bindings alone. The unreachable wrappers are pruned.
- **Traps.** A trap in the foreign function (for example a Rust panic or Wasm `unreachable`), or a JavaScript exception from a host function, is not a Java exception. Java `catch` blocks do not see it, and it propagates to the host as a `WebAssembly.RuntimeError` or as the thrown value. Java `finally` blocks on the way out do not run.
  - Wasm cannot catch a trap, so a trap also skips the runtime's bookkeeping. If it happens while a class initializer is running, directly or through calls from `<clinit>`, that class stays marked as initializing. Later calls would then silently see its partly initialized statics. **Discard the instance after a foreign trap** and create a new one.
  - `__reset_exception_trace` only clears the exception-trace stack. It is enough after a trap outside class initialization, but it cannot restore initialization state; see [the initialization contract](compatibility.md#automatic-bytecode-class-initialization).
  - A JavaScript exception from a host function is handled like other foreign host exceptions: it marks a class whose initializer it escapes as erroneous.
- **Signature mismatches.** The host must supply functions with the declared Wasm types. A mismatched export fails at instantiation with a `LinkError`.

Limits:
- Only primitive values cross. Strings and arrays still need a copy into the other module's memory, which bytecode modules cannot express without JS glue. Use the source compiler's `@JS` snippets for that.
- Bindings are compiler options, so the Maven plugin's dependency cache, whose key does not cover compiler options yet, does not record them (Phase 8 in `Todo.md`).
- Instance, synchronized and reference-typed natives, and the JDK's own native methods, still need the host-adapter contracts in `Todo.md`.

Evidence:
- `BytecodeNativeImportTest` compiles javac output and runs it in Node against a hand-written foreign module, whose exports object is passed as the import namespace. It covers:
  - every primitive kind and mixed parameters;
  - void calls with foreign state;
  - out-of-range sub-int results, and out-of-range sub-int arguments from ASM-generated bytecode;
  - class initialization before the first call;
  - a method reference;
  - a public bound native's export;
  - an unused binding (imported, wrapper pruned);
  - a foreign trap that Java cannot catch, including one in a class initializer.

  It fails if the boolean result narrowing or the argument narrowing is removed. It also checks that import names containing pruning markers (`call_indirect`, `(table`, `(;`) are escaped and leave pruning unchanged, and that the nested-class hint is given.
- The same test checks the diagnostics above. They cover unbound, synchronized and instance natives, reference parameters and results, missing and non-native methods, runtime classes, duplicate bindings and import names, the reserved `env` names, a build without the bundled runtime, and a forged class-file attribute. It also checks `NativeImport` parsing and the command line.
- [`examples/foreign-wasm`](../examples/foreign-wasm) also builds `bytecode/ForeignBytecode.java` with `javac` and `javaw-bytecode`, and `run-bytecode.cjs` runs it against the real Go and Rust modules. Run on 2026-11-12 with the toolchains above (wabt 1.0.42), it printed:

  ```
  imports: env.memory, golib.add, golib.fib, rustlib.mul, rustlib.hypot
  goSum(40, 2)       = 84
  goFibonacci(90)    = 2880067194370816121
  rustProduct(6, 7)  = 55
  rustHypot(3, 4)    = 5
  summary()          = 47
  all bytecode foreign-module calls match
  ```
