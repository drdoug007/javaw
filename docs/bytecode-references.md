# Bytecode references

Revision `references-v346` links `java.lang.ref.Reference`, `WeakReference`, `SoftReference`,
`PhantomReference` and `ReferenceQueue` in the bundled exception runtime under the single-threaded,
no-reclamation contract.

Contract:

- `get()` returns the referent, or `null` after `clear()` or `enqueue()`. `refersTo(obj)` is `final` and
  reads the `referent` field with `==` (identity, never `get()`), so it is `true` for a cleared reference
  or a `null` referent (`refersTo(null)`) and stays `true` on a `PhantomReference` until cleared or
  enqueued. `clear()` only nulls the field (never enqueues) and is idempotent.
- `enqueue()` first sets the `referent` field to `null` directly (the JDK calls the private `clear0()`, not
  the overridable `clear()`, and never calls `get()` or `isEnqueued()`), then returns
  `queue.enqueue(this)` — it clears the referent even when it returns `false` (no queue, already enqueued,
  already polled). A reference is enqueued at most once: `poll()` puts its `queue` back to `NULL`, so a
  second `enqueue()` returns `false` without throwing.
- `isEnqueued()` (deprecated) is `queue == ENQUEUED`. A reference made with no queue or a `null` queue holds
  the `NULL` sentinel: `enqueue()` returns `false` and `isEnqueued()` stays `false`.
- A queue is a LIFO list pushed and polled at the head (a self-loop marks the end). `enqueue(r)` returns
  `false` when `r.queue` is `NULL` or `ENQUEUED`; otherwise it links `r` before `head`, sets
  `r.queue = ENQUEUED` and returns `true`.
- `poll()` returns the most recently enqueued reference, or `null` when empty. `remove(timeout)` throws
  `IllegalArgumentException("Negative timeout value")` for a negative timeout, `remove(0)` is `remove()`,
  and a positive timeout is `poll()` (it does not wait). `remove()` returns `poll()` when the queue is not
  empty; on an empty queue it throws `IllegalStateException`
  ("ReferenceQueue.remove would block forever: the queue is empty and no other thread") instead of
  blocking. It never throws `InterruptedException` but declares it.
- `PhantomReference.get()` always returns `null` while `refersTo` still reads the referent field. References
  and queues compare by `Object` identity and `toString()` is `Object`'s
  (`java.lang.ref.WeakReference@...`).

Known differences: referents are held strongly and never cleared or enqueued by a collector (a legal JVM
outcome when no collector clears them; the runtime has no GC-observable references). `ReferenceQueue.poll()`
returns only references the program enqueued itself, so reference-based caches never purge. `remove()` on an
empty queue throws instead of blocking; `remove(timeout)` does not wait. No `Cleaner`, `reachabilityFence`,
`FinalReference` or finalization.

Evidence: `BytecodeReferenceTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
fresh instances). `java.lang.ref.Cleaner.create()` and `Reference.reachabilityFence(Object)` are rejected before
WAT generation.
