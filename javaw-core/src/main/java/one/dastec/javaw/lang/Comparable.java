package one.dastec.javaw.lang;

/**
 * Imposes a total ordering on the objects of each class that implements it.
 *
 * @param <T> the type of objects that this object may be compared to
 */
public interface Comparable<T> {

    /**
     * Compares this object with the specified object for order.
     *
     * @param o the object to be compared
     * @return a negative integer, zero, or a positive integer as this object
     *         is less than, equal to, or greater than the specified object.
     */
    int compareTo(T o);
}
