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

class BytecodeMapCollectorsTest implements Opcodes {
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

    @Test void mapsDuplicatesMergersAndSnapshots() throws Exception {
        fixture("""
            import java.util.*;import java.util.function.*;import java.util.stream.*;
            public class Metadata {
                static <A,R> R combine(Collector<String,A,R> collector){A a=collector.supplier().get(),b=collector.supplier().get();collector.accumulator().accept(a,"a");collector.accumulator().accept(b,"b");collector.accumulator().accept(b,"aa");return collector.finisher().apply(collector.combiner().apply(a,b));}
                public static int run(){int hash=1;
                    Map<String,Integer> map=Stream.of("a","bb","ccc").collect(Collectors.toMap(Function.identity(),String::length));map.put(null,4);hash=31*hash+map.hashCode();
                    map=Stream.of("a","b","aa").collect(Collectors.toMap(v->v.substring(0,1),String::length,Integer::sum));hash=31*hash+map.hashCode();
                    Map<String,String> ordered=Stream.of("b1","a1","b2").collect(Collectors.toMap(v->v.substring(0,1),Function.identity(),(a,b)->a+b,LinkedHashMap::new));hash=31*hash+ordered.toString().hashCode();
                    hash=31*hash+combine(Collectors.toMap(Function.identity(),String::length)).hashCode();
                    hash=31*hash+combine(Collectors.toMap(v->v.substring(0,1),String::length,Integer::sum,LinkedHashMap::new)).toString().hashCode();
                    hash=31*hash+combine(Collectors.toUnmodifiableMap(Function.identity(),String::length)).hashCode();
                    Map<String,String> deleted=Stream.of("x","x","x").collect(Collectors.toMap(Function.identity(),Function.identity(),(a,b)->null));hash=31*hash+deleted.hashCode();
                    Map<String,Integer> frozen=Stream.of("a","b","aa").collect(Collectors.toUnmodifiableMap(v->v.substring(0,1),String::length,Integer::sum));hash=31*hash+frozen.hashCode();
                    try{frozen.put("x",2);throw new Error();}catch(UnsupportedOperationException e){hash++;}
                    try{frozen.entrySet().iterator().next().setValue(7);throw new Error();}catch(UnsupportedOperationException e){hash++;}
                    if(!Collectors.toMap(Function.identity(),Function.identity()).characteristics().contains(Collector.Characteristics.IDENTITY_FINISH) || Collectors.toUnmodifiableMap(Function.identity(),Function.identity()).characteristics().contains(Collector.Characteristics.IDENTITY_FINISH))throw new Error();
                    try{Stream.of("a","a").collect(Collectors.toMap(Function.identity(),String::length));throw new Error();}catch(IllegalStateException e){hash++;}
                    try{Stream.of("a").collect(Collectors.toMap(Function.identity(),v->null));throw new Error();}catch(NullPointerException e){hash++;}
                    try{Stream.of("a").collect(Collectors.toUnmodifiableMap(v->null,String::length));throw new Error();}catch(NullPointerException e){hash++;}
                    try{Collectors.toUnmodifiableMap(null,Function.identity());throw new Error();}catch(NullPointerException e){hash++;}
                    try{Collectors.toUnmodifiableMap(Function.identity(),Function.identity(),null);throw new Error();}catch(NullPointerException e){hash++;}
                    hash+=Stream.<String>empty().collect(Collectors.toMap(null,Function.identity())).size();
                    hash+=Stream.<String>empty().collect(Collectors.toMap(Function.identity(),Function.identity(),null)).size();
                    Collector<String,?,Map<String,String>> badFactory=Collectors.toMap(Function.identity(),Function.identity(),(a,b)->a,null);
                    try{Stream.<String>empty().collect(badFactory);throw new Error();}catch(NullPointerException e){hash++;}
                    return hash;
                }
            }
            """);compare(List.of("run"));
    }
    @Test void duplicateCombinerAndCallbackFailures() throws Exception {
        fixture("""
            import java.util.*;import java.util.function.*;import java.util.stream.*;
            public class Metadata {
                static <A> int duplicate(Collector<String,A,Map<String,Integer>> collector){A a=collector.supplier().get(),b=collector.supplier().get();collector.accumulator().accept(a,"x");collector.accumulator().accept(b,"x");try{collector.combiner().apply(a,b);throw new Error();}catch(IllegalStateException expected){return 1;}}
                public static int run(){int hash=duplicate(Collectors.toMap(Function.identity(),String::length));
                    RuntimeException failure=new RuntimeException();Stream<String> stream=Stream.of("x");
                    try{stream.collect(Collectors.toMap(v->{throw failure;},String::length));throw new Error();}catch(RuntimeException actual){if(actual!=failure)throw new Error();hash++;}
                    try{stream.count();throw new Error();}catch(IllegalStateException expected){hash++;}
                    try{Stream.of("x","x").collect(Collectors.toMap(Function.identity(),String::length,(a,b)->{throw failure;}));throw new Error();}catch(RuntimeException actual){if(actual!=failure)throw new Error();hash++;}
                    return hash;
                }
            }
            """);compare(List.of("run"));
    }
    @Test void executesOriginalPinnedHibernateIndexMapPipeline() throws Exception {
        Path repository=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()));
        Path artifact=repository.resolve("org/hibernate/orm/hibernate-core/6.4.4.Final/hibernate-core-6.4.4.Final.jar");
        assertDigest(artifact,"a1324b7c80c800826c9a5d74b61b0de768141f967c2b082650cb7bf4675570a7");
        try(JarFile jar=new JarFile(artifact.toFile())){for(String simple:List.of("Index","Column")){
            ClassNode original=new ClassNode();new ClassReader(jar.getInputStream(jar.getJarEntry("org/hibernate/mapping/"+simple+".class")).readAllBytes()).accept(original,0);
            original.interfaces.clear();original.signature=null;
            if(simple.equals("Index")){
                original.fields.removeIf(f->!Set.of("selectables","selectableOrderMap").contains(f.name));
                original.methods.removeIf(m->!(m.name.equals("<init>") && m.desc.equals("()V")) && !Set.of("getColumnOrderMap","lambda$getColumnOrderMap$1").contains(m.name));
                assertEquals(3,original.methods.size());
                for(FieldNode field:original.fields){field.access=ACC_PUBLIC;field.signature=null;}
            }else{
                original.fields.removeIf(f->!Set.of("nullable","checkConstraints").contains(f.name));
                original.methods.removeIf(m->!m.name.equals("<init>") || !m.desc.equals("()V"));assertEquals(1,original.methods.size());
            }
            ClassWriter writer=new ClassWriter(0);original.accept(writer);writeClass(original.name,writer.toByteArray());
        }}
        fixtureClasspath=directory.toString();fixture("""
            import java.util.*;import org.hibernate.mapping.*;
            public class Metadata {public static int run(){Index index=new Index();Column a=new Column(),b=new Column();
                Map raw=index.selectableOrderMap;raw.put(a,"asc");raw.put(b,"desc");Map<Column,String> result=index.getColumnOrderMap();
                int hash=result.size()+result.get(a).hashCode()+result.get(b).hashCode();raw.put(a,"changed");
                if(!result.get(a).equals("asc"))throw new Error();
                try{result.clear();throw new Error();}catch(UnsupportedOperationException expected){hash++;}
                raw.put(b,null);try{index.getColumnOrderMap();throw new Error();}catch(NullPointerException expected){hash++;}
                raw.clear();if(!index.getColumnOrderMap().isEmpty())throw new Error();raw.put(null,"x");
                try{index.getColumnOrderMap();throw new Error();}catch(NullPointerException expected){hash++;}return hash;
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
