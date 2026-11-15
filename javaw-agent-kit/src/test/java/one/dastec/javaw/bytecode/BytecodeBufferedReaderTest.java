package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** T32: java.io.BufferedReader over the runtime Reader. Expected values come from the JVM; see tasks/T32-buffered-reader.md. */
@Tag("agent-pending")
class BytecodeBufferedReaderTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.io.BufferedReader;
        import java.io.Closeable;
        import java.io.IOException;
        import java.io.Reader;
        import java.io.StringWriter;

        /** A Reader over a String that logs every call: rL:off,len=n for read(char[] of length L, off, len), ready=b, close.
         *  chunk caps each read, zeros makes the next reads return 0, failOn makes that read call (counting from 1) throw. */
        class Src extends Reader {
            final StringBuilder log=new StringBuilder(); final String s; int pos,chunk=Integer.MAX_VALUE,zeros,calls,failOn; boolean readyAnswer=true; IOException failClose;
            Src(String s){this.s=s;}
            Src chunk(int c){chunk=c;return this;}
            Src notReady(){readyAnswer=false;return this;}
            public int read(char[] b,int off,int len) throws IOException {
                log.append('r').append(b.length).append(':').append(off).append(',').append(len).append('=');
                if(++calls==failOn){log.append("! ");throw new IOException("disk");}
                if(zeros>0){zeros--;log.append("0 ");return 0;}
                if(pos>=s.length()){log.append("-1 ");return -1;}
                int n=Math.min(Math.min(len,chunk),s.length()-pos);s.getChars(pos,pos+n,b,off);pos+=n;log.append(n).append(' ');return n;
            }
            public boolean ready(){boolean r=readyAnswer&&pos<s.length();log.append("ready=").append(r).append(' ');return r;}
            public void close() throws IOException {log.append("close ");if(failClose!=null)throw failClose;}
        }
        /** Like Tomcat's CoyoteReader: a BufferedReader subclass with its own buffer size that overrides some methods (and logs what they receive). */
        class Spy extends BufferedReader {
            final StringBuilder log=new StringBuilder();
            Spy(Reader in,int sz){super(in,sz);}
            boolean lockIs(Object o){return lock==o;}
            public int read(char[] b,int off,int len) throws IOException {log.append('R').append(b.length).append(':').append(off).append(',').append(len).append(' ');return super.read(b,off,len);}
            public boolean ready() throws IOException {log.append("Y ");return super.ready();}
            public void mark(int limit) throws IOException {log.append("M").append(limit).append(' ');super.mark(limit);}
        }

        public class Fixture {
            interface Op{Object run() throws Exception;}
            static String t(Op op){
                try{return String.valueOf(op.run());}catch(Exception e){return e.getClass().getSimpleName()+":"+e.getMessage();}
            }
            static String ty(Op op){
                try{return String.valueOf(op.run());}catch(Exception e){return e.getClass().getSimpleName();}
            }
            /** Makes \\r and \\n visible. */
            static String show(String s){return s==null?"null":s.replace("\\r","\\\\r").replace("\\n","\\\\n");}
            static String allLines(String text,int size){
                return t(()->{BufferedReader b=new BufferedReader(new Src(text),size);StringBuilder o=new StringBuilder();
                    String line;while((line=b.readLine())!=null)o.append('[').append(show(line)).append(']');return o.append(show(b.readLine())).toString();});
            }
            static String chars(char[] a,int n){return n<=0?"":show(new String(a,0,n));}

            public static String constructors(){
                Src s=new Src("abc");
                String a=t(()->{new BufferedReader(s,0);return "ok";})+","+t(()->{new BufferedReader(s,-5);return "ok";})+","+t(()->{new BufferedReader(s,Integer.MIN_VALUE);return "ok";})
                        +","+ty(()->{new BufferedReader(null);return "ok";})+","+ty(()->{new BufferedReader(null,0);return "ok";})+","+ty(()->{new BufferedReader(null,1);return "ok";});
                Spy p=new Spy(s,1);Object o=p;
                a+="|"+p.lockIs(s)+","+p.lockIs(p)+","+(o instanceof Reader)+","+(o instanceof Closeable)+","+p.markSupported()+",log="+s.log;
                return a+"|"+t(()->{BufferedReader b=new BufferedReader(s,1);return b.read()+","+b.read()+","+b.read()+","+b.read()+","+b.read()+" "+s.log;});
            }
            public static String readSingle(){
                Src s=new Src("ab\\u00e9\\uffff");BufferedReader b=new BufferedReader(s);
                String a=t(()->b.read()+","+b.read()+","+b.read()+","+b.read()+","+b.read()+","+b.read()+" "+s.log);
                Src c=new Src("abcde").chunk(2);BufferedReader d=new BufferedReader(c);
                String e=t(()->{StringBuilder o=new StringBuilder();int x;while((x=d.read())>=0)o.append((char)x);return o+" "+c.log;});
                Src z=new Src("xy");z.zeros=3;BufferedReader y=new BufferedReader(z,5);
                return a+"|"+e+"|"+t(()->y.read()+","+y.read()+","+y.read()+" "+z.log);
            }
            public static String readArrays(){
                Src s=new Src("abcdefghij").chunk(3);BufferedReader b=new BufferedReader(s,4);char[] buf=new char[10];
                String a=t(()->{int n=b.read(buf,0,10);return n+":"+chars(buf,n)+" "+s.log;});
                Src q=new Src("abcdefghij").chunk(3).notReady();BufferedReader c=new BufferedReader(q,4);
                String d=t(()->{int n=c.read(buf,0,10);return n+":"+chars(buf,n)+" "+q.log;});
                Src r=new Src("abcdefghij");BufferedReader e=new BufferedReader(r,8);
                String f=t(()->{int n=e.read(buf,0,3);int m=e.read(buf,0,10);int k=e.read(buf,0,10);int z=e.read(buf,0,0);int w=e.read(buf,2,1);return n+","+m+":"+chars(buf,m)+","+k+","+z+","+w+" "+r.log;});
                Src u=new Src("0123456789abcdefghij");BufferedReader g=new BufferedReader(u);
                String h=t(()->{int n=g.read(buf,1,4);char[] big=new char[9000];int m=g.read(big,0,9000);int k=g.read(big);return n+","+m+","+k+" "+u.log;});
                return a+"|"+d+"|"+f+"|"+h;
            }
            public static String readRanges(){
                Src s=new Src("abc");BufferedReader b=new BufferedReader(s,4);char[] c=new char[4];
                return t(()->b.read(c,-1,1))+";"+t(()->b.read(c,0,5))+";"+t(()->b.read(c,2,3))+";"+t(()->b.read(c,0,-1))+";"+t(()->b.read(c,5,0))
                        +";"+t(()->b.read(c,1,Integer.MAX_VALUE))+";"+ty(()->b.read(null,0,0))+";"+ty(()->b.read((char[])null))+";"+t(()->b.read(c,4,0))+";log="+s.log
                        +";"+t(()->b.read(c,1,3)+chars(c,4)+" "+s.log);
            }

            public static String readLineTerminators(){
                String[] texts={"a\\nb\\r\\nc\\rd\\n\\ne","x\\r","","\\n","\\r\\n","\\r\\r\\n\\n","no newline","\\u2028x\\u0085y\\u000bz\\fw","tab\\tend\\n\\n","\\r\\n\\r\\n\\rq"};
                int[] sizes={8192,1,2,3};
                StringBuilder o=new StringBuilder();
                for(String text:texts){for(int size:sizes)o.append(allLines(text,size)).append(' ');o.append(';');}
                return o.toString();
            }
            public static String readLineCalls(){
                Src s=new Src("ab\\r\\ncd");BufferedReader b=new BufferedReader(s,3);
                String a=t(()->show(b.readLine())+","+show(b.readLine())+","+show(b.readLine())+","+show(b.readLine())+" "+s.log);
                Src q=new Src("abcdefghij\\nk");BufferedReader c=new BufferedReader(q,4);
                String d=t(()->show(c.readLine())+","+show(c.readLine())+","+show(c.readLine())+" "+q.log);
                Src r=new Src("\\r\\n");BufferedReader e=new BufferedReader(r,1);
                String f=t(()->show(e.readLine())+","+show(e.readLine())+" "+r.log);
                Src u=new Src("one\\ntwo\\n").chunk(2);BufferedReader g=new BufferedReader(u);
                return a+"|"+d+"|"+f+"|"+t(()->show(g.readLine())+","+show(g.readLine())+","+show(g.readLine())+" "+u.log);
            }
            public static String readFailures(){
                Src s=new Src("abcdef\\nxy");s.failOn=2;BufferedReader b=new BufferedReader(s,2);
                String a=t(()->b.readLine())+","+t(()->b.readLine())+","+t(()->b.readLine())+" "+s.log;
                Src q=new Src("abc");q.failOn=1;BufferedReader c=new BufferedReader(q);
                String d=t(()->c.read())+","+t(()->c.read())+" "+q.log;
                Src r=new Src("abcdefgh");r.failOn=2;BufferedReader e=new BufferedReader(r,4);char[] buf=new char[8];
                return a+"|"+d+"|"+t(()->e.read(buf,0,3))+","+t(()->e.read(buf,0,8))+","+t(()->e.read(buf,0,8)+":"+chars(buf,4))+" "+r.log;
            }

            public static String carriageReturns(){
                StringBuilder o=new StringBuilder();
                Src s1=new Src("a\\r\\nb");BufferedReader b1=new BufferedReader(s1);
                o.append(t(()->show(b1.readLine())+","+b1.read()+","+b1.read())).append(';');
                BufferedReader b2=new BufferedReader(new Src("a\\r\\rb"));
                o.append(t(()->show(b2.readLine())+","+b2.read()+","+show(b2.readLine()))).append(';');
                Src s3=new Src("a\\r\\nbc");BufferedReader b3=new BufferedReader(s3,2);char[] buf=new char[5];
                o.append(t(()->{String l=b3.readLine();int n=b3.read(buf,0,1);return show(l)+","+n+":"+chars(buf,n)+" "+s3.log;})).append(';');
                Src s4=new Src("ab\\r\\n");BufferedReader b4=new BufferedReader(s4,3);
                o.append(t(()->show(b4.readLine())+","+b4.read(buf,0,5)+" "+s4.log)).append(';');
                Src s5=new Src("a\\r\\nbcd");BufferedReader b5=new BufferedReader(s5);
                o.append(t(()->show(b5.readLine())+","+b5.skip(2)+","+b5.read())).append(';');
                Src s6=new Src("a\\r\\nbc");BufferedReader b6=new BufferedReader(s6,2);
                o.append(t(()->show(b6.readLine())+","+b6.skip(1)+","+b6.read()+" "+s6.log)).append(';');
                Src s7=new Src("a\\r\\nb");BufferedReader b7=new BufferedReader(s7,2);
                o.append(t(()->show(b7.readLine())+","+b7.ready()+","+b7.read()+" "+s7.log)).append(';');
                Src s8=new Src("a\\r\\nb").notReady();BufferedReader b8=new BufferedReader(s8,2);
                o.append(t(()->show(b8.readLine())+","+b8.ready()+","+b8.ready()+","+b8.read()+" "+s8.log)).append(';');
                BufferedReader b9=new BufferedReader(new Src("\\r\\nx"));
                o.append(t(()->b9.read()+","+show(b9.readLine())+","+show(b9.readLine()))).append(';');
                BufferedReader b10=new BufferedReader(new Src("a\\r"));
                o.append(t(()->show(b10.readLine())+","+b10.read()+","+b10.skip(3)));
                Src s11=new Src("a\\r\\nb").chunk(1);BufferedReader b11=new BufferedReader(s11,4);char[] big=new char[8];
                o.append(';').append(t(()->{String l=b11.readLine();int n=b11.read(big,0,8);return show(l)+","+n+":"+chars(big,n)+" "+s11.log;}));
                return o.toString();
            }

            public static String skipping(){
                Src s=new Src("abcdefghij");BufferedReader b=new BufferedReader(s,4);
                String a=t(()->b.skip(-1))+","+t(()->b.skip(Long.MIN_VALUE))+","+t(()->b.skip(0))+",log="+s.log;
                a+="|"+t(()->b.skip(3)+","+(char)b.read()+","+b.skip(5)+","+(char)b.read()+","+b.skip(10)+","+b.skip(1)+" "+s.log);
                Src q=new Src("abcdef");BufferedReader c=new BufferedReader(q);
                return a+"|"+t(()->c.skip(Long.MAX_VALUE)+","+c.read()+" "+q.log);
            }
            public static String readiness(){
                Src s=new Src("abc").notReady();BufferedReader b=new BufferedReader(s);
                String a=t(()->b.ready()+","+(char)b.read()+","+b.ready()+","+b.skip(2)+","+b.ready()+" "+s.log);
                Src q=new Src("abc");BufferedReader c=new BufferedReader(q);
                return a+"|"+t(()->c.ready()+","+c.skip(3)+","+c.ready()+","+c.read()+","+c.ready()+" "+q.log);
            }

            public static String markReset(){
                BufferedReader x=new BufferedReader(new Src("abc"));
                String a=t(()->{x.reset();return "x";})+","+t(()->{x.mark(-1);return "x";})+","+t(()->{x.mark(Integer.MIN_VALUE);return "x";});
                Src s=new Src("abcdef");BufferedReader b=new BufferedReader(s);
                a+="|"+t(()->{StringBuilder o=new StringBuilder();o.append((char)b.read());b.mark(3);o.append((char)b.read()).append((char)b.read());b.reset();
                    for(int i=0;i<4;i++)o.append((char)b.read());b.reset();o.append((char)b.read());int c;while((c=b.read())>=0)o.append((char)c);
                    o.append(',').append(t(()->{b.reset();return "x";})).append(',').append(t(()->{b.reset();return "x";}));
                    b.mark(1);o.append(',').append(b.read());b.reset();o.append(',').append(b.read()).append(' ').append(s.log);return o;});
                BufferedReader c=new BufferedReader(new Src("abc"));
                a+="|"+t(()->{c.read();c.mark(0);int m=c.read();c.reset();return m+","+c.read();});
                BufferedReader d=new BufferedReader(new Src("a\\r\\nb\\nc"));
                a+="|"+t(()->{String l=d.readLine();d.mark(5);int r=d.read();d.reset();String m=d.readLine();d.reset();return show(l)+","+r+","+show(m)+","+d.read()+","+show(d.readLine());});
                return a;
            }
            public static String markFills(){
                Src s=new Src("abcdefghij");BufferedReader b=new BufferedReader(s,4);
                String a=t(()->{StringBuilder o=new StringBuilder();o.append((char)b.read());b.mark(4);for(int i=0;i<4;i++)o.append((char)b.read());b.reset();
                    for(int i=0;i<5;i++)o.append((char)b.read());o.append(',').append(t(()->{b.reset();return "x";}));return o+" "+s.log;});
                Src q=new Src("abcdefghijklmn");BufferedReader c=new BufferedReader(q,4);
                a+="|"+t(()->{StringBuilder o=new StringBuilder();c.mark(10);for(int i=0;i<12;i++)o.append((char)c.read());o.append(',').append(t(()->{c.reset();return "x";}));return o+" "+q.log;});
                Src r=new Src("abcdefghijklmnopqrstuvwxyz");BufferedReader d=new BufferedReader(r,4);char[] buf=new char[8];
                a+="|"+t(()->{d.mark(6);int n=d.read(buf,0,8);String o=n+":"+chars(buf,n);n=d.read(buf,0,8);o+=","+n+":"+chars(buf,n)+","+t(()->{d.reset();return "x";});
                    n=d.read(buf,0,8);return o+","+n+":"+chars(buf,n)+" "+r.log;});
                Src u=new Src("abcdefgh");BufferedReader e=new BufferedReader(u,4);
                a+="|"+t(()->{e.read();e.mark(3);e.read();e.read();e.read();int c4=e.read();e.mark(2);int c5=e.read();e.reset();return (char)c4+","+(char)c5+","+(char)e.read()+" "+u.log;});
                return a;
            }

            public static String closing(){
                Src s=new Src("abc");BufferedReader b=new BufferedReader(s);char[] buf=new char[4];
                String a=t(()->{int c=b.read();b.close();b.close();return c+" "+s.log;});
                a+="|"+t(()->b.read())+","+t(()->b.read(buf,0,1))+","+t(()->b.read(buf,-1,1))+","+t(()->b.read(buf,0,0))+","+t(()->b.readLine())+","+t(()->b.skip(1))+","+t(()->b.skip(-1))
                        +","+t(()->b.ready())+","+t(()->{b.mark(1);return "x";})+","+t(()->{b.mark(-1);return "x";})+","+t(()->{b.reset();return "x";})+","+b.markSupported()
                        +","+t(()->b.read(buf))+","+t(()->b.transferTo(new StringWriter()))+","+ty(()->b.read((char[])null))+" "+s.log;
                Src q=new Src("xyz");q.failClose=new IOException("boom");BufferedReader c=new BufferedReader(q);
                a+="|"+t(()->{c.close();return "ok";})+","+t(()->{c.close();return "ok";})+","+t(()->c.read())+" "+q.log;
                Src r=new Src("line\\nrest");
                a+="|"+t(()->{String l;try(BufferedReader d=new BufferedReader(r)){l=d.readLine();}return l+" "+r.log;});
                return a;
            }

            public static String subclassCalls(){
                Src s=new Src("line1\\nline2\\r\\nrest of the text");Spy p=new Spy(s,4);char[] buf=new char[3];
                return t(()->{StringBuilder o=new StringBuilder();o.append(show(p.readLine())).append(',').append((char)p.read()).append(',');
                    int n=p.read(buf);o.append(n).append(':').append(chars(buf,n)).append(',').append(p.skip(2)).append(',').append(p.ready()).append(',');
                    o.append(show(p.readLine())).append(',');StringWriter w=new StringWriter();o.append(p.transferTo(w)).append(':').append(show(w.toString()));
                    p.mark(2);o.append(',').append(p.read());
                    return o+" | "+p.log+"| "+s.log;});
            }
        }
        """;

    @Test void constructionAndSubclasses() throws Exception {
        matchesJvm(FIXTURE, "constructors", "subclassCalls");
    }

    @Test void readCharacters() throws Exception {
        matchesJvm(FIXTURE, "readSingle", "readArrays", "readRanges");
    }

    @Test void readLines() throws Exception {
        matchesJvm(FIXTURE, "readLineTerminators", "readLineCalls", "readFailures");
    }

    @Test void carriageReturnAcrossCalls() throws Exception {
        matchesJvm(FIXTURE, "carriageReturns");
    }

    @Test void skipAndReady() throws Exception {
        matchesJvm(FIXTURE, "skipping", "readiness");
    }

    @Test void markAndReset() throws Exception {
        matchesJvm(FIXTURE, "markReset", "markFills");
    }

    @Test void closing() throws Exception {
        matchesJvm(FIXTURE, "closing");
    }

    @Test void linesStaysUnlinked() {
        rejectsUnusedCall("java/io/BufferedReader", "lines", "()Ljava/util/stream/Stream;", INVOKEVIRTUAL, false);
    }
}
