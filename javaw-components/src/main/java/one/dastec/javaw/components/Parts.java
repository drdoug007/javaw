package one.dastec.javaw.components;

import one.dastec.javaw.dom.Event;
import one.dastec.javaw.dom.EventListener;
import one.dastec.javaw.dom.FocusEvent;
import one.dastec.javaw.dom.InputEvent;
import one.dastec.javaw.dom.KeyboardEvent;
import one.dastec.javaw.dom.MouseEvent;
import one.dastec.javaw.dom.PointerEvent;
import one.dastec.javaw.dom.SubmitEvent;

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
    /** Items render like single child values and are matched by index between renders. */
    public static TemplatePart child(Iterable<?> value) { return null; }
    public static TemplatePart child(Repeat value) { return null; }
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

    // Events whose type the event name determines (docs/web-components.md, "Binding kinds").
    public static TemplatePart onMouse(Runnable listener) { return null; }
    public static TemplatePart onMouse(EventListener<MouseEvent> listener) { return null; }
    public static TemplatePart onPointer(Runnable listener) { return null; }
    public static TemplatePart onPointer(EventListener<PointerEvent> listener) { return null; }
    public static TemplatePart onKeyboard(Runnable listener) { return null; }
    public static TemplatePart onKeyboard(EventListener<KeyboardEvent> listener) { return null; }
    public static TemplatePart onFocus(Runnable listener) { return null; }
    public static TemplatePart onFocus(EventListener<FocusEvent> listener) { return null; }
    public static TemplatePart onInput(Runnable listener) { return null; }
    public static TemplatePart onInput(EventListener<InputEvent> listener) { return null; }
    public static TemplatePart onSubmit(Runnable listener) { return null; }
    public static TemplatePart onSubmit(EventListener<SubmitEvent> listener) { return null; }
}
