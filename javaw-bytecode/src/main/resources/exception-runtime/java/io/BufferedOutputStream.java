package java.io;

import java.util.Arrays;

/** Buffered output over the protected `out` stream, exactly like java.io.BufferedOutputStream. */
public class BufferedOutputStream extends FilterOutputStream {
    private static final int DEFAULT_MAX_BUFFER_SIZE = 8192;
    protected byte[] buf;
    protected int count;
    private final int maxBufSize;

    public BufferedOutputStream(OutputStream out) {this(out, DEFAULT_MAX_BUFFER_SIZE);}
    public BufferedOutputStream(OutputStream out,int size) {
        super(out);
        if(size <= 0)throw new IllegalArgumentException("Buffer size <= 0");
        buf = new byte[size];
        maxBufSize = size;
    }
    private void flushBuffer() throws IOException {
        if(count > 0){
            out.write(buf, 0, count);
            count = 0;
        }
    }
    private void growIfNeeded(int len) {
        int neededSize = count + len + 1;
        if(neededSize < 0)neededSize = Integer.MAX_VALUE;
        int bufSize = buf.length;
        if(neededSize > bufSize && bufSize < maxBufSize)
            buf = Arrays.copyOf(buf, Math.min(neededSize, maxBufSize));
    }
    public void write(int b) throws IOException {
        growIfNeeded(1);
        if(count >= buf.length)flushBuffer();
        buf[count++] = (byte) b;
    }
    public void write(byte[] b,int off,int len) throws IOException {
        if(len >= maxBufSize){
            flushBuffer();
            out.write(b, off, len);
            return;
        }
        growIfNeeded(len);
        if(len > buf.length - count)flushBuffer();
        System.arraycopy(b, off, buf, count, len);
        count += len;
    }
    public void flush() throws IOException {
        flushBuffer();
        out.flush();
    }
}
