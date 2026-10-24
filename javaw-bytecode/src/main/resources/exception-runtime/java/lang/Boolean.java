package java.lang;

/** Value and identity operations required by bootstrap adapters. */
public final class Boolean implements Comparable<Boolean>,java.io.Serializable {
    public static final Class<Boolean> TYPE=(Class<Boolean>)javaw.runtime.ExceptionSupport.classMirror(-20);
    private final boolean value;
    public Boolean(boolean value){this.value=value;}
    public boolean booleanValue(){return value;}
    public static final Boolean TRUE=new Boolean(true),FALSE=new Boolean(false);
    public static Boolean valueOf(boolean value){return value?TRUE:FALSE;}
    public String toString(){return String.valueOf(value);}
    public static String toString(boolean value){return String.valueOf(value);}
    public static int compare(boolean left,boolean right){return left==right?0:left?1:-1;}
    public boolean equals(Object other){return other instanceof Boolean && value==((Boolean)other).value;}
    public int hashCode(){return value?1231:1237;}
    public static int hashCode(boolean value){return value?1231:1237;}
    public int compareTo(Boolean other){return compare(value,other.value);}
}
