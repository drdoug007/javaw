package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T39: java.io.UncheckedIOException and BufferedReader.lines(). Expected values come from the JVM; see tasks/T39-unchecked-io-lines.md. */
class BytecodeBufferedReaderLinesTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.io.BufferedReader;
        import java.io.EOFException;
        import java.io.IOException;
        import java.io.Reader;
        import java.io.StringReader;
        import java.io.UncheckedIOException;
        import java.util.Iterator;
        import java.util.List;
        import java.util.Spliterator;
        import java.util.stream.Collectors;
        import java.util.stream.Stream;

        /** A Reader over a String that logs close and throws IOException("disk") when a read starts at char failAt. */
        class Src extends Reader {
            final StringBuilder log=new StringBuilder(); final String s; int pos,failAt=-1;
            Src(String s){this.s=s;}
            Src failAt(int n){failAt=n;return this;}
            public int read(char[] b,int off,int len) throws IOException {
                if(pos==failAt){log.append("fail ");throw new IOException("disk");}
                if(pos>=s.length())return -1;
                int end=s.length();if(failAt>pos&&failAt<end)end=failAt;
                int n=Math.min(len,end-pos);s.getChars(pos,pos+n,b,off);pos+=n;return n;
            }
            public void close(){log.append("close ");}
        }
        /** A BufferedReader subclass that logs every readLine call and its result: lines() must go through readLine(). */
        class Spy extends BufferedReader {
            final StringBuilder log=new StringBuilder();
            Spy(Reader in){super(in);}
            public String readLine() throws IOException {String r=super.readLine();log.append("L").append(r==null?"-":r).append(' ');return r;}
        }
        /** An IOException subclass with its own toString. */
        class DiskFull extends IOException {
            DiskFull(String m){super(m);}
            public String toString(){return "DiskFull<"+getMessage()+">";}
        }

        public class Fixture {
            interface Op{Object run() throws Exception;}
            static String t(Op op){
                try{return String.valueOf(op.run());}
                catch(UncheckedIOException e){IOException c=e.getCause();return "UncheckedIOException:"+e.getMessage()+"/"+c.getClass().getSimpleName()+":"+c.getMessage();}
                catch(Exception e){return e.getClass().getSimpleName()+":"+e.getMessage();}
            }
            static String show(String s){return s==null?"null":s.replace("\\r","\\\\r").replace("\\n","\\\\n");}
            static String list(String text){return t(()->{BufferedReader b=new BufferedReader(new StringReader(text));StringBuilder o=new StringBuilder();
                b.lines().forEach(l->o.append('[').append(show(l)).append(']'));return o.append(b.readLine()).toString();});}

            public static String exceptionConstructors(){
                IOException io=new IOException("boom");
                UncheckedIOException a=new UncheckedIOException(io);
                UncheckedIOException b=new UncheckedIOException("wrapped",io);
                UncheckedIOException c=new UncheckedIOException(null,io);
                UncheckedIOException d=new UncheckedIOException(new IOException());
                UncheckedIOException e=new UncheckedIOException(new EOFException("eof"));
                UncheckedIOException f=new UncheckedIOException("m",new DiskFull("full"));
                IOException ac=a.getCause();
                Throwable bt=b;
                return a.getMessage()+"|"+(ac==io)+"|"+(b.getCause()==io)+"|"+b.getMessage()+"|"+c.getMessage()+"|"+d.getMessage()+"|"+e.getMessage()
                    +"|"+e.getCause().getClass().getSimpleName()+"|"+f.getMessage()+"|"+new UncheckedIOException(new DiskFull("x")).getMessage()
                    +"|"+a+"|"+b+"|"+(bt.getCause()==io)+"|"+(a instanceof RuntimeException)+"|"+a.getSuppressed().length;
            }
            public static String exceptionNullCause(){
                return t(()->new UncheckedIOException((IOException)null))+","+t(()->new UncheckedIOException("m",null))+","+t(()->new UncheckedIOException(null,null))
                    +","+t(()->{throw new UncheckedIOException(new IOException("x"));})+","+t(()->{throw new UncheckedIOException("top",new EOFException(null));});
            }
            public static String exceptionCatching(){
                StringBuilder o=new StringBuilder();
                try{throw new UncheckedIOException("u",new IOException("i"));}
                catch(RuntimeException r){o.append(r.getClass().getName()).append(' ').append(r.getCause().getClass().getName());}
                try{UncheckedIOException u=new UncheckedIOException(new IOException("i"));u.addSuppressed(new IOException("s"));throw u;}
                catch(UncheckedIOException u){o.append(' ').append(u.getSuppressed()[0].getMessage()).append(' ').append(u.getCause().getMessage());}
                return o.toString();
            }
            public static String linesTerminators(){
                return list("a\\nb\\r\\nc\\rd")+","+list("")+","+list("\\n")+","+list("x\\n\\n")+","+list("\\r\\r\\n\\n")+","+list("tail")+","+list("one\\r")+","+list(" sp \\t\\n");
            }
            public static String linesLong(){
                StringBuilder s=new StringBuilder();for(int i=0;i<3000;i++)s.append("line").append(i).append(i%3==0?"\\r\\n":"\\n");
                return t(()->{BufferedReader b=new BufferedReader(new StringReader(s.toString()),16);List<String> l=b.lines().collect(Collectors.toList());
                    return l.size()+" "+l.get(0)+" "+l.get(1234)+" "+l.get(2999)+" "+b.lines().count();});
            }
            public static String linesLaziness(){
                return t(()->{Spy b=new Spy(new StringReader("a\\nb\\nc\\nd\\ne"));
                    Stream<String> s=b.lines();String before=b.log.toString();
                    Stream<String> m=s.filter(x->!x.equals("b")).map(String::toUpperCase);String mid=b.log.toString();
                    String first=m.findFirst().get();String after=b.log.toString();
                    String next=b.readLine();
                    List<String> rest=b.lines().toList();
                    return "["+before+"]["+mid+"]"+first+"["+after+"]"+next+rest+"["+b.log+"]";});
            }
            public static String linesShortCircuit(){
                return t(()->{Spy b=new Spy(new StringReader("1\\n2\\n3\\n4\\n5\\n6"));
                    boolean any=b.lines().anyMatch(x->x.equals("2"));String l1=b.log.toString();b.log.setLength(0);
                    boolean none=b.lines().noneMatch(x->x.equals("4"));String l2=b.log.toString();b.log.setLength(0);
                    long n=b.lines().count();String l3=b.log.toString();b.log.setLength(0);
                    long z=b.lines().count();
                    return any+"["+l1+"]"+none+"["+l2+"]"+n+"["+l3+"]"+z+"["+b.log+"]";});
            }
            public static String linesIterator(){
                return t(()->{Spy b=new Spy(new StringReader("p\\nq\\n"));
                    Iterator<String> it=b.lines().iterator();String l0=b.log.toString();
                    boolean h1=it.hasNext(),h2=it.hasNext();String l1=b.log.toString();
                    String p=it.next(),q=it.next();String l2=b.log.toString();
                    boolean h3=it.hasNext();
                    String end=t(()->it.next());
                    return "["+l0+"]"+h1+h2+"["+l1+"]"+p+q+"["+l2+"]"+h3+" "+end;});
            }
            public static String linesSpliterator(){
                return t(()->{Spy b=new Spy(new StringReader("x\\ny\\nz"));
                    Spliterator<String> sp=b.lines().spliterator();
                    String c=sp.characteristics()+" "+sp.hasCharacteristics(Spliterator.ORDERED)+" "+sp.hasCharacteristics(Spliterator.NONNULL)+" "+sp.hasCharacteristics(Spliterator.SIZED)
                        +" "+(sp.estimateSize()==Long.MAX_VALUE)+" "+sp.getExactSizeIfKnown();
                    String l0=b.log.toString();
                    StringBuilder o=new StringBuilder();boolean adv=sp.tryAdvance(o::append);
                    sp.forEachRemaining(x->o.append('+').append(x));
                    boolean again=sp.tryAdvance(o::append);
                    return c+"["+l0+"]"+adv+o+again+"["+b.log+"]";});
            }
            public static String linesFailures(){
                String mid=t(()->{BufferedReader b=new BufferedReader(new Src("ab\\ncd\\nef").failAt(4),4);StringBuilder o=new StringBuilder();
                    try{b.lines().forEach(l->o.append('[').append(l).append(']'));}catch(UncheckedIOException e){o.append(e.getMessage()).append(' ').append(e.getCause().getMessage());}
                    return o.append(' ').append(t(()->b.readLine())).append(' ').append(t(()->b.lines().count())).toString();});
                String custom=t(()->{BufferedReader b=new BufferedReader(new Reader(){public int read(char[] c,int o,int l)throws IOException{throw new DiskFull("quota");}public void close(){}});return b.lines().count();});
                String closed=t(()->{BufferedReader b=new BufferedReader(new StringReader("a"));b.close();return b.lines().count();});
                String closedLater=t(()->{BufferedReader b=new BufferedReader(new StringReader("a\\nb"));Stream<String> s=b.lines();b.close();return s.toList();});
                String eofFirst=t(()->{BufferedReader b=new BufferedReader(new Src("abc").failAt(0));Stream<String> s=b.lines();return "created "+s.findFirst();});
                return mid+","+custom+","+closed+","+closedLater+","+eofFirst;
            }
            public static String linesClosing(){
                return t(()->{Src src=new Src("a\\nb\\nc");BufferedReader b=new BufferedReader(src);
                    Stream<String> s=b.lines();StringBuilder o=new StringBuilder();s.onClose(()->o.append("handler "));
                    String first=s.findFirst().get();s.close();
                    String reuse;try{s.count();reuse="reused";}catch(IllegalStateException e){reuse="ISE";}
                    String next=b.readLine();
                    try(Stream<String> t=b.lines()){o.append(t.toList());}
                    return first+" "+o+" "+reuse+" "+next+" ["+src.log+"] "+b.readLine();});
            }
        }
        """;

    @Test void uncheckedIOException() throws Exception {
        matchesJvm(FIXTURE, "exceptionConstructors", "exceptionNullCause", "exceptionCatching");
    }

    @Test void linesContent() throws Exception {
        matchesJvm(FIXTURE, "linesTerminators", "linesLong");
    }

    @Test void linesIsLazy() throws Exception {
        matchesJvm(FIXTURE, "linesLaziness", "linesShortCircuit");
    }

    @Test void linesTraversal() throws Exception {
        matchesJvm(FIXTURE, "linesIterator", "linesSpliterator");
    }

    @Test void linesFailuresAndClosing() throws Exception {
        matchesJvm(FIXTURE, "linesFailures", "linesClosing");
    }
}
