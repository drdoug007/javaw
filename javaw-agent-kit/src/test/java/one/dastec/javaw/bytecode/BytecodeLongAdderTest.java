package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** T43: java.util.concurrent.atomic.LongAdder and LongAccumulator (single-threaded). See tasks/T43-long-adder.md. */
@Tag("agent-pending")
class BytecodeLongAdderTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.util.concurrent.atomic.LongAccumulator;
        import java.util.concurrent.atomic.LongAdder;
        import java.util.function.LongBinaryOperator;
        class LoggingAdder extends LongAdder {
            final StringBuilder log=new StringBuilder();
            public void add(long x){log.append("add(").append(x).append(");");super.add(x);}
            public long sum(){log.append("sum;");return super.sum();}
            public int intValue(){log.append("i;");return super.intValue();}
            public long longValue(){log.append("l;");return super.longValue();}
            public float floatValue(){log.append("f;");return super.floatValue();}
            public double doubleValue(){log.append("d;");return super.doubleValue();}
            public void reset(){log.append("reset;");super.reset();}
        }
        class LoggingAccumulator extends LongAccumulator {
            final StringBuilder log;
            LoggingAccumulator(StringBuilder log,LongBinaryOperator f,long identity){super(f,identity);this.log=log;}
            public long get(){log.append("get;");return super.get();}
            public int intValue(){log.append("i;");return super.intValue();}
            public long longValue(){log.append("l;");return super.longValue();}
            public float floatValue(){log.append("f;");return super.floatValue();}
            public double doubleValue(){log.append("d;");return super.doubleValue();}
            public void reset(){log.append("reset;");super.reset();}
        }
        class Recorder implements LongBinaryOperator {
            final StringBuilder log;
            Recorder(StringBuilder log){this.log=log;}
            public long applyAsLong(long a,long b){log.append('f').append(a).append(',').append(b).append(';');return a+b;}
        }
        public class Fixture {
            static String name(Throwable t){return t.getClass().getName();}

            public static String adderBasics(){
                LongAdder a=new LongAdder();StringBuilder out=new StringBuilder();
                out.append(a.sum()).append(',').append(a).append(',');
                a.add(5L);a.increment();a.increment();a.decrement();out.append(a.sum()).append(',');
                a.add(-10L);out.append(a).append(',');
                a.add(1L<<40);out.append(a.sum()).append(',');
                a.add(0L);out.append(a.toString()).append(',');
                out.append(a.equals(a)).append(',').append(a.equals(new LongAdder())).append(',');
                out.append((Object)a instanceof Number).append(',').append((Object)a instanceof java.io.Serializable);
                return out.toString();
            }
            public static String adderOverflow(){
                LongAdder a=new LongAdder();StringBuilder out=new StringBuilder();
                a.add(Long.MAX_VALUE);out.append(a).append(',');
                a.increment();out.append(a.sum()).append(',');
                a.decrement();out.append(a.sum()).append(',');
                a.add(Long.MIN_VALUE);out.append(a.sum()).append(',');
                a.add(Long.MIN_VALUE);out.append(a.sum());
                return out.toString();
            }
            public static String adderReset(){
                LongAdder a=new LongAdder();StringBuilder out=new StringBuilder();
                out.append(a.sumThenReset()).append(',');
                a.add(42L);out.append(a.sumThenReset()).append(',').append(a.sum()).append(',');
                a.add(-3L);a.reset();out.append(a.sum()).append(',');
                a.reset();out.append(a).append(',');
                a.add(7L);a.increment();out.append(a.sumThenReset()).append(',').append(a.sumThenReset()).append(',').append(a);
                return out.toString();
            }
            public static String adderConversions(){
                LongAdder a=new LongAdder();StringBuilder out=new StringBuilder();
                a.add(0xFFFFFFFFL);
                out.append(a.intValue()).append(',').append(a.longValue()).append(',').append(a.floatValue()).append(',').append(a.doubleValue()).append(',');
                out.append(a.byteValue()).append(',').append(a.shortValue()).append(';');
                a.add(6L);
                out.append(a.intValue()).append(',').append(a.byteValue()).append(',').append(a.shortValue()).append(';');
                a.reset();a.add(Long.MAX_VALUE);
                out.append(a.intValue()).append(',').append(a.floatValue()).append(',').append(a.doubleValue()).append(';');
                a.reset();a.add(16777217L);
                out.append(a.floatValue()).append(',').append(a.doubleValue()).append(',').append(a.intValue());
                return out.toString();
            }
            public static String adderCallPattern(){
                LoggingAdder a=new LoggingAdder();StringBuilder out=new StringBuilder();
                a.increment();a.decrement();a.add(4L);out.append(a.log).append('|');a.log.setLength(0);
                a.toString();out.append(a.log).append('|');a.log.setLength(0);
                a.longValue();out.append(a.log).append('|');a.log.setLength(0);
                a.intValue();out.append(a.log).append('|');a.log.setLength(0);
                a.floatValue();a.doubleValue();out.append(a.log).append('|');a.log.setLength(0);
                a.byteValue();a.shortValue();out.append(a.log).append('|');a.log.setLength(0);
                out.append(a.sumThenReset()).append(a.log).append('|');a.log.setLength(0);
                a.add(2L);a.reset();out.append(a.log).append(a.sum());
                return out.toString();
            }

            public static String accumulatorBasics(){
                StringBuilder out=new StringBuilder();
                LongAccumulator max=new LongAccumulator(Long::max,Long.MIN_VALUE);
                out.append(max.get()).append(',').append(max).append(',');
                max.accumulate(3L);max.accumulate(-8L);max.accumulate(11L);max.accumulate(2L);
                out.append(max.get()).append(',').append(max.getThenReset()).append(',').append(max.get()).append(',');
                max.accumulate(5L);max.reset();out.append(max).append(';');
                LongAccumulator prod=new LongAccumulator((x,y)->x*y,1L);
                prod.accumulate(6L);prod.accumulate(7L);out.append(prod.get()).append(',');
                prod.accumulate(0L);out.append(prod.longValue()).append(',');
                prod.reset();prod.accumulate(1L<<40);prod.accumulate(1L<<30);out.append(prod).append(',').append(prod.intValue()).append(';');
                LongAccumulator sub=new LongAccumulator((x,y)->x-y,100L);
                sub.accumulate(30L);out.append(sub.get()).append(',');
                sub.accumulate(-5L);out.append(sub.getThenReset()).append(',').append(sub.get()).append(';');
                out.append(sub.equals(sub)).append(',').append(sub.equals(new LongAccumulator((x,y)->x-y,100L))).append(',');
                out.append((Object)sub instanceof Number).append(',').append((Object)sub instanceof java.io.Serializable);
                return out.toString();
            }
            public static String accumulatorConversions(){
                StringBuilder out=new StringBuilder();
                LongAccumulator a=new LongAccumulator(Long::sum,0xFFFFFFFFL);
                out.append(a.intValue()).append(',').append(a.longValue()).append(',').append(a.floatValue()).append(',').append(a.doubleValue()).append(',');
                out.append(a.byteValue()).append(',').append(a.shortValue()).append(';');
                a.accumulate(Long.MAX_VALUE);
                out.append(a.get()).append(',').append(a.intValue()).append(',').append(a.floatValue()).append(',').append(a.doubleValue()).append(';');
                a.reset();out.append(a.get());
                return out.toString();
            }
            public static String accumulatorFunctionCalls(){
                StringBuilder log=new StringBuilder();StringBuilder out=new StringBuilder();
                LongAccumulator a=new LongAccumulator(new Recorder(log),10L);
                out.append(log).append('|');
                a.accumulate(5L);a.accumulate(-2L);out.append(log).append('|');log.setLength(0);
                out.append(a.get()).append(a).append(a.longValue()).append(log).append('|');
                out.append(a.getThenReset()).append(log).append('|');
                a.accumulate(0L);out.append(log).append(a.get()).append('|');log.setLength(0);
                a.reset();out.append(log).append(a.get());
                return out.toString();
            }
            public static String accumulatorFailures(){
                StringBuilder out=new StringBuilder();
                LongAccumulator nul=new LongAccumulator(null,7L);
                out.append(nul.get()).append(',').append(nul).append(',').append(nul.getThenReset()).append(',');
                nul.reset();out.append(nul.get()).append(',');
                try{nul.accumulate(1L);out.append("no;");}catch(NullPointerException e){out.append(name(e)).append(';');}
                out.append(nul.get()).append(';');
                int[] calls={0};
                LongAccumulator bad=new LongAccumulator((x,y)->{calls[0]++;if(y<0)throw new IllegalArgumentException("neg "+y);return x+y;},1L);
                bad.accumulate(4L);
                try{bad.accumulate(-3L);out.append("no;");}catch(IllegalArgumentException e){out.append(name(e)).append(':').append(e.getMessage()).append(';');}
                out.append(bad.get()).append(',').append(calls[0]);
                return out.toString();
            }
            public static String accumulatorCallPattern(){
                StringBuilder log=new StringBuilder();StringBuilder out=new StringBuilder();
                LoggingAccumulator a=new LoggingAccumulator(log,Long::sum,3L);
                a.accumulate(4L);out.append(log).append('|');
                a.toString();out.append(log).append('|');log.setLength(0);
                a.longValue();a.intValue();out.append(log).append('|');log.setLength(0);
                a.floatValue();a.doubleValue();out.append(log).append('|');log.setLength(0);
                a.byteValue();a.shortValue();out.append(log).append('|');log.setLength(0);
                out.append(a.getThenReset()).append(log).append('|');
                a.reset();out.append(log);
                return out.toString();
            }
        }
        """;

    @Test void adderArithmetic() throws Exception {
        matchesJvm(FIXTURE, "adderBasics", "adderOverflow", "adderReset");
    }

    @Test void adderNumberMethods() throws Exception {
        matchesJvm(FIXTURE, "adderConversions", "adderCallPattern");
    }

    @Test void accumulatorArithmetic() throws Exception {
        matchesJvm(FIXTURE, "accumulatorBasics", "accumulatorConversions");
    }

    @Test void accumulatorFunctions() throws Exception {
        matchesJvm(FIXTURE, "accumulatorFunctionCalls", "accumulatorFailures", "accumulatorCallPattern");
    }

    @Test void leavesOutSerialization() {
        rejectsUnusedCall("java/util/concurrent/atomic/LongAdder", "writeReplace", "()Ljava/lang/Object;", INVOKEVIRTUAL, false);
    }
}
