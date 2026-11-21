package one.dastec.javaw.components;

import one.dastec.javaw.dom.Event;
import one.dastec.javaw.dom.EventListener;

/**
 * Typed wrappers the {@code html(...)} rewrite places around each binding expression. The overload
 * that accepts an expression decides which values a binding position allows (docs/web-components.md,
 * "Binding kinds"). Not for direct use.
 */
public final class Parts {
    private Parts() {
    }

    // Child content.
    public static TemplatePart child(String value) { return null; }
    public static TemplatePart child(int value) { return null; }
    public static TemplatePart child(long value) { return null; }
    public static TemplatePart child(char value) { return null; }
    public static TemplatePart child(float value) { return null; }
    public static TemplatePart child(double value) { return null; }
    public static TemplatePart child(boolean value) { return null; }
    public static TemplatePart child(Template value) { return null; }
    /** Boxes and enums render as {@code String.valueOf}; {@code null} renders nothing. */
    public static TemplatePart child(Object value) { return null; }
    /** Lists arrive in milestone M4; this overload throws {@link UnsupportedOperationException}. */
    public static TemplatePart child(Iterable<?> value) { return null; }
    public static TemplatePart childNothing() { return null; }

    // Attribute values; null removes the attribute.
    public static TemplatePart attr(String value) { return null; }
    public static TemplatePart attr(int value) { return null; }
    public static TemplatePart attr(long value) { return null; }
    public static TemplatePart attr(char value) { return null; }
    public static TemplatePart attr(float value) { return null; }
    public static TemplatePart attr(double value) { return null; }
    public static TemplatePart attr(boolean value) { return null; }
    public static TemplatePart attr(Object value) { return null; }
    public static TemplatePart attrNothing() { return null; }

    // Boolean attributes.
    public static TemplatePart bool(boolean value) { return null; }

    // JS properties.
    public static TemplatePart prop(String value) { return null; }
    public static TemplatePart prop(boolean value) { return null; }
    public static TemplatePart prop(int value) { return null; }
    public static TemplatePart prop(char value) { return null; }
    public static TemplatePart prop(double value) { return null; }
    public static TemplatePart propNothing() { return null; }

    // Events.
    public static TemplatePart on(Runnable listener) { return null; }
    public static TemplatePart on(EventListener<Event> listener) { return null; }
    public static TemplatePart eventNothing() { return null; }
}
