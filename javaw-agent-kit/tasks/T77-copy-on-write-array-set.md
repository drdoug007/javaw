# T77 CopyOnWriteArraySet

```kit
id: T77
slug: copy-on-write-array-set
title: link java.util.concurrent.CopyOnWriteArraySet
tests: BytecodeCopyOnWriteArraySetTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/CopyOnWriteArraySet.java
allow: docs/bytecode-copy-on-write-array-set.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-copy-on-write-array-set-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeCopyOnWriteArraySetTest.java`.

## Goal

Add `java.util.concurrent.CopyOnWriteArraySet` to the bundled runtime as a thin wrapper over the already linked
`java.util.concurrent.CopyOnWriteArrayList` (see `docs/bytecode-copy-on-write-array-list.md`).
`runtime-target/target/audit/member-references.tsv` lists the construction sites (opcode 183, `<init>()V`): Tomcat
`org/apache/coyote/AbstractProcessorLight.<init>()V`, Spring beans `MethodOverrides.<init>()V` (2) and Spring core
`GenericConversionService$Converters.<init>()V`. Those libraries then use the set through the `Set`/`Collection`
interfaces (`add`, `addAll`, `remove`, `contains`, `iterator`, `isEmpty`, `equals`, `hashCode`), so the whole
public JDK 21 surface below is implemented, copying the JDK's delegation exactly. Iteration is insertion order,
so everything is deterministic.

## Members to implement

`javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/CopyOnWriteArraySet.java`
(`package java.util.concurrent;`):
`public class CopyOnWriteArraySet<E> extends AbstractSet<E> implements java.io.Serializable` with

- field `private final CopyOnWriteArrayList<E> al;`
- `public CopyOnWriteArraySet()`
- `public CopyOnWriteArraySet(Collection<? extends E> c)`
- `public int size()`
- `public boolean isEmpty()`
- `public boolean contains(Object o)`
- `public Object[] toArray()`
- `public <T> T[] toArray(T[] a)`
- `public void clear()`
- `public boolean remove(Object o)`
- `public boolean add(E e)`
- `public boolean containsAll(Collection<?> c)`
- `private static int compareSets(Object[] snapshot, Set<?> set)`
- `public boolean addAll(Collection<? extends E> c)`
- `public boolean removeAll(Collection<?> c)`
- `public boolean retainAll(Collection<?> c)`
- `public Iterator<E> iterator()`
- `public boolean equals(Object o)`
- `public boolean removeIf(Predicate<? super E> filter)`
- `public void forEach(Consumer<? super E> action)`

Inherited and correct as they are (do not override): `hashCode()` (AbstractSet: sum of element hash codes, `0`
for `null`, via `iterator()`) and `toString()` (AbstractCollection: `[a, b]` via `iterator()`).

## Members to leave out

- `spliterator()`: the inherited `Set.spliterator()` default is used; do not override it.
- Serialization methods: there are none in the JDK class (the `al` field is serialized); add none.
- Do not edit `CopyOnWriteArrayList.java`, `AbstractSet.java`, `AbstractCollection.java` or any other runtime file
  (`kit guard` rejects it). Everything needed already exists there, including the package-private
  `final Object[] getArray()`, `addIfAbsent(E)` and `addAllAbsent(Collection)`.

## JDK behaviour to match

Every method delegates to `al`; `null` elements are allowed (one at most, like any other value).

- `()`: `al = new CopyOnWriteArrayList<E>();`. `(Collection c)`: if `c.getClass() == CopyOnWriteArraySet.class`
  (exact class, not `instanceof`), `al = new CopyOnWriteArrayList<E>(((CopyOnWriteArraySet<E>) c).al)`; otherwise
  `al = new CopyOnWriteArrayList<E>(); al.addAllAbsent(c);`. So duplicates collapse to their first occurrence in
  the collection's order, `null` collection throws `NullPointerException`, the source's `toArray()` is called
  (not `iterator()`), and `this.add` is never called (the `Spy` subclass logs it). The copy is independent.
- `size`, `isEmpty`, `contains`, `toArray()`, `toArray(a)`, `clear`, `remove(o)`, `iterator`, `removeIf`,
  `forEach`, `removeAll`, `retainAll`: `return al.same(...)`. They must not call the overridable methods of
  `this` (e.g. `isEmpty` must not call `size()`, which `AbstractCollection.isEmpty` would).
- `add(e)`: `return al.addIfAbsent(e);`. `addAll(c)`: `return al.addAllAbsent(c) > 0;` (also deduplicates inside
  `c`; `addAll(this)` returns `false`; never calls `this.add`).
- `iterator()` is the list's snapshot iterator: it sees the elements at the time it was created, later changes do
  not show, `remove()` throws `UnsupportedOperationException` (message `null`), `next()` past the end throws
  `NoSuchElementException`.
- `containsAll(c)`: `return (c instanceof Set) ? compareSets(al.getArray(), (Set<?>) c) >= 0 : al.containsAll(c);`
  The non-Set path uses the list's `o.equals(element)` with **no** `==` shortcut; the Set path uses
  `Objects.equals(x, snapshot[i])` (`==` first). The test's `Key("never")` equals nothing, so the two paths differ.
- `compareSets(snapshot, set)` (copy exactly; the test logs every `equals` call and its order):

      final int len = snapshot.length;
      final boolean[] matched = new boolean[len];
      int j = 0;
      outer: for (Object x : set) {
          for (int i = j; i < len; i++) {
              if (!matched[i] && Objects.equals(x, snapshot[i])) {
                  matched[i] = true;
                  if (i == j)
                      do { j++; } while (j < len && matched[j]);
                  continue outer;
              }
          }
          return -1;
      }
      return (j == len) ? 0 : 1;

  It returns `-1` when some element of `set` is not in the snapshot, `0` when both hold the same elements, `1`
  when the snapshot has extra elements. It never calls `set.size()`.
- `equals(o)`: `return (o == this) || ((o instanceof Set) && compareSets(al.getArray(), (Set<?>) o) == 0);`.
  Not the `AbstractSet` algorithm (no size check, no `containsAll`); a `List` with the same elements is not equal.

## Steps

1. Read the test, then `java/util/concurrent/CopyOnWriteArrayList.java`, `java/util/AbstractSet.java` and
   `java/util/AbstractCollection.java` under `javaw-bytecode/src/main/resources/exception-runtime/`.
2. Create `java/util/concurrent/CopyOnWriteArraySet.java` there with the members above.
3. Register it (do not edit `BytecodeExceptionRuntime.java` by hand):
   `javaw-agent-kit/bin/kit register java/util/concurrent/CopyOnWriteArraySet.java`
4. `javaw-agent-kit/bin/kit check T77` until PASS.
5. Write `docs/bytecode-copy-on-write-array-set.md` (below), the report, then `javaw-agent-kit/bin/kit finish T77`.

## Hints

- Imports: `java.util.AbstractSet`, `Collection`, `Iterator`, `Objects`, `Set`, `java.util.function.Consumer`,
  `java.util.function.Predicate`. `CopyOnWriteArrayList` is in the same package (no import), so `al.getArray()`
  is accessible.
- The `(Collection)` constructor casts to `CopyOnWriteArraySet<E>` and reads `cc.al`; it needs
  `@SuppressWarnings("unchecked")` or a raw cast, not a call to any overridable method.
- Easy mistakes: inheriting `isEmpty`/`containsAll`/`addAll`/`equals` from `AbstractSet`/`AbstractCollection`
  (the `Spy` log shows extra `size:`, `contains:`, `add:` or `iterator` calls, and the `Key` log changes);
  implementing `add` as `contains` + `al.add`; using `instanceof` instead of the exact class check in the
  constructor; adding a `==` check or a size check to `containsAll`/`equals`.
- The Wasm runner stops at the first failing fixture of each `@Test` method; after a fix another fixture may fail next.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
basics, constructorsAndAddAll, snapshotIterators, equalsAndContainsAll, bulkOperationsArraysAndSubclasses: if FIX THE RUNTIME says `link the symbolic class/interface java/util/concurrent/CopyOnWriteArraySet`, `link a complete acyclic type hierarchy` or names one of its methods, the class is missing or not registered: run `javaw-agent-kit/bin/kit register java/util/concurrent/CopyOnWriteArraySet.java` (never edit BytecodeExceptionRuntime.java by hand) and add every member listed under Members to implement. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
basics: add is al.addIfAbsent (null allowed once), remove/contains/clear delegate to al; hashCode and toString are inherited from AbstractSet/AbstractCollection.
constructorsAndAddAll: the collection constructor copies `cc.al` only when c.getClass() == CopyOnWriteArraySet.class, else calls al.addAllAbsent(c) (calls c.toArray(), keeps first occurrences); addAll returns al.addAllAbsent(c) > 0.
snapshotIterators: iterator() and forEach delegate to al: a snapshot, and iterator remove() throws UnsupportedOperationException.
equalsAndContainsAll: copy compareSets exactly (Objects.equals(x, snapshot[i]) with x from the argument set, matched[] and j); containsAll uses it only for a Set argument and al.containsAll otherwise; equals is o == this || (o instanceof Set && compareSets(...) == 0), with no size check.
bulkOperationsArraysAndSubclasses: removeAll/retainAll/removeIf/toArray delegate to al; isEmpty, addAll, containsAll, equals, removeAll, retainAll and the constructor must not call size/add/contains/remove/iterator/toArray on this.
```

## Docs

Create `docs/bytecode-copy-on-write-array-set.md` with this skeleton (fill in the brackets):

    # Bytecode CopyOnWriteArraySet

    Revision `copy-on-write-array-set-vN` links `java.util.concurrent.CopyOnWriteArraySet` in the bundled
    exception runtime as a wrapper over the linked `CopyOnWriteArrayList`.

    Contract:

    - <delegation to the list (addIfAbsent/addAllAbsent); constructor copy rule; snapshot iterator; containsAll and
      equals through compareSets; inherited hashCode/toString; no self-calls through overridable methods>

    Known differences: none observable in the single-threaded deployment; `spliterator()` is the inherited `Set`
    default instead of an array spliterator.

    Evidence: `BytecodeCopyOnWriteArraySetTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
    orders, two fresh instances).
