package java.lang;

/** Canonical closed-world type identity and basic metadata, not a reflective class loader. */
public final class Class<T> implements java.io.Serializable {
    private final int token;
    private Class(int token) { this.token=token; }
    public java.lang.reflect.Method[] getDeclaredMethods() {
        if(isPrimitive() || isArray())return new java.lang.reflect.Method[0];
        return javaw.runtime.ReflectionSupport.declaredMethods(this);
    }
    public java.lang.reflect.Method getDeclaredMethod(String name,Class<?>... parameters) throws NoSuchMethodException {
        if(name==null)throw new NullPointerException();
        int count=parameters==null?0:parameters.length;
        java.lang.reflect.Method selected=null;
        for(java.lang.reflect.Method candidate:getDeclaredMethods()) {
            if(!name.equals(candidate.getName()) || count!=candidate.getParameterCount())continue;
            Class<?>[] actual=candidate.getParameterTypes();boolean matches=true;
            for(int i=0;i<count;i++)if(parameters[i]!=actual[i])matches=false;
            if(matches && (selected==null || selected.getReturnType().isAssignableFrom(candidate.getReturnType())))selected=candidate;
        }
        if(selected==null)throw new NoSuchMethodException(name);return selected;
    }
    public ClassLoader getClassLoader() {
        if(isArray())return getComponentType().getClassLoader();if(isPrimitive())return null;
        String name=getName();
        if(name.length()>=5 && name.charAt(0)=='j' && name.charAt(1)=='a' && name.charAt(2)=='v' && name.charAt(3)=='a' && name.charAt(4)=='.')return null;
        return ClassLoader.javawApplication();
    }
    public String getName() { return javaw.runtime.ExceptionSupport.typeName(token); }
    public String getSimpleName() { return javaw.runtime.ExceptionSupport.simpleName(token); }
    public String getCanonicalName() { return javaw.runtime.ExceptionSupport.canonicalName(token); }
    public String getTypeName() {
        return isArray() ? getComponentType().getTypeName().concat("[]") : getName();
    }
    public boolean isArray() { return getComponentType()!=null; }
    public boolean isPrimitive() { return token<=-20 && token>=-28; }
    public boolean isInterface() { return (getModifiers()&512)!=0; }
    public boolean isAnnotation() { return (getModifiers()&8192)!=0; }
    public boolean isSynthetic() { return (getModifiers()&4096)!=0; }
    public boolean isEnum() { return javaw.runtime.ExceptionSupport.enumType(token); }
    public int getModifiers() { return javaw.runtime.ExceptionSupport.typeModifiers(token); }
    public Class<?> getComponentType() { return javaw.runtime.ExceptionSupport.componentType(token); }
    public Class<? super T> getSuperclass() { return (Class<? super T>)javaw.runtime.ExceptionSupport.superType(token); }
    public Class<?>[] getInterfaces() { return javaw.runtime.ExceptionSupport.interfaces(token); }
    public boolean isAssignableFrom(Class<?> other) { return javaw.runtime.ExceptionSupport.assignable(other.token,token); }
    public boolean isInstance(Object value) { return value!=null && isAssignableFrom(value.getClass()); }
    public T cast(Object value) {
        if(value!=null && !isInstance(value))throw new ClassCastException();
        return (T)value;
    }
    public <U> Class<? extends U> asSubclass(Class<U> parent) {
        if(!parent.isAssignableFrom(this))throw new ClassCastException();
        return (Class<? extends U>)this;
    }
    public String toString() {
        return isPrimitive() ? getName() : (isInterface()?"interface ":"class ").concat(getName());
    }
}
