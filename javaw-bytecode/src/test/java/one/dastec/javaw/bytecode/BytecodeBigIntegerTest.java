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
import java.util.jar.*;

import static org.junit.jupiter.api.Assertions.*;

class BytecodeBigIntegerTest implements Opcodes {
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

    @Test void arithmeticAndExactConversions() throws Exception {
        fixture("""
            import java.math.*;import java.util.*;
            public class Metadata { public static int run() {
int hash=1;String[] texts={"0","1","-1","123456789012345678901234567890","-98765432109876543210","-9223372036854775808"};
 for(String text:texts){BigInteger a=new BigInteger(text);hash=31*hash+a.hashCode();hash+=a.signum()+a.intValue()+Long.hashCode(a.longValue());
 for(String other:texts){BigInteger b=new BigInteger(other);hash=31*hash+a.add(b).hashCode();hash=31*hash+a.subtract(b).hashCode();hash=31*hash+a.multiply(b).hashCode();hash+=a.compareTo(b)+a.min(b).hashCode()+a.max(b).hashCode();
 if(b.signum()!=0){BigInteger[] qr=a.divideAndRemainder(b);hash=31*hash+qr[0].hashCode();hash=31*hash+qr[1].hashCode();if(!qr[0].multiply(b).add(qr[1]).equals(a))throw new Error();hash+=a.divide(b).hashCode()+a.remainder(b).hashCode();}hash+=a.gcd(b).hashCode();}
 hash=31*hash+a.pow(3).toString().hashCode();hash+=a.abs().hashCode()+a.negate().hashCode();
 try{hash+=a.intValueExact();}catch(ArithmeticException e){hash+=7;}try{hash+=Long.hashCode(a.longValueExact());}catch(ArithmeticException e){hash+=11;}
 try{hash+=a.byteValueExact();}catch(ArithmeticException e){hash+=13;}try{hash+=a.shortValueExact();}catch(ArithmeticException e){hash+=17;}}
 try{BigInteger.ONE.divide(BigInteger.ZERO);throw new Error();}catch(ArithmeticException e){hash++;}
 try{BigInteger.TEN.pow(-1);throw new Error();}catch(ArithmeticException e){hash++;}
 try{BigInteger.ONE.add(null);throw new Error();}catch(NullPointerException e){hash++;}
 return hash;
            }}
            """); compare(List.of("run"));
    }

    @Test void radixAndBinaryConversions() throws Exception {
        fixture("""
            import java.math.*;import java.util.*;
            public class Metadata { public static int run() {
int hash=1;String[] texts={"0","1","-1","128","-128","255","-256","2147483648","-9223372036854775809","123456789012345678901234567890123456789"};
 for(String text:texts){BigInteger a=new BigInteger(text);for(int radix=2;radix<=36;radix++){String formatted=a.toString(radix);hash=31*hash+formatted.hashCode();if(!new BigInteger(formatted,radix).equals(a))throw new Error();}
 byte[] bytes=a.toByteArray();hash=31*hash+Arrays.hashCode(bytes);if(!new BigInteger(bytes).equals(a))throw new Error();bytes[0]^=0x7f;if(!a.toString().equals(text))throw new Error();
 for(int shift=-17;shift<40;shift+=7){hash=31*hash+a.shiftLeft(shift).hashCode();hash=31*hash+a.shiftRight(shift).hashCode();}
 hash+=a.bitLength();for(int bit=0;bit<150;bit++)hash=31*hash+(a.testBit(bit)?1:0);}
 for(int sign=-1;sign<=1;sign++)for(byte[] bytes:new byte[][]{{},{0},{-1,0,127}})try{hash=31*hash+new BigInteger(sign,bytes).hashCode();}catch(NumberFormatException e){hash+=7;}
 hash+=new BigInteger("１２٣٤").intValue();
 for(String text:new String[]{"","+","-","1_0"," 1","0x12"})try{new BigInteger(text);throw new Error();}catch(NumberFormatException e){hash++;}
 try{new BigInteger(new byte[0]);throw new Error();}catch(NumberFormatException e){hash++;}
 try{new BigInteger((byte[])null);throw new Error();}catch(NullPointerException e){hash++;}
 try{new BigInteger(2,new byte[]{0});throw new Error();}catch(NumberFormatException e){hash++;}
 try{new BigInteger("1",1);throw new Error();}catch(NumberFormatException e){hash++;}
 try{BigInteger.ONE.testBit(-1);throw new Error();}catch(ArithmeticException e){hash++;}
 return hash;
            }}
            """); compare(List.of("run"));
    }

    @Test void floatingRoundingAndWidthLimits() throws Exception {
        fixture("""
            import java.math.*;import java.util.*;
            public class Metadata { public static int run() {
int hash=1;
 for(int bits:new int[]{0,1,23,24,25,52,53,54,127,128,1023,1024}){
 BigInteger power=BigInteger.ONE.shiftLeft(bits);for(int offset=-2;offset<=2;offset++){
 BigInteger value=power.add(BigInteger.valueOf(offset));hash=31*hash+Double.hashCode(value.doubleValue());hash=31*hash+Float.hashCode(value.floatValue());
 hash=31*hash+Double.hashCode(value.negate().doubleValue());hash=31*hash+Float.hashCode(value.negate().floatValue());}}
 for(long value:new long[]{Long.MIN_VALUE,Long.MAX_VALUE,0,1,-1}){BigInteger a=BigInteger.valueOf(value);hash=31*hash+a.toString().hashCode();hash+=a.byteValue()+a.shortValue();
 hash+=a.shiftLeft(Integer.MIN_VALUE).hashCode()+a.shiftRight(Integer.MAX_VALUE).hashCode();}
 return hash;
            }}
            """); compare(List.of("run"));
    }

    @Test void deterministicDivisionAndSignedRemainders() throws Exception {
        fixture("""
            import java.math.*;
            public class Metadata { public static int run(){int hash=1;long state=179;
                for(int i=0;i<40;i++){
                    state=state*6364136223846793005L+1442695040888963407L;BigInteger a=BigInteger.valueOf(state).shiftLeft(i%37).add(BigInteger.valueOf(i));
                    state=state*6364136223846793005L+1442695040888963407L;BigInteger b=BigInteger.valueOf(state|1L).shiftLeft(i%11);
                    BigInteger[] qr=a.divideAndRemainder(b);hash=31*hash+qr[0].hashCode();hash=31*hash+qr[1].hashCode();
                    if(!qr[0].multiply(b).add(qr[1]).equals(a))throw new Error();
                    if(qr[1].abs().compareTo(b.abs())>=0 || (qr[1].signum()!=0 && qr[1].signum()!=a.signum()))throw new Error();
                    hash=31*hash+a.mod(b.abs()).hashCode();
                }
                return hash;
            }}
            """);compare(List.of("run"));
    }

    @Test void exactNarrowingBoundsAndUnsupportedApis() throws Exception {
        fixture("""
            import java.math.*;
            public class Metadata { public static int run(){int hash=1;
                for(long value:new long[]{-32769,-32768,32767,32768,-129,-128,127,128,-2147483649L,-2147483648L,2147483647L,2147483648L,Long.MIN_VALUE,Long.MAX_VALUE}){
                    BigInteger number=BigInteger.valueOf(value);hash=31*hash+Long.hashCode(number.longValueExact());
                    try{hash+=number.byteValueExact();}catch(ArithmeticException e){hash+=7;}
                    try{hash+=number.shortValueExact();}catch(ArithmeticException e){hash+=11;}
                    try{hash+=number.intValueExact();}catch(ArithmeticException e){hash+=13;}
                }
                for(BigInteger modulus:new BigInteger[]{BigInteger.ZERO,BigInteger.ONE.negate()})try{BigInteger.TEN.mod(modulus);throw new Error();}catch(ArithmeticException e){hash++;}
                return hash;
            }}
            """);compare(List.of("run"));
        fixture("public class Metadata {public static java.math.BigInteger run(){return java.math.BigInteger.ONE.modInverse(java.math.BigInteger.TEN);}}");
        var failure=assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar())));
        assertTrue(failure.getMessage().contains("java/math/BigInteger.modInverse"),failure.getMessage());
    }

    @Test void executesSelectedPinnedJacksonStaticInitializer() throws Exception {
        Path artifact=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString())).resolve("com/fasterxml/jackson/core/jackson-core/2.15.4/jackson-core-2.15.4.jar");
        assertEquals("8dc9210dd285db366f45f518dd1e6a9ccfeb0f1a8e184a899fe96d29edf1fd94",java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(artifact))));
        fixtureClasspath=artifact.toString();fixtureClassName="com.fasterxml.jackson.core.io.doubleparser.Metadata";
        fixture("""
            package com.fasterxml.jackson.core.io.doubleparser;
            import java.math.*;import java.lang.reflect.*;
            public class Metadata { public static int run() throws Exception {
                Class<?> type=FastIntegerMath.class;int hash=1;
                for(String name:new String[]{"FIVE","TEN_POW_16","FIVE_POW_16"}){Field f=type.getDeclaredField(name);f.setAccessible(true);BigInteger value=(BigInteger)f.get(null);hash=31*hash+value.toString().hashCode()+value.hashCode();}
                Field f=type.getDeclaredField("SMALL_POWERS_OF_TEN");f.setAccessible(true);
                for(BigInteger value:(BigInteger[])f.get(null))hash=31*hash+value.multiply(value).toString().hashCode();return hash;
            }}
            """);
        String name="com/fasterxml/jackson/core/io/doubleparser/FastIntegerMath";byte[] original;
        try(JarFile jar=new JarFile(artifact.toFile())){original=jar.getInputStream(jar.getJarEntry(name+".class")).readAllBytes();}
        ClassNode selected=new ClassNode();new ClassReader(original).accept(selected,0);
        // Keep the actual initializer and fields; other methods require the separate tree-map/FFT workstreams.
        selected.methods.removeIf(method->!method.name.equals("<clinit>"));
        ClassWriter writer=new ClassWriter(0);selected.accept(writer);byte[] bytes=writer.toByteArray();
        Path file=directory.resolve(name+".class");Files.createDirectories(file.getParent());Files.write(file,bytes);
        compare(List.of("run"));assertArrayEquals(bytes,Files.readAllBytes(file));
    }

    private void compare(List<String> names) throws Exception {
        StringBuilder assertions=new StringBuilder();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)){
            Class<?> type=loader.loadClass(fixtureClassName);for(String name:names){Object expected=type.getMethod(name).invoke(null);assertions.append("assert.equal(").append(expected instanceof String?"text(e['"+fixtureClassName+".":"e['"+fixtureClassName+".").append(name).append(expected instanceof String?"']()),":"'](),").append(expected instanceof String?js((String)expected):expected).append(");\n");}
        }
        execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar())),assertions.toString());
    }

    private String js(String value) {
        if(value==null)return "null";
        StringBuilder out=new StringBuilder("\"");
        for(char c:value.toCharArray())out.append(String.format("\\u%04x",(int)c));
        return out.append('"').toString();
    }
    private void execute(String wat,String assertions) throws Exception {
        WasmTestSupport.execute(directory,wat,assertions);
    }
}
