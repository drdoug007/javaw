package java.io;

import java.nio.charset.Charset;
import java.util.Arrays;

/** Growable in-memory byte sink; single-threaded, matches the JDK 21 semantics. */
public class ByteArrayOutputStream extends OutputStream {
    protected byte[] buf;
    protected int count;

    public ByteArrayOutputStream(){this(32);}
    public ByteArrayOutputStream(int size){
        if(size<0)throw new IllegalArgumentException("Negative initial size: "+size);
        buf=new byte[size];
    }
    public void write(int b){
        ensureCapacity(count+1);
        buf[count]=(byte)b;
        count++;
    }
    public void write(byte[] b,int off,int len){
        if((off|len)<0 || len>b.length-off)
            throw new IndexOutOfBoundsException("Range ["+off+", "+off+" + "+len+") out of bounds for length "+b.length);
        ensureCapacity(count+len);
        System.arraycopy(b,off,buf,count,len);
        count+=len;
    }
    public void writeBytes(byte[] b){write(b,0,b.length);}
    public void writeTo(OutputStream out) throws IOException {out.write(buf,0,count);}
    public void reset(){count=0;}
    public byte[] toByteArray(){return Arrays.copyOf(buf,count);}
    public int size(){return count;}
    public String toString(){return new String(buf,0,count);}
    public String toString(String charsetName) throws UnsupportedEncodingException {
        return new String(buf,0,count,charsetName);
    }
    public String toString(Charset charset){return new String(buf,0,count,charset);}
    @Deprecated public String toString(int hibyte){return new String(buf,hibyte,0,count);}
    public void close() throws IOException {}
    // Overflow-conscious, as in the JDK: count + len may have wrapped negative, so compare by subtraction.
    private void ensureCapacity(int minCapacity){
        int oldCapacity=buf.length;
        int minGrowth=minCapacity-oldCapacity;
        if(minGrowth>0)buf=Arrays.copyOf(buf,newLength(oldCapacity,minGrowth,oldCapacity));
    }
    private static final int SOFT_MAX_ARRAY_LENGTH=Integer.MAX_VALUE-8;
    // JDK 21 jdk.internal.util.ArraysSupport.newLength semantics.
    private static int newLength(int oldLength,int minGrowth,int prefGrowth){
        int prefLength=oldLength+Math.max(minGrowth,prefGrowth);
        if(0<prefLength && prefLength<=SOFT_MAX_ARRAY_LENGTH)return prefLength;
        int minLength=oldLength+minGrowth;
        if(minLength<0)
            throw new OutOfMemoryError("Required array length "+oldLength+" + "+minGrowth+" is too large");
        return minLength<=SOFT_MAX_ARRAY_LENGTH?SOFT_MAX_ARRAY_LENGTH:minLength;
    }
}
