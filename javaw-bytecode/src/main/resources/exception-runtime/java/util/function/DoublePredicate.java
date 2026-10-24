package java.util.function;import java.util.Objects;
@FunctionalInterface public interface DoublePredicate{boolean test(double value);
 default DoublePredicate and(DoublePredicate other){Objects.requireNonNull(other);DoublePredicate self=this;return new DoublePredicate(){public boolean test(double value){return self.test(value)&&other.test(value);}};}
 default DoublePredicate or(DoublePredicate other){Objects.requireNonNull(other);DoublePredicate self=this;return new DoublePredicate(){public boolean test(double value){return self.test(value)||other.test(value);}};}
 default DoublePredicate negate(){DoublePredicate self=this;return new DoublePredicate(){public boolean test(double value){return !self.test(value);}};}
 }
