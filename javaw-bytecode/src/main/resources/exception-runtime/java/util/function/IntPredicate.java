package java.util.function;import java.util.Objects;
@FunctionalInterface public interface IntPredicate{boolean test(int value);
 default IntPredicate and(IntPredicate other){Objects.requireNonNull(other);IntPredicate self=this;return new IntPredicate(){public boolean test(int value){return self.test(value)&&other.test(value);}};}
 default IntPredicate or(IntPredicate other){Objects.requireNonNull(other);IntPredicate self=this;return new IntPredicate(){public boolean test(int value){return self.test(value)||other.test(value);}};}
 default IntPredicate negate(){IntPredicate self=this;return new IntPredicate(){public boolean test(int value){return !self.test(value);}};}
 }
