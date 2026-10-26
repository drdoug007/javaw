package dastec.one.javaw;
import org.junit.jupiter.api.Test;
class StreamSupportTest {
    @Test void executesLazySequentialPipelinesAndTerminals() throws Exception {
        var result=JavacToWasm.compile("App.java", """
            class App {
                public static int visited;
                public static int total;
                public static int test() {
                    Consumer<Integer> visit = n -> visited++;
                    Predicate<Integer> even = n -> n%2==0;
                    Function<Integer,Integer> twice = n -> n*2;
                    Stream<Integer> pipeline=Stream.of(1,2,3,4,5,6).peek(visit).filter(even).map(twice).limit(2);
                    if(visited!=0) return -1;
                    List<Integer> values=pipeline.toList();
                    if(values.size()!=2 || values.get(0)!=4 || values.get(1)!=8 || visited!=4) return -2;
                    visited=0;
                    if(!Stream.of(1,2,3).peek(visit).anyMatch(even) || visited!=2) return -3;
                    visited=0;
                    if(Stream.of(1,2,3).peek(visit).limit(0).count()!=0 || visited!=0) return -4;
                    if(Stream.of(1,1,2,3,3).distinct().skip(1).count()!=2) return -5;
                    if(Stream.<Integer>empty().findFirst().isPresent()) return -6;
                    if(Stream.of(7,8).findFirst().get()!=7) return -7;
                    BinaryOperator<Integer> add=(a,b)->a+b;
                    if(Stream.of(1,2,3).reduce(0,add)!=6 || Stream.of(1,2,3).reduce(add).get()!=6) return -8;
                    if(!Stream.of(2,4).allMatch(even) || !Stream.of(1,3).noneMatch(even)) return -9;
                    Consumer<Integer> accumulate = n -> { total+=n; };
                    Stream.of(2,3).forEachOrdered(accumulate); if(total!=5) return -10;
                    Comparator<Integer> order=(a,b)->a-b;
                    if(Stream.of(3,1,2).min(order).get()!=1 || Stream.of(3,1,2).max(order).get()!=3) return -11;
                    ArrayList<Integer> list=new ArrayList<Integer>(); list.add(10); list.add(20);
                    if(list.stream().count()!=2) return -12;
                    Function<Integer,Stream<Integer>> duplicate=n->Stream.of(n,n);
                    if(Stream.of(1,2,3).flatMap(duplicate).limit(3).count()!=3) return -13;
                    Object[] array=Stream.of("a","b").toArray(); if(array.length!=2) return -14;
                    UnaryOperator<Integer> next=n->n+1;
                    if(Stream.iterate(1,next).limit(5).reduce(0,add)!=15) return -15;
                    Supplier<Integer> one=()->1;
                    if(Stream.generate(one).limit(4).count()!=4) return -16;
                    List<Integer> collected=Stream.of(1,2).collect(Collectors.<Integer>toList());
                    collected.add(3); if(collected.size()!=3) return -17;
                    Set<Integer> unique=Stream.of(1,1,2).collect(Collectors.<Integer>toSet());
                    if(unique.size()!=2) return -18;
                    Stream<Integer> late=list.stream(); list.add(30);
                    if(late.count()!=3) return -19;
                    return 42;
                }
                public static int reused() { Stream<Integer> s=Stream.of(1); s.count(); return (int)s.count(); }
                public static int linked() { Stream<Integer> s=Stream.of(1); s.limit(1); return (int)s.count(); }
                public static int closed() { Stream<Integer> s=Stream.of(1); s.close(); return (int)s.count(); }
                public static int readOnly() { List<Integer> list=Stream.of(1).toList(); list.add(2); return 0; }
                public static int nullFirst() { return Stream.<Integer>of((Integer)null).findFirst().get(); }
                public static int negativeLimit() { Stream.of(1).limit(-1); return 0; }
                public static int parallel() { Stream.of(1).parallel(); return 0; }
            }
            """,false);
        byte[] wasm=result.getWasm();
        OverloadResolutionTest.execute(wasm,"""
            assert.equal(e.test(),42);
            for(const name of ['reused','linked','closed','readOnly','nullFirst','negativeLimit','parallel']) assert.throws(()=>e[name](),name);
            """);
    }
}
