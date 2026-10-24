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

class BytecodeMethodTypeTest implements Opcodes {
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

    @Test void matchesAuditedFactoriesInspectionAndImmutableTransformations() throws Exception {
        fixture("""
            import java.lang.invoke.MethodType;
            public class Metadata {
                static MethodType type(int index){
                    switch(index) {
                        case 0:return MethodType.methodType(void.class);
                        case 1:return MethodType.methodType(int.class,String.class);
                        case 2:return MethodType.methodType(Object[].class,long.class,double.class,int[][].class);
                        case 3:return MethodType.methodType(String.class,new Class<?>[]{int.class,boolean.class,byte.class,char.class,short.class,float.class});
                        case 4:return MethodType.methodType(Object.class,MethodType.methodType(void.class,String[][].class));
                        case 5:return MethodType.genericMethodType(3,true);
                        case 6:return MethodType.methodType(Void.class,Integer.class,Void.class,Double.class);
                        default:return MethodType.genericMethodType(0);
                    }
                }
                static MethodType transformed(int index,int operation){
                    MethodType t=type(index);
                    switch(operation){
                        case 0:return t;case 1:return t.changeReturnType(Object.class);
                        case 2:return t.insertParameterTypes(0,long.class,double.class);
                        case 3:return t.appendParameterTypes(String.class,int[].class);
                        case 4:return t.dropParameterTypes(0,t.parameterCount());
                        case 5:return t.erase();case 6:return t.generic();case 7:return t.wrap();case 8:return t.unwrap();
                        default:return t.parameterCount()==0?t:t.changeParameterType(0,Object.class);
                    }
                }
                public static String descriptor(int index,int operation){return transformed(index,operation).toMethodDescriptorString();}
                public static String text(int index,int operation){return transformed(index,operation).toString();}
                public static int flags(int index,int operation){MethodType t=transformed(index,operation);
                    return t.parameterCount()+(t.hasPrimitives()?256:0)+(t.hasWrappers()?512:0);}
                public static String last(int index){return type(index).lastParameterType().getName();}
                public static String result(int index){return type(index).returnType().getName();}
                public static int defensive(){
                    Class<?>[] parameters={int.class,String.class};MethodType a=MethodType.methodType(void.class,parameters);
                    parameters[0]=Object.class;Class<?>[] copy=a.parameterArray();copy[1]=Object.class;
                    MethodType b=MethodType.methodType(void.class,int.class,String.class);
                    return (a==b?1:0)+(a.equals(b)?2:0)+(a.hashCode()==b.hashCode()?4:0)
                        +(a.parameterType(0)==int.class && a.parameterType(1)==String.class?8:0)
                        +(a.changeReturnType(void.class)==a?16:0)+(a.dropParameterTypes(1,1)==a?32:0)
                        +(a.insertParameterTypes(0,new Class<?>[0])==a?64:0)+(!a.equals(null) && !a.equals(new Object())?128:0);
                }
            }
            """);
        StringBuilder checks=new StringBuilder();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)) {
            Class<?> jvm=loader.loadClass("Metadata");
            for(int index=0;index<8;index++) {
                for(int operation=0;operation<10;operation++)for(String method:List.of("descriptor","text","flags")) {
                    Object expected=jvm.getMethod(method,int.class,int.class).invoke(null,index,operation);
                    checks.append("assert.equal(").append(expected instanceof String?"text(":"").append("e['Metadata.").append(method).append("'](")
                        .append(index).append(',').append(operation).append(expected instanceof String?")),":"),").append(expected instanceof String?js((String)expected):expected).append(");\n");
                }
                for(String method:List.of("last","result"))checks.append("assert.equal(text(e['Metadata.").append(method).append("'](").append(index).append(")),")
                    .append(js((String)jvm.getMethod(method,int.class).invoke(null,index))).append(");\n");
            }
            checks.append("assert.equal(e['Metadata.defensive'](),").append(jvm.getMethod("defensive").invoke(null)).append(");\n");
        }
        for(boolean reverse:new boolean[]{false,true})execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),checks.toString());
    }

    @Test void preservesSlotLimitsAndTypedFactoryAndTransformationFailures() throws Exception {
        fixture("""
            import java.lang.invoke.MethodType;
            public class Metadata {
                public static int failure(int index){
                    MethodType t=MethodType.methodType(void.class,long.class);
                    try {
                        switch(index){
                            case 0:MethodType.methodType(null);break;
                            case 1:MethodType.methodType(void.class,(Class<?>[])null);break;
                            case 2:MethodType.methodType(void.class,new Class<?>[]{null});break;
                            case 3:MethodType.methodType(void.class,void.class);break;
                            case 4:t.parameterType(-1);break;
                            case 5:t.changeParameterType(1,int.class);break;
                            case 6:t.changeParameterType(0,null);break;
                            case 7:t.changeParameterType(0,void.class);break;
                            case 8:t.insertParameterTypes(2,int.class);break;
                            case 9:t.dropParameterTypes(1,0);break;
                            case 10:t.dropParameterTypes(0,2);break;
                            case 11:MethodType.genericMethodType(-1);break;
                            case 12:MethodType.genericMethodType(255,true);break;
                            case 13:MethodType.genericMethodType(256);break;
                            case 14:t.appendParameterTypes((Class<?>[])null);break;
                            case 15:Class<?>[] types=new Class<?>[128];for(int i=0;i<types.length;i++)types[i]=double.class;MethodType.methodType(void.class,types);break;
                            case 16:MethodType.methodType(void.class,int.class,(Class<?>[])null);break;
                            case 17:t.changeReturnType(null);break;
                            case 18:MethodType.methodType(void.class,new Class<?>[256]);break;
                        }
                        return 0;
                    }catch(NullPointerException e){return 1;}catch(IllegalArgumentException e){return 2;}catch(IndexOutOfBoundsException e){return 3;}
                }
                public static int maximum(){
                    Class<?>[] types=new Class<?>[128];for(int i=0;i<127;i++)types[i]=double.class;types[127]=int.class;
                    return MethodType.methodType(void.class,types).parameterCount()+MethodType.genericMethodType(255).parameterCount();
                }
            }
            """);
        StringBuilder checks=new StringBuilder();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)) {
            Class<?> jvm=loader.loadClass("Metadata");
            for(int index=0;index<19;index++)checks.append("assert.equal(e['Metadata.failure'](").append(index).append("),")
                .append(jvm.getMethod("failure",int.class).invoke(null,index)).append(");\n");
            checks.append("assert.equal(e['Metadata.maximum'](),").append(jvm.getMethod("maximum").invoke(null)).append(");\n");
        }
        execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(false))),checks.toString());
    }

    @Test void resolvesCanonicalMethodTypeConstantsLazilyWithoutInitializingSignatureClasses() throws Exception {
        release="8";
        fixture("""
            import java.lang.invoke.MethodType;
            public class Metadata {
                private static class Hidden {static {Counts.value+=100;}}
                public static MethodType literal(){return null;}
                public static MethodType other(){return null;}
                public static MethodType simple(){return null;}
                public static int events(){return Counts.value;}
                public static int canonical(){return literal()==MethodType.methodType(Hidden.class,Dormant.class,int[][].class,long.class,double.class)?1:0;}
                public static int crossClass(){return (Other.literal()==MethodType.methodType(void.class,int.class)?1:0)+(Other.literal()==simple()?2:0);}
                public static String descriptor(){return literal().toMethodDescriptorString();}
                public static int initialize(){new Dormant();return Counts.value;}
            }
            class Counts {static int value;}
            class Dormant {static {Counts.value++;}}
            class Other {static java.lang.invoke.MethodType literal(){return null;}}
            """);
        replaceLiteral("Metadata",List.of("literal","other"),"(LDormant;[[IJD)LMetadata$Hidden;");
        replaceLiteral("Other",List.of("literal"),"(I)V");
        replaceLiteral("Metadata",List.of("simple"),"(I)V");
        StringBuilder checks=new StringBuilder();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)) {
            Class<?> jvm=loader.loadClass("Metadata");
            for(String method:List.of("events","canonical","crossClass","events","initialize","events"))checks.append("assert.equal(e['Metadata.").append(method).append("'](),")
                .append(jvm.getMethod(method).invoke(null)).append(");\n");
            checks.append("assert.equal(text(e['Metadata.descriptor']()),").append(js((String)jvm.getMethod("descriptor").invoke(null))).append(");\n");
            assertSame(jvm.getMethod("literal").invoke(null),jvm.getMethod("other").invoke(null));
        }
        checks.append("const value=e['Metadata.literal'](),before=allocations;assert.equal(e['Metadata.other'](),value);assert.equal(allocations,before);\n");
        for(boolean reverse:new boolean[]{false,true})execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),checks.toString());
        execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(false))),"""
            failAllocation=true;try{e['Metadata.literal']();assert.fail('allocation unexpectedly succeeded');}
            catch(error){assert(error instanceof WebAssembly.Exception);}
            assert(e['Metadata.literal']()>0);assert.equal(e['Metadata.events'](),0);
            """);
    }
    private void replaceLiteral(String owner,List<String> methods,String descriptor) throws Exception {
        Path file=directory.resolve(owner+".class");ClassNode type=new ClassNode();new ClassReader(Files.readAllBytes(file)).accept(type,0);
        for(MethodNode method:type.methods)if(methods.contains(method.name)) {
            method.instructions.clear();method.instructions.add(new LdcInsnNode(Type.getMethodType(descriptor)));method.instructions.add(new InsnNode(ARETURN));
            method.maxStack=1;method.maxLocals=0;
        }
        ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);type.accept(writer);Files.write(file,writer.toByteArray());
    }

    @Test void diagnosesUnlinkedAndMalformedMethodTypeConstantsAtOriginalSite() throws Exception {
        for(String descriptor:List.of("(Lmissing/Type;)V","("+"J".repeat(128)+")V","(V)V")) {
            ClassWriter writer=new ClassWriter(0);writer.visit(V17,ACC_PUBLIC,"Metadata",null,"java/lang/Object",null);writer.visitSource("Metadata.java",null);
            MethodVisitor method=writer.visitMethod(ACC_PUBLIC|ACC_STATIC,"literal","()Ljava/lang/invoke/MethodType;",null,null);
            method.visitCode();Label line=new Label();method.visitLabel(line);method.visitLineNumber(17,line);
            method.visitLdcInsn(Type.getMethodType(descriptor));method.visitInsn(ARETURN);method.visitMaxs(1,0);method.visitEnd();writer.visitEnd();
            String message=assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileClassWithExceptionRuntime(writer.toByteArray())).getMessage();
            assertTrue(message.contains("Metadata.java:17"),message);assertTrue(message.contains("Metadata.literal"),message);
        }
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
