# Repository guidance

## Project goals

1. Build a Java-to-WebAssembly compiler that executes Java programs correctly in the browser and other supported Wasm hosts. Prioritize observable Java semantics, clear unsupported-feature diagnostics, and reproducible JVM-versus-Wasm tests. Expand compatibility in tested stages rather than implying complete JVM/JDK support.
2. Use `javaw-bytecode` to convert library dependency class files and JARs into WebAssembly. Link those dependencies with compiled application code using explicit runtime and host contracts. Progress from small real-library execution tests toward the pinned H2/Spring/Hibernate target; track required JDK APIs, initialization, exceptions, reflection, resources, memory management, and concurrency. Establish and test source/bytecode interoperability before claiming mixed applications work.
3. If the compiler and library runtime milestones succeed, investigate running Java Swing applications in the browser using WebAssembly. Treat this as a feasibility study before committing to implementation. Audit Swing/AWT and Java2D dependencies, native peers, fonts/text layout, image rendering, input/focus, accessibility, clipboard, the event dispatch thread, and browser threading restrictions. Compare a browser-backed AWT/Java2D implementation with a rendered-surface approach, document compatibility and performance tradeoffs, and prove a small window/widget/event-loop demo before expanding scope. Keep this exploratory goal subordinate to compiler correctness and library execution.

## Scope and progress

- Follow `Todo.md` in order. Large open milestones contain completed subsets; implement a concrete next subset and record its evidence without marking the parent complete prematurely.
- Update `Readme.md` and `Todo.md` with completed behavior, validation, and remaining limitations. Keep detailed runtime contracts in `docs/`.
- Commit each completed task. Preserve unrelated working-tree changes, including IDE settings.

## Local agent kit

- `javaw-agent-kit/` holds tasks, guard rails and pre-written tests for a local offline model. An agent working a kit task follows `javaw-agent-kit/AGENT.md`, which narrows these instructions to that task. Review its `agent-kit/work` commits with `javaw-agent-kit/REVIEW.md` before merging.

## Compiler architecture

- `javaw-core` is the source compiler and runtime; `javaw-bytecode` handles class/JAR input. Source facades do not establish third-party bytecode compatibility.
- Bytecode declaration/hierarchy admission must diagnose invalid unused linked declarations before WAT generation. Keep admission and lowering resolution consistent.
- Runtime/admission semantics changes require a compiler revision bump in `javaw-bytecode/src/main/java/one/dastec/javaw/bytecode/cache/CacheMetadata.java`.
- Actual H2/Spring/Hibernate execution is an open target. Keep claims separate from the existing source-backed API subset.

## Validation

- Tests and builds write generated sources and outputs (`.wat`, `.wasm`, source maps, copied runtime scripts) under `target/` directories, never into the source tree. Do not commit generated modules.
- For bytecode changes run `mvn -o -pl javaw-bytecode,javaw-agent-kit -am -Dtest='one.dastec.javaw.bytecode.*Test' -Dsurefire.failIfNoSpecifiedTests=false test` on JDK 21 (the suite fails on JDK 24+). `javaw-agent-kit` holds finished agent-kit task tests; pending ones are tagged `agent-pending` and excluded.
- Use malformed ASM fixtures for admission checks and executable javac-to-Wasm comparisons for supported behavior. Runtime tests require `wat2wasm` and Node.
- Run `python3 runtime-target/audit.py --check-baseline` when changing admission or the compiler revision. Review generated reports before refreshing `runtime-target/audit-baseline.json`; a revision-only change should leave inventory counts unchanged.
