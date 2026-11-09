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

class BytecodeFusedMathTest implements Opcodes {
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

    private void compare(int groups) throws Exception {
        StringJoiner expected=new StringJoiner(",","[","]");
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)) {
            var run=loader.loadClass(fixtureClassName).getMethod("run",int.class);
            for(int group=0;group<groups;group++)expected.add(run.invoke(null,group).toString());
        }
        for(boolean reverse:new boolean[]{false,true})execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),expected.toString());
    }

    private String doubleFixture(String operation) {
        return """
            public class Metadata {
                interface Ternary {double apply(double a,double b,double c);}
                static double calculate(double a,double b,double c){return OPERATION(a,b,c);}
                public static int run(int group){
                    double[] values={0.0,-0.0,1,-1,0.5,-0.5,1.5,-1.5,0x1.0000000000001p0,0x1.fffffffffffffp-1,
                        Double.MIN_VALUE,-Double.MIN_VALUE,Double.MIN_NORMAL,-Double.MIN_NORMAL,Double.MAX_VALUE,-Double.MAX_VALUE,
                        Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NaN,0x0.fffffffffffffp-1022,-0x0.fffffffffffffp-1022,
                        0x1.0000002p0,0x1.ffffffcp-1};
                    if(calculate(0x1.0000002p0,0x1.ffffffcp-1,-1)==0)throw new Error("unfused cancellation");
                    if(calculate(Double.MAX_VALUE,2,-Double.MAX_VALUE)!=Double.MAX_VALUE)throw new Error("intermediate overflow");
                    if(calculate(Double.MAX_VALUE,2,Double.NEGATIVE_INFINITY)!=Double.NEGATIVE_INFINITY)throw new Error("finite product plus infinity");
                    Ternary method=OPERATION;
                    int hash=Double.hashCode(method.apply(values[group%values.length],0.5,-0.0));
                    if(group<values.length){double a=values[group];
                        for(double b:values)for(double c:values)hash=31*hash+Double.hashCode(calculate(a,b,c));
                    } else {
                        long bits=42+group;
                        for(int i=0;i<96;i++){
                            bits=bits*6364136223846793005L+1442695040888963407L;double a=Double.longBitsToDouble(bits);
                            bits=bits*6364136223846793005L+1442695040888963407L;double b=Double.longBitsToDouble(bits);
                            bits=bits*6364136223846793005L+1442695040888963407L;double c=i%3==0?-(a*b):Double.longBitsToDouble(bits);
                            hash=31*hash+Double.hashCode(calculate(a,b,c));
                        }
                    }
                    return hash;
                }
            }
            """.replace("Ternary method=OPERATION;","Ternary method="+operation.replace(".fma","::fma")+";").replace("OPERATION",operation);
    }

    @Test void doubleFmaRoundsOnceAcrossFiniteAndSpecialInputs() throws Exception {
        fixture(doubleFixture("Math.fma"));compare(31);
    }

    @Test void floatFmaAvoidsDoubleRounding() throws Exception {
        fixture("""
            public class Metadata {
                interface Ternary {float apply(float a,float b,float c);}
                public static int run(int group){
                    float[] values={0.0f,-0.0f,1,-1,0.5f,-0.5f,1.5f,-1.5f,0x1.000002p0f,0x1.fffffep-1f,
                        Float.MIN_VALUE,-Float.MIN_VALUE,Float.MIN_NORMAL,-Float.MIN_NORMAL,Float.MAX_VALUE,-Float.MAX_VALUE,
                        Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NaN,0x0.fffffep-126f,-0x0.fffffep-126f,
                        0x1.001p0f,0x1.ffep-1f};
                    float a=0x1.000002p0f,b=1.5f,c=-Float.MIN_VALUE;
                    if(Math.fma(a,b,c)==(float)Math.fma((double)a,(double)b,(double)c))throw new Error("double rounding");
                    if(Math.fma(Float.MAX_VALUE,2f,-Float.MAX_VALUE)!=Float.MAX_VALUE)throw new Error("intermediate overflow");
                    Ternary method=Math::fma;int hash=Float.hashCode(method.apply(values[group%values.length],0.5f,-0.0f));
                    if(group<values.length){a=values[group];
                        for(float x:values)for(float y:values)hash=31*hash+Float.hashCode(Math.fma(a,x,y));
                    } else {
                        int bits=42+group;
                        for(int i=0;i<96;i++){
                            bits=bits*1664525+1013904223;a=Float.intBitsToFloat(bits);
                            bits=bits*1664525+1013904223;b=Float.intBitsToFloat(bits);
                            bits=bits*1664525+1013904223;c=i%3==0?-(a*b):Float.intBitsToFloat(bits);
                            hash=31*hash+Float.hashCode(Math.fma(a,b,c));
                        }
                    }
                    return hash;
                }
            }
            """);compare(31);
    }

    @Test void executesOriginalPinnedJacksonJava17Fma() throws Exception {
        Path artifact=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString())).resolve("com/fasterxml/jackson/core/jackson-core/2.15.4/jackson-core-2.15.4.jar");
        assertEquals("8dc9210dd285db366f45f518dd1e6a9ccfeb0f1a8e184a899fe96d29edf1fd94",HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(artifact))));
        String name="com/fasterxml/jackson/core/io/doubleparser/FastDoubleSwar";ClassNode selected=new ClassNode();
        try(JarFile jar=new JarFile(artifact.toFile())){new ClassReader(jar.getInputStream(jar.getJarEntry("META-INF/versions/17/"+name+".class")).readAllBytes()).accept(selected,0);}
        selected.fields.clear();selected.methods.removeIf(method->!method.name.equals("fma"));assertEquals(1,selected.methods.size());
        MethodNode method=selected.methods.get(0);assertEquals("(DDD)D",method.desc);
        assertTrue(Arrays.stream(method.instructions.toArray()).anyMatch(instruction->instruction instanceof MethodInsnNode call && call.owner.equals("java/lang/Math") && call.name.equals("fma") && call.desc.equals("(DDD)D")));
        ClassWriter writer=new ClassWriter(0);selected.accept(writer);Path file=directory.resolve(name+".class");Files.createDirectories(file.getParent());Files.write(file,writer.toByteArray());
        fixtureClasspath=directory.toString();fixtureClassName="com.fasterxml.jackson.core.io.doubleparser.Metadata";
        fixture("package com.fasterxml.jackson.core.io.doubleparser;\n"+doubleFixture("FastDoubleSwar.fma"));compare(31);
    }

    @Test void malformedFmaDescriptorIsRejected() {
        ClassWriter writer=new ClassWriter(0);writer.visit(V17,ACC_PUBLIC,"BadFma",null,"java/lang/Object",null);
        MethodVisitor method=writer.visitMethod(ACC_PUBLIC|ACC_STATIC,"run","()F",null,null);
        method.visitCode();method.visitInsn(DCONST_0);method.visitInsn(DCONST_0);method.visitInsn(DCONST_0);
        method.visitMethodInsn(INVOKESTATIC,"java/lang/Math","fma","(DDD)F",false);method.visitInsn(FRETURN);method.visitMaxs(6,0);method.visitEnd();writer.visitEnd();
        var failure=assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileClassWithExceptionRuntime(writer.toByteArray()));
        assertTrue(failure.getMessage().contains("fma"),failure.getMessage());
    }

    private void run(String... arguments) throws Exception {
        Path log=directory.resolve("command.log");Process process=new ProcessBuilder(arguments).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if(!process.waitFor(60,TimeUnit.SECONDS)){process.destroyForcibly();fail("Command timed out");}
        assertEquals(0,process.exitValue(),Files.readString(log));
    }
    private void execute(String wat,String expected) throws Exception {
        Path input=directory.resolve("metadata.wat"),output=directory.resolve("metadata.wasm"),script=directory.resolve("run.cjs");
        Files.writeString(input,wat);run(Wat2Wasm.command(input,output,Wat2Wasm.EXCEPTIONS).toArray(String[]::new));
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
        run("node",script.toString(),output.toString());
    }
}
