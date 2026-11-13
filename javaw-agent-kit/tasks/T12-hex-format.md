# T12 HexFormat

```kit
id: T12
slug: hex-format
title: link java.util.HexFormat
tests: BytecodeHexFormatTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/HexFormat.java
allow: docs/bytecode-hex-format.md
```

## Goal

Add `java.util.HexFormat`, an immutable formatter between bytes and hexadecimal text. Spring Boot uses it
(`RandomValuePropertySource.getRandomBytes` calls `withLowerCase`/`formatHex`, `ApplicationTemp.getPath` calls
`withUpperCase`, `PemPrivateKeyParser$EncodedOid.of` calls `parseHex`), and the HMAC task T15 uses
`HexFormat.of().formatHex(...)` to print digests.

## Members to implement

One file, `java/util/HexFormat.java`: `public final class HexFormat`, private fields
`String delimiter, prefix, suffix; boolean upperCase`, one private constructor.

- `static HexFormat of()` (always the same instance), `static HexFormat ofDelimiter(String delimiter)`
- `HexFormat withDelimiter(String)`, `withPrefix(String)`, `withSuffix(String)`, `withUpperCase()`, `withLowerCase()`
- `String delimiter()`, `String prefix()`, `String suffix()`, `boolean isUpperCase()`
- `String formatHex(byte[] bytes)`, `String formatHex(byte[] bytes, int fromIndex, int toIndex)`
- `byte[] parseHex(CharSequence string)`, `byte[] parseHex(CharSequence string, int fromIndex, int toIndex)`
- `char toLowHexDigit(int value)`, `char toHighHexDigit(int value)`
- `String toHexDigits(byte)`, `toHexDigits(char)`, `toHexDigits(short)`, `toHexDigits(int)`, `toHexDigits(long)`,
  `toHexDigits(long value, int digits)`
- `static boolean isHexDigit(int ch)`, `static int fromHexDigit(int ch)`
- `static int fromHexDigits(CharSequence)`, `static int fromHexDigits(CharSequence, int fromIndex, int toIndex)`
- `static long fromHexDigitsToLong(CharSequence)`, `static long fromHexDigitsToLong(CharSequence, int, int)`
- `boolean equals(Object)`, `int hashCode()`, `String toString()`

## Members to leave out

`formatHex(Appendable, byte[])`, `formatHex(Appendable, byte[], int, int)`, `toHexDigits(Appendable, byte)` (they
need `java.io.UncheckedIOException`, which the runtime does not have) and `parseHex(char[], int, int)` (needs
`java.nio.CharBuffer`). The test checks that these stay unlinked.

## JDK behaviour to match

- Digits: lowercase `0-9a-f` by default, `withUpperCase()` gives `0-9A-F`. Each byte is two digits
  (`(byte)-1` -> `ff`, `10` -> `0a`). Multi-byte values print big-endian with all leading zeros:
  `toHexDigits(255)` = `000000ff`, `toHexDigits((short)-2)` = `fffe`, `toHexDigits('A')` = `0041`.
- `toHexDigits(long value, int digits)`: the low `digits` hex digits, `digits` 0..16 (`0` gives `""`), else
  `IllegalArgumentException("number of digits: " + digits)`.
- `toLowHexDigit(v)` = digit of `v & 0xf`, `toHighHexDigit(v)` = digit of `(v >> 4) & 0xf`.
- `formatHex`: each byte is written as `prefix + 2 digits + suffix`, and `delimiter` goes **between** bytes only.
  `ofDelimiter(", ").withPrefix("0x").withSuffix("h")` formats `{10,-6}` as `0x0ah, 0xfah`. An empty range gives `""`.
- Range checks (both `formatHex` and `parseHex` and `fromHexDigits*` ranges) throw
  `IndexOutOfBoundsException("Range [" + from + ", " + to + ") out of bounds for length " + length)` when
  `from < 0`, `from > to` or `to > length`. A null array/string throws `NullPointerException`.
- `parseHex` (digits accept upper and lower case):
  - empty input gives an empty array;
  - with no delimiter, prefix or suffix: odd length throws
    `IllegalArgumentException("string length not even: " + length)`;
  - otherwise let `value = prefix.length()+2+suffix.length()` and `stride = value + delimiter.length()`; if
    `(length - value) % stride != 0` throw `IllegalArgumentException("extra or missing delimiters or values
    consisting of prefix, two hexadecimal digits, and suffix")` (one line, single spaces);
  - then check the literal text: prefix at index 0, suffix at the end, and `suffix+delimiter+prefix` between
    values. A mismatch throws `IllegalArgumentException("found: \"" + actualText + "\", expected: \"" + literal +
    "\", index: " + index + " ch: " + (int) firstWrongChar)` where `actualText` is the
    `literal.length()` characters at `index`. Example: `ofDelimiter(":").parseHex("0a-fa")` ->
    `found: "-", expected: ":", index: 2 ch: 45`.
- `fromHexDigit(ch)`: value 0..15 for `0-9`, `a-f`, `A-F`; anything else (including negative and `>= 256`)
  throws `NumberFormatException("not a hexadecimal digit: \"" + (char) ch + "\" = " + ch)`. `NumberFormatException`
  extends `IllegalArgumentException`, so a bad digit inside `parseHex` also throws `NumberFormatException`.
- `isHexDigit(ch)` is true exactly where `fromHexDigit` would succeed.
- `fromHexDigits`: at most 8 digits, else `IllegalArgumentException("string length greater than 8: " + n)`;
  `fromHexDigitsToLong`: at most 16 (`... greater than 16: n`). `""` gives 0. `"ffffffff"` gives `-1` (no overflow check).
- `delimiter`, `prefix`, `suffix` must not be null: `Objects.requireNonNull(x, "delimiter")` (message `prefix`,
  `suffix` for the others).
- `toString()`: `uppercase: false, delimiter: "", prefix: "", suffix: ""` (true/false, then each string in
  quotes). Then every newline character in the result becomes a backslash followed by `n`, and every carriage
  return a backslash followed by `r`: in Java, `s.replace("\n", "\\n").replace("\r", "\\r")`. The
  `found: ...` parse message is escaped the same way.
- `equals`: same class and same upperCase, delimiter, prefix, suffix. `hashCode()`:
  `31 * Objects.hash(delimiter, prefix, suffix) + Boolean.hashCode(upperCase)`.

## Steps

1. Read `javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeHexFormatTest.java`.
2. Create `javaw-bytecode/src/main/resources/exception-runtime/java/util/HexFormat.java` (`package java.util;`).
3. Register it: in `BytecodeExceptionRuntime.java` find the `for(String name:List.of("Iterator","ListIterator",...`
   line that contains `"LinkedList"` and add `"HexFormat",` right after `"LinkedList",`.
4. `javaw-agent-kit/bin/kit check T12` until PASS.
5. Write `docs/bytecode-hex-format.md` (below), the report, then `kit finish T12`.

## Hints

- Keep a `private static final String DIGITS_LOWER = "0123456789abcdef"` (and upper) and pick with `charAt`.
  Build strings with `StringBuilder`.
- `Objects.checkFromToIndex` does **not** exist in the runtime: write a private static `checkRange(from, to, length)`
  that throws the exact message above.
- Implement `toHexDigits(byte/short/int/long)` with one private loop helper (`hex(long value, int digits)`) that fills
  digits from the right with `value >>> 4`; the same helper serves `toHexDigits(long,int)`.
- `fromHexDigit` must test `ch >= 0 && ch < 256` (or just the three ranges) **before** anything else; `'a'+256`
  is not a hex digit.
- `parseHex(string, from, to)`: check the range, then work on `string.subSequence(from, to)`. Use `charAt`, not
  `String` methods, because the argument is a `CharSequence`.
- `of()` returns one static instance (`HexFormat.of()==HexFormat.of()` is true in the test).
- String `replace(CharSequence, CharSequence)` exists; use it for the `\n`/`\r` escaping in `toString`.
- `Objects.hash(Object...)` and `Boolean.hashCode(boolean)` exist. `new String(char[])` exists.
- In `parseHex` with delimiters, after the length check there are `(length - value) / stride + 1` values. Check
  `suffix + delimiter + prefix` only between values (not after the last one).
- Calibration: a straightforward implementation (about 150 lines) passed on the first `kit check`; no existing
  test changes were needed.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
formatsBytes, parsesHexStrings, convertsSingleValuesAndDigits, exposesSettingsEqualityAndNullChecks: if the message says `unsupported static call java/util/HexFormat.of()` (or `unsupported ... call java/util/HexFormat.<name>`), HexFormat is not registered or lacks that member: in BytecodeExceptionRuntime.java (do not edit that file by hand: run `javaw-agent-kit/bin/kit register <path under exception-runtime>` for each new file, which makes exactly this edit) add `"HexFormat",` right after `"LinkedList",` in the java/util `List.of(...)` line, and give HexFormat.java a public method with exactly that name, parameter types and return type; if it says `Cannot build bytecode exception runtime`, javac rejected HexFormat.java: fix the line it quotes (Objects.checkFromToIndex does not exist, and a CharSequence only has length, charAt, subSequence and toString).
rangeFailures, delimited: formatHex(bytes, from, to) starts with a private static `checkRange(from, to, length)` that throws `new IndexOutOfBoundsException("Range [" + from + ", " + to + ") out of bounds for length " + length)` when `from < 0 || from > to || to > length` (do not call Objects.checkFromToIndex, it is not in the runtime); then per byte `if (i > from) out.append(delimiter); out.append(prefix).append(toHighHexDigit(b)).append(toLowHexDigit(b)).append(suffix);` so prefix and suffix wrap EVERY byte (`#0a#fa#00`) and the delimiter goes only between bytes.
parse, parseFailures: parseHex(s, from, to) does checkRange and then parses `s.subSequence(from, to)`; let fromHexDigit's NumberFormatException propagate (never catch or rewrap it); when delimiter, prefix and suffix are all empty, an odd length throws IllegalArgumentException("string length not even: 3"); otherwise first test `(length - value) % stride != 0` (`0a:fa:` must give the `extra or missing delimiters ...` message), then the literals: prefix at index 0, suffix at the end, `suffix + delimiter + prefix` only BETWEEN values; a mismatch gives `found: "(", expected: "[", index: 0 ch: 40`, where index is the character position in the parsed text.
digits, digitCountFailure: never use Integer.toHexString/Long.toHexString (they drop leading zeros and sign-extend a byte to ffffffff); write one helper `hex(long value, int digits)` that fills a `char[digits]` from the right with `toLowHexDigit((int) value); value >>>= 4;` and call it with 2 digits for byte, 4 for char and short, 8 for int, 16 for long; toHexDigits(long, digits) accepts 0..16 (0 gives "") else `IllegalArgumentException("number of digits: " + digits)`; toLowHexDigit(v) is the digit of `v & 0xf`, toHighHexDigit(v) of `(v >> 4) & 0xf` (0x3c gives low `c`, high `3`).
hexDigitChecks, fromHexDigitFailures: fromHexDigit/isHexDigit test the int itself against the three ranges '0'..'9', 'a'..'f', 'A'..'F' with no cast or `& 0xff` first, so -1, 256, 0xff10 and 'a'+256 are NOT digits (do not use Character.digit); anything else throws `new NumberFormatException("not a hexadecimal digit: \"" + (char) ch + "\" = " + ch)` (NumberFormatException, not a plain IllegalArgumentException).
fromDigits, fromDigitsFailures: do not use Integer.parseInt/Long.parseLong (they throw for `ffffffff`, which must return -1); first `checkRange(from, to, string.length())`, then the length limit (`string length greater than 8: 9`, `string length greater than 16: 17` for fromHexDigitsToLong), then loop `value = (value << 4) + fromHexDigit(string.charAt(i))` over from..to; an empty range returns 0.
accessors, equality, nullArguments: toString builds `String s = "uppercase: " + upperCase + ", delimiter: \"" + delimiter + "\", prefix: \"" + prefix + "\", suffix: \"" + suffix + "\"";` and returns `s.replace("\n", "\\n").replace("\r", "\\r")` (escape the WHOLE string, not just the last literal); hashCode is `31 * Objects.hash(delimiter, prefix, suffix) + Boolean.hashCode(upperCase)`; of() returns one static final instance; the private constructor calls `Objects.requireNonNull(delimiter, "delimiter")` (the message is only the field name; same for "prefix" and "suffix").
leavesAppendableAndCharArrayOverloadsUnlinked: this test fails when HexFormat.java declares formatHex(Appendable, byte[]), formatHex(Appendable, byte[], int, int), toHexDigits(Appendable, byte) or parseHex(char[], int, int): delete those methods from HexFormat.java; nothing else needs to change.
```

## Docs

Create `docs/bytecode-hex-format.md` with this skeleton (fill in the brackets):

    # Bytecode HexFormat

    Revision `hex-format-vN` links `java.util.HexFormat` in the bundled exception runtime.

    Contract:

    - <formatting, parsing, digit conversion, failure messages, equality>

    Evidence: `BytecodeHexFormatTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two fresh
    instances). The `Appendable` overloads and `parseHex(char[],int,int)` are rejected before WAT generation.
