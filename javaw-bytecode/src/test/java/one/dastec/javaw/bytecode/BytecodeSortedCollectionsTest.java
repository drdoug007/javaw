package one.dastec.javaw.bytecode;
import one.dastec.javaw.Wat2Wasm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import javax.tools.ToolProvider;
import java.io.*;import java.nio.file.*;import java.util.*;import java.util.concurrent.TimeUnit;import java.util.jar.*;
import static org.junit.jupiter.api.Assertions.*;
class BytecodeSortedCollectionsTest {
 @TempDir Path directory;
 private final String fixtureClassName="Metadata";
 private String fixtureClasspath="";
 private final Map<String,byte[]> carriers=new HashMap<>();
 @Test void orderedMap() throws Exception {fixture("""
import java.util.*;
public class Metadata {public static int run(int group){int h=1;for(Comparator<String> cmp:Arrays.<Comparator<String>>asList(null,Comparator.reverseOrder(),String.CASE_INSENSITIVE_ORDER)){
 TreeMap<String,Integer> m=new TreeMap<>(cmp);for(String k:new String[]{"c","A","b","a","z"}){Integer old=m.put(k,k.length());h=31*h+(old==null?7:old);}
 h=31*h+m.toString().hashCode();h=31*h+m.firstKey().hashCode();h=31*h+m.lastKey().hashCode();h=31*h+m.size();m.put("b",null);h=31*h+(m.containsKey("b")?1:0);h=31*h+(m.get("b")==null?1:0);m.remove("z");
 TreeMap<String,Integer> copy=new TreeMap<>((SortedMap<String,Integer>)m);h=31*h+copy.toString().hashCode();h=31*h+(copy.comparator()==cmp?1:0);TreeMap<String,Integer> natural=new TreeMap<>((Map<String,Integer>)m);h=31*h+natural.toString().hashCode();
 }
 TreeMap<String,Integer> m=new TreeMap<>();try{m.firstKey();throw new Error();}catch(NoSuchElementException e){h++;}try{m.get(null);throw new Error();}catch(NullPointerException e){h++;}try{((Map)m).put(new Object(),1);throw new Error();}catch(ClassCastException e){h++;}return h;}}
""");compare(1);}
 @Test void mapNavigation() throws Exception {fixture("""
import java.util.*;public class Metadata {public static int run(int g){int h=1;for(Comparator<Integer> order:Arrays.<Comparator<Integer>>asList(null,Comparator.reverseOrder())){NavigableMap<Integer,String> m=new TreeMap<>(order);for(int k:new int[]{1,3,5})m.put(k,"v"+k);for(int k=0;k<=6;k++){h=31*h+Objects.hash(m.lowerKey(k),m.floorKey(k),m.ceilingKey(k),m.higherKey(k));h=31*h+Objects.hash(m.lowerEntry(k),m.floorEntry(k),m.ceilingEntry(k),m.higherEntry(k));}Map.Entry<Integer,String> old=m.firstEntry();m.put(old.getKey(),"changed");h=31*h+old.hashCode();try{old.setValue("bad");throw new Error();}catch(UnsupportedOperationException e){h++;}while(!m.isEmpty()){h=31*h+m.pollLastEntry().hashCode();}h=31*h+Objects.hash(m.firstEntry(),m.lastEntry(),m.pollFirstEntry(),m.pollLastEntry());}return h;}}
""");compare(1);}
 @Test void sortedRanges() throws Exception {fixture("""
import java.util.*;public class Metadata {public static int run(int g){int h=1;TreeMap<Integer,String> root=new TreeMap<>();for(int i=0;i<8;i++)root.put(i,"v"+i);SortedMap<Integer,String> v=root.subMap(2,6);h=31*h+v.toString().hashCode();root.put(3,"new");h=31*h+v.get(3).hashCode();v.put(4,"view");v.remove(2);h=31*h+root.toString().hashCode();h=31*h+v.headMap(5).toString().hashCode();h=31*h+v.tailMap(4).toString().hashCode();h=31*h+v.subMap(4,4).size();for(int i=0;i<4;i++)try{if(i==0)v.put(6,"bad");if(i==1)v.subMap(1,5);if(i==2)v.tailMap(6);if(i==3)root.subMap(5,3);throw new Error();}catch(IllegalArgumentException e){h++;}v.clear();h=31*h+root.toString().hashCode();return h;}}
""");compare(1);}
 @Test void descendingRanges() throws Exception {fixture("""
import java.util.*;public class Metadata {public static int run(int g){int h=1;for(boolean reverse:new boolean[]{false,true})for(boolean fi:new boolean[]{false,true})for(boolean ti:new boolean[]{false,true}){TreeMap<Integer,Integer> root=new TreeMap<>();for(int i=0;i<8;i++)root.put(i,i*i);NavigableMap<Integer,Integer> m=reverse?root.descendingMap():root;NavigableMap<Integer,Integer> v=m.subMap(reverse?6:1,fi,reverse?1:6,ti);h=31*h+v.toString().hashCode();h=31*h+v.descendingMap().toString().hashCode();h=31*h+v.headMap(4,true).toString().hashCode();h=31*h+v.tailMap(4,false).toString().hashCode();for(int k=0;k<8;k++)h=31*h+Objects.hash(v.lowerKey(k),v.floorKey(k),v.ceilingKey(k),v.higherKey(k));h=31*h+Objects.hash(v.firstEntry(),v.lastEntry(),v.pollFirstEntry(),v.pollLastEntry());h=31*h+root.toString().hashCode();h=31*h+(m.comparator()==null?7:m.comparator().compare(1,2));}NavigableMap<Integer,Integer> v=new TreeMap<Integer,Integer>().subMap(1,false,4,false);h=31*h+v.headMap(1,false).size();try{v.headMap(1,true);throw new Error();}catch(IllegalArgumentException e){h++;}return h;}}
""");compare(1);}
 @Test void backedTraversal() throws Exception {fixture("""
import java.util.*;public class Metadata {public static int run(int g){int h=1;TreeMap<String,Integer> root=new TreeMap<>(String.CASE_INSENSITIVE_ORDER);for(String k:new String[]{"a","B","c","D"})root.put(k,k.equals("c")?null:1);h=31*h+(root.entrySet().contains(new AbstractMap.SimpleEntry<>("b",1))?1:0);h=31*h+(root.entrySet().remove(new AbstractMap.SimpleEntry<>("C",null))?1:0);NavigableSet<String> keys=root.navigableKeySet();h=31*h+keys.descendingSet().toString().hashCode();h=31*h+Objects.hash(keys.floor("b"),keys.higher("b"));Iterator<Map.Entry<String,Integer>> it=root.entrySet().iterator();Map.Entry<String,Integer> entry=it.next();entry.setValue(8);root.put("B",9);h=31*h+it.next().getValue();it.remove();try{it.remove();throw new Error();}catch(IllegalStateException e){h++;}Iterator<String> stale=keys.iterator();root.put("e",5);try{stale.next();throw new Error();}catch(ConcurrentModificationException e){h++;}Iterator<String> rev=keys.descendingIterator();rev.next();rev.remove();h=31*h+root.toString().hashCode();root.values().remove(8);h=31*h+root.toString().hashCode();try{keys.add("bad");throw new Error();}catch(UnsupportedOperationException e){h++;}keys.clear();h=31*h+root.size();return h;}}
""");compare(1);}
 @Test void orderedSet() throws Exception {fixture("""
import java.util.*;public class Metadata {public static int run(int g){int h=1;for(Comparator<String> cmp:Arrays.<Comparator<String>>asList(null,String.CASE_INSENSITIVE_ORDER,Comparator.nullsFirst(Comparator.naturalOrder()))){TreeSet<String> s=new TreeSet<>(cmp);for(String v:new String[]{"b","A","a","c","b"})h=31*h+(s.add(v)?1:0);if(cmp!=null&&cmp!=String.CASE_INSENSITIVE_ORDER)s.add(null);h=31*h+s.toString().hashCode();h=31*h+Objects.hash(s.first(),s.last(),s.floor("b"),s.higher("b"));TreeSet<String> copy=(TreeSet<String>)s.clone();h=31*h+(copy.comparator()==cmp?1:0);copy.pollFirst();h=31*h+s.size();h=31*h+copy.toString().hashCode();TreeSet<String> sorted=new TreeSet<>((SortedSet<String>)s);h=31*h+sorted.toString().hashCode();while(!s.isEmpty())h=31*h+Objects.hashCode(s.pollLast());h=31*h+Objects.hash(s.pollFirst(),s.pollLast());}try{new TreeSet<String>().add(null);throw new Error();}catch(NullPointerException e){h++;}try{new TreeSet<String>().last();throw new Error();}catch(NoSuchElementException e){h++;}return h;}}
""");compare(1);}
 @Test void setRanges() throws Exception {fixture("""
import java.util.*;public class Metadata {public static int run(int g){int h=1;for(boolean desc:new boolean[]{false,true})for(boolean fi:new boolean[]{false,true})for(boolean ti:new boolean[]{false,true}){TreeSet<Integer> root=new TreeSet<>(Arrays.asList(0,1,2,3,4,5,6));NavigableSet<Integer> order=desc?root.descendingSet():root;NavigableSet<Integer> v=order.subSet(desc?5:1,fi,desc?1:5,ti);h=31*h+v.toString().hashCode();h=31*h+v.headSet(3,true).toString().hashCode();h=31*h+v.tailSet(3,false).toString().hashCode();v.remove(3);v.add(3);Iterator<Integer> it=v.descendingIterator();while(it.hasNext()){int n=it.next();h=31*h+n;if(n==3)it.remove();}h=31*h+root.toString().hashCode();TreeSet<Integer> clone=(TreeSet<Integer>)((TreeSet<Integer>)root).clone();clone.clear();h=31*h+root.size();v.clear();h=31*h+root.toString().hashCode();try{v.add(9);throw new Error();}catch(IllegalArgumentException e){h++;}}return h;}}
""");compare(1);}
 @Test void unmodifiableSorted() throws Exception {fixture("""
import java.util.*;public class Metadata {public static int run(int g){int h=1;TreeMap<Integer,String> root=new TreeMap<>();root.put(1,"a");root.put(3,"c");SortedMap<Integer,String> m=Collections.unmodifiableSortedMap(root);SortedMap<Integer,String> v=m.headMap(3);root.put(2,"b");h=31*h+v.toString().hashCode();h=31*h+Objects.hash(m.firstKey(),m.lastKey(),m.comparator());for(int i=0;i<7;i++)try{if(i==0)v.put(2,"bad");if(i==1)m.computeIfAbsent(9,k->{throw new Error();});if(i==2)m.entrySet().iterator().next().setValue("bad");if(i==3)((Map.Entry<Integer,String>)m.entrySet().toArray()[0]).setValue("bad");if(i==4)m.keySet().remove(99);if(i==5)m.values().clear();if(i==6)m.subMap(1,3).replaceAll((k,x)->{throw new Error();});throw new Error();}catch(UnsupportedOperationException e){h++;}TreeSet<Integer> set=new TreeSet<>(Arrays.asList(1,2,3));SortedSet<Integer> view=Collections.unmodifiableSortedSet(set);set.add(4);h=31*h+view.tailSet(2).toString().hashCode();for(int i=0;i<4;i++)try{if(i==0)view.headSet(3).clear();if(i==1)view.removeIf(x->{throw new Error();});if(i==2)view.iterator().remove();if(i==3)view.add(8);throw new Error();}catch(UnsupportedOperationException e){h++;}return h;}}
""");compare(1);}
 @Test void unmodifiableNavigable() throws Exception {fixture("""
import java.util.*;public class Metadata {public static int run(int g){int h=1;TreeMap<Integer,String> root=new TreeMap<>();for(int i=1;i<5;i++)root.put(i,"v"+i);NavigableMap<Integer,String> m=Collections.unmodifiableNavigableMap(root);for(int k=0;k<6;k++)h=31*h+Objects.hash(m.lowerEntry(k),m.floorEntry(k),m.ceilingEntry(k),m.higherEntry(k));h=31*h+m.descendingMap().toString().hashCode();h=31*h+m.subMap(1,false,4,true).toString().hashCode();root.put(2,"new");h=31*h+m.headMap(3,true).toString().hashCode();NavigableSet<Integer> s=Collections.unmodifiableNavigableSet(new TreeSet<>(root.keySet()));h=31*h+s.descendingSet().toString().hashCode();h=31*h+s.tailSet(2,false).toString().hashCode();for(int i=0;i<8;i++)try{if(i==0)m.firstEntry().setValue("bad");if(i==1)m.pollFirstEntry();if(i==2)m.descendingMap().pollLastEntry();if(i==3)m.navigableKeySet().pollLast();if(i==4)s.descendingIterator().remove();if(i==5)s.subSet(1,true,3,true).pollFirst();if(i==6)Collections.emptyNavigableMap().pollFirstEntry();if(i==7)Collections.emptyNavigableSet().pollLast();throw new Error();}catch(UnsupportedOperationException e){h++;}h=31*h+Collections.emptySortedMap().size();h=31*h+Collections.emptySortedSet().size();h=31*h+(Collections.emptyNavigableMap()==Collections.emptySortedMap()?1:0);h=31*h+(Collections.emptyNavigableSet()==Collections.emptySortedSet()?1:0);return h;}}
""");compare(1);}
 @Test void libraryAndAdmission() throws Exception {fixture("""
import java.util.*;public class Metadata {public static int run(int g){int h=1;TreeMap<String,List<Integer>> root=new TreeMap<>(String.CASE_INSENSITIVE_ORDER);List<Integer> values=new ArrayList<>();root.put("a",values);TreeMap<String,List<Integer>> copy=(TreeMap<String,List<Integer>>)root.clone();copy.put("b",values);h=31*h+root.size();h=31*h+(copy.comparator()==root.comparator()?1:0);h=31*h+(copy.get("A")==values?1:0);values.add(3);h=31*h+copy.get("a").get(0);NavigableMap<Integer,Integer> empty=new TreeMap<>();h=31*h+Objects.hash(empty.lowerKey(null),empty.floorKey(null),empty.ceilingKey(null),empty.higherKey(null));return h;}}
""");compare(1);springOrdering();sortedAdmission();}
    private void sortedAdmission(){
        String[][] signatures={
          {"java/util/TreeMap","put","(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;","virtual"},
          {"java/util/TreeMap","floorEntry","(Ljava/lang/Object;)Ljava/util/Map$Entry;","virtual"},
          {"java/util/TreeMap","descendingMap","()Ljava/util/NavigableMap;","virtual"},
          {"java/util/SortedMap","subMap","(Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/SortedMap;","interface"},
          {"java/util/NavigableMap","pollFirstEntry","()Ljava/util/Map$Entry;","interface"},
          {"java/util/NavigableSet","floor","(Ljava/lang/Object;)Ljava/lang/Object;","interface"},
          {"java/util/TreeSet","add","(Ljava/lang/Object;)Z","virtual"},
          {"java/util/Collections","unmodifiableNavigableMap","(Ljava/util/NavigableMap;)Ljava/util/NavigableMap;","static"}
        };
        for(String[] call:signatures)for(int variant=0;variant<4;variant++){
            boolean iface=call[3].equals("interface"),statik=call[3].equals("static");
            String descriptor=variant%2==0?"(J)Ljava/lang/Object;":call[2];
            ClassWriter writer=new ClassWriter(0);writer.visit(Opcodes.V17,Opcodes.ACC_PUBLIC,"BadSortedCall",null,"java/lang/Object",null);writer.visitSource("SortedCalls.java",null);
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
            assertTrue(failure.getMessage().contains("SortedCalls.java:61"),failure.getMessage());
        }
        for(String[] api:new String[][]{{"java/util/Collections","synchronizedSortedSet","(Ljava/util/SortedSet;)Ljava/util/SortedSet;"},{"java/util/Collections","synchronizedSortedMap","(Ljava/util/SortedMap;)Ljava/util/SortedMap;"},{"java/util/Collections","synchronizedNavigableSet","(Ljava/util/NavigableSet;)Ljava/util/NavigableSet;"}}){
            ClassWriter w=new ClassWriter(0);w.visit(Opcodes.V17,Opcodes.ACC_PUBLIC,"UnsupportedSortedApi",null,"java/lang/Object",null);w.visitSource("SortedCalls.java",null);MethodVisitor m=w.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"unused","()Ljava/lang/Object;",null,null);m.visitCode();Label line=new Label();m.visitLabel(line);m.visitLineNumber(61,line);m.visitInsn(Opcodes.ACONST_NULL);m.visitMethodInsn(api[0].endsWith("Collections")?Opcodes.INVOKESTATIC:Opcodes.INVOKEVIRTUAL,api[0],api[1],api[2],false);m.visitInsn(Opcodes.ARETURN);m.visitMaxs(2,0);m.visitEnd();w.visitEnd();
            var failure=assertThrows(IllegalArgumentException.class,()->BytecodeCompiler.compileClassWithExceptionRuntime(w.toByteArray()));assertTrue(failure.getMessage().contains("unused"),failure.getMessage());assertTrue(failure.getMessage().contains("SortedCalls.java:61"),failure.getMessage());
        }
    }
    private void springOrdering() throws Exception {
        for(String kind:new String[]{"Map","Set"}){
            String name="org/springframework/aot/generate/ValueCodeGeneratorDelegates$"+kind+"Delegate";
            ClassNode selected=selectedLibrary(name+".class","org/springframework/spring-core/6.1.5/spring-core-6.1.5.jar","476dac28509b3f47d0b5e895a639fa75ac2218f074ce214e44832a836c469f7e",method->method.equals("orderForCodeConsistency"));selected.superName="java/lang/Object";
            MethodVisitor ctor=selected.visitMethod(Opcodes.ACC_PUBLIC,"<init>","()V",null,null);ctor.visitCode();ctor.visitVarInsn(Opcodes.ALOAD,0);ctor.visitMethodInsn(Opcodes.INVOKESPECIAL,"java/lang/Object","<init>","()V",false);ctor.visitInsn(Opcodes.RETURN);ctor.visitMaxs(1,1);ctor.visitEnd();
            String descriptor="(Ljava/util/"+kind+";)Ljava/util/"+kind+";";
            MethodVisitor shim=selected.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"javawOrder",descriptor,null,null);shim.visitCode();shim.visitTypeInsn(Opcodes.NEW,name);shim.visitInsn(Opcodes.DUP);shim.visitMethodInsn(Opcodes.INVOKESPECIAL,name,"<init>","()V",false);shim.visitVarInsn(Opcodes.ALOAD,0);shim.visitMethodInsn(Opcodes.INVOKESPECIAL,name,"orderForCodeConsistency",descriptor,false);shim.visitInsn(Opcodes.ARETURN);shim.visitMaxs(2,1);shim.visitEnd();carrier(selected);
        }
        Path bridge=directory.resolve("SortedBridge.java");Files.writeString(bridge,"""
            package org.springframework.aot.generate;
            public class SortedBridge {public static Object order(int kind,Object value){try{
             Class<?> type=Class.forName("org.springframework.aot.generate.ValueCodeGeneratorDelegates$"+(kind==0?"Map":"Set")+"Delegate");var ctor=type.getDeclaredConstructor();ctor.setAccessible(true);var method=type.getDeclaredMethod("orderForCodeConsistency",kind==0?java.util.Map.class:java.util.Set.class);method.setAccessible(true);return method.invoke(ctor.newInstance(),value);
            }catch(java.lang.reflect.InvocationTargetException e){Throwable cause=e.getCause();if(cause instanceof RuntimeException)throw (RuntimeException)cause;throw (Error)cause;}catch(ReflectiveOperationException e){throw new Error(e);}}}
            """);assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,"--release","17","-d",directory.toString(),bridge.toString()));
        ClassNode direct=new ClassNode();direct.visit(Opcodes.V17,Opcodes.ACC_PUBLIC,"org/springframework/aot/generate/SortedBridge",null,"java/lang/Object",null);MethodVisitor m=direct.visitMethod(Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,"order","(ILjava/lang/Object;)Ljava/lang/Object;",null,null);m.visitCode();Label set=new Label();m.visitVarInsn(Opcodes.ILOAD,0);m.visitJumpInsn(Opcodes.IFNE,set);
        for(String kind:new String[]{"Map","Set"}){if(kind.equals("Set")){m.visitLabel(set);m.visitFrame(Opcodes.F_SAME,0,null,0,null);}m.visitVarInsn(Opcodes.ALOAD,1);m.visitTypeInsn(Opcodes.CHECKCAST,"java/util/"+kind);m.visitMethodInsn(Opcodes.INVOKESTATIC,"org/springframework/aot/generate/ValueCodeGeneratorDelegates$"+kind+"Delegate","javawOrder","(Ljava/util/"+kind+";)Ljava/util/"+kind+";",false);m.visitInsn(Opcodes.ARETURN);}m.visitMaxs(1,2);m.visitEnd();carrier(direct);
        fixture("""
            import java.util.*;import org.springframework.aot.generate.SortedBridge;
            public class Metadata {public static int run(int group){int h=1;for(boolean comparable:new boolean[]{true,false}){
             Map<Object,String> map=new LinkedHashMap<>();map.put("z","last");map.put(comparable?"a":Integer.valueOf(1),"first");Object ordered=SortedBridge.order(0,map);h=31*h+ordered.toString().hashCode();h=31*h+(ordered==map?1:0);
             Set<Object> set=new LinkedHashSet<>(map.keySet());ordered=SortedBridge.order(1,set);h=31*h+ordered.toString().hashCode();h=31*h+(ordered==set?1:0);
            }return h;}}
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
            """.replace("EXPECTED",expected)+"assert.equal(e['"+fixtureClassName+".run'](group),expected[group],'batch '+group);\n}");
        WasmTestSupport.node(script,output);
    }
}
