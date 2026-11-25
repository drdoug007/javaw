package java.util;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * A single-threaded copy of {@code java.util.WeakHashMap} under the no-reclamation contract: entries hold their
 * keys strongly and are never removed by the runtime. Lookup matches by hash, then object identity, then the
 * argument's {@code equals}; iterators are fail-fast and the key/value/entry views are cached.
 */
public class WeakHashMap<K,V> extends AbstractMap<K,V> implements Map<K,V> {
    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    private static final int MAXIMUM_CAPACITY = 1 << 30;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;
    private static final Object NULL_KEY = new Object();

    Entry<K,V>[] table;
    private int size;
    private int threshold;
    private final float loadFactor;
    int modCount;
    private Set<K> keySetView;
    private Collection<V> valuesView;
    private Set<Map.Entry<K,V>> entrySetView;

    public WeakHashMap() {
        this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

    public WeakHashMap(int initialCapacity) {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
    }

    public WeakHashMap(int initialCapacity, float loadFactor) {
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Illegal Initial Capacity: " + initialCapacity);
        if (initialCapacity > MAXIMUM_CAPACITY)
            initialCapacity = MAXIMUM_CAPACITY;
        if (loadFactor <= 0 || Float.isNaN(loadFactor))
            throw new IllegalArgumentException("Illegal Load factor: " + loadFactor);
        this.loadFactor = loadFactor;
        int capacity = 1;
        while (capacity < initialCapacity)
            capacity <<= 1;
        this.table = (Entry<K,V>[]) new Entry<?,?>[capacity];
        this.threshold = (int) (capacity * loadFactor);
    }

    public WeakHashMap(Map<? extends K, ? extends V> m) {
        this(Math.max((int) Math.ceil(m.size() / (double) DEFAULT_LOAD_FACTOR), DEFAULT_INITIAL_CAPACITY), DEFAULT_LOAD_FACTOR);
        putAll(m);
    }

    private static Object maskNull(Object key) {
        return key == null ? NULL_KEY : key;
    }

    static Object unmaskNull(Object key) {
        return key == NULL_KEY ? null : key;
    }

    final int hash(Object k) {
        int h = k.hashCode();
        h ^= (h >>> 20) ^ (h >>> 12);
        return h ^ (h >>> 7) ^ (h >>> 4);
    }

    private static int indexFor(int h, int length) {
        return h & (length - 1);
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public V get(Object key) {
        Object k = maskNull(key);
        int h = hash(k);
        Entry<K,V>[] tab = table;
        int index = indexFor(h, tab.length);
        Entry<K,V> e = tab[index];
        while (e != null) {
            if (e.hash == h && (e.key == k || k.equals(e.key)))
                return e.value;
            e = e.next;
        }
        return null;
    }

    public boolean containsKey(Object key) {
        return getEntry(key) != null;
    }

    Entry<K,V> getEntry(Object key) {
        Object k = maskNull(key);
        int h = hash(k);
        Entry<K,V>[] tab = table;
        int index = indexFor(h, tab.length);
        Entry<K,V> e = tab[index];
        while (e != null && !(e.hash == h && (e.key == k || k.equals(e.key))))
            e = e.next;
        return e;
    }

    public V put(K key, V value) {
        Object k = maskNull(key);
        int h = hash(k);
        Entry<K,V>[] tab = table;
        int i = indexFor(h, tab.length);
        for (Entry<K,V> e = tab[i]; e != null; e = e.next) {
            if (e.hash == h && (e.key == k || k.equals(e.key))) {
                V oldValue = e.value;
                e.value = value;
                return oldValue;
            }
        }
        modCount++;
        tab[i] = new Entry<K,V>(k, value, h, tab[i]);
        if (++size > threshold)
            resize(tab.length * 2);
        return null;
    }

    private void resize(int newCapacity) {
        Entry<K,V>[] oldTable = table;
        int oldCapacity = oldTable.length;
        if (oldCapacity == MAXIMUM_CAPACITY) {
            threshold = Integer.MAX_VALUE;
            return;
        }
        Entry<K,V>[] newTable = (Entry<K,V>[]) new Entry<?,?>[newCapacity];
        threshold = (int) (newCapacity * loadFactor);
        for (int j = 0; j < oldCapacity; j++) {
            Entry<K,V> oldFirst = oldTable[j];
            oldTable[j] = null;
            while (oldFirst != null) {
                Entry<K,V> next = oldFirst.next;
                int i = indexFor(oldFirst.hash, newCapacity);
                oldFirst.next = newTable[i];
                newTable[i] = oldFirst;
                oldFirst = next;
            }
        }
        table = newTable;
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        int n = m.size();
        if (n == 0)
            return;
        int needed = (int) Math.ceil(n / loadFactor);
        if (needed < 0)
            needed = MAXIMUM_CAPACITY;
        int target = 1;
        while (target < needed)
            target <<= 1;
        if (target > table.length)
            resize(target);
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet())
            put(e.getKey(), e.getValue());
    }

    public V remove(Object key) {
        Object k = maskNull(key);
        int h = hash(k);
        Entry<K,V>[] tab = table;
        int i = indexFor(h, tab.length);
        Entry<K,V> prev = tab[i];
        Entry<K,V> e = prev;
        while (e != null) {
            Entry<K,V> next = e.next;
            if (e.hash == h && (e.key == k || k.equals(e.key))) {
                modCount++;
                size--;
                if (prev == e)
                    tab[i] = next;
                else
                    prev.next = next;
                return e.value;
            }
            prev = e;
            e = next;
        }
        return null;
    }

    boolean removeMapping(Object o) {
        if (!(o instanceof Map.Entry))
            return false;
        Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
        Object k = maskNull(entry.getKey());
        int h = hash(k);
        Entry<K,V>[] tab = table;
        int i = indexFor(h, tab.length);
        Entry<K,V> prev = tab[i];
        Entry<K,V> e = prev;
        while (e != null) {
            Entry<K,V> next = e.next;
            if (h == e.hash && e.equals(entry)) {
                modCount++;
                size--;
                if (prev == e)
                    tab[i] = next;
                else
                    prev.next = next;
                return true;
            }
            prev = e;
            e = next;
        }
        return false;
    }

    public void clear() {
        modCount++;
        Arrays.fill(table, null);
        size = 0;
    }

    public boolean containsValue(Object value) {
        Entry<K,V>[] tab = table;
        for (int i = tab.length; i-- > 0; ) {
            for (Entry<K,V> e = tab[i]; e != null; e = e.next) {
                if (value == null ? e.value == null : value.equals(e.value))
                    return true;
            }
        }
        return false;
    }

    public Set<K> keySet() {
        if (keySetView == null)
            keySetView = new KeySet();
        return keySetView;
    }

    public Collection<V> values() {
        if (valuesView == null)
            valuesView = new Values();
        return valuesView;
    }

    public Set<Map.Entry<K,V>> entrySet() {
        if (entrySetView == null)
            entrySetView = new EntrySet();
        return entrySetView;
    }

    public void forEach(BiConsumer<? super K, ? super V> action) {
        Objects.requireNonNull(action);
        int expectedModCount = modCount;
        Entry<K,V>[] tab = table;
        for (Entry<K,V> entry : tab) {
            while (entry != null) {
                action.accept(entry.getKey(), entry.value);
                entry = entry.next;
                if (expectedModCount != modCount)
                    throw new ConcurrentModificationException();
            }
        }
    }

    public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
        Objects.requireNonNull(function);
        int expectedModCount = modCount;
        Entry<K,V>[] tab = table;
        for (Entry<K,V> entry : tab) {
            while (entry != null) {
                entry.value = function.apply(entry.getKey(), entry.value);
                entry = entry.next;
                if (expectedModCount != modCount)
                    throw new ConcurrentModificationException();
            }
        }
    }

    private static class Entry<K,V> implements Map.Entry<K,V> {
        final Object key;
        V value;
        final int hash;
        Entry<K,V> next;

        Entry(Object key, V value, int hash, Entry<K,V> next) {
            this.key = key;
            this.value = value;
            this.hash = hash;
            this.next = next;
        }

        public K getKey() {
            return (K) unmaskNull(key);
        }

        public V getValue() {
            return value;
        }

        public V setValue(V value) {
            V oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?, ?> other = (Map.Entry<?, ?>) o;
            K k1 = getKey();
            Object k2 = other.getKey();
            if (k1 == k2 || (k1 != null && k1.equals(k2))) {
                V v1 = getValue();
                Object v2 = other.getValue();
                if (v1 == v2 || (v1 != null && v1.equals(v2)))
                    return true;
            }
            return false;
        }

        public int hashCode() {
            return Objects.hashCode(getKey()) ^ Objects.hashCode(getValue());
        }

        public String toString() {
            return getKey() + "=" + getValue();
        }
    }

    private abstract class HashIterator<T> implements Iterator<T> {
        int index;
        Entry<K,V> entry;
        Entry<K,V> lastReturned;
        int expectedModCount = modCount;

        HashIterator() {
            index = isEmpty() ? 0 : table.length;
        }

        public boolean hasNext() {
            Entry<K,V> e = entry;
            Entry<K,V>[] t = table;
            while (e == null && index > 0)
                e = t[--index];
            entry = e;
            return e != null;
        }

        Entry<K,V> nextEntry() {
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            if (!hasNext())
                throw new NoSuchElementException();
            lastReturned = entry;
            entry = entry.next;
            return lastReturned;
        }

        public void remove() {
            if (lastReturned == null)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            WeakHashMap.this.remove(lastReturned.getKey());
            expectedModCount = modCount;
            lastReturned = null;
        }
    }

    private class KeyIterator extends HashIterator<K> {
        public K next() {
            return nextEntry().getKey();
        }
    }

    private class ValueIterator extends HashIterator<V> {
        public V next() {
            return nextEntry().getValue();
        }
    }

    private class EntryIterator extends HashIterator<Map.Entry<K,V>> {
        public Map.Entry<K,V> next() {
            return nextEntry();
        }
    }

    private class KeySet extends AbstractSet<K> {
        public Iterator<K> iterator() {
            return new KeyIterator();
        }

        public int size() {
            return WeakHashMap.this.size();
        }

        public boolean contains(Object o) {
            return containsKey(o);
        }

        public boolean remove(Object o) {
            if (containsKey(o)) {
                WeakHashMap.this.remove(o);
                return true;
            }
            return false;
        }

        public void clear() {
            WeakHashMap.this.clear();
        }
    }

    private class Values extends AbstractCollection<V> {
        public Iterator<V> iterator() {
            return new ValueIterator();
        }

        public int size() {
            return WeakHashMap.this.size();
        }

        public boolean contains(Object o) {
            return containsValue(o);
        }

        public void clear() {
            WeakHashMap.this.clear();
        }
    }

    private class EntrySet extends AbstractSet<Map.Entry<K,V>> {
        public Iterator<Map.Entry<K,V>> iterator() {
            return new EntryIterator();
        }

        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?, ?> other = (Map.Entry<?, ?>) o;
            Entry<K,V> e = getEntry(other.getKey());
            return e != null && e.equals(other);
        }

        public boolean remove(Object o) {
            return removeMapping(o);
        }

        public int size() {
            return WeakHashMap.this.size();
        }

        public void clear() {
            WeakHashMap.this.clear();
        }

        public Object[] toArray() {
            return deepCopy().toArray();
        }

        public <T> T[] toArray(T[] a) {
            return deepCopy().toArray(a);
        }

        private List<Map.Entry<K,V>> deepCopy() {
            List<Map.Entry<K,V>> list = new ArrayList<Map.Entry<K,V>>(size());
            for (Map.Entry<K,V> e : this)
                list.add(new AbstractMap.SimpleEntry<K,V>(e));
            return list;
        }
    }
}
