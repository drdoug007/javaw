# Host-independent character streams

The source compiler supplies in-memory `java.io` character streams; no file handles, Node filesystem APIs, or browser APIs are needed. This extends the byte streams under Phase 4 of Todo.md. It does not complete the broader I/O/NIO/networking roadmap item.

## Available operations

| Type | Supported core operations |
| --- | --- |
| Reader | Single/bulk read, skip, ready, mark/reset defaults, abstract close |
| Writer | Character/array/string write, whole/ranged append, flush, close |
| StringReader | String input, single/bulk read, bounded forward/backward skip, ready, mark/reset, close |
| StringWriter | Writes/appends, toString, flush, no-op close; remains writable after close |
| CharArrayReader | Whole/ranged input, single/bulk read, skip, ready, mark/reset, close |
| CharArrayWriter | Growable storage, writes/appends, size/reset, copied toCharArray, toString, writeTo, no-op close |
| BufferedReader | Sized/default buffer, single/bulk read, readLine, skip, ready, mark/reset with read-ahead limits, close |
| BufferedWriter | Sized/default buffer, character/array/string write, newLine, flush, close |

Reader and Writer implement Closeable; Closeable extends AutoCloseable. Writer and OutputStream implement Flushable. Writer implements Appendable, with append methods returning the writer for chaining. String arguments and null sequences (`"null"`) are supported by writer append operations.

Buffered line reading recognizes LF, CR, and CRLF, including separators crossing buffer boundaries. It returns a final unterminated line, then null on subsequent calls. Valid marks retain buffered characters across EOF; refill invalidates marks whose read-ahead limit has been reached. Closing readers makes further data operations throw IOException. BufferedWriter flushes and closes its sink; even when writing buffered characters fails, it attempts sink closure and clears its own state. Exceptions from a custom sink propagate. Simultaneous write and close failures do not currently preserve JDK suppressed-exception details.

Null buffers and invalid ranges throw the corresponding NullPointerException or bounds exceptions; negative capacities and nonpositive buffer sizes are rejected. StringReader allows backward skip up to the beginning while not at EOF, matching its JDK-specific skip behavior. CharArrayReader clamps positive skip and does not skip backward. BufferedReader rejects negative skip.

## Limits

The runtime stores strings as byte sequences, rather than Java UTF-16 strings. String-based character operations are verified with ASCII; full non-ASCII/surrogate and charset semantics are pending. Character-array storage preserves Java char values, but conversion to/from strings inherits the string runtime's limitations. These streams do not encode/decode bytes: InputStreamReader, OutputStreamWriter, Charset, files, sockets, NIO, and serialization remain separate work.

StringWriter.getBuffer() requires the unsupported StringBuffer API and is not exposed. Arbitrary CharSequence implementations and StringBuilder through CharSequence/Appendable interfaces are outside this tested subset. Direct StringBuilder operations remain available. Newer conveniences such as Reader.transferTo/read(CharBuffer), nullReader/nullWriter, and BufferedReader.lines are pending.

There is no Java monitor or thread-safe stream guarantee. Default buffer sizes and capacity hints can differ from the JDK; they do not change stream contents. Heap exhaustion is subject to the existing allocator's limits. This is a core library subset, not full JDK I/O compatibility.

## Executable evidence

Run:

```sh
mvn -pl javaw-core -Dtest=IoSupportTest test
```

IoSupportTest compiles source to Wasm and executes it through Node. It covers byte streams, core character-stream operations, base-class/interface dispatch, long lines with one-character buffers, CR/LF/CRLF, repeated EOF, mark preservation/invalidation, backward skip, null/range errors, growth, copy isolation, no-op and enforced close behavior, and closure after sink-write failure.
