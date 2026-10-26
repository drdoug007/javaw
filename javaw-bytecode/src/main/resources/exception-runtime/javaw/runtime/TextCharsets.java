package javaw.runtime;

/** Stateless, replacement-mode String codecs for the explicitly supported charsets. */
public final class TextCharsets {
    private TextCharsets() {}
    public static boolean validName(String name){
        if(name.length()==0)return false;
        for(int i=0;i<name.length();i++){char c=name.charAt(i);
            if(c>='A' && c<='Z' || c>='a' && c<='z' || c>='0' && c<='9')continue;
            if(i!=0 && (c=='-' || c=='+' || c==':' || c=='_' || c=='.'))continue;
            return false;
        }return true;
    }
    public static int lookup(String name) throws java.io.UnsupportedEncodingException {
        if(name==null)throw new NullPointerException();
        if(!validName(name))throw new java.io.UnsupportedEncodingException(name);
        if(name.equalsIgnoreCase("UTF-8") || name.equalsIgnoreCase("UTF8") || name.equalsIgnoreCase("unicode-1-1-utf-8"))return 0;
        if(name.equalsIgnoreCase("ISO-8859-1") || name.equalsIgnoreCase("cp819") || name.equalsIgnoreCase("ISO_8859_1") || name.equalsIgnoreCase("csISOLatin1") || name.equalsIgnoreCase("IBM819") || name.equalsIgnoreCase("latin1") || name.equalsIgnoreCase("ISO8859-1") || name.equalsIgnoreCase("iso-ir-100") || name.equalsIgnoreCase("ISO_8859-1:1987") || name.equalsIgnoreCase("ISO8859_1") || name.equalsIgnoreCase("819") || name.equalsIgnoreCase("l1") || name.equalsIgnoreCase("ISO_8859-1") || name.equalsIgnoreCase("8859_1") || name.equalsIgnoreCase("IBM-819"))return 1;
        if(name.equalsIgnoreCase("US-ASCII") || name.equalsIgnoreCase("iso_646.irv:1983") || name.equalsIgnoreCase("ASCII") || name.equalsIgnoreCase("iso-ir-6") || name.equalsIgnoreCase("cp367") || name.equalsIgnoreCase("ANSI_X3.4-1986") || name.equalsIgnoreCase("us") || name.equalsIgnoreCase("646") || name.equalsIgnoreCase("ISO646-US") || name.equalsIgnoreCase("ISO_646.irv:1991") || name.equalsIgnoreCase("csASCII") || name.equalsIgnoreCase("IBM367") || name.equalsIgnoreCase("ascii7") || name.equalsIgnoreCase("ANSI_X3.4-1968"))return 2;
        throw new java.io.UnsupportedEncodingException(name);
    }
    public static byte[] encode(String text,String name) throws java.io.UnsupportedEncodingException {
        return encode(text,lookup(name));
    }
    public static byte[] encode(String text,int charset){
        if(charset==0)return text.getBytes();
        if(charset==2){
            int size=text.length();
            for(int j=0;j<text.length()-1;j++)if(Character.isHighSurrogate(text.charAt(j)) && Character.isLowSurrogate(text.charAt(j+1))){size--;j++;}
            byte[] result=new byte[size];int out=0;
            for(int j=0;j<text.length();j++){char c=text.charAt(j);
                if(c<=127)result[out++]=(byte)c;
                else{result[out++]=63;if(Character.isHighSurrogate(c) && j+1<text.length() && Character.isLowSurrogate(text.charAt(j+1)))j++;}
            }return result;
        }
        if(charset==1){
            int size=text.length();
            for(int j=0;j<text.length()-1;j++)if(Character.isHighSurrogate(text.charAt(j)) && Character.isLowSurrogate(text.charAt(j+1))){size--;j++;}
            byte[] result=new byte[size];int out=0;
            for(int j=0;j<text.length();j++){char c=text.charAt(j);
                if(c<=255)result[out++]=(byte)c;
                else{result[out++]=63;if(Character.isHighSurrogate(c) && j+1<text.length() && Character.isLowSurrogate(text.charAt(j+1)))j++;}
            }return result;
        }
        throw new UnsupportedOperationException();
    }
    public static String decode(byte[] bytes,int offset,int count,int charset){
        int length=bytes.length;
        if(offset<0 || count<0 || offset>length-count)throw new StringIndexOutOfBoundsException();
        if(charset==0)return new String(bytes,offset,count);
        if(charset==2){char[] chars=new char[count];
            for(int j=0;j<count;j++){int b=bytes[offset+j]&255;chars[j]=(char)(b<128?b:0xfffd);}
            return new String(chars);
        }

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
