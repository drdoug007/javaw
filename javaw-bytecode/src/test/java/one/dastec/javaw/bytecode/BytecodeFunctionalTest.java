package one.dastec.javaw.bytecode;

import one.dastec.javaw.Wat2Wasm;
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

class BytecodeFunctionalTest implements Opcodes {
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

    @Test void executesCompositionsComparatorsAndOptionals() throws Exception {
        fixture("""
            import java.util.*;import java.util.function.*;
            public class Metadata {
                static int events;
                static class NullText{public String toString(){return null;}}
                public static String nullText(){Object value=new NullText();return Optional.of(value).toString()+"/"+new AbstractMap.SimpleEntry<>(value,value)+"/"+new AbstractMap.SimpleImmutableEntry<>(value,value);}

                public static String run(){
                    Function<String,Integer> length=String::length;Function<Integer,Integer> twice=n->n*2;
                    int value=length.andThen(twice).compose((String s)->s.concat("!")).apply("ab");
                    Predicate<Integer> positive=n->{events++;return n>0;};Predicate<Integer> even=n->{events+=10;return n%2==0;};
                    positive.and(even).test(-1);positive.or(even).test(1);Predicate.not(positive).test(-2);
                    Consumer<Integer> a=n->events+=n;a.andThen(n->events+=n*3).accept(2);
                    Comparator<String> order=Comparator.nullsFirst(Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder()));
                    List<String> strings=new ArrayList<>(Arrays.asList("b","aa",null,"a"));strings.sort(order);strings.removeIf(s->s!=null && s.length()>1);strings.replaceAll(s->s==null?"NULL":s.concat("!"));
                    Optional<String> present=Optional.of("abc");String optional=present.map(length).flatMap(n->Optional.of(n+1)).filter(n->n>0).map(Object::toString).orElse("missing");
                    Optional.empty().ifPresentOrElse(v->{throw new Error();},()->events+=100);present.orElseGet(()->{throw new Error();});
                    OptionalDouble nan=OptionalDouble.of(Double.NaN);OptionalDouble zero=OptionalDouble.of(-0.0);
                    return value+"/"+events+"/"+strings+"/"+optional+"/"+Optional.empty()+"/"+nan.equals(OptionalDouble.of(Double.NaN))+"/"+zero.equals(OptionalDouble.of(0.0))+"/"+OptionalInt.of(7).orElse(2);
                }
                public static int primitives(){
                    int score=0;
                    IntUnaryOperator ints=n->n+1;
                    LongUnaryOperator longs=n->n*3;
                    DoubleUnaryOperator doubles=n->n/2;
                    if(ints.compose(n->n*2).andThen(n->n+3).applyAsInt(4)==12)score++;
                    if(longs.compose(n->n+1).andThen(n->n-2).applyAsLong(4)==13)score+=2;
                    if(doubles.compose(n->n+2).andThen(n->n*3).applyAsDouble(4)==9)score+=4;
                    IntPredicate positive=n->{events++;return n>0;};
                    IntPredicate even=n->{events+=10;return n%2==0;};
                    events=0;if(!positive.and(even).test(-1) && events==1)score+=8;
                    events=0;if(positive.or(even).test(1) && events==1)score+=16;
                    IntConsumer consume=n->events+=n;events=0;
                    consume.andThen(n->events*=n).accept(3);if(events==9)score+=32;
                    RuntimeException failure=new RuntimeException();
                    try{((LongConsumer)(n->{throw failure;})).andThen(n->{events=100;}).accept(1);}
                    catch(RuntimeException caught){if(caught==failure && events==9)score+=64;}
                    try{doubles.andThen(null);}catch(NullPointerException caught){score+=128;}
                    Exception checked=new Exception();
                    try{OptionalLong.empty().orElseThrow(()->checked);}catch(Exception caught){if(caught==checked)score+=256;}
                    if(OptionalInt.of(1).orElseGet(null)==1 && OptionalDouble.of(2).orElseGet(null)==2)score+=512;
                    if(BinaryOperator.<String>minBy(Comparator.comparingInt(String::length)).apply("a","b").equals("a"))score+=1024;
                    Predicate<String> custom=new Predicate<String>(){public boolean test(String value){return false;}public Predicate<String> negate(){return value->false;}};
                    if(!Predicate.not(custom).test("x"))score+=2048;
                    return score;
                }
                public static int failures(){int score=0;
                    try{Optional.empty().map(null);}catch(NullPointerException e){score++;}
                    try{Optional.of("x").flatMap(s->null);}catch(NullPointerException e){score+=10;}
                    try{Optional.empty().get();}catch(NoSuchElementException e){score+=100;}
                    RuntimeException failure=new RuntimeException("same");try{Optional.empty().orElseThrow(()->failure);}catch(RuntimeException e){if(e==failure)score+=1000;}
                    try{((Function<Integer,Integer>)(n->n)).andThen(null);}catch(NullPointerException e){score+=10000;}
                    try{Comparator.comparingInt(null);}catch(NullPointerException e){score+=100000;}return score;
                }
            }
            """);compare(List.of("run","failures","primitives","nullText"));
    }
    @Test void executesUnchangedPinnedSpringThrowingFunction() throws Exception {
        Path repository=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()));Path artifact=repository.resolve("org/springframework/spring-core/6.1.5/spring-core-6.1.5.jar");
        assertEquals("476dac28509b3f47d0b5e895a639fa75ac2218f074ce214e44832a836c469f7e",java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(artifact))));fixtureClasspath=artifact.toString();
        fixture("""
            import org.springframework.util.function.ThrowingFunction;
            public class Metadata {
                public static int run(){
                    ThrowingFunction<String,Integer> good=ThrowingFunction.of(String::length);int score=good.andThen(n->n*2).apply("abc");
                    Exception checked=new Exception("checked");ThrowingFunction<String,Integer> bad=ThrowingFunction.of(s->{throw checked;});
                    try{bad.apply("x");}catch(RuntimeException error){if(error.getCause()==checked)score+=10;}
                    RuntimeException replacement=new IllegalArgumentException("replacement");try{bad.throwing((message,error)->replacement).apply("x");}catch(RuntimeException error){if(error==replacement)score+=100;}
                    RuntimeException direct=new RuntimeException();ThrowingFunction<String,Integer> runtime=ThrowingFunction.of(s->{throw direct;});try{runtime.apply("x");}catch(RuntimeException error){if(error==direct)score+=1000;}return score;
                }
            }
            """);
        try(JarFile jar=new JarFile(artifact.toFile())){for(String name:List.of("org/springframework/util/function/ThrowingFunction.class","org/springframework/util/function/ThrowingFunction$1.class")){Path output=directory.resolve(name);Files.createDirectories(output.getParent());Files.write(output,jar.getInputStream(jar.getJarEntry(name)).readAllBytes());}}
        compare(List.of("run"));
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
        Files.writeString(input,wat);run(Wat2Wasm.command(input,output,Wat2Wasm.EXCEPTIONS).toArray(String[]::new));
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
