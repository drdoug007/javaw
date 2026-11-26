package java.lang.ref;

/** A single-threaded copy of {@code java.lang.ref.WeakReference} under the no-reclamation contract. */
public class WeakReference<T> extends Reference<T> {
    public WeakReference(T referent) {
        super(referent);
    }

    public WeakReference(T referent, ReferenceQueue<? super T> q) {
        super(referent, q);
    }
}
