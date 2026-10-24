package java.lang;

/** Value and identity operations required by bootstrap adapters. */
public final class Byte extends Number implements Comparable<Byte>,java.io.Serializable {
    public static final Class<Byte> TYPE=(Class<Byte>)javaw.runtime.ExceptionSupport.classMirror(-21);
    private final byte value;
    public Byte(byte value){this.value=value;}
    public byte byteValue(){return value;}
    public int intValue(){return (int)value;}
    public long longValue(){return (long)value;}
    public float floatValue(){return (float)value;}
    public double doubleValue(){return (double)value;}
    private static Byte[] cache;
    public static Byte valueOf(byte value){
        if(value>=-128 && value<=127){
            if(cache==null){
                Byte[] values=new Byte[256];
                for(int i=0;i<values.length;i++)values[i]=new Byte((byte)(i-128));
                cache=values;
            }
            return cache[(int)value+128];
        }
        return new Byte(value);
    }
    public String toString(){return String.valueOf((int)value);}
    public static String toString(byte value){return String.valueOf((int)value);}
    public static int compare(byte left,byte right){return left==right?0:left<right?-1:1;}
    public boolean equals(Object other){return other instanceof Byte && value==((Byte)other).value;}
    public int hashCode(){return (int)value;}
    public static int hashCode(byte value){return (int)value;}
    public int compareTo(Byte other){return compare(value,other.value);}
}
