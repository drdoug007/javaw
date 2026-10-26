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

`BytecodeBigDecimalTest` compiles javac fixtures and compares them with JVM execution, then assembles WAT and runs Node/Wasm in both JAR orders and two independent instances using the default imported memory limit. Cases cover preferred arithmetic scales, all rounding directions and ties, fractional/overflow conversions, scale extremes, malformed input, exact binary-double construction and IEEE exponent/precision boundaries. An unsupported sqrt call must fail admission.

The library fixture verifies pinned Hibernate Core 6.4.4.Final and Jakarta Persistence API 3.1.0 artifact hashes, then retains **six original BigDecimal coercion overloads and the shared error wrapper** from Hibernate's CoercionHelper. It links the unchanged Coercer interface and CoercionException/HibernateException/PersistenceException hierarchy. JVM/Wasm comparisons exercise exact primitive/BigInteger conversions, double conversion, method-reference lambdas, boxing, null checks and wrapped ArithmeticException causes. Other CoercionHelper methods are removed before linking; this establishes selected-method evidence, not whole-class or Hibernate startup compatibility. Retained method instructions are not replaced by source equivalents.

## Remaining limits

The context arithmetic overloads documented below are supported; context divide/pow and other missing descriptors (including sqrt) remain unsupported and receive compiler diagnostics. General BigInteger bitwise/modular/primality operations, primitive streams and other core-JDK work remain open. The decimal algorithms do not promise JDK performance; large coefficients, scale alignment and plain formatting consume memory proportional to their expanded results. Arbitrary precision is bounded by available Wasm memory. Serialization, managed reclamation, concurrency and source/bytecode object marshalling remain separate milestones. This subset does not establish complete H2/Spring/Hibernate execution or complete the core-JDK parent.

At revision `decimal-context-v64`, the selected whole-JAR core-signature inventory has 797 declared signatures, 207 missing members and 333 missing owners. BigDecimal.round(MathContext) and MathContext(int) are now declared. These counts measure declarations, not execution coverage or reachability. The reviewed 63-JAR baseline changes only the compiler revision; inventory counts are unchanged.

Validation: all 280 bytecode regressions pass, including eight BigDecimal tests and five audit tests. The reviewed 63-JAR audit baseline passes with unchanged inventory counts and revision `big-decimal-v62`.

## Precision contexts (`decimal-context-v64`)

Bundled MathContext supports the UNLIMITED/DECIMAL32/DECIMAL64/DECIMAL128 constants, precision and precision/mode constructors, the JDK text constructor, accessors, equality/hash and text. Precision zero selects unlimited precision. Negative precision and malformed context text raise IllegalArgumentException; null context text or rounding modes raise NullPointerException. Hashes include the rounding enum's instance hash, so their absolute values are not compared across hosts.

BigDecimal supports round/plus/abs/negate with MathContext and all eight context constructor descriptors (String, char array, char slice, int, long, BigInteger, BigInteger/scale, double). Rounding removes coefficient digits, adjusts scale and handles a carry that increases precision. It preserves trailing zeros when precision permits, all rounding directions and ties, exact UNNECESSARY checks, zero scales and catchable scale overflow. Double construction rounds the exact binary input rather than its shortest printed decimal.

Five additional JVM/Wasm tests cover context contracts and malformed/null arguments, every rounding mode, positive/negative ties and carries, scale extrema, constants, constructors and subclass dispatch without constructor callbacks. The pinned H2 2.2.224 artifact hash is verified. A generated adapter lifts the **six original instructions from the first NEW MathContext through BigDecimal.round** in TruncateValueFunction.getValue, retaining operands and local 5. The adapter supplies the decimal stack value and precision local. It links the original MathUtils.convertLongToInt body with unrelated methods/fields excluded. This executes an original call sequence and precision conversion, not the complete getValue method, H2 Value types, SQL dispatch, or H2 startup. The same extracted adapter and helper run on the JVM and in Wasm, with both JAR orders and independent instances.

Validation: the required full bytecode suite passes all 293 tests. After the final constructor/subclass-dispatch refinement, all 13 decimal tests and five audit tests pass on rerun. The reviewed 63-JAR audit baseline passes with unchanged dependency inventory and revision `decimal-context-v64`.

## Engineering notation (`engineering-decimals-v77`)

`BigDecimal.toEngineeringString()` groups nonzero exponents into multiples of three, pads short coefficients and preserves fractional coefficient zeros. Zero formatting retains its scale through fractional zeros and an adjusted exponent. Exponent arithmetic uses long values so both int scale extrema format without overflow; engineering text need not preserve the original scale when reparsed, but represents the same numeric value.

`BytecodeBigDecimalTest.engineeringNotationPreservesScaleAndExponentGroups` compares exact JVM/Wasm output hashes across signed and zero coefficients, trailing zeros, ordinary notation thresholds, all three exponent residues and both scale extrema, in both JAR orders and independent instances. The pinned closure contains no direct toEngineeringString reference; this is API conformance evidence, not a new pinned-library execution claim. Context arithmetic and the core-JDK parent remain open.

Validation: the required full bytecode run executed 339 tests, with 338 passing and one stale test-fixture compilation failure (the unsupported sqrt fixture initially returned BigDecimal from a String method). The fixture was corrected; both affected tests pass on focused rerun. The reviewed 63-JAR audit passes with unchanged inventory counts and only the compiler revision updated.

## Context arithmetic (v80–v84)

| Operation | Revision | Behavior |
| --- | --- | --- |
| add(BigDecimal, MathContext) | context-addition-v80 | Rounds once after alignment; guard/sticky digits for distant nonzero operands and bounded padding for zeros avoid expansion proportional to extreme scale gaps. |
| subtract(BigDecimal, MathContext) | context-subtraction-v81 | Negates the subtrahend and uses context addition, preserving cancellation and preferred scales. |
| multiply(BigDecimal, MathContext) | context-multiplication-v82 | Forms the exact coefficient product and preferred scale before context rounding, including carries and UNNECESSARY failures. |
| divideToIntegralValue(BigDecimal, MathContext) | context-integral-division-v83 | Truncates toward zero, discarding only trailing coefficient zeros to fit precision; otherwise raises ArithmeticException, regardless of rounding mode. |
| remainder / divideAndRemainder(BigDecimal, MathContext) | context-remainders-v84 | Constrains the integral quotient, then computes the exact remainder without rounding it; remainder precision can exceed the context precision. |

Precision zero retains the corresponding exact operation. Null operands/contexts raise NullPointerException; zero divisors and impossible quotient precision raise ArithmeticException. Exception messages are outside the compatibility contract. Available Wasm memory bounds arbitrary precision; expanded coefficients and exact alignment can exhaust it. Integral division currently forms the exact quotient before applying its precision constraint.

Five focused `BytecodeBigDecimalTest` cases compare JVM/Wasm results in both JAR orders and independent instances. They cover every rounding mode, signed inputs, zero scales, precision-zero behavior, ties/carries, cancellation, precision boundaries and typed failures. Addition also covers extreme scale gaps and signed sticky rounding; integral division covers trailing-zero compression; remainder tests require exact results with more digits than context precision.

Each task's focused regression and reviewed 63-JAR audit passes. Each baseline update changes only the compiler revision, preserving dependency inventory counts. The pinned linkage inventory contains no direct calls to these context overloads; these tests establish API conformance, without new pinned-library execution evidence. Context divide/pow, full framework execution and the broad numeric milestone remain open.

Combined validation at `context-remainders-v84`: the required offline bytecode suite passes all 352 tests with zero failures, errors or skips (four test forks). The final 63-JAR baseline audit passes with unchanged dependency inventory.
