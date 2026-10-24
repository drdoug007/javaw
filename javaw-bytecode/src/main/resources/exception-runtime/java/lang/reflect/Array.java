package java.lang.reflect;
import java.lang.invoke.MethodHandle;
public final class Array {
    private Array(){}
    public static Object newInstance(Class<?> component,int length){
        if(component==null)throw new NullPointerException();if(length<0)throw new NegativeArraySizeException();
        if(component==void.class)throw new IllegalArgumentException();
        if(component==boolean.class)return new boolean[length];if(component==byte.class)return new byte[length];if(component==char.class)return new char[length];if(component==short.class)return new short[length];
        if(component==int.class)return new int[length];if(component==long.class)return new long[length];if(component==float.class)return new float[length];if(component==double.class)return new double[length];
        return javaw.runtime.ReflectionSupport.newArray(component,length);
    }
    public static Object newInstance(Class<?> component,int... dimensions){
        if(component==null || dimensions==null)throw new NullPointerException();if(dimensions.length==0 || dimensions.length>255 || component==void.class)throw new IllegalArgumentException();
        int existing=0;for(Class<?> type=component;type.isArray();type=type.getComponentType())existing++;if(existing+dimensions.length>255)throw new IllegalArgumentException();
        for(int size:dimensions)if(size<0)throw new NegativeArraySizeException();return multi(component,dimensions,0);
    }
    private static Object multi(Class<?> component,int[] dimensions,int index){
        if(index==dimensions.length-1)return newInstance(component,dimensions[index]);
        Class<?> inner=component;for(int i=index+1;i<dimensions.length;i++)inner=newInstance(inner,0).getClass();Object result=newInstance(inner,dimensions[index]);
        for(int i=0;i<dimensions[index];i++)set(result,i,multi(component,dimensions,index+1));return result;
    }
    public static int getLength(Object array){
        if(array==null)throw new NullPointerException();
        if(array instanceof Object[])return ((Object[])array).length;if(array instanceof boolean[])return ((boolean[])array).length;if(array instanceof byte[])return ((byte[])array).length;if(array instanceof char[])return ((char[])array).length;if(array instanceof short[])return ((short[])array).length;if(array instanceof int[])return ((int[])array).length;if(array instanceof long[])return ((long[])array).length;if(array instanceof float[])return ((float[])array).length;if(array instanceof double[])return ((double[])array).length;throw new IllegalArgumentException();
    }
    public static Object get(Object array,int index){
        if(array==null)throw new NullPointerException();
        if(array instanceof Object[])return ((Object[])array)[index];if(array instanceof boolean[])return ((boolean[])array)[index];if(array instanceof byte[])return ((byte[])array)[index];if(array instanceof char[])return ((char[])array)[index];if(array instanceof short[])return ((short[])array)[index];if(array instanceof int[])return ((int[])array)[index];if(array instanceof long[])return ((long[])array)[index];if(array instanceof float[])return ((float[])array)[index];if(array instanceof double[])return ((double[])array)[index];throw new IllegalArgumentException();
    }
    public static void set(Object array,int index,Object value){
        if(array==null)throw new NullPointerException();if(!array.getClass().isArray())throw new IllegalArgumentException();
        int length=getLength(array);if(index<0 || index>=length)throw new ArrayIndexOutOfBoundsException();
        Class<?> component=array.getClass().getComponentType();Object converted;
        try{converted=MethodHandle.javawConvert(value,Object.class,component);}catch(ClassCastException error){throw new IllegalArgumentException();}catch(NullPointerException error){throw new IllegalArgumentException();}
        if(array instanceof Object[]){((Object[])array)[index]=converted;return;}
        if(array instanceof boolean[]){((boolean[])array)[index]=(Boolean)converted;return;}if(array instanceof byte[]){((byte[])array)[index]=(Byte)converted;return;}if(array instanceof char[]){((char[])array)[index]=(Character)converted;return;}if(array instanceof short[]){((short[])array)[index]=(Short)converted;return;}if(array instanceof int[]){((int[])array)[index]=(Integer)converted;return;}if(array instanceof long[]){((long[])array)[index]=(Long)converted;return;}if(array instanceof float[]){((float[])array)[index]=(Float)converted;return;}((double[])array)[index]=(Double)converted;
    }
    public static boolean getBoolean(Object array,int index){
        Object value=get(array,index);Class<?> component=array.getClass().getComponentType();
        if(!component.isPrimitive())throw new IllegalArgumentException();
        try{return ((Boolean)MethodHandle.javawConvert(value,component,boolean.class)).booleanValue();}catch(ClassCastException error){throw new IllegalArgumentException();}
    }
    public static void setBoolean(Object array,int index,boolean value){
        if(array==null)throw new NullPointerException();
        if(!array.getClass().isArray() || !array.getClass().getComponentType().isPrimitive())throw new IllegalArgumentException();
        set(array,index,Boolean.valueOf(value));
    }
    public static byte getByte(Object array,int index){
        Object value=get(array,index);Class<?> component=array.getClass().getComponentType();
        if(!component.isPrimitive())throw new IllegalArgumentException();
        try{return ((Byte)MethodHandle.javawConvert(value,component,byte.class)).byteValue();}catch(ClassCastException error){throw new IllegalArgumentException();}
    }
    public static void setByte(Object array,int index,byte value){
        if(array==null)throw new NullPointerException();
        if(!array.getClass().isArray() || !array.getClass().getComponentType().isPrimitive())throw new IllegalArgumentException();
        set(array,index,Byte.valueOf(value));
    }
    public static char getChar(Object array,int index){
        Object value=get(array,index);Class<?> component=array.getClass().getComponentType();
        if(!component.isPrimitive())throw new IllegalArgumentException();
        try{return ((Character)MethodHandle.javawConvert(value,component,char.class)).charValue();}catch(ClassCastException error){throw new IllegalArgumentException();}
    }
    public static void setChar(Object array,int index,char value){
        if(array==null)throw new NullPointerException();
        if(!array.getClass().isArray() || !array.getClass().getComponentType().isPrimitive())throw new IllegalArgumentException();
        set(array,index,Character.valueOf(value));
    }
    public static short getShort(Object array,int index){
        Object value=get(array,index);Class<?> component=array.getClass().getComponentType();
        if(!component.isPrimitive())throw new IllegalArgumentException();
        try{return ((Short)MethodHandle.javawConvert(value,component,short.class)).shortValue();}catch(ClassCastException error){throw new IllegalArgumentException();}
    }
    public static void setShort(Object array,int index,short value){
        if(array==null)throw new NullPointerException();
        if(!array.getClass().isArray() || !array.getClass().getComponentType().isPrimitive())throw new IllegalArgumentException();
        set(array,index,Short.valueOf(value));
    }
    public static int getInt(Object array,int index){
        Object value=get(array,index);Class<?> component=array.getClass().getComponentType();
        if(!component.isPrimitive())throw new IllegalArgumentException();
        try{return ((Integer)MethodHandle.javawConvert(value,component,int.class)).intValue();}catch(ClassCastException error){throw new IllegalArgumentException();}
    }
    public static void setInt(Object array,int index,int value){
        if(array==null)throw new NullPointerException();
        if(!array.getClass().isArray() || !array.getClass().getComponentType().isPrimitive())throw new IllegalArgumentException();
        set(array,index,Integer.valueOf(value));
    }
    public static long getLong(Object array,int index){
        Object value=get(array,index);Class<?> component=array.getClass().getComponentType();
        if(!component.isPrimitive())throw new IllegalArgumentException();
        try{return ((Long)MethodHandle.javawConvert(value,component,long.class)).longValue();}catch(ClassCastException error){throw new IllegalArgumentException();}
    }
    public static void setLong(Object array,int index,long value){
        if(array==null)throw new NullPointerException();
        if(!array.getClass().isArray() || !array.getClass().getComponentType().isPrimitive())throw new IllegalArgumentException();
        set(array,index,Long.valueOf(value));
    }
    public static float getFloat(Object array,int index){
        Object value=get(array,index);Class<?> component=array.getClass().getComponentType();
        if(!component.isPrimitive())throw new IllegalArgumentException();
        try{return ((Float)MethodHandle.javawConvert(value,component,float.class)).floatValue();}catch(ClassCastException error){throw new IllegalArgumentException();}
    }
    public static void setFloat(Object array,int index,float value){
        if(array==null)throw new NullPointerException();
        if(!array.getClass().isArray() || !array.getClass().getComponentType().isPrimitive())throw new IllegalArgumentException();
        set(array,index,Float.valueOf(value));
    }
    public static double getDouble(Object array,int index){
        Object value=get(array,index);Class<?> component=array.getClass().getComponentType();
        if(!component.isPrimitive())throw new IllegalArgumentException();
        try{return ((Double)MethodHandle.javawConvert(value,component,double.class)).doubleValue();}catch(ClassCastException error){throw new IllegalArgumentException();}
    }
    public static void setDouble(Object array,int index,double value){
        if(array==null)throw new NullPointerException();
        if(!array.getClass().isArray() || !array.getClass().getComponentType().isPrimitive())throw new IllegalArgumentException();
        set(array,index,Double.valueOf(value));
    }
}
