# T31 String and StringBuilder range failure messages

```kit
id: T31
slug: string-range-messages
title: give String, StringBuilder and StringBuffer index and range failures the JDK 21 exception messages
tests: BytecodeStringRangeMessagesTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/lang/String.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/lang/StringBuilder.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/lang/StringIndexOutOfBoundsException.java
allow: javaw-bytecode/src/main/resources/exception-runtime/javaw/runtime/TextCharsets.java
allow: docs/bytecode-string-range-messages.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-string-range-messages-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeStringRangeMessagesTest.java`.

## Goal

`java.lang.String`, `StringBuilder` and `StringBuffer` already work in the runtime and already throw the right
exception **types** in almost every case, but most failures have **no message** (`getMessage()` is `null`) or a
bare number (`"5"`). Libraries log, wrap and compare these messages. The pinned libraries call these members
thousands of times (`runtime-target/target/audit/member-references.tsv`): `String.charAt` 1408,
`String.substring(int,int)` 1033, `substring(int)` 979, `String(char[],int,int)` 76, `getChars` 34,
`String(byte[],int,int,Charset)` 31, `codePointAt` 27, `repeat` 12, `subSequence` 9; `StringBuilder.setLength` 90,
`append(CharSequence,int,int)` 88 (H2 `Tokenizer.readCharacterString`, Hibernate
`AbstractPostgreSQLStructJdbcType$PostgreSQLAppender`), `charAt` 52, `setCharAt` 50, `insert(int,String)` 32,
`append(char[],int,int)` 17, `replace` 17, `delete` 15, `insert(int,char)` 15, `appendCodePoint` 14,
`substring(int,int)` 13, `deleteCharAt` 11. AspectJ bcel (`classfile/Utility`, `generic/Type`), H2
`RegexpFunction` and Hibernate `SubSequence` name `StringIndexOutOfBoundsException` directly
(`external-types.tsv`). `StringWriter`, `CharArrayWriter`, `PrintWriter` and `Writer` reach these checks through
`StringBuffer.append(CharSequence,int,int)`, `String.getChars` and `subSequence`.

This task changes **only exception messages** (and one exception type, `String(int[],int,int)`). No new public
members, no new classes. You add five package-private static helpers to `String` and change the throw statements
listed below.

## Members to implement

### New package-private helpers in `String.java` (no modifier: not `public`, not `private`)

Put them right after `public char charAt(int index)`. `StringBuilder` (same package `java.lang`) calls them as
`String.checkIndex(...)` and so on.

```java
static void checkIndex(int index,int length){
    if(index<0 || index>=length)throw new StringIndexOutOfBoundsException("Index "+index+" out of bounds for length "+length);
}
static void checkOffset(int offset,int length){
    if(offset<0 || offset>length)throw new StringIndexOutOfBoundsException(rangeMessage(offset,length,length));
}
static void checkBoundsBeginEnd(int begin,int end,int length){
    if(begin<0 || begin>end || end>length)throw new StringIndexOutOfBoundsException(rangeMessage(begin,end,length));
}
static void checkBoundsOffCount(int offset,int count,int length){
    if(offset<0 || count<0 || offset>length-count)
        throw new StringIndexOutOfBoundsException("Range ["+offset+", "+offset+" + "+count+") out of bounds for length "+length);
}
static String rangeMessage(int begin,int end,int length){return "Range ["+begin+", "+end+") out of bounds for length "+length;}
```

### `StringIndexOutOfBoundsException.java`

- `public StringIndexOutOfBoundsException(int index)`: change `super(String.valueOf(index))` to
  `super("String index out of range: "+index)`. Leave the other two constructors alone.

### `String.java` method bodies

Keep every other statement of each body as it is; replace only the check named here.

- `charAt(int index)`: `checkIndex(index,value.length);`
- `codePointAt(int index)`: no change (it calls `charAt`, which now has the message)
- `codePointBefore(int index)`: `checkIndex(index-1,length());` (note `index-1`)
- `codePointCount(int begin,int end)`: `if(begin<0 || begin>end || end>length())throw new IndexOutOfBoundsException(rangeMessage(begin,end,length()));` (plain `IndexOutOfBoundsException`, not the String one)
- `substring(int start,int end)`: `checkBoundsBeginEnd(start,end,length());` (`substring(int)` and `subSequence` call it)
- `getChars(int start,int end,char[] destination,int offset)`: `checkBoundsBeginEnd(start,end,length());checkBoundsOffCount(offset,end-start,destination.length);` then the existing `System.arraycopy`
- `getBytes(int start,int end,byte[] destination,int offset)` (deprecated): first check `checkBoundsBeginEnd(start,end,length());`, keep the `null` check, second check `checkBoundsOffCount(offset,count,destination.length);`
- `String(char[] chars,int offset,int count)`: `checkBoundsOffCount(offset,count,length);` (`valueOf(char[],int,int)` and `copyValueOf(char[],int,int)` call it)
- `String(byte[] bytes,int highByte,int offset,int count)` (deprecated): `checkBoundsOffCount(offset,count,length);`
- `decodeUtf8(byte[] bytes,int offset,int count)` (private, used by `String(byte[],int,int)`): `checkBoundsOffCount(offset,count,length);`
- `String(int[] codePoints,int offset,int count)`: `checkBoundsOffCount(offset,count,sourceLength);` (this changes the type from `IndexOutOfBoundsException` to `StringIndexOutOfBoundsException`, like the JDK), and the invalid code point throw becomes `throw new IllegalArgumentException(String.valueOf(code));`
- `repeat(int count)`: `if(count<0)throw new IllegalArgumentException("count is negative: "+count);`

`offsetByCodePoints(int,int)` stays exactly as it is: the JDK throws `IndexOutOfBoundsException` with a `null`
message there too, and the test checks that.

### `javaw/runtime/TextCharsets.java`

- In `public static String decode(byte[] bytes,int offset,int count,int charset)`, the first check: keep the
  condition `offset<0 || count<0 || offset>length-count` and throw
  `new StringIndexOutOfBoundsException("Range ["+offset+", "+offset+" + "+count+") out of bounds for length "+length)`.
  This is the check behind `String(byte[],int,int,Charset)` and `String(byte[],int,int,String)`. It is a different
  package, so it cannot call `String.checkBoundsOffCount`; write the message inline.

### `StringBuilder.java` method bodies (`StringBuffer` delegates to `StringBuilder`; do not edit `StringBuffer.java`)

- `charAt(int index)`: `String.checkIndex(index,count);`
- `setCharAt(int index,char ch)`: `String.checkIndex(index,count);`
- `deleteCharAt(int index)`: `String.checkIndex(index,count);`
- `setLength(int length)`: no change: it keeps `new StringIndexOutOfBoundsException(length)`, which now says `String index out of range: -1`
- `append(CharSequence text,int start,int end)`: after `if(text==null)text="null";`: `int textLength=text.length();if(start<0 || start>end || end>textLength)throw new IndexOutOfBoundsException(String.rangeMessage(start,end,textLength));` (call the local `textLength`, not `length`: the body already declares `int length=end-start;` below it)
- `append(char[] chars,int offset,int length)`: `int end=offset+length;if(offset<0 || offset>end || end>chars.length)throw new IndexOutOfBoundsException(String.rangeMessage(offset,end,chars.length));`
- `delete(int start,int end)`: `if(end>count)end=count;String.checkBoundsBeginEnd(start,end,count);` (clamp FIRST, then check)
- `replace(int start,int end,String text)`: `if(end>count)end=count;String.checkBoundsBeginEnd(start,end,count);` (clamp first; keep `text.length()` after the check)
- `insert(int offset,String text)`: `String.checkOffset(offset,count);`
- `insert(int offset,char[] text)`: `String.checkOffset(offset,count);`
- `insert(int offset,char[] text,int start,int size)`: `String.checkOffset(offset,count);String.checkBoundsBeginEnd(start,start+size,text.length);`
- `insertSequence(int offset,CharSequence text,int start,int end)` (private): after `if(text==null)text="null";`: `String.checkOffset(offset,count);int sourceLength=text.length();if(start<0 || start>end || end>sourceLength)throw new IndexOutOfBoundsException(String.rangeMessage(start,end,sourceLength));`
- `substring(int start,int end)`: `String.checkBoundsBeginEnd(start,end,count);`
- `getChars(int start,int end,char[] target,int offset)`: `String.checkBoundsBeginEnd(start,end,count);int stop=offset+(end-start);if(offset<0 || offset>stop || stop>target.length)throw new IndexOutOfBoundsException(String.rangeMessage(offset,stop,target.length));` then the existing `System.arraycopy`
- `appendCodePoint(int code)`: `if(code<0 || code>1114111)throw new IllegalArgumentException("Not a valid Unicode code point: 0x"+Integer.toHexString(code).toUpperCase(java.util.Locale.ROOT));`

The members that already delegate get the new messages without edits: `substring(int)`, `subSequence`,
`insert(int,char)`, `insert(int,CharSequence)`, `insert(int,CharSequence,int,int)`, `append(CharSequence)`,
`append(char[])`, `codePointAt`, `codePointBefore`, `codePointCount` (they go through `toString()`), and every
`StringBuffer` method.

## Members to leave out

- No new public or protected members anywhere. The five helpers are package-private (no modifier).
- Do not edit `StringBuffer.java`, `Character.java` (`Character.toChars(-1)` keeps its current message),
  `IndexOutOfBoundsException.java` (its `int` constructor already says `Index out of range: 4`, like the JDK), the
  writers, or `java/util/Objects.java`. `Objects.checkIndex`, `checkFromToIndex` and `checkFromIndexSize` are not in
  the runtime: do not add them and do not call them.
- `new StringBuilder(-1)` (`NegativeArraySizeException`) and the `NullPointerException` messages (helpful NPE text
  such as `Cannot invoke "String.length()"`) are out of scope; the test checks only the NPE type.

## JDK behaviour to match

`n` is `length()` of the String, `count` of the builder, or the length of the array or `CharSequence` argument.
Three message formats, all verified on JDK 21:

| Format | Text | Used by |
|---|---|---|
| index | `Index <i> out of bounds for length <n>` | `String.charAt`, `codePointAt`, `codePointBefore` (with `i = index-1`), `StringBuilder.charAt`, `setCharAt`, `deleteCharAt`, `codePointAt`, `codePointBefore` |
| begin/end | `Range [<begin>, <end>) out of bounds for length <n>` | `substring`, `subSequence`, `getChars` (source), deprecated `getBytes` (source), `codePointCount`, builder `delete`, `replace`, `insert` offsets (`Range [<offset>, <count>) out of bounds for length <count>`), `insert(int,char[],int,int)` (`[start, start+size)`), `append`/`insert` of a `CharSequence` range, `append(char[],int,int)` (`[offset, offset+len)`), builder `getChars` destination (`[offset, offset+n)`) |
| offset/count | `Range [<offset>, <offset> + <count>) out of bounds for length <n>` | `String(char[],int,int)`, `valueOf(char[],int,int)`, `copyValueOf`, `String(byte[],int,int)` with or without a charset, deprecated `String(byte[],int,int,int)`, `String(int[],int,int)`, `String.getChars` destination, deprecated `getBytes` destination |

Exception types (keep them exactly):

- `StringIndexOutOfBoundsException` for everything above **except**: `String.codePointCount` and
  `StringBuilder.codePointCount`, `append(CharSequence,int,int)`, `append(char[],int,int)`, the `CharSequence`
  range check of `insert(int,CharSequence,int,int)`, and the **destination** check of `StringBuilder.getChars`.
  Those throw a plain `IndexOutOfBoundsException`. Note `String.getChars` destination failures are
  `StringIndexOutOfBoundsException` (offset/count format) but `StringBuilder.getChars` destination failures are
  `IndexOutOfBoundsException` (begin/end format): `"abc".getChars(0,3,new char[2],0)` gives
  `StringIndexOutOfBoundsException:Range [0, 0 + 3) out of bounds for length 2`, while
  `new StringBuilder("abc").getChars(0,3,new char[2],0)` gives
  `IndexOutOfBoundsException:Range [0, 3) out of bounds for length 2`.
- `setLength(-1)`: `StringIndexOutOfBoundsException:String index out of range: -1`; `setLength(Integer.MIN_VALUE)`:
  `StringIndexOutOfBoundsException:String index out of range: -2147483648`.
- `offsetByCodePoints` (String and builder): `IndexOutOfBoundsException:null`, unchanged.

Examples on `"abc"` / `new StringBuilder("abc")` (all from JDK 21):

- `charAt(5)` -> `Index 5 out of bounds for length 3`; `charAt(Integer.MIN_VALUE)` ->
  `Index -2147483648 out of bounds for length 3`; `"".charAt(0)` -> `Index 0 out of bounds for length 0`.
- `codePointBefore(0)` -> `Index -1 out of bounds for length 3`; `codePointBefore(4)` -> `Index 3 out of bounds for length 3`.
- `codePointCount(2,1)` -> `IndexOutOfBoundsException:Range [2, 1) out of bounds for length 3`.
- `substring(4)` -> `Range [4, 3) out of bounds for length 3`; `substring(-1)` -> `Range [-1, 3) ...`;
  `substring(5,4)` -> `Range [5, 4) ...`; `substring(3)` and `substring(3,3)` are `""`.
- `getChars(0,4,new char[1],7)`: the source is checked first -> `Range [0, 4) out of bounds for length 3`.
  `getChars(0,3,dst5,3)` -> `Range [3, 3 + 3) out of bounds for length 5`; a negative destination offset
  `getChars(0,3,dst5,-1)` -> `Range [-1, -1 + 3) out of bounds for length 5` (the deprecated `getBytes` gives the
  same text). Nothing is copied on failure.
- `new String(xyz,1,Integer.MAX_VALUE)` -> `Range [1, 1 + 2147483647) out of bounds for length 3` (no overflow:
  the check is `offset>length-count`, never `offset+count`); `new String(xyz,4,0)` -> `Range [4, 4 + 0) ...`;
  `new String(xyz,3,0)` is `""`.
- `new String(bytes,2,2,StandardCharsets.UTF_8)` (any charset, also `"UTF-16LE"` by name) ->
  `Range [2, 2 + 2) out of bounds for length 3`. With a `null` Charset the `NullPointerException` comes first,
  even when the range is also bad.
- `new String(new int[]{97,-1},0,2)` -> `IllegalArgumentException:-1`; `{0x110000}` -> `IllegalArgumentException:1114112`;
  `new String(new int[]{-1},0,2)` -> the range failure first (`StringIndexOutOfBoundsException:Range [0, 0 + 2) out of bounds for length 1`).
- `"ab".repeat(-1)` -> `IllegalArgumentException:count is negative: -1`; `"".repeat(Integer.MIN_VALUE)` ->
  `IllegalArgumentException:count is negative: -2147483648`. Keep the `count<0` check FIRST, before the
  `count==1` and `length()==0` fast paths, or `""` would be returned.
- Builder `delete(4,5)` -> `Range [4, 3) out of bounds for length 3` (end was clamped to 3 first);
  `delete(1,9)` -> `"a"`; `delete(4,2)` -> `Range [4, 2) ...`; `delete(3,9)` -> `"abc"`. `replace` is the same:
  `replace(1,9,"XY")` -> `"aXY"`, `replace(4,5,"XY")` -> `Range [4, 3) ...`, and `replace(-1,1,null)` throws the
  range failure, not a `NullPointerException`.
- Builder `insert(4,"x")`, `insert(4,'x')`, `insert(4,char[])`, and `StringBuffer` `insert(4,7)`,
  `insert(5,1.5)` -> `Range [4, 3) out of bounds for length 3` (`[5, 3)` for 5); `insert(-1,"x")` and
  `StringBuffer.insert(-1,(Object)"o")` -> `Range [-1, 3) out of bounds for length 3`. (The runtime `StringBuilder`
  has no `insert(int,int)` or `insert(int,Object)`; `StringBuffer` turns those into `insert(int,String)`. Do not add
  them.)
- `insert(1,xyz,1,Integer.MAX_VALUE)` -> `StringIndexOutOfBoundsException:Range [1, -2147483648) out of bounds for length 3`
  and `append(xyz,1,Integer.MAX_VALUE)` -> `IndexOutOfBoundsException:Range [1, -2147483648) out of bounds for length 3`:
  here `start+size` / `offset+len` **is** computed as an `int` and wraps, exactly like the JDK.
  `append(xyz,1,-1)` -> `Range [1, 0) ...`; `append(xyz,4,0)` -> `Range [4, 4) ...`; `append(xyz,3,0)` is fine.
- `append("xy",1,3)` -> `IndexOutOfBoundsException:Range [1, 3) out of bounds for length 2`;
  `append((CharSequence)null,0,5)` -> `Range [0, 5) out of bounds for length 4` (null is `"null"`);
  `append((CharSequence)null,1,3)` appends `"ul"`.
- `insert(1,"xy",1,3)` -> `IndexOutOfBoundsException:Range [1, 3) out of bounds for length 2`;
  `insert(9,(CharSequence)null,0,9)` -> the offset first: `StringIndexOutOfBoundsException:Range [9, 3) out of bounds for length 3`.
- Builder `getChars(1,3,dst5,4)` -> `IndexOutOfBoundsException:Range [4, 6) out of bounds for length 5`;
  `getChars(0,4,dst1,9)` -> the source first: `StringIndexOutOfBoundsException:Range [0, 4) out of bounds for length 3`.
- `appendCodePoint(-1)` -> `IllegalArgumentException:Not a valid Unicode code point: 0xFFFFFFFF`; `0x110000` ->
  `0x110000`; `Integer.MIN_VALUE` -> `0x80000000`; `0xABCDEF` -> `0xABCDEF` (upper-case hex, no padding).
- `new StringIndexOutOfBoundsException(7).getMessage()` is `String index out of range: 7`; the `String` and no-arg
  constructors are unchanged (`m`, `null`).
- A failed call leaves the builder unchanged (`abc`).

Calls a `CharSequence` argument sees (the test records them with its own `CharSequence` class; `L` is `length()`,
`c<i>` is `charAt(i)`), matching JDK 21 call for call:

- `append(seq,start,end)`: `length()` **once, always, before** the range check (also for `start<0`), then
  `charAt(i)` once for each `i` from `start` to `end-1`. A failure records only `L`. `append(r,1,3)` -> `Lc1c2`.
- `insert(offset,seq,start,end)`: the offset check first (with a bad offset `seq` sees **no** call), then
  `length()` once, then the range check, then `charAt(i)` per char.
- `insert(offset,seq)` (no range): `insert(offset,seq,0,seq.length())`, so `length()` twice on success
  (`LLc0c1c2c3`) and once with a bad offset (`L`). For a non-String `seq`, `insert(int,CharSequence)` calls
  `seq.length()` BEFORE the offset check (it is the argument of the delegation), which is why `insert(7,r)` records
  `L` while `insert(7,r,0,1)` records nothing. The runtime already does this; do not change it.

Writers reach these checks (no writer edits needed): `StringWriter.write("abc",2,2)` ->
`IndexOutOfBoundsException:Range [2, 4) out of bounds for length 3` (from `StringBuffer.append(CharSequence,int,int)`);
`StringWriter.append("abc",2,1)` -> `StringIndexOutOfBoundsException:Range [2, 1) ...` (from `subSequence`);
`StringWriter.append(null,1,5)` -> `StringIndexOutOfBoundsException:Range [1, 5) out of bounds for length 4` (null is
`"null"`; from `subSequence`); every writer `append(CharSequence,int,int)` failure is a
`StringIndexOutOfBoundsException` from `subSequence` (`PrintWriter.append("abc",2,1)` -> `Range [2, 1) ...`);
`CharArrayWriter.write("abc",1,5)` -> `StringIndexOutOfBoundsException:Range [1, 6) ...` (from `getChars`) and
`CharArrayWriter.write("abc",-1,1)` -> `StringIndexOutOfBoundsException:Range [-1, 0) out of bounds for length 3`,
with nothing written;
a `Writer` subclass's `write("abcdef",2,9)` -> `StringIndexOutOfBoundsException:Range [2, 11) out of bounds for length 6` before its
`write(char[],int,int)` is called; `PrintWriter.write("abc",1,5)` over a `StringWriter` ->
`IndexOutOfBoundsException:Range [1, 6) out of bounds for length 3`.

## Steps

1. Read the test `javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeStringRangeMessagesTest.java`.
2. Open `javaw-bytecode/src/main/resources/exception-runtime/java/lang/StringIndexOutOfBoundsException.java` and
   change the `int` constructor as described.
3. Open `javaw-bytecode/src/main/resources/exception-runtime/java/lang/String.java`. Add the five helpers right
   after `charAt`, then change each body in the String table, one `Edit` per method. The file has dense one-line
   methods: search for the exact signature (for example `public String substring(int start,int end){`), then
   replace only its `if(...)throw new ...;` statement.
4. Open `javaw-bytecode/src/main/resources/exception-runtime/javaw/runtime/TextCharsets.java` and change the throw in
   `decode(byte[],int,int,int)`.
5. Open `javaw-bytecode/src/main/resources/exception-runtime/java/lang/StringBuilder.java` and change each body in
   the StringBuilder table.
6. No registration: all four files are already registered. Do not run `kit register`.
7. `javaw-agent-kit/bin/kit check T31` until PASS.
8. Write `docs/bytecode-string-range-messages.md` (see Docs), the report, then `javaw-agent-kit/bin/kit finish T31`.

## Hints

- This is a message-only edit of existing methods plus five small helpers. If you are adding public methods or
  new files, stop and re-read the tables.
- Build messages with string `+`. `String.format`, `Objects.checkIndex`, `Objects.checkFromToIndex`,
  `Objects.checkFromIndexSize` and `jdk.internal.util.Preconditions` do not exist in the runtime.
- The helpers must have **no** access modifier. `private` breaks `StringBuilder` (javac error), and `public` would
  add a member the JDK `String` does not have.
- Two different range formats: the begin/end form `Range [2, 1)` and the offset/count form `Range [1, 1 + 5)`
  (with ` + ` and spaces). Use `checkBoundsOffCount` only for the members the String table names with it.
- Order matters: `delete` and `replace` clamp `end` to `count` **before** the check (so `delete(4,5)` reports
  `[4, 3)`); `getChars` checks the source range before the destination; `insert` checks the offset before the
  `CharSequence` range; `String(int[],...)` checks the range before the code point values.
- In `append(CharSequence,int,int)` read `text.length()` into a new local `int textLength` **before** the `if`
  (not `length`: that name is already taken by `int length=end-start;` further down). The JDK always calls
  `length()` once, and the old `start<0 || end<start || end>text.length()` skipped it when `start<0`
  (`builderRecordingSequence` sees `|L|` there).
- `append(char[],int,int)` and `insert(int,char[],int,int)` must compute `end` as an `int` sum and compare
  `offset>end`; that is what makes `(1, Integer.MAX_VALUE)` report `Range [1, -2147483648)`. Do not "fix" it with
  long arithmetic. `String(char[],int,int)` is the opposite: offset/count format, `offset>length-count`, no sum.
- `codePointBefore(index)` reports `index-1`. `StringBuilder.codePointAt/Before/Count` go through `toString()`;
  `toString()` has the same length as `count`, so they need no change.
- `Integer.toHexString` returns lower case: add `.toUpperCase(java.util.Locale.ROOT)` in `appendCodePoint`.
- `TextCharsets` is in package `javaw.runtime`: it cannot see package-private `String` helpers.
- The fixtures fill destination arrays with `.` so a partial copy shows up (`..bc`); a failed call must copy
  nothing, which holds as long as every check runs before `System.arraycopy`.
- The Wasm runner stops at the **first** failing fixture of each `@Test` method, so after a fix another fixture of
  the same group may fail next. That is progress.
- Sizes of the calibration reference: about 27 added lines in `String.java` (14 helper lines plus 13 edited lines:
  the 13 bullets of the String list, where `getChars` holds two checks on one line), 21 in `StringBuilder.java`,
  1 in `StringIndexOutOfBoundsException.java`, 2 in `TextCharsets.java`.
- Calibration: the full suite passed with the new messages; existing tests check only the exception types.
- Calibration: the reference passed kit check after 1 run (about 50 new lines).

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
stringIndexFailures, stringSubstringAndGetCharsRanges, stringConstructorRanges, builderIndexFailures, builderDeleteReplaceSubstringAndGetChars, builderInsertAndAppendRanges, codePointAndRepeatArguments, writersReachTheseChecks: if FIX THE RUNTIME says the compiler rejected the fixture or `Cannot build bytecode exception runtime`, a helper is private/missing or you called a member that is not in the runtime (Objects.checkIndex, String.format, Preconditions). The five helpers in String.java have no modifier; StringBuilder calls them as String.checkIndex(...). No class needs registering in this task.
charAtPastEnd, charAtLength, charAtNegative, charAtMinValue, charAtEmpty, codePointAtPastEnd, codePointAtNegative: String.charAt must call `checkIndex(index,value.length);` which throws `new StringIndexOutOfBoundsException("Index "+index+" out of bounds for length "+length)`. A Wasm value like `StringIndexOutOfBoundsException:5` or `StringIndexOutOfBoundsException:String index out of range: 5` means charAt still uses the int constructor.
codePointBeforeZero, codePointBeforePastEnd, codePointBeforeInBounds: String.codePointBefore(index) checks `checkIndex(index-1,length())`, so codePointBefore(0) says `Index -1 out of bounds for length 3` and codePointBefore(4) says `Index 3 ...`.
codePointCountInverted, codePointCountNegative, codePointCountPastEnd: String.codePointCount throws a plain `IndexOutOfBoundsException(rangeMessage(begin,end,length()))` (not the String one) when `begin<0 || begin>end || end>length()`.
offsetByCodePointsKeepsNullMessages: offsetByCodePoints must keep `IndexOutOfBoundsException` with NO message; undo any message you added there.
stringIndexExceptionConstructors, builderSetLengthNegative, builderSetLengthMinValue, bufferIndexFailures: `StringIndexOutOfBoundsException(int index)` is `super("String index out of range: "+index)`; setLength keeps using that constructor.
substringFromPastEnd, substringFromNegative, substringInverted, substringNegativeBegin, substringEndPastLength, substringBothPastEnd, subSequenceInverted, subSequencePastEnd, substringEdges: String.substring(int,int) is `checkBoundsBeginEnd(start,end,length());` with the message `Range [begin, end) out of bounds for length n`; keep the `return this` fast path and `substring(int)` delegating with `length()` as end.
getCharsSourceRange, getCharsDestinationRange, getCharsSourceCheckedFirst, getCharsEmptyRangeAtEnd: String.getChars is `checkBoundsBeginEnd(start,end,length());checkBoundsOffCount(offset,end-start,destination.length);` then arraycopy. The destination message is the offset/count form `Range [3, 3 + 3) out of bounds for length 5`.
deprecatedGetBytesRanges: String.getBytes(int,int,byte[],int): first `checkBoundsBeginEnd(start,end,length())`, then the null check, then `checkBoundsOffCount(offset,count,destination.length)`.
charsOffsetCount, valueOfAndCopyValueOf: String(char[],int,int) is `checkBoundsOffCount(offset,count,length);` with `offset<0 || count<0 || offset>length-count` (never offset+count, so (1,MAX_VALUE) prints `1 + 2147483647`).
bytesOffsetCount: String(byte[],int,int) goes through the private decodeUtf8: use `checkBoundsOffCount(offset,count,length);` there.
bytesWithCharset, bytesWithCharsetName, bytesNullCharsetBeforeRange: the charset constructors check in javaw/runtime/TextCharsets.decode(byte[],int,int,int); throw `new StringIndexOutOfBoundsException("Range ["+offset+", "+offset+" + "+count+") out of bounds for length "+length)` there. Keep the null-Charset NullPointerException first (TextCharsets.identifier runs before decode).
deprecatedHighByte: String(byte[],int,int,int) uses `checkBoundsOffCount(offset,count,length);`.
codePointsOffsetCount, codePointsInvalidValue: String(int[],int,int) uses `checkBoundsOffCount(offset,count,sourceLength);` (StringIndexOutOfBoundsException, offset/count form) BEFORE the loop, and an invalid code point throws `new IllegalArgumentException(String.valueOf(code))`.
repeatNegative: String.repeat throws `new IllegalArgumentException("count is negative: "+count)` when count<0.
builderCharAt, builderCharAtAfterSetLength, builderSetCharAt, builderDeleteCharAt, builderCodePoints: StringBuilder.charAt, setCharAt and deleteCharAt call `String.checkIndex(index,count);` (count, not value.length). codePointAt/Before/Count go through toString() and need no change once String is fixed.
builderSetLengthValid: setLength must still grow with zero chars; only the exception message changed.
builderDelete, builderReplace, bufferRangeFailures: delete and replace start with `if(end>count)end=count;` and only then `String.checkBoundsBeginEnd(start,end,count);` (delete(4,5) is `Range [4, 3)`, delete(1,9) is `a`, replace(1,9,"XY") is `aXY`).
builderReplaceRangeBeforeNull, builderReplaceNullInRange: in replace, keep the range check before `text.length()`: replace(-1,1,null) is the range failure, replace(1,2,null) a NullPointerException.
builderSubstring: StringBuilder.substring(int,int) is `String.checkBoundsBeginEnd(start,end,count);`.
builderGetChars, builderGetCharsDestination: StringBuilder.getChars checks the source with `String.checkBoundsBeginEnd(start,end,count)` (StringIndexOutOfBoundsException), then `int stop=offset+(end-start);` and `if(offset<0 || offset>stop || stop>target.length)throw new IndexOutOfBoundsException(String.rangeMessage(offset,stop,target.length));` (plain IndexOutOfBoundsException, begin/end form `Range [4, 6)`).
builderInsertOffset: every insert offset check is `String.checkOffset(offset,count)`, message `Range [offset, count) out of bounds for length count` (insert(-1,"x") is `Range [-1, 3)`). Fix insert(int,String), insert(int,char[]), insert(int,char[],int,int) and insertSequence.
builderInsertCharArrayRange: insert(int,char[],int,int) is `String.checkOffset(offset,count);String.checkBoundsBeginEnd(start,start+size,text.length);` with the int sum (so (1,MAX_VALUE) prints `Range [1, -2147483648)`), StringIndexOutOfBoundsException.
builderInsertSequenceRange, builderRecordingSequence: insertSequence is `if(text==null)text="null";String.checkOffset(offset,count);int sourceLength=text.length();` then `if(start<0 || start>end || end>sourceLength)throw new IndexOutOfBoundsException(String.rangeMessage(start,end,sourceLength));` (offset first, so a bad offset makes no call on the sequence).
builderAppendSequenceRange, builderRecordingSequence: append(CharSequence,int,int) is `if(text==null)text="null";int textLength=text.length();if(start<0 || start>end || end>textLength)throw new IndexOutOfBoundsException(String.rangeMessage(start,end,textLength));` with length() read BEFORE the if, so append(r,-1,2) still records `L`. Do not call the local `length`: the body already has `int length=end-start;`.
builderAppendCharArrayRange, bufferInsertAppendFailures: append(char[],int,int) is `int end=offset+length;if(offset<0 || offset>end || end>chars.length)throw new IndexOutOfBoundsException(String.rangeMessage(offset,end,chars.length));` (int sum on purpose: (1,MAX_VALUE) prints `Range [1, -2147483648)`, (1,-1) prints `Range [1, 0)`).
appendCodePointInvalid, bufferAppendCodePointInvalid: appendCodePoint throws `new IllegalArgumentException("Not a valid Unicode code point: 0x"+Integer.toHexString(code).toUpperCase(java.util.Locale.ROOT))` when `code<0 || code>1114111`.
stringWriterWriteStringRange, stringWriterAppendRange, charArrayWriterStringRange, writerSubclassStringRange, printWriterStringRange: do not edit the writers; these pass once StringBuilder.append(CharSequence,int,int), String.subSequence/substring and String.getChars have the messages above.
```

## Docs

Create `docs/bytecode-string-range-messages.md` with this skeleton (fill in the brackets):

    # Bytecode String range failure messages

    Revision `string-range-messages-vN` gives `String`, `StringBuilder` and `StringBuffer` index and range failures
    the JDK 21 exception messages in the bundled exception runtime.

    Contract:

    - <the three message formats and which members use each>
    - <which members throw plain IndexOutOfBoundsException instead of StringIndexOutOfBoundsException>
    - <check order: delete/replace clamp first, getChars source before destination, insert offset before range>
    - <setLength, appendCodePoint, String(int[],int,int) code point and repeat messages; offsetByCodePoints keeps null>
    - Known differences: `NullPointerException` messages and `new StringBuilder(-1)` are not matched.

    Evidence: `BytecodeStringRangeMessagesTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
    orders, two fresh instances), including the `length()`/`charAt` calls a `CharSequence` argument receives and the
    writer paths (`StringWriter`, `CharArrayWriter`, `PrintWriter`, a `Writer` subclass).
