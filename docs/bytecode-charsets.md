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

## Little-endian UTF-16 String conversion (`utf16le-codec-v127`)

UTF-16LE applies little-endian conversion, literal BOM handling and replacement semantics for truncated bytes and malformed surrogate sequences. Focused tests exercise exact output and typed failures with JVM/Wasm comparisons.

## BOM-aware UTF-16 String conversion (`utf16-bom-codec-v128`)

UTF-16 detects a leading byte-order mark, defaults to big endian, emits a big-endian BOM for nonempty encoding and preserves subsequent BOM units. Focused tests exercise exact output and typed failures with JVM/Wasm comparisons.

## Standard charset identities and String overloads (`standard-charsets-v129`)

Charset/StandardCharsets expose the six standard singleton identities, canonical names, aliases, containment and explicit lookup failures; String Charset overloads execute the same replacement-mode codecs. Streaming encoders/decoders and custom providers remain unsupported. Focused tests exercise exact output and typed failures with JVM/Wasm comparisons.

## Standard contract and executable library evidence

The six canonical encodings are UTF-8, ISO-8859-1, US-ASCII, UTF-16BE, UTF-16LE and UTF-16. Their Java 17 alias sets are shared by named lookup and immutable Charset metadata. Charset.forName rejects malformed ASCII names with IllegalCharsetNameException and valid unavailable names with UnsupportedCharsetException; isSupported returns false only for valid unavailable names. Null lookup names raise IllegalArgumentException, while null String charset arguments raise NullPointerException. String name overloads raise UnsupportedEncodingException for malformed or unavailable names before validating array bounds. Exception types and stored charset names are tested; general JVM exception-message wording is not promised. The runtime default remains UTF-8.

Aliases and identities are tested through both Charset.forName and StandardCharsets fields. Built-in contains/compare/equality/hash behavior is compared with the JVM, including the Unicode-versus-single-byte contains(null) behavior. String conversion accepts the six built-in singleton objects; foreign implementations receive an explicit UnsupportedOperationException. Charset encoder/decoder factories, buffers, coding error actions, provider discovery, additional platform encodings and locale overloads are not bundled and their missing members/owners remain diagnostic. This is a selected String conversion contract, not a complete java.nio.charset implementation.

UTF-16 replaces a malformed high-surrogate sequence together with the next two-byte unit, or the remaining truncated suffix, as the JVM decoder does. Explicit-endian codecs preserve BOM units; UTF-16 consumes only an initial BOM and defaults to big endian without one. UTF-16 encodes nonempty strings with a big-endian BOM and emits no BOM for empty input. ASCII/Latin-1 use one question-mark byte per unmappable character; a valid surrogate pair consumes one replacement byte. UTF encoders replace isolated surrogates using their charset-specific replacement bytes.

The Jandex 3.1.2 artifact SHA-256 is checked against the pinned lock. The complete original org.jboss.jandex.Utils class, including its initializer, is copied unchanged from that JAR and linked beside a same-package fixture. JVM and Wasm execute its toUTF8/fromUTF8 methods, static INIT_METHOD_NAME initialization, malformed-byte replacement and null failures. No methods, instructions or class metadata are rewritten. Index scanning, Jandex startup and full Hibernate/Spring/H2 execution remain outside this evidence.

## Combined validation

Combined v120–v129 validation: all 407 tests pass in the required offline bytecode suite (four test forks), with zero failures, errors or skips. All 13 charset/admission tests pass, including exhaustive unit/byte cases, malformed replacement grouping, aliases, typed failures, immutable metadata, snapshots, functional conversion callbacks and unchanged pinned Jandex Utils execution. The reviewed 63-JAR audit passes with unchanged dependency inventory totals; selected core-JDK coverage is 998 declared signatures, 37 missing members and 302 missing owners (six fewer missing String members). Broader text/numeric signatures, streaming codecs/providers, managed allocation stability and full framework execution remain open.
