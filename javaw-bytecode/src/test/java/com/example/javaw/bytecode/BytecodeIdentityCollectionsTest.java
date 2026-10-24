package com.example.javaw.bytecode;

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

class BytecodeIdentityCollectionsTest implements Opcodes {
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

    @Test void distinctEqualObjectsNullsViewsAndClone() throws Exception {
        fixture("""
            import java.util.*;
            public class Metadata {
                static class Key {final int id;Key(int id){this.id=id;}public boolean equals(Object o){return o instanceof Key && ((Key)o).id==id;}public int hashCode(){return id;}}
                static class Child extends IdentityHashMap<Object,Object>{int extra=17;}
                public static int run(){int score=0;Object a=new Key(1),b=new Key(1),v=new Key(2),equalV=new Key(2);
                    Child map=new Child();map.put(a,v);map.put(b,equalV);map.put(null,null);
                    if(map.size()!=3 || map.get(a)!=v || map.get(b)!=equalV || map.containsKey(new Key(1)) || map.containsValue(new Key(2)))throw new Error();
                    if(!map.containsKey(null) || !map.containsValue(null))throw new Error();
                    Child copy=(Child)map.clone();if(copy.extra!=17 || !copy.equals(map))throw new Error();copy.remove(a);if(!map.containsKey(a))throw new Error();
                    if(map.keySet().hashCode()!=System.identityHashCode(a)+System.identityHashCode(b))throw new Error();
                    if(map.hashCode()!=(System.identityHashCode(a)^System.identityHashCode(v))+(System.identityHashCode(b)^System.identityHashCode(equalV)))throw new Error();
                    if(map.entrySet().contains(new AbstractMap.SimpleEntry<>(a,new Key(2))))throw new Error();
                    if(map.values().remove(new Key(2)))throw new Error();if(!map.values().remove(equalV) || map.containsKey(b))throw new Error();
                    map.put(b,equalV);Iterator<Object> keys=map.keySet().iterator();map.put(new Key(3),v);try{keys.next();throw new Error();}catch(ConcurrentModificationException e){score++;}
                    Map.Entry<Object,Object> entry=map.entrySet().iterator().next();Object oldKey=entry.getKey();entry.setValue(equalV);if(map.get(oldKey)!=equalV)throw new Error();
                    map.computeIfAbsent(a,k->equalV);map.compute(b,(k,old)->v);if(map.get(b)!=v)throw new Error();
                    map.replaceAll((k,old)->equalV);map.forEach((k,old)->{if(old!=equalV)throw new Error();});
                    IdentityHashMap<Object,Object> duplicate=new IdentityHashMap<>(map);if(!duplicate.equals(map))throw new Error();
                    if(duplicate.entrySet().remove(new AbstractMap.SimpleEntry<>(a,new Key(2))))throw new Error();
                    duplicate.keySet().removeAll(Collections.singleton(new Key(1)));if(duplicate.containsKey(a) || duplicate.containsKey(b))throw new Error();
                    try{new IdentityHashMap<>(-1);throw new Error();}catch(IllegalArgumentException e){score++;}
                    return score+map.size();
                }
            }
            """);compare(List.of("run"));
    }
    @Test void modernConditionalMutationUsesValueIdentity() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeTrue(Runtime.version().feature()>=20,"JDK 20+ identity-value conditional mutation contract");
        fixture("""
            import java.util.*;
            public class Metadata {public static int run(){
                Object key=new Object();Integer a=new Integer(1000),b=new Integer(1000),next=new Integer(2000);
                IdentityHashMap<Object,Integer> map=new IdentityHashMap<>();map.put(key,a);
                if(map.remove(key,b) || map.replace(key,b,next) || map.get(key)!=a)throw new Error();
                if(!map.replace(key,a,next) || map.get(key)!=next || !map.remove(key,next) || !map.isEmpty())throw new Error();
                map.put(null,null);if(!map.remove(null,null) || map.replace(key,null,next))throw new Error();
                return 17;
            }}
            """);compare(List.of("run"));
    }

    @Test void entryArraySnapshotsAndIteratorRemoval() throws Exception {
        fixture("""
            import java.util.*;
            public class Metadata {public static int run(){
                Object a=new Object(),b=new Object(),v=new Object(),next=new Object();IdentityHashMap<Object,Object> map=new IdentityHashMap<>();map.put(a,v);map.put(b,v);
                Object[] entries=map.entrySet().toArray();Map.Entry<Object,Object> snapshot=(Map.Entry<Object,Object>)entries[0];Object key=snapshot.getKey();snapshot.setValue(next);if(map.get(key)!=v)throw new Error();
                Map.Entry<Object,Object>[] typed=map.entrySet().toArray(new Map.Entry[3]);if(typed.length!=3 || typed[2]!=null)throw new Error();typed[0].setValue(next);if(map.get(typed[0].getKey())!=v)throw new Error();
                Iterator<Map.Entry<Object,Object>> cursor=map.entrySet().iterator();Map.Entry<Object,Object> live=cursor.next();cursor.remove();int score=map.size();
                try{live.getValue();throw new Error();}catch(IllegalStateException e){score++;}
                try{cursor.remove();throw new Error();}catch(IllegalStateException e){score++;}
                try{map.keySet().toArray(new String[0]);throw new Error();}catch(ArrayStoreException e){score++;}
                Set<Object> set=Collections.newSetFromMap(new IdentityHashMap<>());set.add(a);set.add(b);if(set.size()!=2 || set.contains(new Object()))throw new Error();
                return score+set.size();
            }}
            """);compare(List.of("run"));
    }
    @Test void executesUnchangedPinnedHibernateIdentitySet() throws Exception {
        Path repository=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()));
        Path artifact=repository.resolve("org/hibernate/orm/hibernate-core/6.4.4.Final/hibernate-core-6.4.4.Final.jar");assertDigest(artifact,"a1324b7c80c800826c9a5d74b61b0de768141f967c2b082650cb7bf4675570a7");fixtureClasspath=artifact.toString();
        fixture("""
            import java.util.*;import org.hibernate.internal.util.collections.IdentitySet;
            public class Metadata {
                static class Key {public boolean equals(Object other){return other instanceof Key;}public int hashCode(){return 1;}}
                public static int run(){Object a=new Key(),b=new Key();IdentitySet<Object> set=new IdentitySet<>(4);int score=0;
                    if(!set.add(a) || set.add(a) || !set.add(b) || !set.add(null) || set.size()!=3 || set.contains(new Key()))throw new Error();
                    if(!set.containsAll(Arrays.asList(a,b,null)))throw new Error();if(set.toArray().length!=3 || set.toArray(new Object[0]).length!=3)throw new Error();
                    if(!set.removeAll(Arrays.asList(a,null)) || set.contains(a) || !set.contains(b))throw new Error();set.addAll(Arrays.asList(a,b));
                    Iterator<Object> cursor=set.iterator();cursor.next();cursor.remove();if(set.size()!=1)throw new Error();
                    try{set.retainAll(Collections.emptyList());throw new Error();}catch(UnsupportedOperationException e){score++;}
                    set.clear();if(!set.isEmpty())throw new Error();IdentitySet<Object> empty=new IdentitySet<>();return score+empty.size();
                }
            }
            """);String name="org/hibernate/internal/util/collections/IdentitySet";byte[] original;
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
