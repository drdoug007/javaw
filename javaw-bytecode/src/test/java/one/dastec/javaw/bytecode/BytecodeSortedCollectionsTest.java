package one.dastec.javaw.bytecode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import javax.tools.ToolProvider;
import java.io.*;import java.nio.file.*;import java.util.*;import java.util.concurrent.TimeUnit;import java.util.jar.*;
import static org.junit.jupiter.api.Assertions.*;
class BytecodeSortedCollectionsTest {
 @TempDir Path directory;
 private final String fixtureClassName="Metadata";
 private String fixtureClasspath="";
 private final Map<String,byte[]> carriers=new HashMap<>();
 @Test void orderedMap() throws Exception {fixture("""
import java.util.*;
public class Metadata {public static int run(int group){int h=1;for(Comparator<String> cmp:Arrays.<Comparator<String>>asList(null,Comparator.reverseOrder(),String.CASE_INSENSITIVE_ORDER)){
 TreeMap<String,Integer> m=new TreeMap<>(cmp);for(String k:new String[]{"c","A","b","a","z"}){Integer old=m.put(k,k.length());h=31*h+(old==null?7:old);}
 h=31*h+m.toString().hashCode();h=31*h+m.firstKey().hashCode();h=31*h+m.lastKey().hashCode();h=31*h+m.size();m.put("b",null);h=31*h+(m.containsKey("b")?1:0);h=31*h+(m.get("b")==null?1:0);m.remove("z");
 TreeMap<String,Integer> copy=new TreeMap<>((SortedMap<String,Integer>)m);h=31*h+copy.toString().hashCode();h=31*h+(copy.comparator()==cmp?1:0);TreeMap<String,Integer> natural=new TreeMap<>((Map<String,Integer>)m);h=31*h+natural.toString().hashCode();
 }
 TreeMap<String,Integer> m=new TreeMap<>();try{m.firstKey();throw new Error();}catch(NoSuchElementException e){h++;}try{m.get(null);throw new Error();}catch(NullPointerException e){h++;}try{((Map)m).put(new Object(),1);throw new Error();}catch(ClassCastException e){h++;}return h;}}
""");compare(1);}
 @Test void mapNavigation() throws Exception {fixture("""
import java.util.*;public class Metadata {public static int run(int g){int h=1;for(Comparator<Integer> order:Arrays.<Comparator<Integer>>asList(null,Comparator.reverseOrder())){NavigableMap<Integer,String> m=new TreeMap<>(order);for(int k:new int[]{1,3,5})m.put(k,"v"+k);for(int k=0;k<=6;k++){h=31*h+Objects.hash(m.lowerKey(k),m.floorKey(k),m.ceilingKey(k),m.higherKey(k));h=31*h+Objects.hash(m.lowerEntry(k),m.floorEntry(k),m.ceilingEntry(k),m.higherEntry(k));}Map.Entry<Integer,String> old=m.firstEntry();m.put(old.getKey(),"changed");h=31*h+old.hashCode();try{old.setValue("bad");throw new Error();}catch(UnsupportedOperationException e){h++;}while(!m.isEmpty()){h=31*h+m.pollLastEntry().hashCode();}h=31*h+Objects.hash(m.firstEntry(),m.lastEntry(),m.pollFirstEntry(),m.pollLastEntry());}return h;}}
""");compare(1);}
 @Test void sortedRanges() throws Exception {fixture("""
import java.util.*;public class Metadata {public static int run(int g){int h=1;TreeMap<Integer,String> root=new TreeMap<>();for(int i=0;i<8;i++)root.put(i,"v"+i);SortedMap<Integer,String> v=root.subMap(2,6);h=31*h+v.toString().hashCode();root.put(3,"new");h=31*h+v.get(3).hashCode();v.put(4,"view");v.remove(2);h=31*h+root.toString().hashCode();h=31*h+v.headMap(5).toString().hashCode();h=31*h+v.tailMap(4).toString().hashCode();h=31*h+v.subMap(4,4).size();for(int i=0;i<4;i++)try{if(i==0)v.put(6,"bad");if(i==1)v.subMap(1,5);if(i==2)v.tailMap(6);if(i==3)root.subMap(5,3);throw new Error();}catch(IllegalArgumentException e){h++;}v.clear();h=31*h+root.toString().hashCode();return h;}}
""");compare(1);}
 @Test void descendingRanges() throws Exception {fixture("""
import java.util.*;public class Metadata {public static int run(int g){int h=1;for(boolean reverse:new boolean[]{false,true})for(boolean fi:new boolean[]{false,true})for(boolean ti:new boolean[]{false,true}){TreeMap<Integer,Integer> root=new TreeMap<>();for(int i=0;i<8;i++)root.put(i,i*i);NavigableMap<Integer,Integer> m=reverse?root.descendingMap():root;NavigableMap<Integer,Integer> v=m.subMap(reverse?6:1,fi,reverse?1:6,ti);h=31*h+v.toString().hashCode();h=31*h+v.descendingMap().toString().hashCode();h=31*h+v.headMap(4,true).toString().hashCode();h=31*h+v.tailMap(4,false).toString().hashCode();for(int k=0;k<8;k++)h=31*h+Objects.hash(v.lowerKey(k),v.floorKey(k),v.ceilingKey(k),v.higherKey(k));h=31*h+Objects.hash(v.firstEntry(),v.lastEntry(),v.pollFirstEntry(),v.pollLastEntry());h=31*h+root.toString().hashCode();h=31*h+(m.comparator()==null?7:m.comparator().compare(1,2));}NavigableMap<Integer,Integer> v=new TreeMap<Integer,Integer>().subMap(1,false,4,false);h=31*h+v.headMap(1,false).size();try{v.headMap(1,true);throw new Error();}catch(IllegalArgumentException e){h++;}return h;}}
""");compare(1);}
    private String classpath(){return directory+(fixtureClasspath.isEmpty()?"":File.pathSeparator+fixtureClasspath);}

    /** Preserve selected original method instructions; omit unrelated library state and initialization. */
    private ClassNode selectedLibrary(String relative,String artifact,String hash,java.util.function.Predicate<String> select) throws Exception {
        Path jar=Path.of(System.getProperty("user.home"),".m2/repository",artifact);
        assertTrue(Files.isRegularFile(jar),"Pinned artifact required: "+jar);
        assertEquals(hash,HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar))));
        fixtureClasspath=jar.toString();byte[] original;
        try(JarFile input=new JarFile(jar.toFile())){original=input.getInputStream(input.getJarEntry(relative)).readAllBytes();}
        Path target=directory.resolve(relative);Files.createDirectories(target.getParent());Files.write(target,original);
        ClassNode full=new ClassNode();new ClassReader(original).accept(full,0);
        ClassNode carrier=new ClassNode();carrier.visit(full.version,full.access,full.name,null,full.superName,null);
        carrier.methods.addAll(full.methods.stream().filter(m->select.test(m.name)).toList());
        return carrier;
    }
    private void carrier(ClassNode node){ClassWriter writer=new ClassWriter(0);node.accept(writer);carriers.put(node.name+".class",writer.toByteArray());}

    private byte[] fixture(String source) throws Exception {
        try(var old=Files.list(directory)){for(Path path:old.filter(v->v.getFileName().toString().matches("Metadata.*\\.class")).toList())Files.delete(path);}
        Path file=directory.resolve("Metadata.java");Files.writeString(file,source);
        List<String> options=new ArrayList<>(List.of("-g","-parameters","--release","17","-d",directory.toString()));
        options.add("-classpath");options.add(classpath());
        Path oracle=directory.resolve("CaseOracle.java");Files.writeString(oracle,"public class CaseOracle {public static void main(String[] args)throws Exception{if(Runtime.version().feature()!=17)throw new Error();var method=Class.forName(args[0]).getMethod(\"run\",int.class);System.out.print(\"[\");for(int i=0;i<Integer.parseInt(args[1]);i++){if(i>0)System.out.print(\",\");System.out.print(method.invoke(null,i));}System.out.print(\"]\");}}");
        options.add(file.toString());options.add(oracle.toString());
        assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,options.toArray(String[]::new)));
        return jar(false);
    }

    private byte[] jar(boolean reverse) throws Exception {
        List<Path> files;
        try(var paths=Files.walk(directory)) {
            files=new ArrayList<>(paths.filter(path->path.toString().endsWith(".class") && !path.getFileName().toString().equals("CaseOracle.class")).sorted().toList());
        }
        if(reverse)Collections.reverse(files);
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(JarOutputStream jar=new JarOutputStream(bytes)) {
            for(Path file:files){
                String entry=directory.relativize(file).toString();
                jar.putNextEntry(new JarEntry(directory.relativize(file).toString()));jar.write(carriers.containsKey(entry)?carriers.get(entry):Files.readAllBytes(file));jar.closeEntry();}
        }
        return bytes.toByteArray();
    }

    private void compare(int groups) throws Exception {
        Path java=Path.of(System.getProperty("javaw.jdk17.home",System.getProperty("user.home")+"/Library/Java/JavaVirtualMachines/graalvm-ce-17.0.9/Contents/Home"),"bin/java");
        assertTrue(Files.isExecutable(java),"A JDK 17 oracle is required (javaw.jdk17.home)");
        run(java.toString(),"-Djava.locale.providers=CLDR","-Duser.language=","-Duser.country=","-Duser.variant=","-cp",classpath(),"CaseOracle",fixtureClassName,Integer.toString(groups));
        String expected=Files.readString(directory.resolve("command.log")).trim();
        for(boolean reverse:new boolean[]{false,true})
            execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),expected);
    }

    private void run(String... arguments) throws Exception {
        Path log=directory.resolve("command.log");Process process=new ProcessBuilder(arguments).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if(!process.waitFor(60,TimeUnit.SECONDS)){process.destroyForcibly();fail("Command timed out");}
        assertEquals(0,process.exitValue(),Files.readString(log));
    }
    private void execute(String wat,String expected) throws Exception {
        Path input=directory.resolve("metadata.wat"),output=directory.resolve("metadata.wasm"),script=directory.resolve("run.cjs");
        Files.writeString(input,wat);run("wat2wasm","--enable-exceptions","--debug-names",input.toString(),"-o",output.toString());
        Files.writeString(script,"""
            const fs=require('node:fs'),assert=require('node:assert/strict');
            const compiled=new WebAssembly.Module(fs.readFileSync(process.argv[2]));
            const expected=EXPECTED;
            for(let group=0;group<expected.length;group++)for(let instance=0;instance<2;instance++) {
                const memory=new WebAssembly.Memory({initial:2,maximum:20});let heap=1024,allocations=0;
                const e=new WebAssembly.Instance(compiled,{env:{memory,__heap_alloc:size=>{
                    allocations++;
                    const address=heap;heap+=size;
                    while(heap>memory.buffer.byteLength)memory.grow(1);
                    new Uint8Array(memory.buffer,address,size).fill(0x7f);return address;
                }}}).exports;
                assert.equal(allocations,0);heap=Math.max(heap,e.__exception_heap_base?.value??0);
            """.replace("EXPECTED",expected)+"if(e['Metadata.excludedState'])assert.equal(e['Metadata.excludedState'](),1);\nif(e['Metadata.exclusions'])assert.equal(e['Metadata.exclusions'](),1);\nassert.equal(e['"+fixtureClassName+".run'](group),expected[group],'batch '+group);\n}");
        run("node",script.toString(),output.toString());
    }
}
