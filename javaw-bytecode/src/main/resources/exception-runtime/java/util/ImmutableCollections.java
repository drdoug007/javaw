package java.util;
final class ImmutableCollections {
    private ImmutableCollections(){}
    static <E> List<E> list(E[] values){ArrayList<E> copy=new ArrayList<E>(values.length);for(E value:values)copy.add(Objects.requireNonNull(value));return new ImmutableList<E>(copy);}
    static <E> Set<E> set(E[] values,boolean rejectDuplicates){LinkedHashSet<E> copy=new LinkedHashSet<E>();for(E value:values)if(!copy.add(Objects.requireNonNull(value)) && rejectDuplicates)throw new IllegalArgumentException();return new ImmutableSet<E>(copy);}
    static <K,V> Map<K,V> map(Map.Entry<? extends K,? extends V>[] entries){LinkedHashMap<K,V> copy=new LinkedHashMap<K,V>();for(Map.Entry<? extends K,? extends V> entry:entries){K key=Objects.requireNonNull(entry.getKey());V value=Objects.requireNonNull(entry.getValue());if(copy.containsKey(key))throw new IllegalArgumentException();copy.put(key,value);}return new ImmutableMap<K,V>(copy);}
    private static final class ImmutableList<E> extends UnmodifiableViews.ListView<E> implements RandomAccess {
        ImmutableList(List<? extends E> source){super(source);}public boolean contains(Object value){return super.contains(Objects.requireNonNull(value));}public int indexOf(Object value){return super.indexOf(Objects.requireNonNull(value));}public int lastIndexOf(Object value){return super.lastIndexOf(Objects.requireNonNull(value));}
        public List<E> subList(int start,int end){return new ImmutableList<E>(list.subList(start,end));}
    }
    private static final class ImmutableSet<E> extends UnmodifiableViews.SetView<E> {
        ImmutableSet(Set<E> source){super(source);}public boolean contains(Object value){return super.contains(Objects.requireNonNull(value));}
    }
    private static final class ImmutableMap<K,V> extends UnmodifiableViews.MapView<K,V> {
        ImmutableMap(Map<K,V> source){super(source);}public V get(Object key){return super.get(Objects.requireNonNull(key));}public boolean containsKey(Object key){return super.containsKey(Objects.requireNonNull(key));}public boolean containsValue(Object value){return super.containsValue(Objects.requireNonNull(value));}
    }
}
