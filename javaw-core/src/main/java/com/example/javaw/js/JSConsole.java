package com.example.javaw.js;

/**
 * Standard JavaScript console global object facade bound via @JS.Import.
 */
@JS.Import("console")
public class JSConsole {

    public static native void log(String message);

    public static native void info(String message);

    public static native void warn(String message);

    public static native void error(String message);

    public static native void debug(String message);

    public static native void trace(String message);

    public static native void time(String label);

    public static native void timeEnd(String label);

    public static native void count(String label);

    public static native void clear();
}
