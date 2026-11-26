package java.lang.ref;

/** A single-threaded copy of {@code java.lang.ref.SoftReference} under the no-reclamation contract. */
public class SoftReference<T> extends Reference<T> {
    public SoftReference(T referent) {
        super(referent);
    }

    public SoftReference(T referent, ReferenceQueue<? super T> q) {
        super(referent, q);
    }

    public T get() {
        return super.get();
    }
}
