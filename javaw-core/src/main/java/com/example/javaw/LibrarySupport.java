package com.example.javaw;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;

/** Source-backed library objects use the same memory layout and dispatch as user classes. */
final class LibrarySupport {
    static void addTypes(CompilationUnit cu) {
        add(cu, "java.util.function", """
            interface Supplier<T> { public T get(); }
            interface Function<T,R> { public R apply(T value); }
            interface Predicate<T> { public boolean test(T value); }
            interface Consumer<T> { public void accept(T value); }
            interface UnaryOperator<T> { public T apply(T value); }
            interface BinaryOperator<T> { public T apply(T left, T right); }
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
