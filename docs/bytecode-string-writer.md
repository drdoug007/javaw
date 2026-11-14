# Bytecode StringWriter and CharArrayWriter

Revision `string-writer-v285` links `java.io.StringWriter` and `java.io.CharArrayWriter` in the bundled exception
runtime. Like `Writer`, they keep the JDK `lock` field but never enter a monitor (single-threaded contract).

Contract:

- `StringWriter` holds a `private final StringBuffer buf`. Both constructors assign `lock = buf` (so a
  subclass sees `lock == getBuffer()`); `StringWriter(int)` rejects a negative size with
  `IllegalArgumentException("Negative buffer size")` (no number in the message) while `0` is allowed.
  `write(int)` appends only the low 16 bits as a char; `write(char[],int,int)` reads `cbuf.length` first
  (a `null` array fails there with `NullPointerException`, even for `(null,0,0)`) and applies the JDK
  `Range [off, off + len) out of bounds for length length` test `if ((off | len) < 0 || len > length - off)`
  inline, never computing `off + len`, writes nothing on failure, and returns immediately on `len == 0`;
  `write(char[])` and the inherited `Writer` forms are not overridden, so they funnel through
  `write(char[],int,int)` with `NullPointerException` for a `null` array. `write(String)` appends the string
  to `buf` directly (a `null` string appends the four chars `null`), and `write(String,int,int)` is exactly
  `buf.append(str, off, off + len)` with no range check of its own, so a bad range throws plain
  `IndexOutOfBoundsException` from `StringBuffer` and `(null,1,2)` writes `ul`. `append(CharSequence)` calls
  the overridable `write(String)` on `String.valueOf(csq)` and returns `this`; `append(csq,start,end)`
  reassigns a `null` `csq` to `"null"` and calls the overridable `append(CharSequence)` on
  `csq.subSequence(start,end)`, so a bad range throws what `subSequence` throws
  (`StringIndexOutOfBoundsException` for `String` and `StringBuilder`); `append(char)` calls the overridable
  `write(int)`. All `append` returns are covariant (`StringWriter`). `getBuffer()` returns the live buffer
  itself (same object every call; `setLength`, `insert`, `append` on it change what the writer holds),
  `toString()` is `buf.toString()`, and `flush()`/`close()` are no-ops with no closed flag, so writing after
  `close()` still works.
- `CharArrayWriter` keeps `protected char[] buf` and `protected int count`, read by every method so a
  subclass may assign them directly (`lock` stays the `Writer` default `this`). `CharArrayWriter(int)`
  rejects a negative size with `IllegalArgumentException("Negative initial size: " + initialSize)` while `0`
  is allowed, and the no-argument constructor is `this(32)`. Every write grows first with
  `int newcount = count + len; if (newcount > buf.length) buf = Arrays.copyOf(buf, Math.max(buf.length << 1,
  newcount));` (the same rule as `ByteArrayOutputStream`: from capacity 0 single writes give 1, 2, 4, 4, 8,
  ...; `len == 0` and negative lengths never grow). `write(char[],int,int)` applies the same inline range
  check as `StringWriter` before the growth; `write(int)` stores `(char) c`; `write(String,int,int)` has no
  range check of its own and grows for `count + len` before `str.getChars(off, off + len, buf, count)`, so a
  bad range throws `StringIndexOutOfBoundsException` and leaves the grown capacity with `count` unchanged
  (`("abc",1,5)` on a capacity-2 writer holding one char leaves capacity 6, size 1), a `null` string throws
  `NullPointerException`, and `write(char[])`/`write(String)` are not overridden, so
  `write((String) null)` fails in the inherited `Writer.write(String)`. `writeTo(Writer)` is exactly one
  call `out.write(buf, 0, count)` passing the internal array, made even when empty, with no null check or
  catch (null throws `NullPointerException`; a sink `IOException` propagates unchanged). `append(CharSequence)`
  calls the overridable `write(String,int,int)` on `String.valueOf(csq)`; `append(csq,start,end)` and
  `append(char)` behave as in `StringWriter`, returning covariant `CharArrayWriter`. `reset()` only sets
  `count = 0` and keeps the buffer and its capacity; `toCharArray()` returns a fresh
  `Arrays.copyOf(buf, count)`; `size()` is `count`; `toString()` is `new String(buf, 0, count)`;
  `flush()`/`close()` are no-ops, so writing after `close()` still works.

Evidence: `BytecodeStringWriterTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
fresh instances), including subclasses that record the overridable calls each method makes.
