package java.lang;

public class ExceptionInInitializerError extends LinkageError {
    private Throwable exception;
    public ExceptionInInitializerError() { super(); initCause(null); }
    public ExceptionInInitializerError(String message) { super(message); initCause(null); }
    public ExceptionInInitializerError(Throwable cause) { super(); exception=cause; initCause(cause); }
    public Throwable getException() { return exception; }
}
