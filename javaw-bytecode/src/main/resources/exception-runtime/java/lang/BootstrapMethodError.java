package java.lang;

public class BootstrapMethodError extends LinkageError {
    public BootstrapMethodError() { super(); }
    public BootstrapMethodError(String message) { super(message); }
    public BootstrapMethodError(String message, Throwable cause) { super(message); initCause(cause); }
    public BootstrapMethodError(Throwable cause) { super(cause==null?null:cause.toString()); initCause(cause); }
}
