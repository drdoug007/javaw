# Bytecode ThreadLocal

Revision `thread-local-v335` links `java.lang.ThreadLocal` (with `ThreadLocal$SuppliedThreadLocal`) and
`java.lang.InheritableThreadLocal` in the bundled exception runtime under the single-threaded contract.

Contract:

- One value slot per instance: a `T value` field plus a `present` flag, so `set(null)` stores `null` (a later
  `get()` returns `null` without calling `initialValue()`) while `remove()` clears the slot and makes the next
  `get()` call `initialValue()` again. Each `ThreadLocal` instance has its own slot; instances are independent.
- `initialValue()` is called lazily by `get()` only when nothing is stored, and again after `remove()`, never by
  `set`. The JDK's private `setInitialValue()` is matched exactly: it calls the virtual `initialValue()`, stores
  the result directly in the slot, and returns it — it does **not** call `this.set(v)`, so subclasses overriding
  `set` do not see a call. If `initialValue()` throws, nothing is stored, the exception propagates unchanged, and
  the next `get()` retries.
- `set(v)` stores `v` (including `null`) and marks the slot present; it calls none of the overridable methods.
  `remove()` sets `value = null` and `present = false`; it is a no-op when nothing is stored, is safe to call
  repeatedly, and calls none of the overridable methods.
- `withInitial(Supplier)` returns a `ThreadLocal$SuppliedThreadLocal` (a static final nested class); its
  constructor does `Objects.requireNonNull(supplier)`, so a `null` supplier throws `NullPointerException` at the
  call site, and its overridden `initialValue()` returns `supplier.get()` (called lazily on the first `get()`,
  once per `remove()`; a supplier returning `null` is not called again).
- `InheritableThreadLocal` inherits `get`/`set`/`remove`/`initialValue` unchanged; `childValue(p)` returns `p`
  (same reference, `null` for `null`) for subclasses such as logback's `CopyOnInheritThreadLocal`.

Known differences: there is only one thread, so each ThreadLocal is a single field pair instead of an entry in a
per-thread ThreadLocalMap; values are not weakly referenced and are not discarded with a thread.
`InheritableThreadLocal.childValue` is kept for subclasses but never called, because no child thread is ever
created. `java.lang.Thread`, `ThreadLocalMap` and the map/hash internals are not linked.

Evidence: `BytecodeThreadLocalTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
fresh instances). `Thread.currentThread()`, `ThreadLocal.nextHashCode()`, `ThreadLocal.getMap(Thread)` and
`InheritableThreadLocal.createMap(Thread, Object)` are rejected before WAT generation.
