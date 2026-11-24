package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T51: java.util.concurrent.BlockingQueue and LinkedBlockingQueue (single-threaded). Expected values come from the JVM; see tasks/T51-linked-blocking-queue.md. */
class BytecodeLinkedBlockingQueueTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.io.Serializable;
        import java.util.AbstractQueue;
        import java.util.ArrayList;
        import java.util.Collection;
        import java.util.Iterator;
        import java.util.List;
        import java.util.Queue;
        import java.util.concurrent.BlockingQueue;
        import java.util.concurrent.LinkedBlockingQueue;

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
        /** A target collection whose add rejects "bad". */
        class Picky extends ArrayList<String> {
            public boolean add(String s){if(s.equals("bad"))throw new IllegalArgumentException("no "+s);return super.add(s);}
        }
        /** Like a library subclass: logs the overridable calls the queue makes on itself. */
        class Spy<E> extends LinkedBlockingQueue<E> {
            final StringBuilder log=new StringBuilder();
            Spy(int capacity){super(capacity);}
            Spy(Collection<? extends E> c){super(c);}
            public boolean offer(E e){log.append("offer:").append(e).append(' ');return super.offer(e);}
            public E poll(){E e=super.poll();log.append("poll:").append(e).append(' ');return e;}
            public E peek(){E e=super.peek();log.append("peek:").append(e).append(' ');return e;}
            public int size(){int n=super.size();log.append("size:").append(n).append(' ');return n;}
            public boolean contains(Object o){log.append("contains:").append(o).append(' ');return super.contains(o);}
            public Iterator<E> iterator(){log.append("iterator ");return super.iterator();}
            public Object[] toArray(){log.append("toArray ");return super.toArray();}
            public int remainingCapacity(){log.append("remaining ");return super.remainingCapacity();}
            public int drainTo(Collection<? super E> c,int max){log.append("drainTo:").append(max).append(' ');return super.drainTo(c,max);}
        }

        public class Fixture {
            interface Op{Object run() throws Exception;}
            static String t(Op op){
                try{return String.valueOf(op.run());}catch(Exception e){return e.getClass().getSimpleName()+":"+e.getMessage();}
            }
            static String ty(Op op){
                try{return String.valueOf(op.run());}catch(Exception e){return e.getClass().getSimpleName();}
            }
            static LinkedBlockingQueue<String> q(int capacity,String... items){LinkedBlockingQueue<String> q=new LinkedBlockingQueue<>(capacity);for(String s:items)q.offer(s);return q;}
            static String all(Iterator<?> it){StringBuilder o=new StringBuilder();while(it.hasNext())o.append(it.next()).append(' ');return o.append(t(()->it.next())).toString();}

            public static String capacityAndOffer(){
                LinkedBlockingQueue<String> u=new LinkedBlockingQueue<>();
                String a=u.remainingCapacity()+","+u.size()+","+u.isEmpty()+","+u.peek()+","+u.poll()+","+t(()->u.remove())+","+t(()->u.element())+","+u;
                a+="|"+t(()->new LinkedBlockingQueue<String>(0))+","+t(()->new LinkedBlockingQueue<String>(-5))+","+new LinkedBlockingQueue<String>(1).remainingCapacity();
                LinkedBlockingQueue<String> q=new LinkedBlockingQueue<>(2);
                a+="|"+q.remainingCapacity()+","+q.offer("a")+","+q.add("b")+","+q.offer("c")+","+t(()->q.add("c"))+","+q.remainingCapacity()+","+q.size()+","+q;
                a+="|"+ty(()->q.offer(null))+","+ty(()->q.add(null))+","+ty(()->{q.put(null);return "x";})+","+ty(()->new LinkedBlockingQueue<String>(1).offer(null))+","+q.size();
                a+="|"+q.peek()+","+q.element()+","+q.poll()+","+q.remainingCapacity()+","+q.offer("c")+","+q.offer("d")+","+q+","+q.remove()+","+q.poll()+","+q.poll()+","+q.remainingCapacity()+","+q.isEmpty();
                LinkedBlockingQueue<String> r=new LinkedBlockingQueue<>(3);
                for(int i=0;i<40;i++){r.offer("x"+i);r.offer("y"+i);r.poll();}
                a+="|"+r.size()+","+r+","+r.remainingCapacity()+","+r.peek();
                Object w=q;
                return a+"|"+(w instanceof BlockingQueue)+","+(w instanceof Queue)+","+(w instanceof AbstractQueue)+","+(w instanceof Serializable)+","+(w instanceof Collection);
            }
            public static String putAndTake(){
                BlockingQueue<String> q=new LinkedBlockingQueue<>(2);
                StringBuilder o=new StringBuilder();
                try{
                    q.put("a");q.put("b");
                    o.append(q).append(',').append(q.remainingCapacity()).append(',').append(q.offer("c")).append(',');
                    o.append(q.take()).append(',').append(q.remainingCapacity()).append(',');
                    q.put("c");
                    o.append(q.take()).append(',').append(q.take()).append(',').append(q.size()).append(',').append(q.poll()).append(',');
                    for(int i=0;i<5;i++){q.put("p"+i);o.append(q.take());}
                    o.append(',').append(q.isEmpty()).append(',').append(q.remainingCapacity());
                }catch(InterruptedException e){o.append("interrupted");}
                LinkedBlockingQueue<Integer> n=new LinkedBlockingQueue<>(1);
                o.append('|').append(t(()->{n.put(7);return n.take()+n.remainingCapacity();})).append(',').append(n.add(8)).append(',').append(t(()->n.add(9))).append(',').append(t(()->n.take()));
                return o.toString();
            }
            public static String constructorsAndAddAll(){
                List<String> withNull=new ArrayList<>();withNull.add("a");withNull.add(null);withNull.add("b");
                LinkedBlockingQueue<String> c=new LinkedBlockingQueue<>(List.of("p","q","r"));
                String a=c+","+c.size()+","+c.remainingCapacity()+","+new LinkedBlockingQueue<>(new ArrayList<String>()).isEmpty()
                        +","+ty(()->new LinkedBlockingQueue<String>((Collection<String>)null))+","+t(()->new LinkedBlockingQueue<>(withNull));
                a+="|"+c.offer("s")+","+c.poll()+","+c;
                LinkedBlockingQueue<String> q=q(3,"a");
                a+="|"+t(()->q.addAll(q))+","+q.addAll(new ArrayList<String>())+","+ty(()->q.addAll(null))+","+q.addAll(List.of("b"))+","+q;
                a+="|"+t(()->q.addAll(List.of("c","d","e")))+","+q+","+q.remainingCapacity();
                LinkedBlockingQueue<String> r=q(5);
                a+="|"+ty(()->r.addAll(withNull))+","+r+","+r.size();
                return a;
            }
            public static String containsAndRemove(){
                LinkedBlockingQueue<Key> q=new LinkedBlockingQueue<>(5);
                Key never=new Key("never");
                q.add(new Key("a"));q.add(new Key("b"));q.add(never);q.add(new Key("b"));q.add(new Key("c"));
                Key.log.setLength(0);
                String a=q.contains(new Key("b"))+" "+Key.log;Key.log.setLength(0);
                a+="|"+q.contains(never)+","+q.contains(null)+","+q.contains("a")+" "+Key.log;Key.log.setLength(0);
                a+="|"+q.remove(new Key("b"))+","+q+","+q.remainingCapacity()+" "+Key.log;Key.log.setLength(0);
                a+="|"+q.remove(never)+","+q.remove(null)+","+q.remove(new Key("zz"))+","+q+" "+Key.log;Key.log.setLength(0);
                a+="|"+q.remove(new Key("c"))+","+q.offer(new Key("d"))+","+q.offer(new Key("e"))+","+q.offer(new Key("f"))+","+q+" "+Key.log;Key.log.setLength(0);
                a+="|"+q.containsAll(List.of(new Key("b"),new Key("e")))+","+q.containsAll(List.of(new Key("x")))+" "+Key.log;
                LinkedBlockingQueue<String> s=q(3,"x","y","z");
                BlockingQueue<String> b=s;
                return a+"|"+b.remove("z")+","+s.offer("w")+","+s+","+b.contains("w")+","+s.remove("x")+","+s.remove("w")+","+s.remove("y")+","+s.isEmpty()+","+s.offer("v")+","+s+","+s.peek()+","+s.remainingCapacity();
            }
            public static String drainTo(){
                LinkedBlockingQueue<String> q=q(10,"a","b","c","d","e");
                List<String> out=new ArrayList<>();
                String a=q.drainTo(out,2)+","+out+","+q+","+q.remainingCapacity();
                a+="|"+q.drainTo(out,0)+","+q.drainTo(out,-1)+","+q.drainTo(out)+","+out+","+q+","+q.size()+","+q.drainTo(out)+","+q.drainTo(out,3);
                a+="|"+ty(()->q.drainTo(null))+","+ty(()->q.drainTo(null,0))+","+t(()->q.drainTo(q))+","+t(()->q.drainTo(q,0))+","+t(()->q.drainTo(q,-1));
                q.offer("f");q.offer("g");
                BlockingQueue<String> b=q;
                List<String> two=new ArrayList<>();
                a+="|"+b.drainTo(two,5)+","+two+","+q.offer("h")+","+q;
                LinkedBlockingQueue<String> p=q(5,"a","b","bad","c");Picky picky=new Picky();
                a+="|"+t(()->p.drainTo(picky))+","+picky+","+p+","+p.size()+","+p.remainingCapacity()+","+p.peek()+","+p.offer("d")+","+p;
                LinkedBlockingQueue<String> src=q(5,"x","y","z");LinkedBlockingQueue<String> dst=q(2,"w");
                a+="|"+t(()->src.drainTo(dst))+","+dst+","+src+","+src.size();
                return a;
            }
            public static String iterators(){
                LinkedBlockingQueue<String> q=q(5,"a","b","c");
                String a=all(q.iterator())+"|"+all(new LinkedBlockingQueue<String>().iterator());
                Iterator<String> it=q.iterator();q.poll();
                a+="|"+it.next()+","+all(it);
                LinkedBlockingQueue<String> r=q(5,"a","b","c");Iterator<String> i2=r.iterator();
                a+="|"+i2.next()+","+r.remove("b")+","+i2.next()+","+r.remove("c")+","+i2.hasNext()+","+r.offer("d")+","+i2.hasNext()+","+t(()->i2.next())+","+r;
                LinkedBlockingQueue<String> s=q(5,"a","b");Iterator<String> i3=s.iterator();
                a+="|"+i3.next()+","+s.offer("c")+","+i3.next()+","+i3.next()+","+s.offer("d")+","+i3.hasNext()+","+t(()->i3.next());
                LinkedBlockingQueue<String> u=q(4,"a","b","c","d");Iterator<String> i4=u.iterator();
                a+="|"+t(()->{i4.remove();return "x";})+","+i4.next()+","+i4.next();
                i4.remove();
                a+=","+t(()->{i4.remove();return "x";})+","+u+","+u.size()+","+u.remainingCapacity()+","+i4.next();
                u.poll();i4.remove();
                a+=","+u+","+i4.next()+","+i4.hasNext();i4.remove();
                a+=","+u+","+u.size()+","+u.contains("d")+","+u.offer("e")+","+u.offer("f")+","+u.offer("g")+","+u.offer("h")+","+all(u.iterator())+","+u.peek();
                LinkedBlockingQueue<String> v=q(5,"a","b","c");Iterator<String> i5=v.iterator();
                a+="|"+i5.next()+","+v.poll()+","+v.poll()+","+v.poll()+","+i5.next()+","+v.offer("d")+","+i5.hasNext()+","+t(()->i5.next())+","+v;
                LinkedBlockingQueue<String> w=q(5,"a","b","c");Iterator<String> i6=w.iterator();
                StringBuilder o=new StringBuilder();
                for(String x:w){o.append(x);if(x.equals("a"))w.remove("b");if(x.equals("c"))w.offer("e");}
                a+="|"+o+","+i6.next()+","+i6.next()+","+i6.next()+","+t(()->i6.next())+","+i6.hasNext();
                LinkedBlockingQueue<String> x=q(5,"a","b","c");Iterator<String> i7=x.iterator();i7.next();
                x.clear();
                a+="|"+i7.next()+","+i7.hasNext()+","+x.offer("d")+","+x+","+i7.hasNext();
                LinkedBlockingQueue<String> y=q(5,"a","b","c");Iterator<String> i8=y.iterator();i8.next();
                List<String> sink=new ArrayList<>();y.drainTo(sink,2);
                a+="|"+i8.next()+","+i8.next()+","+i8.hasNext()+","+y+","+sink;
                return a;
            }
            public static String textArraysAndClear(){
                LinkedBlockingQueue<Object> q=new LinkedBlockingQueue<>();
                q.offer("a");q.offer(1);q.offer('c');q.offer(List.of(2,3));
                String a=q.toString()+","+q.toArray().length+","+q.toArray()[3]+","+q.toArray().getClass().getSimpleName();
                String[] big={"1","2","3","4","5","6"};
                LinkedBlockingQueue<String> s=q(3,"x","y","z");
                String[] r1=s.toArray(new String[0]);String[] r2=s.toArray(big);String[] r3=s.toArray(new String[3]);
                a+="|"+r1.length+":"+String.join("/",r1)+","+(r2==big)+":"+r2[0]+r2[1]+r2[2]+":"+r2[3]+":"+r2[4]+","+r3.length+":"+r3[2]+","+r1.getClass().getSimpleName()+","+ty(()->s.toArray((String[])null));
                Object[] o=s.toArray(new Object[1]);
                a+="|"+o.length+":"+o[0]+":"+o.getClass().getSimpleName()+","+new LinkedBlockingQueue<String>().toArray(new String[0]).length+","+new LinkedBlockingQueue<String>().toArray().length;
                LinkedBlockingQueue<Object> b=new LinkedBlockingQueue<>();b.offer("x");b.offer(new Blank());b.offer(b);
                a+="|"+b+","+b.size();
                LinkedBlockingQueue<String> c=q(3,"a","b","c");
                c.clear();
                a+="|"+c+","+c.size()+","+c.remainingCapacity()+","+c.isEmpty()+","+c.peek()+","+c.offer("d")+","+c.offer("e")+","+c.offer("f")+","+c.offer("g")+","+c+","+c.poll();
                c.clear();c.clear();
                return a+"|"+c+","+c.remainingCapacity()+","+c.offer("z")+","+c;
            }
            public static String subclassCalls(){
                Spy<String> p=new Spy<>(List.of("a","b"));
                String a=p.log+"|";
                p.add("c");a+=p.log+"|";p.log.setLength(0);
                p.addAll(List.of("d"));p.isEmpty();a+=p.log+"|";p.log.setLength(0);
                p.toString();a+=p.log+"|";p.log.setLength(0);
                p.toArray(new String[0]);p.containsAll(List.of("a","d"));a+=p.log+"|";p.log.setLength(0);
                p.remove();p.element();a+=p.log+"|";p.log.setLength(0);
                a+=t(()->{p.put("e");return p.take();})+" "+p.log+"|";p.log.setLength(0);
                List<String> sink=new ArrayList<>();
                a+=p.drainTo(sink,1)+","+sink+" "+p.log+"|";p.log.setLength(0);
                p.remove("zz");p.remove("c");a+=p.log+"|";p.log.setLength(0);
                a+=p.drainTo(sink)+","+sink+" "+p.log+"|";p.log.setLength(0);
                p.offer("f");p.offer("g");p.log.setLength(0);
                p.clear();
                a+=p.log+","+p.isEmpty()+","+t(()->p.remove())+","+t(()->p.element())+" "+p.log;
                Spy<String> small=new Spy<>(1);small.offer("x");small.log.setLength(0);
                return a+"|"+t(()->small.add("y"))+","+t(()->small.addAll(List.of("y")))+" "+small.log;
            }
        }
        """;

    @Test void capacityAndOffer() throws Exception {
        matchesJvm(FIXTURE, "capacityAndOffer");
    }

    @Test void putAndTakeWithoutBlocking() throws Exception {
        matchesJvm(FIXTURE, "putAndTake");
    }

    @Test void constructorsAndAddAll() throws Exception {
        matchesJvm(FIXTURE, "constructorsAndAddAll");
    }

    @Test void containsAndRemove() throws Exception {
        matchesJvm(FIXTURE, "containsAndRemove");
    }

    @Test void drainTo() throws Exception {
        matchesJvm(FIXTURE, "drainTo");
    }

    @Test void weaklyConsistentIterators() throws Exception {
        matchesJvm(FIXTURE, "iterators");
    }

    @Test void textArraysClearAndSubclasses() throws Exception {
        matchesJvm(FIXTURE, "textArraysAndClear", "subclassCalls");
    }

    @Test void leavesOutTimedOperationsAndSerialization() {
        rejectsUnusedCall("java/util/concurrent/LinkedBlockingQueue", "writeObject", "(Ljava/io/ObjectOutputStream;)V", INVOKESPECIAL, false);
    }
}
