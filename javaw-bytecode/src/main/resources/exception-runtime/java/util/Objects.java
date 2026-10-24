package java.util;

/** Minimal null checks used by javac's bound method-reference adapters. */
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
}
