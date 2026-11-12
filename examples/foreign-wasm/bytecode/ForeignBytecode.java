package foreign;

/**
 * Compiled with javac, then by javaw-bytecode with one --native-import per method. Each native method becomes a Wasm
 * import, which the host satisfies with the Go or Rust module's export itself: no JavaScript sits on the call path.
 */
final class GoLib {
    static native int add(int a, int b);
    static native long fib(int n);
}

final class RustLib {
    static native int mul(int a, int b);
    static native double hypot(double a, double b);
}

public class ForeignBytecode {
    public static int goSum(int a, int b) { return GoLib.add(a, b) * 2; }
    public static long goFibonacci(int n) { return GoLib.fib(n) + 1; }
    public static int rustProduct(int a, int b) { return RustLib.mul(a, b) + GoLib.add(a, b); }
    public static double rustHypot(double a, double b) { return RustLib.hypot(a, b); }
    /** Ordinary Java around the calls: a bundled-runtime collection and string building. */
    public static int summary() {
        java.util.List<Long> values = new java.util.ArrayList<>();
        for (int n = 10; n <= 50; n += 10) values.add(GoLib.fib(n));
        return (values + " " + RustLib.hypot(5, 12)).length();
    }
}
