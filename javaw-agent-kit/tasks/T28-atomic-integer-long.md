# T28 AtomicInteger and AtomicLong

```kit
id: T28
slug: atomic-integer-long
title: link java.util.concurrent.atomic.AtomicInteger and AtomicLong (single-threaded)
tests: BytecodeAtomicIntegerLongTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/atomic/AtomicInteger.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/atomic/AtomicLong.java
allow: docs/bytecode-atomic-integer-long.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-atomic-integer-long-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeAtomicIntegerLongTest.java`.

## Goal

Add `java.util.concurrent.atomic.AtomicInteger` and `java.util.concurrent.atomic.AtomicLong` to the bundled runtime.
The pinned libraries name `AtomicInteger` from 69 classes and `AtomicLong` from 67
(`runtime-target/target/audit/external-types.tsv`). The member rows (`member-references.tsv`) are dominated by
`AtomicLong.get()` (102 call sites), `AtomicInteger.get()` (77), `<init>()` (45 and 40), `incrementAndGet()` (43 and
34), `<init>(J)` (30), `<init>(I)` (28), `AtomicLong.compareAndSet` (25), `set`, `decrementAndGet`,
`getAndIncrement`, `addAndGet`, `lazySet`, `getAndSet`, `getAndAdd`, `getAndUpdate`, `updateAndGet`, `toString` and
`longValue`. Tomcat (23 sites), H2 (16: `JdbcConnectionPool.getConnection`, `Database.getTempTableName`,
`MVStore.setOldestVersionToKeep`, `StreamStore.getAndIncrementNextKey`, `Transaction.logUndo`), Spring, Hibernate,
HikariCP, logback and Jackson (`AtomicIntegerSerializer`, `PrivateMaxEntriesMap`) use them.

The runtime is **single-threaded**, the same contract as the existing runtime
`java/util/concurrent/atomic/AtomicReferenceArray.java` (see `docs/bytecode-atomic-reference-array.md`): the value is
a plain `private int value;` / `private long value;` field, every access mode (plain, opaque, acquire, release,
volatile) is a plain field read or write, and the compare-and-set family compares with `==` and never fails
spuriously. No `volatile` or `synchronized` (RULES.md); `Unsafe` and `VarHandle` do not exist in the runtime.

## Members to implement

Two files, both `package java.util.concurrent.atomic;`. Each class is **not** final (libraries subclass it), and its
methods are `final` exactly where the JDK's are: every method below is `public final` except the constructors,
`toString`, `intValue`, `longValue`, `floatValue` and `doubleValue`, which are plain `public` (subclasses override
them; the test does).

`java/util/concurrent/atomic/AtomicInteger.java`:
`public class AtomicInteger extends Number implements java.io.Serializable`

- `private int value;`
- `public AtomicInteger(int initialValue)` and `public AtomicInteger()` (value 0)
- `public final int get()`, `public final void set(int newValue)`, `public final void lazySet(int newValue)`
- `public final int getAndSet(int newValue)`
- `public final boolean compareAndSet(int expectedValue, int newValue)`
- `@Deprecated(since="9") public final boolean weakCompareAndSet(int expectedValue, int newValue)`
- `public final boolean weakCompareAndSetPlain(int expectedValue, int newValue)`
- `public final boolean weakCompareAndSetVolatile(int expectedValue, int newValue)`
- `public final boolean weakCompareAndSetAcquire(int expectedValue, int newValue)`
- `public final boolean weakCompareAndSetRelease(int expectedValue, int newValue)`
- `public final int compareAndExchange(int expectedValue, int newValue)`
- `public final int compareAndExchangeAcquire(int expectedValue, int newValue)`
- `public final int compareAndExchangeRelease(int expectedValue, int newValue)`
- `public final int getAndIncrement()`, `getAndDecrement()`, `getAndAdd(int delta)`
- `public final int incrementAndGet()`, `decrementAndGet()`, `addAndGet(int delta)`
- `public final int getAndUpdate(java.util.function.IntUnaryOperator updateFunction)`
- `public final int updateAndGet(java.util.function.IntUnaryOperator updateFunction)`
- `public final int getAndAccumulate(int x, java.util.function.IntBinaryOperator accumulatorFunction)`
- `public final int accumulateAndGet(int x, java.util.function.IntBinaryOperator accumulatorFunction)`
- `public final int getPlain()`, `public final void setPlain(int newValue)`
- `public final int getOpaque()`, `public final void setOpaque(int newValue)`
- `public final int getAcquire()`, `public final void setRelease(int newValue)`
- `public String toString()`, `public int intValue()`, `public long longValue()`, `public float floatValue()`,
  `public double doubleValue()`

`java/util/concurrent/atomic/AtomicLong.java`:
`public class AtomicLong extends Number implements java.io.Serializable` with exactly the same members, with `long`
in place of `int` for the value, `expectedValue`, `newValue`, `delta` and `x`, and
`java.util.function.LongUnaryOperator` / `java.util.function.LongBinaryOperator` as the function types.
`intValue()` still returns `int`, `floatValue()` `float` and `doubleValue()` `double`.

## Members to leave out

- `byteValue()` and `shortValue()`: do **not** declare them. They are inherited from the runtime `java.lang.Number`
  (`(byte) intValue()` and `(short) intValue()`), exactly as in the JDK; the test checks that a subclass's
  `intValue()` override is what they call.
- `equals` and `hashCode`: do not override them (the JDK does not; `a.equals(new AtomicInteger(a.get()))` is
  `false`).
- The JDK's private `Unsafe`/`VALUE` fields, `serialVersionUID` and `AtomicLong.VM_SUPPORTS_LONG_CAS`.
- Do not edit any other runtime file (`kit guard` rejects it), in particular not `java/lang/Number.java` and not
  `AtomicReferenceArray.java`.

## JDK behaviour to match

- Constructors store the value; the no-argument constructors start at `0`.
- `get`, `getPlain`, `getOpaque`, `getAcquire` return `value`. `set`, `lazySet`, `setPlain`, `setOpaque`,
  `setRelease` assign it. `getAndSet(v)` returns the old value and stores `v`.
- Arithmetic wraps silently like Java `int`/`long` arithmetic, no exceptions:
  `new AtomicInteger(Integer.MAX_VALUE).incrementAndGet()` is `-2147483648`, `Long.MIN_VALUE` `getAndDecrement()`
  returns `-9223372036854775808` and leaves `9223372036854775807`.
  `getAndIncrement`/`getAndDecrement`/`getAndAdd(d)` return the value **before** the change;
  `incrementAndGet`/`decrementAndGet`/`addAndGet(d)` return the value **after** it. `getAndAdd(0)` changes nothing.
- `compareAndSet(expected, v)` and every `weakCompareAndSet*` variant: if `value == expected`, store `v` and return
  `true`; otherwise change nothing and return `false`. `compareAndSet(7, 7)` on 7 returns `true`.
- `compareAndExchange*(expected, v)`: return the old value (the "witness") either way, and store `v` only when the
  old value `== expected`. On 1, `compareAndExchange(2, 9)` returns 1 and keeps 1; `compareAndExchange(1, 2)` returns
  1 and stores 2.
- `getAndUpdate(f)`, `updateAndGet(f)`, `getAndAccumulate(x, f)`, `accumulateAndGet(x, f)` follow the JDK retry loop,
  which matters when the function itself changes this atomic (the test does that):

      int prev = value;
      while (true) {
          int next = updateFunction.applyAsInt(prev);   // accumulate: accumulatorFunction.applyAsInt(prev, x)
          if (value == prev) { value = next; return prev; }   // ...AndGet forms return next
          prev = value;                                   // the function changed the value: call it again
      }

  So the function is called once in normal use, with the current value (and for accumulate: `(prev, x)` in that
  order, `prev` first, `x` second; on 20, `getAndAccumulate(3, (a, b) -> a - b)` calls it with `(20, 3)` and stores
  17). If the function sets the atomic to a **different** value, the result is discarded and the function is called
  again with that new value; if it sets the **same** value, its result is kept (one call). Example on 5:
  `getAndUpdate(x -> { if (first call) a.set(100); return x + 1; })` calls it with 5 and then 100, returns 100 and
  stores 101.
- A `null` function throws `NullPointerException` (calling it does that; do not add a message) and leaves the value
  unchanged. An exception thrown by the function propagates unchanged and leaves the value unchanged.
- `toString()`: `Integer.toString(get())` / `Long.toString(get())` (decimal, `-7`, `-9223372036854775808`).
- `AtomicInteger`: `intValue()` is `get()`, `longValue()` is `(long) get()`, `floatValue()` is `(float) get()`
  (16777217 gives `1.6777216E7`), `doubleValue()` is `(double) get()`.
- `AtomicLong`: `intValue()` is `(int) get()` (the low 32 bits: 4294967295 gives -1, 4294967301 gives 5),
  `longValue()` is `get()`, `floatValue()` is `(float) get()` (`Long.MAX_VALUE` gives `9.223372E18`),
  `doubleValue()` is `(double) get()` (`9.223372036854776E18`).
- None of these call each other: `floatValue()`, `doubleValue()`, `longValue()` and `toString()` read the value
  directly (call `get()`, which is final), never `intValue()`/`longValue()`. Only the inherited `byteValue()` and
  `shortValue()` call `intValue()`. With a subclass that logs each Number method and calls `super`,
  `byteValue(); shortValue(); floatValue(); doubleValue(); longValue(); toString()` logs `i;i;f;d;l;` on an
  `AtomicInteger` (and `byteValue(); shortValue(); floatValue(); doubleValue(); intValue()` logs `i;i;f;d;i;` on an
  `AtomicLong`); `incrementAndGet` and `getAndUpdate` log nothing.

## Steps

1. Read `javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeAtomicIntegerLongTest.java`.
2. Read `javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/atomic/AtomicReferenceArray.java`
   (same package, same single-threaded style) and `javaw-bytecode/src/main/resources/exception-runtime/java/lang/Number.java`
   (the superclass: four abstract methods plus `byteValue`/`shortValue`).
3. Create `javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/atomic/AtomicInteger.java`.
4. Create `javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/atomic/AtomicLong.java`.
5. Register both (order does not matter):
   `javaw-agent-kit/bin/kit register java/util/concurrent/atomic/AtomicInteger.java` and
   `javaw-agent-kit/bin/kit register java/util/concurrent/atomic/AtomicLong.java`. (Each adds a `paths.add(...)` line
   before the `ExceptionSupport` line of `BytecodeExceptionRuntime.java`; do not edit that file by hand.)
6. `javaw-agent-kit/bin/kit check T28` until PASS.
7. Write `docs/bytecode-atomic-integer-long.md` (below), the report, then `javaw-agent-kit/bin/kit finish T28`.

## Hints

- Write `AtomicInteger` first, get it compiling, then copy it to `AtomicLong` and change the types. Remember the
  `AtomicLong` versions: `intValue()` needs the cast `(int) value`, the operators are `LongUnaryOperator.applyAsLong`
  and `LongBinaryOperator.applyAsLong`, and the constants are `1L`/`-1L`.
- `java.util.function.IntUnaryOperator`, `IntBinaryOperator`, `LongUnaryOperator` and `LongBinaryOperator` exist in
  the runtime; `java.io.Serializable` exists. Write the full names or add imports.
- Do not write the lowercase keywords `volatile`, `synchronized` or `native`, or the word `Thread`, anywhere in the
  files, not even in comments. That is the easiest way to stay clear of `kit guard`. The method name
  `weakCompareAndSetVolatile` is fine (`AtomicReferenceArray` has it too).
- Every weak/acquire/release/exchange variant can simply call `compareAndSet` / `compareAndExchange`.
  `compareAndExchange` is `int old = value; if (old == expectedValue) value = newValue; return old;`.
- Do not copy `AtomicReferenceArray`'s update methods: they call the function exactly once and ignore a change made
  by the function. Use the retry loop from "JDK behaviour to match"; `reentrantUpdates` checks it.
- Do not declare `byteValue`/`shortValue`, and do not write `floatValue()` as `(float) intValue()`:
  `numberCallPattern` counts the calls a subclass sees.
- `toString()` must use the value, not `intValue()`: `Labelled` overrides only `toString()` and calls
  `super.toString()`.
- The Wasm runner stops at the **first** failing fixture of each `@Test` method, so after a fix a different fixture
  of the same group may fail next. That is progress, not a regression.
- Keep `getAndAdd`/`addAndGet` simple (`int old = value; value = old + delta; return old;` and
  `value += delta; return value;`); the increment/decrement methods can call them with `1`/`-1` (`1L`/`-1L` in
  `AtomicLong`).
- Calibration: the reference passed kit check after 1 run (about 115 new lines: 57 per class, written as one
  method per line, plus the two registration lines).

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
constructsReadsAndWrites, arithmeticWraps, compareAndSetFamily, updatesAndAccumulates, reentrantFunctionsRetryLikeTheJdk, numberConversions: if FIX THE RUNTIME says `link a complete acyclic type hierarchy: java/util/concurrent/atomic/AtomicInteger` (or `AtomicLong`) or `link the descriptor class/interface ...AtomicInteger`, the class is missing or not registered: run `javaw-agent-kit/bin/kit register java/util/concurrent/atomic/AtomicInteger.java` and `javaw-agent-kit/bin/kit register java/util/concurrent/atomic/AtomicLong.java` (never edit BytecodeExceptionRuntime.java by hand). If it says `link a matching instance method in the declaring hierarchy`, the member the fixture calls is missing, not public, or has another signature: add it exactly as listed under Members to implement. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes (often a missing `(int)` cast or `applyAsInt` used in AtomicLong instead of `applyAsLong`).
intBasics, longBasics: the no-argument constructor starts at 0; every get*/set* access mode reads or writes the one `value` field; getAndSet returns the old value; equals is NOT overridden (identity), and the class must be `extends Number implements java.io.Serializable`.
subclasses: the classes must not be final; toString() is `Integer.toString(get())` / `Long.toString(get())` so a subclass's `"L" + super.toString()` prints `L4`.
intArithmetic, intOverflow, longArithmetic, longOverflow: getAndX returns the value BEFORE the change, xAndGet returns the value AFTER it; use plain `value += delta` arithmetic that wraps (MAX_VALUE + 1 is MIN_VALUE) and never throw; AtomicLong must use long arithmetic everywhere (`1L`, not `1`, and a `long delta` parameter).
intCompareAndSet, longCompareAndSet: compareAndSet and every weakCompareAndSet variant store newValue and return true only when `value == expectedValue`; otherwise they return false and change nothing (`compareAndSet(7, 7)` on 7 is true).
intCompareAndExchange, longCompareAndExchange: compareAndExchange returns the OLD value in both cases (`int old = value; if (old == expectedValue) value = newValue; return old;`), never a boolean and never the new value.
intUpdates, longUpdates: getAndUpdate/getAndAccumulate return the old value, updateAndGet/accumulateAndGet the new one; the accumulator is called as `applyAsInt(prev, x)` (current value FIRST, then x): on 20, getAndAccumulate(3, (a, b) -> a - b) stores 17, not -17; each call invokes the function exactly once when it does not change the atomic.
reentrantUpdates: the four update methods must use the JDK retry loop: `prev = value; while (true) { next = f(prev...); if (value == prev) { value = next; return prev-or-next; } prev = value; }`. A function that sets the atomic to a different value gets called again with the new value (expected `u5;u100;100,101,2`); one that sets the same value is called once (`1029,1029,1`). Calling the function once and storing its result unconditionally is wrong.
updateFailures: let exceptions from the function propagate and assign `value` only after the function returned; a null function throws NullPointerException just by calling it (no requireNonNull message needed), and the value stays 4.
intConversions, longConversions: AtomicInteger: intValue() = value, longValue() = (long) value, floatValue() = (float) value, doubleValue() = (double) value. AtomicLong: intValue() = (int) value (low 32 bits: 4294967295 -> -1), longValue() = value, floatValue() = (float) value, doubleValue() = (double) value. Do not declare byteValue/shortValue (Number's `(byte) intValue()` is correct).
numberCallPattern: none of toString, intValue, longValue, floatValue, doubleValue may call another of them (read `value` or call get()); byteValue and shortValue must be the inherited Number versions, which call intValue(); the update/increment methods must not call the Number methods. Expected logs: `i;i;f;d;l;` for AtomicInteger and `i;i;f;d;i;` for AtomicLong.
```

## Docs

Create `docs/bytecode-atomic-integer-long.md` with this skeleton (fill in the brackets):

    # Bytecode AtomicInteger and AtomicLong

    Revision `atomic-integer-long-vN` links `java.util.concurrent.atomic.AtomicInteger` and `AtomicLong` in the
    bundled exception runtime. This contract applies only to the single-threaded deployment, like
    `AtomicReferenceArray`: every access mode is a plain field access and no Java Memory Model ordering is claimed.

    Contract:

    - <constructors, access modes, getAndSet, wrapping arithmetic, compareAndSet/weak variants, compareAndExchange
      witnesses, the update/accumulate retry loop and argument order, null and throwing functions, toString and the
      Number conversions (byteValue/shortValue inherited from Number)>

    Evidence: `BytecodeAtomicIntegerLongTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
    orders, two fresh instances).
