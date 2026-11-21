package one.dastec.javaw.dom;

/** A form {@code submit} event. Valid only while its listener runs, like {@link Event}. */
public class SubmitEvent extends Event {

    public SubmitEvent(JSObject js) {
        super(js);
    }

    /** The button that submitted the form, or {@code null}. */
    public Element submitter() {
        return null;
    }
}
