package javaw.runtime;

/** These two methods are replaced by module-local compiler intrinsics. */
public final class ExceptionSupport {
    public static final int ABI = 1;
    public static StackTraceElement[] capture(){return new StackTraceElement[0];}
    public static String className(Throwable value){return null;}
}
