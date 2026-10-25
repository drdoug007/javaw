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
    public static final int MIN_RADIX=2,MAX_RADIX=36;
    // Unicode decimal-digit blocks in the BMP; supplementary code points are not chars.
    private static final int[] DECIMAL_STARTS={0x30,0x660,0x6f0,0x7c0,0x966,0x9e6,0xa66,0xae6,0xb66,0xbe6,0xc66,0xce6,0xd66,0xde6,0xe50,0xed0,0xf20,0x1040,0x1090,0x17e0,0x1810,0x1946,0x19d0,0x1a80,0x1a90,0x1b50,0x1bb0,0x1c40,0x1c50,0xa620,0xa8d0,0xa900,0xa9d0,0xa9f0,0xaa50,0xabf0,0xff10};
    public static int digit(char ch,int radix){
        if(radix<2 || radix>36)return -1;int value=-1;
        if(ch>='a' && ch<='z')value=ch-'a'+10;else if(ch>='A' && ch<='Z')value=ch-'A'+10;
        else if(ch>=0xff21 && ch<=0xff3a)value=ch-0xff21+10;else if(ch>=0xff41 && ch<=0xff5a)value=ch-0xff41+10;
        else for(int start:DECIMAL_STARTS)if(ch>=start && ch<start+10){value=ch-start;break;}
        return value<radix?value:-1;
    }
    public static int toUpperCase(int value){return javaw.runtime.UnicodeCase.upper(value);}
    public static char toUpperCase(char value){return (char)toUpperCase((int)value);}
    public static int toLowerCase(int value){return javaw.runtime.UnicodeCase.lower(value);}
    public static char toLowerCase(char value){return (char)toLowerCase((int)value);}
    public static boolean isWhitespace(char value){return isWhitespace((int)value);}
    public static boolean isWhitespace(int value){
        return value>=9 && value<=13 || value>=0x1c && value<=0x20 || value==0x1680
            || value>=0x2000 && value<=0x2006 || value>=0x2008 && value<=0x200a
            || value==0x2028 || value==0x2029 || value==0x205f || value==0x3000;
    }
    public static char forDigit(int digit,int radix){return radix<2 || radix>36 || digit<0 || digit>=radix?0:(char)(digit<10?'0'+digit:'a'+digit-10);}
    public String toString(){return String.valueOf(value);}
    public static String toString(char value){return String.valueOf(value);}
    public static int compare(char left,char right){return left==right?0:left<right?-1:1;}
    public boolean equals(Object other){return other instanceof Character && value==((Character)other).value;}
    public int hashCode(){return (int)value;}
    public static int hashCode(char value){return (int)value;}
    public int compareTo(Character other){return compare(value,other.value);}
}
