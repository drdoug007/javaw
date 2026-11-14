package java.io;

import java.util.Arrays;
import java.util.Objects;

/** Byte source base class for the single-threaded bytecode contract; subclasses supply read(). */
public abstract class InputStream implements Closeable {
    private static final int DEFAULT_BUFFER_SIZE = 16384;
    private static final int MAX_BUFFER_SIZE = Integer.MAX_VALUE - 8;
    public InputStream() {}
    static void checkFromIndexSize(int off, int len, int length) {
        if(off < 0 || len < 0 || len > length - off)
            throw new IndexOutOfBoundsException("Range ["+off+", "+off+" + "+len+") out of bounds for length "+length);
    }
    public static InputStream nullInputStream() {
        return new InputStream() {
            private boolean closed;
            private void ensureOpen() throws IOException {if(closed)throw new IOException("Stream closed");}
            public int available() throws IOException {ensureOpen();return 0;}
            public int read() throws IOException {ensureOpen();return -1;}
            public int read(byte[] b, int off, int len) throws IOException {
                checkFromIndexSize(off, len, b.length);
                if(len == 0)return 0;
                ensureOpen();
                return -1;
            }
            public byte[] readAllBytes() throws IOException {ensureOpen();return new byte[0];}
            public int readNBytes(byte[] b, int off, int len) throws IOException {
                checkFromIndexSize(off, len, b.length);
                ensureOpen();
                return 0;
            }
            public byte[] readNBytes(int len) throws IOException {
                if(len < 0)throw new IllegalArgumentException("len < 0");
                ensureOpen();
                return new byte[0];
            }
            public long skip(long n) throws IOException {ensureOpen();return 0L;}
            public void skipNBytes(long n) throws IOException {ensureOpen();if(n > 0)throw new EOFException();}
            public long transferTo(OutputStream out) throws IOException {Objects.requireNonNull(out);ensureOpen();return 0L;}
            public void close() throws IOException {closed = true;}
        };
    }
    public abstract int read() throws IOException;
    public int read(byte[] b) throws IOException {return read(b, 0, b.length);}
    public int read(byte[] b, int off, int len) throws IOException {
        checkFromIndexSize(off, len, b.length);
        if(len == 0)return 0;
        int c = read();
        if(c == -1)return -1;
        b[off] = (byte) c;
        int i = 1;
        try {
            for(; i < len; i++) {
                c = read();
                if(c == -1)break;
                b[off + i] = (byte) c;
            }
        } catch(IOException ee) {
        }
        return i;
    }
    public byte[] readAllBytes() throws IOException {return readNBytes(Integer.MAX_VALUE);}
    public byte[] readNBytes(int len) throws IOException {
        if(len < 0)throw new IllegalArgumentException("len < 0");
        // Same call sequence as JDK 21: fresh chunks of at most 16384 bytes, each filled until read
        // returns <= 0, so a full chunk (or len == 0) ends with a read(buf, nread, 0) call.
        byte[][] chunks = null;
        int chunkCount = 0;
        int total = 0;
        int remaining = len;
        int n;
        do {
            byte[] buf = new byte[Math.min(remaining, DEFAULT_BUFFER_SIZE)];
            int nread = 0;
            while((n = read(buf, nread, Math.min(buf.length - nread, remaining))) > 0) {
                nread += n;
                remaining -= n;
            }
            if(nread > 0) {
                if(MAX_BUFFER_SIZE - total < nread)throw new OutOfMemoryError("Required array size too large");
                if(nread < buf.length)buf = Arrays.copyOfRange(buf, 0, nread);
                total += nread;
                if(chunks == null) {
                    chunks = new byte[4][];
                } else if(chunkCount == chunks.length) {
                    byte[][] grown = new byte[chunkCount * 2][];
                    System.arraycopy(chunks, 0, grown, 0, chunkCount);
                    chunks = grown;
                }
                chunks[chunkCount++] = buf;
            }
        } while(n >= 0 && remaining > 0);
        if(chunkCount == 0)return new byte[0];
        if(chunkCount == 1)return chunks[0];
        byte[] result = new byte[total];
        int offset = 0;
        for(int i = 0; i < chunkCount; i++) {
            System.arraycopy(chunks[i], 0, result, offset, chunks[i].length);
            offset += chunks[i].length;
        }
        return result;
    }
    public int readNBytes(byte[] b, int off, int len) throws IOException {
        checkFromIndexSize(off, len, b.length);
        int n = 0;
        while(n < len) {
            int count = read(b, off + n, len - n);
            if(count < 0)break;
            n += count;
        }
        return n;
    }
    public long skip(long n) throws IOException {
        long remaining = n;
        if(n <= 0)return 0;
        int size = (int) Math.min(2048, remaining);
        byte[] skipBuffer = new byte[size];
        while(remaining > 0) {
            int nr = read(skipBuffer, 0, (int) Math.min(size, remaining));
            if(nr < 0)break;
            remaining -= nr;
        }
        return n - remaining;
    }
    public void skipNBytes(long n) throws IOException {
        while(n > 0) {
            long ns = skip(n);
            if(ns > 0 && ns <= n) {
                n -= ns;
            } else if(ns == 0) {
                if(read() == -1)throw new EOFException();
                n--;
            } else {
                throw new IOException("Unable to skip exactly");
            }
        }
    }
    public int available() throws IOException {return 0;}
    public void close() throws IOException {}
    public void mark(int readlimit) {}
    public void reset() throws IOException {throw new IOException("mark/reset not supported");}
    public boolean markSupported() {return false;}
    public long transferTo(OutputStream out) throws IOException {
        Objects.requireNonNull(out, "out");
        long transferred = 0;
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int n;
        while((n = read(buffer, 0, DEFAULT_BUFFER_SIZE)) >= 0) {
            out.write(buffer, 0, n);
            if(transferred < Long.MAX_VALUE) {
                transferred += n;
                if(transferred < 0)transferred = Long.MAX_VALUE;
            }
        }
        return transferred;
    }
}
