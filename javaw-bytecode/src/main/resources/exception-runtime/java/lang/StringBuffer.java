package java.lang;

/** Sequential text contract only; monitor/thread support is a separate milestone. */
public final class StringBuffer implements CharSequence,Comparable<StringBuffer>,java.io.Serializable {
    private final StringBuilder builder;
    public StringBuffer(){builder=new StringBuilder();}
    public StringBuffer(int capacity){builder=new StringBuilder(capacity);}
    public StringBuffer(String text){builder=new StringBuilder(text);}
    public StringBuffer(CharSequence text){builder=new StringBuilder(text);}
    public int length(){return builder.length();}
    public int capacity(){return builder.capacity();}
    public boolean isEmpty(){return builder.isEmpty();}
    public void ensureCapacity(int minimum){builder.ensureCapacity(minimum);}
    public void trimToSize(){builder.trimToSize();}
    public char charAt(int index){return builder.charAt(index);}
    public void setCharAt(int index,char ch){builder.setCharAt(index,ch);}
    public void setLength(int length){builder.setLength(length);}
    public String substring(int start){return builder.substring(start);}
    public String substring(int start,int end){return builder.substring(start,end);}
    public CharSequence subSequence(int start,int end){return builder.subSequence(start,end);}
    public void getChars(int start,int end,char[] target,int offset){builder.getChars(start,end,target,offset);}
    public int indexOf(String text){return builder.indexOf(text);}
    public int indexOf(String text,int from){return builder.indexOf(text,from);}
    public int lastIndexOf(String text){return builder.lastIndexOf(text);}
    public int lastIndexOf(String text,int from){return builder.lastIndexOf(text,from);}
    public int codePointAt(int index){return builder.codePointAt(index);}
    public int codePointBefore(int index){return builder.codePointBefore(index);}
    public int codePointCount(int begin,int end){return builder.codePointCount(begin,end);}
    public int offsetByCodePoints(int index,int offset){return builder.offsetByCodePoints(index,offset);}
    public String toString(){return builder.toString();}
    public StringBuffer append(Object text){builder.append(text);return this;}
    public StringBuffer append(String text){builder.append(text);return this;}
    public StringBuffer append(StringBuffer text){builder.append(text);return this;}
    public StringBuffer append(CharSequence text){builder.append(text);return this;}
    public StringBuffer append(char[] text){builder.append(text);return this;}
    public StringBuffer append(char text){builder.append(text);return this;}
    public StringBuffer append(boolean text){builder.append(text);return this;}
    public StringBuffer append(int text){builder.append(text);return this;}
    public StringBuffer append(long text){builder.append(text);return this;}
    public StringBuffer append(float text){builder.append(text);return this;}
    public StringBuffer append(double text){builder.append(text);return this;}
    public StringBuffer append(CharSequence text,int start,int end){builder.append(text,start,end);return this;}
    public StringBuffer append(char[] text,int offset,int length){builder.append(text,offset,length);return this;}
    public StringBuffer appendCodePoint(int code){builder.appendCodePoint(code);return this;}
    public StringBuffer insert(int offset,String text){builder.insert(offset,text);return this;}
    public StringBuffer insert(int offset,char ch){builder.insert(offset,ch);return this;}
    public StringBuffer delete(int start,int end){builder.delete(start,end);return this;}
    public StringBuffer deleteCharAt(int index){builder.deleteCharAt(index);return this;}
    public StringBuffer replace(int start,int end,String text){builder.replace(start,end,text);return this;}
    public StringBuffer reverse(){builder.reverse();return this;}
    public int compareTo(StringBuffer other){return builder.compareTo(other.builder);}
    public StringBuffer insert(int offset,Object text){builder.insert(offset,String.valueOf(text));return this;}
    public StringBuffer insert(int offset,boolean text){builder.insert(offset,String.valueOf(text));return this;}
    public StringBuffer insert(int offset,int text){builder.insert(offset,String.valueOf(text));return this;}
    public StringBuffer insert(int offset,long text){builder.insert(offset,String.valueOf(text));return this;}
    public StringBuffer insert(int offset,float text){builder.insert(offset,String.valueOf(text));return this;}
    public StringBuffer insert(int offset,double text){builder.insert(offset,String.valueOf(text));return this;}
    public StringBuffer insert(int offset,char[] text){builder.insert(offset,text);return this;}
    public StringBuffer insert(int offset,char[] text,int start,int size){builder.insert(offset,text,start,size);return this;}
    public StringBuffer insert(int offset,CharSequence text){builder.insert(offset,text);return this;}
}
