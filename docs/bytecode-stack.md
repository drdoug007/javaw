# Bytecode Stack

Revision `stack-v269` links `java.util.Stack` on top of the monitor-free `Vector` in the bundled
exception runtime.

Contract:

- `Stack<E>` extends `Vector<E>` and has no fields and no other members; the top of the stack is
  the **end** of the vector (`size() - 1`), so iteration, `toString` and `get(0)` run bottom to
  top and `pop()` removes from the end.
- `push(E)` calls `addElement` and returns the argument (also when it is `null`). `peek()` throws
  `new EmptyStackException()` (null message, a `RuntimeException`) when `size() == 0` and
  otherwise returns `elementAt(size() - 1)` without removing. `pop()` first calls `peek()` (so an
  empty stack fails with `EmptyStackException`, not `ArrayIndexOutOfBoundsException`) and then
  removes `removeElementAt(size() - 1)`. `empty()` is `size() == 0`.
- `search(Object)` is `int i = lastIndexOf(o); return i >= 0 ? size() - i : -1;`: 1-based distance
  from the top, the topmost copy of a duplicate, `null` via `Objects.equals` in
  `Vector.lastIndexOf`, and `-1` when absent or on an empty stack.
- Everything else is inherited. The runtime `Vector` defines `add`, `addAll`, `get`, `set`,
  `elementAt`, `elements`, `indexOf`, `lastIndexOf`, `contains`, `isEmpty`, `capacity`, `clear` and
  `clone`; `iterator`, `listIterator`, `subList`, `toString`, `equals` and `hashCode` come from
  `AbstractList`/`AbstractCollection`. `clone()` uses `super.clone()` in `Vector`, so a cloned `Stack` is a `Stack`. The inherited
  `modCount` bumps from `Vector` keep the `AbstractList` iterators fail-fast. The JDK's
  `synchronized` modifiers on `pop`, `peek` and `search` are intentionally absent (single-threaded
  contract). `Stack(int)` and `setSize` do not exist in the runtime and are rejected before WAT
  generation.
- Stack inherits the runtime Vector's gaps and differences (see [the Vector contract](bytecode-vector.md)):
  `setElementAt`, `ensureCapacity`, `trimToSize`, `removeRange`, the `spliterator` override and the
  serialization hooks are absent; `listIterator(int)` reports "Index: i, Size: n"; `sort`/`replaceAll` do
  not bump `modCount`; bulk removal goes through the iterator. `search`, `contains` and `indexOf` compare
  with `Objects.equals`, which matches an identical reference without calling `equals`, where the JDK
  calls `o.equals(element)`; this only shows with an `equals` that is not reflexive or that has side
  effects, and the runtime's other lists do the same.

Evidence: `BytecodeStackTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two fresh
instances). `Stack.setSize` and `Stack(int)` are rejected before WAT generation.
