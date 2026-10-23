package com.example.javaw;
import com.github.javaparser.ast.CompilationUnit;

final class CollectionSupport {
    static void addTypes(CompilationUnit cu) {
        LibrarySupport.add(cu,"java.util", """
            interface Iterable<T> {
                public Iterator<T> iterator();
                public default void forEach(Consumer<T> action) {
                    if(action==null) throw new NullPointerException(); Iterator<T> it=iterator(); while(it.hasNext()) action.accept(it.next());
                }
            }
            interface Iterator<T> {
                public boolean hasNext(); public T next();
                public default void remove() { throw new UnsupportedOperationException(); }
                public default void forEachRemaining(Consumer<T> action) {
                    if(action==null) throw new NullPointerException(); while(hasNext()) action.accept(next());
                }
            }
            interface Collection<E> extends Iterable<E> {
                public int size(); public boolean isEmpty(); public boolean add(E value);
                public boolean contains(Object value); public boolean remove(Object value); public void clear();
                public Stream<E> stream();
                public default boolean containsAll(Collection<?> other) {
                    if(other==null) throw new NullPointerException(); Iterator<?> it=other.iterator(); while(it.hasNext()) if(!contains(it.next())) return false; return true;
                }
                public default boolean addAll(Collection<? extends E> other) {
                    if(other==null) throw new NullPointerException(); Object[] snapshot=other.toArray(); boolean changed=false;
                    for(int i=0;i<snapshot.length;i++) if(add((E)snapshot[i])) changed=true; return changed;
                }
                public default boolean removeAll(Collection<?> other) {
                    if(other==null) throw new NullPointerException(); boolean changed=false; Iterator<E> it=iterator();
                    while(it.hasNext()) if(other.contains(it.next())) { it.remove(); changed=true; } return changed;
                }
                public default boolean retainAll(Collection<?> other) {
                    if(other==null) throw new NullPointerException(); boolean changed=false; Iterator<E> it=iterator();
                    while(it.hasNext()) if(!other.contains(it.next())) { it.remove(); changed=true; } return changed;
                }
                public default boolean removeIf(Predicate<E> predicate) {
                    if(predicate==null) throw new NullPointerException(); boolean changed=false; Iterator<E> it=iterator();
                    while(it.hasNext()) if(predicate.test(it.next())) { it.remove(); changed=true; } return changed;
                }
                public default Object[] toArray() {
                    Object[] values=new Object[size()]; Iterator<E> it=iterator(); int i=0;
                    while(it.hasNext()) { values[i]=it.next(); i++; } return values;
                }
            }
            interface List<E> extends Collection<E> {
                public E get(int index); public E set(int index,E value); public E remove(int index);
                public void add(int index,E value); public List<E> subList(int from,int to);
                public default int indexOf(Object value) { for(int i=0;i<size();i++) if(__JavawObjects.same(value,get(i))) return i; return -1; }
                public default int lastIndexOf(Object value) { for(int i=size()-1;i>=0;i--) if(__JavawObjects.same(value,get(i))) return i; return -1; }
                public default boolean addAll(int index,Collection<? extends E> other) {
                    if(index<0 || index>size()) throw new IndexOutOfBoundsException();
                    if(other==null) throw new NullPointerException(); Object[] values=other.toArray();
                    for(int i=0;i<values.length;i++) add(index+i,(E)values[i]); return values.length>0;
                }
                public default void replaceAll(UnaryOperator<E> operator) { if(operator==null) throw new NullPointerException(); for(int i=0;i<size();i++) set(i,operator.apply(get(i))); }
                public default void sort(Comparator<E> comparator) {
                    if(comparator==null) comparator=new __JavawNaturalComparator<E>();
                    for(int i=1;i<size();i++) { E value=get(i); int j=i; while(j>0 && comparator.compare(get(j-1),value)>0) { set(j,get(j-1)); j--; } set(j,value); }
                }
            }
            interface Set<E> extends Collection<E> {}
            interface Map<K,V> {
                interface Entry<K,V> { public K getKey(); public V getValue(); public V setValue(V value); }
                public int size(); public boolean isEmpty(); public V put(K key,V value);
                public V get(Object key); public boolean containsKey(Object key);
                public V remove(Object key); public void clear(); public Set<K> keySet();
                public Collection<V> values(); public Set<Entry<K,V>> entrySet();
                public default boolean containsValue(Object value) { return values().contains(value); }
                public default V getOrDefault(Object key,V fallback) { V value=get(key); return value!=null || containsKey(key) ? value : fallback; }
                public default void putAll(Map<? extends K,? extends V> other) {
                    if(other==null) throw new NullPointerException(); Iterator<?> it=other.entrySet().iterator();
                    while(it.hasNext()) { Entry entry=(Entry)it.next(); put((K)entry.getKey(),(V)entry.getValue()); }
                }
                public default V putIfAbsent(K key,V value) { V old=get(key); if(old==null) put(key,value); return old; }
                public default boolean remove(Object key,Object value) { if(!containsKey(key) || !__JavawObjects.same(value,get(key))) return false; remove(key); return true; }
                public default V replace(K key,V value) { if(!containsKey(key)) return null; return put(key,value); }
                public default boolean replace(K key,V old,V value) { if(!containsKey(key) || !__JavawObjects.same(old,get(key))) return false; put(key,value); return true; }
                public default void forEach(BiConsumer<K,V> action) {
                    if(action==null) throw new NullPointerException(); Iterator<K> it=keySet().iterator(); while(it.hasNext()) { K key=it.next(); action.accept(key,get(key)); }
                }
                public default void replaceAll(BiFunction<K,V,V> function) {
                    if(function==null) throw new NullPointerException(); Iterator<K> it=keySet().iterator(); while(it.hasNext()) { K key=it.next(); put(key,function.apply(key,get(key))); }
                }
                public default V computeIfAbsent(K key,Function<K,V> function) {
                    if(function==null) throw new NullPointerException(); V old=get(key); if(old!=null) return old;
                    V value=function.apply(key); if(value!=null) put(key,value); return value;
                }
                public default V computeIfPresent(K key,BiFunction<K,V,V> function) {
                    if(function==null) throw new NullPointerException(); V old=get(key); if(old==null) return null;
                    V value=function.apply(key,old); if(value==null) remove(key); else put(key,value); return value;
                }
                public default V compute(K key,BiFunction<K,V,V> function) {
                    if(function==null) throw new NullPointerException(); V value=function.apply(key,get(key)); if(value==null) remove(key); else put(key,value); return value;
                }
                public default V merge(K key,V value,BiFunction<V,V,V> function) {
                    if(function==null || value==null) throw new NullPointerException(); V old=get(key);
                    V result=old==null ? value : function.apply(old,value); if(result==null) remove(key); else put(key,result); return result;
                }
            }
            class __JavawObjects {
                public static boolean same(Object a,Object b) { return false; }
                public static int hash(Object value) { return 0; }
                public static int compare(Object a,Object b) { return 0; }
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
                public void add(int index,E value) {
                    if(index<0 || index>count) throw new IndexOutOfBoundsException(); add(value);
                    for(int i=count-1;i>index;i--) data[i]=data[i-1]; data[index]=value;
                }
                public List<E> subList(int from,int to) { return new __JavawSubList<E>(this,null,from,to); }
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
                public Stream<E> stream() { return Stream.fromCollection(this); }
                public Object[] toArray() {
                    Object[] result=new Object[count]; for(int i=0;i<count;i++) result[i]=data[i]; return result;
                }
            }
            class __JavawListIterator<E> implements Iterator<E> {
                private List<E> list; private ArrayList<E> root; private int index; private int expected; private int last;
                public __JavawListIterator(List<E> list,ArrayList<E> root) { this.list=list; this.root=root; expected=root.version; last=-1; }
                public __JavawListIterator(ArrayList<E> list) { this.list=list; root=list; expected=list.version; last=-1; }
                public boolean hasNext() { return index<list.size(); }
                public E next() {
                    if(expected!=root.version) throw new ConcurrentModificationException();
                    if (!hasNext()) throw new NoSuchElementException();
                    E result=list.get(index); last=index; index++; return result;
                }
            }
            class __JavawMapEntry<K,V> implements Entry<K,V> {
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
                public Entry<K,V> entryForKey(K key) { return entry(key,__JavawObjects.hash(key)); }
                public Collection<V> values() { return new __JavawMapValues<K,V>(this); }
                public Set<Entry<K,V>> entrySet() { return new __JavawEntrySet<K,V>(this); }
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
                private int index; private int expected; private __JavawMapEntry<K,V> current; private K lastKey; private boolean removable;
                public __JavawKeyIterator(HashMap<K,V> map,__JavawMapEntry[] buckets) {
                    this.map=map; this.buckets=buckets; expected=map.version; seek();
                }
                private void seek() { while(current==null && index<buckets.length) { current=buckets[index]; index++; } }
                public boolean hasNext() { return current!=null; }
                public K next() {
                    if(expected!=map.version) throw new ConcurrentModificationException();
                    if(current==null) throw new NoSuchElementException();
                    K key=current.key; lastKey=key; removable=true; current=current.next; seek(); return key;
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
                public Stream<K> stream() { return Stream.fromCollection(this); }
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
                public Stream<E> stream() { return Stream.fromCollection(this); }
            }
            """);
        var listIterator=cu.getTypes().stream().filter(t->t.getNameAsString().equals("__JavawListIterator")).findFirst().orElseThrow();
        listIterator.addMember(com.github.javaparser.StaticJavaParser.parseBodyDeclaration("public void remove() { if(expected!=root.version) throw new ConcurrentModificationException(); if(last<0) throw new IllegalStateException(); list.remove(last); index=last; last=-1; expected=root.version; }"));
        var keyIterator=cu.getTypes().stream().filter(t->t.getNameAsString().equals("__JavawKeyIterator")).findFirst().orElseThrow();
        keyIterator.addMember(com.github.javaparser.StaticJavaParser.parseBodyDeclaration("public void remove() { if(expected!=map.version) throw new ConcurrentModificationException(); if(!removable) throw new IllegalStateException(); map.remove(lastKey); expected=map.version; removable=false; }"));
        var entry=cu.getTypes().stream().filter(t->t.getNameAsString().equals("__JavawMapEntry")).findFirst().orElseThrow();
        for(String method:new String[]{"public boolean equals(Object other) { if(!(other instanceof Entry)) return false; Entry e=(Entry)other; return __JavawObjects.same(key,e.getKey()) && __JavawObjects.same(value,e.getValue()); }", "public int hashCode() { return __JavawObjects.hash(key) ^ __JavawObjects.hash(value); }", "public K getKey() { return key; }", "public V getValue() { return value; }", "public V setValue(V value) { V old=this.value; this.value=value; return old; }"}) entry.addMember(com.github.javaparser.StaticJavaParser.parseBodyDeclaration(method));
        CollectionViews.addTypes(cu);
    }
}
