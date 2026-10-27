package one.dastec.javaw.dom;

/**
 * JSString
 * Typed facade for JavaScript string runtime values.
 */
public class JSString extends JSValue {
    public JSString() {
        super(0);
    }

    public JSString(int handle) {
        super(handle);
    }

    public static JSString of(String val) {
        return null;
    }

    public String stringValue() {
        return asString();
    }

    public int length() {
        return 0;
    }

    public String charAt(int index) {
        return null;
    }

    public String substring(int start, int end) {
        return null;
    }
}
