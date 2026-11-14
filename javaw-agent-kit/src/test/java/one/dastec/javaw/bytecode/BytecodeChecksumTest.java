package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T16: java.util.zip.Checksum, CRC32 and Adler32. Expected values come from the JVM; see tasks/T16-checksums.md. */
class BytecodeChecksumTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.util.zip.Adler32;
        import java.util.zip.CRC32;
        import java.util.zip.Checksum;
        public class Fixture {
            static String name(Throwable t){return t.getClass().getSimpleName();}
            static byte[] ascii(String s){byte[] b=new byte[s.length()];for(int i=0;i<b.length;i++)b[i]=(byte)s.charAt(i);return b;}
            static byte[] noise(int n){byte[] b=new byte[n];int x=12345;for(int i=0;i<n;i++){x=x*1103515245+12345;b[i]=(byte)(x>>>16);}return b;}
            static String crc(String s){CRC32 c=new CRC32();c.update(ascii(s));return Long.toHexString(c.getValue());}
            static String adler(String s){Adler32 a=new Adler32();a.update(ascii(s));return Long.toHexString(a.getValue());}
            static String fail(Checksum c,byte[] b,int off,int len){
                try{c.update(b,off,len);return "ok;";}catch(RuntimeException e){return name(e)+":"+e.getMessage()+";";}
            }

            public static String crcVectors(){
                return crc("")+","+crc("a")+","+crc("abc")+","+crc("123456789")+","+crc("The quick brown fox jumps over the lazy dog")+","+crc("message digest");
            }
            public static String crcAllBytes(){
                byte[] all=new byte[256];for(int i=0;i<256;i++)all[i]=(byte)i;
                CRC32 c=new CRC32();c.update(all);long first=c.getValue();
                CRC32 d=new CRC32();d.update(noise(5000));
                CRC32 z=new CRC32();z.update(new byte[64]);
                return first+","+d.getValue()+","+z.getValue();
            }
            public static String crcUnsignedValue(){
                CRC32 c=new CRC32();c.update(ascii("123456789"));
                long v=c.getValue();
                return v+","+(v>0)+","+(v>Integer.MAX_VALUE)+","+(v>>>32)+","+new CRC32().getValue();
            }
            public static String crcSingleBytes(){
                CRC32 a=new CRC32();for(byte b:ascii("abc"))a.update(b);
                CRC32 b=new CRC32();b.update(0x161);b.update(-158);b.update('c'+0x7f00);
                CRC32 m=new CRC32();m.update(255);long ff=m.getValue();m.reset();m.update(-1);
                return a.getValue()+","+b.getValue()+","+ff+","+m.getValue();
            }
            public static String crcIncremental(){
                byte[] data=noise(300);
                CRC32 whole=new CRC32();whole.update(data);
                CRC32 parts=new CRC32();parts.update(data,0,1);parts.update(data,1,99);parts.update(data,100,0);parts.update(data[100]);parts.update(data,101,199);
                CRC32 ranged=new CRC32();ranged.update(data,10,20);
                CRC32 copy=new CRC32();byte[] slice=new byte[20];System.arraycopy(data,10,slice,0,20);copy.update(slice);
                return (whole.getValue()==parts.getValue())+","+whole.getValue()+","+ranged.getValue()+","+(ranged.getValue()==copy.getValue());
            }
            public static String crcReset(){
                CRC32 c=new CRC32();c.update(ascii("hello"));long before=c.getValue();
                c.reset();long empty=c.getValue();
                c.update(ascii("hello"));long again=c.getValue();
                c.reset();c.reset();
                return before+","+empty+","+(before==again)+","+c.getValue();
            }

            public static String adlerVectors(){
                return adler("")+","+adler("a")+","+adler("abc")+","+adler("Wikipedia")+","+adler("123456789")+","+adler("The quick brown fox jumps over the lazy dog");
            }
            public static String adlerLarge(){
                byte[] ff=new byte[6000];java.util.Arrays.fill(ff,(byte)-1);
                Adler32 a=new Adler32();a.update(ff);
                Adler32 n=new Adler32();n.update(noise(20000));
                Adler32 z=new Adler32();z.update(new byte[70000]);
                Adler32 big=new Adler32();for(int i=0;i<20;i++)big.update(ff);
                return a.getValue()+","+n.getValue()+","+z.getValue()+","+big.getValue();
            }
            public static String adlerSingleBytesAndReset(){
                Adler32 a=new Adler32();long fresh=a.getValue();
                a.update('W'+0x100);a.update(-151);a.update(ascii("kipedia"),0,7);long wiki=a.getValue();
                a.reset();long reset=a.getValue();
                a.update(255);long ff=a.getValue();a.reset();a.update(-1);
                return fresh+","+Long.toHexString(wiki)+","+reset+","+ff+","+a.getValue();
            }
            public static String adlerIncremental(){
                byte[] data=noise(9000);
                Adler32 whole=new Adler32();whole.update(data);
                Adler32 parts=new Adler32();parts.update(data,0,5551);parts.update(data,5551,2);parts.update(data,5553,3447);
                Adler32 ranged=new Adler32();ranged.update(data,3,4);
                return (whole.getValue()==parts.getValue())+","+whole.getValue()+","+ranged.getValue();
            }

            public static String rangeFailures(){
                StringBuilder out=new StringBuilder();
                for(Checksum c:new Checksum[]{new CRC32(),new Adler32()}){
                    byte[] b={1,2,3};
                    out.append(fail(c,b,1,5)).append(fail(c,b,-1,1)).append(fail(c,b,1,-1)).append(fail(c,b,1,Integer.MAX_VALUE))
                       .append(fail(c,b,4,0)).append(fail(c,b,3,0)).append(fail(c,b,0,3)).append(c.getValue()).append('|');
                }
                return out.toString();
            }
            public static String failedUpdateKeepsState(){
                CRC32 c=new CRC32();c.update(ascii("abc"));long before=c.getValue();
                fail(c,new byte[2],1,2);fail(c,new byte[2],-1,0);
                Adler32 a=new Adler32();a.update(ascii("abc"));long abefore=a.getValue();
                fail(a,new byte[2],2,1);
                return (before==c.getValue())+","+(abefore==a.getValue())+","+c.getValue()+","+a.getValue();
            }
            public static String nullArrays(){
                StringBuilder out=new StringBuilder();
                for(Checksum c:new Checksum[]{new CRC32(),new Adler32()}){
                    try{c.update(null,0,0);out.append("ok;");}catch(NullPointerException e){out.append(name(e)).append(':').append(e.getMessage()).append(';');}
                    try{c.update(null,5,-1);out.append("ok;");}catch(RuntimeException e){out.append(name(e)).append(':').append(e.getMessage()).append(';');}
                    try{c.update((byte[])null);out.append("ok;");}catch(NullPointerException e){out.append(name(e)).append(';');}
                    out.append(c.getValue()).append('|');
                }
                return out.toString();
            }

            static final class Recorder implements Checksum {
                final StringBuilder log=new StringBuilder();long sum;
                public void update(int b){log.append("i").append(b).append(' ');sum+=b&0xff;}
                public void update(byte[] b,int off,int len){log.append("r").append(off).append('+').append(len).append(' ');for(int i=off;i<off+len;i++)sum+=b[i]&0xff;}
                public long getValue(){return sum;}
                public void reset(){sum=0;log.append("reset ");}
            }
            public static String defaultUpdateDelegates(){
                Recorder r=new Recorder();
                r.update(new byte[]{1,2,3});r.update(new byte[0]);r.update(7);
                Checksum c=r;c.update(new byte[]{(byte)200});long v=c.getValue();c.reset();
                return r.log+"="+v+","+r.getValue();
            }
            public static String throughInterface(){
                StringBuilder out=new StringBuilder();
                byte[] data=ascii("checksum");
                for(Checksum c:new Checksum[]{new CRC32(),new Adler32(),new Recorder()}){
                    c.update(data);c.update(data,2,3);c.update('!');
                    out.append(c.getValue()).append(',');
                    c.reset();out.append(c.getValue()).append(',').append(c instanceof Checksum).append(';');
                }
                Object o=new CRC32();
                return out+"|"+(o instanceof Checksum)+(o instanceof Adler32)+(new Adler32() instanceof Checksum);
            }
            public static String independentInstances(){
                CRC32 a=new CRC32(),b=new CRC32();Adler32 x=new Adler32(),y=new Adler32();
                a.update(ascii("one"));x.update(ascii("one"));
                return b.getValue()+","+y.getValue()+","+(a.getValue()!=b.getValue())+","+(x.getValue()!=y.getValue())+","+a.getValue()+","+x.getValue();
            }
        }
        """;

    @Test void computesCrc32() throws Exception {
        matchesJvm(FIXTURE, "crcVectors", "crcAllBytes", "crcUnsignedValue", "crcSingleBytes", "crcIncremental", "crcReset");
    }

    @Test void computesAdler32() throws Exception {
        matchesJvm(FIXTURE, "adlerVectors", "adlerLarge", "adlerSingleBytesAndReset", "adlerIncremental");
    }

    @Test void checksArgumentsLikeTheJdk() throws Exception {
        matchesJvm(FIXTURE, "rangeFailures", "failedUpdateKeepsState", "nullArrays");
    }

    @Test void dispatchesThroughChecksumInterface() throws Exception {
        matchesJvm(FIXTURE, "defaultUpdateDelegates", "throughInterface", "independentInstances");
    }

    @Test void leavesByteBufferOverloadsAndCrc32cUnlinked() {
        rejectsUnusedCall("java/util/zip/Checksum", "update", "(Ljava/nio/ByteBuffer;)V", INVOKEINTERFACE, true);
        rejectsUnusedCall("java/util/zip/CRC32", "update", "(Ljava/nio/ByteBuffer;)V", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/zip/Adler32", "update", "(Ljava/nio/ByteBuffer;)V", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/zip/CRC32C", "getValue", "()J", INVOKEVIRTUAL, false);
    }
}
