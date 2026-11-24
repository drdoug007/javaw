package one.dastec.javaw.dom;

/** An event created with {@code new CustomEvent(type, {detail})}. Valid only while its listener runs, like {@link Event}. */
public class CustomEvent extends Event {

    public CustomEvent(JSObject js) {
        super(js);
    }

    /** The {@code detail} value. */
    public JSObject detail() {
        return null;
    }
}
