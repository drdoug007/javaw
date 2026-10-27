package one.dastec.javaw.bytecode;

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

class BytecodeBufferInsertTest {
    @TempDir Path directory;
    private String release="17",fixtureClasspath,fixtureClassName="Metadata";
    private byte[] pinnedCarrier;
    private final Map<String,byte[]> carriers=new HashMap<>();
    @Test void insertObject() throws Exception {
        fixture("""
public class Metadata {
 static class Value {public String toString(){return "object";}}
 public static int run(int group){int h=1;
 for(int offset:new int[]{Integer.MIN_VALUE,-1,0,1,3,4,Integer.MAX_VALUE})for(Object text:new Object[]{null,"hello",Integer.valueOf(-17),new Value()}){
 StringBuffer b=new StringBuffer("abc");int result=0;
 try{result=b.insert(offset,text)==b?7:9;}catch(StringIndexOutOfBoundsException e){result=11;}catch(IndexOutOfBoundsException e){result=13;}catch(NullPointerException e){result=17;}
 h=31*h+result;h=31*h+b.toString().hashCode();h=31*h+b.length();
 }
return h;}}
            """);compare(1);
    }
    @Test void insertBoolean() throws Exception {
        fixture("""
public class Metadata {
 static class Value {public String toString(){return "object";}}
 public static int run(int group){int h=1;
 for(int offset:new int[]{Integer.MIN_VALUE,-1,0,1,3,4,Integer.MAX_VALUE})for(boolean text:new boolean[]{false,true}){
 StringBuffer b=new StringBuffer("abc");int result=0;
 try{result=b.insert(offset,text)==b?7:9;}catch(StringIndexOutOfBoundsException e){result=11;}catch(IndexOutOfBoundsException e){result=13;}catch(NullPointerException e){result=17;}
 h=31*h+result;h=31*h+b.toString().hashCode();h=31*h+b.length();
 }
return h;}}
            """);compare(1);
    }
    @Test void insertInt() throws Exception {
        fixture("""
public class Metadata {
 static class Value {public String toString(){return "object";}}
 public static int run(int group){int h=1;
 for(int offset:new int[]{Integer.MIN_VALUE,-1,0,1,3,4,Integer.MAX_VALUE})for(int text:new int[]{0,17,-17,Integer.MIN_VALUE,Integer.MAX_VALUE}){
 StringBuffer b=new StringBuffer("abc");int result=0;
 try{result=b.insert(offset,text)==b?7:9;}catch(StringIndexOutOfBoundsException e){result=11;}catch(IndexOutOfBoundsException e){result=13;}catch(NullPointerException e){result=17;}
 h=31*h+result;h=31*h+b.toString().hashCode();h=31*h+b.length();
 }
return h;}}
            """);compare(1);
    }
    @Test void insertLong() throws Exception {
        fixture("""
public class Metadata {
 static class Value {public String toString(){return "object";}}
 public static int run(int group){int h=1;
 for(int offset:new int[]{Integer.MIN_VALUE,-1,0,1,3,4,Integer.MAX_VALUE})for(long text:new long[]{0L,-17L,Long.MIN_VALUE,Long.MAX_VALUE}){
 StringBuffer b=new StringBuffer("abc");int result=0;
 try{result=b.insert(offset,text)==b?7:9;}catch(StringIndexOutOfBoundsException e){result=11;}catch(IndexOutOfBoundsException e){result=13;}catch(NullPointerException e){result=17;}
 h=31*h+result;h=31*h+b.toString().hashCode();h=31*h+b.length();
 }
return h;}}
            """);compare(1);
    }
    @Test void insertFloat() throws Exception {
        fixture("""
public class Metadata {
 static class Value {public String toString(){return "object";}}
 public static int run(int group){int h=1;
 for(int offset:new int[]{Integer.MIN_VALUE,-1,0,1,3,4,Integer.MAX_VALUE})for(float text:new float[]{0f,-0f,Float.NaN,Float.POSITIVE_INFINITY,Float.MIN_VALUE,Float.MAX_VALUE,1.25f}){
 StringBuffer b=new StringBuffer("abc");int result=0;
 try{result=b.insert(offset,text)==b?7:9;}catch(StringIndexOutOfBoundsException e){result=11;}catch(IndexOutOfBoundsException e){result=13;}catch(NullPointerException e){result=17;}
 h=31*h+result;h=31*h+b.toString().hashCode();h=31*h+b.length();
 }
return h;}}
            """);compare(1);
    }
    @Test void insertDouble() throws Exception {
        fixture("""
public class Metadata {
 static class Value {public String toString(){return "object";}}
 public static int run(int group){int h=1;
 for(int offset:new int[]{Integer.MIN_VALUE,-1,0,1,3,4,Integer.MAX_VALUE})for(double text:new double[]{0d,-0d,Double.NaN,Double.NEGATIVE_INFINITY,Double.MIN_VALUE,Double.MAX_VALUE,1.25d}){
 StringBuffer b=new StringBuffer("abc");int result=0;
 try{result=b.insert(offset,text)==b?7:9;}catch(StringIndexOutOfBoundsException e){result=11;}catch(IndexOutOfBoundsException e){result=13;}catch(NullPointerException e){result=17;}
 h=31*h+result;h=31*h+b.toString().hashCode();h=31*h+b.length();
 }
return h;}}
            """);compare(1);
    }
    @Test void insertArray() throws Exception {
        fixture("""
public class Metadata {
 static class Value {public String toString(){return "object";}}
 public static int run(int group){int h=1;
 for(int offset:new int[]{Integer.MIN_VALUE,-1,0,1,3,4,Integer.MAX_VALUE})for(char[] text:new char[][]{null,new char[0],new char[]{65,0,(char)0xd800,(char)0xdc00,90}}){
 StringBuffer b=new StringBuffer("abc");int result=0;
 try{result=b.insert(offset,text)==b?7:9;}catch(StringIndexOutOfBoundsException e){result=11;}catch(IndexOutOfBoundsException e){result=13;}catch(NullPointerException e){result=17;}
 h=31*h+result;h=31*h+b.toString().hashCode();h=31*h+b.length();
 }
return h;}}
            """);compare(1);
    }
    private byte[] fixture(String source) throws Exception {
        Path file=directory.resolve("Metadata.java");Files.writeString(file,source);
        List<String> options=new ArrayList<>(List.of("-g","-parameters","--release",release,"-d",directory.toString()));
        if(fixtureClasspath!=null)options.addAll(List.of("-classpath",fixtureClasspath));
        Path oracle=directory.resolve("CaseOracle.java");Files.writeString(oracle,"public class CaseOracle {public static void main(String[] args)throws Exception{if(Runtime.version().feature()!=17)throw new Error();var method=Class.forName(args[0]).getMethod(\"run\",int.class);System.out.print(\"[\");for(int i=0;i<Integer.parseInt(args[1]);i++){if(i>0)System.out.print(\",\");System.out.print(method.invoke(null,i));}System.out.print(\"]\");}}");
        options.add(file.toString());options.add(oracle.toString());
        assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,options.toArray(String[]::new)));
        return jar(false);
    }

    private byte[] jar(boolean reverse) throws Exception {
        List<Path> files;
        try(var paths=Files.walk(directory)) {
            files=new ArrayList<>(paths.filter(path->path.toString().endsWith(".class") && !path.getFileName().toString().equals("CaseOracle.class") && !path.toString().contains("javaw/runtime/WordBreaks.class")).sorted().toList());
        }
        if(reverse)Collections.reverse(files);
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(JarOutputStream jar=new JarOutputStream(bytes)) {
            for(Path file:files){
                String entry=directory.relativize(file).toString();
                if(pinnedCarrier!=null && entry.startsWith("com/fasterxml/") && !entry.endsWith("JacksonInject$Value.class"))continue;
                jar.putNextEntry(new JarEntry(directory.relativize(file).toString()));jar.write(pinnedCarrier!=null && entry.endsWith("JacksonInject$Value.class")?pinnedCarrier:carriers.getOrDefault(entry,Files.readAllBytes(file)));jar.closeEntry();}
        }
        return bytes.toByteArray();
    }

    private void compare(int groups) throws Exception {
        Path java=Path.of(System.getProperty("javaw.jdk17.home",System.getProperty("user.home")+"/Library/Java/JavaVirtualMachines/graalvm-ce-17.0.9/Contents/Home"),"bin/java");
        assertTrue(Files.isExecutable(java),"A JDK 17 oracle is required (javaw.jdk17.home)");
        run(java.toString(),"-Djava.locale.providers=CLDR","-Duser.language=","-Duser.country=","-Duser.variant=","-cp",directory.toString()+(fixtureClasspath==null?"":File.pathSeparator+fixtureClasspath),"CaseOracle",fixtureClassName,Integer.toString(groups));
        String expected=Files.readString(directory.resolve("command.log")).trim();
        for(boolean reverse:new boolean[]{false,true})
            execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),expected);
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
