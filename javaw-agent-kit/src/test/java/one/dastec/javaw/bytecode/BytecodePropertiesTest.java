package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/**
 * T22: java.util.Properties on top of the runtime Hashtable. The JDK keeps the entries in a ConcurrentHashMap, so
 * multi-entry iteration order differs; every fixture here is order-independent (single entries, sorted names, sizes)
 * except propertyNames(), which returns a fresh Hashtable's keys() and uses keys that never share a bucket.
 * See tasks/T22-properties.md.
 */
class BytecodePropertiesTest extends KitDifferential {
    private static final String HELPERS = """
                static String failure(Runnable action){
                    try{action.run();return "no exception";}
                    catch(RuntimeException e){return e.getClass().getSimpleName()+": "+e.getMessage();}
                }
                static String type(Runnable action){
                    try{action.run();return "no exception";}
                    catch(RuntimeException e){return e.getClass().getSimpleName();}
                }
                static Properties props(String... pairs){Properties p=new Properties();for(int i=0;i<pairs.length;i+=2)p.setProperty(pairs[i],pairs[i+1]);return p;}
                static String names(Enumeration<?> e){StringBuilder out=new StringBuilder();while(e.hasMoreElements())out.append(e.nextElement()).append(' ');return out.toString().trim();}
                static String sorted(Collection<?> values){List<String> out=new ArrayList<>();for(Object v:values)out.add(String.valueOf(v));Collections.sort(out);return out.toString();}
                /** Exposes the protected defaults field, as library subclasses do. */
                static class Exposed extends Properties{
                    Exposed(){}
                    Exposed(Properties defaults){super(defaults);}
                    Properties defaults(){return defaults;}
                    void defaults(Properties d){defaults=d;}
                }
                /** A Map that records which of its methods another map calls. */
                static class Recording extends AbstractMap<Object,Object>{
                    final StringBuilder log=new StringBuilder();final Map<Object,Object> data=new LinkedHashMap<>();
                    Recording(Object... pairs){for(int i=0;i<pairs.length;i+=2)data.put(pairs[i],pairs[i+1]);}
                    public int size(){log.append("size ");return data.size();}
                    public Object get(Object k){log.append("get(").append(k).append(") ");return data.get(k);}
                    public boolean containsKey(Object k){log.append("containsKey(").append(k).append(") ");return data.containsKey(k);}
                    public Set<Map.Entry<Object,Object>> entrySet(){log.append("entrySet ");return data.entrySet();}
                    String take(){String s=log.toString().trim();log.setLength(0);return s;}
                }
                /** A library-style subclass that records every overridable call Properties makes on it. */
                static class Recorder extends Properties{
                    final StringBuilder log=new StringBuilder();
                    Recorder(){}
                    Recorder(Properties defaults){super(defaults);}
                    public Object put(Object k,Object v){log.append("put(").append(k).append('=').append(v).append(") ");return super.put(k,v);}
                    public Object get(Object k){log.append("get(").append(k).append(") ");return super.get(k);}
                    public String getProperty(String k){log.append("getProperty(").append(k).append(") ");return super.getProperty(k);}
                    public Set<Map.Entry<Object,Object>> entrySet(){log.append("entrySet ");return super.entrySet();}
                    public Set<Object> keySet(){log.append("keySet ");return super.keySet();}
                    public Enumeration<Object> keys(){log.append("keys ");return super.keys();}
                    public int size(){log.append("size ");return super.size();}
                    public boolean containsKey(Object k){log.append("containsKey(").append(k).append(") ");return super.containsKey(k);}
                    String take(){String s=log.toString().trim();log.setLength(0);return s;}
                }
            """;

    private static String fixture(String body) {
        return "import java.util.*;\nimport java.util.function.*;\npublic class Fixture {\n" + HELPERS + body + "}\n";
    }

    @Test void constructorsAndLookup() throws Exception {
        matchesJvm(fixture("""
                public static String constructors(){
                    Properties a=new Properties(),b=new Properties(0),c=new Properties(100),d=new Properties((Properties)null);
                    return a.size()+""+b.size()+c.size()+d.size()+"|"+a.isEmpty()+"|"+a+b+c+d+"|"+a.getProperty("x")+d.getProperty("x","dx")+"|"+a.equals(b)+"|"+a.hashCode();
                }
                public static String hierarchy(){
                    Object p=new Properties();
                    return (p instanceof Hashtable)+"|"+(p instanceof Dictionary)+"|"+(p instanceof Map)+"|"+(p instanceof Cloneable)+"|"+(p instanceof java.io.Serializable)+"|"+(p instanceof AbstractMap);
                }
                public static String negativeCapacity(){return failure(()->new Properties(-1))+"|"+failure(()->new Properties(Integer.MIN_VALUE));}
                public static String setAndGet(){
                    Properties p=new Properties();
                    Object first=p.setProperty("a","1"),second=p.setProperty("a","2");
                    return first+"|"+second+"|"+p.getProperty("a")+"|"+p.get("a")+"|"+p.getProperty("zz")+"|"+p.size()+"|"+p.containsKey("a")+p.contains("2")+p.containsValue("1")+"|"+p;
                }
                public static String getPropertyWithDefault(){
                    Properties p=props("a","1","empty","");
                    return p.getProperty("a","d")+"|"+p.getProperty("b","d")+"|"+p.getProperty("b",null)+"|["+p.getProperty("empty","d")+"]";
                }
                public static String nonStringEntries(){
                    Properties p=new Properties();p.put("n",5);p.put(7,"seven");p.put("flag",Boolean.TRUE);
                    Object old=p.setProperty("n","five");
                    return p.getProperty("flag")+"|"+p.getProperty("flag","d")+"|"+p.get("flag")+"|"+old+(old instanceof Integer)+"|"+p.getProperty("n")+"|"+p.get(7)+"|"+p.getProperty("7")+"|"+p.size();
                }
                public static String nullFailures(){
                    Properties p=props("a","1");
                    return failure(()->p.setProperty("a",null))+"|"+type(()->p.setProperty(null,"v"))+"|"+type(()->p.getProperty(null))+"|"+type(()->p.getProperty(null,"d"))+"|"+p;
                }
            """), "constructors", "hierarchy", "negativeCapacity", "setAndGet", "getPropertyWithDefault", "nonStringEntries", "nullFailures");
    }

    @Test void defaultsChain() throws Exception {
        matchesJvm(fixture("""
                public static String defaultsLookup(){
                    Properties d=props("a","da","b","db");Properties p=new Properties(d);p.setProperty("a","pa");
                    return p.getProperty("a")+"|"+p.getProperty("b")+"|"+p.getProperty("c")+"|"+p.getProperty("b","x")+p.getProperty("c","x")+"|"+p.get("b")+"|"+p.containsKey("b")+"|"+p.size()+"|"+p+"|"+d.getProperty("a");
                }
                public static String defaultsAreLive(){
                    Properties d=new Properties();Properties p=new Properties(d);
                    String before=p.getProperty("k");d.setProperty("k","v");String added=p.getProperty("k");
                    p.setProperty("k","own");String own=p.getProperty("k");p.remove("k");String back=p.getProperty("k");d.clear();
                    return before+"|"+added+"|"+own+"|"+back+"|"+p.getProperty("k");
                }
                public static String threeLevels(){
                    Properties grand=props("g","1","x","grand"),parent=new Properties(grand),child=new Properties(parent);
                    parent.setProperty("p","2");parent.setProperty("x","parent");child.setProperty("c","3");
                    return child.getProperty("g")+child.getProperty("p")+child.getProperty("c")+"|"+child.getProperty("x")+"|"+parent.getProperty("c")+"|"+child.size()+parent.size()+grand.size();
                }
                public static String nonStringValuesFallThrough(){
                    Properties grand=props("k","grand","m","gm"),parent=new Properties(grand),child=new Properties(parent);
                    parent.put("k",42);child.put("m",Boolean.TRUE);child.put("only",7);
                    return child.getProperty("k")+"|"+child.getProperty("m")+"|"+child.getProperty("only")+"|"+child.getProperty("only","fallback")+"|"+child.get("m")+"|"+parent.get("k");
                }
                public static String defaultsField(){
                    Properties d=props("a","1");Exposed none=new Exposed(),some=new Exposed(d);
                    String start=(none.defaults()==null)+"|"+(some.defaults()==d)+"|"+none.getProperty("a");
                    none.defaults(d);String assigned=none.getProperty("a");
                    some.defaults(null);
                    return start+"|"+assigned+"|"+some.getProperty("a")+"|"+some.getProperty("a","gone");
                }
                public static String defaultsAreNotEntries(){
                    Properties d=props("a","1");Properties p=new Properties(d),q=new Properties();
                    return p.equals(q)+"|"+p.isEmpty()+"|"+p+"|"+p.keySet()+"|"+p.contains("1")+"|"+p.remove("a")+"|"+p.getProperty("a")+"|"+(p.hashCode()==q.hashCode());
                }
            """), "defaultsLookup", "defaultsAreLive", "threeLevels", "nonStringValuesFallThrough", "defaultsField", "defaultsAreNotEntries");
    }

    @Test void propertyNamesAndStringPropertyNames() throws Exception {
        matchesJvm(fixture("""
                public static String propertyNamesOrder(){
                    Properties d=props("a","1","c","3","e","5"),p=new Properties(d);p.setProperty("b","2");p.setProperty("d","4");p.setProperty("a","own");
                    Enumeration<?> e=p.propertyNames();
                    return names(e)+"|"+names(d.propertyNames())+"|"+failure(()->e.nextElement());
                }
                public static String emptyNames(){
                    Properties p=new Properties();Enumeration<?> e=p.propertyNames();
                    return e.hasMoreElements()+"|"+failure(()->e.nextElement())+"|"+p.stringPropertyNames()+"|"+p.stringPropertyNames().isEmpty();
                }
                public static String propertyNamesKeepsNonStringValues(){
                    Properties d=new Properties();d.put("dv",1);Properties p=new Properties(d);p.put("pv",2);p.setProperty("s","x");
                    return names(p.propertyNames())+"|"+sorted(p.stringPropertyNames());
                }
                public static String propertyNamesRejectsNonStringKeys(){
                    Properties own=new Properties();own.put(1,"one");own.setProperty("a","x");
                    Properties d=new Properties();d.put(2,"two");Properties child=new Properties(d);child.setProperty("b","y");
                    return type(()->own.propertyNames())+"|"+type(()->child.propertyNames())+"|"+sorted(own.stringPropertyNames())+sorted(child.stringPropertyNames());
                }
                public static String propertyNamesIsACopy(){
                    Properties p=props("a","1","b","2");Enumeration<?> e=p.propertyNames();
                    p.setProperty("c","3");p.remove("a");
                    return names(e)+"|"+names(p.propertyNames());
                }
                public static String stringPropertyNames(){
                    Properties grand=props("g","1","k","grand"),parent=new Properties(grand),child=new Properties(parent);
                    parent.put("k",42);parent.put(3,"three");child.setProperty("c","2");child.put("n",5);
                    Set<String> names=child.stringPropertyNames();
                    return sorted(names)+"|"+names.size()+"|"+names.contains("k")+names.contains("n")+"|"+child.getProperty("k")+"|"+sorted(parent.stringPropertyNames());
                }
                public static String stringPropertyNamesIsAnUnmodifiableCopy(){
                    Properties p=props("a","1");Set<String> names=p.stringPropertyNames();
                    String add=failure(()->names.add("b")),remove=failure(()->names.remove("a")),clear=failure(()->names.clear());
                    String it=failure(()->{Iterator<String> i=names.iterator();i.next();i.remove();});
                    p.setProperty("b","2");p.remove("a");
                    return add+"|"+remove+"|"+clear+"|"+it+"|"+names+"|"+sorted(p.stringPropertyNames());
                }
            """), "propertyNamesOrder", "emptyNames", "propertyNamesKeepsNonStringValues", "propertyNamesRejectsNonStringKeys",
                "propertyNamesIsACopy", "stringPropertyNames", "stringPropertyNamesIsAnUnmodifiableCopy");
    }

    @Test void concurrentMapRules() throws Exception {
        matchesJvm(fixture("""
                public static String conditionalRemove(){
                    Properties p=props("a","1","b","2");
                    return p.remove("a",null)+"|"+p.remove("a","2")+"|"+p.remove("a","1")+"|"+type(()->p.remove(null,"1"))+"|"+type(()->p.remove(null,null))+"|"+p.remove("zz",null)+"|"+p;
                }
                public static String mergeRejectsNulls(){
                    Properties p=props("a","1");BiFunction<Object,Object,Object> concat=(x,y)->x+"+"+y;
                    return failure(()->p.merge("a",null,concat))+"|"+failure(()->p.merge("zz",null,concat))+"|"+type(()->p.merge(null,"v",concat))
                        +"|"+failure(()->p.merge("a","v",null))+"|"+p.merge("a","2",concat)+"|"+p.merge("b","3",concat)+"|"+p.merge("b","x",(x,y)->null)
                        +"|"+p.size()+p.getProperty("a")+p.getProperty("b");
                }
                public static String forEachMayRemove(){
                    Properties p=props("a","1","b","2","c","3");int[] seen={0};
                    String all=failure(()->p.forEach((k,v)->{seen[0]++;p.remove(k);}));
                    Properties q=props("a","1","b","2");int[] calls={0};
                    String other=failure(()->q.forEach((k,v)->{calls[0]++;q.remove(k.equals("a")?"b":"a");}));
                    return all+"|"+seen[0]+"|"+p+"|"+other+"|"+calls[0]+"|"+q.size();
                }
                public static String forEachUpdatesAndNulls(){
                    Properties p=props("a","1","b","2");int[] calls={0};
                    p.forEach((k,v)->{calls[0]++;p.setProperty((String)k,v+"!");});
                    return calls[0]+"|"+p.getProperty("a")+p.getProperty("b")+"|"+failure(()->new Properties().forEach(null))+"|"+failure(()->p.forEach(null));
                }
                public static String putAllStopsAtNull(){
                    Properties p=new Properties();Map<Object,Object> m=new LinkedHashMap<>();m.put("x","1");m.put("y",null);m.put("z","3");
                    return type(()->p.putAll(m))+"|"+p+"|"+type(()->p.putAll(null));
                }
                public static String toStringForms(){
                    Properties p=new Properties();String empty=p.toString();p.setProperty("k","v");String one=p.toString();
                    Properties number=new Properties();number.put(7,8);
                    return empty+"|"+one+"|"+number+"|"+props("x","")+"|"+p.keySet()+p.values()+p.entrySet();
                }
                public static String equalityAndHash(){
                    Properties p=props("a","1","b","2"),q=props("b","2","a","1");
                    Map<String,String> plain=new HashMap<>();plain.put("a","1");plain.put("b","2");
                    Hashtable<Object,Object> table=new Hashtable<>(plain);
                    return p.equals(q)+"|"+p.equals(plain)+"|"+plain.equals(p)+"|"+p.equals(table)+"|"+table.equals(p)+"|"+p.equals(props("a","1"))+"|"+p.equals(props("a","1","b","3"))
                        +"|"+p.equals("text")+"|"+p.equals(null)+"|"+p.equals(p)+"|"+(p.hashCode()==plain.hashCode())+"|"+(p.hashCode()==q.hashCode());
                }
                public static String equalsCallPattern(){
                    Properties p=props("a","1");
                    Recording same=new Recording("a","1"),other=new Recording("a","2"),bigger=new Recording("a","1","b","2"),nullValue=new Recording("a","1","b",null);
                    boolean s=p.equals(same),o=p.equals(other),b=p.equals(bigger),n=p.equals(nullValue);
                    return s+" "+same.take()+"|"+o+" "+other.take()+"|"+b+" "+bigger.take()+"|"+n+" "+nullValue.take();
                }
                public static String inheritedMapMembers(){
                    Properties p=props("a","1");
                    return p.putIfAbsent("a","x")+"|"+p.putIfAbsent("b","2")+"|"+p.computeIfAbsent("c",k->k+"3")+"|"+p.computeIfPresent("c",(k,v)->null)+"|"+p.compute("a",(k,v)->v+"!")
                        +"|"+p.replace("b","22")+"|"+p.replace("b","x","y")+"|"+p.getOrDefault("zz","dflt")+"|"+type(()->p.put("k",null))+"|"+type(()->p.contains(null))+"|"+p.size()+p.getProperty("a")+p.getProperty("b");
                }
            """), "conditionalRemove", "mergeRejectsNulls", "forEachMayRemove", "forEachUpdatesAndNulls", "putAllStopsAtNull", "toStringForms",
                "equalityAndHash", "equalsCallPattern", "inheritedMapMembers");
    }

    @Test void subclassCallPatterns() throws Exception {
        matchesJvm(fixture("""
                public static String setPropertyCallsPut(){
                    Recorder r=new Recorder();Object first=r.setProperty("a","1");Object second=r.setProperty("a","2");
                    return first+"|"+second+"|"+r.take();
                }
                public static String getPropertyCalls(){
                    Recorder r=new Recorder();r.put("a","1");r.take();
                    String v=r.getProperty("a");String one=r.take();
                    String w=r.getProperty("zz","d");String two=r.take();
                    Object x=r.get("a");String three=r.take();
                    return v+":"+one+"|"+w+":"+two+"|"+x+":"+three;
                }
                public static String defaultsCallGetProperty(){
                    Recorder d=new Recorder();d.put("k","v");d.take();Properties p=new Properties(d);
                    p.setProperty("own","x");
                    String a=p.getProperty("k"),b=p.getProperty("own"),c=p.getProperty("zz","dflt");
                    return a+b+c+"|"+d.take();
                }
                public static String putAllBypassesPut(){
                    Recorder r=new Recorder();Recording source=new Recording("a","1","b","2");
                    r.putAll(source);
                    return r.take()+"|"+source.take()+"|"+r.getProperty("a")+r.getProperty("b")+"|"+r.take()+"|"+type(()->r.putAll(null));
                }
                public static String namesUseEntrySet(){
                    Recorder d=new Recorder();d.setProperty("a","1");Recorder r=new Recorder(d);r.setProperty("b","2");d.take();r.take();
                    String names=names(r.propertyNames());String first=r.take()+"/"+d.take();
                    String strings=sorted(r.stringPropertyNames());String second=r.take()+"/"+d.take();
                    return names+"|"+first+"|"+strings+"|"+second;
                }
                static class Hidden extends Properties{
                    public Set<Map.Entry<Object,Object>> entrySet(){
                        Set<Map.Entry<Object,Object>> visible=new HashSet<>();
                        for(Map.Entry<Object,Object> e:super.entrySet())if(!String.valueOf(e.getKey()).startsWith("_"))visible.add(e);
                        return visible;
                    }
                }
                public static String overriddenEntrySet(){
                    Hidden h=new Hidden();h.setProperty("_secret","s");h.setProperty("a","1");
                    Properties child=new Properties(h);child.setProperty("c","3");
                    Hidden one=new Hidden();one.setProperty("_only","x");
                    return names(h.propertyNames())+"|"+sorted(h.stringPropertyNames())+"|"+sorted(child.stringPropertyNames())+"|"+h.getProperty("_secret")+child.getProperty("_secret")
                        +"|"+h.size()+"|"+one+"|"+one.equals(props("_only","x"));
                }
                public static String objectMethodsIgnoreOverrides(){
                    Recorder r=new Recorder();r.setProperty("a","1");r.take();
                    String s=r.toString();boolean e=r.equals(props("a","1"));boolean h=r.hashCode()==props("a","1").hashCode();boolean empty=r.isEmpty();
                    int[] calls={0};r.forEach((k,v)->calls[0]++);
                    Object c=r.clone();
                    String own=r.take();
                    boolean self=r.equals(r);String selfLog=r.take();
                    boolean reverse=props("a","1").equals(r);
                    return s+"|"+e+h+empty+calls[0]+"|["+own+"]|"+(c instanceof Recorder)+"|"+self+" "+selfLog+"|"+reverse+" "+r.take();
                }
                public static String cloneIsIndependentAndSharesDefaults(){
                    Properties d=props("dk","dv");Exposed p=new Exposed(d);p.setProperty("a","1");
                    Exposed c=(Exposed)p.clone();c.setProperty("a","2");c.setProperty("b","3");d.setProperty("late","x");
                    return p+"|"+c.getProperty("a")+c.getProperty("b")+"|"+(c.defaults()==d)+"|"+c.getProperty("dk")+c.getProperty("late")+"|"+p.size()+c.size()+"|"+p.getProperty("b");
                }
            """), "setPropertyCallsPut", "getPropertyCalls", "defaultsCallGetProperty", "putAllBypassesPut", "namesUseEntrySet",
                "overriddenEntrySet", "objectMethodsIgnoreOverrides", "cloneIsIndependentAndSharesDefaults");
    }

    @Test void leavesOutRehashAndXml() {
        rejectsUnusedCall("java/util/Properties", "rehash", "()V", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/Properties", "loadFromXML", "(Ljava/io/InputStream;)V", INVOKEVIRTUAL, false);
    }
}
