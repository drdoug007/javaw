package com.example.javaw.dom;

/**
 * JSNull
 * Typed facade for JavaScript null runtime value.
 */
public class JSNull extends JSValue {
    public static final JSNull NULL = new JSNull(0);

    public JSNull() {
        super(0);
    }

    public JSNull(int handle) {
        super(handle);
    }

    public static JSNull getInstance() {
        return NULL;
    }
}
