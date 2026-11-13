package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** T13: java.security.MessageDigest with SHA-256. Expected values come from the JVM; see tasks/T13-message-digest.md. */
@Tag("agent-pending")
class BytecodeMessageDigestTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.nio.charset.StandardCharsets;
        import java.security.DigestException;
        import java.security.GeneralSecurityException;
        import java.security.MessageDigest;
        import java.security.NoSuchAlgorithmException;

        public class Fixture {
            static String hex(byte[] bytes){
                String digits="0123456789abcdef";StringBuilder out=new StringBuilder();
                for(byte b:bytes)out.append(digits.charAt((b>>4)&15)).append(digits.charAt(b&15));
                return out.toString();
            }
            static byte[] ascii(String s){return s.getBytes(StandardCharsets.UTF_8);}
            static String sha(String s) throws NoSuchAlgorithmException{return hex(MessageDigest.getInstance("SHA-256").digest(ascii(s)));}
            static String name(Throwable t){return t.getClass().getSimpleName();}

            public static String emptyAndAbc(){
                try{return sha("")+"|"+sha("abc");}catch(NoSuchAlgorithmException e){return name(e);}
            }
            public static String nistTwoBlock(){
                try{return sha("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq");}catch(NoSuchAlgorithmException e){return name(e);}
            }
            public static String nistLong(){
                try{
                    String block="abcdefghbcdefghicdefghijdefghijkefghijklfghijklmghijklmnhijklmnoijklmnopjklmnopqklmnopqrlmnopqrsmnopqrstnopqrstu";
                    return sha(block);
                }catch(NoSuchAlgorithmException e){return name(e);}
            }
            public static String millionA(){
                try{
                    MessageDigest md=MessageDigest.getInstance("SHA-256");
                    byte[] chunk=new byte[1000];java.util.Arrays.fill(chunk,(byte)'a');
                    for(int i=0;i<1000;i++)md.update(chunk);
                    return hex(md.digest());
                }catch(NoSuchAlgorithmException e){return name(e);}
            }
            public static String singleByteUpdates(){
                try{
                    MessageDigest md=MessageDigest.getInstance("SHA-256");
                    for(int i=0;i<100000;i++)md.update((byte)'a');
                    return hex(md.digest());
                }catch(NoSuchAlgorithmException e){return name(e);}
            }
            public static String paddingBoundaries(){
                try{
                    StringBuilder out=new StringBuilder();
                    for(int n:new int[]{1,55,56,57,63,64,65,119,120,128}){
                        byte[] data=new byte[n];for(int i=0;i<n;i++)data[i]=(byte)(i*7+n);
                        out.append(n).append(':').append(hex(MessageDigest.getInstance("SHA-256").digest(data)).substring(0,16)).append(';');
                    }
                    return out.toString();
                }catch(NoSuchAlgorithmException e){return name(e);}
            }
            public static String multiPartUpdates(){
                try{
                    byte[] data=new byte[300];for(int i=0;i<data.length;i++)data[i]=(byte)(i*31+5);
                    String whole=hex(MessageDigest.getInstance("SHA-256").digest(data));
                    StringBuilder out=new StringBuilder();
                    for(int split:new int[]{0,1,63,64,65,127,200,299,300}){
                        MessageDigest md=MessageDigest.getInstance("SHA-256");
                        md.update(data,0,split);md.update(data,split,data.length-split);
                        out.append(whole.equals(hex(md.digest()))?'1':'0');
                    }
                    MessageDigest mixed=MessageDigest.getInstance("SHA-256");
                    mixed.update(data[0]);mixed.update(data,1,10);mixed.update(java.util.Arrays.copyOfRange(data,11,150));
                    for(int i=150;i<300;i++)mixed.update(data[i]);
                    out.append(whole.equals(hex(mixed.digest()))?'1':'0');
                    MessageDigest zero=MessageDigest.getInstance("SHA-256");zero.update(data,5,0);zero.update(new byte[0]);
                    out.append('|').append(hex(zero.digest()).substring(0,8));
                    return out.toString();
                }catch(NoSuchAlgorithmException e){return name(e);}
            }
            public static String resetAndReuse(){
                try{
                    MessageDigest md=MessageDigest.getInstance("SHA-256");
                    md.update(ascii("garbage that must be forgotten"));md.reset();md.update(ascii("abc"));
                    String first=hex(md.digest());
                    md.update(ascii("abc"));
                    String second=hex(md.digest());
                    String third=hex(md.digest());
                    md.reset();md.reset();
                    return first.substring(0,8)+","+first.equals(second)+","+third.substring(0,8)+","+hex(md.digest()).substring(0,8);
                }catch(NoSuchAlgorithmException e){return name(e);}
            }
            public static String digestWithInput(){
                try{
                    MessageDigest a=MessageDigest.getInstance("SHA-256"),b=MessageDigest.getInstance("SHA-256");
                    a.update(ascii("hello "));
                    byte[] viaDigest=a.digest(ascii("world"));
                    b.update(ascii("hello world"));
                    return MessageDigest.isEqual(viaDigest,b.digest())+","+viaDigest.length;
                }catch(NoSuchAlgorithmException e){return name(e);}
            }
            public static String digestIntoBuffer(){
                StringBuilder out=new StringBuilder();
                try{
                    MessageDigest md=MessageDigest.getInstance("SHA-256");
                    byte[] buffer=new byte[40];java.util.Arrays.fill(buffer,(byte)9);
                    md.update(ascii("abc"));
                    out.append(md.digest(buffer,4,32)).append(',').append(buffer[0]).append(',').append(buffer[36]).append(',').append(hex(java.util.Arrays.copyOfRange(buffer,4,12))).append(';');
                    out.append(md.digest(buffer,0,40)).append(',').append(hex(java.util.Arrays.copyOfRange(buffer,0,4))).append(';');
                    try{md.digest(buffer,0,31);}catch(DigestException e){out.append(name(e)).append(':').append(e.getMessage()).append(';');}
                    try{md.digest(buffer,20,32);}catch(IllegalArgumentException e){out.append(name(e)).append(':').append(e.getMessage()).append(';');}
                    try{md.digest(null,0,32);}catch(IllegalArgumentException e){out.append(name(e)).append(':').append(e.getMessage()).append(';');}
                }catch(GeneralSecurityException e){out.append("unexpected ").append(name(e));}
                return out.toString();
            }
            public static String updateFailures(){
                StringBuilder out=new StringBuilder();
                try{
                    MessageDigest md=MessageDigest.getInstance("SHA-256");
                    try{md.update(null,0,1);}catch(IllegalArgumentException e){out.append(name(e)).append(':').append(e.getMessage()).append(';');}
                    try{md.update(new byte[4],2,3);}catch(IllegalArgumentException e){out.append(name(e)).append(':').append(e.getMessage()).append(';');}
                    try{md.update(new byte[4],-1,2);}catch(RuntimeException e){out.append(name(e)).append(';');}
                    try{md.update((byte[])null);}catch(NullPointerException e){out.append(name(e)).append(';');}
                    md.reset();
                    out.append(hex(md.digest()).substring(0,8));
                }catch(NoSuchAlgorithmException e){out.append("unexpected");}
                return out.toString();
            }
            public static String names(){
                StringBuilder out=new StringBuilder();
                for(String n:new String[]{"SHA-256","sha-256","Sha-256","SHA256","sha256","2.16.840.1.101.3.4.2.1","OID.2.16.840.1.101.3.4.2.1","oid.2.16.840.1.101.3.4.2.1"}){
                    try{MessageDigest md=MessageDigest.getInstance(n);out.append(md.getAlgorithm()).append('/').append(md.getDigestLength()).append('/').append(hex(md.digest(ascii("abc"))).substring(0,8)).append(';');}
                    catch(NoSuchAlgorithmException e){out.append(n).append(':').append(name(e)).append(';');}
                }
                return out.toString();
            }
            public static String unknownNames(){
                StringBuilder out=new StringBuilder();
                for(String n:new String[]{"FOO","SHA-257","sha-2","","SHA 256"}){
                    try{MessageDigest.getInstance(n);out.append("found ").append(n).append(';');}
                    catch(NoSuchAlgorithmException e){out.append(name(e)).append(':').append(e.getMessage()).append(';');}
                }
                try{MessageDigest.getInstance(null);}catch(NullPointerException e){out.append(name(e)).append(':').append(e.getMessage()).append(';');}catch(NoSuchAlgorithmException e){out.append("wrong");}
                return out.toString();
            }
            public static String exceptionTypes(){
                GeneralSecurityException a=new NoSuchAlgorithmException("x");
                GeneralSecurityException b=new DigestException("y",new IllegalStateException("z"));
                Exception c=new GeneralSecurityException(new IllegalArgumentException("w"));
                return (a instanceof Exception)+","+a.getMessage()+","+b.getMessage()+","+b.getCause().getMessage()+","+c.getMessage()+","+new NoSuchAlgorithmException().getMessage()
                        +","+new DigestException(new IllegalStateException("q")).getMessage()+","+new NoSuchAlgorithmException("m",null).getCause();
            }
            public static String isEqualRules(){
                byte[] x={1,2,3},y={1,2,3},z={1,2,4},shorter={1,2},empty={};
                return MessageDigest.isEqual(x,y)+","+MessageDigest.isEqual(x,z)+","+MessageDigest.isEqual(x,shorter)+","+MessageDigest.isEqual(shorter,x)
                        +","+MessageDigest.isEqual(empty,new byte[0])+","+MessageDigest.isEqual(x,empty)+","+MessageDigest.isEqual(null,null)+","+MessageDigest.isEqual(x,null)+","+MessageDigest.isEqual(null,empty);
            }
            public static String cloneInProgress(){
                try{
                    MessageDigest md=MessageDigest.getInstance("SHA-256");
                    md.update(ascii("ab"));
                    MessageDigest copy=(MessageDigest)md.clone();
                    md.update(ascii("c"));copy.update(ascii("c"));
                    String a=hex(md.digest()),b=hex(copy.digest());
                    copy.update(ascii("x"));
                    return a.equals(b)+","+copy.getAlgorithm()+","+a.substring(0,8)+","+hex(md.digest()).substring(0,8)+","+hex(copy.digest()).substring(0,8);
                }catch(GeneralSecurityException|CloneNotSupportedException e){return name(e);}
            }
            public static String describe(){
                try{
                    MessageDigest md=MessageDigest.getInstance("sha-256");
                    String before=md.toString();md.update((byte)1);String during=md.toString();md.digest();
                    return before+"#"+during+"#"+md;
                }catch(NoSuchAlgorithmException e){return name(e);}
            }
            // A user subclass, the way H2's SHA3 extends MessageDigest: protected constructor plus engine methods.
            static final class Xor extends MessageDigest {
                private int value;
                Xor(){super("XOR8");}
                protected void engineUpdate(byte input){value^=input&0xff;}
                protected void engineUpdate(byte[] input,int offset,int len){for(int i=offset;i<offset+len;i++)value^=input[i]&0xff;}
                protected byte[] engineDigest(){byte[] out={(byte)value};value=0;return out;}
                protected void engineReset(){value=0;}
                protected int engineGetDigestLength(){return 1;}
            }
            public static String userSubclass(){
                MessageDigest md=new Xor();
                md.update((byte)0x0f);md.update(new byte[]{(byte)0xf0,1},0,2);
                byte[] first=md.digest();
                md.update((byte)5);md.reset();
                String cloneResult;
                try{md.clone();cloneResult="cloned";}catch(CloneNotSupportedException e){cloneResult=name(e);}
                return md.getAlgorithm()+","+md.getDigestLength()+","+hex(first)+","+hex(md.digest(new byte[]{3,4}))+","+cloneResult+","+md.toString().trim();
            }
            // digest(buf,off,len) on a user subclass uses MessageDigestSpi's default engineDigest(buf,off,len).
            public static String userDigestIntoBuffer(){
                StringBuilder out=new StringBuilder();
                MessageDigest md=new Xor();
                byte[] buffer=new byte[4];
                md.update((byte)0x5a);
                try{out.append(md.digest(buffer,1,3)).append(',').append(hex(buffer)).append(';');}catch(DigestException e){out.append("unexpected ").append(name(e)).append(';');}
                md.update((byte)7);
                try{md.digest(buffer,0,0);out.append("no exception;");}catch(DigestException e){out.append(name(e)).append(':').append(e.getMessage()).append(';');}
                out.append(hex(md.digest())).append(';');
                try{md.digest(buffer,3,2);}catch(IllegalArgumentException e){out.append(name(e)).append(':').append(e.getMessage()).append(';');}catch(DigestException e){out.append("wrong");}
                return out.toString();
            }
            // Subclasses that do not override engineGetDigestLength: getDigestLength() falls back to clone().digest().length.
            static class Sum extends MessageDigest {
                int sum;
                Sum(String name){super(name);}
                protected void engineUpdate(byte input){sum+=input&0xff;}
                protected void engineUpdate(byte[] input,int offset,int len){for(int i=offset;i<offset+len;i++)sum+=input[i]&0xff;}
                protected byte[] engineDigest(){byte[] out={(byte)(sum>>8),(byte)sum};sum=0;return out;}
                protected void engineReset(){sum=0;}
            }
            static final class CloneableSum extends Sum implements Cloneable { CloneableSum(){super("SUM16C");} }
            public static String digestLengthFallback(){
                Sum plain=new Sum("SUM16");
                CloneableSum cloneable=new CloneableSum();
                plain.update((byte)200);cloneable.update((byte)200);cloneable.update((byte)100);
                int a=plain.getDigestLength(),b=cloneable.getDigestLength();
                String c;try{plain.clone();c="cloned";}catch(CloneNotSupportedException e){c=name(e);}
                return a+","+b+","+hex(plain.digest())+","+hex(cloneable.digest())+","+c+","+plain.toString().trim()+","+cloneable.toString().trim();
            }
        }
        """;

    @Test void sha256KnownAnswers() throws Exception {
        matchesJvm(FIXTURE, "emptyAndAbc", "nistTwoBlock", "nistLong", "millionA", "singleByteUpdates");
    }

    @Test void blockBoundariesMultiPartAndReset() throws Exception {
        matchesJvm(FIXTURE, "paddingBoundaries", "multiPartUpdates", "resetAndReuse", "digestWithInput");
    }

    @Test void outputBuffersFailuresAndExceptions() throws Exception {
        matchesJvm(FIXTURE, "digestIntoBuffer", "updateFailures", "isEqualRules", "exceptionTypes");
    }

    @Test void namesCloneToStringAndSubclasses() throws Exception {
        matchesJvm(FIXTURE, "names", "unknownNames", "cloneInProgress", "describe", "userSubclass");
    }

    @Test void userSubclassDefaults() throws Exception {
        matchesJvm(FIXTURE, "userDigestIntoBuffer", "digestLengthFallback");
    }

    @Test void leavesProviderAndByteBufferMembersUnlinked() {
        rejectsUnusedCall("java/security/MessageDigest", "getInstance", "(Ljava/lang/String;Ljava/lang/String;)Ljava/security/MessageDigest;", INVOKESTATIC, false);
        rejectsUnusedCall("java/security/MessageDigest", "getProvider", "()Ljava/security/Provider;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/security/MessageDigest", "update", "(Ljava/nio/ByteBuffer;)V", INVOKEVIRTUAL, false);
    }
}
