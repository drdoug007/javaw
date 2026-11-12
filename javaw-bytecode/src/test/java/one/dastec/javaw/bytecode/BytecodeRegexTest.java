package one.dastec.javaw.bytecode;
import one.dastec.javaw.Wat2Wasm;
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
 @Test void findAndReset() throws Exception {
 fixture("""
import java.util.regex.*;
public class Metadata {public static int excludedState(){Matcher m=Pattern.compile("a").matcher("b");try{m.matches();m.find();return 0;}catch(UnsupportedOperationException e){m.reset();return m.find()?0:1;}}static int result(Matcher m){int h=1;try{h=31*h+m.start();h=31*h+m.end();for(int i=0;i<=m.groupCount();i++){String s=m.group(i);h=31*h+(s==null?17:s.hashCode());h=31*h+m.start(i);h=31*h+m.end(i);}}catch(IllegalStateException e){h+=7;}return h;}
 public static int run(int group){int h=1;String regex=new String[]{"","(a+)(b?)","^a","a$","\\\\A|\\\\z",".","a*?","(a|ab)"}[group];Pattern p=Pattern.compile(regex,Pattern.MULTILINE);
 for(String text:new String[]{"","ab aa\\nab","😀a","a\\r\\nb","aa"}){Matcher m=p.matcher(text);int count=0;while(m.find()){h=31*h+result(m);if(++count>30)throw new Error();}h=31*h+count;h=31*h+result(m);h=31*h+(m.find()?1:0);h=31*h+result(m);
  h=31*h+(m.reset()==m?1:0);h=31*h+(m.lookingAt()?1:0);h=31*h+result(m);h=31*h+(m.matches()?1:0);h=31*h+result(m);m.reset();h=31*h+(m.find()?1:0);h=31*h+result(m);
  h=31*h+(m.find(text.length())?1:0);h=31*h+result(m);for(int start:new int[]{-1,text.length()+1})try{m.find(start);throw new Error();}catch(IndexOutOfBoundsException e){h++;}
  m.reset(new StringBuilder(text));h=31*h+(m.find()?1:0);try{m.reset(null);throw new Error();}catch(NullPointerException e){h++;}h=31*h+result(m);
 }
 h=31*h+("aaa".matches("a+")?1:0);return h;
}}
""");compare(8);
 }
 @Test void replacements() throws Exception {
 fixture("""
import java.util.regex.*;
public class Metadata {public static int run(int group){int h=1;String regex=new String[]{"(a+)(b?)","","a","z","(a)?b"}[group];
 for(String text:new String[]{"","ab aa b","😀ab"}){
  for(String replacement:new String[]{"x","$0","$1:$2","$10","$01","$","\\\\","$9","$x",null})for(int first=0;first<2;first++)try{String out=first==0?text.replaceAll(regex,replacement):text.replaceFirst(regex,replacement);h=31*h+out.hashCode();}catch(NullPointerException e){h=31*h+1;}catch(IllegalArgumentException e){h=31*h+2;}catch(IndexOutOfBoundsException e){h=31*h+3;}
  for(String replacement:new String[]{"plain","$1","a\\\\E$0","\\\\$"}){String quote=Matcher.quoteReplacement(replacement);h=31*h+quote.hashCode();h=31*h+text.replaceAll(regex,quote).hashCode();}
  Matcher m=Pattern.compile(regex).matcher(text);StringBuilder a=new StringBuilder("prefix");while(m.find())h=31*h+(m.appendReplacement(a,"<$0>")==m?1:0);h=31*h+(m.appendTail(a)==a?1:0);h=31*h+a.toString().hashCode();
  m.reset();StringBuffer b=new StringBuffer();while(m.find())m.appendReplacement(b,"<$0>");h=31*h+(m.appendTail(b)==b?1:0);h=31*h+b.toString().hashCode();
 }
 Matcher unused=Pattern.compile("a").matcher("a");try{unused.appendReplacement(new StringBuilder(),"x");throw new Error();}catch(IllegalStateException e){h++;}
 try{Matcher.quoteReplacement(null);throw new Error();}catch(NullPointerException e){h++;}return h;
}}
""");compare(5);
 }
    private void h2Help() throws Exception {
        ClassNode selected=selectedLibrary("org/h2/command/dml/Help.class","com/h2database/h2/2.2.224/h2-2.2.224.jar",
            "b9d8f19358ada82a4f6eb5b174c6cfe320a375b5a9cb5a4fe456d623e6e55497",name->name.equals("stripAnnotationsFromSyntax"));
        selected.superName="java/lang/Object";carrier(selected);
        fixture("""
            import org.h2.command.dml.Help;
            public class Metadata {public static int run(int group){int h=1;
             for(String text:new String[]{"","  SELECT @c@ a @h2@ b  ","@c@@h2@@c@", " x @c@ @h2@ 😀 ","ordinary"})h=31*h+Help.stripAnnotationsFromSyntax(text).hashCode();
             try{Help.stripAnnotationsFromSyntax(null);throw new Error();}catch(NullPointerException e){h++;}return h;
            }}
            """);compare(1);
    }

 @Test void splitSemantics() throws Exception {
 fixture("""
import java.util.*;import java.util.regex.*;
public class Metadata {public static int run(int group){int h=1;String regex=new String[]{",","","a*","a*?","^|$","[,;]+","(,)","\\\\Q..\\\\E"}[group];Pattern p=Pattern.compile(regex);
 for(String text:new String[]{"","a,b,,","a..b..c","😀x","aaa",",,a,", "a;b,c"})for(int limit:new int[]{-3,-1,0,1,2,3,20}){
  h=31*h+Arrays.hashCode(text.split(regex,limit));h=31*h+Arrays.hashCode(p.split(new StringBuilder(text),limit));
 }
 h=31*h+Arrays.hashCode("a,b,".split(regex));h=31*h+Arrays.hashCode(p.split("a,b,"));try{p.split(null);throw new Error();}catch(NullPointerException e){h++;}return h;
}}
""");compare(8);h2Help();
 }
    private void logbackRange() throws Exception {
        ClassNode selected=selectedLibrary("ch/qos/logback/classic/pattern/CallerDataConverter.class",
            "ch/qos/logback/logback-classic/1.4.14/logback-classic-1.4.14.jar",
            "8e832f7263ca606ae36dabb2d8b24c2f43d82cf634e81dad9d1640fa6ee3c596",name->name.equals("splitRange")||name.equals("getDefaultRangeDelimiter"));
        selected.superName="java/lang/Object";
        MethodVisitor ctor=selected.visitMethod(Opcodes.ACC_PUBLIC,"<init>","()V",null,null);ctor.visitCode();ctor.visitVarInsn(Opcodes.ALOAD,0);ctor.visitMethodInsn(Opcodes.INVOKESPECIAL,"java/lang/Object","<init>","()V",false);ctor.visitInsn(Opcodes.RETURN);ctor.visitMaxs(1,1);ctor.visitEnd();
        MethodVisitor shim=selected.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"javawSplit","(Ljava/lang/String;)[Ljava/lang/String;",null,null);shim.visitCode();shim.visitTypeInsn(Opcodes.NEW,selected.name);shim.visitInsn(Opcodes.DUP);shim.visitMethodInsn(Opcodes.INVOKESPECIAL,selected.name,"<init>","()V",false);shim.visitVarInsn(Opcodes.ALOAD,0);shim.visitMethodInsn(Opcodes.INVOKESPECIAL,selected.name,"splitRange","(Ljava/lang/String;)[Ljava/lang/String;",false);shim.visitInsn(Opcodes.ARETURN);shim.visitMaxs(2,1);shim.visitEnd();carrier(selected);
        for(String dependency:new String[]{"ch/qos/logback/logback-core/1.4.14/logback-core-1.4.14.jar","org/slf4j/slf4j-api/2.0.2/slf4j-api-2.0.2.jar"})fixtureClasspath+=File.pathSeparator+Path.of(System.getProperty("user.home"),".m2/repository",dependency);
        Path bridge=directory.resolve("RegexBridge.java");Files.writeString(bridge,"""
            public class RegexBridge {public static String[] run(String text){try{
              Class<?> type=Class.forName("ch.qos.logback.classic.pattern.CallerDataConverter");var method=type.getDeclaredMethod("splitRange",String.class);method.setAccessible(true);
              return (String[])method.invoke(type.getConstructor().newInstance(),text);
            }catch(java.lang.reflect.InvocationTargetException e){Throwable c=e.getCause();if(c instanceof RuntimeException)throw (RuntimeException)c;throw (Error)c;
            }catch(ReflectiveOperationException e){throw new Error(e);}}}
            """);assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,"--release","17","-d",directory.toString(),bridge.toString()));
        ClassNode direct=new ClassNode();direct.visit(Opcodes.V17,Opcodes.ACC_PUBLIC,"RegexBridge",null,"java/lang/Object",null);
        MethodVisitor run=direct.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"run","(Ljava/lang/String;)[Ljava/lang/String;",null,null);run.visitCode();run.visitVarInsn(Opcodes.ALOAD,0);run.visitMethodInsn(Opcodes.INVOKESTATIC,selected.name,"javawSplit","(Ljava/lang/String;)[Ljava/lang/String;",false);run.visitInsn(Opcodes.ARETURN);run.visitMaxs(1,1);run.visitEnd();carrier(direct);
        fixture("""
            import java.util.Arrays;
            public class Metadata {public static int run(int group){int h=1;for(String text:new String[]{"","1","1..5","..5","1..","1..5..7","...","😀..𐐀"})h=31*h+Arrays.hashCode(RegexBridge.run(text));
             try{RegexBridge.run(null);throw new Error();}catch(NullPointerException e){h++;}return h;}}
            """);compare(1);
    }

 @Test void predicatesUnicodeAndAdmission() throws Exception {
 fixture("""
import java.util.regex.*;import java.util.function.*;
public class Metadata {
 public static int exclusions(){int h=0;for(String regex:new String[]{"(?=a)","(?<x>a)","(?i)a","(a)\\\\1","a++","[a&&b]","\\\\p{L}","\\\\b","\\\\x{d800}"})try{Pattern.compile(regex);return 0;}catch(UnsupportedOperationException e){h++;}
 for(int flag:new int[]{Pattern.COMMENTS,Pattern.CANON_EQ,Pattern.UNICODE_CHARACTER_CLASS})try{Pattern.compile("a",flag);return 0;}catch(UnsupportedOperationException e){h++;}return h==12?1:0;}
 public static int run(int group){int h=1;Pattern p=Pattern.compile("a+");Predicate<String> find=p.asPredicate(),match=p.asMatchPredicate();
 for(String text:new String[]{"","a","baab","aa","b"}){h=31*h+(find.test(text)?1:0);h=31*h+(match.test(text)?1:0);h=31*h+(find.test(text)?1:0);}
 h=31*h+(p instanceof java.io.Serializable?1:0);Function<String,Matcher> make=p::matcher;h=31*h+(make.apply("a").matches()?1:0);
 for(int i=0;i<2;i++)try{(i==0?find:match).test(null);throw new Error();}catch(NullPointerException e){h++;}
 for(String regex:new String[]{"\\\\uD83D\\\\uDE00+","[\\\\uD83D\\\\uDE00]","\\\\x{1f600}+","\\\\x{41}","\\\\uD800","\\\\x41"})for(String text:new String[]{"","A","😀","😀😀",new String(new char[]{(char)0xd800}),"a"})h=31*h+(Pattern.matches(regex,text)?1:0);
 for(String regex:new String[]{"\\\\q","[\\\\q]","\\\\x{z}","\\\\x{}","\\\\x{110000}"})try{Pattern.compile(regex);throw new Error();}catch(PatternSyntaxException e){h++;}
 return h;
 }}
""");compare(1);logbackRange();regexAdmission();
 }
    private void regexAdmission() {
        String[][] signatures={
          {"java/util/regex/Pattern","compile","(Ljava/lang/String;)Ljava/util/regex/Pattern;","static"},
          {"java/util/regex/Pattern","quote","(Ljava/lang/String;)Ljava/lang/String;","static"},
          {"java/util/regex/Matcher","find","()Z","virtual"},
          {"java/util/regex/Matcher","matches","()Z","virtual"},
          {"java/util/regex/Matcher","group","(I)Ljava/lang/String;","virtual"},
          {"java/util/regex/MatchResult","group","(I)Ljava/lang/String;","interface"},
          {"java/lang/String","matches","(Ljava/lang/String;)Z","virtual"},
          {"java/lang/String","split","(Ljava/lang/String;I)[Ljava/lang/String;","virtual"}
        };
        for(String[] call:signatures)for(int variant=0;variant<4;variant++){
            boolean iface=call[3].equals("interface"),statik=call[3].equals("static");
            String descriptor=variant%2==0?"(J)Ljava/lang/Object;":call[2];
            ClassWriter writer=new ClassWriter(0);writer.visit(Opcodes.V17,Opcodes.ACC_PUBLIC,"BadRegexCall",null,"java/lang/Object",null);writer.visitSource("RegexCalls.java",null);
            MethodVisitor m=writer.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"unused","()Ljava/lang/Object;",null,null);
            m.visitCode();Label line=new Label();m.visitLabel(line);m.visitLineNumber(61,line);
            if(variant<2){m.visitInsn(Opcodes.ACONST_NULL);m.visitInsn(Opcodes.ACONST_NULL);m.visitInsn(Opcodes.ACONST_NULL);
                int opcode=variant==1?(statik?Opcodes.INVOKEINTERFACE:Opcodes.INVOKESTATIC):(statik?Opcodes.INVOKESTATIC:iface?Opcodes.INVOKEINTERFACE:Opcodes.INVOKEVIRTUAL);
                m.visitMethodInsn(opcode,call[0],call[1],descriptor,iface);
            }else{int tag=variant==3?(statik?Opcodes.H_INVOKEINTERFACE:Opcodes.H_INVOKESTATIC):(statik?Opcodes.H_INVOKESTATIC:iface?Opcodes.H_INVOKEINTERFACE:Opcodes.H_INVOKEVIRTUAL);
                m.visitLdcInsn(new Handle(tag,call[0],call[1],descriptor,iface));}
            m.visitInsn(Opcodes.ARETURN);m.visitMaxs(4,0);m.visitEnd();writer.visitEnd();
            var failure=assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileClassWithExceptionRuntime(writer.toByteArray()));
            assertTrue(failure.getMessage().contains("unused"),failure.getMessage());
            assertTrue(failure.getMessage().contains("RegexCalls.java:61"),failure.getMessage());
        }
        for(String[] api:new String[][]{
          {"java/util/regex/Matcher","region","(II)Ljava/util/regex/Matcher;"},
          {"java/util/regex/Matcher","toMatchResult","()Ljava/util/regex/MatchResult;"},
          {"java/util/regex/Matcher","replaceAll","(Ljava/util/function/Function;)Ljava/lang/String;"},
          {"java/util/regex/Pattern","splitAsStream","(Ljava/lang/CharSequence;)Ljava/util/stream/Stream;"}}){
            ClassWriter writer=new ClassWriter(0);writer.visit(Opcodes.V17,Opcodes.ACC_PUBLIC,"UnsupportedRegexApi",null,"java/lang/Object",null);writer.visitSource("RegexCalls.java",null);
            MethodVisitor m=writer.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"unused","()Ljava/lang/Object;",null,null);m.visitCode();Label line=new Label();m.visitLabel(line);m.visitLineNumber(61,line);m.visitInsn(Opcodes.ACONST_NULL);
            m.visitMethodInsn(Opcodes.INVOKEVIRTUAL,api[0],api[1],api[2],false);m.visitInsn(Opcodes.ARETURN);m.visitMaxs(4,0);m.visitEnd();writer.visitEnd();
            var failure=assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileClassWithExceptionRuntime(writer.toByteArray()));
            assertTrue(failure.getMessage().contains("unused"),failure.getMessage());assertTrue(failure.getMessage().contains("RegexCalls.java:61"),failure.getMessage());
        }
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
        return jar();
    }

    private byte[] jar() throws Exception {
        List<Path> files;
        try(var paths=Files.walk(directory)) {
            files=new ArrayList<>(paths.filter(path->path.toString().endsWith(".class") && !path.getFileName().toString().equals("CaseOracle.class")).sorted().toList());
        }
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
        execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar())),expected);
    }

    private void run(String... arguments) throws Exception {
        Path log=directory.resolve("command.log");Process process=new ProcessBuilder(arguments).redirectErrorStream(true).redirectOutput(log.toFile()).start();
        if(!process.waitFor(60,TimeUnit.SECONDS)){process.destroyForcibly();fail("Command timed out");}
        assertEquals(0,process.exitValue(),Files.readString(log));
    }
    private void execute(String wat,String expected) throws Exception {
        Path input=directory.resolve("metadata.wat"),output=directory.resolve("metadata.wasm"),script=directory.resolve("run.cjs");
        Files.writeString(input,wat);WasmTestSupport.assemble(input,output,Wat2Wasm.EXCEPTIONS, "--debug-names");
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
            """.replace("EXPECTED",expected)+"if(e['Metadata.excludedState'])assert.equal(e['Metadata.excludedState'](),1);\nif(e['Metadata.exclusions'])assert.equal(e['Metadata.exclusions'](),1);\nassert.equal(e['"+fixtureClassName+".run'](group),expected[group],'batch '+group);\n}");
        WasmTestSupport.node(script,output);
    }
}
