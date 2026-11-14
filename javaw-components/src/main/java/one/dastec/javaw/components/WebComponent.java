package one.dastec.javaw.components;

import one.dastec.javaw.dom.Element;
import one.dastec.javaw.dom.ShadowRoot;

/**
 * Base class of a custom element authored in Java; annotate the subclass with {@link CustomElement}.
 *
 * <p>The browser constructs the element and its Java object together. {@code new X()} for a component
 * class creates the element through the custom element registry. Constructors and field initializers
 * must not touch the DOM: {@link #host()} and {@link #renderRoot()} are {@code null} until the
 * element exists and, for the shadow root, until it is first connected.
 *
 * <p>This class is a compile-time facade; the javaw-components compiler extension supplies the
 * implementation that runs in WebAssembly.
 */
public abstract class WebComponent {

    protected WebComponent() {
    }

    /** Called each time the element is inserted into a document. The shadow root exists by then. */
    protected void connected() {
    }

    /** Called each time the element is removed from a document. */
    protected void disconnected() {
    }

    /** Called when the element moves to a new document. */
    protected void adopted() {
    }

    /**
     * Called when an observed attribute is added, changed or removed. {@code oldValue} and
     * {@code newValue} are {@code null} when the attribute was absent.
     */
    protected void attributeChanged(String name, String oldValue, String newValue) {
    }

    /**
     * Creates an element for this component's DOM. Unlike {@code DOM.createElement}, the element gets a
     * handle in the component's scope, so it does not keep the component alive after the component is
     * removed and collected. Not available in the constructor.
     */
    protected Element createElement(String tag) {
        return null;
    }

    /** The custom element itself. */
    public Element host() {
        return null;
    }

    /** The element's open shadow root, attached before the first {@link #connected()}. */
    public ShadowRoot renderRoot() {
        return null;
    }
}
