# T34 OutputStreamWriter

```kit
id: T34
slug: output-stream-writer
title: link java.io.OutputStreamWriter and the PrintWriter(OutputStream) constructors
tests: BytecodeOutputStreamWriterTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/io/OutputStreamWriter.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/io/PrintWriter.java
allow: docs/bytecode-output-stream-writer.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-output-stream-writer-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeOutputStreamWriterTest.java`. It also uses
`ByteArrayOutputStream` (T19) and `PrintWriter` (T27), which are already in the runtime.

## Goal

Add `java.io.OutputStreamWriter` to the bundled runtime: a `Writer` that encodes characters into bytes for an
`OutputStream`, with the charsets the runtime has (UTF-8, ISO-8859-1, US-ASCII, UTF-16BE, UTF-16LE, UTF-16). Then add
the two `PrintWriter(OutputStream)` constructors that T27 left out. The pinned libraries name `OutputStreamWriter`
from 34 classes (`runtime-target/target/audit/external-types.tsv`: Jackson `JsonFactory`, Jakarta Activation
`ObjectDataContentHandler`, ANTLR `Utils`, Tomcat `DefaultServlet`, `MemoryUserDatabase`, `URLEncoder`,
`HttpServlet$NoBodyPrintWriter`, JAXB `XMLWriter`, Spring `ContentCachingResponseWrapper$ResponsePrintWriter`, ...).
The member rows (`member-references.tsv`) are `<init>(OutputStream, Charset)` 19 call sites,
`<init>(OutputStream, String)` 13, `<init>(OutputStream)` 9, `flush()` 8, `write(String)` 6, `close()` 6,
`write(int)` 3 and `append(char)` 1. `PrintWriter.<init>(OutputStream)` has 12 call sites (H2 `MVStoreTool`,
AspectJ `MessageWriter`, Tomcat `HttpServlet$NoBodyPrintWriter`) and `<init>(OutputStream, boolean)` 4 (Log4j
`LowLevelLogUtil`, AspectJ `IMessageHandler`).

The JDK class hands all work to `sun.nio.cs.StreamEncoder`, which uses `CharsetEncoder`, `ByteBuffer` and
`CharBuffer`. None of those exist in the runtime, and the runtime `Charset` has no `newEncoder()`. So write the
encoder **inside** `OutputStreamWriter.java`: a byte buffer, a pending high surrogate, and a small `put` method that
encodes one code point. The JDK methods take a lock; here the runtime is single-threaded, so take none (RULES.md).

## Members to implement

### `java/io/OutputStreamWriter.java` (new)

`package java.io;`, `import java.nio.charset.Charset;`, `public class OutputStreamWriter extends Writer`.

Fields (all `private`):

- `static final String[] HISTORICAL = {"UTF8", "ISO8859_1", "ASCII", "UnicodeBigUnmarked", "UnicodeLittleUnmarked", "UTF-16"};`
- `static final int[] MAX_BYTES = {3, 1, 1, 2, 2, 4};` (the JDK encoders' `maxBytesPerChar`, rounded)
- `final OutputStream out;`
- `final int charset;` (the runtime charset number: 0 UTF-8, 1 ISO-8859-1, 2 US-ASCII, 3 UTF-16BE, 4 UTF-16LE,
  5 UTF-16; it indexes the two arrays above)
- `final byte[] bytes = new byte[8192];` (the encoded bytes not yet given to `out`)
- `final byte[] unit = new byte[4];` (scratch space for the bytes of one code point)
- `int capacity = 512;` (how much of `bytes` may be used now; it grows, see below)
- `int count;` (bytes waiting in `bytes[0..count)`)
- `boolean closed;`
- `boolean needsMark;` (UTF-16 only: the byte order mark is still to be written)
- `boolean haveLeftover;` and `char leftover;` (a high surrogate that ended the previous write)

Constructors:

- `public OutputStreamWriter(OutputStream out, String charsetName) throws UnsupportedEncodingException`
- `public OutputStreamWriter(OutputStream out)`
- `public OutputStreamWriter(OutputStream out, Charset cs)`

Methods:

- `public String getEncoding()`
- `public void write(int c) throws IOException`
- `public void write(char[] cbuf, int off, int len) throws IOException`
- `public void write(String str, int off, int len) throws IOException`
- `public Writer append(CharSequence csq) throws IOException` (return type `Writer`)
- `public void flush() throws IOException`
- `public void close() throws IOException`
- private helpers: `void ensureOpen() throws IOException`, `void writeString(String str, int off, int len) throws
  IOException`, `void writeChars(char[] cbuf, int off, int len) throws IOException`, `void grow(int len) throws
  IOException`, `void put(int c) throws IOException`, `void flushBytes() throws IOException` (more private helpers,
  for example one that stores a 16-bit unit into `unit`, are fine)

Do **not** override `write(char[])`, `write(String)`, `append(CharSequence, int, int)` or `append(char)`: the
inherited `Writer` versions are exactly the JDK behaviour (they call the overridable methods of `this`, and the test
checks that). The JDK class does override `append(CharSequence, int, int)`, but with the same body as `Writer`'s
(`if (csq == null) csq = "null"; return append(csq.subSequence(start, end));`); inheriting it is equivalent, so do not
add it.

### `java/io/PrintWriter.java` (existing, from T27)

Add exactly two constructors next to the two `Writer` ones and change nothing else:

- `public PrintWriter(OutputStream out)`: `this(out, false);`
- `public PrintWriter(OutputStream out, boolean autoFlush)`: `this(new OutputStreamWriter(out), autoFlush);`

## Members to leave out

- `OutputStreamWriter(OutputStream, CharsetEncoder)`: there is no `CharsetEncoder` in the runtime.
- The package-private `OutputStreamWriter.flushBuffer()` (only the JDK's `PrintStream` calls it; there is no
  `PrintStream`) and the JDK's private `lockFor`.
- `PrintWriter(OutputStream, boolean, Charset)` (no pinned call sites): the test checks that it stays unlinked, so do
  **not** add it. Also still leave out every `String`/`File` `PrintWriter` constructor (`PrintWriter(String)` is
  checked by the T27 test).
- No `BufferedWriter`. The JDK `PrintWriter(OutputStream, boolean)` is
  `this(new BufferedWriter(new OutputStreamWriter(out, charset)), autoFlush)`. There is no `BufferedWriter` in the
  runtime, so here the `PrintWriter` wraps the `OutputStreamWriter` directly. That is a known difference (see the
  end of "JDK behaviour to match"); the fixtures do not observe it.
- Do not edit any other runtime file: not `Writer.java`, `OutputStream.java`, `Charset.java`, `StandardCharsets.java`
  or `javaw/runtime/TextCharsets.java`.

## JDK behaviour to match

Every value below was checked on JDK 21.

### Constructors and `getEncoding`

All three constructors start with `super(out);`. The runtime `Writer(Object lock)` throws `NullPointerException`
(no message) for `null`, so a `null` stream always fails first, whatever the other argument is
(`new OutputStreamWriter(null, (String) null)` and `new OutputStreamWriter(null, "x-no-such-charset")` throw
`NullPointerException` with message `null`). `super(out)` also makes the protected `lock` field the stream
(a subclass sees `lock == out`).

- `(out, String charsetName)`: `super(out); if (charsetName == null) throw new NullPointerException("charsetName");`
  then `charset = javaw.runtime.TextCharsets.lookup(charsetName);`. That runtime method already throws
  `UnsupportedEncodingException(charsetName)` for an unknown or illegal name, like the JDK: the messages are the
  name itself (`""` gives message `""`, and `x-no-such-charset`, `bad name`, `_utf8`, `utf-8?` give those names).
  Names match the aliases case-insensitively: `UTF-8`, `utf8`, `Utf-8`, `ISO-8859-1`, `latin1`, `ISO8859_1`,
  `8859_1`, `US-ASCII`, `ascii`, `646`, `UTF-16BE`, `x-UTF-16LE`, `utf-16` and `UnicodeBig` all work.
- `(out, Charset cs)`: `super(out); if (cs == null) throw new NullPointerException("charset");` then
  `charset = javaw.runtime.TextCharsets.identifier(cs);`.
- `(out)`: `this(out, Charset.defaultCharset());` (UTF-8, as on JDK 18 and later).
- The `(out, String)` and `(out, Charset)` constructors also set `this.out = out;` and `needsMark = charset == 5;`
  (after `charset` is assigned). The `(out)` constructor only calls `this(out, Charset.defaultCharset());` and assigns
  nothing itself (the fields are `final`).
- `getEncoding()`: `closed ? null : HISTORICAL[charset]`. These are the JDK's historical names: `UTF8`,
  `ISO8859_1`, `ASCII`, `UnicodeBigUnmarked`, `UnicodeLittleUnmarked`, `UTF-16` (not the canonical names). After
  `close()`, also a `close()` that threw, it returns `null`.

### Encoding one code point: `put(int c)`

`put` receives a code point (`0` to `0x10FFFF`) or `-1` for a malformed input (a lone surrogate). It fills `unit`
with `n` bytes, then: `if (capacity - count < n) flushBytes();` and copies the `n` bytes to `bytes[count..]`
(`count += n`). So a code point is never split between two writes to `out`.

- UTF-8 (0): `-1` -> `?` (0x3F). Else 1 byte below 0x80; 2 bytes below 0x800 (`0xC0 | (c >> 6)`,
  `0x80 | (c & 0x3F)`); 3 bytes below 0x10000 (`0xE0 | (c >> 12)`, `0x80 | ((c >> 6) & 0x3F)`, `0x80 | (c & 0x3F)`);
  else 4 bytes (`0xF0 | (c >> 18)`, `0x80 | ((c >> 12) & 0x3F)`, `0x80 | ((c >> 6) & 0x3F)`, `0x80 | (c & 0x3F)`).
  `"A\u00e9\u20ac\ud83d\ude00"` -> `41 c3a9 e282ac f09f9880`; `"\u0000\u007f\u0080\u07ff\u0800\uffff"` ->
  `00 7f c280 dfbf e0a080 efbfbf`; `"\udbff\udfff"` -> `f48fbfbf`.
- ISO-8859-1 (1) and US-ASCII (2): one byte, `c` itself when `0 <= c <= 0xFF` (ISO-8859-1) or `0 <= c <= 0x7F`
  (US-ASCII), else `?`. A surrogate pair is one code point, so it becomes **one** `?`. For
  `"A\u00e9\u00ff\u0100\u20ac\ud83d\ude00\udc00\ud800x\u007f\u0080"` ISO-8859-1 gives `41 e9 ff 3f 3f 3f 3f 3f 78 7f 80`
  and US-ASCII `41 3f 3f 3f 3f 3f 3f 3f 78 7f 3f`.
- UTF-16BE (3), UTF-16LE (4), UTF-16 (5): `-1` becomes `0xFFFD`. A code point below 0x10000 is one 16-bit unit (2
  bytes), a larger one is two units, `Character.highSurrogate(c)` then `Character.lowSurrogate(c)` (4 bytes).
  UTF-16BE and UTF-16 write each unit high byte first, UTF-16LE low byte first. UTF-16 writes the byte order mark
  `fe ff` before its first character (see `writeChars`). For `"A\u00e9\ud83d\ude00\udc00\ud800x"`: UTF-16BE
  `0041 00e9 d83d de00 fffd fffd 0078`, UTF-16LE `4100 e900 3dd8 00de fdff fdff 7800`, UTF-16
  `feff 0041 00e9 d83d de00 fffd fffd 0078`.

### Surrogates: `writeChars(char[] cbuf, int off, int len)`

Every write ends up here. In order:

1. `ensureOpen();` (`if (closed) throw new IOException("Stream closed");`)
2. The JDK range check, exactly in this order (so a `null` array with `off < 0` is an `IndexOutOfBoundsException`,
   with `off == 0` a `NullPointerException`):
   `if ((off < 0) || (off > cbuf.length) || (len < 0) || ((off + len) > cbuf.length) || ((off + len) < 0)) throw new IndexOutOfBoundsException();`
   The exception has **no message** (`getMessage()` is `null`).
3. `if (len == 0) return;` (an empty write changes nothing, not even the byte order mark or a pending surrogate).
4. Let `end = off + len`. First finish a pending high surrogate, one char at a time, while `haveLeftover && off < end`:
   take `char c = cbuf[off++]`; if `c` is a low surrogate, `put(Character.toCodePoint(leftover, c)); haveLeftover = false;`;
   otherwise `put(-1)` and then, if `c` is a high surrogate, `leftover = c` (still pending), else
   `put(c); haveLeftover = false;`. Clear `haveLeftover` only after the `put` calls: if a `put` makes `out.write`
   throw, the surrogate must still be pending so that `close()` writes its replacement, as the JDK does.
5. `grow(end - off);` (the chars that are left).
6. `if (needsMark) { needsMark = false; put(0xFEFF); }`
7. Then for each remaining char `c = cbuf[off++]`:
   - a high surrogate as the **last** char: `haveLeftover = true; leftover = c;` (it waits for the next write).
     For ISO-8859-1 only, first write out the buffer if it is exactly full (`count == capacity`): the JDK's
     ISO-8859-1 encoder reports overflow before it looks at a trailing high surrogate. The same applies when a
     pending surrogate is followed by another high surrogate as the last char in step 4;
   - a high surrogate followed by a low surrogate: `put(Character.toCodePoint(c, cbuf[off++]))`;
   - a high surrogate followed by anything else: `put(-1)` (the next char is handled normally);
   - a lone low surrogate: `put(-1)`;
   - anything else: `put(c)`.

So a pair split over two writes is still one code point: `write("a\ud83d"); flush();` gives `61` and then
`write("\ude00b"); flush();` gives `f09f9880 62`; `write(0xd83d); write(0xde01);` gives `f09f9881`. A high surrogate
followed by `y` in the next write gives `3f 79`; `write("\ud800"); write("\ud83d"); write("\ude00");` gives
`3f f09f9880`; `write("\ud83d"); write(""); write("\ude03");` gives `f09f9883`. `flush()` does not touch the pending
surrogate (it sends only the bytes before it). In UTF-16 the mark comes once, before the first character of the first
non-empty write: `write("a"); write("b");` gives `feff 0061 0062`, and a writer that only wrote `""` writes no bytes.

### Writes

- `write(int c)`: `writeChars(new char[] {(char) c}, 0, 1);` (the int is cast to char: `write(0x1f600)` writes
  `\uf600`, `ef 98 80` in UTF-8).
- `write(char[] cbuf, int off, int len)`: `writeChars(cbuf, off, len);`.
- `write(String str, int off, int len)`: `writeString(str, off, len);`.
- `writeString(str, off, len)`: `if (len < 0) throw new IndexOutOfBoundsException();` (no message, checked first, so
  `write((String) null, 0, -1)` and `write("abc", 0, -1)` give it even after `close()`), then
  `char[] cbuf = new char[len]; str.getChars(off, off + len, cbuf, 0); writeChars(cbuf, 0, len);`. A bad `off` makes
  `getChars` throw `StringIndexOutOfBoundsException` (also after close), a `null` string `NullPointerException`; the
  test compares only the type of those two. The runtime `String.getChars` and `subSequence` already throw
  `StringIndexOutOfBoundsException` for a bad range; add no range check of your own for `off`.
- `append(CharSequence csq)`: `String s = String.valueOf(csq); writeString(s, 0, s.length()); return this;` (`null`
  writes `null`).
- None of `write(int)`, `write(char[], int, int)`, `write(String, int, int)` and `append(CharSequence)` calls another
  overridable method. The inherited `Writer` methods do: `write(char[])` calls `write(cbuf, 0, cbuf.length)`,
  `write(String)` calls `write(str, 0, str.length())`, `append(csq, start, end)` calls `append(csq.subSequence(...))`
  and `append(char)` calls `write(int)`. The fixture's `Spy` subclass records exactly this:
  `c(a)a(0,2)S(de)s(de,0,2)s(fgh,1,1)A(ij)A(kl)A(null)c(m)` for `write('a'); write(new char[]{'b','c'}); write("de");
  write("fgh",1,1); append("ij"); append("xkl",1,3); append(null); append('m');`.

### Buffering: when bytes reach `out`

The JDK keeps the encoded bytes and gives them to `out` only on `flush()`, `close()`, when the next code point does
not fit (`put`), and when the buffer grows (`grow`). The bytes always go out as **one** call
`out.write(bytes, 0, count)`, never `out.write(int)`. The test records every call, so copy these rules exactly:

- `flushBytes()`: `if (count > 0) { int n = count; count = 0; out.write(bytes, 0, n); }`. Set `count = 0` **before**
  calling `out.write`, so bytes that `out` rejected are not sent again (see "Failures").
- `grow(int len)`: `if (capacity < 8192) { int n = Math.min(len * MAX_BYTES[charset], 8192); if (n > capacity) { flushBytes(); capacity = n; } }`.
  The capacity starts at 512 and never shrinks.
- Examples (UTF-8 unless named; `[n]` is one `out.write(bytes, 0, n)`, `F` one `out.flush()`):
  - `write("hello")` -> nothing; `flush()` -> `[5] F`; `flush()` again -> `F` only.
  - 100 `a` -> nothing; then 500 `b` (`500 * 3 = 1500 > 512`) -> `[100]` and capacity 1500; then 1000 `c` -> `[500]`
    (capacity 3000); 3000 `d` -> `[1000]` (capacity 8192); 5000 `e` -> nothing; `flush()` -> `[8000] F`.
  - 8192 `a` in one write -> nothing (it fits exactly); then `write('b')` -> `[8192]`; then 10000 `c` -> `[8192]`;
    `close()` -> `[1809] F C`.
  - 2731 `\u20ac` (3 bytes each) -> `[8190]` (the 2731st does not fit in the last 2 bytes); then 3000 `\u0000`
    (`write(new char[3000])`) -> nothing (1 byte each, capacity already 8192); `flush()` -> `[3003] F`.
  - ISO-8859-1: 600 `a` -> nothing (capacity 600); `write('b')` -> `[600]`.
  - US-ASCII: 513 single `write('x')` calls -> `[512]` at the 513th (each write is 1 char, so no growth).
  - 509 single `write('y')` then `write("\ud83d\ude00")` -> `[509]` (the 4-byte pair does not fit in 3 bytes).
  - UTF-16: 200 `z` (`200 * 4 = 800`) -> nothing; then `z` -> nothing; `flush()` -> `[404] F` (mark + 201 units).

### `flush` and `close`

- `flush()`: `ensureOpen(); flushBytes(); out.flush();`. After `close()` it throws `IOException("Stream closed")`.
- `close()`:

      if (closed) return;
      try {
          if (haveLeftover) { haveLeftover = false; put(-1); }
          flushBytes();
          try { out.flush(); } finally { out.close(); }
      } finally {
          closed = true;
      }

  So a pending high surrogate becomes a replacement at close (`write("x\ud83d"); close();` -> `78 3f` in UTF-8,
  `fe ff fffd` for a UTF-16 writer that wrote only `"\ud800"`), the stream sees `[n] F C` (`F C` with nothing
  pending), and a second `close()` does nothing. Exceptions:
  - `out.flush()` throws `IOException("f")` and `out.close()` throws `IOException("c")`: `close()` throws `c`
    (the `finally` replaces `f`; no `addSuppressed`).
  - `out.write` throws `IOException("w")` while closing: `close()` throws it and neither `out.flush()` nor
    `out.close()` is called.
  - A `RuntimeException` from `out.flush()` (the test uses `IllegalStateException("rf")`) propagates and
    `out.close()` still runs.
  - Only `out.close()` throws (`IOException("only")`) and nothing is buffered: `close()` throws `only`, a later
    `flush()` throws `Stream closed`, and the stream log is `F C`.
  - In every case the writer is closed afterwards: `getEncoding()` is `null`, writes and `flush()` throw
    `IOException("Stream closed")` and `close()` returns at once.
- After `close()`: `write(int)`, `write(char[])`, `write(char[], int, int)` (even with a bad range or a `null`
  array: `ensureOpen` comes first), `write(String)`, `write("")`, `append` and `flush` throw
  `IOException("Stream closed")`; `write(String, int, int)` first checks `len < 0` and calls `getChars` (see
  "Writes").

### Failures

- `out.write` throws during `flush()`: the exception propagates, `out.flush()` is not called, and the bytes are
  dropped (`count` is already 0). A second `flush()` only calls `out.flush()`, and `close()` then gives `F C` without
  another write. The test does not write more characters after such a failure (see the known difference below).
- `out.flush()` throws during `flush()`: it propagates; the bytes were already written.

### PrintWriter over an OutputStream

`new PrintWriter(out)` and `new PrintWriter(out, autoFlush)` encode with UTF-8 through an `OutputStreamWriter`, so
nothing reaches `out` before a flush: `print("h\u00e9"); println(42);` leaves a `ByteArrayOutputStream` empty, and
`flush()` writes `68 c3a9 34 32 0a`. With `autoFlush` true, `print("a")` sends nothing and the following `println("b")` gives `[3] F` (`61 62 0a`, the
buffered `a` plus `b` and the newline). `printf("%d", 5)` gives `[1] F`. `print`, `write` and `append` send nothing,
and a later `println()` sends everything buffered (`[3] F`, `63 64 0a`). `close()` gives `F C` plus any pending
bytes. A `null` stream throws `NullPointerException`. An `IOException` from the stream only sets the error flag
(`checkError()` is `true`).

`libraryWriters` depends only on the unchanged T27 `PrintWriter`: its `println` newline goes to `out.write` directly,
so a subclass that overrides `write(String, int, int)` to flush does not flush the newline (for an ISO-8859-1 writer,
`print("\u00e9!")` gives `[2] F`, `println("\u20ac")` gives `[1] F` and `close()` gives `[1] F C`). Do not change
`PrintWriter` to "fix" this.

### Known differences from JDK 21 (not observed by the test; write them in the docs)

- The JDK `PrintWriter(OutputStream...)` puts a `BufferedWriter` (8192 chars) between the `PrintWriter` and the
  `OutputStreamWriter`. Here there is none: the protected `out` field of such a `PrintWriter` is the
  `OutputStreamWriter`, and output larger than the 512-byte starting buffer may reach the stream in different
  pieces (the bytes are the same).
- After `out.write` throws, the JDK `ByteBuffer` stays flipped and later writes fill only the old length before the
  next write-out; here the buffer is simply empty. Only `flush()` and `close()` after such a failure are matched.

## Steps

1. Read the test `javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeOutputStreamWriterTest.java`.
2. Read `javaw-bytecode/src/main/resources/exception-runtime/java/io/Writer.java` (the superclass; do not edit it),
   `java/io/PrintWriter.java` and `javaw/runtime/TextCharsets.java` (only `lookup` and `identifier` are needed).
3. Create `javaw-bytecode/src/main/resources/exception-runtime/java/io/OutputStreamWriter.java` with the members
   above.
4. Register it: `javaw-agent-kit/bin/kit register java/io/OutputStreamWriter.java`. It adds a
   `paths.add("java/io/OutputStreamWriter.java");` line; do not edit `BytecodeExceptionRuntime.java` by hand.
   `PrintWriter.java` is already registered.
5. Add the two constructors to `java/io/PrintWriter.java`.
6. `javaw-agent-kit/bin/kit check T34` until PASS.
7. Write `docs/bytecode-output-stream-writer.md` (below), the report, then `javaw-agent-kit/bin/kit finish T34`.

## Hints

- The whole class is about 130 lines (the reference has 136, about 40 of them for `put`). `IOException`, `UnsupportedEncodingException`, `OutputStream` and `Writer` are
  in `java.io` (no import). `Character.isHighSurrogate`, `isLowSurrogate`, `toCodePoint`, `highSurrogate`,
  `lowSurrogate`, `Math.min`, `System.arraycopy` and `String.getChars` exist in the runtime.
- Use `javaw.runtime.TextCharsets.lookup(charsetName)` (it throws the right `UnsupportedEncodingException`) and
  `javaw.runtime.TextCharsets.identifier(cs)`. Do not call `Charset.forName` and catch its exceptions, and do not use
  `String.getBytes(Charset)` or `TextCharsets.encode`: they encode a whole string at once and cannot keep a high
  surrogate pending between writes, and the UTF-16 forms would write a byte order mark every time.
- Write `put` with `if`/`else` on `charset`, not a `switch` on anything enum-like. For UTF-16 and a code point
  `c >= 0x10000`, write the two units `Character.highSurrogate(c)` and `Character.lowSurrogate(c)`, each high byte
  first (UTF-16BE, UTF-16) or low byte first (UTF-16LE). Every byte stored in `unit` needs a `(byte)` cast.
- `capacity - count < n` is the overflow test; `bytes` is always 8192 long, `capacity` says how much of it counts.
  Do not flush when `count == capacity` after a put; only the **next** put that does not fit flushes (`8192 a`
  followed by nothing stays in the buffer).
- `grow` runs on every non-empty write, after the pending surrogate is finished, with the number of chars still left.
  It flushes what is buffered only when it really raises `capacity`.
- `writeChars` must check `ensureOpen()` before the range check and the range check before `len == 0`.
- Do not write the words `synchronized`, `volatile`, `native` or `Thread` (capital T) anywhere in the file, not even
  in comments. Do not use `assert` statements (StreamEncoder has several; leave them out) or `System.out`/`System.err`.
  This is the simplest way to stay clear of `kit guard`.
- The Wasm runner stops at the **first** failing fixture of each `@Test` method, so after a fix a different fixture
  of the same group may fail next. That is progress, not a regression.
- Before the class exists, every `matchesJvm` test fails with a message naming `java/io/OutputStreamWriter` (or the
  `PrintWriter(OutputStream)` constructor): all tests share one fixture.
- Calibration mutations of the reference, each caught by the fixtures named (the first one fails first):
  `count = 0` after `out.write` instead of before (`flushFailures`, `printWriterErrors`: the second `flush` writes
  `[3]` again); no `grow` (`bufferFills`); flushing as soon as the buffer is full instead of when the next code point
  does not fit (`bufferFills`); `append(CharSequence)` through `write(String)` (`subclassCalls`: extra
  `S(ij)s(ij,0,2)`); encoding a pending high surrogate at `flush()` (`splitPairs`); writing the byte order mark before
  the `len == 0` return (`utf16Encoding`, `bufferGrowth`).
- Sizes of the calibration reference: 136 lines for OutputStreamWriter (about 40 of them for `put`), 2 new lines in
  PrintWriter.
- Calibration: the reference passed kit check after 1 run (about 138 new lines); `kit guard` was clean.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first. Failure lines name the `@Test` method and the
fixture method; look up the fixture name in the list below.

```kit-hints
constructorsAndEncodingNames, charsetEncoding, surrogatesAcrossWrites, bufferingAndFlush, writeFormsAndCalls, closingAndFailures, printWriterOverOutputStream: if the message names `java/io/OutputStreamWriter` or `java/io/PrintWriter.<init>` (`link a complete acyclic type hierarchy`, `link the descriptor class/interface`, `link a matching ... method`), the class is missing, not registered, or lacks that member: run `javaw-agent-kit/bin/kit register java/io/OutputStreamWriter.java` once and add the member exactly as listed (append returns Writer; PrintWriter gets PrintWriter(OutputStream) and PrintWriter(OutputStream, boolean)); if it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
encodingNames: getEncoding() returns HISTORICAL[charset] = UTF8, ISO8859_1, ASCII, UnicodeBigUnmarked, UnicodeLittleUnmarked, UTF-16 (not Charset.name()), and null once closed; the String constructor uses javaw.runtime.TextCharsets.lookup(charsetName), the Charset one TextCharsets.identifier(cs), and OutputStreamWriter(out) is this(out, Charset.defaultCharset()).
constructorFailures: every constructor starts with super(out) (NullPointerException with no message for a null stream, before any other check); then `throw new NullPointerException("charsetName")` / `throw new NullPointerException("charset")` for a null name or charset; an unknown or illegal name throws UnsupportedEncodingException whose message is the name (TextCharsets.lookup does this).
typesAndLock: OutputStreamWriter extends Writer and super(out) makes the protected lock field the stream.
utf8Encoding, singleByteEncoding, utf16Encoding: put(c) encodes one code point or -1 (malformed): UTF-8 1-4 bytes and `?` for -1; ISO-8859-1/US-ASCII one byte, `?` above 0xFF/0x7F or for -1 (a surrogate pair is one `?`); UTF-16 2 or 4 bytes, 0xFFFD for -1, little-endian only for UTF-16LE; UTF-16 writes fe ff once, before the first char of the first non-empty write (not in the constructor, not for write("")).
splitPairs, unpairedAcrossWrites, pendingHighSurrogate: a high surrogate that is the last char of a write is kept (haveLeftover/leftover) and combined with a low surrogate at the start of the next non-empty write; followed by anything else it becomes put(-1) and the char is encoded normally; flush() never encodes the pending surrogate, close() turns it into put(-1).
flushPattern, bufferFills, bufferGrowth: bytes reach out only as one out.write(bytes, 0, count) from flushBytes(), called by flush(), close(), put (when `capacity - count < n`, before adding the code point) and grow (only when it raises capacity: `n = Math.min(len * MAX_BYTES[charset], 8192)` with MAX_BYTES = {3, 1, 1, 2, 2, 4}, capacity starts at 512); flush() then calls out.flush() once.
writeForms, charRanges, stringRanges: write(int) is writeChars(new char[]{(char) c}, 0, 1); writeChars checks ensureOpen, then `(off < 0) || (off > cbuf.length) || (len < 0) || ((off + len) > cbuf.length) || ((off + len) < 0)` -> `new IndexOutOfBoundsException()` with no message, then returns for len == 0; writeString checks `len < 0` first, then `str.getChars(off, off + len, cbuf, 0)`.
subclassCalls: write(int), write(char[], int, int), write(String, int, int) and append(CharSequence) must not call other overridable methods (append uses the private writeString, never write(String)); do not override write(char[]), write(String), append(CharSequence, int, int) or append(char); close() does not call flush() on this.
closeSequence, closeFailures: close() is `if (closed) return; try { if (haveLeftover) { haveLeftover = false; put(-1); } flushBytes(); try { out.flush(); } finally { out.close(); } } finally { closed = true; }`; afterwards getEncoding() is null and write/append/flush throw new IOException("Stream closed") from ensureOpen.
flushFailures: flushBytes() sets count = 0 BEFORE calling out.write, so after out.write threw, the next flush() only calls out.flush() and close() writes nothing more.
printWriterOverStream, printWriterAutoFlush, printWriterErrors, libraryWriters: PrintWriter(OutputStream out) is `this(out, false)` and PrintWriter(OutputStream out, boolean autoFlush) is `this(new OutputStreamWriter(out), autoFlush)`; nothing reaches the stream before flush, a println with autoflush, or close.
leavesOutCharsetPrintWriterConstructor: do not add PrintWriter(OutputStream, boolean, Charset); only the two OutputStream constructors from the task.
```

## Docs

Create `docs/bytecode-output-stream-writer.md` with this skeleton (fill in the brackets):

    # Bytecode OutputStreamWriter

    Revision `output-stream-writer-vN` links `java.io.OutputStreamWriter` and the `PrintWriter(OutputStream)` and
    `PrintWriter(OutputStream, boolean)` constructors in the bundled exception runtime. The encoder is written inside
    `OutputStreamWriter` (no `CharsetEncoder`/`ByteBuffer`) and takes no lock (single-threaded contract).

    Contract:

    - <constructors (null checks and their order, UnsupportedEncodingException, the default UTF-8), getEncoding
      historical names and null after close, the six charsets and their replacements (`?`, U+FFFD), the UTF-16 byte
      order mark, a high surrogate kept between writes, the write forms and range checks, which overridable methods
      are called, the 512-to-8192-byte buffer with its growth and overflow rule, flush, close and the closed state,
      failures of the stream>
    - <PrintWriter(OutputStream[, boolean]) wraps an OutputStreamWriter (UTF-8)>
    - <known differences: no BufferedWriter under PrintWriter(OutputStream) (the out field and the chunking of large
      output differ); after a failed out.write the buffer is empty instead of the JDK's flipped ByteBuffer>

    Evidence: `BytecodeOutputStreamWriterTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
    orders, two fresh instances), including a subclass and output streams that record every call.
    `PrintWriter(OutputStream, boolean, Charset)` is rejected before WAT generation.
