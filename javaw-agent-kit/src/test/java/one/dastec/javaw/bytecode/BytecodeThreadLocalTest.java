package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T72: java.lang.ThreadLocal and InheritableThreadLocal (single-threaded). See tasks/T72-thread-local.md. */

class BytecodeThreadLocalTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.util.function.Supplier;
        class Spy extends ThreadLocal<String> {
            final StringBuilder log=new StringBuilder();int made;
            protected String initialValue(){made++;log.append("init;");return "v"+made;}
            public String get(){log.append("get;");return super.get();}
            public void set(String v){log.append("set:").append(v).append(';');super.set(v);}
            public void remove(){log.append("remove;");super.remove();}
        }
        class Sneaky extends ThreadLocal<String> {
            protected String initialValue(){set("inner");return "outer";}
        }
        class Failing extends ThreadLocal<Integer> {
            int calls;
            protected Integer initialValue(){calls++;if(calls==1)throw new IllegalStateException("first");return calls;}
        }
        class Inherit extends InheritableThreadLocal<StringBuilder> {
            int made;final StringBuilder log=new StringBuilder();
            protected StringBuilder initialValue(){made++;log.append("init;");return new StringBuilder("i"+made);}
            protected StringBuilder childValue(StringBuilder parent){log.append("child;");return new StringBuilder(parent).append("+c");}
            StringBuilder child(StringBuilder p){return childValue(p);}
        }
        class PlainInherit extends InheritableThreadLocal<Object> {
            Object child(Object p){return childValue(p);}
        }
        public class Fixture {
            static String name(Throwable t){return t.getClass().getName();}

            public static String basics(){
                StringBuilder out=new StringBuilder();
                ThreadLocal<String> t=new ThreadLocal<>();
                out.append(t.get()).append(',');
                t.set("a");out.append(t.get()).append(',');
                t.set("b");out.append(t.get()).append(',');
                t.remove();out.append(t.get()).append(',');
                t.remove();t.remove();out.append(t.get()).append(',');
                t.set(null);out.append(t.get()).append(',');
                ThreadLocal<Integer> u=new ThreadLocal<>();
                u.set(7);t.set("x");
                out.append(u.get()).append(t.get()).append(',');
                u.remove();out.append(u.get()).append(t.get()).append(';');
                ThreadLocal<int[]> counter=new ThreadLocal<>();
                counter.set(new int[1]);
                for(int i=0;i<5;i++)counter.get()[0]+=i;
                out.append(counter.get()[0]).append(',').append(counter.get()==counter.get());
                return out.toString();
            }
            public static String lazyInitialValue(){
                StringBuilder out=new StringBuilder();
                Spy s=new Spy();
                out.append(s.made).append(';');
                out.append(s.get()).append(s.get()).append(';').append(s.log).append('|');s.log.setLength(0);
                s.remove();out.append(s.get()).append(';').append(s.log).append('|');s.log.setLength(0);
                s.set("z");out.append(s.get()).append(';').append(s.log).append('|');s.log.setLength(0);
                s.set(null);out.append(s.get()).append(';').append(s.log).append('|');s.log.setLength(0);
                s.remove();s.remove();out.append(s.get()).append(';').append(s.log).append('|');s.log.setLength(0);
                out.append(s.made);
                return out.toString();
            }
            public static String initialValueEdges(){
                StringBuilder out=new StringBuilder();
                Sneaky k=new Sneaky();
                out.append(k.get()).append(',').append(k.get()).append(';');
                k.remove();k.set("own");out.append(k.get()).append(';');
                Failing f=new Failing();
                try{f.get();out.append("no;");}catch(IllegalStateException e){out.append(name(e)).append(':').append(e.getMessage()).append(';');}
                out.append(f.get()).append(',').append(f.get()).append(',').append(f.calls).append(';');
                f.remove();out.append(f.get()).append(',').append(f.calls).append(';');
                ThreadLocal<String> plain=new ThreadLocal<String>(){};
                out.append(plain.get()).append(',');
                ThreadLocal<String> anon=new ThreadLocal<String>(){
                    int n;protected String initialValue(){return "anon"+(++n);}
                };
                out.append(anon.get()).append(anon.get());anon.remove();out.append(anon.get());
                return out.toString();
            }
            public static String withInitial(){
                StringBuilder out=new StringBuilder();
                int[] calls=new int[1];
                ThreadLocal<String> w=ThreadLocal.withInitial(()->"s"+(++calls[0]));
                out.append(calls[0]).append(',').append(w.get()).append(w.get()).append(',').append(calls[0]).append(';');
                w.set("set");out.append(w.get()).append(',').append(calls[0]).append(';');
                w.remove();out.append(w.get()).append(',').append(calls[0]).append(';');
                ThreadLocal<Object> n=ThreadLocal.withInitial(()->{calls[0]+=10;return null;});
                out.append(n.get()).append(n.get()).append(',').append(calls[0]).append(';');
                try{ThreadLocal.withInitial(null);out.append("no;");}catch(NullPointerException e){out.append(name(e)).append(';');}
                Supplier<StringBuilder> sup=StringBuilder::new;
                ThreadLocal<StringBuilder> buf=ThreadLocal.withInitial(sup);
                buf.get().append("ab");buf.get().append("cd");out.append(buf.get()).append(';');
                ThreadLocal<Integer> boom=ThreadLocal.withInitial(()->{throw new UnsupportedOperationException("nope");});
                try{boom.get();}catch(UnsupportedOperationException e){out.append(e.getMessage()).append(',');}
                boom.set(3);out.append(boom.get()).append(';');
                out.append(w instanceof ThreadLocal).append(',').append(w.getClass()==ThreadLocal.class).append(',');
                out.append(w.getClass().getName()).append(',').append(w.getClass().getSuperclass()==ThreadLocal.class);
                return out.toString();
            }
            public static String inheritable(){
                StringBuilder out=new StringBuilder();
                InheritableThreadLocal<String> i=new InheritableThreadLocal<>();
                out.append(i.get()).append(',');
                i.set("p");out.append(i.get()).append(',');
                i.remove();out.append(i.get()).append(',');
                out.append((Object)i instanceof ThreadLocal).append(';');
                ThreadLocal<String> asBase=i;asBase.set("base");out.append(i.get()).append(';');
                Inherit h=new Inherit();
                out.append(h.get()).append(',').append(h.get()).append(',').append(h.made).append(',').append(h.log).append(';');
                StringBuilder c=h.child(h.get());
                out.append(c).append(',').append(c==h.get()).append(',').append(h.get()).append(',').append(h.log).append(';');
                h.remove();out.append(h.get()).append(',').append(h.made).append(';');
                PlainInherit pi=new PlainInherit();
                Object o=new Object();
                out.append(pi.child(o)==o).append(',').append(pi.child(null)).append(',').append(pi.get());
                return out.toString();
            }
            public static String patterns(){
                StringBuilder out=new StringBuilder();
                ThreadLocal<java.util.ArrayList<String>> stack=ThreadLocal.withInitial(java.util.ArrayList::new);
                stack.get().add("a");stack.get().add("b");
                out.append(stack.get()).append(',');
                stack.get().remove(stack.get().size()-1);
                out.append(stack.get()).append(',');
                stack.remove();out.append(stack.get()).append(';');
                ThreadLocal<String> holder=new ThreadLocal<>();
                String prev=holder.get();
                try{holder.set("request");out.append(holder.get()).append(',');throw new RuntimeException("fail");}
                catch(RuntimeException e){out.append(e.getMessage()).append(',');}
                finally{if(prev==null)holder.remove();else holder.set(prev);}
                out.append(holder.get()).append(';');
                ThreadLocal<?>[] many=new ThreadLocal<?>[20];
                for(int k=0;k<many.length;k++){ThreadLocal<Integer> t=new ThreadLocal<>();t.set(k*k);many[k]=t;}
                int sum=0;for(ThreadLocal<?> t:many)sum+=(Integer)t.get();
                for(int k=0;k<many.length;k+=2)many[k].remove();
                int left=0;for(ThreadLocal<?> t:many)if(t.get()!=null)left++;
                out.append(sum).append(',').append(left);
                return out.toString();
            }
        }
        """;

    @Test void getSetRemove() throws Exception {
        matchesJvm(FIXTURE, "basics", "patterns");
    }

    @Test void initialValues() throws Exception {
        matchesJvm(FIXTURE, "lazyInitialValue", "initialValueEdges", "withInitial");
    }

    @Test void inheritableThreadLocal() throws Exception {
        matchesJvm(FIXTURE, "inheritable");
    }

    @Test void leavesOutThreadInternals() {
        rejectsUnusedCall("java/lang/Thread", "currentThread", "()Ljava/lang/Thread;", INVOKESTATIC, false);
        rejectsUnusedCall("java/lang/ThreadLocal", "nextHashCode", "()I", INVOKESTATIC, false);
        rejectsUnusedCall("java/lang/ThreadLocal", "getMap", "(Ljava/lang/Thread;)Ljava/lang/ThreadLocal$ThreadLocalMap;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/lang/InheritableThreadLocal", "createMap", "(Ljava/lang/Thread;Ljava/lang/Object;)V", INVOKEVIRTUAL, false);
    }
}
