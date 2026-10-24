package java.lang.invoke;

/** Immutable, canonical signatures for the isolated, single-threaded closed-world runtime. */
public final class MethodType implements java.io.Serializable {
    private static MethodType interned;
    private final Class<?> result;
    private final Class<?>[] parameters;
    private final MethodType next;
    private MethodType(Class<?> result,Class<?>[] parameters,MethodType next) {
        this.result=result;this.parameters=parameters;this.next=next;
    }
    public static MethodType methodType(Class<?> result,Class<?>[] parameters) {
        if(result==null || parameters==null)throw new NullPointerException();
        int slots=0;
        for(Class<?> type:parameters) {
            if(type==null)throw new NullPointerException();
            if(type==void.class)throw new IllegalArgumentException();
            slots+=type==long.class || type==double.class?2:1;
        }
        if(slots>255)throw new IllegalArgumentException();
        for(MethodType type=interned;type!=null;type=type.next)if(type.matches(result,parameters))return type;
        MethodType type=new MethodType(result,parameters.clone(),interned);interned=type;return type;
    }
    public static MethodType methodType(Class<?> result){return methodType(result,new Class<?>[0]);}
    public static MethodType methodType(Class<?> result,Class<?> parameter){return methodType(result,new Class<?>[]{parameter});}
    public static MethodType methodType(Class<?> result,Class<?> first,Class<?>... rest) {
        Class<?>[] parameters=new Class<?>[rest.length+1];parameters[0]=first;
        System.arraycopy(rest,0,parameters,1,rest.length);return methodType(result,parameters);
    }
    public static MethodType methodType(Class<?> result,MethodType parameters){return methodType(result,parameters.parameters);}
    private boolean matches(Class<?> result,Class<?>[] parameters) {
        if(this.result!=result || this.parameters.length!=parameters.length)return false;
        for(int i=0;i<parameters.length;i++)if(this.parameters[i]!=parameters[i])return false;
        return true;
    }
    public boolean equals(Object other){return this==other || other instanceof MethodType && matches(((MethodType)other).result,((MethodType)other).parameters);}
    public int hashCode(){int hash=31+result.hashCode();for(Class<?> type:parameters)hash=31*hash+type.hashCode();return hash;}
    public Class<?> returnType(){return result;}
    public int parameterCount(){return parameters.length;}
    public Class<?> parameterType(int index){return parameters[index];}
    public Class<?> lastParameterType(){return parameters.length==0?void.class:parameters[parameters.length-1];}
    public Class<?>[] parameterArray(){return parameters.clone();}
    public MethodType changeReturnType(Class<?> type){return methodType(type,parameters);}
    public MethodType changeParameterType(int index,Class<?> type){
        Class<?>[] changed=parameters.clone();changed[index]=type;return methodType(result,changed);
    }
    public MethodType insertParameterTypes(int index,Class<?>... types) {
        if(index<0 || index>parameters.length)throw new IndexOutOfBoundsException();
        Class<?>[] changed=new Class<?>[parameters.length+types.length];
        System.arraycopy(parameters,0,changed,0,index);System.arraycopy(types,0,changed,index,types.length);
        System.arraycopy(parameters,index,changed,index+types.length,parameters.length-index);return methodType(result,changed);
    }
    public MethodType appendParameterTypes(Class<?>... types){return insertParameterTypes(parameters.length,types);}
    public MethodType dropParameterTypes(int start,int end) {
        if(start<0 || end<start || end>parameters.length)throw new IndexOutOfBoundsException();
        Class<?>[] changed=new Class<?>[parameters.length-(end-start)];
        System.arraycopy(parameters,0,changed,0,start);System.arraycopy(parameters,end,changed,start,parameters.length-end);
        return methodType(result,changed);
    }
    public static MethodType genericMethodType(int count){return genericMethodType(count,false);}
    public static MethodType genericMethodType(int count,boolean finalArray) {
        if(count<0 || count>255 || finalArray && count==255)throw new IllegalArgumentException();
        Class<?>[] parameters=new Class<?>[count+(finalArray?1:0)];
        for(int i=0;i<count;i++)parameters[i]=Object.class;
        if(finalArray)parameters[count]=Object[].class;
        return methodType(Object.class,parameters);
    }
    public MethodType erase(){
        Class<?>[] changed=new Class<?>[parameters.length];
        for(int i=0;i<changed.length;i++)changed[i]=parameters[i].isPrimitive()?parameters[i]:Object.class;
        return methodType(result.isPrimitive()?result:Object.class,changed);
    }
    public MethodType generic(){return genericMethodType(parameters.length);}
    public boolean hasPrimitives(){
        if(result.isPrimitive())return true;for(Class<?> type:parameters)if(type.isPrimitive())return true;return false;
    }
    public boolean hasWrappers(){
        if(primitive(result)!=result)return true;for(Class<?> type:parameters)if(type!=Void.class && primitive(type)!=type)return true;return false;
    }
    private static Class<?> wrapper(Class<?> type){
        if(type==boolean.class)return Boolean.class;if(type==byte.class)return Byte.class;if(type==char.class)return Character.class;
        if(type==short.class)return Short.class;if(type==int.class)return Integer.class;if(type==long.class)return Long.class;
        if(type==float.class)return Float.class;if(type==double.class)return Double.class;if(type==void.class)return Void.class;return type;
    }
    private static Class<?> primitive(Class<?> type){
        if(type==Boolean.class)return boolean.class;if(type==Byte.class)return byte.class;if(type==Character.class)return char.class;
        if(type==Short.class)return short.class;if(type==Integer.class)return int.class;if(type==Long.class)return long.class;
        if(type==Float.class)return float.class;if(type==Double.class)return double.class;if(type==Void.class)return void.class;return type;
    }
    public MethodType wrap(){
        Class<?>[] changed=new Class<?>[parameters.length];for(int i=0;i<changed.length;i++)changed[i]=wrapper(parameters[i]);
        return methodType(wrapper(result),changed);
    }
    public MethodType unwrap(){
        Class<?>[] changed=new Class<?>[parameters.length];for(int i=0;i<changed.length;i++) {
            Class<?> type=primitive(parameters[i]);changed[i]=type==void.class?parameters[i]:type;
        }
        return methodType(primitive(result),changed);
    }
    private static String descriptor(Class<?> type) {
        if(type.isPrimitive()) {
            if(type==boolean.class)return "Z";if(type==byte.class)return "B";if(type==char.class)return "C";
            if(type==short.class)return "S";if(type==int.class)return "I";if(type==long.class)return "J";
            if(type==float.class)return "F";if(type==double.class)return "D";return "V";
        }
        String name=type.getName();char[] chars=new char[name.length()];
        for(int i=0;i<chars.length;i++)chars[i]=name.charAt(i)=='.'?'/':name.charAt(i);
        String text=new String(chars);return type.isArray()?text:"L".concat(text).concat(";");
    }
    public String toMethodDescriptorString(){
        String text="(";for(Class<?> type:parameters)text=text.concat(descriptor(type));return text.concat(")").concat(descriptor(result));
    }
    public String toString(){
        String text="(";for(int i=0;i<parameters.length;i++)text=text.concat(i==0?"":",").concat(parameters[i].getSimpleName());
        return text.concat(")").concat(result.getSimpleName());
    }
}
