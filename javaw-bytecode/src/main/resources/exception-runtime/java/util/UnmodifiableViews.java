package java.util;
import java.util.function.*;

/** Live read-only views; iterator and entry mutation paths are closed as well. */
final class UnmodifiableViews {
    private UnmodifiableViews(){}
    static <E> Iterator<E> iterator(final Iterator<? extends E> source){return new Iterator<E>(){public boolean hasNext(){return source.hasNext();}public E next(){return source.next();}public void remove(){throw new UnsupportedOperationException();}public void forEachRemaining(Consumer<? super E> action){source.forEachRemaining(action);}};}
    static class CollectionView<E> extends AbstractCollection<E> implements java.io.Serializable {
        final Collection<? extends E> source;
        CollectionView(Collection<? extends E> source){this.source=Objects.requireNonNull(source);}
        public int size(){return source.size();}public boolean isEmpty(){return source.isEmpty();}public boolean contains(Object value){return source.contains(value);}public boolean containsAll(Collection<?> values){return source.containsAll(values);}
        public Iterator<E> iterator(){return UnmodifiableViews.<E>iterator(source.iterator());}
        public Object[] toArray(){return source.toArray();}public <T>T[] toArray(T[] values){return source.toArray(values);}
        public void forEach(Consumer<? super E> action){source.forEach(action);}
        public String toString(){return source.toString();}
        public boolean add(E value){throw new UnsupportedOperationException();}public boolean remove(Object value){throw new UnsupportedOperationException();}
        public boolean addAll(Collection<? extends E> values){throw new UnsupportedOperationException();}public boolean removeAll(Collection<?> values){throw new UnsupportedOperationException();}public boolean retainAll(Collection<?> values){throw new UnsupportedOperationException();}
        public boolean removeIf(Predicate<? super E> filter){throw new UnsupportedOperationException();}public void clear(){throw new UnsupportedOperationException();}
    }
    static class SetView<E> extends CollectionView<E> implements Set<E> {
        SetView(Set<? extends E> source){super(source);}public boolean equals(Object value){return value==this || source.equals(value);}public int hashCode(){return source.hashCode();}
    }
    static class ListView<E> extends CollectionView<E> implements List<E> {
        final List<? extends E> list;
        ListView(List<? extends E> list){super(list);this.list=list;}
        public E get(int index){return list.get(index);}public int indexOf(Object value){return list.indexOf(value);}public int lastIndexOf(Object value){return list.lastIndexOf(value);}
        public boolean equals(Object value){return value==this || list.equals(value);}public int hashCode(){return list.hashCode();}
        public E set(int index,E value){throw new UnsupportedOperationException();}public void add(int index,E value){throw new UnsupportedOperationException();}public E remove(int index){throw new UnsupportedOperationException();}
        public boolean addAll(int index,Collection<? extends E> values){throw new UnsupportedOperationException();}public void replaceAll(UnaryOperator<E> function){throw new UnsupportedOperationException();}public void sort(Comparator<? super E> comparator){throw new UnsupportedOperationException();}
        public ListIterator<E> listIterator(){return listIterator(0);}public ListIterator<E> listIterator(int index){final ListIterator<? extends E> cursor=list.listIterator(index);return new ListIterator<E>(){
            public boolean hasNext(){return cursor.hasNext();}public E next(){return cursor.next();}public boolean hasPrevious(){return cursor.hasPrevious();}public E previous(){return cursor.previous();}public int nextIndex(){return cursor.nextIndex();}public int previousIndex(){return cursor.previousIndex();}
            public void remove(){throw new UnsupportedOperationException();}public void set(E value){throw new UnsupportedOperationException();}public void add(E value){throw new UnsupportedOperationException();}
        };}
        public List<E> subList(int start,int end){return list(list.subList(start,end));}
    }
    static final class RandomList<E> extends ListView<E> implements RandomAccess {RandomList(List<? extends E> list){super(list);}}
    static <E> List<E> list(List<? extends E> source){Objects.requireNonNull(source);return source instanceof RandomAccess?new RandomList<E>(source):new ListView<E>(source);}
    static class MapView<K,V> extends AbstractMap<K,V> implements java.io.Serializable {
        final Map<? extends K,? extends V> source;
        MapView(Map<? extends K,? extends V> source){this.source=Objects.requireNonNull(source);}
        public int size(){return source.size();}public boolean isEmpty(){return source.isEmpty();}public boolean containsKey(Object key){return source.containsKey(key);}public boolean containsValue(Object value){return source.containsValue(value);}public V get(Object key){return source.get(key);}
        public boolean equals(Object value){return value==this || source.equals(value);}public int hashCode(){return source.hashCode();}public String toString(){return source.toString();}
        public Set<K> keySet(){return new SetView<K>(source.keySet());}public Collection<V> values(){return new CollectionView<V>(source.values());}
        public Set<Map.Entry<K,V>> entrySet(){
            Set<Map.Entry<K,V>> entries=new AbstractSet<Map.Entry<K,V>>(){
                public int size(){return source.size();}
                public Iterator<Map.Entry<K,V>> iterator(){final Iterator<? extends Map.Entry<? extends K,? extends V>> cursor=source.entrySet().iterator();return new Iterator<Map.Entry<K,V>>(){
                    public boolean hasNext(){return cursor.hasNext();}public Map.Entry<K,V> next(){return new EntryView<K,V>(cursor.next());}public void remove(){throw new UnsupportedOperationException();}
                };}
            };return new SetView<Map.Entry<K,V>>(entries);
        }
        public V put(K key,V value){throw new UnsupportedOperationException();}public V remove(Object key){throw new UnsupportedOperationException();}public void putAll(Map<? extends K,? extends V> values){throw new UnsupportedOperationException();}public void clear(){throw new UnsupportedOperationException();}
        public V putIfAbsent(K key,V value){throw new UnsupportedOperationException();}public boolean remove(Object key,Object value){throw new UnsupportedOperationException();}public boolean replace(K key,V old,V value){throw new UnsupportedOperationException();}public V replace(K key,V value){throw new UnsupportedOperationException();}
        public void replaceAll(BiFunction<? super K,? super V,? extends V> function){throw new UnsupportedOperationException();}public V computeIfAbsent(K key,Function<? super K,? extends V> function){throw new UnsupportedOperationException();}public V computeIfPresent(K key,BiFunction<? super K,? super V,? extends V> function){throw new UnsupportedOperationException();}public V compute(K key,BiFunction<? super K,? super V,? extends V> function){throw new UnsupportedOperationException();}public V merge(K key,V value,BiFunction<? super V,? super V,? extends V> function){throw new UnsupportedOperationException();}
    }
    static final class EntryView<K,V> implements Map.Entry<K,V> {
        final Map.Entry<? extends K,? extends V> entry;EntryView(Map.Entry<? extends K,? extends V> entry){this.entry=entry;}
        public K getKey(){return entry.getKey();}public V getValue(){return entry.getValue();}public V setValue(V value){throw new UnsupportedOperationException();}
        public boolean equals(Object value){return entry.equals(value);}public int hashCode(){return entry.hashCode();}public String toString(){return entry.toString();}
    }
}
