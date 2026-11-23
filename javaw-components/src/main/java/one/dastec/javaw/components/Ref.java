package one.dastec.javaw.components;

import one.dastec.javaw.dom.Element;

/**
 * Holds the element of a {@code ${ref(r)}} binding. The renderer assigns the element when the binding is
 * first committed and clears it when the binding moves to another {@code Ref} or its template is removed.
 * Like other component nodes, the element is valid only while its component lives.
 *
 * <p>This class is a compile-time facade; the javaw-components compiler extension supplies the
 * implementation that runs in WebAssembly.
 */
public final class Ref<T extends Element> {
    public Ref() {
    }

    /** The bound element, or {@code null} before the first render and after the binding is removed. */
    public T value() {
        return null;
    }
}
