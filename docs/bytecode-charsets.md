# Bytecode String charset conversions

The isolated bytecode runtime uses UTF-8 by default. Named conversions support only the explicitly listed built-in charsets and Java 17 aliases. Conversion replaces malformed/unmappable input like JVM String; streaming codecs, configurable defaults, custom providers and full framework startup remain unsupported. Tests use JVM execution of the same Java fixtures as independent oracles, both JAR orders, dirty allocation and fresh bounded-memory instances. No sustained allocation reclamation is established.

## Named UTF-8 String constructors (`named-utf8-decode-v120`)

Named whole-array and sliced String constructors resolve UTF-8 aliases, preserve replacement behavior and report unsupported names before array bounds failures. Focused tests exercise exact output and typed failures with JVM/Wasm comparisons.
