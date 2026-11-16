# T41 CopyOnWriteArrayList

```kit
id: T41
slug: copy-on-write-array-list
title: link java.util.concurrent.CopyOnWriteArrayList
tests: BytecodeCopyOnWriteArrayListTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/CopyOnWriteArrayList.java
allow: docs/bytecode-copy-on-write-array-list.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-copy-on-write-array-list-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeCopyOnWriteArrayListTest.java`.

## Goal

Add `java.util.concurrent.CopyOnWriteArrayList` to the bundled runtime. The pinned libraries name it from 37 classes
(`runtime-target/target/audit/external-types.tsv`: logback `Logger`, `ContextBase`, `COWArrayList`, `TurboFilterList`,
HikariCP `ConcurrentBag`, H2 `Table` / `TableView` / `DropTable`, Spring `AbstractBeanFactory` (and its subclass
`AbstractBeanFactory$BeanPostProcessorCacheAwareList`), `MutablePropertySources`, Tomcat `StandardContext`,
`ContainerBase`, `GenericUser`, Log4j `StatusLogger`, Micrometer, SLF4J `BasicMarker`, ...). The member rows in
`member-references.tsv` are `<init>()` 42 call sites, `remove(Object)` 9, `iterator` 9, `isEmpty` 7, `clear` 5,
`addIfAbsent` 5, `addAll(Collection)` 5, `add(E)` 5, `contains` 4, `size` 3, `remove(int)` 3, `toArray(T[])` 2,
`set` 2, `retainAll` 2, `removeAll` 2, `addAll(int,Collection)` 2, `add(int,E)` 2, and one each of `subList`,
`stream`, `replaceAll`, `removeIf`, `listIterator()`, `listIterator(int)`, `lastIndexOf`, `indexOf`, `forEach`,
`containsAll` and `clone`. The task links the whole Java 17 public API except serialization.

The runtime is single-threaded, so there is no lock: each write builds a **new** array and stores it in the field
`array`; reads take the current array once. Everything observable comes from this: iterators are snapshots, a write
that changes nothing keeps the same array, and a sub list notices any other write because the array it remembered is
no longer the current one.

## Members to implement

One file `javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/CopyOnWriteArrayList.java`
(`package java.util.concurrent;`), with imports from `java.util` (`AbstractList`, `ArrayList`, `Arrays`, `Collection`,
`Comparator`, `ConcurrentModificationException`, `Iterator`, `List`, `ListIterator`, `NoSuchElementException`,
`Objects`, `RandomAccess`) and `java.util.function` (`Consumer`, `Predicate`, `UnaryOperator`):

`public class CopyOnWriteArrayList<E> implements List<E>, RandomAccess, Cloneable, java.io.Serializable` with

- field `private Object[] array;` (never `volatile`) and `final Object[] getArray()`, `final void setArray(Object[] a)`
- constructors `public CopyOnWriteArrayList()`, `public CopyOnWriteArrayList(Collection<? extends E> c)`,
  `public CopyOnWriteArrayList(E[] toCopyIn)`
- `public int size()`, `public boolean isEmpty()`, `public boolean contains(Object o)`
- `public int indexOf(Object o)`, `public int indexOf(E e, int index)`, `public int lastIndexOf(Object o)`,
  `public int lastIndexOf(E e, int index)`
- `public Object clone()`, `public Object[] toArray()`, `public <T> T[] toArray(T[] a)`
- `public E get(int index)`, `public E set(int index, E element)`, `public boolean add(E e)`,
  `public void add(int index, E element)`, `public E remove(int index)`, `public boolean remove(Object o)`
- `public boolean addIfAbsent(E e)`, `public int addAllAbsent(Collection<? extends E> c)`
- `public boolean containsAll(Collection<?> c)`, `public boolean removeAll(Collection<?> c)`,
  `public boolean retainAll(Collection<?> c)`, `public void clear()`, `public boolean addAll(Collection<? extends E> c)`,
  `public boolean addAll(int index, Collection<? extends E> c)`
- `public void forEach(Consumer<? super E> action)`, `public boolean removeIf(Predicate<? super E> filter)`,
  `public void replaceAll(UnaryOperator<E> operator)`, `public void sort(Comparator<? super E> c)`
- `public String toString()`, `public boolean equals(Object o)`, `public int hashCode()`
- `public Iterator<E> iterator()`, `public ListIterator<E> listIterator()`, `public ListIterator<E> listIterator(int index)`
- `public List<E> subList(int fromIndex, int toIndex)`
- package-private helpers used by the sub list: `void removeRange(int fromIndex, int toIndex)`,
  `boolean bulkRemove(Predicate<? super E> filter, int i, int end)`,
  `void replaceAllRange(UnaryOperator<E> operator, int i, int end)`, `void sortRange(Comparator<? super E> c, int i, int end)`
- private static helpers: `indexOfRange(Object o, Object[] es, int from, int to)`,
  `lastIndexOfRange(Object o, Object[] es, int from, int to)`, `static String outOfBounds(int index, int size)`
- nested `static final class COWIterator<E> implements ListIterator<E>` (fields `private final Object[] snapshot;
  private int cursor;`, constructor `COWIterator(Object[] es, int initialCursor)`)
- inner (non-static) `private class COWSubList extends AbstractList<E> implements RandomAccess` (see below)

`stream()` and `spliterator()` are inherited from the `Collection` / `List` defaults: do not override them.

## Members to leave out

- Serialization: `writeObject`, `readObject`, `serialVersionUID` and the `lock` field.
- The Java 21 `SequencedCollection` members `getFirst`, `getLast`, `addFirst`, `addLast`, `removeFirst`, `removeLast`
  and `reversed` (and the JDK's `Reversed` class): Java 17 code cannot call them. The test checks that
  `CopyOnWriteArrayList.getFirst()` stays unlinked.
- Do not edit any other runtime file (`kit guard` rejects it).

Known difference: the JDK `COWSubList` implements `List` directly; here it extends the runtime `AbstractList` and
inherits `toString`, `equals`, `hashCode`, `indexOf`, `lastIndexOf`, `contains`, `containsAll`, `isEmpty` and `toArray`
(all go through `size()` or `iterator()`, so they check for modification like the JDK). The fixtures do not observe the
difference.

## JDK behaviour to match

Helpers (copy them):

- `outOfBounds(index, size)` returns `"Index: " + index + ", Size: " + size`.
- `indexOfRange(o, es, from, to)`: if `o == null` the first `i` in `from..to-1` with `es[i] == null`, otherwise the first
  with `o.equals(es[i])` (always `o.equals(element)`, never `element.equals(o)`: the fixture class `Any` equals
  everything, so `indexOf(new Any())` is 0 and `remove(new Any())` removes the first element); `-1` if none.
  `lastIndexOfRange` is the same loop from `to - 1` down to `from`. Neither checks its bounds: an index outside the
  array throws the `ArrayIndexOutOfBoundsException` of the array access.

Constructors and reads:

- `()`: `setArray(new Object[0])`.
- `(Collection c)`: if `c.getClass() == CopyOnWriteArrayList.class`, take `((CopyOnWriteArrayList<?>) c).getArray()`;
  otherwise `es = c.toArray()` and, if `c.getClass() != ArrayList.class`, `es = Arrays.copyOf(es, es.length,
  Object[].class)`. `null` -> `NullPointerException` (from `getClass()`). The copy matters: the fixture's `Spy`
  collection returns its own `String[]` from `toArray()`, later changes it, and then stores an `Integer` in the list.
  Without the copy the list shows the change and `set(0, 1)` throws `ArrayStoreException`.
- `(E[] toCopyIn)`: `setArray(Arrays.copyOf(toCopyIn, toCopyIn.length, Object[].class))` (`null` -> NPE).
- `size()` is `getArray().length`; `isEmpty()` is `size() == 0` and `contains(o)` is `indexOf(o) >= 0` (these two call
  the overridable methods: the test's subclass logs `size` and `indexOf`). Every other method reads `getArray()`
  directly and calls no other overridable method of the list.
- `indexOf(o)` / `lastIndexOf(o)`: the helpers over the whole array. `indexOf(e, index)` is
  `indexOfRange(e, es, index, es.length)` (`indexOf("x", 99)` is -1, `indexOf("x", -1)` throws
  `ArrayIndexOutOfBoundsException: Index -1 out of bounds for length 5`). `lastIndexOf(e, index)` is
  `lastIndexOfRange(e, es, 0, index + 1)` (`lastIndexOf("x", -1)` is -1, `lastIndexOf("x", 5)` on 5 elements throws
  `Index 5 out of bounds for length 5`).
- `get(index)` is `(E) getArray()[index]`: a bad index throws `ArrayIndexOutOfBoundsException` with the array message
  (`Index 4 out of bounds for length 4`, `Index -1 out of bounds for length 4`, `Index 0 out of bounds for length 0`).
- `toArray()` is `getArray().clone()`. `toArray(a)`: if `a.length < len` return
  `(T[]) Arrays.copyOf(es, len, a.getClass())` (a `String[]` for `new String[1]`); else copy into `a`, set
  `a[len] = null` if `a` is longer, and return `a` itself. `toArray(null)` -> NPE.
- `clone()`: `return super.clone();` in `try` with `catch (CloneNotSupportedException e) { throw new InternalError(); }`.
  The copy shares the array, which is safe because neither list ever writes into an array.
- `toString()` is `Arrays.toString(getArray())` (`[a, null, c]`, `[]`). `hashCode()` is the `List` hash
  (`h = 31 * h + (x == null ? 0 : x.hashCode())` from `h = 1`). `equals(o)`: `o == this` -> true; not a `List` -> false;
  otherwise walk `getArray()` against `((List<?>) o).iterator()` with `Objects.equals` and require both to end together.

Writes. Each takes `Object[] es = getArray()` once, builds a new array and calls `setArray` once at the end:

- `set(index, element)`: `E old = (E) es[index]` (bad index -> the array exception, before anything else); **only if
  `old != element`** clone `es` and store `element`; `setArray(es)`; return `old`. Setting the identical element keeps the
  same array, so open sub lists stay valid (`setSame:2` in `copyOnWriteIdentity`).
- `add(e)`: `Arrays.copyOf(es, len + 1)` plus `e`; returns `true`.
- `add(index, element)`: `if (index > len || index < 0) throw new IndexOutOfBoundsException(outOfBounds(index, len))`
  (`Index: 7, Size: 6`, `Index: -1, Size: 6`, `Index: 1, Size: 0`), then a new array of `len + 1` with the element at
  `index`.
- `remove(index)`: `E old = (E) es[index]` first (so `remove(4)` on 4 elements throws
  `ArrayIndexOutOfBoundsException: Index 4 out of bounds for length 4`, `remove(0)` on an empty list
  `Index 0 out of bounds for length 0`), then a new array of `len - 1` without it.
- `remove(o)`: `int index = indexOfRange(o, es, 0, es.length)`; `-1` -> return `false` (same array); otherwise the new
  array without `index`, `true`. Do not call `remove(int)` or `indexOf`.
- `addIfAbsent(e)`: if `indexOfRange(e, es, 0, len) >= 0` return `false` (same array), otherwise append, `true`.
- `addAllAbsent(c)`: `Object[] cs = c.toArray(); if (c.getClass() != ArrayList.class) cs = cs.clone();` (the `Spy`'s own
  array must not be changed); `if (cs.length == 0) return 0;` then keep, in order, each `cs[i]` that is in neither `es`
  nor the kept prefix (`indexOfRange(e, es, 0, len) < 0 && indexOfRange(e, cs, 0, added) < 0`, compacting into
  `cs[added++]`); if `added > 0` the new array is `Arrays.copyOf(es, len + added)` with the kept ones copied after
  `len`. Returns `added` (an `int`). `null` -> NPE.
- `addAll(c)`: `Object[] cs = c.getClass() == CopyOnWriteArrayList.class ? ((CopyOnWriteArrayList<?>) c).getArray()
  : c.toArray();` `if (cs.length == 0) return false;` (same array). If `len == 0` and `c` is exactly a
  `CopyOnWriteArrayList` or an `ArrayList`, the new array is `cs` itself; otherwise `Arrays.copyOf(es, len + cs.length)`
  with `cs` copied after `len` (so the `Spy`'s `String[]` is never stored). Returns `true`. `addAll(this)` doubles the
  list.
- `addAll(index, c)`: `Object[] cs = c.toArray();` **first** (`addAll(99, null)` -> NPE), then the `add(index, ...)`
  bounds check with the same message (`addAll(9, List.of())` on 6 elements -> `Index: 9, Size: 6`), then
  `if (cs.length == 0) return false;`, then insert.
- `clear()`: `setArray(new Object[0])`, always (a new array even when the list is already empty).
- `removeRange(from, to)`: `if (from < 0 || to > len || to < from) throw new IndexOutOfBoundsException();` then the
  new array without `from..to-1`.
- `containsAll(c)`: `for (Object e : c) if (indexOfRange(e, es, 0, len) < 0) return false; return true;` (iterates `c`;
  `null` -> NPE, also on an empty list).
- `removeAll(c)`: `Objects.requireNonNull(c); return bulkRemove(e -> c.contains(e), 0, getArray().length);`
  `retainAll(c)` the same with `!c.contains(e)`. `removeIf(filter)`: `Objects.requireNonNull(filter)` then the same
  `bulkRemove(filter, 0, getArray().length)`. All three throw NPE for `null` even on an empty list.
- `bulkRemove(filter, i, end)` (copy the JDK logic; a `boolean[]` instead of the JDK's `long[]` bit set is fine):
  `es = getArray()`; call `filter.test` exactly once per element of `es[i..end-1]`, in order; then
  `if (es != getArray()) throw new ConcurrentModificationException();` (no message; the check runs whether or not
  something matched, so a filter that adds to the list throws after all its calls and the additions stay); if nothing
  matched return `false` (same array); otherwise `setArray` a new array without the matched elements (everything
  outside `i..end-1` kept) and return `true`. A filter that throws leaves the list unchanged.
- `replaceAll(op)` is `replaceAllRange(op, 0, getArray().length)`: `Objects.requireNonNull(op)` (NPE even when empty);
  `es = getArray().clone()`; `es[k] = op.apply((E) es[k])` for `k` in `i..end-1`; `setArray(es)` (always a new array).
  An operator that throws leaves the list unchanged; additions the operator makes are lost because `setArray(es)`
  replaces them (`[XX, YY, ZZ]`).
- `sort(c)` is `sortRange(c, 0, getArray().length)`: `es = getArray().clone(); Arrays.sort(es, i, end,
  (Comparator<Object>) c); setArray(es);` (`null` is natural order; the runtime sort is stable like the JDK's).
- `forEach(action)`: `Objects.requireNonNull(action)` then `for (Object x : getArray()) action.accept((E) x);`
  (a snapshot: elements the action adds are not visited).

Iterators:

- `iterator()` and `listIterator()` are `new COWIterator<E>(getArray(), 0)`. `listIterator(index)`: if
  `index < 0 || index > len` throw `new IndexOutOfBoundsException(outOfBounds(index, len))` (`Index: 4, Size: 3`).
- `COWIterator` reads only its `snapshot`: later writes to the list are never seen (`list.clear()` during iteration
  does not stop it). `hasNext()` is `cursor < snapshot.length`, `hasPrevious()` is `cursor > 0`; `next()` /
  `previous()` throw `new NoSuchElementException()` (no message) past either end; `nextIndex()` is `cursor`,
  `previousIndex()` is `cursor - 1`. `remove()`, `set(e)` and `add(e)` always throw
  `new UnsupportedOperationException()` (no message). `forEachRemaining(action)`: `Objects.requireNonNull(action)`,
  visit `snapshot[cursor..]`, leave `cursor` at the end.

`subList(from, to)`: `size = to - from`; `if (from < 0 || to > len || size < 0) throw new IndexOutOfBoundsException();`
(no message); `return new COWSubList(es, from, size);`. `COWSubList` has fields `private final int offset; private int
size; private Object[] expectedArray;` and:

- Constructor: `COWSubList(Object[] es, int offset, int size) { expectedArray = es; this.offset = offset; this.size =
  size; }`. `expectedArray` starts as the array passed in, so a write to the list after `subList` makes the sub list
  stale.
- Do not override `add(E)` in `COWSubList`; the runtime `AbstractList.add(e)` calls `add(size(), e)`, which checks for
  modification and logs `add<offset+size>` (for example `true{add3}`). `forEach` is likewise inherited and goes
  through `iterator()`.

- `checkForComodification()`: `if (getArray() != expectedArray) throw new ConcurrentModificationException();`.
  `rangeCheck(index)`: `index < 0 || index >= size` -> `new IndexOutOfBoundsException(outOfBounds(index, size))`;
  `rangeCheckForAdd(index)`: the same with `index > size`.
- The order of the two checks is observable on a stale sub list: `get`, `set` and `remove(int)` do `rangeCheck` first
  (`get(5)` -> `Index: 5, Size: 2`, `get(1)` -> `ConcurrentModificationException`); `add(int, E)` checks
  modification first; `addAll(int, c)` does `rangeCheckForAdd` first; `removeIf`, `removeAll`, `retainAll` do
  `Objects.requireNonNull` first.
- `size()`: check, return `size`. `get(i)`: `CopyOnWriteArrayList.this.get(offset + i)`. `set(i, e)`:
  `CopyOnWriteArrayList.this.set(offset + i, e)` then `expectedArray = getArray()`. `add(i, e)`:
  `CopyOnWriteArrayList.this.add(offset + i, e)`, `expectedArray = getArray()`, `size++`. `remove(int i)`:
  `CopyOnWriteArrayList.this.remove(offset + i)`, `expectedArray = getArray()`, `size--`. These call the list's own
  (overridable) methods: the test's subclass logs `get1`, `set1`, `add3`, `remove1`. Always write
  `CopyOnWriteArrayList.this.`: inside the sub list a plain `get(...)` is the sub list's own method.
- `remove(Object o)`: check; `int index = indexOf(o)` (the inherited `AbstractList.indexOf`); `-1` -> `false`; else
  `remove(index)` (the sub list's) and `true`.
- `addAll(c)` / `addAll(i, c)` (second: `rangeCheckForAdd(i)` first): check; `Object[] old = getArray();`
  `boolean m = CopyOnWriteArrayList.this.addAll(offset + size /* or offset + i */, c);`
  `size += (expectedArray = getArray()).length - old.length; return m;` (the subclass logs `addAll3`, `addAll2`).
- `clear()`: check; `removeRange(offset, offset + size); expectedArray = getArray(); size = 0;`
- `listIterator(int i)` (`iterator()` and `listIterator()` come from `AbstractList` and call it): check;
  `rangeCheckForAdd(i)`; `return new COWIterator<E>(Arrays.copyOfRange(getArray(), offset, offset + size), i);` (a
  snapshot whose `remove`/`set`/`add` throw `UnsupportedOperationException`).
- `subList(from, to)`: check; `if (from < 0 || to > size || from > to) throw new IndexOutOfBoundsException();`
  `return new COWSubList(expectedArray, from + offset, to - from);`
- `replaceAll(op)` / `sort(c)`: check; `replaceAllRange(op, offset, offset + size)` / `sortRange(c, offset, offset +
  size)`; `expectedArray = getArray();`.
- `removeIf(f)`: `Objects.requireNonNull(f)`; check; `Object[] old = getArray(); boolean m = bulkRemove(f, offset,
  offset + size); size += (expectedArray = getArray()).length - old.length; return m;`. `removeAll(c)` /
  `retainAll(c)`: `Objects.requireNonNull(c); return removeIf(e -> c.contains(e));` (or `!c.contains(e)`).

## Steps

1. Read the test, then `javaw-bytecode/src/main/resources/exception-runtime/java/util/AbstractList.java`,
   `AbstractCollection.java`, `List.java` and `ArrayList.java`.
2. Create `javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/CopyOnWriteArrayList.java` with the
   members above.
3. Register it (do not edit `BytecodeExceptionRuntime.java` by hand):
   `javaw-agent-kit/bin/kit register java/util/concurrent/CopyOnWriteArrayList.java`
4. `javaw-agent-kit/bin/kit check T41` until PASS.
5. Write `docs/bytecode-copy-on-write-array-list.md` (below), the report, then `javaw-agent-kit/bin/kit finish T41`.

## Hints

- The runtime has `Arrays.copyOf(U[], int, Class)`, `Arrays.copyOf(T[], int)`, `Arrays.copyOfRange(T[], int, int)`,
  `Arrays.sort(T[], int, int, Comparator)` (stable, `null` is natural order), `Arrays.toString(Object[])`,
  `Objects.equals`, `Objects.requireNonNull` and `InternalError()`.
- `AbstractList` here has `protected final` methods named `index(int)` and `position(int)`: do not declare methods
  with those names in `COWSubList`. Name the checks `rangeCheck` and `rangeCheckForAdd`.
- Inside `COWSubList` the names `get`, `set`, `add`, `remove`, `addAll`, `size` mean the sub list's own members;
  `getArray()`, `removeRange`, `bulkRemove`, `replaceAllRange`, `sortRange` and `outOfBounds` reach the outer list.
- Never compare arrays with `Arrays.equals` for the modification check: it is `getArray() != expectedArray` (the same
  array object), and every write that changes something must store a **new** array. A write that changes nothing
  (`set` of the identical element, `remove`/`addIfAbsent`/`removeIf`/`removeAll`/`retainAll` that find nothing,
  `addAll` of an empty collection, `addAllAbsent` that adds nothing) must keep the old array.
- The Wasm runner stops at the **first** failing fixture of each `@Test` method, so after a fix another fixture of the
  same group may fail next. That is progress.
- Calibration mutations, each caught by the fixtures named: no `Object[].class` copy in the Collection constructor
  (`constructors`); `set` always cloning, even for the identical element (`copyOnWriteIdentity`, `subListComodification`);
  `isEmpty()` reading the array instead of calling `size()` (`subclassCalls`); no `es != getArray()` check in
  `bulkRemove` (`functional`).
- Calibration: the reference passed kit check after 1 run (about 310 new lines, all in CopyOnWriteArrayList.java).

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
constructorsAndPositions, searchingAndBulk, iteratorsAndFunctional, equalityAndCopying, subListViews, leavesOutSequencedMembers: if FIX THE RUNTIME says `link the symbolic class/interface java/util/concurrent/CopyOnWriteArrayList`, `link a complete acyclic type hierarchy`, `link the descriptor class/interface java/util/concurrent/CopyOnWriteArrayList` or names a method of it, the class is missing or not registered: run `javaw-agent-kit/bin/kit register java/util/concurrent/CopyOnWriteArrayList.java` (never edit BytecodeExceptionRuntime.java by hand) and add every member listed under Members to implement with the exact signature. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
constructors, bulk: the Collection constructor must copy `c.toArray()` with `Arrays.copyOf(es, es.length, Object[].class)` unless c.getClass() is exactly ArrayList or CopyOnWriteArrayList; addAll into an empty list may reuse cs only for exactly ArrayList or CopyOnWriteArrayList, otherwise `Arrays.copyOf(es, len + cs.length)`. An ArrayStoreException or a `Z` in the output means the Spy's String[] was stored.
positional: get, set and remove(int) read `es[index]` before anything else and let the array throw (`Index 4 out of bounds for length 4`); add(int,E), addAll(int,c) and listIterator(int) throw `new IndexOutOfBoundsException("Index: " + index + ", Size: " + len)`; indexOf(e, i) is indexOfRange(e, es, i, es.length) and lastIndexOf(e, i) is lastIndexOfRange(e, es, 0, i + 1) with no bounds checks of their own.
absentAndContains: compare with `o.equals(es[i])` (argument first) or `es[i] == null` for a null argument; addAllAbsent clones c.toArray() unless c is exactly an ArrayList, drops elements already in the list or earlier in cs, and returns the count as an int; containsAll iterates c (the Spy logs `iterator`).
bulk, functional: removeAll/retainAll/removeIf call Objects.requireNonNull first (NPE even on an empty list), then bulkRemove tests each element exactly once in order and throws `new ConcurrentModificationException()` if getArray() changed meanwhile (also when nothing matched); addAll(int, c) calls c.toArray() before the index check, and the index check comes before the `cs.length == 0` return.
iterators: iterator()/listIterator() wrap the current array as a snapshot; COWIterator.remove/set/add throw `new UnsupportedOperationException()`; next/previous past an end throw `new NoSuchElementException()`; forEach walks the array it read first, so elements it adds are not visited.
functional: replaceAll and sort work on `getArray().clone()` and call setArray once at the end (a throwing operator leaves the list unchanged, additions made by the operator are lost); replaceAll(null) throws NPE even on an empty list.
equalityAndCopies: clone() is just super.clone(); toArray() returns a clone; toArray(a) returns a new array of a's type when a is too short and otherwise writes into a, setting a[len] = null when a is longer; equals walks the other list's iterator with Objects.equals.
copyOnWriteIdentity: a write that changes nothing must keep the same array object (set of the identical element, remove/addIfAbsent/removeIf/removeAll/retainAll that change nothing, addAll/addAll(int,..)/addAllAbsent of nothing); clear, add, replaceAll and sort always install a new array.
subLists, subListComodification: COWSubList compares `getArray() != expectedArray` and updates expectedArray (and size) after each of its own writes; get/set/remove(int) do rangeCheck before the modification check, add(int,E) does the modification check first, addAll(int,c) does rangeCheckForAdd first; range messages are `Index: i, Size: size`; subList bounds failures are `new IndexOutOfBoundsException()` with no message.
subclassCalls: isEmpty must call size() and contains must call indexOf(o); every other outer method reads getArray() directly; the sub list calls CopyOnWriteArrayList.this.get/set/add(int,E)/remove(int)/addAll(int,c) with `offset +` the index, and its remove(Object) uses its own indexOf then its own remove(int); the Collection constructor and addAll(c) call c.toArray() unless c.getClass() == CopyOnWriteArrayList.class.
leavesOutSequencedMembers: this fails when CopyOnWriteArrayList.getFirst() has become linked. Do not add getFirst, getLast, addFirst, addLast, removeFirst, removeLast or reversed.
```

## Docs

Create `docs/bytecode-copy-on-write-array-list.md` with this skeleton (fill in the brackets):

    # Bytecode CopyOnWriteArrayList

    Revision `copy-on-write-array-list-vN` links `java.util.concurrent.CopyOnWriteArrayList` in the bundled exception
    runtime.

    Contract:

    - <single-threaded copy-on-write array (no lock); snapshot iterators whose remove/set/add throw
      UnsupportedOperationException; addIfAbsent/addAllAbsent; the array exceptions of get/set/remove(int) and the
      `Index: i, Size: n` messages of add/addAll/listIterator; writes that change nothing keep the array; sub lists
      that throw ConcurrentModificationException after any other write; bulkRemove's reentrancy check; the
      overridable self-calls (isEmpty -> size, contains -> indexOf, sub list -> outer get/set/add/remove/addAll)>

    Known differences: no lock (the runtime is single-threaded); the sub list extends `AbstractList`; serialization
    and the Java 21 SequencedCollection members are not linked.

    Evidence: `BytecodeCopyOnWriteArrayListTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
    orders, two fresh instances). `CopyOnWriteArrayList.getFirst()` is rejected before WAT generation.
