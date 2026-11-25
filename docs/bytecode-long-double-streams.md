# Bytecode LongStream and DoubleStream

Revision `long-double-streams-v336` links sequential `java.util.stream.LongStream` and `DoubleStream`,
`PrimitiveIterator.OfLong`/`OfDouble` and `Stream.mapToLong`/`mapToDouble` in the bundled exception runtime.

Contract:

- Boxed-adapter design shared with `IntStream`: `LongPipeline` wraps a `Stream<Long>` and `DoublePipeline` a
  `Stream<Double>` built on `ReferenceStream`, so factories, `filter`/`map`/`peek`, `distinct`/`sorted`
  (natural `Long`/`Double` order for `sorted()`, `Double.equals` semantics for `distinct()`), `limit`/`skip`
  (negative counts throw `IllegalArgumentException` from the wrapped stream), `mapToObj`/`boxed`, the
  `anyMatch`/`allMatch`/`noneMatch` and `findFirst`/`findAny` terminals, `toArray`, `count`, `reduce` and
  null-argument `NullPointerException`s all delegate to the shared reference pipeline. `iterator()` returns
  `PrimitiveIterator.OfLong`/`OfDouble` whose `next()` boxes `nextLong()`/`nextDouble()`.
- `LongStream` factories: `empty`, `of(long)`, `of(long...)`, `range` and `rangeClosed`. `range(a, b)` is
  empty for `a >= b` and `rangeClosed(a, b)` empty for `a > b`; both walk `first..last` inclusive with a
  `done` flag set after returning `last`, so they are correct at the ends of the `long` range
  (`rangeClosed(Long.MAX_VALUE - 2, Long.MAX_VALUE)`, `range(Long.MIN_VALUE, Long.MIN_VALUE + 2)`) without
  computing `end + 1`.
- `LongStream.sum()` is `reduce(0, Long::sum)` (wraps on overflow); `min`/`max` use `Math.min`/`Math.max`;
  `average()` keeps a wrapping `long` total and divides as `double` (`of(Long.MAX_VALUE, Long.MAX_VALUE)`
  averages `-1.0`). `DoubleStream.sum()` and `average()` copy the JDK 21 compensated (Kahan) summation:
  `sumWithCompensation` updates a `double[3]`/`double[4]` state and `computeFinalSum` returns the simple
  sum from the last slot when the compensated result is `NaN` and the simple sum is infinite, so
  `of(0.1 x 10).sum()` is `1.0` and `of(1e308, 1e308).sum()` is `Infinity`.
- `Stream.mapToLong`/`mapToDouble` check the mapper for null and wrap the boxed `map(...)` in
  `LongPipeline.from`/`DoublePipeline.from`; `LongStream.sequential()`/`DoubleStream.sequential()` have the
  JDK covariant redeclarations.
- `close()`/`onClose()`/`isParallel()`/`sequential()`/`unordered()` share the wrapped stream's state exactly
  as `IntPipeline` does: handlers run once, in registration order, closing is idempotent, a terminal
  operation on a closed or consumed stream throws `IllegalStateException`, and closing a derived stream
  closes the shared source pipeline.

Known differences: sequential only (`parallel()` throws `UnsupportedOperationException`, `isParallel()` is
`false`); the reused-stream `IllegalStateException` message differs from the JDK; `summaryStatistics`,
`collect`, `flatMap`, `takeWhile`/`dropWhile`, `generate`/`iterate`/`concat`, the cross-primitive
conversions (`mapToInt`/`mapToDouble`/`mapToLong`, `asDoubleStream`/`asLongStream`) and `Random.longs` are
not linked.

Evidence: `BytecodeLongDoubleStreamTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
orders, two fresh instances). `LongStream.summaryStatistics`, `LongStream.mapToDouble`, `DoubleStream.iterate`
and `IntStream.asLongStream` are rejected before WAT generation.
