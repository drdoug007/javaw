# Unicode case comparison in bytecode (`unicode-case-comparison-v71`)

The bundled bytecode runtime adds these binary JDK declarations:

- `Character.toUpperCase(char/int)` and `Character.toLowerCase(char/int)`.
- `String.equalsIgnoreCase(String)`, `compareToIgnoreCase(String)`, and `regionMatches(boolean, int, String, int, int)`.
- The `String.CASE_INSENSITIVE_ORDER` comparator, usable with existing object sorting and comparator composition.

These are locale-independent **simple** case mappings, pinned to Java 17 / Unicode 13. They do not implement full String case conversion: for example, the multi-character uppercase expansion of sharp s, contextual final sigma, and Turkish/Lithuanian locale rules belong to the remaining `String.toUpperCase`/`toLowerCase` work. Invalid integer code points and unpaired surrogates remain unchanged by Character conversion.

String comparisons first check UTF-16 units and simple uppercase/lowercase equivalence. Mismatches involving a surrogate pair compare the complete code point, including pairs whose high halves already matched. Region boundaries prevent pairing with units outside the requested region. Region matching preserves negative-length and overflow-safe bounds behavior, null failures and the case-sensitive overload. Equals handles null without throwing; comparator comparison rejects null operands.

The comparison contract guarantees equality and ordering signs. Nonzero comparison magnitudes are not a compatibility guarantee: a JVM's compact Latin-1 versus UTF-16 representation can change those magnitudes. Sorting remains stable through the existing collection runtime. The comparator implements the Serializable marker; serialized forms, private implementation class identity, locale configuration and parallel sorting are not established by this subset.

## Reproducible data

The tables are not committed. They are built by the `javaw-unicode-data` module, which the reactor builds before `javaw-bytecode`. No Python is involved.

- **Generation.** In the `generate-resources` phase, Maven's own JDK runs `javaw-unicode-data/tools/GenerateJdk17Data.java` in single-file source mode, so there is no compile step. It finds a JDK 17 and runs `tools/Jdk17Tables.java <table> --output <file>` on it, once per table and in parallel, with the JVM options each table needs. `Jdk17Tables` derives each table from that JDK's own APIs.
- **Outputs.** `UnicodeCase.java`, plus the properties, uppercase and lowercase tables, go to `javaw-unicode-data/target/generated-resources/jdk17/exception-runtime/javaw/runtime/`. The test oracle goes to `javaw-unicode-data/character-traversal-jdk17.txt` in the same resource directory, outside the runtime's `exception-runtime/` tree.
- **Packaging.** The module's one jar carries the tables, with the `legal/word-break` notices, and the oracle. `javaw-bytecode` depends on it. The oracle is in the main jar rather than a test jar, so `-Dmaven.test.skip=true` cannot drop it. `BytecodeExceptionRuntime` loads the tables as `exception-runtime/` classpath resources, and `BytecodeUnicodeDataTest` checks that they and the oracle resolve from the data module.
- **JDK 17.** It comes from `-Djavaw.jdk17.home`, `JAVA_HOME_17` (also `_X64`/`_ARM64`) or, on macOS, `java_home -v 17`. The build fails if none is a JDK 17.
- **Regeneration.** Outputs are regenerated only when a tool source or the JDK changes, which a stamp file outside the resource root records.
- **Checks.** Each table rejects other Java feature versions. The case table prints independent whole-domain reference checksums before compressing the mappings into 676 uppercase and 660 lowercase ranges.

The Java generators replaced the earlier Python scripts. Their output is byte-identical except for one comment line per table, which now names `Jdk17Tables.java`. Line numbers are unchanged, so the compiled runtime classes and every compiler output are the same.

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
