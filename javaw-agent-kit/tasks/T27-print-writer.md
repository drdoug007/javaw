# T27 PrintWriter

```kit
id: T27
slug: print-writer
title: link java.io.PrintWriter over a Writer
tests: BytecodePrintWriterTest
depends: T26
allow: javaw-bytecode/src/main/resources/exception-runtime/java/io/PrintWriter.java
allow: docs/bytecode-print-writer.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-print-writer-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodePrintWriterTest.java`. The fixture uses
`java.io.StringWriter` from T26, so T26 must be done first.

## Goal

Add `java.io.PrintWriter` to the bundled runtime, for the case where it wraps another `Writer`. The pinned libraries
name `PrintWriter` from 99 classes (`runtime-target/target/audit/external-types.tsv`: logback
`ViewStatusMessagesServletBase`, HikariCP `HikariDataSource` and `DriverDataSource`, the Jakarta Servlet
`ServletResponse` family, Tomcat `CoyoteWriter`, H2 `TraceSystem`, `Recover`, `MVStoreTool`, `JdbcDataSource`, JAXB
`JAXBException`, ...). The member rows (`member-references.tsv`) are `println(String)` 256 call sites,
`print(String)` 207, `append(char)` 117, `write(String)` 114, `println()` 50, `append(CharSequence)` 50,
`<init>(Writer)` 48, `printf(String, Object...)` 47, `print(char)` 37, `flush()` 37, `close()` 32,
`print(Object)` 31, `write(int)` 9, `<init>(Writer, boolean)` 7, `print(int)` 6, `write(char[], int, int)` 5,
`println(Object)` 5, `write(String, int, int)` 4, `println(char)` 4, and the other `print`/`println` overloads 3
each. The left-out `<init>(OutputStream)` (12 call sites) and `<init>(OutputStream, boolean)` (4) stay unlinked
(see "Members to leave out"). Tomcat's
`CoyoteWriter` and Servlet's `HttpServlet$NoBodyPrintWriter` subclass it.

The JDK `PrintWriter` uses `synchronized (lock)` blocks and an `InternalLock`. Here the runtime is
**single-threaded**: write it with no `synchronized` (RULES.md). Leave `Writer.java` unchanged; it already keeps
the `protected Object lock` field without entering monitors.

## Members to implement

`java/io/PrintWriter.java` (`package java.io;`, `import java.util.Locale;`): `public class PrintWriter extends Writer`

Fields:

- `protected Writer out;` (subclasses read and assign it; the test does)
- `private final boolean autoFlush;`
- `private boolean trouble;`
- `private static final String LINE_SEPARATOR = "\n";` (see "Line separator" below)

Constructors:

- `public PrintWriter(Writer out)`: `this(out, false)`
- `public PrintWriter(Writer out, boolean autoFlush)`: `super(out); this.out = out; this.autoFlush = autoFlush;`

Methods (only the private `ensureOpen()` declares `throws IOException`; every public and protected method declares no
`throws`):

- `private void ensureOpen() throws IOException`
- `public void flush()`, `public void close()`, `public boolean checkError()`
- `protected void setError()`, `protected void clearError()`
- `public void write(int c)`, `public void write(char[] buf, int off, int len)`, `public void write(char[] buf)`,
  `public void write(String s, int off, int len)`, `public void write(String s)`
- `private void newLine()`
- `public void print(boolean b)`, `print(char c)`, `print(int i)`, `print(long l)`, `print(float f)`,
  `print(double d)`, `print(char[] s)`, `print(String s)`, `print(Object obj)` (all `public void`)
- `public void println()` and `public void println(x)` for the same nine parameter types:
  `boolean`, `char`, `int`, `long`, `float`, `double`, `char[]`, `String`, `Object`
- `public PrintWriter printf(String format, Object... args)`,
  `public PrintWriter printf(Locale l, String format, Object... args)`
- `public PrintWriter format(String format, Object... args)`,
  `public PrintWriter format(Locale l, String format, Object... args)`
- `public PrintWriter append(CharSequence csq)`, `public PrintWriter append(CharSequence csq, int start, int end)`,
  `public PrintWriter append(char c)` (the return type is `PrintWriter`, not `Writer`)

## Members to leave out

- Every constructor that does not take a `Writer`: `PrintWriter(OutputStream)`, `(OutputStream, boolean)`,
  `(OutputStream, boolean, Charset)`, `(String fileName)`, `(String, String)`, `(String, Charset)`, `(File)`,
  `(File, String)`, `(File, Charset)`. The runtime has no `OutputStreamWriter`, `BufferedWriter`,
  `FileOutputStream` or `File`. `java.nio.charset.Charset` does exist, but every `Charset` constructor also needs
  one of those missing classes, so leave those constructors out too. The test checks that `PrintWriter(String)`
  stays unlinked.
- The `psOut` field (`PrintStream` is not in the runtime) and the `Formatter` field (`java.util.Formatter` is not in
  the runtime: use `String.format`, see below).
- The package-private `PrintWriter(Writer, Object lock)` constructor and the `SharedSecrets` static block.
- Do not edit any other runtime file (not `Writer.java`, `String.java` or `javaw/runtime/TextFormats.java`).

## JDK behaviour to match

Every method below reads the field `out` again each time (a subclass may assign it). "Catch `IOException`" means
`catch (IOException x) { trouble = true; }`. Catch **only** `IOException`: a `RuntimeException` from `out` (the test
throws `IllegalStateException`) must propagate unchanged and leave `trouble` alone. The JDK also catches
`InterruptedIOException` to re-interrupt the thread; leave that out (no threads here).

- `PrintWriter(Writer out, boolean autoFlush)`: `super(out)` makes `lock == out`; the runtime `Writer(Object)`
  throws `NullPointerException` for `null`, so `new PrintWriter((Writer) null)` throws it (the test checks the type).
  `lock` is set once by `super(out)`; assigning `out` later does not change it (in `replacedOut`, `lock == out` is
  `false` after a subclass assigns another writer to `out`).
- `ensureOpen()`: `if (out == null) throw new IOException("Stream closed");`. Every write, print, println, format,
  printf and `flush` calls it inside its `try`, so after `close()` they do nothing except set `trouble`.
- `write(int c)`: `try { ensureOpen(); out.write(c); }` catch `IOException`.
- `write(char[] buf, int off, int len)`: `try { ensureOpen(); out.write(buf, off, len); }` catch `IOException`.
  **No range check of its own**: a bad range throws the `IndexOutOfBoundsException` of the underlying writer
  and it is not caught. The test compares the full messages, all from T26's `StringWriter`: `write(b, -1, 1)` gives
  `Range [-1, -1 + 1) out of bounds for length 3`, `write(b, 2, 2)` gives `Range [2, 2 + 2) out of bounds for length 3`
  and `write(b, 0, -1)` gives `Range [0, 0 + -1) out of bounds for length 3` (with `b` of length 3). Likewise
  `write((char[]) null, 0, 0)` throws `NullPointerException` from the underlying `StringWriter`; do not add a null
  check.
- `write(String s, int off, int len)`: `try { ensureOpen(); out.write(s, off, len); }` catch `IOException`. No
  range check, no null check (`StringWriter` writes `ul` for `write((String) null, 1, 2)`). For `write("abc", 2, 5)`
  and `write("abc", -1, 1)` the test compares only the exception type, `IndexOutOfBoundsException`.
- `write(char[] buf)`: `write(buf, 0, buf.length);` and `write(String s)`: `write(s, 0, s.length());`. Both read
  the length **before** `ensureOpen`, so a `null` argument throws `NullPointerException` even after `close()`.
- `newLine()` (private, used by `println()`): `try { ensureOpen(); out.write(LINE_SEPARATOR); if (autoFlush)
  out.flush(); }` catch `IOException`. It calls `out.write(String)` on the **underlying** writer, never
  `this.write`, so a `PrintWriter` subclass does not see the line separator in its `write` overrides.
- `print(boolean b)`, `print(int)`, `print(long)`, `print(float)`, `print(double)`: `write(String.valueOf(x));`.
  `print(char c)`: `write(c);` (calls `write(int)`). `print(char[] s)`: `write(s);` (calls `write(char[])`; `null`
  throws `NullPointerException`). `print(String s)` and `print(Object obj)`: `write(String.valueOf(s));` (`null`
  prints `null`).
- `println()`: `newLine();`. `println(x)` for `boolean`, `char`, `int`, `long`, `float`, `double`, `char[]`,
  `String`: `print(x); println();` (both overridable calls). `println(Object x)`: `String s = String.valueOf(x);
  print(s); println();`: it calls `print(String)`, **not** `print(Object)`.
- `format(String format, Object... args)`: `try { ensureOpen(); write(String.format(format, args)); if (autoFlush)
  out.flush(); }` catch `IOException`; `return this;`. `format(Locale l, String format, Object... args)` is the same
  with `String.format(l, format, args)` (`l` may be `null`). `printf(...)` is `return format(...);` with the same
  arguments. A bad format throws the `IllegalFormatException` subclass from `String.format` (not caught); after
  `close()` the format string is not even looked at (`printf("%q")` just sets the error).
- `append(CharSequence csq)`: `write(String.valueOf(csq)); return this;`.
  `append(CharSequence csq, int start, int end)`: `if (csq == null) csq = "null"; return append(csq.subSequence(start, end));`.
  So `append(null, 1, 3)` writes `ul`, and `append("abc", 2, 1)` throws `StringIndexOutOfBoundsException` from
  `String.subSequence` (not caught).
  `append(char c)`: `write(c); return this;`.
- `flush()`: `try { ensureOpen(); out.flush(); }` catch `IOException`.
- `close()`: `try { if (out != null) { out.close(); out = null; } }` catch `IOException`. So a second `close()` does
  not call `out.close()` again, and when `out.close()` throws, `out` stays set: later writes still reach it and the
  next `close()` tries again.
- `checkError()`: `if (out != null) flush();` (the overridable `flush()`), then
  `if (out instanceof PrintWriter) return ((PrintWriter) out).checkError();` (a wrapped `PrintWriter` decides,
  and this writer's own `trouble` is ignored), else `return trouble;`. It does not reset the flag.
- `setError()`: `trouble = true;`. `clearError()`: `trouble = false;`.
- autoflush: only `println(...)` (through `newLine`) and `format`/`printf` call `out.flush()`. `print`, `write` and
  `append` never flush.

Line separator: the JDK writes `System.lineSeparator()`. There is no runtime `System.java`: the compiler supports
only `System.arraycopy`, `System.identityHashCode` and the property methods (redirected to
`javaw/runtime/SystemProperties`, which has no default properties). So there is no `System.lineSeparator()`, and
`System.getProperty("line.separator")` returns `null` in Wasm. Use the constant `"\n"`, which is what JDK 21 gives
on macOS and Linux, where the tests run.

Formatting: the JDK keeps a `java.util.Formatter` over `this` and writes each literal piece and each converted
argument with a separate `append`. The runtime has no `Formatter`, so write the whole result of `String.format`
with one `write(String)`. The test only looks at the text, never at how `format` splits it. For the same reason,
the JDK can write part of a format before an argument conversion fails (`printf("ab%d","x")` writes `ab` and then
throws), while here a failing format writes nothing. The test never relies on that difference. Its formats either
fail while parsing (`"ab%q"`: the JDK parses the whole format first, so nothing is written there either) or fail at
the first specifier with no literal text before it (`"%d"`, `"%2$s"`, `"%,.2f"`, a `null` format). In
`formatFailures`, expect `<>` with no text between the brackets.

Call patterns (the fixture's `Logging` subclass and `Recorder` writer record them; `/` stands for `\n`):

- `print("ab")` -> `ps(ab)ws(ab)wso(ab,0,2)`; `print(7)` -> `pi(7)ws(7)wso(7,0,1)`; `print('c')` -> `pc(c)w(c)`;
  `print(new char[]{'d','e'})` -> `pa(2)wa(2)wao(0,2)`; `println(new Named("p"))` -> `ps(<p>)ws(<p>)wso(<p>,0,3)ln;`;
  `println(2.5)` -> `ws(2.5)wso(2.5,0,3)ln;`; `append("xyz", 0, 2)` -> `ap(xy)ws(xy)wso(xy,0,2)`;
  `checkError()` -> `fl;`; `checkError()` after `close()` -> nothing.
- On the underlying writer: `write('x')` -> `c(x)`; `write(new char[]{'y','z'})` -> `a(yz,0,2)`;
  `write("abc")` -> `s(abc,0,3)`; `print(true)` -> `s(true,0,4)`; `println("ln")` -> `s(ln,0,2)S(/)`;
  `flush()` -> `F;`; `close(); close();` -> `C;` once.
- `autoFlush` fixture, on a `Recorder` under `new PrintWriter(r, true)`: `print("a"); write('b'); write("c");
  append("d"); print(1);` -> `s(a,0,1)c(b)s(c,0,1)s(d,0,1)s(1,0,1)` (no flush); `println("e")` -> `s(e,0,1)S(/)F;`;
  `println()` -> `S(/)F;`; `println(3)` -> `s(3,0,1)S(/)F;`; `printf("%s","f")` -> `s(f,0,1)F;`;
  `format(Locale.ROOT,"g")` -> `s(g,0,1)F;`. With autoflush off, `println("h"); printf("i");
  format(Locale.ROOT,"j"); println();` -> `s(h,0,1)S(/)s(i,0,1)s(j,0,1)S(/)` and then `flush()` -> `F;`.
- `runtimeFailures`: the `Recorder` throws `IllegalStateException("boom " + w)`, where `w` names the Writer method
  that was called (`s` = `write(String,int,int)`, `S` = `write(String)`, `c` = `write(int)`). The expected messages
  are `boom s` for `print("x")`, `boom S` for `println()`, `boom s` for `printf("y")` and `boom c` for `write('z')`,
  so the call patterns above must hold; afterwards `checkError()` is `false` and its flush logs `F;`.

## Steps

1. Read the test `javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodePrintWriterTest.java`.
2. Read `javaw-bytecode/src/main/resources/exception-runtime/java/io/Writer.java` (the superclass; do not edit it)
   and `java/io/StringWriter.java` (T26, the same style).
3. Create `javaw-bytecode/src/main/resources/exception-runtime/java/io/PrintWriter.java` with the members above.
4. Register it: `javaw-agent-kit/bin/kit register java/io/PrintWriter.java`. It adds a
   `paths.add("java/io/PrintWriter.java");` line before the `ExceptionSupport` line of
   `BytecodeExceptionRuntime.java`; do not edit that file by hand.
5. `javaw-agent-kit/bin/kit check T27` until PASS.
6. Write `docs/bytecode-print-writer.md` (below), the report, then `javaw-agent-kit/bin/kit finish T27`.

## Hints

- The whole class is about 75 lines; most methods are one line. `IOException` is in `java.io` (no import);
  `Locale` needs `import java.util.Locale;`. `String.format(String, Object...)` and
  `String.format(Locale, String, Object...)` exist in the runtime `String`.
- `Writer` declares `write(int)`, `write(char[])`, `write(String)`, `write(String, int, int)`, `append(...)` and
  `flush()`/`close()` with `throws IOException`. PrintWriter overrides **all** of them without `throws`, so fixture
  code can call them outside `try`. If javac reports `unreported exception IOException` inside PrintWriter, a call
  goes to a `Writer` method you did not override, or a call on `out` is outside the `try`.
- Calls on `out` (`out.write`, `out.flush`, `out.close`) can throw `IOException`; calls on `this` cannot. Keep that
  split: `write(String s)` is `write(s, 0, s.length())` on `this`, while `newLine()` calls `out.write(LINE_SEPARATOR)`.
- The covariant `append` methods must be declared to return `PrintWriter`; javac adds the `Writer`/`Appendable`
  bridges. `append(csq, start, end)` must call `append(...)` on `this`, not `write`.
- `checkError()` uses `instanceof PrintWriter` on `out`; write it with a cast (`((PrintWriter) out).checkError()`).
- Do not write the words `synchronized`, `volatile`, `native` or `Thread` (capital T) anywhere in the file, not
  even in comments; it is the simplest way to stay clear of `kit guard`.
- The Wasm runner stops at the **first** failing fixture of each `@Test` method, so after a fix a different fixture
  of the same group may fail next. That is progress, not a regression.
- Before the class exists, every `matchesJvm` test fails with a message naming `java/io/PrintWriter`: the class is
  missing or not registered. All tests share one fixture, so one missing member fails all six.
- `System.getProperty("line.separator")` is `null` in Wasm (no default properties), so writing it prints `null`
  and the Recorder fails with `NullPointerException`. Use the `"\n"` constant.
- Mutations of the reference that the test caught, with the fixture that fails first: `write(String s)` calling
  `out.write(s)` directly (`autoFlush`, `underlyingCalls`, `writeErrors`, `afterClose`: the Recorder logs `S(a)`
  instead of `s(a,0,1)`, and `write((String) null)` stops throwing after close); `println(Object)` calling
  `print(Object)` (`subclassCalls`: `po(<p>)` instead of `ps(<p>)`); catching `Exception` or `RuntimeException`
  (`writeRanges`, `runtimeFailures`); no wrapped-`PrintWriter` branch in `checkError` (`nestedCheckError`); setting
  `out = null` in a `finally` of `close` (`failingClose`); `format` without the autoflush (`autoFlush`).
- Calibration: the reference passed kit check after 1 run (about 75 new lines); `kit guard` was clean.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
constructorsAndPrint, writesAndAppends, formatting, callPatterns, errorFlag, closing: if the message names `java/io/PrintWriter` (`link a complete acyclic type hierarchy`, `link the descriptor class/interface` or `unsupported ... java/io/PrintWriter.<name>`), the class is missing, not registered, or lacks that member: run `javaw-agent-kit/bin/kit register java/io/PrintWriter.java` once and give the class a public method with exactly that name, parameters and return type (printf/format/append return PrintWriter); if it says `Cannot build bytecode exception runtime`, javac rejected PrintWriter.java: fix the quoted line (`unreported exception IOException` means a call to `out` is outside the try or a Writer method is not overridden).
constructors: the constructors are `this(out, false)` and `super(out); this.out = out; this.autoFlush = autoFlush;` (super(out) sets lock = out and throws NullPointerException for null); checkError() of a fresh writer is false.
printPrimitives, printNulls, writeForms: print(x) for boolean/int/long/float/double/String/Object is `write(String.valueOf(x))` (null prints `null`), print(char) is `write(c)`, print(char[]) is `write(s)`; write(char[]) is `write(buf, 0, buf.length)` and write(String) is `write(s, 0, s.length())`, both outside any try, so a null array or string throws NullPointerException.
printlnForms, printNulls: println() writes exactly "\n" (`out.write(LINE_SEPARATOR)` with `LINE_SEPARATOR = "\n"`, never System.lineSeparator() or System.getProperty); println(x) is `print(x); println();`.
writeRanges: PrintWriter.write(char[], int, int) and write(String, int, int) must not check the range or catch the exception: `out.write(buf, off, len)` inside `try { ... } catch (IOException x) { trouble = true; }` lets the IndexOutOfBoundsException of StringWriter through.
appendForms: append(CharSequence) is `write(String.valueOf(csq)); return this;`, append(csq, start, end) is `if (csq == null) csq = "null"; return append(csq.subSequence(start, end));`, append(char) is `write(c); return this;`; all three return type PrintWriter.
formatForms, formatFailures: format(fmt, args) is `try { ensureOpen(); write(String.format(fmt, args)); if (autoFlush) out.flush(); } catch (IOException x) { trouble = true; } return this;`, format(l, fmt, args) uses `String.format(l, fmt, args)`, and printf(...) returns format(...) with the same arguments; do not catch IllegalFormatException or NullPointerException.
autoFlush: only newLine() (so every println) and format/printf call `out.flush()` when autoFlush is true; print, write and append never flush, and with autoFlush false nothing flushes until flush() is called.
autoFlush, underlyingCalls, writeErrors, afterClose: if the Recorder shows `S(a)` where `s(a,0,1)` is expected, PrintWriter.write(String s) calls out.write(s) directly: it must be `write(s, 0, s.length());` on this, outside any try (so `write((String) null)` throws NullPointerException even after close); only newLine() calls out.write(String).
underlyingCalls: write(int) calls `out.write(c)`, write(char[], off, len) calls `out.write(buf, off, len)`, write(String, off, len) calls `out.write(s, off, len)`, and println() calls `out.write("\n")` (the one-argument String form, logged `S(/)`); close() calls out.close() once and sets out = null, so the second close() does nothing.
subclassCalls: print(String)/print(Object)/print(int)... call write(String) on this, which calls write(s, 0, s.length()); print(char) calls write(int); print(char[]) calls write(char[]), which calls write(buf, 0, buf.length); println(Object) calls print(String) (logged `ps(<p>)`, never `po`); println(x) calls print(x) then println(); append(csq, start, end) calls append(CharSequence); checkError() calls the overridable flush() only while out != null.
replacedOut, afterClose: every method reads the protected field `out` each time; ensureOpen() is `if (out == null) throw new IOException("Stream closed");` inside the method's try, so with out == null writes, prints, println, printf/format (before looking at the format) and flush only set trouble = true.
writeErrors, flushErrors: every call on out sits in `try { ensureOpen(); ... } catch (IOException x) { trouble = true; }`; checkError() is `if (out != null) flush(); if (out instanceof PrintWriter) return ((PrintWriter) out).checkError(); return trouble;` (it never clears the flag); setError() sets and clearError() clears `trouble`.
runtimeFailures: catch only IOException; an IllegalStateException thrown by the underlying writer must propagate unchanged and must not set the error flag.
nestedCheckError: checkError() returns `((PrintWriter) out).checkError()` when out is a PrintWriter (this writer's own flag is then ignored), and `trouble` once out is null after close().
failingClose: close() is `try { if (out != null) { out.close(); out = null; } } catch (IOException x) { trouble = true; }`: when out.close() throws, out stays set, so later writes still reach it and the next close() calls out.close() again.
leavesOutFileNameConstructor: PrintWriter must not declare a `PrintWriter(String)` constructor (or any constructor other than the two Writer ones).
```

## Docs

Create `docs/bytecode-print-writer.md` with this skeleton (fill in the brackets):

    # Bytecode PrintWriter

    Revision `print-writer-vN` links `java.io.PrintWriter` over a `Writer` in the bundled exception runtime. Like
    `Writer`, it keeps the JDK `lock` field (here the wrapped writer) but never enters a monitor (single-threaded
    contract).

    Contract:

    - <constructors and the protected out field, write/print/println forms and their null handling, the "\n" line
      separator, format/printf through String.format, append forms, autoflush, the error flag (IOException
      swallowing, checkError, setError/clearError, wrapped PrintWriter), close and writes after close>
    - <limits: only the Writer constructors; a failing format writes nothing; no Formatter chunking>

    Evidence: `BytecodePrintWriterTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
    fresh instances), including a subclass and an underlying writer that record every call. `PrintWriter(String)` is
    rejected before WAT generation.
