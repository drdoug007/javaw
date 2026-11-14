package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T19: java.io.ByteArrayOutputStream. Expected values come from the JVM; see tasks/T19-byte-array-output-stream.md. */
class BytecodeByteArrayOutputStreamTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.io.ByteArrayOutputStream;
        import java.io.IOException;
        import java.io.OutputStream;
        import java.io.UnsupportedEncodingException;
        import java.nio.charset.Charset;
        import java.nio.charset.StandardCharsets;
        import java.util.Arrays;
        class Probe extends ByteArrayOutputStream {
            Probe(){super();}
            Probe(int size){super(size);}
            int capacity(){return buf.length;}
            int counted(){return count;}
        }
        class Preloaded extends ByteArrayOutputStream {
            Preloaded(){super(0);buf=new byte[]{65,66,67};count=3;}
            void truncate(int n){count=n;}
            void poke(int i,int b){buf[i]=(byte)b;}
            int capacity(){return buf.length;}
        }
        class Recorder extends OutputStream {
            final StringBuilder log=new StringBuilder();
            public void write(int b){log.append("w").append(b).append(';');}
            public void write(byte[] b,int off,int len){
                log.append("a").append(b.length).append(',').append(off).append(',').append(len).append(':');
                for(int i=0;i<len;i++)log.append(b[off+i]).append(' ');
                log.append(';');
            }
        }
        class Failing extends OutputStream {
            public void write(int b) throws IOException {throw new IOException("sink full");}
            public void write(byte[] b,int off,int len) throws IOException {throw new IOException("sink full "+len);}
        }
        public class Fixture {
            static String name(Throwable t){return t.getClass().getSimpleName();}
            static String codes(String s){StringBuilder out=new StringBuilder();for(int i=0;i<s.length();i++)out.append(Integer.toHexString(s.charAt(i))).append(' ');return out.toString();}

            public static String defaultCapacities(){
                return new Probe().capacity()+","+new Probe(0).capacity()+","+new Probe(5).capacity()+","+new Probe().counted()+","+new ByteArrayOutputStream().size();
            }
            public static String growthByOne(){
                Probe p=new Probe(0);StringBuilder out=new StringBuilder();
                for(int i=0;i<9;i++){p.write(i);out.append(p.capacity()).append(',');}
                Probe q=new Probe();for(int i=0;i<33;i++)q.write(i);
                return out.append('|').append(q.capacity()).append(',').append(q.size()).toString();
            }
            public static String growthByArray(){
                Probe p=new Probe(3);StringBuilder out=new StringBuilder();
                p.write(new byte[10],0,10);out.append(p.capacity()).append(',');
                p.write(1);out.append(p.capacity()).append(',');
                p.write(new byte[50],0,50);out.append(p.capacity()).append(',');
                p.write(new byte[3],0,3);out.append(p.capacity()).append(',');
                p.write(new byte[0],0,0);out.append(p.capacity()).append(',').append(p.size());
                Probe z=new Probe(0);z.write(new byte[0],0,0);out.append('|').append(z.capacity());
                return out.toString();
            }
            public static String negativeSize(){
                StringBuilder out=new StringBuilder();
                for(int n:new int[]{-1,Integer.MIN_VALUE}){
                    try{new ByteArrayOutputStream(n);out.append("none;");}catch(IllegalArgumentException e){out.append(name(e)).append(':').append(e.getMessage()).append(';');}
                }
                return out.toString();
            }
            public static String subclassFields(){
                Preloaded p=new Preloaded();StringBuilder out=new StringBuilder();
                out.append(p.size()).append(p).append(',');
                p.write('D');out.append(p.capacity()).append(p).append(',');
                p.truncate(2);out.append(p.size()).append(p).append(Arrays.toString(p.toByteArray())).append(',');
                p.write('Z');p.poke(0,'a');out.append(p).append(',').append(p.capacity());
                return out.toString();
            }

            public static String writeSingleBytes(){
                ByteArrayOutputStream s=new ByteArrayOutputStream();
                s.write(65);s.write(256+66);s.write(-1);s.write(0x7f);s.write(0x80);s.write(Integer.MIN_VALUE+67);
                return s.size()+Arrays.toString(s.toByteArray());
            }
            public static String writeRanges(){
                ByteArrayOutputStream s=new ByteArrayOutputStream(2);
                byte[] b={10,20,30,40,50};
                s.write(b,1,3);s.write(b,0,0);s.write(b,5,0);s.write(b,4,1);
                return s.size()+Arrays.toString(s.toByteArray());
            }
            public static String writeBytesAndInherited() throws IOException {
                ByteArrayOutputStream s=new ByteArrayOutputStream(1);
                s.writeBytes(new byte[]{1,2});s.writeBytes(new byte[0]);s.write(new byte[]{3,4,5});
                s.flush();
                return s.size()+Arrays.toString(s.toByteArray());
            }
            public static String writeRangeFailures(){
                ByteArrayOutputStream s=new ByteArrayOutputStream();s.write(9);
                byte[] b={1,2,3,4,5};StringBuilder out=new StringBuilder();
                int[][] cases={{-1,1},{0,-1},{2,4},{6,0},{1,Integer.MAX_VALUE},{Integer.MIN_VALUE,0}};
                for(int[] c:cases){
                    try{s.write(b,c[0],c[1]);out.append("none;");}catch(IndexOutOfBoundsException e){out.append(name(e)).append(':').append(e.getMessage()).append(';');}
                }
                return out.append(s.size()).append(Arrays.toString(s.toByteArray())).toString();
            }
            public static String nullArrays(){
                ByteArrayOutputStream s=new ByteArrayOutputStream();s.write(7);StringBuilder out=new StringBuilder();
                try{s.write(null,0,0);out.append("none;");}catch(NullPointerException e){out.append(name(e)).append(';');}
                try{s.writeBytes(null);out.append("none;");}catch(NullPointerException e){out.append(name(e)).append(';');}
                try{s.write((byte[])null);out.append("none;");}catch(NullPointerException e){out.append(name(e)).append(';');}catch(IOException e){out.append("io;");}
                return out.append(s.size()).toString();
            }

            public static String toByteArrayCopies(){
                Probe p=new Probe();p.write(1);p.write(2);p.write(3);
                byte[] a=p.toByteArray();a[0]=99;byte[] b=p.toByteArray();
                return a.length+","+b[0]+","+(a!=b)+","+new ByteArrayOutputStream().toByteArray().length+","+new ByteArrayOutputStream(0).toByteArray().length;
            }
            public static String resetKeepsBuffer(){
                Probe p=new Probe(0);for(int i=0;i<5;i++)p.write(i);
                int before=p.capacity();p.reset();
                String mid=p.size()+","+p.toByteArray().length+","+p.capacity()+","+before;
                p.write(42);p.reset();p.reset();p.write(43);
                return mid+"|"+p.size()+Arrays.toString(p.toByteArray())+p.capacity();
            }
            public static String writeTo() throws IOException {
                ByteArrayOutputStream s=new ByteArrayOutputStream();s.write(1);s.write(-2);s.write(3);
                Recorder r=new Recorder();s.writeTo(r);
                ByteArrayOutputStream empty=new ByteArrayOutputStream(4);empty.writeTo(r);
                ByteArrayOutputStream copy=new ByteArrayOutputStream();copy.write(9);s.writeTo(copy);s.writeTo(copy);
                return r.log+"|"+Arrays.toString(copy.toByteArray())+"|"+s.size();
            }
            public static String writeToFailures(){
                ByteArrayOutputStream s=new ByteArrayOutputStream();s.write(1);s.write(2);StringBuilder out=new StringBuilder();
                try{s.writeTo(new Failing());out.append("none;");}catch(IOException e){out.append(name(e)).append(':').append(e.getMessage()).append(';');}
                try{s.writeTo(null);out.append("none;");}catch(NullPointerException e){out.append(name(e)).append(';');}catch(IOException e){out.append("io;");}
                return out.append(s.size()).toString();
            }

            public static String defaultCharsetString(){
                ByteArrayOutputStream s=new ByteArrayOutputStream();
                s.writeBytes("Hi, ".getBytes(StandardCharsets.UTF_8));
                s.writeBytes(new byte[]{(byte)0xc3,(byte)0xa9,(byte)0xe2,(byte)0x82,(byte)0xac,(byte)0xf0,(byte)0x9f,(byte)0x98,(byte)0x80,'!'});
                return s.toString()+"|"+codes(s.toString())+"|"+new ByteArrayOutputStream().toString().isEmpty();
            }
            public static String malformedUtf8(){
                StringBuilder out=new StringBuilder();
                byte[][] inputs={{(byte)0xc3},{(byte)0xff,65},{(byte)0xe2,(byte)0x82},{(byte)0x80,(byte)0x80,66},{(byte)0xc0,(byte)0xaf}};
                for(byte[] in:inputs){ByteArrayOutputStream s=new ByteArrayOutputStream();s.writeBytes(in);out.append(codes(s.toString())).append('|');}
                return out.toString();
            }
            public static String charsetStrings() throws UnsupportedEncodingException {
                ByteArrayOutputStream s=new ByteArrayOutputStream();
                s.write('A');s.write(0xe9);s.write(0xff);s.write('z');
                return codes(s.toString("ISO-8859-1"))+"|"+codes(s.toString(StandardCharsets.ISO_8859_1))+"|"+codes(s.toString("US-ASCII"))
                        +"|"+codes(s.toString(StandardCharsets.UTF_16BE))+"|"+codes(s.toString("utf-8"))+"|"+codes(s.toString(Charset.forName("latin1")));
            }
            public static String hibyteStrings(){
                ByteArrayOutputStream s=new ByteArrayOutputStream();
                s.write('A');s.write(0xe9);
                return codes(s.toString(0))+"|"+codes(s.toString(1))+"|"+codes(s.toString(0x1fe))+"|"+new ByteArrayOutputStream().toString(5).length();
            }
            public static String stringFailures(){
                ByteArrayOutputStream s=new ByteArrayOutputStream();s.write('x');StringBuilder out=new StringBuilder();
                for(String n:new String[]{"no-such-charset","bad name!"}){
                    try{s.toString(n);out.append("none;");}catch(UnsupportedEncodingException e){out.append(name(e)).append(':').append(e.getMessage()).append(';');}
                }
                try{s.toString((String)null);out.append("none;");}catch(NullPointerException e){out.append(name(e)).append(';');}catch(UnsupportedEncodingException e){out.append("uee;");}
                try{s.toString((Charset)null);out.append("none;");}catch(NullPointerException e){out.append(name(e)).append(';');}
                return out.append(s).toString();
            }
            public static String closeIsNoOp() throws IOException {
                ByteArrayOutputStream s=new ByteArrayOutputStream();s.write(1);
                s.close();s.write(2);s.close();s.write(new byte[]{3,4},0,2);s.flush();
                OutputStream o=s;o.close();o.write(5);
                try(ByteArrayOutputStream t=new ByteArrayOutputStream()){t.write(6);s.writeBytes(t.toByteArray());}
                return s.size()+Arrays.toString(s.toByteArray());
            }
        }
        """;

    @Test void constructsAndGrows() throws Exception {
        matchesJvm(FIXTURE, "defaultCapacities", "growthByOne", "growthByArray", "negativeSize", "subclassFields");
    }

    @Test void writesBytesAndChecksRanges() throws Exception {
        matchesJvm(FIXTURE, "writeSingleBytes", "writeRanges", "writeBytesAndInherited", "writeRangeFailures", "nullArrays");
    }

    @Test void copiesResetsAndWritesTo() throws Exception {
        matchesJvm(FIXTURE, "toByteArrayCopies", "resetKeepsBuffer", "writeTo", "writeToFailures");
    }

    @Test void decodesStrings() throws Exception {
        matchesJvm(FIXTURE, "defaultCharsetString", "malformedUtf8", "charsetStrings", "hibyteStrings");
    }

    @Test void stringFailuresAndClose() throws Exception {
        matchesJvm(FIXTURE, "stringFailures", "closeIsNoOp");
    }
}
