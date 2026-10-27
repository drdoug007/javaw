package one.dastec.javaw.dom;

/**
 * JSSymbol
 * Typed facade for JavaScript Symbol runtime values.
 */
public class JSSymbol extends JSValue {
    public JSSymbol() {
        super(0);
    }

    public JSSymbol(int handle) {
        super(handle);
    }

    public static JSSymbol of(String description) {
        return null;
    }

    public static JSSymbol forSymbol(String key) {
        return null;
    }

    public String getDescription() {
        return null;
    }
}
