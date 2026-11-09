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

class BytecodeIoSinkTest implements Opcodes {
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

    @Test void matchesByteAndCharacterSinkBaseClasses() throws Exception {
        fixture("""
            import java.io.*;
            public class Metadata {
                static final class Bytes extends OutputStream {
                    final StringBuilder log=new StringBuilder();int flushes,closes;
                    public void write(int value){log.append(value).append(',');}
                    public void flush(){flushes++;}public void close(){closes++;}
                }
                static final class Chars extends Writer {
                    final StringBuilder log=new StringBuilder();int calls;
                    Chars(){}Chars(Object lock){super(lock);}
                    Object lock(){return lock;}
                    public void write(char[] chars,int offset,int length){calls++;log.append('[').append(chars,offset,length).append(']');}
                    public void flush(){log.append("F");}public void close(){log.append("C");}
                }
                static final class Plain extends OutputStream {public void write(int value){}}
                public static String bytes() throws IOException {
                    Bytes out=new Bytes();out.write(65);out.write(-1);out.write(300);out.write(new byte[]{1,-2,3});out.write(new byte[]{9,8,7,6},1,2);out.write(new byte[0],0,0);out.flush();out.close();
                    Plain plain=new Plain();plain.flush();plain.close();
                    OutputStream view=out;Closeable closeable=out;Flushable flushable=out;AutoCloseable auto=out;closeable.close();flushable.flush();
                    return out.log+"|"+out.flushes+"|"+out.closes+"|"+(view instanceof Closeable)+(auto instanceof Flushable);
                }
                public static String chars() throws IOException {
                    Chars out=new Chars();out.write('a');out.write(0x1f600);out.write(new char[]{'b','c'});out.write("hello",1,3);out.write("");out.write("x".repeat(1500));
                    Writer chained=out.append("cs").append(new StringBuilder("sequence"),2,5).append('!').append(null).append(null,1,3);
                    Appendable appendable=out;appendable.append('?');out.flush();out.close();
                    Object lock=new Object();Chars locked=new Chars(lock);
                    return out.log.toString().replace("x".repeat(1500),"<1500>")+"|"+out.calls+"|"+(chained==out)+"|"+(out.lock()==out)+(locked.lock()==lock)+"|"+(out instanceof Closeable)+(out instanceof Flushable);
                }
                public static int failures() throws IOException {
                    int score=0;Bytes out=new Bytes();
                    for(int[] range:new int[][]{{-1,1},{0,-1},{2,2},{4,1},{Integer.MAX_VALUE,1},{1,Integer.MAX_VALUE}})try{out.write(new byte[3],range[0],range[1]);throw new Error();}catch(IndexOutOfBoundsException expected){score=31*score+expected.getMessage().hashCode();}
                    try{out.write((byte[])null);throw new Error();}catch(NullPointerException expected){score+=3;}
                    try{out.write(null,0,0);throw new Error();}catch(NullPointerException expected){score+=5;}
                    Chars chars=new Chars();
                    try{chars.write((String)null);throw new Error();}catch(NullPointerException expected){score+=7;}
                    try{chars.write((char[])null);throw new Error();}catch(NullPointerException expected){score+=11;}
                    try{chars.write("abc",2,2);throw new Error();}catch(IndexOutOfBoundsException expected){score+=13;}
                    try{chars.append("abc",2,1);throw new Error();}catch(IndexOutOfBoundsException expected){score+=17;}
                    try{new Chars(null);throw new Error();}catch(NullPointerException expected){score+=19;}
                    return score+chars.log.length();
                }
            }
            """);compare(List.of("bytes","chars","failures"));
    }

    @Test void rejectsUnusedUnlinkedSinkMembers() {
        rejectsUnused("java/io/OutputStream","nullOutputStream","()Ljava/io/OutputStream;",INVOKESTATIC,false);
        rejectsUnused("java/io/Writer","nullWriter","()Ljava/io/Writer;",INVOKESTATIC,false);
        rejectsUnused("java/io/PrintWriter","flush","()V",INVOKEVIRTUAL,false);
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
        Files.writeString(input,wat);run(Wat2Wasm.command(input,output,Wat2Wasm.EXCEPTIONS).toArray(String[]::new));
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
