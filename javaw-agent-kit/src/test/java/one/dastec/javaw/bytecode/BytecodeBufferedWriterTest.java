package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T40: java.io.BufferedWriter over the runtime Writer. Expected values come from the JVM; see tasks/T40-buffered-writer.md. */
class BytecodeBufferedWriterTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.io.BufferedWriter;
        import java.io.Closeable;
        import java.io.Flushable;
        import java.io.IOException;
        import java.io.Writer;

        /** A Writer that logs every call: wL:off,len=text for write(char[] of length L, off, len), f for flush, c for close.
         *  failWriteOn makes that write call (counting from 1) throw IOException("disk"); failFlush and failClose throw too. */
        class Sink extends Writer {
            final StringBuilder log=new StringBuilder(); int writes,failWriteOn; boolean failFlush,failClose;
            public void write(char[] b,int off,int len) throws IOException {
                log.append('w').append(b.length).append(':').append(off).append(',').append(len).append('=');
                if(++writes==failWriteOn){log.append("! ");throw new IOException("disk");}
                log.append(len<=20?Fixture.show(new String(b,off,len)):"...").append(' ');
            }
            public void flush() throws IOException {log.append("f ");if(failFlush)throw new IOException("stuck");}
            public void close() throws IOException {log.append("c ");if(failClose)throw new IOException("shut");}
        }
        /** A BufferedWriter subclass that logs the public calls it receives before passing them on. */
        class Spy extends BufferedWriter {
            final StringBuilder log=new StringBuilder();
            Spy(Writer out,int sz){super(out,sz);}
            boolean lockIs(Object o){return lock==o;}
            public void write(int c) throws IOException {log.append("I ");super.write(c);}
            public void write(char[] b,int off,int len) throws IOException {log.append("C").append(off).append(',').append(len).append(' ');super.write(b,off,len);}
            public void write(String s,int off,int len) throws IOException {log.append("S").append(off).append(',').append(len).append(' ');super.write(s,off,len);}
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
            /** Makes \\r and \\n visible. */
            static String show(String s){return s.replace("\\r","\\\\r").replace("\\n","\\\\n");}
            static String fill(int n){char[] c=new char[n];for(int i=0;i<n;i++)c[i]=(char)('a'+i%26);return new String(c);}

            public static String constructors(){
                Sink s=new Sink();
                String a=t(()->{new BufferedWriter(s,0);return "ok";})+","+t(()->{new BufferedWriter(s,-1);return "ok";})+","+t(()->{new BufferedWriter(s,Integer.MIN_VALUE);return "ok";})
                        +","+ty(()->{new BufferedWriter(null);return "ok";})+","+ty(()->{new BufferedWriter(null,0);return "ok";})+","+ty(()->{new BufferedWriter(null,4);return "ok";});
                Spy p=new Spy(s,1);Object o=p;
                return a+"|"+p.lockIs(s)+","+p.lockIs(p)+","+(o instanceof Writer)+","+(o instanceof Closeable)+","+(o instanceof Flushable)+","+(o instanceof Appendable)+",log="+s.log;
            }
            public static String writeSingle(){
                Sink s=new Sink();BufferedWriter b=new BufferedWriter(s,3);
                String a=t(()->{b.write('a');b.write('b');b.write('c');String x=s.log+"/";b.write('d');x+=s.log+"/";b.write(0x10045);b.flush();return x+s.log;});
                Sink d=new Sink();BufferedWriter e=new BufferedWriter(d);
                String f=t(()->{for(int i=0;i<8192;i++)e.write('x');String x=d.log+"/";e.write('y');x+=d.log+"/";e.flush();return x+d.log;});
                Sink g=new Sink();BufferedWriter h=new BufferedWriter(g,1);
                return a+"|"+f+"|"+t(()->{h.write('p');h.write('q');h.append('r');h.write('s');return g.log;});
            }
            public static String writeArrays(){
                Sink s=new Sink();BufferedWriter b=new BufferedWriter(s,4);char[] c="0123456789".toCharArray();
                String a=t(()->{b.write(c,0,2);String x=s.log+"/";b.write(c,2,3);x+=s.log+"/";b.write(c,5,4);x+=s.log+"/";b.write(c,9,0);b.write(c,3,1);b.flush();return x+s.log;});
                Sink d=new Sink();BufferedWriter e=new BufferedWriter(d,3);
                String f=t(()->{e.write(c,0,3);String x=d.log+"/";e.write(c,0,9);x+=d.log+"/";e.write(c);x+=d.log+"/";e.write(c,0,0);e.flush();return x+d.log;});
                Sink g=new Sink();BufferedWriter h=new BufferedWriter(g);
                String k=t(()->{h.write(fill(8191).toCharArray(),0,8191);String x=g.log+"/";h.write(fill(8192).toCharArray());x+=g.log+"/";h.write('z');h.write(new char[9000],100,8192);return x+g.log;});
                Sink m=new Sink();BufferedWriter n=new BufferedWriter(m,2);
                String q=t(()->{n.write('a');n.write('b');n.write(c,0,3);return m.log;});
                return a+"|"+f+"|"+k+"|"+q;
            }
            public static String arrayRanges(){
                Sink s=new Sink();BufferedWriter b=new BufferedWriter(s,8);char[] c=new char[4];
                return t(()->{b.write(c,-1,1);return "ok";})+";"+t(()->{b.write(c,0,5);return "ok";})+";"+t(()->{b.write(c,2,3);return "ok";})
                        +";"+t(()->{b.write(c,0,-1);return "ok";})+";"+t(()->{b.write(c,5,0);return "ok";})+";"+t(()->{b.write(c,1,Integer.MAX_VALUE);return "ok";})
                        +";"+t(()->{b.write(c,4,0);return "ok";})+";"+ty(()->{b.write((char[])null,0,0);return "ok";})+";"+ty(()->{b.write((char[])null);return "ok";})
                        +";"+t(()->{b.write(new char[20],-1,10);return "ok";})+";log="+s.log;
            }
            public static String writeStrings(){
                Sink s=new Sink();BufferedWriter b=new BufferedWriter(s,4);
                String a=t(()->{b.write("abcdefghij");String x=s.log+"/";b.write("xyz",1,1);b.write("");b.write("klm",1,0);b.write("nop",0,-3);b.flush();return x+s.log;});
                Sink d=new Sink();BufferedWriter e=new BufferedWriter(d,4);
                String f=t(()->{e.write("ab");return "ok";})+";"+t(()->{e.write("abc",-1,2);return "ok";})+";"+t(()->{e.write("abc",2,5);return "ok";})+";"+t(()->{e.write("abc",4,-1);return "ok";})
                        +";"+t(()->{e.write((String)null,0,0);return "ok";})+";"+t(()->{e.write((String)null,3,-2);return "ok";})+";"+ty(()->{e.write((String)null,0,1);return "ok";})
                        +";"+ty(()->{e.write((String)null);return "ok";})+";"+t(()->{e.append(null);e.append("hello",1,3);e.append("XY");e.flush();return d.log;});
                Sink g=new Sink();BufferedWriter h=new BufferedWriter(g);
                String k=t(()->{h.write(fill(8191));String x=g.log+"/";h.write("ab");x+=g.log+"/";h.write(fill(20000),5,17000);return x+g.log;});
                return a+"|"+f+"|"+k;
            }
            public static String newLines(){
                Sink s=new Sink();BufferedWriter b=new BufferedWriter(s);
                String a=t(()->{b.write("a");b.newLine();b.newLine();b.write('b');b.flush();return s.log;});
                Sink d=new Sink();BufferedWriter e=new BufferedWriter(d,1);
                String f=t(()->{e.newLine();e.write("c");e.newLine();return d.log;});
                Sink g=new Sink();Spy p=new Spy(g,8);
                String h=t(()->{p.newLine();p.write("de");p.append('f');p.append("gh");p.write("ij".toCharArray());p.flush();return p.log+"/"+g.log;});
                return a+"|"+f+"|"+h;
            }
            public static String failures(){
                Sink s=new Sink();s.failWriteOn=1;BufferedWriter b=new BufferedWriter(s,2);
                String a=t(()->{b.write("abc");return "ok";})+","+t(()->{b.flush();return "ok";})+","+t(()->{b.write('d');b.flush();return "ok";})+" "+s.log;
                Sink d=new Sink();d.failFlush=true;BufferedWriter e=new BufferedWriter(d,4);
                String f=t(()->{e.write("xy");e.flush();return "ok";})+","+t(()->{e.flush();return "ok";})+","+t(()->{e.write("z");e.close();return "ok";})+" "+d.log;
                Sink g=new Sink();g.failWriteOn=2;BufferedWriter h=new BufferedWriter(g,3);char[] c="0123456789".toCharArray();
                String k=t(()->{h.write(c,0,2);h.write(c,0,5);return "ok";})+","+t(()->{h.write(c,0,1);h.flush();return "ok";})+" "+g.log;
                return a+"|"+f+"|"+k;
            }
            public static String closing(){
                Sink s=new Sink();BufferedWriter b=new BufferedWriter(s,4);
                String a=t(()->{b.write("ab");b.close();String x=s.log+"/";b.close();b.close();return x+s.log;});
                a+=";"+t(()->{b.write('x');return "ok";})+";"+t(()->{b.write(new char[2],5,9);return "ok";})+";"+t(()->{b.write((char[])null,0,0);return "ok";})
                        +";"+t(()->{b.write((String)null,0,1);return "ok";})+";"+t(()->{b.write("abc",-1,2);return "ok";})+";"+t(()->{b.flush();return "ok";})
                        +";"+t(()->{b.newLine();return "ok";})+";"+t(()->{b.append("q");return "ok";})+";log="+s.log;
                Sink d=new Sink();BufferedWriter e=new BufferedWriter(d,4);
                String f=t(()->{e.close();return d.log;});
                Sink g=new Sink();g.failWriteOn=1;g.failClose=true;BufferedWriter h=new BufferedWriter(g,4);
                String k=t(()->{h.write("ab");try{h.close();return "ok";}catch(IOException x){Throwable[] su=x.getSuppressed();return x.getMessage()+" suppressed="+su.length+(su.length>0?":"+su[0].getMessage():"");}})
                        +","+t(()->{h.close();return "again";})+","+t(()->{h.write('z');return "ok";})+" "+g.log;
                Sink m=new Sink();m.failClose=true;BufferedWriter n=new BufferedWriter(m,4);
                String q=t(()->{n.write("cd");n.close();return "ok";})+","+t(()->{n.close();return "again";})+","+t(()->{n.write("e");return "ok";})+" "+m.log;
                Sink r=new Sink();r.failWriteOn=1;BufferedWriter u=new BufferedWriter(r,4);
                String v=t(()->{u.write("ab");u.close();return "ok";})+","+t(()->{u.flush();return "ok";})+" "+r.log;
                Sink w=new Sink();Spy p=new Spy(w,4);
                String y=t(()->{p.write("hi");p.close();p.close();return p.log+"/"+w.log;});
                return a+"|"+f+"|"+k+"|"+q+"|"+v+"|"+y;
            }
        }
        """;

    @Test void construction() throws Exception {
        matchesJvm(FIXTURE, "constructors");
    }

    @Test void writeCharacters() throws Exception {
        matchesJvm(FIXTURE, "writeSingle", "writeArrays", "arrayRanges");
    }

    @Test void writeStrings() throws Exception {
        matchesJvm(FIXTURE, "writeStrings");
    }

    @Test void newLines() throws Exception {
        matchesJvm(FIXTURE, "newLines");
    }

    @Test void writeFailures() throws Exception {
        matchesJvm(FIXTURE, "failures");
    }

    @Test void closing() throws Exception {
        matchesJvm(FIXTURE, "closing");
    }

    @Test void bufferDoesNotGrow() {
        rejectsUnusedCall("java/io/BufferedWriter", "growIfNeeded", "(I)V", INVOKEVIRTUAL, false);
    }
}
