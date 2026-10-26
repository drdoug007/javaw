# Bytecode String charset conversions

The isolated bytecode runtime uses UTF-8 by default. Named conversions support only the explicitly listed built-in charsets and Java 17 aliases. Conversion replaces malformed/unmappable input like JVM String; streaming codecs, configurable defaults, custom providers and full framework startup remain unsupported. Tests use JVM execution of the same Java fixtures as independent oracles, both JAR orders, dirty allocation and fresh bounded-memory instances. No sustained allocation reclamation is established.

## Named UTF-8 String constructors (`named-utf8-decode-v120`)

Named whole-array and sliced String constructors resolve UTF-8 aliases, preserve replacement behavior and report unsupported names before array bounds failures. Focused tests exercise exact output and typed failures with JVM/Wasm comparisons.

## Named UTF-8 String encoding (`named-utf8-encode-v121`)

String.getBytes(String) supports UTF-8 aliases with isolated-surrogate replacement, fresh output arrays and typed unknown/null-name failures. Focused tests exercise exact output and typed failures with JVM/Wasm comparisons.

## Latin-1 String decoding (`latin1-decode-v122`)

ISO-8859-1 and its Java 17 aliases decode every unsigned byte to the corresponding UTF-16 unit. Focused tests exercise exact output and typed failures with JVM/Wasm comparisons.

## Latin-1 String encoding (`latin1-encode-v123`)

Latin-1 encoding preserves representable units and replaces unmappable units or surrogate pairs with one question-mark byte per unmappable character. Focused tests exercise exact output and typed failures with JVM/Wasm comparisons.

## ASCII String decoding (`ascii-decode-v124`)

US-ASCII aliases decode ASCII bytes exactly and replace each non-ASCII byte with U+FFFD. Focused tests exercise exact output and typed failures with JVM/Wasm comparisons.

## ASCII String encoding (`ascii-encode-v125`)

ASCII encoding replaces unmappable characters and malformed surrogates, consuming valid surrogate pairs as one replacement. Focused tests exercise exact output and typed failures with JVM/Wasm comparisons.

Named lookups validate the ASCII charset-name grammar before case-insensitive alias matching, so Unicode case-fold lookalikes cannot select an ASCII-named encoding. Invalid and unsupported names both produce UnsupportedEncodingException for String name overloads.

## Big-endian UTF-16 String conversion (`utf16be-codec-v126`)

UTF-16BE preserves byte order and literal BOM units, replaces malformed input with the JVM consumption rules and encodes malformed surrogates as U+FFFD. Focused tests exercise exact output and typed failures with JVM/Wasm comparisons.
