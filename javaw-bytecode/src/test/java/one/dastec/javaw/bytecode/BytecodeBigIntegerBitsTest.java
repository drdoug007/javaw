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

class BytecodeBigIntegerBitsTest implements Opcodes {
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

    @Test void and() throws Exception {
        fixture("""
            import java.math.BigInteger;
            public class Metadata {public static int run(){int hash=1;
                String[] values={"0","1","-1","127","128","255","256","-128","-129","-256","9223372036854775808","-9223372036854775809","123456789012345678901234567890","-123456789012345678901234567890"};
                java.util.function.BinaryOperator<BigInteger> op=BigInteger::and;
                for(String x:values)for(String y:values){BigInteger a=new BigInteger(x),b=new BigInteger(y);
                    BigInteger result=a.and(b);if(!result.equals(op.apply(a,b)))throw new Error();
                    hash=31*hash+result.toString().hashCode();hash=31*hash+a.toString().hashCode();}
                try{BigInteger.ONE.and(null);throw new Error();}catch(NullPointerException expected){hash++;}
                return hash;}}
            """);compare(List.of("run"));
    }

    @Test void or() throws Exception {
        fixture("""
            import java.math.BigInteger;
            public class Metadata {public static int run(){int hash=1;
                String[] values={"0","1","-1","127","128","255","256","-128","-129","-256","9223372036854775808","-9223372036854775809","123456789012345678901234567890","-123456789012345678901234567890"};
                java.util.function.BinaryOperator<BigInteger> op=BigInteger::or;
                for(String x:values)for(String y:values){BigInteger a=new BigInteger(x),b=new BigInteger(y);
                    BigInteger result=a.or(b);if(!result.equals(op.apply(a,b)))throw new Error();
                    hash=31*hash+result.toString().hashCode();hash=31*hash+a.toString().hashCode();}
                try{BigInteger.ONE.or(null);throw new Error();}catch(NullPointerException expected){hash++;}
                return hash;}}
            """);compare(List.of("run"));
    }

    @Test void xor() throws Exception {
        fixture("""
            import java.math.BigInteger;
            public class Metadata {public static int run(){int hash=1;
                String[] values={"0","1","-1","127","128","255","256","-128","-129","-256","9223372036854775808","-9223372036854775809","123456789012345678901234567890","-123456789012345678901234567890"};
                java.util.function.BinaryOperator<BigInteger> op=BigInteger::xor;
                for(String x:values)for(String y:values){BigInteger a=new BigInteger(x),b=new BigInteger(y);
                    BigInteger result=a.xor(b);if(!result.equals(op.apply(a,b)))throw new Error();
                    hash=31*hash+result.toString().hashCode();hash=31*hash+a.toString().hashCode();}
                try{BigInteger.ONE.xor(null);throw new Error();}catch(NullPointerException expected){hash++;}
                return hash;}}
            """);compare(List.of("run"));
    }

    @Test void andNot() throws Exception {
        fixture("""
            import java.math.BigInteger;
            public class Metadata {public static int run(){int hash=1;
                String[] values={"0","1","-1","127","128","255","256","-128","-129","-256","9223372036854775808","-9223372036854775809","123456789012345678901234567890","-123456789012345678901234567890"};
                java.util.function.BinaryOperator<BigInteger> op=BigInteger::andNot;
                for(String x:values)for(String y:values){BigInteger a=new BigInteger(x),b=new BigInteger(y);
                    BigInteger result=a.andNot(b);if(!result.equals(op.apply(a,b)))throw new Error();
                    hash=31*hash+result.toString().hashCode();hash=31*hash+a.toString().hashCode();}
                try{BigInteger.ONE.andNot(null);throw new Error();}catch(NullPointerException expected){hash++;}
                return hash;}}
            """);compare(List.of("run"));
    }

    @Test void not() throws Exception {
        fixture("""
            import java.math.BigInteger;
            public class Metadata {public static int run(){int hash=1;
                String[] values={"0","1","-1","127","128","255","256","-128","-129","-256","9223372036854775808","-9223372036854775809","123456789012345678901234567890","-123456789012345678901234567890"};
                java.util.function.UnaryOperator<BigInteger> op=BigInteger::not;
                for(String x:values){BigInteger a=new BigInteger(x);BigInteger result=a.not();
                    if(!result.equals(op.apply(a)) || !result.not().equals(a))throw new Error();hash=31*hash+result.toString().hashCode();}
                return hash;}}
            """);compare(List.of("run"));
    }

    @Test void setBit() throws Exception {
        fixture("""
            import java.math.BigInteger;
            public class Metadata {public static int run0(){return sample("0");}public static int run1(){return sample("1");}public static int run2(){return sample("-1");}public static int run3(){return sample("127");}public static int run4(){return sample("128");}public static int run5(){return sample("255");}public static int run6(){return sample("256");}public static int run7(){return sample("-128");}public static int run8(){return sample("-129");}public static int run9(){return sample("-256");}public static int run10(){return sample("9223372036854775808");}public static int run11(){return sample("-9223372036854775809");}public static int run12(){return sample("123456789012345678901234567890");}public static int run13(){return sample("-123456789012345678901234567890");}private static int sample(String input){int hash=1;
                String[] values={input};
                java.util.function.BiFunction<BigInteger,Integer,BigInteger> op=BigInteger::setBit;
                for(String x:values)for(int bit:new int[]{0,1,7,8,15,31,32,63,64,95,127,255,513}){BigInteger a=new BigInteger(x);
                    BigInteger result=a.setBit(bit);if(!result.equals(op.apply(a,bit)))throw new Error();
                    hash=31*hash+result.toString().hashCode();hash=31*hash+a.toString().hashCode();}
                for(int bit:new int[]{-1,Integer.MIN_VALUE})try{BigInteger.ONE.setBit(bit);throw new Error();}catch(ArithmeticException expected){hash++;}
                return hash;}}
            """);compare(List.of("run0","run1","run2","run3","run4","run5","run6","run7","run8","run9","run10","run11","run12","run13"));
    }

    @Test void clearBit() throws Exception {
        fixture("""
            import java.math.BigInteger;
            public class Metadata {public static int run0(){return sample("0");}public static int run1(){return sample("1");}public static int run2(){return sample("-1");}public static int run3(){return sample("127");}public static int run4(){return sample("128");}public static int run5(){return sample("255");}public static int run6(){return sample("256");}public static int run7(){return sample("-128");}public static int run8(){return sample("-129");}public static int run9(){return sample("-256");}public static int run10(){return sample("9223372036854775808");}public static int run11(){return sample("-9223372036854775809");}public static int run12(){return sample("123456789012345678901234567890");}public static int run13(){return sample("-123456789012345678901234567890");}private static int sample(String input){int hash=1;
                String[] values={input};
                java.util.function.BiFunction<BigInteger,Integer,BigInteger> op=BigInteger::clearBit;
                for(String x:values)for(int bit:new int[]{0,1,7,8,15,31,32,63,64,95,127,255,513}){BigInteger a=new BigInteger(x);
                    BigInteger result=a.clearBit(bit);if(!result.equals(op.apply(a,bit)))throw new Error();
                    hash=31*hash+result.toString().hashCode();hash=31*hash+a.toString().hashCode();}
                for(int bit:new int[]{-1,Integer.MIN_VALUE})try{BigInteger.ONE.clearBit(bit);throw new Error();}catch(ArithmeticException expected){hash++;}
                return hash;}}
            """);compare(List.of("run0","run1","run2","run3","run4","run5","run6","run7","run8","run9","run10","run11","run12","run13"));
    }

    @Test void flipBit() throws Exception {
        fixture("""
            import java.math.BigInteger;
            public class Metadata {public static int run0(){return sample("0");}public static int run1(){return sample("1");}public static int run2(){return sample("-1");}public static int run3(){return sample("127");}public static int run4(){return sample("128");}public static int run5(){return sample("255");}public static int run6(){return sample("256");}public static int run7(){return sample("-128");}public static int run8(){return sample("-129");}public static int run9(){return sample("-256");}public static int run10(){return sample("9223372036854775808");}public static int run11(){return sample("-9223372036854775809");}public static int run12(){return sample("123456789012345678901234567890");}public static int run13(){return sample("-123456789012345678901234567890");}private static int sample(String input){int hash=1;
                String[] values={input};
                java.util.function.BiFunction<BigInteger,Integer,BigInteger> op=BigInteger::flipBit;
                for(String x:values)for(int bit:new int[]{0,1,7,8,15,31,32,63,64,95,127,255,513}){BigInteger a=new BigInteger(x);
                    BigInteger result=a.flipBit(bit);if(!result.equals(op.apply(a,bit)))throw new Error();
                    hash=31*hash+result.toString().hashCode();hash=31*hash+a.toString().hashCode();}
                for(int bit:new int[]{-1,Integer.MIN_VALUE})try{BigInteger.ONE.flipBit(bit);throw new Error();}catch(ArithmeticException expected){hash++;}
                return hash;}}
            """);compare(List.of("run0","run1","run2","run3","run4","run5","run6","run7","run8","run9","run10","run11","run12","run13"));
    }

    private void compare(List<String> names) throws Exception {
        StringBuilder assertions=new StringBuilder();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)){
            Class<?> type=loader.loadClass(fixtureClassName);
            for(String name:names){Object expected=type.getMethod(name).invoke(null);assertInstanceOf(Integer.class,expected);
                assertions.append("assert.equal(e['").append(fixtureClassName).append(".").append(name).append("'](),").append(expected).append(");\n");}
        }
        for(boolean reverse:new boolean[]{false,true})execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),assertions.toString());
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
                function fresh(){
                    const memory=new WebAssembly.Memory({initial:2,maximum:20});let heap=1024,allocations=0;
                    const e=new WebAssembly.Instance(compiled,{env:{memory,__heap_alloc:size=>{
                        allocations++;const address=heap;heap+=size;
                        while(heap>memory.buffer.byteLength)memory.grow(1);
                        new Uint8Array(memory.buffer,address,size).fill(0x7f);return address;
                    }}}).exports;
                    assert.equal(allocations,0);heap=Math.max(heap,e.__exception_heap_base?.value??0);return e;
                }
            """+assertions.replace("e['","fresh()['")+"\n}");
        run("node",script.toString(),output.toString());
    }
}
