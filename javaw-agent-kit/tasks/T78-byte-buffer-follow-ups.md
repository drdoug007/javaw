# T78 ByteBuffer float/double accessors, absolute bulk transfers and slice(int, int)

```kit
id: T78
slug: byte-buffer-follow-ups
title: link the ByteBuffer float/double accessors, the absolute bulk get/put members and slice(int, int)
tests: BytecodeByteBufferFollowUpsTest
depends: T74
allow: javaw-bytecode/src/main/resources/exception-runtime/java/nio/ByteBuffer.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/nio/HeapByteBuffer.java
allow: docs/bytecode-byte-buffer.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-byte-buffer-follow-ups-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeByteBufferFollowUpsTest.java`.

## Goal

T74 linked the heap `java.nio.ByteBuffer` core. This task adds the members it left out that the pinned libraries
call. `runtime-target/target/audit/member-references.tsv` lists, on owner `java/nio/ByteBuffer`:

- H2 (`com.h2database:h2:2.2.224`): `getFloat()F` 4 (`org/h2/mvstore/rtree/SpatialDataType.read`), `getDouble()D` 2
  (`org/h2/mvstore/type/ObjectDataType$DoubleType.read`), `putFloat(F)Ljava/nio/ByteBuffer;` 1 and
  `putDouble(D)Ljava/nio/ByteBuffer;` 1 (`org/h2/mvstore/WriteBuffer.putFloat`/`putDouble`).
- Spring core (`org.springframework:spring-core:6.1.5`): `slice(II)Ljava/nio/ByteBuffer;` 2
  (`DefaultDataBuffer.readableByteBuffers`) and `put(ILjava/nio/ByteBuffer;II)Ljava/nio/ByteBuffer;` 2
  (`DefaultDataBuffer.toByteBuffer`).

For coherence the absolute float/double variants and the JDK 13 absolute array bulk members
`get(int, byte[])`, `get(int, byte[], int, int)`, `put(int, byte[])`, `put(int, byte[], int, int)` are included too.

## Members to implement

Only two files change, both under `javaw-bytecode/src/main/resources/exception-runtime/java/nio/`. Do not add new
classes; do not register anything (both classes are already registered).

1. `ByteBuffer.java` (all `public`, the puts and bulk methods return `ByteBuffer`):
   - `float getFloat()`, `float getFloat(int index)`, `ByteBuffer putFloat(float value)`,
     `ByteBuffer putFloat(int index, float value)`
   - `double getDouble()`, `double getDouble(int index)`, `ByteBuffer putDouble(double value)`,
     `ByteBuffer putDouble(int index, double value)`
   - `ByteBuffer get(int index, byte[] dst, int offset, int length)`, `ByteBuffer get(int index, byte[] dst)`
   - `ByteBuffer put(int index, byte[] src, int offset, int length)`, `ByteBuffer put(int index, byte[] src)`
   - `ByteBuffer put(int index, ByteBuffer src, int offset, int length)`
   - `public abstract ByteBuffer slice(int index, int length)`
2. `HeapByteBuffer.java`: `public ByteBuffer slice(int index, int length)`.
3. The T74 test `BytecodeByteBufferTest.leavesOutDirectBuffersViewsAndFloatingPoint` no longer checks `getFloat`
   or `slice(int, int)` (removed when this task was queued). Do not edit any test.

## Members to leave out

- View buffers (`asFloatBuffer`, `asDoubleBuffer`, ...; the test checks both), `mismatch` (checked),
  `alignmentOffset`, `alignedSlice` (checked), and direct buffers (`allocateDirect` stays unlinked, checked).
- No changes to `Buffer.java`, `Float.java`, `Double.java` or `ByteOrder.java`.

## JDK behaviour to match

Float and double:
- `getFloat()` is `Float.intBitsToFloat(getInt())`, `putFloat(v)` is `putInt(Float.floatToRawIntBits(v))`, and the
  same for the absolute forms and for `double` with `getLong`/`putLong`, `Double.longBitsToDouble` and
  `Double.doubleToRawLongBits`. So they honour the byte order, need 4 (float) or 8 (double) bytes, throw
  `BufferUnderflowException`/`BufferOverflowException` (relative) or `IndexOutOfBoundsException` with no message
  (absolute, checked against the **limit**), and the puts on a read-only buffer throw `ReadOnlyBufferException`
  before any index check. Reusing the T74 int/long accessors gives all of that.
- Use the **raw** conversions (`floatToRawIntBits`, `doubleToRawLongBits`), never `floatToIntBits`/`doubleToLongBits`:
  the JDK stores NaN payloads bit-exactly. `putInt(0, 0x7fc12345)`, `getFloat(0)`, `putFloat(4, f)`, `getInt(4)`
  gives `7fc12345`, and `0xffc00001`, `0x7fffffff`, `0xfff8000000000001L` round-trip the same way.

Range check: T74's private static helper (JDK `Objects.checkFromIndexSize(from, size, length)`), message
`Range [from, from + size) out of bounds for length length`, e.g. `Range [5, 5 + 3) out of bounds for length 7`.

Absolute bulk transfers (none of them reads or moves the position or the mark; they return `this`):
- `get(index, dst, off, len)`: first check `index`/`len` against `limit()`, then `off`/`len` against `dst.length`,
  then copy `len` bytes from buffer index `index` (array slot `offset + index`). `get(index, dst)` is
  `get(index, dst, 0, dst.length)`. So `get(9, null, 0, 1)` on a limit-7 buffer throws the range
  `IndexOutOfBoundsException` (index check first), while `get(0, null)` throws `NullPointerException`.
- `put(index, src, off, len)`: read-only check first (`ReadOnlyBufferException`, even with bad indexes), then the
  same two range checks (limit, then `src.length`), then copy. `put(index, src)` is `put(index, src, 0, src.length)`.
- `put(index, src, off, len)` with a `ByteBuffer` source: read-only check first, then `index`/`len` against this
  buffer's `limit()`, then `off`/`len` against **`src.limit()`** (not its capacity or remaining); then copy `len`
  bytes from source index `off` to index `index`. `src == this` is allowed (no exception) and the ranges may overlap
  (also between a buffer and its duplicate or slice): `System.arraycopy` handles overlap within one array correctly.
  A `null` source throws `NullPointerException` from `src.limit()`, but only after the index check against this limit
  passed. The byte order of either buffer and a read-only source do not matter. Neither position moves.
- Bounds are against the **limit**, so `get(8, e, 0, 0)` on a limit-7 buffer throws
  `Range [8, 8 + 0) out of bounds for length 7`, while `get(7, new byte[0])` succeeds.

`slice(index, length)` (`HeapByteBuffer`):
- Check `index`/`length` against `limit()` with the same range message (e.g. `slice(0, 7)` on limit 6 throws
  `Range [0, 0 + 7) out of bounds for length 6`), then return a buffer sharing `hb` with mark -1, position 0,
  limit = capacity = `length`, offset `offset + index` (**not** relative to the position), the same read-only flag,
  big-endian. The source's position and mark do not matter and do not change.

## Steps

1. Read the test, then `java/nio/ByteBuffer.java`, `java/nio/HeapByteBuffer.java` and `docs/bytecode-byte-buffer.md`.
2. Add the members above to `ByteBuffer.java` and `HeapByteBuffer.java`.
3. `javaw-agent-kit/bin/kit check T78` until PASS.
4. Update `docs/bytecode-byte-buffer.md` (below), write the report, then `javaw-agent-kit/bin/kit finish T78`.

## Hints

- `Float.floatToRawIntBits`, `Float.intBitsToFloat`, `Double.doubleToRawLongBits` and `Double.longBitsToDouble`
  already exist in the runtime and keep NaN payloads.
- Every float/double member is one line on top of `getInt`/`putInt`/`getLong`/`putLong` (relative and absolute).
- Reuse T74's `checkRange(size, from, length)` helper in `ByteBuffer`. Mind its parameter order: the **array/limit
  length first**, then `from`, then the count. `slice(int, int)` in `HeapByteBuffer` cannot call a `private`
  helper of `ByteBuffer`; make the helper package-private (`static void checkRange`) or repeat the check.
- Copy with `System.arraycopy(src.hb, src.offset + off, hb, offset + index, len)`; it is correct for overlapping
  ranges in the same array, so no temporary array is needed here. Use the `offset` field (not `arrayOffset()`,
  which throws on read-only buffers) for the read-only source.
- The bulk methods must not call `position(...)`: they are absolute.
- In `get(int, byte[], int, int)` the parameter named `offset` shadows the field; write `this.offset + index`.
- The reference solution was about 45 added lines.
- The Wasm runner stops at the first failing fixture of each `@Test` method; when Wasm throws where the JVM did not,
  the failure shows `(thrown in Wasm: <Java exception>)`.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
floatAndDoubleAccessors, nanPayloadsRoundTripBitExactly, absoluteBulkArrayTransfers, absoluteBulkBufferTransfer, sliceWithIndexAndLength: if FIX THE RUNTIME says `link` or names a `java/nio/ByteBuffer` method such as getFloat, putDouble, slice(II) or put(I[BII), add that member to ByteBuffer.java with exactly the listed signature (slice(int,int) abstract in ByteBuffer, implemented in HeapByteBuffer). If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
floatsAndDoubles: float/double accessors are Float.intBitsToFloat(getInt(..)) / putInt(Float.floatToRawIntBits(v)) and the same with getLong/putLong and Double; that gives byte order, underflow/overflow, the limit index check and the read-only check for free.
nanPayloads: use floatToRawIntBits/doubleToRawLongBits, not floatToIntBits/doubleToLongBits, which collapse every NaN to 7fc00000 / 7ff8000000000000.
absoluteBulkArrays: get(index, dst, off, len) checks index/len against limit() first, then off/len against dst.length, with `Range [from, from + size) out of bounds for length n`; put(index, src, off, len) throws ReadOnlyBufferException before both checks; none of them moves the position.
absoluteBulkBuffer: put(index, src, off, len) checks read-only first, then index/len against this limit(), then off/len against src.limit(); src == this and overlapping ranges are allowed (System.arraycopy); neither position moves.
sliceIndexLength: slice(index, length) checks index/length against limit() (range message), then returns offset = offset + index (not + position), position 0, limit = capacity = length, mark -1, same read-only flag, BIG_ENDIAN.
leavesOutViewBuffersAndMismatch: this fails when asFloatBuffer, asDoubleBuffer, mismatch, alignedSlice or allocateDirect became linked. Do not add them.
```

## Docs

Update `docs/bytecode-byte-buffer.md`: add a line saying revision `byte-buffer-follow-ups-vN` adds the float/double
accessors, the absolute bulk `get`/`put` members and `slice(int, int)`; add their rules to the Contract list (raw
bit conversions keep NaN payloads; absolute bulk checks against the limit then the array/source limit, read-only
first for puts, positions unchanged, overlap allowed; slice offset relative to index 0); remove "float/double
accessors or absolute bulk methods" from Known differences; and name `BytecodeByteBufferFollowUpsTest` in Evidence
with its rejected members (`asFloatBuffer`, `asDoubleBuffer`, `mismatch`, `alignedSlice`, `allocateDirect`).
