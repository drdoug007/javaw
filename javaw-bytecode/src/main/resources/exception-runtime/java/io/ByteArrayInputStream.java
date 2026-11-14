package java.io;

import java.util.Arrays;

/** In-memory byte source; the caller's array is shared, and pos/count/mark stay protected for subclasses. */
public class ByteArrayInputStream extends InputStream {
    private static final int MAX_TRANSFER_SIZE = 128 * 1024;
    protected byte[] buf;
    protected int pos;
    protected int mark = 0;
    protected int count;
    public ByteArrayInputStream(byte[] buf) {
        this.buf = buf;
        this.pos = 0;
        this.count = buf.length;
    }
    public ByteArrayInputStream(byte[] buf, int offset, int length) {
        this.buf = buf;
        this.pos = offset;
        this.count = Math.min(offset + length, buf.length);
        this.mark = offset;
    }
    public int read() {return pos < count ? (buf[pos++] & 0xff) : -1;}
    public int read(byte[] b, int off, int len) {
        checkFromIndexSize(off, len, b.length);
        if(pos >= count)return -1;
        int avail = count - pos;
        if(len > avail)len = avail;
        if(len <= 0)return 0;
        System.arraycopy(buf, pos, b, off, len);
        pos += len;
        return len;
    }
    public byte[] readAllBytes() {
        byte[] result = Arrays.copyOfRange(buf, pos, count);
        pos = count;
        return result;
    }
    public int readNBytes(byte[] b, int off, int len) {
        int n = read(b, off, len);
        return n == -1 ? 0 : n;
    }
    public long transferTo(OutputStream out) throws IOException {
        int len = count - pos;
        int written = 0;
        while(written < len) {
            int chunk = Math.min(len - written, MAX_TRANSFER_SIZE);
            out.write(buf, pos, chunk);
            pos += chunk;
            written += chunk;
        }
        return len;
    }
    public long skip(long n) {
        long k = count - pos;
        if(n < k)k = n < 0 ? 0 : n;
        pos += (int) k;
        return k;
    }
    public int available() {return count - pos;}
    public boolean markSupported() {return true;}
    public void mark(int readAheadLimit) {mark = pos;}
    public void reset() {pos = mark;}
    public void close() throws IOException {}
}
