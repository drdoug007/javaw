# Bytecode text utilities (`core-text-utilities-v70`)

The bundled bytecode runtime supplies these additional binary JDK descriptors:

- `Character.isWhitespace(char/int)`, with Java whitespace ranges (including control separators, excluding nonbreaking spaces U+00A0/U+2007/U+202F). Invalid code points return false.
- `String.isBlank`, `strip`, `stripLeading` and `stripTrailing`. These differ from `trim`: they use Java Unicode whitespace rather than characters at or below U+0020. Surrogates are preserved.
- `String(int[], int, int)`: defensive UTF-16 construction from code points, including supplementary pairs and isolated surrogate values; invalid code points, bounds and null inputs raise the JVM exception types.
- Case-sensitive `String.regionMatches(int, String, int, int)`, including negative lengths and overflow-safe bounds comparisons.
- `String.repeat(int)` and both `String.join` overloads (varargs and Iterable). Joining snapshots delimiter text and converts elements using `toString`, including null elements. Repetition rejects negative counts and impossible int-sized results before allocation.
- Sequential lazy `String.lines()`, backed by the existing reference-stream runtime. LF, CR and CRLF terminate lines; a trailing terminator creates no extra line. Unicode line/paragraph separators are ordinary content. Pipelines retain single-use, short-circuit and close behavior from the stream runtime. Each traversal scans only the current line; advancing may scan it again. No JDK complexity or parallel/spliterator contract is claimed.

## Executable evidence

`BytecodeTextUtilitiesTest` compiles javac callers and compares JVM results with Wasm in both JAR entry orders and two independent instances. It covers every Unicode code point for whitespace classification, UTF-16 boundaries, typed failures, repetition limits, joining callbacks, region bounds and line traversal/laziness.

The tests verify artifact SHA-256 hashes before loading pinned bytecode:

- Micrometer Commons 1.12.4 `io.micrometer.common.util.StringUtils` executes unchanged, including blank/empty predicates, both truncation overloads and failures.
- Spring Core 6.1.5 `org.springframework.util.StringUtils` executes seven selected original methods: the two `hasLength`, three whitespace trim methods and two `trimAllWhitespace` overloads. Other methods and fields are removed; this is selected-method evidence, not whole Spring utility class or application startup.

The whole-JAR signature inventory moves from 909 to 921 declared signatures, with 114 missing members and 302 missing owners across 1,337 selected signatures. Inventory counts are declarations, not proven reachability or semantic coverage. The 63-JAR dependency inventory is unchanged apart from the compiler revision.

Validation: the required full bytecode run completed 316 cases with 315 passing and one new fixture-generation encoding error. After correcting that fixture, all four text utility tests pass on rerun, including the final CharSequence conversion and stream lifecycle assertions. There are no remaining failing cases in the combined test reports. The reviewed 63-JAR audit passes with unchanged dependency counts and the revised compiler identity.

## Remaining milestone

Case-insensitive/case-mapping APIs, charset/regex/formatting bridges, primitive streams and spliterators, sorted/synchronized collections, broader collectors, and closure-wide execution/classification remain open. The pinned inventory contains no missing BigDecimal descriptor and no context arithmetic calls; this does not prove that arbitrary BigDecimal use is supported. Source facades remain separate from this bytecode contract.
