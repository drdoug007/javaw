package javaw.runtime;

/** Stateless, replacement-mode String codecs for the explicitly supported charsets. */
public final class TextCharsets {
    private TextCharsets() {}
    private static final String[][] NAMES={
        {"UTF-8","UTF8","unicode-1-1-utf-8"},
        {"ISO-8859-1","cp819","ISO_8859_1","csISOLatin1","IBM819","latin1","ISO8859-1","iso-ir-100","ISO_8859-1:1987","ISO8859_1","819","l1","ISO_8859-1","8859_1","IBM-819"},
        {"US-ASCII","iso_646.irv:1983","ASCII","iso-ir-6","cp367","ANSI_X3.4-1986","us","646","ISO646-US","ISO_646.irv:1991","csASCII","IBM367","ascii7","ANSI_X3.4-1968"},
        {"UTF-16BE","ISO-10646-UCS-2","UTF_16BE","UnicodeBigUnmarked","X-UTF-16BE"},
        {"UTF-16LE","UnicodeLittleUnmarked","UTF_16LE","X-UTF-16LE"},
        {"UTF-16","UTF_16","unicode","UnicodeBig","utf16"}
    };
    public static String canonical(int id){return NAMES[id][0];}
    public static String[] aliases(int id){String[] names=NAMES[id],result=new String[names.length-1];
        System.arraycopy(names,1,result,0,result.length);return result;}
    public static int identifier(java.nio.charset.Charset charset){
        if(charset==null)throw new NullPointerException();
        if(charset==java.nio.charset.StandardCharsets.UTF_8)return 0;
        if(charset==java.nio.charset.StandardCharsets.ISO_8859_1)return 1;
        if(charset==java.nio.charset.StandardCharsets.US_ASCII)return 2;
        if(charset==java.nio.charset.StandardCharsets.UTF_16BE)return 3;
        if(charset==java.nio.charset.StandardCharsets.UTF_16LE)return 4;
        if(charset==java.nio.charset.StandardCharsets.UTF_16)return 5;
        throw new UnsupportedOperationException("Custom charset conversion is unsupported");
    }

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
        for(int id=0;id<NAMES.length;id++)for(String alias:NAMES[id])if(name.equalsIgnoreCase(alias))return id;
        throw new java.io.UnsupportedEncodingException(name);
    }
    public static byte[] encode(String text,String name) throws java.io.UnsupportedEncodingException {
        return encode(text,lookup(name));
    }
    public static byte[] encode(String text,int charset){
        if(charset==0)return text.getBytes();
        if(charset==1 || charset==2){
            int limit=charset==1?255:127;
            int size=text.length();
            for(int j=0;j<text.length()-1;j++)if(Character.isHighSurrogate(text.charAt(j)) && Character.isLowSurrogate(text.charAt(j+1))){size--;j++;}
            byte[] result=new byte[size];int out=0;
            for(int j=0;j<text.length();j++){char c=text.charAt(j);
                if(c<=limit)result[out++]=(byte)c;
                else{result[out++]=63;if(Character.isHighSurrogate(c) && j+1<text.length() && Character.isLowSurrogate(text.charAt(j+1)))j++;}
            }return result;
        }
        if(charset==3)return encode16(text,false,false);
        if(charset==4)return encode16(text,true,false);
        if(charset==5)return encode16(text,false,true);
        throw new UnsupportedOperationException();
    }
    private static byte[] encode16(String text,boolean little,boolean bom){
        long size=(long)text.length()*2+(bom && text.length()!=0?2:0);
        if(size>Integer.MAX_VALUE)throw new OutOfMemoryError();
        byte[] bytes=new byte[(int)size];int out=0;
        if(bom && text.length()!=0){bytes[out++]=(byte)0xfe;bytes[out++]=(byte)0xff;}
        for(int j=0;j<text.length();j++){
            char c=text.charAt(j);
            if(Character.isHighSurrogate(c)){
                if(j+1<text.length() && Character.isLowSurrogate(text.charAt(j+1))){
                    put16(bytes,out,c,little);out+=2;c=text.charAt(++j);
                }else c=0xfffd;
            }else if(Character.isLowSurrogate(c))c=0xfffd;
            put16(bytes,out,c,little);out+=2;
        }return bytes;
    }
    private static void put16(byte[] bytes,int offset,char unit,boolean little){
        bytes[offset]=(byte)(little?unit:unit>>8);bytes[offset+1]=(byte)(little?unit>>8:unit);
    }
    private static int unit16(byte[] bytes,int offset,boolean little){
        int a=bytes[offset]&255,b=bytes[offset+1]&255;return little?a|(b<<8):(a<<8)|b;
    }
    private static String decode16(byte[] bytes,int offset,int count,boolean little,boolean bom){
        int end=offset+count;
        if(bom && count>=2){int mark=unit16(bytes,offset,false);
            if(mark==0xfeff || mark==0xfffe){little=mark==0xfffe;offset+=2;}}
        char[] chars=new char[(end-offset)/2+((end-offset)&1)];int out=0;
        while(offset<end){
            if(end-offset<2){chars[out++]=0xfffd;break;}
            char c=(char)unit16(bytes,offset,little);offset+=2;
            if(Character.isHighSurrogate(c)){
                if(end-offset<2){chars[out++]=0xfffd;break;}
                char next=(char)unit16(bytes,offset,little);offset+=2;
                if(Character.isLowSurrogate(next)){chars[out++]=c;chars[out++]=next;}else chars[out++]=0xfffd;
            }else chars[out++]=Character.isLowSurrogate(c)?0xfffd:c;
        }return new String(chars,0,out);
    }
    public static String decode(byte[] bytes,int offset,int count,int charset){
        int length=bytes.length;
        if(offset<0 || count<0 || offset>length-count)throw new StringIndexOutOfBoundsException();
        if(charset==0)return new String(bytes,offset,count);
        if(charset==3)return decode16(bytes,offset,count,false,false);
        if(charset==4)return decode16(bytes,offset,count,true,false);
        if(charset==5)return decode16(bytes,offset,count,false,true);
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
