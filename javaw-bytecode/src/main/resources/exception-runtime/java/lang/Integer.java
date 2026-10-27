package java.lang;

/** Value and identity operations required by bootstrap adapters. */
public final class Integer extends Number implements Comparable<Integer>,java.io.Serializable {
    public static final Class<Integer> TYPE=(Class<Integer>)javaw.runtime.ExceptionSupport.classMirror(-24);
    public static final int MIN_VALUE=-2147483648,MAX_VALUE=2147483647;
    public static final int SIZE=32,BYTES=4;
    public Integer(String text){this(parseInt(text));}
    public static int parseInt(String text){return parseInt(text,10);}
    public static int parseInt(String text,int radix){
        if(text==null)throw new NumberFormatException();return (int)javaw.runtime.IntegralNumbers.parse(text,0,text.length(),radix,MIN_VALUE,MAX_VALUE);
    }
    public static int parseInt(CharSequence text,int start,int end,int radix){
        if(text==null)throw new NullPointerException();if(start<0 || end<start || end>text.length())throw new IndexOutOfBoundsException();
        return (int)javaw.runtime.IntegralNumbers.parse(text,start,end,radix,MIN_VALUE,MAX_VALUE);
    }
    public static Integer valueOf(String text){return valueOf(parseInt(text));}
    public static Integer valueOf(String text,int radix){return valueOf(parseInt(text,radix));}
    public static Integer decode(String text){return valueOf((int)javaw.runtime.IntegralNumbers.decode(text,MIN_VALUE,MAX_VALUE));}
    public static String toString(int value,int radix){return javaw.runtime.IntegralNumbers.format(value,radix);}
    public static String toHexString(int value){return javaw.runtime.IntegralNumbers.unsigned(value&0xffffffffL,4);}
    public static String toOctalString(int value){return javaw.runtime.IntegralNumbers.unsigned(value&0xffffffffL,3);}
    public static String toBinaryString(int value){return javaw.runtime.IntegralNumbers.unsigned(value&0xffffffffL,1);}
    private final int value;
    public Integer(int value){this.value=value;}
    public int intValue(){return value;}
    public long longValue(){return (long)value;}
    public float floatValue(){return (float)value;}
    public double doubleValue(){return (double)value;}
    private static Integer[] cache;
    public static Integer valueOf(int value){
        if(value>=-128 && value<=127){
            if(cache==null){
                Integer[] values=new Integer[256];
                for(int i=0;i<values.length;i++)values[i]=new Integer((int)(i-128));
                cache=values;
            }
            return cache[(int)value+128];
        }
        return new Integer(value);
    }
    public String toString(){return String.valueOf((int)value);}
    public static String toString(int value){return String.valueOf((int)value);}
    public static int signum(int value){return value==0?0:value<0?-1:1;}
    public static int compare(int left,int right){return left==right?0:left<right?-1:1;}
    public boolean equals(Object other){return other instanceof Integer && value==((Integer)other).value;}
    public int hashCode(){return (int)value;}
    public static int hashCode(int value){return (int)value;}
    public int compareTo(Integer other){return compare(value,other.value);}

    public static int bitCount(int value){value-=((value>>>1)&0x55555555);value=(value&0x33333333)+((value>>>2)&0x33333333);value=(value+(value>>>4))&0x0f0f0f0f;value+=value>>>8;value+=value>>>16;return value&63;}
    public static int numberOfLeadingZeros(int value){if(value==0)return 32;int count=0;while(value>=0){count++;value<<=1;}return count;}
    public static int numberOfTrailingZeros(int value){if(value==0)return 32;int count=0;while((value&1)==0){count++;value>>>=1;}return count;}
    public static int highestOneBit(int value){return value==0?0:MIN_VALUE>>>numberOfLeadingZeros(value);}
    public static int lowestOneBit(int value){return value&-value;}
    public static int rotateLeft(int value,int distance){return (value<<distance)|(value>>>-distance);}
    public static int rotateRight(int value,int distance){return (value>>>distance)|(value<<-distance);}
    public static int reverse(int value){int result=0;for(int i=0;i<32;i++){result=(result<<1)|(value&1);value>>>=1;}return result;}
    public static int reverseBytes(int value){return (value>>>24)|((value>>>8)&0xff00)|((value<<8)&0xff0000)|(value<<24);}
    public static int sum(int a,int b){return a+b;}
    public static int min(int a,int b){return Math.min(a,b);}
    public static int max(int a,int b){return Math.max(a,b);}


    public static long toUnsignedLong(int value){return value&0xffffffffL;}
    public static int compareUnsigned(int a,int b){return compare(a+MIN_VALUE,b+MIN_VALUE);}
    public static int divideUnsigned(int a,int b){return (int)(toUnsignedLong(a)/toUnsignedLong(b));}
    public static int remainderUnsigned(int a,int b){return (int)(toUnsignedLong(a)%toUnsignedLong(b));}
    public static String toUnsignedString(int value){return toUnsignedString(value,10);}
    public static String toUnsignedString(int value,int radix){return Long.toString(toUnsignedLong(value),radix);}
    public static int parseUnsignedInt(String text){return parseUnsignedInt(text,10);}
    public static int parseUnsignedInt(String text,int radix){if(text==null)throw new NumberFormatException();return parseUnsignedInt(text,0,text.length(),radix);}
    public static int parseUnsignedInt(CharSequence text,int start,int end,int radix){
        if(text==null)throw new NullPointerException();if(start<0 || end<start || end>text.length())throw new IndexOutOfBoundsException();
        if(start<end && text.charAt(start)=='-')throw new NumberFormatException();
        return (int)javaw.runtime.IntegralNumbers.parse(text,start,end,radix,0,0xffffffffL);
    }

    public static Integer getInteger(String name){
        String value;
        try{value=javaw.runtime.SystemProperties.getProperty(name);}
        catch(IllegalArgumentException failure){return null;}catch(NullPointerException failure){return null;}
        if(value!=null)try{return decode(value);}catch(NumberFormatException failure){}
        return null;
    }
    public static Integer getInteger(String name,int fallback){Integer value=getInteger(name);return value==null?valueOf(fallback):value;}
    public static Integer getInteger(String name,Integer fallback){Integer value=getInteger(name);return value==null?fallback:value;}
}
