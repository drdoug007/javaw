package java.lang.reflect;
import java.lang.annotation.*;
public interface AnnotatedElement {
    Annotation[] getDeclaredAnnotations();
    default Annotation[] getAnnotations(){return getDeclaredAnnotations();}
    default <T extends Annotation> T getDeclaredAnnotation(Class<T> type){if(type==null)throw new NullPointerException();for(Annotation value:getDeclaredAnnotations())if(value.annotationType()==type)return (T)value;return null;}
    default <T extends Annotation> T getAnnotation(Class<T> type){if(type==null)throw new NullPointerException();for(Annotation value:getAnnotations())if(value.annotationType()==type)return (T)value;return null;}
    default boolean isAnnotationPresent(Class<? extends Annotation> type){return getAnnotation(type)!=null;}
    default <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> type){return javaw.runtime.AnnotationValue.byType(getDeclaredAnnotations(),type);}
    default <T extends Annotation> T[] getAnnotationsByType(Class<T> type){return getDeclaredAnnotationsByType(type);}
}
