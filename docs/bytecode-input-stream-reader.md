# Bytecode InputStreamReader

Revision `input-stream-reader-v297` links `java.io.InputStreamReader` in the bundled exception runtime. The
decoder is written inside `InputStreamReader` (no `CharsetDecoder`, `ByteBuffer` or `CharBuffer`) and takes no
lock (single-threaded contract).

Contract:

- Constructors: `super(in)` first (a `null` stream is a `NullPointerException` with no message, before any other
  argument is examined); then `null` charset name is `NullPointerException("charsetName")` and `null` charset is
  `NullPointerException("charset")`; an unknown or illegal name is the `UnsupportedEncodingException` whose message
  is the name, from `javaw.runtime.TextCharsets.lookup`. `InputStreamReader(in)` decodes UTF-8. `getEncoding()`
  returns the JDK historical name (`UTF8`, `ISO8859_1`, `ASCII`) and `null` once closed (also after a `close()`
  whose `in.close()` threw).
- Bytes sit in an 8192-byte buffer. Each fill compacts the unread bytes to the front and then makes one
  `in.read(bytes, byteLimit, 8192 - byteLimit)` call, so the stream sees the full array with an offset equal to
  the bytes already buffered; a throw from that call leaves the unread bytes in place for the next call. A fill
  returning zero is `IOException("Underlying input stream returned zero bytes")`. After the first character of a
  `read(char[], int, int)` the loop asks `in.available() > 0` at most once before blocking on the next fill; while
  no character has been produced it keeps filling without asking, so a multi-byte sequence split across the
  underlying stream is reassembled. `in.available()` that throws makes the reader not ready rather than throw.
- Decoding is UTF-8, ISO-8859-1 (every byte is the char of the same value) or US-ASCII (every byte `>= 0x80` is one
  `U+FFFD`), in the JDK's replacement mode. Each malformed UTF-8 sequence is one `U+FFFD`, and the number of bytes
  it covers follows the JDK decoder: a lone continuation byte or an invalid lead (`80`, `c0`, `f8`, `ff`) is one
  byte; a lead whose second byte is out of range for it (`e0 80`, `f0 80`) is the lead alone, so each following
  continuation byte is its own `U+FFFD` (`e0 80 41` is `U+FFFD U+FFFD A`, `f0 80 80 41` is three `U+FFFD` then
  `A`); a valid prefix followed by a byte that cannot continue it is the prefix (`e2 82 41` and `f0 9f 98 41` are
  one `U+FFFD` then `A`); an encoded surrogate `ed a0 80` is one `U+FFFD` for all three bytes. The same counts
  apply at end of input (`e0 80` and `c0 80` give two `U+FFFD`, `f0 80 80` gives three); only a tail that is an
  incomplete sequence with nothing malformed in it yet (`c3`, `e2 82`, `f0 9f 98`) is one `U+FFFD` for the whole
  remaining tail. A supplementary character decodes to a surrogate pair that
  needs two free slots; with one slot left it waits for the next read. A `read()` keeps the low half of a decoded
  pair as the leftover, returned on the next `read()` even after `close()`.
- `read(char[], int, int)` checks `ensureOpen` (`IOException("Stream closed")`) before the argument test, then the
  JDK five-condition range test (no-message `IndexOutOfBoundsException`, overflow-safe), then returns for `len == 0`
  without touching the stream; a `null` array is a `NullPointerException`. A pending leftover is copied first and,
  if that fills the request or nothing is ready, the method returns at once; a single-slot request goes through the
  two-char read, which parks the second char. `read(char[])`, `skip`, `transferTo`, `mark` and `reset` are inherited
  from `Reader` unchanged, and the single-char read never calls the overridable `read(char[], int, int)`.
  `ready()` is `ensureOpen`, then a pending leftover, a buffered byte, or `in.available() > 0`. `close()` is
  `in.close()` with `closed` set in a `finally`, so the reader is closed even when the stream throws and a second
  `close()` does nothing.
- Known differences from JDK 21 (not observed by the bundled tests): UTF-16, UTF-16BE and UTF-16LE charsets throw
  `UnsupportedOperationException` (the JDK decodes them); there is no `InputStreamReader(InputStream,
  CharsetDecoder)` and no `read(CharBuffer)`.

Evidence: `BytecodeInputStreamReaderTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders,
two fresh instances). `read(CharBuffer)` is rejected before WAT generation.
