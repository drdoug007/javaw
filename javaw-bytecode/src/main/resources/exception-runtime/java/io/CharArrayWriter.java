package java.io;

import java.util.Arrays;

/** Growable in-memory char sink; single-threaded, matches JDK 21 semantics. */
public class CharArrayWriter extends Writer {
    protected char[] buf;
    protected int count;
    public CharArrayWriter(){this(32);}
    public CharArrayWriter(int initialSize){
        if(initialSize<0)throw new IllegalArgumentException("Negative initial size: "+initialSize);
        buf=new char[initialSize];
    }
    public void write(int c){
        int newcount=count+1;
        if(newcount>buf.length)buf=Arrays.copyOf(buf,Math.max(buf.length<<1,newcount));
        buf[count]=(char)c;
        count=newcount;
    }
    public void write(char[] c,int off,int len){
        int length=c.length;
        if((off|len)<0 || len>length-off)
            throw new IndexOutOfBoundsException("Range ["+off+", "+off+" + "+len+") out of bounds for length "+length);
        if(len==0)return;
        int newcount=count+len;
        if(newcount>buf.length)buf=Arrays.copyOf(buf,Math.max(buf.length<<1,newcount));
        System.arraycopy(c,off,buf,count,len);
        count=newcount;
    }
    public void write(String str,int off,int len){
        int newcount=count+len;
        if(newcount>buf.length)buf=Arrays.copyOf(buf,Math.max(buf.length<<1,newcount));
        str.getChars(off,off+len,buf,count);
        count=newcount;
    }
    public void writeTo(Writer out) throws IOException {out.write(buf,0,count);}
    public CharArrayWriter append(CharSequence csq){
        String s=String.valueOf(csq);
        write(s,0,s.length());
        return this;
    }
    public CharArrayWriter append(CharSequence csq,int start,int end){
        if(csq==null)csq="null";
        return append(csq.subSequence(start,end));
    }
    public CharArrayWriter append(char c){write(c);return this;}
    public void reset(){count=0;}
    public char[] toCharArray(){return Arrays.copyOf(buf,count);}
    public int size(){return count;}
    public String toString(){return new String(buf,0,count);}
    public void flush(){}
    public void close(){}
}
