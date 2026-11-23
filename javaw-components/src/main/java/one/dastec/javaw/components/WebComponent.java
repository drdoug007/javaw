package one.dastec.javaw.components;

import java.util.Map;

import one.dastec.javaw.dom.Element;
import one.dastec.javaw.dom.Event;
import one.dastec.javaw.dom.EventListener;
import one.dastec.javaw.dom.JSObject;
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

    /**
     * A keyed list: {@code key} identifies each item between renders and {@code template} renders it.
     * Reordering moves the existing DOM of each item; a repeated key is reported and its later item
     * gets its own DOM.
     */
    protected static <T> Repeat repeat(Iterable<T> items, KeyFunction<T> key, ItemTemplate<T> template) {
        return null;
    }

    /** {@code trueCase} when {@code condition} holds, otherwise nothing; only the chosen case is rendered. */
    protected static Template when(boolean condition, TemplateSupplier trueCase) {
        return null;
    }

    /** {@code trueCase} or {@code falseCase}; only the chosen case is rendered. */
    protected static Template when(boolean condition, TemplateSupplier trueCase, TemplateSupplier falseCase) {
        return null;
    }

    /**
     * Classes for a {@code class} attribute: each key whose value is {@code true} is a class. Must be the
     * only binding of the attribute; static classes around it are kept. Classes follow the map's
     * iteration order.
     */
    protected static ClassMap classMap(Map<String, Boolean> classes) {
        return null;
    }

    /**
     * Style properties for a {@code style} attribute. Keys with a dash ({@code font-size}, {@code --gap})
     * are CSS property names, other keys are camelCase style names ({@code fontSize}); a {@code null} value
     * removes the property and a value ending in {@code " !important"} is set with priority. Must be the
     * only binding of the attribute.
     */
    protected static StyleMap styleMap(Map<String, String> styles) {
        return null;
    }

    /** Binds an element to {@code ref} in element position: {@code <input ${ref(field)}>}. */
    protected static <T extends Element> RefDirective ref(Ref<T> ref) {
        return null;
    }

    /**
     * A style sheet. The argument must be a string literal or text block without {@code ${...}}. Assign it
     * to a {@code static final Css} field of a component class to style that component's shadow root.
     */
    protected static Css css(String text) {
        return Css.__of(text);
    }

    /** Renders nothing as child content and removes the attribute in an attribute binding. */
    protected static Template nothing() {
        return null;
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

    /**
     * Dispatches {@code event} from the host element as a {@code CustomEvent}. Returns
     * {@code !event.defaultPrevented}, which can only be {@code false} for a cancelable event.
     * Not available in the constructor.
     */
    protected boolean dispatch(ComponentEvent event) {
        return true;
    }

    /**
     * Listens to {@code type} events on a long-lived target such as {@link #window()} or {@link #document()}:
     * the listener is added while the component is connected and removed when it is disconnected, so the
     * target does not keep a removed component alive.
     */
    protected void listen(JSObject target, String type, EventListener<Event> listener) {
    }

    /** {@link #listen(JSObject, String, EventListener)} with a listener that ignores the event. */
    protected void listen(JSObject target, String type, Runnable listener) {
    }

    /** {@link #listen(JSObject, String, EventListener)} for an element target. */
    protected void listen(Element target, String type, EventListener<Event> listener) {
    }

    /** {@link #listen(JSObject, String, EventListener)} for an element target. */
    protected void listen(Element target, String type, Runnable listener) {
    }

    /** The global {@code window}, as a {@link #listen} target. */
    protected static JSObject window() {
        return null;
    }

    /** The {@code document}, as a {@link #listen} target. */
    protected static JSObject document() {
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
