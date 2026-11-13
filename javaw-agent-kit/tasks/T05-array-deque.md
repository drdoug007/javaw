# T05 ArrayDeque

```kit
id: T05
slug: array-deque
title: link java.util.ArrayDeque
tests: BytecodeArrayDequeTest
depends: T04
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/ArrayDeque.java
allow: docs/bytecode-array-deque.md
```

## Goal

Add `java.util.ArrayDeque<E>`, a resizable ring-buffer `Deque` that rejects `null`. H2 uses it in 53 audit rows
(`<init>`, `push`, `pop`, `poll`, `add`, `isEmpty`, `peekLast`, ...). `Queue` and `Deque` exist since T04.

## Members to implement

    public class ArrayDeque<E> extends AbstractCollection<E> implements Deque<E>, Cloneable, java.io.Serializable

- Constructors: `ArrayDeque()`, `ArrayDeque(int numElements)`, `ArrayDeque(Collection<? extends E> c)`.
- Deque/Queue: `addFirst`, `addLast`, `offerFirst`, `offerLast`, `removeFirst`, `removeLast`, `pollFirst`,
  `pollLast`, `getFirst`, `getLast`, `peekFirst`, `peekLast`, `removeFirstOccurrence(Object)`,
  `removeLastOccurrence(Object)`, `add`, `offer`, `remove()`, `poll`, `element`, `peek`, `push`, `pop`,
  `addAll(Collection<? extends E>)`, `iterator()`, `descendingIterator()`.
- Collection: `size`, `isEmpty`, `contains(Object)`, `remove(Object)`, `clear`, `toArray()`.
- `public ArrayDeque<E> clone()` — the return type must be `ArrayDeque<E>` (the test calls
  `ArrayDeque.clone()Ljava/util/ArrayDeque;`).
- Inherited from AbstractCollection, do NOT write them: `toArray(T[])`, `containsAll`, `removeAll`, `retainAll`,
  `toString`. `removeIf` and `forEach` come from Collection/Iterable defaults.

## Members to leave out

- `reversed()` (JDK 21). The test checks `ArrayDeque.reversed()` is rejected.
- `spliterator()`, `forEach`, `removeIf` overrides, serialization hooks. Do not override `equals`/`hashCode`
  (ArrayDeque uses identity, like the JDK).

## JDK behaviour to match

- `null` elements: `addFirst`, `addLast`, `offerFirst`, `offerLast`, `push`, `add`, `offer` throw
  `NullPointerException`. `new ArrayDeque<>((Collection) null)` and `addAll(null)` throw NPE.
- `addAll(c)` and the `Collection` constructor add the elements one at a time with `addLast`, in iteration order.
  If an element is null, the elements before it stay added, then NPE. `addAll` returns true if the size grew.
- `ArrayDeque(int n)`: any `n` is accepted (0 and negative too); it is only a capacity hint.
- Empty deque: `getFirst`, `getLast`, `removeFirst`, `removeLast`, `pop`, `element`, `remove()` throw
  `NoSuchElementException`; `peek*`, `poll*`, `peek`, `poll` return null.
- `contains(null)`, `remove(null)`, `removeFirstOccurrence(null)`, `removeLastOccurrence(null)` return false (no
  exception). Matching uses `o.equals(element)`. `remove(Object)` = `removeFirstOccurrence`.
- `push` = `addFirst`, `pop` = `removeFirst`; `add`/`offer` = add at the tail; `poll`/`peek`/`element`/`remove()`
  work on the head.
- Iterators (both): `next()` past the end throws `NoSuchElementException`; `remove()` before `next()` or twice in a
  row throws `IllegalStateException`. `remove()` deletes the last returned element and iteration continues with the
  element that followed it (descending: that preceded it). Do not throw `ConcurrentModificationException`; tests do
  not check it.
- `clone()` is a shallow copy with its own array (changing one does not change the other) and keeps the runtime
  class of subclasses: a `class Named extends ArrayDeque<String>` clones to a `Named`.
- `toString()` (inherited) prints head to tail: `[a, b, c]`.

## Steps

1. Create `javaw-bytecode/src/main/resources/exception-runtime/java/util/ArrayDeque.java`.
2. Register it: in `javaw-bytecode/src/main/java/one/dastec/javaw/bytecode/BytecodeExceptionRuntime.java`, in the
   `List.of("Iterator","ListIterator","Collection","List","Queue","Deque",...)` line, insert `"ArrayDeque",` right
   after `"LinkedList",`.
3. `javaw-agent-kit/bin/kit check T05` until PASS.
4. Write `docs/bytecode-array-deque.md` (skeleton below).

## Hints

- Structure: fields `Object[] elements; int head, size;`. Logical position `i` (0 = head) lives in slot
  `(head + i) % elements.length`; write a helper `slot(i)` that subtracts `elements.length` instead of using `%`.
  `addFirst` moves head back one slot (wrap from 0 to `length-1`); `addLast` writes `slot(size)`.
- Grow when `size == elements.length`: allocate a bigger array, copy the elements in logical order to slots
  `0..size-1`, set `head = 0`. Default capacity 16; `ArrayDeque(n)` allocates `n < 1 ? 1 : n + 1` slots (never
  0, or `addFirst` breaks). Doubling (`length * 2 + 2`) is fine; the capacity is never observable.
- Write a private `delete(int i)` that shifts logical positions `i+1..size-1` one place toward the head and nulls
  the old last slot. Use it for `removeFirstOccurrence`, `removeLastOccurrence` and both iterators' `remove()`.
- Iterators as anonymous classes holding logical indexes: ascending `next` starts at 0, `last = -1`; after
  `remove()` set `next = last` (the following element moved into `last`). Descending `next` starts at `size-1` and
  needs no adjustment after `delete`.
- `clone()`: copy LinkedList's pattern: `(ArrayDeque<E>) super.clone()`, then give the copy
  `Arrays.copyOf(elements, elements.length)`. Catch `CloneNotSupportedException` and throw `new InternalError()`.
  `AssertionError` does NOT exist in the runtime.
- Null out slots you vacate (poll, delete, clear) so stale values never come back after a wrap.
- Before ArrayDeque exists the failure reads `javaw/runtime/ObjectReflection.class ... unsupported method handle
  java/lang/Object.getClass()...; link a complete acyclic type hierarchy: java/util/ArrayDeque`. Only the last
  part matters (ArrayDeque missing or unregistered); the ObjectReflection text disappears once ArrayDeque links.
- `addAll` may simply loop `for (E value : values) addLast(value);` and return `size > before`; that gives the
  JDK partial-add-then-NPE behaviour and NPE for a null collection.
- `getSimpleName()` of the copy is checked; `super.clone()` keeps the class, `new ArrayDeque<>(this)` does not.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
constructorsAndEnds, nullsAreRejected, queueAndStackViews, ringBufferWrapsAndGrows, iterators, copiesAndCollectionMembers: only if the message says the compiler rejected the fixture, read its LAST part. `link a complete acyclic type hierarchy: java/util/ArrayDeque` means ArrayDeque.java is missing or not registered: insert `"ArrayDeque",` right after `"LinkedList",` in BytecodeExceptionRuntime.java (do not edit that file by hand: run `javaw-agent-kit/bin/kit register <path under exception-runtime>` for each new file, which makes exactly this edit), and ignore the ObjectReflection text before it. `java.util.ArrayDeque.clone()Ljava/util/ArrayDeque;: unsupported exception handler frame ... ATHROW requires ... Throwable` means clone() throws AssertionError, which the runtime does not have: catch CloneNotSupportedException and throw `new InternalError()`. `Fixture.cloneCopy()...: unsupported dynamic call java/util/ArrayDeque.clone()Ljava/util/ArrayDeque;` means you declared `Object clone()`: declare `public ArrayDeque<E> clone()`. Any other `unsupported ... call java/util/ArrayDeque.X(...)` means member X is missing: add it with exactly that signature.
constructors, addEnds, stack, wrapAround, iteration, cloneCopy: if Fixture.addEnds(), stack(), wrapAround(), iteration() or cloneCopy() threw an uncaught exception in Wasm, addFirst/push probably made head negative, because Java's `%` keeps the sign. In addFirst write `head = head == 0 ? elements.length - 1 : head - 1;`, never `(head - 1) % elements.length`. Also ArrayDeque(int n) must allocate `n < 1 ? 1 : n + 1` slots, so 0 and -5 still work. Expected constructors: `[],0,true,[z],[n, m],[x, y, z],3`.
wrapAround, growWhileWrapped, manyElements, occurrences, cloneCopy: a different value with the wrong order or `null` entries after the buffer wraps or grows (wrapAround should be `[a, b, c, d, e],5,a,e,abcde,edcba`; growWhileWrapped printing `[f36, f30, f21, null, null, ...]`) means grow() or slot() is wrong. slot(i) is `head + i`, minus elements.length if that is >= elements.length. grow() must copy in logical order, `for (i = 0; i < size; i++) bigger[i] = elements[slot(i)];`, then set `head = 0`. A plain Arrays.copyOf or System.arraycopy of the old array is wrong when the buffer has wrapped.
constructorFailures, addAllWithNull: expected `NullPointerException,NullPointerException` and `NullPointerException,[x, a],...`. addAll and the Collection constructor must add one element at a time with addLast, which throws NPE for null, so the elements before the null stay added. Do not pre-scan for nulls or copy toArray(). Use `int before = size; for (E value : values) addLast(value); return size > before;` and let the constructor call addAll(values).
nullQueries: contains(null), remove(null), removeFirstOccurrence(null) and removeLastOccurrence(null) must return false without throwing (expected `false,false,false,false,[a]`). Start each with `if (o == null) return false;` and match with `o.equals(element)`.
iteratorRemove, iteratorRemoveAll: the ascending iterator holds logical indexes (next = 0, last = -1). remove() throws IllegalStateException when last < 0; otherwise `delete(last); next = last; last = -1;`, because the following element moved into position last. Expected iteratorRemove: `IllegalStateException,abcde,[a, c],IllegalStateException,2,c`. Getting `abd,[a, c, e]` means the `next = last` step is missing.
descending: expected `IllegalStateException,edcba,[b, c, e],NoSuchElementException,IllegalStateException,ecb`. The descending iterator starts at `next = size - 1` and walks down with `last = next--`. Its remove() is `delete(last); last = -1;` and must NOT change next: delete only shifts the later positions toward the head. Getting `IllegalStateException,edecbab,...` means you copied `next = last` from the ascending iterator; remove it there. next() throws NoSuchElementException when next < 0.
cloneCopy: expected `[new, a, b, c, d, e],[a, b, c, d],true,ArrayDeque,Named,[n]`. clone() must do `ArrayDeque<E> copy = (ArrayDeque<E>) super.clone();` so a subclass stays Named, then `copy.elements = Arrays.copyOf(elements, elements.length);` so the two deques do not share one array. `new ArrayDeque<>(this)` gives the wrong class.
```

## Docs

Create `docs/bytecode-array-deque.md`:

    # Bytecode ArrayDeque

    Revision `array-deque-vN` links `java.util.ArrayDeque` (a resizable ring buffer implementing `Deque`) in the
    bundled exception runtime.

    Contract:

    - JDK null rules (null elements rejected with NPE; null queries return false), NoSuchElementException on empty
      ends, iterator remove/next state failures, clone keeps subclasses. Iterators are not fail-fast.
    - `reversed()`, spliterator and serialization are not linked.

    Evidence: `BytecodeArrayDequeTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
    fresh instances), including wrapped and growing buffers. `ArrayDeque.reversed()` is rejected before WAT
    generation.
