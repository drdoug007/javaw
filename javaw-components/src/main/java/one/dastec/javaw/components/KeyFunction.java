package one.dastec.javaw.components;

/** The key of a list item for {@code repeat}; keys are compared with {@code equals} and {@code hashCode}. */
@FunctionalInterface
public interface KeyFunction<T> {
    Object key(T item);
}
