# Memory management for the linear-memory backend

Phase 3 uses a non-moving conservative collector, invoked explicitly by the host at a safepoint. The Spring bridge invokes it after each request has completed its Wasm calls and copied its response into JavaScript text. Other applications opt in through `runtime.collectGarbage()` or `__gc_collect`.

This is a collector for the existing linear-memory ABI. It does not implement JVM garbage-collection semantics, finalizers, weak references, concurrent collection, or Java Memory Model guarantees.

## Alternatives evaluated

| Approach | Benefits | Costs and constraints | Decision |
| --- | --- | --- | --- |
| Request arena / checkpoint reset | Cheap, predictable cleanup of request-local data | Persistent rows, closures, caches, and updated entities can reference newly allocated data. Resetting the heap would invalidate those references unless escape analysis, promotion, or graph copying were added. | Insufficient for the current repository and mutable object graph. |
| Explicit free / reference counts | Straightforward for host buffers and acyclic owned objects | Java code does not identify every last use; cycles and shared collections require additional tracing. | Count host roots explicitly; trace the Wasm graph. |
| Precise tracing during execution | Reclaims promptly with fewer false positives | Requires reference maps for every stack/local safepoint, array/object layouts, live operand-stack values, reentrant imports, and suspended callbacks. Those maps do not currently exist. | Future collector improvement. Never trigger collection from an allocation today. |
| Conservative tracing between calls | Preserves raw pointers and existing layouts; traverses cycles and unknown library graphs without stack maps | Can retain blocks when numeric words resemble pointers. Requires a stack-free boundary and explicit roots for host-held pointers. | Implemented for the default linear memory. |
| WasmGC structs/arrays | Native host-managed reachability and typed references | The roadmap proposes a separate `WasmGcVisitor`, but no implemented backend was found. This would change `i32` object references, vtables, strings, arrays, casts, exported class wrappers, raw Memory APIs, and Spring field-offset marshalling. | Keep as a separate backend proposal. It cannot replace this allocator without changing the ABI. |

## Allocation

Each allocation has a 16-byte internal header followed by an eight-byte-aligned payload. The header stores capacity, allocation/mark/scan/object flags, and a counted host retain. Source objects keep their existing payload layouts and vtable locations. Strings, arrays, builder buffers, exception objects, and class metadata use the same managed allocator. No separate linked object-identity registry is allocated.

Allocation first searches free blocks, splits sufficiently large blocks, clears reused payloads, and grows memory if needed. Collection clears dead blocks and coalesces adjacent free blocks. Overflow and exhausted growth trap without attempting to collect an active stack. This is not yet a Java `OutOfMemoryError` or `NegativeArraySizeException` implementation.

Collection is iterative, without recursive tracing or allocation of a mark stack. It reaches a fixed point over marked blocks, then sweeps unreachable blocks, including cycles. The allocator's high-water address does not decrease, and WebAssembly pages cannot shrink. Reuse bounds growth in stable workloads; live datasets and larger peak requests still require more memory.

## Root set

The compiler gathers the final i32 global symbols emitted by its source, enum, class, and runtime components. Their pointer-like values are roots, excluding allocator bookkeeping. This conservatively includes primitive i32 globals as well as source reference globals. Class descriptors, enum singletons, boxed-value caches, repository/controller singletons, and static closures are therefore retained.

Every retained host pointer is another root. Each reachable allocated payload is scanned at four-byte offsets for values inside allocated payloads, including interior pointers. This reaches reference arrays, fields, closures, collection backing storage, decimal digit arrays, and StringBuilder buffers. Static strings lie outside the allocation region and are permanent.

There are no stack/local roots: the host must not collect while any Wasm frame is active. A JavaScript import must not collect reentrantly. Pending asynchronous work and generated JS wrappers are not automatically scanned. Multiple workers must not allocate or collect concurrently; parallel scheduling remains Phase 2 work. Direct accesses to secondary memories and manually chosen raw addresses remain caller-owned.

## Host ownership

- A returned pointer is borrowed until the next collection. Copy data into JS or retain the pointer before collecting.
- `runtime.retain(ptr)` / `__gc_retain(ptr)` increments its allocation's root count and returns the pointer. Each call requires a matching `release` / `__gc_release` when that ownership ends.
- Hosts must retain object pointers in JS wrappers (`wrapper.__ptr`), custom maps, or asynchronous callbacks if they survive a collection. Retaining an object traces its reachable fields; each child need not be retained separately.
- Interior pointers retain their enclosing allocation. Static string-pool pointers need no retain. JS handles, table indexes, and ordinary JS objects do not establish Wasm roots.
- A released pointer must not be used after collection. Addresses may be reused, so a stale pointer is not an ownership token. Double release of a live unretained block traps.
- All temporary request arguments and responses must finish their Wasm use before collecting. The Spring handler reads/serializes exceptions before its collection boundary, including partially encoded requests that fail validation.

The Spring handler awaits request bodies before allocating arguments. Once argument construction begins, controller invocation, response copying, and collection are synchronous. Another request awaiting its body holds only JS data; persistent controllers and repositories are static roots. The bridge does not keep transient Wasm pointers between these synchronous sections.

## Evidence and limits

`ManagedHeapTest` verifies static roots, cycles, captured arrays, counted and interior host roots, cleared reuse, exception cleanup, and builder-buffer growth. After warm-up, 500 allocation/collection cycles keep live bytes, heap high-water bytes, and memory pages unchanged.

`examples/springboot-h2/test.js` exercises exact decimal HTTP round-trips and then runs the actual request handler through 160 warm-up and 300 measured CRUD/error cycles. Warming past the small Integer cache avoids mistaking intentional cache population for a leak. The measured live bytes, high-water bytes, and pages remain unchanged; seeded rows remain readable.

These are executable bounds for those workloads, not a proof that every application has bounded memory. Conservative false roots, unbalanced host retains, static caches, growing live graphs, fragmentation, and a single oversized call can still increase memory use. Precise stack maps, automatic safepoints, compaction, and concurrency require further runtime work.
