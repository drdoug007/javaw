# T79 java.nio.CharBuffer heap and CharSequence core

```kit
id: T79
slug: char-buffer
title: link java.nio.CharBuffer (heap and CharSequence-backed buffers)
tests: BytecodeCharBufferTest
depends: T74
allow: javaw-bytecode/src/main/resources/exception-runtime/java/nio/CharBuffer.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/nio/HeapCharBuffer.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/nio/StringCharBuffer.java
allow: docs/bytecode-char-buffer.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-char-buffer-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeCharBufferTest.java`.

## Goal

Add `java.nio.CharBuffer` on top of the T74 `java.nio.Buffer`, with two package-private implementations:
`HeapCharBuffer` (a `char[]`, writable or read-only through a flag) and `StringCharBuffer` (a read-only view of a
`CharSequence`, from `wrap(CharSequence)`). `Buffer`, `ByteOrder` and the four buffer exceptions already exist and
are not changed. `runtime-target/target/audit/member-references.tsv` lists 63 `java/nio/CharBuffer` rows in 6
artifacts (H2, ANTLR, Tomcat core and websocket, Spring core, SnakeYAML). The largest by reference count:
`position(I)Ljava/nio/CharBuffer;` 39 (Tomcat core), `position()I` 34, `remaining()I` 35, `array()[C` 22 (Tomcat, ANTLR,
H2), `arrayOffset()I` 17, `limit()I` 17, `capacity()I` 14, `allocate(I)` 14 (H2, ANTLR, Tomcat core and websocket),
`flip()Ljava/nio/CharBuffer;` 10, `limit(I)Ljava/nio/CharBuffer;` 10, `clear()Ljava/nio/CharBuffer;` 9 (Tomcat
websocket), `get()C` 6, `wrap([CII)` 5, `wrap(Ljava/lang/CharSequence;)` 5 (Tomcat, Spring core), `toString()` 4
(Tomcat websocket, SnakeYAML), `compact()` 4, `put(C)` 4, `put([CII)` 3, `hasArray()` 3, `get([CII)` 2,
`put(Ljava/nio/CharBuffer;)` 2, `hasRemaining()` 2, and one each of `get(I)C`, `length()I`, `wrap([C)`, `slice()`,
`put(Ljava/lang/String;)`, `put(Ljava/lang/String;II)`. Libraries compiled for Java 8 (H2, ANTLR) call
`CharBuffer.flip()Ljava/nio/Buffer;` 8 times, `position(I)Ljava/nio/Buffer;` 9, `limit(I)Ljava/nio/Buffer;` 1 and
`rewind()Ljava/nio/Buffer;` 1; the covariant overrides below give both descriptors. The `CharSequence` and
`Appendable` members, `duplicate`, `asReadOnlyBuffer`, `subSequence`, `equals`/`hashCode`/`compareTo` and `order()`
have no audit rows but are part of the class's interfaces and contract, so they are included.

## Members to implement

All files are under `javaw-bytecode/src/main/resources/exception-runtime/java/nio/` (`package java.nio;`).

1. `CharBuffer.java`: `public abstract class CharBuffer extends Buffer implements Comparable<CharBuffer>,
   Appendable, CharSequence` (not `Readable`, not `Serializable`) with
   - fields `final char[] hb; final int offset; final boolean isReadOnly;` (`hb` is `null` for a `StringCharBuffer`)
   - package-private constructor `CharBuffer(int mark, int pos, int lim, int cap, char[] hb, int offset, boolean readOnly)`
   - `public static CharBuffer allocate(int capacity)`, `wrap(char[] array, int offset, int length)`,
     `wrap(char[] array)`, `wrap(CharSequence csq, int start, int end)`, `wrap(CharSequence csq)` (all `public static`)
   - abstract: `public abstract CharBuffer slice()`, `duplicate()`, `asReadOnlyBuffer()`, `public abstract char get()`,
     `public abstract CharBuffer put(char c)`, `public abstract char get(int index)`,
     `public abstract CharBuffer put(int index, char c)`, `public abstract CharBuffer compact()`,
     `public abstract CharBuffer subSequence(int start, int end)`, `public abstract ByteOrder order()`,
     and a package-private `abstract String toString(int start, int end)`
   - `public CharBuffer get(char[] dst, int offset, int length)`, `public CharBuffer get(char[] dst)`
   - `public CharBuffer put(CharBuffer src)`, `public CharBuffer put(char[] src, int offset, int length)`,
     `public final CharBuffer put(char[] src)`, `public CharBuffer put(String src, int start, int end)`,
     `public final CharBuffer put(String src)`
   - `public final boolean hasArray()`, `public final char[] array()`, `public final int arrayOffset()`,
     `public boolean isDirect()`
   - covariant overrides returning `CharBuffer`, all `public final`: `position(int)`, `limit(int)`, `mark()`, `reset()`,
     `clear()`, `flip()`, `rewind()` (each `super.x(...); return this;`)
   - `CharSequence`: `public final int length()`, `public final boolean isEmpty()`, `public final char charAt(int index)`,
     `public String toString()` (and `subSequence` above; `chars()` is the existing `CharSequence` default, do not
     override it)
   - `Appendable`: `public CharBuffer append(CharSequence csq)`, `public CharBuffer append(CharSequence csq, int start,
     int end)`, `public CharBuffer append(char c)` (no `throws` clause)
   - `public int hashCode()`, `public boolean equals(Object ob)`, `public int compareTo(CharBuffer that)`

2. `HeapCharBuffer.java`: `final class HeapCharBuffer extends CharBuffer` (package-private) with the constructor
   `HeapCharBuffer(char[] hb, int mark, int pos, int lim, int cap, int offset, boolean readOnly)` and `isReadOnly()`,
   `order()`, `slice()`, `duplicate()`, `asReadOnlyBuffer()`, `get()`, `get(int)`, `put(char)`, `put(int, char)`,
   `compact()`, `subSequence(int, int)`, `toString(int, int)`. One class serves writable and read-only heap buffers.

3. `StringCharBuffer.java`: `final class StringCharBuffer extends CharBuffer` (package-private) with a field
   `final CharSequence str`, the constructor `StringCharBuffer(CharSequence str, int mark, int pos, int lim, int cap,
   int offset)` (passes `hb = null`, `readOnly = true`) and `isReadOnly()`, `order()`, `slice()`, `duplicate()`,
   `asReadOnlyBuffer()`, `get()`, `get(int)`, `put(char)`, `put(int, char)`, `compact()`, `subSequence(int, int)`,
   `toString(int, int)`.

## Members to leave out

- `read(CharBuffer)` and the `Readable` interface (the runtime has no `java.lang.Readable`, and `Reader` does not
  implement it; the test checks `CharBuffer.read` stays unlinked). Do not add `Readable.java`.
- The JDK 13+ absolute bulk members `slice(int, int)` (checked), `get(int, char[])` (checked),
  `get(int, char[], int, int)`, `put(int, char[])`, `put(int, char[], int, int)`, `put(int, CharBuffer, int, int)`,
  and `mismatch(CharBuffer)` (checked): a later task.
- Direct buffers and byte-buffer views: `ByteBuffer.asCharBuffer()` stays unlinked (checked; do not edit
  `ByteBuffer.java`), no `DirectCharBuffer`/`ByteBufferAsCharBuffer` classes. The `CharsetEncoder`/`CharsetDecoder`
  interplay is a later task. No `chars()` override and no `CharBufferSpliterator`.
- Do not edit `Buffer.java`, `ByteOrder.java`, `ByteBuffer.java` or the exception classes.

## JDK behaviour to match

Buffer state is inherited from `Buffer` (T74): position/limit/mark rules, messages such as `newPosition > limit:
(6 > 5)`, `nextGetIndex`/`nextPutIndex`/`checkIndex` (absolute indexes are checked against the **limit**, no message).

Construction (`CharBuffer`):
- `allocate(c)`: `c < 0` throws `createCapacityException(c)` (`capacity < 0: (-2 < 0)`); else a writable heap buffer on
  `new char[c]`, position 0, limit = capacity = `c`, offset 0. Its `toString()` is `c` NUL chars (`'\0'`), not empty.
- `wrap(a, off, len)`: reads `a.length` first (`wrap((char[])null)` throws `NullPointerException`); `off < 0 ||
  off > a.length || len < 0 || len > a.length - off` throws `new IndexOutOfBoundsException()` (no message); else a
  writable buffer on `a` itself with capacity `a.length`, **position `off`, limit `off + len`, offset 0**.
  `wrap(a)` is `wrap(a, 0, a.length)`.
- `wrap(csq, start, end)`: reads `csq.length()` first (`null` gives `NullPointerException`); `start < 0 ||
  start > end || end > csq.length()` throws `new IndexOutOfBoundsException()` (no message); else a
  `StringCharBuffer` with position `start`, limit `end`, capacity `csq.length()`, offset 0, mark -1. It reads the
  sequence live: later changes to a wrapped `StringBuilder` are visible. `wrap(csq)` is `wrap(csq, 0, csq.length())`.
- `order()` returns `ByteOrder.nativeOrder()` for both classes (the JVM fixture compares with `nativeOrder()`).
  `isDirect()` is `false`.

Elements:
- Heap: `get()`/`put(c)` use `nextGetIndex(1)`/`nextPutIndex(1)`, `get(i)`/`put(i, c)` use `checkIndex(i, 1)`; the
  slot is `hb[offset + index]`. String: `get()` is `str.charAt(offset + nextGetIndex(1))`, `get(i)` is
  `str.charAt(offset + checkIndex(i, 1))`.
- `charAt(i)` is **relative to the position**: `get(position() + checkIndex(i, 1))`. `length()` is `remaining()`,
  `isEmpty()` is `remaining() == 0`. Out-of-range `charAt` throws `IndexOutOfBoundsException` with no message.
- `toString()` returns the **remaining chars** (position to limit) as a `String`, not a `java.nio...[pos=...]`
  description: heap `new String(hb, offset + start, end - start)`, string `str.subSequence(offset + start,
  offset + end).toString()`. It does not move the position.

Bulk transfers (`CharBuffer`):
- Range check helper (JDK `Objects.checkFromIndexSize`, which the runtime does not have; write it as a static helper,
  the same as `ByteBuffer.checkRange`): if `(length | from | size) < 0 || size > length - from` throw
  `new IndexOutOfBoundsException("Range [" + from + ", " + from + " + " + size + ") out of bounds for length " +
  length)`, e.g. `Range [4, 4 + 3) out of bounds for length 6`.
- `get(dst, off, len)`: range check against `dst.length`, then `len > limit - position` throws
  `BufferUnderflowException` (nothing copied), else copy `get(position + i)` for each `i` and advance.
  `get(dst)` is `get(dst, 0, dst.length)`. This must work for string buffers too, so read through `get(int)`.
- `put(src, off, len)` (char array): read-only throws `ReadOnlyBufferException` first, then the range check against
  `src.length`, then `BufferOverflowException` if `len > remaining`, else copy and advance.
- `put(String src, int start, int end)`: range check with `from = start`, `size = end - start` and `length =
  src.length()` (so `put("abc", 2, 1)` gives `Range [2, 2 + -1) out of bounds for length 3`), then `ReadOnlyBufferException`,
  then `BufferOverflowException` if `end - start > remaining`, else store the chars and advance. **Exception: a
  read-only heap buffer throws `ReadOnlyBufferException` before the range check** (JDK `HeapCharBufferR` overrides
  it); a string buffer does the range check first. `put(src)` is `put(src, 0, src.length())` (`null` gives
  `NullPointerException`).
- `put(CharBuffer src)`: a read-only heap buffer throws `ReadOnlyBufferException` first (even for `put(itself)`); then
  `src == this` throws `new IllegalArgumentException("The source buffer is this buffer")` (so a string buffer's
  `put(itself)` gives this message); then a read-only string buffer throws `ReadOnlyBufferException`;
  `n = max(src.limit() - src.position(), 0)` (`null` src throws `NullPointerException`); `n > remaining` throws
  `BufferOverflowException` (neither position moves); else copy and advance both positions by `n`. The source may be a
  string buffer, or a duplicate sharing the same array with overlapping ranges in either direction: read the `n`
  source chars into a temporary `char[n]` first, then store them.

Appendable (`CharBuffer`):
- `append(char c)` is `put(c)`.
- `append(csq)`: a read-only heap buffer throws `ReadOnlyBufferException`; `null` appends `"null"`; a `CharBuffer`
  argument is `put(that buffer)` (its position advances); anything else is `put(csq.toString())`.
- `append(csq, start, end)`: a read-only heap buffer throws `ReadOnlyBufferException`; `null` means `"null"`; if
  `start < 0 || start > end || end > csq.length()` throw `new IndexOutOfBoundsException("Range [" + start + ", " + end +
  ") out of bounds for length " + csq.length())` (e.g. `Range [2, 1) out of bounds for length 3`); else
  `put(csq.subSequence(start, end).toString())`. Overflow gives `BufferOverflowException` from `put`.
- Through the `Appendable` interface the same methods run (javac emits the `Appendable`-returning bridges).

Array access:
- `hasArray()`: `hb != null && !isReadOnly`. `array()` and `arrayOffset()`: `hb == null` throws
  `new UnsupportedOperationException()` (string buffers), then read-only throws `new ReadOnlyBufferException()`, else
  return `hb` (the same array) and `offset`.

Views, subSequence and compact:
- `slice()`: shares the storage, mark -1, position 0, limit = capacity = `remaining()`, offset `offset + position()`,
  same read-only flag (a string slice stays a string buffer).
- `duplicate()`: same mark, position, limit, capacity, offset and read-only flag; positions independent afterwards.
- `asReadOnlyBuffer()`: like `duplicate()` but read-only (keeps the mark). For a string buffer it is `duplicate()`.
- `subSequence(start, end)`: relative to the position. With `pos = position()` and `n = limit() - pos`:
  - heap: `start < 0 || start > end || end > n` throws `new IndexOutOfBoundsException("Range [" + start + ", " + end +
    ") out of bounds for length " + n)`, e.g. `Range [1, 3) out of bounds for length 2`;
  - string: the same condition throws `new IndexOutOfBoundsException()` with **no message**;
  - result: same storage, mark -1, **position `pos + start`, limit `pos + end`, the same capacity and offset** (not a
    slice: `arrayOffset()` is unchanged), same read-only flag and class.
- `compact()`: read-only (both classes) throws `ReadOnlyBufferException`; else move the remaining chars to the start,
  `position(rem)`, `limit(capacity())`, discard the mark.
- Every put/compact/append on a string buffer throws `ReadOnlyBufferException` (after the range check where described
  above). `isReadOnly()` is always `true` for it.

Object methods (`CharBuffer`):
- `hashCode()`: `h = 1`, then for `i` from `limit() - 1` **down to** `position()`: `h = 31 * h + get(i)`.
- `equals(ob)`: `true` for `this`; `false` unless `ob instanceof CharBuffer` (a `String` is never equal); both
  remaining counts equal (and not negative); then the remaining chars pairwise. Heap and string buffers with the same
  remaining chars are equal; the read-only flag does not matter.
- `compareTo(that)`: `length = min(thisRem, thatRem)`; `-1` if `length < 0`; at the first differing pair return
  `x - y` of the two chars (e.g. `57` for `'z'` vs `'A'`, `65438` for `'￿'` vs `'a'`); else `thisRem - thatRem`.
  `compareTo(null)` throws `NullPointerException`.

## Steps

1. Read the test, then `java/nio/Buffer.java`, `java/nio/ByteBuffer.java`, `java/nio/HeapByteBuffer.java` (T74; copy
   their structure) and `java/lang/CharSequence.java`, `java/lang/Appendable.java`, all under
   `javaw-bytecode/src/main/resources/exception-runtime/`.
2. Create `CharBuffer.java`, `HeapCharBuffer.java` and `StringCharBuffer.java` in
   `javaw-bytecode/src/main/resources/exception-runtime/java/nio/` with the members above.
3. Register all three (do not edit `BytecodeExceptionRuntime.java` by hand):
   - `javaw-agent-kit/bin/kit register java/nio/CharBuffer.java`
   - `javaw-agent-kit/bin/kit register java/nio/HeapCharBuffer.java`
   - `javaw-agent-kit/bin/kit register java/nio/StringCharBuffer.java`
4. `javaw-agent-kit/bin/kit check T79` until PASS.
5. Write `docs/bytecode-char-buffer.md` (below), the report, then `javaw-agent-kit/bin/kit finish T79`.

## Hints

- All classes are in `java.nio`; no imports are needed. `Buffer` gives `markValue()`, `discardMark()`,
  `nextGetIndex(int)`, `nextPutIndex(int)`, `checkIndex(int, int)` and `createCapacityException(int)`.
- The covariant overrides `public final CharBuffer position(int p) { super.position(p); return this; }` (and `limit`,
  `mark`, `reset`, `clear`, `flip`, `rewind`) make javac emit the `position(I)Ljava/nio/Buffer;` bridges Java 8
  libraries call. Do not write the bridges by hand. The same goes for the `Appendable` bridges of the three
  `append` methods: declare them returning `CharBuffer` without `throws`.
- Write the bulk methods, `equals`, `hashCode` and `compareTo` in `CharBuffer` on top of the abstract `get(int)` and
  `put(int, char)`, so both subclasses share them. Only `get`/`put`/`compact`/`slice`/`duplicate`/`asReadOnlyBuffer`/
  `subSequence`/`toString(int, int)`/`order`/`isReadOnly` differ. The whole reference solution was about 280 lines.
- The read-only checks differ between the two classes (calibration hit this): a read-only **heap** buffer throws
  `ReadOnlyBufferException` at the start of `put(CharBuffer)` (also for `put(itself)`), `put(String, int, int)` and
  both `append(CharSequence...)`; a **string** buffer runs the `src == this` check and the range checks first. In
  `CharBuffer`, `isReadOnly && hb != null` means "read-only heap buffer".
- `wrap(csq, start, end)` must not copy the sequence: `StringCharBuffer.get` calls `str.charAt` each time.
- `toString()` is the remaining text; `charAt(i)` and `subSequence` are relative to the position, while `get(i)` is
  absolute.
- Easy mistakes: `subSequence` returning a slice (it keeps capacity and offset, only position/limit move); a message
  on the string `subSequence` exception; using `Character.compare` without checking (the runtime's returns `x - y`, the
  same as the JDK; plain `x - y` is simplest); `array()` on a string buffer throwing `ReadOnlyBufferException` instead
  of `UnsupportedOperationException`; `put(CharBuffer)` corrupting chars when both buffers share one array.
- The Wasm runner stops at the first failing fixture of each `@Test` method; after a fix another fixture may fail next.
  When Wasm throws where the JVM did not, the failure shows `(thrown in Wasm: <Java exception>)`.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
allocateAndWrap, positionAndElements, bulkTransfers, viewsReadOnlyAndCompact, charSequenceBuffers, appendEqualityAndOrdering, leavesOutReadableAbsoluteBulkAndViews: if FIX THE RUNTIME says `link the symbolic class/interface java/nio/CharBuffer` (or `HeapCharBuffer`, `StringCharBuffer`), `link a complete acyclic type hierarchy` or names one of their methods, a class is missing or not registered: run `javaw-agent-kit/bin/kit register` for each of the three files (never edit BytecodeExceptionRuntime.java by hand) and add every member listed under Members to implement. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
allocateAndWrap: wrap(char[], off, len) keeps arrayOffset 0 with position off and limit off+len; wrap(CharSequence, start, end) has capacity csq.length(), throws IndexOutOfBoundsException without a message for a bad range, and array()/arrayOffset() on it throw UnsupportedOperationException (hb is null); allocate(n).toString() is n NUL chars.
positionAndElements: charAt(i) is get(position() + checkIndex(i, 1)) (relative to the position), length() is remaining(), toString() returns the remaining chars; absolute get(i)/put(i, c) check against the limit and throw IndexOutOfBoundsException without a message.
bulkTransfers: put(String, start, end) checks `Range [start, start + (end - start)) out of bounds for length n` before the overflow check; put(CharBuffer) checks `src == this` (`The source buffer is this buffer`) and must copy through a temporary char[] so overlapping duplicates (in either direction) and string sources work.
viewsAndCompact: subSequence(start, end) keeps capacity and offset and sets position pos+start, limit pos+end (heap message `Range [start, end) out of bounds for length remaining`); a read-only heap buffer throws ReadOnlyBufferException first in put(CharBuffer) (also put(itself)), put(String...), append(...) and compact.
sequenceBuffers: StringCharBuffer reads str.charAt(offset + index) live (no copy); its subSequence throws IndexOutOfBoundsException with no message; put(itself) gives IllegalArgumentException `The source buffer is this buffer` and put("abc", 2, 1) gives the Range message before ReadOnlyBufferException; get(char[]) reads through get(int).
appendAndCompare: append(null) appends "null"; append(csq, start, end) throws `Range [start, end) out of bounds for length n` for a bad range; compareTo returns x - y of the first differing chars (65438 for '￿' vs 'a'), else thisRemaining - thatRemaining; hashCode walks from limit-1 down to position with h = 31*h + c starting at 1; equals compares only the remaining chars of any two CharBuffers.
leavesOutReadableAbsoluteBulkAndViews: this fails when CharBuffer.read(CharBuffer), slice(int,int), get(int,char[]), mismatch or ByteBuffer.asCharBuffer became linked. Do not add them.
```

## Docs

Create `docs/bytecode-char-buffer.md` with this skeleton (fill in the brackets):

    # Bytecode CharBuffer

    Revision `char-buffer-vN` links `java.nio.CharBuffer` (heap buffers through a package-private `HeapCharBuffer`,
    `CharSequence` views through a package-private read-only `StringCharBuffer`) in the bundled exception runtime.

    Contract:

    - <allocate and the wrap overloads; relative and absolute char access; CharSequence members relative to the
      position; bulk transfers with range checks; put(String) and put(CharBuffer); Appendable; slice/duplicate/
      asReadOnlyBuffer/subSequence/compact; toString, hashCode, equals and compareTo as in JDK 21; order() is
      nativeOrder; covariant Buffer-returning bridges for Java 8 callers>

    Known differences: one heap class serves writable and read-only buffers, so `getClass()` of a read-only heap
    buffer is `java.nio.HeapCharBuffer`. No `Readable`/`read(CharBuffer)`, direct or byte-buffer views, absolute bulk
    methods or `mismatch`.

    Evidence: `BytecodeCharBufferTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
    fresh instances). `CharBuffer.read`, `slice(int, int)`, `get(int, char[])`, `mismatch` and
    `ByteBuffer.asCharBuffer` are rejected before WAT generation.
