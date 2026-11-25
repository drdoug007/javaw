# T72 ThreadLocal and InheritableThreadLocal

```kit
id: T72
slug: thread-local
title: link java.lang.ThreadLocal and InheritableThreadLocal (single-threaded)
tests: BytecodeThreadLocalTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/lang/ThreadLocal.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/lang/InheritableThreadLocal.java
allow: docs/bytecode-thread-local.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-thread-local-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeThreadLocalTest.java`.

## Goal

Add `java.lang.ThreadLocal` and `java.lang.InheritableThreadLocal` to the bundled runtime under the
single-threaded contract (Wasm runs one thread, so every `ThreadLocal` holds exactly one value slot).
`runtime-target/target/audit/member-references.tsv` has 92 rows on `java/lang/ThreadLocal` and 8 on
`java/lang/InheritableThreadLocal` across 27 artifacts. `ThreadLocal.get()` 27 rows / 227 sites (snakeyaml
`UnicodeEscaper.escapeSlow`, Spring, Hibernate, logback, log4j, Tomcat, Jackson, HikariCP), `set(Object)` 23 rows /
140 sites (Spring `ScriptTemplateView.getEngine`, `TransactionSynchronizationManager`), `remove()` 16 rows / 62 sites
(Spring `RequestContextHolder.resetRequestAttributes`), `<init>()` 18 rows / 60 sites (snakeyaml
`UnicodeEscaper$2.<init>`, every anonymous subclass that overrides `initialValue`), and
`withInitial(Supplier)` 8 rows / 28 sites (Spring `BeanDefinitionPropertiesCodeGenerator$PropertyNamesStack.<clinit>`).
`InheritableThreadLocal.<init>()` 5 rows (Spring `NamedInheritableThreadLocal.<init>(String)`, used by
`RequestContextHolder.<clinit>`), `get()`, `set(Object)` and `remove()` in slf4j `BasicMDCAdapter`; logback
`CopyOnInheritThreadLocal` overrides `childValue`. Spring `NamedThreadLocal`, H2 `DebuggingThreadLocal` and Spring Boot
`SpringBootExceptionHandler$LoggedExceptionHandlerThreadLocal` subclass `ThreadLocal` and override `initialValue`.

Copy the JDK 21 *observable* behaviour: `initialValue()` is called lazily on the first `get()` and again after
`remove()`, never by `set`; `set(null)` stores a value (a later `get()` returns `null` without calling
`initialValue`); `withInitial` returns a `ThreadLocal$SuppliedThreadLocal`; and exactly the JDK's calls to the
overridable methods of `this` (the test logs a subclass).

## Members to implement

1. `javaw-bytecode/src/main/resources/exception-runtime/java/lang/ThreadLocal.java`:
   `public class ThreadLocal<T>` with

   - fields `private boolean present;` and `private T value;` (the single slot; `present` says whether a value,
     possibly `null`, is stored)
   - `public ThreadLocal()`
   - `protected T initialValue()` (returns `null`)
   - `public static <S> ThreadLocal<S> withInitial(Supplier<? extends S> supplier)`
   - `public T get()`
   - `private T setInitialValue()`
   - `public void set(T value)`
   - `public void remove()`
   - `static final class SuppliedThreadLocal<T> extends ThreadLocal<T>` with
     `private final Supplier<? extends T> supplier;`, constructor `SuppliedThreadLocal(Supplier<? extends T> supplier)`
     and `protected T initialValue()` overriding.

2. `javaw-bytecode/src/main/resources/exception-runtime/java/lang/InheritableThreadLocal.java`:
   `public class InheritableThreadLocal<T> extends ThreadLocal<T>` with

   - `public InheritableThreadLocal()`
   - `protected T childValue(T parentValue)` (returns `parentValue`)

## Members to leave out

- Everything about threads: `java.lang.Thread` is **not** in the runtime and must not be referenced (no
  `Thread.currentThread()`, no `Thread` parameter types). Leave out `ThreadLocalMap`, `getMap(Thread)`,
  `createMap(Thread, T)`, `createInheritedMap`, `threadLocalHashCode`, `nextHashCode()`, `HASH_INCREMENT`,
  `isPresent`, `isCarrierThreadLocal` and the virtual-thread/carrier variants. The test checks that
  `Thread.currentThread()`, `ThreadLocal.nextHashCode()`, `ThreadLocal.getMap(Thread)` and
  `InheritableThreadLocal.createMap(Thread, Object)` stay unlinked. Do not create `Thread.java`.
- `childValue` is only ever called by the JDK when a new `Thread` copies its parent's inheritable values. There is
  no second thread here, so the runtime never calls it; keep the method (subclasses such as logback's
  `CopyOnInheritThreadLocal` override it and call `super.childValue`) and document that.
- Do not override `toString`, `equals` or `hashCode` (the JDK uses `Object`'s).
- Do not edit any other runtime file (`kit guard` rejects it).

## JDK behaviour to match

- `get()`: if a value is stored (`present`), return it (even `null`). Otherwise call the private
  `setInitialValue()`: `T v = initialValue();` then store `v` (`value = v; present = true;`) and return `v`.
  **Do not call `set(v)`**: the JDK stores the value directly (the `Spy` subclass logs `set:` and must not see it).
  If `initialValue()` throws, nothing is stored, the exception propagates unchanged, and the next `get()` calls
  `initialValue()` again.
- If `initialValue()` itself calls `this.set("inner")` and returns `"outer"`, `get()` returns `"outer"` and keeps
  it (the store after `initialValue` wins).
- `set(v)`: store `v` (including `null`), `present = true`. Does not call `get` or `initialValue`.
- `remove()`: `value = null; present = false;` (no-op when nothing is stored; safe to call repeatedly). Does not call
  `get`, `set` or `initialValue`. The next `get()` calls `initialValue()` again.
- Each `ThreadLocal` instance has its own slot; instances are independent.
- `withInitial(supplier)`: `return new SuppliedThreadLocal<>(supplier);`. The constructor does
  `this.supplier = Objects.requireNonNull(supplier);`, so a `null` supplier throws `NullPointerException` at the
  `withInitial` call. `SuppliedThreadLocal.initialValue()` returns `supplier.get()` (called lazily on the first
  `get()`, once per `remove()`; a supplier returning `null` is not called again). The class name is
  `java.lang.ThreadLocal$SuppliedThreadLocal` and its superclass is `ThreadLocal` (the test prints both).
- `InheritableThreadLocal` behaves exactly like `ThreadLocal` for `get`/`set`/`remove`/`initialValue` (it inherits
  them). `childValue(p)` returns `p` (same reference, `null` for `null`) and is never called by the runtime.
- Overridable self-calls (the `Spy` subclass logs `initialValue`, `get`, `set` and `remove`): only `get()` ->
  `initialValue()` (when nothing is stored). `set`, `remove` and `withInitial` call none of them.

## Steps

1. Read the test, then `java/util/function/Supplier.java` and, for style, `java/lang/Void.java`.
2. Create `java/lang/ThreadLocal.java` and `java/lang/InheritableThreadLocal.java` under
   `javaw-bytecode/src/main/resources/exception-runtime/` with the members above.
3. Register both (do not edit `BytecodeExceptionRuntime.java` by hand):
   - `javaw-agent-kit/bin/kit register java/lang/ThreadLocal.java`
   - `javaw-agent-kit/bin/kit register java/lang/InheritableThreadLocal.java`
4. `javaw-agent-kit/bin/kit check T72` until PASS.
5. Write `docs/bytecode-thread-local.md` (below), the report, then `javaw-agent-kit/bin/kit finish T72`.

## Hints

- Import `java.util.Objects` and `java.util.function.Supplier` in `ThreadLocal.java`. `InheritableThreadLocal` is in
  the same package and needs no imports.
- A single `T value` field is not enough: you need the `present` flag, because `set(null)` must stop
  `initialValue()` from being called while `remove()` must start it again.
- `SuppliedThreadLocal` must be a `static final` nested class of `ThreadLocal` (not a lambda or an anonymous class),
  otherwise the class name in the `withInitial` fixture differs.
- Easy calibration mistakes: `get()` written as `set(initialValue())` (the `Spy` log shows an extra `set:`);
  storing the value before calling `initialValue()`, or storing it when `initialValue()` throws (the `Failing`
  fixture then never retries); `remove()` leaving `present` true.
- The Wasm runner stops at the first failing fixture of each `@Test` method; after a fix another fixture may fail next.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
getSetRemove, initialValues, inheritableThreadLocal, leavesOutThreadInternals: if FIX THE RUNTIME says `link the symbolic class/interface java/lang/ThreadLocal` (or `InheritableThreadLocal`, `ThreadLocal$SuppliedThreadLocal`), `link a complete acyclic type hierarchy` or names one of their methods, a class is missing or not registered: run `javaw-agent-kit/bin/kit register` for both files (never edit BytecodeExceptionRuntime.java by hand) and add every member listed under Members to implement. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
getSetRemove: set(null) stores null (get returns null), remove clears the slot, and each ThreadLocal instance has its own slot.
initialValues: get() calls initialValue() only when nothing is stored, stores the result directly without calling set(), and stores nothing when initialValue() throws; remove() makes the next get() call initialValue() again; withInitial(null) throws NullPointerException via Objects.requireNonNull and the result is a static nested class named ThreadLocal$SuppliedThreadLocal.
inheritableThreadLocal: InheritableThreadLocal extends ThreadLocal and inherits get/set/remove/initialValue; childValue(p) returns p and is never called by the runtime.
leavesOutThreadInternals: this fails when Thread, ThreadLocal.nextHashCode, ThreadLocal.getMap or InheritableThreadLocal.createMap became linked. Do not add Thread, ThreadLocalMap or the map methods.
```

## Docs

Create `docs/bytecode-thread-local.md` with this skeleton (fill in the brackets):

    # Bytecode ThreadLocal

    Revision `thread-local-vN` links `java.lang.ThreadLocal` (with `ThreadLocal$SuppliedThreadLocal`) and
    `java.lang.InheritableThreadLocal` in the bundled exception runtime under the single-threaded contract.

    Contract:

    - <one value slot per instance with a present flag; lazy initialValue on get and after remove; set(null) stores
      null; withInitial and the null-supplier NullPointerException; the overridable self-calls>

    Known differences: there is only one thread, so each ThreadLocal is a single field pair instead of an entry in a
    per-thread ThreadLocalMap; values are not weakly referenced and are not discarded with a thread.
    `InheritableThreadLocal.childValue` is kept for subclasses but never called, because no child thread is ever
    created. `java.lang.Thread`, `ThreadLocalMap` and the map/hash internals are not linked.

    Evidence: `BytecodeThreadLocalTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
    fresh instances). `Thread.currentThread()`, `ThreadLocal.nextHashCode()`, `ThreadLocal.getMap(Thread)` and
    `InheritableThreadLocal.createMap(Thread, Object)` are rejected before WAT generation.
