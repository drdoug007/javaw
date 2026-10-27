package java.util;
public abstract class AbstractSequentialList<E> extends AbstractList<E>{
 protected AbstractSequentialList(){}
 public E get(int index){try{return listIterator(index).next();}catch(NoSuchElementException failure){throw new IndexOutOfBoundsException("Index: "+index);}}
 public E set(int index,E value){try{ListIterator<E> cursor=listIterator(index);E old=cursor.next();cursor.set(value);return old;}catch(NoSuchElementException failure){throw new IndexOutOfBoundsException("Index: "+index);}}
 public void add(int index,E value){try{listIterator(index).add(value);}catch(NoSuchElementException failure){throw new IndexOutOfBoundsException("Index: "+index);}}
 public E remove(int index){try{ListIterator<E> cursor=listIterator(index);E old=cursor.next();cursor.remove();return old;}catch(NoSuchElementException failure){throw new IndexOutOfBoundsException("Index: "+index);}}
 public boolean addAll(int index,Collection<? extends E> values){try{boolean changed=false;ListIterator<E> cursor=listIterator(index);for(E value:values){cursor.add(value);changed=true;}return changed;}catch(NoSuchElementException failure){throw new IndexOutOfBoundsException("Index: "+index);}}
 public Iterator<E> iterator(){return listIterator();}
 public abstract ListIterator<E> listIterator(int index);
}
