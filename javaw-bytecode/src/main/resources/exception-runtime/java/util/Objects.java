package java.util;

/** Null, equality and hashing helpers for linked bytecode. */
public final class Objects {
    private Objects() {}
    public static boolean equals(Object left,Object right){return left==right || left!=null && left.equals(right);}
    public static int hashCode(Object value){return value==null?0:value.hashCode();}
    public static <T> T requireNonNull(T value) {
        if(value==null)throw new NullPointerException();
        return value;
    }
    public static <T> T requireNonNull(T value,String message) {
        if(value==null)throw new NullPointerException(message);
        return value;
    }

    public static boolean deepEquals(Object a,Object b){return Arrays.deepElementEquals(a,b);}
    public static int hash(Object... values){return Arrays.hashCode(values);}
    public static boolean isNull(Object value){return value==null;}
    public static boolean nonNull(Object value){return value!=null;}
    public static String toString(Object value){return String.valueOf(value);}
    public static String toString(Object value,String fallback){return value==null?fallback:value.toString();}
    public static <T>T requireNonNullElse(T value,T fallback){return value==null?requireNonNull(fallback,"defaultObj"):value;}
}
