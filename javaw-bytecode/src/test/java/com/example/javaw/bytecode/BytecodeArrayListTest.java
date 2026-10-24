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

class BytecodeArrayListTest implements Opcodes {
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

    @Test void matchesMutableListsViewsIteratorsAndTypedArrays() throws Exception {
        fixture("""
            import java.util.*;
            public class Metadata {
                static class Derived extends ArrayList<String>{int marker=7;}
                public static String run(){
                    ArrayList<String> list=new ArrayList<>(Arrays.asList("a",null,"b","a"));list.add(1,"x");list.set(2,"y");list.addAll(2,list);list.remove("a");list.remove(0);
                    ListIterator<String> it=list.listIterator(2);it.previous();it.set("P");it.add("I");it.next();it.remove();
                    List<String> view=list.subList(1,list.size()-1);List<String> nested=view.subList(0,2);nested.add("N");nested.remove(0);nested.set(0,"S");
                    String text=list+"|"+view+"|"+nested+"|"+list.indexOf("a")+"|"+list.lastIndexOf("a")+"|"+list.hashCode();
                    ArrayList<String> copy=(ArrayList<String>)list.clone();copy.clear();if(copy.isEmpty() && !list.isEmpty())text+="|clone";
                    Derived derived=new Derived();derived.add("d");Derived cloned=(Derived)derived.clone();cloned.set(0,"e");if(cloned.marker==7 && derived.get(0).equals("d"))text+="|subclass";
                    list.removeAll(Arrays.asList("a","b"));list.retainAll(Arrays.asList("S","N","I","x"));return text+"|"+list;
                }
                public static int arrays(){
                    ArrayList<String> list=new ArrayList<>(Arrays.asList("a","b"));String[] small=list.toArray(new String[0]);String[] large={"x","x","x","tail"};
                    int score=small.getClass()==String[].class && small[1].equals("b")?1:0;
                    if(list.toArray(large)==large && large[2]==null && large[3].equals("tail"))score+=10;
                    Object[] copy=list.toArray();copy[0]="z";if(list.get(0).equals("a"))score+=100;
                    try{list.toArray(new Integer[0]);}catch(ArrayStoreException expected){score+=1000;}
                    String[] backing={"a","b"};List<String> fixed=Arrays.asList(backing);fixed.set(0,"c");backing[1]="d";if(fixed.get(1).equals("d") && backing[0].equals("c"))score+=10000;
                    try{fixed.add("e");}catch(UnsupportedOperationException expected){score+=100000;}
                    return score;
                }
                public static int failures(){
                    int score=0;ArrayList<String> list=new ArrayList<>();Iterator<String> iterator=list.iterator();
                    try{iterator.next();}catch(NoSuchElementException expected){score++;}try{iterator.remove();}catch(IllegalStateException expected){score+=10;}
                    list.add("a");try{iterator.next();}catch(ConcurrentModificationException expected){score+=100;}
                    List<String> view=list.subList(0,1);ListIterator<String> viewIterator=view.listIterator();list.add("b");try{view.get(0);}catch(ConcurrentModificationException expected){score+=1000;}
                    try{viewIterator.previous();}catch(ConcurrentModificationException expected){score+=100000000;}
                    iterator=list.iterator();list.set(0,"c");if(iterator.next().equals("c"))score+=10000;
                    iterator=list.iterator();list.ensureCapacity(100);try{iterator.next();}catch(ConcurrentModificationException expected){score+=100000;}
                    try{list.get(-1);}catch(IndexOutOfBoundsException expected){score+=1000000;}
                    try{list.subList(1,0);}catch(IllegalArgumentException expected){score+=10000000;}
                    return score;
                }
            }
            """);compare(List.of("run","arrays","failures"));
    }

    @Test void executesUnchangedPinnedByteBuddyCompoundList() throws Exception {
        Path repository=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()));Path artifact=repository.resolve("net/bytebuddy/byte-buddy/1.14.11/byte-buddy-1.14.11.jar");
        assertEquals("62ae28187ed2b062813da6a9d567bfee733c341582699b62dd980230729a0313",java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(artifact))));
        fixtureClasspath=artifact.toString();
        fixture("""
            import java.util.*;import net.bytebuddy.utility.CompoundList;
            public class Metadata {
                public static String run(){
                    List<String> first=new ArrayList<>(Arrays.asList("a",null));List<String> second=Arrays.asList("b","c");List<String> empty=Collections.emptyList();
                    List<String> prefix=CompoundList.of("start",first),suffix=CompoundList.of(first,"end"),pair=CompoundList.of(first,second),triple=CompoundList.of(first,empty,second);first.set(0,"changed");
                    return prefix+"|"+suffix+"|"+pair+"|"+triple+"|"+CompoundList.of("x",empty)+"|"+CompoundList.of(empty,(String)null)+"|"+pair.equals(triple)+"|"+pair.hashCode();
                }
                public static int failures(){
                    int score=0;List<String> singleton=CompoundList.of("x",Collections.<String>emptyList());try{singleton.add("y");}catch(UnsupportedOperationException expected){score++;}
                    try{singleton.set(0,"y");}catch(UnsupportedOperationException expected){score+=10;}
                    try{CompoundList.of("x",(List<String>)null);}catch(NullPointerException expected){score+=100;}
                    List<String> mutable=CompoundList.of(Collections.<String>emptyList(),Collections.<String>emptyList());mutable.add("yes");if(mutable.size()==1)score+=1000;return score;
                }
            }
            """);
        Path selected=directory.resolve("net/bytebuddy/utility/CompoundList.class");Files.createDirectories(selected.getParent());byte[] original;
        try(JarFile jar=new JarFile(artifact.toFile())){original=jar.getInputStream(jar.getJarEntry("net/bytebuddy/utility/CompoundList.class")).readAllBytes();}
        Files.write(selected,original);compare(List.of("run","failures"));assertArrayEquals(original,Files.readAllBytes(selected));
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
