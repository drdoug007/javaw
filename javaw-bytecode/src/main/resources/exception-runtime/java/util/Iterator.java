package java.util;
public interface Iterator<E>{boolean hasNext();E next();default void forEachRemaining(java.util.function.Consumer<? super E> action){Objects.requireNonNull(action);while(hasNext())action.accept(next());}default void remove(){throw new UnsupportedOperationException();}}
