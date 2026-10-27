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

The Spring escape evidence pins `spring-core-6.1.5.jar` (SHA-256 `476dac28509b3f47d0b5e895a639fa75ac2218f074ce214e44832a836c469f7e`). The JVM runs the original private `BasicJsonWriter.escape(CharSequence)` through a reflection bridge. Wasm substitutes a bridge that directly calls an added carrier shim; the original escape and `lambda$escape$2` instructions remain unchanged. The carrier excludes writer fields, constructors, other methods and nested writer/I/O dependencies. Tests exercise all 32 ASCII control characters, quotes/backslashes/slashes, non-ASCII text, paired/unpaired surrogates, builders/buffers, a custom sequence with a different `toString()`, and null failure. This establishes the selected escaping path, not the complete JSON writer or Spring startup.
- v207: CharSequence default chars/codePoints with terminal-time source binding and selected original Spring JSON escaping instructions.
- v208: Stream.mapToInt lazy conversion, callback/failure timing, single-use and close propagation.
