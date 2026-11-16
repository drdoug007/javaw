# Bytecode CopyOnWriteArrayList

Revision `copy-on-write-array-list-v304` links `java.util.concurrent.CopyOnWriteArrayList` in the bundled exception
runtime.

Contract:

- Every write builds a new array and installs it once with `setArray`; there is no lock (the runtime is single-threaded), and reads take the current array once.
- `get`, `set` and `remove(int)` read the current array's slot first and let the array throw; `add(int, E)`, `addAll(int, c)` and `listIterator(int)` throw `IndexOutOfBoundsException("Index: i, Size: n")`. `indexOf(e, i)` and `lastIndexOf(e, i)` are range scans with no bounds checks of their own.
- Snapshot iterators: `iterator` and `listIterator(int)` capture the array at creation, never throw `ConcurrentModificationException`, and their `remove`/`set`/`add` throw `UnsupportedOperationException`.
- `addIfAbsent` and `addAllAbsent` skip elements already present, including duplicates within the added batch; the Collection constructor copies `c.toArray()` with `Object[]` unless the argument is exactly `ArrayList` or `CopyOnWriteArrayList`.
- Writes that change nothing (`set` of the identical element, `remove`/`addIfAbsent`/`removeIf`/`removeAll`/`retainAll` that find nothing, `addAll` of an empty collection, `addAllAbsent` that adds nothing) keep the same array object, so existing sub lists stay valid.
- Sub lists from `subList` throw `ConcurrentModificationException` from `size`, `get`, `set`, `add`, `remove`, `addAll`, `clear`, `listIterator`, `subList`, `replaceAll`, `sort`, `removeIf`, `removeAll`, `retainAll` and the inherited iterator-based operations after any other write stored a new array; their range checks precede the modification check where the JDK orders them that way.
- `bulkRemove` (behind `removeIf`, `removeAll`, `retainAll`) runs the whole filter first and then throws `ConcurrentModificationException` if the array changed while filtering; `removeIf(null)` and `removeAll(null)`/`retainAll(null)` throw `NullPointerException`.
- Overridable self-calls: `isEmpty` calls `size()`, `contains` calls `indexOf`, and the sub list forwards `get`/`set`/`add`/`remove(int)`/`addAll` to the outer list, so subclasses observe those calls.

Known differences: no lock (the runtime is single-threaded); the sub list extends `AbstractList`; serialization
and the Java 21 SequencedCollection members are not linked.

Evidence: `BytecodeCopyOnWriteArrayListTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
orders, two fresh instances). `CopyOnWriteArrayList.getFirst()` is rejected before WAT generation.
