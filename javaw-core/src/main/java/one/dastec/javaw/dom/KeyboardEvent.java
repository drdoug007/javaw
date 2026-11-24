package one.dastec.javaw.dom;

/** A keyboard event: {@code keydown} or {@code keyup}. Valid only while its listener runs, like {@link Event}. */
public class KeyboardEvent extends Event {

    public KeyboardEvent(JSObject js) {
        super(js);
    }

    /** The key value, for example {@code "Enter"} or {@code "a"}. */
    public String key() {
        return null;
    }

    /** The physical key, for example {@code "KeyA"}. */
    public String code() {
        return null;
    }

    public boolean repeat() {
        return false;
    }

    public boolean altKey() {
        return false;
    }

    public boolean ctrlKey() {
        return false;
    }

    public boolean shiftKey() {
        return false;
    }

    public boolean metaKey() {
        return false;
    }
}
