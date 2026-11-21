package one.dastec.javaw.dom;

/** An input event delivered for {@code beforeinput}. Valid only while its listener runs, like {@link Event}. */
public class InputEvent extends Event {

    public InputEvent(JSObject js) {
        super(js);
    }

    /** The inserted text, or {@code ""} when there is none. */
    public String data() {
        return null;
    }

    public String inputType() {
        return null;
    }

    public boolean isComposing() {
        return false;
    }
}
