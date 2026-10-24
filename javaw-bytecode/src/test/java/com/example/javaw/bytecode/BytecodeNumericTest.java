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

class BytecodeNumericTest implements Opcodes {
    @TempDir Path directory;
    private String release="17";
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

    @Test void integerBits() throws Exception {
        fixture("""
            public class Metadata { public static int run() {
int hash=1,value=123456789;
 for(int i=0;i<600;i++){value=value*1664525+1013904223;if(i<34)value=i==0?0:1<<(i-1);
 hash=31*hash+Integer.bitCount(value);hash=31*hash+Integer.numberOfLeadingZeros(value);hash=31*hash+Integer.numberOfTrailingZeros(value);
 hash=31*hash+Integer.highestOneBit(value);hash=31*hash+Integer.lowestOneBit(value);hash=31*hash+Integer.reverse(value);hash=31*hash+Integer.reverseBytes(value);
 for(int d=-65;d<=65;d++)hash=31*hash+Integer.rotateLeft(value,d)+Integer.rotateRight(value,d);
 hash+=Integer.sum(value,17)+Integer.min(value,-3)+Integer.max(value,7); }return hash;
            }}
            """); compare(List.of("run"));
    }

    @Test void longBits() throws Exception {
        fixture("""
            public class Metadata { public static int run() {
int hash=1;long value=123456789;
 for(int i=0;i<600;i++){value=value*6364136223846793005L+1442695040888963407L;if(i<66)value=i==0?0:1L<<(i-1);
 hash=31*hash+Long.bitCount(value)+Long.numberOfLeadingZeros(value)+Long.numberOfTrailingZeros(value)+Long.signum(value);
 hash=31*hash+Long.hashCode(Long.highestOneBit(value)^Long.lowestOneBit(value)^Long.reverse(value)^Long.reverseBytes(value));
 for(int d=-129;d<=129;d++)hash=31*hash+Long.hashCode(Long.rotateLeft(value,d)^Long.rotateRight(value,d));
 hash+=Long.hashCode(Long.sum(value,17)^Long.min(value,-3)^Long.max(value,7));}return hash;
            }}
            """); compare(List.of("run"));
    }

    @Test void unsignedIntegers() throws Exception {
        fixture("""
            public class Metadata { public static int run() {
int hash=1;int[] values={0,1,-1,Integer.MIN_VALUE,Integer.MAX_VALUE,65535,-65536,7,-7};
 for(int a:values){for(int b:values){hash=31*hash+Integer.compareUnsigned(a,b);if(b!=0){hash=31*hash+Integer.divideUnsigned(a,b);hash=31*hash+Integer.remainderUnsigned(a,b);}}
 for(int r=2;r<=36;r++){String s=Integer.toUnsignedString(a,r);hash=31*hash+s.hashCode();if(Integer.parseUnsignedInt(s,r)!=a)throw new Error();}
 hash+=Long.hashCode(Integer.toUnsignedLong(a));}
 hash+=Integer.parseUnsignedInt(new StringBuilder("x+4294967295z"),1,12,10);hash+=Integer.parseUnsignedInt("+１２٣",10)+Integer.parseUnsignedInt("42");
 for(String s:new String[]{null,"","+","-0","-1","4294967296"," 1","1_0"})try{Integer.parseUnsignedInt(s);throw new Error();}catch(NumberFormatException e){hash++;}
 try{Integer.divideUnsigned(1,0);throw new Error();}catch(ArithmeticException e){hash++;}
 try{Integer.remainderUnsigned(1,0);throw new Error();}catch(ArithmeticException e){hash++;}
 try{Integer.parseUnsignedInt((CharSequence)null,0,1,10);throw new Error();}catch(NullPointerException e){hash++;}
 try{Integer.parseUnsignedInt("x",-1,1,10);throw new Error();}catch(IndexOutOfBoundsException e){hash++;}
 return hash;
            }}
            """); compare(List.of("run"));
    }

    private void compare(List<String> names) throws Exception {
        StringBuilder assertions=new StringBuilder();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)){
            Class<?> type=loader.loadClass("Metadata");for(String name:names){Object expected=type.getMethod(name).invoke(null);assertions.append("assert.equal(").append(expected instanceof String?"text(e['Metadata.":"e['Metadata.").append(name).append(expected instanceof String?"']()),":"'](),").append(expected instanceof String?js((String)expected):expected).append(");\n");}
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
