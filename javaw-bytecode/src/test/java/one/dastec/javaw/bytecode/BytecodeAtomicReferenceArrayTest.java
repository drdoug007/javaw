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

class BytecodeAtomicReferenceArrayTest implements Opcodes {
    @TempDir Path directory;
    private String release="17";
    private String fixtureClasspath;

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

    @Test void matchesSingleThreadedAtomicReferenceArrayAccess() throws Exception {
        fixture("""
            import java.util.concurrent.atomic.AtomicReferenceArray;
            public class Metadata {
                public static String run(){
                    AtomicReferenceArray<String> array=new AtomicReferenceArray<>(4);String text=array+"|"+array.length()+"|"+array.get(0);
                    array.set(0,"a");array.lazySet(1,"b");array.setPlain(2,"c");array.setOpaque(3,"d");
                    text+="|"+array+array.get(0)+array.getPlain(1)+array.getOpaque(2)+array.getAcquire(3);
                    String x=new String("x"),y=new String("x");array.setRelease(0,x);
                    text+="|"+array.compareAndSet(0,y,"no")+array.compareAndSet(0,x,"yes")+array.get(0)+array.weakCompareAndSet(1,"b","B")+array.weakCompareAndSetPlain(1,"B","p")+array.weakCompareAndSetVolatile(1,"p","v")+array.weakCompareAndSetAcquire(1,"v","q")+array.weakCompareAndSetRelease(1,"q","r")+array.weakCompareAndSetRelease(1,"z","s");
                    text+="|"+array.compareAndExchange(2,"c","C")+array.compareAndExchange(2,"zz","D")+array.compareAndExchangeAcquire(2,"C","E")+array.compareAndExchangeRelease(2,"E","F")+array.get(2);
                    text+="|"+array.getAndSet(3,null)+array.get(3)+array.getAndUpdate(0,v->v+"!")+array.updateAndGet(0,v->v+"?")+array.getAndAccumulate(1,"+",String::concat)+array.accumulateAndGet(1,"-",(l,r)->r+l);
                    String[] source={"s",null,"t"};AtomicReferenceArray<String> copy=new AtomicReferenceArray<>(source);source[0]="changed";copy.set(2,"u");
                    AtomicReferenceArray<Object> wide=new AtomicReferenceArray<Object>(new Integer[]{1,2});wide.set(0,"string");
                    return text+"|"+array+"|"+copy+source[2]+"|"+wide+"|"+new AtomicReferenceArray<>(0)+new AtomicReferenceArray<>(new Object[]{null});
                }
                public static int failures(){
                    int score=0;AtomicReferenceArray<String> array=new AtomicReferenceArray<>(2);
                    for(int index:new int[]{-1,2,Integer.MIN_VALUE}){
                        try{array.get(index);throw new Error();}catch(IndexOutOfBoundsException expected){score=31*score+expected.getClass().getName().hashCode();}
                        try{array.set(index,"x");throw new Error();}catch(IndexOutOfBoundsException expected){score=31*score+expected.getClass().getName().hashCode();}
                        try{array.compareAndSet(index,null,"x");throw new Error();}catch(IndexOutOfBoundsException expected){score=31*score+expected.getClass().getName().hashCode();}
                        try{array.getAndUpdate(index,v->v);throw new Error();}catch(IndexOutOfBoundsException expected){score=31*score+expected.getClass().getName().hashCode();}
                    }
                    try{new AtomicReferenceArray<String>(-1);throw new Error();}catch(NegativeArraySizeException expected){score+=3;}
                    try{new AtomicReferenceArray<String>((String[])null);throw new Error();}catch(NullPointerException expected){score+=5;}
                    try{array.updateAndGet(0,null);throw new Error();}catch(NullPointerException expected){score+=7;}
                    try{array.accumulateAndGet(0,"x",null);throw new Error();}catch(NullPointerException expected){score+=11;}
                    array.set(0,"kept");try{array.getAndUpdate(0,v->{throw new IllegalStateException(v);});throw new Error();}catch(IllegalStateException expected){score+=expected.getMessage().length();}
                    return 31*score+array.get(0).hashCode();
                }
            }
            """);compare(List.of("run","failures"));
    }

    @Test void rejectsUnusedUnlinkedAtomicMembers() {
        rejectsUnused("java/util/concurrent/atomic/AtomicReference","get","()Ljava/lang/Object;",INVOKEVIRTUAL,false);
        rejectsUnused("java/util/concurrent/atomic/AtomicReferenceFieldUpdater","get","(Ljava/lang/Object;)Ljava/lang/Object;",INVOKEVIRTUAL,false);
        rejectsUnused("java/util/concurrent/ConcurrentHashMap","get","(Ljava/lang/Object;)Ljava/lang/Object;",INVOKEVIRTUAL,false);
    }

    private void rejectsUnused(String owner,String name,String descriptor,int opcode,boolean isInterface) {
        ClassWriter writer=new ClassWriter(0);writer.visit(V17,ACC_PUBLIC,"UnusedCall",null,"java/lang/Object",null);writer.visitSource("UnusedCall.java",null);
        MethodVisitor method=writer.visitMethod(ACC_PUBLIC|ACC_STATIC,"unused","()V",null,null);method.visitCode();Label line=new Label();method.visitLabel(line);method.visitLineNumber(41,line);
        method.visitInsn(ACONST_NULL);method.visitInsn(ACONST_NULL);method.visitMethodInsn(opcode,owner,name,descriptor,isInterface);method.visitInsn(RETURN);method.visitMaxs(2,0);method.visitEnd();writer.visitEnd();
        var failure=assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileClassWithExceptionRuntime(writer.toByteArray()));
        assertTrue(failure.getMessage().contains("UnusedCall.java:41"),failure.getMessage());
    }

    private void compare(List<String> names) throws Exception {
        StringBuilder assertions=new StringBuilder();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)){
            Class<?> type=loader.loadClass("Metadata");for(String name:names){Object expected=type.getMethod(name).invoke(null);assertions.append("assert.equal(").append(expected instanceof String?"text(e['Metadata.":"e['Metadata.").append(name).append(expected instanceof String?"']()),":"'](),").append(expected instanceof String?js((String)expected):expected).append(");\n");}
        }
        for(boolean reverse:new boolean[]{false,true})execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),assertions.toString());
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
                const memory=new WebAssembly.Memory({initial:2});let heap=1024,allocations=0,failAllocation=false;
                const e=new WebAssembly.Instance(compiled,{env:{memory,__heap_alloc:size=>{
                    allocations++;if(failAllocation){failAllocation=false;return 0;}
                    const address=heap;heap+=size;
                    while(heap>memory.buffer.byteLength)memory.grow(1);
                    new Uint8Array(memory.buffer,address,size).fill(0x7f);return address;
                }}}).exports;
                assert.equal(allocations,0);assert.equal(e.__gc_heap_bytes?.()??0,0);if(e.__gc_scribble)e.__gc_scribble.value=1;heap=Math.max(heap,e.__exception_heap_base?.value??0);
                function text(pointer){if(!pointer)return null;const view=new DataView(memory.buffer);
                    const chars=view.getInt32(pointer+4,true),length=view.getInt32(chars+4,true);
                    let result='';for(let i=0;i<length;i++)result+=String.fromCharCode(view.getUint16(chars+8+2*i,true));return result;}
            """+assertions+"\n}");
        run("node",script.toString(),output.toString());
    }
}
