# Bytecode integer and character streams

This is a selected sequential IntStream implementation backed by the existing boxed reference-pipeline engine. It establishes linked bytecode behavior, separately from source facades. Specialized primitive storage, LongStream/DoubleStream conversion, builders, spliterators/StreamSupport, splitting and scheduling remain follow-ups. Regex and full library/framework execution remain separate milestones.

BytecodeIntStreamTest compiles Java 17 fixtures, runs an independent Java 17 JVM oracle, then compares Wasm in both JAR orders and two fresh instances whose allocator fills new memory with nonzero bytes. Admission fixtures use ASM, including unused invalid methods. Claims are restricted to the recorded subset.

Completed subsets:

- v200: Sequential IntStream factories, live arrays, bounded ranges, primitive/boxed iterator dispatch and typed traversal failures.
- v201: Integer reductions, numeric terminals, OptionalInt/OptionalDouble results, mutable collection and IntSummaryStatistics.
- v202: Lazy primitive map/filter/peek, mapToObj and boxed conversion with callback and null-failure tests.
- v203: Ordered skip/limit/takeWhile/dropWhile, distinct/sorted, matching and find short-circuit behavior.
- v204: Generate/iterate, concat/flatMap, child cleanup, single-use/onClose behavior and explicit parallel rejection.
- v205: String.chars UTF-16 unit traversal, surrogate preservation and fresh single-use pipelines.
- v206: String.codePoints supplementary pairing and malformed surrogate preservation, with selected pinned H2 trim instructions.

The H2 trim evidence uses `h2-2.2.224.jar` (SHA-256 `b9d8f19358ada82a4f6eb5b174c6cfe320a375b5a9cb5a4fe456d623e6e55497`). The JVM loads the original `org.h2.util.StringUtils`; Wasm links a carrier with its three unchanged `trim` methods and two predicate lambda bodies, omitting unrelated fields, methods and class initialization. The >2-code-point trim set executes the original `codePoints().forEach(HashSet::add)` path. Tests compare results, unchanged-string identity, direction flags, null/empty trim sets, supplementary and unpaired surrogate inputs, and null-source failure. This does not establish H2 engine execution.
