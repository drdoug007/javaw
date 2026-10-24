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

class BytecodeProxyTest implements Opcodes {
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

    @Test void diagnosesProxyReturnDescriptorConflicts() throws Exception {
        byte[] bytes=fixture("""
            public class Metadata {
                public interface Wide {Object value();}
                public interface Narrow {String value();}
            }
            """);
        String error=assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(bytes),List.of(List.of("Metadata$Wide","Metadata$Narrow")))).getMessage();
        assertTrue(error.contains("return descriptors require an explicit adapter"),error);
    }

    @Test void executesRegisteredProxyHandlersMetadataAndReflectiveDelegationAgainstJvm() throws Exception {
        fixture("""
            import java.lang.reflect.*;
            public class Metadata {
                public interface Service {long add(int n,long m);String name();default int defaultValue(){return 12;}}
                public interface Marker {int marker();}
                public static class Target implements Service {
                    public long add(int n,long m){return n+m;}
                    public String name(){return "target";}
                }
                public static class Handler implements InvocationHandler {
                    final Target target=new Target();
                    public Object invoke(Object proxy,Method method,Object[] args) throws Throwable {
                        if(method.getName().equals("equals"))return proxy==args[0];
                        if(method.getName().equals("hashCode"))return 123;
                        if(method.getName().equals("toString"))return "proxy";
                        if(method.getName().equals("marker"))return 7;
                        if(method.getName().equals("defaultValue"))return method.invoke(target,args);
                        return method.invoke(target,args);
                    }
                }
                static Object create(Handler h){return Proxy.newProxyInstance(Metadata.class.getClassLoader(),new Class<?>[]{Service.class,Marker.class},h);}
                public static long run(){Handler h=new Handler();Object value=create(h);Service service=(Service)value;
                    return service.add(2,40L)+service.defaultValue()+((Marker)value).marker()+value.hashCode()
                        +(value.equals(value)?1000:0)+(Proxy.isProxyClass(value.getClass())?2000:0)
                        +(Proxy.getInvocationHandler(value)==h?4000:0);}
                public static int proxyClass(){
                    Class<?> type=Proxy.getProxyClass(Metadata.class.getClassLoader(),Service.class,Marker.class);
                    return type==create(new Handler()).getClass() && Proxy.isProxyClass(type)?1:0;
                }
                public static int nullInterface(){
                    try{Proxy.getProxyClass(Metadata.class.getClassLoader(),Service.class,null);return 0;}
                    catch(NullPointerException expected){return 1;}
                }
                public static String text(){return create(new Handler()).toString();}
                public static String name(){return ((Service)create(new Handler())).name();}
                public static int metadata(){
                    final int[] result={0};InvocationHandler h=(proxy,method,args)->{
                        Class<?>[] p=method.getParameterTypes();p[0]=String.class;
                        result[0]=(method.getName().equals("add")?1:0)+(method.getDeclaringClass()==Service.class?2:0)
                            +(method.getReturnType()==long.class?4:0)+(method.getParameterTypes()[0]==int.class?8:0)
                            +(method.getParameterCount()==2?16:0);return 0L;};
                    ((Service)Proxy.newProxyInstance(Metadata.class.getClassLoader(),new Class<?>[]{Service.class,Marker.class},h)).add(1,2L);return result[0];
                }
                public static int rejected(){try{Proxy.newProxyInstance(Metadata.class.getClassLoader(),new Class<?>[]{Marker.class,Service.class},new Handler());return 0;}
                    catch(IllegalArgumentException expected){return 1;}}
            }
            """);
        StringBuilder checks=new StringBuilder();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)) {
            Class<?> jvm=loader.loadClass("Metadata");
            for(String method:List.of("run","metadata","proxyClass","nullInterface")) {
                Object result=jvm.getMethod(method).invoke(null);checks.append("assert.equal(e['Metadata.").append(method).append("'](),").append(result).append(result instanceof Long?"n);\n":");\n");
            }
            for(String method:List.of("text","name"))checks.append("assert.equal(text(e['Metadata.").append(method).append("']()),")
                .append(js((String)jvm.getMethod(method).invoke(null))).append(");\n");
        }
        checks.append("assert.equal(e['Metadata.rejected'](),1);\n");
        for(boolean reverse:new boolean[]{false,true})execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse)),List.of(List.of("Metadata$Service","Metadata$Marker"))),checks.toString());
    }

    @Test void preservesProxyExceptionIntersectionsNullArgumentsAndReturnFailures() throws Exception {
        fixture("""
            import java.lang.reflect.*;
            public class Metadata {
                public static class Checked extends Exception {}
                static final Checked checked=new Checked();static final RuntimeException runtime=new RuntimeException();static final Error error=new Error();
                public interface Wide {int failure(int value) throws Checked;int empty();}
                public interface Narrow {int failure(int value);}
                static final InvocationHandler handler=(proxy,method,args)->{
                    if(method.getName().equals("empty"))return args==null?77:0;
                    int n=(Integer)args[0];if(n==0)throw checked;if(n==1)throw runtime;if(n==2)throw error;
                    if(n==3)return null;if(n==4)return "wrong";return 42;
                };
                static Wide create(int mixed){return (Wide)Proxy.newProxyInstance(Metadata.class.getClassLoader(),
                    mixed==0?new Class<?>[]{Wide.class}:new Class<?>[]{Wide.class,Narrow.class},handler);}
                public static int test(int mixed,int n){
                    try{return create(mixed).failure(n);}catch(Checked e){return e==checked?10:0;}
                    catch(UndeclaredThrowableException e){return e.getUndeclaredThrowable()==checked && e.getCause()==checked?11:0;}
                    catch(NullPointerException e){return 12;}catch(ClassCastException e){return 13;}
                    catch(RuntimeException e){return e==runtime?14:0;}catch(Error e){return e==error?15:0;}
                }
                public static int empty(){return create(0).empty();}
                public static int shapeIdentity(){return create(0).getClass()==create(0).getClass() && create(0).getClass()!=create(1).getClass()?1:0;}
            }
            """);
        StringBuilder checks=new StringBuilder();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)) {
            Class<?> jvm=loader.loadClass("Metadata");
            for(int mixed=0;mixed<2;mixed++)for(int n=0;n<6;n++)checks.append("assert.equal(e['Metadata.test'](").append(mixed).append(',').append(n).append("),")
                .append(jvm.getMethod("test",int.class,int.class).invoke(null,mixed,n)).append(");\n");
            for(String method:List.of("empty","shapeIdentity"))checks.append("assert.equal(e['Metadata.").append(method).append("'](),")
                .append(jvm.getMethod(method).invoke(null)).append(");\n");
        }
        execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(false)),List.of(List.of("Metadata$Wide"),List.of("Metadata$Wide","Metadata$Narrow"))),checks.toString());
    }

    @Test void diagnosesUnregisteredProxyGenerationBeforeReturningWat() throws Exception {
        byte[] jar=fixture("""
            public class Metadata {
                public static Object run(){return java.lang.reflect.Proxy.newProxyInstance(Metadata.class.getClassLoader(),new Class<?>[]{Runnable.class},(p,m,a)->null);}
            }
            """);
        String error=assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar))).getMessage();
        assertTrue(error.contains("Metadata.run"),error);assertTrue(error.contains("register an ordered public-interface shape"),error);
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
