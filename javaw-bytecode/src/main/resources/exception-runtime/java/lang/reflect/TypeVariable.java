package java.lang.reflect;
public interface TypeVariable<D extends GenericDeclaration> extends Type,AnnotatedElement {Type[] getBounds();AnnotatedType[] getAnnotatedBounds();D getGenericDeclaration();String getName();}
