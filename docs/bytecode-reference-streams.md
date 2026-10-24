# Bytecode sequential reference streams

Revision `reference-streams-v63` begins reference-stream support in the isolated bundled bytecode runtime (`--exception-runtime` or the corresponding compiler APIs). Ordinary Stream/BaseStream/Collector class-file declarations link dependency calls to Wasm implementations. This is separate from source facades and host-JDK execution. The core-JDK parent milestone remains incomplete.

## Supported operations

- Sources: Collection.stream, reference Arrays.stream (whole array or bounded range), Optional.stream, Stream.empty/of/ofNullable, generate, both iterate overloads and concat.
- Lazy intermediate operations: filter, map, flatMap, peek, distinct, stable sorted (natural/custom), skip/limit and takeWhile/dropWhile.
- Terminals: iterator, forEach/forEachOrdered, count, all/any/noneMatch, findFirst/findAny, min/max, all three reference reduce overloads, supplier/accumulator/combiner collect, Collector collect, Object/typed toArray and toList.
- Sequential lifecycle: isParallel returns false; sequential/unordered, onClose and close. Linking or consuming a stage prevents another operation on it. Closing any stage closes the shared pipeline. Terminal operations do not automatically run pipeline onClose handlers; explicit close or try-with-resources does.
- Collectors: both Collector.of factories; toCollection, toList, toSet, toUnmodifiableList, joining overloads, mapping and collectingAndThen. Supplier, accumulator, combiner, finisher and immutable characteristics are exposed through the ordinary Collector API. Sequential collection does not call the combiner; tests also invoke combiners directly.

Intermediate operations do not visit source elements. Array elements remain backed by the original array; collection iterators are acquired when execution begins. Pull-based traversal short-circuits without draining infinite sources. Distinct retains encounter order, including a single null. Sorting buffers its input and uses the existing stable insertion-sort implementation. No parallel scheduling or JDK complexity guarantee is implied.

FlatMap treats a null mapped stream as empty and closes each mapped stream after traversal, including terminal short-circuiting and callback failures. Cleanup failures are suppressed onto a primary traversal failure. Close handlers run in registration order, once, even when earlier handlers fail; later distinct failures are suppressed onto the first failure. Concat consumes its input streams and explicitly closing it closes both inputs.

Stream.toList returns an unmodifiable snapshot which permits nulls. Its array exports are defensive and its sublists remain unmodifiable. Collectors.toList/toSet return mutable collections; toUnmodifiableList rejects null elements. Joining accepts null elements as the text `null`. Find/reduce/min/max results use Optional's non-null value contract. Invalid counts/ranges, null callbacks, array component mismatches and consumed/closed stream reuse raise linked Java exceptions. Exact failure messages and callback invocation counts that the Stream specification permits implementations to elide are not promised. Count currently traverses the pipeline instead of applying the JDK's sized-source optimization.

## Executable evidence

`BytecodeStreamsTest` compiles javac fixtures and compares JVM results with assembled Node/Wasm execution, in both JAR orders and two independent instances under the default memory limit. Regressions cover lazy mutations, ordered callbacks, finite/infinite short-circuiting, nulls, typed arrays, stable sorting, iterator exhaustion, reductions, collector combiners/finishers, snapshots and immutable mutation paths, exception propagation/suppression and stream lifecycle.

The library fixture hash-verifies pinned Micrometer Commons 1.12.4 and links four **unchanged complete class files**: KeyName, KeyValue, ImmutableKeyValue and ValidatedKeyValue. KeyName.merge executes its original Arrays.stream/flatMap/typed-toArray bytecode, lambda helpers and private component-array allocation. Tests check empty and multi-array merges, ordering/identity, snapshot isolation, null-array failure and key/value methods. This establishes those utility classes' execution, not Micrometer startup or complete framework compatibility.

That fixture also exposed and corrected an admission version check: class-file version 52 permits private concrete interface methods, including javac 8's private static lambda helpers. Java source syntax introduced private interface methods later. ASM regressions execute private static helpers with and without ACC_SYNTHETIC against the JVM and Wasm; version-51 variants must be rejected before WAT generation.

## Remaining work

Primitive streams and mapToInt/mapToLong/mapToDouble, spliterators/StreamSupport, builders, mapMulti, parallel streams/Collection.parallelStream, collector map/grouping/reduction families and other undeclared descriptors remain unsupported. Primitive mapping and parallel calls have explicit admission regressions: they must not silently run sequentially or use source facades. Managed reclamation, sustained bounded-memory execution, concurrency and source/bytecode object sharing remain separate milestones. MathContext, broader text/wrapper/collection/math APIs and complete H2/Spring/Hibernate execution remain open.

The selected whole-JAR core-signature inventory now has 795 declared signatures, 208 missing members and 334 missing owners (55 additional declarations). These figures measure declarations, not proven reachability or runtime compatibility. The 63-JAR inventory counts are unchanged; the reviewed audit baseline changes only the compiler revision.

Validation: the full 289-test bytecode run passed 288 tests; its sole failure was the obsolete Java-8 private-interface rejection assertion. After correcting the fixture expectation to its actual missing-Code defect, all seven declaration tests passed. All nine new stream tests passed in the full run. The reviewed 63-JAR audit baseline passes with unchanged inventory counts and revision `reference-streams-v63`.
