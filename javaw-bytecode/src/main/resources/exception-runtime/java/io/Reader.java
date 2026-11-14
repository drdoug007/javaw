package java.io;

import java.util.Objects;

/** Character source base class. The lock is retained for subclasses; the single-threaded contract does not enter monitors. */
public abstract class Reader implements Closeable {
    protected Object lock;
    protected Reader() {lock=this;}
    protected Reader(Object lock) {if(lock==null)throw new NullPointerException();this.lock=lock;}
    public int read() throws IOException {
        char[] cb=new char[1];if(read(cb,0,1)==-1)return -1;return cb[0];
    }
    public int read(char[] cbuf) throws IOException {return read(cbuf,0,cbuf.length);}
    public abstract int read(char[] cbuf,int off,int len) throws IOException;
    public long skip(long n) throws IOException {
        if(n<0)throw new IllegalArgumentException("skip value is negative");
        int nn=(int)Math.min(n,8192);char[] buf=new char[nn];long r=n;
        while(r>0){int nc=read(buf,0,(int)Math.min(r,nn));if(nc==-1)break;r-=nc;}
        return n-r;
    }
    public boolean ready() throws IOException {return false;}
    public boolean markSupported() {return false;}
    public void mark(int readAheadLimit) throws IOException {throw new IOException("mark() not supported");}
    public void reset() throws IOException {throw new IOException("reset() not supported");}
    public abstract void close() throws IOException;
    public long transferTo(Writer out) throws IOException {
        Objects.requireNonNull(out,"out");long total=0;char[] buffer=new char[8192];int n;
        while((n=read(buffer,0,8192))>=0){out.write(buffer,0,n);total+=n;}
        return total;
    }
    public static Reader nullReader() {
        return new Reader() {
            boolean closed;
            private void ensureOpen() throws IOException {if(closed)throw new IOException("Stream closed");}
            public int read() throws IOException {ensureOpen();return -1;}
            public int read(char[] cbuf,int off,int len) throws IOException {
                checkFromIndexSize(off,len,cbuf.length);ensureOpen();return len==0?0:-1;
            }
            public boolean ready() throws IOException {ensureOpen();return false;}
            public long skip(long n) throws IOException {ensureOpen();return 0;}
            public long transferTo(Writer out) throws IOException {Objects.requireNonNull(out);ensureOpen();return 0;}
            public void close() {closed=true;}
        };
    }
    static void checkFromIndexSize(int off,int len,int length){
        if((length|off|len)<0 || len>length-off)
            throw new IndexOutOfBoundsException("Range ["+off+", "+off+" + "+len+") out of bounds for length "+length);
    }
}
