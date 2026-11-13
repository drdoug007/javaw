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

## Docs

Create `docs/bytecode-hex-format.md` with this skeleton (fill in the brackets):

    # Bytecode HexFormat

    Revision `hex-format-vN` links `java.util.HexFormat` in the bundled exception runtime.

    Contract:

    - <formatting, parsing, digit conversion, failure messages, equality>

    Evidence: `BytecodeHexFormatTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two fresh
    instances). The `Appendable` overloads and `parseHex(char[],int,int)` are rejected before WAT generation.
