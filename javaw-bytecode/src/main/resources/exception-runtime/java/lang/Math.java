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


    public static int multiplyExact(int a,int b){return toIntExact((long)a*b);}
    public static long multiplyExact(long a,long b){long result=a*b;if(multiplyHigh(a,b)!=(result>>63))throw new ArithmeticException();return result;}
    public static long multiplyExact(long a,int b){return multiplyExact(a,(long)b);}
    public static long multiplyFull(int a,int b){return (long)a*b;}
    public static long multiplyHigh(long a,long b){
        long ah=a>>32,al=a&0xffffffffL,bh=b>>32,bl=b&0xffffffffL;
        long low=al*bl,t=ah*bl+(low>>>32),middle=(t&0xffffffffL)+al*bh;
        return ah*bh+(t>>32)+(middle>>32);
    }


    public static int floorDiv(int a,int b){int quotient=a/b;if((a^b)<0 && quotient*b!=a)quotient--;return quotient;}
    public static long floorDiv(long a,long b){long quotient=a/b;if((a^b)<0 && quotient*b!=a)quotient--;return quotient;}
    public static long floorDiv(long a,int b){return floorDiv(a,(long)b);}
    public static int floorMod(int a,int b){int remainder=a%b;return remainder!=0 && (a^b)<0?remainder+b:remainder;}
    public static long floorMod(long a,long b){long remainder=a%b;return remainder!=0 && (a^b)<0?remainder+b:remainder;}
    public static int floorMod(long a,int b){return (int)floorMod(a,(long)b);}


    public static int abs(int value){return value<0?-value:value;}
    public static long abs(long value){return value<0?-value:value;}
    public static int absExact(int value){return value<0?negateExact(value):value;}
    public static long absExact(long value){return value<0?negateExact(value):value;}
    public static float abs(float value){return Float.intBitsToFloat(Float.floatToRawIntBits(value)&0x7fffffff);}
    public static double abs(double value){return Double.longBitsToDouble(Double.doubleToRawLongBits(value)&0x7fffffffffffffffL);}
    public static float max(float a,float b){if(a!=a)return a;if(a==0 && b==0 && Float.floatToRawIntBits(a)<0)return b;return a>=b?a:b;}
    public static double max(double a,double b){if(a!=a)return a;if(a==0 && b==0 && Double.doubleToRawLongBits(a)<0)return b;return a>=b?a:b;}
    public static float min(float a,float b){if(a!=a)return a;if(a==0 && b==0 && Float.floatToRawIntBits(b)<0)return b;return a<=b?a:b;}
    public static double min(double a,double b){if(a!=a)return a;if(a==0 && b==0 && Double.doubleToRawLongBits(b)<0)return b;return a<=b?a:b;}
    public static float copySign(float magnitude,float sign){return Float.intBitsToFloat((Float.floatToRawIntBits(magnitude)&0x7fffffff)|(Float.floatToRawIntBits(sign)&0x80000000));}
    public static double copySign(double magnitude,double sign){return Double.longBitsToDouble((Double.doubleToRawLongBits(magnitude)&0x7fffffffffffffffL)|(Double.doubleToRawLongBits(sign)&0x8000000000000000L));}
    public static float signum(float value){return value==0 || value!=value?value:value<0?-1:1;}
    public static double signum(double value){return value==0 || value!=value?value:value<0?-1:1;}

}
