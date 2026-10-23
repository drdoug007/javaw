interface Annotation { public Class<?> annotationType(); }
interface AnnotatedElement {
    public Annotation[] getDeclaredAnnotations();
    public Annotation[] getAnnotations();
    public <A extends Annotation> A getAnnotation(Class<A> type);
    public boolean isAnnotationPresent(Class<? extends Annotation> type);
}
class ReflectiveOperationException extends Exception {
    public ReflectiveOperationException() { super(); }
    public ReflectiveOperationException(String message) { super(message); }
}
class NoSuchFieldException extends ReflectiveOperationException {
    public NoSuchFieldException(String message) { super(message); }
}
class NoSuchMethodException extends ReflectiveOperationException {
    public NoSuchMethodException(String message) { super(message); }
}
class IllegalAccessException extends ReflectiveOperationException { public IllegalAccessException() { super(); } }
class InstantiationException extends ReflectiveOperationException { public InstantiationException() { super(); } }
class ClassNotFoundException extends ReflectiveOperationException {
    public ClassNotFoundException(String message) { super(message); }
}
class InvocationTargetException extends ReflectiveOperationException {
    private Throwable target;
    public InvocationTargetException(Throwable target) { super(); this.target=target; }
    public Throwable getTargetException() { return target; }
    public Throwable getCause() { return target; }
}
class AccessibleObject {
    private boolean overrideAccess;
    public void setAccessible(boolean flag) { overrideAccess=flag; }
    public boolean trySetAccessible() { overrideAccess=true; return true; }
    public boolean isAccessible() { return overrideAccess; }
    protected void __javaw_check_access(int modifiers) {
        if(!overrideAccess && (modifiers & 1)==0) throw new IllegalAccessException();
    }
}
abstract class __JavawMember extends AccessibleObject implements AnnotatedElement {
    protected int memberId; protected int memberModifiers; protected String memberName; protected Class<?> memberOwner;
    public String getName() { return memberName; }
    public Class<?> getDeclaringClass() { return memberOwner; }
    public int getModifiers() { return memberModifiers; }
    public boolean isSynthetic() { return false; }
    public Annotation[] getDeclaredAnnotations() { return __JavawReflection.__javaw_member_annotations(memberId); }
    public Annotation[] getAnnotations() { return getDeclaredAnnotations(); }
    public <A extends Annotation> A getAnnotation(Class<A> type) {
        if(type==null) throw new NullPointerException();
        for(Annotation annotation:getAnnotations()) if(annotation.annotationType()==type) return (A)annotation;
        return null;
    }
    public boolean isAnnotationPresent(Class<? extends Annotation> type) { return getAnnotation(type)!=null; }
}
class Field extends __JavawMember {
    private Class<?> fieldType;
    public Field(int id,String name,Class<?> owner,Class<?> type,int modifiers) {
        memberId=id; memberName=name; memberOwner=owner; fieldType=type; memberModifiers=modifiers;
    }
    public Class<?> getType() { return fieldType; }
    public Object get(Object receiver) { __javaw_check_access(memberModifiers); return __JavawReflection.__javaw_field_get(memberId,receiver); }
    public void set(Object receiver,Object value) {
        __javaw_check_access(memberModifiers);
        if((memberModifiers & 16)!=0) throw new IllegalAccessException();
        __JavawReflection.__javaw_field_set(memberId,receiver,value);
    }
    public boolean getBoolean(Object receiver) { return __JavawReflection.__javaw_as_boolean(get(receiver)); }
    public byte getByte(Object receiver) { return __JavawReflection.__javaw_as_byte(get(receiver)); }
    public short getShort(Object receiver) { return __JavawReflection.__javaw_as_short(get(receiver)); }
    public char getChar(Object receiver) { return __JavawReflection.__javaw_as_char(get(receiver)); }
    public int getInt(Object receiver) { return __JavawReflection.__javaw_as_int(get(receiver)); }
    public long getLong(Object receiver) { return __JavawReflection.__javaw_as_long(get(receiver)); }
    public float getFloat(Object receiver) { return __JavawReflection.__javaw_as_float(get(receiver)); }
    public double getDouble(Object receiver) { return __JavawReflection.__javaw_as_double(get(receiver)); }
    public void setBoolean(Object receiver,boolean value) { set(receiver,Boolean.valueOf(value)); }
    public void setByte(Object receiver,byte value) { set(receiver,Byte.valueOf(value)); }
    public void setShort(Object receiver,short value) { set(receiver,Short.valueOf(value)); }
    public void setChar(Object receiver,char value) { set(receiver,Character.valueOf(value)); }
    public void setInt(Object receiver,int value) { set(receiver,Integer.valueOf(value)); }
    public void setLong(Object receiver,long value) { set(receiver,Long.valueOf(value)); }
    public void setFloat(Object receiver,float value) { set(receiver,Float.valueOf(value)); }
    public void setDouble(Object receiver,double value) { set(receiver,Double.valueOf(value)); }
}
abstract class __JavawExecutable extends __JavawMember {
    protected Class<?>[] parameterTypes; protected boolean variableArity;
    public Class<?>[] getParameterTypes() {
        Class<?>[] copy=new Class<?>[parameterTypes.length];
        for(int i=0;i<copy.length;i++) copy[i]=parameterTypes[i]; return copy;
    }
    public int getParameterCount() { return parameterTypes.length; }
    public boolean isVarArgs() { return variableArity; }
}
class Method extends __JavawExecutable {
    private Class<?> resultType;
    public Method(int id,String name,Class<?> owner,Class<?> result,Class<?>[] parameters,int modifiers,boolean varargs) {
        memberId=id; memberName=name; memberOwner=owner; resultType=result; parameterTypes=parameters; memberModifiers=modifiers; variableArity=varargs;
    }
    public Class<?> getReturnType() { return resultType; }
    public Object invoke(Object receiver,Object... arguments) {
        __javaw_check_access(memberModifiers); return __JavawReflection.__javaw_method_invoke(memberId,receiver,arguments);
    }
}
class Constructor<T> extends __JavawExecutable {
    public Constructor(int id,String name,Class<?> owner,Class<?>[] parameters,int modifiers,boolean varargs) {
        memberId=id; memberName=name; memberOwner=owner; parameterTypes=parameters; memberModifiers=modifiers; variableArity=varargs;
    }
    public T newInstance(Object... arguments) {
        __javaw_check_access(memberModifiers); return (T)__JavawReflection.__javaw_construct(memberId,arguments);
    }
}
class Modifier {
    public static boolean isPublic(int flags) { return (flags & 1)!=0; }
    public static boolean isPrivate(int flags) { return (flags & 2)!=0; }
    public static boolean isProtected(int flags) { return (flags & 4)!=0; }
    public static boolean isStatic(int flags) { return (flags & 8)!=0; }
    public static boolean isFinal(int flags) { return (flags & 16)!=0; }
    public static boolean isAbstract(int flags) { return (flags & 1024)!=0; }
    public static boolean isInterface(int flags) { return (flags & 512)!=0; }
}
