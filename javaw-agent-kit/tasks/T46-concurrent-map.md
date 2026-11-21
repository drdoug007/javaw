# T46 ConcurrentMap

```kit
id: T46
slug: concurrent-map
title: link java.util.concurrent.ConcurrentMap with its default methods
tests: BytecodeConcurrentMapTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/ConcurrentMap.java
allow: docs/bytecode-concurrent-map.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-concurrent-map-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeConcurrentMapTest.java`.

## Goal

Add the interface `java.util.concurrent.ConcurrentMap` to the bundled runtime. 78 classes in the pinned libraries
name it (`runtime-target/target/audit/external-types.tsv`), and `member-references.tsv` has `invokeinterface`
(opcode 185) rows on `java/util/concurrent/ConcurrentMap` from 17 artifacts: `get` 79 sites (Jackson, Byte Buddy,
log4j, Tomcat, Hibernate, Spring ...), `putIfAbsent` 36, `put` 29, `values` 21, `remove(Object)` 21, `clear` 17,
`computeIfAbsent(Object,Function)` 14 (Hibernate, Spring core/beans/context/tx/web/data), `keySet` 12, `containsKey`
9, `size` 7, `remove(Object,Object)` 7 (Jackson `PrivateMaxEntriesMap.clear()`, Byte Buddy, Tomcat, Spring core),
`isEmpty` 4, `entrySet` 4, `computeIfPresent` 4, and one each of `replace(K,V,V)`, `replace(K,V)`,
`containsValue` and `compute` (all Tomcat). Fields of type `ConcurrentMap` appear in H2 `RandomAccessStore`, Byte
Buddy `TypeCache`/`Nexus` and Jackson `PrivateMaxEntriesMap`. Without the interface none of these classes link.

The interface only adds default methods on top of `java.util.Map`. They are written for concurrent maps: instead of
`containsKey`/`put`/`remove(key)` they use the atomic `putIfAbsent`, `replace(k, old, new)` and `remove(k, old)`
and **retry** when one of those fails. The runtime is single-threaded, but a user implementation can still make
these calls fail, so the retry loops must be copied exactly. The test uses its own `LogMap implements
ConcurrentMap<String,String>` over a `TreeMap` that logs every call and can be told to fail `replace` /
`putIfAbsent` a set number of times.

## Members to implement

One new file `javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/ConcurrentMap.java`:

```java
package java.util.concurrent;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
public interface ConcurrentMap<K,V> extends Map<K,V> { ... }
```

Abstract (re-declared without a body, so implementations must define them):

- `V putIfAbsent(K key, V value);`
- `boolean remove(Object key, Object value);`
- `boolean replace(K key, V oldValue, V newValue);`
- `V replace(K key, V value);`

Default methods (each overrides the `Map` default):

- `default V getOrDefault(Object key, V defaultValue)`
- `default void forEach(BiConsumer<? super K, ? super V> action)`
- `default void replaceAll(BiFunction<? super K, ? super V, ? extends V> function)`
- `default V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction)`
- `default V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction)`
- `default V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction)`
- `default V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction)`

## Members to leave out

- `ConcurrentHashMap`, `ConcurrentNavigableMap`, `ConcurrentSkipListMap`: separate later tasks. The test checks that
  `ConcurrentHashMap.size()` and `ConcurrentNavigableMap.keySet()` stay unlinked
  (`leavesOutConcurrentHashMapAndNavigableMap`). Do not create those files.
- Do not edit `java/util/Map.java` or any other runtime file. `Map`'s own defaults stay as they are.

## JDK behaviour to match

`k`, `v` below are the key and value; "calls" means virtual calls on `this`, which the test logs.

- `getOrDefault(key, d)`: calls `get(key)` once; returns the value if it is not null, else `d`. It does **not** call
  `containsKey` (unlike `Map`): a key mapped to `null` gives `d`.
- `forEach(action)`: `Objects.requireNonNull(action)` first (NPE, message null, nothing logged). Then
  `for (Map.Entry<K,V> e : entrySet())`: read `k = e.getKey()` and `v = e.getValue()` inside a
  `try { ... } catch (IllegalStateException ise) { continue; }` and call `action.accept(k, v)` **outside** the try.
  So an entry whose `getKey` or `getValue` throws `IllegalStateException` is skipped, but an
  `IllegalStateException` thrown by the action propagates.
- `replaceAll(f)`: `Objects.requireNonNull(f)` first; then calls `this.forEach(...)` (virtual: a subclass sees
  `forEach;`) with a body that, for each `(k, v)`, loops `while (!replace(k, v, f.apply(k, v))) { if ((v = get(k)) == null) break; }`.
  It never calls `put` or `Entry.setValue`. An exception from `f` propagates; entries already replaced stay replaced.
- `computeIfAbsent(k, f)`: `requireNonNull(f)` first. `old = get(k)`; if `old != null` return it (f not called).
  Else `nv = f.apply(k)`; if `nv == null` return `null` (nothing stored). Else `old = putIfAbsent(k, nv)`; return
  `nv` if that returned `null`, otherwise return what `putIfAbsent` returned. Never calls `put` or `containsKey`.
- `computeIfPresent(k, f)`: `requireNonNull(f)` first (NPE even when the key is absent). Loop: `old = get(k)`; if
  `old == null` return `null`. `nv = f.apply(k, old)`; if `nv == null` try `remove(k, old)`, else try
  `replace(k, old, nv)`; if that returned `true` return `nv`, otherwise go round the loop again (new `get`, new
  `apply`).
- `compute(k, f)`: **no** `requireNonNull`: a null `f` throws the NPE from `f.apply` after `get(k)` was called.
  Outer loop: `old = get(k)`. Inner loop: `nv = f.apply(k, old)`.
  - `nv != null` and `old != null`: if `replace(k, old, nv)` return `nv`, else restart the outer loop (new `get`).
  - `nv != null` and `old == null`: `old = putIfAbsent(k, nv)`; if that is `null` return `nv`, else repeat the
    **inner** loop with this `old` (no new `get`).
  - `nv == null`: if `old == null` return `null` (nothing removed); else if `remove(k, old)` return `null`, else
    restart the outer loop.
- `merge(k, value, f)`: `Objects.requireNonNull(f)` **then** `Objects.requireNonNull(value)` (both NPE, message null,
  nothing logged). Outer loop: `old = get(k)`. Inner loop:
  - `old != null`: `nv = f.apply(old, value)` (old first); `nv != null`: if `replace(k, old, nv)` return `nv`;
    `nv == null`: if `remove(k, old)` return `null`; otherwise restart the outer loop.
  - `old == null`: `old = putIfAbsent(k, value)`; if that is `null` return `value` (f not called), else repeat the
    inner loop with this `old`.
- None of the defaults call `containsKey`, `put` or `remove(key)`.

## Steps

1. Read `javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeConcurrentMapTest.java` (the `LogMap`
   class shows exactly which calls are logged).
2. Read `javaw-bytecode/src/main/resources/exception-runtime/java/util/Map.java` to see the defaults you override.
3. Create `javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/ConcurrentMap.java`.
4. Register it: `javaw-agent-kit/bin/kit register java/util/concurrent/ConcurrentMap.java` (never edit
   `BytecodeExceptionRuntime.java` by hand).
5. `javaw-agent-kit/bin/kit check T46` until PASS.
6. Write `docs/bytecode-concurrent-map.md` (below), the report, then `javaw-agent-kit/bin/kit finish T46`.

## Hints

- The JDK 21 source of this interface is short and uses no internal helpers:
  `unzip -p ~/.sdkman/candidates/java/21.0.2-graalce/lib/src.zip java.base/java/util/concurrent/ConcurrentMap.java`.
  The loops are the whole point; write them with labelled `for (;;)` loops (`retry:` outer, `haveOldValue:` inner)
  or equivalent `while` loops, but keep the call order identical.
- In `replaceAll`, the lambda passed to `forEach` assigns to `v`, so copy it to a local first
  (`forEach((k, v0) -> { V v = v0; ... })`) or use an anonymous `BiConsumer`.
- `java.util.Objects.requireNonNull`, `BiConsumer`, `BiFunction` and `Function` exist in the runtime.
- Do not write the lowercase words `volatile`, `synchronized`, `native` or `Thread` anywhere in the file, not even in
  comments.
- The Wasm runner stops at the first failing fixture of each `@Test` method.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
getOrDefaultBasics, forEachBasics, computeIfAbsentBasics, computeBasics: if FIX THE RUNTIME says `link a complete acyclic type hierarchy: java/util/concurrent/ConcurrentMap` or `link the descriptor class/interface ...ConcurrentMap`, the interface is missing or not registered: run `javaw-agent-kit/bin/kit register java/util/concurrent/ConcurrentMap.java`. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
getOrDefaultBasics: getOrDefault calls only get(key) and returns the default when the value is null; it must not call containsKey. Expected starts `1,get(a);|d,get(z);|d,get(n);`.
forEachBasics, forEachSkipsStaleEntries: forEach does requireNonNull(action), iterates entrySet(), reads getKey/getValue inside try/catch(IllegalStateException) -> continue, and calls action.accept OUTSIDE the try so the action's own IllegalStateException propagates (`a=1;c=3;e=5;` then `...from action,2`).
replaceAllBasics, replaceAllFunctionFailure: replaceAll does requireNonNull(f) then calls this.forEach and, per entry, replace(k, v, f.apply(k, v)); never put or setValue. Expected log `forEach;entrySet;replace(a,1,a1);...`.
replaceAllRetries: when replace(k,v,nv) returns false, set v = get(k); break if null, else call f again and retry replace.
computeIfAbsentBasics, computeIfAbsentLosesRace: computeIfAbsent stores with putIfAbsent (not put) and returns putIfAbsent's non-null result when it loses; null from the function stores nothing; requireNonNull(f) before get.
computeIfPresentBasics, computeIfPresentRetries: computeIfPresent uses replace(k,old,nv) or remove(k,old) and loops (get, apply, replace) until one succeeds or get returns null; requireNonNull(f) even when absent.
computeBasics, computeRetries: compute has no requireNonNull (NPE comes from f.apply after get(a)); absent+non-null uses putIfAbsent and on failure re-applies with the returned value WITHOUT calling get; present uses replace/remove and on failure restarts with get; absent+null returns null without remove.
mergeBasics, mergeRetries: merge checks requireNonNull(function) then requireNonNull(value); absent -> putIfAbsent(k,value) and returns value without calling f; present -> f.apply(old, value) then replace or remove(k,old); a lost putIfAbsent re-runs the present branch with the returned value.
leavesOutConcurrentHashMapAndNavigableMap: this fails when ConcurrentHashMap or ConcurrentNavigableMap has become linked. Do not add them in this task.
```

## Docs

Create `docs/bytecode-concurrent-map.md` with this skeleton (fill in the brackets):

    # Bytecode ConcurrentMap

    Revision `concurrent-map-vN` links the interface `java.util.concurrent.ConcurrentMap` in the bundled exception
    runtime. The runtime is single-threaded; the interface adds no locking, but its default methods keep the JDK's
    putIfAbsent/replace/remove retry loops so user implementations observe the same calls.

    Contract:

    - <getOrDefault without containsKey; forEach skipping entries that throw IllegalStateException; replaceAll via
      forEach and replace(k, v, nv) with retry; computeIfAbsent/computeIfPresent/compute/merge call order, retry and
      null handling; which methods check for null and in what order>

    Known differences: no `ConcurrentHashMap`, `ConcurrentNavigableMap` or `ConcurrentSkipListMap` yet.

    Evidence: `BytecodeConcurrentMapTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
    fresh instances). `ConcurrentHashMap.size()` and `ConcurrentNavigableMap.keySet()` are rejected before WAT
    generation.
