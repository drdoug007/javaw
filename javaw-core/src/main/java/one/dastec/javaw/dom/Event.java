package one.dastec.javaw.dom;

/**
 * A DOM event delivered to an instance-bound listener registered with
 * {@link Element#addEventListener(String, EventListener)}.
 *
 * <p>An {@code Event} is valid only while its listener runs. The runtime
 * releases the underlying JavaScript handle when the listener returns, so
 * storing an {@code Event} and using it later is unsupported.
 */
public class Event {

    public Event(JSObject js) {
    }

    /** The event type, for example {@code "click"}. */
    public String type() {
        return null;
    }

    /** The element the event was dispatched to, or {@code null} if it is not an element. */
    public Element target() {
        return null;
    }

    /** The element whose listener is running, or {@code null} if it is not an element. */
    public Element currentTarget() {
        return null;
    }

    public void preventDefault() {
    }

    public boolean defaultPrevented() {
        return false;
    }

    public void stopPropagation() {
    }

    /** The underlying JavaScript event object, for properties without a typed accessor. */
    public JSObject js() {
        return null;
    }
}
