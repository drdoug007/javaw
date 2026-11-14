# Bytecode ByteArrayOutputStream

Revision `byte-array-output-stream-v279` links `java.io.ByteArrayOutputStream` in the bundled exception
runtime.

Contract:

- Constructors: the no-argument form allocates capacity 32 (`this(32)`); `ByteArrayOutputStream(int)` throws
  `IllegalArgumentException("Negative initial size: " + size)` for a negative size and allows 0. Growth
  (`ensureCapacity`) never shrinks and follows the JDK's overflow-conscious form: with
  `minGrowth = minCapacity - buf.length`, it grows only when `minGrowth > 0` (so a `count + len` that wrapped
  negative still reaches the size check), to the JDK 21 `ArraysSupport.newLength(buf.length, minGrowth,
  buf.length)` result: `buf.length + Math.max(minGrowth, buf.length)` (at least double, or exactly what is
  needed when that is more) when that is positive and at most `Integer.MAX_VALUE - 8`; otherwise, if
  `buf.length + minGrowth` overflows, `OutOfMemoryError("Required array length " + buf.length + " + " +
  minGrowth + " is too large")` with `buf` and `count` unchanged, else `max(Integer.MAX_VALUE - 8,
  buf.length + minGrowth)`. The old contents are copied.
- `write(int)` stores only the low 8 bits after `ensureCapacity(count + 1)` and never throws for out-of-range
  values. `write(byte[], int, int)` first range-checks with
  `if ((off | len) < 0 || len > b.length - off) throw new IndexOutOfBoundsException("Range [" + off + ", " +
  off + " + " + len + ") out of bounds for length " + b.length)` (no `off + len` overflow; null array throws
  `NullPointerException`), then copies with `System.arraycopy` into `buf` starting at `count`. `writeBytes(b)`
  delegates to `write(b, 0, b.length)`. The inherited `OutputStream.write(byte[])` and `flush()` are not
  overridden.
- `writeTo(OutputStream)` makes exactly one call `out.write(buf, 0, count)` with the internal array (so the
  receiver sees `b.length == buf.length`), even for an empty stream; an `IOException` from the receiver
  propagates unchanged and a null receiver throws `NullPointerException`.
- `reset()` sets `count = 0` and keeps `buf` and its capacity. `toByteArray()` returns a fresh
  `Arrays.copyOf(buf, count)`. `size()` returns `count`. `close()` does nothing; everything keeps working
  afterwards.
- `buf` and `count` are protected fields, and every method reads them each call, so subclasses that reassign
  `buf`/`count` directly (as JDK subclasses and tests do) drive the stream's behaviour.
- `toString()` is `new String(buf, 0, count)` (default charset, UTF-8, malformed bytes replaced as the
  runtime `String` does); `toString(String)` and `toString(Charset)` delegate to the runtime `String`
  constructors and throw `UnsupportedEncodingException(name)` / `NullPointerException` exactly as they do;
  `toString(int hibyte)` is `new String(buf, hibyte, 0, count)`. All `toString` overloads use only the first
  `count` bytes.

Evidence: `BytecodeByteArrayOutputStreamTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
orders, two fresh instances). `growthOverflow` drives the overflow `OutOfMemoryError` paths through a subclass whose
`count` is near `Integer.MAX_VALUE` over a 3-byte `buf`, so no large array is allocated. The soft-max cap
(for example a 2^30-byte buffer growing to `Integer.MAX_VALUE - 8`) needs gigabyte arrays and is checked
only on the JVM, by comparing a renamed copy of the runtime class with the JDK class.
