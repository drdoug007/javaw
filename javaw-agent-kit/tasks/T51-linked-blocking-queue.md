# T51 BlockingQueue and LinkedBlockingQueue

```kit
id: T51
slug: linked-blocking-queue
title: link java.util.concurrent.BlockingQueue and LinkedBlockingQueue (single-threaded)
tests: BytecodeLinkedBlockingQueueTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/lang/InterruptedException.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/BlockingQueue.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/LinkedBlockingQueue.java
allow: docs/bytecode-linked-blocking-queue.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-linked-blocking-queue-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeLinkedBlockingQueueTest.java`.

## Goal

Add `java.lang.InterruptedException`, the `java.util.concurrent.BlockingQueue` interface and
`java.util.concurrent.LinkedBlockingQueue` to the bundled runtime under the single-threaded contract (Wasm runs one
thread; no locks, no conditions, no atomics). `runtime-target/target/audit/member-references.tsv` lists the uses.
Construction: `LinkedBlockingQueue.<init>()V` in H2 `Utils.createSingleThreadExecutor`, Tomcat `TaskQueue.<init>()`,
AspectJ `AsynchronousFileCacheBacking.<clinit>` and slf4j `SubstituteLoggerFactory.<init>`; `<init>(I)V` in Tomcat
`TaskQueue.<init>(I)`, HikariCP `HikariPool.<init>` (2) and Spring `ThreadPoolExecutorFactoryBean.createQueue` (2);
`<init>(Ljava/util/Collection;)V` in Tomcat `TaskQueue`. Direct calls: `offer(Object)` 5 times in Tomcat
`TaskQueue.force`, `take()` in `TaskQueue.take`, `clear()` in slf4j `SubstituteLoggerFactory.clear`, and
`size()` and `drainTo(Collection,int)` in slf4j `LoggerFactory.replayEvents`. Interface calls on
`java/util/concurrent/BlockingQueue` (opcode 185): `size()` 9 sites (Tomcat `StandardThreadExecutor.getQueueSize`,
logback `AsyncAppenderBase.getNumberOfElementsInQueue`, Spring `ThreadPoolTaskExecutor.getQueueSize`, H2
`FileStore.submitOrRun`), `isEmpty()` 8 (Tomcat `ThreadPoolExecutor.addWorker`), `offer(Object)` 4 (logback
`AsyncAppenderBase.put`, Tomcat `ThreadPoolExecutor.executeInternal`, AspectJ), `take()` 4 (logback
`AsyncAppenderBase$Worker.run`, Tomcat `ThreadPoolExecutor.getTask`, AspectJ), `remove(Object)` 4,
`drainTo(Collection)` 2 (logback `AsyncAppenderBase$Worker.run`, Tomcat `ThreadPoolExecutor.drainQueue`),
`remainingCapacity()` 2 (logback `AsyncAppenderBase.getRemainingCapacity`), `iterator()` 2, `put(Object)`
(logback `putUninterruptibly`), `poll()`, `toArray()` and `toArray(Object[])` (Tomcat `purge`/`drainQueue`).
The timed `offer(Object,long,TimeUnit)` / `poll(long,TimeUnit)` rows (Tomcat `AsyncFileHandler`, `ThreadPoolExecutor.getTask`,
`TaskQueue.poll`) need `java.util.concurrent.TimeUnit`, which is not in the runtime yet: they are a later task.

Copy the JDK 21 *observable* behaviour: a capacity bound, `IllegalStateException("Queue full")` from `add`,
`o.equals(item)` (argument first, no `==` shortcut), a weakly consistent iterator that already holds the next item,
`drainTo` that keeps what it already moved when the target throws, and exactly the JDK's calls to overridable methods
of `this` (the test logs a subclass).

## Members to implement

1. `javaw-bytecode/src/main/resources/exception-runtime/java/lang/InterruptedException.java`:
   `public class InterruptedException extends Exception` with `public InterruptedException()` and
   `public InterruptedException(String s)` (copy `java/lang/CloneNotSupportedException.java`). T49 (ReentrantLock) adds the
   same class: if `java/lang/InterruptedException.java` already exists and is registered, leave it unchanged and skip
   creating and registering it.

2. `javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/BlockingQueue.java`
   (`package java.util.concurrent;`): `public interface BlockingQueue<E> extends Queue<E>` declaring exactly

   - `boolean add(E e);`
   - `boolean offer(E e);`
   - `void put(E e) throws InterruptedException;`
   - `E take() throws InterruptedException;`
   - `int remainingCapacity();`
   - `boolean remove(Object o);`
   - `boolean contains(Object o);`
   - `int drainTo(Collection<? super E> c);`
   - `int drainTo(Collection<? super E> c, int maxElements);`

3. `javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/LinkedBlockingQueue.java`:
   `public class LinkedBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E>, java.io.Serializable`
   with

   - `static class Node<E> { E item; Node<E> next; Node(E x) { item = x; } }`
   - fields `private final int capacity;`, `private int count;`, `Node<E> head;` (sentinel, `item == null`),
     `private Node<E> last;`
   - `private void enqueue(Node<E> node)`, `private E dequeue()`, `void unlink(Node<E> p, Node<E> pred)`,
     `Node<E> succ(Node<E> p)`, `Node<E> findPred(Node<E> p, Node<E> ancestor)`
   - `public LinkedBlockingQueue()`
   - `public LinkedBlockingQueue(int capacity)`
   - `public LinkedBlockingQueue(Collection<? extends E> c)`
   - `public int size()`
   - `public int remainingCapacity()`
   - `public void put(E e) throws InterruptedException`
   - `public boolean offer(E e)`
   - `public E take() throws InterruptedException`
   - `public E poll()`
   - `public E peek()`
   - `public boolean remove(Object o)`
   - `public boolean contains(Object o)`
   - `public Object[] toArray()`
   - `public <T> T[] toArray(T[] a)`
   - `public String toString()`
   - `public void clear()`
   - `public int drainTo(Collection<? super E> c)`
   - `public int drainTo(Collection<? super E> c, int maxElements)`
   - `public Iterator<E> iterator()` (returns `new Itr()`)
   - `private class Itr implements Iterator<E>` with fields `Node<E> next; E nextItem; Node<E> lastRet;
     Node<E> ancestor;`, constructor, `hasNext()`, `next()`, `remove()`

Inherited and correct as they are (do not override): `add` and `addAll` (AbstractQueue: `add` calls `offer` and
throws `IllegalStateException("Queue full")`; `addAll` checks null and `this`, then calls `add` per element and
keeps the prefix it added), `remove()` (calls `poll`), `element()` (calls `peek`), `isEmpty()` (AbstractCollection:
`size() == 0`), `containsAll` (calls `contains` per element).

## Members to leave out

- Timed operations `offer(E, long, TimeUnit)` and `poll(long, TimeUnit)`, on both the interface and the class:
  `java.util.concurrent.TimeUnit` is not in the runtime, and a positive timeout on a full/empty queue would have to
  sleep. A later task adds them after TimeUnit. The test checks that `BlockingQueue.poll(JLTimeUnit;)` and
  `LinkedBlockingQueue.offer(Object,JLTimeUnit;)` stay unlinked. Do not create `TimeUnit.java`.
- Serialization: `writeObject(ObjectOutputStream)` and `readObject(ObjectInputStream)`. The test checks that
  `writeObject` stays unlinked. Keep `implements java.io.Serializable`.
- `forEach`, `removeIf`, `removeAll`, `retainAll`, `spliterator()`/`LBQSpliterator`, `Itr.forEachRemaining` and
  `bulkRemove`: the inherited defaults are used for now and the test does not call them. A later task.
- The JDK internals: `ReentrantLock`, `Condition`, `AtomicInteger count`, `signalNotEmpty`/`signalNotFull`,
  `fullyLock`/`fullyUnlock`. No `synchronized`, no `volatile`, no `Thread`.
- Do not edit `AbstractQueue.java`, `AbstractCollection.java` or any other runtime file (`kit guard` rejects it).

## JDK behaviour to match

Structure (the JDK's, without the locks): `head` is a sentinel node whose `item` is always `null`; the elements are
the nodes after it, in order, all with non-null items; `last` is the last node (`head` when empty); `count` is the
number of elements. Removed nodes are really unlinked (no dead nodes stay in the chain).

- `enqueue(node)`: `last = last.next = node;`. `dequeue()`: `Node<E> h = head; Node<E> first = h.next; h.next = h;
  head = first; E x = first.item; first.item = null; return x;` (the old sentinel links to itself; the first element's
  node becomes the new sentinel). `succ(p)`: `if (p == (p = p.next)) p = head.next; return p;` (a self-linked node
  means "it was dequeued, restart at the front").
- Constructors: `()` is `this(Integer.MAX_VALUE)`. `(int capacity)`: `capacity <= 0` throws
  `new IllegalArgumentException()` (message `null`; also for negative values); `last = head = new Node<E>(null)`.
  `(Collection c)`: `this(Integer.MAX_VALUE)`, then for each element: `null` throws `new NullPointerException()`
  (message `null`), else `enqueue(new Node<E>(e))` and count it. A `null` collection throws `NullPointerException`
  from the for-each. Does not call `offer` or `add`.
- `size()`: `count`. `remainingCapacity()`: `capacity - count` (`2147483647` for the default constructor).
- `offer(e)`: `null` throws `NullPointerException` even when the queue is full (check null first); if
  `count == capacity` return `false`; else enqueue, `count++`, `true`. `add` (inherited) turns `false` into
  `IllegalStateException` with message `Queue full`.
- `put(e)`: `null` throws `NullPointerException`; if not full, enqueue exactly like `offer` but **without calling
  `offer`** (the subclass fixture checks). If full, the JDK waits for another thread forever; this runtime has no
  other thread, so throw `new IllegalStateException("LinkedBlockingQueue.put would block forever: queue full and
  no other thread")`. The test never reaches that path (it cannot be compared with the JVM).
- `poll()`: `null` when `count == 0`, else `dequeue()` and `count--`. `peek()`: `null` when empty, else
  `head.next.item`. `take()`: like `poll()` on a non-empty queue, **without calling `poll`**; on an empty queue throw
  `new IllegalStateException("LinkedBlockingQueue.take would block forever: queue empty and no other thread")`
  (never reached by the test). `remove()` and `element()` on an empty queue throw `NoSuchElementException` with
  message `null` (inherited).
- `contains(o)`: `false` for `o == null`; otherwise walk `p = head.next` and return `true` at the first
  `o.equals(p.item)`. The argument's `equals` is called for each element in order and there is no `==` shortcut: an
  element whose `equals` always returns `false` is not found even by itself.
- `remove(o)`: `false` for `null`; `for (pred = head, p = pred.next; p != null; pred = p, p = p.next)` and at the first
  `o.equals(p.item)` call `unlink(p, pred)` and return `true`. `unlink(p, pred)`: `p.item = null; pred.next = p.next;
  if (last == p) last = pred; count--;`. `p.next` is **not** changed, so an iterator holding `p` can walk on.
- `toArray()`: `new Object[count]` filled from the chain. `toArray(a)`: read `a.length` first (so `null` throws
  `NullPointerException`); if `a.length < count` use `Arrays.copyOf(a, count)` (same component type, so
  `new String[0]` gives a `String[]`); fill from the chain; if `a.length > count` set `a[count] = null` (later
  elements stay).
- `toString()`: calls `this.toArray()` (**virtual**: the subclass fixture logs `toArray`), returns `[]` for an empty
  array, else `[` + elements joined with `, ` + `]`, where an element that is `this` prints `(this Collection)`, and
  an element that is `null` or whose `toString()` returns `null` prints `null` (unlike ConcurrentLinkedQueue, no
  exception). Must not call `iterator()` or `size()`.
- `clear()`: `for (Node<E> p, h = head; (p = h.next) != null; h = p) { h.next = h; p.item = null; }` then
  `head = last; count = 0;`. Must not call `poll()` (the inherited `AbstractQueue.clear` does; override it).
- `drainTo(c)`: `return drainTo(c, Integer.MAX_VALUE);` (**virtual** call: the subclass fixture logs
  `drainTo:2147483647`). `drainTo(c, max)`: in this order: `Objects.requireNonNull(c)`; `c == this` throws
  `new IllegalArgumentException()` (message `null`, even when `max <= 0`); `max <= 0` returns `0`. Then
  `n = Math.min(max, count)`, `h = head`, `i = 0`, and in a `try` loop while `i < n`: `p = h.next; c.add(p.item);
  p.item = null; h.next = h; h = p; ++i;` returning `n`; in `finally`, if `i > 0`: `head = h; count -= i;`. So if
  `c.add` throws on the k-th element, the k-1 elements before it are gone from the queue, the rejected element and
  the rest stay, and the exception propagates unchanged (`IllegalArgumentException:no bad` from the `Picky` target,
  `IllegalStateException:Queue full` from a full `LinkedBlockingQueue` target).
- `Itr`: the constructor sets `next = head.next` and, if not `null`, `nextItem = next.item`. `hasNext()`:
  `next != null`. `next()`: `if ((p = next) == null) throw new NoSuchElementException();` (message `null`) then
  `lastRet = p; E x = nextItem;` then advance: `E e = null; for (p = p.next; p != null && (e = p.item) == null; )
  p = succ(p); next = p; nextItem = e;` and return `x`. So the iterator returns the item it already fetched even
  if that element was polled or removed meanwhile; once `next` is `null` it stays finished even if elements are
  added later; an element added while `next` is set is seen. `remove()`: `p = lastRet; if (p == null) throw new
  IllegalStateException();` (message `null`: before the first `next()` and twice in a row) `lastRet = null;` and if
  `p.item != null`: `if (ancestor == null) ancestor = head; ancestor = findPred(p, ancestor); unlink(p, ancestor);`
  (no effect when the element is already gone). `findPred(p, ancestor)`: `if (ancestor.item == null) ancestor =
  head; for (Node<E> q; (q = ancestor.next) != p; ) ancestor = q; return ancestor;`.
- Overridable self-calls (the `Spy` subclass logs `offer`, `poll`, `peek`, `size`, `contains`, `iterator`,
  `toArray()`, `remainingCapacity` and `drainTo(c,int)`): only `add`/`addAll` -> `offer`, `remove()` -> `poll`,
  `element()` -> `peek`, `isEmpty()` -> `size`, `containsAll` -> `contains`, `toString()` -> `toArray()` and
  `drainTo(c)` -> `drainTo(c, Integer.MAX_VALUE)`. The `(Collection)` constructor, `put`, `take`, `remove(Object)`,
  `clear` and `drainTo(c, max)` call none of them.

## Steps

1. Read the test, then `javaw-bytecode/src/main/resources/exception-runtime/java/util/AbstractQueue.java`,
   `AbstractCollection.java`, `Queue.java` and, for style, `java/util/concurrent/ConcurrentLinkedQueue.java`.
2. Create `java/lang/InterruptedException.java`, `java/util/concurrent/BlockingQueue.java` and
   `java/util/concurrent/LinkedBlockingQueue.java` under `javaw-bytecode/src/main/resources/exception-runtime/` with
   the members above.
3. Register all three (do not edit `BytecodeExceptionRuntime.java` by hand):
   - `javaw-agent-kit/bin/kit register java/lang/InterruptedException.java` (skip if T49 already added it)
   - `javaw-agent-kit/bin/kit register java/util/concurrent/BlockingQueue.java`
   - `javaw-agent-kit/bin/kit register java/util/concurrent/LinkedBlockingQueue.java`
4. `javaw-agent-kit/bin/kit check T51` until PASS.
5. Write `docs/bytecode-linked-blocking-queue.md` (below), the report, then `javaw-agent-kit/bin/kit finish T51`.

## Hints

- Import `java.util.AbstractQueue`, `Arrays`, `Collection`, `Iterator`, `NoSuchElementException`, `Objects` in
  `LinkedBlockingQueue.java`, and `java.util.Collection`, `java.util.Queue` in `BlockingQueue.java`.
  `InterruptedException` is in `java.lang` (no import).
- `Itr` must be a non-static inner class (`private class Itr`) so it can read `head`.
- Use a plain `int count` field, not `AtomicInteger`, and keep it exact: `offer`/`put` add 1, `poll`/`take` and
  `unlink` subtract 1, `drainTo` subtracts `i`, `clear` sets 0, the collection constructor sets `n`.
- Use `Arrays.copyOf(a, count)` for a short array; `java.lang.reflect.Array.newInstance` is not available.
- In `toString()`, build with a `StringBuilder` from `Object[] a = toArray();` and check `e == this` and
  `e == null || e.toString() == null` per element.
- Make the `throws InterruptedException` clauses match exactly on the interface and the class; the methods never
  actually throw it here.
- Easy calibration mistakes: `take()` written as `poll()` and `put()` written as `offer()` (the subclass log shows an
  extra `poll:`/`offer:`); not overriding `clear()` (logs `poll:`); `toString()` written with the iterator (logs
  `iterator` instead of `toArray`); `drainTo(c, max)` checking `max <= 0` before `c == this`.
- The Wasm runner stops at the first failing fixture of each `@Test` method; after a fix another fixture may fail next.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
capacityAndOffer, putAndTakeWithoutBlocking, constructorsAndAddAll, containsAndRemove, drainTo, weaklyConsistentIterators, textArraysClearAndSubclasses, leavesOutTimedOperationsAndSerialization: if FIX THE RUNTIME says `link the symbolic class/interface java/util/concurrent/LinkedBlockingQueue` (or `BlockingQueue`, `java/lang/InterruptedException`), `link a complete acyclic type hierarchy` or names one of their methods, a class is missing or not registered: run `javaw-agent-kit/bin/kit register` for each of the three files (never edit BytecodeExceptionRuntime.java by hand) and add every member listed under Members to implement. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
capacityAndOffer: the capacity constructor throws `new IllegalArgumentException()` for capacity <= 0; offer checks null before fullness and returns false when count == capacity; remainingCapacity is capacity - count and the no-argument constructor uses Integer.MAX_VALUE.
putAndTake: put and take on a non-full/non-empty queue behave like offer and poll, and add on a full queue throws IllegalStateException `Queue full` (inherited from AbstractQueue).
constructorsAndAddAll: the collection constructor throws `new NullPointerException()` for a null element and never calls offer; addAll is inherited, so with too little room it adds the prefix and then throws IllegalStateException `Queue full`.
containsAndRemove: compare with `o.equals(item)` (argument first) and no `==` shortcut, so `never` is not found; null returns false; remove unlinks only the first match, fixes `last` when it removes the last node, and decrements count (remainingCapacity grows).
drainTo: drainTo(c, max) checks requireNonNull(c), then c == this (IllegalArgumentException, message null), then max <= 0 returns 0; when c.add throws, set head/count for the elements already moved in a finally block so the rejected element stays at the front.
iterators: the iterator returns the item it already fetched even if that element was polled, removed, drained or cleared; dequeue and clear self-link old nodes (`h.next = h`) and succ() restarts at head.next; remove() throws `new IllegalStateException()` without a message and only unlinks when lastRet.item != null.
textArraysAndClear: toString prints `null` for a null-returning toString and `(this Collection)` for the queue itself; toArray(a) uses `a` when it is long enough and sets `a[count] = null` only when a.length > count; clear resets count to 0 and head = last.
subclassCalls: take must not call poll, put must not call offer, clear must not call poll, toString must call toArray() and nothing else, drainTo(c) must call drainTo(c, Integer.MAX_VALUE); do not override isEmpty, add or addAll.
leavesOutTimedOperationsAndSerialization: this fails when the timed poll/offer or LinkedBlockingQueue.writeObject became linked. Do not add TimeUnit, timed methods or serialization methods.
```

## Docs

Create `docs/bytecode-linked-blocking-queue.md` with this skeleton (fill in the brackets):

    # Bytecode LinkedBlockingQueue

    Revision `linked-blocking-queue-vN` links `java.lang.InterruptedException`, the
    `java.util.concurrent.BlockingQueue` interface and `java.util.concurrent.LinkedBlockingQueue` in the bundled
    exception runtime under the single-threaded contract.

    Contract:

    - <optionally bounded linked queue with a sentinel head; capacity constructors and IllegalArgumentException;
      offer/add/put/poll/peek/take/remainingCapacity; null elements rejected; contains/remove(Object) with the
      argument's equals; drainTo with the self check and partial transfer when the target throws; weakly consistent
      iterator; toString via toArray; clear without poll; the overridable self-calls>

    Known differences: no locks, conditions or atomics (the selected deployment is single-threaded). `put` on a full queue and `take`
    on an empty queue throw IllegalStateException instead of blocking forever; no thread could ever wake them.
    Timed `offer`/`poll` (needs TimeUnit), forEach/removeIf/removeAll/retainAll overrides, the spliterator and
    serialization are not linked.

    Evidence: `BytecodeLinkedBlockingQueueTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
    orders, two fresh instances). `BlockingQueue.poll(long, TimeUnit)`, `LinkedBlockingQueue.offer(Object, long,
    TimeUnit)` and `LinkedBlockingQueue.writeObject(ObjectOutputStream)` are rejected before WAT generation.
