package java.lang.reflect;

import java.lang.invoke.*;

/** Metadata and invocation for explicitly generated proxy methods, not member discovery. */
public final class Method {
    private final Class<?> owner;private final String name;private final MethodType signature;private final MethodHandle target;private final int modifiers;
    public Method(Class<?> owner,String name,MethodType signature,MethodHandle target,int modifiers){
        this.owner=owner;this.name=name;this.signature=signature;this.target=target;this.modifiers=modifiers;
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
        MethodHandle handle=target;
        if((modifiers&8)==0){if(receiver==null)throw new NullPointerException();if(!owner.isInstance(receiver))throw new IllegalArgumentException();handle=handle.bindTo(receiver);}
        int count=arguments==null?0:arguments.length;if(count!=signature.parameterCount())throw new IllegalArgumentException();
        Object[] converted=new Object[count];
        try{for(int i=0;i<count;i++)converted[i]=MethodHandle.javawConvert(arguments[i],Object.class,signature.parameterType(i));}
        catch(ClassCastException e){throw new IllegalArgumentException();}catch(NullPointerException e){throw new IllegalArgumentException();}
        try{return handle.javawCall(handle.type(),converted,true);}catch(Throwable error){throw new InvocationTargetException(error);}
    }
}
