package java.util;
/** Selected sequential sorted set backed by the sorted-map runtime. */
public class TreeSet<E> extends AbstractSet<E> implements NavigableSet<E>,Cloneable,java.io.Serializable {
 final NavigableMap<E,Object> map;private static final Object PRESENT=new Object();
 public TreeSet(){this((Comparator<? super E>)null);}public TreeSet(Comparator<? super E> comparator){map=new TreeMap<E,Object>(comparator);}
 public TreeSet(Collection<? extends E> values){this();addAll(values);}public TreeSet(SortedSet<E> values){this(values.comparator());addAll(values);}
 TreeSet(NavigableMap<E,Object> map){this.map=map;}
 public int size(){return map.size();}public boolean isEmpty(){return map.isEmpty();}public boolean contains(Object value){return map.containsKey(value);}public boolean add(E value){return map.put(value,PRESENT)==null;}public boolean remove(Object value){return map.remove(value)!=null;}public void clear(){map.clear();}
 public Iterator<E> iterator(){return map.navigableKeySet().iterator();}public Iterator<E> descendingIterator(){return map.descendingKeySet().iterator();}
 public Comparator<? super E> comparator(){return map.comparator();}public E first(){return map.firstKey();}public E last(){return map.lastKey();}public E lower(E e){return map.lowerKey(e);}public E floor(E e){return map.floorKey(e);}public E ceiling(E e){return map.ceilingKey(e);}public E higher(E e){return map.higherKey(e);}
 E key(Map.Entry<E,Object> e){return e==null?null:e.getKey();}public E pollFirst(){return key(map.pollFirstEntry());}public E pollLast(){return key(map.pollLastEntry());}
 public Object clone(){return new TreeSet<E>(new TreeMap<E,Object>((SortedMap<E,Object>)map));}
 public NavigableSet<E> descendingSet(){return new TreeSet<E>(map.descendingMap());}public NavigableSet<E> subSet(E from,boolean fi,E to,boolean ti){return new TreeSet<E>(map.subMap(from,fi,to,ti));}public NavigableSet<E> headSet(E to,boolean inclusive){return new TreeSet<E>(map.headMap(to,inclusive));}public NavigableSet<E> tailSet(E from,boolean inclusive){return new TreeSet<E>(map.tailMap(from,inclusive));}
 public SortedSet<E> subSet(E from,E to){return subSet(from,true,to,false);}public SortedSet<E> headSet(E to){return headSet(to,false);}public SortedSet<E> tailSet(E from){return tailSet(from,true);}
}
