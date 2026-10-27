package one.dastec.javaw;

public class GlobalVar {
    public final String name;
    public final String type;
    public final String initVal;
    public final String javaType;

    public GlobalVar(String name, String type, String initVal, String javaType) {
        this.name = name;
        this.type = type;
        this.initVal = initVal;
        this.javaType = javaType;
    }

    public GlobalVar(String name, String type, String initVal) {
        this(name, type, initVal, null);
    }

    public String getJavaType() {
        return javaType;
    }
}
