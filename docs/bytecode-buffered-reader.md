# Bytecode BufferedReader

Revision `buffered-reader-v294` links `java.io.BufferedReader` in the bundled exception runtime. Like `Reader`,
it keeps the JDK `lock` field (the wrapped reader) but never enters a monitor (single-threaded contract).

Contract:

- `BufferedReader(Reader in)` delegates to `BufferedReader(in, 8192)`; `BufferedReader(Reader in, int sz)` first
  calls `super(in)` (so a null reader throws `NullPointerException` before any size check and `lock` is the
  wrapped reader), then rejects `sz <= 0` with `IllegalArgumentException("Buffer size <= 0")` and allocates
  `cb = new char[sz]`. The constructor makes no call on the wrapped reader.
- `fill()` is the only place that reads into `cb`. Without a mark it reads `in.read(cb, 0, cb.length)`; with a
  valid mark it first moves the marked chars to the front of `cb` (growing `cb` to `readAheadLimit` for good
  when `readAheadLimit > cb.length`), or invalidates the mark when `nextChar - markedChar >= readAheadLimit`.
  It asks for the whole free part of the buffer and retries while `in.read` returns `0`; a `-1` result leaves
  `nextChar`/`nChars` unchanged.
- `read()` returns chars from the buffer, filling when exhausted (and returning `-1` at the end) and swallowing
  a single `\n` right after a line that ended with `\r`. It never calls `in.read()` or `this.read(char[],int,int)`.
- `read(char[],int,int)` checks the closed state **before** the range check
  (`IndexOutOfBoundsException`, message `Range [off, off + len) out of bounds for length L`, `NullPointerException`
  for a null array), returns `0` for `len == 0` without a call, and otherwise copies via `read1`, topping up in a
  `while (n < len && in.ready())` loop (always `in.ready()`, never `this.ready()`). `read1` reads directly from
  `in` into the caller's array only when the buffer is empty and `len >= cb.length` and the mark is unset and no
  `\n` is pending; otherwise it fills and copies `min(len, nChars - nextChar)` chars. The inherited
  `Reader.read(char[])` and `Reader.transferTo(Writer)` work on top of this override (8192-char chunks).
- `readLine()` ends a line at `\n`, `\r` or `\r\n` (other Unicode line breaks are ordinary chars); the terminator
  is not part of the line. A `\r` sets `skipLF` so the following `\n` is swallowed even when it lands at the start
  of a later fill or call, and a line longer than the buffer is accumulated in a `StringBuilder` across fills.
  At the end it returns the collected text only when it is non-empty, else `null`; every further call returns
  `null` and fills again. An `IOException` from `in.read` mid-line discards the chars already copied.
- `skip(n)` rejects `n < 0` with `IllegalArgumentException("skip value is negative")` **before** the closed check
  (also for `Long.MIN_VALUE`), makes no call for `skip(0)`, moves `nextChar` through the buffer filling as
  needed, swallows a pending `\n` without counting it, and returns the count skipped (`0` at the end); it never
  calls `this.read`.
- `ready()` returns `true` without asking `in` while chars are buffered, otherwise returns `in.ready()`; with a
  pending `\n` and an empty buffer it may fill and swallows the `\n` (asking `in.ready()` twice when the source
  answers `false`).
- `mark(readAheadLimit)` rejects a negative limit with `IllegalArgumentException("Read-ahead limit < 0")` before
  the closed check; `markSupported()` is always `true`. `reset()` throws `IOException("Stream not marked")` when
  unmarked and `IOException("Mark invalid")` once invalidated, and restores the position and the pending-`\n`
  flag without clearing the mark (reset twice works; an invalid mark stays invalid until the next `mark`). The
  mark is only checked inside `fill()`.
- `close()` closes the wrapped reader (a throwing `in.close()` still closes the reader) and nulls the source and
  buffer; a second `close()` does nothing. Afterwards `read()`, `read(char[],int,int)` (even with `len == 0` or a
  bad range), `readLine()`, `skip(n >= 0)`, `ready()`, `mark(n >= 0)` and `reset()` throw
  `IOException("Stream closed")`, while `skip(-1)`/`mark(-1)` still throw `IllegalArgumentException`, a null array
  still throws `NullPointerException` and `markSupported()` still returns `true`.

Not linked: `lines()` (needs `java.io.UncheckedIOException` and is unused by the pinned libraries), so callers
are rejected before WAT generation; `read(java.nio.CharBuffer)`.

Evidence: `BytecodeBufferedReaderTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders,
two fresh instances). `BufferedReader.lines()` is rejected before WAT generation.
