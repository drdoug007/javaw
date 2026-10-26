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

class BytecodeLowercaseTest implements Opcodes {
    @TempDir Path directory;
    private String release="17";
    private byte[] pinnedCarrier;
    private String fixtureClasspath;
    private String fixtureClassName="Metadata";

    @Test void rootWordBoundaries() throws Exception {
        Path helper=directory.resolve("javaw/runtime/WordBreaks.java");Files.createDirectories(helper.getParent());
        Files.writeString(helper,"package javaw.runtime;import java.text.*;import java.util.*;public class WordBreaks{public static boolean[] boundaries(String s){BreakIterator b=BreakIterator.getWordInstance(Locale.ROOT);b.setText(s);boolean[] result=new boolean[s.length()+1];for(int i=0;i<result.length;i++)result[i]=b.isBoundary(i);return result;}}");
        assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,"--release","17","-d",directory.toString(),helper.toString()));
        fixtureClasspath=directory.toString();
        fixture("""
            import java.util.*;import javaw.runtime.WordBreaks;
            public class Metadata {
             static int check(String s){int h=1;boolean[] b=WordBreaks.boundaries(s);for(int i=0;i<b.length;i++)h=31*h+(b[i]?1:0);return h;}
             public static int run(int group){int h=1;
             for(String s:new String[]{"","ΟΣ","ΣΟΣ","ΟΣ'Α","ΑΣ-Α","ΑΣ1Α","ΑΣ_Α","ΑΣ.Α","ΑΣ:Α","ΑΣ\\u0301Α","ΑΣ\\u200dΑ","a\\uffffΣ","aΣ\\uffffb","aΣ"+(char)0xd800+"b","aΣ"+(char)0xdc00+"b","aΣ𐐀","אΣא","กΣก","中文Σ中文","a\\r\\nΣ"})h=31*h+check(s);
             for(int cp=group*1024;cp<group*1024+1024 && cp<=0x10ffff;cp+=31){String c=new String(Character.toChars(cp));for(String s:new String[]{"aΣ"+c,"aΣ"+c+"b","a"+c+"Σ",c+"Σa"})h=31*h+check(s);}
             return h;}}
            """);compare(1088);
    }

    @Test void unicodeRootLowercase() throws Exception {
        fixture("""
            import java.util.*;
            public class Metadata {public static int run(int group){int h=1;
             for(int cp=group*1024;cp<group*1024+1024 && cp<=0x10ffff;cp++){
              if(cp==0x3a3)continue;String s=new String(Character.toChars(cp)),lower=s.toLowerCase(Locale.ROOT);
              h=31*h+lower.hashCode();h=31*h+(s==lower?1:0);
             }
             try{"".toLowerCase((Locale)null);throw new Error();}catch(NullPointerException e){h++;}
             return h;}}
            """);compare(1088);
    }

    @Test void finalSigmaContexts() throws Exception {
        fixture("""
            import java.util.*;
            public class Metadata {static int hash(String s){String lower=s.toLowerCase(Locale.ROOT);return 31*lower.hashCode()+(s==lower?1:0);}
             public static int run(int group){int h=1;
             for(String s:new String[]{"Σ","ΟΣ","ΣΟΣ","ΟΣΣ","ΣΣ","ΑΣ'Α","ΑΣ-Α","ΑΣ1Α","ΑΣ_Α","ΑΣ.Α","ΑΣ:Α","ΑΣ\\u0301Α","ΑΣ\\u200dΑ","a\\uffffΣ","aΣ\\uffffb","aΣ𐐀","𐐀Σ","אΣא","กΣก","中文Σ中文"})h=31*h+hash(s);
             for(int cp=group*1024;cp<group*1024+1024 && cp<=0x10ffff;cp+=31){String c=new String(Character.toChars(cp));for(String s:new String[]{"aΣ"+c,"aΣ"+c+"b","a"+c+"Σ",c+"Σa",c+"Σ"})h=31*h+hash(s);}
             return h;}}
            """);compare(1088);
    }

    @Test void turkicLowercase() throws Exception {
        fixture("""
            import java.util.*;
            public class Metadata {public static int run(int group){int h=1;
             for(String language:new String[]{"tr","az","TR"}){Locale l=new Locale(language,"TR");
              for(String s:new String[]{"","Iİiı","I\\u0307","I\\u0323\\u0307","I\\u0301\\u0307","I\\u034f\\u0307","I\\u0307\\u0307","I\\u0323I\\u0307","ΟΣI","𐐀I","i"}){String lower=s.toLowerCase(l);h=31*h+lower.hashCode();h=31*h+(s==lower?1:0);}
              for(int cp=group*256;cp<group*256+256;cp+=13){String c=new String(Character.toChars(cp));for(String s:new String[]{"I"+c+"\\u0307", "İ"+c,"I"+c+"\\u0307\\u0307",c+"I"})h=31*h+s.toLowerCase(l).hashCode();}
             }return h;}}
            """);compare(256);
    }

    @Test void lithuanianLowercase() throws Exception {
        fixture("""
            import java.util.*;
            public class Metadata {public static int run(int group){int h=1;Locale l=new Locale("LT","lt");
             for(String s:new String[]{"","IJĮĮÌÍĨİ","I\\u0307","J\\u0301","Į\\u0323\\u0301","I\\u034f\\u0301","ÌÍĨ","I\\u0307\\u0307","ΟΣI","𐐀J","i"}){String lower=s.toLowerCase(l);h=31*h+lower.hashCode();h=31*h+(s==lower?1:0);}
             for(int cp=group*256;cp<group*256+256;cp+=13){String c=new String(Character.toChars(cp));for(String s:new String[]{"I"+c+"\\u0301", "J"+c,"Į"+c+"\\u0307",c+"I","I"+c+"J\\u0301"})h=31*h+s.toLowerCase(l).hashCode();}
             return h;}}
            """);compare(256);
    }

    @Test void defaultLowercase() throws Exception {
        fixture("""
            import java.util.*;import java.util.function.*;
            public class Metadata {public static int run(int group){int h=1;Function<String,String> f=String::toLowerCase;BiFunction<String,Locale,String> withLocale=String::toLowerCase;
             for(String s:new String[]{"","abc","ABC","Iİıi","ΟΣ","ΑΣ'Α","𐐀Σ","𐐀",new String(new char[]{(char)0xd800,120,(char)0xdc00}),"A\\u0301Σ","ⱢĲẞ"}){
              h=31*h+s.toLowerCase().hashCode();h=31*h+f.apply(s).hashCode();h=31*h+(s==s.toLowerCase()?1:0);
              h=31*h+(s.toLowerCase().equals(withLocale.apply(s,Locale.ROOT))?1:0);
             }return h;}}
            """);compare(1);
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
            files=new ArrayList<>(paths.filter(path->path.toString().endsWith(".class") && !path.getFileName().toString().equals("CaseOracle.class") && !path.toString().contains("javaw/runtime/WordBreaks.class")).sorted().toList());
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
