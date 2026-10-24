package java.util.function;import java.util.Objects;
@FunctionalInterface public interface LongUnaryOperator{long applyAsLong(long value);
 default LongUnaryOperator compose(LongUnaryOperator before){Objects.requireNonNull(before);LongUnaryOperator self=this;return new LongUnaryOperator(){public long applyAsLong(long value){return self.applyAsLong(before.applyAsLong(value));}};}
 default LongUnaryOperator andThen(LongUnaryOperator after){Objects.requireNonNull(after);LongUnaryOperator self=this;return new LongUnaryOperator(){public long applyAsLong(long value){return after.applyAsLong(self.applyAsLong(value));}};}
 static LongUnaryOperator identity(){return new LongUnaryOperator(){public long applyAsLong(long value){return value;}};}
 }
