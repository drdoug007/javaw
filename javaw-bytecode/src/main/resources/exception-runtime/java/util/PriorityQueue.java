package java.util;

import java.io.Serializable;

/**
 * A priority queue based on a binary heap array, following the JDK's PriorityQueue
 * layout and siftUp/siftDown algorithms exactly. The queue array holds the elements
 * in heap order (not sorted order); iteration, toString and toArray show that array.
 */
public class PriorityQueue<E> extends AbstractQueue<E> implements Serializable {
    private Object[] queue;
    private int size;
    private final Comparator<? super E> comparator;

    public PriorityQueue() {
        this(11, null);
    }

    public PriorityQueue(int initialCapacity) {
        this(initialCapacity, null);
    }

    public PriorityQueue(Comparator<? super E> c) {
        this(11, c);
    }

    public PriorityQueue(int initialCapacity, Comparator<? super E> c) {
        if (initialCapacity < 1) {
            throw new IllegalArgumentException();
        }
        this.queue = new Object[initialCapacity];
        this.comparator = c;
    }

    /**
     * Copies the elements of c into a new array and heapifies it. A null collection or a
     * collection holding null elements throws NullPointerException. A SortedSet or
     * PriorityQueue argument contributes its comparator; otherwise natural ordering.
     */
    public PriorityQueue(Collection<? extends E> c) {
        if (c instanceof SortedSet<? extends E>) {
            this.comparator = (Comparator<? super E>) ((SortedSet<? extends E>) c).comparator();
        } else if (c instanceof PriorityQueue<? extends E>) {
            this.comparator = (Comparator<? super E>) ((PriorityQueue<? extends E>) c).comparator();
        } else {
            this.comparator = null;
        }
        Object[] values = c.toArray();
        for (Object value : values) {
            if (value == null) {
                throw new NullPointerException();
            }
        }
        this.queue = new Object[Math.max(1, values.length)];
        System.arraycopy(values, 0, this.queue, 0, values.length);
        this.size = values.length;
        for (int i = (this.size >>> 1) - 1; i >= 0; i--) {
            siftDown(i, this.queue[i]);
        }
    }

    /** Unlike AbstractQueue.add, returns offer's result instead of throwing when it is false (as the JDK). */
    public boolean add(E e) {
        return offer(e);
    }

    public boolean offer(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        if (size == queue.length) {
            grow();
        }
        // Cast before storing so a non-Comparable element fails on an empty queue, as in the JDK.
        Comparable<? super E> key = comparator == null ? (Comparable<? super E>) e : null;
        siftUp(size, key == null ? e : key);
        size++;
        return true;
    }

    public E poll() {
        if (size == 0) {
            return null;
        }
        E result = (E) queue[0];
        int last = --size;
        Object x = queue[last];
        queue[last] = null;
        if (last > 0) {
            siftDown(0, x);
        }
        return result;
    }

    public E peek() {
        return size == 0 ? null : (E) queue[0];
    }

    public boolean remove(Object o) {
        if (o == null) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            if (o.equals(queue[i])) {
                removeAt(i);
                return true;
            }
        }
        return false;
    }

    public boolean contains(Object o) {
        if (o == null) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            if (o.equals(queue[i])) {
                return true;
            }
        }
        return false;
    }

    public int size() {
        return size;
    }

    public void clear() {
        for (int i = 0; i < size; i++) {
            queue[i] = null;
        }
        size = 0;
    }

    public Comparator<? super E> comparator() {
        return comparator;
    }

    public Object[] toArray() {
        return Arrays.copyOf(queue, size);
    }

    public Iterator<E> iterator() {
        return new Itr();
    }

    /**
     * Grows the heap array. The growth rate is not observable, so a simple doubling works.
     */
    private void grow() {
        int length = queue.length;
        queue = Arrays.copyOf(queue, length < 64 ? 2 * length + 2 : length * 3 / 2);
    }

    /**
     * Moves x up from index k while it is smaller than its parent.
     */
    private void siftUp(int k, Object x) {
        while (k > 0) {
            int parent = (k - 1) >>> 1;
            Object e = queue[parent];
            if (compare(x, e) >= 0) {
                break;
            }
            queue[k] = e;
            k = parent;
        }
        queue[k] = x;
    }

    /**
     * Moves x down from index k while it is larger than its smaller child, using the current size.
     */
    private void siftDown(int k, Object x) {
        int half = size >>> 1;
        while (k < half) {
            int child = 2 * k + 1;
            Object c = queue[child];
            int right = child + 1;
            if (right < size && compare(c, queue[right]) > 0) {
                c = queue[child = right];
            }
            if (compare(x, c) <= 0) {
                break;
            }
            queue[k] = c;
            k = child;
        }
        queue[k] = x;
    }

    /**
     * Removes the element at index i, moving the last element into its place and re-heapifying.
     */
    private void removeAt(int i) {
        int last = --size;
        if (last == i) {
            queue[i] = null;
        } else {
            Object moved = queue[last];
            queue[last] = null;
            siftDown(i, moved);
            if (queue[i] == moved) {
                siftUp(i, moved);
            }
        }
    }

    private int compare(Object a, Object b) {
        return comparator == null ? ((Comparable<Object>) a).compareTo(b) : comparator.compare((E) a, (E) b);
    }

    /**
     * Visits a snapshot of the heap array taken when the iterator was created, so it reports
     * every element exactly once even when remove() re-heaps the live array. Not fail-fast.
     */
    private class Itr implements Iterator<E> {
        private final Object[] snapshot;
        private int next = 0;
        private boolean removed = false;

        Itr() {
            snapshot = Arrays.copyOf(queue, size);
        }

        public boolean hasNext() {
            return next < snapshot.length;
        }

        public E next() {
            if (next >= snapshot.length) {
                throw new NoSuchElementException();
            }
            removed = true;
            return (E) snapshot[next++];
        }

        public void remove() {
            if (!removed) {
                throw new IllegalStateException();
            }
            removed = false;
            Object value = snapshot[next - 1];
            for (int i = 0; i < size; i++) {
                if (queue[i] == value) {
                    removeAt(i);
                    break;
                }
            }
        }
    }
}
