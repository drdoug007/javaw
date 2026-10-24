package java.util;
public interface List<E> extends Collection<E>{
 E get(int index);E set(int index,E value);void add(int index,E value);E remove(int index);int indexOf(Object value);int lastIndexOf(Object value);boolean addAll(int index,Collection<? extends E> values);ListIterator<E> listIterator();ListIterator<E> listIterator(int index);List<E> subList(int start,int end);
}
