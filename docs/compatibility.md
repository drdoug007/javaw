# Executable compatibility and runtime boundaries

Java source compilation and JVM bytecode compilation are different backends. A
source facade does not make the corresponding JDK class file usable in a JAR.
The JVM bytecode backend currently admits a conservative i32 subset. It rejects
other constructs before WAT is returned, including constructs in unreachable
instructions. This is intentionally stricter than the previous emitter, which
could silently discard instructions or emit invalid typed locals.

## Bytecode admission matrix

| Construct / opcode family | Current behavior |
| --- | --- |
| `NOP`, `ACONST_NULL`, `ICONST_M1`–`ICONST_5`, `BIPUSH`, `SIPUSH`, integer `LDC` | Admitted |
| `ILOAD`, `ALOAD`, `ISTORE`, `ASTORE`, `IINC` | Admitted; references are opaque i32 values |
| `POP`, `DUP` | Admitted for single-slot values |
| `IADD`, `ISUB`, `IMUL`, `INEG`, `IAND`, `IOR`, `IXOR`, `ISHL`, `ISHR`, `IUSHR` | Admitted; Java i32 wrapping and shift widths |
| `IDIV`, `IREM` | Admitted; signed division truncates toward zero, including MIN_VALUE / -1 overflow. Division by zero traps in Wasm; Java ArithmeticException mapping is pending |
| `I2B`, `I2C`, `I2S` | Admitted |
| `IFEQ`, `IFNE`, `IFLT`, `IFGE`, `IFGT`, `IFLE`, all `IF_ICMP*`, `IF_ACMPEQ`, `IF_ACMPNE`, `IFNULL`, `IFNONNULL`, `GOTO` | Admitted; state dispatch preserves branches, joins, and loop back edges |
| `GETSTATIC`, `PUTSTATIC` | Admitted for resolved i32 fields; integer ConstantValue attributes initialize globals |
| `INVOKESTATIC`, `INVOKESPECIAL` | Admitted only for concrete linked targets with matching static/instance kind. The empty Object constructor is a no-op |
| `IRETURN`, `ARETURN`, `RETURN` | Admitted |
| Labels, line numbers, stack-map frames | Compiler metadata, not instructions; ASM analysis supplies entry stack depths and checks stack shape |
| Long, float, double descriptors/fields and remaining numeric opcodes | Rejected; typed source lowering is available for supported source operations |
| String/class/method-handle/constant-dynamic `LDC` | Rejected; placeholder class pointers and uninitialized string data are not emitted |
| `INVOKEDYNAMIC` (including javac lambdas and string concatenation) | Rejected; bootstrap/linkage support is pending. Source lambdas and concatenation have separate implementations |
| `INVOKEVIRTUAL`, `INVOKEINTERFACE` | Rejected; JVM dynamic dispatch is pending |
| `NEW`, array creation/access, instance field access, casts, `INSTANCEOF`, multianewarray | Rejected; the old provisional heap lowering is not an admitted compatibility target |
| `TABLESWITCH`, `LOOKUPSWITCH` | Admitted; selector comparisons select a state-dispatch successor, preserving default, shared targets, fallthrough, and stack values below the selector. Dispatch uses a linear comparison chain |
| `JSR`, `RET`, remaining stack manipulation | Rejected |
| `ATHROW`, exception handler tables | Rejected; use source compilation for supported Java exceptions |
| Native/synchronized methods, volatile fields, monitor opcodes | Rejected; no general JNI, monitor, or Java Memory Model runtime |
| `<clinit>` | Rejected; automatic JVM class initialization is pending |
| Abstract method declarations | No function body; virtual/interface invocation remains rejected |
| Overloaded public method exports | Rejected with remediation; use unique public entry-point names. Internal method symbols include the descriptor |
| Every other opcode | Rejected; the instruction visitor also fails closed if admission and lowering diverge |

An admitted module imports `env.memory` (2 initial / 20 maximum pages) and
`env.__heap_alloc`. Supported integer-only code does not allocate. This memory ABI
is separate from the source backend's managed heap. References supplied to a
bytecode module must already follow its ABI; admission does not provide a JDK
object model, class loader, GC integration, or host-object marshalling.

`compileClass` resolves callees against that class. `compileJar` resolves against
classes within that JAR. Inherited lookup, linking separate dependency JARs,
JDK implementations, JNI, automatic class initialization, resources/service
loading, and multirelease selection are not general compatibility guarantees.
Dynamic loading of additional code after Wasm instantiation is unsupported.
Source `Class` descriptors describe linked types. Generated member invocation,
annotation instances, and reflective construction are available for linked
source classes, enums, and records under [the source reflection contract](reflection.md).

Diagnostics include source file and line when present, class, method descriptor,
construct/opcode, and remediation. Without debug information, diagnostics say
`line unavailable`; an artificial source line is not invented. Cache metadata
includes a compiler semantics revision, so earlier permissive output is not
reused solely because a dependency JAR hash matches.

## Executable evidence

| Case | Evidence and result |
| --- | --- |
| javac-produced integer library | [BytecodeCompatibilityTest](../javaw-bytecode/src/test/java/com/example/javaw/bytecode/BytecodeCompatibilityTest.java): assembles WAT and executes it in Node; loops, dense/sparse switches with signed extreme keys and stack-preserving joins, branch joins, conditional expressions, overloaded private calls, narrowing, constants, and division overflow |
| Unsupported bytecode/source metadata | Same suite: javac lambdas/concatenation, handler tables, native/synchronized methods, wide returns, unresolved JDK calls, class literals, arrays, class initialization, overloaded exports; ASM fixture verifies INVOKEDYNAMIC and missing-line diagnostics |
| Actual Maven ASM 9.7 JAR | Same suite reads the installed dependency artifact and verifies explicit rejection. Not a supported runtime library |
| Actual Maven JUnit Jupiter API 5.10.0 JAR | Same suite reads the installed dependency artifact and verifies explicit rejection. Not a supported runtime library |
| Dependency cache | [LocalM2CacheManagerTest](../javaw-bytecode/src/test/java/com/example/javaw/bytecode/LocalM2CacheManagerTest.java): hash and compiler-revision invalidation |
| Assertions and source diagnostics | [SourceRuntimeSupportTest](../javaw-core/src/test/java/com/example/javaw/SourceRuntimeSupportTest.java): executes Wasm assertion enable/disable, lazy checks/details, Error/Exception hierarchy, and union catches; verifies source locations and remediation |
| Linked source reflection | [ReflectionSupportTest](../javaw-core/src/test/java/com/example/javaw/ReflectionSupportTest.java): executes construction, field access, virtual/static invocation, conversions, access/target errors, runtime/nested/array/inherited annotations, linked lookup, and unsupported boundaries in Wasm |
| Repeatable and parameter annotations | [RepeatableReflectionTest](../javaw-core/src/test/java/com/example/javaw/RepeatableReflectionTest.java) and [ParameterReflectionTest](../javaw-core/src/test/java/com/example/javaw/ParameterReflectionTest.java): execute container expansion/retention/inheritance, ordered typed arrays, parameter declaration annotations, enum synthetic positions, and record propagation in Wasm |
| Byte streams | [IoSupportTest](../javaw-core/src/test/java/com/example/javaw/IoSupportTest.java): executes byte-stream growth, copy isolation, bulk operations, unsigned reads, mark/reset, skip, bounds/null errors, and no-op close in Wasm |
| Spring in-memory target | [SpringWebSupportTest](../javaw-core/src/test/java/com/example/javaw/SpringWebSupportTest.java) and [Node integration test](../examples/springboot-h2/test.js): existing lowered CRUD/HTTP, exact decimal, and memory-reclamation subset. Actual Spring/Hibernate/H2 dependency execution is pending |

Run the core and bytecode compatibility suites with:

```sh
mvn -o -pl javaw-bytecode -am test \
  -Dtest=SourceRuntimeSupportTest,IoSupportTest,BytecodeCompatibilityTest,BytecodeCompilerTest,LocalM2CacheManagerTest \
  -Dsurefire.failIfNoSpecifiedTests=false
```

## Remaining Phase 4 and Phase 5 work

Remaining reflection compatibility (library members,
full access and Class semantics); worker-safe allocation,
monitor ownership, blocking wait/notify and thread lifecycle/JMM; comprehensive
I/O/NIO/network/date-time/regex/serialization; complete JLS conversions and
inference; general Spring bean lifecycle/proxies/annotation processing and broader query languages (the [derived equality subset](spring-repository-queries.md) is supported);
JDBC, advanced transaction policies, Hibernate and the actual H2 engine (the [synchronous in-memory REQUIRED subset](spring-transactions.md) is supported); and further positively
supported third-party Maven libraries remain unimplemented. The in-memory Spring
adapter remains a distinct target. The roadmap keeps those broad tasks open.
