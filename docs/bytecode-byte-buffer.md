# Bytecode ByteBuffer

Revision `byte-buffer-v337` links `java.nio.Buffer`, `java.nio.ByteBuffer` (heap buffers through a package-private
`HeapByteBuffer`), `BufferUnderflowException`, `BufferOverflowException`, `InvalidMarkException` and
`ReadOnlyBufferException` in the bundled exception runtime. `java.nio.ByteOrder` already existed and is unchanged.

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
(`asLongBuffer` etc.), float/double accessors or absolute bulk methods (`slice(int, int)`, `put(int, byte[], ...)`,
`mismatch`, ...).

Evidence: `BytecodeByteBufferTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
fresh instances). `ByteBuffer.allocateDirect`, `asLongBuffer`, `getFloat` and `slice(int, int)` are rejected
before WAT generation.
