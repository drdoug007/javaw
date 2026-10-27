package java.util.stream;
import java.util.*;import java.util.function.*;
/** Sequential reference bridge. Parallel hosts require a separate contract. */
public final class StreamSupport {
 private StreamSupport(){}
 public static <T>Stream<T> stream(Spliterator<T> source,boolean parallel){Objects.requireNonNull(source);if((source.characteristics()&Spliterator.SORTED)!=0)source.getComparator();if(parallel)throw new UnsupportedOperationException("parallel StreamSupport");return create(()->source,false);}
 public static <T>Stream<T> stream(Supplier<? extends Spliterator<T>> source,int flags,boolean parallel){Objects.requireNonNull(source);if(parallel)throw new UnsupportedOperationException("parallel StreamSupport");return create(source,true);}
 static <T>Stream<T> create(Supplier<? extends Spliterator<T>> source,boolean deferred){ReferenceStream<T> stream=(ReferenceStream<T>)ReferenceStream.fromIterator(()->new DeferredIterator<T>(source));return stream.withSpliteratorSource(()->deferred?new LazySpliterator<T>(source):source.get());}
 static final class LazySpliterator<T> implements Spliterator<T>{
 final Supplier<? extends Spliterator<T>> supplier;Spliterator<T> source;LazySpliterator(Supplier<? extends Spliterator<T>> supplier){this.supplier=supplier;}
 Spliterator<T> get(){if(source==null)source=Objects.requireNonNull(supplier.get());return source;}
 public boolean tryAdvance(Consumer<? super T> action){return get().tryAdvance(action);}public void forEachRemaining(Consumer<? super T> action){get().forEachRemaining(action);}public Spliterator<T> trySplit(){return get().trySplit();}public long estimateSize(){return get().estimateSize();}public int characteristics(){return get().characteristics();}public Comparator<? super T> getComparator(){return get().getComparator();}
 }
 static final class DeferredIterator<T> implements Iterator<T>{
 final Supplier<? extends Spliterator<T>> source;Iterator<T> iterator;
 DeferredIterator(Supplier<? extends Spliterator<T>> source){this.source=source;}
 Iterator<T> get(){if(iterator==null)iterator=Spliterators.iterator(Objects.requireNonNull(source.get()));return iterator;}
 public boolean hasNext(){return get().hasNext();}public T next(){return get().next();}
 }
}
