package java.util;
public interface List<E> extends Collection<E>{
 default void replaceAll(java.util.function.UnaryOperator<E> operator){Objects.requireNonNull(operator);ListIterator<E> cursor=listIterator();while(cursor.hasNext())cursor.set(operator.apply(cursor.next()));}
 default void sort(Comparator<? super E> comparator){Object[] values=toArray();Arrays.sort(values,(Comparator)comparator);ListIterator<E> cursor=listIterator();for(Object value:values){cursor.next();cursor.set((E)value);}}
 E get(int index);E set(int index,E value);void add(int index,E value);E remove(int index);int indexOf(Object value);int lastIndexOf(Object value);boolean addAll(int index,Collection<? extends E> values);ListIterator<E> listIterator();ListIterator<E> listIterator(int index);List<E> subList(int start,int end);
}
