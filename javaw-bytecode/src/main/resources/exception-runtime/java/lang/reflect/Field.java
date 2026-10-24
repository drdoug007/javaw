package java.lang.reflect;
import java.lang.invoke.*;
public final class Field extends AccessibleObject implements Member,AnnotatedElement {
    private final Class<?> owner,type;private final String name;private final int modifiers;private final MethodHandle getter,setter;
    public boolean javawCanAccess(Object receiver,Class<?> caller){return javawAccess(owner,modifiers,caller,receiver,false);}
    public java.lang.annotation.Annotation[] getDeclaredAnnotations(){return javaw.runtime.ReflectionSupport.annotations(owner,"F".concat(name).concat(javaw.runtime.ReflectionSupport.descriptor(type)));}
    public Field(Class<?> owner,String name,Class<?> type,int modifiers,MethodHandle getter,MethodHandle setter){this.owner=owner;this.name=name;this.type=type;this.modifiers=modifiers;this.getter=getter;this.setter=setter;}
    public AnnotatedType getAnnotatedType(){return javaw.runtime.AnnotatedTypes.of(getGenericType(),owner,"F".concat(name).concat(javaw.runtime.ReflectionSupport.descriptor(type)),19<<24,"");}
    public Type getGenericType(){String generic=javaw.runtime.ReflectionSupport.signature(owner,"F".concat(name).concat(javaw.runtime.ReflectionSupport.descriptor(type)));return generic==null?type:javaw.runtime.GenericSignatures.field(owner,generic);}
    public Class<?> getDeclaringClass(){return owner;}public String getName(){return name;}public Class<?> getType(){return type;}public int getModifiers(){return modifiers;}
    public boolean isSynthetic(){return (modifiers&4096)!=0;}public boolean isEnumConstant(){return (modifiers&16384)!=0;}
    public boolean equals(Object other){return other instanceof Field && owner==((Field)other).owner && name.equals(((Field)other).name) && type==((Field)other).type;}
    public String toString(){String flags=Modifier.toString(modifiers&Modifier.fieldModifiers());return (flags.length()==0?"":flags.concat(" ")).concat(type.getTypeName()).concat(" ").concat(owner.getName()).concat(".").concat(name);}
    public String toGenericString(){String flags=Modifier.toString(modifiers&Modifier.fieldModifiers());return (flags.length()==0?"":flags.concat(" ")).concat(getGenericType().getTypeName()).concat(" ").concat(owner.getName()).concat(".").concat(name);}
    public int hashCode(){return owner.getName().hashCode()^name.hashCode();}
    public Object get(Object receiver) throws IllegalAccessException{return javawGet(receiver,Field.class);}
    public void set(Object receiver,Object value) throws IllegalAccessException{javawSet(receiver,value,Field.class);}
    public Object javawGet(Object receiver,Class<?> caller) throws IllegalAccessException {
        javawCheck(owner,modifiers,caller,receiver);checkReceiver(receiver);
        if((modifiers&8)!=0)javaw.runtime.ReflectionSupport.initialize(owner);
        try{return getter.javawCall(getter.type(),(modifiers&8)!=0?new Object[0]:new Object[]{receiver},true);}
        catch(RuntimeException error){throw error;}catch(Error error){throw error;}catch(Throwable impossible){throw new IllegalStateException();}
    }
    public void javawSet(Object receiver,Object value,Class<?> caller) throws IllegalAccessException {
        javawCheck(owner,modifiers,caller,receiver);checkReceiver(receiver);
        if((modifiers&8)!=0)javaw.runtime.ReflectionSupport.initialize(owner);
        if(setter==null)throw new IllegalAccessException("final field writes are excluded from the selected reflection target");
        Object converted;try{converted=MethodHandle.javawConvert(value,Object.class,type);}catch(ClassCastException error){throw new IllegalArgumentException();}catch(NullPointerException error){throw new IllegalArgumentException();}
        try{setter.javawCall(setter.type(),(modifiers&8)!=0?new Object[]{converted}:new Object[]{receiver,converted},true);}
        catch(RuntimeException error){throw error;}catch(Error error){throw error;}catch(Throwable impossible){throw new IllegalStateException();}
    }
    private void checkReceiver(Object receiver){if((modifiers&8)==0){if(receiver==null)throw new NullPointerException();if(!owner.isInstance(receiver))throw new IllegalArgumentException();}}
    public boolean getBoolean(Object receiver) throws IllegalAccessException{return javawGetBoolean(receiver,Field.class);}
    public boolean javawGetBoolean(Object receiver,Class<?> caller) throws IllegalAccessException{
        Object value=javawGet(receiver,caller);if(!type.isPrimitive())throw new IllegalArgumentException();
        try{return ((Boolean)MethodHandle.javawConvert(value,type,boolean.class)).booleanValue();}catch(ClassCastException error){throw new IllegalArgumentException();}
    }
    public void setBoolean(Object receiver,boolean value) throws IllegalAccessException{javawSetBoolean(receiver,value,Field.class);}
    public void javawSetBoolean(Object receiver,boolean value,Class<?> caller) throws IllegalAccessException{
        if(!type.isPrimitive())throw new IllegalArgumentException();javawSet(receiver,Boolean.valueOf(value),caller);
    }
    public byte getByte(Object receiver) throws IllegalAccessException{return javawGetByte(receiver,Field.class);}
    public byte javawGetByte(Object receiver,Class<?> caller) throws IllegalAccessException{
        Object value=javawGet(receiver,caller);if(!type.isPrimitive())throw new IllegalArgumentException();
        try{return ((Byte)MethodHandle.javawConvert(value,type,byte.class)).byteValue();}catch(ClassCastException error){throw new IllegalArgumentException();}
    }
    public void setByte(Object receiver,byte value) throws IllegalAccessException{javawSetByte(receiver,value,Field.class);}
    public void javawSetByte(Object receiver,byte value,Class<?> caller) throws IllegalAccessException{
        if(!type.isPrimitive())throw new IllegalArgumentException();javawSet(receiver,Byte.valueOf(value),caller);
    }
    public char getChar(Object receiver) throws IllegalAccessException{return javawGetChar(receiver,Field.class);}
    public char javawGetChar(Object receiver,Class<?> caller) throws IllegalAccessException{
        Object value=javawGet(receiver,caller);if(!type.isPrimitive())throw new IllegalArgumentException();
        try{return ((Character)MethodHandle.javawConvert(value,type,char.class)).charValue();}catch(ClassCastException error){throw new IllegalArgumentException();}
    }
    public void setChar(Object receiver,char value) throws IllegalAccessException{javawSetChar(receiver,value,Field.class);}
    public void javawSetChar(Object receiver,char value,Class<?> caller) throws IllegalAccessException{
        if(!type.isPrimitive())throw new IllegalArgumentException();javawSet(receiver,Character.valueOf(value),caller);
    }
    public short getShort(Object receiver) throws IllegalAccessException{return javawGetShort(receiver,Field.class);}
    public short javawGetShort(Object receiver,Class<?> caller) throws IllegalAccessException{
        Object value=javawGet(receiver,caller);if(!type.isPrimitive())throw new IllegalArgumentException();
        try{return ((Short)MethodHandle.javawConvert(value,type,short.class)).shortValue();}catch(ClassCastException error){throw new IllegalArgumentException();}
    }
    public void setShort(Object receiver,short value) throws IllegalAccessException{javawSetShort(receiver,value,Field.class);}
    public void javawSetShort(Object receiver,short value,Class<?> caller) throws IllegalAccessException{
        if(!type.isPrimitive())throw new IllegalArgumentException();javawSet(receiver,Short.valueOf(value),caller);
    }
    public int getInt(Object receiver) throws IllegalAccessException{return javawGetInt(receiver,Field.class);}
    public int javawGetInt(Object receiver,Class<?> caller) throws IllegalAccessException{
        Object value=javawGet(receiver,caller);if(!type.isPrimitive())throw new IllegalArgumentException();
        try{return ((Integer)MethodHandle.javawConvert(value,type,int.class)).intValue();}catch(ClassCastException error){throw new IllegalArgumentException();}
    }
    public void setInt(Object receiver,int value) throws IllegalAccessException{javawSetInt(receiver,value,Field.class);}
    public void javawSetInt(Object receiver,int value,Class<?> caller) throws IllegalAccessException{
        if(!type.isPrimitive())throw new IllegalArgumentException();javawSet(receiver,Integer.valueOf(value),caller);
    }
    public long getLong(Object receiver) throws IllegalAccessException{return javawGetLong(receiver,Field.class);}
    public long javawGetLong(Object receiver,Class<?> caller) throws IllegalAccessException{
        Object value=javawGet(receiver,caller);if(!type.isPrimitive())throw new IllegalArgumentException();
        try{return ((Long)MethodHandle.javawConvert(value,type,long.class)).longValue();}catch(ClassCastException error){throw new IllegalArgumentException();}
    }
    public void setLong(Object receiver,long value) throws IllegalAccessException{javawSetLong(receiver,value,Field.class);}
    public void javawSetLong(Object receiver,long value,Class<?> caller) throws IllegalAccessException{
        if(!type.isPrimitive())throw new IllegalArgumentException();javawSet(receiver,Long.valueOf(value),caller);
    }
    public float getFloat(Object receiver) throws IllegalAccessException{return javawGetFloat(receiver,Field.class);}
    public float javawGetFloat(Object receiver,Class<?> caller) throws IllegalAccessException{
        Object value=javawGet(receiver,caller);if(!type.isPrimitive())throw new IllegalArgumentException();
        try{return ((Float)MethodHandle.javawConvert(value,type,float.class)).floatValue();}catch(ClassCastException error){throw new IllegalArgumentException();}
    }
    public void setFloat(Object receiver,float value) throws IllegalAccessException{javawSetFloat(receiver,value,Field.class);}
    public void javawSetFloat(Object receiver,float value,Class<?> caller) throws IllegalAccessException{
        if(!type.isPrimitive())throw new IllegalArgumentException();javawSet(receiver,Float.valueOf(value),caller);
    }
    public double getDouble(Object receiver) throws IllegalAccessException{return javawGetDouble(receiver,Field.class);}
    public double javawGetDouble(Object receiver,Class<?> caller) throws IllegalAccessException{
        Object value=javawGet(receiver,caller);if(!type.isPrimitive())throw new IllegalArgumentException();
        try{return ((Double)MethodHandle.javawConvert(value,type,double.class)).doubleValue();}catch(ClassCastException error){throw new IllegalArgumentException();}
    }
    public void setDouble(Object receiver,double value) throws IllegalAccessException{javawSetDouble(receiver,value,Field.class);}
    public void javawSetDouble(Object receiver,double value,Class<?> caller) throws IllegalAccessException{
        if(!type.isPrimitive())throw new IllegalArgumentException();javawSet(receiver,Double.valueOf(value),caller);
    }

}
