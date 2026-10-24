package java.lang.annotation;
public class AnnotationTypeMismatchException extends RuntimeException {
    private final java.lang.reflect.Method element;private final String found;
    public AnnotationTypeMismatchException(java.lang.reflect.Method element,String found){this.element=element;this.found=found;}
    public java.lang.reflect.Method element(){return element;}public String foundType(){return found;}
}
