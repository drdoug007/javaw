package java.util.stream;
import java.util.*;
import java.util.function.*;
public interface Stream<T> extends BaseStream<T,Stream<T>> {
    static <T> Stream<T> empty(){return ReferenceStream.array((T[])new Object[0],0,0);}
    static <T> Stream<T> of(T value){return ReferenceStream.array((T[])new Object[]{value},0,1);}
    @SafeVarargs static <T> Stream<T> of(T... values){return ReferenceStream.array(values,0,values.length);}
    static <T> Stream<T> ofNullable(T value){return value==null?Stream.<T>empty():of(value);}
    static <T> Stream<T> generate(Supplier<? extends T> supplier){return ReferenceStream.generate(supplier);}
    static <T> Stream<T> iterate(T seed,UnaryOperator<T> next){return ReferenceStream.iterate(seed,null,next);}
    static <T> Stream<T> iterate(T seed,Predicate<? super T> hasNext,UnaryOperator<T> next){Objects.requireNonNull(hasNext);return ReferenceStream.iterate(seed,hasNext,next);}
    static <T> Stream<T> concat(Stream<? extends T> left,Stream<? extends T> right){return ReferenceStream.concat(left,right);}
    Stream<T> filter(Predicate<? super T> predicate);
    <R> Stream<R> map(Function<? super T,? extends R> mapper);
    <R> Stream<R> flatMap(Function<? super T,? extends Stream<? extends R>> mapper);
    Stream<T> peek(Consumer<? super T> action);
    Stream<T> distinct();
    Stream<T> sorted();
    Stream<T> sorted(Comparator<? super T> comparator);
    Stream<T> limit(long count);
    Stream<T> skip(long count);
    Stream<T> takeWhile(Predicate<? super T> predicate);
    Stream<T> dropWhile(Predicate<? super T> predicate);
    void forEach(Consumer<? super T> action);
    void forEachOrdered(Consumer<? super T> action);
    Object[] toArray();
    <A> A[] toArray(IntFunction<A[]> generator);
    List<T> toList();
    long count();
    boolean anyMatch(Predicate<? super T> predicate);
    boolean allMatch(Predicate<? super T> predicate);
    boolean noneMatch(Predicate<? super T> predicate);
    Optional<T> findFirst();
    Optional<T> findAny();
    Optional<T> min(Comparator<? super T> comparator);
    Optional<T> max(Comparator<? super T> comparator);
    T reduce(T identity,BinaryOperator<T> accumulator);
    Optional<T> reduce(BinaryOperator<T> accumulator);
    <U> U reduce(U identity,BiFunction<U,? super T,U> accumulator,BinaryOperator<U> combiner);
    <R> R collect(Supplier<R> supplier,BiConsumer<R,? super T> accumulator,BiConsumer<R,R> combiner);
    <R,A> R collect(Collector<? super T,A,R> collector);
    IntStream mapToInt(ToIntFunction<? super T> mapper);
}
