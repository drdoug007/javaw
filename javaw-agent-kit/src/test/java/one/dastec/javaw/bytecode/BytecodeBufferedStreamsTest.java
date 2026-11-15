package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** T33: java.io.BufferedInputStream and java.io.BufferedOutputStream. Expected values come from the JVM; see tasks/T33-buffered-streams.md. */
@Tag("agent-pending")
class BytecodeBufferedStreamsTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.io.BufferedInputStream;
        import java.io.BufferedOutputStream;
        import java.io.ByteArrayOutputStream;
        import java.io.FilterInputStream;
        import java.io.FilterOutputStream;
        import java.io.IOException;
        import java.io.InputStream;
        import java.io.OutputStream;

        /** A source over the chars of `data` (one byte each) that hands out at most `step` bytes per bulk read and logs
            every call: `r` read(), `len@off/b.length=n` read(byte[],int,int), `av=n` available(), `sN=k` skip(N),
            `xfer` transferTo, `close`. */
        class Src extends InputStream {
            final StringBuilder log=new StringBuilder(); final String data; final int step; int at; int avail=-1; int failAt=-1; int calls; long xfer=-1; boolean failClose;
            Src(String data,int step){this.data=data;this.step=step;}
            public int read(){log.append("r ");return at<data.length()?data.charAt(at++)&0xff:-1;}
            public int read(byte[] b,int off,int len) throws IOException {
                log.append(len).append('@').append(off).append('/').append(b.length);
                if(++calls==failAt){log.append("=fail ");throw new IOException("source failed");}
                int n;
                if(len==0)n=0;else if(at>=data.length())n=-1;
                else{n=Math.min(Math.min(len,step),data.length()-at);for(int i=0;i<n;i++)b[off+i]=(byte)data.charAt(at++);}
                log.append('=').append(n).append(' ');return n;
            }
            public int available(){int n=avail>=0?avail:data.length()-at;log.append("av=").append(n).append(' ');return n;}
            public long skip(long n){long k=Math.max(0,Math.min(n,data.length()-at));at+=(int)k;log.append('s').append(n).append('=').append(k).append(' ');return k;}
            public long transferTo(OutputStream out) throws IOException {log.append("xfer ");return xfer>=0?xfer:super.transferTo(out);}
            public void close() throws IOException {log.append("close ");if(failClose)throw new IOException("close failed");}
        }
        /** A BufferedInputStream subclass that shows the protected fields and logs calls of its available(). */
        class Peek extends BufferedInputStream {
            final StringBuilder plog=new StringBuilder();
            public int available() throws IOException {plog.append("P ");return super.available();}
            Peek(InputStream in){super(in);}
            Peek(InputStream in,int size){super(in,size);}
            String st(){return "[buf="+(buf==null?"null":String.valueOf(buf.length))+" count="+count+" pos="+pos+" markpos="+markpos+" marklimit="+marklimit+" in="+(in==null?"null":"set")+"]";}
            String content(){StringBuilder s=new StringBuilder("{");for(int i=0;i<count;i++)s.append((char)(buf[i]&0xff));return s.append('}').toString();}
        }
        /** Logs wN for write(int), aLEN@OFF/LENGTH:chars for write(byte[],int,int) (chars only when len <= 16), flush, close. */
        class Sink extends OutputStream {
            final StringBuilder log=new StringBuilder(); int failWrites; boolean failFlush,failClose;
            public void write(int b){log.append('w').append(b).append(' ');}
            public void write(byte[] b,int off,int len) throws IOException {
                log.append('a').append(len).append('@').append(off).append('/').append(b.length);
                if(len<=16&&off>=0&&len>=0&&len<=b.length-off){log.append(':');for(int i=0;i<len;i++){int c=b[off+i]&0xff;log.append(c<32?'.':(char)c);}}
                log.append(' ');
                if(failWrites>0){failWrites--;throw new IOException("write failed");}
            }
            public void flush() throws IOException {log.append("flush ");if(failFlush)throw new IOException("flush failed");}
            public void close() throws IOException {log.append("close ");if(failClose)throw new IOException("close failed");}
        }
        /** A BufferedOutputStream subclass that shows buf/count, can replace buf, and logs its overridable methods. */
        class OSpy extends BufferedOutputStream {
            final StringBuilder log=new StringBuilder();
            OSpy(OutputStream out){super(out);}
            OSpy(OutputStream out,int size){super(out,size);}
            String st(){return "[buf="+buf.length+" count="+count+"]";}
            void replace(int n){buf=new byte[n];}
            public void write(int b) throws IOException {log.append('W').append(b).append(' ');super.write(b);}
            public void write(byte[] b,int off,int len) throws IOException {log.append('A').append(len).append('@').append(off).append(' ');super.write(b,off,len);}
            public void flush() throws IOException {log.append("F ");super.flush();}
        }

        public class Fixture {
            interface Op{Object run() throws Exception;}
            static String t(Op op){
                try{return String.valueOf(op.run());}catch(Exception e){return e.getClass().getSimpleName()+":"+e.getMessage();}
            }
            static String ty(Op op){
                try{return String.valueOf(op.run());}catch(Exception e){return e.getClass().getSimpleName();}
            }
            static String chars(byte[] b,int off,int n){StringBuilder s=new StringBuilder("'");for(int i=0;i<n;i++)s.append((char)(b[off+i]&0xff));return s.append('\\'').toString();}
            /** n calls of read(): letters as themselves, other values as [N]. */
            static String reads(InputStream in,int n) throws IOException {
                StringBuilder s=new StringBuilder();
                for(int i=0;i<n;i++){int c=in.read();if(c>='a'&&c<='z')s.append((char)c);else s.append('[').append(c).append(']');}
                return s.toString();
            }
            static String reset(InputStream in){
                try{in.reset();return " reset ";}catch(IOException e){return " "+e.getMessage()+" ";}
            }
            static String closeResult(OutputStream s){
                try{s.close();return "ok";}
                catch(Exception e){
                    StringBuilder b=new StringBuilder(e.getClass().getSimpleName()+":"+e.getMessage()+" [");
                    for(Throwable x:e.getSuppressed())b.append(x.getClass().getSimpleName()).append(':').append(x.getMessage());
                    return b.append(']').toString();
                }
            }

            public static String readSingleBytes(){
                Src s=new Src("abcdefghij",3);BufferedInputStream b=new BufferedInputStream(s,4);
                String a=t(()->reads(b,12)+"|"+s.log);
                Src h=new Src("\\u00ff\\u0080\\u007f",8);BufferedInputStream c=new BufferedInputStream(h,2);
                return a+";"+t(()->reads(c,4)+"|"+h.log);
            }
            public static String defaultSize(){
                Src s=new Src("xyz",100);BufferedInputStream b=new BufferedInputStream(s);
                Src q=new Src("klmnop",100);Peek p=new Peek(q);Peek r=new Peek(new Src("",1),3);
                return t(()->reads(b,4)+"|"+s.log)+";"+p.st()+r.st()+t(()->reads(p,2)+p.st()+p.content()+q.log);
            }
            public static String bulkReads(){
                Src s=new Src("abcdefghijklmnop",3);BufferedInputStream b=new BufferedInputStream(s,4);byte[] x=new byte[8];
                String r=t(()->{StringBuilder o=new StringBuilder();
                    o.append(b.read(x,0,2)).append(chars(x,0,2)).append(' ');
                    o.append(b.read(x,0,5)).append(chars(x,0,5)).append(' ');
                    o.append(b.read(x,0,0)).append(' ');
                    o.append(b.read(x,2,6)).append(chars(x,2,6)).append(' ');
                    o.append(b.read(x)).append(' ');
                    o.append(b.read(x,0,8)).append(' ');
                    return o+"|"+s.log;});
                Src z=new Src("abcdefghij",3);z.avail=0;BufferedInputStream c=new BufferedInputStream(z,2);byte[] y=new byte[10];
                String q=t(()->c.read(y,0,10)+chars(y,0,3)+","+c.read(y,0,1)+chars(y,0,1)+","+c.read(y,0,4)+chars(y,0,4)+"|"+z.log);
                Src u=new Src("abcdefghij",4);Peek p=new Peek(u,4);byte[] w=new byte[6];
                String v=t(()->{int a=p.read();int n=p.read(w,0,6);return a+","+n+chars(w,0,n)+p.st()+"|"+u.log+"|own available() calls: "+p.plog+"|"+reads(p,1)+p.available()+p.plog;});
                return r+";"+q+";"+v;
            }
            public static String bulkReadChecks(){
                Src s=new Src("abcdef",8);BufferedInputStream b=new BufferedInputStream(s,4);byte[] x=new byte[4];
                String r=t(()->{b.read(x,-1,1);return "x";})+";"+t(()->{b.read(x,0,-1);return "x";})+";"+t(()->{b.read(x,3,2);return "x";})
                    +";"+t(()->{b.read(x,5,0);return "x";})+";"+t(()->{b.read(x,1,Integer.MAX_VALUE);return "x";})+";"+t(()->b.read(x,4,0))
                    +";"+ty(()->b.read(null,0,1))+";"+ty(()->b.read(null))+";"+ty(()->b.read(null,0,0));
                return r+"|"+s.log;
            }

            public static String skipAndAvailable(){
                Src s=new Src("abcdefghijklmnopqrst",4);BufferedInputStream b=new BufferedInputStream(s,4);
                String r=t(()->{StringBuilder o=new StringBuilder();
                    o.append((char)b.read()).append(' ');
                    o.append(b.available()).append(' ');
                    o.append(b.skip(10)).append(' ');
                    o.append(b.skip(5)).append(' ');
                    o.append(b.skip(0)).append(',').append(b.skip(-3)).append(' ');
                    b.mark(100);
                    o.append(b.skip(2)).append(' ');
                    o.append((char)b.read()).append(' ');
                    b.reset();o.append((char)b.read()).append(' ');
                    o.append(b.skip(100)).append(' ');
                    o.append(b.skip(100)).append(' ');
                    o.append(b.available()).append(' ');
                    return o+"|"+s.log;});
                Src e=new Src("abc",4);BufferedInputStream f=new BufferedInputStream(e,2);
                String g=t(()->f.skip(5)+","+f.skip(5)+","+f.read()+","+f.available()+"|"+e.log);
                Src m=new Src("abcdefgh",4);BufferedInputStream c=new BufferedInputStream(m,4);
                String q=t(()->{c.read();c.read();StringBuilder o=new StringBuilder();
                    m.avail=Integer.MAX_VALUE;o.append(c.available()).append(',');
                    m.avail=Integer.MAX_VALUE-2;o.append(c.available()).append(',');
                    m.avail=Integer.MAX_VALUE-3;o.append(c.available()).append(',');
                    m.avail=0;o.append(c.available());return o+"|"+m.log;});
                return r+";"+g+";"+q;
            }

            public static String markAndReset(){
                Src s=new Src("abcdefghijklmnop",4);Peek p=new Peek(s,4);
                StringBuilder o=new StringBuilder();
                o.append(t(()->{p.reset();return "x";})).append(' ').append(p.markSupported()).append(p.st()).append(' ');
                o.append(t(()->{p.read();p.mark(10);p.read();p.read();return p.st();})).append(' ');
                o.append(t(()->{p.read();p.read();return p.st()+p.content();})).append(' ');
                o.append(t(()->{p.reset();return reads(p,4)+p.st();})).append(' ');
                o.append(t(()->reads(p,3)+p.st()+p.content())).append(' ');
                o.append(t(()->{p.reset();return reads(p,10)+p.st();})).append(' ');
                o.append(t(()->{p.reset();return reads(p,1)+p.st();})).append(' ');
                o.append(t(()->reads(p,6)+p.st()+p.content())).append(' ');
                o.append(t(()->reads(p,4)+p.st()+p.content())).append(' ');
                o.append(reset(p)).append(p.st());
                return o+"|"+s.log;
            }
            public static String markLimits(){
                Src s=new Src("abcdefghij",8);Peek p=new Peek(s,4);
                String a=t(()->{p.mark(0);String r=reads(p,3)+reset(p)+reads(p,5)+p.st();return r+reset(p)+p.st();});
                Src q=new Src("abcdefghij",8);Peek m=new Peek(q,4);
                String b=t(()->{m.read();m.read();m.mark(-1);String r=reads(m,2)+m.st();r+=reads(m,1)+m.st();return r+reset(m)+reads(m,1)+m.st();});
                Src u=new Src("abcdefghijklmnopqrstuvwxyz",5);Peek k=new Peek(u,4);byte[] x=new byte[12];
                String c=t(()->{k.mark(6);int n=k.read(x,0,12);String r=n+chars(x,0,n)+k.st()+reset(k);n=k.read(x,0,12);
                    return r+n+chars(x,0,n)+k.st()+"|"+u.log;});
                Src v=new Src("abcdefghijklmnop",4);BufferedInputStream w=new BufferedInputStream(v,4);
                String d=t(()->{w.mark(3);String r=w.skip(10)+",";r+=w.skip(10)+" "+reset(w);w.mark(3);r+=w.skip(1)+" "+reset(w);return r+reads(w,3)+"|"+v.log;});
                return a+";"+b+";"+c+";"+d;
            }
            public static String closeBehaviour(){
                Src s=new Src("abcdef",8);Peek p=new Peek(s,4);
                String a=t(()->{p.read();p.mark(3);p.close();String st=p.st();p.close();return st+s.log;});
                String b=t(()->{p.read();return "x";})+";"+t(()->{p.read(new byte[2],0,1);return "x";})+";"+t(()->{p.read(null,0,0);return "x";})
                    +";"+t(()->p.skip(1))+";"+t(()->p.skip(0))+";"+t(()->p.available())+";"+t(()->{p.reset();return "x";})
                    +";"+t(()->{p.mark(1);return p.markSupported()+p.st();})+";"+t(()->p.transferTo(new Sink()));
                Src c=new Src("abc",8);BufferedInputStream d=new BufferedInputStream(c,4);
                String e=t(()->{d.close();return reads(d,1);})+";"+t(()->{d.close();return c.log;});
                return a+"|"+b+"|"+s.log+"|"+e;
            }
            public static String nullSourceAndSize(){
                String a=t(()->{new BufferedInputStream(new Src("a",1),0);return "x";})+";"+t(()->{new BufferedInputStream(new Src("a",1),-1);return "x";})
                    +";"+t(()->{new BufferedInputStream(null,0);return "x";})+";"+t(()->{new BufferedInputStream(null,1);return "ok";});
                Peek p=new Peek(null,2);
                String b=t(()->{p.read();return "x";})+";"+t(()->p.available())+";"+t(()->p.skip(1))+";"+t(()->{p.read(new byte[1],0,1);return "x";})
                    +";"+t(()->{p.read(new byte[4],0,4);return "x";})+";"+t(()->p.markSupported())+";"+t(()->{p.mark(5);p.reset();return p.st();})+";"+t(()->{p.close();return p.st();});
                return a+"|"+b;
            }
            public static String sourceFailures(){
                Src s=new Src("abcdefgh",2);s.failAt=2;Peek p=new Peek(s,4);
                String a=t(()->reads(p,3))+";"+p.st()+t(()->reads(p,3))+p.st()+"|"+s.log;
                Src c=new Src("ab",2);c.failClose=true;Peek q=new Peek(c,4);
                String b=t(()->{q.read();q.close();return "x";})+q.st()+";"+t(()->{q.close();return c.log;});
                return a+";"+b;
            }

            public static String transferToCalls(){
                Src s=new Src("abcdefghij",3);BufferedInputStream b=new BufferedInputStream(s,4);Sink k=new Sink();
                String a=t(()->{b.read();long n=b.transferTo(k);long m=b.transferTo(k);return n+","+m+"|"+s.log+"|"+k.log;});
                Src u=new Src("abcdefg",3);Peek p=new Peek(u,4);Sink l=new Sink();
                String c=t(()->{p.read();return p.transferTo(l)+"|"+u.log+"|"+l.log;});
                Src v=new Src("abcdefg",3);BufferedInputStream q=new BufferedInputStream(v,4);Sink j=new Sink();
                String d=t(()->{q.read();q.mark(10);return q.transferTo(j)+"|"+v.log+"|"+j.log;});
                Src w=new Src("abcdef",4);w.xfer=Long.MAX_VALUE;BufferedInputStream r=new BufferedInputStream(w,4);Sink i=new Sink();
                String e=t(()->{r.read();r.read();return r.transferTo(i)+"|"+w.log+"|"+i.log;});
                Src x=new Src("abc",4);x.xfer=Long.MAX_VALUE-3;BufferedInputStream y=new BufferedInputStream(x,4);Sink h=new Sink();
                String f=t(()->{y.read();return y.transferTo(h)+"|"+x.log+"|"+h.log;});
                return a+";"+c+";"+d+";"+e+";"+f+";"+t(()->b.transferTo(null))+";"+t(()->p.transferTo(null));
            }

            public static String bufferedWrites(){
                Sink k=new Sink();BufferedOutputStream b=new BufferedOutputStream(k,4);
                return t(()->{StringBuilder o=new StringBuilder();
                    b.write('a');b.write('b');b.write('c');b.write('d');o.append('[').append(k.log).append("] ");
                    b.write('e');o.append(k.log).append("/ ");
                    b.write(new byte[]{'f','g'},0,2);
                    b.write(new byte[]{'h','i','j'},1,2);
                    o.append(k.log).append("/ ");
                    b.write(new byte[]{'k','l','m','n','o'},1,4);
                    b.write(new byte[0],0,0);b.write(new byte[]{'p'});
                    b.flush();b.flush();
                    b.write(0x171);b.write(-191);b.write(new byte[]{'q','r','s'});b.write(new byte[]{'t','u'},0,2);
                    String c1=closeResult(b),c2=closeResult(b);
                    b.write('z');b.flush();
                    return o+k.log.toString()+c1+","+c2;});
            }
            public static String defaultOutputSize(){
                Sink k=new Sink();BufferedOutputStream b=new BufferedOutputStream(k);
                String a=t(()->{for(int i=0;i<8192;i++)b.write('x');String s="["+k.log+"] ";b.write('y');String c=k.log+"/ ";
                    b.write(new byte[8190]);String d=k.log+"/ ";b.write(new byte[8192],0,8192);b.flush();return s+c+d+k.log;});
                Sink m=new Sink();OSpy s=new OSpy(m);
                String c=t(()->{String a0=s.st();s.write(new byte[8191],0,8191);s.write(new byte[2]);return a0+s.st()+s.log+"|"+m.log;});
                return a+";"+c;
            }
            public static String outputChecks(){
                String a=t(()->{new BufferedOutputStream(new Sink(),0);return "x";})+";"+t(()->{new BufferedOutputStream(new Sink(),-5);return "x";})
                    +";"+t(()->{new BufferedOutputStream(null,0);return "x";})+";"+t(()->{new BufferedOutputStream(null);return "ok";});
                Sink k=new Sink();BufferedOutputStream b=new BufferedOutputStream(k,4);byte[] x={'a','b','c','d'};
                String c=t(()->{b.write(x,0,3);return "ok";})+";"+ty(()->{b.write(x,-1,2);return "x";})+";"+k.log
                    +";"+ty(()->{b.write(x,0,-1);return "x";})+";"+ty(()->{b.write(x,3,2);return "x";})+";"+ty(()->{b.write(null,0,1);return "x";})
                    +";"+ty(()->{b.write(null);return "x";})+";"+t(()->{b.write(x,2,9000);b.write(x,-3,4);return k.log;});
                BufferedOutputStream n=new BufferedOutputStream(null,2);
                String d=t(()->{n.write(1);n.write(2);return "buffered";})+";"+ty(()->{n.write(3);return "x";})+";"+ty(()->{n.flush();return "x";});
                BufferedOutputStream e=new BufferedOutputStream(new ByteArrayOutputStream(),4);
                String f=t(()->{e.write(x,2,9000);return "x";});
                return a+"|"+c+"|"+d+"|"+f;
            }

            public static String outputSubclass(){
                Sink k=new Sink();OSpy s=new OSpy(k,4);
                String a=t(()->{String st=s.st();s.write(new byte[]{'a','b'});s.write('c');String m1=s.st();s.write(new byte[]{'d','e','f','g','h'});
                    String m2=s.st();String c=closeResult(s);return st+m1+m2+s.st()+c+" "+s.log+"|"+k.log;});
                Sink m=new Sink();OSpy g=new OSpy(m,8);
                String b=t(()->{StringBuilder o=new StringBuilder();g.replace(2);o.append(g.st());
                    g.write('a');o.append(g.st());g.write('b');o.append(g.st());g.write('c');o.append(g.st());
                    g.write(new byte[]{'d','e','f','g'},0,4);o.append(g.st());g.write(new byte[]{'h','i'},0,2);o.append(g.st());
                    g.write(new byte[]{'1','2','3','4','5','6','7','8'},0,8);o.append(g.st());
                    g.replace(1);g.write(new byte[]{'j','k','l','m','n','o','p'},0,7);o.append(g.st());g.flush();
                    return o+" "+m.log;});
                Sink f=new Sink();FilterOutputStream w=new BufferedOutputStream(f,3);
                String c=t(()->{w.write(new byte[]{'a','b'});w.write('c');w.write('d');w.close();return f.log.toString();});
                return a+";"+b+";"+c;
            }
            public static String outputFailures(){
                Sink k=new Sink();k.failWrites=1;BufferedOutputStream b=new BufferedOutputStream(k,4);
                String a=t(()->{b.write('a');b.write('b');b.flush();return "x";})+" "+t(()->{b.flush();return k.log;});
                Sink f=new Sink();f.failFlush=true;f.failClose=true;BufferedOutputStream c=new BufferedOutputStream(f,4);
                String d=t(()->{c.write('x');return closeResult(c)+" / "+closeResult(c)+" / "+f.log;});
                Sink g=new Sink();g.failWrites=1;BufferedOutputStream e=new BufferedOutputStream(g,2);
                String h=t(()->{e.write('a');e.write('b');try{e.write('c');}catch(IOException x){g.log.append("caught ");}e.write('d');return closeResult(e)+" "+g.log;});
                Sink i=new Sink();i.failWrites=1;BufferedOutputStream j=new BufferedOutputStream(i,4);
                String l=t(()->{j.write('a');String r=closeResult(j);j.write('b');j.flush();return r+" "+i.log;});
                return a+";"+d+";"+h+";"+l;
            }
        }
        """;

    @Test void bufferedInputReads() throws Exception {
        matchesJvm(FIXTURE, "readSingleBytes", "defaultSize", "bulkReads", "bulkReadChecks");
    }

    @Test void bufferedInputSkipAndAvailable() throws Exception {
        matchesJvm(FIXTURE, "skipAndAvailable");
    }

    @Test void bufferedInputMarkAndReset() throws Exception {
        matchesJvm(FIXTURE, "markAndReset", "markLimits");
    }

    @Test void bufferedInputClose() throws Exception {
        matchesJvm(FIXTURE, "closeBehaviour", "nullSourceAndSize", "sourceFailures");
    }

    @Test void bufferedInputTransferTo() throws Exception {
        matchesJvm(FIXTURE, "transferToCalls");
    }

    @Test void bufferedOutputWrites() throws Exception {
        matchesJvm(FIXTURE, "bufferedWrites", "defaultOutputSize", "outputChecks");
    }

    @Test void bufferedOutputSubclassAndFailures() throws Exception {
        matchesJvm(FIXTURE, "outputSubclass", "outputFailures");
    }
}
