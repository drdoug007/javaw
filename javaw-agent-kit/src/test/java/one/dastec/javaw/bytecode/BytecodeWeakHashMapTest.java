package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** T66: java.util.WeakHashMap under the single-threaded, no-reclamation contract. Expected values come from the JVM; see tasks/T66-weak-hash-map.md. */
@Tag("agent-pending")
class BytecodeWeakHashMapTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.util.AbstractMap;
        import java.util.ArrayList;
        import java.util.Collection;
        import java.util.Collections;
        import java.util.ConcurrentModificationException;
        import java.util.HashMap;
        import java.util.Iterator;
        import java.util.List;
        import java.util.Map;
        import java.util.Set;
        import java.util.TreeMap;
        import java.util.WeakHashMap;

        /** Key with a chosen hash and an id; equal when the names match; logs every equals call as this?other. */
        class Key {
            static final StringBuilder log=new StringBuilder();
            final String n; final int h; final int id;
            Key(String n,int h,int id){this.n=n;this.h=h;this.id=id;}
            public boolean equals(Object o){log.append(n).append(id).append('?').append(o).append(' ');return o instanceof Key&&((Key)o).n.equals(n);}
            public int hashCode(){return h;}
            public String toString(){return n+id;}
        }
        /** Equal to nothing, not even itself. */
        class Never {
            public boolean equals(Object o){return false;}
            public int hashCode(){return 7;}
            public String toString(){return "never";}
        }
        /** Like a library subclass: logs the overridable calls the map makes on itself. */
        class Spy<K,V> extends WeakHashMap<K,V> {
            /** Static: the copy constructor logs before the subclass fields are set. */
            static final StringBuilder log=new StringBuilder();
            Spy(){}
            Spy(Map<? extends K,? extends V> m){super(m);}
            public V get(Object k){log.append("get:").append(k).append(' ');return super.get(k);}
            public V put(K k,V v){log.append("put:").append(k).append(' ');return super.put(k,v);}
            public V remove(Object k){log.append("remove:").append(k).append(' ');return super.remove(k);}
            public boolean containsKey(Object k){log.append("containsKey:").append(k).append(' ');return super.containsKey(k);}
            public boolean containsValue(Object v){log.append("containsValue:").append(v).append(' ');return super.containsValue(v);}
            public void putAll(Map<? extends K,? extends V> m){log.append("putAll ");super.putAll(m);}
            public int size(){log.append("size ");return super.size();}
            public boolean isEmpty(){log.append("isEmpty ");return super.isEmpty();}
            public void clear(){log.append("clear ");super.clear();}
            public Set<Map.Entry<K,V>> entrySet(){log.append("entrySet ");return super.entrySet();}
            public Set<K> keySet(){log.append("keySet ");return super.keySet();}
            public Collection<V> values(){log.append("values ");return super.values();}
        }

        public class Fixture {
            interface Op{Object run() throws Exception;}
            static String t(Op op){
                try{return String.valueOf(op.run());}catch(Exception e){return e.getClass().getSimpleName()+":"+e.getMessage();}
            }
            /** Exception type only (the JVM's helpful NullPointerException messages are not part of the contract). */
            static String ty(Op op){
                try{return String.valueOf(op.run());}catch(Exception e){return e.getClass().getSimpleName();}
            }
            /** Sorted text of a map with String (or null) keys, independent of iteration order. */
            static String s(Map<?,?> m){List<String> l=new ArrayList<>();for(Map.Entry<?,?> e:m.entrySet())l.add(e.getKey()+"="+e.getValue());Collections.sort(l);return l.toString();}
            /** Sorted text of any collection. */
            static String s(Collection<?> c){List<String> l=new ArrayList<>();for(Object o:c)l.add(String.valueOf(o));Collections.sort(l);return l.toString();}
            static WeakHashMap<String,Integer> m(String... keys){WeakHashMap<String,Integer> m=new WeakHashMap<>();int i=0;for(String k:keys)m.put(k,i++);return m;}

            public static String basics(){
                WeakHashMap<String,Integer> m=new WeakHashMap<>();
                String a=m.isEmpty()+","+m.size()+","+m.get("a")+","+m.containsKey("a")+","+m.remove("a")+","+m+","+m.containsValue(1)+","+m.containsValue(null);
                a+="|"+m.put("a",1)+","+m.put("b",2)+","+m.put("a",3)+","+m.size()+","+m.isEmpty()+","+m.get("a")+","+m.containsKey("b")+","+m.containsValue(3)+","+m.containsValue(1)+","+s(m);
                a+="|"+m.put("n",null)+","+m.containsKey("n")+","+m.get("n")+","+m.containsValue(null)+","+m.put("n",4)+","+m.containsValue(null)+","+m.size();
                a+="|"+m.remove("a")+","+m.remove("a")+","+m.size()+","+s(m);
                for(int i=0;i<200;i++)m.put("k"+i,i);
                int sum=0;for(int i=0;i<200;i++)sum+=m.get("k"+i);
                a+="|"+m.size()+","+sum+","+m.get("k157")+","+m.containsKey("k200")+","+m.remove("k3")+","+m.size()+","+m.keySet().size()+","+m.values().size()+","+m.entrySet().size();
                m.clear();
                a+="|"+m.size()+","+m.isEmpty()+","+m+","+m.get("k5")+","+m.put("z",26)+","+m+","+m.size();
                Object w=m;
                return a+"|"+(w instanceof AbstractMap)+","+(w instanceof Map)+","+(w instanceof Cloneable)+","+(w instanceof java.io.Serializable);
            }
            public static String nullKeyAndKeyIdentity(){
                WeakHashMap<String,String> m=new WeakHashMap<>();
                String a=m.get(null)+","+m.containsKey(null)+","+m.put(null,"x")+","+m.get(null)+","+m.containsKey(null)+","+m.size()+","+m+","+m.keySet()+","+m.keySet().contains(null);
                Map.Entry<String,String> e=m.entrySet().iterator().next();
                a+="|"+e.getKey()+","+e.getValue()+","+e+","+e.hashCode()+","+m.put(null,"y")+","+m.size()+","+m.remove(null)+","+m.containsKey(null)+","+m.isEmpty();
                m.put(null,null);
                a+="|"+m.containsKey(null)+","+m.get(null)+","+m.size()+","+m+","+m.getOrDefault(null,"d")+","+m.putIfAbsent(null,"p")+","+m.get(null)+","+m.keySet().remove(null)+","+m.size();
                WeakHashMap<Key,String> k=new WeakHashMap<>();
                Key a1=new Key("a",1,1);
                k.put(a1,"v1");
                a+="|"+k.put(new Key("a",1,2),"v2")+","+k+","+(k.keySet().iterator().next()==a1)+","+k.get(new Key("a",1,3))+","+k.get(new Key("a",2,4))+","+k.containsKey(new Key("a",2,5));
                Never never=new Never();
                WeakHashMap<Object,String> n=new WeakHashMap<>();n.put(never,"found");
                return a+"|"+n.get(never)+","+n.containsKey(never)+","+n.get(new Never())+","+n.remove(new Never())+","+n.remove(never)+","+n.size();
            }
            public static String equalsCalls(){
                WeakHashMap<Key,String> m=new WeakHashMap<>();
                Key a=new Key("a",1,1),b=new Key("b",1,2),c=new Key("c",2,3);
                m.put(a,"A");m.put(b,"B");m.put(c,"C");
                Key.log.setLength(0);
                String r=m.get(new Key("a",1,4))+" "+Key.log;Key.log.setLength(0);
                r+="|"+m.get(new Key("b",1,5))+" "+Key.log;Key.log.setLength(0);
                r+="|"+m.get(new Key("c",2,6))+" "+Key.log;Key.log.setLength(0);
                r+="|"+m.get(a)+","+m.get(b)+","+m.containsKey(c)+" "+Key.log;Key.log.setLength(0);
                r+="|"+m.containsKey(new Key("z",1,7))+" "+Key.log;Key.log.setLength(0);
                r+="|"+m.put(new Key("a",1,8),"A2")+","+m.get(a)+" "+Key.log;Key.log.setLength(0);
                r+="|"+m.remove(new Key("b",1,9))+","+m.size()+" "+Key.log;Key.log.setLength(0);
                r+="|"+m.get(new Key("a",1,10))+" "+Key.log;Key.log.setLength(0);
                return r;
            }
            public static String constructors(){
                String a=new WeakHashMap<String,String>(0).isEmpty()+","+new WeakHashMap<String,String>(1000).size()+","+new WeakHashMap<String,String>(4,0.5f).isEmpty()+","+new WeakHashMap<String,String>(3,7f).isEmpty();
                a+="|"+t(()->new WeakHashMap<String,String>(-1))+","+t(()->new WeakHashMap<String,String>(-5,0.75f))+","+t(()->new WeakHashMap<String,String>(1,0f))+","+t(()->new WeakHashMap<String,String>(1,-1f))+","+t(()->new WeakHashMap<String,String>(1,Float.NaN))+","+t(()->new WeakHashMap<String,String>(-1,Float.NaN))+","+t(()->new WeakHashMap<String,String>(1,-0.5f));
                WeakHashMap<String,String> one=new WeakHashMap<>(1,0.25f);
                for(int i=0;i<40;i++)one.put("s"+i,"t"+i);
                a+="|"+one.size()+","+one.get("s39")+","+one.get("s0")+","+one.remove("s0")+","+one.size();
                Map<String,Integer> src=new TreeMap<>();src.put("x",1);src.put("y",2);
                WeakHashMap<String,Integer> c=new WeakHashMap<>(src);
                src.put("z",3);
                a+="|"+s(c)+","+c.size()+","+ty(()->new WeakHashMap<String,String>(null));
                Map<String,Integer> withNull=new HashMap<>();withNull.put(null,1);withNull.put("q",null);
                WeakHashMap<String,Integer> d=new WeakHashMap<>(withNull);
                a+="|"+s(d)+","+d.get(null)+","+d.containsKey("q")+","+ty(()->{d.putAll(null);return "x";});
                Map<String,Integer> big=new TreeMap<>();for(int i=0;i<50;i++)big.put("b"+i,i);
                WeakHashMap<String,Integer> e=new WeakHashMap<>(big);
                WeakHashMap<String,Integer> f=m("p");f.putAll(big);f.putAll(new HashMap<String,Integer>());f.putAll(Map.of("p",9));
                return a+"|"+e.size()+","+e.get("b49")+","+e.equals(big)+","+f.size()+","+f.get("p")+","+f.get("b7");
            }
            public static String defaultMethods(){
                WeakHashMap<String,Integer> m=m("a","b");
                String a=m.getOrDefault("a",9)+","+m.getOrDefault("q",9)+","+m.putIfAbsent("a",5)+","+m.putIfAbsent("c",5)+","+m.get("c");
                m.put("n",null);
                a+="|"+m.getOrDefault("n",9)+","+m.putIfAbsent("n",6)+","+m.get("n")+","+m.remove("a",1)+","+m.remove("a",0)+","+m.containsKey("a")+","+m.replace("b",7)+","+m.replace("q",7)+","+m.containsKey("q")+","+m.replace("b",7,8)+","+m.get("b");
                a+="|"+m.computeIfAbsent("d",k->k.length())+","+m.computeIfPresent("d",(k,v)->v+1)+","+m.merge("d",10,Integer::sum)+","+m.compute("d",(k,v)->null)+","+m.containsKey("d")+","+s(m);
                StringBuilder seen=new StringBuilder();
                List<String> pairs=new ArrayList<>();
                m.forEach((k,v)->pairs.add(k+":"+v));Collections.sort(pairs);
                a+="|"+pairs+","+t(()->{m.forEach(null);return "x";})+","+t(()->{new WeakHashMap<String,String>().forEach(null);return "x";});
                m.replaceAll((k,v)->v==null?-1:v*10);
                a+="|"+s(m)+","+t(()->{m.replaceAll(null);return "x";});
                WeakHashMap<String,Integer> one=m("only");
                a+="|"+t(()->{one.forEach((k,v)->one.put("new",1));return "x";})+","+s(one);
                WeakHashMap<String,Integer> two=m("only");
                a+="|"+t(()->{two.forEach((k,v)->two.put(k,v+1));return "x";})+","+s(two);
                WeakHashMap<String,Integer> three=m("only");
                a+="|"+t(()->{three.replaceAll((k,v)->{three.remove(k);return 5;});return "x";})+","+s(three);
                WeakHashMap<String,Integer> four=m("x","y");
                four.replaceAll((k,v)->{seen.append(four.get(k)).append(' ');return v+100;});
                return a+"|"+s(four)+","+seen.length();
            }
            public static String equalsHashAndText(){
                WeakHashMap<String,Integer> m=m("a","b","c");
                Map<String,Integer> h=new HashMap<>(m);
                String a=m.equals(h)+","+h.equals(m)+","+m.equals(new TreeMap<>(h))+","+m.equals(m)+","+m.equals(m("a","b"))+","+m("a","b").equals(m)+","+m.equals(List.of())+","+m.equals(null)+","+(m.hashCode()==h.hashCode())+","+m.hashCode();
                WeakHashMap<String,String> one=new WeakHashMap<>();one.put("k","v");
                WeakHashMap<Object,Object> self=new WeakHashMap<>();self.put("me",self);
                return a+"|"+one+","+one.keySet()+","+one.values()+","+one.entrySet()+","+self+","+new WeakHashMap<String,String>()+","+new WeakHashMap<String,String>().hashCode();
            }
            public static String views(){
                WeakHashMap<String,Integer> m=m("a","b","c","d","e");
                Set<String> ks=m.keySet();Collection<Integer> vs=m.values();Set<Map.Entry<String,Integer>> es=m.entrySet();
                String a=(ks==m.keySet())+","+(vs==m.values())+","+(es==m.entrySet())+","+s(ks)+","+s(vs)+","+s(es)+","+ks.size()+","+vs.size()+","+es.size();
                a+="|"+ks.contains("a")+","+ks.contains("q")+","+ks.remove("a")+","+ks.remove("a")+","+m.containsKey("a")+","+m.size()+","+vs.contains(1)+","+vs.contains(0)+","+vs.remove(1)+","+vs.remove(1)+","+m.containsKey("b")+","+s(m);
                a+="|"+es.contains(new AbstractMap.SimpleEntry<>("c",2))+","+es.contains(new AbstractMap.SimpleEntry<>("c",9))+","+es.contains("c")+","+es.remove(new AbstractMap.SimpleEntry<>("c",9))+","+es.remove(new AbstractMap.SimpleEntry<>("c",2))+","+es.remove("d")+","+s(m);
                for(Map.Entry<String,Integer> e:es)e.setValue(e.getValue()*100);
                a+="|"+s(m)+","+t(()->ks.add("x"))+","+t(()->vs.add(1));
                Object[] copy=es.toArray();
                ((Map.Entry<String,Integer>)copy[0]).setValue(-1);
                Map.Entry<String,Integer> live=es.iterator().next();
                a+="|"+copy.length+","+(copy[0] instanceof AbstractMap.SimpleEntry)+","+s(m)+","+live.equals(new AbstractMap.SimpleEntry<>(live.getKey(),live.getValue()))+","+(live.hashCode()==new AbstractMap.SimpleEntry<>(live.getKey(),live.getValue()).hashCode())+","+es.toArray(new Map.Entry[0]).length;
                WeakHashMap<String,Integer> r=m("p","q","r","s","t");
                r.keySet().removeAll(List.of("p","q","zz"));r.values().retainAll(List.of(2,3));
                a+="|"+s(r)+","+r.keySet().containsAll(List.of("r","s"))+","+s(List.of(r.keySet().toArray(new String[0])));
                r.keySet().clear();
                WeakHashMap<String,Integer> v=m("x","y");v.values().clear();
                WeakHashMap<String,Integer> w=m("x","y");w.entrySet().clear();
                return a+"|"+r.isEmpty()+","+v.isEmpty()+","+w.size()+","+ks.isEmpty()+","+m.isEmpty();
            }
            public static String failFastIterators(){
                WeakHashMap<String,Integer> m=m("a","b","c");
                Iterator<String> it=m.keySet().iterator();
                it.next();m.put("d",4);
                String a=it.hasNext()+","+t(()->it.next());
                Iterator<String> it2=m.keySet().iterator();
                it2.next();m.put("a",99);
                a+="|"+t(()->it2.next()!=null)+","+m.get("a");
                Iterator<Integer> it3=m.values().iterator();
                it3.next();m.remove("b");
                a+="|"+t(()->it3.next())+","+t(()->{it3.remove();return "x";});
                Iterator<Map.Entry<String,Integer>> it4=m.entrySet().iterator();
                a+="|"+t(()->{it4.remove();return "x";});
                it4.next();it4.remove();
                a+=","+t(()->{it4.remove();return "x";})+","+m.size();
                Iterator<String> it5=m.keySet().iterator();
                while(it5.hasNext())it5.next();
                a+="|"+t(()->it5.next())+","+it5.hasNext();
                m.remove(m.keySet().iterator().next());
                a+=","+t(()->it5.next());
                Iterator<String> it6=m.keySet().iterator();m.clear();
                a+="|"+t(()->it6.next())+","+m.isEmpty();
                WeakHashMap<String,Integer> big=new WeakHashMap<>();for(int i=0;i<30;i++)big.put("k"+i,i);
                Iterator<Map.Entry<String,Integer>> all=big.entrySet().iterator();int seen=0;
                while(all.hasNext()){Map.Entry<String,Integer> e=all.next();seen++;if(e.getValue()%3!=0)all.remove();}
                a+="|"+seen+","+big.size()+","+s(big.values());
                Iterator<String> empty=new WeakHashMap<String,String>().keySet().iterator();
                a+="|"+empty.hasNext()+","+t(()->empty.next());
                WeakHashMap<String,Integer> nk=new WeakHashMap<>();nk.put(null,1);nk.put("x",2);
                Iterator<String> ki=nk.keySet().iterator();List<String> keys=new ArrayList<>();
                while(ki.hasNext()){String k=ki.next();keys.add(String.valueOf(k));if(k==null)ki.remove();}
                Collections.sort(keys);
                return a+"|"+keys+","+nk.containsKey(null)+","+nk.size()+","+s(nk);
            }
            public static String subclassCalls(){
                Spy.log.setLength(0);
                Map<String,Integer> src=new TreeMap<>();src.put("x",1);src.put("y",2);
                Spy<String,Integer> m=new Spy<>(src);
                String a=Spy.log.toString();Spy.log.setLength(0);
                m.putIfAbsent("x",5);m.putIfAbsent("z",3);
                a+="|"+Spy.log;Spy.log.setLength(0);
                m.getOrDefault("q",0);m.getOrDefault("x",0);
                a+="|"+Spy.log;Spy.log.setLength(0);
                m.isEmpty();m.containsKey("x");m.get("x");
                a+="|"+Spy.log;Spy.log.setLength(0);
                Set<String> ks=m.keySet();
                a+="|"+Spy.log;Spy.log.setLength(0);
                ks.size();ks.contains("x");
                a+="|"+Spy.log;Spy.log.setLength(0);
                ks.remove("x");ks.remove("q");
                a+="|"+Spy.log;Spy.log.setLength(0);
                Iterator<String> it=ks.iterator();
                a+="|"+Spy.log;Spy.log.setLength(0);
                it.next();it.remove();
                a+="|"+Spy.log.toString().replaceAll("remove:[yz]","remove:K");Spy.log.setLength(0);
                m.values().contains(3);
                a+="|"+Spy.log;Spy.log.setLength(0);
                m.putAll(Map.of("w",9));
                a+="|"+Spy.log;Spy.log.setLength(0);
                m.entrySet().size();
                a+="|"+Spy.log;Spy.log.setLength(0);
                ks.clear();
                a+="|"+Spy.log;Spy.log.setLength(0);
                m.put("e",1);m.forEach((k,v)->{});m.replaceAll((k,v)->v);
                a+="|"+Spy.log;Spy.log.setLength(0);
                m.remove("e",1);
                return a+"|"+Spy.log;
            }
        }
        """;

    @Test void putGetRemoveAndSize() throws Exception {
        matchesJvm(FIXTURE, "basics");
    }

    @Test void nullKeyAndEqualsLookup() throws Exception {
        matchesJvm(FIXTURE, "nullKeyAndKeyIdentity", "equalsCalls");
    }

    @Test void constructorsAndPutAll() throws Exception {
        matchesJvm(FIXTURE, "constructors");
    }

    @Test void defaultMethodsForEachAndReplaceAll() throws Exception {
        matchesJvm(FIXTURE, "defaultMethods");
    }

    @Test void equalsHashCodeAndToString() throws Exception {
        matchesJvm(FIXTURE, "equalsHashAndText");
    }

    @Test void keyValueAndEntryViews() throws Exception {
        matchesJvm(FIXTURE, "views");
    }

    @Test void failFastIterators() throws Exception {
        matchesJvm(FIXTURE, "failFastIterators");
    }

    @Test void subclassSelfCalls() throws Exception {
        matchesJvm(FIXTURE, "subclassCalls");
    }

    @Test void leavesOutReferenceQueueMachinery() {
        rejectsUnusedCall("java/lang/ref/WeakReference", "get", "()Ljava/lang/Object;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/lang/ref/ReferenceQueue", "poll", "()Ljava/lang/ref/Reference;", INVOKEVIRTUAL, false);
    }
}
