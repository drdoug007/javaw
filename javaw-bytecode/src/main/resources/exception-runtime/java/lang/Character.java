package java.lang;

/** Value and identity operations required by bootstrap adapters. */
public final class Character implements Comparable<Character>,java.io.Serializable {
    public static final Class<Character> TYPE=(Class<Character>)javaw.runtime.ExceptionSupport.classMirror(-22);
    private final char value;
    public Character(char value){this.value=value;}
    public char charValue(){return value;}
    private static Character[] cache;
    public static Character valueOf(char value){
        if(value>=0 && value<=127){
            if(cache==null){
                Character[] values=new Character[128];
                for(int i=0;i<values.length;i++)values[i]=new Character((char)(i));
                cache=values;
            }
            return cache[value];
        }
        return new Character(value);
    }
    public String toString(){return String.valueOf(value);}
    public static String toString(char value){return String.valueOf(value);}
    public static int compare(char left,char right){return left==right?0:left<right?-1:1;}
    public boolean equals(Object other){return other instanceof Character && value==((Character)other).value;}
    public int hashCode(){return (int)value;}
    public static int hashCode(char value){return (int)value;}
    public int compareTo(Character other){return compare(value,other.value);}
}
