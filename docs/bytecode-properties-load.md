# Bytecode Properties.load

Revision `properties-load-v288` links `Properties.load(Reader)` and `Properties.load(InputStream)` in the bundled
exception runtime, with the JDK line reader (8192-char/byte buffer, ISO-8859-1 for streams).

Contract:

- Logical lines: a private nested `LineReader.readLine()` fills `lineBuf` (1024 chars, doubled with
  `Arrays.copyOf` when full) with the next logical line and returns its length, or `-1` at the end of the input.
  It skips leading `' '`, `'\t'` and `'\f'` (and only those: vertical tab 11 and no-break space 160 are ordinary
  key chars), skips blank lines, and treats a line whose first char is `'#'` or `'!'` as a comment: it is consumed
  up to the next `'\r'` or `'\n'`, a backslash at the end of a comment does not continue it, and a comment that
  reaches the end of the input ends the load. A natural line ends at `'\r'`, `'\n'` or `'\r\n'`; after a
  continuation the `'\n'` following a `'\r'` is skipped as part of the one line end, but a `'\n'` followed by a
  `'\r'` is a continuation followed by an empty line, which ends the value. An odd number of trailing backslashes
  continues the line: the last backslash is dropped, leading whitespace of the next line is skipped, and a
  backslash at the very end of the input is dropped.
- Key/value split and escapes: `load0` ends the key at the first unescaped `=`, `:` or `' '`, `'\t'`, `'\f'`
  (a backslash toggles the escape flag as it walks the key), then skips `' '`, `'\t'`, `'\f'` and swallows at most
  one `=` or `:` (only when the key did not already end at one); the value is the rest of the line, trailing
  spaces included. `loadConvert` resolves escapes of the key before the value: `\t`, `\r`, `\n`, `\f` become tab,
  CR, LF, form feed, `\uxxxx` becomes the char of exactly four hex digits (`0-9`, `a-f`, `A-F`) — fewer than four
  chars left after the `u`, or any non-hex char, throws `IllegalArgumentException("Malformed \uxxxx encoding.")`,
  and the key is converted first so a bad escape in either throws before the `put` while earlier lines stay
  loaded — and every other escaped char maps to itself (`\\` is one backslash, `\=`, `\:`, `\ `, `\#`, `\!`, `\b`
  are literals). Each line is inserted with the **virtual** `put(key, value)` in input order (duplicates are put
  again, the last wins), and `load` never clears the table, never touches `defaults`, and a line is put only
  after `readLine` returned it.
- Source call pattern: the source is read only with the one-argument `Reader.read(char[])` /
  `InputStream.read(byte[])` on the 8192-char/byte buffer, and only when the buffer is exhausted (`off >= limit`)
  and — at the end of a natural line — before the continuation is checked, so a CR-LF split across two reads is
  still one line end. A `0` return counts like `-1` (`limit <= 0`): the pending line is returned (or `-1` when
  none), and the next `readLine` reads again, which is why `"a=1\n"` makes 3 read calls, `""` 1 and `"#only"` 2
  (`inOff`/`inLimit` are left stale on an end-of-input return). `load` never closes the source and never calls the
  3-argument or 0-argument `read` forms; an `IOException` from the source propagates out of `load`. Stream bytes
  are decoded as `(char) (b & 0xFF)` (ISO-8859-1: `0xE9` is char 233, never a sign-extended 65513), and the
  comment scan compares the signed byte, so `0x8D`/`0x8A` inside a comment are not line ends.

Not linked: `store`, `save` (they need the current date), `list`, `loadFromXML`, `storeToXML`.

Evidence: `BytecodePropertiesLoadTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders,
two fresh instances). `Properties.store(Writer,String)` is rejected before WAT generation.
