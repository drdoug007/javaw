# Pinned dependency audit

Run `python3 runtime-target/audit.py --check-baseline` from the repository root. The command verifies all artifact identities, compiles the auditor, and scans the entire locked library closure. It defaults to offline Maven; `--online` permits missing dependency downloads. The machine-independent aggregate snapshot is `runtime-target/audit-baseline.json`. After reviewing a changed target/compiler inventory, `--update-baseline` records the new snapshot. Full reports are generated under `runtime-target/target/audit/` and are not committed.

The initial scan selected Java 17 multi-release entries from 63 JARs and found 26,625 unique classes, no duplicate class definitions, 734 resources and 28 service providers. The initial opcode/feature inventory includes 17,383 invokedynamic sites, 23,483 ATHROW instructions, 22,506 exception-table entries, 5,581 class initializers, 464 synchronized methods and 90 native declarations. Counts cover all selected library classes, including optional paths; they do not establish which facilities the first application workload must execute.

| Report | Meaning |
| --- | --- |
| `summary.tsv` / `summary.json` | Compiler revision, target Java level, roots, counts and analysis limits; JSON adds aggregated opcode/feature/bootstrap counts |
| `artifacts.tsv`, `classes.tsv` | Entire locked closure and static class-reference estimate |
| `normalized-opcodes.tsv` | Raw legacy subroutine occurrences handled by preprocessing; the audit verifies their classes can be normalized |
| `unsupported-opcodes.tsv` | All currently unadmitted instruction occurrences, grouped by artifact/opcode with first method evidence |
| `member-references.tsv` | Field/method/handle references with descriptors, counts and first referencing method; negative opcode values identify bootstrap method-handle tags |
| `requirements.tsv` | Initialization, handlers, strings, bootstrap sites, native/synchronized/volatile declarations and platform/reflection/I/O/concurrency lookups |
| `external-types.tsv` | Referenced types outside the locked JARs; includes JDK classes and optional integrations, not just missing dependencies |
| `resources.tsv`, `services.tsv` | Selected bundled resources and declared service providers |
| `duplicate-classes.tsv` | Conflicting class definitions; duplicates make a single-origin reachability estimate ambiguous |

The roots are `org/h2/Driver`, `org/hibernate/cfg/Configuration`, and `org/springframework/boot/SpringApplication`. A class-reference closure starts at those types and follows superclass/interface, descriptor, signature, annotation/record/nest metadata, instruction, handle and bootstrap references across all methods of each class. It initially includes 11,441 bundled classes. This is an estimate, not method-level or sound dynamic reachability: reflection strings, resources, framework conditionals, service activation, generated classes and runtime traces require separate analysis. Unreached classes are not proven removable. The JDK implementation itself is not part of the 63-JAR scan. Opcode admission alone does not establish supported semantics or member linkage.

Concrete blocker ordering from this scan:

1. Continue declaration/reference/uninitialized-object verification. Byte Buddy 1.14.11 contains legacy JSR/RET subroutines (162/77 occurrences), including `TypeCache$WithInlineExpunction.find`; normalization before ordinary frame analysis is now implemented and verified across all 61 affected methods. Other Byte Buddy requirements remain open.
2. Arrays, catchable exceptions and automatic initialization are immediate H2 requirements. H2 alone has 3,106 exception-table entries and 305 static initializers, in addition to JDBC/JDK linkage.
3. Build runtime strings/classes and required JDK members before bootstrap/reflection integration. The scan found StringConcatFactory (8,917 sites), LambdaMetafactory.metafactory (8,269), altMetafactory (46), and record ObjectMethods.bootstrap (151). Constant-dynamic scanning is tested, but the pinned selected closure initially contains no constant-dynamic sites.
4. Reflection, annotation/service/resource lookup, method handles, Byte Buddy/proxy/enhancement behavior and framework conditionals require actual startup traces and targeted tests. No report treats static analysis as a substitute for those execution gates.
5. Concurrency, memory roots and platform adapters must be implemented before claiming actual full-stack startup. H2 alone uses synchronized methods, volatile fields, concurrency APIs and I/O/NIO; background pools are not solved by the source-subset transaction implementation.
6. Optional Tomcat native/APR/OpenSSL paths account for most native declarations in this closure. They must be excluded explicitly for the initial non-web target or implemented for a selected later host mode. JDK-internal references include Spring Objenesis/Unsafe allocation and H2 extended socket options. Presence in a JAR does not make every such path mandatory for embedded JDBC.

The next runtime features must use actual-library JVM and Wasm regressions. `mvn -o -pl runtime-target test` currently passes on the JVM; it is not a Wasm integration gate yet. Synthetic auditor tests verify multi-release selection, unused-class inventory, metadata/bootstrap references, service parsing, duplicates, missing roots and deterministic output. Compiler bytecode tests continue to run independently.
