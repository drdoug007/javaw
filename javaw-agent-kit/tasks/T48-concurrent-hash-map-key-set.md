# T48 ConcurrentHashMap key sets, enumerations and remaining overrides

```kit
id: T48
slug: concurrent-hash-map-key-set
title: link ConcurrentHashMap.newKeySet, keySet(V), keys/elements, mappingCount and the remaining overrides
tests: BytecodeConcurrentHashMapKeySetTest
depends: T47
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/ConcurrentHashMap.java
allow: docs/bytecode-concurrent-hash-map-key-set.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-concurrent-hash-map-key-set-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeConcurrentHashMapKeySetTest.java`.

## Goal

Finish the `java.util.concurrent.ConcurrentHashMap` members the pinned libraries use beyond the T47 core map. The
audit rows (`runtime-target/target/audit/member-references.tsv`) are `newKeySet()` returning
`ConcurrentHashMap$KeySetView` (10 calls, first site Tomcat `HostConfig.<init>`), `keySet(Object)` (1, Tomcat
`UpgradeGroupInfo.<init>`), and on `ConcurrentHashMap$KeySetView` itself `iterator()` (H2 `MVTable.checkDeadlock`),
`removeIf(Predicate)` (Tomcat `TimeBucketCounter$Maintenance.run`) and `toArray(Object[])` (H2
`TcpServer.stopServer`). The `Map`/`ConcurrentMap` defaults `getOrDefault`, `forEach(BiConsumer)` and
`replaceAll` reach `ConcurrentHashMap` through interface calls, and the JDK overrides them with different
observable behaviour (no `entrySet()` call, no `containsKey` call), so they are overridden here too, together with
the small legacy members `keys()`, `elements()`, `contains(Object)` and `mappingCount()`. None of the parallel bulk
methods (`forEach(long, ...)`, `search*`, `reduce*`, `forEachKey/Value/Entry`) has an audit row, so they stay out.

Same single-threaded contract and storage design as T47 (`docs/bytecode-concurrent-hash-map.md`).

## Members to implement

All in the existing `java/util/concurrent/ConcurrentHashMap.java` (no new files, no registration). No public
method final.

- `public static <K> KeySetView<K,Boolean> newKeySet()` = `new KeySetView<K,Boolean>(new ConcurrentHashMap<K,Boolean>(), Boolean.TRUE)`.
- `public static <K> KeySetView<K,Boolean> newKeySet(int initialCapacity)` = the same over
  `new ConcurrentHashMap<K,Boolean>(initialCapacity)` (so a negative capacity throws `IllegalArgumentException`).
- `public KeySetView<K,V> keySet(V mappedValue)`: `throw new NullPointerException()` for null, else a **new**
  `KeySetView<K,V>(this, mappedValue)` each call (not the cached `keySet()`).
- In `KeySetView`: `public V getMappedValue()` (returns `value`; `null` for `keySet()`), and
  `public void forEach(Consumer<? super K> action)`.
- `public Enumeration<K> keys()` and `public Enumeration<V> elements()`: the T47 key and value iterator classes
  also implement `Enumeration` with `hasMoreElements()` (= `hasNext()`) and `nextElement()` (= `next()`); return a
  new one. So `m.keys() instanceof Iterator` is true.
- `public long mappingCount()` (returns `count`), `public boolean contains(Object value)` (= virtual
  `containsValue(value)`).
- `public V getOrDefault(Object key, V defaultValue)`: `V v = get(key); return v == null ? defaultValue : v;`
  (virtual `get`, no `containsKey`).
- `public void forEach(BiConsumer<? super K, ? super V> action)`: NPE if null; walk the nodes and call
  `action.accept(key, val)`.
- `public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function)`: NPE if null; for each node in
  order: `nv = function.apply(key, val)`; `nv == null` -> `throw new NullPointerException()` (earlier nodes stay
  replaced); else `replaceNode(key, nv, val)`.
- In `ValuesView`: `public void forEach(Consumer<? super V> action)`, `public boolean removeIf(Predicate<? super V> filter)`.
- In `EntrySetView`: `public void forEach(Consumer<? super Map.Entry<K,V>> action)`,
  `public boolean removeIf(Predicate<? super Map.Entry<K,V>> filter)`.

## Members to leave out

- The parallel bulk operations: `forEach(long, BiConsumer)`, `forEach(long, BiFunction, Consumer)`, `search`,
  `reduce`, `reduceToLong/Int/Double`, `forEachKey`, `searchKeys`, `reduceKeys*`, `forEachValue`,
  `searchValues`, `reduceValues*`, `forEachEntry`, `searchEntries`, `reduceEntries*`. No pinned class calls them
  and they need `ForkJoinPool`. The test checks `forEach(long, BiConsumer)`, `reduceValues(long, BiFunction)`,
  `search(long, BiFunction)` and `forEachKey(long, Consumer)` stay unlinked.
- `KeySetView.removeIf`: do not override; the `Collection` default (iterator plus `it.remove()`) is exactly the
  JDK's behaviour here.
- `spliterator()` overrides, serialization, and everything T47 left out.

## JDK behaviour to match

- `newKeySet()` sets: `add(e)` = `putVal(e, Boolean.TRUE, true) == null` (the T47 `KeySetView.add` with a non-null
  value); `add(null)` and `contains(null)` throw NPE; `addAll(null)` throws NPE (from iterating); `getMap()` is a
  plain `ConcurrentHashMap` whose values are `true`.
- `keySet(V)` views: `add` keeps an existing key's value (`putIfAbsent` semantics) and returns false; `addAll`
  returns true if any key was new; `remove`, `clear` and `size` work on the backing map. `keySet()` still throws
  `UnsupportedOperationException` (message `null`) from `add`/`addAll` and has `getMappedValue() == null`.
- `keys()`/`elements()` are weakly consistent exactly like the T47 iterators (a preloaded removed key is still
  returned, later slots are read live); `nextElement()` past the end throws `NoSuchElementException` (message
  `null`).
- `contains(null)` and `getOrDefault(null, d)` throw NPE (from `containsValue` / `get`). `getOrDefault(k, null)`
  returns `null` for an absent key.
- `forEach(BiConsumer)` and the views' `forEach(Consumer)` throw NPE for a null action before walking; they walk
  the table live **without preloading** (unlike the T47 iterators): find the next node (`cur.next`, else the next
  non-empty slot) only after the callback for the current node returned, so removing a later key in the callback
  skips it, even the very next one, and putting a later key is seen. The same walk is used by `replaceAll` and
  `removeIf`. They never call `entrySet()`. The entry view's `forEach` passes a T47 `MapEntry` (its `setValue` writes through
  `put`, `setValue(null)` throws NPE).
- `replaceAll` on an empty map never calls the function.
- `values().removeIf(f)` / `entrySet().removeIf(f)`: NPE if `f == null`; for each node read `k = key, v = val`;
  the values view tests `f.test(v)`, the entry view tests `f.test(new AbstractMap.SimpleImmutableEntry<>(k, v))`
  (so `setValue` inside the predicate throws `UnsupportedOperationException`); if true,
  `replaceNode(k, null, v) != null` sets the result to true. Removing an unvisited key inside the predicate
  skips it.
- Overridable self-calls (the `Spy` fixture logs them): `getOrDefault` -> `get`, `contains` -> `containsValue`;
  `forEach`, `replaceAll`, `mappingCount`, `keys`, `elements`, `keySet(V).add/addAll`, the views' `removeIf` and
  `toArray` call no overridable map method (`toArray` reads `count`, not `size()`).

## Steps

1. Read the test, then your T47 `ConcurrentHashMap.java` (iterators, `putVal`, `replaceNode`, `KeySetView`).
2. Add the members above to `javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/ConcurrentHashMap.java`.
   Nothing to register: the file is already registered by T47.
3. `javaw-agent-kit/bin/kit check T48` until PASS, and `javaw-agent-kit/bin/kit check T47` still PASS.
4. Write `docs/bytecode-concurrent-hash-map-key-set.md` (below), the report, then `javaw-agent-kit/bin/kit finish T48`.

## Hints

- `java.util.Enumeration`, `java.util.function.BiConsumer`, `Consumer`, `Predicate` and
  `AbstractMap.SimpleImmutableEntry` exist in the runtime.
- The iterator classes need `implements Iterator<K>, Enumeration<K>`; `nextElement()` just returns `next()`.
- Without the `forEach(BiConsumer)` override the inherited `Map.forEach` calls `entrySet()`: `subclassCalls`
  then shows `entrySet ` in the third section.
- For the walks add a small non-preloading traverser (`Node[] tab; int index; Node cur; Node advance()` returning
  `cur = cur.next` or the next non-empty slot); the T47 preloading iterator would print `2` in `defaultsAndForEach`.
- About 60 new lines. The Wasm runner stops at the first failing fixture of each `@Test` method.
- Do not write the lowercase words `volatile`, `synchronized`, `native` or `Thread` anywhere in the file.
- Calibration: not yet run by a local model (the expected values were produced on JDK 21 with `--release 17`).

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
newKeySet, keySetWithMappedValue, enumerationsAndMappingCount, getOrDefaultAndForEach, replaceAllAndViewRemoveIf, subclassSelfCalls: if FIX THE RUNTIME says `link a matching static method` or `link a matching instance method` for ConcurrentHashMap or `ConcurrentHashMap$KeySetView`, add that member exactly as listed under Members to implement. If it says `link the symbolic class/interface java/util/concurrent/ConcurrentHashMap`, T47 is not finished. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
newKeySets: newKeySet wraps a new ConcurrentHashMap with value Boolean.TRUE; add is `putVal(e, value, true) == null`; KeySetView.removeIf must stay the Collection default; Integer keys 0..9 print ascending.
mappedKeySets: keySet(V) rejects null with NPE and returns a NEW KeySetView each call; add keeps the existing value (`{x=1, y=7}`); keySet() keeps value null, so add throws UnsupportedOperationException and getMappedValue() is null.
legacyAndCounts: keys()/elements() return the key/value iterators, which implement Enumeration (hasMoreElements = hasNext, nextElement = next) and preload one node; mappingCount returns count; contains(v) calls containsValue.
defaultsAndForEach: getOrDefault returns the default only when get(key) is null; forEach walks the live table (a key removed in the callback before it is reached is skipped) and rejects a null action with NPE before walking; entrySet().forEach gives write-through MapEntry objects.
replaceAllAndRemoveIf: replaceAll throws NPE at the first null result after replacing earlier nodes; values/entrySet removeIf call replaceNode(k, null, v) and the entry predicate gets an AbstractMap.SimpleImmutableEntry (setValue throws UnsupportedOperationException).
subclassCalls: only getOrDefault -> get and contains -> containsValue call overridable methods; forEach, replaceAll, keySet(V).add/addAll, removeIf and toArray must not call entrySet(), get(), put(), putIfAbsent() or size().
leavesOutParallelBulkOperations: this fails when a parallel bulk method (forEach/search/reduceValues/forEachKey with a long threshold) became linked. Remove it.
```

## Docs

Create `docs/bytecode-concurrent-hash-map-key-set.md` with this skeleton (fill in the brackets):

    # Bytecode ConcurrentHashMap key sets

    Revision `concurrent-hash-map-key-set-vN` adds the remaining used `java.util.concurrent.ConcurrentHashMap`
    members to the T47 single-threaded map.

    Contract:

    - <newKeySet/newKeySet(int) Boolean.TRUE key sets; keySet(V) views with putIfAbsent add and null rejection;
      getMappedValue; keys()/elements() weakly consistent enumerations; mappingCount and contains; getOrDefault via
      get; forEach/replaceAll walking the live table; values/entrySet forEach and removeIf through replaceNode>

    Known differences: as in `bytecode-concurrent-hash-map.md`. The parallel bulk operations (`forEach(long, ...)`,
    `search*`, `reduce*`) are not linked.

    Evidence: `BytecodeConcurrentHashMapKeySetTest` compares Java 17 javac fixtures on the JVM and in Wasm (both
    JAR orders, two fresh instances). `forEach(long, BiConsumer)`, `reduceValues`, `search` and `forEachKey` are
    rejected before WAT generation.
