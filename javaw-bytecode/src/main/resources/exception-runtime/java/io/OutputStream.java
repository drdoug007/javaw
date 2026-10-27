package java.io;

/** Byte sink base class for the single-threaded bytecode contract; subclasses supply write(int). */
public abstract class OutputStream implements Closeable,Flushable {
    public OutputStream() {}
    public abstract void write(int value) throws IOException;
    public void write(byte[] bytes) throws IOException {write(bytes,0,bytes.length);}
    public void write(byte[] bytes,int offset,int length) throws IOException {
        if((bytes.length|offset|length)<0 || length>bytes.length-offset)
            throw new IndexOutOfBoundsException("Range ["+offset+", "+offset+" + "+length+") out of bounds for length "+bytes.length);
        for(int i=0;i<length;i++)write(bytes[offset+i]);
    }
    public void flush() throws IOException {}
    public void close() throws IOException {}
}
