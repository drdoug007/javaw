# T37 EnumSet

```kit
id: T37
slug: enum-set
title: link java.util.EnumSet
tests: BytecodeEnumSetTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/EnumSet.java
allow: docs/bytecode-enum-set.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-enum-set-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeEnumSetTest.java`.

## Goal

Add `java.util.EnumSet` to the bundled runtime: a `Set` of the constants of one enum type, stored as bits indexed by
`ordinal()`. The pinned libraries name `java/util/EnumSet` from 80 classes (`runtime-target/target/audit/external-types.tsv`:
Jackson `EnumSetDeserializer` / `EnumSetSerializer` / `ClassUtil`, Hibernate `EventTypeSets`, `Generator` and the
`*Generation` classes, `SchemaCreatorImpl`, Spring `CollectionFactory`, `MergedAnnotation$Adapt`,
`ConcurrentReferenceHashMap$Task`, javapoet `CodeWriter`, H2 `FileUtils`, Log4j `StandardLevel`, Tomcat, ...). The
member rows in `member-references.tsv` are `noneOf(Class)` 20 call sites, `contains` 15, `allOf` 13, `of(E)` 11,
`size` 10, `iterator` 9, `add` 8, `addAll` 8, `of(E,E...)` 7, `copyOf(Collection)` 7, `isEmpty` 7, `of(E,E)` 6,
`of(E,E,E)` 4, `of(E,E,E,E)` 4, `copyOf(EnumSet)` 3, `equals` 2, `toArray(Object[])` 1 and `remove` 1. The task links
every public Java 17 factory and the `Set` operations, so later libraries need no follow-up.

The JDK splits the class into an abstract sealed `EnumSet` and two hidden subclasses: `RegularEnumSet` (one `long`, up
to 64 constants) and `JumboEnumSet` (a `long[]`, more than 64). Here there is **one** concrete class `EnumSet` holding a
`long[] elements` (one word per 64 constants, at least one word). Its iterator is written exactly like the JDK
`JumboEnumSet` iterator, which for a one-word set behaves exactly like the `RegularEnumSet` iterator, so both JDK
behaviours come out of the same code. The test covers enums with 2, 3, 7, 64 and 130 constants, an empty enum, and an
enum whose constants have bodies.

## Members to implement

One file `javaw-bytecode/src/main/resources/exception-runtime/java/util/EnumSet.java` (`package java.util;`):

`public class EnumSet<E extends Enum<E>> extends AbstractSet<E> implements Cloneable, java.io.Serializable` with

- fields `final Class<E> elementType;`, `final Enum<?>[] universe;` (package-private, no modifier) and
  `private long[] elements;` (not final: `clone()` replaces it in the copy)
- `EnumSet(Class<E> elementType, Enum<?>[] universe)` (package-private constructor): stores both and sets
  `elements = new long[Math.max(1, (universe.length + 63) >>> 6)]`
- `public static <E extends Enum<E>> EnumSet<E> noneOf(Class<E> elementType)`
- `public static <E extends Enum<E>> EnumSet<E> allOf(Class<E> elementType)`
- `public static <E extends Enum<E>> EnumSet<E> copyOf(EnumSet<E> s)`
- `public static <E extends Enum<E>> EnumSet<E> copyOf(Collection<E> c)`
- `public static <E extends Enum<E>> EnumSet<E> complementOf(EnumSet<E> s)`
- `public static <E extends Enum<E>> EnumSet<E> of(E e)`, `of(E e1, E e2)`, `of(E e1, E e2, E e3)`,
  `of(E e1, E e2, E e3, E e4)`, `of(E e1, E e2, E e3, E e4, E e5)` (all `public static <E extends Enum<E>> EnumSet<E>`)
- `@SafeVarargs public static <E extends Enum<E>> EnumSet<E> of(E first, E... rest)`
- `public static <E extends Enum<E>> EnumSet<E> range(E from, E to)`
- `public EnumSet<E> clone()`
- `final void typeCheck(E e)` (package-private)
- private helpers: `private void fillAll()`, `private void complement()`, `private void addRange(E from, E to)`,
  `private boolean accepts(Object e)`, `private EnumSet<?> sameKind(Object c)`
- `public Iterator<E> iterator()` with a private inner (non-static) class `Itr implements Iterator<E>`
  (`hasNext()`, `next()`, `remove()`)
- `public int size()`, `public boolean isEmpty()`, `public boolean contains(Object e)`, `public boolean add(E e)`,
  `public boolean remove(Object e)`, `public void clear()`
- `public boolean containsAll(Collection<?> c)`, `public boolean addAll(Collection<? extends E> c)`,
  `public boolean removeAll(Collection<?> c)`, `public boolean retainAll(Collection<?> c)`
- `public boolean equals(Object o)`

`hashCode()`, `toString()`, `toArray()`, `toArray(T[])` and `removeIf` are inherited (`AbstractSet`,
`AbstractCollection`, `Collection`) and already behave like the JDK: do not override them.

## Members to leave out

- Serialization: `writeReplace()`, `readObject`, `readObjectNoData` and the nested `SerializationProxy`. The test
  checks that `EnumSet.writeReplace()` stays unlinked.
- The JDK subclasses `RegularEnumSet` and `JumboEnumSet`: do not create them. Known difference: in the JDK,
  `EnumSet` is `abstract sealed` and `getClass()` of a set is `java.util.RegularEnumSet` or `java.util.JumboEnumSet`;
  here it is `java.util.EnumSet`. The fixtures never print it.
- Do not edit `Class.java`, `Enum.java`, `AbstractSet.java`, `AbstractCollection.java` or any other runtime file
  (`kit guard` rejects it).

## JDK behaviour to match

Words: `elements[k]` holds the constants with ordinals `64*k .. 64*k+63`; constant `o` is bit `1L << o` (Java masks the
shift to `o & 63`) of word `o >>> 6`. "Mask" below is `-1L >>> -universe.length`: the low `universe.length % 64` bits,
or all 64 bits when the length is a multiple of 64 (64 constants: `-1L >>> -64` is `-1L`; 130 constants: 2 bits).

Factories:

- `noneOf(elementType)`: `Enum<?>[] universe = elementType.getEnumConstants();` (a `null` class throws
  `NullPointerException` here). If `universe == null` throw
  `new ClassCastException(elementType + " not an enum")`. Messages: `noneOf((Class) String.class)` ->
  `class java.lang.String not an enum`; `allOf((Class) Integer.class)` -> `class java.lang.Integer not an enum`; the
  class of a constant with a body is not an enum class: `noneOf((Class) Op.PLUS.getClass())` ->
  `class Op$1 not an enum`. An enum with no constants works (`noneOf`, `allOf` and `complementOf` give `[]`).
- `allOf(t)`: `noneOf(t)` then `fillAll()`: if `universe.length == 0` do nothing, else every word `-1L` and the last
  word `-1L >>> -universe.length`.
- `of(e)` ... `of(e1..e5)`: `noneOf(e1.getDeclaringClass())` (not `getClass()`: a constant with a body has an anonymous
  class `Op$1` whose superclass is `Op`), then `add` each argument in order. Duplicates are fine
  (`of(PINK, BLUE, RED, BLUE)` is `[RED, BLUE, PINK]`). A `null` argument throws `NullPointerException` (from
  `getDeclaringClass()` or from `typeCheck`). A raw call with two enum types, `of((Enum) Color.RED, (Enum) Fruit.PEAR)`,
  throws `ClassCastException: class Fruit != class Color` from `add`.
- `of(first, rest...)`: `noneOf(first.getDeclaringClass())`, `add(first)`, then `for (E e : rest) add(e);` (a `null`
  `rest` array throws `NullPointerException`; `of(GREEN, new Color[0])` is `[GREEN]`).
- `range(from, to)`: `if (from.compareTo(to) > 0) throw new IllegalArgumentException(from + " > " + to);` (string `+`
  uses `toString()`, not `name()`: `range(BLUE, RED)` -> `BLUE > RED`, and for an enum whose `toString` returns
  `"lvl-" + ordinal()`, `range(HIGH, LOW)` -> `lvl-2 > lvl-0`). Then `noneOf(from.getDeclaringClass())` and
  `addRange(from, to)`: set every bit from `from.ordinal()` to `to.ordinal()` inclusive (`range(BLUE, BLUE)` is
  `[BLUE]`; `range(H60, H70)` crosses from word 0 into word 1). `range(null, X)` is a `NullPointerException`; a raw
  `range` with two enum types throws the `ClassCastException` of `Enum.compareTo` (the test checks only the type).
- `copyOf(EnumSet s)`: `s.clone()` (`null` -> `NullPointerException`; an empty set gives `[]`).
- `copyOf(Collection c)`: if `c instanceof EnumSet` return `((EnumSet<E>) c).clone()` (so an empty `EnumSet` passed as
  a `Collection` gives `[]`). Otherwise `if (c.isEmpty()) throw new IllegalArgumentException("Collection is empty");`
  then take `c.iterator()`, `EnumSet<E> result = EnumSet.of(i.next());` and `add` the rest. `null` ->
  `NullPointerException`; a list starting with `null` -> `NullPointerException`; a raw list `[RED, APPLE]` ->
  `ClassCastException: class Fruit != class Color`.
- `complementOf(s)`: `EnumSet<E> result = copyOf(s); result.complement(); return result;` where `complement()` does
  nothing for an empty universe and otherwise flips every word and ANDs the last word with the mask (the bits above the
  last constant must stay 0: otherwise the iterator, and so `toString()`, reads `universe[universe.length]` and throws
  `ArrayIndexOutOfBoundsException: Index 7 out of bounds for length 7` for `Color`, and `size()` is too large). `complementOf(null)` -> `NullPointerException`.
- `clone()`: `EnumSet<E> copy = (EnumSet<E>) super.clone(); copy.elements = elements.clone(); return copy;` inside
  `try` with `catch (CloneNotSupportedException e) { throw new InternalError(e); }`. Without `elements.clone()` the
  copy shares the bit array and changes to one show in the other.

Element checks:

- `typeCheck(e)`: `Class<?> eClass = e.getClass(); if (eClass != elementType && eClass.getSuperclass() != elementType)
  throw new ClassCastException(eClass + " != " + elementType);`. `null` throws `NullPointerException` from
  `getClass()`. Messages print `Class.toString()`: `class Fruit != class Color`, and for a constant with a body the
  anonymous class: adding `Op.PLUS` to a `Color` set -> `class Op$1 != class Color`.
- `accepts(e)` (for `contains` and `remove`): `false` for `null` and for any object whose class is neither
  `elementType` nor a direct subclass of it (another enum, a `String`, an `Integer`): `contains` and `remove` then
  return `false`, never throw.

Single-element operations:

- `add(e)`: `typeCheck(e)`, set the bit, return whether the word changed.
- `remove(e)`: if `!accepts(e)` return `false`; clear the bit; return whether the word changed.
- `contains(e)`: `accepts(e)` and the bit is set.
- `size()`: the sum of `Long.bitCount` over all words. `isEmpty()`: every word is 0. `clear()`:
  `Arrays.fill(elements, 0L)` (keep the same array: a running iterator reads it).

Iterator (copy the JDK `JumboEnumSet` iterator exactly; fields `long unseen = elements[0]; int unseenIndex = 0;
long lastReturned = 0; int lastReturnedIndex = 0;`):

- `hasNext()`: `while (unseen == 0 && unseenIndex < elements.length - 1) unseen = elements[++unseenIndex]; return unseen != 0;`
- `next()`: `if (!hasNext()) throw new NoSuchElementException();` then `lastReturned = unseen & -unseen;
  lastReturnedIndex = unseenIndex; unseen -= lastReturned;` and return
  `(E) universe[(lastReturnedIndex << 6) + Long.numberOfTrailingZeros(lastReturned)]`.
- `remove()`: `if (lastReturned == 0) throw new IllegalStateException();` then
  `elements[lastReturnedIndex] &= ~lastReturned; lastReturned = 0;`.
- Exceptions carry no message (`getMessage()` is `null`). There is no `ConcurrentModificationException`.
- Order is ordinal order, whatever order the elements were added in.
- Snapshot rules that follow from this code and that the test checks: the current word is copied into `unseen`, so
  changes to the set inside the word being iterated are not seen: an element removed from the set after the iterator
  passed the start of its word is still returned, and an added one is not. For a set with 64 constants or fewer there
  is only one word, so the whole iteration is a snapshot taken when the iterator is created (`set.clear()` during
  iteration: `hasNext()` stays `true` and the remaining old elements are still returned; `remove()` on one of them
  changes nothing). Words after the current one are read when the iterator gets to them, so for 130 constants an
  element added in word 1 or 2 while the iterator is in word 0 is returned, and one removed there is not.

Bulk operations. `sameKind(c)` returns `(EnumSet<?>) c` when `c instanceof EnumSet` **and**
`(((EnumSet<?>) c).universe.length > 64) == (universe.length > 64)` (both are "regular" or both "jumbo" in JDK terms),
otherwise `null`. When it returns `null`, call the inherited method (`super.containsAll(c)` and so on), which works
element by element through `contains`/`add`/`remove`/the iterator and throws `NullPointerException` for `c == null`.

- `containsAll(c)`: `es == null` -> `super.containsAll(c)`. Other element type -> `es.isEmpty()`. Same type -> every bit
  of `es` is set here.
- `addAll(c)`: `es == null` -> `super.addAll(c)`. Other element type: `es.isEmpty()` -> `false`, otherwise
  `throw new ClassCastException(es.elementType + " != " + elementType)`. Same type -> OR the words, return whether any
  word changed.
- `removeAll(c)`: `es == null` -> `super.removeAll(c)`. Other element type -> `false`. Same type -> clear the bits of
  `es`, return whether any word changed.
- `retainAll(c)`: `es == null` -> `super.retainAll(c)`. Other element type -> `boolean changed = !isEmpty(); clear();
  return changed;`. Same type -> AND the words, return whether any word changed.
- `equals(o)`: `es == null` -> `super.equals(o)` (`AbstractSet`: equal to any `Set` with the same elements, such as a
  `HashSet` or `TreeSet`; never equal to a `List` or `null`). Other element type -> `isEmpty() && es.isEmpty()` (so
  `noneOf(Color.class).equals(noneOf(Fruit.class))` is `true`). Same type -> `Arrays.equals(elements, es.elements)`.

Why `sameKind` matters: the fast-path message prints the **element type** of the other set, the element-by-element path
prints the **class of the first element**. With `Op.MINUS` (class `Op$2`): `colors.addAll(EnumSet.of(Op.MINUS))` ->
`class Op != class Color` (fast path, both small), `colors.addAll(List.of(Op.MINUS))` -> `class Op$2 != class Color`
(`typeCheck`), and on a 130-constant set `huge.addAll(EnumSet.of(Op.PLUS))` -> `class Op$1 != class Huge` (the JDK
`JumboEnumSet` does not take the fast path for a `RegularEnumSet` argument).

Inherited and already correct: `hashCode()` (sum of the elements' hash codes, so equal to a `HashSet` with the same
elements; 0 when empty), `toString()` (`[RED, BLUE]`, `[]`, element `toString()`), `toArray`, `removeIf`.

## Steps

1. Read the test `javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeEnumSetTest.java`, then
   `javaw-bytecode/src/main/resources/exception-runtime/java/util/AbstractSet.java`, `AbstractCollection.java` and
   `BitSet.java` (for `clone()` and `long` word handling) in the same directory.
2. Create `javaw-bytecode/src/main/resources/exception-runtime/java/util/EnumSet.java` with the members above.
3. Register it (do not edit `BytecodeExceptionRuntime.java` by hand):
   `javaw-agent-kit/bin/kit register java/util/EnumSet.java`
4. `javaw-agent-kit/bin/kit check T37` until PASS.
5. Write `docs/bytecode-enum-set.md` (below), the report, then `javaw-agent-kit/bin/kit finish T37`.

## Hints

- The runtime `Class.getEnumConstants()` returns a fresh array of the constants (or `null` for a class that is not an
  enum, including the anonymous class of a constant with a body), and `Enum.getDeclaringClass()` returns the enum class
  for both kinds of constant. `Class.toString()` prints `class Name`. `Long.bitCount`, `Long.numberOfTrailingZeros`,
  `Arrays.fill(long[], long)`, `Arrays.equals(long[], long[])` and `InternalError(Throwable)` exist in the runtime.
- Write `universe` as `Enum<?>[]` and assign `elementType.getEnumConstants()` to it directly; cast with `(E)` only in
  `Itr.next()`.
- Compare classes with `!=` (`eClass != elementType`), never with `equals` or by name.
- `-1L >>> -universe.length` is the mask of the last word. Guard `universe.length == 0` in `fillAll()` and
  `complement()`: for an empty enum the mask would be `-1L` (a shift by 0), so `allOf(Empty.class)` and
  `complementOf(noneOf(Empty.class))` would hold 64 bits; printing them then throws
  `ArrayIndexOutOfBoundsException: Index 0 out of bounds for length 0` in `emptyEnum`.
- `addRange` can be a plain loop `for (int i = from.ordinal(); i <= to.ordinal(); i++) elements[i >>> 6] |= 1L << i;`.
  Use `1L << i`, not `1 << i` (an `int` shift wraps at 32, so `W33` would set bit 1 instead of bit 33).
- `Itr` must be a non-static inner class that reads the outer `elements` field every time (`elements[++unseenIndex]`,
  `elements[lastReturnedIndex] &= ...`). Do not copy the whole array when the iterator is created: the 130-constant
  fixture sees later words change.
- Do not add a `size` field: count with `Long.bitCount`, so `remove()` on an element that is already gone cannot make
  the count wrong.
- `hashCode()` of an enum constant is an identity hash, so the fixtures only compare hash codes with each other and
  never print one (except `0` for an empty set).
- Do not use `switch` on enum constants (RULES.md). Write `c instanceof EnumSet` followed by a cast `(EnumSet<?>) c`, as
  in `sameKind`.
- The Wasm runner stops at the **first** failing fixture of each `@Test` method, so after a fix another fixture of the
  same group may fail next. That is progress.
- Calibration mutations, each caught by the fixtures named: no `elements.clone()` in `clone()` (`copyOfVariants`,
  `constantBodies`, `textAndCopies`, `wideUniverse`, `hugeUniverse`, `hugeOperations`: `complementOf` goes through `copyOf`, so the original set flips
  too); no mask in `complement()` (`rangeAndComplement`, `constantBodies`, `textAndCopies`, `hugeUniverse`); no
  `universe.length == 0` guard in `fillAll()` (`emptyEnum`); `getClass()` instead of `getDeclaringClass()` in `of`
  (`constantBodies`); `sameKind` without the size test (`hugeIteration`); `1 << i` in `addRange` (`wideUniverse`, `hugeUniverse`,
  `hugeOperations`);
  an iterator over a copy of the whole array (`hugeIteration`).
- Calibration: the reference passed kit check after 1 run (about 260 new lines, all in EnumSet.java).

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
factories, elementTypes, iteration, bulkOperations, equalityTextAndCopies, largeUniverses, leavesOutSerialization: if FIX THE RUNTIME says `link the symbolic class/interface java/util/EnumSet`, `link a complete acyclic type hierarchy: java/util/EnumSet`, `link the descriptor class/interface java/util/EnumSet` or a static/instance method of java/util/EnumSet, the class is missing or not registered: run `javaw-agent-kit/bin/kit register java/util/EnumSet.java` (never edit BytecodeExceptionRuntime.java by hand) and add every member listed under Members to implement with the exact signature. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
factoryBasics, emptyEnum: allOf must set exactly universe.length bits: fill every word with -1L and AND the last word with `-1L >>> -universe.length`; do nothing at all when universe.length == 0 (Empty must stay [] with size 0). An `ArrayIndexOutOfBoundsException: Index 0 out of bounds for length 0` while printing an Empty set means the universe.length == 0 guard is missing in fillAll() or complement().
ofOverloads, constantBodies: every of(...) overload uses `noneOf(<first argument>.getDeclaringClass())` (e, e1 or first) and range uses `noneOf(from.getDeclaringClass())` (never getClass(), which is Op$1 for a constant with a body); then add each argument in order; duplicates are ignored.
rangeAndComplement, hugeUniverse: range checks `from.compareTo(to) > 0` first and throws `new IllegalArgumentException(from + " > " + to)` (toString, so `lvl-2 > lvl-0`); addRange sets ordinals from..to inclusive across words; complement flips every word and then ANDs the last word with `-1L >>> -universe.length`. An ArrayIndexOutOfBoundsException printing a set means the last-word mask is missing.
copyOfVariants, textAndCopies, hugeOperations: clone() must copy the bit array (`copy.elements = elements.clone();` after super.clone()); copyOf(EnumSet) and copyOf(Collection) of an EnumSet return clone(); copyOf of any other empty collection throws `new IllegalArgumentException("Collection is empty")`.
factoryFailures: noneOf throws `new ClassCastException(elementType + " not an enum")` when getEnumConstants() returns null (`class java.lang.String not an enum`, `class Op$1 not an enum`); null arguments must throw NullPointerException; a mixed raw list or raw of(...) throws typeCheck's `class Fruit != class Color`.
addRemoveContains: add calls typeCheck, which throws `new ClassCastException(eClass + " != " + elementType)` using `e.getClass()` (so `class Op$1 != class Color`) unless eClass == elementType or eClass.getSuperclass() == elementType; contains and remove return false for null, Strings, Integers and other enums instead of throwing.
sizeAndClear, iteratorOrder: size() is the sum of Long.bitCount over the words, clear() is Arrays.fill(elements, 0L), and iteration goes in ordinal order (lowest set bit first: `unseen & -unseen` and Long.numberOfTrailingZeros).
iteratorRemove: Itr.remove() throws `new IllegalStateException()` (no message) when lastReturned == 0, i.e. before the first next() and twice in a row; next() past the end throws `new NoSuchElementException()` (no message); removeIf works through Itr.remove().
iteratorSnapshot, hugeIteration: copy the JumboEnumSet iterator exactly: `unseen = elements[0]` when created, hasNext loads `elements[++unseenIndex]` only when unseen is 0, next takes `unseen & -unseen`, remove does `elements[lastReturnedIndex] &= ~lastReturned`. Never copy the whole array, never check for concurrent modification.
addAllCases, removeAllCases, retainAllCases, containsAllCases: of(...) and range must use getDeclaringClass(), never getClass() (otherwise `class Op$1 not an enum`); for another EnumSet of the same kind use the word fast path; a different element type gives addAll -> false if it is empty else `new ClassCastException(es.elementType + " != " + elementType)` (`class Op != class Color`), removeAll -> false, retainAll -> clear() and return whether it was non-empty, containsAll -> es.isEmpty(); any other collection goes to the super method (element by element, NullPointerException for null).
hugeIteration: `class Op$1 != class Huge` comes from typeCheck: sameKind(c) must return null when exactly one of the two universes has more than 64 constants, so addAll falls back to super.addAll(c).
equalsAndHash: of(...) and range must use getDeclaringClass(), never getClass() (otherwise `class Op$1 not an enum`). equals uses sameKind: null -> super.equals(o) (HashSet/TreeSet with the same elements are equal, lists and null are not); different element type -> both empty; same type -> Arrays.equals of the words. Do not override hashCode.
wideUniverse: 64 constants still use one word: the mask `-1L >>> -64` is -1L (all 64 bits), and bits above 31 need `1L << ordinal` (a long shift), never `1 << ordinal`.
leavesOutSerialization: this fails when EnumSet.writeReplace() has become linked. Do not add writeReplace, readObject or a SerializationProxy.
```

## Docs

Create `docs/bytecode-enum-set.md` with this skeleton (fill in the brackets):

    # Bytecode EnumSet

    Revision `enum-set-vN` links `java.util.EnumSet` in the bundled exception runtime.

    Contract:

    - <factories (noneOf/allOf/of/range/complementOf/copyOf) with the `not an enum`, `Collection is empty` and
      `from > to` messages; the long[] bit vector for any number of constants; typeCheck and its message; the
      JumboEnumSet-style iterator and its snapshot rules; bulk fast paths only between sets of the same kind; equals;
      clone copies the bits>

    Known differences: `EnumSet` is one concrete class here (the JDK has an abstract sealed `EnumSet` with hidden
    `RegularEnumSet` / `JumboEnumSet` subclasses), so `getClass()` differs. Serialization (`writeReplace`,
    `SerializationProxy`) is not linked.

    Evidence: `BytecodeEnumSetTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two fresh
    instances). `EnumSet.writeReplace()` is rejected before WAT generation.
