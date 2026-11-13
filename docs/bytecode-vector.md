# Bytecode Vector

Revision `vector-v268` links `java.util.EmptyStackException` and `java.util.Vector` in the bundled
exception runtime. Vector is monitor-free (single-threaded contract, like AtomicReferenceArray).

Contract:

- `EmptyStackException` extends `RuntimeException` with only the no-arg constructor (null message).
- Capacity growth: `new Vector()` starts at capacity 10; when full, the new capacity is
  `max(old * 2, needed)` (0 → 1 → 2 → 4 → 8 → 16…, 10 → 20, 3 → 6, bulk adds reach at least
  `size + added`). `capacity()` returns `elementData.length`. `Vector(int)` throws
  `IllegalArgumentException("Illegal Capacity: n")` for a negative capacity and
  `NullPointerException` for a null `Collection`. `Vector(Collection)` sizes the backing array to
  exactly `c.toArray().length`.
- Legacy members: `addElement`, `elementAt`, `firstElement`/`lastElement` (the latter two throw
  `NoSuchElementException` with a null message on an empty vector), `insertElementAt`,
  `removeElementAt`, `removeElement`, `removeAllElements` (nulls the slots, keeps capacity),
  `copyInto` (via `System.arraycopy`, too-small target throws `ArrayIndexOutOfBoundsException`),
  `elements`, and the from-index `indexOf(o, from)` (scans `from..size-1`) and
  `lastIndexOf(o, from)` (scans `from..0`).
- `elements()` returns an anonymous `Enumeration` whose `hasMoreElements` reads the live vector
  (`count < elementCount` on every call), so it sees later adds; `nextElement` past the end throws
  `NoSuchElementException("Vector Enumeration")`.
- Exception types and messages: `get`, `set` and `remove(int)` throw
  `new ArrayIndexOutOfBoundsException(i)` ("Array index out of range: i") for `i >= size` and for
  negative `i`; `elementAt` and `removeElementAt` throw
  `new ArrayIndexOutOfBoundsException(i + " >= " + size)` when `i >= size` and
  `new ArrayIndexOutOfBoundsException(i)` when `i < 0`; `insertElementAt` and `add(int)` throw
  `new ArrayIndexOutOfBoundsException(i + " > " + size)` when `i > size` (`add(int, E)` delegates to
  `insertElementAt`), and `addAll(int, Collection)` throws `new ArrayIndexOutOfBoundsException(i)`
  when `i < 0 || i > size`. `lastIndexOf(o, from)` is the one method that throws a plain
  `IndexOutOfBoundsException(from + " >= " + size)`. Negative indices throw typed
  `ArrayIndexOutOfBoundsException` explicitly rather than relying on host array access, so their
  messages differ from the JDK's array-access text ("Index -1 out of bounds for length n"); a failed
  negative `insertElementAt` also does not grow the array first as the JDK does.
- `clone()` calls `super.clone()` so a subclass clones to its own class; the copy's `elementData`
  is `Arrays.copyOf(elementData, elementCount)` (capacity equals size) and `modCount` is reset.
  `modCount++` happens in every structural change (`add`, `addElement`, `insertElementAt`,
  `removeElementAt`, `remove(int)`, `removeAllElements`, `addAll`) but not in `set`, so the
  inherited `AbstractList` iterators are fail-fast. As in the JDK, `removeElement`/`remove(Object)`
  bump it even when nothing matches, and `remove(int)` bumps it before its bounds check.
- `isEmpty`, `contains` (via `indexOf`) and `remove(Object)` (via `removeElement`) are implemented
  directly. `addAll(int, Collection)` checks the index before reading the collection.
- `setSize(int)` and the `Vector(int, int)` capacity-increment constructor are intentionally absent
  and rejected before WAT generation; `trimToSize`, `ensureCapacity`, `removeRange`, `spliterator`
  overrides and serialization hooks are not implemented. `iterator`, `listIterator`, `subList`,
  `equals`, `hashCode`, `toString`, `containsAll`, `removeAll`, `retainAll`, `removeIf` and
  `forEach` are inherited from `AbstractList`/`AbstractCollection`. Consequences: `listIterator(int)`
  reports "Index: i, Size: n" instead of the JDK's "Index: i"; `sort`/`replaceAll` (List defaults) do
  not bump `modCount`; `removeIf`/`removeAll`/`retainAll` remove through the iterator (O(n²), and not
  atomic when the predicate throws).

Evidence: `BytecodeVectorTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two fresh
instances). `Vector.setSize` and `Vector(int,int)` are rejected before WAT generation.
