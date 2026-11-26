package java.lang.ref;

/** A single-threaded copy of {@code java.lang.ref.PhantomReference}: {@code get()} is always {@code null}. */
public class PhantomReference<T> extends Reference<T> {
    public PhantomReference(T referent, ReferenceQueue<? super T> q) {
        super(referent, q);
    }

    public T get() {
        return null;
    }
}
