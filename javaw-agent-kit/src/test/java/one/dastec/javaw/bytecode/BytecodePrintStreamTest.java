package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T36: java.io.PrintStream over an OutputStream. Expected values come from the JVM; see tasks/T36-print-stream.md. */
class BytecodePrintStreamTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.io.ByteArrayOutputStream;
        import java.io.FilterOutputStream;
        import java.io.IOException;
        import java.io.OutputStream;
        import java.io.PrintStream;
        import java.nio.charset.Charset;
        import java.nio.charset.StandardCharsets;
        import java.util.Locale;

        /** An OutputStream that logs every call PrintStream makes on it; it can be told to fail. */
        class Recorder extends OutputStream {
            final StringBuilder log=new StringBuilder();
            boolean failWrites,failFlush,failClose,failRuntime;
            private void check(String what) throws IOException {
                if(failRuntime)throw new IllegalStateException("boom "+what);
                if(failWrites)throw new IOException("write failed: "+what);
            }
            public void write(int b) throws IOException {check("w");log.append("w(").append(Fixture.show(b)).append(')');}
            public void write(byte[] b,int off,int len) throws IOException {
                check("a");log.append("a(");
                for(int i=0;i<len;i++)log.append(Fixture.show(b[off+i]));
                log.append(',').append(off).append(',').append(len).append(')');
            }
            public void flush() throws IOException {log.append("F;");if(failFlush)throw new IOException("flush failed");}
            public void close() throws IOException {log.append("C;");if(failClose)throw new IOException("close failed");}
            String take(){String s=log.toString();log.setLength(0);return s;}
        }

        /** A PrintStream subclass that records every call an overridable method receives. */
        class Logging extends PrintStream {
            final StringBuilder log=new StringBuilder();
            Logging(OutputStream o){super(o);}
            Logging(OutputStream o,boolean autoFlush){super(o,autoFlush);}
            public void write(int b){log.append("w(").append(Fixture.show(b)).append(')');super.write(b);}
            public void write(byte[] b) throws IOException {log.append("wb(").append(b.length).append(')');super.write(b);}
            public void write(byte[] b,int off,int len){log.append("wa(").append(off).append(',').append(len).append(')');super.write(b,off,len);}
            public void print(char c){log.append("pc(").append(c).append(')');super.print(c);}
            public void print(int i){log.append("pi(").append(i).append(')');super.print(i);}
            public void print(String s){log.append("ps(").append(s).append(')');super.print(s);}
            public void print(Object o){log.append("po(").append(o).append(')');super.print(o);}
            public void print(char[] s){log.append("pa(").append(s.length).append(')');super.print(s);}
            public void println(){log.append("ln;");super.println();}
            public void flush(){log.append("fl;");super.flush();}
            public void close(){log.append("cl;");super.close();}
            public PrintStream append(CharSequence csq){log.append("ap(").append(csq).append(')');return super.append(csq);}
            String take(){String s=log.toString();log.setLength(0);return s;}
        }

        /** Reads and assigns the protected members of PrintStream. */
        class Probe extends PrintStream {
            Probe(OutputStream o){super(o);}
            Probe(OutputStream o,boolean autoFlush){super(o,autoFlush);}
            OutputStream target(){return out;}
            void retarget(OutputStream o){out=o;}
            void fail(){setError();}
            void clear(){clearError();}
        }

        class Named {
            final String name;
            Named(String name){this.name=name;}
            public String toString(){return "<"+name+">";}
        }

        /** toString() returns null. */
        class Nully {
            public String toString(){return null;}
        }

        public class Fixture {
            interface Body { Object run() throws Exception; }
            /** The exception type and message; NullPointerException messages are not compared. */
            static String io(Body body){
                try{return String.valueOf(body.run());}
                catch(Exception e){
                    String m=e.getMessage();
                    if(e instanceof NullPointerException)m="npe";
                    return e.getClass().getSimpleName()+":"+m;
                }
            }
            /** The exception type and message, NullPointerException included. */
            static String msg(Body body){
                try{return String.valueOf(body.run());}
                catch(Exception e){return e.getClass().getSimpleName()+":"+e.getMessage();}
            }
            /** For failures whose message comes from another runtime class: only the exception type is compared. */
            static String type(Body body){
                try{return String.valueOf(body.run());}
                catch(Exception e){return e.getClass().getSimpleName();}
            }
            /** One byte: printable ASCII as itself, newline as '/', anything else as <hex>. */
            static String show(int b){
                b&=0xff;
                if(b=='\\n')return "/";
                if(b>=0x20 && b<0x7f)return String.valueOf((char)b);
                return "<"+Integer.toHexString(b)+">";
            }
            static String show(ByteArrayOutputStream bytes){
                StringBuilder out=new StringBuilder();
                for(byte b:bytes.toByteArray())out.append(show(b));
                return out.toString();
            }
            static String encoded(PrintStream ps,ByteArrayOutputStream bytes,String text){
                ps.print(text);String s=show(bytes);bytes.reset();return s;
            }

            // ---- constructors and charsets
            public static String constructors(){
                ByteArrayOutputStream b=new ByteArrayOutputStream();StringBuilder out=new StringBuilder();
                out.append(msg(()->new PrintStream((OutputStream)null))).append(';');
                out.append(msg(()->new PrintStream((OutputStream)null,true))).append(';');
                out.append(msg(()->new PrintStream((OutputStream)null,true,"UTF-8"))).append(';');
                out.append(msg(()->new PrintStream((OutputStream)null,false,(String)null))).append(';');
                out.append(msg(()->new PrintStream(b,true,(String)null))).append(';');
                out.append(msg(()->new PrintStream(b,false,"x-nope"))).append(';');
                out.append(msg(()->new PrintStream(b,false,"bad name!"))).append(';');
                out.append(msg(()->new PrintStream(b,false,""))).append(';');
                out.append(msg(()->new PrintStream(b,true,(Charset)null))).append(';');
                Probe p=new Probe(b);Probe q=new Probe(b,true);
                out.append(p.target()==b).append(',').append(q.target()==b).append(',').append(p.checkError()).append(',');
                PrintStream ps=new PrintStream(b);OutputStream asStream=ps;
                out.append(asStream instanceof FilterOutputStream).append(',').append(((Object)ps) instanceof Appendable).append(',');
                out.append(new PrintStream(b,false).checkError()).append(';');
                PrintStream none=new PrintStream((OutputStream)null,false,StandardCharsets.UTF_8);
                out.append(none.checkError()).append(',');none.print("x");out.append(none.checkError()).append(';');
                return out.append('[').append(show(b)).append(']').toString();
            }
            public static String charsets(){
                String sample="A\\u00e9\\u20ac\\ud83d\\ude00";StringBuilder out=new StringBuilder();
                String[] names={"UTF-8","ISO-8859-1","US-ASCII","UTF-16BE","utf-16le","latin1","UTF8"};
                ByteArrayOutputStream b=new ByteArrayOutputStream();
                out.append(encoded(new PrintStream(b),b,sample)).append(';');
                out.append(encoded(new PrintStream(b,true),b,sample)).append(';');
                for(String name:names)out.append(io(()->encoded(new PrintStream(b,false,name),b,sample))).append(';');
                out.append(encoded(new PrintStream(b,false,StandardCharsets.US_ASCII),b,sample)).append(';');
                out.append(encoded(new PrintStream(b,false,StandardCharsets.ISO_8859_1),b,"\\u00ff\\u0100")).append(';');
                return out.toString();
            }
            public static String utf16Mark(){
                ByteArrayOutputStream b=new ByteArrayOutputStream();StringBuilder out=new StringBuilder();
                out.append(io(()->{PrintStream ps=new PrintStream(b,false,"UTF-16");ps.print("");ps.print("a");ps.print("b");ps.println('c');return show(b);})).append(';');
                b.reset();
                PrintStream ps=new PrintStream(b,false,StandardCharsets.UTF_16);ps.write('!');ps.print("d");ps.print("e");
                return out.append(show(b)).toString();
            }
            public static String inheritedCharset(){
                ByteArrayOutputStream b=new ByteArrayOutputStream();StringBuilder out=new StringBuilder();
                out.append(io(()->{
                    PrintStream inner=new PrintStream(b,false,"UTF-16BE");
                    new PrintStream(inner).print("h\\u00e9");String s=show(b);b.reset();
                    new PrintStream(inner,true).print("i");s+=","+show(b);b.reset();
                    new PrintStream(inner,false,StandardCharsets.UTF_8).print("j\\u00e9");s+=","+show(b);b.reset();
                    new PrintStream(new PrintStream(inner)).print("k");return s+","+show(b);
                })).append(';');
                b.reset();
                Probe p=new Probe(b);p.print("\\u00e9");
                return out.append(show(b)).toString();
            }

            // ---- print and println
            public static String printPrimitives(){
                ByteArrayOutputStream b=new ByteArrayOutputStream();PrintStream ps=new PrintStream(b);
                ps.print(true);ps.print(' ');ps.print(false);ps.print('|');
                ps.print(Integer.MIN_VALUE);ps.print('|');ps.print(42);ps.print('|');
                ps.print(Long.MAX_VALUE);ps.print('|');ps.print(-7L);ps.print('|');
                ps.print(1.5f);ps.print('|');ps.print(-0.0f);ps.print('|');ps.print(Float.NaN);ps.print('|');
                ps.print(0.1);ps.print('|');ps.print(1.0E10);ps.print('|');ps.print(Double.NEGATIVE_INFINITY);ps.print('|');
                ps.print(new char[]{'c','h'});ps.print('|');ps.print("str");ps.print('|');ps.print(new Named("n"));ps.print('|');
                ps.print('\\u00e9');ps.print((String)null);ps.print((Object)null);ps.print("");ps.print(new char[0]);
                return show(b)+"|"+ps.checkError();
            }
            public static String printlnForms(){
                ByteArrayOutputStream b=new ByteArrayOutputStream();PrintStream ps=new PrintStream(b);
                ps.println();ps.println(true);ps.println('c');ps.println(-12);ps.println(9_000_000_000L);
                ps.println(2.5f);ps.println(-1.25);ps.println(new char[]{'x','y'});ps.println("s");ps.println(new Named("o"));ps.println("");
                ps.println((String)null);ps.println((Object)null);ps.println(new Nully());
                byte[] bytes=b.toByteArray();int cr=0;for(byte x:bytes)if(x=='\\r')cr++;
                return show(b)+"|"+bytes.length+"|"+cr;
            }
            public static String printNulls(){
                ByteArrayOutputStream b=new ByteArrayOutputStream();PrintStream ps=new PrintStream(b);StringBuilder out=new StringBuilder();
                out.append(io(()->{ps.print((char[])null);return "none";})).append(';');
                out.append(io(()->{ps.println((char[])null);return "none";})).append(';');
                out.append(io(()->{ps.print(new Nully());return "none";})).append(';');
                Logging lg=new Logging(b);
                out.append(io(()->{lg.println((char[])null);return "none";})).append(';');
                lg.println(new Nully());lg.print((String)null);
                out.append(lg.take()).append(';');
                return out.append(show(b)).append(';').append(ps.checkError()).toString();
            }
            public static String surrogates(){
                Recorder r=new Recorder();PrintStream ps=new PrintStream(r);StringBuilder out=new StringBuilder();
                ps.print('\\ud83d');out.append(r.take()).append(',');ps.print('\\ude00');out.append(r.take()).append(';');
                ps.print("x\\ud83d");ps.print("\\ude00y");out.append(r.take()).append(';');
                ps.print('\\ud83d');ps.print('z');out.append(r.take()).append(';');
                ps.print('\\ude00');ps.print("\\ud83d\\ud83d");ps.print("q");out.append(r.take()).append(';');
                ps.print('\\ud83d');ps.write('r');ps.print('\\ude00');out.append(r.take()).append(';');
                ps.print('\\ud83d');ps.println();out.append(r.take()).append(';');
                ps.print('\\ud83d');ps.flush();out.append(r.take()).append(',');ps.println('\\ude00');out.append(r.take()).append(';');
                ps.print('\\ud83d');ps.close();out.append(r.take()).append(';');
                ByteArrayOutputStream b=new ByteArrayOutputStream();
                out.append(io(()->{
                    PrintStream a=new PrintStream(b,false,"US-ASCII");a.print("\\ud83d\\ude00");a.print('\\ud83d');a.print("a");
                    PrintStream l=new PrintStream(b,false,"ISO-8859-1");l.print('\\ud83d');l.print('\\ude00');l.print('\\udc00');
                    PrintStream u=new PrintStream(b,false,"UTF-16BE");u.print('\\ud800');u.print("c");u.print('\\ud83d');u.print('\\ude00');
                    return show(b);
                }));
                return out.toString();
            }
            public static String longText(){
                ByteArrayOutputStream b=new ByteArrayOutputStream();PrintStream ps=new PrintStream(b);StringBuilder out=new StringBuilder();
                String x="x".repeat(9000);ps.print(x);ps.println(x);
                byte[] bytes=b.toByteArray();out.append(bytes.length).append(',').append(show(bytes[17999])).append(show(bytes[18000])).append(';');
                b.reset();String e="a"+"\\ud83d\\ude00".repeat(5000)+"\\u00e9".repeat(9000);ps.print(e);
                byte[] u=b.toByteArray();boolean same=java.util.Arrays.equals(u,e.getBytes(StandardCharsets.UTF_8));
                out.append(u.length).append(',').append(same).append(';');
                Recorder r=new Recorder();Logging lg=new Logging(r);lg.print("y".repeat(100));lg.println("\\u20ac".repeat(100));
                return out.append(lg.take()).append(',').append(r.take().length()).toString();
            }

            // ---- raw bytes and append
            public static String writeForms(){
                Recorder r=new Recorder();PrintStream ps=new PrintStream(r);StringBuilder out=new StringBuilder();
                ps.write(0x41);ps.write(0x1ff);ps.write(-1);ps.write(new byte[]{1,2,3},1,2);ps.write(new byte[0],0,0);
                out.append(r.take()).append(';');
                out.append(io(()->{ps.write(new byte[]{'B','C'});return "ok";})).append(',');
                ps.writeBytes(new byte[]{'D'});ps.writeBytes(new byte[0]);out.append(r.take()).append(';');
                Logging lg=new Logging(r);
                out.append(io(()->{lg.write(new byte[]{'E','F'});return "ok";})).append(',');
                lg.writeBytes(new byte[]{'G'});lg.write('H');
                return out.append(lg.take()).append(',').append(r.take()).append(',').append(ps.checkError()).toString();
            }
            public static String writeRanges(){
                ByteArrayOutputStream b=new ByteArrayOutputStream();PrintStream ps=new PrintStream(b);StringBuilder out=new StringBuilder();
                byte[] x={'1','2','3'};
                out.append(io(()->{ps.write(x,-1,1);return "ok";})).append(';');
                out.append(io(()->{ps.write(x,2,2);return "ok";})).append(';');
                out.append(io(()->{ps.write(x,0,-1);return "ok";})).append(';');
                out.append(io(()->{ps.write(null,0,0);return "ok";})).append(';');
                out.append(io(()->{ps.write((byte[])null);return "ok";})).append(';');
                out.append(io(()->{ps.writeBytes(null);return "ok";})).append(';');
                PrintStream overFilter=new PrintStream(new FilterOutputStream(b));
                out.append(io(()->{overFilter.write(x,1,5);return "ok";})).append(';');
                ps.write(x,1,2);
                return out.append(show(b)).append(';').append(ps.checkError()).append(',').append(overFilter.checkError()).toString();
            }
            public static String appendForms(){
                ByteArrayOutputStream b=new ByteArrayOutputStream();PrintStream ps=new PrintStream(b);
                PrintStream r1=ps.append("ab");PrintStream r2=ps.append('c');PrintStream r3=ps.append("0123456",2,5);
                ps.append(null);ps.append(null,1,3);ps.append(new StringBuilder("sb"));ps.append('\\u00e9');
                Appendable base=ps;
                String same=String.valueOf(r1==ps && r2==ps && r3==ps);
                String fail=type(()->{ps.append("abc",2,1);return "ok";});
                return io(()->{Appendable r4=base.append("!").append('?').append("xyz",1,2);return show(b)+"|"+same+"|"+(r4==ps)+"|"+fail;});
            }

            // ---- printf and format
            public static String formatForms(){
                ByteArrayOutputStream b=new ByteArrayOutputStream();PrintStream ps=new PrintStream(b);
                PrintStream r1=ps.printf("%s-%d-%x|%5s|%-4s|%%%n","a",12,255,"r","l");
                PrintStream r2=ps.format("[%3d][%b][%c]",7,null,'z');
                PrintStream r3=ps.printf(Locale.GERMANY,"%,.2f;",1234.5);
                PrintStream r4=ps.format(Locale.US,"%,d;",1234567);
                PrintStream r5=ps.printf(Locale.ROOT,"%.3f;",2.0);
                PrintStream r6=ps.format((Locale)null,"%,d;",-9876);
                ps.printf("no args;");ps.printf("%s;",(Object)null);ps.printf("%2$s%1$s;","x","y");
                ps.printf("%c%s;",'\\u00e9',"\\u20ac");
                boolean same=r1==ps && r2==ps && r3==ps && r4==ps && r5==ps && r6==ps;
                return show(b)+"|"+same+"|"+ps.checkError();
            }
            public static String formatFailures(){
                ByteArrayOutputStream b=new ByteArrayOutputStream();PrintStream ps=new PrintStream(b);StringBuilder out=new StringBuilder();
                ps.print("<");
                out.append(type(()->{ps.printf("ab%q","x");return "ok";})).append(';');
                out.append(type(()->{ps.printf("%d","x");return "ok";})).append(';');
                out.append(type(()->{ps.format("%2$s","only");return "ok";})).append(';');
                out.append(type(()->{ps.printf(Locale.GERMANY,"%,.2f",(Object)"t");return "ok";})).append(';');
                out.append(type(()->{ps.printf((String)null);return "ok";})).append(';');
                out.append(type(()->{ps.format(Locale.ROOT,(String)null);return "ok";})).append(';');
                ps.print(">");
                return out.append(show(b)).append(';').append(ps.checkError()).toString();
            }
            public static String formatCalls(){
                Recorder r=new Recorder();PrintStream ps=new PrintStream(r,true);StringBuilder out=new StringBuilder();
                ps.printf("%d",5);out.append(r.take()).append(',');
                ps.printf("plain");out.append(r.take()).append(',');
                ps.printf("%n");out.append(r.take()).append(',');
                ps.format(Locale.ROOT,"%s","q");out.append(r.take()).append(';');
                Recorder q=new Recorder();Logging lg=new Logging(q);
                PrintStream back=lg.printf("%s","t");out.append(back==lg).append(',').append(lg.take()).append(',').append(q.take()).append(',');
                lg.format(Locale.ROOT,"lit");out.append(lg.take()).append(',').append(q.take());
                return out.toString();
            }

            // ---- calls PrintStream makes on the underlying stream and on itself
            public static String underlyingCalls(){
                Recorder r=new Recorder();PrintStream ps=new PrintStream(r);StringBuilder out=new StringBuilder();
                ps.print("ab");ps.print(true);ps.print('q');ps.print(5L);ps.print(new char[]{'k'});ps.print((Object)null);ps.print("");
                out.append(r.take()).append('|');
                ps.println("ln");ps.println('c');ps.println(new char[]{'m'});ps.println();ps.println(1.5);ps.println(new Named("o"));
                out.append(r.take()).append('|');
                ps.append("ap").append('p').append("xyz",1,2);out.append(r.take()).append('|');
                ps.flush();out.append(r.take()).append(ps.checkError()).append(r.take()).append('|');
                ps.close();out.append(r.take()).append(',');ps.close();out.append(r.take());
                return out.toString();
            }
            public static String subclassCalls(){
                Recorder r=new Recorder();Logging w=new Logging(r);StringBuilder out=new StringBuilder();
                w.print("ab");out.append(w.take()).append(r.take()).append('|');
                w.print(7);out.append(w.take()).append('|');
                w.print(true);out.append(w.take()).append('|');
                w.print('c');out.append(w.take()).append('|');
                w.print(new char[]{'d','e'});out.append(w.take()).append('|');
                w.print(new Named("o"));out.append(w.take()).append('|');
                w.println("s");out.append(w.take()).append(r.take()).append('|');
                w.println(new Named("p"));out.append(w.take()).append('|');
                w.println(3);out.append(w.take()).append('|');
                w.println('x');out.append(w.take()).append('|');
                w.println(new char[]{'y'});out.append(w.take()).append('|');
                w.println(2.5);out.append(w.take()).append(r.take()).append('|');
                w.append("ap");w.append("xyz",0,2);w.append('q');out.append(w.take()).append('|');
                out.append(w.checkError()).append(w.take()).append('|');
                w.close();out.append(w.take()).append(r.take()).append(',').append(w.checkError()).append(w.take());
                return out.toString();
            }
            public static String autoFlush(){
                Recorder r=new Recorder();PrintStream ps=new PrintStream(r,true);StringBuilder out=new StringBuilder();
                ps.print("a");out.append(r.take()).append('|');
                ps.print("b\\nc");out.append(r.take()).append('|');
                ps.println("e");out.append(r.take()).append('|');
                ps.println();out.append(r.take()).append('|');
                ps.write('x');ps.write('\\n');ps.write(0x10a);out.append(r.take()).append('|');
                ps.write(new byte[]{'A','\\n'},0,2);out.append(r.take()).append('|');
                ps.print(new char[]{'q','\\n'});ps.print(new char[]{'s'});out.append(r.take()).append('|');
                ps.print("");ps.append('\\n');out.append(r.take()).append('|');
                ps.print('\\ud83d');out.append(r.take()).append(',');ps.print('\\ude00');out.append(r.take()).append('|');
                Recorder q=new Recorder();Logging lg=new Logging(q,true);
                lg.println("f");lg.print("g\\n");out.append(lg.take()).append(q.take()).append('|');
                Recorder z=new Recorder();PrintStream plain=new PrintStream(z);
                plain.println("h");plain.print("i\\n");plain.write('\\n');plain.write(new byte[]{'j'},0,1);out.append(z.take()).append(',');
                plain.flush();out.append(z.take());
                return out.toString();
            }
            public static String replacedOut(){
                ByteArrayOutputStream a=new ByteArrayOutputStream(),b=new ByteArrayOutputStream();Probe p=new Probe(a);StringBuilder out=new StringBuilder();
                p.print("1");p.retarget(b);p.print("2");p.println();p.write('3');
                out.append(show(a)).append(',').append(show(b)).append(',').append(p.checkError()).append(';');
                p.retarget(null);p.print("4");out.append(p.checkError()).append(',');
                p.clear();p.retarget(a);p.print("5");out.append(show(a)).append(',').append(p.checkError());
                return out.toString();
            }

            // ---- the error flag
            public static String writeErrors(){
                Recorder r=new Recorder();Probe p=new Probe(r);StringBuilder out=new StringBuilder();
                r.failWrites=true;
                p.print("x");out.append(p.checkError()).append(',').append(r.take()).append(';');
                p.clear();out.append(p.checkError()).append(',');
                p.write('c');out.append(p.checkError()).append(',');p.clear();
                p.write(new byte[]{'a'},0,1);out.append(p.checkError()).append(',');p.clear();
                p.writeBytes(new byte[]{'b'});out.append(p.checkError()).append(',');p.clear();
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
                p.clear();p.write('b');out.append(r.take()).append(',').append(p.checkError()).append(',');
                r.failFlush=false;p.clear();out.append(p.checkError()).append(',');
                r.failFlush=true;p.clear();p.flush();r.failFlush=false;out.append(p.checkError()).append(',').append(r.take());
                return out.toString();
            }
            public static String runtimeFailures(){
                Recorder r=new Recorder();PrintStream ps=new PrintStream(r,true);StringBuilder out=new StringBuilder();
                r.failRuntime=true;
                out.append(io(()->{ps.print("x");return "none";})).append(';');
                out.append(io(()->{ps.println();return "none";})).append(';');
                out.append(io(()->{ps.printf("y");return "none";})).append(';');
                out.append(io(()->{ps.write('z');return "none";})).append(';');
                out.append(io(()->{ps.write(new byte[1],0,1);return "none";})).append(';');
                r.failRuntime=false;
                return out.append(ps.checkError()).append(',').append(r.take()).toString();
            }
            public static String nestedCheckError(){
                Recorder r=new Recorder();PrintStream inner=new PrintStream(r);Probe outer=new Probe(inner);StringBuilder out=new StringBuilder();
                r.failWrites=true;outer.print("x");r.failWrites=false;
                out.append(outer.checkError()).append(',').append(inner.checkError()).append(',').append(r.take()).append(';');
                Probe o2=new Probe(new PrintStream(new ByteArrayOutputStream()));o2.fail();out.append(o2.checkError()).append(',');
                o2.close();out.append(o2.checkError()).append(',');o2.clear();out.append(o2.checkError()).append(';');
                outer.print("y");outer.close();out.append(r.take()).append(',').append(inner.checkError()).append(',').append(outer.checkError());
                return out.toString();
            }

            // ---- close
            public static String afterClose(){
                Recorder r=new Recorder();Probe p=new Probe(r,true);StringBuilder out=new StringBuilder();
                p.print("a");p.close();out.append(r.take()).append(',').append(p.target()==null).append(',').append(p.checkError()).append(';');
                p.write('b');out.append(p.checkError()).append(',');p.clear();
                p.write(new byte[]{'c'},0,1);out.append(p.checkError()).append(',');p.clear();
                p.print(1);out.append(p.checkError()).append(',');p.clear();
                p.println();out.append(p.checkError()).append(',');p.clear();
                p.println("d");out.append(p.checkError()).append(',');p.clear();
                p.append("e");out.append(p.checkError()).append(',');p.clear();
                p.printf("%q");out.append(p.checkError()).append(',');p.clear();
                p.format(Locale.ROOT,"%q");out.append(p.checkError()).append(',');p.clear();
                p.flush();out.append(p.checkError()).append(',');p.clear();
                p.print((char[])null);out.append(p.checkError()).append(',');p.clear();
                p.println((char[])null);out.append(p.checkError()).append(',');p.clear();
                p.print(new Nully());out.append(p.checkError()).append(',');p.clear();
                out.append(io(()->{p.write((byte[])null);return "none";})).append(',');
                out.append(io(()->{p.writeBytes(null);return "none";})).append(',');
                p.close();out.append(p.checkError()).append(',').append(r.take()).append(';');
                ByteArrayOutputStream b=new ByteArrayOutputStream();
                try(PrintStream t=new PrintStream(b)){t.print("res");}
                return out.append(show(b)).toString();
            }
            public static String failingClose(){
                Recorder r=new Recorder();Probe p=new Probe(r);StringBuilder out=new StringBuilder();
                r.failClose=true;
                p.close();out.append(p.checkError()).append(',').append(p.target()==null).append(',').append(r.take()).append(';');
                r.failClose=false;p.clear();p.close();out.append(p.checkError()).append(',').append(r.take()).append(';');
                Recorder f=new Recorder();Probe q=new Probe(f);f.failFlush=true;
                q.close();out.append(q.checkError()).append(',').append(f.take()).append(';');
                Recorder g=new Recorder();Logging lg=new Logging(g);
                lg.print('\\ud83d');lg.close();out.append(lg.take()).append(g.take()).append(',');lg.close();out.append(lg.take()).append(g.take()).append(';');
                PrintStream none=new PrintStream((OutputStream)null,false,StandardCharsets.UTF_8);
                out.append(io(()->{none.close();return "none";})).append(',').append(io(()->{none.close();return "none";})).append(',').append(none.checkError());
                return out.toString();
            }
        }
        """;

    @Test void constructorsAndCharsets() throws Exception {
        matchesJvm(FIXTURE, "constructors", "charsets", "utf16Mark", "inheritedCharset");
    }

    @Test void printAndPrintln() throws Exception {
        matchesJvm(FIXTURE, "printPrimitives", "printlnForms", "printNulls", "surrogates", "longText");
    }

    @Test void bytesAndAppend() throws Exception {
        matchesJvm(FIXTURE, "writeForms", "writeRanges", "appendForms");
    }

    @Test void formatting() throws Exception {
        matchesJvm(FIXTURE, "formatForms", "formatFailures", "formatCalls");
    }

    @Test void callPatterns() throws Exception {
        matchesJvm(FIXTURE, "underlyingCalls", "subclassCalls", "autoFlush", "replacedOut");
    }

    @Test void errorsAndClose() throws Exception {
        matchesJvm(FIXTURE, "writeErrors", "flushErrors", "runtimeFailures", "nestedCheckError", "afterClose", "failingClose");
    }

    @Test void leavesOutFileNameConstructor() {
        rejectsUnusedCall("java/io/PrintStream", "<init>", "(Ljava/lang/String;)V", INVOKESPECIAL, false);
    }
}
