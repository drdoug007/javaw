package java.util.function;import java.util.Objects;
@FunctionalInterface public interface Function<T,R>{R apply(T value);
 default <V>Function<V,R> compose(Function<? super V,? extends T> before){Objects.requireNonNull(before);Function<T,R> self=this;return new Function<V,R>(){public R apply(V value){return self.apply(before.apply(value));}};}
 default <V>Function<T,V> andThen(Function<? super R,? extends V> after){Objects.requireNonNull(after);Function<T,R> self=this;return new Function<T,V>(){public V apply(T value){return after.apply(self.apply(value));}};}
 static <T>Function<T,T> identity(){return new Function<T,T>(){public T apply(T value){return value;}};}
}
