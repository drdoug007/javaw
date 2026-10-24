package java.util.function;import java.util.Objects;
@FunctionalInterface public interface LongConsumer{void accept(long value);default LongConsumer andThen(LongConsumer after){Objects.requireNonNull(after);LongConsumer self=this;return new LongConsumer(){public void accept(long value){self.accept(value);after.accept(value);}};}}
