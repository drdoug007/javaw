package java.util.function;import java.util.Objects;
@FunctionalInterface public interface BiPredicate<T,U>{boolean test(T first,U second);
 default BiPredicate<T,U> and(BiPredicate<? super T,? super U> other){Objects.requireNonNull(other);BiPredicate<T,U> self=this;return new BiPredicate<T,U>(){public boolean test(T a,U b){return self.test(a,b)&&other.test(a,b);}};}
 default BiPredicate<T,U> or(BiPredicate<? super T,? super U> other){Objects.requireNonNull(other);BiPredicate<T,U> self=this;return new BiPredicate<T,U>(){public boolean test(T a,U b){return self.test(a,b)||other.test(a,b);}};}
 default BiPredicate<T,U> negate(){BiPredicate<T,U> self=this;return new BiPredicate<T,U>(){public boolean test(T a,U b){return !self.test(a,b);}};}
}
