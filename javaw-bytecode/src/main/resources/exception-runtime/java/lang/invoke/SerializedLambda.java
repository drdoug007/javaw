package java.lang.invoke;

/** Serializable lambda metadata; stream serialization/class-loader reflection is outside this runtime. */
public final class SerializedLambda implements java.io.Serializable {
    private final Class<?> capturingClass;
    private final String functionalInterfaceClass,functionalInterfaceMethodName,functionalInterfaceMethodSignature;
    private final int implMethodKind;
    private final String implClass,implMethodName,implMethodSignature,instantiatedMethodType;
    private final Object[] capturedArgs;
    public SerializedLambda(Class<?> capturingClass,String functionalInterfaceClass,String functionalInterfaceMethodName,
            String functionalInterfaceMethodSignature,int implMethodKind,String implClass,String implMethodName,
            String implMethodSignature,String instantiatedMethodType,Object[] capturedArgs) {
        this.capturingClass=capturingClass;this.functionalInterfaceClass=functionalInterfaceClass;
        this.functionalInterfaceMethodName=functionalInterfaceMethodName;this.functionalInterfaceMethodSignature=functionalInterfaceMethodSignature;
        this.implMethodKind=implMethodKind;this.implClass=implClass;this.implMethodName=implMethodName;
        this.implMethodSignature=implMethodSignature;this.instantiatedMethodType=instantiatedMethodType;
        this.capturedArgs=capturedArgs.clone();
    }
    public String getCapturingClass(){
        String name=capturingClass.getName();char[] chars=new char[name.length()];
        for(int i=0;i<chars.length;i++)chars[i]=name.charAt(i)=='.'?'/':name.charAt(i);return new String(chars);
    }
    public String getFunctionalInterfaceClass(){return functionalInterfaceClass;}
    public String getFunctionalInterfaceMethodName(){return functionalInterfaceMethodName;}
    public String getFunctionalInterfaceMethodSignature(){return functionalInterfaceMethodSignature;}
    public int getImplMethodKind(){return implMethodKind;}
    public String getImplClass(){return implClass;}
    public String getImplMethodName(){return implMethodName;}
    public String getImplMethodSignature(){return implMethodSignature;}
    public String getInstantiatedMethodType(){return instantiatedMethodType;}
    public int getCapturedArgCount(){return capturedArgs.length;}
    public Object getCapturedArg(int index){return capturedArgs[index];}
}
