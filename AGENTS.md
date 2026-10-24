# Repository guidance

## Scope and progress

- Follow `Todo.md` in order. Large open milestones contain completed subsets; implement a concrete next subset and record its evidence without marking the parent complete prematurely.
- Update `Readme.md` and `Todo.md` with completed behavior, validation, and remaining limitations. Keep detailed runtime contracts in `docs/`.
- Commit each completed task. Preserve unrelated working-tree changes, including IDE settings.

## Compiler architecture

- `javaw-core` is the source compiler and runtime; `javaw-bytecode` handles class/JAR input. Source facades do not establish third-party bytecode compatibility.
- Bytecode declaration/hierarchy admission must diagnose invalid unused linked declarations before WAT generation. Keep admission and lowering resolution consistent.
- Runtime/admission semantics changes require a compiler revision bump in `javaw-bytecode/src/main/java/com/example/javaw/bytecode/cache/CacheMetadata.java`.
- Actual H2/Spring/Hibernate execution is an open target. Keep claims separate from the existing source-backed API subset.

## Validation

- For bytecode changes run `mvn -o -pl javaw-bytecode -am -Dtest='com.example.javaw.bytecode.*Test' -Dsurefire.failIfNoSpecifiedTests=false test`.
- Use malformed ASM fixtures for admission checks and executable javac-to-Wasm comparisons for supported behavior. Runtime tests require `wat2wasm` and Node.
- Run `python3 runtime-target/audit.py --check-baseline` when changing admission or the compiler revision. Review generated reports before refreshing `runtime-target/audit-baseline.json`; a revision-only change should leave inventory counts unchanged.
