package one.dastec.javaw.bytecode;

import one.dastec.javaw.Wat2Wasm;
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

class BytecodeJacksonNumberInputTest implements Opcodes {
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
            for(String simple:new String[]{"NumberInput","BigDecimalParser"}) {
                String owner="com/fasterxml/jackson/core/io/"+simple;
                ClassNode node=new ClassNode();
                new ClassReader(jar.getInputStream(jar.getJarEntry(owner+".class")).readAllBytes()).accept(node,0);
                node.methods.removeIf(method->simple.equals("NumberInput")
                    ? !(method.name.equals("<clinit>") || method.name.equals("parseInt") || method.name.equals("parseLong") || method.name.equals("parseLong19") || method.name.equals("inLongRange") || method.name.equals("parseBigInteger") && method.desc.equals("(Ljava/lang/String;)Ljava/math/BigInteger;") || method.name.equals("parseBigDecimal") && !method.desc.contains("Z"))
                    : method.name.equals("parseWithFastParser") || method.name.equals("<init>"));
                ClassWriter writer=new ClassWriter(0);node.accept(writer);
                Path file=directory.resolve(owner+".class");Files.createDirectories(file.getParent());Files.write(file,writer.toByteArray());
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

    @Test void bigIntegerDecimal() throws Exception {
        fixture("""
            import java.math.*;
            import com.fasterxml.jackson.core.io.NumberInput;
            public class Metadata {public static int run(){
                int hash=1;
 for(String text:new String[]{"0","-0","+17","127","-128","9223372036854775808","-9223372036854775809","123456789012345678901234567890","\u0661\u0662\u0663"}){BigInteger a=NumberInput.parseBigInteger(text);hash=31*hash+a.toString().hashCode();hash=31*hash+a.bitLength();}
 for(String text:new String[]{"","+","-"," 1","1 ","1.0","1e2","--1","0x10"})try{NumberInput.parseBigInteger(text);throw new Error();}catch(NumberFormatException e){hash++;}
 try{NumberInput.parseBigInteger(null);throw new Error();}catch(NullPointerException e){hash+=3;}return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void intString() throws Exception {
        fixture("""
            import java.math.*;
            import com.fasterxml.jackson.core.io.NumberInput;
            public class Metadata {public static int run(){
                int hash=1;for(String text:new String[]{"0","-0","+1","1","9","10","123456789","2147483647","-2147483648","000000000000001","\u0661\u0662"})hash=31*hash+NumberInput.parseInt(text);
 for(String text:new String[]{"","+","-","2147483648","-2147483649","1.0"," 1","1 ","1e3","--1"})try{NumberInput.parseInt(text);throw new Error();}catch(RuntimeException e){hash=31*hash+e.getClass().getName().hashCode();}
 try{NumberInput.parseInt(null);throw new Error();}catch(NullPointerException e){hash++;}return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void intArray() throws Exception {
        fixture("""
            import java.math.*;
            import com.fasterxml.jackson.core.io.NumberInput;
            public class Metadata {public static int run(){
                int hash=1;for(String text:new String[]{"0","1","12","123","1234","12345","123456","1234567","12345678","123456789","+123","0000123"}){char[] a=("xx"+text+"yy").toCharArray();hash=31*hash+NumberInput.parseInt(a,2,text.length());}
 try{NumberInput.parseInt(null,0,1);throw new Error();}catch(NullPointerException e){hash++;}
 try{NumberInput.parseInt(new char[0],0,1);throw new Error();}catch(ArrayIndexOutOfBoundsException e){hash++;}return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void longString() throws Exception {
        fixture("""
            import java.math.*;
            import com.fasterxml.jackson.core.io.NumberInput;
            public class Metadata {public static int run(){
                int hash=1;for(String text:new String[]{"0","+1","-1","123456789","1234567890","9223372036854775807","-9223372036854775808","0000000000000000000001"})hash=31*hash+Long.hashCode(NumberInput.parseLong(text));
 for(String text:new String[]{"","-","9223372036854775808","-9223372036854775809","1.0"," 1","1e10"})try{NumberInput.parseLong(text);throw new Error();}catch(RuntimeException e){hash=31*hash+e.getClass().getName().hashCode();}
 try{NumberInput.parseLong(null);throw new Error();}catch(NullPointerException e){hash++;}return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void longArray() throws Exception {
        fixture("""
            import java.math.*;
            import com.fasterxml.jackson.core.io.NumberInput;
            public class Metadata {public static int run(){
                int hash=1;for(String text:new String[]{"1000000000","12345678901","123456789012","1234567890123","12345678901234","123456789012345","1234567890123456","12345678901234567","123456789012345678","000000000000000001"})hash=31*hash+Long.hashCode(NumberInput.parseLong(("xx"+text+"yy").toCharArray(),2,text.length()));
 try{NumberInput.parseLong(null,0,10);throw new Error();}catch(NullPointerException e){hash++;}
 try{NumberInput.parseLong(new char[0],0,10);throw new Error();}catch(ArrayIndexOutOfBoundsException e){hash++;}return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void long19() throws Exception {
        fixture("""
            import java.math.*;
            import com.fasterxml.jackson.core.io.NumberInput;
            public class Metadata {public static int run(){
                int hash=1;for(String text:new String[]{"0000000000000000000","0000000000000000001","1234567890123456789","9223372036854775807","9223372036854775808"})for(boolean negative:new boolean[]{false,true})hash=31*hash+Long.hashCode(NumberInput.parseLong19(("xx"+text+"yy").toCharArray(),2,negative));
 try{NumberInput.parseLong19(null,0,false);throw new Error();}catch(NullPointerException e){hash++;}
 try{NumberInput.parseLong19(new char[18],0,false);throw new Error();}catch(ArrayIndexOutOfBoundsException e){hash++;}return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void longRangeString() throws Exception {
        fixture("""
            import java.math.*;
            import com.fasterxml.jackson.core.io.NumberInput;
            public class Metadata {public static int run(){
                int hash=1;for(String text:new String[]{"","0","123456789012345678","9223372036854775806","9223372036854775807","9223372036854775808","9223372036854775809","9999999999999999999","00000000000000000000","12345678901234567890"})for(boolean negative:new boolean[]{false,true})hash=31*hash+(NumberInput.inLongRange(text,negative)?1:0);
 try{NumberInput.inLongRange((String)null,false);throw new Error();}catch(NullPointerException e){hash++;}return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void longRangeArray() throws Exception {
        fixture("""
            import java.math.*;
            import com.fasterxml.jackson.core.io.NumberInput;
            public class Metadata {public static int run(){
                int hash=1;for(String text:new String[]{"","0","123456789012345678","9223372036854775806","9223372036854775807","9223372036854775808","9223372036854775809","9999999999999999999","00000000000000000000"})for(boolean negative:new boolean[]{false,true})hash=31*hash+(NumberInput.inLongRange(("xx"+text+"yy").toCharArray(),2,text.length(),negative)?1:0);
 try{NumberInput.inLongRange((char[])null,0,19,false);throw new Error();}catch(NullPointerException e){hash++;}
 try{NumberInput.inLongRange(new char[0],0,19,false);throw new Error();}catch(ArrayIndexOutOfBoundsException e){hash++;}return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void decimalStrings() throws Exception {
        fixture("""
            import java.math.*;
            import com.fasterxml.jackson.core.io.NumberInput;
            public class Metadata {public static int run(){
                int hash=1;for(String text:new String[]{"0","-0","+1.00","12345678901234567890.00001","1E+30","-1e-30","1e2147483648","1e-2147483647",".123","123.","\u0661.\u0662"}){BigDecimal a=NumberInput.parseBigDecimal(text);hash=31*hash+a.toString().hashCode();hash=31*hash+a.scale();}
 for(String text:new String[]{"","+","1e","1e2147483649","1.2.3"," 1","1 ","NaN","Infinity"})try{NumberInput.parseBigDecimal(text);throw new Error();}catch(RuntimeException e){hash=31*hash+e.getClass().getName().hashCode();}
 try{NumberInput.parseBigDecimal((String)null);throw new Error();}catch(NullPointerException e){hash++;}return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void decimalArrays() throws Exception {
        fixture("""
            import java.math.*;
            import com.fasterxml.jackson.core.io.NumberInput;
            public class Metadata {
                public static int runLarge0(){return large(500);}
                public static int runLarge1(){return large(501);}
                public static int runLarge2(){return large(1001);}
                static int large(int length){
                    String text="1234567890".repeat(length/10)+"1".repeat(length%10);
                    BigDecimal result=NumberInput.parseBigDecimal(("xx"+text+"yy").toCharArray(),2,length);
                    return 31*result.toString().hashCode()+result.scale();
                }
                public static int runLongFailure(){
                    try{NumberInput.parseBigDecimal(("1".repeat(1001)+"x").toCharArray());throw new Error();}
                    catch(RuntimeException e){return e.getClass().getName().hashCode();}
                }
                public static int runSlices(){int hash=1;
                    for(int[] range:new int[][]{{-1,1},{0,-1},{3,2},{Integer.MAX_VALUE,1},{0,Integer.MAX_VALUE}})
                        try{NumberInput.parseBigDecimal(new char[]{'1','2','3'},range[0],range[1]);throw new Error();}
                        catch(RuntimeException e){hash=31*hash+e.getClass().getName().hashCode();}
                    return hash;
                }
                public static int run(){
                int hash=1;for(String text:new String[]{"0","-0.000","+12.34","12345678901234567890.00001","1E+30","-1e-30",".123","123."}){char[] a=("xx"+text+"yy").toCharArray();BigDecimal b=NumberInput.parseBigDecimal(a,2,text.length());hash=31*hash+b.toString().hashCode();hash=31*hash+b.scale();hash=31*hash+NumberInput.parseBigDecimal(text.toCharArray()).toString().hashCode();a[2]='9';hash=31*hash+b.hashCode();}
 for(String text:new String[]{"","+","1e","1.2.3","NaN"})try{NumberInput.parseBigDecimal(text.toCharArray());throw new Error();}catch(RuntimeException e){hash=31*hash+e.getClass().getName().hashCode();}
 try{NumberInput.parseBigDecimal((char[])null);throw new Error();}catch(NullPointerException e){hash++;}return hash;
            }}
            """);compare(List.of("run","runLarge0","runLarge1","runLarge2","runLongFailure","runSlices"));
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

    private void run(String... arguments) throws Exception {
        Path log=directory.resolve("command.log");Process process=new ProcessBuilder(arguments).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if(!process.waitFor(60,TimeUnit.SECONDS)){process.destroyForcibly();fail("Command timed out");}
        assertEquals(0,process.exitValue(),Files.readString(log));
    }
    private void execute(String wat,String assertions) throws Exception {
        Path input=directory.resolve("metadata.wat"),output=directory.resolve("metadata.wasm"),script=directory.resolve("run.cjs");
        Files.writeString(input,wat);run(Wat2Wasm.command(input,output,Wat2Wasm.EXCEPTIONS).toArray(String[]::new));
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
        run("node",script.toString(),output.toString());
    }
}
