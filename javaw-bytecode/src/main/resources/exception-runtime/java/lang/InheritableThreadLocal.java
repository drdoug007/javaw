package java.lang;

/** InheritableThreadLocal: identical to ThreadLocal here; no child thread is ever created, so childValue is never called. */
public class InheritableThreadLocal<T> extends ThreadLocal<T> {
    public InheritableThreadLocal() {}

    protected T childValue(T parentValue) {
        return parentValue;
    }
}
