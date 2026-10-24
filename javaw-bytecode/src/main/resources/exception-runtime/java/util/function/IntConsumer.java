package java.util.function;import java.util.Objects;
@FunctionalInterface public interface IntConsumer{void accept(int value);default IntConsumer andThen(IntConsumer after){Objects.requireNonNull(after);IntConsumer self=this;return new IntConsumer(){public void accept(int value){self.accept(value);after.accept(value);}};}}
