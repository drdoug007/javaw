# Bytecode integer and character streams

This is a selected sequential IntStream implementation backed by the existing boxed reference-pipeline engine. It establishes linked bytecode behavior, separately from source facades. Specialized primitive storage, LongStream/DoubleStream conversion, builders, spliterators/StreamSupport, splitting and scheduling remain follow-ups. Regex and full library/framework execution remain separate milestones.

BytecodeIntStreamTest compiles Java 17 fixtures, runs an independent Java 17 JVM oracle, then compares Wasm in both JAR orders and two fresh instances whose allocator fills new memory with nonzero bytes. Admission fixtures use ASM, including unused invalid methods. Claims are restricted to the recorded subset.

Completed subsets:

- v200: Sequential IntStream factories, live arrays, bounded ranges, primitive/boxed iterator dispatch and typed traversal failures.
- v201: Integer reductions, numeric terminals, OptionalInt/OptionalDouble results, mutable collection and IntSummaryStatistics.
- v202: Lazy primitive map/filter/peek, mapToObj and boxed conversion with callback and null-failure tests.
- v203: Ordered skip/limit/takeWhile/dropWhile, distinct/sorted, matching and find short-circuit behavior.
