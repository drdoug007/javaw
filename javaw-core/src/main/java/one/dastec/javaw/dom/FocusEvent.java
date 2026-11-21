package one.dastec.javaw.dom;

/** A focus event: {@code focus}, {@code blur}, {@code focusin} or {@code focusout}. Valid only while its listener runs, like {@link Event}. */
public class FocusEvent extends Event {

    public FocusEvent(JSObject js) {
        super(js);
    }

    /** The element losing or gaining focus in exchange, or {@code null}. */
    public Element relatedTarget() {
        return null;
    }
}
