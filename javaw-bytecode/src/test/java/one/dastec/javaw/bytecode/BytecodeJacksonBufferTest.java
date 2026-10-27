package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.tools.ToolProvider;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.jar.*;

import static org.junit.jupiter.api.Assertions.*;

class BytecodeJacksonBufferTest {
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
            for(String owner:List.of("com/fasterxml/jackson/core/util/BufferRecycler","com/fasterxml/jackson/core/util/ByteArrayBuilder")) {
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
        return jar(false);
    }

    private byte[] jar(boolean reverse) throws Exception {
        List<Path> files;
        try(var paths=Files.walk(directory)) {
            files=new ArrayList<>(paths.filter(path->path.toString().endsWith(".class")).sorted().toList());
        }
        if(reverse)Collections.reverse(files);
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(JarOutputStream jar=new JarOutputStream(bytes)) {
            for(Path file:files){jar.putNextEntry(new JarEntry(directory.relativize(file).toString()));jar.write(Files.readAllBytes(file));jar.closeEntry();}
        }
        return bytes.toByteArray();
    }

    // BufferRecycler and ByteArrayBuilder differential cases.

    @Test void builderRecycling() throws Exception {
        fixture("""
            import com.fasterxml.jackson.core.util.*;
            public class Metadata {
                static int bytes(int hash,byte[] bytes){hash=31*hash+bytes.length;for(byte b:bytes)hash=31*hash+b;return hash;}
                public static int run(){
                    int hash=1;BufferRecycler recycler=new BufferRecycler();
                    ByteArrayBuilder builder=new ByteArrayBuilder(recycler);byte[] first=builder.getCurrentSegment();hash=31*hash+first.length;
                    for(int i=0;i<700;i++)builder.write(i);hash=bytes(hash,builder.toByteArray());
                    builder.release();byte[] reused=recycler.allocByteBuffer(BufferRecycler.BYTE_WRITE_CONCAT_BUFFER);hash=31*hash+(reused==first?1:0)+reused.length;
                    recycler.releaseByteBuffer(BufferRecycler.BYTE_WRITE_CONCAT_BUFFER,reused);
                    ByteArrayBuilder sized=new ByteArrayBuilder(recycler,10);hash=31*hash+(sized.getCurrentSegment()==reused?1:0)+sized.getCurrentSegment().length;
                    sized.write(new byte[]{1,2,3});byte[] segment=sized.resetAndGetFirstSegment();hash=31*hash+sized.size()+segment.length+(segment==reused?1:0);
                    for(int i=0;i<3000;i++)sized.append(i*7);byte[] all=sized.toByteArray();hash=bytes(hash,all);hash=31*hash+sized.size();
                    sized.release();hash=31*hash+sized.size();sized.release();
                    ByteArrayBuilder plain=new ByteArrayBuilder(64);plain.append(5);plain.release();plain.reset();hash=31*hash+plain.size()+plain.getCurrentSegment().length;
                    ByteArrayBuilder initial=ByteArrayBuilder.fromInitial(new byte[]{9,8,7,6},2);hash=31*hash+initial.size();initial.append(1);initial.append(2);initial.append(3);hash=bytes(hash,initial.toByteArray());
                    ByteArrayBuilder defaults=new ByteArrayBuilder();hash=31*hash+defaults.getCurrentSegment().length+ByteArrayBuilder.NO_BYTES.length;
                    hash=bytes(hash,defaults.toByteArray());hash=31*hash+(defaults.toByteArray()==ByteArrayBuilder.NO_BYTES?1:0);
                    java.io.OutputStream stream=defaults;try{stream.write(new byte[]{4,5},0,2);stream.flush();stream.close();}catch(java.io.IOException impossible){throw new Error(impossible);}
                    return bytes(hash,defaults.toByteArray());
                }
                public static int failures(){
                    int hash=1;ByteArrayBuilder builder=new ByteArrayBuilder(4);
                    for(int[] range:new int[][]{{-1,1},{0,-1},{3,1},{1,Integer.MAX_VALUE}})try{builder.write(new byte[3],range[0],range[1]);hash=31*hash+builder.size();}catch(RuntimeException failure){hash=31*hash+failure.getClass().getName().hashCode();}
                    try{builder.write((byte[])null);throw new Error();}catch(NullPointerException expected){hash+=3;}
                    try{new ByteArrayBuilder(-1);throw new Error();}catch(NegativeArraySizeException expected){hash+=5;}
                    try{ByteArrayBuilder.fromInitial(null,0).append(1);throw new Error();}catch(NullPointerException expected){hash+=7;}
                    ByteArrayBuilder empty=ByteArrayBuilder.fromInitial(new byte[0],0);empty.append(1);hash=31*hash+empty.size()+empty.getCurrentSegment().length;
                    return hash;
                }
            }
            """);compare(List.of("run","failures"));
    }

    @Test void builderSegments() throws Exception {
        fixture("""
            import com.fasterxml.jackson.core.util.*;
            public class Metadata {
                static int bytes(int hash,byte[] bytes){hash=31*hash+bytes.length;for(int i=0;i<bytes.length;i+=Math.max(1,bytes.length/997))hash=31*hash+bytes[i];return 31*hash+(bytes.length==0?0:bytes[bytes.length-1]);}
                public static int appends(){
                    int hash=1;ByteArrayBuilder builder=new ByteArrayBuilder(5);
                    for(int i=0;i<8000;i++){switch(i%4){case 0->builder.append(i);case 1->builder.appendTwoBytes(i*31);case 2->builder.appendThreeBytes(i*997);default->builder.appendFourBytes(i*7919);}}
                    hash=31*hash+builder.size();byte[] all=builder.toByteArray();hash=bytes(hash,all);
                    int[] values={0x12345678,-1,0x80,0x7fffff,Integer.MIN_VALUE};for(int value:values){ByteArrayBuilder small=new ByteArrayBuilder(1);small.appendFourBytes(value);small.appendThreeBytes(value);small.appendTwoBytes(value);small.append(value);hash=bytes(hash,small.toByteArray());}
                    builder.reset();hash=31*hash+builder.size();
                    return hash;
                }
                public static int large(){
                    ByteArrayBuilder builder=new ByteArrayBuilder();byte[] block=new byte[60000];for(int i=0;i<block.length;i++)block[i]=(byte)(i*13);
                    builder.write(block,0,block.length);builder.write(new byte[]{1,2,3,4,5},1,3);int hash=31+builder.size();return bytes(hash,builder.toByteArray());
                }
                public static int segments(){
                    int hash=1;ByteArrayBuilder builder=new ByteArrayBuilder(8);
                    byte[] current=builder.getCurrentSegment();for(int i=0;i<8;i++)current[i]=(byte)(i+1);builder.setCurrentSegmentLength(8);hash=31*hash+builder.getCurrentSegmentLength()+builder.size();
                    byte[] next=builder.finishCurrentSegment();hash=31*hash+next.length+builder.getCurrentSegmentLength()+builder.size();
                    for(int i=0;i<5;i++)next[i]=(byte)(100+i);byte[] coalesced=builder.completeAndCoalesce(5);hash=bytes(hash,coalesced);
                    hash=31*hash+builder.size();
                    ByteArrayBuilder exact=new ByteArrayBuilder(4);exact.write(new byte[]{1,2,3,4});hash=bytes(hash,exact.completeAndCoalesce(0));
                    ByteArrayBuilder many=new ByteArrayBuilder(2);for(int round=0;round<12;round++){byte[] segment=many.getCurrentSegment();java.util.Arrays.fill(segment,(byte)round);many.setCurrentSegmentLength(segment.length);hash=31*hash+segment.length;many.finishCurrentSegment();}
                    hash=31*hash+many.size();hash=bytes(hash,many.toByteArray());byte[] twice=many.toByteArray();hash=31*hash+(twice==many.toByteArray()?1:0);
                    many.reset();hash=31*hash+many.size()+many.getCurrentSegmentLength();
                    return hash;
                }
            }
            """);compare(List.of("appends","large","segments"));
    }

    @Test void recyclerHooks() throws Exception {
        fixture("""
            import com.fasterxml.jackson.core.util.BufferRecycler;
            public class Metadata {
                static final class Tracking extends BufferRecycler {
                    final StringBuilder log=new StringBuilder();
                    Tracking(){super(2,3);}
                    protected int byteBufferLength(int index){log.append("bl").append(index);return 10+index;}
                    protected int charBufferLength(int index){log.append("cl").append(index);return 20+index;}
                    protected byte[] balloc(int size){log.append("b").append(size);return new byte[size];}
                    protected char[] calloc(int size){log.append("c").append(size);return new char[size];}
                }
                public static String run(){
                    Tracking recycler=new Tracking();byte[] a=recycler.allocByteBuffer(1);byte[] b=recycler.allocByteBuffer(1,40);
                    char[] c=recycler.allocCharBuffer(2);char[] d=recycler.allocCharBuffer(0,5);
                    recycler.releaseByteBuffer(1,b);byte[] e=recycler.allocByteBuffer(1,30);byte[] f=recycler.allocByteBuffer(1,50);
                    recycler.releaseCharBuffer(2,c);char[] g=recycler.allocCharBuffer(2,100);
                    String text=recycler.log+"|"+a.length+","+b.length+","+c.length+","+d.length+","+(e==b)+","+f.length+","+(g==c)+","+g.length;
                    try{recycler.allocByteBuffer(2);text+="|no";}catch(IndexOutOfBoundsException expected){text+="|"+expected.getClass().getName();}
                    try{recycler.allocCharBuffer(3);text+="|no";}catch(IndexOutOfBoundsException expected){text+="|"+expected.getClass().getName();}
                    try{recycler.releaseByteBuffer(-1,a);text+="|no";}catch(IndexOutOfBoundsException expected){text+="|"+expected.getClass().getName();}
                    return text+"|"+recycler.log;
                }
            }
            """);compareText(List.of("run"));
    }

    @Test void recyclerAllocation() throws Exception {
        fixture("""
            import com.fasterxml.jackson.core.util.BufferRecycler;
            public class Metadata {public static int run(){
                int hash=1;BufferRecycler recycler=new BufferRecycler();
                hash=31*hash+BufferRecycler.BYTE_READ_IO_BUFFER+2*BufferRecycler.BYTE_WRITE_ENCODING_BUFFER+4*BufferRecycler.BYTE_WRITE_CONCAT_BUFFER+8*BufferRecycler.BYTE_BASE64_CODEC_BUFFER;
                hash=31*hash+BufferRecycler.CHAR_TOKEN_BUFFER+2*BufferRecycler.CHAR_CONCAT_BUFFER+4*BufferRecycler.CHAR_TEXT_BUFFER+8*BufferRecycler.CHAR_NAME_COPY_BUFFER;
                for(int index=0;index<4;index++){
                    byte[] bytes=recycler.allocByteBuffer(index);char[] chars=recycler.allocCharBuffer(index);hash=31*hash+bytes.length;hash=31*hash+chars.length;
                    hash=31*hash+recycler.allocByteBuffer(index,1).length+recycler.allocByteBuffer(index,20000).length;
                    hash=31*hash+recycler.allocCharBuffer(index,1).length+recycler.allocCharBuffer(index,10000).length;
                    recycler.releaseByteBuffer(index,bytes);recycler.releaseCharBuffer(index,chars);
                    hash=31*hash+(recycler.allocByteBuffer(index)==bytes?1:0)+(recycler.allocCharBuffer(index)==chars?2:0);
                    hash=31*hash+(recycler.allocByteBuffer(index)==bytes?1:0)+(recycler.allocCharBuffer(index)==chars?2:0);
                    recycler.releaseByteBuffer(index,new byte[3]);hash=31*hash+recycler.allocByteBuffer(index,2).length+recycler.allocByteBuffer(index,2).length;
                    recycler.releaseByteBuffer(index,new byte[3]);hash=31*hash+recycler.allocByteBuffer(index,4).length;
                    recycler.releaseCharBuffer(index,new char[7]);hash=31*hash+recycler.allocCharBuffer(index,8).length;
                    recycler.releaseByteBuffer(index,null);hash=31*hash+recycler.allocByteBuffer(index).length;
                }
                BufferRecycler other=new BufferRecycler();byte[] mine=other.allocByteBuffer(0);recycler.releaseByteBuffer(0,mine);hash=31*hash+(recycler.allocByteBuffer(0)==mine?1:0);
                for(int index:new int[]{-1,4,Integer.MAX_VALUE}){
                    try{recycler.allocByteBuffer(index);throw new Error();}catch(IndexOutOfBoundsException expected){hash=31*hash+expected.getClass().getName().hashCode();}
                    try{recycler.allocCharBuffer(index,1);throw new Error();}catch(IndexOutOfBoundsException expected){hash=31*hash+expected.getClass().getName().hashCode();}
                    try{recycler.releaseCharBuffer(index,new char[1]);throw new Error();}catch(IndexOutOfBoundsException expected){hash=31*hash+expected.getClass().getName().hashCode();}
                }
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
        for(boolean reverse:new boolean[]{false,true})execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),assertions.toString());
    }

    private void compareText(List<String> names) throws Exception {
        StringBuilder assertions=new StringBuilder();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL(),artifact().toUri().toURL()},null)){
            Class<?> type=loader.loadClass(fixtureClassName);
            for(String name:names){Object expected=type.getMethod(name).invoke(null);assertInstanceOf(String.class,expected);
                assertions.append("assert.equal(text(e['").append(fixtureClassName).append(".").append(name).append("']()),").append(js((String)expected)).append(");\n");}
        }
        selectedCarriers();
        for(boolean reverse:new boolean[]{false,true})execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),assertions.toString());
    }
    private String js(String value) {
        StringBuilder out=new StringBuilder("\"");
        for(char c:value.toCharArray())out.append(String.format("\\u%04x",(int)c));
        return out.append('"').toString();
    }

    private void run(String... arguments) throws Exception {
        Path log=directory.resolve("command.log");Process process=new ProcessBuilder(arguments).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if(!process.waitFor(60,TimeUnit.SECONDS)){process.destroyForcibly();fail("Command timed out");}
        assertEquals(0,process.exitValue(),Files.readString(log));
    }
    private void execute(String wat,String assertions) throws Exception {
        Path input=directory.resolve("metadata.wat"),output=directory.resolve("metadata.wasm"),script=directory.resolve("run.cjs");
        Files.writeString(input,wat);run("wat2wasm","--enable-exceptions",input.toString(),"-o",output.toString());
        Files.writeString(script,"""
            const fs=require('node:fs'),assert=require('node:assert/strict');
            const compiled=new WebAssembly.Module(fs.readFileSync(process.argv[2]));
            for(let instance=0;instance<2;instance++) {
                function fresh(){
                    const memory=new WebAssembly.Memory({initial:2,maximum:20});let heap=1024,allocations=0;
                    const e=new WebAssembly.Instance(compiled,{env:{memory,__heap_alloc:size=>{
                        allocations++;const address=heap;heap+=size;
                        while(heap>memory.buffer.byteLength)memory.grow(1);
                        new Uint8Array(memory.buffer,address,size).fill(0x7f);return address;
                    }}}).exports;
                    assert.equal(allocations,0);heap=Math.max(heap,e.__exception_heap_base?.value??0);current=memory;return e;
                }
                let current;
                function text(pointer){if(!pointer)return null;const view=new DataView(current.buffer);
                    const chars=view.getInt32(pointer+4,true),length=view.getInt32(chars+4,true);
                    let result='';for(let i=0;i<length;i++)result+=String.fromCharCode(view.getUint16(chars+8+2*i,true));return result;}
            """+assertions.replace("e['","fresh()['")+"\n}");
        run("node",script.toString(),output.toString());
    }
}
