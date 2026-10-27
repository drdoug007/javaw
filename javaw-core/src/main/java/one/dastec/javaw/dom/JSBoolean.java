package one.dastec.javaw.dom;

/**
 * JSBoolean
 * Typed facade for JavaScript boolean runtime values.
 */
public class JSBoolean extends JSValue {
    public static final JSBoolean TRUE = new JSBoolean(0);
    public static final JSBoolean FALSE = new JSBoolean(0);

    public JSBoolean() {
        super(0);
    }

    public JSBoolean(int handle) {
        super(handle);
    }

    public static JSBoolean of(boolean val) {
        return null;
    }

    public boolean booleanValue() {
        return asBooleanPrimitive();
    }
}
