# T18 InputStream and ByteArrayInputStream

```kit
id: T18
slug: input-stream
title: link java.io.InputStream, ByteArrayInputStream and EOFException
tests: BytecodeInputStreamTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/io/EOFException.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/io/InputStream.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/io/ByteArrayInputStream.java
allow: docs/bytecode-input-stream.md
```

## Goal

Add the byte-source side of `java.io` to the bundled runtime: the abstract `java.io.InputStream`, the in-memory
`java.io.ByteArrayInputStream` and `java.io.EOFException`. The pinned libraries refer to `InputStream` from 636
classes and `ByteArrayInputStream` from 100 (`runtime-target/target/audit/external-types.tsv`); the member rows are
dominated by `InputStream.close` (413 call sites), `ByteArrayInputStream.<init>([B)` (93), `read([BII)` (41),
`read([B)` (37), `EOFException.<init>()` (33), `read()` (24), `skip`, `available`, `nullInputStream`,
`mark`/`reset`/`markSupported`, `transferTo` and `readAllBytes`. Jackson's `ByteSourceJsonBootstrapper`, bcel's
`ClassParser` and `ByteSequence` (which subclasses `ByteArrayInputStream` and reads its protected `pos`), Log4j's
`SortedArrayStringMap` and many Tomcat classes subclass or call these. `java.io.OutputStream` already exists in the
runtime; this task only adds the input side.

The JDK marks the `ByteArrayInputStream` methods `synchronized` and the `nullInputStream` flag `volatile`. Here the
runtime is **single-threaded**: write them with no `synchronized` and no `volatile` (RULES.md).

## Members to implement

`java/io/EOFException.java`: `public class EOFException extends IOException` with `public EOFException()` and
`public EOFException(String s)` (copy `java/io/UnsupportedEncodingException.java`).

`java/io/InputStream.java`: `public abstract class InputStream implements Closeable`

- `public InputStream()`
- `public static InputStream nullInputStream()` (returns a **new** stream on every call, see below)
- `public abstract int read() throws IOException`
- `public int read(byte[] b) throws IOException`
- `public int read(byte[] b, int off, int len) throws IOException`
- `public byte[] readAllBytes() throws IOException`
- `public byte[] readNBytes(int len) throws IOException`
- `public int readNBytes(byte[] b, int off, int len) throws IOException`
- `public long skip(long n) throws IOException`
- `public void skipNBytes(long n) throws IOException`
- `public int available() throws IOException`
- `public void close() throws IOException`
- `public void mark(int readlimit)`
- `public void reset() throws IOException`
- `public boolean markSupported()`
- `public long transferTo(OutputStream out) throws IOException`

`java/io/ByteArrayInputStream.java`: `public class ByteArrayInputStream extends InputStream`

- fields `protected byte[] buf; protected int pos; protected int mark = 0; protected int count;` (exactly these
  names: the test subclasses `ByteArrayInputStream` and reads and writes them)
- `public ByteArrayInputStream(byte[] buf)`, `public ByteArrayInputStream(byte[] buf, int offset, int length)`
- overrides, none of which declares `throws IOException` except `close` and `transferTo`:
  `int read()`, `int read(byte[] b, int off, int len)`, `byte[] readAllBytes()`,
  `int readNBytes(byte[] b, int off, int len)`, `long transferTo(OutputStream out) throws IOException`,
  `long skip(long n)`, `int available()`, `boolean markSupported()`, `void mark(int readAheadLimit)`,
  `void reset()`, `void close() throws IOException`

Do **not** override `read(byte[])`, `readNBytes(int)` or `skipNBytes(long)` in `ByteArrayInputStream`: the
inherited `InputStream` versions give the JDK results.

## Members to leave out

`java.io.ByteArrayOutputStream`, `BufferedInputStream`, `FilterInputStream`, `DataInputStream` and the readers are
separate work: do not create them (`kit guard` allows only this task's three runtime files,
`docs/bytecode-input-stream.md` and the registration lines `kit register` adds). Do not add members to
`OutputStream`. Nothing in `InputStream` needs a class the runtime lacks, so all of its public members are in.

## JDK behaviour to match

Range check. Wherever the list below says "range check", throw
`new IndexOutOfBoundsException("Range [" + off + ", " + off + " + " + len + ") out of bounds for length " + b.length)`
when `off < 0 || len < 0 || len > b.length - off` (the JDK's `Objects.checkFromIndexSize`, which the runtime does
**not** have; write a package-private static helper (no access modifier:
`static void checkFromIndexSize(int off, int len, int length)`), so `ByteArrayInputStream` in the same package can
call it; do not make it private or public). Example: `read(new byte[4], 2, 3)` gives
`Range [2, 2 + 3) out of bounds for length 4`. A `null` array throws `NullPointerException` (just touch
`b.length`; the message is not checked).

`InputStream` defaults (they are only built on `read()` and on each other, so a subclass that only implements
`read()` gets all of them):

- `read(b)` is `read(b, 0, b.length)`.
- `read(b, off, len)`: range check; if `len == 0` return `0` **without calling `read()`**; call `read()` once: `-1`
  returns `-1` and stores nothing (`b` keeps its old contents); store it as `(byte) c` at `b[off]`; then keep calling `read()` for the remaining positions and stop
  at `-1`. If one of those later calls throws an **`IOException`**, catch it, stop, and return the count read so far
  (the first call's exception propagates; other exception types always propagate). Return the count.
- `readNBytes(len)`: `len < 0` throws `IllegalArgumentException("len < 0")`. Otherwise read until `len` bytes or
  end of stream by calling `read(byte[], int, int)` in a loop (the JDK uses buffers of at most 16384 bytes and then
  joins them; any buffer size works as long as you never allocate `len` bytes up front, because
  `readAllBytes()` is `readNBytes(Integer.MAX_VALUE)`). Returns an array exactly as long as what was read
  (`new byte[0]` at end of stream). `readNBytes(int)` and `readNBytes(byte[], int, int)` catch nothing: in the
  test's `Failing(true)` (returns 'a', 'b', then throws `IOException("boom")`), `readAllBytes()` throws
  `IOException: boom`, because the first bulk read returns 2 and the second bulk read's first `read()` throws.
- `readNBytes(b, off, len)`: range check, then loop `read(b, off + n, len - n)` until `n == len` or a call returns a
  negative value. Returns `n` (never `-1`; `0` at end of stream).
- `skip(n)`: `n <= 0` returns `0` without reading. Otherwise read into a scratch buffer of
  `(int) Math.min(2048, n)` bytes with `read(buf, 0, (int) Math.min(size, remaining))` until `remaining` is 0 or a
  read returns a negative value; return `n - remaining`.
- `skipNBytes(n)`: `while (n > 0)`: `ns = skip(n)`; if `0 < ns <= n` then `n -= ns`; else if `ns == 0` call
  `read()`: `-1` throws `new EOFException()` (null message), otherwise `n--`; else (negative, or more than `n`)
  throw `new IOException("Unable to skip exactly")`. `n <= 0` does nothing. Example: the test's `FixedSkip(5, 0)`
  (a `Counter` whose `skip` always returns 0) after `skipNBytes(3)` gives `read() == 21` and `calls == 4` (three
  `read()` calls from the `ns == 0` branch, then the fourth).
- `available()` returns `0`; `close()` and `mark(int)` do nothing; `markSupported()` is `false`; `reset()` throws
  `new IOException("mark/reset not supported")`.
- `transferTo(out)`: `Objects.requireNonNull(out, "out")` (message `out`), then loop: `read(buffer, 0,
  buffer.length)`; stop when it returns a negative value, otherwise `out.write(buffer, 0, read)` and add to the
  total. Returns the total as a `long`.

`InputStream.nullInputStream()` returns a fresh anonymous subclass with a private `boolean closed` field.
`ensureOpen()` throws `new IOException("Stream closed")` when closed. Its overrides, in this order of checks:

| member | behaviour |
|---|---|
| `available()` | ensureOpen; `0` |
| `read()` | ensureOpen; `-1` |
| `read(b, off, len)` | range check; `len == 0` returns `0` (even when closed); ensureOpen; `-1` |
| `readAllBytes()` | ensureOpen; `new byte[0]` |
| `readNBytes(b, off, len)` | range check; ensureOpen; `0` |
| `readNBytes(len)` | `len < 0` throws `IllegalArgumentException("len < 0")`; ensureOpen; `new byte[0]` |
| `skip(n)` | ensureOpen; `0L` |
| `skipNBytes(n)` | ensureOpen; if `n > 0` throw `new EOFException()` |
| `transferTo(out)` | `Objects.requireNonNull(out)` (**no** message); ensureOpen; `0L` |
| `close()` | `closed = true` (closing twice is fine) |

`read(byte[])`, `mark`, `reset` and `markSupported` are inherited (so `reset()` gives `mark/reset not supported`).

`ByteArrayInputStream`:

- `ByteArrayInputStream(buf)`: `pos = 0`, `count = buf.length`, `mark = 0`. The array is **shared**, not copied.
- `ByteArrayInputStream(buf, offset, length)`: `pos = offset`, `count = Math.min(offset + length, buf.length)`,
  `mark = offset`. No range check (`(data, 4, 100)` on 6 bytes gives `count = 6`; `(data, 6, 2)` is simply empty).
- `read()`: `pos < count ? (buf[pos++] & 0xff) : -1` (bytes are returned as 0..255).
- `read(b, off, len)`: range check **first**; then `pos >= count` returns `-1` (**even when `len == 0`**, unlike
  the `InputStream` default: on a 5-byte stream `read(b, 0, 0)` is `0` before the end and `-1` after all 5 bytes
  are read); clamp `len` to `count - pos`; `len <= 0` returns `0`; `System.arraycopy` and
  advance `pos`.
- `readAllBytes()`: `Arrays.copyOfRange(buf, pos, count)` and `pos = count`.
- `readNBytes(b, off, len)`: `n = read(b, off, len)`, returns `n == -1 ? 0 : n`.
- `transferTo(out)`: `len = count - pos`; if `len > 0`, `out.write(buf, pos, len)` and `pos = count`; return `len`.
  There is no null check: at end of stream `transferTo(null)` returns `0`; with bytes left it throws
  `NullPointerException` from the `out.write` call.
- `skip(n)`: `k = count - pos`; if `n < k` then `k = n < 0 ? 0 : n`; `pos += (int) k`; return `k`.
- `available()`: `count - pos`. `markSupported()`: `true`. `mark(limit)`: `mark = pos` (the limit is ignored).
  `reset()`: `pos = mark`. `close()`: does nothing; the stream still reads after `close()`.

## Steps

1. Read the test `javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeInputStreamTest.java`.
2. Create `javaw-bytecode/src/main/resources/exception-runtime/java/io/EOFException.java` (`package java.io;`).
3. Create `java/io/InputStream.java` (with the `nullInputStream` anonymous class inside it), then
   `java/io/ByteArrayInputStream.java`, in the same directory.
4. Register all three, in any order:
   `javaw-agent-kit/bin/kit register java/io/EOFException.java`,
   `javaw-agent-kit/bin/kit register java/io/InputStream.java`,
   `javaw-agent-kit/bin/kit register java/io/ByteArrayInputStream.java`.
   Each adds a `paths.add("java/io/....java");` line before the `ExceptionSupport` line of
   `BytecodeExceptionRuntime.java`.
5. `javaw-agent-kit/bin/kit check T18` until PASS.
6. Write `docs/bytecode-input-stream.md` (below), the report, then `kit finish T18`.

## Hints

- Copy the style of `java/io/OutputStream.java`, which is the mirror image of this task (an abstract base with a
  loop default over `write(int)`).
- Write one package-private static helper in `InputStream` (no access modifier:
  `static void checkFromIndexSize(int off, int len, int length)`), with the exact message above, and use it from
  `ByteArrayInputStream` and the null stream too. Do not make it private (`ByteArrayInputStream` is a separate class
  and javac rejects the call with `checkFromIndexSize has private access in InputStream`) or public (it would add
  public API).
- `java.util.Arrays.copyOf(byte[], int)`, `Arrays.copyOfRange(byte[], int, int)`, `Objects.requireNonNull(T)` and
  `Objects.requireNonNull(T, String)` exist in the runtime. `Objects.checkFromIndexSize` does **not**. Use
  `java.util.Objects`/`java.util.Arrays` with an import or the full name.
- `readNBytes(int)`: a simple version is a growing buffer: start with `byte[] buf = new byte[Math.min(len, 8192)]`,
  call `read(buf, total, Math.min(buf.length - total, len - total))`, grow with `Arrays.copyOf(buf, ...)` when it
  is full and `total < len`, stop at `-1` or `total == len`, and return `Arrays.copyOf(buf, total)` (or `buf` when
  the lengths match). Do not call `read(buf, total, 0)` in a loop: the default returns `0` for a zero length and
  you would loop forever. The test reads 40000 bytes, so the growing branch is exercised.
- The `nullInputStream` stream is an anonymous class (`return new InputStream() { private boolean closed; ... };`).
  Anonymous classes need no registration. It must not be a shared static instance.
- Never use `synchronized` or `volatile`: the JDK has them on most `ByteArrayInputStream` methods and on `closed`.
- `transferTo` in `InputStream` uses a buffer (any size, e.g. 8192) and `out.write(buffer, 0, n)`; the existing
  `OutputStream.write(byte[], int, int)` loops over `write(int)`, which the test's sink implements.
- Do not paste the JDK's `assert pos == count;` (assert is forbidden) or its `Math.addExact` overflow guard (not
  needed); `transferred += read` is enough.
- Before any runtime file exists, every matching test fails with
  `link a complete acyclic type hierarchy: java/io/InputStream`: that only means the classes are missing or
  unregistered.
- Calibration: the reference passed kit check after 1 run (about 180 new lines: EOFException 4, InputStream 127,
  ByteArrayInputStream 52); `kit guard` was clean and the full bytecode suite still passed.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
defaultReadMethodsOfASubclass, defaultBulkSkipMarkAndTransfer, skipNBytesAndEofException, byteArrayInputStream, byteArrayInputStreamFieldsForSubclasses, nullInputStream: if the message says `link a complete acyclic type hierarchy: java/io/InputStream` or `unsupported ... java/io/InputStream` or names `java/io/ByteArrayInputStream`/`java/io/EOFException`, a class is missing or unregistered: run `javaw-agent-kit/bin/kit register java/io/EOFException.java`, then the same for `java/io/InputStream.java` and `java/io/ByteArrayInputStream.java`, and make sure the member it names exists with exactly that signature; if it says `Cannot build bytecode exception runtime`, javac rejected a runtime file: fix the line it quotes (Objects.checkFromIndexSize does not exist; EOFException must extend IOException).
counterBounds, baisBounds: every bounds failure must be `IndexOutOfBoundsException` with the message `Range [2, 2 + 3) out of bounds for length 4` (off, off, len, b.length) and must be thrown before anything is read (calls=0); the condition is `off < 0 || len < 0 || len > b.length - off`; readNBytes(-1) throws IllegalArgumentException("len < 0").
counterRead, counterReadOffset: InputStream.read(b, off, len) returns 0 for len == 0 WITHOUT calling read() (calls=3, not 4), returns -1 only when the very first read() returns -1, and otherwise returns how many bytes it stored starting at b[off]; read(b) is read(b, 0, b.length).
defaultLoopExceptions: in InputStream.read(b, off, len) only the FIRST read() may throw out of the method; wrap the loop for the second and later bytes in `try { ... } catch (IOException e) { }` and return the count so far (2 for 'a','b'), but do not catch RuntimeException (IllegalStateException:bad must propagate).
counterBulk, counterLarge, chunkyBulk: readNBytes(len) loops over read(byte[], int, int) until len bytes or -1 and returns exactly the bytes read (grow the buffer; never allocate len up front, readAllBytes is readNBytes(Integer.MAX_VALUE)); readNBytes(b, off, len) loops read(b, off + n, len - n) until n == len or a negative result and returns n, never -1 (Chunky hands out 3 bytes per call, so a single read is not enough).
counterSkip: InputStream.skip(n) returns 0 for n <= 0 without calling read(), and otherwise reads through a scratch buffer of at most 2048 bytes until n bytes or end of stream, returning how many were skipped (5000, then 1000, then 0).
counterMarkAndClose, counterTransfer: InputStream defaults: markSupported() false, available() 0, mark() and close() do nothing, reset() throws `new IOException("mark/reset not supported")`; transferTo(null) throws via `Objects.requireNonNull(out, "out")` (message `out`), and transferTo returns the number of bytes copied (0 at end of stream).
skipNBytes, baisSkipNBytes: skipNBytes(n) loops while n > 0: ns = skip(n); 0 < ns <= n subtracts; ns == 0 calls read() and throws `new EOFException()` (getMessage() null) at -1, else n--; anything else (negative or ns > n) throws `new IOException("Unable to skip exactly")`; ByteArrayInputStream must NOT override skipNBytes.
eofException: EOFException extends IOException and has exactly two constructors, `EOFException()` (message null) and `EOFException(String s) { super(s); }`.
baisRead, baisRange: ByteArrayInputStream.read() returns `buf[pos++] & 0xff` (255, not -1, for byte -1); read(b, off, len) returns -1 when pos >= count EVEN for len == 0, else 0 for len == 0; the (buf, offset, length) constructor sets pos = offset, mark = offset, count = Math.min(offset + length, buf.length) with no range check.
baisSkip, baisBulk: ByteArrayInputStream.skip(n): `long k = count - pos; if (n < k) k = n < 0 ? 0 : n; pos += (int) k; return k;` (skip(-5) is 0, skip(100) clamps to what is left, skip(Long.MAX_VALUE) on 4 bytes is 4); readNBytes(b, off, len) returns 0 (not -1) at end of stream; readAllBytes() copies buf[pos..count) and sets pos = count.
baisTransfer: ByteArrayInputStream.transferTo(out): `int len = count - pos; if (len > 0) { out.write(buf, pos, len); pos = count; } return len;` with no null check, so transferTo(null) at end of stream returns 0.
baisMarkReset: ByteArrayInputStream.markSupported() is true, mark(limit) sets `mark = pos`, reset() sets `pos = mark` (before any mark() that is the constructor's offset, 0 here), close() does nothing and reading still works afterwards.
protectedFields, bufferIsShared: ByteArrayInputStream must have the fields `protected byte[] buf; protected int pos; protected int mark = 0; protected int count;` and every method must read them on each call (a subclass changes pos and count directly); the constructor keeps the caller's array (no copy), readAllBytes returns a copy.
nullStreamOpen, nullStreamClosed: nullInputStream() returns a NEW anonymous InputStream with `private boolean closed`; read(b, off, len) does the range check, then returns 0 for len == 0 even when closed, then ensureOpen (`new IOException("Stream closed")`), then -1; readNBytes(-1) throws IllegalArgumentException("len < 0") before ensureOpen; readNBytes(b, off, len) does the range check then ensureOpen then returns 0; skipNBytes(n) does ensureOpen and then throws `new EOFException()` only for n > 0; transferTo(null) uses `Objects.requireNonNull(out)` with NO message (getMessage() null) before ensureOpen.
```

## Docs

Create `docs/bytecode-input-stream.md` with this skeleton (fill in the brackets):

    # Bytecode InputStream

    Revision `input-stream-vN` links `java.io.InputStream`, `java.io.ByteArrayInputStream` and
    `java.io.EOFException` in the bundled exception runtime.

    Contract:

    - <InputStream defaults built on read(), bounds messages, skip/skipNBytes, mark/reset, transferTo>
    - <nullInputStream open and closed behaviour>
    - <ByteArrayInputStream: shared buffer, protected fields, clamping, mark/reset; single-threaded, no monitors>

    Evidence: `BytecodeInputStreamTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
    fresh instances), including library-style subclasses of `InputStream` and `ByteArrayInputStream`.
