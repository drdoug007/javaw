# T40 BufferedWriter

```kit
id: T40
slug: buffered-writer
title: link java.io.BufferedWriter over a Writer
tests: BytecodeBufferedWriterTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/io/BufferedWriter.java
allow: docs/bytecode-buffered-writer.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-buffered-writer-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeBufferedWriterTest.java`.

## Goal

Add `java.io.BufferedWriter` to the bundled runtime, on top of the existing `java.io.Writer`. The pinned libraries
name `java/io/BufferedWriter` in 9 rows of `runtime-target/target/audit/external-types.tsv`, and
`member-references.tsv` lists `<init>(Writer)` (H2 `Csv.initWrite` 3 sites, AspectJ `AsmManager` 2, ANTLR
`LogManager.save`, JAXB `MarshallerImpl.createWriter`, txw2 `StreamSerializer.createWriter`, Spring
`GroovyMarkupView`), `<init>(Writer,int)` (Tomcat `AccessLogValve.open`), `write(String)` (AspectJ 6, ANTLR 1),
`flush()` (AspectJ) and `close()` (ANTLR 2). `write(String)` and `append` are inherited from `Writer` and end in
`write(String,int,int)` / `write(int)` here, so the task links every public JDK 21 member of the class.

## Members to implement

One file `javaw-bytecode/src/main/resources/exception-runtime/java/io/BufferedWriter.java` (`package java.io;`):

`public class BufferedWriter extends Writer` with

- fields `private static final int DEFAULT_MAX_BUFFER_SIZE = 8192;`, `private Writer out;`, `private char[] cb;`,
  `private int nChars;`, `private int nextChar;`
- `public BufferedWriter(Writer out)`: `this(out, DEFAULT_MAX_BUFFER_SIZE);`
- `public BufferedWriter(Writer out, int sz)`
- `private void ensureOpen() throws IOException`
- `void flushBuffer() throws IOException` (package-private, as in the JDK)
- `public void write(int c) throws IOException`
- `public void write(char[] cbuf, int off, int len) throws IOException`
- `public void write(String s, int off, int len) throws IOException`
- `public void newLine() throws IOException`
- `public void flush() throws IOException`
- `public void close() throws IOException`

Do not override `write(char[])`, `write(String)` or the `append` methods: the runtime `Writer` versions already call
`write(char[],0,len)`, `write(String,0,len)` and `write(int)` like the JDK (the `Spy` subclass in the test checks
these calls).

## Members to leave out

- `growIfNeeded(int)` and `maxChars`. In JDK 21 only `BufferedWriter(Writer)` on a virtual thread starts at 512
  chars and grows to 8192. Otherwise the buffer starts at its full size (8192, or `sz`) and never grows. There are no threads here, so `cb` is
  allocated at full size once and `growIfNeeded` does not exist (the test checks that `BufferedWriter.growIfNeeded(I)V`
  stays unlinked). Without growth, `maxChars == nChars == cb.length` always.
- Do not edit `Writer.java` or any other runtime file (`kit guard` rejects it).

## JDK behaviour to match

Constructor `BufferedWriter(Writer out, int sz)`:

    super(out);
    if (sz <= 0) throw new IllegalArgumentException("Buffer size <= 0");
    this.out = out;
    cb = new char[sz];
    nChars = sz;
    nextChar = 0;

`super(out)` is the `Writer(Object lock)` constructor: it sets `lock = out` (the test checks `lock == out`) and
throws `NullPointerException` for a null writer **before** the size check (`new BufferedWriter(null, 0)` is a
`NullPointerException`, not an `IllegalArgumentException`). The constructor writes nothing.

    private void ensureOpen() throws IOException { if (out == null) throw new IOException("Stream closed"); }

    void flushBuffer() throws IOException {
        ensureOpen();
        if (nextChar == 0) return;
        out.write(cb, 0, nextChar);
        nextChar = 0;
    }

If `out.write` throws, `nextChar` is **not** reset: the next flush writes the same characters again
(`failures` expects `w2:0,2=! w2:0,2=ab`).

    public void write(int c) throws IOException {
        ensureOpen();
        if (nextChar >= nChars) flushBuffer();
        cb[nextChar++] = (char) c;
    }

The flush happens **before** storing, so a full buffer is left full: with `sz = 3`, writing `a b c` makes no call on
`out`; writing `d` then calls `out.write(cb, 0, 3)` with `abc`. `(char) 0x10045` is `E`.

    public void write(char[] cbuf, int off, int len) throws IOException {
        ensureOpen();
        if ((off | len) < 0 || len > cbuf.length - off)
            throw new IndexOutOfBoundsException("Range [" + off + ", " + off + " + " + len + ") out of bounds for length " + cbuf.length);
        if (len == 0) return;
        if (len >= nChars) { flushBuffer(); out.write(cbuf, off, len); return; }
        int b = off, t = off + len;
        while (b < t) {
            int d = Math.min(nChars - nextChar, t - b);
            System.arraycopy(cbuf, b, cb, nextChar, d);
            b += d;
            nextChar += d;
            if (nextChar >= nChars) flushBuffer();
        }
    }

- Order: `ensureOpen()` first (after `close()` even a bad range or a null array gives `IOException: Stream closed`),
  then the range check (a `null` array gives `NullPointerException` from `cbuf.length`), then `len == 0`.
- Message example: `write(new char[4], 1, Integer.MAX_VALUE)` -> `Range [1, 1 + 2147483647) out of bounds for length 4`
  (the `len > cbuf.length - off` form does not overflow). `write(c, 4, 0)` on a 4-char array is fine.
- Flush-through: `len >= nChars` (8192 for the default constructor) flushes what is buffered (nothing if empty) and
  passes the caller's array, offset and length straight to `out.write`. With `sz = 4`, buffered `01`, then
  `write(c, 2, 3)` gives `w4:0,4=0123` (the buffer fills and flushes inside the loop) and keeps `4`.

`write(String s, int off, int len)` has **no** range check of its own:

    ensureOpen();
    int b = off, t = off + len;
    while (b < t) {
        int d = Math.min(nChars - nextChar, t - b);
        s.getChars(b, b + d, cb, nextChar);
        b += d;
        nextChar += d;
        if (nextChar >= nChars) flushBuffer();
    }

- Strings never flush through: a long string is copied in buffer-sized chunks, and each full buffer is written with
  `out.write(cb, 0, nChars)`.
- `len <= 0` writes nothing and does not throw, even for a `null` string (`write((String) null, 0, 0)` and
  `write((String) null, 3, -2)` return normally; `write((String) null, 0, 1)` is a `NullPointerException`).
- Bad ranges come from `String.getChars` with the chunk bounds: `write("abc", -1, 2)` ->
  `StringIndexOutOfBoundsException: Range [-1, 1) out of bounds for length 3`; with `ab` already in a 4-char buffer,
  `write("abc", 2, 5)` -> `Range [2, 4) out of bounds for length 3` (the first chunk is only 2 chars).

`newLine()`: `write("\n");` (through the inherited `write(String)`, so a subclass sees `write(String,0,1)`). The JDK
writes `System.lineSeparator()`, but the runtime has no `System.lineSeparator`; use the same constant `"\n"` that the
runtime `PrintWriter` uses (`LINE_SEPARATOR`). The JVM in the test runs on macOS/Linux, where it is `"\n"` too.

`flush()`: `flushBuffer(); out.flush();` (so `out.flush()` is called even when nothing is buffered, and not when
`flushBuffer()` throws).

`close()` (JDK `implClose`):

    if (out == null) return;
    try (Writer w = out) {
        flushBuffer();
    } finally {
        out = null;
        cb = null;
    }

- `close()` calls `flushBuffer()`, **not** `flush()`: `out.flush()` is not called (the log shows `w4:0,2=ab c`, no
  `f`) and a subclass `flush()` override is not called.
- `out.close()` is always called, also when `flushBuffer()` throws. If both throw, the `flushBuffer` exception is
  thrown with the `close` exception added as suppressed (`disk suppressed=1:shut`); try-with-resources does this.
- After `close()`, even one that threw, the writer is closed: a second `close()` does nothing, and `write`, `flush`,
  `newLine` and `append` throw `IOException("Stream closed")`.
- A failed `out.flush()` does not close or break the writer: with `out.flush()` throwing `stuck`, `write("xy")`,
  `flush()`, `flush()`, `write("z")`, `close()` throws `stuck` twice, then the close succeeds and the sink log is
  `w4:0,2=xy f f w4:0,1=z c`.
- Do not override `write(int)`, `append` or `write(String)` paths away from the inherited ones. The `Spy` subclass
  (size 8) doing `newLine()`, `write("de")`, `append('f')`, `append("gh")`, `write("ij".toCharArray())`, `flush()`
  logs `S0,1 S0,2 I S0,2 C0,2 F ` and the sink gets one `w8:0,8` write of `"\ndefghij"` then `f`: `append(char)` goes to
  `write(int)`, `append(CharSequence)` goes through `write(String)` to `write(String,0,len)`.

## Steps

1. Read `javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeBufferedWriterTest.java`. `Sink` is the
   recording writer (`wL:off,len=text` per `write(char[] of length L,off,len)`, `f` per flush, `c` per close; `!`
   marks a write that throws `IOException("disk")`); `Spy` is the subclass.
2. Read `javaw-bytecode/src/main/resources/exception-runtime/java/io/Writer.java` and `BufferedOutputStream.java`
   (style) in the same directory.
3. Create `javaw-bytecode/src/main/resources/exception-runtime/java/io/BufferedWriter.java` with the members above,
   following the code blocks in "JDK behaviour to match".
4. Register it (never edit `BytecodeExceptionRuntime.java` by hand):
   `javaw-agent-kit/bin/kit register java/io/BufferedWriter.java`
5. `javaw-agent-kit/bin/kit check T40` until PASS.
6. Write `docs/bytecode-buffered-writer.md` (below), the report, then `javaw-agent-kit/bin/kit finish T40`.

## Hints

- Call `super(out)`, not `super()`, and do it before the size check.
- Never write the words `synchronized`, `volatile`, `native` or `Thread` anywhere in `BufferedWriter.java`, not even
  in a comment. Do not use or mention `InternalLock`, `VM`, `System.lineSeparator` or `Objects.checkFromIndexSize` in
  that file (none exist in the runtime). This applies only to the Java source: copy the Docs skeleton below as written.
  `Math.min(int,int)`, `System.arraycopy`, `String.getChars(int,int,char[],int)` and `Throwable.addSuppressed` exist.
- Try-with-resources on the local `Writer w = out` is fine in a runtime source; you can also write it out by hand
  (catch `Throwable` from `flushBuffer()`, call `w.close()` in its own try and `addSuppressed` its exception).
- Keep `cb` at full size from the constructor; do not add a growing buffer or a `maxChars` field.
- The Wasm runner stops at the first failing fixture of each `@Test`, so after a fix another fixture of the same
  group may fail next. That is progress.
- Calibration: the reference passed kit check after 1 run (about 70 new lines).

## Failure hints

```kit-hints
construction, writeCharacters, writeStrings, newLines, writeFailures, closing, bufferDoesNotGrow: if FIX THE RUNTIME says `link the symbolic class/interface java/io/BufferedWriter`, `link a complete acyclic type hierarchy: java/io/BufferedWriter`, `link the descriptor class/interface java/io/BufferedWriter` or a method of java/io/BufferedWriter, the class is missing or not registered: run `javaw-agent-kit/bin/kit register java/io/BufferedWriter.java` (never edit BytecodeExceptionRuntime.java by hand) and add every member listed under Members to implement with the exact signature. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
constructors: the constructor calls super(out) first (lock == out, NullPointerException for a null writer even with size 0), then throws new IllegalArgumentException("Buffer size <= 0") for sz <= 0.
writeSingle: write(int) flushes BEFORE storing when nextChar >= nChars, so a full buffer stays full until the next write; the default constructor uses 8192.
writeArrays, arrayRanges: write(char[],off,len) does ensureOpen, the Range [off, off + len) check, return on len == 0, then flush-through when len >= nChars (flushBuffer then out.write(cbuf, off, len) with the caller's array), else copy in chunks and flushBuffer each time the buffer becomes full.
writeStrings: write(String,off,len) has no range check and no flush-through: loop while b < t copying min(nChars - nextChar, t - b) chars with s.getChars(b, b + d, cb, nextChar), flushing when full; len <= 0 does nothing even for a null string.
newLines: newLine() is write("\n") through the inherited write(String); flush() is flushBuffer() then out.flush().
failures: flushBuffer resets nextChar to 0 only after out.write returns; flush() calls out.flush() only after flushBuffer() succeeded.
closing: close() returns at once when out == null; otherwise flushBuffer() inside try-with-resources on out (out.close() always runs, its exception is suppressed into the flush exception), never out.flush() or this.flush(), and finally out = null, cb = null. Every later write, flush, newLine and append throws IOException("Stream closed") from ensureOpen, before any range or null check.
bufferDoesNotGrow: this fails when BufferedWriter.growIfNeeded(int) has become linked. Allocate cb at full size in the constructor and do not add growIfNeeded.
```

## Docs

Create `docs/bytecode-buffered-writer.md` with this skeleton (fill in the brackets):

    # Bytecode BufferedWriter

    Revision `buffered-writer-vN` links `java.io.BufferedWriter` in the bundled exception runtime.

    Contract:

    - <constructors and the `Buffer size <= 0` message; write(int) flushing before storing; write(char[],int,int)
      range message and flush-through at the buffer size; write(String,int,int) chunking without its own range check;
      newLine; flush; close order, suppression and idempotence; `Stream closed`>

    Known differences: the buffer always starts at its full size (JDK 21 starts at 512 chars on a virtual thread and
    grows), and `newLine()` writes `"\n"` (the JDK writes `System.lineSeparator()`).

    Evidence: `BytecodeBufferedWriterTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders,
    two fresh instances). `BufferedWriter.growIfNeeded(int)` is rejected before WAT generation.
