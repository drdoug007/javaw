package java.util;
/** Sequential sorted-array map. Ordering semantics, not balanced-tree complexity. */
public class TreeMap<K,V> extends AbstractMap<K,V> implements NavigableMap<K,V>,Cloneable,java.io.Serializable {
 final ArrayList<SimpleEntry<K,V>> entries;final Comparator<? super K> order;final TreeMap<K,V> owner;K low,high;boolean hasLow,hasHigh,lowInclusive,highInclusive,descending;int modifications;
 public TreeMap(){this((Comparator<? super K>)null);}public TreeMap(Comparator<? super K> order){this.order=order;entries=new ArrayList<>();owner=this;}
 public TreeMap(Map<? extends K,? extends V> source){this();putAll(source);}public TreeMap(SortedMap<K,? extends V> source){this(source.comparator());putAll(source);}
 TreeMap(TreeMap<K,V> parent,K low,boolean hasLow,boolean li,K high,boolean hasHigh,boolean hi){descending=parent.descending;owner=parent.owner;entries=owner.entries;order=owner.order;this.low=low;this.hasLow=hasLow;lowInclusive=li;this.high=high;this.hasHigh=hasHigh;highInclusive=hi;}
 boolean inRange(Object key){int c;if(hasLow&&((c=compare(key,low))<0||c==0&&!lowInclusive))return false;if(hasHigh&&((c=compare(key,high))>0||c==0&&!highInclusive))return false;return true;}
 void endpoint(K key,boolean inclusive){compare(key,key);int c;if(hasLow&&((c=compare(key,low))<0||c==0&&inclusive&&!lowInclusive)||hasHigh&&((c=compare(key,high))>0||c==0&&inclusive&&!highInclusive))throw new IllegalArgumentException("key out of range");}
 int compare(Object a,Object b){return order==null?((Comparable<Object>)Objects.requireNonNull(a)).compareTo(b):order.compare((K)a,(K)b);}
 int locate(Object key){if(order==null){Objects.requireNonNull(key);Comparable<?> checked=(Comparable<?>)key;}int low=0,high=entries.size();while(low<high){int middle=(low+high)>>>1;if(compare(key,entries.get(middle).getKey())>0)low=middle+1;else high=middle;}return low;}
 public Comparator<? super K> comparator(){return descending?Collections.reverseOrder((Comparator<K>)order):order;}public int size(){if(!hasLow&&!hasHigh)return entries.size();int count=0;for(SimpleEntry<K,V> e:entries)if(inRange(e.getKey()))count++;return count;}
 public boolean containsKey(Object key){int i=locate(key);if(!inRange(key))return false;return i<entries.size()&&compare(key,entries.get(i).getKey())==0;}
 public V get(Object key){int i=locate(key);if(!inRange(key))return null;return i<entries.size()&&compare(key,entries.get(i).getKey())==0?entries.get(i).getValue():null;}
 public V put(K key,V value){if(!inRange(key))throw new IllegalArgumentException("key out of range");if(entries.isEmpty())compare(key,key);int i=locate(key);if(i<entries.size()&&compare(key,entries.get(i).getKey())==0)return entries.get(i).setValue(value);owner.modifications++;entries.add(i,new SimpleEntry<K,V>(key,value));return null;}
 public V remove(Object key){int i=locate(key);if(!inRange(key))return null;if(i<entries.size()&&compare(key,entries.get(i).getKey())==0){owner.modifications++;return entries.remove(i).getValue();}return null;}
 public void clear(){if(this==owner){modifications++;entries.clear();return;}Iterator<Entry<K,V>> it=entrySet().iterator();while(it.hasNext()){it.next();it.remove();}}
 public K firstKey(){Entry<K,V> e=firstEntry();if(e==null)throw new NoSuchElementException();return e.getKey();}public K lastKey(){Entry<K,V> e=lastEntry();if(e==null)throw new NoSuchElementException();return e.getKey();}
 public SortedMap<K,V> subMap(K from,K to){return subMap(from,true,to,false);}public SortedMap<K,V> headMap(K to){return headMap(to,false);}public SortedMap<K,V> tailMap(K from){return tailMap(from,true);}
 public Set<Map.Entry<K,V>> entrySet(){return new AbstractSet<Map.Entry<K,V>>(){public int size(){return TreeMap.this.size();}
 public boolean contains(Object value){if(!(value instanceof Map.Entry))return false;Map.Entry<?,?> e=(Map.Entry<?,?>)value;return containsKey(e.getKey())&&Objects.equals(get(e.getKey()),e.getValue());}
 public boolean remove(Object value){if(!contains(value))return false;TreeMap.this.remove(((Map.Entry<?,?>)value).getKey());return true;}
 public void clear(){TreeMap.this.clear();}public Iterator<Map.Entry<K,V>> iterator(){return new Iterator<Map.Entry<K,V>>(){int index=descending?entries.size()-1:0,last=-1,expected=owner.modifications;void check(){if(expected!=owner.modifications)throw new ConcurrentModificationException();}
 public boolean hasNext(){while(index>=0&&index<entries.size()&&!inRange(entries.get(index).getKey()))index+=descending?-1:1;return index>=0&&index<entries.size();}
 public Map.Entry<K,V> next(){check();if(!hasNext())throw new NoSuchElementException();last=index;index+=descending?-1:1;return entries.get(last);}
 public void remove(){if(last<0)throw new IllegalStateException();check();entries.remove(last);owner.modifications++;expected=owner.modifications;index=descending?last-1:last;last=-1;}};}};}
 K key(Entry<K,V> entry){return entry==null?null:entry.getKey();}
 Entry<K,V> neighbor(K key,boolean up,boolean inclusive){Entry<K,V> best=null;for(Entry<K,V> e:entrySet()){int c=descending?compare(key,e.getKey()):compare(e.getKey(),key);if((up?(c>0||c==0&&inclusive):(c<0||c==0&&inclusive))&&(best==null||(up?(descending?compare(best.getKey(),e.getKey()):compare(e.getKey(),best.getKey()))<0:(descending?compare(best.getKey(),e.getKey()):compare(e.getKey(),best.getKey()))>0)))best=e;}return best==null?null:new SimpleImmutableEntry<K,V>(best);}
 public Entry<K,V> lowerEntry(K key){return neighbor(key,false,false);}public K lowerKey(K key){return key(lowerEntry(key));}
 public Entry<K,V> floorEntry(K key){return neighbor(key,false,true);}public K floorKey(K key){return key(floorEntry(key));}
 public Entry<K,V> ceilingEntry(K key){return neighbor(key,true,true);}public K ceilingKey(K key){return key(ceilingEntry(key));}
 public Entry<K,V> higherEntry(K key){return neighbor(key,true,false);}public K higherKey(K key){return key(higherEntry(key));}
 public Entry<K,V> firstEntry(){for(Entry<K,V> e:entrySet())return new SimpleImmutableEntry<K,V>(e);return null;}public Entry<K,V> lastEntry(){Entry<K,V> result=null;for(Entry<K,V> e:entrySet())result=e;return result==null?null:new SimpleImmutableEntry<K,V>(result);}
 public Entry<K,V> pollFirstEntry(){Entry<K,V> e=firstEntry();if(e!=null)remove(e.getKey());return e;}public Entry<K,V> pollLastEntry(){Entry<K,V> e=lastEntry();if(e!=null)remove(e.getKey());return e;}
 public NavigableMap<K,V> descendingMap(){TreeMap<K,V> result=new TreeMap<K,V>(this,low,hasLow,lowInclusive,high,hasHigh,highInclusive);result.descending=!descending;return result;}
 public Set<K> keySet(){return navigableKeySet();}public NavigableSet<K> navigableKeySet(){return new TreeKeySet<K>(this);}public NavigableSet<K> descendingKeySet(){return descendingMap().navigableKeySet();}
 public NavigableMap<K,V> subMap(K from,boolean fi,K to,boolean ti){endpoint(from,fi);endpoint(to,ti);if((descending?compare(to,from):compare(from,to))>0)throw new IllegalArgumentException();return descending?new TreeMap<K,V>(this,to,true,ti,from,true,fi):new TreeMap<K,V>(this,from,true,fi,to,true,ti);}
 public NavigableMap<K,V> headMap(K to,boolean inclusive){endpoint(to,inclusive);return descending?new TreeMap<K,V>(this,to,true,inclusive,high,hasHigh,highInclusive):new TreeMap<K,V>(this,low,hasLow,lowInclusive,to,true,inclusive);}
 public NavigableMap<K,V> tailMap(K from,boolean inclusive){endpoint(from,inclusive);return descending?new TreeMap<K,V>(this,low,hasLow,lowInclusive,from,true,inclusive):new TreeMap<K,V>(this,from,true,inclusive,high,hasHigh,highInclusive);}
 public Object clone(){return new TreeMap<K,V>((SortedMap<K,V>)this);}
}
