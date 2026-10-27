package java.util.stream;
import java.util.Iterator;
public interface BaseStream<T,S extends BaseStream<T,S>> extends AutoCloseable {
    Iterator<T> iterator();
    default java.util.Spliterator<T> spliterator(){throw new UnsupportedOperationException("selected source reference spliterators only");}
    boolean isParallel();
    S sequential();
    S unordered();
    S onClose(Runnable handler);
    void close();
}
