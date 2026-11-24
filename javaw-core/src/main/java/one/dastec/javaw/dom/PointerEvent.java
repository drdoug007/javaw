package one.dastec.javaw.dom;

/** A pointer event, for example {@code pointerdown}. Valid only while its listener runs, like {@link Event}. */
public class PointerEvent extends MouseEvent {

    public PointerEvent(JSObject js) {
        super(js);
    }

    public int pointerId() {
        return 0;
    }

    /** {@code "mouse"}, {@code "pen"} or {@code "touch"}. */
    public String pointerType() {
        return null;
    }

    public double pressure() {
        return 0;
    }

    public boolean isPrimary() {
        return false;
    }
}
