# T36 PrintStream

```kit
id: T36
slug: print-stream
title: link java.io.PrintStream over an OutputStream
tests: BytecodePrintStreamTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/io/PrintStream.java
allow: docs/bytecode-print-stream.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-print-stream-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodePrintStreamTest.java`. It builds on
`java.io.FilterOutputStream` (T24) and `java.io.ByteArrayOutputStream` (T19), which are already on main.

## Goal

Add `java.io.PrintStream` to the bundled runtime, for the case where it wraps another `OutputStream`. The pinned
libraries name `PrintStream` from 196 classes (115 of them in the static closure,
`runtime-target/target/audit/external-types.tsv`: H2's `JdbcSQLException` family and `org.h2.tools`, AspectJ's
weaver and bcel, logback's `ConsoleAppender`, log4j's `StatusLogger`, Spring Boot, ANTLR, Jandex, ...). The member
rows (`member-references.tsv`, 550 call sites) are `println(String)` 356, `print(String)` 54,
`printf(String, Object...)` 27, `println(Object)` 26, `println()` 13, `<init>(OutputStream)` 9, `flush()` 7,
`close()` 6, `write(int)` 4, `write(byte[], int, int)` 3, `write(byte[])` 3, `print(int)` 3, `print(char)` 3,
`<init>(OutputStream, boolean, String)` 3 (Spring Boot's `SpringApplicationBannerPrinter`, H2's `WebApp.tools`),
`<init>(OutputStream, boolean)` 2, `printf(Locale, String, Object...)` 2, and the other `print`/`println`
overloads, `format(Locale, ...)` and `checkError()` once or twice each. Tomcat's `CaptureLog` prints into a
`ByteArrayOutputStream`, and AspectJ's `MessageUtil$1HandlerPrintStream` subclasses `PrintStream`.

The JDK `PrintStream` uses an `InternalLock`, `synchronized` blocks, an `OutputStreamWriter` and a `BufferedWriter`.
Here the runtime is **single-threaded**: write it with no `synchronized` (RULES.md), and do not use
`OutputStreamWriter`, `BufferedWriter` or any `Writer` even if one exists in the runtime (T34 is not a dependency).
Encode text with `String.getBytes(Charset)` plus the small amount of encoder state described below.
`System.out` and `System.err` are not part of this task: do not mention `System` members other than
`System.arraycopy` in the file.

## Members to implement

`java/io/PrintStream.java` (`package java.io;`, imports `java.nio.charset.Charset`,
`java.nio.charset.IllegalCharsetNameException`, `java.nio.charset.UnsupportedCharsetException`, `java.util.Arrays`,
`java.util.Locale`): `public class PrintStream extends FilterOutputStream implements Appendable, Closeable`

Fields (all private; the protected `out` field is inherited from `FilterOutputStream`):

- `private final boolean autoFlush;`
- `private boolean trouble;` (the error flag)
- `private final Charset charset;`
- `private boolean closing;` (set by the first `close()`)
- `private boolean haveLeftover;` and `private char leftover;` (a high surrogate waiting for its low half)
- `private boolean marked;` (UTF-16 only: the byte order mark has been written)

Constructors:

- `public PrintStream(OutputStream out)`: `this(out, false);`
- `public PrintStream(OutputStream out, boolean autoFlush)`
- `public PrintStream(OutputStream out, boolean autoFlush, String encoding) throws UnsupportedEncodingException`
- `public PrintStream(OutputStream out, boolean autoFlush, Charset charset)`

Public and protected methods (only `write(byte[])` declares `throws IOException`; every other public and protected
method declares no `throws`):

- `public void flush()`, `public void close()`, `public boolean checkError()`
- `protected void setError()`, `protected void clearError()`
- `public void write(int b)`, `public void write(byte[] buf, int off, int len)`,
  `public void write(byte[] buf) throws IOException`, `public void writeBytes(byte[] buf)`
- `public void print(boolean b)`, `print(char c)`, `print(int i)`, `print(long l)`, `print(float f)`,
  `print(double d)`, `print(char[] s)`, `print(String s)`, `print(Object obj)` (all `public void`)
- `public void println()` and `public void println(x)` for the same nine parameter types:
  `boolean`, `char`, `int`, `long`, `float`, `double`, `char[]`, `String`, `Object`
- `public PrintStream printf(String format, Object... args)`,
  `public PrintStream printf(Locale l, String format, Object... args)`
- `public PrintStream format(String format, Object... args)`,
  `public PrintStream format(Locale l, String format, Object... args)`
- `public PrintStream append(CharSequence csq)`, `public PrintStream append(CharSequence csq, int start, int end)`,
  `public PrintStream append(char c)` (the return type is `PrintStream`)

Private helpers (names are suggestions; the test cannot see them):

- `private static <T> T requireNonNull(T obj, String message)`, `private static Charset toCharset(String csn) throws UnsupportedEncodingException`
- `private void ensureOpen() throws IOException`
- `private void encode(String s)` and `private void emit(byte[] bytes)` (the encoder, below)
- `private void writeText(String s)`, `private void writeText(char[] buf)` (the JDK's private `write(String)` and
  `write(char[])`), `private void writeLine(String s)`, `private void writeLine(char[] buf)` (the JDK's `writeln`),
  `private void newLine()`

## Members to leave out

- The file constructors `PrintStream(String fileName)`, `(String, String)`, `(String, Charset)`, `(File)`,
  `(File, String)`, `(File, Charset)`: the runtime has no `File` or `FileOutputStream`. The test checks that
  `PrintStream(String)` stays unlinked.
- `public Charset charset()`: it is a Java 18 method, so Java 17 code cannot call it. Read the private field instead.
- The `lock`, `formatter`, `textOut` and `charOut` fields, the `SharedSecrets` static block, and the
  `InterruptedIOException` handling (no threads here).
- `System.out`, `System.err` and `System.setOut`: no runtime code may touch them.
- Do not edit any other runtime file (not `FilterOutputStream.java`, `OutputStream.java`, `String.java`, `Charset.java`
  or `javaw/runtime/*`).

## JDK behaviour to match

Every method below reads the inherited field `out` again each time (a subclass may assign it). "Catch `IOException`"
means `catch (IOException x) { trouble = true; }`. Catch **only** `IOException`: a `RuntimeException` from `out` (the
test throws `IllegalStateException`) must propagate unchanged and leave `trouble` alone.

Constructors:

- `requireNonNull(obj, message)`: `if (obj == null) throw new NullPointerException(message); return obj;`.
- `PrintStream(OutputStream out, boolean autoFlush)`: `super(requireNonNull(out, "Null output stream"));`, then
  `this.autoFlush = autoFlush;` and `this.charset = out instanceof PrintStream ? ((PrintStream) out).charset :
  Charset.defaultCharset();`. So a `PrintStream` that wraps another `PrintStream` (through the one- or two-argument
  constructor) encodes with the inner stream's charset; otherwise the charset is UTF-8.
- `PrintStream(OutputStream out, boolean autoFlush, String encoding)`:
  `this(requireNonNull(out, "Null output stream"), autoFlush, toCharset(encoding));`. The `out` check comes first.
- `toCharset(csn)`: `requireNonNull(csn, "charsetName");` then `return Charset.forName(csn);`, catching
  `IllegalCharsetNameException | UnsupportedCharsetException` and throwing `new UnsupportedEncodingException(csn)`.
  Examples (from JDK 21): `new PrintStream(null, true, "UTF-8")` -> `NullPointerException: Null output stream`;
  `new PrintStream(b, true, (String) null)` -> `NullPointerException: charsetName`; `"x-nope"` ->
  `UnsupportedEncodingException: x-nope`; `"bad name!"` -> `UnsupportedEncodingException: bad name!`; `""` ->
  `UnsupportedEncodingException` with the empty message. Aliases work: `"utf-16le"`, `"latin1"`, `"UTF8"`.
- `PrintStream(OutputStream out, boolean autoFlush, Charset charset)`: `super(out);` (**no** null check of `out`:
  a stream with `out == null` behaves like a closed one), `this.autoFlush = autoFlush;`,
  `this.charset = requireNonNull(charset, "charset");` (`NullPointerException: charset`).

Raw bytes (no encoding):

- `ensureOpen()`: `if (out == null) throw new IOException("Stream closed");`.
- `write(int b)`: `try { ensureOpen(); out.write(b); if (b == '\n' && autoFlush) out.flush(); }` catch
  `IOException`. The test compares the **int** with `'\n'`: `write(0x10a)` writes byte `0x0a` but does not flush.
- `write(byte[] buf, int off, int len)`: `try { ensureOpen(); out.write(buf, off, len); if (autoFlush) out.flush(); }`
  catch `IOException`. **No range or null check of its own**: `out.write` is called even for `len == 0`, and a bad
  range throws the `IndexOutOfBoundsException` of the underlying stream (not caught). With a
  `ByteArrayOutputStream` and a 3-byte array, `write(b, -1, 1)` gives `Range [-1, -1 + 1) out of bounds for length 3`,
  `write(b, 2, 2)` gives `Range [2, 2 + 2) out of bounds for length 3`, `write(b, 0, -1)` gives
  `Range [0, 0 + -1) out of bounds for length 3`; over a `FilterOutputStream`, `write(b, 1, 5)` gives
  `Range [1, 1 + 5) out of bounds for length 3`. The existing runtime `ByteArrayOutputStream` and
  `FilterOutputStream` already throw exactly these messages; PrintStream only passes the call on.
- `write(byte[] buf) throws IOException` and `writeBytes(byte[] buf)`: both `this.write(buf, 0, buf.length);`
  (the overridable three-argument form). `buf.length` is read first, so `null` throws `NullPointerException` even
  after `close()`.

The encoder. The JDK sends every piece of text through an `OutputStreamWriter` whose encoder writes the encoded
bytes back into **this** stream's `write(byte[], int, int)` (the overridable method, so a subclass sees them and
autoflush applies). Reproduce that with two helpers:

- `encode(String s)`: `int n = s.length();` (first, so `null` throws `NullPointerException`). If `haveLeftover`,
  prepend the held char: `s = String.valueOf(leftover) + s; n++; haveLeftover = false;`. Then, if `n > 0` and the
  **last** char is a high surrogate (`Character.isHighSurrogate`), hold it back: `leftover = s.charAt(n - 1);
  haveLeftover = true; s = s.substring(0, n - 1);`. Finally, if `s` is not empty, `emit(s.getBytes(charset));`.
  An empty string (or one that is only a held surrogate) makes **no** write call.
- `emit(byte[] bytes)`: for the charset named `UTF-16` only (`"UTF-16".equals(charset.name())`), the runtime's
  `getBytes` puts the byte order mark `FE FF` in front of every call, while the JDK writes it once per stream: so
  `if (marked) bytes = Arrays.copyOfRange(bytes, 2, bytes.length); marked = true;`. Then `write(bytes, 0, bytes.length);`.
- So `print('\ud83d'); print('\ude00')` writes nothing and then the 4 bytes `F0 9F 98 80` in one call;
  `print('\ud83d'); print('z')` writes `?z` (`3F 7A`) in one call; a lone low surrogate is `?`; raw `write(int)` and
  `write(byte[], ...)` calls do not touch the held char; `flush()` keeps it. In `US-ASCII` and `ISO-8859-1` an
  unmappable char is `?` and a surrogate pair is a single `?`; in `UTF-16BE`/`UTF-16LE`/`UTF-16` a lone surrogate is
  `FF FD` / `FD FF`. The runtime `String.getBytes(Charset)` already does all of that.

Text output (each helper is the JDK private method of the same purpose):

- `writeText(String s)`: `try { ensureOpen(); encode(s); if (autoFlush && s.indexOf('\n') >= 0) out.flush(); }`
  catch `IOException`.
- `writeText(char[] buf)`: the same with `String s = new String(buf);` **inside** the `try`, after `ensureOpen()`.
  So `print((char[]) null)` throws `NullPointerException` on an open stream but after `close()` only sets the
  error flag (unlike `PrintWriter`).
- `writeLine(String s)`: `try { ensureOpen(); encode(s + "\n"); if (autoFlush) out.flush(); }` catch `IOException`.
  The text and the line end go out in **one** write. `writeLine(char[] buf)`: the same with
  `encode(new String(buf) + "\n")` inside the `try`.
- `newLine()`: `try { ensureOpen(); encode("\n"); if (autoFlush) out.flush(); }` catch `IOException`. The line end
  is always `"\n"` (what JDK 21 writes on macOS and Linux; `System.lineSeparator()` does not exist in the runtime).
- autoflush: with `autoFlush` on, every non-empty text write flushes once inside `write(byte[], int, int)`, and
  `writeText` flushes a second time when the text contains `'\n'`, `writeLine` and `newLine` always. So
  `print("a")` -> `a(a,0,1)F;`, `print("b\nc")` -> `a(b/c,0,3)F;F;`, `println("e")` -> `a(e/,0,2)F;F;`.

print and println:

- `print(boolean/int/long/float/double x)`: `writeText(String.valueOf(x));`. `print(char c)`:
  `writeText(String.valueOf(c));` (**not** `write(int)`). `print(char[] s)`: `writeText(s);`.
  `print(String s)`: `writeText(String.valueOf(s));` (`null` prints `null`). `print(Object obj)`:
  `writeText(String.valueOf(obj));`, so an object whose `toString()` returns `null` throws `NullPointerException`
  from `encode` (the test checks it).
- `println()`: `newLine();`.
- `println(x)` for `boolean`, `char`, `int`, `long`, `float`, `double`, `char[]`, `String`:
  `if (getClass() == PrintStream.class) writeLine(String.valueOf(x)); else { print(x); newLine(); }` (for `char[]`
  call `writeLine(x)` with the array). A plain `PrintStream` writes `x` and the line end in one call; a subclass
  sees its own `print(x)` and then a separate write of `\n`.
- `println(Object x)`: `String s = String.valueOf(x);` then `if (getClass() == PrintStream.class)
  writeLine(String.valueOf(s)); else { print(s); newLine(); }`: a subclass sees `print(String)`, never
  `print(Object)`, and a `toString()` of `null` prints `null` here.

format, printf and append:

- `format(String format, Object... args)`: `try { ensureOpen(); append(String.format(format, args)); }` catch
  `IOException`; `return this;`. `format(Locale l, ...)` is the same with `String.format(l, format, args)` (`l` may be
  `null`). `printf(...)` is `return format(...);` with the same arguments. There is no explicit autoflush in format:
  the flushes come from the text writes. A bad format throws the `IllegalFormatException` subclass (or
  `NullPointerException` for a `null` format) from `String.format`, not caught; after `close()` the format string
  is not looked at (`printf("%q")` only sets the error flag).
- `append(CharSequence csq)`: `print(String.valueOf(csq)); return this;`.
  `append(CharSequence csq, int start, int end)`: `if (csq == null) csq = "null"; return append(csq.subSequence(start, end));`.
  `append(char c)`: `print(c); return this;`. `append("abc", 2, 1)` throws `StringIndexOutOfBoundsException` from
  `subSequence` (not caught).

flush, close and the error flag:

- `flush()`: `try { ensureOpen(); out.flush(); }` catch `IOException`.
- `close()` (the JDK closes its writer chain, which calls back into this stream):

      if (closing) return;
      closing = true;
      try {
          if (haveLeftover) { haveLeftover = false; emit(String.valueOf(leftover).getBytes(charset)); }
          flush();
          close();
          out.close();
      } catch (IOException x) {
          trouble = true;
      }
      out = null;

  `flush()` and the inner `close()` are the overridable methods (a subclass logs `cl;fl;cl;`); the inner `close()`
  returns at once because `closing` is set. A held high surrogate is encoded on its own first,
  `String.valueOf(leftover).getBytes(charset)` (that gives `?` in UTF-8, the charset the fixture uses, and `FF FD` in
  UTF-16BE). Do not hard-code `"?"`. The underlying stream sees
  `F;C;`. When `out.close()` throws `IOException`, the flag is set and `out` still becomes `null`; a second `close()`
  does nothing at all. With `out == null` from the start, `flush()` sets the flag and `out.close()` throws
  `NullPointerException` out of `close()`; a second `close()` then returns quietly.
- `checkError()`: `if (out != null) flush();` (the overridable `flush()`), then
  `if (out instanceof PrintStream) return ((PrintStream) out).checkError();` (the wrapped stream decides), else
  `return trouble;`. It does not reset the flag. After a clean `close()` it returns `false`.
- `setError()`: `trouble = true;`. `clearError()`: `trouble = false;`.

Call patterns the fixture records (`Recorder` logs the underlying stream: `w(x)` = `write(int)`,
`a(text,off,len)` = `write(byte[], int, int)`, `F;` = `flush()`, `C;` = `close()`, newline shown as `/`, other
non-ASCII bytes as `<hex>`; `Logging` is a subclass logging its overridden methods):

- plain `PrintStream`, no autoflush: `print("ab")` -> `a(ab,0,2)`; `print("")` -> nothing; `println("ln")` ->
  `a(ln/,0,3)`; `println(1.5)` -> `a(1.5/,0,4)`; `flush()` -> `F;`; `checkError()` -> `F;`; `close(); close();` ->
  `F;C;` once.
- `Logging`: `print("ab")` -> `ps(ab)wa(0,2)`; `print(true)` -> `wa(0,4)`; `print('c')` -> `pc(c)wa(0,1)`;
  `print(new char[]{'d','e'})` -> `pa(2)wa(0,2)`; `println("s")` -> `ps(s)wa(0,1)wa(0,1)`;
  `println(new Named("p"))` -> `ps(<p>)wa(0,3)wa(0,1)`; `append("xyz", 0, 2)` -> `ap(xy)ps(xy)wa(0,2)`;
  `append('q')` -> `pc(q)wa(0,1)`; `write(new byte[]{'E','F'})` -> `wb(2)wa(0,2)`; `checkError()` -> `fl;`;
  `close()` -> `cl;fl;cl;` (and `wa(0,1)` before `fl;` when a high surrogate is held).
- format: `printf("%d", 5)` with autoflush -> `a(5,0,1)F;`; `printf("%n")` -> `a(/,0,1)F;F;`;
  `Logging.printf("%s", "t")` -> `ap(t)ps(t)wa(0,1)`.

Known differences (the fixtures do not observe them; record them in the docs):

- Write chunking: the JDK encodes through an 8192-char `BufferedWriter` and a byte buffer of at most 8192 bytes, so a
  text of more than 8192 chars or encoded bytes reaches the stream in several `write(byte[], int, int)` calls. Here
  each text write is one call. The bytes are the same.
- Formatting: the JDK's `Formatter` first parses the whole format string (an unknown conversion such as `%q` throws
  before anything is written), then appends each literal piece through `append(CharSequence, int, int)` and each
  converted argument through `append(CharSequence)`. That is one write per piece, each flushed with autoflush, and an
  argument that fails to convert (`printf("ab%d", "x")`) leaves the earlier pieces (`ab`) written. Here the whole
  `String.format` result is one `append(CharSequence)`, and a failing format writes nothing. Formats with one piece
  look the same; an empty result still calls `append("")` here, while the JDK calls nothing (no bytes are written
  either way).

## Steps

1. Read the test `javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodePrintStreamTest.java`.
2. Read `javaw-bytecode/src/main/resources/exception-runtime/java/io/FilterOutputStream.java` (the superclass; do
   not edit it) and `java/io/PrintWriter.java` (the same error-flag style; copy its shape).
3. Create `javaw-bytecode/src/main/resources/exception-runtime/java/io/PrintStream.java` with the members above.
4. Register it: `javaw-agent-kit/bin/kit register java/io/PrintStream.java`. It adds a
   `paths.add("java/io/PrintStream.java");` line before the `ExceptionSupport` line of
   `BytecodeExceptionRuntime.java`; do not edit that file by hand.
5. `javaw-agent-kit/bin/kit check T36` until PASS.
6. Write `docs/bytecode-print-stream.md` (below), the report, then `javaw-agent-kit/bin/kit finish T36`.

## Hints

- The whole class is about 165 lines; most methods are one line. `IOException`, `UnsupportedEncodingException` and
  `FilterOutputStream` are in `java.io` (no import). `String.format(String, Object...)`,
  `String.format(Locale, String, Object...)`, `String.getBytes(Charset)`, `Charset.forName`, `Charset.name()`,
  `Charset.defaultCharset()` (UTF-8), `Character.isHighSurrogate` and `Arrays.copyOfRange(byte[], int, int)` exist
  in the runtime.
- `FilterOutputStream` declares `write(int)`, `write(byte[], int, int)`, `flush()` and `close()` with
  `throws IOException`. PrintStream overrides them **without** `throws`, so fixture code calls them outside `try`.
  `write(byte[])` is the exception: it keeps `throws IOException` (Java 14+). If javac reports
  `unreported exception IOException` inside PrintStream, a call on `out` is outside the `try`.
- Calls on `out` can throw `IOException`; calls on `this` cannot. `emit` must call `write(bytes, 0, bytes.length)`
  on **this** (the overridable method), never `out.write(...)`: the test's subclass must see `wa(0,n)` and the
  autoflush flush must happen.
- Never write text with `write(int)` or a per-char loop: one text call is one `write(byte[], 0, n)` call with
  `off == 0`. Do not use `String.getBytes()` without a charset (the stream charset may be `UTF-16BE`).
- `getClass() == PrintStream.class` works in the runtime. Do not replace it with `instanceof`: every object is an
  instance, so subclasses would lose their separate `print` + `newLine` calls (`subclassCalls`).
- `requireNonNull` must be a private static helper in PrintStream (the message is part of the test). Calling it
  inside `super(...)` and `this(...)` arguments is legal Java.
- Do not write the words `synchronized`, `volatile`, `native` or `Thread` (capital T) anywhere in the file, and no
  `System.` other than `System.arraycopy`, not even in comments; it is the simplest way to stay clear of `kit guard`.
- The Wasm runner stops at the **first** failing fixture of each `@Test` method, so after a fix a different fixture
  of the same group may fail next. That is progress, not a regression.
- Before the class exists, every `matchesJvm` test fails with a message naming `java/io/PrintStream`: the class is
  missing or not registered. All tests share one fixture, so one missing member fails all six.
- Mutations of the reference that the test caught, with the fixtures that failed: `emit` calling `out.write`
  (`printNulls`, `formatCalls`, `subclassCalls`, `flushErrors`); `println(x)` always doing `print(x); newLine();`
  (`surrogates`, `underlyingCalls`: `a(ln,0,2)a(/,0,1)` instead of `a(ln/,0,3)`); no held surrogate (`surrogates`,
  `autoFlush`, `failingClose`); the byte order mark on every write (`utf16Mark`); `new String(buf)` before
  `ensureOpen()` (`afterClose`); `out.flush()` instead of `flush(); close();` in `close()` (`subclassCalls`,
  `failingClose`); `out = null` only when `out.close()` succeeds (`failingClose`); `print(char)` calling
  `write(int)` (`printPrimitives`, `appendForms`, `underlyingCalls`, `failingClose`); catching `Exception` in
  `write(byte[], int, int)` (`writeRanges`, `runtimeFailures`).
- The `formatForms` and `formatFailures` outputs (`Locale.GERMANY` grouping, `%b`/`%s` with `null`, `%c`, `%2$s`,
  `%n`, and the `UnknownFormatConversionException`, `IllegalFormatConversionException` and
  `MissingFormatArgumentException` types) all come from the runtime `String.format`, which already matches the JVM
  for every fixture call. If one differs, the bug is in how PrintStream calls `String.format` (wrong overload or
  arguments); do not edit `String.java` or `TextFormats`.
- The runtime `String.format` now formats a `%s` whose `toString()` returns `null` like the JDK: plain `%s` prints
  `null`, and a width, a precision or `%S` throws `NullPointerException`. The fixture does not format such an object;
  nothing in PrintStream needs to handle it.
- Calibration: the reference passed kit check after 2 runs (about 165 new lines); `kit guard` was clean. The first
  run failed only because the fixture formatted an object whose `toString()` is `null` (the runtime then crashed on
  it; it now matches the JDK); that call was removed.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
constructorsAndCharsets, printAndPrintln, bytesAndAppend, formatting, callPatterns, errorsAndClose: if the message names `java/io/PrintStream` (`link a complete acyclic type hierarchy`, `link the descriptor class/interface` or `unsupported ... java/io/PrintStream.<name>`), the class is missing, not registered, or lacks that member: run `javaw-agent-kit/bin/kit register java/io/PrintStream.java` once and give the class a public method with exactly that name, parameters and return type (printf/format/append return PrintStream); if it says `Cannot build bytecode exception runtime`, javac rejected PrintStream.java: fix the quoted line (`unreported exception IOException` means a call on `out` is outside the try).
constructors: the messages come from `requireNonNull`: `Null output stream` for a null out in the one-, two- and String-argument constructors (checked before the charset name), `charsetName` for a null name, `charset` for a null Charset; an unknown or illegal name throws `new UnsupportedEncodingException(csn)`; the Charset constructor does not check out, and a stream with out == null sets the error flag on print.
charsets, inheritedCharset: text is encoded with `s.getBytes(charset)`; the one- and two-argument constructors use `((PrintStream) out).charset` when out is a PrintStream and `Charset.defaultCharset()` (UTF-8) otherwise; the Charset constructor always uses its argument.
utf16Mark: for the charset named UTF-16 only the first emitted text keeps the FE FF mark: in emit, `if (marked) bytes = Arrays.copyOfRange(bytes, 2, bytes.length); marked = true;`; raw write(int) bytes do not set marked.
surrogates, autoFlush: encode(s) prepends a held high surrogate and holds back a trailing one (`haveLeftover`/`leftover`), so print('\ud83d') writes nothing and the next text write carries the pair; flush() keeps the held char and close() writes it as `String.valueOf(leftover).getBytes(charset)` (`?` in UTF-8; never hard-code "?") before flushing; print(char) must go through writeText(String.valueOf(c)), never write(int).
printPrimitives, printlnForms, longText: print(x) is `writeText(String.valueOf(x))` and println(x) on a plain PrintStream is `writeLine(String.valueOf(x))`, which encodes `s + "\n"` (the line end is the constant "\n", never System.lineSeparator()); each text write is one `write(bytes, 0, bytes.length)` on this.
printNulls, afterClose: writeText(char[]) and writeLine(char[]) build `new String(buf)` inside the try after ensureOpen(), so a null array throws NullPointerException only while open; print(Object) with a null toString() throws NullPointerException from encode (s.length() first); println(Object) uses `String.valueOf(String.valueOf(x))` and prints `null`.
writeForms, writeRanges: write(byte[], off, len) is `try { ensureOpen(); out.write(buf, off, len); if (autoFlush) out.flush(); } catch (IOException x) { trouble = true; }` with no range or null check (the underlying stream throws); write(byte[]) and writeBytes(byte[]) are `this.write(buf, 0, buf.length)`; write(int) passes the int to out.write unchanged.
appendForms, formatCalls: append(CharSequence) is `print(String.valueOf(csq)); return this;`, append(csq, start, end) is `if (csq == null) csq = "null"; return append(csq.subSequence(start, end));`, append(char) is `print(c); return this;`; format is `try { ensureOpen(); append(String.format(...)); } catch (IOException x) { trouble = true; } return this;` and printf returns format(...).
formatForms, formatFailures: format(fmt, args) uses `String.format(fmt, args)` and format(l, fmt, args) uses `String.format(l, fmt, args)`; do not catch IllegalFormatException or NullPointerException.
underlyingCalls, subclassCalls: println(x) is `if (getClass() == PrintStream.class) writeLine(String.valueOf(x)); else { print(x); newLine(); }` (println(Object) passes the String s to print); emit must call this.write(bytes, 0, n), not out.write; close() must call the overridable flush() and close() before out.close().
autoFlush: write(int) flushes only when `b == '\n'` (the int, so 0x10a does not); write(byte[], off, len) always flushes with autoFlush; writeText flushes again when the text contains '\n', writeLine and newLine always flush again; with autoFlush off nothing flushes until flush().
replacedOut, nestedCheckError: every method reads the protected field out each time; checkError() is `if (out != null) flush(); if (out instanceof PrintStream) return ((PrintStream) out).checkError(); return trouble;` and never clears the flag.
writeErrors, flushErrors, runtimeFailures: every call on out sits in `try { ensureOpen(); ... } catch (IOException x) { trouble = true; }`; catch only IOException, so an IllegalStateException from the underlying stream propagates unchanged and leaves the flag false.
failingClose: close() is `if (closing) return; closing = true; try { [held surrogate] flush(); close(); out.close(); } catch (IOException x) { trouble = true; } out = null;`: out becomes null even when out.close() throws, a second close() does nothing, and with out == null from the start the NullPointerException of out.close() propagates.
leavesOutFileNameConstructor: PrintStream must not declare a `PrintStream(String)` constructor (or any constructor other than the four OutputStream ones).
```

## Docs

Create `docs/bytecode-print-stream.md` with this skeleton (fill in the brackets):

    # Bytecode PrintStream

    Revision `print-stream-vN` links `java.io.PrintStream` over an `OutputStream` in the bundled exception runtime
    (single-threaded contract: no lock and no monitors). `System.out` and `System.err` are not linked.

    Contract:

    - <constructors and their null/charset failures, the inherited charset of a wrapped PrintStream, raw byte writes
      (no range check of their own), text encoding through the overridable write(byte[], int, int) with a held high
      surrogate and a single UTF-16 mark, print/println forms (one write per println on a plain PrintStream,
      print + newline for subclasses), the "\n" line separator, format/printf through String.format, append forms,
      autoflush, the error flag (IOException swallowing, checkError, setError/clearError, wrapped PrintStream),
      close order and writes after close>
    - <limits: only the OutputStream constructors; no charset() (Java 18); each text write is one write call where
      the JDK splits texts over 8192 chars or bytes; a format is one append where the JDK appends each piece>

    Evidence: `BytecodePrintStreamTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
    fresh instances), including a subclass and an underlying stream that record every call. `PrintStream(String)` is
    rejected before WAT generation.
