package dastec.one.javaw.js;

/**
 * Standard JavaScript Date object facade bound via @JS.Import.
 */
@JS.Import("Date")
public class JSDate {

    public static native JSDate create();

    public static native JSDate create(double milliseconds);

    public static native JSDate create(String dateString);

    public static native double now();

    public static native double parse(String dateString);

    public static native double UTC(int year, int month, int date, int hours, int minutes, int seconds, int ms);

    public native double getTime();

    public native int getFullYear();

    public native int getMonth();

    public native int getDate();

    public native int getDay();

    public native int getHours();

    public native int getMinutes();

    public native int getSeconds();

    public native int getMilliseconds();

    public native int getTimezoneOffset();

    public native String toISOString();

    public native String toUTCString();

    public native String toDateString();

    public native String toTimeString();

    public native String toString();
}
