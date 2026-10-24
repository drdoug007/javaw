# Bytecode reflection and runtime annotations

Revision `reflection-metadata-v45` completes the reflection milestone for the selected closed-world, single-threaded bytecode runtime. Metadata comes from linked class files, including dependency JARs, rather than source facades or the compiler host's JDK. This supplies reflection primitives needed by framework startup; it does not establish full Spring/Hibernate startup, JDK compatibility, or dynamic class loading.

## Discovery and execution

Class supports declared and public/inherited method and field discovery, constructors, declaring/enclosing/member classes, enum constants and annotation queries. Enumeration uses original declarations before compiler helpers are introduced. Flags, erased descriptors, declared exceptions, bridge/synthetic/default/varargs distinctions and covariant method selection are retained. Returned metadata arrays are defensive; enumeration order is not an application contract. Discovery does not actively initialize represented classes.

Methods, constructors and fields execute through fixed AOT bridges in their original declaring classes. Invocation preserves virtual dispatch, primitive unboxing/widening, reference checks, explicit varargs arrays and target Throwable identity through InvocationTargetException. Static method/field access and construction trigger initialization before argument conversion; initialization failures escape outside target wrapping. Class.newInstance unwraps constructor exceptions. Abstract/enum construction, incompatible arguments and absent members produce typed failures.

Lexical caller identity is retained for direct calls and method-handle/reference entry points. Access checks preserve packages, nests and cross-package protected receiver restrictions in one unnamed-module domain. AccessibleObject overrides are supported. All final-field writes are explicitly rejected, including overrides: the selected target does not implement reflective mutation of final instance fields. Named-module/security-manager policies and multiple class loaders are excluded.

Reflective Array supports primitive/reference allocation, multidimensional allocation, length, boxed and typed reads/writes with widening and incompatible-value rejection. Generated factories cover linked reference components and their one-dimensional array components, and primitive array components through rank two. Higher unregistered reference-array components fail explicitly with UnsupportedOperationException; arbitrary dynamic rank generation is excluded. JVM's 255-dimension and negative-size checks remain enforced.

## Signatures, parameters and annotations

Class, Method, Constructor and Field expose generic signatures through Type, TypeVariable, ParameterizedType, GenericArrayType and WildcardType. Declaration-scoped variables, recursive bounds, nested owners, wildcard/array types and generic exceptions are represented lazily. Missing types and malformed signature grammar produce TypeNotPresentException and GenericSignatureFormatError. Platform implementation signatures are not fabricated.

Executable and Parameter expose MethodParameters names/flags, erased/generic types and parameter annotations, including implicit constructor parameter offsets. Annotated types retain class-file type paths for arrays, nested owners, type arguments, wildcard and variable bounds, receivers and exceptions.

Runtime-visible declaration, parameter and type annotations become typed AOT values with defaults, nested annotations, enums, class values and defensive arrays. Equality/hash behavior, superclass-only @Inherited lookup and repeatable containers are supported. Runtime-invisible annotations are excluded. Unlinked annotation declarations are ignored as on the JVM; missing class-valued members, missing enum constants, missing required values, incompatible member values and duplicate annotations fail lazily with typed annotation errors. Annotation text is diagnostic rather than an exact host-JDK formatting contract.

## Boundaries and evidence

Metadata covers linked application/library declarations and bundled annotation types. Other bundled platform or compiler-generated class discovery fails explicitly instead of silently returning incomplete declarations. Object has standard method discovery and callable getClass/hashCode/equals/toString plus construction; its other reflected method targets remain explicitly unsupported. Missing enclosing/member types fail on resolution rather than masquerading as absent declarations. Record components, sealed-class reflection, class/resource/service loading, full collections/JDK APIs, managed memory and concurrency remain separate roadmap work.

BytecodeReflectionTest compares JVM/Wasm discovery, access, invocation/conversion/failure priority, defensive values, generic and annotated signatures, hidden/named parameters, arrays, malformed metadata and initialization. Supported fixtures run in both JAR orders and two independent Wasm instances. Real-artifact evidence includes:

- Unchanged SHA-verified Spring Boot Autoconfigure 3.2.4 RabbitConnectionDetails$Address method metadata.
- Unchanged pinned Jakarta Persistence 3.1.0 Entity/Table/Column/UniqueConstraint/Index annotations.
- Selected invocation-method bodies from pinned Spring Core 6.1.5 ReflectionUtils, preserving original instructions/handlers/source lines and comparing with the full original Spring class on the JVM. Unrelated declarations and class initialization are pruned from the Wasm fixture. This proves the selected caller closure, not whole-class execution or Spring startup.

The pinned dependency inventory remains a whole-closure audit, not proof that frameworks can start. Integration with the remaining resource, JDK, memory and host contracts must be established by the later startup milestones.

Validation: all 240 bytecode tests pass; the reviewed 63-JAR audit baseline passes with inventory counts unchanged and compiler revision `reflection-metadata-v45`.
