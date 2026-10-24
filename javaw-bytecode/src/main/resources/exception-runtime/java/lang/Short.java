package java.lang;

/** Value and identity operations required by bootstrap adapters. */
public final class Short extends Number implements Comparable<Short>,java.io.Serializable {
    public static final Class<Short> TYPE=(Class<Short>)javaw.runtime.ExceptionSupport.classMirror(-23);
    public static final short MIN_VALUE=-32768,MAX_VALUE=32767;
    public Short(String text){this(parseShort(text));}
    public static short parseShort(String text){return parseShort(text,10);}
    public static short parseShort(String text,int radix){int value=Integer.parseInt(text,radix);if(value<MIN_VALUE || value>MAX_VALUE)throw new NumberFormatException();return (short)value;}
    public static Short valueOf(String text){return valueOf(parseShort(text));}
    public static Short valueOf(String text,int radix){return valueOf(parseShort(text,radix));}
    public static Short decode(String text){int value=Integer.decode(text).intValue();if(value<MIN_VALUE || value>MAX_VALUE)throw new NumberFormatException();return valueOf((short)value);}
    private final short value;
    public Short(short value){this.value=value;}
    public short shortValue(){return value;}
    public int intValue(){return (int)value;}
    public long longValue(){return (long)value;}
    public float floatValue(){return (float)value;}
    public double doubleValue(){return (double)value;}
    private static Short[] cache;
    public static Short valueOf(short value){
        if(value>=-128 && value<=127){
            if(cache==null){
                Short[] values=new Short[256];
                for(int i=0;i<values.length;i++)values[i]=new Short((short)(i-128));
                cache=values;
            }
            return cache[(int)value+128];
        }
        return new Short(value);
    }
    public String toString(){return String.valueOf((int)value);}
    public static String toString(short value){return String.valueOf((int)value);}
    public static int compare(short left,short right){return left==right?0:left<right?-1:1;}
    public boolean equals(Object other){return other instanceof Short && value==((Short)other).value;}
    public int hashCode(){return (int)value;}
    public static int hashCode(short value){return (int)value;}
    public int compareTo(Short other){return compare(value,other.value);}
}
