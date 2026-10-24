package java.lang;

/** Integral extrema used by linked core utility classes. */
public final class Math {
    private Math() {}
    public static int max(int a,int b){return a>=b?a:b;}
    public static int min(int a,int b){return a<=b?a:b;}
    public static long max(long a,long b){return a>=b?a:b;}
    public static long min(long a,long b){return a<=b?a:b;}
}
