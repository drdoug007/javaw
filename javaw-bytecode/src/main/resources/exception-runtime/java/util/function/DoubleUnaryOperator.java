package java.util.function;import java.util.Objects;
@FunctionalInterface public interface DoubleUnaryOperator{double applyAsDouble(double value);
 default DoubleUnaryOperator compose(DoubleUnaryOperator before){Objects.requireNonNull(before);DoubleUnaryOperator self=this;return new DoubleUnaryOperator(){public double applyAsDouble(double value){return self.applyAsDouble(before.applyAsDouble(value));}};}
 default DoubleUnaryOperator andThen(DoubleUnaryOperator after){Objects.requireNonNull(after);DoubleUnaryOperator self=this;return new DoubleUnaryOperator(){public double applyAsDouble(double value){return after.applyAsDouble(self.applyAsDouble(value));}};}
 static DoubleUnaryOperator identity(){return new DoubleUnaryOperator(){public double applyAsDouble(double value){return value;}};}
 }
