package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** T38: java.util.EnumMap. Expected values come from the JVM; see tasks/T38-enum-map.md. */
@Tag("agent-pending")
class BytecodeEnumMapTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.util.AbstractMap;
        import java.util.Collection;
        import java.util.EnumMap;
        import java.util.HashMap;
        import java.util.Iterator;
        import java.util.LinkedHashMap;
        import java.util.List;
        import java.util.Map;
        import java.util.Set;
        import java.util.TreeMap;

        enum Color{RED,GREEN,BLUE,YELLOW,BLACK}
        enum Fruit{APPLE,PEAR}
        /** Constants with bodies: each constant's getClass() is an anonymous subclass (Op$1, Op$2, Op$3). */
        enum Op{
            PLUS{int apply(int a,int b){return a+b;}},
            MINUS{int apply(int a,int b){return a-b;}},
            TIMES{int apply(int a,int b){return a*b;}};
            abstract int apply(int a,int b);
        }
        /** toString differs from name(): map toString uses toString. */
        enum Level{LOW,MID,HIGH;public String toString(){return "lvl-"+ordinal();}}
        enum Empty{}

        public class Fixture {
            interface Op0{Object run() throws Exception;}
            /** The value, or "SimpleName:message" of the exception. */
            static String t(Op0 op){
                try{return String.valueOf(op.run());}catch(Exception e){return e.getClass().getSimpleName()+":"+e.getMessage();}
            }
            /** The value, or only the exception's simple class name. */
            static String ty(Op0 op){
                try{return String.valueOf(op.run());}catch(Exception e){return e.getClass().getSimpleName();}
            }
            static EnumMap<Color,String> colors(){
                EnumMap<Color,String> m=new EnumMap<>(Color.class);
                m.put(Color.BLUE,"b");m.put(Color.RED,"r");m.put(Color.BLACK,"k");
                return m;
            }

            // ---- constructors ----
            public static String constructors(){
                EnumMap<Color,String> a=colors();
                EnumMap<Color,String> b=new EnumMap<>(a);b.put(Color.GREEN,"g");b.remove(Color.RED);
                EnumMap<Color,String> c=new EnumMap<>((Map<Color,String>)a);c.put(Color.YELLOW,"y");
                LinkedHashMap<Color,String> l=new LinkedHashMap<>();l.put(Color.YELLOW,"1");l.put(Color.RED,"2");l.put(Color.YELLOW,"3");
                EnumMap<Color,String> d=new EnumMap<>(l);
                LinkedHashMap<Op,Integer> ops=new LinkedHashMap<>();ops.put(Op.TIMES,3);ops.put(Op.PLUS,1);
                EnumMap<Op,Integer> e=new EnumMap<>(ops);e.put(Op.MINUS,2);
                EnumMap<Color,String> f=new EnumMap<>((Map<Color,String>)new EnumMap<Color,String>(Color.class));f.put(Color.GREEN,"g");
                TreeMap<Level,String> tm=new TreeMap<>();tm.put(Level.HIGH,"h");tm.put(Level.LOW,null);
                EnumMap<Empty,String> z=new EnumMap<>(Empty.class);
                return a+" "+b+" "+c+" "+d+" "+e+" "+f+" "+new EnumMap<>(tm)+" "+z+" "+z.size()+" "+z.isEmpty()+" "+new EnumMap<>(z)+" "+new EnumMap<>(Color.class).isEmpty()
                        +" "+a.size()+" "+b.size()+" "+c.size()+" "+d.size()+" "+e.size();
            }
            @SuppressWarnings({"unchecked","rawtypes"})
            public static String constructorFailures(){
                LinkedHashMap mixed=new LinkedHashMap();mixed.put(Color.BLUE,"b");mixed.put(Fruit.PEAR,"p");
                LinkedHashMap nullKey=new LinkedHashMap();nullKey.put(null,"n");
                return t(()->new EnumMap<Color,String>(new HashMap<Color,String>()))+" "+t(()->new EnumMap<Color,String>(new TreeMap<Color,String>()))
                        +" "+ty(()->new EnumMap((Class)String.class))+" "+ty(()->new EnumMap((Class)Op.PLUS.getClass()))+" "+ty(()->new EnumMap((Class)null))
                        +" "+ty(()->new EnumMap((EnumMap)null))+" "+ty(()->new EnumMap((Map)null))+" "+ty(()->new EnumMap(nullKey))+" "+t(()->new EnumMap(mixed));
            }

            // ---- single mappings ----
            public static String putGetRemove(){
                EnumMap<Color,String> m=new EnumMap<>(Color.class);
                String r=m.put(Color.BLUE,"b")+" "+m.put(Color.BLUE,"B")+" "+m.put(Color.RED,"r")+" "+m+" "+m.size()+" "+m.get(Color.BLUE)+" "+m.get(Color.GREEN)
                        +" "+m.containsKey(Color.RED)+" "+m.containsKey(Color.GREEN)+" "+m.containsValue("B")+" "+m.containsValue("b")+" "+m.containsValue(null)
                        +" "+m.remove(Color.RED)+" "+m.remove(Color.RED)+" "+m.size()+" "+m;
                m.put(Color.BLACK,"k");m.put(Color.GREEN,"g");
                r+=" "+m+" "+m.isEmpty()+" "+m.getOrDefault(Color.YELLOW,"d")+" "+m.putIfAbsent(Color.GREEN,"x")+" "+m.merge(Color.BLACK,"!",String::concat)+" "+m;
                m.clear();
                r+=" "+m+" "+m.size()+" "+m.isEmpty()+" "+m.get(Color.BLUE)+" "+m.put(Color.BLUE,"again")+" "+m;
                EnumMap<Op,Integer> o=new EnumMap<>(Op.class);o.put(Op.MINUS,2);o.put(Op.PLUS,1);
                return r+" "+o+" "+o.get(Op.PLUS)+" "+o.containsKey(Op.TIMES)+" "+o.keySet().iterator().next().apply(4,5);
            }
            public static String nullValues(){
                EnumMap<Color,String> m=new EnumMap<>(Color.class);
                String r=m.containsValue(null)+" "+m.put(Color.GREEN,null)+" "+m.size()+" "+m.containsKey(Color.GREEN)+" "+m.containsValue(null)+" "+m.get(Color.GREEN)
                        +" "+m.put(Color.GREEN,null)+" "+m.size()+" "+m+" "+m.getOrDefault(Color.GREEN,"d")+" "+m.getOrDefault(Color.RED,"d")+" "+m.putIfAbsent(Color.GREEN,"p")+" "+m;
                m.put(Color.RED,null);m.put(Color.BLUE,"b");
                r+=" "+m+" "+m.size()+" "+m.remove(Color.RED)+" "+m.size()+" "+m.containsKey(Color.RED)+" "+m.put(Color.BLUE,null)+" "+m.containsValue("b")+" "+m;
                r+=" "+m.values().remove(null)+" "+m+" "+m.size()+" "+m.keySet().remove(Color.BLUE)+" "+m+" "+m.keySet().remove(Color.BLUE)+" "+m.isEmpty();
                Map<Color,Object> self=new EnumMap<>(Color.class);self.put(Color.RED,self);self.put(Color.BLUE,null);
                m.put(Color.BLACK,null);
                return r+" "+self+" "+m.keySet().remove(Color.BLACK)+" "+m.size()+" "+m.keySet().remove(Color.BLACK)+" "+m;
            }
            @SuppressWarnings({"unchecked","rawtypes"})
            public static String keyChecks(){
                EnumMap<Color,String> m=colors();
                Map raw=m;
                String r=m.get(null)+" "+m.get(Fruit.APPLE)+" "+m.get("RED")+" "+m.get(0)+" "+m.containsKey(null)+" "+m.containsKey(Fruit.APPLE)+" "+m.containsKey("BLUE")
                        +" "+m.remove(null)+" "+m.remove(Fruit.PEAR)+" "+m.remove("BLUE")+" "+m;
                r+=" "+t(()->raw.put(Fruit.APPLE,"a"))+" "+t(()->raw.put(Op.PLUS,"p"))+" "+ty(()->raw.put(null,"n"))+" "+ty(()->raw.put("RED","s"))+" "+m+" "+m.size();
                Map ops=new EnumMap<Op,String>(Op.class);
                r+=" "+t(()->ops.put(Level.LOW,"l"))+" "+ops.put(Op.TIMES,"t")+" "+ops+" "+ops.get(Op.TIMES)+" "+ops.containsKey(Op.PLUS);
                EnumMap<Level,Integer> lv=new EnumMap<>(Level.class);lv.put(Level.HIGH,2);lv.put(Level.LOW,0);
                return r+" "+lv+" "+lv.keySet();
            }

            // ---- views ----
            public static String keySetView(){
                EnumMap<Color,String> m=colors();
                Set<Color> k=m.keySet();
                String r=k+" "+k.size()+" "+(k==m.keySet())+" "+k.contains(Color.RED)+" "+k.contains(Color.GREEN)+" "+k.contains(null)+" "+k.contains(Fruit.APPLE)
                        +" "+k.remove(Color.RED)+" "+k.remove(Color.RED)+" "+k.remove(Fruit.APPLE)+" "+m;
                m.put(Color.GREEN,"g");
                r+=" "+k+" "+k.size()+" "+k.removeIf(c->c.ordinal()>3)+" "+m+" "+k.removeAll(List.of(Color.GREEN,Color.YELLOW))+" "+m+" "+k.retainAll(List.of(Color.RED))+" "+m+" "+k.isEmpty();
                m.put(Color.YELLOW,"y");m.put(Color.RED,"r");
                k.clear();
                return r+" "+m+" "+k+" "+m.size()+" "+k.equals(Set.of())+" "+colors().keySet().equals(Set.of(Color.RED,Color.BLUE,Color.BLACK));
            }
            public static String valuesView(){
                EnumMap<Color,String> m=colors();m.put(Color.YELLOW,"b");
                Collection<String> v=m.values();
                String r=v+" "+v.size()+" "+(v==m.values())+" "+v.contains("b")+" "+v.contains("x")+" "+v.contains(null)+" "+v.remove("b")+" "+m+" "+v.remove("x")+" "+v.size();
                m.put(Color.GREEN,null);
                r+=" "+v+" "+v.contains(null)+" "+v.remove(null)+" "+v.remove(null)+" "+m+" "+v.removeIf(s->s.equals("r"))+" "+m;
                v.clear();
                return r+" "+m+" "+v+" "+v.isEmpty()+" "+m.size();
            }
            public static String entrySetView(){
                EnumMap<Color,String> m=colors();m.put(Color.GREEN,null);
                Set<Map.Entry<Color,String>> es=m.entrySet();
                String r=es+" "+es.size()+" "+(es==m.entrySet())+" "+es.contains(new AbstractMap.SimpleEntry<>(Color.RED,"r"))+" "+es.contains(new AbstractMap.SimpleEntry<>(Color.RED,"x"))
                        +" "+es.contains(new AbstractMap.SimpleEntry<Color,String>(Color.GREEN,null))+" "+es.contains(new AbstractMap.SimpleEntry<Color,String>(Color.YELLOW,null))
                        +" "+es.contains(Map.entry(Fruit.APPLE,"r"))+" "+es.contains("RED")+" "+es.contains(null);
                r+=" "+es.remove(new AbstractMap.SimpleEntry<>(Color.BLUE,"x"))+" "+es.remove(new AbstractMap.SimpleEntry<>(Color.BLUE,"b"))+" "+es.remove(new AbstractMap.SimpleEntry<Color,String>(Color.GREEN,null))
                        +" "+es.remove(new AbstractMap.SimpleEntry<Color,String>(Color.GREEN,null))+" "+es.remove("x")+" "+m+" "+es.size();
                for(Map.Entry<Color,String> e:es)e.setValue(e.getKey().name().toLowerCase());
                r+=" "+m;
                Map.Entry<Color,String> first=es.iterator().next();
                r+=" "+first+" "+first.getKey()+" "+first.getValue()+" "+first.setValue(null)+" "+first+" "+m+" "+m.size()+" "+first.equals(new AbstractMap.SimpleEntry<Color,String>(Color.RED,null))
                        +" "+first.equals(Map.entry(Color.BLACK,"black"))+" "+first.equals("RED")+" "+(first.hashCode()==new AbstractMap.SimpleEntry<Color,String>(Color.RED,null).hashCode())
                        +" "+es.removeIf(e->e.getValue()==null)+" "+m;
                es.clear();
                return r+" "+m+" "+m.size();
            }

            // ---- iterators ----
            public static String iterators(){
                EnumMap<Color,String> m=colors();
                StringBuilder b=new StringBuilder();
                Iterator<Color> k=m.keySet().iterator();
                b.append(k.next()).append(' ');
                m.put(Color.GREEN,"g");m.remove(Color.BLUE);m.put(Color.RED,"R");
                while(k.hasNext())b.append(k.next()).append(' ');
                b.append(t(()->k.next())).append(' ').append(k.hasNext()).append(' ');
                Iterator<String> v=m.values().iterator();
                b.append(t(()->{v.remove();return "x";})).append(' ').append(v.next()).append(' ');v.remove();
                b.append(t(()->{v.remove();return "x";})).append(' ').append(m).append(' ').append(m.size()).append(' ').append(v.next()).append(' ');
                m.put(Color.YELLOW,"y");
                while(v.hasNext())b.append(v.next()).append(' ');
                Iterator<Map.Entry<Color,String>> e=m.entrySet().iterator();
                b.append(t(()->{e.remove();return "x";})).append(' ');
                while(e.hasNext()){Map.Entry<Color,String> x=e.next();if(x.getKey()==Color.YELLOW)e.remove();}
                b.append(m).append(' ').append(m.size()).append(' ').append(t(()->e.next()));
                Iterator<Empty> z=new EnumMap<Empty,String>(Empty.class).keySet().iterator();
                return b.append(' ').append(z.hasNext()).append(' ').append(t(()->z.next())).toString();
            }
            public static String entryAfterRemove(){
                EnumMap<Color,String> m=colors();
                Iterator<Map.Entry<Color,String>> i=m.entrySet().iterator();
                Map.Entry<Color,String> red=i.next();
                i.remove();
                String r=m+" "+m.size()+" "+t(red::getKey)+" "+t(red::getValue)+" "+t(()->red.setValue("z"))+" "+red.equals(red)+" "+red.equals(new AbstractMap.SimpleEntry<>(Color.RED,"r"))
                        +" "+t(()->{i.remove();return "x";})+" "+m;
                Map.Entry<Color,String> blue=i.next();
                m.remove(Color.BLUE);
                r+=" "+blue.getKey()+" "+blue.getValue()+" "+m+" "+m.size();
                Map.Entry<Color,String> black=i.next();
                m.put(Color.BLACK,"K");
                return r+" "+black+" "+black.getValue()+" "+i.hasNext();
            }

            // ---- bulk ----
            @SuppressWarnings({"unchecked","rawtypes"})
            public static String putAllCases(){
                EnumMap<Color,String> m=new EnumMap<>(Color.class);m.put(Color.GREEN,"g");
                EnumMap<Color,String> src=colors();src.put(Color.GREEN,null);
                m.putAll(src);
                String r=m+" "+m.size()+" "+src;
                m.putAll(new EnumMap<Color,String>(Color.class));
                LinkedHashMap<Color,String> l=new LinkedHashMap<>();l.put(Color.YELLOW,"y");l.put(Color.RED,"R");
                m.putAll(l);
                r+=" "+m+" "+m.size();
                Map raw=m;
                raw.putAll(new EnumMap<Fruit,String>(Fruit.class));
                EnumMap<Fruit,String> fruit=new EnumMap<>(Fruit.class);fruit.put(Fruit.PEAR,"p");
                r+=" "+t(()->{raw.putAll(fruit);return "x";});
                EnumMap<Op,String> ops=new EnumMap<>(Op.class);ops.put(Op.MINUS,"m");
                r+=" "+t(()->{raw.putAll(ops);return "x";})+" "+t(()->{raw.putAll(Map.of(Op.MINUS,"m"));return "x";});
                LinkedHashMap mixed=new LinkedHashMap();mixed.put(Color.BLACK,"K");mixed.put(Fruit.APPLE,"a");mixed.put(Color.BLUE,"B");
                r+=" "+t(()->{raw.putAll(mixed);return "x";})+" "+m+" "+ty(()->{m.putAll(null);return "x";});
                EnumMap<Color,String> e=new EnumMap<>(Color.class);e.putAll(Map.of());
                return r+" "+e;
            }

            // ---- equality, hashing, copies, text ----
            public static String equalsAndHash(){
                EnumMap<Color,String> a=colors(),b=colors();
                HashMap<Color,String> h=new HashMap<>(a);
                TreeMap<Color,String> t=new TreeMap<>(a);
                String r=a.equals(b)+" "+a.equals(h)+" "+h.equals(a)+" "+a.equals(t)+" "+(a.hashCode()==b.hashCode())+" "+(a.hashCode()==h.hashCode())+" "+a.equals(a)
                        +" "+a.equals(null)+" "+a.equals(List.of())+" "+a.equals(a.keySet());
                b.put(Color.RED,"x");
                r+=" "+a.equals(b)+" "+b.equals(a);
                b.put(Color.RED,"r");b.put(Color.GREEN,"g");
                r+=" "+a.equals(b)+" "+b.equals(a);
                a.put(Color.GREEN,null);h.put(Color.GREEN,null);
                r+=" "+a.equals(h)+" "+h.equals(a)+" "+(a.hashCode()==h.hashCode())+" "+a.equals(b);
                HashMap<Color,String> other=new HashMap<>(colors());other.put(Color.YELLOW,null);
                r+=" "+a.equals(other)+" "+other.equals(a);
                EnumMap<Color,String> nc=new EnumMap<>(Color.class);EnumMap<Fruit,String> nf=new EnumMap<>(Fruit.class);
                r+=" "+nc.equals(nf)+" "+nc.equals(new HashMap<>())+" "+nc.hashCode()+" "+nf.equals(nc);
                nf.put(Fruit.APPLE,"r");nc.put(Color.RED,"r");
                EnumMap<Color,String> n1=new EnumMap<>(Color.class),n2=new EnumMap<>(Color.class);n1.put(Color.RED,null);n2.put(Color.GREEN,null);
                HashMap<Color,String> h2=new HashMap<>();h2.put(Color.GREEN,null);
                return r+" "+nc.equals(nf)+" "+nf.equals(nc)+" "+n1.equals(n2)+" "+n1.equals(h2)+" "+n2.equals(h2)+" "+h2.equals(n2);
            }
            public static String cloneAndText(){
                EnumMap<Color,String> m=colors();
                Set<Color> k=m.keySet();Collection<String> v=m.values();Set<Map.Entry<Color,String>> es=m.entrySet();
                EnumMap<Color,String> c=m.clone();
                c.put(Color.GREEN,"g");c.keySet().remove(Color.RED);c.values().remove("k");c.entrySet().remove(new AbstractMap.SimpleEntry<>(Color.BLUE,"b"));
                String r=m+" "+c+" "+m.size()+" "+c.size()+" "+k+" "+v+" "+es+" "+(c.keySet()!=k)+" "+(c.values()!=v)+" "+(c.entrySet()!=es)+" "+c.keySet()+" "+c.values()+" "+c.entrySet();
                m.clear();
                EnumMap<Level,String> lv=new EnumMap<>(Level.class);lv.put(Level.MID,null);lv.put(Level.LOW,"x");
                EnumMap<Level,String> lc=lv.clone();lc.put(Level.HIGH,"h");lc.put(Level.MID,"m");
                return r+" "+m+" "+c+" "+lv+" "+lc+" "+lv.size()+" "+lc.size()+" "+new EnumMap<>(Color.class).clone()+" "+lv.entrySet()+" "+lv.values()+" "+lv.keySet();
            }
        }
        """;

    @Test void constructors() throws Exception {
        matchesJvm(FIXTURE, "constructors", "constructorFailures");
    }

    @Test void singleMappings() throws Exception {
        matchesJvm(FIXTURE, "putGetRemove", "nullValues", "keyChecks");
    }

    @Test void views() throws Exception {
        matchesJvm(FIXTURE, "keySetView", "valuesView", "entrySetView");
    }

    @Test void iterators() throws Exception {
        matchesJvm(FIXTURE, "iterators", "entryAfterRemove");
    }

    @Test void bulkOperations() throws Exception {
        matchesJvm(FIXTURE, "putAllCases");
    }

    @Test void equalityCopiesAndText() throws Exception {
        matchesJvm(FIXTURE, "equalsAndHash", "cloneAndText");
    }

    @Test void leavesOutSerialization() {
        rejectsUnusedCall("java/util/EnumMap", "writeObject", "(Ljava/io/ObjectOutputStream;)V", INVOKEVIRTUAL, false);
    }
}
