# Bytecode PrintWriter

Revision `print-writer-v290` links `java.io.PrintWriter` over a `Writer` in the bundled exception runtime. Like
`Writer`, it keeps the JDK `lock` field (here the wrapped writer) but never enters a monitor (single-threaded
contract).

Contract:

- The two constructors are `PrintWriter(Writer out)` (`this(out, false)`) and `PrintWriter(Writer out, boolean
  autoFlush)`, which calls `super(out)`; that sets `lock` to the wrapped writer once and throws
  `NullPointerException` for `null`, so `new PrintWriter((Writer) null)` throws it. The `protected Writer out`
  field is read by every method, so a subclass may assign a different writer (or `null`) at any time; assigning
  `out` later does not change `lock` (`lock == out` becomes `false`). `write(int)`, `write(char[],int,int)` and
  `write(String,int,int)` each do `try { ensureOpen(); out.write(...); }` and catch only `IOException` (setting
  `trouble`), with no range check or null check of their own: a bad range or a `null` array/string throws the
  underlying writer's `IndexOutOfBoundsException`/`NullPointerException` unchanged and a `RuntimeException` from
  `out` propagates with `trouble` untouched. `write(char[])` is `write(buf, 0, buf.length)` and `write(String)` is
  `write(s, 0, s.length())` on `this`, both reading the length first, so a `null` argument throws
  `NullPointerException` even after `close()`. `print(boolean/int/long/float/double/String/Object)` is
  `write(String.valueOf(x))` (`null` prints `null`), `print(char)` is `write(c)` (the `write(int)` override) and
  `print(char[])` is `write(s)` (the `write(char[])` override, so `null` throws). `println()` is `newLine()`,
  which writes `LINE_SEPARATOR` to the **underlying** writer via `out.write(String)` (never `this.write`, so a
  subclass does not see the separator in its `write` overrides); `println(x)` is `print(x); println();` and
  `println(Object)` calls `print(String)` on `String.valueOf(x)`, not `print(Object)`. The line separator is the
  constant `"\n"` (the runtime has no `System.lineSeparator()` and `System.getProperty("line.separator")` is
  `null` in Wasm). `format(String,Object...)` and `format(Locale,String,Object...)` are
  `try { ensureOpen(); write(String.format(...)); if (autoFlush) out.flush(); }` (the `Locale` overload may take
  a `null` `Locale`) and return `this`; `printf(...)` returns `format(...)` with the same arguments, so a failing
  format writes nothing, and a bad format or `null` format throws what `String.format` throws, uncaught.
  `append(CharSequence)` is `write(String.valueOf(csq)); return this;`; `append(csq,start,end)` reassigns a
  `null` `csq` to `"null"` and returns `append(csq.subSequence(start,end))`, so a bad range throws
  `StringIndexOutOfBoundsException` uncaught; `append(char)` is `write(c); return this;` — all three covariant,
  returning `PrintWriter`. Autoflush happens only through `newLine()` (so every `println`) and `format`/`printf`;
  `print`, `write` and `append` never flush. The error flag: `checkError()` calls the overridable `flush()` while
  `out != null`, then returns `((PrintWriter) out).checkError()` when `out` is a wrapped `PrintWriter` (this
  writer's own flag is ignored) and `trouble` otherwise; it never clears the flag. `setError()` sets and
  `clearError()` clears `trouble`. `close()` is `try { if (out != null) { out.close(); out = null; } }`, so a
  second `close()` does nothing and, when `out.close()` throws, `out` stays set — later writes still reach it and
  the next `close()` tries again. After `close()` (or with `out == null`) every write, print, println,
  `format`/`printf` (before looking at the format) and `flush` just set `trouble`.
- Limits: only the two `Writer` constructors are linked; the `OutputStream`, `String`/`Charset` and `File`
  constructors, the `PrintStream`/`Formatter` fields and the `SharedSecrets` constructor stay out (their
  dependencies are not in the runtime). `PrintWriter(String)` is rejected before WAT generation. `format` writes
  the whole `String.format` result with one `write(String)`: no `Formatter`, no per-literal chunking, so a format
  that fails mid-conversion writes nothing (the JDK could have written the literal prefix).

Evidence: `BytecodePrintWriterTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
fresh instances), including a subclass and an underlying writer that record every call. `PrintWriter(String)` is
rejected before WAT generation.
