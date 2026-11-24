# Bytecode ConcurrentLinkedQueue

Revision `concurrent-linked-queue-v305` links `java.util.concurrent.ConcurrentLinkedQueue` in the bundled
exception runtime under the single-threaded contract.

Contract:

- A chain of nodes after a permanent sentinel `head` whose `item` is always `null`; live elements are the
  nodes after it with a non-null item, and `tail` always points at the last node. Removing an element sets
  its `item` to null (a dead node) and unlinks it only when it has a successor, so the last node stays as a
  dead `tail` and unlinked nodes keep their own `next` for iterators that hold them.
- Null elements are rejected with `NullPointerException` from `Objects.requireNonNull` before any state
  changes: in `offer`, in the `(Collection)` constructor, and in `addAll`, which builds a private chain and
  appends it all-or-nothing (`addAll(this)` throws `IllegalArgumentException` first; an empty collection
  returns `false`; neither path calls `offer`/`add`).
- `poll`, `peek`, `isEmpty`, `size`, `contains` and `remove(Object)` walk the nodes directly; `contains` and
  `remove` use the argument's `o.equals(item)` in chain order with no `==` shortcut, and `remove` takes only
  the first match.
- The weakly consistent `Itr` constructor calls `advance(head)`, remembers the fetched next item, returns it
  from `next()` even if it was polled or removed meanwhile, never revives once `nextNode` is null, and
  `remove()` throws `IllegalStateException` (no message) and only sets `lastRet.item = null`.
- `toString` reads each item's `toString()` and throws `NullPointerException` if it returns `null`;
  `toArray`/`toArray(T[])` count the live items in their own loop (no `size()`/`iterator()` calls) and
  `toArray(T[])` reuses the argument when long enough, setting element `n` to `null` only when it is longer.
- `forEach` reads `p.next` before the action runs (an element appended while the action handles the last
  element is not visited); `removeIf`, `removeAll`, `retainAll` and `clear` all go through `bulkRemove`,
  which walks once with a `pred` node, tests the filter once per live item, and unlinks dead nodes that have
  a successor. None of these call overridable `iterator()`, `size()`, `offer()` or `poll()` methods of `this`;
  only `add` → `offer`, `remove()` → `poll`, `element()` → `peek` and `containsAll` → `contains` (inherited)
  invoke overridable methods, as the JDK does.

Known differences: no locks or atomics (the selected deployment is single-threaded); polled nodes are not self-linked.
Serialization (`writeObject`, `readObject`) and the CLQ spliterator are not linked.

Evidence: `BytecodeConcurrentLinkedQueueTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
orders, two fresh instances). `ConcurrentLinkedQueue.writeObject(ObjectOutputStream)` is rejected before WAT
generation.
