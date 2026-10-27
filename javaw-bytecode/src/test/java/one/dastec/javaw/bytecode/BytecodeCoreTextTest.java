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

class BytecodeCoreTextTest implements Opcodes {
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

    @Test void matchesUtf16StringsAndMutableBuilders() throws Exception {
        fixture("""
            public class Metadata {
                public static String run(){
                    StringBuilder b=new StringBuilder(0);b.append("a").append((String)null).append(':').append(-2147483648).append('/').append(-9223372036854775808L);
                    b.append(new char[]{'x','y','z'},1,2).append((CharSequence)b,0,3);String snapshot=b.toString();
                    b.insert(1,"INSERT").delete(2,5).deleteCharAt(0).setCharAt(0,'Q');b.setLength(b.length()+2);b.setLength(b.length()-1);
                    b.append(true).append(1.25f).append(-0.0d);b.trimToSize();
                    return snapshot+"|"+b.toString()+"|"+new StringBuilder("x😀y").reverse()+"|"+b.capacity()+"|"+b.substring(1,4);
                }
                public static int strings(){
                    String s=new String(new char[]{'a','b','c','d'},1,2);char[] copy=s.toCharArray();copy[0]='z';char[] target=new char[5];s.getChars(0,2,target,2);
                    int score=s.equals("bc") && target[2]=='b'?1:0;
                    if(" abc ".trim().equals("abc") && "hello".substring(1,4).equals("ell"))score+=10;
                    if("abc".startsWith("bc",1) && "abc".endsWith("bc") && "abc".contains(new StringBuilder("b")))score+=100;
                    if("abc".indexOf("",99)==3 && "x😀y".indexOf(128512)==1 && "abc".indexOf('b',-1)==1)score+=1000;
                    if("abc".replace('b','x').equals("axc") && "abc".compareTo("abd")==-1 && s.subSequence(0,1).toString().equals("b"))score+=10000;
                    return score;
                }
                public static int failures(){
                    int score=0;try{"x".charAt(1);}catch(StringIndexOutOfBoundsException e){score++;}
                    try{"x".substring(-1);}catch(StringIndexOutOfBoundsException e){score+=10;}
                    try{new StringBuilder(-1);}catch(NegativeArraySizeException e){score+=100;}
                    StringBuilder b=new StringBuilder("abc");try{b.append(new char[]{'x'},0,2);}catch(IndexOutOfBoundsException e){if(b.toString().equals("abc"))score+=1000;}
                    try{b.setLength(-1);}catch(StringIndexOutOfBoundsException e){score+=10000;}
                    try{b.append((char[])null);}catch(NullPointerException e){score+=100000;}
                    try{b.append("abc",2,1);}catch(IndexOutOfBoundsException e){score+=1000000;}
                    return score;
                }
            }
            """);
        compare(List.of("run","strings","failures"));
    }

    @Test void executesUnchangedPinnedLogbackJsonEscaping() throws Exception {
        Path repository=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()));
        Path artifact=repository.resolve("ch/qos/logback/logback-core/1.4.14/logback-core-1.4.14.jar");
        assertEquals("f8c2f05f42530b1852739507c1792f0080167850ed8f396444c6913d6617a293",java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(artifact))));
        fixtureClasspath=artifact.toString();
        fixture("""
            import ch.qos.logback.core.encoder.JsonEscapeUtil;
            public class Metadata {
                public static String run(){char[] chars=new char[132];for(int i=0;i<128;i++)chars[i]=(char)i;chars[128]='é';chars[129]='中';chars[130]=(char)0xd83d;chars[131]=(char)0xde00;return JsonEscapeUtil.jsonEscapeString(new String(chars));}
                public static String empty(){return JsonEscapeUtil.jsonEscapeString("");}
                public static int failures(){try{JsonEscapeUtil.jsonEscapeString(null);return 0;}catch(NullPointerException expected){return 1;}}
            }
            """);
        Path selected=directory.resolve("ch/qos/logback/core/encoder/JsonEscapeUtil.class");Files.createDirectories(selected.getParent());
        byte[] original;try(JarFile jar=new JarFile(artifact.toFile())){original=jar.getInputStream(jar.getJarEntry("ch/qos/logback/core/encoder/JsonEscapeUtil.class")).readAllBytes();}
        Files.write(selected,original);compare(List.of("run","empty","failures"));assertArrayEquals(original,Files.readAllBytes(selected));
    }

    @Test void stringBufferMutationSnapshotsAndBinaryDescriptors() throws Exception {
        fixture("""
            public class Metadata {public static String run(){
                StringBuffer b=new StringBuffer(0);b.ensureCapacity(2);b.append('a').append("bc");
                b.append(b).append((CharSequence)b,1,4).append((String)null).append((Object)null).append((StringBuffer)null);
                b.append(new char[]{'x','y','z'},1,2).append(true).append(123).append(-45L).append(1.25f).append(-0.0d);
                String before=new String(b);b.replace(1,4,"LONGER").insert(0,'!').insert(2,(String)null).delete(3,5).deleteCharAt(0);
                b.setCharAt(1,'?');b.setLength(b.length()+2);String filled=b.toString();b.setLength(7);b.trimToSize();
                char[] copy=new char[5];b.getChars(1,4,copy,1);
                StringBuffer source=new StringBuffer("xy");StringBuffer defensive=new StringBuffer((CharSequence)source);source.setCharAt(0,'z');
                StringBuilder builder=new StringBuilder("prefix").append(b);String fromBuilder=new String(builder);builder.setCharAt(0,'!');
                return before+"|"+filled+"|"+b+"|"+b.capacity()+"|"+new String(copy)+"|"+b.substring(1)+"|"+b.subSequence(0,2)+"|"+defensive+"|"+fromBuilder+"|"+new StringBuffer("x😀y").reverse();
            }
            public static int compare(){
                StringBuffer a=new StringBuffer("abc"),b=new StringBuffer("abd");
                StringBuilder c=new StringBuilder("abc"),d=new StringBuilder("ab");
                return a.compareTo(b)+((Comparable<StringBuffer>)a).compareTo(b)+c.compareTo(d)+((Comparable<StringBuilder>)c).compareTo(d);
            }}
            """);compare(List.of("run","compare"));
    }

    @Test void utf16CodePointsAndBackwardSearchMatchJdk() throws Exception {
        fixture("""
            public class Metadata {public static int run(){int hash=1;
                for(String text:new String[]{"","abcab","x😀😀y",new String(new char[]{(char)0xd800,'x',(char)0xdc00,(char)0xd800,(char)0xdc00})}){
                    StringBuilder builder=new StringBuilder(text);StringBuffer buffer=new StringBuffer(text);
                    for(int i=0;i<text.length();i++)hash=31*hash+text.codePointAt(i)+builder.codePointAt(i)+buffer.codePointAt(i);
                    for(int i=1;i<=text.length();i++)hash=31*hash+text.codePointBefore(i)+builder.codePointBefore(i)+buffer.codePointBefore(i);
                    for(int i=0;i<=text.length();i++)for(int j=i;j<=text.length();j++)hash=31*hash+text.codePointCount(i,j)+builder.codePointCount(i,j)+buffer.codePointCount(i,j);
                    for(int i=0;i<=text.length();i++)for(int offset=-4;offset<=4;offset++)try{
                        int result=text.offsetByCodePoints(i,offset);if(result!=builder.offsetByCodePoints(i,offset) || result!=buffer.offsetByCodePoints(i,offset))throw new Error();hash=31*hash+result;
                    }catch(IndexOutOfBoundsException expected){hash+=7;}
                    for(int from:new int[]{Integer.MIN_VALUE,-1,0,1,2,4,99,Integer.MAX_VALUE}){
                        for(int code:new int[]{-1,'a','y',0xd800,0xdc00,128512,1114111,1114112})hash=31*hash+text.lastIndexOf(code,from);
                        for(String needle:new String[]{"","a","ab","😀","😀y","long text"}){
                            int found=text.lastIndexOf(needle,from);if(found!=builder.lastIndexOf(needle,from) || found!=buffer.lastIndexOf(needle,from))throw new Error();hash=31*hash+found;
                            hash+=text.lastIndexOf(needle)+builder.lastIndexOf(needle)+buffer.lastIndexOf(needle);
                        }
                    }
                    hash+=text.lastIndexOf(128512);
                }
                StringBuilder b=new StringBuilder();StringBuffer c=new StringBuffer();for(int code:new int[]{0,65,0xd800,65535,65536,128512,1114111}){b.appendCodePoint(code);c.appendCodePoint(code);}
                if(!b.toString().equals(c.toString()))throw new Error();return hash+b.toString().hashCode();
            }}
            """);compare(List.of("run"));
    }

    @Test void literalReplacementsAndMutableTextFailures() throws Exception {
        fixture("""
            public class Metadata {public static int run(){int hash=1;
                for(String text:new String[]{"","aaa","ababa","😀x😀"})for(String from:new String[]{"","a","aba","😀","none"})for(String to:new String[]{"","$1\\\\","😀"}){
                    hash=31*hash+text.replace((CharSequence)new StringBuffer(from),new StringBuilder(to)).hashCode();
                }
                StringBuffer b=new StringBuffer("abc");StringBuilder c=new StringBuilder("abc");
                b.replace(1,99,"X");c.replace(1,99,"X");if(!b.toString().equals(c.toString()))throw new Error();
                b.replace(1,1,"");b.replace(1,1,"123");b.replace(0,3,"");hash=31*hash+b.toString().hashCode();
                if(!"aX".contentEquals(c) || !"aX".contentEquals(new StringBuffer("aX")) || "aX".contentEquals(new StringBuilder("aY")))throw new Error();
                try{b.replace(0,1,null);throw new Error();}catch(NullPointerException e){hash++;}
                try{b.replace(-1,1,null);throw new Error();}catch(StringIndexOutOfBoundsException e){hash++;}
                try{b.setLength(-1);throw new Error();}catch(StringIndexOutOfBoundsException e){hash++;}
                try{b.charAt(b.length());throw new Error();}catch(StringIndexOutOfBoundsException e){hash++;}
                try{new StringBuffer(-1);throw new Error();}catch(NegativeArraySizeException e){hash++;}
                try{new StringBuffer((String)null);throw new Error();}catch(NullPointerException e){hash++;}
                try{new String((StringBuilder)null);throw new Error();}catch(NullPointerException e){hash++;}
                try{new String((StringBuffer)null);throw new Error();}catch(NullPointerException e){hash++;}
                try{b.append((char[])null);throw new Error();}catch(NullPointerException e){hash++;}
                try{b.append("abc",2,1);throw new Error();}catch(IndexOutOfBoundsException e){hash++;}
                try{b.appendCodePoint(-1);throw new Error();}catch(IllegalArgumentException e){hash++;}
                try{c.appendCodePoint(1114112);throw new Error();}catch(IllegalArgumentException e){hash++;}
                try{"x".codePointAt(1);throw new Error();}catch(IndexOutOfBoundsException e){hash++;}
                try{b.codePointBefore(0);throw new Error();}catch(IndexOutOfBoundsException e){hash++;}
                try{c.codePointCount(-1,1);throw new Error();}catch(IndexOutOfBoundsException e){hash++;}
                try{"".offsetByCodePoints(0,Integer.MIN_VALUE);throw new Error();}catch(IndexOutOfBoundsException e){hash++;}
                try{"x".lastIndexOf((String)null,-1);throw new Error();}catch(NullPointerException e){hash++;}
                try{"x".replace((CharSequence)null,"y");throw new Error();}catch(NullPointerException e){hash++;}
                try{"x".replace("",(CharSequence)null);throw new Error();}catch(NullPointerException e){hash++;}
                try{"x".contentEquals((StringBuffer)null);throw new Error();}catch(NullPointerException e){hash++;}
                return 31*hash+b.toString().hashCode();
            }}
            """);compare(List.of("run"));
    }

    @Test void executesUnchangedPinnedJaxbWhitespaceProcessor() throws Exception {
        Path repository=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()));
        Path artifact=repository.resolve("org/glassfish/jaxb/jaxb-core/4.0.2/jaxb-core-4.0.2.jar");
        assertDigest(artifact,"d7ff2954ad78480bbab9391cccff3a22f42a82b6e09aeca1c7d502411c470ccd");fixtureClasspath=artifact.toString();
        fixture("""
            import org.glassfish.jaxb.core.WhiteSpaceProcessor;
            public class Metadata {public static int run(){int hash=1;
                for(String text:new String[]{"","abc"," "," \\t\\r\\n "," x  y\\tz ","\\u00a0x\\u2003","😀\\n😀",new String(new char[]{0,'x',11,12})}){
                    hash=31*hash+WhiteSpaceProcessor.replace(text).hashCode();hash=31*hash+WhiteSpaceProcessor.collapse(text).hashCode();
                    StringBuffer buffer=new StringBuffer(text);
                    hash=31*hash+WhiteSpaceProcessor.replace((CharSequence)buffer).toString().hashCode();
                    hash=31*hash+WhiteSpaceProcessor.collapse((CharSequence)buffer).toString().hashCode();
                    hash=31*hash+WhiteSpaceProcessor.trim(buffer).toString().hashCode();
                    if(!buffer.toString().equals(text))throw new Error();
                    hash+=WhiteSpaceProcessor.isWhiteSpace(buffer)?7:11;
                }
                for(int ch=0;ch<256;ch++)hash=31*hash+(WhiteSpaceProcessor.isWhiteSpace((char)ch)?1:0);
                try{WhiteSpaceProcessor.replace((String)null);throw new Error();}catch(NullPointerException e){hash++;}
                StringBuffer identity=new StringBuffer("abc");if(WhiteSpaceProcessor.replace((CharSequence)identity)!=identity || WhiteSpaceProcessor.trim(identity)!=identity || WhiteSpaceProcessor.collapse((CharSequence)identity)!=identity)throw new Error();
                return hash;
            }}
            """);
        String name="org/glassfish/jaxb/core/WhiteSpaceProcessor";byte[] original;
        try(JarFile jar=new JarFile(artifact.toFile())){original=jar.getInputStream(jar.getJarEntry(name+".class")).readAllBytes();}
        writeClass(name,original);compare(List.of("run"));assertArrayEquals(original,Files.readAllBytes(directory.resolve(name+".class")));
    }

    @Test void executesSelectedPinnedLogbackStringBufferEscaping() throws Exception {
        Path repository=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()));
        Path artifact=repository.resolve("ch/qos/logback/logback-core/1.4.14/logback-core-1.4.14.jar");
        assertDigest(artifact,"f8c2f05f42530b1852739507c1792f0080167850ed8f396444c6913d6617a293");fixtureClasspath=artifact.toString();
        fixture("""
            import ch.qos.logback.core.helpers.Transform;
            public class Metadata {public static String run(){
                char[] chars=new char[132];for(int i=0;i<128;i++)chars[i]=(char)i;chars[128]='é';chars[129]='中';chars[130]=(char)0xd83d;chars[131]=(char)0xde00;
                StringBuffer b=new StringBuffer(new String(chars));String snapshot=new String(b);String result=Transform.escapeTags(b);
                if(!result.equals(b.toString()) || !snapshot.equals(new String(chars)))throw new Error();
                return result+"|"+Transform.escapeTags(new StringBuffer(""))+"|"+Transform.escapeTags(new StringBuffer("<>&\\\"'"));
            }
            public static int failures(){try{Transform.escapeTags((StringBuffer)null);return 0;}catch(NullPointerException expected){return 1;}}
            }
            """);
        try(JarFile jar=new JarFile(artifact.toFile())){
            String name="ch/qos/logback/core/helpers/Transform";ClassNode selected=new ClassNode();
            new ClassReader(jar.getInputStream(jar.getJarEntry(name+".class")).readAllBytes()).accept(selected,0);
            selected.fields.clear();selected.methods.removeIf(method->!method.name.equals("escapeTags") || !method.desc.equals("(Ljava/lang/StringBuffer;)Ljava/lang/String;"));
            assertEquals(1,selected.methods.size());ClassWriter writer=new ClassWriter(0);selected.accept(writer);writeClass(name,writer.toByteArray());
        }
        compare(List.of("run","failures"));
    }

    private void writeClass(String name,byte[] bytes) throws Exception {
        Path file=directory.resolve(name+".class");Files.createDirectories(file.getParent());Files.write(file,bytes);
    }
    private void assertDigest(Path artifact,String expected) throws Exception {
        assertEquals(expected,HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(artifact))));
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
