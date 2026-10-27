# Selected sorted bytecode collections

Sequential sorted-array storage establishes tested ordering behavior, not balanced-tree performance or concurrency. Serialization, spliterators and broad framework execution are not established.

- v221: Ordered TreeMap construction, comparator identity, key equivalence and typed failures.
- v222: Navigable map neighbors and immutable entry snapshots.
- v223: Live sorted map range views and nested bound checks.
- v224: Inclusive/exclusive and descending map views.
- v225: Fail-fast sorted traversal and backed key/value/entry mutation.
- v226: TreeSet construction, ordering and navigation.
- v227: Live navigable set ranges and descending traversal.
- v228: Unmodifiable sorted map/set wrappers and nested view protection.
- v229: Unmodifiable navigable families and empty sorted/navigable factories.
- v230: Pinned-library sorted execution and unused malformed call/handle admission.

## Runtime contract

`SortedMap`, `NavigableMap`, `SortedSet` and `NavigableSet` expose the selected Java 17 ordering/view contracts. `TreeMap` provides natural/comparator construction, Map and SortedMap copies, comparator-equivalent keys, nullable values, lookup/update/removal, first/last and neighbor keys/entries, polling, and live sorted/inclusive/descending ranges. SortedMap construction preserves the comparator; Map construction uses natural order. Natural ordering rejects null/incomparable keys where Java performs a comparison; empty-map neighbor queries return null without comparing the key. Custom comparators may admit null keys.

Range endpoints are checked against the parent view, including the distinction between an excluded boundary and an inclusive child endpoint. Reversing a view reverses its traversal and navigation order without copying storage. Key, value and entry traversal is backed by the same map. Navigation methods return immutable snapshots; entry-set entries support value replacement. Iterators detect structural changes on next/remove, allow value-only updates, and support their own removals in both directions. This is best-effort sequential fail-fast behavior, not synchronization.

`TreeSet` provides natural/comparator construction, Collection/SortedSet copies, navigation/polling, live sorted/inclusive/descending ranges, and both traversal directions. Map/set clones copy storage while retaining comparator and element/key/value references. Concrete view implementation classes, cached view identity, subclass cloning, serialization protocols, spliterators and structural mutation inside remapping callbacks are outside this tested contract.

`Collections.unmodifiableSortedMap`, `unmodifiableSortedSet`, `unmodifiableNavigableMap` and `unmodifiableNavigableSet` provide live read-only views. Mutation paths are closed through nested ranges, descending views, map entries (including array exposure), iterators, key/value views and default callback APIs. Navigable entry access is protected; polling throws even when empty. Empty sorted/navigable factories share an immutable empty map/set respectively. Synchronized wrappers remain unsupported pending the concurrency contract.

Storage is a sorted ArrayList, with binary-search lookup and linear insertion/removal; range size and navigation may scan storage. This is not a balanced-tree implementation and provides no TreeMap/TreeSet complexity guarantee. Comparator consistency follows Java's comparator requirements; comparator call counts are not specified. Sustained allocation bounds, concurrent use and full framework execution are not established.

## Pinned library evidence

Spring Core 6.1.5, SHA-256 `476dac28509b3f47d0b5e895a639fa75ac2218f074ce214e44832a836c469f7e`, supplies the unchanged private `orderForCodeConsistency` method bodies from `ValueCodeGeneratorDelegates$MapDelegate` and `$SetDelegate`. They construct TreeMap/TreeSet instances and return the original input after ClassCastException for incomparable keys/elements. Differential fixtures cover successful sorted copies and that fallback with reference identity checks.

The JVM oracle invokes the original methods against the pinned JAR. Wasm carriers retain only those helper bodies, replace their superclass with Object, and add simple constructors plus forwarding shims. The bridge uses reflection only on the JVM and direct calls in Wasm. Original initialization, delegate hierarchy, code generation and Spring startup are excluded.

Malformed ASM calls/handles in unused methods check descriptor and invocation-kind admission with source/method context. Unsupported spliterator and synchronized sorted/navigable APIs must fail admission before WAT generation. Executable cases compare Java 17 fingerprints in both JAR orders and fresh dirty-memory instances.

## Validation

At `sorted-admission-v230`, all 520 bytecode tests pass with the prescribed offline selection, using four forks and JVMCI compilation disabled. The combined sorted-collection/object-identity run passes all 15 tests. The audit fixture now distinguishes missing TreeMap members from unsupported WeakHashMap ownership. The reviewed runtime audit baseline passes with 63 JARs, 26,625 classes and 734 resources unchanged; selected JDK linkage is 1,097 declared / 18 missing-member / 222 missing-owner signatures. Broad collection, concurrency and framework milestones remain open.
