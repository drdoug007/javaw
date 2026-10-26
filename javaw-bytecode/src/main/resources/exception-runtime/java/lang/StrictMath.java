package java.lang;

/** Selected reproducible fdlibm operations; other StrictMath APIs remain unsupported. */
public final class StrictMath {
    private StrictMath() {}
    public static double exp(double x){return Math.exp(x);}
    public static double log(double x){return Math.log(x);}
    public static double log10(double x){return Math.log10(x);}
    public static double pow(double x,double y){return Math.pow(x,y);}
    public static double sin(double x){return Math.sin(x);}
    public static double cos(double x){return Math.cos(x);}
    public static double tan(double x){return Math.tan(x);}
    public static double asin(double x){return Math.asin(x);}
    public static double acos(double x){return Math.acos(x);}
    public static double atan(double x){return Math.atan(x);}
    public static double atan2(double x,double y){return Math.atan2(x,y);}
    public static double expm1(double x){return Math.expm1(x);}
    public static double log1p(double x){return Math.log1p(x);}
    public static double sinh(double x){return Math.sinh(x);}
    public static double cosh(double x){return Math.cosh(x);}
    public static double tanh(double x){return Math.tanh(x);}
    public static double cbrt(double x){return Math.cbrt(x);}
    public static double hypot(double x,double y){return Math.hypot(x,y);}
    public static double IEEEremainder(double x,double y){return Math.IEEEremainder(x,y);}
}
