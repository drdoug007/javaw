# Reflection in source-compiled WebAssembly

Reflection uses generated metadata and typed Wasm calls for classes linked into
the source module. It does not delegate Java execution to the host JVM or load
bytecode at runtime. The bytecode backend has a separate compatibility contract.

The executable contract is covered by
[ReflectionSupportTest](../javaw-core/src/test/java/com/example/javaw/ReflectionSupportTest.java).
The broad reflection roadmap item remains open for the limitations below.

## Available operations

For ordinary linked source classes and interfaces:

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
metadata definitions. Repeated applications require an explicit container:
automatic @Repeatable expansion and getAnnotationsByType are pending.
Parameter/type-use annotations, generic Type metadata, and annotation processing
are outside this contract.

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

Non-null array arguments and field writes currently throw
UnsupportedOperationException because arrays do not yet carry enough runtime
component-type information for safe reflection conversion. Null arrays,
array type metadata, and array return values are supported. Reflective varargs
are passed as a single array argument, not automatically repacked; the same
non-null array restriction applies.

Enum and record member lookup/enumeration explicitly throw
UnsupportedOperationException. Member metadata for compiler-injected library
facades, JAR classes, and synthesized members is not provided; those types may
have class descriptors without reflectable members. Runtime annotation inspection
on linked enum/record declarations is available.

The host runtime tags its managed arrays so they cannot be mistaken for strings
during reflective argument checks. Host integrations must use the runtime's
array writers (or __javaw_array_bytes_alloc), and its string/object ABI; arbitrary
raw pointers do not acquire Java runtime types.
