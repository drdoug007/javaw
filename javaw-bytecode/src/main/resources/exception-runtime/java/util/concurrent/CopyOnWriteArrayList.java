package java.util.concurrent;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Single-threaded CopyOnWriteArrayList: every mutating operation builds a new
 * array and installs it with setArray; no lock is kept because the runtime is
 * single-threaded. Snapshot iterators read the array captured when they were
 * created, so they observe writes as stale state, and their mutating methods
 * throw UnsupportedOperationException.
 */
public class CopyOnWriteArrayList<E> implements List<E>,RandomAccess,Cloneable,java.io.Serializable{
 private Object[] array;

 final Object[] getArray(){return array;}

 final void setArray(Object[] a){array=a;}

 public CopyOnWriteArrayList(){
  setArray(new Object[0]);
 }

 public CopyOnWriteArrayList(Collection<? extends E> c){
  Object[] es;
  if(c.getClass()==CopyOnWriteArrayList.class)
   es=((CopyOnWriteArrayList<?>)c).getArray();
  else{
   es=c.toArray();
   if(c.getClass()!=ArrayList.class)
    es=Arrays.copyOf(es,es.length,Object[].class);
  }
  setArray(es);
 }

 public CopyOnWriteArrayList(E[] toCopyIn){
  setArray(Arrays.copyOf(toCopyIn,toCopyIn.length,Object[].class));
 }

 public int size(){
  return getArray().length;
 }

 public boolean isEmpty(){
  return size()==0;
 }

 private static int indexOfRange(Object o,Object[] es,int from,int to){
  if(o==null){
   for(int i=from;i<to;i++)
    if(es[i]==null)
     return i;
  }else{
   for(int i=from;i<to;i++)
    if(o.equals(es[i]))
     return i;
  }
  return -1;
 }

 private static int lastIndexOfRange(Object o,Object[] es,int from,int to){
  if(o==null){
   for(int i=to-1;i>=from;i--)
    if(es[i]==null)
     return i;
  }else{
   for(int i=to-1;i>=from;i--)
    if(o.equals(es[i]))
     return i;
  }
  return -1;
 }

 public boolean contains(Object o){
  return indexOf(o)>=0;
 }

 public int indexOf(Object o){
  Object[] es=getArray();
  return indexOfRange(o,es,0,es.length);
 }

 public int indexOf(E e,int index){
  Object[] es=getArray();
  return indexOfRange(e,es,index,es.length);
 }

 public int lastIndexOf(Object o){
  Object[] es=getArray();
  return lastIndexOfRange(o,es,0,es.length);
 }

 public int lastIndexOf(E e,int index){
  Object[] es=getArray();
  return lastIndexOfRange(e,es,0,index+1);
 }

 public Object clone(){
  try{
   return super.clone();
  }catch(CloneNotSupportedException impossible){
   throw new InternalError();
  }
 }

 public Object[] toArray(){
  return getArray().clone();
 }

 @SuppressWarnings("unchecked")
 public <T>T[] toArray(T[] a){
  Object[] es=getArray();
  int len=es.length;
  if(a.length<len)
   return (T[])Arrays.copyOf(es,len,a.getClass());
  System.arraycopy(es,0,a,0,len);
  if(a.length>len)
   a[len]=null;
  return a;
 }

 public E get(int index){
  return (E)getArray()[index];
 }

 public E set(int index,E element){
  Object[] es=getArray();
  E oldValue=(E)es[index];
  if(oldValue!=element){
   es=es.clone();
   es[index]=element;
  }
  setArray(es);
  return oldValue;
 }

 public boolean add(E e){
  Object[] es=getArray();
  int len=es.length;
  es=Arrays.copyOf(es,len+1);
  es[len]=e;
  setArray(es);
  return true;
 }

 public void add(int index,E element){
  Object[] es=getArray();
  int len=es.length;
  if(index>len||index<0)
   throw new IndexOutOfBoundsException(outOfBounds(index,len));
  Object[] newElements;
  int numMoved=len-index;
  if(numMoved==0)
   newElements=Arrays.copyOf(es,len+1);
  else{
   newElements=new Object[len+1];
   System.arraycopy(es,0,newElements,0,index);
   System.arraycopy(es,index,newElements,index+1,numMoved);
  }
  newElements[index]=element;
  setArray(newElements);
 }

 public E remove(int index){
  Object[] es=getArray();
  int len=es.length;
  E oldValue=(E)es[index];
  Object[] newElements;
  int numMoved=len-index-1;
  if(numMoved==0)
   newElements=Arrays.copyOf(es,len-1);
  else{
   newElements=new Object[len-1];
   System.arraycopy(es,0,newElements,0,index);
   System.arraycopy(es,index+1,newElements,index,numMoved);
  }
  setArray(newElements);
  return oldValue;
 }

 public boolean remove(Object o){
  Object[] es=getArray();
  int index=indexOfRange(o,es,0,es.length);
  if(index<0)
   return false;
  Object[] newElements;
  int numMoved=es.length-index-1;
  if(numMoved==0)
   newElements=Arrays.copyOf(es,es.length-1);
  else{
   newElements=new Object[es.length-1];
   System.arraycopy(es,0,newElements,0,index);
   System.arraycopy(es,index+1,newElements,index,numMoved);
  }
  setArray(newElements);
  return true;
 }

 void removeRange(int fromIndex,int toIndex){
  Object[] es=getArray();
  int len=es.length;
  if(fromIndex<0||toIndex>len||toIndex<fromIndex)
   throw new IndexOutOfBoundsException();
  int newlen=len-(toIndex-fromIndex);
  int numMoved=len-toIndex;
  if(numMoved==0)
   setArray(Arrays.copyOf(es,newlen));
  else{
   Object[] newElements=new Object[newlen];
   System.arraycopy(es,0,newElements,0,fromIndex);
   System.arraycopy(es,toIndex,newElements,fromIndex,numMoved);
   setArray(newElements);
  }
 }

 public boolean addIfAbsent(E e){
  Object[] es=getArray();
  int len=es.length;
  if(indexOfRange(e,es,0,len)>=0)
   return false;
  es=Arrays.copyOf(es,len+1);
  es[len]=e;
  setArray(es);
  return true;
 }

 public int addAllAbsent(Collection<? extends E> c){
  Object[] cs=c.toArray();
  if(c.getClass()!=ArrayList.class)
   cs=cs.clone();
  if(cs.length==0)
   return 0;
  Object[] es=getArray();
  int len=es.length;
  int added=0;
  for(int i=0;i<cs.length;++i){
   Object e=cs[i];
   if(indexOfRange(e,es,0,len)<0
     &&indexOfRange(e,cs,0,added)<0)
    cs[added++]=e;
  }
  if(added>0){
   Object[] newElements=Arrays.copyOf(es,len+added);
   System.arraycopy(cs,0,newElements,len,added);
   setArray(newElements);
  }
  return added;
 }

 public boolean containsAll(Collection<?> c){
  Object[] es=getArray();
  int len=es.length;
  for(Object e:c)
   if(indexOfRange(e,es,0,len)<0)
    return false;
  return true;
 }

 public boolean removeAll(Collection<?> c){
  Objects.requireNonNull(c);
  return bulkRemove(e->c.contains(e),0,getArray().length);
 }

 public boolean retainAll(Collection<?> c){
  Objects.requireNonNull(c);
  return bulkRemove(e->!c.contains(e),0,getArray().length);
 }

 public void clear(){
  setArray(new Object[0]);
 }

 public boolean addAll(Collection<? extends E> c){
  Object[] cs=(c.getClass()==CopyOnWriteArrayList.class)
   ?((CopyOnWriteArrayList<?>)c).getArray()
   :c.toArray();
  if(cs.length==0)
   return false;
  Object[] es=getArray();
  int len=es.length;
  Object[] newElements;
  if(len==0&&(c.getClass()==CopyOnWriteArrayList.class||c.getClass()==ArrayList.class)){
   newElements=cs;
  }else{
   newElements=Arrays.copyOf(es,len+cs.length);
   System.arraycopy(cs,0,newElements,len,cs.length);
  }
  setArray(newElements);
  return true;
 }

 public boolean addAll(int index,Collection<? extends E> c){
  Object[] cs=c.toArray();
  Object[] es=getArray();
  int len=es.length;
  if(index>len||index<0)
   throw new IndexOutOfBoundsException(outOfBounds(index,len));
  if(cs.length==0)
   return false;
  Object[] newElements;
  int numMoved=len-index;
  if(numMoved==0)
   newElements=Arrays.copyOf(es,len+cs.length);
  else{
   newElements=new Object[len+cs.length];
   System.arraycopy(es,0,newElements,0,index);
   System.arraycopy(es,index,newElements,index+cs.length,numMoved);
  }
  System.arraycopy(cs,0,newElements,index,cs.length);
  setArray(newElements);
  return true;
 }

 public void forEach(Consumer<? super E> action){
  Objects.requireNonNull(action);
  for(Object x:getArray())
   action.accept((E)x);
 }

 public boolean removeIf(Predicate<? super E> filter){
  Objects.requireNonNull(filter);
  return bulkRemove(filter,0,getArray().length);
 }

 boolean bulkRemove(Predicate<? super E> filter,int i,int end){
  Object[] es=getArray();
  boolean[] removed=new boolean[end-i];
  int deleted=0;
  for(int k=i;k<end;k++)
   if(filter.test((E)es[k])){
    removed[k-i]=true;
    deleted++;
   }
  if(es!=getArray())
   throw new ConcurrentModificationException();
  if(deleted==0)
   return false;
  Object[] newElts=Arrays.copyOf(es,es.length-deleted);
  int w=0;
  for(int k=0;k<es.length;k++)
   if(k<i||k>=end||!removed[k-i])
    newElts[w++]=es[k];
  setArray(newElts);
  return true;
 }

 public void replaceAll(UnaryOperator<E> operator){
  replaceAllRange(operator,0,getArray().length);
 }

 void replaceAllRange(UnaryOperator<E> operator,int i,int end){
  Objects.requireNonNull(operator);
  Object[] es=getArray().clone();
  for(int k=i;k<end;k++)
   es[k]=operator.apply((E)es[k]);
  setArray(es);
 }

 public void sort(Comparator<? super E> c){
  sortRange(c,0,getArray().length);
 }

 @SuppressWarnings("unchecked")
 void sortRange(Comparator<? super E> c,int i,int end){
  Object[] es=getArray().clone();
  Arrays.sort(es,i,end,(Comparator<Object>)c);
  setArray(es);
 }

 public String toString(){
  return Arrays.toString(getArray());
 }

 public boolean equals(Object o){
  if(o==this)
   return true;
  if(!(o instanceof List))
   return false;
  Iterator<?> it=((List<?>)o).iterator();
  for(Object element:getArray())
   if(!it.hasNext()||!Objects.equals(element,it.next()))
    return false;
  return !it.hasNext();
 }

 public int hashCode(){
  Object[] es=getArray();
  int hashCode=1;
  for(int i=0;i<es.length;i++){
   Object x=es[i];
   hashCode=31*hashCode+(x==null?0:x.hashCode());
  }
  return hashCode;
 }

 public Iterator<E> iterator(){
  return new COWIterator<E>(getArray(),0);
 }

 public ListIterator<E> listIterator(){
  return new COWIterator<E>(getArray(),0);
 }

 public ListIterator<E> listIterator(int index){
  Object[] es=getArray();
  int len=es.length;
  if(index<0||index>len)
   throw new IndexOutOfBoundsException(outOfBounds(index,len));
  return new COWIterator<E>(es,index);
 }

 public List<E> subList(int fromIndex,int toIndex){
  Object[] es=getArray();
  int len=es.length;
  int size=toIndex-fromIndex;
  if(fromIndex<0||toIndex>len||size<0)
   throw new IndexOutOfBoundsException();
  return new COWSubList(es,fromIndex,size);
 }

 static String outOfBounds(int index,int size){
  return "Index: "+index+", Size: "+size;
 }

 static final class COWIterator<E> implements ListIterator<E>{
  private final Object[] snapshot;
  private int cursor;

  COWIterator(Object[] es,int initialCursor){
   cursor=initialCursor;
   snapshot=es;
  }

  public boolean hasNext(){
   return cursor<snapshot.length;
  }

  public boolean hasPrevious(){
   return cursor>0;
  }

  public E next(){
   if(!hasNext())
    throw new NoSuchElementException();
   return (E)snapshot[cursor++];
  }

  public E previous(){
   if(!hasPrevious())
    throw new NoSuchElementException();
   return (E)snapshot[--cursor];
  }

  public int nextIndex(){
   return cursor;
  }

  public int previousIndex(){
   return cursor-1;
  }

  public void remove(){
   throw new UnsupportedOperationException();
  }

  public void set(E e){
   throw new UnsupportedOperationException();
  }

  public void add(E e){
   throw new UnsupportedOperationException();
  }

  public void forEachRemaining(Consumer<? super E> action){
   Objects.requireNonNull(action);
   int size=snapshot.length;
   int i=cursor;
   cursor=size;
   for(;i<size;i++)
    action.accept((E)snapshot[i]);
  }
 }

 private class COWSubList extends AbstractList<E> implements RandomAccess{
  private final int offset;
  private int size;
  private Object[] expectedArray;

  COWSubList(Object[] es,int offset,int size){
   expectedArray=es;
   this.offset=offset;
   this.size=size;
  }

  private void checkForComodification(){
   if(getArray()!=expectedArray)
    throw new ConcurrentModificationException();
  }

  private void rangeCheck(int index){
   if(index<0||index>=size)
    throw new IndexOutOfBoundsException(outOfBounds(index,size));
  }

  private void rangeCheckForAdd(int index){
   if(index<0||index>size)
    throw new IndexOutOfBoundsException(outOfBounds(index,size));
  }

  public int size(){
   checkForComodification();
   return size;
  }

  public E get(int index){
   rangeCheck(index);
   checkForComodification();
   return CopyOnWriteArrayList.this.get(offset+index);
  }

  public E set(int index,E element){
   rangeCheck(index);
   checkForComodification();
   E x=CopyOnWriteArrayList.this.set(offset+index,element);
   expectedArray=getArray();
   return x;
  }

  public void add(int index,E element){
   checkForComodification();
   rangeCheckForAdd(index);
   CopyOnWriteArrayList.this.add(offset+index,element);
   expectedArray=getArray();
   size++;
  }

  public E remove(int index){
   rangeCheck(index);
   checkForComodification();
   E result=CopyOnWriteArrayList.this.remove(offset+index);
   expectedArray=getArray();
   size--;
   return result;
  }

  public boolean remove(Object o){
   checkForComodification();
   int index=indexOf(o);
   if(index==-1)
    return false;
   remove(index);
   return true;
  }

  public boolean addAll(Collection<? extends E> c){
   checkForComodification();
   Object[] old=getArray();
   boolean modified=CopyOnWriteArrayList.this.addAll(offset+size,c);
   size+=(expectedArray=getArray()).length-old.length;
   return modified;
  }

  public boolean addAll(int index,Collection<? extends E> c){
   rangeCheckForAdd(index);
   checkForComodification();
   Object[] old=getArray();
   boolean modified=CopyOnWriteArrayList.this.addAll(offset+index,c);
   size+=(expectedArray=getArray()).length-old.length;
   return modified;
  }

  public void clear(){
   checkForComodification();
   removeRange(offset,offset+size);
   expectedArray=getArray();
   size=0;
  }

  public ListIterator<E> listIterator(int index){
   checkForComodification();
   rangeCheckForAdd(index);
   return new COWIterator<E>(Arrays.copyOfRange(getArray(),offset,offset+size),index);
  }

  public List<E> subList(int fromIndex,int toIndex){
   checkForComodification();
   if(fromIndex<0||toIndex>size||fromIndex>toIndex)
    throw new IndexOutOfBoundsException();
   return new COWSubList(expectedArray,fromIndex+offset,toIndex-fromIndex);
  }

  public void replaceAll(UnaryOperator<E> operator){
   checkForComodification();
   replaceAllRange(operator,offset,offset+size);
   expectedArray=getArray();
  }

  public void sort(Comparator<? super E> c){
   checkForComodification();
   sortRange(c,offset,offset+size);
   expectedArray=getArray();
  }

  public boolean removeIf(Predicate<? super E> filter){
   Objects.requireNonNull(filter);
   checkForComodification();
   Object[] old=getArray();
   boolean modified=bulkRemove(filter,offset,offset+size);
   size+=(expectedArray=getArray()).length-old.length;
   return modified;
  }

  public boolean removeAll(Collection<?> c){
   Objects.requireNonNull(c);
   return removeIf(e->c.contains(e));
  }

  public boolean retainAll(Collection<?> c){
   Objects.requireNonNull(c);
   return removeIf(e->!c.contains(e));
  }
 }
}
