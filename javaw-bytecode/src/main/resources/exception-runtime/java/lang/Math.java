package java.lang;

/** Selected numeric operations for the isolated linked bytecode runtime. */
public final class Math {
    private Math() {}
    public static final double E=2.718281828459045;
    public static final double PI=3.141592653589793;
    public static double fma(double a,double b,double c){return javaw.runtime.FusedMath.fma(a,b,c);}
    public static float fma(float a,float b,float c){return javaw.runtime.FusedMath.fma(a,b,c);}
    public static double sqrt(double value){return javaw.runtime.ExceptionSupport.doubleSqrt(value);}
    public static double toRadians(double degrees){return degrees*0.017453292519943295;}
    public static double toDegrees(double radians){return radians*57.29577951308232;}
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


    public static double floor(double value){
        if(value!=value || value==0 || abs(value)>=0x1.0p52)return value;
        long integer=(long)value;return value<integer?(double)(integer-1):(double)integer;
    }
    public static double ceil(double value){
        if(value!=value || value==0 || abs(value)>=0x1.0p52)return value;
        if(value<0 && value>-1)return -0.0;
        long integer=(long)value;return value>integer?(double)(integer+1):(double)integer;
    }
    public static double rint(double value){
        if(value!=value || value==0 || abs(value)>=0x1.0p52)return value;
        double low=floor(value),fraction=value-low,result;
        if(fraction<0.5)result=low;else if(fraction>0.5)result=low+1;else result=(((long)low&1)==0)?low:low+1;
        return result==0?copySign(0.0,value):result;
    }
    public static int round(float value){
        int bits=Float.floatToRawIntBits(value),shift=149-((bits>>>23)&255);
        if((shift&-32)==0){int significand=(bits&0x7fffff)|0x800000;if(bits<0)significand=-significand;return ((significand>>shift)+1)>>1;}
        return (int)value;
    }
    public static long round(double value){
        long bits=Double.doubleToRawLongBits(value);int shift=1074-(int)((bits>>>52)&2047);
        if((shift&-64)==0){long significand=(bits&0xfffffffffffffL)|0x10000000000000L;if(bits<0)significand=-significand;return ((significand>>shift)+1)>>1;}
        return (long)value;
    }

    public static int getExponent(float value){return ((Float.floatToRawIntBits(value)>>>23)&255)-127;}
    public static int getExponent(double value){return (int)((Double.doubleToRawLongBits(value)>>>52)&2047)-1023;}
    public static float ulp(float value){
        int exponent=getExponent(value);
        if(exponent==128)return abs(value);
        if(exponent==-127)return Float.intBitsToFloat(1);
        exponent-=23;
        return Float.intBitsToFloat(exponent>=-126?(exponent+127)<<23:1<<(exponent+149));
    }
    public static double ulp(double value){
        int exponent=getExponent(value);
        if(exponent==1024)return abs(value);
        if(exponent==-1023)return Double.longBitsToDouble(1);
        exponent-=52;
        return Double.longBitsToDouble(exponent>=-1022?(long)(exponent+1023)<<52:1L<<(exponent+1074));
    }
    public static float nextUp(float value){
        if(Float.isNaN(value) || value==1.0f/0.0f)return value;
        if(value==0)return Float.intBitsToFloat(1);
        int bits=Float.floatToRawIntBits(value);return Float.intBitsToFloat(value>0?bits+1:bits-1);
    }
    public static double nextUp(double value){
        if(Double.isNaN(value) || value==1.0/0.0)return value;
        if(value==0)return Double.longBitsToDouble(1);
        long bits=Double.doubleToRawLongBits(value);return Double.longBitsToDouble(value>0?bits+1:bits-1);
    }
    public static float nextDown(float value){
        if(Float.isNaN(value) || value==-1.0f/0.0f)return value;
        if(value==0)return Float.intBitsToFloat(0x80000001);
        int bits=Float.floatToRawIntBits(value);return Float.intBitsToFloat(value>0?bits-1:bits+1);
    }
    public static double nextDown(double value){
        if(Double.isNaN(value) || value==-1.0/0.0)return value;
        if(value==0)return Double.longBitsToDouble(0x8000000000000001L);
        long bits=Double.doubleToRawLongBits(value);return Double.longBitsToDouble(value>0?bits-1:bits+1);
    }
}
