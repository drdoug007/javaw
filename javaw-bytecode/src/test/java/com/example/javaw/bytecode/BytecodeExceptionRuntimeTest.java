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

class BytecodeExceptionRuntimeTest implements Opcodes {
    @TempDir Path directory;

    private void fixture() throws Exception {
        Path source=directory.resolve("Fixture.java");
        Files.writeString(source,"""
            public class Fixture {
                static int ticks;
                private static long leaf(int kind,long value) throws Fault,Other {
                    if(kind==1) throw new Child(value);
                    if(kind==2) throw new Fault(value);
                    if(kind==3) throw new Other(value);
                    return value;
                }
                private static long middle(int kind) throws Fault,Other {
                    try {return leaf(kind,9007199254740993L);} finally {ticks+=3;}
                }
                public static long caught(int kind) {
                    ticks=0;
                    try {return middle(kind);}
                    catch(Child e) {return e.code+100+ticks;}
                    catch(Fault e) {return e.code+200+ticks;}
                    catch(Throwable e) {return ((Other)e).code+300+ticks;}
                }
                private static void rethrow(Fault value) throws Fault {
                    try {throw value;} catch(Fault same) {ticks++;throw same;}
                }
                private static long selective(int kind) throws Fault,Other {
                    try {return leaf(kind,5);} catch(Child e) {return 50;}
                }
                public static long unmatched(int kind) {
                    try {return selective(kind);} catch(Fault e) {return e.code+60;} catch(Other e) {return e.code+70;}
                }
                public static long stacked(int kind) {
                    try {return 100L+leaf(kind,7);} catch(Fault|Other e) {return 9;}
                }
                public static double floating(int kind,double value) {
                    try {if(kind!=0)throw new Fault(2);return value+1.25;} catch(Fault e) {return value-0.5;}
                }
                public static int identity() {
                    ticks=0;Fault value=new Child(7);
                    try {rethrow(value);return -1;} catch(Fault caught) {return (caught==value?10:0)+ticks;}
                }
                public static int nested() {
                    ticks=0;
                    try {
                        try {throw new Child(3);} catch(Child e) {throw new Other(4);} finally {ticks+=2;}
                    } catch(Other e) {return (int)e.code+ticks;}
                }
                public static int loop(int n) {
                    int sum=0;
                    for(int i=0;i<n;i++) {
                        try {if(i%2==0)throw new Fault(i);sum+=100;}
                        catch(Fault e) {sum+=(int)e.code;}
                    }
                    return sum;
                }
                public static long dynamic(int kind) {
                    Step step=new Worker();
                    try {return step.run(kind);} catch(Fault e) {return e.code+10;}
                }
                public static int overrideReturn() {
                    try {throw new Fault(1);} finally {return 44;}
                }
                public static int multi(int kind) {
                    try {leaf(kind,2);return 0;} catch(Fault|Other e) {return e instanceof Child?10:20;}
                }
                public static int construction() {
                    try {new Broken();return 0;} catch(Fault e) {return (int)e.code;}
                }
                public static Throwable make() {return new Fault(17);}
                public static void escape() throws Fault {throw new Child(123);}
                public static long payload(Throwable value) {return ((Fault)value).code;}
                public static int boundary(int kind) throws Fault {
                    try {if(kind==0)throw new Fault(1);} catch(Fault e) {return 7;}
                    throw new Fault(2);
                }
                public static int implicit(int n) {try{return 10/n;}catch(Throwable e){return 55;}}
                public static void nullThrow() throws Fault {throw (Fault)null;}
            }
            class Broken {Broken() throws Fault {throw new Fault(12);}}
            class Fault extends Throwable {long code;Fault(long code){this.code=code;}}
            class Child extends Fault {Child(long code){super(code);}}
            class Other extends Throwable {long code;Other(long code){this.code=code;}}
            interface Step {long run(int kind) throws Fault;}
            class Worker implements Step {
                public long run(int kind) throws Fault {if(kind!=0)throw new Fault(19);return 7;}
            }
            """);
        assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,"-g","-d",directory.toString(),source.toString()));
        Files.write(directory.resolve("LegacyHandled.class"),legacyHandler());
    }

    private byte[] legacyHandler() {
        ClassWriter writer=new ClassWriter(0);writer.visit(V1_5,ACC_PUBLIC,"LegacyHandled",null,"java/lang/Object",null);
        MethodVisitor method=writer.visitMethod(ACC_PUBLIC|ACC_STATIC,"run","(Ljava/lang/Throwable;)I",null,null);
        method.visitCode();Label start=new Label(),end=new Label(),handler=new Label(),alias=new Label();
        method.visitTryCatchBlock(start,end,handler,null);method.visitLabel(start);method.visitVarInsn(ALOAD,0);method.visitInsn(ATHROW);
        method.visitLabel(end);method.visitLabel(handler);method.visitLabel(alias);method.visitInsn(POP);method.visitIntInsn(BIPUSH,7);method.visitInsn(IRETURN);
        method.visitMaxs(1,1);method.visitEnd();writer.visitEnd();return writer.toByteArray();
    }

    /** Minimal linked allocation/constructor fixture, not an implementation of the JDK Throwable API. */
    private byte[] throwable() {
        ClassWriter writer=new ClassWriter(0);
        writer.visit(V17,ACC_PUBLIC,"java/lang/Throwable",null,"java/lang/Object",null);
        MethodVisitor init=writer.visitMethod(ACC_PUBLIC,"<init>","()V",null,null);
        init.visitCode();init.visitVarInsn(ALOAD,0);init.visitMethodInsn(INVOKESPECIAL,"java/lang/Object","<init>","()V",false);
        init.visitInsn(RETURN);init.visitMaxs(1,1);init.visitEnd();writer.visitEnd();return writer.toByteArray();
    }

    private String jar(boolean reverse) throws Exception {
        List<String> names=new ArrayList<>(List.of("Fixture","Fault","Child","Other","Step","Worker","Broken","LegacyHandled","java/lang/Throwable"));
        if(reverse) Collections.reverse(names);
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(JarOutputStream jar=new JarOutputStream(bytes)) {
            for(String name:names) {
                jar.putNextEntry(new JarEntry(name+".class"));
                jar.write(name.equals("java/lang/Throwable")?throwable():Files.readAllBytes(directory.resolve(name+".class")));
                jar.closeEntry();
            }
        }
        return BytecodeCompiler.compileJar(new ByteArrayInputStream(bytes.toByteArray()));
    }

    private void run(String... args) throws Exception {
        Path log=directory.resolve("command.log");
        Process process=new ProcessBuilder(args).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if(!process.waitFor(30,TimeUnit.SECONDS)){process.destroyForcibly();fail("Timed out: "+String.join(" ",args));}
        assertEquals(0,process.exitValue(),Files.readString(log));
    }

    private void execute(String wat,String assertions) throws Exception {
        Path input=directory.resolve("exceptions.wat"),output=directory.resolve("exceptions.wasm"),script=directory.resolve("run.cjs");
        Files.writeString(input,wat);run("wat2wasm","--enable-exceptions",input.toString(),"-o",output.toString());
        Files.writeString(script,"""
            const fs=require('node:fs');const assert=require('node:assert/strict');
            const moduleBytes=fs.readFileSync(process.argv[2]);const compiled=new WebAssembly.Module(moduleBytes);
            const memory=new WebAssembly.Memory({initial:2,maximum:20});let heap=1024;
            const instance=new WebAssembly.Instance(compiled,{env:{memory,__heap_alloc:size=>{
                const address=heap;heap+=size;new Uint8Array(memory.buffer,address,size).fill(0x7f);return address;
            }}});const e=instance.exports;
            """+assertions);
        run("node",script.toString(),output.toString());
    }

    @Test void executesTypedHandlersFinallyRethrowLoopsAndCrossMethodUnwindingInEitherJarOrder() throws Exception {
        fixture();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)) {
            Class<?> jvm=loader.loadClass("Fixture");StringBuilder checks=new StringBuilder();
            for(String name:new String[]{"caught","dynamic","multi","unmatched","stacked"}) for(int kind=0;kind<4;kind++) {
                Object expected=jvm.getMethod(name,int.class).invoke(null,kind);
                checks.append("assert.equal(e['Fixture.").append(name).append("'](").append(kind).append("),")
                        .append(expected).append(name.equals("multi")?"":"n").append(");");
            }
            for(int kind:new int[]{0,1}) for(double value:new double[]{0.0,-0.0,1.25,Double.NEGATIVE_INFINITY,Double.NaN})
                checks.append("assert.equal(e['Fixture.floating'](").append(kind).append(',').append(value).append("),")
                        .append(jvm.getMethod("floating",int.class,double.class).invoke(null,kind,value)).append(");");
            for(String name:new String[]{"identity","nested","overrideReturn","construction"})
                checks.append("assert.equal(e['Fixture.").append(name).append("'](),").append(jvm.getMethod(name).invoke(null)).append(");");
            for(int n:new int[]{0,1,7,20}) checks.append("assert.equal(e['Fixture.loop'](").append(n).append("),")
                    .append(jvm.getMethod("loop",int.class).invoke(null,n)).append(");");
            Object thrown=jvm.getMethod("make").invoke(null);
            Object legacy=loader.loadClass("LegacyHandled").getMethod("run",Throwable.class).invoke(null,thrown);
            checks.append("assert.equal(e['LegacyHandled.run'](e['Fixture.make']()),").append(legacy).append(");");
            checks.append("assert.equal(e['Fixture.boundary'](0),").append(jvm.getMethod("boundary",int.class).invoke(null,0)).append(");");
            assertThrows(java.lang.reflect.InvocationTargetException.class,()->jvm.getMethod("boundary",int.class).invoke(null,1));
            checks.append("assert.throws(()=>e['Fixture.boundary'](1),error=>error instanceof WebAssembly.Exception && error.is(e.__java_exception));");
            for(boolean reverse:new boolean[]{false,true}) {
                String wat=jar(reverse);
                assertEquals(1,wat.split(java.util.regex.Pattern.quote("(tag $__java_exception"),-1).length-1);
                execute(wat,checks.toString());
            }
        }
    }

    @Test void exposesUncaughtIdentityAndKeepsTrapsAndHostExceptionsOutsideJavaHandlers() throws Exception {
        fixture();String wat=jar(false);
        execute(wat,"""
            for(let i=0;i<3;i++) {
                let exception;try{e['Fixture.escape']();assert.fail('expected throw');}catch(error){exception=error;}
                assert(exception instanceof WebAssembly.Exception);assert(exception.is(e.__java_exception));
                const pointer=exception.getArg(e.__java_exception,0);
                assert.equal(e['Fixture.payload'](pointer),123n);assert.equal(e['Fixture.identity'](),11);
            }
            assert.equal(e['Fixture.implicit'](2),5);
            assert.throws(()=>e['Fixture.implicit'](0),WebAssembly.RuntimeError);
            assert.throws(()=>e['Fixture.nullThrow'](),WebAssembly.RuntimeError);
            const sentinel=new Error('host allocator failed');
            const foreign=new WebAssembly.Instance(compiled,{env:{memory,__heap_alloc:()=>{throw sentinel;}}}).exports;
            assert.throws(()=>foreign['Fixture.caught'](1),error=>error===sentinel);
            """);
    }
}
