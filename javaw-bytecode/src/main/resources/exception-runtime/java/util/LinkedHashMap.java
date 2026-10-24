package java.util;import java.util.function.*;
public class LinkedHashMap<K,V> extends HashMap<K,V> implements Map<K,V>{
 private final boolean accessOrder;
 public LinkedHashMap(){accessOrder=false;}public LinkedHashMap(int capacity){super(capacity);accessOrder=false;}public LinkedHashMap(int capacity,float loadFactor){super(capacity,loadFactor);accessOrder=false;}public LinkedHashMap(int capacity,float loadFactor,boolean accessOrder){super(capacity,loadFactor);this.accessOrder=accessOrder;}public LinkedHashMap(Map<? extends K,? extends V> source){this();putEntries(source,false);}
 protected void afterAccess(Node<K,V> node){if(!accessOrder || node==last)return;if(node.previous==null)first=node.next;else node.previous.next=node.next;node.next.previous=node.previous;node.previous=last;node.next=null;last.next=node;last=node;modCount++;}
 protected boolean removeEldestEntry(Map.Entry<K,V> eldest){return false;}protected void afterInsert(){if(first!=null && removeEldestEntry(first))unlink(first);}
}
