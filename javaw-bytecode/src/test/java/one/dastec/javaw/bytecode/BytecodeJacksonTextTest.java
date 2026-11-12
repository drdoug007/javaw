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
import java.util.jar.*;

import static org.junit.jupiter.api.Assertions.*;

class BytecodeJacksonTextTest {
    @TempDir Path directory;
    private String release="17";
    private String fixtureClassName="Metadata";
    private String fixtureClasspath;
    private static final List<String> WHOLE=List.of("com/fasterxml/jackson/core/io/CharTypes",
        "com/fasterxml/jackson/core/io/CharTypes$AltEscapes","com/fasterxml/jackson/core/io/JsonStringEncoder");
    /** Carriers whose unreachable members would pull in IOContext, AtomicReferenceArray or the full numeric parser. */
    private static final List<String> SELECTED=List.of("com/fasterxml/jackson/core/util/TextBuffer","com/fasterxml/jackson/core/util/ByteArrayBuilder",
        "com/fasterxml/jackson/core/util/BufferRecycler","com/fasterxml/jackson/core/io/UTF8Writer");
    private static final String SAMPLES="String[] samples={\"\",\"plain\",\"\\\"\",\"\\\\\",\"a\\nb\\tc\\r\\b\\f\",\"\\u0000\\u001f\\u007f\",\"</script>\",\"\\u00e9\\u2028\\u2029\\uffff\",\"\\ud83d\\ude00\",\"x\".repeat(300),\"\\n\".repeat(200),\"\\u00e9\".repeat(700)};";
    private Path artifact() throws Exception {
        Path path=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()))
            .resolve("com/fasterxml/jackson/core/jackson-core/2.15.4/jackson-core-2.15.4.jar");
        assertEquals("8dc9210dd285db366f45f518dd1e6a9ccfeb0f1a8e184a899fe96d29edf1fd94",java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))));
        return path;
    }
    private void selectedCarriers() throws Exception {
        Map<String,ClassNode> nodes=new LinkedHashMap<>();
        try(JarFile jar=new JarFile(artifact().toFile())) {
            for(String owner:concat(WHOLE,SELECTED)){ClassNode node=new ClassNode();new ClassReader(jar.getInputStream(jar.getJarEntry(owner+".class")).readAllBytes()).accept(node,0);nodes.put(owner,node);}
        }
        // Original bodies are retained; selected carriers keep only the method closure reachable from the encoder and OutputStream overrides.
        Set<String> reachable=new HashSet<>();ArrayDeque<String> pending=new ArrayDeque<>();
        for(var entry:nodes.entrySet())for(MethodNode method:entry.getValue().methods)
            if(WHOLE.contains(entry.getKey()) || method.name.equals("<clinit>") || entry.getKey().endsWith("ByteArrayBuilder") && List.of("write","flush","close").contains(method.name))pending.add(entry.getKey()+"."+method.name+method.desc);
        while(!pending.isEmpty()){String key=pending.pop();if(!reachable.add(key))continue;
            String owner=key.substring(0,key.indexOf('.'));
            for(MethodNode method:nodes.get(owner).methods)if(key.equals(owner+"."+method.name+method.desc))
                for(AbstractInsnNode instruction:method.instructions)if(instruction instanceof MethodInsnNode call && nodes.containsKey(call.owner))pending.add(call.owner+"."+call.name+call.desc);
        }
        Set<String> fields=new HashSet<>();
        for(var entry:nodes.entrySet()){
            if(SELECTED.contains(entry.getKey()))entry.getValue().methods.removeIf(method->!reachable.contains(entry.getKey()+"."+method.name+method.desc));
            for(MethodNode method:entry.getValue().methods)for(AbstractInsnNode instruction:method.instructions)if(instruction instanceof FieldInsnNode field)fields.add(field.owner+"."+field.name);
        }
        for(var entry:nodes.entrySet()){
            if(SELECTED.contains(entry.getKey()))entry.getValue().fields.removeIf(field->!fields.contains(entry.getKey()+"."+field.name));
            ClassWriter writer=new ClassWriter(0);entry.getValue().accept(writer);
            Path file=directory.resolve(entry.getKey()+".class");Files.createDirectories(file.getParent());Files.write(file,writer.toByteArray());
        }
    }
    private static List<String> concat(List<String> left,List<String> right){List<String> result=new ArrayList<>(left);result.addAll(right);return result;}

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

    // CharTypes and JsonStringEncoder differential cases.

    @Test void encodeUtf8() throws Exception {
        fixture("""
            import com.fasterxml.jackson.core.io.JsonStringEncoder;
            public class Metadata {public static int run(){
                int hash=1;JsonStringEncoder encoder=JsonStringEncoder.getInstance();SAMPLES
        for(String text:samples){hash=31*hash+java.util.Arrays.hashCode(encoder.encodeAsUTF8(text));hash=31*hash+java.util.Arrays.hashCode(encoder.encodeAsUTF8(new StringBuilder(text)));}
        for(String text:new String[]{"\\ud800","\\udc00","a\\ud800b","\\ud83d\\ud83d","\\ude00\\ud83d"})try{encoder.encodeAsUTF8(text);throw new Error();}catch(IllegalArgumentException expected){hash=31*hash+expected.getMessage().hashCode();}
        try{encoder.encodeAsUTF8((String)null);throw new Error();}catch(NullPointerException expected){hash++;}return hash;
            }}
            """.replace("SAMPLES",SAMPLES));compare(List.of("run"));
    }

    @Test void quoteUtf8() throws Exception {
        fixture("""
            import com.fasterxml.jackson.core.io.JsonStringEncoder;
            public class Metadata {public static int run(){
                int hash=1;JsonStringEncoder encoder=JsonStringEncoder.getInstance();SAMPLES
        for(String text:samples)hash=31*hash+java.util.Arrays.hashCode(encoder.quoteAsUTF8(text));
        for(String text:new String[]{"\\ud800","\\udc00","a\\ud800b","\\ud83d\\ud83d"})try{encoder.quoteAsUTF8(text);throw new Error();}catch(IllegalArgumentException expected){hash=31*hash+expected.getMessage().hashCode();}
        try{encoder.quoteAsUTF8(null);throw new Error();}catch(NullPointerException expected){hash++;}return hash;
            }}
            """.replace("SAMPLES",SAMPLES));compare(List.of("run"));
    }

    @Test void quoteBuilder() throws Exception {
        fixture("""
            import com.fasterxml.jackson.core.io.JsonStringEncoder;
            public class Metadata {public static int run(){
                int hash=1;JsonStringEncoder encoder=JsonStringEncoder.getInstance();SAMPLES
        StringBuilder all=new StringBuilder("<");for(String text:samples){StringBuilder out=new StringBuilder("[");encoder.quoteAsString(text,out);encoder.quoteAsString(new StringBuilder(text),all);hash=31*hash+out.toString().hashCode();}
        hash=31*hash+all.toString().hashCode();
        try{encoder.quoteAsString("x",null);throw new Error();}catch(NullPointerException expected){hash++;}
        try{encoder.quoteAsString(null,new StringBuilder());throw new Error();}catch(NullPointerException expected){hash++;}return hash;
            }}
            """.replace("SAMPLES",SAMPLES));compare(List.of("run"));
    }

    @Test void quoteSequence() throws Exception {
        fixture("""
            import com.fasterxml.jackson.core.io.JsonStringEncoder;
            public class Metadata {public static int run(){
                int hash=1;JsonStringEncoder encoder=JsonStringEncoder.getInstance();SAMPLES
        for(String text:samples){hash=31*hash+new String(encoder.quoteAsString((CharSequence)new StringBuilder(text))).hashCode();hash=31*hash+new String(encoder.quoteAsString((CharSequence)text)).hashCode();}
        try{encoder.quoteAsString((CharSequence)null);throw new Error();}catch(NullPointerException expected){hash++;}return hash;
            }}
            """.replace("SAMPLES",SAMPLES));compare(List.of("run"));
    }

    @Test void quoteString() throws Exception {
        fixture("""
            import com.fasterxml.jackson.core.io.JsonStringEncoder;
            public class Metadata {public static int run(){
                int hash=1;JsonStringEncoder encoder=JsonStringEncoder.getInstance();SAMPLES
        hash=31*hash+(encoder==JsonStringEncoder.getInstance()?1:0);
        for(String text:samples){char[] quoted=encoder.quoteAsString(text);hash=31*hash+new String(quoted).hashCode();hash=31*hash+quoted.length;}
        hash=31*hash+new String(new JsonStringEncoder().quoteAsString("\\u0001")).hashCode();
        try{encoder.quoteAsString((String)null);throw new Error();}catch(NullPointerException expected){hash++;}return hash;
            }}
            """.replace("SAMPLES",SAMPLES));compare(List.of("run"));
    }

    @Test void appendQuoted() throws Exception {
        fixture("""
            import com.fasterxml.jackson.core.io.CharTypes;
            public class Metadata {public static int run(){
                int hash=1;StringBuilder all=new StringBuilder();for(char ch=0;ch<300;ch++)all.append(ch);
        for(String text:new String[]{"","plain","\\"","\\\\","a\\nb\\tc\\r\\b\\f","\\u0000\\u001f\\u007f","\\u2028\\u2029","\\ud83d\\ude00","\\ud800x\\udc00",all.toString()}){
            StringBuilder out=new StringBuilder("<");CharTypes.appendQuoted(out,text);out.append('>');hash=31*hash+out.toString().hashCode();hash=31*hash+out.length();}
        try{CharTypes.appendQuoted(new StringBuilder(),null);throw new Error();}catch(NullPointerException expected){hash++;}
        try{CharTypes.appendQuoted(null,"\\n");throw new Error();}catch(NullPointerException expected){hash++;}
        CharTypes.appendQuoted(null,"");return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void outputEscapes() throws Exception {
        fixture("""
            import com.fasterxml.jackson.core.io.CharTypes;
            public class Metadata {public static int run(){
                int hash=1;int[] standard=CharTypes.get7BitOutputEscapes();hash=31*hash+java.util.Arrays.hashCode(standard);hash=31*hash+standard.length;
        hash=31*hash+(standard==CharTypes.get7BitOutputEscapes()?1:0);
        for(int quote:new int[]{0,'"','\\'','`','\\\\',127}){int[] alternate=CharTypes.get7BitOutputEscapes(quote);
            hash=31*hash+java.util.Arrays.hashCode(alternate);hash=31*hash+(alternate==standard?1:0);hash=31*hash+(alternate==CharTypes.get7BitOutputEscapes(quote)?1:0);}
        for(int quote:new int[]{128,Integer.MAX_VALUE,-1})try{CharTypes.get7BitOutputEscapes(quote);throw new Error();}catch(ArrayIndexOutOfBoundsException expected){hash++;}
        return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void inputCodes() throws Exception {
        fixture("""
            import com.fasterxml.jackson.core.io.CharTypes;
            public class Metadata {public static int run(){
                int hash=1;for(int[] table:new int[][]{CharTypes.getInputCodeLatin1(),CharTypes.getInputCodeUtf8(),CharTypes.getInputCodeLatin1JsNames(),CharTypes.getInputCodeUtf8JsNames(),CharTypes.getInputCodeComment(),CharTypes.getInputCodeWS()}){hash=31*hash+java.util.Arrays.hashCode(table);hash=31*hash+table.length;}
        hash=31*hash+(CharTypes.getInputCodeUtf8()==CharTypes.getInputCodeUtf8()?1:0);return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void hexDigits() throws Exception {
        fixture("""
            import com.fasterxml.jackson.core.io.CharTypes;
            public class Metadata {public static int run(){
                int hash=1;for(int digit=0;digit<16;digit++)hash=31*hash+CharTypes.hexToChar(digit);
        for(int digit:new int[]{16,-1,Integer.MAX_VALUE})try{CharTypes.hexToChar(digit);throw new Error();}catch(ArrayIndexOutOfBoundsException expected){hash++;}
        char[] chars=CharTypes.copyHexChars();hash=31*hash+new String(chars).hashCode();chars[0]='x';hash=31*hash+new String(CharTypes.copyHexChars()).hashCode();
        hash=31*hash+new String(CharTypes.copyHexChars(true)).hashCode();hash=31*hash+new String(CharTypes.copyHexChars(false)).hashCode();
        byte[] bytes=CharTypes.copyHexBytes();hash=31*hash+java.util.Arrays.hashCode(bytes);bytes[0]=0;hash=31*hash+java.util.Arrays.hashCode(CharTypes.copyHexBytes());
        hash=31*hash+java.util.Arrays.hashCode(CharTypes.copyHexBytes(true));hash=31*hash+java.util.Arrays.hashCode(CharTypes.copyHexBytes(false));
        hash=31*hash+(CharTypes.copyHexChars()!=CharTypes.copyHexChars()?1:0);return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void charToHex() throws Exception {
        fixture("""
            import com.fasterxml.jackson.core.io.CharTypes;
            public class Metadata {public static int run(){
                int hash=1;for(int ch=-2;ch<=260;ch++)hash=31*hash+CharTypes.charToHex(ch);
        for(int ch:new int[]{Integer.MIN_VALUE,Integer.MAX_VALUE,0xff10,0x0660,0x10000})hash=31*hash+CharTypes.charToHex(ch);return hash;
            }}
            """);compare(List.of("run"));
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
                    assert.equal(allocations,0);assert.equal(e.__gc_heap_bytes?.()??0,0);if(e.__gc_scribble)e.__gc_scribble.value=1;heap=Math.max(heap,e.__exception_heap_base?.value??0);return e;
                }
            """+assertions.replace("e['","fresh()['")+"\n}");
        WasmTestSupport.node(script,output);
    }
}
