package com.example.javaw.js;

import com.example.javaw.dom.JSObject;
import com.example.javaw.dom.JSValue;

/**
 * Standard JavaScript JSON global object facade bound via @JS.Import.
 */
@JS.Import("JSON")
public class JSJSON {

    public static native String stringify(JSValue value);

    public static native String stringify(JSObject value);

    public static native String stringify(String text);

    public static native String stringify(Object value);

    public static native JSValue parse(String text);
}
