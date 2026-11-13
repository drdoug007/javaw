# T07 EmptyStackException and Vector

```kit
id: T07
slug: vector
title: link java.util.EmptyStackException and a monitor-free java.util.Vector
tests: BytecodeVectorTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/EmptyStackException.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/Vector.java
allow: docs/bytecode-vector.md
```

## Goal

Add `java.util.EmptyStackException` and `java.util.Vector<E>` to the bundled runtime. Jakarta Activation,
Angus Activation, JAXB txw2, Byte Buddy and AspectJ call `Vector` (`<init>`, `add`, `addElement`, `copyInto`,
`elementAt`, `elements`, `get`, `removeElementAt`, `size`, `toArray`); ANTLR, Tomcat and txw2 throw
`EmptyStackException`. T08 (`Stack extends Vector`) builds on this task.

The JDK `Vector` is `synchronized`. Here it is **single-threaded**: write it like `ArrayList`, with no
`synchronized` anywhere (same contract as `docs/bytecode-atomic-reference-array.md`).

## Members to implement

`public class EmptyStackException extends RuntimeException` with `public EmptyStackException()` only.

`public class Vector<E> extends AbstractList<E> implements List<E>, RandomAccess, Cloneable, java.io.Serializable`
with fields `protected Object[] elementData; protected int elementCount;` and:

- Constructors: `Vector()` (capacity 10), `Vector(int initialCapacity)`, `Vector(Collection<? extends E> c)`.
- Sizes: `int size()`, `boolean isEmpty()`, `int capacity()` (returns `elementData.length`).
- Legacy: `void addElement(E)`, `E elementAt(int)`, `E firstElement()`, `E lastElement()`,
  `void insertElementAt(E,int)`, `void removeElementAt(int)`, `boolean removeElement(Object)`,
  `void removeAllElements()`, `void copyInto(Object[])`, `Enumeration<E> elements()`,
  `int indexOf(Object,int)`, `int lastIndexOf(Object,int)`.
- List: `E get(int)`, `E set(int,E)`, `boolean add(E)`, `void add(int,E)`, `E remove(int)`,
  `boolean remove(Object)`, `boolean addAll(Collection)`, `boolean addAll(int,Collection)`, `void clear()`,
  `int indexOf(Object)`, `int lastIndexOf(Object)`, `boolean contains(Object)`, `Object[] toArray()`,
  `<T> T[] toArray(T[])`, `Object clone()`.
- Inherit (do not write): `iterator`, `listIterator`, `subList`, `equals`, `hashCode`, `toString`,
  `containsAll`, `removeAll`, `retainAll`, `removeIf`, `forEach`. The inherited iterators are fail-fast
  only if **every structural change does `modCount++`**.

## Members to leave out

`setSize(int)` and the `Vector(int,int)` capacity-increment constructor (the test checks both are rejected).
`trimToSize`, `ensureCapacity`, `removeRange`, `spliterator` overrides and serialization hooks are not needed.

## JDK behaviour to match

- Capacity: `new Vector()` has capacity 10. When full, the new capacity is `max(old*2, needed)`
  (so 0 -> 1 -> 2 -> 4 -> 8; 10 -> 20; 3 -> 6; `addAll` grows to at least `size+added`).
  `Vector(Collection)` copies `c.toArray()` into an array of exactly that length.
- `new Vector(-1)`: `IllegalArgumentException("Illegal Capacity: -1")`. `new Vector(null)`: `NullPointerException`.
- Exceptions (type and message):
  - `get(i)`, `set(i,x)`, `remove(i)` with `i >= size`: `new ArrayIndexOutOfBoundsException(i)`, whose
    message is `"Array index out of range: 5"`. `addAll(i,c)` with `i<0 || i>size`: the same constructor.
  - `elementAt(i)` with `i >= size`: `ArrayIndexOutOfBoundsException(i + " >= " + size)`, e.g. `"3 >= 3"`.
  - `removeElementAt(i)`: `i >= size` -> `ArrayIndexOutOfBoundsException(i + " >= " + size)`;
    `i < 0` -> `new ArrayIndexOutOfBoundsException(i)`.
  - `insertElementAt(x,i)` and `add(i,x)` with `i > size`: `ArrayIndexOutOfBoundsException(i + " > " + size)`.
  - `lastIndexOf(o,i)` with `i >= size`: `IndexOutOfBoundsException(i + " >= " + size)`.
  - Negative indexes elsewhere (`get(-1)`, `set(-1,..)`, `elementAt(-1)`, `insertElementAt(x,-1)`): the JDK just
    touches `elementData[-1]` / `System.arraycopy`, so the type is `ArrayIndexOutOfBoundsException`. Only the type
    is tested.
  - `firstElement()`/`lastElement()` on an empty vector: `NoSuchElementException()` (null message).
  - `elements().nextElement()` past the end: `NoSuchElementException("Vector Enumeration")`.
  - `copyInto` into a too-small array: `ArrayIndexOutOfBoundsException` (from `System.arraycopy`).
- `elements()` reads the live vector (`count < elementCount` on each call), so it sees later adds.
- `indexOf(o,from)` scans `from..size-1`, `lastIndexOf(o,from)` scans `from..0`; both use `equals` and
  handle `null`. `lastIndexOf(o,-1)` returns -1.
- `toArray()` returns a fresh `Object[]` of length `size`. `toArray(a)`: if `a.length < size` return
  `Arrays.copyOf(elementData, size, a.getClass())`; otherwise copy into `a` and set `a[size]=null` if room.
- `clone()`: **must** call `super.clone()` (never `new Vector<>(this)`: T08's `Stack` inherits `clone` and must get a
  `Stack` back; the test clones a subclass), then `elementData = Arrays.copyOf(elementData, elementCount)` (so the clone's
  capacity equals its size) and `modCount = 0`.
- `modCount++` in `add`, `addElement`, `insertElementAt`, `removeElementAt`, `remove(int)`,
  `removeAllElements`, `addAll`; not in `set`.
- `removeAllElements()`/`clear()` null the slots and set size 0 but keep the capacity.

## Steps

1. Create `java/util/EmptyStackException.java` (copy `java/util/NoSuchElementException.java`, keep only the
   no-arg constructor).
2. Create `java/util/Vector.java` under `javaw-bytecode/src/main/resources/exception-runtime/` with the
   members above. Use `java/util/ArrayList.java` as the model.
3. Register both: in `BytecodeExceptionRuntime.java`, on the `List.of(...)` line that contains
   `"LinkedList"`, insert `"Vector","EmptyStackException",` right after `"LinkedList",`.
4. `javaw-agent-kit/bin/kit check T07` until PASS.
5. Write `docs/bytecode-vector.md` (skeleton below).

## Hints

- `AbstractList` (runtime copy) already gives `iterator()`, `listIterator()`, `subList`, `equals`,
  `hashCode`; `AbstractCollection` gives `toString`, `containsAll`, `removeAll`, `retainAll`.
- Write one private `grow(int minimum)` helper: `int n = elementData.length * 2; if (n < minimum) n = minimum;`
  then `elementData = Arrays.copyOf(elementData, n)`. Call it from every add path.
- `add(int,E)` must call `insertElementAt` (the message differs from `AbstractList.position`), and
  `addAll(int,Collection)` must be overridden (the inherited one throws a plain `IndexOutOfBoundsException`).
- Implement `elements()` as an anonymous `Enumeration<E>` with an `int count` field.
- `Arrays.copyOf(T[],int)` and `Arrays.copyOf(U[],int,Class)` exist in the runtime `Arrays.java`. In
  `Vector(Collection)` use `Arrays.copyOf(source, source.length, Object[].class)` so the backing array is a real
  `Object[]`.
- Throw `ArrayIndexOutOfBoundsException` yourself in `get`/`set`/`remove(int)`/`elementAt`/`removeElementAt`/
  `insertElementAt`/`addAll(int,..)`. Do not use `Objects.checkIndex` or ArrayList-style index helpers: they throw
  `IndexOutOfBoundsException` and the test compares exception types.
- Never write the word `synchronized` (or `native`, `Thread`) even inside a `/* */` or Javadoc comment: guard
  only strips `//` comments and string literals before matching.
- The reference solution is about 150 lines; passing took one `kit check` run once every member above existed.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
emptyStackException, basicsAndCapacity: if FIX THE RUNTIME says to link java/util/Vector or java/util/EmptyStackException (`link the descriptor class/interface ...`, `link the symbolic class/interface ...` or `link a complete acyclic type hierarchy`), the classes are not registered: in BytecodeExceptionRuntime.java (do not edit that file by hand: run `javaw-agent-kit/bin/kit register <path under exception-runtime>` for each new file, which makes exactly this edit), on the List.of(...) line that contains "LinkedList", insert "Vector","EmptyStackException", right after "LinkedList",. If it says `link a matching instance method in the declaring hierarchy` or `link a matching special declaration`, the Vector method or constructor that the named fixture calls is missing, not public, or has a different signature: add it exactly as listed under Members to implement.
defaultCapacity, sizedCapacity, zeroCapacity, fromCollection: capacity() is wrong, so grow is wrong. Vector doubles (not ArrayList's 1.5x) and must still reach the needed size: `if (minimum <= elementData.length) return; int n = elementData.length * 2; if (n < minimum) n = minimum; elementData = Arrays.copyOf(elementData, n);` (expected 10,20,40,21 and 30,6,[0, 1, 2, 3] and 0,1,2,4,4,8). Vector(Collection) must size the array to exactly c.toArray().length (so [x, y, z]|3|4).
getFailures, setFailures, removeByIndex, elementAtFailures, removeElementAtFailures: the exception type or message is wrong. get, set and remove(int) throw `new ArrayIndexOutOfBoundsException(index)` ("Array index out of range: 3"); elementAt and removeElementAt throw `new ArrayIndexOutOfBoundsException(index + " >= " + elementCount)` when index >= elementCount ("3 >= 3", "0 >= 0"). For index < 0 every one of them throws `new ArrayIndexOutOfBoundsException(index)` (removeElementAt(-1) prints "Array index out of range: -1"). Write these checks yourself; Objects.checkIndex and ArrayList's helpers throw IndexOutOfBoundsException ("Index 3 out of bounds for length 3").
insertFailures, addAtIndex, addAllVariants: insertElementAt must throw `new ArrayIndexOutOfBoundsException(index + " > " + elementCount)` when index > elementCount ("4 > 3"; after add(0,..) and add(4,..) the size is 5, so add(9,"x") gives "9 > 5") and `new ArrayIndexOutOfBoundsException(index)` when index < 0. Override add(int,E) as `insertElementAt(value, index);` and override addAll(int,Collection) to throw `new ArrayIndexOutOfBoundsException(index)` when `index < 0 || index > elementCount`; the inherited AbstractList versions throw IndexOutOfBoundsException instead.
lastIndexOfFailure, indexesFrom: lastIndexOf(o, from) is the one method that throws plain IndexOutOfBoundsException: `if (from >= elementCount) throw new IndexOutOfBoundsException(from + " >= " + elementCount);` then scan i = from down to 0 (so lastIndexOf(o,-1) returns -1). indexOf(o, from) scans from up to elementCount-1 and returns -1 when from >= size. Both compare with Objects.equals(o, elementData[i]).
iteratorFailFast: the inherited iterator only throws ConcurrentModificationException if modCount changes. Put modCount++ in addElement, insertElementAt, removeElementAt, remove(int), removeAllElements and addAll (not in set); add(E) should just call addElement.
cloneIsIndependent, subclassClone: clone must be `Vector<E> copy = (Vector<E>) super.clone();` inside try/catch (CloneNotSupportedException e) { throw new InternalError(); }, then `copy.elementData = Arrays.copyOf(elementData, elementCount); copy.modCount = 0; return copy;` (the copy's capacity equals its size, 3). Vector must implement Cloneable. Never use new Vector<>(this): a subclass must clone to its own class.
firstAndLastEmpty, elementsExhausted: firstElement/lastElement on an empty vector throw `new NoSuchElementException()` (prints "NoSuchElementException: null"); the elements() Enumeration throws `new NoSuchElementException("Vector Enumeration")` past the end, and hasMoreElements is `count < elementCount` read live on every call.
```

## Docs

Create `docs/bytecode-vector.md` with this skeleton (fill in the brackets):

    # Bytecode Vector

    Revision `vector-vN` links `java.util.EmptyStackException` and `java.util.Vector` in the bundled
    exception runtime. Vector is monitor-free (single-threaded contract, like AtomicReferenceArray).

    Contract:

    - <capacity growth, legacy members, Enumeration, exception types/messages>

    Evidence: `BytecodeVectorTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two fresh
    instances). `Vector.setSize` and `Vector(int,int)` are rejected before WAT generation.
