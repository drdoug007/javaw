package java.util;
public abstract class AbstractList<E> extends AbstractCollection<E> implements List<E>{
 protected int modCount;protected AbstractList(){}
 protected final void index(int index){if(index<0 || index>=size())throw new IndexOutOfBoundsException("Index "+index+" out of bounds for length "+size());}
 protected final void position(int index){if(index<0 || index>size())throw new IndexOutOfBoundsException("Index: "+index+", Size: "+size());}
 public E set(int index,E value){throw new UnsupportedOperationException();}public void add(int index,E value){throw new UnsupportedOperationException();}public E remove(int index){throw new UnsupportedOperationException();}
 public boolean add(E value){add(size(),value);return true;}
 public int indexOf(Object value){ListIterator<E> iterator=listIterator();while(iterator.hasNext())if(Objects.equals(value,iterator.next()))return iterator.previousIndex();return -1;}
 public int lastIndexOf(Object value){ListIterator<E> iterator=listIterator(size());while(iterator.hasPrevious())if(Objects.equals(value,iterator.previous()))return iterator.nextIndex();return -1;}
 public boolean addAll(int index,Collection<? extends E> values){position(index);boolean changed=false;for(E value:values){add(index++,value);changed=true;}return changed;}
 public Iterator<E> iterator(){return listIterator();}public ListIterator<E> listIterator(){return listIterator(0);}
 public ListIterator<E> listIterator(int index){position(index);return new Cursor(index);}
 private final class Cursor implements ListIterator<E>{
  int cursor,last=-1,expected=modCount;Cursor(int index){cursor=index;}
  private void check(){AbstractList.this.size();if(expected!=modCount)throw new ConcurrentModificationException();}
  public boolean hasNext(){return cursor!=size();}public boolean hasPrevious(){return cursor!=0;}public int nextIndex(){return cursor;}public int previousIndex(){return cursor-1;}
  public E next(){check();if(cursor>=size())throw new NoSuchElementException();E value=get(cursor);last=cursor++;return value;}
  public E previous(){check();if(cursor<=0)throw new NoSuchElementException();E value=get(cursor-1);last=--cursor;return value;}
  public void remove(){if(last<0)throw new IllegalStateException();check();AbstractList.this.remove(last);if(last<cursor)cursor--;last=-1;expected=modCount;}
  public void set(E value){if(last<0)throw new IllegalStateException();check();AbstractList.this.set(last,value);}
  public void add(E value){check();AbstractList.this.add(cursor,value);cursor++;last=-1;expected=modCount;}
 }
 public boolean equals(Object other){if(other==this)return true;if(!(other instanceof List))return false;Iterator<?> right=((List<?>)other).iterator();for(E value:this)if(!right.hasNext() || !Objects.equals(value,right.next()))return false;return !right.hasNext();}
 public int hashCode(){int hash=1;for(E value:this)hash=31*hash+Objects.hashCode(value);return hash;}
 public List<E> subList(int start,int end){if(start<0)throw new IndexOutOfBoundsException("fromIndex = "+start);if(end>size())throw new IndexOutOfBoundsException("toIndex = "+end);if(start>end)throw new IllegalArgumentException("fromIndex("+start+") > toIndex("+end+")");return this instanceof RandomAccess?new RandomView<E>(this,start,end):new View<E>(this,start,end);}
 private static class View<E> extends AbstractList<E>{
  final AbstractList<E> parent;final int offset;int length,expected;
  View(AbstractList<E> parent,int start,int end){this.parent=parent;offset=start;length=end-start;expected=parent.modCount;}
  void check(){parent.size();if(expected!=parent.modCount)throw new ConcurrentModificationException();}
  void refresh(){expected=parent.modCount;modCount++;}
  public int size(){check();return length;}public E get(int index){check();index(index);return parent.get(offset+index);}
  public E set(int index,E value){check();index(index);return parent.set(offset+index,value);}
  public void add(int index,E value){check();position(index);parent.add(offset+index,value);length++;refresh();}
  public E remove(int index){check();index(index);E old=parent.remove(offset+index);length--;refresh();return old;}
 }
 private static final class RandomView<E> extends View<E> implements RandomAccess{RandomView(AbstractList<E> parent,int start,int end){super(parent,start,end);}}
}
