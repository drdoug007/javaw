package javaw.runtime;

/** Replaced with closed-world declaration tables when method discovery is requested. */
public final class ReflectionSupport {
    private ReflectionSupport(){}
    public static java.lang.reflect.Constructor<?>[] declaredConstructors(Class<?> type){throw new UnsupportedOperationException();}
    public static java.lang.reflect.Field[] declaredFields(Class<?> type){throw new UnsupportedOperationException();}
    public static java.lang.annotation.Annotation[] annotations(Class<?> type,String key){return new java.lang.annotation.Annotation[0];}
    public static Object defaultValue(Class<?> type,String key){return null;}
    public static Object newArray(Class<?> component,int length){throw new UnsupportedOperationException("unregistered reflective array component");}
    public static String descriptor(Class<?> type){
        if(type==void.class)return "V";if(type==boolean.class)return "Z";if(type==byte.class)return "B";if(type==char.class)return "C";if(type==short.class)return "S";if(type==int.class)return "I";if(type==long.class)return "J";if(type==float.class)return "F";if(type==double.class)return "D";
        String name=type.getName();char[] chars=new char[name.length()];for(int i=0;i<chars.length;i++)chars[i]=name.charAt(i)=='.'?'/':name.charAt(i);String internal=new String(chars);return type.isArray()?internal:"L".concat(internal).concat(";");
    }
    public static String[] parameterNames(Class<?> owner,String key){return null;}
    public static int[] parameterModifiers(Class<?> owner,String key){return null;}
    public static String signature(Class<?> owner,String key){return null;}
    public static Class<?> declaringClass(Class<?> owner){return null;}
    public static Class<?> resolve(String name){throw new TypeNotPresentException(name,null);}
    public static Class<?>[] declaredClasses(Class<?> type){return new Class<?>[0];}
    public static String[] enclosing(Class<?> type){return null;}
    public static void initialize(Class<?> type){throw new UnsupportedOperationException();}
    public static Class<?> nestHost(Class<?> type){return type;}
    public static java.lang.reflect.Method[] declaredMethods(Class<?> type){
        throw new UnsupportedOperationException("declared-method metadata is unavailable for this class");
    }
}
