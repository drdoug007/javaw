# Bytecode fused multiply-add

Revision `fused-multiply-add-v79` adds the bundled `Math.fma(double,double,double)` and `Math.fma(float,float,float)` declarations to the isolated bytecode runtime. Select it through `compileJarWithExceptionRuntime`, `compileClassWithExceptionRuntime` or CLI `--exception-runtime`. Source facades do not establish this binary compatibility.

## Arithmetic contract

Both overloads compute the exact product plus the exact addend and then round once to the destination IEEE-754 format, using nearest with ties to even. Float arithmetic rounds directly to binary32; a fused double result cast to float can round twice and is not used. Intermediate product overflow/underflow does not lose information: `fma(MAX_VALUE,2,-MAX_VALUE)` returns MAX_VALUE, and a product smaller than the smallest subnormal still participates in final rounding and cancellation.

The ordinary bundled `javaw.runtime.FusedMath` bytecode decodes finite inputs into signed binary significands and exponents, multiplies into base-65536 integer digits, aligns and adds/subtracts exact magnitudes, then rounds using retained, guard and sticky bits. It uses existing bit-conversion adapters and integer instructions, with no JavaScript math import, BigInteger arithmetic or new Wasm feature requirement. Alignment spans at most 3172 bits for double and 426 bits for float. Two aligned arrays hold at most 200 digits each for double, or 28 for float, plus an eight-digit product workspace.

NaNs produce NaN; payloads and signs are unspecified. Infinity multiplied by zero and opposite infinite product/addend signs produce NaN. A finite exact product plus an infinite addend returns that addend, even if ordinary multiplication would overflow. Other infinite products retain their sign. Exact nonzero cancellation gives positive zero; zero operands preserve the Java product/addend zero-sign rules, and underflow of a nonzero exact result retains its sign.

Calls and method references use the ordinary descriptor-qualified Math overloads. Malformed `(DDD)F` calls are rejected by existing linkage admission; they do not select either overload or compile to an unfused operation.

## Executable evidence

`BytecodeFusedMathTest` compares canonical result-bit hashes against the JVM in both JAR orders and two independent Wasm instances per batch:

- A 23-by-23-by-23 boundary/special-value matrix for each precision, including signed zeros, both infinity signs, NaNs, subnormals, normal boundaries, maximum finite values and adjacent significands.
- Another 768 deterministic raw-pattern triples per precision, including cancellation against the ordinarily rounded product. Each precision has 12,935 matrix/random triples in total.
- Direct calls and functional-interface method references; explicit checks for intermediate-overflow cancellation, double cancellation lost by ordinary multiply-add, and float results that differ from a fused-double result cast to float.
- An ASM fixture with a malformed public fma descriptor, which must fail before WAT generation.

The library regression verifies the SHA-256 of Jackson Core 2.15.4 and selects `META-INF/versions/17/.../FastDoubleSwar.class`. It retains the **original fma method body**, which invokes Math.fma, with the original package access. A caller in the same package runs the same double matrix and random triples against that selected method on both the JVM and Wasm. Other methods and fields are excluded. This distinction matters: the base Java 8 entry uses unfused multiply/add and is not the tested entry. The evidence covers this selected Java 17 method, not the whole helper class, FFT implementation or Jackson parser.

The selected whole-JAR signature inventory now has 974 declared signatures, 61 missing members and 302 missing owners. Only the inventoried double fma reference accounts for the declaration-count improvement; counts are not reachability or semantic-coverage proofs. The reviewed 63-JAR dependency baseline changes only its compiler revision.

## Memory and remaining limits

Finite nonzero products allocate the bounded digit arrays on each call. Fresh Wasm instances per test batch keep the bump-only test allocator within its default 20-page limit; this is not a sustained-allocation or reclamation claim. General managed library memory and concurrency remain separate milestones. No JDK performance or allocation-free intrinsic guarantee is claimed. Remaining logarithmic, exponential, trigonometric and power APIs, the broader core-JDK gate and framework startup stay open.

## Validation

All 347 bytecode tests pass with zero failures, errors or skips. The required offline reactor command used `-DforkCount=4` for four isolated test JVMs. The reviewed `python3 runtime-target/audit.py --check-baseline` gate passes with unchanged dependency inventory counts and only the compiler revision updated in the baseline.
