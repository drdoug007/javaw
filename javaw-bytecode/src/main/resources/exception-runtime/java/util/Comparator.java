package java.util;
import java.util.function.*;
@FunctionalInterface public interface Comparator<T>{
 int compare(T left,T right);boolean equals(Object other);
 default Comparator<T> reversed(){return Collections.reverseOrder(this);}
 default Comparator<T> thenComparing(Comparator<? super T> other){Objects.requireNonNull(other);return new Chain<T>(this,other);}
 default <U>Comparator<T> thenComparing(Function<? super T,? extends U> key,Comparator<? super U> order){return thenComparing(comparing(key,order));}
 default <U extends Comparable<? super U>>Comparator<T> thenComparing(Function<? super T,? extends U> key){return thenComparing(comparing(key));}
 default Comparator<T> thenComparingInt(ToIntFunction<? super T> key){return thenComparing(comparingInt(key));}
 default Comparator<T> thenComparingLong(ToLongFunction<? super T> key){return thenComparing(comparingLong(key));}
 default Comparator<T> thenComparingDouble(ToDoubleFunction<? super T> key){return thenComparing(comparingDouble(key));}
 static <T extends Comparable<? super T>>Comparator<T> naturalOrder(){return (Comparator<T>)Natural.INSTANCE;}
 static <T extends Comparable<? super T>>Comparator<T> reverseOrder(){return Collections.reverseOrder();}
 static <T>Comparator<T> nullsFirst(Comparator<? super T> order){return new NullOrder<T>(true,order);}
 static <T>Comparator<T> nullsLast(Comparator<? super T> order){return new NullOrder<T>(false,order);}
 static <T,U>Comparator<T> comparing(Function<? super T,? extends U> key,Comparator<? super U> order){Objects.requireNonNull(key);Objects.requireNonNull(order);return new KeyOrder<T,U>(key,order);}
 static <T,U extends Comparable<? super U>>Comparator<T> comparing(Function<? super T,? extends U> key){return comparing(key,Comparator.<U>naturalOrder());}
 static <T>Comparator<T> comparingInt(ToIntFunction<? super T> key){Objects.requireNonNull(key);return new IntOrder<T>(key);}
 static <T>Comparator<T> comparingLong(ToLongFunction<? super T> key){Objects.requireNonNull(key);return new LongOrder<T>(key);}
 static <T>Comparator<T> comparingDouble(ToDoubleFunction<? super T> key){Objects.requireNonNull(key);return new DoubleOrder<T>(key);}
 final class Natural implements Comparator<Comparable<Object>>,java.io.Serializable{static final Natural INSTANCE=new Natural();public int compare(Comparable<Object> a,Comparable<Object> b){return a.compareTo(b);}public Comparator<Comparable<Object>> reversed(){return Collections.reverseOrder(this);}}
 final class Chain<T> implements Comparator<T>,java.io.Serializable{final Comparator<? super T> a,b;Chain(Comparator<? super T>a,Comparator<? super T>b){this.a=a;this.b=b;}public int compare(T x,T y){int result=a.compare(x,y);return result==0?b.compare(x,y):result;}}
 final class NullOrder<T> implements Comparator<T>,java.io.Serializable{final boolean first;final Comparator<? super T> order;NullOrder(boolean first,Comparator<? super T>order){this.first=first;this.order=order;}public int compare(T a,T b){if(a==null)return b==null?0:first?-1:1;if(b==null)return first?1:-1;return order==null?0:order.compare(a,b);}public Comparator<T> reversed(){return new NullOrder<T>(!first,order==null?null:Collections.reverseOrder(order));}public Comparator<T> thenComparing(Comparator<? super T> next){Objects.requireNonNull(next);return new NullOrder<T>(first,order==null?next:new Chain<T>(order,next));}}
 final class IntOrder<T> implements Comparator<T>,java.io.Serializable{final ToIntFunction<? super T> key;IntOrder(ToIntFunction<? super T> key){this.key=key;}public int compare(T a,T b){return Integer.compare(key.applyAsInt(a),key.applyAsInt(b));}}
 final class LongOrder<T> implements Comparator<T>,java.io.Serializable{final ToLongFunction<? super T> key;LongOrder(ToLongFunction<? super T> key){this.key=key;}public int compare(T a,T b){return Long.compare(key.applyAsLong(a),key.applyAsLong(b));}}
 final class DoubleOrder<T> implements Comparator<T>,java.io.Serializable{final ToDoubleFunction<? super T> key;DoubleOrder(ToDoubleFunction<? super T> key){this.key=key;}public int compare(T a,T b){return Double.compare(key.applyAsDouble(a),key.applyAsDouble(b));}}
 final class KeyOrder<T,U> implements Comparator<T>,java.io.Serializable{final Function<? super T,? extends U> key;final Comparator<? super U> order;KeyOrder(Function<? super T,? extends U> key,Comparator<? super U> order){this.key=key;this.order=order;}public int compare(T a,T b){return order.compare(key.apply(a),key.apply(b));}}
}
