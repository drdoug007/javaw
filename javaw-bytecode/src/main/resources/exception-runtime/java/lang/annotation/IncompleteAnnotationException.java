package java.lang.annotation;
public class IncompleteAnnotationException extends RuntimeException {
    private final Class<? extends Annotation> type;private final String element;
    public IncompleteAnnotationException(Class<? extends Annotation> type,String element){this.type=type;this.element=element;}
    public Class<? extends Annotation> annotationType(){return type;}public String elementName(){return element;}
}
