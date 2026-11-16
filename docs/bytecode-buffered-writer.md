# Bytecode BufferedWriter

Revision `buffered-writer-v303` links `java.io.BufferedWriter` in the bundled exception runtime.

Contract:

- `BufferedWriter(Writer out, int sz)` throws `NullPointerException` if `out` is null, and `IllegalArgumentException("Buffer size <= 0")` if `sz <= 0`.
- `write(int c)` flushes the buffer before storing the character if the buffer is full.
- `write(char[] cbuf, int off, int len)` throws `IndexOutOfBoundsException` with `Range [off, off + len) out of bounds for length cbuf.length` if the range is invalid. It performs a flush-through if `len >= nChars`.
- `write(String s, int off, int len)` uses chunking via `String.getChars` and does not perform its own range check.
- A null writer throws `NullPointerException` from `super(out)` before the size check, so `new BufferedWriter(null, 0)` throws NPE, not `IllegalArgumentException`.
- `newLine()` writes `"\n"`.
- `flush()` calls `flushBuffer()` and then `out.flush()`.
- `close()` ensures `out.close()` is called via try-with-resources, even if `flushBuffer()` throws. If both throw, the `flushBuffer` exception propagates with the `out.close()` exception added to it as suppressed. After `close()`, all operations throw `IOException("Stream closed")`.

Known differences: the buffer always starts at its full size (JDK 21 starts at 512 chars on a virtual thread and
grows), and `newLine()` writes `"\n"` (the JDK writes `System.lineSeparator()`).

Evidence: `BytecodeBufferedWriterTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders,
two fresh instances). `BufferedWriter.growIfNeeded(int)` is rejected before WAT generation.
