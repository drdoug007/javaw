package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** T34: java.io.OutputStreamWriter and the PrintWriter(OutputStream) constructors. Expected values come from the JVM; see tasks/T34-output-stream-writer.md. */
@Tag("agent-pending")
class BytecodeOutputStreamWriterTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.io.ByteArrayOutputStream;
        import java.io.Closeable;
        import java.io.Flushable;
        import java.io.IOException;
        import java.io.OutputStream;
        import java.io.OutputStreamWriter;
        import java.io.PrintWriter;
        import java.io.UnsupportedEncodingException;
        import java.io.Writer;
        import java.nio.charset.Charset;
        import java.nio.charset.StandardCharsets;

        /** Logs every call: [off,len:hex] = write(byte[], off, len) (hex only up to 12 bytes), wN = write(int N), F = flush, C = close. */
        class Rec extends OutputStream {
            final StringBuilder log=new StringBuilder(); final ByteArrayOutputStream all=new ByteArrayOutputStream();
            public void write(int b){log.append('w').append(b).append(' ');all.write(b);}
            public void write(byte[] b,int off,int len){
                log.append('[').append(off).append(',').append(len);
                if(len<=12){log.append(':');for(int i=0;i<len;i++)log.append(Fixture.hex2(b[off+i]));}
                log.append("] ");all.write(b,off,len);
            }
            public void flush(){log.append("F ");}
            public void close(){log.append("C ");}
            String take(){String s=log.toString();log.setLength(0);return s;}
        }
        /** Logs like Rec and throws the configured exception from write, flush or close. */
        class Bad extends OutputStream {
            final StringBuilder log=new StringBuilder(); final Exception onWrite,onFlush,onClose;
            Bad(Exception onWrite,Exception onFlush,Exception onClose){this.onWrite=onWrite;this.onFlush=onFlush;this.onClose=onClose;}
            static void raise(Exception e) throws IOException {if(e instanceof IOException)throw (IOException)e;if(e!=null)throw (RuntimeException)e;}
            public void write(int b) throws IOException {log.append('w').append(b).append(' ');raise(onWrite);}
            public void write(byte[] b,int off,int len) throws IOException {log.append('[').append(len).append("] ");raise(onWrite);}
            public void flush() throws IOException {log.append("F ");raise(onFlush);}
            public void close() throws IOException {log.append("C ");raise(onClose);}
        }
        /** Records which overridable methods OutputStreamWriter calls on itself, then runs the JDK version. */
        class Spy extends OutputStreamWriter {
            final StringBuilder log=new StringBuilder();
            Spy(OutputStream out){super(out);}
            Object lockObject(){return lock;}
            public void write(int c) throws IOException {log.append("c(").append((char)c).append(')');super.write(c);}
            public void write(char[] b,int off,int len) throws IOException {log.append("a(").append(off).append(',').append(len).append(')');super.write(b,off,len);}
            public void write(String s,int off,int len) throws IOException {log.append("s(").append(s).append(',').append(off).append(',').append(len).append(')');super.write(s,off,len);}
            public void write(String s) throws IOException {log.append("S(").append(s).append(')');super.write(s);}
            public Writer append(CharSequence c) throws IOException {log.append("A(").append(c).append(')');return super.append(c);}
            public void flush() throws IOException {log.append("F;");super.flush();}
        }
        /** Like Spring's ContentCachingResponseWrapper.ResponsePrintWriter: a PrintWriter over a named-charset OutputStreamWriter. */
        class ResponseWriter extends PrintWriter {
            ResponseWriter(OutputStream out,String encoding) throws UnsupportedEncodingException {super(new OutputStreamWriter(out,encoding));}
            public void write(String s,int off,int len){super.write(s,off,len);super.flush();}
        }

        public class Fixture {
            interface Op{Object run() throws Exception;}
            interface W{void run(OutputStreamWriter w) throws IOException;}
            static String hex2(byte b){return Integer.toHexString((b&0xff)|0x100).substring(1);}
            static String hex(byte[] a){StringBuilder s=new StringBuilder();for(byte x:a)s.append(hex2(x));return s.toString();}
            static String t(Op op){
                try{return String.valueOf(op.run());}catch(Exception e){return e.getClass().getSimpleName()+":"+e.getMessage();}
            }
            static String ty(Op op){
                try{return String.valueOf(op.run());}catch(Exception e){return e.getClass().getSimpleName();}
            }
            /** Writes with w on a fresh writer for charset cs over a ByteArrayOutputStream, closes it, and returns the bytes as hex. */
            static String enc(Charset cs,W w){
                ByteArrayOutputStream b=new ByteArrayOutputStream();OutputStreamWriter o=new OutputStreamWriter(b,cs);
                try{w.run(o);o.close();}catch(Exception e){return e.getClass().getSimpleName()+":"+e.getMessage();}
                return hex(b.toByteArray());
            }
            static String enc(Charset cs,String s){return enc(cs,w->w.write(s));}
            static String sum(byte[] a){long h=0;for(byte x:a)h=h*31+(x&0xff);return a.length+"#"+h;}

            public static String encodingNames(){
                StringBuilder out=new StringBuilder();
                Charset[] sets={StandardCharsets.UTF_8,StandardCharsets.ISO_8859_1,StandardCharsets.US_ASCII,StandardCharsets.UTF_16BE,StandardCharsets.UTF_16LE,StandardCharsets.UTF_16};
                for(Charset c:sets)out.append(new OutputStreamWriter(new Rec(),c).getEncoding()).append(',');
                out.append('|');
                String[] names={"UTF-8","utf8","Utf-8","ISO-8859-1","latin1","ISO8859_1","8859_1","US-ASCII","ascii","646","UTF-16BE","x-UTF-16LE","utf-16","UnicodeBig"};
                for(String n:names)out.append(t(()->new OutputStreamWriter(new Rec(),n).getEncoding())).append(',');
                out.append('|').append(new OutputStreamWriter(new Rec()).getEncoding());
                out.append('|').append(t(()->new OutputStreamWriter(new Rec(),Charset.forName("latin1")).getEncoding()));
                return out.toString();
            }
            public static String constructorFailures(){
                StringBuilder out=new StringBuilder();
                String[] bad={"","x-no-such-charset","bad name","_utf8","utf-8?"};
                for(String n:bad)out.append(t(()->new OutputStreamWriter(new Rec(),n))).append(',');
                out.append('|').append(t(()->new OutputStreamWriter(new Rec(),(String)null)));
                out.append(',').append(t(()->new OutputStreamWriter(new Rec(),(Charset)null)));
                out.append(',').append(t(()->new OutputStreamWriter(null)));
                out.append(',').append(t(()->new OutputStreamWriter(null,"UTF-8")));
                out.append(',').append(t(()->new OutputStreamWriter(null,(String)null)));
                out.append(',').append(t(()->new OutputStreamWriter(null,"x-no-such-charset")));
                out.append(',').append(t(()->new OutputStreamWriter(null,StandardCharsets.UTF_8)));
                out.append(',').append(t(()->{try{new OutputStreamWriter(new Rec(),"nope");return "x";}catch(IOException e){return "io "+e.getMessage();}}));
                return out.toString();
            }
            public static String typesAndLock(){
                Rec r=new Rec();Spy s=new Spy(r);Object o=new OutputStreamWriter(r);
                return (s.lockObject()==r)+","+(o instanceof Writer)+","+(o instanceof Appendable)+","+(o instanceof Closeable)+","+(o instanceof Flushable)
                        +","+(new UnsupportedEncodingException("u") instanceof IOException)+","+r.take();
            }

            public static String utf8Encoding(){
                Charset u=StandardCharsets.UTF_8;
                return enc(u,"A\\u00e9\\u20ac\\ud83d\\ude00")+","+enc(u,"\\u0000\\u007f\\u0080\\u07ff\\u0800\\uffff")+","+enc(u,"\\udc00x")+","+enc(u,"\\ud800x")
                        +","+enc(u,"\\ud800\\ud800\\udc00")+","+enc(u,"x\\ud83d")+","+enc(u,"\\udbff\\udfff\\ud800\\udc00")+","+enc(u,"")+","+enc(u,"\\ude00\\ud83d");
            }
            public static String singleByteEncoding(){
                String s="A\\u00e9\\u00ff\\u0100\\u20ac\\ud83d\\ude00\\udc00\\ud800x\\u007f\\u0080";
                return enc(StandardCharsets.ISO_8859_1,s)+","+enc(StandardCharsets.US_ASCII,s)+","+enc(StandardCharsets.ISO_8859_1,"z\\ud83d")+","+enc(StandardCharsets.US_ASCII,"\\ud83d\\ud83d\\ude00");
            }
            public static String utf16Encoding(){
                String s="A\\u00e9\\ud83d\\ude00\\udc00\\ud800x";
                return enc(StandardCharsets.UTF_16BE,s)+","+enc(StandardCharsets.UTF_16LE,s)+","+enc(StandardCharsets.UTF_16,s)
                        +","+enc(StandardCharsets.UTF_16,"")+","+enc(StandardCharsets.UTF_16,w->{w.write("a");w.write("b");})
                        +","+enc(StandardCharsets.UTF_16,w->{w.write("");w.flush();})+","+enc(StandardCharsets.UTF_16,"\\ud800")+","+enc(StandardCharsets.UTF_16LE,"q\\ud83d");
            }

            public static String splitPairs(){
                Rec r=new Rec();OutputStreamWriter w=new OutputStreamWriter(r,StandardCharsets.UTF_8);
                return t(()->{w.write("a\\ud83d");w.flush();String a=r.take();w.write("\\ude00b");w.flush();String b=r.take();
                    w.write(0xd83d);w.write(0xde01);w.flush();String c=r.take();
                    w.write(new char[]{'c','\\ud83d'});w.write(new char[]{'\\ude02'},0,1);w.flush();String d=r.take();
                    w.write("\\ud83d");w.write("");w.write(new char[0]);w.write("\\ude03");w.flush();String e=r.take();
                    return a+"/"+b+"/"+c+"/"+d+"/"+e;})
                        +"|"+enc(StandardCharsets.ISO_8859_1,w2->{w2.write("y\\ud83d");w2.write("\\ude00z");})
                        +"|"+enc(StandardCharsets.UTF_16LE,w2->{w2.write('\\ud83d');w2.write('\\ude00');})
                        +"|"+enc(StandardCharsets.UTF_16,w2->{w2.write("\\ud83d");w2.write("\\ude00");});
            }
            public static String unpairedAcrossWrites(){
                Charset u=StandardCharsets.UTF_8;
                return enc(u,w->{w.write("\\ud83d");w.write("y");})+","+enc(u,w->{w.write("\\ud800");w.write("\\ud83d");w.write("\\ude00");})
                        +","+enc(u,w->{w.write("\\ud800");w.write("\\ud800\\udc00z");})+","+enc(u,w->{w.write("\\ud800");w.write("\\udc00");w.write("\\udc00");})
                        +","+enc(u,w->{w.write("\\ud83d");w.write("\\ud83d");})+","+enc(StandardCharsets.UTF_16BE,w->{w.write("\\ud83d");w.write("y");})
                        +","+enc(StandardCharsets.US_ASCII,w->{w.write("\\ud800");w.write("\\ud800");w.write("x");});
            }
            public static String pendingHighSurrogate(){
                Rec r=new Rec();OutputStreamWriter w=new OutputStreamWriter(r,StandardCharsets.UTF_8);
                return t(()->{w.write("\\ud83d");w.flush();String a=r.take();w.close();return a+"/"+r.take()+"/"+hex(r.all.toByteArray());});
            }

            public static String flushPattern(){
                Rec r=new Rec();OutputStreamWriter w=new OutputStreamWriter(r);
                return t(()->{w.write("hello");String a=r.take();w.flush();String b=r.take();w.flush();String c=r.take();
                    w.write("");w.write('x');w.write("yz",1,1);w.append("\\u00e9");String d=r.take();w.flush();String e=r.take();w.close();return a+"/"+b+"/"+c+"/"+d+"/"+e+"/"+r.take();});
            }
            public static String bufferFills(){
                StringBuilder out=new StringBuilder();
                Rec r=new Rec();OutputStreamWriter w=new OutputStreamWriter(r,StandardCharsets.UTF_8);
                out.append(t(()->{w.write("a".repeat(8192));String a=r.take();w.write('b');String b=r.take();w.write("c".repeat(10000));String c=r.take();
                    w.close();return a+"/"+b+"/"+c+"/"+r.take()+"/"+sum(r.all.toByteArray());}));
                Rec e=new Rec();OutputStreamWriter x=new OutputStreamWriter(e,StandardCharsets.UTF_8);
                out.append('|').append(t(()->{x.write("\\u20ac".repeat(2731));String a=e.take();x.write(new char[3000]);String b=e.take();x.flush();return a+"/"+b+"/"+e.take()+"/"+sum(e.all.toByteArray());}));
                return out.toString();
            }
            public static String bufferGrowth(){
                StringBuilder out=new StringBuilder();
                Rec r=new Rec();OutputStreamWriter w=new OutputStreamWriter(r,StandardCharsets.UTF_8);
                out.append(t(()->{w.write("a".repeat(100));String a=r.take();w.write("b".repeat(500));String b=r.take();w.write("c".repeat(1000));String c=r.take();
                    w.write("d".repeat(3000));String d=r.take();w.write("e".repeat(5000));String e=r.take();w.flush();return a+"/"+b+"/"+c+"/"+d+"/"+e+"/"+r.take();}));
                Rec s=new Rec();OutputStreamWriter l=new OutputStreamWriter(s,StandardCharsets.ISO_8859_1);
                out.append('|').append(t(()->{l.write("a".repeat(600));String a=s.take();l.write('b');String b=s.take();l.flush();return a+"/"+b+"/"+s.take();}));
                Rec q=new Rec();OutputStreamWriter m=new OutputStreamWriter(q,StandardCharsets.US_ASCII);
                out.append('|').append(t(()->{for(int i=0;i<513;i++)m.write('x');String a=q.take();m.flush();return a+"/"+q.take();}));
                Rec p=new Rec();OutputStreamWriter n=new OutputStreamWriter(p,StandardCharsets.UTF_8);
                out.append('|').append(t(()->{for(int i=0;i<509;i++)n.write('y');String a=p.take();n.write("\\ud83d\\ude00");String b=p.take();n.flush();return a+"/"+b+"/"+p.take();}));
                Rec v=new Rec();OutputStreamWriter k=new OutputStreamWriter(v,StandardCharsets.UTF_16);
                out.append('|').append(t(()->{k.write("z".repeat(200));String a=v.take();k.write("z");String b=v.take();k.flush();return a+"/"+b+"/"+v.take();}));
                return out.toString();
            }
            public static String closeSequence(){
                Rec r=new Rec();OutputStreamWriter w=new OutputStreamWriter(r,StandardCharsets.UTF_8);
                String s=t(()->{w.write("ab");w.close();String a=r.take();w.close();return a+"/"+r.take()+"/"+w.getEncoding();});
                s+="|"+t(()->{w.write("x");return "x";})+","+t(()->{w.write('x');return "x";})+","+t(()->{w.write(new char[]{'x'});return "x";})
                        +","+t(()->{w.write(new char[2],5,1);return "x";})+","+t(()->{w.write((char[])null,0,1);return "x";})
                        +","+t(()->{w.append("q");return "x";})+","+t(()->{w.flush();return "x";})+","+t(()->{w.write("abc",0,-1);return "x";})
                        +","+ty(()->{w.write("abc",2,5);return "x";})+","+t(()->{w.write("");return "x";})+","+r.take();
                Rec e=new Rec();OutputStreamWriter x=new OutputStreamWriter(e);
                return s+"|"+t(()->{x.close();return e.take()+x.getEncoding();});
            }

            public static String writeForms(){
                ByteArrayOutputStream b=new ByteArrayOutputStream();OutputStreamWriter w=new OutputStreamWriter(b,StandardCharsets.UTF_8);
                return t(()->{w.write(0x41);w.write(0x1f600);w.write(new char[]{'b','c'});w.write(new char[]{'d','e','f','g'},1,2);w.write("hij");w.write("klmn",1,2);
                    Writer r1=w.append("op");Writer r2=w.append(null);w.append("qrst",1,3);w.append(null,1,3);w.append('\\u00e9');w.write(new char[3],1,0);w.write("abc",3,0);
                    w.flush();return hex(b.toByteArray())+","+(r1==w)+","+(r2==w)+","+w.getEncoding();});
            }
            public static String charRanges(){
                ByteArrayOutputStream b=new ByteArrayOutputStream();OutputStreamWriter w=new OutputStreamWriter(b);char[] c={'a','b','c'};
                return t(()->{w.write(c,-1,1);return "x";})+","+t(()->{w.write(c,2,2);return "x";})+","+t(()->{w.write(c,0,-1);return "x";})
                        +","+t(()->{w.write(c,4,0);return "x";})+","+t(()->{w.write(c,1,Integer.MAX_VALUE);return "x";})+","+t(()->{w.write((char[])null,-1,0);return "x";})
                        +","+ty(()->{w.write((char[])null,0,0);return "x";})+","+ty(()->{w.write((char[])null);return "x";})+","+t(()->{w.write(c,3,0);w.write(c,1,2);w.flush();return hex(b.toByteArray());});
            }
            public static String stringRanges(){
                ByteArrayOutputStream b=new ByteArrayOutputStream();OutputStreamWriter w=new OutputStreamWriter(b);
                return t(()->{w.write("abc",0,-1);return "x";})+","+ty(()->{w.write("abc",-1,1);return "x";})+","+ty(()->{w.write("abc",2,5);return "x";})
                        +","+ty(()->{w.write("abc",4,0);return "x";})+","+ty(()->{w.write((String)null,0,1);return "x";})+","+t(()->{w.write((String)null,0,-1);return "x";})
                        +","+ty(()->{w.write((String)null);return "x";})+","+ty(()->{w.append("abc",2,1);return "x";})+","+t(()->{w.write("abc",1,2);w.flush();return hex(b.toByteArray());});
            }

            public static String subclassCalls(){
                Rec r=new Rec();Spy s=new Spy(r);
                return t(()->{s.write('a');s.write(new char[]{'b','c'});s.write("de");s.write("fgh",1,1);s.append("ij");s.append("xkl",1,3);s.append(null);s.append('m');
                    String a=s.log.toString();s.log.setLength(0);s.flush();s.close();return a+"/"+s.log+"/"+r.take();});
            }
            public static String closeFailures(){
                StringBuilder out=new StringBuilder();
                Bad a=new Bad(null,new IOException("f"),new IOException("c"));OutputStreamWriter w=new OutputStreamWriter(a);
                out.append(t(()->{w.close();return "ok";})).append(',').append(w.getEncoding()).append(',').append(t(()->{w.close();return "again";})).append(',').append(a.log);
                Bad b=new Bad(new IOException("w"),null,null);OutputStreamWriter x=new OutputStreamWriter(b);
                out.append('|').append(t(()->{x.write("ab");x.close();return "ok";})).append(',').append(x.getEncoding()).append(',').append(t(()->{x.write("z");return "x";})).append(',').append(t(()->{x.close();return "again";})).append(',').append(b.log);
                Bad c=new Bad(null,new IllegalStateException("rf"),null);OutputStreamWriter y=new OutputStreamWriter(c);
                out.append('|').append(t(()->{y.write("q");y.close();return "ok";})).append(',').append(y.getEncoding()).append(',').append(c.log);
                Bad d=new Bad(null,null,new IOException("only"));OutputStreamWriter z=new OutputStreamWriter(d);
                out.append('|').append(t(()->{z.close();return "ok";})).append(',').append(t(()->{z.flush();return "x";})).append(',').append(d.log);
                return out.toString();
            }
            public static String flushFailures(){
                Bad a=new Bad(new IOException("w"),null,null);OutputStreamWriter w=new OutputStreamWriter(a,StandardCharsets.ISO_8859_1);
                String s=t(()->{w.write("abc");w.flush();return "ok";})+","+t(()->{w.flush();return "ok";})+","+t(()->{w.close();return "ok";})+","+a.log;
                Bad b=new Bad(null,new IOException("f"),null);OutputStreamWriter x=new OutputStreamWriter(b);
                return s+"|"+t(()->{x.write("d");x.flush();return "ok";})+","+t(()->{x.flush();return "ok";})+","+x.getEncoding()+","+b.log;
            }

            public static String printWriterOverStream(){
                ByteArrayOutputStream b=new ByteArrayOutputStream();PrintWriter p=new PrintWriter(b);
                p.print("h\\u00e9");p.println(42);p.print('\\ud83d');String a=b.size()+"";p.print('\\ude00');p.flush();String c=hex(b.toByteArray());
                p.printf("%s|%d",(Object)"x",(Object)7);p.append("\\u20ac");p.close();
                return a+","+c+","+hex(b.toByteArray())+","+p.checkError();
            }
            public static String printWriterAutoFlush(){
                Rec r=new Rec();PrintWriter p=new PrintWriter(r,true);
                p.print("a");String a=r.take();p.println("b");String b=r.take();p.printf("%d",5);String c=r.take();p.write("c");p.append('d');String d=r.take();
                p.println();String e=r.take();p.close();String f=r.take();
                Rec q=new Rec();PrintWriter n=new PrintWriter(q,false);n.println("x");String g=q.take();n.printf("y");String h=q.take();n.flush();
                Rec u=new Rec();PrintWriter o=new PrintWriter(u);o.println("z");String i=u.take();o.close();
                return a+"/"+b+"/"+c+"/"+d+"/"+e+"/"+f+"|"+g+"/"+h+"/"+q.take()+"|"+i+"/"+u.take();
            }
            public static String printWriterErrors(){
                StringBuilder out=new StringBuilder();
                out.append(t(()->new PrintWriter((OutputStream)null))).append(',').append(t(()->new PrintWriter((OutputStream)null,true)));
                Bad a=new Bad(new IOException("w"),null,null);PrintWriter p=new PrintWriter(a);
                p.print("abc");out.append(',').append(p.checkError());p.close();out.append(',').append(p.checkError()).append(',').append(a.log);
                Bad b=new Bad(null,null,new IOException("c"));PrintWriter q=new PrintWriter(b,true);
                q.println("x");out.append(',').append(q.checkError());q.close();out.append(',').append(q.checkError()).append(',').append(b.log);
                return out.toString();
            }
            public static String libraryWriters(){
                Rec r=new Rec();
                return t(()->{ResponseWriter w=new ResponseWriter(r,"ISO-8859-1");w.print("\\u00e9!");String a=r.take();w.println("\\u20ac");String b=r.take();w.close();return a+"/"+b+"/"+r.take();})
                        +"|"+t(()->new ResponseWriter(new Rec(),"no-such-charset"));
            }
        }
        """;

    @Test void constructorsAndEncodingNames() throws Exception {
        matchesJvm(FIXTURE, "encodingNames", "constructorFailures", "typesAndLock");
    }

    @Test void charsetEncoding() throws Exception {
        matchesJvm(FIXTURE, "utf8Encoding", "singleByteEncoding", "utf16Encoding");
    }

    @Test void surrogatesAcrossWrites() throws Exception {
        matchesJvm(FIXTURE, "splitPairs", "unpairedAcrossWrites", "pendingHighSurrogate");
    }

    @Test void bufferingAndFlush() throws Exception {
        matchesJvm(FIXTURE, "flushPattern", "bufferFills", "bufferGrowth");
    }

    @Test void writeFormsAndCalls() throws Exception {
        matchesJvm(FIXTURE, "writeForms", "charRanges", "stringRanges", "subclassCalls");
    }

    @Test void closingAndFailures() throws Exception {
        matchesJvm(FIXTURE, "closeSequence", "closeFailures", "flushFailures");
    }

    @Test void printWriterOverOutputStream() throws Exception {
        matchesJvm(FIXTURE, "printWriterOverStream", "printWriterAutoFlush", "printWriterErrors", "libraryWriters");
    }

    @Test void leavesOutCharsetPrintWriterConstructor() {
        rejectsUnusedCall("java/io/PrintWriter", "<init>", "(Ljava/io/OutputStream;ZLjava/nio/charset/Charset;)V", INVOKESPECIAL, false);
    }
}
