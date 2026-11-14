package java.util;import java.util.function.*;
public class Hashtable<K,V> extends Dictionary<K,V> implements Map<K,V>,Cloneable,java.io.Serializable{
    private Entry<?,?>[] table;
    private int count;
    private int threshold;
    private float loadFactor;
    private int modCount;
    private Set<K> keySet;
    private Set<Map.Entry<K,V>> entrySet;
    private Collection<V> values;

    public Hashtable(int initialCapacity,float loadFactor){
        if(initialCapacity<0)throw new IllegalArgumentException("Illegal Capacity: "+initialCapacity);
        if(loadFactor<=0 || Float.isNaN(loadFactor))throw new IllegalArgumentException("Illegal Load: "+loadFactor);
        if(initialCapacity==0)initialCapacity=1;
        this.loadFactor=loadFactor;
        table=new Entry<?,?>[initialCapacity];
        threshold=(int)Math.min(initialCapacity*loadFactor,Integer.MAX_VALUE-7);
    }
    public Hashtable(int initialCapacity){this(initialCapacity,0.75f);}
    public Hashtable(){this(11,0.75f);}
    public Hashtable(Map<? extends K,? extends V> t){this(Math.max(2*t.size(),11),0.75f);putAll(t);}

    private static int indexFor(int hash,int length){return (hash&0x7FFFFFFF)%length;}
    private Entry<K,V> find(Object key){
        int hash=key.hashCode();
        int index=indexFor(hash,table.length);
        for(Entry<K,V> entry=(Entry<K,V>)table[index];entry!=null;entry=entry.next)
            if(entry.hash==hash && entry.key.equals(key))return entry;
        return null;
    }
    private void addEntry(int hash,K key,V value,int index){
        if(count>=threshold){grow();index=indexFor(hash,table.length);}
        table[index]=new Entry<K,V>(hash,key,value,(Entry<K,V>)table[index]);
        count++;
        modCount++;
    }
    private void grow(){
        int oldCapacity=table.length;
        int newCapacity=oldCapacity*2+1;
        modCount++;
        threshold=(int)Math.min(newCapacity*loadFactor,Integer.MAX_VALUE-7);
        Entry<?,?>[] newTable=new Entry<?,?>[newCapacity];
        for(int i=oldCapacity;i-- >0;){
            Entry<K,V> entry=(Entry<K,V>)table[i];
            while(entry!=null){
                Entry<K,V> next=entry.next;
                int newIndex=indexFor(entry.hash,newCapacity);
                entry.next=(Entry<K,V>)newTable[newIndex];
                newTable[newIndex]=entry;
                entry=next;
            }
        }
        table=newTable;
    }

    public int size(){return count;}
    public boolean isEmpty(){return count==0;}
    public Enumeration<K> keys(){return count==0?Collections.<K>emptyEnumeration():new Enumerator<K>(Enumerator.KEYS,false);}
    public Enumeration<V> elements(){return count==0?Collections.<V>emptyEnumeration():new Enumerator<V>(Enumerator.VALUES,false);}
    public boolean contains(Object value){
        if(value==null)throw new NullPointerException();
        for(int i=table.length;i-- >0;){
            for(Entry<K,V> entry=(Entry<K,V>)table[i];entry!=null;entry=entry.next)
                if(entry.value.equals(value))return true;
        }
        return false;
    }
    public boolean containsValue(Object value){return contains(value);}
    public boolean containsKey(Object key){return find(key)!=null;}
    public V get(Object key){Entry<K,V> entry=find(key);return entry==null?null:entry.value;}
    public V put(K key,V value){
        if(value==null)throw new NullPointerException();
        int hash=key.hashCode();
        int index=indexFor(hash,table.length);
        for(Entry<K,V> entry=(Entry<K,V>)table[index];entry!=null;entry=entry.next)
            if(entry.hash==hash && entry.key.equals(key)){
                V old=entry.value;
                entry.value=value;
                return old;
            }
        addEntry(hash,key,value,index);
        return null;
    }
    public V remove(Object key){
        int hash=key.hashCode();
        int index=indexFor(hash,table.length);
        Entry<K,V> previous=null;
        for(Entry<K,V> entry=(Entry<K,V>)table[index];entry!=null;entry=entry.next){
            if(entry.hash==hash && entry.key.equals(key)){
                if(previous!=null)previous.next=entry.next;else table[index]=entry.next;
                count--;
                modCount++;
                V value=entry.value;
                entry.value=null;
                return value;
            }
            previous=entry;
        }
        return null;
    }
    public void putAll(Map<? extends K,? extends V> t){
        for(Map.Entry<? extends K,? extends V> entry:t.entrySet())put(entry.getKey(),entry.getValue());
    }
    public void clear(){
        for(int i=table.length;i-- >0;)table[i]=null;
        count=0;
        modCount++;
    }
    public Object clone(){
        Hashtable<K,V> copy;
        try{copy=(Hashtable<K,V>)super.clone();}catch(CloneNotSupportedException impossible){throw new InternalError();}
        Entry<?,?>[] newTable=new Entry<?,?>[table.length];
        for(int i=table.length;i-- >0;){
            Entry<K,V> source=(Entry<K,V>)table[i];
            if(source!=null)newTable[i]=source.copy();
        }
        copy.table=newTable;
        copy.keySet=null;
        copy.entrySet=null;
        copy.values=null;
        copy.modCount=0;
        return copy;
    }
    public String toString(){
        int max=size()-1;
        if(max==-1)return "{}";
        StringBuilder out=new StringBuilder("{");
        Iterator<Map.Entry<K,V>> it=entrySet().iterator();
        for(int i=0;;i++){
            Map.Entry<K,V> entry=it.next();
            K key=entry.getKey();
            V value=entry.getValue();
            out.append(key==this?"(this Map)":key.toString()).append('=').append(value==this?"(this Map)":value.toString());
            if(i==max)return out.append('}').toString();
            out.append(", ");
        }
    }
    public boolean equals(Object o){
        if(o==this)return true;
        if(!(o instanceof Map))return false;
        Map<?,?> other=(Map<?,?>)o;
        if(other.size()!=size())return false;
        try{
            for(Map.Entry<K,V> entry:entrySet()){
                K key=entry.getKey();
                V value=entry.getValue();
                if(value==null){
                    if(!(other.get(key)==null && other.containsKey(key)))return false;
                }else if(!value.equals(other.get(key)))return false;
            }
            return true;
        }catch(ClassCastException failure){return false;}
        catch(NullPointerException failure){return false;}
    }
    public int hashCode(){
        int hash=0;
        for(int i=table.length;i-- >0;){
            for(Entry<K,V> entry=(Entry<K,V>)table[i];entry!=null;entry=entry.next)hash+=entry.hashCode();
        }
        return hash;
    }
    public Set<K> keySet(){if(keySet==null)keySet=new KeySet();return keySet;}
    public Collection<V> values(){if(values==null)values=new ValueCollection();return values;}
    public Set<Map.Entry<K,V>> entrySet(){if(entrySet==null)entrySet=new EntrySet();return entrySet;}

    public V getOrDefault(Object key,V defaultValue){V value=get(key);return value==null?defaultValue:value;}
    public void forEach(BiConsumer<? super K,? super V> action){
        Objects.requireNonNull(action);
        int expectedModCount=modCount;
        for(Entry<?,?> slot:table){
            for(Entry<K,V> entry=(Entry<K,V>)slot;entry!=null;entry=entry.next){
                action.accept(entry.key,entry.value);
                if(expectedModCount!=modCount)throw new ConcurrentModificationException();
            }
        }
    }
    public void replaceAll(BiFunction<? super K,? super V,? extends V> function){
        Objects.requireNonNull(function);
        int expectedModCount=modCount;
        for(Entry<?,?> slot:table){
            for(Entry<K,V> entry=(Entry<K,V>)slot;entry!=null;entry=entry.next){
                entry.value=Objects.requireNonNull(function.apply(entry.key,entry.value));
                if(expectedModCount!=modCount)throw new ConcurrentModificationException();
            }
        }
    }
    public V putIfAbsent(K key,V value){
        Objects.requireNonNull(value);
        int hash=key.hashCode();
        int index=indexFor(hash,table.length);
        for(Entry<K,V> entry=(Entry<K,V>)table[index];entry!=null;entry=entry.next)
            if(entry.hash==hash && entry.key.equals(key))return entry.value;
        addEntry(hash,key,value,index);
        return null;
    }
    public boolean remove(Object key,Object value){
        Objects.requireNonNull(value);
        int hash=key.hashCode();
        int index=indexFor(hash,table.length);
        Entry<K,V> previous=null;
        for(Entry<K,V> entry=(Entry<K,V>)table[index];entry!=null;entry=entry.next){
            if(entry.hash==hash && entry.key.equals(key) && entry.value.equals(value)){
                if(previous!=null)previous.next=entry.next;else table[index]=entry.next;
                count--;
                modCount++;
                entry.value=null;
                return true;
            }
            previous=entry;
        }
        return false;
    }
    public boolean replace(K key,V oldValue,V newValue){
        Objects.requireNonNull(oldValue);
        Objects.requireNonNull(newValue);
        int hash=key.hashCode();
        int index=indexFor(hash,table.length);
        for(Entry<K,V> entry=(Entry<K,V>)table[index];entry!=null;entry=entry.next)
            if(entry.hash==hash && entry.key.equals(key) && entry.value.equals(oldValue)){
                entry.value=newValue;
                return true;
            }
        return false;
    }
    public V replace(K key,V value){
        Objects.requireNonNull(value);
        int hash=key.hashCode();
        int index=indexFor(hash,table.length);
        for(Entry<K,V> entry=(Entry<K,V>)table[index];entry!=null;entry=entry.next)
            if(entry.hash==hash && entry.key.equals(key)){
                V old=entry.value;
                entry.value=value;
                return old;
            }
        return null;
    }
    public V computeIfAbsent(K key,Function<? super K,? extends V> mappingFunction){
        Objects.requireNonNull(mappingFunction);
        int hash=key.hashCode();
        int index=indexFor(hash,table.length);
        for(Entry<K,V> entry=(Entry<K,V>)table[index];entry!=null;entry=entry.next)
            if(entry.hash==hash && entry.key.equals(key))return entry.value;
        int expectedModCount=modCount;
        V value=mappingFunction.apply(key);
        if(expectedModCount!=modCount)throw new ConcurrentModificationException();
        if(value!=null)addEntry(hash,key,value,index);
        return value;
    }

    public V computeIfPresent(K key,BiFunction<? super K,? super V,? extends V> remappingFunction){
        Objects.requireNonNull(remappingFunction);
        int hash=key.hashCode();
        int index=indexFor(hash,table.length);
        Entry<K,V> previous=null;
        for(Entry<K,V> entry=(Entry<K,V>)table[index];entry!=null;previous=entry,entry=entry.next)
            if(entry.hash==hash && entry.key.equals(key)){
                int expectedModCount=modCount;
                return remap(index,previous,entry,remappingFunction.apply(key,entry.value),expectedModCount);
            }
        return null;
    }
    public V compute(K key,BiFunction<? super K,? super V,? extends V> remappingFunction){
        Objects.requireNonNull(remappingFunction);
        int hash=key.hashCode();
        int index=indexFor(hash,table.length);
        Entry<K,V> previous=null;
        for(Entry<K,V> entry=(Entry<K,V>)table[index];entry!=null;previous=entry,entry=entry.next)
            if(entry.hash==hash && entry.key.equals(key)){
                int expectedModCount=modCount;
                return remap(index,previous,entry,remappingFunction.apply(key,entry.value),expectedModCount);
            }
        int expectedModCount=modCount;
        V value=remappingFunction.apply(key,null);
        if(expectedModCount!=modCount)throw new ConcurrentModificationException();
        if(value!=null)addEntry(hash,key,value,index);
        return value;
    }
    public V merge(K key,V value,BiFunction<? super V,? super V,? extends V> remappingFunction){
        Objects.requireNonNull(remappingFunction);
        int hash=key.hashCode();
        int index=indexFor(hash,table.length);
        Entry<K,V> previous=null;
        for(Entry<K,V> entry=(Entry<K,V>)table[index];entry!=null;previous=entry,entry=entry.next)
            if(entry.hash==hash && entry.key.equals(key)){
                int expectedModCount=modCount;
                return remap(index,previous,entry,remappingFunction.apply(entry.value,value),expectedModCount);
            }
        if(value!=null)addEntry(hash,key,value,index);
        return value;
    }
    /** Stores a remapped value, or unlinks the entry without clearing its value when the result is null. */
    private V remap(int index,Entry<K,V> previous,Entry<K,V> entry,V newValue,int expectedModCount){
        if(expectedModCount!=modCount)throw new ConcurrentModificationException();
        if(newValue==null){
            if(previous!=null)previous.next=entry.next;else table[index]=entry.next;
            modCount=expectedModCount+1;
            count--;
        }else entry.value=newValue;
        return newValue;
    }

    private static class Entry<K,V> implements Map.Entry<K,V>{
        final int hash;
        final K key;
        V value;
        Entry<K,V> next;
        Entry(int hash,K key,V value,Entry<K,V> next){this.hash=hash;this.key=key;this.value=value;this.next=next;}
        public K getKey(){return key;}
        public V getValue(){return value;}
        public V setValue(V newValue){if(newValue==null)throw new NullPointerException();V old=value;value=newValue;return old;}
        public boolean equals(Object o){
            if(!(o instanceof Map.Entry))return false;
            Map.Entry<?,?> other=(Map.Entry<?,?>)o;
            return Objects.equals(key,other.getKey()) && Objects.equals(value,other.getValue());
        }
        public int hashCode(){return hash^Objects.hashCode(value);}
        public String toString(){return key.toString()+"="+value.toString();}
        Entry<K,V> copy(){return new Entry<K,V>(hash,key,value,next==null?null:next.copy());}
    }

    private class Enumerator<T> implements Enumeration<T>,Iterator<T>{
        private static final int KEYS=0;
        private static final int VALUES=1;
        private static final int ENTRIES=2;
        private final Entry<?,?>[] table;
        private final int type;
        private final boolean iterator;
        private int index;
        private int expectedModCount;
        private Entry<K,V> entry;
        private Entry<K,V> lastReturned;
        Enumerator(int type,boolean iterator){
            this.type=type;
            this.iterator=iterator;
            this.table=Hashtable.this.table;
            this.expectedModCount=modCount;
            this.index=table.length;
        }
        public boolean hasNext(){return hasMoreElements();}
        public boolean hasMoreElements(){
            if(entry!=null)return true;
            while(index>0){
                if(table[--index]!=null){entry=(Entry<K,V>)table[index];return true;}
            }
            return false;
        }
        @SuppressWarnings("unchecked")
        public T next(){
            if(modCount!=expectedModCount)throw new ConcurrentModificationException();
            return nextElement();
        }
        @SuppressWarnings("unchecked")
        public T nextElement(){
            if(entry==null && !hasMoreElements())throw new NoSuchElementException("Hashtable Enumerator");
            lastReturned=entry;
            entry=entry.next;
            return type==KEYS?(T)lastReturned.key:type==VALUES?(T)lastReturned.value:(T)lastReturned;
        }
        public void remove(){
            if(!iterator)throw new UnsupportedOperationException();
            if(lastReturned==null)throw new IllegalStateException("Hashtable Enumerator");
            if(modCount!=expectedModCount)throw new ConcurrentModificationException();
            Entry<K,V> last=lastReturned;
            lastReturned=null;
            Entry<?,?>[] current=Hashtable.this.table;
            int bucket=indexFor(last.hash,current.length);
            Entry<K,V> cursor=(Entry<K,V>)current[bucket];
            Entry<K,V> previous=null;
            while(cursor!=null && cursor!=last){previous=cursor;cursor=cursor.next;}
            if(cursor==null)throw new ConcurrentModificationException();
            if(previous!=null)previous.next=cursor.next;else current[bucket]=cursor.next;
            count--;
            modCount++;
            expectedModCount++;
        }
    }

    private class KeySet extends AbstractSet<K>{
        public Iterator<K> iterator(){return count==0?Collections.<K>emptyIterator():new Enumerator<K>(Enumerator.KEYS,true);}
        public int size(){return count;}
        public boolean contains(Object value){return containsKey(value);}
        public boolean remove(Object value){return Hashtable.this.remove(value)!=null;}
        public void clear(){Hashtable.this.clear();}
    }

    private class ValueCollection extends AbstractCollection<V>{
        public Iterator<V> iterator(){return count==0?Collections.<V>emptyIterator():new Enumerator<V>(Enumerator.VALUES,true);}
        public int size(){return count;}
        public boolean contains(Object value){return containsValue(value);}
        public void clear(){Hashtable.this.clear();}
    }

    private class EntrySet extends AbstractSet<Map.Entry<K,V>>{
        public Iterator<Map.Entry<K,V>> iterator(){return count==0?Collections.<Map.Entry<K,V>>emptyIterator():new Enumerator<Map.Entry<K,V>>(Enumerator.ENTRIES,true);}
        public int size(){return count;}
        public boolean contains(Object value){
            if(!(value instanceof Map.Entry))return false;
            Map.Entry<?,?> other=(Map.Entry<?,?>)value;
            Object key=other.getKey();
            int hash=key.hashCode();
            int index=indexFor(hash,table.length);
            for(Entry<K,V> entry=(Entry<K,V>)table[index];entry!=null;entry=entry.next)
                if(entry.hash==hash && entry.equals(other))return true;
            return false;
        }
        public boolean remove(Object value){
            if(!(value instanceof Map.Entry))return false;
            Map.Entry<?,?> other=(Map.Entry<?,?>)value;
            Object key=other.getKey();
            int hash=key.hashCode();
            int index=indexFor(hash,table.length);
            Entry<K,V> previous=null;
            for(Entry<K,V> entry=(Entry<K,V>)table[index];entry!=null;entry=entry.next){
                if(entry.hash==hash && entry.equals(other)){
                    if(previous!=null)previous.next=entry.next;else table[index]=entry.next;
                    count--;
                    modCount++;
                    entry.value=null;
                    return true;
                }
                previous=entry;
            }
            return false;
        }
        public void clear(){Hashtable.this.clear();}
    }
}
