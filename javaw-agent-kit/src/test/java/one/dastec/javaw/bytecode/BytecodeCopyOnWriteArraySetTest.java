package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T77: java.util.concurrent.CopyOnWriteArraySet over CopyOnWriteArrayList. Expected values come from the JVM; see tasks/T77-copy-on-write-array-set.md. */
class BytecodeCopyOnWriteArraySetTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.io.Serializable;
        import java.util.AbstractSet;
        import java.util.ArrayList;
        import java.util.Arrays;
        import java.util.Collection;
        import java.util.Iterator;
        import java.util.LinkedHashSet;
        import java.util.List;
        import java.util.Set;
        import java.util.TreeSet;
        import java.util.concurrent.CopyOnWriteArrayList;
        import java.util.concurrent.CopyOnWriteArraySet;

        /** Matches another Key by name and logs every equals call as this?other. "never" matches nothing, not even itself. */
        class Key {
            static final StringBuilder log=new StringBuilder();
            final String n; Key(String n){this.n=n;}
            public boolean equals(Object o){log.append(n).append('?').append(o).append(' ');return !n.equals("never")&&o instanceof Key&&((Key)o).n.equals(n);}
            public int hashCode(){return n.length();}
            public String toString(){return n;}
        }
        /** Like a library subclass: logs the overridable calls the set makes on itself. */
        class Spy<E> extends CopyOnWriteArraySet<E> {
            final StringBuilder log=new StringBuilder();
            Spy(){}
            Spy(Collection<? extends E> c){super(c);}
            public boolean add(E e){log.append("add:").append(e).append(' ');return super.add(e);}
            public boolean contains(Object o){log.append("contains:").append(o).append(' ');return super.contains(o);}
            public boolean remove(Object o){log.append("remove:").append(o).append(' ');return super.remove(o);}
            public int size(){int n=super.size();log.append("size:").append(n).append(' ');return n;}
            public Iterator<E> iterator(){log.append("iterator ");return super.iterator();}
            public Object[] toArray(){log.append("toArray ");return super.toArray();}
        }
        /** A list that logs the toArray calls made on it. */
        class Source extends ArrayList<String> {
            final StringBuilder log=new StringBuilder();
            Source(String... s){super(Arrays.asList(s));}
            public Object[] toArray(){log.append("toArray ");return super.toArray();}
        }

        public class Fixture {
            interface Op{Object run() throws Exception;}
            static String t(Op op){
                try{return String.valueOf(op.run());}catch(Exception e){return e.getClass().getSimpleName()+":"+e.getMessage();}
            }
            static String ty(Op op){
                try{return String.valueOf(op.run());}catch(Exception e){return e.getClass().getSimpleName();}
            }
            static CopyOnWriteArraySet<String> s(String... items){CopyOnWriteArraySet<String> s=new CopyOnWriteArraySet<>();for(String x:items)s.add(x);return s;}

            public static String basics(){
                CopyOnWriteArraySet<String> s=new CopyOnWriteArraySet<>();
                String a=s.size()+","+s.isEmpty()+","+s+","+s.contains("a")+","+s.remove("a")+","+s.hashCode();
                a+="|"+s.add("b")+","+s.add("a")+","+s.add("b")+","+s.add(null)+","+s.add(null)+","+s.add("c")+","+s+","+s.size()+","+s.isEmpty();
                a+="|"+s.contains(null)+","+s.contains("a")+","+s.contains("z")+","+s.contains(1);
                a+="|"+s.remove(null)+","+s.remove(null)+","+s.remove("b")+","+s+","+s.add("b")+","+s;
                a+="|"+s.hashCode()+","+(s.hashCode()==("a".hashCode()+"c".hashCode()+"b".hashCode()));
                s.clear();
                a+="|"+s+","+s.size()+","+s.isEmpty()+","+s.add("x")+","+s;
                Object w=s;
                return a+"|"+(w instanceof Set)+","+(w instanceof AbstractSet)+","+(w instanceof Serializable)+","+(w instanceof List);
            }
            public static String constructorsAndAddAll(){
                Source src=new Source("b","a","b",null,"c","a",null);
                CopyOnWriteArraySet<String> c=new CopyOnWriteArraySet<>(src);
                String a=c+","+c.size()+","+src.log+","+new CopyOnWriteArraySet<>(new ArrayList<String>()).isEmpty()+","+ty(()->new CopyOnWriteArraySet<String>(null));
                CopyOnWriteArraySet<String> d=new CopyOnWriteArraySet<>(c);
                d.add("z");c.remove("b");
                a+="|"+c+","+d+","+new CopyOnWriteArraySet<>(new TreeSet<>(List.of("q","p","r")));
                CopyOnWriteArraySet<String> s=s("a","b");
                Source more=new Source("c","a","d","c","e");
                a+="|"+s.addAll(more)+","+s+","+more.log+","+s.addAll(List.of("a","e"))+","+s.addAll(new ArrayList<String>())+","+ty(()->s.addAll(null))+","+s.addAll(s)+","+s;
                return a;
            }
            public static String iterators(){
                CopyOnWriteArraySet<String> s=s("a","b","c");
                Iterator<String> it=s.iterator();
                s.add("d");s.remove("a");
                StringBuilder o=new StringBuilder();
                o.append(it.next()).append(',').append(t(()->{it.remove();return "x";})).append(',');
                while(it.hasNext())o.append(it.next()).append(' ');
                o.append(t(()->it.next())).append(',').append(s);
                for(String x:s){if(x.equals("b"))s.add("e");o.append(' ').append(x);}
                o.append(',').append(s);
                StringBuilder f=new StringBuilder();
                s.forEach(x->{f.append(x);s.remove(x);});
                o.append('|').append(f).append(',').append(s).append(',').append(ty(()->{s.forEach(null);return "x";}));
                return o.toString();
            }
            public static String equalsAndContainsAll(){
                Key never=new Key("never");
                Key a1=new Key("a"),b1=new Key("bb"),c1=new Key("ccc");
                CopyOnWriteArraySet<Key> s=new CopyOnWriteArraySet<>();
                s.add(a1);s.add(b1);s.add(never);s.add(c1);
                Key.log.setLength(0);
                String r=s.size()+","+s.add(never)+","+s.contains(never)+","+s.size()+" "+Key.log;Key.log.setLength(0);
                LinkedHashSet<Key> same=new LinkedHashSet<>();same.add(new Key("ccc"));same.add(never);same.add(new Key("a"));same.add(new Key("bb"));
                r+="|"+s.equals(same)+" "+Key.log;Key.log.setLength(0);
                r+="|"+s.containsAll(same)+" "+Key.log;Key.log.setLength(0);
                r+="|"+s.containsAll(new ArrayList<>(same))+" "+Key.log;Key.log.setLength(0);
                LinkedHashSet<Key> fewer=new LinkedHashSet<>();fewer.add(new Key("bb"));fewer.add(new Key("a"));
                r+="|"+s.equals(fewer)+","+s.containsAll(fewer)+" "+Key.log;Key.log.setLength(0);
                LinkedHashSet<Key> other=new LinkedHashSet<>();other.add(new Key("a"));other.add(new Key("zz"));
                r+="|"+s.equals(other)+","+s.containsAll(other)+" "+Key.log;Key.log.setLength(0);
                CopyOnWriteArraySet<String> p=s("x","y",null);
                r+="|"+p.equals(p)+","+p.equals(s("y",null,"x"))+","+p.equals(s("x","y"))+","+p.equals(s("x","y",null,"z"))+","+p.equals(List.of("x","y"))+","+p.equals(null)+","+p.equals(new CopyOnWriteArrayList<>(p));
                r+="|"+s("x","y").equals(new TreeSet<>(List.of("y","x")))+","+new TreeSet<>(List.of("y","x")).equals(s("x","y"))+","+s().equals(new TreeSet<String>())+","+s("x").equals(new TreeSet<String>());
                r+="|"+p.containsAll(List.of())+","+p.containsAll(Arrays.asList("x",null,"x"))+","+p.containsAll(List.of("x","q"))+","+ty(()->p.containsAll(null));
                r+="|"+(p.hashCode()==s(null,"y","x").hashCode())+","+s("x","y").hashCode()+","+(s("x","y").hashCode()==new TreeSet<>(List.of("x","y")).hashCode());
                return r;
            }
            public static String bulkAndArrays(){
                CopyOnWriteArraySet<String> s=s("a","b","c","d","e");
                String r=s.removeAll(List.of("b","z","d"))+","+s+","+s.removeAll(List.of("q"))+","+ty(()->s.removeAll(null));
                r+="|"+s.retainAll(List.of("e","a","x"))+","+s+","+s.retainAll(List.of("a","e"))+","+ty(()->s.retainAll(null));
                CopyOnWriteArraySet<String> t=s("one","two","three","four");
                r+="|"+t.removeIf(x->x.length()==3)+","+t+","+t.removeIf(x->false)+","+ty(()->t.removeIf(null));
                CopyOnWriteArraySet<String> u=s("p","q","r");
                Object[] o=u.toArray();
                String[] small=u.toArray(new String[0]);
                String[] exact=new String[3];
                String[] big={"0","1","2","3","4"};
                String[] r1=u.toArray(exact),r2=u.toArray(big);
                o[0]="changed";
                r+="|"+Arrays.toString(o)+","+o.getClass().getSimpleName()+","+Arrays.toString(small)+","+small.getClass().getSimpleName()+","+(r1==exact)+","+Arrays.toString(exact)+","+(r2==big)+","+Arrays.toString(big)+","+u;
                r+="|"+ty(()->u.toArray((String[])null))+","+Arrays.toString(s().toArray(new String[]{"k","l"}));
                return r;
            }
            public static String subclassCalls(){
                Spy<String> s=new Spy<>(List.of("a","b","a"));
                StringBuilder o=new StringBuilder(s.log);s.log.setLength(0);
                o.append('|').append(s.addAll(List.of("c","a"))).append(' ').append(s.log);s.log.setLength(0);
                o.append('|').append(s.isEmpty()).append(' ').append(s.log);s.log.setLength(0);
                o.append('|').append(s.containsAll(List.of("a","c"))).append(' ').append(s.log);s.log.setLength(0);
                o.append('|').append(s.containsAll(s("a"))).append(' ').append(s.log);s.log.setLength(0);
                o.append('|').append(s.equals(s("c","b","a"))).append(' ').append(s.log);s.log.setLength(0);
                o.append('|').append(s.removeAll(List.of("b"))).append(' ').append(s.log);s.log.setLength(0);
                o.append('|').append(s.retainAll(List.of("a","c"))).append(' ').append(s.log);s.log.setLength(0);
                o.append('|').append(s.hashCode()).append(' ').append(s.log);s.log.setLength(0);
                o.append('|').append(s).append(' ').append(s.log);s.log.setLength(0);
                s.clear();
                o.append('|').append(s.log).append(new Spy<String>().log);
                return o.toString();
            }
        }
        """;

    @Test void basics() throws Exception {
        matchesJvm(FIXTURE, "basics");
    }

    @Test void constructorsAndAddAll() throws Exception {
        matchesJvm(FIXTURE, "constructorsAndAddAll");
    }

    @Test void snapshotIterators() throws Exception {
        matchesJvm(FIXTURE, "iterators");
    }

    @Test void equalsAndContainsAll() throws Exception {
        matchesJvm(FIXTURE, "equalsAndContainsAll");
    }

    @Test void bulkOperationsArraysAndSubclasses() throws Exception {
        matchesJvm(FIXTURE, "bulkAndArrays", "subclassCalls");
    }
}
