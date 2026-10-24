package java.lang;

/** Value and identity operations required by bootstrap adapters. */
public final class Float extends Number implements Comparable<Float>,java.io.Serializable {
    public static final Class<Float> TYPE=(Class<Float>)javaw.runtime.ExceptionSupport.classMirror(-26);
    private final float value;
    public Float(float value){this.value=value;}
    public float floatValue(){return value;}
    public int intValue(){return (int)value;}
    public long longValue(){return (long)value;}
    public double doubleValue(){return (double)value;}
    public static Float valueOf(float value){return new Float(value);}
    public String toString(){return String.valueOf(value);}
    public static String toString(float value){return String.valueOf(value);}
    public static int floatToRawIntBits(float value){return javaw.runtime.ExceptionSupport.floatBits(value);}
    public static int floatToIntBits(float value){return value!=value?0x7fc00000:floatToRawIntBits(value);}
    public static float intBitsToFloat(int bits){return javaw.runtime.ExceptionSupport.floatValue(bits);}
    public static boolean isNaN(float value){return value!=value;}
    public boolean isNaN(){return isNaN(value);}
    public static int compare(float left,float right){
        if(left<right)return -1;if(left>right)return 1;
        int a=floatToIntBits(left),b=floatToIntBits(right);return a==b?0:a<b?-1:1;
    }
    public boolean equals(Object other){return other instanceof Float && floatToIntBits(value)==floatToIntBits(((Float)other).value);}
    public int hashCode(){return floatToIntBits(value);}
    public static int hashCode(float value){return floatToIntBits(value);}
    public int compareTo(Float other){return compare(value,other.value);}
}
