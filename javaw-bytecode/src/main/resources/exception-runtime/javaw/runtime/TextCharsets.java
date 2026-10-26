package javaw.runtime;

/** Stateless, replacement-mode String codecs for the explicitly supported charsets. */
public final class TextCharsets {
    private TextCharsets() {}
    public static int lookup(String name) throws java.io.UnsupportedEncodingException {
        if(name==null)throw new NullPointerException();
        if(name.equalsIgnoreCase("UTF-8") || name.equalsIgnoreCase("UTF8") || name.equalsIgnoreCase("unicode-1-1-utf-8"))return 0;
        if(name.equalsIgnoreCase("ISO-8859-1") || name.equalsIgnoreCase("cp819") || name.equalsIgnoreCase("ISO_8859_1") || name.equalsIgnoreCase("csISOLatin1") || name.equalsIgnoreCase("IBM819") || name.equalsIgnoreCase("latin1") || name.equalsIgnoreCase("ISO8859-1") || name.equalsIgnoreCase("iso-ir-100") || name.equalsIgnoreCase("ISO_8859-1:1987") || name.equalsIgnoreCase("ISO8859_1") || name.equalsIgnoreCase("819") || name.equalsIgnoreCase("l1") || name.equalsIgnoreCase("ISO_8859-1") || name.equalsIgnoreCase("8859_1") || name.equalsIgnoreCase("IBM-819"))return 1;
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
        if(charset==1){char[] chars=new char[count];
            for(int j=0;j<count;j++){int b=bytes[offset+j]&255;chars[j]=(char)(b);}
            return new String(chars);
        }

        throw new UnsupportedOperationException();
    }
    public static String decode(byte[] bytes,int offset,int count,String name) throws java.io.UnsupportedEncodingException {
        return decode(bytes,offset,count,lookup(name));
    }
}
