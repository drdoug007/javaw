package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** T47: java.util.concurrent.ConcurrentHashMap core map (single-threaded contract). Expected values come from the JVM; see tasks/T47-concurrent-hash-map.md. */
@Tag("agent-pending")
class BytecodeConcurrentHashMapTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.io.Serializable;
        import java.util.AbstractMap;
        import java.util.ArrayList;
        import java.util.Collection;
        import java.util.HashMap;
        import java.util.HashSet;
        import java.util.Iterator;
        import java.util.List;
        import java.util.Map;
        import java.util.Set;
        import java.util.TreeMap;
        import java.util.TreeSet;
        import java.util.concurrent.ConcurrentHashMap;
        import java.util.concurrent.ConcurrentMap;

        /** Key with a chosen hash; logs every equals call as this?other. */
        class Key {
            static final StringBuilder log=new StringBuilder();
            final String n; final int h; Key(String n,int h){this.n=n;this.h=h;}
            public boolean equals(Object o){log.append(n).append('?').append(o).append(' ');return o instanceof Key&&((Key)o).n.equals(n);}
            public int hashCode(){return h;}
            public String toString(){return n;}
        }
        /** Value that logs every equals call as this?other. */
        class Val {
            static final StringBuilder log=new StringBuilder();
            final String n; Val(String n){this.n=n;}
            public boolean equals(Object o){log.append(n).append('?').append(o).append(' ');return o instanceof Val&&((Val)o).n.equals(n);}
            public int hashCode(){return n.hashCode();}
            public String toString(){return n;}
        }
        /** Like a library subclass: logs the overridable calls the map makes on itself. */
        class Spy<K,V> extends ConcurrentHashMap<K,V> {
            /** Static: the copy constructor logs before the subclass fields are set. */
            static final StringBuilder log=new StringBuilder();
            Spy(){}
            Spy(Map<? extends K,? extends V> m){super(m);}
            public V get(Object k){log.append("get:").append(k).append(' ');return super.get(k);}
            public V put(K k,V v){log.append("put:").append(k).append(' ');return super.put(k,v);}
            public V remove(Object k){log.append("remove:").append(k).append(' ');return super.remove(k);}
            public boolean remove(Object k,Object v){log.append("remove2:").append(k).append(' ');return super.remove(k,v);}
            public boolean containsKey(Object k){log.append("containsKey:").append(k).append(' ');return super.containsKey(k);}
            public boolean containsValue(Object v){log.append("containsValue:").append(v).append(' ');return super.containsValue(v);}
            public void putAll(Map<? extends K,? extends V> m){log.append("putAll ");super.putAll(m);}
            public V putIfAbsent(K k,V v){log.append("putIfAbsent:").append(k).append(' ');return super.putIfAbsent(k,v);}
            public int size(){log.append("size ");return super.size();}
            public boolean isEmpty(){log.append("isEmpty ");return super.isEmpty();}
            public void clear(){log.append("clear ");super.clear();}
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
            /** Sorted text of any map, independent of iteration order. */
            static String s(Map<?,?> m){return new TreeMap<>(m).toString();}
            /** Sorted text of any collection of comparable elements. */
            static String s(Collection<?> c){List<String> l=new ArrayList<>();for(Object o:c)l.add(String.valueOf(o));java.util.Collections.sort(l);return l.toString();}
            /** Map with Integer keys 0..15 (these iterate in ascending key order). */
            static ConcurrentHashMap<Integer,String> m(int... keys){ConcurrentHashMap<Integer,String> m=new ConcurrentHashMap<>();for(int k:keys)m.put(k,"v"+k);return m;}
            static String all(Iterator<?> it){StringBuilder o=new StringBuilder();while(it.hasNext())o.append(it.next()).append(' ');return o.append(t(()->it.next())).toString();}

            public static String basics(){
                ConcurrentHashMap<String,Integer> m=new ConcurrentHashMap<>();
                String a=m.isEmpty()+","+m.size()+","+m.get("a")+","+m.containsKey("a")+","+m.remove("a")+","+m+","+m.containsValue(1);
                a+="|"+m.put("a",1)+","+m.put("b",2)+","+m.put("a",3)+","+m.size()+","+m.isEmpty()+","+m.get("a")+","+m.containsKey("b")+","+m.containsValue(3)+","+m.containsValue(1)+","+s(m);
                a+="|"+m.remove("a")+","+m.remove("a")+","+m.size()+","+s(m);
                for(int i=0;i<100;i++)m.put("k"+i,i);
                int sum=0;for(int i=0;i<100;i++)sum+=m.get("k"+i);
                a+="|"+m.size()+","+sum+","+m.get("k57")+","+m.containsKey("k100")+","+m.remove("k3")+","+m.size()+","+m.keySet().size()+","+m.values().size()+","+m.entrySet().size();
                m.clear();
                a+="|"+m.size()+","+m.isEmpty()+","+m+","+m.get("k5")+","+m.put("z",26)+","+m;
                Object w=m;
                a+="|"+(w instanceof ConcurrentMap)+","+(w instanceof AbstractMap)+","+(w instanceof Serializable)+","+(w instanceof Map);
                ConcurrentHashMap<Integer,String> n=m(5,1,9,0,15,7);
                return a+"|"+n+","+n.keySet()+","+n.values()+","+n.entrySet();
            }
            public static String nullRules(){
                ConcurrentHashMap<String,String> m=new ConcurrentHashMap<>();m.put("a","x");
                String a=ty(()->m.put(null,"v"))+","+ty(()->m.put("k",null))+","+ty(()->m.get(null))+","+ty(()->m.containsKey(null))+","+ty(()->m.containsValue(null))+","+ty(()->m.remove(null));
                a+="|"+ty(()->m.putIfAbsent(null,"v"))+","+ty(()->m.putIfAbsent("a",null))+","+ty(()->m.putIfAbsent("q",null))+","+ty(()->m.replace(null,"v"))+","+ty(()->m.replace("a",null))+","+ty(()->m.replace("q",null));
                a+="|"+ty(()->m.replace(null,"x","y"))+","+ty(()->m.replace("a",null,"y"))+","+ty(()->m.replace("a","x",null))+","+ty(()->m.remove(null,"x"))+","+m.remove("a",null)+","+m.remove("q",null);
                a+="|"+ty(()->m.computeIfAbsent(null,k->"v"))+","+ty(()->m.computeIfAbsent("a",null))+","+ty(()->m.computeIfPresent(null,(k,v)->v))+","+ty(()->m.computeIfPresent("q",null))+","+ty(()->m.compute(null,(k,v)->v))+","+ty(()->m.compute("a",null));
                a+="|"+ty(()->m.merge(null,"v",(x,y)->x))+","+ty(()->m.merge("a",null,(x,y)->x))+","+ty(()->m.merge("a","v",null))+","+ty(()->{m.putAll(null);return "x";})+","+ty(()->new ConcurrentHashMap<String,String>().get(null));
                a+="|"+ty(()->m.keySet().contains(null))+","+ty(()->m.keySet().remove(null))+","+ty(()->m.values().contains(null))+","+m.values().remove(null)+","+m.entrySet().contains(null)+","+m.entrySet().remove(null);
                return a+"|"+m+","+m.size();
            }
            public static String constructors(){
                String a=new ConcurrentHashMap<String,String>(0).isEmpty()+","+new ConcurrentHashMap<String,String>(1000).size()+","+new ConcurrentHashMap<String,String>(4,0.5f).isEmpty()+","+new ConcurrentHashMap<String,String>(4,2f,8).isEmpty();
                a+="|"+t(()->new ConcurrentHashMap<String,String>(-1))+","+t(()->new ConcurrentHashMap<String,String>(1,0f))+","+t(()->new ConcurrentHashMap<String,String>(1,-1f))+","+t(()->new ConcurrentHashMap<String,String>(1,Float.NaN))+","+t(()->new ConcurrentHashMap<String,String>(-1,0.75f))+","+t(()->new ConcurrentHashMap<String,String>(1,0.75f,0))+","+t(()->new ConcurrentHashMap<String,String>(0,0.75f,-3));
                ConcurrentHashMap<String,String> one=new ConcurrentHashMap<>(1);
                for(int i=0;i<20;i++)one.put("s"+i,"t"+i);
                a+="|"+one.size()+","+one.get("s19")+","+one.remove("s0")+","+one.size();
                Map<String,Integer> src=new HashMap<>();src.put("x",1);src.put("y",2);
                ConcurrentHashMap<String,Integer> c=new ConcurrentHashMap<>(src);
                src.put("z",3);
                a+="|"+s(c)+","+c.size()+","+ty(()->new ConcurrentHashMap<String,String>(null));
                Map<Integer,String> withNull=new TreeMap<>();withNull.put(1,"a");withNull.put(2,null);withNull.put(3,"c");
                a+="|"+ty(()->new ConcurrentHashMap<>(withNull));
                ConcurrentHashMap<Integer,String> p=m(0);
                a+=","+ty(()->{p.putAll(withNull);return "x";})+","+p+","+p.size();
                p.putAll(Map.of(4,"d"));p.putAll(new HashMap<Integer,String>());
                return a+"|"+p+","+new ConcurrentHashMap<>(p).equals(p);
            }
            public static String conditionalWrites(){
                ConcurrentHashMap<String,String> m=new ConcurrentHashMap<>();
                String a=m.putIfAbsent("a","1")+","+m.putIfAbsent("a","2")+","+m.get("a")+","+m.replace("b","1")+","+m.containsKey("b")+","+m.replace("a","3")+","+m.get("a");
                a+="|"+m.replace("a","9","4")+","+m.replace("a","3","4")+","+m.get("a")+","+m.replace("q","x","y")+","+m.containsKey("q");
                a+="|"+m.remove("a","9")+","+m.containsKey("a")+","+m.remove("a","4")+","+m.containsKey("a")+","+m.remove("zz","x")+","+m.size();
                ConcurrentHashMap<String,Val> v=new ConcurrentHashMap<>();v.put("k",new Val("p"));
                Val.log.setLength(0);
                a+="|"+v.replace("k",new Val("q"),new Val("r"))+","+v.replace("k",new Val("p"),new Val("r"))+","+v.get("k")+" "+Val.log;Val.log.setLength(0);
                a+="|"+v.remove("k",new Val("p"))+","+v.remove("k",new Val("r"))+","+v.isEmpty()+" "+Val.log;Val.log.setLength(0);
                v.put("j",new Val("a"));v.put("i",new Val("b"));
                a+="|"+v.containsValue(new Val("b"))+","+v.containsValue("b")+" "+Val.log;
                return a;
            }
            public static String computeAndMerge(){
                ConcurrentHashMap<String,Integer> m=new ConcurrentHashMap<>();StringBuilder calls=new StringBuilder();
                String a=m.computeIfAbsent("a",k->{calls.append("A:"+k+" ");return 1;})+","+m.computeIfAbsent("a",k->{calls.append("B ");return 2;})+","+m.computeIfAbsent("b",k->{calls.append("C ");return null;})+","+m.containsKey("b")+","+s(m)+" "+calls;calls.setLength(0);
                a+="|"+m.computeIfPresent("a",(k,v)->{calls.append(k+v+" ");return v+10;})+","+m.computeIfPresent("q",(k,v)->{calls.append("Q ");return 5;})+","+m.containsKey("q")+","+m.computeIfPresent("a",(k,v)->null)+","+m.containsKey("a")+","+s(m)+" "+calls;calls.setLength(0);
                a+="|"+m.compute("c",(k,v)->{calls.append(k+v+" ");return 7;})+","+m.compute("c",(k,v)->{calls.append(k+v+" ");return v*2;})+","+m.compute("c",(k,v)->null)+","+m.containsKey("c")+","+m.compute("d",(k,v)->null)+","+m.containsKey("d")+","+s(m)+" "+calls;calls.setLength(0);
                a+="|"+m.merge("e",1,(x,y)->{calls.append("never ");return 0;})+","+m.merge("e",5,(x,y)->{calls.append(x+"+"+y+" ");return x+y;})+","+m.get("e")+","+m.merge("e",1,(x,y)->null)+","+m.containsKey("e")+","+s(m)+" "+calls;calls.setLength(0);
                a+="|"+t(()->m.computeIfAbsent("f",k->{throw new IllegalStateException("boom");}))+","+m.containsKey("f")+","+m.put("g",1)+","+t(()->m.compute("g",(k,v)->{throw new IllegalArgumentException("bad");}))+","+m.get("g")+","+t(()->m.merge("g",2,(x,y)->{throw new ArithmeticException("x");}))+","+m.get("g");
                ConcurrentHashMap<String,List<String>> multi=new ConcurrentHashMap<>();
                multi.computeIfAbsent("x",k->new ArrayList<>()).add("1");multi.computeIfAbsent("x",k->new ArrayList<>()).add("2");multi.computeIfAbsent("y",k->new ArrayList<>()).add("3");
                ConcurrentHashMap<String,Integer> words=new ConcurrentHashMap<>();
                for(String w:"a b a c b a".split(" "))words.merge(w,1,Integer::sum);
                return a+"|"+s(multi)+","+s(words)+","+words.size();
            }
            public static String equalsHashAndText(){
                ConcurrentHashMap<Integer,String> m=m(1,2,3);
                Map<Integer,String> h=new HashMap<>(m);
                String a=m.equals(h)+","+h.equals(m)+","+m.equals(new TreeMap<>(m))+","+m.equals(m)+","+m.equals(m(1,2))+","+m(1,2).equals(m)+","+m.equals(List.of())+","+m.equals(null)+","+(m.hashCode()==h.hashCode())+","+m.hashCode();
                Map<Integer,String> nv=new HashMap<>();nv.put(1,"v1");nv.put(2,"v2");nv.put(3,null);
                Map<Integer,String> nk=new HashMap<>();nk.put(1,"v1");nk.put(2,"v2");nk.put(null,"v3");
                a+="|"+m.equals(nv)+","+m.equals(nk)+","+m.equals(Map.of(1,"v1",2,"v2",3,"x"))+","+new ConcurrentHashMap<>().equals(new HashMap<>())+","+new ConcurrentHashMap<>().hashCode();
                ConcurrentHashMap<String,Object> self=new ConcurrentHashMap<>();self.put("me",self);
                ConcurrentHashMap<Object,String> selfKey=new ConcurrentHashMap<>();selfKey.put(selfKey,"k");
                a+="|"+self+","+t(()->selfKey.toString())+","+new ConcurrentHashMap<>()+","+m(7)+","+m(0,15);
                ConcurrentHashMap<Integer,Val> v=new ConcurrentHashMap<>();v.put(1,new Val("a"));
                Map<Integer,Val> hv=new HashMap<>();hv.put(1,new Val("a"));
                Val.log.setLength(0);
                return a+"|"+v.equals(hv)+" "+Val.log;
            }
            public static String keyEquality(){
                ConcurrentHashMap<Key,String> m=new ConcurrentHashMap<>();
                Key a1=new Key("a",5),b1=new Key("b",5),c1=new Key("c",6);
                m.put(a1,"A");m.put(b1,"B");m.put(c1,"C");
                Key.log.setLength(0);
                String a=m.get(new Key("b",5))+" "+Key.log;Key.log.setLength(0);
                a+="|"+m.get(a1)+","+m.get(b1)+","+m.get(new Key("a",6))+","+m.containsKey(new Key("c",6))+" "+Key.log;Key.log.setLength(0);
                a+="|"+m.put(new Key("b",5),"B2")+","+m.size()+","+m+" "+Key.log;Key.log.setLength(0);
                a+="|"+m.remove(new Key("a",5))+","+m+","+m.remove(new Key("zz",5))+" "+Key.log;Key.log.setLength(0);
                m.put(new Key("d",5),"D");
                a+="|"+m+","+m.keySet()+","+m.get(new Key("d",5))+" "+Key.log;
                return a;
            }
            public static String views(){
                ConcurrentHashMap<Integer,String> m=m(1,2,3,4);
                Set<Integer> ks=m.keySet();Collection<String> vs=m.values();Set<Map.Entry<Integer,String>> es=m.entrySet();
                Object wk=ks,wv=vs,we=es;
                String a=(ks==m.keySet())+","+(vs==m.values())+","+(es==m.entrySet())+","+(wk instanceof Set)+","+(wv instanceof Set)+","+(we instanceof Set)+","+(wk instanceof Serializable)+","+m.keySet().getMap().equals(m)+","+(m.keySet().getMap()==m);
                a+="|"+ks.contains(2)+","+ks.contains(9)+","+vs.contains("v3")+","+vs.contains("x")+","+es.contains(Map.entry(1,"v1"))+","+es.contains(Map.entry(1,"x"))+","+es.contains("v1")+","+ks.size()+","+vs.isEmpty();
                a+="|"+ks.remove(2)+","+ks.remove(2)+","+vs.remove("v3")+","+vs.remove("x")+","+es.remove(Map.entry(4,"no"))+","+es.remove(Map.entry(4,"v4"))+","+m+","+ks+","+vs+","+es;
                a+="|"+t(()->ks.add(5))+","+t(()->ks.addAll(List.of()))+","+t(()->vs.add("x"))+","+t(()->vs.addAll(List.of("x")))+","+es.add(Map.entry(6,"v6"))+","+es.add(Map.entry(6,"w6"))+","+es.addAll(List.of(Map.entry(7,"v7")))+","+m;
                ConcurrentHashMap<Integer,String> n=m(0,1,2,3,4,5,6,7);
                a+="|"+n.keySet().removeAll(List.of(1,9))+","+n.values().removeAll(List.of("v2","v3"))+","+n.entrySet().removeAll(List.of(Map.entry(4,"v4")))+","+n+","+n.keySet().retainAll(List.of(0,5,6,7))+","+n.values().retainAll(Set.of("v0","v7"))+","+n+","+ty(()->n.keySet().removeAll(null))+","+ty(()->n.values().retainAll(null))+","+n.keySet().removeAll(List.of());
                a+="|"+n.keySet().containsAll(List.of(0,7))+","+n.keySet().containsAll(List.of(0,8))+","+n.values().containsAll(List.of("v7"))+","+n.keySet().containsAll(n.keySet())+","+n.keySet().equals(Set.of(0,7))+","+n.keySet().equals(Set.of(0))+","+n.keySet().equals(List.of(0,7))+","+n.keySet().hashCode()+","+n.entrySet().equals(Map.of(0,"v0",7,"v7").entrySet())+","+n.entrySet().hashCode()+","+(n.entrySet().hashCode()==n.hashCode());
                Object[] ka=n.keySet().toArray();String[] big={"1","2","3","4"};String[] va=n.values().toArray(big);Integer[] small=n.keySet().toArray(new Integer[0]);Object[] ea=n.entrySet().toArray();
                a+="|"+ka.length+":"+ka[0]+ka[1]+":"+ka.getClass().getSimpleName()+","+(va==big)+":"+va[0]+va[1]+va[2]+va[3]+","+small.length+":"+small[1]+":"+small.getClass().getSimpleName()+","+ea.length+":"+ea[1]+","+ty(()->n.keySet().toArray((Integer[])null))+","+new ConcurrentHashMap<String,String>().values().toArray().length;
                n.keySet().clear();
                a+="|"+n+","+n.size()+","+n.keySet().isEmpty()+","+n.put(3,"x")+","+n.values()+","+m(1,2).values().removeIf(x->x.equals("v1"));
                ConcurrentHashMap<Integer,Integer> d=new ConcurrentHashMap<>();d.put(1,7);d.put(2,7);d.put(3,8);
                return a+"|"+d.values().remove(7)+","+d+","+d.values().removeAll(List.of(7,8))+","+d;
            }
            public static String entries(){
                ConcurrentHashMap<Integer,String> m=m(1,2,3);
                Iterator<Map.Entry<Integer,String>> it=m.entrySet().iterator();
                Map.Entry<Integer,String> e=it.next();
                String a=e+","+e.getKey()+","+e.getValue()+","+e.equals(Map.entry(1,"v1"))+","+Map.entry(1,"v1").equals(e)+","+e.equals(Map.entry(1,"x"))+","+e.hashCode()+","+(e.hashCode()==Map.entry(1,"v1").hashCode())+","+e.equals("x");
                a+="|"+e.setValue("w1")+","+e+","+m.get(1)+","+ty(()->e.setValue(null))+","+e.getValue()+","+m.get(1);
                Map.Entry<Integer,String> f=it.next();
                m.remove(2);
                a+="|"+f.setValue("back")+","+m.get(2)+","+m+","+m.size();
                m.put(3,"changed");
                Map.Entry<Integer,String> g=it.next();
                a+="|"+g+","+m.put(3,"again")+","+g.getValue()+","+it.hasNext();
                ConcurrentHashMap<String,String> h=new ConcurrentHashMap<>();h.put("k","v");
                Map.Entry<String,String> x=h.entrySet().iterator().next();
                return a+"|"+x+","+x.equals(new AbstractMap.SimpleEntry<>("k","v"))+","+new AbstractMap.SimpleEntry<>("k","v").equals(x)+","+x.equals(new AbstractMap.SimpleEntry<String,String>("k",null))+","+x.equals(new AbstractMap.SimpleEntry<String,String>(null,"v"));
            }
            public static String iterators(){
                ConcurrentHashMap<Integer,String> m=m(1,3,5);
                String a=all(m.keySet().iterator())+"|"+all(m.values().iterator())+"|"+all(m.entrySet().iterator())+"|"+all(m(0).keySet().iterator());
                Iterator<Integer> it=m.keySet().iterator();
                a+="|"+it.next()+","+m.put(4,"v4")+","+m.put(0,"v0")+","+m.remove(5)+","+m.remove(3)+","+it.hasNext()+","+all(it)+","+m;
                ConcurrentHashMap<Integer,String> r=m(1,2,3,4);Iterator<Integer> i2=r.keySet().iterator();
                a+="|"+t(()->{i2.remove();return "x";})+","+i2.next();
                i2.remove();
                a+=","+t(()->{i2.remove();return "x";})+","+r+","+i2.next()+","+r.put(2,"z");
                i2.remove();
                a+=","+r+","+i2.next()+","+r.remove(3);
                i2.remove();
                a+=","+r+","+i2.next()+","+i2.hasNext();
                ConcurrentHashMap<Integer,String> v=m(1,2,3);Iterator<String> i3=v.values().iterator();
                a+="|"+i3.next()+","+v.put(2,"new2")+","+i3.next()+","+v.remove(3)+","+i3.next()+","+i3.hasNext();
                i3.remove();
                a+=","+v;
                ConcurrentHashMap<Integer,String> c=m(1,2,3);Iterator<Map.Entry<Integer,String>> i4=c.entrySet().iterator();
                a+="|"+i4.next()+",";c.clear();
                a+=c.size()+","+i4.hasNext()+","+i4.next()+","+i4.hasNext()+","+c.put(9,"v9")+","+t(()->i4.next());
                ConcurrentHashMap<Integer,String> w=m(1,2,3,4,5);StringBuilder o=new StringBuilder();
                for(Integer k:w.keySet()){o.append(k);if(k==1)w.remove(4);if(k==2)w.put(6,"v6");if(k==3)w.remove(1);}
                return a+"|"+o+","+w;
            }
            public static String subclassCalls(){
                Map<String,String> src=new TreeMap<>();src.put("a","1");src.put("b","2");
                Spy<String,String> p=new Spy<>(src);
                String a=p.log+"|";p.log.setLength(0);
                p.putIfAbsent("c","3");p.replace("c","4");p.replace("c","4","5");p.computeIfAbsent("d",k->"6");p.computeIfPresent("d",(k,v)->"7");p.compute("e",(k,v)->"8");p.merge("e","9",(x,y)->x+y);
                a+=p.log+"|";p.log.setLength(0);
                p.containsKey("a");a+=p.log+"|";p.log.setLength(0);
                p.toString();p.hashCode();p.containsValue("x");a+=p.log+"|";p.log.setLength(0);
                p.putAll(Map.of("f","10"));a+=p.log+"|";p.log.setLength(0);
                p.keySet().contains("a");p.keySet().remove("f");p.keySet().size();p.keySet().isEmpty();a+=p.log+"|";p.log.setLength(0);
                p.values().contains("1");p.values().remove("2");a+=p.log+"|";p.log.setLength(0);
                p.entrySet().contains(Map.entry("a","1"));p.entrySet().remove(Map.entry("c","5"));p.entrySet().add(Map.entry("g","11"));a+=p.log+"|";p.log.setLength(0);
                Map.Entry<String,String> e=p.entrySet().iterator().next();p.log.setLength(0);e.setValue("12");a+=p.log+"|";p.log.setLength(0);
                Iterator<String> it=p.keySet().iterator();it.next();p.log.setLength(0);it.remove();a+=p.log+"|";p.log.setLength(0);
                p.equals(Map.of("x","y"));a+=p.log+"|";p.log.setLength(0);
                Map<String,String> copy=new HashMap<>();for(Map.Entry<String,String> x:p.entrySet())copy.put(x.getKey(),x.getValue());p.log.setLength(0);
                a+=p.equals(copy)+":"+s(List.of(p.log.toString().split(" ")))+"|";p.log.setLength(0);
                p.keySet().removeAll(List.of("zz"));p.keySet().retainAll(new ArrayList<>(p.keySet()));p.keySet().clear();a+=p.log+"|";p.log.setLength(0);
                p.remove("q");p.remove("q","r");p.isEmpty();p.size();
                return a+p.log+","+p;
            }
        }
        """;

    @Test void putGetRemoveAndSize() throws Exception {
        matchesJvm(FIXTURE, "basics");
    }

    @Test void nullKeysAndValuesAreRejected() throws Exception {
        matchesJvm(FIXTURE, "nullRules");
    }

    @Test void constructorsAndPutAll() throws Exception {
        matchesJvm(FIXTURE, "constructors");
    }

    @Test void conditionalWrites() throws Exception {
        matchesJvm(FIXTURE, "conditionalWrites");
    }

    @Test void computeAndMerge() throws Exception {
        matchesJvm(FIXTURE, "computeAndMerge");
    }

    @Test void equalsHashCodeAndToString() throws Exception {
        matchesJvm(FIXTURE, "equalsHashAndText", "keyEquality");
    }

    @Test void keyValueAndEntryViews() throws Exception {
        matchesJvm(FIXTURE, "views", "entries");
    }

    @Test void weaklyConsistentIterators() throws Exception {
        matchesJvm(FIXTURE, "iterators");
    }

    @Test void subclassSelfCalls() throws Exception {
        matchesJvm(FIXTURE, "subclassCalls");
    }

    @Test void leavesOutParallelBulkAndSerialization() {
        rejectsUnusedCall("java/util/concurrent/ConcurrentHashMap", "forEach", "(JLjava/util/function/BiConsumer;)V", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/concurrent/ConcurrentHashMap", "writeObject", "(Ljava/io/ObjectOutputStream;)V", INVOKESPECIAL, false);
    }
}
