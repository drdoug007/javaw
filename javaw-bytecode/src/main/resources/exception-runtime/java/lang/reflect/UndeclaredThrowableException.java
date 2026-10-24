package java.lang.reflect;

public class UndeclaredThrowableException extends RuntimeException {
    public UndeclaredThrowableException(Throwable cause){super(cause);}
    public Throwable getUndeclaredThrowable(){return getCause();}
}
