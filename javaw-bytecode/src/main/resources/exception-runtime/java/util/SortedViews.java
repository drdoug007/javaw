package java.util;
/** Live read-only sorted views, including protected entries and iterators. */
final class SortedViews {
 static class SetView<E> extends UnmodifiableViews.SetView<E> implements SortedSet<E>{
 final SortedSet<E> sorted;SetView(SortedSet<E> sorted){super(sorted);this.sorted=sorted;}
 public Comparator<? super E> comparator(){return sorted.comparator();}public E first(){return sorted.first();}public E last(){return sorted.last();}
 public SortedSet<E> subSet(E from,E to){return new SetView<E>(sorted.subSet(from,to));}public SortedSet<E> headSet(E to){return new SetView<E>(sorted.headSet(to));}public SortedSet<E> tailSet(E from){return new SetView<E>(sorted.tailSet(from));}
 }
 static class MapView<K,V> extends UnmodifiableViews.MapView<K,V> implements SortedMap<K,V>{
 final SortedMap<K,? extends V> sorted;MapView(SortedMap<K,? extends V> sorted){super(sorted);this.sorted=sorted;}
 public Comparator<? super K> comparator(){return sorted.comparator();}public K firstKey(){return sorted.firstKey();}public K lastKey(){return sorted.lastKey();}
 public SortedMap<K,V> subMap(K from,K to){return new MapView<K,V>(sorted.subMap(from,to));}public SortedMap<K,V> headMap(K to){return new MapView<K,V>(sorted.headMap(to));}public SortedMap<K,V> tailMap(K from){return new MapView<K,V>(sorted.tailMap(from));}
 }

 static class NavigableSetView<E> extends SetView<E> implements NavigableSet<E>{
 final NavigableSet<E> navigable;NavigableSetView(NavigableSet<E> source){super(source);navigable=source;}
 public E lower(E e){return navigable.lower(e);}public E floor(E e){return navigable.floor(e);}public E ceiling(E e){return navigable.ceiling(e);}public E higher(E e){return navigable.higher(e);}
 public E pollFirst(){throw new UnsupportedOperationException();}public E pollLast(){throw new UnsupportedOperationException();}
 public NavigableSet<E> descendingSet(){return new NavigableSetView<E>(navigable.descendingSet());}public Iterator<E> descendingIterator(){return UnmodifiableViews.iterator(navigable.descendingIterator());}
 public NavigableSet<E> subSet(E from,boolean fi,E to,boolean ti){return new NavigableSetView<E>(navigable.subSet(from,fi,to,ti));}public NavigableSet<E> headSet(E to,boolean inclusive){return new NavigableSetView<E>(navigable.headSet(to,inclusive));}public NavigableSet<E> tailSet(E from,boolean inclusive){return new NavigableSetView<E>(navigable.tailSet(from,inclusive));}
 }
 static class NavigableMapView<K,V> extends MapView<K,V> implements NavigableMap<K,V>{
 final NavigableMap<K,? extends V> navigable;NavigableMapView(NavigableMap<K,? extends V> source){super(source);navigable=source;}
 Entry<K,V> protect(Entry<K,? extends V> e){return e==null?null:new UnmodifiableViews.EntryView<K,V>(e);}
 public K lowerKey(K key){return navigable.lowerKey(key);}public K floorKey(K key){return navigable.floorKey(key);}public K ceilingKey(K key){return navigable.ceilingKey(key);}public K higherKey(K key){return navigable.higherKey(key);}
 public Entry<K,V> lowerEntry(K key){return protect(navigable.lowerEntry(key));}public Entry<K,V> floorEntry(K key){return protect(navigable.floorEntry(key));}public Entry<K,V> ceilingEntry(K key){return protect(navigable.ceilingEntry(key));}public Entry<K,V> higherEntry(K key){return protect(navigable.higherEntry(key));}public Entry<K,V> firstEntry(){return protect(navigable.firstEntry());}public Entry<K,V> lastEntry(){return protect(navigable.lastEntry());}
 public Entry<K,V> pollFirstEntry(){throw new UnsupportedOperationException();}public Entry<K,V> pollLastEntry(){throw new UnsupportedOperationException();}
 public NavigableMap<K,V> descendingMap(){return new NavigableMapView<K,V>(navigable.descendingMap());}
 public NavigableSet<K> navigableKeySet(){return new NavigableSetView<K>(navigable.navigableKeySet());}public NavigableSet<K> descendingKeySet(){return new NavigableSetView<K>(navigable.descendingKeySet());}
 public NavigableMap<K,V> subMap(K from,boolean fi,K to,boolean ti){return new NavigableMapView<K,V>(navigable.subMap(from,fi,to,ti));}public NavigableMap<K,V> headMap(K to,boolean inclusive){return new NavigableMapView<K,V>(navigable.headMap(to,inclusive));}public NavigableMap<K,V> tailMap(K from,boolean inclusive){return new NavigableMapView<K,V>(navigable.tailMap(from,inclusive));}
 }
}
