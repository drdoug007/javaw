package java.util;

public class EnumSet<E extends Enum<E>> extends AbstractSet<E> implements Cloneable, java.io.Serializable {
    final Class<E> elementType;
    final Enum<?>[] universe;
    private long[] elements;

    EnumSet(Class<E> elementType, Enum<?>[] universe) {
        this.elementType = elementType;
        this.universe = universe;
        elements = new long[Math.max(1, (universe.length + 63) >>> 6)];
    }

    public static <E extends Enum<E>> EnumSet<E> noneOf(Class<E> elementType) {
        Enum<?>[] universe = elementType.getEnumConstants();
        if (universe == null)
            throw new ClassCastException(elementType + " not an enum");
        return new EnumSet<>(elementType, universe);
    }

    public static <E extends Enum<E>> EnumSet<E> allOf(Class<E> elementType) {
        EnumSet<E> result = noneOf(elementType);
        result.fillAll();
        return result;
    }

    public static <E extends Enum<E>> EnumSet<E> copyOf(EnumSet<E> s) {
        return s.clone();
    }

    public static <E extends Enum<E>> EnumSet<E> copyOf(Collection<E> c) {
        if (c instanceof EnumSet)
            return ((EnumSet<E>) c).clone();
        if (c.isEmpty())
            throw new IllegalArgumentException("Collection is empty");
        Iterator<E> i = c.iterator();
        EnumSet<E> result = of(i.next());
        while (i.hasNext())
            result.add(i.next());
        return result;
    }

    public static <E extends Enum<E>> EnumSet<E> complementOf(EnumSet<E> s) {
        EnumSet<E> result = copyOf(s);
        result.complement();
        return result;
    }

    public static <E extends Enum<E>> EnumSet<E> of(E e) {
        EnumSet<E> result = noneOf(e.getDeclaringClass());
        result.add(e);
        return result;
    }

    public static <E extends Enum<E>> EnumSet<E> of(E e1, E e2) {
        EnumSet<E> result = noneOf(e1.getDeclaringClass());
        result.add(e1);
        result.add(e2);
        return result;
    }

    public static <E extends Enum<E>> EnumSet<E> of(E e1, E e2, E e3) {
        EnumSet<E> result = noneOf(e1.getDeclaringClass());
        result.add(e1);
        result.add(e2);
        result.add(e3);
        return result;
    }

    public static <E extends Enum<E>> EnumSet<E> of(E e1, E e2, E e3, E e4) {
        EnumSet<E> result = noneOf(e1.getDeclaringClass());
        result.add(e1);
        result.add(e2);
        result.add(e3);
        result.add(e4);
        return result;
    }

    public static <E extends Enum<E>> EnumSet<E> of(E e1, E e2, E e3, E e4, E e5) {
        EnumSet<E> result = noneOf(e1.getDeclaringClass());
        result.add(e1);
        result.add(e2);
        result.add(e3);
        result.add(e4);
        result.add(e5);
        return result;
    }

    @SafeVarargs
    public static <E extends Enum<E>> EnumSet<E> of(E first, E... rest) {
        EnumSet<E> result = noneOf(first.getDeclaringClass());
        result.add(first);
        for (E e : rest)
            result.add(e);
        return result;
    }

    public static <E extends Enum<E>> EnumSet<E> range(E from, E to) {
        if (from.compareTo(to) > 0)
            throw new IllegalArgumentException(from + " > " + to);
        EnumSet<E> result = noneOf(from.getDeclaringClass());
        result.addRange(from, to);
        return result;
    }

    public EnumSet<E> clone() {
        try {
            EnumSet<E> copy = (EnumSet<E>) super.clone();
            copy.elements = elements.clone();
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
    }

    final void typeCheck(E e) {
        Class<?> eClass = e.getClass();
        if (eClass != elementType && eClass.getSuperclass() != elementType)
            throw new ClassCastException(eClass + " != " + elementType);
    }

    private void fillAll() {
        if (universe.length == 0)
            return;
        Arrays.fill(elements, -1L);
        elements[elements.length - 1] = -1L >>> -universe.length;
    }

    private void complement() {
        if (universe.length == 0)
            return;
        for (int i = 0; i < elements.length; i++)
            elements[i] = ~elements[i];
        elements[elements.length - 1] &= -1L >>> -universe.length;
    }

    private void addRange(E from, E to) {
        for (int i = from.ordinal(); i <= to.ordinal(); i++)
            elements[i >>> 6] |= 1L << i;
    }

    private boolean accepts(Object e) {
        if (e == null)
            return false;
        Class<?> eClass = e.getClass();
        return eClass == elementType || eClass.getSuperclass() == elementType;
    }

    private EnumSet<?> sameKind(Object c) {
        if (!(c instanceof EnumSet))
            return null;
        EnumSet<?> es = (EnumSet<?>) c;
        if ((es.universe.length > 64) != (universe.length > 64))
            return null;
        return es;
    }

    public Iterator<E> iterator() {
        return new Itr();
    }

    private class Itr implements Iterator<E> {
        long unseen = elements[0];
        int unseenIndex = 0;
        long lastReturned = 0;
        int lastReturnedIndex = 0;

        public boolean hasNext() {
            while (unseen == 0 && unseenIndex < elements.length - 1)
                unseen = elements[++unseenIndex];
            return unseen != 0;
        }

        public E next() {
            if (!hasNext())
                throw new NoSuchElementException();
            lastReturned = unseen & -unseen;
            lastReturnedIndex = unseenIndex;
            unseen -= lastReturned;
            return (E) universe[(lastReturnedIndex << 6) + Long.numberOfTrailingZeros(lastReturned)];
        }

        public void remove() {
            if (lastReturned == 0)
                throw new IllegalStateException();
            elements[lastReturnedIndex] &= ~lastReturned;
            lastReturned = 0;
        }
    }

    public int size() {
        int size = 0;
        for (int i = 0; i < elements.length; i++)
            size += Long.bitCount(elements[i]);
        return size;
    }

    public boolean isEmpty() {
        for (int i = 0; i < elements.length; i++)
            if (elements[i] != 0)
                return false;
        return true;
    }

    public boolean contains(Object e) {
        if (!accepts(e))
            return false;
        int o = ((Enum<?>) e).ordinal();
        return (elements[o >>> 6] & (1L << o)) != 0;
    }

    public boolean add(E e) {
        typeCheck(e);
        int o = e.ordinal();
        int w = o >>> 6;
        long before = elements[w];
        long after = before | (1L << o);
        elements[w] = after;
        return after != before;
    }

    public boolean remove(Object e) {
        if (!accepts(e))
            return false;
        int o = ((Enum<?>) e).ordinal();
        int w = o >>> 6;
        long before = elements[w];
        long after = before & ~(1L << o);
        elements[w] = after;
        return after != before;
    }

    public void clear() {
        Arrays.fill(elements, 0L);
    }

    public boolean containsAll(Collection<?> c) {
        EnumSet<?> es = sameKind(c);
        if (es == null)
            return super.containsAll(c);
        if (es.elementType != elementType)
            return es.isEmpty();
        for (int i = 0; i < elements.length; i++)
            if ((es.elements[i] & ~elements[i]) != 0)
                return false;
        return true;
    }

    public boolean addAll(Collection<? extends E> c) {
        EnumSet<?> es = sameKind(c);
        if (es == null)
            return super.addAll(c);
        if (es.elementType != elementType) {
            if (es.isEmpty())
                return false;
            throw new ClassCastException(es.elementType + " != " + elementType);
        }
        boolean changed = false;
        for (int i = 0; i < elements.length; i++) {
            long before = elements[i];
            long after = before | es.elements[i];
            if (after != before) {
                elements[i] = after;
                changed = true;
            }
        }
        return changed;
    }

    public boolean removeAll(Collection<?> c) {
        EnumSet<?> es = sameKind(c);
        if (es == null)
            return super.removeAll(c);
        if (es.elementType != elementType)
            return false;
        boolean changed = false;
        for (int i = 0; i < elements.length; i++) {
            long before = elements[i];
            long after = before & ~es.elements[i];
            if (after != before) {
                elements[i] = after;
                changed = true;
            }
        }
        return changed;
    }

    public boolean retainAll(Collection<?> c) {
        EnumSet<?> es = sameKind(c);
        if (es == null)
            return super.retainAll(c);
        if (es.elementType != elementType) {
            boolean changed = !isEmpty();
            clear();
            return changed;
        }
        boolean changed = false;
        for (int i = 0; i < elements.length; i++) {
            long before = elements[i];
            long after = before & es.elements[i];
            if (after != before) {
                elements[i] = after;
                changed = true;
            }
        }
        return changed;
    }

    public boolean equals(Object o) {
        EnumSet<?> es = sameKind(o);
        if (es == null)
            return super.equals(o);
        if (es.elementType != elementType)
            return isEmpty() && es.isEmpty();
        return Arrays.equals(elements, es.elements);
    }
}
