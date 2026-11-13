package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** T09: monitor-free Collections.synchronizedCollection/Set/List/Map delegating views. */
@Tag("agent-pending")
class BytecodeSynchronizedCollectionsTest extends KitDifferential {
    private static String fixture(String body) {
        return "import java.util.*;\nimport java.util.stream.*;\npublic class Fixture {\n" + """
                static String failure(Runnable action){
                    try{action.run();return "no exception";}
                    catch(RuntimeException e){return e.getClass().getSimpleName();}
                }
                static List<String> abc(){return new ArrayList<>(List.of("a","b","c"));}
            """ + body + "}\n";
    }

    @Test void synchronizedCollection() throws Exception {
        matchesJvm(fixture("""
                public static String readsDelegate(){
                    List<String> backing=abc();Collection<String> c=Collections.synchronizedCollection(backing);
                    return c.size()+"|"+c.isEmpty()+"|"+c.contains("b")+c.contains("z")+"|"+c.containsAll(List.of("a","c"))+"|"+c+"|"+c.toArray().length+"|"+c.toArray(new String[0])[2];
                }
                public static String writesThrough(){
                    List<String> backing=abc();Collection<String> c=Collections.synchronizedCollection(backing);
                    boolean a=c.add("d"),r=c.remove("a"),m=c.remove("zz");c.addAll(List.of("e","f"));
                    String mid=backing.toString();
                    c.removeAll(List.of("e"));c.retainAll(List.of("b","c","f","g"));boolean ri=c.removeIf(s->s.equals("f"));
                    backing.add("late");
                    return a+","+r+","+m+","+ri+"|"+mid+"|"+backing+"|"+c+"|"+c.size();
                }
                public static String iteratorWritesThrough(){
                    List<String> backing=abc();Collection<String> c=Collections.synchronizedCollection(backing);
                    Iterator<String> it=c.iterator();it.next();it.remove();StringBuilder out=new StringBuilder();
                    for(String s:c)out.append(s);
                    c.forEach(s->out.append(s.toUpperCase()));
                    return out+"|"+backing;
                }
                public static String clearAndStream(){
                    List<String> backing=abc();Collection<String> c=Collections.synchronizedCollection(backing);
                    String joined=c.stream().map(String::toUpperCase).collect(Collectors.joining("-"));
                    c.clear();
                    return joined+"|"+backing.isEmpty()+"|"+c.isEmpty()+"|"+c;
                }
                public static String identityEquality(){
                    List<String> backing=abc();Collection<String> c=Collections.synchronizedCollection(backing);
                    return c.equals(c)+","+c.equals(backing)+","+backing.equals(c)+","+c.equals(abc());
                }
                public static String viewOfASet(){
                    Set<String> backing=new TreeSet<>(Set.of("q","p"));Collection<String> c=Collections.synchronizedCollection(backing);
                    boolean dup=c.add("p");c.add("o");
                    return dup+"|"+c+"|"+backing+"|"+(c instanceof Set)+","+(c instanceof List);
                }
            """), "readsDelegate", "writesThrough", "iteratorWritesThrough", "clearAndStream", "identityEquality", "viewOfASet");
    }

    @Test void synchronizedSet() throws Exception {
        matchesJvm(fixture("""
                public static String setBasics(){
                    Set<String> backing=new LinkedHashSet<>(List.of("x","y"));Set<String> s=Collections.synchronizedSet(backing);
                    boolean a=s.add("z"),b=s.add("x"),r=s.remove("y");
                    return a+","+b+","+r+"|"+s+"|"+backing+"|"+s.size()+"|"+s.contains("z");
                }
                public static String setEquality(){
                    Set<String> backing=new TreeSet<>(List.of("b","a"));Set<String> s=Collections.synchronizedSet(backing);
                    return s.equals(s)+","+s.equals(backing)+","+backing.equals(s)+","+s.equals(new LinkedHashSet<>(List.of("a","b")))+","+(s.hashCode()==backing.hashCode())+","+s.equals(List.of("a","b"))+","+s.hashCode();
                }
                public static String setIterator(){
                    Set<String> backing=new TreeSet<>(List.of("c","a","b"));Set<String> s=Collections.synchronizedSet(backing);
                    Iterator<String> it=s.iterator();it.next();it.remove();s.removeIf(x->x.equals("c"));
                    return s+"|"+backing+"|"+(s instanceof Set);
                }
            """), "setBasics", "setEquality", "setIterator");
    }

    @Test void synchronizedList() throws Exception {
        matchesJvm(fixture("""
                public static String positional(){
                    List<String> backing=abc();List<String> l=Collections.synchronizedList(backing);
                    String old=l.set(1,"B");l.add(0,"first");String removed=l.remove(3);l.add("b");
                    return old+","+removed+"|"+l+"|"+backing+"|"+l.get(1)+l.get(2)+"|"+l.indexOf("b")+","+l.lastIndexOf("first")+","+l.indexOf("zz");
                }
                public static String listFailuresDelegate(){
                    List<String> l=Collections.synchronizedList(abc());
                    return failure(()->l.get(3))+"|"+failure(()->l.remove(-1))+"|"+failure(()->l.add(9,"x"));
                }
                public static String listEquality(){
                    List<String> backing=abc();List<String> l=Collections.synchronizedList(backing);
                    return l.equals(l)+","+l.equals(backing)+","+backing.equals(l)+","+l.equals(List.of("a","b","c"))+","+(l.hashCode()==backing.hashCode())+","+l.hashCode()+","+l.equals(Set.of("a"));
                }
                public static String randomAccess(){
                    List<String> fast=Collections.synchronizedList(abc());
                    List<String> slow=Collections.synchronizedList(new LinkedList<>(abc()));
                    return (fast instanceof RandomAccess)+","+(slow instanceof RandomAccess)+","+(fast.subList(0,2) instanceof RandomAccess)+","+(slow.subList(0,2) instanceof RandomAccess);
                }
                public static String subListWritesThrough(){
                    List<String> backing=abc();backing.add("d");List<String> l=Collections.synchronizedList(backing);
                    List<String> sub=l.subList(1,3);String seen=sub.toString();sub.set(0,"B");sub.remove(1);sub.add("x");
                    return seen+"|"+sub+"|"+backing+"|"+l;
                }
                public static String listIterators(){
                    List<String> backing=abc();List<String> l=Collections.synchronizedList(backing);
                    ListIterator<String> it=l.listIterator();it.next();it.set("A");it.add("x");
                    ListIterator<String> back=l.listIterator(l.size());StringBuilder out=new StringBuilder();
                    while(back.hasPrevious())out.append(back.previous());
                    return out+"|"+backing;
                }
                public static String bulkAndDefaults(){
                    List<String> backing=new ArrayList<>(List.of("d","b","a","c"));List<String> l=Collections.synchronizedList(backing);
                    l.sort(null);String sorted=backing.toString();
                    l.replaceAll(String::toUpperCase);l.addAll(1,List.of("x","y"));l.removeIf(s->s.equals("C"));
                    return sorted+"|"+backing+"|"+l.size()+"|"+l.stream().collect(Collectors.joining());
                }
            """), "positional", "listFailuresDelegate", "listEquality", "randomAccess", "subListWritesThrough",
                "listIterators", "bulkAndDefaults");
    }

    @Test void synchronizedMap() throws Exception {
        matchesJvm(fixture("""
                static Map<String,Integer> backing(){Map<String,Integer> m=new LinkedHashMap<>();m.put("one",1);m.put("two",2);return m;}
                public static String basics(){
                    Map<String,Integer> backing=backing();Map<String,Integer> m=Collections.synchronizedMap(backing);
                    Integer old=m.put("one",11),fresh=m.put("three",3),gone=m.remove("two"),missing=m.remove("zz");
                    return old+","+fresh+","+gone+","+missing+"|"+m+"|"+backing+"|"+m.size()+","+m.isEmpty()+","+m.get("three")+","+m.get("zz")+","+m.containsKey("one")+","+m.containsValue(3)+","+m.containsValue(2);
                }
                public static String defaults(){
                    Map<String,Integer> backing=backing();Map<String,Integer> m=Collections.synchronizedMap(backing);
                    int d=m.getOrDefault("zz",-1);Integer p=m.putIfAbsent("one",9),q=m.putIfAbsent("four",4);
                    m.computeIfAbsent("five",k->k.length());m.computeIfPresent("two",(k,v)->v*10);m.merge("one",5,Integer::sum);m.compute("six",(k,v)->v==null?6:v);
                    boolean r1=m.remove("four",5),r2=m.remove("four",4),rp=m.replace("two",20,21);Integer rv=m.replace("zz",0);
                    m.replaceAll((k,v)->v+1000);StringBuilder out=new StringBuilder();m.forEach((k,v)->out.append(k).append('=').append(v).append(';'));
                    return d+","+p+","+q+","+r1+","+r2+","+rp+","+rv+"|"+out+"|"+backing;
                }
                public static String putAllAndClear(){
                    Map<String,Integer> backing=backing();Map<String,Integer> m=Collections.synchronizedMap(backing);
                    m.putAll(Map.of("ten",10));String filled=backing.toString();m.clear();
                    return filled+"|"+backing.isEmpty()+"|"+m+"|"+m.isEmpty();
                }
                public static String equality(){
                    Map<String,Integer> backing=new TreeMap<>(backing());Map<String,Integer> m=Collections.synchronizedMap(backing);
                    return m.equals(m)+","+m.equals(backing)+","+backing.equals(m)+","+m.equals(new LinkedHashMap<>(backing))+","+(m.hashCode()==backing.hashCode())+","+m.hashCode()+","+m;
                }
                public static String viewsWriteThrough(){
                    Map<String,Integer> backing=backing();backing.put("three",3);Map<String,Integer> m=Collections.synchronizedMap(backing);
                    m.keySet().remove("one");m.values().remove(3);
                    for(Map.Entry<String,Integer> e:m.entrySet())e.setValue(e.getValue()*7);
                    String keys=m.keySet().toString(),values=m.values().toString(),entries=m.entrySet().toString();
                    backing.put("late",0);
                    return keys+"|"+values+"|"+entries+"|"+backing+"|"+m.keySet()+"|"+m.entrySet().size();
                }
                public static String viewIteratorRemove(){
                    Map<String,Integer> backing=backing();Map<String,Integer> m=Collections.synchronizedMap(backing);
                    Iterator<String> it=m.keySet().iterator();it.next();it.remove();m.entrySet().removeIf(e->e.getValue()==2);
                    return backing+"|"+m.isEmpty()+"|"+m.keySet().equals(Set.of());
                }
                public static String nullKeysAndValues(){
                    Map<String,Integer> m=Collections.synchronizedMap(new LinkedHashMap<>());
                    m.put(null,1);m.put("n",null);
                    return m.get(null)+","+m.get("n")+","+m.containsKey("n")+","+m.containsValue(null)+","+m.getOrDefault("n",5)+"|"+m;
                }
            """), "basics", "defaults", "putAllAndClear", "equality", "viewsWriteThrough", "viewIteratorRemove", "nullKeysAndValues");
    }

    @Test void nullBackingCollections() throws Exception {
        matchesJvm(fixture("""
                public static String nulls(){
                    return failure(()->Collections.synchronizedCollection(null))+","+failure(()->Collections.synchronizedSet(null))+","
                        +failure(()->Collections.synchronizedList(null))+","+failure(()->Collections.synchronizedMap(null));
                }
                public static String wrappingTwice(){
                    List<String> backing=abc();List<String> twice=Collections.synchronizedList(Collections.synchronizedList(backing));
                    twice.add("d");
                    return twice+"|"+backing+"|"+twice.equals(backing);
                }
                public static String wrapsUnmodifiable(){
                    List<String> l=Collections.synchronizedList(Collections.unmodifiableList(abc()));
                    return l+"|"+failure(()->l.add("x"))+"|"+failure(()->l.set(0,"x"));
                }
            """), "nulls", "wrappingTwice", "wrapsUnmodifiable");
    }

    @Test void leavesOutSortedVariants() {
        rejectsUnusedCall("java/util/Collections", "synchronizedSortedMap", "(Ljava/util/SortedMap;)Ljava/util/SortedMap;", INVOKESTATIC, false);
        rejectsUnusedCall("java/util/Collections", "synchronizedNavigableMap", "(Ljava/util/NavigableMap;)Ljava/util/NavigableMap;", INVOKESTATIC, false);
        rejectsUnusedCall("java/util/Collections", "synchronizedNavigableSet", "(Ljava/util/NavigableSet;)Ljava/util/NavigableSet;", INVOKESTATIC, false);
    }
}
