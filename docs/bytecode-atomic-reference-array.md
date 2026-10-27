# Bytecode AtomicReferenceArray

Revision `atomic-reference-array-v253` links `java.util.concurrent.atomic.AtomicReferenceArray` in the bundled exception runtime. This contract applies only to the selected **single-threaded** deployment. Every access mode (plain, opaque, acquire/release and volatile) is a plain `Object[]` access, and the compare-and-set/exchange families compare by reference identity without retry loops. That matches JDK results only while there are no concurrent mutators; no Java Memory Model ordering is claimed. Background-worker or multi-threaded configurations remain part of the concurrency milestone.

Contract:

- Construction by length (`NegativeArraySizeException` for negative values) or by copying a source array into a fresh `Object[]` (`NullPointerException` for null). Later source mutation is not visible, and covariant sources accept any reference afterward.
- `length`, `get`/`set`/`lazySet`, the `Plain`/`Opaque`/`Acquire`/`Release` accessors, `getAndSet`, `compareAndSet` and all `weakCompareAndSet*` variants, `compareAndExchange*`, `getAndUpdate`/`updateAndGet`, `getAndAccumulate`/`accumulateAndGet` and the JDK `toString` format.
- Out-of-range indexes raise `ArrayIndexOutOfBoundsException`. Null update/accumulator functions raise `NullPointerException`, and callback exceptions propagate without storing a value.

Evidence: `BytecodeAtomicReferenceArrayTest` compares Java 17 javac fixtures against the JVM in both JAR orders and two fresh dirty-memory instances. ASM fixtures prove unused calls to unlinked concurrency owners (`AtomicReference`, `AtomicReferenceFieldUpdater`, `ConcurrentHashMap`) are rejected with source locations before WAT generation.

The pinned closure uses this class in Jackson Core `BufferRecycler`, Jackson Databind `PrivateMaxEntriesMap` and H2 `TraceSystem`. The selected core-JDK linkage report excludes concurrency owners, so its counts are unchanged (1,141 declared / 9 missing-member / 187 missing-owner).
