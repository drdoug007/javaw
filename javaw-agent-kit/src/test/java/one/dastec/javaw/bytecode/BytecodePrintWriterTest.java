package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** T27: java.io.PrintWriter over a Writer. Expected values come from the JVM; see tasks/T27-print-writer.md. */
@Tag("agent-pending")
class BytecodePrintWriterTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.io.IOException;
        import java.io.PrintWriter;
        import java.io.StringWriter;
        import java.io.Writer;
        import java.util.Locale;

        /** A Writer that logs every call PrintWriter makes on it; it can be told to fail. */
        class Recorder extends Writer {
            final StringBuilder log=new StringBuilder();
            boolean failWrites,failFlush,failClose,failRuntime;
            private void check(String what) throws IOException {
                if(failRuntime)throw new IllegalStateException("boom "+what);
                if(failWrites)throw new IOException("write failed: "+what);
            }
            public void write(int c) throws IOException {check("c");log.append("c(").append((char)c).append(')');}
            public void write(char[] b,int off,int len) throws IOException {check("a");log.append("a(").append(new String(b,off,len)).append(',').append(off).append(',').append(len).append(')');}
            public void write(String s) throws IOException {check("S");log.append("S(").append(s.replace('\\n','/')).append(')');}
            public void write(String s,int off,int len) throws IOException {check("s");log.append("s(").append(s.substring(off,off+len)).append(',').append(off).append(',').append(len).append(')');}
            public void flush() throws IOException {log.append("F;");if(failFlush)throw new IOException("flush failed");}
            public void close() throws IOException {log.append("C;");if(failClose)throw new IOException("close failed");}
            String take(){String s=log.toString();log.setLength(0);return s;}
        }

        /** A PrintWriter subclass that records every call an overridable method receives. */
        class Logging extends PrintWriter {
            final StringBuilder log=new StringBuilder();
            Logging(Writer w){super(w);}
            public void write(int c){log.append("w(").append((char)c).append(')');super.write(c);}
            public void write(char[] b){log.append("wa(").append(b.length).append(')');super.write(b);}
            public void write(char[] b,int off,int len){log.append("wao(").append(off).append(',').append(len).append(')');super.write(b,off,len);}
            public void write(String s){log.append("ws(").append(s).append(')');super.write(s);}
            public void write(String s,int off,int len){log.append("wso(").append(s).append(',').append(off).append(',').append(len).append(')');super.write(s,off,len);}
            public void print(char c){log.append("pc(").append(c).append(')');super.print(c);}
            public void print(int i){log.append("pi(").append(i).append(')');super.print(i);}
            public void print(String s){log.append("ps(").append(s).append(')');super.print(s);}
            public void print(Object o){log.append("po(").append(o).append(')');super.print(o);}
            public void print(char[] s){log.append("pa(").append(s.length).append(')');super.print(s);}
            public void println(){log.append("ln;");super.println();}
            public void flush(){log.append("fl;");super.flush();}
            public PrintWriter append(CharSequence csq){log.append("ap(").append(csq).append(')');return super.append(csq);}
            String take(){String s=log.toString();log.setLength(0);return s;}
        }

        /** Reads and assigns the protected members of PrintWriter. */
        class Probe extends PrintWriter {
            Probe(Writer w){super(w);}
            Probe(Writer w,boolean autoFlush){super(w,autoFlush);}
            boolean lockIsOut(){return lock==out;}
            Writer target(){return out;}
            void retarget(Writer w){out=w;}
            void fail(){setError();}
            void clear(){clearError();}
        }

        class Named {
            final String name;
            Named(String name){this.name=name;}
            public String toString(){return "<"+name+">";}
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
            /** For failures whose message comes from another runtime class: only the exception type is compared. */
            static String type(Body body){
                try{return String.valueOf(body.run());}
                catch(Exception e){return e.getClass().getSimpleName();}
            }
            static String nl(Object o){return String.valueOf(o).replace('\\n','/');}

            // ---- constructors, print and println
            public static String constructors(){
                StringWriter sw=new StringWriter();
                Probe p=new Probe(sw);Probe q=new Probe(sw,true);
                StringBuilder out=new StringBuilder();
                out.append(p.target()==sw).append(',').append(p.lockIsOut()).append(',').append(q.target()==sw).append(',').append(q.lockIsOut()).append(',');
                out.append(p.checkError()).append(',').append(new PrintWriter(sw).checkError()).append(',').append(new PrintWriter(sw,false).checkError()).append(';');
                out.append(io(()->new PrintWriter((Writer)null))).append(';');
                out.append(io(()->new PrintWriter((Writer)null,true))).append(';');
                return out.append('[').append(sw).append(']').toString();
            }
            public static String printPrimitives(){
                StringWriter sw=new StringWriter();PrintWriter pw=new PrintWriter(sw);
                pw.print(true);pw.print(' ');pw.print(false);pw.print('|');
                pw.print(Integer.MIN_VALUE);pw.print('|');pw.print(42);pw.print('|');
                pw.print(Long.MAX_VALUE);pw.print('|');pw.print(-7L);pw.print('|');
                pw.print(1.5f);pw.print('|');pw.print(-0.0f);pw.print('|');pw.print(Float.NaN);pw.print('|');
                pw.print(0.1);pw.print('|');pw.print(1.0E10);pw.print('|');pw.print(Double.NEGATIVE_INFINITY);pw.print('|');
                pw.print(new char[]{'c','h'});pw.print('|');pw.print("str");pw.print('|');pw.print(new Named("n"));pw.print('|');
                pw.print((String)null);pw.print((Object)null);pw.print("");
                return sw+"|"+pw.checkError();
            }
            public static String printNulls(){
                StringWriter sw=new StringWriter();PrintWriter pw=new PrintWriter(sw);StringBuilder out=new StringBuilder();
                out.append(io(()->{pw.print((char[])null);return "none";})).append(';');
                out.append(io(()->{pw.println((char[])null);return "none";})).append(';');
                out.append(io(()->{pw.write((char[])null);return "none";})).append(';');
                out.append(io(()->{pw.write((String)null);return "none";})).append(';');
                out.append(io(()->{pw.write((char[])null,0,0);return "none";})).append(';');
                pw.println((String)null);pw.println((Object)null);
                return out.append(nl(sw)).append(';').append(pw.checkError()).toString();
            }
            public static String printlnForms(){
                StringWriter sw=new StringWriter();PrintWriter pw=new PrintWriter(sw);
                pw.println();pw.println(true);pw.println('c');pw.println(-12);pw.println(9_000_000_000L);
                pw.println(2.5f);pw.println(-1.25);pw.println(new char[]{'x','y'});pw.println("s");pw.println(new Named("o"));pw.println("");
                String s=sw.toString();
                return nl(s)+"|"+s.length()+"|"+s.indexOf('\\r');
            }

            // ---- write and append
            public static String writeForms(){
                StringWriter sw=new StringWriter();PrintWriter pw=new PrintWriter(sw);
                pw.write('a');pw.write(0x10042);pw.write(new char[]{'c','d'});pw.write(new char[]{'e','f','g','h'},1,2);
                pw.write(new char[2],2,0);pw.write("ij");pw.write("klmno",1,3);pw.write("",0,0);pw.write((String)null,1,2);
                return sw+"|"+pw.checkError();
            }
            public static String writeRanges(){
                StringWriter sw=new StringWriter();PrintWriter pw=new PrintWriter(sw);StringBuilder out=new StringBuilder();
                char[] b={'1','2','3'};
                out.append(io(()->{pw.write(b,-1,1);return "ok";})).append(';');
                out.append(io(()->{pw.write(b,2,2);return "ok";})).append(';');
                out.append(io(()->{pw.write(b,0,-1);return "ok";})).append(';');
                out.append(type(()->{pw.write("abc",2,5);return "ok";})).append(';');
                out.append(type(()->{pw.write("abc",-1,1);return "ok";})).append(';');
                return out.append(sw).append(';').append(pw.checkError()).toString();
            }
            public static String appendForms(){
                StringWriter sw=new StringWriter();PrintWriter pw=new PrintWriter(sw);
                PrintWriter r1=pw.append("ab");PrintWriter r2=pw.append('c');PrintWriter r3=pw.append("0123456",2,5);
                pw.append(null);pw.append(null,1,3);pw.append(new StringBuilder("sb"));
                Writer base=pw;
                String same=String.valueOf(r1==pw && r2==pw && r3==pw);
                String fail=type(()->{pw.append("abc",2,1);return "ok";});
                return io(()->{Writer r4=base.append("!").append('?');return sw+"|"+same+"|"+(r4==pw)+"|"+fail;});
            }

            // ---- printf and format
            public static String formatForms(){
                StringWriter sw=new StringWriter();PrintWriter pw=new PrintWriter(sw);
                PrintWriter r1=pw.printf("%s-%d-%x|%5s|%-4s|%%%n","a",12,255,"r","l");
                PrintWriter r2=pw.format("[%3d][%b][%c]",7,null,'z');
                PrintWriter r3=pw.printf(Locale.GERMANY,"%,.2f;",1234.5);
                PrintWriter r4=pw.format(Locale.US,"%,d;",1234567);
                PrintWriter r5=pw.printf(Locale.ROOT,"%.3f;",2.0);
                PrintWriter r6=pw.format((Locale)null,"%,d;",-9876);
                pw.printf("no args;");pw.printf("%s;",(Object)null);pw.printf("%2$s%1$s;","x","y");
                boolean same=r1==pw && r2==pw && r3==pw && r4==pw && r5==pw && r6==pw;
                return nl(sw)+"|"+same+"|"+pw.checkError();
            }
            public static String formatFailures(){
                StringWriter sw=new StringWriter();PrintWriter pw=new PrintWriter(sw);StringBuilder out=new StringBuilder();
                pw.print("<");
                out.append(type(()->{pw.printf("ab%q","x");return "ok";})).append(';');
                out.append(type(()->{pw.printf("%d","x");return "ok";})).append(';');
                out.append(type(()->{pw.format("%2$s","only");return "ok";})).append(';');
                out.append(type(()->{pw.printf(Locale.GERMANY,"%,.2f",(Object)"t");return "ok";})).append(';');
                out.append(type(()->{pw.printf((String)null);return "ok";})).append(';');
                out.append(type(()->{pw.format(Locale.ROOT,(String)null);return "ok";})).append(';');
                pw.print(">");
                return out.append(sw).append(';').append(pw.checkError()).toString();
            }
            public static String autoFlush(){
                Recorder r=new Recorder();PrintWriter pw=new PrintWriter(r,true);StringBuilder out=new StringBuilder();
                pw.print("a");pw.write('b');pw.write("c");pw.append("d");pw.print(1);out.append(r.take()).append('|');
                pw.println("e");out.append(r.take()).append('|');
                pw.println();out.append(r.take()).append('|');
                pw.println(3);out.append(r.take()).append('|');
                pw.printf("%s","f");out.append(r.take()).append('|');
                pw.format(Locale.ROOT,"g");out.append(r.take()).append('|');
                Recorder q=new Recorder();PrintWriter plain=new PrintWriter(q);
                plain.println("h");plain.printf("i");plain.format(Locale.ROOT,"j");plain.println();out.append(q.take()).append('|');
                plain.flush();out.append(q.take());
                return out.toString();
            }

            // ---- calls PrintWriter makes on the underlying Writer and on itself
            public static String underlyingCalls(){
                Recorder r=new Recorder();PrintWriter pw=new PrintWriter(r);StringBuilder out=new StringBuilder();
                pw.write('x');pw.write(new char[]{'y','z'});pw.write(new char[]{'1','2','3'},1,2);pw.write("abc");pw.write("defg",1,2);
                out.append(r.take()).append('|');
                pw.print(true);pw.print('q');pw.print(5L);pw.print(new char[]{'k'});pw.print((Object)null);out.append(r.take()).append('|');
                pw.println("ln");pw.println('c');pw.println(new char[]{'m'});out.append(r.take()).append('|');
                pw.append("ap").append('p').append("xyz",1,2);out.append(r.take()).append('|');
                pw.flush();out.append(pw.checkError()).append(r.take()).append('|');
                pw.close();pw.close();out.append(r.take());
                return out.toString();
            }
            public static String subclassCalls(){
                Logging w=new Logging(new StringWriter());StringBuilder out=new StringBuilder();
                w.print("ab");out.append(w.take()).append('|');
                w.print(7);out.append(w.take()).append('|');
                w.print(true);out.append(w.take()).append('|');
                w.print('c');out.append(w.take()).append('|');
                w.print(new char[]{'d','e'});out.append(w.take()).append('|');
                w.print(new Named("o"));out.append(w.take()).append('|');
                w.println("s");out.append(w.take()).append('|');
                w.println(new Named("p"));out.append(w.take()).append('|');
                w.println(3);out.append(w.take()).append('|');
                w.println('x');out.append(w.take()).append('|');
                w.println(new char[]{'y'});out.append(w.take()).append('|');
                w.println(2.5);out.append(w.take()).append('|');
                w.append("ap");w.append("xyz",0,2);w.append('q');out.append(w.take()).append('|');
                w.write("wx");w.write(new char[]{'1','2'});out.append(w.take()).append('|');
                out.append(w.checkError()).append(w.take()).append('|');
                w.close();out.append(w.checkError()).append(w.take());
                return out.toString();
            }
            public static String replacedOut(){
                StringWriter a=new StringWriter(),b=new StringWriter();Probe p=new Probe(a);StringBuilder out=new StringBuilder();
                p.print("1");p.retarget(b);p.print("2");p.println();
                out.append(a).append(',').append(nl(b)).append(',').append(p.checkError()).append(',').append(p.lockIsOut()).append(';');
                p.retarget(null);p.print("3");out.append(p.checkError()).append(',');
                p.close();p.clear();out.append(p.checkError()).append(',');
                p.retarget(a);p.print("4");out.append(a).append(',').append(p.checkError());
                return out.toString();
            }

            // ---- the error flag
            public static String writeErrors(){
                Recorder r=new Recorder();Probe p=new Probe(r);StringBuilder out=new StringBuilder();
                r.failWrites=true;
                p.print("x");out.append(p.checkError()).append(',').append(r.take()).append(';');
                p.clear();out.append(p.checkError()).append(',');
                p.write('c');out.append(p.checkError()).append(',');p.clear();
                p.write(new char[]{'a'},0,1);out.append(p.checkError()).append(',');p.clear();
                p.println();out.append(p.checkError()).append(',');p.clear();
                p.printf("%d",1);out.append(p.checkError()).append(',');p.clear();
                p.append('z');out.append(p.checkError()).append(',');p.clear();
                r.failWrites=false;p.print("ok");out.append(p.checkError()).append(',').append(r.take()).append(';');
                p.fail();out.append(p.checkError()).append(',');p.print("still");out.append(r.take()).append(',');
                p.clear();out.append(p.checkError());
                return out.toString();
            }
            public static String flushErrors(){
                Recorder r=new Recorder();Probe p=new Probe(r,true);StringBuilder out=new StringBuilder();
                r.failFlush=true;
                p.print("a");out.append(p.checkError()).append(',').append(r.take()).append(';');
                p.clear();p.println("b");out.append(r.take()).append(',');
                p.clear();out.append(p.checkError()).append(',');
                r.failFlush=false;p.clear();out.append(p.checkError()).append(',');
                r.failFlush=true;p.clear();p.flush();r.failFlush=false;out.append(p.checkError()).append(',').append(r.take());
                return out.toString();
            }
            public static String runtimeFailures(){
                Recorder r=new Recorder();PrintWriter pw=new PrintWriter(r,true);StringBuilder out=new StringBuilder();
                r.failRuntime=true;
                out.append(io(()->{pw.print("x");return "none";})).append(';');
                out.append(io(()->{pw.println();return "none";})).append(';');
                out.append(io(()->{pw.printf("y");return "none";})).append(';');
                out.append(io(()->{pw.write('z');return "none";})).append(';');
                r.failRuntime=false;
                return out.append(pw.checkError()).append(',').append(r.take()).toString();
            }
            public static String nestedCheckError(){
                Recorder r=new Recorder();PrintWriter inner=new PrintWriter(r);Probe outer=new Probe(inner);StringBuilder out=new StringBuilder();
                r.failWrites=true;outer.print("x");r.failWrites=false;
                out.append(outer.checkError()).append(',').append(inner.checkError()).append(',').append(r.take()).append(';');
                Probe o2=new Probe(new PrintWriter(new StringWriter()));o2.fail();out.append(o2.checkError()).append(',');
                o2.close();out.append(o2.checkError()).append(',');o2.clear();out.append(o2.checkError()).append(';');
                outer.print("y");outer.close();out.append(r.take()).append(',').append(inner.checkError()).append(',').append(outer.checkError());
                return out.toString();
            }

            // ---- close
            public static String afterClose(){
                Recorder r=new Recorder();Probe p=new Probe(r,true);StringBuilder out=new StringBuilder();
                p.print("a");p.close();out.append(r.take()).append(',').append(p.target()==null).append(',').append(p.checkError()).append(';');
                p.write('b');out.append(p.checkError()).append(',');p.clear();
                p.write("c");out.append(p.checkError()).append(',');p.clear();
                p.print(1);out.append(p.checkError()).append(',');p.clear();
                p.println();out.append(p.checkError()).append(',');p.clear();
                p.append("d");out.append(p.checkError()).append(',');p.clear();
                p.printf("%q");out.append(p.checkError()).append(',');p.clear();
                p.format(Locale.ROOT,"%q");out.append(p.checkError()).append(',');p.clear();
                p.flush();out.append(p.checkError()).append(',');p.clear();
                p.print((String)null);out.append(p.checkError()).append(',');p.clear();
                out.append(io(()->{p.write((String)null);return "none";})).append(',');
                out.append(io(()->{p.print((char[])null);return "none";})).append(',');
                p.close();out.append(p.checkError()).append(',').append(r.take()).append(';');
                StringWriter sw=new StringWriter();
                try(PrintWriter t=new PrintWriter(sw)){t.print("res");}
                return out.append(sw).toString();
            }
            public static String failingClose(){
                Recorder r=new Recorder();Probe p=new Probe(r);StringBuilder out=new StringBuilder();
                r.failClose=true;
                p.close();out.append(p.checkError()).append(',').append(p.target()==r).append(',').append(r.take()).append(';');
                p.clear();p.print("x");out.append(p.checkError()).append(',').append(r.take()).append(';');
                r.failClose=false;p.close();out.append(p.checkError()).append(',').append(p.target()==null).append(',').append(r.take());
                return out.toString();
            }
        }
        """;

    @Test void constructorsAndPrint() throws Exception {
        matchesJvm(FIXTURE, "constructors", "printPrimitives", "printNulls", "printlnForms");
    }

    @Test void writesAndAppends() throws Exception {
        matchesJvm(FIXTURE, "writeForms", "writeRanges", "appendForms");
    }

    @Test void formatting() throws Exception {
        matchesJvm(FIXTURE, "formatForms", "formatFailures", "autoFlush");
    }

    @Test void callPatterns() throws Exception {
        matchesJvm(FIXTURE, "underlyingCalls", "subclassCalls", "replacedOut");
    }

    @Test void errorFlag() throws Exception {
        matchesJvm(FIXTURE, "writeErrors", "flushErrors", "runtimeFailures", "nestedCheckError");
    }

    @Test void closing() throws Exception {
        matchesJvm(FIXTURE, "afterClose", "failingClose");
    }

    @Test void leavesOutFileNameConstructor() {
        rejectsUnusedCall("java/io/PrintWriter", "<init>", "(Ljava/lang/String;)V", INVOKESPECIAL, false);
    }
}
