package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T26: java.io.StringWriter and java.io.CharArrayWriter. Expected values come from the JVM; see tasks/T26-string-writer.md. */
class BytecodeStringWriterTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.io.CharArrayWriter;
        import java.io.IOException;
        import java.io.StringWriter;
        import java.io.Writer;

        /** A StringWriter subclass that records every call an overridable write/append method receives. */
        class LoggingStringWriter extends StringWriter {
            final StringBuilder log=new StringBuilder();
            LoggingStringWriter(){super();}
            LoggingStringWriter(int size){super(size);}
            public void write(int c){log.append("w(").append(c).append(')');super.write(c);}
            public void write(char[] b,int off,int len){log.append("wa(").append(b.length).append(',').append(off).append(',').append(len).append(')');super.write(b,off,len);}
            public void write(String s){log.append("ws(").append(s).append(')');super.write(s);}
            public void write(String s,int off,int len){log.append("wso(").append(s).append(',').append(off).append(',').append(len).append(')');super.write(s,off,len);}
            public StringWriter append(CharSequence csq){log.append("a(").append(csq).append(')');return super.append(csq);}
            boolean lockIsBuffer(){return lock==getBuffer();}
        }

        /** A CharArrayWriter subclass that records every call an overridable write/append method receives. */
        class LoggingCharArrayWriter extends CharArrayWriter {
            final StringBuilder log=new StringBuilder();
            LoggingCharArrayWriter(){super();}
            LoggingCharArrayWriter(int size){super(size);}
            public void write(int c){log.append("w(").append(c).append(')');super.write(c);}
            public void write(char[] b,int off,int len){log.append("wa(").append(b.length).append(',').append(off).append(',').append(len).append(')');super.write(b,off,len);}
            public void write(String s,int off,int len){log.append("wso(").append(s).append(',').append(off).append(',').append(len).append(')');super.write(s,off,len);}
            public CharArrayWriter append(CharSequence csq){log.append("a(").append(csq).append(')');return super.append(csq);}
            boolean lockIsThis(){return lock==this;}
        }

        /** Reads the protected fields of CharArrayWriter. */
        class Probe extends CharArrayWriter {
            Probe(){super();}
            Probe(int size){super(size);}
            int capacity(){return buf.length;}
            int counted(){return count;}
        }

        /** Assigns the protected fields directly, as library subclasses do. */
        class Preloaded extends CharArrayWriter {
            Preloaded(){super(0);buf=new char[]{'x','y','z'};count=3;}
            void truncate(int n){count=n;}
            void poke(int i,char c){buf[i]=c;}
            int capacity(){return buf.length;}
        }

        /** A Writer that records the array length, offset and length of every write(char[],int,int) call. */
        class Recorder extends Writer {
            final StringBuilder log=new StringBuilder();
            public void write(char[] b,int off,int len){
                log.append("[").append(b.length).append(',').append(off).append(',').append(len).append(':').append(new String(b,off,len)).append(']');
            }
            public void flush(){log.append("flush;");}
            public void close(){log.append("close;");}
        }

        class Failing extends Writer {
            public void write(char[] b,int off,int len) throws IOException {throw new IOException("sink full "+len);}
            public void flush(){}
            public void close(){}
        }

        public class Fixture {
            interface Body { Object run() throws Exception; }
            static String io(Body body){
                try{return String.valueOf(body.run());}
                catch(Exception e){
                    String m=e.getMessage();
                    if(e instanceof NullPointerException)m="npe";
                    return e.getClass().getSimpleName()+":"+m;
                }
            }
            /** For failures whose message comes from String/StringBuffer: only the exception type is compared. */
            static String type(Body body){
                try{return String.valueOf(body.run());}
                catch(Exception e){return e.getClass().getSimpleName();}
            }

            // ---- StringWriter
            public static String stringWriterWrites() throws IOException {
                StringWriter w=new StringWriter();
                w.write('a');w.write(0x10042);w.write(new char[]{'c','d','e'});w.write(new char[]{'f','g','h','i'},1,2);
                w.write(new char[3],3,0);w.write("jk");w.write("lmnop",1,3);w.write("",0,0);w.write("q",1,0);
                return w.toString()+"|"+w.getBuffer().length()+"|"+new StringWriter().toString().isEmpty()+"|"+new StringWriter(0).toString().isEmpty();
            }
            public static String stringWriterSizes(){
                StringBuilder out=new StringBuilder();
                for(int n:new int[]{0,1,100,-1,Integer.MIN_VALUE}){
                    out.append(io(()->{StringWriter w=new StringWriter(n);w.write("ok");return w;})).append(';');
                }
                return out.toString();
            }
            public static String stringWriterNulls(){
                StringWriter w=new StringWriter();StringBuilder out=new StringBuilder();
                w.write((String)null);
                w.write((String)null,1,2);
                out.append(w).append(';');
                out.append(io(()->{w.write((char[])null,0,0);return "none";})).append(';');
                out.append(io(()->{w.write((char[])null);return "none";})).append(';');
                return out.append(w).toString();
            }
            public static String stringWriterAppends(){
                StringWriter w=new StringWriter();
                StringWriter r1=w.append("ab");StringWriter r2=w.append('c');StringWriter r3=w.append("0123456",2,5);
                w.append(null);w.append(null,1,3);w.append(new StringBuilder("sb"));w.append(new StringBuffer("xyz"),1,2);
                Writer base=w;
                String ok=String.valueOf(r1==w && r2==w && r3==w);
                return io(()->{Writer r4=base.append("!").append('?').append("[]",0,1);return w+"|"+ok+"|"+(r4==w);});
            }
            public static String stringWriterBuffer(){
                StringWriter w=new StringWriter(4);w.write("hello");
                StringBuffer b=w.getBuffer();
                b.append(" world");b.setCharAt(0,'H');
                String mid=w.toString()+","+(w.getBuffer()==b)+","+b.length();
                b.setLength(2);w.write('!');
                String after=w+","+b;
                b.insert(0,'<');w.append('>');
                return mid+"|"+after+"|"+w+"|"+new LoggingStringWriter().lockIsBuffer()+","+new LoggingStringWriter(7).lockIsBuffer();
            }
            public static String stringWriterFlushClose(){
                StringWriter w=new StringWriter();w.write("a");
                return io(()->{
                    w.flush();w.close();w.write("b");w.close();w.append('c');w.flush();
                    try(StringWriter t=new StringWriter()){t.write("d");w.write(t.toString());}
                    Writer base=w;base.close();base.write("e");base.flush();
                    return w.toString();
                });
            }
            public static String stringWriterCharRanges(){
                StringWriter w=new StringWriter();w.write("x");StringBuilder out=new StringBuilder();
                char[] b={'1','2','3','4','5'};
                int[][] cases={{-1,1},{0,-1},{2,4},{6,0},{1,Integer.MAX_VALUE},{Integer.MIN_VALUE,0},{5,0},{0,5}};
                for(int[] c:cases)out.append(io(()->{w.write(b,c[0],c[1]);return "ok";})).append(';');
                return out.append(w).toString();
            }
            public static String stringWriterStringRanges(){
                StringWriter w=new StringWriter();w.write("x");StringBuilder out=new StringBuilder();
                int[][] cases={{-1,1},{0,-1},{2,4},{4,0},{1,Integer.MAX_VALUE},{3,0},{0,3}};
                for(int[] c:cases)out.append(type(()->{w.write("abc",c[0],c[1]);return "ok";})).append(';');
                out.append(type(()->{w.append("abc",2,1);return "ok";})).append(';');
                out.append(type(()->{w.append("abc",-1,1);return "ok";})).append(';');
                out.append(type(()->{w.append(null,0,5);return "ok";})).append(';');
                out.append(type(()->{w.append(new StringBuilder("abc"),1,4);return "ok";})).append(';');
                return out.append(w).toString();
            }
            public static String stringWriterSubclassCalls() throws IOException {
                LoggingStringWriter w=new LoggingStringWriter();StringBuilder out=new StringBuilder();
                w.append("ab");out.append(w.log).append('|');w.log.setLength(0);
                w.append("xyz",1,3);out.append(w.log).append('|');w.log.setLength(0);
                w.append(null);w.append(null,0,2);out.append(w.log).append('|');w.log.setLength(0);
                w.append('c');out.append(w.log).append('|');w.log.setLength(0);
                w.write(new char[]{'d','e'});out.append(w.log).append('|');w.log.setLength(0);
                w.write("fg");w.write("hij",1,1);w.write('k');out.append(w.log).append('|');w.log.setLength(0);
                return out.append(w).toString();
            }

            // ---- CharArrayWriter
            public static String charArrayCapacities(){
                return new Probe().capacity()+","+new Probe(0).capacity()+","+new Probe(5).capacity()+","+new Probe().counted()+","+new CharArrayWriter().size()+","+new CharArrayWriter(9).size();
            }
            public static String charArrayGrowthByOne(){
                Probe p=new Probe(0);StringBuilder out=new StringBuilder();
                for(int i=0;i<9;i++){p.write('a'+i);out.append(p.capacity()).append(',');}
                Probe q=new Probe();for(int i=0;i<33;i++)q.write('0');
                return out.append('|').append(q.capacity()).append(',').append(q.size()).toString();
            }
            public static String charArrayGrowthByArray() throws IOException {
                Probe p=new Probe(3);StringBuilder out=new StringBuilder();
                p.write(new char[10],0,10);out.append(p.capacity()).append(',');
                p.write('x');out.append(p.capacity()).append(',');
                p.write(new char[50],0,50);out.append(p.capacity()).append(',');
                p.write(new char[3],0,3);out.append(p.capacity()).append(',');
                p.write(new char[0],0,0);out.append(p.capacity()).append(',').append(p.size()).append('|');
                Probe s=new Probe(2);s.write("abcde",0,5);out.append(s.capacity()).append(',');
                s.write("f");out.append(s.capacity()).append(',');
                s.append("0123456789").append("0123456789");out.append(s.capacity()).append(',').append(s.size());
                Probe z=new Probe(0);z.write(new char[0],0,0);z.write("",0,0);out.append('|').append(z.capacity());
                return out.toString();
            }
            public static String charArraySizes(){
                StringBuilder out=new StringBuilder();
                for(int n:new int[]{0,1,-1,-32,Integer.MIN_VALUE}){
                    out.append(io(()->{CharArrayWriter w=new CharArrayWriter(n);w.write('k');return w;})).append(';');
                }
                return out.toString();
            }
            public static String charArrayFields(){
                Preloaded p=new Preloaded();StringBuilder out=new StringBuilder();
                out.append(p.size()).append(p).append(',');
                p.write('D');out.append(p.capacity()).append(p).append(',');
                p.truncate(2);out.append(p.size()).append(p).append(new String(p.toCharArray())).append(',');
                p.write('Z');p.poke(0,'a');out.append(p).append(',').append(p.capacity()).append(',');
                p.append("123");out.append(p).append(',').append(p.capacity());
                return out.toString();
            }
            public static String charArrayWrites() throws IOException {
                CharArrayWriter w=new CharArrayWriter(2);
                w.write('a');w.write(0x10042);w.write(new char[]{'c','d'});w.write(new char[]{'e','f','g','h'},1,2);
                w.write(new char[4],4,0);w.write("ij");w.write("klmno",1,3);w.write("p",1,0);
                return io(()->{w.write("q");return w.size()+":"+w+":"+w.toCharArray().length;});
            }
            public static String charArrayCharRanges(){
                CharArrayWriter w=new CharArrayWriter();w.write('x');StringBuilder out=new StringBuilder();
                char[] b={'1','2','3','4','5'};
                int[][] cases={{-1,1},{0,-1},{2,4},{6,0},{1,Integer.MAX_VALUE},{Integer.MIN_VALUE,0},{5,0},{0,5}};
                for(int[] c:cases)out.append(io(()->{w.write(b,c[0],c[1]);return "ok";})).append(';');
                return out.append(w.size()).append(w).toString();
            }
            public static String charArrayStringFailures(){
                Probe w=new Probe(2);w.write('x');StringBuilder out=new StringBuilder();
                out.append(type(()->{w.write("abc",1,5);return "ok";})).append(w.capacity()).append(w.size()).append(';');
                out.append(type(()->{w.write("abc",1,-1);return "ok";})).append(w.capacity()).append(w.size()).append(';');
                out.append(type(()->{w.write("abc",-1,2);return "ok";})).append(w.capacity()).append(w.size()).append(';');
                out.append(type(()->{w.write("abc",4,0);return "ok";})).append(w.capacity()).append(w.size()).append(';');
                out.append(type(()->{w.write("abc",3,0);return "ok";})).append(w.capacity()).append(w.size()).append(';');
                out.append(type(()->{w.append("abc",2,1);return "ok";})).append(w.size()).append(';');
                out.append(io(()->{w.write((String)null);return "ok";})).append(';');
                out.append(io(()->{w.write((String)null,0,0);return "ok";})).append(';');
                out.append(io(()->{w.write((char[])null,0,0);return "ok";})).append(';');
                out.append(io(()->{w.write((char[])null);return "ok";})).append(';');
                return out.append(w.size()).append(w).toString();
            }
            public static String charArrayAppends(){
                CharArrayWriter w=new CharArrayWriter();
                CharArrayWriter r1=w.append("ab");CharArrayWriter r2=w.append('c');CharArrayWriter r3=w.append("0123456",2,5);
                w.append(null);w.append(null,1,3);w.append(new StringBuilder("sb"));w.append(new StringBuffer("xyz"),1,2);
                Writer base=w;
                String ok=String.valueOf(r1==w && r2==w && r3==w);
                return io(()->{Writer r4=base.append("!").append('?').append("[]",0,1);return w+"|"+ok+"|"+(r4==w)+"|"+w.size();});
            }
            public static String charArrayCopiesAndReset(){
                Probe p=new Probe(0);p.append("abcde");
                char[] a=p.toCharArray();a[0]='Z';char[] b=p.toCharArray();
                String first=a.length+","+b[0]+","+(a!=b)+","+new CharArrayWriter().toCharArray().length+","+new CharArrayWriter(0).toString().isEmpty();
                int before=p.capacity();p.reset();
                String mid=p.size()+","+p.toCharArray().length+","+p.capacity()+","+before+","+p.toString().isEmpty();
                p.write('q');p.reset();p.reset();p.append("rs");
                return first+"|"+mid+"|"+p.size()+p+p.capacity();
            }
            public static String charArrayWriteTo(){
                CharArrayWriter w=new CharArrayWriter();w.append("abc");
                Recorder r=new Recorder();
                return io(()->{
                    w.writeTo(r);
                    new CharArrayWriter(4).writeTo(r);
                    Probe big=new Probe(2);big.append("hello");big.writeTo(r);
                    CharArrayWriter copy=new CharArrayWriter();copy.write('>');w.writeTo(copy);w.writeTo(copy);
                    StringWriter sw=new StringWriter();w.writeTo(sw);
                    return r.log+"|"+copy+"|"+sw+"|"+w.size();
                });
            }
            public static String charArrayWriteToFailures(){
                CharArrayWriter w=new CharArrayWriter();w.append("hi");StringBuilder out=new StringBuilder();
                out.append(io(()->{w.writeTo(new Failing());return "none";})).append(';');
                out.append(io(()->{w.writeTo(null);return "none";})).append(';');
                out.append(io(()->{new CharArrayWriter().writeTo(null);return "none";})).append(';');
                return out.append(w).toString();
            }
            public static String charArrayFlushClose(){
                CharArrayWriter w=new CharArrayWriter();w.write('a');
                w.flush();w.close();w.write('b');w.close();w.append("c");w.flush();
                return io(()->{
                    try(CharArrayWriter t=new CharArrayWriter()){t.write('d');t.writeTo(w);}
                    Writer base=w;base.close();base.write("e");base.flush();
                    return w.size()+":"+w;
                });
            }
            public static String charArraySubclassCalls() throws IOException {
                LoggingCharArrayWriter w=new LoggingCharArrayWriter(1);StringBuilder out=new StringBuilder();
                w.append("ab");out.append(w.log).append('|');w.log.setLength(0);
                w.append("xyz",1,3);out.append(w.log).append('|');w.log.setLength(0);
                w.append(null);w.append(null,0,2);out.append(w.log).append('|');w.log.setLength(0);
                w.append('c');out.append(w.log).append('|');w.log.setLength(0);
                w.write(new char[]{'d','e'});out.append(w.log).append('|');w.log.setLength(0);
                out.append(io(()->{w.write("fg");w.write("hij",1,1);w.write('k');return w.log;})).append('|');w.log.setLength(0);
                return out.append(w).append(',').append(w.lockIsThis()).append(',').append(new LoggingCharArrayWriter().lockIsThis()).toString();
            }
        }
        """;

    @Test void stringWriterWritesAndSizes() throws Exception {
        matchesJvm(FIXTURE, "stringWriterWrites", "stringWriterSizes", "stringWriterNulls", "stringWriterFlushClose");
    }

    @Test void stringWriterAppendsAndBuffer() throws Exception {
        matchesJvm(FIXTURE, "stringWriterAppends", "stringWriterBuffer", "stringWriterSubclassCalls");
    }

    @Test void stringWriterRanges() throws Exception {
        matchesJvm(FIXTURE, "stringWriterCharRanges", "stringWriterStringRanges");
    }

    @Test void charArrayWriterGrowth() throws Exception {
        matchesJvm(FIXTURE, "charArrayCapacities", "charArrayGrowthByOne", "charArrayGrowthByArray", "charArraySizes", "charArrayFields");
    }

    @Test void charArrayWriterWrites() throws Exception {
        matchesJvm(FIXTURE, "charArrayWrites", "charArrayCharRanges", "charArrayStringFailures", "charArrayAppends");
    }

    @Test void charArrayWriterCopiesAndWriteTo() throws Exception {
        matchesJvm(FIXTURE, "charArrayCopiesAndReset", "charArrayWriteTo", "charArrayWriteToFailures", "charArrayFlushClose");
    }

    @Test void charArrayWriterSubclassCalls() throws Exception {
        matchesJvm(FIXTURE, "charArraySubclassCalls");
    }
}
