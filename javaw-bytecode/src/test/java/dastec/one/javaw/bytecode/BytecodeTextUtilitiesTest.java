package dastec.one.javaw.bytecode;

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

class BytecodeTextUtilitiesTest implements Opcodes {
    @TempDir Path directory;
    private String release="17";
    private String fixtureClassName="Metadata";
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

    @Test void whitespaceAndCodePointConstruction() throws Exception {
        fixture("""
            public class Metadata {public static int run(){int hash=1;
                for(int c=0;c<=0x10ffff;c++)if(Character.isWhitespace(c))hash=31*hash+c;
                for(String text:new String[]{"", " ","\\t\\r\\n", "  hello  ","\\u1680\\u2000abc\\u205f\\u3000", "\\u00a0x\\u2007", "\\ud800 x ", "\\u2028\\u2029", "\\u0085"}){
                    hash=31*hash+text.strip().hashCode();hash=31*hash+text.stripLeading().hashCode();hash=31*hash+text.stripTrailing().hashCode();hash=31*hash+(text.isBlank()?1:0);
                }
                int[] points={65,0x10000,0x10ffff,0xd800,0xdc00,0,66};
                for(int i=0;i<=points.length;i++)for(int n=0;n<=points.length-i;n++)hash=31*hash+new String(points,i,n).hashCode();
                for(int bad:new int[]{-1,0x110000,Integer.MAX_VALUE})try{new String(new int[]{bad},0,1);throw new Error();}catch(IllegalArgumentException e){hash++;}
                try{new String(points,-1,1);throw new Error();}catch(IndexOutOfBoundsException e){hash++;}
                try{new String(points,1,Integer.MAX_VALUE);throw new Error();}catch(IndexOutOfBoundsException e){hash++;}
                try{new String((int[])null,-1,-1);throw new Error();}catch(NullPointerException e){hash++;}return hash;
            }}
            """);compare(List.of("run"));
    }
    @Test void repetitionJoiningRegionsAndLazyLines() throws Exception {
        fixture("""
            import java.util.*;import java.util.stream.*;
            public class Metadata {public static int run(){int hash=1;
                for(String text:new String[]{"","ab","\\ud800\\udc00","x\\ud800"})for(int n=0;n<5;n++)hash=31*hash+text.repeat(n).hashCode();
                try{"".repeat(-1);throw new Error();}catch(IllegalArgumentException e){hash++;}
                try{"xx".repeat(Integer.MAX_VALUE);throw new Error();}catch(OutOfMemoryError e){hash++;}
                for(CharSequence separator:new CharSequence[]{"",",",new StringBuilder("::")}){
                    CharSequence[] values={"a",null,new StringBuilder("b")};hash=31*hash+String.join(separator,values).hashCode();hash=31*hash+String.join(separator,Arrays.asList(values)).hashCode();
                    hash=31*hash+String.join(separator,new CharSequence[0]).hashCode();
                }
                CharSequence unusual=new CharSequence(){public int length(){throw new Error();}public char charAt(int i){throw new Error();}public CharSequence subSequence(int a,int b){throw new Error();}public String toString(){return "converted";}};
                hash=31*hash+String.join(unusual,unusual,null).hashCode();hash=31*hash+String.join("/",Arrays.asList(unusual,null)).hashCode();
                try{String.join(null,new CharSequence[0]);throw new Error();}catch(NullPointerException e){hash++;}
                try{String.join(",",(CharSequence[])null);throw new Error();}catch(NullPointerException e){hash++;}
                try{String.join(",",(Iterable<CharSequence>)null);throw new Error();}catch(NullPointerException e){hash++;}
                for(int i:new int[]{-1,0,1,3,4,Integer.MAX_VALUE})for(int j:new int[]{-1,0,2,4,Integer.MAX_VALUE})for(int n:new int[]{Integer.MIN_VALUE,-1,0,1,3,Integer.MAX_VALUE})hash=31*hash+("abba".regionMatches(i,"baab",j,n)?1:0);
                try{"x".regionMatches(0,null,0,1);throw new Error();}catch(NullPointerException e){hash++;}
                for(String text:new String[]{"","a","a\\n","\\n","\\r\\n","a\\r\\nb\\rc\\n\\n","a\\u2028b","\\r\\r\\n"}){
                    hash=31*hash+text.lines().collect(Collectors.joining("|")).hashCode();hash+=text.lines().count();
                    hash=31*hash+text.lines().limit(1).collect(Collectors.joining()).hashCode();
                }
                int[] calls={0};Stream<String> lines="a\\nb".lines().peek(x->calls[0]++);if(calls[0]!=0)throw new Error();if(!lines.findFirst().get().equals("a") || calls[0]!=1)throw new Error();
                try{lines.count();throw new Error();}catch(IllegalStateException e){hash++;}
                Stream<String> closed="a".lines().onClose(()->calls[0]++);closed.close();closed.close();if(calls[0]!=2)throw new Error();
                try{closed.count();throw new Error();}catch(IllegalStateException e){hash++;}
                try{"a".lines().map(x->{throw new IllegalArgumentException();}).count();throw new Error();}catch(IllegalArgumentException e){hash++;}return hash;
            }}
            """);compare(List.of("run"));
    }
    @Test void executesOriginalPinnedSpringWhitespaceMethods() throws Exception {
        Path repository=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()));
        Path artifact=repository.resolve("org/springframework/spring-core/6.1.5/spring-core-6.1.5.jar");
        assertDigest(artifact,"476dac28509b3f47d0b5e895a639fa75ac2218f074ce214e44832a836c469f7e");
        try(JarFile jar=new JarFile(artifact.toFile())){
            ClassNode original=new ClassNode();new ClassReader(jar.getInputStream(jar.getJarEntry("org/springframework/util/StringUtils.class")).readAllBytes()).accept(original,0);
            original.fields.clear();original.methods.removeIf(m->!Set.of("hasLength","trimWhitespace","trimLeadingWhitespace","trimTrailingWhitespace","trimAllWhitespace").contains(m.name));
            assertEquals(7,original.methods.size());ClassWriter writer=new ClassWriter(0);original.accept(writer);writeClass(original.name,writer.toByteArray());
        }
        fixtureClasspath=directory.toString();
        fixture("""
            import org.springframework.util.StringUtils;
            public class Metadata {public static int run(){int hash=1;
                for(String text:new String[]{null,"","  x  ","\\u2000x\\t y\\u3000","\\u00a0 x\\u2007","\\ud800 x\\udc00"}){
                    String[] results={StringUtils.trimWhitespace(text),StringUtils.trimLeadingWhitespace(text),StringUtils.trimTrailingWhitespace(text),StringUtils.trimAllWhitespace(text)};
                    for(String value:results)hash=31*hash+(value==null?7:value.hashCode());
                    CharSequence value=StringUtils.trimAllWhitespace(text==null?null:new StringBuilder(text));hash=31*hash+(value==null?7:value.toString().hashCode());
                }return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void executesUnchangedPinnedMicrometerStringUtils() throws Exception {
        Path repository=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()));
        Path artifact=repository.resolve("io/micrometer/micrometer-commons/1.12.4/micrometer-commons-1.12.4.jar");
        assertDigest(artifact,"62e46f3b46e234e38f8333cf3e3a5ff54d0a9f3ca890e4b9773674d180c36914");
        try(JarFile jar=new JarFile(artifact.toFile())){
            String name="io/micrometer/common/util/StringUtils";
            writeClass(name,jar.getInputStream(jar.getJarEntry(name+".class")).readAllBytes());
        }
        fixtureClasspath=directory.toString();
        fixture("""
            import io.micrometer.common.util.StringUtils;
            public class Metadata {public static int run(){int hash=1;
                for(String text:new String[]{null,""," ","\\t\\n","\\u1680\\u2000\\u2028\\u3000","\\u00a0","\\u2007","\\u202f","x","\\ud800"}){
                    hash=31*hash+(StringUtils.isBlank(text)?1:0);hash=31*hash+(StringUtils.isNotBlank(text)?1:0);
                    hash=31*hash+(StringUtils.isEmpty(text)?1:0);hash=31*hash+(StringUtils.isNotEmpty(text)?1:0);
                }
                for(String text:new String[]{"","abcdef","x\\ud800y"})for(int n:new int[]{-1,0,1,2,6,10}){
                    try{hash=31*hash+StringUtils.truncate(text,n).hashCode();}catch(IndexOutOfBoundsException e){hash+=7;}
                    try{hash=31*hash+StringUtils.truncate(text,n,"..").hashCode();}catch(IllegalArgumentException e){hash+=11;}
                }
                try{StringUtils.truncate(null,3);throw new Error();}catch(NullPointerException e){hash++;}
                try{StringUtils.truncate("text",3,null);throw new Error();}catch(NullPointerException e){hash++;}return hash;
            }}
            """);compare(List.of("run"));
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
            Class<?> type=loader.loadClass(fixtureClassName);for(String name:names){Object expected=type.getMethod(name).invoke(null);assertions.append("assert.equal(").append(expected instanceof String?"text(e['"+fixtureClassName+".":"e['"+fixtureClassName+".").append(name).append(expected instanceof String?"']()),":"'](),").append(expected instanceof String?js((String)expected):expected).append(");\n");}
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
