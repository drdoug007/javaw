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

class BytecodeCharsetTest implements Opcodes {
    @TempDir Path directory;
    private String release="17";
    private String fixtureClasspath;
    private String fixtureClassName="Metadata";

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

    @Test void namedUtf8Constructors() throws Exception {
        fixture("""
            public class Metadata {
                static int result(byte[] bytes,int offset,int count,String name,boolean whole){
                    try{String text=whole?new String(bytes,name):new String(bytes,offset,count,name);return 31*text.hashCode()+text.length();}
                    catch(Exception e){return e.getClass().getName().hashCode();}}
                public static int run(int group) throws Exception {int hash=1;
                    String[] names={"UTF-8","utf8","UnIcOdE-1-1-uTf-8"};String name=names[group%names.length];
                    for(int second=0;second<256;second++){
                        byte[] pair={(byte)group,(byte)second};hash=31*hash+result(pair,0,2,name,true);
                        hash=31*hash+result(new byte[]{65,pair[0],pair[1],90},1,2,name,false);
                    }
                    long seed=group+17;for(int n=0;n<32;n++){byte[] bytes=new byte[n%9];
                        for(int j=0;j<bytes.length;j++){seed=seed*6364136223846793005L+1442695040888963407L;bytes[j]=(byte)(seed>>>32);}
                        hash=31*hash+result(bytes,0,bytes.length,name,true);
                    }
                    if(group==0){for(String n:new String[]{null,"","!","unknown","UTF-8"})for(byte[] b:new byte[][]{null,new byte[0],new byte[]{65}}){
                        hash=31*hash+result(b,-1,-1,n,false);hash=31*hash+result(b,0,0,n,true);}
                        byte[] snapshot={65,66};String text=new String(snapshot,"utf8");snapshot[0]=90;hash=31*hash+text.hashCode();
                        for(int[] range:new int[][]{{-1,1},{0,-1},{1,2},{Integer.MAX_VALUE,1},{1,Integer.MAX_VALUE}})
                            hash=31*hash+result(new byte[2],range[0],range[1],"UTF-8",false);
                    }return hash;}}
            """);compare(256);
    }

    @Test void utf8NamedEncoding() throws Exception {
        fixture("""
            import java.util.Arrays;
            public class Metadata {
                static int encoded(String text,String name){try{byte[] bytes=text.getBytes(name);return 31*Arrays.hashCode(bytes)+bytes.length;}
                    catch(Exception e){return e.getClass().getName().hashCode();}}
                public static int run(int group) throws Exception {int hash=1;String name="UTF-8";
                    for(int c=group*1024;c<(group+1)*1024;c++)hash=31*hash+encoded(new String(new char[]{(char)c}),name);
                    int[] units={0,65,127,128,255,256,0xd800,0xdbff,0xdc00,0xdfff,0xffff};
                    for(int b:units)for(int c:units)hash=31*hash+encoded(new String(new char[]{(char)(0xd800+group),(char)b,(char)c}),name);
                    if(group==0){for(String n:new String[]{null,"","!","unknown",name})hash=31*hash+encoded("é😀",n);
                        String text="Aé😀";byte[] a=text.getBytes(name),b=text.getBytes(name);if(a==b)throw new Error();a[0]=0;if(b[0]!=65)throw new Error();
                        if("".getBytes(name)=="".getBytes(name))throw new Error();}
                    return hash;}}
            """);compare(64);
    }

    @Test void latin1Decoding() throws Exception {
        fixture("""
            public class Metadata {public static int run(int group) throws Exception {int hash=1;String[] names={"ISO-8859-1","cp819","ISO_8859_1","csISOLatin1","IBM819","latin1","ISO8859-1","iso-ir-100","ISO_8859-1:1987","ISO8859_1","819","l1","ISO_8859-1","8859_1","IBM-819"};
                for(String name:names)for(int b=0;b<256;b++){
                    byte[] bytes={(byte)b,(byte)(255-b),65};String text=new String(bytes,name);
                    hash=31*hash+text.hashCode()+text.length();hash=31*hash+new String(bytes,1,1,name).hashCode();
                }return hash;}}
            """);compare(1);
    }

    @Test void latin1Encoding() throws Exception {
        fixture("""
            import java.util.Arrays;
            public class Metadata {
                static int encoded(String text,String name){try{byte[] bytes=text.getBytes(name);return 31*Arrays.hashCode(bytes)+bytes.length;}
                    catch(Exception e){return e.getClass().getName().hashCode();}}
                public static int run(int group) throws Exception {int hash=1;String name="ISO-8859-1";
                    for(int c=group*1024;c<(group+1)*1024;c++)hash=31*hash+encoded(new String(new char[]{(char)c}),name);
                    int[] units={0,65,127,128,255,256,0xd800,0xdbff,0xdc00,0xdfff,0xffff};
                    for(int b:units)for(int c:units)hash=31*hash+encoded(new String(new char[]{(char)(0xd800+group),(char)b,(char)c}),name);
                    if(group==0){for(String n:new String[]{null,"","!","unknown",name})hash=31*hash+encoded("é😀",n);
                        String text="Aé😀";byte[] a=text.getBytes(name),b=text.getBytes(name);if(a==b)throw new Error();a[0]=0;if(b[0]!=65)throw new Error();
                        if("".getBytes(name)=="".getBytes(name))throw new Error();}
                    return hash;}}
            """);compare(64);
    }

    private void compare(int groups) throws Exception {
        List<Integer> hashes=new ArrayList<>();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)){
            var method=loader.loadClass(fixtureClassName).getMethod("run",int.class);
            for(int group=0;group<groups;group++)hashes.add((Integer)method.invoke(null,group));
        }
        for(boolean reverse:new boolean[]{false,true})
            execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),hashes.toString());
    }

    private void run(String... arguments) throws Exception {
        Path log=directory.resolve("command.log");Process process=new ProcessBuilder(arguments).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if(!process.waitFor(60,TimeUnit.SECONDS)){process.destroyForcibly();fail("Command timed out");}
        assertEquals(0,process.exitValue(),Files.readString(log));
    }
    private void execute(String wat,String expected) throws Exception {
        Path input=directory.resolve("metadata.wat"),output=directory.resolve("metadata.wasm"),script=directory.resolve("run.cjs");
        Files.writeString(input,wat);run("wat2wasm","--enable-exceptions",input.toString(),"-o",output.toString());
        Files.writeString(script,"""
            const fs=require('node:fs'),assert=require('node:assert/strict');
            const compiled=new WebAssembly.Module(fs.readFileSync(process.argv[2]));
            const expected=EXPECTED;
            for(let group=0;group<expected.length;group++)for(let instance=0;instance<2;instance++) {
                const memory=new WebAssembly.Memory({initial:2,maximum:20});let heap=1024,allocations=0;
                const e=new WebAssembly.Instance(compiled,{env:{memory,__heap_alloc:size=>{
                    allocations++;
                    const address=heap;heap+=size;
                    while(heap>memory.buffer.byteLength)memory.grow(1);
                    new Uint8Array(memory.buffer,address,size).fill(0x7f);return address;
                }}}).exports;
                assert.equal(allocations,0);heap=Math.max(heap,e.__exception_heap_base?.value??0);
            """.replace("EXPECTED",expected)+"assert.equal(e['"+fixtureClassName+".run'](group),expected[group],'batch '+group);\n}");
        run("node",script.toString(),output.toString());
    }
}
