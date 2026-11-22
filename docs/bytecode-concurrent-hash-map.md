# Bytecode ConcurrentHashMap

Revision `concurrent-hash-map-v313` links `java.util.concurrent.ConcurrentHashMap` in the
bundled exception runtime under the single-threaded contract.

Contract:

- A chained table that always starts with 16 slots, indexed by `spread(hash) & (length - 1)`; new keys are
  appended at the tail of their bin and the table doubles at 3/4 full, moving nodes in bin and chain order.
- Null keys and values are rejected with `NullPointerException` (keys through `key.hashCode()`, also on an empty
  map); `remove(key, null)` returns false; the compute family and `merge` check their arguments before touching
  the map.
- Matching uses the argument's `equals` after identity (`key.equals(e.key)`, `cv.equals(current)`,
  `value.equals(current)` in `containsValue`); `replace` (both forms) and `remove(k, v)` go through
  `replaceNode`; `put`, `putIfAbsent`, `putAll` and the copy constructor through `putVal`.
- `computeIfAbsent`, `computeIfPresent`, `compute` and `merge` call their function at most once; a null result
  removes (or does not add) the key, and an exception leaves the map unchanged.
- `equals` (own nodes through the other map's `get`, then the other map's entries through this map's `get`; a
  null key or value in the other map gives false), `hashCode` and `toString` (`(this Map)` for self references).
- Cached `keySet()` (`KeySetView`), `values()` and `entrySet()` views with the JDK's `contains`, `remove`, `add`,
  `addAll`, `toArray`, `toString`, `containsAll`, `removeAll` and `retainAll` rules (`KeySetView.add` and the
  values view's `add` throw `UnsupportedOperationException`; `entrySet().add` replaces an existing value).
- Weakly consistent iterators that preload one node, read later slots live, remove through `replaceNode` and
  never throw `ConcurrentModificationException`; `MapEntry.setValue` writes through the virtual `put`.
- Overridable self-calls only where the JDK makes them: the copy constructor calls `putAll`, `containsKey` calls
  `get`, the views call `size`/`isEmpty`/`clear`/`containsKey`/`remove`/`containsValue`/`get`/`remove(k, v)`,
  `equals` calls `get` and `MapEntry.setValue` calls `put`.

Known differences: no locks, CAS, counter cells or tree bins (Wasm is single-threaded); the table is always
created with 16 slots, so iteration order for keys outside the documented cases can differ from the JDK;
iterators created before a resize are unspecified. Serialization and the parallel bulk operations are not linked.

Evidence: `BytecodeConcurrentHashMapTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
orders, two fresh instances). `ConcurrentHashMap.forEach(long, BiConsumer)` and `writeObject` are rejected
before WAT generation.
