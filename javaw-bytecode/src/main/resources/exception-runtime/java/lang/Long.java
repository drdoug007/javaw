package java.lang;

/** Value and identity operations required by bootstrap adapters. */
public final class Long extends Number implements Comparable<Long>,java.io.Serializable {
    public static final Class<Long> TYPE=(Class<Long>)javaw.runtime.ExceptionSupport.classMirror(-25);
    public static final long MIN_VALUE=-9223372036854775808L,MAX_VALUE=9223372036854775807L;
    public static final int SIZE=64,BYTES=8;
    public Long(String text){this(parseLong(text));}
    public static long parseLong(String text){return parseLong(text,10);}
    public static long parseLong(String text,int radix){
        if(text==null)throw new NumberFormatException();return (long)javaw.runtime.IntegralNumbers.parse(text,0,text.length(),radix,MIN_VALUE,MAX_VALUE);
    }
    public static long parseLong(CharSequence text,int start,int end,int radix){
        if(text==null)throw new NullPointerException();if(start<0 || end<start || end>text.length())throw new IndexOutOfBoundsException();
        return (long)javaw.runtime.IntegralNumbers.parse(text,start,end,radix,MIN_VALUE,MAX_VALUE);
    }
    public static Long valueOf(String text){return valueOf(parseLong(text));}
    public static Long valueOf(String text,int radix){return valueOf(parseLong(text,radix));}
    public static Long decode(String text){return valueOf((long)javaw.runtime.IntegralNumbers.decode(text,MIN_VALUE,MAX_VALUE));}
    public static String toString(long value,int radix){return javaw.runtime.IntegralNumbers.format(value,radix);}
    public static String toHexString(long value){return javaw.runtime.IntegralNumbers.unsigned(value,4);}
    public static String toOctalString(long value){return javaw.runtime.IntegralNumbers.unsigned(value,3);}
    public static String toBinaryString(long value){return javaw.runtime.IntegralNumbers.unsigned(value,1);}
    private final long value;
    public Long(long value){this.value=value;}
    public long longValue(){return value;}
    public int intValue(){return (int)value;}
    public float floatValue(){return (float)value;}
    public double doubleValue(){return (double)value;}
    private static Long[] cache;
    public static Long valueOf(long value){
        if(value>=-128 && value<=127){
            if(cache==null){
                Long[] values=new Long[256];
                for(int i=0;i<values.length;i++)values[i]=new Long((long)(i-128));
                cache=values;
            }
            return cache[(int)value+128];
        }
        return new Long(value);
    }
    public String toString(){return String.valueOf(value);}
    public static String toString(long value){return String.valueOf(value);}
    public static int compare(long left,long right){return left==right?0:left<right?-1:1;}
    public boolean equals(Object other){return other instanceof Long && value==((Long)other).value;}
    public int hashCode(){return (int)(value^(value>>>32));}
    public static int hashCode(long value){return (int)(value^(value>>>32));}
    public int compareTo(Long other){return compare(value,other.value);}

    public static int bitCount(long value){return Integer.bitCount((int)value)+Integer.bitCount((int)(value>>>32));}
    public static int numberOfLeadingZeros(long value){int high=(int)(value>>>32);return high==0?32+Integer.numberOfLeadingZeros((int)value):Integer.numberOfLeadingZeros(high);}
    public static int numberOfTrailingZeros(long value){int low=(int)value;return low==0?32+Integer.numberOfTrailingZeros((int)(value>>>32)):Integer.numberOfTrailingZeros(low);}
    public static long highestOneBit(long value){return value==0?0:MIN_VALUE>>>numberOfLeadingZeros(value);}
    public static long lowestOneBit(long value){return value&-value;}
    public static long rotateLeft(long value,int distance){return (value<<distance)|(value>>>-distance);}
    public static long rotateRight(long value,int distance){return (value>>>distance)|(value<<-distance);}
    public static long reverse(long value){return ((long)Integer.reverse((int)value)<<32)|(Integer.reverse((int)(value>>>32))&0xffffffffL);}
    public static long reverseBytes(long value){return ((long)Integer.reverseBytes((int)value)<<32)|(Integer.reverseBytes((int)(value>>>32))&0xffffffffL);}
    public static int signum(long value){return value==0?0:value<0?-1:1;}
    public static long sum(long a,long b){return a+b;}
    public static long min(long a,long b){return Math.min(a,b);}
    public static long max(long a,long b){return Math.max(a,b);}

}
