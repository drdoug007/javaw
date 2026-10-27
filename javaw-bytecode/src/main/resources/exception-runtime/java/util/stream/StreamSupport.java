package java.util.stream;
import java.util.*;import java.util.function.*;
/** Sequential reference bridge. Parallel hosts require a separate contract. */
public final class StreamSupport {
 private StreamSupport(){}
 public static <T>Stream<T> stream(Spliterator<T> source,boolean parallel){Objects.requireNonNull(source);if((source.characteristics()&Spliterator.SORTED)!=0)source.getComparator();if(parallel)throw new UnsupportedOperationException("parallel StreamSupport");return create(()->source);}
 public static <T>Stream<T> stream(Supplier<? extends Spliterator<T>> source,int flags,boolean parallel){Objects.requireNonNull(source);if(parallel)throw new UnsupportedOperationException("parallel StreamSupport");return create(source);}
 static <T>Stream<T> create(Supplier<? extends Spliterator<T>> source){return ReferenceStream.fromIterator(()->new DeferredIterator<T>(source));}
 static final class DeferredIterator<T> implements Iterator<T>{
 final Supplier<? extends Spliterator<T>> source;Iterator<T> iterator;
 DeferredIterator(Supplier<? extends Spliterator<T>> source){this.source=source;}
 Iterator<T> get(){if(iterator==null)iterator=Spliterators.iterator(Objects.requireNonNull(source.get()));return iterator;}
 public boolean hasNext(){return get().hasNext();}public T next(){return get().next();}
 }
}
