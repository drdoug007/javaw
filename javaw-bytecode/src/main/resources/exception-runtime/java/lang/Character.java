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
    public static int digit(int value,int radix){
        if(radix<2 || radix>36)return -1;
        if(value>=0 && value<=65535)return digit((char)value,radix);
        if(!isValidCodePoint(value))return -1;
        int digit=-1;
        if(getType(value)==9)digit=getNumericValue(value);
        else if(value>='A' && value<='Z')digit=value-'A'+10;
        else if(value>='a' && value<='z')digit=value-'a'+10;
        else if(value>=0xff21 && value<=0xff3a)digit=value-0xff21+10;
        else if(value>=0xff41 && value<=0xff5a)digit=value-0xff41+10;
        return digit>=0 && digit<radix?digit:-1;
    }
    public static int getType(int code){return javaw.runtime.UnicodeProperties.flags(code)&31;}
    public static int getType(char code){return getType((int)code);}
    public static int getNumericValue(int code){return javaw.runtime.UnicodeProperties.numeric(code);}
    public static int getNumericValue(char code){return getNumericValue((int)code);}
    public static boolean isDefined(int code){return getType(code)!=0;}
    public static boolean isDigit(int code){return getType(code)==9;}
    public static boolean isLetter(int code){int type=getType(code);return type>=1 && type<=5;}
    public static boolean isLetterOrDigit(int code){return isLetter(code) || isDigit(code);}
    public static boolean isLowerCase(int code){return (javaw.runtime.UnicodeProperties.flags(code)&32)!=0;}
    public static boolean isUpperCase(int code){return (javaw.runtime.UnicodeProperties.flags(code)&64)!=0;}
    public static boolean isAlphabetic(int code){return (javaw.runtime.UnicodeProperties.flags(code)&128)!=0;}
    public static boolean isJavaIdentifierStart(int code){return (javaw.runtime.UnicodeProperties.flags(code)&256)!=0;}
    public static boolean isJavaIdentifierPart(int code){return (javaw.runtime.UnicodeProperties.flags(code)&512)!=0;}
    public static boolean isUnicodeIdentifierStart(int code){return (javaw.runtime.UnicodeProperties.flags(code)&1024)!=0;}
    public static boolean isUnicodeIdentifierPart(int code){return (javaw.runtime.UnicodeProperties.flags(code)&2048)!=0;}
    public static boolean isSpaceChar(int code){int type=getType(code);return type>=12 && type<=14;}
    public static boolean isISOControl(int code){return code>=0 && code<=31 || code>=127 && code<=159;}
    public static boolean isDefined(char code){return isDefined((int)code);}
    public static boolean isDigit(char code){return isDigit((int)code);}
    public static boolean isLetter(char code){return isLetter((int)code);}
    public static boolean isLetterOrDigit(char code){return isLetterOrDigit((int)code);}
    public static boolean isLowerCase(char code){return isLowerCase((int)code);}
    public static boolean isUpperCase(char code){return isUpperCase((int)code);}
    public static boolean isJavaIdentifierStart(char code){return isJavaIdentifierStart((int)code);}
    public static boolean isJavaIdentifierPart(char code){return isJavaIdentifierPart((int)code);}
    public static boolean isUnicodeIdentifierStart(char code){return isUnicodeIdentifierStart((int)code);}
    public static boolean isUnicodeIdentifierPart(char code){return isUnicodeIdentifierPart((int)code);}
    public static boolean isSpaceChar(char code){return isSpaceChar((int)code);}
    public static boolean isISOControl(char code){return isISOControl((int)code);}

    public static final int MIN_CODE_POINT=0,MAX_CODE_POINT=0x10ffff,MIN_SUPPLEMENTARY_CODE_POINT=0x10000;
    public static final char MIN_HIGH_SURROGATE=0xd800,MAX_HIGH_SURROGATE=0xdbff,MIN_LOW_SURROGATE=0xdc00,MAX_LOW_SURROGATE=0xdfff,MIN_SURROGATE=0xd800,MAX_SURROGATE=0xdfff;
    public static boolean isValidCodePoint(int code){return code>=0 && code<=MAX_CODE_POINT;}
    public static boolean isBmpCodePoint(int code){return code>=0 && code<=0xffff;}
    public static boolean isSupplementaryCodePoint(int code){return code>=0x10000 && code<=MAX_CODE_POINT;}
    public static boolean isHighSurrogate(char value){return value>=0xd800 && value<=0xdbff;}
    public static boolean isLowSurrogate(char value){return value>=0xdc00 && value<=0xdfff;}
    public static boolean isSurrogate(char value){return value>=0xd800 && value<=0xdfff;}
    public static boolean isSurrogatePair(char high,char low){return isHighSurrogate(high) && isLowSurrogate(low);}
    public static int charCount(int code){return code>=0x10000?2:1;}
    public static int toCodePoint(char high,char low){return ((high-0xd800)<<10)+(low-0xdc00)+0x10000;}
    public static char highSurrogate(int code){return (char)((code>>>10)+(0xd800-(0x10000>>>10)));}
    public static char lowSurrogate(int code){return (char)((code&1023)+0xdc00);}
    public static char[] toChars(int code){
        if(!isValidCodePoint(code))throw new IllegalArgumentException();
        return isBmpCodePoint(code)?new char[]{(char)code}:new char[]{highSurrogate(code),lowSurrogate(code)};
    }
    public static int toChars(int code,char[] target,int index){
        if(!isValidCodePoint(code))throw new IllegalArgumentException();
        if(isBmpCodePoint(code)){target[index]=(char)code;return 1;}
        // Match the JDK's write order, including partial writes when index == -1.
        target[index+1]=lowSurrogate(code);target[index]=highSurrogate(code);return 2;
    }
    public static int codePointAt(CharSequence text,int index){
        char high=text.charAt(index);if(isHighSurrogate(high) && index+1<text.length()){char low=text.charAt(index+1);if(isLowSurrogate(low))return toCodePoint(high,low);}return high;
    }
    public static int codePointAt(char[] text,int index){
        int length=text.length;char high=text[index];if(isHighSurrogate(high) && index+1<length){char low=text[index+1];if(isLowSurrogate(low))return toCodePoint(high,low);}return high;
    }
    public static int codePointAt(char[] text,int index,int limit){
        if(index>=limit || limit<0 || limit>text.length)throw new IndexOutOfBoundsException();
        char high=text[index];if(isHighSurrogate(high) && index+1<limit){char low=text[index+1];if(isLowSurrogate(low))return toCodePoint(high,low);}return high;
    }
    public static int codePointBefore(CharSequence text,int index){
        char low=text.charAt(index-1);if(isLowSurrogate(low) && index>1){char high=text.charAt(index-2);if(isHighSurrogate(high))return toCodePoint(high,low);}return low;
    }
    public static int codePointBefore(char[] text,int index){
        char low=text[index-1];if(isLowSurrogate(low) && index>1){char high=text[index-2];if(isHighSurrogate(high))return toCodePoint(high,low);}return low;
    }
    public static int codePointBefore(char[] text,int index,int start){
        if(index<=start || start<0 || start>=text.length)throw new IndexOutOfBoundsException();
        char low=text[index-1];if(isLowSurrogate(low) && index-2>=start){char high=text[index-2];if(isHighSurrogate(high))return toCodePoint(high,low);}return low;
    }
    public static int codePointCount(CharSequence text,int begin,int end){
        int length=text.length();if(begin<0 || end<begin || end>length)throw new IndexOutOfBoundsException();int count=0;
        for(int i=begin;i<end;i++){char high=text.charAt(i);if(isHighSurrogate(high) && i+1<end && isLowSurrogate(text.charAt(i+1)))i++;count++;}return count;
    }
    public static int codePointCount(char[] text,int offset,int count){
        if(offset<0 || count<0 || offset>text.length-count)throw new IndexOutOfBoundsException();int result=0,end=offset+count;
        for(int i=offset;i<end;i++){if(isHighSurrogate(text[i]) && i+1<end && isLowSurrogate(text[i+1]))i++;result++;}return result;
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
    public static int compare(char left,char right){return left-right;}
    public boolean equals(Object other){return other instanceof Character && value==((Character)other).value;}
    public int hashCode(){return (int)value;}
    public static int hashCode(char value){return (int)value;}
    public int compareTo(Character other){return compare(value,other.value);}
}
