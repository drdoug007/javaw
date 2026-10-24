package java.util;
public interface Collection<E> extends Iterable<E>{
 int size();boolean isEmpty();boolean contains(Object value);Iterator<E> iterator();Object[] toArray();<T>T[] toArray(T[] array);
 boolean add(E value);boolean remove(Object value);boolean containsAll(Collection<?> values);boolean addAll(Collection<? extends E> values);boolean removeAll(Collection<?> values);boolean retainAll(Collection<?> values);void clear();
}
