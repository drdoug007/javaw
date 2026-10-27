package java.util;
import java.util.function.Consumer;
/** Selected sequential reference traversal contract. */
public interface Spliterator<T>{
 int DISTINCT=1,SORTED=4,ORDERED=16,SIZED=64,NONNULL=256,IMMUTABLE=1024,CONCURRENT=4096,SUBSIZED=16384;
 boolean tryAdvance(Consumer<? super T> action);Spliterator<T> trySplit();long estimateSize();int characteristics();
 default void forEachRemaining(Consumer<? super T> action){Objects.requireNonNull(action);while(tryAdvance(action)){} }
 default long getExactSizeIfKnown(){return (characteristics()&SIZED)==0?-1:estimateSize();}
 default boolean hasCharacteristics(int flags){return (characteristics()&flags)==flags;}
 default Comparator<? super T> getComparator(){throw new IllegalStateException();}
}
