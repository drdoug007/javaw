package java.lang;

/** UTF-16 support for exception messages and trace metadata, not the complete JDK String API. */
public final class String implements CharSequence,Comparable<String>,java.io.Serializable {
    private final char[] value;
    private static String interned;
    private String nextInterned;
    public String intern() {
        for(String text=interned;text!=null;text=text.nextInterned) if(equals(text))return text;
        nextInterned=interned;interned=this;return this;
    }
    public String() { value = new char[0]; }
    public String(char[] chars) { value = chars.clone(); }
    private String(char[] chars, boolean trusted) { value = chars; }
    public String(String text){value=text.value;}
    public String(char[] chars,int offset,int count){
        if(offset<0 || count<0 || offset>chars.length-count)throw new StringIndexOutOfBoundsException();
        value=new char[count];System.arraycopy(chars,offset,value,0,count);
    }
    public boolean isEmpty(){return value.length==0;}
    public char[] toCharArray(){return value.clone();}
    public void getChars(int start,int end,char[] destination,int offset){
        if(start<0 || end>length() || start>end)throw new StringIndexOutOfBoundsException();System.arraycopy(value,start,destination,offset,end-start);
    }
    public String substring(int start){return substring(start,length());}
    public String substring(int start,int end){
        if(start<0 || end>length() || start>end)throw new StringIndexOutOfBoundsException();
        if(start==0 && end==length())return this;return new String(value,start,end-start);
    }
    public CharSequence subSequence(int start,int end){return substring(start,end);}
    public int compareTo(String other){int count=length()<other.length()?length():other.length();for(int i=0;i<count;i++)if(value[i]!=other.value[i])return value[i]-other.value[i];return length()-other.length();}
    public boolean startsWith(String prefix){return startsWith(prefix,0);}
    public boolean startsWith(String prefix,int offset){if(offset<0 || offset>length()-prefix.length())return false;for(int i=0;i<prefix.length();i++)if(value[offset+i]!=prefix.value[i])return false;return true;}
    public boolean endsWith(String suffix){return startsWith(suffix,length()-suffix.length());}
    public int indexOf(String text){return indexOf(text,0);}
    public int indexOf(String text,int from){int size=text.length();if(from<0)from=0;if(size==0)return from>length()?length():from;for(int i=from;i<=length()-size;i++)if(startsWith(text,i))return i;return -1;}
    public int indexOf(int code){return indexOf(code,0);}
    public int indexOf(int code,int from){
        if(from<0)from=0;if(code<0 || code>1114111)return -1;
        for(int i=from;i<length();i++){if(code<65536){if(value[i]==code)return i;}else if(i+1<length() && value[i]==(char)(55296+((code-65536)>>10)) && value[i+1]==(char)(56320+((code-65536)&1023)))return i;}return -1;
    }
    public boolean contains(CharSequence text){return indexOf(text.toString())>=0;}
    public String replace(char oldChar,char newChar){int index=indexOf(oldChar);if(index<0 || oldChar==newChar)return this;char[] chars=value.clone();for(int i=index;i<chars.length;i++)if(chars[i]==oldChar)chars[i]=newChar;return new String(chars,true);}
    public String trim(){int start=0,end=length();while(start<end && value[start]<=32)start++;while(start<end && value[end-1]<=32)end--;return substring(start,end);}
    public static String valueOf(char[] chars){return new String(chars);}
    public static String valueOf(char[] chars,int offset,int count){return new String(chars,offset,count);}
    public int length() { return value.length; }
    public char charAt(int index) { if(index<0 || index>=value.length)throw new StringIndexOutOfBoundsException(index);return value[index]; }
    public String toString() { return this; }
    public boolean equals(Object other) {
        if (!(other instanceof String)) return false;
        String text = (String) other;
        if (value.length != text.value.length) return false;
        for (int i=0;i<value.length;i++) if (value[i]!=text.value[i]) return false;
        return true;
    }
    public int hashCode() {
        int hash=0;for(int i=0;i<value.length;i++)hash=31*hash+value[i];return hash;
    }
    public String concat(String other) {
        int length=other.value.length;
        if(length==0)return this;
        char[] chars=new char[value.length+length];
        System.arraycopy(value,0,chars,0,value.length);
        System.arraycopy(other.value,0,chars,value.length,length);
        return new String(chars,true);
    }
    public static String valueOf(int number) {
        char[] chars=new char[11];int end=11;boolean negative=number<0;
        if(!negative)number=-number;
        do {chars[--end]=(char)('0'-(number%10));number/=10;}while(number!=0);
        if(negative)chars[--end]='-';
        char[] result=new char[11-end];System.arraycopy(chars,end,result,0,result.length);
        return new String(result,true);
    }
    public static String valueOf(Object value) { return value==null ? "null" : value.toString(); }
    public static String valueOf(boolean value) { return value ? "true" : "false"; }
    public static String valueOf(char value) { return new String(new char[]{value},true); }
    public static String valueOf(float value) { return javaw.runtime.Decimal.floatText(value); }
    public static String valueOf(double value) { return javaw.runtime.Decimal.doubleText(value); }
    public static String valueOf(long number) {
        char[] chars=new char[20];int end=20;boolean negative=number<0;
        if(!negative)number=-number;
        do {chars[--end]=(char)('0'-(number%10));number/=10;}while(number!=0);
        if(negative)chars[--end]='-';
        char[] result=new char[20-end];System.arraycopy(chars,end,result,0,result.length);
        return new String(result,true);
    }

}
