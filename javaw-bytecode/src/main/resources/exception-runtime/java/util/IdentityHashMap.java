package java.util;
import java.util.function.*;

/** Sequential identity-key/value map; storage scans nodes without JDK complexity guarantees. */
public class IdentityHashMap<K,V> extends AbstractMap<K,V> implements Map<K,V>,Cloneable,java.io.Serializable {
    private static final class Storage<K,V> extends HashMap<K,V> {
        protected boolean keyEquals(Object a,Object b){return a==b;}
        protected int keyHash(Object key){return System.identityHashCode(key);}
    }
    private Storage<K,V> storage;
    public IdentityHashMap(){storage=new Storage<K,V>();}
    public IdentityHashMap(int expectedSize){this();if(expectedSize<0)throw new IllegalArgumentException();}
    public IdentityHashMap(Map<? extends K,? extends V> source){this();putAll(source);}
    public int size(){return storage.size();}public boolean containsKey(Object key){return storage.containsKey(key);}public V get(Object key){return storage.get(key);}
    public boolean containsValue(Object value){for(Map.Entry<K,V> entry:storage.entrySet())if(entry.getValue()==value)return true;return false;}
    public V put(K key,V value){return storage.put(key,value);}public V remove(Object key){return storage.remove(key);}public void clear(){storage.clear();}
    public boolean remove(Object key,Object value){if(!containsKey(key) || get(key)!=value)return false;remove(key);return true;}
    public boolean replace(K key,V old,V value){if(!containsKey(key) || get(key)!=old)return false;put(key,value);return true;}
    public void forEach(BiConsumer<? super K,? super V> action){storage.forEach(action);}public void replaceAll(BiFunction<? super K,? super V,? extends V> action){storage.replaceAll(action);}
    private static boolean removeAllByMembership(Collection<?> target,Collection<?> values){Objects.requireNonNull(values);boolean changed=false;Iterator<?> cursor=target.iterator();while(cursor.hasNext())if(values.contains(cursor.next())){cursor.remove();changed=true;}return changed;}
    public Set<Map.Entry<K,V>> entrySet(){return new AbstractSet<Map.Entry<K,V>>(){
        public int size(){return storage.size();}public void clear(){IdentityHashMap.this.clear();}
        public boolean removeAll(Collection<?> values){return removeAllByMembership(this,values);}
        public Object[] toArray(){return snapshots().toArray();}public <T>T[] toArray(T[] values){return snapshots().toArray(values);}
        private ArrayList<Map.Entry<K,V>> snapshots(){ArrayList<Map.Entry<K,V>> result=new ArrayList<Map.Entry<K,V>>();for(Map.Entry<K,V> entry:this)result.add(new SimpleEntry<K,V>(entry));return result;}
        public boolean contains(Object value){if(!(value instanceof Map.Entry))return false;Map.Entry<?,?> entry=(Map.Entry<?,?>)value;return containsKey(entry.getKey()) && get(entry.getKey())==entry.getValue();}
        public boolean remove(Object value){if(!(value instanceof Map.Entry))return false;Map.Entry<?,?> entry=(Map.Entry<?,?>)value;return IdentityHashMap.this.remove(entry.getKey(),entry.getValue());}
        public Iterator<Map.Entry<K,V>> iterator(){final Iterator<Map.Entry<K,V>> cursor=storage.entrySet().iterator();return new Iterator<Map.Entry<K,V>>(){IdentityEntry<K,V> previous;public boolean hasNext(){return cursor.hasNext();}public Map.Entry<K,V> next(){previous=new IdentityEntry<K,V>(cursor.next());return previous;}public void remove(){cursor.remove();previous.valid=false;}};}
    };}
    public Set<K> keySet(){return new AbstractSet<K>(){
        public int size(){return storage.size();}public boolean contains(Object key){return containsKey(key);}public boolean remove(Object key){if(!containsKey(key))return false;IdentityHashMap.this.remove(key);return true;}public void clear(){IdentityHashMap.this.clear();}
        public boolean removeAll(Collection<?> values){return removeAllByMembership(this,values);}
        public int hashCode(){int hash=0;for(K key:this)hash+=System.identityHashCode(key);return hash;}
        public Iterator<K> iterator(){final Iterator<Map.Entry<K,V>> cursor=entrySet().iterator();return new Iterator<K>(){public boolean hasNext(){return cursor.hasNext();}public K next(){return cursor.next().getKey();}public void remove(){cursor.remove();}};}
    };}
    public Collection<V> values(){return new AbstractCollection<V>(){
        public int size(){return storage.size();}public boolean contains(Object value){return containsValue(value);}public void clear(){IdentityHashMap.this.clear();}
        public boolean remove(Object value){Iterator<Map.Entry<K,V>> cursor=storage.entrySet().iterator();while(cursor.hasNext())if(cursor.next().getValue()==value){cursor.remove();return true;}return false;}
        public Iterator<V> iterator(){final Iterator<Map.Entry<K,V>> cursor=entrySet().iterator();return new Iterator<V>(){public boolean hasNext(){return cursor.hasNext();}public V next(){return cursor.next().getValue();}public void remove(){cursor.remove();}};}
    };}
    public boolean equals(Object value){if(value==this)return true;if(!(value instanceof Map))return false;return entrySet().equals(((Map<?,?>)value).entrySet());}
    public int hashCode(){int hash=0;for(Map.Entry<K,V> entry:entrySet())hash+=entry.hashCode();return hash;}
    public Object clone(){try{IdentityHashMap<K,V> copy=(IdentityHashMap<K,V>)super.clone();copy.storage=(Storage<K,V>)storage.clone();return copy;}catch(CloneNotSupportedException impossible){throw new InternalError();}}
    private static final class IdentityEntry<K,V> implements Map.Entry<K,V> {
        boolean valid=true;final Map.Entry<K,V> entry;IdentityEntry(Map.Entry<K,V> entry){this.entry=entry;}
        private void check(){if(!valid)throw new IllegalStateException();}public K getKey(){check();return entry.getKey();}public V getValue(){check();return entry.getValue();}public V setValue(V value){check();return entry.setValue(value);}
        public boolean equals(Object value){return value instanceof Map.Entry && getKey()==((Map.Entry<?,?>)value).getKey() && getValue()==((Map.Entry<?,?>)value).getValue();}
        public int hashCode(){return System.identityHashCode(getKey())^System.identityHashCode(getValue());}
        public String toString(){return entry.toString();}
    }
}
