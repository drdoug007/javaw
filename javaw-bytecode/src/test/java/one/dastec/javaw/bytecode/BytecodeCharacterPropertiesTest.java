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

class BytecodeCharacterPropertiesTest implements Opcodes {
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

    @Test void exhaustiveJava17Properties() throws Exception {
        fixture("""
            public class Metadata {
                static int flags(int c){int f=Character.getType(c);
                    if(Character.isLowerCase(c))f|=32;if(Character.isUpperCase(c))f|=64;
                    if(Character.isAlphabetic(c))f|=128;if(Character.isJavaIdentifierStart(c))f|=256;
                    if(Character.isJavaIdentifierPart(c))f|=512;if(Character.isUnicodeIdentifierStart(c))f|=1024;
                    if(Character.isUnicodeIdentifierPart(c))f|=2048;return f;}
                public static int run(){int hash=1;for(int c=-1;c<=0x110000;c++){hash=31*hash+flags(c);hash=31*hash+Character.getNumericValue(c);}return hash;}
            }
            """);
        for(boolean reverse:new boolean[]{false,true})execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),"assert.equal(e['Metadata.run'](),-1110705514);");
    }
    @Test void bmpOverloadsDigitsAndInvalidCodePoints() throws Exception {
        fixture("""
            public class Metadata {public static int run(){int hash=1;
                for(int c=0;c<=65535;c++){char v=(char)c;
                    int f=Character.getType(v);if(Character.isDefined(v))f|=32;if(Character.isDigit(v))f|=64;
                    if(Character.isLetter(v))f|=128;if(Character.isLetterOrDigit(v))f|=256;
                    if(Character.isLowerCase(v))f|=512;if(Character.isUpperCase(v))f|=1024;
                    if(Character.isJavaIdentifierStart(v))f|=2048;if(Character.isJavaIdentifierPart(v))f|=4096;
                    if(Character.isSpaceChar(v))f|=8192;if(Character.isISOControl(v))f|=16384;
                    hash=31*hash+f;hash=31*hash+Character.getNumericValue(v);
                    hash=31*hash+Character.digit(v,36);
                }
                int[] points={-1,Integer.MIN_VALUE,Integer.MAX_VALUE,0x110000,0x104a5,0x1d7d8,0x216c,0x00bd,0x1f600};
                for(int c:points){for(int r=0;r<=38;r++)hash=31*hash+Character.digit(c,r);
                    hash=31*hash+(Character.isDefined(c)?1:0)+(Character.isDigit(c)?2:0)+(Character.isLetter(c)?4:0)+(Character.isLetterOrDigit(c)?8:0)+(Character.isSpaceChar(c)?16:0)+(Character.isISOControl(c)?32:0);}
                return hash;
            }}
            """);
        for(boolean reverse:new boolean[]{false,true})execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),"assert.equal(e['Metadata.run'](),1007540470);");
    }
    @Test void surrogateConstructionAndFailureWrites() throws Exception {
        fixture("""
            import java.util.*;
            public class Metadata {public static int run(){int hash=1;
                for(int c:new int[]{Integer.MIN_VALUE,-1,0,0x41,0xd800,0xdbff,0xdc00,0xffff,0x10000,0x10400,0x10ffff,0x110000,Integer.MAX_VALUE}){
                    hash=31*hash+Character.charCount(c);hash=31*hash+(Character.isBmpCodePoint(c)?1:0)+(Character.isValidCodePoint(c)?2:0)+(Character.isSupplementaryCodePoint(c)?4:0);
                    hash=31*hash+Character.highSurrogate(c)+Character.lowSurrogate(c);
                    try{hash=31*hash+Arrays.hashCode(Character.toChars(c));}catch(IllegalArgumentException e){hash++;}
                    for(int i=-2;i<=4;i++){char[] chars={'x','y','z'};
                        try{hash=31*hash+Character.toChars(c,chars,i);}catch(IllegalArgumentException e){hash+=3;}catch(IndexOutOfBoundsException e){hash+=7;}
                        hash=31*hash+Arrays.hashCode(chars);
                    }
                }
                for(int c=0;c<=65535;c++){char v=(char)c;hash=31*hash+(Character.isHighSurrogate(v)?1:0)+(Character.isLowSurrogate(v)?2:0)+(Character.isSurrogate(v)?4:0);
                    hash=31*hash+Character.compare(v,'x');hash=31*hash+Character.toCodePoint(v,'x');hash=31*hash+(Character.isSurrogatePair(v,(char)0xdc00)?1:0);}
                try{Character.toChars(0x110000,null,0);throw new Error();}catch(IllegalArgumentException expected){hash++;}
                try{Character.toChars(0x10000,null,0);throw new Error();}catch(NullPointerException expected){hash++;}return hash;
            }}
            """);compare(List.of("run"));
    }
    @Test void arrayAndSequenceTraversalBoundaries() throws Exception {
        fixture("""
            public class Metadata {public static int probe(int kind,int index,int end){
                char[] text={'a',0xd801,0xdc28,0xd800,'x',0xdc00,0xd800,0xd800,0xdc00};String s=new String(text);
                try{
                    switch(kind){
                        case 0:return Character.codePointAt(text,index,end);
                        case 1:return Character.codePointBefore(text,index,end);
                        case 2:return Character.codePointCount(text,index,end);
                        case 3:return Character.codePointCount(s,index,end);
                        case 4:return Character.codePointAt(text,index);
                        case 5:return Character.codePointBefore(text,index);
                        case 6:return Character.codePointAt(s,index);
                        case 7:return Character.codePointBefore(s,index);
                        case 8:return Character.codePointAt((char[])null,index,end);
                        case 9:return Character.codePointBefore((char[])null,index,end);
                        case 10:return Character.codePointCount((CharSequence)null,index,end);
                        case 11:return Character.codePointAt((char[])null,index);
                        case 12:return Character.codePointBefore((char[])null,index);
                        default:throw new Error();
                    }
                }catch(NullPointerException e){return -1;}
                 catch(ArrayIndexOutOfBoundsException e){return -2;}
                 catch(StringIndexOutOfBoundsException e){return -3;}
                 catch(IndexOutOfBoundsException e){return -4;}
            }}
            """);
        StringBuilder checks=new StringBuilder();
        // --release controls class-file APIs, not the host JVM's newer bounds checks.
        try(InputStream oracle=getClass().getResourceAsStream("/character-traversal-jdk17.txt")){
            assertNotNull(oracle);String data=new String(oracle.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
            List<String> rows=data.lines().toList();assertEquals(1872,rows.size());
            for(String row:rows){String[] fields=row.split(" ");String arguments=fields[0]+","+fields[1]+","+fields[2];
                checks.append("assert.equal(e['Metadata.probe'](").append(arguments).append("),").append(fields[3]).append(",'").append(arguments).append("');\n");
            }
        }
        for(boolean reverse:new boolean[]{false,true})execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar(reverse))),checks.toString());
    }
    @Test void executesUnchangedPinnedJacksonCharTypes() throws Exception {
        Path repository=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()));
        Path artifact=repository.resolve("com/fasterxml/jackson/core/jackson-core/2.15.4/jackson-core-2.15.4.jar");
        assertDigest(artifact,"8dc9210dd285db366f45f518dd1e6a9ccfeb0f1a8e184a899fe96d29edf1fd94");
        try(JarFile jar=new JarFile(artifact.toFile())){for(String name:List.of("com/fasterxml/jackson/core/io/CharTypes","com/fasterxml/jackson/core/io/CharTypes$AltEscapes"))writeClass(name,jar.getInputStream(jar.getJarEntry(name+".class")).readAllBytes());}
        fixtureClasspath=directory.toString();
        fixture("""
            import com.fasterxml.jackson.core.io.CharTypes;import java.util.*;
            public class Metadata {public static int run(){int hash=1;
                for(int[] codes:new int[][]{CharTypes.getInputCodeLatin1(),CharTypes.getInputCodeUtf8(),CharTypes.getInputCodeLatin1JsNames(),CharTypes.getInputCodeUtf8JsNames(),CharTypes.getInputCodeComment(),CharTypes.getInputCodeWS(),CharTypes.get7BitOutputEscapes(),CharTypes.get7BitOutputEscapes(39)})hash=31*hash+Arrays.hashCode(codes);
                for(int c=-2;c<300;c++)hash=31*hash+CharTypes.charToHex(c);
                hash=31*hash+Arrays.hashCode(CharTypes.copyHexChars());hash=31*hash+Arrays.hashCode(CharTypes.copyHexBytes());
                char[] snapshot=CharTypes.copyHexChars(true);snapshot[0]='x';hash=31*hash+Arrays.hashCode(CharTypes.copyHexChars(true));
                hash=31*hash+Arrays.hashCode(CharTypes.copyHexBytes(true));
                StringBuilder quoted=new StringBuilder();CharTypes.appendQuoted(quoted,"a\\n\\t\\\"\\\\"+new String(new int[]{0,31,127,0x10400},0,4));return 31*hash+quoted.toString().hashCode();
            }}
            """);compare(List.of("run"));
    }

    @Test void executesUnchangedPinnedSnakeYamlMarks() throws Exception {
        Path repository=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()));
        Path artifact=repository.resolve("org/yaml/snakeyaml/2.2/snakeyaml-2.2.jar");
        assertDigest(artifact,"1467931448a0817696ae2805b7b8b20bfb082652bf9c4efaed528930dc49389b");
        try(JarFile jar=new JarFile(artifact.toFile())){for(String name:List.of("org/yaml/snakeyaml/error/Mark","org/yaml/snakeyaml/scanner/Constant"))writeClass(name,jar.getInputStream(jar.getJarEntry(name+".class")).readAllBytes());}
        fixtureClasspath=directory.toString();
        fixture("""
            import org.yaml.snakeyaml.error.Mark;import org.yaml.snakeyaml.scanner.Constant;import java.util.*;
            public class Metadata {public static int run(){int hash=1;
                char[][] texts={new char[0],{'a','b','c'},{'x',0xd801,0xdc28,'y',0xd800,'z',0xdc00},{'a',10,'b',0x2028,'c'}};
                for(char[] text:texts){Mark mark=new Mark("fixture",4,2,3,text,0);
                    hash=31*hash+Arrays.hashCode(mark.getBuffer());hash=31*hash+mark.getIndex()+mark.getLine()+mark.getColumn()+mark.getPointer()+mark.getName().hashCode();
                    if(text.length>0){hash=31*hash+mark.get_snippet().hashCode();hash=31*hash+mark.toString().hashCode();}
                }
                int[] data=new int[100];Arrays.fill(data,'a');data[50]=0x10400;data[80]=0x1f600;
                Mark longLine=new Mark("long",70,0,70,data,70);hash=31*hash+longLine.get_snippet(2,25).hashCode();
                hash=31*hash+longLine.get_snippet().hashCode();
                for(int c:new int[]{0,9,10,13,32,65,0x85,0x2028,0x2029,0x10400}){
                    hash=31*hash+(Constant.LINEBR.has(c)?1:0)+(Constant.NULL_BL_T_LINEBR.has(c)?2:0)+(Constant.ALPHA.has(c)?4:0)+(Constant.URI_CHARS.hasNo(c)?8:0);
                }return hash;
            }}
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
            Class<?> type=loader.loadClass(fixtureClassName);for(String name:names){Object expected=type.getMethod(name).invoke(null);assertions.append("assert.equal(").append(expected instanceof String?"text(e['"+fixtureClassName+".":"e['"+fixtureClassName+".").append(name).append(expected instanceof String?"']()),":"'](),").append(expected instanceof String?js((String)expected):expected).append(");\n");}
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
                const memory=new WebAssembly.Memory({initial:2});let heap=1024,allocations=0,failAllocation=false;
                const e=new WebAssembly.Instance(compiled,{env:{memory,__heap_alloc:size=>{
                    allocations++;if(failAllocation){failAllocation=false;return 0;}
                    const address=heap;heap+=size;
                    while(heap>memory.buffer.byteLength)memory.grow(1);
                    new Uint8Array(memory.buffer,address,size).fill(0x7f);return address;
                }}}).exports;
                assert.equal(allocations,0);assert.equal(e.__gc_heap_bytes?.()??0,0);if(e.__gc_scribble)e.__gc_scribble.value=1;heap=Math.max(heap,e.__exception_heap_base?.value??0);
                function text(pointer){if(!pointer)return null;const view=new DataView(memory.buffer);
                    const chars=view.getInt32(pointer+4,true),length=view.getInt32(chars+4,true);
                    let result='';for(let i=0;i<length;i++)result+=String.fromCharCode(view.getUint16(chars+8+2*i,true));return result;}
            """+assertions+"\n}");
        run("node",script.toString(),output.toString());
    }
}
