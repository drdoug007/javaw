package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T46: java.util.concurrent.ConcurrentMap default methods (single-threaded). See tasks/T46-concurrent-map.md. */
class BytecodeConcurrentMapTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.util.*;
        import java.util.concurrent.ConcurrentMap;
        import java.util.function.*;
        class LogMap implements ConcurrentMap<String,String> {
            final TreeMap<String,String> data=new TreeMap<>();
            final StringBuilder log=new StringBuilder();
            int failReplace;String sneak;boolean sneakRemove;
            int failPutIfAbsent;String racer;
            LogMap with(String k,String v){data.put(k,v);return this;}
            String take(){String s=log.toString();log.setLength(0);return s;}
            public int size(){return data.size();}
            public boolean isEmpty(){return data.isEmpty();}
            public boolean containsKey(Object k){log.append("containsKey(").append(k).append(");");return data.containsKey(k);}
            public boolean containsValue(Object v){return data.containsValue(v);}
            public String get(Object k){log.append("get(").append(k).append(");");return data.get(k);}
            public String put(String k,String v){log.append("put(").append(k).append(',').append(v).append(");");return data.put(k,v);}
            public String remove(Object k){log.append("remove(").append(k).append(");");return data.remove(k);}
            public void putAll(Map<? extends String,? extends String> m){data.putAll(m);}
            public void clear(){data.clear();}
            public Set<String> keySet(){return data.keySet();}
            public Collection<String> values(){return data.values();}
            public Set<Map.Entry<String,String>> entrySet(){log.append("entrySet;");return new TreeMap<String,String>(data).entrySet();}
            public String putIfAbsent(String k,String v){
                log.append("putIfAbsent(").append(k).append(',').append(v).append(");");
                if(failPutIfAbsent>0){failPutIfAbsent--;data.put(k,racer);return racer;}
                String old=data.get(k);if(old==null)data.put(k,v);return old;
            }
            public boolean remove(Object k,Object v){
                log.append("remove(").append(k).append(',').append(v).append(");");
                if(data.containsKey(k)&&Objects.equals(data.get(k),v)){data.remove(k);return true;}return false;
            }
            public boolean replace(String k,String o,String n){
                log.append("replace(").append(k).append(',').append(o).append(',').append(n).append(");");
                if(failReplace>0){failReplace--;if(sneakRemove)data.remove(k);else data.put(k,sneak);return false;}
                if(data.containsKey(k)&&Objects.equals(data.get(k),o)){data.put(k,n);return true;}return false;
            }
            public String replace(String k,String v){log.append("replace(").append(k).append(',').append(v).append(");");return data.containsKey(k)?data.put(k,v):null;}
            public void forEach(BiConsumer<? super String,? super String> action){log.append("forEach;");ConcurrentMap.super.forEach(action);}
        }
        class StaleEntry implements Map.Entry<String,String> {
            final String key;final boolean keyFails;
            StaleEntry(String key,boolean keyFails){this.key=key;this.keyFails=keyFails;}
            public String getKey(){if(keyFails)throw new IllegalStateException("gone key");return key;}
            public String getValue(){if(!keyFails)throw new IllegalStateException("gone value");return "x";}
            public String setValue(String v){throw new UnsupportedOperationException();}
        }
        class StaleMap extends LogMap {
            public Set<Map.Entry<String,String>> entrySet(){
                log.append("entrySet;");
                final List<Map.Entry<String,String>> list=new ArrayList<>();
                list.add(new AbstractMap.SimpleEntry<String,String>("a","1"));
                list.add(new StaleEntry("b",true));
                list.add(new AbstractMap.SimpleEntry<String,String>("c","3"));
                list.add(new StaleEntry("d",false));
                list.add(new AbstractMap.SimpleEntry<String,String>("e","5"));
                return new AbstractSet<Map.Entry<String,String>>(){
                    public Iterator<Map.Entry<String,String>> iterator(){return list.iterator();}
                    public int size(){return list.size();}
                };
            }
        }
        public class Fixture {
            static String name(Throwable t){return t.getClass().getName();}

            public static String getOrDefaultBasics(){
                LogMap m=new LogMap().with("a","1").with("n",null);StringBuilder out=new StringBuilder();
                out.append(m.getOrDefault("a","d")).append(',').append(m.take()).append('|');
                out.append(m.getOrDefault("z","d")).append(',').append(m.take()).append('|');
                out.append(m.getOrDefault("n","d")).append(',').append(m.take()).append('|');
                out.append(m.getOrDefault("z",null)).append(',').append(m.take()).append('|');
                ConcurrentMap<String,String> cm=m;
                out.append(cm.getOrDefault("a","q")).append(',').append(m.take()).append('|');
                Map<String,String> plain=m;
                out.append(plain.getOrDefault("n","p")).append(',').append(m.take()).append('|');
                out.append((Object)m instanceof Map);
                return out.toString();
            }
            public static String forEachBasics(){
                LogMap m=new LogMap().with("c","3").with("a","1").with("b","2");StringBuilder out=new StringBuilder();
                StringBuilder seen=new StringBuilder();
                m.forEach((k,v)->seen.append(k).append('=').append(v).append(';'));
                out.append(seen).append('|').append(m.take()).append('|');
                new LogMap().forEach((k,v)->seen.append("never"));out.append(seen.length()).append('|');
                try{m.forEach(null);out.append("no");}catch(NullPointerException e){out.append(name(e)).append(':').append(e.getMessage());}
                out.append('|').append(m.take());
                return out.toString();
            }
            public static String forEachSkipsStaleEntries(){
                StaleMap m=new StaleMap();StringBuilder out=new StringBuilder();
                StringBuilder seen=new StringBuilder();
                m.forEach((k,v)->seen.append(k).append('=').append(v).append(';'));
                out.append(seen).append('|').append(m.take()).append('|');
                int[] calls={0};
                try{m.forEach((k,v)->{calls[0]++;if(k.equals("c"))throw new IllegalStateException("from action");});out.append("no");}
                catch(IllegalStateException e){out.append(name(e)).append(':').append(e.getMessage());}
                out.append(',').append(calls[0]);
                return out.toString();
            }
            public static String replaceAllBasics(){
                LogMap m=new LogMap().with("b","2").with("a","1").with("c","3");StringBuilder out=new StringBuilder();
                m.replaceAll((k,v)->k+v);
                out.append(m.data).append('|').append(m.take()).append('|');
                new LogMap().replaceAll((k,v)->"never");
                try{m.replaceAll(null);out.append("no");}catch(NullPointerException e){out.append(name(e)).append(':').append(e.getMessage());}
                out.append('|').append(m.take()).append('|');
                LogMap e=new LogMap();e.replaceAll((k,v)->"x");out.append(e.take());
                return out.toString();
            }
            public static String replaceAllFunctionFailure(){
                LogMap m=new LogMap().with("a","1").with("b","2").with("c","3");StringBuilder out=new StringBuilder();
                try{m.replaceAll((k,v)->{if(k.equals("b"))throw new IllegalArgumentException("bad "+k);return v+v;});out.append("no");}
                catch(IllegalArgumentException e){out.append(name(e)).append(':').append(e.getMessage());}
                out.append('|').append(m.data).append('|').append(m.take());
                return out.toString();
            }
            public static String computeIfAbsentBasics(){
                LogMap m=new LogMap().with("a","1").with("n",null);StringBuilder out=new StringBuilder();
                        Function<String,String> f=k->{m.log.append("apply(").append(k).append(");");return k.equals("none")?null:k.toUpperCase();};
                out.append(m.computeIfAbsent("a",f)).append(',').append(m.take()).append('|');
                out.append(m.computeIfAbsent("b",f)).append(',').append(m.take()).append('|');
                out.append(m.computeIfAbsent("none",f)).append(',').append(m.take()).append('|');
                out.append(m.computeIfAbsent("n",f)).append(',').append(m.take()).append('|');
                out.append(m.data).append('|');
                try{m.computeIfAbsent("q",null);out.append("no");}catch(NullPointerException e){out.append(name(e)).append(':').append(e.getMessage());}
                out.append(',').append(m.take()).append('|');
                try{m.computeIfAbsent("q",k->{throw new IllegalStateException("boom "+k);});out.append("no");}
                catch(IllegalStateException e){out.append(name(e)).append(':').append(e.getMessage());}
                out.append(',').append(m.take()).append(',').append(m.data.containsKey("q"));
                return out.toString();
            }
            public static String computeIfAbsentLosesRace(){
                LogMap m=new LogMap();m.failPutIfAbsent=1;m.racer="R";StringBuilder out=new StringBuilder();
                out.append(m.computeIfAbsent("k",k->"mine")).append(',').append(m.take()).append(',').append(m.data);
                return out.toString();
            }
            public static String computeIfPresentBasics(){
                LogMap m=new LogMap().with("a","1").with("b","2").with("n",null);StringBuilder out=new StringBuilder();
                        BiFunction<String,String,String> f=(k,v)->{m.log.append("apply(").append(k).append(',').append(v).append(");");return k.equals("b")?null:v+"!";};
                out.append(m.computeIfPresent("a",f)).append(',').append(m.take()).append('|');
                out.append(m.computeIfPresent("b",f)).append(',').append(m.take()).append('|');
                out.append(m.computeIfPresent("z",f)).append(',').append(m.take()).append('|');
                out.append(m.computeIfPresent("n",f)).append(',').append(m.take()).append('|');
                out.append(m.data).append('|');
                try{m.computeIfPresent("z",null);out.append("no");}catch(NullPointerException e){out.append(name(e)).append(':').append(e.getMessage());}
                out.append(',').append(m.take());
                return out.toString();
            }
            public static String computeBasics(){
                LogMap m=new LogMap().with("a","1").with("b","2");StringBuilder out=new StringBuilder();
                        BiFunction<String,String,String> f=(k,v)->{m.log.append("apply(").append(k).append(',').append(v).append(");");return k.startsWith("x")?null:k+v;};
                out.append(m.compute("a",f)).append(',').append(m.take()).append('|');
                out.append(m.compute("c",f)).append(',').append(m.take()).append('|');
                out.append(m.compute("x1",f)).append(',').append(m.take()).append('|');
                m.with("x2","9");
                out.append(m.compute("x2",f)).append(',').append(m.take()).append('|');
                out.append(m.data).append('|');
                try{m.compute("a",null);out.append("no");}catch(NullPointerException e){out.append(name(e));}
                out.append(',').append(m.take()).append(',').append(m.data.get("a"));
                return out.toString();
            }
            public static String mergeBasics(){
                LogMap m=new LogMap().with("a","1").with("b","2");StringBuilder out=new StringBuilder();
                        BiFunction<String,String,String> f=(o,v)->{m.log.append("apply(").append(o).append(',').append(v).append(");");return v.equals("drop")?null:o+v;};
                out.append(m.merge("a","+",f)).append(',').append(m.take()).append('|');
                out.append(m.merge("c","new",f)).append(',').append(m.take()).append('|');
                out.append(m.merge("b","drop",f)).append(',').append(m.take()).append('|');
                out.append(m.data).append('|');
                try{m.merge("a",null,null);out.append("no");}catch(NullPointerException e){out.append(name(e)).append(':').append(e.getMessage());}
                out.append(',').append(m.take()).append('|');
                try{m.merge("a",null,f);out.append("no");}catch(NullPointerException e){out.append(name(e)).append(':').append(e.getMessage());}
                out.append(',').append(m.take()).append('|');
                try{m.merge("zz","v",null);out.append("no");}catch(NullPointerException e){out.append(name(e)).append(':').append(e.getMessage());}
                out.append(',').append(m.take()).append(',').append(m.data.containsKey("zz"));
                return out.toString();
            }
            public static String replaceAllRetries(){
                StringBuilder out=new StringBuilder();
                LogMap m=new LogMap().with("a","1").with("b","2");m.failReplace=1;m.sneak="7";
                m.replaceAll((k,v)->v+"*");
                out.append(m.data).append('|').append(m.take()).append('|');
                LogMap g=new LogMap().with("a","1").with("b","2");g.failReplace=1;g.sneakRemove=true;
                g.replaceAll((k,v)->v+"*");
                out.append(g.data).append('|').append(g.take());
                return out.toString();
            }
            public static String computeIfPresentRetries(){
                StringBuilder out=new StringBuilder();
                LogMap m=new LogMap().with("k","1");m.failReplace=2;m.sneak="5";
                out.append(m.computeIfPresent("k",(k,v)->v+"+")).append(',').append(m.data).append(',').append(m.take()).append('|');
                LogMap g=new LogMap().with("k","1");g.failReplace=1;g.sneakRemove=true;
                out.append(g.computeIfPresent("k",(k,v)->v+"+")).append(',').append(g.data).append(',').append(g.take());
                return out.toString();
            }
            public static String computeRetries(){
                StringBuilder out=new StringBuilder();
                LogMap m=new LogMap();m.failPutIfAbsent=1;m.racer="R";
                out.append(m.compute("k",(k,v)->v+"+")).append(',').append(m.data).append(',').append(m.take()).append('|');
                LogMap g=new LogMap().with("k","1");g.failReplace=1;g.sneak="2";
                out.append(g.compute("k",(k,v)->v+"+")).append(',').append(g.data).append(',').append(g.take()).append('|');
                LogMap h=new LogMap().with("k","1");h.failReplace=1;h.sneakRemove=true;
                out.append(h.compute("k",(k,v)->v+"+")).append(',').append(h.data).append(',').append(h.take());
                return out.toString();
            }
            public static String mergeRetries(){
                StringBuilder out=new StringBuilder();
                LogMap m=new LogMap();m.failPutIfAbsent=1;m.racer="R";
                out.append(m.merge("k","v",(o,v)->o+v)).append(',').append(m.data).append(',').append(m.take()).append('|');
                LogMap g=new LogMap().with("k","1");g.failReplace=1;g.sneakRemove=true;
                out.append(g.merge("k","v",(o,v)->o+v)).append(',').append(g.data).append(',').append(g.take());
                return out.toString();
            }
        }
        """;

    @Test void getOrDefaultAndForEach() throws Exception {
        matchesJvm(FIXTURE, "getOrDefaultBasics", "forEachBasics", "forEachSkipsStaleEntries");
    }

    @Test void replaceAll() throws Exception {
        matchesJvm(FIXTURE, "replaceAllBasics", "replaceAllFunctionFailure", "replaceAllRetries");
    }

    @Test void computeIfAbsentAndPresent() throws Exception {
        matchesJvm(FIXTURE, "computeIfAbsentBasics", "computeIfAbsentLosesRace", "computeIfPresentBasics", "computeIfPresentRetries");
    }

    @Test void computeAndMerge() throws Exception {
        matchesJvm(FIXTURE, "computeBasics", "mergeBasics", "computeRetries", "mergeRetries");
    }

    @Test void leavesOutConcurrentHashMapAndNavigableMap() {
        // ConcurrentHashMap itself is linked by T47; its parallel bulk operations stay out of the runtime.
        rejectsUnusedCall("java/util/concurrent/ConcurrentHashMap", "forEach", "(JLjava/util/function/BiConsumer;)V", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/concurrent/ConcurrentNavigableMap", "keySet", "()Ljava/util/NavigableSet;", INVOKEINTERFACE, true);
    }
}
