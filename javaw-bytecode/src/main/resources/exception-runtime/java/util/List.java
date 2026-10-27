package java.util;
public interface List<E> extends Collection<E>{
 default Spliterator<E> spliterator(){return Spliterators.spliterator(this,Spliterator.ORDERED);}

 static <E>List<E> of(){return ImmutableCollections.<E>list((E[])new Object[]{});}
 static <E>List<E> of(E e0){return ImmutableCollections.<E>list((E[])new Object[]{e0});}
 static <E>List<E> of(E e0,E e1){return ImmutableCollections.<E>list((E[])new Object[]{e0,e1});}
 static <E>List<E> of(E e0,E e1,E e2){return ImmutableCollections.<E>list((E[])new Object[]{e0,e1,e2});}
 static <E>List<E> of(E e0,E e1,E e2,E e3){return ImmutableCollections.<E>list((E[])new Object[]{e0,e1,e2,e3});}
 static <E>List<E> of(E e0,E e1,E e2,E e3,E e4){return ImmutableCollections.<E>list((E[])new Object[]{e0,e1,e2,e3,e4});}
 static <E>List<E> of(E e0,E e1,E e2,E e3,E e4,E e5){return ImmutableCollections.<E>list((E[])new Object[]{e0,e1,e2,e3,e4,e5});}
 static <E>List<E> of(E e0,E e1,E e2,E e3,E e4,E e5,E e6){return ImmutableCollections.<E>list((E[])new Object[]{e0,e1,e2,e3,e4,e5,e6});}
 static <E>List<E> of(E e0,E e1,E e2,E e3,E e4,E e5,E e6,E e7){return ImmutableCollections.<E>list((E[])new Object[]{e0,e1,e2,e3,e4,e5,e6,e7});}
 static <E>List<E> of(E e0,E e1,E e2,E e3,E e4,E e5,E e6,E e7,E e8){return ImmutableCollections.<E>list((E[])new Object[]{e0,e1,e2,e3,e4,e5,e6,e7,e8});}
 static <E>List<E> of(E e0,E e1,E e2,E e3,E e4,E e5,E e6,E e7,E e8,E e9){return ImmutableCollections.<E>list((E[])new Object[]{e0,e1,e2,e3,e4,e5,e6,e7,e8,e9});}
 @SafeVarargs static <E>List<E> of(E... values){return ImmutableCollections.list(values);}
 static <E>List<E> copyOf(Collection<? extends E> values){return ImmutableCollections.list((E[])values.toArray());}

 default void replaceAll(java.util.function.UnaryOperator<E> operator){Objects.requireNonNull(operator);ListIterator<E> cursor=listIterator();while(cursor.hasNext())cursor.set(operator.apply(cursor.next()));}
 default void sort(Comparator<? super E> comparator){Object[] values=toArray();Arrays.sort(values,(Comparator)comparator);ListIterator<E> cursor=listIterator();for(Object value:values){cursor.next();cursor.set((E)value);}}
 E get(int index);E set(int index,E value);void add(int index,E value);E remove(int index);int indexOf(Object value);int lastIndexOf(Object value);boolean addAll(int index,Collection<? extends E> values);ListIterator<E> listIterator();ListIterator<E> listIterator(int index);List<E> subList(int start,int end);
}
