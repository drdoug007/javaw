package java.lang;

/** Single-threaded bytecode runtime; deliberately independent of the host JDK. */
public class Throwable {
    private String message;
    private Throwable cause = this;
    private Throwable[] suppressed;
    private boolean suppressionEnabled = true;
    private boolean writable = true;
    private StackTraceElement[] trace;

    public Throwable() { fillInStackTrace(); }
    public Throwable(String message) { this.message = message; fillInStackTrace(); }
    public Throwable(String message, Throwable cause) { this.message = message; this.cause = cause; fillInStackTrace(); }
    public Throwable(Throwable cause) { this(cause == null ? null : cause.toString(), cause); }
    protected Throwable(String message, Throwable cause, boolean suppression, boolean writable) {
        this.message = message; this.cause = cause; this.suppressionEnabled = suppression;
        this.writable = writable; if (writable) fillInStackTrace();
    }
    public String getMessage() { return message; }
    public String getLocalizedMessage() { return getMessage(); }
    public Throwable getCause() { return cause == this ? null : cause; }
    public Throwable initCause(Throwable value) {
        if (cause != this) throw new IllegalStateException();
        if (value == this) throw new IllegalArgumentException();
        cause = value; return this;
    }
    public final void addSuppressed(Throwable value) {
        if (value == this) throw new IllegalArgumentException();
        if (value == null) throw new NullPointerException();
        if (!suppressionEnabled) return;
        int length = suppressed == null ? 0 : suppressed.length;
        Throwable[] copy = new Throwable[length + 1];
        if (length != 0) System.arraycopy(suppressed, 0, copy, 0, length);
        copy[length] = value; suppressed = copy;
    }
    public final Throwable[] getSuppressed() {
        return suppressed == null ? new Throwable[0] : suppressed.clone();
    }
    public Throwable fillInStackTrace() {
        if (writable) trace = javaw.runtime.ExceptionSupport.capture();
        return this;
    }
    public StackTraceElement[] getStackTrace() {
        return trace == null ? new StackTraceElement[0] : trace.clone();
    }
    public void setStackTrace(StackTraceElement[] value) {
        StackTraceElement[] copy = value.clone();
        for (int i = 0; i < copy.length; i++) if (copy[i] == null) throw new NullPointerException();
        if (writable) trace = copy;
    }
    public String toString() {
        String name = javaw.runtime.ExceptionSupport.className(this);
        String detail = getLocalizedMessage();
        return detail == null ? name : name.concat(": ").concat(detail);
    }
}
