# T42 ConcurrentLinkedQueue

```kit
id: T42
slug: concurrent-linked-queue
title: link java.util.concurrent.ConcurrentLinkedQueue (single-threaded)
tests: BytecodeConcurrentLinkedQueueTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/ConcurrentLinkedQueue.java
allow: docs/bytecode-concurrent-linked-queue.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-concurrent-linked-queue-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeConcurrentLinkedQueueTest.java`.

## Goal

Add `java.util.concurrent.ConcurrentLinkedQueue` to the bundled runtime under the single-threaded contract (Wasm
runs one thread; no locks, no atomics). The pinned libraries name `java/util/concurrent/ConcurrentLinkedQueue` as a
type in 22 classes (`runtime-target/target/audit/external-types.tsv`, 9 of them in the static closure) and
`requirements.tsv` lists 22 call rows (52 call sites) from 10 artifacts, category `concurrency`: `<init>()` 22
times across H2 `JdbcConnectionPool`, Hibernate `DriverManagerConnectionProviderImpl$PooledConnections`, Tomcat
`SessionIdGeneratorBase` and websocket `Util`, Spring `ConcurrentLruCache$WriteOperations`, Spring web
`PartGenerator$InMemoryState`, Spring Boot `BufferingApplicationStartup`, Jackson `PrivateMaxEntriesMap`, log4j
`StatusLogger$BoundedQueue` and istack `Pool$Impl`; `poll()` 7, `size()` 6, `add(Object)` 6, `remove(Object)` 5,
`iterator()` 3, `offer(Object)` 2 and `isEmpty()` once. Hibernate's connection pool is the main user: it adds, polls, removes a given connection and
iterates while validating.

Copy the JDK 21 *observable* behaviour: weakly consistent iterators that remember the next item, `o.equals(item)`
(argument first, no `==` shortcut), all-or-nothing `addAll`, and no calls to overridable methods of `this` except
where the JDK makes them (the test logs a subclass).

## Members to implement

One new file `javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/ConcurrentLinkedQueue.java`
(`package java.util.concurrent;`, imports from `java.util` and `java.util.function`):
`public class ConcurrentLinkedQueue<E> extends AbstractQueue<E> implements Queue<E>, java.io.Serializable` with

- `static final class Node<E> { E item; Node<E> next; Node(E item) { this.item = item; } }`
- fields `private final Node<E> head = new Node<E>(null);` (a permanent sentinel, never removed) and
  `private Node<E> tail = head;` (always the last node of the chain)
- `public ConcurrentLinkedQueue()`
- `public ConcurrentLinkedQueue(Collection<? extends E> c)`
- `public boolean offer(E e)`
- `public E poll()`
- `public E peek()`
- `public boolean isEmpty()`
- `public int size()`
- `public boolean contains(Object o)`
- `public boolean remove(Object o)`
- `public boolean addAll(Collection<? extends E> c)`
- `public String toString()`
- `public Object[] toArray()`
- `public <T> T[] toArray(T[] a)`
- `public Iterator<E> iterator()` (returns `new Itr()`)
- `public void forEach(Consumer<? super E> action)`
- `public boolean removeIf(Predicate<? super E> filter)`
- `public boolean removeAll(Collection<?> c)`
- `public boolean retainAll(Collection<?> c)`
- `public void clear()`
- `private boolean bulkRemove(Predicate<? super E> filter)`
- `private final class Itr implements Iterator<E>` with fields `Node<E> nextNode; E nextItem; Node<E> lastRet;`,
  constructor, `private void advance(Node<E> from)`, `hasNext()`, `next()`, `remove()`

Inherited and correct as they are (do not override): `add` (AbstractQueue: calls `offer`), `remove()` (calls
`poll`, `NoSuchElementException` with no message), `element()` (calls `peek`), `containsAll` (AbstractCollection:
calls `contains` per element).

## Members to leave out

- Serialization: `writeObject(ObjectOutputStream)` and `readObject(ObjectInputStream)` (those classes are not in the
  runtime). The test checks that `writeObject` stays unlinked. Keep `implements java.io.Serializable`.
- `spliterator()` and `CLQSpliterator`: the inherited `Collection.spliterator()` default is enough.
- The JDK internals: `Unsafe`/`VarHandle`, `casItem`, `updateHead`, `succ`, `skipDeadNodes`, `first()`, `MAX_HOPS`,
  self-linked nodes. No `synchronized`, no `volatile`.
- Do not edit `AbstractQueue.java`, `AbstractCollection.java` or any other runtime file (`kit guard` rejects it).

## JDK behaviour to match

Structure: `head` is a sentinel whose `item` is always `null`; live elements are the nodes after it with a non-null
`item`. Removing an element sets its node's `item = null` ("dead"). A dead node is unlinked (`pred.next = p.next`)
only if `p.next != null`: the last node is never unlinked, it stays as a dead `tail` and new nodes are appended
after it. An unlinked node keeps its own `next`, so an iterator that holds it can still walk forward. Every walk
starts at `head.next` and skips nodes whose `item` is `null`.

- `offer(e)`: `Node<E> n = new Node<E>(Objects.requireNonNull(e)); tail.next = n; tail = n; return true;`. A null
  element throws `NullPointerException` (also through `add(null)`), and the queue is unchanged.
- `poll()`: find the first node `p` with a non-null item; set `p.item = null`;
  `head.next = p.next != null ? p.next : p;` and return the item. Return `null` when there is none.
- `peek()`: the first non-null item or `null`. `isEmpty()`: walk the nodes itself and return `false` at the first
  non-null item; it must not call `peek()` or `size()` (both are overridable; the subclass fixture logs them).
- `size()`: count the non-null items (stop at `Integer.MAX_VALUE`).
- `contains(o)`: `false` for `o == null`; otherwise `true` at the first live item with `o.equals(item)`. The argument's
  `equals` is called (not the item's) for each live item in order, and there is no `o == item` shortcut: an element
  whose `equals` always returns `false` is not found even by itself (`Objects.equals` would be wrong). The inherited
  `containsAll(c)` calls `contains` per element of `c`, so it logs one `o.equals(item)` call per live item until a
  match, with the element of `c` as `o`.
- `remove(o)`: `false` for `null`; walk with `pred` (start `pred = head`); at the first live `p` with
  `o.equals(item)`: `p.item = null; if (p.next != null) pred.next = p.next; return true;`. Only the first match goes.
- Constructor `(Collection c)` and `addAll(c)`: build a private chain from `for (E e : c)` with
  `new Node<E>(Objects.requireNonNull(e))`, then append the whole chain (`tail.next = first; tail = last;`). A null
  element throws `NullPointerException` **before anything is appended** (all or nothing; `AbstractQueue.addAll` adds
  the prefix and is wrong here). A null collection throws `NullPointerException`. `addAll(this)` throws
  `new IllegalArgumentException()` (message `null`) checked first; `addAll` of an empty collection returns `false`.
  Neither calls `offer` or `add`.
- `toString()`: `[a, b, c]` (`[]` when empty) from `item.toString()` of each live item. If `toString()` of an item
  returns `null` the JDK throws `NullPointerException` (it reads the string's length); `StringBuilder.append` would
  print `null`, so check and throw. Does not call `iterator()` or `size()`.
- `toArray()`: an `Object[]` of exactly the live items (count them in your own loop first). `toArray(a)`: `Objects.requireNonNull(a)`; count the live
  items `n`; use `a` if `a.length >= n`, else `Arrays.copyOf(a, n)` (same component type, so `new String[0]` gives a
  `String[]`); fill; if the array is longer than `n`, set element `n` to `null` (later elements stay). Neither
  calls `size()` or `iterator()` of `this` (count in your own loop; a subclass may override `size()`).
- `Itr`: the constructor calls `advance(head)`. `advance(from)`: `p = from.next`, skip nodes with null item, then
  `nextNode = p; nextItem = (p == null) ? null : p.item;`. `hasNext()`: `nextItem != null`. `next()`:
  `if (nextNode == null) throw new NoSuchElementException();` then remember `x = nextItem`, `lastRet = nextNode`,
  `advance(nextNode)` and return `x`. So the iterator already holds the *next* item: if it is polled or removed
  meanwhile, `next()` still returns it; once `nextNode` is `null` the iterator stays finished even if elements are
  added later; but an element added while `nextNode` is set is seen. `remove()`:
  `if (lastRet == null) throw new IllegalStateException();` (no message: before the first `next()` and twice in a
  row) then `lastRet.item = null; lastRet = null;` (no unlinking, no effect if the element is already gone).
- `forEach(action)`: `Objects.requireNonNull(action)`, then
  `for (p = head.next; p != null; ) { Node<E> q = p.next; E item = p.item; if (item != null) action.accept(item); p = q; }`.
  `next` is read **before** the action runs: an element appended while the action handles the last element is not
  visited, an element the action removes ahead is skipped (unlike the iterator, which would still return it).
- `removeIf(f)`: `Objects.requireNonNull(f)` then `bulkRemove(f)`. `removeAll(c)` / `retainAll(c)`:
  `Objects.requireNonNull(c)` then `bulkRemove(e -> c.contains(e))` / `bulkRemove(e -> !c.contains(e))`. `clear()`:
  `bulkRemove(e -> true)`. `bulkRemove` walks once with `pred`, calls the filter once per live item in order, sets
  `item = null` on matches, unlinks dead nodes that have a successor and returns whether anything was removed. None
  of these call `iterator()`, `poll()` or `size()`.

Known difference: the JDK self-links polled nodes and restarts iterators from `head`; following the kept `next`
pointers instead reaches the same live elements, so no fixture can tell.

## Steps

1. Read the test, then `javaw-bytecode/src/main/resources/exception-runtime/java/util/AbstractQueue.java` and
   `AbstractCollection.java`.
2. Create `javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/ConcurrentLinkedQueue.java` with
   the members above.
3. Register it (do not edit `BytecodeExceptionRuntime.java` by hand):
   `javaw-agent-kit/bin/kit register java/util/concurrent/ConcurrentLinkedQueue.java`
4. `javaw-agent-kit/bin/kit check T42` until PASS.
5. Write `docs/bytecode-concurrent-linked-queue.md` (below), the report, then `javaw-agent-kit/bin/kit finish T42`.

## Hints

- `Objects.requireNonNull(T)`, `Arrays.copyOf(T[], int)` and lambdas exist in the runtime. Import
  `java.util.function.Consumer` and `java.util.function.Predicate`.
- `Itr` must be a non-static inner class (`private final class Itr`) so it can read `head`.
- In `bulkRemove`, read `q = p.next` first; after testing, `if (p.item == null && q != null) pred.next = q; else pred = p;`
  then `p = q`.
- `isEmpty()` written as `peek() == null` and `toArray()` written with `new Object[size()]` were the calibration
  mistakes: both call overridable methods (`subclassCalls` shows `peek:a size:4`). Walk the nodes directly.
- Do not add a `size` field: a removed-via-iterator node and a polled node would make it wrong.
- Overriding `toString`, `toArray`, `removeIf`, `removeAll`, `retainAll`, `clear` and `isEmpty` is required: the
  inherited versions call `iterator()`, `size()` or `poll()`, which the subclass fixture logs.
- The Wasm runner stops at the first failing fixture of each `@Test` method; after a fix another fixture may fail next.
- Calibration: the reference passed kit check after 2 runs (about 240 new lines, all in ConcurrentLinkedQueue.java).

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
offerPollAndPeek, constructorsAndAddAll, containsAndRemove, weaklyConsistentIterators, bulkOperationsAndForEach, textArraysAndSubclasses, leavesOutSerialization: if FIX THE RUNTIME says `link the symbolic class/interface java/util/concurrent/ConcurrentLinkedQueue`, `link a complete acyclic type hierarchy` or names a method of java/util/concurrent/ConcurrentLinkedQueue, the class is missing or not registered: run `javaw-agent-kit/bin/kit register java/util/concurrent/ConcurrentLinkedQueue.java` (never edit BytecodeExceptionRuntime.java by hand) and add every member listed under Members to implement. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
basics: poll sets `head.next = p.next != null ? p.next : p`; offer appends after `tail` even when tail is a dead node; offer(null) throws NullPointerException from Objects.requireNonNull before changing anything.
constructorsAndAddAll: build the whole private chain first and append it only at the end, so a null element leaves the queue unchanged; addAll(this) throws `new IllegalArgumentException()` before iterating.
containsAndRemove: compare with `o.equals(item)` (argument first) and no `==` shortcut, so `never` is not found; null returns false; remove takes only the first match.
iterators: the iterator returns the item it already fetched even if it was polled or removed; once nextNode is null it never revives; remove() throws `new IllegalStateException()` without a message and only sets `lastRet.item = null`.
bulkOperations: forEach reads `p.next` before calling the action and checks `item` when it gets to a node; clear() must not call poll(), it uses bulkRemove(e -> true).
textAndArrays: an item whose toString() returns null makes toString() throw NullPointerException; toArray(a) uses `a` when it is long enough and sets `a[n] = null` only when a.length > n.
subclassCalls: only add->offer, remove()->poll, element()->peek and containsAll->contains may call overridable methods; toString, toArray, isEmpty, addAll, removeIf, removeAll, retainAll, clear, forEach and remove(Object) must not call iterator(), size(), offer() or poll(). An extra `peek:a size:4` after `offer:c` means isEmpty() calls peek() and toArray() calls size(): walk the nodes directly in both.
```

## Docs

Create `docs/bytecode-concurrent-linked-queue.md` with this skeleton (fill in the brackets):

    # Bytecode ConcurrentLinkedQueue

    Revision `concurrent-linked-queue-vN` links `java.util.concurrent.ConcurrentLinkedQueue` in the bundled
    exception runtime under the single-threaded contract.

    Contract:

    - <sentinel-headed linked nodes; null elements rejected; poll/peek/size/contains/remove(Object) with the
      argument's equals; all-or-nothing addAll and the self-addAll IllegalArgumentException; weakly consistent
      iterator that keeps the fetched next item; forEach and bulk removal without overridable calls>

    Known differences: no locks or atomics (the selected deployment is single-threaded); polled nodes are not self-linked.
    Serialization (`writeObject`, `readObject`) and the CLQ spliterator are not linked.

    Evidence: `BytecodeConcurrentLinkedQueueTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
    orders, two fresh instances). `ConcurrentLinkedQueue.writeObject(ObjectOutputStream)` is rejected before WAT
    generation.
