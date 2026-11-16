# T32 BufferedReader

```kit
id: T32
slug: buffered-reader
title: link java.io.BufferedReader over a Reader
tests: BytecodeBufferedReaderTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/io/BufferedReader.java
allow: docs/bytecode-buffered-reader.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-buffered-reader-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeBufferedReaderTest.java`.

## Goal

Add `java.io.BufferedReader` to the bundled runtime, on top of the existing `java.io.Reader` and `StringReader`
(task T20, done). The pinned libraries name `java/io/BufferedReader` as a type in 42 classes
(`runtime-target/target/audit/external-types.tsv`, 17 of them in the static closure) and call it from 13 artifacts
(`member-references.tsv`): `<init>(Reader)` 42 times, `readLine()` 42, `close()` 42, `<init>(Reader,int)` 5,
`read()` 3, `mark(int)` 3, `reset()` 3 and `read(char[],int,int)` once. Users include H2 (`RunScriptCommand.update`
with `mark`/`read`/`reset`, `Csv.readBuffer` with `read(char[],int,int)`, `ScriptBase.openInput` with a buffer
size), Spring (`XmlValidationModeDetector`, `MediaTypeFactory.parseMimeTypes`, Boot `ImportCandidates`), Hibernate
`SingleLineSqlScriptExtractor`, Tomcat (`CGIServlet`, `RewriteValve.parse`, and `CoyoteReader`, which **extends**
`BufferedReader` through `<init>(Reader,int)`), jaxb `ContextFactory.loadIndexedClasses`, logback, AspectJ
`FileUtil`, ANTLR, Byte Buddy and angus-activation `MailcapFile`. Nothing in the pinned set calls `lines()`.

Copy the JDK 21 structure **exactly**: the underlying `Reader` sees every `read(char[],int,int)` and `ready()`
call, and the test records them (array length, offset, length, result). The JDK methods take a lock; here they are
plain methods.

## Members to implement

One new file `javaw-bytecode/src/main/resources/exception-runtime/java/io/BufferedReader.java` (`package java.io;`,
no imports needed): `public class BufferedReader extends Reader` with

- fields: `private Reader in;`, `private char[] cb;`, `private int nChars, nextChar;`,
  `private static final int INVALIDATED = -2;`, `private static final int UNMARKED = -1;`,
  `private int markedChar = UNMARKED;`, `private int readAheadLimit = 0;`, `private boolean skipLF;`,
  `private boolean markedSkipLF;`
- `public BufferedReader(Reader in, int sz)`
- `public BufferedReader(Reader in)` (`this(in, 8192);`)
- `private void ensureOpen() throws IOException`
- `private void fill() throws IOException`
- `public int read() throws IOException`
- `private int read1(char[] cbuf, int off, int len) throws IOException`
- `public int read(char[] cbuf, int off, int len) throws IOException`
- `public String readLine() throws IOException`
- `public long skip(long n) throws IOException`
- `public boolean ready() throws IOException`
- `public boolean markSupported()`
- `public void mark(int readAheadLimit) throws IOException`
- `public void reset() throws IOException`
- `public void close() throws IOException`

Nothing else. In particular do **not** override `read(char[])` or `transferTo(Writer)`: the inherited `Reader`
versions call the overridable `read(char[],int,int)` of `this`, and the test checks that a subclass sees those calls.

## Members to leave out

- `public Stream<String> lines()`: it wraps `IOException` in `java.io.UncheckedIOException`, which is not in the
  runtime, and no pinned library calls it. The test checks with `rejectsUnusedCall` that `BufferedReader.lines()`
  stays unlinked. Do not add `UncheckedIOException` either.
- The JDK's package-private `String readLine(boolean ignoreLF, boolean[] term)`: put its body into `readLine()`
  (with `ignoreLF == false` and `term == null`).
- `read(CharBuffer)` and `Readable` (no `java.nio` in the runtime; never mention them in `BufferedReader.java`; the
  docs line below may name them).
- Do not edit `Reader.java`, `StringReader.java` or any other runtime file (`kit guard` rejects it).

## JDK behaviour to match

State: `cb` is the buffer, `nextChar` the next char to hand out, `nChars` one past the last valid char,
`markedChar` the marked position (or `UNMARKED` = -1, or `INVALIDATED` = -2), `skipLF` is true right after a line
that ended with `\r` (so that a following `\n` is swallowed).

Constructor `BufferedReader(Reader in, int sz)`: `super(in);` first (the runtime `Reader(Object lock)` throws
`NullPointerException` for a null `in`, so `new BufferedReader(null)` and `new BufferedReader(null, 0)` are both
`NullPointerException`, the size is never checked), then `if (sz <= 0) throw new IllegalArgumentException("Buffer size <= 0");`
(also for -5 and `Integer.MIN_VALUE`), then `this.in = in; cb = new char[sz];`. The constructor makes no call on
`in`. The protected `lock` field (from `Reader`) is the wrapped reader, not `this`.

`ensureOpen()`: `if (in == null) throw new IOException("Stream closed");`.

`fill()` (the only place that reads into `cb`):

    int dst;
    if (markedChar <= UNMARKED) {
        dst = 0;                                    // no mark
    } else {
        int delta = nextChar - markedChar;
        if (delta >= readAheadLimit) {              // past the read-ahead limit: invalidate the mark
            markedChar = INVALIDATED; readAheadLimit = 0; dst = 0;
        } else {
            if (readAheadLimit <= cb.length) {      // shuffle the marked chars to the front of cb
                System.arraycopy(cb, markedChar, cb, 0, delta);
            } else {                                // grow cb to readAheadLimit (it stays that big)
                char[] ncb = new char[readAheadLimit];
                System.arraycopy(cb, markedChar, ncb, 0, delta);
                cb = ncb;
            }
            markedChar = 0; dst = delta; nextChar = nChars = delta;
        }
    }
    int n;
    do { n = in.read(cb, dst, cb.length - dst); } while (n == 0);   // a reader returning 0 is asked again
    if (n > 0) { nChars = dst + n; nextChar = dst; }                 // -1 leaves nChars and nextChar alone

So each fill asks for the whole free part of `cb`: the default buffer gives `in.read(cb, 0, 8192)`. A source that
returns 0 three times and then 2 sees `r5:0,5=0 r5:0,5=0 r5:0,5=0 r5:0,5=2` (buffer 5). Every `read()` at the end
of input calls `fill()` again (another `in.read` that returns -1).

`read()`: `ensureOpen();` then loop: if `nextChar >= nChars` then `fill()` and return -1 if still
`nextChar >= nChars`; if `skipLF` then set it false and, if `cb[nextChar] == '\n'`, `nextChar++` and loop again;
otherwise `return cb[nextChar++];` (a char, so `'\uffff'` is 65535). `read()` never calls `in.read()` (the
no-argument one) and never calls `this.read(char[],int,int)`.

`read1(cbuf, off, len)`:

    if (nextChar >= nChars) {
        if (len >= cb.length && markedChar <= UNMARKED && !skipLF) return in.read(cbuf, off, len);  // direct, no copy
        fill();
    }
    if (nextChar >= nChars) return -1;
    if (skipLF) {
        skipLF = false;
        if (cb[nextChar] == '\n') {
            nextChar++;
            if (nextChar >= nChars) fill();
            if (nextChar >= nChars) return -1;
        }
    }
    int n = Math.min(len, nChars - nextChar);
    System.arraycopy(cb, nextChar, cbuf, off, n);
    nextChar += n;
    return n;

`read(cbuf, off, len)`: `ensureOpen();` **first** (a closed reader gives `Stream closed` even for `(-1, 1)` or
`len == 0`), then the range check with `checkFromIndexSize(off, len, cbuf.length)` (the package-private static
helper that already exists in the runtime `Reader`; a null `cbuf` throws `NullPointerException` at `cbuf.length`),
message `Range [-1, -1 + 1) out of bounds for length 4`, `Range [0, 0 + 5) out of bounds for length 4`,
`Range [0, 0 + -1) out of bounds for length 4`, `Range [5, 5 + 0) out of bounds for length 4`,
`Range [1, 1 + 2147483647) out of bounds for length 4` (`(4, 0)` is fine). Then `if (len == 0) return 0;` (no call
on `in`), then

    int n = read1(cbuf, off, len);
    if (n <= 0) return n;
    while (n < len && in.ready()) {
        int n1 = read1(cbuf, off + n, len - n);
        if (n1 <= 0) break;
        n += n1;
    }
    return n;

Examples (`rL:off,len=n` is one `in.read` on an array of length L; the source in the test returns at most `chunk`
chars per call):

- buffer 4, source `abcdefghij` with chunk 3, `read(buf10, 0, 10)` returns 10 with
  `r10:0,10=3 ready=true r10:3,7=3 ready=true r10:6,4=3 ready=true r4:0,4=1` (the first three are direct reads into
  the caller's array; the last request is smaller than the buffer, so it fills `cb`).
- The same with a source whose `ready()` is false: returns 3 after `r10:0,10=3 ready=false`.
- buffer 8 over `abcdefghij`: `read(buf,0,3)` gives 3 (`r8:0,8=8`), `read(buf,0,10)` gives 7 (5 from the buffer,
  then `ready=true r8:0,8=2 ready=false`), the next `read(buf,0,10)` is a direct `r10:0,10=-1` and returns -1,
  `read(buf,0,0)` returns 0 with no call, `read(buf,2,1)` fills again (`r8:0,8=-1`) and returns -1.
- default buffer: `read(buf,1,4)` on 20 chars makes one `r8192:0,8192=20`; `read(big9000,0,9000)` then copies the
  16 buffered chars and asks `ready=false`; `read(big9000)` (inherited `Reader.read(char[])`) is a direct
  `r9000:0,9000=-1`.

`readLine()` (the JDK `readLine(false, null)`):

    StringBuilder s = null;
    ensureOpen();
    boolean omitLF = skipLF;
    for (;;) {
        if (nextChar >= nChars) fill();
        if (nextChar >= nChars) {                         // end of input
            if (s != null && s.length() > 0) return s.toString();
            return null;
        }
        boolean eol = false; char c = 0; int i;
        if (omitLF && cb[nextChar] == '\n') nextChar++;   // the \n of a \r\n split across calls or fills
        skipLF = false; omitLF = false;
        for (i = nextChar; i < nChars; i++) {
            c = cb[i];
            if (c == '\n' || c == '\r') { eol = true; break; }
        }
        int startChar = nextChar;
        nextChar = i;
        if (eol) {
            String str;
            if (s == null) str = new String(cb, startChar, i - startChar);
            else { s.append(cb, startChar, i - startChar); str = s.toString(); }
            nextChar++;
            if (c == '\r') skipLF = true;
            return str;
        }
        if (s == null) s = new StringBuilder(80);
        s.append(cb, startChar, i - startChar);
    }

Only `\n`, `\r` and `\r\n` end a line (`\u2028`, `\u0085`, `\u000b` and `\f` are ordinary chars). The terminator is
not part of the line. Lines for every buffer size (8192, 1, 2, 3) are the same: `a\nb\r\nc\rd\n\ne` gives `a b c d "" e`
then null; `x\r` gives `x`; `""` gives null at once; `\n` and `\r\n` give one empty line; `\r\r\n\n` gives three
empty lines; `\r\n\r\n\rq` gives `"" "" "" q`. After the end every `readLine()` returns null and calls `fill()`
again. With buffer 1 over `\r\n` the second `readLine()` skips the `\n`, finds the end with an empty `s`, and
returns null (`s.length() > 0` is false). If `in.read` throws in the middle of a line, the chars already copied into
`s` are lost (buffer 2 over `abcdef\nxy`, the second `in.read` throws: `IOException:disk`, then `cdef`, then `xy`).
If a later `read1` inside the `in.ready()` loop throws, `read(char[],int,int)` throws and the chars already copied
are dropped (buffer 4 over `abcdefgh`, the second `in.read` throws: `read(buf8,0,3)` gives 3, `read(buf8,0,8)` gives
`IOException:disk`, the next `read(buf8,0,8)` gives 4 (`efgh`); log
`r4:0,4=4 ready=true r8:1,7=! r8:0,8=4 ready=false`).

`skipLF` across calls (after `readLine()` returned a line that ended with `\r`):

- `read()` swallows one `\n` (`a\r\nb`: `readLine` `a`, `read` 98); a second `\r` is returned (`a\r\rb`: 13).
- `read(char[],int,int)` swallows it in `read1`, and the direct path is not taken while `skipLF` is set: buffer 3
  over `ab\r\n`, after `readLine` gives `ab`, `read(buf,0,5)` makes `r3:0,3=1` (the `\n`) and `r3:0,3=-1` and
  returns -1.
- `skip(n)` swallows it without counting it (`a\r\nbcd`: `readLine`, `skip(2)` returns 2, `read` gives 100 `d`).
- `ready()` swallows it (see below).
- `read()` itself never sets `skipLF`: `read()` of `\r` in `\r\nx` returns 13 and the next `readLine()` returns "".

`skip(n)`: `if (n < 0L) throw new IllegalArgumentException("skip value is negative");` **before** `ensureOpen()`
(also on a closed reader; `Long.MIN_VALUE` too), then `ensureOpen();` and

    long r = n;
    while (r > 0) {
        if (nextChar >= nChars) fill();
        if (nextChar >= nChars) break;
        if (skipLF) { skipLF = false; if (cb[nextChar] == '\n') nextChar++; }
        long d = nChars - nextChar;
        if (r <= d) { nextChar += (int) r; r = 0; break; }
        r -= d; nextChar = nChars;
    }
    return n - r;

`skip(0)` makes no call. Buffer 4 over `abcdefghij`: `skip(3)` 3, `read` `d`, `skip(5)` 5, `read` `j`, `skip(10)` 0,
`skip(1)` 0. `skip(Long.MAX_VALUE)` over `abcdef` returns 6. It does not call `this.read`.

`ready()`: `ensureOpen();` then

    if (skipLF) {
        if (nextChar >= nChars && in.ready()) fill();
        if (nextChar < nChars) { if (cb[nextChar] == '\n') nextChar++; skipLF = false; }
    }
    return nextChar < nChars || in.ready();

So with chars in the buffer it returns true without asking `in`; with an empty buffer it returns `in.ready()`. With
`skipLF` set and an empty buffer, a source whose `ready()` is false is asked twice per call
(`ready=false ready=false`).

`markSupported()`: `true` (also when closed).

`mark(readAheadLimit)`: `if (readAheadLimit < 0) throw new IllegalArgumentException("Read-ahead limit < 0");`
**before** `ensureOpen()`, then `ensureOpen(); this.readAheadLimit = readAheadLimit; markedChar = nextChar; markedSkipLF = skipLF;`.

`reset()`: `ensureOpen();` then `if (markedChar < 0) throw new IOException(markedChar == INVALIDATED ? "Mark invalid" : "Stream not marked");`
then `nextChar = markedChar; skipLF = markedSkipLF;`. Reset does not clear the mark (reset twice works), and an
invalid mark stays invalid until the next `mark`.

The mark is only checked in `fill()`: as long as no fill happens, any number of chars can be read and reset
(`mark(0)` then one `read` from the buffer, `reset` works). Examples with buffer 4 over `abcdefghij`:
`read` `a`, `mark(4)`, read `bcde` (the read of `e` fills with `r4:3,1=1` after shuffling `bcd` to the front),
`reset`, read `bcdef` (the read of `f` fills with `r4:0,4=4` and invalidates), `reset` throws `Mark invalid`.
Buffer 4, `mark(10)` at the start: the first fill grows `cb` to 10 (`r10:0,10=10`), and after the mark is invalidated
`cb` stays 10 long (`r10:0,10=4`). While a mark is valid, `read(char[],int,int)` never takes the direct path:
buffer 4 over `abcdefghijklmnopqrstuvwxyz`, `mark(6)`, the first `read(buf8,0,8)` returns 8 (`abcdefgh`) with
`r6:0,6=6 ready=true r6:0,6=6` (the first fill grows `cb` to 6, the second fill invalidates the mark); the next
`read(buf8,0,8)` copies the 4 buffered chars and fills again (len 4 < cb.length 6: `ready=true r6:0,6=6`), returns 8
(`ijklmnop`); `reset` throws `Mark invalid`; the third `read(buf8,0,8)` copies `qr` and then reads directly
(`ready=true r8:2,6=6`), returns 8 (`qrstuvwx`), because `markedChar == -2` is `<= UNMARKED`. Full log:
`r6:0,6=6 ready=true r6:0,6=6 ready=true r6:0,6=6 ready=true r8:2,6=6`. A new `mark` after an invalidation is valid
again: buffer 4 over `abcdefgh`, `read`, `mark(3)`, three `read`s, `read` gives `e` (its fill invalidates the mark),
`mark(2)`, `read` gives `f`, `reset`, `read` gives `f` again (log `r4:0,4=4 r4:0,4=4`). `readLine` `a` of `a\r\nb\nc`, `mark(5)`, `read` 98, `reset`, `readLine` `b`, `reset`, `read` 98:
`markedSkipLF` brings `skipLF` back on reset.

`close()`:

    if (in == null) return;
    try { in.close(); } finally { in = null; cb = null; }

A second `close()` does nothing (no second `in.close()`). If `in.close()` throws (`IOException:boom`) the reader is
closed anyway: the next `close()` returns normally and `read()` throws `Stream closed`. After close, `read()`,
`read(buf,0,1)`, `read(buf,-1,1)`, `read(buf,0,0)`, `readLine()`, `skip(1)`, `ready()`, `mark(1)`, `reset()`,
the inherited `read(char[])` and `transferTo(Writer)` throw `IOException("Stream closed")`; `skip(-1)` and
`mark(-1)` throw their `IllegalArgumentException`; `read((char[]) null)` throws `NullPointerException`.

Subclasses (like Tomcat's `CoyoteReader`): `BufferedReader` never calls its own overridable methods. A subclass that
overrides `read(char[],int,int)`, `ready()` and `mark(int)` sees nothing from `readLine()`, `read()`, `skip()`; it
sees `R3:0,3` from the inherited `read(char[])` on a 3-char array, and `R8192:0,8192` for each round of the inherited
`transferTo` (`Reader.transferTo` reads `read(buffer, 0, 8192)` until -1). The whole `Spy` log in the test is
`R3:0,3 Y R8192:0,8192 R8192:0,8192 M2`: the `Y` and `M2` are the fixture's own direct `p.ready()` and `p.mark(2)`
calls; `BufferedReader` itself adds none, because its read loop uses `in.ready()`.

## Steps

1. Read `javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeBufferedReaderTest.java`. `Src` is the
   recording source reader (`rL:off,len=n` per `read(char[],int,int)`, `ready=b` per `ready()`, `close`); `Spy` is
   the subclass.
2. Read `javaw-bytecode/src/main/resources/exception-runtime/java/io/Reader.java` (the `lock` constructors and the
   package-private `checkFromIndexSize`) and `StringReader.java` (style).
3. Create `javaw-bytecode/src/main/resources/exception-runtime/java/io/BufferedReader.java` with the members above,
   following the code blocks in "JDK behaviour to match".
4. Register it (never edit `BytecodeExceptionRuntime.java` by hand):
   `javaw-agent-kit/bin/kit register java/io/BufferedReader.java`
5. `javaw-agent-kit/bin/kit check T32` until PASS.
6. Write `docs/bytecode-buffered-reader.md` (below), the report, then `javaw-agent-kit/bin/kit finish T32`.

## Hints

- The constructor must call `super(in)` (the `Reader(Object lock)` constructor), not `super()`: that sets
  `lock = in` (the test checks `lock == in`) and gives the `NullPointerException` for a null reader before the size
  check.
- Use the runtime helper `checkFromIndexSize(off, len, cbuf.length)` from `Reader` (it is package-private and
  `BufferedReader` is in the same package). `Objects.checkFromIndexSize` does **not** exist in the runtime.
- `String(char[], int, int)`, `StringBuilder(int)`, `StringBuilder.append(char[], int, int)`, `Math.min(int, int)`
  and `System.arraycopy` exist in the runtime.
- Never write the words `synchronized`, `volatile`, `native` or `Thread` anywhere, not even in a comment. Do not
  mention the types `InternalLock`, `java.util.stream.Stream`, `StreamSupport`, `UncheckedIOException`, `CharBuffer`
  or `Readable` (no `lines()` method, no `Stream<String>`). The message strings "Stream closed" and
  "Stream not marked" are required; keep them exactly.
- Keep the JDK's call pattern; the most likely mistakes, each caught by the fixture named in the kit-hints:
  - a fixed request size instead of `cb.length - dst` in `fill()` (`markFills` expects `r4:3,1`);
  - forgetting the `do { ... } while (n == 0)` loop (`readSingle` expects three 0 results and then 2);
  - no direct path in `read1`, or a direct path that ignores the mark or `skipLF` (`readArrays`, `markFills`,
    `carriageReturns`);
  - calling `read1` once instead of the `while (n < len && in.ready())` loop, or calling `this.ready()` instead of
    `in.ready()` (`readArrays`, `subclassCalls`);
  - inheriting `Reader.read()` (it calls `read(cb, 0, 1)`, which a subclass sees as `R1:0,1`) instead of writing
    `read()` over the buffer (`subclassCalls`);
  - checking the range before `ensureOpen()` in `read(char[],int,int)` or `ensureOpen()` before the
    negative-argument checks in `skip`/`mark` (`closing`).
- `fill()` may leave `nextChar`/`nChars` unchanged when `in.read` returns -1; callers check `nextChar >= nChars`
  after every `fill()`.
- The Wasm runner stops at the **first** failing fixture of each `@Test` method, so after a fix another fixture of
  the same group may fail next. That is progress.
- Calibration mutations, each caught by the fixture named: inheriting `Reader.read()` plus `this.ready()` in the
  read loop plus the range check before `ensureOpen()` (`subclassCalls`, `closing`); a single `in.read` without the
  `while (n == 0)` retry, a direct path that ignores the mark, and `ready()` filling without asking `in.ready()`
  (`readSingle`, `markFills`, `carriageReturns`). Note that inheriting `Reader.read()` is invisible to the source
  reader (the calls look the same); only the `Spy` subclass sees the extra `R1:0,1`.
- Calibration: the reference passed kit check after 1 run (about 215 new lines, all in BufferedReader.java).

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
constructionAndSubclasses, readCharacters, readLines, carriageReturnAcrossCalls, skipAndReady, markAndReset, closing: if FIX THE RUNTIME says `link a complete acyclic type hierarchy: java/io/BufferedReader`, `link the descriptor class/interface java/io/BufferedReader` or `link the symbolic class/interface java/io/BufferedReader`, the class is missing or not registered: run `javaw-agent-kit/bin/kit register java/io/BufferedReader.java` (never edit BytecodeExceptionRuntime.java by hand). If it names a member (`link a matching instance method ...` or `link a matching special declaration`), add it exactly as listed under Members to implement. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes (remove any use of the Stream type, UncheckedIOException, CharBuffer or InternalLock; keep the "Stream closed" and "Stream not marked" messages).
constructors: the constructor is `super(in); if (sz <= 0) throw new IllegalArgumentException("Buffer size <= 0"); this.in = in; cb = new char[sz];` (super(in) sets lock = in and throws NullPointerException for null before the size check); BufferedReader(Reader) is `this(in, 8192)`; markSupported() is true; the constructor makes no call on in.
readSingle, constructors: read() is ensureOpen() then a loop: fill() when nextChar >= nChars (return -1 if still empty), swallow one '\n' when skipLF is set, else `return cb[nextChar++];`; fill() reads `in.read(cb, dst, cb.length - dst)` in `do { ... } while (n == 0)` and sets `nChars = dst + n; nextChar = dst;` only when n > 0.
readArrays, subclassCalls: read(cbuf, off, len) is ensureOpen(), checkFromIndexSize, `if (len == 0) return 0;`, `n = read1(...)`, `if (n <= 0) return n;`, then `while (n < len && in.ready())` more read1 calls (in.ready(), never this.ready()); read1 reads directly with `in.read(cbuf, off, len)` only when the buffer is empty and `len >= cb.length && markedChar <= UNMARKED && !skipLF`, otherwise it fills cb and copies `Math.min(len, nChars - nextChar)` chars.
readRanges: read(cbuf, off, len) must call `checkFromIndexSize(off, len, cbuf.length)` (the Reader helper, message `Range [off, off + len) out of bounds for length L`) before anything touches in, and return 0 for len == 0 without a call.
readLineTerminators, readLineCalls: readLine() follows the code block in the task: only '\n' and '\r' end a line, a '\r' sets skipLF so that a '\n' at the start of the next fill or call is skipped (omitLF), a line longer than the buffer is collected in a StringBuilder across fills, and at the end it returns the collected text only if `s != null && s.length() > 0`, else null.
readFailures: an IOException from in.read propagates unchanged out of fill(); follow the fill() code block exactly (with no mark it changes nothing before in.read, so the next call carries on with the next chars). The part of a line already collected in readLine's local StringBuilder is lost, and so are chars that read(char[],int,int) already copied before a later read1 throws. Both are correct.
carriageReturns: after readLine() returned a line ending in '\r', skipLF is true: read() swallows one '\n', read1 swallows it (and may fill twice), skip swallows it without counting, ready() swallows it after `if (nextChar >= nChars && in.ready()) fill();`; read1 must not take the direct path while skipLF is set.
skipping: skip(n) throws `new IllegalArgumentException("skip value is negative")` for n < 0 before ensureOpen(); skip(0) makes no call; otherwise it moves nextChar through the buffer, filling as needed, swallows a skipLF '\n' without counting it, and returns the count skipped (0 at the end); it never calls this.read.
readiness, carriageReturns: ready() is ensureOpen(), the skipLF block from the task, then `return nextChar < nChars || in.ready();` (true without asking in when chars are buffered).
markReset: reset() is ensureOpen() then `if (markedChar < 0) throw new IOException(markedChar == INVALIDATED ? "Mark invalid" : "Stream not marked");` then `nextChar = markedChar; skipLF = markedSkipLF;`; mark(limit) throws `new IllegalArgumentException("Read-ahead limit < 0")` before ensureOpen(), then stores readAheadLimit, `markedChar = nextChar` and `markedSkipLF = skipLF`; the mark is only invalidated inside fill().
markFills: fill() with a mark: `delta = nextChar - markedChar`; if `delta >= readAheadLimit` set markedChar = INVALIDATED (-2), readAheadLimit = 0, dst = 0; else copy the marked chars to the front of cb (or of a new `char[readAheadLimit]` when readAheadLimit > cb.length, which replaces cb for good), markedChar = 0, `dst = delta; nextChar = nChars = delta;`, then read `in.read(cb, dst, cb.length - dst)`.
closing: close() is `if (in == null) return; try { in.close(); } finally { in = null; cb = null; }`; every other method starts with ensureOpen() (`new IOException("Stream closed")`), but skip(-1) and mark(-1) throw IllegalArgumentException first and read(char[],int,int) calls ensureOpen() before the range check; markSupported() stays true.
subclassCalls: BufferedReader must not call its own overridable methods: read(), readLine(), skip() and the ready loop use in.read/in.ready and the buffer directly; do not override read(char[]) or transferTo (the inherited Reader versions call this.read(cbuf, 0, len) and this.read(buffer, 0, 8192)).
```

## Docs

Create `docs/bytecode-buffered-reader.md` with this skeleton (fill in the brackets):

    # Bytecode BufferedReader

    Revision `buffered-reader-vN` links `java.io.BufferedReader` in the bundled exception runtime. Like `Reader`,
    it keeps the JDK `lock` field (the wrapped reader) but never enters a monitor (single-threaded contract).

    Contract:

    - <constructors and the buffer size check; fill pattern (whole free buffer, retry on 0); read() and
      read(char[],int,int) with the direct path and the ready() loop; readLine with \n, \r and \r\n across calls
      and fills; skip; ready; mark/reset with the read-ahead limit, buffer growth and invalidation; close and the
      closed-stream failures>

    Not linked: `lines()` (needs `java.io.UncheckedIOException` and is unused by the pinned libraries), so callers
    are rejected before WAT generation; `read(java.nio.CharBuffer)`.

    Evidence: `BytecodeBufferedReaderTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders,
    two fresh instances). `BufferedReader.lines()` is rejected before WAT generation.
