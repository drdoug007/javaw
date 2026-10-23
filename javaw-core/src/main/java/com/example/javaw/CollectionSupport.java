package com.example.javaw;
import com.github.javaparser.ast.CompilationUnit;

final class CollectionSupport {
    static void addTypes(CompilationUnit cu) {
        LibrarySupport.add(cu,"java.util", """
            interface Iterable<T> { public Iterator<T> iterator(); }
            interface Iterator<T> { public boolean hasNext(); public T next(); }
            interface Collection<E> extends Iterable<E> {
                public int size(); public boolean isEmpty(); public boolean add(E value);
                public boolean contains(Object value); public boolean remove(Object value); public void clear();
            }
            interface List<E> extends Collection<E> {
                public E get(int index); public E set(int index,E value); public E remove(int index);
            }
            interface Set<E> extends Collection<E> {}
            interface Map<K,V> {
                public int size(); public boolean isEmpty(); public V put(K key,V value);
                public V get(Object key); public boolean containsKey(Object key);
                public V remove(Object key); public void clear(); public Set<K> keySet();
            }
            class __JavawObjects {
                public static boolean same(Object a,Object b) { return false; }
                public static int hash(Object value) { return 0; }
            }
            class ArrayList<E> implements List<E> {
                private Object[] data;
                private int count;
                public int version;
                public ArrayList() { data = new Object[8]; }
                public ArrayList(int capacity) {
                    if (capacity < 0) throw new IllegalArgumentException();
                    data = new Object[capacity];
                }
                public int size() { return count; }
                public boolean isEmpty() { return count==0; }
                public boolean add(E value) {
                    if (count == data.length) {
                        Object[] next = new Object[count*2+8];
                        for (int i=0;i<count;i++) next[i]=data[i];
                        data=next;
                    }
                    data[count]=value; count++; version++; return true;
                }
                private void check(int index) { if (index<0 || index>=count) throw new IndexOutOfBoundsException(); }
                public E get(int index) { check(index); return (E)data[index]; }
                public E set(int index,E value) { check(index); E old=(E)data[index]; data[index]=value; return old; }
                public E remove(int index) {
                    check(index); E old=(E)data[index];
                    for(int i=index;i<count-1;i++) data[i]=data[i+1];
                    count--; data[count]=null; version++; return old;
                }
                public int indexOf(Object value) {
                    for(int i=0;i<count;i++) if (__JavawObjects.same(value,data[i])) return i;
                    return -1;
                }
                public boolean contains(Object value) { return indexOf(value)>=0; }
                public boolean remove(Object value) {
                    int index=indexOf(value); if(index<0) return false; remove(index); return true;
                }
                public void clear() { for(int i=0;i<count;i++) data[i]=null; count=0; version++; }
                public Iterator<E> iterator() { return new __JavawListIterator<E>(this); }
                public Object[] toArray() {
                    Object[] result=new Object[count]; for(int i=0;i<count;i++) result[i]=data[i]; return result;
                }
            }
            class __JavawListIterator<E> implements Iterator<E> {
                private ArrayList<E> list; private int index; private int expected;
                public __JavawListIterator(ArrayList<E> list) { this.list=list; expected=list.version; }
                public boolean hasNext() { return index<list.size(); }
                public E next() {
                    if(expected!=list.version) throw new ConcurrentModificationException();
                    if (!hasNext()) throw new NoSuchElementException();
                    E result=list.get(index); index++; return result;
                }
            }
            class __JavawMapEntry<K,V> {
                public K key; public V value; public int hash; public __JavawMapEntry<K,V> next;
                public __JavawMapEntry(K key,V value,int hash,__JavawMapEntry<K,V> next) {
                    this.key=key; this.value=value; this.hash=hash; this.next=next;
                }
            }
            class HashMap<K,V> implements Map<K,V> {
                private __JavawMapEntry[] buckets; private int count;
                public int version;
                public HashMap() { buckets=new __JavawMapEntry[32]; }
                public int size() { return count; }
                public boolean isEmpty() { return count==0; }
                private int bucket(int hash) { return hash & (buckets.length-1); }
                private __JavawMapEntry<K,V> entry(Object key,int hash) {
                    __JavawMapEntry<K,V> e=buckets[bucket(hash)];
                    while(e!=null) { if(e.hash==hash && __JavawObjects.same(key,e.key)) return e; e=e.next; }
                    return null;
                }
                public V get(Object key) { var e=entry(key,__JavawObjects.hash(key)); return e==null ? null : e.value; }
                public boolean containsKey(Object key) { return entry(key,__JavawObjects.hash(key))!=null; }
                public V put(K key,V value) {
                    int hash=__JavawObjects.hash(key); var e=entry(key,hash);
                    if(e!=null) { V old=e.value; e.value=value; return old; }
                    int index=bucket(hash);
                    buckets[index]=new __JavawMapEntry<K,V>(key,value,hash,buckets[index]); count++; version++;
                    if(count>buckets.length*3/4) grow();
                    return null;
                }
                private void grow() {
                    __JavawMapEntry[] old=buckets; buckets=new __JavawMapEntry[old.length*2];
                    for(int i=0;i<old.length;i++) {
                        __JavawMapEntry<K,V> e=old[i];
                        while(e!=null) { var next=e.next; int index=bucket(e.hash); e.next=buckets[index]; buckets[index]=e; e=next; }
                    }
                }
                public V remove(Object key) {
                    int hash=__JavawObjects.hash(key); int index=bucket(hash);
                    __JavawMapEntry<K,V> e=buckets[index]; __JavawMapEntry<K,V> previous=null;
                    while(e!=null) {
                        if(e.hash==hash && __JavawObjects.same(key,e.key)) {
                            if(previous==null) buckets[index]=e.next; else previous.next=e.next;
                            count--; version++; return e.value;
                        }
                        previous=e; e=e.next;
                    }
                    return null;
                }
                public void clear() { for(int i=0;i<buckets.length;i++) buckets[i]=null; count=0; version++; }
                public Set<K> keySet() { return new __JavawKeySet<K,V>(this); }
                public Iterator<K> keyIterator() { return new __JavawKeyIterator<K,V>(this,buckets); }
            }
            class __JavawKeyIterator<K,V> implements Iterator<K> {
                private HashMap<K,V> map; private __JavawMapEntry[] buckets;
                private int index; private int expected; private __JavawMapEntry<K,V> current;
                public __JavawKeyIterator(HashMap<K,V> map,__JavawMapEntry[] buckets) {
                    this.map=map; this.buckets=buckets; expected=map.version; seek();
                }
                private void seek() { while(current==null && index<buckets.length) { current=buckets[index]; index++; } }
                public boolean hasNext() { return current!=null; }
                public K next() {
                    if(expected!=map.version) throw new ConcurrentModificationException();
                    if(current==null) throw new NoSuchElementException();
                    K key=current.key; current=current.next; seek(); return key;
                }
            }
            class __JavawKeySet<K,V> implements Set<K> {
                private HashMap<K,V> map;
                public __JavawKeySet(HashMap<K,V> map) { this.map=map; }
                public int size() { return map.size(); }
                public boolean isEmpty() { return map.isEmpty(); }
                public boolean contains(Object key) { return map.containsKey(key); }
                public boolean remove(Object key) { boolean exists=map.containsKey(key); map.remove(key); return exists; }
                public boolean add(K key) { throw new UnsupportedOperationException(); }
                public void clear() { map.clear(); }
                public Iterator<K> iterator() { return map.keyIterator(); }
            }
            class HashSet<E> implements Set<E> {
                private HashMap<E,Boolean> map;
                public HashSet() { map=new HashMap<E,Boolean>(); }
                public int size() { return map.size(); }
                public boolean isEmpty() { return map.isEmpty(); }
                public boolean contains(Object value) { return map.containsKey(value); }
                public boolean add(E value) { boolean exists=map.containsKey(value); map.put(value,true); return !exists; }
                public boolean remove(Object value) { boolean exists=map.containsKey(value); map.remove(value); return exists; }
                public void clear() { map.clear(); }
                public Iterator<E> iterator() { return map.keyIterator(); }
            }
            """);
    }
}
