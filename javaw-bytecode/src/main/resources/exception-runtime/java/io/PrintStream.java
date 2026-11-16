package java.io;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Arrays;
import java.util.Locale;

/** Character text over an OutputStream; single-threaded, swallows IOException into an error flag. */
public class PrintStream extends FilterOutputStream implements Appendable, Closeable {
    private final boolean autoFlush;
    private boolean trouble;
    private final Charset charset;
    /** The stream's charset, for PrintWriter(OutputStream) over a PrintStream (the public charset() is Java 18). */
    Charset streamCharset() {return charset;}
    private boolean closing;
    private boolean haveLeftover;
    private char leftover;
    private boolean marked;
    public PrintStream(OutputStream out) {
        this(out, false);
    }
    public PrintStream(OutputStream out, boolean autoFlush) {
        super(requireNonNull(out, "Null output stream"));
        this.autoFlush = autoFlush;
        this.charset = out instanceof PrintStream ? ((PrintStream) out).charset : Charset.defaultCharset();
    }
    public PrintStream(OutputStream out, boolean autoFlush, String encoding) throws UnsupportedEncodingException {
        this(requireNonNull(out, "Null output stream"), autoFlush, toCharset(encoding));
    }
    public PrintStream(OutputStream out, boolean autoFlush, Charset charset) {
        super(out);
        this.autoFlush = autoFlush;
        this.charset = requireNonNull(charset, "charset");
    }
    public void flush() {
        try {
            ensureOpen();
            out.flush();
        } catch (IOException x) {
            trouble = true;
        }
    }
    public void close() {
        if (closing) return;
        closing = true;
        try {
            if (haveLeftover) {
                haveLeftover = false;
                emit(String.valueOf(leftover).getBytes(charset));
            }
            flush();
            close();
            out.close();
        } catch (IOException x) {
            trouble = true;
        }
        out = null;
    }
    public boolean checkError() {
        if (out != null) flush();
        if (out instanceof PrintStream) return ((PrintStream) out).checkError();
        return trouble;
    }
    protected void setError() { trouble = true; }
    protected void clearError() { trouble = false; }
    public void write(int b) {
        try {
            ensureOpen();
            out.write(b);
            if (b == '\n' && autoFlush) out.flush();
        } catch (IOException x) {
            trouble = true;
        }
    }
    public void write(byte[] buf, int off, int len) {
        try {
            ensureOpen();
            out.write(buf, off, len);
            if (autoFlush) out.flush();
        } catch (IOException x) {
            trouble = true;
        }
    }
    public void write(byte[] buf) throws IOException {
        this.write(buf, 0, buf.length);
    }
    public void writeBytes(byte[] buf) {
        this.write(buf, 0, buf.length);
    }
    public void print(boolean b) { writeText(String.valueOf(b)); }
    public void print(char c) { writeText(String.valueOf(c)); }
    public void print(int i) { writeText(String.valueOf(i)); }
    public void print(long l) { writeText(String.valueOf(l)); }
    public void print(float f) { writeText(String.valueOf(f)); }
    public void print(double d) { writeText(String.valueOf(d)); }
    public void print(char[] s) { writeText(s); }
    public void print(String s) { writeText(String.valueOf(s)); }
    public void print(Object obj) { writeText(String.valueOf(obj)); }
    public void println() { newLine(); }
    public void println(boolean b) {
        if (getClass() == PrintStream.class) writeLine(String.valueOf(b));
        else { print(b); newLine(); }
    }
    public void println(char c) {
        if (getClass() == PrintStream.class) writeLine(String.valueOf(c));
        else { print(c); newLine(); }
    }
    public void println(int i) {
        if (getClass() == PrintStream.class) writeLine(String.valueOf(i));
        else { print(i); newLine(); }
    }
    public void println(long l) {
        if (getClass() == PrintStream.class) writeLine(String.valueOf(l));
        else { print(l); newLine(); }
    }
    public void println(float f) {
        if (getClass() == PrintStream.class) writeLine(String.valueOf(f));
        else { print(f); newLine(); }
    }
    public void println(double d) {
        if (getClass() == PrintStream.class) writeLine(String.valueOf(d));
        else { print(d); newLine(); }
    }
    public void println(char[] s) {
        if (getClass() == PrintStream.class) writeLine(s);
        else { print(s); newLine(); }
    }
    public void println(String s) {
        if (getClass() == PrintStream.class) writeLine(String.valueOf(s));
        else { print(s); newLine(); }
    }
    public void println(Object x) {
        String s = String.valueOf(x);
        if (getClass() == PrintStream.class) writeLine(String.valueOf(s));
        else { print(s); newLine(); }
    }
    public PrintStream printf(String format, Object... args) { return format(format, args); }
    public PrintStream printf(Locale l, String format, Object... args) { return format(l, format, args); }
    public PrintStream format(String format, Object... args) {
        try {
            ensureOpen();
            append(String.format(format, args));
        } catch (IOException x) {
            trouble = true;
        }
        return this;
    }
    public PrintStream format(Locale l, String format, Object... args) {
        try {
            ensureOpen();
            append(String.format(l, format, args));
        } catch (IOException x) {
            trouble = true;
        }
        return this;
    }
    public PrintStream append(CharSequence csq) {
        print(String.valueOf(csq));
        return this;
    }
    public PrintStream append(CharSequence csq, int start, int end) {
        if (csq == null) csq = "null";
        return append(csq.subSequence(start, end));
    }
    public PrintStream append(char c) {
        print(c);
        return this;
    }
    private void writeText(String s) {
        try {
            ensureOpen();
            encode(s);
            if (autoFlush && s.indexOf('\n') >= 0) out.flush();
        } catch (IOException x) {
            trouble = true;
        }
    }
    private void writeText(char[] buf) {
        try {
            ensureOpen();
            String s = new String(buf);
            encode(s);
            if (autoFlush && s.indexOf('\n') >= 0) out.flush();
        } catch (IOException x) {
            trouble = true;
        }
    }
    private void writeLine(String s) {
        try {
            ensureOpen();
            encode(s + "\n");
            if (autoFlush) out.flush();
        } catch (IOException x) {
            trouble = true;
        }
    }
    private void writeLine(char[] buf) {
        try {
            ensureOpen();
            encode(new String(buf) + "\n");
            if (autoFlush) out.flush();
        } catch (IOException x) {
            trouble = true;
        }
    }
    private void newLine() {
        try {
            ensureOpen();
            encode("\n");
            if (autoFlush) out.flush();
        } catch (IOException x) {
            trouble = true;
        }
    }
    private void encode(String s) {
        int n = s.length();
        if (haveLeftover) {
            s = String.valueOf(leftover) + s;
            n++;
            haveLeftover = false;
        }
        if (n > 0 && Character.isHighSurrogate(s.charAt(n - 1))) {
            leftover = s.charAt(n - 1);
            haveLeftover = true;
            s = s.substring(0, n - 1);
        }
        if (!s.isEmpty()) emit(s.getBytes(charset));
    }
    private void emit(byte[] bytes) {
        if ("UTF-16".equals(charset.name())) {
            if (marked) bytes = Arrays.copyOfRange(bytes, 2, bytes.length);
            marked = true;
        }
        write(bytes, 0, bytes.length);
    }
    private void ensureOpen() throws IOException {
        if (out == null) throw new IOException("Stream closed");
    }
    private static <T> T requireNonNull(T obj, String message) {
        if (obj == null) throw new NullPointerException(message);
        return obj;
    }
    private static Charset toCharset(String csn) throws UnsupportedEncodingException {
        requireNonNull(csn, "charsetName");
        try {
            return Charset.forName(csn);
        } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
            throw new UnsupportedEncodingException(csn);
        }
    }
}
