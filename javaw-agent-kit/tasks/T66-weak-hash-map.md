# T66 WeakHashMap

```kit
id: T66
slug: weak-hash-map
title: link java.util.WeakHashMap (single-threaded, keys never reclaimed)
tests: BytecodeWeakHashMapTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/WeakHashMap.java
allow: docs/bytecode-weak-hash-map.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-weak-hash-map-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeWeakHashMapTest.java`.

## Goal

Add `java.util.WeakHashMap` to the bundled runtime under the single-threaded, **no-reclamation** contract: entries
hold their keys strongly and are never removed by the runtime. That is a legal JVM outcome (a JVM whose garbage
collector never runs, or never finds the key unreachable, behaves exactly like this), and the runtime has no
GC-observable weak references. `runtime-target/target/audit/core-jdk-linkage.tsv` lists 19 `java/util/WeakHashMap`
signatures as `missing-owner`; `member-references.tsv` has 31 rows in 13 artifacts. Construction:
`<init>()V` in H2 `OnExitDatabaseCloser.<clinit>` (7), AspectJ `ClassLoaderRepository.<init>` (5), jaxb-runtime
`DatatypeConverterImpl.<clinit>` (4), Tomcat `StandardHost.<init>` (3), activation `CommandMap.<clinit>`, ANTLR
`Recognizer.<clinit>`, log4j `LoggerRegistry$WeakMapFactory.createInnerMap`, jaxb-core `ClassFactory$1.initialValue`,
Spring AOP `CglibAopProxy.<clinit>`, Spring core `AbstractClassGenerator.<clinit>` and Spring Boot
`SpringApplicationShutdownHook.<init>`; `<init>(I)V` in Tomcat `ConcurrentCache.<init>`, tomcat-embed-el
`BeanELResolver$ConcurrentCache.<init>` and spring-data `SimpleTypeHolder.<init>`; `<init>(Ljava/util/Map;)V` in
spring-data `SimpleTypeHolder.put` and Spring core `AbstractClassGenerator.create`. Calls (opcode 182): `get` (H2
`SessionLocal.getCurrentValueFor`, Tomcat `CustomObjectInputStream.<init>`, AspectJ
`ClassLoaderRepository.findClassLocal`, jaxb `EditDistance.editDistance`), `put` (H2 `OnExitDatabaseCloser.register`,
AspectJ `storeClassAsReference`, jaxb), `remove` (H2 `OnExitDatabaseCloser.unregister`, AspectJ `removeClass`),
`isEmpty` (H2 `unregister`), `keySet` (H2 `OnExitDatabaseCloser.onShutdown`), `putIfAbsent` (Tomcat
`CustomObjectInputStream`), `containsKey` (jaxb `EditDistance`), `clear` (AspectJ `ClassLoaderRepository.clear`) and
`size` (AspectJ `ClassLoaderRepository.report`). The rest of the class (views, `containsValue`, `putAll`, `forEach`,
`replaceAll`, `entrySet`) is needed so that a `WeakHashMap` passed around as a `Map` behaves like the JDK's.

Copy the JDK 21 *observable* behaviour: a null key (masked with a private `NULL_KEY` sentinel), null values, lookup
by hash then `==` then `argument.equals(storedKey)`, new entries at the head of their bucket, fail-fast iterators
(`ConcurrentModificationException`), cached views, the constructor messages, and exactly the JDK's calls to
overridable methods of `this` (the test logs a subclass).

## Members to implement

One file, `javaw-bytecode/src/main/resources/exception-runtime/java/util/WeakHashMap.java`:
`public class WeakHashMap<K,V> extends AbstractMap<K,V> implements Map<K,V>` (**not** `Cloneable`, **not**
`Serializable`, like the JDK) with

- constants `DEFAULT_INITIAL_CAPACITY = 16`, `MAXIMUM_CAPACITY = 1 << 30`, `DEFAULT_LOAD_FACTOR = 0.75f`,
  `private static final Object NULL_KEY = new Object();`
- fields `Entry<K,V>[] table;`, `private int size;`, `private int threshold;`, `private final float loadFactor;`,
  `int modCount;`, and three view caches `private Set<K> keySetView;`, `private Collection<V> valuesView;`,
  `private Set<Map.Entry<K,V>> entrySetView;` (the runtime `AbstractMap` has no `keySet`/`values` fields)
- helpers `private static Object maskNull(Object key)`, `static Object unmaskNull(Object key)`,
  `final int hash(Object k)`, `private static int indexFor(int h, int length)`, `Entry<K,V> getEntry(Object key)`,
  `void resize(int newCapacity)`, `boolean removeMapping(Object o)`
- `public WeakHashMap(int initialCapacity, float loadFactor)`, `public WeakHashMap(int initialCapacity)`,
  `public WeakHashMap()`, `public WeakHashMap(Map<? extends K, ? extends V> m)`
- `public int size()`, `public boolean isEmpty()`, `public V get(Object key)`, `public boolean containsKey(Object key)`,
  `public V put(K key, V value)`, `public void putAll(Map<? extends K, ? extends V> m)`, `public V remove(Object key)`,
  `public void clear()`, `public boolean containsValue(Object value)`
- `public Set<K> keySet()`, `public Collection<V> values()`, `public Set<Map.Entry<K,V>> entrySet()`
- `public void forEach(BiConsumer<? super K, ? super V> action)`,
  `public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function)`
- `private static class Entry<K,V> implements Map.Entry<K,V>` with `final Object key` (the masked key, held
  strongly), `V value`, `final int hash`, `Entry<K,V> next`; `getKey()` (unmasks), `getValue()`, `setValue(V)`,
  `equals(Object)`, `hashCode()`, `toString()`. Do **not** extend `java.lang.ref.WeakReference`.
- `private abstract class HashIterator<T> implements Iterator<T>` (`hasNext()`, `nextEntry()`, `remove()`) and its
  subclasses `KeyIterator`, `ValueIterator`, `EntryIterator`
- `private class KeySet extends AbstractSet<K>` (`iterator`, `size`, `contains`, `remove`, `clear`),
  `private class Values extends AbstractCollection<V>` (`iterator`, `size`, `contains`, `clear`),
  `private class EntrySet extends AbstractSet<Map.Entry<K,V>>` (`iterator`, `contains`, `remove`, `size`, `clear`,
  `toArray()`, `toArray(T[])`)

Inherited and correct as they are (do not override): `getOrDefault`, `putIfAbsent`, `remove(Object,Object)`,
`replace` (both), `computeIfAbsent`, `computeIfPresent`, `compute`, `merge` (the `Map` defaults, which the JDK
`WeakHashMap` also inherits), and `equals`, `hashCode`, `toString` (`AbstractMap`).

## Members to leave out

- `java.lang.ref` (`WeakReference`, `ReferenceQueue`, `Reference`) and everything built on it in the JDK:
  `expungeStaleEntries`, `getTable`, the `queue` field, `refersTo`. Do not create files under `java/lang/ref/`. The test
  checks that `WeakReference.get()` and `ReferenceQueue.poll()` stay unlinked.
- Spliterators (`WeakHashMapSpliterator`, `KeySpliterator`, ...) and the views' `spliterator()` overrides: the
  inherited defaults are used; the test does not call them.
- `clone`, serialization: the JDK class has neither.
- Do not edit `AbstractMap.java`, `Map.java`, `HashMap.java` or any other runtime file (`kit guard` rejects it).

## JDK behaviour to match

Structure: a power-of-two array of singly linked buckets. An entry's `key` field holds `maskNull(key)` (so it is
never `null`); `getKey()` returns `unmaskNull(key)`.

- `hash(k)`: `int h = k.hashCode(); h ^= (h >>> 20) ^ (h >>> 12); return h ^ (h >>> 7) ^ (h >>> 4);` and
  `indexFor(h, length)`: `h & (length - 1)`. (The fixtures sort their output, so iteration order is not compared,
  but use these so the colliding-key fixture behaves like the JDK.)
- Key match, used by `get`, `containsKey`, `put`, `remove` (`k` is the masked argument): an entry matches when
  `e.hash == h && (e.key == k || k.equals(e.key))`. So the **argument's** `equals` is called, only for entries with
  the same `hashCode()`, and never when the stored key is the same object (a key whose `equals` always returns
  `false` is still found by itself). Walk the bucket from its head.
- `put(key, value)`: if a matching entry exists, replace its value and return the old one; the **stored key stays**
  (the new equal key object is not stored), and `modCount` does **not** change. Otherwise `modCount++`, insert the new
  entry **at the head** of its bucket (`table[i] = new Entry<>(k, value, h, table[i])`), and
  `if (++size > threshold) resize(table.length * 2)`; return `null`.
- `resize(n)`: if the old length is `MAXIMUM_CAPACITY` set `threshold = Integer.MAX_VALUE` and return; else move
  every entry into a new array of length `n` and set `threshold = (int) (n * loadFactor)`. (The JDK's
  "shrink back if half the entries were reclaimed" branch never applies here.)
- Constructors: `(int c, float lf)`: `c < 0` throws `IllegalArgumentException("Illegal Initial Capacity: " + c)`
  (checked first, so `(-1, NaN)` gives the capacity message); clamp `c` to `MAXIMUM_CAPACITY`; `lf <= 0 ||
  Float.isNaN(lf)` throws `IllegalArgumentException("Illegal Load factor: " + lf)` (capital `L`, lower-case `f`;
  `Illegal Load factor: 0.0`, `-1.0`, `NaN`, `-0.5`). The table length is the smallest power of two `>= c` (1 for
  `c == 0`), `threshold = (int) (capacity * lf)`. A load factor above 1 is allowed. `(int c)`: `this(c, 0.75f)`. `()`:
  `this(16, 0.75f)`. `(Map m)`: `this(Math.max((int) Math.ceil(m.size() / (double) 0.75f), 16), 0.75f)` then
  **`putAll(m)`** (virtual). A `null` map throws `NullPointerException` from `m.size()`.
- `putAll(m)`: `n = m.size()` (so `null` throws NPE); return if `n == 0`; if `n > threshold`, grow the table once to
  the smallest power-of-two multiple of its length that is `>= ceil(n / loadFactor)` (capped at
  `MAXIMUM_CAPACITY`); then `put(e.getKey(), e.getValue())` (virtual) for each entry of `m.entrySet()`.
- `remove(key)`: unlink the first matching entry, `modCount++`, `size--`, return its value; `null` if none
  (`modCount` unchanged).
- `size()`: `size`. `isEmpty()`: **`size() == 0`** (virtual). `clear()`: `modCount++`, every bucket `null`,
  `size = 0` (also on an empty map: an open iterator then throws CME). `containsValue(v)`: scan every entry;
  `null` matches a `null` value, otherwise `v.equals(e.value)`.
- `Entry`: `equals(o)`: `o` must be a `Map.Entry`; keys equal by `==` or `k1.equals(k2)` (with `k1 = getKey()`), then
  values the same way. `hashCode()`: `Objects.hashCode(getKey()) ^ Objects.hashCode(getValue())`. `toString()`:
  `getKey() + "=" + getValue()` (`null=x` for the null key). `setValue` writes through to the map.
- `HashIterator`: fields `int index`, `Entry<K,V> entry`, `Entry<K,V> lastReturned`,
  `int expectedModCount = modCount`. The constructor sets `index = isEmpty() ? 0 : table.length` (**calls
  `isEmpty()`**, which the subclass fixture logs as `isEmpty size`). `hasNext()`: if `entry == null`, scan down
  `t[--index]` while `index > 0` for a non-null bucket; true if an entry was found (it never checks `modCount`).
  `nextEntry()`: **first** `if (modCount != expectedModCount) throw new ConcurrentModificationException();` (so an
  exhausted iterator throws CME, not `NoSuchElementException`, after a modification), then
  `if (!hasNext()) throw new NoSuchElementException();`, then `lastReturned = entry; entry = entry.next;`.
  `remove()`: `lastReturned == null` throws `new IllegalStateException()` (checked **before** the modCount check);
  CME if `modCount != expectedModCount`; then **`WeakHashMap.this.remove(lastReturned.getKey())`** (virtual: logged as
  `remove:`), `expectedModCount = modCount; lastReturned = null;`. All exceptions have message `null`.
- Views: `keySet()`, `values()`, `entrySet()` create their view once and return the **same object** every time.
  `KeySet`: `size()` -> `WeakHashMap.this.size()`, `contains(o)` -> `containsKey(o)`, `remove(o)`:
  `if (containsKey(o)) { WeakHashMap.this.remove(o); return true; } return false;`, `clear()` ->
  `WeakHashMap.this.clear()`. `Values`: `size`, `contains(o)` -> `containsValue(o)`, `clear`; `remove` and
  `retainAll` are inherited (they go through the iterator). `EntrySet`: `contains(o)`: `o` is a `Map.Entry`, `e =
  getEntry(o.getKey())` (non-virtual) is not null and `e.equals(o)`; `remove(o)`: `removeMapping(o)` (find by key hash
  and `entry.equals(o)`, unlink, `modCount++`, `size--`); `size`, `clear` as above; `toArray()` / `toArray(a)` return
  **copies** (`new AbstractMap.SimpleEntry<>(e)` per entry in an `ArrayList`, then its `toArray`), so `setValue` on an
  array element does not change the map. `add` on any view throws `UnsupportedOperationException` (inherited).
- `forEach(action)` / `replaceAll(function)`: `Objects.requireNonNull` first (also on an empty map); remember
  `modCount`; for each entry call `action.accept(e.getKey(), e.value)` (or set `e.value = function.apply(...)`),
  step to `e.next`, **then** throw `new ConcurrentModificationException()` if `modCount` changed. So an action that adds
  a key runs once and then CME is thrown (the key stays added); an action that only replaces the value of an existing
  key causes no CME.
- Overridable self-calls (the `Spy` subclass logs `get`, `put`, `remove`, `containsKey`, `containsValue`, `putAll`,
  `size`, `isEmpty`, `clear`, `entrySet`, `keySet`, `values`): `(Map)` constructor -> `putAll` -> `put` per entry;
  `isEmpty` -> `size`; creating any view iterator -> `isEmpty` -> `size`; iterator `remove` -> `remove(key)`;
  `KeySet.size` -> `size`; `KeySet.contains` -> `containsKey`; `KeySet.remove` -> `containsKey` then `remove` (only
  when present); `Values.contains` -> `containsValue`; view `clear` -> `clear`. `get`, `containsKey`, `put`, `remove`,
  `forEach`, `replaceAll`, `EntrySet.contains` call none of them. The `Map` defaults keep their calls
  (`putIfAbsent` -> `get` then `put`; `getOrDefault` -> `get` then `containsKey` when the value is null;
  `remove(k,v)` -> `get` then `remove`).

## Steps

1. Read the test, then `javaw-bytecode/src/main/resources/exception-runtime/java/util/AbstractMap.java`, `Map.java`,
   `AbstractSet.java`, `AbstractCollection.java` and, for style, `HashMap.java` and `IdentityHashMap.java`. The JDK
   reference is `unzip -p ~/.sdkman/candidates/java/21.0.2-graalce/lib/src.zip java.base/java/util/WeakHashMap.java`
   (ignore everything about the reference queue).
2. Create `java/util/WeakHashMap.java` under `javaw-bytecode/src/main/resources/exception-runtime/` with the members
   above.
3. Register it (do not edit `BytecodeExceptionRuntime.java` by hand):
   `javaw-agent-kit/bin/kit register java/util/WeakHashMap.java`
4. `javaw-agent-kit/bin/kit check T66` until PASS.
5. Write `docs/bytecode-weak-hash-map.md` (below), the report, then `javaw-agent-kit/bin/kit finish T66`.

## Hints

- Imports: `java.util.function.BiConsumer`, `java.util.function.BiFunction` (the class is in `java.util`, so
  `Arrays`, `Objects`, `ArrayList`, `ConcurrentModificationException`, ... need no import).
- Create the table with an unchecked cast: `(Entry<K,V>[]) new Entry<?,?>[n]`. Compute the power-of-two capacity
  with a loop (`int cap = 1; while (cap < c) cap <<= 1;`); `HashMap.tableSizeFor` does not exist in the runtime.
- `Entry` is a `private static class` named `Entry`; inside `WeakHashMap` write `Map.Entry<K,V>` for the interface so
  the two do not clash. The iterators and views must be non-static inner classes so they can read `table`,
  `modCount` and call `WeakHashMap.this.remove`.
- Cache the views in the three fields; the inherited `AbstractMap.keySet()`/`values()` create a new object per call
  and are wrong here.
- Calibration pitfalls (each one was caught by the test): appending new entries at the tail of the bucket, or
  dropping the `e.key == k` shortcut (`equalsCalls`/`nullKeyAndKeyIdentity` logs differ, and the `Never` key is not
  found); `keySet()` returning a new view each call; a `HashIterator` constructor that does not call `isEmpty()`
  (subclass log misses `isEmpty size`); `clear()` without `modCount++`; writing `Illegal load factor`.
- `put` on an existing key must not bump `modCount`: the fixture puts an existing key during iteration and expects
  no CME.
- The Wasm runner stops at the first failing fixture of each `@Test` method; after a fix another fixture may fail next.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
basics, nullKeyAndKeyIdentity, equalsCalls, constructors, defaultMethods, equalsHashAndText, views, failFastIterators, subclassCalls: if FIX THE RUNTIME says `link the symbolic class/interface java/util/WeakHashMap`, `link a complete acyclic type hierarchy` or names one of its methods, the class is missing or not registered: run `javaw-agent-kit/bin/kit register java/util/WeakHashMap.java` (never edit BytecodeExceptionRuntime.java by hand) and add every member listed under Members to implement. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
basics: size/isEmpty/get/put/remove/containsValue must handle null values; put on an existing key returns the old value; the class must extend AbstractMap and must not implement Cloneable or Serializable.
nullKeyAndKeyIdentity: store null as the NULL_KEY sentinel and unmask it in getKey(); put on an equal key keeps the original key object; match with `e.key == k || k.equals(e.key)` so the Never key is found by itself.
equalsCalls: compare hash first, then `==`, then argument.equals(storedKey); insert new entries at the head of the bucket (`table[i] = new Entry<>(k, value, h, table[i])`) so the newest colliding key is checked first.
constructors: messages are exactly `Illegal Initial Capacity: <c>` (checked first) and `Illegal Load factor: <lf>` for lf <= 0 or NaN; the Map constructor and putAll call m.size() first and putAll calls put per entry.
defaultMethods: forEach/replaceAll call requireNonNull first, run the action, step to next, then throw ConcurrentModificationException if modCount changed; put on an existing key must not change modCount.
equalsHashAndText: Entry.hashCode is hash(key) ^ hash(value), Entry.toString is key=value; equals/hashCode/toString are inherited from AbstractMap and go through entrySet().
views: keySet(), values() and entrySet() must return the same cached object every call; EntrySet.toArray returns AbstractMap.SimpleEntry copies; EntrySet.remove only removes when key and value both match.
failFastIterators: nextEntry checks modCount before hasNext (CME even when exhausted); remove() checks lastReturned (IllegalStateException, message null) before modCount; clear() increments modCount; iterator remove calls WeakHashMap.this.remove(key) and resyncs expectedModCount.
subclassCalls: the HashIterator constructor calls isEmpty() (which calls size()); KeySet.remove calls containsKey then remove; KeySet.size calls size; Values.contains calls containsValue; the Map constructor calls putAll; do not override putIfAbsent, getOrDefault or remove(Object,Object).
leavesOutReferenceQueueMachinery: this fails when java.lang.ref.WeakReference or ReferenceQueue became linked. Do not add files under java/lang/ref; Entry holds the key in a plain field.
```

## Docs

Create `docs/bytecode-weak-hash-map.md` with this skeleton (fill in the brackets):

    # Bytecode WeakHashMap

    Revision `weak-hash-map-vN` links `java.util.WeakHashMap` in the bundled exception runtime under the
    single-threaded, no-reclamation contract.

    Contract:

    - <chained power-of-two table; null key via NULL_KEY; null values; hash, ==, then argument.equals lookup; head
      insertion; constructor IllegalArgumentException messages; putAll pre-sizing; fail-fast iterators; cached
      key/value/entry views; entrySet toArray copies; forEach/replaceAll with ConcurrentModificationException; the
      overridable self-calls>

    Known differences: keys are held strongly and entries are never reclaimed (a legal JVM outcome when the collector
    never clears the keys; the runtime has no GC-observable weak references). Code that relies on a WeakHashMap to
    release memory keeps every key alive. No `java.lang.ref` classes, no spliterator overrides.

    Evidence: `BytecodeWeakHashMapTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
    fresh instances). `java.lang.ref.WeakReference.get()` and `java.lang.ref.ReferenceQueue.poll()` are rejected
    before WAT generation.
