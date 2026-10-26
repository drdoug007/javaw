# Selected bytecode text operations

These bundled bytecode implementations target Java 17 semantics. They do not establish complete JDK or framework support.

Current selected String operations are locale/default uppercase and formatting conversions s/S, b/B, h/H, c/C, d, o, x/X, percent and LF newline. Lowercase, regex, floating-point/date formatting, BigInteger formatting, Formattable and public Formatter/provider APIs remain open. Valid excluded formatting conversions raise UnsupportedOperationException; unavailable public APIs fail linking. Exception types and accessor payloads are tested; exact message text is not promised. The isolated default locale is ROOT and cannot be changed. Numeric decimal localization accepts only ROOT, ENGLISH, US and UK; text casing supports other language/country/variant identities. Extensions and locale services are excluded.

BytecodeTextFormatTest uses an external **JDK 17** oracle so Unicode data matches the runtime tables. Set `-Djavaw.jdk17.home=/path/to/jdk17` if it is not installed at the local default path. Oracle execution sets empty user language/country/variant properties for the isolated ROOT default. Wasm comparisons run both JAR orders and fresh dirty-memory instances. The exhaustive uppercase test covers all 1,114,112 code-point values including surrogate values; contextual tests cover selected sequences and BMP samples, not all possible strings.

Pinned formatting evidence uses jackson-annotations 2.15.4, SHA-256 `f204ebbd552614a22b8531ffe350d47f8fd42c45bb60517c07974dc27a5a1dd3`. Wasm retains original `_id`/`_useInput` fields and unchanged constructor/toString method bodies in a Java 17 carrier, dropping the original interfaces, annotations and other methods. The JVM oracle loads the complete original classes and calls the same constructor through a test subclass. Cases include null/empty/text/integer identifiers and null/false/true use-input flags. This proves this selected String.format call path; it excludes annotation normalization and the remainder of Jackson.

## Locale identities (locale-identities-v130)

Language/country/variant constructors normalize ASCII case and legacy language aliases; constants, equality, hash, clone and underscore text match Java 17. The isolated default is ROOT. Locale providers, tags, extensions, display names, mutable defaults and categories remain unsupported. BytecodeTextFormatTest JVM/Wasm comparisons pass in both JAR orders and fresh instances.

## Unicode full uppercase (unicode-uppercase-v131)

String.toUpperCase(Locale) supports general Unicode 13 uppercase, 102 full expansions, supplementary mappings and Java 17 unchanged-result identity behavior. Exhaustive JVM/Wasm singleton comparisons cover every code point in both JAR orders and fresh instances. Reproducible Java 17 data generation includes combining classes for subsequent contextual rules; tr/az/lt remain explicitly unsupported at this stage.

## Turkic uppercase (turkic-uppercase-v132)

String.toUpperCase(Locale) maps i to dotted capital I for Turkish and Azeri, with general full uppercase elsewhere. JVM/Wasm tests pass across BMP samples, locale normalization, mixed dotted/dotless text and null-locale failures. Tests use bounded batches because the host allocator does not reclaim memory.

## Lithuanian uppercase (lithuanian-uppercase-v133)

Lithuanian uppercase removes combining dot above after soft-dotted letters with Java 17 combining-class barriers and preserves the initial unchanged-prefix behavior. JVM/Wasm comparisons pass for all ten Java 17 soft-dotted bases, intervening BMP samples, repeated dots, surrogate text and nonmatching bases; full-table data remains reproducible.

## Default String uppercase (default-uppercase-v134)

String.toUpperCase() uses the isolated ROOT locale. Direct calls and method references match a Java 17 ROOT-default oracle for expansions, supplementary and malformed-surrogate text, unchanged identity and mixed combining marks. Configurable defaults and lowercase remain unsupported.

## String text formatting (text-formatting-v135)

String.format(String,Object...) supports s/S, percent and LF newline, explicit/ordinary indexes and reuse, UTF-16 precision and width, null arrays, and typed syntax/flag/argument failures. Parsing validates the complete format before rendering. JVM/Wasm comparisons pass with exception getter payloads. Boolean, hash, character and numeric conversions are staged follow-ups; floating/date conversions, Formattable and public Formatter/provider APIs remain unsupported, with explicit diagnostics.

## Boolean hash and character formatting (general-formatting-v136)

String.format supports b/B, h/H and c/C, including nulls, UTF-16 precision for general conversions, supplementary code points and boxed byte/short/int/char validation. JVM/Wasm comparisons pass for formatting and typed conversion/code-point/precision/flag failures with getter payloads. Hash formatting uses the object hash contract; stable hash values are required for cross-host output comparisons.

## Decimal integral formatting (decimal-formatting-v137)

String.format d supports boxed byte/short/int/long, exact minimum/maximum values, signs, grouping, parentheses, alignment and zeros after signs. JVM/Wasm comparisons pass for values, nulls and typed flag/precision/type failures. Numeric localization is restricted to ROOT and selected English identities; BigInteger formatting is explicitly unsupported.

## Octal and hexadecimal formatting (radix-formatting-v138)

String.format o/x/X supports unsigned boxed byte/short/int/long bit patterns, alternate prefixes, alignment and zeros after prefixes, including null handling. JVM/Wasm comparisons pass for negative bit patterns, missing widths, minimum/maximum values, zero and typed failures; canonical uppercase error flags and lowercase conversion getter payloads match Java 17. BigInteger formatting remains unsupported.

## Locale and instance formatting with pinned execution (locale-formatting-v139)

String.format(Locale,...) and String.formatted(...) share the selected formatting engine. Locale text uppercase uses the Java 17 context rules; decimal localization supports ROOT/English/US/UK, with null Locale selecting ROOT. Focused tests pass for overloads, excluded-form diagnostics, malformed descriptors and canonical exception precedence/payloads. The unchanged Jackson Annotations 2.15.4 JacksonInject.Value constructor and toString bytecode execute in a carrier and match an original-library JVM oracle; old annotation-class normalization prevents whole-class Wasm execution, so no whole-library/framework claim is made.

## Combined validation

Combined v130–v139 validation: all 421 tests pass in the required offline bytecode suite (four forks), with zero failures, errors or skips. All 14 text/admission tests pass, including exhaustive Java 17 singleton uppercase, contextual casing, formatting and typed failures, excluded-form diagnostics, and pinned Jackson constructor/formatting-method carrier execution against the original-library JVM oracle. Unicode table regeneration and the reviewed 63-JAR audit check pass with unchanged inventory totals (26,625 classes and 734 resources). Selected core-JDK coverage is 1,003 declared signatures, 32 missing members and 302 missing owners. Lowercase/regex, excluded formatting, configurable locales, allocation stability and complete framework execution remain open.
