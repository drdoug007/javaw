package java.util;
public class ArrayList<E> extends AbstractList<E> implements List<E>,RandomAccess,Cloneable,java.io.Serializable{
 private Object[] elements;private int count;private boolean defaultCapacity;
 public ArrayList(){elements=new Object[0];defaultCapacity=true;}
 public ArrayList(int capacity){if(capacity<0)throw new IllegalArgumentException("Illegal Capacity: "+capacity);elements=new Object[capacity];}
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
 public Spliterator<E> spliterator(){return new ArrayTraversal(0,-1,0);}
 private final class ArrayTraversal implements Spliterator<E>{
 int index,fence,expected;ArrayTraversal(int index,int fence,int expected){this.index=index;this.fence=fence;this.expected=expected;}
 int fence(){if(fence<0){fence=count;expected=modCount;}return fence;}
 public int characteristics(){return Spliterator.ORDERED|Spliterator.SIZED|Spliterator.SUBSIZED;}public long estimateSize(){return fence()-index;}
 public Spliterator<E> trySplit(){int end=fence(),middle=(index+end)>>>1;if(index>=middle)return null;int from=index;index=middle;return new ArrayTraversal(from,middle,expected);}
 public boolean tryAdvance(java.util.function.Consumer<? super E> action){Objects.requireNonNull(action);int end=fence();if(index>=end)return false;if(index>=elements.length)throw new ConcurrentModificationException();E value=(E)elements[index++];action.accept(value);if(modCount!=expected)throw new ConcurrentModificationException();return true;}
 public void forEachRemaining(java.util.function.Consumer<? super E> action){Objects.requireNonNull(action);int end=fence(),from=index;index=end;for(int i=from;i<end;i++){if(i>=elements.length)throw new ConcurrentModificationException();action.accept((E)elements[i]);}if(modCount!=expected)throw new ConcurrentModificationException();}
 }
}
