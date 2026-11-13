# Bytecode LinkedList

Revision `linked-list-v252` links `java.util.AbstractSequentialList` and a doubly linked `java.util.LinkedList` in the bundled exception runtime.

Contract:

- List operations (`add`/`addAll` with index, `get`/`set`/`remove`, `indexOf`/`lastIndexOf`, `contains`, `clear`, `toArray`, `subList`, `equals`/`hashCode`) follow JDK node traversal from the nearer end. Index failures use the JDK `Index: i, Size: n` messages. `addAll` snapshots the source with `toArray` and leaves `modCount` unchanged for empty sources.
- LinkedList's own first/last/stack/queue-style members (`addFirst`, `getLast`, `removeFirst`, `push`, `pop`, `peek*`, `poll*`, `offer*`, `element`, `remove()`, `removeFirstOccurrence`, `removeLastOccurrence`, `descendingIterator`) are class members with JDK empty-list results and `NoSuchElementException` failures.
- List iterators are fail-fast, with JDK `remove`/`set`/`add` state rules. `clone` returns a shallow copy that preserves subclasses; serialization hooks are not implemented.
- LinkedList implements `java.util.Deque` (and so `Queue`) since revision `queue-deque`. Newer members such as `reversed()` are still rejected before WAT generation. Spliterators use the `List` default (`ORDERED|SIZED|SUBSIZED`), not the JDK late-binding `LLSpliterator`.

Evidence: `BytecodeLinkedListTest` compares Java 17 javac fixtures against the JVM in both JAR orders and two fresh dirty-memory instances, including subclass cloning, iterator mutation, descending removal, sub-list views, long-list traversal and typed failures/messages. ASM fixtures prove unused `reversed()` calls are rejected with source locations.

Audit: after this revision, the selected core-JDK linkage report changes from 1,121 declared / 9 missing-member / 207 missing-owner signatures to 1,141 / 9 / 187. All 20 LinkedList signatures used across the 63-JAR closure are now declared. These are declaration counts, not library execution evidence.
