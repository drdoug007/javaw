package one.dastec.javaw.components;

/** The template of one list item for {@code repeat}. */
@FunctionalInterface
public interface ItemTemplate<T> {
    Template render(T item);
}
