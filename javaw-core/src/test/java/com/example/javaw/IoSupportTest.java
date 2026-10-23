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
}
