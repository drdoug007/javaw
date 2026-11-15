# Bytecode String range failure messages

Revision `string-range-messages-v293` gives `String`, `StringBuilder` and `StringBuffer` index and range failures
the JDK 21 exception messages in the bundled exception runtime.

Contract:

- Three message formats, and the members that use each:
  - `Index <i> out of bounds for length <n>` - `StringIndexOutOfBoundsException` from `String.charAt` and
    `String.codePointBefore`, and from `StringBuilder.charAt`, `setCharAt` and `deleteCharAt` (all via
    `String.checkIndex(index,length)`).
  - `Range [<begin>, <end>) out of bounds for length <n>` - `StringIndexOutOfBoundsException` from
    `String.substring(int,int)`, `String.getChars`, the deprecated `String.getBytes(int,int,byte[],int)`,
    `StringBuilder.delete`, `replace`, `substring(int,int)`, `insert(int,char[],int,int)` and the source
    check of `StringBuilder.getChars` (via `String.checkBoundsBeginEnd`); the same format with a plain
    `IndexOutOfBoundsException` from `String.codePointCount` and from `StringBuilder.append(CharSequence,int,int)`,
    `append(char[],int,int)`, the range check of `insertSequence` and the destination check of
    `StringBuilder.getChars` (via `String.rangeMessage`).
  - `Range [<offset>, <offset> + <count>) out of bounds for length <n>` (the ` + ` is literal in the
    message) - `StringIndexOutOfBoundsException` from the `String(char[],int,int)`, deprecated
    `String(byte[],int,int,int)` and `String(int[],int,int)` constructors, the range check of
    `TextCharsets.decode(byte[],int,int,int)`, and the destination checks of `String.getChars` and the
    deprecated `String.getBytes` (via `String.checkBoundsOffCount`).
- Plain `IndexOutOfBoundsException` (not the `String` subclass): `String.codePointCount`,
  `StringBuilder.append(CharSequence,int,int)`, `StringBuilder.append(char[],int,int)`, the range check of
  `StringBuilder.insertSequence` (and therefore `insert(int,CharSequence,int,int)`), and the destination
  check of `StringBuilder.getChars`.
- Check order: `delete` and `replace` clamp `end` to `count` before the range check; `String.getChars` and
  `StringBuilder.getChars` check the source before the destination (the source always as
  `StringIndexOutOfBoundsException`; the destination as `StringIndexOutOfBoundsException` in the offset/count
  form for `String.getChars` and as plain `IndexOutOfBoundsException` in the begin/end form for
  `StringBuilder.getChars`); every `insert` checks
  the destination offset before the source range; `String(int[],int,int)` checks the range before code point
  values; `repeat` checks `count<0` before the `count==1`/empty fast paths; `append(char[],int,int)` and
  `insert(int,char[],int,int)` compute `end` as an int sum, so overflow wraps (`(1,Integer.MAX_VALUE)`
  reports `Range [1, -2147483648)`).
- `setLength` throws `StringIndexOutOfBoundsException` with `String index out of range: <n>`;
  `appendCodePoint` throws `IllegalArgumentException` with `Not a valid Unicode code point: 0x<HEX>`
  (uppercase hex, no padding); `String(int[],int,int)` throws `IllegalArgumentException` whose message is the
  offending code point, and throws `StringIndexOutOfBoundsException` (not plain `IndexOutOfBoundsException`)
  for its range; `repeat` throws `IllegalArgumentException` with `count is negative: <count>`, and
  `OutOfMemoryError` with `Required length exceeds implementation limit` when `Integer.MAX_VALUE / count` is
  below the JDK 21 compact-string byte length (the char length, doubled when any char is above `0xFF`),
  before allocating;
  `offsetByCodePoints` keeps its message-less `IndexOutOfBoundsException`.
- Known differences: `NullPointerException` messages and `new StringBuilder(-1)` are not matched.

Evidence: `BytecodeStringRangeMessagesTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
orders, two fresh instances), including the `length()`/`charAt` calls a `CharSequence` argument receives and the
writer paths (`StringWriter`, `CharArrayWriter`, `PrintWriter`, a `Writer` subclass).
