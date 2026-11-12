package one.dastec.javaw.bytecode;

import one.dastec.javaw.Wat2Wasm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.tools.ToolProvider;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

import static org.junit.jupiter.api.Assertions.*;

class BytecodeJacksonNumberOutputTest {
    @TempDir Path directory;
    private String release="17";
    private String fixtureClassName="Metadata";
    private String fixtureClasspath;
    private Path artifact() throws Exception {
        Path path=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()))
            .resolve("com/fasterxml/jackson/core/jackson-core/2.15.4/jackson-core-2.15.4.jar");
        assertEquals("8dc9210dd285db366f45f518dd1e6a9ccfeb0f1a8e184a899fe96d29edf1fd94",java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))));
        return path;
    }
    private void selectedCarriers() throws Exception {
        try(JarFile jar=new JarFile(artifact().toFile())) {
            for(String owner:List.of("com/fasterxml/jackson/core/io/NumberOutput",
                    "com/fasterxml/jackson/core/io/schubfach/DoubleToDecimal",
                    "com/fasterxml/jackson/core/io/schubfach/FloatToDecimal",
                    "com/fasterxml/jackson/core/io/schubfach/MathUtils")) {
                Path file=directory.resolve(owner+".class");
                Files.createDirectories(file.getParent());
                Files.write(file,jar.getInputStream(jar.getJarEntry(owner+".class")).readAllBytes());
            }
        }
    }

    private byte[] fixture(String source) throws Exception {
        fixtureClasspath=artifact().toString();
        Path file=directory.resolve("Metadata.java");Files.writeString(file,source);
        List<String> options=new ArrayList<>(List.of("-g","-parameters","--release",release,"-d",directory.toString()));
        if(fixtureClasspath!=null)options.addAll(List.of("-classpath",fixtureClasspath));
        options.add(file.toString());
        assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,options.toArray(String[]::new)));
        return jar();
    }

    private byte[] jar() throws Exception {
        List<Path> files;
        try(var paths=Files.walk(directory)) {
            files=new ArrayList<>(paths.filter(path->path.toString().endsWith(".class")).sorted().toList());
        }
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(JarOutputStream jar=new JarOutputStream(bytes)) {
            for(Path file:files){jar.putNextEntry(new JarEntry(directory.relativize(file).toString()));jar.write(Files.readAllBytes(file));jar.closeEntry();}
        }
        return bytes.toByteArray();
    }

    // NumberOutput differential cases.

    @Test void finiteFloat() throws Exception {
        fixture("""
            import com.fasterxml.jackson.core.io.NumberOutput;
            public class Metadata {public static int run(){
                int hash=1;for(int bits:new int[]{0,Integer.MIN_VALUE,1,-1,0x7f7fffff,0x7f800000,0xff800000,0x7f800001,0xffc00001}){float value=Float.intBitsToFloat(bits);hash=31*hash+(NumberOutput.notFinite(value)?1:0);}
        int bits=17;for(int i=0;i<256;i++){bits=bits*1664525+1013904223;hash=31*hash+(NumberOutput.notFinite(Float.intBitsToFloat(bits))?1:0);}return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void finiteDouble() throws Exception {
        fixture("""
            import com.fasterxml.jackson.core.io.NumberOutput;
            public class Metadata {public static int run(){
                int hash=1;for(long bits:new long[]{0,Long.MIN_VALUE,1,-1,0x7fefffffffffffffL,0x7ff0000000000000L,0xfff0000000000000L,0x7ff0000000000001L,0xfff8000000000001L}){double value=Double.longBitsToDouble(bits);hash=31*hash+(NumberOutput.notFinite(value)?1:0);}
        long bits=17;for(int i=0;i<256;i++){bits=bits*1664525+1013904223;hash=31*hash+(NumberOutput.notFinite(Double.longBitsToDouble(bits))?1:0);}return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void floatStrings() throws Exception {
        fixture("""
            import com.fasterxml.jackson.core.io.NumberOutput;
            public class Metadata {public static int run(){
                int hash=1;for(int bits:new int[]{0,Integer.MIN_VALUE,1,-1,0x007fffff,0x00800000,0x7f7fffff,0x7f800000,0xff800000,0x7fc00001,0x3f800000,0x3dcccccd}){float value=Float.intBitsToFloat(bits);
        hash=31*hash+NumberOutput.toString(value).hashCode();
        hash=31*hash+NumberOutput.toString(value,false).hashCode();
        hash=31*hash+NumberOutput.toString(value,true).hashCode();}return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void doubleStrings() throws Exception {
        fixture("""
            import com.fasterxml.jackson.core.io.NumberOutput;
            public class Metadata {public static int run(){
                int hash=1;for(long bits:new long[]{0,Long.MIN_VALUE,1,-1,0x000fffffffffffffL,0x0010000000000000L,0x7fefffffffffffffL,0x7ff0000000000000L,0xfff0000000000000L,0x7ff8000000000001L,0x3ff0000000000000L,0x3fb999999999999aL}){double value=Double.longBitsToDouble(bits);
        hash=31*hash+NumberOutput.toString(value).hashCode();
        hash=31*hash+NumberOutput.toString(value,false).hashCode();
        hash=31*hash+NumberOutput.toString(value,true).hashCode();}return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void longStrings() throws Exception {
        fixture("""
            import com.fasterxml.jackson.core.io.NumberOutput;
            public class Metadata {public static int run(){
                int hash=1;for(long value:new long[]{0,1,-1,9,-9,10,-10,99,100,999,1000,999999,1000000,999999999,1000000000,Integer.MAX_VALUE,Integer.MIN_VALUE,2147483648L,-2147483649L,999999999999999999L,1000000000000000000L,Long.MAX_VALUE,Long.MIN_VALUE})hash=31*hash+NumberOutput.toString(value).hashCode();
        for(int value=-12;value<=12;value++)hash=31*hash+NumberOutput.toString((long)value).hashCode();return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void intStrings() throws Exception {
        fixture("""
            import com.fasterxml.jackson.core.io.NumberOutput;
            public class Metadata {public static int run(){
                int hash=1;for(int value:new int[]{0,1,-1,9,-9,10,-10,99,100,999,1000,999999,1000000,999999999,1000000000,Integer.MAX_VALUE,Integer.MIN_VALUE})hash=31*hash+NumberOutput.toString(value).hashCode();
        for(int value=-12;value<=12;value++)hash=31*hash+NumberOutput.toString((int)value).hashCode();return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void longBytes() throws Exception {
        fixture("""
            import com.fasterxml.jackson.core.io.NumberOutput;
            public class Metadata {public static int run(){
                int hash=1;
        for(long value:new long[]{0,1,-1,9,-9,10,-10,99,100,999,1000,999999,1000000,999999999,1000000000,Integer.MAX_VALUE,Integer.MIN_VALUE,2147483648L,-2147483649L,999999999999999999L,1000000000000000000L,Long.MAX_VALUE,Long.MIN_VALUE})for(int offset:new int[]{0,3}){
            byte[] buffer=new byte[32];java.util.Arrays.fill(buffer,(byte)'#');
            int end=NumberOutput.outputLong(value,buffer,offset);
            hash=31*hash+end;
            for(int i=0;i<buffer.length;i++)hash=31*hash+buffer[i];
            if(end-offset!=String.valueOf(value).length())throw new Error();
        }
        try{NumberOutput.outputLong(1,(byte[])null,0);throw new Error();}catch(NullPointerException expected){hash++;}
        for(int offset:new int[]{-1,0,Integer.MAX_VALUE})try{NumberOutput.outputLong(Long.MIN_VALUE,new byte[1],offset);throw new Error();}catch(IndexOutOfBoundsException expected){hash++;}
        return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void longChars() throws Exception {
        fixture("""
            import com.fasterxml.jackson.core.io.NumberOutput;
            public class Metadata {public static int run(){
                int hash=1;
        for(long value:new long[]{0,1,-1,9,-9,10,-10,99,100,999,1000,999999,1000000,999999999,1000000000,Integer.MAX_VALUE,Integer.MIN_VALUE,2147483648L,-2147483649L,999999999999999999L,1000000000000000000L,Long.MAX_VALUE,Long.MIN_VALUE})for(int offset:new int[]{0,3}){
            char[] buffer=new char[32];java.util.Arrays.fill(buffer,(char)'#');
            int end=NumberOutput.outputLong(value,buffer,offset);
            hash=31*hash+end;
            for(int i=0;i<buffer.length;i++)hash=31*hash+buffer[i];
            if(end-offset!=String.valueOf(value).length())throw new Error();
        }
        try{NumberOutput.outputLong(1,(char[])null,0);throw new Error();}catch(NullPointerException expected){hash++;}
        for(int offset:new int[]{-1,0,Integer.MAX_VALUE})try{NumberOutput.outputLong(Long.MIN_VALUE,new char[1],offset);throw new Error();}catch(IndexOutOfBoundsException expected){hash++;}
        return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void intBytes() throws Exception {
        fixture("""
            import com.fasterxml.jackson.core.io.NumberOutput;
            public class Metadata {public static int run(){
                int hash=1;
        for(int value:new int[]{0,1,-1,9,-9,10,-10,99,100,999,1000,999999,1000000,999999999,1000000000,Integer.MAX_VALUE,Integer.MIN_VALUE})for(int offset:new int[]{0,3}){
            byte[] buffer=new byte[32];java.util.Arrays.fill(buffer,(byte)'#');
            int end=NumberOutput.outputInt(value,buffer,offset);
            hash=31*hash+end;
            for(int i=0;i<buffer.length;i++)hash=31*hash+buffer[i];
            if(end-offset!=String.valueOf(value).length())throw new Error();
        }
        try{NumberOutput.outputInt(1,(byte[])null,0);throw new Error();}catch(NullPointerException expected){hash++;}
        for(int offset:new int[]{-1,0,Integer.MAX_VALUE})try{NumberOutput.outputInt(Integer.MIN_VALUE,new byte[1],offset);throw new Error();}catch(IndexOutOfBoundsException expected){hash++;}
        return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void intChars() throws Exception {
        fixture("""
            import com.fasterxml.jackson.core.io.NumberOutput;
            public class Metadata {public static int run(){
                int hash=1;
        for(int value:new int[]{0,1,-1,9,-9,10,-10,99,100,999,1000,999999,1000000,999999999,1000000000,Integer.MAX_VALUE,Integer.MIN_VALUE})for(int offset:new int[]{0,3}){
            char[] buffer=new char[32];java.util.Arrays.fill(buffer,(char)'#');
            int end=NumberOutput.outputInt(value,buffer,offset);
            hash=31*hash+end;
            for(int i=0;i<buffer.length;i++)hash=31*hash+buffer[i];
            if(end-offset!=String.valueOf(value).length())throw new Error();
        }
        try{NumberOutput.outputInt(1,(char[])null,0);throw new Error();}catch(NullPointerException expected){hash++;}
        for(int offset:new int[]{-1,0,Integer.MAX_VALUE})try{NumberOutput.outputInt(Integer.MIN_VALUE,new char[1],offset);throw new Error();}catch(IndexOutOfBoundsException expected){hash++;}
        return hash;
            }}
            """);compare(List.of("run"));
    }

    private void compare(List<String> names) throws Exception {
        StringBuilder assertions=new StringBuilder();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL(),artifact().toUri().toURL()},null)){
            Class<?> type=loader.loadClass(fixtureClassName);
            for(String name:names){Object expected=type.getMethod(name).invoke(null);assertInstanceOf(Integer.class,expected);
                assertions.append("assert.equal(e['").append(fixtureClassName).append(".").append(name).append("'](),").append(expected).append(");\n");}
        }
        selectedCarriers();
        execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar())),assertions.toString());
    }

    private void execute(String wat,String assertions) throws Exception {
        Path input=directory.resolve("metadata.wat"),output=directory.resolve("metadata.wasm"),script=directory.resolve("run.cjs");
        Files.writeString(input,wat);WasmTestSupport.assemble(input,output,Wat2Wasm.EXCEPTIONS);
        Files.writeString(script,"""
            const fs=require('node:fs'),assert=require('node:assert/strict');
            const compiled=new WebAssembly.Module(fs.readFileSync(process.argv[2]));
            for(let instance=0;instance<2;instance++) {
                function fresh(){
                    const memory=new WebAssembly.Memory({initial:2});let heap=1024,allocations=0;
                    const e=new WebAssembly.Instance(compiled,{env:{memory,__heap_alloc:size=>{
                        allocations++;const address=heap;heap+=size;
                        while(heap>memory.buffer.byteLength)memory.grow(1);
                        new Uint8Array(memory.buffer,address,size).fill(0x7f);return address;
                    }}}).exports;
                    assert.equal(allocations,0);assert.equal(e.__gc_heap_bytes?.()??0,0);if(e.__gc_scribble)e.__gc_scribble.value=1;heap=Math.max(heap,e.__exception_heap_base?.value??0);return e;
                }
            """+assertions.replace("e['","fresh()['")+"\n}");
        WasmTestSupport.node(script,output);
    }
}
