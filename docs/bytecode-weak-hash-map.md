# Bytecode WeakHashMap

Revision `weak-hash-map-v334` links `java.util.WeakHashMap` in the bundled exception runtime under the
single-threaded, no-reclamation contract.

Contract:

- A chained power-of-two table (default 16 slots, load factor 0.75, doubling when `size` exceeds the threshold)
  with the JDK supplemental hash. The null key is stored as the private `NULL_KEY` sentinel and unmasked on the way
  out; null values are allowed.
- Lookup compares the hash, then `==`, then `argument.equals(storedKey)`; new entries go at the head of their bucket.
- Constructors: `WeakHashMap()`, `(int)`, `(int, float)` and `(Map)`, with the JDK messages
  `Illegal Initial Capacity: -1` and `Illegal Load factor: NaN`/`-1.0`; the map constructor pre-sizes like the JDK.
- `size`, `isEmpty` (through the virtual `size()`), `get`, `containsKey`, `put`, `putAll` (pre-sizes the table, then
  calls the virtual `put`), `remove`, `clear`, `containsValue` (scans the table from the last slot down).
- Fail-fast iterators: the constructor calls `isEmpty()`, `next` checks `modCount` before `hasNext`, and `remove`
  throws `IllegalStateException` before `ConcurrentModificationException`. `forEach` and `replaceAll` throw
  `ConcurrentModificationException` right after the action that changed the map.
- Cached `keySet`, `values` and `entrySet` views; `entrySet().toArray()` returns `SimpleEntry` copies.
- The other `Map` defaults (`getOrDefault`, `putIfAbsent`, `compute*`, `merge`, ...) and `AbstractMap`
  `equals`/`hashCode`/`toString` are inherited, so subclasses see the JDK's overridable self-calls.

Known differences: keys are held strongly and entries are never reclaimed (a legal JVM outcome when the collector
never clears the keys; the runtime has no GC-observable weak references). Code that relies on a WeakHashMap to
release memory keeps every key alive. No `java.lang.ref` classes, no spliterator overrides.

Evidence: `BytecodeWeakHashMapTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
fresh instances). `java.lang.ref.WeakReference.get()` and `java.lang.ref.ReferenceQueue.poll()` are rejected
before WAT generation. Review also ran the class against JDK 21 `java.util.WeakHashMap` on the JVM: 3,000 random
sequences of 120 operations (null keys, colliding hashes, resizes, views, iterator removal, `replaceAll`,
`compute`, `merge`) and the fail-fast cases matched.
