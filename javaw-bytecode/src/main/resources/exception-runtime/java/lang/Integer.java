package java.lang;

/** Value and identity operations required by bootstrap adapters. */
public final class Integer extends Number implements Comparable<Integer>,java.io.Serializable {
    public static final Class<Integer> TYPE=(Class<Integer>)javaw.runtime.ExceptionSupport.classMirror(-24);
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
}
