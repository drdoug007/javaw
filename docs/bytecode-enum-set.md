# Bytecode EnumSet

Revision `enum-set-v299` links `java.util.EnumSet` in the bundled exception runtime.

Contract:

- Storage: one concrete class (the JDK's abstract sealed `EnumSet` with hidden `RegularEnumSet`/`JumboEnumSet`
  is collapsed) holding `final Class<E> elementType`, the package-private `final Enum<?>[] universe` from
  `elementType.getEnumConstants()`, and `private long[] elements` with one word per 64 constants (at least one
  word), so a single `long[]` bit vector works for any number of constants. Constant `o` is bit `1L << o`
  (Java masks the shift to `o & 63`) of word `o >>> 6`. `size()` sums `Long.bitCount` over the words; `isEmpty()`
  tests that every word is 0; `clear()` is `Arrays.fill(elements, 0L)` on the same array. Keeping the array
  means a running iterator over more than 64 constants sees the cleared later words, but the word it is in
  stays a snapshot (and with 64 or fewer constants the whole iteration does): after `allOf`, `iterator()`,
  `next()`, `clear()`, a 3-constant set still returns `B`, `C`, and a 70-constant set still returns `H1` ..
  `H63` and nothing from word 1.
- Factories: `noneOf(t)` takes `t.getEnumConstants()` and throws
  `ClassCastException(elementType + " not an enum")` when it is `null` (`class java.lang.String not an enum`;
  a constant-with-body class gives `class Op$1 not an enum`), with `NullPointerException` for a `null` class.
  `allOf(t)` is `noneOf` then `fillAll()` (every word `-1L`, the last word `-1L >>> -universe.length`, and no
  change at all when the enum has no constants). `of(e)` .. `of(e1..e5)` and the varargs `of(first, rest...)`
  use `noneOf(first.getDeclaringClass())` — `getDeclaringClass()`, never `getClass()`, which for a constant with
  a body is the anonymous class `Op$1` — then `add` each argument in order (duplicates collapse; a `null`
  argument is `NullPointerException`; a mixed-type raw call is `typeCheck`'s `ClassCastException`).
  `range(from, to)` first throws `IllegalArgumentException(from + " > " + to)` when `from.compareTo(to) > 0`
  (string `+`, so a custom `toString` prints, e.g. `lvl-2 > lvl-0`), then `addRange` sets every ordinal from
  `from` to `to` inclusive across word boundaries (`1L << i`, a long shift). `copyOf(EnumSet s)` is `s.clone()`
  (`null` is `NullPointerException`); `copyOf(Collection c)` clones an `EnumSet` argument, throws
  `IllegalArgumentException("Collection is empty")` for any other empty collection, and otherwise starts from
  `of(first)` and adds the rest. `complementOf(s)` clones and flips every word, then ANDs the last word with
  the mask `-1L >>> -universe.length` (bits above the last constant stay 0), doing nothing for an empty enum.
- Element checks: `typeCheck(e)` (used by `add`) throws
  `ClassCastException(eClass + " != " + elementType)` — `class Fruit != class Color`, `class Op$1 != class
  Color` — unless `e.getClass()` is `elementType` or a direct subclass of it, and `null` is
  `NullPointerException` from `getClass()`. `accepts(e)` (used by `contains` and `remove`) returns `false` for
  `null` and for any class that is neither `elementType` nor a direct subclass, so `contains`/`remove` never
  throw for a foreign element (another enum, a `String`, an `Integer`). Classes are compared with `!=`, never
  by name or `equals`.
- Iterator: the JDK `JumboEnumSet` iterator copied exactly (`unseen = elements[0]`, `unseenIndex`,
  `lastReturned`, `lastReturnedIndex`), which for a one-word set behaves exactly like the `RegularEnumSet`
  iterator. `hasNext()` loads `elements[++unseenIndex]` only while `unseen == 0`; `next()` takes the lowest set
  bit (`unseen & -unseen`, `Long.numberOfTrailingZeros`) in ordinal order; `next()` past the end throws
  `NoSuchElementException` and `remove()` before a `next()` or twice in a row throws `IllegalStateException`,
  both with no message, and there is no `ConcurrentModificationException`. Snapshot rules: the current word is
  copied into `unseen`, so changes inside the word being iterated are not seen (a removed element after the
  iterator passed the word start is still returned, an added one is not; with 64 or fewer constants the whole
  iteration is a snapshot from creation), while later words are read when the iterator reaches them, so for a
  130-constant set an element added in word 1 or 2 while the iterator is in word 0 is returned.
- Bulk operations: `sameKind(c)` returns the other `EnumSet` only when `c instanceof EnumSet` **and** both
  universes are on the same side of 64 constants (both "regular" or both "jumbo"); otherwise the inherited
  element-by-element method runs (and throws `NullPointerException` for `c == null`). Between sets of the same
  kind: `containsAll` is true iff every bit of the other is set here (other element type gives
  `es.isEmpty()`); `addAll` ORs the words and returns whether any word changed; `removeAll` clears the other's
  bits; `retainAll` ANDs the words; for another element type `addAll` returns `false` when the other is empty
  and otherwise throws `ClassCastException(es.elementType + " != " + elementType)` (the fast path prints the
  **element type**, `class Op != class Color`, while the element-by-element path prints the first element's
  class, `class Op$1 != class Color`), `removeAll` returns `false`, and `retainAll` clears and returns whether
  it was non-empty. The 130-constant set never takes the fast path for a small set, so
  `huge.addAll(EnumSet.of(Op.PLUS))` falls back to `typeCheck`'s `class Op$1 != class Huge`.
- Equality and copying: `equals(o)` uses `sameKind` — `null` or a non-set falls to `AbstractSet.equals`
  (equal to any `Set` with the same elements, such as a `HashSet` or `TreeSet`; never to a `List` or `null`),
  another element type is equal iff both are empty, and the same type compares `Arrays.equals` of the words.
  `hashCode`, `toString`, `toArray`, `toArray(T[])` and `removeIf` are inherited unchanged, so a set hashes and
  prints like the JDK. `clone()` is `super.clone()` plus `copy.elements = elements.clone()` (without the
  `elements.clone()` the copy shares the bit array and changes to one show in the other), with
  `CloneNotSupportedException` wrapped in `InternalError`.

Known differences: `EnumSet` is one concrete class here (the JDK has an abstract sealed `EnumSet` with hidden
`RegularEnumSet` / `JumboEnumSet` subclasses), so `getClass()` differs. Serialization (`writeReplace`,
`SerializationProxy`) is not linked.

Evidence: `BytecodeEnumSetTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
fresh instances). `EnumSet.writeReplace()` is rejected before WAT generation.
