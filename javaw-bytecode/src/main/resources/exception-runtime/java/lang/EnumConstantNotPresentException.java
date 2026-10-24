package java.lang;
public class EnumConstantNotPresentException extends RuntimeException {
    private final Class<? extends Enum> type;private final String name;
    public EnumConstantNotPresentException(Class<? extends Enum> type,String name){this.type=type;this.name=name;}
    public Class<? extends Enum> enumType(){return type;}public String constantName(){return name;}
}
