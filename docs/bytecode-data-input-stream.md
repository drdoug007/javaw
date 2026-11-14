# Bytecode DataInputStream

Revision `data-input-stream-v289` links `java.io.FilterInputStream`, `java.io.DataInput` and
`java.io.DataInputStream` in the bundled exception runtime.

Contract:

- `FilterInputStream` extends `InputStream` and holds a `protected InputStream in` field (assigned by the
  protected constructor; `null` is allowed), read fresh on every call. Every method is a one-line delegation:
  `read()`, `read(byte[],int,int)`, `skip(long)`, `available()`, `close()`, `mark(int)`, `reset()` and
  `markSupported()` call the same member on `in` (so a `null` `in` throws `NullPointerException`, and
  `read(null)` fails at `b.length` before the delegation); `read(byte[])` calls the overridable
  `read(b, 0, b.length)` of `this`, so subclasses that override the 3-argument read see it. The bulk
  read applies no range check of its own. `readAllBytes`, `readNBytes`, `skipNBytes` and `transferTo`
  are not overridden: the inherited `InputStream` defaults call `this.read(byte[],int,int)`, which
  delegates to `in`, and the inherited `skip(long)` default reads through the source's bulk read.
- `DataInput` is a plain interface declaring exactly 14 abstract members, each `throws IOException`:
  `readFully(byte[])`, `readFully(byte[],int,int)`, `skipBytes(int)`, `readBoolean`, `readByte`,
  `readUnsignedByte`, `readShort`, `readUnsignedShort`, `readChar`, `readInt`, `readLong`, `readFloat`,
  `readDouble` and `readUTF`. There are no default methods and no `readLine`.
- `DataInputStream` extends `FilterInputStream`, implements `DataInput`, and keeps the JDK fields
  `private final byte[] readBuffer = new byte[8]` plus `private byte[] bytearr = new byte[0]` and
  `private char[] chararr = new char[0]` for `readUTF`. It overrides `read(byte[])` as
  `in.read(b, 0, b.length)` and `read(byte[],int,int)` as `in.read(b, off, len)`, both `final`, going
  straight to `in` with no range check (a `ByteArrayInputStream` underneath reports its own
  `Range [...] out of bounds for length ...`). It does not override `read()`, `skip`, `available`,
  `close`, `mark`, `reset` or `markSupported`: the `FilterInputStream` versions are the JDK behaviour.
- `readFully(b)` is `readFully(b, 0, b.length)` (a `null` array throws `NullPointerException` from
  `b.length` before any read). `readFully(b, off, len)` first applies the inherited package-private
  `checkFromIndexSize(off, len, b.length)` (the JDK `Range [off, off + len) out of bounds for length
  b.length` `IndexOutOfBoundsException`, nothing read), then loops
  `int count = in.read(b, off + n, len - n); if (count < 0) throw new EOFException(); n += count;`
  until `n == len`: short reads are retried with the rest, `len == 0` makes no `read` call (even with a
  `null` `in`), and at end of stream a `new EOFException()` (message `null`) is thrown with the bytes
  read so far stored in `b` and consumed from `in`.
- `skipBytes(n)` is `int total = 0, cur; while (total < n && (cur = (int) in.skip(n - total)) > 0)
  total += cur; return total;`: it calls `in.skip`, never `this.skip`, `n <= 0` returns `0` without
  calling `skip`, a `skip` result of `0` or less stops the loop, and the `long` result is cast to `int`
  (a `skip` that returns `0x100000002L` counts as 2).
- The primitive reads call `in.read()` once (`readUnsignedByte` throws `new EOFException()` on a
  negative value; `readBoolean` is `readUnsignedByte() != 0`; `readByte` is `(byte) readUnsignedByte()`)
  or `readFully(readBuffer, 0, n)` so the source always sees the 8-byte buffer, then combine
  big-endian, most significant byte first, masking every byte with `& 0xff`: `readShort` casts to
  `short`, `readChar` to `char`, `readUnsignedShort` does not cast; `readInt` shifts the four bytes by
  `24`, `16`, `8`, `0`; `readLong` is `((long) hi << 32) + (lo & 0xFFFFFFFFL)` of the two `readInt`-style
  halves (the masked low half gives `0xffffffff` for `4294967295`, not `-1`); `readFloat` is
  `Float.intBitsToFloat(readInt())` and `readDouble` is `Double.longBitsToDouble(readLong())`, so NaN
  payloads and the `-0.0` sign bit pass through unchanged. At end of stream every read throws
  `new EOFException()` (message `null`) from `readFully`/`readUnsignedByte`, after consuming what was
  there.
- `readUTF()` is `readUTF(this)`. The static `readUTF(DataInput in)` reads `utflen` with
  `in.readUnsignedShort()`, takes `readBuffer`-style buffers from the instance's `bytearr`/`chararr`
  fields when `in instanceof DataInputStream` — replaced by `new byte[utflen * 2]`/`new char[utflen * 2]`
  only while `bytearr.length < utflen` — or allocates `new byte[utflen]`/`new char[utflen]` for any other
  `DataInput`; it then makes the single call `in.readFully(bytearr, 0, utflen)` (so the source sees the
  buffer length, and a short stream throws `EOFException` from `readFully`/`readUnsignedShort` before any
  decoding) and decodes modified UTF-8: a first loop consumes the leading run of bytes `<= 127` as single
  chars (breaking at the first `> 127`), and a second loop handles the rest on `c >> 4`: `0..7` is one
  byte, `12`/`13` is `((c & 0x1F) << 6) | (char2 & 0x3F)` after `count += 2`, `14` is
  `((c & 0x0F) << 12) | ((char2 & 0x3F) << 6) | (char3 & 0x3F)` after `count += 3`, anything else
  (`8..11`, `15`) throws `UTFDataFormatException("malformed input around byte " + count)`. The partial
  check (`count > utflen` after the `+= 2`/`+= 3`, message `malformed input: partial character at end`)
  runs before the continuation bytes are inspected; a bad 2-byte continuation throws `malformed input
  around byte " + count` and a bad 3-byte continuation `malformed input around byte " + (count - 1)`,
  where the signed `char2`/`char3` bytes are tested with `& 0xC0` only. Non-shortest forms (`c0 80`,
  `e0 80 80`) and lone surrogates (`ed a0 80`) are accepted, all `utflen` bytes are consumed before
  decoding (so a malformed string leaves the stream past it), and the result is
  `new String(chararr, 0, chararr_count)`. `readUTF(null)` throws `NullPointerException`.
- Single-threaded: no monitors, no `volatile` (the JDK's `in` field is volatile; the bundled field is a
  plain `protected` field), no `Thread`, no `native`.
