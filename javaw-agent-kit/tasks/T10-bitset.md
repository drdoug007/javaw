# T10 BitSet: single-bit and search operations

```kit
id: T10
slug: bitset
title: link java.util.BitSet single-bit, range and search operations
tests: BytecodeBitSetTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/BitSet.java
allow: docs/bytecode-bitset.md
```

## Goal

Add `java.util.BitSet` to the bundled runtime: single bits, ranges, searches, `length`/`size`/`cardinality` and
`toString`. H2 and jackson-core/databind use it (`CoreJdkCoverage` lists `BitSet`; it is a missing owner today).
Bulk operations, `equals`/`hashCode`/`clone` and conversions are task T11 and must NOT be added now.

## Members to implement

```
public class BitSet implements Cloneable, java.io.Serializable
public BitSet()                       // one word (64 bits)
public BitSet(int nbits)              // ceil(nbits/64) words
public boolean get(int bitIndex)
public void set(int bitIndex)
public void set(int bitIndex, boolean value)
public void set(int fromIndex, int toIndex)     // bits fromIndex..toIndex-1
public void clear(int bitIndex)
public void clear(int fromIndex, int toIndex)
public void clear()
public void flip(int bitIndex)
public int nextSetBit(int fromIndex)
public int nextClearBit(int fromIndex)
public int previousSetBit(int fromIndex)
public int previousClearBit(int fromIndex)
public int length()
public int size()
public boolean isEmpty()
public int cardinality()
public String toString()
```

## Members to leave out

`get(int,int)` and `stream()` (the test checks both are rejected). Also leave out everything T11 adds: `or`,
`and`, `xor`, `andNot`, `intersects`, `equals`, `hashCode`, `clone`, `toByteArray`, `toLongArray`, `valueOf`.
Do not add `flip(int,int)` or `set(int,int,boolean)` either.

## JDK behaviour to match

- Storage: `long[] words`, bit `i` is bit `i % 64` of `words[i / 64]` (`i >> 6`), plus `int wordsInUse` = number
  of words up to and including the last non-zero word. `length()`, `isEmpty()`, searches and `toString` use
  `wordsInUse`, never `words.length`.
- `size()` = `64 * words.length`. `new BitSet()` has 1 word (size 64). `new BitSet(n)` has `(n + 63) / 64`
  words (size 0 for n=0, 64 for 1..64, 128 for 65..128, 192 for 130).
- Growth (only `set`, `set(int,boolean)` with true, `set(int,int)` and `flip` grow): when word `w` is needed and
  `words.length <= w`, the new length is `max(2 * words.length, w + 1)`. Examples: `new BitSet()` then
  `set(100)` -> size 128; `set(1000)` -> 1024; `new BitSet(0)` then `set(5)` -> 64; `new BitSet(130)` then
  `set(200)` -> 384. `set(10,300)` on a new BitSet needs word 4 -> size 320.
- `clear(...)`, `set(i,false)` and `get(...)` never grow: `get` past the end returns false, `clear` past the end
  does nothing. `clear()` zeroes the bits but keeps `size()`.
- `set(i,i)` and `clear(i,i)` (empty range) do nothing and do not grow.
- After clearing the highest bit, `wordsInUse` must shrink (walk down while `words[wordsInUse-1]==0`), so
  `length()` and `isEmpty()` are right.
- `length()` = index of the highest set bit + 1, or 0. `cardinality()` = sum of `Long.bitCount(word)`.
- `nextSetBit(from)`: first set bit `>= from`, or -1. `nextClearBit(from)`: first clear bit `>= from`; a
  `from` past the in-use words returns `from` itself.
- `previousSetBit(from)`: last set bit `<= from`, or -1. `previousClearBit(from)`: last clear bit `<= from`, or
  -1; a `from` past the in-use words returns `from`. Both return -1 for `from == -1`.
- `toString()`: `"{}"`, `"{5}"`, `"{1, 3, 5}"` (comma and one space).

Exceptions (type and message are checked; every check happens before anything changes):

| Call | Exception | Message |
|---|---|---|
| `new BitSet(-1)` | `NegativeArraySizeException` | `nbits < 0: -1` |
| `get/set/clear/flip` with a negative index | `IndexOutOfBoundsException` | `bitIndex < 0: <bitIndex>`, e.g. `get(-1)` -> `bitIndex < 0: -1`, `set(-2,true)` -> `bitIndex < 0: -2`, `flip(-3)` -> `bitIndex < 0: -3` |
| range with `fromIndex < 0` | `IndexOutOfBoundsException` | `fromIndex < 0: -1` |
| range with `toIndex < 0` | `IndexOutOfBoundsException` | `toIndex < 0: -2` |
| range with `fromIndex > toIndex` | `IndexOutOfBoundsException` | `fromIndex: 5 > toIndex: 2` |
| `nextSetBit(-1)`, `nextClearBit(-1)` | `IndexOutOfBoundsException` | `fromIndex < 0: -1` |
| `previousSetBit(-2)`, `previousClearBit(-2)` | `IndexOutOfBoundsException` | `fromIndex < -1: -2` |

Range checks run in this order: fromIndex < 0, then toIndex < 0, then fromIndex > toIndex. So `set(-1,-5)`
reports `fromIndex < 0: -1`.

## Steps

1. Create `javaw-bytecode/src/main/resources/exception-runtime/java/util/BitSet.java` (package `java.util`).
2. Fields `private long[] words; private int wordsInUse;`. Private helpers: `wordIndex(int bit)` = `bit >> 6`,
   `ensureWords(int count)` (growth rule above), `recalculateWordsInUse()`, `checkRange(int from,int to)`.
3. Write the members above. Do the single-bit ones first, then ranges, then searches, then `toString`.
4. Register it: in `javaw-bytecode/src/main/java/one/dastec/javaw/bytecode/BytecodeExceptionRuntime.java`, find
   the `List.of(...)` line containing `"LinkedList"` and add `"BitSet"` right after `"LinkedList"`, i.e.
   `..."ArrayList","LinkedList","BitSet","Collections",...`.
5. `javaw-agent-kit/bin/kit check T10` until PASS. Then write the docs file and the report, and `kit finish T10`.

## Hints

- Masks: `1L << bit` uses only the low 6 bits of `bit` in Java, so `1L << 70 == 1L << 6`. For ranges use
  `-1L << from` (bits from..63 of the first word) and `-1L >>> -to` (bits 0..(to-1)%64 of the last word, `to`
  exclusive). When first and last word are the same, AND the two masks.
- In range code compute `endWord = wordIndex(to - 1)` (to is exclusive). `set(64,128)` touches only word 1.
- `nextSetBit`: mask the first word with `words[w] & (-1L << from)`, then scan up; answer is
  `w * 64 + Long.numberOfTrailingZeros(word)`. `nextClearBit`: same with `~words[w]`.
- `previousSetBit`: mask with `words[w] & (-1L >>> -(from + 1))`, scan down, answer is
  `(w + 1) * 64 - 1 - Long.numberOfLeadingZeros(word)`. If `w >= wordsInUse`, start from the last in-use word
  (set) or return `from` (clear).
- `nextSetBit` past `wordsInUse` returns -1 (do not read beyond `wordsInUse`). `nextClearBit` that runs off
  the last in-use word returns `wordsInUse * 64`.
- `clear(from,to)`: return early if `wordIndex(from) >= wordsInUse`; if `to` reaches past the in-use words,
  clamp `to = length()` first and set `endWord = wordsInUse - 1` (or compute `endWord` only after clamping).
  Never index past `wordsInUse - 1`. Then recalculate `wordsInUse`.
- `flip` and `clear` must recalculate `wordsInUse` (a flip can clear the top bit). `set` only raises it.
- Grow with `Arrays.copyOf(words, newLength)` (exists for `long[]`).
- `set(i,false)` must call `clear(i)`, which never grows: in `noGrowth`, `set(700,false)` leaves `size()` at 64.
  In `setWithBoolean`, `set(70,true)` grows it to 128.
- `Long.bitCount`, `Long.numberOfTrailingZeros`, `Long.numberOfLeadingZeros` and `Math.max` exist in the runtime.
  Use a plain loop and `StringBuilder` for `toString`.
- Do not add `equals`/`hashCode`/`clone` yet; T11 tests them. Implementing `Cloneable` with no `clone` is fine.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
matchesSingleBitOperations, matchesRangeOperations, matchesSearches, matchesSizes, matchesFailures, rejectsUnlinkedMembers: if the failure says "unsupported ... link the symbolic class/interface java/util/BitSet" or "link the descriptor class/interface java/util/BitSet", BitSet is not registered: in BytecodeExceptionRuntime.java (do not edit that file by hand: run `javaw-agent-kit/bin/kit register <path under exception-runtime>` for each new file, which makes exactly this edit) add "BitSet" right after "LinkedList" in the List.of(...) of java/util names. "unsupported dynamic call java/util/BitSet.NAME(DESC); link a matching instance method" (or "link a matching special declaration" for <init>) means that member or constructor is missing from BitSet.java or its signature differs from the spec's member list. If rejectsUnlinkedMembers fails, delete get(int,int) and stream() from BitSet.java.
basics, emptyValue, wordBoundaries: if bits 31/63 come out wrong or extra bits appear, you used an int shift: always write `1L << bitIndex`, never `1 << bitIndex`, in get/set/clear/flip. get must return false when wordIndex(i) >= wordsInUse (emptyValue calls get(1000)). If the set looks empty after set(i), set(int) never raised wordsInUse: it must make wordsInUse at least wordIndex(i) + 1. length() is `64 * (wordsInUse - 1) + 64 - Long.numberOfLeadingZeros(words[wordsInUse - 1])`, or 0 when wordsInUse == 0 (wordBoundaries expects length 193). toString prints `{1, 3, 5}` (", " between entries, `{}` when empty).
clearBits, flipBits, clearAll: if length()/isEmpty()/toString still count a cleared top bit, wordsInUse never shrank. clear(int) returns at once when wordIndex(i) >= wordsInUse, otherwise clears the bit and ends with recalculateWordsInUse() (`while (wordsInUse > 0 && words[wordsInUse - 1] == 0) wordsInUse--;`); flip(int) also ends with recalculateWordsInUse(). clear() zeroes the words but keeps words.length (clearAll expects `{}|0|true|320|320|-1`).
initialSizes, growth, noGrowth, emptyRanges, setWithBoolean: a wrong size() comes from the constructor or the grow helper, not from size(). initialSizes expects `64,0,64,64,128,128,192`, so use `new long[(nbits + 63) / 64]`. growth expects `128,1024,64,384,320,192`, so grow to `Math.max(2 * words.length, wordIndex + 1)`, never to the exact size. noGrowth expects `64,0,true`: get, clear(...) and set(i,false) (which must just call clear(i)) never grow. set(i,i) and clear(i,i) return before growing (emptyRanges expects `{}|0|64`).
setRange, setRangeAcrossWords, setRangeWholeWord, clearRange, clearRangeInsideWord, clearRangePastEnd, noGrowth: the range code in set(int,int)/clear(int,int) is wrong. toIndex is exclusive, so `endWord = wordIndex(toIndex - 1)` (set(64,128) touches only word 1 and expects `64|128|falsetruetruefalse|128`). First-word mask `-1L << fromIndex`, last-word mask `-1L >>> -toIndex`, AND them when startWord == endWord; words in between become -1L (set) or 0 (clear). A Wasm exception in clearRangePastEnd/noGrowth means clear(int,int) indexed past the in-use words: first `if (wordIndex(fromIndex) >= wordsInUse) return;`, then if wordIndex(toIndex - 1) >= wordsInUse set `toIndex = length()` and `endWord = wordsInUse - 1` BEFORE building the masks, and finish with recalculateWordsInUse() (clearRangePastEnd expects `{3}|4|{}|0|true`).
forwardSetBits, forwardClearBits, fullWordClearBit, emptySearches, presizedIsEmpty: nextSetBit/nextClearBit must only read words[0..wordsInUse-1]. nextSetBit returns -1 when it runs out. nextClearBit returns fromIndex itself when wordIndex(fromIndex) >= wordsInUse, and returns `wordsInUse * 64` when the scan runs off the last in-use word (sample expects `1,6,66,128,201,201,9999`; set(0,128) then nextClearBit(70) is 128). Constructors leave wordsInUse = 0: new BitSet(500) is isEmpty() true with nextClearBit(0) = 0.
backwardSetBits, backwardClearBits, fullWordClearBit, emptySearches: in previousSetBit/previousClearBit, fromIndex == -1 returns -1. Mask the first word with `-1L >>> -(fromIndex + 1)`, scan DOWN, and answer `(w + 1) * 64 - 1 - Long.numberOfLeadingZeros(word)`. If wordIndex(fromIndex) >= wordsInUse, previousSetBit returns `length() - 1` and previousClearBit returns fromIndex (sample previousClearBit expects `-1,4,62,66,199,9999,-1`).
negativeSize, negativeBitIndex, badRanges, badSearches, failedCallsChangeNothing: the exception type or message is wrong, or a check ran after the state changed. Put every check first, before any sizing or growth. new BitSet(-1) needs an explicit `if (nbits < 0) throw new NegativeArraySizeException("nbits < 0: " + nbits);` because (-1 + 63) / 64 is 0 and would not throw. Check ranges in order fromIndex < 0, then toIndex < 0, then fromIndex > toIndex ("fromIndex: 5 > toIndex: 2"), all IndexOutOfBoundsException. nextSetBit/nextClearBit(-1) say "fromIndex < 0: -1", but previousSetBit/previousClearBit(-2) say "fromIndex < -1: -2".
```

## Docs

Create `docs/bytecode-bitset.md` with this skeleton (fill in the brackets):

    # Bytecode BitSet

    Revision `bitset-vN` links java.util.BitSet (single bits, ranges, searches) in the bundled exception runtime.

    Contract:

    - <behaviour: storage, size/growth rule, search results, toString format, exception messages>

    Evidence: `BytecodeBitSetTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
    fresh instances). `BitSet.get(int,int)` and `BitSet.stream()` are rejected before WAT generation.
