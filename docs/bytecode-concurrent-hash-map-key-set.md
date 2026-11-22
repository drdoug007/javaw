# Bytecode ConcurrentHashMap key sets

Revision `concurrent-hash-map-key-set-v314` adds the remaining used `java.util.concurrent.ConcurrentHashMap`
members to the T47 single-threaded map.

Contract:

- `newKeySet()` and `newKeySet(int)` return a `KeySetView` over a fresh plain `ConcurrentHashMap` whose value
  is `Boolean.TRUE` (a negative capacity throws `IllegalArgumentException`); `add(e)` is
  `putVal(e, Boolean.TRUE, true) == null`, `add(null)` and `contains(null)` throw `NullPointerException`,
  `addAll(null)` throws from iterating, and `getMap()` is the plain map.
- `keySet(V)` rejects a null mapped value with `NullPointerException` and returns a **new** view on every call:
  `add` keeps an existing key's value (`putIfAbsent` semantics), `addAll` reports whether any key was new,
  `remove`/`clear`/`size` work on the backing map, and `getMappedValue()` returns the mapped value. The cached
  `keySet()` still has `getMappedValue() == null` and `add`/`addAll` throw `UnsupportedOperationException`.
- `keys()` and `elements()` return the T47 key and value iterators, which now also implement `Enumeration`
  (`hasMoreElements()` = `hasNext()`, `nextElement()` = `next()`, `nextElement()` past the end throws
  `NoSuchElementException`); they stay weakly consistent (a preloaded removed node is still returned, later
  slots are read live) and are `Iterator`s. `mappingCount()` returns the count and `contains(value)` goes
  through the virtual `containsValue`.
- `getOrDefault(key, d)` calls the virtual `get` and returns `d` only when it is null (`getOrDefault(k, null)`
  is null for an absent key); `contains(null)` and `getOrDefault(null, d)` throw `NullPointerException` from
  `containsValue` / `get`.
- `forEach(BiConsumer)`, the views' `forEach(Consumer)` and `replaceAll(BiFunction)` reject a null function
  with `NullPointerException` before walking and walk the table live with a non-preloading traverser
  (`LiveWalker`): the next node (`cur.next`, else the next non-empty slot) is found only after the current
  node's callback returned, so a key removed in the callback is skipped — even in the very next slot — and a
  key put in a later slot is seen while the table is not resized. `resize()` copies the nodes, so a walk or
  iterator started before a resize (or `clear()`, which installs a fresh table) keeps following the old
  table's unchanged chains and looks each key up in the current table: every key present throughout is seen
  exactly once with its current value, removed keys are skipped, and keys added after the resize are not seen
  (the JDK's weakly consistent traversal allows either). `replaceAll` applies the function in order, keeps
  the earlier replacements when it throws `NullPointerException` on a null result, and replaces through
  `replaceNode(key, new, old)`; when that fails because the value changed meanwhile, it re-reads the value
  through the virtual `get(key)` and calls the function again, stopping once the key is gone, as the JDK does
  (an empty map never calls the function). The entry view's `forEach` hands out write-through `MapEntry`
  objects (`setValue(null)` throws `NullPointerException`). Apart from that `replaceAll` retry, none of these
  calls `entrySet()`, `get()`, `put()`, `putIfAbsent()` or `size()`.
- `values().removeIf(Predicate)` and `entrySet().removeIf(Predicate)` reject a null filter and, for each node
  read live, test the value — or, for the entry view, an `AbstractMap.SimpleImmutableEntry` whose `setValue`
  throws `UnsupportedOperationException` — and remove through `replaceNode(key, null, value)`; a key removed in
  the filter before it is reached is skipped. `KeySetView.removeIf` stays the `Collection` default (iterator
  plus `remove()`), which is exactly the JDK's behaviour.

Known differences: as in `bytecode-concurrent-hash-map.md`. The parallel bulk operations (`forEach(long, ...)`,
`search*`, `reduce*`) are not linked.

Evidence: `BytecodeConcurrentHashMapKeySetTest` compares Java 17 javac fixtures on the JVM and in Wasm (both
JAR orders, two fresh instances). `forEach(long, BiConsumer)`, `reduceValues`, `search` and `forEachKey` are
rejected before WAT generation.
