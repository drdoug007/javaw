package com.example.javaw.dom;

/**
 * JSError
 * Typed facade for JavaScript Error runtime objects (Error, TypeError, RangeError, SyntaxError).
 */
public class JSError extends JSValue {
    public JSError() {
        super(0);
    }

    public JSError(int handle) {
        super(handle);
    }

    public static JSError of(String message) {
        return null;
    }

    public static JSError typeError(String message) {
        return null;
    }

    public static JSError rangeError(String message) {
        return null;
    }

    public static JSError syntaxError(String message) {
        return null;
    }

    public String getMessage() {
        return null;
    }

    public String getName() {
        return null;
    }

    public String getStack() {
        return null;
    }
}
