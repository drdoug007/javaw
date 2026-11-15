package java.io;

import java.nio.charset.Charset;

/** Character output over an OutputStream; encodes the runtime's six charsets with an internal byte buffer. */
public class OutputStreamWriter extends Writer {
    static final String[] HISTORICAL = {"UTF8", "ISO8859_1", "ASCII", "UnicodeBigUnmarked", "UnicodeLittleUnmarked", "UTF-16"};
    static final int[] MAX_BYTES = {3, 1, 1, 2, 2, 4};
    final OutputStream out;
    final int charset;
    final byte[] bytes = new byte[8192];
    final byte[] unit = new byte[4];
    int capacity = 512;
    int count;
    boolean closed;
    boolean needsMark;
    boolean haveLeftover;
    char leftover;

    public OutputStreamWriter(OutputStream out, String charsetName) throws UnsupportedEncodingException {
        super(out);
        if (charsetName == null) throw new NullPointerException("charsetName");
        this.out = out;
        this.charset = javaw.runtime.TextCharsets.lookup(charsetName);
        this.needsMark = charset == 5;
    }

    public OutputStreamWriter(OutputStream out) {
        this(out, Charset.defaultCharset());
    }

    public OutputStreamWriter(OutputStream out, Charset cs) {
        super(out);
        if (cs == null) throw new NullPointerException("charset");
        this.out = out;
        this.charset = javaw.runtime.TextCharsets.identifier(cs);
        this.needsMark = charset == 5;
    }

    public String getEncoding() {
        return closed ? null : HISTORICAL[charset];
    }

    public void write(int c) throws IOException {
        writeChars(new char[] {(char) c}, 0, 1);
    }

    public void write(char[] cbuf, int off, int len) throws IOException {
        writeChars(cbuf, off, len);
    }

    public void write(String str, int off, int len) throws IOException {
        writeString(str, off, len);
    }

    public Writer append(CharSequence csq) throws IOException {
        String s = String.valueOf(csq);
        writeString(s, 0, s.length());
        return this;
    }

    public void flush() throws IOException {
        ensureOpen();
        flushBytes();
        out.flush();
    }

    public void close() throws IOException {
        if (closed) return;
        try {
            if (haveLeftover) { haveLeftover = false; put(-1); }
            flushBytes();
            try { out.flush(); } finally { out.close(); }
        } finally {
            closed = true;
        }
    }

    private void ensureOpen() throws IOException {
        if (closed) throw new IOException("Stream closed");
    }

    private void writeString(String str, int off, int len) throws IOException {
        if (len < 0) throw new IndexOutOfBoundsException();
        char[] cbuf = new char[len];
        str.getChars(off, off + len, cbuf, 0);
        writeChars(cbuf, 0, len);
    }

    private void writeChars(char[] cbuf, int off, int len) throws IOException {
        ensureOpen();
        if ((off < 0) || (off > cbuf.length) || (len < 0) || ((off + len) > cbuf.length) || ((off + len) < 0))
            throw new IndexOutOfBoundsException();
        if (len == 0) return;
        int end = off + len;
        // The pending surrogate is cleared only once it and the next char are buffered, so if out.write
        // fails it is still pending and close() replaces it, as in the JDK.
        while (haveLeftover && off < end) {
            char c = cbuf[off++];
            if (Character.isLowSurrogate(c)) {
                put(Character.toCodePoint(leftover, c));
                haveLeftover = false;
            } else {
                put(-1);
                if (Character.isHighSurrogate(c)) {
                    if (off == end) overflowBeforeLeftover();
                    leftover = c;
                } else {
                    put(c);
                    haveLeftover = false;
                }
            }
        }
        grow(end - off);
        if (needsMark) { needsMark = false; put(0xFEFF); }
        for (int i = off; i < end; i++) {
            char c = cbuf[i];
            if (Character.isHighSurrogate(c)) {
                if (i + 1 < end && Character.isLowSurrogate(cbuf[i + 1])) {
                    put(Character.toCodePoint(c, cbuf[i + 1]));
                    i++;
                } else if (i + 1 == end) {
                    overflowBeforeLeftover();
                    haveLeftover = true;
                    leftover = c;
                } else {
                    put(-1);
                }
            } else if (Character.isLowSurrogate(c)) {
                put(-1);
            } else {
                put(c);
            }
        }
    }

    // The JDK's ISO-8859-1 encoder reports overflow on a full buffer before it looks at a trailing high
    // surrogate, so the full buffer is written out before that surrogate is kept pending.
    private void overflowBeforeLeftover() throws IOException {
        if (charset == 1 && count == capacity) flushBytes();
    }

    private void grow(int len) throws IOException {
        if (capacity < 8192) {
            int n = Math.min(len * MAX_BYTES[charset], 8192);
            if (n > capacity) {
                flushBytes();
                capacity = n;
            }
        }
    }

    private void put(int c) throws IOException {
        int n;
        if (charset == 0) {
            if (c == -1) {
                unit[0] = (byte) 0x3F;
                n = 1;
            } else if (c < 0x80) {
                unit[0] = (byte) c;
                n = 1;
            } else if (c < 0x800) {
                unit[0] = (byte) (0xC0 | (c >> 6));
                unit[1] = (byte) (0x80 | (c & 0x3F));
                n = 2;
            } else if (c < 0x10000) {
                unit[0] = (byte) (0xE0 | (c >> 12));
                unit[1] = (byte) (0x80 | ((c >> 6) & 0x3F));
                unit[2] = (byte) (0x80 | (c & 0x3F));
                n = 3;
            } else {
                unit[0] = (byte) (0xF0 | (c >> 18));
                unit[1] = (byte) (0x80 | ((c >> 12) & 0x3F));
                unit[2] = (byte) (0x80 | ((c >> 6) & 0x3F));
                unit[3] = (byte) (0x80 | (c & 0x3F));
                n = 4;
            }
        } else if (charset == 1 || charset == 2) {
            int limit = charset == 1 ? 0xFF : 0x7F;
            unit[0] = c >= 0 && c <= limit ? (byte) c : (byte) 0x3F;
            n = 1;
        } else {
            if (c == -1) c = 0xFFFD;
            if (c < 0x10000) {
                putUnit(c, 0);
                n = 2;
            } else {
                putUnit(Character.highSurrogate(c), 0);
                putUnit(Character.lowSurrogate(c), 2);
                n = 4;
            }
        }
        if (capacity - count < n) flushBytes();
        System.arraycopy(unit, 0, bytes, count, n);
        count += n;
    }

    private void putUnit(int unit, int at) {
        if (charset == 4) {
            this.unit[at] = (byte) unit;
            this.unit[at + 1] = (byte) (unit >> 8);
        } else {
            this.unit[at] = (byte) (unit >> 8);
            this.unit[at + 1] = (byte) unit;
        }
    }

    private void flushBytes() throws IOException {
        if (count > 0) {
            int n = count;
            count = 0;
            out.write(bytes, 0, n);
        }
    }
}
