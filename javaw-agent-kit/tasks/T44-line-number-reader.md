# T44 LineNumberReader

```kit
id: T44
slug: line-number-reader
title: link java.io.LineNumberReader
tests: BytecodeLineNumberReaderTest
depends: T32
allow: javaw-bytecode/src/main/resources/exception-runtime/java/io/LineNumberReader.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/io/BufferedReader.java
allow: docs/bytecode-line-number-reader.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-line-number-reader-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeLineNumberReaderTest.java`.

## Goal

Add `java.io.LineNumberReader` to the bundled runtime, as a subclass of the runtime `BufferedReader` (task T32,
done). The pinned libraries name `java/io/LineNumberReader` in 5 classes (`runtime-target/target/audit/external-types.tsv`)
and call it from 3 artifacts (`member-references.tsv`): `<init>(Reader)` 7 times, `readLine()` 8, `close()` 4,
`read()` 5 and `getLineNumber()` once. Users: H2 `ConvertTraceFile.convertFile` and `Profiler.readStackTrace`,
Spring JDBC `ScriptUtils.readScript` (SQL scripts line by line) and Spring Boot
`OriginTrackedPropertiesLoader$CharacterReader` (reads `application.properties` char by char with `read()` and
reports `getLineNumber()` in origins).

Copy the JDK 21 class **exactly** (without its locks). Its `readLine()` calls a package-private
`BufferedReader.readLine(boolean ignoreLF, boolean[] term)`, which the runtime `BufferedReader` does not have yet,
so this task also changes `BufferedReader.java` (only that method split, nothing else).

## Members to implement

1. In `javaw-bytecode/src/main/resources/exception-runtime/java/io/BufferedReader.java`:
   - `String readLine(boolean ignoreLF, boolean[] term) throws IOException` (package-private, no modifier): the
     current body of `readLine()` with three changes (see Steps).
   - `public String readLine() throws IOException { return readLine(false, null); }`
2. New file `javaw-bytecode/src/main/resources/exception-runtime/java/io/LineNumberReader.java` (`package java.io;`,
   no imports): `public class LineNumberReader extends BufferedReader` with
   - fields: `private static final int NONE = 0, CHAR = 1, EOL = 2, EOF = 3;`, `private int prevChar = NONE;`,
     `private int lineNumber = 0;`, `private int markedLineNumber;`, `private boolean skipLF;`,
     `private boolean markedSkipLF;`, `private static final int maxSkipBufferSize = 8192;`,
     `private char[] skipBuffer = null;` (its own `skipLF`, separate from the private one in `BufferedReader`)
   - `public LineNumberReader(Reader in)` (`super(in);`)
   - `public LineNumberReader(Reader in, int sz)` (`super(in, sz);`)
   - `public void setLineNumber(int lineNumber)`
   - `public int getLineNumber()`
   - `public int read() throws IOException`
   - `public int read(char[] cbuf, int off, int len) throws IOException`
   - `public String readLine() throws IOException`
   - `public long skip(long n) throws IOException`
   - `public void mark(int readAheadLimit) throws IOException`
   - `public void reset() throws IOException`

Nothing else. Do not override `close()`, `ready()`, `markSupported()`, `read(char[])` or `transferTo(Writer)`.

## Members to leave out

- `lines()` stays unlinked (it belongs to `BufferedReader`, see T32). Do not add `UncheckedIOException` or streams.
- Do not change anything else in `BufferedReader.java` (`kit guard` allows the file, the T32 test must still pass).

## JDK behaviour to match

`lineNumber` starts at 0 and counts line terminators: `\n`, `\r` or `\r\n` (one line). `setLineNumber` just stores
the value (`-5` is fine; `Integer.MAX_VALUE` then one more line gives `-2147483648`). In JDK 21 the end of input also
counts one line when the last char before it was an ordinary char: this happens in `read()`, `read(char[],int,int)`
and `readLine()` (not only in `readLine`): `x` read char by char gives `120@0 -1@1 -1@1` (`value@getLineNumber()`).
`prevChar` (NONE, CHAR, EOL, EOF) remembers the last thing seen; only `prevChar == CHAR` at the end adds the line.

`read()`:

    int c = super.read();
    if (skipLF) { if (c == '\n') c = super.read(); skipLF = false; }
    if (c == '\r' || c == '\n') { if (c == '\r') skipLF = true; lineNumber++; prevChar = EOL; return '\n'; }
    if (c == -1) { if (prevChar == CHAR) lineNumber++; prevChar = EOF; return -1; }
    prevChar = CHAR;
    return c;

So `\r` and `\r\n` are returned as one `\n` (10): `a\nb\r\nc\rd` reads `97@0 10@1 98@1 10@2 99@2 10@3 100@3 -1@4 -1@4`
for every buffer size. `\r\r\n\n` gives `10@1 10@2 10@3 -1@3`. `\u2028`, `\u0085`, `\u000b` and `\f` are ordinary
chars (end of input after them counts a line).

`read(cbuf, off, len)` (chars are **not** translated; `\r` stays in the array):

    int n = super.read(cbuf, off, len);
    if (n == -1) { if (prevChar == CHAR) lineNumber++; prevChar = EOF; return -1; }
    for (int i = off; i < off + n; i++) {
        int c = cbuf[i];
        if (skipLF) { skipLF = false; if (c == '\n') continue; }
        if (c == '\r') { skipLF = true; lineNumber++; } else if (c == '\n') lineNumber++;
    }
    if (n > 0) { char last = cbuf[off + n - 1]; prevChar = (last == '\r' || last == '\n') ? EOL : CHAR; }
    return n;

Range checks and `Stream closed` come from `BufferedReader` (`Range [-1, -1 + 1) out of bounds for length 4`).
`len == 0` returns 0 and changes nothing. A `\r\n` split across two calls counts once: over `a\r\nb\n`,
`read(buf,0,2)` gives `2@1`, `read(buf,0,1)` gives the `\n` at `@1`. After `read()` returned 10 for a `\r`, the
`\n` is still in the stream: over `\r\nz`, `read()` is `10@1`, then `read(buf,0,8)` returns `2` (`\nz`) at `@1`.

`readLine()`:

    boolean[] term = new boolean[1];
    String l = super.readLine(skipLF, term);
    skipLF = false;
    if (l != null) { lineNumber++; prevChar = term[0] ? EOL : EOF; }
    else { if (prevChar == CHAR) lineNumber++; prevChar = EOF; }
    return l;

So `no newline` gives `[no newline]1[null]1`; after `read()` of `a` and `b` (over `ab`), `readLine()` returns null
and the count becomes 1. After `read()` returned 10 for `\r` (over `a\r\nb\nc`), `readLine()` skips the `\n`
(`ignoreLF`) and returns `b`.

The new `BufferedReader.readLine(ignoreLF, term)` is the old body with `boolean omitLF = ignoreLF || skipLF;`,
`if (term != null) term[0] = false;` right after it, and `if (term != null) term[0] = true;` where a `\n` or `\r`
is found in the char loop.

`skip(n)`:

    if (n < 0) throw new IllegalArgumentException("skip() value is negative");
    int nn = (int) Math.min(n, maxSkipBufferSize);
    if ((skipBuffer == null) || (skipBuffer.length < nn)) skipBuffer = new char[nn];
    long r = n;
    while (r > 0) { int nc = read(skipBuffer, 0, (int) Math.min(r, nn)); if (nc == -1) break; r -= nc; }
    if (n - r > 0) prevChar = NONE;
    return n - r;

The message is `skip() value is negative` (with `()`, unlike BufferedReader). The negative check comes before
anything else (also when closed); `skip(0)` returns 0 and makes no call even when closed; `skip(1)` when closed is
`IOException: Stream closed`. `skip` calls the **overridable** `read(char[],int,int)` of `this`, so a subclass sees
it (`R4:0,4`), and the skip buffer is kept and reused: `skip(Long.MAX_VALUE)` allocates 8192 chars, a later
`skip(5)` calls `read(buf8192, 0, 5)`. Because `prevChar` becomes NONE after a skip that skipped something, the end
of input right after a skip does not count a line: over `ab`, `read()`, `skip(1)` then `read()` gives `-1@0`.

`mark(readAheadLimit)`: `if (skipLF) readAheadLimit++;` then `super.mark(readAheadLimit);` then
`markedLineNumber = lineNumber; markedSkipLF = skipLF;`. The increment is observable: buffer 2 over `\r\nab`,
`read()` (10), `mark(1)`, `read()` (97, which reads the `\n` and the `a`), `reset()` works; `mark(0)` instead gives
`IOException: Mark invalid` later. After a `\r`, `mark(Integer.MAX_VALUE)` overflows to a negative limit and throws
`IllegalArgumentException: Read-ahead limit < 0`; `mark(-1)` gives the same.

`reset()`: `super.reset();` then `lineNumber = markedLineNumber; skipLF = markedSkipLF;`. A failed reset
(`Stream not marked`) changes nothing. `prevChar` is not saved: over `a`, `read()`, `mark(5)`, `read()` (`-1@1`),
`reset()` (`0`), `read()` gives `-1@0`.

`close()`, `ready()`, `markSupported()` are inherited. After `close()`, `getLineNumber()` and `setLineNumber()` still
work; `read`, `readLine`, `skip(1)`, `mark(1)`, `reset`, `ready` throw `IOException: Stream closed`.

## Steps

1. Read the test (`Src` is a recording source reader, `rL:off,len=n` per `read(char[],int,int)`; `Spy` extends
   `LineNumberReader` and logs `RL:off,len` for each `read(char[],int,int)` it receives) and
   `javaw-bytecode/src/main/resources/exception-runtime/java/io/BufferedReader.java`.
2. In `BufferedReader.java`, rename `public String readLine() throws IOException {` to
   `String readLine(boolean ignoreLF, boolean[] term) throws IOException {`; in its body change
   `boolean omitLF=skipLF;` to `boolean omitLF=ignoreLF||skipLF;if(term!=null)term[0]=false;` and
   `if(c=='\n'||c=='\r'){eol=true;break;}` to `if(c=='\n'||c=='\r'){if(term!=null)term[0]=true;eol=true;break;}`.
   Then add `public String readLine() throws IOException {return readLine(false,null);}` above it.
3. Create `LineNumberReader.java` with the members above, following the code blocks.
4. Register it (never edit `BytecodeExceptionRuntime.java` by hand):
   `javaw-agent-kit/bin/kit register java/io/LineNumberReader.java`
5. `javaw-agent-kit/bin/kit check T44` until PASS; also `javaw-agent-kit/bin/kit check T32` must still pass.
6. Write `docs/bytecode-line-number-reader.md` (below), the report, then `javaw-agent-kit/bin/kit finish T44`.

## Hints

- Use `super.read()`, `super.read(cbuf, off, len)` and `super.readLine(skipLF, term)` exactly where the code blocks
  do; `skip` must call `read(skipBuffer, ...)` without `super.` (the `Spy` subclass checks it).
- `read()` returns `'\n'` (10) for both `\r` and `\n`; `read(char[],int,int)` never changes the chars.
- `Math.min(long, long)` and `Math.min(int, int)` exist in the runtime. No `switch` is needed; the if/else in the
  code blocks is enough. Never write `synchronized` (the JDK has `synchronized (lock)` blocks; drop them).
- The end-of-input line (`prevChar == CHAR`) is counted in all three read methods, not only in `readLine`.
- The pinned code is single-threaded here; the JDK's `synchronized (lock)` wrappers have no observable effect.
- Calibration: the reference passed kit check after 1 run (about 66 new lines: 62 in LineNumberReader.java, 3
  changed in BufferedReader.java, 1 line added by kit register).
- The constructors only call `super(in)` / `super(in, sz)`: `new LineNumberReader(s, 0)` and `(s, -1)` throw
  IllegalArgumentException "Buffer size <= 0" and a null reader throws NullPointerException from BufferedReader
  unchanged; add no checks of your own.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
constructionAndSubclasses, readCharacters, readArrays, readLines, skip, markAndReset, closing: if FIX THE RUNTIME says `link a complete acyclic type hierarchy: java/io/LineNumberReader`, `link the descriptor class/interface java/io/LineNumberReader` or `link the symbolic class/interface java/io/LineNumberReader`, the class is missing or not registered: run `javaw-agent-kit/bin/kit register java/io/LineNumberReader.java` (never edit BytecodeExceptionRuntime.java by hand). If it names a member, add it exactly as listed under Members to implement (including the package-private BufferedReader.readLine(boolean, boolean[])). If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
readSingle, constructors: read() is `c = super.read()`, then the skipLF block (a '\n' right after a '\r' is read past with another super.read()), then '\r' or '\n' -> lineNumber++, prevChar = EOL, return '\n' (a '\r' also sets skipLF); -1 -> lineNumber++ only if prevChar == CHAR, prevChar = EOF; any other char -> prevChar = CHAR.
readArrays: read(cbuf, off, len) returns super.read's chars unchanged; it counts '\n' and '\r' in cbuf[off..off+n), skipping one '\n' right after a '\r' (skipLF survives between calls); -1 adds a line only when prevChar == CHAR; prevChar is EOL or CHAR from the last char returned.
readLines, mixedCalls: readLine() is `super.readLine(skipLF, term)` with a new boolean[1], then skipLF = false; a non-null line adds 1 and sets prevChar = term[0] ? EOL : EOF; null adds 1 only when prevChar == CHAR. BufferedReader.readLine(ignoreLF, term) must start with `omitLF = ignoreLF || skipLF` and set term[0] = true when it finds '\n' or '\r'.
skipping: skip throws `new IllegalArgumentException("skip() value is negative")` first, keeps a skipBuffer of min(n, 8192) chars (reused when big enough), loops `read(skipBuffer, 0, (int) Math.min(r, nn))` on this (not super) until -1, and sets prevChar = NONE when it skipped anything.
markReset: mark adds 1 to the limit when skipLF is set before super.mark, then saves lineNumber and skipLF; reset is super.reset() first, then restores lineNumber and skipLF (prevChar is not saved).
closing: close, ready and markSupported are inherited from BufferedReader; getLineNumber and setLineNumber work after close; skip(-1) throws IllegalArgumentException before the stream check, skip(0) returns 0 without reading.
subclassCalls: of the LineNumberReader methods, only skip calls this.read(char[],int,int); read() and readLine() use super.read()/super.readLine and never this.read. The inherited read(char[]) and transferTo(Writer) also reach this.read(char[],int,int) through Reader (transferTo with an 8192-char buffer), so the Spy logs those calls too; do not override them.
```

## Docs

Create `docs/bytecode-line-number-reader.md` with this skeleton (fill in the brackets):

    # Bytecode LineNumberReader

    Revision `line-number-reader-vN` links `java.io.LineNumberReader` in the bundled exception runtime, on top of
    `BufferedReader` (which gains the package-private `readLine(boolean, boolean[])` the JDK subclass calls).

    Contract:

    - <line counting of \n, \r and \r\n in read(), read(char[],int,int) and readLine(), including split \r\n
      and the end-of-input line after an ordinary char; read() returning \n for \r; setLineNumber/getLineNumber;
      skip through this.read with a reused buffer; mark/reset restoring the line number and the +1 read-ahead
      limit after \r; failures after close>

    Evidence: `BytecodeLineNumberReaderTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
    orders, two fresh instances).
