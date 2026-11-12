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
        return jar();
    }

    private byte[] jar() throws Exception {
        List<Path> files;
        try(var paths=Files.walk(directory)) {
            files=new ArrayList<>(paths.filter(path->path.toString().endsWith(".class")).sorted().toList());
        }
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

    @Test void bitCount() throws Exception {
        fixture("""
            import java.math.BigInteger;
            public class Metadata {public static int run0(){return sample(0);}public static int run1(){return sample(12);}public static int run2(){return sample(24);}public static int run3(){return sample(36);}public static int run4(){return sample(48);}public static int run5(){return sample(60);}public static int run6(){return sample(72);}public static int run7(){return sample(84);}public static int run8(){return sample(96);}public static int run9(){return sample(108);}public static int run10(){return sample(120);}public static int run11(){return sample(132);}public static int run12(){return sample(144);}public static int run13(){return sample(156);}public static int run14(){return sample(168);}private static int sample(int offset){int hash=1;
                String[] values={"0","1","-1","127","128","255","256","-128","-129","-256","9223372036854775808","-9223372036854775809","123456789012345678901234567890","-123456789012345678901234567890"};
                java.util.function.ToIntFunction<BigInteger> op=BigInteger::bitCount;
                for(String x:values){BigInteger a=new BigInteger(x);if(a.bitCount()!=op.applyAsInt(a))throw new Error();hash=31*hash+a.bitCount();}
                for(int bit=offset;bit<offset+12;bit++){BigInteger a=BigInteger.ONE.shiftLeft(bit);hash=31*hash+a.bitCount();hash=31*hash+a.negate().bitCount();}
                return hash;}}
            """);compare(List.of("run0","run1","run2","run3","run4","run5","run6","run7","run8","run9","run10","run11","run12","run13","run14"));
    }

    @Test void getLowestSetBit() throws Exception {
        fixture("""
            import java.math.BigInteger;
            public class Metadata {public static int run0(){return sample(0);}public static int run1(){return sample(12);}public static int run2(){return sample(24);}public static int run3(){return sample(36);}public static int run4(){return sample(48);}public static int run5(){return sample(60);}public static int run6(){return sample(72);}public static int run7(){return sample(84);}public static int run8(){return sample(96);}public static int run9(){return sample(108);}public static int run10(){return sample(120);}public static int run11(){return sample(132);}public static int run12(){return sample(144);}public static int run13(){return sample(156);}public static int run14(){return sample(168);}private static int sample(int offset){int hash=1;
                String[] values={"0","1","-1","127","128","255","256","-128","-129","-256","9223372036854775808","-9223372036854775809","123456789012345678901234567890","-123456789012345678901234567890"};
                java.util.function.ToIntFunction<BigInteger> op=BigInteger::getLowestSetBit;
                for(String x:values){BigInteger a=new BigInteger(x);if(a.getLowestSetBit()!=op.applyAsInt(a))throw new Error();hash=31*hash+a.getLowestSetBit();}
                for(int bit=offset;bit<offset+12;bit++){BigInteger a=BigInteger.ONE.shiftLeft(bit);hash=31*hash+a.getLowestSetBit();hash=31*hash+a.negate().getLowestSetBit();}
                return hash;}}
            """);compare(List.of("run0","run1","run2","run3","run4","run5","run6","run7","run8","run9","run10","run11","run12","run13","run14"));
    }

    @Test void malformedBitOperationAdmission() {
        String owner="java/math/BigInteger";
        for(String name:new String[]{"and","or","xor","andNot","not","setBit","clearBit","flipBit","bitCount","getLowestSetBit"}) {
            String descriptor=switch(name){
                case "and","or","xor","andNot" -> "(Ljava/math/BigInteger;)Ljava/math/BigInteger;";
                case "setBit","clearBit","flipBit" -> "(I)Ljava/math/BigInteger;";
                case "not" -> "()Ljava/math/BigInteger;";
                default -> "()I";
            };
            for(int variant=0;variant<4;variant++) {
                ClassWriter writer=new ClassWriter(0);
                writer.visit(V17,ACC_PUBLIC,"BadBits",null,"java/lang/Object",null);
                writer.visitSource("BitCalls.java",null);
                MethodVisitor method=writer.visitMethod(ACC_PUBLIC|ACC_STATIC,"unused","()Ljava/lang/Object;",null,null);
                method.visitCode();Label line=new Label();method.visitLabel(line);method.visitLineNumber(42,line);
                String signature=variant%2==0?"(J)Ljava/lang/Object;":descriptor;
                if(variant<2) {
                    method.visitInsn(ACONST_NULL);method.visitInsn(ACONST_NULL);
                    method.visitMethodInsn(variant==1?INVOKESTATIC:INVOKEVIRTUAL,owner,name,signature,false);
                } else method.visitLdcInsn(new Handle(variant==3?H_INVOKESTATIC:H_INVOKEVIRTUAL,owner,name,signature,false));
                method.visitInsn(ARETURN);method.visitMaxs(4,0);method.visitEnd();writer.visitEnd();
                var failure=assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileClassWithExceptionRuntime(writer.toByteArray()));
                assertTrue(failure.getMessage().contains("unused"),failure.getMessage());
                assertTrue(failure.getMessage().contains("BitCalls.java:42"),failure.getMessage());
            }
        }
    }

    private void compare(List<String> names) throws Exception {
        StringBuilder assertions=new StringBuilder();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)){
            Class<?> type=loader.loadClass(fixtureClassName);
            for(String name:names){Object expected=type.getMethod(name).invoke(null);assertInstanceOf(Integer.class,expected);
                assertions.append("assert.equal(e['").append(fixtureClassName).append(".").append(name).append("'](),").append(expected).append(");\n");}
        }
        execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar())),assertions.toString());
    }

    private void execute(String wat,String assertions) throws Exception {
        Path input=directory.resolve("metadata.wat"),output=directory.resolve("metadata.wasm"),script=directory.resolve("run.cjs");
        Files.writeString(input,wat);WasmTestSupport.assemble(input,output,Wat2Wasm.EXCEPTIONS);
        Files.writeString(script,"""
            const fs=require('node:fs'),assert=require('node:assert/strict');
            const compiled=new WebAssembly.Module(fs.readFileSync(process.argv[2]));
            for(let instance=0;instance<2;instance++) {
                function fresh(){
                    const memory=new WebAssembly.Memory({initial:2});let heap=1024,allocations=0;
                    const e=new WebAssembly.Instance(compiled,{env:{memory,__heap_alloc:size=>{
                        allocations++;const address=heap;heap+=size;
                        while(heap>memory.buffer.byteLength)memory.grow(1);
                        new Uint8Array(memory.buffer,address,size).fill(0x7f);return address;
                    }}}).exports;
                    assert.equal(allocations,0);assert.equal(e.__gc_heap_bytes?.()??0,0);if(e.__gc_scribble)e.__gc_scribble.value=1;heap=Math.max(heap,e.__exception_heap_base?.value??0);return e;
                }
            """+assertions.replace("e['","fresh()['")+"\n}");
        WasmTestSupport.node(script,output);
    }
}
