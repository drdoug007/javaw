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

 public static <T extends Comparable<? super T>> void sort(List<T> values){values.sort(null);}
 public static <T> void sort(List<T> values,Comparator<? super T> comparator){values.sort(comparator);}
 private static <T> int compare(T a,T b,Comparator<? super T> comparator){return comparator==null?((Comparable<? super T>)a).compareTo(b):comparator.compare(a,b);}
 public static <T> int binarySearch(List<? extends Comparable<? super T>> values,T key){return binarySearch((List)values,key,null);}
 public static <T> int binarySearch(List<? extends T> values,T key,Comparator<? super T> comparator){int low=0,high=values.size()-1;while(low<=high){int middle=(low+high)>>>1,c=compare(values.get(middle),key,comparator);if(c<0)low=middle+1;else if(c>0)high=middle-1;else return middle;}return -low-1;}
 public static void swap(List<?> values,int a,int b){List list=values;list.set(a,list.set(b,list.get(a)));}
 public static void reverse(List<?> values){for(int a=0,b=values.size()-1;a<b;a++,b--)swap(values,a,b);}
 public static <T> void fill(List<? super T> values,T value){ListIterator<? super T> cursor=values.listIterator();while(cursor.hasNext()){cursor.next();cursor.set(value);}}
 public static <T> void copy(List<? super T> destination,List<? extends T> source){if(source.size()>destination.size())throw new IndexOutOfBoundsException();for(int i=0;i<source.size();i++)destination.set(i,source.get(i));}
 public static <T extends Object & Comparable<? super T>> T min(Collection<? extends T> values){return min(values,null);}
 public static <T> T min(Collection<? extends T> values,Comparator<? super T> comparator){Iterator<? extends T> cursor=values.iterator();T result=cursor.next();while(cursor.hasNext()){T value=cursor.next();if(compare(value,result,comparator)<0)result=value;}return result;}
 public static <T extends Object & Comparable<? super T>> T max(Collection<? extends T> values){return max(values,null);}
 public static <T> T max(Collection<? extends T> values,Comparator<? super T> comparator){Iterator<? extends T> cursor=values.iterator();T result=cursor.next();while(cursor.hasNext()){T value=cursor.next();if(compare(value,result,comparator)>0)result=value;}return result;}
 @SafeVarargs public static <T> boolean addAll(Collection<? super T> values,T... additions){boolean changed=false;for(T value:additions)changed|=values.add(value);return changed;}
 public static int frequency(Collection<?> values,Object value){int count=0;for(Object element:values)if(Objects.equals(value,element))count++;return count;}
 public static boolean disjoint(Collection<?> a,Collection<?> b){for(Object value:a)if(b.contains(value))return false;return true;}
 public static <T> boolean replaceAll(List<T> values,T oldValue,T newValue){boolean changed=false;ListIterator<T> cursor=values.listIterator();while(cursor.hasNext())if(Objects.equals(oldValue,cursor.next())){cursor.set(newValue);changed=true;}return changed;}
 public static int indexOfSubList(List<?> source,List<?> target){return subListIndex(source,target,false);}
 public static int lastIndexOfSubList(List<?> source,List<?> target){return subListIndex(source,target,true);}
 private static int subListIndex(List<?> source,List<?> target,boolean last){int limit=source.size()-target.size();for(int n=0;n<=limit;n++){int candidate=last?limit-n:n;boolean match=true;for(int i=0;i<target.size();i++)if(!Objects.equals(target.get(i),source.get(candidate+i))){match=false;break;}if(match)return candidate;}return -1;}

}
