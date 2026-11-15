# Bytecode BufferedInputStream and BufferedOutputStream

Revision `buffered-streams-v295` links `java.io.BufferedInputStream` and `java.io.BufferedOutputStream` in the
bundled exception runtime.

Contract:

- `BufferedInputStream` keeps the protected `buf`, `count`, `pos`, `markpos` and `marklimit` fields (not
  `volatile`), the private `initialSize`, and the `getInIfOpen`/`getBufIfOpen`/`ensureOpen` guards, all of which
  report `IOException("Stream closed")`. `fill()` keeps a mark by shifting the marked bytes to the front when
  `markpos > 0`, invalidates it when `buf.length >= marklimit`, and otherwise grows the buffer to
  `min(pos + max(1, pos), marklimit)` (capped at `Integer.MAX_VALUE - 8`, matching
  `ArraysSupport.newLength(pos, 1, pos)`, so a subclass that installs a zero-length `buf` still grows to one
  byte instead of reporting a false end of stream) with the first `pos` bytes copied; it sets
  `count = pos` **before** reading, so a throwing source leaves `count == pos`. `read()` refills on every call
  at the end of the input. `read(byte[],int,int)` checks the closed state first, then the message-less
  `IndexOutOfBoundsException` bounds test (`(off | len | (off + len) | (b.length - (off + len))) < 0`,
  `NullPointerException` for a null array) and returns `0` for `len == 0` without a source call. The `read1`
  helper reads directly into the caller's array via `in.read(b, off, len)` only when nothing is buffered,
  `len >= Math.max(buf.length, initialSize)` and `markpos == -1`; the read loop asks the **source's**
  `in.available()` (never `this.available()`) between chunks and stops when it is `<= 0`. `skip(n)` returns `0`
  for `n <= 0`, delegates to `in.skip(n)` when empty and unmarked, otherwise fills (if marked) and skips at most
  the buffered bytes. `available()` adds the buffered bytes to `in.available()` with the
  `n > (Integer.MAX_VALUE - avail)` overflow clamp. `reset()` throws `IOException("Resetting to invalid mark")`
  when `markpos < 0`. `close()` nulls `buf` and `in` before calling `in.close()` (a second `close()` is a no-op),
  leaving `count`/`pos`/`markpos`/`marklimit` intact; afterwards `read`, `read(byte[],int,int)`, `skip` (also
  `skip(0)`), `available`, `reset` and `transferTo` throw `"Stream closed"` while `mark` and
  `markSupported()` (always `true`) still work. `transferTo(OutputStream)` starts with
  `Objects.requireNonNull(out, "out")`; only the plain class with no mark writes the buffered bytes as one
  `out.write(Arrays.copyOfRange(buf, pos, count))` and returns `Math.addExact(avail, in.transferTo(out))`
  (`Long.MAX_VALUE` on overflow), while subclasses and marked streams use the `InputStream` default.
  `read(byte[])` is the inherited `FilterInputStream` version.
- `BufferedOutputStream` keeps the protected `buf` and `count` fields, the private `maxBufSize` and the
  default size 8192; a null `out` is allowed until the buffer is flushed. `flushBuffer()` writes
  `out.write(buf, 0, count)` only when `count > 0` and resets `count` only after it returns, so a failing flush
  writes the same bytes again next time. `write(int)` grows, flushes only when the buffer is already full, then
  stores the low byte. `write(byte[],int,int)` has no range check of its own: `len >= maxBufSize` flushes and
  writes straight through with the arguments passed to `out` unchanged; otherwise it grows, flushes first when
  `len > buf.length - count`, and copies (bad ranges surface as `System.arraycopy`'s
  `ArrayIndexOutOfBoundsException`, `NullPointerException` for a null array). `growIfNeeded(len)` grows a
  subclass-replaced `buf` up to `maxBufSize` to `min(count + len + 1, maxBufSize)`. `flush()` is
  `flushBuffer()` then `out.flush()`; `write(byte[])` and `close()` are the inherited `FilterOutputStream`
  versions (close flushes, then closes `out`, with the T24 suppression rules).

Known difference: both buffers are allocated eagerly with their full size (JDK 21 allocates the input buffer
lazily for the plain class and uses a 512-byte output buffer on virtual threads); this is not observable
through the API.

Evidence: `BytecodeBufferedStreamsTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders,
two fresh instances).
