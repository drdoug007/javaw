package java.io;

import java.util.Objects;

/** Runtime exception wrapping an IOException thrown from a lambda or functional interface. */
public class UncheckedIOException extends RuntimeException {
    public UncheckedIOException(String message,IOException cause){super(message,Objects.requireNonNull(cause));}
    public UncheckedIOException(IOException cause){super(Objects.requireNonNull(cause));}
    public IOException getCause(){return (IOException)super.getCause();}
}
