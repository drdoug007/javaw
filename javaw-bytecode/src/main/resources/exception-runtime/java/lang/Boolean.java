package java.lang;

/** Value and identity operations required by bootstrap adapters. */
public final class Boolean implements Comparable<Boolean>,java.io.Serializable {
    public static final Class<Boolean> TYPE=(Class<Boolean>)javaw.runtime.ExceptionSupport.classMirror(-20);
    private final boolean value;
    public Boolean(boolean value){this.value=value;}
    public Boolean(String text){this(parseBoolean(text));}
    public static Boolean valueOf(String text){return valueOf(parseBoolean(text));}
    public static boolean parseBoolean(String text){return text!=null && text.length()==4 && (text.charAt(0)=='t' || text.charAt(0)=='T') && (text.charAt(1)=='r' || text.charAt(1)=='R') && (text.charAt(2)=='u' || text.charAt(2)=='U') && (text.charAt(3)=='e' || text.charAt(3)=='E');}
    public static boolean logicalAnd(boolean a,boolean b){return a&b;}
    public static boolean logicalOr(boolean a,boolean b){return a|b;}
    public static boolean logicalXor(boolean a,boolean b){return a^b;}
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
    public static boolean getBoolean(String name){
        try{return parseBoolean(javaw.runtime.SystemProperties.getProperty(name));}
        catch(IllegalArgumentException failure){return false;}catch(NullPointerException failure){return false;}
    }
}
