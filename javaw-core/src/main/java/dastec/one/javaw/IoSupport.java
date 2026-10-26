package dastec.one.javaw;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

/** Host-independent byte and character streams. Files and sockets require separate host adapters. */
final class IoSupport {
    static void addTypes(CompilationUnit cu) {
        if (cu.findAll(ClassOrInterfaceType.class).stream().noneMatch(t -> java.util.Set.of(
                "InputStream", "OutputStream", "ByteArrayInputStream", "ByteArrayOutputStream", "IOException",
                "Closeable", "Flushable", "Reader", "Writer", "StringReader", "StringWriter",
                "CharArrayReader", "CharArrayWriter", "BufferedReader", "BufferedWriter", "Appendable").contains(t.getNameAsString()))) return;
        LibrarySupport.add(cu, "java.lang", """
            interface Appendable {
                public Appendable append(CharSequence csq) throws Exception;
                public Appendable append(CharSequence csq, int start, int end) throws Exception;
                public Appendable append(char c) throws Exception;
            }
            """);
        LibrarySupport.add(cu, "java.io", """
            class IOException extends Exception {
                public IOException() { super(); }
                public IOException(String message) { super(message); }
            }
            interface Closeable extends AutoCloseable {
                public void close() throws IOException;
            }
            interface Flushable {
                public void flush() throws IOException;
            }
            abstract class InputStream implements Closeable {
                public abstract int read() throws IOException;
                public int read(byte[] bytes) throws IOException { if(bytes==null) throw new NullPointerException(); return read(bytes,0,bytes.length); }
                public int read(byte[] bytes,int offset,int length) throws IOException {
                    if(bytes==null) throw new NullPointerException();
                    if(offset<0 || length<0 || offset>bytes.length-length) throw new IndexOutOfBoundsException();
                    if(length==0) return 0;
                    int value=read(); if(value<0) return -1; bytes[offset]=(byte)value;
                    int count=1; while(count<length) { value=read(); if(value<0) break; bytes[offset+count]=(byte)value; count++; }
                    return count;
                }
                public long skip(long count) throws IOException { long skipped=0; while(skipped<count && read()>=0) skipped++; return skipped; }
                public int available() throws IOException { return 0; }
                public boolean markSupported() { return false; }
                public void mark(int limit) {}
                public void reset() throws IOException { throw new IOException("mark/reset not supported"); }
                public void close() throws IOException {}
            }
            abstract class OutputStream implements Closeable, Flushable {
                public abstract void write(int value) throws IOException;
                public void write(byte[] bytes) throws IOException { if(bytes==null) throw new NullPointerException(); write(bytes,0,bytes.length); }
                public void write(byte[] bytes,int offset,int length) throws IOException {
                    if(bytes==null) throw new NullPointerException();
                    if(offset<0 || length<0 || offset>bytes.length-length) throw new IndexOutOfBoundsException();
                    for(int i=0;i<length;i++) write(bytes[offset+i]);
                }
                public void flush() throws IOException {}
                public void close() throws IOException {}
            }
            class ByteArrayInputStream extends InputStream {
                private byte[] buffer; private int position; private int limit; private int marked;
                public ByteArrayInputStream(byte[] bytes) { this(bytes,0,__javaw_input_length(bytes)); }
                private static int __javaw_input_length(byte[] bytes) { if(bytes==null) throw new NullPointerException(); return bytes.length; }
                public ByteArrayInputStream(byte[] bytes,int offset,int length) {
                    if(bytes==null) throw new NullPointerException();
                    if(offset<0 || length<0 || offset>bytes.length) throw new IndexOutOfBoundsException();
                    buffer=bytes; position=offset; marked=offset;
                    limit=length>bytes.length-offset ? bytes.length : offset+length;
                }
                public int read() { if(position>=limit) return -1; return buffer[position++] & 255; }
                public int read(byte[] bytes,int offset,int length) {
                    if(bytes==null) throw new NullPointerException();
                    if(offset<0 || length<0 || offset>bytes.length-length) throw new IndexOutOfBoundsException();
                    if(position>=limit) return -1;
                    int count=length<limit-position ? length : limit-position;
                    for(int i=0;i<count;i++) bytes[offset+i]=buffer[position+i]; position+=count; return count;
                }
                public long skip(long count) {
                    if(count<=0) return 0L; int remaining=limit-position;
                    int skipped=count>remaining ? remaining : (int)count; position+=skipped; return (long)skipped;
                }
                public int available() { return limit-position; }
                public boolean markSupported() { return true; }
                public void mark(int readLimit) { marked=position; }
                public void reset() { position=marked; }
            }
            class ByteArrayOutputStream extends OutputStream {
                private byte[] buffer; private int count;
                public ByteArrayOutputStream() { this(32); }
                public ByteArrayOutputStream(int size) {
                    if(size<0) throw new IllegalArgumentException(); buffer=new byte[size];
                }
                public void write(int value) {
                    if(count==buffer.length) {
                        int capacity=buffer.length==0 ? 1 : buffer.length*2;
                        byte[] grown=new byte[capacity]; for(int i=0;i<count;i++) grown[i]=buffer[i]; buffer=grown;
                    }
                    buffer[count++]=(byte)value;
                }
                public int size() { return count; }
                public void reset() { count=0; }
                public byte[] toByteArray() { byte[] copy=new byte[count]; for(int i=0;i<count;i++) copy[i]=buffer[i]; return copy; }
                public void writeTo(OutputStream target) throws IOException { target.write(buffer,0,count); }
            }
            abstract class Reader implements Closeable {
                public int read() throws IOException {
                    char[] cb = new char[1];
                    if (read(cb, 0, 1) == -1) return -1;
                    return cb[0];
                }
                public int read(char[] cbuf) throws IOException {
                    if (cbuf == null) throw new NullPointerException();
                    return read(cbuf, 0, cbuf.length);
                }
                public abstract int read(char[] cbuf, int offset, int length) throws IOException;
                public long skip(long count) throws IOException {
                    if (count < 0L) throw new IllegalArgumentException("skip value is negative");
                    long remaining = count;
                    int bufferSize = (int)(count < 512L ? count : 512L);
                    char[] skipBuffer = new char[bufferSize];
                    while (remaining > 0L) {
                        int toRead = (int)(remaining < (long)bufferSize ? remaining : (long)bufferSize);
                        int numRead = read(skipBuffer, 0, toRead);
                        if (numRead < 0) break;
                        remaining -= (long)numRead;
                    }
                    return count - remaining;
                }
                public boolean ready() throws IOException { return false; }
                public boolean markSupported() { return false; }
                public void mark(int readAheadLimit) throws IOException { throw new IOException("mark() not supported"); }
                public void reset() throws IOException { throw new IOException("reset() not supported"); }
                public abstract void close() throws IOException;
            }
            abstract class Writer implements Closeable, Flushable, Appendable {
                public void write(int value) throws IOException {
                    char[] cb = new char[1];
                    cb[0] = (char) value;
                    write(cb, 0, 1);
                }
                public void write(char[] cbuf) throws IOException {
                    if (cbuf == null) throw new NullPointerException();
                    write(cbuf, 0, cbuf.length);
                }
                public abstract void write(char[] cbuf, int offset, int length) throws IOException;
                public void write(String str) throws IOException {
                    if (str == null) throw new NullPointerException();
                    write(str, 0, str.length());
                }
                public void write(String str, int offset, int length) throws IOException {
                    if (str == null) throw new NullPointerException();
                    if (offset < 0 || length < 0 || offset > str.length() - length) throw new IndexOutOfBoundsException();
                    char[] cbuf = new char[length];
                    for (int i = 0; i < length; i++) cbuf[i] = str.charAt(offset + i);
                    write(cbuf, 0, length);
                }
                public Writer append(CharSequence csq) throws IOException {
                    if (csq == null) write("null");
                    else write(csq.toString());
                    return this;
                }
                public Writer append(CharSequence csq, int start, int end) throws IOException {
                    CharSequence cs = (csq == null ? "null" : csq);
                    write(cs.subSequence(start, end).toString());
                    return this;
                }
                public Writer append(char c) throws IOException {
                    write((int) c);
                    return this;
                }
                public abstract void flush() throws IOException;
                public abstract void close() throws IOException;
            }
            class StringReader extends Reader {
                private String str;
                private int length;
                private int next;
                private int mark;
                public StringReader(String s) {
                    if (s == null) throw new NullPointerException();
                    this.str = s;
                    this.length = s.length();
                    this.next = 0;
                    this.mark = 0;
                }
                private void ensureOpen() throws IOException {
                    if (str == null) throw new IOException("Stream closed");
                }
                public int read() throws IOException {
                    ensureOpen();
                    if (next >= length) return -1;
                    return str.charAt(next++);
                }
                public int read(char[] cbuf, int offset, int length) throws IOException {
                    ensureOpen();
                    if (cbuf == null) throw new NullPointerException();
                    if (offset < 0 || length < 0 || offset > cbuf.length - length) throw new IndexOutOfBoundsException();
                    if (length == 0) return 0;
                    if (next >= this.length) return -1;
                    int n = this.length - next < length ? this.length - next : length;
                    for (int i = 0; i < n; i++) cbuf[offset + i] = str.charAt(next + i);
                    next += n;
                    return n;
                }
                public long skip(long count) throws IOException {
                    ensureOpen();
                    if (next >= length) return 0L;
                    long skipped = count;
                    if (skipped > (long)(length - next)) skipped = (long)(length - next);
                    if (skipped < -(long)next) skipped = -(long)next;
                    next += (int)skipped;
                    return skipped;
                }
                public boolean ready() throws IOException {
                    ensureOpen();
                    return true;
                }
                public boolean markSupported() { return true; }
                public void mark(int readAheadLimit) throws IOException {
                    if (readAheadLimit < 0) throw new IllegalArgumentException("Read-ahead limit < 0");
                    ensureOpen();
                    mark = next;
                }
                public void reset() throws IOException {
                    ensureOpen();
                    next = mark;
                }
                public void close() {
                    str = null;
                }
            }
            class StringWriter extends Writer {
                private StringBuilder buf;
                public StringWriter() { this.buf = new StringBuilder(); }
                public StringWriter(int initialSize) {
                    if (initialSize < 0) throw new IllegalArgumentException("Negative buffer size");
                    this.buf = new StringBuilder();
                }
                public void write(int c) { buf.append((char) c); }
                public void write(char[] cbuf, int offset, int length) {
                    if (cbuf == null) throw new NullPointerException();
                    if (offset < 0 || length < 0 || offset > cbuf.length - length) throw new IndexOutOfBoundsException();
                    if (length == 0) return;
                    for (int i = 0; i < length; i++) buf.append(cbuf[offset + i]);
                }
                public void write(String str) {
                    if (str == null) throw new NullPointerException();
                    buf.append(str);
                }
                public void write(String str, int offset, int length) {
                    if (str == null) throw new NullPointerException();
                    if (offset < 0 || length < 0 || offset > str.length() - length) throw new IndexOutOfBoundsException();
                    buf.append(str.substring(offset, offset + length));
                }
                public StringWriter append(CharSequence csq) {
                    if (csq == null) write("null");
                    else write(csq.toString());
                    return this;
                }
                public StringWriter append(CharSequence csq, int start, int end) {
                    CharSequence cs = (csq == null ? "null" : csq);
                    write(cs.subSequence(start, end).toString());
                    return this;
                }
                public StringWriter append(char c) {
                    write((int) c);
                    return this;
                }
                public String toString() { return buf.toString(); }
                public void flush() {}
                public void close() throws IOException {}
            }
            class CharArrayReader extends Reader {
                private char[] buf;
                private int pos;
                private int markedPos;
                private int count;
                public CharArrayReader(char[] buf) {
                    if (buf == null) throw new NullPointerException();
                    this.buf = buf;
                    this.pos = 0;
                    this.count = buf.length;
                    this.markedPos = 0;
                }
                public CharArrayReader(char[] buf, int offset, int length) {
                    if (buf == null) throw new NullPointerException();
                    if (offset < 0 || offset > buf.length || length < 0 || offset + length < 0) throw new IllegalArgumentException();
                    this.buf = buf;
                    this.pos = offset;
                    this.count = offset + length > buf.length ? buf.length : offset + length;
                    this.markedPos = offset;
                }
                private void ensureOpen() throws IOException {
                    if (buf == null) throw new IOException("Stream closed");
                }
                public int read() throws IOException {
                    ensureOpen();
                    if (pos >= count) return -1;
                    return buf[pos++];
                }
                public int read(char[] b, int offset, int length) throws IOException {
                    ensureOpen();
                    if (b == null) throw new NullPointerException();
                    if (offset < 0 || length < 0 || offset > b.length - length) throw new IndexOutOfBoundsException();
                    if (length == 0) return 0;
                    if (pos >= count) return -1;
                    int avail = count - pos;
                    int n = length < avail ? length : avail;
                    for (int i = 0; i < n; i++) b[offset + i] = buf[pos + i];
                    pos += n;
                    return n;
                }
                public long skip(long n) throws IOException {
                    ensureOpen();
                    if (n <= 0L) return 0L;
                    int avail = count - pos;
                    int skipped = (int)(n > (long)avail ? (long)avail : n);
                    pos += skipped;
                    return (long)skipped;
                }
                public boolean ready() throws IOException {
                    ensureOpen();
                    return count - pos > 0;
                }
                public boolean markSupported() { return true; }
                public void mark(int readAheadLimit) throws IOException {
                    ensureOpen();
                    markedPos = pos;
                }
                public void reset() throws IOException {
                    ensureOpen();
                    pos = markedPos;
                }
                public void close() { buf = null; }
            }
            class CharArrayWriter extends Writer {
                private char[] buf;
                private int count;
                public CharArrayWriter() { this(32); }
                public CharArrayWriter(int initialSize) {
                    if (initialSize < 0) throw new IllegalArgumentException("Negative initial size: " + initialSize);
                    this.buf = new char[initialSize];
                    this.count = 0;
                }
                public void write(int c) {
                    if (count == buf.length) {
                        int newCap = buf.length == 0 ? 1 : buf.length * 2;
                        char[] newBuf = new char[newCap];
                        for (int i = 0; i < count; i++) newBuf[i] = buf[i];
                        buf = newBuf;
                    }
                    buf[count++] = (char) c;
                }
                public void write(char[] c, int offset, int length) {
                    if (c == null) throw new NullPointerException();
                    if (offset < 0 || length < 0 || offset > c.length - length) throw new IndexOutOfBoundsException();
                    if (length == 0) return;
                    int newCount = count + length;
                    if (newCount > buf.length) {
                        int newCap = buf.length * 2 > newCount ? buf.length * 2 : newCount;
                        char[] newBuf = new char[newCap];
                        for (int i = 0; i < count; i++) newBuf[i] = buf[i];
                        buf = newBuf;
                    }
                    for (int i = 0; i < length; i++) buf[count + i] = c[offset + i];
                    count = newCount;
                }
                public void write(String str, int offset, int length) {
                    if (str == null) throw new NullPointerException();
                    if (offset < 0 || length < 0 || offset > str.length() - length) throw new IndexOutOfBoundsException();
                    if (length == 0) return;
                    int newCount = count + length;
                    if (newCount > buf.length) {
                        int newCap = buf.length * 2 > newCount ? buf.length * 2 : newCount;
                        char[] newBuf = new char[newCap];
                        for (int i = 0; i < count; i++) newBuf[i] = buf[i];
                        buf = newBuf;
                    }
                    for (int i = 0; i < length; i++) buf[count + i] = str.charAt(offset + i);
                    count = newCount;
                }
                public void writeTo(Writer out) throws IOException {
                    if (out == null) throw new NullPointerException();
                    out.write(buf, 0, count);
                }
                public CharArrayWriter append(CharSequence csq) {
                    try {
                        if (csq == null) write("null");
                        else write(csq.toString());
                    } catch (IOException e) {}
                    return this;
                }
                public CharArrayWriter append(CharSequence csq, int start, int end) {
                    try {
                        CharSequence cs = (csq == null ? "null" : csq);
                        write(cs.subSequence(start, end).toString());
                    } catch (IOException e) {}
                    return this;
                }
                public CharArrayWriter append(char c) {
                    write((int) c);
                    return this;
                }
                public void reset() { count = 0; }
                public char[] toCharArray() {
                    char[] copy = new char[count];
                    for (int i = 0; i < count; i++) copy[i] = buf[i];
                    return copy;
                }
                public int size() { return count; }
                public String toString() {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < count; i++) sb.append(buf[i]);
                    return sb.toString();
                }
                public void flush() {}
                public void close() {}
            }
            class BufferedReader extends Reader {
                private Reader in;
                private char[] cb;
                private int nChars;
                private int nextChar;
                private int markedChar;
                private int readAheadLimit;
                public BufferedReader(Reader in, int sz) {
                    if (in == null) throw new NullPointerException();
                    if (sz <= 0) throw new IllegalArgumentException("Buffer size <= 0");
                    this.in = in;
                    this.cb = new char[sz];
                    this.nextChar = 0;
                    this.nChars = 0;
                    this.markedChar = -1;
                    this.readAheadLimit = 0;
                }
                public BufferedReader(Reader in) { this(in, 32); }
                private void ensureOpen() throws IOException {
                    if (in == null) throw new IOException("Stream closed");
                }
                private void fill() throws IOException {
                    int dst = 0;
                    if (markedChar <= -1) {
                        dst = 0;
                    } else {
                        int delta = nextChar - markedChar;
                        if (delta >= readAheadLimit) {
                            markedChar = -1;
                            readAheadLimit = 0;
                            dst = 0;
                        } else {
                            if (readAheadLimit <= cb.length) {
                                for (int i = 0; i < delta; i++) cb[i] = cb[markedChar + i];
                                markedChar = 0;
                                dst = delta;
                                nextChar = delta;
                                nChars = delta;
                            } else {
                                int newLength = cb.length * 2 > readAheadLimit ? readAheadLimit : cb.length * 2;
                                char[] ncb = new char[newLength];
                                for (int i = 0; i < delta; i++) ncb[i] = cb[markedChar + i];
                                cb = ncb;
                                markedChar = 0;
                                dst = delta;
                                nextChar = delta;
                                nChars = delta;
                            }
                        }
                    }
                    // Even at EOF, keep characters covered by a valid mark.
                    // Reposition before reading so an exhausted buffer is never replayed.
                    nChars = dst;
                    nextChar = dst;
                    int n = in.read(cb, dst, cb.length - dst);
                    if (n > 0) nChars = dst + n;
                }
                public int read() throws IOException {
                    ensureOpen();
                    if (nextChar >= nChars) {
                        fill();
                        if (nextChar >= nChars) return -1;
                    }
                    return cb[nextChar++];
                }
                public int read(char[] cbuf, int offset, int length) throws IOException {
                    ensureOpen();
                    if (cbuf == null) throw new NullPointerException();
                    if (offset < 0 || length < 0 || offset > cbuf.length - length) throw new IndexOutOfBoundsException();
                    if (length == 0) return 0;
                    int count = 0;
                    while (count < length) {
                        if (nextChar >= nChars) {
                            fill();
                            if (nextChar >= nChars) break;
                        }
                        int avail = nChars - nextChar;
                        int toCopy = length - count < avail ? length - count : avail;
                        for (int i = 0; i < toCopy; i++) cbuf[offset + count + i] = cb[nextChar + i];
                        nextChar += toCopy;
                        count += toCopy;
                    }
                    return count == 0 && nextChar >= nChars ? -1 : count;
                }
                public String readLine() throws IOException {
                    ensureOpen();
                    if (nextChar >= nChars) {
                        fill();
                        if (nextChar >= nChars) return null;
                    }
                    char[] lineBuf = new char[80];
                    int lineLen = 0;
                    boolean eof = false;
                    while (true) {
                        if (nextChar >= nChars) {
                            fill();
                            if (nextChar >= nChars) {
                                eof = true;
                                break;
                            }
                        }
                        char c = cb[nextChar++];
                        if (c == 10) break;
                        if (c == 13) {
                            if (nextChar >= nChars) fill();
                            if (nextChar < nChars && cb[nextChar] == 10) nextChar++;
                            break;
                        }
                        if (lineLen == lineBuf.length) {
                            char[] newBuf = new char[lineBuf.length * 2];
                            for (int i = 0; i < lineLen; i++) newBuf[i] = lineBuf[i];
                            lineBuf = newBuf;
                        }
                        lineBuf[lineLen++] = c;
                    }
                    if (lineLen == 0 && eof) return null;
                    StringBuilder line = new StringBuilder();
                    for (int i = 0; i < lineLen; i++) line.append(lineBuf[i]);
                    return line.toString();
                }
                public long skip(long n) throws IOException {
                    ensureOpen();
                    if (n < 0L) throw new IllegalArgumentException("skip value is negative");
                    long r = n;
                    while (r > 0L) {
                        if (nextChar >= nChars) fill();
                        if (nextChar >= nChars) break;
                        int avail = nChars - nextChar;
                        if (r <= (long) avail) {
                            nextChar += (int) r;
                            r = 0L;
                            break;
                        } else {
                            r -= (long) avail;
                            nextChar = nChars;
                        }
                    }
                    return n - r;
                }
                public boolean ready() throws IOException {
                    ensureOpen();
                    return (nChars - nextChar > 0) || in.ready();
                }
                public boolean markSupported() { return true; }
                public void mark(int readAheadLimit) throws IOException {
                    if (readAheadLimit < 0) throw new IllegalArgumentException("Read-ahead limit < 0");
                    ensureOpen();
                    this.readAheadLimit = readAheadLimit;
                    markedChar = nextChar;
                }
                public void reset() throws IOException {
                    ensureOpen();
                    if (markedChar < 0) throw new IOException("Mark invalid");
                    nextChar = markedChar;
                }
                public void close() throws IOException {
                    if (in == null) return;
                    in.close();
                    in = null;
                    cb = null;
                }
            }
            class BufferedWriter extends Writer {
                private Writer out;
                private char[] cb;
                private int nChars;
                private int nextChar;
                public BufferedWriter(Writer out, int sz) {
                    if (out == null) throw new NullPointerException();
                    if (sz <= 0) throw new IllegalArgumentException("Buffer size <= 0");
                    this.out = out;
                    this.cb = new char[sz];
                    this.nChars = sz;
                    this.nextChar = 0;
                }
                public BufferedWriter(Writer out) { this(out, 32); }
                private void ensureOpen() throws IOException {
                    if (out == null) throw new IOException("Stream closed");
                }
                private void flushBuffer() throws IOException {
                    ensureOpen();
                    if (nextChar == 0) return;
                    out.write(cb, 0, nextChar);
                    nextChar = 0;
                }
                public void write(int c) throws IOException {
                    ensureOpen();
                    if (nextChar >= nChars) flushBuffer();
                    cb[nextChar++] = (char) c;
                }
                public void write(char[] cbuf, int offset, int length) throws IOException {
                    ensureOpen();
                    if (cbuf == null) throw new NullPointerException();
                    if (offset < 0 || length < 0 || offset > cbuf.length - length) throw new IndexOutOfBoundsException();
                    if (length >= nChars) {
                        flushBuffer();
                        out.write(cbuf, offset, length);
                        return;
                    }
                    int b = offset, t = offset + length;
                    while (b < t) {
                        int avail = nChars - nextChar;
                        int toCopy = t - b < avail ? t - b : avail;
                        for (int i = 0; i < toCopy; i++) cb[nextChar + i] = cbuf[b + i];
                        nextChar += toCopy;
                        b += toCopy;
                        if (nextChar >= nChars) flushBuffer();
                    }
                }
                public void write(String s, int offset, int length) throws IOException {
                    ensureOpen();
                    if (s == null) throw new NullPointerException();
                    if (offset < 0 || length < 0 || offset > s.length() - length) throw new IndexOutOfBoundsException();
                    int b = offset, t = offset + length;
                    while (b < t) {
                        int avail = nChars - nextChar;
                        int toCopy = t - b < avail ? t - b : avail;
                        for (int i = 0; i < toCopy; i++) cb[nextChar + i] = s.charAt(b + i);
                        nextChar += toCopy;
                        b += toCopy;
                        if (nextChar >= nChars) flushBuffer();
                    }
                }
                public void newLine() throws IOException { write(10); }
                public void flush() throws IOException {
                    flushBuffer();
                    out.flush();
                }
                public void close() throws IOException {
                    if (out == null) return;
                    try {
                        try { flushBuffer(); } finally { out.close(); }
                    } finally {
                        out = null;
                        cb = null;
                    }
                }
            }
            """);
    }
}
