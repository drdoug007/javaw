# T23 Properties.load

```kit
id: T23
slug: properties-load
title: link Properties.load(Reader) and Properties.load(InputStream)
tests: BytecodePropertiesLoadTest
depends: T22, T20, T18
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/Properties.java
allow: docs/bytecode-properties-load.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-properties-load-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodePropertiesLoadTest.java`.

## Goal

Add the two `load` methods to the runtime `java.util.Properties` (task T22), so that a `.properties` text can be
parsed from a `java.io.Reader` (T20) or a `java.io.InputStream` (T18, read as ISO-8859-1). In the pinned libraries
(`runtime-target/target/audit/member-references.tsv`) `Properties.load(InputStream)` has 31 call sites in 12
artifacts (logback `PropertyModelHandler`, Jackson `VersionUtil.mavenVersionFor`, H2 `DocumentedMBean`, HikariCP
`HikariConfig.loadProperties`, Log4j `PropertiesUtil.loadClose`, Tomcat `DefaultInstanceManager` and 7 more in
Tomcat, AspectJ `Version.<clinit>`, Hibernate `ConfigLoader`, Spring `PropertiesEditor`...) and
`Properties.load(Reader)` is called by Spring `DefaultPropertiesPersister`. `java/util/Properties` itself is named by
282 classes (`external-types.tsv`).

The whole parser is the JDK's: a private nested `LineReader` that turns the input into logical lines, a `load0`
loop that splits each line into key and value, and `loadConvert`, which resolves escapes. Copy its structure
**exactly**: the order and number of `read` calls on the source are observable by a `Reader`/`InputStream`
subclass, and the test records them. The JDK methods are `synchronized`; here they are plain methods.

## Members to implement

All in the existing `javaw-bytecode/src/main/resources/exception-runtime/java/util/Properties.java`. Add the imports
`import java.io.IOException; import java.io.InputStream; import java.io.Reader;` (keep the existing
`import java.util.function.*;`). Do not change any existing member.

- `public void load(Reader reader) throws IOException`
- `public void load(InputStream inStream) throws IOException`
- `private void load0(LineReader lr) throws IOException`
- `private static String loadConvert(char[] in, int off, int len, StringBuilder out)`
- `private static final class LineReader` (a nested class: it needs no registration) with
  - fields `char[] lineBuf = new char[1024];`, `InputStream inStream`, `Reader reader`, `byte[] inByteBuf`,
    `char[] inCharBuf`, `int inLimit = 0`, `int inOff = 0`. Declare them as plain (non-`final`) fields. If you make
    any of them `final`, each constructor must assign every `final` field, including `null` for the unused source and
    buffer, or javac reports an unassigned final field.
  - `LineReader(InputStream inStream)` (sets `inByteBuf = new byte[8192]`) and `LineReader(Reader reader)` (sets
    `inCharBuf = new char[8192]`)
  - `int readLine() throws IOException`

## Members to leave out

- `store(Writer, String)`, `store(OutputStream, String)` and `save`: they write the current date, which the runtime
  must not read (RULES.md). The test checks that `Properties.store(Writer,String)` stays unlinked: do not declare any
  `store` method.
- `list(PrintStream)`, `list(PrintWriter)`, `loadFromXML`, `storeToXML`: out of scope for this task (no PrintStream
  or XML in the runtime). Do not declare them even though `java/io/PrintWriter.java` exists in the runtime. T22
  already checks `loadFromXML`.

## JDK behaviour to match

Read `unzip -p ~/.sdkman/candidates/java/21.0.2-graalce/lib/src.zip java.base/java/util/Properties.java` from
`public synchronized void load(Reader reader)` down to the end of `loadConvert` (about 250 lines including
comments). Write the same logic in your own words; the rules below are what the test checks.

**`load(Reader reader)`**: `Objects.requireNonNull(reader, "reader parameter is null");` then
`load0(new LineReader(reader));`. **`load(InputStream inStream)`**: `Objects.requireNonNull(inStream, "inStream
parameter is null");` then `load0(new LineReader(inStream));`. Neither closes the source, neither clears the existing
entries, and the `defaults` are never touched (entries go into this object with `put`).

**`LineReader.readLine()`** returns the length of the next logical line (stored in `lineBuf[0..len)`) or `-1` at the
end. Its exact algorithm (locals `len = 0`, `off = inOff`, `limit = inLimit`, flags `skipWhiteSpace = true`,
`appendedLineBegin = false`, `precedingBackslash = false`), repeated in `while (true)`:

1. Refill when `off >= limit`: `inLimit = limit = (inStream != null ? inStream.read(inByteBuf) : reader.read(inCharBuf));`
   that is the **one-argument** `read(byte[])` / `read(char[])` with the 8192 buffer, every time. If `limit <= 0`
   (so **a source returning 0 counts like -1**): return `-1` when `len == 0`, else `precedingBackslash ? len - 1 : len`.
   Otherwise `off = 0`.
2. Take `c = inStream != null ? (char) (inByteBuf[off++] & 0xFF) : inCharBuf[off++];` (the `& 0xFF` is the
   ISO-8859-1 decoding: byte `0xE9` is char 233 (e with acute accent), never 65513).
3. If `skipWhiteSpace`: `continue` for `' '`, `'\t'`, `'\f'`; also `continue` for `'\r'`/`'\n'` unless
   `appendedLineBegin`; otherwise set both `skipWhiteSpace` and `appendedLineBegin` to false. Only those three chars
   are whitespace: vertical tab `'\u000b'` (11) and no-break space `'\u00a0'` (160) are ordinary key chars, so
   the line `v`, char 11, `w=1` has key `v<11>w` (as `Recorder` logs it), and a line that starts with char 11 keeps
   it in the key (char 11 then `z=3` has key `<11>z`).
4. If `len == 0` and `c` is `'#'` or `'!'`: a comment. Consume chars until a `'\r'` or `'\n'`, refilling (with the
   same `read(buf)` call) whenever the buffer is used up; if such a refill returns `<= 0`, return `-1`. Then
   `skipWhiteSpace = true; continue;`. A backslash at the end of a comment does not continue it.
5. If `c` is not `'\n'`/`'\r'`: `lineBuf[len++] = c;` growing `lineBuf` when full (for example
   `lineBuf = Arrays.copyOf(lineBuf, len * 2)`; lines longer than 1024 chars are tested), then
   `precedingBackslash = (c == '\\') ? !precedingBackslash : false;` and continue.
6. Otherwise (end of a natural line): if `len == 0`, `skipWhiteSpace = true; continue;` (blank line). Else, if
   `off >= limit`, refill **now** (before looking at the backslash): `inLimit = limit = read(buf); off = 0;` and if
   `limit <= 0` return `precedingBackslash ? len - 1 : len`. Then, if `precedingBackslash` (an odd number of
   trailing backslashes): `len -= 1; skipWhiteSpace = true; appendedLineBegin = true; precedingBackslash = false;`
   and if `c == '\r'` and the next buffered char is `'\n'`, skip it (`off++`); continue. Else `inOff = off; return len;`.
   The refill must come before the `'\r'`-then-`'\n'` check, so that a CR-LF split across two reads is still one
   line end: the test reads the Java string `"a=1\r\nb=2\\\r\n c\r\n#x\\\r\nd\\\r"` 1, 2, 3 and 5 chars per call
   and expects `[a]=[1] [b]=[2c] [d]=[]` each time.

Consequences the test checks (all verified on JDK 21):

- A source that returns its data then `-1` is read 3 times for `"a=1\n"` (read 1 returns the data; read 2 is the
  step-6 refill at the `'\n'` and returns -1, so readLine returns 3; read 3 comes from the next `readLine` and returns
  -1 again) and 3 times for `"a=1"` (read 2 is the ordinary step-1 refill after `'1'`, which returns -1, so readLine
  returns 3; read 3 comes from the next `readLine`). The extra read happens because `inOff`/`inLimit` are not updated
  on an end-of-input return. `""` makes 1 read and `"#only"` makes 2. A `Reader` subclass that logs `read(char[])` as `a<len>`
  and `read(char[],int,int)` as `len@off` sees `a8192 8192@0` per call; never call `read(cbuf, 0, n)` or `read()`
  directly.
- A `0` return in the middle splits or ends the input: on `"a=1\n\nb=2\n"` delivered one char per call, a `0` on call
  2 gives the puts `[a]=[] []=[1] [b]=[2]`, a `0` on call 6 gives only `[a]=[1]` and leaves `b=2` unread (5 chars
  consumed, 6 calls). The test tries a `0` on every call from 1 to 8; two more examples: a `0` on call 1 ends `load`
  with no puts at all (0 chars consumed, 1 call); a `0` on call 3 gives `[a]=[] [1]=[] [b]=[2]` (9 chars consumed,
  12 calls).
- The `InputStream` path reads the same way: `"a=1\r\n#\u00e9\nb=\\\n  2"` (here `\u00e9` is the Java escape for
  char 233) delivered at most 3 bytes per call makes 7 calls to `read(byte[])`, each logged `a8192 8192@0`, puts
  `[a]=[1] [b]=[2]` and never closes the stream.
- An `IOException` from the source propagates, and a line is put only after `readLine` returned it: for
  `"a=1\nb=2\n"` in chunks of 4 chars, failing on read call 2 puts **nothing** (the refill at the end of `a=1` threw),
  failing on call 3 puts `a=1` only.
- Line ends: `\n`, `\r` and `\r\n` all end a line, and a final line without terminator counts
  (`"a=1\rb=2\r\nc=3\n\nd=4\n\re=5\r\rf=6"` gives 6 entries). After a continuation, `\r\n` counts as one line end but
  `\n\r` does not: `"f=1\\\n\r  2\n"` (a backslash, LF, CR) gives `f=1` and then a key `2` with value `""`.
- Continuation: `"a=one\\\n    two\\\n\tthree"` gives `onetwothree` (leading whitespace of each continued line is
  dropped). Two backslashes at the end are an escaped backslash: `"b=x\\\\\n"` gives `x\`; three continue:
  `"c=x\\\\\\\ny"` gives `x\y`. A continuation followed by an empty line ends the value (`"f=1\\\n\n g=2"` gives
  `f=1` and `g=2`); a backslash at the end of the input is dropped (`"h=1\\"` gives `h=1`); a continued line starting
  with `#` is not a comment when `len > 0` (`"i=\\\n#not a comment"` gives `i=#not a comment`). In this list `\\` means one
  backslash char in the input, as in a Java string literal.

**`load0(lr)`**: `StringBuilder outBuffer = new StringBuilder();` then
`while ((limit = lr.readLine()) >= 0)`:

- Find the key end: `keyLen = 0; valueStart = limit; hasSep = false; precedingBackslash = false;` and while
  `keyLen < limit`: let `c = lr.lineBuf[keyLen]`; if `c` is `'='` or `':'` and not escaped, `valueStart = keyLen + 1;
  hasSep = true; break;`; if `c` is `' '`, `'\t'` or `'\f'` and not escaped, `valueStart = keyLen + 1; break;`;
  then `precedingBackslash = (c == '\\') ? !precedingBackslash : false; keyLen++;`.
- Skip to the value: while `valueStart < limit`: `c = lr.lineBuf[valueStart]`; if `c` is not whitespace, then if
  `!hasSep && (c == '=' || c == ':')` set `hasSep = true` (one separator after whitespace is swallowed), else break;
  `valueStart++`.
- `String key = loadConvert(lr.lineBuf, 0, keyLen, outBuffer); String value = loadConvert(lr.lineBuf, valueStart,
  limit - valueStart, outBuffer); put(key, value);`, the **virtual** `put` (the test's `Properties` subclass logs
  every put in order; duplicate keys are put again and the last one wins).

Examples: `"  key  =  value  "` gives `key`/`value  ` (trailing spaces stay), `"k = = v"` gives `k`/`= v`,
`"k4 :=x"` gives `k4`/`=x`, `"k5 x y"` gives `k5`/`x y`, `"lonely"` gives `lonely`/`""`, `"=v"` gives `""`/`v`,
`":"` and `"   =   "` give `""`/`""`.

**`loadConvert(in, off, len, out)`** returns the chars `in[off..off+len)` with escapes resolved (`out` is a scratch
buffer: `out.setLength(0)` before use). A backslash takes the next char `c`:

- `u`: if fewer than 4 chars remain (`off > end - 4`, where `off` is just after the `u`) throw
  `new IllegalArgumentException("Malformed \\uxxxx encoding.")` (the message text is `Malformed \uxxxx encoding.`).
  Otherwise read exactly 4 hex digits (`0-9`, `a-f`, `A-F`) into a char; any other char throws the same exception.
  In the `.properties` text, `\u0041` is `A`, `\u20AC` is char 8364, `\uabcdz` is char 43981 followed by `z`, and
  `\u00410` is `A` followed by `0` (exactly 4 hex digits are read; the rest is ordinary text). Here `\u` means a
  backslash char followed by `u` in the input. The key is converted before the
  value, so a bad escape in either throws before `put`, and the earlier lines stay loaded (`"a=1\nb=\\u12"` puts
  `a=1`, then throws).
- `t`, `r`, `n`, `f`: tab, CR, LF, form feed.
- anything else (including `\\`, `=`, `:`, space, `#`, `!`, `b`, `"`): that char itself. So `"a\\=b\\:c\\ d=e"` has
  key `a=b:c d`, `"k=\\ lead"` has value ` lead`, `"\\#notcomment=1"` has key `#notcomment`, `"\\b"` is `b`.

There is no index check after the backslash: the line reader never leaves an odd trailing backslash.

## Steps

1. Read `javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodePropertiesLoadTest.java`. Note `Recorder`
   (logs each `put(key, value)` as `[key]=[value]`, chars below 32 or above 126 shown as `<code>`), `Chunks` (a
   `Reader` that returns at most `step` chars per call, logs both read methods, and can return 0 or throw on a given
   call) and `ByteChunks` (the same for `InputStream`).
2. Read the JDK source named above and `javaw-bytecode/src/main/resources/exception-runtime/java/util/Properties.java`.
3. Add the imports and the members listed above to `Properties.java`. Do not change the existing members.
4. No registration is needed (`Properties` is registered by T22, and `LineReader` is nested). Do not run
   `kit register`.
5. `javaw-agent-kit/bin/kit check T23` until PASS.
6. Write `docs/bytecode-properties-load.md` (below), the report, then `javaw-agent-kit/bin/kit finish T23`.

## Hints

- `Objects.requireNonNull(Object, String)`, `StringBuilder.setLength`, `StringBuilder.append(char)`,
  `Arrays.copyOf(char[], int)`, `Reader.read(char[])` and `InputStream.read(byte[])` exist in the runtime.
- Write the hex digit test with `if`/`else`; do not use a `switch` with `->` arms copied from the JDK. Do not use
  `jdk.internal.util.ArraysSupport` (it is not in the runtime); grow `lineBuf` with `Arrays.copyOf`.
- Do not write the words `synchronized`, `volatile`, `native` or `Thread` anywhere in the file, not even in comments.
- The two `LineReader` constructors leave the other source `null`; test `inStream != null` (as the JDK's
  `fromStream`) to choose the buffer. Keep `inLimit` and `inOff` as fields: the next `readLine` starts from them.
- `inOff` is only stored on the normal `return len` in step 6. The end-of-input returns leave it stale, which is why
  the JDK reads once more after the end; the test counts those calls. Do not "fix" it.
- In the comment loop, compare the **decoded** char (or the signed byte) with `'\r'` and `'\n'`; bytes `0x8D` or `0x8A`
  are not line ends.
- Do not skip whitespace while parsing the key: leading whitespace was already removed by `readLine`.
- The Wasm runner stops at the first failing fixture of each `@Test` method, so after a fix another fixture of the
  same group may fail next. That is progress.
- Mutations of the reference were run against the test: refilling with `read(buf, 0, 8192)` instead of the
  one-argument `read(buf)` fails `readerCallPattern`; decoding a byte as `(char) b` without `& 0xFF` fails
  `streamLatin1`; treating only `-1` (not `0`) as the end of input (`limit < 0`) fails `readerReturnsZero`.
- Calibration: the reference passed kit check after 1 run (about 130 new lines).

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
lineFormat, lineEndsAndContinuations, escapesAndFailures, inputStreamIsLatin1, longLinesAndLongInput, sourceCallPatterns, leavesStoreUnlinked: class missing or not registered, or member missing. `Properties` is registered by T22; if FIX THE RUNTIME says `unsupported dynamic call Recorder.load(Ljava/io/Reader;)V` (or `Properties.load`, or `load(Ljava/io/InputStream;)V`) `; link a matching instance method in the declaring hierarchy`, the load methods are missing or have another signature: add `public void load(Reader reader) throws IOException` and `public void load(InputStream inStream) throws IOException` exactly; if it says `Cannot build bytecode exception runtime`, fix the javac error it quotes (missing `import java.io.Reader;` and friends, or a `switch` copied from the JDK). leavesStoreUnlinked fails only if Properties.java declares a `store` method: delete it.
separators, whitespaceAroundValue, emptyKeysAndValues: load0 is wrong: the key ends at the first unescaped `=`, `:`, space, tab or form feed; then skip spaces/tabs/form feeds and swallow at most ONE `=` or `:` (only if the key did not already end at one); the value is the rest, trailing spaces included. Only ' ', '\t', '\f' are whitespace.
comments, commentOnlyInput, continuationOfComment: a line whose first char after leading whitespace (len == 0) is `#` or `!` is a comment up to the next `\r` or `\n`, and a trailing backslash does not continue it; a comment that reaches the end of the input makes readLine return -1. A `#` after a continuation (len > 0) is ordinary text.
duplicateKeys, loadKeepsExistingEntries: load0 calls the virtual `put(key, value)` once per line in input order (duplicates are put again); load never clears the table and never touches `defaults`.
lineEnds, finalLineWithoutTerminator, continuationLineEnds: `\r`, `\n` and `\r\n` end a line; blank lines (len == 0) are skipped; at the end of the input return the pending line (`precedingBackslash ? len - 1 : len`). After a continuation a `\r` followed by `\n` is skipped as one line end, but a `\n` followed by `\r` is a continuation followed by an empty line, which ends the value.
continuation, backslashCounts, continuationEdgeCases: precedingBackslash flips on every `\\` (`precedingBackslash = c == '\\' ? !precedingBackslash : false`), so only an ODD number of trailing backslashes continues the line; the continuation drops that last backslash and sets skipWhiteSpace = true and appendedLineBegin = true, so leading ' ', '\t', '\f' of the next line are dropped but a line end right after it ends the value.
escapes, escapedTerminators: loadConvert maps `\t` `\r` `\n` `\f` to tab, CR, LF, form feed and every other escaped char to itself (`\b` is `b`, `\\` is one backslash, `\=` `\:` `\ ` `\#` `\!` are literal); escaped terminators stay in the key.
unicodeEscapes, malformedUnicode, malformedInKeyBeforeValue, streamMalformed: after `\u`, if `off > end - 4` (fewer than 4 chars left in the key or value) or any of the 4 chars is not 0-9, a-f, A-F, throw `new IllegalArgumentException("Malformed \\uxxxx encoding.")` (Java source text; the message is `Malformed \uxxxx encoding.`); convert the key before the value; lines already put stay put.
nullSources: load(Reader) starts with `Objects.requireNonNull(reader, "reader parameter is null")` and load(InputStream) with `Objects.requireNonNull(inStream, "inStream parameter is null")`.
streamLatin1, streamFormat, streamHighByteComment: the InputStream path decodes each byte as `(char) (b & 0xFF)` (ISO-8859-1, never UTF-8 and never a sign-extended `(char) b`), so 0xE9 is 233 and the UTF-8 bytes C3 A9 are two chars 195 169; bytes 0x8D and 0x8A inside a comment are not line ends.
longValue, manyLines: lineBuf starts at 1024 chars and must grow (Arrays.copyOf to twice the length) when full; a line or a CRLF may straddle the 8192-char buffer boundary, so keep off/limit in fields between readLine calls.
readerCallPattern, readerSmallChunks, streamCallPattern: refill only with `reader.read(inCharBuf)` / `inStream.read(inByteBuf)` (the one-argument read with the 8192 buffer, logged as `a8192 8192@0`), only when `off >= limit`, and at the end of a natural line refill BEFORE checking for a continuation; `inOff = off` is stored only on the normal return, so "a=1\n" makes 3 read calls, "" 1 and "#only" 2. load never calls close().
readerReturnsZero, readerThrows: a refill that returns 0 is treated exactly like -1 (`limit <= 0`): return -1 if len == 0, else the pending line; the next readLine reads again. An IOException from read propagates out of load; a line is put only after readLine returned it.
readerLeftOpen: load must not close or drain the source beyond what it read: a StringReader stays open (ready() is true) and a ByteArrayInputStream is at its end afterwards.
```

## Docs

Create `docs/bytecode-properties-load.md` with this skeleton (fill in the brackets):

    # Bytecode Properties.load

    Revision `properties-load-vN` links `Properties.load(Reader)` and `Properties.load(InputStream)` in the bundled
    exception runtime, with the JDK line reader (8192-char/byte buffer, ISO-8859-1 for streams).

    Contract:

    - <logical lines: comments, blank lines, leading whitespace, line ends, continuation rules>
    - <key/value split, separators, escapes and the malformed \uxxxx failure>
    - <source call pattern: one-argument read, extra reads at the end, 0 treated as end, no close, put per line>

    Not linked: `store`, `save` (they need the current date), `list`, `loadFromXML`, `storeToXML`.

    Evidence: `BytecodePropertiesLoadTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders,
    two fresh instances). `Properties.store(Writer,String)` is rejected before WAT generation.
