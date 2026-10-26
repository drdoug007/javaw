package dastec.one.javaw.dom;

/**
 * JSUndefined
 * Typed facade for JavaScript undefined runtime value.
 */
public class JSUndefined extends JSValue {
    public static final JSUndefined UNDEFINED = new JSUndefined(0);

    public JSUndefined() {
        super(0);
    }

    public JSUndefined(int handle) {
        super(handle);
    }

    public static JSUndefined getInstance() {
        return UNDEFINED;
    }
}
