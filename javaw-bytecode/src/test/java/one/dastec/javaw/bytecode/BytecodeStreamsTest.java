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

class BytecodeStreamsTest implements Opcodes {
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

    @Test void lazySourcesAndShortCircuiting() throws Exception {
        fixture("""
            import java.util.*;import java.util.function.*;import java.util.stream.*;
            public class Metadata { public static int run(){

 int[] calls={0};String[] source={"a","bb","ccc","dddd"};
 Stream<String> pipeline=Arrays.stream(source).peek(v->calls[0]++).filter(v->v.length()>1).map(v->v+"!");
 if(calls[0]!=0)throw new Error();source[1]="xy";
 String first=pipeline.findFirst().get();if(calls[0]!=2 || !first.equals("xy!"))throw new Error();int hash=first.hashCode();
 List<String> values=new ArrayList<>();values.add("a");Stream<String> late=values.stream();values.add("b");hash+=late.collect(Collectors.joining(",")).hashCode();
 hash=31*hash+Arrays.stream(source,1,3).collect(Collectors.joining()).hashCode();
 hash+=Stream.iterate(1,n->n+1).skip(3).limit(4).reduce(0,Integer::sum);
 hash+=Stream.iterate(1,n->n<6,n->n+1).reduce(0,Integer::sum);
 hash+=Stream.generate(()->7).limit(3).reduce(0,Integer::sum);
 Stream.generate(()->{throw new Error();}).limit(0).forEach(v->{throw new Error();});
 hash+=Stream.of(1,2,3,2,1).takeWhile(v->v<3).reduce(0,Integer::sum);
 hash+=Stream.of(1,2,3,2,1).dropWhile(v->v<3).reduce(0,Integer::sum);
 if(!Stream.of(1,2,3).anyMatch(v->v==2) || !Stream.<Integer>empty().allMatch(v->false) || !Stream.of(1,2).noneMatch(v->v>3))throw new Error();
 int[] visited={0};if(!Stream.iterate(1,n->n+1).peek(v->visited[0]++).anyMatch(v->v==3) || visited[0]!=3)throw new Error();
 hash+=Optional.of("ok").stream().findAny().get().hashCode()+Optional.empty().stream().count();
 hash+=Stream.ofNullable(null).count()+Stream.ofNullable("x").count();
 return hash;

            }}
            """);compare(List.of("run"));
    }

    @Test void orderingArraysAndReductions() throws Exception {
        fixture("""
            import java.util.*;import java.util.function.*;import java.util.stream.*;
            public class Metadata { public static int run(){

 int hash=1;
 String[] result=Stream.of("bb","a","cc","a","d").distinct().sorted(Comparator.comparingInt(String::length)).toArray(String[]::new);
 hash=31*hash+Arrays.hashCode(result);if(!Arrays.equals(result,new String[]{"a","d","bb","cc"}))throw new Error();
 hash+=Arrays.hashCode(Stream.of(3,1,2).sorted().toArray());
 hash+=Stream.of(3,1,2).min(Integer::compare).get()+Stream.of(3,1,2).max(Integer::compare).get();
 hash+=Stream.of("a","b","c").reduce("",String::concat).hashCode();
 hash+=Stream.of("a","bb","ccc").reduce(0,(count,value)->count+value.length(),Integer::sum);
 hash+=Stream.of(1,2,3).reduce(Integer::sum).get();if(Stream.empty().reduce((a,b)->a).isPresent())throw new Error();
 List<Integer> seen=new ArrayList<>();Stream.of(1,2,3).forEachOrdered(seen::add);hash=31*hash+seen.hashCode();
 Iterator<String> cursor=Stream.of("x","y").map(v->v+"!").iterator();if(!cursor.hasNext() || !cursor.hasNext())throw new Error();hash+=cursor.next().hashCode();cursor.forEachRemaining(v->seen.add(v.length()));hash+=seen.hashCode();
 try{cursor.next();throw new Error();}catch(NoSuchElementException expected){hash++;}
 try{Stream.<String>of((String)null).findFirst();throw new Error();}catch(NullPointerException expected){hash++;}
 try{Stream.of("x").reduce((a,b)->null); }catch(NullPointerException unexpected){throw new Error();}
 try{Stream.of("x","y").reduce((a,b)->null);throw new Error();}catch(NullPointerException expected){hash++;}
 return hash;

            }}
            """);compare(List.of("run"));
    }

    @Test void flattenedResourcesAndFailures() throws Exception {
        fixture("""
            import java.util.*;import java.util.function.*;import java.util.stream.*;
            public class Metadata { public static int run(){

 int[] mapped={0},closed={0};
 List<Integer> values=Stream.of(1,2,3).flatMap(v->{mapped[0]++;return Stream.of(v,v+10).onClose(()->closed[0]++);}).limit(3).toList();
 if(mapped[0]!=2 || closed[0]!=2)throw new Error();int hash=values.hashCode();
 hash+=Stream.of(1,2,3).flatMap(v->v==2?null:Stream.of(v)).reduce(0,Integer::sum);
 hash+=Stream.of(1).flatMap(v->Stream.iterate(0,n->n+1)).limit(3).reduce(0,Integer::sum);
 RuntimeException primary=new RuntimeException("primary"),closing=new RuntimeException("closing");
 try{Stream.of(1).flatMap(v->Stream.of(v).onClose(()->{throw closing;})).forEach(v->{throw primary;});throw new Error();}
 catch(RuntimeException actual){if(actual!=primary || actual.getSuppressed().length!=1 || actual.getSuppressed()[0]!=closing)throw new Error();hash++;}
 try{Stream.of(1).flatMap(v->Stream.of(v).onClose(()->{throw closing;})).findFirst();throw new Error();}catch(RuntimeException actual){if(actual!=closing)throw new Error();hash++;}
 int[] outerClosed={0};Stream<Integer> stream=Stream.of(1,2).onClose(()->outerClosed[0]++);stream.count();if(outerClosed[0]!=0)throw new Error();stream.close();if(outerClosed[0]!=1)throw new Error();
 return hash;

            }}
            """);compare(List.of("run"));
    }

    @Test void singleUseAndCloseHandlers() throws Exception {
        fixture("""
            import java.util.*;import java.util.function.*;import java.util.stream.*;
            public class Metadata { public static int run(){

 int hash=1;Stream<Integer> source=Stream.of(1,2),tail=source.map(v->v+1);
 try{source.count();throw new Error();}catch(IllegalStateException expected){hash++;}hash+=tail.count();
 try{tail.count();throw new Error();}catch(IllegalStateException expected){hash++;}
 try{tail.onClose(()->{});throw new Error();}catch(IllegalStateException expected){hash++;}
 int[] trace={0};RuntimeException first=new RuntimeException(),second=new RuntimeException();
 Stream<Integer> closing=Stream.of(1).onClose(()->{trace[0]=trace[0]*10+1;throw first;}).onClose(()->{trace[0]=trace[0]*10+2;throw second;}).onClose(()->trace[0]=trace[0]*10+3);
 try{closing.close();throw new Error();}catch(RuntimeException actual){if(actual!=first || actual.getSuppressed().length!=1 || actual.getSuppressed()[0]!=second)throw new Error();hash++;}
 closing.close();if(trace[0]!=123)throw new Error();hash+=trace[0];try{closing.findAny();throw new Error();}catch(IllegalStateException expected){hash++;}
 Stream<Integer> parent=Stream.of(1);Stream<Integer> child=parent.filter(v->true);parent.close();try{child.count();throw new Error();}catch(IllegalStateException expected){hash++;}
 Stream<Integer> left=Stream.of(1,2).onClose(()->trace[0]++),right=Stream.of(3).onClose(()->trace[0]++);Stream<Integer> joined=Stream.concat(left,right);
 try{left.count();throw new Error();}catch(IllegalStateException expected){hash++;}hash+=joined.reduce(0,Integer::sum);joined.close();hash+=trace[0];
 Stream<Integer> failed=Stream.of(1);try{failed.collect(()->{throw first;},(a,b)->{},(a,b)->{});throw new Error();}catch(RuntimeException expected){if(expected!=first)throw new Error();}
 try{failed.count();throw new Error();}catch(IllegalStateException expected){hash++;}
 if(Stream.empty().isParallel())throw new Error();return hash;

            }}
            """);compare(List.of("run"));
    }

    @Test void collectorsFactoriesCombinersAndNulls() throws Exception {
        fixture("""
            import java.util.*;import java.util.function.*;import java.util.stream.*;
            public class Metadata { public static int run(){

 int hash=1;
 List<String> list=Stream.of("a",null,"b").collect(Collectors.toList());list.add("c");hash+=list.hashCode();
 Set<String> set=Stream.of("a","b","a",null).collect(Collectors.toSet());hash+=set.hashCode();
 List<Integer> custom=Stream.of(1,2,3).collect(ArrayList<Integer>::new,List<Integer>::add,List<Integer>::addAll);hash+=custom.hashCode();
 hash+=Stream.of("a",null,"b").collect(Collectors.joining(",","[","]")).hashCode();hash+=Stream.<String>empty().collect(Collectors.joining(",","[","]")).hashCode();
 hash+=Stream.of(1,2,3).collect(Collectors.mapping(v->"x"+v,Collectors.joining("|"))).hashCode();
 hash+=Stream.of("a","bb").collect(Collectors.collectingAndThen(Collectors.toList(),List::size));
 Collector<String,List<String>,String> collector=Collector.of(ArrayList<String>::new,List<String>::add,(a,b)->{a.addAll(b);return a;},v->v.get(0)+","+v.get(1));
 List<String> a=collector.supplier().get(),b=collector.supplier().get();collector.accumulator().accept(a,"a");collector.accumulator().accept(b,"b");hash+=collector.finisher().apply(collector.combiner().apply(a,b)).hashCode();
 if(collector.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH) || !Collectors.toList().characteristics().contains(Collector.Characteristics.IDENTITY_FINISH) || !Collectors.toSet().characteristics().contains(Collector.Characteristics.UNORDERED))throw new Error();
 try{Collectors.toList().characteristics().clear();throw new Error();}catch(UnsupportedOperationException expected){hash++;}
 try{Stream.of("a",null).collect(Collectors.toUnmodifiableList());throw new Error();}catch(NullPointerException expected){hash++;}
 return hash;

            }}
            """);compare(List.of("run"));
    }

    @Test void immutableListsAndTypedFailures() throws Exception {
        fixture("""
            import java.util.*;import java.util.function.*;import java.util.stream.*;
            public class Metadata { public static int run(){

 int hash=1;List<String> list=Stream.of("a",null,"b").toList();hash+=list.hashCode();Object[] copy=list.toArray();copy[0]="changed";if(!list.get(0).equals("a"))throw new Error();
 for(List<String> value:Arrays.asList(list,Stream.<String>empty().toList(),list.subList(1,1),Stream.of("a").collect(Collectors.toUnmodifiableList()))){
 try{value.clear();throw new Error();}catch(UnsupportedOperationException expected){hash++;}
 try{value.remove("absent");throw new Error();}catch(UnsupportedOperationException expected){hash++;}
 try{value.addAll(Collections.emptyList());throw new Error();}catch(UnsupportedOperationException expected){hash++;}
 try{value.removeIf(v->false);throw new Error();}catch(UnsupportedOperationException expected){hash++;}
 try{value.sort(null);throw new Error();}catch(UnsupportedOperationException expected){hash++;}}
 try{Stream.of(1).limit(-1);throw new Error();}catch(IllegalArgumentException expected){hash++;}
 try{Stream.of(1).skip(-1);throw new Error();}catch(IllegalArgumentException expected){hash++;}
 try{Stream.of(1).filter(null);throw new Error();}catch(NullPointerException expected){hash++;}
 try{Stream.of(1).map(null);throw new Error();}catch(NullPointerException expected){hash++;}
 try{Stream.of(1).flatMap(null);throw new Error();}catch(NullPointerException expected){hash++;}
 try{Stream.of(1).sorted(null);throw new Error();}catch(NullPointerException expected){hash++;}
 try{Arrays.stream(new String[]{"a"},1,0);throw new Error();}catch(ArrayIndexOutOfBoundsException expected){hash++;}
 try{Stream.of(1).toArray(String[]::new);throw new Error();}catch(ArrayStoreException expected){hash++;}
 return hash;

            }}
            """);compare(List.of("run"));
    }

    @Test void executesUnchangedPinnedMicrometerKeyNames() throws Exception {
        Path artifact=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString())).resolve("io/micrometer/micrometer-commons/1.12.4/micrometer-commons-1.12.4.jar");
        assertDigest(artifact,"62e46f3b46e234e38f8333cf3e3a5ff54d0a9f3ca890e4b9773674d180c36914");
        fixtureClasspath=artifact.toString();
        fixture("""
            import java.util.*;import java.util.stream.*;import io.micrometer.common.*;import io.micrometer.common.docs.*;
            public class Metadata {public static int run(){
                KeyName a=()->"alpha",b=()->"beta";int hash=1;
                KeyName[] input={a,b};KeyName[] merged=KeyName.merge(input,new KeyName[0],new KeyName[]{a});
                if(merged.length!=3 || merged[0]!=a || merged[1]!=b || merged[2]!=a || merged.getClass()!=KeyName[].class)throw new Error();
                input[0]=b;if(merged[0]!=a)throw new Error();
                hash+=Arrays.stream(merged).map(KeyName::asString).collect(Collectors.joining(",")).hashCode();
                hash+=a.withValue("one").hashCode();hash+=b.withValue("two").toString().hashCode();
                hash+=a.withValue("three",v->v!=null).getValue().hashCode();
                if(!a.isRequired() || KeyName.merge().length!=0)throw new Error();
                try{KeyName.merge(new KeyName[][]{null});throw new Error();}catch(NullPointerException expected){hash++;}
                return hash;
            }}
            """);
        try(JarFile jar=new JarFile(artifact.toFile())){
            for(String name:new String[]{"io/micrometer/common/docs/KeyName","io/micrometer/common/KeyValue","io/micrometer/common/ImmutableKeyValue","io/micrometer/common/ValidatedKeyValue"}){
                byte[] original=jar.getInputStream(jar.getJarEntry(name+".class")).readAllBytes();writeClass(name,original);
                assertArrayEquals(original,Files.readAllBytes(directory.resolve(name+".class")));
            }
        }
        compare(List.of("run"));
    }

    @Test void java8PrivateInterfaceHelpersMatchJvmAndOlderVersionsFailAdmission() throws Exception {
        for(int synthetic:new int[]{0,ACC_SYNTHETIC}){
            ClassWriter writer=new ClassWriter(0);writer.visit(V1_8,ACC_PUBLIC|ACC_INTERFACE|ACC_ABSTRACT,"Helper",null,"java/lang/Object",null);
            var helper=writer.visitMethod(ACC_PRIVATE|ACC_STATIC|synthetic,"helper","()I",null,null);helper.visitCode();helper.visitInsn(ICONST_2);helper.visitInsn(IRETURN);helper.visitMaxs(1,0);helper.visitEnd();
            var run=writer.visitMethod(ACC_PUBLIC|ACC_STATIC,"run","()I",null,null);run.visitCode();run.visitMethodInsn(INVOKESTATIC,"Helper","helper","()I",true);run.visitInsn(IRETURN);run.visitMaxs(1,0);run.visitEnd();writer.visitEnd();
            byte[] valid=writer.toByteArray();writeClass("Helper",valid);fixtureClasspath=directory.toString();
            fixture("public class Metadata {public static int run(){return Helper.run();}}");compare(List.of("run"));
            byte[] old=valid.clone();old[7]=(byte)V1_7;writeClass("Helper",old);
            try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)){
                assertThrows(ClassFormatError.class,()->loader.loadClass("Helper").getDeclaredMethods());
            }
            var failure=assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(false))));
            assertTrue(failure.getMessage().contains("pre-Java-8 interface methods"),failure.getMessage());
        }
    }

    @Test void unsupportedLongAndParallelOperationsFailAdmission() throws Exception {
        fixture("public class Metadata {public static long run(){return java.util.stream.Stream.of(1).mapToLong(Integer::longValue).sum();}}");
        var primitive=assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(false))));
        assertTrue(primitive.getMessage().contains("java/util/stream/Stream.mapToLong"),primitive.getMessage());
        fixture("public class Metadata {public static long run(){return java.util.stream.Stream.of(1).parallel().count();}}");
        var parallel=assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(false))));
        assertTrue(parallel.getMessage().contains("java/util/stream/Stream.parallel"),parallel.getMessage());
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
