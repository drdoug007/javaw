package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T18: java.io.InputStream, ByteArrayInputStream and EOFException. Expected values come from the JVM; see tasks/T18-input-stream.md. */
class BytecodeInputStreamTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.io.ByteArrayInputStream;
        import java.io.EOFException;
        import java.io.IOException;
        import java.io.InputStream;
        import java.io.OutputStream;
        import java.util.Arrays;

        /** A library-style source that only implements read(): yields (i * 7) & 0xff for i = 0 .. size-1. */
        class Counter extends InputStream {
            final int size; int next; int calls;
            Counter(int size){this.size=size;}
            public int read(){calls++;return next<size?(next++*7)&0xff:-1;}
        }
        /** Returns 'a' and 'b', then throws from read(). */
        class Failing extends InputStream {
            int next; final boolean checked;
            Failing(boolean checked){this.checked=checked;}
            public int read() throws IOException {
                if(next<2)return 'a'+next++;
                if(checked)throw new IOException("boom");
                throw new IllegalStateException("bad");
            }
        }
        /** Overrides read(byte[],int,int) to hand out at most 3 bytes per call, like a network stream. */
        class Chunky extends InputStream {
            final byte[] data; int pos; int bulkCalls;
            Chunky(int size){data=new byte[size];for(int i=0;i<size;i++)data[i]=(byte)(i+1);}
            public int read(){return pos<data.length?data[pos++]&0xff:-1;}
            public int read(byte[] b,int off,int len){
                bulkCalls++;
                if(len==0)return 0;
                if(pos>=data.length)return -1;
                int n=Math.min(Math.min(len,3),data.length-pos);
                System.arraycopy(data,pos,b,off,n);pos+=n;return n;
            }
        }
        /** skip() reports a fixed answer so skipNBytes takes its other branches. */
        class FixedSkip extends Counter {
            final long answer;
            FixedSkip(int size,long answer){super(size);this.answer=answer;}
            public long skip(long n){return answer<0?n+1:answer;}
        }
        /** Exposes the protected fields, the way library subclasses (bcel's ByteSequence) use them. */
        class Peek extends ByteArrayInputStream {
            Peek(byte[] b){super(b);}
            Peek(byte[] b,int off,int len){super(b,off,len);}
            String state(){return "pos="+pos+",count="+count+",mark="+mark+",buf="+buf.length;}
            void seek(int p){pos=p;}
            void shrink(int c){count=c;}
        }
        /** Collects written bytes through OutputStream.write(int). */
        class Sink extends OutputStream {
            final StringBuilder out=new StringBuilder();
            public void write(int b){out.append(b&0xff).append(' ');}
        }

        /** Records every read(byte[],int,int) as len@off/b.length; throws once closed, like a checked stream. */
        class Logged extends InputStream {
            final StringBuilder log=new StringBuilder(); final int size; final int step; int pos; boolean closed;
            Logged(int size,int step){this.size=size;this.step=step;}
            public int read() throws IOException {if(closed)throw new IOException("Stream closed");return pos<size?pos++&0xff:-1;}
            public int read(byte[] b,int off,int len) throws IOException {
                log.append(len).append('@').append(off).append('/').append(b.length).append(' ');
                if(closed)throw new IOException("Stream closed");
                if(len==0)return 0;
                if(pos>=size)return -1;
                int n=Math.min(Math.min(len,step),size-pos);
                for(int i=0;i<n;i++)b[off+i]=(byte)(pos+i);
                pos+=n;return n;
            }
            public void close(){closed=true;}
        }
        /** Records each write(byte[],int,int) as off+len and throws on the failAt-th call (0 never fails). */
        class ChunkSink extends OutputStream {
            final StringBuilder log=new StringBuilder(); final int failAt; int calls;
            ChunkSink(int failAt){this.failAt=failAt;}
            public void write(int b){log.append("w1 ");}
            public void write(byte[] b,int off,int len) throws IOException {
                log.append(off).append('+').append(len).append(' ');
                if(++calls==failAt)throw new IOException("sink full");
            }
        }

        public class Fixture {
            interface Op{Object run() throws Exception;}
            static String t(Op op){
                try{Object r=op.run();return r instanceof byte[]?Arrays.toString((byte[])r):String.valueOf(r);}
                catch(Exception e){return e.getClass().getSimpleName()+":"+e.getMessage();}
            }
            static String ty(Op op){
                try{return String.valueOf(op.run());}catch(Exception e){return e.getClass().getSimpleName();}
            }
            static byte[] bytes(int... v){byte[] b=new byte[v.length];for(int i=0;i<v.length;i++)b[i]=(byte)v[i];return b;}
            static int sum(byte[] b){int s=0;for(byte x:b)s=s*31+(x&0xff);return s;}

            public static String counterRead(){
                Counter c=new Counter(5);byte[] b=new byte[4];
                return c.read()+","+t(()->c.read(b)+Arrays.toString(b))+","+t(()->c.read(b)+Arrays.toString(b))+","+t(()->c.read(b))+","+c.read()+",calls="+c.calls;
            }
            public static String counterReadOffset(){
                Counter c=new Counter(3);byte[] b=new byte[6];
                return t(()->c.read(b,2,3)+Arrays.toString(b))+","+t(()->c.read(b,0,0))+",calls="+c.calls+","+t(()->new Counter(0).read(b,1,0))+","+t(()->new Counter(0).read(b,1,2));
            }
            public static String counterBounds(){
                Counter c=new Counter(3);byte[] b=new byte[4];
                return t(()->c.read(b,-1,1))+";"+t(()->c.read(b,2,3))+";"+t(()->c.read(b,0,-1))+";"+t(()->c.read(b,5,0))
                        +";"+t(()->c.readNBytes(b,3,2))+";"+ty(()->c.read(null,0,0))+";"+t(()->c.readNBytes(-1))+",calls="+c.calls;
            }
            public static String defaultLoopExceptions(){
                Failing f=new Failing(true);byte[] b=new byte[5];
                String first=t(()->f.read(b,0,5))+Arrays.toString(b);
                return first+";"+t(()->f.read(b,0,5))+";"+t(()->new Failing(true).readAllBytes())+";"+t(()->new Failing(false).read(new byte[4],0,4));
            }
            public static String counterBulk(){
                Counter c=new Counter(10);
                return t(()->c.readNBytes(3))+t(()->c.readNBytes(0))+t(()->c.readAllBytes())+t(()->c.readAllBytes())+t(()->c.readNBytes(4));
            }
            public static String counterLarge(){
                Counter c=new Counter(40000);
                return t(()->{byte[] a=c.readNBytes(20001);byte[] r=c.readAllBytes();return a.length+"/"+sum(a)+"/"+r.length+"/"+sum(r)+"/"+c.read();})
                        +";"+t(()->{byte[] all=new Counter(40000).readAllBytes();return all.length+"/"+sum(all);});
            }
            public static String chunkyBulk(){
                Chunky c=new Chunky(10);byte[] b=new byte[12];
                return t(()->c.readNBytes(b,1,8))+Arrays.toString(b)+","+t(()->c.readNBytes(b,0,5))+","+t(()->c.readNBytes(b,0,5))+","+t(()->c.read(b))
                        +";"+t(()->new Chunky(7).readAllBytes())+";"+t(()->new Chunky(8).readNBytes(5));
            }
            public static String counterSkip(){
                Counter c=new Counter(6000);
                return t(()->c.skip(0))+","+t(()->c.skip(-3))+",calls="+c.calls+","+t(()->c.skip(5000))+","+t(()->c.skip(5000))+","+t(()->c.skip(1))+","+c.read();
            }
            public static String counterMarkAndClose(){
                Counter c=new Counter(4);
                c.mark(100);
                String s=c.markSupported()+","+t(()->c.available())+","+t(()->{c.reset();return "reset";})+","+c.read()+","+c.read();
                return s+","+t(()->{c.close();return "closed";})+","+c.read();
            }
            public static String counterTransfer(){
                Counter c=new Counter(6);Sink s=new Sink();c.read();
                return t(()->c.transferTo(s))+":"+s.out+","+t(()->c.transferTo(s))+","+t(()->new Counter(2).transferTo(null))+";"+t(()->{Sink k=new Sink();new Chunky(7).transferTo(k);return k.out;});
            }

            public static String skipNBytes(){
                Counter c=new Counter(10);
                return t(()->{c.skipNBytes(4);return c.read();})+","+t(()->{c.skipNBytes(0);c.skipNBytes(-2);return c.read();})+","+t(()->{c.skipNBytes(9);return "x";})
                        +";"+t(()->{Counter z=new FixedSkip(5,0);z.skipNBytes(3);return z.read()+"/"+z.calls;})+";"+t(()->{new FixedSkip(2,0).skipNBytes(3);return "x";})
                        +";"+t(()->{new FixedSkip(5,-1).skipNBytes(3);return "x";})+";"+t(()->{new FixedSkip(5,2).skipNBytes(3);return "x";});
            }
            public static String eofException(){
                EOFException a=new EOFException(),b=new EOFException("short read");
                Exception e=b;
                return a.getMessage()+","+b.getMessage()+","+(e instanceof IOException)+","+t(()->{throw new EOFException("x");});
            }

            public static String baisRead(){
                ByteArrayInputStream in=new ByteArrayInputStream(bytes(1,-1,127,-128,0));
                byte[] b=new byte[3];
                return in.read()+","+in.read()+","+in.available()+","+t(()->in.read(b)+Arrays.toString(b))+","+in.read(b,0,0)+","+t(()->in.read(b))+","+in.read(b,0,0)+","+in.read()+","+in.available();
            }
            public static String baisRange(){
                byte[] data=bytes(10,20,30,40,50,60);
                ByteArrayInputStream a=new ByteArrayInputStream(data,2,3),b=new ByteArrayInputStream(data,4,100),c=new ByteArrayInputStream(data,6,2);
                return a.available()+":"+t(()->a.readAllBytes())+","+b.available()+":"+t(()->b.readAllBytes())+","+c.available()+":"+c.read()
                        +","+t(()->{a.reset();return a.read();});
            }
            public static String baisBounds(){
                ByteArrayInputStream in=new ByteArrayInputStream(bytes(1,2));byte[] b=new byte[3];
                in.skip(2);
                return t(()->in.read(b,2,2))+";"+t(()->in.read(b,-1,0))+";"+t(()->in.read(b,0,-2))+";"+t(()->in.readNBytes(b,1,3))+";"+ty(()->in.read(null,0,0))
                        +";"+t(()->in.readNBytes(-1));
            }
            public static String baisSkip(){
                ByteArrayInputStream in=new ByteArrayInputStream(bytes(1,2,3,4,5,6,7));
                return in.skip(2)+","+in.skip(-5)+","+in.skip(0)+","+in.read()+","+in.skip(100)+","+in.available()+","+in.skip(1)+","+in.read()
                        +","+new ByteArrayInputStream(new byte[4]).skip(Long.MAX_VALUE)+","+new ByteArrayInputStream(new byte[4]).skip(Long.MIN_VALUE);
            }
            public static String baisBulk(){
                ByteArrayInputStream in=new ByteArrayInputStream(bytes(1,2,3,4,5,6,7,8));byte[] b=new byte[6];
                return t(()->in.readNBytes(2))+t(()->in.readNBytes(b,1,3))+Arrays.toString(b)+t(()->in.readAllBytes())+t(()->in.readAllBytes())
                        +","+in.readNBytes(b,0,2)+","+t(()->in.readNBytes(3))+","+t(()->{in.skipNBytes(0);return "ok";})+","+t(()->{in.skipNBytes(1);return "ok";});
            }
            public static String baisSkipNBytes(){
                ByteArrayInputStream in=new ByteArrayInputStream(bytes(1,2,3,4,5));
                return t(()->{in.skipNBytes(2);return in.read();})+","+t(()->{in.skipNBytes(5);return "x";})+","+in.available();
            }
            public static String baisTransfer(){
                ByteArrayInputStream in=new ByteArrayInputStream(bytes(9,8,7,6,-1));in.read();
                Sink s=new Sink();
                return t(()->in.transferTo(s))+":"+s.out+","+t(()->in.transferTo(s))+","+in.read()+","+t(()->in.transferTo(null))
                        +","+ty(()->new ByteArrayInputStream(bytes(1)).transferTo(null));
            }
            public static String baisMarkReset(){
                ByteArrayInputStream in=new ByteArrayInputStream(bytes(1,2,3,4,5));
                String s=in.markSupported()+","+in.read();
                in.reset();
                s+=","+in.read()+","+in.read();
                in.mark(0);
                s+=","+in.read()+","+in.read();
                in.reset();
                s+=","+in.read()+","+t(()->{in.close();return in.read();})+","+in.available();
                in.skip(10);in.reset();
                return s+","+in.read();
            }

            public static String protectedFields(){
                byte[] data=bytes(1,2,3,4,5,6);
                Peek p=new Peek(data,1,3),q=new Peek(data),r=new Peek(data,4,9);
                String s=p.state()+"|"+q.state()+"|"+r.state();
                p.read();p.mark(0);p.read();
                s+="|"+p.state();
                p.seek(0);
                s+="|"+p.read()+","+p.available();
                q.shrink(2);
                s+="|"+t(()->q.readAllBytes())+","+q.state();
                return s;
            }
            public static String bufferIsShared(){
                byte[] data=bytes(1,2,3);
                ByteArrayInputStream in=new ByteArrayInputStream(data);
                data[1]=42;
                return in.read()+","+in.read()+","+t(()->{byte[] rest=in.readAllBytes();rest[0]=9;return data[2];});
            }

            public static String nullStreamOpen(){
                InputStream in=InputStream.nullInputStream();byte[] b=new byte[2];
                return t(()->in.read())+","+t(()->in.read(b))+","+t(()->in.read(b,0,0))+","+t(()->in.available())+","+t(()->in.readAllBytes())+","+t(()->in.readNBytes(5))
                        +","+t(()->in.readNBytes(b,0,2))+","+t(()->in.skip(5))+","+t(()->{in.skipNBytes(0);return "ok";})+","+t(()->{in.skipNBytes(1);return "ok";})
                        +","+t(()->in.transferTo(new Sink()))+","+t(()->in.transferTo(null))+","+in.markSupported()+","+t(()->{in.reset();return "ok";})
                        +","+t(()->in.read(b,1,2))+","+(InputStream.nullInputStream()!=in);
            }
            public static String nullStreamClosed(){
                InputStream in=InputStream.nullInputStream();byte[] b=new byte[2];
                String s=t(()->{in.close();in.close();return "closed";});
                return s+";"+t(()->in.read())+";"+t(()->in.read(b))+";"+t(()->in.read(b,0,0))+";"+t(()->in.read(b,0,3))+";"+t(()->in.available())
                        +";"+t(()->in.readAllBytes())+";"+t(()->in.readNBytes(1))+";"+t(()->in.readNBytes(-1))+";"+t(()->in.readNBytes(b,0,0))
                        +";"+t(()->in.skip(1))+";"+t(()->{in.skipNBytes(0);return "ok";})+";"+t(()->in.transferTo(new Sink()))+";"+t(()->in.transferTo(null))
                        +";"+t(()->InputStream.nullInputStream().read());
            }

            public static String readNBytesZeroLengthReads(){
                Logged closed=new Logged(3,10);closed.close();
                Logged empty=new Logged(3,10),exact=new Logged(10,10),shortSrc=new Logged(4,2),eof=new Logged(0,10);
                String s=t(()->closed.readNBytes(0))+"|"+closed.log+"|"+t(()->empty.readNBytes(0))+"|"+empty.log;
                s+="|"+t(()->exact.readNBytes(5))+"|"+exact.log+"|"+t(()->shortSrc.readNBytes(6))+"|"+shortSrc.log;
                return s+"|"+t(()->eof.readNBytes(4))+"|"+eof.log;
            }
            public static String readNBytesChunks(){
                Logged big=new Logged(20000,7000),all=new Logged(16384,16384),rest=new Logged(5,3);
                String s=t(()->sum(big.readNBytes(18000)))+"|"+big.log+"|"+t(()->sum(all.readAllBytes()))+"|"+all.log;
                byte[] b=new byte[8];
                return s+"|"+t(()->rest.readNBytes(b,1,6)+Arrays.toString(b))+"|"+rest.log+"|"+t(()->rest.readNBytes(b,0,0))+"|"+rest.log;
            }
            public static String transferToBufferSize(){
                Logged in=new Logged(20000,20000);ChunkSink out=new ChunkSink(0);
                return t(()->in.transferTo(out))+"|"+in.log+"|"+out.log;
            }
            public static String baisTransferChunks(){
                byte[] data=new byte[140000];data[131072]=5;
                ByteArrayInputStream ok=new ByteArrayInputStream(data),fail=new ByteArrayInputStream(data,3,139990),first=new ByteArrayInputStream(data);
                ChunkSink a=new ChunkSink(0),b=new ChunkSink(2),c=new ChunkSink(1);
                String s=t(()->ok.transferTo(a))+"|"+a.log+"|"+ok.available();
                s+="|"+t(()->fail.transferTo(b))+"|"+b.log+"|"+fail.available()+","+fail.read();
                return s+"|"+t(()->first.transferTo(c))+"|"+c.log+"|"+first.available();
            }
        }
        """;

    @Test void defaultReadMethodsOfASubclass() throws Exception {
        matchesJvm(FIXTURE, "counterRead", "counterReadOffset", "counterBounds", "defaultLoopExceptions");
    }

    @Test void defaultBulkSkipMarkAndTransfer() throws Exception {
        matchesJvm(FIXTURE, "counterBulk", "counterLarge", "chunkyBulk", "counterSkip", "counterMarkAndClose", "counterTransfer");
    }

    @Test void skipNBytesAndEofException() throws Exception {
        matchesJvm(FIXTURE, "skipNBytes", "eofException");
    }

    @Test void byteArrayInputStream() throws Exception {
        matchesJvm(FIXTURE, "baisRead", "baisRange", "baisBounds", "baisSkip", "baisBulk", "baisSkipNBytes", "baisTransfer", "baisMarkReset");
    }

    @Test void byteArrayInputStreamFieldsForSubclasses() throws Exception {
        matchesJvm(FIXTURE, "protectedFields", "bufferIsShared");
    }

    @Test void nullInputStream() throws Exception {
        matchesJvm(FIXTURE, "nullStreamOpen", "nullStreamClosed");
    }

    @Test void bulkReadsFollowJdkCallSequence() throws Exception {
        matchesJvm(FIXTURE, "readNBytesZeroLengthReads", "readNBytesChunks", "transferToBufferSize", "baisTransferChunks");
    }
}
