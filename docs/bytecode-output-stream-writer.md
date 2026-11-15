# Bytecode OutputStreamWriter

Revision `output-stream-writer-v296` links `java.io.OutputStreamWriter` and the `PrintWriter(OutputStream)` and
`PrintWriter(OutputStream, boolean)` constructors in the bundled exception runtime. The encoder is written inside
`OutputStreamWriter` (no `CharsetEncoder`/`ByteBuffer`) and takes no lock (single-threaded contract).

Contract:

- Constructors: `super(out)` first (a `null` stream is a `NullPointerException` with no message, before any other
  argument is examined); then `null` charset name is `NullPointerException("charsetName")` and `null` charset is
  `NullPointerException("charset")`; an unknown or illegal name is `UnsupportedEncodingException` whose message is
  the name, from `javaw.runtime.TextCharsets.lookup`. `OutputStreamWriter(out)` delegates to
  `this(out, Charset.defaultCharset())` (UTF-8). The six runtime charsets are UTF-8, ISO-8859-1, US-ASCII,
  UTF-16BE, UTF-16LE and UTF-16. `getEncoding()` returns the JDK historical name (`UTF8`, `ISO8859_1`, `ASCII`,
  `UnicodeBigUnmarked`, `UnicodeLittleUnmarked`, `UTF-16`) and `null` once closed. A lone surrogate, or a code point
  the charset cannot encode, becomes `?` (0x3F) in UTF-8, ISO-8859-1 and US-ASCII (a surrogate pair above the
  charset limit is one `?`) and U+FFFD in the UTF-16 forms; U+0000 is encoded like any other character; a code point never splits across two writes to the stream. UTF-16 writes the
  byte order mark `fe ff` once, before the first character of the first non-empty write (not in the constructor
  and not for `write("")`). A high surrogate that ends a write is kept pending; a following low surrogate is the
  one code point, anything else is a replacement. `flush()` leaves the pending surrogate; `close()` encodes it as
  a replacement. The pending surrogate is cleared only after it and the character that follows it are in the
  buffer, so if `out.write` throws while it is being completed it stays pending. `write(int)` casts to `char` and
  goes through the private `writeChars` (not the overridable `write(char[], int, int)`); `write(char[], int, int)`
  checks `ensureOpen`, then the JDK range test (no-message `IndexOutOfBoundsException`, overflow-safe), then
  returns for `len == 0`; `write(String, int, int)` checks `len < 0` first and then `String.getChars` (bad `off` is
  `StringIndexOutOfBoundsException`, `null` string `NullPointerException`); `append(CharSequence)` writes
  `String.valueOf(csq)` through the private `writeString`. The public `write` and `append` forms never call other
  overridable methods, and `write(char[])`, `write(String)`, `append(CharSequence, int, int)` and `append(char)`
  are inherited from `Writer`. Encoded bytes sit in an 8192-byte buffer with a growing capacity (512 to
  8192, `len * maxBytesPerChar` per write, capped at 8192); bytes reach the stream as one
  `out.write(bytes, 0, count)` from `flushBytes()`, called by `flush()`, `close()`, a `put` that does not fit
  (`capacity - count < n`) and a `grow` that raises capacity. ISO-8859-1 also writes a full buffer out
  before it keeps a trailing high surrogate pending, because the JDK's ISO-8859-1 encoder reports overflow on a
  full buffer before it looks at that surrogate (UTF-8, US-ASCII and the UTF-16 forms do not). `flush()` is `ensureOpen`, `flushBytes`,
  `out.flush()`. `close()` flushes a pending surrogate, the bytes, `out.flush()` and finally `out.close()`, and
  sets `closed` even when the stream throws. Afterwards `getEncoding()` is `null` and every write, append and
  `flush` throws `IOException("Stream closed")` (except `write(String, int, int)` with `len < 0`, which fails its
  range check first); a second `close()` does nothing. If `out.write` throws, the buffered bytes are
  dropped (`count` is zeroed before the call). A later `flush()`/`close()` then only issues `out.flush()`/
  `out.close()`, unless a high surrogate is still pending: `close()` then writes its replacement with
  `out.write` first (and if that write throws, `out.flush()`/`out.close()` are skipped, as in the JDK).
- `PrintWriter(OutputStream out)` is `this(out, false)` and `PrintWriter(OutputStream out, boolean autoFlush)`
  wraps an `OutputStreamWriter` over `out` in UTF-8, or, when `out` is a `PrintStream`, in that print stream's
  charset. The runtime `PrintStream` keeps its charset private (its `charset()` accessor is Java 18), so the
  constructor reads it by encoding one probe character with a throwaway `PrintStream(ps, false)` (which copies the
  charset) redirected to a `ByteArrayOutputStream`; nothing reaches `ps`. Over a `PrintStream`, `checkError()`
  flushes and then returns the print stream's `checkError()`, as the JDK's `psOut` does, because the print
  stream swallows the stream's `IOException` into its own flag; nothing reaches the stream before a flush, an auto-flushing
  `println`/`printf`, or `close`; stream `IOException`s only set the error flag. `PrintWriter(OutputStream,
  boolean, Charset)` stays unlinked and is rejected before WAT generation.
- Known differences from JDK 21 (not observed by the bundled tests): the JDK puts an 8192-char `BufferedWriter`
  between such a `PrintWriter` and its `OutputStreamWriter`; here the protected `out` field is the
  `OutputStreamWriter` itself, so output larger than the 512-byte starting buffer may reach the stream in
  different pieces (the bytes are the same). And after a failed `out.write`, the JDK's flipped `ByteBuffer`
  limits the next write-out to the old length before filling on; here the buffer is simply empty, so only
  `flush()`/`close()` after such a failure (including the replacement for a pending surrogate) are matched.

Evidence: `BytecodeOutputStreamWriterTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
orders, two fresh instances), including a subclass and output streams that record every call, the ISO-8859-1 full-buffer write-out
before a pending surrogate, a pending surrogate that survives a failed `out.write`, and `PrintWriter` over a
`PrintStream` in all six charsets with its error flag.
`PrintWriter(OutputStream, boolean, Charset)` is rejected before WAT generation.
