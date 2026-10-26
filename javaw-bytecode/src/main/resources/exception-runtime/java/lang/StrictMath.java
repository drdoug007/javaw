package java.lang;

/** Deterministic numeric operations for the isolated linked bytecode runtime. */
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

    public static final double E=2.718281828459045;
    public static final double PI=3.141592653589793;
    public static double sqrt(double x){return Math.sqrt(x);}
    public static double toRadians(double x){return Math.toRadians(x);}
    public static double toDegrees(double x){return Math.toDegrees(x);}

    public static double floor(double x){return Math.floor(x);}
    public static double ceil(double x){return Math.ceil(x);}
    public static double rint(double x){return Math.rint(x);}
    public static long round(double x){return Math.round(x);}
    public static int round(float x){return Math.round(x);}

    public static int abs(int x){return Math.abs(x);}
    public static long abs(long x){return Math.abs(x);}
    public static float abs(float x){return Math.abs(x);}
    public static double abs(double x){return Math.abs(x);}
    public static int absExact(int x){return Math.absExact(x);}
    public static long absExact(long x){return Math.absExact(x);}

    public static int min(int a,int b){return Math.min(a,b);}
    public static long min(long a,long b){return Math.min(a,b);}
    public static float min(float a,float b){return Math.min(a,b);}
    public static double min(double a,double b){return Math.min(a,b);}
    public static int max(int a,int b){return Math.max(a,b);}
    public static long max(long a,long b){return Math.max(a,b);}
    public static float max(float a,float b){return Math.max(a,b);}
    public static double max(double a,double b){return Math.max(a,b);}

    public static float signum(float x){return Math.signum(x);}
    public static double signum(double x){return Math.signum(x);}
    public static float copySign(float a,float b){return Math.copySign(a,Float.isNaN(b)?1:b);}
    public static double copySign(double a,double b){return Math.copySign(a,Double.isNaN(b)?1:b);}

    public static int getExponent(float x){return Math.getExponent(x);}
    public static int getExponent(double x){return Math.getExponent(x);}
    public static float ulp(float x){return Math.ulp(x);}
    public static double ulp(double x){return Math.ulp(x);}
}
