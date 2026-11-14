# T24 FilterOutputStream, DataOutput and DataOutputStream

```kit
id: T24
slug: data-output-stream
title: link java.io.FilterOutputStream, DataOutput, DataOutputStream and UTFDataFormatException
tests: BytecodeDataOutputStreamTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/io/FilterOutputStream.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/io/DataOutput.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/io/DataOutputStream.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/io/UTFDataFormatException.java
allow: docs/bytecode-data-output-stream.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-data-output-stream-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeDataOutputStreamTest.java`.

## Goal

Add `java.io.FilterOutputStream`, the `java.io.DataOutput` interface, `java.io.DataOutputStream` and
`java.io.UTFDataFormatException` to the bundled runtime, on top of the existing `java.io.OutputStream`.
The pinned libraries name `DataOutputStream` 153 times as a type (151 of them in the static closure,
`runtime-target/target/audit/external-types.tsv`) and make 357 member calls on it (`member-references.tsv`):
`writeShort(int)` 137, `writeByte(int)` 92, `writeInt(int)` 36, `<init>(OutputStream)` 22, `writeUTF(String)` 15,
`writeLong(long)` / `write(byte[])` / `close()` 11 each, `write(byte[],int,int)` 8, `flush()` 5, `writeFloat` /
`writeDouble` 3 each, `writeChars`, `writeBoolean`, `write(int)` once. AspectJ's bcel writes class files with it
(`Attribute.dump`, `ConstantUtf8.dump` with `writeUTF`, `JavaClass.dump` with `close`), H2's
`PgServerThread.writeDataColumn` / `sendMessage` write the PostgreSQL wire protocol (`writeInt`, `writeLong`,
`writeFloat`, `writeDouble`, `flush`), and Spring's objenesis `ObjectInputStreamInstantiator$MockStream` builds a
serialization header. Jackson's `DataOutputAsStream`, `JsonFactory` and `ObjectMapper` use the `DataOutput`
interface. Spring's `StreamUtils$NonClosingOutputStream` and `FormHttpMessageConverter$MultipartOutputStream` extend
`FilterOutputStream` (`<init>(OutputStream)`).

## Members to implement

Four files under `javaw-bytecode/src/main/resources/exception-runtime/java/io/` (all `package java.io;`).

`UTFDataFormatException.java`: `public class UTFDataFormatException extends IOException` with
`public UTFDataFormatException()` and `public UTFDataFormatException(String s)` (`super(s)`).

`DataOutput.java`: `public interface DataOutput` with exactly these abstract methods, each `throws IOException`:
`void write(int b)`, `void write(byte[] b)`, `void write(byte[] b, int off, int len)`,
`void writeBoolean(boolean v)`, `void writeByte(int v)`, `void writeShort(int v)`, `void writeChar(int v)`,
`void writeInt(int v)`, `void writeLong(long v)`, `void writeFloat(float v)`, `void writeDouble(double v)`,
`void writeBytes(String s)`, `void writeChars(String s)`, `void writeUTF(String s)`. No default methods.

`FilterOutputStream.java`: `public class FilterOutputStream extends OutputStream` with

- `protected OutputStream out;` (subclasses read and assign it; the test does) and `private boolean closed;`
- `public FilterOutputStream(OutputStream out)` (stores it; `null` is allowed)
- `public void write(int b) throws IOException`
- `public void write(byte[] b) throws IOException`
- `public void write(byte[] b, int off, int len) throws IOException`
- `public void flush() throws IOException`
- `public void close() throws IOException`

`DataOutputStream.java`: `public class DataOutputStream extends FilterOutputStream implements DataOutput` with

- `protected int written;` (subclasses read and assign it; the test does)
- `private byte[] bytearr;` (the reusable `writeUTF` buffer, initially `null`) and
  `private final byte[] writeBuffer = new byte[8];`
- `public DataOutputStream(OutputStream out)` (`super(out)`)
- `private void incCount(int value)`
- `public void write(int b) throws IOException`, `public void write(byte[] b, int off, int len) throws IOException`,
  `public void flush() throws IOException`
- `public final void writeBoolean(boolean v)`, `writeByte(int v)`, `writeShort(int v)`, `writeChar(int v)`,
  `writeInt(int v)`, `writeLong(long v)`, `writeFloat(float v)`, `writeDouble(double v)`, `writeBytes(String s)`,
  `writeChars(String s)`, `writeUTF(String str)` (all `public final void ... throws IOException`)
- `public final int size()`
- `private static String tooLongMsg(String s, int bits32)`

Do **not** override `write(byte[])` or `close()` in `DataOutputStream`: the inherited `FilterOutputStream` versions
are the JDK behaviour. So `DataOutputStream.write(byte[] b)` is `FilterOutputStream.write(b, 0, b.length)`, which
calls the overridable 3-argument `write` of `this`: a subclass that overrides `write(byte[],int,int)` sees
`s.write(new byte[3])` as one call `(b, 0, 3)`, and if it does not call `super`, neither `size()` nor `out` changes.
Add no methods beyond the ones listed above.

## Members to leave out

Nothing else exists on these JDK 21 classes except the package-private `static int writeUTF(String, DataOutput)`
that the JDK shares with `ObjectOutputStream`; write the encoding inside `writeUTF(String)` instead (a private helper
is fine). Do not edit any other runtime file (`kit guard` rejects it). In particular do **not** add
`Objects.checkFromIndexSize` to `java/util/Objects.java`, and do not change `OutputStream.java` or
`ByteArrayOutputStream.java`.

## JDK behaviour to match

FilterOutputStream (every method is overridable, and the test records which ones get called):

- `write(int b)`: `out.write(b)` with the int unchanged (`write(300)` passes 300, `write(-1)` passes -1).
- `write(byte[] b)`: `write(b, 0, b.length)` (the overridable 3-argument method of `this`; `null` -> NPE).
- `write(byte[] b, int off, int len)`: first read `int length = b.length;` (a `null` array throws
  `NullPointerException` here, before any other check, so `write(null, -1, 0)` is also a `NullPointerException`).
  Then the range check of the JDK's `Objects.checkFromIndexSize(off, len, length)`:
  `if ((off | len) < 0 || len > length - off)` throw
  `new IndexOutOfBoundsException("Range [" + off + ", " + off + " + " + len + ") out of bounds for length " + length)`
  (`(-1,1)` on length 4 -> `Range [-1, -1 + 1) out of bounds for length 4`; `(1, Integer.MAX_VALUE)` ->
  `Range [1, 1 + 2147483647) out of bounds for length 4`; `(5,0)` -> `Range [5, 5 + 0) out of bounds for length 4`;
  `(4,0)` is fine). Then **one call per byte**: `for (int i = 0; i < len; i++) write(b[off + i]);` through `this`, so
  each byte arrives sign-extended (`-2`, not 254) at `write(int)`. It never calls `out.write(byte[],int,int)`: for
  `write(new byte[]{1,-2,127})` the sink sees `w1 w-2 w127`.
- `flush()`: `out.flush()`.
- `close()`: `if (closed) return; closed = true;` then call `flush()` (the overridable one) and afterwards
  `out.close()`, with JDK 21's exception rules:
  - flush ok, close ok: returns normally. The sink sees `flush close`.
  - flush throws F, `out.close()` ok: F is thrown (after `out.close()` ran).
  - flush ok, `out.close()` throws C: C is thrown.
  - flush throws F and `out.close()` throws C: C is thrown with F added by `C.addSuppressed(F)`, unless `C == F`
    (same object), which is thrown without suppression. This holds for any `Throwable`
    (`IllegalStateException` from flush, `IllegalArgumentException` from close).
  - A second `close()` does nothing at all (no flush, no `out.close()`), also after a close that threw.
  - With `out == null`: `close()` throws the `NullPointerException` from `out.close()` with the one from
    `out.flush()` suppressed (two different objects); a second close returns normally.
  - `write` and `flush` do **not** check `closed`: writes after `close()` still reach `out`.
  - The flag belongs to the filter, not to `out`: after `close()`, assigning a new `out` and closing again does
    nothing to the new stream.

DataOutputStream:

- `incCount(value)`: `int temp = written + value; if (temp < 0) temp = Integer.MAX_VALUE; written = temp;`. So the
  counter sticks at `Integer.MAX_VALUE` (from `MAX_VALUE - 3`, `writeInt` gives `MAX_VALUE`), and a negative
  `written` set by a subclass jumps to `MAX_VALUE` on the next write (`-5` then `writeByte` -> 2147483647).
  `size()` returns `written`.
- Every write calls `out` **first** and `incCount` **after** it returns, so when `out` throws, `written` does not change.
- `write(int)`, `write(byte[],int,int)`, `flush()`, `writeBoolean`, `writeByte`, `writeShort`, `writeChar`,
  `writeInt`, `writeLong`, `writeFloat`, `writeDouble`, `writeBytes` and `writeChars` call `out` directly
  (`out.write(...)`, `out.flush()`), never `this.write(...)`. Only `writeUTF` (and the inherited
  `FilterOutputStream.write(byte[])`, `write(byte[],int,int)` and `close()`) call overridable methods of `this`. For
  example a subclass that overrides `write(int)` sees `write(9)`, but not `writeByte(1)` or `writeBoolean(true)`.
- `write(int b)`: `out.write(b); incCount(1);`.
- `write(byte[] b, int off, int len)`: `out.write(b, off, len); incCount(len);` with **no** range check of its own:
  the arguments go straight to `out` (the same array object, offset and length). With a `ByteArrayOutputStream`
  sink the sink's own check throws (`Range [2, 2 + 2) out of bounds for length 3`). With a sink that accepts
  `len == -1`, `incCount(-1)` runs: from `written == 10` it gives 9, from `written == 0` it gives
  `Integer.MAX_VALUE` (-1 is negative).
- `flush()`: `out.flush()`.
- `writeBoolean(v)`: `out.write(v ? 1 : 0); incCount(1);`. `writeByte(v)`: `out.write(v); incCount(1);` (the int
  unchanged: `writeByte(300)` passes 300 to the sink, which stores the low byte).
- `writeShort(v)` and `writeChar(v)`: put the two low bytes big-endian into `writeBuffer[0..1]`, then
  `out.write(writeBuffer, 0, 2); incCount(2);`. `writeInt` (4 bytes), `writeLong` (8), `writeFloat` (4, the bits
  of `Float.floatToIntBits(v)`) and `writeDouble` (8, `Double.doubleToLongBits(v)`) work the same way:
  `out.write(writeBuffer, 0, n)`. The sink therefore sees an **8-byte array** each time (`a8,0,2:12.34.` for
  `writeShort(0x1234)`). Big-endian: most significant byte first: `writeShort(0x12345)` -> `23 45`,
  `writeChar(-1)` -> `ff ff`, `writeInt(0x01020304)` -> `01 02 03 04`, `writeLong(-2)` -> `ff ff ff ff ff ff ff fe`,
  `writeFloat(1.5f)` -> `3f c0 00 00`, `writeFloat(-0.0f)` -> `80 00 00 00`, any float NaN -> `7f c0 00 00`,
  `writeDouble(1.0/3)` -> `3f d5 55 55 55 55 55 55`, any double NaN -> `7f f8 00 00 00 00 00 00`.
- `writeBytes(s)`: `for (int i = 0; i < len; i++) out.write((byte) s.charAt(i));` then **one** `incCount(len)` at
  the end. The cast makes each value a sign-extended byte: `"A\u00e9\u0100\uffff"` sends `65 -23 0 -1`. If `out`
  throws part-way, the bytes before stay written and `written` is unchanged.
- `writeChars(s)`: for each char, the two bytes big-endian in `writeBuffer` and `out.write(writeBuffer, 0, 2)` (one
  call per char), then one `incCount(len * 2)`.
- `writeUTF(str)` (modified UTF-8):
  1. `utflen` = sum over the chars: 1 for `0x0001..0x007F`, 2 for `0x0000` and `0x0080..0x07FF`, 3 for
     `0x0800..0xFFFF` (each surrogate of a pair counts on its own: 3 + 3).
  2. `if (utflen > 65535 || utflen < strlen) throw new UTFDataFormatException(tooLongMsg(str, utflen));` before
     anything is written (`written` and the sink unchanged).
  3. Buffer: `if (bytearr == null || bytearr.length < utflen + 2) bytearr = new byte[utflen * 2 + 2];` and reuse it
     otherwise. `writeUTF("abc")` makes an 8-byte array; then `writeUTF("hello, world")` (needs 14) makes a 26-byte
     one, which `"hi"`, `""` and `"hello, world!"` (15) reuse.
  4. `bytearr[0..1]` = `utflen` big-endian, then the encoding: 1 byte `c`; 2 bytes `0xC0 | ((c >> 6) & 0x1F)`,
     `0x80 | (c & 0x3F)` (so `\0` is `c0 80`); 3 bytes `0xE0 | ((c >> 12) & 0x0F)`, `0x80 | ((c >> 6) & 0x3F)`,
     `0x80 | (c & 0x3F)`.
  5. **One** call `write(bytearr, 0, utflen + 2)` on `this` (the overridable `DataOutputStream.write(byte[],int,int)`,
     not `out.write`). That method does the `incCount`; `writeUTF` itself does **not** call `incCount`. So a subclass
     whose `write(byte[],int,int)` does not call `super` leaves `size()` at 0 after `writeUTF("abc")`. Any subclass
     that overrides `write(byte[],int,int)` sees the first `writeUTF("ab")` of a stream as one call `A6,0,4` (a new
     6-byte `bytearr`, offset 0, length 4).
  Examples: `"A\0"` -> `00 03 41 c0 80`; `"\u00e9\u07ff"` -> `00 04 c3 a9 df bf`; `"\u0800\uffff"` ->
  `00 06 e0 a0 80 ef bf bf`; `"\ud83d\ude00"` (one emoji, a surrogate pair) -> `00 06 ed a0 bd ed b8 80`; `""` -> `00 00`; 65535 `'a'` -> 65537
  bytes starting `ff ff 61 61`.
- `tooLongMsg(s, bits32)`: `long actualLength = (long) s.length() + Integer.toUnsignedLong(bits32 - s.length());`
  and `"encoded string (" + s.substring(0, 8) + "..." + s.substring(s.length() - 8) + ") too long: " + actualLength + " bytes"`.
  Examples: 65536 `'a'` -> `encoded string (aaaaaaaa...aaaaaaaa) too long: 65536 bytes`; `"HEAD1234"` + 40000
  `'\u20ac'` + `"5678TAIL"` -> `encoded string (HEAD1234...5678TAIL) too long: 120016 bytes`.
- `writeUTF(null)`, `writeBytes(null)`, `writeChars(null)` throw `NullPointerException` (from `s.length()`) and write
  nothing. The test checks only the type of every `NullPointerException`.
- `close()` is the inherited `FilterOutputStream.close()`: it calls `DataOutputStream.flush()` (`out.flush()`) and
  then `out.close()`. A `DataOutputStream` is a `DataOutput`, `FilterOutputStream`, `OutputStream`, `Closeable` and
  `Flushable`; a plain `FilterOutputStream` is not a `DataOutput`.

## Steps

1. Read `javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeDataOutputStreamTest.java`, then
   `javaw-bytecode/src/main/resources/exception-runtime/java/io/OutputStream.java` and `EOFException.java` in the
   same directory.
2. Create `UTFDataFormatException.java`, `DataOutput.java`, `FilterOutputStream.java` and `DataOutputStream.java` in
   `javaw-bytecode/src/main/resources/exception-runtime/java/io/`.
3. Register all four (do not edit `BytecodeExceptionRuntime.java` by hand; the order does not matter):
   - `javaw-agent-kit/bin/kit register java/io/UTFDataFormatException.java`
   - `javaw-agent-kit/bin/kit register java/io/DataOutput.java`
   - `javaw-agent-kit/bin/kit register java/io/FilterOutputStream.java`
   - `javaw-agent-kit/bin/kit register java/io/DataOutputStream.java`
4. `javaw-agent-kit/bin/kit check T24` until PASS.
5. Write `docs/bytecode-data-output-stream.md` (below), the report, then `javaw-agent-kit/bin/kit finish T24`.

## Hints

- Copy `EOFException.java` for `UTFDataFormatException`. `DataOutput` is a plain interface with exactly the 14
  abstract methods listed above and nothing else.
- Write the bytes of `writeShort`/`writeInt`/`writeLong` with shifts into `writeBuffer`, for example
  `writeBuffer[0] = (byte) (v >>> 24); writeBuffer[1] = (byte) (v >>> 16); writeBuffer[2] = (byte) (v >>> 8);
  writeBuffer[3] = (byte) v;`. `writeLong` fills all 8 bytes the same way:
  `writeBuffer[0] = (byte) (v >>> 56); writeBuffer[1] = (byte) (v >>> 48);` ...
  `writeBuffer[6] = (byte) (v >>> 8); writeBuffer[7] = (byte) v;`. Every element needs the `(byte)` cast, because
  `writeBuffer` is a `byte[]`.
  `writeFloat(v)` is `writeInt`'s body applied to `Float.floatToIntBits(v)` and `writeDouble(v)` is `writeLong`'s body
  applied to `Double.doubleToLongBits(v)`, but they must not call `writeInt`/`writeLong` and then `incCount` twice;
  a small private helper `void putInt(int v)` / `void putLong(long v)` that only fills `writeBuffer` works well.
  Do not use `floatToRawIntBits` (NaN payloads would leak through).
- No `synchronized` (JDK `write` methods are synchronized; RULES.md forbids it), no `volatile` (`closed` is volatile
  in the JDK), no `closeLock`, no `jdk.internal.util.ByteArray`. Do not write the words `synchronized`, `volatile`,
  `native` or `Thread` anywhere, not even in comments.
- `Objects.checkFromIndexSize` does **not** exist in the runtime: write the range check in
  `FilterOutputStream.write(byte[],int,int)` inline with the message above. Do not compute `off + len` (it overflows
  for `(1, Integer.MAX_VALUE)`).
- Do not call `super.write(b, off, len)` from `DataOutputStream.write(byte[],int,int)`: that is the per-byte
  `FilterOutputStream` loop. Call `out.write(b, off, len)`.
- `close()` in the JDK form, which javac accepts as is:

      if (closed) return;
      closed = true;
      Throwable flushException = null;
      try {
          flush();
      } catch (Throwable e) {
          flushException = e;
          throw e;
      } finally {
          if (flushException == null) {
              out.close();
          } else {
              try {
                  out.close();
              } catch (Throwable closeException) {
                  if (flushException != closeException) closeException.addSuppressed(flushException);
                  throw closeException;
              }
          }
      }

  `Throwable.addSuppressed` and `getSuppressed` exist in the runtime. javac accepts `throw e;` of a `Throwable`
  in a method declared `throws IOException` only because the try block calls nothing but `flush()` (precise
  rethrow): put no other code into that try block.
- `Float.floatToIntBits`, `Double.doubleToLongBits`, `Integer.toUnsignedLong` and `String.substring(int, int)` exist
  in the runtime.
- The Wasm runner stops at the **first** failing fixture of each `@Test` method, so after a fix another fixture of the
  same group may fail next. That is progress.
- Calibration mutations, each caught by the fixture named: `out.flush()` instead of `flush()` in `close()`
  (`closeSequence`, `spyCalls`, `closeFailuresThroughSubclasses`), `floatToRawIntBits`/`doubleToRawLongBits` (`floatingPrimitives`: Wasm keeps the NaN
  payload, so `7fc00001` leaks through), `super.write(b, off, len)` in `DataOutputStream.write(byte[],int,int)`
  (`smallPrimitives`, `primitiveCalls`, `sizeOverflow`, `utfBufferCalls`, `spyCalls`, `utfEncoding`, `utfLimits`,
  `negativeLengthPassesThrough`, `sizeAfterFailures`), and `out.write(bytearr, ...)` plus its own `incCount` in
  `writeUTF` (`spyCalls`: the subclass no longer sees `A6,0,4`).
- Sizes of the calibration reference: 4 lines for UTFDataFormatException, 19 for DataOutput, 44 for
  FilterOutputStream, 99 for DataOutputStream.
- Calibration: the reference passed kit check after 1 run (about 170 new lines).

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
filterOutputStreamWrites, filterOutputStreamClose, dataOutputStreamPrimitives, dataOutputStreamCallPattern, writeUtf, sizeCounter, dataOutputInterfaceAndException: if FIX THE RUNTIME says `link a complete acyclic type hierarchy: java/io/...` or `link the descriptor class/interface java/io/...`, a class is missing or not registered: run `javaw-agent-kit/bin/kit register java/io/UTFDataFormatException.java`, `... java/io/DataOutput.java`, `... java/io/FilterOutputStream.java` and `... java/io/DataOutputStream.java` (never edit BytecodeExceptionRuntime.java by hand). If it names a member (`link a matching instance method ...`), add it exactly as listed under Members to implement. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
filterWrites, filterSubclasses: FilterOutputStream.write(int) is `out.write(b);`, write(byte[]) is `write(b, 0, b.length);`, and write(byte[],int,int) checks the range and then loops `for (int i = 0; i < len; i++) write(b[off + i]);` on `this` (bytes arrive sign-extended, e.g. `w-2`); it must never call `out.write(b, off, len)`.
filterRangeChecks: first `int length = b.length;` (a null array fails here with NullPointerException), then `if ((off | len) < 0 || len > length - off) throw new IndexOutOfBoundsException("Range [" + off + ", " + off + " + " + len + ") out of bounds for length " + length);` before any write.
filterOutFieldAndNull: `out` must be a `protected OutputStream out` field (not private, not final) that every method reads each time; close() with a null `out` throws the NullPointerException of out.close() with the flush NullPointerException suppressed, and the second close() returns without doing anything.
closeSequence, closeFailures, closeFailuresThroughSubclasses: close() is `if (closed) return; closed = true;` then the try/catch/finally from the Hints: call the overridable `flush()` (not `out.flush()`), always call `out.close()` afterwards, throw the close exception with the flush exception added via addSuppressed unless they are the same object, and never flush or close twice.
smallPrimitives, wordPrimitives, primitiveCalls: writeShort/writeChar/writeInt/writeLong/writeFloat/writeDouble fill `writeBuffer` (a `new byte[8]` field) big-endian, most significant byte first, and make one call `out.write(writeBuffer, 0, n)`; writeBoolean is `out.write(v ? 1 : 0)` and writeByte/write(int) are `out.write(v)` with the int unchanged; each then calls incCount(n) once.
floatingPrimitives: writeFloat must use `Float.floatToIntBits(v)` and writeDouble `Double.doubleToLongBits(v)` (NaN becomes 7fc00000 / 7ff8000000000000), never the Raw versions.
sinkRangeFailures, negativeLengthPassesThrough, sizeOverflow: DataOutputStream.write(byte[],int,int) is exactly `out.write(b, off, len); incCount(len);` with no range check of its own (the sink throws, or accepts len == -1 and incCount(-1) runs).
smallPrimitives, primitiveCalls: if size() is too high (`9:` instead of `5:` after write(0x180), write(new byte[]{1,2}), write(b,1,2)) or the sink sees `w5 w6 w7` instead of one `a3,0,3:5.6.7.` call, DataOutputStream.write(byte[],int,int) calls `super.write(b, off, len)` (the per-byte FilterOutputStream loop, which also counts every byte through write(int)); replace it with `out.write(b, off, len); incCount(len);`.
stringCalls: writeBytes sends `out.write((byte) s.charAt(i))` per char (sign-extended: '\u00e9' sends -23) and writeChars sends `out.write(writeBuffer, 0, 2)` per char; each calls incCount once at the end (len and len * 2).
utfBufferCalls, spyCalls, spySwallowsBulk: writeUTF ends with ONE call `write(bytearr, 0, utflen + 2)` on `this` (DataOutputStream.write(byte[],int,int), which does the incCount), never `out.write` and never its own incCount; bytearr is a field, replaced by `new byte[utflen * 2 + 2]` only when it is null or shorter than utflen + 2 (`abc` gives an 8-byte array, `hello, world` a 26-byte one that later strings reuse). If spyCalls lacks only the second `F` after close, FilterOutputStream.close() calls `out.flush()` instead of the overridable `flush()`.
utfEncoding, utfLimits: modified UTF-8: chars 0x0001-0x007F take 1 byte, 0x0000 and 0x0080-0x07FF take 2 (`\0` is c0 80), 0x0800-0xFFFF take 3, each surrogate on its own; the first two bytes are utflen big-endian; 65535 bytes is still allowed.
utfTooLong: when `utflen > 65535 || utflen < strlen` throw `new UTFDataFormatException(tooLongMsg(str, utflen))` before writing anything; the message is `"encoded string (" + s.substring(0, 8) + "..." + s.substring(s.length() - 8) + ") too long: " + ((long) s.length() + Integer.toUnsignedLong(bits32 - s.length())) + " bytes"`.
stringNulls: writeUTF, writeBytes and writeChars start with `s.length()`, so null throws NullPointerException before anything is written or counted.
sizeOverflow, sizeAfterFailures: `written` is a `protected int written` field; incCount is `int temp = written + value; if (temp < 0) temp = Integer.MAX_VALUE; written = temp;` and runs only AFTER the out call returned (a throwing sink leaves size() unchanged; writeBytes counts once at the end); size() returns written.
dataOutputInterface, typesAndException: DataOutput declares the 14 methods listed in the task, each `throws IOException`, with no defaults; DataOutputStream implements DataOutput and extends FilterOutputStream; UTFDataFormatException extends IOException with a no-argument and a String constructor.
```

## Docs

Create `docs/bytecode-data-output-stream.md` with this skeleton (fill in the brackets):

    # Bytecode DataOutputStream

    Revision `data-output-stream-vN` links `java.io.FilterOutputStream`, `java.io.DataOutput`,
    `java.io.DataOutputStream` and `java.io.UTFDataFormatException` in the bundled exception runtime.

    Contract:

    - <FilterOutputStream delegation (per-byte array writes, range check message), close with the closed flag and
      suppression rules, DataOutputStream big-endian primitives through the 8-byte buffer, writeBytes/writeChars,
      modified UTF-8 writeUTF with its buffer reuse and too-long message, the written counter and its clamp>

    Evidence: `BytecodeDataOutputStreamTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders,
    two fresh instances).
