package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T28: java.util.concurrent.atomic.AtomicInteger and AtomicLong (single-threaded). See tasks/T28-atomic-integer-long.md. */
class BytecodeAtomicIntegerLongTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.util.concurrent.atomic.AtomicInteger;
        import java.util.concurrent.atomic.AtomicLong;
        import java.util.function.IntBinaryOperator;
        import java.util.function.IntUnaryOperator;
        import java.util.function.LongBinaryOperator;
        import java.util.function.LongUnaryOperator;
        class CountingInt extends AtomicInteger {
            final StringBuilder log=new StringBuilder();
            CountingInt(int v){super(v);}
            public int intValue(){log.append("i;");return super.intValue();}
            public long longValue(){log.append("l;");return super.longValue();}
            public float floatValue(){log.append("f;");return super.floatValue();}
            public double doubleValue(){log.append("d;");return super.doubleValue();}
        }
        class CountingLong extends AtomicLong {
            final StringBuilder log=new StringBuilder();
            CountingLong(long v){super(v);}
            public int intValue(){log.append("i;");return super.intValue();}
            public long longValue(){log.append("l;");return super.longValue();}
            public float floatValue(){log.append("f;");return super.floatValue();}
            public double doubleValue(){log.append("d;");return super.doubleValue();}
        }
        class Labelled extends AtomicInteger {
            Labelled(int v){super(v);}
            public String toString(){return "L"+super.toString();}
        }
        class Sequence extends AtomicLong {
            Sequence(){super(100L);}
            long next(){return incrementAndGet();}
        }
        public class Fixture {
            static String name(Throwable t){return t.getClass().getSimpleName()+":"+t.getMessage();}
            static String sum(Number n){return n.intValue()+"/"+n.longValue()+"/"+n.byteValue()+"/"+n.shortValue();}

            public static String intBasics(){
                AtomicInteger a=new AtomicInteger(),b=new AtomicInteger(-7);StringBuilder out=new StringBuilder();
                out.append(a.get()).append(',').append(b.get()).append(',').append(a).append(',').append(b.toString()).append(',');
                a.set(5);out.append(a.get()).append(',');
                a.lazySet(6);out.append(a.getPlain()).append(',');
                a.setPlain(7);out.append(a.getOpaque()).append(',');
                a.setOpaque(8);out.append(a.getAcquire()).append(',');
                a.setRelease(9);out.append(a.get()).append(',');
                out.append(a.getAndSet(Integer.MIN_VALUE)).append(',').append(a).append(',');
                out.append(new AtomicInteger(Integer.MAX_VALUE)).append(',');
                out.append(a.equals(a)).append(',').append(a.equals(new AtomicInteger(a.get()))).append(',');
                out.append((Object)a instanceof Number).append(',').append((Object)a instanceof java.io.Serializable);
                return out.toString();
            }
            public static String longBasics(){
                AtomicLong a=new AtomicLong(),b=new AtomicLong(-7L);StringBuilder out=new StringBuilder();
                out.append(a.get()).append(',').append(b.get()).append(',').append(a).append(',').append(b.toString()).append(',');
                a.set(5L);out.append(a.get()).append(',');
                a.lazySet(6L);out.append(a.getPlain()).append(',');
                a.setPlain(7L);out.append(a.getOpaque()).append(',');
                a.setOpaque(8L);out.append(a.getAcquire()).append(',');
                a.setRelease(9L);out.append(a.get()).append(',');
                out.append(a.getAndSet(Long.MIN_VALUE)).append(',').append(a).append(',');
                out.append(new AtomicLong(Long.MAX_VALUE)).append(',').append(new AtomicLong(1L<<40)).append(',');
                out.append(a.equals(a)).append(',').append(a.equals(new AtomicLong(a.get()))).append(',');
                out.append((Object)a instanceof Number).append(',').append((Object)a instanceof java.io.Serializable);
                return out.toString();
            }
            public static String subclasses(){
                Labelled l=new Labelled(3);Sequence s=new Sequence();
                l.incrementAndGet();s.next();s.next();
                return l+","+l.get()+","+String.valueOf(l)+","+s+","+s.get()+","+s.next();
            }

            public static String intArithmetic(){
                AtomicInteger a=new AtomicInteger(10);StringBuilder out=new StringBuilder();
                out.append(a.getAndIncrement()).append(',').append(a.get()).append(';');
                out.append(a.getAndDecrement()).append(',').append(a.get()).append(';');
                out.append(a.incrementAndGet()).append(',').append(a.decrementAndGet()).append(';');
                out.append(a.getAndAdd(5)).append(',').append(a.get()).append(';');
                out.append(a.addAndGet(-20)).append(',').append(a.getAndAdd(0)).append(',').append(a.addAndGet(0)).append(';');
                return out.toString();
            }
            public static String intOverflow(){
                AtomicInteger a=new AtomicInteger(Integer.MAX_VALUE);StringBuilder out=new StringBuilder();
                out.append(a.incrementAndGet()).append(',').append(a.decrementAndGet()).append(',');
                a.set(Integer.MIN_VALUE);out.append(a.getAndDecrement()).append(',').append(a.get()).append(',');
                out.append(a.addAndGet(Integer.MAX_VALUE)).append(',').append(a.getAndAdd(Integer.MIN_VALUE)).append(',').append(a.get()).append(',');
                a.set(-1);out.append(a.getAndIncrement()).append(',').append(a.get());
                return out.toString();
            }
            public static String longArithmetic(){
                AtomicLong a=new AtomicLong(10L);StringBuilder out=new StringBuilder();
                out.append(a.getAndIncrement()).append(',').append(a.get()).append(';');
                out.append(a.getAndDecrement()).append(',').append(a.get()).append(';');
                out.append(a.incrementAndGet()).append(',').append(a.decrementAndGet()).append(';');
                out.append(a.getAndAdd(5L)).append(',').append(a.get()).append(';');
                out.append(a.addAndGet(-20L)).append(',').append(a.getAndAdd(0L)).append(',').append(a.addAndGet(0L)).append(';');
                out.append(a.addAndGet(1L<<35)).append(',').append(a.getAndAdd(-(1L<<35))).append(',').append(a.get()).append(';');
                return out.toString();
            }
            public static String longOverflow(){
                AtomicLong a=new AtomicLong(Long.MAX_VALUE);StringBuilder out=new StringBuilder();
                out.append(a.incrementAndGet()).append(',').append(a.decrementAndGet()).append(',');
                a.set(Long.MIN_VALUE);out.append(a.getAndDecrement()).append(',').append(a.get()).append(',');
                out.append(a.addAndGet(Long.MAX_VALUE)).append(',').append(a.getAndAdd(Long.MIN_VALUE)).append(',').append(a.get()).append(',');
                a.set(0xFFFFFFFFL);out.append(a.incrementAndGet()).append(',');
                a.set(-1L);out.append(a.getAndIncrement()).append(',').append(a.get());
                return out.toString();
            }

            @SuppressWarnings("deprecation")
            public static String intCompareAndSet(){
                AtomicInteger a=new AtomicInteger(1);StringBuilder out=new StringBuilder();
                out.append(a.compareAndSet(2,9)).append(a.get()).append(',');
                out.append(a.compareAndSet(1,2)).append(a.get()).append(',');
                out.append(a.weakCompareAndSet(2,3)).append(a.get()).append(',');
                out.append(a.weakCompareAndSet(2,4)).append(a.get()).append(',');
                out.append(a.weakCompareAndSetPlain(3,4)).append(a.get()).append(',');
                out.append(a.weakCompareAndSetVolatile(4,5)).append(a.get()).append(',');
                out.append(a.weakCompareAndSetAcquire(5,6)).append(a.get()).append(',');
                out.append(a.weakCompareAndSetRelease(6,7)).append(a.get()).append(',');
                out.append(a.weakCompareAndSetPlain(0,8)).append(a.weakCompareAndSetVolatile(0,8)).append(a.weakCompareAndSetAcquire(0,8)).append(a.weakCompareAndSetRelease(0,8)).append(a.get()).append(',');
                out.append(a.compareAndSet(7,7)).append(a.get());
                return out.toString();
            }
            @SuppressWarnings("deprecation")
            public static String longCompareAndSet(){
                AtomicLong a=new AtomicLong(1L);StringBuilder out=new StringBuilder();
                out.append(a.compareAndSet(2L,9L)).append(a.get()).append(',');
                out.append(a.compareAndSet(1L,2L)).append(a.get()).append(',');
                out.append(a.weakCompareAndSet(2L,3L)).append(a.get()).append(',');
                out.append(a.weakCompareAndSet(2L,4L)).append(a.get()).append(',');
                out.append(a.weakCompareAndSetPlain(3L,4L)).append(a.get()).append(',');
                out.append(a.weakCompareAndSetVolatile(4L,5L)).append(a.get()).append(',');
                out.append(a.weakCompareAndSetAcquire(5L,6L)).append(a.get()).append(',');
                out.append(a.weakCompareAndSetRelease(6L,7L)).append(a.get()).append(',');
                out.append(a.weakCompareAndSetPlain(0L,8L)).append(a.weakCompareAndSetVolatile(0L,8L)).append(a.weakCompareAndSetAcquire(0L,8L)).append(a.weakCompareAndSetRelease(0L,8L)).append(a.get()).append(',');
                a.set(1L<<33);out.append(a.compareAndSet(0L,1L)).append(a.compareAndSet(1L<<33,-1L)).append(a.get());
                return out.toString();
            }
            public static String intCompareAndExchange(){
                AtomicInteger a=new AtomicInteger(1);StringBuilder out=new StringBuilder();
                out.append(a.compareAndExchange(2,9)).append('/').append(a.get()).append(',');
                out.append(a.compareAndExchange(1,2)).append('/').append(a.get()).append(',');
                out.append(a.compareAndExchangeAcquire(2,3)).append('/').append(a.get()).append(',');
                out.append(a.compareAndExchangeAcquire(0,4)).append('/').append(a.get()).append(',');
                out.append(a.compareAndExchangeRelease(3,-4)).append('/').append(a.get()).append(',');
                out.append(a.compareAndExchangeRelease(3,5)).append('/').append(a.get());
                return out.toString();
            }
            public static String longCompareAndExchange(){
                AtomicLong a=new AtomicLong(1L);StringBuilder out=new StringBuilder();
                out.append(a.compareAndExchange(2L,9L)).append('/').append(a.get()).append(',');
                out.append(a.compareAndExchange(1L,2L)).append('/').append(a.get()).append(',');
                out.append(a.compareAndExchangeAcquire(2L,3L)).append('/').append(a.get()).append(',');
                out.append(a.compareAndExchangeAcquire(0L,4L)).append('/').append(a.get()).append(',');
                out.append(a.compareAndExchangeRelease(3L,-4L)).append('/').append(a.get()).append(',');
                out.append(a.compareAndExchangeRelease(3L,5L)).append('/').append(a.get()).append(',');
                a.set(Long.MIN_VALUE);out.append(a.compareAndExchange(Long.MIN_VALUE,Long.MAX_VALUE)).append('/').append(a.get());
                return out.toString();
            }

            public static String intUpdates(){
                AtomicInteger a=new AtomicInteger(5);StringBuilder log=new StringBuilder();
                IntUnaryOperator twice=x->{log.append("u").append(x).append(';');return x*2;};
                IntBinaryOperator minus=(x,y)->{log.append("b").append(x).append(',').append(y).append(';');return x-y;};
                String r=a.getAndUpdate(twice)+","+a.get()+","+a.updateAndGet(twice)+","+a.get()
                    +","+a.getAndAccumulate(3,minus)+","+a.get()+","+a.accumulateAndGet(100,minus)+","+a.get()
                    +","+a.updateAndGet(x->x)+","+a.getAndAccumulate(Integer.MIN_VALUE,(x,y)->x+y)+","+a.get();
                return r+"|"+log;
            }
            public static String longUpdates(){
                AtomicLong a=new AtomicLong(5L);StringBuilder log=new StringBuilder();
                LongUnaryOperator twice=x->{log.append("u").append(x).append(';');return x*2;};
                LongBinaryOperator minus=(x,y)->{log.append("b").append(x).append(',').append(y).append(';');return x-y;};
                String r=a.getAndUpdate(twice)+","+a.get()+","+a.updateAndGet(twice)+","+a.get()
                    +","+a.getAndAccumulate(3L,minus)+","+a.get()+","+a.accumulateAndGet(100L,minus)+","+a.get()
                    +","+a.updateAndGet(x->x<<40)+","+a.getAndAccumulate(Long.MIN_VALUE,(x,y)->x+y)+","+a.get();
                return r+"|"+log;
            }
            public static String reentrantUpdates(){
                StringBuilder out=new StringBuilder();
                AtomicInteger a=new AtomicInteger(5);int[] calls={0};
                int r1=a.getAndUpdate(x->{calls[0]++;out.append("u").append(x).append(';');if(calls[0]==1)a.set(100);return x+1;});
                out.append(r1).append(',').append(a.get()).append(',').append(calls[0]).append('|');
                calls[0]=0;
                int r2=a.accumulateAndGet(10,(x,y)->{calls[0]++;out.append("b").append(x).append(';');if(calls[0]<3)a.addAndGet(1);return x*y;});
                out.append(r2).append(',').append(a.get()).append(',').append(calls[0]).append('|');
                calls[0]=0;
                int r3=a.updateAndGet(x->{calls[0]++;a.set(x);return x-1;});
                out.append(r3).append(',').append(a.get()).append(',').append(calls[0]).append('|');
                AtomicLong l=new AtomicLong(7L);calls[0]=0;
                long r4=l.getAndAccumulate(2L,(x,y)->{calls[0]++;out.append("L").append(x).append(';');if(calls[0]==1)l.set(-1L);return x+y;});
                out.append(r4).append(',').append(l.get()).append(',').append(calls[0]).append('|');
                calls[0]=0;
                long r5=l.updateAndGet(x->{calls[0]++;if(calls[0]==1)l.incrementAndGet();return x*3;});
                out.append(r5).append(',').append(l.get()).append(',').append(calls[0]);
                return out.toString();
            }
            public static String updateFailures(){
                StringBuilder out=new StringBuilder();
                AtomicInteger a=new AtomicInteger(4);AtomicLong l=new AtomicLong(4L);
                try{a.updateAndGet(x->{throw new IllegalStateException("boom "+x);});out.append("none;");}catch(IllegalStateException e){out.append(name(e)).append(';');}
                try{a.getAndAccumulate(1,(x,y)->{throw new ArithmeticException("acc "+x+" "+y);});out.append("none;");}catch(ArithmeticException e){out.append(name(e)).append(';');}
                try{a.getAndUpdate(x->x/0);out.append("none;");}catch(ArithmeticException e){out.append(name(e)).append(';');}
                IntUnaryOperator nu=null;IntBinaryOperator nb=null;
                try{a.getAndUpdate(nu);out.append("none;");}catch(NullPointerException e){out.append("npe;");}
                try{a.accumulateAndGet(1,nb);out.append("none;");}catch(NullPointerException e){out.append("npe;");}
                out.append(a.get()).append('|');
                try{l.getAndUpdate(x->{throw new IllegalArgumentException("long "+x);});out.append("none;");}catch(IllegalArgumentException e){out.append(name(e)).append(';');}
                try{l.accumulateAndGet(1L,(x,y)->x/(y-1));out.append("none;");}catch(ArithmeticException e){out.append(name(e)).append(';');}
                LongUnaryOperator lu=null;LongBinaryOperator lb=null;
                try{l.updateAndGet(lu);out.append("none;");}catch(NullPointerException e){out.append("npe;");}
                try{l.getAndAccumulate(1L,lb);out.append("none;");}catch(NullPointerException e){out.append("npe;");}
                return out.append(l.get()).toString();
            }

            public static String intConversions(){
                StringBuilder out=new StringBuilder();
                for(int v:new int[]{0,-1,127,128,-129,32768,65535,16777217,Integer.MAX_VALUE,Integer.MIN_VALUE}){
                    AtomicInteger a=new AtomicInteger(v);
                    out.append(a.intValue()).append(' ').append(a.longValue()).append(' ').append(a.floatValue()).append(' ')
                       .append(a.doubleValue()).append(' ').append(a.byteValue()).append(' ').append(a.shortValue()).append(' ').append(sum(a)).append(';');
                }
                return out.toString();
            }
            public static String longConversions(){
                StringBuilder out=new StringBuilder();
                for(long v:new long[]{0L,-1L,255L,4294967295L,4294967296L+5,-4294967297L,(1L<<53)+1,Long.MAX_VALUE,Long.MIN_VALUE}){
                    AtomicLong a=new AtomicLong(v);
                    out.append(a.intValue()).append(' ').append(a.longValue()).append(' ').append(a.floatValue()).append(' ')
                       .append(a.doubleValue()).append(' ').append(a.byteValue()).append(' ').append(a.shortValue()).append(' ').append(sum(a)).append(';');
                }
                return out.toString();
            }
            public static String numberCallPattern(){
                CountingInt a=new CountingInt(300);CountingLong l=new CountingLong(70000L);StringBuilder out=new StringBuilder();
                out.append(a.byteValue()).append(',');out.append(a.shortValue()).append(',');out.append(a.floatValue()).append(',');
                out.append(a.doubleValue()).append(',').append(a.longValue()).append(',').append(a).append(',').append(a.incrementAndGet()).append(',').append(a.getAndUpdate(x->x+1)).append('|').append(a.log).append('|');
                out.append(l.byteValue()).append(',');out.append(l.shortValue()).append(',');out.append(l.floatValue()).append(',');
                out.append(l.doubleValue()).append(',').append(l.intValue()).append(',').append(l).append(',').append(l.addAndGet(2L)).append(',').append(l.accumulateAndGet(3L,Math::max)).append('|').append(l.log);
                return out.toString();
            }
        }
        """;

    @Test void constructsReadsAndWrites() throws Exception {
        matchesJvm(FIXTURE, "intBasics", "longBasics", "subclasses");
    }

    @Test void arithmeticWraps() throws Exception {
        matchesJvm(FIXTURE, "intArithmetic", "intOverflow", "longArithmetic", "longOverflow");
    }

    @Test void compareAndSetFamily() throws Exception {
        matchesJvm(FIXTURE, "intCompareAndSet", "longCompareAndSet", "intCompareAndExchange", "longCompareAndExchange");
    }

    @Test void updatesAndAccumulates() throws Exception {
        matchesJvm(FIXTURE, "intUpdates", "longUpdates", "updateFailures");
    }

    @Test void reentrantFunctionsRetryLikeTheJdk() throws Exception {
        matchesJvm(FIXTURE, "reentrantUpdates");
    }

    @Test void numberConversions() throws Exception {
        matchesJvm(FIXTURE, "intConversions", "longConversions", "numberCallPattern");
    }
}
