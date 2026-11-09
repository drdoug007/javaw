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

class BytecodeArrayBoundsMessageTest implements Opcodes {
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

    @Test void matchesImplicitElementAccessBoundsMessages() throws Exception {
        fixture("""
            public class Metadata {
                interface Access{void run(int index);}
                static int score(int hash,Access access,int index){
                    try{access.run(index);return 31*hash;}
                    catch(IndexOutOfBoundsException failure){return 31*hash+failure.getClass().getName().hashCode()+31*String.valueOf(failure.getMessage()).hashCode();}
                }
                static int[] indexes(){return new int[]{-1,0,1,2,3,Integer.MIN_VALUE,Integer.MAX_VALUE};}
                public static int loads(){
                    boolean[] z=new boolean[2];byte[] b=new byte[2];char[] c=new char[2];short[] s=new short[2];int[] i=new int[2];long[] j=new long[2];float[] f=new float[2];double[] d=new double[2];Object[] o=new Object[2];String[] t=new String[2];int[][] n=new int[2][];
                    int hash=1;for(int index:indexes()){
                        hash=score(hash,k->{boolean v=z[k];},index);hash=score(hash,k->{byte v=b[k];},index);hash=score(hash,k->{char v=c[k];},index);hash=score(hash,k->{short v=s[k];},index);
                        hash=score(hash,k->{int v=i[k];},index);hash=score(hash,k->{long v=j[k];},index);hash=score(hash,k->{float v=f[k];},index);hash=score(hash,k->{double v=d[k];},index);
                        hash=score(hash,k->{Object v=o[k];},index);hash=score(hash,k->{String v=t[k];},index);hash=score(hash,k->{int[] v=n[k];},index);
                    }
                    return hash;
                }
                public static int stores(){
                    boolean[] z=new boolean[3];byte[] b=new byte[3];char[] c=new char[3];short[] s=new short[3];int[] i=new int[3];long[] j=new long[3];float[] f=new float[3];double[] d=new double[3];Object[] o=new Object[3];Object[] covariant=new String[3];
                    int hash=1;for(int index:indexes()){
                        hash=score(hash,k->z[k]=true,index);hash=score(hash,k->b[k]=1,index);hash=score(hash,k->c[k]='x',index);hash=score(hash,k->s[k]=2,index);
                        hash=score(hash,k->i[k]=3,index);hash=score(hash,k->j[k]=4,index);hash=score(hash,k->f[k]=5,index);hash=score(hash,k->d[k]=6,index);
                        hash=score(hash,k->o[k]="o",index);hash=score(hash,k->covariant[k]="s",index);hash=score(hash,k->covariant[k]=null,index);
                        hash=score(hash,k->i[k]++,index);
                    }
                    return hash+i[0]+i[2];
                }
                public static int empty(){
                    int hash=1;int[] none=new int[0];Object[] nothing=new Object[0];long[][] grid=new long[2][3];
                    for(int index:new int[]{0,-1,5}){hash=score(hash,k->{int v=none[k];},index);hash=score(hash,k->nothing[k]=null,index);hash=score(hash,k->grid[1][k]=1,index);hash=score(hash,k->{long[] row=grid[k];},index);}
                    return hash;
                }
                public static String message(){
                    String text="";int[] values=new int[4];
                    try{values[4]=1;}catch(ArrayIndexOutOfBoundsException failure){text+=failure.getMessage()+"|";}
                    int[][] sizes={{-7,2},{3,-1},{-2,-3},{0,-4},{Integer.MIN_VALUE,0}};
                    for(int[] size:sizes){
                        try{Object v=new Object[size[0]][size[1]];}catch(RuntimeException failure){text+=failure.getClass().getName()+":"+failure.getMessage()+"|";}
                        try{Object v=new long[size[1]][size[0]][1];}catch(RuntimeException failure){text+=failure.getMessage()+"|";}
                    }
                    for(int length:new int[]{-1,Integer.MIN_VALUE}){
                        try{Object v=new byte[length];}catch(NegativeArraySizeException failure){text+=failure.getMessage()+"|";}
                        try{Object v=new String[length];}catch(NegativeArraySizeException failure){text+=failure.getMessage()+"|";}
                        try{Object v=new double[length][];}catch(NegativeArraySizeException failure){text+=failure.getMessage()+"|";}
                    }
                    try{char v="abc".toCharArray()[3];}catch(IndexOutOfBoundsException failure){text+=failure.getMessage()+"|";}
                    try{throw new ArrayIndexOutOfBoundsException(-9);}catch(ArrayIndexOutOfBoundsException failure){text+=failure.getMessage()+"|";}
                    text+=new IndexOutOfBoundsException(7).getMessage()+"|"+new IndexOutOfBoundsException(1L<<40).getMessage()+"|"+new ArrayIndexOutOfBoundsException().getMessage();
                    return text;
                }
            }
            """);compare(List.of("loads","stores","empty","message"));
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
