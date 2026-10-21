package com.example.javaw.dom;

/**
 * JSBigInt
 * Typed facade for JavaScript BigInt runtime values.
 */
public class JSBigInt extends JSValue {
    public JSBigInt() {
        super(0);
    }

    public JSBigInt(int handle) {
        super(handle);
    }

    public static JSBigInt of(long val) {
        return null;
    }

    public static JSBigInt of(String val) {
        return null;
    }

    public long longValue() {
        return 0L;
    }

    public String stringValue() {
        return asString();
    }
}
