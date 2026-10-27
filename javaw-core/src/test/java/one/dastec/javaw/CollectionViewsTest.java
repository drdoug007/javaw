package one.dastec.javaw;
import org.junit.jupiter.api.Test;
class CollectionViewsTest {
    @Test void executesBulkViewsAndIteratorRemoval() throws Exception {
        byte[] wasm=JavacToWasm.compile("App.java", """
            class App {
                public static int test() {
                    List<Integer> list=new ArrayList<Integer>(); list.add(1); list.add(2); list.add(3); list.add(4);
                    List<Integer> view=list.subList(1,4); List<Integer> nested=view.subList(1,2);
                    nested.add(9); if(list.size()!=5 || view.size()!=4 || nested.size()!=2 || list.get(3)!=9) return -1;
                    Iterator<Integer> it=nested.iterator(); if(it.next()!=3) return -2; it.remove();
                    if(view.size()!=3 || list.get(2)!=9) return -3;
                    list.addAll(list); if(list.size()!=8) return -4;
                    Predicate<Integer> even=n->n%2==0; list.removeIf(even);
                    if(list.size()!=4 || list.contains(2)) return -5;
                    List<Integer> keep=new ArrayList<Integer>(); keep.add(9); list.retainAll(keep);
                    if(list.size()!=2 || list.get(1)!=9) return -6;
                    list.addAll(0,keep); if(list.size()!=3) return -7;
                    Map<String,Integer> map=new HashMap<String,Integer>(); map.put(null,1); map.put("a",2);
                    Collection<Integer> values=map.values(); Set<Map.Entry<String,Integer>> entries=map.entrySet();
                    map.put("b",3); if(values.size()!=3 || entries.size()!=3) return -8;
                    Iterator<Map.Entry<String,Integer>> ei=entries.iterator(); Map.Entry<String,Integer> entry=ei.next();
                    String key=entry.getKey(); entry.setValue(7); if(map.get(key)!=7) return -9;
                    ei.remove(); if(map.containsKey(key) || values.size()!=2) return -10;
                    if(!values.remove(2) && !values.remove(3)) return -11;
                    BiFunction<String,Integer,Integer> increment=(k,v)->v==null ? 10 : v+1;
                    map.compute("z",increment); if(map.get("z")!=10) return -12;
                    BiFunction<Integer,Integer,Integer> sum=(a,b)->a+b;
                    map.merge("z",5,sum); if(map.get("z")!=15) return -13;
                    Iterator<Integer> invalid=list.iterator();
                    try { invalid.remove(); return -14; } catch(IllegalStateException ex) {}
                    invalid.next(); list.add(8);
                    try { invalid.remove(); return -15; } catch(ConcurrentModificationException ex) {}
                    try { view.size(); return -16; } catch(ConcurrentModificationException ex) {}
                    return 42;
                }
                public static int immutableList() { Stream.of(1,2).toList().add(0,3); return 0; }
                public static int immutableFlags() {
                    Iterator<Collector.Characteristics> it=Collectors.<Integer>toList().characteristics().iterator(); it.next(); it.remove(); return 0;
                }
            }
            """,false).getWasm();
        OverloadResolutionTest.execute(wasm,"assert.equal(e.test(),42); assert.throws(()=>e.immutableList()); assert.throws(()=>e.immutableFlags());");
    }
}
