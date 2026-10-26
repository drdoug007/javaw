package dastec.one.javaw.dom;

/**
 * JSNumber
 * Typed facade for JavaScript number runtime values.
 */
public class JSNumber extends JSValue {
    public JSNumber() {
        super(0);
    }

    public JSNumber(int handle) {
        super(handle);
    }

    public static JSNumber of(int val) {
        return null;
    }

    public static JSNumber of(double val) {
        return null;
    }

    public double doubleValue() {
        return asDouble();
    }

    public int intValue() {
        return asInt();
    }

    public boolean isNaN() {
        return false;
    }

    public boolean isFinite() {
        return false;
    }

    public boolean isInteger() {
        return false;
    }
}
