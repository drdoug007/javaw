package javaw.runtime;

/** Stateless, replacement-mode String codecs for the explicitly supported charsets. */
public final class TextCharsets {
    private TextCharsets() {}
    public static int lookup(String name) throws java.io.UnsupportedEncodingException {
        if(name==null)throw new NullPointerException();
        if(name.equalsIgnoreCase("UTF-8") || name.equalsIgnoreCase("UTF8") || name.equalsIgnoreCase("unicode-1-1-utf-8"))return 0;
        throw new java.io.UnsupportedEncodingException(name);
    }
    public static byte[] encode(String text,String name) throws java.io.UnsupportedEncodingException {
        return encode(text,lookup(name));
    }
    public static byte[] encode(String text,int charset){
        if(charset==0)return text.getBytes();
        throw new UnsupportedOperationException();
    }
    public static String decode(byte[] bytes,int offset,int count,int charset){
        int length=bytes.length;
        if(offset<0 || count<0 || offset>length-count)throw new StringIndexOutOfBoundsException();
        if(charset==0)return new String(bytes,offset,count);
        throw new UnsupportedOperationException();
    }
    public static String decode(byte[] bytes,int offset,int count,String name) throws java.io.UnsupportedEncodingException {
        return decode(bytes,offset,count,lookup(name));
    }
}
