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
        int length=chars.length;
        if(offset<0 || count<0 || offset>length-count)throw new StringIndexOutOfBoundsException();
        value=new char[count];System.arraycopy(chars,offset,value,0,count);
    }
    @Deprecated public String(byte[] bytes,int highByte){this(bytes,highByte,0,bytes.length);}
    @Deprecated public String(byte[] bytes,int highByte,int offset,int count){
        int length=bytes.length;
        if(offset<0 || count<0 || offset>length-count)throw new StringIndexOutOfBoundsException();
        value=new char[count];int high=(highByte&255)<<8;
        for(int i=0;i<count;i++)value[i]=(char)(high|(bytes[offset+i]&255));
    }
    /** The isolated runtime's default charset is UTF-8. */
    public String(byte[] bytes){this(bytes,0,bytes.length);}
    public String(byte[] bytes,int offset,int count){value=decodeUtf8(bytes,offset,count);}
    public byte[] getBytes(){
        long length=0;
        for(int i=0;i<value.length;i++){
            int unit=value[i];
            if(unit<0x80)length++;
            else if(unit<0x800)length+=2;
            else if(unit>=0xd800 && unit<=0xdbff && i+1<value.length && value[i+1]>=0xdc00 && value[i+1]<=0xdfff){length+=4;i++;}
            else length+=unit>=0xd800 && unit<=0xdfff?1:3;
        }
        if(length>Integer.MAX_VALUE)throw new OutOfMemoryError();
        byte[] bytes=new byte[(int)length];int out=0;
        for(int i=0;i<value.length;i++){
            int unit=value[i];
            if(unit<0x80)bytes[out++]=(byte)unit;
            else if(unit<0x800){bytes[out++]=(byte)(0xc0|(unit>>6));bytes[out++]=(byte)(0x80|(unit&63));}
            else if(unit>=0xd800 && unit<=0xdbff && i+1<value.length && value[i+1]>=0xdc00 && value[i+1]<=0xdfff){
                int code=0x10000+((unit-0xd800)<<10)+(value[++i]-0xdc00);
                bytes[out++]=(byte)(0xf0|(code>>18));bytes[out++]=(byte)(0x80|((code>>12)&63));
                bytes[out++]=(byte)(0x80|((code>>6)&63));bytes[out++]=(byte)(0x80|(code&63));
            }else if(unit>=0xd800 && unit<=0xdfff)bytes[out++]=63;
            else{bytes[out++]=(byte)(0xe0|(unit>>12));bytes[out++]=(byte)(0x80|((unit>>6)&63));bytes[out++]=(byte)(0x80|(unit&63));}
        }
        return bytes;
    }

    private static boolean continuation(int value){return (value&0xc0)==0x80;}
    private static char[] decodeUtf8(byte[] bytes,int offset,int count){
        int length=bytes.length;
        if(offset<0 || count<0 || offset>length-count)throw new StringIndexOutOfBoundsException();
        char[] decoded=new char[count];int out=0,end=offset+count;
        while(offset<end){
            int first=bytes[offset++]&255;
            if(first<128){decoded[out++]=(char)first;continue;}
            if(first>=0xc2 && first<=0xdf){
                if(offset<end && continuation(bytes[offset]))decoded[out++]=(char)(((first&31)<<6)|(bytes[offset++]&63));
                else decoded[out++]='\ufffd';
                continue;
            }
            if(first>=0xe0 && first<=0xef){
                if(offset==end){decoded[out++]='\ufffd';continue;}
                int second=bytes[offset]&255;
                if(!continuation(second) || first==0xe0 && second<0xa0){decoded[out++]='\ufffd';continue;}
                offset++;
                if(offset==end || !continuation(bytes[offset])){decoded[out++]='\ufffd';continue;}
                int code=((first&15)<<12)|((second&63)<<6)|(bytes[offset++]&63);
                decoded[out++]=(char)(code>=0xd800 && code<=0xdfff?0xfffd:code);
                continue;
            }
            if(first>=0xf0 && first<=0xf4){
                if(offset==end){decoded[out++]='\ufffd';continue;}
                int second=bytes[offset]&255;
                if(!continuation(second) || first==0xf0 && second<0x90 || first==0xf4 && second>0x8f){decoded[out++]='\ufffd';continue;}
                offset++;
                if(offset==end || !continuation(bytes[offset])){decoded[out++]='\ufffd';continue;}
                int third=bytes[offset++]&63;
                if(offset==end || !continuation(bytes[offset])){decoded[out++]='\ufffd';continue;}
                int code=((first&7)<<18)|((second&63)<<12)|(third<<6)|(bytes[offset++]&63);
                code-=0x10000;decoded[out++]=(char)(0xd800+(code>>10));decoded[out++]=(char)(0xdc00+(code&1023));
                continue;
            }
            decoded[out++]='\ufffd';
        }
        if(out==count)return decoded;
        char[] result=new char[out];System.arraycopy(decoded,0,result,0,out);return result;
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
    public static final java.util.Comparator<String> CASE_INSENSITIVE_ORDER=new CaseComparator();
    private static final class CaseComparator implements java.util.Comparator<String>,java.io.Serializable {
        public int compare(String left,String right){return left.compareToIgnoreCase(right);}
    }
    private static int foldedDifference(int left,int right){
        int upperLeft=Character.toUpperCase(left),upperRight=Character.toUpperCase(right);
        return upperLeft==upperRight?0:Character.toLowerCase(upperLeft)-Character.toLowerCase(upperRight);
    }
    // A mismatching low surrogate can belong to a pair whose high halves already matched.
    // Negative results identify pairs starting at index, so the caller advances two units.
    private int comparisonPoint(int index,int begin,int end){
        int unit=value[index];
        if(unit>=0xdc00 && unit<=0xdfff && index>begin && value[index-1]>=0xd800 && value[index-1]<=0xdbff)
            return 0x10000+((value[index-1]-0xd800)<<10)+(unit-0xdc00);
        if(unit>=0xd800 && unit<=0xdbff && index+1<end && value[index+1]>=0xdc00 && value[index+1]<=0xdfff)
            return -(0x10000+((unit-0xd800)<<10)+(value[index+1]-0xdc00));
        return unit;
    }
    private int compareFolded(int begin,int count,String other,int otherBegin,int otherCount){
        int end=begin+count,otherEnd=otherBegin+otherCount;
        for(int left=begin,right=otherBegin;left<end && right<otherEnd;left++,right++){
            int a=value[left],b=other.value[right];if(a==b || foldedDifference(a,b)==0)continue;
            a=comparisonPoint(left,begin,end);b=other.comparisonPoint(right,otherBegin,otherEnd);
            if(a<0){a=-a;left++;}if(b<0){b=-b;right++;}
            int difference=foldedDifference(a,b);if(difference!=0)return difference;
        }return count-otherCount;
    }
    public int compareToIgnoreCase(String other){return compareFolded(0,length(),other,0,other.length());}
    public boolean equalsIgnoreCase(String other){return other!=null && length()==other.length() && compareFolded(0,length(),other,0,other.length())==0;}
    public boolean regionMatches(boolean ignoreCase,int offset,String other,int otherOffset,int count){
        if(!ignoreCase)return regionMatches(offset,other,otherOffset,count);
        if(offset<0 || otherOffset<0 || (long)offset>length()-(long)count || (long)otherOffset>other.length()-(long)count)return false;
        return count<=0 || compareFolded(offset,count,other,otherOffset,count)==0;
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

    public static String copyValueOf(char[] chars){return new String(chars);}
    public static String copyValueOf(char[] chars,int offset,int count){return new String(chars,offset,count);}
    public String translateEscapes(){
        char[] decoded=new char[length()];int out=0;
        for(int i=0;i<length();i++){
            char c=value[i];
            if(c!=92){decoded[out++]=c;continue;}
            if(++i==length())throw new IllegalArgumentException();
            c=value[i];
            switch(c){
                case 'b':c=8;break;case 't':c=9;break;case 'n':c=10;break;
                case 'f':c=12;break;case 'r':c=13;break;case 's':c=32;break;
                case 34:case 39:case 92:break;
                case 10:continue;
                case 13:if(i+1<length() && value[i+1]==10)i++;continue;
                default:
                    if(c<'0' || c>'7')throw new IllegalArgumentException();
                    int code=c-'0',remaining=c<='3'?2:1;
                    while(remaining-->0 && i+1<length() && value[i+1]>='0' && value[i+1]<='7')code=(code<<3)+(value[++i]-'0');
                    c=(char)code;
            }
            decoded[out++]=c;
        }
        return new String(decoded,0,out);
    }
    public String indent(int amount){
        if(isEmpty())return "";
        String padding=amount>0?" ".repeat(amount):"";
        StringBuilder out=new StringBuilder();
        for(int start=0;start<length();){
            int end=lineEnd(start),content=start;
            if(amount<0){long remaining=-(long)amount;
                while(content<end && remaining>0 && Character.isWhitespace(value[content])){content++;remaining--;}}
            out.append(padding).append(this,content,end).append((char)10);
            start=nextLine(start);
        }
        return out.toString();
    }
}
