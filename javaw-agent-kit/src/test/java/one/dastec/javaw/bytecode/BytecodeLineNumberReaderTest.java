package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T44: java.io.LineNumberReader over the runtime BufferedReader. Expected values come from the JVM; see tasks/T44-line-number-reader.md. */
class BytecodeLineNumberReaderTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.io.BufferedReader;
        import java.io.IOException;
        import java.io.LineNumberReader;
        import java.io.Reader;
        import java.io.StringWriter;

        /** A Reader over a String that logs rL:off,len=n for each read(char[] of length L, off, len); chunk caps each read. */
        class Src extends Reader {
            final StringBuilder log=new StringBuilder(); final String s; int pos,chunk=Integer.MAX_VALUE;
            Src(String s){this.s=s;}
            Src chunk(int c){chunk=c;return this;}
            public int read(char[] b,int off,int len){
                log.append('r').append(b.length).append(':').append(off).append(',').append(len).append('=');
                if(pos>=s.length()){log.append("-1 ");return -1;}
                int n=Math.min(Math.min(len,chunk),s.length()-pos);s.getChars(pos,pos+n,b,off);pos+=n;log.append(n).append(' ');return n;
            }
            public boolean ready(){return pos<s.length();}
            public void close(){log.append("close ");}
        }
        /** A LineNumberReader subclass that logs the read(char[],int,int) calls it receives. */
        class Spy extends LineNumberReader {
            final StringBuilder log=new StringBuilder();
            Spy(Reader in,int sz){super(in,sz);}
            public int read(char[] b,int off,int len) throws IOException {log.append('R').append(b.length).append(':').append(off).append(',').append(len).append(' ');return super.read(b,off,len);}
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
            static String chars(char[] a,int off,int n){return n<=0?"":show(new String(a,off,n));}
            /** read() until -1 (and once more), logging value@lineNumber. */
            static String readAll(LineNumberReader r) throws IOException {
                StringBuilder o=new StringBuilder();int c;
                do{c=r.read();o.append(c).append('@').append(r.getLineNumber()).append(' ');}while(c>=0);
                c=r.read();return o.append(c).append('@').append(r.getLineNumber()).toString();
            }
            static String linesOf(String text,int size){
                return t(()->{LineNumberReader r=new LineNumberReader(new Src(text),size);StringBuilder o=new StringBuilder();String l;
                    do{l=r.readLine();o.append('[').append(show(l)).append(']').append(r.getLineNumber());}while(l!=null);
                    l=r.readLine();return o.append('[').append(show(l)).append(']').append(r.getLineNumber()).toString();});
            }

            public static String constructors(){
                Src s=new Src("abc");
                LineNumberReader r=new LineNumberReader(s);Object o=r;
                String a=r.getLineNumber()+","+(o instanceof BufferedReader)+","+r.markSupported()+",log="+s.log;
                r.setLineNumber(-5);a+=","+r.getLineNumber();r.setLineNumber(Integer.MAX_VALUE);a+=","+r.getLineNumber();
                a+=","+t(()->{r.read();r.read();r.read();return r.read()+"@"+r.getLineNumber();});
                a+="|"+t(()->{new LineNumberReader(s,0);return "ok";})+","+t(()->{new LineNumberReader(s,-1);return "ok";})
                  +","+ty(()->{new LineNumberReader(null);return "ok";})+","+ty(()->{new LineNumberReader(null,0);return "ok";});
                LineNumberReader q=new LineNumberReader(new Src("x\\ny"),1);q.setLineNumber(10);
                return a+"|"+t(()->readAll(q));
            }
            public static String readSingle(){
                StringBuilder o=new StringBuilder();
                String[] texts={"a\\nb\\r\\nc\\rd","","\\n","\\r","\\r\\n","\\r\\r\\n\\n","x","x\\n","\\u2028\\u0085\\u000b\\f"};
                int[] sizes={8192,1,2};
                for(String text:texts){for(int size:sizes)o.append(t(()->readAll(new LineNumberReader(new Src(text),size)))).append(' ');o.append(';');}
                return o.toString();
            }
            public static String readArrays(){
                StringBuilder o=new StringBuilder();
                LineNumberReader a=new LineNumberReader(new Src("ab\\r\\ncd\\re\\n\\nf"));char[] buf=new char[16];
                o.append(t(()->{int n=a.read(buf,2,3);String x=n+":"+chars(buf,2,n)+"@"+a.getLineNumber();n=a.read(buf,0,1);x+=","+n+":"+chars(buf,0,n)+"@"+a.getLineNumber();
                    n=a.read(buf,0,16);x+=","+n+":"+chars(buf,0,n)+"@"+a.getLineNumber();n=a.read(buf,0,16);x+=","+n+"@"+a.getLineNumber();n=a.read(buf,0,16);return x+","+n+"@"+a.getLineNumber();})).append(';');
                LineNumberReader b=new LineNumberReader(new Src("a\\r\\nb\\n"));
                o.append(t(()->{int n=b.read(buf,0,2);String x=n+"@"+b.getLineNumber();n=b.read(buf,0,1);x+=","+n+":"+chars(buf,0,n)+"@"+b.getLineNumber();
                    n=b.read(buf,0,0);x+=","+n+"@"+b.getLineNumber();n=b.read(buf,0,8);x+=","+n+":"+chars(buf,0,n)+"@"+b.getLineNumber();n=b.read(buf,0,8);return x+","+n+"@"+b.getLineNumber();})).append(';');
                LineNumberReader c=new LineNumberReader(new Src("x\\ry"));
                o.append(t(()->{int r1=c.read();int r2=c.read();int n=c.read(buf,0,8);return r1+","+r2+"@"+c.getLineNumber()+","+n+":"+chars(buf,0,n)+"@"+c.getLineNumber()+","+c.read()+"@"+c.getLineNumber();})).append(';');
                LineNumberReader d=new LineNumberReader(new Src("\\r\\nz"));
                o.append(t(()->{int r1=d.read();int n=d.read(buf,0,8);return r1+"@"+d.getLineNumber()+","+n+":"+chars(buf,0,n)+"@"+d.getLineNumber();})).append(';');
                LineNumberReader e=new LineNumberReader(new Src("ab\\r"));
                o.append(t(()->{int n=e.read(buf,0,8);int r1=e.read();return n+"@"+e.getLineNumber()+","+r1+"@"+e.getLineNumber();})).append(';');
                Src s=new Src("abc");LineNumberReader f=new LineNumberReader(s,4);char[] c4=new char[4];
                o.append(t(()->f.read(c4,-1,1))).append(';').append(t(()->f.read(c4,2,3))).append(';').append(t(()->f.read(c4,0,-1))).append(';')
                 .append(ty(()->f.read(null,0,1))).append(";log=").append(s.log).append(f.getLineNumber());
                return o.toString();
            }
            public static String readLines(){
                StringBuilder o=new StringBuilder();
                String[] texts={"a\\nb\\r\\nc\\rd\\n\\ne","x\\r","","\\n","\\r\\n","\\r\\r\\n\\n","no newline","\\u2028x\\u0085y"};
                int[] sizes={8192,1,2};
                for(String text:texts){for(int size:sizes)o.append(linesOf(text,size)).append(' ');o.append(';');}
                return o.toString();
            }
            public static String mixedCalls(){
                StringBuilder o=new StringBuilder();
                LineNumberReader a=new LineNumberReader(new Src("a\\r\\nb\\nc"));
                o.append(t(()->a.read()+","+a.read()+"@"+a.getLineNumber()+","+show(a.readLine())+"@"+a.getLineNumber()+","+show(a.readLine())+"@"+a.getLineNumber()+","+show(a.readLine())+"@"+a.getLineNumber())).append(';');
                LineNumberReader b=new LineNumberReader(new Src("a\\r\\nb"));
                o.append(t(()->show(b.readLine())+"@"+b.getLineNumber()+","+b.read()+"@"+b.getLineNumber()+","+b.read()+"@"+b.getLineNumber())).append(';');
                LineNumberReader c=new LineNumberReader(new Src("ab"));
                o.append(t(()->c.read()+","+c.read()+"@"+c.getLineNumber()+","+show(c.readLine())+"@"+c.getLineNumber()+","+show(c.readLine())+"@"+c.getLineNumber())).append(';');
                LineNumberReader d=new LineNumberReader(new Src("ab"));
                o.append(t(()->show(d.readLine())+"@"+d.getLineNumber()+","+d.read()+"@"+d.getLineNumber())).append(';');
                LineNumberReader e=new LineNumberReader(new Src("ab\\ncd"));char[] buf=new char[8];
                o.append(t(()->{int n=e.read(buf,0,4);return n+"@"+e.getLineNumber()+","+show(e.readLine())+"@"+e.getLineNumber()+","+show(e.readLine())+"@"+e.getLineNumber();})).append(';');
                LineNumberReader f=new LineNumberReader(new Src("q\\r\\nw"));
                o.append(t(()->{int n=f.read(buf,0,2);return n+"@"+f.getLineNumber()+","+show(f.readLine())+"@"+f.getLineNumber()+","+show(f.readLine())+"@"+f.getLineNumber();})).append(';');
                LineNumberReader g=new LineNumberReader(new Src("k\\r"),1);
                o.append(t(()->g.read()+","+g.read()+"@"+g.getLineNumber()+","+show(g.readLine())+"@"+g.getLineNumber()+","+g.read()+"@"+g.getLineNumber()));
                return o.toString();
            }
            public static String skipping(){
                StringBuilder o=new StringBuilder();
                LineNumberReader a=new LineNumberReader(new Src("a\\nb\\r\\nc\\rd"));
                o.append(t(()->a.skip(-1))).append(',').append(t(()->a.skip(Long.MIN_VALUE))).append(',').append(t(()->a.skip(0)+"@"+a.getLineNumber()))
                 .append(',').append(t(()->a.skip(4)+"@"+a.getLineNumber()+","+a.skip(2)+"@"+a.getLineNumber()+","+a.skip(100)+"@"+a.getLineNumber()+","+a.read()+"@"+a.getLineNumber()+","+a.skip(1)+"@"+a.getLineNumber())).append(';');
                LineNumberReader b=new LineNumberReader(new Src("ab"));
                o.append(t(()->b.read()+","+b.skip(1)+"@"+b.getLineNumber()+","+b.read()+"@"+b.getLineNumber())).append(';');
                LineNumberReader c=new LineNumberReader(new Src("ab"));
                o.append(t(()->c.read()+","+c.skip(0)+","+c.read()+","+c.read()+"@"+c.getLineNumber())).append(';');
                LineNumberReader d=new LineNumberReader(new Src("x\\r"));
                o.append(t(()->d.skip(2)+"@"+d.getLineNumber()+","+show(d.readLine())+"@"+d.getLineNumber())).append(';');
                Src s=new Src("0123456789\\nabcdefghij");Spy p=new Spy(s,4);
                o.append(t(()->p.skip(3)+","+p.skip(Long.MAX_VALUE)+"@"+p.getLineNumber()+","+p.skip(5)+" "+p.log+"| "+s.log)).append(';');
                Src q=new Src("abcdefghij").chunk(3);Spy r=new Spy(q,2);
                o.append(t(()->r.skip(7)+","+r.read()+" "+r.log+"| "+q.log));
                return o.toString();
            }
            public static String markReset(){
                StringBuilder o=new StringBuilder();
                LineNumberReader a=new LineNumberReader(new Src("a\\nb\\nc\\n"));
                o.append(t(()->{a.reset();return "x";})).append(',').append(t(()->{a.mark(-1);return "x";})).append(',').append(a.getLineNumber()).append(',');
                o.append(t(()->{a.readLine();a.mark(10);String x=a.readLine()+a.readLine()+"@"+a.getLineNumber();a.setLineNumber(40);a.reset();x+=","+a.getLineNumber()+","+show(a.readLine())+"@"+a.getLineNumber();
                    a.setLineNumber(7);a.reset();return x+","+a.getLineNumber()+","+a.read()+","+a.read()+"@"+a.getLineNumber();})).append(';');
                LineNumberReader b=new LineNumberReader(new Src("a"));
                o.append(t(()->{int c1=b.read();b.mark(5);int c2=b.read();String x=c1+","+c2+"@"+b.getLineNumber();b.reset();return x+","+b.getLineNumber()+","+b.read()+"@"+b.getLineNumber();})).append(';');
                LineNumberReader c=new LineNumberReader(new Src("\\r\\nab"),2);
                o.append(t(()->{int c1=c.read();c.mark(1);int c2=c.read();c.reset();return c1+","+c2+"@"+c.getLineNumber()+","+c.read()+"@"+c.getLineNumber()+","+c.read();})).append(';');
                LineNumberReader d=new LineNumberReader(new Src("\\r\\nab"),2);
                o.append(t(()->{d.read();d.mark(0);d.read();d.read();d.reset();return "x";})).append(';');
                LineNumberReader e=new LineNumberReader(new Src("\\r\\nab"));
                o.append(t(()->{e.read();e.mark(Integer.MAX_VALUE);return "x";})).append(',').append(t(()->{e.mark(Integer.MAX_VALUE-1);return e.read()+"@"+e.getLineNumber();})).append(';');
                LineNumberReader f=new LineNumberReader(new Src("1\\r\\n2"));
                o.append(t(()->{f.read();f.read();f.mark(3);String x=f.read()+"@"+f.getLineNumber();f.reset();x+=","+f.read()+","+f.read()+"@"+f.getLineNumber();f.reset();return x+","+show(f.readLine())+"@"+f.getLineNumber();}));
                return o.toString();
            }
            public static String closing(){
                Src s=new Src("a\\nb");LineNumberReader r=new LineNumberReader(s);char[] buf=new char[4];
                String a=t(()->{String l=r.readLine();r.close();r.close();return l+"@"+r.getLineNumber()+" "+s.log;});
                r.setLineNumber(9);
                return a+"|"+t(()->r.read())+","+t(()->r.read(buf,0,1))+","+t(()->r.read(buf,-1,1))+","+t(()->r.readLine())+","+t(()->r.skip(1))+","+t(()->r.skip(0))+","+t(()->r.skip(-1))
                    +","+t(()->{r.mark(1);return "x";})+","+t(()->{r.mark(-1);return "x";})+","+t(()->{r.reset();return "x";})+","+t(()->r.ready())+","+r.getLineNumber();
            }
            public static String subclassCalls(){
                Src s=new Src("one\\ntwo\\r\\nthree\\rfour\\nfive");Spy p=new Spy(s,4);char[] buf=new char[3];
                return t(()->{StringBuilder o=new StringBuilder();o.append(show(p.readLine())).append(',').append(p.read()).append(',');
                    int n=p.read(buf);o.append(n).append(':').append(chars(buf,0,n)).append('@').append(p.getLineNumber()).append(',');
                    o.append(p.skip(4)).append('@').append(p.getLineNumber()).append(',');
                    StringWriter w=new StringWriter();o.append(p.transferTo(w)).append(':').append(show(w.toString())).append('@').append(p.getLineNumber());
                    return o+" | "+p.log;});
            }
        }
        """;

    @Test void constructionAndSubclasses() throws Exception {
        matchesJvm(FIXTURE, "constructors", "subclassCalls");
    }

    @Test void readCharacters() throws Exception {
        matchesJvm(FIXTURE, "readSingle");
    }

    @Test void readArrays() throws Exception {
        matchesJvm(FIXTURE, "readArrays");
    }

    @Test void readLines() throws Exception {
        matchesJvm(FIXTURE, "readLines", "mixedCalls");
    }

    @Test void skip() throws Exception {
        matchesJvm(FIXTURE, "skipping");
    }

    @Test void markAndReset() throws Exception {
        matchesJvm(FIXTURE, "markReset");
    }

    @Test void closing() throws Exception {
        matchesJvm(FIXTURE, "closing");
    }
}
