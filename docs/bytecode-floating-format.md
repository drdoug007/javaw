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
