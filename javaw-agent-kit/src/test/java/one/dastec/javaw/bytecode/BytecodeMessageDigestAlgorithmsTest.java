package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * T14: MessageDigest MD5, SHA-1, SHA-512 and SHA-384. Expected values come from the JDK 21 JVM (the kit's oracle);
 * see tasks/T14-message-digest-algorithms.md. The MD5 OID aliases are not checked: JDK 17 rejects them, JDK 21 accepts them.
 */
@Tag("agent-pending")
class BytecodeMessageDigestAlgorithmsTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.nio.charset.StandardCharsets;
        import java.security.DigestException;
        import java.security.MessageDigest;
        import java.security.NoSuchAlgorithmException;

        public class Fixture {
            static String hex(byte[] bytes){
                String digits="0123456789abcdef";StringBuilder out=new StringBuilder();
                for(byte b:bytes)out.append(digits.charAt((b>>4)&15)).append(digits.charAt(b&15));
                return out.toString();
            }
            static byte[] ascii(String s){return s.getBytes(StandardCharsets.UTF_8);}
            static String name(Throwable t){return t.getClass().getSimpleName();}
            static String digest(String algorithm,String text){
                try{return hex(MessageDigest.getInstance(algorithm).digest(ascii(text)));}catch(NoSuchAlgorithmException e){return algorithm+":"+name(e);}
            }
            static final String TWO_BLOCK="abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq";
            static final String LONG="abcdefghbcdefghicdefghijdefghijkefghijklfghijklmghijklmnhijklmnoijklmnopjklmnopqklmnopqrlmnopqrsmnopqrstnopqrstu";
            static String million(String algorithm){
                try{
                    MessageDigest md=MessageDigest.getInstance(algorithm);
                    byte[] chunk=new byte[1000];java.util.Arrays.fill(chunk,(byte)'a');
                    for(int i=0;i<1000;i++)md.update(chunk);
                    return hex(md.digest());
                }catch(NoSuchAlgorithmException e){return name(e);}
            }
            static String boundaries(String algorithm,int[] lengths){
                try{
                    StringBuilder out=new StringBuilder();
                    for(int n:lengths){
                        byte[] data=new byte[n];for(int i=0;i<n;i++)data[i]=(byte)(i*7+n);
                        out.append(n).append(':').append(hex(MessageDigest.getInstance(algorithm).digest(data)).substring(0,12)).append(';');
                    }
                    return out.toString();
                }catch(NoSuchAlgorithmException e){return name(e);}
            }
            static String multiPart(String algorithm){
                try{
                    byte[] data=new byte[400];for(int i=0;i<data.length;i++)data[i]=(byte)(i*31+5);
                    String whole=hex(MessageDigest.getInstance(algorithm).digest(data));
                    StringBuilder out=new StringBuilder();
                    for(int split:new int[]{0,1,63,64,65,111,112,127,128,129,250,400}){
                        MessageDigest md=MessageDigest.getInstance(algorithm);
                        md.update(data,0,split);md.update(data,split,data.length-split);
                        out.append(whole.equals(hex(md.digest()))?'1':'0');
                    }
                    MessageDigest bytes=MessageDigest.getInstance(algorithm);
                    for(byte b:data)bytes.update(b);
                    out.append(whole.equals(hex(bytes.digest()))?'1':'0');
                    bytes.update(ascii("junk"));bytes.reset();
                    out.append(hex(bytes.digest()).equals(hex(MessageDigest.getInstance(algorithm).digest()))?'1':'0');
                    return out.toString();
                }catch(NoSuchAlgorithmException e){return name(e);}
            }

            public static String md5Vectors(){
                return digest("MD5","")+"|"+digest("MD5","a")+"|"+digest("MD5","abc")+"|"+digest("MD5","message digest")+"|"+digest("MD5","abcdefghijklmnopqrstuvwxyz")
                        +"|"+digest("MD5","ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789")
                        +"|"+digest("MD5","12345678901234567890123456789012345678901234567890123456789012345678901234567890");
            }
            public static String md5Long(){return million("MD5")+"|"+boundaries("MD5",new int[]{55,56,63,64,65,119,120})+"|"+multiPart("MD5");}
            public static String sha1Vectors(){
                return digest("SHA-1","")+"|"+digest("SHA-1","abc")+"|"+digest("SHA-1",TWO_BLOCK)+"|"+digest("SHA-1",LONG);
            }
            public static String sha1Long(){return million("SHA-1")+"|"+boundaries("SHA-1",new int[]{55,56,63,64,65,119,120})+"|"+multiPart("SHA-1");}
            public static String sha512Vectors(){
                return digest("SHA-512","")+"|"+digest("SHA-512","abc")+"|"+digest("SHA-512",TWO_BLOCK)+"|"+digest("SHA-512",LONG);
            }
            public static String sha512Long(){return million("SHA-512")+"|"+boundaries("SHA-512",new int[]{111,112,113,127,128,129,239,240})+"|"+multiPart("SHA-512");}
            public static String sha384Vectors(){
                return digest("SHA-384","")+"|"+digest("SHA-384","abc")+"|"+digest("SHA-384",LONG)+"|"+boundaries("SHA-384",new int[]{111,112,128})+"|"+multiPart("SHA-384");
            }
            public static String names(){
                StringBuilder out=new StringBuilder();
                for(String n:new String[]{"SHA-1","sha-1","SHA1","SHA","sha","1.3.14.3.2.26","OID.1.3.14.3.2.26","MD5","md5","SHA-512","sha512","2.16.840.1.101.3.4.2.3","OID.2.16.840.1.101.3.4.2.3","SHA-384","Sha384","2.16.840.1.101.3.4.2.2","oid.2.16.840.1.101.3.4.2.2","SHA-256"}){
                    try{MessageDigest md=MessageDigest.getInstance(n);out.append(md.getAlgorithm()).append('/').append(md.getDigestLength()).append('/').append(hex(md.digest()).substring(0,6)).append(';');}
                    catch(NoSuchAlgorithmException e){out.append(n).append(':').append(name(e)).append(';');}
                }
                return out.toString();
            }
            public static String unknownNames(){
                StringBuilder out=new StringBuilder();
                for(String n:new String[]{"MD6","SHA-0","SHA-1024","SHA_1"}){
                    try{MessageDigest.getInstance(n);out.append("found ").append(n).append(';');}
                    catch(NoSuchAlgorithmException e){out.append(e.getMessage()).append(';');}
                }
                return out.toString();
            }
            public static String outputBuffers(){
                StringBuilder out=new StringBuilder();
                for(String n:new String[]{"MD5","SHA-1","SHA-512","SHA-384"}){
                    try{
                        MessageDigest md=MessageDigest.getInstance(n);
                        byte[] buffer=new byte[70];md.update(ascii("abc"));
                        out.append(md.digest(buffer,2,68)).append(',').append(hex(java.util.Arrays.copyOfRange(buffer,0,6))).append(',');
                        try{md.digest(buffer,0,md.getDigestLength()-1);}catch(DigestException e){out.append(e.getMessage());}
                        out.append(';');
                    }catch(Exception e){out.append(name(e)).append(';');}
                }
                return out.toString();
            }
            public static String clones(){
                StringBuilder out=new StringBuilder();
                for(String n:new String[]{"MD5","SHA-1","SHA-512","SHA-384"}){
                    try{
                        MessageDigest md=MessageDigest.getInstance(n);
                        md.update(ascii("The quick brown fox jumps over the lazy dog, and then over the lazy dog again, and again."));
                        MessageDigest copy=(MessageDigest)md.clone();
                        md.update(ascii("!"));copy.update(ascii("!"));
                        out.append(hex(md.digest()).equals(hex(copy.digest()))).append(',').append(copy.getAlgorithm()).append(';');
                    }catch(Exception e){out.append(name(e)).append(';');}
                }
                return out.toString();
            }
        }
        """;

    @Test void md5() throws Exception {
        matchesJvm(FIXTURE, "md5Vectors", "md5Long");
    }

    @Test void sha1() throws Exception {
        matchesJvm(FIXTURE, "sha1Vectors", "sha1Long");
    }

    @Test void sha512AndSha384() throws Exception {
        matchesJvm(FIXTURE, "sha512Vectors", "sha512Long", "sha384Vectors");
    }

    @Test void namesBuffersAndClones() throws Exception {
        matchesJvm(FIXTURE, "names", "unknownNames", "outputBuffers", "clones");
    }
}
