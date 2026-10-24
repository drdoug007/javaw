package javaw.runtime;

/** Initialization failure adapters plus module-local stack/metadata intrinsics. */
public final class ExceptionSupport {
    public static Throwable initializationException(Throwable cause) {
        if (cause instanceof Error) return cause;
        return new ExceptionInInitializerError(cause);
    }
    public static Throwable initializationFailed(Throwable cause) {
        NoClassDefFoundError failure = new NoClassDefFoundError();
        failure.initCause(cause);
        return failure;
    }
    public static final int ABI = 1;
    public static StackTraceElement[] capture(){return new StackTraceElement[0];}
    public static String className(Throwable value){return null;}
}
