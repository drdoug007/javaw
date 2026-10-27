package java.util;

/** Doubly linked list. Deque/Queue interfaces are not linked yet; their LinkedList methods are class members only. */
public class LinkedList<E> extends AbstractSequentialList<E> implements List<E>,Cloneable,java.io.Serializable{
 private static final class Node<E>{E item;Node<E> next,prev;Node(Node<E> prev,E item,Node<E> next){this.prev=prev;this.item=item;this.next=next;}}
 transient int size;transient Node<E> first,last;
 public LinkedList(){}
 public LinkedList(Collection<? extends E> values){this();addAll(values);}
 private void linkFirst(E value){Node<E> old=first,node=new Node<>(null,value,old);first=node;if(old==null)last=node;else old.prev=node;size++;modCount++;}
 void linkLast(E value){Node<E> old=last,node=new Node<>(old,value,null);last=node;if(old==null)first=node;else old.next=node;size++;modCount++;}
 void linkBefore(E value,Node<E> successor){Node<E> previous=successor.prev,node=new Node<>(previous,value,successor);successor.prev=node;if(previous==null)first=node;else previous.next=node;size++;modCount++;}
 E unlink(Node<E> node){
  E value=node.item;Node<E> next=node.next,previous=node.prev;
  if(previous==null)first=next;else{previous.next=next;node.prev=null;}
  if(next==null)last=previous;else{next.prev=previous;node.next=null;}
  node.item=null;size--;modCount++;return value;
 }
 public E getFirst(){if(first==null)throw new NoSuchElementException();return first.item;}
 public E getLast(){if(last==null)throw new NoSuchElementException();return last.item;}
 public E removeFirst(){if(first==null)throw new NoSuchElementException();return unlink(first);}
 public E removeLast(){if(last==null)throw new NoSuchElementException();return unlink(last);}
 public void addFirst(E value){linkFirst(value);}public void addLast(E value){linkLast(value);}
 public boolean contains(Object value){return indexOf(value)>=0;}
 public int size(){return size;}
 public boolean add(E value){linkLast(value);return true;}
 public boolean remove(Object value){for(Node<E> node=first;node!=null;node=node.next)if(Objects.equals(value,node.item)){unlink(node);return true;}return false;}
 public boolean addAll(Collection<? extends E> values){return addAll(size,values);}
 public boolean addAll(int index,Collection<? extends E> values){
  cursor(index);Object[] source=values.toArray();if(source.length==0)return false;
  Node<E> successor,previous;if(index==size){successor=null;previous=last;}else{successor=node(index);previous=successor.prev;}
  for(Object item:source){Node<E> node=new Node<>(previous,(E)item,null);if(previous==null)first=node;else previous.next=node;previous=node;}
  if(successor==null)last=previous;else{previous.next=successor;successor.prev=previous;}
  size+=source.length;modCount++;return true;
 }
 public void clear(){for(Node<E> node=first;node!=null;){Node<E> next=node.next;node.item=null;node.next=null;node.prev=null;node=next;}first=last=null;size=0;modCount++;}
 private void element(int index){if(index<0 || index>=size)throw new IndexOutOfBoundsException("Index: "+index+", Size: "+size);}
 private void cursor(int index){if(index<0 || index>size)throw new IndexOutOfBoundsException("Index: "+index+", Size: "+size);}
 public E get(int index){element(index);return node(index).item;}
 public E set(int index,E value){element(index);Node<E> node=node(index);E old=node.item;node.item=value;return old;}
 public void add(int index,E value){cursor(index);if(index==size)linkLast(value);else linkBefore(value,node(index));}
 public E remove(int index){element(index);return unlink(node(index));}
 Node<E> node(int index){
  if(index<(size>>1)){Node<E> node=first;for(int i=0;i<index;i++)node=node.next;return node;}
  Node<E> node=last;for(int i=size-1;i>index;i--)node=node.prev;return node;
 }
 public int indexOf(Object value){int index=0;for(Node<E> node=first;node!=null;node=node.next,index++)if(Objects.equals(value,node.item))return index;return -1;}
 public int lastIndexOf(Object value){int index=size;for(Node<E> node=last;node!=null;node=node.prev){index--;if(Objects.equals(value,node.item))return index;}return -1;}
 public E peek(){return first==null?null:first.item;}
 public E element(){return getFirst();}
 public E poll(){return first==null?null:unlink(first);}
 public E remove(){return removeFirst();}
 public boolean offer(E value){return add(value);}
 public boolean offerFirst(E value){addFirst(value);return true;}
 public boolean offerLast(E value){addLast(value);return true;}
 public E peekFirst(){return first==null?null:first.item;}
 public E peekLast(){return last==null?null:last.item;}
 public E pollFirst(){return first==null?null:unlink(first);}
 public E pollLast(){return last==null?null:unlink(last);}
 public void push(E value){addFirst(value);}
 public E pop(){return removeFirst();}
 public boolean removeFirstOccurrence(Object value){return remove(value);}
 public boolean removeLastOccurrence(Object value){for(Node<E> node=last;node!=null;node=node.prev)if(Objects.equals(value,node.item)){unlink(node);return true;}return false;}
 public ListIterator<E> listIterator(int index){cursor(index);return new Cursor(index);}
 private final class Cursor implements ListIterator<E>{
  private Node<E> lastReturned,next;private int nextIndex,expected=modCount;
  Cursor(int index){next=index==size?null:node(index);nextIndex=index;}
  private void check(){if(modCount!=expected)throw new ConcurrentModificationException();}
  public boolean hasNext(){return nextIndex<size;}
  public E next(){check();if(!hasNext())throw new NoSuchElementException();lastReturned=next;next=next.next;nextIndex++;return lastReturned.item;}
  public boolean hasPrevious(){return nextIndex>0;}
  public E previous(){check();if(!hasPrevious())throw new NoSuchElementException();lastReturned=next=next==null?last:next.prev;nextIndex--;return lastReturned.item;}
  public int nextIndex(){return nextIndex;}public int previousIndex(){return nextIndex-1;}
  public void remove(){check();if(lastReturned==null)throw new IllegalStateException();Node<E> following=lastReturned.next;unlink(lastReturned);if(next==lastReturned)next=following;else nextIndex--;lastReturned=null;expected++;}
  public void set(E value){if(lastReturned==null)throw new IllegalStateException();check();lastReturned.item=value;}
  public void add(E value){check();lastReturned=null;if(next==null)linkLast(value);else linkBefore(value,next);nextIndex++;expected++;}
 }
 public Iterator<E> descendingIterator(){
  return new Iterator<E>(){
   private final Cursor cursor=new Cursor(size);
   public boolean hasNext(){return cursor.hasPrevious();}public E next(){return cursor.previous();}public void remove(){cursor.remove();}
  };
 }
 public Object clone(){
  try{LinkedList<E> copy=(LinkedList<E>)super.clone();copy.first=copy.last=null;copy.size=0;copy.modCount=0;for(Node<E> node=first;node!=null;node=node.next)copy.add(node.item);return copy;}
  catch(CloneNotSupportedException impossible){throw new InternalError();}
 }
 public Object[] toArray(){Object[] result=new Object[size];int index=0;for(Node<E> node=first;node!=null;node=node.next)result[index++]=node.item;return result;}
}
