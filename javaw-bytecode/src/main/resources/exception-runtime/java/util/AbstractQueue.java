package java.util;

/**
 * Base implementation of Queue, deferring insertion and removal to subclasses.
 * Follows the JDK's AbstractQueue: add/remove/element/clear/addAll on top of offer/poll/peek.
 */
public abstract class AbstractQueue<E> extends AbstractCollection<E> implements Queue<E> {
    protected AbstractQueue() {}

    public boolean add(E e) {
        if (!offer(e)) {
            throw new IllegalStateException("Queue full");
        }
        return true;
    }

    public E remove() {
        E value = poll();
        if (value == null) {
            throw new NoSuchElementException();
        }
        return value;
    }

    public E element() {
        E value = peek();
        if (value == null) {
            throw new NoSuchElementException();
        }
        return value;
    }

    public void clear() {
        while (poll() != null) {
        }
    }

    public boolean addAll(Collection<? extends E> c) {
        if (c == null) {
            throw new NullPointerException();
        }
        if (c == this) {
            throw new IllegalArgumentException();
        }
        boolean added = false;
        for (E value : c) {
            if (add(value)) {
                added = true;
            }
        }
        return added;
    }
}
