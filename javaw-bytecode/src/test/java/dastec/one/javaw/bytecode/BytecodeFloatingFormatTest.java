package dastec.one.javaw.bytecode;

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

class BytecodeFloatingFormatTest implements Opcodes {
    @TempDir Path directory;
    private String release="17";
    private byte[] pinnedCarrier;
    private String fixtureClasspath;
    private String fixtureClassName="Metadata";

    @Test void finiteFixed() throws Exception {
        fixture("""
            import java.util.*;
            public class Metadata {
             public static int run(int group){
              double[] edges={0,1,-1,0.1,0.005,0.015,2.675,-2.675,9.995,99.9995,1e23,1e-4,1e-5,1e-6,1e-7,Double.MIN_VALUE,-Double.MIN_VALUE,Double.MIN_NORMAL,Double.MAX_VALUE,Math.nextDown(1e23),Math.nextUp(1e23),0x1p60,Math.nextUp(0x1p60),0x1p-1022};
              long bits=12345+group;for(int i=0;i<9;i++)bits=bits*6364136223846793005L+1442695040888963407L;
              double n=group<edges.length?edges[group]:Double.longBitsToDouble(bits);
              if(!Double.isFinite(n) || n==0)n=1.2345678901234567;
              float f=(float)n;if(!Float.isFinite(f) || f==0)f=Float.intBitsToFloat((int)bits&0x7f7fffff);
              int h=1;
              for(Object value:new Object[]{Double.valueOf(n),Float.valueOf(f)})
               for(String format:new String[]{"%f","%.0f","%#.0f","%.2f","%.6f","%.17f","%.25f","%+040.3f","% 40.2f","%(,045.2f","%-40.9f","%,.5f"})h=31*h+String.format(Locale.ROOT,format,value).hashCode();
              return h;
             }
            }
            """);compare(80);
    }

    @Test void finiteScientific() throws Exception {
        fixture("""
            import java.util.*;
            public class Metadata {
             public static int run(int group){
              double[] edges={0,1,-1,0.1,0.005,0.015,2.675,-2.675,9.995,99.9995,1e23,1e-4,1e-5,1e-6,1e-7,Double.MIN_VALUE,-Double.MIN_VALUE,Double.MIN_NORMAL,Double.MAX_VALUE,Math.nextDown(1e23),Math.nextUp(1e23),0x1p60,Math.nextUp(0x1p60),0x1p-1022};
              long bits=12345+group;for(int i=0;i<9;i++)bits=bits*6364136223846793005L+1442695040888963407L;
              double n=group<edges.length?edges[group]:Double.longBitsToDouble(bits);
              if(!Double.isFinite(n) || n==0)n=1.2345678901234567;
              float f=(float)n;if(!Float.isFinite(f) || f==0)f=Float.intBitsToFloat((int)bits&0x7f7fffff);
              int h=1;
              for(Object value:new Object[]{Double.valueOf(n),Float.valueOf(f)})
               for(String format:new String[]{"%e","%E","%.0e","%#.0e","%.2e","%.6E","%.17e","%.25E","%+040.3e","% 40.2E","%(045.2e","%-40.9E"})h=31*h+String.format(Locale.ROOT,format,value).hashCode();
              return h;
             }
            }
            """);compare(100);
    }

    @Test void finiteGeneral() throws Exception {
        fixture("""
            import java.util.*;
            public class Metadata {
             public static int run(int group){
              double[] edges={0,1,-1,0.1,0.005,0.015,2.675,-2.675,9.995,99.9995,1e23,1e-4,1e-5,1e-6,1e-7,Double.MIN_VALUE,-Double.MIN_VALUE,Double.MIN_NORMAL,Double.MAX_VALUE,Math.nextDown(1e23),Math.nextUp(1e23),0x1p60,Math.nextUp(0x1p60),0x1p-1022};
              long bits=12345+group;for(int i=0;i<9;i++)bits=bits*6364136223846793005L+1442695040888963407L;
              double n=group<edges.length?edges[group]:Double.longBitsToDouble(bits);
              if(!Double.isFinite(n) || n==0)n=1.2345678901234567;
              float f=(float)n;if(!Float.isFinite(f) || f==0)f=Float.intBitsToFloat((int)bits&0x7f7fffff);
              int h=1;
              for(Object value:new Object[]{Double.valueOf(n),Float.valueOf(f)})
               for(String format:new String[]{"%g","%G","%.0g","%.1G","%.2g","%.6G","%.17g","%.25G","%+040.3g","% 40.2G","%(,045.2g","%-40.9G"})h=31*h+String.format(Locale.ROOT,format,value).hashCode();
              return h;
             }
            }
            """);compare(100);
    }

    @Test void nonfiniteDecimal() throws Exception {
        fixture("""
            import java.util.*;
            public class Metadata {public static int run(int group){int h=1;
             Locale[] locales={Locale.ROOT,Locale.GERMANY,Locale.FRANCE,new Locale("ar","EG"),new Locale("tr"),new Locale("lt"),Locale.JAPAN};
             for(Object v:new Object[]{Double.NaN,Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY,Float.NaN,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,Double.longBitsToDouble(0xfff8000000000001L),Float.intBitsToFloat(0xffc00001)})
              for(String f:new String[]{"%f","%e","%E","%g","%G","%.0f","%.1e","%.2g","%#.0f","%020f","%+020E","% 020G","%(020f","%-20.0G","%(020.2E","%+,020.1g"})h=31*h+String.format(locales[group],f,v).hashCode();
             return h;}}
            """);compare(7);
    }

    @Test void signedZerosAndRoundedZeros() throws Exception {
        fixture("""
            import java.util.*;
            public class Metadata {public static int run(int group){int h=1;
             Locale[] locales={Locale.ROOT,Locale.GERMANY,Locale.FRANCE,new Locale("ar","EG")};
             for(Object v:new Object[]{0.0,-0.0,0.0f,-0.0f,Double.MIN_VALUE,-Double.MIN_VALUE,Float.MIN_VALUE,-Float.MIN_VALUE,0.0000001,-0.0000001})
              for(String f:new String[]{"%f","%e","%E","%g","%G","%.0f","%.0e","%.0g","%#.0f","%#.0e","%020.2f","%+020.1E","% 020.3G","%(020.0f","%-20.0G","%(020.2E","%+,020.1g"})h=31*h+String.format(locales[group],f,v).hashCode();
             return h;}}
            """);compare(4);
    }

    @Test void italianNumeric() throws Exception {
        fixture("""
            import java.util.*;import java.math.*;
            public class Metadata {public static int run(int group){
             Locale[] locales={Locale.ITALIAN,Locale.ITALY};Locale locale=locales[group%locales.length];int index=group/locales.length;
             double[] values={1234567.895,-1234567.895,0.00009999995,9.99995,-0.0,Double.MIN_VALUE,Double.MAX_VALUE,Double.NaN};
             double value=values[index];float f=(float)value;int h=1;
             for(String fmt:new String[]{"%,d","%+025d","%(,030d","%#030x","%#030X","%#030o"})for(Object v:new Object[]{1234567,-1234567,Long.MIN_VALUE,new BigInteger("-123456789012345678901234567890")})h=31*h+String.format(locale,fmt,v).hashCode();
             for(String fmt:new String[]{"%,020.2f","%(030.3f","%+.4e","%025.2E","%,.6g","% 030.9G"})for(Object v:new Object[]{value,f,new BigDecimal("-123456.78995")})h=31*h+String.format(locale,fmt,v).hashCode();
             h=31*h+String.format(locale,"%.2f",(Object)null).hashCode();return h;
            }}
            """);compare(16);
    }

    private byte[] fixture(String source) throws Exception {
        Path file=directory.resolve("Metadata.java");Files.writeString(file,source);
        List<String> options=new ArrayList<>(List.of("-g","-parameters","--release",release,"-d",directory.toString()));
        if(fixtureClasspath!=null)options.addAll(List.of("-classpath",fixtureClasspath));
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
                if(pinnedCarrier!=null && entry.startsWith("com/fasterxml/") && !entry.endsWith("JacksonInject$Value.class"))continue;
                jar.putNextEntry(new JarEntry(directory.relativize(file).toString()));jar.write(pinnedCarrier!=null && entry.endsWith("JacksonInject$Value.class")?pinnedCarrier:Files.readAllBytes(file));jar.closeEntry();}
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
