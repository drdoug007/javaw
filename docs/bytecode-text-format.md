# Selected bytecode text operations

These bundled bytecode implementations target Java 17 semantics. They do not establish complete JDK or framework support.

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
