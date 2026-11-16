# Bytecode EnumMap

Revision `enum-map-v301` links `java.util.EnumMap` in the bundled exception runtime.

Contract:

- Constructors: `EnumMap(Class<K> keyType)` stores `keyType` and takes `keyUniverse =
  keyType.getEnumConstants()` — a `null` class throws `NullPointerException` from `getEnumConstants()`, and a
  non-enum class (including the anonymous `Op$1` class of a constant with a body) makes
  `getEnumConstants()` return `null`, so `keyUniverse.length` throws `NullPointerException`.
  `EnumMap(EnumMap m)` copies `keyType`, `keyUniverse` and `size` and `vals = m.vals.clone()` (the copy is
  independent; `null` gives `NullPointerException`). `EnumMap(Map m)` takes the same four assignments when
  `m instanceof EnumMap` (an empty `EnumMap` passed as a `Map` works); otherwise an empty map throws
  `IllegalArgumentException("Specified map is empty")`, then `keyType` is
  `m.keySet().iterator().next().getDeclaringClass()` — `getDeclaringClass()`, never `getClass()`, so
  `Op.TIMES` still gives `Op` — the universe and `vals` as above, then `putAll(m)`. `null` arguments and a
  `null` first key give `NullPointerException`; a mixed raw `LinkedHashMap` throws `put`'s
  `ClassCastException` (`class Fruit != class Color`).
- Storage: `vals[k]` holds the value of the key with ordinal `k`; `null` in `vals` means "no mapping" and a
  mapping to `null` is stored as the private `NULL` stand-in (`hashCode()` 0, `toString()`
  `java.util.EnumMap.NULL`), every write going through `maskNull` and every read through `unmaskNull`. `size`
  counts the filled slots and is updated by every method that fills or empties a slot. `clear()` is
  `Arrays.fill(vals, null)` on the same array, so running iterators read the live array.
- Keys: queries (`get`, `containsKey`, `remove`, `containsMapping`, `removeMapping`) go through `isValidKey`,
  which is `false` for `null` and for any class that is neither `keyType` nor a direct subclass of it (classes
  compared with `==`, never `equals`), so a `null`, a foreign enum constant, a `String` or an `Integer` gives
  `null`/`false` and never throws. `put` alone runs `typeCheck`, which throws
  `ClassCastException(keyClass + " != " + keyType)` — `class Fruit != class Color`, `class Op$1 != class
  Color`, `class Level != class Op` — and `null` throws `NullPointerException` from `getClass()`. `put`
  returns the old value and increments `size` only when the slot was empty, so `put(GREEN, null)` twice keeps
  `size` 1.
- Values: `containsValue(value)` compares `maskNull(value).equals(val)` over the slots, so
  `containsValue(null)` is true only while some key maps to `null`. `remove(key)` clears the slot, decrements
  `size` only when it was filled, and returns the unmasked old value (`null` for a key mapped to `null`, with
  `size` still going down). `containsMapping`/`removeMapping` compare `maskNull(value)` against the slot.
- `putAll`: for an `EnumMap` argument the fast path copies every non-`null` slot of the source (the `NULL`
  stand-in copied as is) and counts newly filled slots, but a different `em.keyType` returns when the source
  is empty and otherwise throws `ClassCastException(em.keyType + " != " + keyType)` (the key type, `class Op
  != class Color`, not the constant's anonymous class). Any other map goes to `super.putAll`, which puts per
  entry in source order, so `putAll(Map.of(Op.MINUS, "m"))` throws `class Op$2 != class Color`,
  `putAll(null)` throws `NullPointerException`, and the earlier entries of a mixed map stay stored before the
  throw.
- Views: `keySet()`, `values()` and `entrySet()` each create their view once and cache it, so
  `m.keySet() == m.keySet()`. The views are live: each `size()` returns the map's `size`, each `clear()` calls
  `EnumMap.this.clear()`, and `iterator()` returns a fresh `KeyIterator`, `ValueIterator` or `EntryIterator`.
  `KeySet.contains` is `containsKey` and `KeySet.remove` compares the map `size` before and after (true for a
  key mapped to `null`); `Values.contains` is `containsValue` and `Values.remove(o)` clears the first slot
  equal to `maskNull(o)`; `EntrySet.contains`/`remove` go through `containsMapping`/`removeMapping` and return
  `false` for a non-`Map.Entry` argument.
- Iterators: in ordinal order, with no `ConcurrentModificationException`. `hasNext()` skips `null` slots of
  the live `vals` array (a key put later in ordinal order during iteration is returned, a key removed before
  the iterator reaches it is not). `next()` past the end throws `NoSuchElementException`; `remove()` before
  the first `next()` or twice in a row throws `IllegalStateException` (message `null`) and sets
  `lastReturnedIndex` back to `-1`. `EntryIterator.remove()` copies `lastReturnedEntry.index` into
  `lastReturnedIndex` before `super.remove()`, so the removed `MapEntry` is left with index `-1`.
- `MapEntry` (a live view of slot `index`; named to avoid clashing with the inherited `Map.Entry`):
  `getKey()` is `keyUniverse[index]`, `getValue()` the unmasked slot, and `setValue` writes
  `maskNull(value)` through without touching `size`. With index `-1` (removed via the iterator),
  `getKey`/`getValue`/`setValue` throw `IllegalStateException("Entry was removed")`, `equals` is `o == this`,
  and `hashCode`/`toString` fall back to `Object`. An entry whose key was removed through the map (not the
  iterator) keeps its index and reads `null` from the slot. `equals` compares the key by identity against
  `keyUniverse[index]` and the value with `==`/`equals`; `hashCode` is `entryHashCode(index)`; `toString` is
  `keyUniverse[index] + "=" + value` (`RED=null`).
- Equality and hashing: `equals(o)` is `true` for `this`, `equals(EnumMap)` for another `EnumMap`, `false`
  for a non-`Map` (`null`, a `List`, the map's own `keySet()`), and for a `Map` of the same size every filled
  slot must match — a `null` value requires `m.get(key) == null && m.containsKey(key)`, so `{RED=null}` does
  not equal a `HashMap` `{GREEN=null}`. `equals(EnumMap)` compares `size`, then a different `keyType` is equal
  iff both maps are empty, then the raw slots slot by slot (both use the same `NULL`). `hashCode()` sums
  `keyUniverse[i].hashCode() ^ vals[i].hashCode()` over the filled slots (`NULL.hashCode()` is 0, so it
  matches a `HashMap` with the same mappings; 0 when empty). `toString()` is inherited from `AbstractMap`
  (`{RED=r, BLUE=b}`, key `toString()` such as `lvl-0=x`, `(this Map)` for a map that contains itself), as are
  `isEmpty`, `getOrDefault`, `putIfAbsent`, `merge` and the other `Map`/`AbstractMap` defaults.
- `clone()` is `super.clone()` (the runtime `AbstractMap` has no fields, so `Object.clone()`) plus
  `result.vals = result.vals.clone()` and `result.keySet = result.values = result.entrySet = null`: without
  `vals.clone()` the copy shares the slots, and without resetting the caches the clone's views are the
  original's view objects, whose methods act on the original map. `CloneNotSupportedException` is wrapped in
  `InternalError`.

Known differences: `EntrySet.toArray` returns the live entries (the JDK returns `AbstractMap.SimpleEntry` copies).
Serialization (`writeObject`, `readObject`) is not linked.

Evidence: `BytecodeEnumMapTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two fresh
instances). `EnumMap.writeObject` is rejected before WAT generation.
