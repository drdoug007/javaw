# Bytecode byte/character sink base classes

Revision `io-sinks-v251` links `java.lang.Appendable`, `java.io.Closeable`, `java.io.Flushable`, `java.io.OutputStream` and `java.io.Writer` from the bundled exception runtime so library subclasses (for example Jackson `ByteArrayBuilder` and `UTF8Writer`) can admit their declared hierarchy.

Contract:

- `OutputStream.write(byte[])` and `write(byte[],int,int)` delegate to `write(int)` per byte. Null arrays raise `NullPointerException`; invalid ranges raise `IndexOutOfBoundsException` with the JDK `Range [o, o + l) out of bounds for length n` message. `flush` and `close` are no-ops.
- `Writer` keeps the JDK `lock` field (`this` by default, or a non-null supplied object), a reused 1,024-character buffer for `write(int)` and short `write(String,int,int)` calls, and fresh buffers for longer strings. `append` follows JDK null handling (`"null"`) and `subSequence` bounds failures.
- The selected deployment is single-threaded: `Writer` never enters monitors on `lock`. Concurrent writers remain part of the concurrency milestone.

Evidence: `BytecodeIoSinkTest` compares Java 17 javac fixtures against the JVM in both JAR orders and two fresh dirty-memory instances, covering subclass dispatch, interface views, signed/truncated byte writes, offsets, the surrogate-truncating `write(int)`, long strings, lock identity and typed failures/messages. ASM fixtures prove unused calls to unlinked members (`OutputStream.nullOutputStream`, `Writer.nullWriter`) and owners (`PrintWriter`) are rejected with source locations before WAT generation.

Not linked: concrete JDK streams/writers (`ByteArrayOutputStream`, `StringWriter`, `PrintStream`, `PrintWriter`, buffered/file/filter streams), `InputStream`/`Reader`, `nullOutputStream`/`nullWriter`, charset encoders and host-backed files/channels.
