package java.io;

import java.nio.charset.Charset;

/**
 * Byte-to-character bridge for UTF-8, ISO-8859-1 and US-ASCII. Decodes while the bytes
 * arrive, so a multi-byte sequence may be split over several reads of the underlying
 * stream. Holds an 8192-byte buffer that is compacted on every fill; a supplementary
 * character decodes to a surrogate pair, the low half of which a single-char read keeps
 * as the leftover.
 */
public class InputStreamReader extends Reader {
    private final InputStream in;
    private final int charset;
    private boolean closed;
    private boolean haveLeftoverChar;
    private char leftoverChar;
    private final byte[] bytes = new byte[8192];
    private int bytePos;
    private int byteLimit;
    private int charPos;

    public InputStreamReader(InputStream in) {
        super(in);
        this.in = in;
        this.charset = 0;
    }

    public InputStreamReader(InputStream in, String charsetName) throws UnsupportedEncodingException {
        super(in);
        if (charsetName == null)throw new NullPointerException("charsetName");
        this.in = in;
        this.charset = checked(javaw.runtime.TextCharsets.lookup(charsetName));
    }

    public InputStreamReader(InputStream in, Charset cs) {
        super(in);
        if (cs == null)throw new NullPointerException("charset");
        this.in = in;
        this.charset = checked(javaw.runtime.TextCharsets.identifier(cs));
    }

    private static int checked(int id) {
        if (id >= 3)throw new UnsupportedOperationException("InputStreamReader decodes only UTF-8, ISO-8859-1 and US-ASCII");
        return id;
    }

    public String getEncoding() {
        if (closed)return null;
        if (charset == 0)return "UTF8";
        if (charset == 1)return "ISO8859_1";
        return "ASCII";
    }

    private void ensureOpen() throws IOException {
        if (closed)throw new IOException("Stream closed");
    }

    public int read() throws IOException {
        return read0();
    }

    public int read(char[] cbuf, int off, int len) throws IOException {
        return readChars(cbuf, off, len);
    }

    private int read0() throws IOException {
        if (haveLeftoverChar) {haveLeftoverChar = false;return leftoverChar;}
        char[] cb = new char[2];
        int n = readChars(cb, 0, 2);
        if (n == -1)return -1;
        if (n == 2) {haveLeftoverChar = true;leftoverChar = cb[1];}
        return cb[0];
    }

    private int readChars(char[] cbuf, int off, int len) throws IOException {
        ensureOpen();
        if (off < 0 || off > cbuf.length || len < 0 || off + len > cbuf.length || off + len < 0)
            throw new IndexOutOfBoundsException();
        if (len == 0)return 0;
        int n = 0;
        if (haveLeftoverChar) {
            cbuf[off] = leftoverChar;off++;len--;haveLeftoverChar = false;n = 1;
            if (len == 0 || !implReady())return n;
        }
        if (len == 1) {
            int c = read0();
            if (c == -1)return n == 0 ? -1 : n;
            cbuf[off] = (char) c;
            return n + 1;
        }
        int nr = implRead(cbuf, off, off + len);
        return nr < 0 ? (n == 1 ? 1 : nr) : n + nr;
    }

    public boolean ready() throws IOException {
        ensureOpen();
        return haveLeftoverChar || implReady();
    }

    private boolean implReady() {
        return bytePos < byteLimit || inReady();
    }

    private boolean inReady() {
        try {return in.available() > 0;} catch (IOException x) {return false;}
    }

    public void close() throws IOException {
        if (closed)return;
        try {in.close();} finally {closed = true;}
    }

    private int readBytes() throws IOException {
        int rem = byteLimit - bytePos;
        System.arraycopy(bytes, bytePos, bytes, 0, rem);
        bytePos = 0;
        byteLimit = rem;
        int n = in.read(bytes, byteLimit, bytes.length - byteLimit);
        if (n < 0)return n;
        if (n == 0)throw new IOException("Underlying input stream returned zero bytes");
        byteLimit += n;
        return byteLimit - bytePos;
    }

    private int implRead(char[] cbuf, int off, int end) throws IOException {
        charPos = off;
        boolean eof = false;
        for (;;) {
            if (!decode(cbuf, end, eof))break;
            if (eof)break;
            if (charPos >= end)break;
            if (charPos > off && !inReady())break;
            int n = readBytes();
            if (n < 0) {
                eof = true;
                if (charPos == off && bytePos >= byteLimit)break;
            }
        }
        return charPos == off ? -1 : charPos - off;
    }

    private boolean decode(char[] cbuf, int end, boolean eof) {
        for (;;) {
            int r = decodeLoop(cbuf, end);
            if (r < 0)return false;
            if (r == 0) {
                if (!eof || bytePos >= byteLimit)return true;
                r = byteLimit - bytePos;
            }
            if (charPos >= end)return false;
            cbuf[charPos++] = (char) 0xfffd;
            bytePos += r;
        }
    }

    private int decodeLoop(char[] cbuf, int end) {
        for (;;) {
            int rem = byteLimit - bytePos;
            if (rem <= 0)return 0;
            int b1 = bytes[bytePos] & 0xff;
            int b2 = rem > 1 ? bytes[bytePos + 1] & 0xff : 0;
            int b3 = rem > 2 ? bytes[bytePos + 2] & 0xff : 0;
            if (b1 < 0x80 || charset == 1) {
                if (charPos >= end)return -1;
                cbuf[charPos++] = (char) b1;
                bytePos++;
            } else if (charset == 2) {
                return 1;
            } else if (b1 >= 0xc2 && b1 <= 0xdf) {
                if (rem < 2)return 0;
                if (charPos >= end)return -1;
                if (!cont(b2))return 1;
                cbuf[charPos++] = (char) (((b1 & 0x1f) << 6) | (b2 & 0x3f));
                bytePos += 2;
            } else if (b1 >= 0xe0 && b1 <= 0xef) {
                if (rem > 1 && bad3(b1, b2))return 1;
                if (rem < 3)return 0;
                if (charPos >= end)return -1;
                if (!cont(b3))return 2;
                char c = (char) (((b1 & 0x0f) << 12) | ((b2 & 0x3f) << 6) | (b3 & 0x3f));
                if (Character.isSurrogate(c))return 3;
                cbuf[charPos++] = c;
                bytePos += 3;
            } else if (b1 >= 0xf0 && b1 <= 0xf7) {
                if (b1 > 0xf4 || (rem > 1 && bad4(b1, b2)))return 1;
                if (rem > 2 && !cont(b3))return 2;
                if (rem < 4)return 0;
                if (end - charPos < 2)return -1;
                int b4 = bytes[bytePos + 3] & 0xff;
                if (!cont(b4))return 3;
                int cp = ((b1 & 0x07) << 18) | ((b2 & 0x3f) << 12) | ((b3 & 0x3f) << 6) | (b4 & 0x3f);
                cbuf[charPos++] = Character.highSurrogate(cp);
                cbuf[charPos++] = Character.lowSurrogate(cp);
                bytePos += 4;
            } else {
                return 1;
            }
        }
    }

    private static boolean cont(int b) {
        return (b & 0xc0) == 0x80;
    }

    private static boolean bad3(int b1, int b2) {
        return (b1 == 0xe0 && (b2 & 0xe0) == 0x80) || !cont(b2);
    }

    private static boolean bad4(int b1, int b2) {
        return b1 > 0xf4 || (b1 == 0xf0 && (b2 < 0x90 || b2 > 0xbf)) || (b1 == 0xf4 && (b2 & 0xf0) != 0x80) || !cont(b2);
    }
}
