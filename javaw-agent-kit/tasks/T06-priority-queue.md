# T06 AbstractQueue and PriorityQueue

```kit
id: T06
slug: priority-queue
title: link java.util.AbstractQueue and java.util.PriorityQueue
tests: BytecodePriorityQueueTest
depends: T04
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/AbstractQueue.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/PriorityQueue.java
allow: docs/bytecode-priority-queue.md
```

## Goal

Add `java.util.AbstractQueue` and a binary-heap `java.util.PriorityQueue`. H2 uses `PriorityQueue`
(`<init>(I,Comparator)`, `offer`, `poll`, `isEmpty`); snakeyaml subclasses `AbstractQueue` (`<init>`). `Queue`
exists since T04.

## Members to implement

`public abstract class AbstractQueue<E> extends AbstractCollection<E> implements Queue<E>`:

    protected AbstractQueue()
    public boolean add(E e)        // offer(e) ? true : throw new IllegalStateException("Queue full")
    public E remove()              // poll(), or NoSuchElementException if that returned null
    public E element()             // peek(), or NoSuchElementException if that returned null
    public void clear()            // while (poll() != null) ;
    public boolean addAll(Collection<? extends E> c)
        // c == null -> NullPointerException; c == this -> IllegalArgumentException;
        // otherwise add(e) for each element in order (stops at the first exception); true if any add returned true

`public class PriorityQueue<E> extends AbstractQueue<E> implements java.io.Serializable`:

    PriorityQueue()  PriorityQueue(int initialCapacity)  PriorityQueue(Comparator<? super E> c)
    PriorityQueue(int initialCapacity, Comparator<? super E> c)  PriorityQueue(Collection<? extends E> c)
    boolean offer(E e)  boolean add(E e)  E poll()  E peek()  boolean remove(Object o)  boolean contains(Object o)
    int size()  void clear()  Comparator<? super E> comparator()  Object[] toArray()  Iterator<E> iterator()

Inherited, do not write: `isEmpty`, `toString`, `toArray(T[])`, `containsAll`, `removeAll`, `retainAll` (from
AbstractCollection) and `remove()`, `element()`, `addAll` (from AbstractQueue).

## Members to leave out

- Constructors `PriorityQueue(SortedSet)` and `PriorityQueue(PriorityQueue)`: the test checks both are rejected.
  (The `Collection` constructor still handles those argument types, see below.)
- `spliterator`, `forEach`/`removeIf`/`removeAll`/`retainAll` overrides, serialization hooks.

## JDK behaviour to match

- `initialCapacity < 1` -> `IllegalArgumentException` (null message). Default capacity 11. A null comparator means
  natural ordering. `comparator()` returns exactly the comparator passed in (or null).
- `offer(null)`/`add(null)` -> `NullPointerException`. `contains(null)` and `remove(null)` return false.
- Natural ordering: cast the new element to `Comparable` before storing it, even when the queue is empty.
  `new PriorityQueue<Object>().offer(new Object())` throws `ClassCastException` and the size stays 0.
- `poll`/`peek` on empty return null; `remove()`/`element()` throw `NoSuchElementException` (null message).
- `remove(Object o)` removes one element `e` with `o.equals(e)` (the first in array order).
- `PriorityQueue(Collection c)`: NPE if `c` is null or contains null. If `c` is a `SortedSet`, use its
  `comparator()`; if `c` is a `PriorityQueue`, use its `comparator()`; otherwise natural ordering. Copy
  `c.toArray()` into a NEW `Object[]` (never keep the array `toArray()` returned) and heapify: `for (i = (size >>> 1) - 1; i >= 0; i--) siftDown(i, queue[i])`.
- Iteration order, `toString()` and `toArray()` show the heap array `queue[0..size-1]`. The test
  `heapOrder` checks that layout, so use exactly the JDK algorithm (write `cmp(a, b)` as
  `comparator == null ? ((Comparable) a).compareTo(b) : comparator.compare(a, b)`):
  - `offer(e)`: grow if full, then `siftUp(size, e)`, then `size++`.
  - `siftUp(k, x)`: `while (k > 0) { parent = (k - 1) >>> 1; e = queue[parent]; if (cmp(x, e) >= 0) break;
    queue[k] = e; k = parent; } queue[k] = x;`
  - `siftDown(k, x)` (uses the current `size`): `half = size >>> 1; while (k < half) { child = 2*k + 1;
    c = queue[child]; right = child + 1; if (right < size && cmp(c, queue[right]) > 0) c = queue[child = right];
    if (cmp(x, c) <= 0) break; queue[k] = c; k = child; } queue[k] = x;`
  - `poll()`: `result = queue[0]; last = --size; x = queue[last]; queue[last] = null; if (last > 0) siftDown(0, x);`
  - `removeAt(i)`: `last = --size; if (last == i) queue[i] = null; else { moved = queue[last];
    queue[last] = null; siftDown(i, moved); if (queue[i] == moved) siftUp(i, moved); }`
- Iterator: visits every element exactly once; `next()` past the end -> `NoSuchElementException`; `remove()`
  before `next()` or twice -> `IllegalStateException` (null message). Do not throw
  `ConcurrentModificationException`; tests do not check it.

## Steps

1. Create `javaw-bytecode/src/main/resources/exception-runtime/java/util/AbstractQueue.java`.
2. Create `.../java/util/PriorityQueue.java`.
3. Register both: in `javaw-bytecode/src/main/java/one/dastec/javaw/bytecode/BytecodeExceptionRuntime.java`, in
   the `List.of("Iterator","ListIterator","Collection","List","Queue","Deque",...)` line, insert
   `"AbstractQueue","PriorityQueue",` right after `"AbstractCollection",` (AbstractQueue before PriorityQueue).
4. `javaw-agent-kit/bin/kit check T06` until PASS.
5. Write `docs/bytecode-priority-queue.md` (skeleton below).

## Hints

- Fields: `Object[] queue; int size; final Comparator<? super E> comparator;`. Let every constructor call
  `this(capacity, comparator)` except the `Collection` one.
- Grow with `queue = Arrays.copyOf(queue, newLength)`; `Arrays.copyOf(Object[], int)` exists. JDK growth:
  `length < 64 ? 2 * length + 2 : length * 3 / 2` (not observable, any growth works).
- The empty-queue ClassCastException needs an explicit cast that javac keeps, e.g. at the top of `offer` when
  `comparator == null`: `Comparable<? super E> key = (Comparable<? super E>) e;` and then compare with `key`.
  Without it the failure reads `Fixture.notComparable() threw an uncaught exception in Wasm` (the bad Object is
  stored and blows up later).
- Simplest correct iterator: copy `queue[0..size-1]` into a snapshot array when the iterator is created, walk
  the snapshot, and make `remove()` delete the last returned element from the real queue by identity (`==`) with
  `removeAt`. This visits every element once even when removal moves elements. A cursor over the live array
  (`removeAt(last); next = last;`) is WRONG: `removeAt` can sift the moved element above the cursor, and
  `iteratorRemove` then shows a duplicate and a missed element.
- `removeNeedsSiftUp` fails (wrong toString after `remove(11)`) if `removeAt` forgets the `siftUp` step.
- `toArray()` = `Arrays.copyOf(queue, size)`. `clear()` nulls slots `0..size-1` and sets `size = 0`.
- In the `Collection` constructor the field `comparator` is final: assign it exactly once in an if/else chain.
  Then copy the elements into your own array: `Object[] es = c.toArray(); queue = new Object[Math.max(1,
  es.length)]; System.arraycopy(es, 0, queue, 0, es.length);`, check for nulls, set `size` and heapify. A user
  collection may return its own internal or typed array from `toArray()`; `copiesCollectionArray` fails if you
  heapify that array in place (`queue = es`). The 1-slot minimum keeps capacity >= 1 so `offer` can grow it.
- Error messages are compared for IllegalStateException ("Queue full"), IllegalArgumentException and
  NoSuchElementException (both null). Use `new IllegalArgumentException()`, not one with text.
- Before AbstractQueue exists the failure reads `javaw/runtime/ObjectReflection.class ... unsupported method
  handle java/lang/Object.getClass()...; link a complete acyclic type hierarchy: java/util/AbstractQueue`. Only
  the last part matters; the ObjectReflection text goes away once the classes link.
- Most tests drain with `poll()` and sort, so a wrong heap shows up first in `heapOrder`; compare your
  siftUp/siftDown with the pseudo-code above line by line (`>= 0` in siftUp, `> 0` and `<= 0` in siftDown).
- Ties matter: `equalPriorities` orders strings by length only, so equal-length strings tie but print
  differently. Writing `> 0` in siftUp, `>= 0` for the right-child choice, or `< 0` in siftDown's break changes
  its output (with `> 0` in siftUp the first toString is `[c, a, b, ...]` instead of `[a, b, c, ...]`; the
  siftDown mistakes show up after the first `poll()`).

## Docs

Create `docs/bytecode-priority-queue.md`:

    # Bytecode PriorityQueue

    Revision `priority-queue-vN` links `java.util.AbstractQueue` and a binary-heap `java.util.PriorityQueue` in
    the bundled exception runtime.

    Contract:

    - JDK heap layout (siftUp/siftDown/removeAt), natural ordering with ClassCastException for non-Comparable
      elements, NPE for null elements, IllegalArgumentException for capacity < 1, `Collection` constructor that
      keeps SortedSet/PriorityQueue comparators. AbstractQueue add/remove/element/clear/addAll follow the JDK,
      including IllegalStateException("Queue full"). Iterators walk a snapshot and are not fail-fast.
    - The SortedSet/PriorityQueue copy constructors, spliterator and serialization are not linked.

    Evidence: `BytecodePriorityQueueTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders,
    two fresh instances). `PriorityQueue(SortedSet)` and `PriorityQueue(PriorityQueue)` are rejected before WAT
    generation.
