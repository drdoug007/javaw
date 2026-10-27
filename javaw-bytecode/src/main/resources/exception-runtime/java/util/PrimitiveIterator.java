package java.util;
import java.util.function.*;
public interface PrimitiveIterator<T,C> extends Iterator<T> {
    void forEachRemaining(C action);
    interface OfInt extends PrimitiveIterator<Integer,IntConsumer> {
        int nextInt();
        default Integer next(){return Integer.valueOf(nextInt());}
        default void forEachRemaining(IntConsumer action){Objects.requireNonNull(action);while(hasNext())action.accept(nextInt());}
        default void forEachRemaining(Consumer<? super Integer> action){Objects.requireNonNull(action);while(hasNext())action.accept(next());}
    }
}
