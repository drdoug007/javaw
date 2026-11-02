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

class BytecodeTranscendentalTest implements Opcodes {
    @TempDir Path directory;
    private String release="17";
    private String fixtureClasspath;
    private String fixtureClassName="Metadata";

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

    @Test void exponential() throws Exception {unary("exp",StrictMath::exp,Math::exp);}

    @Test void naturalLogarithm() throws Exception {unary("log",StrictMath::log,Math::log);}

    @Test void decimalLogarithm() throws Exception {unary("log10",StrictMath::log10,Math::log10);}

    @Test void powers() throws Exception {binary("pow",StrictMath::pow,Math::pow);}

    @Test void pinnedAntlrPowerRoundingBody() throws Exception {
        Path artifact=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()))
            .resolve("org/antlr/antlr4-runtime/4.13.0/antlr4-runtime-4.13.0.jar");
        assertEquals("bd7f7b5d07bc0b047f10915b32ca4bb1de9e57d8049098882e4453c88c076a5d",
            HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(artifact))));
        ClassNode original=new ClassNode();
        try(JarFile jar=new JarFile(artifact.toFile())){new ClassReader(jar.getInputStream(jar.getJarEntry("org/antlr/v4/runtime/CodePointBuffer$Builder.class")).readAllBytes()).accept(original,0);}
        MethodNode selected=original.methods.stream().filter(m->m.name.equals("roundUpToNextPowerOfTwo")).findFirst().orElseThrow();
        assertEquals("(I)I",selected.desc);
        assertTrue(Arrays.stream(selected.instructions.toArray()).anyMatch(insn->insn instanceof MethodInsnNode call && call.owner.equals("java/lang/Math") && call.name.equals("pow")));
        // Preserve the complete instruction body in a public test carrier; buffers/builders are excluded.
        selected.access=ACC_PUBLIC|ACC_STATIC;selected.name="round";
        ClassWriter writer=new ClassWriter(0);writer.visit(original.version,ACC_PUBLIC,"PinnedPower",null,"java/lang/Object",null);selected.accept(writer);writer.visitEnd();
        Files.write(directory.resolve("PinnedPower.class"),writer.toByteArray());fixtureClasspath=directory.toString();
        fixture("public class Metadata {public static int run(int group){int hash=1;for(int i=-100;i<100;i++)hash=31*hash+PinnedPower.round(i);"+
            "for(int bit=0;bit<31;bit++)for(int delta=-1;delta<=1;delta++)hash=31*hash+PinnedPower.round((1<<bit)+delta);return hash;}}");
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)){
            compare(List.of((Integer)loader.loadClass("Metadata").getMethod("run",int.class).invoke(null,0)));
        }
    }

    @Test void sine() throws Exception {unary("sin",StrictMath::sin,Math::sin);}

    @Test void cosine() throws Exception {unary("cos",StrictMath::cos,Math::cos);}

    @Test void tangent() throws Exception {unary("tan",StrictMath::tan,Math::tan);}

    @Test void inverseSine() throws Exception {unary("asin",StrictMath::asin,Math::asin);}

    @Test void inverseCosine() throws Exception {unary("acos",StrictMath::acos,Math::acos);}

    @Test void inverseTangent() throws Exception {unary("atan",StrictMath::atan,Math::atan);}

    @Test void atan2Pairs() throws Exception {binary("atan2",StrictMath::atan2,Math::atan2);}

    @Test void expm1Portable() throws Exception {unary("expm1",StrictMath::expm1,Math::expm1);}

    @Test void log1pPortable() throws Exception {unary("log1p",StrictMath::log1p,Math::log1p);}

    @Test void sinhPortable() throws Exception {unary("sinh",StrictMath::sinh,Math::sinh);}

    @Test void coshPortable() throws Exception {unary("cosh",StrictMath::cosh,Math::cosh);}

    @Test void tanhPortable() throws Exception {unary("tanh",StrictMath::tanh,Math::tanh);}

    @Test void cbrtPortable() throws Exception {unary("cbrt",StrictMath::cbrt,Math::cbrt);}

    @Test void hypotPairs() throws Exception {binary("hypot",StrictMath::hypot,Math::hypot);}

    @Test void IEEEremainderPairs() throws Exception {binary("IEEEremainder",StrictMath::IEEEremainder,Math::IEEEremainder);}

    @Test void strictMathAdapters() throws Exception {
        List<Double> inputs=values();StringJoiner bits=new StringJoiner(",");
        for(double x:inputs)bits.add(Double.doubleToRawLongBits(x)+"L");
        int groups=(inputs.size()+63)/64;
        String[] names={"exp","log","log10","pow","sin","cos","tan","asin","acos","atan","atan2","expm1","log1p","sinh","cosh","tanh","cbrt","hypot","IEEEremainder"};
        StringBuilder dispatch=new StringBuilder("switch(operation){");
        for(int i=0;i<names.length;i++){
            String name=names[i];boolean binary=List.of("pow","atan2","hypot","IEEEremainder").contains(name);
            dispatch.append("case ").append(i).append(":{")
                .append(binary?"java.util.function.DoubleBinaryOperator":"java.util.function.DoubleUnaryOperator")
                .append(" f=StrictMath::").append(name).append(";double r=StrictMath.").append(name)
                .append(binary?"(x,y);":"(x);")
                .append("if(Double.doubleToLongBits(r)!=Double.doubleToLongBits(f.")
                .append(binary?"applyAsDouble(x,y)))":"applyAsDouble(x)))")
                .append("throw new Error();return r;}");
        }
        dispatch.append("default:throw new Error();}");
        fixture("public class Metadata {static long[] bits={"+bits+"};static double call(int operation,double x,double y){"+dispatch+"}"+
            "public static int run(int group){int operation=group/"+groups+",start=(group%"+groups+")*64,hash=1;"+
            "for(int i=start;i<bits.length && i<start+64;i++){double x=Double.longBitsToDouble(bits[i]),y=Double.longBitsToDouble(bits[(i*31+7)%bits.length]);"+
            "long b=Double.doubleToLongBits(call(operation,x,y));hash=31*hash+(int)(b^(b>>>32));}return hash;}}");
        List<Integer> hashes=new ArrayList<>();
        for(String name:names){
            boolean binary=List.of("pow","atan2","hypot","IEEEremainder").contains(name);
            var oracle=StrictMath.class.getMethod(name,binary?new Class<?>[]{double.class,double.class}:new Class<?>[]{double.class});
            for(int start=0;start<inputs.size();start+=64){int hash=1;
                for(int i=start;i<Math.min(start+64,inputs.size());i++){
                    double x=inputs.get(i),y=inputs.get((i*31+7)%inputs.size());
                    double result=(double)oracle.invoke(null,binary?new Object[]{x,y}:new Object[]{x});
                    long b=Double.doubleToLongBits(result);hash=31*hash+(int)(b^(b>>>32));
                }hashes.add(hash);
            }
        }
        compare(hashes);
    }

    @Test void malformedDescriptors() {
        for(String owner:List.of("java/lang/Math","java/lang/StrictMath"))for(String name:List.of("exp","log","log10","pow","sin","cos","tan","asin","acos","atan","atan2","expm1","log1p","sinh","cosh","tanh","cbrt","hypot","IEEEremainder")){
            ClassWriter writer=new ClassWriter(0);writer.visit(V17,ACC_PUBLIC,"BadTranscendental",null,"java/lang/Object",null);
            MethodVisitor method=writer.visitMethod(ACC_PUBLIC|ACC_STATIC,"run","()F",null,null);
            method.visitCode();method.visitInsn(FCONST_0);if(List.of("pow","atan2","hypot","IEEEremainder").contains(name))method.visitInsn(FCONST_0);
            method.visitMethodInsn(INVOKESTATIC,owner,name,List.of("pow","atan2","hypot","IEEEremainder").contains(name)?"(FF)F":"(F)F",false);
            method.visitInsn(FRETURN);method.visitMaxs(2,0);method.visitEnd();writer.visitEnd();
            var failure=assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileClassWithExceptionRuntime(writer.toByteArray()));
            assertTrue(failure.getMessage().contains(name),failure.getMessage());
        }
    }

    private void binary(String name,java.util.function.DoubleBinaryOperator strict,java.util.function.DoubleBinaryOperator intrinsic) throws Exception {
        List<Double> bases=new ArrayList<>(),powers=new ArrayList<>();
        double[] edges={0,-0.0,1,-1,2,-2,3,-3,5,-5,7,-7,Math.nextDown(5),Math.nextUp(5),0.5,-0.5,Double.MIN_VALUE,-Double.MIN_VALUE,Double.MIN_NORMAL,
            Math.nextDown(0x1p500),0x1p500,Math.nextUp(0x1p500),0x1p-500,0x1p600,0x1p-600,
            Double.MAX_VALUE,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NaN,
            0x1.fffffffffffffp-1,0x1.0000000000001p0,-0x1.fffffffffffffp-1,-0x1.0000000000001p0};
        double[] exponents={0,-0.0,0.5,-0.5,1,-1,2,-2,3,-3,4,-4,31,-31,1023,-1023,1024,-1074,1075,
            0x1p31,-0x1p31,0x1p52-1,0x1p52,0x1p53-1,0x1p53,Math.nextDown(0x1p500),0x1p500,Math.nextUp(0x1p500),0x1p-500,0x1p600,0x1p-600,
            Double.MAX_VALUE,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NaN};
        for(double x:edges)for(double y:exponents){bases.add(x);powers.add(y);}
        long bits=17;for(int i=0;i<1536;i++){
            bits=bits*6364136223846793005L+1442695040888963407L;double x=Double.longBitsToDouble(bits);
            bits=bits*6364136223846793005L+1442695040888963407L;double y=i%3==0?(double)(int)(bits>>32):Double.longBitsToDouble(bits);
            bases.add(x);powers.add(y);
        }
        for(int i=0;i<512;i++){
            bits=bits*6364136223846793005L+1442695040888963407L;double x=(bits>>>11)*0x1p-52+0.5;
            bits=bits*6364136223846793005L+1442695040888963407L;double y=(bits>>11)*0x1p-41;
            bases.add(i%2==0?x:-x);powers.add(y);
        }
        for(int exponent=0;exponent<2048;exponent+=16){
            double x=Double.longBitsToDouble(((long)exponent<<52)|0x921fb54442d18L);
            for(double y:new double[]{Math.nextDown(x),x,Math.nextUp(x),-x,0x1p-1022,0x1p500}){
                bases.add(x);powers.add(y);bases.add(y);powers.add(-x);
            }
        }
        StringJoiner xb=new StringJoiner(","),yb=new StringJoiner(",");
        for(int i=0;i<bases.size();i++){xb.add(Double.doubleToRawLongBits(bases.get(i))+"L");yb.add(Double.doubleToRawLongBits(powers.get(i))+"L");}
        fixture("public class Metadata {static final long[] x=x(),y=y();static long[] x(){return new long[]{"+xb+"};}static long[] y(){return new long[]{"+yb+"};}"+
            "public static int run(int group){java.util.function.DoubleBinaryOperator reference=Math::"+name+";int hash=1;"+
            "for(int i=group*64;i<x.length && i<(group+1)*64;i++){double a=Double.longBitsToDouble(x[i]),b=Double.longBitsToDouble(y[i]);double result=Math."+name+"(a,b);"+
            "long bits=Double.doubleToLongBits(result);if(bits!=Double.doubleToLongBits(reference.applyAsDouble(a,b)))throw new Error();"+
            "hash=31*hash+(int)(bits^(bits>>>32));}return hash;}}");
        List<Integer> hashes=new ArrayList<>();
        for(int start=0;start<bases.size();start+=64){int hash=1;
            for(int i=start;i<Math.min(start+64,bases.size());i++){
                double x=bases.get(i),y=powers.get(i),expected=strict.applyAsDouble(x,y);
                checkMathAllowance(expected,intrinsic.applyAsDouble(x,y),name+" "+x+" "+y);
                long b=Double.doubleToLongBits(expected);hash=31*hash+(int)(b^(b>>>32));
            }hashes.add(hash);
        }compare(hashes);
    }

    private List<Double> values(){
        List<Double> values=new ArrayList<>();
        double[] boundaries={0.0,-0.0,1,-1,0.5,-0.5,2,-2,Math.PI,-Math.PI,Math.PI/2,-Math.PI/2,
            Math.PI/4,-Math.PI/4,0.4375,-0.4375,0.6875,-0.6875,1.1875,-1.1875,2.4375,-2.4375,0.975,-0.975,10,100,1000,1.0e-300,1.0e300,1.0e20,-1.0e20,709.782712893384,-745.1332191019411,
            22,-22,Math.log(2),-Math.log(2),56*Math.log(2),-56*Math.log(2),710.4758600739439,-710.4758600739439,
            27,-27,64,-64,0x1p-28,-0x1p-28,0x1p-54,-0x1p-54,0x1p20,-0x1p20,Double.MIN_NORMAL,-Double.MIN_NORMAL,
            Double.MAX_VALUE,-Double.MAX_VALUE,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NaN};
        for(double value:boundaries){values.add(Math.nextDown(value));values.add(value);values.add(Math.nextUp(value));}
        for(int exponent=0;exponent<2048;exponent++){
            long bits=((long)exponent<<52)|0x000921fb54442d18L;
            values.add(Double.longBitsToDouble(bits));values.add(Double.longBitsToDouble(bits|Long.MIN_VALUE));
        }
        for(int multiple=-32;multiple<=32;multiple++){
            double value=multiple*(Math.PI/2);values.add(Math.nextDown(value));values.add(value);values.add(Math.nextUp(value));
        }
        long seed=17;
        for(int i=0;i<512;i++){seed=seed*6364136223846793005L+1442695040888963407L;
            values.add(Double.longBitsToDouble(seed));values.add((seed>>>11)*0x1p-52-1.0);
        }
        return values;
    }

    private void unary(String name,java.util.function.DoubleUnaryOperator strict,java.util.function.DoubleUnaryOperator intrinsic) throws Exception {
        List<Double> values=values();StringJoiner bits=new StringJoiner(",");
        for(double value:values)bits.add(Double.doubleToRawLongBits(value)+"L");
        fixture("public class Metadata {static final long[] bits={"+bits+"};"+
            "public static int run(int group){java.util.function.DoubleUnaryOperator reference=Math::"+name+";int hash=1;"+
            "for(int i=group*64;i<bits.length && i<(group+1)*64;i++){double x=Double.longBitsToDouble(bits[i]);double y=Math."+name+"(x);"+
            "long b=Double.doubleToLongBits(y);if(b!=Double.doubleToLongBits(reference.applyAsDouble(x)))throw new Error();"+
            "hash=31*hash+(int)(b^(b>>>32));}return hash;}}");
        List<Integer> hashes=new ArrayList<>();
        for(int start=0;start<values.size();start+=64){int hash=1;
            for(int i=start;i<Math.min(start+64,values.size());i++){
                double x=values.get(i),expected=strict.applyAsDouble(x),actual=intrinsic.applyAsDouble(x);
                checkMathAllowance(expected,actual,name+" input "+Double.toHexString(x));
                long b=Double.doubleToLongBits(expected);hash=31*hash+(int)(b^(b>>>32));
            }hashes.add(hash);
        }
        compare(hashes);
    }

    private void checkMathAllowance(double expected,double actual,String context){
        if(Double.isNaN(expected)){assertTrue(Double.isNaN(actual),context);return;}
        if(expected==0.0 || Double.isInfinite(expected)){
            // This checks the host JVM's Math, not Wasm, which is compared with StrictMath. HotSpot's x86_64 pow
            // intrinsic treats the odd exponent 2^52-1 as even, so pow(-0.0, y) gives +0.0 and pow(-Infinity, y)
            // gives +Infinity against the Math.pow specification. Record a sign-only deviation of a zero or
            // infinite result rather than failing; any other mismatch still fails.
            if(actual==-expected && Double.doubleToLongBits(expected)!=Double.doubleToLongBits(actual)){
                System.err.println("Host Math deviates from its specification in the result sign ("+context+" gave "+actual+" on "
                    +System.getProperty("java.vm.name")+" "+System.getProperty("java.vm.version")+" "+System.getProperty("os.arch")+")");
                return;
            }
            assertEquals(Double.doubleToLongBits(expected),Double.doubleToLongBits(actual),context);return;
        }
        // Math and StrictMath each permit error; their results need not be bit-identical.
        assertTrue(Math.abs(expected-actual)<=2*Math.ulp(expected),context+" expected "+expected+" actual "+actual);
    }

    private void compare(List<Integer> hashes) throws Exception {
        String expected=hashes.toString();
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
                const memory=new WebAssembly.Memory({initial:2});let heap=1024,allocations=0;
                const e=new WebAssembly.Instance(compiled,{env:{memory,__heap_alloc:size=>{
                    allocations++;
                    const address=heap;heap+=size;
                    while(heap>memory.buffer.byteLength)memory.grow(1);
                    new Uint8Array(memory.buffer,address,size).fill(0x7f);return address;
                }}}).exports;
                assert.equal(allocations,0);assert.equal(e.__gc_heap_bytes?.()??0,0);if(e.__gc_scribble)e.__gc_scribble.value=1;heap=Math.max(heap,e.__exception_heap_base?.value??0);
            """.replace("EXPECTED",expected)+"assert.equal(e['"+fixtureClassName+".run'](group),expected[group],'batch '+group);\n}");
        run("node",script.toString(),output.toString());
    }
}
