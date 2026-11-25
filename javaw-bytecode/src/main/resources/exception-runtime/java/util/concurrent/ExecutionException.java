package java.util.concurrent;

public class ExecutionException extends Exception {
    protected ExecutionException() { super(); }
    protected ExecutionException(String message) { super(message); }
    public ExecutionException(String message, Throwable cause) { super(message, cause); }
    public ExecutionException(Throwable cause) { super(cause); }
}
