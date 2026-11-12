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

class BytecodeJacksonCharacterEscapesTest {
    @TempDir Path directory;
    private String release="17";
    private String fixtureClassName="Metadata";
    private String fixtureClasspath;
    private static final List<String> WHOLE=List.of("com/fasterxml/jackson/core/io/CharacterEscapes","com/fasterxml/jackson/core/JsonpCharacterEscapes",
        "com/fasterxml/jackson/core/io/CharTypes","com/fasterxml/jackson/core/io/CharTypes$AltEscapes","com/fasterxml/jackson/core/io/JsonStringEncoder",
        "com/fasterxml/jackson/core/util/ByteArrayBuilder","com/fasterxml/jackson/core/util/BufferRecycler");
    /** ByteBuffer and object-stream serialization are not linked; only members whose descriptors use them are removed. */
    private static final List<String> UNLINKED_MEMBERS=List.of("com/fasterxml/jackson/core/SerializableString","com/fasterxml/jackson/core/io/SerializedString");
    /** Linking these whole would pull in IOContext and the full numeric parser; keep the closure reachable from retained members. */
    private static final List<String> REACHABLE=List.of("com/fasterxml/jackson/core/util/TextBuffer","com/fasterxml/jackson/core/io/UTF8Writer");
    private Path artifact() throws Exception {
        Path path=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()))
            .resolve("com/fasterxml/jackson/core/jackson-core/2.15.4/jackson-core-2.15.4.jar");
        assertEquals("8dc9210dd285db366f45f518dd1e6a9ccfeb0f1a8e184a899fe96d29edf1fd94",java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))));
        return path;
    }
    private void selectedCarriers() throws Exception {
        Map<String,ClassNode> nodes=new LinkedHashMap<>();
        try(JarFile jar=new JarFile(artifact().toFile())) {
            for(List<String> group:List.of(WHOLE,UNLINKED_MEMBERS,REACHABLE))for(String owner:group){ClassNode node=new ClassNode();new ClassReader(jar.getInputStream(jar.getJarEntry(owner+".class")).readAllBytes()).accept(node,0);nodes.put(owner,node);}
        }
        for(String owner:UNLINKED_MEMBERS)nodes.get(owner).methods.removeIf(method->method.desc.contains("Ljava/nio/") || method.desc.contains("Ljava/io/Object"));
        Set<String> reachable=new HashSet<>();ArrayDeque<String> pending=new ArrayDeque<>();
        for(var entry:nodes.entrySet())for(MethodNode method:entry.getValue().methods)
            if(!REACHABLE.contains(entry.getKey()) || method.name.equals("<clinit>"))pending.add(entry.getKey()+"."+method.name+method.desc);
        while(!pending.isEmpty()){String key=pending.pop();if(!reachable.add(key))continue;
            String owner=key.substring(0,key.indexOf('.'));
            for(MethodNode method:nodes.get(owner).methods)if(key.equals(owner+"."+method.name+method.desc))
                for(AbstractInsnNode instruction:method.instructions)if(instruction instanceof MethodInsnNode call && nodes.containsKey(call.owner))pending.add(call.owner+"."+call.name+call.desc);
        }
        Set<String> fields=new HashSet<>();
        for(var entry:nodes.entrySet()){
            if(REACHABLE.contains(entry.getKey()))entry.getValue().methods.removeIf(method->!reachable.contains(entry.getKey()+"."+method.name+method.desc));
            for(MethodNode method:entry.getValue().methods)for(AbstractInsnNode instruction:method.instructions)if(instruction instanceof FieldInsnNode field)fields.add(field.owner+"."+field.name);
        }
        for(var entry:nodes.entrySet()){
            if(REACHABLE.contains(entry.getKey()))entry.getValue().fields.removeIf(field->!fields.contains(entry.getKey()+"."+field.name));
            ClassWriter writer=new ClassWriter(0);entry.getValue().accept(writer);
            Path file=directory.resolve(entry.getKey()+".class");Files.createDirectories(file.getParent());Files.write(file,writer.toByteArray());
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

    // CharacterEscapes, JsonpCharacterEscapes and SerializedString differential cases.

    @Test void serializedStrings() throws Exception {
        fixture("""
            import com.fasterxml.jackson.core.SerializableString;
            import com.fasterxml.jackson.core.io.SerializedString;
            public class Metadata {
                static final class Sink extends java.io.OutputStream{int hash=1,count;public void write(int b){hash=31*hash+b;count++;}}
                public static int run(){
                    int hash=1;
                    for(String value:new String[]{"","plain","quote\\"back\\\\slash","line\\nbreak\\u0001","\\u00e9\\u2028\\ud83d\\ude00","x".repeat(150)}){
                        SerializedString s=new SerializedString(value);SerializableString view=s;
                        hash=31*hash+view.getValue().hashCode()+view.charLength();hash=31*hash+new String(s.asQuotedChars()).hashCode();hash=31*hash+(s.asQuotedChars()==s.asQuotedChars()?1:0);
                        hash=31*hash+java.util.Arrays.hashCode(s.asQuotedUTF8())+(s.asQuotedUTF8()==s.asQuotedUTF8()?1:0);hash=31*hash+java.util.Arrays.hashCode(s.asUnquotedUTF8())+(s.asUnquotedUTF8()==s.asUnquotedUTF8()?1:0);
                        for(int size:new int[]{0,4,400}){char[] chars=new char[size];byte[] bytes=new byte[size];
                            hash=31*hash+s.appendQuoted(chars,1>size?0:1);hash=31*hash+s.appendUnquoted(chars,0);hash=31*hash+new String(chars).hashCode();
                            hash=31*hash+s.appendQuotedUTF8(bytes,0);hash=31*hash+s.appendUnquotedUTF8(bytes,size>2?2:0);hash=31*hash+java.util.Arrays.hashCode(bytes);}
                        try{Sink quoted=new Sink(),unquoted=new Sink();hash=31*hash+s.writeQuotedUTF8(quoted)+s.writeUnquotedUTF8(unquoted);hash=31*hash+quoted.hash+quoted.count;hash=31*hash+unquoted.hash+unquoted.count;}catch(java.io.IOException impossible){throw new Error(impossible);}
                        hash=31*hash+s.toString().hashCode()+s.hashCode();hash=31*hash+(s.equals(new SerializedString(value))?1:0)+(s.equals(value)?2:0)+(s.equals(null)?4:0);
                    }
                    try{new SerializedString(null);throw new Error();}catch(IllegalStateException expected){hash=31*hash+expected.getMessage().hashCode();}
                    return hash;
                }
            }
            """);compare(List.of("run"));
    }

    @Test void jsonpEscapes() throws Exception {
        fixture("""
            import com.fasterxml.jackson.core.*;
            import com.fasterxml.jackson.core.io.CharacterEscapes;
            public class Metadata {public static String run(){
                JsonpCharacterEscapes escapes=JsonpCharacterEscapes.instance();StringBuilder text=new StringBuilder();
                text.append(escapes==JsonpCharacterEscapes.instance()).append(escapes instanceof CharacterEscapes).append(new JsonpCharacterEscapes()!=escapes).append('|');
                int[] codes=escapes.getEscapeCodesForAscii();text.append(java.util.Arrays.toString(codes)).append(codes==escapes.getEscapeCodesForAscii()).append('|');
                for(int ch:new int[]{0,'a','"','\\\\',0x7f,0x2027,0x2028,0x2029,0x202a,0xffff,0x10000,-1}){SerializableString escape=escapes.getEscapeSequence(ch);
                    text.append(ch).append('=').append(escape==null?"null":escape.getValue()+":"+escape.charLength()+":"+new String(escape.asQuotedChars())+":"+(escape==escapes.getEscapeSequence(ch))).append(',');}
                return text.toString();
            }}
            """);compareText(List.of("run"));
    }

    @Test void customEscapes() throws Exception {
        fixture("""
            import com.fasterxml.jackson.core.SerializableString;
            import com.fasterxml.jackson.core.io.*;
            public class Metadata {
                static final class Custom extends CharacterEscapes {
                    private final int[] codes;Custom(){codes=standardAsciiEscapesForJSON();codes['<']=ESCAPE_CUSTOM;codes['/']=ESCAPE_STANDARD;codes['\\t']=ESCAPE_NONE;}
                    public int[] getEscapeCodesForAscii(){return codes;}
                    public SerializableString getEscapeSequence(int ch){return ch=='<'?new SerializedString("\\\\u003C"):null;}
                }
                public static String run(){
                    StringBuilder text=new StringBuilder();text.append(CharacterEscapes.ESCAPE_NONE).append(CharacterEscapes.ESCAPE_STANDARD).append(CharacterEscapes.ESCAPE_CUSTOM).append('|');
                    int[] standard=CharacterEscapes.standardAsciiEscapesForJSON();text.append(standard.length).append(java.util.Arrays.toString(standard)).append(standard!=CharacterEscapes.standardAsciiEscapesForJSON()).append('|');
                    standard[0]=99;text.append(CharacterEscapes.standardAsciiEscapesForJSON()[0]).append(CharTypes.get7BitOutputEscapes()[0]).append('|');
                    Custom custom=new Custom();CharacterEscapes view=custom;int[] codes=view.getEscapeCodesForAscii();
                    text.append(codes['<']).append(codes['/']).append(codes['\\t']).append(codes['a']).append(view.getEscapeSequence('<').getValue()).append(view.getEscapeSequence('a')).append(custom instanceof java.io.Serializable);
                    return text.toString();
                }
            }
            """);compareText(List.of("run"));
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
