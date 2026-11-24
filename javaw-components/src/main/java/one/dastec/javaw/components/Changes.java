package one.dastec.javaw.components;

/**
 * The reactive fields changed in one update cycle, with the value each had before its first change in
 * the cycle. The first update lists every reactive field, with its type's default as the old value.
 */
public final class Changes {
    Changes() {
    }

    /** Whether the named field changed in this cycle. */
    public boolean has(String name) {
        return false;
    }

    /** The field's value before its first change in this cycle (boxed for primitives), or {@code null}. */
    public Object get(String name) {
        return null;
    }

    /** The number of changed fields. */
    public int size() {
        return 0;
    }
}
