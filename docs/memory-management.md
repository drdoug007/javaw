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
| WasmGC structs/arrays | Native host-managed reachability and typed references | A separate `WasmGcVisitor` exists as a standalone prototype ([WasmGC](wasmgc.md)); it is not a general Java target. This would change `i32` object references, vtables, strings, arrays, casts, exported class wrappers, raw Memory APIs, and Spring field-offset marshalling. | Keep as a separate backend proposal. It cannot replace this allocator without changing the ABI. |

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


A separate [experimental WasmGC prototype](wasmgc.md) now emits native structs for a restricted source subset. It does not replace the default allocator or change the ownership contract above.

## Bytecode modules

Bytecode modules (`javaw-bytecode`) have their own memory ABI. They import `env.memory` with 2 initial pages and no maximum, so the host decides how far memory may grow. `BytecodeCompiler.Memory` configures the initial size and an optional maximum (CLI: `--memory-pages=initial[:maximum]`). Before v257 every module declared a 20-page (1.25 MB) maximum.

Since v258, modules built with the bundled exception runtime own their heap by default (`BytecodeCompiler.Memory.managed()`). They do not import `__heap_alloc`, and they export the same host API as the source backend: `__gc_collect`, `__gc_retain`, `__gc_release`, `__gc_live_bytes` and `__gc_heap_bytes`.

`BytecodeCompiler.Memory.hostAllocator()` (CLI: `--host-allocator`) keeps the earlier ABI: the module imports `env.__heap_alloc` and the host allocates. Builds without the bundled runtime always use it. v257 had the opposite default, with an opt-in `--managed-heap` flag. That flag is still accepted and changes nothing.

Managed modules also export debug hooks, which test hosts use in place of their own allocators:

- `__gc_scribble`: when non-zero, every new block is filled with `0x7f` before its caller zeroes it, as a dirty host heap would be.
- `__gc_allocations`: the number of allocation requests so far, including failed ones. This is the same count a host `__heap_alloc` would see.
- `__gc_fail_allocations`: while non-zero, allocation requests fail as if memory were exhausted, raising `OutOfMemoryError`. A positive value fails that many requests; a negative value fails every request until the host clears it. Injected failures neither search the free lists nor grow memory.

### Allocation

The heap starts after the static prefix: the emergency OutOfMemoryError/StackOverflowError objects and the trace shadow stack. Its first 136 bytes hold the free-list heads.

Each block has an 8-byte header before an 8-byte-aligned payload:

- Word 0 holds the capacity, with flags in its low bits: used, marked, scanned.
- Word 1 holds the host retain count of a used block, or the next block of a free list.

Payloads of up to 256 bytes have exact free lists, one per 8-byte size. On an exact-list miss, the smallest larger small free block is split. Coalescing produces sizes that no request asks for exactly, such as 40-byte runs left by pairs of freed 16-byte objects. Larger free blocks share a first-fit list and are split when the remainder can hold a block.

When no free block fits, the bump pointer advances and memory grows with `memory.grow`. If memory cannot grow, allocation returns 0, which the existing allocation sites raise as `OutOfMemoryError`. Allocation never collects. Callers zero what they allocate, so reused blocks need not be cleared.

### Collection

Collection is precise and non-moving. Every heap block is a tagged object or array:

- Objects are traced through every reference field slot, including inherited fields that a subclass field hides, using a `br_table` over class ids that covers exactly the classes the module can instantiate (see [the scaling notes](bytecode-compile-scaling.md)).
- Reference arrays (tags at or below -100) are traced through their elements.
- Primitive arrays hold no references.

An object tag outside that set traps instead of being traced incompletely.

Roots are:

- blocks with a non-zero retain count
- reference static fields
- the cached String and Class mirror globals
- class-initialization failure globals
- the fields of the static emergency objects

These are read from the final, pruned module text, so pruned globals cost nothing.

Marking uses an explicit stack placed above the heap, growing memory if needed. If memory cannot grow, collection still completes by rescanning the heap for marked blocks that have not been scanned. This happens in the collection that follows an `OutOfMemoryError`. A push dropped below the scan position rewinds the scan to that block, so a long graph linked toward lower addresses needs no pass per node. `__gc_mark_overflows` reports how many pushes the last collection dropped. The sweep rebuilds every free list, coalesces adjacent free blocks and returns a trailing free run to the bump pointer. Pages are never returned to the host.

The host contract is the one above:

- Collect only when no Wasm frame is active.
- Retain pointers that must survive a collection; a returned pointer is borrowed until the next one.
- Release each retain exactly once.
- Pass in only references the module created.

Identity hashes are addresses. They are stable while the object is live, and a dead object's address may be reused.

### Evidence and limits

`BytecodeManagedHeapTest` compares results with the JVM, using a host with no allocator and an unbounded memory:

- A single call needs more than 20 pages. Across 24 rounds of about 4 MB of garbage each, memory pages and live bytes stay unchanged after warm-up.
- More than 2 MB of live static data survives collection, and is reclaimed once released.
- Cyclic graphs are reclaimed, and repeated cycles keep live bytes stable.
- A 50,000-node chain survives with the explicit mark stack.
- A retained host pointer survives collections, is reclaimed after release, and repeated retain/release returns to the same live bytes.
- A parent reference field hidden by a subclass instance or static field is the only reference to its value, and the value survives collections followed by reuse of same-sized blocks.

On a 64-page memory:

- Exhaustion raises a Java `OutOfMemoryError` only near the maximum.
- A collection with the live set still at the maximum overflows the mark stack (`__gc_mark_overflows` > 0) and keeps every block.
- After release and collection, allocation succeeds again.
- Filling memory with 16-byte nodes, freeing two of every three and collecting lets 24-byte objects reuse at least 90% of the coalesced runs without growing memory. Without the split step, one allocation succeeds.

The default heap (v258) is checked against a host allocator:

- With the same calls, `__gc_allocations` grows exactly as a host `__heap_alloc` call count does.
- A failure count of 1 raises one Java `OutOfMemoryError` and then clears. A negative count fails every request without growing memory, until the host resets it.

The executable bytecode tests now use the managed heap by default, with scribbling enabled and no memory maximum. A BigInteger bitwise fixture that fit the former 20-page limit under a bump allocator (1.01 MB over 48,072 requests) needs 1.55 MB, or 24 pages, uncollected. The difference comes from block headers and 8-byte rounding.

Limits:

- Each block costs an 8-byte header plus rounding to 8 bytes, about 11 bytes per request in that fixture.
- There are no stack maps, so the collector never runs during a call; one oversized call still needs memory for everything it allocates.
- Free-list search for large blocks is first-fit, and nothing is compacted.
- There are no weak or soft references, finalization, or concurrent collection.
