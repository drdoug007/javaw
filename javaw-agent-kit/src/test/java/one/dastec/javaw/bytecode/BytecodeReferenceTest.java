package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** T83: java.lang.ref references and ReferenceQueue under the no-reclamation contract. Expected values come from the JVM; see tasks/T83-references.md. */
@Tag("agent-pending")
class BytecodeReferenceTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.lang.ref.PhantomReference;
        import java.lang.ref.Reference;
        import java.lang.ref.ReferenceQueue;
        import java.lang.ref.SoftReference;
        import java.lang.ref.WeakReference;

        /** Like a library subclass: logs the overridable calls the reference makes on itself. */
        class Spy<T> extends WeakReference<T> {
            static final StringBuilder log=new StringBuilder();
            Spy(T t,ReferenceQueue<? super T> q){super(t,q);}
            public T get(){log.append("get ");return super.get();}
            public void clear(){log.append("clear ");super.clear();}
            public boolean isEnqueued(){log.append("isEnqueued ");return super.isEnqueued();}
            public boolean enqueue(){log.append("enqueue ");return super.enqueue();}
        }
        /** A soft reference subclass that logs get. */
        class SoftSpy<T> extends SoftReference<T> {
            static final StringBuilder log=new StringBuilder();
            SoftSpy(T t){super(t);}
            public T get(){log.append("get ");return super.get();}
        }
        /** A cache entry like the libraries' WeakEntryReference: a reference that carries a key. */
        class Keyed extends WeakReference<Object> {
            final String key;
            Keyed(String key,Object value,ReferenceQueue<Object> q){super(value,q);this.key=key;}
            public String toString(){return "Keyed("+key+")";}
        }

        public class Fixture {
            interface Op{Object run() throws Exception;}
            static String t(Op op){
                try{return String.valueOf(op.run());}catch(Exception e){return e.getClass().getSimpleName()+":"+e.getMessage();}
            }
            /** Name of a polled reference (or null) without printing identity hashes. */
            static String n(Reference<?> r){return r==null?"null":r instanceof Keyed?r.toString():r.getClass().getSimpleName();}

            public static String basics(){
                String a="A",b="B";
                WeakReference<String> w=new WeakReference<>(a);
                SoftReference<String> s=new SoftReference<>(b);
                String r=w.get()+","+s.get()+","+(w.get()==a)+","+w.refersTo(a)+","+w.refersTo(b)+","+w.refersTo(null)+","+w.isEnqueued()+","+s.isEnqueued();
                w.clear();
                r+="|"+w.get()+","+w.refersTo(a)+","+w.refersTo(null)+","+w.isEnqueued();
                w.clear();
                r+=","+w.get();
                WeakReference<Object> nul=new WeakReference<>(null);
                SoftReference<Object> snul=new SoftReference<>(null,null);
                r+="|"+nul.get()+","+nul.refersTo(null)+","+nul.refersTo(a)+","+snul.get()+","+snul.refersTo(null)+","+nul.enqueue()+","+snul.enqueue()+","+nul.isEnqueued();
                WeakReference<String> wq=new WeakReference<>(a,null);
                r+="|"+wq.get()+","+wq.enqueue()+","+wq.get()+","+wq.isEnqueued()+","+wq.enqueue();
                s.clear();
                r+="|"+s.get()+","+s.refersTo(b)+","+s.refersTo(null);
                Object o=w;
                r+="|"+(o instanceof Reference)+","+(o instanceof java.io.Serializable)+","+(o instanceof Cloneable)+","+(s instanceof Reference);
                return r;
            }

            public static String queueing() throws Exception {
                ReferenceQueue<Object> q=new ReferenceQueue<>();
                Object x=new Object(),y=new Object(),z=new Object();
                Keyed kx=new Keyed("x",x,q),ky=new Keyed("y",y,q),kz=new Keyed("z",z,q);
                String r=n(q.poll())+","+kx.isEnqueued()+","+(kx.get()==x);
                r+="|"+kx.enqueue()+","+kx.isEnqueued()+","+kx.get()+","+kx.refersTo(null)+","+kx.refersTo(x)+","+kx.enqueue();
                r+="|"+ky.enqueue()+","+kz.enqueue()+","+n(q.poll())+","+kz.isEnqueued()+","+ky.isEnqueued()+","+n(q.poll())+","+n(q.poll())+","+n(q.poll());
                r+="|"+kx.isEnqueued()+","+kx.enqueue()+","+kx.isEnqueued()+","+n(q.poll());
                Keyed ka=new Keyed("a",x,q),kb=new Keyed("b",y,q),kc=new Keyed("c",z,q);
                ka.enqueue();kb.enqueue();
                r+="|"+n(q.remove())+","+n(q.remove(0))+","+kc.enqueue()+","+n(q.remove(5))+","+n(q.remove(1))+","+t(()->q.remove(-1))+","+t(()->n(q.remove(-5)));
                kc.clear();
                r+="|"+kc.enqueue()+","+n(q.poll());
                ReferenceQueue<String> sq=new ReferenceQueue<>();
                SoftReference<String> s1=new SoftReference<>("s1",sq);
                WeakReference<String> w1=new WeakReference<>("w1",sq);
                r+="|"+s1.enqueue()+","+w1.enqueue()+","+n(sq.poll())+","+n(sq.poll())+","+n(sq.poll())+","+s1.get()+","+w1.get();
                ReferenceQueue<Object> other=new ReferenceQueue<>();
                Keyed ko=new Keyed("o",x,other);
                ko.enqueue();
                r+="|"+n(q.poll())+","+n(other.poll())+","+q.equals(other)+","+q.equals(q);
                return r;
            }

            public static String phantoms() throws Exception {
                ReferenceQueue<String> q=new ReferenceQueue<>();
                String a="A";
                PhantomReference<String> p=new PhantomReference<>(a,q);
                String r=p.get()+","+p.refersTo(a)+","+p.refersTo(null)+","+p.refersTo("B")+","+p.isEnqueued();
                r+="|"+p.enqueue()+","+p.get()+","+p.refersTo(a)+","+p.refersTo(null)+","+p.isEnqueued()+","+(q.poll()==p)+","+p.isEnqueued()+","+p.enqueue();
                PhantomReference<String> pn=new PhantomReference<>(a,null);
                r+="|"+pn.get()+","+pn.refersTo(a)+","+pn.enqueue()+","+pn.refersTo(a)+","+pn.refersTo(null);
                PhantomReference<String> pc=new PhantomReference<>(a,q);
                pc.clear();
                r+="|"+pc.refersTo(a)+","+pc.refersTo(null)+","+pc.enqueue()+","+(q.poll()==pc);
                return r;
            }

            public static String subclassCalls() throws Exception {
                ReferenceQueue<Object> q=new ReferenceQueue<>();
                Object x=new Object();
                Spy<Object> s=new Spy<>(x,q);
                String r=s.refersTo(x)+":"+Spy.log;Spy.log.setLength(0);
                r+="|"+s.enqueue()+":"+Spy.log;Spy.log.setLength(0);
                r+="|"+s.refersTo(null)+","+(q.poll()==s)+":"+Spy.log;Spy.log.setLength(0);
                r+="|"+s.enqueue()+":"+Spy.log;Spy.log.setLength(0);
                Spy<Object> s2=new Spy<>(x,q);
                s2.clear();
                r+="|"+s2.refersTo(x)+","+s2.get()+":"+Spy.log;Spy.log.setLength(0);
                SoftSpy<String> ss=new SoftSpy<>("v");
                r+="|"+ss.refersTo("v")+","+ss.enqueue()+","+ss.refersTo(null)+":"+SoftSpy.log;
                return r;
            }

            public static String identity(){
                Object x=new Object();
                WeakReference<Object> a=new WeakReference<>(x),b=new WeakReference<>(x);
                SoftReference<Object> c=new SoftReference<>(x);
                String r=a.equals(b)+","+a.equals(a)+","+a.equals(c)+","+(a.hashCode()==a.hashCode());
                a.clear();b.clear();
                r+=","+a.equals(b);
                ReferenceQueue<Object> q1=new ReferenceQueue<>(),q2=new ReferenceQueue<>();
                r+="|"+q1.equals(q2)+","+q1.equals(q1)+","+(q1.hashCode()==q1.hashCode())+","+a.toString().startsWith("java.lang.ref.WeakReference@")+","+q1.toString().startsWith("java.lang.ref.ReferenceQueue@");
                return r;
            }
        }
        """;

    @Test void getRefersToAndClear() throws Exception {
        matchesJvm(FIXTURE, "basics");
    }

    @Test void enqueueAndQueueOrder() throws Exception {
        matchesJvm(FIXTURE, "queueing");
    }

    @Test void phantomReferences() throws Exception {
        matchesJvm(FIXTURE, "phantoms");
    }

    @Test void subclassSelfCalls() throws Exception {
        matchesJvm(FIXTURE, "subclassCalls");
    }

    @Test void identityEqualsAndToString() throws Exception {
        matchesJvm(FIXTURE, "identity");
    }

    @Test void leavesOutCleanerAndReachabilityFence() {
        rejectsUnusedCall("java/lang/ref/Cleaner", "create", "()Ljava/lang/ref/Cleaner;", INVOKESTATIC, false);
        rejectsUnusedCall("java/lang/ref/Reference", "reachabilityFence", "(Ljava/lang/Object;)V", INVOKESTATIC, false);
    }
}
