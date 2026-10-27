package one.dastec.javaw.bytecode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import javax.tools.ToolProvider;
import java.io.*;import java.nio.file.*;import java.util.*;import java.util.concurrent.TimeUnit;import java.util.jar.*;
import static org.junit.jupiter.api.Assertions.*;
class BytecodeRegexTest {
 @TempDir Path directory;
 private final String fixtureClassName="Metadata";
 private String fixtureClasspath="";
 private final Map<String,byte[]> carriers=new HashMap<>();
 @Test void literalsAndDiagnostics() throws Exception {
 fixture("""
import java.util.regex.*;import java.util.function.*;
public class Metadata {public static int run(int group){int h=1;
 for(String text:new String[]{"","abc","a.b[+]","😀x",new String(new char[]{(char)0xd800}),"a\\\\Eb"}){
  Pattern p=Pattern.compile(text,Pattern.LITERAL);h=31*h+(p.matcher(text).matches()?1:0);h=31*h+(p.matcher(text+"x").matches()?1:0);
  h=31*h+p.pattern().hashCode();h=31*h+p.toString().hashCode();h=31*h+p.flags();h=31*h+(p.matcher(text).pattern()==p?1:0);
  String quoted=Pattern.quote(text);h=31*h+quoted.hashCode();h=31*h+(Pattern.matches(quoted,text)?1:0);
  Function<String,Pattern> compile=Pattern::compile;h=31*h+(compile.apply(quoted).matcher(new StringBuilder(text)).matches()?1:0);
 }
 PatternSyntaxException e=new PatternSyntaxException("bad","abc",1);h=31*h+e.getDescription().hashCode();h=31*h+e.getPattern().hashCode();h=31*h+e.getIndex();h=31*h+e.getMessage().hashCode();
 for(int mode=0;mode<4;mode++){try{if(mode==0)Pattern.compile(null);if(mode==1)Pattern.quote(null);if(mode==2)Pattern.compile("a").matcher(null);if(mode==3)Pattern.compile("a",512);throw new Error();}catch(NullPointerException|IllegalArgumentException ex){h++;}}
 return h;
}}
""");compare(1);
 }
 @Test void atomsAndEscapes() throws Exception {
 fixture("""
import java.util.regex.*;
public class Metadata {public static int run(int group){int h=1;
 for(String regex:new String[]{".","a.b","\\\\.","\\\\n","\\\\x41","\\\\u0041","\\\\Q.*\\\\E","\\\\Qa\\\\E.b","a\\\\+b"})
  for(String text:new String[]{"","a","A","a.b","a😀b",".",".*","a+b","\\n","😀",new String(new char[]{(char)0xd800})})h=31*h+(Pattern.matches(regex,text)?1:0);
 for(String regex:new String[]{"*a","\\\\","\\\\xzz"}){try{Pattern.compile(regex);throw new Error();}catch(PatternSyntaxException e){h=31*h+e.getDescription().hashCode();h=31*h+e.getIndex();}}
 return h;
}}
""");compare(1);
 }
 @Test void characterClasses() throws Exception {
 fixture("""
import java.util.regex.*;
public class Metadata {public static int run(int group){int h=1;
 for(String regex:new String[]{"[abc]","[^abc]","[a-z]","[a-zA-Z0-9_]","[-a]","[a-]","[]a]","[\\\\]x]","[😀-🙏]","\\\\d","\\\\D","\\\\w","\\\\W","\\\\s","\\\\S","[\\\\dA-F]","[^\\\\s]"})
  for(String text:new String[]{"","a","A","0","_","-","]","é","😀","🙏","\\n","\\u00a0"," ","xx"})h=31*h+(Pattern.matches(regex,text)?1:0);
 for(String regex:new String[]{"[","[abc","[z-a]"})try{Pattern.compile(regex);throw new Error();}catch(PatternSyntaxException e){h=31*h+e.getDescription().hashCode();h=31*h+e.getIndex();}
 return h;
}}
""");compare(1);
 }
 @Test void repetition() throws Exception {
 fixture("""
import java.util.regex.*;
public class Metadata {public static int run(int group){int h=1;
 String regex=new String[]{"a*","a+","a?","a{2}","a{1,3}","a{2,}","a*?a","a+?a",".*a",".{0,2}","\\\\Qab\\\\E+","[ab]*a","a?a?aa"}[group];Pattern pattern=Pattern.compile(regex);
 for(String text:new String[]{"","a","aa","aaa","aaaa","ab","abb","aba","ba","😀","😀a"})h=31*h+(pattern.matcher(text).matches()?1:0);
 for(String bad:new String[]{"a{3,2}","a{2","a{}","*"})try{Pattern.compile(bad);throw new Error();}catch(PatternSyntaxException e){h=31*h+e.getDescription().hashCode();h=31*h+e.getIndex();}
 return h;
}}
""");compare(13);
 }
 @Test void groupsAndAlternatives() throws Exception {
 fixture("""
import java.util.regex.*;
public class Metadata {public static int run(int group){int h=1;
 String regex=new String[]{"(a|ab)c","(ab|a)b","((a)b)?(c*)","(?:ab)+","(a(b)?)+","(a?)*","(a?)*a","(a*){2,3}","a|","|a","((a)|(b))+"}[group];Pattern p=Pattern.compile(regex);
 for(String text:new String[]{"","a","b","ab","abc","ababc","aba","aaa","ccc"}){Matcher m=p.matcher(text);h=31*h+m.groupCount();boolean found=m.matches();h=31*h+(found?1:0);if(found){MatchResult result=m;for(int i=0;i<=m.groupCount();i++){String capture=result.group(i);h=31*h+(capture==null?17:capture.hashCode());h=31*h+result.start(i);h=31*h+result.end(i);}try{m.group(m.groupCount()+1);throw new Error();}catch(IndexOutOfBoundsException e){h++;}}else try{m.group();throw new Error();}catch(IllegalStateException e){h++;}}
 for(String bad:new String[]{"(","(abc","a)"})try{Pattern.compile(bad);throw new Error();}catch(PatternSyntaxException e){h=31*h+e.getDescription().hashCode();h=31*h+e.getIndex();}
 return h;
}}
""");compare(11);
 }
 @Test void anchorsAndFlags() throws Exception {
 fixture("""
import java.util.regex.*;
public class Metadata {public static int run(int group){int h=1;int flags=new int[]{0,2,66,32,8,1,9,18}[group];
 for(String regex:new String[]{"a","A","é","É","[A-Z]","[^A]","[K]","[ſ]","[İ]","𐐀",".","^a$","\\\\Aa\\\\z","a$\\\\n","a\\\\Z\\\\r\\\\n"}){Pattern p=Pattern.compile(regex,flags);h=31*h+p.flags();
  for(String text:new String[]{"a","A","é","É","K","k","K","S","s","ſ","I","i","İ","ı","𐐨","😀","\\n","a\\n","a\\r\\n","\\u0085","^a$"})h=31*h+(p.matcher(text).matches()?1:0);}
 return h;
}}
""");compare(8);
 }
    private String classpath(){return directory+(fixtureClasspath.isEmpty()?"":File.pathSeparator+fixtureClasspath);}

    /** Preserve selected original method instructions; omit unrelated library state and initialization. */
    private ClassNode selectedLibrary(String relative,String artifact,String hash,java.util.function.Predicate<String> select) throws Exception {
        Path jar=Path.of(System.getProperty("user.home"),".m2/repository",artifact);
        assertTrue(Files.isRegularFile(jar),"Pinned artifact required: "+jar);
        assertEquals(hash,HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(jar))));
        fixtureClasspath=jar.toString();byte[] original;
        try(JarFile input=new JarFile(jar.toFile())){original=input.getInputStream(input.getJarEntry(relative)).readAllBytes();}
        Path target=directory.resolve(relative);Files.createDirectories(target.getParent());Files.write(target,original);
        ClassNode full=new ClassNode();new ClassReader(original).accept(full,0);
        ClassNode carrier=new ClassNode();carrier.visit(full.version,full.access,full.name,null,full.superName,null);
        carrier.methods.addAll(full.methods.stream().filter(m->select.test(m.name)).toList());
        return carrier;
    }
    private void carrier(ClassNode node){ClassWriter writer=new ClassWriter(0);node.accept(writer);carriers.put(node.name+".class",writer.toByteArray());}

    private byte[] fixture(String source) throws Exception {
        try(var old=Files.list(directory)){for(Path path:old.filter(v->v.getFileName().toString().matches("Metadata.*\\.class")).toList())Files.delete(path);}
        Path file=directory.resolve("Metadata.java");Files.writeString(file,source);
        List<String> options=new ArrayList<>(List.of("-g","-parameters","--release","17","-d",directory.toString()));
        options.add("-classpath");options.add(classpath());
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
                jar.putNextEntry(new JarEntry(directory.relativize(file).toString()));jar.write(carriers.containsKey(entry)?carriers.get(entry):Files.readAllBytes(file));jar.closeEntry();}
        }
        return bytes.toByteArray();
    }

    private void compare(int groups) throws Exception {
        Path java=Path.of(System.getProperty("javaw.jdk17.home",System.getProperty("user.home")+"/Library/Java/JavaVirtualMachines/graalvm-ce-17.0.9/Contents/Home"),"bin/java");
        assertTrue(Files.isExecutable(java),"A JDK 17 oracle is required (javaw.jdk17.home)");
        run(java.toString(),"-Djava.locale.providers=CLDR","-Duser.language=","-Duser.country=","-Duser.variant=","-cp",classpath(),"CaseOracle",fixtureClassName,Integer.toString(groups));
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
        Files.writeString(input,wat);run("wat2wasm","--enable-exceptions","--debug-names",input.toString(),"-o",output.toString());
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
            """.replace("EXPECTED",expected)+"if(e['Metadata.unsupportedParallel'])assert.equal(e['Metadata.unsupportedParallel'](),1);\nassert.equal(e['"+fixtureClassName+".run'](group),expected[group],'batch '+group);\n}");
        run("node",script.toString(),output.toString());
    }
}
