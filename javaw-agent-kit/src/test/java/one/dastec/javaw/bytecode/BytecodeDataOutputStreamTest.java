package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** T24: java.io.FilterOutputStream, DataOutput, DataOutputStream and UTFDataFormatException. Expected values come from the JVM; see tasks/T24-data-output-stream.md. */
@Tag("agent-pending")
class BytecodeDataOutputStreamTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.io.ByteArrayOutputStream;
        import java.io.Closeable;
        import java.io.DataOutput;
        import java.io.DataOutputStream;
        import java.io.FilterOutputStream;
        import java.io.Flushable;
        import java.io.IOException;
        import java.io.OutputStream;
        import java.io.UTFDataFormatException;
        import java.util.Arrays;

        /** Logs every call: wN = write(int N), aL,off,len:hex = write(byte[] of length L, off, len), flush, close. */
        class Rec extends OutputStream {
            final StringBuilder log=new StringBuilder();
            public void write(int b){log.append('w').append(b).append(' ');}
            public void write(byte[] b,int off,int len){
                log.append('a').append(b.length).append(',').append(off).append(',').append(len).append(':');
                for(int i=0;i<len&&off>=0&&off+i<b.length;i++)log.append(Integer.toHexString(b[off+i]&0xff)).append('.');
                log.append(' ');
            }
            public void flush(){log.append("flush ");}
            public void close(){log.append("close ");}
        }
        /** Throws onWrite from every write after okWrites successful write(int) calls, onFlush from flush, onClose from close. */
        class Bad extends OutputStream {
            final StringBuilder log=new StringBuilder(); final Exception onWrite,onFlush,onClose; int okWrites;
            Bad(Exception onWrite,Exception onFlush,Exception onClose){this.onWrite=onWrite;this.onFlush=onFlush;this.onClose=onClose;}
            static void raise(Exception e) throws IOException {if(e instanceof IOException)throw (IOException)e;if(e!=null)throw (RuntimeException)e;}
            public void write(int b) throws IOException {log.append('w').append(b).append(' ');if(okWrites>0){okWrites--;return;}raise(onWrite);}
            public void write(byte[] b,int off,int len) throws IOException {log.append('a').append(len).append(' ');raise(onWrite);}
            public void flush() throws IOException {log.append("flush ");raise(onFlush);}
            public void close() throws IOException {log.append("close ");raise(onClose);}
        }
        /** A byte-transforming filter that overrides only write(int) (adds 1) and flush. */
        class Shift extends FilterOutputStream {
            final StringBuilder log=new StringBuilder();
            Shift(OutputStream out){super(out);}
            public void write(int b) throws IOException {log.append('u').append(b).append(' ');out.write(b+1);}
            public void flush() throws IOException {log.append("F ");super.flush();}
        }
        /** Like Spring's StreamUtils.NonClosingOutputStream: bulk writes go straight to out and close() does nothing. */
        class NonClosing extends FilterOutputStream {
            NonClosing(OutputStream out){super(out);}
            public void write(byte[] b,int off,int len) throws IOException {out.write(b,off,len);}
            public void close(){}
        }
        /** Records the bulk calls that reach FilterOutputStream.write(byte[],int,int), then lets the JDK version run. */
        class BulkSpy extends FilterOutputStream {
            final StringBuilder log=new StringBuilder();
            BulkSpy(OutputStream out){super(out);}
            public void write(byte[] b,int off,int len) throws IOException {log.append('s').append(off).append(',').append(len).append(' ');super.write(b,off,len);}
        }
        /** Starts with a null stream and assigns the protected `out` field later. */
        class Swap extends FilterOutputStream {
            Swap(){super(null);}
            void to(OutputStream o){out=o;}
            OutputStream current(){return out;}
        }
        /** A DataOutputStream subclass that reads and assigns `written` and records what its overridable methods receive. */
        class Spy extends DataOutputStream {
            final StringBuilder log=new StringBuilder(); boolean swallow;
            Spy(OutputStream out){super(out);}
            void set(int w){written=w;}
            int raw(){return written;}
            public void write(int b) throws IOException {log.append('W').append(b).append(' ');super.write(b);}
            public void write(byte[] b,int off,int len) throws IOException {
                log.append('A').append(b.length).append(',').append(off).append(',').append(len).append(' ');
                if(!swallow)super.write(b,off,len);
            }
            public void flush() throws IOException {log.append("F ");super.flush();}
        }
        /** A DataOutput that is not a stream: logs the name and arguments of every call. */
        class Names implements DataOutput {
            final StringBuilder log=new StringBuilder();
            public void write(int b){log.append("write(").append(b).append(") ");}
            public void write(byte[] b){log.append("write[").append(b.length).append("] ");}
            public void write(byte[] b,int off,int len){log.append("write[").append(off).append(',').append(len).append("] ");}
            public void writeBoolean(boolean v){log.append("boolean ").append(v).append(' ');}
            public void writeByte(int v){log.append("byte ").append(v).append(' ');}
            public void writeShort(int v){log.append("short ").append(v).append(' ');}
            public void writeChar(int v){log.append("char ").append(v).append(' ');}
            public void writeInt(int v){log.append("int ").append(v).append(' ');}
            public void writeLong(long v){log.append("long ").append(v).append(' ');}
            public void writeFloat(float v){log.append("float ").append(v).append(' ');}
            public void writeDouble(double v){log.append("double ").append(v).append(' ');}
            public void writeBytes(String s){log.append("bytes ").append(s).append(' ');}
            public void writeChars(String s){log.append("chars ").append(s).append(' ');}
            public void writeUTF(String s){log.append("utf ").append(s).append(' ');}
        }

        public class Fixture {
            interface Op{Object run() throws Exception;}
            interface W{void run(DataOutputStream d) throws IOException;}
            static String t(Op op){
                try{return String.valueOf(op.run());}catch(Exception e){return e.getClass().getSimpleName()+":"+e.getMessage();}
            }
            static String ty(Op op){
                try{return String.valueOf(op.run());}catch(Exception e){return e.getClass().getSimpleName();}
            }
            static String hexOf(byte[] a){StringBuilder s=new StringBuilder();for(byte x:a)s.append(Integer.toHexString((x&0xff)|0x100).substring(1));return s.toString();}
            /** Runs w on a DataOutputStream over a ByteArrayOutputStream: "size:hex", or "Exception:message/size/hex". */
            static String data(W w){
                ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(b);
                try{w.run(d);}
                catch(Exception e){return e.getClass().getSimpleName()+(e instanceof NullPointerException?"":":"+e.getMessage())+"/"+d.size()+"/"+hexOf(b.toByteArray());}
                return d.size()+":"+hexOf(b.toByteArray());
            }
            static String closeResult(OutputStream s){
                try{s.close();return "ok";}
                catch(Exception e){
                    StringBuilder b=new StringBuilder(e.getClass().getSimpleName()+":"+e.getMessage()+" [");
                    for(Throwable x:e.getSuppressed())b.append(x.getClass().getSimpleName()).append(':').append(x.getMessage());
                    return b.append(']').toString();
                }
            }
            static String filled(String head,char c,int n,String tail){char[] a=new char[n];Arrays.fill(a,c);return head+new String(a)+tail;}

            public static String filterWrites(){
                Rec r=new Rec();FilterOutputStream f=new FilterOutputStream(r);
                return t(()->{f.write(65);f.write(300);f.write(-1);f.write(new byte[]{1,-2,127});f.write(new byte[]{9,8,7,6},1,2);
                    f.write(new byte[0]);f.write(new byte[3],3,0);f.flush();return r.log;});
            }
            public static String filterRangeChecks(){
                Rec r=new Rec();FilterOutputStream f=new FilterOutputStream(r);byte[] b=new byte[4];
                return t(()->{f.write(b,-1,1);return "x";})+";"+t(()->{f.write(b,2,3);return "x";})+";"+t(()->{f.write(b,0,-1);return "x";})
                        +";"+t(()->{f.write(b,5,0);return "x";})+";"+t(()->{f.write(b,1,Integer.MAX_VALUE);return "x";})
                        +";"+ty(()->{f.write(null);return "x";})+";"+ty(()->{f.write(null,0,0);return "x";})+";log="+r.log;
            }
            public static String filterSubclasses(){
                Rec r=new Rec();Shift s=new Shift(r);
                String a=t(()->{s.write(new byte[]{97,98});s.write(new byte[]{1,2,3},2,1);s.write(-1);s.flush();return s.log+"|"+r.log;});
                Rec q=new Rec();BulkSpy b=new BulkSpy(q);
                String c=t(()->{b.write(new byte[]{1,2,3});b.write(new byte[]{4,5},1,1);b.write(6);return b.log+"|"+q.log;});
                Rec p=new Rec();NonClosing n=new NonClosing(p);
                String d=t(()->{n.write(new byte[]{1,2});n.write(new byte[]{3,4,5},1,2);n.close();n.write(9);return p.log;});
                return a+";"+c+";"+d;
            }
            public static String filterOutFieldAndNull(){
                Rec r=new Rec();FilterOutputStream f=new FilterOutputStream(null);
                String s=ty(()->{f.write(1);return "x";})+","+ty(()->{f.flush();return "x";});
                try{f.close();s+=",ok";}catch(Exception e){s+=","+e.getClass().getSimpleName()+"+"+e.getSuppressed().length+(e.getSuppressed().length>0?e.getSuppressed()[0].getClass().getSimpleName():"");}
                s+=","+closeResult(f);
                Rec q=new Rec();Swap w=new Swap();
                return s+"|"+(w.current()==null)+","+t(()->{w.to(r);w.write(5);w.close();w.write(6);w.to(q);w.close();w.write(7);return r.log+"/"+q.log+(w.current()==q);});
            }

            public static String closeSequence(){
                Rec r=new Rec();FilterOutputStream f=new FilterOutputStream(r);
                String s=closeResult(f)+":"+r.log+"|"+closeResult(f)+":"+r.log;
                Rec q=new Rec();Shift h=new Shift(q);
                s+="|"+closeResult(h)+":"+h.log+q.log+"|"+closeResult(h)+":"+h.log+q.log;
                Rec p=new Rec();DataOutputStream d=new DataOutputStream(p);
                s+="|"+closeResult(d)+":"+p.log+"|"+t(()->{d.writeByte(1);d.flush();return p.log;});
                return s;
            }
            public static String closeFailures(){
                IOException same=new IOException("same");
                Bad[] all={new Bad(null,new IOException("flush"),null),new Bad(null,null,new IOException("close")),
                    new Bad(null,new IOException("flush"),new IOException("close")),new Bad(null,same,same),
                    new Bad(null,new IllegalStateException("rt"),new IOException("close")),new Bad(null,new IOException("flush"),new IllegalArgumentException("arg")),
                    new Bad(null,new IllegalStateException("rt"),null)};
                StringBuilder out=new StringBuilder();
                for(Bad b:all){FilterOutputStream f=new FilterOutputStream(b);out.append(closeResult(f)).append(" / ").append(closeResult(f)).append(" / ").append(b.log).append(';');}
                return out.toString();
            }
            public static String closeFailuresThroughSubclasses(){
                Bad a=new Bad(null,null,new IOException("close"));Shift s=new Shift(a);
                Bad b=new Bad(null,new IOException("flush"),new IOException("close"));DataOutputStream d=new DataOutputStream(b);
                return closeResult(s)+"/"+s.log+a.log+"/"+closeResult(s)+";"+closeResult(d)+"/"+b.log+"/"+closeResult(d)+"/"+b.log;
            }

            public static String smallPrimitives(){
                return data(d->{d.writeBoolean(true);d.writeBoolean(false);})+";"+data(d->{d.writeByte(0x1ff);d.writeByte(-128);d.writeByte(65);})
                        +";"+data(d->{d.writeShort(0x12345);d.writeShort(-2);d.writeShort(32768);})
                        +";"+data(d->{d.writeChar('\\u00e9');d.writeChar(0x1ffff);d.writeChar(-1);d.writeChar('A');})
                        +";"+data(d->{d.write(0x180);d.write(new byte[]{1,2});d.write(new byte[]{3,4,5,6},1,2);});
            }
            public static String wordPrimitives(){
                return data(d->{d.writeInt(0x01020304);d.writeInt(-1);d.writeInt(Integer.MIN_VALUE);})
                        +";"+data(d->{d.writeLong(0x0102030405060708L);d.writeLong(-2L);d.writeLong(Long.MIN_VALUE);});
            }
            public static String floatingPrimitives(){
                return data(d->{d.writeFloat(1.5f);d.writeFloat(-0.0f);d.writeFloat(Float.intBitsToFloat(0x7fc00001));d.writeFloat(Float.POSITIVE_INFINITY);d.writeFloat(Float.MIN_VALUE);})
                        +";"+data(d->{d.writeDouble(1.0/3);d.writeDouble(-0.0);d.writeDouble(Double.longBitsToDouble(0x7ff8000000000001L));d.writeDouble(Double.NEGATIVE_INFINITY);});
            }
            public static String sinkRangeFailures(){
                return data(d->{d.writeByte(1);d.write(new byte[3],2,2);})+";"+data(d->d.write(new byte[3],-1,1))+";"+data(d->d.write(new byte[3],0,-1))
                        +";"+data(d->d.write(null))+";"+ty(()->{new DataOutputStream(new ByteArrayOutputStream()).write(null,0,0);return "x";});
            }

            public static String primitiveCalls(){
                Rec r=new Rec();DataOutputStream d=new DataOutputStream(r);
                return t(()->{d.writeBoolean(true);d.writeBoolean(false);d.writeByte(300);d.writeShort(0x1234);d.writeChar('A');d.writeInt(1);d.writeLong(1);d.writeFloat(1f);d.writeDouble(1d);
                    d.write(200);d.write(new byte[]{5,6,7});d.write(new byte[]{1,2,3,4},1,2);d.flush();return d.size()+":"+r.log;});
            }
            public static String stringCalls(){
                Rec r=new Rec();DataOutputStream d=new DataOutputStream(r);
                return t(()->{d.writeBytes("A\\u00e9\\u0100\\uffff");d.writeChars("a\\u00e9\\u20ac");d.writeBytes("");d.writeChars("");return d.size()+":"+r.log;});
            }
            public static String utfBufferCalls(){
                Rec r=new Rec();DataOutputStream d=new DataOutputStream(r);
                return t(()->{d.writeUTF("abc");d.writeUTF("hello, world");d.writeUTF("hi");d.writeUTF("");d.writeUTF("\\u00e9\\u00e9\\u00e9\\u00e9\\u00e9\\u00e9");d.writeUTF("hello, world!");
                    return d.size()+":"+r.log;});
            }
            public static String spyCalls(){
                Rec r=new Rec();Spy s=new Spy(r);
                return t(()->{s.writeInt(7);s.writeByte(1);s.writeBoolean(true);s.write(new byte[]{1,2});s.write(9);s.writeUTF("ab");s.writeBytes("xy");s.writeChars("z");s.flush();
                    String before=s.log+"|"+r.log+"|"+s.size();s.close();return before+"|"+s.log+"|"+r.log;});
            }
            public static String spySwallowsBulk(){
                Rec r=new Rec();Spy s=new Spy(r);s.swallow=true;
                return t(()->{s.writeUTF("abc");String a=s.size()+"";s.writeShort(1);s.write(new byte[3]);s.write(new byte[]{1,2,3},1,1);
                    return a+","+s.size()+","+s.raw()+"|"+s.log+"|"+r.log;});
            }

            public static String utfEncoding(){
                return data(d->d.writeUTF("A\\0"))+";"+data(d->d.writeUTF("\\u00e9\\u07ff"))+";"+data(d->d.writeUTF("\\u0800\\uffff"))
                        +";"+data(d->d.writeUTF("\\ud83d\\ude00"))+";"+data(d->d.writeUTF("ab\\u00e9cd\\u0000z"))+";"+data(d->d.writeUTF(""))+";"+data(d->d.writeUTF("\\u007f\\u0080"));
            }
            public static String utfLimits(){
                String max=filled("",'a',65535,""),ok2=filled("abcdefgh",'\\0',32759,"stuvwxyz"),ok3=filled("",'\\uffff',21845,"");
                String r1=t(()->{ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(b);d.writeUTF(max);byte[] a=b.toByteArray();return d.size()+"/"+a.length+"/"+hexOf(Arrays.copyOf(a,4))+"/"+hexOf(Arrays.copyOfRange(a,a.length-2,a.length));});
                String r2=t(()->{ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(b);d.writeUTF(ok2);byte[] a=b.toByteArray();return d.size()+"/"+hexOf(Arrays.copyOf(a,13))+"/"+hexOf(Arrays.copyOfRange(a,a.length-9,a.length));});
                String r3=t(()->{ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(b);d.writeUTF(ok3);byte[] a=b.toByteArray();return d.size()+"/"+hexOf(Arrays.copyOf(a,8));});
                return r1+";"+r2+";"+r3;
            }
            public static String utfTooLong(){
                String a=filled("",'a',65536,""),z=filled("abcdefgh",'\\0',32760,"stuvwxyz"),three=filled("abcdefgh",'\\u0800',21840,"stuvwxyz"),big=filled("HEAD1234",'\\u20ac',40000,"5678TAIL");
                return data(d->d.writeUTF(a))+";"+data(d->d.writeUTF(z))+";"+data(d->{d.writeInt(258);d.writeUTF(three);})+";"+data(d->d.writeUTF(big))
                        +";"+t(()->{try{new DataOutputStream(new Rec()).writeUTF(a);return "x";}catch(UTFDataFormatException e){return "UTFDataFormatException is IOException: "+(e instanceof IOException);}});
            }
            public static String stringNulls(){
                return data(d->d.writeUTF(null))+","+data(d->{d.writeByte(1);d.writeBytes(null);})+","+data(d->d.writeChars(null))
                        +","+t(()->{DataOutputStream d=new DataOutputStream(new Rec());try{d.writeUTF(null);}catch(NullPointerException e){}return d.size();});
            }

            public static String sizeOverflow(){
                Rec r=new Rec();Spy s=new Spy(r);
                return t(()->{StringBuilder o=new StringBuilder();
                    s.set(Integer.MAX_VALUE-3);s.writeInt(1);o.append(s.size()).append(',');
                    s.writeByte(1);o.append(s.size()).append(',');
                    s.set(Integer.MAX_VALUE-4);s.writeInt(1);o.append(s.size()).append(',');
                    s.set(-5);s.writeByte(1);o.append(s.size()).append(',');
                    s.set(10);s.write(new byte[2],0,-1);o.append(s.size()).append(',');
                    s.set(0);s.write(new byte[2],0,-1);o.append(s.size()).append(',').append(s.raw());
                    return o.toString();});
            }
            public static String negativeLengthPassesThrough(){
                Rec r=new Rec();DataOutputStream d=new DataOutputStream(r);
                return t(()->{d.write(new byte[3],1,5);String a=d.size()+"";d.write(new byte[3],0,-1);return a+","+d.size()+"|"+r.log;});
            }
            public static String sizeAfterFailures(){
                Bad a=new Bad(new IOException("full"),null,null);DataOutputStream d=new DataOutputStream(a);
                String s=t(()->{d.writeInt(1);return "x";})+","+d.size()+","+t(()->{d.writeByte(1);return "x";})+","+d.size()+","+t(()->{d.writeUTF("hi");return "x";})+","+d.size()
                        +","+t(()->{d.write(1);return "x";})+","+t(()->{d.write(new byte[2]);return "x";})+","+d.size()+"|"+a.log;
                Bad b=new Bad(new IllegalStateException("third"),null,null);b.okWrites=2;DataOutputStream e=new DataOutputStream(b);
                return s+"|"+t(()->{e.writeBytes("abcd");return "x";})+","+e.size()+"|"+b.log+"|"+t(()->{e.writeBoolean(true);return e.size();});
            }

            public static String dataOutputInterface(){
                Names n=new Names();DataOutput o=n;
                String s=t(()->{o.write(1);o.write(new byte[3]);o.write(new byte[3],1,2);o.writeBoolean(true);o.writeByte(2);o.writeShort(3);o.writeChar(65);o.writeInt(4);
                    o.writeLong(5L);o.writeFloat(1.5f);o.writeDouble(2.5);o.writeBytes("b");o.writeChars("c");o.writeUTF("u");return n.log;});
                ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutput d=new DataOutputStream(b);
                return s+"|"+t(()->{d.writeShort(258);d.write(new byte[]{1});d.write(new byte[]{1,2,3},1,1);d.write(7);d.writeUTF("x");d.writeBoolean(false);d.writeChars("A");
                    d.writeBytes("B");d.writeChar(67);d.writeByte(68);d.writeInt(5);d.writeLong(6);d.writeFloat(0f);d.writeDouble(0d);return hexOf(b.toByteArray());});
            }
            public static String typesAndException(){
                Object d=new DataOutputStream(new Rec());
                UTFDataFormatException a=new UTFDataFormatException(),b=new UTFDataFormatException("bad");Exception e=b;
                return (d instanceof DataOutput)+","+(d instanceof FilterOutputStream)+","+(d instanceof OutputStream)+","+(d instanceof Closeable)+","+(d instanceof Flushable)
                        +","+(new FilterOutputStream(new Rec()) instanceof DataOutput)+"|"+a.getMessage()+","+b.getMessage()+","+(e instanceof IOException)+","+t(()->{throw new UTFDataFormatException("x");});
            }
        }
        """;

    @Test void filterOutputStreamWrites() throws Exception {
        matchesJvm(FIXTURE, "filterWrites", "filterRangeChecks", "filterSubclasses", "filterOutFieldAndNull");
    }

    @Test void filterOutputStreamClose() throws Exception {
        matchesJvm(FIXTURE, "closeSequence", "closeFailures", "closeFailuresThroughSubclasses");
    }

    @Test void dataOutputStreamPrimitives() throws Exception {
        matchesJvm(FIXTURE, "smallPrimitives", "wordPrimitives", "floatingPrimitives", "sinkRangeFailures");
    }

    @Test void dataOutputStreamCallPattern() throws Exception {
        matchesJvm(FIXTURE, "primitiveCalls", "stringCalls", "utfBufferCalls", "spyCalls", "spySwallowsBulk");
    }

    @Test void writeUtf() throws Exception {
        matchesJvm(FIXTURE, "utfEncoding", "utfLimits", "utfTooLong", "stringNulls");
    }

    @Test void sizeCounter() throws Exception {
        matchesJvm(FIXTURE, "sizeOverflow", "negativeLengthPassesThrough", "sizeAfterFailures");
    }

    @Test void dataOutputInterfaceAndException() throws Exception {
        matchesJvm(FIXTURE, "dataOutputInterface", "typesAndException");
    }
}
