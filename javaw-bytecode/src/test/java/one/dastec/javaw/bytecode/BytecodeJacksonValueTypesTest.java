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

class BytecodeJacksonValueTypesTest {
    @TempDir Path directory;
    private String release="17";
    private String fixtureClassName="Metadata";
    private String fixtureClasspath;
    private Path artifact() throws Exception {
        Path path=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()))
            .resolve("com/fasterxml/jackson/core/jackson-core/2.15.4/jackson-core-2.15.4.jar");
        assertEquals("8dc9210dd285db366f45f518dd1e6a9ccfeb0f1a8e184a899fe96d29edf1fd94",java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path))));
        return path;
    }
    private void selectedCarriers() throws Exception {
        try(JarFile jar=new JarFile(artifact().toFile())) {
            for(String owner:List.of("com/fasterxml/jackson/core/Version","com/fasterxml/jackson/core/util/Separators")) {
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

    // Version and Separators differential cases.

    @Test void separators() throws Exception {
        fixture("""
            import com.fasterxml.jackson.core.util.Separators;
            public class Metadata {
                static String describe(Separators s){return ""+s.getObjectFieldValueSeparator()+s.getObjectEntrySeparator()+s.getArrayValueSeparator();}
                public static String run(){
                    Separators defaults=Separators.createDefaultInstance(),plain=new Separators(),custom=new Separators('=',';','|');
                    String text=describe(defaults)+describe(plain)+describe(custom)+(defaults==Separators.createDefaultInstance())+(defaults instanceof java.io.Serializable)+"|";
                    text+=(custom.withObjectFieldValueSeparator('=')==custom)+""+(custom.withObjectEntrySeparator(';')==custom)+(custom.withArrayValueSeparator('|')==custom)+"|";
                    Separators field=custom.withObjectFieldValueSeparator('-'),entry=custom.withObjectEntrySeparator('&'),array=custom.withArrayValueSeparator('\\u0000');
                    text+=describe(field)+describe(entry)+(int)array.getArrayValueSeparator()+describe(custom)+(field!=custom)+(entry!=custom)+(array!=custom)+"|";
                    Separators chained=plain.withObjectFieldValueSeparator('a').withObjectEntrySeparator('b').withArrayValueSeparator('c').withArrayValueSeparator('c');
                    return text+describe(chained)+describe(plain)+describe(new Separators('\\uffff','\\ud800','x'))+plain.equals(new Separators())+(plain.hashCode()==plain.hashCode());
                }
            }
            """);compareText(List.of("run"));
    }

    @Test void versions() throws Exception {
        fixture("""
            import com.fasterxml.jackson.core.Version;
            public class Metadata {
                static String describe(Version v){return v.getMajorVersion()+"."+v.getMinorVersion()+"."+v.getPatchLevel()+":"+v.getGroupId()+":"+v.getArtifactId()+":"+v.isSnapshot()+v.isUnknownVersion()+v.isUknownVersion()+":"+v+":"+v.toFullString()+":"+v.hashCode();}
                public static String run(){
                    Version[] versions={Version.unknownVersion(),new Version(0,0,0,null,null,null),new Version(2,15,4,null),new Version(2,15,4,"SNAPSHOT"),new Version(2,15,4,"","",""),
                        new Version(2,15,4,null,"com.fasterxml.jackson.core","jackson-core"),new Version(2,15,4,"rc1","com.fasterxml.jackson.core","jackson-core"),new Version(2,15,10,null,"com.fasterxml.jackson.core","jackson-core"),
                        new Version(2,16,0,null,"com.fasterxml.jackson.core","jackson-core"),new Version(3,0,0,null,"com.fasterxml.jackson.core","jackson-core"),new Version(2,15,4,null,"com.fasterxml.jackson.core","jackson-databind"),
                        new Version(2,15,4,null,"org.example","jackson-core"),new Version(-1,-2,-3,"x-SNAPSHOT","g","a"),new Version(Integer.MAX_VALUE,Integer.MIN_VALUE,7,"SNAPSHOT","g","a")};
                    StringBuilder text=new StringBuilder(Version.unknownVersion()==Version.unknownVersion()?"same|":"fresh|");
                    for(Version v:versions)text.append(describe(v)).append('|');
                    for(Version left:versions)for(Version right:versions)text.append(Integer.signum(left.compareTo(right))+1).append(left.equals(right)?'e':'n');
                    Comparable<Version> comparable=versions[2];text.append('|').append(Integer.signum(comparable.compareTo(versions[3]))).append(versions[2].equals(null)).append(versions[2].equals("2.15.4")).append(versions[2].equals(versions[2]));
                    try{versions[2].compareTo(null);text.append("no");}catch(NullPointerException expected){text.append("npe");}
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
