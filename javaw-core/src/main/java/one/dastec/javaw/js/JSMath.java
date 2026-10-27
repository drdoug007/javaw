package one.dastec.javaw.js;

/**
 * Standard JavaScript Math global object facade bound via @JS.Import.
 */
@JS.Import("Math")
public class JSMath {

    public static native double sin(double x);

    public static native double cos(double x);

    public static native double tan(double x);

    public static native double asin(double x);

    public static native double acos(double x);

    public static native double atan(double x);

    public static native double atan2(double y, double x);

    public static native double sinh(double x);

    public static native double cosh(double x);

    public static native double tanh(double x);

    public static native double sqrt(double x);

    public static native double cbrt(double x);

    public static native double exp(double x);

    public static native double expm1(double x);

    public static native double log(double x);

    public static native double log10(double x);

    public static native double log2(double x);

    public static native double log1p(double x);

    public static native double pow(double base, double exponent);

    public static native double hypot(double a, double b);

    public static native double floor(double x);

    public static native double ceil(double x);

    public static native double round(double x);

    public static native double trunc(double x);

    public static native double abs(double x);

    public static native double sign(double x);

    public static native double min(double a, double b);

    public static native double max(double a, double b);

    public static native double random();

    public static native double PI();

    public static native double E();
}
