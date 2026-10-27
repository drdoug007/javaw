package one.dastec.javaw.bytecode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import javax.tools.ToolProvider;
import java.io.*;import java.nio.file.*;import java.util.*;import java.util.concurrent.TimeUnit;import java.util.jar.*;
import static org.junit.jupiter.api.Assertions.*;
class BytecodeSpliteratorTest {
 @TempDir Path directory;
 private final String fixtureClassName="Metadata";
 private String fixtureClasspath="";
 private final Map<String,byte[]> carriers=new HashMap<>();
 @Test void generatedArrays() throws Exception {fixture("""
import java.util.*;import java.util.function.*;public class Metadata {public static int run(int g){int h=1;for(Collection<String> c:Arrays.<Collection<String>>asList(new ArrayList<>(Arrays.asList("b",null,"a")),new TreeSet<>(Arrays.asList("c","a")),Collections.unmodifiableList(Arrays.asList("x","y")),Collections.emptyList())){int[] calls={0};String[] a=c.toArray(n->{calls[0]++;if(n!=0)throw new Error();return new String[n];});h=31*h+Arrays.hashCode(a);h=31*h+calls[0];h=31*h+(a instanceof String[]?1:0);try{c.toArray((IntFunction<String[]>)null);throw new Error();}catch(NullPointerException e){h++;}}Collection<Object> wrong=new ArrayList<>();wrong.add(1);try{wrong.toArray(String[]::new);throw new Error();}catch(ArrayStoreException e){h++;}try{wrong.toArray(n->{throw new IllegalArgumentException();});throw new Error();}catch(IllegalArgumentException e){h++;}return h;}}
""");compare(1);}
 @Test void defaultsAndEmpty() throws Exception {fixture("""
import java.util.*;import java.util.function.*;public class Metadata {
 static class Counter implements Spliterator<Integer>{int n;public boolean tryAdvance(Consumer<? super Integer> a){Objects.requireNonNull(a);if(n==3)return false;a.accept(++n);return true;}public Spliterator<Integer> trySplit(){return null;}public long estimateSize(){return 3-n;}public int characteristics(){return Spliterator.SIZED|Spliterator.ORDERED;}}
 public static int run(int g){int h=1;Spliterator<String> e=Spliterators.emptySpliterator();h=31*h+(int)e.estimateSize();h=31*h+e.characteristics();h=31*h+(int)e.getExactSizeIfKnown();h=31*h+(e.hasCharacteristics(Spliterator.SIZED|Spliterator.SUBSIZED)?1:0);h=31*h+(e.trySplit()==null?1:0);h=31*h+(e.tryAdvance(x->{throw new Error();})?1:0);for(int i=0;i<3;i++)try{if(i==0)e.tryAdvance(null);if(i==1)e.forEachRemaining(null);if(i==2)e.getComparator();throw new Error();}catch(NullPointerException|IllegalStateException x){h++;}Counter c=new Counter();int[] sum={0};c.forEachRemaining(x->sum[0]+=x);h=31*h+sum[0];h=31*h+(int)c.getExactSizeIfKnown();h=31*h+(c.hasCharacteristics(0)?1:0);return h;}}
""");compare(1);}
 @Test void arraySplitting() throws Exception {fixture("""
import java.util.*;import java.util.function.*;public class Metadata {static int drain(Spliterator<String> s){int[] h={1};s.forEachRemaining(x->h[0]=31*h[0]+Objects.hashCode(x));return h[0];}public static int run(int g){int h=1;String[] a={"a",null,"c","d","e"};Spliterator<String> s=Spliterators.spliterator(a,1,5,Spliterator.ORDERED);h=31*h+s.characteristics();h=31*h+(int)s.getExactSizeIfKnown();Spliterator<String> left=s.trySplit();a[2]="changed";h=31*h+drain(left);h=31*h+drain(s);h=31*h+(int)s.estimateSize();s=Spliterators.spliterator(a,0);try{s.tryAdvance(x->{throw new IllegalArgumentException();});}catch(IllegalArgumentException e){h++;}h=31*h+(int)s.estimateSize();try{s.forEachRemaining(x->{throw new IllegalArgumentException();});}catch(IllegalArgumentException e){h++;}h=31*h+(int)s.estimateSize();for(int i=0;i<4;i++)try{if(i==0)Spliterators.spliterator(a,4,2,0);if(i==1)Spliterators.spliterator(a,-1,2,0);if(i==2)Spliterators.spliterator(a,1,7,0);if(i==3)Spliterators.spliterator((Object[])null,0);throw new Error();}catch(ArrayIndexOutOfBoundsException|NullPointerException e){h++;}h=31*h+(Spliterators.spliterator(a,Spliterator.SORTED).getComparator()==null?1:0);try{s.tryAdvance(null);throw new Error();}catch(NullPointerException e){h++;}return h;}}
""");compare(1);}
 @Test void iteratorAdapters() throws Exception {fixture("""
import java.util.*;public class Metadata {public static int run(int g){int h=1;for(boolean known:new boolean[]{false,true}){Iterator<Integer> it=Arrays.asList(1,2,3,4,5).iterator();Spliterator<Integer> s=known?Spliterators.spliterator(it,5,Spliterator.ORDERED):Spliterators.spliteratorUnknownSize(it,Spliterator.ORDERED|Spliterator.SIZED);h=31*h+s.characteristics();h=31*h+(int)s.getExactSizeIfKnown();int[] sum={0};s.tryAdvance(x->sum[0]+=x);h=31*h+(int)s.estimateSize();Spliterator<Integer> prefix=s.trySplit();h=31*h+(int)prefix.estimateSize();h=31*h+prefix.characteristics();Iterator<Integer> adapter=Spliterators.iterator(prefix);h=31*h+(adapter.hasNext()&&adapter.hasNext()?1:0);while(adapter.hasNext())sum[0]+=adapter.next();h=31*h+sum[0];h=31*h+(s.trySplit()==null?1:0);try{adapter.next();throw new Error();}catch(NoSuchElementException e){h++;}try{adapter.remove();throw new Error();}catch(UnsupportedOperationException e){h++;}}for(int i=0;i<3;i++)try{if(i==0)Spliterators.iterator((Spliterator<Object>)null);if(i==1)Spliterators.spliteratorUnknownSize((Iterator<Object>)null,0);if(i==2)Spliterators.spliterator((Iterator<Object>)null,1,0);throw new Error();}catch(NullPointerException e){h++;}return h;}}
""");compare(1);}
 @Test void abstractBatching() throws Exception {fixture("""
import java.util.*;import java.util.function.*;public class Metadata {static class Counter extends Spliterators.AbstractSpliterator<Integer>{int n;Counter(long size,int flags){super(size,flags);}public boolean tryAdvance(Consumer<? super Integer> a){Objects.requireNonNull(a);if(n==12)return false;a.accept(n++);return true;}}public static int run(int g){int h=1;for(boolean sized:new boolean[]{false,true}){Counter c=new Counter(sized?12:Long.MAX_VALUE,Spliterator.ORDERED|(sized?Spliterator.SIZED:0));h=31*h+c.characteristics();int[] sum={0};c.tryAdvance(x->sum[0]+=x);Spliterator<Integer> p=c.trySplit();h=31*h+(int)p.getExactSizeIfKnown();h=31*h+p.characteristics();p.forEachRemaining(x->sum[0]+=x);h=31*h+sum[0];h=31*h+(int)c.estimateSize();h=31*h+(c.trySplit()==null?1:0);try{c.forEachRemaining(null);throw new Error();}catch(NullPointerException e){h++;}}Counter sorted=new Counter(0,Spliterator.SORTED);try{sorted.getComparator();throw new Error();}catch(IllegalStateException e){h++;}return h;}}
""");compare(1);}
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
            """.replace("EXPECTED",expected)+"assert.equal(e['"+fixtureClassName+".run'](group),expected[group],'batch '+group);\n}");
        run("node",script.toString(),output.toString());
    }
}
