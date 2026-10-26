package dastec.one.javaw;
import org.junit.jupiter.api.Test;
class StreamOrderingTest {
    @Test void executesStableSortAndPrefixOperations() throws Exception {
        byte[] wasm=JavacToWasm.compile("App.java", """
            class Item { public int rank; public int id; public Item(int r,int i) { rank=r; id=i; } }
            class App {
                public static int visited;
                public static int test() {
                    List<Integer> numbers=Stream.of(4,1,3,2).sorted().toList();
                    if(numbers.get(0)!=1 || numbers.get(3)!=4) return -1;
                    List<String> strings=Stream.of("c","a","b").sorted().toList();
                    if(!strings.get(0).equals("a")) return -2;
                    Comparator<Item> order=(a,b)->a.rank-b.rank;
                    List<Item> stable=Stream.of(new Item(2,0),new Item(1,1),new Item(1,2)).sorted(order).toList();
                    if(stable.get(0).id!=1 || stable.get(1).id!=2) return -3;
                    Predicate<Integer> below=n->n<3;
                    Consumer<Integer> visit=n->visited++;
                    long count=Stream.of(1,2,3,1).peek(visit).takeWhile(below).count();
                    if(count!=2 || visited!=3) return -4;
                    List<Integer> tail=Stream.of(1,2,3,1).dropWhile(below).toList();
                    if(tail.size()!=2 || tail.get(1)!=1) return -5;
                    UnaryOperator<Integer> next=n->n+1;
                    if(Stream.iterate(1,next).takeWhile(below).count()!=2) return -6;
                    return 42;
                }
            }
            """,false).getWasm();
        OverloadResolutionTest.execute(wasm,"assert.equal(e.test(),42);");
    }
}
