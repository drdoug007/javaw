# T35 InputStreamReader

```kit
id: T35
slug: input-stream-reader
title: link java.io.InputStreamReader with streaming UTF-8, ISO-8859-1 and US-ASCII decoding
tests: BytecodeInputStreamReaderTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/io/InputStreamReader.java
allow: docs/bytecode-input-stream-reader.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-input-stream-reader-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeInputStreamReaderTest.java`.

## Goal

Add `java.io.InputStreamReader` to the bundled runtime, on top of the existing `java.io.Reader` and
`java.io.InputStream`. It turns a byte stream into chars, decoding UTF-8, ISO-8859-1 or US-ASCII **while the bytes
arrive**: a multi-byte sequence may be split over several `read` calls of the underlying stream. In the pinned
libraries `java/io/InputStreamReader` is named by 60 classes (`runtime-target/target/audit/external-types.tsv`, 20 of
them in the static closure) and gets 112 member calls (`member-references.tsv`): `<init>(InputStream, Charset)` 37
(10 sites), `<init>(InputStream)` 32 (10 sites), `<init>(InputStream, String)` 22 (15 sites), `close()` 15,
`read(char[])` 2, `read(char[],int,int)` 2 and `getEncoding()` 1, plus 1 call of the left-out
`<init>(InputStream, CharsetDecoder)`. Users: Jackson
`ByteSourceJsonBootstrapper.constructReader` (String name), H2 `FileFunction.getValue` and `Bnf.getInstance`,
logback `FileUtil.resourceAsString` (`read(char[],int,int)`, `close`), ANTLR `ANTLRInputStream.<init>` and
`UnbufferedCharStream.<init>`, Tomcat `DefaultServlet.copy`, `WebappServiceLoader.parseConfigFile` and the EL
`SimpleCharStream`, Hibernate `ScriptSourceInputFromFile.toReader`, Spring, SnakeYAML, JAXB and AspectJ.

The JDK class is a thin wrapper around `sun.nio.cs.StreamDecoder`, which owns an 8192-byte buffer and a
`CharsetDecoder`. Neither exists here (the runtime has no `CharsetDecoder`, `ByteBuffer` or `CharBuffer`), so the
whole decoder goes **inside** `InputStreamReader.java`: the byte buffer, the read loop and three small decode loops.
Copy the JDK structure described below **exactly**: the test records every call the underlying stream receives
(`read(byte[], off, len)` with the buffer length, offset and length, and every `available()` call), so the buffer
size, the compaction and the "read again only if `available() > 0`" rule are all observable.

## Members to implement

One new file `javaw-bytecode/src/main/resources/exception-runtime/java/io/InputStreamReader.java`
(`package java.io;`, `import java.nio.charset.Charset;`):
`public class InputStreamReader extends Reader` with

- `public InputStreamReader(InputStream in)` (UTF-8, the default charset)
- `public InputStreamReader(InputStream in, String charsetName) throws UnsupportedEncodingException`
- `public InputStreamReader(InputStream in, Charset cs)`
- `public String getEncoding()`
- `public int read() throws IOException`
- `public int read(char[] cbuf, int off, int len) throws IOException`
- `public boolean ready() throws IOException`
- `public void close() throws IOException`

Private state (names are suggestions; everything except the public members above must be `private`):

- `private final InputStream in;` and `private final int charset;` (0 = UTF-8, 1 = ISO-8859-1, 2 = US-ASCII)
- `private boolean closed;`
- `private boolean haveLeftoverChar;` and `private char leftoverChar;`
- `private final byte[] bytes = new byte[8192];` with `private int bytePos, byteLimit;` (the unread bytes are
  `bytes[bytePos .. byteLimit-1]`; both start at 0, so the buffer starts empty)
- `private int charPos;` (the next free slot while decoding into the caller's array)
- private helpers, for example `ensureOpen()`, `read0()`, `readChars(char[], int, int)`, `implRead(char[], int, int)`,
  `readBytes()`, `inReady()`, `implReady()`, `decode(char[], int, boolean)` and `decodeLoop(char[], int)`

`read(char[])`, `skip`, `transferTo`, `markSupported`, `mark` and `reset` are inherited from the runtime `Reader`
unchanged; do not override them. Do not edit `Reader.java`, `InputStream.java` or any other existing file.

## Members to leave out

- `InputStreamReader(InputStream, CharsetDecoder)`: there is no `java.nio.charset.CharsetDecoder` in the runtime.
- `read(CharBuffer)`: there is no `java.nio.CharBuffer`. The test checks with `rejectsUnusedCall` that
  `InputStreamReader.read(Ljava/nio/CharBuffer;)I` stays unlinked, so do not add it, and do not write `CharBuffer`
  anywhere in InputStreamReader.java (not even in comments or imports). The docs file must still name
  `read(CharBuffer)` as a known difference, as the skeleton below shows.
- UTF-16 decoding (known difference). The runtime knows the names `UTF-16`, `UTF-16BE` and `UTF-16LE`, but this task
  decodes only UTF-8, ISO-8859-1 and US-ASCII: when the charset id is 3, 4 or 5 both charset constructors throw
  `new UnsupportedOperationException("InputStreamReader decodes only UTF-8, ISO-8859-1 and US-ASCII")` (the JDK would
  accept them). The test does not use UTF-16, nor any charset the runtime does not have (`windows-1252`, `UTF-32`).

## JDK behaviour to match

Charset selection (use the existing runtime helpers in
`javaw-bytecode/src/main/resources/exception-runtime/javaw/runtime/TextCharsets.java`; call them with the full name
`javaw.runtime.TextCharsets`):

- Every constructor first calls `super(in)` (the runtime `Reader(Object lock)`), which throws
  `new NullPointerException()` (message `null`) for a `null` stream. This happens **before** the charset checks, so
  `new InputStreamReader(null, (String) null)` and `new InputStreamReader(null, (Charset) null)` also throw an NPE with
  message `null`.
- `InputStreamReader(in)`: UTF-8 (`charset = 0`).
- `InputStreamReader(in, String charsetName)`: `if (charsetName == null) throw new NullPointerException("charsetName");`
  then `charset = javaw.runtime.TextCharsets.lookup(charsetName);`. `lookup` matches names and aliases ignoring case
  and throws `UnsupportedEncodingException` whose message is the name for unknown and for illegal names (JDK:
  `"no-such-charset"` -> message `no-such-charset`, `""` -> empty message, `"bad name!"` -> `bad name!`). Names the
  test uses: `UTF-8`, `utf8`, `ISO-8859-1`, `latin1`, `8859_1`, `US-ASCII`, `ascii`, `ISO646-US`.
- `InputStreamReader(in, Charset cs)`: `if (cs == null) throw new NullPointerException("charset");` then
  `charset = javaw.runtime.TextCharsets.identifier(cs);`.
- `getEncoding()`: `null` once the reader is closed (also after a `close()` whose `in.close()` threw); otherwise the
  JDK's historical name: `"UTF8"` (UTF-8), `"ISO8859_1"` (ISO-8859-1), `"ASCII"` (US-ASCII).

`read(char[] cbuf, int off, int len)` (public; JDK `StreamDecoder.lockedRead`):

1. `ensureOpen()`: `if (closed) throw new IOException("Stream closed");` (before the argument checks:
   `read(new char[4], 9, 9)` on a closed reader is this IOException).
2. `if (off < 0 || off > cbuf.length || len < 0 || off + len > cbuf.length || off + len < 0) throw new
   IndexOutOfBoundsException();` (message `null`; `(1, Integer.MAX_VALUE)` overflows and is caught by
   `off + len < 0`; `(4, 0)` on length 4 is fine, `(5, 0)` is not). A `null` array throws `NullPointerException` here
   (the test checks only the type).
3. `if (len == 0) return 0;` (no call reaches the stream).
4. `int n = 0;` If `haveLeftoverChar`: `cbuf[off] = leftoverChar; off++; len--; haveLeftoverChar = false; n = 1;` and
   `if (len == 0 || !implReady()) return n;`.
5. `if (len == 1)`: `int c = read0(); if (c == -1) return n == 0 ? -1 : n; cbuf[off] = (char) c; return n + 1;`.
6. `int nr = implRead(cbuf, off, off + len); return nr < 0 ? (n == 1 ? 1 : nr) : n + nr;`.

Put steps 1-6 in a private method (`readChars`) and let the public `read(char[],int,int)` just return it:
`read0()` calls the private method, never the overridable public one (a subclass that overrides
`read(char[],int,int)` does **not** see a call when `read()` is used; it does see the calls from the inherited
`Reader.read(char[])` and `Reader.skip`).

`read()` is `read0()`: `if (haveLeftoverChar) { haveLeftoverChar = false; return leftoverChar; }` then
`char[] cb = new char[2]; int n = readChars(cb, 0, 2);` and `-1` -> `-1`; `2` -> store `cb[1]` as the leftover char
and return `cb[0]`; `1` -> return `cb[0]`. The leftover check comes **before** `ensureOpen()`, so `read()` after
`close()` still returns a pending leftover char (JDK: after `read()` of `"ab"` returned `'a'` and `close()`, `read()`
returns `98`; after `read()` returned the high surrogate `d83d` and `close()`, `read()` returns `de00`), and only
the next one throws `IOException: Stream closed`.

`ready()`: `ensureOpen(); return haveLeftoverChar || implReady();` where `implReady()` is
`bytePos < byteLimit || inReady()` and `inReady()` is `try { return in.available() > 0; } catch (IOException x)
{ return false; }` (an `available()` that throws makes `ready()` return `false`, not throw).

`close()`: `if (closed) return; try { in.close(); } finally { closed = true; }`. The exception from `in.close()`
propagates, but the reader is closed anyway and a second `close()` does nothing (no second `in.close()` call).

`readBytes()` (JDK `StreamDecoder.readBytes`, one call of the underlying stream):

1. Compact: move the unread bytes to the front: `int rem = byteLimit - bytePos; System.arraycopy(bytes, bytePos,
   bytes, 0, rem); bytePos = 0; byteLimit = rem;` (always, also when `rem == 0`).
2. `int n = in.read(bytes, byteLimit, bytes.length - byteLimit);`. So the stream always sees the 8192-byte array,
   offset = number of unread bytes, length = the rest: `r8192,0,8192`, then `r8192,1,8191` when one byte of a
   sequence was left over. If `in.read` throws, the unread bytes stay (the next call is again
   `read(bytes, rem, 8192 - rem)`).
3. `if (n < 0) return n;` `if (n == 0) throw new IOException("Underlying input stream returned zero bytes");`
4. `byteLimit += n; return byteLimit - bytePos;`

`implRead(char[] cbuf, int off, int end)` (JDK `StreamDecoder.implRead`; the caller guarantees `end - off >= 2`):

    charPos = off;
    boolean eof = false;
    for (;;) {
        if (!decode(cbuf, end, eof)) break;              // overflow: cbuf is full
        if (eof) break;
        if (charPos >= end) break;
        if (charPos > off && !inReady()) break;          // block at most once
        int n = readBytes();
        if (n < 0) {
            eof = true;
            if (charPos == off && bytePos >= byteLimit) break;
        }
    }
    return charPos == off ? -1 : charPos - off;

`inReady()` (that is `in.available()`) is called only when at least one char was produced and `cbuf` still has
room. A stream whose `available()` returns 0 is read **once** per `read(char[],int,int)` after the first char;
while no char has been produced yet the loop keeps reading (a 3-byte sequence from a 1-byte-per-call stream takes
three calls `r8192,0,8192=1 r8192,1,8191=1 r8192,2,8190=1`). At end of input the loop runs `decode` once more with
`eof == true`, so trailing incomplete bytes become one `\ufffd`. `eof` is a local: every later `read` calls the
stream again (`r8192,0,8192=-1` each time).

`decode(char[] cbuf, int end, boolean eof)` (JDK `CharsetDecoder.decode` in REPLACE mode) returns `true` for
underflow (all complete input consumed) and `false` for overflow (no room in `cbuf`):

    for (;;) {
        int r = decodeLoop(cbuf, end);                   // -1 overflow, 0 underflow, k > 0 malformed length
        if (r < 0) return false;
        if (r == 0) {
            if (!eof || bytePos >= byteLimit) return true;
            r = byteLimit - bytePos;                     // incomplete tail at end of input: ONE replacement
        }
        if (charPos >= end) return false;                // no room for the replacement char
        cbuf[charPos++] = (char) 0xfffd;
        bytePos += r;
    }

`decodeLoop(char[] cbuf, int end)` decodes from `bytes[bytePos..byteLimit-1]` into `cbuf[charPos..end-1]`,
advancing `bytePos` and `charPos` only over complete chars. With `b1 = bytes[bytePos] & 0xff`, `rem = byteLimit -
bytePos`, `cont(x)` = `(x & 0xc0) == 0x80` and `bK` the unsigned K-th byte, it loops while `rem > 0` and returns 0
when the bytes are used up:

- ISO-8859-1: `if (charPos >= end) return -1;` then `cbuf[charPos++] = (char) b1; bytePos++;`.
- US-ASCII: `b1 < 0x80` as ISO-8859-1; otherwise `return 1` (one `\ufffd` per byte, whether or not there is room;
  `decode` checks the room).
- UTF-8 (JDK `sun.nio.cs.UTF_8.Decoder`), in this order for each lead byte:
  - `b1 < 0x80`: `if (charPos >= end) return -1;` one char.
  - `0xc2 <= b1 <= 0xdf`: `if (rem < 2) return 0; if (charPos >= end) return -1; if (!cont(b2)) return 1;` then
    `cbuf[charPos++] = (char) (((b1 & 0x1f) << 6) | (b2 & 0x3f)); bytePos += 2;`.
  - `0xe0 <= b1 <= 0xef`: `if (rem > 1 && bad3(b1, b2)) return 1; if (rem < 3) return 0; if (charPos >= end)
    return -1; if (!cont(b3)) return 2;` then
    `char c = (char) (((b1 & 0x0f) << 12) | ((b2 & 0x3f) << 6) | (b3 & 0x3f)); if (Character.isSurrogate(c)) return 3;
    cbuf[charPos++] = c; bytePos += 3;` (the `(char)` casts are required: the expressions are `int`, and
    `Character.isSurrogate` takes a `char`). An encoded surrogate `ed a0 80` is **one** `\ufffd` for all three bytes. `bad3(b1, b2)` is `(b1 == 0xe0 && (b2 & 0xe0) == 0x80) || !cont(b2)`.
  - `0xf0 <= b1 <= 0xf7`: `if (b1 > 0xf4 || rem > 1 && bad4(b1, b2)) return 1; if (rem > 2 && !cont(b3)) return 2;
    if (rem < 4) return 0; if (end - charPos < 2) return -1; if (!cont(b4)) return 3;` then
    `int cp = ((b1 & 0x07) << 18) | ((b2 & 0x3f) << 12) | ((b3 & 0x3f) << 6) | (b4 & 0x3f);` two chars
    `Character.highSurrogate(cp)`, `Character.lowSurrogate(cp)`, `bytePos += 4`. `bad4(b1, b2)` is
    `b1 > 0xf4 || (b1 == 0xf0 && (b2 < 0x90 || b2 > 0xbf)) || (b1 == 0xf4 && (b2 & 0xf0) != 0x80) || !cont(b2)`.
    A supplementary char needs **two** free slots: with one slot left the loop returns -1 and the 4 bytes stay
    for the next read.
  - any other `b1` (`0x80..0xc1`, `0xf8..0xff`): `return 1`.

Results checked by the test (JDK 21, bytes in hex, output as char codes; `fffd` is `\ufffd`):
`80` -> `fffd`; `c0 80` -> `fffd fffd`; `c2` at end -> `fffd`; `c2 41` -> `fffd 41`; `c3 c3 a9` -> `fffd e9`;
`e0 80 80` -> 3 x `fffd`; `e0 a0` at end -> `fffd`; `e2 82 41` -> `fffd 41`; `e2 41 42` -> `fffd 41 42`;
`ed a0 80` -> `fffd`; `ed 9f bf` -> `d7ff`; `ef bf bf` -> `ffff`; `f0 9f 98` at end -> `fffd`;
`f0 9f 98 41` -> `fffd 41`; `f0 9f 41 42` -> `fffd 41 42`; `f0 41 42 43` -> `fffd 41 42 43`;
`f0 80 80 80` and `f0 8f bf bf` and `f4 90 80 80` and `f5 80 80 80` -> 4 x `fffd`; `f4 8f bf bf` -> `dbff dfff`;
`f7 bf` -> `fffd fffd`; `f8 88 80 80 80` -> 5 x `fffd`; `f0 9f 98 80 80` -> `d83d de00 fffd`;
`41 e2 82 ac e2` -> `41 20ac fffd`; `bf 41` -> `fffd 41`; `c1 bf` -> `fffd fffd`; `e0 9f bf` -> 3 x `fffd`;
`e0 a0 41` -> `fffd 41`; `e2 82` at end -> `fffd`; `ed bf bf` -> `fffd`; `f0 9f` at end -> `fffd`;
`f0` at end -> `fffd`; `fe` -> `fffd`; `ff 41` -> `fffd 41`. The chars are the same whether the bytes come in one
call or one byte per call. How they are grouped into `read` results follows from the algorithm: `decode` returns
`false` (overflow) once `cbuf` is full, and the rest comes in the next `read`. With `read(new char[2], 0, 2)`:
`e0 80 80` gives `2:fffd.fffd 1:fffd`, `f0 41 42 43` gives `2:fffd.41 2:42.43`, `41 e2 82 ac e2` gives
`2:41.20ac 1:fffd`.
US-ASCII: every byte `>= 0x80` is one `fffd`, also when it is the last byte at end of input (for US-ASCII
`decodeLoop` never returns 0 while bytes are left, so the end-of-input tail rule never fires; do not special-case
it). ISO-8859-1: every byte is the char with the same value.

Call patterns checked by the test (`rL,off,len=n` is one `read(byte[] of length L, off, len)` that returned n,
`aN` an `available()` that returned N):

- `"A\u00e9\u20ac\ud83d\ude00z"` (bytes `41 c3 a9 e2 82 ac f0 9f 98 80 7a`) from a 1-byte-per-call stream whose
  `available()` returns 0, read with `read(new char[8], 0, 8)`: `1:41 <r8192,0,8192=1 a0>`, then
  `1:e9 <r8192,0,8192=1 r8192,1,8191=1 a0>`, then `1:20ac` (three calls), `2:d83d.de00` (four calls), `1:7a`, and
  finally `-1 <r8192,0,8192=-1>`.
- The same bytes with `available()` returning the bytes left and 2 bytes per call: one `read` returns all 6 chars:
  `r8192,0,8192=2 a9 r8192,1,8191=2 a7 r8192,1,8191=2 a5 r8192,0,8192=2 a3 r8192,2,8190=2 a1 r8192,0,8192=1 a0`.
- `41 42 e2 82` read with `read(new char[2], 0, 2)`: `2:41.42` (the two bytes `e2 82` stay buffered and
  `ready()` is `true`), then `1:fffd` after `r8192,2,8190=-1`, then `-1` after `r8192,0,8192=-1`, again and again.
- `largeInput`: 20000 bytes (`41 c3 a9` repeated, so they end with `41 c3`), `available()` returning the bytes left,
  one `read(new char[30000], 0, 30000)`: it returns 13333 chars after `r8192,0,8192=8192 r8192,1,8191=8191
  r8192,0,8192=3617` (the first chunk ends with an incomplete `c3`, kept at offset 0; the second chunk ends on a
  complete char). The last byte `c3` (index 19999) stays buffered, so the next `read` calls `r8192,1,8191=-1` and
  returns 1 (one `fffd`).
- A stream that returns 0 bytes: `read()` throws `IOException: Underlying input stream returned zero bytes`; the
  next `read()` calls the stream again.
- A stream that throws `IOException("boom 2")` on its second call after handing out `e2`: the `read` throws it,
  `ready()` is then `true` (one byte buffered), and the next `read` continues with `r8192,1,8191`.

## Steps

1. Read `javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeInputStreamReaderTest.java`, then
   `javaw-bytecode/src/main/resources/exception-runtime/java/io/Reader.java`, `InputStream.java`,
   `StringReader.java` and `javaw-bytecode/src/main/resources/exception-runtime/javaw/runtime/TextCharsets.java`
   (only `lookup` and `identifier` are needed).
2. Create `javaw-bytecode/src/main/resources/exception-runtime/java/io/InputStreamReader.java` with the members above.
3. Register it (do not edit `BytecodeExceptionRuntime.java` by hand):
   `javaw-agent-kit/bin/kit register java/io/InputStreamReader.java`
4. `javaw-agent-kit/bin/kit check T35` until PASS.
5. Write `docs/bytecode-input-stream-reader.md` (below), the report, then `javaw-agent-kit/bin/kit finish T35`.

## Hints

- The pseudo-code above is the whole algorithm. Write it as given; do not invent a different buffering scheme
  (for example "read everything, then decode" or `new String(bytes, charset)`): the stream call log differs at once.
- Work on unsigned bytes: `int b1 = bytes[bytePos] & 0xff;` and the same for `b2`, `b3`, `b4`
  (`bytes[bytePos + 1] & 0xff`, ...). Read `b2` only when `rem > 1`, `b3` only when `rem > 2`, `b4` only when
  `rem > 3`, or you index past the buffered bytes.
- `Character.isSurrogate`, `Character.highSurrogate` and `Character.lowSurrogate` exist in the runtime.
- `javaw.runtime.TextCharsets.lookup(String)` throws `java.io.UnsupportedEncodingException` already carrying the
  name; do not wrap it. `identifier(Charset)` returns 0..5 for the StandardCharsets constants and throws
  `UnsupportedOperationException("Custom charset conversion is unsupported")` for any other Charset; leave that
  exception alone. After `lookup` or `identifier`, throw the UTF-16 UnsupportedOperationException for ids 3..5, for
  example through a private static helper `checked(int id)`, so the final `charset` field is assigned once.
- No `synchronized` (the JDK locks on `lock`), no `volatile` (`closed` is volatile in the JDK), no `assert`. Do not
  write those words anywhere, not even in comments.
- `read(char[],int,int)` must not compute anything before `ensureOpen()`, and the range check uses the JDK's five
  conditions with **no** message (`new IndexOutOfBoundsException()`), not `Reader.checkFromIndexSize`.
- The Wasm runner stops at the **first** failing fixture of each `@Test` method, so after a fix another fixture of
  the same group may fail next. That is progress.
- Write the replacement char as `(char) 0xfffd` in the source. Some editors turn a written backslash-u escape
  into the raw character; runtime sources must stay plain ASCII.
- In decodeLoop test `charset == 1` (every byte is one char: `if (charPos >= end) return -1; cbuf[charPos++] =
  (char) b1; bytePos++;`) and `charset == 2` (`b1 >= 0x80` is `return 1`) before any UTF-8 branch, for example
  `if (b1 < 0x80 || charset == 1) {...} else if (charset == 2) { return 1; } else if (b1 >= 0xc2 && b1 <= 0xdf)
  {...}`. You may read `b2` and `b3` at the top of the loop only through guards:
  `int b2 = rem > 1 ? bytes[bytePos + 1] & 0xff : 0; int b3 = rem > 2 ? bytes[bytePos + 2] & 0xff : 0;`. Read `b4`
  only inside the 4-byte branch, after `if (rem < 4) return 0;`.
- Calibration mutations, each caught by the fixture named: `read(cb, 0, 2)` instead of the private `readChars` in
  `read0()` (`readerSubclass`), one replacement per trailing byte instead of one for the whole tail
  (`truncatedAtEof`, `utf8Malformed`), `ensureOpen()` at the top of `read0()` (`closeBehaviour`,
  `leftoverAfterClose`), asking `available()` before checking `charPos > off` (`splitChunk1`, `availableThrows`).
- Calibration: the reference passed kit check after 1 run (about 215 new lines, one file).

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
constructorsAndEncodings, closeAndArgumentChecks, sequencesSplitAcrossReads, malformedInput, supplementaryCharacters, streamFailuresAndSubclasses: if FIX THE RUNTIME says `link a complete acyclic type hierarchy: java/io/InputStreamReader` or `link the descriptor class/interface java/io/InputStreamReader`, the class is missing or not registered: run `javaw-agent-kit/bin/kit register java/io/InputStreamReader.java` (never edit BytecodeExceptionRuntime.java by hand). If it names a member (`link a matching instance method ...`), add it exactly as listed under Members to implement. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
encodingNames: getEncoding returns the historical names "UTF8", "ISO8859_1" and "ASCII" (not "UTF-8"), and null after close().
constructorFailures: call super(in) first (null stream -> NullPointerException with message null), then `throw new NullPointerException("charsetName")` / `new NullPointerException("charset")`; unknown or illegal names throw the UnsupportedEncodingException from javaw.runtime.TextCharsets.lookup(charsetName) unchanged.
closeBehaviour, closeFailure, leftoverAfterClose: close() is `if (closed) return; try { in.close(); } finally { closed = true; }`; read(char[],int,int) and ready() start with ensureOpen() (`IOException("Stream closed")`) before any argument check, but read() returns a pending leftover char before it checks anything.
indexChecks: the check is `off < 0 || off > cbuf.length || len < 0 || off + len > cbuf.length || off + len < 0` -> `new IndexOutOfBoundsException()` (no message), then `if (len == 0) return 0;` before touching the stream; len == 1 goes through read0(), which reads two chars and keeps the second as leftover.
splitChunk1, splitChunk2, splitChunk3, splitWithAvailable, largeInput, failingStream: the stream log is wrong: readBytes() must compact the unread bytes to the front and call `in.read(bytes, byteLimit, bytes.length - byteLimit)` on an 8192-byte array, and implRead must call `inReady()` (in.available() > 0) only when `charPos > off` and cbuf still has room; while no char was produced it reads again without asking available().
truncatedAtEof, utf8Malformed, utf8MalformedChunked: an incomplete sequence at end of input is ONE \ufffd for all its bytes: after readBytes() returned -1, implRead must still call decode(cbuf, end, true) when bytes are left, and decode then replaces `byteLimit - bytePos` bytes with one \ufffd.
utf8Malformed, utf8MalformedChunked, utf8MalformedSingles: the number of \ufffd per bad sequence comes from decodeLoop's return values: bad3 -> 1, bad third byte -> 2, encoded surrogate (ed a0 80) -> 3 (one \ufffd), bad4 or b1 > 0xf4 -> 1, bad third byte of four -> 2, bad fourth byte -> 3; lead bytes 80..c1 and f8..ff -> 1. Check the order of the tests in the task's UTF-8 list.
asciiAndLatin1Malformed, latin1AndAscii: US-ASCII turns each byte >= 0x80 into one \ufffd; ISO-8859-1 maps every byte to the char with the same value (`(char) (bytes[bytePos] & 0xff)`).
oneCharAtATime, leftoverChar, leftoverWithBufferedBytes, singleSlotReads: read() must be read0(): return the leftover char first, otherwise readChars(new char[2], 0, 2) and keep cb[1] as the leftover; in readChars a leftover char is copied first and the method returns at once unless len > 1 and implReady().
noRoomForPair, utf8MalformedSingles: a 4-byte sequence needs two free slots (`end - charPos < 2` -> return -1, overflow) and a replacement needs one (`charPos >= end` in decode -> return false); the bytes then stay for the next read.
zeroByteStream: readBytes() throws `new IOException("Underlying input stream returned zero bytes")` when in.read returns 0, after the compaction, so the next read calls the stream again.
availableThrows, readyStates: inReady() is `in.available() > 0` inside try/catch(IOException) returning false; ready() is `ensureOpen(); return haveLeftoverChar || bytePos < byteLimit || inReady();`.
readerSubclass: read() must not call the overridable read(char[],int,int); put the read logic in a private method that both read0() and the public read(char[],int,int) use. Reader.read(char[]) and Reader.skip are inherited unchanged.
leavesOutCharBufferRead: InputStreamReader must not declare read(java.nio.CharBuffer); remove it.
```

## Docs

Create `docs/bytecode-input-stream-reader.md` with this skeleton (fill in the brackets):

    # Bytecode InputStreamReader

    Revision `input-stream-reader-vN` links `java.io.InputStreamReader` in the bundled exception runtime.

    Contract:

    - <constructors and their null/unsupported-name failures, getEncoding historical names and null after close,
      the 8192-byte buffer with compaction and the at-most-once available() rule, UTF-8/ISO-8859-1/US-ASCII
      decoding with JDK replacement counts, the leftover char for surrogate pairs (also after close), ready, close>
    - Known differences: UTF-16 charsets throw UnsupportedOperationException; there is no
      InputStreamReader(InputStream, CharsetDecoder) and no read(CharBuffer).

    Evidence: `BytecodeInputStreamReaderTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders,
    two fresh instances). `read(CharBuffer)` is rejected before WAT generation.
