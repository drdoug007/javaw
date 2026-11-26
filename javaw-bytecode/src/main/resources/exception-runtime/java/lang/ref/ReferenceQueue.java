package java.lang.ref;

/**
 * A single-threaded copy of {@code java.lang.ref.ReferenceQueue} under the no-reclamation contract: it
 * holds only the references the program enqueues itself, as a LIFO list pushed and polled at the head.
 */
public class ReferenceQueue<T> {
    private static class Null extends ReferenceQueue<Object> {
        boolean enqueue(Reference<?> r) {
            return false;
        }
    }

    static final ReferenceQueue<Object> NULL = new Null();
    static final ReferenceQueue<Object> ENQUEUED = new Null();

    private Reference<? extends T> head;

    public ReferenceQueue() {
    }

    @SuppressWarnings("unchecked")
    boolean enqueue(Reference<? extends T> r) { // called only by Reference
        if (r.queue == NULL || r.queue == ENQUEUED)
            return false;
        r.next = (head == null) ? r : head;
        head = r;
        r.queue = ENQUEUED;
        return true;
    }

    @SuppressWarnings("unchecked")
    public Reference<? extends T> poll() {
        Reference<? extends T> r = head;
        if (r == null)
            return null;
        r.queue = NULL;
        head = (r.next == r) ? null : r.next;
        r.next = r;
        return r;
    }

    public Reference<? extends T> remove(long timeout) throws IllegalArgumentException, InterruptedException {
        if (timeout < 0)
            throw new IllegalArgumentException("Negative timeout value");
        if (timeout == 0)
            return remove();
        return poll();
    }

    public Reference<? extends T> remove() throws InterruptedException {
        if (head == null)
            throw new IllegalStateException("ReferenceQueue.remove would block forever: the queue is empty and no other thread");
        return poll();
    }
}
