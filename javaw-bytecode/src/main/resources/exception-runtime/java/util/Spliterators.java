package java.util;
import java.util.function.Consumer;
public final class Spliterators{
 private Spliterators(){}
 private static final Spliterator<Object> EMPTY=new Spliterator<Object>(){
 public boolean tryAdvance(Consumer<? super Object> action){Objects.requireNonNull(action);return false;}public Spliterator<Object> trySplit(){return null;}public long estimateSize(){return 0;}public int characteristics(){return Spliterator.SIZED|Spliterator.SUBSIZED;}};
 public static <T>Spliterator<T> emptySpliterator(){return (Spliterator<T>)EMPTY;}
}
