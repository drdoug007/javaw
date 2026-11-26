# Bytecode CharBuffer

Revision `char-buffer-v342` links `java.nio.CharBuffer` (heap buffers through a package-private `HeapCharBuffer`,
`CharSequence` views through a package-private read-only `StringCharBuffer`) in the bundled exception runtime.
`Buffer`, `ByteOrder` and the four buffer exceptions already exist and are unchanged.

Contract:

- `allocate(c)` throws `IllegalArgumentException("capacity < 0: (c < 0)")` for negative capacity, else a writable
  heap buffer with position 0, limit = capacity = `c` (its `toString()` is `c` NUL chars, not empty).
  `wrap(char[], off, len)` throws `IndexOutOfBoundsException` (no message) for a bad range and returns a writable
  buffer on the array itself with capacity `array.length`, **position `off`, limit `off + len`, `arrayOffset()` 0**.
  `wrap(CharSequence, start, end)` throws the same message-less exception and returns a `StringCharBuffer` with
  capacity `csq.length()`, position `start`, limit `end`; it reads the sequence live, so later changes to a
  wrapped `StringBuilder` are visible. `wrap(null)` in any form throws `NullPointerException`.
- Relative `get()`/`put(c)` move the position and throw `BufferUnderflowException`/`BufferOverflowException`
  (no message) without moving it on failure; absolute `get(i)`/`put(i, c)` check `i < 0 || 1 > limit - i` against
  the **limit** and throw `IndexOutOfBoundsException` without a message. `toString()` returns the remaining chars
  (position to limit) and does not move the position; `charAt(i)` and `subSequence(start, end)` are relative to
  the position (`charAt(i)` is `get(position() + checkIndex(i, 1))`, exactly as the JDK does), `length()` is
  `remaining()` and `isEmpty()` is `remaining() == 0`.
- Bulk transfers `get(dst, off, len)` / `put(src, off, len)` (char arrays) apply a `checkFromIndexSize`-style
  range check first (`IndexOutOfBoundsException("Range [off, off + len) out of bounds for length n")`, written as
  a static `checkRange` helper because the runtime has no `java.util.Objects`), then the
  underflow/overflow check; `get` reads through `get(int)` so it also works for string buffers.
  `put(String, start, end)` checks the range with `size = end - start` (so `put("abc", 2, 1)` gives
  `Range [2, 2 + -1) out of bounds for length 3`), then overflow. A read-only **heap** buffer throws
  `ReadOnlyBufferException` before the range check (JDK `HeapCharBufferR`); a **string** buffer runs the range
  check first. `put(CharBuffer)` checks read-only heap first (even for `put(itself)`), then `src == this`
  (`IllegalArgumentException("The source buffer is this buffer")`), then read-only string buffers, then
  `n > remaining` (`BufferOverflowException`, neither position moves); it copies the `n` source chars through a
  temporary `char[]` so a string source or a duplicate sharing the same array with overlapping ranges in either
  direction works.
- `Appendable`: `append(char)` is `put(c)`; `append(csq)` throws `ReadOnlyBufferException` on a read-only heap
  buffer, appends `"null"` for `null`, `put`s a `CharBuffer` argument (advancing its position) and
  `put(csq.toString())` otherwise; `append(csq, start, end)` throws the same read-only check, treats `null` as
  `"null"`, throws `IndexOutOfBoundsException("Range [start, end) out of bounds for length n")` for a bad range,
  else `put`s the subsequence text. None declare `throws IOException`.
- `slice()` shares the storage with offset `offset + position()`, mark -1, position 0, limit = capacity =
  remaining, the same read-only flag and class (a string slice stays a string buffer); `duplicate()` shares the
  storage and keeps mark/position/limit; `asReadOnlyBuffer()` is a read-only duplicate that keeps the mark (a
  string buffer returns `duplicate()`). `subSequence(start, end)` is relative to the position and keeps the
  capacity and offset (only position/limit move): the heap message is
  `Range [start, end) out of bounds for length remaining`, the string buffer's exception has **no message**.
  `compact()` throws `ReadOnlyBufferException` on a read-only buffer, else moves the remaining chars to the
  start, sets `position(rem)`, `limit(capacity)` and discards the mark. Every `put`/`compact`/`append` on a
  string buffer throws `ReadOnlyBufferException` (after the range check where described above).
- `hasArray()` is `hb != null && !isReadOnly`; `array()`/`arrayOffset()` throw
  `UnsupportedOperationException` on a string buffer (`hb == null`) and `ReadOnlyBufferException` on a read-only
  heap buffer, else return the same backing array and offset. `order()` is `ByteOrder.nativeOrder()` and
  `isDirect()` is false.
- `hashCode()` walks the remaining chars from `limit-1` down to `position` with `h = 31*h + c` starting at 1;
  `equals` compares only the remaining chars of any two `CharBuffer`s (a `String` is never equal), ignoring the
  read-only flag; `compareTo` returns the difference `x - y` of the first differing chars (65438 for `'￿'`
  vs `'a'`), else `thisRemaining - thatRemaining`, and throws `NullPointerException` for a null argument.
- The covariant `CharBuffer` overrides of `position(int)`, `limit(int)`, `mark()`, `reset()`, `clear()`, `flip()`
  and `rewind()` make javac emit the `Buffer`-returning bridge methods that Java 8 compiled libraries call, and
  the `append` overrides emit the `Appendable`-returning bridges.

Known differences: one heap class serves writable and read-only buffers, so `getClass()` of a read-only heap
buffer is `java.nio.HeapCharBuffer`. No `Readable`/`read(CharBuffer)`, direct or byte-buffer views, absolute bulk
methods or `mismatch`.

Evidence: `BytecodeCharBufferTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
fresh instances). `CharBuffer.read`, `slice(int, int)`, `get(int, char[])`, `mismatch` and
`ByteBuffer.asCharBuffer` are rejected before WAT generation.
