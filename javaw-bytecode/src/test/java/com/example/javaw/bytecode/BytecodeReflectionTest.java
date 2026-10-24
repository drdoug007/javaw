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

class BytecodeReflectionTest implements Opcodes {
    @TempDir Path directory;
    private String release="17";
    private String fixtureClasspath;

    private byte[] fixture(String source) throws Exception {
        Path file=directory.resolve("Metadata.java");Files.writeString(file,source);
        List<String> options=new ArrayList<>(List.of("-g","--release",release,"-d",directory.toString()));
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

    @Test void discoversThroughMethodReferencesWithoutDirectDiscoveryCalls() throws Exception {
        fixture("""
            import java.lang.reflect.Method;
            public class Metadata {
                interface Discovery {Method[] run();}
                private static class Subject {public void target(){}}
                public static int run(){Discovery discovery=Subject.class::getDeclaredMethods;Method[] methods=discovery.run();
                    return methods.length==1 && methods[0].getName().equals("target")?1:0;}
            }
            """);
        int expected;try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)){expected=(Integer)loader.loadClass("Metadata").getMethod("run").invoke(null);}
        for(boolean reverse:new boolean[]{false,true})execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),"assert.equal(e['Metadata.run'](),"+expected+");\n");
    }

    @Test void discoversUnchangedPinnedSpringBootRecordDeclarations() throws Exception {
        Path repository=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()));
        Path artifact=repository.resolve("org/springframework/boot/spring-boot-autoconfigure/3.2.4/spring-boot-autoconfigure-3.2.4.jar");
        assertEquals("14b4694e1a782ea946264a7676da2936d2474abf0f875739b1af5da3bb7ad85b",
            java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(artifact))));
        fixtureClasspath=artifact.toString();
        fixture("""
            import org.springframework.boot.autoconfigure.amqp.RabbitConnectionDetails.Address;
            import java.lang.reflect.Method;
            public class Metadata {
                public static int run() throws Exception {
                    Method[] methods=Address.class.getDeclaredMethods();int score=methods.length;
                    Method host=Address.class.getDeclaredMethod("host");Method port=Address.class.getDeclaredMethod("port",(Class<?>[])null);
                    if(host.getReturnType()==String.class && host.getModifiers()==1)score+=10;
                    if(port.getReturnType()==int.class && port.getParameterCount()==0)score+=100;
                    if(Address.class.getDeclaredMethod("equals",Object.class).getReturnType()==boolean.class)score+=1000;
                    return score;
                }
            }
            """);
        String name="org/springframework/boot/autoconfigure/amqp/RabbitConnectionDetails$Address.class";byte[] original;
        try(JarFile jar=new JarFile(artifact.toFile())){original=jar.getInputStream(jar.getJarEntry(name)).readAllBytes();}
        Path output=directory.resolve(name);Files.createDirectories(output.getParent());Files.write(output,original);
        int expected;try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL(),artifact.toUri().toURL()},null)){expected=(Integer)loader.loadClass("Metadata").getMethod("run").invoke(null);}
        for(boolean reverse:new boolean[]{false,true})execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),"assert.equal(e['Metadata.run'](),"+expected+");\n");
        assertArrayEquals(original,Files.readAllBytes(output));
    }

    @Test void discoversOriginalDeclarationsWithoutInitializingTypes() throws Exception {
        fixture("""
            import java.lang.reflect.*;
            public class Metadata {
                static int initialized;
                private static class Hidden {static {initialized+=10;}}
                public static class Parent {public void inherited(){}}
                public static class Subject extends Parent {
                    static {initialized++;}
                    public Subject(){}private Subject(int value){}
                    public static long wide(int i,long j,double d) throws IllegalStateException,Exception {return j;}
                    private Hidden hidden(Hidden[] values){return null;}
                    protected void varargs(String... values){}
                }
                public interface Contract {
                    default int value(){return 1;}
                    static void statik(){}
                    private void secret(){}
                }
                public static int discovery() throws Exception {
                    Method[] methods=Subject.class.getDeclaredMethods();int flags=methods.length;
                    for(Method method:methods) {
                        if(method.getName().equals("wide"))flags+=method.getModifiers()==9?10:0;
                        if(method.getName().equals("hidden"))flags+=method.getModifiers()==2 && method.getReturnType()==Hidden.class && method.getParameterTypes()[0]==Hidden[].class?100:0;
                        if(method.getName().equals("varargs"))flags+=method.isVarArgs()?1000:0;
                        if(method.getDeclaringClass()!=Subject.class)return -1;
                    }
                    return flags+initialized*10000;
                }
                public static int copies() throws Exception {
                    Method first=Subject.class.getDeclaredMethod("wide",int.class,long.class,double.class);
                    Method second=Subject.class.getDeclaredMethod("wide",int.class,long.class,double.class);
                    Class<?>[] exceptions=first.getExceptionTypes();exceptions[0]=null;
                    Class<?>[] parameters=first.getParameterTypes();parameters[0]=null;
                    Method[] methods=Subject.class.getDeclaredMethods();methods[0]=null;
                    return (first.equals(second)?1:0)+(first.hashCode()==second.hashCode()?2:0)
                        +(first.getExceptionTypes()[0]==IllegalStateException.class && first.getExceptionTypes()[1]==Exception.class?4:0)
                        +(first.getParameterTypes()[0]==int.class?8:0)+(Subject.class.getDeclaredMethods()[0]!=null?16:0);
                }
                public static int interfaces(){int score=0;for(Method method:Contract.class.getDeclaredMethods()){
                    if(method.getName().equals("value") && method.isDefault())score+=1;
                    if(method.getName().equals("statik") && !method.isDefault())score+=2;
                    if(method.getName().equals("secret") && !method.isDefault())score+=4;
                }return score;}
                public static int absent(int mode) throws Exception {
                    try{
                        if(mode==0)Subject.class.getDeclaredMethod("inherited");
                        if(mode==1)Subject.class.getDeclaredMethod("<init>");
                        if(mode==2)Subject.class.getDeclaredMethod("wide",Integer.class,long.class,double.class);
                        if(mode==3)Subject.class.getDeclaredMethod("wide",new Class<?>[]{null,long.class,double.class});
                        if(mode==4)Subject.class.getDeclaredMethod(null);
                        if(mode==5)return int.class.getDeclaredMethods().length+String[].class.getDeclaredMethods().length;
                        return 0;
                    }catch(NoSuchMethodException expected){return 1;}catch(NullPointerException expected){return 2;}
                }
            }
            """);
        StringBuilder checks=new StringBuilder();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)) {
            Class<?> jvm=loader.loadClass("Metadata");
            for(String method:List.of("discovery","copies","interfaces","discovery"))checks.append("assert.equal(e['Metadata.").append(method).append("'](),").append(jvm.getMethod(method).invoke(null)).append(");\n");
            for(int mode=0;mode<6;mode++)checks.append("assert.equal(e['Metadata.absent'](").append(mode).append("),").append(jvm.getMethod("absent",int.class).invoke(null,mode)).append(");\n");
        }
        for(boolean reverse:new boolean[]{false,true})execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),checks.toString());
    }

    @Test void selectsCovariantReturnAndKeepsBridgeAndSyntheticDeclarations() throws Exception {
        fixture("""
            import java.lang.reflect.*;
            public class Metadata {
                interface Parent<T>{T value();}
                static class Child implements Parent<String>{public String value(){return "value";}}
                public static int run() throws Exception {
                    Method found=Child.class.getDeclaredMethod("value",(Class<?>[])null);int score=found.getReturnType()==String.class?1:0;
                    for(Method method:Child.class.getDeclaredMethods()){
                        if(method.isBridge() && method.isSynthetic() && method.getReturnType()==Object.class)score+=10;
                        if(!method.isBridge() && method.getReturnType()==String.class)score+=100;
                    }return score;
                }
            }
            """);
        int expected;try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)){expected=(Integer)loader.loadClass("Metadata").getMethod("run").invoke(null);}
        for(boolean reverse:new boolean[]{false,true})execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),"assert.equal(e['Metadata.run'](),"+expected+");\n");
    }

    @Test void explicitlyRejectsUndeliveredPlatformMetadataAndDiscoveredInvocation() throws Exception {
        byte[] bytes=fixture("""
            public class Metadata {
                public static void target(){}
                public static int run() throws Exception {
                    int score=0;try{String.class.getDeclaredMethods();}catch(UnsupportedOperationException expected){score++;}
                    try{Metadata.class.getDeclaredMethod("target").invoke(null);}catch(UnsupportedOperationException expected){score+=10;}
                    return score;
                }
            }
            """);
        execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(bytes)),"assert.equal(e['Metadata.run'](),11);\n");
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
