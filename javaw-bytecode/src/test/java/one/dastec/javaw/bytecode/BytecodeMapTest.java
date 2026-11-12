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
import java.util.jar.*;

import static org.junit.jupiter.api.Assertions.*;

class BytecodeMapTest implements Opcodes {
    @TempDir Path directory;
    private String release="17";
    private String fixtureClasspath;

    private byte[] fixture(String source) throws Exception {
        Path file=directory.resolve("Metadata.java");Files.writeString(file,source);
        List<String> options=new ArrayList<>(List.of("-g","-parameters","--release",release,"-d",directory.toString()));
        if(fixtureClasspath!=null)options.addAll(List.of("-classpath",fixtureClasspath));
        options.add(file.toString());
        assertEquals(0,ToolProvider.getSystemJavaCompiler().run(null,null,null,options.toArray(String[]::new)));
        return jar();
    }

    private byte[] jar() throws Exception {
        List<Path> files;
        try(var paths=Files.walk(directory)) {
            files=new ArrayList<>(paths.filter(path->path.toString().endsWith(".class")).sorted().toList());
        }
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(JarOutputStream jar=new JarOutputStream(bytes)) {
            for(Path file:files){jar.putNextEntry(new JarEntry(directory.relativize(file).toString()));jar.write(Files.readAllBytes(file));jar.closeEntry();}
        }
        return bytes.toByteArray();
    }

    @Test void preservesMapViewsCallbacksOrderClonesAndCollisions() throws Exception {
        fixture("""
            import java.util.*;
            public class Metadata {
                static class Key {int id;Key(int id){this.id=id;}public int hashCode(){return 1;}public boolean equals(Object other){return other instanceof Key && id==((Key)other).id;}}
                static class Lru extends LinkedHashMap<String,Integer>{Lru(){super(2,.75f,true);}protected boolean removeEldestEntry(Map.Entry<String,Integer> entry){return size()>2;}}
                static class Hooks extends HashMap<String,Integer>{int calls;public Integer put(String key,Integer value){calls++;return super.put(key,value);}}
                static class MutableKey{int hash=1;public int hashCode(){return hash;}}
                static class HashKey{int calls;public int hashCode(){return ++calls;}}
                public static int mutatedKeys(){MutableKey key=new MutableKey();HashMap<MutableKey,String> map=new HashMap<>();map.put(key,"value");key.hash=2;int score=0;if(!map.keySet().remove(key))score++;Map.Entry<MutableKey,String> entry=new AbstractMap.SimpleEntry<>(key,"value");if(!map.entrySet().contains(entry))score+=2;if(!map.entrySet().remove(entry))score+=4;if(map.size()==1 && map.values().remove("value") && map.isEmpty())score+=8;return score;}
                public static int hooks(){Hooks map=new Hooks();HashMap<String,Integer> seed=new HashMap<>();seed.put("seed",0);map.putAll(seed);map.computeIfAbsent("a",k->1);map.compute("b",(k,v)->2);map.merge("c",3,(a,b)->a+b);map.putIfAbsent("d",4);HashKey key=new HashKey();new HashMap<HashKey,Integer>().put(key,1);return map.calls*100+key.calls;}
                public static String run(){
                    LinkedHashMap<String,Integer> map=new LinkedHashMap<>(2,.75f,true);map.put("a",1);map.put("b",null);map.put("c",3);map.get("a");map.replace("b",99,7);map.computeIfAbsent("b",k->2);map.merge("c",4,(a,b)->a+b);
                    String first=map.toString();map.entrySet().iterator().next().setValue(9);map.values().remove(1);map.keySet().remove("b");
                    HashMap<Key,String> collisions=new HashMap<>();collisions.put(new Key(1),"one");collisions.put(new Key(2),"two");collisions.put(new Key(1),"new");collisions.remove(new Key(2));
                    HashMap<Key,String> cloned=(HashMap<Key,String>)collisions.clone();cloned.clear();
                    Lru lru=new Lru();lru.put("a",1);lru.put("b",2);lru.get("a");lru.put("c",3);
                    Set<String> set=new LinkedHashSet<>(Arrays.asList("a","b","a",null));set.remove("b");set.add("b");
                    return first+"/"+map+"/"+collisions.get(new Key(1))+"/"+collisions.size()+"/"+cloned.size()+"/"+lru+"/"+set+"/"+set.equals(new HashSet<>(set));
                }
                public static int contracts(){
                    int score=0;
                    Object immutable=new AbstractMap.SimpleImmutableEntry<>("a",1);
                    if(!(immutable instanceof AbstractMap.SimpleEntry))score++;
                    if(Map.Entry.<String,Integer>comparingByKey() instanceof java.io.Serializable)score+=2;
                    if(Map.Entry.<String,Integer>comparingByValue() instanceof java.io.Serializable)score+=4;
                    Set<String> rejecting=new AbstractSet<String>(){
                        public int size(){return 1;}
                        public Iterator<String> iterator(){return Collections.singletonList("a").iterator();}
                        public boolean contains(Object value){if(value==null)throw new NullPointerException();throw new ClassCastException();}
                    };
                    if(!rejecting.equals(new HashSet<>(Collections.singletonList(null))))score+=8;
                    if(!rejecting.equals(new HashSet<>(Collections.singletonList("a"))))score+=16;
                    Set<String> set=new HashSet<>(Arrays.asList("a","b","c"));
                    if(set.removeAll(Collections.singletonList("b")) && set.size()==2)score+=32;
                    if(set.removeAll(Arrays.asList("a","c","z")) && set.isEmpty())score+=64;
                    return score;
                }
                public static int failures(){int score=0;HashMap<String,Integer> map=new HashMap<>();map.put("a",1);Iterator<String> keys=map.keySet().iterator();map.put("b",2);try{keys.next();}catch(ConcurrentModificationException e){score++;}
                    try{map.compute("a",(key,value)->{map.put("c",3);return 4;});}catch(ConcurrentModificationException e){if(map.get("a")==1 && map.get("c")==3)score+=10;}
                    try{map.merge("a",null,(a,b)->a+b);}catch(NullPointerException e){score+=100;}
                    map.put(null,null);if(map.containsKey(null) && map.getOrDefault(null,7)==null)score+=1000;
                    try{new HashMap<>(-1);}catch(IllegalArgumentException e){score+=10000;}
                    try{new HashMap<>(1,Float.NaN);}catch(IllegalArgumentException e){score+=100000;}
                    Map.Entry<String,Integer> entry=new AbstractMap.SimpleImmutableEntry<>("a",1);try{entry.setValue(2);}catch(UnsupportedOperationException e){score+=1000000;}return score;
                }
            }
            """);compare(List.of("run","failures","contracts","hooks","mutatedKeys"));
    }
    @Test void executesUnchangedPinnedAntlrOrderedHashSet() throws Exception {
        Path repository=Path.of(System.getProperty("maven.repo.local",Path.of(System.getProperty("user.home"),".m2","repository").toString()));Path artifact=repository.resolve("org/antlr/antlr4-runtime/4.13.0/antlr4-runtime-4.13.0.jar");
        assertEquals("bd7f7b5d07bc0b047f10915b32ca4bb1de9e57d8049098882e4453c88c076a5d",java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(artifact))));fixtureClasspath=artifact.toString();
        fixture("""
            import org.antlr.v4.runtime.misc.OrderedHashSet;
            public class Metadata {
                public static String run(){OrderedHashSet<String> values=new OrderedHashSet<>();values.add("b");values.add("a");values.add("b");values.add(null);values.set(1,"z");OrderedHashSet<String> copy=(OrderedHashSet<String>)values.clone();copy.add("c");return values+"/"+copy+"/"+values.get(1)+"/"+values.hashCode()+"/"+values.contains("z")+"/"+values.equals(copy);}
                public static int failures(){OrderedHashSet<String> values=new OrderedHashSet<>();values.add("a");try{values.remove((Object)"a");return 0;}catch(UnsupportedOperationException e){return values.size();}}
            }
            """);
        try(JarFile jar=new JarFile(artifact.toFile())){String name="org/antlr/v4/runtime/misc/OrderedHashSet.class";Path output=directory.resolve(name);Files.createDirectories(output.getParent());Files.write(output,jar.getInputStream(jar.getJarEntry(name)).readAllBytes());}
        compare(List.of("run","failures"));
    }

    private void compare(List<String> names) throws Exception {
        StringBuilder assertions=new StringBuilder();
        try(URLClassLoader loader=new URLClassLoader(new URL[]{directory.toUri().toURL()},null)){
            Class<?> type=loader.loadClass("Metadata");for(String name:names){Object expected=type.getMethod(name).invoke(null);assertions.append("assert.equal(").append(expected instanceof String?"text(e['Metadata.":"e['Metadata.").append(name).append(expected instanceof String?"']()),":"'](),").append(expected instanceof String?js((String)expected):expected).append(");\n");}
        }
        execute(BytecodeCompiler.compileJarWithExceptionRuntime(new ByteArrayInputStream(jar())),assertions.toString());
    }

    private String js(String value) {
        if(value==null)return "null";
        StringBuilder out=new StringBuilder("\"");
        for(char c:value.toCharArray())out.append(String.format("\\u%04x",(int)c));
        return out.append('"').toString();
    }
    private void execute(String wat,String assertions) throws Exception {
        WasmTestSupport.execute(directory,wat,assertions);
    }
}
