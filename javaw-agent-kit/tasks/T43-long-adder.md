# T43 LongAdder and LongAccumulator

```kit
id: T43
slug: long-adder
title: link java.util.concurrent.atomic.LongAdder and LongAccumulator (single-threaded)
tests: BytecodeLongAdderTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/atomic/LongAdder.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/atomic/LongAccumulator.java
allow: docs/bytecode-long-adder.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-long-adder-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeLongAdderTest.java`.

## Goal

Add `java.util.concurrent.atomic.LongAdder` and `java.util.concurrent.atomic.LongAccumulator` to the bundled runtime.
The pinned libraries name `LongAdder` from 9 classes (`runtime-target/target/audit/external-types.tsv`): Tomcat
`StandardWrapperValve`, `webresources.Cache`, `UpgradeGroupInfo` and Hibernate's statistics classes
(`StatisticsImpl`, `QueryStatisticsImpl`, `EntityStatisticsImpl`, `CollectionStatisticsImpl`,
`CacheRegionStatisticsImpl`, `AbstractCacheableDataStatistics`). The member rows (`member-references.tsv`) are
`<init>()`, `add(J)`, `increment()`, `sum()`, `longValue()` and `reset()`, and Hibernate's `toString()` methods
concatenate `LongAdder` objects (so `toString()` is called). `LongAccumulator` is the same class with a function in
place of `+`; it is small, so it is linked too.

The runtime is **single-threaded**, the same contract as `AtomicLong` (`docs/bytecode-atomic-integer-long.md`). The
JDK spreads the count over a base field plus a `Cell[]` table in the hidden superclass `Striped64`; without threads
the table is never created, so here each class keeps one plain `private long` field and extends `Number` directly.
No `volatile` or `synchronized` (RULES.md); `Striped64`, `Cell`, `Unsafe` and `VarHandle` do not exist in the runtime.

## Members to implement

Two files, both `package java.util.concurrent.atomic;`. Neither class is final and **no method is final** (libraries
and the test subclass them and override `add`, `sum`, `get`, `reset` and the `Number` methods).

`java/util/concurrent/atomic/LongAdder.java`:
`public class LongAdder extends Number implements java.io.Serializable`

- `private long base;`
- `public LongAdder()` (starts at 0)
- `public void add(long x)`, `public void increment()`, `public void decrement()`
- `public long sum()`, `public void reset()`, `public long sumThenReset()`
- `public String toString()`, `public long longValue()`, `public int intValue()`, `public float floatValue()`,
  `public double doubleValue()`

`java/util/concurrent/atomic/LongAccumulator.java`:
`public class LongAccumulator extends Number implements java.io.Serializable`

- `private final java.util.function.LongBinaryOperator function;`, `private final long identity;`, `private long base;`
- `public LongAccumulator(java.util.function.LongBinaryOperator accumulatorFunction, long identity)`
- `public void accumulate(long x)`, `public long get()`, `public void reset()`, `public long getThenReset()`
- `public String toString()`, `public long longValue()`, `public int intValue()`, `public float floatValue()`,
  `public double doubleValue()`

## Members to leave out

- Serialization: `writeReplace()`, `readObject(ObjectInputStream)` and the `SerializationProxy` nested classes. The
  test checks that `LongAdder.writeReplace()` stays unlinked (`leavesOutSerialization`).
- `byteValue()` and `shortValue()`: do **not** declare them; the runtime `java.lang.Number` versions
  (`(byte) intValue()`, `(short) intValue()`) are exactly the JDK's.
- `equals` and `hashCode`: not overridden (identity), like the JDK.
- `Striped64` and its `Cell` class, `serialVersionUID`. Do not edit any other runtime file.

## JDK behaviour to match

- `LongAdder`: `add(x)` is `base += x` (wraps silently: `Long.MAX_VALUE` then `increment()` gives
  `-9223372036854775808`). `increment()` calls `add(1L)` and `decrement()` calls `add(-1L)` (virtual calls: a
  subclass overriding `add` sees `add(1);add(-1);`). `sum()` returns `base`. `reset()` sets `base = 0L`.
  `sumThenReset()` returns the old `base` and sets it to 0 **without** calling `sum()` or `reset()`.
- `LongAdder.toString()` is `Long.toString(sum())`; `longValue()` is `sum()`; `intValue()` is `(int) sum()` (low 32
  bits: 4294967295 gives -1, 4294967301 gives 5); `floatValue()` is `(float) sum()` (4294967295 gives
  `4.2949673E9`, 16777217 gives `1.6777216E7`); `doubleValue()` is `(double) sum()`. Each calls `sum()` (the virtual
  method, once) and none calls another Number method. With a subclass that logs and calls `super`:
  `toString()` logs `sum;`, `longValue()` logs `l;sum;`, `byteValue(); shortValue()` logs `i;sum;i;sum;`.
- `LongAccumulator(f, identity)`: stores both and sets `base = identity`. It does **not** check `f` for null and
  does **not** call `f`.
- `accumulate(x)`: `long r = function.applyAsLong(base, x); if (r != base) base = r;` -- the function is called
  exactly once, current value first, `x` second (on 100 with `(x, y) -> x - y`, `accumulate(30L)` gives 70). A null
  function throws `NullPointerException` from that call (no message is compared) and `base` is unchanged; an
  exception thrown by the function propagates unchanged and `base` is unchanged.
- `get()` returns `base` and never calls the function. `reset()` sets `base = identity`. `getThenReset()` returns
  the old `base`, sets `base = identity`, and calls neither `get()`, `reset()` nor the function.
- `LongAccumulator.toString()` is `Long.toString(get())`, `longValue()` is `get()`, `intValue()` is `(int) get()`,
  `floatValue()` is `(float) get()`, `doubleValue()` is `(double) get()`; each calls the virtual `get()` once.
  `new LongAccumulator(Long::max, Long.MIN_VALUE)` prints `-9223372036854775808` before any accumulate.

## Steps

1. Read `javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeLongAdderTest.java`.
2. Read `javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/atomic/AtomicLong.java` (same
   package, same single-threaded style, same Number methods).
3. Create `javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/atomic/LongAdder.java`.
4. Create `javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/atomic/LongAccumulator.java`.
5. Register both: `javaw-agent-kit/bin/kit register java/util/concurrent/atomic/LongAdder.java` and
   `javaw-agent-kit/bin/kit register java/util/concurrent/atomic/LongAccumulator.java` (never edit
   `BytecodeExceptionRuntime.java` by hand).
6. `javaw-agent-kit/bin/kit check T43` until PASS.
7. Write `docs/bytecode-long-adder.md` (below), the report, then `javaw-agent-kit/bin/kit finish T43`.

## Hints

- Do not copy the JDK source: it needs `Striped64`, `Cell`, `getProbe`, `casBase` and `longAccumulate`, none of which
  exist. Each class is about 25 lines with one method per line.
- Do not mark any method `final`: the fixtures override `add`, `sum`, `get` and `reset`.
- The Number methods must call `sum()` / `get()`, not read `base` directly (the call-pattern fixtures count calls),
  and `sumThenReset()` / `getThenReset()` must read `base` directly (they log nothing).
- `increment()` must be `add(1L)`, not `base++`: `adderCallPattern` expects `add(1);add(-1);add(4);`.
- Do not write the lowercase words `volatile`, `synchronized`, `native` or `Thread` anywhere in the files, not even in
  comments.
- `java.util.function.LongBinaryOperator` and `java.io.Serializable` exist in the runtime.
- The Wasm runner stops at the first failing fixture of each `@Test` method.
- Calibration: the reference passed kit check after 1 run (about 50 new lines: 20 for LongAdder, 26 for
  LongAccumulator, plus the two registration lines).

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
adderArithmetic, adderNumberMethods, accumulatorArithmetic, accumulatorFunctions: if FIX THE RUNTIME says `link a complete acyclic type hierarchy: java/util/concurrent/atomic/LongAdder` (or `LongAccumulator`) or `link the descriptor class/interface ...LongAdder`, the class is missing or not registered: run `javaw-agent-kit/bin/kit register java/util/concurrent/atomic/LongAdder.java` and `javaw-agent-kit/bin/kit register java/util/concurrent/atomic/LongAccumulator.java`. If it says `link a matching instance method in the declaring hierarchy`, add the member exactly as listed under Members to implement. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
adderBasics, adderOverflow, adderReset: add is `base += x` (wrapping), sum returns base, reset sets 0, sumThenReset returns the old base and sets 0; the class is `extends Number implements java.io.Serializable` and does not override equals.
adderConversions, accumulatorConversions: intValue is `(int)` of the long (4294967295 -> -1), floatValue `(float)`, doubleValue `(double)`; do not declare byteValue/shortValue.
adderCallPattern: increment/decrement call add(1L)/add(-1L); toString, longValue, intValue, floatValue, doubleValue each call sum() once and nothing else; sumThenReset reads base directly (logs nothing); no method may be final. Expected `add(1);add(-1);add(4);|sum;|l;sum;|i;sum;|f;sum;d;sum;|i;sum;i;sum;|4|add(2);reset;0`.
accumulatorBasics, accumulatorFunctionCalls: the constructor sets base = identity and never calls the function; accumulate calls `function.applyAsLong(base, x)` once (base FIRST, then x) and stores the result; get returns base without calling the function; reset sets base = identity; getThenReset returns base and sets identity without calling the function.
accumulatorFailures: the constructor accepts a null function; get, toString, reset and getThenReset still work (7); accumulate throws the NullPointerException from calling the null function; a throwing function leaves base unchanged (assign only after the call returned).
accumulatorCallPattern: toString, longValue, intValue, floatValue, doubleValue each call get() once; getThenReset and accumulate must not call get(); reset must not be final. Expected `|get;|l;get;i;get;|f;get;d;get;|i;get;i;get;|7|reset;`.
leavesOutSerialization: this fails when LongAdder.writeReplace() has become linked. Do not add writeReplace, readObject or a SerializationProxy.
```

## Docs

Create `docs/bytecode-long-adder.md` with this skeleton (fill in the brackets):

    # Bytecode LongAdder and LongAccumulator

    Revision `long-adder-vN` links `java.util.concurrent.atomic.LongAdder` and `LongAccumulator` in the bundled
    exception runtime. This contract applies only to the single-threaded deployment, like `AtomicLong`: one plain
    `long` field, no cells and no Java Memory Model ordering claimed.

    Contract:

    - <add/increment/decrement/sum/reset/sumThenReset with wrapping arithmetic; accumulate argument order and single
      function call; identity, reset and getThenReset; null and throwing functions; toString and the Number
      conversions calling sum()/get(); byteValue/shortValue inherited from Number>

    Known differences: both classes extend `Number` directly (the JDK superclass is the package-private
    `Striped64`), so `getSuperclass()` differs. Serialization (`writeReplace`, `SerializationProxy`) is not linked.

    Evidence: `BytecodeLongAdderTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
    fresh instances). `LongAdder.writeReplace()` is rejected before WAT generation.
