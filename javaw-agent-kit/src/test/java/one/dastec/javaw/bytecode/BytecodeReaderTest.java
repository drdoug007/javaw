package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T20: java.io.Reader and java.io.StringReader. Expected values come from the JVM; see tasks/T20-reader.md. */
class BytecodeReaderTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.io.Closeable;
        import java.io.IOException;
        import java.io.Reader;
        import java.io.StringReader;
        import java.io.Writer;

        /** A Reader that returns at most max chars per call and records the len of every read(char[],int,int) call. */
        class Chunky extends Reader {
            final String text; final int max; int pos; final StringBuilder calls=new StringBuilder();
            Chunky(String text,int max){this.text=text;this.max=max;}
            Chunky(Object lock,String text,int max){super(lock);this.text=text;this.max=max;}
            public int read(char[] b,int off,int len){
                calls.append(len).append(' ');
                if(len==0)return 0;
                if(pos>=text.length())return -1;
                int n=Math.min(Math.min(len,max),text.length()-pos);
                for(int i=0;i<n;i++)b[off+i]=text.charAt(pos++);
                return n;
            }
            public void close(){calls.append("close ");}
            boolean ownLock(){return lock==this;}
            Object lockObject(){return lock;}
        }

        /** A Writer that collects text and records the length of every write(char[],int,int) call. */
        class Sink extends Writer {
            final StringBuilder text=new StringBuilder(),writes=new StringBuilder();
            public void write(char[] b,int off,int len){writes.append(len).append(' ');for(int i=0;i<len;i++)text.append(b[off+i]);}
            public void flush(){}
            public void close(){}
        }

        public class Fixture {
            interface Body { Object run() throws Exception; }
            static String io(Body body){
                try{return String.valueOf(body.run());}
                catch(Exception e){
                    // HotSpot's helpful NPE text ("Cannot read the array length ...") is not part of the contract: print it as null.
                    String m=e.getMessage();
                    if(e instanceof NullPointerException && m!=null && m.startsWith("Cannot "))m=null;
                    return e.getClass().getSimpleName()+":"+m;
                }
            }

            // ---- StringReader reading
            public static String stringReadSingle(){
                StringReader r=new StringReader("ab\\uffff");
                return io(()->r.read()+","+r.read()+","+r.read()+","+r.read()+","+r.read());
            }
            public static String stringReadArrays(){
                StringReader r=new StringReader("hello world");
                char[] b=new char[4];
                return io(()->{
                    StringBuilder out=new StringBuilder();
                    int n=r.read(b);out.append(n).append(new String(b,0,n)).append('|');
                    n=r.read(b,1,2);out.append(n).append(new String(b)).append('|');
                    char[] big=new char[20];n=r.read(big,3,20-3);out.append(n).append(new String(big,3,n)).append('|');
                    out.append(r.read(big)).append(',').append(r.read(big,0,0)).append(',').append(r.read());
                    return out.toString();
                });
            }
            public static String stringReadEmpty(){
                StringReader r=new StringReader("");
                return io(()->r.read()+","+r.read(new char[3])+","+r.read(new char[3],1,0)+","+r.ready());
            }
            public static String stringReadBounds(){
                StringReader r=new StringReader("abc");
                char[] b=new char[4];
                return io(()->r.read(b,2,3))+";"+io(()->r.read(b,-1,1))+";"+io(()->r.read(b,0,-1))+";"+io(()->r.read(b,5,0))
                        +";"+io(()->r.read(null,0,0))+";"+io(()->r.read((char[])null))+";"+io(()->r.read(b,4,0))+";"+io(()->r.read());
            }
            public static String stringNullSource(){
                return io(()->{new StringReader(null);return "made";});
            }

            // ---- StringReader skip, ready, mark and reset
            public static String stringSkip(){
                StringReader r=new StringReader("0123456789");
                return io(()->r.skip(3)+","+(char)r.read()+","+r.skip(0)+","+r.skip(-2)+","+(char)r.read()+","+r.skip(-100)+","+(char)r.read()
                        +","+r.skip(100)+","+r.read()+","+r.skip(5)+","+r.skip(-5)+","+r.read());
            }
            public static String stringSkipExtremes(){
                StringReader r=new StringReader("abcdef");
                return io(()->r.skip(Long.MAX_VALUE)+","+r.skip(Long.MIN_VALUE)+","+r.ready());
            }
            public static String stringSkipLongValues(){
                StringReader r=new StringReader("abcdef");
                return io(()->r.skip(2)+","+r.skip(Long.MIN_VALUE)+","+(char)r.read()+","+r.skip(4294967298L)+","+r.read());
            }
            public static String stringMarkReset(){
                StringReader r=new StringReader("abcdef");
                return io(()->{
                    StringBuilder out=new StringBuilder().append(r.markSupported()).append(r.ready()).append(',');
                    r.reset();out.append((char)r.read());
                    r.skip(1);r.mark(0);out.append((char)r.read()).append((char)r.read());
                    r.reset();out.append((char)r.read());
                    r.mark(1);r.skip(10);r.reset();out.append((char)r.read());
                    r.skip(100);r.reset();r.reset();out.append((char)r.read());
                    return out.toString();
                });
            }
            public static String stringMarkNegative(){
                StringReader r=new StringReader("abc");
                return io(()->{r.mark(-1);return "none";})+";"+io(()->(char)r.read());
            }

            // ---- StringReader after close
            public static String stringClosed(){
                StringReader r=new StringReader("abc");
                r.close();
                char[] b=new char[2];
                return io(()->r.read())+";"+io(()->r.read(b))+";"+io(()->r.read(b,0,0))+";"+io(()->r.read(b,5,9))+";"+io(()->r.skip(1))
                        +";"+io(()->r.skip(-1))+";"+io(()->r.ready())+";"+io(()->{r.mark(1);return "marked";})+";"+io(()->{r.reset();return "reset";})
                        +";"+r.markSupported();
            }
            public static String stringClosedChecksArgumentsLate(){
                StringReader r=new StringReader("abc");
                r.close();r.close();
                return io(()->r.read(null,0,1))+";"+io(()->{r.mark(-1);return "none";})+";"+io(()->r.transferTo(new Sink()))+";"+io(()->r.transferTo(null));
            }
            public static String tryWithResources(){
                StringReader[] keep=new StringReader[1];
                String first=io(()->{try(StringReader r=new StringReader("xyz")){keep[0]=r;return (char)r.read();}});
                Reader asReader=keep[0];Closeable c=keep[0];
                return first+";"+io(()->keep[0].read())+";"+(asReader instanceof Closeable)+(c instanceof AutoCloseable);
            }

            // ---- Reader base-class defaults (exercised through a subclass that only implements read(char[],int,int))
            public static String baseRead(){
                Chunky r=new Chunky("a\\uffffbcdef",2);
                char[] b=new char[5];
                return io(()->{
                    StringBuilder out=new StringBuilder();
                    out.append(r.read()).append(',').append(r.read()).append(',');
                    int n=r.read(b);out.append(n).append(new String(b,0,n)).append(',');
                    out.append(r.read(b)).append(r.read(b)).append(r.read()).append(',').append(r.read(new char[0]));
                    return out+"|"+r.calls;
                });
            }
            public static String baseReadNullArray(){
                Chunky r=new Chunky("abc",2);
                return io(()->r.read((char[])null))+"|"+r.calls;
            }
            public static String baseSkipChunks(){
                Chunky small=new Chunky("abcdefghij",3);
                Chunky rest=new Chunky("abcdefghij",3);
                return io(()->small.skip(8)+","+(char)small.read()+"|"+small.calls)+";"+io(()->rest.skip(100)+"|"+rest.calls);
            }
            public static String baseSkipLarge(){
                Chunky r=new Chunky("x".repeat(9000),10000);
                return io(()->r.skip(20000)+"|"+r.calls);
            }
            public static String baseSkipZeroAndNegative(){
                Chunky r=new Chunky("abc",2);
                return io(()->r.skip(0))+";"+io(()->r.skip(-1))+";"+io(()->r.skip(Long.MIN_VALUE))+"|"+r.calls;
            }
            public static String baseDefaults(){
                Chunky r=new Chunky("abc",2);
                return io(()->r.ready())+";"+r.markSupported()+";"+io(()->{r.mark(10);return "marked";})+";"+io(()->{r.mark(-1);return "marked";})
                        +";"+io(()->{r.reset();return "reset";})+"|"+r.calls;
            }
            public static String lockField(){
                Object shared=new Object();
                Chunky own=new Chunky("a",1),locked=new Chunky(shared,"b",1);
                String nullLock=io(()->{new Chunky(null,"c",1);return "made";});
                return own.ownLock()+","+(own.lockObject()==own)+","+locked.ownLock()+","+(locked.lockObject()==shared)+","+nullLock;
            }

            // ---- transferTo
            public static String transferString(){
                StringReader r=new StringReader("hello, world");
                Sink s=new Sink();
                return io(()->r.transferTo(s)+"|"+s.text+"|"+s.writes+"|"+r.read()+"|"+r.transferTo(s)+"|"+s.writes);
            }
            public static String transferLong(){
                StringBuilder text=new StringBuilder();
                for(int i=0;i<10000;i++)text.append((char)('a'+i%26));
                StringReader r=new StringReader(text.toString());
                Sink s=new Sink();
                return io(()->r.transferTo(s)+"|"+s.writes+"|"+s.text.toString().equals(text.toString()));
            }
            public static String transferChunks(){
                Chunky r=new Chunky("hello",2);
                Sink s=new Sink();
                return io(()->r.transferTo(s)+"|"+s.text+"|"+s.writes+"|"+r.calls);
            }
            public static String transferPartlyRead(){
                StringReader r=new StringReader("abcdef");
                Sink s=new Sink();
                return io(()->{r.skip(4);return r.transferTo(s)+"|"+s.text;});
            }
            public static String transferNull(){
                Chunky r=new Chunky("abc",2);
                return io(()->r.transferTo(null))+";"+io(()->new StringReader("x").transferTo(null))+"|"+r.calls;
            }

            // ---- Reader.nullReader()
            public static String nullReaderOpen(){
                Reader r=Reader.nullReader();
                char[] b=new char[2];
                return io(()->r.read()+","+r.read(b)+","+r.read(b,0,0)+","+r.read(b,1,1)+","+r.ready()+","+r.skip(5)+","+r.skip(-5)
                        +","+r.transferTo(new Sink())+","+r.markSupported());
            }
            public static String nullReaderFailures(){
                Reader r=Reader.nullReader();
                char[] b=new char[2];
                return io(()->r.read(b,1,2))+";"+io(()->r.read(null,0,0))+";"+io(()->r.transferTo(null))+";"+io(()->{r.mark(1);return "marked";})
                        +";"+io(()->{r.reset();return "reset";});
            }
            public static String nullReaderClosed(){
                Reader r=Reader.nullReader();
                char[] b=new char[2];
                return io(()->{r.close();r.close();return "closed";})+";"+io(()->r.read())+";"+io(()->r.read(b))+";"+io(()->r.read(b,0,0))
                        +";"+io(()->r.read(b,3,0))+";"+io(()->r.ready())+";"+io(()->r.skip(1))+";"+io(()->r.transferTo(new Sink()))
                        +";"+io(()->r.transferTo(null))+";"+(Reader.nullReader()!=r);
            }
        }
        """;

    @Test void stringReaderReads() throws Exception {
        matchesJvm(FIXTURE, "stringReadSingle", "stringReadArrays", "stringReadEmpty", "stringReadBounds", "stringNullSource");
    }

    @Test void stringReaderSkipsMarksAndResets() throws Exception {
        matchesJvm(FIXTURE, "stringSkip", "stringSkipExtremes", "stringSkipLongValues", "stringMarkReset", "stringMarkNegative");
    }

    @Test void stringReaderAfterClose() throws Exception {
        matchesJvm(FIXTURE, "stringClosed", "stringClosedChecksArgumentsLate", "tryWithResources");
    }

    @Test void readerBaseDefaults() throws Exception {
        matchesJvm(FIXTURE, "baseRead", "baseReadNullArray", "baseSkipChunks", "baseSkipLarge", "baseSkipZeroAndNegative", "baseDefaults", "lockField");
    }

    @Test void transfersToWriter() throws Exception {
        matchesJvm(FIXTURE, "transferString", "transferLong", "transferChunks", "transferPartlyRead", "transferNull");
    }

    @Test void nullReader() throws Exception {
        matchesJvm(FIXTURE, "nullReaderOpen", "nullReaderFailures", "nullReaderClosed");
    }

    @Test void leavesCharBufferMembersUnlinked() {
        rejectsUnusedCall("java/io/Reader", "read", "(Ljava/nio/CharBuffer;)I", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/io/StringReader", "read", "(Ljava/nio/CharBuffer;)I", INVOKEVIRTUAL, false);
    }
}
