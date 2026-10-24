package java.lang.reflect;

import java.lang.invoke.*;

/** Declared application metadata and fixed proxy invocation; general reflection is incremental. */
public final class Method {
    private final Class<?> owner;private final String name;private final MethodType signature;private final MethodHandle target;private final int modifiers;
    private final Class<?>[] exceptions;
    public Method(Class<?> owner,String name,MethodType signature,int modifiers,Class<?>[] exceptions){
        this.owner=owner;this.name=name;this.signature=signature;this.target=null;this.modifiers=modifiers;this.exceptions=exceptions;
    }
    public Class<?>[] getExceptionTypes(){Class<?>[] copy=new Class<?>[exceptions.length];System.arraycopy(exceptions,0,copy,0,copy.length);return copy;}
    public Method(Class<?> owner,String name,MethodType signature,MethodHandle target,int modifiers){
        this(owner,name,signature,target,modifiers,new Class<?>[0]);
    }
    public Method(Class<?> owner,String name,MethodType signature,MethodHandle target,int modifiers,Class<?>[] exceptions){
        this.owner=owner;this.name=name;this.signature=signature;this.target=target;this.modifiers=modifiers;this.exceptions=exceptions;
    }
    public String getName(){return name;}
    public Class<?> getDeclaringClass(){return owner;}
    public Class<?> getReturnType(){return signature.returnType();}
    public Class<?>[] getParameterTypes(){return signature.parameterArray();}
    public int getParameterCount(){return signature.parameterCount();}
    public int getModifiers(){return modifiers;}
    public boolean isDefault(){return owner.isInterface() && (modifiers&(8|1024|2))==0;}
    public boolean isBridge(){return (modifiers&64)!=0;}
    public boolean isSynthetic(){return (modifiers&4096)!=0;}
    public boolean isVarArgs(){return (modifiers&128)!=0;}
    public boolean equals(Object other){return other instanceof Method && owner==((Method)other).owner && name.equals(((Method)other).name) && signature.equals(((Method)other).signature);}
    public int hashCode(){return owner.getName().hashCode()^name.hashCode();}
    public Object invoke(Object receiver,Object... arguments) throws IllegalAccessException,InvocationTargetException {
        if(target==null)throw new UnsupportedOperationException("invocation of discovered methods requires the reflective access/invocation runtime");
        MethodHandle handle=target;
        if((modifiers&8)==0){if(receiver==null)throw new NullPointerException();if(!owner.isInstance(receiver))throw new IllegalArgumentException();handle=handle.bindTo(receiver);}
        int count=arguments==null?0:arguments.length;if(count!=signature.parameterCount())throw new IllegalArgumentException();
        Object[] converted=new Object[count];
        try{for(int i=0;i<count;i++)converted[i]=MethodHandle.javawConvert(arguments[i],Object.class,signature.parameterType(i));}
        catch(ClassCastException e){throw new IllegalArgumentException();}catch(NullPointerException e){throw new IllegalArgumentException();}
        try{return handle.javawCall(handle.type(),converted,true);}catch(Throwable error){throw new InvocationTargetException(error);}
    }
}
