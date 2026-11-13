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
