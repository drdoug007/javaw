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
                    assert.equal(allocations,0);heap=Math.max(heap,e.__exception_heap_base?.value??0);return e;
                }
            """+assertions.replace("e['","fresh()['")+"\n}");
        run("node",script.toString(),output.toString());
    }
}
