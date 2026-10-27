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
    public char charAt(int index){if(index<0 || index>=count)throw new StringIndexOutOfBoundsException(index);return value[index];}
    public void setCharAt(int index,char ch){if(index<0 || index>=count)throw new StringIndexOutOfBoundsException(index);value[index]=ch;}
    public void setLength(int length){if(length<0)throw new StringIndexOutOfBoundsException(length);ensureCapacity(length);for(int i=count;i<length;i++)value[i]=0;count=length;}
    public StringBuilder append(char ch){grow(1);value[count++]=ch;return this;}
    public StringBuilder append(String text){if(text==null)text="null";int length=text.length();grow(length);text.getChars(0,length,value,count);count+=length;return this;}
    public StringBuilder append(Object object){return append(String.valueOf(object));}
    public StringBuilder append(CharSequence text){if(text==null)text="null";return append(text,0,text.length());}
    public StringBuilder append(CharSequence text,int start,int end){
        if(text==null)text="null";if(start<0 || end<start || end>text.length())throw new IndexOutOfBoundsException();
        int length=end-start;grow(length);for(int i=0;i<length;i++)value[count+i]=text.charAt(start+i);count+=length;return this;
    }
    public StringBuilder append(char[] chars){return append(chars,0,chars.length);}
    public StringBuilder append(char[] chars,int offset,int length){if(offset<0 || length<0 || offset>chars.length-length)throw new IndexOutOfBoundsException();grow(length);System.arraycopy(chars,offset,value,count,length);count+=length;return this;}
    public StringBuilder append(boolean v){return append(String.valueOf(v));}public StringBuilder append(int v){return append(String.valueOf(v));}public StringBuilder append(long v){return append(String.valueOf(v));}public StringBuilder append(float v){return append(String.valueOf(v));}public StringBuilder append(double v){return append(String.valueOf(v));}
    public StringBuilder delete(int start,int end){if(start<0 || start>count || start>end)throw new StringIndexOutOfBoundsException();if(end>count)end=count;System.arraycopy(value,end,value,start,count-end);count-=end-start;return this;}
    public StringBuilder deleteCharAt(int index){if(index<0 || index>=count)throw new StringIndexOutOfBoundsException(index);return delete(index,index+1);}
    public StringBuilder insert(int offset,String text){if(offset<0 || offset>count)throw new StringIndexOutOfBoundsException(offset);if(text==null)text="null";int length=text.length();grow(length);System.arraycopy(value,offset,value,offset+length,count-offset);text.getChars(0,length,value,offset);count+=length;return this;}
    public StringBuilder append(StringBuffer text){return append((CharSequence)text);}
    public StringBuilder appendCodePoint(int code){
        if(code<0 || code>1114111)throw new IllegalArgumentException();
        if(code<65536)return append((char)code);
        grow(2);value[count++]=(char)(55296+((code-65536)>>10));value[count++]=(char)(56320+((code-65536)&1023));return this;
    }
    public StringBuilder insert(int offset,char ch){return insert(offset,String.valueOf(ch));}
    public StringBuilder replace(int start,int end,String text){
        if(start<0 || start>count || start>end)throw new StringIndexOutOfBoundsException();if(end>count)end=count;
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
    public String substring(int start){return substring(start,count);}public String substring(int start,int end){if(start<0 || end>count || start>end)throw new StringIndexOutOfBoundsException();return new String(value,start,end-start);}
    public CharSequence subSequence(int start,int end){return substring(start,end);}
    public void getChars(int start,int end,char[] target,int offset){if(start<0 || end>count || start>end)throw new StringIndexOutOfBoundsException();System.arraycopy(value,start,target,offset,end-start);}
    public int indexOf(String text){return toString().indexOf(text);}public int indexOf(String text,int from){return toString().indexOf(text,from);}
    public String toString(){return new String(value,0,count);}
    public StringBuilder insert(int offset,char[] text){if(offset<0 || offset>count)throw new StringIndexOutOfBoundsException(offset);return insert(offset,new String(text));}
    public StringBuilder insert(int offset,char[] text,int start,int size){if(offset<0 || offset>count)throw new StringIndexOutOfBoundsException(offset);int sourceLength=text.length;if(start<0 || size<0 || start>sourceLength-size)throw new StringIndexOutOfBoundsException();grow(size);System.arraycopy(value,offset,value,offset+size,count-offset);System.arraycopy(text,start,value,offset,size);count+=size;return this;}
    public StringBuilder insert(int offset,CharSequence text){if(text==null)text="null";if(text instanceof String)return insert(offset,(String)text);return insertSequence(offset,text,0,text.length());}
    private StringBuilder insertSequence(int offset,CharSequence text,int start,int end){if(text==null)text="null";if(offset<0 || offset>count)throw new StringIndexOutOfBoundsException();int sourceLength=text.length();if(start<0 || end<start || end>sourceLength)throw new IndexOutOfBoundsException();int size=end-start;grow(size);System.arraycopy(value,offset,value,offset+size,count-offset);count+=size;for(int j=0;j<size;j++)value[offset+j]=text.charAt(start+j);return this;}
    public StringBuilder insert(int offset,CharSequence text,int start,int end){return insertSequence(offset,text,start,end);}
}
