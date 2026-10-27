# Bytecode compile cost and output size

## Measurements (before this work, `aa52ecf`)

The figures are for a trivial fixture (`return 41+1`) compiled with the bundled exception runtime. Every compile lowered and emitted the whole runtime: about 12 s, 153 MB of WAT, 12,036 functions and an 11.8 MB `.wasm`. Lowering methods (`compileMethod`) took only about 2.5% of that compile time. Most of the rest was whole-program analysis repeated for each of the ~300 runtime classes:

- `BytecodeDispatch` construction and array-type collection (~24%)
- object layouts (~12%)
- whole-program validation repeated in every class's `BytecodeCompatibility.validate` (~22%)

Only 1,255 functions (3.7% of the WAT text) are reachable from the fixture's exports.

## Per-compilation analysis reuse

`BytecodeAnalysisCache` memoizes whole-program analyses for one compilation. It is opened in `BytecodeCompiler` after normalization and keyed by the identity of the linked map. It caches:

- object layouts, including the `sizes`/`offsets` maps
- the caller-independent `BytecodeDispatch` core: the effective linked map, class and array IDs, and shared subtype answers
- trace-site IDs
- three whole-program checks in `BytecodeCompatibility.validate` (declarations, overrides, descriptor linkage), each memoized at its original position so the first diagnostic and its order are unchanged

This relies on one invariant: lowering and validation never mutate linked classes after normalization; all mutation happens in the normalizers. Callers outside a compilation (no active cache) compute every analysis as before.

Result: the trivial fixture compiles in 2.2 s instead of 11.8 s, and its WAT is byte-identical to `aa52ecf`. All 592 prescribed bytecode tests pass (four forks). Output is unchanged, so the compiler revision is unchanged.

## Dead-function pruning (`dead-function-pruning-v255`)

After WAT generation, exception-runtime builds drop module functions that nothing can reach (`BytecodeDeadCode`). Admission has already validated every linked declaration, including unused invalid ones, so pruning never hides a diagnostic.

Roots:

- exports of the input classes (compared in their escaped WAT form, so non-ASCII names work)
- `__`-prefixed host hooks
- `java.lang.Throwable.*` exports, which hosts use to inspect uncaught exceptions
- the start function and any function referenced from a non-function module field
- function IDs passed through `BytecodeCompiler.compileJarWithExceptionRuntime(InputStream, Set<String>)`, for hosts that add exports to the returned text

The pass follows `call`, `return_call` and `ref.func`. It returns the module unchanged if it finds tables, `call_indirect` or block comments. Exports of removed functions are dropped; all other text is preserved byte-for-byte.

Module API change: bundled-runtime methods are no longer exported unless they are reachable. Builds without the exception runtime are not pruned.

Results: the trivial fixture goes from 153.9 MB of WAT and an 11.8 MB `.wasm` to 8.4 MB and 654 KB. A Jackson `NumberOutput` fixture goes from 12.1 MB to 1.0 MB and still returns the JVM result. `wat2wasm` time drops from about 1 s to 0.07 s.

Limits:

- Pruning costs about 0.9 s per compile.
- Lowering still processes every linked method.
- Interface/virtual dispatch helpers call every linked implementation, so a runtime class remains whenever a reachable call could dispatch to it, even if it is never instantiated. An instantiation-aware analysis is the next Phase 7 step.

Evidence: `BytecodeDeadCodeTest` covers:

- a non-ASCII package/class entry point compared with the JVM in both JAR orders
- removal of an unreachable private method and runtime class
- retention of reachable runtime code and `Throwable` exports
- the retained-function API
- admission of unused invalid calls

`BytecodeDynamicTest` now names the serialization hooks it exports after compilation. All 594 prescribed bytecode tests pass (four forks). The audit changes only the revision.
