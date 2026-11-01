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

class BytecodeArraysTest implements Opcodes {
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

    @Test void matchesPrimitiveReferenceNestedAndTypedArrayAlgorithms() throws Exception {
        fixture("""

            import java.util.*;
            public class Metadata {
                public static String describe(Throwable error){return error+"/"+java.util.Arrays.toString(error.getStackTrace());}
                public static String objects(){
                    String[] original={"c",null,"a"};Object[] copy=Arrays.copyOf(original,5);copy[0]="z";
                    Number[] numbers=Arrays.copyOf(new Integer[]{1,2},4,Number[].class);
                    Object[] padded=Arrays.copyOfRange(original,1,5);Object[] widened=Arrays.copyOfRange(original,0,2,Object[].class);
                    String[] sorted={"keep","z","b","a","tail"};Arrays.sort(sorted,1,4);
                    int location=Arrays.binarySearch(sorted,1,4,"b");Arrays.sort(sorted,Comparator.reverseOrder());
                    Object[] shared={new int[]{1,2},new boolean[]{true,false},new double[]{-0.0,Double.NaN},null};
                    Object[] nested={shared,shared,new Object[]{new char[]{'x','y'},new long[]{Long.MIN_VALUE}}};Object[] cycle=new Object[2];cycle[0]=cycle;cycle[1]=new Object[]{cycle};
                    return Arrays.toString(original)+"/"+Arrays.toString(copy)+"/"+copy.getClass().getName()+"/"+Arrays.toString(numbers)+"/"+Arrays.toString(padded)+"/"+widened.getClass().getName()+"/"+Arrays.toString(sorted)+"/"+location+"/"+Arrays.deepToString(nested)+"/"+Arrays.deepToString(cycle);
                }
                public static int hashes(){
                    Object[] a={new int[]{1,2},new Object[]{"x",new byte[]{3}},new float[]{Float.NaN,-0.0f},null};
                    Object[] b={new int[]{1,2},new Object[]{"x",new byte[]{3}},new float[]{Float.intBitsToFloat(0x7fa12345),-0.0f},null};
                    int score=Arrays.deepEquals(a,b) && Objects.deepEquals(a,b)?1:0;
                    if(!Arrays.equals(a,b))score+=2;if(!Objects.deepEquals(new int[]{1},new long[]{1}))score+=4;
                    if(Objects.isNull(null) && Objects.nonNull(a) && Objects.requireNonNullElse("x",null).equals("x"))score+=8;
                    if(Objects.toString(null,"fallback").equals("fallback"))score+=16;
                    if(!Arrays.equals(new double[]{-0.0},new double[]{0.0}))score+=32;
                    if(Arrays.equals(new float[]{Float.NaN},new float[]{Float.intBitsToFloat(0x7fa12345)}))score+=64;
                    if(Math.max(Long.MIN_VALUE,Long.MAX_VALUE)==Long.MAX_VALUE && Math.min(Long.MIN_VALUE,Long.MAX_VALUE)==Long.MIN_VALUE && Math.min(Integer.MAX_VALUE,Integer.MIN_VALUE)==Integer.MIN_VALUE)score+=128;
                    return score+31*Arrays.deepHashCode(a)+Objects.hash("x",null,17)+Arrays.hashCode((int[])null);
                }
                public static int searches(){
                    float[] f={Float.NaN,0.0f,-0.0f,Float.NEGATIVE_INFINITY,1.0f};Arrays.sort(f);
                    double[] d={Double.NaN,0.0,-0.0,Double.POSITIVE_INFINITY,-2};Arrays.sort(d,0,d.length);
                    return Arrays.binarySearch(f,-0.0f)+10*Arrays.binarySearch(f,0.0f)+100*Arrays.binarySearch(f,Float.NaN)+1000*Arrays.binarySearch(d,Double.NaN)+10000*Arrays.binarySearch(new int[]{1,3,5},4)+100000*Arrays.binarySearch(new String[]{"c","b","a"},"b",Comparator.reverseOrder());
                }
                public static int failures(){
                    int score=0;
                    try{Arrays.copyOf((int[])null,-1);}catch(NullPointerException e){score++;}
                    try{Arrays.copyOf((String[])null,-1);}catch(NullPointerException e){score+=2;}
                    try{Arrays.copyOf((String[])null,-1,String[].class);}catch(NegativeArraySizeException e){score+=4;}
                    try{Arrays.copyOfRange(new int[]{1},2,2);}catch(ArrayIndexOutOfBoundsException e){score+=8;}
                    try{Arrays.copyOfRange((int[])null,2,1);}catch(IllegalArgumentException e){score+=16;}
                    try{Arrays.copyOf(new Object[]{"x",1},2,String[].class);}catch(ArrayStoreException e){score+=32;}
                    String[] values={"x"};try{Arrays.fill((Object[])values,0,2,1);}catch(ArrayIndexOutOfBoundsException e){if(values[0].equals("x"))score+=64;}
                    try{Arrays.fill((Object[])values,0,1,1);}catch(ArrayStoreException e){score+=128;}
                    try{Arrays.sort(new int[0],1,0);}catch(IllegalArgumentException e){score+=256;}
                    try{Arrays.binarySearch((int[])null,2,1,0);}catch(NullPointerException e){score+=512;}
                    try{Objects.requireNonNullElse(null,null);}catch(NullPointerException e){if(e.getMessage().equals("defaultObj"))score+=1024;}
                    return score;
                }
                public static String booleanArrays(){
                    boolean[] source={true,false,true};boolean[] copy=Arrays.copyOf(source,5);boolean[] range=Arrays.copyOfRange(source,1,5);
                    Arrays.fill(copy,1,3,false);Arrays.fill(range,false);
                    String result=Arrays.toString(source)+"/"+Arrays.toString(copy)+"/"+Arrays.toString(range)+"/"+Arrays.hashCode(source)+"/"+Arrays.equals(source,Arrays.copyOf(source,source.length));
                    return result;
                }
                public static String byteArrays(){
                    byte[] source={3,-1,2};byte[] copy=Arrays.copyOf(source,5);byte[] range=Arrays.copyOfRange(source,1,5);
                    Arrays.fill(copy,1,3,(byte)7);Arrays.fill(range,(byte)7);
                    String result=Arrays.toString(source)+"/"+Arrays.toString(copy)+"/"+Arrays.toString(range)+"/"+Arrays.hashCode(source)+"/"+Arrays.equals(source,Arrays.copyOf(source,source.length));
                    Arrays.sort(source);result+="/"+Arrays.toString(source)+"/"+Arrays.binarySearch(source,source[0])+"/"+Arrays.binarySearch(source,1,source.length,source[source.length-1]);
                    return result;
                }
                public static String shortArrays(){
                    short[] source={3,-1,2};short[] copy=Arrays.copyOf(source,5);short[] range=Arrays.copyOfRange(source,1,5);
                    Arrays.fill(copy,1,3,(short)7);Arrays.fill(range,(short)7);
                    String result=Arrays.toString(source)+"/"+Arrays.toString(copy)+"/"+Arrays.toString(range)+"/"+Arrays.hashCode(source)+"/"+Arrays.equals(source,Arrays.copyOf(source,source.length));
                    Arrays.sort(source);result+="/"+Arrays.toString(source)+"/"+Arrays.binarySearch(source,source[0])+"/"+Arrays.binarySearch(source,1,source.length,source[source.length-1]);
                    return result;
                }
                public static String charArrays(){
                    char[] source={'z','a','b'};char[] copy=Arrays.copyOf(source,5);char[] range=Arrays.copyOfRange(source,1,5);
                    Arrays.fill(copy,1,3,'Q');Arrays.fill(range,'Q');
                    String result=Arrays.toString(source)+"/"+Arrays.toString(copy)+"/"+Arrays.toString(range)+"/"+Arrays.hashCode(source)+"/"+Arrays.equals(source,Arrays.copyOf(source,source.length));
                    Arrays.sort(source);result+="/"+Arrays.toString(source)+"/"+Arrays.binarySearch(source,source[0])+"/"+Arrays.binarySearch(source,1,source.length,source[source.length-1]);
                    return result;
                }
                public static String intArrays(){
                    int[] source={Integer.MAX_VALUE,Integer.MIN_VALUE,2};int[] copy=Arrays.copyOf(source,5);int[] range=Arrays.copyOfRange(source,1,5);
                    Arrays.fill(copy,1,3,7);Arrays.fill(range,7);
                    String result=Arrays.toString(source)+"/"+Arrays.toString(copy)+"/"+Arrays.toString(range)+"/"+Arrays.hashCode(source)+"/"+Arrays.equals(source,Arrays.copyOf(source,source.length));
                    Arrays.sort(source);result+="/"+Arrays.toString(source)+"/"+Arrays.binarySearch(source,source[0])+"/"+Arrays.binarySearch(source,1,source.length,source[source.length-1]);
                    return result;
                }
                public static String longArrays(){
                    long[] source={Long.MAX_VALUE,Long.MIN_VALUE,2};long[] copy=Arrays.copyOf(source,5);long[] range=Arrays.copyOfRange(source,1,5);
                    Arrays.fill(copy,1,3,7L);Arrays.fill(range,7L);
                    String result=Arrays.toString(source)+"/"+Arrays.toString(copy)+"/"+Arrays.toString(range)+"/"+Arrays.hashCode(source)+"/"+Arrays.equals(source,Arrays.copyOf(source,source.length));
                    Arrays.sort(source);result+="/"+Arrays.toString(source)+"/"+Arrays.binarySearch(source,source[0])+"/"+Arrays.binarySearch(source,1,source.length,source[source.length-1]);
                    return result;
                }
                public static String floatArrays(){
                    float[] source={Float.NaN,-0.0f,0.0f};float[] copy=Arrays.copyOf(source,5);float[] range=Arrays.copyOfRange(source,1,5);
                    Arrays.fill(copy,1,3,7f);Arrays.fill(range,7f);
                    String result=Arrays.toString(source)+"/"+Arrays.toString(copy)+"/"+Arrays.toString(range)+"/"+Arrays.hashCode(source)+"/"+Arrays.equals(source,Arrays.copyOf(source,source.length));
                    Arrays.sort(source);result+="/"+Arrays.toString(source)+"/"+Arrays.binarySearch(source,source[0])+"/"+Arrays.binarySearch(source,1,source.length,source[source.length-1]);
                    return result;
                }
                public static String doubleArrays(){
                    double[] source={Double.NaN,-0.0,0.0};double[] copy=Arrays.copyOf(source,5);double[] range=Arrays.copyOfRange(source,1,5);
                    Arrays.fill(copy,1,3,7d);Arrays.fill(range,7d);
                    String result=Arrays.toString(source)+"/"+Arrays.toString(copy)+"/"+Arrays.toString(range)+"/"+Arrays.hashCode(source)+"/"+Arrays.equals(source,Arrays.copyOf(source,source.length));
                    Arrays.sort(source);result+="/"+Arrays.toString(source)+"/"+Arrays.binarySearch(source,source[0])+"/"+Arrays.binarySearch(source,1,source.length,source[source.length-1]);
                    return result;
                }
            }
            """);compare(List.of("objects","hashes","searches","failures","booleanArrays","byteArrays","shortArrays","charArrays","intArrays","longArrays","floatArrays","doubleArrays"));
    }
    @Test void registersTypedCopyAllocationThroughMethodReferences() throws Exception {
        fixture("""
            import java.util.Arrays;import java.util.function.BiFunction;
            public class Metadata {
                public static String describe(Throwable error){return error+"/"+java.util.Arrays.toString(error.getStackTrace());}
                static int initialized;
                private static class Passive{static{initialized++;}}
                public static String types(){BiFunction<Passive[],Integer,Passive[]> copy=Arrays::copyOf;Passive[] values=copy.apply(new Passive[1],2);BiFunction<Object[][],Integer,Object[][]> nested=Arrays::copyOf;Object[][] original={new Object[]{"x"}};Object[][] result=nested.apply(original,3);return values.getClass().getName()+"/"+initialized+"/"+result.getClass().getName()+"/"+(result[0]==original[0])+"/"+result[2];}
                public static String run(){BiFunction<String[],Integer,String[]> copy=Arrays::copyOf;String[] result=copy.apply(new String[]{"a"},3);return result.getClass().getName()+"/"+result[0]+"/"+result.length+"/"+result[2];}
            }
            """);compare(List.of("run","types"));
    }

    @Test void executesUnchangedPinnedH2IntArray() throws Exception {
        Path repository=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()));
        Path artifact=repository.resolve("com/h2database/h2/2.2.224/h2-2.2.224.jar");
        assertEquals("b9d8f19358ada82a4f6eb5b174c6cfe320a375b5a9cb5a4fe456d623e6e55497",java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(artifact))));fixtureClasspath=artifact.toString();
        fixture("""
            import org.h2.util.IntArray;import java.util.Arrays;
            public class Metadata {
                public static String describe(Throwable error){return error+"/"+java.util.Arrays.toString(error.getStackTrace());}
                public static String run(){IntArray values=new IntArray(0);for(int i=0;i<12;i++)values.add(i*2);values.remove(2);values.removeRange(3,5);values.ensureCapacity(50);int[] output=new int[values.size()];values.toArray(output);IntArray same=new IntArray(output.clone());return values+"/"+Arrays.toString(output)+"/"+values.equals(same)+"/"+values.hashCode()+"/"+values.get(2)+"/"+new IntArray().size();}
                public static String failures(){IntArray values=new IntArray(new int[]{1,2});String messages="";try{values.get(2);}catch(ArrayIndexOutOfBoundsException e){messages+=e.getMessage();}try{values.removeRange(2,1);}catch(ArrayIndexOutOfBoundsException e){messages+="/"+e.getMessage();}return messages+"/"+values;}
            }
            """);
        try(JarFile jar=new JarFile(artifact.toFile())){String name="org/h2/util/IntArray.class";Path output=directory.resolve(name);Files.createDirectories(output.getParent());Files.write(output,jar.getInputStream(jar.getJarEntry(name)).readAllBytes());}
        compare(List.of("run","failures"));
    }

    private void compare(List<String> names) throws Exception {
        StringBuilder assertions=new StringBuilder();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)){
            Class<?> type=loader.loadClass("Metadata");for(String name:names){assertions.append("try{");Object expected=type.getMethod(name).invoke(null);assertions.append("assert.equal(").append(expected instanceof String?"text(e['Metadata.":"e['Metadata.").append(name).append(expected instanceof String?"']()),":"'](),").append(expected instanceof String?js((String)expected):expected).append(");}catch(error){if(error instanceof WebAssembly.Exception)throw new Error(").append(js(name)).append("+\": \"+text(e['Metadata.describe'](error.getArg(e.__java_exception,0))));throw error;}\n");}
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
