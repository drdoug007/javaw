package java.lang.reflect;
public abstract class Executable extends AccessibleObject implements Member,GenericDeclaration {
    protected Executable(){}
    protected abstract String javawKey();
    public String javawMetadataKey(){return javawKey();}
    public abstract Class<?>[] getParameterTypes();public abstract Class<?>[] getExceptionTypes();
    public abstract boolean isVarArgs();
    public int getParameterCount(){return getParameterTypes().length;}
    protected javaw.runtime.GenericSignatures.Result javawGeneric(){return javaw.runtime.GenericSignatures.parse(this,javaw.runtime.ReflectionSupport.signature(getDeclaringClass(),javawKey()));}
    public TypeVariable<?>[] getTypeParameters(){return javawGeneric().variables;}
    public Type[] getGenericParameterTypes(){String signature=javaw.runtime.ReflectionSupport.signature(getDeclaringClass(),javawKey());return signature==null?getParameterTypes():javawGeneric().parameters;}
    public Type[] getGenericExceptionTypes(){Type[] values=javawGeneric().exceptions;return values.length==0?getExceptionTypes():values;}
    public AnnotatedType[] getAnnotatedParameterTypes(){Type[] types=getGenericParameterTypes();AnnotatedType[] values=new AnnotatedType[types.length];for(int i=0;i<values.length;i++)values[i]=javaw.runtime.AnnotatedTypes.of(types[i],getDeclaringClass(),javawKey(),22<<24|i<<16,"");return values;}
    public AnnotatedType[] getAnnotatedExceptionTypes(){Type[] types=getGenericExceptionTypes();AnnotatedType[] values=new AnnotatedType[types.length];for(int i=0;i<values.length;i++)values[i]=javaw.runtime.AnnotatedTypes.of(types[i],getDeclaringClass(),javawKey(),23<<24|i<<8,"");return values;}
    public AnnotatedType getAnnotatedReceiverType(){Class<?> receiver=getDeclaringClass();if(this instanceof Constructor){receiver=receiver.getDeclaringClass();if(receiver==null || (getDeclaringClass().getModifiers()&8)!=0)return null;}else if((getModifiers()&8)!=0)return null;return javaw.runtime.AnnotatedTypes.of(receiver,getDeclaringClass(),javawKey(),21<<24,"");}
    protected String javawText(boolean generic){
        String modifiers=Modifier.toString(getModifiers()&(this instanceof Method?Modifier.methodModifiers():Modifier.constructorModifiers()));if(this instanceof Method && ((Method)this).isDefault())modifiers=modifiers.concat(" default");String result=modifiers.length()==0?"":modifiers.concat(" ");
        if(generic){TypeVariable<?>[] variables=getTypeParameters();if(variables.length>0){result=result.concat("<");for(int i=0;i<variables.length;i++){if(i>0)result=result.concat(",");result=result.concat(variables[i].getName());Type[] bounds=variables[i].getBounds();if(bounds.length!=1 || bounds[0]!=Object.class){result=result.concat(" extends ");for(int j=0;j<bounds.length;j++){if(j>0)result=result.concat(" & ");result=result.concat(bounds[j].getTypeName());}}}result=result.concat("> ");}}
        if(this instanceof Method){Method method=(Method)this;result=result.concat((generic?method.getGenericReturnType():method.getReturnType()).getTypeName()).concat(" ").concat(getDeclaringClass().getName()).concat(".").concat(getName());}else result=result.concat(getName());
        result=result.concat("(");Type[] parameters=generic?getGenericParameterTypes():getParameterTypes();for(int i=0;i<parameters.length;i++){if(i>0)result=result.concat(",");String text=parameters[i].getTypeName();if(generic && isVarArgs() && i==parameters.length-1)text=javawVarargs(text);result=result.concat(text);}result=result.concat(")");
        Type[] exceptions=generic?getGenericExceptionTypes():getExceptionTypes();if(exceptions.length>0){result=result.concat(" throws ");for(int i=0;i<exceptions.length;i++){if(i>0)result=result.concat(",");result=result.concat(exceptions[i].getTypeName());}}return result;
    }
    static String javawVarargs(String text){char[] chars=new char[text.length()-2];for(int i=0;i<chars.length;i++)chars[i]=text.charAt(i);return new String(chars).concat("...");}
    public String toString(){return javawText(false);}public String toGenericString(){return javawText(true);}
    public Parameter[] getParameters(){Parameter[] values=new Parameter[getParameterCount()];String[] names=javaw.runtime.ReflectionSupport.parameterNames(getDeclaringClass(),javawKey());int[] modifiers=javaw.runtime.ReflectionSupport.parameterModifiers(getDeclaringClass(),javawKey());for(int i=0;i<values.length;i++)values[i]=new Parameter(this,i,names==null?null:names[i],modifiers==null?0:modifiers[i]);return values;}
    public java.lang.annotation.Annotation[][] getParameterAnnotations(){java.lang.annotation.Annotation[][] values=new java.lang.annotation.Annotation[getParameterCount()][];for(int i=0;i<values.length;i++)values[i]=javaw.runtime.ReflectionSupport.annotations(getDeclaringClass(),"P".concat(javawKey()).concat("#").concat(String.valueOf(i)));return values;}
}
