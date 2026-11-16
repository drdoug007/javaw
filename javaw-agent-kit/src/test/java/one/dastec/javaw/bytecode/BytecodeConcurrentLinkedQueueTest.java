package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** T42: java.util.concurrent.ConcurrentLinkedQueue (single-threaded contract). Expected values come from the JVM; see tasks/T42-concurrent-linked-queue.md. */
@Tag("agent-pending")
class BytecodeConcurrentLinkedQueueTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.io.Serializable;
        import java.util.AbstractQueue;
        import java.util.ArrayList;
        import java.util.Collection;
        import java.util.Iterator;
        import java.util.List;
        import java.util.Queue;
        import java.util.concurrent.ConcurrentLinkedQueue;

        /** Matches another Key by name and logs every equals call as this?other. "never" matches nothing, not even itself. */
        class Key {
            static final StringBuilder log=new StringBuilder();
            final String n; Key(String n){this.n=n;}
            public boolean equals(Object o){log.append(n).append('?').append(o).append(' ');return !n.equals("never")&&o instanceof Key&&((Key)o).n.equals(n);}
            public int hashCode(){return n.hashCode();}
            public String toString(){return n;}
        }
        /** toString returns null. */
        class Blank { public String toString(){return null;} }
        /** Like a library subclass: logs the overridable calls the queue makes on itself. */
        class Spy<E> extends ConcurrentLinkedQueue<E> {
            final StringBuilder log=new StringBuilder();
            Spy(){}
            Spy(Collection<? extends E> c){super(c);}
            public boolean offer(E e){log.append("offer:").append(e).append(' ');return super.offer(e);}
            public E poll(){E e=super.poll();log.append("poll:").append(e).append(' ');return e;}
            public E peek(){E e=super.peek();log.append("peek:").append(e).append(' ');return e;}
            public int size(){int n=super.size();log.append("size:").append(n).append(' ');return n;}
            public boolean contains(Object o){log.append("contains:").append(o).append(' ');return super.contains(o);}
            public Iterator<E> iterator(){log.append("iterator ");return super.iterator();}
        }

        public class Fixture {
            interface Op{Object run() throws Exception;}
            static String t(Op op){
                try{return String.valueOf(op.run());}catch(Exception e){return e.getClass().getSimpleName()+":"+e.getMessage();}
            }
            static String ty(Op op){
                try{return String.valueOf(op.run());}catch(Exception e){return e.getClass().getSimpleName();}
            }
            static ConcurrentLinkedQueue<String> q(String... items){ConcurrentLinkedQueue<String> q=new ConcurrentLinkedQueue<>();for(String s:items)q.offer(s);return q;}
            static String all(Iterator<?> it){StringBuilder o=new StringBuilder();while(it.hasNext())o.append(it.next()).append(' ');return o.append(t(()->it.next())).toString();}

            public static String basics(){
                ConcurrentLinkedQueue<String> q=new ConcurrentLinkedQueue<>();
                String a=q.isEmpty()+","+q.size()+","+q.peek()+","+q.poll()+","+t(()->q.remove())+","+t(()->q.element())+","+q+","+q.toArray().length;
                a+="|"+q.offer("a")+","+q.add("b")+","+q.offer("a")+","+q.isEmpty()+","+q.size()+","+q.peek()+","+q.element()+","+q;
                a+="|"+ty(()->q.offer(null))+","+ty(()->q.add(null))+","+q.size();
                a+="|"+q.poll()+","+q.remove()+","+q.size()+","+q.peek()+","+q.poll()+","+q.poll()+","+q.isEmpty()+","+t(()->q.remove())+","+q;
                for(int i=0;i<40;i++)q.offer("x"+i);
                StringBuilder o=new StringBuilder();
                for(int i=0;i<38;i++)q.poll();
                q.offer("y");
                o.append(q.size()).append(',').append(q).append(',').append(q.peek());
                Object w=q;
                return a+"|"+o+"|"+(w instanceof Queue)+","+(w instanceof AbstractQueue)+","+(w instanceof Serializable)+","+(w instanceof Collection);
            }
            public static String constructorsAndAddAll(){
                List<String> withNull=new ArrayList<>();withNull.add("a");withNull.add(null);withNull.add("b");
                String a=new ConcurrentLinkedQueue<>(List.of("p","q","r"))+","+new ConcurrentLinkedQueue<>(new ArrayList<String>()).isEmpty()
                        +","+ty(()->new ConcurrentLinkedQueue<String>(null))+","+ty(()->new ConcurrentLinkedQueue<>(withNull));
                ConcurrentLinkedQueue<String> q=q("a","b");
                a+="|"+t(()->q.addAll(q))+","+q.addAll(new ArrayList<String>())+","+ty(()->q.addAll(withNull))+","+q+","+ty(()->q.addAll(null));
                a+="|"+q.addAll(List.of("c","d"))+","+q+","+q.size()+","+q.addAll(q("e"))+","+q.poll()+","+q.offer("f")+","+q;
                ConcurrentLinkedQueue<String> e=new ConcurrentLinkedQueue<>(new ArrayList<String>());
                a+="|"+e.offer("z")+","+e+","+e.poll()+","+e.addAll(List.of("1","2"))+","+e.poll()+","+e.poll()+","+e.poll()+","+e.addAll(List.of("3"))+","+e;
                return a;
            }
            public static String containsAndRemove(){
                ConcurrentLinkedQueue<Key> q=new ConcurrentLinkedQueue<>();
                Key never=new Key("never");
                q.add(new Key("a"));q.add(new Key("b"));q.add(never);q.add(new Key("b"));q.add(new Key("c"));
                Key.log.setLength(0);
                String a=q.contains(new Key("b"))+" "+Key.log;Key.log.setLength(0);
                a+="|"+q.contains(never)+","+q.contains(null)+","+q.contains("a")+" "+Key.log;Key.log.setLength(0);
                a+="|"+q.remove(new Key("b"))+","+q+" "+Key.log;Key.log.setLength(0);
                a+="|"+q.remove(never)+","+q.remove(null)+","+q.remove(new Key("zz"))+","+q+" "+Key.log;Key.log.setLength(0);
                a+="|"+q.remove(new Key("c"))+","+q.remove(new Key("a"))+","+q+","+q.size()+" "+Key.log;Key.log.setLength(0);
                q.offer(new Key("d"));
                a+="|"+q+","+q.containsAll(List.of(new Key("b"),new Key("d")))+","+q.containsAll(List.of(new Key("x")))+" "+Key.log;
                ConcurrentLinkedQueue<String> s=q("x","y","z");
                return a+"|"+s.remove("z")+","+s.offer("w")+","+s+","+s.remove("x")+","+s.remove("w")+","+s.remove("y")+","+s.isEmpty()+","+s.offer("v")+","+s+","+s.peek();
            }
            public static String iterators(){
                ConcurrentLinkedQueue<String> q=q("a","b","c");
                String a=all(q.iterator())+"|"+all(new ConcurrentLinkedQueue<String>().iterator());
                Iterator<String> it=q.iterator();q.poll();
                a+="|"+it.next()+","+all(it);
                ConcurrentLinkedQueue<String> r=q("a","b","c");Iterator<String> i2=r.iterator();
                a+="|"+i2.next()+","+r.remove("b")+","+i2.next()+","+r.remove("c")+","+i2.hasNext()+","+r.offer("d")+","+i2.hasNext()+","+t(()->i2.next())+","+r;
                ConcurrentLinkedQueue<String> s=q("a","b");Iterator<String> i3=s.iterator();
                a+="|"+i3.next()+","+s.offer("c")+","+i3.next()+","+i3.next()+","+s.offer("d")+","+i3.hasNext()+","+t(()->i3.next());
                ConcurrentLinkedQueue<String> u=q("a","b","c","d");Iterator<String> i4=u.iterator();
                a+="|"+t(()->{i4.remove();return "x";})+","+i4.next()+","+i4.next();
                i4.remove();
                a+=","+t(()->{i4.remove();return "x";})+","+u+","+u.size()+","+i4.next();
                u.poll();i4.remove();
                a+=","+u+","+i4.next()+","+i4.hasNext();i4.remove();
                a+=","+u+","+u.size()+","+u.contains("d")+","+u.offer("e")+","+all(u.iterator())+","+u.peek();
                ConcurrentLinkedQueue<String> v=q("a","b","c");Iterator<String> i5=v.iterator();
                a+="|"+i5.next()+","+v.poll()+","+v.poll()+","+v.poll()+","+i5.next()+","+v.offer("d")+","+i5.hasNext()+","+t(()->i5.next())+","+v;
                ConcurrentLinkedQueue<String> w=q("a","b","c");Iterator<String> i6=w.iterator();
                StringBuilder o=new StringBuilder();
                for(String x:w){o.append(x);if(x.equals("a"))w.remove("b");if(x.equals("c"))w.offer("e");}
                return a+"|"+o+","+i6.next()+","+i6.next()+","+i6.next()+","+t(()->i6.next())+","+i6.hasNext();
            }
            public static String bulkOperations(){
                ConcurrentLinkedQueue<String> q=q("a","bb","c","dd","e");StringBuilder seen=new StringBuilder();
                String a=q.removeIf(s->{seen.append(s);return s.length()==2;})+","+seen+","+q+","+q.size()+","+q.removeIf(s->false)+","+ty(()->q.removeIf(null));
                a+="|"+q.removeAll(List.of("a","x"))+","+q+","+q.removeAll(List.of())+","+ty(()->q.removeAll(null))+","+q.offer("f")+","+q.retainAll(List.of("e","f","g"))+","+q+","+q.retainAll(List.of("x"))+","+q+","+ty(()->q.retainAll(null));
                ConcurrentLinkedQueue<String> r=q("a","b","c");Iterator<String> it=r.iterator();it.next();
                r.clear();
                a+="|"+r+","+r.size()+","+r.isEmpty()+","+r.peek()+","+it.next()+","+it.hasNext()+","+r.offer("d")+","+r+","+r.poll();
                ConcurrentLinkedQueue<Integer> n=new ConcurrentLinkedQueue<>(List.of(1,2));StringBuilder o=new StringBuilder();
                n.forEach(x->{o.append(x).append(' ');if(x<100)n.offer(x*10);});
                a+="|"+o+n.size()+","+ty(()->{n.forEach(null);return "x";});
                ConcurrentLinkedQueue<String> s=q("a","b","c","d");StringBuilder p=new StringBuilder();
                s.forEach(x->{p.append(x);if(x.equals("a"))s.remove("b");if(x.equals("c"))s.poll();if(x.equals("d"))s.offer("z");});
                ConcurrentLinkedQueue<String> e=q("a");StringBuilder f=new StringBuilder();
                e.forEach(x->{f.append(x);e.offer("b");});
                return a+"|"+p+","+s+"|"+f+","+e;
            }
            public static String textAndArrays(){
                ConcurrentLinkedQueue<Object> q=new ConcurrentLinkedQueue<>();
                q.offer("a");q.offer(1);q.offer('c');q.offer(List.of(2,3));
                String a=q.toString()+","+q.toArray().length+","+q.toArray()[3]+","+q.toArray().getClass().getSimpleName();
                String[] big={"1","2","3","4","5","6"};
                ConcurrentLinkedQueue<String> s=q("x","y","z");
                String[] r1=s.toArray(new String[0]);String[] r2=s.toArray(big);String[] r3=s.toArray(new String[3]);
                a+="|"+r1.length+":"+String.join("/",r1)+","+(r2==big)+":"+r2[0]+r2[1]+r2[2]+":"+r2[3]+":"+r2[4]+","+r3.length+":"+r3[2]+","+r1.getClass().getSimpleName()+","+ty(()->s.toArray((String[])null));
                Object[] o=s.toArray(new Object[1]);
                a+="|"+o.length+":"+o[0]+":"+o.getClass().getSimpleName()+","+new ConcurrentLinkedQueue<String>().toArray(new String[0]).length;
                ConcurrentLinkedQueue<Object> b=new ConcurrentLinkedQueue<>();b.offer("x");b.offer(new Blank());
                a+="|"+ty(()->b.toString())+","+b.size()+","+b.poll()+","+ty(()->b.toString());
                ConcurrentLinkedQueue<Object> one=new ConcurrentLinkedQueue<>();one.offer("only");
                return a+"|"+one+","+one.poll()+","+one+","+one.toArray().length;
            }
            public static String subclassCalls(){
                Spy<String> p=new Spy<>(List.of("a","b"));
                String a=p.log+"|";
                p.add("c");a+=p.log+"|";p.log.setLength(0);
                p.addAll(List.of("d"));p.isEmpty();p.toString();p.toArray();p.toArray(new String[0]);p.containsAll(List.of("a","d"));a+=p.log+"|";p.log.setLength(0);
                p.remove();p.element();a+=p.log+"|";p.log.setLength(0);
                p.removeIf(x->x.equals("b"));p.removeAll(List.of("c"));p.retainAll(List.of("d","a"));p.forEach(x->{});p.remove("zz");a+=p.log+"|";p.log.setLength(0);
                a+=p+","+p.contains("d")+","+p.size()+" "+p.log;p.log.setLength(0);
                p.clear();
                return a+"|"+p.log+","+p.isEmpty()+","+t(()->p.remove())+","+t(()->p.element())+" "+p.log;
            }
        }
        """;

    @Test void offerPollAndPeek() throws Exception {
        matchesJvm(FIXTURE, "basics");
    }

    @Test void constructorsAndAddAll() throws Exception {
        matchesJvm(FIXTURE, "constructorsAndAddAll");
    }

    @Test void containsAndRemove() throws Exception {
        matchesJvm(FIXTURE, "containsAndRemove");
    }

    @Test void weaklyConsistentIterators() throws Exception {
        matchesJvm(FIXTURE, "iterators");
    }

    @Test void bulkOperationsAndForEach() throws Exception {
        matchesJvm(FIXTURE, "bulkOperations");
    }

    @Test void textArraysAndSubclasses() throws Exception {
        matchesJvm(FIXTURE, "textAndArrays", "subclassCalls");
    }

    @Test void leavesOutSerialization() {
        rejectsUnusedCall("java/util/concurrent/ConcurrentLinkedQueue", "writeObject", "(Ljava/io/ObjectOutputStream;)V", INVOKESPECIAL, false);
    }
}
