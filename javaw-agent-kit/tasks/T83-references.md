# T83 java.lang.ref references and ReferenceQueue

```kit
id: T83
slug: references
title: link java.lang.ref.Reference, WeakReference, SoftReference, PhantomReference and ReferenceQueue (no reclamation)
tests: BytecodeReferenceTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/lang/ref/Reference.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/lang/ref/ReferenceQueue.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/lang/ref/WeakReference.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/lang/ref/SoftReference.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/lang/ref/PhantomReference.java
allow: docs/bytecode-references.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-references-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeReferenceTest.java`.

## Goal

Add the `java.lang.ref` reference classes to the bundled runtime under the single-threaded, **no-reclamation**
contract (the same contract as T66 `WeakHashMap`, see `docs/bytecode-weak-hash-map.md`): a referent is held
strongly and is never cleared or enqueued by a collector. That is a legal JVM outcome (a JVM whose collector never
runs, or never finds the referent unreachable, behaves exactly like this). Only the program clears (`clear()`) or
enqueues (`enqueue()`) a reference. `runtime-target/target/audit/member-references.tsv` lists the uses.
`WeakReference.<init>(Ljava/lang/Object;)V` (opcode 183): AspectJ `ThreadLocalAwareRepository.setCurrentRepository`
(28), H2 `DelayedDatabaseCloser.<init>` (3), jaxb-runtime `JAXBContextImpl.getTypeInfoSet` (3), Byte Buddy
`TypeCache$Sort$1.wrap` (4), tomcat-embed-el `ExpressionFactory$CacheKey.<init>` (4), Spring core
`AbstractClassGenerator$ClassLoaderData.<init>` (4), HikariCP `ConcurrentBag.requite`, log4j `Provider.<init>`,
Hibernate `EnhancingClassTransformerImpl.getEnhancer`, Spring Boot `ParentContextCloserApplicationListener`, Spring
beans `DefaultListableBeanFactory.setSerializationId`, istack `Pool$Impl.getQueue`, jaxb `JAXB.getContext`;
`<init>(Ljava/lang/Object;Ljava/lang/ref/ReferenceQueue;)V`: Spring core `ConcurrentReferenceHashMap$WeakEntryReference`,
Hibernate `ConcurrentReferenceHashMap$WeakKeyReference` (2), Byte Buddy `TypeCache$StorageKey` (2), AspectJ
`World$TypeMap.put` (2), Tomcat `ManagedConcurrentWeakHashMap$Key`, jakarta.persistence `LoaderReference`.
`WeakReference.get()` (182): AspectJ `ThreadLocalAwareRepository.currentRepository` (13), tomcat-embed-el
`CacheKey.equals` (6), H2 `DelayedDatabaseCloser.run` (5), Spring Boot `ContextCloserListener.equals` (4), log4j,
jaxb, Spring core, HikariCP, Hibernate, istack. `WeakReference.clear()`: AspectJ `ReferenceType.addParent` (6).
`SoftReference.<init>` (both forms): H2 `StringUtils.getCache` (4) and `SoftValuesHashMap$SoftValue`, AspectJ
`ClassLoaderRepository.storeClassAsReference` (4) and `SoftHashMap$SpecialValue` (4), Byte Buddy, Spring Boot
`SoftReferenceConfigurationPropertyCache`, Jackson `BufferRecyclers`/`ThreadLocalBufferManager`, Spring core
`SoftEntryReference`, Hibernate `SoftKeyReference` (2), jakarta.persistence; `SoftReference.get()` (H2
`SoftValuesHashMap.get` 5, Byte Buddy 4, AspectJ 3, Jackson, Spring Boot) and `clear()` (Jackson). `Reference.get()`:
Hibernate `ConcurrentReferenceHashMap$HashEntry.dereferenceValue` (2), Byte Buddy `TypeCache.find` (2), AspectJ (2),
Tomcat `WebappClassLoaderBase.checkThreadLocalMapForLeaks` (3), Spring beans. `PhantomReference.<init>` with a
queue: H2 `CloseWatcher.<init>` (2). `ReferenceQueue.<init>()V`: H2 `CloseWatcher.<clinit>` (3), AspectJ
`SoftHashMap.<init>` (5), Hibernate (2), Spring core, Tomcat, Byte Buddy, Jackson, jakarta.persistence;
`ReferenceQueue.poll()`: AspectJ `SoftHashMap.processQueue` (8), H2 `CloseWatcher.pollUnclosed` (3), Spring core
`ReferenceManager.pollForPurge`, Hibernate `Segment.removeStale`, Tomcat `ManagedConcurrentWeakHashMap.maintain`,
Jackson, jakarta.persistence. Under this contract `poll()` returns `null` until the program itself enqueues a
reference, so those caches never purge. `refersTo`, `enqueue`, `isEnqueued` and `ReferenceQueue.remove` are not in
the audit but complete the public JDK 21 surface so the classes behave coherently.

## Members to implement

Five files under `javaw-bytecode/src/main/resources/exception-runtime/java/lang/ref/` (`package java.lang.ref;`):

`Reference.java`: `public abstract class Reference<T>` (extends `Object`, implements nothing; not `sealed`:
there is no `FinalReference`) with

- fields `private T referent;`, `ReferenceQueue<? super T> queue;` (package-private), `Reference next;`
  (package-private, used by the queue's list)
- package-private constructors `Reference(T referent)` (`this(referent, null)`) and
  `Reference(T referent, ReferenceQueue<? super T> queue)` (`queue == null` stores `ReferenceQueue.NULL`)
- `public T get()`, `public final boolean refersTo(T obj)`, `public void clear()`,
  `@Deprecated public boolean isEnqueued()`, `public boolean enqueue()`

`ReferenceQueue.java`: `public class ReferenceQueue<T>` with

- `private static class Null extends ReferenceQueue<Object>` whose `boolean enqueue(Reference<?> r)` returns `false`
  (no other members), and `static final ReferenceQueue<Object> NULL = new Null();`,
  `static final ReferenceQueue<Object> ENQUEUED = new Null();`
- field `private Reference<? extends T> head;`
- `public ReferenceQueue()`
- package-private `boolean enqueue(Reference<? extends T> r)` (called only by `Reference.enqueue`)
- `public Reference<? extends T> poll()`
- `public Reference<? extends T> remove(long timeout) throws IllegalArgumentException, InterruptedException`
- `public Reference<? extends T> remove() throws InterruptedException`

`WeakReference.java`: `public class WeakReference<T> extends Reference<T>` with `public WeakReference(T referent)` and
`public WeakReference(T referent, ReferenceQueue<? super T> q)` (just `super(...)`).

`SoftReference.java`: `public class SoftReference<T> extends Reference<T>` with the same two constructors and
`public T get()` returning `super.get()` (the JDK overrides `get` to update an LRU timestamp; keep the override, drop
the timestamp).

`PhantomReference.java`: `public class PhantomReference<T> extends Reference<T>` with only
`public PhantomReference(T referent, ReferenceQueue<? super T> q)` and `public T get()` returning `null`.

Do **not** override `equals`, `hashCode` or `toString` anywhere (identity, from `Object`). None of the classes is
`Serializable` or `Cloneable`.

## Members to leave out

- `java.lang.ref.Cleaner` (and `Cleaner$Cleanable`): it needs a daemon thread. The test checks that
  `Cleaner.create()` stays unlinked. Do not create `Cleaner.java`.
- `Reference.reachabilityFence(Object)`: not referenced by the pinned libraries; the test checks it stays unlinked.
- `Reference.clone()` (the JDK's `protected` override that throws `CloneNotSupportedException`), `FinalReference`,
  `Finalizer`, the reference-handler thread, `waitForReferenceProcessing`, `ReferenceQueue.forEach`, the queue's
  lock/condition and `queueLength`, `SoftReference`'s `clock`/`timestamp`, native `refersTo0`/`clear0`.
- No `synchronized`, `volatile`, `Thread`, `ReentrantLock` or time calls.
- Do not change `WeakHashMap.java` (keep it independent of `WeakReference`) or any other runtime file.

## JDK behaviour to match

- `get()`: the referent, or `null` after `clear()` or `enqueue()`. `PhantomReference.get()` always returns `null`.
- `refersTo(obj)`: `referent == obj` (identity, never `equals`), reading the **field**, not calling `get()`. So
  `phantom.refersTo(referent)` is `true` (until cleared/enqueued), and `refersTo(null)` is `true` for a cleared
  reference or a `null` referent. It is `final`.
- `clear()`: `referent = null` (does not enqueue; calling it twice is fine).
- `enqueue()`: **first** sets `referent = null` directly (the JDK calls the private `clear0()`, **not** the
  overridable `clear()`; the `Spy` fixture logs `clear`), then `return queue.enqueue(this)`. So it clears the
  referent even when it returns `false` (null queue, already enqueued, already removed). It does not call `get()`
  or `isEnqueued()`.
- `isEnqueued()`: `queue == ReferenceQueue.ENQUEUED`.
- A reference made with no queue or a `null` queue has `queue == NULL`: `enqueue()` returns `false` and
  `isEnqueued()` stays `false`.
- `ReferenceQueue.enqueue(r)`: if `r.queue` is `NULL` or `ENQUEUED` return `false`; else push at the **head**
  (`r.next = (head == null) ? r : head; head = r;`, a self-loop marks the end), set `r.queue = ENQUEUED`, return
  `true`. A reference is enqueued at most once: after it was polled its `queue` is `NULL`, so a second `enqueue()`
  returns `false`.
- `poll()`: `null` when empty; else take the **head** (the queue is **LIFO**: the most recently enqueued reference
  comes out first), set `r.queue = NULL`, `head = (r.next == r) ? null : r.next`, `r.next = r`, return it.
- `remove(timeout)`: `timeout < 0` throws `new IllegalArgumentException("Negative timeout value")`;
  `timeout == 0` is `remove()`; otherwise return `poll()` (the JDK waits up to `timeout` ms for another thread, and
  with no other thread the outcome is the same: the head, or `null` after the timeout; do not sleep).
- `remove()`: returns `poll()` when the queue is not empty. On an empty queue the JDK blocks forever; this runtime has
  no other thread, so throw
  `new IllegalStateException("ReferenceQueue.remove would block forever: the queue is empty and no other thread")`
  (same convention as `LinkedBlockingQueue.take` and `CountDownLatch.await`). The test never reaches that path (the
  JVM would hang). Never throws `InterruptedException`, but declares it.
- References and queues compare by identity; `toString()` is `Object`'s (`java.lang.ref.WeakReference@...`).

## Steps

1. Read the test, then `docs/bytecode-weak-hash-map.md` and, for the blocking convention,
   `java/util/concurrent/CountDownLatch.java` under `javaw-bytecode/src/main/resources/exception-runtime/`. The JDK
   reference is `unzip -p ~/.sdkman/candidates/java/21.0.2-graalce/lib/src.zip java.base/java/lang/ref/Reference.java`
   (and `ReferenceQueue.java`, `WeakReference.java`, `SoftReference.java`, `PhantomReference.java`); ignore the
   native, GC, lock and `FinalReference` parts.
2. Create the five files with the members above.
3. Register each one (do not edit `BytecodeExceptionRuntime.java` by hand), in this order:
   `javaw-agent-kit/bin/kit register java/lang/ref/Reference.java`, then `ReferenceQueue.java`,
   `WeakReference.java`, `SoftReference.java`, `PhantomReference.java` (same command, same `java/lang/ref/` prefix).
4. `javaw-agent-kit/bin/kit check T83` until PASS.
5. Write `docs/bytecode-references.md` (below), the report, then `javaw-agent-kit/bin/kit finish T83`.

## Hints

- No imports are needed (all five classes are in `java.lang.ref`; `IllegalStateException` and
  `IllegalArgumentException` are in `java.lang`).
- `Null` overrides the package-private `enqueue`; its parameter type `Reference<?>` erases to the same descriptor as
  `ReferenceQueue.enqueue(Reference<? extends T>)`, so it is an override. `poll()` needs an unchecked assignment for
  `r.next` (the raw `Reference next` field): add `@SuppressWarnings("unchecked")`.
- `NULL` and `ENQUEUED` are distinct `Null` instances; compare with `==`.
- Calibration pitfalls (each one is caught by the test): a FIFO queue (`queueing` expects the last enqueued first);
  `enqueue()` calling `clear()` (the `Spy` log shows `clear`); `enqueue()` that does not clear on a `false` result
  (null-queue reference keeps its referent); `refersTo` implemented as `get() == obj` (phantom references would
  answer `false`, and the `Spy` log shows `get`); setting `queue = null` after `poll()` instead of `NULL` (a second
  `enqueue()` then throws `NullPointerException`); `remove(-1)` with a different message.
- The finished T66 test (`BytecodeWeakHashMapTest.leavesOutReferenceQueueMachinery`) checked that
  `WeakReference.get()` and `ReferenceQueue.poll()` stay unlinked; those checks are removed when this task is queued.
  If `kit finish` reports one of them failing, stop and `kit block` (do not edit tests).
- The Wasm runner stops at the first failing fixture of each `@Test` method; after a fix another fixture may fail next.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
basics, queueing, phantoms, subclassCalls, identity: if FIX THE RUNTIME says `link the symbolic class/interface java/lang/ref/...`, `link a complete acyclic type hierarchy` or names one of its methods, a class is missing or not registered: run `javaw-agent-kit/bin/kit register java/lang/ref/<Name>.java` for each of the five files (never edit BytecodeExceptionRuntime.java by hand) and add every member listed under Members to implement. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
basics: get returns the referent until clear or enqueue; refersTo compares the field with ==; enqueue on a reference without a queue returns false but still clears the referent; none of the classes is Serializable or Cloneable.
queueing: the queue is LIFO (push and poll at head, self-loop end); poll sets r.queue = NULL so a second enqueue returns false; remove(0) is remove(), remove(t > 0) is poll(), remove(negative) throws IllegalArgumentException("Negative timeout value").
phantoms: PhantomReference.get always returns null, but refersTo reads the referent field (true for the referent until clear or enqueue); a null queue is allowed.
subclassCalls: enqueue clears the field directly (never calls the overridable clear) and does not call get or isEnqueued; refersTo does not call get.
identity: do not override equals, hashCode or toString in Reference or ReferenceQueue.
leavesOutCleanerAndReachabilityFence: this fails when java.lang.ref.Cleaner or Reference.reachabilityFence became linked. Do not create Cleaner.java or add reachabilityFence.
```

## Docs

Create `docs/bytecode-references.md` with this skeleton (fill in the brackets):

    # Bytecode references

    Revision `references-vN` links `java.lang.ref.Reference`, `WeakReference`, `SoftReference`,
    `PhantomReference` and `ReferenceQueue` in the bundled exception runtime under the single-threaded,
    no-reclamation contract.

    Contract:

    - <get/refersTo/clear; enqueue clears then enqueues once; isEnqueued; LIFO queue; poll, remove(timeout),
      remove() on an empty queue throws IllegalStateException; null queue; PhantomReference.get is null>

    Known differences: referents are held strongly and never cleared or enqueued by a collector (a legal JVM
    outcome when no collector clears them; the runtime has no GC-observable references). `ReferenceQueue.poll()`
    returns only references the program enqueued itself, so reference-based caches never purge. `remove()` on an
    empty queue throws instead of blocking; `remove(timeout)` does not wait. No `Cleaner`, `reachabilityFence`,
    `FinalReference` or finalization.

    Evidence: `BytecodeReferenceTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
    fresh instances). `java.lang.ref.Cleaner.create()` and `Reference.reachabilityFence(Object)` are rejected before
    WAT generation.
