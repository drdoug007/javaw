package com.example.javaw;
import org.junit.jupiter.api.Test;
class CollectorContractTest {
    @Test void executesStandardMutableReductionAndFactories() throws Exception {
        byte[] wasm=JavacToWasm.compile("App.java", """
            class App {
                public static int combines;
                public static int test() {
                    Supplier<int[]> source=()->new int[1];
                    BiConsumer<int[],Integer> add=(sum,value)->{ sum[0]+=value; };
                    BinaryOperator<int[]> merge=(a,b)->{ combines++; a[0]+=b[0]; return a; };
                    Function<int[],Integer> finish=sum->sum[0]*2;
                    Collector<Integer,int[],Integer> custom=Collector.of(source,add,merge,finish);
                    if(Stream.of(1,2,3).collect(custom)!=12 || combines!=0) return -1;
                    int[] left=custom.supplier().get(); int[] right=custom.supplier().get();
                    custom.accumulator().accept(left,4); custom.accumulator().accept(right,5);
                    if(custom.finisher().apply(custom.combiner().apply(left,right))!=18 || combines!=1) return -2;
                    Collector<Integer,int[],int[]> identity=Collector.of(source,add,merge);
                    if(!identity.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) return -3;
                    if(Stream.of(1,2).collect(identity)[0]!=3) return -4;
                    if(!Stream.of("a","b").collect(Collectors.joining(",","[","]")).equals("[a,b]")) return -5;
                    if(Stream.of(1,2,3).collect(Collectors.<Integer>counting())!=3L) return -6;
                    ToIntFunction<String> length=s->s.length();
                    if(Stream.of("abc","de").collect(Collectors.summingInt(length))!=5) return -7;
                    Function<Integer,String> text=n->"x"+n;
                    Collector<Integer,__JavawJoinBuffer,String> mapped=Collectors.mapping(text,Collectors.joining("-"));
                    if(!Stream.of(1,2).collect(mapped).equals("x1-x2")) return -8;
                    Predicate<Integer> even=n->n%2==0;
                    List<Integer> filtered=Stream.of(1,2,3,4).collect(Collectors.filtering(even,Collectors.<Integer>toList()));
                    if(filtered.size()!=2 || filtered.get(1)!=4) return -9;
                    Function<List<Integer>,Integer> size=list->list.size();
                    if(Stream.of(1,2).collect(Collectors.collectingAndThen(Collectors.<Integer>toList(),size))!=2) return -10;
                    BinaryOperator<Integer> sum=(a,b)->a+b;
                    if(Stream.of(1,2,3).collect(Collectors.reducing(0,sum))!=6) return -11;
                    if(Stream.<Integer>empty().collect(Collectors.reducing(sum)).isPresent()) return -12;
                    return 42;
                }
                public static int immutableFlags() { Collectors.<Integer>toList().characteristics().clear(); return 0; }
            }
            """,false).getWasm();
        OverloadResolutionTest.execute(wasm,"assert.equal(e.test(),42); assert.throws(()=>e.immutableFlags());");
    }
}
