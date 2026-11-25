package java.util.stream;
import java.util.*;
import java.util.function.*;
/** Selected sequential primitive-long API; traversal currently uses boxed pipeline storage. */
public interface LongStream extends BaseStream<Long,LongStream> {
    static LongStream empty(){return of(new long[0]);}
    static LongStream of(long value){return of(new long[]{value});}
    static LongStream of(long... values){return LongPipeline.array(values);}
    static LongStream range(long startInclusive,long endExclusive){return LongPipeline.range(startInclusive,endExclusive,false);}
    static LongStream rangeClosed(long startInclusive,long endInclusive){return LongPipeline.range(startInclusive,endInclusive,true);}
    @Override PrimitiveIterator.OfLong iterator();
    @Override LongStream sequential();
    long[] toArray();
    void forEach(LongConsumer action);
    void forEachOrdered(LongConsumer action);

    long sum();long count();OptionalLong min();OptionalLong max();OptionalDouble average();
    long reduce(long identity,LongBinaryOperator accumulator);OptionalLong reduce(LongBinaryOperator accumulator);


    LongStream filter(LongPredicate predicate);LongStream map(LongUnaryOperator mapper);LongStream peek(LongConsumer action);
    <U> Stream<U> mapToObj(LongFunction<? extends U> mapper);Stream<Long> boxed();


    LongStream distinct();LongStream sorted();LongStream limit(long count);LongStream skip(long count);
    boolean anyMatch(LongPredicate predicate);boolean allMatch(LongPredicate predicate);boolean noneMatch(LongPredicate predicate);
    OptionalLong findFirst();OptionalLong findAny();


    default LongStream parallel(){throw new UnsupportedOperationException("Parallel bytecode long streams require worker scheduling");}

}
