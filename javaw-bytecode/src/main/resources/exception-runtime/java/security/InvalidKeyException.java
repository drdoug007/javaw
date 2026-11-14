package java.security;

public class InvalidKeyException extends KeyException {
    public InvalidKeyException(){super();}
    public InvalidKeyException(String message){super(message);}
    public InvalidKeyException(String message,Throwable cause){super(message,cause);}
    public InvalidKeyException(Throwable cause){super(cause);}
}
