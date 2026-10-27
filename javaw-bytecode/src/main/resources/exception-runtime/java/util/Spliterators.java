package java.util;
import java.util.function.Consumer;
public final class Spliterators{
 private Spliterators(){}
 private static final Spliterator<Object> EMPTY=new Spliterator<Object>(){
 public boolean tryAdvance(Consumer<? super Object> action){Objects.requireNonNull(action);return false;}public Spliterator<Object> trySplit(){return null;}public long estimateSize(){return 0;}public int characteristics(){return Spliterator.SIZED|Spliterator.SUBSIZED;}};
 public static <T>Spliterator<T> emptySpliterator(){return (Spliterator<T>)EMPTY;}

 public static <T>Spliterator<T> spliterator(Object[] array,int characteristics){return spliterator(array,0,array.length,characteristics);}
 public static <T>Spliterator<T> spliterator(Object[] array,int start,int end,int characteristics){Objects.requireNonNull(array);if(start>end||start<0||end>array.length)throw new ArrayIndexOutOfBoundsException();return new ArrayCursor<T>(array,start,end,characteristics);}
 static final class ArrayCursor<T> implements Spliterator<T>{
 final Object[] array;int index;final int fence,flags;
 ArrayCursor(Object[] array,int start,int fence,int flags){this.array=array;index=start;this.fence=fence;this.flags=flags|Spliterator.SIZED|Spliterator.SUBSIZED;}
 public boolean tryAdvance(Consumer<? super T> action){Objects.requireNonNull(action);if(index==fence)return false;T value=(T)array[index++];action.accept(value);return true;}
 public void forEachRemaining(Consumer<? super T> action){Objects.requireNonNull(action);int from=index;index=fence;for(int i=from;i<fence;i++)action.accept((T)array[i]);}
 public Spliterator<T> trySplit(){int middle=(index+fence)>>>1;if(index>=middle)return null;int from=index;index=middle;return new ArrayCursor<T>(array,from,middle,flags);}
 public long estimateSize(){return fence-index;}public int characteristics(){return flags;}
 public Comparator<? super T> getComparator(){if((flags&Spliterator.SORTED)==0)throw new IllegalStateException();return null;}
 }

 public static <T>Spliterator<T> spliterator(Iterator<? extends T> iterator,long size,int flags){return new IteratorCursor<T>(Objects.requireNonNull(iterator),size,(flags&Spliterator.CONCURRENT)==0?flags|Spliterator.SIZED|Spliterator.SUBSIZED:flags);}
 public static <T>Spliterator<T> spliteratorUnknownSize(Iterator<? extends T> iterator,int flags){return new IteratorCursor<T>(Objects.requireNonNull(iterator),Long.MAX_VALUE,flags&~(Spliterator.SIZED|Spliterator.SUBSIZED));}
 static final class IteratorCursor<T> implements Spliterator<T>{
 Iterator<? extends T> iterator;long estimate;final int flags;int batch;
 IteratorCursor(Iterator<? extends T> iterator,long size,int flags){this.iterator=iterator;estimate=size;this.flags=flags;}
 public boolean tryAdvance(Consumer<? super T> action){Objects.requireNonNull(action);if(!iterator.hasNext())return false;action.accept(iterator.next());return true;}
 public void forEachRemaining(Consumer<? super T> action){iterator.forEachRemaining(action);}
 public Spliterator<T> trySplit(){if(estimate<=1||!iterator.hasNext())return null;int n=batch+1024;if(n>estimate)n=(int)estimate;if(n>33554432)n=33554432;Object[] values=new Object[n];int count=0;do{values[count++]=iterator.next();}while(count<n&&iterator.hasNext());batch=count;if(estimate!=Long.MAX_VALUE)estimate-=count;return new ArrayCursor<T>(values,0,count,flags);}
 public long estimateSize(){return estimate;}public int characteristics(){return flags;}public Comparator<? super T> getComparator(){if((flags&Spliterator.SORTED)==0)throw new IllegalStateException();return null;}
 }
 public static <T>Iterator<T> iterator(Spliterator<? extends T> source){Objects.requireNonNull(source);return new Adapter<T>(source);}
 private static final class Adapter<T> implements Iterator<T>,Consumer<T>{
 final Spliterator<? extends T> source;boolean ready;T value;Adapter(Spliterator<? extends T> source){this.source=source;}
 public void accept(T value){ready=true;this.value=value;}public boolean hasNext(){if(!ready)source.tryAdvance(this);return ready;}public T next(){if(!hasNext())throw new NoSuchElementException();ready=false;T result=value;value=null;return result;}
 }
}
