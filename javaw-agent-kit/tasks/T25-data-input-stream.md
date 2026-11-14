# T25 FilterInputStream, DataInput and DataInputStream

```kit
id: T25
slug: data-input-stream
title: link java.io.FilterInputStream, DataInput and DataInputStream
tests: BytecodeDataInputStreamTest
depends: T18, T24
allow: javaw-bytecode/src/main/resources/exception-runtime/java/io/FilterInputStream.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/io/DataInput.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/io/DataInputStream.java
allow: docs/bytecode-data-input-stream.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-data-input-stream-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeDataInputStreamTest.java`.

## Goal

Add `java.io.FilterInputStream`, the `java.io.DataInput` interface and `java.io.DataInputStream` to the bundled
runtime, on top of `InputStream`, `ByteArrayInputStream` and `EOFException` (T18) and `UTFDataFormatException` (T24).
The pinned libraries name `DataInputStream` from 147 classes (`runtime-target/target/audit/external-types.tsv`) and
make 334 member calls on it (`member-references.tsv`): `readUnsignedShort()` 192, `readUnsignedByte()` 26,
`<init>(InputStream)` 24, `readInt()` 20, `close()` 13, `readFully([BII)` 11, `readFully([B)` 10, `readByte()` 10,
`readShort()` 7, `readUTF()` 4, `readLong`/`readFloat`/`readDouble` 3 each, `readBoolean`/`read()` 2, `skipBytes`,
`readChar`, `read([B)` and the deprecated `readLine` once each. AspectJ's bcel (`ClassParser`, `Constant`,
`ConstantUtf8` with `readUTF`, `ByteSequence`), jandex (`Indexer`), byte-buddy's and AspectJ's ASM `Constants`, and
H2's `PgServerThread` and `Transfer` read class files and the PostgreSQL wire protocol with it. The `DataInput`
interface has 123 call sites from 21 classes: Jackson's `ByteSourceJsonBootstrapper` (`readUnsignedByte`, 93),
`UTF8DataInputJsonParser`, `JsonFactory`, `ObjectMapper`, and Tomcat's bcel (`readUnsignedShort` 20, `readInt`,
`readUTF`, `skipBytes`...). `FilterInputStream` is extended by Spring's `StreamUtils$NonClosingInputStream`,
Hibernate's `AbstractHANADialect$CloseSuppressingInputStream`, Tomcat's `LimitedInputStream`, H2's
`RangeInputStream`, `LobStorageMap$LobInputStream` and `TcpServerThread$CachedInputStream`, and Logback's
`NonClosableInputStream` (`<init>`, `read()`, `read([BII)`, `skip`, `close` via `super`).

## Members to implement

Three files under `javaw-bytecode/src/main/resources/exception-runtime/java/io/` (all `package java.io;`).

`FilterInputStream.java`: `public class FilterInputStream extends InputStream` with

- `protected InputStream in;` (subclasses read and assign it; the test does). **Not** `volatile`.
- `protected FilterInputStream(InputStream in)` (stores it; `null` is allowed)
- `public int read() throws IOException`
- `public int read(byte[] b) throws IOException`
- `public int read(byte[] b, int off, int len) throws IOException`
- `public long skip(long n) throws IOException`
- `public int available() throws IOException`
- `public void close() throws IOException`
- `public void mark(int readlimit)`
- `public void reset() throws IOException`
- `public boolean markSupported()`

Nothing else: `readAllBytes`, `readNBytes`, `skipNBytes` and `transferTo` stay inherited from `InputStream`.

`DataInput.java`: `public interface DataInput` with exactly these abstract methods, each `throws IOException`:
`void readFully(byte[] b)`, `void readFully(byte[] b, int off, int len)`, `int skipBytes(int n)`,
`boolean readBoolean()`, `byte readByte()`, `int readUnsignedByte()`, `short readShort()`,
`int readUnsignedShort()`, `char readChar()`, `int readInt()`, `long readLong()`, `float readFloat()`,
`double readDouble()`, `String readUTF()`. No default methods, and **no** `readLine()` (see below).

`DataInputStream.java`: `public class DataInputStream extends FilterInputStream implements DataInput` with

- `private final byte[] readBuffer = new byte[8];`
- `private byte[] bytearr = new byte[0];` and `private char[] chararr = new char[0];` (the `readUTF` buffers)
- `public DataInputStream(InputStream in)` (`super(in)`)
- `public final int read(byte[] b) throws IOException`
- `public final int read(byte[] b, int off, int len) throws IOException`
- `public final void readFully(byte[] b) throws IOException`
- `public final void readFully(byte[] b, int off, int len) throws IOException`
- `public final int skipBytes(int n) throws IOException`
- `public final boolean readBoolean()`, `byte readByte()`, `int readUnsignedByte()`, `short readShort()`,
  `int readUnsignedShort()`, `char readChar()`, `int readInt()`, `long readLong()`, `float readFloat()`,
  `double readDouble()`, `String readUTF()` (all `public final ... throws IOException`)
- `public static final String readUTF(DataInput in) throws IOException`

Do **not** override `read()`, `skip`, `available`, `close`, `mark`, `reset` or `markSupported` in
`DataInputStream`: the `FilterInputStream` versions are the JDK behaviour, and the test's `DataInputStream`
subclass overrides some of them.

## Members to leave out

- `DataInputStream.readLine()` and `DataInput.readLine()` (deprecated; the JDK version wraps `in` in a
  `java.io.PushbackInputStream`, which the runtime does not have). Do not declare `readLine` anywhere; the test's
  `leavesOutReadLine` checks that a call to `DataInputStream.readLine()` is rejected.
- Do not edit any other runtime file (`kit guard` rejects it). In particular do **not** add
  `Objects.checkFromIndexSize` to `java/util/Objects.java`, and do not change `InputStream.java`,
  `ByteArrayInputStream.java`, `EOFException.java` or `UTFDataFormatException.java`: they already exist.

## JDK behaviour to match

FilterInputStream (every method is overridable; the test logs which ones reach `in`):

- `read()`: `return in.read();`
- `read(byte[] b)`: `return read(b, 0, b.length);` (the overridable 3-argument method of **`this`**, so a
  subclass that overrides `read(byte[],int,int)` sees `read(b)` as `read(b, 0, b.length)`).
- `read(byte[] b, int off, int len)`: `return in.read(b, off, len);` with **no** range check of its own.
- `skip(n)`: `return in.skip(n);` `available()`: `return in.available();` `close()`: `in.close();`
  `mark(readlimit)`: `in.mark(readlimit);` `reset()`: `in.reset();` `markSupported()`: `return in.markSupported();`
- With `in == null` every one of these throws `NullPointerException` (just call `in.x(...)`; the message is not
  checked). `read(null)` also throws `NullPointerException` (from `b.length`).
- The inherited `InputStream` defaults go through `this.read(byte[],int,int)`, so on a `FilterInputStream` over a
  stream that hands out 3 bytes per call, `readNBytes(2)` then `readAllBytes()` make the calls
  `2@0/2 0@2/2 16384@0/16384 16381@3/16384 16379@5/16384` (`len@off/b.length`). That already works once
  `read(byte[],int,int)` delegates. Likewise `skip(5)` on a `FilterInputStream` over a 3-byte source that does not
  override `skip` returns 3 and logs `s5 5@0/5 2@0/5`: the reads come from the T18 `InputStream.skip` default of
  the source, not from `FilterInputStream`.

DataInputStream:

- `read(byte[] b)`: `return in.read(b, 0, b.length);` and `read(b, off, len)`: `return in.read(b, off, len);`.
  Both go **straight to `in`**, not through `this`, and do no range check (with a `ByteArrayInputStream` underneath,
  `read(new byte[8], 5, 9)` throws that stream's `Range [5, 5 + 9) out of bounds for length 8`; an `in` that does
  not check receives `9@1/6` unchanged).
- `readFully(b)`: `readFully(b, 0, b.length)`.
- `readFully(b, off, len)`: first the range check: `if (off < 0 || len < 0 || len > b.length - off)` throw
  `new IndexOutOfBoundsException("Range [" + off + ", " + off + " + " + len + ") out of bounds for length " + b.length)`.
  The package-private `static void checkFromIndexSize(int off, int len, int length)` that T18 put in
  `InputStream` does exactly this; call it (`checkFromIndexSize(off, len, b.length);`, it is inherited). Examples on
  a length-8 array: `(-1, 2)` -> `Range [-1, -1 + 2) out of bounds for length 8`, `(3, 6)` ->
  `Range [3, 3 + 6) out of bounds for length 8`, `(0, -1)` -> `Range [0, 0 + -1) out of bounds for length 8`,
  `(9, 0)` -> `Range [9, 9 + 0) out of bounds for length 8`, `(8, 0)` is fine. Nothing is read when it throws.
  `readFully(null)` and `readFully(null, 0, 0)` throw `NullPointerException` (from `b.length`) before any read. Then:

      int n = 0;
      while (n < len) {
          int count = in.read(b, off + n, len - n);
          if (count < 0) throw new EOFException();
          n += count;
      }

  So a short read is retried with the rest, `len == 0` makes no call at all (even with `in == null`), and at end
  of stream `new EOFException()` (message `null`) is thrown with the bytes read so far already stored in `b` and
  consumed from `in`. On a source of 11 bytes that hands out 3 per call, `readFully(new byte[8])` calls
  `8@0/8 5@3/8 2@6/8`.
- `skipBytes(n)`: `int total = 0, cur; while (total < n && (cur = (int) in.skip(n - total)) > 0) total += cur;
  return total;`. It calls `in.skip`, never `this.skip`. `n <= 0` returns `0` without calling `skip`; a `skip`
  result of `0` or less stops the loop; the `long` result is cast to `int` (a `skip` that returns `0x100000002L`
  counts as 2, and `skipBytes(5)` then returns 6). With a `ByteArrayInputStream` underneath,
  `skipBytes(Integer.MAX_VALUE)` on 3 bytes returns 3. When the source does not override `skip`, its `skip` is the
  T18 `InputStream.skip` default, which reads through `read(byte[],int,int)` into a `min(2048, remaining)`-sized
  buffer: on 6 bytes handed out 4 per call, `skipBytes(5)` returns 5 and logs `s5 5@0/5 1@0/5`. Those `len@off`
  entries after `sN` come from `InputStream.skip`; do not override `skip` in `DataInputStream` and do not
  reimplement it.
- `readUnsignedByte()`: `int ch = in.read(); if (ch < 0) throw new EOFException(); return ch;` (calls `in.read()`,
  never `this.read()`). `readBoolean()`: `readUnsignedByte() != 0` (2 is `true`). `readByte()`:
  `(byte) readUnsignedByte()`.
- `readShort`, `readUnsignedShort`, `readChar` (2 bytes), `readInt`, `readFloat` (4) and `readLong`, `readDouble`
  (8): `readFully(readBuffer, 0, n)` (so the source sees the **8-byte** `readBuffer`: `2@0/8`, `4@0/8 1@3/8`...),
  then combine big-endian, most significant byte first, masking each byte with `& 0xff`:
  - `readShort`: `(short) (((readBuffer[0] & 0xff) << 8) | (readBuffer[1] & 0xff))` (`80 01` -> -32767);
    `readUnsignedShort`: the same without the cast (`ff ff` -> 65535); `readChar`: cast to `char` (`ff fe` -> 65534).
  - `readInt`: four bytes `<< 24`, `<< 16`, `<< 8`, `<< 0` (`ff ff ff fe` -> -2).
  - `readLong`: build it from two `readInt`-style halves with `((long) hi << 32) + (lo & 0xFFFFFFFFL)`, or from
    eight `(long) (readBuffer[i] & 0xff) << (56 - 8 * i)`. `80 00 .. 00` -> `Long.MIN_VALUE`.
  - `readFloat`: `Float.intBitsToFloat(int)` of the four bytes; `readDouble`: `Double.longBitsToDouble(long)`.
    Do not touch NaN payloads: return `Float.intBitsToFloat(bits)` / `Double.longBitsToDouble(bits)` unchanged. The
    test prints `7fc00000` / `7ff8000000000000` for the input bits `0x7fc00001` / `0x7ff0000000000001` because the
    fixture's own `Float.floatToIntBits` / `Double.doubleToLongBits` canonicalize NaN, not because of `readFloat` or
    `readDouble`.
  At end of stream all of them throw `new EOFException()` from `readFully`/`readUnsignedByte`, after consuming
  what was there: `readInt()` on 3 bytes throws and afterwards `read()` is `-1` and `available()` is `0`.
- `readUTF()`: `return readUTF(this);`
- `static readUTF(DataInput in)` (modified UTF-8, the format `DataOutputStream.writeUTF` writes):
  1. `int utflen = in.readUnsignedShort();` (an interface call; for a `DataInputStream` it is the method above).
  2. Buffers. If `in instanceof DataInputStream` (use a plain cast, `DataInputStream dis = (DataInputStream) in;`):
     `if (dis.bytearr.length < utflen) { dis.bytearr = new byte[utflen * 2]; dis.chararr = new char[utflen * 2]; }`
     and use `dis.bytearr` / `dis.chararr`. Otherwise `new byte[utflen]` and `new char[utflen]`.
  3. `in.readFully(bytearr, 0, utflen);` (one interface call, even for `utflen == 0`; with a `DataInputStream` that
     call makes no `read` for 0 bytes). The source therefore sees the buffer length: reading `"hello"`, `"hi"`,
     `""`, `"0123456789AB"`, `"abc"` from a source that hands out 2 bytes per call logs
     `2@0/8 5@0/10 3@2/10 1@4/10 | 2@0/8 2@0/10 | 2@0/8 | 2@0/8 12@0/24 10@2/24 8@4/24 6@6/24 4@8/24 2@10/24 | 2@0/8 3@0/24 1@2/24`.
     A `DataInput` that is not a `DataInputStream` sees `readUnsignedShort` then `readFully(byte[utflen], 0, utflen)`.
  4. Decode `count` from 0 while `count < utflen`, with `c = bytearr[count] & 0xff` and the JDK's checks in this
     order (`char2`/`char3` are the **signed** bytes; only `& 0xC0` is tested):
     - `c >> 4` in 0..7: one byte, `(char) c`, `count++`.
     - `c >> 4` is 12 or 13: `count += 2`; if `count > utflen` throw
       `new UTFDataFormatException("malformed input: partial character at end")`; `char2 = bytearr[count - 1]`; if
       `(char2 & 0xC0) != 0x80` throw `new UTFDataFormatException("malformed input around byte " + count)`; char
       `((c & 0x1F) << 6) | (char2 & 0x3F)`.
     - `c >> 4` is 14: `count += 3`; if `count > utflen` throw the "partial character at end" exception;
       `char2 = bytearr[count - 2]`, `char3 = bytearr[count - 1]`; if either `& 0xC0` is not `0x80` throw
       `new UTFDataFormatException("malformed input around byte " + (count - 1))`; char
       `((c & 0x0F) << 12) | ((char2 & 0x3F) << 6) | (char3 & 0x3F)`.
     - anything else (`c >> 4` is 8, 9, 10, 11 or 15): throw
       `new UTFDataFormatException("malformed input around byte " + count)` (`count` not advanced).
  5. `return new String(chararr, 0, chararr_count);`
  Examples (bytes after the 2 length bytes): `c3` -> `malformed input: partial character at end`; `c3 41` ->
  `malformed input around byte 2`; `e0 80 41` and `e0 41 80` -> `around byte 2`; `80` -> `around byte 0`;
  `41 f0` -> `around byte 1`; `41 42 e0` -> partial; `41 42 43 c3` -> partial; `41 bf` -> `around byte 1`;
  `41 e0 80 ff` -> `around byte 3`; `ff 41 42` -> `around byte 0`. Non-shortest and lone-surrogate forms are
  accepted: `c0 80` and `e0 80 80` give `"\0"`, `c1 bf` gives `"\u007f"`, `ed a0 80` gives `"\ud800"`. The `utflen`
  bytes are always consumed first: after a malformed string the next `read()` returns the byte after it. A length
  that runs past the end throws `EOFException` (message `null`) from `readFully` (or from `readUnsignedShort` when
  fewer than 2 bytes are left). `readUTF(null)` throws `NullPointerException`.
- `DataInputStream` is a `DataInput`, `FilterInputStream`, `InputStream` and `Closeable`; a plain
  `FilterInputStream` is not a `DataInput`.

## Steps

1. Read `javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeDataInputStreamTest.java`, then
   `InputStream.java`, `FilterOutputStream.java` and `DataOutputStream.java` in
   `javaw-bytecode/src/main/resources/exception-runtime/java/io/` (the output side is the mirror image).
2. Create `FilterInputStream.java`, `DataInput.java` and `DataInputStream.java` in
   `javaw-bytecode/src/main/resources/exception-runtime/java/io/`.
3. Register all three (do not edit `BytecodeExceptionRuntime.java` by hand; the order does not matter):
   - `javaw-agent-kit/bin/kit register java/io/FilterInputStream.java`
   - `javaw-agent-kit/bin/kit register java/io/DataInput.java`
   - `javaw-agent-kit/bin/kit register java/io/DataInputStream.java`
4. `javaw-agent-kit/bin/kit check T25` until PASS.
5. Write `docs/bytecode-data-input-stream.md` (below), the report, then `javaw-agent-kit/bin/kit finish T25`.

## Hints

- `FilterInputStream` is about 20 lines: one-line delegations. Its constructor is `protected`, so the test makes
  instances with `new FilterInputStream(in){}` (an anonymous subclass).
- `DataInput` is a plain interface of 14 methods.
- In `DataInputStream`, write the decoding of `readUTF` with `if`/`else` on `c >> 4` (for example
  `int k = c >> 4; if (k <= 7) {...} else if (k == 12 || k == 13) {...} else if (k == 14) {...} else throw ...`).
  Do not use `instanceof` with a pattern variable; use `if (in instanceof DataInputStream) { DataInputStream dis =
  (DataInputStream) in; ... }`. The private fields `bytearr`/`chararr` of another `DataInputStream` are accessible
  from the static method (same class).
- `Float.intBitsToFloat`, `Double.longBitsToDouble` and `new String(char[], int, int)` exist in the runtime.
  `Objects.checkFromIndexSize` does **not**; use the inherited `checkFromIndexSize(off, len, b.length)` from
  `InputStream` (package-private, same package).
- Mask every byte with `& 0xff` before combining: bytes are signed, so `(readBuffer[0] << 8) | readBuffer[1]` gives
  -1 for `12 ff` instead of 4863.
- `readFully`, `readUnsignedByte` and `skipBytes` must call `in.read(...)` / `in.skip(...)`, not `read(...)` /
  `skip(...)` on `this`. The test's `Spy` subclass overrides `read()` and `skip(long)` and logs `R`/`S`: those
  letters must appear only for the calls the fixture makes itself.
- `DataInputStream.read(byte[])` must be `in.read(b, 0, b.length)`, not `read(b, 0, b.length)` (that would still
  give the same bytes here, but it is the JDK's code; both methods are `final`).
- No `synchronized`, no `volatile` (the JDK's `in` field is volatile), no `native`, no `Thread`; do not write those
  words anywhere, not even in comments.
- The Wasm runner stops at the **first** failing fixture of each `@Test` method, so after a fix another fixture of
  the same group may fail next. That is progress.
- Before any runtime file exists, every `matchesJvm` test fails with
  `link a complete acyclic type hierarchy: java/io/FilterInputStream` or a similar message naming
  `java/io/DataInputStream`/`java/io/DataInput`: that only means the classes are missing or unregistered.
- Calibration mutations, each caught by the fixture named: `read()` instead of `in.read()` in `readUnsignedByte`
  (`subclassOverrides`), `in.read(b, 0, b.length)` in `FilterInputStream.read(byte[])` (`filterSubclasses`), a new
  `readUTF` buffer whenever `bytearr.length != utflen` (`readUtfBuffers`), `count` instead of `count - 1` in the
  3-byte message (`readUtfMalformed`), checking the continuation byte before the partial check
  (`readUtfMalformed`, `readUtfOtherDataInput`) and an unmasked low half in `readLong` (`wordPrimitives`,
  `floatingPrimitives`).
- Sizes of the calibration reference: 26 lines for FilterInputStream, 19 for DataInput, 115 for DataInputStream.
- Calibration: the reference passed kit check after 1 run (about 160 new lines).

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
filterInputStreamDelegation, readFullyAndSkipBytes, primitiveReads, readDelegationAndSubclasses, readUtf, readUtfBuffersAndDataInput, leavesOutReadLine: if FIX THE RUNTIME says `link a complete acyclic type hierarchy: java/io/...`, `link the descriptor class/interface java/io/...` or `unsupported ... java/io/...`, a class is missing or not registered: run `javaw-agent-kit/bin/kit register java/io/FilterInputStream.java`, `... java/io/DataInput.java` and `... java/io/DataInputStream.java` (never edit BytecodeExceptionRuntime.java by hand). If it names a member (`link a matching instance method ...`), add it exactly as listed under Members to implement. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes. leavesOutReadLine fails only if `readLine` was declared: remove it.
filterDelegation, filterNullIn: every FilterInputStream method is a one-line delegation to the `protected InputStream in` field (`in.read()`, `in.read(b, off, len)`, `in.skip(n)`, `in.available()`, `in.close()`, `in.mark(readlimit)`, `in.reset()`, `in.markSupported()`), except read(byte[]) which is `read(b, 0, b.length)` on this; no range check, no null check.
filterDefaultsGoThroughIn: FilterInputStream must NOT override readNBytes, readAllBytes, skipNBytes or transferTo; the InputStream defaults call this.read(byte[],int,int), which delegates to in.
filterSubclasses: `in` must be a `protected InputStream in` field (not private, not final) that every method reads on each call; read(byte[]) must call the overridable `read(b, 0, b.length)` of this (the subclass logs `F4@0`), not `in.read(b)`.
readFully, readFullyEof: readFully(b, off, len) loops `int count = in.read(b, off + n, len - n); if (count < 0) throw new EOFException(); n += count;` until n == len, so short reads are retried with the rest (`8@0/8 5@3/8 2@6/8`); len == 0 makes no call; readFully(b) is readFully(b, 0, b.length).
readFullyBounds: readFully checks `off < 0 || len < 0 || len > b.length - off` first and throws IndexOutOfBoundsException("Range [" + off + ", " + off + " + " + len + ") out of bounds for length " + b.length), via the inherited InputStream.checkFromIndexSize; a null array throws NullPointerException; nothing is read (log= stays empty).
skipBytes: skipBytes(n) is `int total = 0, cur; while (total < n && (cur = (int) in.skip(n - total)) > 0) total += cur; return total;` (calls in.skip, stops at a result <= 0, casts the long to int).
smallPrimitives, wordPrimitives: combine bytes big-endian and mask each one with `& 0xff` before shifting; readLong must mask the low half (`((long) hi << 32) + (lo & 0xFFFFFFFFL)`: `00 00 00 00 ff ff ff ff` is 4294967295, not -1); readShort casts to short, readChar to char, readUnsignedShort does not cast; readBoolean is `readUnsignedByte() != 0`, readByte is `(byte) readUnsignedByte()`.
floatingPrimitives: readFloat is Float.intBitsToFloat of the readInt bits and readDouble is Double.longBitsToDouble of the readLong bits (-0.0 keeps its sign bit, so 80000000 / 8000000000000000).
primitiveEof: every read throws `new EOFException()` (message null) at end of stream: readUnsignedByte when `in.read()` returns a negative value, the multi-byte reads from readFully; the bytes before the end are consumed. With a null `in`, readFully(new byte[0]) and skipBytes(0) return without touching in.
primitiveCalls: readByte/readUnsignedByte/readBoolean call `in.read()` once (log `r`); readShort/readUnsignedShort/readChar/readInt/readFloat/readLong/readDouble call readFully(readBuffer, 0, n) on the 8-byte `readBuffer` field (log `2@0/8`, `4@0/8 1@3/8`, `8@0/8 5@3/8 2@6/8`).
readDelegation: DataInputStream.read(b) is `in.read(b, 0, b.length)` and read(b, off, len) is `in.read(b, off, len)` with no range check (the arguments reach in unchanged, e.g. `9@1/6`); read(), skip, available, mark, reset, markSupported and close are inherited from FilterInputStream.
subclassOverrides: readUnsignedByte must call `in.read()` and skipBytes `in.skip(...)`, never this.read()/this.skip(), so the subclass logs `R S1 A` only for its own direct calls; DataInputStream must not override read(), skip, available, close, mark, reset or markSupported.
readUtfRoundTrip, readUtfNonShortest: readUTF decodes modified UTF-8: `c >> 4` 0..7 is one byte, 12/13 two bytes `((c & 0x1F) << 6) | (char2 & 0x3F)`, 14 three bytes `((c & 0x0F) << 12) | ((char2 & 0x3F) << 6) | (char3 & 0x3F)`; non-shortest forms (`c0 80`) and lone surrogates are accepted; the instance readUTF() is `readUTF(this)`.
readUtfMalformed: messages are `malformed input: partial character at end` when count (after += 2 or += 3) > utflen, `malformed input around byte " + count` for a bad 2-byte continuation (count already advanced) and for a lead byte with c >> 4 in 8..11 or 15 (count not advanced), and `"malformed input around byte " + (count - 1)` for a bad 3-byte continuation; check partial BEFORE the continuation bytes.
readUtfEof: readUTF reads utflen with readUnsignedShort and then readFully(bytearr, 0, utflen) BEFORE decoding, so a short stream throws EOFException (null message) and a malformed string still consumes all utflen bytes.
readUtfBuffers: for a DataInputStream, bytearr/chararr are fields starting as `new byte[0]`/`new char[0]`, replaced by `new byte[utflen * 2]`/`new char[utflen * 2]` only when `bytearr.length < utflen`; the bulk read is `readFully(bytearr, 0, utflen)` so the source sees the buffer length (`5@0/10`, later `12@0/24`).
readUtfOtherDataInput: static readUTF(DataInput in) calls `in.readUnsignedShort()` then `in.readFully(new byte[utflen], 0, utflen)` on any DataInput that is not a DataInputStream (log `ushort full[3,0,3]`, and `full[0,0,0]` for an empty string); readUTF(null) throws NullPointerException.
dataInputInterface, typesAndExceptions: DataInput declares exactly the 14 methods listed (no readLine), each `throws IOException`; DataInputStream extends FilterInputStream and implements DataInput, and its read methods are reachable through the interface.
```

## Docs

Create `docs/bytecode-data-input-stream.md` with this skeleton (fill in the brackets):

    # Bytecode DataInputStream

    Revision `data-input-stream-vN` links `java.io.FilterInputStream`, `java.io.DataInput` and
    `java.io.DataInputStream` in the bundled exception runtime.

    Contract:

    - <FilterInputStream one-line delegation to the protected `in` field (read(byte[]) through this), DataInputStream
      readFully loops and EOFException, skipBytes, big-endian primitives through the 8-byte buffer, modified UTF-8
      readUTF with its buffer growth and malformed-input messages, readUTF(DataInput) for other DataInput sources;
      single-threaded, no monitors>

    Evidence: `BytecodeDataInputStreamTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders,
    two fresh instances). `DataInputStream.readLine()` is rejected before WAT generation.
