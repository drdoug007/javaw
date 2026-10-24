package java.lang;

/** Value and identity operations required by bootstrap adapters. */
public final class Long extends Number implements Comparable<Long>,java.io.Serializable {
    public static final Class<Long> TYPE=(Class<Long>)javaw.runtime.ExceptionSupport.classMirror(-25);
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
}
