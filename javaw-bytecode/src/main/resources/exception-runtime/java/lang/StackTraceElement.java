package java.lang;

public final class StackTraceElement {
    private final String declaringClass, methodName, fileName;
    private final int lineNumber;
    public StackTraceElement(String declaringClass,String methodName,String fileName,int lineNumber) {
        if(declaringClass==null || methodName==null)throw new NullPointerException();
        this.declaringClass=declaringClass;this.methodName=methodName;this.fileName=fileName;this.lineNumber=lineNumber;
    }
    public String getClassName(){return declaringClass;}
    public String getMethodName(){return methodName;}
    public String getFileName(){return fileName;}
    public int getLineNumber(){return lineNumber;}
    public boolean isNativeMethod(){return lineNumber==-2;}
    public boolean equals(Object other) {
        if(!(other instanceof StackTraceElement))return false;
        StackTraceElement value=(StackTraceElement)other;
        return declaringClass.equals(value.declaringClass) && methodName.equals(value.methodName)
            && lineNumber==value.lineNumber && (fileName==null?value.fileName==null:fileName.equals(value.fileName));
    }
    public int hashCode(){return 31*(31*declaringClass.hashCode()+methodName.hashCode())+(fileName==null?0:fileName.hashCode())+lineNumber;}
    public String toString(){
        String location=isNativeMethod()?"Native Method":fileName==null?"Unknown Source":lineNumber>=0?fileName.concat(":").concat(String.valueOf(lineNumber)):fileName;
        return declaringClass.concat(".").concat(methodName).concat("(").concat(location).concat(")");
    }
}
