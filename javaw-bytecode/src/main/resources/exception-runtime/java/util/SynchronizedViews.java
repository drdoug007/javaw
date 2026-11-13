package java.util;
import java.util.function.*;

/** Read-write delegating views; every call forwards to the backing collection and writes through. */
final class SynchronizedViews {
    private SynchronizedViews(){}
    static <E> Collection<E> collection(Collection<E> c){return new CollectionView<E>(c);}
    static <E> Set<E> set(Set<E> s){return new SetView<E>(s);}
    static <E> List<E> list(List<E> l){Objects.requireNonNull(l);return l instanceof RandomAccess?new RandomListView<E>(l):new ListView<E>(l);}
    static <K,V> Map<K,V> map(Map<K,V> m){return new MapView<K,V>(m);}
    static class CollectionView<E> implements Collection<E>, java.io.Serializable {
        final Collection<E> c;
        CollectionView(Collection<E> c){this.c=Objects.requireNonNull(c);}
        public int size(){return c.size();}
        public boolean isEmpty(){return c.isEmpty();}
        public boolean contains(Object value){return c.contains(value);}
        public boolean containsAll(Collection<?> values){return c.containsAll(values);}
        public Iterator<E> iterator(){return c.iterator();}
        public Object[] toArray(){return c.toArray();}
        public <T>T[] toArray(T[] values){return c.toArray(values);}
        public <T>T[] toArray(IntFunction<T[]> generator){return c.toArray(generator);}
        public boolean add(E value){return c.add(value);}
        public boolean remove(Object value){return c.remove(value);}
        public boolean addAll(Collection<? extends E> values){return c.addAll(values);}
        public boolean removeAll(Collection<?> values){return c.removeAll(values);}
        public boolean retainAll(Collection<?> values){return c.retainAll(values);}
        public void clear(){c.clear();}
        public boolean removeIf(Predicate<? super E> filter){return c.removeIf(filter);}
        public Spliterator<E> spliterator(){return c.spliterator();}
        public java.util.stream.Stream<E> stream(){return c.stream();}
        public void forEach(Consumer<? super E> action){c.forEach(action);}
        public String toString(){return c.toString();}
    }
    static class SetView<E> extends CollectionView<E> implements Set<E> {
        SetView(Set<E> source){super(source);}
        public boolean equals(Object o){return this == o || c.equals(o);}
        public int hashCode(){return c.hashCode();}
    }
    static class ListView<E> extends CollectionView<E> implements List<E> {
        final List<E> list;
        ListView(List<E> list){super(list);this.list=list;}
        public E get(int index){return list.get(index);}
        public E set(int index,E value){return list.set(index,value);}
        public void add(int index,E value){list.add(index,value);}
        public E remove(int index){return list.remove(index);}
        public int indexOf(Object value){return list.indexOf(value);}
        public int lastIndexOf(Object value){return list.lastIndexOf(value);}
        public boolean addAll(int index,Collection<? extends E> values){return list.addAll(index,values);}
        public ListIterator<E> listIterator(){return list.listIterator();}
        public ListIterator<E> listIterator(int index){return list.listIterator(index);}
        // As in the JDK, the wrapper's class (not the backing sublist) decides whether a sublist is RandomAccess.
        public List<E> subList(int start,int end){return new ListView<E>(list.subList(start,end));}
        public void replaceAll(UnaryOperator<E> function){list.replaceAll(function);}
        public void sort(Comparator<? super E> comparator){list.sort(comparator);}
        public boolean equals(Object o){return this == o || list.equals(o);}
        public int hashCode(){return list.hashCode();}
    }
    static final class RandomListView<E> extends ListView<E> implements RandomAccess {
        RandomListView(List<E> list){super(list);}
        public List<E> subList(int start,int end){return new RandomListView<E>(list.subList(start,end));}
    }
    static class MapView<K,V> implements Map<K,V>, java.io.Serializable {
        final Map<K,V> m;
        // Created on first use and then reused, as the JDK does; each wraps the backing map's live view.
        private transient Set<K> keySet;
        private transient Collection<V> values;
        private transient Set<Map.Entry<K,V>> entrySet;
        MapView(Map<K,V> m){this.m=Objects.requireNonNull(m);}
        public int size(){return m.size();}
        public boolean isEmpty(){return m.isEmpty();}
        public boolean containsKey(Object key){return m.containsKey(key);}
        public boolean containsValue(Object value){return m.containsValue(value);}
        public V get(Object key){return m.get(key);}
        public V put(K key,V value){return m.put(key,value);}
        public V remove(Object key){return m.remove(key);}
        public void putAll(Map<? extends K,? extends V> values){m.putAll(values);}
        public void clear(){m.clear();}
        public Set<K> keySet(){if(keySet==null)keySet=new SetView<K>(m.keySet());return keySet;}
        public Collection<V> values(){if(values==null)values=new CollectionView<V>(m.values());return values;}
        public Set<Map.Entry<K,V>> entrySet(){if(entrySet==null)entrySet=new SetView<Map.Entry<K,V>>(m.entrySet());return entrySet;}
        public V getOrDefault(Object key,V fallback){return m.getOrDefault(key,fallback);}
        public V putIfAbsent(K key,V value){return m.putIfAbsent(key,value);}
        public boolean remove(Object key,Object value){return m.remove(key,value);}
        public boolean replace(K key,V oldValue,V value){return m.replace(key,oldValue,value);}
        public V replace(K key,V value){return m.replace(key,value);}
        public void forEach(BiConsumer<? super K,? super V> action){m.forEach(action);}
        public void replaceAll(BiFunction<? super K,? super V,? extends V> function){m.replaceAll(function);}
        public V computeIfAbsent(K key,Function<? super K,? extends V> function){return m.computeIfAbsent(key,function);}
        public V computeIfPresent(K key,BiFunction<? super K,? super V,? extends V> function){return m.computeIfPresent(key,function);}
        public V compute(K key,BiFunction<? super K,? super V,? extends V> function){return m.compute(key,function);}
        public V merge(K key,V value,BiFunction<? super V,? super V,? extends V> function){return m.merge(key,value,function);}
        public boolean equals(Object o){return this == o || m.equals(o);}
        public int hashCode(){return m.hashCode();}
        public String toString(){return m.toString();}
    }
}
