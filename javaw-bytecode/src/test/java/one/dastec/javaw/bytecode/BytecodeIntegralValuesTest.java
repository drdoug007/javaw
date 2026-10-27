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

class BytecodeIntegralValuesTest implements Opcodes {
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

    @Test void matchesRadicesBoundariesUnicodeAndFailures() throws Exception {
        fixture("""
            public class Metadata {
                public static int run(){
                    int hash=1;int[] ints={0,1,-1,2147483647,-2147483648,127,-128,65535,-65536};long[] longs={0,1,-1,9223372036854775807L,-9223372036854775808L,4294967295L,-4294967296L};
                    for(int radix=2;radix<=36;radix++){
                        for(int value:ints){String text=Integer.toString(value,radix);hash=31*hash+text.hashCode();if(Integer.parseInt(text,radix)!=value)throw new Error();}
                        for(long value:longs){String text=Long.toString(value,radix);hash=31*hash+text.hashCode();if(Long.parseLong(text,radix)!=value)throw new Error();}
                    }
                    for(int value:ints)hash=31*hash+(Integer.toHexString(value)+Integer.toOctalString(value)+Integer.toBinaryString(value)).hashCode();
                    for(long value:longs)hash=31*hash+(Long.toHexString(value)+Long.toOctalString(value)+Long.toBinaryString(value)).hashCode();
                    hash+=Integer.parseInt(new StringBuilder("x-2147483648y"),1,12,10);hash+=(int)Long.parseLong("x+7fffffffffffffffy",1,18,16);
                    hash+=Integer.decode("-0x80000000")+Long.decode("-0x8000000000000000").intValue()+Byte.decode("-0200")+Short.decode("#7fff");
                    if(Integer.valueOf("127")==Integer.valueOf(127) && Long.valueOf("-128")==Long.valueOf(-128))hash++;
                    hash+=new Integer("+17").intValue()+new Long("-33").intValue()+new Byte("12").intValue()+new Short("23").intValue();
                    hash+=Byte.parseByte("7f",16)+Short.parseShort("-8000",16);return hash;
                }
                public static int unicode(){int hash=1;for(int i=0;i<65536;i++)hash=31*hash+Character.digit((char)i,36);for(int radix=0;radix<39;radix++)for(int digit=-1;digit<38;digit++)hash=31*hash+Character.forDigit(digit,radix);return hash+Integer.parseInt("１２٣٤",10);}
                public static int failures(){
                    String[] bad={null,"","+","-"," 1","1 ","1_0","0x12","2147483648","-2147483649","😀"};int score=0;
                    for(String text:bad)try{Integer.parseInt(text);throw new Error();}catch(NumberFormatException expected){score++;}
                    String[] badLongs={"9223372036854775808","-9223372036854775809"};for(String text:badLongs)try{Long.parseLong(text);throw new Error();}catch(NumberFormatException expected){score+=10;}
                    for(String text:new String[]{"0x-1","--1","09","0x","-+1"})try{Integer.decode(text);throw new Error();}catch(NumberFormatException expected){score+=100;}
                    try{Byte.parseByte("128");}catch(NumberFormatException expected){score+=1000;}try{Short.parseShort("-32769");}catch(NumberFormatException expected){score+=10000;}
                    try{Integer.parseInt("0",1);}catch(NumberFormatException expected){score+=100000;}
                    try{Long.parseLong((CharSequence)null,0,0,10);}catch(NullPointerException expected){score+=1000000;}
                    try{Integer.parseInt("1",-1,1,10);}catch(IndexOutOfBoundsException expected){score+=10000000;}
                    return score;
                }
                public static String fallback(){return Integer.toString(-21,1)+"/"+Long.toString(-33,37);}
            }
            """);compare(List.of("run","unicode","failures","fallback"));
    }

    @Test void executesUnchangedPinnedLogbackPatternFormatInfo() throws Exception {
        Path repository=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()));Path artifact=repository.resolve("ch/qos/logback/logback-core/1.4.14/logback-core-1.4.14.jar");
        assertEquals("f8c2f05f42530b1852739507c1792f0080167850ed8f396444c6913d6617a293",java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(artifact))));fixtureClasspath=artifact.toString();
        fixture("""
            import ch.qos.logback.core.pattern.FormatInfo;
            public class Metadata {
                public static String run(){StringBuilder result=new StringBuilder();for(String spec:new String[]{"","5","-5.12",".7","3.-9","+4.１２","-2147483648","2147483647.0"}){FormatInfo value=FormatInfo.valueOf(spec);result.append(value.toString()).append(':').append(value.hashCode()).append(':').append(value.equals(FormatInfo.valueOf(spec))).append(';');}return result.toString();}
                public static int failures(){int hash=1;for(String spec:new String[]{null,"5.","abc","2147483648","1.2.3"," 2"})try{FormatInfo.valueOf(spec);throw new Error();}catch(RuntimeException expected){hash=31*hash+expected.getClass().getName().hashCode();}return hash;}
            }
            """);
        Path selected=directory.resolve("ch/qos/logback/core/pattern/FormatInfo.class");Files.createDirectories(selected.getParent());byte[] original;
        try(JarFile jar=new JarFile(artifact.toFile())){original=jar.getInputStream(jar.getJarEntry("ch/qos/logback/core/pattern/FormatInfo.class")).readAllBytes();}
        Files.write(selected,original);compare(List.of("run","failures"));assertArrayEquals(original,Files.readAllBytes(selected));
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
