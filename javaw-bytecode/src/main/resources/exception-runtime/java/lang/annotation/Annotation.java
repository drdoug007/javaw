package java.lang.annotation;
public interface Annotation {boolean equals(Object value);int hashCode();String toString();Class<? extends Annotation> annotationType();}
