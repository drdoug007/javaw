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

class BytecodeCaseComparisonTest implements Opcodes {
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

    @Test void matchesExhaustiveJdk17SimpleCaseReference() throws Exception {
        fixture("""
            public class Metadata {
                public static int upper(){int hash=1;for(int c=-1;c<=0x110000;c++)hash=31*hash+Character.toUpperCase(c);return hash;}
                public static int lower(){int hash=1;for(int c=-1;c<=0x110000;c++)hash=31*hash+Character.toLowerCase(c);return hash;}
                public static int chars(){int hash=1;for(int c=0;c<=65535;c++){hash=31*hash+Character.toUpperCase((char)c);hash=31*hash+Character.toLowerCase((char)c);}return hash;}
            }
            """);
        // Oracle: JDK 17.0.9 / Unicode 13. The generator prints these independent
        // whole-domain checksums before compressing the mapping ranges.
        String checks="assert.equal(e['Metadata.upper'](),-387757952);assert.equal(e['Metadata.lower'](),-169441335);assert.equal(e['Metadata.chars'](),-1620715694);";
        for(boolean reverse:new boolean[]{false,true})execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),checks);
    }
    @Test void unicodeComparisonsAndSurrogateRegionBoundaries() throws Exception {
        fixture("""
            import java.util.*;
            public class Metadata {public static int run(){int hash=1;
                String[] texts={"","abc","ABC","ab","aBcD","I","i","\\u0130","\\u0131","\\u212a","k","S","\\u017f","\\u00df","\\u1e9e","\\u03a3","\\u03c3","\\u03c2","\\u10d0","\\u1c90","\\ud801\\udc00","\\ud801\\udc28","\\ud801","\\udc00","x\\ud801\\udc00y","x\\ud801\\udc28Y","\\ud801\\ud801\\udc28","\\ud800\\udc00"};
                int[] offsets={-1,0,1,2,4,Integer.MAX_VALUE};int[] lengths={Integer.MIN_VALUE,-1,0,1,2,3,Integer.MAX_VALUE};
                for(String a:texts)for(String b:texts){
                    hash=31*hash+Integer.signum(a.compareToIgnoreCase(b));hash=31*hash+Integer.signum(String.CASE_INSENSITIVE_ORDER.compare(a,b));hash=31*hash+(a.equalsIgnoreCase(b)?1:0);
                    for(int i:offsets)for(int j:offsets)for(int n:lengths){hash=31*hash+(a.regionMatches(true,i,b,j,n)?1:0);hash=31*hash+(a.regionMatches(false,i,b,j,n)?1:0);}
                }
                if("abc".equalsIgnoreCase(null))throw new Error();
                try{"x".compareToIgnoreCase(null);throw new Error();}catch(NullPointerException e){hash++;}
                try{"x".regionMatches(true,0,null,0,1);throw new Error();}catch(NullPointerException e){hash++;}
                if("x".regionMatches(true,-1,null,0,1))throw new Error();
                try{String.CASE_INSENSITIVE_ORDER.compare(null,"x");throw new Error();}catch(NullPointerException e){hash++;}
                try{String.CASE_INSENSITIVE_ORDER.compare("x",null);throw new Error();}catch(NullPointerException e){hash++;}
                if(!(String.CASE_INSENSITIVE_ORDER instanceof java.io.Serializable))throw new Error();return hash;
            }}
            """);compare(List.of("run"));
    }
    @Test void comparatorStableSortingAndFunctionalComposition() throws Exception {
        fixture("""
            import java.util.*;
            public class Metadata {public static int run(){int hash=1;
                String[] values={"z","A","a","\\u0130","I","i","\\u0131","k","\\u212a","S","\\u017f","\\ud801\\udc28","\\ud801\\udc00"};
                for(Comparator<String> order:Arrays.<Comparator<String>>asList(String.CASE_INSENSITIVE_ORDER,String.CASE_INSENSITIVE_ORDER.reversed(),String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder()))){
                    String[] sorted=values.clone();Arrays.sort(sorted,order);hash=31*hash+Arrays.hashCode(sorted);
                    List<String> list=new ArrayList<>(Arrays.asList(values));list.sort(order);hash=31*hash+list.hashCode();
                }
                for(int c:new int[]{-1,Integer.MIN_VALUE,Integer.MAX_VALUE,0x110000})hash=31*hash+Character.toUpperCase(c)+Character.toLowerCase(c);return hash;
            }}
            """);compare(List.of("run"));
    }
    @Test void executesUnchangedPinnedLogbackAndSlf4jLevels() throws Exception {
        Path repository=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()));
        Path logback=repository.resolve("ch/qos/logback/logback-classic/1.4.14/logback-classic-1.4.14.jar");
        Path slf4j=repository.resolve("org/slf4j/slf4j-api/2.0.2/slf4j-api-2.0.2.jar");
        assertDigest(logback,"8e832f7263ca606ae36dabb2d8b24c2f43d82cf634e81dad9d1640fa6ee3c596");
        assertDigest(slf4j,"f920e120b32ffddb2fdf5af96eed3cd5320199693f47a36b0c2afde3913d7071");
        for(Path artifact:List.of(logback,slf4j))try(JarFile jar=new JarFile(artifact.toFile())){
            String name=artifact.equals(logback)?"ch/qos/logback/classic/Level":"org/slf4j/event/Level";
            writeClass(name,jar.getInputStream(jar.getJarEntry(name+".class")).readAllBytes());
        }
        fixtureClasspath=directory.toString();
        fixture("""
            import ch.qos.logback.classic.Level;
            public class Metadata {public static int run(){int hash=1;
                for(String text:new String[]{null,"","ALL","all","aLl","TRACE","trace","DEBUG","dEbUg","INFO","\\u0131nfo","\\u0130NFO","WARN","warn","ERROR","error","OFF","off"," OFF ","unknown"}){
                    Level a=Level.toLevel(text);hash=31*hash+a.toInt();hash=31*hash+a.toString().hashCode();hash=31*hash+Level.valueOf(text).toInt();
                    Level b=Level.toLevel(text,Level.WARN);hash=31*hash+b.toInteger().intValue();if(Level.toLevel(text,null)!=null)hash++;
                }
                for(Level a:new Level[]{Level.ALL,Level.TRACE,Level.DEBUG,Level.INFO,Level.WARN,Level.ERROR,Level.OFF}){
                    if(Level.toLevel(a.toInt())!=a)throw new Error();
                    for(Level b:new Level[]{Level.ALL,Level.INFO,Level.OFF})hash=31*hash+(a.isGreaterOrEqual(b)?1:0);
                }
                for(org.slf4j.event.Level level:org.slf4j.event.Level.values()){
                    Level a=Level.convertAnSLF4JLevel(level);hash=31*hash+a.toInt();hash=31*hash+level.toString().hashCode();
                    if(org.slf4j.event.Level.intToLevel(level.toInt())!=level || org.slf4j.event.Level.valueOf(level.name())!=level)throw new Error();
                    hash=31*hash+Level.toLocationAwareLoggerInteger(a);if(Level.fromLocationAwareLoggerInteger(level.toInt())!=a)throw new Error();
                }
                for(int n:new int[]{-1,1,99,Integer.MIN_VALUE,Integer.MAX_VALUE}){
                    hash=31*hash+Level.toLevel(n,Level.TRACE).toInt();
                    try{Level.fromLocationAwareLoggerInteger(n);throw new Error();}catch(IllegalArgumentException e){hash++;}
                    try{org.slf4j.event.Level.intToLevel(n);throw new Error();}catch(IllegalArgumentException e){hash++;}
                }
                try{Level.convertAnSLF4JLevel(null);throw new Error();}catch(NullPointerException e){hash++;}return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void executesOriginalPinnedSpringCaseInsensitiveRegions() throws Exception {
        Path repository=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()));
        Path artifact=repository.resolve("org/springframework/spring-core/6.1.5/spring-core-6.1.5.jar");
        assertDigest(artifact,"476dac28509b3f47d0b5e895a639fa75ac2218f074ce214e44832a836c469f7e");
        try(JarFile jar=new JarFile(artifact.toFile())){
            ClassNode original=new ClassNode();new ClassReader(jar.getInputStream(jar.getJarEntry("org/springframework/util/StringUtils.class")).readAllBytes()).accept(original,0);
            original.fields.clear();original.methods.removeIf(m->!Set.of("startsWithIgnoreCase","endsWithIgnoreCase").contains(m.name));
            assertEquals(2,original.methods.size());ClassWriter writer=new ClassWriter(0);original.accept(writer);writeClass(original.name,writer.toByteArray());
        }
        fixtureClasspath=directory.toString();
        fixture("""
            import org.springframework.util.StringUtils;
            public class Metadata {
                static String point(int code){return new String(new int[]{code},0,1);}
                public static int run(){int hash=1;
                    String[] values={null,"","a","A","AbCd","bc",point(0x130),point(0x131),"i",point(0x10400),point(0x10428),point(0xd801),point(0xdc00),"x"+point(0x10400),point(0x10428)+"X"};
                    for(String text:values)for(String part:values){
                        hash=31*hash+(StringUtils.startsWithIgnoreCase(text,part)?1:0);hash=31*hash+(StringUtils.endsWithIgnoreCase(text,part)?1:0);
                    }return hash;
                }
            }
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
