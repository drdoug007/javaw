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
| `INVOKESTATIC`, `INVOKESPECIAL` | Admitted only for concrete linked targets with matching static/instance kind. Interface-super calls resolve direct/inherited public defaults. The empty Object constructor is a no-op |
| `IRETURN`, `ARETURN`, `RETURN` | Admitted |
| Labels, line numbers, stack-map frames | Compiler metadata, not instructions; ASM analysis supplies entry stack depths and checks stack shape |
| Long, float, double descriptors/fields and remaining numeric opcodes | Rejected; typed source lowering is available for supported source operations |
| String/class/method-handle/constant-dynamic `LDC` | Rejected; placeholder class pointers and uninitialized string data are not emitted |
| `INVOKEDYNAMIC` (including javac lambdas and string concatenation) | Rejected; bootstrap/linkage support is pending. Source lambdas and concatenation have separate implementations |
| `INVOKEVIRTUAL`, `INVOKEINTERFACE` | Admitted for public methods in linked closed-world hierarchies with class-provided or maximally-specific default implementations; interface declarations may be inherited. Accessible private helpers bind to their declaring method; protected/package virtual calls receive diagnostics |
| `NEW`, `GETFIELD`, `PUTFIELD` | Admitted for linked concrete classes with a complete superclass chain through Object and i32 fields; inherited parent-prefix layouts, hidden fields, and inherited symbolic field owners are supported; shared descriptor-qualified layouts and size-based zero initialization. Null access traps; Java exception mapping is pending |
| Array creation/access, casts, `INSTANCEOF`, multianewarray | Rejected |
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

### Bytecode object allocation contract

`NEW` uses `env.__heap_alloc(size)` and clears the entire payload with `memory.fill`, including a four-byte runtime class-ID header; `NEW` then writes the module-local class ID. Each instance field occupies four bytes after its superclass payload; hidden fields retain distinct slots, resolved by symbolic owner, name, and descriptor; references are opaque i32 addresses. The host allocator must return a nonzero, four-byte-aligned, writable region of the requested size without overlapping live allocations. A zero result traps. These modules require WebAssembly bulk-memory support. This is a bytecode layout, with class IDs assigned in sorted internal-name order and no vtable, and is not a promise of compatibility with source-backend objects.

Field access and instance calls trap on null. Catchable Java NullPointerException and OutOfMemoryError, automatic collection and protected/package virtual calls remain pending. Allocation requires a concrete linked class with its entire superclass chain linked through Object. Abstract intermediate superclasses are supported; allocating an abstract class or using a missing/cyclic superclass chain receives diagnostics. Static fields still require their directly declaring symbolic owner. Constructor calls use the existing direct INVOKESPECIAL path. `BytecodeCompatibilityTest` executes objects larger than 64 bytes, dirty-memory zero defaults, constructor initialization, separate allocations, aliases/cycles, null reads/writes, cross-class field access, and multilevel inheritance in a linked JAR. Inheritance tests cover abstract intermediate classes, superclass constructor initialization, hidden fields with matching/different descriptors, static-field exclusion, inherited symbolic field owners, parent/subclass aliases, independent parent/subclass allocations, and reverse JAR hierarchy order.

### Bytecode dynamic dispatch contract

For public virtual/interface calls the compiler enumerates linked concrete receiver classes assignable to the symbolic owner and resolves the most-derived public class implementation by method name and descriptor, falling back to a maximally-specific public interface default when no class declaration exists. Calls use a linear class-ID comparison chain followed by a direct Wasm call. Class IDs and layouts agree across a linked JAR, independent of entry order. Abstract intermediate classes and inherited interface declarations are supported. Public interface declarations may be abstract or default. Each concrete eligible class must have a linked class/default implementation and a complete layout. Null or unmatched receiver IDs trap. As with all raw Wasm references, arbitrary host-supplied addresses are outside the Java object contract; IDs are not pointer validation.

`NEW` installs the most-derived class ID before any constructor runs, preserving virtual calls during superclass construction. `INVOKESPECIAL` retains its direct binding, including directly declared superclass targets. Arguments are evaluated before the receiver null check; void and i32/reference results preserve the caller operand stack. Dynamic class loading, protected/package virtual access/override rules, Object methods supplied by the JDK, catchable linkage/null exceptions, and general JVM verification remain pending. Module-local class IDs and object pointers must not be shared between independently linked modules.

`BytecodeCompatibilityTest` executes public class/interface overrides, inherited implementations and symbolic owners, inherited interface declarations, abstract intermediate classes, constructor-time dispatch, direct super calls, nested/multiple arguments, void and reference returns, argument side effects before null failure, and unrelated-receiver rejection. Unsupported dynamic calls include source/method diagnostics.

Default resolution collects matching non-static, non-private declarations from the linked interface hierarchy and suppresses ancestor declarations whenever a subinterface redeclares the method. An abstract redeclaration suppresses an ancestor default. A class declaration takes precedence over every default, including when that class declaration is abstract. One maximally-specific concrete default supplies the implementation; conflicting concrete defaults or a concrete receiver without an implementation receive call-site diagnostics. Missing interface hierarchy dependencies also receive diagnostics when default resolution requires them. `INVOKESPECIAL` interface-super calls bind directly to the resolved public default, including an inherited default named through a direct superinterface.

Executable default-method tests cover diamond inheritance, class overrides inherited from a superclass, re-abstracted methods implemented by a class, defaults calling abstract and other default methods, void/reference returns, and direct/inherited interface-super calls. Modified separately compiled class files verify diagnostics for conflicting defaults, interface re-abstraction without an override, and abstract superclass declarations suppressing a default. These rules extend the existing public i32/reference closed-world subset; private interface helpers are supported as described below; protected/package virtual calls remain outside it.

### Private bytecode methods and nests

Private class/interface calls bind to the resolved declaring method and descriptor, even when the runtime subclass declares a same-named public method. The existing receiver class-ID checks and null traps remain in place for virtual/interface opcodes. Private helpers can return i32/reference values or void, and overloads retain distinct internal symbols. Public/default dispatch keeps its existing overriding rules.

Private method access requires either the declaring class itself or a validated linked nest. For nestmate calls, the caller and declaring class must name the same linked host, the host must list both members in NestMembers (unless it is itself a participant), and all participants must have the same package. Missing, mismatched, nonreciprocal, or cross-package nest metadata produces a call-site diagnostic. The checks also cover private static calls and private constructors/direct INVOKESPECIAL calls. This is private-method admission, not a complete JVM verifier, module-access model, or private-field access audit.

`BytecodeCompatibilityTest` executes modern javac private class/interface helpers, same-named subclass methods, descriptor overloads, void/reference results, argument effects before null failure, reciprocal host/member and member/member calls, private constructors, and Java 8 private INVOKESPECIAL bytecode. Negative tests reject missing hosts, nonreciprocal membership, and forged cross-package nests across virtual/special/static call kinds. Protected/package virtual access and override rules remain pending.
