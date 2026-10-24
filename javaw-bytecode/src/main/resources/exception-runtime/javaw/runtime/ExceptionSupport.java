package javaw.runtime;

/** Initialization failure adapters plus module-local stack/metadata intrinsics. */
public final class ExceptionSupport {
    public static String concatValue(String value) { return value==null ? "null" : value; }

    public static Throwable initializationException(Throwable cause) {
        if (cause instanceof Error) return cause;
        return new ExceptionInInitializerError(cause);
    }
    public static Throwable initializationFailed(Throwable cause) {
        NoClassDefFoundError failure = new NoClassDefFoundError();
        failure.initCause(cause);
        return failure;
    }
    public static void cloneFailure() throws CloneNotSupportedException { throw new CloneNotSupportedException(); }
    public static Class<?> classMirror(int token){return null;}
    public static String typeName(int token){return null;}
    public static String simpleName(int token){return null;}
    public static String canonicalName(int token){return null;}
    public static boolean enumType(int token){return false;}
    public static int typeModifiers(int token){return 0;}
    public static Class<?> componentType(int token){return null;}
    public static Class<?> superType(int token){return null;}
    public static Class<?>[] interfaces(int token){return null;}
    public static boolean assignable(int from,int to){return false;}
    public static String objectText(Object value) {
        return value.getClass().getName().concat("@").concat(hex(value.hashCode()));
    }
    private static String hex(int value) {
        char[] chars=new char[8];int start=8;
        do {int digit=value&15;chars[--start]=(char)(digit<10?'0'+digit:'a'+digit-10);value>>>=4;}while(value!=0);
        char[] result=new char[8-start];System.arraycopy(chars,start,result,0,result.length);
        return new String(result);
    }
    public static final int ABI = 1;
    public static StackTraceElement[] capture(){return new StackTraceElement[0];}
    public static String className(Throwable value){return null;}
}
