package java.util.function;import java.util.Objects;
@FunctionalInterface public interface LongPredicate{boolean test(long value);
 default LongPredicate and(LongPredicate other){Objects.requireNonNull(other);LongPredicate self=this;return new LongPredicate(){public boolean test(long value){return self.test(value)&&other.test(value);}};}
 default LongPredicate or(LongPredicate other){Objects.requireNonNull(other);LongPredicate self=this;return new LongPredicate(){public boolean test(long value){return self.test(value)||other.test(value);}};}
 default LongPredicate negate(){LongPredicate self=this;return new LongPredicate(){public boolean test(long value){return !self.test(value);}};}
 }
