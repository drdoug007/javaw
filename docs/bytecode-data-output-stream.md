# Bytecode DataOutputStream

Revision `data-output-stream-v284` links `java.io.FilterOutputStream`, `java.io.DataOutput`,
`java.io.DataOutputStream` and `java.io.UTFDataFormatException` in the bundled exception runtime.

Contract:

- `FilterOutputStream` delegates to a `protected OutputStream out` field read on every call:
  `write(int)` passes the int through unchanged; `write(byte[])` calls the overridable
  `write(b, 0, b.length)`; `write(byte[],int,int)` first reads `b.length` (a `null` array fails there with
  `NullPointerException`, before any range check) and then applies the JDK
  `Objects.checkFromIndexSize` range test `if ((off | len) < 0 || len > length - off)` with the JDK message
  `Range [off, off + len) out of bounds for length length`, after which each byte is sent one at a time
  through `this.write(int)`, sign-extended. `flush()` calls `out.flush()`. `close()` sets a private
  `closed` flag first: `if (closed) return; closed = true;`, then calls the overridable `flush()`, always
  follows it with `out.close()`. If only the flush fails, the flush exception is thrown. If `out.close()`
  fails, its exception is thrown, with a flush exception attached via `addSuppressed` unless both are the same
  object (as in JDK 21); a second `close()` does nothing,
  even after a close that threw, and `write`/`flush` never consult the flag.
- `DataOutputStream` extends `FilterOutputStream`, implements `DataOutput`, and counts bytes in a
  `protected int written` field via `incCount(value)`: `int temp = written + value; if (temp < 0) temp =
  Integer.MAX_VALUE; written = temp;`, run only after the sink call returned, so `size()` sticks at
  `Integer.MAX_VALUE` on overflow and a throwing sink leaves it unchanged. Every data write calls the
  sink directly (`out.write(...)`, `out.flush()`) and `incCount` once after it: `writeBoolean` sends
  `v ? 1 : 0`, `writeByte` and `write(int)` pass the int through, and `writeShort`/`writeChar`/`writeInt`/
  `writeLong`/`writeFloat`/`writeDouble` fill a reusable `byte[8] writeBuffer` big-endian, most significant
  byte first (`Float.floatToIntBits`/`Double.doubleToLongBits`, so any NaN becomes `7fc00000`/
  `7ff8000000000000`), then make one `out.write(writeBuffer, 0, n)` call. `write(byte[],int,int)` is exactly
  `out.write(b, off, len); incCount(len);` with no range check of its own, so a sink rejects or accepts
  the arguments as-is (a negative length even decrements or clamps `written`). `writeBytes` sends
  `out.write((byte) s.charAt(i))` per character and counts `len` once at the end; `writeChars` sends one
  big-endian `out.write(writeBuffer, 0, 2)` per character and counts `len * 2` once. `writeUTF` computes
  the modified UTF-8 length first (`\0` and `0x0080..0x07FF` take 2 bytes, `0x0800..0xFFFF` take 3, each
  surrogate on its own), throws `UTFDataFormatException` with the JDK
  `encoded string (first8...last8) too long: N bytes` message (computed from
  `s.length() + Integer.toUnsignedLong(utflen - s.length())`) before writing anything when
  `utflen > 65535 || utflen < strlen`, reuses a `bytearr` field (grown to `utflen * 2 + 2` only when null
  or too short), stores the big-endian length prefix at `bytearr[0..1]`, encodes from index 2, and ends
  with one `write(bytearr, 0, utflen + 2)` call on `this`, which performs the counting. The inherited
  `FilterOutputStream.write(byte[])` and `close()` are the JDK behaviour: the 2-argument write delegates
  to the overridable 3-argument method, and close flushes through the overridable `flush()`.
- `DataOutput` declares exactly the 14 abstract write methods, each `throws IOException`; a plain
  `FilterOutputStream` is not a `DataOutput`. `UTFDataFormatException` extends `IOException` with a
  no-argument and a `String` constructor.

Evidence: `BytecodeDataOutputStreamTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders,
two fresh instances).
