package one.dastec.javaw.js;

/**
 * Standard JavaScript Web Storage (localStorage / sessionStorage) facade bound via @JS.Import.
 */
@JS.Import("localStorage")
public class JSStorage {

    public static native void setItem(String key, String value);

    public static native String getItem(String key);

    public static native void removeItem(String key);

    public static native void clear();

    public static native int length();

    public static native String key(int index);
}
