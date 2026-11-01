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

class BytecodeLowercaseTest implements Opcodes {
    @TempDir Path directory;
    private String release="17";
    private byte[] pinnedCarrier;
    private final Map<String,byte[]> carriers=new HashMap<>();
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

    private byte[] original(Path artifact,String hash,String name) throws Exception {
        assertEquals(hash,HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(artifact))));
        byte[] bytes;try(JarFile jar=new JarFile(artifact.toFile())){bytes=jar.getInputStream(jar.getJarEntry(name+".class")).readAllBytes();}
        Path file=directory.resolve(name+".class");Files.createDirectories(file.getParent());Files.write(file,bytes);
        fixtureClasspath=artifact.toString();return bytes;
    }

    @Test void pinnedSpringBooleanConverter() throws Exception {
        Path artifact=Path.of(System.getProperty("maven.repo.local",System.getProperty("user.home")+"/.m2/repository"))
            .resolve("org/springframework/spring-core/6.1.5/spring-core-6.1.5.jar");
        String name="org/springframework/core/convert/support/StringToBooleanConverter";ClassNode carrier=new ClassNode();
        new ClassReader(original(artifact,"476dac28509b3f47d0b5e895a639fa75ac2218f074ce214e44832a836c469f7e",name)).accept(carrier,0);
        carrier.interfaces.clear();carrier.signature=null;carrier.visibleAnnotations=null;carrier.invisibleAnnotations=null;
        for(MethodNode m:carrier.methods){m.visibleAnnotations=null;m.invisibleAnnotations=null;m.visibleTypeAnnotations=null;m.invisibleTypeAnnotations=null;}
        ClassWriter writer=new ClassWriter(0);carrier.accept(writer);carriers.put(name+".class",writer.toByteArray());
        fixtureClassName="org.springframework.core.convert.support.Metadata";
        fixture("""
            package org.springframework.core.convert.support;
            public class Metadata {public static int run(int group){int h=1;StringToBooleanConverter c=new StringToBooleanConverter();
             for(String s:new String[]{""," ","true","TRUE","TrUe","on","YES","1","FALSE","Off","NO","0"," true ","TrUe\\t","unknown","İ","Σ","𐐀"}){
              try{h=31*h+String.valueOf(c.convert(s)).hashCode();}catch(IllegalArgumentException e){h=31*h+e.getMessage().hashCode();}
             }
             try{c.convert(null);throw new Error();}catch(NullPointerException e){h++;}return h;}}
            """);compare(1);
    }

    @Test void pinnedJacksonLowerCaseStrategy() throws Exception {
        Path artifact=Path.of(System.getProperty("maven.repo.local",System.getProperty("user.home")+"/.m2/repository"))
            .resolve("com/fasterxml/jackson/core/jackson-databind/2.15.4/jackson-databind-2.15.4.jar");
        String name="com/fasterxml/jackson/databind/PropertyNamingStrategies$LowerCaseStrategy";ClassNode original=new ClassNode();
        new ClassReader(original(artifact,"0f99ff7719eef3ee85608b3dee6ecf3c698003fe9d6e34eabbfabccdc9b5ac2c",name)).accept(original,0);
        ClassWriter carrier=new ClassWriter(0);carrier.visit(V17,ACC_PUBLIC,name,null,"java/lang/Object",null);
        for(FieldNode f:original.fields)f.accept(carrier);
        for(MethodNode m:original.methods)if(m.name.equals("translate") || m.name.equals("<clinit>"))m.accept(carrier);
        MethodVisitor init=carrier.visitMethod(ACC_PUBLIC,"<init>","()V",null,null);init.visitCode();init.visitVarInsn(ALOAD,0);
        init.visitMethodInsn(INVOKESPECIAL,"java/lang/Object","<init>","()V",false);init.visitInsn(RETURN);init.visitMaxs(1,1);init.visitEnd();carrier.visitEnd();
        carriers.put(name+".class",carrier.toByteArray());
        fixture("""
            import com.fasterxml.jackson.databind.PropertyNamingStrategies.LowerCaseStrategy;
            public class Metadata {public static int run(int group){int h=1;LowerCaseStrategy c=LowerCaseStrategy.INSTANCE;
             for(String s:new String[]{null,"","abc","ABC","Iİıi","ΟΣ","ΑΣ'Α","𐐀Σ","𐐀",new String(new char[]{(char)0xd800,120,(char)0xdc00}),"ÁΣ"}){
              String result=c.translate(s);h=31*h+(result==null?0:result.hashCode());h=31*h+(s==result?1:0);
             }return h;}}
            """);compare(1);
    }

    @Test void excludedSigmaDictionaryAndMalformedDescriptors() throws Exception {
        fixture("""
            import java.util.Locale;
            public class Metadata {public static int run(int group){
             try{"aΣ".toLowerCase(new Locale("th"));throw new Error();}catch(UnsupportedOperationException e){return 7;}
            }}
            """);
        for(boolean reverse:new boolean[]{false,true})execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),"[7]");
        for(boolean locale:new boolean[]{false,true}){
            ClassWriter writer=new ClassWriter(0);writer.visit(V17,ACC_PUBLIC,"BadLowercase",null,"java/lang/Object",null);
            MethodVisitor m=writer.visitMethod(ACC_PUBLIC|ACC_STATIC,"run","()Ljava/lang/Object;",null,null);m.visitCode();m.visitLdcInsn("x");if(locale)m.visitInsn(ACONST_NULL);
            m.visitMethodInsn(INVOKEVIRTUAL,"java/lang/String","toLowerCase",locale?"(Ljava/util/Locale;)Ljava/lang/Object;":"()Ljava/lang/Object;",false);
            m.visitInsn(ARETURN);m.visitMaxs(2,0);m.visitEnd();writer.visitEnd();
            var failure=assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileClassWithExceptionRuntime(writer.toByteArray()));
            assertTrue(failure.getMessage().contains("toLowerCase"),failure.getMessage());
        }
    }

    @Test void stringTransform() throws Exception {
        fixture("""
            import java.util.*;import java.util.function.*;
            public class Metadata {public static int run(int group){int h=1;int[] calls={0};
             for(String s:new String[]{"","abc","AΣ","𐐀",new String(new char[]{(char)0xd800,120})}){
              h=31*h+s.transform(String::length);
              String same=s.transform(Function.identity());h=31*h+(same==s?1:0);
              Object captured=s.transform(v->{calls[0]++;if(v!=s)throw new Error();return v;});h=31*h+(captured==s?1:0);
              h=31*h+s.transform(v->v.toLowerCase(Locale.ROOT)).hashCode();
              int[] a=s.transform(v->new int[]{v.length(),v.hashCode()});h=31*h+a[0]+a[1];
              h=31*h+(s.transform(v->null)==null?1:0);
             }
             IllegalStateException sentinel=new IllegalStateException("sentinel");
             try{"x".transform(v->{throw sentinel;});throw new Error();}catch(IllegalStateException e){h=31*h+(e==sentinel?1:0);}
             try{"".transform(null);throw new Error();}catch(NullPointerException e){h++;}
             return 31*h+calls[0];}}
            """);compare(1);
    }

    @Test void stripIndent() throws Exception {
        fixture("""
            import java.util.*;import java.util.function.*;
            public class Metadata {public static int run(int group){int h=1;Function<String,String> f=String::stripIndent;
             for(String ending:new String[]{"","\\n","\\r","\\r\\n"})for(String s:new String[]{""," ","\\t","  one  ","  one\\n    two\\n "," \\n   \\n  ","\\tA\\r\\n \\tB\\r    C","\\n","\\n\\n","a\\r\\rb","  😀\\n   𐐀  ","  a\\u2028  \\n  b\\u2029","\\u2000a\\u2003\\n\\u2000b\\u2002","\\u00a0a\\u00a0\\n  b"," a\\u0085\\n b"}){
              String text=s+ending;String result=text.stripIndent();h=31*h+result.hashCode();h=31*h+f.apply(text).hashCode();h=31*h+result.stripIndent().hashCode();
             }
             for(int cp=group*256;cp<group*256+256;cp+=13){String c=new String(Character.toChars(cp));h=31*h+(c+"A "+c+"\\r\\n  "+c+"B ").stripIndent().hashCode();}
             return h;}}
            """);compare(256);
    }

    @Test void legacyLowByteCopy() throws Exception {
        fixture("""
            import java.util.*;
            public class Metadata {public static int run(int group){char[] chars=new char[256];for(int i=0;i<chars.length;i++)chars[i]=(char)(group*256+i);String s=new String(chars);byte[] target=new byte[300];Arrays.fill(target,(byte)0x55);s.getBytes(0,s.length(),target,3);int h=Arrays.hashCode(target);s.getBytes(17,s.length()-13,target,7);h=31*h+Arrays.hashCode(target);
             for(String text:new String[]{"","ASCII","ĀÿéΩ","😀𐐀",new String(new char[]{(char)0xd800,120,(char)0xdc00})}){byte[] b=new byte[text.length()+4];Arrays.fill(b,(byte)-7);text.getBytes(0,text.length(),b,2);h=31*h+Arrays.hashCode(b);}
             return h;}}
            """);compare(256);
    }

    @Test void legacyCopyFailurePrecedence() throws Exception {
        fixture("""
            import java.util.*;
            public class Metadata {static int call(String s,int start,int end,byte[] target,int offset){int h=1;try{s.getBytes(start,end,target,offset);h=7;}catch(StringIndexOutOfBoundsException e){h=11;}catch(NullPointerException e){h=13;}return 31*h+Arrays.hashCode(target);}
             public static int run(int group){int h=1;int[] bounds={Integer.MIN_VALUE,-1,0,1,3,4,Integer.MAX_VALUE};
             for(int offset:new int[]{Integer.MIN_VALUE,-1,0,1,3,4,Integer.MAX_VALUE})for(byte[] b:new byte[][]{null,new byte[0],new byte[]{77,88,99}})h=31*h+call("abc",bounds[group/7],bounds[group%7],b,offset);
             h=31*h+call(null,0,0,new byte[]{77},0);return h;}}
            """);compare(49);
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
                jar.putNextEntry(new JarEntry(directory.relativize(file).toString()));jar.write(pinnedCarrier!=null && entry.endsWith("JacksonInject$Value.class")?pinnedCarrier:carriers.getOrDefault(entry,Files.readAllBytes(file)));jar.closeEntry();}
        }
        return bytes.toByteArray();
    }

    private void compare(int groups) throws Exception {
        Path java=Path.of(System.getProperty("javaw.jdk17.home",System.getProperty("user.home")+"/Library/Java/JavaVirtualMachines/graalvm-ce-17.0.9/Contents/Home"),"bin/java");
        assertTrue(Files.isExecutable(java),"A JDK 17 oracle is required (javaw.jdk17.home)");
        run(java.toString(),"-Djava.locale.providers=CLDR","-Duser.language=","-Duser.country=","-Duser.variant=","-cp",directory.toString()+(fixtureClasspath==null?"":File.pathSeparator+fixtureClasspath),"CaseOracle",fixtureClassName,Integer.toString(groups));
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
