package java.lang.reflect;
import java.lang.invoke.*;
public final class Constructor<T> extends Executable implements Member,AnnotatedElement {
    private final Class<T> owner;private final MethodType signature;private final MethodHandle target;private final int modifiers;private final Class<?>[] exceptions;
    protected String javawKey(){return "C".concat(signature.toMethodDescriptorString());}
    public boolean javawCanAccess(Object receiver,Class<?> caller){return javawAccess(owner,modifiers,caller,receiver,true);}
    public java.lang.annotation.Annotation[] getDeclaredAnnotations(){return javaw.runtime.ReflectionSupport.annotations(owner,"C".concat(signature.toMethodDescriptorString()));}
    public Constructor(Class<T> owner,MethodType signature,MethodHandle target,int modifiers,Class<?>[] exceptions){this.owner=owner;this.signature=signature;this.target=target;this.modifiers=modifiers;this.exceptions=exceptions;}
    public AnnotatedType getAnnotatedReturnType(){return javaw.runtime.AnnotatedTypes.of(owner,owner,javawKey(),20<<24,"");}
    public Class<T> getDeclaringClass(){return owner;}public String getName(){return owner.getName();}public int getModifiers(){return modifiers;}
    public boolean isSynthetic(){return (modifiers&4096)!=0;}public boolean isVarArgs(){return (modifiers&128)!=0;}
    public int getParameterCount(){return signature.parameterCount();}public Class<?>[] getParameterTypes(){return signature.parameterArray();}
    public Class<?>[] getExceptionTypes(){Class<?>[] copy=new Class<?>[exceptions.length];System.arraycopy(exceptions,0,copy,0,copy.length);return copy;}
    public boolean equals(Object other){return other instanceof Constructor && owner==((Constructor<?>)other).owner && signature.equals(((Constructor<?>)other).signature);}
    public int hashCode(){return owner.getName().hashCode();}
    public T newInstance(Object... arguments) throws InstantiationException,IllegalAccessException,InvocationTargetException {return javawNewInstance(arguments,Constructor.class);}
    public T javawNewInstance(Object[] arguments,Class<?> caller) throws InstantiationException,IllegalAccessException,InvocationTargetException {
        javawCheck(owner,modifiers,caller,null);
        if(owner.isEnum())throw new IllegalArgumentException("cannot reflectively create enum objects");
        if((owner.getModifiers()&1024)!=0)throw new InstantiationException();
        javaw.runtime.ReflectionSupport.initialize(owner);
        int count=arguments==null?0:arguments.length;if(count!=signature.parameterCount())throw new IllegalArgumentException();
        Object[] values=new Object[count];try{for(int i=0;i<count;i++)values[i]=MethodHandle.javawConvert(arguments[i],Object.class,signature.parameterType(i));}
        catch(ClassCastException error){throw new IllegalArgumentException();}catch(NullPointerException error){throw new IllegalArgumentException();}
        try{return (T)target.javawCall(target.type(),values,true);}catch(Throwable error){throw new InvocationTargetException(error);}
    }
}
