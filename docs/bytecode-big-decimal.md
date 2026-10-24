# Bytecode BigDecimal and rounding foundation

Revision `big-decimal-v62` adds ordinary `java.math.BigDecimal` and `java.math.RoundingMode` class files to the isolated bundled bytecode runtime. Select it through `compileJarWithExceptionRuntime`, `compileClassWithExceptionRuntime`, or CLI `--exception-runtime`. Decimal arithmetic executes inside Wasm using the bundled BigInteger coefficient and signed integer scale. These classes have the bytecode API descriptors and javac Comparable bridge; source facades and host-JDK arithmetic are not used for linked execution.

Supported APIs:

- ZERO, ONE, TEN; String, char-array/slice, int, long, BigInteger/unscaled-scale and exact double constructors; long, long/scale and double valueOf factories.
- Addition, subtraction, multiplication, exact division, scale/rounding-mode division, integral division, remainder and divideAndRemainder; nonnegative integer powers.
- All eight RoundingMode enum values, values/valueOf, integer mode lookup and legacy integer rounding constants/overloads.
- Scale, precision, signum, unscaledValue, setScale, decimal-point movement, scaleByPowerOfTen, stripTrailingZeros, plus, abs, negate, ulp, min/max and typed/bridge comparison.
- Scale-sensitive equality/hash, canonical decimal/scientific toString and toPlainString; truncating and exact integral conversions, toBigInteger and toBigIntegerExact.
- Float/double conversion rounds the rational value directly to nearest with ties to even, including subnormals, overflow to infinity and signed underflow zero. The double constructor preserves the exact binary input value; valueOf(double) uses the existing bundled Double.toString path.

Nulls, malformed numerals, invalid slices/modes, nonterminating exact division, zero divisors, inexact conversions and nonzero scale overflow raise linked Java exceptions. Message text is not an exact JDK contract. Zero scale overflow is clamped where required. Text parsing accepts BMP decimal digits, signed exponents and scale-boundary exponents. Char-array inputs are copied. Values are immutable through the supported public APIs.

## Evidence

`BytecodeBigDecimalTest` compiles javac fixtures and compares them with JVM execution, then assembles WAT and runs Node/Wasm in both JAR orders and two independent instances using the default imported memory limit. Cases cover preferred arithmetic scales, all rounding directions and ties, fractional/overflow conversions, scale extremes, malformed input, exact binary-double construction and IEEE exponent/precision boundaries. An unsupported toEngineeringString call must fail admission.

The library fixture verifies pinned Hibernate Core 6.4.4.Final and Jakarta Persistence API 3.1.0 artifact hashes, then retains **six original BigDecimal coercion overloads and the shared error wrapper** from Hibernate's CoercionHelper. It links the unchanged Coercer interface and CoercionException/HibernateException/PersistenceException hierarchy. JVM/Wasm comparisons exercise exact primitive/BigInteger conversions, double conversion, method-reference lambdas, boxing, null checks and wrapped ArithmeticException causes. Other CoercionHelper methods are removed before linking; this establishes selected-method evidence, not whole-class or Hibernate startup compatibility. Retained method instructions are not replaced by source equivalents.

## Remaining limits

MathContext and its constructors/overloads, engineering formatting, sqrt and other missing descriptors remain unsupported and receive compiler diagnostics. General BigInteger bitwise/modular/primality operations, streams and other core-JDK work remain open. The decimal algorithms do not promise JDK performance; large coefficients, scale alignment and plain formatting consume memory proportional to their expanded results. Arbitrary precision is bounded by available Wasm memory. Serialization, managed reclamation, concurrency and source/bytecode object marshalling remain separate milestones. This subset does not establish complete H2/Spring/Hibernate execution or complete the core-JDK parent.

The selected whole-JAR core-signature inventory has 740 declared signatures, 206 missing members and 391 missing owners. It includes declarations for 50 BigDecimal and nine RoundingMode signatures; the inventoried BigDecimal.round(MathContext) remains missing. These counts measure declarations, not execution coverage or reachability. The reviewed 63-JAR baseline changes only the compiler revision; inventory counts are unchanged.

Validation: all 280 bytecode regressions pass, including eight BigDecimal tests and five audit tests. The reviewed 63-JAR audit baseline passes with unchanged inventory counts and revision `big-decimal-v62`.
