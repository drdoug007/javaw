package java.util.concurrent;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Single-threaded ConcurrentHashMap: a chained table of 16 slots that doubles at 3/4, with the JDK's null
 * rules, argument-first equals matching, overridable self-calls and weakly consistent iterators (no
 * ConcurrentModificationException). Wasm runs one thread, so there are no locks, CAS or counter cells.
 */
public class ConcurrentHashMap<K,V> extends AbstractMap<K,V> implements ConcurrentMap<K,V>, java.io.Serializable {

    static final class Node<K,V> {
        final int hash;
        final K key;
        V val;
        Node<K,V> next;

        Node(int hash, K key, V val) {
            this.hash = hash;
            this.key = key;
            this.val = val;
        }
    }

    Node<K,V>[] table;
    int count;
    private KeySetView<K,V> keySet;
    private ValuesView<K,V> values;
    private EntrySetView<K,V> entrySet;

    public ConcurrentHashMap() {
        table = newTable(16);
    }

    public ConcurrentHashMap(int initialCapacity) {
        this(initialCapacity, 0.75f, 1);
    }

    public ConcurrentHashMap(Map<? extends K, ? extends V> m) {
        table = newTable(16);
        putAll(m);
    }

    public ConcurrentHashMap(int initialCapacity, float loadFactor) {
        this(initialCapacity, loadFactor, 1);
    }

    public ConcurrentHashMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
        if (!(loadFactor > 0.0f) || initialCapacity < 0 || concurrencyLevel <= 0)
            throw new IllegalArgumentException();
        table = newTable(16);
    }

    @SuppressWarnings("unchecked")
    static <K,V> Node<K,V>[] newTable(int length) {
        return (Node<K,V>[]) new Node[length];
    }

    static int spread(int h) {
        return (h ^ (h >>> 16)) & 0x7fffffff;
    }

    // -- internal helpers (not overridable) ---------------------------------------------------------------

    final Node<K,V> findNode(Object key) {
        int h = spread(key.hashCode());
        Node<K,V>[] tab = table;
        for (Node<K,V> e = tab[h & (tab.length - 1)]; e != null; e = e.next)
            if (e.hash == h && (e.key == key || key.equals(e.key)))
                return e;
        return null;
    }

    /** Appends a new node for a key known to be absent, then resizes at 3/4. */
    final void addNode(int h, K key, V value) {
        Node<K,V>[] tab = table;
        int i = h & (tab.length - 1);
        Node<K,V> node = new Node<K,V>(h, key, value);
        Node<K,V> e = tab[i];
        if (e == null) {
            tab[i] = node;
        } else {
            while (e.next != null) e = e.next;
            e.next = node;
        }
        count++;
        if (count >= tab.length - (tab.length >>> 2))
            resize();
    }

    final void resize() {
        Node<K,V>[] old = table;
        Node<K,V>[] tab = newTable(old.length * 2);
        Node<K,V>[] tails = newTable(tab.length);
        for (int i = 0; i < old.length; i++) {
            Node<K,V> e = old[i];
            while (e != null) {
                Node<K,V> next = e.next;
                e.next = null;
                int j = e.hash & (tab.length - 1);
                if (tails[j] == null) tab[j] = e;
                else tails[j].next = e;
                tails[j] = e;
                e = next;
            }
        }
        table = tab;
    }

    /** Unlinks the given node from its bin; the node's own next is left unchanged. */
    final void unlink(Node<K,V> node) {
        Node<K,V>[] tab = table;
        int i = node.hash & (tab.length - 1);
        Node<K,V> pred = null;
        for (Node<K,V> e = tab[i]; e != null; pred = e, e = e.next) {
            if (e == node) {
                if (pred == null) tab[i] = e.next;
                else pred.next = e.next;
                count--;
                return;
            }
        }
    }

    final V putVal(K key, V value, boolean onlyIfAbsent) {
        if (key == null || value == null) throw new NullPointerException();
        int h = spread(key.hashCode());
        Node<K,V>[] tab = table;
        for (Node<K,V> e = tab[h & (tab.length - 1)]; e != null; e = e.next) {
            if (e.hash == h && (e.key == key || key.equals(e.key))) {
                V old = e.val;
                if (!onlyIfAbsent) e.val = value;
                return old;
            }
        }
        addNode(h, key, value);
        return null;
    }

    final V replaceNode(Object key, V value, Object cv) {
        int h = spread(key.hashCode());
        Node<K,V>[] tab = table;
        int i = h & (tab.length - 1);
        Node<K,V> pred = null;
        for (Node<K,V> e = tab[i]; e != null; pred = e, e = e.next) {
            if (e.hash == h && (e.key == key || key.equals(e.key))) {
                V ev = e.val;
                if (cv == null || cv == ev || cv.equals(ev)) {
                    if (value != null) {
                        e.val = value;
                    } else {
                        if (pred == null) tab[i] = e.next;
                        else pred.next = e.next;
                        count--;
                    }
                    return ev;
                }
                return null;
            }
        }
        return null;
    }

    // -- queries ------------------------------------------------------------------------------------------

    public int size() {
        return count;
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public V get(Object key) {
        Node<K,V> e = findNode(key);
        return e == null ? null : e.val;
    }

    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    public boolean containsValue(Object value) {
        if (value == null) throw new NullPointerException();
        Node<K,V>[] tab = table;
        for (int i = 0; i < tab.length; i++)
            for (Node<K,V> e = tab[i]; e != null; e = e.next) {
                V v = e.val;
                if (v == value || value.equals(v)) return true;
            }
        return false;
    }

    // -- writes -------------------------------------------------------------------------------------------

    public V put(K key, V value) {
        return putVal(key, value, false);
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        if (m == null) throw new NullPointerException();
        for (Map.Entry<? extends K, ? extends V> e : m.entrySet())
            putVal(e.getKey(), e.getValue(), false);
    }

    public V remove(Object key) {
        return replaceNode(key, null, null);
    }

    public void clear() {
        Node<K,V>[] tab = table;
        for (int i = 0; i < tab.length; i++) tab[i] = null;
        count = 0;
    }

    public V putIfAbsent(K key, V value) {
        return putVal(key, value, true);
    }

    public boolean remove(Object key, Object value) {
        if (key == null) throw new NullPointerException();
        return value != null && replaceNode(key, null, value) != null;
    }

    public boolean replace(K key, V oldValue, V newValue) {
        if (key == null || oldValue == null || newValue == null) throw new NullPointerException();
        return replaceNode(key, newValue, oldValue) != null;
    }

    public V replace(K key, V value) {
        if (key == null || value == null) throw new NullPointerException();
        return replaceNode(key, value, null);
    }

    // -- compute family (the function is called at most once) ---------------------------------------------

    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        if (key == null || mappingFunction == null) throw new NullPointerException();
        Node<K,V> e = findNode(key);
        if (e != null) return e.val;
        V v = mappingFunction.apply(key);
        if (v != null) {
            Node<K,V> now = findNode(key);
            if (now != null) now.val = v;
            else addNode(spread(key.hashCode()), key, v);
        }
        return v;
    }

    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (key == null || remappingFunction == null) throw new NullPointerException();
        Node<K,V> e = findNode(key);
        if (e == null) return null;
        V v = remappingFunction.apply(key, e.val);
        Node<K,V> now = findNode(key);
        if (v == null) {
            if (now != null) unlink(now);
        } else if (now != null) {
            now.val = v;
        } else {
            addNode(spread(key.hashCode()), key, v);
        }
        return v;
    }

    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (key == null || remappingFunction == null) throw new NullPointerException();
        Node<K,V> e = findNode(key);
        V v = remappingFunction.apply(key, e == null ? null : e.val);
        Node<K,V> now = findNode(key);
        if (v == null) {
            if (now != null) unlink(now);
        } else if (now != null) {
            now.val = v;
        } else {
            addNode(spread(key.hashCode()), key, v);
        }
        return v;
    }

    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        if (key == null || value == null || remappingFunction == null) throw new NullPointerException();
        Node<K,V> e = findNode(key);
        if (e == null) {
            addNode(spread(key.hashCode()), key, value);
            return value;
        }
        V r = remappingFunction.apply(e.val, value);
        Node<K,V> now = findNode(key);
        if (r == null) {
            if (now != null) unlink(now);
        } else if (now != null) {
            now.val = r;
        } else {
            addNode(spread(key.hashCode()), key, r);
        }
        return r;
    }

    // -- equals, hashCode, toString (walk the table; never the overridable views) --------------------------

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Map)) return false;
        Map<?,?> m = (Map<?,?>) o;
        Node<K,V>[] tab = table;
        for (int i = 0; i < tab.length; i++)
            for (Node<K,V> e = tab[i]; e != null; e = e.next) {
                V val = e.val;
                Object v = m.get(e.key);
                if (v == null || (v != val && !v.equals(val))) return false;
            }
        for (Map.Entry<?,?> entry : m.entrySet()) {
            Object mk = entry.getKey();
            Object mv = entry.getValue();
            if (mk == null || mv == null) return false;
            Object v = get(mk);
            if (v == null || (mv != v && !mv.equals(v))) return false;
        }
        return true;
    }

    public int hashCode() {
        int h = 0;
        Node<K,V>[] tab = table;
        for (int i = 0; i < tab.length; i++)
            for (Node<K,V> e = tab[i]; e != null; e = e.next)
                h += e.key.hashCode() ^ e.val.hashCode();
        return h;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        Node<K,V>[] tab = table;
        for (int i = 0; i < tab.length; i++)
            for (Node<K,V> e = tab[i]; e != null; e = e.next) {
                if (!first) sb.append(", ");
                first = false;
                Object k = e.key;
                Object v = e.val;
                sb.append(k == this ? "(this Map)" : String.valueOf(k));
                sb.append('=');
                sb.append(v == this ? "(this Map)" : String.valueOf(v));
            }
        return sb.append('}').toString();
    }

    // -- views --------------------------------------------------------------------------------------------

    public KeySetView<K,V> keySet() {
        KeySetView<K,V> ks = keySet;
        if (ks == null) keySet = ks = new KeySetView<K,V>(this, null);
        return ks;
    }

    public Collection<V> values() {
        ValuesView<K,V> vs = values;
        if (vs == null) values = vs = new ValuesView<K,V>(this);
        return vs;
    }

    public Set<Map.Entry<K,V>> entrySet() {
        EntrySetView<K,V> es = entrySet;
        if (es == null) entrySet = es = new EntrySetView<K,V>(this);
        return es;
    }

    abstract static class CollectionView<K,V,E> implements Collection<E>, java.io.Serializable {
        final ConcurrentHashMap<K,V> map;

        CollectionView(ConcurrentHashMap<K,V> map) {
            this.map = map;
        }

        public ConcurrentHashMap<K,V> getMap() {
            return map;
        }

        public final void clear() {
            map.clear();
        }

        public final int size() {
            return map.size();
        }

        public final boolean isEmpty() {
            return map.isEmpty();
        }

        public abstract Iterator<E> iterator();

        public abstract boolean contains(Object o);

        public abstract boolean remove(Object o);

        public final Object[] toArray() {
            int n = map.count;
            Object[] r = new Object[n];
            int i = 0;
            Iterator<E> it = iterator();
            while (i < n && it.hasNext()) r[i++] = it.next();
            return i == n ? r : Arrays.copyOf(r, i);
        }

        @SuppressWarnings("unchecked")
        public final <T> T[] toArray(T[] a) {
            if (a == null) throw new NullPointerException();
            int n = map.count;
            T[] r = a.length >= n ? a : Arrays.copyOf(a, n);
            int i = 0;
            Iterator<E> it = iterator();
            while (i < n && it.hasNext()) r[i++] = (T) it.next();
            if (r == a && a.length > i) a[i] = null;
            return r;
        }

        public final String toString() {
            StringBuilder sb = new StringBuilder("[");
            Iterator<E> it = iterator();
            while (it.hasNext()) {
                Object e = it.next();
                sb.append(e == this ? "(this Collection)" : String.valueOf(e));
                if (it.hasNext()) sb.append(", ");
            }
            return sb.append(']').toString();
        }

        public final boolean containsAll(Collection<?> c) {
            if (c != this) {
                for (Object e : c)
                    if (e == null || !contains(e)) return false;
            }
            return true;
        }

        public boolean removeAll(Collection<?> c) {
            if (c == null) throw new NullPointerException();
            boolean modified = false;
            if (c instanceof Set && c.size() > map.table.length) {
                for (Iterator<E> it = iterator(); it.hasNext(); ) {
                    if (c.contains(it.next())) {
                        it.remove();
                        modified = true;
                    }
                }
            } else {
                for (Object e : c) modified |= remove(e);
            }
            return modified;
        }

        public final boolean retainAll(Collection<?> c) {
            if (c == null) throw new NullPointerException();
            boolean modified = false;
            for (Iterator<E> it = iterator(); it.hasNext(); ) {
                if (!c.contains(it.next())) {
                    it.remove();
                    modified = true;
                }
            }
            return modified;
        }
    }

    public static final class KeySetView<K,V> extends CollectionView<K,V,K> implements Set<K>, java.io.Serializable {
        private final V value;

        KeySetView(ConcurrentHashMap<K,V> map, V value) {
            super(map);
            this.value = value;
        }

        public boolean contains(Object o) {
            return map.containsKey(o);
        }

        public boolean remove(Object o) {
            return map.remove(o) != null;
        }

        public Iterator<K> iterator() {
            return new KeyIterator<K,V>(map);
        }

        public boolean add(K e) {
            V v = value;
            if (v == null) throw new UnsupportedOperationException();
            return map.putVal(e, v, true) == null;
        }

        public boolean addAll(Collection<? extends K> c) {
            V v = value;
            if (v == null) throw new UnsupportedOperationException();
            boolean added = false;
            for (K e : c)
                if (map.putVal(e, v, true) == null) added = true;
            return added;
        }

        public int hashCode() {
            int h = 0;
            for (K e : this) h += e.hashCode();
            return h;
        }

        public boolean equals(Object o) {
            if (!(o instanceof Set)) return false;
            Set<?> c = (Set<?>) o;
            return c == this || (containsAll(c) && c.containsAll(this));
        }
    }

    static final class ValuesView<K,V> extends CollectionView<K,V,V> implements java.io.Serializable {
        ValuesView(ConcurrentHashMap<K,V> map) {
            super(map);
        }

        public boolean contains(Object o) {
            return map.containsValue(o);
        }

        public boolean remove(Object o) {
            if (o != null) {
                for (Iterator<V> it = iterator(); it.hasNext(); ) {
                    if (o.equals(it.next())) {
                        it.remove();
                        return true;
                    }
                }
            }
            return false;
        }

        public Iterator<V> iterator() {
            return new ValueIterator<K,V>(map);
        }

        public boolean add(V e) {
            throw new UnsupportedOperationException();
        }

        public boolean addAll(Collection<? extends V> c) {
            throw new UnsupportedOperationException();
        }

        public boolean removeAll(Collection<?> c) {
            if (c == null) throw new NullPointerException();
            boolean modified = false;
            for (Iterator<V> it = iterator(); it.hasNext(); ) {
                if (c.contains(it.next())) {
                    it.remove();
                    modified = true;
                }
            }
            return modified;
        }
    }

    static final class EntrySetView<K,V> extends CollectionView<K,V,Map.Entry<K,V>>
            implements Set<Map.Entry<K,V>>, java.io.Serializable {
        EntrySetView(ConcurrentHashMap<K,V> map) {
            super(map);
        }

        public boolean contains(Object o) {
            if (!(o instanceof Map.Entry)) return false;
            Map.Entry<?,?> e = (Map.Entry<?,?>) o;
            Object k = e.getKey();
            if (k == null) return false;
            Object r = map.get(k);
            if (r == null) return false;
            Object v = e.getValue();
            return v != null && (v == r || v.equals(r));
        }

        public boolean remove(Object o) {
            if (!(o instanceof Map.Entry)) return false;
            Map.Entry<?,?> e = (Map.Entry<?,?>) o;
            Object k = e.getKey();
            Object v = e.getValue();
            return k != null && v != null && map.remove(k, v);
        }

        public Iterator<Map.Entry<K,V>> iterator() {
            return new EntryIterator<K,V>(map);
        }

        public boolean add(Map.Entry<K,V> e) {
            return map.putVal(e.getKey(), e.getValue(), false) == null;
        }

        public boolean addAll(Collection<? extends Map.Entry<K,V>> c) {
            boolean added = false;
            for (Map.Entry<K,V> e : c)
                if (add(e)) added = true;
            return added;
        }

        public int hashCode() {
            int h = 0;
            Node<K,V>[] tab = map.table;
            for (int i = 0; i < tab.length; i++)
                for (Node<K,V> e = tab[i]; e != null; e = e.next)
                    h += e.key.hashCode() ^ e.val.hashCode();
            return h;
        }

        public boolean equals(Object o) {
            if (!(o instanceof Set)) return false;
            Set<?> c = (Set<?>) o;
            return c == this || (containsAll(c) && c.containsAll(this));
        }
    }

    // -- iterators (weakly consistent: one node preloaded, later slots read live) --------------------------

    abstract static class BaseIterator<K,V,T> implements Iterator<T> {
        final ConcurrentHashMap<K,V> map;
        final Node<K,V>[] tab;
        int index;
        Node<K,V> next;
        Node<K,V> lastReturned;

        BaseIterator(ConcurrentHashMap<K,V> map) {
            this.map = map;
            this.tab = map.table;
            next = firstFrom(0);
        }

        final Node<K,V> firstFrom(int i) {
            for (index = i; index < tab.length; index++)
                if (tab[index] != null) return tab[index];
            return null;
        }

        public final boolean hasNext() {
            return next != null;
        }

        public final T next() {
            Node<K,V> e = next;
            if (e == null) throw new NoSuchElementException();
            lastReturned = e;
            T result = extract(e);
            next = e.next != null ? e.next : firstFrom(index + 1);
            return result;
        }

        abstract T extract(Node<K,V> e);

        public final void remove() {
            Node<K,V> p = lastReturned;
            if (p == null) throw new IllegalStateException();
            lastReturned = null;
            map.replaceNode(p.key, null, null);
        }
    }

    static final class KeyIterator<K,V> extends BaseIterator<K,V,K> {
        KeyIterator(ConcurrentHashMap<K,V> map) {
            super(map);
        }

        K extract(Node<K,V> e) {
            return e.key;
        }
    }

    static final class ValueIterator<K,V> extends BaseIterator<K,V,V> {
        ValueIterator(ConcurrentHashMap<K,V> map) {
            super(map);
        }

        V extract(Node<K,V> e) {
            return e.val;
        }
    }

    static final class EntryIterator<K,V> extends BaseIterator<K,V,Map.Entry<K,V>> {
        EntryIterator(ConcurrentHashMap<K,V> map) {
            super(map);
        }

        Map.Entry<K,V> extract(Node<K,V> e) {
            return new MapEntry<K,V>(e.key, e.val, map);
        }
    }

    static final class MapEntry<K,V> implements Map.Entry<K,V> {
        final K key;
        V val;
        final ConcurrentHashMap<K,V> map;

        MapEntry(K key, V val, ConcurrentHashMap<K,V> map) {
            this.key = key;
            this.val = val;
            this.map = map;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return val;
        }

        public int hashCode() {
            return key.hashCode() ^ val.hashCode();
        }

        public String toString() {
            return key + "=" + val;
        }

        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry)) return false;
            Map.Entry<?,?> e = (Map.Entry<?,?>) o;
            Object k = e.getKey();
            Object v = e.getValue();
            return k != null && v != null && (k == key || k.equals(key)) && (v == val || v.equals(val));
        }

        public V setValue(V value) {
            if (value == null) throw new NullPointerException();
            V v = val;
            val = value;
            map.put(key, value);
            return v;
        }
    }
}
