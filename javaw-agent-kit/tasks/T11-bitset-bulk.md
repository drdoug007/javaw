# T11 BitSet: bulk operations, equality and conversions

```kit
id: T11
slug: bitset-bulk
title: link java.util.BitSet bulk operations, equality, clone and array conversions
tests: BytecodeBitSetBulkTest
depends: T10
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/BitSet.java
allow: docs/bytecode-bitset-bulk.md
```

## Goal

Finish the `java.util.BitSet` subset H2 needs: the logical operations, `equals`/`hashCode`, `clone` and the
`long[]`/`byte[]` conversions. T10 already added `BitSet.java` with single-bit, range and search operations;
you only ADD members to that file. Do not change how T10's members behave (`BytecodeBitSetTest` must still pass).

## Members to implement

```
public void and(BitSet set)
public void or(BitSet set)
public void xor(BitSet set)
public void andNot(BitSet set)
public boolean intersects(BitSet set)
public boolean equals(Object obj)
public int hashCode()
public Object clone()
public byte[] toByteArray()
public long[] toLongArray()
public static BitSet valueOf(long[] longs)
public static BitSet valueOf(byte[] bytes)
```

## Members to leave out

`valueOf(java.nio.ByteBuffer)` and `valueOf(java.nio.LongBuffer)`: the buffer classes are not in the runtime
(the test checks both are rejected). Still leave out `get(int,int)` and `stream()` (T10's test rejects them).

## JDK behaviour to match

All of these work on the in-use words (`wordsInUse` from T10), never on `words.length`.

- `and(set)`: if `set == this` do nothing. Zero this set's words at and above `set.wordsInUse`, AND the rest,
  then recalculate `wordsInUse`. Never grows; `size()` stays the same.
- `or(set)`: if `set == this` do nothing. If `set.wordsInUse > wordsInUse`, grow with the SAME rule as T10
  (`new length = max(2 * words.length, set.wordsInUse)` when `words.length < set.wordsInUse`) and set
  `wordsInUse = set.wordsInUse`. OR the common words, copy the extra words of `set`. Example: `new BitSet()`
  OR a set whose highest bit is 130 -> size 192. `new BitSet(0).or(new BitSet())` keeps size 0.
- `xor(set)`: like `or` (same growth), XOR instead, then recalculate `wordsInUse`. No `this` shortcut:
  `a.xor(a)` clears `a`.
- `andNot(set)`: clear in this set every bit set in `set`, over the common words; recalculate. Never grows.
  `a.andNot(a)` clears `a`.
- `intersects(set)`: true if any common word has `(words[i] & set.words[i]) != 0`.
- Any of these five with a `null` argument throws `NullPointerException` (just read a field of `set`).
- `equals(obj)`: false unless `obj instanceof BitSet`; true for the same object; otherwise true iff
  `wordsInUse` is equal and those words are equal. Size does not matter: `new BitSet(1000)` with bit 3 equals
  `new BitSet()` with bit 3. `equals(null)` is false.
- `hashCode()`: exactly
  `long h = 1234; for (int i = wordsInUse; --i >= 0; ) h ^= words[i] * (i + 1); return (int) ((h >> 32) ^ h);`
  Note `>>` (signed), not `>>>`. An empty set hashes to 1234.
- `clone()`: `(BitSet) super.clone()` inside `try { ... } catch (CloneNotSupportedException e) { throw new
  InternalError(e); }`, then give the copy its own array (`copy.words = words.clone()`). Changing the copy must
  not change the original. Before copying, the JDK trims the ORIGINAL's array to `wordsInUse` words unless the
  set's size is "sticky" (see the `sizeIsSticky` hint), so both the original and the copy can shrink:
  `new BitSet().clone()` leaves both at size 0, `new BitSet(); set(1000); clear(1000); clone()` too, while
  `new BitSet(2000)` keeps size 2048 through a clone. Not covered by the test yet, but match it.
- `toLongArray()`: a new array of exactly `wordsInUse` longs (`Arrays.copyOf(words, wordsInUse)`).
- `toByteArray()`: little-endian bytes of the in-use words, length `(length() + 7) / 8`. Byte `k` is bits
  `8k..8k+7`: `(byte) (words[k / 8] >>> (8 * (k % 8)))`. Empty set -> `new byte[0]`. `{7}` -> `[-128]`,
  `{8}` -> `[0, 1]`, `{64}` -> 9 bytes.
- `valueOf(long[] longs)`: ignore trailing zero longs (`n` = index after the last non-zero long), copy the first
  `n` longs into a NEW array of length `n`; that array is `words` and `wordsInUse = n`. So
  `valueOf(new long[]{5,0,0}).size()` is 64 and `valueOf(new long[0]).size()` is 0. Later changes to the
  argument must not affect the BitSet.
- `valueOf(byte[] bytes)`: ignore trailing zero bytes (`n`), make `words = new long[(n + 7) / 8]`, and OR byte
  `k` in as `(bytes[k] & 0xffL) << (8 * (k % 8))` into `words[k / 8]`; `wordsInUse = words.length`.
  `{1, (byte)0x80, 0, 0}` -> `{0, 15}`, size 64; 9 bytes ending in 1 -> `{64}`, size 128.
- `valueOf(null)` (either overload) throws `NullPointerException`.

## Steps

1. Open `javaw-bytecode/src/main/resources/exception-runtime/java/util/BitSet.java` and find T10's fields
   (`words`, `wordsInUse`) and helpers (growth, recalculate). Reuse them; do not duplicate the growth rule.
2. Add a private constructor `private BitSet(long[] words)` that sets `this.words = words` and
   `this.wordsInUse = words.length` for the `valueOf` methods. (Callers must pass an array without trailing
   zero words.)
3. Add `and`, `or`, `xor`, `andNot`, `intersects`, then `equals`, `hashCode`, `clone`, then the conversions.
4. No registration edit is needed: T10 already registered `"BitSet"`.
5. `javaw-agent-kit/bin/kit check T11` until PASS. Write `docs/bytecode-bitset-bulk.md` and the report, then
   `kit finish T11`.

## Hints

- If T10's growth helper takes a word INDEX (`expandTo(w)`, needs `w + 1` words), call it with
  `set.wordsInUse - 1`; if it takes a word COUNT, pass `set.wordsInUse`. Read it first. Only call it when
  `set.wordsInUse > 0`.
- Check whether the helper also raises `wordsInUse`. If it does not, set `wordsInUse = set.wordsInUse` yourself
  after growing, in both `or` and `xor`, before the copy and the recalculation.
- `or`/`xor`: compute `common = Math.min(wordsInUse, set.wordsInUse)` BEFORE growing, then copy
  `set.words[common .. set.wordsInUse-1]` with `System.arraycopy`.
- `and`: shrinking `wordsInUse` first (zeroing the dropped words) is what makes `length()` right afterwards.
- `equals`: use `if (!(obj instanceof BitSet)) return false;` then a cast (what calibration used).
- `long * int` multiplies as long; keep `h` a `long` until the final `(int)` cast.
- `toByteArray`: allocate `(length() + 7) / 8` bytes and fill each from its word; `(byte)` keeps the low 8 bits.
- The growth rule is tested: `of(70).or(of(130))` must give size 256 (2 words doubled to 4), not 192.
  Exact-size growth (`Arrays.copyOf(words, set.wordsInUse)`) fails `orGrowth`.
- Before this task `equals`/`hashCode`/`clone` fall back to `Object` (identity), so the equality test fails
  with a value difference, not a rejection. That is expected.
- `Arrays.copyOf(long[],int)`, `long[].clone()`, `System.arraycopy`, `Math.min/max` exist in the runtime.
- `InternalError(Throwable)` and `CloneNotSupportedException` exist; `ArrayList.clone()` shows the pattern.
- `sizeIsSticky` (for `clone`): add `private transient boolean sizeIsSticky;` to BitSet. Set it to `true` in
  T10's `BitSet(int nbits)` constructor only; `BitSet()` and the private `valueOf` constructor leave it `false`.
  Set it back to `false` in T10's growth helper, inside the branch that actually allocates a bigger array (the
  user's chosen size is gone once the set grows). Then start `clone()` with
  `if (!sizeIsSticky && wordsInUse != words.length) words = Arrays.copyOf(words, wordsInUse);` before
  `super.clone()`. None of this changes what T10's members return, so `BytecodeBitSetTest` still passes.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
matchesLogicalOperations, matchesArrayConversions, matchesNullArguments, matchesEqualityHashingAndClone, rejectsUnlinkedMembers: if the failure says "unsupported dynamic call java/util/BitSet.NAME(DESC); link a matching instance method" (or "unsupported static call ...; link a matching static declaration" for valueOf), that member is missing from BitSet.java or its signature differs. Add it exactly as listed, e.g. `public void or(BitSet set)`, `public static BitSet valueOf(long[] longs)`, `public byte[] toByteArray()`, `public Object clone()`. No registration edit is needed (T10 did it). If rejectsUnlinkedMembers fails, delete valueOf(ByteBuffer) and valueOf(LongBuffer).
equality, hashMatchesEquals, hashCodes, cloneOfEmpty: "falsefalse..." means equals does not override Object.equals. The signature must be exactly `public boolean equals(Object obj)` (not equals(BitSet)): `if (!(obj instanceof BitSet)) return false;`, then compare wordsInUse and words[0..wordsInUse-1] only, never words.length or size() (new BitSet(1000) with {3, 64} equals a plain {3, 64}). hashCode keeps `long h = 1234` and multiplies by (i + 1), not i. hashCodes expects `1234,1235,1272,-2147482414,1232,1234,-2147478318,1106,1106`.
cloneIsDeep: the original changes when the copy changes, so clone shares the words array. After `BitSet copy = (BitSet) super.clone();` add `copy.words = words.clone();`, inside `try { ... } catch (CloneNotSupportedException e) { throw new InternalError(e); }`. If that line does not compile, remove `final` from the words field.
orBasics, orSmallerArgument, orGrowth, xorBasics: or/xor lose high bits or report the wrong size. orGrowth expects `{130} len=131 size=192|{} len=0 size=0|{5} len=6 size=64|{70, 130} len=131 size=256|{70, 140} len=141 size=256`. First compute `int common = Math.min(wordsInUse, set.wordsInUse);`. Only when set.wordsInUse > wordsInUse, grow with T10's helper (rule max(2 * words.length, set.wordsInUse); pass set.wordsInUse - 1 if the helper takes a word index) and make wordsInUse = set.wordsInUse. Use set.wordsInUse, never set.words.length. OR/XOR words 0..common-1, then `System.arraycopy(set.words, common, words, common, set.wordsInUse - common)`. xor must end with recalculateWordsInUse() (xorBasics: {4, 64} xor {4, 64} is {} and isEmpty() true).
andBasics, andShrinks, andNotBasics, withItself: an exception in Wasm or extra bits means and/andNot read past the common words. and(): `while (wordsInUse > set.wordsInUse) words[--wordsInUse] = 0;`, then AND words 0..wordsInUse-1, then recalculateWordsInUse(). andNot loops `i < Math.min(wordsInUse, set.wordsInUse)` and recalculates. Neither changes size() (andShrinks expects size=320). Only and/or may `return` when set == this: a.xor(a) and a.andNot(a) must clear a (withItself expects `{1, 65}|{1, 65}|{}true|{}true`).
nullSets, nullArrays: the JVM prints NullPointerException for every call and `{4}` at the end of nullSets. Do not add `if (set == null) return;` (or return false or an empty set for null). Just read set.wordsInUse, longs.length or bytes.length, and the NPE happens by itself.
toByteArrays, toByteArrayAfterClear, toLongArrays, arraysAreCopies, roundTrips: toByteArrays expects `[][1][-128][0, 1][1, 0, 0, 0, 0, 0, 0, -128][0, 0, 0, 0, 0, 0, 0, 0, 1][14, 0, 16]`. The byte length is `(length() + 7) / 8`, not wordsInUse * 8, and byte k is `(byte) (words[k / 8] >>> (8 * (k % 8)))` (little-endian); toByteArrayAfterClear expects `[32]|1`. toLongArray must return a NEW array of exactly wordsInUse longs, `Arrays.copyOf(words, wordsInUse)`, never words or words.clone() (new BitSet(1000).toLongArray() is `[]`).
valueOfLongs, valueOfLongsCopies, valueOfBytes, roundTrips: valueOf must drop trailing zeros and copy. For long[]: find n = index after the last non-zero long, then use `Arrays.copyOf(longs, n)` as words with wordsInUse = n ({5,0,0} -> size=64, {0,0} -> {} size=0, later edits to the argument must not show). For byte[]: find n = index after the last non-zero byte, `words = new long[(n + 7) / 8]`, then `words[k / 8] |= (bytes[k] & 0xffL) << (8 * (k % 8));` for k < n. Without `& 0xffL` a negative byte sign-extends: {-1} must give {0, 1, 2, 3, 4, 5, 6, 7} size=64, {1,(byte)0x80,0,0} must give {0, 15} size=64, and {0,0,0} must give {} size=0.
```

## Docs

Create `docs/bytecode-bitset-bulk.md` with this skeleton (fill in the brackets):

    # Bytecode BitSet bulk operations

    Revision `bitset-bulk-vN` links the java.util.BitSet logical operations, equality, clone and array
    conversions in the bundled exception runtime.

    Contract:

    - <behaviour: and/or/xor/andNot growth, equals ignores size, hashCode formula, clone is deep, byte/long
      array layouts, valueOf trims trailing zeros>

    Evidence: `BytecodeBitSetBulkTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders,
    two fresh instances). `BitSet.valueOf(ByteBuffer)` and `BitSet.valueOf(LongBuffer)` are rejected before
    WAT generation.
