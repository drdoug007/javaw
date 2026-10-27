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
}
