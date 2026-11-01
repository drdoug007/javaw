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
- Lowering still processes every linked method (addressed by v256 below).
- Interface/virtual dispatch helpers call every linked implementation, so a runtime class remains whenever a reachable call could dispatch to it, even if it is never instantiated (addressed by v256 below).

Evidence: `BytecodeDeadCodeTest` covers:

- a non-ASCII package/class entry point compared with the JVM in both JAR orders
- removal of an unreachable private method and runtime class
- retention of reachable runtime code and `Throwable` exports
- the retained-function API
- admission of unused invalid calls

`BytecodeDynamicTest` now names the serialization hooks it exports after compilation. All 594 prescribed bytecode tests pass (four forks). The audit changes only the revision.

## Instantiation-aware reachability (`instantiation-reachability-v256`)

Exception-runtime builds now run a rapid type analysis before lowering (`BytecodeReachability`). Builds without the runtime are unchanged.

Admission comes first and is unchanged: for each class in order, `ClassBytecodeCompiler.admit` runs `BytecodeCompatibility.validate`, checks every export name and runs the operand-stack frame analysis for every method. These are the checks lowering used to perform, in the same order, so unused invalid methods are still diagnosed.

The analysis is a worklist over WAT function ids:

- Roots are the same as for pruning: exports matching the prune predicate (evaluated on every linked method's would-be export name), exported and non-function fields of the generated runtime text, and retained function ids.
- A reachable method is lowered, and its edges are read from the lowered text (`call`, `return_call`, `ref.func`). Generated runtime functions and `<initialize>` functions contribute the edges in their text. Direct edges are therefore exact.
- Virtual and interface dispatch is lowered only for receiver classes instantiated so far (`BytecodeDispatch.restrictReceivers`). If a class is instantiated later and a lowered call site could dispatch to it, the method is lowered again.
- A class is instantiated by a reachable `NEW`, or by a reachable runtime helper that tags fresh memory itself: `$__text*` (String), `$__mirror*` (Class), `$__trace_capture` (StackTraceElement) and each implicit `$__throw_*` helper (its exception class, including the OutOfMemoryError/StackOverflowError data-segment objects). `Object.clone` only copies an existing header.

After the analysis:

- `$__trace_capture` names only trace sites whose method was lowered, because only a lowered method can push its trace id. Trace ids are unchanged.
- `$__exception_class_name` covers only instantiated classes.
- Dead-code pruning still runs. It now also removes unreferenced named globals (`global.get`/`global.set`); exported globals are kept.

Host contract: references passed into a module must have been created by that module. A host-built object of a class the module never instantiates would fall through narrowed dispatch to IncompatibleClassChangeError.

Results (same machine, warm compiles, baseline `c0cc2ec`):

| Fixture | Compile | WAT | `.wasm` | Functions |
| --- | --- | --- | --- | --- |
| Trivial (`return 41+1`) | 2.4 s → 1.7 s | 8.41 MB → 1.10 MB | 653 KB → 115 KB | 1,529 → 118 |
| Jackson `NumberOutput` (`toString(double,boolean)`, `toString(int)`) | 2.9 s → 2.1 s | 47.4 MB → 11.7 MB | 3.79 MB → 1.13 MB | |

For the trivial fixture, 53 of 4,340 linked methods are lowered.

Limits:

- Admission still runs frame analysis over every linked method. That, plus whole-program validation, is now most of the remaining compile time.
- Reflection, lookup and annotation tables compare names across every user class. Their arms keep the classes they construct reachable, so a reachable table costs most of the precision.
- Type-test chains (`CHECKCAST`, `INSTANCEOF`, catch handlers, `$__reference_array_store`) and class/array ids still cover every linked class.

Evidence: `BytecodeReachabilityTest` compares with the JVM:

- user and bundled-runtime interface receivers, including a receiver instantiated after its call site was first lowered (`Collection.size` on a later `LinkedList`)
- dispatch on a String that exists only as a text constant
- an implicit NullPointerException's class name and trace method name

It also checks that an uninstantiated runtime receiver (`TreeSet.size`) and its static state are absent. It fails on `c0cc2ec` at that assertion. An unused method with an invalid operand stack is still rejected with its source line.

The 594 prescribed bytecode tests pass (four forks). The audit changes only the revision.
