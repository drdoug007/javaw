package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** T41: java.util.concurrent.CopyOnWriteArrayList. Expected values come from the JVM; see tasks/T41-copy-on-write-array-list.md. */
@Tag("agent-pending")
class BytecodeCopyOnWriteArrayListTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.util.AbstractCollection;
        import java.util.ArrayList;
        import java.util.Arrays;
        import java.util.Collection;
        import java.util.Comparator;
        import java.util.Iterator;
        import java.util.LinkedList;
        import java.util.List;
        import java.util.ListIterator;
        import java.util.Set;
        import java.util.concurrent.CopyOnWriteArrayList;
        import java.util.stream.Collectors;

        /** A collection that logs the calls made on it; toArray() hands out its own (String[]) array. */
        class Spy extends AbstractCollection<Object>{
            final Object[] data;final StringBuilder log;
            Spy(StringBuilder log,Object[] data){this.log=log;this.data=data;}
            public int size(){log.append("size ");return data.length;}
            public Iterator<Object> iterator(){log.append("iterator ");return Arrays.asList(data).iterator();}
            public boolean contains(Object o){log.append("contains(").append(o).append(") ");return Arrays.asList(data).contains(o);}
            public Object[] toArray(){log.append("toArray ");return data;}
        }
        /** Equal to everything: shows that the list calls argument.equals(element), never element.equals(argument). */
        class Any{public boolean equals(Object o){return true;}public int hashCode(){return 0;}public String toString(){return "any";}}
        /** A subclass that logs the overridable calls the list makes on itself. */
        class Rec extends CopyOnWriteArrayList<String>{
            final StringBuilder log=new StringBuilder();
            Rec(){super();}
            public int size(){log.append("size ");return super.size();}
            public int indexOf(Object o){log.append("indexOf ");return super.indexOf(o);}
            public String get(int i){log.append("get"+i+" ");return super.get(i);}
            public String set(int i,String e){log.append("set"+i+" ");return super.set(i,e);}
            public boolean add(String e){log.append("add ");return super.add(e);}
            public void add(int i,String e){log.append("add"+i+" ");super.add(i,e);}
            public String remove(int i){log.append("remove"+i+" ");return super.remove(i);}
            public boolean addAll(int i,Collection<? extends String> c){log.append("addAll"+i+" ");return super.addAll(i,c);}
            public Object[] toArray(){log.append("toArray ");return super.toArray();}
            String take(){String s=log.toString().trim();log.setLength(0);return "{"+s+"}";}
        }

        public class Fixture {
            interface Op0{Object run() throws Exception;}
            /** The value, or "SimpleName:message" of the exception (only the name for a NullPointerException, whose JVM text is a helpful message). */
            static String t(Op0 op){
                try{return String.valueOf(op.run());}
                catch(NullPointerException e){return "NullPointerException";}
                catch(Exception e){return e.getClass().getSimpleName()+":"+e.getMessage();}
            }
            static CopyOnWriteArrayList<String> abc(){return new CopyOnWriteArrayList<>(List.of("a","b","c"));}
            /** "ok" if sub is still valid after op, "CME" if op replaced the list's array. */
            static String probe(String name,java.util.function.Consumer<CopyOnWriteArrayList<String>> op){
                CopyOnWriteArrayList<String> l=abc();List<String> sub=l.subList(0,2);
                op.accept(l);
                return name+":"+t(()->sub.size());
            }

            // ---- constructors ----
            @SuppressWarnings({"unchecked","rawtypes"})
            public static String constructors(){
                StringBuilder log=new StringBuilder();
                String[] data={"p","q"};
                CopyOnWriteArrayList<Object> a=new CopyOnWriteArrayList<>(new Spy(log,data));
                data[0]="Z";
                String r=a+" "+log+"| "+t(()->a.set(0,1))+" "+t(()->a.add(2))+" "+a;
                CopyOnWriteArrayList<Object> b=new CopyOnWriteArrayList<Object>(new String[]{"x","y"});
                r+=" "+t(()->b.set(1,3))+" "+b+" "+b.size();
                CopyOnWriteArrayList<String> c=abc();CopyOnWriteArrayList<String> d=new CopyOnWriteArrayList<>(c);c.add("d");d.remove("a");
                CopyOnWriteArrayList<String> e=new CopyOnWriteArrayList<>();
                r+=" "+c+" "+d+" "+e+" "+e.size()+" "+e.isEmpty()+" "+c.isEmpty()+" "+new CopyOnWriteArrayList<>(new ArrayList<>(List.of(1,2)));
                r+=" "+t(()->new CopyOnWriteArrayList<String>((Collection<String>)null))+" "+t(()->new CopyOnWriteArrayList<String>((String[])null));
                return r;
            }

            // ---- positional access and searching ----
            public static String positional(){
                CopyOnWriteArrayList<String> l=new CopyOnWriteArrayList<>(List.of("a","b","c","b"));
                String r=l.get(1)+" "+t(()->l.get(4))+" "+t(()->l.get(-1))+" "+l.set(1,"B")+" "+t(()->l.set(4,"x"))+" "+l;
                l.add(4,"e");l.add(0,"s");
                r+=" "+l+" "+t(()->{l.add(7,"x");return "ok";})+" "+t(()->{l.add(-1,"x");return "ok";});
                r+=" "+l.remove(0)+" "+l.remove(4)+" "+t(()->l.remove(4))+" "+t(()->l.remove(-1))+" "+l;
                CopyOnWriteArrayList<String> e=new CopyOnWriteArrayList<>();
                r+=" "+t(()->e.remove(0))+" "+t(()->e.get(0))+" "+t(()->{e.add(1,"x");return "ok";});
                e.add(0,"only");
                r+=" "+e;
                CopyOnWriteArrayList<String> s=new CopyOnWriteArrayList<>(Arrays.asList("x",null,"y","x",null));
                r+=" "+s.indexOf("x")+" "+s.lastIndexOf("x")+" "+s.indexOf(null)+" "+s.lastIndexOf(null)+" "+s.indexOf("q")+" "+s.contains(null)+" "+s.contains("y")
                        +" "+s.indexOf("x",1)+" "+s.indexOf(null,2)+" "+s.indexOf("x",5)+" "+s.indexOf("x",99)+" "+t(()->s.indexOf("x",-1))
                        +" "+s.lastIndexOf("x",2)+" "+s.lastIndexOf(null,3)+" "+s.lastIndexOf("x",-1)+" "+t(()->s.lastIndexOf("x",5))+" "+t(()->s.lastIndexOf(null,7));
                return r;
            }

            // ---- remove(Object), addIfAbsent, addAllAbsent, containsAll ----
            @SuppressWarnings({"unchecked","rawtypes"})
            public static String absentAndContains(){
                CopyOnWriteArrayList<String> l=new CopyOnWriteArrayList<>(Arrays.asList("a","b",null,"a"));
                String r=l.remove("a")+" "+l+" "+l.remove(null)+" "+l.remove("zz")+" "+l;
                r+=" "+l.addIfAbsent("b")+" "+l.addIfAbsent("c")+" "+l.addIfAbsent(null)+" "+l.addIfAbsent(null)+" "+l;
                List raw=new CopyOnWriteArrayList<>(List.of("p","q","r"));
                r+=" "+raw.indexOf(new Any())+" "+raw.lastIndexOf(new Any())+" "+raw.contains(new Any())+" "+((CopyOnWriteArrayList)raw).addIfAbsent(new Any())+" "+raw.remove(new Any())+" "+raw;
                StringBuilder log=new StringBuilder();
                String[] data={"b","x","x","y",null,null,"c"};
                CopyOnWriteArrayList<Object> o=new CopyOnWriteArrayList<>(List.of("a","b","c"));
                r+=" "+o.addAllAbsent(new Spy(log,data))+" "+o+" "+Arrays.toString(data)+" "+log+"| "+t(()->o.set(3,1))+" "+o.addAllAbsent(List.of("a","c"))
                        +" "+o.addAllAbsent(List.of())+" "+t(()->o.addAllAbsent(null));
                log.setLength(0);
                r+=" "+o.containsAll(new Spy(log,new Object[]{"a",null}))+" "+o.containsAll(new Spy(log,new Object[]{"q"}))+" "+o.containsAll(List.of())+" "+log+"|";
                r+=" "+new CopyOnWriteArrayList<String>().containsAll(List.of())+" "+t(()->new CopyOnWriteArrayList<String>().containsAll(null))+" "+t(()->l.containsAll(Arrays.asList("b",null)));
                return r;
            }

            // ---- addAll, removeAll, retainAll, clear ----
            public static String bulk(){
                StringBuilder log=new StringBuilder();
                CopyOnWriteArrayList<Object> e=new CopyOnWriteArrayList<>();
                String r=e.addAll(new Spy(log,new String[]{"p","q"}))+" "+log+"| "+t(()->e.set(0,7))+" "+e;
                CopyOnWriteArrayList<String> l=abc();
                r+=" "+l.addAll(List.of("d","e"))+" "+l.addAll(List.of())+" "+l.addAll(l)+" "+l+" "+t(()->l.addAll(null));
                l.clear();l.addAll(List.of("a","b","c"));
                r+=" "+l.addAll(1,List.of("x","y"))+" "+l.addAll(5,List.of("z"))+" "+l.addAll(0,List.of())+" "+l
                        +" "+t(()->l.addAll(9,List.of()))+" "+t(()->l.addAll(-1,List.of("w")))+" "+t(()->l.addAll(99,null));
                log.setLength(0);
                r+=" "+l.removeAll(new Spy(log,new Object[]{"x","c","q"}))+" "+log+"| "+l;
                log.setLength(0);
                r+=" "+l.retainAll(new Spy(log,new Object[]{"a","b","y","z"}))+" "+log+"| "+l+" "+l.retainAll(List.of("a","b","y","z"))+" "+l.removeAll(List.of("q"));
                CopyOnWriteArrayList<String> n=new CopyOnWriteArrayList<>();
                r+=" "+t(()->n.removeAll(null))+" "+t(()->n.retainAll(null))+" "+n.removeAll(List.of())+" "+l.retainAll(List.of())+" "+l+" "+l.size();
                l.add("again");l.clear();l.clear();
                return r+" "+l+" "+l.isEmpty();
            }

            // ---- iterators, forEach, stream ----
            public static String iterators(){
                CopyOnWriteArrayList<String> l=abc();
                Iterator<String> i=l.iterator();
                l.add("d");l.remove("a");l.set(0,"B");
                StringBuilder b=new StringBuilder();
                b.append(i.next()).append(' ').append(t(()->{i.remove();return "x";})).append(' ');
                while(i.hasNext())b.append(i.next());
                b.append(' ').append(t(()->i.next())).append(' ').append(l);
                ListIterator<String> li=l.listIterator(2);
                b.append(' ').append(li.nextIndex()).append(li.previousIndex()).append(li.hasPrevious()).append(li.previous()).append(li.previous())
                        .append(li.hasPrevious()).append(' ').append(t(()->li.previous())).append(' ').append(t(()->{li.set("x");return "x";}))
                        .append(' ').append(t(()->{li.add("x");return "x";})).append(' ').append(li.next());
                b.append(' ').append(t(()->l.listIterator(4))).append(' ').append(t(()->l.listIterator(-1))).append(' ').append(l.listIterator(3).hasNext());
                ListIterator<String> zero=l.listIterator();
                b.append(' ').append(zero.hasPrevious()).append(zero.nextIndex());
                StringBuilder rest=new StringBuilder();
                Iterator<String> j=l.iterator();j.next();l.clear();j.forEachRemaining(rest::append);
                b.append(' ').append(rest).append(' ').append(j.hasNext()).append(' ').append(l);
                CopyOnWriteArrayList<String> f=abc();
                StringBuilder seen=new StringBuilder();
                f.forEach(s->{seen.append(s);f.add(s.toUpperCase());});
                for(String s:f)if(s.length()==1&&Character.isLowerCase(s.charAt(0)))f.remove(s);
                b.append(' ').append(seen).append(' ').append(f).append(' ').append(t(()->{f.forEach(null);return "x";}))
                        .append(' ').append(f.stream().map(String::toLowerCase).collect(Collectors.joining(",")));
                return b.toString();
            }

            // ---- removeIf, replaceAll, sort ----
            public static String functional(){
                CopyOnWriteArrayList<String> l=new CopyOnWriteArrayList<>(List.of("bb","a","ccc","dd","e"));
                StringBuilder calls=new StringBuilder();
                String r=l.removeIf(s->{calls.append(s).append(',');return s.length()==2;})+" "+calls+" "+l+" "+l.removeIf(s->false);
                r+=" "+t(()->l.removeIf(s->{if(s.equals("ccc"))throw new IllegalStateException("boom");return true;}))+" "+l;
                r+=" "+t(()->l.removeIf(s->{l.add("+");return false;}))+" "+l;
                r+=" "+t(()->l.removeIf(s->{l.add("-");return s.equals("a");}))+" "+l;
                CopyOnWriteArrayList<String> n=new CopyOnWriteArrayList<>();
                r+=" "+t(()->n.removeIf(null))+" "+t(()->{n.replaceAll(null);return "x";})+" "+t(()->{n.forEach(null);return "x";});
                CopyOnWriteArrayList<String> m=new CopyOnWriteArrayList<>(List.of("x","y","z"));
                m.replaceAll(s->s+s);
                r+=" "+m+" "+t(()->{m.replaceAll(s->{if(s.equals("yy"))throw new IllegalStateException("bad");return "!";});return "x";})+" "+m;
                m.replaceAll(s->{m.add("lost");return s.toUpperCase();});
                r+=" "+m;
                CopyOnWriteArrayList<String> s=new CopyOnWriteArrayList<>(List.of("pear","fig","apple","kiwi","date","plum"));
                s.sort(Comparator.comparing(String::length));
                r+=" "+s;
                s.sort(null);
                r+=" "+s;
                s.sort(Comparator.reverseOrder());
                CopyOnWriteArrayList<Integer> q=new CopyOnWriteArrayList<>(List.of(3,1,2));
                Iterator<Integer> qi=q.iterator();q.sort(null);
                r+=" "+s+" "+q+" "+qi.next();
                return r;
            }

            // ---- equals, hashCode, toString, clone, toArray ----
            public static String equalityAndCopies(){
                CopyOnWriteArrayList<String> l=new CopyOnWriteArrayList<>(Arrays.asList("a",null,"c"));
                List<String> al=new ArrayList<>(Arrays.asList("a",null,"c"));
                String r=l+" "+l.equals(al)+" "+al.equals(l)+" "+l.equals(new LinkedList<>(al))+" "+l.equals(List.of("a","c"))+" "+l.equals(Arrays.asList("a",null,"c","d"))
                        +" "+l.equals(Set.of("a"))+" "+l.equals(null)+" "+l.equals(l)+" "+(l.hashCode()==al.hashCode())+" "+new CopyOnWriteArrayList<>().hashCode()
                        +" "+new CopyOnWriteArrayList<>(List.of("a","b")).hashCode()+" "+new CopyOnWriteArrayList<>().equals(List.of())+" "+new CopyOnWriteArrayList<>();
                @SuppressWarnings("unchecked") CopyOnWriteArrayList<String> c=(CopyOnWriteArrayList<String>)l.clone();
                c.add("d");l.set(0,"A");
                r+=" "+l+" "+c+" "+(c!=l)+" "+c.getClass().getSimpleName()+" "+l.clone().equals(l);
                Object[] arr=l.toArray();arr[0]="changed";
                String[] small=l.toArray(new String[1]);
                String[] same=new String[3];String[] same2=l.toArray(same);
                String[] big={"1","2","3","4","5"};l.toArray(big);
                r+=" "+l+" "+arr.length+" "+small.getClass().getSimpleName()+" "+Arrays.toString(small)+" "+(same==same2)+" "+Arrays.toString(same)+" "+Arrays.toString(big)
                        +" "+Arrays.toString(new CopyOnWriteArrayList<>().toArray())+" "+t(()->l.toArray((String[])null));
                return r;
            }

            // ---- subList ----
            public static String subLists(){
                CopyOnWriteArrayList<String> l=new CopyOnWriteArrayList<>(List.of("a","b","c","d","e"));
                List<String> s=l.subList(1,4);
                String r=s+" "+s.size()+" "+s.get(0)+" "+s.set(1,"C")+" "+l+" "+s.indexOf("d")+" "+s.indexOf("a")+" "+s.lastIndexOf("C")+" "+s.contains("e")+" "+s.isEmpty();
                s.add("x");s.add(0,"y");
                r+=" "+s+" "+l+" "+s.remove(1)+" "+s.remove("x")+" "+s.remove("zz")+" "+s+" "+l;
                r+=" "+t(()->s.get(3))+" "+t(()->s.get(-1))+" "+t(()->{s.add(4,"q");return "x";})+" "+t(()->s.set(3,"q"))+" "+t(()->s.remove(3))+" "+t(()->s.listIterator(4));
                r+=" "+s.equals(List.of("y","C","d"))+" "+(s.hashCode()==List.of("y","C","d").hashCode())+" "+s.subList(1,3)+" "+t(()->s.subList(2,1))+" "+t(()->s.subList(0,4));
                Iterator<String> it=s.iterator();s.set(0,"Y");
                r+=" "+it.next()+" "+t(()->{it.remove();return "x";})+" "+s;
                s.addAll(List.of("1","2"));s.addAll(1,List.of("3"));
                r+=" "+s+" "+l;
                s.removeIf(x->x.length()==1&&Character.isDigit(x.charAt(0)));
                s.replaceAll(String::toLowerCase);
                r+=" "+s+" "+l;
                s.sort(Comparator.reverseOrder());
                r+=" "+s+" "+l+" "+s.removeAll(List.of("y"))+" "+s.retainAll(List.of("d"))+" "+s+" "+l;
                s.clear();
                r+=" "+s+" "+s.size()+" "+l+" "+t(()->l.subList(-1,1))+" "+t(()->l.subList(1,0))+" "+t(()->l.subList(0,3))+" "+l.subList(2,2)+" "+l.subList(0,2);
                return r;
            }
            public static String subListComodification(){
                CopyOnWriteArrayList<String> l=abc();
                List<String> s=l.subList(0,2);
                List<String> nested=s.subList(1,2);
                l.add("d");
                String r=t(()->s.size())+" "+t(()->s.get(1))+" "+t(()->s.get(5))+" "+t(()->s.set(5,"x"))+" "+t(()->s.remove(5))+" "+t(()->{s.add(5,"x");return "x";})
                        +" "+t(()->s.addAll(9,List.of("x")))+" "+t(()->s.addAll(List.of("x")))+" "+t(()->s.toString())+" "+t(()->s.iterator())+" "+t(()->s.listIterator(9))
                        +" "+t(()->s.equals(List.of()))+" "+s.equals("ab")+" "+t(()->s.hashCode())+" "+t(()->s.isEmpty())+" "+t(()->s.contains("a"))+" "+t(()->s.subList(0,1))
                        +" "+t(()->{s.clear();return "x";})+" "+t(()->s.remove("a"))+" "+t(()->s.indexOf("a"))+" "+t(()->nested.size())+" "+t(()->s.removeIf(x->true))
                        +" "+t(()->{s.replaceAll(x->x);return "x";})+" "+t(()->{s.sort(null);return "x";})+" "+t(()->s.removeAll(null))+" "+l;
                List<String> one=l.subList(0,2),two=l.subList(1,3);
                one.set(0,"A");
                r+=" "+t(()->two.size())+" "+one+" "+l;
                List<String> three=l.subList(0,2),four=l.subList(2,4);
                three.set(1,three.get(1));
                r+=" "+four+" "+three.remove("zz")+" "+four;
                return r;
            }
            public static String copyOnWriteIdentity(){
                return probe("setSame",l->l.set(0,l.get(0)))+" "+probe("setOther",l->l.set(0,"z"))+" "+probe("removeAbsent",l->l.remove("q"))
                        +" "+probe("removePresent",l->l.remove("c"))+" "+probe("ifAbsentPresent",l->l.addIfAbsent("a"))+" "+probe("ifAbsentNew",l->l.addIfAbsent("q"))
                        +" "+probe("allAbsentNone",l->l.addAllAbsent(List.of("a","b")))+" "+probe("addAllEmpty",l->l.addAll(List.of()))
                        +" "+probe("addAllAtEmpty",l->l.addAll(1,List.of()))+" "+probe("removeIfNone",l->l.removeIf(x->false))
                        +" "+probe("removeAllNone",l->l.removeAll(List.of("q")))+" "+probe("retainAllAll",l->l.retainAll(List.of("a","b","c")))
                        +" "+probe("clear",l->l.clear())+" "+probe("replaceAll",l->l.replaceAll(x->x))+" "+probe("sort",l->l.sort(null))
                        +" "+probe("add",l->l.add("q"))+" "+probe("forEach",l->l.forEach(x->{}))+" "+probe("toArray",l->l.toArray())
                        +" "+probe("cloneAdd",l->((CopyOnWriteArrayList<String>)l.clone()).add("q"))+" "+probe("iterator",l->l.iterator().next());
            }

            // ---- overridable self-calls ----
            public static String subclassCalls(){
                Rec l=new Rec();
                l.add("a");l.add("b");l.add("c");
                String r=l.take()+" "+l.isEmpty()+l.take()+" "+l.contains("b")+l.take()+" "+l.addIfAbsent("d")+l.take()+" "+l.addAll(List.of("e"))+l.take()
                        +" "+l.remove("e")+l.take()+" "+l.addAllAbsent(List.of("f"))+l.take()+" "+l.removeIf(x->x.equals("f"))+l.take()+" "+l+l.take()
                        +" "+l.equals(List.of("a","b","c","d"))+l.take()+" "+(l.hashCode()==List.of("a","b","c","d").hashCode())+l.take();
                List<String> s=l.subList(1,3);
                r+=" "+l.take()+" "+s.get(0)+l.take()+" "+s.set(0,"B")+l.take()+" "+s.add("x")+l.take()+" ";
                s.add(0,"y");
                r+=l.take()+" "+s.remove(0)+l.take()+" "+s.remove("x")+l.take()+" "+s.addAll(List.of("p","q"))+l.take()+" "+s.addAll(1,List.of("r"))+l.take()
                        +" "+s.size()+l.take()+" "+s.contains("p")+l.take()+" "+s+l.take();
                s.clear();
                r+=" "+l.take()+" "+l+" "+new CopyOnWriteArrayList<>(l)+l.take()+" "+new CopyOnWriteArrayList<>().addAll(l)+l.take()+" "+l.clone().getClass().getSimpleName();
                return r;
            }
        }
        """;

    @Test void constructorsAndPositions() throws Exception {
        matchesJvm(FIXTURE, "constructors", "positional");
    }

    @Test void searchingAndBulk() throws Exception {
        matchesJvm(FIXTURE, "absentAndContains", "bulk");
    }

    @Test void iteratorsAndFunctional() throws Exception {
        matchesJvm(FIXTURE, "iterators", "functional");
    }

    @Test void equalityAndCopying() throws Exception {
        matchesJvm(FIXTURE, "equalityAndCopies", "copyOnWriteIdentity");
    }

    @Test void subListViews() throws Exception {
        matchesJvm(FIXTURE, "subLists", "subListComodification", "subclassCalls");
    }

    @Test void leavesOutSequencedMembers() {
        rejectsUnusedCall("java/util/concurrent/CopyOnWriteArrayList", "getFirst", "()Ljava/lang/Object;", INVOKEVIRTUAL, false);
    }
}
