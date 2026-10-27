package one.dastec.javaw;
import org.junit.jupiter.api.Test;
class CollectionSupportTest {
    @Test void executesCollectionsEqualityCollisionsAndIteration() throws Exception {
        byte[] wasm=JavacToWasm.compile("App.java", """
            class Key {
                public int value;
                public Key(int value) { this.value=value; }
                public int hashCode() { return 1; }
                public boolean equals(Object other) { return other instanceof Key && value==((Key)other).value; }
            }
            class App {
                public static int test() {
                    List<Integer> values=new ArrayList<Integer>();
                    for(int i=0;i<40;i++) values.add(i);
                    if(values.size()!=40 || values.get(39)!=39) return -1;
                    if(values.set(0,5)!=0 || values.remove(1)!=1) return -2;
                    values.add(null);
                    if(!values.contains(null) || !values.remove((Object)null)) return -3;
                    int sum=0; for(Integer x:values) sum+=x;
                    if(sum!=784) return -4;
                    Map<String,Integer> map=new HashMap<String,Integer>();
                    map.put("abc",3); map.put("a"+"bc",4); map.put(null,null);
                    if(map.size()!=2 || map.get("ab"+"c")!=4 || !map.containsKey(null)) return -5;
                    if(map.remove("abc")!=4 || map.size()!=1) return -6;
                    HashMap<Key,Integer> collisions=new HashMap<Key,Integer>();
                    for(int i=0;i<70;i++) collisions.put(new Key(i),i);
                    if(collisions.get(new Key(55))!=55 || collisions.size()!=70) return -7;
                    collisions.put(new Key(55),99);
                    if(collisions.remove(new Key(55))!=99 || collisions.size()!=69) return -8;
                    int keys=0; for(Key key:collisions.keySet()) keys++;
                    if(keys!=69) return -9;
                    Set<String> set=new HashSet<String>();
                    if(!set.add("same") || set.add("sa"+"me")) return -10;
                    set.add(null); if(set.size()!=2 || !set.contains(null)) return -11;
                    int seen=0; for(String value:set) seen++;
                    if(seen!=2) return -12;
                    Collection<String> collection=set; collection.clear();
                    return collection.isEmpty() ? 42 : -13;
                }
                public static int exhausted() { ArrayList<String> list=new ArrayList<String>(); return list.iterator().next().length(); }
                public static int bounds() { ArrayList<String> list=new ArrayList<String>(); return list.get(0).length(); }
                public static int concurrent() {
                    ArrayList<Integer> list=new ArrayList<Integer>(); list.add(1);
                    Iterator<Integer> iterator=list.iterator(); list.add(2); return iterator.next();
                }
            }
            """,false).getWasm();
        OverloadResolutionTest.execute(wasm,"""
            assert.equal(e.test(),42);
            assert.throws(()=>e.exhausted()); assert.throws(()=>e.bounds()); assert.throws(()=>e.concurrent());
            """);
    }
}
