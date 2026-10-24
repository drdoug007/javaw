package javaw.runtime;
import java.lang.reflect.*;
import java.lang.annotation.Annotation;

/** Class-file generic signatures are parsed lazily in their declaration scope. */
public final class GenericSignatures {
    private GenericSignatures(){}
    public static final class Result {
        public TypeVariable<?>[] variables=new TypeVariable<?>[0];public Type[] parameters=new Type[0],exceptions=new Type[0],interfaces=new Type[0];public Type result;
    }
    public static Result parse(GenericDeclaration declaration,String signature){
        if(signature==null)return new Result();try{return new Parser(declaration,signature).parse();}catch(IndexOutOfBoundsException error){throw new GenericSignatureFormatError();}
    }
    public static Type field(Class<?> owner,String signature){if(signature==null)return null;try{Parser parser=new Parser(owner,signature);parser.fieldMode=true;Type type=parser.type();if(parser.at!=signature.length())throw new GenericSignatureFormatError();return type;}catch(IndexOutOfBoundsException error){throw new GenericSignatureFormatError();}}
    private static Type[] append(Type[] array,Type value){Type[] result=new Type[array.length+1];System.arraycopy(array,0,result,0,array.length);result[array.length]=value;return result;}
    private static Type[] copy(Type[] values){Type[] result=new Type[values.length];System.arraycopy(values,0,result,0,values.length);return result;}
    private static boolean equal(Type[] a,Type[] b){if(a.length!=b.length)return false;for(int i=0;i<a.length;i++)if(!a[i].equals(b[i]))return false;return true;}
    private static int hash(Type[] values){int result=1;for(Type value:values)result=31*result+value.hashCode();return result;}
    private static String slice(String source,int start,int end){char[] chars=new char[end-start];for(int i=0;i<chars.length;i++)chars[i]=source.charAt(start+i);return new String(chars);}
    private static final class Parser {
        final GenericDeclaration declaration;final String text;int at;boolean fieldMode;Variable[] variables=new Variable[0];
        Parser(GenericDeclaration declaration,String text){this.declaration=declaration;this.text=text;}
        char peek(){return text.charAt(at);}void expect(char value){if(peek()!=value)throw new GenericSignatureFormatError();at++;}
        Result parse(){
            Result result=new Result();formals();result.variables=variables;
            if(declaration instanceof Class){result.result=type();while(at<text.length())result.interfaces=append(result.interfaces,type());}
            else {expect('(');while(peek()!=')')result.parameters=append(result.parameters,type());at++;result.result=type();while(at<text.length()){expect('^');result.exceptions=append(result.exceptions,type());}}
            if(at!=text.length())throw new GenericSignatureFormatError();return result;
        }
        void formals(){
            if(peek()!='<')return;int start=++at;
            while(peek()!='>'){
                int nameStart=at;while(peek()!=':')at++;String name=slice(text,nameStart,at);Variable[] grown=new Variable[variables.length+1];System.arraycopy(variables,0,grown,0,variables.length);grown[variables.length]=new Variable(declaration,name,variables.length);variables=grown;
                at++;if(peek()!=':')skip();while(peek()==':'){at++;skip();}
            }
            int end=at;at=start;
            for(Variable variable:variables){while(peek()!=':')at++;at++;variable.offset=peek()==':'?1:0;Type[] bounds=new Type[0];if(peek()!=':')bounds=append(bounds,type());while(peek()==':'){at++;bounds=append(bounds,type());}variable.bounds=bounds;}
            if(at!=end)throw new GenericSignatureFormatError();expect('>');
        }
        void skip(){char value=peek();at++;if(value=='['){skip();return;}if(value=='T'){while(peek()!=';')at++;at++;return;}if(value=='L'){int depth=0;while(true){char c=peek();at++;if(c=='<')depth++;if(c=='>')depth--;if(c==';' && depth==0)return;}}else if("BCDFIJSZV".length()>0 && (value=='B'||value=='C'||value=='D'||value=='F'||value=='I'||value=='J'||value=='S'||value=='Z'||value=='V'))return;else throw new GenericSignatureFormatError();}
        Type variable(String name){
            for(Variable variable:variables)if(variable.name.equals(name))return variable;
            Class<?> owner=declaration instanceof Class?(Class<?>)declaration:((Member)declaration).getDeclaringClass();
            if(declaration!=owner || fieldMode)for(TypeVariable<?> variable:owner.getTypeParameters())if(variable.getName().equals(name))return variable;
            for(Class<?> outer=owner.getDeclaringClass();outer!=null;outer=outer.getDeclaringClass())for(TypeVariable<?> variable:outer.getTypeParameters())if(variable.getName().equals(name))return variable;
            throw new GenericSignatureFormatError("unresolved type variable ".concat(name));
        }
        Type type(){
            char value=peek();at++;
            if(value=='T'){int start=at;while(peek()!=';')at++;String name=slice(text,start,at++);return variable(name);}
            if(value=='['){Type component=type();return component instanceof Class?(Type)Array.newInstance((Class<?>)component,0).getClass():new GenericArray(component);}
            if(value=='L'){
                String binary="";Type owner=null,result=null;
                while(true){int start=at;while(peek()!=';' && peek()!='<' && peek()!='.')at++;String segment=slice(text,start,at);char[] chars=new char[segment.length()];for(int i=0;i<chars.length;i++)chars[i]=segment.charAt(i)=='/'?'.':segment.charAt(i);segment=new String(chars);
                    binary=binary.length()==0?segment:binary.concat("$").concat(segment);Class<?> raw=ReflectionSupport.resolve(binary);Type[] arguments=new Type[0];
                    if(peek()=='<'){at++;while(peek()!='>'){char variance=peek();if(variance=='*'){at++;arguments=append(arguments,new Wildcard(new Type[]{Object.class},new Type[0]));}else if(variance=='+' || variance=='-'){at++;Type bound=type();arguments=append(arguments,variance=='+'?new Wildcard(new Type[]{bound},new Type[0]):new Wildcard(new Type[]{Object.class},new Type[]{bound}));}else arguments=append(arguments,type());}at++;}
                    Type actualOwner=owner==null?raw.getDeclaringClass():owner;result=arguments.length==0 && !(actualOwner instanceof ParameterizedType)?raw:new Parameterized(raw,actualOwner,arguments);
                    if(peek()==';'){at++;return result;}expect('.');owner=result;
                }
            }
            if(value=='B')return byte.class;if(value=='C')return char.class;if(value=='D')return double.class;if(value=='F')return float.class;if(value=='I')return int.class;if(value=='J')return long.class;if(value=='S')return short.class;if(value=='Z')return boolean.class;if(value=='V')return void.class;throw new GenericSignatureFormatError();
        }
    }
    private static final class Variable implements TypeVariable<GenericDeclaration> {
        final GenericDeclaration declaration;final String name;final int index;int offset;Type[] bounds;
        Variable(GenericDeclaration declaration,String name,int index){this.declaration=declaration;this.name=name;this.index=index;}
        public Type[] getBounds(){return copy(bounds);}public GenericDeclaration getGenericDeclaration(){return declaration;}public String getName(){return name;}public String getTypeName(){return name;}public String toString(){return name;}
        private Class<?> owner(){return declaration instanceof Class?(Class<?>)declaration:((Member)declaration).getDeclaringClass();}
        private String key(){return declaration instanceof Class?"":((Executable)declaration).javawMetadataKey();}
        public Annotation[] getDeclaredAnnotations(){int reference=(declaration instanceof Class?0:1)<<24 | index<<16;return ReflectionSupport.annotations(owner(),"Y".concat(key()).concat("#").concat(String.valueOf(reference)).concat("#"));}
        public AnnotatedType[] getAnnotatedBounds(){AnnotatedType[] values=new AnnotatedType[bounds.length];for(int i=0;i<values.length;i++){int reference=(declaration instanceof Class?17:18)<<24 | index<<16 | (i+offset)<<8;values[i]=AnnotatedTypes.of(bounds[i],owner(),key(),reference,"");}return values;}
        public boolean equals(Object other){return other instanceof TypeVariable && declaration.equals(((TypeVariable<?>)other).getGenericDeclaration()) && name.equals(((TypeVariable<?>)other).getName());}public int hashCode(){return declaration.hashCode()^name.hashCode();}
    }
    private static final class Parameterized implements ParameterizedType {
        final Class<?> raw;final Type owner;final Type[] arguments;
        Parameterized(Class<?> raw,Type owner,Type[] arguments){this.raw=raw;this.owner=owner;this.arguments=arguments;}
        public Type[] getActualTypeArguments(){return copy(arguments);}public Type getRawType(){return raw;}public Type getOwnerType(){return owner;}
        public String getTypeName(){String result=owner instanceof ParameterizedType?owner.getTypeName().concat("$").concat(raw.getSimpleName()):raw.getName();if(arguments.length!=0){result=result.concat("<");for(int i=0;i<arguments.length;i++){if(i>0)result=result.concat(", ");result=result.concat(arguments[i].getTypeName());}result=result.concat(">");}return result;}
        public String toString(){return getTypeName();}public boolean equals(Object other){if(!(other instanceof ParameterizedType))return false;ParameterizedType value=(ParameterizedType)other;return raw.equals(value.getRawType()) && (owner==null?value.getOwnerType()==null:owner.equals(value.getOwnerType())) && equal(arguments,value.getActualTypeArguments());}
        public int hashCode(){return hash(arguments)^raw.hashCode()^(owner==null?0:owner.hashCode());}
    }
    private static final class GenericArray implements GenericArrayType {
        final Type component;GenericArray(Type component){this.component=component;}public Type getGenericComponentType(){return component;}public String getTypeName(){return component.getTypeName().concat("[]");}public String toString(){return getTypeName();}
        public boolean equals(Object other){return other instanceof GenericArrayType && component.equals(((GenericArrayType)other).getGenericComponentType());}public int hashCode(){return component.hashCode();}
    }
    private static final class Wildcard implements WildcardType {
        final Type[] upper,lower;Wildcard(Type[] upper,Type[] lower){this.upper=upper;this.lower=lower;}
        public Type[] getUpperBounds(){return copy(upper);}public Type[] getLowerBounds(){return copy(lower);}
        public String getTypeName(){return lower.length!=0?"? super ".concat(lower[0].getTypeName()):upper[0]==Object.class?"?":"? extends ".concat(upper[0].getTypeName());}public String toString(){return getTypeName();}
        public boolean equals(Object other){return other instanceof WildcardType && equal(upper,((WildcardType)other).getUpperBounds()) && equal(lower,((WildcardType)other).getLowerBounds());}public int hashCode(){return hash(upper)^hash(lower);}
    }
}
