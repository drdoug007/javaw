package java.lang.reflect;
public final class Parameter implements AnnotatedElement {
    private final Executable executable;private final int index,modifiers;private final String name;
    public Parameter(Executable executable,int index,String name,int modifiers){this.executable=executable;this.index=index;this.name=name;this.modifiers=modifiers;}
    public String getName(){return name==null?"arg".concat(String.valueOf(index)):name;}public boolean isNamePresent(){return name!=null;}
    public int getModifiers(){return modifiers;}public boolean isImplicit(){return (modifiers&32768)!=0;}public boolean isSynthetic(){return (modifiers&4096)!=0;}
    public boolean isVarArgs(){return executable.isVarArgs() && index==executable.getParameterCount()-1;}
    public Executable getDeclaringExecutable(){return executable;}public Class<?> getType(){return executable.getParameterTypes()[index];}
    public AnnotatedType getAnnotatedType(){return executable.getAnnotatedParameterTypes()[index];}
    public Type getParameterizedType(){return executable.getGenericParameterTypes()[index];}
    public java.lang.annotation.Annotation[] getDeclaredAnnotations(){return executable.getParameterAnnotations()[index];}
    public boolean equals(Object other){return other instanceof Parameter && index==((Parameter)other).index && executable.equals(((Parameter)other).executable);}
    public int hashCode(){return executable.hashCode()^index;}
}
