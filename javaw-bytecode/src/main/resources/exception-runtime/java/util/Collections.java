package java.util;
public final class Collections{
 public static <T>Comparator<T> reverseOrder(){return new Reverse<T>(null);}
 public static <T>Comparator<T> reverseOrder(Comparator<T> comparator){if(comparator instanceof Reverse)return ((Reverse<T>)comparator).order==null?(Comparator<T>)Comparator.naturalOrder():((Reverse<T>)comparator).order;return new Reverse<T>(comparator);}
 private static final class Reverse<T> implements Comparator<T>,java.io.Serializable{final Comparator<T> order;Reverse(Comparator<T> order){this.order=order;}public int compare(T a,T b){return order==null?((Comparable<T>)b).compareTo(a):order.compare(b,a);}public Comparator<T> reversed(){return order==null?(Comparator<T>)Comparator.naturalOrder():order;}}
 private Collections(){}private static final List<?> EMPTY=new Frozen<Object>(new Object[0]);
 public static <T>List<T> emptyList(){return (List<T>)EMPTY;}
 public static <T>List<T> singletonList(T value){return new Frozen<T>(new Object[]{value});}
 private static final class Frozen<E> extends AbstractList<E> implements RandomAccess,java.io.Serializable{
  final Object[] values;Frozen(Object[] values){this.values=values;}public int size(){return values.length;}public E get(int index){index(index);return (E)values[index];}
  public void clear(){if(values.length!=0)throw new UnsupportedOperationException();}
 }
}
