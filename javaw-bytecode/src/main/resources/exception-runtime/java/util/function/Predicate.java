package java.util.function;import java.util.Objects;
@FunctionalInterface public interface Predicate<T>{boolean test(T value);
 default Predicate<T> and(Predicate<? super T> other){Objects.requireNonNull(other);Predicate<T> self=this;return new Predicate<T>(){public boolean test(T value){return self.test(value)&&other.test(value);}};}
 default Predicate<T> or(Predicate<? super T> other){Objects.requireNonNull(other);Predicate<T> self=this;return new Predicate<T>(){public boolean test(T value){return self.test(value)||other.test(value);}};}
 default Predicate<T> negate(){Predicate<T> self=this;return new Predicate<T>(){public boolean test(T value){return !self.test(value);}};}
 static <T>Predicate<T> isEqual(Object target){return new Predicate<T>(){public boolean test(T value){return Objects.equals(target,value);}};}
 static <T>Predicate<T> not(Predicate<? super T> target){return (Predicate<T>)Objects.requireNonNull(target).negate();}
}
