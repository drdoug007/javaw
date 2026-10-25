package com.example.javaw.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import javax.tools.ToolProvider;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.jar.*;

import static org.junit.jupiter.api.Assertions.*;

class BytecodeLegacyByteTextTest implements Opcodes {
    @TempDir Path directory;
    private String release="17";
    private String fixtureClassName="Metadata";
    private String fixtureClasspath;

    private byte[] fixture(String source) throws Exception {
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

    @Test void highByteConstructionAndSnapshots() throws Exception {
        fixture("""
            import java.util.*;
            public class Metadata {public static int run(){int hash=1;byte[] bytes=new byte[256];for(int i=0;i<256;i++)bytes[i]=(byte)i;
                for(int high:new int[]{Integer.MIN_VALUE,-65537,-256,-1,0,1,127,128,255,256,257,Integer.MAX_VALUE}){
                    hash=31*hash+new String(bytes,high).hashCode();hash=31*hash+new String(bytes,high,1,254).hashCode();
                    for(int offset:new int[]{Integer.MIN_VALUE,-1,0,128,256,257,Integer.MAX_VALUE})for(int count:new int[]{Integer.MIN_VALUE,-1,0,1,128,256,Integer.MAX_VALUE}){
                        try{hash=31*hash+new String(bytes,high,offset,count).hashCode();}catch(StringIndexOutOfBoundsException e){hash++;}
                    }
                }
                String snapshot=new String(bytes,0);bytes[65]=0;if(snapshot.charAt(65)!='A')throw new Error();hash=31*hash+snapshot.hashCode();
                try{new String((byte[])null,0);throw new Error();}catch(NullPointerException e){hash++;}
                for(int offset:new int[]{-1,0,Integer.MAX_VALUE})for(int count:new int[]{-1,0,Integer.MAX_VALUE}){
                    try{new String((byte[])null,0,offset,count);throw new Error();}catch(NullPointerException e){hash++;}
                }
                return hash;
            }}
            """);compare(List.of("run"));
    }
    private void jackson() throws Exception {
        Path repository=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()));
        Path artifact=repository.resolve("com/fasterxml/jackson/core/jackson-core/2.15.4/jackson-core-2.15.4.jar");
        assertDigest(artifact,"8dc9210dd285db366f45f518dd1e6a9ccfeb0f1a8e184a899fe96d29edf1fd94");
        try(JarFile jar=new JarFile(artifact.toFile())){for(String name:List.of("DoubleToDecimal","FloatToDecimal","MathUtils")){
            String path="com/fasterxml/jackson/core/io/schubfach/"+name;writeClass(path,jar.getInputStream(jar.getJarEntry(path+".class")).readAllBytes());}}
        fixtureClasspath=directory.toString();
    }
    @Test void executesUnchangedPinnedJacksonDoubleFormatting() throws Exception {
        jackson();fixture("""
            import com.fasterxml.jackson.core.io.schubfach.DoubleToDecimal;
            public class Metadata {public static int run(){int hash=1;
                long[] cases={0,Long.MIN_VALUE,1,-1,0x000fffffffffffffL,0x0010000000000000L,0x7fefffffffffffffL,0x7ff0000000000000L,0xfff0000000000000L,0x7ff8000000000000L,0x3ff0000000000000L,0x3fb999999999999aL};
                for(long bits:cases)hash=31*hash+DoubleToDecimal.toString(Double.longBitsToDouble(bits)).hashCode();
                long bits=42;for(int i=0;i<600;i++){bits=bits*6364136223846793005L+1442695040888963407L;
                    double value=Double.longBitsToDouble(bits);String text=DoubleToDecimal.toString(value);
                    hash=31*hash+text.hashCode();}return hash;
            }}
            """);compare(List.of("run"));
    }
    @Test void executesUnchangedPinnedJacksonFloatFormatting() throws Exception {
        jackson();fixture("""
            import com.fasterxml.jackson.core.io.schubfach.FloatToDecimal;
            public class Metadata {public static int run(){int hash=1;
                int[] cases={0,Integer.MIN_VALUE,1,-1,0x007fffff,0x00800000,0x7f7fffff,0x7f800000,0xff800000,0x7fc00000,0x3f800000,0x3dcccccd};
                for(int bits:cases)hash=31*hash+FloatToDecimal.toString(Float.intBitsToFloat(bits)).hashCode();
                int bits=42;for(int i=0;i<600;i++){bits=bits*1664525+1013904223;
                    float value=Float.intBitsToFloat(bits);String text=FloatToDecimal.toString(value);
                    hash=31*hash+text.hashCode();}return hash;
            }}
            """);compare(List.of("run"));
    }
    private void writeClass(String name,byte[] bytes) throws Exception {
        Path file=directory.resolve(name+".class");Files.createDirectories(file.getParent());Files.write(file,bytes);
    }
    private void assertDigest(Path artifact,String expected) throws Exception {
        assertEquals(expected,HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(artifact))));
    }

    private void compare(List<String> names) throws Exception {
        StringBuilder assertions=new StringBuilder();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)){
            Class<?> type=loader.loadClass(fixtureClassName);for(String name:names){Object expected=type.getMethod(name).invoke(null);assertions.append("assert.equal(").append(expected instanceof String?"text(e['"+fixtureClassName+".":"e['"+fixtureClassName+".").append(name).append(expected instanceof String?"']()),":"'](),").append(expected instanceof String?js((String)expected):expected).append(");\n");}
        }
        for(boolean reverse:new boolean[]{false,true})execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),assertions.toString());
    }

    private String js(String value) {
        if(value==null)return "null";
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
                const memory=new WebAssembly.Memory({initial:2,maximum:20});let heap=1024,allocations=0,failAllocation=false;
                const e=new WebAssembly.Instance(compiled,{env:{memory,__heap_alloc:size=>{
                    allocations++;if(failAllocation){failAllocation=false;return 0;}
                    const address=heap;heap+=size;
                    while(heap>memory.buffer.byteLength)memory.grow(1);
                    new Uint8Array(memory.buffer,address,size).fill(0x7f);return address;
                }}}).exports;
                assert.equal(allocations,0);heap=Math.max(heap,e.__exception_heap_base?.value??0);
                function text(pointer){if(!pointer)return null;const view=new DataView(memory.buffer);
                    const chars=view.getInt32(pointer+4,true),length=view.getInt32(chars+4,true);
                    let result='';for(let i=0;i<length;i++)result+=String.fromCharCode(view.getUint16(chars+8+2*i,true));return result;}
            """+assertions+"\n}");
        run("node",script.toString(),output.toString());
    }
}
