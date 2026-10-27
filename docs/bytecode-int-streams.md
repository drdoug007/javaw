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
- v207: CharSequence default chars/codePoints with terminal-time source binding and selected original Spring JSON escaping instructions.
- v208: Stream.mapToInt lazy conversion, callback/failure timing, single-use and close propagation.
- v210: Malformed primitive stream call/handle and unsupported primitive API admission guards.

The H2 trim evidence uses `h2-2.2.224.jar` (SHA-256 `b9d8f19358ada82a4f6eb5b174c6cfe320a375b5a9cb5a4fe456d623e6e55497`). The JVM loads the original `org.h2.util.StringUtils`; Wasm links a carrier with its three unchanged `trim` methods and two predicate lambda bodies, omitting unrelated fields, methods and class initialization. The >2-code-point trim set executes the original `codePoints().forEach(HashSet::add)` path. Tests compare results, unchanged-string identity, direction flags, null/empty trim sets, supplementary and unpaired surrogate inputs, and null-source failure. This does not establish H2 engine execution.

The Spring escape evidence pins `spring-core-6.1.5.jar` (SHA-256 `476dac28509b3f47d0b5e895a639fa75ac2218f074ce214e44832a836c469f7e`). The JVM runs the original private `BasicJsonWriter.escape(CharSequence)` through a reflection bridge. Wasm substitutes a bridge that directly calls an added carrier shim; the original escape and `lambda$escape$2` instructions remain unchanged. The carrier excludes writer fields, constructors, other methods and nested writer/I/O dependencies. Tests exercise all 32 ASCII control characters, quotes/backslashes/slashes, non-ASCII text, paired/unpaired surrogates, builders/buffers, a custom sequence with a different `toString()`, and null failure. This establishes the selected escaping path, not the complete JSON writer or Spring startup.

## Runtime contract and limits

`IntPipeline` delegates traversal and lifecycle to `ReferenceStream<Integer>`; intermediate integer values currently use boxed storage. Arithmetic remains Java `int`/`long` arithmetic, including overflow and long sums for average/statistics. Ranges track endpoints in a `long` to avoid wrapping at `Integer.MAX_VALUE`. Array sources observe mutations made before traversal. Sources and callbacks are lazy, pipelines are single-use, and null callbacks fail before claiming a still-unused pipeline. Matching/find/limit short-circuit upstream traversal. Flat-mapped child streams close on exhaustion, short circuit, exceptional termination, or closing an active iterator's stream; registered close handlers preserve order, idempotence and suppressed failures.

Only the declarations recorded above are supported. `IntSummaryStatistics` supports its no-argument constructor, `accept`, `combine`, and count/sum/min/max/average getters with JDK empty sentinels. Its other constructors and textual representation are outside this subset. `LongStream`, `DoubleStream`, primitive builders, specialized array storage, spliterators/StreamSupport and parallel scheduling remain unsupported. Direct `IntStream.parallel()` explicitly throws `UnsupportedOperationException`; erased `BaseStream.parallel()` remains outside the admitted subset. `IntStream.sequential()` has the JDK covariant descriptor; `sequential()` and `unordered()` preserve this selected sequential execution model.

The current pipeline traverses for `count()` and does not implement size-based callback elision. The Java Stream contract permits optimizations that omit incidental callback invocation; applications must not rely on `peek`/`map` callbacks being executed by `count()` across hosts. Differential tests compare traversal-sensitive callbacks using terminals that require values.

Character sources bind sequence length at terminal traversal, read `charAt` lazily, and do not call `toString()`. `chars()` emits unsigned UTF-16 code units. `codePoints()` combines adjacent high/low surrogate pairs, retaining isolated surrogates as individual values. Mutation before terminal traversal is covered; mutation during traversal and concurrent access have no added guarantee. Raw iterator binding details and spliterator behavior are not claimed.

The primitive iterator consumer overload checks whether a boxed `Consumer<Integer>` also implements `IntConsumer` and dispatches to the primitive overload, as Java 17 requires. Tests cover this dual-interface case, plain boxed/primitive callbacks, erased bridge calls, null actions, exhaustion and unsupported removal. ASM fixtures put malformed call descriptors, invocation kinds and method handles in unused methods and require source/line diagnostics before WAT generation; unsupported conversions, builder and primitive spliterator calls also fail admission.

The expanded runtime exposed a reproducible Node 24 optimizing-compiler Zone OOM in the existing object-identity regression: each virtual call repeated an identical inherited target for every concrete receiver class. Revision v210 groups receiver tags by their resolved implementation (including failure targets), retaining the exact resolution/access/override decisions while merging only equal destinations. This reduces dispatch result branches without host flags or larger Wasm memory limits. The original object-identity class passes after this change. The old unsupported `Stream.mapToInt` assertion now tests still-unsupported `mapToLong`.

## Batch validation

Batch validation (v210): all 500 bytecode tests passed using the prescribed offline selection (four forks, JVMCI compilation disabled); `python3 runtime-target/audit.py --check-baseline` passed. Inventory remains 63 JARs, 26,625 classes and 734 resources. Audited JDK linkage changed from 1,008 declared / 27 missing-member / 302 missing-owner signatures to 1,025 / 23 / 289. Introducing IntStream reclassifies some previously missing-owner calls as missing members; these counts do not establish full linkage or framework execution.
