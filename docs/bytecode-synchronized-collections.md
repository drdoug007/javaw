# Bytecode synchronized collection wrappers

Revision `synchronized-collections-v270` links `Collections.synchronizedCollection/Set/List/Map` as
monitor-free delegating views (single-threaded contract) in the bundled exception runtime.

Contract:

- Every call forwards to the backing collection as one delegating line (`CollectionView` to `c`,
  `ListView` also to `list`, `MapView` to `m`), including the interface defaults `forEach`, `removeIf`,
  `spliterator`, `stream`, `toArray(IntFunction)`, `replaceAll`, `sort` and every `Map` default
  (`getOrDefault`, `putIfAbsent`, `remove(Object,Object)`, `replace`, `computeIfAbsent`,
  `computeIfPresent`, `compute`, `merge`). Nothing is copied: `iterator()`, `listIterator()` and the
  map views are live, so
  `iterator().remove()`, `ListIterator.set/add` and `Entry.setValue` write through to the backing
  collection, and removal through `keySet()` removes the mapping.
- Equality: `CollectionView` keeps identity `equals`/`hashCode` (the JDK does not override them
  there), so `synchronizedCollection(list).equals(list)` is `false`. `SetView`, `ListView` and
  `MapView` delegate: `equals(o)` is `this == o || backing.equals(o)` and `hashCode()` is the
  backing `hashCode`.
- `synchronizedList` returns a `RandomListView` (adds `implements RandomAccess`) when the backing
  list is a `RandomAccess`, else a `ListView`. As in the JDK, `subList` keeps the wrapper's kind
  (a `RandomListView`'s sublist is a `RandomListView` and a `ListView`'s is a `ListView`) whatever the
  backing sublist is.
- `MapView.keySet()` and `entrySet()` return `SetView`s over the backing sets (so
  `keySet().equals(Set.of())` works), and `values()` returns a `CollectionView`. Like the JDK, each
  view is created on first use and then reused, so `m.values() == m.values()` and
  `m.values().equals(m.values())` hold. (`Collections.unmodifiableMap` still creates new views on
  every call; that is an open difference.)
- The view classes are `java.util.SynchronizedViews$...`, not the JDK's `Collections$Synchronized...`
  names, following the runtime's `UnmodifiableViews` convention.
- A null backing collection raises `NullPointerException` in all four factories
  (`Objects.requireNonNull`). Index failures and `UnsupportedOperationException` (for example when
  wrapping an unmodifiable list) come from the backing collection; the views add no checks of
  their own.
- Single-threaded contract: unlike the JDK, no mutex is acquired around calls, matching the
  exception runtime's single-threaded deployment. Concurrency is not claimed.

Evidence: `BytecodeSynchronizedCollectionsTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
orders, two fresh instances). The sorted and navigable variants are rejected before WAT generation.
