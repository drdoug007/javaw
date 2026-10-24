package java.lang;
public class TypeNotPresentException extends RuntimeException {
    private final String name;public TypeNotPresentException(String name,Throwable cause){super(name,cause);this.name=name;}public String typeName(){return name;}
}
