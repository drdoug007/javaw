# Bytecode HexFormat

Revision `hex-format-v273` links `java.util.HexFormat` in the bundled exception runtime.

Contract:

- Formatting: `formatHex(bytes)` / `formatHex(bytes, from, to)` render each byte in `[from, to)` as
  `prefix + 2 hex digits + suffix`, with `delimiter` written **between** bytes only (never after the
  last). Digits are lowercase `0-9a-f` by default; `withUpperCase()` switches to `0-9A-F`. An empty
  range gives `""`, and `toHexDigits` prints big-endian with all leading zeros (`toHexDigits(255)` =
  `000000ff`, `toHexDigits((short)-2)` = `fffe`, `toHexDigits('A')` = `0041`).
- Parsing: `parseHex(string)` / `parseHex(string, from, to)` accept upper and lower case digits. Empty
  input gives an empty array. With no delimiter/prefix/suffix, an odd length throws
  `IllegalArgumentException("string length not even: " + length)`. Otherwise, with
  `value = prefix.length() + 2 + suffix.length()` and `stride = value + delimiter.length()`,
  `(length - value) % stride != 0` throws `IllegalArgumentException("extra or missing delimiters or
  values consisting of prefix, two hexadecimal digits, and suffix")`; then the literal text is checked
  (prefix at 0, suffix at the end, `suffix + delimiter + prefix` only **between** values) and a mismatch
  throws `IllegalArgumentException("found: \"...\", expected: \"...\", index: n ch: m")` where `m` is the
  first wrong character.
- Digit conversion: `toLowHexDigit(v)` is the digit of `v & 0xf`, `toHighHexDigit(v)` of `(v >> 4) & 0xf`.
  `toHexDigits(long, digits)` returns the low `digits` hex digits for `digits` in 0..16 (`0` gives `""`),
  else `IllegalArgumentException("number of digits: " + digits)`. `isHexDigit(ch)` is true exactly where
  `fromHexDigit` succeeds; `fromHexDigit(ch)` maps `0-9`, `a-f`, `A-F` to 0..15 and throws
  `NumberFormatException("not a hexadecimal digit: \"c\" = n")` for anything else (tested on the raw `int`,
  so `-1`, `256`, `0xff10`, `'a'+256` are not digits). `fromHexDigits` / `fromHexDigitsToLong` parse up to
  8 / 16 digits respectively (else `IllegalArgumentException("string length greater than N: m")`); an empty
  range gives 0 and `ffffffff` gives `-1` (no overflow check).
- Failure messages: `formatHex`, `parseHex` and the `fromHexDigits*` range forms all check
  `from < 0 || from > to || to > length` and throw
  `IndexOutOfBoundsException("Range [from, to) out of bounds for length L")`; a null array/string throws
  `NullPointerException`; the `fromHexDigits*` range forms check it first with the message `string`, as the JDK does. `delimiter`, `prefix` and `suffix` are non-null, enforced by
  `Objects.requireNonNull` with the bare field name as message.
- Equality: `equals` compares class, `upperCase`, `delimiter`, `prefix` and `suffix`; `hashCode` is
  `31 * Objects.hash(delimiter, prefix, suffix) + Boolean.hashCode(upperCase)`. `of()` always returns the
  same static instance. `toString` is
  `uppercase: b, delimiter: "d", prefix: "p", suffix: "s"` with every `\n` / `\r` in the whole result
  escaped to a literal `\n` / `\r`; the `found:` parse message is escaped the same way.

The `formatHex(Appendable, ...)` overloads, `toHexDigits(Appendable, byte)` and
`parseHex(char[], int, int)` are deliberately left out: they need `java.io.UncheckedIOException` and
`java.nio.CharBuffer`, which the runtime does not have, and `leavesAppendableAndCharArrayOverloadsUnlinked`
confirms they are rejected before WAT generation.

Evidence: `BytecodeHexFormatTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two fresh
instances). The `Appendable` overloads and `parseHex(char[],int,int)` are rejected before WAT generation.
