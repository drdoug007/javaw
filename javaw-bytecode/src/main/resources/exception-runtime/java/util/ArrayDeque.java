package java.util;

import java.io.Serializable;

/**
 * A resizable ring-buffer Deque implementation that rejects null elements.
 * Follows the java.util.ArrayDeque contract with a head/size ring buffer (not the OpenJDK head/tail layout).
 */
public class ArrayDeque<E> extends AbstractCollection<E> implements Deque<E>, Cloneable, Serializable {
    private static final long serialVersionUID = 876323262645176354L;

    // The elements in the deque, logical position i lives in slot (head + i) mod elements.length
    private transient Object[] elements;
    // The index of the first element in the deque (logical position 0)
    private transient int head;
    // The number of elements in the deque
    private transient int size;

    // Default initial capacity - JDK uses 16
    private static final int MIN_INITIAL_CAPACITY = 16;

    /**
     * Constructs an empty deque with default initial capacity (16).
     */
    public ArrayDeque() {
        this.elements = new Object[MIN_INITIAL_CAPACITY];
        this.head = 0;
        this.size = 0;
    }

    /**
     * Constructs an empty deque with the specified initial capacity.
     * The actual capacity will be at least n + 1 (but never less than 1).
     *
     * @param numElements the initial capacity of the deque
     */
    public ArrayDeque(int numElements) {
        // Ensure capacity is at least 1 to prevent head overflow in addFirst
        int capacity = (numElements < 1) ? 1 : numElements + 1;
        this.elements = new Object[capacity];
        this.head = 0;
        this.size = 0;
    }

    /**
     * Constructs a deque containing the elements of the specified collection,
     * in the order they are returned by the collection's iterator.
     *
     * @param c the collection whose elements are to be placed into this deque
     * @throws NullPointerException if the specified collection is null
     */
    public ArrayDeque(Collection<? extends E> c) {
        this();
        if (c == null) {
            throw new NullPointerException();
        }
        addAll(c);
    }

    /**
     * Grows the buffer after it became full: allocates a bigger array and copies the
     * elements in logical order to slots 0..size-1.
     */
    private void grow() {
        int newCapacity = (elements.length * 2) + 2;
        if (newCapacity < 0) {
            newCapacity = Integer.MAX_VALUE;
        }
        Object[] newElements = new Object[newCapacity];
        for (int i = 0; i < size; i++) {
            newElements[i] = elements[slot(i)];
        }
        head = 0;
        elements = newElements;
    }

    /**
     * Returns the logical slot index for a given position in the deque.
     *
     * @param i the logical index
     * @return the actual array index
     */
    private int slot(int i) {
        int index = head + i;
        // Handle wraparound properly to avoid negative indices
        if (index >= elements.length) {
            index -= elements.length;
        }
        return index;
    }

    /**
     * Adds an element to the front of the deque.
     *
     * @param e the element to add
     * @throws NullPointerException if the element is null
     */
    public void addFirst(E e) {
        if (e == null) {
            throw new NullPointerException();
        }

        // Move head back by one position (wrap from 0 to elements.length-1)
        // This is the key fix - use the direct assignment instead of modulo
        head = (head == 0) ? elements.length - 1 : head - 1;

        elements[head] = e;
        size++;
        if (size == elements.length) {
            grow();
        }
    }

    /**
     * Adds an element to the end of the deque.
     *
     * @param e the element to add
     * @throws NullPointerException if the element is null
     */
    public void addLast(E e) {
        if (e == null) {
            throw new NullPointerException();
        }

        // Get the slot for the new element (at logical position size)
        int slot = slot(size);

        // Add element to the new tail position
        elements[slot] = e;
        size++;
        if (size == elements.length) {
            grow();
        }
    }

    /**
     * Inserts an element at the front of the deque.
     *
     * @param e the element to add
     * @throws NullPointerException if the element is null
     */
    public void push(E e) {
        addFirst(e);
    }

    /**
     * Removes and returns the element at the front of the deque.
     *
     * @return the element at the front of the deque
     * @throws NoSuchElementException if the deque is empty
     */
    public E pop() {
        return removeFirst();
    }

    /**
     * Adds an element to the end of the deque.
     *
     * @param e the element to add
     * @throws NullPointerException if the element is null
     */
    public boolean offerLast(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        addLast(e);
        return true;
    }

    /**
     * Adds an element to the front of the deque.
     *
     * @param e the element to add
     * @throws NullPointerException if the element is null
     */
    public boolean offerFirst(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        addFirst(e);
        return true;
    }

    /**
     * Inserts an element at the end of the deque.
     *
     * @param e the element to add
     * @throws NullPointerException if the element is null
     */
    public boolean add(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        addLast(e);
        return true;
    }

    /**
     * Inserts an element at the end of the deque.
     *
     * @param e the element to add
     * @throws NullPointerException if the element is null
     */
    public boolean offer(E e) {
        if (e == null) {
            throw new NullPointerException();
        }
        addLast(e);
        return true;
    }

    /**
     * Returns the element at the front of the deque without removing it.
     *
     * @return the element at the front of the deque, or null if the deque is empty
     */
    public E peekFirst() {
        if (size == 0) {
            return null;
        }
        return (E) elements[head];
    }

    /**
     * Returns the element at the end of the deque without removing it.
     *
     * @return the element at the end of the deque, or null if the deque is empty
     */
    public E peekLast() {
        if (size == 0) {
            return null;
        }
        int tail = slot(size - 1);
        return (E) elements[tail];
    }

    /**
     * Returns the element at the front of the deque, or null if empty.
     *
     * @return the element at the front of the deque, or null if empty
     */
    public E peek() {
        return peekFirst();
    }

    /**
     * Returns the element at the front of the deque.
     *
     * @return the element at the front of the deque
     * @throws NoSuchElementException if the deque is empty
     */
    public E element() {
        return getFirst();
    }

    /**
     * Returns the element at the front of the deque.
     *
     * @return the element at the front of the deque
     * @throws NoSuchElementException if the deque is empty
     */
    public E getFirst() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        return (E) elements[head];
    }

    /**
     * Returns the element at the end of the deque.
     *
     * @return the element at the end of the deque
     * @throws NoSuchElementException if the deque is empty
     */
    public E getLast() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        int tail = slot(size - 1);
        return (E) elements[tail];
    }

    /**
     * Removes and returns the element at the front of the deque.
     *
     * @return the element at the front of the deque
     * @throws NoSuchElementException if the deque is empty
     */
    public E removeFirst() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        E result = (E) elements[head];
        elements[head] = null; // Help GC
        head = slot(1); // Move head to next position (logical index 1)
        size--;
        return result;
    }

    /**
     * Removes and returns the element at the end of the deque.
     *
     * @return the element at the end of the deque
     * @throws NoSuchElementException if the deque is empty
     */
    public E removeLast() {
        if (size == 0) {
            throw new NoSuchElementException();
        }
        int tail = slot(size - 1);
        E result = (E) elements[tail];
        elements[tail] = null; // Help GC
        size--;
        return result;
    }

    /**
     * Removes and returns the element at the front of the deque, or null if empty.
     *
     * @return the element at the front of the deque, or null if empty
     */
    public E pollFirst() {
        if (size == 0) {
            return null;
        }
        return removeFirst();
    }

    /**
     * Removes and returns the element at the end of the deque, or null if empty.
     *
     * @return the element at the end of the deque, or null if empty
     */
    public E pollLast() {
        if (size == 0) {
            return null;
        }
        int tail = slot(size - 1);
        E result = (E) elements[tail];
        elements[tail] = null; // Help GC
        size--;
        return result;
    }

    /**
     * Removes and returns the element at the front of the deque, or null if empty.
     *
     * @return the element at the front of the deque, or null if empty
     */
    public E poll() {
        return pollFirst();
    }

    /**
     * Removes and returns the element at the front of the deque.
     *
     * @return the element at the front of the deque
     * @throws NoSuchElementException if the deque is empty
     */
    public E remove() {
        return removeFirst();
    }

    /**
     * Adds all elements from the specified collection to this deque.
     *
     * @param c the collection containing elements to add
     * @return true if this deque changed as a result of the call
     * @throws NullPointerException if the collection is null or contains null elements
     */
    public boolean addAll(Collection<? extends E> c) {
        if (c == null) {
            throw new NullPointerException();
        }

        if (c == this) {
            // The JDK adds the original elements, then reports the self-modification once its forEach sees the
            // tail move (an empty deque stays unchanged); iterating over this deque while appending would never end.
            int n = size;
            if (n == 0) {
                return false;
            }
            for (int i = 0; i < n; i++) {
                addLast((E) elements[slot(i)]);
            }
            throw new ConcurrentModificationException();
        }
        int before = size;
        for (E value : c) {
            addLast(value);
        }
        return size > before;
    }

    /**
     * Returns true if this deque contains the specified element.
     *
     * @param o the element to check for
     * @return true if this deque contains the specified element, false otherwise
     */
    public boolean contains(Object o) {
        if (o == null) {
            return false;
        }

        // Check each element in order
        for (int i = 0; i < size; i++) {
            Object element = elements[slot(i)];
            if (o.equals(element)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes the first occurrence of the specified element from this deque.
     *
     * @param o the element to remove
     * @return true if this deque contained the specified element, false otherwise
     */
    public boolean removeFirstOccurrence(Object o) {
        if (o == null) {
            return false;
        }

        for (int i = 0; i < size; i++) {
            int slot = slot(i);
            Object element = elements[slot];
            if (o.equals(element)) {
                delete(i);
                return true;
            }
        }
        return false;
    }

    /**
     * Removes the last occurrence of the specified element from this deque.
     *
     * @param o the element to remove
     * @return true if this deque contained the specified element, false otherwise
     */
    public boolean removeLastOccurrence(Object o) {
        if (o == null) {
            return false;
        }

        for (int i = size - 1; i >= 0; i--) {
            int slot = slot(i);
            Object element = elements[slot];
            if (o.equals(element)) {
                delete(i);
                return true;
            }
        }
        return false;
    }

    /**
     * Removes the first occurrence of the specified element from this deque.
     *
     * @param o the element to remove
     * @return true if this deque contained the specified element, false otherwise
     */
    public boolean remove(Object o) {
        return removeFirstOccurrence(o);
    }

    /**
     * Removes all elements from this deque.
     */
    public void clear() {
        // Null out all elements to help GC
        for (int i = 0; i < size; i++) {
            elements[slot(i)] = null;
        }
        head = 0;
        size = 0;
    }

    /**
     * Returns the number of elements in this deque.
     *
     * @return the number of elements in this deque
     */
    public int size() {
        return size;
    }

    /**
     * Returns true if this deque contains no elements.
     *
     * @return true if this deque contains no elements
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns an array containing all of the elements in this deque.
     *
     * @return an array containing all of the elements in this deque
     */
    public Object[] toArray() {
        Object[] result = new Object[size];
        for (int i = 0; i < size; i++) {
            result[i] = elements[slot(i)];
        }
        return result;
    }

    /**
     * Returns an iterator over the elements in this deque in proper sequence.
     *
     * @return an iterator over the elements in this deque in proper sequence
     */
    public Iterator<E> iterator() {
        return new Itr();
    }

    /**
     * Returns a reverse iterator over the elements in this deque in reverse sequence.
     *
     * @return a reverse iterator over the elements in this deque in reverse sequence
     */
    public Iterator<E> descendingIterator() {
        return new DescendingItr();
    }

    /**
     * Deletes the element at the specified logical index.
     *
     * @param i the logical index of the element to delete
     */
    private void delete(int i) {

        // Shift all elements after position i one place towards the head
        int j = i;
        while (j < size - 1) {
            int nextSlot = slot(j + 1);
            elements[slot(j)] = elements[nextSlot];
            j++;
        }

        // Null out the last element and decrement size
        elements[slot(size - 1)] = null;
        size--;
    }

    /**
     * Returns a shallow copy of this deque.
     *
     * @return a shallow copy of this deque
     */
    @SuppressWarnings("unchecked")
    public ArrayDeque<E> clone() {
        try {
            ArrayDeque<E> copy = (ArrayDeque<E>) super.clone();
            copy.elements = Arrays.copyOf(elements, elements.length);
            return copy;
        } catch (CloneNotSupportedException e) {
            // Should never happen as we implement Cloneable
            throw new InternalError();
        }
    }

    /**
     * Iterator implementation for the deque.
     */
    private class Itr implements Iterator<E> {
        private int next = 0;
        private int last = -1;

        @Override
        public boolean hasNext() {
            return next < size;
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            int slot = slot(next);
            last = next++;
            return (E) elements[slot];
        }

        @Override
        public void remove() {
            if (last < 0) {
                throw new IllegalStateException();
            }
            delete(last);
            next = last;
            last = -1;
        }
    }

    /**
     * Descending iterator implementation for the deque.
     */
    private class DescendingItr implements Iterator<E> {
        private int next = size - 1;
        private int last = -1;

        @Override
        public boolean hasNext() {
            return next >= 0;
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            int slot = slot(next);
            last = next--;
            return (E) elements[slot];
        }

        @Override
        public void remove() {
            if (last < 0) {
                throw new IllegalStateException();
            }
            delete(last);
            last = -1;
        }
    }

    // Methods inherited from AbstractCollection (not overridden)
    // - toArray(T[])
    // - containsAll
    // - removeAll
    // - retainAll
    // - toString
    // - removeIf and forEach (from Collection/Iterable defaults)

}