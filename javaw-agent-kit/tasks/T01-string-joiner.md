# T01 StringJoiner

```kit
id: T01
slug: string-joiner
title: link java.util.StringJoiner
tests: BytecodeStringJoinerTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/StringJoiner.java
allow: docs/bytecode-string-joiner.md
```

## Goal

Add `java.util.StringJoiner` to the bundled exception runtime. Twelve pinned artifacts call it (H2 uses
`new StringJoiner(...)`, `add` and `toString`). Today any bytecode that mentions `StringJoiner` is rejected
because the class does not exist in the runtime.

## Members to implement

All public members of the JDK class, nothing else public:

    public final class StringJoiner {
        public StringJoiner(CharSequence delimiter)
        public StringJoiner(CharSequence delimiter, CharSequence prefix, CharSequence suffix)
        public StringJoiner setEmptyValue(CharSequence emptyValue)
        public String toString()
        public StringJoiner add(CharSequence newElement)
        public StringJoiner merge(StringJoiner other)
        public int length()
    }

## Members to leave out

None. Do not add other public methods (no `isEmpty`, no `equals`/`hashCode` overrides).

## JDK behaviour to match

- The 1-argument constructor behaves exactly like `StringJoiner(delimiter, "", "")`.
- Null checks in the 3-argument constructor run in this order: prefix, delimiter, suffix. Each throws
  `NullPointerException` with exactly this message:
  `The prefix must not be null`, `The delimiter must not be null`, `The suffix must not be null`.
  So `new StringJoiner(null, null, null)` reports the prefix, and `new StringJoiner(null)` reports the delimiter.
- `setEmptyValue(null)` throws `NullPointerException("The empty value must not be null")` and keeps the old
  empty value. `merge(null)` throws a `NullPointerException` whose message is `null`
  (use `Objects.requireNonNull(other)`).
- Arguments are copied with `toString()` when they are given. Later changes to a `StringBuilder` passed as the
  delimiter, prefix, suffix, element or empty value do not change the joiner.
- `add(null)` appends the text `"null"` (`String.valueOf(newElement)`). `add`, `setEmptyValue` and `merge`
  return `this`.
- `toString()`: if no element was ever added and an empty value was set, return the empty value (without
  prefix/suffix). Otherwise return `prefix + elements joined with delimiter + suffix`; with no elements that is
  `prefix + suffix`. Adding the empty string `""` counts as adding an element.
- `length()` is always `toString().length()` (empty value length while nothing was added).
- `merge(other)`: if `other` has no elements, do nothing (its empty value is ignored). Otherwise add ONE
  element to `this`: other's elements joined with **other's** delimiter, without other's prefix and suffix.
  `other` is unchanged and still usable. `a.merge(a)` works: `[a,b]` merged with itself gives `[a,b,a,b]`.

## Steps

1. Read `java/util/OptionalInt.java` for the style of a small runtime class.
2. Create `javaw-bytecode/src/main/resources/exception-runtime/java/util/StringJoiner.java`,
   `package java.util;`, `public final class StringJoiner`.
3. Fields: `String prefix, delimiter, suffix`, `String emptyValue` (null = not set), and a `StringBuilder`
   holding the joined elements without prefix/suffix, plus an `int` element count (or a boolean "has elements").
4. Implement the members above. Use `Objects.requireNonNull(value, "message")` for the null checks.
5. Register it: in `javaw-bytecode/src/main/java/one/dastec/javaw/bytecode/BytecodeExceptionRuntime.java`, in the
   `List.of(...)` line that contains `"LinkedList"`, add `"StringJoiner"` right after `"LinkedList"`.
6. Run `javaw-agent-kit/bin/kit check T01` until it prints PASS.
7. Write `docs/bytecode-string-joiner.md` (skeleton below) and your report, then `kit finish T01`.

## Hints

- Before you start, `kit check T01` fails with `FIX THE RUNTIME: the bytecode compiler rejected the fixture`
  naming `java/util/StringJoiner`. That is expected; it means the class is missing.
- `merge(this)`: read the other joiner's joined text into a local `String` **before** you append anything to
  `this`, otherwise you read a buffer you are changing.
- Do not compute `merge` by cutting `other.toString()`: that is wrong when other has an empty value or when
  prefix/suffix lengths differ. Keep the joined elements separately from prefix/suffix.
- `length()` must agree with `toString()` in both states (empty value set and nothing added, or not).
  Decide "nothing was added" with an element count, not with the length of the joined text: after `add("")`
  the joined text is empty but the joiner is no longer empty (`[]`, not the empty value).
- Calibration: a ~50-line class (final String fields, a StringBuilder, an int count) passed on the first
  `kit check`, and the full suite stayed green. No other runtime file needs to change.
- Only `String`, `StringBuilder`, `Objects` and `CharSequence` are needed. `String.valueOf(Object)` exists in the
  runtime and turns `null` into `"null"`.
- The JDK throws `OutOfMemoryError` when the result would exceed `Integer.MAX_VALUE` chars; that is not tested.
  Do not add it.

## Docs

Create `docs/bytecode-string-joiner.md` with this skeleton (fill in the brackets):

    # Bytecode StringJoiner

    Revision `string-joiner-vN` links java.util.StringJoiner in the bundled exception runtime.

    Contract:

    - <behaviour: constructors and null messages, add/toString, empty value, length, merge>

    Evidence: `BytecodeStringJoinerTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders,
    two fresh instances).
