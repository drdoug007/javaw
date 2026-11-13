# Bytecode PriorityQueue

Revision `priority-queue-v267` links `java.util.AbstractQueue` and a binary-heap `java.util.PriorityQueue` in
the bundled exception runtime.

Contract:

- JDK heap layout (siftUp/siftDown/removeAt), natural ordering with ClassCastException for non-Comparable
  elements, NPE for null elements, IllegalArgumentException for capacity < 1, `Collection` constructor that
  keeps the argument's `comparator()` for a SortedSet or PriorityQueue. AbstractQueue
  add/remove/element/clear/addAll follow the JDK, including IllegalStateException("Queue full");
  `PriorityQueue.add` returns `offer`'s result directly, so a subclass whose `offer` returns false does not
  see that exception.
- The `Collection` constructor always scans for nulls before heapifying, so a collection holding both a
  null and a non-Comparable element throws NPE where the JDK can throw ClassCastException first.
- Iterators walk a snapshot and are not fail-fast. `Iterator.remove()` removes the first live slot holding
  the same reference, so with duplicate references (cached boxed values) the heap layout after removal can
  differ from the JDK's. `toArray(T[])`, `toString`, `removeAll`, `retainAll` and `removeIf` are inherited and
  go through the iterator, so bulk removal is O(n²) and can leave a different heap layout than the JDK's
  `bulkRemove` + heapify.
- The SortedSet/PriorityQueue copy constructors, spliterator and serialization are not linked.

Evidence: `BytecodePriorityQueueTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders,
two fresh instances). `PriorityQueue(SortedSet)` and `PriorityQueue(PriorityQueue)` are rejected before WAT
generation.
