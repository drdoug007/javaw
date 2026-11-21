package java.util.concurrent;

import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Single-threaded LinkedBlockingQueue: an optionally bounded chain of nodes
 * after a sentinel head whose item is always null. Live elements are the
 * nodes after the sentinel, in order, each with a non-null item; removed
 * nodes are really unlinked. There are no locks, conditions or atomics:
 * Wasm runs one thread, so put on a full queue and take on an empty queue
 * throw instead of blocking forever. Iterators are weakly consistent and
 * remember the item they already fetched.
 */
public class LinkedBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E>, java.io.Serializable {

    static final class Node<E> {
        E item;
        Node<E> next;

        Node(E x) {
            item = x;
        }
    }

    private final int capacity;
    private int count;
    private Node<E> head;
    private Node<E> last;

    public LinkedBlockingQueue() {
        this(Integer.MAX_VALUE);
    }

    public LinkedBlockingQueue(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException();
        }
        this.capacity = capacity;
        last = head = new Node<E>(null);
    }

    public LinkedBlockingQueue(Collection<? extends E> c) {
        this(Integer.MAX_VALUE);
        for (E e : c) {
            if (e == null) {
                throw new NullPointerException();
            }
            enqueue(new Node<E>(e));
            count++;
        }
    }

    private void enqueue(Node<E> node) {
        last = last.next = node;
    }

    private E dequeue() {
        Node<E> h = head;
        Node<E> first = h.next;
        h.next = h;
        head = first;
        E x = first.item;
        first.item = null;
        return x;
    }

    void unlink(Node<E> p, Node<E> pred) {
        p.item = null;
        pred.next = p.next;
        if (last == p) {
            last = pred;
        }
        count--;
    }

    public int size() {
        return count;
    }

    public int remainingCapacity() {
        return capacity - count;
    }

    public void put(E e) throws InterruptedException {
        if (e == null) {
            throw new NullPointerException();
        }
        if (count == capacity) {
            throw new IllegalStateException("LinkedBlockingQueue.put would block forever: queue full and no other thread");
        }
        enqueue(new Node<E>(e));
        count++;
    }

    public boolean offer(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        if (count == capacity) {
            return false;
        }
        enqueue(new Node<E>(e));
        count++;
        return true;
    }

    public E poll() {
        if (count == 0) {
            return null;
        }
        count--;
        return dequeue();
    }

    public E take() throws InterruptedException {
        if (count == 0) {
            throw new IllegalStateException("LinkedBlockingQueue.take would block forever: queue empty and no other thread");
        }
        count--;
        return dequeue();
    }

    public E peek() {
        if (count == 0) {
            return null;
        }
        return head.next.item;
    }

    public boolean remove(Object o) {
        if (o == null) {
            return false;
        }
        for (Node<E> pred = head, p = pred.next; p != null; pred = p, p = p.next) {
            if (o.equals(p.item)) {
                unlink(p, pred);
                return true;
            }
        }
        return false;
    }

    public boolean contains(Object o) {
        if (o == null) {
            return false;
        }
        for (Node<E> p = head.next; p != null; p = p.next) {
            if (o.equals(p.item)) {
                return true;
            }
        }
        return false;
    }

    public Object[] toArray() {
        Object[] result = new Object[count];
        int i = 0;
        for (Node<E> p = head.next; p != null; p = p.next) {
            result[i++] = p.item;
        }
        return result;
    }

    public <T> T[] toArray(T[] a) {
        if (a.length < count) {
            a = Arrays.copyOf(a, count);
        }
        int i = 0;
        for (Node<E> p = head.next; p != null; p = p.next) {
            a[i++] = (T) p.item;
        }
        if (a.length > count) {
            a[count] = null;
        }
        return a;
    }

    public String toString() {
        Object[] a = toArray();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < a.length; i++) {
            E e = (E) a[i];
            sb.append(e == this ? "(this Collection)" : e);
            if (i != a.length - 1) {
                sb.append(", ");
            }
        }
        return sb.append(']').toString();
    }

    public void clear() {
        for (Node<E> p, h = head; (p = h.next) != null; h = p) {
            h.next = h;
            p.item = null;
        }
        head = last;
        count = 0;
    }

    public int drainTo(Collection<? super E> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }

    public int drainTo(Collection<? super E> c, int maxElements) {
        Objects.requireNonNull(c);
        if (c == this) {
            throw new IllegalArgumentException();
        }
        if (maxElements <= 0) {
            return 0;
        }
        int n = Math.min(maxElements, count);
        Node<E> h = head;
        int i = 0;
        try {
            while (i < n) {
                Node<E> p = h.next;
                c.add(p.item);
                p.item = null;
                h.next = h;
                h = p;
                ++i;
            }
            return n;
        } finally {
            if (i > 0) {
                head = h;
                count -= i;
            }
        }
    }

    public Iterator<E> iterator() {
        return new Itr();
    }

    private class Itr implements Iterator<E> {
        Node<E> next;
        E nextItem;
        Node<E> lastRet;
        Node<E> ancestor;

        Itr() {
            Node<E> p = head.next;
            if (p != null) {
                next = p;
                nextItem = p.item;
            }
        }

        public boolean hasNext() {
            return next != null;
        }

        public E next() {
            if (next == null) {
                throw new NoSuchElementException();
            }
            Node<E> p = next;
            lastRet = p;
            E x = nextItem;
            E e = null;
            for (p = p.next; p != null && (e = p.item) == null; ) {
                p = succ(p);
            }
            next = p;
            nextItem = e;
            return x;
        }

        public void remove() {
            Node<E> p = lastRet;
            if (p == null) {
                throw new IllegalStateException();
            }
            lastRet = null;
            if (p.item != null) {
                if (ancestor == null) {
                    ancestor = head;
                }
                ancestor = findPred(p, ancestor);
                unlink(p, ancestor);
            }
        }

        Node<E> succ(Node<E> p) {
            if (p == (p = p.next)) {
                p = head.next;
            }
            return p;
        }

        Node<E> findPred(Node<E> p, Node<E> ancestor) {
            if (ancestor.item == null) {
                ancestor = head;
            }
            for (Node<E> q; (q = ancestor.next) != p; ) {
                ancestor = q;
            }
            return ancestor;
        }
    }
}
