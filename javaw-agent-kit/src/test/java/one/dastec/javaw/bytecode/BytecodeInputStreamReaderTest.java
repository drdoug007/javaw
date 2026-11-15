package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T35: java.io.InputStreamReader. Expected values come from the JVM; see tasks/T35-input-stream-reader.md. */
class BytecodeInputStreamReaderTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.io.ByteArrayInputStream;
        import java.io.IOException;
        import java.io.InputStream;
        import java.io.InputStreamReader;
        import java.io.Reader;
        import java.io.UnsupportedEncodingException;
        import java.nio.charset.Charset;
        import java.nio.charset.StandardCharsets;

        /**
         * A network-like source: read(byte[],int,int) hands out at most `chunk` bytes and logs `rL,off,len=n` (L = b.length).
         * available() answers 0 unless `avail` is set (then the bytes left), and logs `aN` when `logAvail` is set.
         * `failAt` makes that bulk call (1-based) throw IOException("boom N"); `zeroAt` makes it return 0.
         */
        class Chunky extends InputStream {
            final byte[] data; final int chunk; int pos, calls, failAt, zeroAt; boolean avail, logAvail, availThrows;
            final StringBuilder log=new StringBuilder();
            Chunky(int chunk,int... bytes){this.chunk=chunk;data=new byte[bytes.length];for(int i=0;i<bytes.length;i++)data[i]=(byte)bytes[i];}
            Chunky(int chunk,byte[] bytes){this.chunk=chunk;data=bytes;}
            public int read(){log.append("r1 ");return pos<data.length?data[pos++]&0xff:-1;}
            public int read(byte[] b,int off,int len) throws IOException {
                calls++;
                log.append('r').append(b.length).append(',').append(off).append(',').append(len);
                if(calls==failAt){log.append("=boom ");throw new IOException("boom "+calls);}
                int n=calls==zeroAt?0:pos>=data.length?-1:Math.min(Math.min(chunk,len),data.length-pos);
                if(n>0){System.arraycopy(data,pos,b,off,n);pos+=n;}
                log.append('=').append(n).append(' ');
                return n;
            }
            public int available() throws IOException {
                if(availThrows){log.append("a! ");throw new IOException("no available");}
                int a=avail?data.length-pos:0;
                if(logAvail)log.append('a').append(a).append(' ');
                return a;
            }
            public void close(){log.append("close ");}
        }
        /** close() fails every time with IOException("close failed"). */
        class BadClose extends ByteArrayInputStream {
            int closes;
            BadClose(byte[] b){super(b);}
            public void close() throws IOException {closes++;throw new IOException("close failed");}
        }
        /** A library-style subclass (like Tomcat's or ANTLR's wrappers) that logs what reaches the overridable read(char[],int,int). */
        class LoggingReader extends InputStreamReader {
            final StringBuilder log=new StringBuilder();
            LoggingReader(InputStream in){super(in,StandardCharsets.UTF_8);}
            public int read(char[] c,int off,int len) throws IOException {
                log.append('R').append(off).append(',').append(len);
                int n=super.read(c,off,len);
                log.append('=').append(n).append(' ');
                return n;
            }
            public int read() throws IOException {log.append("R1 ");return super.read();}
        }

        public class Fixture {
            interface Op{Object run() throws Exception;}
            static String t(Op op){
                try{return String.valueOf(op.run());}catch(Exception e){return e.getClass().getSimpleName()+":"+e.getMessage();}
            }
            /** Like t, but only the exception type (for NullPointerExceptions whose JVM message is a helpful-NPE text). */
            static String ty(Op op){
                try{return String.valueOf(op.run());}catch(Exception e){return e.getClass().getSimpleName();}
            }
            /** Hex code units separated by '.', e.g. "41.e9.d83d.de00". */
            static String hex(char[] c,int off,int len){
                StringBuilder s=new StringBuilder();
                for(int i=0;i<len;i++){if(i>0)s.append('.');s.append(Integer.toHexString(c[off+i]));}
                return s.toString();
            }
            static byte[] bytes(int... v){byte[] b=new byte[v.length];for(int i=0;i<v.length;i++)b[i]=(byte)v[i];return b;}
            /** Reads with read(char[size]) until -1 (at most 40 calls): "n:hex n:hex ... -1". */
            static String drain(Reader r,int size) throws IOException {
                StringBuilder s=new StringBuilder();char[] c=new char[size];
                for(int i=0;i<40;i++){
                    int n=r.read(c,0,size);
                    if(n<0){s.append(n);return s.toString();}
                    s.append(n).append(':').append(hex(c,0,n)).append(' ');
                }
                return s.append("...").toString();
            }
            /** Reads with read() until -1 (at most 40 calls): "hex hex ... -1". */
            static String singles(Reader r) throws IOException {
                StringBuilder s=new StringBuilder();
                for(int i=0;i<40;i++){
                    int c=r.read();
                    if(c<0){s.append(c);return s.toString();}
                    s.append(Integer.toHexString(c)).append(' ');
                }
                return s.append("...").toString();
            }
            static String all(Reader r) throws IOException {
                StringBuilder s=new StringBuilder();char[] c=new char[64];int n;
                while((n=r.read(c,0,64))>=0)s.append(hex(c,0,n)).append(n>0?".":"");
                return s.toString();
            }
            static ByteArrayInputStream in(int... v){return new ByteArrayInputStream(bytes(v));}

            // ---- constructors and getEncoding

            public static String encodingNames() throws Exception {
                StringBuilder s=new StringBuilder();
                s.append(new InputStreamReader(in()).getEncoding()).append(' ');
                for(String name:new String[]{"UTF-8","utf8","ISO-8859-1","latin1","8859_1","US-ASCII","ascii","ISO646-US"})
                    s.append(name).append('=').append(new InputStreamReader(in(),name).getEncoding()).append(' ');
                s.append(new InputStreamReader(in(),StandardCharsets.UTF_8).getEncoding()).append(' ');
                s.append(new InputStreamReader(in(),StandardCharsets.ISO_8859_1).getEncoding()).append(' ');
                s.append(new InputStreamReader(in(),StandardCharsets.US_ASCII).getEncoding()).append(' ');
                s.append(new InputStreamReader(in(),Charset.forName("utf-8")).getEncoding());
                return s.toString();
            }
            public static String constructorFailures() {
                ByteArrayInputStream b=in(65);
                return t(()->new InputStreamReader(null))
                    +" | "+t(()->new InputStreamReader(null,"UTF-8"))
                    +" | "+t(()->new InputStreamReader(null,(String)null))
                    +" | "+t(()->new InputStreamReader(null,(Charset)null))
                    +" | "+t(()->new InputStreamReader(b,(String)null))
                    +" | "+t(()->new InputStreamReader(b,(Charset)null))
                    +" | "+t(()->new InputStreamReader(b,"no-such-charset"))
                    +" | "+t(()->new InputStreamReader(b,""))
                    +" | "+t(()->new InputStreamReader(b,"bad name!"))
                    +" | "+t(()->new InputStreamReader(b,"UTF-8") instanceof Reader)
                    +" | "+t(()->{try{new InputStreamReader(b,"nope");return "none";}catch(UnsupportedEncodingException e){return "caught "+(e instanceof IOException);}});
            }

            // ---- whole-input decoding

            public static String decodeWhole() throws Exception {
                int[] text={0x41,0xc3,0xa9,0xe2,0x82,0xac,0xf0,0x9f,0x98,0x80,0x7a};
                return drain(new InputStreamReader(in(text)),16)
                    +" | "+drain(new InputStreamReader(in(text),"UTF-8"),16)
                    +" | "+drain(new InputStreamReader(in(text),StandardCharsets.ISO_8859_1),16)
                    +" | "+drain(new InputStreamReader(in(text),"US-ASCII"),16)
                    +" | "+drain(new InputStreamReader(in()),4);
            }
            public static String latin1AndAscii() throws Exception {
                int[] b={0x00,0x41,0x7f,0x80,0x9f,0xa0,0xe9,0xff};
                return drain(new InputStreamReader(in(b),"ISO-8859-1"),16)+" | "+drain(new InputStreamReader(in(b),StandardCharsets.US_ASCII),16)
                    +" | "+singles(new InputStreamReader(in(b),"latin1"))+" | "+singles(new InputStreamReader(in(b),"ascii"));
            }
            public static String readIntoArray() throws Exception {
                InputStreamReader r=new InputStreamReader(in(0x61,0x62,0x63,0x64,0x65),StandardCharsets.UTF_8);
                char[] c={'.','.','.','.','.','.'};
                int n1=r.read(c,2,3);
                String first=n1+":"+new String(c);
                int n2=r.read(c);
                return first+" "+n2+":"+new String(c)+" "+r.read(c)+" "+r.read(c,0,0)+" "+r.read();
            }
            public static String largeInput() throws Exception {
                byte[] b=new byte[20000];
                for(int i=0;i<b.length;i++)b[i]=(byte)(i%3==2?0xa9:i%3==1?0xc3:0x41);
                b[8191]=(byte)0xc3;b[8192]=(byte)0xa9;
                Chunky in=new Chunky(100000,b);in.avail=true;
                InputStreamReader r=new InputStreamReader(in,StandardCharsets.UTF_8);
                char[] c=new char[30000];
                int n=r.read(c,0,c.length);
                int sum=0;for(int i=0;i<n;i++)sum=sum*31+c[i];
                return n+" "+sum+" "+r.read(c,0,c.length)+" "+in.log;
            }

            // ---- constructor and close errors, argument checks

            public static String closeBehaviour() throws Exception {
                Chunky in=new Chunky(4,0x61,0x62);
                InputStreamReader r=new InputStreamReader(in,"UTF-8");
                String s=r.getEncoding()+" "+(char)r.read()+" ";
                r.close();
                s+=r.getEncoding()+" "+in.log+"| ";
                r.close();
                s+=in.log+"| "+t(()->r.read())+" "+t(()->r.read(new char[4],0,2))+" "+t(()->r.read(new char[4],0,1))
                    +" "+t(()->r.read(new char[4],9,9))+" "+t(()->r.read(new char[4],0,0))+" "+t(()->r.ready())+" "+t(()->r.read(null,0,1))+" "+t(()->r.read());
                return s;
            }
            public static String closeFailure() {
                BadClose in=new BadClose(bytes(0x61));
                InputStreamReader r=new InputStreamReader(in,StandardCharsets.US_ASCII);
                return t(()->{r.close();return "closed";})+" "+r.getEncoding()+" "+t(()->{r.close();return "again";})+" "+in.closes
                    +" "+t(()->r.read())+" "+t(()->r.ready());
            }
            public static String indexChecks() {
                Chunky in=new Chunky(4,0x61,0x62);
                InputStreamReader r=new InputStreamReader(in,StandardCharsets.UTF_8);
                char[] c=new char[4];
                return t(()->r.read(c,-1,1))+" "+t(()->r.read(c,0,-1))+" "+t(()->r.read(c,5,0))+" "+t(()->r.read(c,2,3))
                    +" "+t(()->r.read(c,1,Integer.MAX_VALUE))+" "+t(()->r.read(c,4,0))+" "+t(()->r.read(c,0,0))+" "+ty(()->r.read(null,0,1))
                    +" "+ty(()->r.read(null,0,0))+" ["+in.log+"] "+t(()->r.read(c,3,1))+" "+(int)c[3];
            }

            // ---- multi-byte sequences split across reads of the underlying stream

            static String split(int chunk,int size,boolean avail){
                Chunky in=new Chunky(chunk,0x41,0xc3,0xa9,0xe2,0x82,0xac,0xf0,0x9f,0x98,0x80,0x7a);
                in.avail=avail;in.logAvail=true;
                StringBuilder s=new StringBuilder();
                try{
                    InputStreamReader r=new InputStreamReader(in,"UTF-8");char[] c=new char[size];
                    for(int i=0;i<20;i++){
                        int n=r.read(c,0,size);
                        s.append(n<0?"-1":n+":"+hex(c,0,n)).append(" <").append(in.log).append("> ");
                        in.log.setLength(0);
                        if(n<0)break;
                    }
                }catch(IOException e){s.append(e);}
                return s.toString();
            }
            public static String splitChunk1(){return split(1,8,false);}
            public static String splitChunk2(){return split(2,8,false);}
            public static String splitChunk3(){return split(3,8,false);}
            public static String splitWithAvailable(){return split(2,8,true)+" || "+split(3,3,true);}
            public static String truncatedAtEof() throws Exception {
                Chunky in=new Chunky(8,0x41,0x42,0xe2,0x82);
                InputStreamReader r=new InputStreamReader(in,StandardCharsets.UTF_8);
                char[] c=new char[2];
                StringBuilder s=new StringBuilder();
                for(int i=0;i<4;i++){
                    int n=r.read(c,0,2);
                    s.append(n<0?"-1":n+":"+hex(c,0,n)).append(" ready=").append(n<0?"-":String.valueOf(r.ready())).append(" <").append(in.log).append("> ");
                    in.log.setLength(0);
                }
                return s.toString();
            }

            // ---- malformed input

            static final int[][] BAD={
                {0x80},{0xbf,0x41},{0xc0,0x80},{0xc1,0xbf},{0xc2},{0xc2,0x41},{0xc3,0xc3,0xa9},
                {0xe0,0x80,0x80},{0xe0,0x9f,0xbf},{0xe0,0xa0},{0xe0,0xa0,0x41},{0xe2,0x82},{0xe2,0x82,0x41},{0xe2,0x41,0x42},
                {0xed,0xa0,0x80},{0xed,0xbf,0xbf},{0xed,0x9f,0xbf},{0xef,0xbf,0xbf},
                {0xf0,0x9f,0x98},{0xf0,0x9f},{0xf0},{0xf0,0x9f,0x98,0x41},{0xf0,0x9f,0x41,0x42},{0xf0,0x41,0x42,0x43},
                {0xf0,0x80,0x80,0x80},{0xf0,0x8f,0xbf,0xbf},{0xf4,0x8f,0xbf,0xbf},{0xf4,0x90,0x80,0x80},{0xf5,0x80,0x80,0x80},
                {0xf7,0xbf},{0xf8,0x88,0x80,0x80,0x80},{0xfe},{0xff,0x41},{0xf0,0x9f,0x98,0x80,0x80},{0x41,0xe2,0x82,0xac,0xe2}
            };
            static String bad(String charset,int chunk,int size) throws Exception {
                StringBuilder s=new StringBuilder();
                for(int[] b:BAD){
                    InputStream in=chunk==0?in(b):new Chunky(chunk,b);
                    InputStreamReader r=new InputStreamReader(in,charset);
                    s.append(size==0?singles(r):size<0?all(r):drain(r,size)).append(" / ");
                }
                return s.toString();
            }
            public static String utf8Malformed() throws Exception {return bad("UTF-8",0,64);}
            public static String utf8MalformedChunked() throws Exception {return bad("UTF-8",1,-1)+" || "+bad("UTF-8",2,-1);}
            public static String utf8MalformedSingles() throws Exception {return bad("UTF-8",1,0)+" || "+bad("UTF-8",0,2);}
            public static String asciiAndLatin1Malformed() throws Exception {return bad("US-ASCII",1,-1)+" || "+bad("ISO-8859-1",3,-1);}

            // ---- supplementary characters and single-char reads

            public static String oneCharAtATime() throws Exception {
                Chunky in=new Chunky(1,0x61,0xf0,0x9f,0x98,0x80,0x62,0xf0,0x90,0x80,0x80);
                InputStreamReader r=new InputStreamReader(in,StandardCharsets.UTF_8);
                StringBuilder s=new StringBuilder();
                for(int i=0;i<8;i++){int c=r.read();s.append(Integer.toHexString(c)).append(" <").append(in.log).append("> ");in.log.setLength(0);}
                return s.toString();
            }
            public static String leftoverChar() throws Exception {
                Chunky in=new Chunky(1,0xf0,0x9f,0x98,0x80,0x61,0x62);
                InputStreamReader r=new InputStreamReader(in,StandardCharsets.UTF_8);
                char[] c=new char[4];
                String s=Integer.toHexString(r.read())+" ready="+r.ready()+" ";
                int n=r.read(c,0,4);
                s+=n+":"+hex(c,0,n)+" ready="+r.ready()+" <"+in.log+"> ";
                in.log.setLength(0);
                n=r.read(c,0,4);
                s+=n+":"+hex(c,0,n)+" <"+in.log+">";
                return s;
            }
            public static String leftoverWithBufferedBytes() throws Exception {
                InputStreamReader r=new InputStreamReader(in(0xf0,0x9f,0x98,0x80,0x61,0xf0,0x9f,0x98,0x81,0x62),"UTF-8");
                char[] c=new char[5];
                String s=Integer.toHexString(r.read())+" ";
                int n=r.read(c,0,2);s+=n+":"+hex(c,0,n)+" ";
                n=r.read(c,0,1);s+=n+":"+hex(c,0,n)+" ";
                n=r.read(c,0,5);s+=n+":"+hex(c,0,n)+" ";
                s+=r.read()+" "+r.ready();
                return s;
            }
            public static String leftoverAfterClose() throws Exception {
                Chunky in=new Chunky(8,0xf0,0x9f,0x98,0x80,0x61);
                InputStreamReader r=new InputStreamReader(in,StandardCharsets.UTF_8);
                String s=Integer.toHexString(r.read())+" ";
                r.close();
                return s+r.getEncoding()+" "+t(()->Integer.toHexString(r.read()))+" "+t(()->r.read())+" "+t(()->r.ready())+" <"+in.log+">";
            }
            public static String singleSlotReads() throws Exception {
                Chunky in=new Chunky(1,0xf0,0x9f,0x98,0x80,0xf0,0x9f,0x98,0x81);
                InputStreamReader r=new InputStreamReader(in,StandardCharsets.UTF_8);
                char[] c=new char[1];StringBuilder s=new StringBuilder();
                for(int i=0;i<5;i++){int n=r.read(c,0,1);s.append(n<0?"-1":n+":"+hex(c,0,n)).append(' ');}
                return s.append('<').append(in.log).append('>').toString();
            }
            public static String noRoomForPair() throws Exception {
                String a=drain(new InputStreamReader(in(0x61,0xf0,0x9f,0x98,0x80,0x62),"UTF-8"),2);
                String b=drain(new InputStreamReader(in(0x61,0x62,0xf0,0x9f,0x98,0x80),"UTF-8"),3);
                String c=drain(new InputStreamReader(in(0x61,0x80,0x62,0xff),"UTF-8"),2);
                String d=drain(new InputStreamReader(in(0x61,0xf0,0x9f,0x98,0x41),"UTF-8"),2);
                return a+" | "+b+" | "+c+" | "+d;
            }

            // ---- stream failures, available() and subclasses

            public static String zeroByteStream() {
                Chunky in=new Chunky(4,0x61,0x62,0x63);in.zeroAt=1;
                InputStreamReader r=new InputStreamReader(in,StandardCharsets.UTF_8);
                return t(()->r.read())+" "+t(()->Integer.toHexString(r.read()))+" <"+in.log+">";
            }
            public static String failingStream() throws Exception {
                Chunky in=new Chunky(1,0xe2,0x82,0xac,0x41);in.failAt=2;
                InputStreamReader r=new InputStreamReader(in,StandardCharsets.UTF_8);
                char[] c=new char[4];
                String s=t(()->r.read(c,0,4))+" ready="+r.ready()+" <"+in.log+"> ";in.log.setLength(0);
                int n=r.read(c,0,4);
                return s+n+":"+hex(c,0,n)+" <"+in.log+"> "+drain(r,4);
            }
            public static String availableThrows() throws Exception {
                Chunky in=new Chunky(2,0x61,0x62,0x63,0x64);in.availThrows=true;
                InputStreamReader r=new InputStreamReader(in,StandardCharsets.UTF_8);
                String s=r.ready()+" ";
                char[] c=new char[8];
                int n=r.read(c,0,8);
                return s+n+":"+hex(c,0,n)+" "+r.ready()+" <"+in.log+">";
            }
            public static String readyStates() throws Exception {
                Chunky in=new Chunky(1,0x61,0x62);in.logAvail=true;
                InputStreamReader r=new InputStreamReader(in,StandardCharsets.UTF_8);
                String s=r.ready()+" <"+in.log+"> ";in.log.setLength(0);
                in.avail=true;
                s+=r.ready()+" <"+in.log+"> ";in.log.setLength(0);
                ByteArrayInputStream b=in(0x61,0x62,0x63);
                InputStreamReader q=new InputStreamReader(b,StandardCharsets.UTF_8);
                s+=q.ready()+" "+(char)q.read()+" "+q.ready()+" "+b.available()+" "+(char)q.read()+(char)q.read()+" "+q.ready()+" "+q.read()+" "+q.ready();
                return s;
            }
            public static String readerSubclass() throws Exception {
                LoggingReader r=new LoggingReader(in(0x61,0x62,0x63,0x64,0x65,0x66,0x67,0x68,0x69,0x6a));
                char[] c=new char[3];
                String s=(char)r.read()+" "+r.log+"| ";r.log.setLength(0);
                s+=r.read(c)+" "+r.log+"| ";r.log.setLength(0);
                s+=r.skip(2)+" "+r.log+"| ";r.log.setLength(0);
                s+=r.read(c,1,1)+" "+r.log+"| ";r.log.setLength(0);
                StringBuilder rest=new StringBuilder();
                int n;while((n=r.read(c))>=0)rest.append(c,0,n);
                return s+rest+" "+r.log+" "+r.getEncoding();
            }
        }
        """;

    @Test void constructorsAndEncodings() throws Exception {
        matchesJvm(FIXTURE, "encodingNames", "constructorFailures", "decodeWhole", "latin1AndAscii", "readIntoArray");
    }

    @Test void closeAndArgumentChecks() throws Exception {
        matchesJvm(FIXTURE, "closeBehaviour", "closeFailure", "indexChecks");
    }

    @Test void sequencesSplitAcrossReads() throws Exception {
        matchesJvm(FIXTURE, "splitChunk1", "splitChunk2", "splitChunk3", "splitWithAvailable", "truncatedAtEof", "largeInput");
    }

    @Test void malformedInput() throws Exception {
        matchesJvm(FIXTURE, "utf8Malformed", "utf8MalformedChunked", "utf8MalformedSingles", "asciiAndLatin1Malformed");
    }

    @Test void supplementaryCharacters() throws Exception {
        matchesJvm(FIXTURE, "oneCharAtATime", "leftoverChar", "leftoverWithBufferedBytes", "leftoverAfterClose", "singleSlotReads", "noRoomForPair");
    }

    @Test void streamFailuresAndSubclasses() throws Exception {
        matchesJvm(FIXTURE, "zeroByteStream", "failingStream", "availableThrows", "readyStates", "readerSubclass");
    }

    @Test void leavesOutCharBufferRead() {
        rejectsUnusedCall("java/io/InputStreamReader", "read", "(Ljava/nio/CharBuffer;)I", INVOKEVIRTUAL, false);
    }
}
