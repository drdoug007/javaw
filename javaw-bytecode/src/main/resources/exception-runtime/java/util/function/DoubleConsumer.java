package java.util.function;import java.util.Objects;
@FunctionalInterface public interface DoubleConsumer{void accept(double value);default DoubleConsumer andThen(DoubleConsumer after){Objects.requireNonNull(after);DoubleConsumer self=this;return new DoubleConsumer(){public void accept(double value){self.accept(value);after.accept(value);}};}}
