package com.example.javaw;

import com.github.javaparser.ast.CompilationUnit;

/** Live collection views backed by ordinary Wasm objects. */
final class CollectionViews {
    static void addTypes(CompilationUnit cu) {
        LibrarySupport.add(cu,"java.util", """
            class __JavawSubList<E> implements List<E> {
                private ArrayList<E> root; private __JavawSubList<E> parent;
                private int offset; private int count; private int expected;
                public __JavawSubList(ArrayList<E> root,__JavawSubList<E> parent,int from,int to) {
                    int size=parent==null ? root.size() : parent.size();
                    if(from<0 || to>size) throw new IndexOutOfBoundsException();
                    if(from>to) throw new IllegalArgumentException();
                    this.root=root; this.parent=parent; offset=parent==null ? from : parent.offset+from;
                    count=to-from; expected=root.version;
                }
                private void check() { if(expected!=root.version) throw new ConcurrentModificationException(); }
                private void index(int i) { check(); if(i<0 || i>=count) throw new IndexOutOfBoundsException(); }
                private void changed(int delta) {
                    __JavawSubList<E> view=this;
                    while(view!=null) { view.count+=delta; view.expected=root.version; view=view.parent; }
                }
                public int size() { check(); return count; }
                public boolean isEmpty() { return size()==0; }
                public E get(int i) { index(i); return root.get(offset+i); }
                public E set(int i,E value) { index(i); return root.set(offset+i,value); }
                public boolean add(E value) { add(size(),value); return true; }
                public void add(int i,E value) { check(); if(i<0 || i>count) throw new IndexOutOfBoundsException(); root.add(offset+i,value); changed(1); }
                public E remove(int i) { index(i); E old=root.remove(offset+i); changed(-1); return old; }
                public boolean contains(Object value) { return indexOf(value)>=0; }
                public boolean remove(Object value) { int i=indexOf(value); if(i<0) return false; remove(i); return true; }
                public void clear() { check(); while(count>0) remove(count-1); }
                public List<E> subList(int from,int to) { check(); return new __JavawSubList<E>(root,this,from,to); }
                public Iterator<E> iterator() { check(); return new __JavawListIterator<E>(this,root); }
                public Stream<E> stream() { return Stream.fromCollection(this); }
            }
            class __JavawMapValueIterator<K,V> implements Iterator<V> {
                private HashMap<K,V> map; private Iterator<K> keys;
                public __JavawMapValueIterator(HashMap<K,V> map) { this.map=map; keys=map.keyIterator(); }
                public boolean hasNext() { return keys.hasNext(); }
                public V next() { return map.get(keys.next()); }
                public void remove() { keys.remove(); }
            }
            class __JavawMapValues<K,V> implements Collection<V> {
                private HashMap<K,V> map;
                public __JavawMapValues(HashMap<K,V> map) { this.map=map; }
                public int size() { return map.size(); }
                public boolean isEmpty() { return map.isEmpty(); }
                public void clear() { map.clear(); }
                public boolean add(V value) { throw new UnsupportedOperationException(); }
                public Iterator<V> iterator() { return new __JavawMapValueIterator<K,V>(map); }
                public boolean contains(Object value) { Iterator<V> it=iterator(); while(it.hasNext()) if(__JavawObjects.same(value,it.next())) return true; return false; }
                public boolean remove(Object value) { Iterator<V> it=iterator(); while(it.hasNext()) if(__JavawObjects.same(value,it.next())) { it.remove(); return true; } return false; }
                public Stream<V> stream() { return Stream.fromCollection(this); }
            }
            class __JavawEntryIterator<K,V> implements Iterator<Entry<K,V>> {
                private HashMap<K,V> map; private Iterator<K> keys;
                public __JavawEntryIterator(HashMap<K,V> map) { this.map=map; keys=map.keyIterator(); }
                public boolean hasNext() { return keys.hasNext(); }
                public Entry<K,V> next() { return map.entryForKey(keys.next()); }
                public void remove() { keys.remove(); }
            }
            class __JavawEntrySet<K,V> implements Set<Entry<K,V>> {
                private HashMap<K,V> map;
                public __JavawEntrySet(HashMap<K,V> map) { this.map=map; }
                public int size() { return map.size(); }
                public boolean isEmpty() { return map.isEmpty(); }
                public void clear() { map.clear(); }
                public boolean add(Entry<K,V> value) { throw new UnsupportedOperationException(); }
                public boolean contains(Object value) {
                    if(!(value instanceof Entry)) return false; Entry entry=(Entry)value;
                    return map.containsKey(entry.getKey()) && __JavawObjects.same(map.get(entry.getKey()),entry.getValue());
                }
                public boolean remove(Object value) { if(!contains(value)) return false; Entry entry=(Entry)value; map.remove(entry.getKey()); return true; }
                public Iterator<Entry<K,V>> iterator() { return new __JavawEntryIterator<K,V>(map); }
                public Stream<Entry<K,V>> stream() { return Stream.fromCollection(this); }
            }
            class __JavawReadOnlyIterator<E> implements Iterator<E> {
                private Iterator<E> source;
                public __JavawReadOnlyIterator(Iterator<E> source) { this.source=source; }
                public boolean hasNext() { return source.hasNext(); }
                public E next() { return source.next(); }
            }
            """);
    }
}
