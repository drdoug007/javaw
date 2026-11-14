# Bytecode Hashtable

Revision `hashtable-v281` links `java.util.Dictionary` and `java.util.Hashtable` in the bundled exception
runtime. Hashtable is monitor-free (single-threaded contract, like Vector).

Contract:

- `Dictionary<K,V>` is an abstract class with a public no-arg constructor and the seven abstract members
  `size`, `isEmpty`, `keys`, `elements`, `get`, `put`, `remove` and nothing else; `Hashtable` overrides all
  seven and the whole `Map` surface.
- Bucket layout matches the JDK exactly: `index = (hash & 0x7FFFFFFF) % table.length` with
  `hash = key.hashCode()`, new keys go to the **head** of their bucket chain, and replacing the value of an
  existing key keeps its position without bumping `modCount`.
- Growth: the constructor `(capacity, loadFactor)` rejects `capacity < 0` with
  `IllegalArgumentException("Illegal Capacity: n")` before `loadFactor <= 0 || Float.isNaN(loadFactor)` with
  `IllegalArgumentException("Illegal Load: n")`, turns capacity 0 into 1, and sets
  `threshold = (int) Math.min(capacity * loadFactor, Integer.MAX_VALUE - 7)`. `addEntry` checks
  `count >= threshold` **before** inserting (the 9th key of a default table grows it; `new Hashtable<>(0)`
  grows on its first put), doubling to `oldCapacity * 2 + 1`, bumping `modCount`, and re-inserting every
  entry by walking the old buckets from the **highest** index down, each old chain head to tail, pushing
  onto the head of its new bucket. `Hashtable(Map t)` uses capacity `max(2*t.size(), 11)`.
- Iteration order (enumerations, view iterators, `toString`, `equals`) walks buckets from
  `table.length - 1` down to 0, each chain head to tail; `forEach`, `replaceAll` and `hashCode` walk from
  bucket 0 up (the `hashCode` sum does not depend on order).
- Nulls: `put(k, null)` and `contains(null)`/`containsValue(null)` throw a bare
  `NullPointerException`; a null key fails wherever `key.hashCode()` is called (`put`, `get`, `containsKey`,
  `remove`, `getOrDefault`, `putIfAbsent`, `computeIfAbsent`, `putAll(null)`); `putAll` copies in the source's
  iteration order and stops at the first null value, keeping earlier entries.
- `keys()`/`elements()` return `Collections.emptyEnumeration()` and the view iterators return
  `Collections.emptyIterator()` when `count == 0` (their failures have null messages and an iterator made on
  an empty table stays empty after later puts). Otherwise all four share the private inner `Enumerator<T>`
  (types KEYS/VALUES/ENTRIES plus an `iterator` flag) that captures the table and `modCount` when created,
  starts at `index = table.length` and advances with `while (entry == null && index > 0) entry = table[--index]`.
- Fail-fast: only `Iterator.next()` checks `modCount != expectedModCount` (throws
  `ConcurrentModificationException`) before `nextElement()`; `nextElement()` and `hasMoreElements()` never do,
  so an `Enumeration` sees structural changes. `modCount` changes on add, remove, clear and growth, not on
  value replacement. Past the end `nextElement()` throws `NoSuchElementException("Hashtable Enumerator")`.
  `Iterator.remove()` (enumerations cannot remove) unlinks `lastReturned` from the current table's bucket with
  `count--`, `modCount++`, `expectedModCount++`; `remove()` before `next()` throws
  `IllegalStateException("Hashtable Enumerator")` and, if the entry vanished, `ConcurrentModificationException`.
  Unlike `remove(k)` and `EntrySet.remove`, the iterator's remove does not null the removed entry's value.
- Views are created once and cached in fields (`t.keySet() == t.keySet()`), read the table live, and are plain
  `KeySet extends AbstractSet`, `ValueCollection extends AbstractCollection`, `EntrySet extends AbstractSet`
  (not `Collections.synchronizedX` wrappers). `KeySet.remove` is `Hashtable.remove(o) != null`;
  `EntrySet.contains`/`remove` reject non-`Map.Entry` values and otherwise match on the entry's key bucket
  with `e.hash == hash && e.equals(o)`. `add` is inherited (throws `UnsupportedOperationException`).
- `clone()` calls `super.clone()` (catching `CloneNotSupportedException` into `InternalError`) so a subclass
  clones to its own class, then gives the copy a **new** table array whose chains are recursive copies of the
  original chains in the same order (sharing `Entry` objects would leak changes), and resets the cached views
  and `modCount`. `remove`/`remove(k,v)`/`EntrySet.remove` null the removed entry's value.
- `equals` and `toString` go through the overridable `size()` and `entrySet()` like the JDK, so a subclass that
  overrides either (for example a sorted `entrySet()`) changes their result. `equals`: identity,
  `instanceof Map`, `t.size() != size()`, then for each `entrySet()` entry `value.equals(t.get(key))` (a null
  value needs `t.get(key) == null && t.containsKey(key)`), catching `ClassCastException`/`NullPointerException`
  -> false. `toString` prints `{}` when `size() == 0`, else takes `size()` entries from an `entrySet()` iterator
  as `{k=v, k=v}`, using `key.toString()`/`value.toString()` and printing `(this Map)` for a key or value that
  is the table itself. `hashCode` walks the table directly and sums `entry.hashCode()`
  (`hash ^ Objects.hashCode(value)`). `Entry.toString()` is `key.toString() + "=" + value.toString()`, so a held
  entry whose value was nulled by a removal throws `NullPointerException`.
- Overridden `Map` defaults: `getOrDefault` (`v == null ? defaultValue : v`), `forEach`/`replaceAll`
  (bucket 0 up, `Objects.requireNonNull` on the function even on an empty table, `ConcurrentModificationException`
  after each entry when `modCount` changed; `replaceAll` stores `Objects.requireNonNull(result)`), `putIfAbsent`
  (requireNonNull value first, return the existing value if present), `remove(k,v)` (requireNonNull value first,
  unlink only on key+value match), `replace(k,v)`/`replace(k,old,v)` (requireNonNull first, no `modCount`
  change), `computeIfAbsent` (requireNonNull function, return the present value without calling the function,
  else apply it, check `modCount`, add only a non-null result).
- `compute`, `computeIfPresent` and `merge` are overridden like the JDK (not taken from the `Map` defaults).
  Each requires a non-null function, then finds the key's bucket (a null key fails at `key.hashCode()`).
  For a present key it records `modCount`, applies the function (`f(key, old)` for the two compute methods,
  `f(old, value)` for `merge`) and throws `ConcurrentModificationException` if `modCount` changed. A non-null
  result replaces the value with no `modCount` change. A null result unlinks the entry with `count--` and
  `modCount = recorded + 1` and does **not** null the entry's value, so a held `Map.Entry` or a positioned
  `elements()` enumeration still sees the old value (unlike `remove(k)`). For an absent key,
  `computeIfPresent` returns null without calling the function. `compute` applies `f(key, null)`, checks
  `modCount` the same way and adds a non-null result. `merge` never calls the function and adds `value` only
  when it is non-null. `merge` checks only the function for null, so `merge(k, null, f)` returns null without
  adding for an absent key and calls `f(old, null)` for a present one.
- Left out: `protected void rehash()` (the private helper is named `grow()`, so `Hashtable.rehash()` stays
  unlinked), serialization hooks, the `Hashtable(Void)` constructor, the negative-`loadFactor`
  "hash in progress" trick for self-containing tables, and spliterators. `Hashtable` does not extend
  `AbstractMap`.

Evidence: `BytecodeHashtableTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two fresh
instances). `Hashtable.rehash()` is rejected before WAT generation.
