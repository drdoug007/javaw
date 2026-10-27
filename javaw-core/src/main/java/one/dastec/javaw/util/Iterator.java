package one.dastec.javaw.util;

/**
 * An iterator over a collection.
 *
 * @param <E> the type of elements returned by this iterator
 */
public interface Iterator<E> {

    /**
     * Returns {@code true} if the iteration has more elements.
     *
     * @return {@code true} if the iteration has more elements
     */
    boolean hasNext();

    /**
     * Returns the next element in the iteration.
     *
     * @return the next element in the iteration
     */
    E next();
}
