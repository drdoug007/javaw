# T39 UncheckedIOException and BufferedReader.lines()

```kit
id: T39
slug: unchecked-io-lines
title: link java.io.UncheckedIOException and BufferedReader.lines()
tests: BytecodeBufferedReaderLinesTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/io/UncheckedIOException.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/io/BufferedReader.java
allow: docs/bytecode-unchecked-io-lines.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-unchecked-io-lines-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeBufferedReaderLinesTest.java`.

## Goal

Add `java.io.UncheckedIOException` to the bundled runtime and add `BufferedReader.lines()` to the existing
`java/io/BufferedReader.java` (task T32, done). The pinned libraries name `java/io/UncheckedIOException` as a type
in 17 classes (`runtime-target/target/audit/external-types.tsv`) and call it from 6 artifacts
(`member-references.tsv`): `<init>(IOException)` 7 times, `getCause()Ljava/io/IOException;` 7, `<init>(String,
IOException)` 5 and `getMessage()` once. Users are Spring core/web/webmvc (`FileCopyUtils`-style helpers that wrap
an `IOException` thrown inside a lambda), Spring Boot and Boot autoconfigure (`NativeImageResourceProvider`) and
Tomcat fileupload (`FileItem`, `DiskFileItem`). Nothing in the pinned set calls `BufferedReader.lines()` yet; it is
the standard way JDK code wraps `readLine()` failures in `UncheckedIOException`, so it is linked here together with
the exception on top of the existing runtime stream support (`java/util/stream/StreamSupport.java`,
`java/util/Spliterators.java`).

Copy the JDK 21 structure **exactly**: `lines()` reads nothing until a terminal operation runs, and every line comes
from a virtual call to `readLine()`, so a subclass that overrides `readLine()` sees every call. The test records them.

## Members to implement

1. New file `javaw-bytecode/src/main/resources/exception-runtime/java/io/UncheckedIOException.java`
   (`package java.io;`, `public class UncheckedIOException extends RuntimeException`):
   - `public UncheckedIOException(String message, IOException cause)`
   - `public UncheckedIOException(IOException cause)`
   - `public IOException getCause()` (a covariant override of `Throwable.getCause()`)
2. In the existing `javaw-bytecode/src/main/resources/exception-runtime/java/io/BufferedReader.java`, one new method:
   - `public java.util.stream.Stream<String> lines()`

## Members to leave out

- `UncheckedIOException.readObject(ObjectInputStream)` and `serialVersionUID`: there is no serialization and
  `ObjectInputStream` is not in the runtime. Do not add them.
- Do not touch any other `BufferedReader` method; the T32 test still checks them.

## JDK behaviour to match

- `new UncheckedIOException(cause)` is `super(Objects.requireNonNull(cause))`, and
  `new UncheckedIOException(message, cause)` is `super(message, Objects.requireNonNull(cause))`. A null cause throws
  `NullPointerException` with message `null`, also when the message is null too.
- So the one-argument form takes its message from `cause.toString()` (that is what `Throwable(Throwable)` does):
  `new UncheckedIOException(new IOException("boom")).getMessage()` is `java.io.IOException: boom`, with
  `new IOException()` it is `java.io.IOException`, and a cause class that overrides `toString()` decides the message
  (`DiskFull<x>`). The two-argument form keeps the given message, even `null`.
- `toString()` is inherited: `java.io.UncheckedIOException: java.io.IOException: boom` and
  `java.io.UncheckedIOException: wrapped`.
- `getCause()` returns `(IOException) super.getCause()`: the same object that was passed in.
- `lines()` is exactly the JDK code:

      public Stream<String> lines() {
          Iterator<String> iter = new Iterator<>() {
              String nextLine = null;
              public boolean hasNext() {
                  if (nextLine != null) return true;
                  try { nextLine = readLine(); return (nextLine != null); }
                  catch (IOException e) { throw new UncheckedIOException(e); }
              }
              public String next() {
                  if (nextLine != null || hasNext()) { String line = nextLine; nextLine = null; return line; }
                  else throw new NoSuchElementException();
              }
          };
          return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                  iter, Spliterator.ORDERED | Spliterator.NONNULL), false);
      }

  Consequences the test checks: creating the stream and adding `filter`/`map` calls `readLine()` zero times;
  `findFirst`, `anyMatch` and `noneMatch` stop reading once they have their answer, and the reader can be used again
  afterwards (`readLine()` or another `lines()` continues after the last line read); a `readLine()` that throws
  `IOException("disk")` turns into `UncheckedIOException` with message `java.io.IOException: disk` and that same
  `IOException` as the cause; `lines()` on a closed reader is fine, but the terminal operation throws
  `UncheckedIOException: java.io.IOException: Stream closed`. Closing the reader after `lines()` but before the
  terminal operation also gives `UncheckedIOException: java.io.IOException: Stream closed`, and a source whose very
  first read fails gives `UncheckedIOException: java.io.IOException: disk` from `findFirst`; after a `readLine()`
  failure, `readLine()` and a new `lines()` fail again because the source is still positioned at the failing read.
  The spliterator has characteristics
  `272` (`ORDERED | NONNULL`), `estimateSize()` is `Long.MAX_VALUE`, and a `tryAdvance` after the end calls
  `readLine()` again. Closing the stream does **not** close the reader (no `onClose` handler is added).
  User `onClose` handlers and the `IllegalStateException` on reuse of a closed stream come from the existing stream
  runtime; `lines()` adds nothing for them.

## Steps

1. Read the test `javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeBufferedReaderLinesTest.java`,
   then `javaw-bytecode/src/main/resources/exception-runtime/java/io/BufferedReader.java`,
   `java/io/IOException.java`, `java/util/Spliterators.java` (`spliteratorUnknownSize`) and
   `java/util/stream/StreamSupport.java` (`stream(Spliterator, boolean)`) in the same runtime directory.
2. Create `javaw-bytecode/src/main/resources/exception-runtime/java/io/UncheckedIOException.java` with the three
   members above.
3. Register it (do not edit `BytecodeExceptionRuntime.java` by hand):
   `javaw-agent-kit/bin/kit register java/io/UncheckedIOException.java`
4. Add `lines()` to `BufferedReader.java` as shown above, with the imports `java.util.Iterator`,
   `java.util.NoSuchElementException`, `java.util.Spliterator`, `java.util.Spliterators`,
   `java.util.stream.Stream` and `java.util.stream.StreamSupport` at the top of the file (after `package java.io;`).
5. `javaw-agent-kit/bin/kit check T39` until PASS.
6. Write `docs/bytecode-unchecked-io-lines.md` (below), the report, then `javaw-agent-kit/bin/kit finish T39`.

## Hints

- `java.util.Objects.requireNonNull(Object)` exists in the runtime and throws `NullPointerException` with no
  message. Call it inside the `super(...)` argument, exactly as in the JDK.
- Do not add a `getMessage()` or `toString()` override to `UncheckedIOException`; `Throwable` already does the
  right thing.
- The covariant `getCause()` must be `public IOException getCause() { return (IOException) super.getCause(); }`.
  Without it, a call compiled as `UncheckedIOException.getCause()Ljava/io/IOException;` does not link.
- Inside the anonymous `Iterator`, call `readLine()` unqualified (or `BufferedReader.this.readLine()`), never a
  private helper: the `Spy` fixture overrides `readLine()` and logs every call.
- Do not read ahead, do not cache all lines in a list and do not use `Stream.generate`/`iterate`: the laziness
  fixtures count every `readLine()` call.
- Known difference, not yours to fix: on the JDK, `next()` on a `lines().iterator()` that is already at the end
  calls `readLine()` once more; the runtime stream iterator remembers that it ended and does not. The
  `linesIterator` fixture does not log `readLine()` calls after the end, so do not try to match this in
  `BufferedReader` (it lives in `java/util/stream/ReferenceStream.java`, which you may not change).
- The whole reference is about 30 lines: 10 in UncheckedIOException.java and about 20 in BufferedReader.java
  (six imports and `lines()`).
- Calibration: the reference passed kit check after 2 runs (about 30 new lines).

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
uncheckedIOException, linesContent, linesIsLazy, linesTraversal, linesFailuresAndClosing: if FIX THE RUNTIME says `link the symbolic class/interface java/io/UncheckedIOException`, `link the descriptor class/interface java/io/UncheckedIOException` or a method of java/io/UncheckedIOException or `java/io/BufferedReader.lines`, the class is missing or not registered or lines() is missing: run `javaw-agent-kit/bin/kit register java/io/UncheckedIOException.java` (never edit BytecodeExceptionRuntime.java by hand), add the three UncheckedIOException members and BufferedReader.lines() with the exact signatures. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes (usually a missing import in BufferedReader.java).
exceptionConstructors, exceptionNullCause: the constructors are `super(Objects.requireNonNull(cause))` and `super(message, Objects.requireNonNull(cause))`; the one-argument message is cause.toString() (from Throwable), so do not build a message yourself and do not override getMessage or toString.
linesLaziness, linesShortCircuit, linesIterator, linesSpliterator: lines() must be the JDK anonymous Iterator (nextLine field, hasNext calls readLine() only when nextLine is null) wrapped in `StreamSupport.stream(Spliterators.spliteratorUnknownSize(iter, Spliterator.ORDERED | Spliterator.NONNULL), false)`; never read lines before the terminal operation and never call a private reader method instead of readLine().
linesFailures: hasNext must catch IOException from readLine() and throw `new UncheckedIOException(e)` with the same exception object; do not check for a closed reader in lines() itself.
linesClosing: do not register an onClose handler that closes the reader; closing the stream leaves the BufferedReader open.
```

## Docs

Create `docs/bytecode-unchecked-io-lines.md` with this skeleton (fill in the brackets):

    # Bytecode UncheckedIOException and BufferedReader.lines()

    Revision `unchecked-io-lines-vN` links `java.io.UncheckedIOException` and `java.io.BufferedReader.lines()` in
    the bundled exception runtime.

    Contract:

    - <UncheckedIOException constructors (NullPointerException for a null cause, message from cause.toString()),
      covariant getCause(); lines() as a lazy ordered NONNULL stream over readLine() calls, IOException wrapped in
      UncheckedIOException, closing the stream does not close the reader>

    Known differences: no serialization (`readObject`). After the end, `next()` on `lines().iterator()` does not
    call `readLine()` again as the JDK does (the runtime stream iterator remembers the end). Parallel `lines()` streams are not supported by the runtime
    stream support.

    Evidence: `BytecodeBufferedReaderLinesTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
    orders, two fresh instances).
