package java.lang;

/** Canonical closed-world type identity and basic metadata, not a reflective class loader. */
public final class Class<T> implements java.io.Serializable,java.lang.reflect.GenericDeclaration,java.lang.reflect.Type {
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
    public java.lang.reflect.Method[] getMethods(){
        if(isPrimitive())return new java.lang.reflect.Method[0];
        java.lang.reflect.Method[] own=isArray()?new java.lang.reflect.Method[0]:getDeclaredMethods();
        java.lang.reflect.Method[] inherited=getSuperclass()==null?new java.lang.reflect.Method[0]:getSuperclass().getMethods();
        java.lang.reflect.Method[] all=new java.lang.reflect.Method[own.length+inherited.length];int count=0;
        for(java.lang.reflect.Method method:own)if((method.getModifiers()&1)!=0)all[count++]=method;
        for(java.lang.reflect.Method method:inherited)all[count++]=method;
        for(Class<?> iface:getInterfaces()){
            java.lang.reflect.Method[] additional=iface.getMethods();java.lang.reflect.Method[] grown=new java.lang.reflect.Method[count+additional.length];
            System.arraycopy(all,0,grown,0,count);all=grown;
            for(java.lang.reflect.Method method:additional)if((method.getModifiers()&8)==0)all[count++]=method;
        }
        for(int i=0;i<count;i++)if(all[i]!=null)for(int j=i+1;j<count;j++)if(all[j]!=null && all[i].javawSameDescriptor(all[j])){
            Class<?> a=all[i].getDeclaringClass(),b=all[j].getDeclaringClass();
            if(a==b || b.isAssignableFrom(a) || !a.isInterface() && b.isInterface())all[j]=null;
            else if(a.isAssignableFrom(b) || a.isInterface() && !b.isInterface()){all[i]=all[j];all[j]=null;}
        }
        int size=0;for(int i=0;i<count;i++)if(all[i]!=null)size++;
        java.lang.reflect.Method[] result=new java.lang.reflect.Method[size];size=0;for(int i=0;i<count;i++)if(all[i]!=null)result[size++]=all[i];return result;
    }
    public java.lang.reflect.Method getMethod(String name,Class<?>... parameters) throws NoSuchMethodException {
        if(name==null)throw new NullPointerException();java.lang.reflect.Method selected=null;
        for(java.lang.reflect.Method method:getMethods())if(method.javawMatches(name,parameters) && (selected==null || selected.getReturnType().isAssignableFrom(method.getReturnType())))selected=method;
        if(selected==null)throw new NoSuchMethodException(name);return selected;
    }
    public java.lang.reflect.Constructor<T>[] getDeclaredConstructors(){
        if(isPrimitive() || isArray() || isInterface())return (java.lang.reflect.Constructor<T>[])new java.lang.reflect.Constructor<?>[0];
        return (java.lang.reflect.Constructor<T>[])javaw.runtime.ReflectionSupport.declaredConstructors(this);
    }
    public java.lang.reflect.Constructor<?>[] getConstructors(){
        java.lang.reflect.Constructor<?>[] all=getDeclaredConstructors();int count=0;for(java.lang.reflect.Constructor<?> value:all)if((value.getModifiers()&1)!=0)count++;
        java.lang.reflect.Constructor<?>[] result=new java.lang.reflect.Constructor<?>[count];count=0;for(java.lang.reflect.Constructor<?> value:all)if((value.getModifiers()&1)!=0)result[count++]=value;return result;
    }
    private java.lang.reflect.Constructor<T> constructor(boolean publicOnly,Class<?>[] parameters) throws NoSuchMethodException {
        int count=parameters==null?0:parameters.length;
        for(java.lang.reflect.Constructor<T> value:getDeclaredConstructors()){
            if(publicOnly && (value.getModifiers()&1)==0 || count!=value.getParameterCount())continue;
            Class<?>[] types=value.getParameterTypes();boolean match=true;for(int i=0;i<count;i++)if(parameters[i]!=types[i])match=false;if(match)return value;
        }throw new NoSuchMethodException(getName());
    }
    public java.lang.reflect.Constructor<T> getDeclaredConstructor(Class<?>... parameters) throws NoSuchMethodException{return constructor(false,parameters);}
    public java.lang.reflect.Constructor<T> getConstructor(Class<?>... parameters) throws NoSuchMethodException{return constructor(true,parameters);}
    public java.lang.reflect.Field[] getDeclaredFields(){
        if(isPrimitive() || isArray() || this==Object.class)return new java.lang.reflect.Field[0];return javaw.runtime.ReflectionSupport.declaredFields(this);
    }
    public java.lang.reflect.Field getDeclaredField(String name) throws NoSuchFieldException {
        if(name==null)throw new NullPointerException();for(java.lang.reflect.Field field:getDeclaredFields())if(name.equals(field.getName()))return field;throw new NoSuchFieldException(name);
    }
    public java.lang.reflect.Field[] getFields(){
        java.lang.reflect.Field[] own=getDeclaredFields(),parent=getSuperclass()==null?new java.lang.reflect.Field[0]:getSuperclass().getFields();int count=parent.length;
        for(java.lang.reflect.Field field:own)if((field.getModifiers()&1)!=0)count++;
        java.lang.reflect.Field[] result=new java.lang.reflect.Field[count];count=0;for(java.lang.reflect.Field field:own)if((field.getModifiers()&1)!=0)result[count++]=field;
        for(java.lang.reflect.Field field:parent)result[count++]=field;
        for(Class<?> iface:getInterfaces()){
            java.lang.reflect.Field[] additional=iface.getFields(),grown=new java.lang.reflect.Field[result.length+additional.length];System.arraycopy(result,0,grown,0,result.length);System.arraycopy(additional,0,grown,result.length,additional.length);result=grown;
        }return result;
    }
    public java.lang.reflect.Field getField(String name) throws NoSuchFieldException {
        if(name==null)throw new NullPointerException();for(java.lang.reflect.Field field:getDeclaredFields())if(name.equals(field.getName()) && (field.getModifiers()&1)!=0)return field;
        for(Class<?> iface:getInterfaces())try{return iface.getField(name);}catch(NoSuchFieldException ignored){}
        if(getSuperclass()!=null)return getSuperclass().getField(name);throw new NoSuchFieldException(name);
    }
    public java.lang.annotation.Annotation[] getDeclaredAnnotations(){return javaw.runtime.ReflectionSupport.annotations(this,"");}
    public java.lang.annotation.Annotation[] getAnnotations(){
        java.lang.annotation.Annotation[] own=getDeclaredAnnotations();Class<?> parent=getSuperclass();if(parent==null)return own;
        java.lang.annotation.Annotation[] inherited=parent.getAnnotations(),temp=new java.lang.annotation.Annotation[own.length+inherited.length];System.arraycopy(own,0,temp,0,own.length);int size=own.length;
        for(java.lang.annotation.Annotation value:inherited){boolean found=false;for(java.lang.annotation.Annotation direct:own)if(direct.annotationType()==value.annotationType())found=true;
            if(!found && value.annotationType().isAnnotationPresent(java.lang.annotation.Inherited.class))temp[size++]=value;}
        java.lang.annotation.Annotation[] result=new java.lang.annotation.Annotation[size];System.arraycopy(temp,0,result,0,size);return result;
    }
    public <A extends java.lang.annotation.Annotation> A[] getAnnotationsByType(Class<A> type){
        A[] own=getDeclaredAnnotationsByType(type);if(own.length!=0 || !type.isAnnotationPresent(java.lang.annotation.Inherited.class) || getSuperclass()==null)return own;return getSuperclass().getAnnotationsByType(type);
    }
    public T[] getEnumConstants(){
        if(!isEnum())return null;try{java.lang.reflect.Method values=getDeclaredMethod("values");values.setAccessible(true);return (T[])values.invoke(null);}catch(ReflectiveOperationException error){return null;}
    }
    public java.lang.reflect.AnnotatedType getAnnotatedSuperclass(){return javaw.runtime.AnnotatedTypes.of(getGenericSuperclass(),this,"",16<<24|65535<<8,"");}
    public java.lang.reflect.AnnotatedType[] getAnnotatedInterfaces(){java.lang.reflect.Type[] types=getGenericInterfaces();java.lang.reflect.AnnotatedType[] values=new java.lang.reflect.AnnotatedType[types.length];for(int i=0;i<values.length;i++)values[i]=javaw.runtime.AnnotatedTypes.of(types[i],this,"",16<<24|i<<8,"");return values;}
    public Class<?> getDeclaringClass(){return javaw.runtime.ReflectionSupport.declaringClass(this);}
    public java.lang.reflect.TypeVariable<?>[] getTypeParameters(){return javaw.runtime.GenericSignatures.parse(this,javaw.runtime.ReflectionSupport.signature(this,"")).variables;}
    public java.lang.reflect.Type getGenericSuperclass(){String signature=javaw.runtime.ReflectionSupport.signature(this,"");return signature==null?getSuperclass():isInterface()?null:javaw.runtime.GenericSignatures.parse(this,signature).result;}
    public java.lang.reflect.Type[] getGenericInterfaces(){String signature=javaw.runtime.ReflectionSupport.signature(this,"");return signature==null?getInterfaces():javaw.runtime.GenericSignatures.parse(this,signature).interfaces;}
    public T newInstance() throws InstantiationException,IllegalAccessException {return javawNewInstance(Class.class);}
    public T javawNewInstance(Class<?> caller) throws InstantiationException,IllegalAccessException {
        try{return getDeclaredConstructor().javawNewInstance(new Object[0],caller);}
        catch(NoSuchMethodException missing){InstantiationException failure=new InstantiationException(getName());failure.initCause(missing);throw failure;}
        catch(java.lang.reflect.InvocationTargetException target){return Class.<RuntimeException,T>javawThrow(target.getCause());}
    }
    private static <E extends Throwable,R> R javawThrow(Throwable failure) throws E {throw (E)failure;}
    public Class<?>[] getDeclaredClasses(){if(isPrimitive() || isArray() || this==Object.class)return new Class<?>[0];return javaw.runtime.ReflectionSupport.declaredClasses(this);}
    public Class<?>[] getClasses(){Class<?>[] own=getDeclaredClasses(),parent=getSuperclass()==null?new Class<?>[0]:getSuperclass().getClasses();int count=parent.length;for(Class<?> value:own)if((value.getModifiers()&1)!=0)count++;Class<?>[] result=new Class<?>[count];count=0;for(Class<?> value:own)if((value.getModifiers()&1)!=0)result[count++]=value;for(Class<?> value:parent)result[count++]=value;return result;}
    public Class<?> getEnclosingClass(){String[] enclosing=javaw.runtime.ReflectionSupport.enclosing(this);return enclosing==null?getDeclaringClass():javaw.runtime.ReflectionSupport.resolve(enclosing[0]);}
    public java.lang.reflect.Method getEnclosingMethod(){String[] enclosing=javaw.runtime.ReflectionSupport.enclosing(this);if(enclosing==null || enclosing[1]==null || enclosing[1].equals("<init>") || enclosing[1].equals("<clinit>"))return null;for(java.lang.reflect.Method method:getEnclosingClass().getDeclaredMethods())if(method.javawMetadataKey().equals("M".concat(enclosing[1]).concat(enclosing[2])))return method;return null;}
    public java.lang.reflect.Constructor<?> getEnclosingConstructor(){String[] enclosing=javaw.runtime.ReflectionSupport.enclosing(this);if(enclosing==null || !"<init>".equals(enclosing[1]))return null;for(java.lang.reflect.Constructor<?> constructor:getEnclosingClass().getDeclaredConstructors())if(constructor.javawMetadataKey().equals("C".concat(enclosing[2])))return constructor;return null;}
    public boolean isMemberClass(){return getDeclaringClass()!=null;}public boolean isAnonymousClass(){return getEnclosingClass()!=null && getSimpleName().length()==0;}public boolean isLocalClass(){return getEnclosingClass()!=null && getDeclaringClass()==null && !isAnonymousClass();}
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
