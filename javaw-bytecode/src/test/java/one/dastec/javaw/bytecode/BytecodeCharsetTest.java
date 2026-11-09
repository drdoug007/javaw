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
import java.util.concurrent.TimeUnit;
import java.util.jar.*;

import static org.junit.jupiter.api.Assertions.*;

class BytecodeCharsetTest implements Opcodes {
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

    @Test void namedUtf8Constructors() throws Exception {
        fixture("""
            public class Metadata {
                static int result(byte[] bytes,int offset,int count,String name,boolean whole){
                    try{String text=whole?new String(bytes,name):new String(bytes,offset,count,name);return 31*text.hashCode()+text.length();}
                    catch(Exception e){return e.getClass().getName().hashCode();}}
                public static int run(int group) throws Exception {int hash=1;
                    String[] names={"UTF-8","utf8","UnIcOdE-1-1-uTf-8"};String name=names[group%names.length];
                    for(int second=0;second<256;second++){
                        byte[] pair={(byte)group,(byte)second};hash=31*hash+result(pair,0,2,name,true);
                        hash=31*hash+result(new byte[]{65,pair[0],pair[1],90},1,2,name,false);
                    }
                    long seed=group+17;for(int n=0;n<32;n++){byte[] bytes=new byte[n%9];
                        for(int j=0;j<bytes.length;j++){seed=seed*6364136223846793005L+1442695040888963407L;bytes[j]=(byte)(seed>>>32);}
                        hash=31*hash+result(bytes,0,bytes.length,name,true);
                    }
                    if(group==0){for(String n:new String[]{null,"","!","unknown","UTF-8"})for(byte[] b:new byte[][]{null,new byte[0],new byte[]{65}}){
                        hash=31*hash+result(b,-1,-1,n,false);hash=31*hash+result(b,0,0,n,true);}
                        byte[] snapshot={65,66};String text=new String(snapshot,"utf8");snapshot[0]=90;hash=31*hash+text.hashCode();
                        for(int[] range:new int[][]{{-1,1},{0,-1},{1,2},{Integer.MAX_VALUE,1},{1,Integer.MAX_VALUE}})
                            hash=31*hash+result(new byte[2],range[0],range[1],"UTF-8",false);
                    }return hash;}}
            """);compare(256);
    }

    @Test void utf8NamedEncoding() throws Exception {
        fixture("""
            import java.util.Arrays;
            public class Metadata {
                static int encoded(String text,String name){try{byte[] bytes=text.getBytes(name);return 31*Arrays.hashCode(bytes)+bytes.length;}
                    catch(Exception e){return e.getClass().getName().hashCode();}}
                public static int run(int group) throws Exception {int hash=1;String name="UTF-8";
                    for(int c=group*1024;c<(group+1)*1024;c++)hash=31*hash+encoded(new String(new char[]{(char)c}),name);
                    int[] units={0,65,127,128,255,256,0xd800,0xdbff,0xdc00,0xdfff,0xffff};
                    for(int b:units)for(int c:units)hash=31*hash+encoded(new String(new char[]{(char)(0xd800+group),(char)b,(char)c}),name);
                    if(group==0){for(String n:new String[]{null,"","!","unknown","uſ-ASCII","IſO-8859-1",name})hash=31*hash+encoded("é😀",n);
                        String text="Aé😀";byte[] a=text.getBytes(name),b=text.getBytes(name);if(a==b)throw new Error();a[0]=0;if(b[0]!=65)throw new Error();
                        if("".getBytes(name)=="".getBytes(name))throw new Error();}
                    return hash;}}
            """);compare(64);
    }

    @Test void latin1Decoding() throws Exception {
        fixture("""
            public class Metadata {public static int run(int group) throws Exception {int hash=1;String[] names={"ISO-8859-1","cp819","ISO_8859_1","csISOLatin1","IBM819","latin1","ISO8859-1","iso-ir-100","ISO_8859-1:1987","ISO8859_1","819","l1","ISO_8859-1","8859_1","IBM-819"};
                for(String name:names)for(int b=0;b<256;b++){
                    byte[] bytes={(byte)b,(byte)(255-b),65};String text=new String(bytes,name);
                    hash=31*hash+text.hashCode()+text.length();hash=31*hash+new String(bytes,1,1,name).hashCode();
                }return hash;}}
            """);compare(1);
    }

    @Test void latin1Encoding() throws Exception {
        fixture("""
            import java.util.Arrays;
            public class Metadata {
                static int encoded(String text,String name){try{byte[] bytes=text.getBytes(name);return 31*Arrays.hashCode(bytes)+bytes.length;}
                    catch(Exception e){return e.getClass().getName().hashCode();}}
                public static int run(int group) throws Exception {int hash=1;String name="ISO-8859-1";
                    for(int c=group*1024;c<(group+1)*1024;c++)hash=31*hash+encoded(new String(new char[]{(char)c}),name);
                    int[] units={0,65,127,128,255,256,0xd800,0xdbff,0xdc00,0xdfff,0xffff};
                    for(int b:units)for(int c:units)hash=31*hash+encoded(new String(new char[]{(char)(0xd800+group),(char)b,(char)c}),name);
                    if(group==0){for(String n:new String[]{null,"","!","unknown","uſ-ASCII","IſO-8859-1",name})hash=31*hash+encoded("é😀",n);
                        String text="Aé😀";byte[] a=text.getBytes(name),b=text.getBytes(name);if(a==b)throw new Error();a[0]=0;if(b[0]!=65)throw new Error();
                        if("".getBytes(name)=="".getBytes(name))throw new Error();}
                    return hash;}}
            """);compare(64);
    }

    @Test void asciiDecoding() throws Exception {
        fixture("""
            public class Metadata {public static int run(int group) throws Exception {int hash=1;String[] names={"US-ASCII","iso_646.irv:1983","ASCII","iso-ir-6","cp367","ANSI_X3.4-1986","us","646","ISO646-US","ISO_646.irv:1991","csASCII","IBM367","ascii7","ANSI_X3.4-1968"};
                for(String name:names)for(int b=0;b<256;b++){
                    byte[] bytes={(byte)b,(byte)(255-b),65};String text=new String(bytes,name);
                    hash=31*hash+text.hashCode()+text.length();hash=31*hash+new String(bytes,1,1,name).hashCode();
                }return hash;}}
            """);compare(1);
    }

    @Test void asciiEncoding() throws Exception {
        fixture("""
            import java.util.Arrays;
            public class Metadata {
                static int encoded(String text,String name){try{byte[] bytes=text.getBytes(name);return 31*Arrays.hashCode(bytes)+bytes.length;}
                    catch(Exception e){return e.getClass().getName().hashCode();}}
                public static int run(int group) throws Exception {int hash=1;String name="US-ASCII";
                    for(int c=group*1024;c<(group+1)*1024;c++)hash=31*hash+encoded(new String(new char[]{(char)c}),name);
                    int[] units={0,65,127,128,255,256,0xd800,0xdbff,0xdc00,0xdfff,0xffff};
                    for(int b:units)for(int c:units)hash=31*hash+encoded(new String(new char[]{(char)(0xd800+group),(char)b,(char)c}),name);
                    if(group==0){for(String n:new String[]{null,"","!","unknown","uſ-ASCII","IſO-8859-1",name})hash=31*hash+encoded("é😀",n);
                        String text="Aé😀";byte[] a=text.getBytes(name),b=text.getBytes(name);if(a==b)throw new Error();a[0]=0;if(b[0]!=65)throw new Error();
                        if("".getBytes(name)=="".getBytes(name))throw new Error();}
                    return hash;}}
            """);compare(64);
    }

    @Test void utf16BigEndian() throws Exception {
        fixture("""
            import java.util.Arrays;
            public class Metadata {public static int run(int group) throws Exception {int hash=1;String[] names={"UTF-16BE","ISO-10646-UCS-2","UTF_16BE","UnicodeBigUnmarked","X-UTF-16BE"};String name=names[group%names.length];
                for(int c=group*1024;c<(group+1)*1024;c++){
                    byte[] bytes={(byte)(c>>8),(byte)c};String text=new String(bytes,name);hash=31*hash+text.hashCode()+text.length();
                    hash=31*hash+Arrays.hashCode(new String(new char[]{(char)c}).getBytes(name));
                }
                int[] units={0,65,0xd800,0xdbff,0xdc00,0xdfff,0xfeff,0xfffe};
                for(int a:units)for(int b:units){char[] chars={(char)a,(char)b};hash=31*hash+Arrays.hashCode(new String(chars).getBytes(name));
                    byte[] bytes={(byte)(a>>8),(byte)a,(byte)(b>>8),(byte)b,(byte)65};
                    for(int offset=0;offset<3;offset++)for(int length=0;length<=bytes.length-offset;length++){
                        String text=new String(bytes,offset,length,name);hash=31*hash+text.hashCode()+text.length();}}
                for(String alias:names){byte[] bytes={(byte)0xff,(byte)0xfe,65,0,(byte)0xfe,(byte)0xff};
                    hash=31*hash+new String(bytes,alias).hashCode();hash=31*hash+Arrays.hashCode("é😀".getBytes(alias));
                    hash=31*hash+Arrays.hashCode("".getBytes(alias));hash=31*hash+new String(new byte[]{65},alias).hashCode();}
                return hash;}}
            """);compare(64);
    }

    @Test void utf16LittleEndian() throws Exception {
        fixture("""
            import java.util.Arrays;
            public class Metadata {public static int run(int group) throws Exception {int hash=1;String[] names={"UTF-16LE","UnicodeLittleUnmarked","UTF_16LE","X-UTF-16LE"};String name=names[group%names.length];
                for(int c=group*1024;c<(group+1)*1024;c++){
                    byte[] bytes={(byte)(c>>8),(byte)c};String text=new String(bytes,name);hash=31*hash+text.hashCode()+text.length();
                    hash=31*hash+Arrays.hashCode(new String(new char[]{(char)c}).getBytes(name));
                }
                int[] units={0,65,0xd800,0xdbff,0xdc00,0xdfff,0xfeff,0xfffe};
                for(int a:units)for(int b:units){char[] chars={(char)a,(char)b};hash=31*hash+Arrays.hashCode(new String(chars).getBytes(name));
                    byte[] bytes={(byte)(a>>8),(byte)a,(byte)(b>>8),(byte)b,(byte)65};
                    for(int offset=0;offset<3;offset++)for(int length=0;length<=bytes.length-offset;length++){
                        String text=new String(bytes,offset,length,name);hash=31*hash+text.hashCode()+text.length();}}
                for(String alias:names){byte[] bytes={(byte)0xff,(byte)0xfe,65,0,(byte)0xfe,(byte)0xff};
                    hash=31*hash+new String(bytes,alias).hashCode();hash=31*hash+Arrays.hashCode("é😀".getBytes(alias));
                    hash=31*hash+Arrays.hashCode("".getBytes(alias));hash=31*hash+new String(new byte[]{65},alias).hashCode();}
                return hash;}}
            """);compare(64);
    }

    @Test void utf16Bom() throws Exception {
        fixture("""
            import java.util.Arrays;
            public class Metadata {public static int run(int group) throws Exception {int hash=1;String[] names={"UTF-16","UTF_16","unicode","UnicodeBig","utf16"};String name=names[group%names.length];
                for(int c=group*1024;c<(group+1)*1024;c++){
                    byte[] bytes={(byte)(c>>8),(byte)c};String text=new String(bytes,name);hash=31*hash+text.hashCode()+text.length();
                    hash=31*hash+Arrays.hashCode(new String(new char[]{(char)c}).getBytes(name));
                }
                int[] units={0,65,0xd800,0xdbff,0xdc00,0xdfff,0xfeff,0xfffe};
                for(int a:units)for(int b:units){char[] chars={(char)a,(char)b};hash=31*hash+Arrays.hashCode(new String(chars).getBytes(name));
                    byte[] bytes={(byte)(a>>8),(byte)a,(byte)(b>>8),(byte)b,(byte)65};
                    for(int offset=0;offset<3;offset++)for(int length=0;length<=bytes.length-offset;length++){
                        String text=new String(bytes,offset,length,name);hash=31*hash+text.hashCode()+text.length();}}
                for(String alias:names){byte[] bytes={(byte)0xff,(byte)0xfe,65,0,(byte)0xfe,(byte)0xff};
                    hash=31*hash+new String(bytes,alias).hashCode();hash=31*hash+Arrays.hashCode("é😀".getBytes(alias));
                    hash=31*hash+Arrays.hashCode("".getBytes(alias));hash=31*hash+new String(new byte[]{65},alias).hashCode();}
                return hash;}}
            """);compare(64);
    }

    @Test void standardCharsetIdentities() throws Exception {
        assertEquals(java.nio.charset.StandardCharsets.UTF_8,java.nio.charset.Charset.defaultCharset());
        fixture("""
            import java.nio.charset.*;import java.util.*;
            public class Metadata {
                static String mixed(String name){char[] chars=name.toCharArray();for(int i=0;i<chars.length;i++){
                    char c=chars[i];if(c>='A' && c<='Z')chars[i]=(char)(c+32);else if(c>='a' && c<='z')chars[i]=(char)(c-32);}return new String(chars);}
                static int lookup(String name){try{return Charset.forName(name).name().hashCode();}catch(IllegalCharsetNameException e){return 31*e.getCharsetName().hashCode()+e.getClass().getName().hashCode();}
                    catch(UnsupportedCharsetException e){return 31*e.getCharsetName().hashCode()+e.getClass().getName().hashCode();}
                    catch(IllegalArgumentException e){return e.getClass().getName().hashCode();}}
                static int supported(String name){try{return Charset.isSupported(name)?1:0;}catch(IllegalArgumentException e){return e.getClass().getName().hashCode();}}
                public static int run(int group) throws Exception {int hash=1;
                    Charset[] all={StandardCharsets.UTF_8,StandardCharsets.ISO_8859_1,StandardCharsets.US_ASCII,StandardCharsets.UTF_16BE,StandardCharsets.UTF_16LE,StandardCharsets.UTF_16};
                    if(Charset.defaultCharset()!=StandardCharsets.UTF_8)throw new Error();
                    for(Charset cs:all){if(Charset.forName(cs.name())!=cs || Charset.forName(mixed(cs.name()))!=cs)throw new Error();
                        hash=31*hash+cs.name().hashCode()+cs.displayName().hashCode()+cs.toString().hashCode()+cs.hashCode();
                        hash=31*hash+cs.aliases().hashCode()+cs.aliases().size()+(cs.isRegistered()?1:0)+(cs.canEncode()?1:0);
                        if(cs.equals(null) || cs.equals(cs.name()) || !cs.equals(cs))throw new Error();
                        try{hash=31*hash+(cs.contains(null)?1:0);}catch(NullPointerException e){hash++;}
                        for(Charset other:all)hash=31*hash+cs.compareTo(other)+(cs.contains(other)?1:0)+(cs.equals(other)?1:0);
                        for(String alias:cs.aliases()){
                            if(Charset.forName(alias)!=cs || Charset.forName(mixed(alias))!=cs || !Charset.isSupported(alias))throw new Error();
                            hash+=Arrays.hashCode("é😀".getBytes(alias));
                        }
                        try{cs.aliases().add("x");throw new Error();}catch(UnsupportedOperationException e){hash++;}
                        try{cs.aliases().remove("missing");throw new Error();}catch(UnsupportedOperationException e){hash++;}
                        try{cs.aliases().clear();throw new Error();}catch(UnsupportedOperationException e){hash++;}
                        Iterator<String> it=cs.aliases().iterator();it.next();try{it.remove();throw new Error();}catch(UnsupportedOperationException e){hash++;}
                        try{cs.compareTo(null);throw new Error();}catch(NullPointerException e){hash++;}
                    }
                    for(String name:new String[]{null,"","-x","x!","é"," x","x/y","x\u0000","uſ-ASCII","IſO-8859-1","javaw-not-installed","UTF-8","ascii"}){
                        hash=31*hash+lookup(name);hash=31*hash+supported(name);}
                    return hash;}}
            """);compare(1);
    }

    @Test void charsetStringOverloads() throws Exception {
        fixture("""
            import java.nio.charset.*;import java.util.*;
            public class Metadata {
                static int decode(byte[] bytes,int offset,int count,Charset cs,boolean whole){try{
                    String text=whole?new String(bytes,cs):new String(bytes,offset,count,cs);return 31*text.hashCode()+text.length();
                }catch(Exception e){return e.getClass().getName().hashCode();}}
                public static int run(int group) throws Exception {int hash=1;
                    Charset[] all={StandardCharsets.UTF_8,StandardCharsets.ISO_8859_1,StandardCharsets.US_ASCII,StandardCharsets.UTF_16BE,StandardCharsets.UTF_16LE,StandardCharsets.UTF_16};
                    Charset cs=all[group%all.length];java.util.function.Function<byte[],String> constructor=bytes->new String(bytes,cs);
                    java.util.function.Function<String,byte[]> encoder=text->text.getBytes(cs);
                    for(int c=group*1024;c<(group+1)*1024;c++){
                        String text=new String(new char[]{(char)c});byte[] b=encoder.apply(text);
                        if(!Arrays.equals(b,text.getBytes(cs.name())))throw new Error();hash=31*hash+Arrays.hashCode(b);
                        byte[] bytes={(byte)(c>>8),(byte)c};hash=31*hash+constructor.apply(bytes).hashCode();
                    }
                    long seed=group+17;for(int n=0;n<32;n++){byte[] bytes=new byte[n%9];
                        for(int j=0;j<bytes.length;j++){seed=seed*6364136223846793005L+1442695040888963407L;bytes[j]=(byte)(seed>>>32);}
                        for(int offset=0;offset<=bytes.length;offset++)hash=31*hash+decode(bytes,offset,bytes.length-offset,cs,false);
                    }
                    for(int[] range:new int[][]{{-1,1},{0,-1},{2,1},{Integer.MAX_VALUE,1},{1,Integer.MAX_VALUE}})
                        hash=31*hash+decode(new byte[2],range[0],range[1],cs,false);
                    if(group==0){for(Charset nullable:new Charset[]{null,cs})for(byte[] b:new byte[][]{null,new byte[0]}){
                        hash=31*hash+decode(b,-1,-1,nullable,false);hash=31*hash+decode(b,0,0,nullable,true);}
                        try{"".getBytes((Charset)null);throw new Error();}catch(NullPointerException e){hash++;}
                        for(Charset codec:all){String text="Aé😀";byte[] a=text.getBytes(codec),b=text.getBytes(codec);if(a==b)throw new Error();
                            int before=Arrays.hashCode(b);a[0]^=1;if(Arrays.hashCode(b)!=before)throw new Error();
                            byte[] input={65,0,66,0};String snapshot=new String(input,codec);int original=snapshot.hashCode();input[0]=90;if(snapshot.hashCode()!=original)throw new Error();
                            if("".getBytes(codec)=="".getBytes(codec))throw new Error();}
                    }
                    return hash;}}
            """);compare(64);
    }

    @Test void pinnedJandexUtf8Utility() throws Exception {
        Path artifact=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()))
            .resolve("io/smallrye/jandex/3.1.2/jandex-3.1.2.jar");
        assertEquals("dee12fa1787d5523ed1a02d6c63b19e7aef6ac560f7c6d70595db01aa37e041e",
            HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(artifact))));
        try(JarFile jar=new JarFile(artifact.toFile())){
            Path file=directory.resolve("org/jboss/jandex/Utils.class");Files.createDirectories(file.getParent());
            Files.write(file,jar.getInputStream(jar.getJarEntry("org/jboss/jandex/Utils.class")).readAllBytes());
        }
        fixtureClasspath=directory.toString();fixtureClassName="org.jboss.jandex.Metadata";
        fixture("""
            package org.jboss.jandex;
            import java.util.*;
            public class Metadata {public static int run(int group){int hash=1;
                for(String text:new String[]{"","ASCII","é","😀",new String(new char[]{65,0xd800,66}),new String(new char[]{65,0xdc00,66})}){
                    byte[] bytes=Utils.toUTF8(text);hash=31*hash+Arrays.hashCode(bytes);hash=31*hash+Utils.fromUTF8(bytes).hashCode();}
                for(byte[] bytes:new byte[][]{{(byte)0xc0,(byte)0xaf},{(byte)0xed,(byte)0xa0,(byte)0x80},{(byte)0xf0,(byte)0x9f},{(byte)0xf4,(byte)0x90,(byte)0x80,(byte)0x80}})
                    hash=31*hash+Utils.fromUTF8(bytes).hashCode();
                hash=31*hash+Arrays.hashCode(Utils.INIT_METHOD_NAME);
                try{Utils.toUTF8(null);throw new Error();}catch(NullPointerException e){hash++;}
                try{Utils.fromUTF8(null);throw new Error();}catch(NullPointerException e){hash++;}
                return hash;}}
            """);compare(1);
    }

    @Test void malformedCharsetDescriptors() {
        for(int fixture=0;fixture<3;fixture++){
            ClassWriter writer=new ClassWriter(0);writer.visit(V17,ACC_PUBLIC,"BadCharset",null,"java/lang/Object",null);
            MethodVisitor method=writer.visitMethod(ACC_PUBLIC|ACC_STATIC,"run","()Ljava/lang/Object;",null,null);method.visitCode();
            if(fixture==0){method.visitInsn(ACONST_NULL);method.visitMethodInsn(INVOKESTATIC,"java/nio/charset/Charset","forName","(Ljava/lang/Object;)Ljava/nio/charset/Charset;",false);}
            else if(fixture==1)method.visitFieldInsn(GETSTATIC,"java/nio/charset/StandardCharsets","UTF_8","Ljava/lang/String;");
            else {method.visitLdcInsn("A");method.visitInsn(ACONST_NULL);method.visitMethodInsn(INVOKEVIRTUAL,"java/lang/String","getBytes","(Ljava/lang/Object;)[B",false);}
            method.visitInsn(ARETURN);method.visitMaxs(2,0);method.visitEnd();writer.visitEnd();
            assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileClassWithExceptionRuntime(writer.toByteArray()));
        }
    }

    private void compare(int groups) throws Exception {
        List<Integer> hashes=new ArrayList<>();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)){
            var method=loader.loadClass(fixtureClassName.replace('/','.')).getMethod("run",int.class);
            for(int group=0;group<groups;group++)hashes.add((Integer)method.invoke(null,group));
        }
        for(boolean reverse:new boolean[]{false,true})
            execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),hashes.toString());
    }

    private void run(String... arguments) throws Exception {
        Path log=directory.resolve("command.log");Process process=new ProcessBuilder(arguments).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if(!process.waitFor(60,TimeUnit.SECONDS)){process.destroyForcibly();fail("Command timed out");}
        assertEquals(0,process.exitValue(),Files.readString(log));
    }
    private void execute(String wat,String expected) throws Exception {
        Path input=directory.resolve("metadata.wat"),output=directory.resolve("metadata.wasm"),script=directory.resolve("run.cjs");
        Files.writeString(input,wat);run(Wat2Wasm.command(input,output,Wat2Wasm.EXCEPTIONS).toArray(String[]::new));
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
