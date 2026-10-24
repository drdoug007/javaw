package java.util;import java.util.function.*;
public interface Map<K,V>{
 static <K,V>Map<K,V> of(){return ImmutableCollections.map((Entry<K,V>[])new Entry[]{});}
 static <K,V>Map<K,V> of(K k0,V v0){return ImmutableCollections.map((Entry<K,V>[])new Entry[]{entry(k0,v0)});}
 static <K,V>Map<K,V> of(K k0,V v0,K k1,V v1){return ImmutableCollections.map((Entry<K,V>[])new Entry[]{entry(k0,v0),entry(k1,v1)});}
 static <K,V>Map<K,V> of(K k0,V v0,K k1,V v1,K k2,V v2){return ImmutableCollections.map((Entry<K,V>[])new Entry[]{entry(k0,v0),entry(k1,v1),entry(k2,v2)});}
 static <K,V>Map<K,V> of(K k0,V v0,K k1,V v1,K k2,V v2,K k3,V v3){return ImmutableCollections.map((Entry<K,V>[])new Entry[]{entry(k0,v0),entry(k1,v1),entry(k2,v2),entry(k3,v3)});}
 static <K,V>Map<K,V> of(K k0,V v0,K k1,V v1,K k2,V v2,K k3,V v3,K k4,V v4){return ImmutableCollections.map((Entry<K,V>[])new Entry[]{entry(k0,v0),entry(k1,v1),entry(k2,v2),entry(k3,v3),entry(k4,v4)});}
 static <K,V>Map<K,V> of(K k0,V v0,K k1,V v1,K k2,V v2,K k3,V v3,K k4,V v4,K k5,V v5){return ImmutableCollections.map((Entry<K,V>[])new Entry[]{entry(k0,v0),entry(k1,v1),entry(k2,v2),entry(k3,v3),entry(k4,v4),entry(k5,v5)});}
 static <K,V>Map<K,V> of(K k0,V v0,K k1,V v1,K k2,V v2,K k3,V v3,K k4,V v4,K k5,V v5,K k6,V v6){return ImmutableCollections.map((Entry<K,V>[])new Entry[]{entry(k0,v0),entry(k1,v1),entry(k2,v2),entry(k3,v3),entry(k4,v4),entry(k5,v5),entry(k6,v6)});}
 static <K,V>Map<K,V> of(K k0,V v0,K k1,V v1,K k2,V v2,K k3,V v3,K k4,V v4,K k5,V v5,K k6,V v6,K k7,V v7){return ImmutableCollections.map((Entry<K,V>[])new Entry[]{entry(k0,v0),entry(k1,v1),entry(k2,v2),entry(k3,v3),entry(k4,v4),entry(k5,v5),entry(k6,v6),entry(k7,v7)});}
 static <K,V>Map<K,V> of(K k0,V v0,K k1,V v1,K k2,V v2,K k3,V v3,K k4,V v4,K k5,V v5,K k6,V v6,K k7,V v7,K k8,V v8){return ImmutableCollections.map((Entry<K,V>[])new Entry[]{entry(k0,v0),entry(k1,v1),entry(k2,v2),entry(k3,v3),entry(k4,v4),entry(k5,v5),entry(k6,v6),entry(k7,v7),entry(k8,v8)});}
 static <K,V>Map<K,V> of(K k0,V v0,K k1,V v1,K k2,V v2,K k3,V v3,K k4,V v4,K k5,V v5,K k6,V v6,K k7,V v7,K k8,V v8,K k9,V v9){return ImmutableCollections.map((Entry<K,V>[])new Entry[]{entry(k0,v0),entry(k1,v1),entry(k2,v2),entry(k3,v3),entry(k4,v4),entry(k5,v5),entry(k6,v6),entry(k7,v7),entry(k8,v8),entry(k9,v9)});}
 @SafeVarargs static <K,V>Map<K,V> ofEntries(Entry<? extends K,? extends V>... entries){return ImmutableCollections.map(entries);}
 static <K,V>Entry<K,V> entry(K key,V value){return new AbstractMap.SimpleImmutableEntry<K,V>(Objects.requireNonNull(key),Objects.requireNonNull(value));}
 static <K,V>Map<K,V> copyOf(Map<? extends K,? extends V> source){return ImmutableCollections.map(source.entrySet().toArray(new Entry[0]));}

 int size();boolean isEmpty();boolean containsKey(Object key);boolean containsValue(Object value);V get(Object key);V put(K key,V value);V remove(Object key);void putAll(Map<? extends K,? extends V> values);void clear();Set<K> keySet();Collection<V> values();Set<Entry<K,V>> entrySet();
 interface Entry<K,V>{K getKey();V getValue();V setValue(V value);boolean equals(Object other);int hashCode();
 static <K extends Comparable<? super K>,V>Comparator<Entry<K,V>> comparingByKey(){return comparingByKey(Comparator.<K>naturalOrder());}
 static <K,V extends Comparable<? super V>>Comparator<Entry<K,V>> comparingByValue(){return comparingByValue(Comparator.<V>naturalOrder());}
 static <K,V>Comparator<Entry<K,V>> comparingByKey(Comparator<? super K> order){Objects.requireNonNull(order);return new EntryOrder<K,V,K>(order,true);}
 static <K,V>Comparator<Entry<K,V>> comparingByValue(Comparator<? super V> order){Objects.requireNonNull(order);return new EntryOrder<K,V,V>(order,false);}
 }
 final class EntryOrder<K,V,T> implements Comparator<Entry<K,V>>,java.io.Serializable {
  private final Comparator<? super T> order;private final boolean key;
  EntryOrder(Comparator<? super T> order,boolean key){this.order=order;this.key=key;}
  public int compare(Entry<K,V> a,Entry<K,V> b){return order.compare((T)(key?a.getKey():a.getValue()),(T)(key?b.getKey():b.getValue()));}
 }
 default V getOrDefault(Object key,V fallback){V value=get(key);return value!=null || containsKey(key)?value:fallback;}
 default V putIfAbsent(K key,V value){V old=get(key);if(old==null)old=put(key,value);return old;}
 default boolean remove(Object key,Object value){V old=get(key);if(!Objects.equals(old,value) || old==null && !containsKey(key))return false;remove(key);return true;}
 default boolean replace(K key,V old,V value){if(!Objects.equals(get(key),old) || old==null && !containsKey(key))return false;put(key,value);return true;}
 default V replace(K key,V value){return containsKey(key)?put(key,value):null;}
 default void forEach(BiConsumer<? super K,? super V> action){Objects.requireNonNull(action);for(Entry<K,V> entry:entrySet())action.accept(entry.getKey(),entry.getValue());}
 default void replaceAll(BiFunction<? super K,? super V,? extends V> function){Objects.requireNonNull(function);for(Entry<K,V> entry:entrySet())entry.setValue(function.apply(entry.getKey(),entry.getValue()));}
 default V computeIfAbsent(K key,Function<? super K,? extends V> function){Objects.requireNonNull(function);V value=get(key);if(value==null){V next=function.apply(key);if(next!=null){put(key,next);return next;}}return value;}
 default V computeIfPresent(K key,BiFunction<? super K,? super V,? extends V> function){Objects.requireNonNull(function);V value=get(key);if(value==null)return null;V next=function.apply(key,value);if(next==null)remove(key);else put(key,next);return next;}
 default V compute(K key,BiFunction<? super K,? super V,? extends V> function){Objects.requireNonNull(function);V old=get(key),next=function.apply(key,old);if(next==null){if(old!=null || containsKey(key))remove(key);}else put(key,next);return next;}
 default V merge(K key,V value,BiFunction<? super V,? super V,? extends V> function){Objects.requireNonNull(value);Objects.requireNonNull(function);V old=get(key),next=old==null?value:function.apply(old,value);if(next==null)remove(key);else put(key,next);return next;}
}
