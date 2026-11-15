package java.lang;

/** Mutable UTF-16 text for the selected bytecode library contract. */
public final class StringBuilder implements CharSequence,Comparable<StringBuilder>,java.io.Serializable {
    private char[] value;private int count;
    public StringBuilder(){this(16);}
    public StringBuilder(int capacity){value=new char[capacity];}
    public StringBuilder(String text){this(text.length()+16);append(text);}
    public StringBuilder(CharSequence text){this(text.length()+16);append(text);}
    public int length(){return count;}public int capacity(){return value.length;}public boolean isEmpty(){return count==0;}
    public void ensureCapacity(int minimum){
        if(minimum<=value.length)return;int grown=value.length*2+2;if(grown<minimum)grown=minimum;if(grown<0)throw new OutOfMemoryError();
        char[] replacement=new char[grown];System.arraycopy(value,0,replacement,0,count);value=replacement;
    }
    public void trimToSize(){if(count<value.length){char[] replacement=new char[count];System.arraycopy(value,0,replacement,0,count);value=replacement;}}
    private void grow(int extra){if(extra<0 || count>2147483647-extra)throw new OutOfMemoryError();ensureCapacity(count+extra);}
    public char charAt(int index){String.checkIndex(index,count);return value[index];}
    public void setCharAt(int index,char ch){String.checkIndex(index,count);value[index]=ch;}
    public void setLength(int length){if(length<0)throw new StringIndexOutOfBoundsException(length);ensureCapacity(length);for(int i=count;i<length;i++)value[i]=0;count=length;}
    public StringBuilder append(char ch){grow(1);value[count++]=ch;return this;}
    public StringBuilder append(String text){if(text==null)text="null";int length=text.length();grow(length);text.getChars(0,length,value,count);count+=length;return this;}
    public StringBuilder append(Object object){return append(String.valueOf(object));}
    public StringBuilder append(CharSequence text){if(text==null)text="null";return append(text,0,text.length());}
    public StringBuilder append(CharSequence text,int start,int end){
        if(text==null)text="null";int textLength=text.length();if(start<0 || start>end || end>textLength)throw new IndexOutOfBoundsException(String.rangeMessage(start,end,textLength));
        int length=end-start;grow(length);for(int i=0;i<length;i++)value[count+i]=text.charAt(start+i);count+=length;return this;
    }
    public StringBuilder append(char[] chars){return append(chars,0,chars.length);}
    public StringBuilder append(char[] chars,int offset,int length){int end=offset+length;if(offset<0 || offset>end || end>chars.length)throw new IndexOutOfBoundsException(String.rangeMessage(offset,end,chars.length));grow(length);System.arraycopy(chars,offset,value,count,length);count+=length;return this;}
    public StringBuilder append(boolean v){return append(String.valueOf(v));}public StringBuilder append(int v){return append(String.valueOf(v));}public StringBuilder append(long v){return append(String.valueOf(v));}public StringBuilder append(float v){return append(String.valueOf(v));}public StringBuilder append(double v){return append(String.valueOf(v));}
    public StringBuilder delete(int start,int end){if(end>count)end=count;String.checkBoundsBeginEnd(start,end,count);System.arraycopy(value,end,value,start,count-end);count-=end-start;return this;}
    public StringBuilder deleteCharAt(int index){String.checkIndex(index,count);return delete(index,index+1);}
    public StringBuilder insert(int offset,String text){String.checkOffset(offset,count);if(text==null)text="null";int length=text.length();grow(length);System.arraycopy(value,offset,value,offset+length,count-offset);text.getChars(0,length,value,offset);count+=length;return this;}
    public StringBuilder append(StringBuffer text){return append((CharSequence)text);}
    public StringBuilder appendCodePoint(int code){
        if(code<0 || code>1114111)throw new IllegalArgumentException("Not a valid Unicode code point: 0x"+Integer.toHexString(code).toUpperCase(java.util.Locale.ROOT));
        if(code<65536)return append((char)code);
        grow(2);value[count++]=(char)(55296+((code-65536)>>10));value[count++]=(char)(56320+((code-65536)&1023));return this;
    }
    public StringBuilder insert(int offset,char ch){return insert(offset,String.valueOf(ch));}
    public StringBuilder replace(int start,int end,String text){
        if(end>count)end=count;String.checkBoundsBeginEnd(start,end,count);
        int size=text.length(),removed=end-start;if(size>removed)grow(size-removed);
        System.arraycopy(value,end,value,start+size,count-end);text.getChars(0,size,value,start);count+=size-removed;return this;
    }
    public int lastIndexOf(String text){return toString().lastIndexOf(text);}
    public int lastIndexOf(String text,int from){return toString().lastIndexOf(text,from);}
    public int codePointAt(int index){return toString().codePointAt(index);}
    public int codePointBefore(int index){return toString().codePointBefore(index);}
    public int codePointCount(int begin,int end){return toString().codePointCount(begin,end);}
    public int offsetByCodePoints(int index,int offset){return toString().offsetByCodePoints(index,offset);}
    public int compareTo(StringBuilder other){int end=count<other.count?count:other.count;for(int i=0;i<end;i++)if(value[i]!=other.value[i])return value[i]-other.value[i];return count-other.count;}
    public StringBuilder reverse(){
        for(int i=0,j=count-1;i<j;i++,j--){char ch=value[i];value[i]=value[j];value[j]=ch;}
        for(int i=0;i+1<count;i++)if(value[i]>=56320 && value[i]<=57343 && value[i+1]>=55296 && value[i+1]<=56319){char ch=value[i];value[i]=value[i+1];value[++i]=ch;}return this;
    }
    public String substring(int start){return substring(start,count);}public String substring(int start,int end){String.checkBoundsBeginEnd(start,end,count);return new String(value,start,end-start);}
    public CharSequence subSequence(int start,int end){return substring(start,end);}
    public void getChars(int start,int end,char[] target,int offset){String.checkBoundsBeginEnd(start,end,count);int stop=offset+(end-start);if(offset<0 || offset>stop || stop>target.length)throw new IndexOutOfBoundsException(String.rangeMessage(offset,stop,target.length));System.arraycopy(value,start,target,offset,end-start);}
    public int indexOf(String text){return toString().indexOf(text);}public int indexOf(String text,int from){return toString().indexOf(text,from);}
    public String toString(){return new String(value,0,count);}
    public StringBuilder insert(int offset,char[] text){String.checkOffset(offset,count);return insert(offset,new String(text));}
    public StringBuilder insert(int offset,char[] text,int start,int size){String.checkOffset(offset,count);String.checkBoundsBeginEnd(start,start+size,text.length);grow(size);System.arraycopy(value,offset,value,offset+size,count-offset);System.arraycopy(text,start,value,offset,size);count+=size;return this;}
    public StringBuilder insert(int offset,CharSequence text){if(text==null)text="null";if(text instanceof String)return insert(offset,(String)text);return insertSequence(offset,text,0,text.length());}
    private StringBuilder insertSequence(int offset,CharSequence text,int start,int end){if(text==null)text="null";String.checkOffset(offset,count);int sourceLength=text.length();if(start<0 || start>end || end>sourceLength)throw new IndexOutOfBoundsException(String.rangeMessage(start,end,sourceLength));int size=end-start;grow(size);System.arraycopy(value,offset,value,offset+size,count-offset);count+=size;for(int j=0;j<size;j++)value[offset+j]=text.charAt(start+j);return this;}
    public StringBuilder insert(int offset,CharSequence text,int start,int end){return insertSequence(offset,text,start,end);}
}
