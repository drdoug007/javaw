# Bytecode InputStream

Revision `input-stream-v282` links `java.io.InputStream`, `java.io.ByteArrayInputStream` and
`java.io.EOFException` in the bundled exception runtime.

Contract:

- `InputStream` defaults build on `read()`: `read(b)` is `read(b, 0, b.length)`; `read(b, off, len)`
  range-checks (`IndexOutOfBoundsException` with `Range [off, off + len) out of bounds for length b.length`
  from the package-private `checkFromIndexSize` helper, `NullPointerException` for a null array), returns
  `0` for `len == 0` without calling `read()`, returns `-1` only when the very first `read()` is `-1`, and
  swallows an `IOException` from a later `read()`, returning the count read so far (other exceptions
  propagate). `readAllBytes()` is `readNBytes(Integer.MAX_VALUE)`. `readNBytes(len)` makes the same
  `read(b, off, len)` calls as JDK 21: it fills fresh chunks of at most 16384 bytes (never allocating `len` up
  front), calling `read(chunk, nread, min(chunk.length - nread, remaining))` until a call returns `<= 0`, so
  `readNBytes(0)` calls `read(chunk, 0, 0)` and a full chunk or a satisfied request ends with one more
  zero-length call; it stops on `-1` or once `len` bytes are read, then joins the chunks.
  `readNBytes(b, off, len)` loops `read` while fewer than `len` bytes are read (no call for `len == 0`) and
  never returns `-1`. `skip(n)` returns `0` for
  `n <= 0` and otherwise drains through a scratch buffer of at most 2048 bytes; `skipNBytes(n)` loops
  `skip(n)`, falls back to `read()` when `skip` reports `0`, throws `EOFException` at end of stream, and
  throws `IOException("Unable to skip exactly")` for an inconsistent `skip`. `mark`/`close` do nothing,
  `available()` is `0`, `markSupported()` is `false`, `reset()` throws
  `IOException("mark/reset not supported")`. `transferTo(out)` checks `Objects.requireNonNull(out, "out")`
  and copies through a 16384-byte buffer (`read(buffer, 0, 16384)` until `-1`), saturating the count at
  `Long.MAX_VALUE`.
- `nullInputStream()` returns a fresh anonymous `InputStream` each call with a private `closed` flag whose
  `ensureOpen()` throws `IOException("Stream closed")`. Open, it yields end-of-stream values (`-1`, `0`,
  empty arrays); closed, only `read(b, off, 0)`, `readNBytes(-1)` and `close` still succeed (range check /
  `len < 0` throw before the open check), everything else throws `IOException("Stream closed")` except
  `skipNBytes(n > 0)`, which throws `EOFException`, and `transferTo(null)`, which throws an
  `Objects.requireNonNull` `NullPointerException` with no message.
- `ByteArrayInputStream` shares the caller's array (no copy), keeps the protected fields `buf`, `pos`,
  `mark`, `count` that library subclasses read and write, and sets `pos = offset`, `mark = offset`,
  `count = Math.min(offset + length, buf.length)` with no range check. `read()` returns `buf[pos++] & 0xff`;
  `read(b, off, len)` range-checks first and returns `-1` at end of stream even for `len == 0`; `skip`
  clamps to what is left; `readAllBytes()` copies `buf[pos..count)`; `readNBytes(b, off, len)` returns `0`
  (not `-1`) at end of stream; `transferTo` writes `buf[pos..count)` directly with no null check, in `write(buf, pos, n)` calls of at
  most 131072 bytes, advancing `pos` after each write (a failing write leaves the earlier chunks consumed). Marking
  ignores the limit and `reset()` restores `mark`. The runtime is single-threaded, so none of the JDK's
  `synchronized` methods or `volatile` flag is used.

Evidence: `BytecodeInputStreamTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
fresh instances), including library-style subclasses of `InputStream` and `ByteArrayInputStream`.
