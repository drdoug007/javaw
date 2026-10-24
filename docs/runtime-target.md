# Actual-library runtime target

This target pins the real-library workload behind Todo section 12. It does not claim that these libraries execute in Wasm today. `examples/springboot-h2` remains the independently supported source-backed API subset. The new `runtime-target` module has no dependency on compiler facades and runs real libraries on the JVM as a reference for future Wasm comparisons.

| Component | Pinned target |
| --- | --- |
| Application bytecode | Java 17 / class-file major 61; library multi-release entries selected for Java 17 |
| Spring Boot / Spring Data JPA | 3.2.4 |
| Spring Framework | 6.1.5 |
| Hibernate ORM | 6.4.4.Final |
| Jakarta Persistence / Transactions | 3.1.0 / 2.0.1 |
| H2 / JDBC driver | 2.2.224, embedded `org.h2.Driver` |
| Embedded servlet stack | Tomcat 10.1.19 (later web milestone) |
| Wasm reference host | Node 24.19.0; module-local linear-memory objects, bulk memory, saturating numeric conversions and Wasm exception tags for bytecode throws/implicit failures; optional isolated Throwable runtime with reserved-prefix bounded stack traces |
| Runtime artifacts | 63 exact third-party JARs, with SHA-256 identities in `runtime-target/dependencies.lock.json` |

`runtime-target/pom.xml` manages every artifact in that runtime closure explicitly. This deliberately preserves the resolved versions from the existing demo, including mediation results, rather than depending on a newly selected BOM. Test-only JUnit and Maven/compiler tooling are outside the library runtime lock. The lock verifies artifact bytes, not JDK/toolchain binaries. The reference tests currently run on the installed Java 25.0.3 JVM while compiling the fixture for Java 17; execution on a Java 17 JVM remains an additional toolchain validation task. Unused versioned entries above Java 17 must not become audit requirements.

Run from the repository root:

```sh
python3 runtime-target/verify-lock.py
mvn -o -pl runtime-target test
```

The verifier resolves the effective runtime closure, rejects missing/extra/version-drifted artifacts and hash mismatches, and writes portable-coordinate/local-path input for the dependency auditor to `runtime-target/target/audit-manifest.tsv`. It defaults to offline Maven; `--online` allows downloading missing artifacts. It never silently updates the lock. Dependency/version changes require reviewing the POM, lock and reference results together.

The executable JVM reference currently covers:

- Actual H2 JDBC DDL, generated keys, Unicode, exact decimal binding/results, savepoints, rollback, unique-constraint failure and file reopen/durability after clean close.
- Actual Spring Boot startup without a web server, Hibernate schema creation, Spring Data repository proxies and derived queries, entity identity, dirty checking, optimistic version increments, committed/rolled-back transactions and context shutdown.

The first Wasm engine milestone is embedded in-memory H2 accessed through actual JDBC. The first framework milestone is the same non-web Boot/JPA application lifecycle and transactional entity/repository workload exercised by the reference test. File-backed H2 and servlet/HTTP execution are separate required later milestones, not implied by in-memory success. File mode requires channels, locking, durability/recovery and failure tests; web mode requires the actual Spring MVC stack with a documented host networking boundary. Concurrent requests, Hikari/background workers, monitors, thread locals and initialization require real thread/Java Memory Model support or a reviewed equivalent that preserves observable behavior; disabling them does not establish full-stack compatibility.

The broader completion gate also requires relationship/lazy-loading and enhancement/proxy paths, supported JPQL/derived/declared queries, paging/sorting, transaction propagation/isolation, framework configuration/resources/service discovery, exceptions, instance isolation, repeated workloads and clean shutdown. Framework features outside an implemented milestone must fail explicitly. H2 server/TCP/cluster modes, arbitrary JVM agents/class loaders and arbitrary additional dependency versions are outside this pinned target; they need their own audited execution gates. Completing this target must not be described as universal compatibility with every Spring/Hibernate feature.

The remaining Todo prerequisites remain unchecked until their implementation has Wasm execution evidence. Successful JVM reference tests, dependency inventories, and first-rejection reports are planning/verification infrastructure, not proof of Wasm framework support.

Declaration validation also checks duplicate member identities, conflicting visibility, selected incompatible class/member flags, interface field/method/version rules, initializer signatures and complete descriptor syntax before lowering. Static storage symbols encode owner, name and descriptor without sanitization collisions. This is partial structural validation; sealed constraints, remaining access/name rules and descriptor/linkage resolution remain prerequisites.
