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

class BytecodeMethodHandleTest implements Opcodes {
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

    @Test void diagnosesVariableArityHandlesAndUnsupportedLookupApis() throws Exception {
        fixture("""
            import java.lang.invoke.*;
            public class Metadata {
                public static int count(Object... values){return values.length;}
                public static MethodHandle constant(){return null;}
                public static int lookup() throws Exception {
                    try{MethodHandles.lookup().findStatic(Metadata.class,"count",MethodType.methodType(int.class,Object[].class));return 0;}
                    catch(UnsupportedOperationException expected){return 1;}
                }
            }
            """);
        execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(false))),"assert.equal(e['Metadata.lookup'](),1);\n");
        add("constant",new Handle(H_INVOKESTATIC,"Metadata","count","([Ljava/lang/Object;)I",false));
        String error=assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(false)))).getMessage();
        assertTrue(error.contains("variable-arity method handles"),error);
        assertTrue(error.contains("Metadata.constant"),error);
    }

    @Test void executesDirectHandlesBindingAdaptationAndFieldAccessAgainstJvm() throws Exception {
        fixture("""
            import java.lang.invoke.*;
            public class Metadata {
                static int events;
                public static class Target {
                    public long field;public static int shared;
                    public Target(long value){field=value;}
                    public long add(int value){return field+value;}
                    private long secret(int value){return field-value;}
                    static {events+=10;}
                    public static long twice(long value){return value*2;}
                }
                public static MethodHandle statik(){return null;}
                public static MethodHandle constructor(){return null;}
                public static MethodHandle virtual(){return null;}
                public static MethodHandle secret(){return null;}
                public static MethodHandle getter(){return null;}
                public static MethodHandle setter(){return null;}
                public static MethodHandle staticGetter(){return null;}
                public static MethodHandle staticSetter(){return null;}
                public static int events(){return events;}
                public static int create(){statik();constructor();virtual();getter();return events;}
                public static long exact(long value) throws Throwable {return (long)statik().invokeExact(value);}
                public static double adapting(int value) throws Throwable {return (double)statik().invoke(value);}
                public static long objectResult() throws Throwable {Object value=statik().invoke((byte)7);return (Long)value;}
                public static long bound() throws Throwable {
                    Target t=(Target)constructor().invokeExact(40L);
                    return (long)virtual().bindTo(t).invokeWithArguments(2)+(long)secret().bindTo(t).invokeWithArguments(3);
                }
                public static long fields() throws Throwable {
                    Target t=new Target(0);setter().invokeExact(t,123L);staticSetter().invokeExact(7);
                    return (long)getter().invokeExact(t)+(int)staticGetter().invokeExact();
                }
                public static long adapted() throws Throwable {
                    MethodHandle h=statik().asType(MethodType.methodType(double.class,int.class));return (long)(double)h.invokeExact(9);
                }
                public static int unchanged(){MethodHandle h=statik();return h==statik() && h.asType(h.type())==h && h.asFixedArity()==h && !h.isVarargsCollector()?1:0;}
            }
            """);
        add("statik",new Handle(H_INVOKESTATIC,"Metadata$Target","twice","(J)J",false));
        add("constructor",new Handle(H_NEWINVOKESPECIAL,"Metadata$Target","<init>","(J)V",false));
        add("virtual",new Handle(H_INVOKEVIRTUAL,"Metadata$Target","add","(I)J",false));
        add("secret",new Handle(H_INVOKEVIRTUAL,"Metadata$Target","secret","(I)J",false));
        add("getter",new Handle(H_GETFIELD,"Metadata$Target","field","J",false));
        add("setter",new Handle(H_PUTFIELD,"Metadata$Target","field","J",false));
        add("staticGetter",new Handle(H_GETSTATIC,"Metadata$Target","shared","I",false));
        add("staticSetter",new Handle(H_PUTSTATIC,"Metadata$Target","shared","I",false));
        StringBuilder checks=new StringBuilder();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)) {
            Class<?> jvm=loader.loadClass("Metadata");
            for(String method:List.of("events","create","events","unchanged","bound","fields","adapted","objectResult","events")) {
                Object expected=jvm.getMethod(method).invoke(null);
                checks.append("assert.equal(e['Metadata.").append(method).append("'](),").append(expected).append(expected instanceof Long?"n);\n":");\n");
            }
            for(long value:new long[]{0,-1,17,Long.MIN_VALUE,Long.MAX_VALUE})checks.append("assert.equal(e['Metadata.exact'](").append(value).append("n),")
                .append(jvm.getMethod("exact",long.class).invoke(null,value)).append("n);\n");
            checks.append("assert.equal(e['Metadata.adapting'](13),").append(jvm.getMethod("adapting",int.class).invoke(null,13)).append(");\n");
        }
        for(boolean reverse:new boolean[]{false,true})execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),checks.toString());
    }
    @Test void executesClosedWorldLookupPrivateLookupAndInterfaceDefaultSpecialCalls() throws Exception {
        fixture("""
            import java.lang.invoke.*;
            public class Metadata {
                public interface Named {default long value(int n){return n+100;}}
                public static class Parent {public long inherited(int n){return n+200;}}
                public static class Target extends Parent implements Named {
                    public long base;
                    public Target(long base){this.base=base;}
                    private long secret(int n){return base-n;}
                    public long value(int n){return base+n;}
                    public static long twice(long n){return n*2;}
                }
                public static long run() throws Throwable {
                    MethodHandles.Lookup lookup=MethodHandles.lookup();
                    MethodHandle ctor=lookup.findConstructor(Target.class,MethodType.methodType(void.class,long.class));
                    Target target=(Target)ctor.invokeExact(40L);
                    MethodHandle virtual=lookup.findVirtual(Target.class,"value",MethodType.methodType(long.class,int.class));
                    MethodHandle inherited=lookup.findVirtual(Target.class,"inherited",MethodType.methodType(long.class,int.class));
                    MethodHandles.Lookup privateLookup=MethodHandles.privateLookupIn(Target.class,lookup);
                    MethodHandle secret=privateLookup.findVirtual(Target.class,"secret",MethodType.methodType(long.class,int.class));
                    MethodHandle special=privateLookup.findSpecial(Named.class,"value",MethodType.methodType(long.class,int.class),Target.class);
                    MethodHandle statik=lookup.findStatic(Target.class,"twice",MethodType.methodType(long.class,long.class));
                    return (long)virtual.bindTo(target).invokeWithArguments(2)+(long)inherited.invokeExact(target,3)
                        +(long)secret.bindTo(target).invokeWithArguments(4)+(long)special.bindTo(target).invokeWithArguments(5)+(long)statik.invokeExact(6L);
                }
                public static int caller(){return MethodHandles.lookup().lookupClass()==Metadata.class?1:0;}
                public static int failures(int n) throws Throwable {
                    MethodHandles.Lookup lookup=MethodHandles.lookup();
                    try {
                        switch(n){
                            case 0:lookup.findStatic(Target.class,"absent",MethodType.methodType(void.class));break;
                            case 1:lookup.findStatic(Target.class,"value",MethodType.methodType(long.class,int.class));break;
                            case 2:lookup.findVirtual(null,"value",MethodType.methodType(void.class));break;
                            case 3:lookup.findVirtual(Target.class,null,MethodType.methodType(void.class));break;
                            case 4:lookup.findVirtual(Target.class,"value",null);break;
                            case 5:MethodHandles.privateLookupIn(int.class,lookup);break;
                            case 6:MethodHandles.privateLookupIn(Target[].class,lookup);break;
                            case 7:lookup.findSpecial(Named.class,"value",MethodType.methodType(long.class,int.class),Target.class);break;
                        }
                        return 0;
                    }catch(NoSuchMethodException e){return 1;}catch(IllegalAccessException e){return 2;}
                    catch(NullPointerException e){return 3;}catch(IllegalArgumentException e){return 4;}
                }
            }
            """);
        StringBuilder checks=new StringBuilder();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)) {
            Class<?> jvm=loader.loadClass("Metadata");
            checks.append("assert.equal(e['Metadata.run'](),").append(jvm.getMethod("run").invoke(null)).append("n);\n");
            checks.append("assert.equal(e['Metadata.caller'](),1);\n");
            for(int n=0;n<8;n++)checks.append("assert.equal(e['Metadata.failures'](").append(n).append("),")
                .append(jvm.getMethod("failures",int.class).invoke(null,n)).append(");\n");
        }
        for(boolean reverse:new boolean[]{false,true})execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),checks.toString());
    }

    @Test void preservesExactAdaptationFailuresTargetExceptionsAndNullPriority() throws Exception {
        fixture("""
            import java.lang.invoke.*;
            public class Metadata {
                static RuntimeException failure=new RuntimeException("target");static int calls;
                public static long twice(long value){calls++;return value*2;}
                public static long throwing(long value){throw failure;}
                public static MethodHandle statik(){return null;}
                public static MethodHandle throwingHandle(){return null;}
                public static int test(int n) throws Throwable {
                    MethodHandle h=statik();
                    try {
                        switch(n){
                            case 0:return (int)h.invokeExact(1L);
                            case 1:return (int)(long)h.invokeExact(1);
                            case 2:return (int)(long)h.invokeWithArguments("bad");
                            case 3:return (int)(long)h.invokeWithArguments(new Object[]{null});
                            case 4:return (int)(long)h.invokeWithArguments();
                            case 5:h.bindTo(1L);break;
                            case 6:h.asType(MethodType.methodType(int.class,long.class));break;
                            case 7:MethodHandle missing=null;return (int)(long)missing.invokeExact(1L);
                            case 8:return (int)(long)throwingHandle().invokeExact(1L);
                            case 9:return (int)(long)h.invoke((Object)Integer.valueOf(3));
                            case 10:return (int)(long)h.invoke((Object)Double.valueOf(3));
                        }
                        return 0;
                    }catch(WrongMethodTypeException e){return 11;}catch(ClassCastException e){return 12;}
                    catch(NullPointerException e){return 13;}catch(IllegalArgumentException e){return 14;}
                    catch(RuntimeException e){return e==failure?15:16;}
                }
                public static int calls(){return calls;}
            }
            """);
        add("statik",new Handle(H_INVOKESTATIC,"Metadata","twice","(J)J",false));
        add("throwingHandle",new Handle(H_INVOKESTATIC,"Metadata","throwing","(J)J",false));
        StringBuilder checks=new StringBuilder();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)) {
            Class<?> jvm=loader.loadClass("Metadata");
            for(int n=0;n<11;n++)checks.append("assert.equal(e['Metadata.test'](").append(n).append("),")
                .append(jvm.getMethod("test",int.class).invoke(null,n)).append(");\n");
            checks.append("assert.equal(e['Metadata.calls'](),").append(jvm.getMethod("calls").invoke(null)).append(");\n");
        }
        execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(false))),checks.toString());
    }

    private void add(String name,Handle handle) throws Exception {
        Path file=directory.resolve("Metadata.class");ClassNode owner=new ClassNode();new ClassReader(Files.readAllBytes(file)).accept(owner,0);
        MethodNode method=owner.methods.stream().filter(m->m.name.equals(name)).findFirst().orElseThrow();
        method.instructions.clear();method.instructions.add(new LdcInsnNode(handle));method.instructions.add(new InsnNode(ARETURN));
        ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);owner.accept(writer);Files.write(file,writer.toByteArray());
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
