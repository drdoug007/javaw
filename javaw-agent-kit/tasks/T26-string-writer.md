# T26 StringWriter and CharArrayWriter

```kit
id: T26
slug: string-writer
title: link java.io.StringWriter and java.io.CharArrayWriter
tests: BytecodeStringWriterTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/io/StringWriter.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/io/CharArrayWriter.java
allow: docs/bytecode-string-writer.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-string-writer-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeStringWriterTest.java`.

## Goal

Add the two in-memory character sinks of `java.io`, `java.io.StringWriter` and `java.io.CharArrayWriter`, on top of
the existing runtime `java.io.Writer`. The pinned libraries name `StringWriter` from 36 classes and
`CharArrayWriter` from 55 (`runtime-target/target/audit/external-types.tsv`). The member rows
(`member-references.tsv`) are `StringWriter.<init>()` 42 call sites, `toString()` 40, `getBuffer()` 5,
`<init>(int)` 4, `write(String)` 3, `flush`, `close`, `append(CharSequence)` and `append(char)` 2 each (H2
`JdbcClob.getSubString`, `JdbcSQLXML`, `ThreadDeadlockDetector`; logback `ViewStatusMessagesServletBase`; Jackson
`DOMSerializer`; Spring, Hibernate, snakeyaml), and `CharArrayWriter.append(CharSequence)` 67, `append(char)` 44,
`write(String,int,int)` 4, `toString()` 2, `writeTo(Writer)`, `write(String)`, `write(int)`, `size()`, `reset()`,
`<init>()` and `<init>(int)` once each (Tomcat's `AbstractAccessLogValve` builds every access-log line in a
`CharArrayWriter`).

The JDK `CharArrayWriter` methods use `synchronized (lock)`. Here the runtime is **single-threaded**: write them with
no `synchronized` (RULES.md). The runtime `Writer` already keeps the `protected Object lock` field without entering
monitors; do not change `Writer.java`.

## Members to implement

`java/io/StringWriter.java` (`package java.io;`): `public class StringWriter extends Writer`

- `private final StringBuffer buf;` (the runtime has `java.lang.StringBuffer`)
- `public StringWriter()` and `public StringWriter(int initialSize)`; both assign `lock = buf;`
- `public void write(int c)`
- `public void write(char[] cbuf, int off, int len)`
- `public void write(String str)`
- `public void write(String str, int off, int len)`
- `public StringWriter append(CharSequence csq)`
- `public StringWriter append(CharSequence csq, int start, int end)`
- `public StringWriter append(char c)`
- `public String toString()`
- `public StringBuffer getBuffer()`
- `public void flush()` (does nothing, no `throws`)
- `public void close() throws IOException` (does nothing; keep the `throws`, the JDK declares it)

`java/io/CharArrayWriter.java` (`package java.io;`): `public class CharArrayWriter extends Writer`

- `protected char[] buf;` and `protected int count;` (subclasses read and assign them directly; the test does)
- `public CharArrayWriter()` (`this(32)`) and `public CharArrayWriter(int initialSize)`
- `public void write(int c)`
- `public void write(char[] c, int off, int len)`
- `public void write(String str, int off, int len)`
- `public void writeTo(Writer out) throws IOException`
- `public CharArrayWriter append(CharSequence csq)`
- `public CharArrayWriter append(CharSequence csq, int start, int end)`
- `public CharArrayWriter append(char c)`
- `public void reset()`
- `public char[] toCharArray()`
- `public int size()`
- `public String toString()`
- `public void flush()` and `public void close()` (both do nothing, no `throws`)

None of the `write` and `append` methods above declares `throws IOException` (fixture code calls them outside
`try`). Only `StringWriter.close()` and `CharArrayWriter.writeTo(Writer)` declare it.

Do **not** override these; the inherited `Writer` versions give the JDK call pattern and the test checks it:

- `StringWriter`: `write(char[])` (Writer calls `write(cbuf, 0, cbuf.length)`).
- `CharArrayWriter`: `write(char[])` and `write(String)` (Writer calls `write(str, 0, str.length())`, so
  `write((String) null)` throws `NullPointerException` there).

## Members to leave out

Both classes have no other public members in JDK 21. Do not edit any other runtime file (`kit guard` rejects it):
not `Writer.java` (no `nullWriter()`), not `StringBuffer.java`, and do **not** add `Objects.checkFromIndexSize` to
`java/util/Objects.java`. `PrintWriter`, `BufferedWriter` and the other writers are separate work.

## JDK behaviour to match

Range check (only in `write(char[], int, int)` of both classes). The JDK calls
`Objects.checkFromIndexSize(off, len, c.length)`, which the runtime does **not** have. Write it inline, reading
the length first: `int length = c.length; if ((off | len) < 0 || len > length - off)` throw
`new IndexOutOfBoundsException("Range [" + off + ", " + off + " + " + len + ") out of bounds for length " + length)`.
Examples on a 5-char array: `(-1, 1)` -> `Range [-1, -1 + 1) out of bounds for length 5`; `(0, -1)` ->
`Range [0, 0 + -1) out of bounds for length 5`; `(1, Integer.MAX_VALUE)` -> `Range [1, 1 + 2147483647) out of
bounds for length 5` (never compute `off + len`, it overflows); `(5, 0)` and `(0, 5)` are fine. A `null` array
throws `NullPointerException`, also with a negative `off` or `len`, because `int length = c.length;` runs before
the range test (the test only checks `(null, 0, 0)`). A failed call writes nothing. Then `len == 0`
returns without doing anything else. The type is plain `IndexOutOfBoundsException`.

`StringWriter`:

- `StringWriter()`: `buf = new StringBuffer(); lock = buf;`.
- `StringWriter(int initialSize)`: `initialSize < 0` throws `new IllegalArgumentException("Negative buffer size")`
  (no number in the message); otherwise `buf = new StringBuffer(initialSize); lock = buf;`. `0` is allowed.
- `write(int c)`: `buf.append((char) c)` (only the low 16 bits: `0x10042` writes `B`).
- `write(char[] cbuf, int off, int len)`: range check, `len == 0` returns, then `buf.append(cbuf, off, len)`.
- `write(String str)`: `buf.append(str)`. `null` appends the four chars `null` (StringBuffer does that; no
  exception).
- `write(String str, int off, int len)`: `buf.append(str, off, off + len)`, nothing else. StringBuffer does the
  checks: a bad range throws `IndexOutOfBoundsException` (the test compares only the type), and a `null` string
  counts as `"null"`, so `write((String) null, 1, 2)` writes `ul`. It must not call `write(String)` or any other
  overridable method: a subclass that calls `write("hij", 1, 1)` logs only `wso(hij,1,1)`.
- The two classes throw different types for the same bad `write(String, int, int)` range, and that is correct: on
  JDK 21 `StringWriter.write("abc", 4, 0)` throws plain `IndexOutOfBoundsException` (from `StringBuffer`), while
  `CharArrayWriter.write("abc", 4, 0)` throws `StringIndexOutOfBoundsException` (from `getChars`). Do not make
  them match.
- `append(CharSequence csq)`: `write(String.valueOf(csq)); return this;` (`null` -> `null`). It must call the
  overridable `write(String)` (a subclass sees `ws(...)`).
- `append(CharSequence csq, int start, int end)`: `if (csq == null) csq = "null"; return append(csq.subSequence(start, end));`
  (it calls the overridable `append(CharSequence)`; a bad range throws whatever `subSequence` throws, the test
  checks `StringIndexOutOfBoundsException` for a `String` and a `StringBuilder`).
- `append(char c)`: `write(c); return this;` (calls the overridable `write(int)`).
- `toString()`: `buf.toString()`.
- `getBuffer()`: returns `buf` itself, the **live** buffer: `getBuffer().setLength(2)`, `insert(0, '<')` or
  `append(...)` change what the writer holds, and every call returns the same object.
- `flush()` and `close()` do nothing; writing after `close()` still works.

`CharArrayWriter`:

- `CharArrayWriter(int initialSize)`: `initialSize < 0` throws
  `new IllegalArgumentException("Negative initial size: " + initialSize)` (`-1` -> `Negative initial size: -1`);
  otherwise `buf = new char[initialSize];`. `0` is allowed. `lock` stays the `Writer` default (`this`).
- Growth, in every write: `int newcount = count + n; if (newcount > buf.length) buf = Arrays.copyOf(buf, Math.max(buf.length << 1, newcount));`.
  It gives the same sizes as the `ByteArrayOutputStream` rule. The test reads `buf.length`: from capacity 0,
  single writes give 1, 2, 4, 4, 8, 8, 8, 8, 16; 33 writes into a default writer give 64; from 3, writing 10 chars gives 10, one
  more gives 20, 50 more give 61, 3 more give 122; from 2, `write("abcde", 0, 5)` gives 5, then `write("f")` gives
  10, then two appends of 10 chars give 20 and 40.
- `write(int c)`: grow for `count + 1`, `buf[count] = (char) c`, `count = newcount`.
- `write(char[] c, int off, int len)`: range check, `len == 0` returns, grow for `count + len`,
  `System.arraycopy(c, off, buf, count, len)`, `count = newcount`.
- `write(String str, int off, int len)`: **no range check of its own**. Grow for `count + len` first, then
  `str.getChars(off, off + len, buf, count)`, then `count = newcount`. So a bad range throws
  `StringIndexOutOfBoundsException` from `getChars` **after** the buffer may have grown: on a capacity-2 writer
  holding one char, `write("abc", 1, 5)` throws and leaves capacity 6 and size 1. Negative `len` does not grow
  (newcount is smaller) and `getChars` throws. A `null` string throws `NullPointerException`.
- `writeTo(Writer out)`: exactly one call `out.write(buf, 0, count)`, passing the **internal** array (a recording
  writer sees `[32,0,3:abc]` for a default writer holding `abc`). It is made even when `count == 0`. An
  `IOException` from `out` propagates unchanged; `writeTo(null)` throws `NullPointerException`, also when empty.
- `append(CharSequence csq)`: `String s = String.valueOf(csq); write(s, 0, s.length()); return this;` (calls the
  overridable `write(String, int, int)`; `null` -> `null`).
- `append(CharSequence csq, int start, int end)`: `if (csq == null) csq = "null"; return append(csq.subSequence(start, end));`.
- `append(char c)`: `write(c); return this;`.
- `reset()`: `count = 0`; the buffer and its capacity are kept.
- `toCharArray()`: `Arrays.copyOf(buf, count)` (a fresh array every time).
- `size()`: `count`. `toString()`: `new String(buf, 0, count)`.
- Every method reads `buf` and `count` each time: a subclass that does
  `super(0); buf = new char[]{'x','y','z'}; count = 3;` and then calls `write('D')` grows `buf.length` from 3 to 6
  (the `charArrayFields` fixture expects `3xyz,6xyzD,2xyxy,ayZ,6,ayZ123,6`).
- `flush()` and `close()` do nothing; writing after `close()` still works.

Subclass call patterns (the `Logging*` classes in the fixture record them):

- `StringWriter`: `append("ab")` -> `a(ab)ws(ab)`; `append("xyz", 1, 3)` -> `a(yz)ws(yz)`; `append(null)` ->
  `a(null)ws(null)`; `append(null, 0, 2)` -> `a(nu)ws(nu)`; `append('c')` -> `w(99)`;
  `write(new char[]{'d','e'})` -> `wa(2,0,2)`; `write("fg")` -> `ws(fg)` only.
- `CharArrayWriter`: `append("ab")` -> `a(ab)wso(ab,0,2)`; `append(null)` -> `a(null)wso(null,0,4)`;
  `append(null, 0, 2)` -> `a(nu)wso(nu,0,2)`; `append('c')` -> `w(99)`; `write(new char[]{'d','e'})` -> `wa(2,0,2)`;
  `write("fg")` -> `wso(fg,0,2)`.

## Steps

1. Read the test `javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeStringWriterTest.java`.
2. Read `javaw-bytecode/src/main/resources/exception-runtime/java/io/Writer.java` (the superclass; do not edit it)
   and `java/io/ByteArrayOutputStream.java` (the same shape for bytes).
3. Create `javaw-bytecode/src/main/resources/exception-runtime/java/io/StringWriter.java` and
   `javaw-bytecode/src/main/resources/exception-runtime/java/io/CharArrayWriter.java`.
4. Register both, in any order:
   `javaw-agent-kit/bin/kit register java/io/StringWriter.java` and
   `javaw-agent-kit/bin/kit register java/io/CharArrayWriter.java`.
   Each adds a `paths.add("java/io/....java");` line before the `ExceptionSupport` line of
   `BytecodeExceptionRuntime.java`; do not edit that file by hand.
5. `javaw-agent-kit/bin/kit check T26` until PASS.
6. Write `docs/bytecode-string-writer.md` (below), the report, then `javaw-agent-kit/bin/kit finish T26`.

## Hints

- Both classes together are about 70 lines (StringWriter about 29, CharArrayWriter about 43).
  `java.util.Arrays.copyOf(char[], int)` exists in the runtime; add `import java.util.Arrays;`. `IOException` is in `java.io`, so it needs no import. `StringBuffer` is in
  `java.lang`.
- Runtime `StringBuffer` methods you need: `append(char)`, `append(char[], int, int)`, `append(String)`,
  `append(CharSequence, int, int)`, `toString()`, constructors `()` and `(int)`. Do not add anything to it.
- Covariant returns: `append` must be declared to return `StringWriter` / `CharArrayWriter`, not `Writer`
  (fixture code assigns `StringWriter r = w.append("ab")`; javac adds the bridge methods).
- `append(csq, start, end)` must call `append(...)` on `this` (not `write`), so a subclass's override of
  `append(CharSequence)` sees the subsequence.
- `StringWriter.write(String)` must be overridden (`buf.append(str)`): the inherited `Writer.write(String)` calls
  `str.length()` (NPE for `null`) and `write(String, int, int)` (the subclass would log `wso` instead of `ws`).
  Without it javac also rejects `append(CharSequence)` with `unreported exception IOException; must be caught or
  declared to be thrown`, because the inherited `Writer.write(String)` declares `throws IOException`. The same
  error on `write(...)` inside `CharArrayWriter.append` means you called a `Writer` method the class does not
  override: call `write(s, 0, s.length())` and `write(c)` (both overridden, no `throws`).
- A `private void grow(int newcount)` helper in `CharArrayWriter` (the growth line above) is fine; all three
  write methods use it.
- Do not add a range check to either `write(String, int, int)`: the String/StringBuffer methods already throw the
  JDK types, and `CharArrayWriter` must grow before `getChars` throws.
- Do not write `Objects.checkFromIndexSize`; it does not exist in the runtime. Write the check inline in each
  class (or as a package-private static helper with no access modifier, never `private` if the other class uses it).
- No `synchronized` anywhere (the JDK `CharArrayWriter` uses `synchronized (lock)` blocks).
- Before the runtime files exist, every test fails with a message naming `java/io/StringWriter` or
  `java/io/CharArrayWriter`: that only means the classes are missing or unregistered. All tests share one
  fixture, so one missing class fails all seven tests.
- Calibration: the reference passed kit check after 1 run (about 72 new lines: StringWriter 29, CharArrayWriter
  43); `kit guard` was clean and the full bytecode suite still passed. Mutations checked against the test:
  dropping `StringWriter.write(String)` (javac error above), a range check before growth in
  `CharArrayWriter.write(String, int, int)` (`charArrayStringFailures`), growing to exactly `newcount`
  (`charArrayGrowthByOne`) and passing `toCharArray()` to `writeTo` (`charArrayWriteTo`) each fail.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
stringWriterWritesAndSizes, stringWriterAppendsAndBuffer, stringWriterRanges, charArrayWriterGrowth, charArrayWriterWrites, charArrayWriterCopiesAndWriteTo, charArrayWriterSubclassCalls: if the message says `link a complete acyclic type hierarchy: java/io/StringWriter` (or `java/io/CharArrayWriter`) or `unsupported ... java/io/StringWriter.<name>`, a class is missing, not registered, or lacks that member: run `javaw-agent-kit/bin/kit register java/io/StringWriter.java` and `javaw-agent-kit/bin/kit register java/io/CharArrayWriter.java` once each, and give the class a public method with exactly that name, parameters and return type (append returns StringWriter / CharArrayWriter); if it says `Cannot build bytecode exception runtime`, javac rejected a runtime file: fix the quoted line (no synchronized, no Objects.checkFromIndexSize, `import java.util.Arrays;`; `unreported exception IOException` in StringWriter.append means write(String) is not overridden: add `public void write(String str) {buf.append(str);}`).
stringWriterSizes: `StringWriter(int)` starts with `if (initialSize < 0) throw new IllegalArgumentException("Negative buffer size");` (no number in the message), then `buf = new StringBuffer(initialSize); lock = buf;`.
stringWriterWrites, stringWriterNulls: write(int) is `buf.append((char) c)`; write(String) is `buf.append(str)` (null appends `null`); write(String, off, len) is `buf.append(str, off, off + len)` (null counts as "null", so (null,1,2) writes `ul`); do not override write(char[]): Writer's version calls write(cbuf, 0, cbuf.length) and a null array throws NullPointerException.
stringWriterAppends, charArrayAppends: append(CharSequence) is `write(String.valueOf(csq))` (StringWriter) or `String s = String.valueOf(csq); write(s, 0, s.length())` (CharArrayWriter) and returns `this`; append(csq, start, end) is `if (csq == null) csq = "null"; return append(csq.subSequence(start, end));`; append(char) is `write(c); return this;`; the return type is the class itself, not Writer.
stringWriterBuffer: getBuffer() returns the field `buf` itself (no copy, same object every call), and toString() is `buf.toString()` so changes made through getBuffer() show up; both constructors must assign `lock = buf;` (the subclass checks `lock == getBuffer()`).
stringWriterFlushClose, charArrayFlushClose: flush() and close() have empty bodies and set no closed flag; writes after close() must still work.
stringWriterCharRanges, charArrayCharRanges: write(char[], off, len) starts with `int length = c.length; if ((off | len) < 0 || len > length - off) throw new IndexOutOfBoundsException("Range [" + off + ", " + off + " + " + len + ") out of bounds for length " + length);` before writing anything; never compute `off + len` (it overflows for (1, Integer.MAX_VALUE)); then `if (len == 0) return;`.
stringWriterStringRanges: StringWriter.write(String, off, len) must not check the range itself: `buf.append(str, off, off + len)` throws IndexOutOfBoundsException; append(csq, start, end) lets `csq.subSequence(start, end)` throw StringIndexOutOfBoundsException.
stringWriterSubclassCalls: StringWriter must override write(String) (logged `ws(fg)`, not `wso(fg,0,2)`), append(CharSequence) must call write(String), append(csq,start,end) must call append(CharSequence), append(char) must call write(int); do not override write(char[]).
charArrayCapacities, charArrayGrowthByOne, charArrayGrowthByArray: the no-argument constructor is `this(32)`; every write grows with `int newcount = count + n; if (newcount > buf.length) buf = Arrays.copyOf(buf, Math.max(buf.length << 1, newcount));` (from 0: 1,2,4,4,8; from 3, writing 10 chars gives exactly 10); never grow when newcount <= buf.length.
charArraySizes: `CharArrayWriter(int)` starts with `if (initialSize < 0) throw new IllegalArgumentException("Negative initial size: " + initialSize);` then `buf = new char[initialSize];` (0 is allowed).
charArrayFields: `buf` and `count` must be `protected` fields named exactly `buf` and `count`, and every method (size, toString, toCharArray, write, append) must read them each time, because the subclass assigns `buf = new char[]{'x','y','z'}; count = 3;` itself.
charArrayWrites: write(int) stores `(char) c` (0x10042 stores 'B'); write(char[], off, len) copies with System.arraycopy; write(String, off, len) uses `str.getChars(off, off + len, buf, count)`; do not override write(String) or write(char[]).
charArrayStringFailures: CharArrayWriter.write(String, off, len) has no range check of its own: grow for count + len FIRST, then `str.getChars(off, off + len, buf, count)` throws StringIndexOutOfBoundsException (so ("abc", 1, 5) on capacity 2 with one char leaves capacity 6, size 1); write((String) null) must throw NullPointerException from the inherited Writer.write(String).
charArrayCopiesAndReset: toCharArray() returns `Arrays.copyOf(buf, count)` (a fresh array, never buf); reset() only sets `count = 0` and keeps buf.
charArrayWriteTo, charArrayWriteToFailures: writeTo(out) is exactly one call `out.write(buf, 0, count);` passing the internal array (the recorder prints `[32,0,3:abc]`, `[4,0,0:]` for an empty capacity-4 writer); no null check of its own and no catch, so null throws NullPointerException and the IOException propagates.
charArraySubclassCalls: CharArrayWriter.append(CharSequence) calls write(s, 0, s.length()) (logged `wso(ab,0,2)`), append(csq,start,end) calls append(CharSequence), append(char) calls write(int); do not override write(String) or write(char[]) (Writer's versions call write(str, 0, len) and write(cbuf, 0, len)); lock must stay `this`.
```

## Docs

Create `docs/bytecode-string-writer.md` with this skeleton (fill in the brackets):

    # Bytecode StringWriter and CharArrayWriter

    Revision `string-writer-vN` links `java.io.StringWriter` and `java.io.CharArrayWriter` in the bundled exception
    runtime. Like `Writer`, they keep the JDK `lock` field but never enter a monitor (single-threaded contract).

    Contract:

    - <StringWriter: constructors and the negative-size message, write forms and null handling, append forms
      returning this, live getBuffer(), flush/close as no-ops>
    - <CharArrayWriter: protected buf/count, growth rule, write range checks and messages, write(String,int,int)
      failures, writeTo, append, reset, toCharArray, size, toString, flush/close as no-ops>

    Evidence: `BytecodeStringWriterTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
    fresh instances), including subclasses that record the overridable calls each method makes.
