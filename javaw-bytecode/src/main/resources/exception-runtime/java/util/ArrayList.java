package java.util;
public class ArrayList<E> extends AbstractList<E> implements List<E>,RandomAccess,Cloneable,java.io.Serializable{
 private Object[] elements;private int count;private boolean defaultCapacity;
 public ArrayList(){elements=new Object[0];defaultCapacity=true;}
 public ArrayList(int capacity){if(capacity<0)throw new IllegalArgumentException();elements=new Object[capacity];}
 public ArrayList(Collection<? extends E> values){Object[] source=values.toArray();elements=new Object[source.length];System.arraycopy(source,0,elements,0,source.length);count=source.length;}
 public int size(){return count;}
 private void capacity(int minimum){if(minimum<=elements.length)return;int grown=elements.length+elements.length/2;if(defaultCapacity && grown<10)grown=10;if(grown<minimum)grown=minimum;if(grown<0)throw new OutOfMemoryError();Object[] replacement=new Object[grown];System.arraycopy(elements,0,replacement,0,count);elements=replacement;defaultCapacity=false;}
 public void ensureCapacity(int minimum){if(minimum>elements.length && (!defaultCapacity || minimum>10)){modCount++;capacity(minimum);}}
 public void trimToSize(){modCount++;if(count<elements.length){Object[] replacement=new Object[count];System.arraycopy(elements,0,replacement,0,count);elements=replacement;}defaultCapacity=false;}
 public E get(int index){index(index);return (E)elements[index];}
 public E set(int index,E value){index(index);E old=(E)elements[index];elements[index]=value;return old;}
 public void add(int index,E value){position(index);if(count==2147483647)throw new OutOfMemoryError();capacity(count+1);System.arraycopy(elements,index,elements,index+1,count-index);elements[index]=value;count++;modCount++;}
 public E remove(int index){index(index);E old=(E)elements[index];System.arraycopy(elements,index+1,elements,index,count-index-1);elements[--count]=null;modCount++;return old;}
 public boolean addAll(Collection<? extends E> values){return addAll(count,values);}
 public boolean addAll(int index,Collection<? extends E> values){position(index);Object[] source=values.toArray();modCount++;if(source.length==0)return false;if(count>2147483647-source.length)throw new OutOfMemoryError();capacity(count+source.length);System.arraycopy(elements,index,elements,index+source.length,count-index);System.arraycopy(source,0,elements,index,source.length);count+=source.length;return true;}
 public void clear(){modCount++;for(int i=0;i<count;i++)elements[i]=null;count=0;}
 public Object clone(){
  try{ArrayList<E> copy=(ArrayList<E>)super.clone();copy.elements=new Object[count];System.arraycopy(elements,0,copy.elements,0,count);copy.modCount=0;copy.defaultCapacity=false;return copy;}
  catch(CloneNotSupportedException impossible){throw new InternalError();}
 }
}
