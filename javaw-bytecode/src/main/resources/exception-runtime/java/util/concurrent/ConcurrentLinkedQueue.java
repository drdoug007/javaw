package java.util.concurrent;

import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Single-threaded ConcurrentLinkedQueue: a chain of nodes after a permanent
 * sentinel head whose item is always null. Live elements are the nodes after
 * the sentinel with a non-null item; removing an element sets its item to null
 * (a "dead" node) and unlinks it only when it has a successor, so the last
 * node stays as a dead tail. There are no locks or atomics: Wasm runs one
 * thread. Iterators are weakly consistent and remember the item they already
 * fetched.
 */
public class ConcurrentLinkedQueue<E> extends AbstractQueue<E> implements Queue<E>, java.io.Serializable {

    static final class Node<E> {
        E item;
        Node<E> next;

        Node(E item) {
            this.item = item;
        }
    }

    private final Node<E> head = new Node<E>(null);
    private Node<E> tail = head;

    public ConcurrentLinkedQueue() {
    }

    public ConcurrentLinkedQueue(Collection<? extends E> c) {
        Objects.requireNonNull(c);
        Node<E> first = null, last = null;
        for (E e : c) {
            Node<E> n = new Node<E>(Objects.requireNonNull(e));
            if (last == null) {
                first = last = n;
            } else {
                last.next = n;
                last = n;
            }
        }
        if (first != null) {
            tail.next = first;
            tail = last;
        }
    }

    public boolean offer(E e) {
        Node<E> n = new Node<E>(Objects.requireNonNull(e));
        tail.next = n;
        tail = n;
        return true;
    }

    public E poll() {
        Node<E> p = head.next;
        while (p != null && p.item == null) {
            p = p.next;
        }
        if (p == null) {
            return null;
        }
        E item = p.item;
        p.item = null;
        head.next = (p.next != null) ? p.next : p;
        return item;
    }

    public E peek() {
        Node<E> p = head.next;
        while (p != null && p.item == null) {
            p = p.next;
        }
        return (p == null) ? null : p.item;
    }

    public boolean isEmpty() {
        Node<E> p = head.next;
        while (p != null && p.item == null) {
            p = p.next;
        }
        return p == null;
    }

    public int size() {
        int n = 0;
        for (Node<E> p = head.next; p != null; p = p.next) {
            if (p.item != null && ++n == Integer.MAX_VALUE) {
                break;
            }
        }
        return n;
    }

    public boolean contains(Object o) {
        if (o == null) {
            return false;
        }
        for (Node<E> p = head.next; p != null; p = p.next) {
            if (p.item != null && o.equals(p.item)) {
                return true;
            }
        }
        return false;
    }

    public boolean remove(Object o) {
        if (o == null) {
            return false;
        }
        Node<E> pred = head;
        Node<E> p = head.next;
        while (p != null) {
            if (p.item != null && o.equals(p.item)) {
                p.item = null;
                if (p.next != null) {
                    pred.next = p.next;
                }
                return true;
            }
            pred = p;
            p = p.next;
        }
        return false;
    }

    public boolean addAll(Collection<? extends E> c) {
        if (c == null) {
            throw new NullPointerException();
        }
        if (c == this) {
            throw new IllegalArgumentException();
        }
        Node<E> first = null, last = null;
        for (E e : c) {
            Node<E> n = new Node<E>(Objects.requireNonNull(e));
            if (last == null) {
                first = last = n;
            } else {
                last.next = n;
                last = n;
            }
        }
        if (first == null) {
            return false;
        }
        tail.next = first;
        tail = last;
        return true;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Node<E> p = head.next; p != null; p = p.next) {
            E item = p.item;
            if (item != null) {
                if (!first) {
                    sb.append(", ");
                }
                first = false;
                String s = item.toString();
                if (s == null) {
                    throw new NullPointerException();
                }
                sb.append(s);
            }
        }
        return sb.append(']').toString();
    }

    public Object[] toArray() {
        int n = 0;
        for (Node<E> p = head.next; p != null; p = p.next) {
            if (p.item != null) {
                n++;
            }
        }
        Object[] result = new Object[n];
        int i = 0;
        for (Node<E> p = head.next; p != null; p = p.next) {
            if (p.item != null) {
                result[i++] = p.item;
            }
        }
        return result;
    }

    public <T> T[] toArray(T[] a) {
        Objects.requireNonNull(a);
        int n = 0;
        for (Node<E> p = head.next; p != null; p = p.next) {
            if (p.item != null) {
                n++;
            }
        }
        T[] result = (a.length >= n) ? a : Arrays.copyOf(a, n);
        int i = 0;
        for (Node<E> p = head.next; p != null; p = p.next) {
            if (p.item != null) {
                result[i++] = (T) p.item;
            }
        }
        if (result.length > n) {
            result[n] = null;
        }
        return result;
    }

    public Iterator<E> iterator() {
        return new Itr();
    }

    public void forEach(Consumer<? super E> action) {
        Objects.requireNonNull(action);
        for (Node<E> p = head.next; p != null; ) {
            Node<E> q = p.next;
            E item = p.item;
            if (item != null) {
                action.accept(item);
            }
            p = q;
        }
    }

    public boolean removeIf(Predicate<? super E> filter) {
        Objects.requireNonNull(filter);
        return bulkRemove(filter);
    }

    public boolean removeAll(Collection<?> c) {
        Objects.requireNonNull(c);
        return bulkRemove(e -> c.contains(e));
    }

    public boolean retainAll(Collection<?> c) {
        Objects.requireNonNull(c);
        return bulkRemove(e -> !c.contains(e));
    }

    public void clear() {
        bulkRemove(e -> true);
    }

    private boolean bulkRemove(Predicate<? super E> filter) {
        boolean any = false;
        Node<E> pred = head;
        Node<E> p = head.next;
        while (p != null) {
            Node<E> q = p.next;
            if (p.item != null && filter.test(p.item)) {
                p.item = null;
                any = true;
            }
            if (p.item == null && q != null) {
                pred.next = q;
            } else {
                pred = p;
            }
            p = q;
        }
        return any;
    }

    private final class Itr implements Iterator<E> {
        Node<E> nextNode;
        E nextItem;
        Node<E> lastRet;

        Itr() {
            advance(head);
        }

        private void advance(Node<E> from) {
            Node<E> p = from.next;
            while (p != null && p.item == null) {
                p = p.next;
            }
            nextNode = p;
            nextItem = (p == null) ? null : p.item;
        }

        public boolean hasNext() {
            return nextItem != null;
        }

        public E next() {
            if (nextNode == null) {
                throw new NoSuchElementException();
            }
            E x = nextItem;
            lastRet = nextNode;
            advance(nextNode);
            return x;
        }

        public void remove() {
            if (lastRet == null) {
                throw new IllegalStateException();
            }
            lastRet.item = null;
            lastRet = null;
        }
    }
}
