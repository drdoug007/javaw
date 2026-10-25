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
    public String(StringBuilder text){this(text.toString());}
    public String(StringBuffer text){this(text.toString());}
    public String(char[] chars,int offset,int count){
        if(offset<0 || count<0 || offset>chars.length-count)throw new StringIndexOutOfBoundsException();
        value=new char[count];System.arraycopy(chars,offset,value,0,count);
    }
    public String(int[] codePoints,int offset,int count){
        int sourceLength=codePoints.length;
        if(offset<0 || count<0 || offset>sourceLength-count)throw new IndexOutOfBoundsException();
        int size=count;
        for(int i=offset;i<offset+count;i++){
            int code=codePoints[i];if(code<0 || code>0x10ffff)throw new IllegalArgumentException();
            if(code>=0x10000){if(size==Integer.MAX_VALUE)throw new OutOfMemoryError();size++;}
        }
        value=new char[size];int out=0;
        for(int i=offset;i<offset+count;i++){int code=codePoints[i];if(code<0x10000)value[out++]=(char)code;
            else{value[out++]=(char)(0xd800+((code-0x10000)>>10));value[out++]=(char)(0xdc00+((code-0x10000)&1023));}}
    }
    public boolean regionMatches(int offset,String other,int otherOffset,int count){
        if(offset<0 || otherOffset<0 || (long)offset>length()-(long)count || (long)otherOffset>other.length()-(long)count)return false;
        for(int i=0;i<count;i++)if(value[offset+i]!=other.value[otherOffset+i])return false;return true;
    }
    public boolean isBlank(){return whitespaceStart()==length();}
    private int whitespaceStart(){int i=0;while(i<length() && Character.isWhitespace(value[i]))i++;return i;}
    private int whitespaceEnd(){int i=length();while(i>0 && Character.isWhitespace(value[i-1]))i--;return i;}
    public String stripLeading(){return substring(whitespaceStart());}
    public String stripTrailing(){return substring(0,whitespaceEnd());}
    public String strip(){int start=whitespaceStart();return start==length()?"":substring(start,whitespaceEnd());}
    public String repeat(int count){
        if(count<0)throw new IllegalArgumentException();if(count==1)return this;if(count==0 || length()==0)return "";
        if(count>Integer.MAX_VALUE/length())throw new OutOfMemoryError();
        char[] chars=new char[count*length()];for(int i=0;i<chars.length;i+=length())System.arraycopy(value,0,chars,i,length());return new String(chars,true);
    }
    public static String join(CharSequence delimiter,CharSequence... elements){
        String separator=delimiter.toString();StringBuilder out=new StringBuilder();
        for(int i=0;i<elements.length;i++){if(i!=0)out.append(separator);out.append(String.valueOf((Object)elements[i]));}return out.toString();
    }
    public static String join(CharSequence delimiter,Iterable<? extends CharSequence> elements){
        String separator=delimiter.toString();StringBuilder out=new StringBuilder();boolean first=true;
        for(CharSequence element:elements){if(!first)out.append(separator);first=false;out.append(String.valueOf((Object)element));}return out.toString();
    }
    private int lineEnd(int start){int end=start;while(end<length() && value[end]!='\n' && value[end]!='\r')end++;return end;}
    private int nextLine(int start){int end=lineEnd(start);if(end==length())return end;return end+1<length() && value[end]=='\r' && value[end+1]=='\n'?end+2:end+1;}
    public java.util.stream.Stream<String> lines(){
        return java.util.stream.Stream.iterate(Integer.valueOf(0),i->i.intValue()<length(),i->Integer.valueOf(nextLine(i.intValue())))
            .map(i->substring(i.intValue(),lineEnd(i.intValue())));
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
    public int lastIndexOf(String text){return lastIndexOf(text,length());}
    public int lastIndexOf(String text,int from){
        int size=text.length();int start=length()-size;if(from<start)start=from;
        if(size==0)return start<0?-1:start;
        for(int i=start;i>=0;i--)if(startsWith(text,i))return i;return -1;
    }
    public int lastIndexOf(int code){return lastIndexOf(code,length()-1);}
    public int lastIndexOf(int code,int from){
        if(code<0 || code>1114111)return -1;
        int start=length()-(code<65536?1:2);if(from<start)start=from;
        for(int i=start;i>=0;i--){if(code<65536){if(value[i]==code)return i;}else if(value[i]==(char)(55296+((code-65536)>>10)) && value[i+1]==(char)(56320+((code-65536)&1023)))return i;}return -1;
    }
    public int codePointAt(int index){
        char first=charAt(index);if(first>=55296 && first<=56319 && index+1<length()){
            char second=value[index+1];if(second>=56320 && second<=57343)return 65536+((first-55296)<<10)+(second-56320);
        }return first;
    }
    public int codePointBefore(int index){
        if(index<1 || index>length())throw new StringIndexOutOfBoundsException(index);
        char last=value[index-1];if(last>=56320 && last<=57343 && index>1){char first=value[index-2];if(first>=55296 && first<=56319)return 65536+((first-55296)<<10)+(last-56320);}return last;
    }
    public int codePointCount(int begin,int end){
        if(begin<0 || end>length() || begin>end)throw new IndexOutOfBoundsException();
        int count=0;for(int i=begin;i<end;i++,count++)if(value[i]>=55296 && value[i]<=56319 && i+1<end && value[i+1]>=56320 && value[i+1]<=57343)i++;return count;
    }
    public int offsetByCodePoints(int index,int offset){
        if(index<0 || index>length())throw new IndexOutOfBoundsException();
        int i=index;
        while(offset>0){if(i==length())throw new IndexOutOfBoundsException();char first=value[i++];if(first>=55296 && first<=56319 && i<length() && value[i]>=56320 && value[i]<=57343)i++;offset--;}
        while(offset<0){if(i==0)throw new IndexOutOfBoundsException();char last=value[--i];if(last>=56320 && last<=57343 && i>0 && value[i-1]>=55296 && value[i-1]<=56319)i--;offset++;}return i;
    }
    public boolean contentEquals(CharSequence text){if(length()!=text.length())return false;for(int i=0;i<length();i++)if(value[i]!=text.charAt(i))return false;return true;}
    public boolean contentEquals(StringBuffer text){return contentEquals((CharSequence)text);}
    public String replace(CharSequence target,CharSequence replacement){
        String from=target.toString(),to=replacement.toString();int size=from.length();
        StringBuilder out=new StringBuilder();int start=0;
        if(size==0){out.append(to);for(int i=0;i<length();i++)out.append(value[i]).append(to);return out.toString();}
        int next=indexOf(from,start);if(next<0)return this;
        while(next>=0){out.append(this,start,next).append(to);start=next+size;next=indexOf(from,start);}
        return out.append(this,start,length()).toString();
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
