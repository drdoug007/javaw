package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T29: java.util.concurrent.atomic.AtomicBoolean and AtomicReference. Expected values come from the JVM; see tasks/T29-atomic-boolean-reference.md. */
class BytecodeAtomicBooleanReferenceTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.util.concurrent.atomic.AtomicBoolean;
        import java.util.concurrent.atomic.AtomicReference;
        import java.util.function.BinaryOperator;
        import java.util.function.UnaryOperator;

        /** Library-style flag: overrides the two non-final weak CAS methods and toString, and logs every call they receive. */
        class LoggedFlag extends AtomicBoolean {
            final StringBuilder log=new StringBuilder();
            LoggedFlag(boolean initial){super(initial);}
            public boolean weakCompareAndSet(boolean expected,boolean next){log.append("weak(").append(expected).append(',').append(next).append(") ");return super.weakCompareAndSet(expected,next);}
            public boolean weakCompareAndSetPlain(boolean expected,boolean next){log.append("plain(").append(expected).append(',').append(next).append(") ");return super.weakCompareAndSetPlain(expected,next);}
            public String toString(){log.append("toString ");return "flag="+super.toString();}
        }
        /** A subclass that keeps the inherited toString. */
        class Switch extends AtomicBoolean {
            Switch(){}
        }
        /** Library-style named holder: overrides toString, the only non-final AtomicReference method. */
        class Holder<T> extends AtomicReference<T> {
            final String name;
            Holder(String name,T initial){super(initial);this.name=name;}
            public String toString(){return name+"->"+super.toString();}
        }
        /** A subclass that keeps the inherited toString and starts at null. */
        class Slot<T> extends AtomicReference<T> {
            Slot(){}
        }
        /** equals() is always true: compareAndSet must compare references, never call equals. */
        class Same {
            static int equalsCalls;
            final String label;
            Same(String label){this.label=label;}
            public boolean equals(Object other){equalsCalls++;return true;}
            public int hashCode(){return 0;}
            public String toString(){return label;}
        }
        /** Counts toString calls, so the test sees how often AtomicReference.toString asks the value. */
        class Tag {
            int calls;
            public String toString(){calls++;return "tag"+calls;}
        }

        public class Fixture {
            interface Op{Object run() throws Exception;}
            static String t(Op op){
                try{return String.valueOf(op.run());}
                catch(NullPointerException e){return "NullPointerException";}
                catch(Exception e){return e.getClass().getSimpleName()+":"+e.getMessage();}
            }

            public static String booleanBasics(){
                AtomicBoolean a=new AtomicBoolean(),b=new AtomicBoolean(true),c=new AtomicBoolean(false);
                String s=a.get()+","+b.get()+","+c.get()+","+a+","+b+","+a.toString()+b.toString();
                a.set(true);b.set(false);
                s+="|"+a.get()+","+b.get();
                a.lazySet(false);b.lazySet(true);
                s+="|"+a+","+b;
                s+="|"+a.getAndSet(true)+","+a.get()+","+a.getAndSet(true)+","+a.getAndSet(false)+","+a.get();
                return s;
            }
            public static String booleanAccessModes(){
                AtomicBoolean a=new AtomicBoolean(true);
                String s=a.getPlain()+","+a.getOpaque()+","+a.getAcquire();
                a.setPlain(false);s+="|"+a.get()+a.getOpaque();
                a.setOpaque(true);s+="|"+a.getAcquire()+a.getPlain();
                a.setRelease(false);s+="|"+a.get()+a.getPlain();
                a.setRelease(true);s+="|"+a;
                return s;
            }
            public static String booleanIdentityAndSerializable(){
                AtomicBoolean a=new AtomicBoolean(true),b=new AtomicBoolean(true);
                Object o=a;
                return a.equals(a)+","+a.equals(b)+","+b.equals(a)+","+a.equals(null)+","+(a.hashCode()==a.hashCode())+","+(o instanceof java.io.Serializable)
                        +","+("x"+new AtomicBoolean())+","+String.valueOf(new AtomicBoolean(true));
            }
            public static String booleanCompareAndSet(){
                AtomicBoolean a=new AtomicBoolean();
                String s=a.compareAndSet(true,false)+":"+a+","+a.compareAndSet(false,true)+":"+a+","+a.compareAndSet(false,true)+":"+a+","+a.compareAndSet(true,true)+":"+a;
                s+="|"+a.weakCompareAndSet(false,false)+":"+a+","+a.weakCompareAndSet(true,false)+":"+a;
                s+="|"+a.weakCompareAndSetPlain(true,true)+":"+a+","+a.weakCompareAndSetPlain(false,true)+":"+a;
                s+="|"+a.weakCompareAndSetVolatile(false,false)+":"+a+","+a.weakCompareAndSetVolatile(true,false)+":"+a;
                s+="|"+a.weakCompareAndSetAcquire(true,true)+":"+a+","+a.weakCompareAndSetAcquire(false,true)+":"+a;
                s+="|"+a.weakCompareAndSetRelease(false,true)+":"+a+","+a.weakCompareAndSetRelease(true,false)+":"+a;
                return s;
            }
            public static String booleanCompareAndExchange(){
                AtomicBoolean a=new AtomicBoolean(true);
                String s=a.compareAndExchange(false,true)+":"+a+","+a.compareAndExchange(true,false)+":"+a+","+a.compareAndExchange(true,true)+":"+a+","+a.compareAndExchange(false,false)+":"+a;
                s+="|"+a.compareAndExchangeAcquire(true,false)+":"+a+","+a.compareAndExchangeAcquire(false,true)+":"+a;
                s+="|"+a.compareAndExchangeRelease(false,false)+":"+a+","+a.compareAndExchangeRelease(true,false)+":"+a;
                return s;
            }
            public static String booleanSubclassHooks(){
                LoggedFlag f=new LoggedFlag(false);
                String s=f.compareAndSet(false,true)+","+f.weakCompareAndSetVolatile(true,false)+","+f.weakCompareAndSetAcquire(false,true)+","+f.weakCompareAndSetRelease(true,false)
                        +","+f.compareAndExchange(false,true)+","+f.compareAndExchangeAcquire(true,false)+","+f.compareAndExchangeRelease(false,true)+","+f.getAndSet(false)+","+f.get();
                f.set(true);f.lazySet(false);f.setPlain(true);f.setOpaque(false);f.setRelease(true);
                s+=","+f.getPlain()+f.getOpaque()+f.getAcquire()+"|log=["+f.log+"]";
                s+="|"+f.weakCompareAndSet(true,false)+","+f.weakCompareAndSetPlain(false,true)+","+f.weakCompareAndSet(false,false)+",value="+f.get()+"|log=["+f.log+"]";
                s+="|"+f+"|log=["+f.log+"]";
                Switch w=new Switch();w.set(true);
                return s+"|"+w+","+w.getAndSet(false)+","+w;
            }

            public static String referenceBasics(){
                AtomicReference<String> a=new AtomicReference<>(),b=new AtomicReference<>("one");
                String s=a.get()+","+b.get()+","+a+","+b+","+new AtomicReference<String>(null).get();
                a.set("two");b.set(null);
                s+="|"+a.get()+","+b.get()+","+b;
                a.lazySet("three");
                s+="|"+a;
                s+="|"+a.getAndSet("four")+","+a.getAndSet(null)+","+a.get()+","+a.getAndSet(null)+","+a;
                return s;
            }
            public static String referenceAccessModes(){
                AtomicReference<String> a=new AtomicReference<>("p");
                String s=a.getPlain()+a.getOpaque()+a.getAcquire();
                a.setPlain("q");s+="|"+a.get()+a.getOpaque();
                a.setOpaque("r");s+="|"+a.getAcquire()+a.getPlain();
                a.setRelease(null);s+="|"+a.get()+a.getPlain();
                a.setRelease("s");s+="|"+a;
                return s;
            }
            public static String referenceToString(){
                Tag tag=new Tag();
                AtomicReference<Object> a=new AtomicReference<>(tag);
                String s=a+","+a.toString()+",calls="+tag.calls;
                a.set(42);s+="|"+a;
                a.set(new StringBuilder("sb"));s+="|"+a;
                a.set('c');s+="|"+a;
                a.set(Boolean.FALSE);s+="|"+a;
                a.set(new AtomicBoolean(true));s+="|"+a;
                a.set(new AtomicReference<>(new AtomicReference<>("deep")));s+="|"+a;
                a.set(null);s+="|"+a+","+String.valueOf(a)+","+("["+a+"]");
                Object o=a;
                return s+","+(o instanceof java.io.Serializable)+","+a.equals(new AtomicReference<>(null))+","+a.equals(a);
            }
            public static String referenceCompareAndSet(){
                String x1=new String("x"),x2=new String("x"),y="y",z="z";
                AtomicReference<String> a=new AtomicReference<>(x1);
                String s=a.compareAndSet(x2,y)+":"+a+","+(a.get()==x1)+","+a.compareAndSet(x1,y)+":"+a+","+a.compareAndSet(y,null)+":"+a+","+a.compareAndSet(null,z)+":"+a+","+a.compareAndSet(null,y)+":"+a;
                s+="|"+a.weakCompareAndSet(y,x1)+":"+a+","+a.weakCompareAndSet(z,x1)+":"+a+","+(a.get()==x1);
                s+="|"+a.weakCompareAndSetPlain(x2,y)+":"+a+","+a.weakCompareAndSetPlain(x1,y)+":"+a;
                s+="|"+a.weakCompareAndSetVolatile(z,null)+":"+a+","+a.weakCompareAndSetVolatile(y,null)+":"+a;
                s+="|"+a.weakCompareAndSetAcquire(z,y)+":"+a+","+a.weakCompareAndSetAcquire(null,z)+":"+a;
                s+="|"+a.weakCompareAndSetRelease(y,y)+":"+a+","+a.weakCompareAndSetRelease(z,z)+":"+a+","+(a.get()==z);
                return s;
            }
            public static String referenceCompareAndExchange(){
                String x1=new String("x"),x2=new String("x"),y="y";
                AtomicReference<String> a=new AtomicReference<>(x1);
                String w1=a.compareAndExchange(x2,y);
                String s=w1+":"+a+","+(w1==x1);
                String w2=a.compareAndExchange(x1,y);
                s+="|"+w2+":"+a+","+(w2==x1);
                s+="|"+a.compareAndExchange(y,null)+":"+a+","+a.compareAndExchange(y,x1)+":"+a+","+a.compareAndExchange(null,x2)+":"+a+","+(a.get()==x2);
                s+="|"+a.compareAndExchangeAcquire(x1,y)+":"+a+","+a.compareAndExchangeAcquire(x2,y)+":"+a;
                s+="|"+a.compareAndExchangeRelease(x1,null)+":"+a+","+a.compareAndExchangeRelease(y,null)+":"+a;
                return s;
            }
            public static String referenceNeverCallsEquals(){
                Same.equalsCalls=0;
                Same p=new Same("p"),q=new Same("q"),r=new Same("r");
                AtomicReference<Same> a=new AtomicReference<>(p);
                String s=a.compareAndSet(q,r)+":"+a+","+a.weakCompareAndSet(q,r)+","+a.weakCompareAndSetPlain(q,r)+","+a.weakCompareAndSetVolatile(q,r)
                        +","+a.weakCompareAndSetAcquire(q,r)+","+a.weakCompareAndSetRelease(q,r)+","+a.compareAndExchange(q,r)+","+a.compareAndExchangeAcquire(q,r)
                        +","+a.compareAndExchangeRelease(q,r)+":"+a+","+a.compareAndSet(p,q)+":"+a;
                s+="|"+a.getAndUpdate(v->r)+":"+a+","+a.accumulateAndGet(p,(v,x)->x)+":"+a;
                return s+"|equalsCalls="+Same.equalsCalls+","+p.equals(q)+",equalsCalls="+Same.equalsCalls;
            }

            public static String referenceUpdateFunctions(){
                AtomicReference<String> a=new AtomicReference<>("a");
                StringBuilder log=new StringBuilder();
                UnaryOperator<String> twice=v->{log.append("f(").append(v).append(") ");return v+v;};
                BinaryOperator<String> join=(v,x)->{log.append("g(").append(v).append(',').append(x).append(") ");return v+"-"+x;};
                String s=a.getAndUpdate(twice)+":"+a+","+a.updateAndGet(twice)+":"+a;
                s+="|"+a.getAndAccumulate("x",join)+":"+a+","+a.accumulateAndGet("y",join)+":"+a;
                s+="|"+a.updateAndGet(v->null)+":"+a+","+a.getAndUpdate(v->"back")+":"+a+","+a.accumulateAndGet(null,(v,x)->v+x)+":"+a+","+a.getAndAccumulate(null,(v,x)->x)+":"+a;
                AtomicReference<Integer> n=new AtomicReference<>(5);
                s+="|"+n.accumulateAndGet(7,(v,x)->v*10+x)+","+n.getAndAccumulate(3,(v,x)->v-x)+","+n.updateAndGet(v->-v)+","+n;
                return s+"|log="+log;
            }
            public static String referenceUpdateExceptions(){
                AtomicReference<String> a=new AtomicReference<>("keep");
                String s=t(()->a.getAndUpdate(v->{throw new IllegalStateException("no:"+v);}))+":"+a;
                s+=","+t(()->a.updateAndGet(v->{throw new IllegalArgumentException("bad");}))+":"+a;
                s+=","+t(()->a.getAndAccumulate("z",(v,x)->{throw new UnsupportedOperationException(v+x);}))+":"+a;
                s+=","+t(()->a.accumulateAndGet("z",(v,x)->{throw new ArithmeticException("acc");}))+":"+a;
                s+="|"+t(()->a.getAndUpdate(null))+","+t(()->a.updateAndGet(null))+","+t(()->a.getAndAccumulate("z",null))+","+t(()->a.accumulateAndGet("z",null))+":"+a;
                AtomicReference<String> empty=new AtomicReference<>();
                s+="|"+t(()->empty.updateAndGet(v->v.trim()))+","+t(()->empty.getAndUpdate(v->v+"!"))+":"+empty;
                return s;
            }
            public static String referenceUpdateRetries(){
                StringBuilder log=new StringBuilder();
                AtomicReference<String> a=new AtomicReference<>("a");
                String other=new String("other");
                int[] calls={0};
                String r1=a.getAndUpdate(v->{log.append("f(").append(v).append(") ");if(calls[0]++==0)a.set(other);return v+"!";});
                String s=r1+":"+a+","+(r1==other)+",calls="+calls[0];
                calls[0]=0;
                String r2=a.updateAndGet(v->{log.append("u(").append(v).append(") ");if(calls[0]++<2)a.set(new String(v));return v+"?";});
                s+="|"+r2+":"+a+",calls="+calls[0];
                calls[0]=0;
                String same=a.get();
                String r3=a.getAndUpdate(v->{log.append("s(").append(v).append(") ");calls[0]++;a.set(same);return "next";});
                s+="|"+r3+":"+a+","+(r3==same)+",calls="+calls[0];
                return s+"|log="+log;
            }
            public static String referenceAccumulateRetries(){
                StringBuilder log=new StringBuilder();
                AtomicReference<String> a=new AtomicReference<>("a");
                int[] calls={0};
                String r1=a.getAndAccumulate("x",(v,x)->{log.append("g(").append(v).append(',').append(x).append(") ");if(calls[0]++==0)a.set("b");return v+x;});
                String s=r1+":"+a+",calls="+calls[0];
                calls[0]=0;
                String r2=a.accumulateAndGet("y",(v,x)->{log.append("h(").append(v).append(',').append(x).append(") ");if(calls[0]++<2)a.set(v+"+");return v+x;});
                s+="|"+r2+":"+a+",calls="+calls[0];
                calls[0]=0;
                String r3=t(()->a.accumulateAndGet("z",(v,x)->{log.append("k(").append(v).append(") ");if(calls[0]++==0){a.set("c");return v;}throw new IllegalStateException("stop:"+v);}));
                s+="|"+r3+":"+a+",calls="+calls[0];
                return s+"|log="+log;
            }
            public static String referenceSubclass(){
                Holder<String> h=new Holder<>("cfg","on");
                String s=h+","+h.get()+","+h.getAndSet("off")+","+h+","+h.compareAndSet("x","y")+","+h.updateAndGet(v->v+"!")+","+h;
                h.set(null);s+=","+h+","+("<"+h+">");
                Slot<Integer> slot=new Slot<>();
                s+="|"+slot+","+slot.compareAndSet(null,7)+","+slot+","+slot.accumulateAndGet(5,(v,x)->v+x)+","+slot.getAndUpdate(v->v*2)+","+slot;
                AtomicReference<?> base=h;
                Object o=slot;
                return s+"|"+base.toString()+","+(o instanceof AtomicReference)+","+(o instanceof java.io.Serializable);
            }
        }
        """;

    @Test void atomicBooleanAccessors() throws Exception {
        matchesJvm(FIXTURE, "booleanBasics", "booleanAccessModes", "booleanIdentityAndSerializable");
    }

    @Test void atomicBooleanCompareAndSet() throws Exception {
        matchesJvm(FIXTURE, "booleanCompareAndSet", "booleanCompareAndExchange");
    }

    @Test void atomicBooleanSubclass() throws Exception {
        matchesJvm(FIXTURE, "booleanSubclassHooks");
    }

    @Test void atomicReferenceAccessors() throws Exception {
        matchesJvm(FIXTURE, "referenceBasics", "referenceAccessModes", "referenceToString");
    }

    @Test void atomicReferenceComparesIdentity() throws Exception {
        matchesJvm(FIXTURE, "referenceCompareAndSet", "referenceCompareAndExchange", "referenceNeverCallsEquals");
    }

    @Test void atomicReferenceUpdateFunctions() throws Exception {
        matchesJvm(FIXTURE, "referenceUpdateFunctions", "referenceUpdateExceptions");
    }

    @Test void atomicReferenceUpdateRetries() throws Exception {
        matchesJvm(FIXTURE, "referenceUpdateRetries", "referenceAccumulateRetries");
    }

    @Test void atomicReferenceSubclass() throws Exception {
        matchesJvm(FIXTURE, "referenceSubclass");
    }
}
