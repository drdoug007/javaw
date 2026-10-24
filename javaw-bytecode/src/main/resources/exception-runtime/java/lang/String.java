package java.lang;

/** UTF-16 support for exception messages and trace metadata, not the complete JDK String API. */
public final class String {
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
    public int length() { return value.length; }
    public char charAt(int index) { return value[index]; }
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
    public static String valueOf(long number) {
        char[] chars=new char[20];int end=20;boolean negative=number<0;
        if(!negative)number=-number;
        do {chars[--end]=(char)('0'-(number%10));number/=10;}while(number!=0);
        if(negative)chars[--end]='-';
        char[] result=new char[20-end];System.arraycopy(chars,end,result,0,result.length);
        return new String(result,true);
    }

}
