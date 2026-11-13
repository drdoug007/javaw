package java.util;
public class Vector<E> extends AbstractList<E> implements List<E>,RandomAccess,Cloneable,java.io.Serializable{
 protected Object[] elementData;protected int elementCount;
 public Vector(){this(10);}
 public Vector(int initialCapacity){if(initialCapacity<0)throw new IllegalArgumentException("Illegal Capacity: "+initialCapacity);elementData=new Object[initialCapacity];}
 public Vector(Collection<? extends E> c){Object[] source=c.toArray();elementData=Arrays.copyOf(source,source.length,Object[].class);elementCount=source.length;}
 private void grow(int minimum){if(minimum<=elementData.length)return;int n=elementData.length*2;if(n<minimum)n=minimum;elementData=Arrays.copyOf(elementData,n);}
 public int capacity(){return elementData.length;}
 public int size(){return elementCount;}
 public boolean isEmpty(){return elementCount==0;}
 public boolean contains(Object o){return indexOf(o,0)>=0;}
 public void addElement(E obj){grow(elementCount+1);elementData[elementCount++]=obj;modCount++;}
 public boolean add(E e){addElement(e);return true;}
 public boolean addAll(Collection<? extends E> c){Object[] a=c.toArray();int numNew=a.length;grow(elementCount+numNew);System.arraycopy(a,0,elementData,elementCount,numNew);modCount++;elementCount+=numNew;return numNew!=0;}
 public boolean addAll(int index,Collection<? extends E> c){if(index<0 || index>elementCount)throw new ArrayIndexOutOfBoundsException(index);Object[] a=c.toArray();int numNew=a.length;grow(elementCount+numNew);int numMoved=elementCount-index;if(numMoved>0)System.arraycopy(elementData,index,elementData,index+numNew,numMoved);System.arraycopy(a,0,elementData,index,numNew);elementCount+=numNew;modCount++;return numNew!=0;}
 public void insertElementAt(E obj,int index){if(index>elementCount)throw new ArrayIndexOutOfBoundsException(index+" > "+elementCount);if(index<0)throw new ArrayIndexOutOfBoundsException(index);grow(elementCount+1);System.arraycopy(elementData,index,elementData,index+1,elementCount-index);elementData[index]=obj;elementCount++;modCount++;}
 public E elementAt(int index){if(index>=elementCount)throw new ArrayIndexOutOfBoundsException(index+" >= "+elementCount);if(index<0)throw new ArrayIndexOutOfBoundsException(index);return (E)elementData[index];}
 public E firstElement(){if(elementCount==0)throw new NoSuchElementException();return (E)elementData[0];}
 public E lastElement(){if(elementCount==0)throw new NoSuchElementException();return (E)elementData[elementCount-1];}
 public void removeElementAt(int index){if(index>=elementCount)throw new ArrayIndexOutOfBoundsException(index+" >= "+elementCount);if(index<0)throw new ArrayIndexOutOfBoundsException(index);modCount++;int numMoved=elementCount-index-1;if(numMoved>0)System.arraycopy(elementData,index+1,elementData,index,numMoved);elementData[--elementCount]=null;}
 public boolean removeElement(Object obj){modCount++;int i=indexOf(obj);if(i>=0){removeElementAt(i);return true;}return false;}
 public void removeAllElements(){for(int i=0;i<elementCount;i++)elementData[i]=null;elementCount=0;modCount++;}
 public void clear(){removeAllElements();}
 public void copyInto(Object[] dest){System.arraycopy(elementData,0,dest,0,elementCount);}
 public Enumeration<E> elements(){return new Enumeration<E>(){int count=0;public boolean hasMoreElements(){return count<elementCount;}public E nextElement(){if(hasMoreElements())return elementAt(count++);throw new NoSuchElementException("Vector Enumeration");}};}
 public int indexOf(Object o){return indexOf(o,0);}
 public int indexOf(Object o,int index){for(int i=index;i<elementCount;i++)if(Objects.equals(o,elementData[i]))return i;return -1;}
 public int lastIndexOf(Object o){return lastIndexOf(o,elementCount-1);}
 public int lastIndexOf(Object o,int index){if(index>=elementCount)throw new IndexOutOfBoundsException(index+" >= "+elementCount);for(int i=index;i>=0;i--)if(Objects.equals(o,elementData[i]))return i;return -1;}
 public E get(int index){if(index<0 || index>=elementCount)throw new ArrayIndexOutOfBoundsException(index);return (E)elementData[index];}
 public E set(int index,E element){if(index<0 || index>=elementCount)throw new ArrayIndexOutOfBoundsException(index);E old=(E)elementData[index];elementData[index]=element;return old;}
 public void add(int index,E element){insertElementAt(element,index);}
 public E remove(int index){modCount++;if(index<0 || index>=elementCount)throw new ArrayIndexOutOfBoundsException(index);E old=(E)elementData[index];int numMoved=elementCount-index-1;if(numMoved>0)System.arraycopy(elementData,index+1,elementData,index,numMoved);elementData[--elementCount]=null;return old;}
 public boolean remove(Object o){return removeElement(o);}
 public Object[] toArray(){return Arrays.copyOf(elementData,elementCount);}
 public <T>T[] toArray(T[] a){if(a.length<elementCount)return (T[])Arrays.copyOf(elementData,elementCount,a.getClass());System.arraycopy(elementData,0,a,0,elementCount);if(a.length>elementCount)a[elementCount]=null;return a;}
 public Object clone(){try{Vector<E> copy=(Vector<E>)super.clone();copy.elementData=Arrays.copyOf(elementData,elementCount);copy.modCount=0;return copy;}catch(CloneNotSupportedException impossible){throw new InternalError();}}
}
