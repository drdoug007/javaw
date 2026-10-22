package com.example.javaw.lang;

import com.example.javaw.util.Iterator;

/**
 * Implementing this interface allows an object to be the target of the enhanced
 * {@code for}-each loop statement or traversed via iterator.
 *
 * @param <T> the type of elements returned by the iterator
 */
public interface Iterable<T> {

    /**
     * Returns an iterator over elements of type {@code T}.
     *
     * @return an Iterator.
     */
    Iterator<T> iterator();
}
