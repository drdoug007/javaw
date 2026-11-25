package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** T73: java.util.stream.LongStream, DoubleStream and Stream.mapToLong/mapToDouble. Expected values come from the JVM; see tasks/T73-long-double-streams.md. */
@Tag("agent-pending")
class BytecodeLongDoubleStreamTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.util.ArrayList;
        import java.util.Arrays;
        import java.util.Iterator;
        import java.util.List;
        import java.util.OptionalDouble;
        import java.util.OptionalLong;
        import java.util.PrimitiveIterator;
        import java.util.stream.DoubleStream;
        import java.util.stream.LongStream;
        import java.util.stream.Stream;

        public class Fixture {
            interface Op{Object run() throws Exception;}
            static String t(Op op){
                try{return String.valueOf(op.run());}catch(Exception e){return e.getClass().getSimpleName();}
            }
            static String all(PrimitiveIterator.OfLong it){StringBuilder o=new StringBuilder();while(it.hasNext())o.append(it.nextLong()).append(' ');return o.append(t(()->it.nextLong())).toString();}

            public static String longFactoriesAndTerminals(){
                String a=Arrays.toString(LongStream.of(5L).toArray())+","+Arrays.toString(LongStream.of(3L,-1L,7L).toArray())+","+Arrays.toString(LongStream.empty().toArray());
                a+="|"+Arrays.toString(LongStream.range(-2L,3L).toArray())+","+LongStream.range(5L,5L).count()+","+LongStream.range(6L,2L).count()
                        +","+Arrays.toString(LongStream.rangeClosed(4L,4L).toArray())+","+LongStream.rangeClosed(3L,2L).count();
                a+="|"+Arrays.toString(LongStream.rangeClosed(Long.MAX_VALUE-2L,Long.MAX_VALUE).toArray())+","+Arrays.toString(LongStream.range(Long.MIN_VALUE,Long.MIN_VALUE+2L).toArray())
                        +","+LongStream.range(Long.MAX_VALUE-1L,Long.MAX_VALUE).count()+","+LongStream.rangeClosed(Long.MIN_VALUE,Long.MIN_VALUE).sum();
                a+="|"+LongStream.of(4L,9L,-3L).sum()+","+LongStream.empty().sum()+","+LongStream.of(Long.MAX_VALUE,1L).sum()+","+LongStream.of(4L,9L,-3L).count();
                a+="|"+LongStream.of(4L,9L,-3L).min()+","+LongStream.of(4L,9L,-3L).max()+","+LongStream.empty().min()+","+LongStream.empty().max();
                a+="|"+LongStream.of(1L,2L).average()+","+LongStream.of(1L,2L,4L).average()+","+LongStream.empty().average()+","+LongStream.of(Long.MAX_VALUE,Long.MAX_VALUE).average()
                        +","+LongStream.rangeClosed(1L,100L).average().getAsDouble();
                a+="|"+LongStream.of(1L,2L,3L,4L).reduce(10L,(x,y)->x*y)+","+LongStream.empty().reduce(7L,(x,y)->x+y)+","+LongStream.of(8L,3L,5L).reduce((x,y)->x-y)+","+LongStream.empty().reduce((x,y)->x+y);
                a+="|"+LongStream.of(6L,7L).findFirst()+","+LongStream.empty().findFirst()+","+LongStream.of(6L,7L).findAny()+","+LongStream.empty().findAny().isPresent();
                return a;
            }
            public static String longIntermediates(){
                StringBuilder log=new StringBuilder();
                String a=Arrays.toString(LongStream.rangeClosed(1L,10L).filter(v->v%3L==0L).map(v->v*v).toArray());
                a+="|"+LongStream.of(3L,1L,3L,2L,1L).distinct().boxed().toList()+","+Arrays.toString(LongStream.of(3L,-1L,Long.MIN_VALUE,2L,Long.MAX_VALUE).sorted().toArray());
                a+="|"+Arrays.toString(LongStream.range(0L,10L).skip(3L).limit(4L).toArray())+","+LongStream.of(1L,2L).limit(0L).count()+","+LongStream.of(1L,2L).skip(5L).count();
                a+="|"+LongStream.of(10L,20L).mapToObj(v->"<"+v+">").toList()+","+LongStream.of(4L,5L).boxed().map(v->v.getClass().getSimpleName()+v).toList();
                a+="|"+LongStream.of(1L,2L,3L,4L).peek(v->log.append("p").append(v)).anyMatch(v->v==2L)+" "+log;log.setLength(0);
                a+=","+LongStream.of(1L,2L,3L).peek(v->log.append("q").append(v)).allMatch(v->v<2L)+" "+log;log.setLength(0);
                a+=","+LongStream.of(1L,2L,3L).noneMatch(v->v>5L)+","+LongStream.empty().anyMatch(v->true)+","+LongStream.empty().allMatch(v->false)+","+LongStream.empty().noneMatch(v->true);
                LongStream.of(9L,8L,7L).forEach(v->log.append(v).append(';'));
                LongStream.of(1L,2L).forEachOrdered(v->log.append(v).append('!'));
                a+="|"+log+","+all(LongStream.of(5L,6L).iterator())+","+all(LongStream.empty().iterator());
                PrimitiveIterator.OfLong it=LongStream.of(7L,8L).iterator();
                Iterator<Long> boxed=it;
                a+=","+boxed.next().getClass().getSimpleName()+","+it.nextLong()+","+it.hasNext();
                a+="|"+t(()->LongStream.of(1L).filter(null))+","+t(()->LongStream.of(1L).map(null))+","+t(()->LongStream.of(1L).limit(-1L))+","+t(()->LongStream.of(1L).skip(-1L))+","+t(()->LongStream.of((long[])null));
                return a;
            }
            public static String doubleSums(){
                double[] tenths=new double[10];Arrays.fill(tenths,0.1);
                double naive=0;for(double d:tenths)naive+=d;
                String a=naive+","+DoubleStream.of(tenths).sum()+","+DoubleStream.of(tenths).average()+","+DoubleStream.of(tenths).reduce(0.0,(x,y)->x+y);
                a+="|"+DoubleStream.of(1e16,1.0,1.0,-1e16).sum()+","+DoubleStream.of(1.0,1e100,1.0,-1e100).sum()+","+DoubleStream.of(1e16,1.0,1.0,-1e16).average()+","+DoubleStream.of(0.1,0.2,0.3).sum()+","+(0.1+0.2+0.3);
                a+="|"+DoubleStream.of(1e308,1e308).sum()+","+DoubleStream.of(-1e308,-1e308).sum()+","+DoubleStream.of(1e308,1e308).average()+","+DoubleStream.of(Double.POSITIVE_INFINITY,1.0).sum()
                        +","+DoubleStream.of(Double.POSITIVE_INFINITY,Double.NEGATIVE_INFINITY).sum()+","+DoubleStream.of(1.0,Double.NaN).sum()+","+DoubleStream.of(Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY).sum();
                a+="|"+DoubleStream.empty().sum()+","+DoubleStream.empty().average()+","+DoubleStream.of(-0.0).sum()+","+DoubleStream.of(2.5).average()+","+DoubleStream.of(1.0,2.0).average().getAsDouble()+","+DoubleStream.of(-0.0,-0.0).sum();
                return a;
            }
            public static String doubleOperations(){
                String a=DoubleStream.of(0.0,-0.0).min()+","+DoubleStream.of(-0.0,0.0).max()+","+DoubleStream.of(-0.0,0.0).min()+","+DoubleStream.of(3.0,Double.NaN,-1.0).min()+","+DoubleStream.of(3.0,Double.NaN).max()
                        +","+DoubleStream.of(2.5,-7.25,1.0).min()+","+DoubleStream.of(2.5,-7.25,1.0).max()+","+DoubleStream.empty().max();
                a+="|"+Arrays.toString(DoubleStream.of(1.5,Double.NaN,-0.0,0.0,Double.NEGATIVE_INFINITY,-2.0,Double.NaN).sorted().toArray())
                        +","+Arrays.toString(DoubleStream.of(0.0,-0.0,0.0,Double.NaN,Double.NaN,1.0).distinct().toArray());
                a+="|"+Arrays.toString(DoubleStream.of(1.0,2.0,3.0,4.0).filter(v->v!=2.0).map(v->v/4).toArray())+","+DoubleStream.of(0.5,1.5).mapToObj(v->"d"+v).toList()+","+DoubleStream.of(2.0).boxed().map(v->v.getClass().getSimpleName()).toList();
                a+="|"+DoubleStream.of(1.0,2.0,3.0).skip(1L).limit(1L).findFirst()+","+DoubleStream.empty().findFirst()+","+DoubleStream.of(4.0).findAny()+","+DoubleStream.of(1.0,2.0).count();
                a+="|"+DoubleStream.of(1.0,2.0).anyMatch(v->v>1.5)+","+DoubleStream.of(1.0,2.0).allMatch(v->v>1.5)+","+DoubleStream.of(1.0,2.0).noneMatch(v->v>5.0)+","+DoubleStream.of(2.0,3.0).reduce(1.0,(x,y)->x*y)+","+DoubleStream.of(2.0,3.0).reduce((x,y)->x-y)+","+DoubleStream.empty().reduce((x,y)->x);
                StringBuilder log=new StringBuilder();
                DoubleStream.of(0.25,-1.0).peek(v->log.append('p')).forEachOrdered(v->log.append(v).append(' '));
                DoubleStream.of(3.0).forEach(v->log.append(v));
                PrimitiveIterator.OfDouble it=DoubleStream.of(1.5,2.5).iterator();
                log.append('|').append(it.nextDouble()).append(',').append(it.next().getClass().getSimpleName()).append(',').append(it.hasNext()).append(',').append(t(()->it.nextDouble()));
                return a+"|"+log+"|"+t(()->DoubleStream.of(1.0).filter(null))+","+t(()->DoubleStream.of((double[])null));
            }
            static final class Ping { final String name; final long nanos; final double km; Ping(String name,long nanos,double km){this.name=name;this.nanos=nanos;this.km=km;} }
            public static String referenceMapping(){
                List<Ping> pings=new ArrayList<>();
                pings.add(new Ping("a",120L,0.1));pings.add(new Ping("b",80L,0.2));pings.add(new Ping("c",101L,0.3));
                String a=pings.stream().mapToLong(p->p.nanos).average()+","+(long)pings.stream().mapToLong(p->p.nanos).average().orElse(0)+","+new ArrayList<Ping>().stream().mapToLong(p->p.nanos).average();
                a+="|"+pings.stream().mapToDouble(p->p.km).average()+","+pings.stream().mapToDouble(p->p.km).sum()+","+Stream.of("x","yy","zzz").mapToLong(String::length).sum()+","+Stream.of("x","yy").mapToDouble(String::length).max();
                a+="|"+LongStream.rangeClosed(10L,20L).filter(v->v%7L==0L).findFirst()+","+LongStream.rangeClosed(10L,13L).filter(v->v%7L==0L).findFirst().isPresent();
                a+="|"+Stream.of(3,1,2).mapToLong(Integer::longValue).sorted().boxed().toList()+","+Stream.of("a","b").mapToDouble(s->s.charAt(0)).boxed().toList();
                a+="|"+t(()->Stream.of(1).mapToLong(null))+","+t(()->Stream.of(1).mapToDouble(null));
                return a;
            }
            public static String closeAndReuse(){
                StringBuilder log=new StringBuilder();
                try(LongStream s=LongStream.of(1L,2L,3L).onClose(()->log.append("c1 ")).onClose(()->log.append("c2 "))){
                    s.forEachOrdered(v->log.append(v).append(' '));
                }
                try(DoubleStream s=DoubleStream.of(0.5,1.5).onClose(()->log.append("d1 "))){
                    s.forEachOrdered(v->log.append(v).append(' '));
                }
                LongStream l=LongStream.of(4L).onClose(()->log.append("once "));
                l.close();l.close();
                String a=log+"|"+t(()->l.sum())+","+t(()->{l.forEachOrdered(v->{});return "x";});
                DoubleStream d=DoubleStream.of(2.0);
                d.close();
                a+=","+t(()->d.average())+","+t(()->{d.close();return "closed";});
                LongStream used=LongStream.of(1L,2L);
                used.count();
                a+=","+t(()->used.findFirst())+","+t(()->used.onClose(()->{}));
                DoubleStream used2=DoubleStream.of(1.0);
                used2.sum();
                a+=","+t(()->used2.average())+","+LongStream.of(1L).isParallel()+","+DoubleStream.of(1.0).sequential().unordered().sum();
                LongStream mapped=LongStream.of(1L,2L).onClose(()->log.append("up "));
                LongStream next=mapped.map(v->v+1L);
                next.close();
                return a+","+t(()->mapped.sum())+"|"+log;
            }
        }
        """;

    @Test void longFactoriesAndTerminals() throws Exception {
        matchesJvm(FIXTURE, "longFactoriesAndTerminals");
    }

    @Test void longIntermediates() throws Exception {
        matchesJvm(FIXTURE, "longIntermediates");
    }

    @Test void compensatedDoubleSums() throws Exception {
        matchesJvm(FIXTURE, "doubleSums");
    }

    @Test void doubleOperations() throws Exception {
        matchesJvm(FIXTURE, "doubleOperations");
    }

    @Test void streamMapToLongAndDoubleAndClose() throws Exception {
        matchesJvm(FIXTURE, "referenceMapping", "closeAndReuse");
    }

    @Test void leavesOutUnlistedPrimitiveStreamMembers() {
        rejectsUnusedCall("java/util/stream/LongStream", "summaryStatistics", "()Ljava/util/LongSummaryStatistics;", INVOKEINTERFACE, true);
        rejectsUnusedCall("java/util/stream/LongStream", "mapToDouble", "(Ljava/util/function/LongToDoubleFunction;)Ljava/util/stream/DoubleStream;", INVOKEINTERFACE, true);
        rejectsUnusedCall("java/util/stream/DoubleStream", "iterate", "(DLjava/util/function/DoubleUnaryOperator;)Ljava/util/stream/DoubleStream;", INVOKESTATIC, true);
        rejectsUnusedCall("java/util/stream/IntStream", "asLongStream", "()Ljava/util/stream/LongStream;", INVOKEINTERFACE, true);
    }
}
