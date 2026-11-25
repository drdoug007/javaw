package java.util;
import java.util.function.*;
public interface PrimitiveIterator<T,C> extends Iterator<T> {
    void forEachRemaining(C action);
    interface OfInt extends PrimitiveIterator<Integer,IntConsumer> {
        int nextInt();
        default Integer next(){return Integer.valueOf(nextInt());}
        default void forEachRemaining(IntConsumer action){Objects.requireNonNull(action);while(hasNext())action.accept(nextInt());}
        default void forEachRemaining(Consumer<? super Integer> action){Objects.requireNonNull(action);if(action instanceof IntConsumer){forEachRemaining((IntConsumer)action);return;}while(hasNext())action.accept(next());}
    }
    interface OfLong extends PrimitiveIterator<Long,LongConsumer> {
        long nextLong();
        default Long next(){return Long.valueOf(nextLong());}
        default void forEachRemaining(LongConsumer action){Objects.requireNonNull(action);while(hasNext())action.accept(nextLong());}
    }
    interface OfDouble extends PrimitiveIterator<Double,DoubleConsumer> {
        double nextDouble();
        default Double next(){return Double.valueOf(nextDouble());}
        default void forEachRemaining(DoubleConsumer action){Objects.requireNonNull(action);while(hasNext())action.accept(nextDouble());}
    }
}
