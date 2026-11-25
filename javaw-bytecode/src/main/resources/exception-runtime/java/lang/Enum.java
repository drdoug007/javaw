package java.lang;
public abstract class Enum<E extends Enum<E>> implements Comparable<E>,java.io.Serializable {
    private final String name;private final int ordinal;
    // Constants per declaring class, filled by the constructor. Arrays grow by doubling and the last class used is
    // tried first (an enum's constants are constructed one after another), so registering n constants is linear.
    private static Class<?>[] javawTypes=new Class<?>[4];
    private static Enum<?>[][] javawConstants=new Enum<?>[4][];
    private static int[] javawCounts=new int[4];
    private static int javawTypeCount,javawLast=-1;
    protected Enum(String name,int ordinal){this.name=name;this.ordinal=ordinal;javawRegister(this);}
    private static int javawIndex(Class<?> type){
        if(javawLast>=0 && javawTypes[javawLast]==type)return javawLast;
        for(int i=0;i<javawTypeCount;i++)if(javawTypes[i]==type)return i;
        return -1;
    }
    private static void javawRegister(Enum<?> constant){
        Class<?> declaring=constant.getDeclaringClass();
        int i=javawIndex(declaring);
        if(i<0){
            if(javawTypeCount==javawTypes.length){
                int size=javawTypes.length*2;
                Class<?>[] types=new Class<?>[size];System.arraycopy(javawTypes,0,types,0,javawTypeCount);javawTypes=types;
                Enum<?>[][] constants=new Enum<?>[size][];System.arraycopy(javawConstants,0,constants,0,javawTypeCount);javawConstants=constants;
                int[] counts=new int[size];System.arraycopy(javawCounts,0,counts,0,javawTypeCount);javawCounts=counts;
            }
            i=javawTypeCount++;
            javawTypes[i]=declaring;javawConstants[i]=new Enum<?>[4];javawCounts[i]=0;
        }
        javawLast=i;
        Enum<?>[] values=javawConstants[i];
        if(javawCounts[i]==values.length){
            Enum<?>[] grown=new Enum<?>[values.length*2];System.arraycopy(values,0,grown,0,values.length);javawConstants[i]=values=grown;
        }
        values[javawCounts[i]++]=constant;
    }
    public final boolean equals(Object other){return this==other;}public final int hashCode(){return super.hashCode();}
    protected final Object clone() throws CloneNotSupportedException{throw new CloneNotSupportedException();}
    protected final void finalize(){}
    public final String name(){return name;}public final int ordinal(){return ordinal;}public String toString(){return name;}
    public final int compareTo(E other){if(getDeclaringClass()!=other.getDeclaringClass())throw new ClassCastException();return ordinal-other.ordinal();}
    public final Class<E> getDeclaringClass(){Class<?> type=getClass();return (Class<E>)(type.isEnum()?type:type.getSuperclass());}
    public static <T extends Enum<T>> T valueOf(Class<T> type,String name){
        if(type==null)throw new NullPointerException();
        int index=javawIndex(type);
        Enum<?>[] values;int count;
        if(index>=0){values=javawConstants[index];count=javawCounts[index];}
        else{
            if(!type.isEnum())throw new IllegalArgumentException(type.getName()+" is not an enum class");
            T[] found=type.getEnumConstants();
            if(found==null)throw new IllegalArgumentException(type.getName()+" is not an enum class");
            values=found;count=found.length;
        }
        if(name==null)throw new NullPointerException("Name is null");
        for(int i=0;i<count;i++)if(name.equals(values[i].name()))return (T)values[i];
        throw new IllegalArgumentException("No enum constant "+type.getCanonicalName()+"."+name);
    }
}
