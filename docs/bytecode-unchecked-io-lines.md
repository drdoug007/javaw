# Bytecode UncheckedIOException and BufferedReader.lines()

Revision `unchecked-io-lines-v302` links `java.io.UncheckedIOException` and `java.io.BufferedReader.lines()` in
the bundled exception runtime.

Contract:

- `UncheckedIOException` is a `RuntimeException` with two constructors: `UncheckedIOException(IOException cause)`
  passes `Objects.requireNonNull(cause)` to `Throwable(Throwable)`, so the message is `cause.toString()`
  (`java.io.IOException: boom`); `UncheckedIOException(String message, IOException cause)` keeps the given message,
  even `null`. A `null` cause throws `NullPointerException`. `getCause()` is a covariant override returning the
  same `IOException` object that was passed in.
- `lines()` is the JDK lazy stream: an anonymous `Iterator` whose `hasNext()` calls `readLine()` (a virtual call,
  so subclasses that override it see every call) only when no line is pending, and `next()` throws
  `NoSuchElementException` at the end. It is wrapped by
  `StreamSupport.stream(Spliterators.spliteratorUnknownSize(iter, ORDERED | NONNULL), false)`, so intermediate
  operations read nothing, short-circuiting operations stop at the first answer, and the reader can be used again
  afterwards. A `readLine()` failure (including on a closed reader) is wrapped in `new UncheckedIOException(e)`
  with the same `IOException` object as the cause. Closing the stream does not close the reader.

Known differences: no serialization (`readObject`). After the end, `next()` on `lines().iterator()` does not
call `readLine()` again as the JDK does (the runtime stream iterator remembers the end). Parallel `lines()` streams are not supported by the runtime
stream support.

Evidence: `BytecodeBufferedReaderLinesTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
orders, two fresh instances).
