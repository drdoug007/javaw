package one.dastec.javaw.bytecode;
import one.dastec.javaw.Wat2Wasm;
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
 @Test void collectionBinding() throws Exception {fixture("""
import java.util.*;public class Metadata {public static int run(int g){int h=1;int[] created={0};Iterable<Integer> iterable=()->{created[0]++;return Arrays.asList(1,2).iterator();};Spliterator<Integer> unknown=iterable.spliterator();h=31*h+created[0];h=31*h+unknown.characteristics();h=31*h+(int)unknown.getExactSizeIfKnown();List<Integer> values=new ArrayList<>(Arrays.asList(1,2));Spliterator<Integer> s=values.spliterator();values.add(3);h=31*h+s.characteristics();h=31*h+(int)s.estimateSize();int[] sum={0};s.forEachRemaining(x->sum[0]+=x);h=31*h+sum[0];Collection<Integer> c=new AbstractCollection<Integer>(){public int size(){return values.size();}public Iterator<Integer> iterator(){return values.iterator();}};s=c.spliterator();values.add(4);h=31*h+(int)s.getExactSizeIfKnown();Spliterator<Integer> all=s.trySplit();h=31*h+(int)all.estimateSize();h=31*h+(int)s.estimateSize();all.forEachRemaining(x->sum[0]+=x);h=31*h+sum[0];s=Spliterators.spliterator(c,Spliterator.CONCURRENT);h=31*h+s.characteristics();try{s.tryAdvance(null);throw new Error();}catch(NullPointerException e){h++;}return h;}}
""");compare(1);}
 @Test void sequentialSupport() throws Exception {fixture("""
import java.util.*;import java.util.stream.*;import java.util.function.*;public class Metadata {public static int exclusions(){try{StreamSupport.stream(Spliterators.emptySpliterator(),true);return 0;}catch(UnsupportedOperationException e){return 1;}}public static int run(int g){int h=1;int[] calls={0},closed={0};Supplier<Spliterator<Integer>> source=()->{calls[0]++;return Spliterators.spliterator(new Integer[]{1,2,3,4},Spliterator.ORDERED);};Stream<Integer> stream=StreamSupport.stream(source,Spliterator.ORDERED|Spliterator.SIZED,false).onClose(()->closed[0]++);h=31*h+calls[0];h=31*h+stream.filter(x->x%2==0).map(x->x*3).toList().hashCode();h=31*h+calls[0];stream.close();stream.close();h=31*h+closed[0];Stream<Integer> fresh=StreamSupport.stream(source,Spliterator.ORDERED,false);Iterator<Integer> it=fresh.iterator();h=31*h+calls[0];h=31*h+it.next();h=31*h+calls[0];fresh.close();h=31*h+StreamSupport.stream(Spliterators.spliterator(new Integer[]{9,8},0),false).toList().hashCode();try{StreamSupport.stream((Spliterator<Object>)null,false);throw new Error();}catch(NullPointerException e){h++;}try{StreamSupport.stream((Supplier<Spliterator<Object>>)null,0,false);throw new Error();}catch(NullPointerException e){h++;}return h;}}
""");compare(1);}
 @Test void sourceStreamTraversal() throws Exception {fixture("""
import java.util.*;import java.util.stream.*;public class Metadata {public static int exclusions(){try{Stream.of(1,2).map(x->x+1).spliterator();return 0;}catch(UnsupportedOperationException e){return 1;}}public static int run(int g){int h=1;int[] calls={0};Stream<Integer> stream=StreamSupport.stream(()->{calls[0]++;return Spliterators.spliterator(new Integer[]{1,2,3},Spliterator.ORDERED);},Spliterator.ORDERED,false);Spliterator<Integer> s=stream.spliterator();h=31*h+calls[0];h=31*h+s.characteristics();h=31*h+calls[0];int[] sum={0};s.tryAdvance(x->sum[0]+=x);s.forEachRemaining(x->sum[0]+=x);h=31*h+sum[0];h=31*h+calls[0];try{stream.count();throw new Error();}catch(IllegalStateException e){h++;}Spliterator<Integer> original=Spliterators.spliterator(new Integer[]{5},0);Stream<Integer> direct=StreamSupport.stream(original,false);h=31*h+(direct.spliterator()==original?1:0);Stream<String> array=Stream.of("a","b","c","d");s=null;Spliterator<String> a=array.spliterator();h=31*h+a.characteristics();h=31*h+(int)a.getExactSizeIfKnown();Spliterator<String> prefix=a.trySplit();h=31*h+(int)prefix.estimateSize();StringBuilder out=new StringBuilder();prefix.forEachRemaining(out::append);a.forEachRemaining(out::append);h=31*h+out.toString().hashCode();Stream<Integer> closed=Stream.of(1,2);closed.close();try{closed.spliterator();throw new Error();}catch(IllegalStateException e){h++;}return h;}}
""");compare(1);}
 @Test void arraysAndSortedMetadata() throws Exception {fixture("""
import java.util.*;public class Metadata {public static int run(int g){int h=1;String[] a={"a","b","c","d"};Spliterator<String> s=Arrays.spliterator(a,1,4);h=31*h+s.characteristics();h=31*h+(int)s.getExactSizeIfKnown();List<Integer> list=new ArrayList<>(Arrays.asList(1,2,3,4));Spliterator<Integer> p=list.spliterator();list.add(5);h=31*h+(int)p.estimateSize();Spliterator<Integer> prefix=p.trySplit();h=31*h+(int)prefix.estimateSize();int[] sum={0};prefix.forEachRemaining(x->sum[0]+=x);p.forEachRemaining(x->sum[0]+=x);h=31*h+sum[0];p=list.spliterator();p.estimateSize();list.add(6);try{p.tryAdvance(x->sum[0]+=x);throw new Error();}catch(ConcurrentModificationException e){h++;}h=31*h+sum[0];for(Comparator<String> cmp:Arrays.<Comparator<String>>asList(null,Comparator.reverseOrder())){TreeSet<String> set=new TreeSet<>(cmp);set.addAll(Arrays.asList("a","b","c"));Spliterator<String> sp=set.spliterator();h=31*h+sp.characteristics();h=31*h+(int)sp.getExactSizeIfKnown();h=31*h+(sp.getComparator()==cmp?1:0);StringBuilder out=new StringBuilder();sp.forEachRemaining(out::append);h=31*h+out.toString().hashCode();SortedSet<String> readonly=Collections.unmodifiableSortedSet(set);sp=readonly.spliterator();h=31*h+sp.characteristics();h=31*h+(sp.getComparator()==cmp?1:0);}return h;}}
""");compare(1);}
 @Test void libraryAndAdmission() throws Exception {fixture("""
import java.util.*;import java.util.function.*;import java.util.stream.*;public class Metadata {public static int run(int g){int h=1;h=31*h+Stream.empty().spliterator().characteristics();h=31*h+Stream.ofNullable(null).spliterator().characteristics();Collection<Integer> custom=new AbstractCollection<Integer>(){public int size(){return 2;}public Iterator<Integer> iterator(){throw new Error();}public Spliterator<Integer> spliterator(){return Arrays.spliterator(new Integer[]{7,8});}};h=31*h+custom.stream().toList().hashCode();List<String> values=new ArrayList<>(Arrays.asList("a","b","c"));Spliterator<String> s=values.spliterator();Function<Spliterator<String>,Iterator<String>> adapt=Spliterators::iterator;Iterator<String> it=adapt.apply(s);while(it.hasNext())h=31*h+it.next().hashCode();for(int mode=0;mode<2;mode++){TreeSet<Integer> set=new TreeSet<>(Arrays.asList(1));Spliterator<Integer> sp=set.spliterator();sp.estimateSize();try{if(mode==0)sp.tryAdvance(x->set.add(2));else sp.forEachRemaining(x->set.add(2));throw new Error();}catch(ConcurrentModificationException e){h++;}}return h;}}
""");compare(1);springTraversal();traversalAdmission();}
    private void traversalAdmission(){
        String[][] signatures={
          {"java/util/Collection","toArray","(Ljava/util/function/IntFunction;)[Ljava/lang/Object;","interface"},
          {"java/util/Spliterator","tryAdvance","(Ljava/util/function/Consumer;)Z","interface"},
          {"java/util/Spliterator","trySplit","()Ljava/util/Spliterator;","interface"},
          {"java/util/Spliterator","getComparator","()Ljava/util/Comparator;","interface"},
          {"java/util/Spliterators","iterator","(Ljava/util/Spliterator;)Ljava/util/Iterator;","static"},
          {"java/util/stream/StreamSupport","stream","(Ljava/util/Spliterator;Z)Ljava/util/stream/Stream;","static"},
          {"java/util/stream/Stream","spliterator","()Ljava/util/Spliterator;","interface"},
          {"java/lang/Iterable","spliterator","()Ljava/util/Spliterator;","interface"}
        };
        for(String[] call:signatures)for(int variant=0;variant<4;variant++){
            boolean iface=call[3].equals("interface"),statik=call[3].equals("static");
            String descriptor=variant%2==0?"(J)Ljava/lang/Object;":call[2];
            ClassWriter writer=new ClassWriter(0);writer.visit(Opcodes.V17,Opcodes.ACC_PUBLIC,"BadTraversalCall",null,"java/lang/Object",null);writer.visitSource("TraversalCalls.java",null);
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
            assertTrue(failure.getMessage().contains("TraversalCalls.java:61"),failure.getMessage());
        }
        for(String[] api:new String[][]{{"java/util/Spliterators","emptyIntSpliterator","()Ljava/util/Spliterator$OfInt;"},{"java/util/stream/StreamSupport","intStream","(Ljava/util/Spliterator$OfInt;Z)Ljava/util/stream/IntStream;"}}){
            ClassWriter w=new ClassWriter(0);w.visit(Opcodes.V17,Opcodes.ACC_PUBLIC,"UnsupportedTraversalApi",null,"java/lang/Object",null);w.visitSource("TraversalCalls.java",null);MethodVisitor m=w.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"unused","()Ljava/lang/Object;",null,null);m.visitCode();Label line=new Label();m.visitLabel(line);m.visitLineNumber(61,line);m.visitInsn(Opcodes.ACONST_NULL);m.visitInsn(Opcodes.ICONST_0);m.visitMethodInsn(Opcodes.INVOKESTATIC,api[0],api[1],api[2],false);m.visitInsn(Opcodes.ARETURN);m.visitMaxs(2,0);m.visitEnd();w.visitEnd();var failure=assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileClassWithExceptionRuntime(w.toByteArray()));assertTrue(failure.getMessage().contains("unused"),failure.getMessage());assertTrue(failure.getMessage().contains("TraversalCalls.java:61"),failure.getMessage());
        }
    }
    private void springTraversal() throws Exception {
        String artifact="org/springframework/data/spring-data-commons/3.2.4/spring-data-commons-3.2.4.jar",hash="35d95442ab476e46d9ac4d60074f9b9c30b18c106fd1ec71d1d68c00d8080f6d";
        ClassNode selected=selectedLibrary("org/springframework/data/util/StreamUtils.class",artifact,hash,name->name.equals("createStreamFromIterator")||name.equals("zip"));selected.methods.removeIf(m->m.name.equals("createStreamFromIterator")&&!m.desc.equals("(Ljava/util/Iterator;)Ljava/util/stream/Stream;"));carrier(selected);
        for(String relative:new String[]{"org/springframework/data/util/StreamUtils$1.class","org/springframework/data/util/Sink.class"}){
            Path jar=Path.of(System.getProperty("user.home"),".m2/repository",artifact);try(JarFile input=new JarFile(jar.toFile())){Path target=directory.resolve(relative);Files.createDirectories(target.getParent());Files.write(target,input.getInputStream(input.getJarEntry(relative)).readAllBytes());}
        }
        ClassNode assertion=selectedLibrary("org/springframework/util/Assert.class","org/springframework/spring-core/6.1.5/spring-core-6.1.5.jar","476dac28509b3f47d0b5e895a639fa75ac2218f074ce214e44832a836c469f7e",name->name.equals("notNull"));assertion.methods.removeIf(m->!m.desc.equals("(Ljava/lang/Object;Ljava/lang/String;)V"));carrier(assertion);
        fixtureClasspath+=File.pathSeparator+Path.of(System.getProperty("user.home"),".m2/repository",artifact);
        fixture("""
            import java.util.*;import java.util.stream.*;import java.util.function.*;import org.springframework.data.util.StreamUtils;
            public class Metadata {public static int run(int group){int h=1;Function<Iterator<Integer>,Stream<Integer>> create=StreamUtils::createStreamFromIterator;h=31*h+create.apply(Arrays.asList(1,2,3).iterator()).map(x->x*2).toList().hashCode();
             for(int length:new int[]{0,1,3,5}){Integer[] left=new Integer[length];for(int i=0;i<length;i++)left[i]=i;int[] combined={0};Stream<Integer> zipped=StreamUtils.zip(Arrays.stream(left),Stream.of(10,20,30),(a,b)->{combined[0]++;return a+b;});h=31*h+combined[0];h=31*h+zipped.toList().hashCode();h=31*h+combined[0];zipped.close();}
             try{StreamUtils.createStreamFromIterator((Iterator<Object>)null);throw new Error();}catch(NullPointerException e){h++;}
             for(int i=0;i<3;i++)try{StreamUtils.zip(i==0?null:Stream.of(1),i==1?null:Stream.of(2),i==2?null:(a,b)->a+b);throw new Error();}catch(IllegalArgumentException e){h++;}
             try{StreamUtils.zip(Stream.of(1,2),Stream.of(3,4),(a,b)->{throw new IllegalStateException();}).toList();throw new Error();}catch(IllegalStateException e){h++;}return h;}}
            """);compare(1);
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
        Files.writeString(input,wat);run(Wat2Wasm.command(input,output,Wat2Wasm.EXCEPTIONS, "--debug-names").toArray(String[]::new));
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
            """.replace("EXPECTED",expected)+"if(e['Metadata.exclusions'])assert.equal(e['Metadata.exclusions'](),1);\nassert.equal(e['"+fixtureClassName+".run'](group),expected[group],'batch '+group);\n}");
        run("node",script.toString(),output.toString());
    }
}
