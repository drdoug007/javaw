package java.util.function;import java.util.Objects;
@FunctionalInterface public interface BiFunction<T,U,R>{R apply(T first,U second);default <V>BiFunction<T,U,V> andThen(Function<? super R,? extends V> after){Objects.requireNonNull(after);BiFunction<T,U,R> self=this;return new BiFunction<T,U,V>(){public V apply(T first,U second){return after.apply(self.apply(first,second));}};}}
