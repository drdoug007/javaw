# Selected bytecode text operations

These bundled bytecode implementations target Java 17 semantics. They do not establish complete JDK or framework support.

## Locale identities (locale-identities-v130)

Language/country/variant constructors normalize ASCII case and legacy language aliases; constants, equality, hash, clone and underscore text match Java 17. The isolated default is ROOT. Locale providers, tags, extensions, display names, mutable defaults and categories remain unsupported. BytecodeTextFormatTest JVM/Wasm comparisons pass in both JAR orders and fresh instances.

## Unicode full uppercase (unicode-uppercase-v131)

String.toUpperCase(Locale) supports general Unicode 13 uppercase, 102 full expansions, supplementary mappings and Java 17 unchanged-result identity behavior. Exhaustive JVM/Wasm singleton comparisons cover every code point in both JAR orders and fresh instances. Reproducible Java 17 data generation includes combining classes for subsequent contextual rules; tr/az/lt remain explicitly unsupported at this stage.
