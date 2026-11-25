# T74 java.nio.Buffer and the heap ByteBuffer core

```kit
id: T74
slug: byte-buffer
title: link java.nio.Buffer and the heap java.nio.ByteBuffer core
tests: BytecodeByteBufferTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/nio/Buffer.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/nio/ByteBuffer.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/nio/HeapByteBuffer.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/nio/BufferUnderflowException.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/nio/BufferOverflowException.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/nio/InvalidMarkException.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/nio/ReadOnlyBufferException.java
allow: docs/bytecode-byte-buffer.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-byte-buffer-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeByteBufferTest.java`.

## Goal

Add `java.nio.Buffer`, `java.nio.ByteBuffer` (heap buffers only, through a package-private `HeapByteBuffer`) and the
four buffer exceptions `BufferUnderflowException`, `BufferOverflowException`, `InvalidMarkException` and
`ReadOnlyBufferException` to the bundled runtime. `java.nio.ByteOrder` already exists and is not changed.
`runtime-target/target/audit/member-references.tsv` lists 196 `java/nio/ByteBuffer` rows in 11 artifacts (H2,
Tomcat core and websocket, Spring core/web/boot, Jackson core/databind, logback, ANTLR, SnakeYAML). The largest by
reference count: `position()I` 265, `remaining()I` 215, `position(I)Ljava/nio/ByteBuffer;` 145, `limit(I)` 117,
`get()B` 109, `put(B)` 100, `allocate(I)` 96 (H2, ANTLR, Tomcat, Spring, SnakeYAML), `limit()I` 95,
`hasRemaining()` 83, `clear()` 69, `capacity()` 68, `flip()` 68, `array()[B` 61, `wrap([B)` 51, `get(I)B` 44,
`put(Ljava/nio/ByteBuffer;)` 37, `get([B)` 30, `arrayOffset()` 29, `put([B)` 27, `get([BII)` 22, `put(IB)` 22,
`compact()` 19, `duplicate()` 19, `put([BII)` 17, `wrap([BII)` 15, `slice()` 14, `getChar()` 11, `getLong()` 10,
`isDirect()` 8, `hasArray()` 7, `getShort()` 6, `getInt()` 5, `asReadOnlyBuffer()` 4 (Jackson databind, Spring
core), and `mark()`/`reset()`/`putShort`/`isReadOnly`/`equals`/`hashCode`/`putInt`/`putLong`/`putChar` (H2, Tomcat
websocket, Spring core). Libraries compiled for Java 8 (H2, ANTLR, SnakeYAML, Jackson databind) also call
`ByteBuffer.position(I)Ljava/nio/Buffer;` 33 times, `rewind()Ljava/nio/Buffer;` 14, `flip()Ljava/nio/Buffer;` 12,
`limit(I)Ljava/nio/Buffer;` 11 and `clear()Ljava/nio/Buffer;` 1, and the `java/nio/Buffer` owner directly (12 rows:
`limit(I)`, `position(I)`, `rewind()`, `capacity()`, `limit()`, `mark()`, `reset()` in H2 and Tomcat, `remaining()`
in Spring core). Both descriptors must link; the covariant overrides below give both (see Hints). No audit row calls
`order(ByteOrder)`, but the typed accessors are only coherent with it, so it is included.

## Members to implement

All files are under `javaw-bytecode/src/main/resources/exception-runtime/java/nio/` (`package java.nio;`).

1. The four exceptions, each with only a public no-argument constructor (no message):
   - `public class BufferUnderflowException extends RuntimeException`
   - `public class BufferOverflowException extends RuntimeException`
   - `public class InvalidMarkException extends IllegalStateException`
   - `public class ReadOnlyBufferException extends UnsupportedOperationException`

2. `Buffer.java`: `public abstract class Buffer` with
   - fields `private int mark = -1; private int position; private int limit; private final int capacity;`
   - package-private constructor `Buffer(int mark, int pos, int lim, int cap)`
   - `static IllegalArgumentException createCapacityException(int capacity)`
   - `public final int capacity()`, `public final int position()`, `public final int limit()`
   - `public Buffer position(int newPosition)`, `public Buffer limit(int newLimit)`
   - `public Buffer mark()`, `public Buffer reset()`, `public Buffer clear()`, `public Buffer flip()`,
     `public Buffer rewind()`
   - `public final int remaining()`, `public final boolean hasRemaining()`
   - abstract: `public abstract boolean isReadOnly()`, `public abstract boolean hasArray()`,
     `public abstract Object array()`, `public abstract int arrayOffset()`, `public abstract boolean isDirect()`,
     `public abstract Buffer slice()`, `public abstract Buffer duplicate()`
   - package-private helpers: `final int markValue()`, `final void discardMark()`, `final int nextGetIndex(int nb)`,
     `final int nextPutIndex(int nb)`, `final int checkIndex(int i, int nb)`

3. `ByteBuffer.java`: `public abstract class ByteBuffer extends Buffer implements Comparable<ByteBuffer>` with
   - fields `final byte[] hb; final int offset; final boolean isReadOnly; boolean bigEndian = true;`
   - package-private constructor `ByteBuffer(int mark, int pos, int lim, int cap, byte[] hb, int offset, boolean readOnly)`
   - `public static ByteBuffer allocate(int capacity)`, `public static ByteBuffer wrap(byte[] array, int offset, int length)`,
     `public static ByteBuffer wrap(byte[] array)`
   - abstract: `public abstract ByteBuffer slice()`, `duplicate()`, `asReadOnlyBuffer()`, `public abstract byte get()`,
     `public abstract ByteBuffer put(byte b)`, `public abstract byte get(int index)`,
     `public abstract ByteBuffer put(int index, byte b)`, `public abstract ByteBuffer compact()`
   - `public ByteBuffer get(byte[] dst, int offset, int length)`, `public ByteBuffer get(byte[] dst)`
   - `public ByteBuffer put(ByteBuffer src)`, `public ByteBuffer put(byte[] src, int offset, int length)`,
     `public final ByteBuffer put(byte[] src)`
   - `public final boolean hasArray()`, `public final byte[] array()`, `public final int arrayOffset()`,
     `public boolean isDirect()`
   - covariant overrides returning `ByteBuffer`: `position(int)`, `limit(int)`, `mark()`, `reset()`, `clear()`,
     `flip()`, `rewind()` (each `super.x(...); return this;`)
   - `public final ByteOrder order()`, `public final ByteBuffer order(ByteOrder bo)`
   - `getShort()`, `getShort(int)`, `putShort(short)`, `putShort(int, short)`, `getChar()`, `getChar(int)`,
     `putChar(char)`, `putChar(int, char)`, `getInt()`, `getInt(int)`, `putInt(int)`, `putInt(int, int)`,
     `getLong()`, `getLong(int)`, `putLong(long)`, `putLong(int, long)` (all `public`; the puts return `ByteBuffer`)
   - `public String toString()`, `public int hashCode()`, `public boolean equals(Object ob)`,
     `public int compareTo(ByteBuffer that)`

4. `HeapByteBuffer.java`: `final class HeapByteBuffer extends ByteBuffer` (package-private) with the constructor
   `HeapByteBuffer(byte[] hb, int mark, int pos, int lim, int cap, int offset, boolean readOnly)` and
   `isReadOnly()`, `slice()`, `duplicate()`, `asReadOnlyBuffer()`, `get()`, `get(int)`, `put(byte)`,
   `put(int, byte)`, `compact()`. One class serves both writable and read-only buffers (the `isReadOnly` flag).

## Members to leave out

- Direct and mapped buffers: `allocateDirect` (the test checks it stays unlinked), `MappedByteBuffer`,
  `DirectByteBuffer`. `isDirect()` always returns `false`.
- View buffers `asLongBuffer`/`asIntBuffer`/`asCharBuffer`/... (`LongBuffer` etc. do not exist; the test checks
  `asLongBuffer`), and `getFloat`/`putFloat`/`getDouble`/`putDouble` (H2 only, 8 rows; a later task; the test checks
  `getFloat()`).
- The JDK 13+ absolute bulk members `slice(int, int)` (checked), `get(int, byte[])`, `get(int, byte[], int, int)`,
  `put(int, byte[])`, `put(int, byte[], int, int)`, `put(int, ByteBuffer, int, int)`, and `mismatch`,
  `alignmentOffset`, `alignedSlice`: a later task.
- Other buffer types (`CharBuffer`, `IntBuffer`, ...) and the JDK internals (`address`, `MemorySegment`,
  `ScopedMemoryAccess`, `Unsafe`, `BufferMismatch`, `Preconditions`). Do not edit `ByteOrder.java`.

## JDK behaviour to match

Buffer state (`Buffer`):
- Invariant `-1 <= mark <= position <= limit <= capacity`. The constructor checks `cap < 0` (throw
  `createCapacityException(cap)`), sets `capacity`, then calls `limit(lim)` and `position(pos)`, then sets `mark` if
  `mark >= 0`.
- `createCapacityException(c)`: `new IllegalArgumentException("capacity < 0: (" + c + " < 0)")`.
- `position(p)`: `p > limit` throws `IllegalArgumentException("newPosition > limit: (17 > 16)")`, `p < 0` throws
  `IllegalArgumentException("newPosition < 0: (-1 < 0)")`; then `if (mark > p) mark = -1; position = p;`.
- `limit(l)`: `l > capacity` throws `IllegalArgumentException("newLimit > capacity: (17 > 16)")`, `l < 0` throws
  `IllegalArgumentException("newLimit < 0: (-2 < 0)")`; then `limit = l; if (position > l) position = l;
  if (mark > l) mark = -1;`.
- `mark()`: `mark = position`. `reset()`: `mark < 0` throws `new InvalidMarkException()` (message `null`), else
  `position = mark`. `clear()`: `position = 0; limit = capacity; mark = -1`. `flip()`: `limit = position;
  position = 0; mark = -1`. `rewind()`: `position = 0; mark = -1`.
- `remaining()`: `limit - position`, never below 0. `hasRemaining()`: `position < limit`.
- `nextGetIndex(nb)`: if `limit - position < nb` throw `new BufferUnderflowException()`, else return the old
  position and advance it by `nb`. `nextPutIndex(nb)`: the same with `new BufferOverflowException()`. On failure the
  position does not move. `checkIndex(i, nb)`: if `i < 0 || nb > limit - i` throw `new IndexOutOfBoundsException()`
  (**no message**; absolute indexes are checked against the **limit**, not the capacity), else return `i`.

Construction (`ByteBuffer`):
- `allocate(c)`: `c < 0` throws `createCapacityException(c)`; else a writable buffer on `new byte[c]`, position 0,
  limit = capacity = `c`, offset 0, mark -1.
- `wrap(a, off, len)`: reads `a.length` first (so `wrap(null)` throws `NullPointerException`); if `off < 0 ||
  off > a.length || len < 0 || len > a.length - off` throw `new IndexOutOfBoundsException()` (no message). Else a
  writable buffer on `a` itself (no copy) with capacity `a.length`, **position `off`, limit `off + len`, offset 0**
  (`arrayOffset()` is `0`, not `off`). `wrap(a)` is `wrap(a, 0, a.length)`.

Elements:
- `get()`/`put(b)` use `nextGetIndex(1)`/`nextPutIndex(1)`; `get(i)`/`put(i, b)` use `checkIndex(i, 1)`. The array
  slot is `hb[offset + index]`. Every put on a read-only buffer throws `new ReadOnlyBufferException()` **before** any
  index check.
- `get(dst, off, len)`: range check `off`, `len` against `dst.length` (below), then if `len > limit - position` throw
  `BufferUnderflowException` (nothing copied, position unchanged), else copy and advance. `get(dst)` is
  `get(dst, 0, dst.length)` (`null` gives `NullPointerException`).
- `put(src, off, len)`: read-only check first, then the range check against `src.length`, then
  `BufferOverflowException` if `len > limit - position`, else copy and advance. `put(src)` is `put(src, 0, src.length)`.
- Range check message (JDK `Objects.checkFromIndexSize`, which the runtime does not have, so write it yourself as a
  static helper): if `(length | from | size) < 0 || size > length - from` throw `new IndexOutOfBoundsException("Range ["
  + from + ", " + from + " + " + size + ") out of bounds for length " + length)`, e.g.
  `Range [4, 4 + 3) out of bounds for length 6` and `Range [1, 1 + -1) out of bounds for length 6`.
- `put(ByteBuffer src)`: in this order: `src == this` throws `new IllegalArgumentException("The source buffer is this
  buffer")`; read-only throws `ReadOnlyBufferException`; `n = max(src.limit() - src.position(), 0)` (a `null` src
  throws `NullPointerException` here); if `n > remaining()` throw `BufferOverflowException` (neither position moves);
  copy the `n` bytes, then advance both positions by `n`. Source and target may share one array (a duplicate) with
  overlapping ranges: copy the source bytes into a temporary `byte[n]` first. The fixture copies `[3 4 5 6]` from
  index 2 to index 0 of the same array.

Typed values:
- `order()` returns `ByteOrder.BIG_ENDIAN` when `bigEndian`, else `ByteOrder.LITTLE_ENDIAN`. `order(bo)` sets
  `bigEndian = (bo == ByteOrder.BIG_ENDIAN)` and returns `this`, so **`order(null)` means little-endian** (no
  exception). Every new buffer is big-endian: `allocate`, `wrap`, and also `slice()`, `duplicate()` and
  `asReadOnlyBuffer()` of a little-endian buffer. Never use `ByteOrder.nativeOrder()`.
- Relative `getX()`/`putX(v)` use `nextGetIndex(n)`/`nextPutIndex(n)` and absolute `getX(i)`/`putX(i, v)` use
  `checkIndex(i, n)` with `n` = 2 (short, char), 4 (int), 8 (long). So a relative read with too few bytes left
  throws `BufferUnderflowException` and does not move, and `getInt(i)` needs `i + 4 <= limit` or throws
  `IndexOutOfBoundsException` without a message.
- Big-endian stores the most significant byte at the lowest index; little-endian the least significant. Assemble with
  `& 0xff` per byte; `getShort` sign-extends (`(short)`), `getChar` does not (`(char)`).
- `putX` on a read-only buffer throws `ReadOnlyBufferException` before the index check.

Array access:
- `hasArray()`: `hb != null && !isReadOnly`. `array()` and `arrayOffset()` throw `new ReadOnlyBufferException()` on a
  read-only buffer, else return `hb` (the same array object) and `offset`.

Views and compact (`HeapByteBuffer`):
- `slice()`: shares `hb`, mark -1, position 0, limit = capacity = `remaining()`, offset `offset + position()`, same
  read-only flag, big-endian. Writes through either buffer are visible in the other.
- `duplicate()`: shares `hb`; same mark, position, limit, capacity, offset and read-only flag; big-endian. Positions
  are independent afterwards.
- `asReadOnlyBuffer()`: like `duplicate()` but read-only (keeps the mark: `reset()` works on it). `slice()` and
  `duplicate()` of a read-only buffer are read-only.
- `compact()`: read-only throws `ReadOnlyBufferException`; else move the `remaining()` bytes from `position` to the
  start (`System.arraycopy(hb, offset + pos, hb, offset, rem)`), then `position(rem)`, `limit(capacity())` and discard
  the mark (a later `reset()` throws `InvalidMarkException`).

Object methods (`ByteBuffer`):
- `toString()`: `java.nio.HeapByteBuffer[pos=0 lim=16 cap=16]` for writable buffers and
  `java.nio.HeapByteBufferR[pos=4 lim=6 cap=8]` for read-only ones (the JDK prints `getClass().getName()`; here
  write the two names as string literals, because this runtime has a single heap class).
- `hashCode()`: `h = 1`, then for `i` from `limit() - 1` **down to** `position()`: `h = 31 * h + get(i)` (the signed
  byte value). Depends only on the remaining bytes.
- `equals(ob)`: `true` for `this`; `false` unless `ob instanceof ByteBuffer`; `false` unless both remaining counts are
  equal (and not negative); then compare the remaining bytes pairwise. Byte order and the read-only flag do not matter.
- `compareTo(that)`: `length = min(thisRem, thatRem)`; `-1` if `length < 0`; at the first differing pair return
  `x - y` (the **difference of the signed bytes**, e.g. `107` for 7 vs -100, `8` for 5 vs -3); if no difference,
  `thisRem - thatRem`. Do **not** use the runtime's `Byte.compare`: it returns only -1/0/1, while the JDK's returns
  `x - y`. `compareTo(null)` throws `NullPointerException`.

## Steps

1. Read the test, then `java/nio/ByteOrder.java`, `java/util/NoSuchElementException.java` (exception style) and
   `java/lang/UnsupportedOperationException.java`, all under `javaw-bytecode/src/main/resources/exception-runtime/`.
2. Create the four exception files, `Buffer.java`, `ByteBuffer.java` and `HeapByteBuffer.java` in
   `javaw-bytecode/src/main/resources/exception-runtime/java/nio/` with the members above.
3. Register all seven (do not edit `BytecodeExceptionRuntime.java` by hand):
   - `javaw-agent-kit/bin/kit register java/nio/Buffer.java`
   - `javaw-agent-kit/bin/kit register java/nio/ByteBuffer.java`
   - `javaw-agent-kit/bin/kit register java/nio/HeapByteBuffer.java`
   - `javaw-agent-kit/bin/kit register java/nio/BufferUnderflowException.java`
   - `javaw-agent-kit/bin/kit register java/nio/BufferOverflowException.java`
   - `javaw-agent-kit/bin/kit register java/nio/InvalidMarkException.java`
   - `javaw-agent-kit/bin/kit register java/nio/ReadOnlyBufferException.java`
4. `javaw-agent-kit/bin/kit check T74` until PASS.
5. Write `docs/bytecode-byte-buffer.md` (below), the report, then `javaw-agent-kit/bin/kit finish T74`.

## Hints

- All classes are in `java.nio`; `ByteOrder` is in the same package. No imports are needed (`Math.min`,
  `System.arraycopy`, `IndexOutOfBoundsException`, `IllegalArgumentException` are `java.lang`).
- The covariant overrides in `ByteBuffer` (`public ByteBuffer position(int p) { super.position(p); return this; }`
  and the same for `limit`, `mark`, `reset`, `clear`, `flip`, `rewind`) make javac emit bridge methods
  `position(I)Ljava/nio/Buffer;` etc. in `ByteBuffer`, which is what Java 8 compiled libraries call. Do not make the
  `Buffer` versions `final`, and do not write the bridges by hand.
- Keep the absolute index check against `limit`, the relative check as `limit - position < nb`, and throw the
  exceptions with no message (only the range check, the capacity/position/limit messages and the "same buffer"
  message have text).
- Put the typed accessors in `ByteBuffer` on top of the abstract `get(int)`/`put(int, byte)`: one private helper that
  reads `nb` bytes into a `long` (honouring `bigEndian`) and one that writes `nb` bytes, plus a read-only check in each
  `putX` before the index check. The whole reference solution was about 290 lines.
- `array()`/`arrayOffset()` return the field values: a buffer from `wrap(a, 1, 3)` has `arrayOffset() == 0` and
  position 1, while `slice()` of a buffer at position 2 has `arrayOffset() == 2` and position 0.
- Easy mistakes: using `Byte.compare` in `compareTo` (gives `1` instead of `107`); `order(null)` throwing or keeping
  big-endian; `duplicate()`/`slice()`/`asReadOnlyBuffer()` copying the source's byte order (the JDK resets it to
  big-endian); `wrap(a, off, len)` setting the offset to `off`; checking absolute indexes against `capacity`;
  `put(ByteBuffer)` corrupting bytes when the two buffers share one array.
- The Wasm runner stops at the first failing fixture of each `@Test` method; after a fix another fixture may fail next.
  When Wasm throws where the JVM did not, the failure shows `(thrown in Wasm: <Java exception>)`.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
positionLimitAndMark, relativeAndAbsoluteBytes, bulkTransfers, typedValuesAndByteOrder, viewsReadOnlyAndCompact, equalityAndOrdering, leavesOutDirectBuffersViewsAndFloatingPoint: if FIX THE RUNTIME says `link the symbolic class/interface java/nio/ByteBuffer` (or `java/nio/Buffer`, `HeapByteBuffer` or one of the four exceptions), `link a complete acyclic type hierarchy` or names one of their methods, a class is missing or not registered: run `javaw-agent-kit/bin/kit register` for each of the seven files (never edit BytecodeExceptionRuntime.java by hand) and add every member listed under Members to implement. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
positionLimitAndMark: position(int) and limit(int) check the upper bound first and use the exact JDK messages (`newPosition > limit: (17 > 16)`, `newLimit < 0: (-2 < 0)`); limit() lowers position and discards a mark above the new limit; position() discards a mark above the new position; flip/rewind/clear discard the mark; reset without a mark throws `new InvalidMarkException()`.
relativeAndAbsolute: absolute get(i)/put(i,b) check `i < 0 || i >= limit` (the limit, not the capacity) and throw `new IndexOutOfBoundsException()` with no message; wrap(a, off, len) keeps arrayOffset 0 and sets position off and limit off+len, and throws IndexOutOfBoundsException without a message for a bad range.
bulkTransfers: get/put with (array, off, len) throw IndexOutOfBoundsException `Range [off, off + len) out of bounds for length n` before the underflow/overflow check; put(ByteBuffer) checks `src == this` (IllegalArgumentException `The source buffer is this buffer`) first and must copy correctly when both buffers share one array (copy via a temporary array).
typedValues: big-endian puts the most significant byte first; order(null) selects LITTLE_ENDIAN; relative typed reads with too few bytes throw BufferUnderflowException (writes BufferOverflowException) without moving; absolute typed access needs index + size <= limit, else IndexOutOfBoundsException without a message.
viewsAndCompact: slice() has offset = offset + position, mark -1 and capacity = remaining; duplicate() and asReadOnlyBuffer() keep mark/position/limit; all three views are BIG_ENDIAN even when the source is little-endian; read-only array()/arrayOffset()/every put/compact throw ReadOnlyBufferException and toString prints `java.nio.HeapByteBufferR[...]`; compact discards the mark.
equalityAndOrdering: compareTo returns the difference `x - y` of the first differing signed bytes (not Byte.compare, which gives only -1/0/1), else thisRemaining - thatRemaining; hashCode walks from limit-1 down to position with h = 31*h + byte starting at 1; equals ignores byte order and read-only.
leavesOutDirectBuffersViewsAndFloatingPoint: this fails when allocateDirect, asLongBuffer, getFloat or slice(int,int) became linked. Do not add them.
```

## Docs

Create `docs/bytecode-byte-buffer.md` with this skeleton (fill in the brackets):

    # Bytecode ByteBuffer

    Revision `byte-buffer-vN` links `java.nio.Buffer`, `java.nio.ByteBuffer` (heap buffers through a package-private
    `HeapByteBuffer`), `BufferUnderflowException`, `BufferOverflowException`, `InvalidMarkException` and
    `ReadOnlyBufferException` in the bundled exception runtime.

    Contract:

    - <mark/position/limit/capacity rules and messages; allocate and wrap; relative and absolute byte access; bulk
      transfers with range checks; short/char/int/long access in either byte order (views reset to big-endian);
      slice/duplicate/asReadOnlyBuffer/compact; toString, hashCode, equals and compareTo as in JDK 21; covariant
      Buffer-returning bridges for Java 8 callers>

    Known differences: one heap class serves writable and read-only buffers, so `getClass()` of a read-only buffer is
    `java.nio.HeapByteBuffer` (toString still prints `HeapByteBufferR`). No direct or mapped buffers, view buffers,
    float/double accessors or absolute bulk methods.

    Evidence: `BytecodeByteBufferTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
    fresh instances). `ByteBuffer.allocateDirect`, `asLongBuffer`, `getFloat` and `slice(int, int)` are rejected
    before WAT generation.
