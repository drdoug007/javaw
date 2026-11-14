# T19 ByteArrayOutputStream

```kit
id: T19
slug: byte-array-output-stream
title: link java.io.ByteArrayOutputStream
tests: BytecodeByteArrayOutputStreamTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/io/ByteArrayOutputStream.java
allow: docs/bytecode-byte-array-output-stream.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-byte-array-output-stream-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeByteArrayOutputStreamTest.java`.

## Goal

Add `java.io.ByteArrayOutputStream`, a growable in-memory byte sink built on the existing runtime
`java.io.OutputStream`. The pinned libraries name it 91 times as a type (`runtime-target/target/audit/external-types.tsv`)
and call it constantly (`member-references.tsv`): `<init>()` and `write(int)` 79 times each, `toByteArray()` 75,
`<init>(int)` 27, `write(byte[],int,int)` 26, `close()` 16, `size()` 12, `reset()` 7, `writeTo(OutputStream)` 5,
`toString()` 5, `toString(Charset)` / `toString(String)` 4 each. H2 uses it throughout (`Tokenizer.readBinaryString`,
`StreamStore.put` with `size`/`writeTo`/`write(byte[])`, `PgServerThread.startMessage` with `reset`,
`Utils10.byteArrayOutputStreamToString` with `toString(Charset)`), as do logback, byte-buddy, log4j and Tomcat.

## Members to implement

One file, `java/io/ByteArrayOutputStream.java` (`package java.io;`):
`public class ByteArrayOutputStream extends OutputStream`.

- `protected byte[] buf;` and `protected int count;` (subclasses read and assign them directly; the test does)
- `public ByteArrayOutputStream()` (initial capacity 32)
- `public ByteArrayOutputStream(int size)`
- `public void write(int b)`
- `public void write(byte[] b, int off, int len)`
- `public void writeBytes(byte[] b)`
- `public void writeTo(OutputStream out) throws IOException`
- `public void reset()`
- `public byte[] toByteArray()`
- `public int size()`
- `public String toString()`
- `public String toString(String charsetName) throws UnsupportedEncodingException`
- `public String toString(java.nio.charset.Charset charset)`
- `@Deprecated public String toString(int hibyte)`
- `public void close() throws IOException` (does nothing)
- one `private void ensureCapacity(int minCapacity)` helper

Do **not** override `write(byte[])` or `flush()`: the inherited `OutputStream` versions are correct
(`write(byte[] b)` calls `write(b, 0, b.length)`). Do not declare `throws IOException` on `write(int)` or
`write(byte[],int,int)` (the JDK does not; fixture code calls them outside `try`).

## Members to leave out

Nothing else exists on the JDK 21 class. Do not edit any other runtime file (`kit guard` rejects it). In
particular do **not** add `Objects.checkFromIndexSize` to `java/util/Objects.java` or `nullOutputStream()` to
`java/io/OutputStream.java`; write the range check inline instead.

## JDK behaviour to match

- `ByteArrayOutputStream(int size)`: `size < 0` throws
  `IllegalArgumentException("Negative initial size: " + size)` (so `-1` gives `Negative initial size: -1`).
  Otherwise `buf = new byte[size]`. `size == 0` is allowed. The no-argument constructor is `this(32)`.
- Growth (`ensureCapacity(minCapacity)`): if `minCapacity > buf.length`, the new capacity is
  `buf.length + Math.max(minCapacity - buf.length, buf.length)`, i.e. at least double, or exactly what is needed
  when that is more. Copy the old contents into the new array. Never shrink. The test reads `buf.length`, so
  the numbers must match exactly: from capacity 0, single writes give 1, 2, 4, 4, 8, 8, 8, 8, 16; from 3, writing
  10 bytes gives 10, then one more byte gives 20, then 50 more bytes give 61 (count 11 + 50 = 61 is more than
  double 20), then 3 more bytes give 122 (61 + max(3, 61)); 32 bytes then the 33rd gives 64.
- `write(int b)`: `ensureCapacity(count + 1)`, store `(byte) b` (only the low 8 bits; `256+66` stores 66),
  `count++`.
- `write(byte[] b, int off, int len)`: first the range check, exactly like the JDK's
  `Objects.checkFromIndexSize(off, len, b.length)`:
  `if ((off | len) < 0 || len > b.length - off)` throw
  `new IndexOutOfBoundsException("Range [" + off + ", " + off + " + " + len + ") out of bounds for length " + b.length)`.
  Examples: `(-1,1)` on length 5 -> `Range [-1, -1 + 1) out of bounds for length 5`;
  `(1, Integer.MAX_VALUE)` -> `Range [1, 1 + 2147483647) out of bounds for length 5` (no overflow: compare
  `len > b.length - off`, never `off + len > b.length`). `(5,0)` on length 5 is fine (writes nothing).
  A `null` array throws `NullPointerException` (reading `b.length` does that). A failed call leaves `count`
  unchanged. Then `ensureCapacity(count + len)`, `System.arraycopy(b, off, buf, count, len)`, `count += len`.
  The exception type is plain `IndexOutOfBoundsException`, not `ArrayIndexOutOfBoundsException`.
- `writeBytes(byte[] b)`: `write(b, 0, b.length)` (null -> `NullPointerException`).
- `writeTo(OutputStream out)`: one call `out.write(buf, 0, count)` — it passes the **internal** array, so the
  receiver sees `b.length == buf.length` (32 for a default stream), offset 0 and length `count`. An empty
  stream still makes the call (with length 0). An `IOException` from `out` propagates unchanged. `null` ->
  `NullPointerException`.
- `reset()`: `count = 0`; the buffer and its capacity are kept.
- `toByteArray()`: a new array of length `count` (`Arrays.copyOf(buf, count)`); changing it does not affect the
  stream. An empty stream gives a length-0 array.
- `size()`: `count`.
- `toString()`: `new String(buf, 0, count)` — the default charset, which is UTF-8 both in the runtime and on
  the JDK 21 test JVM. Malformed bytes become `�` exactly as `String` already does it.
- `toString(String charsetName)`: `new String(buf, 0, count, charsetName)`. An unknown or illegal name throws
  `UnsupportedEncodingException(name)` (message is just the name, e.g. `no-such-charset`, `bad name!`); `null`
  throws `NullPointerException`. The runtime `String` constructor already does all of this: just call it.
- `toString(Charset charset)`: `new String(buf, 0, count, charset)`; `null` -> `NullPointerException`.
- `toString(int hibyte)`: `new String(buf, hibyte, 0, count)` (each char is `((hibyte & 0xff) << 8) | (b & 0xff)`).
- Every `toString` uses only the first `count` bytes, also when a subclass changed `count` or `buf` itself.
- `write` and `ensureCapacity` also read the current `buf` and `count` each time: a subclass that does
  `super(0); buf = new byte[]{65,66,67}; count = 3;` and then calls `write('D')` grows `buf.length` from 3 to 6
  (the `subclassFields` fixture expects `3ABC,6ABCD,2AB[65, 66],aBZ,6`).
- `close()`: does nothing. Writes, `toString`, `toByteArray` and `writeTo` keep working after `close()`.

## Steps

1. Read `javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeByteArrayOutputStreamTest.java`.
2. Read `javaw-bytecode/src/main/resources/exception-runtime/java/io/OutputStream.java` (the superclass) and
   check the `String` constructors you will call:
   `grep -n "public String(byte" javaw-bytecode/src/main/resources/exception-runtime/java/lang/String.java`.
3. Create `javaw-bytecode/src/main/resources/exception-runtime/java/io/ByteArrayOutputStream.java`.
4. Register it: `javaw-agent-kit/bin/kit register java/io/ByteArrayOutputStream.java`. (It adds
   `paths.add("java/io/ByteArrayOutputStream.java");` before the `ExceptionSupport` line of
   `BytecodeExceptionRuntime.java`; do not edit that file by hand.)
5. `javaw-agent-kit/bin/kit check T19` until PASS.
6. Write `docs/bytecode-byte-array-output-stream.md` (below), the report, then `javaw-agent-kit/bin/kit finish T19`.

## Hints

- The whole class is about 40 lines. Use `java.util.Arrays.copyOf(byte[], int)` (it exists in the runtime) for
  growth and `toByteArray`, and `System.arraycopy` in `write(byte[],int,int)`. Add `import java.util.Arrays;`
  or write `java.util.Arrays.copyOf` in full.
- No `synchronized` anywhere (the JDK methods are synchronized; RULES.md forbids it). No `import jdk.internal...`:
  `ArraysSupport.newLength` does not exist; the growth rule above is what it computes for these sizes.
- `Objects.checkFromIndexSize` does **not** exist in the runtime and must not be added. Write the check inline.
- Refer to `Charset` as `java.nio.charset.Charset` or add `import java.nio.charset.Charset;`. Both
  `UnsupportedEncodingException` and `IOException` are in `java.io`, so no import is needed for them.
- `toString(String)` must declare `throws UnsupportedEncodingException`; otherwise javac rejects the call to
  `new String(byte[], int, int, String)`.
- Do not catch and rewrap anything: the `String` constructors already throw the JDK types and messages.
- Do not call `super.write(b, off, len)`: it writes byte by byte through `write(int)`, so the buffer grows one
  byte at a time (from capacity 3, ten bytes gives 12 instead of 10) and `growthByArray` fails.
- Calibration: the reference passed kit check after 1 run (about 38 new lines). Mutations calibrated against the
  test: growing to exactly `minCapacity`, checking `off + len > b.length`, and passing `toByteArray()` to
  `writeTo` each fail a fixture (`growthByOne`, `writeRangeFailures`, `writeTo`).

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
constructsAndGrows, writesBytesAndChecksRanges, copiesResetsAndWritesTo, decodesStrings, stringFailuresAndClose: if the message says `link a complete acyclic type hierarchy: java/io/ByteArrayOutputStream` or `unsupported ... call java/io/ByteArrayOutputStream.<name>`, the class is missing, not registered, or lacks that member: run `javaw-agent-kit/bin/kit register java/io/ByteArrayOutputStream.java` once, and give the class a public method with exactly that name, parameter types and return type; if it says `Cannot build bytecode exception runtime`, javac rejected ByteArrayOutputStream.java: fix the quoted line (no synchronized, no jdk.internal imports, no Objects.checkFromIndexSize, and toString(String) needs `throws UnsupportedEncodingException`).
defaultCapacities, growthByOne, growthByArray: the test reads `buf.length`; the no-argument constructor is `this(32)`, and ensureCapacity(min) does `if (min > buf.length) buf = Arrays.copyOf(buf, buf.length + Math.max(min - buf.length, buf.length));` (from 0 that gives 1,2,4,4,8; from 3 writing 10 bytes gives exactly 10); never grow when min <= buf.length and never allocate count+1 or a fixed step.
negativeSize: the int constructor starts with `if (size < 0) throw new IllegalArgumentException("Negative initial size: " + size);` and then `buf = new byte[size];` (0 is allowed).
subclassFields: `buf` and `count` must be `protected` fields named exactly `buf` and `count` (not private, no getters), and every method (size, toString, toByteArray, write, ensureCapacity) must read the fields each time, never a cached copy, because the subclass assigns `buf = new byte[]{65,66,67}; count = 3;` itself.
writeSingleBytes: write(int) stores `(byte) b` at `buf[count]` after `ensureCapacity(count + 1)`, then `count++`; do not throw for values outside 0..255.
writeRanges, writeBytesAndInherited: write(b, off, len) copies with `System.arraycopy(b, off, buf, count, len)` after `ensureCapacity(count + len)` and then `count += len`; writeBytes(b) is `write(b, 0, b.length)`; do not override write(byte[]) or flush().
writeRangeFailures: the check is `if ((off | len) < 0 || len > b.length - off) throw new IndexOutOfBoundsException("Range [" + off + ", " + off + " + " + len + ") out of bounds for length " + b.length);` BEFORE ensureCapacity or any change to count; do not compute `off + len` (it overflows for (1, Integer.MAX_VALUE)) and do not throw ArrayIndexOutOfBoundsException.
nullArrays: a null array must throw NullPointerException before count changes; reading `b.length` in the range check does that, so do not add a null check that throws something else.
toByteArrayCopies, resetKeepsBuffer: toByteArray returns `Arrays.copyOf(buf, count)` (a fresh array, never `buf` itself); reset() only sets `count = 0` and keeps `buf` (capacity unchanged).
writeTo, writeToFailures: writeTo(out) is exactly one call `out.write(buf, 0, count);` passing the internal array (the recorder prints `a32,0,3` for a default stream, so do not pass toByteArray() and do not loop over write(int)); call it even when count is 0, and let the IOException propagate.
defaultCharsetString, malformedUtf8: toString() is `return new String(buf, 0, count);` (the runtime String decodes UTF-8 and replaces bad bytes); do not decode by hand and do not pass `buf` without `0, count`.
charsetStrings, hibyteStrings: toString(String name) is `return new String(buf, 0, count, name);`, toString(Charset cs) is `return new String(buf, 0, count, cs);` and toString(int hibyte) is `return new String(buf, hibyte, 0, count);` (that argument order: array, hibyte, offset, count).
stringFailures: do not catch or rewrap: the String constructors already throw UnsupportedEncodingException with the bare name as message for `no-such-charset` and `bad name!`, and NullPointerException for a null name or Charset.
closeIsNoOp: close() has an empty body and sets no flag; writes after close must still work.
```

## Docs

Create `docs/bytecode-byte-array-output-stream.md` with this skeleton (fill in the brackets):

    # Bytecode ByteArrayOutputStream

    Revision `byte-array-output-stream-vN` links `java.io.ByteArrayOutputStream` in the bundled exception runtime.

    Contract:

    - <constructors and growth, write/writeBytes range checks and messages, writeTo, reset, toByteArray, size,
      toString overloads and their charsets, close as a no-op, protected buf/count>

    Evidence: `BytecodeByteArrayOutputStreamTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
    orders, two fresh instances).
