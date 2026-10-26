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
