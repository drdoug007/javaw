package com.example.javaw;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

/** Source-backed library objects use the same memory layout and dispatch as user classes. */
final class LibrarySupport {
    static void addTypes(CompilationUnit cu) {
        add(cu, "java.util.function", """
            interface Supplier<T> { public T get(); }
            interface Function<T,R> {
                public R apply(T value);
                public default <V> Function<T,V> andThen(Function<R,V> after) {
                    if(after==null) throw new NullPointerException(); return new __JavawCompose<T,R,V>(this,after);
                }
                public default <V> Function<V,R> compose(Function<V,T> before) {
                    if(before==null) throw new NullPointerException(); return new __JavawCompose<V,T,R>(before,this);
                }
                public static <V> Function<V,V> identity() { return new __JavawIdentity<V>(); }
            }
            interface Predicate<T> {
                public boolean test(T value);
                public default Predicate<T> negate() { return new __JavawNegate<T>(this); }
                public default Predicate<T> and(Predicate<T> other) { if(other==null) throw new NullPointerException(); return new __JavawPredicatePair<T>(this,other,true); }
                public default Predicate<T> or(Predicate<T> other) { if(other==null) throw new NullPointerException(); return new __JavawPredicatePair<T>(this,other,false); }
            }
            interface Consumer<T> {
                public void accept(T value);
                public default Consumer<T> andThen(Consumer<T> after) { if(after==null) throw new NullPointerException(); return new __JavawConsumerPair<T>(this,after); }
            }
            interface UnaryOperator<T> extends Function<T,T> {}
            interface BiFunction<T,U,R> { public R apply(T first,U second); }
            interface BiConsumer<T,U> { public void accept(T first,U second); }
            interface BinaryOperator<T> extends BiFunction<T,T,T> {}
            class __JavawCompose<T,U,R> implements Function<T,R> {
                private Function<T,U> before; private Function<U,R> after;
                public __JavawCompose(Function<T,U> before,Function<U,R> after) { this.before=before; this.after=after; }
                public R apply(T value) { return after.apply(before.apply(value)); }
            }
            class __JavawIdentity<T> implements Function<T,T> { public __JavawIdentity() {} public T apply(T value) { return value; } }
            class __JavawNegate<T> implements Predicate<T> {
                private Predicate<T> predicate;
                public __JavawNegate(Predicate<T> predicate) { this.predicate=predicate; }
                public boolean test(T value) { return !predicate.test(value); }
            }
            class __JavawPredicatePair<T> implements Predicate<T> {
                private Predicate<T> first; private Predicate<T> second; private boolean conjunction;
                public __JavawPredicatePair(Predicate<T> first,Predicate<T> second,boolean conjunction) { this.first=first; this.second=second; this.conjunction=conjunction; }
                public boolean test(T value) { return conjunction ? first.test(value) && second.test(value) : first.test(value) || second.test(value); }
            }
            class __JavawConsumerPair<T> implements Consumer<T> {
                private Consumer<T> first; private Consumer<T> second;
                public __JavawConsumerPair(Consumer<T> first,Consumer<T> second) { this.first=first; this.second=second; }
                public void accept(T value) { first.accept(value); second.accept(value); }
            }
            """);
        add(cu, "java.util", """
            class Optional<T> {
                private T value;
                private Optional(T value) { this.value = value; }
                public static <U> Optional<U> empty() { return new Optional<U>(null); }
                public static <U> Optional<U> of(U value) {
                    if (value == null) throw new NullPointerException();
                    return new Optional<U>(value);
                }
                public static <U> Optional<U> ofNullable(U value) { return new Optional<U>(value); }
                public boolean isPresent() { return value != null; }
                public boolean isEmpty() { return value == null; }
                public T get() { if (value == null) throw new NoSuchElementException(); return value; }
                public T orElse(T fallback) { return value == null ? fallback : value; }
                public T orElseGet(Supplier<T> supplier) { return value == null ? supplier.get() : value; }
                public T orElseThrow() { return get(); }
                public <X extends RuntimeException> T orElseThrow(Supplier<X> supplier) {
                    if (value == null) throw supplier.get(); return value;
                }
                public <R> Optional<R> map(Function<T,R> mapper) {
                    if (mapper == null) throw new NullPointerException();
                    if (value == null) return Optional.<R>empty();
                    return Optional.ofNullable(mapper.apply(value));
                }
                public <R> Optional<R> flatMap(Function<T,Optional<R>> mapper) {
                    if (mapper == null) throw new NullPointerException();
                    if (value == null) return Optional.<R>empty();
                    Optional<R> result = mapper.apply(value);
                    if (result == null) throw new NullPointerException();
                    return result;
                }
                public Optional<T> filter(Predicate<T> predicate) {
                    if (predicate == null) throw new NullPointerException();
                    if (value == null || predicate.test(value)) return this;
                    return Optional.<T>empty();
                }
                public void ifPresent(Consumer<T> action) {
                    if (value != null) action.accept(value);
                }
            }
            """);
    }

    static void add(CompilationUnit cu, String pkg, String source) {
        for (var type : StaticJavaParser.parse(source).getTypes()) {
            if (cu.getTypes().stream().anyMatch(t -> t.getNameAsString().equals(type.getNameAsString()))) continue;
            type.setData(JavaSourcePreparation.PACKAGE, pkg);
            cu.getTypes().add(type.clone());
        }
    }
}
