package java.util;import java.util.function.*;
public abstract class AbstractMap<K,V> implements Map<K,V>{
 protected AbstractMap(){}public int size(){return entrySet().size();}public boolean isEmpty(){return size()==0;}
 public boolean containsKey(Object key){for(Entry<K,V> entry:entrySet())if(Objects.equals(key,entry.getKey()))return true;return false;}
 public boolean containsValue(Object value){for(Entry<K,V> entry:entrySet())if(Objects.equals(value,entry.getValue()))return true;return false;}
 public V get(Object key){for(Entry<K,V> entry:entrySet())if(Objects.equals(key,entry.getKey()))return entry.getValue();return null;}
 public V put(K key,V value){throw new UnsupportedOperationException();}
 public V remove(Object key){Iterator<Entry<K,V>> iterator=entrySet().iterator();while(iterator.hasNext()){Entry<K,V> entry=iterator.next();if(Objects.equals(key,entry.getKey())){V old=entry.getValue();iterator.remove();return old;}}return null;}
 public void putAll(Map<? extends K,? extends V> source){for(Entry<? extends K,? extends V> entry:source.entrySet())put(entry.getKey(),entry.getValue());}
 public void clear(){entrySet().clear();}
 public Set<K> keySet(){return new AbstractSet<K>(){public int size(){return AbstractMap.this.size();}public boolean contains(Object value){return containsKey(value);}public void clear(){AbstractMap.this.clear();}public Iterator<K> iterator(){Iterator<Entry<K,V>> entries=entrySet().iterator();return new Iterator<K>(){public boolean hasNext(){return entries.hasNext();}public K next(){return entries.next().getKey();}public void remove(){entries.remove();}};}};}
 public Collection<V> values(){return new AbstractCollection<V>(){public int size(){return AbstractMap.this.size();}public boolean contains(Object value){return containsValue(value);}public void clear(){AbstractMap.this.clear();}public Iterator<V> iterator(){Iterator<Entry<K,V>> entries=entrySet().iterator();return new Iterator<V>(){public boolean hasNext(){return entries.hasNext();}public V next(){return entries.next().getValue();}public void remove(){entries.remove();}};}};}
 public boolean equals(Object other){if(other==this)return true;if(!(other instanceof Map))return false;Map<?,?> map=(Map<?,?>)other;if(map.size()!=size())return false;try{for(Entry<K,V> entry:entrySet()){Object value=entry.getValue(),key=entry.getKey();if(value==null?map.get(key)!=null || !map.containsKey(key):!value.equals(map.get(key)))return false;}return true;}catch(ClassCastException error){return false;}catch(NullPointerException error){return false;}}
 public int hashCode(){int hash=0;for(Entry<K,V> entry:entrySet())hash+=entry.hashCode();return hash;}
 public String toString(){StringBuilder out=new StringBuilder("{");Iterator<Entry<K,V>> iterator=entrySet().iterator();while(iterator.hasNext()){Entry<K,V> entry=iterator.next();out.append(entry.getKey()==this?"(this Map)":String.valueOf(entry.getKey())).append('=').append(entry.getValue()==this?"(this Map)":String.valueOf(entry.getValue()));if(iterator.hasNext())out.append(", ");}return out.append('}').toString();}
 public static class SimpleEntry<K,V> implements Entry<K,V>,java.io.Serializable{private final K key;private V value;public SimpleEntry(K key,V value){this.key=key;this.value=value;}public SimpleEntry(Entry<? extends K,? extends V> other){this(other.getKey(),other.getValue());}public K getKey(){return key;}public V getValue(){return value;}public V setValue(V value){V old=this.value;this.value=value;return old;}public boolean equals(Object other){return other instanceof Entry && Objects.equals(key,((Entry<?,?>)other).getKey()) && Objects.equals(value,((Entry<?,?>)other).getValue());}public int hashCode(){return Objects.hashCode(key)^Objects.hashCode(value);}public String toString(){return new StringBuilder().append(key).append('=').append(value).toString();}}
 public static class SimpleImmutableEntry<K,V> implements Entry<K,V>,java.io.Serializable {
  private final K key;private final V value;
  public SimpleImmutableEntry(K key,V value){this.key=key;this.value=value;}
  public SimpleImmutableEntry(Entry<? extends K,? extends V> other){this(other.getKey(),other.getValue());}
  public K getKey(){return key;}public V getValue(){return value;}
  public V setValue(V value){throw new UnsupportedOperationException();}
  public boolean equals(Object other){return other instanceof Entry && Objects.equals(key,((Entry<?,?>)other).getKey()) && Objects.equals(value,((Entry<?,?>)other).getValue());}
  public int hashCode(){return Objects.hashCode(key)^Objects.hashCode(value);}
  public String toString(){return new StringBuilder().append(key).append('=').append(value).toString();}
 }
}
