# Bytecode ByteBuffer

Revision `byte-buffer-v337` links `java.nio.Buffer`, `java.nio.ByteBuffer` (heap buffers through a package-private
`HeapByteBuffer`), `BufferUnderflowException`, `BufferOverflowException`, `InvalidMarkException` and
`ReadOnlyBufferException` in the bundled exception runtime. `java.nio.ByteOrder` already existed and is unchanged.
Revision `byte-buffer-follow-ups-v341` adds the float/double accessors (`getFloat`, `putFloat`, `getDouble`,
`putDouble`, relative and absolute), the absolute bulk `get`/`put` members (`get(int, byte[], int, int)`,
`get(int, byte[])`, `put(int, byte[], int, int)`, `put(int, byte[])`, `put(int, ByteBuffer, int, int)`) and
`slice(int, int)`.

Contract:

- Mark/position/limit/capacity follow the JDK invariant `-1 <= mark <= position <= limit <= capacity`.
  `position(p)` checks `p > limit` before `p < 0` and throws
  `IllegalArgumentException("newPosition > limit: (17 > 16)")` / `("newPosition < 0: (-1 < 0)")`, and discards a mark
  above the new position; `limit(l)` checks `l > capacity` before `l < 0` with
  `("newLimit > capacity: (17 > 16)")` / `("newLimit < 0: (-2 < 0)")`, lowers the position and discards a mark above
  the new limit. `mark()/reset()/clear()/flip()/rewind()` behave as in the JDK; `reset()` without a mark throws
  `InvalidMarkException` with a null message.
- `allocate(c)` throws `IllegalArgumentException("capacity < 0: (c < 0)")` for negative capacity; `wrap(a, off, len)`
  throws `IndexOutOfBoundsException` (no message) for a bad range, and the wrapped buffer has capacity `a.length`,
  position `off`, limit `off + len` and `arrayOffset() == 0`.
- Relative byte access moves the position and throws `BufferUnderflowException`/`BufferOverflowException` (no
  message) without moving it on failure; absolute `get(i)`/`put(i, b)` check `i < 0 || 1 > limit - i` against the
  **limit** and throw `IndexOutOfBoundsException` without a message.
- Bulk transfers `get(dst, off, len)` / `put(src, off, len)` apply a `checkFromIndexSize` range check first
  (`IndexOutOfBoundsException("Range [off, off + len) out of bounds for length n")`), then the underflow/overflow
  check. `put(ByteBuffer)` checks read-only first (a read-only buffer throws `ReadOnlyBufferException` even for
  `put(itself)`, as the JDK's `HeapByteBufferR` does), then rejects `src == this` with
  `IllegalArgumentException("The source buffer is this buffer")`, then checks overflow, and copies through a temporary array so overlapping ranges of one shared backing
  array are preserved.
- `getShort/putShort`, `getChar/putChar`, `getInt/putInt` and `getLong/putLong` (relative and absolute) honour the
  buffer byte order: big-endian stores the most significant byte first, little-endian the least. Every new buffer —
  including `slice()`, `duplicate()` and `asReadOnlyBuffer()` of a little-endian source — is big-endian, and
  `order(null)` selects little-endian without throwing. Relative typed reads with too few bytes left throw
  `BufferUnderflowException` and absolute ones need `i + size <= limit`, else `IndexOutOfBoundsException` without a
  message; every `putX` on a read-only buffer throws `ReadOnlyBufferException` before any index check.
- `getFloat/putFloat` and `getDouble/putDouble` (relative and absolute) sit on the int/long accessors with
  `Float.intBitsToFloat`/`Float.floatToRawIntBits` and `Double.longBitsToDouble`/`Double.doubleToRawLongBits`.
  The **raw** conversions are used, never `floatToIntBits`/`doubleToLongBits`, so NaN payloads are stored and
  read back bit-exactly (e.g. the bits `0x7fc12345` round-trip through `putInt(0, b)`/`getFloat(0)`/
  `putFloat(4, f)`/`getInt(4)`); byte order, the 4/8-byte size check, underflow/overflow and read-only
  behaviour are inherited from the int/long accessors.
- The absolute bulk transfers (`get(int, byte[], int, int)`, `get(int, byte[])`, `put(int, byte[], int, int)`,
  `put(int, byte[])`, `put(int, ByteBuffer, int, int)`) never read or move the position or the mark and return
  `this`. `get` checks `index`/`len` against the **limit** first, then `off`/`len` against `dst.length`; every
  `put` throws `ReadOnlyBufferException` before any range check, then applies the same limit check and checks
  `off`/`len` against `src.length` (arrays) or `src.limit()` — not its capacity or remaining — (buffers). A
  failing range throws the `checkFromIndexSize` `IndexOutOfBoundsException`
  (`"Range [from, from + size) out of bounds for length n"`) and a `null` source throws
  `NullPointerException` from that source check, after the limit check passed. The copy is
  `System.arraycopy`, so `src == this` and overlapping ranges (one shared backing array, or between a buffer
  and its duplicate or slice) are allowed. Unlike the relative `put(ByteBuffer)`, the absolute buffer form does
  not reject `src == this`.
- `slice(int, int)` (heap) checks `index`/`length` against the **limit** with the same range message, then
  returns a buffer sharing the backing array with offset `offset + index` — relative to index 0, never to the
  position — position 0, limit = capacity = `length`, mark -1, the same read-only flag and big-endian; the
  source's position and mark are unchanged.
- `slice()` shares the backing array with offset `offset + position`, mark -1 and capacity = remaining;
  `duplicate()` shares the array and keeps mark/position/limit; `asReadOnlyBuffer()` is a read-only duplicate that
  keeps its mark. `compact()` moves the remaining bytes to the start, sets `position(rem)`, `limit(capacity)` and
  discards the mark, and throws `ReadOnlyBufferException` on a read-only buffer.
- `array()`/`arrayOffset()` return the backing array and offset (throwing `ReadOnlyBufferException` on read-only
  buffers); `hasArray()` is false for read-only buffers and `isDirect()` is always false. `toString()` prints
  `java.nio.HeapByteBuffer[pos=.. lim=.. cap=..]` or `java.nio.HeapByteBufferR[...]` for read-only buffers;
  `hashCode()` walks the remaining bytes from `limit-1` down to `position` with `h = 31*h + byte` starting at 1;
  `equals` compares only the remaining bytes, ignoring byte order and the read-only flag; `compareTo` returns the
  difference `x - y` of the first differing signed bytes, else `thisRemaining - thatRemaining`.
- The covariant `ByteBuffer` overrides of `position(int)`, `limit(int)`, `mark()`, `reset()`, `clear()`, `flip()`
  and `rewind()` make javac emit the `Buffer`-returning bridge methods that Java 8 compiled libraries call.

Known differences: one heap class serves writable and read-only buffers, so `getClass()` of a read-only buffer is
`java.nio.HeapByteBuffer` (toString still prints `HeapByteBufferR`). No direct or mapped buffers, view buffers
(`asLongBuffer` etc.), `mismatch`, `alignmentOffset` or `alignedSlice`.

Evidence: `BytecodeByteBufferTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
fresh instances), and `BytecodeByteBufferFollowUpsTest` does the same for the float/double accessors, the absolute
bulk `get`/`put` members and `slice(int, int)`. `ByteBuffer.allocateDirect` and `asLongBuffer` are rejected before
WAT generation, as are `asFloatBuffer`, `asDoubleBuffer`, `mismatch`, `alignedSlice` in the
follow-up test.
