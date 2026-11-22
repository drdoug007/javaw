package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T48: ConcurrentHashMap KeySetView factories, enumerations and the remaining overrides (single-threaded contract). Expected values come from the JVM; see tasks/T48-concurrent-hash-map-key-set.md. */
class BytecodeConcurrentHashMapKeySetTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.util.ArrayList;
        import java.util.Collection;
        import java.util.Enumeration;
        import java.util.Iterator;
        import java.util.List;
        import java.util.Map;
        import java.util.Set;
        import java.util.TreeMap;
        import java.util.concurrent.ConcurrentHashMap;

        /** Like a library subclass: logs the overridable calls the map makes on itself. */
        class Spy<K,V> extends ConcurrentHashMap<K,V> {
            static final StringBuilder log=new StringBuilder();
            public V get(Object k){log.append("get:").append(k).append(' ');return super.get(k);}
            public V put(K k,V v){log.append("put:").append(k).append(' ');return super.put(k,v);}
            public V remove(Object k){log.append("remove:").append(k).append(' ');return super.remove(k);}
            public boolean remove(Object k,Object v){log.append("remove2:").append(k).append(' ');return super.remove(k,v);}
            public boolean replace(K k,V o,V n){log.append("replace3:").append(k).append(' ');return super.replace(k,o,n);}
            public boolean containsKey(Object k){log.append("containsKey:").append(k).append(' ');return super.containsKey(k);}
            public boolean containsValue(Object v){log.append("containsValue:").append(v).append(' ');return super.containsValue(v);}
            public V putIfAbsent(K k,V v){log.append("putIfAbsent:").append(k).append(' ');return super.putIfAbsent(k,v);}
            public int size(){log.append("size ");return super.size();}
            public boolean isEmpty(){log.append("isEmpty ");return super.isEmpty();}
            public Set<Map.Entry<K,V>> entrySet(){log.append("entrySet ");return super.entrySet();}
            public KeySetView<K,V> keySet(){log.append("keySet ");return super.keySet();}
            public Collection<V> values(){log.append("values ");return super.values();}
        }

        public class Fixture {
            interface Op{Object run() throws Exception;}
            static String t(Op op){
                try{return String.valueOf(op.run());}catch(Exception e){return e.getClass().getSimpleName()+":"+e.getMessage();}
            }
            static String ty(Op op){
                try{return String.valueOf(op.run());}catch(Exception e){return e.getClass().getSimpleName();}
            }
            static String s(Map<?,?> m){return new TreeMap<>(m).toString();}
            static String s(Collection<?> c){List<String> l=new ArrayList<>();for(Object o:c)l.add(String.valueOf(o));java.util.Collections.sort(l);return l.toString();}
            /** Map with Integer keys 0..15 (these iterate in ascending key order). */
            static ConcurrentHashMap<Integer,String> m(int... keys){ConcurrentHashMap<Integer,String> m=new ConcurrentHashMap<>();for(int k:keys)m.put(k,"v"+k);return m;}
            static String all(Enumeration<?> e){StringBuilder o=new StringBuilder();while(e.hasMoreElements())o.append(e.nextElement()).append(' ');return o.append(t(()->e.nextElement())).toString();}

            public static String newKeySets(){
                ConcurrentHashMap.KeySetView<String,Boolean> k=ConcurrentHashMap.newKeySet();
                String a=k.isEmpty()+","+k.add("a")+","+k.add("b")+","+k.add("a")+","+k.size()+","+k.contains("a")+","+k.contains("z")+","+k.getMappedValue()+","+s(k)+","+s(k.getMap())+","+ty(()->k.add(null))+","+ty(()->k.contains(null));
                a+="|"+k.addAll(List.of("b","c"))+","+k.addAll(List.of("a"))+","+k.addAll(List.of())+","+s(k)+","+k.remove("b")+","+k.remove("b")+","+s(k)+","+ty(()->k.addAll(null));
                ConcurrentHashMap.KeySetView<Integer,Boolean> n=ConcurrentHashMap.newKeySet(4);
                for(int i=0;i<30;i++)n.add(i%10);
                a+="|"+n.size()+","+n+","+n.removeIf(x->x%3==0)+","+n+","+n.removeIf(x->false)+","+ty(()->n.removeIf(null))+","+ty(()->ConcurrentHashMap.newKeySet(-1));
                Integer[] arr=n.toArray(new Integer[0]);Object[] o=n.toArray();
                a+="|"+arr.length+":"+arr[0]+":"+arr[5]+","+o.length+","+n.getMap().size()+","+n.getMap().get(4)+","+n.getMap().getClass().getSimpleName();
                Object w=k;
                Iterator<Integer> it=n.iterator();it.next();it.remove();
                return a+"|"+(w instanceof Set)+","+n+","+n.getMap()+","+k.equals(Set.of("a","c"))+","+k.hashCode();
            }
            public static String mappedKeySets(){
                ConcurrentHashMap<String,Integer> m=new ConcurrentHashMap<>();m.put("x",1);
                ConcurrentHashMap.KeySetView<String,Integer> k=m.keySet(7);
                String a=k.getMappedValue()+","+k.add("y")+","+k.add("x")+","+s(m)+","+k.addAll(List.of("x","z"))+","+s(m)+","+(k.getMap()==m)+","+(k==m.keySet(7))+","+(m.keySet()==m.keySet())+","+m.keySet().getMappedValue();
                a+="|"+ty(()->m.keySet(null))+","+t(()->m.keySet().add("q"))+","+t(()->m.keySet().addAll(List.of()))+","+k.remove("y")+","+s(m)+","+s(k)+","+k.size();
                k.clear();
                return a+"|"+m+","+k.isEmpty()+","+k.add("w")+","+m;
            }
            public static String legacyAndCounts(){
                ConcurrentHashMap<Integer,String> m=m(2,4,6);
                Enumeration<Integer> ke=m.keys();Enumeration<String> ve=m.elements();Object w=ke;
                String a=all(ke)+"|"+all(ve)+"|"+(w instanceof Iterator)+","+all(m(0).keys())+","+all(new ConcurrentHashMap<String,String>().elements());
                Enumeration<Integer> e=m.keys();
                a+="|"+e.nextElement()+","+m.remove(4)+","+m.put(5,"v5")+","+e.nextElement()+","+e.nextElement()+","+e.hasMoreElements();
                a+="|"+m.mappingCount()+","+new ConcurrentHashMap<String,String>().mappingCount()+","+m.contains("v5")+","+m.contains("v4")+","+ty(()->m.contains(null));
                ConcurrentHashMap<String,Integer> big=new ConcurrentHashMap<>();for(int i=0;i<50;i++)big.put("k"+i,i);
                return a+"|"+big.mappingCount()+","+big.size()+","+(big.mappingCount()==big.size());
            }
            public static String defaultsAndForEach(){
                ConcurrentHashMap<String,String> m=new ConcurrentHashMap<>();m.put("a","1");
                String a=m.getOrDefault("a","d")+","+m.getOrDefault("b","d")+","+m.getOrDefault("b",null)+","+ty(()->m.getOrDefault(null,"d"));
                ConcurrentHashMap<Integer,String> n=m(1,2,3);StringBuilder o=new StringBuilder();
                n.forEach((k,v)->o.append(k).append('=').append(v).append(' '));
                a+="|"+o+ty(()->{n.forEach((java.util.function.BiConsumer<Integer,String>)null);return "x";});o.setLength(0);
                n.forEach((k,v)->{o.append(k);if(k==1){n.remove(2);n.put(5,"v5");n.put(0,"v0");}});
                a+="|"+o+","+n;o.setLength(0);
                n.keySet().forEach(k->o.append(k));n.values().forEach(v->o.append(v));n.entrySet().forEach(x->o.append(x));
                a+="|"+o+","+ty(()->{n.keySet().forEach(null);return "x";})+","+ty(()->{n.values().forEach(null);return "x";})+","+ty(()->{n.entrySet().forEach(null);return "x";});
                n.entrySet().forEach(x->{if(x.getKey()==3)x.setValue("set3");});
                a+="|"+n+","+t(()->{n.entrySet().forEach(x->x.setValue(null));return "x";})+","+n;
                return a;
            }
            public static String replaceAllAndRemoveIf(){
                ConcurrentHashMap<Integer,String> m=m(1,2,3);StringBuilder o=new StringBuilder();
                m.replaceAll((k,v)->{o.append(k).append(v).append(' ');return v+"!";});
                String a=o+","+m+","+ty(()->{m.replaceAll(null);return "x";});
                a+="|"+ty(()->{m.replaceAll((k,v)->k==2?null:v+"?");return "x";})+","+m;
                ConcurrentHashMap<Integer,String> e=new ConcurrentHashMap<>();e.replaceAll((k,v)->{throw new IllegalStateException();});
                a+=","+e;
                ConcurrentHashMap<Integer,String> r=m(1,2,3,4,5);
                a+="|"+r.values().removeIf(v->v.equals("v2"))+","+r.entrySet().removeIf(x->x.getKey()>=4)+","+r+","+r.values().removeIf(v->false)+","+r.entrySet().removeIf(x->false)+","+ty(()->r.values().removeIf(null))+","+ty(()->r.entrySet().removeIf(null));
                a+="|"+t(()->r.entrySet().removeIf(x->{x.setValue("no");return true;}))+","+r+","+r.keySet().removeIf(k->k==1)+","+r;
                ConcurrentHashMap<Integer,String> c=m(1,2,3);StringBuilder seen=new StringBuilder();
                c.entrySet().removeIf(x->{seen.append(x).append(' ');if(x.getKey()==1)c.remove(3);return x.getKey()==2;});
                return a+"|"+seen+","+c;
            }
            public static String subclassCalls(){
                Spy<String,String> p=new Spy<>();p.put("a","1");p.put("b","2");p.put("c","3");Spy.log.setLength(0);
                p.getOrDefault("a","x");p.getOrDefault("z","x");String a=Spy.log+"|";Spy.log.setLength(0);
                p.contains("2");a+=Spy.log+"|";Spy.log.setLength(0);
                p.forEach((k,v)->{});p.keySet().forEach(k->{});a+=Spy.log+"|";Spy.log.setLength(0);
                p.replaceAll((k,v)->v+v);a+=Spy.log+"|";Spy.log.setLength(0);
                p.mappingCount();p.keys();p.elements();p.keySet("v").add("d");p.keySet("v").addAll(List.of("e"));a+=Spy.log+"|";Spy.log.setLength(0);
                p.values().removeIf(v->v.equals("11"));p.entrySet().removeIf(x->x.getKey().equals("b"));p.keySet().removeIf(k->k.equals("c"));a+=Spy.log+"|";Spy.log.setLength(0);
                p.keySet().toArray();p.keySet().toArray(new String[0]);
                return a+Spy.log+","+s(p);
            }
        }
        """;

    @Test void newKeySet() throws Exception {
        matchesJvm(FIXTURE, "newKeySets");
    }

    @Test void keySetWithMappedValue() throws Exception {
        matchesJvm(FIXTURE, "mappedKeySets");
    }

    @Test void enumerationsAndMappingCount() throws Exception {
        matchesJvm(FIXTURE, "legacyAndCounts");
    }

    @Test void getOrDefaultAndForEach() throws Exception {
        matchesJvm(FIXTURE, "defaultsAndForEach");
    }

    @Test void replaceAllAndViewRemoveIf() throws Exception {
        matchesJvm(FIXTURE, "replaceAllAndRemoveIf");
    }

    @Test void subclassSelfCalls() throws Exception {
        matchesJvm(FIXTURE, "subclassCalls");
    }

    @Test void leavesOutParallelBulkOperations() {
        rejectsUnusedCall("java/util/concurrent/ConcurrentHashMap", "forEach", "(JLjava/util/function/BiConsumer;)V", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/concurrent/ConcurrentHashMap", "reduceValues", "(JLjava/util/function/BiFunction;)Ljava/lang/Object;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/concurrent/ConcurrentHashMap", "search", "(JLjava/util/function/BiFunction;)Ljava/lang/Object;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/concurrent/ConcurrentHashMap", "forEachKey", "(JLjava/util/function/Consumer;)V", INVOKEVIRTUAL, false);
    }
}
