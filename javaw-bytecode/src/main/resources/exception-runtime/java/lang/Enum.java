package java.lang;
public abstract class Enum<E extends Enum<E>> implements Comparable<E>,java.io.Serializable {
    private final String name;private final int ordinal;
    protected Enum(String name,int ordinal){this.name=name;this.ordinal=ordinal;}
    public final boolean equals(Object other){return this==other;}public final int hashCode(){return super.hashCode();}
    protected final Object clone() throws CloneNotSupportedException{throw new CloneNotSupportedException();}
    protected final void finalize(){}
    public final String name(){return name;}public final int ordinal(){return ordinal;}public String toString(){return name;}
    public final int compareTo(E other){if(getDeclaringClass()!=other.getDeclaringClass())throw new ClassCastException();return ordinal-other.ordinal();}
    public final Class<E> getDeclaringClass(){Class<?> type=getClass();return (Class<E>)(type.isEnum()?type:type.getSuperclass());}
    public static <T extends Enum<T>> T valueOf(Class<T> type,String name){if(name==null)throw new NullPointerException();T[] values=type.getEnumConstants();if(values==null)throw new IllegalArgumentException();for(T value:values)if(name.equals(value.name()))return value;throw new IllegalArgumentException(name);}
}
