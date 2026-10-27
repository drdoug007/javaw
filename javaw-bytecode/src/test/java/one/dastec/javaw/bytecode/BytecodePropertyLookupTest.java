package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.tools.ToolProvider;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.jar.*;

import static org.junit.jupiter.api.Assertions.*;

class BytecodePropertyLookupTest {
    @TempDir Path directory;
    private final String fixtureClassName="Metadata";
    @Test void isolatedProperties() throws Exception {
        fixture("""
            import java.util.function.*;
            public class Metadata {
             public static int run(int group){int h=1;String key="javaw.test.properties.key";
              Function<String,String> read=System::getProperty,clear=System::clearProperty;
              BiFunction<String,String,String> write=System::setProperty,defaults=System::getProperty;
              h=31*h+(read.apply(key)==null?1:0);
              h=31*h+defaults.apply(key,"fallback").hashCode();
              h=31*h+(write.apply(key,"first")==null?1:0);
              h=31*h+write.apply(new String(key),"second").hashCode();
              h=31*h+read.apply(key).hashCode();h=31*h+clear.apply(key).hashCode();
              h=31*h+(System.clearProperty(key)==null?1:0);
              for(int i=0;i<4;i++)System.setProperty(key+i,"v"+i);
              for(int i:new int[]{2,3,0,1})h=31*h+System.clearProperty(key+i).hashCode();
              for(String invalid:new String[]{null,""}){
               try{System.getProperty(invalid);throw new Error();}catch(NullPointerException e){h+=3;}catch(IllegalArgumentException e){h+=5;}
               try{System.getProperty(invalid,null);throw new Error();}catch(NullPointerException e){h+=7;}catch(IllegalArgumentException e){h+=11;}
               try{System.setProperty(invalid,null);throw new Error();}catch(NullPointerException e){h+=13;}catch(IllegalArgumentException e){h+=17;}
               try{System.clearProperty(invalid);throw new Error();}catch(NullPointerException e){h+=19;}catch(IllegalArgumentException e){h+=23;}
              }
              try{System.setProperty(key,null);throw new Error();}catch(NullPointerException e){h++;}
              System.setProperty(key,"");h=31*h+System.getProperty(key,"ignored").length();System.clearProperty(key);
              return h;
             }
             public static int checkDefault(){return System.getProperty("java.version")==null?1:0;}
             public static int checkIsolation(){String previous=System.setProperty("javaw.test.isolation","left");return previous==null?1:0;}
            }
            """);compare(1);
    }

    @Test void booleanProperty() throws Exception {
        fixture("""
            import java.util.function.*;
            public class Metadata {public static int run(int group){int h=1;String key="javaw.test.boolean";
             Predicate<String> read=Boolean::getBoolean;
             for(String value:new String[]{null,"","true","TRUE","TrUe","false"," true","true ","1","yes","TRUE\u0000"}){
              System.clearProperty(key);if(value!=null)System.setProperty(key,value);
              h=31*h+(Boolean.getBoolean(key)?1:0);h=31*h+(read.test(key)?1:0);
             }
             h=31*h+(read.test(null)?1:0);h=31*h+(read.test("")?1:0);System.clearProperty(key);return h;}}
            """);compare(1);
    }

    private byte[] fixture(String source) throws Exception {
        Path file=directory.resolve("Metadata.java");Files.writeString(file,source);
        List<String> options=new ArrayList<>(List.of("-g","-parameters","--release","17","-d",directory.toString()));
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
                jar.putNextEntry(new JarEntry(directory.relativize(file).toString()));jar.write(Files.readAllBytes(file));jar.closeEntry();}
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
            """.replace("EXPECTED",expected)+"if(e['Metadata.checkDefault'])assert.equal(e['Metadata.checkDefault'](),1);\nif(e['Metadata.checkIsolation'])assert.equal(e['Metadata.checkIsolation'](),1);\nassert.equal(e['"+fixtureClassName+".run'](group),expected[group],'batch '+group);\n}");
        run("node",script.toString(),output.toString());
    }
}
