package java.util.stream;
import java.util.*;
import java.util.function.*;
/** Selected sequential primitive-int API; traversal currently uses boxed pipeline storage. */
public interface IntStream extends BaseStream<Integer,IntStream> {
    static IntStream empty(){return of(new int[0]);}
    static IntStream of(int value){return of(new int[]{value});}
    static IntStream of(int... values){return IntPipeline.array(values);}
    static IntStream range(int start,int end){return IntPipeline.range(start,end,false);}
    static IntStream rangeClosed(int start,int end){return IntPipeline.range(start,end,true);}
    @Override PrimitiveIterator.OfInt iterator();
    int[] toArray();
    void forEach(IntConsumer action);
    void forEachOrdered(IntConsumer action);

    int sum();long count();OptionalInt min();OptionalInt max();OptionalDouble average();
    int reduce(int identity,IntBinaryOperator accumulator);OptionalInt reduce(IntBinaryOperator accumulator);
    <R> R collect(Supplier<R> supplier,ObjIntConsumer<R> accumulator,BiConsumer<R,R> combiner);
    IntSummaryStatistics summaryStatistics();


    IntStream filter(IntPredicate predicate);IntStream map(IntUnaryOperator mapper);IntStream peek(IntConsumer action);
    <U> Stream<U> mapToObj(IntFunction<? extends U> mapper);Stream<Integer> boxed();

}
