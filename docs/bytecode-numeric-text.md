# Bytecode numeric and text APIs

These APIs execute as bundled bytecode inside Wasm. They do not use source facades or host arithmetic/charset callbacks.

## context decimal division (`context-division-v85`)

BigDecimal.divide(BigDecimal, MathContext) rounds the rational quotient directly, preserves preferred scales for exact results, retains precision for inexact results and checks scale overflow. Precision zero uses exact division; zero divisors, nonterminating unlimited division and UNNECESSARY rounding failures raise ArithmeticException.

The focused JVM/Wasm comparisons pass in both JAR orders and independent instances. The reviewed 63-JAR audit baseline changes only the compiler revision, with unchanged dependency inventory.

## context decimal powers (`context-powers-v86`)

BigDecimal.pow(int, MathContext) uses the Java working precision (requested precision plus exponent digits plus one), the specified repeated-squaring order, reciprocal division for negative powers and final rounding. Exponent range/digit limits and unlimited negative powers raise ArithmeticException.

The focused JVM/Wasm comparisons pass in both JAR orders and independent instances. The reviewed 63-JAR audit baseline changes only the compiler revision, with unchanged dependency inventory.

## unsigned byte conversions (`unsigned-bytes-v87`)

Byte.toUnsignedInt and toUnsignedLong mask the signed input to 0–255. JVM/Wasm comparisons cover every byte and execute unchanged pinned Hibernate RowVersionComparator and ComparableComparator classes, including bridge dispatch, ordering and null failures.

The focused JVM/Wasm comparisons pass in both JAR orders and independent instances. The reviewed 63-JAR audit baseline changes only the compiler revision, with unchanged dependency inventory.

## UTF-8 String decoding (`utf8-decoding-v88`)

String(byte[]) and String(byte[], int, int) decode UTF-8 with Java replacement grouping for malformed/truncated input, snapshot the input and validate null/slice arguments. The isolated runtime default charset is explicitly UTF-8, independent of host locale.

The focused JVM/Wasm comparisons pass in both JAR orders and independent instances. The reviewed 63-JAR audit baseline changes only the compiler revision, with unchanged dependency inventory.

Decoder evidence includes every one- and two-byte input, selected three-/four-byte boundaries and truncated prefixes, invalid slices, null precedence and defensive snapshots. The JVM oracle requires its default charset to be UTF-8. Each comparison batch starts a fresh Wasm instance using two initial / twenty maximum memory pages; these tests do not establish managed reclamation.

The SnakeYAML 2.2 artifact SHA-256 is verified. The fixture retains original Base64Coder decodeString/decode and byte-array encode bodies and table initialization. It removes line-oriented methods, encodeString (until encoding support), the systemLineSeparator field and the three original line.separator/System.getProperty/putstatic instructions. The retained JVM and Wasm class is identical. This proves selected Base64/UTF-8 decoding, not whole-class initialization or SnakeYAML parsing. The two decimal context additions above have no direct call in the pinned linkage inventory and establish API conformance only.

The later v120–v129 subsets add six standard charset identities and named/Charset overloads; see [the charset contract](bytecode-charsets.md). Configurable defaults, streaming encoders/decoders and broader I/O remain open; unsupported descriptors receive admission diagnostics.

## UTF-8 String encoding (`utf8-encoding-v89`)

String.getBytes() returns a fresh UTF-8 byte array. Supplementary pairs encode as four bytes; each unpaired surrogate becomes the Java UTF-8 replacement byte 0x3f. The isolated default charset is UTF-8.

The focused JVM/Wasm comparisons pass in both JAR orders and independent instances. The reviewed 63-JAR audit baseline changes only the compiler revision, with unchanged dependency inventory.

Encoding evidence covers all 65,536 UTF-16 single units, 3,072 supplementary pairs spanning every high surrogate, and 343 three-unit combinations with valid and malformed surrogate boundaries. Comparisons include exact encoded byte hashes and lengths, round trips for valid pairs, independent output snapshots and empty-array identity. The selected pinned SnakeYAML encodeString body now executes alongside decodeString; line-oriented methods and the property initialization remain excluded as described above.

Remaining limits: default charset configuration is unsupported. Explicit Charset/name overloads for six standard charsets are covered by the later [charset contract](bytecode-charsets.md). The UTF-8 default is a deliberate isolated-runtime contract, not inheritance of a browser or host locale. These APIs do not prove whole-library startup, managed allocation stability or completion of the broad numeric/text milestone.

Combined validation at `utf8-encoding-v89`: the required offline bytecode suite passes all 362 tests with zero failures, errors or skips (four test forks). This includes all 21 decimal tests and eight unsigned-byte/UTF-8 tests. The final reviewed 63-JAR audit passes with unchanged dependency inventory. The whole-JAR linkage inventory now reports 978 declared signatures, 57 missing members and 302 missing owners; these are declaration counts, not reachable-closure execution proof.

## Unsigned Short conversions (`unsigned-shorts-v90`)

Short.toUnsignedInt/toUnsignedLong preserve every 16-bit pattern through masking, including method references. JVM/Wasm comparisons exercise both JAR orders and independent instances. These are API conformance tests, not additional pinned-library or reachable-closure execution claims. Dependency inventory counts remain unchanged.

## String copy factories (`string-copy-factories-v91`)

Both copyValueOf overloads create defensive UTF-16 snapshots and preserve null/bounds exception types and precedence. JVM/Wasm comparisons exercise both JAR orders and independent instances. These are API conformance tests, not additional pinned-library or reachable-closure execution claims. Dependency inventory counts remain unchanged.

## Floating exponent extraction (`floating-exponents-v92`)

Math.getExponent handles normal/subnormal values, both zeros, infinities and NaNs for float and double; tests cover every exponent transition and deterministic raw-bit patterns. JVM/Wasm comparisons exercise both JAR orders and independent instances. These are API conformance tests, not additional pinned-library or reachable-closure execution claims. Dependency inventory counts remain unchanged.

## Floating units in the last place (`floating-ulps-v93`)

Math.ulp computes exact spacing for float/double, including subnormal spacing, signed zero, maximum finite values, infinities and NaNs. Every exponent transition and deterministic raw-bit patterns are compared. JVM/Wasm comparisons exercise both JAR orders and independent instances. These are API conformance tests, not additional pinned-library or reachable-closure execution claims. Dependency inventory counts remain unchanged.

## Next larger floating values (`floating-next-up-v94`)

Math.nextUp steps one representable value toward positive infinity for float/double, preserving NaNs and positive infinity and crossing signed-zero/subnormal boundaries correctly. JVM/Wasm comparisons exercise both JAR orders and independent instances. These are API conformance tests, not additional pinned-library or reachable-closure execution claims. Dependency inventory counts remain unchanged.

## Next smaller floating values (`floating-next-down-v95`)

Math.nextDown steps one representable value toward negative infinity for float/double, preserving NaNs and negative infinity and crossing signed-zero/subnormal boundaries correctly. JVM/Wasm comparisons exercise both JAR orders and independent instances. These are API conformance tests, not additional pinned-library or reachable-closure execution claims. Dependency inventory counts remain unchanged.

## Directed adjacent floating values (`floating-next-after-v96`)

Both Math.nextAfter overloads retain the double direction, preserve the destination zero sign on equality and handle NaNs, infinities and underflow/overflow boundaries. JVM/Wasm comparisons exercise both JAR orders and independent instances. These are API conformance tests, not additional pinned-library or reachable-closure execution claims. Dependency inventory counts remain unchanged.

## Power-of-two floating scaling (`floating-scaling-v97`)

Math.scalb preserves a single final rounding, signed zeros, subnormal ties and saturation for all int scale factors. Tests cross every exponent transition and random values with 48 scale boundaries including both int extremes. JVM/Wasm comparisons exercise both JAR orders and independent instances. These are API conformance tests, not additional pinned-library or reachable-closure execution claims. Dependency inventory counts remain unchanged.

## String escape translation (`string-escape-translation-v98`)

String.translateEscapes implements standard and octal escapes, space escapes and CR/LF/CRLF continuations; malformed/trailing escapes raise IllegalArgumentException. Unicode escape processing belongs to the Java lexer and is deliberately rejected here. JVM/Wasm comparisons exercise both JAR orders and independent instances. These are API conformance tests, not additional pinned-library or reachable-closure execution claims. Dependency inventory counts remain unchanged.

## String indentation (`string-indentation-v99`)

String.indent adds spaces or removes Java whitespace per line, normalizes CR/LF/CRLF and terminates nonempty output lines with LF. Integer.MIN_VALUE removal avoids negation overflow; empty input remains empty even for Integer.MAX_VALUE padding. JVM/Wasm comparisons exercise both JAR orders and independent instances. These are API conformance tests, not additional pinned-library or reachable-closure execution claims. Dependency inventory counts remain unchanged.

Floating comparisons canonicalize NaNs and distinguish signed zero. NaN payload propagation and JDK algorithmic complexity are not promised. The ten v90–v99 subsets leave locale conversion, regex, formatting, broader charset APIs, additional numeric APIs and actual framework execution open.

Combined v90–v99 validation: the required offline bytecode suite passes all 372 tests with zero failures, errors or skips. The final 63-JAR audit passes with unchanged dependency inventory; only the compiler revision changes in the reviewed baseline.
