package java.lang;

public class InternalError extends VirtualMachineError {
    public InternalError() { super(); }
    public InternalError(String message) { super(message); }
    public InternalError(String message,Throwable cause) { super(message); initCause(cause); }
    public InternalError(Throwable cause) { super(cause==null?null:cause.toString()); initCause(cause); }
}
