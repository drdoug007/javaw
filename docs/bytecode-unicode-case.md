# Unicode case comparison in bytecode (`unicode-case-comparison-v71`)

The bundled bytecode runtime adds these binary JDK declarations:

- `Character.toUpperCase(char/int)` and `Character.toLowerCase(char/int)`.
- `String.equalsIgnoreCase(String)`, `compareToIgnoreCase(String)`, and `regionMatches(boolean, int, String, int, int)`.
- The `String.CASE_INSENSITIVE_ORDER` comparator, usable with existing object sorting and comparator composition.

These are locale-independent **simple** case mappings, pinned to Java 17 / Unicode 13. They do not implement full String case conversion: for example, the multi-character uppercase expansion of sharp s, contextual final sigma, and Turkish/Lithuanian locale rules belong to the remaining `String.toUpperCase`/`toLowerCase` work. Invalid integer code points and unpaired surrogates remain unchanged by Character conversion.

String comparisons first check UTF-16 units and simple uppercase/lowercase equivalence. Mismatches involving a surrogate pair compare the complete code point, including pairs whose high halves already matched. Region boundaries prevent pairing with units outside the requested region. Region matching preserves negative-length and overflow-safe bounds behavior, null failures and the case-sensitive overload. Equals handles null without throwing; comparator comparison rejects null operands.

The comparison contract guarantees equality and ordering signs. Nonzero comparison magnitudes are not a compatibility guarantee: a JVM's compact Latin-1 versus UTF-16 representation can change those magnitudes. Sorting remains stable through the existing collection runtime. The comparator implements the Serializable marker; serialized forms, private implementation class identity, locale configuration and parallel sorting are not established by this subset.

## Reproducible data

`javaw-bytecode/tools/generate-unicode-case.py --java-home <jdk-17-home> --check` checks the committed tables against the public Character API of a JDK 17 installation. Omit `--check` to regenerate for review. The generator rejects other Java feature versions and prints independent whole-domain reference checksums before compressing the mappings into 676 uppercase and 660 lowercase ranges.

Each range stores a start, end and signed delta. Packed UTF-16 constants avoid thousands of array initializer stores and a large Wasm initializer function. Runtime conversion uses binary search and never delegates to a JavaScript or host Unicode implementation.

## JVM/Wasm evidence

`BytecodeCaseComparisonTest` covers:

- Every integer code point from -1 through U+110000, plus all char values, against JDK 17.0.9 reference checksums. This checks the compressed representation and runtime lookup against Java 17 rather than silently adopting a newer host JVM's Unicode data.
- JVM/Wasm equality, ordering and region matching over ASCII, dotted/dotless I, Kelvin sign, long s, sharp s, Greek sigma forms, Georgian and supplementary Deseret letters. Region tests include cuts through surrogate pairs, isolated surrogates, negative lengths and extreme offsets. Sorting, reversed/composed comparators and typed null failures are included.
- Unchanged, SHA-256-verified Logback Classic 1.4.14 `ch.qos.logback.classic.Level` and SLF4J API 2.0.2 `org.slf4j.event.Level` class files. Tests execute level parsing, default selection, case variants (including dotless I), initialization, severity comparisons, enum and location-aware conversions, and failure paths. Serialization is not executed.
- Selected original Spring Core 6.1.5 `StringUtils.startsWithIgnoreCase` and `endsWithIgnoreCase` methods, including nulls, supplementary case pairs and region boundaries. The other Spring methods and fields are removed; this is selected-method evidence, not whole Spring execution.

Every Wasm comparison runs in both JAR entry orders and two independent instances.

The selected whole-JAR inventory now has 928 declared signatures, 107 missing members and 302 missing owners across the same 1,337 signatures. Seven previously missing signatures now have declarations; these counts do not prove reachability or full execution. The reviewed 63-JAR dependency inventory is unchanged apart from the compiler revision.

Validation: all 321 bytecode-module tests pass with zero failures, errors or skips. The JDK 17 table regeneration check and reviewed 63-JAR audit also pass; dependency inventory counts are unchanged.

## Remaining core gate

String locale/full case conversion, remaining wrapper/numeric APIs, charset/regex/formatting bridges, primitive streams and spliterators, sorted/synchronized collections, broader collectors and closure-wide configuration/reachability proof remain open. Source-backed facades and selected utilities do not establish complete H2/Spring/Hibernate binary compatibility. Keep the core-JDK parent milestone unchecked.

String lowercase expansions and locale-sensitive contexts are documented in [the lowercase contract](bytecode-lowercase.md).
