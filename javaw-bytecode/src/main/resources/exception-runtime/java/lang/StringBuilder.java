package java.lang;

/** Mutable UTF-16 text for the selected bytecode library contract. */
public final class StringBuilder implements CharSequence,java.io.Serializable {
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
    public StringBuilder reverse(){
        for(int i=0,j=count-1;i<j;i++,j--){char ch=value[i];value[i]=value[j];value[j]=ch;}
        for(int i=0;i+1<count;i++)if(value[i]>=56320 && value[i]<=57343 && value[i+1]>=55296 && value[i+1]<=56319){char ch=value[i];value[i]=value[i+1];value[++i]=ch;}return this;
    }
    public String substring(int start){return substring(start,count);}public String substring(int start,int end){if(start<0 || end>count || start>end)throw new StringIndexOutOfBoundsException();return new String(value,start,end-start);}
    public CharSequence subSequence(int start,int end){return substring(start,end);}
    public void getChars(int start,int end,char[] target,int offset){if(start<0 || end>count || start>end)throw new StringIndexOutOfBoundsException();System.arraycopy(value,start,target,offset,end-start);}
    public int indexOf(String text){return toString().indexOf(text);}public int indexOf(String text,int from){return toString().indexOf(text,from);}
    public String toString(){return new String(value,0,count);}
}
