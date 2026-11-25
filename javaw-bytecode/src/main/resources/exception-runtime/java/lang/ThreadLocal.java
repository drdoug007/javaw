package java.lang;

import java.util.Objects;
import java.util.function.Supplier;

/** Single-threaded ThreadLocal: one value slot per instance, no per-thread map. */
public class ThreadLocal<T> {
    private boolean present;
    private T value;

    public ThreadLocal() {}

    protected T initialValue() {
        return null;
    }

    public static <S> ThreadLocal<S> withInitial(Supplier<? extends S> supplier) {
        return new SuppliedThreadLocal<>(supplier);
    }

    public T get() {
        if (present)
            return value;
        return setInitialValue();
    }

    private T setInitialValue() {
        T v = initialValue();
        value = v;
        present = true;
        return v;
    }

    public void set(T value) {
        this.value = value;
        present = true;
    }

    public void remove() {
        value = null;
        present = false;
    }

    static final class SuppliedThreadLocal<T> extends ThreadLocal<T> {
        private final Supplier<? extends T> supplier;

        SuppliedThreadLocal(Supplier<? extends T> supplier) {
            this.supplier = Objects.requireNonNull(supplier);
        }

        protected T initialValue() {
            return supplier.get();
        }
    }
}
