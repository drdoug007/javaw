# T73 LongStream and DoubleStream

```kit
id: T73
slug: long-double-streams
title: link java.util.stream.LongStream, DoubleStream and Stream.mapToLong/mapToDouble
tests: BytecodeLongDoubleStreamTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/stream/LongStream.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/stream/LongPipeline.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/stream/DoubleStream.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/stream/DoublePipeline.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/stream/Stream.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/stream/ReferenceStream.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/PrimitiveIterator.java
allow: docs/bytecode-long-double-streams.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-long-double-streams-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeLongDoubleStreamTest.java`.

## Goal

Add the sequential `java.util.stream.LongStream` and `java.util.stream.DoubleStream` interfaces, with the same
boxed-adapter design the runtime already uses for `IntStream` (`IntPipeline` wraps a `Stream<Integer>`), and the
`Stream.mapToLong` / `Stream.mapToDouble` members that create them. `runtime-target/target/audit/core-jdk-linkage.tsv`
lists 7 `missing-owner` and 2 `missing-member` rows that this task closes (opcode 185 = invokeinterface):
`LongStream.close()V` (4 sites) and `LongStream.forEachOrdered(LongConsumer)` in Jackson jdk8
`LongStreamSerializer.serialize`; `DoubleStream.close()V` (4) and `DoubleStream.forEachOrdered(DoubleConsumer)` in
Jackson jdk8 `DoubleStreamSerializer.serialize`; `LongStream.average()` and `Stream.mapToLong(ToLongFunction)` in
Tomcat `Http2UpgradeHandler$PingManager.getRoundTripTimeNano`; `LongStream.findFirst()` in Spring Boot
`RandomValuePropertySource.getNextLongInRange`; `DoubleStream.average()` and `Stream.mapToDouble(ToDoubleFunction)`
in Spring Data `GeoResults.calculateAverageDistance`. The other members below are the coherent core that
`IntStream` already has (factories, filter/map, boxing, terminal operations), so library code that builds such a
stream can run it. After this task the audit should show `missing-owner` 0.

The tricky part is `DoubleStream.sum()` and `average()`: the JDK uses **compensated (Kahan) summation**, so
`DoubleStream.of(0.1 x 10).sum()` is `1.0` while a plain loop gives `0.9999999999999999`. Copy the JDK algorithm
exactly (below).

## Members to implement

1. `java/util/PrimitiveIterator.java` (existing; it has `OfInt`): add, next to `OfInt`,

   - `interface OfLong extends PrimitiveIterator<Long,LongConsumer>` with `long nextLong();`,
     `default Long next()` (returns `Long.valueOf(nextLong())`) and
     `default void forEachRemaining(LongConsumer action)` (null check, then `while(hasNext()) action.accept(nextLong());`)
   - `interface OfDouble extends PrimitiveIterator<Double,DoubleConsumer>` with `double nextDouble();`,
     `default Double next()` and `default void forEachRemaining(DoubleConsumer action)`, the same way.

2. `java/util/stream/LongStream.java`: `public interface LongStream extends BaseStream<Long,LongStream>` with
   - static: `empty()`, `of(long value)`, `of(long... values)`, `range(long startInclusive, long endExclusive)`,
     `rangeClosed(long startInclusive, long endInclusive)`
   - `@Override PrimitiveIterator.OfLong iterator();` and `@Override LongStream sequential();` (both covariant
     redeclarations are required: javac compiles `s.sequential()` to `LongStream.sequential()LLongStream;`)
   - `long[] toArray();` `void forEach(LongConsumer action);` `void forEachOrdered(LongConsumer action);`
   - `long sum();` `long count();` `OptionalLong min();` `OptionalLong max();` `OptionalDouble average();`
   - `long reduce(long identity, LongBinaryOperator op);` `OptionalLong reduce(LongBinaryOperator op);`
   - `LongStream filter(LongPredicate p);` `LongStream map(LongUnaryOperator m);` `LongStream peek(LongConsumer a);`
   - `<U> Stream<U> mapToObj(LongFunction<? extends U> m);` `Stream<Long> boxed();`
   - `LongStream distinct();` `LongStream sorted();` `LongStream limit(long maxSize);` `LongStream skip(long n);`
   - `boolean anyMatch(LongPredicate p);` `boolean allMatch(LongPredicate p);` `boolean noneMatch(LongPredicate p);`
   - `OptionalLong findFirst();` `OptionalLong findAny();`
   - `default LongStream parallel()` throwing `UnsupportedOperationException` (as `IntStream` does)
   - `isParallel`, `sequential`, `unordered`, `onClose`, `close` come from `BaseStream` and are implemented in the
     pipeline class.

3. `java/util/stream/DoubleStream.java`: `public interface DoubleStream extends BaseStream<Double,DoubleStream>` with
   the same members using `double`, `DoubleConsumer`, `DoubleBinaryOperator`, `DoublePredicate`,
   `DoubleUnaryOperator`, `DoubleFunction`, `OptionalDouble`, `Stream<Double>`, `PrimitiveIterator.OfDouble`
   (`@Override DoubleStream sequential();`, `double sum()`, `OptionalDouble min()/max()/average()/findFirst()/findAny()/reduce(op)`,
   `double reduce(double identity, DoubleBinaryOperator op)`, `double[] toArray()`), **without** `range` /
   `rangeClosed` (the JDK has none).

4. `java/util/stream/LongPipeline.java` and `java/util/stream/DoublePipeline.java`: `final class LongPipeline
   implements LongStream` / `final class DoublePipeline implements DoubleStream`, copies of `IntPipeline.java` that
   wrap a `Stream<Long>` / `Stream<Double>`: `static LongStream from(Stream<Long>)`, `static LongStream array(long[])`,
   `static LongStream range(long first, long last)` (both inclusive, Long only), and every interface method.

5. `java/util/stream/Stream.java`: add `LongStream mapToLong(ToLongFunction<? super T> mapper);` and
   `DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper);` next to `mapToInt`.
   `java/util/stream/ReferenceStream.java`: implement both next to `mapToInt` the same way
   (`Objects.requireNonNull(mapper); return LongPipeline.from(map(v -> Long.valueOf(mapper.applyAsLong(v))));`).

## Members to leave out

- `IntStream.asLongStream()` / `asDoubleStream()` and `IntStream.mapToLong`/`mapToDouble`: no pinned library calls
  them, and `BytecodeIntStreamTest` checks that `asLongStream`/`asDoubleStream` stay unlinked. Do not edit
  `IntStream.java` or `IntPipeline.java`.
- `LongStream.mapToInt/mapToDouble/asDoubleStream`, `DoubleStream.mapToInt/mapToLong`, `flatMap`, `takeWhile`,
  `dropWhile`, `collect`, `summaryStatistics` (`LongSummaryStatistics`/`DoubleSummaryStatistics` are not in the
  runtime), `generate`, `iterate`, `concat`, `builder`, `spliterator`: not referenced by the pinned libraries. The
  test checks that `LongStream.summaryStatistics`, `LongStream.mapToDouble`, `DoubleStream.iterate` and
  `IntStream.asLongStream` stay unlinked.
- `Random.longs(...)` (Spring Boot `RandomValuePropertySource` uses it to build its `LongStream`): a later task.
- Parallel execution: `parallel()` throws, `isParallel()` is `false`.

## JDK behaviour to match

- `range(a, b)` is empty when `a >= b`; `rangeClosed(a, b)` is empty when `a > b`. Both must work at the ends of the
  `long` range: `rangeClosed(Long.MAX_VALUE - 2, Long.MAX_VALUE)` has 3 elements and `range(Long.MIN_VALUE,
  Long.MIN_VALUE + 2)` has 2. You cannot use `end + 1` (it overflows); walk from `first` to `last` inclusive and stop
  **after** returning `last` (a `done` flag), with `range(a, b)` = inclusive range `a .. b - 1` when `a < b`.
- `LongStream.sum()` is `reduce(0, Long::sum)` (wraps on overflow). `min`/`max` use `Math.min`/`Math.max`.
  `average()`: count and a `long` total (which may wrap), result `(double) total / count`, empty when count is 0;
  `of(Long.MAX_VALUE, Long.MAX_VALUE).average()` is `OptionalDouble[-1.0]` because the total wraps.
- `DoubleStream.sum()` (JDK 21 `DoublePipeline.sum`, `Collectors.sumWithCompensation`, `computeFinalSum`): keep a
  `double[] s = new double[3]`; for each value `d`: `sumWithCompensation(s, d); s[2] += d;` where
  `sumWithCompensation(s, d)`: `double tmp = d - s[1]; double sum = s[0]; double velvel = sum + tmp;
  s[1] = (velvel - sum) - tmp; s[0] = velvel;`. The result is `computeFinalSum(s)`: `double tmp = s[0] - s[1];
  double simple = s[s.length - 1]; return (Double.isNaN(tmp) && Double.isInfinite(simple)) ? simple : tmp;`.
  So `of(1e308, 1e308).sum()` is `Infinity` (the compensated part is NaN, the simple sum is infinite),
  `of(Infinity, -Infinity).sum()` is `NaN`, the empty sum is `0.0`.
- `DoubleStream.average()`: `double[] s = new double[4]`; for each `d`: `s[2]++; sumWithCompensation(s, d);
  s[3] += d;`; result `s[2] > 0 ? OptionalDouble.of(computeFinalSum(s) / s[2]) : OptionalDouble.empty()` (here
  `computeFinalSum` reads the simple sum from `s[3]`, the last slot).
- `DoubleStream.min()`/`max()` are `reduce(Math::min)` / `reduce(Math::max)`: NaN wins, `-0.0` is smaller than `0.0`.
  `sorted()` orders like `Double.compare` (`-Infinity ... -0.0, 0.0 ... NaN`); `distinct()` compares like
  `Double.equals` (NaN equals NaN, `0.0` differs from `-0.0`). Sorting/deduplicating the boxed `Stream<Double>`
  gives exactly this.
- `toString` of results: `OptionalLong[5]`, `OptionalLong.empty`, `OptionalDouble[1.5]`, `OptionalDouble.empty`
  (from the existing Optional classes).
- `iterator()` returns a `PrimitiveIterator.OfLong`/`OfDouble`; `next()` returns the boxed value; `nextLong()` past
  the end throws `NoSuchElementException`.
- Null arguments to `filter`, `map`, `mapToObj`, `peek`, `forEach`, `reduce`, `anyMatch`..., `of((long[]) null)`,
  `Stream.mapToLong(null)` throw `NullPointerException`. `limit(-1)`/`skip(-1)` throw `IllegalArgumentException`
  (the wrapped reference stream already does this).
- `close()`/`onClose()`: share the wrapped stream's state, exactly as `IntPipeline` does. Handlers run once, in
  registration order; closing twice is a no-op; a terminal operation on a closed or consumed stream throws
  `IllegalStateException` (the test checks the type only). Closing a stream derived with `map` closes the source
  too (the handlers belong to the shared pipeline).

## Steps

1. Read the test, `java/util/stream/IntStream.java`, `IntPipeline.java`, `Stream.java`, `ReferenceStream.java`
   (find `mapToInt` and `fromIterator`) and `java/util/PrimitiveIterator.java`.
2. Add `OfLong` and `OfDouble` to `PrimitiveIterator.java`.
3. Create `LongStream.java`, `LongPipeline.java`, `DoubleStream.java`, `DoublePipeline.java` in
   `javaw-bytecode/src/main/resources/exception-runtime/java/util/stream/`.
4. Add `mapToLong` and `mapToDouble` to `Stream.java` and `ReferenceStream.java`.
5. Register the four new files (do not edit `BytecodeExceptionRuntime.java` by hand):
   - `javaw-agent-kit/bin/kit register java/util/stream/LongStream.java`
   - `javaw-agent-kit/bin/kit register java/util/stream/LongPipeline.java`
   - `javaw-agent-kit/bin/kit register java/util/stream/DoubleStream.java`
   - `javaw-agent-kit/bin/kit register java/util/stream/DoublePipeline.java`
6. `javaw-agent-kit/bin/kit check T73` until PASS.
7. Write `docs/bytecode-long-double-streams.md` (below), the report, then `javaw-agent-kit/bin/kit finish T73`.

## Hints

- Copy `IntPipeline.java` and replace `Integer`/`int`/`IntXxx` with `Long`/`long`/`LongXxx` (and `Double`...). The
  functional interfaces `LongConsumer`, `LongPredicate`, `LongUnaryOperator`, `LongBinaryOperator`, `LongFunction`,
  `ToLongFunction` and the `Double` versions already exist in `java/util/function/`. `OptionalLong` and
  `OptionalDouble` already exist.
- `ReferenceStream.fromIterator(() -> new Iterator<Long>() { ... })` builds the source; `IntPipeline.source` shows
  the pattern.
- `sum()` for longs: `reduce(0L, (a, v) -> a + v)`. Do **not** write `DoubleStream.sum()` that way: it must be the
  compensated sum. Use a `double[]` captured by the `forEach` lambda (a lambda cannot assign a local).
- Write the two helpers once as private static methods in `DoublePipeline` (`sumWithCompensation`,
  `computeFinalSum`); `Collectors` in this runtime does not have them.
- `PrimitiveIterator.OfLong` must declare `default Long next()`; otherwise the anonymous iterator in `LongPipeline`
  must implement `next()` too.
- The IllegalStateException message for a reused stream differs from the JDK (the runtime says `Stream already
  operated upon or closed`); the test only compares the type. Do not change `ReferenceStream`'s message.
- Calibration pitfalls: forgetting `@Override LongStream sequential();` / `DoubleStream sequential();` in the
  interfaces (`unsupported dynamic call java/util/stream/DoubleStream.sequential()`, as `IntStream` does it);
  `range` written with `end + 1` / `position < end + 1` (overflow gives an empty or endless
  stream at `Long.MAX_VALUE`); `DoubleStream.sum()` written as a plain `+` loop or reduce (`0.9999999999999999`
  instead of `1.0`); `computeFinalSum` in `average()` reading `s[2]` (the count) instead of the last slot `s[3]`.
- The Wasm runner stops at the first failing fixture of each `@Test` method; after a fix another fixture may fail next.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
longFactoriesAndTerminals, longIntermediates, doubleSums, doubleOperations, referenceMapping, closeAndReuse: if FIX THE RUNTIME says `link the symbolic class/interface java/util/stream/LongStream` (or `DoubleStream`, `LongPipeline`, `DoublePipeline`, `PrimitiveIterator$OfLong`) or names `Stream.mapToLong`/`mapToDouble`, a class is missing, not registered or lacks a listed member (`unsupported dynamic call java/util/stream/DoubleStream.sequential()` means the interface lacks `@Override DoubleStream sequential();`): run `javaw-agent-kit/bin/kit register` for the four new files and add every member under Members to implement. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
longFactoriesAndTerminals: range/rangeClosed must not compute end + 1; walk first..last inclusive with a done flag so Long.MAX_VALUE works; range(a, b) is empty when a >= b. average() keeps a long total (it may wrap) and divides as double.
longIntermediates: copy the IntPipeline adapters; limit/skip with a negative argument throw IllegalArgumentException from the wrapped Stream; iterator() returns a PrimitiveIterator.OfLong whose next() boxes nextLong().
doubleSums: DoubleStream.sum and average must use the JDK compensated summation (sumWithCompensation, then computeFinalSum returning the simple sum when the compensated result is NaN and the simple sum is infinite); average passes the 4-slot array so the simple sum is s[3].
doubleOperations: min/max use Math.min/Math.max (NaN wins, -0.0 < 0.0); sorted and distinct go through the boxed Stream<Double> (Double.compare / Double.equals order).
referenceMapping: Stream.mapToLong/mapToDouble must exist in Stream.java and ReferenceStream.java, check the mapper for null and wrap map(...) in LongPipeline.from / DoublePipeline.from.
closeAndReuse: onClose/close/isParallel/sequential/unordered must delegate to the wrapped Stream like IntPipeline, so handlers run once and a consumed or closed stream throws IllegalStateException.
leavesOutUnlistedPrimitiveStreamMembers: this fails when summaryStatistics, LongStream.mapToDouble, DoubleStream.iterate or IntStream.asLongStream became linked. Do not add members that are not listed.
```

## Docs

Create `docs/bytecode-long-double-streams.md` with this skeleton (fill in the brackets):

    # Bytecode LongStream and DoubleStream

    Revision `long-double-streams-vN` links sequential `java.util.stream.LongStream` and `DoubleStream`,
    `PrimitiveIterator.OfLong`/`OfDouble` and `Stream.mapToLong`/`mapToDouble` in the bundled exception runtime.

    Contract:

    - <boxed-adapter design shared with IntStream; factories incl. overflow-safe range/rangeClosed; operations;
      compensated DoubleStream sum/average; close/onClose shared with the wrapped pipeline>

    Known differences: sequential only; the reused-stream IllegalStateException message differs from the JDK;
    summaryStatistics, collect, flatMap, takeWhile/dropWhile, generate/iterate/concat, the cross-primitive
    conversions and Random.longs are not linked.

    Evidence: `BytecodeLongDoubleStreamTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
    orders, two fresh instances). `LongStream.summaryStatistics`, `LongStream.mapToDouble`, `DoubleStream.iterate`
    and `IntStream.asLongStream` are rejected before WAT generation.
