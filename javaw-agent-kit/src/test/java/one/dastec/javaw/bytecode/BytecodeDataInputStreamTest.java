package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** T25: java.io.FilterInputStream, DataInput and DataInputStream. Expected values come from the JVM; see tasks/T25-data-input-stream.md. */
@Tag("agent-pending")
class BytecodeDataInputStreamTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.io.ByteArrayInputStream;
        import java.io.ByteArrayOutputStream;
        import java.io.Closeable;
        import java.io.DataInput;
        import java.io.DataInputStream;
        import java.io.DataOutputStream;
        import java.io.EOFException;
        import java.io.FilterInputStream;
        import java.io.IOException;
        import java.io.InputStream;
        import java.io.UTFDataFormatException;

        /**
         * A chunky source that logs every call: r = read(), len@off/b.length = read(byte[],int,int), sN = skip(N),
         * av, mkN = mark(N), rs, ms, cl. Bulk reads hand out at most `step` bytes, like a network stream.
         * With ownSkip false, skip(n) is logged and then the InputStream default runs (which reads through read(byte[],int,int)).
         */
        class Logged extends InputStream {
            final StringBuilder log=new StringBuilder(); final byte[] data; final int step; final boolean ownSkip; int pos; int mark=-1;
            Logged(byte[] data,int step){this(data,step,true);}
            Logged(byte[] data,int step,boolean ownSkip){this.data=data;this.step=step;this.ownSkip=ownSkip;}
            public int read(){log.append("r ");return pos<data.length?data[pos++]&0xff:-1;}
            public int read(byte[] b,int off,int len){
                log.append(len).append('@').append(off).append('/').append(b.length).append(' ');
                if(len==0)return 0;
                if(pos>=data.length)return -1;
                int n=Math.min(Math.min(len,step),data.length-pos);
                System.arraycopy(data,pos,b,off,n);pos+=n;return n;
            }
            public long skip(long n) throws IOException {
                log.append('s').append(n).append(' ');
                if(!ownSkip)return super.skip(n);
                long k=Math.max(0,Math.min(Math.min(n,step),data.length-pos));pos+=(int)k;return k;
            }
            public int available(){log.append("av ");return data.length-pos;}
            public void mark(int limit){log.append("mk").append(limit).append(' ');mark=pos;}
            public void reset() throws IOException {log.append("rs ");if(mark<0)throw new IOException("no mark");pos=mark;}
            public boolean markSupported(){log.append("ms ");return true;}
            public void close(){log.append("cl ");}
        }
        /** skip() always answers the same value. */
        class FixedSkip extends InputStream {
            final long answer; final StringBuilder log=new StringBuilder();
            FixedSkip(long answer){this.answer=answer;}
            public int read(){return 7;}
            public long skip(long n){log.append('s').append(n).append(' ');return answer;}
        }
        /** Like Tomcat's LimitedInputStream / H2's RangeInputStream: a FilterInputStream that overrides the bulk read and counts. */
        class Counting extends FilterInputStream {
            final StringBuilder log=new StringBuilder(); int total;
            Counting(InputStream in){super(in);}
            public int read(byte[] b,int off,int len) throws IOException {
                log.append("F").append(len).append('@').append(off).append(' ');
                int n=super.read(b,off,len);if(n>0)total+=n;return n;
            }
            void swap(InputStream other){in=other;}
            InputStream current(){return in;}
        }
        /** Like Spring's StreamUtils.NonClosingInputStream: close() does nothing. */
        class NonClosing extends FilterInputStream {
            NonClosing(InputStream in){super(in);}
            public void close(){}
        }
        /** A DataInputStream subclass (like aspectj's VersionedDataInputStream) that overrides the overridable members and logs them. */
        class Spy extends DataInputStream {
            final StringBuilder log=new StringBuilder();
            Spy(InputStream in){super(in);}
            public int read() throws IOException {log.append("R ");return super.read();}
            public long skip(long n) throws IOException {log.append("S").append(n).append(' ');return super.skip(n);}
            public int available() throws IOException {log.append("A ");return super.available();}
            void swap(InputStream other){in=other;}
        }
        /** A DataInput that is not a stream (like a bcel class-file reader): logs the calls readUTF(DataInput) makes. */
        class Names implements DataInput {
            final StringBuilder log=new StringBuilder(); final byte[] data; int pos;
            Names(byte[] data){this.data=data;}
            int next() throws IOException {if(pos>=data.length)throw new EOFException("names");return data[pos++]&0xff;}
            public void readFully(byte[] b){log.append("full[").append(b.length).append("] ");}
            public void readFully(byte[] b,int off,int len) throws IOException {
                log.append("full[").append(b.length).append(',').append(off).append(',').append(len).append("] ");
                for(int i=0;i<len;i++)b[off+i]=(byte)next();
            }
            public int skipBytes(int n){log.append("skip ");return 0;}
            public boolean readBoolean(){log.append("boolean ");return false;}
            public byte readByte(){log.append("byte ");return 0;}
            public int readUnsignedByte(){log.append("ubyte ");return 0;}
            public short readShort(){log.append("short ");return 0;}
            public int readUnsignedShort() throws IOException {log.append("ushort ");return (next()<<8)|next();}
            public char readChar(){log.append("char ");return 0;}
            public int readInt(){log.append("int ");return 0;}
            public long readLong(){log.append("long ");return 0;}
            public float readFloat(){log.append("float ");return 0;}
            public double readDouble(){log.append("double ");return 0;}
            public String readLine(){log.append("line ");return null;}
            public String readUTF(){log.append("utf ");return "";}
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
            static byte[] bytes(int... v){byte[] b=new byte[v.length];for(int i=0;i<v.length;i++)b[i]=(byte)v[i];return b;}
            static String hexOf(byte[] a){StringBuilder s=new StringBuilder();for(byte x:a)s.append(Integer.toHexString((x&0xff)|0x100).substring(1));return s.toString();}
            static String units(String s){
                if(s==null)return "null";
                StringBuilder b=new StringBuilder("[");
                for(int i=0;i<s.length();i++){if(i>0)b.append(' ');b.append(Integer.toHexString(s.charAt(i)));}
                return b.append(']').toString();
            }
            static byte[] written(W w) throws IOException {ByteArrayOutputStream b=new ByteArrayOutputStream();w.run(new DataOutputStream(b));return b.toByteArray();}
            static DataInputStream dis(int... v){return new DataInputStream(new ByteArrayInputStream(bytes(v)));}

            public static String filterDelegation(){
                Logged l=new Logged(bytes(1,2,3,4,5,6,7,8,9,10),3);
                FilterInputStream f=new FilterInputStream(l){};
                byte[] b=new byte[5];
                return t(()->{
                    String s=f.read()+","+f.read(b)+","+f.read(b,1,4)+","+f.read(b,2,0)+","+f.skip(2)+","+f.available()+","+f.markSupported();
                    f.mark(42);s+=","+f.read();f.reset();s+=","+f.read();f.close();
                    return s+"|"+l.log+"|"+hexOf(b);
                });
            }
            public static String filterDefaultsGoThroughIn(){
                Logged l=new Logged(bytes(1,2,3,4,5,6,7),3);
                FilterInputStream f=new FilterInputStream(l){};
                return t(()->{
                    String s=hexOf(f.readNBytes(2))+","+hexOf(f.readAllBytes())+","+f.read();
                    return s+"|"+l.log;
                })+";"+t(()->{Logged m=new Logged(bytes(1,2,3),3,false);FilterInputStream g=new FilterInputStream(m){};return g.skip(5)+"|"+m.log;})
                  +";"+t(()->{FilterInputStream e=new FilterInputStream(new Logged(new byte[0],3)){};e.reset();return "x";});
            }
            public static String filterSubclasses(){
                Logged l=new Logged(bytes(1,2,3,4,5,6,7,8),3);
                Counting c=new Counting(l);byte[] b=new byte[4];
                String s=t(()->c.read(b)+","+c.read()+","+c.read(b,1,2)+","+hexOf(c.readNBytes(3))+",total="+c.total+"|"+c.log+"|"+l.log);
                Logged other=new Logged(bytes(9,9),3);
                s+=";"+t(()->{c.swap(other);return c.read()+","+(c.current()==other)+","+c.read(b)+","+l.log.length()+"|"+other.log;});
                Logged inner=new Logged(bytes(1),3);NonClosing n=new NonClosing(inner);
                return s+";"+t(()->{n.close();return n.read()+"|"+inner.log;});
            }
            public static String filterNullIn(){
                FilterInputStream f=new FilterInputStream(null){};
                return ty(()->f.read())+","+ty(()->f.read(new byte[2]))+","+ty(()->f.skip(1))+","+ty(()->f.available())+","+ty(()->{f.close();return "x";})
                        +","+ty(()->{f.mark(1);return "x";})+","+ty(()->f.markSupported())+","+ty(()->f.read(null));
            }

            public static String readFully(){
                Logged l=new Logged(bytes(1,2,3,4,5,6,7,8,9,10,11),3);
                DataInputStream d=new DataInputStream(l);
                byte[] b=new byte[8],c=new byte[6];
                return t(()->{d.readFully(b);return hexOf(b);})+","+t(()->{d.readFully(c,1,0);return hexOf(c);})+","+t(()->{d.readFully(c,2,3);return hexOf(c);})
                        +"|"+l.log;
            }
            public static String readFullyEof(){
                Logged l=new Logged(bytes(1,2,3,4,5),2);
                DataInputStream d=new DataInputStream(l);
                byte[] b=new byte[8];
                String s=t(()->{d.readFully(b);return "x";})+","+hexOf(b)+","+t(()->d.read())+"|"+l.log;
                DataInputStream e=dis(1,2);
                return s+";"+t(()->{e.readFully(new byte[3],0,3);return "x";})+","+t(()->{e.readFully(new byte[0]);return "ok";})+","+t(()->e.read());
            }
            public static String readFullyBounds(){
                Logged l=new Logged(bytes(1,2,3),3);
                DataInputStream d=new DataInputStream(l);byte[] b=new byte[8];
                return t(()->{d.readFully(b,-1,2);return "x";})+";"+t(()->{d.readFully(b,3,6);return "x";})+";"+t(()->{d.readFully(b,0,-1);return "x";})
                        +";"+t(()->{d.readFully(b,9,0);return "x";})+";"+t(()->{d.readFully(b,8,0);return "ok";})+";"+ty(()->{d.readFully(null);return "x";})
                        +";"+ty(()->{d.readFully(null,0,0);return "x";})+";log="+l.log;
            }
            public static String skipBytes(){
                Logged l=new Logged(bytes(1,2,3,4,5,6,7,8,9,10),3);
                DataInputStream d=new DataInputStream(l);
                String s=t(()->d.skipBytes(7)+","+d.skipBytes(0)+","+d.skipBytes(-4)+","+d.read()+","+d.skipBytes(10)+","+d.skipBytes(1)+"|"+l.log);
                Logged m=new Logged(bytes(1,2,3,4,5,6),4,false);
                DataInputStream e=new DataInputStream(m);
                s+=";"+t(()->e.skipBytes(5)+","+e.read()+"|"+m.log);
                FixedSkip z=new FixedSkip(0),neg=new FixedSkip(-5),big=new FixedSkip(0x100000002L);
                s+=";"+t(()->new DataInputStream(z).skipBytes(3)+"|"+z.log)+";"+t(()->new DataInputStream(neg).skipBytes(3)+"|"+neg.log)
                        +";"+t(()->new DataInputStream(big).skipBytes(5)+"|"+big.log);
                return s+";"+t(()->dis(1,2,3).skipBytes(Integer.MAX_VALUE));
            }

            public static String smallPrimitives(){
                return t(()->{
                    DataInputStream d=dis(0,1,2,-1,-1,0x7f,-1,0x80,0x01,0xff,0xff,0x12,0x34,0xff,0xfe,0x00,0x41);
                    return d.readBoolean()+","+d.readBoolean()+","+d.readBoolean()+","+d.readByte()+","+d.readUnsignedByte()+","+d.readByte()
                            +","+d.readUnsignedByte()+","+d.readShort()+","+d.readUnsignedShort()+","+d.readShort()+","+(int)d.readChar()+","+(int)d.readChar()+","+d.read();
                });
            }
            public static String wordPrimitives(){
                return t(()->{
                    byte[] data=written(o->{o.writeInt(0x01020304);o.writeInt(-2);o.writeLong(0x0102030405060708L);o.writeLong(Long.MIN_VALUE);o.writeLong(0xffffffffL);o.writeLong(-2L);o.writeInt(Integer.MAX_VALUE);});
                    DataInputStream d=new DataInputStream(new ByteArrayInputStream(data));
                    return Integer.toHexString(d.readInt())+","+d.readInt()+","+Long.toHexString(d.readLong())+","+d.readLong()+","+d.readLong()+","+d.readLong()+","+d.readInt()+","+d.read();
                });
            }
            public static String floatingPrimitives(){
                return t(()->{
                    byte[] data=written(o->{o.writeFloat(1.5f);o.writeFloat(-0.0f);o.writeInt(0x7fc00001);o.writeInt(0x00000001);o.writeFloat(Float.NEGATIVE_INFINITY);
                        o.writeDouble(1.0/3);o.writeDouble(-0.0);o.writeLong(0x7ff0000000000001L);o.writeDouble(Double.MAX_VALUE);});
                    DataInputStream d=new DataInputStream(new ByteArrayInputStream(data));
                    float a=d.readFloat(),b=d.readFloat(),c=d.readFloat(),e=d.readFloat(),f=d.readFloat();
                    double g=d.readDouble(),h=d.readDouble(),i=d.readDouble(),j=d.readDouble();
                    return (a==1.5f)+","+Integer.toHexString(Float.floatToIntBits(b))+","+Float.isNaN(c)+","+Integer.toHexString(Float.floatToIntBits(c))
                            +","+Integer.toHexString(Float.floatToIntBits(e))+","+(f==Float.NEGATIVE_INFINITY)
                            +","+(g==1.0/3)+","+Long.toHexString(Double.doubleToLongBits(h))+","+Double.isNaN(i)+","+Long.toHexString(Double.doubleToLongBits(i))
                            +","+(j==Double.MAX_VALUE)+","+d.read();
                });
            }
            public static String primitiveEof(){
                return t(()->dis().readBoolean())+";"+t(()->dis().readByte())+";"+t(()->dis().readUnsignedByte())+";"+t(()->dis(1).readShort())
                        +";"+t(()->dis(1).readUnsignedShort())+";"+t(()->dis(1).readChar())+";"+t(()->dis(1,2,3).readInt())+";"+t(()->dis(1,2,3,4,5,6,7).readLong())
                        +";"+t(()->dis(1,2).readFloat())+";"+t(()->dis(1,2,3,4).readDouble())
                        +";"+t(()->{DataInputStream d=dis(1,2,3);try{d.readInt();}catch(EOFException e){}return d.read()+","+d.available();})
                        +";"+ty(()->new DataInputStream(null).readInt())+";"+ty(()->new DataInputStream(null).readByte())
                        +";"+t(()->{DataInputStream d=new DataInputStream(null);d.readFully(new byte[0]);return d.skipBytes(0);});
            }
            public static String primitiveCalls(){
                Logged l=new Logged(bytes(1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33),3);
                DataInputStream d=new DataInputStream(l);
                return t(()->{
                    String s=d.readByte()+","+d.readUnsignedByte()+","+d.readBoolean()+" | "+l.log;
                    l.log.setLength(0);
                    s+=" | "+d.readShort()+","+d.readUnsignedShort()+","+(int)d.readChar()+" | "+l.log;
                    l.log.setLength(0);
                    s+=" | "+d.readInt()+","+d.readLong()+" | "+l.log;
                    l.log.setLength(0);
                    s+=" | "+Integer.toHexString(Float.floatToIntBits(d.readFloat()))+","+Long.toHexString(Double.doubleToLongBits(d.readDouble()))+" | "+l.log;
                    l.log.setLength(0);
                    return s+" | "+t(()->d.readInt())+" | "+l.log;
                });
            }

            public static String readDelegation(){
                Logged l=new Logged(bytes(1,2,3,4,5,6,7,8,9,10),3);
                DataInputStream d=new DataInputStream(l);byte[] b=new byte[6];
                String s=t(()->d.read(b)+","+d.read(b,4,2)+","+d.read(b,7,0)+","+d.read(b,1,9)+","+d.read()+","+d.skip(2)+","+d.available()+","+d.markSupported());
                s+="|"+t(()->{d.mark(9);d.read();d.reset();d.close();return d.read()+"|"+l.log;});
                DataInputStream e=dis(1,2,3,4,5);byte[] c=new byte[8];
                return s+";"+t(()->e.read(c,5,9))+";"+t(()->e.read(c,-1,1))+";"+t(()->e.read(c,0,8)+","+e.read(c)+","+e.read(c,0,0));
            }
            public static String subclassOverrides(){
                Logged l=new Logged(bytes(1,2,3,4,5,6,7,8,9,10),3);
                Spy s=new Spy(l);
                return t(()->{
                    String r=s.readByte()+","+s.readUnsignedByte()+","+s.skipBytes(4)+","+s.readShort()+","+s.read()+","+s.skip(1)+","+s.available();
                    r+="|"+s.log+"|"+l.log;
                    s.swap(new ByteArrayInputStream(bytes(0,3,0x41,0x42,0x43)));
                    return r+"|"+units(s.readUTF())+","+s.read()+"|"+s.log;
                });
            }

            public static String readUtfRoundTrip(){
                return t(()->{
                    String[] samples={"","abc","A\\0","\\u00e9\\u07ff","\\u0800\\uffff","\\ud83d\\ude00","mixed \\u00e9\\u20ac\\0 end"};
                    byte[] data=written(o->{for(String x:samples)o.writeUTF(x);});
                    DataInputStream d=new DataInputStream(new ByteArrayInputStream(data));
                    StringBuilder s=new StringBuilder();
                    for(int i=0;i<samples.length;i++){
                        String r=(i%2==0)?d.readUTF():DataInputStream.readUTF(d);
                        s.append(units(r)).append(r.equals(samples[i])?"=":"!").append(' ');
                    }
                    return s.append(d.read()).toString();
                });
            }
            public static String readUtfNonShortest(){
                return t(()->units(dis(0,2,0xc0,0x80).readUTF()))+","+t(()->units(dis(0,3,0xe0,0x80,0x80).readUTF()))+","+t(()->units(dis(0,2,0xc1,0xbf).readUTF()))
                        +","+t(()->units(dis(0,3,0xed,0xa0,0x80).readUTF()))+","+t(()->units(dis(0,6,0x41,0xc3,0xa9,0x42,0xc3,0xa9).readUTF()));
            }
            public static String readUtfMalformed(){
                return t(()->dis(0,1,0xc3).readUTF())+";"+t(()->dis(0,2,0xc3,0x41).readUTF())+";"+t(()->dis(0,3,0xe0,0x80,0x41).readUTF())
                        +";"+t(()->dis(0,3,0xe0,0x41,0x80).readUTF())+";"+t(()->dis(0,1,0x80).readUTF())+";"+t(()->dis(0,2,0x41,0xf0).readUTF())
                        +";"+t(()->dis(0,3,0x41,0x42,0xe0).readUTF())+";"+t(()->dis(0,4,0x41,0x42,0x43,0xc3).readUTF())+";"+t(()->dis(0,2,0x41,0xbf).readUTF())
                        +";"+t(()->units(dis(0,5,0x41,0x42,0xe0,0x80,0x80,0x7a).readUTF()))+";"+t(()->dis(0,4,0x41,0xe0,0x80,0xff).readUTF())
                        +";"+t(()->dis(0,3,0xff,0x41,0x42).readUTF());
            }
            public static String readUtfEof(){
                return t(()->dis().readUTF())+";"+t(()->dis(0).readUTF())+";"+t(()->dis(0,5,0x61,0x62).readUTF())
                        +";"+t(()->{DataInputStream d=dis(0,2,0xc3,0x41,0x7a);try{d.readUTF();}catch(UTFDataFormatException e){}return d.read();})
                        +";"+t(()->{DataInputStream d=dis(0,0,0x7a);return units(d.readUTF())+","+d.read();});
            }
            public static String readUtfBuffers(){
                return t(()->{
                    byte[] data=written(o->{o.writeUTF("hello");o.writeUTF("hi");o.writeUTF("");o.writeUTF("0123456789AB");o.writeUTF("abc");});
                    Logged l=new Logged(data,2);DataInputStream d=new DataInputStream(l);
                    StringBuilder s=new StringBuilder();
                    for(int i=0;i<5;i++){s.append(units(d.readUTF())).append(':').append(l.log).append("| ");l.log.setLength(0);}
                    return s.toString();
                });
            }
            public static String readUtfOtherDataInput(){
                Names n=new Names(bytes(0,3,0x41,0xc3,0xa9,0,0,0,1,0xc3));
                return t(()->units(DataInputStream.readUTF(n)))+","+t(()->units(DataInputStream.readUTF(n)))+","+t(()->DataInputStream.readUTF(n))+"|"+n.log
                        +";"+ty(()->DataInputStream.readUTF(null));
            }
            public static String dataInputInterface(){
                return t(()->{
                    byte[] data=written(o->{o.writeByte(0xef);o.writeShort(0xbbbf);o.writeBoolean(true);o.writeInt(-7);o.writeLong(99);o.writeChar('Z');
                        o.writeFloat(2.5f);o.writeDouble(-4.25);o.writeUTF("x\\u00e9");o.writeInt(5);o.writeShort(-3);o.write(new byte[]{9,8});o.writeByte(4);});
                    DataInput in=new DataInputStream(new ByteArrayInputStream(data));
                    String s=in.readUnsignedByte()+","+in.readUnsignedShort()+","+in.readBoolean()+","+in.readInt()+","+in.readLong()+","+in.readChar()
                            +","+(in.readFloat()==2.5f)+","+(in.readDouble()==-4.25)+","+units(in.readUTF())+","+in.skipBytes(4)+","+in.readShort();
                    byte[] b=new byte[3];in.readFully(b,1,2);
                    byte[] c=new byte[1];in.readFully(c);
                    return s+","+hexOf(b)+","+hexOf(c)+","+t(()->in.readByte());
                });
            }
            public static String typesAndExceptions(){
                DataInputStream d=dis(1);
                FilterInputStream f=new FilterInputStream(new ByteArrayInputStream(new byte[0])){};
                Object o=d,g=f;
                return (o instanceof DataInput)+","+(o instanceof FilterInputStream)+","+(o instanceof InputStream)+","+(o instanceof Closeable)
                        +","+(g instanceof DataInput)+","+(g instanceof FilterInputStream)
                        +"|"+t(()->{throw new UTFDataFormatException("u");})+","+(new UTFDataFormatException() instanceof IOException)
                        +","+t(()->dis().readInt())+","+t(()->{try{dis().readLong();return "x";}catch(IOException e){return e.getClass().getName()+":"+(e instanceof EOFException);}});
            }
        }
        """;

    @Test void filterInputStreamDelegation() throws Exception {
        matchesJvm(FIXTURE, "filterDelegation", "filterDefaultsGoThroughIn", "filterSubclasses", "filterNullIn");
    }

    @Test void readFullyAndSkipBytes() throws Exception {
        matchesJvm(FIXTURE, "readFully", "readFullyEof", "readFullyBounds", "skipBytes");
    }

    @Test void primitiveReads() throws Exception {
        matchesJvm(FIXTURE, "smallPrimitives", "wordPrimitives", "floatingPrimitives", "primitiveEof", "primitiveCalls");
    }

    @Test void readDelegationAndSubclasses() throws Exception {
        matchesJvm(FIXTURE, "readDelegation", "subclassOverrides");
    }

    @Test void readUtf() throws Exception {
        matchesJvm(FIXTURE, "readUtfRoundTrip", "readUtfNonShortest", "readUtfMalformed", "readUtfEof");
    }

    @Test void readUtfBuffersAndDataInput() throws Exception {
        matchesJvm(FIXTURE, "readUtfBuffers", "readUtfOtherDataInput", "dataInputInterface", "typesAndExceptions");
    }

    @Test void leavesOutReadLine() {
        rejectsUnusedCall("java/io/DataInputStream", "readLine", "()Ljava/lang/String;", INVOKEVIRTUAL, false);
    }
}
