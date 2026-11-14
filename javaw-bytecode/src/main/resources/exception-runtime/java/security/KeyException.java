package java.security;

public class KeyException extends GeneralSecurityException {
    public KeyException(){super();}
    public KeyException(String message){super(message);}
    public KeyException(String message,Throwable cause){super(message,cause);}
    public KeyException(Throwable cause){super(cause);}
}
