# Closed-world handles, proxies and generated classes

Revision `aot-handles-proxies-v43` defines the dynamic-runtime milestone for the explicit bundled, single-threaded bytecode runtime (`--exception-runtime`). Java class definition, instrumentation and arbitrary runtime bytecode generation are excluded from this target. Applications must select interface shapes and fixed targets at build time. This contract supplements [MethodType signatures and byte-array VarHandles](compatibility.md#bytecode-methodtype-signatures).

## Handles and lookup

Standalone Java 7+ method-handle constants support static, virtual, interface, special and constructor calls and instance/static field reads/writes. Access checks use the original lexical caller, including package, nest and protected receiver rules. Final-field setters are rejected. Constants are lazy and cached; constructing a handle does not initialize its target. Invocation triggers ordinary target initialization, virtual dispatch and Java exceptions. Adapters preserve long/double slots, boxing, unboxing, primitive widening, reference casts, void return adaptation and null/type failure ordering.

Supported handle operations are `type`, `invokeExact`, adapting `invoke`, `invokeWithArguments(Object[])`, `bindTo`, `asType`, `isVarargsCollector` (false for admitted targets), `asFixedArity`, and `toString`. Null arguments to `invokeWithArguments` mean zero arguments. Exact invocation requires the canonical call-site MethodType to match; mismatches throw WrongMethodTypeException before calling the target. Target exceptions propagate unchanged.

`MethodHandles.lookup()` becomes a resolver with its original caller's rights. Supported lookup operations are `lookupClass`, `hasFullPrivilegeAccess`, `findStatic`, `findVirtual`, `findConstructor` and `findSpecial`. `privateLookupIn` supports linked application classes in the selected single unnamed-module/application-loader domain. Private/special calls retain nest and caller checks; inaccessible members throw IllegalAccessException and absent names/descriptors throw NoSuchMethodException. Lookup tables contain linked application declarations and inherited candidates, not arbitrary JDK members. Platform/runtime classes cannot acquire private lookup rights. This is not a JPMS or multiple-loader implementation.

Variable-arity direct handle constants and constructor constants for abstract classes receive explicit AOT-wrapper diagnostics. Matching lookup requests for those cases throw UnsupportedOperationException; they are not silently treated as fixed arity. Use a fixed-arity application wrapper. Arbitrary combinators, unreflect, field lookup, publicLookup, lookup-mode transformations, collectors/spreaders and List-based invocation are outside this API. Unsupported members receive compiler diagnostics; this milestone does not promise all `java.lang.invoke` behavior.

The preceding MethodType implementation and byte-array-view plain VarHandle accesses remain supported. Non-byte-array views and ordered/atomic/concurrent accesses remain excluded.

## Registered interface proxies

Register ordered public, unsealed interface shapes:

```java
String wat = BytecodeCompiler.compileJarWithExceptionRuntime(input,
    List.of(List.of("example.Service", "example.Marker")));
```

The equivalent CLI option is repeatable:

```sh
java com.example.javaw.bytecode.BytecodeCompiler --exception-runtime \
  --aot-proxy=example.Service,example.Marker application.jar application.wat
```

All interfaces and their parents must be linked. Generated proxy names are reserved under `javaw/aot/`. A shape has one class identity per module instance; constructing proxies creates distinct objects. Interface order selects the foremost declaring Method for duplicate declarations. Duplicate full descriptors intersect checked exception permissions. Differing return descriptors for the same name/parameters, including covariant returns, require an explicit adapter and are diagnosed during registration. Empty, duplicate, nonpublic and sealed registrations are rejected.

`Proxy.newProxyInstance`, `getProxyClass`, `isProxyClass` and `getInvocationHandler` use this registry. Unregistered factories are diagnosed at the original call site when no shapes were supplied; an unknown runtime shape throws IllegalArgumentException. Null interfaces/handlers retain NullPointerException behavior. Class-loader identity consists of bootstrap/null and one application loader. `Class.getClassLoader()` follows primitive/array component identity and classifies bundled `java.*` types as bootstrap classes; application classes share the canonical loader. Application interface shapes require that loader. No class loading, resources, user loader namespaces or package/module loader policies are supplied.

Each generated override calls InvocationHandler with the proxy, stable Method metadata and boxed arguments (null for zero parameters). Object equals/hashCode/toString also go through the handler with Object-declaring metadata. Primitive results cast/unbox strictly. RuntimeException/Error and permitted checked exceptions retain identity; other checked exceptions become UndeclaredThrowableException. Interface default methods go through the handler; automatic `InvocationHandler.invokeDefault` is not supplied.

The small `Method` runtime supports name, declaring/return/parameter classes, parameter count, modifiers, default/bridge/synthetic/varargs flags, equality/hash and `invoke` for generated metadata. Parameter arrays are defensive. Invocation validates receiver and arguments, delegates to the fixed handle, and wraps target failures in InvocationTargetException. It does not implement general reflective discovery, annotations, generic types, access suppression or serialization. Those remain in the subsequent reflection milestone.

## Pinned framework audit and executable AOT evidence

`runtime-target/target/audit/dynamic-runtime.tsv` records symbolic mechanism sites for all 63 locked Java-17-selected JARs. It is an inventory, not sound dynamic reachability. The selected alternatives are explicit build-time/application plans, not automatic rewrites of arbitrary framework configuration.

| Audited path | Selected execution or AOT boundary |
| --- | --- |
| Log4j ServiceLoaderUtil lookup/findStatic/bindTo/invoke | Fixed linked application targets support those protocols. JDK ServiceLoader and privileged execution remain separate JDK/runtime requirements. |
| Spring Data DefaultMethodInvokingMethodInterceptor lookup/privateLookupIn/findSpecial/bindTo/invokeWithArguments | Caller/private/nest/default-special protocols have JVM/Wasm tests. Full interceptor execution still requires reflection and framework linkage. |
| Spring Expression FunctionReference/ReflectionHelper | MethodType and fixed-arity invocation protocols are supported. Varargs targets require wrappers; SpEL's compiled-expression class definition is excluded. |
| Spring AOP JdkDynamicAopProxy newProxyInstance/getProxyClass | Register shapes and supply a fixed InvocationHandler plan. A real pinned Spring 6.1.5 ProxyFactory/MethodInterceptor JVM oracle is compared with an AOT handler in Wasm for target invocation, advice state and exception unwrapping. Arbitrary advisor discovery and Spring startup remain separate work. |
| Spring CGLIB ReflectUtils/AbstractClassGenerator/Enhancer/KeyFactory | Runtime factories/definitions are diagnosed. Replace the selected behavior with a registered interface handler or package an admitted subclass produced on the build JVM. CGLIB's callback/runtime support is not automatically linked. |
| Hibernate ClassLoaderServiceImpl.generateProxy | Registered shapes provide the JDK-proxy mechanism; full session/service handling still requires Hibernate's remaining runtime dependencies. |
| Hibernate ByteBuddyState and ByteBuddyProxyHelper | Build/runtime generation entry points are diagnosed. A pinned Hibernate 6.4.4.Final ByteBuddyProxyFactory JVM oracle checks an explicit AOT lazy-reference plan: identifier reads without loading, uninitialized access failure, supplied implementation, delegated reads/writes. The AOT plan uses application state and an explicit initialization boundary; it does not install Hibernate's runtime-generated proxy or its LazyInitializer. |
| Hibernate entity enhancement | The real Hibernate enhancer transforms an annotated JVM entity and verifies dirty attribute names, changed scalar writes, clearing and unchanged writes. An explicit one-int-attribute AOT state plan matches the resulting dirty-state trace in Wasm. General entity enhancement, mutable attributes, associations, sessions, flush, SQL loading and JPA callbacks remain in the Hibernate integration milestone. |
| Byte Buddy DynamicType builders/loaders/injectors | Generation/loading is excluded in Wasm. A hash-verified Byte Buddy 1.14.11 build-JVM generator produces a subclass with MethodCall interception and FieldAccessor state. Its unchanged class bytes execute on JVM and Wasm, including constructors/failures, virtual override behavior, wide fields and object identity. This proves that admitted generated artifacts can be linked, not that the Byte Buddy engine runs in Wasm. |

The framework oracle test verifies every JAR against `dependencies.lock.json` before launching the build JVM. Its tools, framework-generated proxy and enhanced entity stay outside the Wasm application JAR. This deliberately distinguishes real framework JVM behavior, tested application AOT equivalents, and the unchanged Byte Buddy-generated artifact that actually executes in Wasm. No complete Spring/Hibernate class, session factory, application context or full library closure is claimed by these equivalents.

## Diagnostics and validation

Runtime definitions through Lookup/Unsafe/ClassLoader (including linked inherited loader owners), Spring CGLIB generation/definition, Hibernate proxy generation entry points and Byte Buddy make/load/inject/define paths report the originating method/source location and recommend generating and packaging classes on the build JVM. Definitions hidden behind arbitrary reflection strings or unlinked custom loader implementations cannot be inferred; unsupported reflection/linkage remains a separate diagnostic boundary.

Executable regressions are `BytecodeMethodHandleTest`, `BytecodeProxyTest`, `BytecodeAotGeneratedTest`, plus the earlier MethodType/VarHandle tests. Supported tests compare with JVM execution, reversed JAR orders and independent Wasm instances. Boundary tests cover varargs, conflicting return descriptors, missing shape registrations and original-site generation errors. All 226 bytecode tests pass. The reviewed pinned audit changes only compiler revision; artifact, opcode, bootstrap and mechanism inventories remain unchanged.
