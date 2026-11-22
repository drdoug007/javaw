# Bytecode ConcurrentMap

Revision `concurrent-map-v309` links the interface `java.util.concurrent.ConcurrentMap` in the bundled exception
runtime. The runtime is single-threaded; the interface adds no locking, but its default methods keep the JDK's
putIfAbsent/replace/remove retry loops so user implementations observe the same calls.

Contract:

- `getOrDefault(key, d)` calls `get(key)` once and returns the value when it is not `null`, else `d`. It never
  calls `containsKey`, unlike the `Map` default: a key mapped to `null` gives `d`.
- `forEach(action)` checks `Objects.requireNonNull(action)` first (NPE, null message). It iterates `entrySet()`;
  each entry's `getKey()`/`getValue()` are read inside `try { ... } catch (IllegalStateException) { continue; }`,
  so a stale entry that throws `IllegalStateException` is skipped, while `action.accept(k, v)` is invoked outside
  the try and an `IllegalStateException` thrown by the action itself propagates.
- `replaceAll(f)` checks `Objects.requireNonNull(f)` first, then calls the virtual `forEach` once and, per entry,
  loops `replace(k, v, f.apply(k, v))`; when `replace` fails it re-reads `v = get(k)`, breaks when that is
  `null`, otherwise calls `f` again with the new `v`. It never calls `put` or `Entry.setValue`; a function
  exception propagates and entries already replaced stay replaced.
- `computeIfAbsent(k, f)` checks `Objects.requireNonNull(f)` before any call. `old = get(k)`; a non-null `old`
  returns it without calling `f`. Otherwise `nv = f.apply(k)`; a `null` `nv` stores nothing and returns `null`.
  Otherwise it stores with `putIfAbsent(k, nv)` and returns `nv` when that returned `null`, else the value
  `putIfAbsent` reported. It never calls `put` or `containsKey`.
- `computeIfPresent(k, f)` checks `Objects.requireNonNull(f)` even when the key is absent. Each round:
  `old = get(k)`; `null` ends the loop returning `null`. `nv = f.apply(k, old)`; a `null` `nv` tries
  `remove(k, old)`, otherwise `replace(k, old, nv)`; a `true` result returns `nv` (or `null`), a `false` result
  starts a new round with a fresh `get` and `apply`.
- `compute(k, f)` performs **no** null check: a `null` `f` fails in `f.apply` after `get(k)` was called. Each
  outer round does `old = get(k)`; the inner round applies `f.apply(k, old)`. A non-null `nv` with non-null
  `old` retries `replace(k, old, nv)` from a new outer round on failure; a non-null `nv` with `old == null`
  uses `putIfAbsent(k, nv)` — returning `nv` on `null`, else repeating the inner round with that returned
  value and no new `get`. A `null` `nv` returns `null` when `old == null` (no `remove` call) and otherwise
  retries `remove(k, old)` from a new outer round on failure.
- `merge(k, value, f)` checks `Objects.requireNonNull(f)` first, then `Objects.requireNonNull(value)` (both NPE,
  null message, nothing logged). Each outer round does `old = get(k)`. A non-null `old` applies
  `f.apply(old, value)` (old first); a non-null result retries `replace(k, old, nv)` from a new outer round on
  failure, a `null` result retries `remove(k, old)` from a new outer round on failure and returns `null` on
  success. `old == null` uses `putIfAbsent(k, value)`: a `null` result returns `value` without calling `f`; a
  non-null result repeats the inner round with that value.
- None of the defaults calls `containsKey`, `put` or `remove(key)`. The four atomic operations
  `putIfAbsent(K,V)`, `remove(Object,Object)`, `replace(K,V,V)` and `replace(K,V)` are abstract (re-declared
  without bodies), so implementations must define them; the `Map` defaults for them are shadowed.

Known differences: no `ConcurrentNavigableMap` or `ConcurrentSkipListMap` yet. `ConcurrentHashMap` is linked separately
(see [its contract](bytecode-concurrent-hash-map.md)).

Evidence: `BytecodeConcurrentMapTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
fresh instances). `ConcurrentHashMap.forEach(long, BiConsumer)` (a parallel bulk operation) and
`ConcurrentNavigableMap.keySet()` are rejected before WAT generation.
