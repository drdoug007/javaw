package javaw.runtime;
import java.lang.reflect.*;
import java.lang.annotation.Annotation;
public class AnnotatedTypes implements AnnotatedType {
    protected final Type type;protected final Class<?> owner;protected final String key,path;protected final int reference;
    protected AnnotatedTypes(Type type,Class<?> owner,String key,int reference,String path){this.type=type;this.owner=owner;this.key=key;this.reference=reference;this.path=path;}
    public Type getType(){return type;}
    public Annotation[] getDeclaredAnnotations(){return ReflectionSupport.annotations(owner,"Y".concat(key).concat("#").concat(String.valueOf(reference)).concat("#").concat(path));}
    public static AnnotatedType of(Type type,Class<?> owner,String key,int reference,String path){
        if(type==null)return null;for(int i=0;i<depth(type);i++)path=path.concat(".");return create(type,owner,key,reference,path);
    }
    private static Class<?> raw(Type type){return type instanceof Class?(Class<?>)type:type instanceof ParameterizedType?(Class<?>)((ParameterizedType)type).getRawType():null;}
    private static int depth(Type type){Class<?> raw=raw(type);if(raw==null || raw.getDeclaringClass()==null || (raw.getModifiers()&8)!=0)return 0;Type parent=type instanceof ParameterizedType?((ParameterizedType)type).getOwnerType():raw.getDeclaringClass();return 1+depth(parent);}
    public AnnotatedType getAnnotatedOwnerType(){
        Class<?> raw=raw(type);Type parent=type instanceof ParameterizedType?((ParameterizedType)type).getOwnerType():raw==null?null:raw.getDeclaringClass();if(parent==null)return null;
        if((raw.getModifiers()&8)!=0)return create(parent,owner,"unannotated static owner",reference,"");
        char[] shortened=new char[path.length()-1];for(int i=0;i<shortened.length;i++)shortened[i]=path.charAt(i);return create(parent,owner,key,reference,new String(shortened));
    }
    private static AnnotatedType create(Type type,Class<?> owner,String key,int reference,String path){
        if(type==null)return null;if(type instanceof ParameterizedType)return new Parameterized(type,owner,key,reference,path);
        if(type instanceof GenericArrayType || type instanceof Class && ((Class<?>)type).isArray())return new ArrayType(type,owner,key,reference,path);
        if(type instanceof WildcardType)return new Wildcard(type,owner,key,reference,path);
        if(type instanceof TypeVariable)return new Variable(type,owner,key,reference,path);return new AnnotatedTypes(type,owner,key,reference,path);
    }
    private static final class Parameterized extends AnnotatedTypes implements AnnotatedParameterizedType {
        Parameterized(Type type,Class<?> owner,String key,int reference,String path){super(type,owner,key,reference,path);}
        public AnnotatedType[] getAnnotatedActualTypeArguments(){Type[] arguments=((ParameterizedType)type).getActualTypeArguments();AnnotatedType[] values=new AnnotatedType[arguments.length];for(int i=0;i<values.length;i++)values[i]=of(arguments[i],owner,key,reference,path.concat(String.valueOf(i)).concat(";"));return values;}
    }
    private static final class ArrayType extends AnnotatedTypes implements AnnotatedArrayType {
        ArrayType(Type type,Class<?> owner,String key,int reference,String path){super(type,owner,key,reference,path);}
        public AnnotatedType getAnnotatedGenericComponentType(){Type component=type instanceof Class?((Class<?>)type).getComponentType():((GenericArrayType)type).getGenericComponentType();return of(component,owner,key,reference,path.concat("["));}
    }
    private static final class Wildcard extends AnnotatedTypes implements AnnotatedWildcardType {
        Wildcard(Type type,Class<?> owner,String key,int reference,String path){super(type,owner,key,reference,path);}
        private AnnotatedType[] bounds(Type[] bounds){AnnotatedType[] values=new AnnotatedType[bounds.length];for(int i=0;i<values.length;i++)values[i]=of(bounds[i],owner,key,reference,path.concat("*"));return values;}
        public AnnotatedType[] getAnnotatedUpperBounds(){return bounds(((WildcardType)type).getUpperBounds());}public AnnotatedType[] getAnnotatedLowerBounds(){return bounds(((WildcardType)type).getLowerBounds());}
    }
    private static final class Variable extends AnnotatedTypes implements AnnotatedTypeVariable {
        Variable(Type type,Class<?> owner,String key,int reference,String path){super(type,owner,key,reference,path);}
        public AnnotatedType[] getAnnotatedBounds(){return ((TypeVariable<?>)type).getAnnotatedBounds();}
    }
}
