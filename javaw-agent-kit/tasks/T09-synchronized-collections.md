# T09 Collections.synchronized wrappers

```kit
id: T09
slug: synchronized-collections
title: link monitor-free Collections.synchronizedCollection/Set/List/Map
tests: BytecodeSynchronizedCollectionsTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/Collections.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/SynchronizedViews.java
allow: docs/bytecode-synchronized-collections.md
```

## Goal

Add `Collections.synchronizedCollection`, `synchronizedSet`, `synchronizedList` and `synchronizedMap`.
Logback, Jackson Databind, AspectJ, H2, Spring Core/Context/Boot/Data, JAXB and Tomcat call them. The JDK
versions lock a mutex around every call. Here they are **monitor-free delegating views** (single-threaded
contract, like `docs/bytecode-atomic-reference-array.md`): every call is forwarded to the backing
collection, so all changes write through in both directions.

## Members to implement

In `java/util/Collections.java` add exactly these four public static methods:

- `public static <T> Collection<T> synchronizedCollection(Collection<T> c)`
- `public static <T> Set<T> synchronizedSet(Set<T> s)`
- `public static <T> List<T> synchronizedList(List<T> list)`
- `public static <K,V> Map<K,V> synchronizedMap(Map<K,V> m)`

Put the view classes in a new package-private `final class SynchronizedViews` in
`java/util/SynchronizedViews.java` (model: `java/util/UnmodifiableViews.java`), with nested static classes:

- `CollectionView<E> implements Collection<E>, java.io.Serializable` holding `final Collection<E> c`.
- `SetView<E> extends CollectionView<E> implements Set<E>`.
- `ListView<E> extends CollectionView<E> implements List<E>` holding `final List<E> list`.
- `RandomListView<E> extends ListView<E> implements RandomAccess`.
- `MapView<K,V> implements Map<K,V>, java.io.Serializable` holding `final Map<K,V> m`.

## Members to leave out

`synchronizedSortedSet`, `synchronizedSortedMap`, `synchronizedNavigableSet`, `synchronizedNavigableMap`
stay unlinked (this test and `BytecodeSortedCollectionsTest` check they are rejected). Do not add the
package-private `(…, Object mutex)` overloads either.

## JDK behaviour to match

- Null backing collection: `NullPointerException` (use `Objects.requireNonNull(c)` in each view constructor).
- CollectionView forwards **every** `Collection` method to `c`: `size`, `isEmpty`, `contains`, `toArray()`,
  `toArray(T[])`, `iterator`, `add`, `remove`, `containsAll`, `addAll`, `removeAll`, `retainAll`, `clear`,
  `toString`, and the defaults `forEach`, `removeIf`, `spliterator`, `stream`. `iterator()` returns
  `c.iterator()` itself, so `iterator().remove()` writes through.
- **Equality**: `CollectionView` does **not** override `equals`/`hashCode` (identity, like the JDK):
  `synchronizedCollection(list).equals(list)` is `false`. `SetView`, `ListView` and `MapView` delegate:
  `equals(o)` is `this == o || c.equals(o)`, `hashCode()` is `c.hashCode()`.
- ListView also forwards `get`, `set`, `add(int,E)`, `remove(int)`, `indexOf`, `lastIndexOf`,
  `addAll(int,Collection)`, `listIterator()`, `listIterator(int)`, `replaceAll`, `sort`. Index failures come
  from the backing list (do not check indexes yourself).
- `synchronizedList(list)` returns a `RandomListView` when `list instanceof RandomAccess`, else a `ListView`.
  Write `subList` once, in `ListView` only: `return SynchronizedViews.list(list.subList(a, b));` (the same
  factory as `synchronizedList`). Do not override it in `RandomListView`.
- MapView forwards `size`, `isEmpty`, `containsKey`, `containsValue`, `get`, `put`, `remove(Object)`, `putAll`,
  `clear`, `toString`, `equals`, `hashCode` and the defaults `getOrDefault`, `forEach`, `replaceAll`,
  `putIfAbsent`, `remove(Object,Object)`, `replace(K,V,V)`, `replace(K,V)`, `computeIfAbsent`,
  `computeIfPresent`, `compute`, `merge`. `keySet()` returns `new SetView<>(m.keySet())`, `entrySet()`
  returns `new SetView<>(m.entrySet())`, `values()` returns `new CollectionView<>(m.values())`. They write
  through (removing from `keySet()` removes the mapping; `Entry.setValue` changes the map).
- Wrapping an unmodifiable list gives a view whose mutators still throw `UnsupportedOperationException`
  (it comes from the backing list).

## Steps

1. Create `javaw-bytecode/src/main/resources/exception-runtime/java/util/SynchronizedViews.java`
   (`package java.util; import java.util.function.*;`) with the five nested classes above.
2. Add the four static methods to `Collections.java`, next to `unmodifiableMap`, e.g.
   `public static <T> List<T> synchronizedList(List<T> list){return SynchronizedViews.list(list);}` where
   `SynchronizedViews.list` does `requireNonNull` and picks `RandomListView` or `ListView`.
3. Register the new file: in `BytecodeExceptionRuntime.java`, on the `List.of(...)` line that contains
   `"UnmodifiableViews"`, insert `"SynchronizedViews",` right after `"UnmodifiableViews",`.
4. `javaw-agent-kit/bin/kit check T09` until PASS.
5. Write `docs/bytecode-synchronized-collections.md` (skeleton below).

## Hints

- Do **not** extend `AbstractCollection`/`AbstractMap`: implement the interfaces directly and forward each
  method. Then nothing (equals, toString, removeIf, defaults) silently uses a different algorithm.
- Never write the keyword `synchronized`, not even as a word in a `/* ... */` or Javadoc comment: guard
  only strips `//` comments and string literals. Names such as `synchronizedList` and `SynchronizedViews`
  are fine (guard matches the whole word only).
- Generic signatures: keep the backing field typed exactly (`Collection<E> c`, not `? extends E`) so `add`
  and `put` compile without casts.
- `ListView` needs its own `equals`/`hashCode` (delegating to `list`), and `MapView.keySet()` must wrap
  `m.keySet()` in a `SetView` (not a `CollectionView`) so `keySet().equals(Set.of())` works.
- Do not cache or copy anything: every method is one `return c.x(...)` line. The reference is about 100 lines.
- Runtime interfaces are small; list them before writing (`grep -o "default [^{]*" java/util/Map.java`).
  `Collection` has no `parallelStream` here, so do not override it.

## Docs

Create `docs/bytecode-synchronized-collections.md` with this skeleton (fill in the brackets):

    # Bytecode synchronized collection wrappers

    Revision `synchronized-collections-vN` links `Collections.synchronizedCollection/Set/List/Map` as
    monitor-free delegating views (single-threaded contract) in the bundled exception runtime.

    Contract:

    - <delegation, write-through, equality rules, RandomAccess, map views, null backing>

    Evidence: `BytecodeSynchronizedCollectionsTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
    orders, two fresh instances). The sorted and navigable variants are rejected before WAT generation.
