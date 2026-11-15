package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * T30: AtomicReferenceArray getAndUpdate/updateAndGet/getAndAccumulate/accumulateAndGet must follow the JDK 21
 * compare-and-set retry loop. Expected values come from the JVM; see tasks/T30-atomic-array-updates.md.
 */
@Tag("agent-pending")
class BytecodeAtomicReferenceArrayUpdatesTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.util.concurrent.atomic.AtomicReferenceArray;

        /** Library-style array: overrides toString (the only non-final method) and updates its own slots. */
        class Slots extends AtomicReferenceArray<String> {
            final StringBuilder log=new StringBuilder();
            Slots(String... values){super(values);}
            String bump(int index){
                return getAndUpdate(index,v->{log.append("bump(").append(v).append(") ");if(v.length()==1)set(index,v+v);return v+"+";});
            }
            public String toString(){return "slots"+super.toString();}
        }

        public class Fixture {
            interface Op{Object run() throws Exception;}
            static String t(Op op){
                try{return String.valueOf(op.run());}
                catch(RuntimeException e){return e.getClass().getSimpleName()+":"+e.getMessage();}
                catch(Exception e){return "checked:"+e;}
            }
            static String npe(Op op){
                try{return String.valueOf(op.run());}
                catch(NullPointerException e){return "NullPointerException";}
                catch(Exception e){return e.getClass().getSimpleName()+":"+e.getMessage();}
            }

            public static String getAndUpdateRetries(){
                AtomicReferenceArray<String> a=new AtomicReferenceArray<>(new String[]{"a","b"});
                StringBuilder log=new StringBuilder();
                String other=new String("other");
                int[] calls={0};
                String r=a.getAndUpdate(0,v->{log.append("f(").append(v).append(") ");if(calls[0]++==0)a.set(0,other);return v+"!";});
                return r+":"+a+","+(r==other)+",calls="+calls[0]+"|log="+log;
            }
            public static String updateAndGetRetries(){
                AtomicReferenceArray<String> a=new AtomicReferenceArray<>(new String[]{"x","y"});
                StringBuilder log=new StringBuilder();
                int[] calls={0};
                String r=a.updateAndGet(1,v->{log.append("u(").append(v).append(") ");if(calls[0]++<2)a.set(1,v+calls[0]);return v+"?";});
                return r+":"+a+",calls="+calls[0]+"|log="+log;
            }
            public static String equalButNotSame(){
                AtomicReferenceArray<String> a=new AtomicReferenceArray<>(new String[]{"k"});
                int[] calls={0};
                String r=a.updateAndGet(0,v->{if(calls[0]++==0)a.set(0,new String(v));return v+calls[0];});
                return r+":"+a+",calls="+calls[0];
            }
            public static String sameReferenceAndOtherSlots(){
                AtomicReferenceArray<String> a=new AtomicReferenceArray<>(new String[]{"p","q","r"});
                String p=a.get(0);
                int[] calls={0};
                String r1=a.getAndUpdate(0,v->{calls[0]++;a.set(0,p);return "P";});
                String s=r1+":"+a+",calls="+calls[0];
                calls[0]=0;
                String r2=a.updateAndGet(1,v->{calls[0]++;a.set(0,"zero");a.set(2,"two");return v+v;});
                s+="|"+r2+":"+a+",calls="+calls[0];
                calls[0]=0;
                String q=a.get(1);
                String r3=a.getAndUpdate(1,v->{calls[0]++;a.set(1,"tmp");a.set(1,q);return "back";});
                return s+"|"+r3+":"+a+","+(r3==q)+",calls="+calls[0];
            }
            public static String nestedUpdate(){
                AtomicReferenceArray<String> a=new AtomicReferenceArray<>(new String[]{"a"});
                StringBuilder log=new StringBuilder();
                int[] calls={0};
                String r=a.getAndUpdate(0,v->{log.append("outer(").append(v).append(") ");
                    if(calls[0]++==0)log.append("inner=").append(a.updateAndGet(0,w->{log.append("in(").append(w).append(") ");return w+"i";})).append(' ');
                    return v+"o";});
                return r+":"+a+",calls="+calls[0]+"|log="+log;
            }

            public static String getAndAccumulateRetries(){
                AtomicReferenceArray<String> a=new AtomicReferenceArray<>(new String[]{"a","b"});
                StringBuilder log=new StringBuilder();
                int[] calls={0};
                String r=a.getAndAccumulate(1,"x",(v,x)->{log.append("g(").append(v).append(',').append(x).append(") ");if(calls[0]++==0)a.set(1,"c");return v+x;});
                return r+":"+a+",calls="+calls[0]+"|log="+log;
            }
            public static String accumulateAndGetRetries(){
                AtomicReferenceArray<Object> a=new AtomicReferenceArray<>(3);
                StringBuilder log=new StringBuilder();
                int[] calls={0};
                Object r=a.accumulateAndGet(2,"y",(v,x)->{log.append("h(").append(v).append(',').append(x).append(") ");if(calls[0]++<2)a.set(2,calls[0]);return v+"|"+x;});
                return r+":"+a+",calls="+calls[0]+"|log="+log;
            }
            public static String accumulateNullArguments(){
                AtomicReferenceArray<String> a=new AtomicReferenceArray<>(2);
                StringBuilder log=new StringBuilder();
                int[] calls={0};
                String r1=a.getAndAccumulate(0,null,(v,x)->{log.append("n(").append(v).append(',').append(x).append(") ");if(calls[0]++==0)a.set(0,"set");return v+"/"+x;});
                String s=r1+":"+a+",calls="+calls[0];
                String r2=a.accumulateAndGet(1,"w",(v,x)->null);
                return s+"|"+r2+":"+a+"|log="+log;
            }

            public static String throwAfterRetry(){
                AtomicReferenceArray<String> a=new AtomicReferenceArray<>(new String[]{"a","b"});
                StringBuilder log=new StringBuilder();
                int[] calls={0};
                String r=t(()->a.accumulateAndGet(0,"z",(v,x)->{log.append("k(").append(v).append(") ");if(calls[0]++==0){a.set(0,"c");return v;}throw new IllegalStateException("stop:"+v);}));
                String s=r+":"+a+",calls="+calls[0];
                calls[0]=0;
                String r2=t(()->a.getAndUpdate(1,v->{log.append("m(").append(v).append(") ");if(calls[0]++==0){a.set(1,"d");return "ignored";}throw new UnsupportedOperationException(v);}));
                return s+"|"+r2+":"+a+",calls="+calls[0]+"|log="+log;
            }
            public static String throwFirstAndNullFunctions(){
                AtomicReferenceArray<String> a=new AtomicReferenceArray<>(new String[]{"keep",null});
                String s=t(()->a.getAndUpdate(0,v->{throw new IllegalStateException("no:"+v);}))+":"+a;
                s+=","+t(()->a.updateAndGet(0,v->{throw new IllegalArgumentException("bad");}))+":"+a;
                s+=","+t(()->a.getAndAccumulate(0,"z",(v,x)->{throw new ArithmeticException(v+x);}))+":"+a;
                s+="|"+npe(()->a.getAndUpdate(0,null))+","+npe(()->a.updateAndGet(1,null))+","+npe(()->a.getAndAccumulate(0,"z",null))+","+npe(()->a.accumulateAndGet(1,"z",null))+":"+a;
                s+="|"+npe(()->a.updateAndGet(1,v->v.trim()))+","+npe(()->a.getAndUpdate(1,v->v+"!"))+":"+a;
                return s;
            }
            public static String indexChecksBeforeFunction(){
                AtomicReferenceArray<String> a=new AtomicReferenceArray<>(new String[]{"a","b"});
                int[] calls={0};
                String s=t(()->a.getAndUpdate(2,v->{calls[0]++;return v;}))+","+t(()->a.updateAndGet(-1,v->{calls[0]++;return v;}))
                        +","+t(()->a.getAndAccumulate(5,"x",(v,x)->{calls[0]++;return v;}))+","+t(()->a.accumulateAndGet(Integer.MIN_VALUE,"x",(v,x)->{calls[0]++;return v;}))
                        +","+t(()->a.getAndUpdate(2,null));
                s+="|calls="+calls[0]+":"+a;
                int[] retries={0};
                s+="|"+t(()->a.updateAndGet(0,v->{if(retries[0]++==0)a.set(0,"A");return v+v;}))+":"+a+",calls="+retries[0];
                return s;
            }

            public static String subclassUpdates(){
                Slots s=new Slots("a","bc",null);
                String r=s.bump(0)+","+s.bump(1)+","+s+"|log="+s.log;
                int[] calls={0};
                String r2=s.accumulateAndGet(2,"!",(v,x)->{calls[0]++;if(calls[0]==1)s.set(2,"n");return v+x;});
                return r+"|"+r2+":"+s+",calls="+calls[0]+","+s.length();
            }
            public static String otherMembersUnchanged(){
                AtomicReferenceArray<String> a=new AtomicReferenceArray<>(3);
                String x1=new String("x"),x2=new String("x");
                a.set(0,x1);a.lazySet(1,"b");a.setPlain(2,"c");
                String s=a+","+a.compareAndSet(0,x2,"no")+","+a.compareAndSet(0,x1,"yes")+","+a.weakCompareAndSetVolatile(1,"b","B")+","+a.weakCompareAndSetVolatile(1,"b","C");
                s+=","+a.compareAndExchange(2,"zz","D")+","+a.compareAndExchange(2,"c","E")+","+a.getAndSet(0,null)+","+a;
                int[] calls={0};
                s+="|"+a.getAndUpdate(0,v->{calls[0]++;return "u";})+","+a.updateAndGet(1,v->{calls[0]++;return v+"!";})+","+a.getAndAccumulate(2,"+",(v,x)->{calls[0]++;return v+x;})+","+a.accumulateAndGet(2,"-",(v,x)->{calls[0]++;return x+v;})+":"+a+",calls="+calls[0];
                return s;
            }
        }
        """;

    @Test void updateFunctionsRetryWhenTheElementChanges() throws Exception {
        matchesJvm(FIXTURE, "getAndUpdateRetries", "updateAndGetRetries", "equalButNotSame");
    }

    @Test void updateFunctionsKeepTheirResultWhenTheElementIsTheSameReference() throws Exception {
        matchesJvm(FIXTURE, "sameReferenceAndOtherSlots", "nestedUpdate");
    }

    @Test void accumulateFunctionsRetryWithTheSameArgument() throws Exception {
        matchesJvm(FIXTURE, "getAndAccumulateRetries", "accumulateAndGetRetries", "accumulateNullArguments");
    }

    @Test void exceptionsLeaveTheElementAsTheFunctionLeftIt() throws Exception {
        matchesJvm(FIXTURE, "throwAfterRetry", "throwFirstAndNullFunctions", "indexChecksBeforeFunction");
    }

    @Test void subclassAndUnchangedMembers() throws Exception {
        matchesJvm(FIXTURE, "subclassUpdates", "otherMembersUnchanged");
    }
}
