# Executable compatibility and runtime boundaries

Java source compilation and JVM bytecode compilation are different backends. A
source facade does not make the corresponding JDK class file usable in a JAR.
The JVM bytecode backend currently admits a conservative numeric/reference subset. It rejects
other constructs before WAT is returned, including constructs in unreachable
instructions. This is intentionally stricter than the previous emitter, which
could silently discard instructions or emit invalid typed locals.

## Bytecode admission matrix

| Construct / opcode family | Current behavior |
| --- | --- |
| `NOP`, `ACONST_NULL`, `ICONST_M1`–`ICONST_5`, `BIPUSH`, `SIPUSH`, numeric `LDC` | Admitted |
| `ILOAD`, `ALOAD`, `ISTORE`, `ASTORE`, `IINC` | Admitted; references are opaque i32 values |
| `POP`, `DUP`, `POP2`, `DUP_X1`, `DUP_X2`, `DUP2`, `DUP2_X1`, `DUP2_X2`, `SWAP` | Admitted for valid numeric/reference category-one/two shapes |
| `IADD`, `ISUB`, `IMUL`, `INEG`, `IAND`, `IOR`, `IXOR`, `ISHL`, `ISHR`, `IUSHR` | Admitted; Java i32 wrapping and shift widths |
| `IDIV`, `IREM` | Admitted; signed division truncates toward zero, including MIN_VALUE / -1 overflow. Division by zero traps in Wasm; Java ArithmeticException mapping is pending |
| `I2B`, `I2C`, `I2S` | Admitted |
| `IFEQ`, `IFNE`, `IFLT`, `IFGE`, `IFGT`, `IFLE`, all `IF_ICMP*`, `IF_ACMPEQ`, `IF_ACMPNE`, `IFNULL`, `IFNONNULL`, `GOTO` | Admitted; state dispatch preserves branches, joins, and loop back edges |
| `GETSTATIC`, `PUTSTATIC` | Admitted for resolved numeric/reference fields, including inherited symbolic owners and interface-first lookup; access/kind/final-write checks apply. Numeric ConstantValue attributes initialize declaring globals |
| `INVOKESTATIC` | Admitted for concrete linked static declarations; inherited class methods resolve to their declaring function, with owner/member access and reference-kind checks. Interface static methods are not inherited |
| `INVOKESPECIAL` | Admitted for concrete linked instance targets with class-owner/member access, inherited resolution, exact constructors, and ACC_SUPER selection. Interface-super calls require accessible direct-superinterface/current-interface owners and resolve direct/inherited defaults or validated private helpers. The empty Object constructor is a no-op |
| `IRETURN`, `LRETURN`, `FRETURN`, `DRETURN`, `ARETURN`, `RETURN` | Admitted |
| Labels, line numbers, stack-map frames | Compiler metadata; linked typed analysis enforces supplied frame declarations and modern branch-target/dead-block frame requirements |
| `LCONST_0/1`, `LLOAD`, `LSTORE`, `LADD`, `LSUB`, `LMUL`, `LDIV`, `LREM`, `LNEG`, `LSHL`, `LSHR`, `LUSHR`, `LAND`, `LOR`, `LXOR`, `LCMP`, `I2L`, `L2I` | Admitted; typed i64 storage, Java wrapping and shift masking, signed comparison and conversions. MIN_VALUE / -1 returns MIN_VALUE; zero division/remainder traps |
| `FCONST_0/1/2`, `DCONST_0/1`, `FLOAD`, `DLOAD`, `FSTORE`, `DSTORE`, `FADD/FSUB/FMUL/FDIV/FNEG`, `DADD/DSUB/DMUL/DDIV/DNEG`, `FCMPL/FCMPG/DCMPL/DCMPG` | Admitted; typed f32/f64 storage, IEEE arithmetic and signed zero, NaN-aware comparisons |
| `I2F/I2D/L2F/L2D/F2I/F2L/F2D/D2I/D2L/D2F` | Admitted; Java rounding/promotion and saturating float-to-integer casts (NaN becomes zero) |
| `FREM`, `DREM` | Admitted; shared portable Wasm remainder helper, emitted once per module |
| String/class/method-handle/constant-dynamic `LDC` | Rejected; placeholder class pointers and uninitialized string data are not emitted |
| `INVOKEDYNAMIC` (including javac lambdas and string concatenation) | Rejected; bootstrap/linkage support is pending. Source lambdas and concatenation have separate implementations |
| `INVOKEVIRTUAL`, `INVOKEINTERFACE` | Admitted for public methods in linked closed-world hierarchies with class-provided or maximally-specific default implementations; interface declarations may be inherited. Accessible private helpers bind to their declaring method; protected/package virtual calls enforce caller access, override slots, and the protected receiver rule |
| `NEW`, `GETFIELD`, `PUTFIELD` | Admitted for linked concrete classes with a complete superclass chain through Object and numeric/reference fields; inherited parent-prefix layouts, hidden fields, and inherited symbolic field owners are supported; shared descriptor-qualified layouts and size-based zero initialization. Null access traps; Java exception mapping is pending |
| `CHECKCAST`, `INSTANCEOF` | Admitted for accessible linked classes/interfaces and Object; runtime class-ID tests, null handling, failed-cast traps |
| Array creation/access, array type tests, multianewarray | Rejected |
| `TABLESWITCH`, `LOOKUPSWITCH` | Admitted; selector comparisons select a state-dispatch successor, preserving default, shared targets, fallthrough, and stack values below the selector. Dispatch uses a linear comparison chain |
| `JSR`, `RET` | Normalized with ASM subroutine inlining before admission/frame analysis; recursive subroutines rejected, handler support checked independently |
| `ATHROW`, exception handler tables | Rejected; use source compilation for supported Java exceptions |
| Native/synchronized methods, volatile fields, monitor opcodes | Rejected; no general JNI, monitor, or Java Memory Model runtime |
| `<clinit>` | Rejected; automatic JVM class initialization is pending |
| Abstract method declarations | No function body; calls must resolve to admitted concrete implementations |
| Overloaded public method exports | Rejected with remediation; use unique public entry-point names. Internal method symbols include the descriptor |
| Every other opcode | Rejected; the instruction visitor also fails closed if admission and lowering diverge |

An admitted module imports `env.memory` (2 initial / 20 maximum pages) and
`env.__heap_alloc`. Pure numeric methods need no allocation. This memory ABI
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

`NEW` uses `env.__heap_alloc(size)` and clears the entire payload with `memory.fill`, including a four-byte runtime class-ID header; `NEW` then writes the module-local class ID. Each instance field occupies four bytes (i32/reference/float) or eight bytes (long/double) after its superclass payload; hidden fields retain distinct slots, resolved by symbolic owner, name, and descriptor; references are opaque i32 addresses. The host allocator must return a nonzero, four-byte-aligned, writable region of the requested size without overlapping live allocations. A zero result traps. These modules require WebAssembly bulk-memory support. This is a bytecode layout, with class IDs assigned in sorted internal-name order and no vtable, and is not a promise of compatibility with source-backend objects.

Field access and instance calls trap on null. Catchable Java NullPointerException and OutOfMemoryError, automatic collection remain pending. Allocation requires a concrete linked class with its entire superclass chain linked through Object. Abstract intermediate superclasses are supported; allocating an abstract class or using a missing/cyclic superclass chain receives diagnostics. Static fields resolve inherited symbolic owners to the declaring global. Constructor calls use the existing direct INVOKESPECIAL path. `BytecodeCompatibilityTest` executes objects larger than 64 bytes, dirty-memory zero defaults, constructor initialization, separate allocations, aliases/cycles, null reads/writes, cross-class field access, and multilevel inheritance in a linked JAR. Inheritance tests cover abstract intermediate classes, superclass constructor initialization, hidden fields with matching/different descriptors, static-field exclusion, inherited symbolic field owners, parent/subclass aliases, independent parent/subclass allocations, and reverse JAR hierarchy order.

### Bytecode dynamic dispatch contract

For public virtual/interface calls the compiler enumerates linked concrete receiver classes assignable to the symbolic owner and resolves the most-derived public class implementation by method name and descriptor, falling back to a maximally-specific public interface default when no class declaration exists. Calls use a linear class-ID comparison chain followed by a direct Wasm call. Class IDs and layouts agree across a linked JAR, independent of entry order. Abstract intermediate classes and inherited interface declarations are supported. Public interface declarations may be abstract or default. Each concrete eligible class must have a linked class/default implementation and a complete layout. Null or unmatched receiver IDs trap. As with all raw Wasm references, arbitrary host-supplied addresses are outside the Java object contract; IDs are not pointer validation.

`NEW` installs the most-derived class ID before any constructor runs, preserving virtual calls during superclass construction. Class `INVOKESPECIAL` resolves inherited declarations and applies ACC_SUPER selection from the direct superclass; constructors bind only to exact declarations. Arguments are evaluated before the receiver null check; void and numeric/reference results preserve the caller operand stack. Dynamic class loading, complete class/module/linkage access checks, Object methods supplied by the JDK, catchable linkage/null exceptions, and general JVM verification remain pending. Module-local class IDs and object pointers must not be shared between independently linked modules.

`BytecodeCompatibilityTest` executes public class/interface overrides, inherited implementations and symbolic owners, inherited interface declarations, abstract intermediate classes, constructor-time dispatch, direct super calls, nested/multiple arguments, void and reference returns, argument side effects before null failure, and unrelated-receiver rejection. Unsupported dynamic calls include source/method diagnostics.

Default resolution collects matching non-static, non-private declarations from the linked interface hierarchy and suppresses ancestor declarations whenever a subinterface redeclares the method. An abstract redeclaration suppresses an ancestor default. A class declaration takes precedence over every default, including when that class declaration is abstract. One maximally-specific concrete default supplies the implementation; conflicting concrete defaults or a concrete receiver without an implementation receive call-site diagnostics. Missing interface hierarchy dependencies also receive diagnostics when default resolution requires them. `INVOKESPECIAL` interface-super calls bind directly to the resolved public default, including an inherited default named through a direct superinterface.

Executable default-method tests cover diamond inheritance, class overrides inherited from a superclass, re-abstracted methods implemented by a class, defaults calling abstract and other default methods, void/reference returns, and direct/inherited interface-super calls. Modified separately compiled class files verify diagnostics for conflicting defaults, interface re-abstraction without an override, and abstract superclass declarations suppressing a default. These rules extend the existing numeric/reference closed-world subset; private helpers and protected/package methods are supported as described below.

### Private bytecode methods and nests

Private class/interface calls bind to the resolved declaring method and descriptor, even when the runtime subclass declares a same-named public method. The existing receiver class-ID checks and null traps remain in place for virtual/interface opcodes. Private helpers can return numeric/reference values or void, and overloads retain distinct internal symbols. Public/default dispatch keeps its existing overriding rules.

Private method access requires either the declaring class itself or a validated linked nest. For nestmate calls, the caller and declaring class must name the same linked host, the host must list both members in NestMembers (unless it is itself a participant), and all participants must have the same package. Missing, mismatched, nonreciprocal, or cross-package nest metadata produces a call-site diagnostic. The checks also cover private static calls and private constructors/direct INVOKESPECIAL calls. Private fields share the same validated nest checks. Complete JVM verification and module access remain open.

`BytecodeCompatibilityTest` executes modern javac private class/interface helpers, same-named subclass methods, descriptor overloads, void/reference results, argument effects before null failure, reciprocal host/member and member/member calls, private constructors, and Java 8 private INVOKESPECIAL bytecode. Negative tests reject missing hosts, nonreciprocal membership, and forged cross-package nests across virtual/special/static call kinds. Protected/package virtual calls are supported as described below.

### Protected and package-access virtual methods

Virtual method admission checks access against the resolved declaring class and current caller. Package-access methods require the same package. Protected methods admit callers in the declaring package or linked subclasses; outside the declaring package, eligible runtime receivers must be instances of the caller class or its subclasses. Null and excluded receiver IDs trap. Inaccessible call sites receive file/method/remediation diagnostics before WAT is returned. Packages here use internal-name prefixes within one closed-world module, without separate class loaders. Interface calls still require public class/default implementations, apart from the separately checked private-helper path.

Dispatch follows the resolved class-method slot through the receiver's superclass chain. A same-named method declared in another package does not override a package-private ancestor slot. A same-package override can widen access; later subclasses then override it transitively across packages. Private/static declarations do not override an instance slot. Overrides reducing public/protected visibility receive diagnostics.

`BytecodeCompatibilityTest` compiles multi-package fixtures and compares Wasm results directly with JVM execution for package slots, unrelated same-named methods, returning to the original package, and transitive overrides after access widening. Tests also execute same-package protected access and subclass overrides, reject unrelated callers, and use an explicit superclass symbolic owner to verify protected base/sibling receiver traps. Complete JVM class/module accessibility, complete special-call verification, linkage exceptions, and verification remain separate work.

### Long bytecode execution

Long values use i64 Wasm locals and operand-stack slots. Separate typed local banks preserve JVM slot reuse across scopes; ASM entry frames restore stack types at joins. Parameter copies honor two-slot long positions in JVM descriptors, while calls consume one mapped value per argument. Long instance fields occupy eight bytes, including inherited and hidden fields; accesses allow the unaligned offsets produced by the four-byte object header and packed layout. Long ConstantValue attributes initialize i64 globals. The compiler semantics cache revision is `1.0-SNAPSHOT-reference-inference-v15`, invalidating older layouts/output.

Arithmetic wraps at 64 bits. Shifts accept an i32 distance and use its low six bits, including negative distances. LCMP returns signed -1/0/1 without subtract-overflow errors; I2L sign-extends and L2I retains the low 32 bits. Division truncates toward zero and handles MIN_VALUE / -1 without a Wasm overflow trap. Division/remainder by zero still trap instead of throwing catchable ArithmeticException. Node hosts pass and receive long exports as BigInt.

`BytecodeCompatibilityTest` executes signed extremes, values beyond JavaScript number precision, long branch joins/loops, int/long slot reuse, mixed-width parameters, inherited/hidden fields, long post-increment, static/default/virtual calls, and every valid integral DUP/SWAP category shape plus both POP2 forms. Long field/call results are compared directly with JVM execution; arithmetic, shifts, conversions, and comparisons are checked against exact BigInt expectations. Numeric arrays, automatic class initialization, and exception mapping remain pending.

### Floating-point bytecode execution

Float and double values use f32/f64 local and stack banks, restored from ASM frames at joins. Double parameters occupy two JVM local slots, as long parameters do. Instance layouts store float in four bytes and double in eight; fields, numeric ConstantValue globals, and LDC literals preserve finite values, signed zero, infinities, and NaN values. Literal formatting uses Wasm nan/inf spellings. NaN payload identity is not promised. The `reference-inference-v15` cache revision invalidates earlier compiled output.

Arithmetic performs IEEE binary32/binary64 addition, subtraction, multiplication, division, and negation. Float operations round at each f32 instruction. FCMP/DCMP distinguish the specified NaN result for the L/G variants and treat signed zeros as equal. Float/double-to-int/long casts use saturating conversions: truncate toward zero, clamp overflow/infinities, and map NaN to zero. Integral-to-floating conversions use signed round-to-nearest conversion; double-to-float demotes and float-to-double promotes. Hosts use JS numbers for floating exports and BigInt for long arguments/results.

`BytecodeCompatibilityTest` executes both comparison variants through all relational operators; signed zero, infinities, subnormals, arithmetic overflow/underflow, saturation boundaries, typed joins/slot reuse, zero-initialized fields, and floating constant globals/literals. JVM comparisons cover mixed int/long/float/double default/virtual calls, inherited/hidden fields and post-increments, and long-to-float conversion near a double-rounding boundary. Additional ASM fixtures verify floating category-one/two shuffles. Numeric arrays, automatic class initialization, general exceptions, and the full JDK runtime remain pending.

Floating remainder (`FREM`/`DREM`) shares the source backend’s portable Wasm runtime, emitted once per standalone class or linked JAR only when needed. Power-of-two magnitude reduction avoids quotient overflow and cancellation; f32 operands promote exactly to f64 before reduction and demote afterward. Zero divisors, infinite dividends, and NaN inputs return NaN; finite dividends modulo infinity return the dividend, and zero results preserve the dividend sign. No host math import is required. NaN payload identity is not promised. Tests compare 2,848 edge/random raw-bit vectors against JVM results and execute conditional joins, nested remainders, and calls across linked classes.

### Bytecode reference type tests

`CHECKCAST` and `INSTANCEOF` use the same deterministic module-local class IDs as virtual dispatch. Assignability follows linked superclass chains and transitive interfaces; abstract classes and interfaces participate as targets, including targets with no concrete implementation. Object tests accept any supported allocated object. Null returns false for `INSTANCEOF` and passes through `CHECKCAST`; successful casts retain the original reference. Failed casts trap rather than throwing catchable ClassCastException. Tests execute inherited/interface checks, casts followed by field reads or interface calls, nested casts, branch joins, nulls, and unrelated receivers.

Targets must be linked and public or in the caller’s package (Object is built in). The compiler rejects array targets and missing/cyclic linked hierarchies before returning WAT, with call-site diagnostics. Arrays, general JVM verifier/access rules, class initialization, and exception mapping remain pending. Host-provided references must be valid object pointers from the same module instance; arbitrary forged pointers are outside the runtime contract. The compiler cache revision is `1.0-SNAPSHOT-reference-inference-v15`.

### Bytecode field linkage and access

Field references resolve by name and descriptor: first the symbolic owner’s declarations, then its direct superinterfaces recursively, then its superclass. Lookup stops at the first declaration, so a static/instance mismatch is diagnosed rather than bypassing a hiding declaration. Inherited static reads/writes address the declaring class’s global; instance accesses retain descriptor-qualified parent-prefix offsets. Resolution diagnoses missing fields and missing/cyclic hierarchy paths encountered during lookup.

The symbolic owner must be public or in the caller’s package. Public fields are accessible; package fields require the declaring package, protected fields also admit linked subclasses, and private fields require the declaring class or the existing validated reciprocal same-package nest. Cross-package protected instance accesses check that the runtime receiver is the caller class or a subclass; null and excluded receivers trap. Static protected accesses have no receiver restriction. Final writes are admitted only in the declaring class’s constructor (`PUTFIELD`) or class initializer (`PUTSTATIC`); automatic class initialization is still rejected independently.

Executable regressions cover inherited long static storage, interface-before-superclass resolution, hidden fields with different descriptors, private instance/static nestmate writes, constructor final initialization, and protected reads/writes on subclass receivers. Admission regressions reject inaccessible private/package/protected fields, opcode-kind mismatches, and final writes outside initializers. These checks do not establish complete JVM verification, module/class-loader access, reference-type verification, or catchable linkage errors. The cache revision is `1.0-SNAPSHOT-reference-inference-v15`.

### Bytecode static method linkage

`INVOKESTATIC` resolves the symbolic class’s declaration by name and descriptor, then searches its superclass chain. The first matching declaration controls static hiding; an instance, abstract, or native declaration is rejected rather than skipped. Lowering calls the resolved declaring function, preserving argument evaluation and numeric types. Interface static calls require a declaration on that exact interface: static methods are not inherited by subinterfaces or implementing classes. The instruction’s interface reference flag must match its symbolic owner, and initializer names cannot be called with `INVOKESTATIC`.

The symbolic owner must be public or in the caller’s package. Resolved member access uses the same checks as virtual methods: public, same declaring package, protected subclass, or validated private nest access. Static calls have no protected receiver restriction. Missing declarations and missing/cyclic superclass paths encountered during lookup produce call-site diagnostics before WAT is returned. Automatic class initialization, complete JVM verification/module access, complete `INVOKESPECIAL` verification, and catchable linkage exceptions remain pending.

Executable regressions compare JVM results for inherited mixed int/long/float/double calls, static hiding through multiple subclass levels, package/protected access, direct interface static calls, and nested side-effecting arguments. Admission regressions reject inaccessible owners/members, reference-kind mismatches, wrong descriptors, non-static/native/abstract targets, inherited interface statics, initializer calls, and missing/cyclic lookup paths. Existing nestmate regressions cover private static calls. The cache revision is `1.0-SNAPSHOT-reference-inference-v15`.

### Class bytecode special calls

Class `INVOKESPECIAL` uses shared admission/lowering resolution. Constructors require an exact name/descriptor declaration on the symbolic owner and are never inherited. Other methods resolve along the class chain, falling back to maximally-specific interface defaults inherited by that class. The symbolic owner must be public or in the caller’s package, and the resolved member must satisfy public/package/protected/private nest access. Non-constructor calls require the caller itself or a superclass as symbolic owner. Initializer names other than `<init>`, wrong owner kinds, static/abstract/native selected targets, missing declarations, and missing/cyclic lookup paths receive call-site diagnostics.

For superclass references, ACC_SUPER selection starts again at the caller’s direct superclass, so a grandparent symbolic reference selects an intervening override. Modern class files (Java 8+) use this behavior regardless of the stored ACC_SUPER bit. Selected calls bind directly and do not dispatch to receiver subclass overrides. Existing interface-special resolution remains separate. The built-in empty Object constructor remains a no-op after the receiver null check.

Executable tests compare JVM results for a grandparent symbolic reference selecting an intermediate override, inherited mixed-width methods, and interface defaults inherited by a superclass, with reversed linked-JAR orders. Existing tests execute exact superclass/private/nestmate constructors and Java 8 private special calls. Complete reference-type and uninitialized-object verification, protected-constructor verifier rules, class initialization, and catchable linkage exceptions remain pending. Raw exported instance methods assume valid module-local receivers. The cache revision is `1.0-SNAPSHOT-reference-inference-v15`.

### Interface bytecode special-call admission

An interface `INVOKESPECIAL` reference must name the caller interface itself or one of the caller’s directly declared superinterfaces. An indirect ancestor or unrelated interface is rejected even if it declares a compatible default. The symbolic owner must be public or in the caller’s package; its reference must have the interface flag, and initializer names are rejected. The target hierarchy must be linked and acyclic. Resolution selects the owner’s own declaration or its maximally-specific inherited default, preserving abstract redeclaration and conflict diagnostics. Static, abstract, native, and inaccessible targets are rejected. Private targets retain the existing validated same-package reciprocal nest checks, including calls from a nestmate that directly implements the declaring interface.

Executable tests compare JVM results for defaults inherited through a direct superinterface, bypassing receiver overrides, mixed-width arguments, void/reference results, private same-interface helpers, and direct-superinterface private nestmate calls. Admission tests reject unrelated/indirect/inaccessible owners, wrong reference kinds, initializers, missing methods/dependencies, cyclic interfaces, and unsupported target kinds. Receiver assignment compatibility, uninitialized-object verification, complete JVM linkage/module access, and catchable linkage exceptions remain pending. The cache revision is `1.0-SNAPSHOT-reference-inference-v15`.

### Symbolic class and interface accessibility

A shared public/package check covers the symbolic owner of `NEW`, field instructions, type tests, and all invocation paths. A non-public owner must be in the caller’s package; names use internal-name prefixes in the single linked module. Member access is checked separately against its resolved declaration. In particular, a public method/default inherited from a non-public superclass/interface remains callable through an accessible symbolic subclass/subinterface; the declaring type need not be public. Virtual/interface invocation also checks that the opcode, reference interface flag, and owner kind agree, and rejects initializer names before resolving dispatch targets.

Executable regressions compare JVM results for package-local hidden class/interface access and calls through public subclasses/subinterfaces with hidden inherited declarations. The class fixture removes javac’s accessibility bridge to exercise inherited resolution directly. Admission regressions reject inaccessible allocation and dynamic-call owners, class/interface reference mismatches, and virtual/interface initializer names; existing field/static/special/type-test regressions continue to verify the shared check. This does not implement module/class-loader access, complete receiver or uninitialized-object verification, or catchable linkage exceptions. The cache revision is `1.0-SNAPSHOT-reference-inference-v15`.

### Bytecode hierarchy declarations

Before validating instructions or lowering methods, the compiler walks each class/interface’s linked ancestors. All parents must be linked except the built-in Object root. Classes must extend a class and cannot extend a final class; interfaces must name Object as their class-file superclass, and entries in the interfaces list must name interfaces. Duplicate direct interfaces and superclass/superinterface cycles are rejected. Shared ancestors in diamonds are valid. Each parent must be public or in the package of the child that directly declares that edge, rather than the package of an unrelated entry class.

These checks apply even to declarations with no executable methods. Hierarchy errors identify the offending declaring class and source file when available; class-level declarations have no bytecode instruction line and report `line unavailable`. Invalid hierarchy declarations may now be rejected before the instruction-specific diagnostics previously produced during member lookup or allocation. Tests cover missing/transitive parents, both cycle kinds, parent kind/access errors, final parents, duplicate interfaces, and entry-versus-declaring-child access. Executable tests preserve shared interface diamonds, abstract intermediates, final leaves, and both JAR hierarchy orders; existing JVM comparisons for public types with hidden ancestors remain passing.

The current [declaration/linkage contract](#closed-world-declaration-and-linkage-contract) completes access-flag, override and unnamed-module sealed checks. Full reference/uninitialized-object verification, named modules/class loaders, automatic class initialization and catchable linkage exceptions remain pending.

### Class instance override declarations

Before lowering method bodies, the compiler validates each declared non-private instance method against matching superclass method names/descriptors. It reuses virtual dispatch’s override selection: public/protected slots can be overridden across packages, package-private slots require the declaring package, and a widening override can establish a slot that later subclasses override across packages. An overriding declaration cannot replace a final selected method or reduce public/protected visibility. These checks run independently of virtual call sites and report the overriding method with available source information.

A same-named method in another package does not override an inaccessible package-private final method; private superclass methods likewise occupy separate declarations. Returning to the original package can override the original package slot and is rejected when that slot is final. Constructors and declared private/static methods are outside this override pass. Tests reject final overrides, visibility reduction, final widening bridges, and returns to a final package slot without needing virtual calls. JVM comparisons execute legal package/private slot separation, final leaf overrides, and inherited final methods in both JAR orders.

The current [declaration/linkage contract](#closed-world-declaration-and-linkage-contract) covers method/interface flags and static/instance invocation-kind conflicts, preserving legal distinct inherited slots. Full reference/uninitialized-object verification, general overloaded export support, named modules and catchable linkage errors remain pending.

### Legacy subroutine normalization

Class bytes, linked JAR entries, and direct ClassNode inputs normalize JSR/RET with ASM’s JSRInlinerAdapter before admission and frame analysis. Shared/nested subroutines become ordinary control flow with separate return paths, preserving operand values, wide local positions, method/source metadata, and handler tables. Recursive subroutines receive source/method/remediation diagnostics. Handler tables remain unsupported independently; inlining does not establish exception handling or full Byte Buddy compatibility.

JVM/Wasm regressions execute multiple return sites, conditional/shared and nested subroutines, RET local index 300, and a long value across a subroutine call. They cover class, JAR and direct ClassNode entry points; metadata/handler preservation and recursion rejection have separate regressions. The pinned audit inventories raw JSR/RET separately in `normalized-opcodes.tsv` and successfully runs normalization on all 61 affected Byte Buddy methods. The cache revision is `1.0-SNAPSHOT-reference-inference-v15`.

### Inferred bytecode operand and reference types

Frame analysis uses a SimpleVerifier subclass with linked hierarchy metadata instead of the previous category-only BasicInterpreter. Class receivers, field values, method arguments/results, primitive local loads, returns and reference merges are checked before lowering. No host ClassLoader reflection supplies type relationships; equal external reference names and assignment to Object are recognised, while more specific relationships require linked metadata. Null and primitive/reference array descriptor assignability are modelled, but unsupported array instructions remain rejected independently.

Interface operands retain ASM/JVM verifier treatment: inference does not prove that every object implements an interface at runtime. Interface dispatch still checks runtime class IDs and traps excluded receivers until catchable linkage exceptions are implemented. Supplied initialized-type stack-map constraints are enforced as described below; constructor NEW aliases/uninitialized-this flows still need their own verifier pass. This is not a claim of complete JVM verification.

New regressions reject incorrect primitive locals/returns, unreadable locals, unrelated class field receivers, incompatible reference field writes and call arguments/results, and an invalid reference merge. Metadata tests use a linked class deliberately named java/lang/String with a custom superclass to prove that host definitions are not loaded; they also check null, interface and primitive/reference array relations. Existing executable JVM/Wasm tests cover valid linked polymorphism, numeric code, joins and legacy subroutines. The cache revision is `1.0-SNAPSHOT-reference-inference-v15`.

### Sealed bytecode hierarchies

Hierarchy admission checks each direct superclass/superinterface edge against the parent's `PermittedSubclasses` list. Unlisted children receive a hierarchy diagnostic before lowering, even without executable methods. A non-sealed permitted child may have descendants without further permits checks. Permit-list entries are names, not required linkage edges: absent permitted classes need not be loaded. Final classes cannot carry this attribute, even with an empty list; declaration checks also apply to unused linked classes. Attribute versions must be Java 17+ or Java 15/16 preview (unspecified version zero remains supported for hand-built ASM inputs).

`BytecodeHierarchyTest` exercises invalid and valid declarations; `BytecodeCompatibilityTest.executesSealedClassesAndInterfacesInEitherJarOrder` compares real javac/JVM execution with Wasm through a sealed interface, sealed abstract class, non-sealed branch and final leaf. The current [declaration/linkage contract](#closed-world-declaration-and-linkage-contract) enforces package placement for the unnamed-module target. Named-module placement and full raw class-file attribute validation remain outside this backend. This feature originally used cache revision `1.0-SNAPSHOT-sealed-hierarchy-v17`.

### Bytecode member names and parameter lengths

Declaration admission rejects empty field/method names and names containing `.`, `;`, `[` or `/`. Ordinary method names also reject angle brackets; `<init>` and `<clinit>` retain their separate initializer checks. Field names may contain angle brackets. Method parameter length must not exceed 255 JVM slots, including one for an instance receiver, two per long/double parameter, and one per other parameter (including arrays). These checks precede body lowering and include unused linked declarations.

`BytecodeDeclarationsTest` covers malformed names, constructor/ordinary signature overflow, primitive and array boundaries, and unused invalid declarations. `BytecodeCompatibilityTest.executesMaximumParameterSlotsWithJvmComparison` executes 255-int and mixed long/int static calls plus a 254-slot instance call, comparing results with the JVM. The remaining descriptor, flag, invocation-kind and special-call work is covered by the [current contract](#closed-world-declaration-and-linkage-contract). This feature originally used cache revision `1.0-SNAPSHOT-member-names-slots-v18`.

### Bytecode ConstantValue attributes

Declaration admission checks the constant-pool value type for every field's `ConstantValue`: boolean/byte/char/short/int require `Integer`, long requires `Long`, float requires `Float`, double requires `Double`, and `java.lang.String` requires `String`. Other reference and array types cannot carry this attribute. Checks include instance fields and unused linked declarations; an absent attribute remains valid. This validates constant-pool types, not source-language constant-expression or range rules.

`BytecodeDeclarationsTest` covers descriptor/value combinations and unused invalid declarations. `BytecodeCompatibilityTest.rejectsConstantValueTypeMismatchesLikeJvm` checks rejection against JVM class loading; `matchesJvmNumericConstantValuesAndIgnoredInstanceConstants` executes integer limits, signed zero, NaN and infinities, and verifies that an instance-field constant does not initialize allocated storage. Correctly typed string constants remain unsupported at runtime; this change does not add string interning or automatic class initialization. Cache revision: `1.0-SNAPSHOT-constant-values-v19`.

### Closed-world declaration and linkage contract

The Todo declaration/linkage milestone is complete for the admitted numeric/reference bytecode backend. `compileClass` links its single definition; `compileJar` links the definitions in that JAR. Both use a single loader and unnamed module. Admission validates every supplied linked declaration, hierarchy and override, including declarations with no call sites. `module-info.class` is metadata excluded by the JAR reader; executable module declarations and replacements for the intrinsic Object root are diagnosed. This contract does not imply complete JVM verification or framework execution.

| Area | Contract and evidence |
| --- | --- |
| Flags and declarations | Duplicate name/descriptor members, malformed names/descriptors, visibility conflicts, class/interface and field/method flag combinations, initializer declarations and ConstantValue types are checked. Interfaces cannot carry ACC_SUPER/ACC_ENUM; nest attributes require Java 11 and cannot include both NestHost and NestMembers. Abstract/native methods cannot carry Code; concrete methods need a body. ACC_STRICT has its version-defined meaning for class versions 46–60; reserved bits are not given new semantics. Class initializer flags other than the applicable static requirement are ignored structurally, but every `<clinit>` still receives the unsupported-initialization diagnostic. `BytecodeDeclarationsTest` and `BytecodeLinkageTest` test invalid unused declarations and JVM loading behavior. |
| Class and member identity | Duplicate declared class identities in JAR entries and mismatched metadata keys are rejected. Method/global symbols encode each complete owner/name/descriptor component separately; layout and duplicate-member keys preserve separate name/descriptor components. This avoids punctuation, sanitized-name and descriptor-separator collisions. Exports escape UTF-8 WAT strings. Private names containing unpaired UTF-16 surrogates remain distinct; public names that cannot be represented as valid UTF-8 exports receive a location and renaming diagnostic. Executable JVM/Wasm tests cover each case. |
| Descriptor resolution | Every field type, method parameter/result, reference-array component and declared exception must resolve from linked metadata, except the intrinsic Object root. Host JDK classes never supply runtime definitions. This is an eager closed-world requirement, including unused declarations; it does not simulate JVM lazy class loading. A declaration may name a non-public linked type from another package; member/type instruction access remains checked against the symbolic owner. Array descriptor validation does not add array execution. |
| Static/instance conflicts | Field opcodes must match the resolved declaration; static, virtual/interface and special calls must resolve to the corresponding method kind. Lookup preserves inherited and hidden declarations. JVM-legal static and instance methods with equal signatures in different classes occupy distinct slots, even with an inherited final instance method; these are not rejected by Java-source-only hiding rules. `preservesJvmStaticInstanceSlotsInsteadOfApplyingJavaSourceRestrictions` compares JVM/Wasm results in both JAR orders, and `rejectsAllStaticInstanceOpcodeConflictsAtLinkage` covers invalid references. |
| Final and sealed hierarchies | Every linked hierarchy is checked for parent kind, access, completeness, cycles, duplicate direct interfaces and final superclass/method constraints. Final Object methods are protected even though Object is intrinsic, including declarations in interfaces. Permitted children must appear in the sealed parent's list and share its package in the unnamed-module target. Missing permitted-list entries are not forced linkage edges. Existing package-aware override slots, private nests and widening bridges remain tested. |
| Special calls | Shared resolution/lowering preserves exact constructor binding, accessible caller/superclass or direct-superinterface owners, inherited defaults/private helpers and ACC_SUPER selection. Interface static/special references require class version 52+, with version-zero ASM metadata treated as unspecified. Non-constructor special receivers must be assignable to the caller. The Object constructor no-op requires the exact class-reference INVOKESPECIAL form; interface-reference impostors are rejected. JVM comparisons cover receiver failures, superclass selection including private resolved methods, and the intrinsic-reference failure. |

Validation: `mvn -o -pl javaw-bytecode -am -Dtest='com.example.javaw.bytecode.*Test' -Dsurefire.failIfNoSpecifiedTests=false test` passes 90 tests. The pinned dependency audit passes with unchanged inventory counts. Cache revision: `1.0-SNAPSHOT-declaration-linkage-v20`.

Separate roadmap items still cover constructor/uninitialized-this/NEW-alias and protected-constructor flow verification, arrays, catchable linkage exceptions, automatic initialization, dynamic bootstraps, named modules/multiple loaders, reflection metadata and actual JDK/framework execution. Raw constant-pool/attribute parser completeness is not claimed by validation of ASM's class model. Existing overload-export, native, monitor and other unsupported-runtime diagnostics remain in force.


## Stack-map conformance

The typed worklist verifier constrains inference with supplied StackMapTable declarations. It reads compressed SAME/SAME1/APPEND/CHOP/FULL and expanded frames without modifying the method. Incoming stack heights and local/stack types must be assignable to each declaration; verification then continues with the declared types. Declaring Object instead of a narrower class cannot authorize a subsequent narrow-class use, and TOP locals discard their previous types. Interface assignment follows JVM verification rules, leaving interface membership checks to runtime dispatch. All hierarchy and frame-type resolution uses linked metadata, never host class loading.

For class version 51 and later, branch/switch targets and instructions following non-fallthrough instructions require explicit frames. Version 50 and older retain inference when frames are absent. Declared frames also start verification of otherwise unreachable blocks. Duplicate offsets, malformed compressed shapes, missing frame types, incompatible incoming values, invalid branch labels, falling off the method, and locals/stack limits are diagnosed before lowering. Long/double locals occupy two slots and stack limits count JVM words.

This contract covers initialized numeric/reference verification. Uninitialized-this and NEW-label frame tokens require the separate constructor/alias verifier and currently receive explicit unsupported diagnostics. TOP operand-stack declarations are also explicitly unsupported; TOP locals are supported. Array descriptor assignment is checked, while array operations remain a separate runtime milestone. Exception-handler frame edges remain outside admission because exception handlers are currently rejected. This does not establish complete JVM verification or raw StackMapTable parser validation beyond ASM's model.

Validation: 101 bytecode tests pass, including compressed/expanded byte-input frames, category-two locals, wider references, discarded locals, null/primitive-array reference assignment, interface assignment, missing frames, framed dead code and malformed maps. Acceptance/rejection comparisons use the JVM verifier; the existing executable JVM/Wasm suite continues to pass. The pinned 63-JAR audit retains its inventory counts. Cache revision: `1.0-SNAPSHOT-stack-maps-v21`.
