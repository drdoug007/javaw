# T38 EnumMap

```kit
id: T38
slug: enum-map
title: link java.util.EnumMap
tests: BytecodeEnumMapTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/EnumMap.java
allow: docs/bytecode-enum-map.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-enum-map-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeEnumMapTest.java`.

## Goal

Add `java.util.EnumMap` to the bundled runtime: a `Map` whose keys are the constants of one enum type, stored in an
`Object[]` indexed by `ordinal()`. The pinned libraries name `java/util/EnumMap` from 19 classes
(`runtime-target/target/audit/external-types.tsv`: Jackson `EnumMapDeserializer`, `EnumValues`, `ClassUtil`,
`TypeFactory`, Hibernate `SingleIdEntityLoaderStandardImpl`, Spring `CollectionFactory`, `DateFormatter`,
`DateTimeFormatterRegistrar`, Spring Boot `CacheConfigurations`, `AbstractLoggingSystem$LogLevels`,
`JacksonProperties`, SnakeYAML `BaseConstructor`, JAXB `RuntimeEnumLeafInfoImpl`, ...). The 16 missing core-JDK
member rows in `member-references.tsv` are `<init>(Class)` (10 rows), `<init>(EnumMap)`, `<init>(Map)`,
`put(Enum,Object)` (2 rows), `get`, `keySet`, `isEmpty` and `clear`. The task links the whole public `Map` surface
with the JDK's views and iterators, so later libraries need no follow-up.

## Members to implement

One file `javaw-bytecode/src/main/resources/exception-runtime/java/util/EnumMap.java` (`package java.util;`):

`public class EnumMap<K extends Enum<K>, V> extends AbstractMap<K, V> implements java.io.Serializable, Cloneable` with

- fields `private final Class<K> keyType;`, `private Enum<?>[] keyUniverse;`, `private Object[] vals;`,
  `private int size;` and the three view caches `private Set<K> keySet;`, `private Collection<V> values;`,
  `private Set<Map.Entry<K, V>> entrySet;` (none of the last five final: `clone()` replaces them in the copy)
- `private static final Object NULL = new Object() { public int hashCode() { return 0; } public String toString()
  { return "java.util.EnumMap.NULL"; } };` (the stored stand-in for a `null` value)
- `private Object maskNull(Object value)` and `@SuppressWarnings("unchecked") private V unmaskNull(Object value)`
- constructors `public EnumMap(Class<K> keyType)`, `public EnumMap(EnumMap<K, ? extends V> m)`,
  `public EnumMap(Map<K, ? extends V> m)`
- `public int size()`, `public boolean containsValue(Object value)`, `public boolean containsKey(Object key)`,
  `public V get(Object key)`, `public V put(K key, V value)`, `public V remove(Object key)`,
  `public void putAll(Map<? extends K, ? extends V> m)`, `public void clear()`
- private helpers `private boolean containsMapping(Object key, Object value)`,
  `private boolean removeMapping(Object key, Object value)`, `private boolean isValidKey(Object key)`,
  `private void typeCheck(K key)`, `private int entryHashCode(int index)`, `private boolean equals(EnumMap<?, ?> em)`
- `public Set<K> keySet()`, `public Collection<V> values()`, `public Set<Map.Entry<K, V>> entrySet()`
- private inner (non-static) classes:
  - `KeySet extends AbstractSet<K>`: `iterator()`, `size()`, `contains(Object)`, `remove(Object)`, `clear()`
  - `Values extends AbstractCollection<V>`: `iterator()`, `size()`, `contains(Object)`, `remove(Object)`, `clear()`
  - `EntrySet extends AbstractSet<Map.Entry<K, V>>`: `iterator()`, `contains(Object)`, `remove(Object)`, `size()`,
    `clear()`
  - `abstract class EnumMapIterator<T> implements Iterator<T>` with fields `int index = 0;` and
    `int lastReturnedIndex = -1;`, and `hasNext()`, `remove()`
  - `KeyIterator extends EnumMapIterator<K>`, `ValueIterator extends EnumMapIterator<V>` (each `next()`) and
    `EntryIterator extends EnumMapIterator<Map.Entry<K, V>>` (field `private MapEntry lastReturnedEntry;`, `next()`,
    `remove()`)
  - `MapEntry implements Map.Entry<K, V>` with field `private int index;`, constructor `MapEntry(int index)`,
    `getKey()`, `getValue()`, `setValue(V)`, `equals(Object)`, `hashCode()`, `toString()` and
    `private void checkIndexForEntryUse()`. (The JDK calls it `Entry` and nests it in `EntryIterator`; use the name
    `MapEntry` so it never clashes with the inherited `Map.Entry`.)
- `public boolean equals(Object o)`, `public int hashCode()`, `public EnumMap<K, V> clone()`

`isEmpty()`, `toString()`, `getOrDefault`, `putIfAbsent`, `merge` and the other `Map` default methods are inherited
(`AbstractMap`, `Map`) and already behave like the JDK: do not override them.

## Members to leave out

- Serialization: `writeObject`, `readObject`, `serialVersionUID`. The test checks that `EnumMap.writeObject` stays
  unlinked.
- `EntrySet.toArray()` and `EntrySet.toArray(T[])`. Known difference: in the JDK they return
  `AbstractMap.SimpleEntry` copies; here the inherited `AbstractCollection` versions return the live `MapEntry`
  objects. The fixtures never call them.
- Do not edit `AbstractMap.java`, `AbstractSet.java`, `AbstractCollection.java`, `Class.java`, `Enum.java` or any other
  runtime file (`kit guard` rejects it).

## JDK behaviour to match

Storage: `vals[k]` is the value of the key with ordinal `k`; `null` in `vals` means "no mapping", and a mapping to
`null` is stored as `NULL`. `maskNull(v)` is `v == null ? NULL : v`; `unmaskNull(v)` is `(V) (v == NULL ? null : v)`.
`size` counts the non-`null` slots and is updated by every method that fills or empties a slot.

Constructors:

- `EnumMap(Class<K> keyType)`: `this.keyType = keyType; keyUniverse = keyType.getEnumConstants(); vals = new
  Object[keyUniverse.length];`. A `null` class throws `NullPointerException` (from `getEnumConstants()`); a class that
  is not an enum (`String.class`, or `Op.PLUS.getClass()`, the anonymous class of a constant with a body) makes
  `getEnumConstants()` return `null`, so `keyUniverse.length` throws `NullPointerException`. (The JDK has a helpful
  NPE message here; the test checks only the type.) An enum with no constants works: `{}`.
- `EnumMap(EnumMap m)`: copy `keyType`, `keyUniverse` and `size`, and `vals = m.vals.clone()` (the copy is
  independent). `null` -> `NullPointerException`.
- `EnumMap(Map m)`: if `m instanceof EnumMap`, the same four assignments as above (cast to
  `EnumMap<K, ? extends V>`; an empty `EnumMap` passed as a `Map` works). Otherwise
  `if (m.isEmpty()) throw new IllegalArgumentException("Specified map is empty");`, then
  `keyType = m.keySet().iterator().next().getDeclaringClass();` (not `getClass()`: `Op.TIMES` gives `Op`), the
  universe and `vals` as in `EnumMap(Class)`, then `putAll(m)`. A `null` first key or a `null` map ->
  `NullPointerException`; a raw `LinkedHashMap` `{BLUE=b, PEAR=p}` -> `ClassCastException: class Fruit != class Color`
  (from `put`). `keyType` is final: assign it exactly once in each branch.

Keys:

- `isValidKey(key)`: `false` for `null`; otherwise `Class<?> keyClass = key.getClass(); return keyClass == keyType
  || keyClass.getSuperclass() == keyType;`. `get`, `containsKey`, `remove`, `containsMapping` and `removeMapping` use
  it, so a `null` key, another enum's constant, a `String` or an `Integer` gives `null` / `false`, never an exception.
- `typeCheck(key)` (used by `put` only): `Class<?> keyClass = key.getClass(); if (keyClass != keyType &&
  keyClass.getSuperclass() != keyType) throw new ClassCastException(keyClass + " != " + keyType);`. `null` throws
  `NullPointerException` from `getClass()`. Messages print `Class.toString()`: `class Fruit != class Color`,
  `class Op$1 != class Color` for `Op.PLUS`, `class Level != class Op`.

Single mappings:

- `put(key, value)`: `typeCheck(key)`; `int index = key.ordinal(); Object oldValue = vals[index]; vals[index] =
  maskNull(value); if (oldValue == null) size++; return unmaskNull(oldValue);`. `put(GREEN, null)` returns `null`,
  makes `size` 1 and `containsKey(GREEN)` `true`; a second `put(GREEN, null)` keeps `size` 1.
- `get(key)`: `isValidKey(key) ? unmaskNull(vals[((Enum<?>) key).ordinal()]) : null`.
- `containsKey(key)`: `isValidKey(key) && vals[ordinal] != null` (true for a key mapped to `null`).
- `containsValue(value)`: `value = maskNull(value); for (Object val : vals) if (value.equals(val)) return true;
  return false;`. So `containsValue(null)` is `true` only while some key maps to `null`.
- `remove(key)`: `null` if `!isValidKey(key)`; otherwise clear the slot, `size--` if it was filled, and return
  `unmaskNull(oldValue)` (`null` for a key that mapped to `null`, and `size` still goes down).
- `containsMapping(key, value)`: `isValidKey(key) && maskNull(value).equals(vals[ordinal])`.
- `removeMapping(key, value)`: `false` if `!isValidKey(key)`; if `maskNull(value).equals(vals[index])` clear the slot,
  `size--`, return `true`; else `false`.
- `clear()`: `Arrays.fill(vals, null); size = 0;` (keep the same array: running iterators read it).
- `size()` returns the field. `isEmpty()` is inherited (`size() == 0`).

`putAll(m)`: if `m instanceof EnumMap`: `EnumMap<?, ?> em = (EnumMap<?, ?>) m;` and if `em.keyType != keyType`, return
if `em.isEmpty()`, otherwise `throw new ClassCastException(em.keyType + " != " + keyType)` (`class Fruit != class
Color`; for an `EnumMap<Op, ...>` it is `class Op != class Color`, the key type, not `Op$2`). Same key type: for every
`i`, if `em.vals[i] != null` then `if (vals[i] == null) size++; vals[i] = em.vals[i];` (the `NULL` stand-in is copied
as is). Otherwise call `super.putAll(m)`, which calls `put` for each entry in the source's order, so
`putAll(Map.of(Op.MINUS, "m"))` throws `class Op$2 != class Color`, a raw `LinkedHashMap`
`{BLACK=K, APPLE=a, BLUE=B}` stores `BLACK=K` and then throws `class Fruit != class Color`, and `putAll(null)` throws
`NullPointerException`.

Views. `keySet()`, `values()` and `entrySet()` create their view once and cache it in the field, so
`m.keySet() == m.keySet()` is `true`. All views are live; each `size()` returns the map's `size`; each `clear()` calls
`EnumMap.this.clear()`; `iterator()` returns a new `KeyIterator`, `ValueIterator` or `EntryIterator`.

- `KeySet.contains(o)` is `containsKey(o)`. `KeySet.remove(o)`: `int oldSize = size; EnumMap.this.remove(o); return
  size != oldSize;` (true for a key mapped to `null`). `removeIf`, `removeAll`, `retainAll`, `equals` and `toString`
  are inherited and go through the iterator.
- `Values.contains(o)` is `containsValue(o)`. `Values.remove(o)`: `o = maskNull(o);` then for `i` from 0 the first
  `i` with `o.equals(vals[i])` is cleared, `size--`, return `true`; else `false` (`values().remove(null)` removes the
  first key mapped to `null`; with `{RED=r, BLUE=b, YELLOW=b}` `remove("b")` removes `BLUE`).
- `EntrySet.contains(o)`: `o instanceof Map.Entry` and `containsMapping(e.getKey(), e.getValue())`.
  `EntrySet.remove(o)`: `o instanceof Map.Entry` and `removeMapping(e.getKey(), e.getValue())`. (Write
  `if (!(o instanceof Map.Entry)) return false; Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;`.)

Iterators (in ordinal order; no `ConcurrentModificationException`; exceptions without a message unless given):

- `EnumMapIterator.hasNext()`: `while (index < vals.length && vals[index] == null) index++; return index !=
  vals.length;`. It reads the live array: a key put later in ordinal order during iteration is returned, a key removed
  before the iterator reaches it is not.
- `next()` in each subclass: `if (!hasNext()) throw new NoSuchElementException(); lastReturnedIndex = index++;` and
  return `(K) keyUniverse[lastReturnedIndex]` (keys) or `unmaskNull(vals[lastReturnedIndex])` (values). The entry
  iterator instead does `lastReturnedEntry = new MapEntry(index++); return lastReturnedEntry;`.
- `EnumMapIterator.remove()`: `if (lastReturnedIndex < 0) throw new IllegalStateException();`, then if
  `vals[lastReturnedIndex] != null` clear it and `size--`; then `lastReturnedIndex = -1`. Before the first `next()` and
  twice in a row -> `IllegalStateException` (message `null`).
- `EntryIterator.remove()`: `lastReturnedIndex = lastReturnedEntry == null ? -1 : lastReturnedEntry.index;
  super.remove(); lastReturnedEntry.index = lastReturnedIndex; lastReturnedEntry = null;`. (`super.remove()` throws
  before `lastReturnedEntry.index` is touched, and it sets `lastReturnedIndex` to `-1`, so the removed entry gets index
  `-1`.)

`MapEntry` (a live view of slot `index`):

- `checkIndexForEntryUse()`: `if (index < 0) throw new IllegalStateException("Entry was removed");`, called first by
  `getKey()`, `getValue()` and `setValue()`.
- `getKey()`: `(K) keyUniverse[index]`. `getValue()`: `unmaskNull(vals[index])` (so after `m.remove(BLUE)` the entry
  still says key `BLUE`, value `null`; after `m.put(BLACK, "K")` the `BLACK` entry says `K`).
- `setValue(value)`: `V oldValue = unmaskNull(vals[index]); vals[index] = maskNull(value); return oldValue;` (writes
  through; `size` is not changed).
- `equals(o)`: `if (index < 0) return o == this;` then `false` unless `o instanceof Map.Entry`; then
  `e.getKey() == keyUniverse[index] && (ourValue == hisValue || (ourValue != null && ourValue.equals(hisValue)))` with
  `V ourValue = unmaskNull(vals[index]); Object hisValue = e.getValue();`.
- `hashCode()`: `index < 0 ? super.hashCode() : entryHashCode(index)`. `toString()`: `index < 0 ? super.toString() :
  keyUniverse[index] + "=" + unmaskNull(vals[index])` (`RED=null`).

Comparison and hashing:

- `equals(Object o)`: `this == o` -> `true`; `o instanceof EnumMap` -> `equals((EnumMap<?, ?>) o)`; not a `Map` ->
  `false` (`null`, a `List`, the map's own `keySet()`); different `size` -> `false`; otherwise for every filled slot
  `i`: `Object key = keyUniverse[i]; V value = unmaskNull(vals[i]);` and if `value == null` require
  `m.get(key) == null && m.containsKey(key)`, else require `value.equals(m.get(key))`. So `{RED=null}` does not equal
  a `HashMap` `{GREEN=null}`.
- `equals(EnumMap<?, ?> em)`: `em.size != size` -> `false`; `em.keyType != keyType` -> `size == 0` (two empty maps of
  different enums are equal); otherwise every slot must match: `hisValue == ourValue || (hisValue != null &&
  hisValue.equals(ourValue))` on the raw `vals` (both use the same `NULL`).
- `hashCode()`: the sum of `entryHashCode(i)` over the filled slots; `entryHashCode(i)` is
  `keyUniverse[i].hashCode() ^ vals[i].hashCode()` (`NULL.hashCode()` is 0, so the result equals a `HashMap`'s hash
  code with the same mappings; 0 when empty).
- `toString()` is inherited from `AbstractMap`: `{RED=r, BLUE=b}`, `{}`, key `toString()` (`{lvl-0=x, lvl-1=null}`)
  and `(this Map)` for a map that contains itself.

`clone()`: `EnumMap<K, V> result; try { result = (EnumMap<K, V>) super.clone(); } catch
(CloneNotSupportedException e) { throw new InternalError(e); }` then `result.vals = result.vals.clone();` and
`result.keySet = null; result.values = null; result.entrySet = null;` and `return result;`. Without
`vals.clone()` the copy shares the slots. Without resetting the caches, the clone's `keySet()` is the original's view
object, whose methods act on the original map (in the JDK, `AbstractMap.clone()` resets `keySet` and `values`, and
`EnumMap.clone()` resets `entrySet`).

## Steps

1. Read the test `javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeEnumMapTest.java`, then
   `javaw-bytecode/src/main/resources/exception-runtime/java/util/AbstractMap.java` (the superclass; do not edit it)
   and `EnumSet.java` (the same key checks and `clone()` shape) in the same directory.
2. Create `javaw-bytecode/src/main/resources/exception-runtime/java/util/EnumMap.java` with the members above.
3. Register it (do not edit `BytecodeExceptionRuntime.java` by hand):
   `javaw-agent-kit/bin/kit register java/util/EnumMap.java`
4. `javaw-agent-kit/bin/kit check T38` until PASS.
5. Write `docs/bytecode-enum-map.md` (below), the report, then `javaw-agent-kit/bin/kit finish T38`.

## Hints

- The class is about 300 lines written compactly (short methods on one line, `if (...) return x;` on one line). The
  runtime `AbstractMap` has no fields and no `clone()`, so `super.clone()` is `Object.clone()` (a shallow copy) and the
  three view caches must be fields of `EnumMap`.
- `Class.getEnumConstants()` returns a fresh `K[]` (or `null` for a non-enum class): assign it to the
  `Enum<?>[] keyUniverse` field directly and cast with `(K)` only in `KeyIterator.next()` and `MapEntry.getKey()`.
  `Enum.getDeclaringClass()`, `Arrays.fill(Object[], Object)` and `InternalError(Throwable)` exist in the runtime.
- Compare classes with `==` / `!=` (`keyClass == keyType`), never with `equals` or by name.
- Do not use `switch` on enum constants (RULES.md). Write `o instanceof EnumMap` followed by a cast, as above.
- Do not store `null` in `vals` for a mapping to `null`: every write goes through `maskNull`, every read through
  `unmaskNull`. `containsValue(null)` must compare `NULL.equals(val)` (identity), never call `val.equals`.
- The views' `size()` must return the map's `size` field; do not count slots.
- The Wasm runner stops at the **first** failing fixture of each `@Test` method, so after a fix another fixture of the
  same group may fail next. That is progress.
- Calibration mutations, each caught by the fixtures named: no `vals.clone()` in `clone()` (`cloneAndText`); not
  resetting the view caches in `clone()` (`cloneAndText`); `getClass()` instead of `getDeclaringClass()` in
  `EnumMap(Map)` (`constructors`); a `null` value stored unmasked (`nullValues`, `entrySetView`); `KeySet.remove`
  returning the removed value `!= null` (`nullValues`); `EntryIterator.remove()` that leaves the entry's index
  (`entryAfterRemove`); `putAll` without the different-key-type check (`putAllCases`).
- Calibration: the reference passed kit check after 1 run (about 300 new lines).

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
constructors, singleMappings, views, iterators, bulkOperations, equalityCopiesAndText, leavesOutSerialization: if FIX THE RUNTIME says `link the symbolic class/interface java/util/EnumMap`, `link a complete acyclic type hierarchy: java/util/EnumMap`, `link the descriptor class/interface java/util/EnumMap` or names a method of java/util/EnumMap, the class is missing or not registered: run `javaw-agent-kit/bin/kit register java/util/EnumMap.java` (never edit BytecodeExceptionRuntime.java by hand) and add every member listed under Members to implement with the exact signature. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
constructors: EnumMap(Map) of a non-EnumMap takes `m.keySet().iterator().next().getDeclaringClass()` (never getClass(), which is Op$3 for Op.TIMES) and then putAll(m); EnumMap(EnumMap) and EnumMap(Map) of an EnumMap copy keyType, keyUniverse and size and clone vals.
constructorFailures: an empty non-EnumMap throws `new IllegalArgumentException("Specified map is empty")`; a non-enum class makes getEnumConstants() return null and `keyUniverse.length` throws NullPointerException; null arguments must throw NullPointerException; a mixed raw map throws put's `class Fruit != class Color`.
putGetRemove, keyChecks: put calls typeCheck, which throws `new ClassCastException(keyClass + " != " + keyType)` using `key.getClass()` (so `class Op$1 != class Color`) unless keyClass == keyType or keyClass.getSuperclass() == keyType; get, containsKey and remove return null/false for null, Strings, Integers and other enums instead of throwing; put returns the old value and increments size only when the slot was empty.
nullValues: store a null value as NULL (maskNull) and return it as null (unmaskNull); containsKey is true for a key mapped to null; containsValue(null) is `NULL.equals(val)`; remove of a null-mapped key decrements size; Values.remove(null) removes the first null-mapped key; KeySet.remove compares the size before and after.
keySetView, valuesView, entrySetView: keySet(), values() and entrySet() cache their view (same object each call); every view's size() is the map's size; EntrySet.contains/remove use containsMapping/removeMapping (maskNull(value).equals(vals[ordinal])); MapEntry.setValue writes maskNull(value) into vals[index] and returns the old unmasked value.
iterators: hasNext() skips null slots of the live vals array; next() throws `new NoSuchElementException()` at the end; remove() throws `new IllegalStateException()` when lastReturnedIndex < 0 and sets it back to -1 after removing; EntryIterator.remove() sets lastReturnedIndex from lastReturnedEntry before calling super.remove().
entryAfterRemove: after EntryIterator.remove() the removed MapEntry has index -1: getKey/getValue/setValue throw `new IllegalStateException("Entry was removed")` and equals is `o == this`; an entry whose key was removed through the map (not the iterator) keeps its index and reads null.
putAllCases: putAll of an EnumMap with another key type returns if it is empty, else throws `new ClassCastException(em.keyType + " != " + keyType)` (`class Op != class Color`); with the same key type copy every non-null em.vals[i] and count newly filled slots; any other map goes to super.putAll (put per entry, so `class Op$2 != class Color` and earlier entries stay).
equalsAndHash: equals(EnumMap) compares size, then a different key type gives `size == 0`, then the raw slots; equals(Map) requires `m.get(key) == null && m.containsKey(key)` for a null value; hashCode sums keyUniverse[i].hashCode() ^ vals[i].hashCode() over the filled slots (NULL.hashCode() is 0).
cloneAndText: clone() must do `result.vals = result.vals.clone();` and set result.keySet, result.values and result.entrySet to null, otherwise the clone's views act on the original map.
leavesOutSerialization: this fails when EnumMap.writeObject has become linked. Do not add writeObject or readObject.
```

## Docs

Create `docs/bytecode-enum-map.md` with this skeleton (fill in the brackets):

    # Bytecode EnumMap

    Revision `enum-map-vN` links `java.util.EnumMap` in the bundled exception runtime.

    Contract:

    - <constructors and the `Specified map is empty` message; the Object[] slots indexed by ordinal with the NULL
      stand-in for null values; isValidKey for queries and typeCheck with its `class X != class Y` message for put;
      putAll fast path and its message; cached live views; the JDK iterator and MapEntry rules including
      `Entry was removed`; equals/hashCode; clone copies the slots and resets the view caches>

    Known differences: `EntrySet.toArray` returns the live entries (the JDK returns `AbstractMap.SimpleEntry` copies).
    Serialization (`writeObject`, `readObject`) is not linked.

    Evidence: `BytecodeEnumMapTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two fresh
    instances). `EnumMap.writeObject` is rejected before WAT generation.
