# Bytecode decimal floating formatting

Selected `String.format`/`String.formatted` behavior for linked bytecode,
separate from source facades. Java 17 CLDR oracle comparisons run both JAR
orders with independent dirty-memory instances per bounded input batch.
Numeric rendering executes in Wasm without a host formatter.

This does not establish full Formatter/JDK compatibility, bounded sustained
allocation, or framework startup. Date conversions, public Formatter,
Formattable, configurable locale defaults/providers and unselected numeric
locales remain unsupported. The isolated default locale is ROOT.

Completed subsets:

- v160: Float/Double fixed-point f formatting with Java 17 decimal digit generation and HALF_UP precision.
- v161: Float/Double scientific e/E formatting with rounded mantissas, exponent padding and alternate decimal points.
- v162: Float/Double general g/G formatting with significant-digit precision and notation selection after rounding.
- v163: NaN and signed infinities for f/e/E/g/G, with ASCII uppercase, sign/parenthesis flags and space padding independent of precision and zero flags.
- v164: Signed-zero and zero-exponent semantics, retaining negative signs and parentheses when finite values round to zero.
- v165: Italian/Italy numeric formatting with comma decimal and period grouping symbols for integral, arbitrary-precision and binary floating values.
- v166: Japanese/Japan numeric formatting with ASCII decimal digits, period decimal and comma grouping, preserving numeric flags and precision.
- v167: Chinese/China/Taiwan numeric formatting with Java 17 CLDR decimal and grouping symbols; PRC and simplified/traditional aliases share these identities.
- v168: Korean/Korea numeric formatting with selected Java 17 CLDR digits and symbols across integral, arbitrary-precision and binary floating conversions.
- v169: Pinned Jackson Databind helper execution with binary decimal conversions, overload/index/reuse and typed failures; every finite double exponent field and malformed descriptors are checked against Java 17.

Binary Float values are promoted exactly to double, as Java 17 Formatter does.
Finite rendering generates decimal digits using strict symmetric rounding
intervals (including the narrower power-of-two margin) and Java 17's integral
fast-path decimal rounding, then applies requested HALF_UP precision. This path
is separate from ordinary String.valueOf/Double.toString shortest rendering.
Binary zero always has exponent zero; its raw sign survives formatting and
finite negative values keep their sign when decimal rounding produces zero.
NaN ignores sign flags, infinities accept sign/parenthesis flags, and neither
uses zero padding or decimal precision. Uppercase numeric nonfinite text is
ASCII even in Turkic locales; general %S still uses locale-sensitive text casing.

Numeric locale support adds it/it_IT, ja/ja_JP, zh/zh_CN/zh_TW and ko/ko_KR,
with Java 17 default CLDR symbols and ASCII digits. Italian decimal/grouping
symbols are comma/period; the selected East Asian identities use period/comma.
Other country identities, variants, extensions, mutable defaults and provider
selection remain unsupported. Nonfinite and null values do not need numeric
symbols, matching the existing selected formatting contract.

Pinned evidence uses jackson-databind 2.15.4, SHA-256
`0f99ff7719eef3ee85608b3dee6ecf3c698003fe9d6e34eabbfabccdc9b5ac2c`.
The test retains unchanged DatabindContext constructor and `_format` instruction
bodies in a Java 17 carrier, omitting its other metadata, methods and dependencies.
Both JVM and Wasm run this carrier with Float/Double values, nulls, indexes/reuse,
no-argument bypass, and typed failures. This proves selected helper execution;
it does not establish whole-class or ObjectMapper/framework execution.

`everyFiniteExponent` tests all 2,046 finite normal double exponent fields with
selected zero/one/max fraction patterns, both signs and varying f/e/g precisions.
Other fixtures cover subnormals, promoted floats, deterministic raw patterns,
rounding carries/ties, special values and selected locales. This is extensive
sampled conformance evidence, not exhaustive binary64 or all-string coverage.
Malformed String.format return descriptors are rejected before Wasm generation.
All allocations use the existing host allocator; large precision/width and
repeated workloads remain subject to its capacity and collection limitations.

Validation: all 457 tests pass in the required offline bytecode suite (four forks), with zero failures, errors or skips. All 12 new formatting regressions pass, including every finite double exponent field and the pinned carrier. The reviewed 63-JAR audit passes with unchanged inventory (26,625 classes, 734 resources) and selected signature counts (1,003 declared, 32 missing members, 302 missing owners); only the compiler revision changes in the baseline.

Reproduce with:

```sh
mvn -o -pl javaw-bytecode -am -Dtest='one.dastec.javaw.bytecode.*Test' -Dsurefire.failIfNoSpecifiedTests=false -DforkCount=4 test
python3 runtime-target/audit.py --check-baseline
```
