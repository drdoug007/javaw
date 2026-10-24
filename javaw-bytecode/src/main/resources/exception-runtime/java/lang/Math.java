package java.lang;

/** Integral extrema used by linked core utility classes. */
public final class Math {
    private Math() {}
    public static int max(int a,int b){return a>=b?a:b;}
    public static int min(int a,int b){return a<=b?a:b;}
    public static long max(long a,long b){return a>=b?a:b;}
    public static long min(long a,long b){return a<=b?a:b;}

    public static int addExact(int a,int b){int result=a+b;if(((a^result)&(b^result))<0)throw new ArithmeticException();return result;}
    public static long addExact(long a,long b){long result=a+b;if(((a^result)&(b^result))<0)throw new ArithmeticException();return result;}
    public static int subtractExact(int a,int b){int result=a-b;if(((a^b)&(a^result))<0)throw new ArithmeticException();return result;}
    public static long subtractExact(long a,long b){long result=a-b;if(((a^b)&(a^result))<0)throw new ArithmeticException();return result;}
    public static int incrementExact(int value){return addExact(value,1);}
    public static long incrementExact(long value){return addExact(value,1);}
    public static int decrementExact(int value){return subtractExact(value,1);}
    public static long decrementExact(long value){return subtractExact(value,1);}
    public static int negateExact(int value){if(value==Integer.MIN_VALUE)throw new ArithmeticException();return -value;}
    public static long negateExact(long value){if(value==Long.MIN_VALUE)throw new ArithmeticException();return -value;}
    public static int toIntExact(long value){int result=(int)value;if(result!=value)throw new ArithmeticException();return result;}

}
