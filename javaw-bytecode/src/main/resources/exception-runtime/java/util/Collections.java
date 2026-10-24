package java.util;
public final class Collections{
 private Collections(){}private static final List<?> EMPTY=new Frozen<Object>(new Object[0]);
 public static <T>List<T> emptyList(){return (List<T>)EMPTY;}
 public static <T>List<T> singletonList(T value){return new Frozen<T>(new Object[]{value});}
 private static final class Frozen<E> extends AbstractList<E> implements RandomAccess,java.io.Serializable{
  final Object[] values;Frozen(Object[] values){this.values=values;}public int size(){return values.length;}public E get(int index){index(index);return (E)values[index];}
  public void clear(){if(values.length!=0)throw new UnsupportedOperationException();}
 }
}
