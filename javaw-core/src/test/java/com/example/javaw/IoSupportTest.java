package com.example.javaw;

import org.junit.jupiter.api.Test;

class IoSupportTest {
    @Test void byteStreamsExecuteWithoutHostIo() throws Exception {
        byte[] wasm = JavacToWasm.compile("Streams.java", """
            import java.io.*;
            class Streams {
                public static int test() {
                    ByteArrayOutputStream out=new ByteArrayOutputStream(0);
                    OutputStream sink=out;
                    for(int i=0;i<300;i++) sink.write(i);
                    if(out.size()!=300) return -1;
                    byte[] copy=out.toByteArray(); copy[0]=12;
                    if(out.toByteArray()[0]!=0) return -2;
                    ByteArrayInputStream in=new ByteArrayInputStream(out.toByteArray(),254,100);
                    InputStream source=in;
                    if(source.available()!=46 || source.read()!=254 || source.read()!=255) return -3;
                    source.mark(10); if(source.skip(1000L)!=44L || source.read()!=-1) return -4;
                    source.reset(); if(source.read()!=0) return -5;
                    byte[] bytes=new byte[10]; if(source.read(bytes,2,4)!=4 || bytes[2]!=1) return -6;
                    source.close(); if(source.read()!=5) return -7;
                    ByteArrayOutputStream other=new ByteArrayOutputStream(); out.writeTo(other);
                    out.reset(); if(out.size()!=0 || other.size()!=300) return -8;
                    sink.write(new byte[]{-1,2,3},1,2); if(out.toByteArray()[0]!=2) return -9;
                    try { source.read(bytes,-1,2); return -10; } catch(IndexOutOfBoundsException expected) {}
                    try { new ByteArrayOutputStream(-1); return -11; } catch(IllegalArgumentException expected) {}
                    try { source.read((byte[])null); return -12; } catch(NullPointerException expected) {}
                    try { sink.write((byte[])null); return -13; } catch(NullPointerException expected) {}
                    try { new ByteArrayInputStream(null); return -14; } catch(NullPointerException expected) {}
                    return 42;
                }
            }
            """, false).getWasm();
        OverloadResolutionTest.execute(wasm,"assert.equal(e.test(),42);");
    }

    @Test void bufferedEofPreservesMark() throws Exception {
        byte[] wasm = JavacToWasm.compile("Eof.java", """
            import java.io.*;
            class Eof {
                static BufferedReader reader;
                public static void init() { reader = new BufferedReader(new StringReader("abc"), 2); }
                public static String line() { return reader.readLine(); }
                public static void mark() { reader.mark(20); }
                public static void reset() { reader.reset(); }
                public static int read() { return reader.read(); }
            }
            """, false).getWasm();
        OverloadResolutionTest.execute(wasm, "e.init(); e.mark(); assert.notEqual(e.line(),0); assert.equal(e.line(),0); e.reset(); assert.equal(e.read(),97);");
    }

    @Test void characterStreamBoundariesExecuteInWasm() throws Exception {
        byte[] wasm = JavacToWasm.compile("Boundaries.java", """
            import java.io.*;
            class Boundaries {
                public static int test() throws Exception {
                    StringReader s = new StringReader("abcd");
                    if (s.skip(3L) != 3L || s.skip(-2L) != -2L || s.read() != 'b') return -1;
                    if (s.skip(-100L) != -2L || s.read() != 'a') return -2;
                    char[] chars = new char[3];
                    try { s.read(chars, 2, 2); return -3; } catch (IndexOutOfBoundsException expected) {}
                    try { s.read((char[])null); return -4; } catch (NullPointerException expected) {}
                    StringWriter w = new StringWriter();
                    Writer writer = w;
                    writer.append((CharSequence)null); writer.append("abcdef", 1, 3); writer.append('!');
                    if (!w.toString().equals("nullbc!")) return -5;
                    try { writer.write(chars, -1, 1); return -6; } catch (IndexOutOfBoundsException expected) {}
                    CharArrayWriter array = new CharArrayWriter(0);
                    for (int i=0;i<300;i++) array.write('x');
                    char[] copy=array.toCharArray(); copy[0]='z';
                    if (array.size()!=300 || array.toCharArray()[0]!='x') return -7;
                    array.reset(); array.close(); array.write('q');
                    if (!array.toString().equals("q")) return -8;
                    BufferedReader lines = new BufferedReader(new StringReader("a".repeat(200)+"\\r\\nb\\rc\\n"), 1);
                    if (lines.readLine().length()!=200 || !lines.readLine().equals("b") || !lines.readLine().equals("c")) return -9;
                    if (lines.readLine()!=null || lines.readLine()!=null) return -10;
                    lines.close(); lines.close();
                    try { lines.read(); return -11; } catch (IOException expected) {}
                    BufferedReader marked = new BufferedReader(new StringReader("abcd"), 1);
                    marked.mark(1); marked.read(); marked.read();
                    try { marked.reset(); return -12; } catch (IOException expected) {}
                    try { marked.skip(-1L); return -13; } catch (IllegalArgumentException expected) {}
                    StringWriter sink = new StringWriter();
                    BufferedWriter buffered = new BufferedWriter(sink, 2);
                    buffered.write('a');
                    if (!sink.toString().equals("")) return -14;
                    buffered.close(); buffered.close();
                    if (!sink.toString().equals("a")) return -15;
                    try { buffered.write('b'); return -16; } catch (IOException expected) {}
                    return 42;
                }
            }
            """, false).getWasm();
        OverloadResolutionTest.execute(wasm, "assert.equal(e.test(),42);");
    }

    @Test void bufferedCloseReleasesSinkAfterWriteFailure() throws Exception {
        byte[] wasm = JavacToWasm.compile("Cleanup.java", """
            import java.io.*;
            class FailingWriter extends Writer {
                boolean closed;
                public void write(char[] c, int offset, int length) throws IOException { throw new IOException("write failed"); }
                public void flush() {}
                public void close() { closed=true; }
            }
            class Cleanup {
                public static int test() {
                    FailingWriter sink=new FailingWriter();
                    BufferedWriter writer=new BufferedWriter(sink,8);
                    writer.write('x');
                    try { writer.close(); return -1; } catch(IOException expected) {}
                    if(!sink.closed) return -2;
                    writer.close();
                    try { writer.flush(); return -3; } catch(IOException expected) {}
                    return 42;
                }
            }
            """, false).getWasm();
        OverloadResolutionTest.execute(wasm,"assert.equal(e.test(),42);");
    }

    @Test void characterStreamsExecuteWithoutHostIo() throws Exception {
        byte[] wasm = JavacToWasm.compile("CharStreams.java", """
            import java.io.*;
            class CharStreams {
                public static int test() throws Exception {
                    // 1. StringWriter & StringReader
                    StringWriter sw = new StringWriter(4);
                    Writer w = sw;
                    w.write("Hello");
                    w.write(' ');
                    w.write("world!extra", 0, 6);
                    sw.append(" 123");
                    if (!sw.toString().equals("Hello world! 123")) return -1;
                    sw.close(); sw.write('!');
                    if (!sw.toString().equals("Hello world! 123!")) return -2;

                    StringReader sr = new StringReader(sw.toString());
                    Reader r = sr;
                    if (!r.ready()) return -3;
                    if (!r.markSupported()) return -4;
                    if (r.read() != 'H') return -5;
                    r.mark(10);
                    char[] cbuf = new char[5];
                    if (r.read(cbuf, 0, 4) != 4) return -6;
                    if (cbuf[0] != 'e' || cbuf[1] != 'l' || cbuf[2] != 'l' || cbuf[3] != 'o') return -7;
                    if (r.skip(1L) != 1L) return -8;
                    if (r.read() != 'w') return -9;
                    r.reset();
                    if (r.read() != 'e') return -10;
                    r.close();
                    try { r.read(); return -11; } catch (IOException expected) {}

                    // 2. CharArrayWriter & CharArrayReader
                    CharArrayWriter caw = new CharArrayWriter(2);
                    caw.write("AlphaBeta", 0, 5);
                    caw.write(' ');
                    caw.write(new char[]{'G','a','m','m','a'}, 0, 5);
                    if (caw.size() != 11) return -12;
                    if (!caw.toString().equals("Alpha Gamma")) return -13;
                    char[] chars = caw.toCharArray();
                    if (chars.length != 11 || chars[0] != 'A' || chars[6] != 'G') return -14;

                    StringWriter copyTarget = new StringWriter();
                    caw.writeTo(copyTarget);
                    if (!copyTarget.toString().equals("Alpha Gamma")) return -15;

                    CharArrayReader car = new CharArrayReader(chars, 6, 5);
                    if (car.read() != 'G' || car.read() != 'a') return -16;
                    car.mark(10);
                    if (car.skip(2L) != 2L) return -17;
                    if (car.read() != 'a') return -18;
                    if (car.read() != -1) return -19;
                    car.reset();
                    if (car.read() != 'm') return -20;
                    car.close();

                    // 3. BufferedReader & BufferedWriter with line reading
                    String text = "Line 1\\nLine 2\\r\\nLine 3";
                    StringReader textReader = new StringReader(text);
                    BufferedReader br = new BufferedReader(textReader, 8);
                    if (!br.markSupported()) return -21;
                    String l1 = br.readLine();
                    if (!l1.equals("Line 1")) return -22;
                    br.mark(20);
                    String l2 = br.readLine();
                    if (!l2.equals("Line 2")) return -23;
                    br.reset();
                    l2 = br.readLine();
                    if (!l2.equals("Line 2")) return -24;
                    String l3 = br.readLine();
                    if (!l3.equals("Line 3")) return -25;
                    String l4 = br.readLine();
                    if (l4 != null) return -26;
                    br.close();

                    StringWriter sink = new StringWriter();
                    BufferedWriter bw = new BufferedWriter(sink, 8);
                    bw.write("First");
                    bw.newLine();
                    bw.write("Second");
                    bw.flush();
                    if (!sink.toString().equals("First\\nSecond")) return -27;
                    bw.close();

                    // 4. Closeable / Flushable / AutoCloseable polymorphism & Exceptions
                    Closeable closeable = new StringReader("test");
                    closeable.close();
                    Flushable flushable = new StringWriter();
                    flushable.flush();

                    try { new StringWriter(-1); return -28; } catch (IllegalArgumentException expected) {}
                    try { new StringReader(null); return -29; } catch (NullPointerException expected) {}
                    try { new CharArrayWriter(-1); return -30; } catch (IllegalArgumentException expected) {}
                    try { new CharArrayReader(null); return -31; } catch (NullPointerException expected) {}
                    try { new BufferedReader(null); return -32; } catch (NullPointerException expected) {}
                    try { new BufferedWriter(null); return -33; } catch (NullPointerException expected) {}

                    return 42;
                }
            }
            """, false).getWasm();
        OverloadResolutionTest.execute(wasm,"assert.equal(e.test(),42);");
    }
}
