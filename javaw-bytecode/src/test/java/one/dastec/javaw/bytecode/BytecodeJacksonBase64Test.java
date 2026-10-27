package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.tools.ToolProvider;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.jar.*;

import static org.junit.jupiter.api.Assertions.*;

class BytecodeJacksonBase64Test {
    @TempDir Path directory;
    private String release="17";
    private String fixtureClassName="Metadata";
    private String fixtureClasspath;
    private static final String VARIANTS="Base64Variant[] variants={Base64Variants.MIME,Base64Variants.MIME_NO_LINEFEEDS,Base64Variants.PEM,Base64Variants.MODIFIED_FOR_URL};";
    private Path artifact() throws Exception {
        Path path=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()))
            .resolve("com/fasterxml/jackson/core/jackson-core/2.15.4/jackson-core-2.15.4.jar");
        assertEquals("8dc9210dd285db366f45f518dd1e6a9ccfeb0f1a8e184a899fe96d29edf1fd94",java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))));
        return path;
    }
    private void selectedCarriers() throws Exception {
        try(JarFile jar=new JarFile(artifact().toFile())) {
            for(String owner:List.of("com/fasterxml/jackson/core/Base64Variant","com/fasterxml/jackson/core/Base64Variant$PaddingReadBehaviour","com/fasterxml/jackson/core/Base64Variants",
                    "com/fasterxml/jackson/core/util/BufferRecycler","com/fasterxml/jackson/core/util/ByteArrayBuilder")) {
                Path file=directory.resolve(owner+".class");
                Files.createDirectories(file.getParent());
                Files.write(file,jar.getInputStream(jar.getJarEntry(owner+".class")).readAllBytes());
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

    // Base64Variant differential cases.

    @Test void encodingPieces() throws Exception {
        fixture("""
            import com.fasterxml.jackson.core.*;
            public class Metadata {public static int run(){
                VARIANTS int hash=1;
                for(Base64Variant v:variants){
                    for(int bits=0;bits<64;bits++){hash=31*hash+v.encodeBase64BitsAsChar(bits);hash=31*hash+v.encodeBase64BitsAsByte(bits);}
                    for(int bits:new int[]{0,0xffffff,0x123456,0x800001,-1,0x7fffffff}){
                        char[] chars=new char[8];java.util.Arrays.fill(chars,'#');hash=31*hash+v.encodeBase64Chunk(bits,chars,2);hash=31*hash+new String(chars).hashCode();
                        byte[] bytes=new byte[8];java.util.Arrays.fill(bytes,(byte)'#');hash=31*hash+v.encodeBase64Chunk(bits,bytes,1);hash=31*hash+java.util.Arrays.hashCode(bytes);
                        StringBuilder builder=new StringBuilder("<");v.encodeBase64Chunk(builder,bits);hash=31*hash+builder.toString().hashCode();
                        for(int count=1;count<=2;count++){
                            char[] partialChars=new char[6];hash=31*hash+v.encodeBase64Partial(bits,count,partialChars,1);hash=31*hash+new String(partialChars).hashCode();
                            byte[] partialBytes=new byte[6];hash=31*hash+v.encodeBase64Partial(bits,count,partialBytes,0);hash=31*hash+java.util.Arrays.hashCode(partialBytes);
                            StringBuilder partial=new StringBuilder();v.encodeBase64Partial(partial,bits,count);hash=31*hash+partial.toString().hashCode();
                        }
                    }
                    for(int bits:new int[]{-1,64})try{v.encodeBase64BitsAsChar(bits);hash=31*hash+1;}catch(IndexOutOfBoundsException failure){hash=31*hash+failure.getClass().getName().hashCode();}
                    try{v.encodeBase64Chunk(0,new char[3],0);throw new Error();}catch(IndexOutOfBoundsException failure){hash=31*hash+failure.getClass().getName().hashCode();}
                    try{v.encodeBase64Chunk(0,(byte[])null,0);throw new Error();}catch(NullPointerException failure){hash+=3;}
                }
                return hash;
            }}
            """.replace("VARIANTS",VARIANTS));compare(List.of("run"));
    }

    @Test void encoding() throws Exception {
        fixture("""
            import com.fasterxml.jackson.core.*;
            public class Metadata {public static int run(){
                VARIANTS int hash=1;
                byte[] all=new byte[256];for(int i=0;i<256;i++)all[i]=(byte)i;
                for(Base64Variant v:variants){
                    for(int length=0;length<=80;length++){byte[] data=java.util.Arrays.copyOf(all,length);
                        hash=31*hash+v.encode(data).hashCode();hash=31*hash+v.encode(data,true).hashCode();hash=31*hash+v.encode(data,false,"\\r\\n").hashCode();}
                    byte[] large=new byte[700];for(int i=0;i<large.length;i++)large[i]=(byte)(i*i);
                    hash=31*hash+v.encode(large).hashCode();hash=31*hash+v.encode(large,true,"\\n").hashCode();hash=31*hash+v.encode(large,false,"").hashCode();hash=31*hash+v.encode(all,true,"--").hashCode();
                    try{v.encode(null);throw new Error();}catch(NullPointerException expected){hash+=3;}
                    try{hash=31*hash+v.encode(all,false,null).hashCode();}catch(NullPointerException expected){hash+=5;}
                }
                return hash;
            }}
            """.replace("VARIANTS",VARIANTS));compare(List.of("run"));
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

    private void compareText(List<String> names) throws Exception {
        StringBuilder assertions=new StringBuilder();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL(),artifact().toUri().toURL()},null)){
            Class<?> type=loader.loadClass(fixtureClassName);
            for(String name:names){Object expected=type.getMethod(name).invoke(null);assertInstanceOf(String.class,expected);
                assertions.append("assert.equal(text(e['").append(fixtureClassName).append(".").append(name).append("']()),").append(js((String)expected)).append(");\n");}
        }
        selectedCarriers();
        for(boolean reverse:new boolean[]{false,true})execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),assertions.toString());
    }
    private String js(String value) {
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
                function fresh(){
                    const memory=new WebAssembly.Memory({initial:2,maximum:20});let heap=1024,allocations=0;
                    const e=new WebAssembly.Instance(compiled,{env:{memory,__heap_alloc:size=>{
                        allocations++;const address=heap;heap+=size;
                        while(heap>memory.buffer.byteLength)memory.grow(1);
                        new Uint8Array(memory.buffer,address,size).fill(0x7f);return address;
                    }}}).exports;
                    assert.equal(allocations,0);heap=Math.max(heap,e.__exception_heap_base?.value??0);current=memory;return e;
                }
                let current;
                function text(pointer){if(!pointer)return null;const view=new DataView(current.buffer);
                    const chars=view.getInt32(pointer+4,true),length=view.getInt32(chars+4,true);
                    let result='';for(let i=0;i<length;i++)result+=String.fromCharCode(view.getUint16(chars+8+2*i,true));return result;}
            """+assertions.replace("e['","fresh()['")+"\n}");
        run("node",script.toString(),output.toString());
    }
}
