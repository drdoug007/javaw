package java.util;
public abstract class AbstractCollection<E> implements Collection<E>{
 protected AbstractCollection(){}
 public boolean isEmpty(){return size()==0;}
 public boolean contains(Object value){for(E item:this)if(Objects.equals(value,item))return true;return false;}
 public boolean add(E value){throw new UnsupportedOperationException();}
 public boolean remove(Object value){Iterator<E> iterator=iterator();while(iterator.hasNext())if(Objects.equals(value,iterator.next())){iterator.remove();return true;}return false;}
 public boolean containsAll(Collection<?> values){for(Object value:values)if(!contains(value))return false;return true;}
 public boolean addAll(Collection<? extends E> values){boolean changed=false;for(E value:values)changed|=add(value);return changed;}
 public boolean removeAll(Collection<?> values){if(values==null)throw new NullPointerException();boolean changed=false;Iterator<E> iterator=iterator();while(iterator.hasNext())if(values.contains(iterator.next())){iterator.remove();changed=true;}return changed;}
 public boolean retainAll(Collection<?> values){if(values==null)throw new NullPointerException();boolean changed=false;Iterator<E> iterator=iterator();while(iterator.hasNext())if(!values.contains(iterator.next())){iterator.remove();changed=true;}return changed;}
 public void clear(){Iterator<E> iterator=iterator();while(iterator.hasNext()){iterator.next();iterator.remove();}}
 public Object[] toArray(){Object[] result=new Object[size()];int index=0;for(E value:this)result[index++]=value;return result;}
 public <T>T[] toArray(T[] array){int size=size();T[] result=array.length<size?(T[])java.lang.reflect.Array.newInstance(array.getClass().getComponentType(),size):array;int index=0;for(E value:this)result[index++]=(T)value;if(result.length>size)result[size]=null;return result;}
 public String toString(){StringBuilder result=new StringBuilder("[");Iterator<E> iterator=iterator();while(iterator.hasNext()){E value=iterator.next();result.append(value==this?"(this Collection)":String.valueOf(value));if(iterator.hasNext())result.append(", ");}return result.append(']').toString();}
}
