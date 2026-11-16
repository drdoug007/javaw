# T33 BufferedInputStream and BufferedOutputStream

```kit
id: T33
slug: buffered-streams
title: link java.io.BufferedInputStream and java.io.BufferedOutputStream
tests: BytecodeBufferedStreamsTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/io/BufferedInputStream.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/io/BufferedOutputStream.java
allow: docs/bytecode-buffered-streams.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-buffered-streams-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeBufferedStreamsTest.java`.

## Goal

Add `java.io.BufferedInputStream` and `java.io.BufferedOutputStream` to the bundled runtime, on top of the existing
`java.io.FilterInputStream` (T25) and `java.io.FilterOutputStream` (T24). The pinned libraries name
`BufferedInputStream` from 22 classes and `BufferedOutputStream` from 27 (`runtime-target/target/audit/external-types.tsv`).
Member calls (`member-references.tsv`): `BufferedInputStream.<init>(InputStream)` 18, `close()` 13,
`<init>(InputStream,int)` 11, `read(byte[])` 5; `BufferedOutputStream.<init>(OutputStream)` 17, `close()` 11,
`<init>(OutputStream,int)` 11, `write(byte[],int,int)` 2, `write(byte[])` 2, `flush()` 1. H2 (`Transfer.init`,
`ScriptBase.openOutput`, `JdbcBlob.setBinaryStream`), Tomcat (`DefaultServlet.copy` / `serveResource`,
`ExpandWar.expand`, `CGIServlet$CGIRunner`, juli `FileHandler`), Logback (`Compressor.gzCompress`, `FileUtil.copy`,
`ResilientFileOutputStream`), AspectJ's bcel (`ClassParser`, `UnwovenClassFile`), jandex (`IndexReader`,
`IndexWriterV1`), Hibernate (`FileInputStreamAccess`), Spring (`DebuggingClassWriter`, `OutputStreamPublisher`,
`JettyClientHttpRequest`) and jakarta.xml.bind wrap their streams with them. The calls that reach the wrapped stream
(how many, with which array, offset and length) are observable, so they must match JDK 21 exactly.

## Members to implement

Two files under `javaw-bytecode/src/main/resources/exception-runtime/java/io/` (both `package java.io;`).

`BufferedInputStream.java`: `public class BufferedInputStream extends FilterInputStream` with

- `private static final int DEFAULT_BUFFER_SIZE = 8192;`
- `private final int initialSize;`
- `protected byte[] buf;` `protected int count;` `protected int pos;` `protected int markpos = -1;`
  `protected int marklimit;` (subclasses read them; the test's `Peek` subclass prints all five). **Not** `volatile`.
- `public BufferedInputStream(InputStream in)` (`this(in, DEFAULT_BUFFER_SIZE)`)
- `public BufferedInputStream(InputStream in, int size)`
- private helpers: `private InputStream getInIfOpen() throws IOException`, `private byte[] getBufIfOpen() throws IOException`,
  `private void ensureOpen() throws IOException`, `private void fill() throws IOException`,
  `private int read1(byte[] b, int off, int len) throws IOException`
- `public int read() throws IOException`
- `public int read(byte[] b, int off, int len) throws IOException`
- `public long skip(long n) throws IOException`
- `public int available() throws IOException`
- `public void mark(int readlimit)`
- `public void reset() throws IOException`
- `public boolean markSupported()`
- `public void close() throws IOException`
- `public long transferTo(OutputStream out) throws IOException`

`read(byte[] b)` is **not** overridden: the inherited `FilterInputStream.read(b)` is `read(b, 0, b.length)` on `this`.

`BufferedOutputStream.java`: `public class BufferedOutputStream extends FilterOutputStream` with

- `private static final int DEFAULT_MAX_BUFFER_SIZE = 8192;`
- `protected byte[] buf;` `protected int count;` (the test's `OSpy` subclass reads both and assigns `buf`)
- `private final int maxBufSize;`
- `public BufferedOutputStream(OutputStream out)` (`this(out, DEFAULT_MAX_BUFFER_SIZE)`)
- `public BufferedOutputStream(OutputStream out, int size)`
- private helpers: `private void flushBuffer() throws IOException`, `private void growIfNeeded(int len)`
- `public void write(int b) throws IOException`
- `public void write(byte[] b, int off, int len) throws IOException`
- `public void flush() throws IOException`

`write(byte[] b)` and `close()` are **not** overridden: the inherited `FilterOutputStream` versions are the JDK
behaviour (`write(b)` is `write(b, 0, b.length)` on `this`; `close()` calls the overridable `flush()` and then
`out.close()`, with the suppression rules of T24).

## Members to leave out

Nothing public. JDK 21 has no other public or protected members on these two classes. Do not add a
`write(byte[])` or `close()` override to `BufferedOutputStream`, nor `read(byte[])` to `BufferedInputStream`.
Do not edit any other runtime file (`kit guard` rejects it): `FilterInputStream`, `FilterOutputStream`,
`InputStream`, `OutputStream` and `java/util/Objects.java` stay as they are.

Known difference, not observable by the test: the JDK allocates the input buffer lazily (on the first `fill()`)
when the class is not subclassed and uses a 512-byte initial output buffer on virtual threads. Here both buffers are
allocated in the constructor with their full size, which is what JDK 21 does for subclasses and platform threads.

## JDK behaviour to match

The test's `Src` source logs `len@off/b.length=n` for each `read(byte[],int,int)` call, `av=n` for `available()`,
`sN=k` for `skip(N)`, `xfer` for `transferTo` and `close`. Its `Sink` logs `aLEN@OFF/LENGTH:chars` for
`write(byte[],int,int)`, `wN` for `write(int)`, `flush` and `close`.

BufferedInputStream:

- Constructor `(in, size)`: `super(in);` then `if (size <= 0) throw new IllegalArgumentException("Buffer size <= 0");`
  then `initialSize = size; buf = new byte[size];`. A `null` `in` is allowed (sizes 0 and -1 throw even then).
  With a `null` `in` (the test's `new Peek(null, 2)`), `buf` is a real array, so `ensureOpen()` passes, but every call
  that reaches the source (`read()`, `read(b, off, len)` with `len > 0`, `skip(n)` with `n > 0`, `available()`) throws
  `IOException("Stream closed")` from `getInIfOpen()`. `skip(0)` returns 0; `markSupported()` returns true;
  `mark(5)` then `reset()` work; `close()` sets `buf` to `null` and calls no source.
- `getInIfOpen()`: `InputStream input = in; if (input == null) throw new IOException("Stream closed"); return input;`
- `getBufIfOpen()`: `byte[] buffer = buf; if (buffer == null) throw new IOException("Stream closed"); return buffer;`
- `ensureOpen()`: `if (buf == null) throw new IOException("Stream closed");`
- `fill()`, exactly in this order:
  1. `byte[] buffer = getBufIfOpen();`
  2. `if (markpos == -1) pos = 0;` (compare with `== -1`, as JDK 21 does, not `< 0`)
  3. `else if (pos >= buffer.length)`: if `markpos > 0`, move the marked part to the front:
     `int sz = pos - markpos; System.arraycopy(buffer, markpos, buffer, 0, sz); pos = sz; markpos = 0;`;
     else if `buffer.length >= marklimit`, drop the mark: `markpos = -1; pos = 0;`; else grow:
     `int nsz = pos + Math.max(1, pos); if (nsz < 0 || nsz > Integer.MAX_VALUE - 8) nsz = Integer.MAX_VALUE - 8;` (so a zero-length `buf` still grows to one byte, as `ArraysSupport.newLength(pos, 1, pos)` does)
     `if (nsz > marklimit) nsz = marklimit;` then a new `byte[nsz]` with the first `pos` bytes copied, stored in
     `buf` and in `buffer`.
  4. `count = pos;` (before the read, so a throwing source leaves `count == pos`)
  5. `int n = getInIfOpen().read(buffer, pos, buffer.length - pos); if (n > 0) count = n + pos;`
- `read()`: `if (pos >= count) { fill(); if (pos >= count) return -1; } return getBufIfOpen()[pos++] & 0xff;`.
  Each `read()` at the end of the input calls `fill()` again (the source sees another `4@0/4=-1`).
  `readSingleBytes` with size 4 over a source that hands out 3 bytes per call logs
  `4@0/4=3 4@0/4=3 4@0/4=3 4@0/4=1 4@0/4=-1 4@0/4=-1` for 12 `read()` calls on 10 bytes.
- `read1(b, off, len)`: `int avail = count - pos;` if `avail <= 0`: `int size = Math.max(getBufIfOpen().length, initialSize);`
  and `if (len >= size && markpos == -1) return getInIfOpen().read(b, off, len);` (the direct read into the
  caller's array, no copy); otherwise `fill(); avail = count - pos; if (avail <= 0) return -1;`. Then
  `int cnt = (avail < len) ? avail : len; System.arraycopy(getBufIfOpen(), pos, b, off, cnt); pos += cnt; return cnt;`
- `read(b, off, len)`:
  1. `ensureOpen();` first (a closed stream throws `IOException("Stream closed")` even for `read(null, 0, 0)`).
  2. `if ((off | len | (off + len) | (b.length - (off + len))) < 0) throw new IndexOutOfBoundsException();` with
     **no message** (`getMessage()` is `null`). A `null` array throws `NullPointerException` here.
     On a length-4 array `(-1,1)`, `(0,-1)`, `(3,2)`, `(5,0)` and `(1, Integer.MAX_VALUE)` all throw it; `(4,0)` returns 0.
  3. `if (len == 0) return 0;` (no source call)
  4. `int n = 0;` then loop: `int nread = read1(b, off + n, len - n); if (nread <= 0) return (n == 0) ? nread : n;`
     `n += nread; if (n >= len) return n;` then `InputStream input = in; if (input != null && input.available() <= 0) return n;`
     (the **source's** `available()`, not `this.available()`: the test's `Peek` subclass overrides `available()`
     and must see no call from `read(b, off, len)`).
  Example (`bulkReads`, size 4, source step 3, 16 bytes): `read(x,0,2)` logs `4@0/4=3`; `read(x,0,5)` copies 1
  byte, calls `av=13`, reads `4@1/8=3` directly into `x`, calls `av=10`, then fills `4@0/4=3` for the last byte.
- `skip(n)`: `ensureOpen();` `if (n <= 0) return 0;` `long avail = count - pos;` if `avail <= 0`: with
  `markpos == -1` return `getInIfOpen().skip(n)` (the source's skip, unbuffered); otherwise `fill(); avail = count - pos;
  if (avail <= 0) return 0;`. Then `long skipped = (avail < n) ? avail : n; pos += (int) skipped; return skipped;`
  (it never skips more than what is buffered once the buffer has bytes: `skip(10)` with 3 buffered returns 3).
- `available()`: `int n = count - pos; int avail = getInIfOpen().available();`
  `return n > (Integer.MAX_VALUE - avail) ? Integer.MAX_VALUE : n + avail;` With 2 bytes buffered, a source
  available of `Integer.MAX_VALUE` or `Integer.MAX_VALUE - 2` gives 2147483647, `Integer.MAX_VALUE - 3` gives
  2147483646. It does not call `ensureOpen()`; after `close()` `in` is `null`, so it throws `IOException("Stream closed")`.
- `mark(readlimit)`: `marklimit = readlimit; markpos = pos;` (no checks, also after close).
- `reset()`: `ensureOpen(); if (markpos < 0) throw new IOException("Resetting to invalid mark"); pos = markpos;`
- `markSupported()`: `return true;` (also when closed or when `in` does not support marks).
- `close()`: if `buf != null`: `InputStream input = in; buf = null; in = null; if (input != null) input.close();`.
  A second `close()` does nothing (the source sees one `close`). If `input.close()` throws, the exception propagates
  but `buf` and `in` are already `null`. After `close()` `read()`, `read(b,off,len)`, `skip` (also `skip(0)`),
  `available()`, `reset()` and `transferTo` throw `IOException("Stream closed")`; `mark` and `markSupported` do not.
  `count`, `pos`, `markpos` and `marklimit` keep their values.
- `transferTo(out)`: `Objects.requireNonNull(out, "out");` (message `out`), then
  - if `getClass() == BufferedInputStream.class && markpos == -1`: `int avail = count - pos;` if `avail > 0`,
    `out.write(Arrays.copyOfRange(getBufIfOpen(), pos, count)); pos = count;` (the one-argument `write` with a fresh
    array of exactly the buffered bytes: the `Sink` logs `a2@0/2:bc`). Then
    `try { return Math.addExact(avail, getInIfOpen().transferTo(out)); } catch (ArithmeticException ignore) { return Long.MAX_VALUE; }`
    (the source's own `transferTo`; `avail` widened to `long`).
  - otherwise (a subclass, or a mark is set) `return super.transferTo(out);`, the `InputStream` default, which reads
    through `this.read(buffer, 0, 16384)`.
  Import `java.util.Arrays` and `java.util.Objects`.

BufferedOutputStream:

- Constructor `(out, size)`: `super(out);` `if (size <= 0) throw new IllegalArgumentException("Buffer size <= 0");`
  `buf = new byte[size]; maxBufSize = size;`. `(out)` uses 8192. A `null` `out` is allowed.
  With a `null` `out` nothing fails until the buffer is written: on `new BufferedOutputStream(null, 2)`,
  `write(1); write(2);` just buffer, then `write(3)` (which calls `flushBuffer()`) and `flush()` throw
  `NullPointerException` from `out.write(buf, 0, count)`. Do not add a null check.
- `flushBuffer()`: `if (count > 0) { out.write(buf, 0, count); count = 0; }` (when `out.write` throws, `count`
  is not reset, so the bytes are written again by the next flush).
- `growIfNeeded(len)`: `int neededSize = count + len + 1; if (neededSize < 0) neededSize = Integer.MAX_VALUE;`
  `int bufSize = buf.length; if (neededSize > bufSize && bufSize < maxBufSize) buf = Arrays.copyOf(buf, Math.min(neededSize, maxBufSize));`
  This only matters when a subclass replaced `buf` with a shorter array: `OSpy(sink, 8)` with `buf = new byte[2]`
  shows `buf` lengths 2, 3, 4 after `write('a')`, `write('b')`, `write('c')`, then 8 after a 4-byte write.
- `write(int b)`: `growIfNeeded(1); if (count >= buf.length) flushBuffer(); buf[count++] = (byte) b;` (a full buffer
  is flushed only when the next byte arrives: 8192 `write(int)` calls on the default stream reach the sink not at all,
  the 8193rd flushes `a8192@0/8192`).
- `write(byte[] b, int off, int len)`:
  1. `if (len >= maxBufSize) { flushBuffer(); out.write(b, off, len); return; }` (compare with `maxBufSize`, not
     `buf.length`; no range check of its own: `write(x, 2, 9000)` and `write(x, -3, 4)` on a size-4 stream reach the
     sink unchanged as `a9000@2/4` and `a4@-3/4`; over a `ByteArrayOutputStream` the sink throws
     `IndexOutOfBoundsException: Range [2, 2 + 9000) out of bounds for length 4`).
  2. `growIfNeeded(len); if (len > buf.length - count) flushBuffer();`
  3. `System.arraycopy(b, off, buf, count, len); count += len;` Bad ranges are reported by `System.arraycopy` as
     `ArrayIndexOutOfBoundsException` (a `null` array as `NullPointerException`), after a possible `flushBuffer()`:
     with 3 bytes buffered in a size-4 stream, `write(x, -1, 2)` first writes `a3@0/4:abc` to the sink, then throws.
     The test checks only the exception types here.
- `flush()`: `flushBuffer(); out.flush();`
- Writes after `close()` are still buffered and reach the sink on the next flush (there is no closed check).
  Example (`bufferedWrites`, size 4): `write('a')`..`write('d')` reach the sink not at all, `write('e')` flushes
  `a4@0/4:abcd`; `write({'h','i','j'}, 1, 2)` with 3 bytes buffered flushes `a3@0/4:efg` first;
  `write({'k','l','m','n','o'}, 1, 4)` flushes `a2@0/4:ij` and then writes `a4@1/5:lmno` straight through.
  `write(0x171)` and `write(-191)` buffer the low bytes `q` and `A`. Import `java.util.Arrays`.

## Steps

1. Read `javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeBufferedStreamsTest.java`, then
   `FilterInputStream.java`, `FilterOutputStream.java` and `InputStream.java` in
   `javaw-bytecode/src/main/resources/exception-runtime/java/io/`.
2. Create `BufferedInputStream.java` and `BufferedOutputStream.java` in that directory.
3. Register both (do not edit `BytecodeExceptionRuntime.java` by hand; the order does not matter):
   - `javaw-agent-kit/bin/kit register java/io/BufferedInputStream.java`
   - `javaw-agent-kit/bin/kit register java/io/BufferedOutputStream.java`
4. `javaw-agent-kit/bin/kit check T33` until PASS.
5. Write `docs/bytecode-buffered-streams.md` (below), the report, then `javaw-agent-kit/bin/kit finish T33`.

## Hints

- The JDK source wraps every public method in a lock and an `implXxx` method. Leave out all of that: write the body
  of `implRead`, `implSkip`, ... directly in the public method. No `InternalLock`, no `Unsafe`, no `EMPTY` array, no
  `jdk.internal.util.ArraysSupport` (use the growth rule above), no compare-and-set loop in `close()`.
- Do not write the words `synchronized`, `volatile`, `native` or `Thread` anywhere in the files, not even in comments.
- `InputStream.checkFromIndexSize` exists but is **wrong here**: it throws with a `Range [...]` message, while
  `BufferedInputStream.read(byte[],int,int)` throws `new IndexOutOfBoundsException()` with no message.
- `fill()` and `read1()` use `markpos == -1`; only `reset()` uses `markpos < 0`. `read1` reads directly into the
  caller's array only when `len >= size && markpos == -1`; with a mark set it always goes through `fill()`.
- `read(b, off, len)` asks the **source** (`in.available()`) whether to keep reading, and only after a `read1` that
  returned bytes but not yet `len` of them. Calling `this.available()` or checking before the first `read1` changes
  the logged calls.
- `transferTo` compares `getClass() == BufferedInputStream.class`; the test's `Peek` subclass must take the
  `super.transferTo(out)` path (the source then sees `16382@2/16384=3`-style reads through `read(byte[],int,int)`).
- In `BufferedOutputStream.write(byte[],int,int)` the direct path compares `len >= maxBufSize`; do not range-check
  the arguments yourself and do not call `super.write(b, off, len)` (that is the per-byte `FilterOutputStream` loop).
- `Arrays.copyOf(byte[], int)`, `Arrays.copyOfRange(byte[], int, int)`, `Math.addExact(long, long)`, `Math.min`,
  `Math.max` and `Objects.requireNonNull(Object, String)` exist in the runtime.
- The Wasm runner stops at the **first** failing fixture of each `@Test` method, so after a fix another fixture of the
  same group may fail next. That is progress.
- Calibration mutations, each caught by the fixture named: `checkFromIndexSize(off, len, b.length)` instead of the
  message-less `IndexOutOfBoundsException` (`bulkReadChecks`), `this.available()` instead of `in.available()` in
  `read(byte[],int,int)` (`bulkReads`), `transferTo` without the `getClass()` test (`transferToCalls`), setting `count`
  only after the source read returned in `fill()` (`sourceFailures`), `len >= buf.length` instead of
  `len >= maxBufSize` in `BufferedOutputStream.write(byte[],int,int)` (`outputSubclass`), and flushing right after a
  `write(int)` fills the buffer instead of before the next byte (`bufferedWrites`, `outputFailures`).
- Sizes of the calibration reference: 162 lines for BufferedInputStream, 56 for BufferedOutputStream.
- Calibration: the reference passed kit check after 1 run (about 220 new lines).

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
bufferedInputReads, bufferedInputSkipAndAvailable, bufferedInputMarkAndReset, bufferedInputClose, bufferedInputTransferTo, bufferedOutputWrites, bufferedOutputSubclassAndFailures: if FIX THE RUNTIME says `link a complete acyclic type hierarchy: java/io/Buffered...` or `link the descriptor class/interface java/io/Buffered...`, a class is missing or not registered: run `javaw-agent-kit/bin/kit register java/io/BufferedInputStream.java` and `javaw-agent-kit/bin/kit register java/io/BufferedOutputStream.java` (never edit BytecodeExceptionRuntime.java by hand). If it names a member (`link a matching instance method ...` or a field), add it exactly as listed under Members to implement (the five protected fields of BufferedInputStream and `buf`/`count` of BufferedOutputStream must be `protected`). If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
readSingleBytes, defaultSize: read() is `if (pos >= count) { fill(); if (pos >= count) return -1; } return getBufIfOpen()[pos++] & 0xff;` and fill() calls `in.read(buffer, pos, buffer.length - pos)` once; the default size is 8192 and `buf` is a `new byte[size]` from the constructor.
bulkReads: read(b,off,len) loops over read1 and, after each read1 that returned fewer than len bytes in total, calls `in.available()` (the source's, never `this.available()`: Peek's own available() must log nothing) and stops when it is <= 0; read1 reads straight into the caller's array with `in.read(b, off, len)` when nothing is buffered, `len >= Math.max(buf.length, initialSize)` and `markpos == -1`, else it calls fill() and copies at most the buffered bytes.
bulkReadChecks: after ensureOpen(), `if ((off | len | (off + len) | (b.length - (off + len))) < 0) throw new IndexOutOfBoundsException();` with NO message (not InputStream.checkFromIndexSize), then `if (len == 0) return 0;` before any source call.
skipAndAvailable: skip(n) returns 0 for n <= 0 without calling the source; with nothing buffered and `markpos == -1` it returns `in.skip(n)`; otherwise it fills (when empty) and skips at most `count - pos`. available() is `count - pos` plus `in.available()`, clamped to Integer.MAX_VALUE with `n > (Integer.MAX_VALUE - avail)`.
markAndReset, markLimits: fill() keeps the mark: with `markpos == -1` set pos = 0; else when `pos >= buffer.length`: if `markpos > 0` move `buffer[markpos..pos)` to the front (pos = pos - markpos, markpos = 0); else if `buffer.length >= marklimit` set markpos = -1 and pos = 0; else grow to `min(pos * 2, marklimit)` keeping the first pos bytes. Then `count = pos` and read into `buffer[pos..]`. reset() throws `new IOException("Resetting to invalid mark")` when `markpos < 0`.
closeBehaviour, nullSourceAndSize: close() is `if (buf != null) { InputStream input = in; buf = null; in = null; if (input != null) input.close(); }`; afterwards read/read(b,off,len)/skip/reset throw IOException("Stream closed") through ensureOpen()/getBufIfOpen(), available() and fill() through getInIfOpen() (`in == null`), while mark() and markSupported() (always true) still work. The constructor throws `new IllegalArgumentException("Buffer size <= 0")` for size <= 0, also when in is null.
sourceFailures: fill() sets `count = pos` BEFORE calling in.read, so after a failing read count == pos; close() nulls buf and in before calling in.close(), so a failing close leaves the stream closed and a second close() does nothing.
transferToCalls: transferTo starts with `Objects.requireNonNull(out, "out")`; only when `getClass() == BufferedInputStream.class && markpos == -1` it writes the buffered bytes with `out.write(Arrays.copyOfRange(buf, pos, count))` (if any), sets pos = count and returns `Math.addExact(avail, in.transferTo(out))`, or Long.MAX_VALUE on ArithmeticException; subclasses and marked streams use `super.transferTo(out)`.
bufferedWrites, defaultOutputSize: write(int) is `growIfNeeded(1); if (count >= buf.length) flushBuffer(); buf[count++] = (byte) b;`; write(b,off,len) flushes and writes straight through when `len >= maxBufSize`, else flushes first only when `len > buf.length - count`, then copies; flush() is flushBuffer() then out.flush(); flushBuffer() writes `out.write(buf, 0, count)` only when count > 0. Do not override write(byte[]) or close().
outputChecks: the constructor throws `new IllegalArgumentException("Buffer size <= 0")` for size <= 0; write(b,off,len) has no range check of its own: the direct path passes the arguments to out unchanged and the buffered path lets System.arraycopy throw (after any flushBuffer()).
outputSubclass: growIfNeeded(len) grows a buf that a subclass made shorter: `neededSize = count + len + 1` (Integer.MAX_VALUE on overflow), and if `neededSize > buf.length && buf.length < maxBufSize` then `buf = Arrays.copyOf(buf, Math.min(neededSize, maxBufSize))`; maxBufSize is the constructor size, not buf.length. write(byte[]) and close() are inherited, so the subclass sees `A2@0` for write(new byte[2]) and one `F` for close().
outputFailures: flushBuffer() resets count to 0 only after out.write returned, so a failed flush writes the same bytes again next time; close() is the inherited FilterOutputStream.close().
```

## Docs

Create `docs/bytecode-buffered-streams.md` with this skeleton (fill in the brackets):

    # Bytecode BufferedInputStream and BufferedOutputStream

    Revision `buffered-streams-vN` links `java.io.BufferedInputStream` and `java.io.BufferedOutputStream` in the
    bundled exception runtime.

    Contract:

    - <BufferedInputStream: fill with mark keeping, shifting, growth and invalidation; direct reads for large requests
      without a mark; the source available() check between chunks; skip, overflow-safe available, reset message,
      close and "Stream closed"; transferTo for the plain class versus subclasses>
    - <BufferedOutputStream: buffer size 8192 or the given size, flush-through for len >= size, flush-before-copy,
      growth of a subclass-replaced buffer, flush and the inherited close; no range check of its own>

    Known difference: both buffers are allocated eagerly with their full size (JDK 21 allocates the input buffer lazily
    for the plain class and uses a 512-byte output buffer on virtual threads); this is not observable through the API.

    Evidence: `BytecodeBufferedStreamsTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders,
    two fresh instances).
