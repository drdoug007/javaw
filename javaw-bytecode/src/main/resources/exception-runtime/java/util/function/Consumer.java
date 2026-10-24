package java.util.function;import java.util.Objects;
@FunctionalInterface public interface Consumer<T>{void accept(T value);default Consumer<T> andThen(Consumer<? super T> after){Objects.requireNonNull(after);Consumer<T> self=this;return new Consumer<T>(){public void accept(T value){self.accept(value);after.accept(value);}};}}
