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

class BytecodeGroupingCollectorsTest implements Opcodes {
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

    @Test void groupingOrderDownstreamFinishingAndCombiners() throws Exception {
        fixture("""
            import java.util.*;import java.util.function.*;import java.util.stream.*;
            public class Metadata {
                static <A,R> R combine(Collector<String,A,R> c){A a=c.supplier().get(),b=c.supplier().get();c.accumulator().accept(a,"a");c.accumulator().accept(a,"bb");c.accumulator().accept(b,"c");c.accumulator().accept(b,"ddd");return c.finisher().apply(c.combiner().apply(a,b));}
                public static int run(){int hash=1;
                    Map<Integer,List<String>> grouped=Stream.of("a","bb","c","ddd").collect(Collectors.groupingBy(String::length));hash=31*hash+grouped.hashCode();
                    if(!grouped.get(1).equals(List.of("a","c")))throw new Error();grouped.get(1).add("x");hash=31*hash+grouped.hashCode();
                    hash=31*hash+combine(Collectors.groupingBy(String::length)).hashCode();
                    hash=31*hash+Stream.of("aa","b","cc","ddd").collect(Collectors.groupingBy(String::length,LinkedHashMap::new,Collectors.joining("|"))).toString().hashCode();
                    hash=31*hash+combine(Collectors.groupingBy(String::length,LinkedHashMap::new,Collectors.joining("|"))).toString().hashCode();
                    hash=31*hash+Stream.of("a","b","aa").collect(Collectors.groupingBy(String::length,Collectors.mapping(String::toString,Collectors.toUnmodifiableSet()))).hashCode();
                    Map<Integer,Integer> lengths=Stream.of("a","b","aa").collect(Collectors.groupingBy(String::length,Collector.of(()->new int[1],(int[] a,String v)->a[0]+=v.length(),(a,b)->{a[0]+=b[0];return a;},a->a[0])));
                    hash=31*hash+lengths.hashCode();if(lengths.get(1)!=2 || lengths.get(2)!=2)throw new Error();
                    if(!Collectors.groupingBy(Function.identity()).characteristics().contains(Collector.Characteristics.IDENTITY_FINISH) || !Collectors.groupingBy(Function.identity(),Collectors.joining()).characteristics().isEmpty())throw new Error();
                    int[] access={0};Collector<String,List<String>,Integer> custom=new Collector<>(){
                        public Supplier<List<String>> supplier(){return ArrayList::new;}
                        public BiConsumer<List<String>,String> accumulator(){return List::add;}
                        public BinaryOperator<List<String>> combiner(){return (a,b)->{a.addAll(b);return a;};}
                        public Function<List<String>,Integer> finisher(){access[0]++;return List::size;}
                        public Set<Collector.Characteristics> characteristics(){return Set.of();}
                    };
                    Collector<String,?,Map<Integer,Integer>> finished=Collectors.groupingBy(String::length,custom);
                    if(access[0]!=1)throw new Error();hash+=Stream.of("a","bb").collect(finished).size();if(access[0]!=1)throw new Error();
                    hash+=Stream.<String>empty().collect(Collectors.groupingBy((Function<String,String>)null)).size();
                    if(!Stream.<String>empty().collect(Collectors.groupingBy(String::length)).isEmpty())throw new Error();
                    return hash;
                }
            }
            """);compare(List.of("run"));
    }
    @Test void groupingNullsCallbackFailuresAndSingleUse() throws Exception {
        fixture("""
            import java.util.*;import java.util.function.*;import java.util.stream.*;
            public class Metadata {public static int run(){int hash=1;
                try{Stream.of("x").collect(Collectors.groupingBy(v->null));throw new Error();}catch(NullPointerException e){hash++;}
                try{Collectors.groupingBy(Function.identity(),(Collector<String,Object,Object>)null);throw new Error();}catch(NullPointerException e){hash++;}
                RuntimeException failure=new RuntimeException();Stream<String> used=Stream.of("x");
                try{used.collect(Collectors.groupingBy(v->{throw failure;}));throw new Error();}catch(RuntimeException e){if(e!=failure)throw new Error();hash++;}
                try{used.count();throw new Error();}catch(IllegalStateException e){hash++;}
                Collector<String,?,Integer> broken=Collectors.collectingAndThen(Collectors.toList(),v->{throw failure;});
                try{Stream.of("x").collect(Collectors.groupingBy(Function.identity(),broken));throw new Error();}catch(RuntimeException e){if(e!=failure)throw new Error();hash++;}
                return hash;
            }}
            """);compare(List.of("run"));
    }
    @Test void executesOriginalPinnedSpringCollectorFactoryAndMaps() throws Exception {
        Path repository=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()));
        Path core=repository.resolve("org/springframework/spring-core/6.1.5/spring-core-6.1.5.jar");
        Path mvc=repository.resolve("org/springframework/spring-webmvc/6.1.5/spring-webmvc-6.1.5.jar");
        Path tomcat=repository.resolve("org/apache/tomcat/embed/tomcat-embed-core/10.1.19/tomcat-embed-core-10.1.19.jar");
        assertDigest(core,"476dac28509b3f47d0b5e895a639fa75ac2218f074ce214e44832a836c469f7e");
        assertDigest(mvc,"f4fbe0c336d4c6b339d84c26418428a34c5e5408321da4e5ab0a5783b1793225");
        assertDigest(tomcat,"ca35025eda35fbef0798ed4ae79d0fbfa11d0a7a4d82fffd3e1a2bfa34abddbe");
        try(JarFile jar=new JarFile(core.toFile())){
            for(String simple:List.of("MultiValueMap","MultiValueMapAdapter","LinkedMultiValueMap","Assert","CollectionUtils")){
                ClassNode type=new ClassNode();new ClassReader(jar.getInputStream(jar.getJarEntry("org/springframework/util/"+simple+".class")).readAllBytes()).accept(type,0);
                if(simple.equals("Assert")){type.fields.clear();type.methods.removeIf(m->!m.name.equals("notNull") || !m.desc.equals("(Ljava/lang/Object;Ljava/lang/String;)V"));assertEquals(1,type.methods.size());}
                if(simple.equals("CollectionUtils")){type.fields.clear();type.methods.removeIf(m->!Set.of("newLinkedHashMap","computeMapInitialCapacity").contains(m.name));assertEquals(2,type.methods.size());}
                ClassWriter writer=new ClassWriter(0);type.accept(writer);writeClass(type.name,writer.toByteArray());
            }
        }
        try(JarFile jar=new JarFile(tomcat.toFile())){
            ClassNode type=new ClassNode();new ClassReader(jar.getInputStream(jar.getJarEntry("jakarta/servlet/http/Part.class")).readAllBytes()).accept(type,0);
            type.methods.removeIf(m->!m.name.equals("getName"));assertEquals(1,type.methods.size());ClassWriter writer=new ClassWriter(0);type.accept(writer);writeClass(type.name,writer.toByteArray());
        }
        try(JarFile jar=new JarFile(mvc.toFile())){
            ClassNode type=new ClassNode();new ClassReader(jar.getInputStream(jar.getJarEntry("org/springframework/web/servlet/function/DefaultServerRequest.class")).readAllBytes()).accept(type,0);
            MethodNode original=type.methods.stream().filter(m->m.name.equals("multipartData")).findFirst().orElseThrow();
            ClassWriter writer=new ClassWriter(0);writer.visit(V17,ACC_PUBLIC,"LibraryGrouping",null,"java/lang/Object",null);
            MethodNode factory=new MethodNode(ACC_PUBLIC|ACC_STATIC,"collector","()Ljava/util/stream/Collector;",null,null);
            boolean copying=false;int count=0;
            for(AbstractInsnNode instruction:original.instructions){
                if(instruction instanceof InvokeDynamicInsnNode)copying=true;
                if(copying && instruction.getOpcode()>=0){factory.instructions.add(instruction.clone(new HashMap<>()));count++;}
                if(instruction instanceof MethodInsnNode call && call.owner.equals("java/util/stream/Collectors") && call.name.equals("groupingBy"))break;
            }
            assertEquals(4,count);factory.instructions.add(new InsnNode(ARETURN));factory.maxStack=3;factory.maxLocals=0;factory.accept(writer);writer.visitEnd();writeClass("LibraryGrouping",writer.toByteArray());
        }
        fixtureClasspath=directory.toString();fixture("""
            import java.util.*;import java.util.stream.*;import jakarta.servlet.http.Part;import org.springframework.util.*;
            public class Metadata {
                static class Item implements Part {final String name;Item(String name){this.name=name;}public String getName(){return name;}}
                public static int run(){Item a=new Item("b"),b=new Item("a"),c=new Item("b");
                    Collector<Part,?,MultiValueMap<String,Part>> collector=(Collector)LibraryGrouping.collector();
                    MultiValueMap<String,Part> result=Stream.<Part>of(a,b,c).collect(collector);
                    if(result.get("b").size()!=2 || result.get("b").get(0)!=a || result.get("b").get(1)!=c || result.getFirst("a")!=b)throw new Error();
                    int hash=new ArrayList<>(result.keySet()).toString().hashCode();
                    Map<String,Part> first=result.toSingleValueMap();if(first.get("b")!=a || first.get("a")!=b)throw new Error();
                    result.add("b",b);hash+=result.get("b").size();result.set("c",c);hash+=result.size();
                    try{Stream.<Part>of(new Item(null)).collect(collector);throw new Error();}catch(NullPointerException expected){hash++;}
                    if(!Stream.<Part>empty().collect(collector).isEmpty())throw new Error();return hash;
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
