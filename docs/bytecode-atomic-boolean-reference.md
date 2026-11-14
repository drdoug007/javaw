# Bytecode AtomicBoolean and AtomicReference

Revision `atomic-boolean-reference-v287` links `java.util.concurrent.atomic.AtomicBoolean` and
`java.util.concurrent.atomic.AtomicReference` in the bundled exception runtime. Like `AtomicReferenceArray`, this
contract applies only to the **single-threaded** deployment: every access mode is a plain field access, and no
Java Memory Model ordering is claimed.

Contract:

- `AtomicBoolean` keeps one private `boolean` field. `get`/`set`/`lazySet`, `getPlain`/`setPlain`,
  `getOpaque`/`setOpaque` and `getAcquire`/`setRelease` are plain reads and writes of that field; `getAndSet`
  returns the old value and stores the new one. `compareAndSet(e, n)` stores `n` and returns `true` only when the
  value already is `e` — never a spurious `false` — and all five `weakCompareAndSet*` forms behave exactly like
  `compareAndSet`. `compareAndExchange*` return the old value and store `n` only when the old value was `e`.
  `toString()` is `Boolean.toString(get())`. `equals`/`hashCode` are `Object`'s; the class is
  `java.io.Serializable`. `weakCompareAndSet`, `weakCompareAndSetPlain` and `toString` are the non-final hooks the
  JDK allows subclasses to override; no other method calls them, so an overriding subclass's log stays empty
  until the test invokes them.
- `AtomicReference<V>` keeps one private field starting at `null`. `getAndSet` returns the old value and every
  access mode is a plain read or write. `compareAndSet`, every `weakCompareAndSet*` and every
  `compareAndExchange*` compare with `==` (reference identity) and never call `equals`;
  `compareAndExchange*` return the old object itself and store the new value only when the old was the expected
  reference. `toString()` is `String.valueOf(get())`: `"null"` for `null`, otherwise exactly one call of the
  value's own `toString()` per call. `equals`/`hashCode` are `Object`'s.
- `getAndUpdate`/`updateAndGet`/`getAndAccumulate`/`accumulateAndGet` follow the JDK's compare-and-set retry loop
  exactly: read `prev = get()`, compute `next = f.apply(prev)` (accumulators: `apply(prev, x)`, current value
  first), and `compareAndSet(prev, next)` — returning `prev` for the `getAnd*` forms and `next` for the `*AndGet`
  forms, reloading `prev` and retrying on failure. A function that mutates the same reference while it runs is
  therefore called again with the new value. A `null` result is stored, a `null` current value is passed to the
  function unchanged, a throwing function propagates with nothing stored, and a `null` function throws
  `NullPointerException` from `apply`.

Evidence: `BytecodeAtomicBooleanReferenceTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
orders, two fresh instances), including library-style subclasses that override the non-final members and
update functions that modify the reference they update.
