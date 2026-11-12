import one.dastec.javaw.js.JS;

/** Functions exported by the Go module (golib.wasm); the host sets globalThis.golib to its exports. */
@JS.Import("golib")
class GoLib {
    public static native int add(int a, int b);
    public static native long fib(int n);
}

/** Numeric functions exported by the Rust module (rustlib.wasm); the host sets globalThis.rustlib. */
@JS.Import("rustlib")
class RustLib {
    public static native int mul(int a, int b);
    public static native double hypot(double a, double b);
}

/**
 * Text has to be copied into the Rust module's own memory; the @JS glue does it with Rust's alloc/dealloc exports.
 * The snippet must be one string literal: javaw-core embeds a concatenated value as its source text.
 */
class RustText {
    @JS("const bytes = new TextEncoder().encode(text); const pointer = rustlib.alloc(bytes.length); new Uint8Array(rustlib.memory.buffer, pointer, bytes.length).set(bytes); try { return rustlib.count_vowels(pointer, bytes.length); } finally { rustlib.dealloc(pointer, bytes.length); }")
    public static native int countVowels(String text);
}

public class ForeignWasm {
    public static int goSum(int a, int b) { return GoLib.add(a, b) * 2; }
    public static long goFibonacci(int n) { return GoLib.fib(n) + 1; }
    public static int rustProduct(int a, int b) { return RustLib.mul(a, b) + GoLib.add(a, b); }
    public static double rustHypot(double a, double b) { return RustLib.hypot(a, b); }
    public static int rustVowels() { return RustText.countVowels("Java meets Go and Rust in WebAssembly"); }
}
