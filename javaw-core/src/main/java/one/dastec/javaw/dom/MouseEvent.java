package one.dastec.javaw.dom;

/** A mouse event, for example {@code click} or {@code mousemove}. Valid only while its listener runs, like {@link Event}. */
public class MouseEvent extends Event {

    public MouseEvent(JSObject js) {
        super(js);
    }

    /** Horizontal position in the viewport. */
    public double clientX() {
        return 0;
    }

    /** Vertical position in the viewport. */
    public double clientY() {
        return 0;
    }

    /** Horizontal position relative to the target's padding edge. */
    public double offsetX() {
        return 0;
    }

    /** Vertical position relative to the target's padding edge. */
    public double offsetY() {
        return 0;
    }

    /** The button that changed: 0 main, 1 auxiliary, 2 secondary. */
    public int button() {
        return 0;
    }

    /** Bit mask of the buttons held down. */
    public int buttons() {
        return 0;
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
