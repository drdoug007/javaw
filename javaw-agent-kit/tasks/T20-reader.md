# T20 Reader and StringReader

```kit
id: T20
slug: reader
title: link java.io.Reader and java.io.StringReader
tests: BytecodeReaderTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/io/Reader.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/io/StringReader.java
allow: docs/bytecode-reader.md
```

## Goal

Add the character source base class `java.io.Reader` and its simplest concrete subclass `java.io.StringReader`
to the bundled runtime. The runtime already has the sink side (`java.io.Writer`, see
`docs/bytecode-io-sinks.md`); this task adds the source side. In the pinned libraries `java/io/Reader` is named
by 222 classes and `java/io/StringReader` by 59 (`runtime-target/target/audit/external-types.tsv`). The member
references are `Reader.close()` (79 sites), `StringReader.<init>(String)` (70, for example Jackson
`JsonFactory.createParser(String)`), `Reader.read()` (60), `Reader.mark(int)` (30, Tomcat `Host.parse`),
`Reader.reset()` (23), `Reader.read(char[],int,int)` (20, Jackson `ReaderBasedJsonParser._loadMore`),
`Reader.<init>()` (8, Jackson `UTF32Reader`), `Reader.skip(long)` (5, H2 `RangeReader`, Hibernate
`DataHelper.extractString`), `Reader.read(char[])` (4) and `Reader.ready()` (1).

The JDK `Reader` locks on `lock` with `synchronized`. Here everything is **single-threaded**: keep the `lock`
field but never enter a monitor (same contract as the runtime `Writer`).

## Members to implement

Two files under `javaw-bytecode/src/main/resources/exception-runtime/java/io/`, both `package java.io;`.

`Reader.java`: `public abstract class Reader implements Closeable` (**not** `Readable`, see below), with

- `protected Object lock;`
- `protected Reader()` (sets `lock = this`), `protected Reader(Object lock)`
- `public static Reader nullReader()`
- `public int read() throws IOException`
- `public int read(char[] cbuf) throws IOException`
- `public abstract int read(char[] cbuf, int off, int len) throws IOException`
- `public long skip(long n) throws IOException`
- `public boolean ready() throws IOException`
- `public boolean markSupported()`
- `public void mark(int readAheadLimit) throws IOException`
- `public void reset() throws IOException`
- `public abstract void close() throws IOException`
- `public long transferTo(Writer out) throws IOException`

`StringReader.java`: `public class StringReader extends Reader`, private fields `int length; String str;
int next; int mark;`, with

- `public StringReader(String s)`
- `public int read() throws IOException`
- `public int read(char[] cbuf, int off, int len) throws IOException`
- `public long skip(long n) throws IOException`
- `public boolean ready() throws IOException`
- `public boolean markSupported()`
- `public void mark(int readAheadLimit) throws IOException`
- `public void reset() throws IOException`
- `public void close()` (no `throws` clause)

`read(char[])` and `transferTo(Writer)` are inherited by `StringReader`; do not override them.

## Members to leave out

- `java.lang.Readable` and `int read(java.nio.CharBuffer target)` on both classes. The runtime has no
  `java.nio.CharBuffer` (and no `Readable`), and every type a runtime class mentions must exist. The test checks
  that `Reader.read(CharBuffer)` and `StringReader.read(CharBuffer)` stay unlinked. Observable consequence: a
  library that calls `read(CharBuffer)` (ANTLR `CharStreams.fromReader`) or treats a `Reader` as a `Readable`
  (for example passes it to `java.util.Scanner`) is rejected before WAT generation, not run with wrong results.
- The package-private `Reader(Reader in)` constructor (it exists only for JDK-internal lock selection) and the
  Java 24 `Reader.of(CharSequence)`.
- Concrete readers other than `StringReader` (`BufferedReader`, `InputStreamReader`, `CharArrayReader`, ...).

## JDK behaviour to match

Range check (written once in `Reader` as a static helper, used by `StringReader.read(char[],int,int)` and
`nullReader().read(char[],int,int)`): `Objects.checkFromIndexSize(off, len, cbuf.length)` is **not** in the runtime.
Its JDK behaviour is: throw `IndexOutOfBoundsException("Range [" + off + ", " + off + " + " + len + ") out of
bounds for length " + cbuf.length)` when `(cbuf.length | off | len) < 0 || len > cbuf.length - off`. Example:
`Range [2, 2 + 3) out of bounds for length 4`, `Range [0, 0 + -1) out of bounds for length 4`. A null `cbuf`
throws `NullPointerException` (from reading `cbuf.length`).

`Reader` defaults:

- `Reader(Object lock)`: `lock == null` throws `new NullPointerException()` (null message).
- `read()`: `char[] cb = new char[1]; if (read(cb, 0, 1) == -1) return -1; return cb[0];` so the char `￿`
  reads as `65535`, not `-1`. It always asks for exactly one char.
- `read(char[] cbuf)` = `read(cbuf, 0, cbuf.length)` (a null array throws `NullPointerException` before any read call).
- `skip(n)`: `n < 0` throws `IllegalArgumentException("skip value is negative")`. Otherwise read into a skip
  buffer of `nn = (int) Math.min(n, 8192)` chars: loop `while (r > 0)` calling
  `read(buf, 0, (int) Math.min(r, nn))`, stop at `-1`, subtract what was read, and return `n - r`. So `skip(0)`
  calls nothing and returns 0, and the len passed to each call is `min(remaining, nn)` (the test records it:
  `skip(8)` on a reader that returns 3 chars per call asks for 8, 5, 2 and returns 8; the expected `baseSkipChunks`
  line shows `8 5 2 1 ` because the fixture's following `read()` adds a len-1 call. `skip(100)` on 10 chars asks for
  100, 97, 94, 91, 90 and returns 10; `skip(20000)` on 9000 chars asks for 8192, 8192, 8192 and returns 9000).
- `ready()` returns false. `markSupported()` returns false.
- `mark(int)` throws `IOException("mark() not supported")` for any argument (even negative);
  `reset()` throws `IOException("reset() not supported")`.
- `transferTo(out)`: `Objects.requireNonNull(out, "out")` first (message `out`), then a `char[8192]` buffer:
  `while ((n = read(buffer, 0, 8192)) >= 0) { out.write(buffer, 0, n); transferred += n; }` and return the total.
  Every read call asks for 8192 chars; `out.write(char[],int,int)` gets each chunk as read.

`nullReader()` returns a new anonymous `Reader` (a new instance per call) with a `boolean closed` field and a
private `ensureOpen()` that throws `IOException("Stream closed")` once closed:

- `read()`: `ensureOpen(); return -1;`
- `read(cbuf, off, len)`: range check **first**, then `ensureOpen()`, then `len == 0 ? 0 : -1`.
  So a bad range on a closed null reader is `IndexOutOfBoundsException`, a good one is `IOException`.
- `ready()`: `ensureOpen(); return false;`
- `skip(n)`: `ensureOpen(); return 0;` (no negative check: `skip(-5)` returns 0).
- `transferTo(out)`: `Objects.requireNonNull(out)` (null message) **then** `ensureOpen()`, return 0.
- `close()`: `closed = true` (closing twice is fine; declare it without `throws`).
- `mark`/`reset`/`markSupported` are the inherited defaults.

`StringReader`:

- Constructor: `length = s.length(); str = s;` so `new StringReader(null)` throws `NullPointerException`.
- Closed means `str == null`; `ensureOpen()` throws `IOException("Stream closed")`. `close()` sets `str = null`;
  closing twice is fine.
- `read()`: `ensureOpen()`; `next >= length` gives -1, else `str.charAt(next++)`.
- `read(cbuf, off, len)`: `ensureOpen()` **first**, then the range check, then `len == 0` returns 0 (even at the
  end), then `next >= length` returns -1, else copy `n = min(length - next, len)` chars with
  `str.getChars(next, next + n, cbuf, off)`, advance and return `n`.
- `skip(n)`: `ensureOpen()`; if `next >= length` return 0 (even for a negative `n`); otherwise
  `long r = Math.min(length - next, n); r = Math.max(-next, r); next += (int) r; return r;`.
  Negative values skip backwards but never before 0: at position 3, `skip(-100)` returns -3. `skip(Long.MAX_VALUE)`
  returns the remaining count and, when not at the end, `skip(Long.MIN_VALUE)` returns `-next`. At the end
  (`next >= length`) every skip, including `Long.MIN_VALUE`, returns 0: on `"abcdef"`, `skip(Long.MAX_VALUE)` gives 6
  and then `skip(Long.MIN_VALUE)` gives 0. No `IllegalArgumentException`.
  Do the min/max in `long` and cast only the bounded result `r` to `int`; never cast `n` to `int` first. On
  `"abcdef"` at position 1, `skip(4294967298L)` (2^32 + 2) returns 5, not 2.
- `ready()`: `ensureOpen(); return true;` (true even at the end).
- `markSupported()` returns true (even after close).
- `mark(limit)`: `limit < 0` throws `IllegalArgumentException("Read-ahead limit < 0")` **before** the closed check;
  then `ensureOpen(); mark = next;`. The limit is otherwise ignored (reset works after reading past it).
- `reset()`: `ensureOpen(); next = mark;` (`mark` starts at 0, so reset without mark goes to the start).
- Inherited `transferTo` on a closed `StringReader` throws `IOException("Stream closed")` from the first read;
  with a null writer it throws `NullPointerException("out")` first.

## Steps

1. Read `javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeReaderTest.java`. Note the fixture's
   `Chunky` (a `Reader` subclass that records the `len` of every `read(char[],int,int)` call) and `Sink` (a
   `Writer` subclass that records the length of every `write(char[],int,int)` call).
2. Look at `javaw-bytecode/src/main/resources/exception-runtime/java/io/Writer.java` (the `lock` constructors)
   and `java/io/OutputStream.java` (the range check message).
3. Create `java/io/Reader.java` with the members above.
4. Create `java/io/StringReader.java`.
5. Register both (they are in `java/io`, so the command adds `paths.add(...)` lines):
   `javaw-agent-kit/bin/kit register java/io/Reader.java` and
   `javaw-agent-kit/bin/kit register java/io/StringReader.java`.
6. `javaw-agent-kit/bin/kit check T20` until PASS.
7. Write `docs/bytecode-reader.md` (below), the report, then `kit finish T20`.

## Hints

- Copy the `lock` field and both constructors from the runtime `Writer.java`.
- Never write `synchronized` (not even in a comment; guard only strips `//` comments and strings) and do not make
  the `closed` field `volatile` as the JDK does.
- Do not mention `Readable`, `CharBuffer` or `java.nio` anywhere: there are no such runtime classes, so the whole
  runtime build would fail admission. `Closeable` (and through it `AutoCloseable`) exists.
- `Objects.checkFromIndexSize` does not exist in the runtime. Write
  `static void checkFromIndexSize(int off, int len, int length)` in `Reader` (package-private, so
  `StringReader` can call it) with the exact message above; it is the same check as `OutputStream.write(byte[],int,int)`.
  Write this copy in `Reader` even if another `java/io` runtime class (for example `InputStream`) already has a
  helper with the same name; never call another class's copy, since that class may not exist when T20 runs alone.
- `Objects.requireNonNull(Object)` and `requireNonNull(Object, String)`, `Math.min/max(long,long)` and
  `String.getChars(int,int,char[],int)` exist in the runtime. `Writer.write(char[],int,int)` is abstract there,
  so `transferTo` just calls it.
- `StringReader` must override `skip(long)` itself: the inherited `Reader.skip` throws `IllegalArgumentException`
  for negative n (even when closed) and cannot move backwards, while `StringReader.skip` moves back and checks
  closed first. Also override `read()` as the JDK does (`ensureOpen(); if (next >= length) return -1; return
  str.charAt(next++);`).
- Mind the order of checks; the test pins each one: StringReader checks closed before the range, nullReader
  checks the range before closed, `mark(-1)` throws `IllegalArgumentException` even when closed, nullReader
  `transferTo(null)` throws NPE even when closed.
- The test prints HotSpot's helpful NPE messages (`Cannot read the array length ...`) as `null`, so an implicit NPE
  from `cbuf.length` is right; do not throw your own NPE with a message for null arrays.
- `BytecodeExitGateTest` already uses `java/net/Socket` (not `java/io/Reader`) as its unlinked-supertype example,
  so linking `Reader` does not break the full suite in `kit finish`. Do not edit that test.
- Calibration: the reference passed kit check after 1 run (about 130 new lines: Reader about 80, StringReader
  about 50). Two mutants (an uncapped skip buffer, and a negative-skip check in StringReader) were caught by
  `baseSkipLarge` and `stringSkip`.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
stringReaderReads, readerBaseDefaults, nullReader: if FIX THE RUNTIME says to link java/io/Reader or java/io/StringReader (`link the descriptor class/interface ...`, `link the symbolic class/interface ...` or `link a complete acyclic type hierarchy`), a class is not registered: run `javaw-agent-kit/bin/kit register java/io/Reader.java` and `javaw-agent-kit/bin/kit register java/io/StringReader.java` (never edit BytecodeExceptionRuntime.java by hand). If it says `link a matching instance method` or `link a matching special declaration`, the member the fixture calls is missing, not public, or has another signature; if it says `Cannot build bytecode exception runtime`, javac rejected Reader.java or StringReader.java: remove any mention of Readable, CharBuffer or java.nio and fix the quoted line.
stringReadSingle, baseRead: Reader.read() must be `char[] cb = new char[1]; if (read(cb, 0, 1) == -1) return -1; return cb[0];` and StringReader.read() returns `str.charAt(next++)` as an int; '￿' must come back as 65535, never -1 (do not cast to byte/short or compare the char with -1).
stringReadArrays, stringReadEmpty, stringReadBounds: StringReader.read(cbuf, off, len) does ensureOpen(), then `checkFromIndexSize(off, len, cbuf.length)` throwing `new IndexOutOfBoundsException("Range [" + off + ", " + off + " + " + len + ") out of bounds for length " + cbuf.length)` when `(cbuf.length | off | len) < 0 || len > cbuf.length - off`, then `if (len == 0) return 0;` (0 even at the end, and read(b,4,0) on a 4-char array is legal), then `if (next >= length) return -1;`, then copies `min(length - next, len)` chars with str.getChars.
stringNullSource: the constructor must call `s.length()` (or Objects.requireNonNull(s)) so `new StringReader(null)` throws NullPointerException immediately; do not turn null into "".
stringSkip, stringSkipExtremes, stringSkipLongValues: StringReader.skip(n) is `ensureOpen(); if (next >= length) return 0; long r = Math.min(length - next, n); r = Math.max(-next, r); next += (int) r; return r;` (negative n moves back but not before 0, and at the end it returns 0 even for negative n, so skip(MAX) then skip(MIN) on "abcdef" gives `6,0`; cast only the bounded r, so skip(4294967298L) at position 1 returns 5); do not throw IllegalArgumentException here and do not use the inherited Reader.skip.
stringMarkReset, stringMarkNegative: markSupported() is true; mark(limit) throws `new IllegalArgumentException("Read-ahead limit < 0")` when limit < 0 BEFORE ensureOpen(), then `mark = next`; reset() is `ensureOpen(); next = mark;` with mark starting at 0; the limit never invalidates the mark (expected `truetrue,acdcdd`).
stringClosed, stringClosedChecksArgumentsLate, tryWithResources: close() sets str = null (no throws clause, safe to call twice); read(), read(char[],int,int) (before the range check), skip, ready, mark and reset all call ensureOpen() which throws `new IOException("Stream closed")`; markSupported() still returns true; mark(-1) on a closed reader is still IllegalArgumentException; transferTo(null) gives NullPointerException with message `out` because Reader.transferTo calls `Objects.requireNonNull(out, "out")` first.
baseReadNullArray, baseDefaults, lockField: Reader.read(char[]) is `return read(cbuf, 0, cbuf.length);` (null gives an NPE before any call); ready() and markSupported() return false; mark(any int) throws `new IOException("mark() not supported")` and reset() `new IOException("reset() not supported")`; Reader() sets lock = this and Reader(Object lock) throws `new NullPointerException()` (no message) for null.
baseSkipChunks, baseSkipLarge, baseSkipZeroAndNegative: Reader.skip(n) throws `new IllegalArgumentException("skip value is negative")` when n < 0 (also Long.MIN_VALUE); then `int nn = (int) Math.min(n, 8192); char[] buf = new char[nn]; long r = n; while (r > 0) { int nc = read(buf, 0, (int) Math.min(r, nn)); if (nc == -1) break; r -= nc; } return n - r;` so skip(8) makes the calls `8 5 2` (the expected baseSkipChunks line shows `8 5 2 1 ` because the fixture's following read() adds a len-1 call; skip(100) on 10 chars gives `100 97 94 91 90` and returns 10), and skip(20000) on 9000 chars gives `8192 8192 8192` and returns 9000; skip(0) makes no call.
transferString, transferLong, transferChunks, transferPartlyRead, transferNull: Reader.transferTo(out) is `Objects.requireNonNull(out, "out"); long total = 0; char[] buffer = new char[8192]; int n; while ((n = read(buffer, 0, 8192)) >= 0) { out.write(buffer, 0, n); total += n; } return total;` so every read call asks for 8192 chars and a 10000-char StringReader writes `8192 1808`; StringReader must not override transferTo.
nullReaderOpen, nullReaderFailures, nullReaderClosed: nullReader() returns `new Reader() { boolean closed; ... }` (a new object per call): read() and ready() and skip(n) do ensureOpen() then return -1 / false / 0 (skip(-5) returns 0, no negative check); read(cbuf, off, len) does checkFromIndexSize FIRST, then ensureOpen(), then returns `len == 0 ? 0 : -1`; transferTo(out) does `Objects.requireNonNull(out)` (null message) then ensureOpen() and returns 0; close() sets closed = true; ensureOpen throws `new IOException("Stream closed")`.
leavesCharBufferMembersUnlinked: this fails when Reader.java or StringReader.java declares read(CharBuffer): delete that method and any `implements Readable`; nothing else needs to change.
```

## Docs

Create `docs/bytecode-reader.md` with this skeleton (fill in the brackets):

    # Bytecode Reader and StringReader

    Revision `reader-vN` links `java.io.Reader` and `java.io.StringReader` in the bundled exception runtime.
    Like `Writer`, they keep the JDK `lock` field but never enter a monitor (single-threaded contract).

    Contract:

    - <Reader defaults: read(), read(char[]), skip chunking and negative skip, ready/mark/reset, transferTo>
    - <nullReader check order>
    - <StringReader reading, range failures, skip back to the start, mark/reset, closed-stream failures>

    Not linked: `java.lang.Readable` and `read(java.nio.CharBuffer)` (no `CharBuffer` in the runtime), so callers of
    `read(CharBuffer)` and code that needs a `Reader` as a `Readable` are rejected before WAT generation; other
    concrete readers.

    Evidence: `BytecodeReaderTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two fresh
    instances). `Reader.read(CharBuffer)` and `StringReader.read(CharBuffer)` are rejected before WAT generation.
