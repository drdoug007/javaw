package java.lang.ref;

/**
 * A single-threaded copy of {@code java.lang.ref.Reference} under the no-reclamation contract: the
 * referent is held strongly and is never cleared or enqueued by a collector. Only the program clears
 * ({@code clear()}) or enqueues ({@code enqueue()}) a reference.
 */
public abstract class Reference<T> {
    private T referent;
    ReferenceQueue<? super T> queue;
    Reference next;

    Reference(T referent) {
        this(referent, null);
    }

    Reference(T referent, ReferenceQueue<? super T> queue) {
        this.referent = referent;
        this.queue = (queue == null) ? ReferenceQueue.NULL : queue;
    }

    public T get() {
        return this.referent;
    }

    public final boolean refersTo(T obj) {
        return this.referent == obj;
    }

    public void clear() {
        this.referent = null;
    }

    @Deprecated
    public boolean isEnqueued() {
        return (this.queue == ReferenceQueue.ENQUEUED);
    }

    public boolean enqueue() {
        // Clear the field directly: the JDK calls the private clear0(), not the overridable clear().
        this.referent = null;
        return this.queue.enqueue(this);
    }
}
