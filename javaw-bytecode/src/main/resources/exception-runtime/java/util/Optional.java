package java.util;
import java.util.function.*;
public final class Optional<T>{
 private static final Optional<?> EMPTY=new Optional<Object>(null);private final T value;private Optional(T value){this.value=value;}
 public static <T>Optional<T> empty(){return (Optional<T>)EMPTY;}public static <T>Optional<T> of(T value){return new Optional<T>(Objects.requireNonNull(value));}public static <T>Optional<T> ofNullable(T value){return value==null?Optional.<T>empty():new Optional<T>(value);}
 public java.util.stream.Stream<T> stream(){return value==null?java.util.stream.Stream.<T>empty():java.util.stream.Stream.of(value);}
 public boolean isPresent(){return value!=null;}public boolean isEmpty(){return value==null;}public T get(){if(value==null)throw new NoSuchElementException("No value present");return value;}
 public void ifPresent(Consumer<? super T> action){if(value!=null)action.accept(value);}public void ifPresentOrElse(Consumer<? super T> action,Runnable empty){if(value!=null)action.accept(value);else empty.run();}
 public Optional<T> filter(Predicate<? super T> predicate){Objects.requireNonNull(predicate);return value==null || predicate.test(value)?this:Optional.<T>empty();}
 public <U>Optional<U> map(Function<? super T,? extends U> mapper){Objects.requireNonNull(mapper);return value==null?Optional.<U>empty():Optional.ofNullable(mapper.apply(value));}
 public <U>Optional<U> flatMap(Function<? super T,? extends Optional<? extends U>> mapper){Objects.requireNonNull(mapper);return value==null?Optional.<U>empty():(Optional<U>)Objects.requireNonNull(mapper.apply(value));}
 public Optional<T> or(Supplier<? extends Optional<? extends T>> supplier){Objects.requireNonNull(supplier);return value==null?(Optional<T>)Objects.requireNonNull(supplier.get()):this;}
 public T orElse(T fallback){return value==null?fallback:value;}public T orElseGet(Supplier<? extends T> supplier){return value==null?supplier.get():value;}
 public T orElseThrow(){return get();}public <X extends Throwable>T orElseThrow(Supplier<? extends X> supplier)throws X{if(value==null)throw supplier.get();return value;}
 public boolean equals(Object other){return other==this || other instanceof Optional && Objects.equals(value,((Optional<?>)other).value);}public int hashCode(){return Objects.hashCode(value);}public String toString(){return value==null?"Optional.empty":new StringBuilder("Optional[").append(value).append(']').toString();}
}
