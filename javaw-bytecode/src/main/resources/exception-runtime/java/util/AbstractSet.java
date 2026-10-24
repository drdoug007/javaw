package java.util;

public abstract class AbstractSet<E> extends AbstractCollection<E> implements Set<E> {
    protected AbstractSet() {}

    public boolean equals(Object other) {
        if (other == this) return true;
        if (!(other instanceof Set) || ((Set<?>) other).size() != size()) return false;
        try {
            return containsAll((Set<?>) other);
        } catch (ClassCastException failure) {
            return false;
        } catch (NullPointerException failure) {
            return false;
        }
    }

    public int hashCode() {
        int hash = 0;
        for (E value : this) hash += Objects.hashCode(value);
        return hash;
    }

    public boolean removeAll(Collection<?> values) {
        Objects.requireNonNull(values);
        boolean changed = false;
        if (size() > values.size()) {
            for (Object value : values) changed |= remove(value);
        } else {
            Iterator<E> iterator = iterator();
            while (iterator.hasNext()) {
                if (values.contains(iterator.next())) {
                    iterator.remove();
                    changed = true;
                }
            }
        }
        return changed;
    }
}
