# Bytecode LinkedBlockingQueue

Revision `linked-blocking-queue-v312` links the
`java.util.concurrent.BlockingQueue` interface and `java.util.concurrent.LinkedBlockingQueue` in the bundled
exception runtime under the single-threaded contract (`java.lang.InterruptedException` was linked in v310).

Contract:

- `LinkedBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E>, java.io.Serializable` is an
  optionally bounded linked queue with a sentinel head: `head` is a node whose `item` is always `null`, the
  elements are the nodes after it in order (all with non-null items), `last` is the last node (`head` when
  empty) and a plain `int count` is exact. `enqueue` is `last = last.next = node`; `dequeue` self-links the
  old sentinel (`h.next = h`) and turns the first element's node into the new sentinel with its item nulled.
  Removed nodes are really unlinked; no dead node stays in the chain.
- Constructors: `()` is `this(Integer.MAX_VALUE)`; `(int)` throws `new IllegalArgumentException()` (null
  message) for `capacity <= 0`, including negatives; `(Collection)` is `this(Integer.MAX_VALUE)` and then
  enqueues each element directly, throwing `new NullPointerException()` (null message) for a `null` element —
  it never calls `offer` or `add`, and a `null` collection throws from the for-each. `size()` is `count`;
  `remainingCapacity()` is `capacity - count` (`2147483647` for the default constructor).
- `offer(e)` checks `null` before fullness (throws `NullPointerException` even on a full queue), returns
  `false` when `count == capacity`, else enqueues and counts. Inherited `add` turns `false` into
  `IllegalStateException("Queue full")` and `addAll` keeps the prefix it added. `put(e)` rejects `null` and,
  when not full, enqueues the same way **without calling `offer`**. `poll()` returns `null` when empty and
  otherwise dequeues and decrements; `take()` behaves like `poll` on a non-empty queue **without calling
  `poll`**. `peek()` is `null` when empty, else `head.next.item`. `remove()` and `element()` on an empty
  queue throw `NoSuchElementException` (null message, inherited); `isEmpty()` is inherited (`size() == 0`).
- `contains(o)` returns `false` for `null` and otherwise walks from `head.next` returning `true` at the
  first `o.equals(p.item)` — the argument's `equals` is called for each element in order, with no `==`
  shortcut, so an element whose `equals` always returns `false` is not found even by itself. `remove(o)`
  returns `false` for `null` and at the first `o.equals(p.item)` calls `unlink(p, pred)` (`p.item = null;
  pred.next = p.next;` and `last = pred` when the last node goes, `count--`) and returns `true`; `p.next`
  is unchanged, so an iterator holding `p` can walk on.
- `drainTo(c)` calls `drainTo(c, Integer.MAX_VALUE)` virtually. `drainTo(c, maxElements)` checks, in order:
  `Objects.requireNonNull(c)`; `c == this` throws `new IllegalArgumentException()` (null message, even when
  `max <= 0`); `max <= 0` returns `0`. Then `n = min(max, count)` elements move in a try/finally: the
  rejected element and the rest stay at the front while the already-moved ones update `head` and `count -=
  i` in the finally, so when `c.add` throws on the k-th element the k-1 earlier ones are gone and the
  exception propagates unchanged.
- `Itr` is a non-static inner class holding `next`, the already-fetched `nextItem`, `lastRet` and `ancestor`.
  The constructor sets `next = head.next` and, if not `null`, `nextItem = next.item`. `hasNext()` is
  `next != null`. `next()` throws `NoSuchElementException` (null message) when `next == null`, else returns
  the fetched `nextItem` — even if that element was polled or removed meanwhile — and advances over nodes
  with `item == null`, restarting at `head.next` through a self-linked (dequeued) node via `succ`. Once
  `next` is `null` the iterator stays finished even if elements are added later; an element added while
  `next` is set is seen. `remove()` throws `IllegalStateException` (null message) when `lastRet == null`
  (before the first `next()` and twice in a row), else sets `lastRet = null` and, only when `p.item !=
  null`, unlinks `p` through `findPred(p, ancestor)` (lazy `ancestor` from `head`).
- `toArray()` is `new Object[count]` filled from the chain; `toArray(a)` reads `a.length` first (so `null`
  throws `NullPointerException`), uses `Arrays.copyOf(a, count)` (same component type) when too short,
  fills from the chain, and sets `a[count] = null` only when `a.length > count`. `toString()` calls
  `this.toArray()` (virtual) and prints `[]` for an empty array, else `[` + elements joined with `, ` +
  `]`, where an element that is `this` prints `(this Collection)` and an element that is `null` or whose
  `toString()` returns `null` prints `null` (no exception, unlike ConcurrentLinkedQueue); it must not call
  `iterator()` or `size()`. `clear()` self-links and nulls the chain (`h.next = h; p.item = null;`), then
  `head = last; count = 0;` — it must not call `poll()`.
- The only overridable self-calls are the inherited ones: `add`/`addAll` -> `offer`, `remove()` -> `poll`,
  `element()` -> `peek`, `isEmpty()` -> `size`, `containsAll` -> `contains`, `toString()` -> `toArray()` and
  `drainTo(c)` -> `drainTo(c, Integer.MAX_VALUE)`. The `(Collection)` constructor, `put`, `take`,
  `remove(Object)`, `clear` and `drainTo(c, max)` call none of them.

Known differences: no locks, conditions or atomics (the selected deployment is single-threaded). `put` on a full queue and `take`
on an empty queue throw IllegalStateException instead of blocking forever; no thread could ever wake them.
Timed `offer`/`poll` (needs TimeUnit), forEach/removeIf/removeAll/retainAll overrides, the spliterator and
serialization are not linked.

Evidence: `BytecodeLinkedBlockingQueueTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
orders, two fresh instances). `BlockingQueue.poll(long, TimeUnit)`, `LinkedBlockingQueue.offer(Object, long,
TimeUnit)` and `LinkedBlockingQueue.writeObject(ObjectOutputStream)` are rejected before WAT generation.
