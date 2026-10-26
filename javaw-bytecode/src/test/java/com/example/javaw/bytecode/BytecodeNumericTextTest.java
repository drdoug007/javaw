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

class BytecodeNumericTextTest implements Opcodes {
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

    @Test void unsignedShorts() throws Exception {
        fixture("""
            public class Metadata {public static int run(){int hash=1;
                java.util.function.ToIntFunction<Short> ints=Short::toUnsignedInt;
                java.util.function.ToLongFunction<Short> longs=Short::toUnsignedLong;
                for(int n=-32768;n<=32767;n++){short s=(short)n;int a=Short.toUnsignedInt(s);long b=Short.toUnsignedLong(s);
                    if(a!=(n&65535) || b!=a)throw new Error();hash=31*hash+a;}
                for(short s:new short[]{-32768,-129,-1,0,127,32767})hash+=ints.applyAsInt(s)+(int)longs.applyAsLong(s);
                return hash;}}
            """);compare(List.of("run"));
    }

    @Test void stringCopyFactories() throws Exception {
        fixture("""
            public class Metadata {public static int run(){int hash=1;
                char[] chars={'A',0,(char)0xd800,(char)0xdc00,'Z'};
                java.util.function.Function<char[],String> factory=String::copyValueOf;
                String snapshot=factory.apply(chars);chars[0]='B';hash=31*hash+snapshot.hashCode();
                for(int start=0;start<=chars.length;start++)for(int count=0;count<=chars.length-start;count++)
                    hash=31*hash+String.copyValueOf(chars,start,count).hashCode();
                for(int[] slice:new int[][]{{-1,1},{0,-1},{4,2},{Integer.MAX_VALUE,1},{1,Integer.MAX_VALUE}})
                    try{String.copyValueOf(chars,slice[0],slice[1]);throw new Error();}catch(StringIndexOutOfBoundsException e){hash++;}
                try{String.copyValueOf(null);throw new Error();}catch(NullPointerException e){hash++;}
                try{String.copyValueOf(null,-1,-1);throw new Error();}catch(NullPointerException e){hash++;}
                return hash;}}
            """);compare(List.of("run"));
    }

    @Test void floatingExponents() throws Exception {
        fixture("""
            public class Metadata {
                static int mix(int h,float v){return 31*h+Float.floatToIntBits(v);}
                static int mix(int h,double v){long b=Double.doubleToLongBits(v);return 31*h+(int)(b^(b>>>32));}
                static int sample(int hash,int bits,long wide){float f=Float.intBitsToFloat(bits);double d=Double.longBitsToDouble(wide);
                    hash=mix(hash,(float)Math.getExponent(f));hash=mix(hash,(double)Math.getExponent(d));return hash;}
                public static int run(){int hash=1;
                    for(int e=0;e<256;e++)for(int delta=-1;delta<=1;delta++)for(int sign=0;sign<2;sign++)
                        hash=sample(hash,((e<<23)+delta)^(sign<<31),0);
                    for(int e=0;e<2048;e++)for(int delta=-1;delta<=1;delta++)for(int sign=0;sign<2;sign++)
                        hash=sample(hash,0,(((long)e<<52)+delta)^((long)sign<<63));
                    long seed=17;for(int i=0;i<3000;i++){seed=seed*6364136223846793005L+1442695040888963407L;hash=sample(hash,(int)(seed>>>32),seed);}
                    java.util.function.DoubleToIntFunction op=Math::getExponent;hash+=op.applyAsInt(-0.0); return hash;}}

            """);compare(List.of("run"));
    }

    @Test void floatingUlps() throws Exception {
        fixture("""
            public class Metadata {
                static int mix(int h,float v){return 31*h+Float.floatToIntBits(v);}
                static int mix(int h,double v){long b=Double.doubleToLongBits(v);return 31*h+(int)(b^(b>>>32));}
                static int sample(int hash,int bits,long wide){float f=Float.intBitsToFloat(bits);double d=Double.longBitsToDouble(wide);
                    hash=mix(hash,Math.ulp(f));hash=mix(hash,Math.ulp(d));return hash;}
                public static int run(){int hash=1;
                    for(int e=0;e<256;e++)for(int delta=-1;delta<=1;delta++)for(int sign=0;sign<2;sign++)
                        hash=sample(hash,((e<<23)+delta)^(sign<<31),0);
                    for(int e=0;e<2048;e++)for(int delta=-1;delta<=1;delta++)for(int sign=0;sign<2;sign++)
                        hash=sample(hash,0,(((long)e<<52)+delta)^((long)sign<<63));
                    long seed=17;for(int i=0;i<3000;i++){seed=seed*6364136223846793005L+1442695040888963407L;hash=sample(hash,(int)(seed>>>32),seed);}
                    java.util.function.DoubleUnaryOperator op=Math::ulp;hash=mix(hash,op.applyAsDouble(1.0)); return hash;}}

            """);compare(List.of("run"));
    }

    @Test void floatingNextUp() throws Exception {
        fixture("""
            public class Metadata {
                static int mix(int h,float v){return 31*h+Float.floatToIntBits(v);}
                static int mix(int h,double v){long b=Double.doubleToLongBits(v);return 31*h+(int)(b^(b>>>32));}
                static int sample(int hash,int bits,long wide){float f=Float.intBitsToFloat(bits);double d=Double.longBitsToDouble(wide);
                    hash=mix(hash,Math.nextUp(f));hash=mix(hash,Math.nextUp(d));return hash;}
                public static int run(){int hash=1;
                    for(int e=0;e<256;e++)for(int delta=-1;delta<=1;delta++)for(int sign=0;sign<2;sign++)
                        hash=sample(hash,((e<<23)+delta)^(sign<<31),0);
                    for(int e=0;e<2048;e++)for(int delta=-1;delta<=1;delta++)for(int sign=0;sign<2;sign++)
                        hash=sample(hash,0,(((long)e<<52)+delta)^((long)sign<<63));
                    long seed=17;for(int i=0;i<3000;i++){seed=seed*6364136223846793005L+1442695040888963407L;hash=sample(hash,(int)(seed>>>32),seed);}
                    java.util.function.DoubleUnaryOperator op=Math::nextUp;hash=mix(hash,op.applyAsDouble(-0.0)); return hash;}}

            """);compare(List.of("run"));
    }

    @Test void floatingNextDown() throws Exception {
        fixture("""
            public class Metadata {
                static int mix(int h,float v){return 31*h+Float.floatToIntBits(v);}
                static int mix(int h,double v){long b=Double.doubleToLongBits(v);return 31*h+(int)(b^(b>>>32));}
                static int sample(int hash,int bits,long wide){float f=Float.intBitsToFloat(bits);double d=Double.longBitsToDouble(wide);
                    hash=mix(hash,Math.nextDown(f));hash=mix(hash,Math.nextDown(d));return hash;}
                public static int run(){int hash=1;
                    for(int e=0;e<256;e++)for(int delta=-1;delta<=1;delta++)for(int sign=0;sign<2;sign++)
                        hash=sample(hash,((e<<23)+delta)^(sign<<31),0);
                    for(int e=0;e<2048;e++)for(int delta=-1;delta<=1;delta++)for(int sign=0;sign<2;sign++)
                        hash=sample(hash,0,(((long)e<<52)+delta)^((long)sign<<63));
                    long seed=17;for(int i=0;i<3000;i++){seed=seed*6364136223846793005L+1442695040888963407L;hash=sample(hash,(int)(seed>>>32),seed);}
                    java.util.function.DoubleUnaryOperator op=Math::nextDown;hash=mix(hash,op.applyAsDouble(0.0)); return hash;}}

            """);compare(List.of("run"));
    }

    @Test void floatingNextAfter() throws Exception {
        fixture("""
            public class Metadata {
                static int mix(int h,float v){return 31*h+Float.floatToIntBits(v);}
                static int mix(int h,double v){long b=Double.doubleToLongBits(v);return 31*h+(int)(b^(b>>>32));}
                static int sample(int hash,int bits,long wide){float f=Float.intBitsToFloat(bits);double d=Double.longBitsToDouble(wide);
                    hash=mix(hash,Math.nextAfter(f,d));hash=mix(hash,Math.nextAfter(d,(double)f));return hash;}
                public static int run(){int hash=1;
                    for(int e=0;e<256;e++)for(int delta=-1;delta<=1;delta++)for(int sign=0;sign<2;sign++)
                        hash=sample(hash,((e<<23)+delta)^(sign<<31),0);
                    for(int e=0;e<2048;e++)for(int delta=-1;delta<=1;delta++)for(int sign=0;sign<2;sign++)
                        hash=sample(hash,0,(((long)e<<52)+delta)^((long)sign<<63));
                    long seed=17;for(int i=0;i<3000;i++){seed=seed*6364136223846793005L+1442695040888963407L;hash=sample(hash,(int)(seed>>>32),seed);}

                    double[] directions={-1.0/0.0,-1,-Double.MIN_VALUE,-0.0,0.0,Double.MIN_VALUE,1,1.0/0.0,0.0/0.0};
                    float[] starts={-1.0f/0.0f,-1,-Float.MIN_VALUE,-0.0f,0.0f,Float.MIN_VALUE,1,1.0f/0.0f,0.0f/0.0f};
                    for(double direction:directions){for(float start:starts)hash=mix(hash,Math.nextAfter(start,direction));
                        for(double start:directions)hash=mix(hash,Math.nextAfter(start,direction));}
                    // Direction must retain double precision for the float overload.
                    hash=mix(hash,Math.nextAfter(1.0f,1.0+0x1p-40));
                    hash=mix(hash,Math.nextAfter(1.0f,1.0-0x1p-40));
                    java.util.function.DoubleBinaryOperator op=Math::nextAfter;hash=mix(hash,op.applyAsDouble(0.0,-0.0)); return hash;}}

            """);compare(List.of("run"));
    }

    @Test void floatingScaling() throws Exception {
        fixture("""
            public class Metadata {
                static final int[] scales={Integer.MIN_VALUE,-2100,-2099,-1075,-1074,-1024,-1023,-1022,-1021,-513,-512,-511,-279,-278,-277,-150,-149,-127,-126,-125,-54,-53,-52,-25,-24,-23,-1,0,1,23,24,52,53,125,126,127,128,277,278,279,511,512,513,1023,1024,2099,2100,Integer.MAX_VALUE};
                static int sample(int hash,int bits,long wide){
                    float f=Float.intBitsToFloat(bits);double d=Double.longBitsToDouble(wide);
                    for(int scale:scales){hash=31*hash+Float.floatToIntBits(Math.scalb(f,scale));
                        long b=Double.doubleToLongBits(Math.scalb(d,scale));hash=31*hash+(int)(b^(b>>>32));}return hash;
                }
                public static int run(){int hash=1;
                    for(int e=0;e<256;e++)for(int delta=-1;delta<=1;delta++)for(int sign=0;sign<2;sign++)
                        hash=sample(hash,((e<<23)+delta)^(sign<<31),0);
                    for(int e=0;e<2048;e++)for(int delta=-1;delta<=1;delta++)for(int sign=0;sign<2;sign++)
                        hash=sample(hash,0,(((long)e<<52)+delta)^((long)sign<<63));
                    long seed=17;for(int i=0;i<3000;i++){seed=seed*6364136223846793005L+1442695040888963407L;hash=sample(hash,(int)(seed>>>32),seed);}
                    java.util.function.BiFunction<Double,Integer,Double> op=Math::scalb;hash+=op.apply(1.0,-1074).hashCode();
                    return hash;}}

            """);compare(List.of("run"));
    }

    @Test void unsignedBytesAndMethodReferences() throws Exception {
        fixture("""
            import java.util.function.*;
            public class Metadata {public static int run(){int hash=1;
                ToIntFunction<Byte> ints=Byte::toUnsignedInt;ToLongFunction<Byte> longs=Byte::toUnsignedLong;
                for(int n=-128;n<128;n++){byte b=(byte)n;int value=Byte.toUnsignedInt(b);long wide=Byte.toUnsignedLong(b);
                    if(value!=(n&255) || wide!=value || ints.applyAsInt(b)!=value || longs.applyAsLong(b)!=wide)throw new Error();
                    hash=31*hash+value;}
                return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void executesPinnedHibernateRowVersionComparators() throws Exception {
        Path repository=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()));
        Path artifact=repository.resolve("org/hibernate/orm/hibernate-core/6.4.4.Final/hibernate-core-6.4.4.Final.jar");
        assertDigest(artifact,"a1324b7c80c800826c9a5d74b61b0de768141f967c2b082650cb7bf4675570a7");
        try(JarFile jar=new JarFile(artifact.toFile())){
            for(String name:List.of("org/hibernate/internal/util/compare/RowVersionComparator","org/hibernate/internal/util/compare/ComparableComparator"))
                writeClass(name,jar.getInputStream(jar.getJarEntry(name+".class")).readAllBytes());
        }
        fixtureClasspath=directory.toString();
        fixture("""
            import java.util.*;import org.hibernate.internal.util.compare.*;
            public class Metadata {public static int run(){int hash=1;
                byte[][] values={{},{0},{1},{127},{-128},{-1},{0,0},{0,-1},{-1,0}};
                Comparator<byte[]> typed=RowVersionComparator.INSTANCE;Comparator raw=typed;
                for(byte[] left:values)for(byte[] right:values){int result=typed.compare(left,right);
                    if(raw.compare(left,right)!=result)throw new Error();hash=31*hash+result;}
                try{typed.compare(null,new byte[0]);throw new Error();}catch(NullPointerException e){hash++;}
                try{raw.compare("bad",new byte[0]);throw new Error();}catch(ClassCastException e){hash++;}
                return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void utf8DecodingAllBytePairs() throws Exception {
        assertEquals(java.nio.charset.StandardCharsets.UTF_8,java.nio.charset.Charset.defaultCharset());
        StringBuilder source=new StringBuilder("public class Metadata {static int pairs(int start){int hash=1;for(int a=start;a<start+16;a++)for(int b=0;b<256;b++){byte[] bytes={(byte)a,(byte)b};String text=new String(bytes);hash=31*hash+text.hashCode()+text.length();}return hash;} ");
        List<String> names=new ArrayList<>();
        for(int batch=0;batch<16;batch++){String name="run"+batch;names.add(name);source.append("public static int ").append(name).append("(){return pairs(").append(batch*16).append(");}");}
        fixture(source.append("}").toString());compare(names);
    }

    @Test void utf8DecodingBoundariesSlicesAndSnapshots() throws Exception {
        fixture("""
            public class Metadata {
                static final int[] tails={0,0x7f,0x80,0x8f,0x90,0x9f,0xa0,0xbf,0xc0,0xff};
                static int triples(int first){int hash=1;for(int b:tails)for(int c:tails){byte[] bytes={(byte)first,(byte)b,(byte)c};
                    for(int size=1;size<=3;size++){String text=new String(bytes,0,size);hash=31*hash+text.hashCode()+text.length();}}return hash;}
                static int quads(int first){int hash=1;for(int b:tails)for(int c:tails)for(int d:tails){byte[] bytes={(byte)first,(byte)b,(byte)c,(byte)d};
                    for(int size=2;size<=4;size++){String text=new String(bytes,0,size);hash=31*hash+text.hashCode()+text.length();}}return hash;}
                public static int a(){return triples(0xe0)+triples(0xed);}
                public static int b(){return triples(0xe1)+triples(0xef);}
                public static int c(){return quads(0xf0);}
                public static int d(){return quads(0xf1);}
                public static int e(){return quads(0xf4);}
                public static int failures(){int hash=1;
                    byte[] bytes={99,65,(byte)0xc3,(byte)0xa9,90};String text=new String(bytes,1,3);bytes[1]=66;
                    if(!text.equals("Aé"))throw new Error();
                    for(int[] slice:new int[][]{{-1,1},{0,-1},{4,2},{Integer.MAX_VALUE,1},{1,Integer.MAX_VALUE}})
                        try{new String(bytes,slice[0],slice[1]);throw new Error();}catch(StringIndexOutOfBoundsException e){hash++;}
                    try{new String((byte[])null);throw new Error();}catch(NullPointerException e){hash++;}
                    try{new String((byte[])null,-1,-1);throw new Error();}catch(NullPointerException e){hash++;}
                    for(int n=0;n<256;n++){String one=new String(new byte[]{(byte)n});hash=31*hash+one.hashCode();}
                    return hash;}
            }
            """);compare(List.of("a","b","c","d","e","failures"));
    }

    private void selectedSnakeYamlBase64(boolean encoding) throws Exception {
        Path repository=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()));
        Path artifact=repository.resolve("org/yaml/snakeyaml/2.2/snakeyaml-2.2.jar");
        assertDigest(artifact,"1467931448a0817696ae2805b7b8b20bfb082652bf9c4efaed528930dc49389b");
        try(JarFile jar=new JarFile(artifact.toFile())){
            ClassNode node=new ClassNode();new ClassReader(jar.getInputStream(jar.getJarEntry("org/yaml/snakeyaml/external/biz/base64Coder/Base64Coder.class")).readAllBytes()).accept(node,0);
            node.fields.removeIf(f->f.name.equals("systemLineSeparator"));
            node.methods.removeIf(m->m.name.equals("encodeLines") || m.name.equals("decodeLines") || !encoding && m.name.equals("encodeString"));
            MethodNode initializer=node.methods.stream().filter(m->m.name.equals("<clinit>")).findFirst().orElseThrow();
            List<AbstractInsnNode> prefix=new ArrayList<>();
            for(AbstractInsnNode instruction:initializer.instructions){if(instruction.getOpcode()>=0)prefix.add(instruction);if(prefix.size()==3)break;}
            assertEquals("line.separator",((LdcInsnNode)prefix.get(0)).cst);
            assertEquals("java/lang/System",((MethodInsnNode)prefix.get(1)).owner);
            assertEquals("getProperty",((MethodInsnNode)prefix.get(1)).name);
            assertEquals("systemLineSeparator",((FieldInsnNode)prefix.get(2)).name);
            prefix.forEach(initializer.instructions::remove);
            ClassWriter writer=new ClassWriter(0);node.accept(writer);writeClass(node.name,writer.toByteArray());
        }
        fixtureClasspath=directory.toString();
    }

    @Test void executesSelectedPinnedSnakeYamlUtf8Decoding() throws Exception {
        selectedSnakeYamlBase64(false);
        fixture("""
            import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;
            public class Metadata {public static int run(){int hash=1;
                for(String encoded:new String[]{"","QQ==","w6k=","8J+YgA==","wK8=","7aCA","8J8=","9JCAgA=="}){
                    String decoded=Base64Coder.decodeString(encoded);hash=31*hash+decoded.hashCode()+decoded.length();}
                try{Base64Coder.decodeString("!");throw new Error();}catch(IllegalArgumentException e){hash++;}
                return hash;}}
            """);compare(List.of("run"));
    }

    @Test void utf8EncodingEveryUtf16Unit() throws Exception {
        assertEquals(java.nio.charset.StandardCharsets.UTF_8,java.nio.charset.Charset.defaultCharset());
        StringBuilder source=new StringBuilder("import java.util.*;public class Metadata {static int units(int start){int hash=1;for(int c=start;c<start+4096;c++){String text=new String(new char[]{(char)c});byte[] bytes=text.getBytes();hash=31*hash+Arrays.hashCode(bytes)+bytes.length;}return hash;} ");
        List<String> names=new ArrayList<>();
        for(int batch=0;batch<16;batch++){String name="run"+batch;names.add(name);source.append("public static int ").append(name).append("(){return units(").append(batch*4096).append(");}");}
        fixture(source.append("}").toString());compare(names);
    }

    @Test void utf8EncodingPairsMalformedSurrogatesAndSnapshots() throws Exception {
        fixture("""
            import java.util.*;
            public class Metadata {
                public static int pairs(){int hash=1;
                    for(int high=0xd800;high<=0xdbff;high++)for(int low:new int[]{0xdc00,0xdc01,0xdfff}){
                        String text=new String(new char[]{(char)high,(char)low});byte[] bytes=text.getBytes();
                        if(!text.equals(new String(bytes)))throw new Error();hash=31*hash+Arrays.hashCode(bytes);}
                    return hash;}
                public static int malformed(){int hash=1;int[] units={0x41,0x7ff,0x800,0xd800,0xdbff,0xdc00,0xdfff};
                    for(int a:units)for(int b:units)for(int c:units){String text=new String(new char[]{(char)a,(char)b,(char)c});hash=31*hash+Arrays.hashCode(text.getBytes());}
                    String text="é😀";byte[] first=text.getBytes(),second=text.getBytes();if(first==second)throw new Error();
                    first[0]=0;if(!text.equals(new String(second)) || text.getBytes()[0]==0)throw new Error();
                    if("".getBytes()=="".getBytes())throw new Error();return hash;}
            }
            """);compare(List.of("pairs","malformed"));
    }

    @Test void executesSelectedPinnedSnakeYamlUtf8Encoding() throws Exception {
        selectedSnakeYamlBase64(true);
        fixture("""
            import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;
            public class Metadata {public static int run(){int hash=1;
                for(String text:new String[]{"","ASCII","é","😀",new String(new char[]{97,0xd800,98}),new String(new char[]{97,0xdc00,98})}){
                    String encoded=Base64Coder.encodeString(text);hash=31*hash+encoded.hashCode();
                    hash=31*hash+Base64Coder.decodeString(encoded).hashCode();}
                try{Base64Coder.encodeString(null);throw new Error();}catch(NullPointerException e){hash++;}
                return hash;}}
            """);compare(List.of("run"));
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
