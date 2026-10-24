package java.util.stream;
import java.util.*;
import java.util.function.*;
public interface Collector<T,A,R> {
    Supplier<A> supplier();
    BiConsumer<A,T> accumulator();
    BinaryOperator<A> combiner();
    Function<A,R> finisher();
    Set<Characteristics> characteristics();
    enum Characteristics { CONCURRENT, UNORDERED, IDENTITY_FINISH }
    static <T,R> Collector<T,R,R> of(Supplier<R> supplier,BiConsumer<R,T> accumulator,BinaryOperator<R> combiner,Characteristics... characteristics){
        return new Collectors.Implementation<T,R,R>(supplier,accumulator,combiner,Function.identity(),characteristics,true);
    }
    static <T,A,R> Collector<T,A,R> of(Supplier<A> supplier,BiConsumer<A,T> accumulator,BinaryOperator<A> combiner,Function<A,R> finisher,Characteristics... characteristics){
        return new Collectors.Implementation<T,A,R>(supplier,accumulator,combiner,finisher,characteristics,false);
    }
}
