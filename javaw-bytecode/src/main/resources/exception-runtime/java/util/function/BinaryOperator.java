package java.util.function;import java.util.*;@FunctionalInterface public interface BinaryOperator<T> extends BiFunction<T,T,T>{
 static <T>BinaryOperator<T> minBy(Comparator<? super T> comparator){Objects.requireNonNull(comparator);return new BinaryOperator<T>(){public T apply(T a,T b){return comparator.compare(a,b)<=0?a:b;}};}
 static <T>BinaryOperator<T> maxBy(Comparator<? super T> comparator){Objects.requireNonNull(comparator);return new BinaryOperator<T>(){public T apply(T a,T b){return comparator.compare(a,b)>=0?a:b;}};}
}
