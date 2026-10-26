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

class BytecodeCollectionFactoriesTest implements Opcodes {
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

    @Test void internalTypedAllocationThroughFactoryMethodReference() throws Exception {
        fixture("""
            import java.util.*;import java.util.function.*;
            public class Metadata {public static int run(){
                Function<Map<String,Integer>,Map<String,Integer>> copier=Map::copyOf;
                Map<String,Integer> source=new HashMap<>();source.put("alpha",7);source.put("beta",11);
                Map<String,Integer> copy=copier.apply(source);source.clear();
                return copy.size()*31+copy.get("alpha")+copy.get("beta");
            }}
            """);compare(List.of("run"));
    }

    @Test void immutableFactoriesCopyInputsAndRejectNullsAndDuplicates() throws Exception {
        fixture("""
            import java.util.*;
            public class Metadata {public static int run(){int hash=1;
                String[] source={"a","b","c"};List<String> list=List.of(source);source[0]="changed";
                if(!list.equals(Arrays.asList("a","b","c")) || !(list instanceof RandomAccess))throw new Error();
                ArrayList<String> mutable=new ArrayList<>(list);List<String> copy=List.copyOf(mutable);mutable.set(1,"changed");if(!copy.equals(list))throw new Error();
                hash=31*hash+List.of().hashCode()+List.of("x").hashCode()+List.of("x","y").hashCode();
                hash=31*hash+List.of(1,2,3,4,5,6,7,8,9,10).hashCode()+List.of(1,2,3,4,5,6,7,8,9,10,11).hashCode();
                hash=31*hash+Set.of("x","y").hashCode()+Set.copyOf(Arrays.asList("x","x","y")).hashCode();
                Map<String,Integer> map=Map.of("a",1,"b",2);HashMap<String,Integer> mutableMap=new HashMap<>(map);Map<String,Integer> mapCopy=Map.copyOf(mutableMap);mutableMap.clear();if(!mapCopy.equals(map))throw new Error();
                hash=31*hash+mapCopy.hashCode()+Map.of().hashCode()+Map.ofEntries(Map.entry("x",3),Map.entry("y",4)).hashCode();
                hash+=Map.of(1,1,2,2,3,3,4,4,5,5,6,6,7,7,8,8,9,9,10,10).size();
                try{list.set(0,"bad");throw new Error();}catch(UnsupportedOperationException e){hash++;}
                try{list.subList(0,1).clear();throw new Error();}catch(UnsupportedOperationException e){hash++;}
                try{map.entrySet().iterator().next().setValue(5);throw new Error();}catch(UnsupportedOperationException e){hash++;}
                try{Map.entry("x",1).setValue(2);throw new Error();}catch(UnsupportedOperationException e){hash++;}
                try{List.of("a",null);throw new Error();}catch(NullPointerException e){hash++;}
                try{List.of((String[])null);throw new Error();}catch(NullPointerException e){hash++;}
                try{List.copyOf(Arrays.asList("a",null));throw new Error();}catch(NullPointerException e){hash++;}
                try{Set.of("x","x");throw new Error();}catch(IllegalArgumentException e){hash++;}
                try{Map.of("x",1,"x",2);throw new Error();}catch(IllegalArgumentException e){hash++;}
                try{Map.ofEntries(Map.entry("x",1),null);throw new Error();}catch(NullPointerException e){hash++;}
                try{Map.of("x",null);throw new Error();}catch(NullPointerException e){hash++;}
                try{list.contains(null);throw new Error();}catch(NullPointerException e){hash++;}
                try{map.get(null);throw new Error();}catch(NullPointerException e){hash++;}
                return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void liveUnmodifiableViewsCloseIteratorEntryAndDefaultMutationPaths() throws Exception {
        fixture("""
            import java.util.*;
            public class Metadata {public static int run(){int hash=1;
                ArrayList<String> source=new ArrayList<>(Arrays.asList("a","b","c"));List<String> view=Collections.unmodifiableList(source),sub=view.subList(1,3);
                source.set(1,"changed");if(!sub.get(0).equals("changed") || !(sub instanceof RandomAccess))throw new Error();
                ListIterator<String> cursor=view.listIterator(1);hash+=cursor.nextIndex()+cursor.previousIndex()+cursor.previous().hashCode();
                try{cursor.set("x");throw new Error();}catch(UnsupportedOperationException e){hash++;}
                try{view.replaceAll(null);throw new Error();}catch(UnsupportedOperationException e){hash++;}
                try{Collections.unmodifiableList(new ArrayList<>()).clear();throw new Error();}catch(UnsupportedOperationException e){hash++;}
                try{view.removeIf(x->false);throw new Error();}catch(UnsupportedOperationException e){hash++;}
                int subHash=sub.hashCode();Iterator<String> iterator=view.iterator();source.add("new");try{iterator.next();throw new Error();}catch(ConcurrentModificationException e){hash++;}
                HashMap<String,Integer> sourceMap=new HashMap<>();sourceMap.put("a",1);Map<String,Integer> map=Collections.unmodifiableMap(sourceMap);
                Map.Entry<String,Integer> entry=map.entrySet().iterator().next();sourceMap.put("a",2);if(entry.getValue()!=2)throw new Error();
                try{entry.setValue(3);throw new Error();}catch(UnsupportedOperationException e){hash++;}
                Object[] entries=map.entrySet().toArray();try{((Map.Entry<String,Integer>)entries[0]).setValue(3);throw new Error();}catch(UnsupportedOperationException e){hash++;}
                Map.Entry<String,Integer>[] typed=map.entrySet().toArray(new Map.Entry[0]);try{typed[0].setValue(3);throw new Error();}catch(UnsupportedOperationException e){hash++;}
                try{map.entrySet().removeIf(x->false);throw new Error();}catch(UnsupportedOperationException e){hash++;}
                try{map.computeIfAbsent("a",x->null);throw new Error();}catch(UnsupportedOperationException e){hash++;}
                try{map.merge("a",1,(a,b)->a+b);throw new Error();}catch(UnsupportedOperationException e){hash++;}
                try{map.keySet().clear();throw new Error();}catch(UnsupportedOperationException e){hash++;}
                sourceMap.put("b",null);hash=31*hash+map.hashCode()+view.hashCode()+subHash;
                if(!map.values().contains(null) || !map.equals(sourceMap) || !view.equals(source))throw new Error();
                Collection<String> collection=Collections.unmodifiableCollection(source);if(collection.equals(source))throw new Error();
                try{Collections.unmodifiableMap(null);throw new Error();}catch(NullPointerException e){hash++;}
                return hash;
            }}
            """);compare(List.of("run"));
    }
    @Test void smallCollectionsEnumerationsAndMapBackedSets() throws Exception {
        fixture("""
            import java.util.*;
            public class Metadata {public static int run(){int hash=1;
                Collections.emptyMap().clear();Collections.emptySet().clear();Collections.emptyMap().putAll(Map.of());Collections.emptyMap().replaceAll((a,b)->a);
                if(Collections.EMPTY_MAP!=Collections.emptyMap() || Collections.EMPTY_SET!=Collections.emptySet() || Collections.EMPTY_LIST!=Collections.emptyList())throw new Error();
                Set<String> singleton=Collections.singleton(null);hash+=singleton.hashCode()+Collections.singletonMap(null,null).hashCode();
                if(singleton.remove("x"))throw new Error();
                try{singleton.remove(null);throw new Error();}catch(UnsupportedOperationException e){hash++;}
                try{singleton.removeIf(x->false);throw new Error();}catch(UnsupportedOperationException e){hash++;}
                Collections.singletonList("a").sort(null);
                try{Collections.singletonList("a").replaceAll(x->x);throw new Error();}catch(UnsupportedOperationException e){hash++;}
                List<String> copies=Collections.nCopies(3,"x");hash=31*hash+copies.hashCode()+copies.subList(1,3).hashCode();
                try{Collections.nCopies(-1,"x");throw new Error();}catch(IllegalArgumentException e){hash++;}
                Enumeration<String> enumeration=Collections.enumeration(copies);hash=31*hash+Collections.list(enumeration).hashCode();
                if(enumeration.hasMoreElements() || Collections.emptyEnumeration().asIterator().hasNext() || Collections.emptyListIterator().hasPrevious())throw new Error();
                try{Collections.emptyIterator().remove();throw new Error();}catch(IllegalStateException e){hash++;}
                try{Collections.emptyEnumeration().nextElement();throw new Error();}catch(NoSuchElementException e){hash++;}
                HashMap<String,Boolean> backing=new HashMap<>();Set<String> set=Collections.newSetFromMap(backing);set.add("x");set.add(null);if(!backing.containsKey(null) || !backing.get("x"))throw new Error();
                backing.put("y",true);hash=31*hash+set.hashCode();set.remove("x");set.clear();if(!backing.isEmpty())throw new Error();
                try{Collections.newSetFromMap(new HashMap<>(Map.of("x",true)));throw new Error();}catch(IllegalArgumentException e){hash++;}
                return hash;
            }}
            """);compare(List.of("run"));
    }
    @Test void executesUnchangedPinnedLogbackBeanDescription() throws Exception {
        Path repository=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()));
        Path artifact=repository.resolve("ch/qos/logback/logback-core/1.4.14/logback-core-1.4.14.jar");assertDigest(artifact,"f8c2f05f42530b1852739507c1792f0080167850ed8f396444c6913d6617a293");fixtureClasspath=artifact.toString();
        fixture("""
            import java.util.*;import java.lang.reflect.*;import ch.qos.logback.core.joran.util.beans.BeanDescription;
            public class Metadata {
                static class Description extends BeanDescription {Description(Map<String,Method> getters,Map<String,Method> setters){super(Metadata.class,getters,setters,new HashMap<>());}}
                public static int run()throws Exception{
                    int hash=1;Map<String,Method> getters=new HashMap<>(),setters=new HashMap<>();Method method=Metadata.class.getMethod("run");getters.put("a",method);
                    Description description=new Description(getters,setters);if(description.getClazz()!=Metadata.class || description.getGetter("a")!=method)throw new Error();
                    getters.put("b",method);if(description.getGetter("b")!=method || description.getSetter("x")!=null || description.getAdder("x")!=null)throw new Error();
                    hash+=description.getPropertyNameToGetter().size()+description.getPropertyNameToSetter().size()+description.getPropertyNameToAdder().size();
                    try{description.getPropertyNameToGetter().clear();throw new Error();}catch(UnsupportedOperationException e){hash++;}
                    try{description.getPropertyNameToGetter().entrySet().iterator().next().setValue(null);throw new Error();}catch(UnsupportedOperationException e){hash++;}
                    try{new Description(null,setters);throw new Error();}catch(NullPointerException e){hash++;}
                    return hash;
                }
            }
            """);
        String name="ch/qos/logback/core/joran/util/beans/BeanDescription";byte[] original;
        try(JarFile jar=new JarFile(artifact.toFile())){original=jar.getInputStream(jar.getJarEntry(name+".class")).readAllBytes();}
        writeClass(name,original);compare(List.of("run"));assertArrayEquals(original,Files.readAllBytes(directory.resolve(name+".class")));
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
