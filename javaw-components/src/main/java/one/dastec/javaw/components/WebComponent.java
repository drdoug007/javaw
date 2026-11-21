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

    /**
     * The component's template, rendered into {@link #renderRoot()} by the default {@link #update(Changes)}.
     * Return {@code html("...")}; {@code null} renders nothing.
     */
    protected Template render() {
        return null;
    }

    /**
     * A template. The argument must be a string literal or text block; {@code ${expr}} marks a binding.
     * The compiler rewrites every call, so this method never runs.
     */
    protected static Template html(String template) {
        throw new UnsupportedOperationException("html(...) must be rewritten by the javaw-components compiler extension");
    }

    /** Schedules an update in a microtask, for changes the reactive fields cannot see (in-place mutation). */
    public void requestUpdate() {
    }

    /** Whether to run this update; returning {@code false} discards the changes. */
    protected boolean shouldUpdate(Changes changes) {
        return true;
    }

    /** Before the update; field writes here join the current cycle. */
    protected void willUpdate(Changes changes) {
    }

    /**
     * Brings the DOM up to date: by default, renders {@link #render()} into {@link #renderRoot()}.
     * Override it (without calling super) for imperative rendering. Field writes here do not schedule
     * another update.
     */
    protected void update(Changes changes) {
    }

    /** After the first update only. */
    protected void firstUpdated(Changes changes) {
    }

    /** After every update; field writes here schedule a new cycle. */
    protected void updated(Changes changes) {
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
