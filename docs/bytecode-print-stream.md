# Bytecode PrintStream

Revision `print-stream-v298` links `java.io.PrintStream` over an `OutputStream` in the bundled exception runtime
(single-threaded contract: no lock and no monitors). `System.out` and `System.err` are not linked.

Contract:

- Constructors: `PrintStream(OutputStream)` delegates to `(out, false)`; the two-argument form checks
  `requireNonNull(out, "Null output stream")` before `super(...)` and takes the charset of a wrapped
  `PrintStream` (`((PrintStream) out).charset`) or `Charset.defaultCharset()` (UTF-8) otherwise. The
  `String encoding` form checks `out` first (same `NullPointerException`), then resolves the name:
  `null` name is `NullPointerException("charsetName")`, an unknown or illegal name
  (`IllegalCharsetNameException`/`UnsupportedCharsetException` from `Charset.forName`) is
  `UnsupportedEncodingException(csn)`, aliases such as `utf-16le`, `latin1` and `UTF8` work. The `Charset`
  form performs **no** null check of `out` (a stream with `out == null` behaves like a closed one, setting
  the error flag on writes) and requires the charset (`NullPointerException("charset")`). `PrintStream(String
  fileName)` and the `File` constructors stay unlinked and are rejected before WAT generation.
- Raw bytes: `write(int b)` passes the int to `out.write` unchanged (so `write(0x10a)` writes byte `0x0a` but
  does not auto-flush; only the exact int `'\n'` does). `write(byte[], int, int)` has no range or null check
  of its own — `out.write` is called even for `len == 0`, and a bad range throws the `IndexOutOfBoundsException`
  of the underlying stream, uncaught. `write(byte[])` and `writeBytes(byte[])` are `this.write(buf, 0,
  buf.length)`; `buf.length` is read first, so `null` is a `NullPointerException` even after `close()`.
  `write(byte[])` keeps `throws IOException` (Java 14+); every other public and protected method declares no
  `throws`.
- Text encoding: every piece of text goes through `encode(String)` into one call of the overridable
  `write(byte[], 0, n)` on **this** (never `out.write` directly), encoded with `s.getBytes(charset)`. A high
  surrogate that ends a write is held (`haveLeftover`/`leftover`) and prepended to the next text write; a
  following low surrogate is the one code point, anything else encodes the lone surrogate as the charset does
  (`?` in UTF-8, ISO-8859-1 and US-ASCII, where a surrogate pair is a single `?`; U+FFFD in the UTF-16 forms).
  An empty string (or one that is only a held surrogate) makes no write call. Raw `write` calls do not touch
  the held char and `flush()` keeps it. For the charset named `UTF-16` only, the runtime `getBytes` prepends
  the byte order mark `fe ff` to every call, while the JDK writes it once: `emit` keeps the mark on the first
  emitted text write and strips it afterwards (`marked`); raw bytes do not set it.
- `print(x)` is `writeText(String.valueOf(x))` for every scalar form and `writeText(char[])` for the array;
  `print(char)` goes through the text path, never `write(int)`. `println(x)` on a plain `PrintStream`
  (`getClass() == PrintStream.class`) is `writeLine(String.valueOf(x))`, which encodes `s + "\n"` — text and
  line end in one write; a subclass instead sees `print(x)` and a separate `newLine()`. `println(Object)`
  builds `String s = String.valueOf(x)` first, so a `toString()` of `null` prints `null`. The line end is the
  constant `"\n"`.
- `format`/`printf`: `format` is `ensureOpen` then `append(String.format(format, args))` (and
  `String.format(l, format, args)` with a possibly `null` locale), `printf` returns `format`; the
  `IllegalFormatException` subclass or `NullPointerException` of a bad format propagates uncaught, and after
  `close()` the format string is not looked at. `append(CharSequence)` is `print(String.valueOf(csq))`,
  `append(csq, start, end)` maps a `null` sequence to `"null"` and then `subSequence` (bad bounds throw
  uncaught), `append(char)` is `print(char)`; all three return `this` as `PrintStream`.
- Auto-flush: with `autoFlush` on, every non-empty raw or text write flushes once inside
  `write(byte[], int, int)`; `writeText` flushes a second time when the text contains `'\n'`; `writeLine` and
  `newLine` always flush a second time; `write(int)` only for the exact int `'\n'`. With it off, nothing
  flushes until `flush()`.
- Error flag: every call on `out` sits in `try { ensureOpen(); ... } catch (IOException x) { trouble = true;
  }` — only `IOException` is swallowed, so an `IllegalStateException` from the underlying stream propagates
  unchanged and leaves the flag alone. `checkError()` flushes (the overridable `flush()`) when `out != null`,
  defers to the wrapped stream when `out instanceof PrintStream`, and otherwise returns `trouble` without
  resetting it; after a clean `close()` it returns `false`. `setError()`/`clearError()` set/clear the flag.
  Every method re-reads the inherited `out` field, so a subclass may retarget it.
- `close()`: once only (`closing`); first encodes a held high surrogate on its own
  (`String.valueOf(leftover).getBytes(charset)`), then the overridable `flush()`, the overridable `close()`
  (returns at once) and `out.close()`; an `IOException` sets the flag, and `out` becomes `null` even when
  `out.close()` throws — a second `close()` does nothing. With `out == null` from the start, `flush()` sets
  the flag and `out.close()` throws `NullPointerException` out of `close()`.
- Known differences from JDK 21 (not observed by the bundled tests): the JDK encodes through an 8192-char
  `BufferedWriter` and a byte buffer of at most 8192 bytes, so a text of more than 8192 chars or encoded
  bytes reaches the stream in several `write(byte[], int, int)` calls; here each text write is one call (the
  bytes are the same). The JDK also splits much shorter texts after a held high surrogate: its encoder
  starts with a 512-byte buffer, encodes the completed pair (or `?` plus the next char) first, and then
  writes those bytes as a separate call when the rest of the text needs a larger buffer (more than 170
  remaining chars in UTF-8, more than 512 in ISO-8859-1 or US-ASCII), or splits at the buffer capacity
  when the bytes overflow it. For example, UTF-8 `print("\uD83D")` then `print("\uDE00" + "x".repeat(200))`
  is two calls of 4 and 200 bytes on the JDK and one call of 204 bytes here. And the JDK's `Formatter` parses the whole format string first, appending each literal
  piece and each converted argument separately (one write per piece, each auto-flushed, with a failing
  argument leaving the earlier pieces written); here the whole `String.format` result is one
  `append(CharSequence)` and a failing format writes nothing — an empty result still calls `append("")`
  (no bytes are written either way).

Evidence: `BytecodePrintStreamTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
fresh instances), including a subclass and an underlying stream that record every call. `PrintStream(String)` is
rejected before WAT generation.
