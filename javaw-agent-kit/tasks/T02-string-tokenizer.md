# T02 StringTokenizer

```kit
id: T02
slug: string-tokenizer
title: link java.util.StringTokenizer
tests: BytecodeStringTokenizerTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/StringTokenizer.java
allow: docs/bytecode-string-tokenizer.md
```

## Goal

Add `java.util.StringTokenizer` to the bundled exception runtime. Thirteen pinned artifacts use it
(jackson-databind and H2 among them). Today any bytecode that mentions `StringTokenizer` is rejected because
the class does not exist in the runtime. `java.util.Enumeration` already exists.

## Members to implement

All public members of the JDK class, nothing else public:

    public class StringTokenizer implements Enumeration<Object> {
        public StringTokenizer(String str, String delim, boolean returnDelims)
        public StringTokenizer(String str, String delim)      // = (str, delim, false)
        public StringTokenizer(String str)                    // = (str, " \t\n\r\f", false)
        public boolean hasMoreTokens()
        public String nextToken()
        public String nextToken(String delim)
        public boolean hasMoreElements()                      // = hasMoreTokens()
        public Object nextElement()                           // = nextToken()
        public int countTokens()
    }

## Members to leave out

None. `asIterator()` comes from the existing `Enumeration` default method; do not override it.

## JDK behaviour to match

- Every character of `delim` is a separate one-character delimiter (`",;"` means comma OR semicolon). The
  default set is exactly the five characters space, tab, newline, carriage return, form feed (`" \t\n\r\f"`).
  Other whitespace (vertical tab, no-break space) is NOT a default delimiter.
- A token is a maximal run of non-delimiter characters. Runs of delimiters, leading and trailing delimiters
  never produce empty tokens. `""` input has no tokens. An empty delimiter set `""` makes the whole remaining
  string one token.
- `returnDelims == true`: each delimiter character is returned as its own one-character token (`"a,,b"` gives
  `a`, `,`, `,`, `b`), and `countTokens` counts them too.
- `hasMoreTokens()` and `countTokens()` never advance the position. `countTokens()` counts the tokens that
  `nextToken()` would still return with the current delimiters.
- `nextToken()` with no token left throws `new NoSuchElementException()` (message `null`).
- `nextToken(String delim)`: first replace the delimiter set **permanently**, then behave like `nextToken()`
  from the current position. The current position is just after the previous token, i.e. ON the old
  delimiter. Example: `new StringTokenizer("a,b;c", ",")`, `nextToken()` gives `a`, then `nextToken(";")`
  gives `,b` (the comma is no longer a delimiter), then `c`. This holds even if `hasMoreTokens()` was called
  in between.
- `str == null`: the constructor throws `NullPointerException`. `delim == null` is accepted by the
  constructor; later `hasMoreTokens()`/`nextToken()` throw `NullPointerException`, while `countTokens()` on an
  empty string returns 0. `nextToken(null)` throws `NullPointerException` and leaves the delimiter set null.
  Only the exception class is tested for these, not the message.
- Delimiters are matched per `char`. JDK supplementary-character (surrogate pair) delimiter handling is not
  required and not tested; leave a one-line comment saying so.

## Steps

1. Read `java/util/Enumeration.java` and `java/util/NoSuchElementException.java`.
2. Create `javaw-bytecode/src/main/resources/exception-runtime/java/util/StringTokenizer.java`,
   `package java.util;`, `public class StringTokenizer implements Enumeration<Object>`.
3. Fields: `String str`, `String delimiters`, `boolean returnDelims`, `int position` (index of the next
   unread character).
4. Write two private helpers on positions (both take a start index and return an index):
   - `skipDelimiters(int start)`: if `delimiters == null` throw `new NullPointerException()`; when
     `returnDelims` is false, move past delimiter characters; return the new index.
   - `scanToken(int start)`: move past non-delimiter characters; if `returnDelims` and nothing moved and
     `start < str.length()`, move past exactly one (delimiter) character.
   A character `c` is a delimiter when `delimiters.indexOf(c) >= 0`.
5. `hasMoreTokens()`: `skipDelimiters(position) < str.length()`. `nextToken()`: `start = skipDelimiters(position)`;
   if `start >= str.length()` throw NoSuchElementException; `position = scanToken(start)`; return the substring.
   `nextToken(delim)`: set `delimiters = delim`, then `return nextToken();`. `countTokens()`: loop the two
   helpers on a local copy `index` of the position, checking `index < str.length()` BEFORE each
   `skipDelimiters` call: `while (index < str.length()) { index = skipDelimiters(index); if (index >=
   str.length()) break; index = scanToken(index); count++; }`. Then `new StringTokenizer("", null)
   .countTokens()` returns 0 instead of throwing NullPointerException.
6. Register it: in `javaw-bytecode/src/main/java/one/dastec/javaw/bytecode/BytecodeExceptionRuntime.java`, in the
   `List.of(...)` line that contains `"LinkedList"`, add `"StringTokenizer"` right after `"LinkedList"` (after
   `"StringJoiner"` if T01 is already merged). If that line conflicts with T01 on merge, keep both names:
   `...,"LinkedList","StringJoiner","StringTokenizer",...`.
7. Run `javaw-agent-kit/bin/kit check T02` until it prints PASS, then write the docs file and your report,
   and run `kit finish T02`.

## Hints

- Before you start, `kit check T02` fails with `FIX THE RUNTIME: the bytecode compiler rejected the fixture`
  naming `java/util/StringTokenizer`. That is expected.
- The JDK caches a `newPosition` in `hasMoreTokens()` and has a `delimsChanged` flag. You do NOT need either:
  always skipping delimiters from `position` inside `nextToken()` gives the same results, including the
  delimiter-change cases. Copying the cache is the easiest way to get `nextToken(String)` wrong.
- Do not trim or skip delimiters at the end of `nextToken()`: `position` must stay on the delimiter that ended
  the token, or the `nextToken(String)` example above gives `b` instead of `,b`.
- In `scanToken` with `returnDelims`, guard the extra step with `start < str.length()` (a `charAt` past the end
  throws `StringIndexOutOfBoundsException`).
- `countTokens()` must test the bound before `skipDelimiters` (step 5); a `do/while` fails `nullDelimitersEmptyInputCount`.
- Check `delimiters == null` inside `skipDelimiters` (explicit `throw new NullPointerException()`), not in the
  constructor: the constructor must accept a null `delim`.
- For `str == null`, reading `str.length()` in the constructor (or `Objects.requireNonNull(str)`) is enough.
- `String.indexOf(int)`, `charAt`, `substring` and `length` exist in the runtime `String`. No other class is
  needed.
- If `changeIsPermanent()` shows `a|b|...` where the JVM has `a| b|...`, you skipped delimiters after a token;
  skipping belongs at the start of `nextToken()`/`hasMoreTokens()`, never at the end.
- Calibration: an ~80-line class (four fields, the two helpers above) passed the first `kit check`, and the
  full suite stayed green. No other runtime file needs to change.

## Docs

Create `docs/bytecode-string-tokenizer.md` with this skeleton (fill in the brackets):

    # Bytecode StringTokenizer

    Revision `string-tokenizer-vN` links java.util.StringTokenizer in the bundled exception runtime.

    Contract:

    - <behaviour: delimiters, returnDelims, countTokens, nextToken(String), failures, no surrogate handling>

    Evidence: `BytecodeStringTokenizerTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
    orders, two fresh instances).
