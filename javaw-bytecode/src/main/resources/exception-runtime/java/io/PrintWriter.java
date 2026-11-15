package java.io;

import java.util.Locale;

/** Character output over a Writer; swallows IOException into an error flag; matches JDK 21 semantics. */
public class PrintWriter extends Writer {
    protected Writer out;
    private final boolean autoFlush;
    private boolean trouble;
    private static final String LINE_SEPARATOR = "\n";
    public PrintWriter(Writer out) {this(out, false);}
    public PrintWriter(Writer out, boolean autoFlush) {super(out);this.out = out;this.autoFlush = autoFlush;}
    private void ensureOpen() throws IOException {if (out == null)throw new IOException("Stream closed");}
    public void flush() {
        try {ensureOpen();out.flush();}
        catch (IOException x) {trouble = true;}
    }
    public void close() {
        try {if (out != null) {out.close();out = null;}}
        catch (IOException x) {trouble = true;}
    }
    public boolean checkError() {
        if (out != null)flush();
        if (out instanceof PrintWriter)
            return ((PrintWriter) out).checkError();
        return trouble;
    }
    protected void setError() {trouble = true;}
    protected void clearError() {trouble = false;}
    public void write(int c) {
        try {ensureOpen();out.write(c);}
        catch (IOException x) {trouble = true;}
    }
    public void write(char[] buf, int off, int len) {
        try {ensureOpen();out.write(buf, off, len);}
        catch (IOException x) {trouble = true;}
    }
    public void write(char[] buf) {write(buf, 0, buf.length);}
    public void write(String s, int off, int len) {
        try {ensureOpen();out.write(s, off, len);}
        catch (IOException x) {trouble = true;}
    }
    public void write(String s) {write(s, 0, s.length());}
    private void newLine() {
        try {ensureOpen();out.write(LINE_SEPARATOR);if (autoFlush)out.flush();}
        catch (IOException x) {trouble = true;}
    }
    public void print(boolean b) {write(String.valueOf(b));}
    public void print(char c) {write(c);}
    public void print(int i) {write(String.valueOf(i));}
    public void print(long l) {write(String.valueOf(l));}
    public void print(float f) {write(String.valueOf(f));}
    public void print(double d) {write(String.valueOf(d));}
    public void print(char[] s) {write(s);}
    public void print(String s) {write(String.valueOf(s));}
    public void print(Object obj) {write(String.valueOf(obj));}
    public void println() {newLine();}
    public void println(boolean b) {print(b);println();}
    public void println(char c) {print(c);println();}
    public void println(int i) {print(i);println();}
    public void println(long l) {print(l);println();}
    public void println(float f) {print(f);println();}
    public void println(double d) {print(d);println();}
    public void println(char[] s) {print(s);println();}
    public void println(String s) {print(s);println();}
    public void println(Object x) {String s = String.valueOf(x);print(s);println();}
    public PrintWriter printf(String format, Object... args) {return format(format, args);}
    public PrintWriter printf(Locale l, String format, Object... args) {return format(l, format, args);}
    public PrintWriter format(String format, Object... args) {
        try {ensureOpen();write(String.format(format, args));if (autoFlush)out.flush();}
        catch (IOException x) {trouble = true;}
        return this;
    }
    public PrintWriter format(Locale l, String format, Object... args) {
        try {ensureOpen();write(String.format(l, format, args));if (autoFlush)out.flush();}
        catch (IOException x) {trouble = true;}
        return this;
    }
    public PrintWriter append(CharSequence csq) {write(String.valueOf(csq));return this;}
    public PrintWriter append(CharSequence csq, int start, int end) {
        if (csq == null)csq = "null";
        return append(csq.subSequence(start, end));
    }
    public PrintWriter append(char c) {write(c);return this;}
}
