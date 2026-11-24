# T47 ConcurrentHashMap (core map)

```kit
id: T47
slug: concurrent-hash-map
title: link java.util.concurrent.ConcurrentHashMap (single-threaded core map)
tests: BytecodeConcurrentHashMapTest
depends: T46
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/ConcurrentHashMap.java
allow: docs/bytecode-concurrent-hash-map.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-concurrent-hash-map-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeConcurrentHashMapTest.java`.

## Goal

Add `java.util.concurrent.ConcurrentHashMap` to the bundled runtime. The `java.util.concurrent.ConcurrentMap`
interface (with its default methods) already exists from T46; do not edit it. `ConcurrentHashMap` is named by 276 pinned classes (`runtime-target/target/audit/external-types.tsv`, for
example logback `LoggerContext`, Jackson `ObjectMapper`, `InternCache`, H2 `Database`) and `ConcurrentMap` by 78.
The member rows (`member-references.tsv`, owner `java/util/concurrent/ConcurrentHashMap`) are dominated by
`<init>()V` (298, first site logback `LoggerContext.<init>`), `put` (137), `<init>(I)V` (128, Jackson
`TypeNameIdResolver.construct`), `get` (80), `values()` (59, H2 `Database.closeImpl`), `remove(Object)` (29),
`isEmpty` (22), `containsKey` (20), `clear` (18), `putIfAbsent` (11), `<init>(IFI)V` (10, Jackson `InternCache`),
`size` (7), `computeIfAbsent` (3, Tomcat `TimeBucketCounter.increment`), `keySet()` returning
`ConcurrentHashMap$KeySetView` (3, H2 `MVTable.checkDeadlock`), `entrySet` (2), `<init>(IF)V`, `<init>(Map)V`
(Byte Buddy `ByteArrayClassLoader`) and `toString` (1 each). Through the interface (owner
`java/util/concurrent/ConcurrentMap`) the libraries call `get` (79), `putIfAbsent` (36), `put` (29), `remove`
(21 + 7 for `remove(Object,Object)`), `values` (21), `clear` (17), `computeIfAbsent` (14), `keySet` (12),
`containsKey` (9), `computeIfPresent` (4), `compute`, `replace` (both forms) and `containsValue`. The key-set
factories (`newKeySet`, `keySet(V)`), `keys()/elements()`, `mappingCount`, `getOrDefault`, `forEach` and
`replaceAll` follow in T48.

The runtime is **single-threaded**, the same contract as `ConcurrentLinkedQueue`
(`docs/bytecode-concurrent-linked-queue.md`) and `AtomicLong` (`docs/bytecode-atomic-integer-long.md`): no locks,
no CAS, no `volatile`, no `Unsafe`/`VarHandle`, no counter cells, no tree bins and no forwarding nodes. What must
match the JDK is the observable behaviour: null rules, return values, which overridable methods call each other,
and the weakly consistent iterators (never a `ConcurrentModificationException`).

## Storage design (prescribed)

The test pins iteration order only where this design gives the JDK's order, so build exactly this:

- `static final class Node<K,V> { final int hash; final K key; V val; Node<K,V> next; }` (nested, not public).
- `Node<K,V>[] table` created in every constructor with length **16** (always 16, whatever the constructor
  arguments; the arguments are only validated). `int count` holds the number of mappings.
- `static int spread(int h) { return (h ^ (h >>> 16)) & 0x7fffffff; }`; the bin of a key is
  `spread(key.hashCode()) & (table.length - 1)`.
- A new key is appended at the **tail** of its bin. Removing a node unlinks it (`pred.next = e.next`, or
  `table[i] = e.next` for the first node) and leaves the removed node's own `next` unchanged.
- After an insert, if `count >= table.length - (table.length >>> 2)` (12 of 16), resize: a new array of twice the
  length, then move every node (bins in index order, nodes in chain order) to the tail of its new bin. Iterators
  created before a resize are not tested.
- `clear()` sets every slot of the **same** array to `null` and `count = 0`.
- Iteration (all iterators, `toString`, `hashCode`, `equals`, `containsValue`, view methods) walks bins from index 0
  upward and each chain from the first node. With this design, Integer keys `0..15` iterate in ascending order and
  single-letter Strings `"a".."o"` alphabetically, exactly like the JDK; the fixtures sort everything else.

## Members to implement

One new file, `package java.util.concurrent;`. `ConcurrentMap` (T46) supplies the four abstract methods and
default `getOrDefault`, `forEach`, `replaceAll`, `computeIfAbsent`, `computeIfPresent`, `compute` and `merge`;
`ConcurrentHashMap` overrides the compute family and `merge` below and inherits the other three defaults until T48.

`java/util/concurrent/ConcurrentHashMap.java`:
`public class ConcurrentHashMap<K,V> extends java.util.AbstractMap<K,V> implements ConcurrentMap<K,V>, java.io.Serializable`
(not final, **no public method final**, the fixture subclasses it).

- Constructors: `public ConcurrentHashMap()`, `public ConcurrentHashMap(int initialCapacity)`,
  `public ConcurrentHashMap(Map<? extends K, ? extends V> m)`, `public ConcurrentHashMap(int initialCapacity, float
  loadFactor)`, `public ConcurrentHashMap(int initialCapacity, float loadFactor, int concurrencyLevel)`.
- Non-public helpers with these names (not overridable, so self-calls stay invisible to subclasses):
  `final V putVal(K key, V value, boolean onlyIfAbsent)` and `final V replaceNode(Object key, V value, Object cv)`
  (see below), plus `static int spread(int)`.
- Queries: `public int size()`, `public boolean isEmpty()`, `public V get(Object key)`,
  `public boolean containsKey(Object key)`, `public boolean containsValue(Object value)`.
- Writes: `public V put(K key, V value)`, `public void putAll(Map<? extends K, ? extends V> m)`,
  `public V remove(Object key)`, `public void clear()`.
- `ConcurrentMap` methods: `public V putIfAbsent(K key, V value)`, `public boolean remove(Object key, Object value)`,
  `public boolean replace(K key, V oldValue, V newValue)`, `public V replace(K key, V value)`.
- `public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction)`,
  `public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction)`,
  `public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction)`,
  `public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction)`.
- `public boolean equals(Object o)`, `public int hashCode()`, `public String toString()`.
- Views: `public KeySetView<K,V> keySet()`, `public Collection<V> values()`, `public Set<Map.Entry<K,V>> entrySet()`
  (each created once and cached in a field: `m.keySet() == m.keySet()`).
- Nested classes:
  - `abstract static class CollectionView<K,V,E> implements Collection<E>, java.io.Serializable` with field
    `final ConcurrentHashMap<K,V> map`; `public ConcurrentHashMap<K,V> getMap()`, `clear()`, `size()`, `isEmpty()`
    (delegating to `map.clear()/map.size()/map.isEmpty()`), `toArray()`, `toArray(T[] a)`, `toString()`,
    `containsAll(Collection<?>)`, `removeAll(Collection<?>)`, `retainAll(Collection<?>)`.
  - `public static final class KeySetView<K,V> extends CollectionView<K,V,K> implements Set<K>, java.io.Serializable`
    with `private final V value` and a package-private constructor `KeySetView(ConcurrentHashMap<K,V> map, V value)`
    (`keySet()` passes `null`); `contains`, `remove`, `iterator`, `add`, `addAll`, `hashCode`, `equals`.
  - `static final class ValuesView<K,V> extends CollectionView<K,V,V>`: `contains`, `remove`, `iterator`, `add`,
    `addAll`, `removeAll`.
  - `static final class EntrySetView<K,V> extends CollectionView<K,V,Map.Entry<K,V>> implements Set<Map.Entry<K,V>>`:
    `contains`, `remove`, `iterator`, `add`, `addAll`, `hashCode`, `equals`.
  - One iterator base class (fields `Node[] tab`, `int index`, `Node next`, `Node lastReturned`, the map) with key,
    value and entry subclasses, and `static final class MapEntry<K,V> implements Map.Entry<K,V>` (key, val, map).

## Members to leave out

- T48 adds these; do **not** add them here: `newKeySet()`, `newKeySet(int)`, `keySet(V)`,
  `KeySetView.getMappedValue()`, `keys()`, `elements()`, `mappingCount()`, `contains(Object)`, `getOrDefault`,
  `forEach(BiConsumer)`, `replaceAll`, the views' `forEach(Consumer)` and the values/entry `removeIf` overrides.
- The parallel bulk operations (`forEach(long, ...)`, `search*`, `reduce*`, `forEachKey/Value/Entry`): no pinned
  class calls them (no audit rows). The test checks that `forEach(long, BiConsumer)` stays unlinked.
- Serialization (`writeObject`, `readObject`, `Segment`), `spliterator()` overrides (the `Collection` default
  stays), `TreeBin`, `ForwardingNode`, `CounterCell`, `sumCount`. The test checks that `writeObject` is unlinked.

## JDK behaviour to match

- **Nulls.** Every method that takes a key calls `key.hashCode()` first, so a null key throws
  `NullPointerException` (also on an empty map, also for `get`, `containsKey`, `remove(Object)`). `put`, `putIfAbsent`
  and `replace` throw NPE on a null value; `replace(k, old, new)` on any null argument; `containsValue(null)` throws
  NPE. `remove(key, null)` returns `false` (NPE only if the key is null). `computeIfAbsent`, `computeIfPresent`,
  `compute` throw NPE for a null key or function, `merge` for a null key, value or function, all before looking at
  the map. `putAll(null)` throws NPE. Only types are compared for NPEs; write `throw new NullPointerException()`.
- **Matching.** A node matches when `e.hash == h && (e.key == key || key.equals(e.key))` (the **argument's** equals,
  identity first, nodes in chain order). `containsValue(v)` uses `val == v || v.equals(val)` per node in iteration
  order. `replaceNode(key, value, cv)`: find the node; if `cv == null || cv == e.val || cv.equals(e.val)` then
  remember the old value and either set `e.val = value` (value non-null) or unlink the node (value null); return
  the old value, or `null` if nothing matched.
- `put` = `putVal(k, v, false)`: returns the old value (and replaces it) or `null` (and appends a node).
  `putIfAbsent` = `putVal(k, v, true)` (keeps the old value). `remove(k)` = `replaceNode(k, null, null)`.
  `remove(k, v)` = `v != null && replaceNode(k, null, v) != null`. `replace(k, v)` = `replaceNode(k, v, null)`;
  `replace(k, o, n)` = `replaceNode(k, n, o) != null`. `containsKey(k)` = `get(k) != null` (calls the virtual
  `get`). `size()` and `isEmpty()` read `count`.
- **Constructors.** `(int c)` and `(int c, float lf)` delegate to `(c, lf or 0.75f, 1)`. The three-argument form
  throws `new IllegalArgumentException()` (message `null`) when `!(loadFactor > 0.0f) || initialCapacity < 0 ||
  concurrencyLevel <= 0` (so NaN fails). `(Map m)` calls the **virtual** `putAll(m)`. `putAll` iterates
  `m.entrySet()` and calls `putVal(key, value, false)` for each entry (not `put`), so a null value throws NPE after
  the earlier entries were stored.
- **compute family** (no retry loops, the function is called at most once):
  `computeIfAbsent(k, f)`: present -> return the value without calling `f`; else `v = f.apply(k)`, store it if
  non-null, return `v`. `computeIfPresent(k, f)`: absent -> `null` without calling `f`; else `v = f.apply(k, old)`,
  `null` removes the key, otherwise replaces; return `v`. `compute(k, f)`: `v = f.apply(k, oldOrNull)`; `null`
  removes the key if present; return `v`. `merge(k, v, f)`: absent -> store `v`, return `v` (no call); present ->
  `r = f.apply(old, v)`, `null` removes, return `r`. An exception from the function propagates and the map is
  unchanged. None of them calls an overridable method of the map.
- `toString()`: `{k1=v1, k2=v2}` in iteration order, a key or value that is the map itself prints `(this Map)`,
  empty is `{}`. `hashCode()`: sum of `key.hashCode() ^ val.hashCode()`. `equals(o)`: `o == this` -> true; not a
  `Map` -> false; for each own node `v = m.get(key)`, false if `v == null || (v != val && !v.equals(val))`; then for
  each entry of `m.entrySet()` false if its key or value is null or `get(key)` (this map's **virtual** `get`) is null
  or `mv != v && !mv.equals(v)`. So a map with a null key or value never equals a `ConcurrentHashMap`.
- **Views.** `keySet().contains(o)` = `map.containsKey(o)`; `keySet().remove(o)` = `map.remove(o) != null` (both
  virtual). `values().contains(o)` = `map.containsValue(o)`; `values().remove(o)`: `o == null` -> false, else
  iterate and remove the first `o.equals(v)` through the iterator. `entrySet().contains(o)`: `o` is a `Map.Entry`
  with non-null key `k`, `r = map.get(k)` (virtual) non-null, non-null value `v`, `v == r || v.equals(r)`.
  `entrySet().remove(o)`: entry with non-null key and value and `map.remove(k, v)` (virtual).
  `entrySet().add(e)` = `putVal(e.getKey(), e.getValue(), false) == null` (replaces an existing value and returns
  false). `KeySetView.add(e)`: `value == null` -> `throw new UnsupportedOperationException()`, else
  `putVal(e, value, true) == null`; `addAll` throws the same UOE **before** iterating (also for an empty
  collection), else adds each. `values().add/addAll` always throw `new UnsupportedOperationException()`.
- `CollectionView`: `size/isEmpty/clear` call `map.size()/isEmpty()/clear()` (virtual). `toString()` is
  `[a, b]` over `iterator()` (an element that is the view itself prints `(this Collection)`). `containsAll(c)`:
  `c == this` -> true, else false at the first element that is null or not `contains(e)`. `removeAll(c)`: NPE if
  `c == null`; if `c instanceof Set && c.size() > table.length` iterate and `it.remove()` each element with
  `c.contains(e)`, else `modified |= remove(e)` for each element of `c`. `ValuesView.removeAll(c)` always iterates
  (NPE for null). `retainAll(c)`: NPE if null, iterate and `it.remove()` each element with `!c.contains(e)`.
  `toArray()` returns an `Object[]` of exactly the element count (read the map's `count` field, never the virtual
  `size()`); `toArray(a)` uses `a` when `a.length >= count`
  and sets `a[count] = null` when there is room, otherwise returns a new array of `a`'s component type
  (`Arrays.copyOf(a, count)`); `toArray(null)` throws NPE.
- `KeySetView.hashCode()` = sum of key hashCodes; `KeySetView.equals` and `EntrySetView.equals`: `o instanceof Set`
  and (`o == this` or `containsAll(o) && o.containsAll(this)`). `EntrySetView.hashCode()` = sum of
  `key.hashCode() ^ val.hashCode()`.
- **Iterators (weakly consistent).** The constructor takes `tab = map.table` and preloads the first node; `next()`
  throws `new NoSuchElementException()` when no node is preloaded, otherwise remembers the node as `lastReturned`,
  reads its key / value at that moment, preloads the following node (`node.next`, then the next non-empty slot of
  `tab`) and returns. `hasNext()` is `next != null`. So: an element preloaded before it was removed is still
  returned; a key put later into a slot after the current one is seen, one before it is not; a value replaced
  before `next()` is read new. `remove()` throws `new IllegalStateException()` when `lastReturned == null`, else sets
  it to null and calls `replaceNode(lastReturned.key, null, null)` (removes the key whatever its current value;
  nothing if already gone; never calls the virtual `remove`). Never `ConcurrentModificationException`.
- `MapEntry` (returned by the entry iterator): `getKey/getValue`, `toString` = `key + "=" + val`, `hashCode` =
  `key.hashCode() ^ val.hashCode()`, `equals(o)`: `o` is a `Map.Entry` with non-null key and value equal (`==` or
  `o`'s key/value `.equals`) to this one. `setValue(v)`: NPE if `v == null`; `old = val; val = v; map.put(key, v);
  return old` (virtual `put`; re-adds a key that was removed meanwhile).
- Overridable self-calls (the `Spy` fixture logs them): only `(Map)` constructor -> `putAll`, `containsKey` -> `get`,
  the views as above, `equals` -> `get`, and `MapEntry.setValue` -> `put`. `putIfAbsent`, `replace`, `compute*`,
  `merge`, `putAll`, `toString`, `hashCode`, `containsValue`, `clear`, iterator `remove` and `removeAll(List)` paths
  that use `it.remove()` call no overridable map method.

## Steps

1. Read the test, then `javaw-bytecode/src/main/resources/exception-runtime/java/util/AbstractMap.java`,
   `java/util/HashMap.java` and `java/util/concurrent/ConcurrentLinkedQueue.java` (same package, same style).
2. Read `java/util/concurrent/ConcurrentMap.java` (from T46; do not edit it).
3. Create `javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/ConcurrentHashMap.java` with
   the storage design and members above.
4. Register it (never edit `BytecodeExceptionRuntime.java` by hand):
   `javaw-agent-kit/bin/kit register java/util/concurrent/ConcurrentHashMap.java`.
5. `javaw-agent-kit/bin/kit check T47` until PASS.
6. Write `docs/bytecode-concurrent-hash-map.md` (below), the report, then `javaw-agent-kit/bin/kit finish T47`.

## Hints

- Do not copy the JDK source (6000 lines of CAS, tree bins and counter cells). About 300 lines are enough: Node,
  table, `putVal`, `replaceNode`, a `findNode(Object key)` helper used by `get` and the compute family, three
  views, one iterator base class and `MapEntry`.
- Imports: `java.util.*`, `java.util.function.BiFunction`, `java.util.function.Function`. `AbstractMap`,
  `Arrays.copyOf(T[], int)`, `Objects`, `NoSuchElementException` exist in the runtime.
- Generic array creation: `@SuppressWarnings("unchecked") Node<K,V>[] t = (Node<K,V>[]) new Node[16];`.
- Override every `AbstractMap` method listed above: the inherited ones iterate `entrySet()`, which the `Spy` logs
  (`entrySet ` showing up in `subclassCalls` means an inherited method ran).
- `putAll` and the `(Map)` constructor must use `putVal`, not `put`: `subclassCalls` expects `putAll |` for the copy
  constructor and nothing else. The Spy's log is static because `super(m)` runs before subclass fields exist.
- Iterators: copy this shape exactly. Earlier attempts lost whole sessions to javac generics errors
  (`non-static type variable K1 cannot be referenced from a static context`, `name clash: wrap(...) have the same
  erasure`). Declare all four classes as **direct** members of `ConcurrentHashMap`, never nested inside a view.
  Each one declares its own type parameters:

      abstract static class BaseIterator<K,V,T> implements Iterator<T> {
          final ConcurrentHashMap<K,V> map; final Node<K,V>[] tab;
          int index; Node<K,V> next; Node<K,V> lastReturned;
          BaseIterator(ConcurrentHashMap<K,V> map) { this.map = map; this.tab = map.table; next = firstFrom(0); }
          final Node<K,V> firstFrom(int i) { for (index = i; index < tab.length; index++)
              if (tab[index] != null) return tab[index]; return null; }
          public final boolean hasNext() { return next != null; }
          public final T next() {
              Node<K,V> e = next;
              if (e == null) throw new NoSuchElementException();
              lastReturned = e;
              T result = extract(e);
              next = e.next != null ? e.next : firstFrom(index + 1);
              return result;
          }
          abstract T extract(Node<K,V> e);
          public final void remove() { /* IllegalStateException, then map.replaceNode(key, null, null) */ }
      }
      static final class KeyIterator<K,V> extends BaseIterator<K,V,K> {
          KeyIterator(ConcurrentHashMap<K,V> m) { super(m); }
          K extract(Node<K,V> e) { return e.key; } }
      // ValueIterator<K,V> extends BaseIterator<K,V,V>; EntryIterator<K,V> extends BaseIterator<K,V,Map.Entry<K,V>>
      // returning new MapEntry<K,V>(e.key, e.val, map).

  The views create them with `new KeyIterator<K,V>(map)`. The views use the same `<K,V>` names as the map
  (they are static, so there is no clash). They read `tab` once and never check a modification count.
- Compare with the argument on the left: `key.equals(e.key)`, `value.equals(v)`, `cv.equals(e.val)`;
  `keyEquality` and `conditionalWrites` log the receiver of every `equals` call.
- `toString()` must use `append(Object)`-style concatenation so a null-returning `toString` of a key is not an
  issue; use `String.valueOf` for each key and value.
- Do not write the lowercase words `volatile`, `synchronized`, `native` or `Thread` anywhere in the files.
- The Wasm runner stops at the first failing fixture of each `@Test` method.
- Calibration: three Qwen3.8-27B-oQ4e-mtp sessions stalled on iterator generics before any behaviour test ran (with the iterators fixed, the saved attempt passed 7 of 10 tests); a fourth session on gemma-4-31B-it-oQ4e-mtp hit the oMLX prefill memory guard. Model runs so far: none passed (the expected values were produced on JDK 21 with `--release 17`).

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
putGetRemoveAndSize, nullKeysAndValuesAreRejected, constructorsAndPutAll, conditionalWrites, computeAndMerge, equalsHashCodeAndToString, keyValueAndEntryViews, weaklyConsistentIterators, subclassSelfCalls: if FIX THE RUNTIME says `link the symbolic class/interface java/util/concurrent/ConcurrentHashMap` (for `ConcurrentMap`, T46 is not finished), `link a complete acyclic type hierarchy` or names a ConcurrentHashMap method, a file is missing or not registered: run `javaw-agent-kit/bin/kit register java/util/concurrent/ConcurrentHashMap.java` and add every member listed under Members to implement. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes; for `non-static type variable ... static context` or `name clash ... same erasure`, rewrite the iterators exactly as the Iterators hint shows (four direct static members of ConcurrentHashMap, each with its own `<K,V>`).
basics: the table is 16 slots indexed by `spread(hash) & (length-1)`, walked from slot 0 upward; Integer keys 0..15 must print ascending (`{0=v0, 1=v1, 5=v5, ...}`); resize doubles at 12 of 16 and keeps every node.
nullRules: every key method calls `key.hashCode()` first (NPE even on an empty map); null values throw NPE in put/putIfAbsent/replace; `remove(k, null)` returns false; the compute family and merge check their arguments before touching the map.
constructors: the three-argument constructor throws `new IllegalArgumentException()` for `!(loadFactor > 0)`, negative capacity or `concurrencyLevel <= 0`; the other constructors delegate to it; the table is always 16; `putAll` stores entries one by one with `putVal`, so `{0=v0, 1=a}` remains after the NPE.
conditionalWrites: replaceNode compares the expected value as `cv.equals(current)` (log `q?p p?p`), and containsValue as `value.equals(current)`.
computeAndMerge: each function is called at most once; a null result removes (or does not add) the key; computeIfAbsent on a present key and merge on an absent key do not call the function; an exception from the function leaves the map unchanged.
equalsHashAndText: equals walks its own nodes with `m.get(key)` and then `m.entrySet()` with this map's `get`; a null key or value in the other map gives false; toString prints `(this Map)` for a key or value that is the map itself.
keyEquality: match with `e.hash == h && (e.key == key || key.equals(e.key))` in chain order; new keys go to the tail of the bin; removing unlinks the node; keys with equal hash print in insertion order (`{b=B2, d=D, c=C}`).
views: keySet/values/entrySet are cached fields; values() is not a Set; entrySet().add replaces the value and returns false for an existing key; KeySetView.add/addAll and values().add/addAll throw `new UnsupportedOperationException()`; toArray(a) sets `a[n] = null` when a is longer.
entries: MapEntry.setValue rejects null, updates its own value and calls `map.put(key, value)` even if the key was removed; the entry iterator reads the value at next() time.
iterators: the iterator preloads one node and returns it even if it was removed; later slots are read live; remove() calls `replaceNode(key, null, null)` and throws `new IllegalStateException()` without a lastReturned; next() past the end throws `new NoSuchElementException()`; clear() nulls the slots of the same array.
subclassCalls: only the copy constructor -> putAll, containsKey -> get, view size/isEmpty/clear/contains/remove, entrySet contains -> get and remove -> remove(k,v), equals -> get and MapEntry.setValue -> put may call overridable methods. `entrySet ` in the log means an AbstractMap method was inherited instead of overridden.
leavesOutParallelBulkAndSerialization: this fails when `forEach(long, BiConsumer)` or `writeObject` became linked. Do not add the parallel bulk methods or serialization.
```

## Docs

Create `docs/bytecode-concurrent-hash-map.md` with this skeleton (fill in the brackets):

    # Bytecode ConcurrentHashMap

    Revision `concurrent-hash-map-vN` links `java.util.concurrent.ConcurrentHashMap` in the
    bundled exception runtime under the single-threaded contract.

    Contract:

    - <16-slot chained table with spread hashing, tail insertion and doubling at 3/4; null keys and values
      rejected with NullPointerException; argument-first equals matching; putIfAbsent/replace/remove(k,v) through
      replaceNode; compute family and merge with at most one function call; equals/hashCode/toString; cached
      keySet/values/entrySet views; weakly consistent preloading iterators without ConcurrentModificationException;
      MapEntry.setValue writing through put; the overridable self-calls>

    Known differences: no locks, CAS, counter cells or tree bins (the selected deployment is single-threaded); the table is always
    created with 16 slots, so iteration order for keys outside the documented cases can differ from the JDK;
    iterators created before a resize are unspecified. Serialization and the parallel bulk operations are not linked.

    Evidence: `BytecodeConcurrentHashMapTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
    orders, two fresh instances). `ConcurrentHashMap.forEach(long, BiConsumer)` and `writeObject` are rejected
    before WAT generation.
