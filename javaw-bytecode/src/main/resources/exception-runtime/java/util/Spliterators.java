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
}
