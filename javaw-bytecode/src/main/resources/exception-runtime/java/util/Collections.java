package java.util;
public final class Collections{
 public static <T>Collection<T> unmodifiableCollection(Collection<? extends T> source){return new UnmodifiableViews.CollectionView<T>(source);}
 public static <T>List<T> unmodifiableList(List<? extends T> source){return UnmodifiableViews.list(source);}
 public static <T>Set<T> unmodifiableSet(Set<? extends T> source){return new UnmodifiableViews.SetView<T>(source);}
 public static <K,V>Map<K,V> unmodifiableMap(Map<? extends K,? extends V> source){return new UnmodifiableViews.MapView<K,V>(source);}
 public static final List EMPTY_LIST=new Frozen<Object>(new Object[0]);
 public static final Set EMPTY_SET=new SmallSet<Object>(new Object[0]);
 public static final Map EMPTY_MAP=new SmallMap<Object,Object>(new HashMap<Object,Object>());
 public static <T>Set<T> emptySet(){return (Set<T>)EMPTY_SET;}
 public static <K,V>Map<K,V> emptyMap(){return (Map<K,V>)EMPTY_MAP;}
 public static <T>Set<T> singleton(T value){return new SmallSet<T>(new Object[]{value});}
 public static <K,V>Map<K,V> singletonMap(K key,V value){HashMap<K,V> map=new HashMap<K,V>();map.put(key,value);return new SmallMap<K,V>(map);}
 public static <T>List<T> nCopies(int count,T value){if(count<0)throw new IllegalArgumentException();return new Copies<T>(count,value);}
 private static final class Copies<T> extends AbstractList<T> implements RandomAccess,java.io.Serializable {
  final int count;final T value;Copies(int count,T value){this.count=count;this.value=value;}public int size(){return count;}public T get(int index){if(index<0 || index>=count)throw new IndexOutOfBoundsException();return value;}
 }


 private static final class SmallSet<T> extends AbstractSet<T> implements java.io.Serializable {
  final Object[] values;SmallSet(Object[] values){this.values=values;}public int size(){return values.length;}public boolean contains(Object value){return values.length!=0 && Objects.equals(value,values[0]);}
  public Iterator<T> iterator(){if(values.length==0)return emptyIterator();return new Iterator<T>(){boolean consumed;public boolean hasNext(){return !consumed;}public T next(){if(consumed)throw new NoSuchElementException();consumed=true;return (T)values[0];}};}
  public boolean removeIf(java.util.function.Predicate<? super T> predicate){if(values.length!=0)throw new UnsupportedOperationException();Objects.requireNonNull(predicate);return false;}
 }
 private static final class SmallMap<K,V> extends UnmodifiableViews.MapView<K,V> {
  SmallMap(Map<K,V> map){super(map);}public V remove(Object key){if(containsKey(key))throw new UnsupportedOperationException();return null;}
  public void clear(){if(!isEmpty())throw new UnsupportedOperationException();}public void putAll(Map<? extends K,? extends V> map){if(!map.isEmpty())throw new UnsupportedOperationException();}
  public void replaceAll(java.util.function.BiFunction<? super K,? super V,? extends V> function){if(!isEmpty())throw new UnsupportedOperationException();Objects.requireNonNull(function);}
  public Set<K> keySet(){return isEmpty()?Collections.<K>emptySet():Collections.singleton(source.entrySet().iterator().next().getKey());}
  public Collection<V> values(){return isEmpty()?Collections.<V>emptySet():Collections.singleton(source.entrySet().iterator().next().getValue());}
  public Set<Map.Entry<K,V>> entrySet(){return isEmpty()?Collections.<Map.Entry<K,V>>emptySet():Collections.<Map.Entry<K,V>>singleton(new AbstractMap.SimpleImmutableEntry<K,V>(source.entrySet().iterator().next()));}
 }
 public static <T>Iterator<T> emptyIterator(){return new Iterator<T>(){public boolean hasNext(){return false;}public T next(){throw new NoSuchElementException();}public void remove(){throw new IllegalStateException();}};}
 public static <T>ListIterator<T> emptyListIterator(){return new ListIterator<T>(){public boolean hasNext(){return false;}public T next(){throw new NoSuchElementException();}public boolean hasPrevious(){return false;}public T previous(){throw new NoSuchElementException();}public int nextIndex(){return 0;}public int previousIndex(){return -1;}public void remove(){throw new IllegalStateException();}public void add(T value){throw new UnsupportedOperationException();}public void set(T value){throw new UnsupportedOperationException();}};}
 public static <T>Enumeration<T> emptyEnumeration(){return enumeration(Collections.<T>emptyList());}
 public static <T>Enumeration<T> enumeration(Collection<T> values){final Iterator<T> cursor=values.iterator();return new Enumeration<T>(){public boolean hasMoreElements(){return cursor.hasNext();}public T nextElement(){return cursor.next();}};}
 public static <T>ArrayList<T> list(Enumeration<T> values){ArrayList<T> result=new ArrayList<T>();while(values.hasMoreElements())result.add(values.nextElement());return result;}
 public static <E>Set<E> newSetFromMap(Map<E,Boolean> map){return new SetFromMap<E>(map);}
 private static final class SetFromMap<E> extends AbstractSet<E> implements java.io.Serializable {
  final Map<E,Boolean> map;SetFromMap(Map<E,Boolean> map){if(!map.isEmpty())throw new IllegalArgumentException();this.map=map;}
  public int size(){return map.size();}public boolean contains(Object value){return map.containsKey(value);}public boolean add(E value){return map.put(value,Boolean.TRUE)==null;}public boolean remove(Object value){return map.remove(value)!=null;}public void clear(){map.clear();}public Iterator<E> iterator(){return map.keySet().iterator();}
 }
 public static <T>Comparator<T> reverseOrder(){return new Reverse<T>(null);}
 public static <T>Comparator<T> reverseOrder(Comparator<T> comparator){if(comparator instanceof Reverse)return ((Reverse<T>)comparator).order==null?(Comparator<T>)Comparator.naturalOrder():((Reverse<T>)comparator).order;return new Reverse<T>(comparator);}
 private static final class Reverse<T> implements Comparator<T>,java.io.Serializable{final Comparator<T> order;Reverse(Comparator<T> order){this.order=order;}public int compare(T a,T b){return order==null?((Comparable<T>)b).compareTo(a):order.compare(b,a);}public Comparator<T> reversed(){return order==null?(Comparator<T>)Comparator.naturalOrder():order;}}
 private Collections(){}private static final List<?> EMPTY=EMPTY_LIST;
 public static <T>List<T> emptyList(){return (List<T>)EMPTY;}
 public static <T>List<T> singletonList(T value){return new Frozen<T>(new Object[]{value});}
 private static final class Frozen<E> extends AbstractList<E> implements RandomAccess,java.io.Serializable{
  final Object[] values;Frozen(Object[] values){this.values=values;}public int size(){return values.length;}public E get(int index){index(index);return (E)values[index];}
  public void clear(){if(values.length!=0)throw new UnsupportedOperationException();}
  public void sort(Comparator<? super E> comparator){}
  public void replaceAll(java.util.function.UnaryOperator<E> operator){if(values.length!=0)throw new UnsupportedOperationException();Objects.requireNonNull(operator);}
  public boolean removeIf(java.util.function.Predicate<? super E> predicate){if(values.length!=0)throw new UnsupportedOperationException();Objects.requireNonNull(predicate);return false;}
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
