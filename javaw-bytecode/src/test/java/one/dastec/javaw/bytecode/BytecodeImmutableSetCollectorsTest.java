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

class BytecodeImmutableSetCollectorsTest implements Opcodes {
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

    @Test void immutableSetsCombinersNullsAndCharacteristics() throws Exception {
        fixture("""
            import java.util.*;import java.util.function.*;import java.util.stream.*;
            public class Metadata {
                static <A> Set<String> combine(Collector<String,A,Set<String>> c){A a=c.supplier().get(),b=c.supplier().get();c.accumulator().accept(a,"a");c.accumulator().accept(b,"a");c.accumulator().accept(b,"b");Set<String> result=c.finisher().apply(c.combiner().apply(a,b));c.accumulator().accept(a,"later");if(result.contains("later"))throw new Error();return result;}
                static <A> int nullFinisher(Collector<String,A,Set<String>> c){A a=c.supplier().get();c.accumulator().accept(a,null);try{c.finisher().apply(a);throw new Error();}catch(NullPointerException expected){return 1;}}
                public static int run(){Set<String> set=Stream.of("a","b","a").collect(Collectors.toUnmodifiableSet());int hash=set.hashCode();
                    hash=31*hash+combine(Collectors.toUnmodifiableSet()).hashCode();hash+=nullFinisher(Collectors.toUnmodifiableSet());
                    if(!set.equals(Set.of("a","b")) || !Stream.empty().collect(Collectors.toUnmodifiableSet()).isEmpty())throw new Error();
                    Set<Collector.Characteristics> flags=Collectors.toUnmodifiableSet().characteristics();if(!flags.contains(Collector.Characteristics.UNORDERED) || flags.contains(Collector.Characteristics.IDENTITY_FINISH))throw new Error();
                    try{set.add("a");throw new Error();}catch(UnsupportedOperationException e){hash++;}
                    try{set.remove("missing");throw new Error();}catch(UnsupportedOperationException e){hash++;}
                    try{set.removeIf(v->false);throw new Error();}catch(UnsupportedOperationException e){hash++;}
                    Iterator<String> cursor=set.iterator();cursor.next();try{cursor.remove();throw new Error();}catch(UnsupportedOperationException e){hash++;}
                    try{Stream.of("x",null).collect(Collectors.toUnmodifiableSet());throw new Error();}catch(NullPointerException e){hash++;}return hash;
                }
            }
            """);compare(List.of("run"));
    }
    @Test void executesOriginalPinnedSpringYamlSupportedTypesPipeline() throws Exception {
        Path repository=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()));
        Path beans=repository.resolve("org/springframework/spring-beans/6.1.5/spring-beans-6.1.5.jar"),core=repository.resolve("org/springframework/spring-core/6.1.5/spring-core-6.1.5.jar");
        assertDigest(beans,"bdcf61036293fc3475272e743b625d679e06d152b781134951c907b185f0f5f1");assertDigest(core,"476dac28509b3f47d0b5e895a639fa75ac2218f074ce214e44832a836c469f7e");
        for(Path artifact:List.of(beans,core))try(JarFile jar=new JarFile(artifact.toFile())){
            for(String name:artifact.equals(beans)?List.of("org/springframework/beans/factory/config/YamlProcessor"):List.of("org/springframework/util/Assert","org/springframework/util/ObjectUtils")){
                ClassNode original=new ClassNode();new ClassReader(jar.getInputStream(jar.getJarEntry(name+".class")).readAllBytes()).accept(original,0);original.signature=null;original.interfaces.clear();
                if(name.endsWith("YamlProcessor")){
                    original.access&=~ACC_ABSTRACT;original.fields.removeIf(f->!f.name.equals("supportedTypes"));original.fields.get(0).access=ACC_PUBLIC;
                    original.methods.removeIf(m->!m.name.equals("setSupportedTypes"));assertEquals(1,original.methods.size());
                    MethodNode ctor=new MethodNode(ACC_PUBLIC,"<init>","()V",null,null);ctor.visitCode();ctor.visitVarInsn(ALOAD,0);ctor.visitMethodInsn(INVOKESPECIAL,"java/lang/Object","<init>","()V",false);ctor.visitInsn(RETURN);ctor.visitMaxs(1,1);ctor.visitEnd();original.methods.add(ctor);
                }else{
                    original.fields.clear();original.methods.removeIf(m->name.endsWith("Assert")?!(m.name.equals("noNullElements") && m.desc.equals("([Ljava/lang/Object;Ljava/lang/String;)V")):!(m.name.equals("isEmpty") && m.desc.equals("([Ljava/lang/Object;)Z")));assertEquals(1,original.methods.size());
                }
                ClassWriter writer=new ClassWriter(0);original.accept(writer);writeClass(original.name,writer.toByteArray());
            }
        }
        fixtureClasspath=directory.toString();fixture("""
            import java.util.*;import org.springframework.beans.factory.config.YamlProcessor;
            public class Metadata {static class Widget{}public static int run(){YamlProcessor processor=new YamlProcessor();int hash=1;
                processor.setSupportedTypes(String.class,Widget.class,int[].class,String.class);Set<String> result=processor.supportedTypes;
                hash=31*hash+result.hashCode();if(result.size()!=3 || !result.contains("Metadata$Widget") || !result.contains("[I"))throw new Error();
                try{result.clear();throw new Error();}catch(UnsupportedOperationException e){hash++;}
                processor.setSupportedTypes((Class<?>[])null);if(!processor.supportedTypes.isEmpty() || result.size()!=3)throw new Error();
                processor.setSupportedTypes();if(!processor.supportedTypes.isEmpty())throw new Error();
                try{processor.setSupportedTypes(String.class,null);throw new Error();}catch(IllegalArgumentException e){hash++;}
                return hash;
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
