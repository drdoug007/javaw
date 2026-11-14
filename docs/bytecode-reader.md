# Bytecode Reader and StringReader

Revision `reader-v280` links `java.io.Reader` and `java.io.StringReader` in the bundled exception runtime.
Like `Writer`, they keep the JDK `lock` field but never enter a monitor (single-threaded contract).

Contract:

- `Reader` defaults: `read()` asks for exactly one char (`'￿'` reads as `65535`, never `-1`) and `read(char[])`
  delegates to `read(cbuf,0,cbuf.length)` (a null array throws `NullPointerException` before any read call).
  `skip(n)` throws `IllegalArgumentException("skip value is negative")` for `n < 0` (including `Long.MIN_VALUE`),
  then reads through an `8192`-char buffer, each call asking for `min(remaining, 8192)` chars, and returns the
  count actually skipped (`skip(0)` makes no call). `ready()` and `markSupported()` return false;
  `mark(any)` and `reset()` throw `IOException` with the JDK "not supported" messages.
  `transferTo(out)` first requires a non-null writer (`NullPointerException` with message `out`), then copies in
  `8192`-char read chunks via `out.write(char[],int,int)` and returns the total. `Reader(Object lock)` throws
  `NullPointerException` (no message) for a null lock.
- `Reader.nullReader()` returns a fresh anonymous `Reader` per call. `read()`/`ready()`/`skip(n)` check the closed
  state first and then return `-1`/`false`/`0` (no negative-skip check); `read(char[],int,int)` performs the JDK
  range check **before** the closed check and returns `len == 0 ? 0 : -1`; `transferTo(out)` requires the writer
  (null message) before the closed check and returns `0`. Once closed, every checked operation throws
  `IOException("Stream closed")`, while a bad range on a closed reader is still `IndexOutOfBoundsException` and
  `transferTo(null)` is still `NullPointerException`.
- `StringReader` reads and copies from its `String` source (a null source throws `NullPointerException` in the
  constructor). `read(char[],int,int)` checks the closed state **before** the range check, returns `0` for
  `len == 0` even at the end, returns `-1` only past the end, and copies with `String.getChars`. `skip(n)` moves
  backwards but never before the start (at position 3, `skip(-100)` returns `-3`), returns `0` at the end for any
  `n` including `Long.MIN_VALUE`, and does all arithmetic in `long` before casting the bounded result, so
  `skip(4294967298L)` at position 1 returns `5`. `mark(limit)` rejects a negative limit with
  `IllegalArgumentException("Read-ahead limit < 0")` even when closed, and the limit never invalidates the mark;
  `reset()` (mark starting at `0`) goes back to the start. `close()` sets the source to null with no `throws`
  clause and is safe to call twice; afterwards `read`, `read(char[],int,int)`, `skip`, `ready`, `mark` (after the
  limit check) and `reset` throw `IOException("Stream closed")`, while `markSupported()` still returns true.

Not linked: `java.lang.Readable` and `read(java.nio.CharBuffer)` (no `CharBuffer` in the runtime), so callers of
`read(CharBuffer)` and code that needs a `Reader` as a `Readable` are rejected before WAT generation; other
concrete readers.

Evidence: `BytecodeReaderTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two fresh
instances). `Reader.read(CharBuffer)` and `StringReader.read(CharBuffer)` are rejected before WAT generation.
