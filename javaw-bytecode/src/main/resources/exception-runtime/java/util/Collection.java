package java.util;
public interface Collection<E> extends Iterable<E>{
 default Spliterator<E> spliterator(){return Spliterators.spliterator(this,0);}

 default boolean removeIf(java.util.function.Predicate<? super E> predicate){Objects.requireNonNull(predicate);boolean changed=false;Iterator<E> cursor=iterator();while(cursor.hasNext())if(predicate.test(cursor.next())){cursor.remove();changed=true;}return changed;}
 default java.util.stream.Stream<E> stream(){return java.util.stream.ReferenceStream.collection(this);}
 default <T>T[] toArray(java.util.function.IntFunction<T[]> generator){return toArray(generator.apply(0));}
 int size();boolean isEmpty();boolean contains(Object value);Iterator<E> iterator();Object[] toArray();<T>T[] toArray(T[] array);
 boolean add(E value);boolean remove(Object value);boolean containsAll(Collection<?> values);boolean addAll(Collection<? extends E> values);boolean removeAll(Collection<?> values);boolean retainAll(Collection<?> values);void clear();
}
