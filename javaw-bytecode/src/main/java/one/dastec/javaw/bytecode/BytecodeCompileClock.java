package one.dastec.javaw.bytecode;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Process-wide wall time and count of outermost bytecode compilations, read by the test timing report. A compilation
 * that starts another on the same thread (a whole-program build lowering each class) is counted once. Failed
 * compilations, including admission diagnostics, are counted too. {@link BytecodeCompiler} entry points include class
 * parsing and legacy-subroutine normalization; constructing a {@link ClassBytecodeCompiler} directly does not.
 */
final class BytecodeCompileClock {
    interface Compilation<T, E extends Exception> { T run() throws E; }

    private static final AtomicLong NANOS = new AtomicLong(), COUNT = new AtomicLong();
    private static final ThreadLocal<int[]> DEPTH = ThreadLocal.withInitial(() -> new int[1]);
    /** Test hook: receives each outermost compilation's result (WAT text) or thrown exception. */
    static volatile java.util.function.Consumer<Object> observer;

    private BytecodeCompileClock() {}

    static <T, E extends Exception> T time(Compilation<T, E> compilation) throws E {
        int[] depth = DEPTH.get();
        if (depth[0]++ > 0) {
            try { return compilation.run(); } finally { depth[0]--; }
        }
        long start = System.nanoTime();
        Object outcome = null;
        try {
            T result = compilation.run();
            outcome = result;
            return result;
        } catch (Exception | Error error) {
            outcome = error;
            throw error;
        } finally {
            depth[0]--; NANOS.addAndGet(System.nanoTime() - start); COUNT.incrementAndGet();
            var listener = observer;
            if (listener != null) listener.accept(outcome);
        }
    }

    static long nanos() { return NANOS.get(); }

    static long count() { return COUNT.get(); }
}
