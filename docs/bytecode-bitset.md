# Bytecode BitSet

Revision `bitset-v271` links java.util.BitSet (single bits, ranges, searches) in the bundled exception runtime.

Contract:

- Storage is `long[] words` (bit `i` is bit `i & 63` of `words[i >> 6]`) plus `int wordsInUse`, the number
  of words up to and including the last non-zero word. `length()`, `isEmpty()`, `cardinality()` and the
  searches read only words `[0..wordsInUse-1]`; constructors leave `wordsInUse = 0`, so `new BitSet(500)`
  is empty at size 512.
- `size()` is `64 * words.length`: `new BitSet()` has 1 word (64), `new BitSet(n)` has ceil(n / 64)
  words, computed as `wordIndex(n - 1) + 1` so it cannot overflow (size 0 for 0, 64 for 1..64, 128 for
  65..128, 192 for 130). Growth happens only in `set(int)`,
  `set(int, boolean)` with true, `set(int, int)` and `flip`: when word `w` is needed and
  `words.length <= w`, the array grows to `max(2 * words.length, w + 1)` via `Arrays.copyOf`
  (`set(100)` on a fresh set gives 128, `set(1000)` gives 1024, `new BitSet(130).set(200)` gives 384,
  `set(10, 300)` gives 320). `get`, `clear(...)`, `set(i, false)` and the searches never grow;
  `set(i, i)` and `clear(i, i)` no-op; `clear()` zeroes the words but keeps `size()`.
- Range operations take an exclusive `toIndex` and check, in order, `fromIndex < 0`
  ("fromIndex < 0: n"), `toIndex < 0` ("toIndex < 0: n"), `fromIndex > toIndex`
  ("fromIndex: n > toIndex: m"), all `IndexOutOfBoundsException`, before any sizing or mutation.
  `set` masks the first word with `-1L << fromIndex`, the last with `-1L >>> -toIndex` (AND-joined when
  they are the same word) and fills the words between with `-1L`; `clear` does the complement and clamps
  `toIndex` to `length()` when it runs past the in-use words, then shrinks `wordsInUse` so a cleared top
  bit drops `length()` and can make the set `isEmpty()`. `flip(i)` is implemented as `get(i) ? clear(i) : set(i)`
  (identical observable state to the JDK's XOR, including growth).
- Searches: `nextSetBit` returns the first set bit `>= from` or -1; `nextClearBit` returns the first clear
  bit `>= from`, `from` itself when `from` is past the in-use words, and `wordsInUse * 64` when the scan
  runs off the last in-use word. `previousSetBit` returns the last set bit `<= from` or -1
  (`length() - 1` when `from` is past the in-use words); `previousClearBit` returns the last clear bit
  `<= from` or -1 (`from` when past the in-use words). Both return -1 for `from == -1`; negative
  `fromIndex` throws (`fromIndex < 0: n` for the forward searches, `fromIndex < -1: n` for the backward).
  `new BitSet(-1)` throws `NegativeArraySizeException("nbits < 0: n")`.
- `length()` is the index of the highest set bit plus 1 (0 when empty); `cardinality()` sums
  `Long.bitCount` over the in-use words; `toString()` prints `{}`, `{5}` or `{1, 3, 5}` (comma and one
  space) by walking `nextSetBit`, stopping after bit `Integer.MAX_VALUE` instead of overflowing.
- There is no `sizeIsSticky` flag yet. It is unobservable until T11's `clone`, where the JDK trims the
  original's array for sets created with `BitSet()` (the runtime will not).
- The class implements `Cloneable` and `java.io.Serializable` with no `clone` yet; `get(int, int)`,
  `stream()` and the T11 bulk/equality/conversion members are absent and rejected before WAT generation.

Evidence: `BytecodeBitSetTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
fresh instances). `BitSet.get(int,int)` and `BitSet.stream()` are rejected before WAT generation.
