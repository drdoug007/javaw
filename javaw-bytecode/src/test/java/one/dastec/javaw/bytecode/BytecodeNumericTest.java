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

class BytecodeNumericTest implements Opcodes {
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

    @Test void integerBits() throws Exception {
        fixture("""
            public class Metadata { public static int run() {
int hash=1,value=123456789;
 for(int i=0;i<600;i++){value=value*1664525+1013904223;if(i<34)value=i==0?0:1<<(i-1);
 hash=31*hash+Integer.bitCount(value);hash=31*hash+Integer.numberOfLeadingZeros(value);hash=31*hash+Integer.numberOfTrailingZeros(value);
 hash=31*hash+Integer.highestOneBit(value);hash=31*hash+Integer.lowestOneBit(value);hash=31*hash+Integer.reverse(value);hash=31*hash+Integer.reverseBytes(value);
 for(int d=-65;d<=65;d++)hash=31*hash+Integer.rotateLeft(value,d)+Integer.rotateRight(value,d);
 hash+=Integer.sum(value,17)+Integer.min(value,-3)+Integer.max(value,7); }return hash;
            }}
            """); compare(List.of("run"));
    }

    @Test void longBits() throws Exception {
        fixture("""
            public class Metadata { public static int run() {
int hash=1;long value=123456789;
 for(int i=0;i<600;i++){value=value*6364136223846793005L+1442695040888963407L;if(i<66)value=i==0?0:1L<<(i-1);
 hash=31*hash+Long.bitCount(value)+Long.numberOfLeadingZeros(value)+Long.numberOfTrailingZeros(value)+Long.signum(value);
 hash=31*hash+Long.hashCode(Long.highestOneBit(value)^Long.lowestOneBit(value)^Long.reverse(value)^Long.reverseBytes(value));
 for(int d=-129;d<=129;d++)hash=31*hash+Long.hashCode(Long.rotateLeft(value,d)^Long.rotateRight(value,d));
 hash+=Long.hashCode(Long.sum(value,17)^Long.min(value,-3)^Long.max(value,7));}return hash;
            }}
            """); compare(List.of("run"));
    }

    @Test void unsignedIntegers() throws Exception {
        fixture("""
            public class Metadata { public static int run() {
int hash=1;int[] values={0,1,-1,Integer.MIN_VALUE,Integer.MAX_VALUE,65535,-65536,7,-7};
 for(int a:values){for(int b:values){hash=31*hash+Integer.compareUnsigned(a,b);if(b!=0){hash=31*hash+Integer.divideUnsigned(a,b);hash=31*hash+Integer.remainderUnsigned(a,b);}}
 for(int r=2;r<=36;r++){String s=Integer.toUnsignedString(a,r);hash=31*hash+s.hashCode();if(Integer.parseUnsignedInt(s,r)!=a)throw new Error();}
 hash+=Long.hashCode(Integer.toUnsignedLong(a));}
 hash+=Integer.parseUnsignedInt(new StringBuilder("x+4294967295z"),1,12,10);hash+=Integer.parseUnsignedInt("+１２٣",10)+Integer.parseUnsignedInt("42");
 for(String s:new String[]{null,"","+","-0","-1","4294967296"," 1","1_0"})try{Integer.parseUnsignedInt(s);throw new Error();}catch(NumberFormatException e){hash++;}
 try{Integer.divideUnsigned(1,0);throw new Error();}catch(ArithmeticException e){hash++;}
 try{Integer.remainderUnsigned(1,0);throw new Error();}catch(ArithmeticException e){hash++;}
 try{Integer.parseUnsignedInt((CharSequence)null,0,1,10);throw new Error();}catch(NullPointerException e){hash++;}
 try{Integer.parseUnsignedInt("x",-1,1,10);throw new Error();}catch(IndexOutOfBoundsException e){hash++;}
 return hash;
            }}
            """); compare(List.of("run"));
    }

    @Test void unsignedLongs() throws Exception {
        fixture("""
            public class Metadata { public static int run() {
int hash=1;long[] values={0,1,-1,Long.MIN_VALUE,Long.MAX_VALUE,4294967295L,-4294967296L,7,-7};
 for(long a:values){for(long b:values){hash=31*hash+Long.compareUnsigned(a,b);if(b!=0){hash=31*hash+Long.hashCode(Long.divideUnsigned(a,b));hash=31*hash+Long.hashCode(Long.remainderUnsigned(a,b));}}
 for(int r=2;r<=36;r++){String s=Long.toUnsignedString(a,r);hash=31*hash+s.hashCode();if(Long.parseUnsignedLong(s,r)!=a)throw new Error();}}
 hash+=Long.hashCode(Long.parseUnsignedLong(new StringBuilder("x+18446744073709551615z"),1,22,10));hash+=Long.parseUnsignedLong("+１２٣",10)+Long.parseUnsignedLong("42");
 for(String s:new String[]{null,"","+","-0","-1","18446744073709551616"," 1","1_0"})try{Long.parseUnsignedLong(s);throw new Error();}catch(NumberFormatException e){hash++;}
 try{Long.divideUnsigned(-1,0);throw new Error();}catch(ArithmeticException e){hash++;}
 try{Long.remainderUnsigned(1,0);throw new Error();}catch(ArithmeticException e){hash++;}
 try{Long.parseUnsignedLong((CharSequence)null,0,1,10);throw new Error();}catch(NullPointerException e){hash++;}
 try{Long.parseUnsignedLong("x",-1,1,10);throw new Error();}catch(IndexOutOfBoundsException e){hash++;}
 return hash;
            }}
            """); compare(List.of("run"));
    }

    @Test void checkedAdditiveArithmetic() throws Exception {
        fixture("""
            public class Metadata { public static int run() {
int hash=1;long[] values={0,1,-1,Integer.MIN_VALUE,Integer.MAX_VALUE,2147483648L,-2147483649L,Long.MIN_VALUE,Long.MAX_VALUE};
 for(long a:values){for(long b:values){
 try{hash=31*hash+Long.hashCode(Math.addExact(a,b));}catch(ArithmeticException e){hash+=7;}
 try{hash=31*hash+Long.hashCode(Math.subtractExact(a,b));}catch(ArithmeticException e){hash+=11;}
 try{hash=31*hash+Math.addExact((int)a,(int)b);}catch(ArithmeticException e){hash+=13;}
 try{hash=31*hash+Math.subtractExact((int)a,(int)b);}catch(ArithmeticException e){hash+=17;}}
 try{hash+=Math.toIntExact(a);}catch(ArithmeticException e){hash+=19;}
 try{hash+=Long.hashCode(Math.incrementExact(a));}catch(ArithmeticException e){hash+=23;}
 try{hash+=Long.hashCode(Math.decrementExact(a));}catch(ArithmeticException e){hash+=29;}
 try{hash+=Long.hashCode(Math.negateExact(a));}catch(ArithmeticException e){hash+=31;}
 try{hash+=Math.incrementExact((int)a);}catch(ArithmeticException e){hash+=37;}
 try{hash+=Math.decrementExact((int)a);}catch(ArithmeticException e){hash+=41;}
 try{hash+=Math.negateExact((int)a);}catch(ArithmeticException e){hash+=43;}}
 return hash;
            }}
            """); compare(List.of("run"));
    }

    @Test void checkedMultiplication() throws Exception {
        fixture("""
            public class Metadata { public static int run() {
int hash=1;long[] values={0,1,-1,2,-2,2147483647L,-2147483648L,4294967295L,3037000499L,3037000500L,Long.MIN_VALUE,Long.MAX_VALUE};
 for(long a:values)for(long b:values){hash=31*hash+Long.hashCode(Math.multiplyHigh(a,b));hash=31*hash+Long.hashCode(Math.multiplyFull((int)a,(int)b));
 try{hash=31*hash+Long.hashCode(Math.multiplyExact(a,b));}catch(ArithmeticException e){hash+=7;}
 try{hash=31*hash+Long.hashCode(Math.multiplyExact(a,(int)b));}catch(ArithmeticException e){hash+=11;}
 try{hash=31*hash+Math.multiplyExact((int)a,(int)b);}catch(ArithmeticException e){hash+=13;}}
 long a=17,b=-31;for(int i=0;i<2000;i++){a=a*6364136223846793005L+1;b=b*1442695040888963407L+31;hash=31*hash+Long.hashCode(Math.multiplyHigh(a,b));}return hash;
            }}
            """); compare(List.of("run"));
    }

    @Test void floorDivision() throws Exception {
        fixture("""
            public class Metadata { public static int run() {
int hash=1;long[] values={0,1,-1,2,-2,7,-7,2147483647L,-2147483648L,4294967295L,Long.MIN_VALUE,Long.MAX_VALUE};
 for(long a:values)for(long b:values){
 try{hash=31*hash+Long.hashCode(Math.floorDiv(a,b));}catch(ArithmeticException e){hash+=7;}
 try{hash=31*hash+Long.hashCode(Math.floorMod(a,b));}catch(ArithmeticException e){hash+=11;}
 try{hash=31*hash+Long.hashCode(Math.floorDiv(a,(int)b));}catch(ArithmeticException e){hash+=13;}
 try{hash=31*hash+Math.floorMod(a,(int)b);}catch(ArithmeticException e){hash+=17;}
 try{hash=31*hash+Math.floorDiv((int)a,(int)b);}catch(ArithmeticException e){hash+=19;}
 try{hash=31*hash+Math.floorMod((int)a,(int)b);}catch(ArithmeticException e){hash+=23;}}
 return hash;
            }}
            """); compare(List.of("run"));
    }

    @Test void extremaAndSigns() throws Exception {
        fixture("""
            public class Metadata { public static int run() {
int hash=1;double[] values={0.0,-0.0,1,-1,Double.MIN_VALUE,-Double.MIN_VALUE,Double.MAX_VALUE,-Double.MAX_VALUE,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NaN};
 for(double a:values){hash=31*hash+Double.hashCode(Math.abs(a));hash=31*hash+Float.hashCode(Math.abs((float)a));hash=31*hash+Double.hashCode(Math.signum(a));hash=31*hash+Float.hashCode(Math.signum((float)a));
 for(double b:values){hash=31*hash+Double.hashCode(Math.min(a,b));hash=31*hash+Double.hashCode(Math.max(a,b));hash=31*hash+Double.hashCode(Math.copySign(a,b));
 hash=31*hash+Float.hashCode(Math.min((float)a,(float)b));hash=31*hash+Float.hashCode(Math.max((float)a,(float)b));hash=31*hash+Float.hashCode(Math.copySign((float)a,(float)b));}}
 for(long a:new long[]{0,1,-1,Long.MIN_VALUE,Long.MAX_VALUE,Integer.MIN_VALUE,Integer.MAX_VALUE}){hash+=Long.hashCode(Math.abs(a))+Math.abs((int)a);
 try{hash+=Long.hashCode(Math.absExact(a));}catch(ArithmeticException e){hash+=7;}
 try{hash+=Math.absExact((int)a);}catch(ArithmeticException e){hash+=11;}}
 return hash;
            }}
            """); compare(List.of("run"));
    }

    @Test void floatingRounding() throws Exception {
        fixture("""
            public class Metadata { public static int run() {
int hash=1;double[] values={0.0,-0.0,0.5,-0.5,1.5,-1.5,2.5,-2.5,0.49999999999999994,-0.5000000000000001,0x1.0p52,-0x1.0p52,0x1.0p63,-0x1.0p63,Double.MIN_VALUE,-Double.MIN_VALUE,Double.MAX_VALUE,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NaN};
 for(double value:values){hash=31*hash+Double.hashCode(Math.floor(value));hash=31*hash+Double.hashCode(Math.ceil(value));hash=31*hash+Double.hashCode(Math.rint(value));hash=31*hash+Long.hashCode(Math.round(value));hash=31*hash+Math.round((float)value);}
 long bits=123456789;for(int i=0;i<10000;i++){bits=bits*6364136223846793005L+1442695040888963407L;double value=Double.longBitsToDouble(bits);float f=Float.intBitsToFloat((int)(bits>>>32));
 hash=31*hash+Double.hashCode(Math.floor(value));hash=31*hash+Double.hashCode(Math.ceil(value));hash=31*hash+Double.hashCode(Math.rint(value));hash=31*hash+Long.hashCode(Math.round(value));hash=31*hash+Math.round(f);}
 return hash;
            }}
            """); compare(List.of("run"));
    }

    @Test void collectionAlgorithms() throws Exception {
        fixture("""
            public class Metadata { public static int run() {
java.util.List<Integer> list=new java.util.ArrayList<>(java.util.Arrays.asList(3,1,2,1));int hash=1;
 java.util.Collections.sort(list);hash=31*hash+list.hashCode();
 for(int key=-1;key<6;key++)hash=31*hash+java.util.Collections.binarySearch(list,key);
 hash+=java.util.Collections.min(list)+java.util.Collections.max(list);java.util.Collections.reverse(list);hash=31*hash+list.hashCode();
 java.util.Collections.sort(list,java.util.Comparator.reverseOrder());for(int key=-1;key<6;key++)hash=31*hash+java.util.Collections.binarySearch(list,key,java.util.Comparator.reverseOrder());
 hash+=java.util.Collections.min(list,java.util.Comparator.reverseOrder())+java.util.Collections.max(list,java.util.Comparator.reverseOrder());
 java.util.Collections.swap(list,0,3);hash=31*hash+list.hashCode();java.util.Collections.fill(list.subList(1,3),9);hash=31*hash+list.hashCode();
 java.util.Collections.copy(list,java.util.Arrays.asList(7,8));hash=31*hash+list.hashCode();java.util.Collections.copy(list.subList(1,4),list.subList(0,3));hash=31*hash+list.hashCode();
 hash+=java.util.Collections.addAll(list,1,2,1,2)?7:0;hash+=java.util.Collections.frequency(list,1);hash+=java.util.Collections.disjoint(list,java.util.Arrays.asList(99))?11:0;
 hash+=java.util.Collections.indexOfSubList(list,java.util.Arrays.asList(1,2));hash+=java.util.Collections.lastIndexOfSubList(list,java.util.Arrays.asList(1,2));
 hash+=java.util.Collections.indexOfSubList(list,java.util.Collections.emptyList());hash+=java.util.Collections.lastIndexOfSubList(list,java.util.Collections.emptyList());
 hash+=java.util.Collections.replaceAll(list,1,null)?13:0;hash+=java.util.Collections.frequency(list,null);hash+=java.util.Collections.replaceAll(list,null,1)?17:0;
 hash+=java.util.Collections.disjoint(list,java.util.Arrays.asList(1))?19:23;
 java.util.Collections.reverse(java.util.Collections.singletonList(1));
 try{java.util.Collections.min(java.util.Collections.<Integer>emptyList());throw new Error();}catch(java.util.NoSuchElementException e){hash++;}
 try{java.util.Collections.copy(java.util.Collections.emptyList(),list);throw new Error();}catch(IndexOutOfBoundsException e){hash++;}
 try{java.util.Collections.fill(java.util.Collections.singletonList(1),2);throw new Error();}catch(UnsupportedOperationException e){hash++;}
 try{java.util.Collections.swap(list,-1,0);throw new Error();}catch(IndexOutOfBoundsException e){hash++;}
 try{java.util.Collections.binarySearch(java.util.Arrays.asList(1),null);throw new Error();}catch(NullPointerException e){hash++;}
 return hash+list.hashCode();
            }}
            """); compare(List.of("run"));
    }

    @Test void pinnedLibraryListAlgorithms() throws Exception {
        Path artifact=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString())).resolve("net/bytebuddy/byte-buddy/1.14.11/byte-buddy-1.14.11.jar");
        assertEquals("62ae28187ed2b062813da6a9d567bfee733c341582699b62dd980230729a0313",java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(artifact))));
        fixtureClasspath=artifact.toString();
        fixture("""
            import java.util.*;import net.bytebuddy.utility.CompoundList;
            public class Metadata {public static int run(){
                List<Integer> values=CompoundList.of(Arrays.asList(9,3,7),Arrays.asList(2,3,1));
                Collections.sort(values);int hash=values.hashCode();hash+=Collections.binarySearch(values,3)+Collections.min(values)+Collections.max(values);
                Collections.reverse(values);Collections.replaceAll(values,3,11);return 31*hash+values.hashCode();
            }}
            """);
        Path selected=directory.resolve("net/bytebuddy/utility/CompoundList.class");Files.createDirectories(selected.getParent());byte[] original;
        try(JarFile jar=new JarFile(artifact.toFile())){original=jar.getInputStream(jar.getJarEntry("net/bytebuddy/utility/CompoundList.class")).readAllBytes();}
        Files.write(selected,original);compare(List.of("run"));assertArrayEquals(original,Files.readAllBytes(selected));
    }

    @Test void squareRootsMatchIeeeBoundariesAndRawPatterns() throws Exception {
        fixture("""
            import java.util.function.DoubleUnaryOperator;
            public class Metadata {public static int run(){int hash=1;
                DoubleUnaryOperator root=Math::sqrt;
                long[] boundaries={0,Long.MIN_VALUE,1,2,3,-1,0x000fffffffffffffL,0x0010000000000000L,
                    0x3fefffffffffffffL,0x3ff0000000000000L,0x3ff0000000000001L,0x4000000000000000L,
                    0x4010000000000000L,0x7fefffffffffffffL,0x7ff0000000000000L,0xfff0000000000000L,
                    0x7ff0000000000001L,0x7ff8000000000000L,0xbff0000000000000L};
                for(long bits:boundaries){double value=Double.longBitsToDouble(bits);
                    hash=31*hash+Double.hashCode(Math.sqrt(value));hash=31*hash+Double.hashCode(root.applyAsDouble(value));}
                for(int exponent=0;exponent<2047;exponent++)for(long offset:new long[]{-1,0,1}){
                    long bits=((long)exponent<<52)+offset;
                    hash=31*hash+Double.hashCode(Math.sqrt(Double.longBitsToDouble(bits)));}
                long bits=42;for(int i=0;i<10000;i++){bits=bits*6364136223846793005L+1442695040888963407L;
                    hash=31*hash+Double.hashCode(Math.sqrt(Double.longBitsToDouble(bits)));}
                return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void angularConversionsPreserveRoundingAndSpecialValues() throws Exception {
        fixture("""
            import java.util.function.DoubleUnaryOperator;
            public class Metadata {public static int run(){int hash=1;
                DoubleUnaryOperator radians=Math::toRadians,degrees=Math::toDegrees;
                double[] cases={0.0,-0.0,Double.MIN_VALUE,-Double.MIN_VALUE,Double.MIN_NORMAL,
                    Double.MAX_VALUE,-Double.MAX_VALUE,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NaN,
                    1,-1,90,-90,180,360,Math.PI,Math.E};
                for(double value:cases){hash=31*hash+Double.hashCode(radians.applyAsDouble(value));
                    hash=31*hash+Double.hashCode(degrees.applyAsDouble(value));}
                long bits=1234;for(int i=0;i<10000;i++){bits=bits*6364136223846793005L+1442695040888963407L;
                    double value=Double.longBitsToDouble(bits);
                    hash=31*hash+Double.hashCode(Math.toRadians(value));hash=31*hash+Double.hashCode(Math.toDegrees(value));}
                return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void executesOriginalPinnedH2StandardDeviationBodies() throws Exception {
        Path artifact=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString())).resolve("com/h2database/h2/2.2.224/h2-2.2.224.jar");
        assertEquals("b9d8f19358ada82a4f6eb5b174c6cfe320a375b5a9cb5a4fe456d623e6e55497",HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(artifact))));
        String name="org/h2/engine/QueryStatisticsData$QueryEntry";
        ClassNode selected=new ClassNode();
        try(JarFile jar=new JarFile(artifact.toFile())){new ClassReader(jar.getInputStream(jar.getJarEntry(name+".class")).readAllBytes()).accept(selected,0);}
        selected.methods.removeIf(method->!Set.of("<init>","getExecutionTimeStandardDeviation","getRowCountStandardDeviation").contains(method.name));
        assertEquals(3,selected.methods.size());
        // Supply private accumulated statistics without replacing any retained H2 instruction.
        MethodNode state=new MethodNode(ACC_PUBLIC,"testState","(IDD)V",null,null);
        state.visitCode();state.visitVarInsn(ALOAD,0);state.visitVarInsn(ILOAD,1);state.visitFieldInsn(PUTFIELD,name,"count","I");
        state.visitVarInsn(ALOAD,0);state.visitVarInsn(DLOAD,2);state.visitFieldInsn(PUTFIELD,name,"executionTimeM2Nanos","D");
        state.visitVarInsn(ALOAD,0);state.visitVarInsn(DLOAD,4);state.visitFieldInsn(PUTFIELD,name,"rowCountM2","D");
        state.visitInsn(RETURN);state.visitMaxs(3,6);state.visitEnd();selected.methods.add(state);
        ClassWriter writer=new ClassWriter(0);selected.accept(writer);
        Path file=directory.resolve(name+".class");Files.createDirectories(file.getParent());Files.write(file,writer.toByteArray());
        fixtureClasspath=directory+File.pathSeparator+artifact;
        fixture("""
            import org.h2.engine.QueryStatisticsData.QueryEntry;
            public class Metadata {public static int run(){int hash=1;
                QueryEntry entry=new QueryEntry("select 1");
                for(int count:new int[]{-1,0,1,2,3,17,Integer.MAX_VALUE})
                    for(double m2:new double[]{0,-0.0,1,2,9,Double.MIN_VALUE,Double.MAX_VALUE,Double.POSITIVE_INFINITY,Double.NaN,-1}){
                        entry.testState(count,m2,m2*0.5);
                        hash=31*hash+Double.hashCode(entry.getExecutionTimeStandardDeviation());
                        hash=31*hash+Double.hashCode(entry.getRowCountStandardDeviation());
                    }
                return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void malformedSquareRootDescriptorDoesNotBypassLinkage() {
        ClassWriter writer=new ClassWriter(0);writer.visit(V17,ACC_PUBLIC,"BadMath",null,"java/lang/Object",null);
        MethodVisitor method=writer.visitMethod(ACC_PUBLIC|ACC_STATIC,"run","()D",null,null);
        method.visitCode();method.visitInsn(LCONST_0);method.visitMethodInsn(INVOKESTATIC,"java/lang/Math","sqrt","(J)D",false);
        method.visitInsn(DRETURN);method.visitMaxs(2,0);method.visitEnd();writer.visitEnd();
        var failure=assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileClassWithExceptionRuntime(writer.toByteArray()));
        assertTrue(failure.getMessage().contains("sqrt"),failure.getMessage());
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
