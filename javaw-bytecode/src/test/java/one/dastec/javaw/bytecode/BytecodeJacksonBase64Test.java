package one.dastec.javaw.bytecode;

import one.dastec.javaw.Wat2Wasm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.tools.ToolProvider;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
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
        return jar();
    }

    private byte[] jar() throws Exception {
        List<Path> files;
        try(var paths=Files.walk(directory)) {
            files=new ArrayList<>(paths.filter(path->path.toString().endsWith(".class")).sorted().toList());
        }
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(JarOutputStream jar=new JarOutputStream(bytes)) {
            for(Path file:files){jar.putNextEntry(new JarEntry(directory.relativize(file).toString()));jar.write(Files.readAllBytes(file));jar.closeEntry();}
        }
        return bytes.toByteArray();
    }

    // Base64Variant differential cases.

    @Test void configuration() throws Exception {
        fixture("""
            import com.fasterxml.jackson.core.*;
            import com.fasterxml.jackson.core.Base64Variant.PaddingReadBehaviour;
            public class Metadata {
                static String describe(Base64Variant v){return v.getName()+":"+v.usesPadding()+v.requiresPaddingOnRead()+v.acceptsPaddingOnRead()+v.paddingReadBehaviour()+":"+(int)v.getPaddingChar()+","+v.getPaddingByte()+","+v.getMaxLineLength()+":"+v.usesPaddingChar('=')+v.usesPaddingChar((int)'=')+v.usesPaddingChar('*')+":"+v+":"+v.hashCode();}
                public static String run(){
                    VARIANTS StringBuilder text=new StringBuilder();
                    for(Base64Variant v:variants){text.append(describe(v)).append('|');
                        Base64Variant allowed=v.withPaddingAllowed(),required=v.withPaddingRequired(),forbidden=v.withPaddingForbidden();
                        text.append(allowed==v).append(required==v).append(forbidden==v).append(v.withReadPadding(v.paddingReadBehaviour())==v).append(v.withWritePadding(v.usesPadding())==v);
                        text.append(describe(required)).append(describe(forbidden)).append(describe(v.withWritePadding(!v.usesPadding())));
                        text.append(v.equals(allowed)).append(v.equals(forbidden)).append(forbidden.equals(v.withPaddingForbidden())).append(v.equals(null)).append(v.equals("x")).append('|');
                    }
                    Base64Variant custom=new Base64Variant("custom","ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_",true,'*',8);
                    Base64Variant renamed=new Base64Variant(Base64Variants.MIME,"renamed",12),changed=new Base64Variant(Base64Variants.PEM,"changed",false,'$',Integer.MAX_VALUE);
                    text.append(describe(custom)).append(describe(renamed)).append(describe(changed)).append(custom.encode(new byte[]{-1,-2,-3,-4,-5,-6,-7,-8})).append('|');
                    text.append(Base64Variants.getDefaultVariant()==Base64Variants.MIME_NO_LINEFEEDS);
                    for(String name:new String[]{"MIME","MIME-NO-LINEFEEDS","PEM","MODIFIED-FOR-URL"})text.append(Base64Variants.valueOf(name).getName()).append(Base64Variants.valueOf(name)==Base64Variants.valueOf(name));
                    for(String name:new String[]{"mime","unknown",""})try{Base64Variants.valueOf(name);text.append("no");}catch(IllegalArgumentException expected){text.append(expected.getMessage());}
                    try{Base64Variants.valueOf(null);text.append("no");}catch(IllegalArgumentException expected){text.append(expected.getMessage());}
                    for(PaddingReadBehaviour behaviour:PaddingReadBehaviour.values())text.append(behaviour.ordinal()).append(behaviour.name()).append(PaddingReadBehaviour.valueOf(behaviour.name())==behaviour);
                    try{new Base64Variant("short","ABC",true,'=',76);text.append("no");}catch(IllegalArgumentException expected){text.append(expected.getMessage());}
                    return text.toString();
                }
            }
            """.replace("VARIANTS",VARIANTS));compareText(List.of("run"));
    }

    @Test void decodingFailures() throws Exception {
        fixture("""
            import com.fasterxml.jackson.core.*;
            import com.fasterxml.jackson.core.util.ByteArrayBuilder;
            public class Metadata {public static String run(){
                VARIANTS StringBuilder text=new StringBuilder();
                String[] inputs={"A","AB","ABC","ABCD","AB==","ABC=","AB=","A===","AB=x","AB==AB","AB$C","\\u00e9AAA","ABCD\\n","  ABCD","AB CD","AB\\tCD==","=","==","AB==\\n","AB==  ","QUJD\\r\\nREVG","QUJDREVGRw","QUJDREVGRw=","QUJDREVGRw==","QUJDREVGRw==="};
                for(Base64Variant base:variants)for(Base64Variant v:new Base64Variant[]{base,base.withPaddingAllowed(),base.withPaddingRequired(),base.withPaddingForbidden()})for(String input:inputs){
                    try{byte[] bytes=v.decode(input);text.append(java.util.Arrays.toString(bytes));}catch(IllegalArgumentException failure){text.append(failure.getMessage());}
                    text.append('|');
                }
                for(Base64Variant v:variants)text.append(v.missingPaddingMessage()).append('|');
                try{Base64Variants.MIME.decode(null);text.append("no");}catch(NullPointerException expected){text.append("npe");}
                try{Base64Variants.MIME.decode("AAAA",null);text.append("no");}catch(NullPointerException expected){text.append("npe");}
                ByteArrayBuilder builder=new ByteArrayBuilder();try{Base64Variants.MIME.decode("QUJD$",builder);}catch(IllegalArgumentException expected){text.append(builder.size()).append(java.util.Arrays.toString(builder.toByteArray()));}
                return text.toString();
            }}
            """.replace("VARIANTS",VARIANTS));compareText(List.of("run"));
    }

    @Test void decoding() throws Exception {
        fixture("""
            import com.fasterxml.jackson.core.*;
            import com.fasterxml.jackson.core.util.ByteArrayBuilder;
            public class Metadata {public static int run(){
                VARIANTS int hash=1;
                for(Base64Variant v:variants){
                    for(int c=0;c<=256;c++)hash=31*hash+v.decodeBase64Char(c);
                    for(int c:new int[]{-1,Integer.MIN_VALUE})try{v.decodeBase64Char(c);throw new Error();}catch(IndexOutOfBoundsException failure){hash=31*hash+failure.getClass().getName().hashCode();}
                    for(char c:new char[]{'A','z','+','/','-','_','=','\\u00ff','\\u0100','\\uffff'})hash=31*hash+v.decodeBase64Char(c);
                    for(int b=-128;b<128;b++)hash=31*hash+v.decodeBase64Byte((byte)b);
                    for(int length=0;length<=80;length+=length<12?1:17){
                        byte[] data=new byte[length];for(int i=0;i<length;i++)data[i]=(byte)(i*37+length);
                        // encode(byte[]) uses the JSON-escaped "\\\\n" linefeed, so long MIME output is not decodable as-is.
                        try{byte[] decoded=v.decode(v.encode(data));hash=31*hash+java.util.Arrays.hashCode(decoded);}catch(IllegalArgumentException failure){hash=31*hash+failure.getMessage().hashCode();}
                        String encoded=v.encode(data,false,"\\n");byte[] decoded=v.decode(encoded);hash=31*hash+java.util.Arrays.hashCode(decoded)+(java.util.Arrays.equals(data,decoded)?1:0);
                        ByteArrayBuilder builder=new ByteArrayBuilder();builder.append(9);v.decode(encoded,builder);hash=31*hash+java.util.Arrays.hashCode(builder.toByteArray());
                        if(v.usesPadding()){try{hash=31*hash+java.util.Arrays.hashCode(v.withPaddingForbidden().decode(encoded.replace("=","")));}catch(IllegalArgumentException failure){hash=31*hash+failure.getMessage().hashCode();}}
                        else{try{hash=31*hash+java.util.Arrays.hashCode(v.withPaddingRequired().decode(encoded));}catch(IllegalArgumentException failure){hash=31*hash+failure.getMessage().hashCode();}}
                    }
                }
                byte[] all=new byte[256];for(int i=0;i<256;i++)all[i]=(byte)i;
                for(Base64Variant v:variants)hash=31*hash+java.util.Arrays.hashCode(v.decode(v.encode(all,false,"\\n")));
                return hash;
            }}
            """.replace("VARIANTS",VARIANTS));compare(List.of("run"));
    }

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
        execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar())),assertions.toString());
    }

    private void compareText(List<String> names) throws Exception {
        StringBuilder assertions=new StringBuilder();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL(),artifact().toUri().toURL()},null)){
            Class<?> type=loader.loadClass(fixtureClassName);
            for(String name:names){Object expected=type.getMethod(name).invoke(null);assertInstanceOf(String.class,expected);
                assertions.append("assert.equal(text(e['").append(fixtureClassName).append(".").append(name).append("']()),").append(js((String)expected)).append(");\n");}
        }
        selectedCarriers();
        execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar())),assertions.toString());
    }
    private String js(String value) {
        StringBuilder out=new StringBuilder("\"");
        for(char c:value.toCharArray())out.append(String.format("\\u%04x",(int)c));
        return out.append('"').toString();
    }

    private void execute(String wat,String assertions) throws Exception {
        Path input=directory.resolve("metadata.wat"),output=directory.resolve("metadata.wasm"),script=directory.resolve("run.cjs");
        Files.writeString(input,wat);WasmTestSupport.assemble(input,output,Wat2Wasm.EXCEPTIONS);
        Files.writeString(script,"""
            const fs=require('node:fs'),assert=require('node:assert/strict');
            const compiled=new WebAssembly.Module(fs.readFileSync(process.argv[2]));
            for(let instance=0;instance<2;instance++) {
                function fresh(){
                    const memory=new WebAssembly.Memory({initial:2});let heap=1024,allocations=0;
                    const e=new WebAssembly.Instance(compiled,{env:{memory,__heap_alloc:size=>{
                        allocations++;const address=heap;heap+=size;
                        while(heap>memory.buffer.byteLength)memory.grow(1);
                        new Uint8Array(memory.buffer,address,size).fill(0x7f);return address;
                    }}}).exports;
                    assert.equal(allocations,0);assert.equal(e.__gc_heap_bytes?.()??0,0);if(e.__gc_scribble)e.__gc_scribble.value=1;heap=Math.max(heap,e.__exception_heap_base?.value??0);current=memory;return e;
                }
                let current;
                function text(pointer){if(!pointer)return null;const view=new DataView(current.buffer);
                    const chars=view.getInt32(pointer+4,true),length=view.getInt32(chars+4,true);
                    let result='';for(let i=0;i<length;i++)result+=String.fromCharCode(view.getUint16(chars+8+2*i,true));return result;}
            """+assertions.replace("e['","fresh()['")+"\n}");
        WasmTestSupport.node(script,output);
    }
}
