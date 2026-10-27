# Reflection in source-compiled WebAssembly

Reflection uses generated metadata and typed Wasm calls for classes linked into
the source module. It does not delegate Java execution to the host JVM or load
bytecode at runtime. The bytecode backend has a separate compatibility contract.

The executable contract is covered by
[ReflectionSupportTest](../javaw-core/src/test/java/one/dastec/javaw/ReflectionSupportTest.java).
The broad reflection roadmap item remains open for the limitations below.

## Available operations

For linked source classes, interfaces, enums, and records:

- Class getDeclaredFields/Methods/Constructors and single-member lookup expose
  source declarations. Public getFields/getMethods include inherited source
  members; constructors are never inherited. Lookup matches exact parameter
  Class descriptors, including primitive types.
- Field exposes name, declaring class, type, modifiers, get/set, and primitive
  accessors. Static fields ignore the receiver. Final writes always throw
  IllegalAccessException.
- Method exposes name, declaring class, modifiers, return type, parameter types,
  parameter count, isVarArgs, and invoke. Instance invocation preserves virtual
  dispatch; static invocation ignores the receiver.
- Constructor exposes declaring class, name, modifiers, parameter metadata, and
  newInstance. Abstract construction throws InstantiationException.
- Arguments unbox and widen primitives using Java invocation conversions.
  Narrowing, incompatible receivers/references, and invalid arity throw
  IllegalArgumentException; null instance receivers throw NullPointerException.
  Target exceptions are wrapped in InvocationTargetException; argument and
  access failures are raised directly.
- Parameter and annotation arrays are returned as fresh arrays.

Access is deliberately limited to a public-member check unless setAccessible(true)
or trySetAccessible() overrides it. Caller-package access, protected access,
nestmates, enclosing-class visibility, and Java module access rules are not
implemented. This is not a security boundary.

## Runtime annotations

Source annotation declarations with RetentionPolicy.RUNTIME become typed
annotation objects on classes, fields, methods, and constructors. Scalar,
class, enum, nested annotation, and array values and defaults are supported.
Array-valued getters return fresh arrays. Annotation equals/hashCode compare
member values; toString currently returns only the annotation name.

Class getAnnotation/getAnnotations honor @Inherited through superclasses,
not interfaces. getDeclaredAnnotations excludes inherited annotations.
Retention, Target, Inherited, Documented, Repeatable, and Deprecated have linked
metadata definitions. Repeated applications pack into their @Repeatable container.
Class and reflected members support getDeclaredAnnotation, getAnnotationsByType,
and getDeclaredAnnotationsByType. By-type queries expand containers in declaration
order and return fresh typed arrays; singular queries expose directly present
annotations/containers without expanding them. Class by-type queries inherit
only when no direct or indirect values exist, and only through superclasses
for @Inherited annotation types. An empty container permits this fallback.
CLASS-retained repeated annotations can still be visible through a
RUNTIME-retained container, matching Java retention behavior.

Container declarations must have a matching value() array, defaults for other
members, retention at least as long as the repeated type, and matching
@Inherited/@Documented requirements. Invalid repeated use and cyclic container
relationships receive source diagnostics. Full annotation applicability and
type-use rules remain part of the source conformance follow-up.

Type-use annotations, generic Type metadata, and annotation processing remain
outside this contract. Repeatable behavior is executed by
[RepeatableReflectionTest](../javaw-core/src/test/java/one/dastec/javaw/RepeatableReflectionTest.java).

## Class lookup and boundaries

One-argument Class.forName resolves already-linked simple names and qualified
package aliases; missing names throw ClassNotFoundException and null throws
NullPointerException. It does not import an otherwise unused JDK class.
ClassLoader/URLClassLoader and loader/initialization overloads are rejected.
There is no runtime class definition, dependency loading, or deferred
initialization switch.

Class names retain javaw's simple-name descriptor ABI. Array descriptor names
use source spelling (for example int[]), not JVM binary spelling. isArray,
isPrimitive, and isAnnotation are available. Full JVM Class semantics, including
array covariance/instance tests and implicit Object superclass metadata, remain
pending.

Array arguments and field writes check managed runtime component-type tags.
Primitive arrays require the exact primitive type (int[] does not widen to
long[]). Reference arrays support covariance, including source class/interface
inheritance and multidimensional arrays. Null arrays are accepted. Incompatible
arrays, strings, objects, and untyped host buffers throw IllegalArgumentException
before invoking the target. Array return values retain their runtime type tags.
Arrays.copyOf/copyOfRange preserve tags; source String.toCharArray/getBytes calls
attach char[]/byte[] tags. Enum values arrays are tagged when
their array descriptor is linked. Garbage collection and block reuse preserve
or reset tags with the allocation.

Reflective varargs are passed as a single array argument, not automatically
repacked. For example, invoke(null, (Object)new int[]{1,2}) passes an int[]
argument; invoke(null, 1,2) does not call a method taking int... correctly.

Enums expose source-declared fields and methods, public static final constants
(Field.isEnumConstant), generated values/valueOf methods, and private constructor
metadata with the synthetic String name/int ordinal parameters. Reflective
construction always fails with IllegalArgumentException after access checks,
even with setAccessible(true). Reflected valueOf wraps missing-name and null-name
errors in InvocationTargetException. Constant-specific class bodies are rejected
with a source diagnostic because their subclass dispatch is not implemented.

Records in reflection-enabled source modules lower to managed objects so they
can cross Object-based reflection boundaries. Modules without reflection keep
the existing multi-value record representation; these representations are not a
cross-module ABI. Canonical, compact, explicit, and varargs constructors,
immutable private backing fields, public accessors, source methods, and generated
equals/hashCode/toString methods participate in reflection. User overrides are
retained. Generic member types use erased Class descriptors. Casts of reflected constructor
results to wildcard-parameterized types remain subject to the compiler's pending
generic conversion work; use the raw record type at that boundary.

Class.isRecord and getRecordComponents expose ordered RecordComponent objects
with name, type, declaring record, accessor, and runtime declaration annotations.
Generated backing fields/accessors receive component annotations only when the
annotation's Target permits FIELD/METHOD; explicitly declared accessors retain
their own annotations. Parameter/type-use annotation APIs remain pending.

Member metadata for compiler-injected library facades, JAR classes, and other
synthesized members is not provided; those types may have class descriptors
without reflectable members. Inherited java.lang.Enum/Record library APIs and
the full Record superclass contract remain follow-ups. The usual source
compiler conversion and library limits still apply to record method bodies.

The host runtime's writeIntArray, writeDoubleArray, and writeStringArray attach
the corresponding int[], double[], and String[] tags. Managed allocations from
__javaw_array_bytes_alloc alone carry an untyped-array marker and are rejected
by typed reflection; host integrations should use the typed writers and the
runtime's string/object ABI. Handle arrays do not acquire Java object-array
types. Arbitrary raw pointers do not acquire Java runtime types.


## Executable parameters

Method and Constructor share an Executable facade exposing getParameters and
getParameterAnnotations. Annotation matrices include an entry for every
descriptor parameter, with fresh outer and inner arrays on each query. Only
RUNTIME declaration annotations with PARAMETER applicability are included;
SOURCE/CLASS-only and TYPE_USE-only annotations are excluded. Repeatable
annotations use the same container and by-type query rules as members.

Parameter exposes source name, erased Class type, modifiers, declaring
Executable, isNamePresent/isSynthetic/isImplicit/isVarArgs, and the standard
declaration annotation queries. Parameters from equivalent descriptor lookups
compare equal by executable identity and parameter index; result arrays are
independent. Source names are available without a javac -parameters flag.

Enum constructors have two leading synthetic parameters with no declaration
annotations, names arg0/arg1, and isNamePresent=false. Their user parameters
retain their source names and annotation positions. The generated enum valueOf
name parameter is marked implicit. Ordinary source and record canonical
parameters are not marked implicit or synthetic; final and varargs source
properties are retained. Generic and annotated Type APIs and a full JVM
MethodParameters/class-file metadata contract remain pending.

[ParameterReflectionTest](../javaw-core/src/test/java/one/dastec/javaw/ParameterReflectionTest.java)
executes this contract in WebAssembly, including inherited versus overridden
method declarations and record component annotation propagation.
