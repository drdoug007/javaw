package java.lang;

/** Value and identity operations required by bootstrap adapters. */
public final class Double extends Number implements Comparable<Double>,java.io.Serializable {
    public static final Class<Double> TYPE=(Class<Double>)javaw.runtime.ExceptionSupport.classMirror(-27);
    private final double value;
    public Double(double value){this.value=value;}
    public Double(String text){this(parseDouble(text));}
    public static Double valueOf(String text){return new Double(parseDouble(text));}
    public static double parseDouble(String text){return javaw.runtime.FloatingNumbers.parseDouble(text);}
    public static boolean isInfinite(double value){return value==1.0/0.0 || value==-1.0/0.0;}
    public boolean isInfinite(){return isInfinite(value);}
    public static boolean isFinite(double value){return !isNaN(value) && !isInfinite(value);}
    public static double sum(double a,double b){return a+b;}
    public static double min(double a,double b){return Math.min(a,b);}
    public static double max(double a,double b){return Math.max(a,b);}

    public double doubleValue(){return value;}
    public int intValue(){return (int)value;}
    public long longValue(){return (long)value;}
    public float floatValue(){return (float)value;}
    public static Double valueOf(double value){return new Double(value);}
    public String toString(){return String.valueOf(value);}
    public static String toString(double value){return String.valueOf(value);}
    public static long doubleToRawLongBits(double value){return javaw.runtime.ExceptionSupport.doubleBits(value);}
    public static long doubleToLongBits(double value){return value!=value?0x7ff8000000000000L:doubleToRawLongBits(value);}
    public static double longBitsToDouble(long bits){return javaw.runtime.ExceptionSupport.doubleValue(bits);}
    public static boolean isNaN(double value){return value!=value;}
    public boolean isNaN(){return isNaN(value);}
    public static int compare(double left,double right){
        if(left<right)return -1;if(left>right)return 1;
        long a=doubleToLongBits(left),b=doubleToLongBits(right);return a==b?0:a<b?-1:1;
    }
    public boolean equals(Object other){return other instanceof Double && doubleToLongBits(value)==doubleToLongBits(((Double)other).value);}
    public int hashCode(){return (int)(doubleToLongBits(value)^(doubleToLongBits(value)>>>32));}
    public static int hashCode(double value){return (int)(doubleToLongBits(value)^(doubleToLongBits(value)>>>32));}
    public int compareTo(Double other){return compare(value,other.value);}
}
