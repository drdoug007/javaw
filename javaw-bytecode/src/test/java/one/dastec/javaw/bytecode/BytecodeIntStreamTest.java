package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.junit.jupiter.api.io.TempDir;
import javax.tools.ToolProvider;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.jar.*;

import static org.junit.jupiter.api.Assertions.*;

class BytecodeIntStreamTest {
    @TempDir Path directory;
    private final String fixtureClassName="Metadata";
    private String fixtureClasspath="";
    private final Map<String,byte[]> carriers=new HashMap<>();
    @Test void factoriesAndTraversal() throws Exception {
        fixture("""
            import java.util.*;import java.util.stream.*;import java.util.function.*;
            public class Metadata {public static int run(int group){int h=1;
             for(int[] data:new int[][]{new int[0],new int[]{7},new int[]{Integer.MIN_VALUE,0,17,Integer.MAX_VALUE}}){
              h=31*h+Arrays.hashCode(IntStream.of(data).toArray());int[] total={0};IntStream.of(data).forEach(v->total[0]=31*total[0]+v);h=31*h+total[0];
              PrimitiveIterator.OfInt it=IntStream.of(data).iterator();while(it.hasNext())h=31*h+it.nextInt();
              try{it.nextInt();throw new Error();}catch(NoSuchElementException e){h++;}
              try{it.remove();throw new Error();}catch(UnsupportedOperationException e){h++;}
              int[] boxed={0};((Iterator<Integer>)IntStream.of(data).iterator()).forEachRemaining(v->boxed[0]+=v);h=31*h+boxed[0];
             }
             h=31*h+Arrays.hashCode(IntStream.empty().toArray());h=31*h+Arrays.hashCode(IntStream.of(7).toArray());
             for(int[] bounds:new int[][]{{0,0},{3,1},{-3,3},{Integer.MAX_VALUE-2,Integer.MAX_VALUE},{Integer.MIN_VALUE,Integer.MIN_VALUE+2}}){
              h=31*h+Arrays.hashCode(IntStream.range(bounds[0],bounds[1]).toArray());
              h=31*h+Arrays.hashCode(IntStream.rangeClosed(bounds[0],bounds[1]).toArray());
             }
             int[] data={1,2};IntStream s=IntStream.of(data);data[0]=9;h=31*h+Arrays.hashCode(s.toArray());
             try{IntStream.of((int[])null);throw new Error();}catch(NullPointerException e){h++;}
             try{IntStream.empty().forEach(null);throw new Error();}catch(NullPointerException e){h++;}
             return h;}}
            """);compare(1);
    }

    @Test void numericTerminals() throws Exception {
        fixture("""
import java.util.*;import java.util.stream.*;import java.util.function.*;
public class Metadata {static int stat(IntSummaryStatistics s){int h=Long.hashCode(s.getCount());h=31*h+Long.hashCode(s.getSum());h=31*h+s.getMin();h=31*h+s.getMax();return 31*h+Double.hashCode(s.getAverage());}
 public static int run(int group){int h=1;
 for(int[] data:new int[][]{new int[0],{7},{-7,4,12},{Integer.MIN_VALUE,Integer.MAX_VALUE,Integer.MAX_VALUE}}){
  h=31*h+IntStream.of(data).sum();h=31*h+Long.hashCode(IntStream.of(data).count());
  h=31*h+IntStream.of(data).min().hashCode();h=31*h+IntStream.of(data).max().hashCode();h=31*h+IntStream.of(data).average().hashCode();
  h=31*h+IntStream.of(data).reduce(9,(a,v)->31*a+v);h=31*h+IntStream.of(data).reduce((a,v)->a-v).hashCode();
  h=31*h+stat(IntStream.of(data).summaryStatistics());
  StringBuilder result=IntStream.of(data).collect(StringBuilder::new,(s,v)->s.append(v).append(','),(a,c)->a.append(c));h=31*h+result.toString().hashCode();
 }
 IntSummaryStatistics a=new IntSummaryStatistics(),c=new IntSummaryStatistics();a.accept(Integer.MIN_VALUE);c.accept(Integer.MAX_VALUE);a.combine(c);h=31*h+stat(a);a.combine(new IntSummaryStatistics());h=31*h+stat(a);
 try{a.combine(null);throw new Error();}catch(NullPointerException e){h++;}
 IntStream s=IntStream.of(1,2);try{s.reduce(0,null);throw new Error();}catch(NullPointerException e){h++;}h=31*h+s.sum();
 IntStream failed=IntStream.of(1);RuntimeException failure=new IllegalStateException("supplier");try{failed.collect(()->{throw failure;},(Object o,int v)->{},(Object x,Object y)->{});throw new Error();}catch(RuntimeException e){h=31*h+(e==failure?1:0);}
 try{failed.count();throw new Error();}catch(IllegalStateException e){h++;}return h;
 }}
            """);compare(1);
    }

    @Test void lazyTransforms() throws Exception {
        fixture("""
import java.util.*;import java.util.stream.*;import java.util.function.*;
public class Metadata {public static int run(int group){int h=1;int[] calls={0};
 IntStream source=IntStream.of(-2,-1,0,1,2);IntStream pipeline=source.filter(v->{calls[0]++;return v>=0;}).map(v->{calls[0]+=10;return v*3;}).peek(v->calls[0]+=100);
 h=31*h+calls[0];try{source.sum();throw new Error();}catch(IllegalStateException e){h++;}
 h=31*h+Arrays.hashCode(pipeline.toArray());h=31*h+calls[0];
 h=31*h+IntStream.of(3,4).mapToObj(v->v==3?null:"v"+v).toList().hashCode();
 h=31*h+IntStream.of(-128,0,127,1000).boxed().toList().hashCode();
 IntStream original=IntStream.of(9);Stream<Integer> boxed=original.boxed();try{original.count();throw new Error();}catch(IllegalStateException e){h++;}h=31*h+boxed.toList().hashCode();
 for(int mode=0;mode<4;mode++){IntStream s=IntStream.of(5);try{if(mode==0)s.filter(null);if(mode==1)s.map(null);if(mode==2)s.peek(null);if(mode==3)s.mapToObj(null);throw new Error();}catch(NullPointerException e){h++;}h=31*h+s.sum();}
 RuntimeException failure=new IllegalStateException("map");try{IntStream.of(1).map(v->{throw failure;}).sum();throw new Error();}catch(RuntimeException e){h=31*h+(e==failure?1:0);}return h;
}}
            """);compare(1);
    }

    @Test void orderingAndShortCircuit() throws Exception {
        fixture("""
import java.util.*;import java.util.stream.*;
public class Metadata {public static int run(int group){int h=1;
 h=31*h+Arrays.hashCode(IntStream.of(3,-1,3,2,-1,Integer.MIN_VALUE,Integer.MAX_VALUE).distinct().sorted().skip(1).limit(3).toArray());
 h=31*h+Arrays.hashCode(IntStream.of(1,2,3,1).takeWhile(v->v<3).toArray());h=31*h+Arrays.hashCode(IntStream.of(1,2,3,1).dropWhile(v->v<3).toArray());
 h=31*h+Arrays.hashCode(IntStream.of(1,2).skip(Long.MAX_VALUE).limit(Long.MAX_VALUE).toArray());
 for(int mode=0;mode<3;mode++){int[] calls={0};IntStream s=IntStream.range(0,10).peek(v->calls[0]++);boolean result=mode==0?s.anyMatch(v->v==3):mode==1?s.allMatch(v->v<3):s.noneMatch(v->v==3);h=31*h+(result?1:0);h=31*h+calls[0];}
 int[] calls={0};h=31*h+IntStream.of(7,8).peek(v->calls[0]++).findFirst().hashCode();h=31*h+calls[0];
 h=31*h+IntStream.empty().findAny().hashCode();h=31*h+(IntStream.empty().allMatch(v->false)?1:0);h=31*h+(IntStream.empty().noneMatch(v->true)?1:0);
 for(int mode=0;mode<2;mode++){IntStream s=IntStream.of(5);try{if(mode==0)s.limit(-1);else s.skip(-1);throw new Error();}catch(IllegalArgumentException e){h++;}h=31*h+s.sum();}
 for(int mode=0;mode<5;mode++){IntStream s=IntStream.of(6);try{if(mode==0)s.takeWhile(null);if(mode==1)s.dropWhile(null);if(mode==2)s.anyMatch(null);if(mode==3)s.allMatch(null);if(mode==4)s.noneMatch(null);throw new Error();}catch(NullPointerException e){h++;}h=31*h+s.sum();}
 return h;
}}
            """);compare(1);
    }

    @Test void lifecycleAndFlattening() throws Exception {
        fixture("""
import java.util.*;import java.util.stream.*;
public class Metadata {public static int unsupportedParallel(){try{IntStream.of(1).parallel();return 0;}catch(UnsupportedOperationException e){return 1;}}
 public static int run(int group){int h=1;int[] generated={0};h=31*h+Arrays.hashCode(IntStream.generate(()->++generated[0]).limit(4).toArray());h=31*h+generated[0];
 h=31*h+Arrays.hashCode(IntStream.iterate(1,v->v*2).limit(5).toArray());h=31*h+Arrays.hashCode(IntStream.iterate(1,v->v<8,v->v+2).toArray());
 int[] closed={0};IntStream pipeline=IntStream.of(1,2,3).flatMap(v->v==2?null:IntStream.of(v,v+10).onClose(()->closed[0]++));h=31*h+Arrays.hashCode(pipeline.limit(1).toArray());h=31*h+closed[0];pipeline.close();h=31*h+closed[0];
 IntStream exhausted=IntStream.of(1,2).flatMap(v->IntStream.of(v).onClose(()->closed[0]++));h=31*h+exhausted.sum();h=31*h+closed[0];
 IntStream iterated=IntStream.of(1).flatMap(v->IntStream.of(8,9).onClose(()->closed[0]++));PrimitiveIterator.OfInt it=iterated.iterator();h=31*h+it.nextInt();iterated.close();h=31*h+closed[0];
 StringBuilder events=new StringBuilder();IntStream joined=IntStream.concat(IntStream.of(3).onClose(()->events.append('a')),IntStream.of(4).onClose(()->events.append('b'))).onClose(()->events.append('c'));h=31*h+joined.sum();joined.close();joined.close();h=31*h+events.toString().hashCode();
 RuntimeException first=new IllegalStateException("first"),second=new IllegalArgumentException("second");IntStream throwing=IntStream.empty().onClose(()->{throw first;}).onClose(()->{throw second;});try{throwing.close();throw new Error();}catch(RuntimeException e){h=31*h+(e==first?1:0);h=31*h+e.getSuppressed().length;h=31*h+(e.getSuppressed()[0]==second?1:0);}
 RuntimeException primary=new IllegalStateException("primary"),cleanup=new IllegalArgumentException("cleanup");try{IntStream.of(1).flatMap(v->IntStream.of(v).onClose(()->{throw cleanup;})).forEach(v->{throw primary;});throw new Error();}catch(RuntimeException e){h=31*h+(e==primary?1:0);h=31*h+e.getSuppressed().length;h=31*h+(e.getSuppressed()[0]==cleanup?1:0);}
 IntStream reusable=IntStream.of(5);try{IntStream.concat(reusable,null);throw new Error();}catch(NullPointerException e){h++;}h=31*h+reusable.sum();
 IntStream used=IntStream.of(7);h=31*h+used.sum();try{used.count();throw new Error();}catch(IllegalStateException e){h++;}
 IntStream ended=IntStream.of(7);ended.close();try{ended.sum();throw new Error();}catch(IllegalStateException e){h++;}
 for(int mode=0;mode<3;mode++){try{if(mode==0)IntStream.generate(null);if(mode==1)IntStream.iterate(0,(java.util.function.IntUnaryOperator)null);if(mode==2)IntStream.of(1).flatMap(null);throw new Error();}catch(NullPointerException e){h++;}}
 return h;
 }}
            """);compare(1);
    }

    @Test void stringCharacters() throws Exception {
        fixture("""
import java.util.*;import java.util.stream.*;import java.util.function.*;
public class Metadata {public static int run(int group){int h=1;
 for(String text:new String[]{"","hello","a𐐀z",new String(new char[]{0,(char)0xd800,65,(char)0xdc00,(char)0xffff})}){
  h=31*h+Arrays.hashCode(text.chars().toArray());h=31*h+text.chars().sum();h=31*h+Long.hashCode(text.chars().count());h=31*h+text.chars().mapToObj(v->Integer.toHexString(v)).toList().hashCode();
  Function<String,IntStream> factory=String::chars;h=31*h+Arrays.hashCode(factory.apply(text).toArray());
 }
 char[] units=new char[2048];for(int i=0;i<units.length;i++)units[i]=(char)(group*2048+i);h=31*h+Arrays.hashCode(new String(units).chars().toArray());
 IntStream one="abc".chars();h=31*h+one.sum();try{one.count();throw new Error();}catch(IllegalStateException e){h++;}
 int[] calls={0};h=31*h+("abca".chars().peek(v->calls[0]++).anyMatch(v->v=='b')?1:0);h=31*h+calls[0];return h;
}}
            """);compare(32);
    }

    @Test void stringCodePoints() throws Exception {
        fixture("""
import java.util.*;import java.util.stream.*;import java.util.function.*;
public class Metadata {public static int run(int group){int h=1;Function<String,IntStream> factory=String::codePoints;
 for(String text:new String[]{"","hello","𐐀a😀",new String(new char[]{(char)0xd800,(char)0xd801,(char)0xdc00,(char)0xdc01,(char)0xdbff,(char)0xdfff,0})}){
  h=31*h+Arrays.hashCode(text.codePoints().toArray());h=31*h+Arrays.hashCode(factory.apply(text).toArray());h=31*h+text.codePoints().sum();h=31*h+Long.hashCode(text.codePoints().count());
 }
 char[] units=new char[2048];for(int i=0;i<units.length;i++)units[i]=(char)(group*2048+i);h=31*h+Arrays.hashCode(new String(units).codePoints().toArray());
 int[] points=new int[256];for(int i=0;i<points.length;i++)points[i]=0x10000+((group*256+i)*127%0x100000);String text=new String(points,0,points.length);h=31*h+Arrays.hashCode(text.codePoints().toArray());
 int[] calls={0};h=31*h+"𐐀😀a".codePoints().peek(v->calls[0]++).findFirst().orElse(-1);h=31*h+calls[0];return h;
}}
            """);compare(32);h2Trim();
    }

    private void h2Trim() throws Exception {
        carrier(selectedLibrary("org/h2/util/StringUtils.class","com/h2database/h2/2.2.224/h2-2.2.224.jar",
            "b9d8f19358ada82a4f6eb5b174c6cfe320a375b5a9cb5a4fe456d623e6e55497",
            name->name.equals("trim")||name.startsWith("lambda$trim$")));
        fixture("""
            import org.h2.util.StringUtils;
            public class Metadata {public static int run(int group){int h=1;
             String lone=new String(new char[]{(char)0xd800});
             for(String trim:new String[]{null,"","x","xy","xyz","😀𐐀x",lone+"xy"})
              for(String text:new String[]{"","xyzhellozyx","😀𐐀hello𐐀😀",lone+"hello"+lone,"  hi  ","unchanged"})
               for(int flags=0;flags<4;flags++){
                String result=StringUtils.trim(text,(flags&1)!=0,(flags&2)!=0,trim);
                h=31*h+result.hashCode();h=31*h+(result==text?1:0);
               }
             for(int flags=0;flags<4;flags++){String text="xxhixx";String result=StringUtils.trim(text,(flags&1)!=0,(flags&2)!=0,'x');h=31*h+result.hashCode();}
             try{StringUtils.trim(null,true,true,"xyz");throw new Error();}catch(NullPointerException e){h++;}
             return h;
            }}
            """);compare(1);
    }

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
        Files.writeString(input,wat);run("wat2wasm","--enable-exceptions",input.toString(),"-o",output.toString());
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
            """.replace("EXPECTED",expected)+"if(e['Metadata.unsupportedParallel'])assert.equal(e['Metadata.unsupportedParallel'](),1);\nassert.equal(e['"+fixtureClassName+".run'](group),expected[group],'batch '+group);\n}");
        run("node",script.toString(),output.toString());
    }
}
