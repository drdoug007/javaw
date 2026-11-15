package java.io;

import java.util.Arrays;
import java.util.Objects;

/** Buffered input over the protected `in` stream, keeping marks, exactly like java.io.BufferedInputStream. */
public class BufferedInputStream extends FilterInputStream {
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    private final int initialSize;
    protected byte[] buf;
    protected int count;
    protected int pos;
    protected int markpos = -1;
    protected int marklimit;

    public BufferedInputStream(InputStream in) {this(in, DEFAULT_BUFFER_SIZE);}
    public BufferedInputStream(InputStream in,int size) {
        super(in);
        if(size <= 0)throw new IllegalArgumentException("Buffer size <= 0");
        initialSize = size;
        buf = new byte[size];
    }
    private InputStream getInIfOpen() throws IOException {
        InputStream input = in;
        if(input == null)throw new IOException("Stream closed");
        return input;
    }
    private byte[] getBufIfOpen() throws IOException {
        byte[] buffer = buf;
        if(buffer == null)throw new IOException("Stream closed");
        return buffer;
    }
    private void ensureOpen() throws IOException {
        if(buf == null)throw new IOException("Stream closed");
    }
    private void fill() throws IOException {
        byte[] buffer = getBufIfOpen();
        if(markpos == -1)
            pos = 0;
        else if(pos >= buffer.length)
            if(markpos > 0){
                int sz = pos - markpos;
                System.arraycopy(buffer, markpos, buffer, 0, sz);
                pos = sz;
                markpos = 0;
            } else if(buffer.length >= marklimit){
                markpos = -1;
                pos = 0;
            } else {
                int nsz = pos + Math.max(1, pos);
                if(nsz < 0 || nsz > Integer.MAX_VALUE - 8)nsz = Integer.MAX_VALUE - 8;
                if(nsz > marklimit)nsz = marklimit;
                byte[] grown = new byte[nsz];
                System.arraycopy(buffer, 0, grown, 0, pos);
                buf = grown;
                buffer = grown;
            }
        count = pos;
        int n = getInIfOpen().read(buffer, pos, buffer.length - pos);
        if(n > 0)count = n + pos;
    }
    public int read() throws IOException {
        if(pos >= count){
            fill();
            if(pos >= count)return -1;
        }
        return getBufIfOpen()[pos++] & 0xff;
    }
    private int read1(byte[] b,int off,int len) throws IOException {
        int avail = count - pos;
        if(avail <= 0){
            int size = Math.max(getBufIfOpen().length, initialSize);
            if(len >= size && markpos == -1)
                return getInIfOpen().read(b, off, len);
            fill();
            avail = count - pos;
            if(avail <= 0)return -1;
        }
        int cnt = (avail < len) ? avail : len;
        System.arraycopy(getBufIfOpen(), pos, b, off, cnt);
        pos += cnt;
        return cnt;
    }
    public int read(byte[] b,int off,int len) throws IOException {
        ensureOpen();
        if((off | len | (off + len) | (b.length - (off + len))) < 0)
            throw new IndexOutOfBoundsException();
        if(len == 0)return 0;
        int n = 0;
        for(;;){
            int nread = read1(b, off + n, len - n);
            if(nread <= 0)
                return (n == 0) ? nread : n;
            n += nread;
            if(n >= len)return n;
            InputStream input = in;
            if(input != null && input.available() <= 0)
                return n;
        }
    }
    public long skip(long n) throws IOException {
        ensureOpen();
        if(n <= 0)return 0;
        long avail = count - pos;
        if(avail <= 0){
            if(markpos == -1)
                return getInIfOpen().skip(n);
            fill();
            avail = count - pos;
            if(avail <= 0)return 0;
        }
        long skipped = (avail < n) ? avail : n;
        pos += (int) skipped;
        return skipped;
    }
    public int available() throws IOException {
        int n = count - pos;
        int avail = getInIfOpen().available();
        return n > (Integer.MAX_VALUE - avail) ? Integer.MAX_VALUE : n + avail;
    }
    public void mark(int readlimit) {
        marklimit = readlimit;
        markpos = pos;
    }
    public void reset() throws IOException {
        ensureOpen();
        if(markpos < 0)throw new IOException("Resetting to invalid mark");
        pos = markpos;
    }
    public boolean markSupported() {return true;}
    public void close() throws IOException {
        if(buf != null){
            InputStream input = in;
            buf = null;
            in = null;
            if(input != null)input.close();
        }
    }
    public long transferTo(OutputStream out) throws IOException {
        Objects.requireNonNull(out, "out");
        if(getClass() == BufferedInputStream.class && markpos == -1){
            int avail = count - pos;
            if(avail > 0){
                out.write(Arrays.copyOfRange(getBufIfOpen(), pos, count));
                pos = count;
            }
            try{
                return Math.addExact(avail, getInIfOpen().transferTo(out));
            }catch(ArithmeticException ignore){
                return Long.MAX_VALUE;
            }
        }
        return super.transferTo(out);
    }
}
