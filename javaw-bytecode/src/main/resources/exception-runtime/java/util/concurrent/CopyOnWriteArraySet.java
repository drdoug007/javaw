package java.util.concurrent;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Single-threaded CopyOnWriteArraySet: a thin wrapper over the linked
 * CopyOnWriteArrayList that keeps each element at most once. Every operation
 * delegates to the list without calling the overridable methods of this set.
 */
public class CopyOnWriteArraySet<E> extends AbstractSet<E> implements java.io.Serializable {
    private final CopyOnWriteArrayList<E> al;

    public CopyOnWriteArraySet() {
        al = new CopyOnWriteArrayList<E>();
    }

    @SuppressWarnings("unchecked")
    public CopyOnWriteArraySet(Collection<? extends E> c) {
        if (c.getClass() == CopyOnWriteArraySet.class)
            al = new CopyOnWriteArrayList<E>(((CopyOnWriteArraySet<E>) c).al);
        else {
            al = new CopyOnWriteArrayList<E>();
            al.addAllAbsent(c);
        }
    }

    public int size() {
        return al.size();
    }

    public boolean isEmpty() {
        return al.isEmpty();
    }

    public boolean contains(Object o) {
        return al.contains(o);
    }

    public Object[] toArray() {
        return al.toArray();
    }

    public <T> T[] toArray(T[] a) {
        return al.toArray(a);
    }

    public void clear() {
        al.clear();
    }

    public boolean remove(Object o) {
        return al.remove(o);
    }

    public boolean add(E e) {
        return al.addIfAbsent(e);
    }

    public boolean containsAll(Collection<?> c) {
        return (c instanceof Set) ? compareSets(al.getArray(), (Set<?>) c) >= 0 : al.containsAll(c);
    }

    public boolean addAll(Collection<? extends E> c) {
        return al.addAllAbsent(c) > 0;
    }

    public boolean removeAll(Collection<?> c) {
        return al.removeAll(c);
    }

    public boolean retainAll(Collection<?> c) {
        return al.retainAll(c);
    }

    public Iterator<E> iterator() {
        return al.iterator();
    }

    public boolean removeIf(Predicate<? super E> filter) {
        return al.removeIf(filter);
    }

    public void forEach(Consumer<? super E> action) {
        al.forEach(action);
    }

    public boolean equals(Object o) {
        return (o == this) || ((o instanceof Set) && compareSets(al.getArray(), (Set<?>) o) == 0);
    }

    /**
     * Returns 0 when snapshot and set hold the same elements, -1 when set holds
     * an element missing from the snapshot, and 1 when the snapshot holds extra
     * elements. Never calls set.size().
     */
    private static int compareSets(Object[] snapshot, Set<?> set) {
        final int len = snapshot.length;
        final boolean[] matched = new boolean[len];
        int j = 0;
        outer:
        for (Object x : set) {
            for (int i = j; i < len; i++) {
                if (!matched[i] && Objects.equals(x, snapshot[i])) {
                    matched[i] = true;
                    if (i == j)
                        do { j++; } while (j < len && matched[j]);
                    continue outer;
                }
            }
            return -1;
        }
        return (j == len) ? 0 : 1;
    }
}
