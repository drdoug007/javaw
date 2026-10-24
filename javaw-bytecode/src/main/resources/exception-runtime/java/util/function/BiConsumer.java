package java.util.function;import java.util.Objects;
@FunctionalInterface public interface BiConsumer<T,U>{void accept(T first,U second);default BiConsumer<T,U> andThen(BiConsumer<? super T,? super U> after){Objects.requireNonNull(after);BiConsumer<T,U> self=this;return new BiConsumer<T,U>(){public void accept(T first,U second){self.accept(first,second);after.accept(first,second);}};}}
