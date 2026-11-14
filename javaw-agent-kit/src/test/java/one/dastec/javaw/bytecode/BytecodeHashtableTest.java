package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** T21: java.util.Dictionary and a monitor-free java.util.Hashtable with the JDK bucket layout. See tasks/T21-hashtable.md. */
@Tag("agent-pending")
class BytecodeHashtableTest extends KitDifferential {
    private static final String HELPERS = """
                static String failure(Runnable action){
                    try{action.run();return "no exception";}
                    catch(RuntimeException e){return e.getClass().getSimpleName()+": "+e.getMessage();}
                }
                static String type(Runnable action){
                    try{action.run();return "no exception";}
                    catch(RuntimeException e){return e.getClass().getSimpleName();}
                }
                static Hashtable<String,Integer> abc(){Hashtable<String,Integer> t=new Hashtable<>();t.put("a",1);t.put("b",2);t.put("c",3);return t;}
                static Hashtable<Integer,String> ints(int... keys){Hashtable<Integer,String> t=new Hashtable<>();for(int k:keys)t.put(k,"v"+k);return t;}
                static String keys(Hashtable<?,?> t){StringBuilder out=new StringBuilder();for(Enumeration<?> e=t.keys();e.hasMoreElements();)out.append(e.nextElement()).append(' ');return out.toString().trim();}
            """;

    private static String fixture(String body) {
        return "import java.util.*;\nimport java.util.function.*;\npublic class Fixture {\n" + HELPERS + body + "}\n";
    }

    @Test void dictionaryConstructorsAndBasics() throws Exception {
        matchesJvm(fixture("""
                static class OneSlot extends Dictionary<String,String>{
                    String key,value;
                    public int size(){return key==null?0:1;}
                    public boolean isEmpty(){return key==null;}
                    public Enumeration<String> keys(){return Collections.enumeration(key==null?List.<String>of():List.of(key));}
                    public Enumeration<String> elements(){return Collections.enumeration(value==null?List.<String>of():List.of(value));}
                    public String get(Object k){return k.equals(key)?value:null;}
                    public String put(String k,String v){String old=k.equals(key)?value:null;key=k;value=v;return old;}
                    public String remove(Object k){if(!k.equals(key))return null;String old=value;key=null;value=null;return old;}
                }
                public static String customDictionary(){
                    Dictionary<String,String> d=new OneSlot();boolean empty=d.isEmpty();
                    String first=d.put("k","v"),second=d.put("k","w");
                    return empty+"|"+first+"|"+second+"|"+d.get("k")+"|"+d.size()+"|"+d.keys().nextElement()+d.elements().nextElement()+"|"+d.remove("k")+d.isEmpty();
                }
                public static String hashtableAsDictionary(){
                    Dictionary<String,Integer> d=new Hashtable<>();d.put("x",1);d.put("y",2);
                    Integer old=d.put("x",10);
                    return old+"|"+d.get("x")+"|"+d.size()+"|"+d.remove("y")+"|"+d.remove("zz")+"|"+d.isEmpty()+"|"+d.keys().nextElement()+"="+d.elements().nextElement();
                }
                public static String interfaces(){
                    Object t=new Hashtable<String,String>();
                    return (t instanceof Dictionary)+"|"+(t instanceof Map)+"|"+(t instanceof Cloneable)+"|"+(t instanceof java.io.Serializable)+"|"+(t instanceof AbstractMap);
                }
                public static String constructors(){
                    Hashtable<Integer,String> a=new Hashtable<>(),b=new Hashtable<>(0),c=new Hashtable<>(3,0.5f),d=new Hashtable<>(1,10f);
                    for(int i=0;i<6;i++){a.put(i,"a");b.put(i,"b");c.put(i,"c");d.put(i,"d");}
                    return a.size()+""+b.size()+c.size()+d.size()+"|"+a+"|"+b+"|"+c+"|"+d;
                }
                public static String constructorFailures(){
                    return failure(()->new Hashtable<String,String>(-1))+"|"+failure(()->new Hashtable<String,String>(-5,0.75f))
                        +"|"+failure(()->new Hashtable<String,String>(4,0f))+"|"+failure(()->new Hashtable<String,String>(4,-1f))
                        +"|"+failure(()->new Hashtable<String,String>(4,Float.NaN))+"|"+failure(()->new Hashtable<String,String>(-1,0f));
                }
                public static String fromMap(){
                    Map<String,Integer> source=new LinkedHashMap<>();
                    for(int i=0;i<14;i++)source.put("k"+i,i);
                    Hashtable<String,Integer> t=new Hashtable<>(source);
                    Hashtable<String,Integer> small=new Hashtable<>(Map.of("one",1));
                    return t.size()+"|"+t+"|"+small;
                }
                public static String fromNullMap(){return type(()->new Hashtable<String,String>((Map<String,String>)null));}
                public static String putAndGet(){
                    Hashtable<String,Integer> t=new Hashtable<>();boolean empty=t.isEmpty();
                    Integer first=t.put("a",1),second=t.put("a",2);t.put("b",3);
                    return empty+"|"+first+"|"+second+"|"+t.get("a")+"|"+t.get("b")+"|"+t.get("zz")+"|"+t.size()+"|"+t.isEmpty();
                }
                public static String containment(){
                    Hashtable<String,Integer> t=abc();
                    return t.containsKey("a")+","+t.containsKey("zz")+","+t.contains(2)+","+t.contains(9)+","+t.containsValue(3)+","+t.containsValue(4)+","+t.containsKey(1);
                }
                public static String removeAndClear(){
                    Hashtable<String,Integer> t=abc();Integer r=t.remove("b"),missing=t.remove("zz");
                    String mid=t+"|"+t.size();t.clear();
                    String cleared=t+"|"+t.size()+"|"+t.isEmpty();t.put("again",7);
                    return r+"|"+missing+"|"+mid+"|"+cleared+"|"+t;
                }
                public static String putAll(){
                    Hashtable<String,Integer> t=abc();Map<String,Integer> more=new LinkedHashMap<>();more.put("b",20);more.put("d",4);
                    t.putAll(more);
                    return t+"|"+t.size();
                }
                public static String putAllStopsAtNullValue(){
                    Hashtable<String,Integer> t=new Hashtable<>();Map<String,Integer> more=new LinkedHashMap<>();more.put("x",1);more.put("y",null);more.put("z",3);
                    return failure(()->t.putAll(more))+"|"+t;
                }
                public static String explicitNullFailures(){
                    Hashtable<String,Integer> t=abc();
                    return failure(()->t.put("a",null))+"|"+failure(()->t.put(null,null))+"|"+failure(()->t.contains(null))+"|"+failure(()->t.containsValue(null))+"|"+t;
                }
                public static String nullKeyFailures(){
                    Hashtable<String,Integer> t=abc();
                    return type(()->t.put(null,1))+","+type(()->t.get(null))+","+type(()->t.containsKey(null))+","+type(()->t.remove(null))+","+type(()->t.putAll(null))+"|"+t;
                }
                public static String emptyTable(){
                    Hashtable<String,Integer> t=new Hashtable<>();
                    return t+"|"+t.get("a")+"|"+t.remove("a")+"|"+t.containsKey("a")+"|"+t.contains(1)+"|"+t.keySet()+t.values()+t.entrySet();
                }
            """), "customDictionary", "hashtableAsDictionary", "interfaces", "constructors", "constructorFailures", "fromMap",
                "fromNullMap", "putAndGet", "containment", "removeAndClear", "putAll", "putAllStopsAtNullValue", "explicitNullFailures",
                "nullKeyFailures", "emptyTable");
    }

    @Test void bucketLayoutAndOrder() throws Exception {
        matchesJvm(fixture("""
                public static String sameBucketNewestFirst(){return ints(0,11,22,33).toString()+"|"+keys(ints(1,12,2,13));}
                public static String stringOrder(){
                    Hashtable<String,Integer> t=new Hashtable<>();
                    for(String s:new String[]{"apple","banana","cherry","date","elderberry","fig","grape"})t.put(s,s.length());
                    return t.toString();
                }
                public static String negativeHashes(){
                    Hashtable<Integer,String> t=ints(-1,-12,Integer.MIN_VALUE,-100,5);
                    return keys(t)+"|"+t.get(Integer.MIN_VALUE)+t.get(-12);
                }
                public static String firstRehash(){
                    Hashtable<Integer,String> eight=ints(0,1,2,3,4,5,6,7,11,12,13,14,15,16,17,18);
                    StringBuilder out=new StringBuilder();
                    for(int n=7;n<=10;n++){int[] k=new int[n];for(int i=0;i<n;i++)k[i]=i*11+i%3;out.append(keys(ints(k))).append('/');}
                    return out+"|"+keys(eight);
                }
                public static String manyRehashes(){
                    Hashtable<Integer,String> t=new Hashtable<>();
                    for(int i=0;i<60;i+=3)t.put(i*7,"x");
                    for(int i=100;i<150;i+=7)t.put(i,"y");
                    return keys(t)+"|"+t.size();
                }
                public static String smallCapacityGrowth(){
                    Hashtable<Integer,String> t=new Hashtable<>(3);StringBuilder out=new StringBuilder();
                    for(int i=0;i<9;i++){t.put(i*5,"v");out.append(keys(t)).append('/');}
                    return out.toString();
                }
                public static String zeroCapacityLayout(){
                    Hashtable<Integer,String> zero=new Hashtable<>(0),one=new Hashtable<>(1),big=new Hashtable<>(1,10f);
                    for(int k:new int[]{0,3,6,9}){zero.put(k,"z");one.put(k,"o");big.put(k,"b");}
                    return keys(zero)+"/"+keys(one)+"/"+keys(big);
                }
                public static String removeKeepsOrder(){
                    Hashtable<Integer,String> t=ints(0,11,22,33,1,12);t.remove(22);t.remove(1);t.put(44,"n");
                    return t.toString();
                }
                public static String replacingKeepsPosition(){
                    Hashtable<Integer,String> t=ints(0,11,22);t.put(11,"new");
                    return t.toString();
                }
            """), "sameBucketNewestFirst", "stringOrder", "negativeHashes", "firstRehash", "manyRehashes", "smallCapacityGrowth",
                "zeroCapacityLayout", "removeKeepsOrder", "replacingKeepsPosition");
    }

    @Test void enumerationsAndIterators() throws Exception {
        matchesJvm(fixture("""
                public static String keysAndElements(){
                    Hashtable<Integer,String> t=ints(3,14,25,4);StringBuilder out=new StringBuilder();
                    for(Enumeration<String> e=t.elements();e.hasMoreElements();)out.append(e.nextElement()).append(' ');
                    return keys(t)+"|"+out.toString().trim();
                }
                public static String exhausted(){
                    Hashtable<String,Integer> t=abc();Enumeration<String> e=t.keys();while(e.hasMoreElements())e.nextElement();
                    Iterator<String> it=t.keySet().iterator();while(it.hasNext())it.next();
                    return failure(()->e.nextElement())+"|"+failure(()->it.next())+"|"+e.hasMoreElements()+it.hasNext();
                }
                public static String emptyEnumerations(){
                    Hashtable<String,Integer> t=new Hashtable<>();
                    return t.keys().hasMoreElements()+"|"+failure(()->t.keys().nextElement())+"|"+failure(()->t.elements().nextElement())
                        +"|"+failure(()->t.keySet().iterator().next())+"|"+failure(()->t.entrySet().iterator().remove());
                }
                public static String emptyIteratorStaysEmpty(){
                    Hashtable<String,Integer> t=new Hashtable<>();Iterator<String> it=t.keySet().iterator();Enumeration<Integer> e=t.elements();
                    t.put("a",1);
                    return it.hasNext()+"|"+e.hasMoreElements()+"|"+t.keySet().iterator().hasNext();
                }
                public static String iteratorRemove(){
                    Hashtable<Integer,String> t=ints(1,2,3,4,5);Iterator<Integer> it=t.keySet().iterator();
                    StringBuilder out=new StringBuilder();
                    while(it.hasNext()){int k=it.next();out.append(k);if(k%2==0)it.remove();}
                    return out+"|"+t+"|"+t.size();
                }
                public static String iteratorRemoveFailures(){
                    Hashtable<String,Integer> t=abc();Iterator<Map.Entry<String,Integer>> it=t.entrySet().iterator();
                    String before=failure(()->it.remove());it.next();it.remove();
                    return before+"|"+failure(()->it.remove())+"|"+t.size();
                }
                public static String failFast(){
                    Hashtable<String,Integer> t=abc();Iterator<String> it=t.keySet().iterator();it.next();t.put("d",4);
                    String added=type(()->it.next());
                    Iterator<Integer> values=t.values().iterator();values.next();t.remove("a");
                    String removed=type(()->values.next());
                    Iterator<String> again=t.keySet().iterator();again.next();t.put("b",22);
                    return added+"|"+removed+"|"+again.next()+"|"+again.hasNext();
                }
                public static String enumerationIsNotFailFast(){
                    Hashtable<String,Integer> t=abc();Enumeration<String> e=t.keys();String first=e.nextElement();t.remove("zz");t.remove(first);
                    StringBuilder out=new StringBuilder(first).append(':');while(e.hasMoreElements())out.append(e.nextElement());
                    return out.toString();
                }
            """), "keysAndElements", "exhausted", "emptyEnumerations", "emptyIteratorStaysEmpty", "iteratorRemove",
                "iteratorRemoveFailures", "failFast", "enumerationIsNotFailFast");
    }

    @Test void liveViews() throws Exception {
        matchesJvm(fixture("""
                public static String viewsAreLive(){
                    Hashtable<String,Integer> t=abc();Set<String> keys=t.keySet();Collection<Integer> values=t.values();Set<Map.Entry<String,Integer>> entries=t.entrySet();
                    String before=keys+"|"+values+"|"+entries;t.put("d",4);t.remove("a");
                    return before+"|"+keys+"|"+values+"|"+entries+"|"+keys.size()+values.size()+entries.size()+"|"+(t.keySet()==keys)+(t.values()==values)+(t.entrySet()==entries);
                }
                public static String keySetOperations(){
                    Hashtable<String,Integer> t=abc();t.put("d",4);Set<String> keys=t.keySet();
                    boolean a=keys.contains("b"),b=keys.contains("zz"),c=keys.remove("b"),d=keys.remove("zz");
                    keys.removeAll(List.of("a"));
                    return a+","+b+","+c+","+d+"|"+t+"|"+type(()->keys.add("q"))+"|"+keys.isEmpty();
                }
                public static String valuesOperations(){
                    Hashtable<String,Integer> t=abc();t.put("d",2);Collection<Integer> values=t.values();
                    boolean a=values.contains(2),b=values.remove(2),c=values.remove(9);
                    String mid=t.toString();values.clear();
                    return a+","+b+","+c+"|"+mid+"|"+t+t.isEmpty();
                }
                public static String entryOperations(){
                    Hashtable<String,Integer> t=abc();Set<Map.Entry<String,Integer>> entries=t.entrySet();
                    boolean a=entries.contains(Map.entry("a",1)),b=entries.contains(Map.entry("a",2)),c=entries.contains("a"),d=entries.contains(Map.entry("zz",1));
                    boolean e=entries.remove(Map.entry("b",9)),f=entries.remove(Map.entry("b",2)),g=entries.remove("b");
                    return a+","+b+","+c+","+d+","+e+","+f+","+g+"|"+t+"|"+type(()->entries.add(Map.entry("z",26)));
                }
                public static String entriesWriteThrough(){
                    Hashtable<String,Integer> t=abc();StringBuilder out=new StringBuilder();
                    for(Map.Entry<String,Integer> e:t.entrySet()){out.append(e).append(',').append(e.getKey()).append(e.getValue()).append(';');e.setValue(e.getValue()*10);}
                    Map.Entry<String,Integer> first=t.entrySet().iterator().next();
                    return out+"|"+t+"|"+first.setValue(5)+t.get(first.getKey())+"|"+failure(()->first.setValue(null));
                }
                public static String entryEqualityAndHash(){
                    Hashtable<String,Integer> t=new Hashtable<>();t.put("key",42);Map.Entry<String,Integer> e=t.entrySet().iterator().next();
                    Map.Entry<String,Integer> same=new AbstractMap.SimpleEntry<>("key",42);
                    return e.equals(same)+","+same.equals(e)+","+(e.hashCode()==same.hashCode())+","+e.hashCode()+","+e.equals(Map.entry("key",43))+","+e.equals("key=42");
                }
                public static String bulkViewOperations(){
                    Hashtable<Integer,String> t=ints(1,2,3,4,5,6);t.keySet().retainAll(List.of(2,4,6,8));
                    Hashtable<Integer,String> u=ints(1,2,3,4,5,6);u.values().removeIf(v->v.compareTo("v3")<0);
                    Hashtable<Integer,String> w=ints(1,2,3);w.entrySet().clear();
                    return t+"|"+u+"|"+w+w.size()+"|"+t.keySet().containsAll(List.of(2,6))+t.entrySet().equals(ints(2,4,6).entrySet())+"|"+t.keySet().hashCode();
                }
            """), "viewsAreLive", "keySetOperations", "valuesOperations", "entryOperations", "entriesWriteThrough",
                "entryEqualityAndHash", "bulkViewOperations");
    }

    @Test void copiesEqualityAndHashing() throws Exception {
        matchesJvm(fixture("""
                public static String cloneIsIndependent(){
                    Hashtable<String,Integer> t=abc();
                    @SuppressWarnings("unchecked") Hashtable<String,Integer> copy=(Hashtable<String,Integer>)t.clone();
                    copy.put("a",100);copy.remove("b");copy.put("d",4);t.put("c",30);
                    copy.entrySet().iterator().next().setValue(-1);
                    return t+"|"+copy+"|"+(copy!=t)+"|"+(copy.getClass()==Hashtable.class);
                }
                static class Sub extends Hashtable<String,Integer>{}
                public static String subclassClone(){
                    Sub s=new Sub();s.put("x",1);Object c=s.clone();s.put("y",2);
                    return (c.getClass()==Sub.class)+"|"+c+"|"+s+"|"+((Hashtable<?,?>)c).keySet();
                }
                public static String cloneThenIterate(){
                    Hashtable<Integer,String> t=ints(0,11,22,1);Hashtable<?,?> c=(Hashtable<?,?>)t.clone();
                    Iterator<?> it=c.keySet().iterator();String seen=""+it.next()+it.next()+it.next();it.remove();
                    return seen+"|"+keys(t)+"|"+keys(c)+"|"+c.size()+t.size();
                }
                public static String equality(){
                    Hashtable<String,Integer> t=abc();Map<String,Integer> m=new HashMap<>();m.put("c",3);m.put("a",1);m.put("b",2);
                    Map<String,Integer> other=new TreeMap<>(m);other.put("b",9);
                    return t.equals(m)+","+m.equals(t)+","+t.equals(abc())+","+t.equals(other)+","+t.equals(null)+","+t.equals("x")+","+t.equals(t)+","+new Hashtable<String,Integer>().equals(Map.of())+","+t.equals(Map.of("a",1));
                }
                public static String hashing(){
                    Hashtable<String,Integer> t=abc();Map<String,Integer> m=new HashMap<>(t);
                    return t.hashCode()+","+(t.hashCode()==m.hashCode())+","+new Hashtable<String,Integer>().hashCode()+","+ints(1,2,3).hashCode();
                }
                public static String stringForms(){
                    Hashtable<String,Object> t=new Hashtable<>();t.put("list",List.of(1,2));t.put("n",7);
                    return new Hashtable<String,String>()+"|"+t+"|"+t.keySet()+"|"+t.values();
                }
            """), "cloneIsIndependent", "subclassClone", "cloneThenIterate", "equality", "hashing", "stringForms");
    }

    @Test void mapDefaultOverrides() throws Exception {
        matchesJvm(fixture("""
                public static String forEachOrder(){
                    Hashtable<Integer,String> t=ints(1,12,2,13);StringBuilder out=new StringBuilder();
                    t.forEach((k,v)->out.append(k).append(' '));
                    return out.toString().trim()+"|"+keys(t)+"|"+failure(()->t.forEach(null))+"|"+failure(()->new Hashtable<String,String>().forEach(null));
                }
                public static String forEachFailFast(){
                    Hashtable<String,Integer> t=abc();int[] calls={0};
                    String r=type(()->t.forEach((k,v)->{calls[0]++;if(calls[0]==1)t.put("new",0);}));
                    Hashtable<String,Integer> u=abc();
                    String same=type(()->u.forEach((k,v)->u.put(k,v+1)));
                    return r+calls[0]+"|"+same+"|"+u;
                }
                public static String replaceAll(){
                    Hashtable<Integer,String> t=ints(1,12,2,13);t.replaceAll((k,v)->v+"!"+k);
                    Hashtable<Integer,String> u=ints(1,12,2,13);
                    String failed=failure(()->u.replaceAll((k,v)->k==2?null:"x"));
                    return t+"|"+failed+"|"+u+"|"+failure(()->u.replaceAll(null));
                }
                public static String getOrDefault(){
                    Hashtable<String,Integer> t=abc();
                    return t.getOrDefault("a",0)+","+t.getOrDefault("zz",-1)+","+t.getOrDefault("zz",null)+","+type(()->t.getOrDefault(null,1));
                }
                public static String putIfAbsent(){
                    Hashtable<String,Integer> t=abc();Integer a=t.putIfAbsent("a",10),b=t.putIfAbsent("d",4);
                    return a+","+b+"|"+t+"|"+failure(()->t.putIfAbsent("a",null))+"|"+failure(()->t.putIfAbsent("e",null))+"|"+type(()->t.putIfAbsent(null,1));
                }
                public static String conditionalRemoveAndReplace(){
                    Hashtable<String,Integer> t=abc();
                    boolean a=t.remove("a",9),b=t.remove("a",1),c=t.remove("zz",1);
                    boolean d=t.replace("b",9,20),e=t.replace("b",2,20),f=t.replace("zz",1,2);
                    Integer g=t.replace("c",30),h=t.replace("zz",1);
                    return a+","+b+","+c+","+d+","+e+","+f+","+g+","+h+"|"+t+"|"+failure(()->t.remove("b",null))+","+failure(()->t.replace("b",null))
                        +","+failure(()->t.replace("b",20,null))+","+failure(()->t.replace("b",null,1))+","+failure(()->t.replace("zz",null));
                }
                public static String computeIfAbsent(){
                    Hashtable<String,Integer> t=abc();int[] calls={0};
                    Integer a=t.computeIfAbsent("a",k->{calls[0]++;return 99;}),b=t.computeIfAbsent("dd",k->{calls[0]++;return k.length();}),c=t.computeIfAbsent("e",k->null);
                    return a+","+b+","+c+","+calls[0]+"|"+t+"|"+failure(()->t.computeIfAbsent("x",null))+"|"+type(()->t.computeIfAbsent(null,k->1));
                }
                public static String computeIfAbsentFailFast(){
                    Hashtable<String,Integer> t=abc();
                    String r=type(()->t.computeIfAbsent("x",k->{t.put("y",1);return 2;}));
                    return r+"|"+t.containsKey("x")+t.containsKey("y")+"|"+t.size();
                }
                public static String inheritedDefaults(){
                    Hashtable<String,Integer> t=abc();
                    Integer a=t.merge("a",5,Integer::sum),b=t.merge("m",7,Integer::sum),c=t.compute("b",(k,v)->v==null?0:v*100),d=t.computeIfPresent("c",(k,v)->null),e=t.computeIfPresent("zz",(k,v)->1);
                    return a+","+b+","+c+","+d+","+e+"|"+t;
                }
            """), "forEachOrder", "forEachFailFast", "replaceAll", "getOrDefault", "putIfAbsent", "conditionalRemoveAndReplace",
                "computeIfAbsent", "computeIfAbsentFailFast", "inheritedDefaults");
    }

    @Test void leavesOutRehash() {
        rejectsUnusedCall("java/util/Hashtable", "rehash", "()V", INVOKEVIRTUAL, false);
    }
}
