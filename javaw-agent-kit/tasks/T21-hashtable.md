# T21 Dictionary and Hashtable

```kit
id: T21
slug: hashtable
title: link java.util.Dictionary and a monitor-free java.util.Hashtable
tests: BytecodeHashtableTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/Dictionary.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/Hashtable.java
allow: docs/bytecode-hashtable.md
```

## Goal

Add `java.util.Dictionary<K,V>` and `java.util.Hashtable<K,V>` to the bundled runtime. `Hashtable` is the largest
`missing-owner` class in `runtime-target/target/audit/core-jdk-linkage.tsv` (51 rows): H2 (`DatabaseInfo`: `<init>`,
`put`), Jakarta/Angus Activation (`MimeTypeParameterList`, `MimeTypeFile`: `get`, `put`, `keys`, `remove`, `size`,
`isEmpty`), Tomcat (`NamingContext`, `JNDIRealm`, `Registry`: `clear`, `computeIfAbsent`, `containsKey`, `elements`,
`get`, `keys`, `put`, `remove`), AspectJ (`entrySet`, `elements`, `keys`, `put`, `get`), JAXB txw2 (`clone`, `keys`),
Hibernate (`JndiServiceImpl`), Log4j, Spring Context and Spring Boot (`<init>()`, `<init>(I)`, `put`).

The JDK `Hashtable` is `synchronized`. Here it is **single-threaded**: no locks anywhere (same contract as the
runtime `Vector`, see `docs/bytecode-vector.md`). Iteration order is observable (`toString`, `keys()`,
`keySet()`...), so the bucket layout must be **exactly** the JDK's; the test checks the order for `String` and
`Integer` keys, including after several rehashes.

## Members to implement

`java/util/Dictionary.java`: `public abstract class Dictionary<K,V>` with `public Dictionary()` and the abstract
methods `int size()`, `boolean isEmpty()`, `Enumeration<K> keys()`, `Enumeration<V> elements()`, `V get(Object key)`,
`V put(K key, V value)`, `V remove(Object key)`. Nothing else.

`java/util/Hashtable.java`:
`public class Hashtable<K,V> extends Dictionary<K,V> implements Map<K,V>, Cloneable, java.io.Serializable`
(it does **not** extend `AbstractMap`) with private fields
`Entry<?,?>[] table; int count; int threshold; float loadFactor; int modCount;` the cached views
`Set<K> keySet; Set<Map.Entry<K,V>> entrySet; Collection<V> values;` (no `volatile`, no `transient` needed), and:

- Constructors: `Hashtable(int initialCapacity, float loadFactor)`, `Hashtable(int initialCapacity)` (load 0.75),
  `Hashtable()` (11, 0.75), `Hashtable(Map<? extends K, ? extends V> t)` (`this(Math.max(2*t.size(), 11), 0.75f)`
  then `putAll(t)`).
- `int size()`, `boolean isEmpty()`, `Enumeration<K> keys()`, `Enumeration<V> elements()`,
  `boolean contains(Object value)`, `boolean containsValue(Object value)`, `boolean containsKey(Object key)`,
  `V get(Object key)`, `V put(K key, V value)`, `V remove(Object key)`, `void putAll(Map<? extends K,? extends V> t)`,
  `void clear()`, `Object clone()`, `String toString()`, `boolean equals(Object o)`, `int hashCode()`.
- Views: `Set<K> keySet()`, `Collection<V> values()`, `Set<Map.Entry<K,V>> entrySet()`.
- Map defaults with Hashtable behaviour: `V getOrDefault(Object key, V defaultValue)`,
  `void forEach(BiConsumer<? super K,? super V> action)`,
  `void replaceAll(BiFunction<? super K,? super V,? extends V> function)`, `V putIfAbsent(K key, V value)`,
  `boolean remove(Object key, Object value)`, `boolean replace(K key, V oldValue, V newValue)`,
  `V replace(K key, V value)`, `V computeIfAbsent(K key, Function<? super K,? extends V> mappingFunction)`.
- Private helpers: `private void grow()` (the JDK's `rehash`), `private void addEntry(int hash, K key, V value, int index)`,
  a `private static class Entry<K,V> implements Map.Entry<K,V>` (fields `final int hash; final K key; V value;
  Entry<K,V> next;`), a private inner class `Enumerator<T> implements Enumeration<T>, Iterator<T>`, and three
  private inner view classes `KeySet extends AbstractSet<K>`, `EntrySet extends AbstractSet<Map.Entry<K,V>>`,
  `ValueCollection extends AbstractCollection<V>`.

## Members to leave out

- `protected void rehash()`: name the private helper `grow()` instead. The test checks that `Hashtable.rehash()`
  stays unlinked, so do not declare any method called `rehash`.
- `computeIfPresent`, `compute` and `merge`: do not write them. The `Map` interface defaults already work for
  Hashtable (they call `get`/`put`/`remove`), and the test checks the basic results. (They differ from the JDK only
  in `ConcurrentModificationException` detection.)
- Serialization hooks (`writeObject`, `readObject`), the `Hashtable(Void)` constructor, the `loadFactor < 0`
  "hashCode in progress" trick for self-containing tables, and spliterators.

## JDK behaviour to match

- Layout. `index = (hash & 0x7FFFFFFF) % table.length` with `hash = key.hashCode()`. A new key goes to the **head** of
  its bucket chain: `table[index] = new Entry<>(hash, key, value, table[index])`. Replacing the value of an existing key
  keeps its position and does not change `modCount`.
- Constructor `(cap, load)`: `cap < 0` -> `IllegalArgumentException("Illegal Capacity: " + cap)` (checked first);
  `load <= 0 || Float.isNaN(load)` -> `IllegalArgumentException("Illegal Load: " + load)` (`"Illegal Load: 0.0"`,
  `"-1.0"`, `"NaN"`). `cap == 0` becomes 1. Then `table = new Entry<?,?>[cap]` and
  `threshold = (int) Math.min(cap * load, Integer.MAX_VALUE - 7)`.
- `addEntry(hash, key, value, index)`: **first** `if (count >= threshold) { grow(); index = (hash & 0x7FFFFFFF) %
  table.length; }`, then insert at the head, `count++`, `modCount++`. So the 9th key of a default table triggers the
  growth (threshold 8), and `new Hashtable<>(0)` (capacity 1, threshold 0) grows on its first `put`.
- `grow()`: `newCapacity = oldCapacity * 2 + 1` (11 -> 23 -> 47 -> 95; 3 -> 7 -> 15; 1 -> 3), `modCount++`,
  `threshold = (int) Math.min(newCapacity * loadFactor, Integer.MAX_VALUE - 7)`, then move every entry with
  `for (int i = oldCapacity; i-- > 0;)` (old buckets from the **highest** index down), walking each old chain from its
  head and pushing each entry onto the **head** of its new bucket. This loop order decides the new chain order; do
  not loop upwards.
- Iteration (`keys()`, `elements()`, and the iterators of `keySet()`, `values()`, `entrySet()`, hence `toString`,
  `equals`, view `toString`): buckets from `table.length - 1` **down to 0**, each chain from head to tail.
  `forEach` and `replaceAll` instead go through the buckets from **0 up** (`for (Entry<?,?> e : table)`), each chain
  from head to tail. For keys `1,12,2,13` in a default table: iteration `13 2 12 1`, `forEach` `12 1 13 2`.
- Nulls. `put(k, null)` -> `throw new NullPointerException()` before looking at the key (so `put(null, null)` also has
  message `null`). `contains(null)`/`containsValue(null)` -> `new NullPointerException()`. A null key fails with
  whatever `key.hashCode()` throws (the test checks only the type, `NullPointerException`) in `put`, `get`,
  `containsKey`, `remove`, `getOrDefault`, `putIfAbsent`, `computeIfAbsent`. `putAll(null)` -> NPE (type only).
  `putAll` puts in the source's iteration order and stops at the first null value (earlier entries stay).
- `get`, `containsKey`, `remove` match an entry with `e.hash == hash && e.key.equals(key)`; `contains` scans all
  buckets with `e.value.equals(value)`. `remove` returns the old value or `null`, and bumps `modCount` only on a hit.
  After unlinking, `remove(k)`, `remove(k, v)` and `EntrySet.remove` set the removed entry's `value = null` (the JDK
  does this; the iterator's `remove()` does not).
- `clear()`: null every bucket, `count = 0`, `modCount++`. The capacity is kept.
- `toString()`: `"{}"` when empty, else `"{" + key + "=" + value + ", " ... + "}"` in iteration order (`(this Map)`
  for a key or value that is the table itself is not tested).
- `equals(o)`: `o == this` -> true; not a `Map` or different size -> false; else every entry's
  `value.equals(t.get(key))` (catch `ClassCastException` and `NullPointerException` -> false).
  `hashCode()`: sum over all entries of `entry.hashCode()`, where `Entry.hashCode() = hash ^ Objects.hashCode(value)`.
- `clone()`: `(Hashtable<K,V>) super.clone()` in `try/catch (CloneNotSupportedException e) { throw new InternalError(); }`,
  then a **new** table array of the same length whose chains are **copies** of the entries (same order), the cached
  views reset to `null`, `modCount = 0`. Sharing `Entry` objects with the original is wrong: `copy.put("a", 100)` or
  removing from the copy would change the original.
- `Entry`: `getKey`, `getValue`, `setValue(v)` (`v == null` -> `new NullPointerException()`; returns the old value;
  writes through to the table), `equals(o)` (`o instanceof Map.Entry` with equal key and value), `hashCode()` as
  above, `toString()` = `key + "=" + value`.
- Enumerations and iterators share one class `Enumerator<T>` (type KEYS=0, VALUES=1, ENTRIES=2, and a flag `iterator`):
  - it captures `table` and `expectedModCount = modCount` when created, starts with `index = table.length`, and
    `hasMoreElements()`/`nextElement()` advance with `while (entry == null && index > 0) entry = table[--index];`;
  - past the end `nextElement()` throws `new NoSuchElementException("Hashtable Enumerator")`;
  - `next()` first checks `modCount != expectedModCount` -> `new ConcurrentModificationException()`, then calls
    `nextElement()`. `nextElement()` (the `Enumeration` path) never checks `modCount`;
  - `remove()`: `if (!iterator) throw new UnsupportedOperationException();` first (an `Enumeration` from
    `keys()`/`elements()` cast to `Iterator` cannot remove); then `lastReturned == null` -> `new
    IllegalStateException("Hashtable Enumerator")`; `modCount != expectedModCount` -> CME; else unlink `lastReturned` from the **current** table's bucket, `count--`,
    `modCount++`, `expectedModCount++`, `lastReturned = null`. If `lastReturned` is not found in that bucket, throw
    `new ConcurrentModificationException()`.
  - `keys()`/`elements()` return `Collections.emptyEnumeration()` and the view iterators return
    `Collections.emptyIterator()` **when `count == 0`** (their failures have a `null` message, not "Hashtable
    Enumerator", and an iterator made on an empty table stays empty after later `put`s).
- Views are created once and cached in the fields (`t.keySet() == t.keySet()`). They read the table live. The JDK
  wraps each view in `Collections.synchronizedSet`/`synchronizedCollection`; do not do that here, return the plain
  `KeySet`/`ValueCollection`/`EntrySet` object.
  `KeySet`: `iterator`, `size` (`count`), `contains` (`containsKey`), `remove(o)` (`Hashtable.this.remove(o) != null`),
  `clear`. `ValueCollection`: `iterator`, `size`, `contains` (`containsValue`), `clear`. `EntrySet`: `iterator`,
  `size`, `clear`, `contains(o)` and `remove(o)`: `false` unless `o instanceof Map.Entry`; find the bucket of
  `o.getKey()` and match `e.hash == hash && e.equals(o)`; `remove` unlinks, `count--`, `modCount++`. `add` stays
  the inherited `AbstractCollection.add` (throws `UnsupportedOperationException`).
- Map defaults:
  - `getOrDefault(k, d)`: `V v = get(k); return v == null ? d : v;`.
  - `forEach(action)`: `Objects.requireNonNull(action)` (even on an empty table), buckets 0 up, and after **each**
    `action.accept` `if (expectedModCount != modCount) throw new ConcurrentModificationException();`.
  - `replaceAll(f)`: `Objects.requireNonNull(f)`; buckets 0 up; `entry.value = Objects.requireNonNull(f.apply(key,
    value))`; the same CME check after each entry. A `null` result throws NPE and keeps the earlier replacements.
  - `putIfAbsent(k, v)`: `Objects.requireNonNull(v)` first (even if `k` is present); return the existing value
    if `k` is present, else `addEntry` and return `null`.
  - `remove(k, v)`: `Objects.requireNonNull(v)`; unlink only when key and value are equal; `modCount++`, `count--`.
  - `replace(k, old, v)`: `requireNonNull(old)`, `requireNonNull(v)`; true only if the key is present with a value
    equal to `old`. `replace(k, v)`: `requireNonNull(v)` first; returns the old value or `null` if absent. Neither
    changes `modCount`.
  - `computeIfAbsent(k, f)`: `Objects.requireNonNull(f)`; if `k` is present return its value without calling `f`;
    else `int mc = modCount; V v = f.apply(k); if (mc != modCount) throw new ConcurrentModificationException();`
    and `addEntry` only if `v != null`; return `v`.
  - All `Objects.requireNonNull(x)` calls have no message (the message prints as `null`).

## Steps

1. Read `javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeHashtableTest.java`, then
   `javaw-bytecode/src/main/resources/exception-runtime/java/util/Vector.java` (style, `clone`) and `AbstractMap.java`
   (view classes) in the same directory.
2. Create `java/util/Dictionary.java` under `javaw-bytecode/src/main/resources/exception-runtime/`.
3. Create `java/util/Hashtable.java` next to it with the members above.
4. Register both (do not edit `BytecodeExceptionRuntime.java` by hand):
   `javaw-agent-kit/bin/kit register java/util/Dictionary.java` and
   `javaw-agent-kit/bin/kit register java/util/Hashtable.java`.
5. `javaw-agent-kit/bin/kit check T21` until PASS.
6. Write `docs/bytecode-hashtable.md` (skeleton below), the report, then `kit finish T21`.

## Hints

- Write one private helper `int indexFor(int hash, int length) { return (hash & 0x7FFFFFFF) % length; }` and use it
  everywhere (`get`, `put`, `remove`, `addEntry`, `grow`, the views, the iterator's `remove`).
- `Math.min(float, float)`, `Math.max(int, int)`, `Float.isNaN(float)`, `Objects.requireNonNull(Object)`,
  `Objects.hashCode(Object)`, `Collections.emptyEnumeration()` and `Collections.emptyIterator()` exist in the runtime.
  `AbstractSet`, `AbstractCollection`, `ConcurrentModificationException`, `IllegalStateException`,
  `NoSuchElementException`, `InternalError`, `CloneNotSupportedException` and `java.util.function.BiConsumer`,
  `BiFunction`, `Function` exist too.
- Generic arrays: declare `private Entry<?,?>[] table;` and create it with `new Entry<?,?>[n]`; cast single entries
  with `(Entry<K,V>) table[i]` (add `@SuppressWarnings("unchecked")` if you like).
- The iterator's `next()` for ENTRIES returns the `Entry` object itself, so `setValue` writes through.
- `Enumerator` must keep the `table` it captured (`final Entry<?,?>[] table = Hashtable.this.table;`), but
  `remove()` unlinks from `Hashtable.this.table`.
- Do not write the words `synchronized`, `volatile`, `native` or `Thread` (capital T) anywhere in the runtime files.
  It is the simplest way to stay clear of `kit guard`, which rejects them in code.
- A private `find(Object key)` that returns the matching `Entry` (or `null`) serves `get`, `containsKey`, `put`,
  `putIfAbsent`, both `replace`s and `computeIfAbsent`. A private `unlink` helper (bucket walk with a `prev` pointer)
  serves `remove(k)`, `remove(k,v)` and `EntrySet.remove`.
- The Wasm runner stops at the **first** failing fixture of each `@Test` method, so after a fix a different fixture of
  the same group may fail next. That is progress, not a regression.
- When the mutation "copy only the table array in `clone`" was tried, `cloneIsIndependent` threw in Wasm: removing
  from the copy nulls the shared entry's value and the original's `toString` then hits a null value.
- `getOrDefault` written as `V v = get(key); return v == null ? defaultValue : v;` is all it needs.
- Calibration: the reference passed kit check after 1 run (about 200 new lines: 11 for Dictionary, 190 for
  Hashtable). Mutations of it were used to write the failure hints below; each was caught by the fixture named there.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
customDictionary, hashtableAsDictionary, dictionaryConstructorsAndBasics, bucketLayoutAndOrder: if FIX THE RUNTIME says `link a complete acyclic type hierarchy: java/util/Dictionary` (or `java/util/Hashtable`) or `link the descriptor class/interface java/util/Hashtable`, a class is missing or not registered: run `javaw-agent-kit/bin/kit register java/util/Dictionary.java` and `javaw-agent-kit/bin/kit register java/util/Hashtable.java` (never edit BytecodeExceptionRuntime.java by hand). If it says `link a matching instance method in the declaring hierarchy`, the member the named fixture calls is missing, not public, or has another signature: add it exactly as listed under Members to implement. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
interfaces: Hashtable must be `extends Dictionary<K,V> implements Map<K,V>, Cloneable, java.io.Serializable` and must NOT extend AbstractMap (the JVM prints false for `instanceof AbstractMap`).
constructors, constructorFailures, zeroCapacityLayout: the (int, float) constructor checks `initialCapacity < 0` first ("Illegal Capacity: -1", also for (-1, 0f)), then `loadFactor <= 0 || Float.isNaN(loadFactor)` ("Illegal Load: 0.0", "Illegal Load: -1.0", "Illegal Load: NaN"), then turns capacity 0 into 1 (not 11), and sets `threshold = (int) Math.min(capacity * loadFactor, Integer.MAX_VALUE - 7)`; so `new Hashtable<>(0)` grows on its first put (1 -> 3 -> 7) and keys 0,3,6,9 iterate as `6 3 9 0`.
fromMap: Hashtable(Map t) is `this(Math.max(2 * t.size(), 11), 0.75f); putAll(t);` (14 entries give capacity 28, not 11).
sameBucketNewestFirst, stringOrder, negativeHashes, keysAndElements, viewsAreLive: iteration order is wrong. index = `(hash & 0x7FFFFFFF) % table.length`; a new key goes to the HEAD of its chain (`table[index] = new Entry<>(hash, key, value, (Entry<K,V>) table[index])`); the Enumerator starts at `index = table.length` and walks the buckets DOWN (`while (entry == null && index > 0) entry = table[--index];`) and each chain from head to tail. Expected for keys 0,11,22,33: `{33=v33, 22=v22, 11=v11, 0=v0}`; for 1,12,2,13: `13 2 12 1`.
firstRehash, manyRehashes, smallCapacityGrowth: growth is wrong. In addEntry test `count >= threshold` BEFORE inserting (the 9th key of a default table grows it, not the 10th), then grow to `oldCapacity * 2 + 1` with `modCount++` and `threshold = (int) Math.min(newCapacity * loadFactor, Integer.MAX_VALUE - 7)`, recompute the index for the new length, then insert. grow() must move entries with `for (int i = oldCapacity; i-- > 0;)` (highest old bucket first, each old chain from head) pushing every entry onto the HEAD of its new bucket; a loop from 0 upwards gives a different chain order (smallCapacityGrowth then prints `.../25 10 20 5 15 0/` instead of the JVM's `.../25 10 5 20 0 15/` after the 6th put).
removeKeepsOrder, replacingKeepsPosition: put of an existing key only replaces `e.value` (same position, no modCount change); remove unlinks with a `prev` pointer (`if (prev != null) prev.next = e.next; else table[index] = e.next;`) and keeps the rest of the chain in order.
putAndGet, containment, removeAndClear, putAll, emptyTable, stringForms: toString returns "{}" when count == 0, otherwise `{k=v, k=v}` in iteration order using key.toString() and value.toString(); `contains`/`containsValue` compare with `e.value.equals(value)` over all buckets; `clear` nulls every bucket, sets count = 0 and does modCount++.
putAllStopsAtNullValue, explicitNullFailures, nullKeyFailures: put checks `if (value == null) throw new NullPointerException();` BEFORE touching the key (so put(null, null) prints "NullPointerException: null"); contains(null) and containsValue(null) throw `new NullPointerException()`; a null key just calls key.hashCode() and fails there; putAll loops `for (Map.Entry e : t.entrySet()) put(e.getKey(), e.getValue());` so entries before a null value stay (`{x=1}`).
exhausted, emptyEnumerations, emptyIteratorStaysEmpty: nextElement() past the end throws `new NoSuchElementException("Hashtable Enumerator")`; but when `count == 0` keys()/elements() must return `Collections.emptyEnumeration()` and the view iterators `Collections.emptyIterator()` (their failures print "NoSuchElementException: null" and "IllegalStateException: null", and an iterator made while empty stays empty after a later put).
iteratorRemove, iteratorRemoveFailures: Enumerator.remove(): `if (lastReturned == null) throw new IllegalStateException("Hashtable Enumerator");` then the modCount check, then unlink lastReturned (compare with `==`) from the CURRENT `Hashtable.this.table` bucket `indexFor(lastReturned.hash, tab.length)`, and do `count--; modCount++; expectedModCount++; lastReturned = null;`.
failFast, enumerationIsNotFailFast: only Iterator.next() checks `if (modCount != expectedModCount) throw new ConcurrentModificationException();` before calling nextElement(); Enumeration.nextElement() and hasNext()/hasMoreElements() never check it. modCount changes only when an entry is added or removed, on clear and on growth: replacing the value of an existing key must NOT bump it (failFast expects `again.next()` to work after `t.put("b", 22)`).
keySetOperations, valuesOperations, entryOperations, bulkViewOperations: views extend AbstractSet / AbstractCollection and override iterator(), size() (`count`), clear() (`Hashtable.this.clear()`) and contains (KeySet: containsKey, ValueCollection: containsValue). KeySet.remove(o) is `Hashtable.this.remove(o) != null`. EntrySet.contains/remove return false unless `o instanceof Map.Entry`, then look in the bucket of `((Map.Entry<?,?>) o).getKey()` for `e.hash == hash && e.equals(o)`; remove unlinks it with count-- and modCount++. Do not override add (the inherited one throws UnsupportedOperationException).
viewsAreLive: keySet(), values() and entrySet() create their view once, store it in a field and return the same object every time (`t.keySet() == keySet` is true); the views read `table`/`count` live.
entriesWriteThrough, entryEqualityAndHash: the ENTRIES iterator returns the Entry object itself; Entry.setValue(null) throws `new NullPointerException()`, otherwise sets and returns the old value; Entry.toString is `key + "=" + value`; equals compares key and value of any Map.Entry; hashCode is `hash ^ Objects.hashCode(value)` (for "key"=42 the JVM prints 106079^42 = 106101).
cloneIsIndependent, subclassClone, cloneThenIterate: clone() must call `super.clone()` (try/catch CloneNotSupportedException -> `throw new InternalError()`), then give the copy a NEW `Entry<?,?>[table.length]` whose chains are COPIES of the original chains in the same order (add `Entry<K,V> copy() { return new Entry<>(hash, key, value, next == null ? null : next.copy()); }`), reset keySet/entrySet/values to null and modCount to 0. Copying only the array shares the Entry objects, so changes to the copy leak into the original.
equality, hashing: equals: `o == this` -> true; `!(o instanceof Map)` -> false; different size -> false; then for every entry `if (!value.equals(t.get(key))) return false;` inside try/catch of ClassCastException and NullPointerException (both -> false). hashCode sums `entry.hashCode()` (= `hash ^ value.hashCode()`) over all entries; an empty table gives 0.
forEachOrder, forEachFailFast, replaceAll: forEach and replaceAll walk the buckets from index 0 UP (`for (Entry<?,?> slot : table)`, each chain from head), the opposite of the iterators (keys 1,12,2,13: forEach gives `12 1 13 2`); both start with Objects.requireNonNull of the function (even on an empty table) and after each entry check `if (expectedModCount != modCount) throw new ConcurrentModificationException();`; replaceAll stores `Objects.requireNonNull(function.apply(key, value))`, so a null result throws NPE after the earlier entries were already replaced.
putIfAbsent, conditionalRemoveAndReplace, getOrDefault: Hashtable must override these Map defaults: putIfAbsent, remove(k, v), replace(k, v) and replace(k, old, v) start with Objects.requireNonNull on every value argument BEFORE looking up the key (so `t.remove("b", null)` and `t.replace("zz", null)` throw "NullPointerException: null"); remove(k, v) unlinks only when `e.value.equals(value)`; replace(k, old, v) returns false when the key is absent or its value does not equal old; getOrDefault is `V v = get(key); return v == null ? defaultValue : v;`.
computeIfAbsent, computeIfAbsentFailFast: computeIfAbsent: Objects.requireNonNull(mappingFunction); if the key is present return its value without calling the function; else `int mc = modCount; V v = mappingFunction.apply(key); if (mc != modCount) throw new ConcurrentModificationException();` and addEntry only when v != null; return v. The Map default does not detect that the function changed the table.
inheritedDefaults: merge, compute and computeIfPresent come from the Map interface defaults; do not write them. If this fixture fails, get/put/remove are wrong for the key the fixture uses.
leavesOutRehash: this test fails when Hashtable.java declares a method named `rehash`; rename it to `private void grow()` and nothing else needs to change.
```

## Docs

Create `docs/bytecode-hashtable.md` with this skeleton (fill in the brackets):

    # Bytecode Hashtable

    Revision `hashtable-vN` links `java.util.Dictionary` and `java.util.Hashtable` in the bundled exception
    runtime. Hashtable is monitor-free (single-threaded contract, like Vector).

    Contract:

    - <bucket layout and growth, iteration order (and forEach/replaceAll order), null rejection, views, fail-fast
      iterators, clone, equals/hashCode, the Map defaults it overrides, and the ones it inherits>

    Evidence: `BytecodeHashtableTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two fresh
    instances). `Hashtable.rehash()` is rejected before WAT generation.
