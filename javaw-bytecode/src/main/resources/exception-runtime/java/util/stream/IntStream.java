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


    IntStream distinct();IntStream sorted();IntStream limit(long count);IntStream skip(long count);
    IntStream takeWhile(IntPredicate predicate);IntStream dropWhile(IntPredicate predicate);
    boolean anyMatch(IntPredicate predicate);boolean allMatch(IntPredicate predicate);boolean noneMatch(IntPredicate predicate);
    OptionalInt findFirst();OptionalInt findAny();


    static IntStream generate(IntSupplier supplier){Objects.requireNonNull(supplier);return IntPipeline.from(Stream.generate(()->Integer.valueOf(supplier.getAsInt())));}
    static IntStream iterate(int seed,IntUnaryOperator next){Objects.requireNonNull(next);return IntPipeline.from(Stream.iterate(Integer.valueOf(seed),v->Integer.valueOf(next.applyAsInt(v.intValue()))));}
    static IntStream iterate(int seed,IntPredicate hasNext,IntUnaryOperator next){Objects.requireNonNull(hasNext);Objects.requireNonNull(next);return IntPipeline.from(Stream.iterate(Integer.valueOf(seed),v->hasNext.test(v.intValue()),v->Integer.valueOf(next.applyAsInt(v.intValue()))));}
    static IntStream concat(IntStream left,IntStream right){Objects.requireNonNull(left);Objects.requireNonNull(right);return IntPipeline.from(Stream.concat(left.boxed(),right.boxed()));}
    IntStream flatMap(IntFunction<? extends IntStream> mapper);
    default IntStream parallel(){throw new UnsupportedOperationException("Parallel bytecode integer streams require worker scheduling");}

}
