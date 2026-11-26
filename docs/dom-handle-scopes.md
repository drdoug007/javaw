# Scoped DOM handles and retained wrappers

Status: implemented in `javaw-runtime.js` for the source compiler. This is the second prerequisite of the [web components track](web-components.md#prerequisites). Milestone M1 will create a scope from each custom element's constructor. No Java API creates scopes yet; the host does it through the runtime methods below.

## Why

The global handle table (`handleMap`/`reverseMap`) holds every element it has handed to Wasm strongly and never releases it. So no element that Java has seen can be garbage collected, and nothing attached to it can be finalized: listeners, a component's Java object, the subtree. Scoped handles give a component's host element and its shadow tree handles that the JS collector can reclaim.

## Runtime API

| Method | Meaning |
| --- | --- |
| `runtime.createHandleScope(host, { peer, root })` | Creates a scope owned by `host`. `peer` is an optional Java object pointer that the scope retains, such as the component object. `root` is an optional shadow root. Throws if `host` already has a live scope. |
| `runtime.setScopeRoot(scope, root)` | Sets or replaces the shadow root that belongs to the scope. |
| `runtime.releaseHandleScope(scope)` | Releases the scope explicitly (see [Release](#release)). |
| `runtime.getHandleStats()` | `{ global, scoped, scopes: { created, released, finalized } }`, for tests and diagnostics. |

## Classification and encoding

`getOrCreateHandle(node)` returns a **scoped handle** when the node is the host of a live scope, or when `node.getRootNode()` is that scope's root. Any other value gets a global handle, as before. A node that already has a global handle and later moves into a scope gets a scoped handle from then on; its global entry keeps the existing lifetime.

- **Encoding.** Scoped handles are negative `i32` values from a monotonic counter, so they never collide with global handles, which are positive, or with `null`, which is 0. A node keeps the same scoped handle for as long as its scope lives.
- **Lookup.** `getElement(handle)` resolves a negative handle through `scopedHandles: Map<handle, WeakRef<scope>>` and then through the scope's own `nodes` map. Every existing DOM import and `@JS` parameter therefore accepts both kinds. The `@JS` fallback that decodes a string pointer runs only for positive handles.
- **No strong global references.** The scope object lives on the host, as a non-enumerable `__javawScope` property, and the scope holds the host and its nodes. The only global references are `WeakRef`s and a `FinalizationRegistry` registration. The host, its scope and its subtree are therefore an ordinary JS object graph.
- **Use after release.** Resolving a handle whose scope was released or collected calls the exported `__javaw_elementReleased`. That export is added to every program that imports from `one.dastec.javaw.dom`, and it throws `java.lang.IllegalStateException("element released")` into the calling Java frame. Without the export, the runtime throws a JS `Error` instead.

## Listeners inside a scope

An instance-bound listener ([DOM event listeners](dom-event-listeners.md)) registered on a node with a scoped handle belongs to its scope:

- Ownership follows the node, whichever handle (scoped or an older global one) was used to add or remove the listener.
- Its JS function, and the runtime's record of which scope owns it, refer to the scope through a `WeakRef`. A node that outlives its component, such as a portal left in the document, therefore does not keep the scope alive.
- Its JS function checks `scope.released` and does nothing once the scope is released. A node that JS still holds after release therefore dispatches to a listener that does nothing.
- The scope releases the listener's closure on release or finalization, unless `removeEventListener` released it first. Scoped listeners are not registered with the per-function finalizer, so a closure is never released twice.

## Release

`releaseHandleScope`:

1. marks the scope released;
2. removes its listeners from their nodes;
3. deletes its handles from `scopedHandles`;
4. releases its listener closures and its peer;
5. clears its node maps.

If the host becomes unreachable without an explicit release, the `FinalizationRegistry` callback does steps 3 and 4. The finalizer's held value contains only integers and sets of integers, never the host. Finalization is **best-effort**: callbacks run in a later task, and JS engines are not required to run them at all. A scope that is never finalized is a leak, not a correctness problem.

## Exported-class wrappers

A JS wrapper of an `@JS.Export` class, whether constructed in JS or returned from Java, now calls `runtime.retain(__ptr)` once and registers itself with a `FinalizationRegistry` that releases that pointer. The Java object therefore survives collections for as long as the wrapper is reachable, where before it could be freed under the wrapper. Wrappers are no longer entered in the global handle table, which would have kept them alive forever. `runtime.wrapperStats` counts retained and finalized wrappers.

## Validation

`DomHandleScopeTest` runs in Node with `--expose-gc` and a fake DOM that has `getRootNode` and shadow roots:

- **Scoped handles.**
  - The host and the shadow-tree child get negative handles, which are stable for a given node.
  - DOM imports resolve them.
  - `Event.target()` inside the scope is scoped.
  - The global table stays level.
  - Across `__gc_collect`, the peer and the listener closures stay retained.
- **Explicit release.**
  - A stashed handle throws `IllegalStateException`, and Java catches it.
  - Listeners are detached.
  - Live bytes return to the baseline.
- **Leak test.**
  - The test runs two batches of 1,000 scopes. Each scope has a peer, two listeners and one dispatch.
  - After each batch it repeats `gc()` and two macrotask turns until the finalized-scope counter equals the number created. That count is then asserted.
  - After `__gc_collect`, these stay level: live bytes (at the baseline), heap high-water bytes after warm-up, scoped handles (zero) and global handles.
- **Wrappers.** A wrapper survives `__gc_collect` plus allocation churn with its object intact, is not in the global table, and is finalized after it is dropped. Live bytes then return to the baseline.

- **Ownership.** A listener belongs to the scope of its node. Re-adding it after the scope was released and replaced attaches it again. Adding through a scoped handle and removing through the node's older global handle detaches it without a double release. A nested host whose own scope was released gets a handle from its parent's scope, not a global one.

Mutation checks: each of these fails the tests:
- a scope that does not release its peer;
- a wrapper that does not retain;
- leaving stale listener entries after release;
- classifying a released nested host as global.

Releases from finalizers and scope teardown are guarded individually, so one failure does not stop the rest.

## Limitations

- **Global-table nodes keep their lifetime.** Nodes obtained outside any scope (light DOM, `DOM.getBody()`, elements created by `DOM.createElement` before they are placed in a scope) stay in the global table forever, as before.
- **Handed-out nodes stay for the scope's lifetime.** A live scope keeps every node it has handed to Wasm, and that node's handle, until the scope is released or finalized. A long-lived component that hands many transient nodes to Java therefore grows its scope. Template rendering does not hand its nodes to Java and does not grow the scope; `ref(...)`, `createElement`, `querySelector` and `Event.target()` do (see [Web components](web-components.md#implementation-status)).
- **Unknown negative handles.** An unknown negative handle is reported as "element released".
- **Fully qualified DOM names.** A program that uses `one.dastec.javaw.dom` types only through fully qualified names, without an import, gets no `__javaw_elementReleased` export. It sees a JS `Error` instead of the Java exception.
- **Shadow roots only.** Classification follows `getRootNode()`, which requires a shadow root. A light-DOM child of a host is not part of the host's scope.
- **Wrappers and the collector.** Exported wrappers retain their objects, but other host code that keeps raw pointers across tasks must still retain them itself (see [memory management](memory-management.md#host-ownership)). The web components collector policy stays off until M2.
