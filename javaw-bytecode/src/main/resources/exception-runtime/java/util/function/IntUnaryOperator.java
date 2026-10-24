package java.util.function;import java.util.Objects;
@FunctionalInterface public interface IntUnaryOperator{int applyAsInt(int value);
 default IntUnaryOperator compose(IntUnaryOperator before){Objects.requireNonNull(before);IntUnaryOperator self=this;return new IntUnaryOperator(){public int applyAsInt(int value){return self.applyAsInt(before.applyAsInt(value));}};}
 default IntUnaryOperator andThen(IntUnaryOperator after){Objects.requireNonNull(after);IntUnaryOperator self=this;return new IntUnaryOperator(){public int applyAsInt(int value){return after.applyAsInt(self.applyAsInt(value));}};}
 static IntUnaryOperator identity(){return new IntUnaryOperator(){public int applyAsInt(int value){return value;}};}
 }
