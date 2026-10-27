package java.util;
/** Sequential sorted-array map. Ordering semantics, not balanced-tree complexity. */
public class TreeMap<K,V> extends AbstractMap<K,V> implements NavigableMap<K,V>,java.io.Serializable {
 final ArrayList<SimpleEntry<K,V>> entries;final Comparator<? super K> order;
 public TreeMap(){this((Comparator<? super K>)null);}public TreeMap(Comparator<? super K> order){this.order=order;entries=new ArrayList<>();}
 public TreeMap(Map<? extends K,? extends V> source){this();putAll(source);}public TreeMap(SortedMap<K,? extends V> source){this(source.comparator());putAll(source);}
 int compare(Object a,Object b){return order==null?((Comparable<Object>)Objects.requireNonNull(a)).compareTo(b):order.compare((K)a,(K)b);}
 int locate(Object key){if(order==null){Objects.requireNonNull(key);Comparable<?> checked=(Comparable<?>)key;}int low=0,high=entries.size();while(low<high){int middle=(low+high)>>>1;if(compare(key,entries.get(middle).getKey())>0)low=middle+1;else high=middle;}return low;}
 public Comparator<? super K> comparator(){return order;}public int size(){return entries.size();}
 public boolean containsKey(Object key){int i=locate(key);return i<size()&&compare(key,entries.get(i).getKey())==0;}
 public V get(Object key){int i=locate(key);return i<size()&&compare(key,entries.get(i).getKey())==0?entries.get(i).getValue():null;}
 public V put(K key,V value){if(entries.isEmpty())compare(key,key);int i=locate(key);if(i<size()&&compare(key,entries.get(i).getKey())==0)return entries.get(i).setValue(value);entries.add(i,new SimpleEntry<K,V>(key,value));return null;}
 public V remove(Object key){int i=locate(key);return i<size()&&compare(key,entries.get(i).getKey())==0?entries.remove(i).getValue():null;}
 public void clear(){entries.clear();}public K firstKey(){if(isEmpty())throw new NoSuchElementException();return entries.get(0).getKey();}public K lastKey(){if(isEmpty())throw new NoSuchElementException();return entries.get(size()-1).getKey();}
 public SortedMap<K,V> subMap(K from,K to){throw new UnsupportedOperationException("sorted range views");}public SortedMap<K,V> headMap(K to){throw new UnsupportedOperationException("sorted range views");}public SortedMap<K,V> tailMap(K from){throw new UnsupportedOperationException("sorted range views");}
 public Set<Map.Entry<K,V>> entrySet(){return new AbstractSet<Map.Entry<K,V>>(){public int size(){return TreeMap.this.size();}public void clear(){TreeMap.this.clear();}public Iterator<Map.Entry<K,V>> iterator(){return new Iterator<Map.Entry<K,V>>(){int index,last=-1;public boolean hasNext(){return index<entries.size();}public Map.Entry<K,V> next(){if(!hasNext())throw new NoSuchElementException();last=index;return entries.get(index++);}public void remove(){if(last<0)throw new IllegalStateException();entries.remove(last);index=last;last=-1;}};}};}
 Entry<K,V> snapshot(int index){return index<0||index>=entries.size()?null:new SimpleImmutableEntry<K,V>(entries.get(index));}
 K key(Entry<K,V> entry){return entry==null?null:entry.getKey();}
 Entry<K,V> neighbor(K key,boolean up,boolean inclusive){int i=locate(key);boolean equal=i<entries.size()&&compare(key,entries.get(i).getKey())==0;return snapshot(up?i+(equal&&!inclusive?1:0):i-(equal&&inclusive?0:1));}
 public Entry<K,V> lowerEntry(K key){return neighbor(key,false,false);}public K lowerKey(K key){return key(lowerEntry(key));}
 public Entry<K,V> floorEntry(K key){return neighbor(key,false,true);}public K floorKey(K key){return key(floorEntry(key));}
 public Entry<K,V> ceilingEntry(K key){return neighbor(key,true,true);}public K ceilingKey(K key){return key(ceilingEntry(key));}
 public Entry<K,V> higherEntry(K key){return neighbor(key,true,false);}public K higherKey(K key){return key(higherEntry(key));}
 public Entry<K,V> firstEntry(){return snapshot(0);}public Entry<K,V> lastEntry(){return snapshot(entries.size()-1);}
 public Entry<K,V> pollFirstEntry(){Entry<K,V> e=firstEntry();if(e!=null)remove(e.getKey());return e;}public Entry<K,V> pollLastEntry(){Entry<K,V> e=lastEntry();if(e!=null)remove(e.getKey());return e;}
 public NavigableMap<K,V> descendingMap(){throw new UnsupportedOperationException("descending map view");}
 public NavigableSet<K> navigableKeySet(){throw new UnsupportedOperationException("navigable key view");}public NavigableSet<K> descendingKeySet(){return descendingMap().navigableKeySet();}
 public NavigableMap<K,V> subMap(K from,boolean fi,K to,boolean ti){throw new UnsupportedOperationException("inclusive range view");}public NavigableMap<K,V> headMap(K to,boolean inclusive){throw new UnsupportedOperationException("inclusive range view");}public NavigableMap<K,V> tailMap(K from,boolean inclusive){throw new UnsupportedOperationException("inclusive range view");}
}
