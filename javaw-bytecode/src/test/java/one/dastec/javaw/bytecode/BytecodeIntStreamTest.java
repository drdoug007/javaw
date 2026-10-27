package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;
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

    private byte[] fixture(String source) throws Exception {
        Path file=directory.resolve("Metadata.java");Files.writeString(file,source);
        List<String> options=new ArrayList<>(List.of("-g","-parameters","--release","17","-d",directory.toString()));
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
                jar.putNextEntry(new JarEntry(directory.relativize(file).toString()));jar.write(Files.readAllBytes(file));jar.closeEntry();}
        }
        return bytes.toByteArray();
    }

    private void compare(int groups) throws Exception {
        Path java=Path.of(System.getProperty("javaw.jdk17.home",System.getProperty("user.home")+"/Library/Java/JavaVirtualMachines/graalvm-ce-17.0.9/Contents/Home"),"bin/java");
        assertTrue(Files.isExecutable(java),"A JDK 17 oracle is required (javaw.jdk17.home)");
        run(java.toString(),"-Djava.locale.providers=CLDR","-Duser.language=","-Duser.country=","-Duser.variant=","-cp",directory.toString(),"CaseOracle",fixtureClassName,Integer.toString(groups));
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
            """.replace("EXPECTED",expected)+"assert.equal(e['"+fixtureClassName+".run'](group),expected[group],'batch '+group);\n}");
        run("node",script.toString(),output.toString());
    }
}
