package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T17: java.util.Base64 with its Encoder and Decoder. Expected values come from the JVM; see tasks/T17-base64.md. */
class BytecodeBase64Test extends KitDifferential {
    private static final String FIXTURE = """
        import java.util.Arrays;
        import java.util.Base64;
        public class Fixture {
            interface Call{Object run();}
            static String show(Object v){return v instanceof byte[]?Arrays.toString((byte[])v):String.valueOf(v);}
            static String t(Call c){
                try{return "ok:"+show(c.run());}
                catch(NullPointerException e){return "NullPointerException";}
                catch(RuntimeException e){return e.getClass().getSimpleName()+":"+e.getMessage();}
            }
            static byte[] ascii(String s){byte[] b=new byte[s.length()];for(int i=0;i<b.length;i++)b[i]=(byte)s.charAt(i);return b;}
            static String text(byte[] b){StringBuilder out=new StringBuilder();for(byte x:b)out.append((char)(x&0xff));return out.toString();}
            static String lines(String s){return s.replace("\\r","\\\\r").replace("\\n","\\\\n");}

            public static String basicEncode(){
                StringBuilder out=new StringBuilder();
                for(String s:new String[]{"","f","fo","foo","foob","fooba","foobar"})out.append(Base64.getEncoder().encodeToString(ascii(s))).append('|');
                out.append(Base64.getEncoder().encodeToString(new byte[]{-1,-2,-3})).append('|');
                out.append(Base64.getEncoder().encodeToString(new byte[]{0,0,0,0})).append('|');
                out.append(Arrays.toString(Base64.getEncoder().encode(new byte[]{-5,-1}))).append('|');
                out.append(Base64.getEncoder().encode(new byte[0]).length);
                return out.toString();
            }
            public static String urlEncode(){
                byte[] b={(byte)0xfb,(byte)0xff,(byte)0xbf,(byte)0xfe};
                return Base64.getEncoder().encodeToString(b)+"|"+Base64.getUrlEncoder().encodeToString(b)+"|"+Base64.getUrlEncoder().withoutPadding().encodeToString(b)
                        +"|"+text(Base64.getUrlEncoder().encode(new byte[]{-8,-1,-2}));
            }
            public static String withoutPadding(){
                Base64.Encoder e=Base64.getEncoder().withoutPadding();
                StringBuilder out=new StringBuilder();
                for(String s:new String[]{"","f","fo","foo","foob"})out.append(e.encodeToString(ascii(s))).append('|');
                out.append(e.encode(ascii("f")).length).append(',').append(e.encode(ascii("fo")).length).append('|');
                out.append(Base64.getEncoder()==Base64.getEncoder()).append(',').append(e.withoutPadding()==e).append(',')
                   .append(Base64.getEncoder().withoutPadding()==Base64.getEncoder()).append(',').append(Base64.getUrlEncoder()==Base64.getUrlEncoder())
                   .append(',').append(Base64.getMimeEncoder()==Base64.getMimeEncoder());
                return out.toString();
            }
            public static String encodeIntoArray(){
                StringBuilder out=new StringBuilder();
                byte[] dst=new byte[10];Arrays.fill(dst,(byte)'.');
                int n=Base64.getEncoder().encode(ascii("fooba"),dst);
                out.append(n).append(':').append(text(dst)).append('|');
                byte[] exact=new byte[4];
                out.append(Base64.getEncoder().encode(ascii("ab"),exact)).append(':').append(text(exact)).append('|');
                byte[] three=new byte[3];
                out.append(Base64.getEncoder().withoutPadding().encode(ascii("ab"),three)).append(':').append(text(three)).append('|');
                out.append(Base64.getEncoder().encode(new byte[0],new byte[0])).append('|');
                out.append(t(()->Base64.getEncoder().encode(ascii("ab"),new byte[3]))).append('|');
                out.append(t(()->Base64.getEncoder().encode((byte[])null))).append('|');
                out.append(t(()->Base64.getEncoder().encodeToString(null))).append('|');
                out.append(t(()->Base64.getEncoder().encode(ascii("ab"),null)));
                return out.toString();
            }

            public static String mimeEncode(){
                byte[] b=new byte[100];for(int i=0;i<b.length;i++)b[i]=(byte)(i*7);
                String all=Base64.getMimeEncoder().encodeToString(b);
                String s57=Base64.getMimeEncoder().encodeToString(Arrays.copyOf(b,57)),s58=Base64.getMimeEncoder().encodeToString(Arrays.copyOf(b,58));
                return lines(all)+"|"+all.length()+"|"+s57.length()+","+s57.indexOf('\\r')+"|"+s58.length()+","+s58.indexOf('\\r')+"|"+Base64.getMimeEncoder().encodeToString(new byte[0]).length()
                        +"|"+lines(Base64.getMimeEncoder().withoutPadding().encodeToString(Arrays.copyOf(b,58)));
            }
            public static String customMime(){
                byte[] b=ascii("The quick brown fox");
                StringBuilder out=new StringBuilder();
                out.append(Base64.getMimeEncoder(10,ascii("#")).encodeToString(b)).append('|');
                out.append(Base64.getMimeEncoder(8,ascii("::")).withoutPadding().encodeToString(b)).append('|');
                out.append(Base64.getMimeEncoder(3,ascii("#")).encodeToString(b)).append('|');
                out.append(Base64.getMimeEncoder(3,ascii("#"))==Base64.getEncoder()).append('|');
                out.append(Base64.getMimeEncoder(-5,new byte[0]).encodeToString(b)).append('|');
                out.append(Base64.getMimeEncoder(4,new byte[0]).encodeToString(b)).append('|');
                out.append(lines(Base64.getMimeEncoder(16,ascii("\\n")).encodeToString(b))).append('|');
                out.append(Base64.getMimeEncoder(12,new byte[]{(byte)0xc3,0}).encodeToString(ascii("foobarfoobar")).replace('\\0','0')).append('|');
                out.append(Arrays.toString(Base64.getMimeEncoder(4,new byte[]{-1}).encode(ascii("foob"))));
                return out.toString();
            }
            public static String mimeEncodeIntoArray(){
                byte[] dst=new byte[16];Arrays.fill(dst,(byte)'.');
                int n=Base64.getMimeEncoder(4,ascii("-")).encode(ascii("foobar"),dst);
                return n+":"+text(dst)+"|"+t(()->Base64.getMimeEncoder(4,ascii("-")).encode(ascii("foobar"),new byte[8]))
                        +"|"+Base64.getMimeEncoder(4,ascii("--")).encode(ascii("foobarf"),new byte[16]);
            }
            public static String mimeEncoderFailures(){
                StringBuilder out=new StringBuilder();
                out.append(t(()->Base64.getMimeEncoder(76,ascii("\\r\\nA")))).append('|');
                out.append(t(()->Base64.getMimeEncoder(76,ascii("=")))).append('|');
                out.append(t(()->Base64.getMimeEncoder(76,ascii("+")))).append('|');
                out.append(t(()->Base64.getMimeEncoder(76,ascii("-_")).encodeToString(ascii("foo")))).append('|');
                out.append(t(()->Base64.getMimeEncoder(0,ascii("/")))).append('|');
                out.append(t(()->Base64.getMimeEncoder(76,null))).append('|');
                out.append(t(()->Base64.getMimeEncoder(-1,null)));
                return out.toString();
            }

            public static String basicDecode(){
                StringBuilder out=new StringBuilder();
                for(String s:new String[]{"","Zg==","Zm8=","Zm9v","Zm9vYg==","Zm9vYmE=","Zm9vYmFy","Zg","Zm8","Zm9vYg"})out.append(text(Base64.getDecoder().decode(s))).append('|');
                out.append(Arrays.toString(Base64.getDecoder().decode("QR=="))).append(Arrays.toString(Base64.getDecoder().decode("//79"))).append('|');
                out.append(Arrays.toString(Base64.getDecoder().decode(ascii("+/+/"))));
                return out.toString();
            }
            public static String urlDecode(){
                return Arrays.toString(Base64.getUrlDecoder().decode("-_-_"))+"|"+Arrays.toString(Base64.getUrlDecoder().decode("-_8"))+"|"+text(Base64.getUrlDecoder().decode("Zm9vYmE="))
                        +"|"+t(()->Base64.getUrlDecoder().decode("+/+/"))+"|"+t(()->Base64.getDecoder().decode("-_-_"))+"|"+t(()->Base64.getUrlDecoder().decode("ab/c"))
                        +"|"+(Base64.getDecoder()==Base64.getDecoder())+(Base64.getUrlDecoder()==Base64.getUrlDecoder())+(Base64.getMimeDecoder()==Base64.getMimeDecoder());
            }
            public static String decodeIntoArray(){
                StringBuilder out=new StringBuilder();
                byte[] dst=new byte[8];Arrays.fill(dst,(byte)'.');
                out.append(Base64.getDecoder().decode(ascii("Zm9vYmE="),dst)).append(':').append(text(dst)).append('|');
                byte[] two=new byte[2];
                out.append(Base64.getDecoder().decode(ascii("Zm8"),two)).append(':').append(text(two)).append('|');
                out.append(Base64.getDecoder().decode(new byte[0],new byte[0])).append('|');
                out.append(t(()->Base64.getDecoder().decode(ascii("Zm9v"),new byte[2]))).append('|');
                out.append(t(()->Base64.getDecoder().decode(ascii("Zm9vYg=="),new byte[3]))).append('|');
                out.append(t(()->Base64.getDecoder().decode(ascii("Zm9vYg"),new byte[3]))).append('|');
                out.append(t(()->Base64.getDecoder().decode((byte[])null))).append('|');
                out.append(t(()->Base64.getDecoder().decode((String)null)));
                return out.toString();
            }
            public static String roundTrip(){
                byte[] all=new byte[256];for(int i=0;i<256;i++)all[i]=(byte)(255-i);
                StringBuilder out=new StringBuilder();
                Base64.Encoder[] es={Base64.getEncoder(),Base64.getUrlEncoder(),Base64.getMimeEncoder(),Base64.getEncoder().withoutPadding(),Base64.getMimeEncoder(20,ascii("\\n"))};
                Base64.Decoder[] ds={Base64.getDecoder(),Base64.getUrlDecoder(),Base64.getMimeDecoder(),Base64.getDecoder(),Base64.getMimeDecoder()};
                for(int i=0;i<es.length;i++){
                    for(int n=0;n<=7;n++){
                        byte[] src=Arrays.copyOf(all,256-n);
                        String s=es[i].encodeToString(src);
                        if(!Arrays.equals(src,ds[i].decode(s)))out.append("bad ").append(i).append(',').append(n).append(';');
                    }
                    out.append(es[i].encodeToString(all).length()).append(',');
                }
                return out.toString();
            }

            public static String decodeFailures(){
                StringBuilder out=new StringBuilder();
                for(String s:new String[]{"Q","=","QQ=","====","Q===","QUJDR","QQ=x","QUJD=","QQ==QQ","QQ== ","Zm8==","QU JD","QUJD\\n","QU*D","QUI="})
                    out.append(lines(s)).append(" -> ").append(t(()->Base64.getDecoder().decode(s))).append('\\n');
                return out.toString();
            }
            public static String highBitFailures(){
                StringBuilder out=new StringBuilder();
                out.append(t(()->Base64.getDecoder().decode(new byte[]{(byte)0xc3,'Q','Q','Q'}))).append('|');
                out.append(t(()->Base64.getDecoder().decode("QQ\\u00e9="))).append('|');
                out.append(t(()->Base64.getDecoder().decode("\\u20acQQQ"))).append('|');
                out.append(t(()->Base64.getDecoder().decode(new byte[]{'Q','Q','Q',(byte)0x80})));
                return out.toString();
            }

            public static String mimeDecode(){
                StringBuilder out=new StringBuilder();
                for(String s:new String[]{"Zm9v\\r\\nYmFy","Zm9v!!Ym*Fy","Zm8=\\r\\n","Zm8==","Zm9v Ym E","Zm-9v_","Zm\\u00e99v","!","","\\r\\n","Zm9vYg","QR=="})
                    out.append(text(Base64.getMimeDecoder().decode(s))).append('|');
                out.append(Arrays.toString(Base64.getMimeDecoder().decode(new byte[]{'Z',(byte)0x80,'m','9',(byte)0xff,'v'})));
                byte[] dst=new byte[7];Arrays.fill(dst,(byte)'.');
                out.append('|').append(Base64.getMimeDecoder().decode(ascii("Zm9v\\r\\nYmFy"),dst)).append(':').append(text(dst));
                out.append('|').append(t(()->Base64.getMimeDecoder().decode(ascii("Zm9v\\r\\nYmFy"),new byte[5])));
                return out.toString();
            }
            public static String mimeDecodeFailures(){
                StringBuilder out=new StringBuilder();
                for(String s:new String[]{"Z","Zm8=Zg","Zm8=xyz","Zm=9","=Zm9v","Zm9vY","Zm9vY\\r\\n","Zm9v=","Zm8= =="})
                    out.append(lines(s)).append(" -> ").append(t(()->Base64.getMimeDecoder().decode(s))).append('\\n');
                out.append(t(()->Base64.getMimeDecoder().decode((String)null)));
                return out.toString();
            }
            public static String trailingAlphabetByte(){
                StringBuilder out=new StringBuilder();
                for(String s:new String[]{"Zm8=Z","NQ==S","Zm8= Z","Zm8=\\r\\nQ","UOJWzck=W","Zm8=\\r\\n","Zm8=!*"})
                    out.append(lines(s)).append(" -> ").append(t(()->Base64.getMimeDecoder().decode(s)))
                       .append(" / ").append(t(()->Base64.getMimeDecoder().decode(ascii(s),new byte[4]))).append('\\n');
                for(String s:new String[]{"Zm8=Z","Zm8= ","NQ==S"})
                    out.append(s).append(" -> ").append(t(()->Base64.getDecoder().decode(s))).append(',').append(t(()->Base64.getUrlDecoder().decode(s))).append('\\n');
                return out.toString();
            }
            public static String mimeExactDst(){
                StringBuilder out=new StringBuilder();
                for(String s:new String[]{"Zm\\r\\n8=","Q\\r\\nQ==","Zm9v\\r\\nYg==","Z m 9 v Y m E =","Zm9v\\r\\nYmFy"}){
                    byte[] dst=new byte[Base64.getMimeDecoder().decode(s).length];
                    out.append(lines(s)).append(" -> ").append(t(()->Base64.getMimeDecoder().decode(ascii(s),dst))).append(':').append(text(dst)).append('\\n');
                }
                out.append(t(()->Base64.getMimeDecoder().decode(ascii("\\u20acY="),new byte[0]))).append('|');
                out.append(t(()->Base64.getMimeDecoder().decode(ascii("Zm\\r\\n8="),new byte[1])));
                return out.toString();
            }
            public static String classFlags(){
                return ((Base64.class.getModifiers()&16)!=0)+","+((Base64.Encoder.class.getModifiers()&16)!=0)+","+((Base64.Decoder.class.getModifiers()&16)!=0);
            }
        }
        """;

    @Test void encodesBasicAndUrl() throws Exception {
        matchesJvm(FIXTURE, "basicEncode", "urlEncode", "withoutPadding", "encodeIntoArray");
    }

    @Test void encodesMime() throws Exception {
        matchesJvm(FIXTURE, "mimeEncode", "customMime", "mimeEncodeIntoArray", "mimeEncoderFailures");
    }

    @Test void decodesBasicAndUrl() throws Exception {
        matchesJvm(FIXTURE, "basicDecode", "urlDecode", "decodeIntoArray", "roundTrip");
    }

    @Test void reportsDecodeFailures() throws Exception {
        matchesJvm(FIXTURE, "decodeFailures", "highBitFailures");
    }

    @Test void decodesMime() throws Exception {
        matchesJvm(FIXTURE, "mimeDecode", "mimeDecodeFailures");
    }

    @Test void matchesJdkMimeTailSizingAndClassFlags() throws Exception {
        matchesJvm(FIXTURE, "trailingAlphabetByte", "mimeExactDst", "classFlags");
    }

    @Test void leavesStreamsAndByteBuffersUnlinked() {
        rejectsUnusedCall("java/util/Base64$Encoder", "wrap", "(Ljava/io/OutputStream;)Ljava/io/OutputStream;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/Base64$Decoder", "wrap", "(Ljava/io/InputStream;)Ljava/io/InputStream;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/Base64$Encoder", "encode", "(Ljava/nio/ByteBuffer;)Ljava/nio/ByteBuffer;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/Base64$Decoder", "decode", "(Ljava/nio/ByteBuffer;)Ljava/nio/ByteBuffer;", INVOKEVIRTUAL, false);
    }
}
