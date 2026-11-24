# Bytecode timed lock and queue operations

Revision `timed-lock-queue-v317` links the `TimeUnit` members `Lock.tryLock(long, TimeUnit)`,
`ReentrantLock.tryLock(long, TimeUnit)`, `ReentrantReadWriteLock.ReadLock/WriteLock.tryLock(long, TimeUnit)`,
`BlockingQueue`/`LinkedBlockingQueue.offer(E, long, TimeUnit)` and `poll(long, TimeUnit)` in the bundled
exception runtime under the single-threaded contract of `bytecode-reentrant-lock.md`,
`bytecode-reentrant-read-write-lock.md` and `bytecode-linked-blocking-queue.md`.

Contract:

- Timed calls never wait: with no other thread nothing can change the lock or queue state while they would
  wait, so each returns at once the result the JDK would return after its timeout. No clock is read, no
  sleeping, no looping on the timeout.
- Every timed member first evaluates `unit.toNanos(timeout)` and ignores the result, so a `null` unit throws
  `NullPointerException` before anything else happens and the state stays unchanged. For the timed `offer`
  the null-element check (`e == null` -> `NullPointerException`) comes first, then the unit check.
- `ReentrantLock.tryLock` returns `true` exactly as the untimed `tryLock` (same `Integer.MAX_VALUE` guard),
  for every timeout, fair or not, held or not. `ReadLock.tryLock` returns `true` with the same `65535` guard,
  also while the write lock is held (downgrade). `WriteLock.tryLock` returns `false` without changing anything
  when a reader holds the read lock without the write lock (upgrade), otherwise `true` with the `65535` guard.
- Timed `offer` returns `false` when the queue is full; timed `poll` returns `null` when empty and otherwise
  removes and returns the head. Timed members never call their untimed siblings or any other overridable
  method on themselves.

Known differences: no clock is read and no time passes; the JDK would wait for the timeout in the failing
cases (write-lock upgrade, full queue, empty queue). Conditions (`newCondition`, `Condition.await(long,
TimeUnit)`), queue inspection (`getQueueLength`, `hasQueuedThreads`) and serialization
(`LinkedBlockingQueue.writeObject`) are still not linked.

Evidence: `BytecodeTimedLockQueueTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
orders, two fresh instances). `newCondition`, `getQueueLength`, `hasQueuedThreads` and
`LinkedBlockingQueue.writeObject` are rejected before WAT generation.
