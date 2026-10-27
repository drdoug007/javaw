package one.dastec.javaw.bytecode;

import org.objectweb.asm.tree.ClassNode;
import java.util.*;
import java.util.function.Supplier;

/** Whole-program analyses memoized while one compilation's normalized linked program is immutable. */
final class BytecodeAnalysisCache implements AutoCloseable {
    private static final ThreadLocal<BytecodeAnalysisCache> ACTIVE = new ThreadLocal<>();
    private final Map<String, ClassNode> linked;
    private final Map<String, Object> values = new HashMap<>();
    private final BytecodeAnalysisCache previous;

    private BytecodeAnalysisCache(Map<String, ClassNode> linked) {
        this.linked = linked;
        previous = ACTIVE.get();
        ACTIVE.set(this);
    }

    /** Open only after normalization: lowering and validation must not mutate the linked classes. */
    static BytecodeAnalysisCache open(Map<String, ClassNode> linked) { return new BytecodeAnalysisCache(linked); }

    /** Results are shared only for the exact linked map instance of the active compilation. */
    @SuppressWarnings("unchecked")
    static <T> T get(Map<String, ClassNode> linked, String key, Supplier<T> compute) {
        BytecodeAnalysisCache cache = ACTIVE.get();
        if (cache == null || cache.linked != linked) return compute.get();
        Object value = cache.values.get(key);
        if (value == null) {
            value = compute.get();
            cache.values.put(key, value);
        }
        return (T) value;
    }

    @Override public void close() {
        if (previous == null) ACTIVE.remove(); else ACTIVE.set(previous);
    }
}
