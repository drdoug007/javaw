package dastec.one.javaw;
import org.junit.jupiter.api.Test;
class PrimitiveStreamSupportTest {
    @Test void executesPrimitiveArithmeticAndLazyPipelines() throws Exception {
        byte[] wasm=JavacToWasm.compile("App.java", """
            class App {
                public static int visits;
                public static int test() {
                    IntUnaryOperator twice=n->n*2;
                    IntPredicate even=n->n%2==0;
                    IntConsumer visit=n->visits++;
                    if(IntStream.range(1,8).peek(visit).filter(even).map(twice).limit(2).sum()!=12 || visits!=4) return -1;
                    int[] sorted=IntStream.of(3,1,2).sorted().toArray(); if(sorted[0]!=1 || sorted[2]!=3) return -2;
                    if(IntStream.rangeClosed(2147483647,2147483647).count()!=1) return -3;
                    if(IntStream.of(1,2,3).average().getAsDouble()!=2.0) return -4;
                    if(!IntStream.empty().min().isEmpty() || IntStream.of(5,2,8).min().getAsInt()!=2) return -5;
                    IntBinaryOperator add=(a,b)->a+b;
                    if(IntStream.of(1,2,3).reduce(10,add)!=16 || IntStream.of(1,2).reduce(add).getAsInt()!=3) return -6;
                    IntFunction<String> text=n->"x"+n;
                    if(IntStream.of(1,2).mapToObj(text).toList().get(0).length()!=2) return -7;
                    ToIntFunction<String> length=s->s.length();
                    if(Stream.of("abc","de").mapToInt(length).sum()!=5) return -8;
                    IntFunction<IntStream> pair=n->IntStream.of(n,n);
                    if(IntStream.of(1,2).flatMap(pair).sum()!=6) return -9;
                    if(IntStream.rangeClosed(-1,1).asLongStream().sum()!=0L) return -10;
                    IntToDoubleFunction halves=n->n/2.0;
                    if(IntStream.of(1,2).mapToDouble(halves).sum()!=1.5) return -11;
                    double nan=DoubleStream.of(0.0/0.0,1.0).min().getAsDouble();
                    if(nan==nan) return -12;
                    if(Double.doubleToLongBits(DoubleStream.of(0.0,-0.0).min().getAsDouble())>=0L) return -13;
                    return 42;
                }
                public static long wide() { LongUnaryOperator next=n->n+1L; return LongStream.iterate(9007199254740993L,next).limit(2).sum(); }
                public static double decimals() { DoubleUnaryOperator twice=n->n*2.0; return DoubleStream.of(1.25,2.5).map(twice).sum(); }
            }
            """,false).getWasm();
        OverloadResolutionTest.execute(wasm,"assert.equal(e.test(),42); assert.equal(e.wide(),18014398509481987n); assert.equal(e.decimals(),7.5);");
    }
}
