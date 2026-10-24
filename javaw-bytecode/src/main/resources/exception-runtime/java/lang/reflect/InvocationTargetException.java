package java.lang.reflect;

public class InvocationTargetException extends ReflectiveOperationException {
    public InvocationTargetException(Throwable cause){super();initCause(cause);}
    public Throwable getTargetException(){return getCause();}
}
