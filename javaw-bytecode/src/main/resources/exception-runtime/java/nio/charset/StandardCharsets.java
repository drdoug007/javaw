package java.nio.charset;

/** The six guaranteed standard charsets, using replacement-mode String conversion. */
public final class StandardCharsets {
    private StandardCharsets() {}
    public static final Charset US_ASCII=new Charset.Builtin(2);
    public static final Charset ISO_8859_1=new Charset.Builtin(1);
    public static final Charset UTF_8=new Charset.Builtin(0);
    public static final Charset UTF_16BE=new Charset.Builtin(3);
    public static final Charset UTF_16LE=new Charset.Builtin(4);
    public static final Charset UTF_16=new Charset.Builtin(5);
}
