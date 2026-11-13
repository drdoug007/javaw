package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** T15: javax.crypto.Mac (HMAC) and SecretKeySpec. Expected values come from the JVM; see tasks/T15-hmac.md. */
@Tag("agent-pending")
class BytecodeHmacTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.nio.charset.StandardCharsets;
        import java.security.GeneralSecurityException;
        import java.security.InvalidKeyException;
        import java.security.Key;
        import java.security.KeyException;
        import java.security.NoSuchAlgorithmException;
        import java.util.Arrays;
        import java.util.HexFormat;
        import javax.crypto.Mac;
        import javax.crypto.SecretKey;
        import javax.crypto.spec.SecretKeySpec;
        import javax.security.auth.DestroyFailedException;

        public class Fixture {
            static byte[] utf8(String s){return s.getBytes(StandardCharsets.UTF_8);}
            static String name(Throwable t){return t.getClass().getSimpleName();}
            static String hmac(String algorithm,byte[] key,byte[] data){
                try{
                    Mac mac=Mac.getInstance(algorithm);
                    mac.init(new SecretKeySpec(key,algorithm));
                    return HexFormat.of().formatHex(mac.doFinal(data));
                }catch(GeneralSecurityException e){return name(e)+":"+e.getMessage();}
            }
            static byte[] filled(int length,int value){byte[] b=new byte[length];Arrays.fill(b,(byte)value);return b;}

            // The user's javaw-core JavaCryptoTest fixture (RFC 4231 test case 1), unchanged apart from catching exceptions.
            public static int hmacSHA256(){
                try{
                    byte[] k=new byte[20];
                    Arrays.fill(k,(byte)0x0b);
                    var mac=Mac.getInstance("HmacSHA256");
                    mac.init(new SecretKeySpec(k,"HmacSHA256"));
                    byte[] r=mac.doFinal("Hi There".getBytes(StandardCharsets.UTF_8));
                    String hex=HexFormat.of().formatHex(r);
                    return hex.equals("b0344c61d8db38535ca8afceaf0bf12b881dc200c9833da726e9376c2e32cff7")?1:0;
                }catch(Exception e){return -1;}
            }
            public static String rfc4231Case1(){
                byte[] key=filled(20,0x0b),data=utf8("Hi There");
                return hmac("HmacSHA256",key,data)+"|"+hmac("HmacSHA512",key,data);
            }
            public static String rfc4231Case2(){
                byte[] key=utf8("Jefe"),data=utf8("what do ya want for nothing?");
                return hmac("HmacSHA256",key,data)+"|"+hmac("HmacSHA512",key,data);
            }
            public static String rfc4231Case6(){
                byte[] key=filled(131,0xaa),data=utf8("Test Using Larger Than Block-Size Key - Hash Key First");
                return hmac("HmacSHA256",key,data)+"|"+hmac("HmacSHA512",key,data);
            }
            public static String otherAlgorithms(){
                byte[] key=utf8("key"),data=utf8("The quick brown fox jumps over the lazy dog");
                return hmac("HmacSHA1",key,data)+"|"+hmac("HmacMD5",key,data)+"|"+hmac("HmacSHA384",key,data)+"|"+hmac("HmacMD5",filled(100,0x11),data)+"|"+hmac("HmacSHA1",filled(64,0x22),data)+"|"+hmac("HmacSHA1",filled(65,0x22),data);
            }
            public static String streamingAndReuse(){
                try{
                    Mac mac=Mac.getInstance("HmacSHA256");
                    mac.init(new SecretKeySpec(utf8("secret"),"HmacSHA256"));
                    byte[] data=new byte[200];for(int i=0;i<data.length;i++)data[i]=(byte)(i*13);
                    String whole=HexFormat.of().formatHex(mac.doFinal(data));
                    mac.update(data[0]);mac.update(data,1,99);mac.update(Arrays.copyOfRange(data,100,200));
                    String parts=HexFormat.of().formatHex(mac.doFinal());
                    mac.update(utf8("garbage"));mac.reset();
                    String afterReset=HexFormat.of().formatHex(mac.doFinal(data));
                    String empty=HexFormat.of().formatHex(mac.doFinal());
                    mac.update((byte[])null);
                    String afterNull=HexFormat.of().formatHex(mac.doFinal());
                    mac.init(new SecretKeySpec(utf8("other"),"AES"));
                    String rekeyed=HexFormat.of().formatHex(mac.doFinal(data)).substring(0,16);
                    return whole.equals(parts)+","+whole.equals(afterReset)+","+empty.equals(afterNull)+","+empty.substring(0,16)+","+rekeyed;
                }catch(GeneralSecurityException e){return name(e);}
            }
            public static String names(){
                StringBuilder out=new StringBuilder();
                for(String n:new String[]{"HmacSHA256","hmacsha256","HMACSHA1","HmacMD5","HmacSHA512","HmacSHA384"}){
                    try{Mac mac=Mac.getInstance(n);out.append(mac.getAlgorithm()).append('/').append(mac.getMacLength()).append(';');}
                    catch(NoSuchAlgorithmException e){out.append(n).append(':').append(name(e)).append(';');}
                }
                for(String n:new String[]{"HmacFOO","SHA-256","Hmac-SHA256",""}){
                    try{Mac.getInstance(n);out.append("found ").append(n).append(';');}
                    catch(NoSuchAlgorithmException e){out.append(name(e)).append(':').append(e.getMessage()).append(';');}
                }
                try{Mac.getInstance(null);}catch(NullPointerException e){out.append(name(e)).append(':').append(e.getMessage());}catch(NoSuchAlgorithmException e){out.append("wrong");}
                return out.toString();
            }
            public static String notInitialized(){
                StringBuilder out=new StringBuilder();
                try{
                    Mac mac=Mac.getInstance("HmacSHA256");
                    try{mac.update((byte)1);}catch(IllegalStateException e){out.append(e.getMessage()).append(';');}
                    try{mac.update(new byte[2]);}catch(IllegalStateException e){out.append(e.getMessage()).append(';');}
                    try{mac.update(new byte[2],0,1);}catch(IllegalStateException e){out.append(e.getMessage()).append(';');}
                    try{mac.doFinal();}catch(IllegalStateException e){out.append(e.getMessage()).append(';');}
                    try{mac.doFinal(new byte[2]);}catch(IllegalStateException e){out.append(e.getMessage()).append(';');}
                    mac.reset();
                    out.append(mac.getMacLength());
                }catch(NoSuchAlgorithmException e){out.append(name(e));}
                return out.toString();
            }
            static final class PlainKey implements Key {
                public String getAlgorithm(){return "RAW";}
                public String getFormat(){return "RAW";}
                public byte[] getEncoded(){return new byte[]{1,2,3};}
            }
            static final class NullSecret implements SecretKey {
                public String getAlgorithm(){return "HmacSHA256";}
                public String getFormat(){return "RAW";}
                public byte[] getEncoded(){return null;}
            }
            static final class CustomSecret implements SecretKey {
                public String getAlgorithm(){return "HmacSHA256";}
                public String getFormat(){return "RAW";}
                public byte[] getEncoded(){return utf8("Jefe");}
            }
            static String initFailure(Key key){
                try{Mac.getInstance("HmacSHA256").init(key);return "accepted;";}
                catch(InvalidKeyException e){return name(e)+":"+e.getMessage()+";";}
                catch(NoSuchAlgorithmException e){return name(e)+";";}
            }
            public static String badKeys(){
                return initFailure(null)+initFailure(new PlainKey())+initFailure(new NullSecret());
            }
            public static String customKeyAndBadArguments(){
                StringBuilder out=new StringBuilder();
                try{
                    Mac mac=Mac.getInstance("HmacSHA256");
                    mac.init(new CustomSecret());
                    out.append(HexFormat.of().formatHex(mac.doFinal(utf8("what do ya want for nothing?"))).substring(0,16)).append(';');
                    try{mac.update(new byte[4],3,2);}catch(IllegalArgumentException e){out.append(name(e)).append(':').append(e.getMessage()).append(';');}
                    try{mac.update(new byte[4],-1,1);}catch(IllegalArgumentException e){out.append(name(e)).append(':').append(e.getMessage()).append(';');}
                    try{mac.update(new byte[4],0,-1);}catch(IllegalArgumentException e){out.append(name(e)).append(':').append(e.getMessage()).append(';');}
                    mac.update(null,0,5);
                    out.append(HexFormat.of().formatHex(mac.doFinal()).substring(0,8));
                }catch(GeneralSecurityException e){out.append("unexpected ").append(name(e)).append(':').append(e.getMessage());}
                return out.toString();
            }
            public static String keySpec(){
                byte[] raw={1,2,3,4,5};
                SecretKeySpec key=new SecretKeySpec(raw,"HmacSHA256");
                raw[0]=99;
                byte[] encoded=key.getEncoded();encoded[1]=99;
                SecretKeySpec part=new SecretKeySpec(new byte[]{9,8,7,6,5},1,3,"AES");
                return key.getAlgorithm()+","+key.getFormat()+","+Arrays.toString(key.getEncoded())+","+(key.getEncoded()!=key.getEncoded())+","+Arrays.toString(part.getEncoded())+","+part.getAlgorithm()
                        +","+(key instanceof SecretKey)+","+(key instanceof Key)+","+(key instanceof java.security.spec.KeySpec)+","+(key instanceof java.io.Serializable)+","+(key instanceof javax.security.auth.Destroyable);
            }
            public static String keySpecFailures(){
                StringBuilder out=new StringBuilder();
                try{new SecretKeySpec(null,"AES");}catch(IllegalArgumentException e){out.append(e.getMessage()).append(';');}
                try{new SecretKeySpec(new byte[1],null);}catch(IllegalArgumentException e){out.append(e.getMessage()).append(';');}
                try{new SecretKeySpec(new byte[0],"AES");}catch(IllegalArgumentException e){out.append(e.getMessage()).append(';');}
                try{new SecretKeySpec(null,0,1,"AES");}catch(IllegalArgumentException e){out.append(e.getMessage()).append(';');}
                try{new SecretKeySpec(new byte[0],0,0,"AES");}catch(IllegalArgumentException e){out.append(e.getMessage()).append(';');}
                try{new SecretKeySpec(new byte[3],-1,1,"AES");}catch(ArrayIndexOutOfBoundsException e){out.append(name(e)).append(':').append(e.getMessage()).append(';');}
                try{new SecretKeySpec(new byte[3],0,-1,"AES");}catch(ArrayIndexOutOfBoundsException e){out.append(name(e)).append(':').append(e.getMessage()).append(';');}
                try{new SecretKeySpec(new byte[3],2,2,"AES");}catch(IllegalArgumentException e){out.append(e.getMessage()).append(';');}
                out.append(new SecretKeySpec(new byte[3],1,0,"AES").getEncoded().length);
                return out.toString();
            }
            public static String keyEquality(){
                SecretKeySpec a=new SecretKeySpec(new byte[]{1,2,3,4},"HmacSHA256"),b=new SecretKeySpec(new byte[]{1,2,3,4},"hmacsha256");
                SecretKeySpec c=new SecretKeySpec(new byte[]{1,2,3,5},"HmacSHA256"),d=new SecretKeySpec(new byte[]{1,2,3,4},"AES");
                SecretKeySpec e=new SecretKeySpec(new byte[]{1,2,3,4},"DESede"),f=new SecretKeySpec(new byte[]{1,2,3,4},"TripleDES");
                return a.equals(b)+","+a.equals(c)+","+a.equals(d)+","+e.equals(f)+","+f.equals(e)+","+a.equals(null)+","+a.equals("x")+","+a.equals(new CustomSecret())
                        +","+new SecretKeySpec(utf8("Jefe"),"HmacSHA256").equals(new CustomSecret())+","+(a.hashCode()==b.hashCode())+","+(e.hashCode()==f.hashCode())+","+(a.hashCode()==c.hashCode());
            }
            // Raw SecretKeySpec.hashCode values follow the JDK 21 formula (JDK 24+ changed it).
            public static String keyHashCodes(){
                SecretKeySpec a=new SecretKeySpec(new byte[]{1,2,3,4},"HmacSHA256"),c=new SecretKeySpec(new byte[]{1,2,3,5},"HmacSHA256");
                SecretKeySpec d=new SecretKeySpec(new byte[]{1,2,3,4},"AES"),e=new SecretKeySpec(new byte[]{1,2,3,4},"DESede"),f=new SecretKeySpec(new byte[]{1,2,3,4},"TripleDES");
                return a.hashCode()+","+c.hashCode()+","+d.hashCode()+","+e.hashCode()+","+f.hashCode()+","+new SecretKeySpec(new byte[]{-128,-1,127},"x").hashCode();
            }
            public static String destroyable(){
                SecretKeySpec key=new SecretKeySpec(new byte[]{1},"AES");
                String result=String.valueOf(key.isDestroyed());
                try{key.destroy();result+=",destroyed";}catch(DestroyFailedException e){result+=","+name(e)+":"+e.getMessage();}
                return result+","+key.isDestroyed()+","+new DestroyFailedException("m").getMessage();
            }
            public static String exceptionTypes(){
                GeneralSecurityException a=new InvalidKeyException("bad");
                KeyException b=new InvalidKeyException("why",new IllegalStateException("cause"));
                return (a instanceof KeyException)+","+a.getMessage()+","+b.getCause().getMessage()+","+new KeyException().getMessage()+","+new InvalidKeyException(new IllegalStateException("c")).getMessage()
                        +","+(new DestroyFailedException() instanceof Exception);
            }
        }
        """;

    @Test void rfc4231Vectors() throws Exception {
        matchesJvm(FIXTURE, "hmacSHA256", "rfc4231Case1", "rfc4231Case2", "rfc4231Case6");
    }

    @Test void otherAlgorithmsStreamingAndNames() throws Exception {
        matchesJvm(FIXTURE, "otherAlgorithms", "streamingAndReuse", "names");
    }

    @Test void stateAndKeyChecks() throws Exception {
        matchesJvm(FIXTURE, "notInitialized", "badKeys", "customKeyAndBadArguments");
    }

    @Test void secretKeySpecAndExceptions() throws Exception {
        matchesJvm(FIXTURE, "keySpec", "keySpecFailures", "keyEquality", "keyHashCodes", "destroyable", "exceptionTypes");
    }

    @Test void leavesProviderParameterAndBufferMembersUnlinked() {
        rejectsUnusedCall("javax/crypto/Mac", "getInstance", "(Ljava/lang/String;Ljava/lang/String;)Ljavax/crypto/Mac;", INVOKESTATIC, false);
        rejectsUnusedCall("javax/crypto/Mac", "init", "(Ljava/security/Key;Ljava/security/spec/AlgorithmParameterSpec;)V", INVOKEVIRTUAL, false);
        rejectsUnusedCall("javax/crypto/Mac", "doFinal", "([BI)V", INVOKEVIRTUAL, false);
    }
}
