package java.util;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Map;
import java.util.Set;
import java.util.Collection;

public class EnumMap<K extends Enum<K>, V> extends AbstractMap<K, V> implements java.io.Serializable, Cloneable {
    private final Class<K> keyType;
    private Enum<?>[] keyUniverse;
    private Object[] vals;
    private int size;
    private Set<K> keySet;
    private Collection<V> values;
    private Set<Map.Entry<K, V>> entrySet;

    private static final Object NULL = new Object() {
        public int hashCode() { return 0; }
        public String toString() { return "java.util.EnumMap.NULL"; }
    };

    private Object maskNull(Object value) { return value == null ? NULL : value; }
    @SuppressWarnings("unchecked")
    private V unmaskNull(Object value) { return (V) (value == NULL ? null : value); }

    public EnumMap(Class<K> keyType) {
        this.keyType = keyType;
        this.keyUniverse = keyType.getEnumConstants();
        this.vals = new Object[keyUniverse.length];
    }

    public EnumMap(EnumMap<K, ? extends V> m) {
        this.keyType = m.keyType;
        this.keyUniverse = m.keyUniverse;
        this.size = m.size;
        this.vals = m.vals.clone();
    }

    public EnumMap(Map<K, ? extends V> m) {
        if (m instanceof EnumMap) {
            EnumMap<K, ? extends V> em = (EnumMap<K, ? extends V>) m;
            this.keyType = em.keyType;
            this.keyUniverse = em.keyUniverse;
            this.size = em.size;
            this.vals = em.vals.clone();
        } else {
            if (m.isEmpty()) throw new IllegalArgumentException("Specified map is empty");
            this.keyType = m.keySet().iterator().next().getDeclaringClass();
            this.keyUniverse = keyType.getEnumConstants();
            this.vals = new Object[keyUniverse.length];
            putAll(m);
        }
    }

    public int size() { return size; }

    public boolean containsValue(Object value) {
        Object masked = maskNull(value);
        for (Object val : vals) if (masked.equals(val)) return true;
        return false;
    }

    public boolean containsKey(Object key) {
        return isValidKey(key) && vals[((Enum<?>) key).ordinal()] != null;
    }

    public V get(Object key) {
        return isValidKey(key) ? unmaskNull(vals[((Enum<?>) key).ordinal()]) : null;
    }

    public V put(K key, V value) {
        typeCheck(key);
        int index = key.ordinal();
        Object oldValue = vals[index];
        vals[index] = maskNull(value);
        if (oldValue == null) size++;
        return unmaskNull(oldValue);
    }

    public V remove(Object key) {
        if (!isValidKey(key)) return null;
        int index = ((Enum<?>) key).ordinal();
        Object oldValue = vals[index];
        vals[index] = null;
        if (oldValue != null) size--;
        return unmaskNull(oldValue);
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        if (m instanceof EnumMap) {
            EnumMap<?, ?> em = (EnumMap<?, ?>) m;
            if (em.keyType != keyType) {
                if (em.isEmpty()) return;
                throw new ClassCastException(em.keyType + " != " + keyType);
            }
            for (int i = 0; i < em.vals.length; i++) {
                if (em.vals[i] != null) {
                    if (vals[i] == null) size++;
                    vals[i] = em.vals[i];
                }
            }
        } else {
            super.putAll(m);
        }
    }

    public void clear() {
        Arrays.fill(vals, null);
        size = 0;
    }

    private boolean containsMapping(Object key, Object value) {
        return isValidKey(key) && maskNull(value).equals(vals[((Enum<?>) key).ordinal()]);
    }

    private boolean removeMapping(Object key, Object value) {
        if (!isValidKey(key)) return false;
        int index = ((Enum<?>) key).ordinal();
        if (maskNull(value).equals(vals[index])) {
            vals[index] = null;
            size--;
            return true;
        }
        return false;
    }

    private boolean isValidKey(Object key) {
        if (key == null) return false;
        Class<?> keyClass = key.getClass();
        return keyClass == keyType || keyClass.getSuperclass() == keyType;
    }

    private void typeCheck(K key) {
        Class<?> keyClass = key.getClass();
        if (keyClass != keyType && keyClass.getSuperclass() != keyType)
            throw new ClassCastException(keyClass + " != " + keyType);
    }

    private int entryHashCode(int index) {
        return keyUniverse[index].hashCode() ^ vals[index].hashCode();
    }

    private boolean equals(EnumMap<?, ?> em) {
        if (em.size != size) return false;
        if (em.keyType != keyType) return size == 0;
        for (int i = 0; i < vals.length; i++) {
            Object ourVal = vals[i];
            Object hisVal = em.vals[i];
            if (!(hisVal == ourVal || (hisVal != null && hisVal.equals(ourVal)))) return false;
        }
        return true;
    }

    public Set<K> keySet() {
        if (keySet == null) keySet = new KeySet();
        return keySet;
    }

    public Collection<V> values() {
        if (values == null) values = new Values();
        return values;
    }

    public Set<Map.Entry<K, V>> entrySet() {
        if (entrySet == null) entrySet = new EntrySet();
        return entrySet;
    }

    private class KeySet extends AbstractSet<K> {
        public int size() { return EnumMap.this.size; }
        public boolean contains(Object o) { return EnumMap.this.containsKey(o); }
        public void clear() { EnumMap.this.clear(); }
        public Iterator<K> iterator() { return new KeyIterator(); }
        public boolean remove(Object o) {
            int oldSize = EnumMap.this.size;
            EnumMap.this.remove(o);
            return EnumMap.this.size != oldSize;
        }
    }

    private class Values extends AbstractCollection<V> {
        public int size() { return EnumMap.this.size; }
        public boolean contains(Object o) { return EnumMap.this.containsValue(o); }
        public void clear() { EnumMap.this.clear(); }
        public Iterator<V> iterator() { return new ValueIterator(); }
        public boolean remove(Object o) {
            Object masked = maskNull(o);
            for (int i = 0; i < vals.length; i++) {
                if (masked.equals(vals[i])) {
                    vals[i] = null;
                    size--;
                    return true;
                }
            }
            return false;
        }
    }

    private class EntrySet extends AbstractSet<Map.Entry<K, V>> {
        public int size() { return EnumMap.this.size; }
        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry)) return false;
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
            return EnumMap.this.containsMapping(entry.getKey(), entry.getValue());
        }
        public boolean remove(Object o) {
            if (!(o instanceof Map.Entry)) return false;
            Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
            return EnumMap.this.removeMapping(entry.getKey(), entry.getValue());
        }
        public void clear() { EnumMap.this.clear(); }
        public Iterator<Map.Entry<K, V>> iterator() { return new EntryIterator(); }
    }

    abstract class EnumMapIterator<T> implements Iterator<T> {
        int index = 0;
        int lastReturnedIndex = -1;
        public boolean hasNext() {
            while (index < vals.length && vals[index] == null) index++;
            return index != vals.length;
        }
        public void remove() {
            if (lastReturnedIndex < 0) throw new IllegalStateException();
            if (vals[lastReturnedIndex] != null) {
                vals[lastReturnedIndex] = null;
                size--;
            }
            lastReturnedIndex = -1;
        }
    }

    class KeyIterator extends EnumMapIterator<K> {
        public K next() {
            if (!hasNext()) throw new NoSuchElementException();
            lastReturnedIndex = index++;
            return (K) keyUniverse[lastReturnedIndex];
        }
    }

    class ValueIterator extends EnumMapIterator<V> {
        public V next() {
            if (!hasNext()) throw new NoSuchElementException();
            lastReturnedIndex = index++;
            return unmaskNull(vals[lastReturnedIndex]);
        }
    }

    class EntryIterator extends EnumMapIterator<Map.Entry<K, V>> {
        private MapEntry lastReturnedEntry;
        public Map.Entry<K, V> next() {
            if (!hasNext()) throw new NoSuchElementException();
            lastReturnedIndex = index++;
            lastReturnedEntry = new MapEntry(lastReturnedIndex);
            return lastReturnedEntry;
        }
        public void remove() {
            lastReturnedIndex = lastReturnedEntry == null ? -1 : lastReturnedEntry.index;
            super.remove();
            if (lastReturnedEntry != null) {
                lastReturnedEntry.index = lastReturnedIndex;
                lastReturnedEntry = null;
            }
        }
    }

    class MapEntry implements Map.Entry<K, V> {
        private int index;
        MapEntry(int index) { this.index = index; }
        private void checkIndexForEntryUse() {
            if (index < 0) throw new IllegalStateException("Entry was removed");
        }
        public K getKey() {
            checkIndexForEntryUse();
            return (K) keyUniverse[index];
        }
        public V getValue() {
            checkIndexForEntryUse();
            return unmaskNull(vals[index]);
        }
        public V setValue(V value) {
            checkIndexForEntryUse();
            V oldValue = unmaskNull(vals[index]);
            vals[index] = maskNull(value);
            return oldValue;
        }
        public boolean equals(Object o) {
            if (index < 0) return o == this;
            if (!(o instanceof Map.Entry)) return false;
            Map.Entry<?, ?> e = (Map.Entry<?, ?>) o;
            V ourValue = unmaskNull(vals[index]);
            Object hisValue = e.getValue();
            return e.getKey() == keyUniverse[index] && (ourValue == hisValue || (ourValue != null && ourValue.equals(hisValue)));
        }
        public int hashCode() {
            return index < 0 ? super.hashCode() : entryHashCode(index);
        }
        public String toString() {
            return index < 0 ? super.toString() : keyUniverse[index] + "=" + unmaskNull(vals[index]);
        }
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof EnumMap) return equals((EnumMap<?, ?>) o);
        if (!(o instanceof Map)) return false;
        Map<?, ?> m = (Map<?, ?>) o;
        if (m.size() != size) return false;
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] != null) {
                Object key = keyUniverse[i];
                V value = unmaskNull(vals[i]);
                if (value == null) {
                    if (m.get(key) != null || !m.containsKey(key)) return false;
                } else if (!value.equals(m.get(key))) {
                    return false;
                }
            }
        }
        return true;
    }

    public int hashCode() {
        int hash = 0;
        for (int i = 0; i < vals.length; i++) {
            if (vals[i] != null) hash += entryHashCode(i);
        }
        return hash;
    }

    public EnumMap<K, V> clone() {
        EnumMap<K, V> result;
        try {
            result = (EnumMap<K, V>) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError(e);
        }
        result.vals = result.vals.clone();
        result.keySet = null;
        result.values = null;
        result.entrySet = null;
        return result;
    }
}
