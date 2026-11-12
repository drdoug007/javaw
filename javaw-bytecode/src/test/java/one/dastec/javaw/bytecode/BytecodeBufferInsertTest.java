package one.dastec.javaw.bytecode;

import one.dastec.javaw.Wat2Wasm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.tools.ToolProvider;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.jar.*;

import static org.junit.jupiter.api.Assertions.*;

class BytecodeBufferInsertTest {
    @TempDir Path directory;
    private final String fixtureClassName="Metadata";
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
    @Test void insertArrayRange() throws Exception {
        fixture("""
public class Metadata {
 static class Value {public String toString(){return "object";}}
 public static int run(int group){int h=1;
 for(int offset:new int[]{Integer.MIN_VALUE,-1,0,1,3,4,Integer.MAX_VALUE})for(char[] text:new char[][]{null,new char[0],new char[]{65,0,(char)0xd800,(char)0xdc00,90}})for(int start:new int[]{-1,0,1,5,Integer.MAX_VALUE})for(int size:new int[]{-1,0,2,5,Integer.MAX_VALUE}){
 StringBuffer b=new StringBuffer("abc");int result=0;
 try{result=b.insert(offset,text,start,size)==b?7:9;}catch(StringIndexOutOfBoundsException e){result=11;}catch(IndexOutOfBoundsException e){result=13;}catch(NullPointerException e){result=17;}
 h=31*h+result;h=31*h+b.toString().hashCode();h=31*h+b.length();
 }
return h;}}
            """);compare(1);
    }
    @Test void insertSequence() throws Exception {
        fixture("""
public class Metadata {
 static class Value {public String toString(){return "object";}}
 static class Sequence implements CharSequence {public int length(){return 3;}public char charAt(int i){return "xyz".charAt(i);}public CharSequence subSequence(int s,int e){return "xyz".substring(s,e);}public String toString(){return "different";}}
 public static int run(int group){int h=1;
 for(int offset:new int[]{Integer.MIN_VALUE,-1,0,1,3,4,Integer.MAX_VALUE})for(CharSequence text:new CharSequence[]{null,"hello",new StringBuilder("xy"),new StringBuffer("ab"),new Sequence()}){
 StringBuffer b=new StringBuffer("abc");int result=0;
 try{result=b.insert(offset,text)==b?7:9;}catch(StringIndexOutOfBoundsException e){result=11;}catch(IndexOutOfBoundsException e){result=13;}catch(NullPointerException e){result=17;}
 h=31*h+result;h=31*h+b.toString().hashCode();h=31*h+b.length();
 }
StringBuffer self=new StringBuffer("abcd");self.insert(1,(CharSequence)self);h=31*h+self.toString().hashCode();
return h;}}
            """);compare(1);
    }
    @Test void insertSequenceRange() throws Exception {
        fixture("""
public class Metadata {
 static class Value {public String toString(){return "object";}}
 static class Sequence implements CharSequence {public int length(){return 3;}public char charAt(int i){return "xyz".charAt(i);}public CharSequence subSequence(int s,int e){return "xyz".substring(s,e);}public String toString(){return "different";}}
 public static int run(int group){int h=1;
 for(int offset:new int[]{Integer.MIN_VALUE,-1,0,1,3,4,Integer.MAX_VALUE})for(CharSequence text:new CharSequence[]{null,"hello",new StringBuilder("xy"),new StringBuffer("ab"),new Sequence()})for(int start:new int[]{-1,0,1,5,Integer.MAX_VALUE})for(int end:new int[]{-1,0,2,5,Integer.MAX_VALUE}){
 StringBuffer b=new StringBuffer("abc");int result=0;
 try{result=b.insert(offset,text,start,end)==b?7:9;}catch(StringIndexOutOfBoundsException e){result=11;}catch(IndexOutOfBoundsException e){result=13;}catch(NullPointerException e){result=17;}
 h=31*h+result;h=31*h+b.toString().hashCode();h=31*h+b.length();
 }
StringBuffer self=new StringBuffer("abcd");self.insert(1,(CharSequence)self,0,4);h=31*h+self.toString().hashCode();
StringBuffer observed=new StringBuffer("abc");
 CharSequence failing=new CharSequence(){public int length(){return 3;}public char charAt(int i){if(i==1)throw new IllegalStateException();return (char)(48+observed.length());}public CharSequence subSequence(int a,int b){throw new UnsupportedOperationException();}};
 try{observed.insert(1,failing,0,3);throw new Error();}catch(IllegalStateException e){h=31*h+observed.length();h=31*h+observed.toString().hashCode();}
 return h;}}
            """);compare(1);
    }
    private byte[] fixture(String source) throws Exception {
        Path file=directory.resolve("Metadata.java");Files.writeString(file,source);
        List<String> options=new ArrayList<>(List.of("-g","-parameters","--release","17","-d",directory.toString()));
        Path oracle=directory.resolve("CaseOracle.java");Files.writeString(oracle,"public class CaseOracle {public static void main(String[] args)throws Exception{if(Runtime.version().feature()!=17)throw new Error();var method=Class.forName(args[0]).getMethod(\"run\",int.class);System.out.print(\"[\");for(int i=0;i<Integer.parseInt(args[1]);i++){if(i>0)System.out.print(\",\");System.out.print(method.invoke(null,i));}System.out.print(\"]\");}}");
        options.add(file.toString());options.add(oracle.toString());
        assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,options.toArray(String[]::new)));
        return jar();
    }

    private byte[] jar() throws Exception {
        List<Path> files;
        try(var paths=Files.walk(directory)) {
            files=new ArrayList<>(paths.filter(path->path.toString().endsWith(".class") && !path.getFileName().toString().equals("CaseOracle.class")).sorted().toList());
        }
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(JarOutputStream jar=new JarOutputStream(bytes)) {
            for(Path file:files){
                String entry=directory.relativize(file).toString();
                jar.putNextEntry(new JarEntry(directory.relativize(file).toString()));jar.write(Files.readAllBytes(file));jar.closeEntry();}
        }
        return bytes.toByteArray();
    }

    private void compare(int groups) throws Exception {
        Path java=Path.of(System.getProperty("javaw.jdk17.home",System.getProperty("user.home")+"/Library/Java/JavaVirtualMachines/graalvm-ce-17.0.9/Contents/Home"),"bin/java");
        assertTrue(Files.isExecutable(java),"A JDK 17 oracle is required (javaw.jdk17.home)");
        run(java.toString(),"-Djava.locale.providers=CLDR","-Duser.language=","-Duser.country=","-Duser.variant=","-cp",directory.toString(),"CaseOracle",fixtureClassName,Integer.toString(groups));
        String expected=Files.readString(directory.resolve("command.log")).trim();
        execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar())),expected);
    }

    private void run(String... arguments) throws Exception {
        Path log=directory.resolve("command.log");Process process=new ProcessBuilder(arguments).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if(!process.waitFor(60,TimeUnit.SECONDS)){process.destroyForcibly();fail("Command timed out");}
        assertEquals(0,process.exitValue(),Files.readString(log));
    }
    private void execute(String wat,String expected) throws Exception {
        Path input=directory.resolve("metadata.wat"),output=directory.resolve("metadata.wasm"),script=directory.resolve("run.cjs");
        Files.writeString(input,wat);WasmTestSupport.assemble(input,output,Wat2Wasm.EXCEPTIONS);
        Files.writeString(script,"""
            const fs=require('node:fs'),assert=require('node:assert/strict');
            const compiled=new WebAssembly.Module(fs.readFileSync(process.argv[2]));
            const expected=EXPECTED;
            for(let group=0;group<expected.length;group++)for(let instance=0;instance<2;instance++) {
                const memory=new WebAssembly.Memory({initial:2});let heap=1024,allocations=0;
                const e=new WebAssembly.Instance(compiled,{env:{memory,__heap_alloc:size=>{
                    allocations++;
                    const address=heap;heap+=size;
                    while(heap>memory.buffer.byteLength)memory.grow(1);
                    new Uint8Array(memory.buffer,address,size).fill(0x7f);return address;
                }}}).exports;
                assert.equal(allocations,0);assert.equal(e.__gc_heap_bytes?.()??0,0);if(e.__gc_scribble)e.__gc_scribble.value=1;heap=Math.max(heap,e.__exception_heap_base?.value??0);
            """.replace("EXPECTED",expected)+"assert.equal(e['"+fixtureClassName+".run'](group),expected[group],'batch '+group);\n}");
        WasmTestSupport.node(script,output);
    }
}
