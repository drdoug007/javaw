# Bytecode BitSet bulk operations

Revision `bitset-bulk-v272` links the java.util.BitSet logical operations, equality, clone and array
conversions in the bundled exception runtime.

Contract:

- `and`, `or`, `xor`, `andNot`, `intersects` all work on the in-use words, never on `words.length`.
  `and` zeroes `this`'s words at and above `set.wordsInUse` (shrinking `wordsInUse`), ANDs the common
  words and recalculates; never grows, so `size()` is unchanged. `andNot` ANDs the common words with
  `~set.words[i]` and recalculates; never grows. `or` and `xor` compute `common = min(wordsInUse,
  set.wordsInUse)` before any growth, grow with T10's `max(2 * words.length, set.wordsInUse)` rule when
  `set.wordsInUse > wordsInUse` (then `wordsInUse = set.wordsInUse`), OR/XOR the common words and
  `System.arraycopy` the extra words of `set`; `xor` recalculates afterwards. `intersects` is true iff
  some common word's AND is non-zero. `and` and `or` no-op when `set == this`, but `a.xor(a)` and
  `a.andNot(a)` clear `a`. A `null` argument throws `NullPointerException` by reading `set.wordsInUse`.
- `equals(obj)` is false unless `obj instanceof BitSet`, true for the same object, otherwise true iff
  `wordsInUse` is equal and words `[0..wordsInUse-1]` are equal. The array size does not matter:
  `new BitSet(1000)` with bits {3, 64} equals `new BitSet()` with bits {3, 64}. `hashCode` is
  `long h = 1234; for (i = wordsInUse; --i >= 0;) h ^= words[i] * (i + 1); return (int) ((h >> 32) ^ h);`
  (empty set hashes to 1234), so equal sets always hash equal.
- `clone` first trims the ORIGINAL's array to `wordsInUse` words unless `sizeIsSticky`, then
  `super.clone()` inside `try { ... } catch (CloneNotSupportedException e) { throw new InternalError(e); }`
  and gives the copy its own array (`copy.words = words.clone()`), so changing the copy never changes
  the original. `sizeIsSticky` starts true only in `BitSet(int nbits)` and is cleared whenever the set
  grows through `ensureWords`; it is `transient` and unobservable outside `clone`. So
  `new BitSet().clone()` leaves both at size 0 and a set grown past its constructor size shrinks through
  a clone, while `new BitSet(2000)` (unused) keeps size 2048.
- `toLongArray` returns a NEW array of exactly `wordsInUse` longs (`Arrays.copyOf`), so a trimmed set or
  a sized empty set gives `[]`. `toByteArray` returns little-endian bytes of length
  `(length() + 7) / 8`, byte `k` = `(byte) (words[k / 8] >>> (8 * (k % 8)))`; empty set gives
  `new byte[0]`. Both are copies: mutating the returned arrays does not affect the set.
- `valueOf(long[])` drops trailing zero longs and copies the first `n` into a new array that becomes
  `words` with `wordsInUse = n` (`{5,0,0}` has size 64, `new long[0]` size 0). `valueOf(byte[])` drops
  trailing zero bytes (`n`), allocates `new long[(n + 7) / 8]` and ORs byte `k` in as
  `(bytes[k] & 0xffL) << (8 * (k % 8))` so negative bytes do not sign-extend (`{-1}` is bits 0..7).
  `valueOf(null)` on either overload throws `NullPointerException` by reading the argument's length.

The runtime leaves out `valueOf(java.nio.ByteBuffer)`, `valueOf(java.nio.LongBuffer)`, `get(int, int)`
and `stream()`: the buffer classes are not in the runtime, and the buffer overloads, `get(int, int)` and
`stream()` are rejected before WAT generation.

Evidence: `BytecodeBitSetBulkTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders,
two fresh instances). `BitSet.valueOf(ByteBuffer)` and `BitSet.valueOf(LongBuffer)` are rejected before
WAT generation.
