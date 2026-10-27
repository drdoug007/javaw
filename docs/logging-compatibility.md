# Optional logging compatibility facades

`javaw-log4j-compat` contains five project-owned Java classes under
`org.apache.logging.log4j` and its `simple` subpackage. They provide a small
Log4j-style API subset for the logging demo. They are not Apache Log4j sources,
a replacement for the complete API, or evidence of real Log4j JAR execution.

The classes and their tests were moved out of `javaw-core`. Neither the core
artifact nor its shaded CLI includes them. The compatibility module has no
production dependency on the compiler; its core dependency is test-only.
Bytecode/compiler modules do not depend on the compatibility module.

For source applications that deliberately use this subset, add:

```xml
<dependency>
    <groupId>dastec.one</groupId>
    <artifactId>javaw-log4j-compat</artifactId>
    <version>1.0-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

The examples module explicitly uses this compile-time dependency. The Maven
plugin excludes provided dependencies from bytecode linking. Other compiler
consumers need no logging compatibility dependency. Do not combine this module
with the real Log4j API on one classpath: their class names overlap. Applications
using real Apache Log4j should depend on its original artifacts; their Wasm
execution compatibility remains a separate target.

## Evidence and limitations

The existing `Log4j2Test` moved with the facades. Its JVM tests cover level
ordering/parsing, map-based diagnostic context, formatting/filtering, and
listeners. ThreadContext uses a shared map and does not establish thread-local
or concurrent behavior. SimpleLogger's history and listeners are shared mutable
state. These APIs include project extensions and differ from the real API.

The source compiler recognizes selected logging calls directly in
`WasmGeneratorVisitor`, `WasiHelper` and `LambdaSupport`. It does not compile
these five facade classes automatically. The existing Wasm test checks emitted
WAT structure, not end-to-end logging semantics. In the current lowering,
context operations and level setters can be no-ops, context queries return
zero/null-like values, enabled checks return true, level queries use INFO, and
listener/history operations do not implement the JVM facade behavior. Selected
log calls emit WASI output. Full MDC, filtering, listeners, configuration,
providers, appenders and actual Apache Log4j bytecode execution are not proven.
Moving the facades does not expand that lowering contract.

## Validation

```sh
mvn -o -pl javaw-log4j-compat -am -Dtest=Log4j2Test -Dsurefire.failIfNoSpecifiedTests=false clean package
```

Inspect both `javaw-core/target/javaw-core-1.0-SNAPSHOT.jar` and
`javaw-core/target/original-javaw-core-1.0-SNAPSHOT.jar`: neither should contain
`org/apache/logging/log4j/` entries. The compatibility JAR should contain the
five facade classes plus SimpleLogger's nested listener interface. Validation: the clean offline package build passes all four relocated logging tests with zero failures, errors or skips. JAR inspection confirms zero Log4j-package classes in both regular and shaded core artifacts and six class files in the compatibility artifact (five facades and the listener interface). The logging demo also compiles with javac against core plus the explicit compatibility JAR.
