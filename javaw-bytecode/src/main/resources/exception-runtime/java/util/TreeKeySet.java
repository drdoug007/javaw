package java.util;
/** A live navigable key view shared by sorted maps and sets. */
final class TreeKeySet<E> extends AbstractSet<E> implements NavigableSet<E> {
 final NavigableMap<E,?> map;TreeKeySet(NavigableMap<E,?> map){this.map=map;}
 public int size(){return map.size();}public boolean contains(Object value){return map.containsKey(value);}public boolean remove(Object value){if(!map.containsKey(value))return false;map.remove(value);return true;}public void clear(){map.clear();}
 public Iterator<E> iterator(){Iterator<? extends Map.Entry<E,?>> entries=map.entrySet().iterator();return new Iterator<E>(){public boolean hasNext(){return entries.hasNext();}public E next(){return entries.next().getKey();}public void remove(){entries.remove();}};}
 public Comparator<? super E> comparator(){return map.comparator();}public E first(){return map.firstKey();}public E last(){return map.lastKey();}public E lower(E e){return map.lowerKey(e);}public E floor(E e){return map.floorKey(e);}public E ceiling(E e){return map.ceilingKey(e);}public E higher(E e){return map.higherKey(e);}
 E key(Map.Entry<E,?> e){return e==null?null:e.getKey();}public E pollFirst(){return key(map.pollFirstEntry());}public E pollLast(){return key(map.pollLastEntry());}
 public NavigableSet<E> descendingSet(){return new TreeKeySet<E>(map.descendingMap());}public Iterator<E> descendingIterator(){return descendingSet().iterator();}
 public NavigableSet<E> subSet(E from,boolean fi,E to,boolean ti){return new TreeKeySet<E>(map.subMap(from,fi,to,ti));}public NavigableSet<E> headSet(E to,boolean inclusive){return new TreeKeySet<E>(map.headMap(to,inclusive));}public NavigableSet<E> tailSet(E from,boolean inclusive){return new TreeKeySet<E>(map.tailMap(from,inclusive));}
 public SortedSet<E> subSet(E from,E to){return subSet(from,true,to,false);}public SortedSet<E> headSet(E to){return headSet(to,false);}public SortedSet<E> tailSet(E from){return tailSet(from,true);}
}
