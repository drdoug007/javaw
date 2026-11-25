package java.util.stream;
import java.util.*;
import java.util.function.*;
/** Selected sequential primitive-double API; traversal currently uses boxed pipeline storage. */
public interface DoubleStream extends BaseStream<Double,DoubleStream> {
    static DoubleStream empty(){return of(new double[0]);}
    static DoubleStream of(double value){return of(new double[]{value});}
    static DoubleStream of(double... values){return DoublePipeline.array(values);}
    @Override PrimitiveIterator.OfDouble iterator();
    @Override DoubleStream sequential();
    double[] toArray();
    void forEach(DoubleConsumer action);
    void forEachOrdered(DoubleConsumer action);

    double sum();long count();OptionalDouble min();OptionalDouble max();OptionalDouble average();
    double reduce(double identity,DoubleBinaryOperator accumulator);OptionalDouble reduce(DoubleBinaryOperator accumulator);


    DoubleStream filter(DoublePredicate predicate);DoubleStream map(DoubleUnaryOperator mapper);DoubleStream peek(DoubleConsumer action);
    <U> Stream<U> mapToObj(DoubleFunction<? extends U> mapper);Stream<Double> boxed();


    DoubleStream distinct();DoubleStream sorted();DoubleStream limit(long count);DoubleStream skip(long count);
    boolean anyMatch(DoublePredicate predicate);boolean allMatch(DoublePredicate predicate);boolean noneMatch(DoublePredicate predicate);
    OptionalDouble findFirst();OptionalDouble findAny();


    default DoubleStream parallel(){throw new UnsupportedOperationException("Parallel bytecode double streams require worker scheduling");}

}
