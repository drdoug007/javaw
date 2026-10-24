package java.util;import java.util.function.*;
public class HashSet<E> extends AbstractSet<E> implements Set<E>,Cloneable,java.io.Serializable{
 protected HashMap<E,Object> map;private static final Object PRESENT=new Object();public HashSet(){map=new HashMap<E,Object>();}public HashSet(int capacity){map=new HashMap<E,Object>(capacity);}public HashSet(int capacity,float loadFactor){map=new HashMap<E,Object>(capacity,loadFactor);}public HashSet(Collection<? extends E> values){this();addAll(values);}
 public int size(){return map.size();}public boolean contains(Object value){return map.containsKey(value);}public boolean add(E value){return map.put(value,PRESENT)==null;}public boolean remove(Object value){return map.remove(value)!=null;}public void clear(){map.clear();}public Iterator<E> iterator(){return map.keySet().iterator();}
 public Object clone(){try{HashSet<E> copy=(HashSet<E>)super.clone();copy.map=(HashMap<E,Object>)map.clone();return copy;}catch(CloneNotSupportedException impossible){throw new InternalError();}}
}
