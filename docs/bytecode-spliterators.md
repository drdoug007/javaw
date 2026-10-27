# Selected reference spliterators and streams

This sequential reference subset does not establish primitive or parallel traversal or full framework execution.

- v231: Collection generated-array overload and typed callback/store failures.
- v232: Reference Spliterator defaults and empty traversal.
- v233: Reference array spliterators, range validation and ordered splitting.
- v234: Sized/unknown iterator spliterators and iterator adapter.
- v235: AbstractSpliterator subclass batching and callback state.
- v236: Iterable and collection late-binding spliterators.
- v237: Sequential StreamSupport direct and supplier sources.
- v238: Single-use lazy reference-stream spliterator traversal.
- v239: Array and sorted-collection spliterator metadata and views.
- v240: Pinned-library traversal and unused malformed call/handle admission.

## Selected contract

`Collection.toArray(IntFunction)` calls the generator with zero and delegates to typed array conversion. Tests cover component types, nulls, callback failures, and ArrayStoreException. This also exposes inherited List/Set/ArrayList signatures required by pinned libraries.

Reference `Spliterator` includes Java 17 characteristic constants, tryAdvance/forEachRemaining/trySplit, estimates, exact-size and characteristic queries, and the throwing comparator default. `Spliterators` provides the reference empty singleton, whole/range object arrays, known-size and unknown-size iterators, Collection sources, iterator adapters, and AbstractSpliterator. Arrays split into ordered prefixes; iterator and abstract sources batch prefixes using the Java batching policy. Estimates retain the source-specific Java behavior rather than universally decrementing after traversal. Array bulk traversal commits its fence before callbacks, including callback failure. AbstractSpliterator inherits the throwing comparator default unless a subclass overrides it.

Iterable defaults obtain an iterator immediately with unknown size; Collection sources bind size/iterator lazily. List and Set defaults add ORDERED or DISTINCT respectively. ArrayList has late-binding indexed prefix splitting and structural-change checks. SortedSet's default exposes its comparator. TreeSet exposes selected sorted/distinct/order/size metadata for root/range/descending views and preserves comparator identity where SORTED is reported. TreeSet sources intentionally decline splitting; no balanced-tree split topology is established. Unmodifiable views preserve source metadata while keeping entry exposure protected.

`StreamSupport.stream` accepts a direct reference spliterator or a supplier/characteristic mask in sequential mode. Supplier sources are evaluated on traversal, not stream construction or iterator creation. Existing sequential transformations and terminals consume these sources; existing close handlers remain single-use and propagate failure/suppression according to the stream runtime contract. Parallel mode throws explicitly; it is not silently executed sequentially.

Source reference streams from arrays, collections and StreamSupport expose single-use spliterators. Empty Stream heads retain their distinct empty-source characteristics. Collection.stream delegates to the collection’s spliterator, including custom overrides. Direct StreamSupport heads preserve source identity; supplier heads defer binding until spliterator operations. Closed/reused streams fail. Spliterator access on transformed/generated streams and primitive stream variants is explicitly unsupported in this stage; their ordinary supported sequential terminals continue to work. Unsupported primitive factories and malformed calls/handles are rejected during unused-method admission before WAT generation.

Comparator consistency follows the JDK comparator requirements. Concrete JDK implementation class identity, every implementation-specific flag/split choice, concurrent traversal, sustained allocation bounds, primitive specializations and parallel execution are not established.

## Pinned library evidence

Spring Data Commons 3.2.4, SHA-256 `35d95442ab476e46d9ac4d60074f9b9c30b18c106fd1ec71d1d68c00d8080f6d`, supplies unchanged `StreamUtils.createStreamFromIterator(Iterator)` and `zip` method bodies, plus the original complete `StreamUtils$1` and `Sink` traversal classes. The carrier omits unrelated StreamUtils methods, including its CloseableIterator overload. Spring Core 6.1.5, SHA-256 `476dac28509b3f47d0b5e895a639fa75ac2218f074ce214e44832a836c469f7e`, supplies the unchanged selected `Assert.notNull(Object,String)` body. JVM oracles use the original artifacts; no source facade substitutes for these library instructions.

Fixtures compare iteration, unequal-length zip traversal, deferred combiner callbacks, null validation and callback failures. Executable cases run against Java 17 in both JAR orders and fresh dirty-memory instances. Malformed ASM fixtures check unused calls/handles with source/method diagnostics. This establishes selected library traversal helpers, not Spring Data repositories, application startup or dependency-closure execution.

## Validation

Validation (v240): all 530 bytecode tests pass with the prescribed offline selection (four forks, JVMCI compilation disabled), with zero failures, errors or skips. The combined focused traversal/sorted-admission/object-identity run passes all 16 tests; the final pinned-library/admission rerun also passes. The reviewed runtime audit passes with unchanged 63-JAR / 26,625-class / 734-resource inventory. Selected JDK linkage reports 1,121 declared signatures, 9 missing members and 207 missing owners; these are declaration counts, not framework execution evidence.
