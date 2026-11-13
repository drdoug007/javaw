package java.security;

public class GeneralSecurityException extends Exception {
    public GeneralSecurityException(){super();}
    public GeneralSecurityException(String message){super(message);}
    public GeneralSecurityException(String message,Throwable cause){super(message,cause);}
    public GeneralSecurityException(Throwable cause){super(cause);}
}
