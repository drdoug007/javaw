# Bytecode CopyOnWriteArraySet

Revision `copy-on-write-array-set-v340` links `java.util.concurrent.CopyOnWriteArraySet` in the bundled exception
runtime as a wrapper over the linked `CopyOnWriteArrayList`.

Contract:

- Every operation delegates to the wrapped list `al` without calling the overridable methods of `this`: `size`, `isEmpty`, `contains`, `toArray`, `clear`, `remove`, `iterator`, `removeIf`, `forEach`, `removeAll` and `retainAll` each forward to the list's own method, so subclasses observing those calls (like the test's `Spy`) see none of them.
- `add(e)` is `al.addIfAbsent(e)` (null allowed once); `addAll(c)` is `al.addAllAbsent(c) > 0`, which also deduplicates inside `c` and never calls `this.add`.
- The Collection constructor copies `cc.al` (the exact list array) only when `c.getClass() == CopyOnWriteArraySet.class`; otherwise it builds an empty list and calls `al.addAllAbsent(c)`, which reads `c.toArray()` (not the iterator) and keeps first occurrences in collection order. A null collection throws `NullPointerException`.
- `iterator()` is the list's snapshot iterator: it sees the elements at creation, later changes do not show, and `remove` throws `UnsupportedOperationException` (message `null`).
- `containsAll(c)` uses `compareSets(al.getArray(), c) >= 0` for a `Set` argument (matching by `Objects.equals(x, snapshot[i])`, `==` first) and `al.containsAll(c)` otherwise (matching by `o.equals(element)` with no `==` shortcut); `equals(o)` is `o == this || (o instanceof Set && compareSets(...) == 0)` — no size check, no `containsAll`, and a `List` with the same elements is not equal. `compareSets` returns `-1` when the argument set has an unmatched element, `0` for the same elements, `1` when the snapshot has extras, and never calls `set.size()`.
- `hashCode()` (AbstractSet: sum of element hash codes, `0` for null, via `this.iterator()`) and `toString()` (AbstractCollection: `[a, b]` via `this.iterator()`) are inherited, so subclasses that override `iterator` observe those calls, as on the JVM.

Known differences: none observable in the single-threaded deployment; `spliterator()` is the inherited `Set`
default instead of an array spliterator.

Evidence: `BytecodeCopyOnWriteArraySetTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
orders, two fresh instances).
