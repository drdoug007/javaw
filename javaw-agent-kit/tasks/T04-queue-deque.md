# T04 Queue and Deque interfaces; LinkedList implements Deque

```kit
id: T04
slug: queue-deque
title: link java.util.Queue and java.util.Deque; LinkedList implements Deque
tests: BytecodeQueueDequeTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/Queue.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/Deque.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/LinkedList.java
allow: javaw-bytecode/src/test/java/one/dastec/javaw/bytecode/BytecodeLinkedListTest.java
allow: docs/bytecode-linked-list.md
allow: docs/bytecode-queue-deque.md
```

## Goal

Add the interfaces `java.util.Queue` and `java.util.Deque` (JDK 17 shape) to the bytecode runtime and make
`LinkedList` implement `Deque<E>`. The pinned libraries call through these interfaces (about 110 `Queue` and 50
`Deque` audit rows: `offer`, `poll`, `peek`, `push`, `pop`, `addFirst`, `pollLast`, `isEmpty`, ...). Later tasks
(T05 ArrayDeque, T06 PriorityQueue) build on these interfaces.

**Exception to AGENT.md's "Never edit tests" rule:** this task changes runtime behaviour that the existing test
`BytecodeLinkedListTest` asserts against, so that one file is in `allow:` and you MUST make exactly the edit in
step 1. Do not touch any other test, and never touch `BytecodeQueueDequeTest`. This is not a reason to `kit block`.

## Members to implement

`Queue.java` — `public interface Queue<E> extends Collection<E>`, all abstract (no default methods):

    boolean add(E e); boolean offer(E e); E remove(); E poll(); E element(); E peek();

`Deque.java` — `public interface Deque<E> extends Queue<E>`, all abstract (JDK 17 has no defaults here):

    void addFirst(E e); void addLast(E e); boolean offerFirst(E e); boolean offerLast(E e);
    E removeFirst(); E removeLast(); E pollFirst(); E pollLast();
    E getFirst(); E getLast(); E peekFirst(); E peekLast();
    boolean removeFirstOccurrence(Object o); boolean removeLastOccurrence(Object o);
    boolean add(E e); boolean offer(E e); E remove(); E poll(); E element(); E peek();
    boolean addAll(Collection<? extends E> c);
    void push(E e); E pop();
    boolean remove(Object o); boolean contains(Object o); int size();
    Iterator<E> iterator(); Iterator<E> descendingIterator();

`LinkedList.java` — change only the class declaration and its comment:

    public class LinkedList<E> extends AbstractSequentialList<E> implements List<E>,Deque<E>,Cloneable,java.io.Serializable{

LinkedList already declares every Deque member (look at it: `addFirst` ... `descendingIterator`). Do not rewrite them.

## Members to leave out

- `reversed()` on `Deque` and on `LinkedList` (JDK 21 `SequencedCollection`). The test checks both are rejected.
  Do not add `SequencedCollection`.

## JDK behaviour to match (LinkedList already does this)

- Empty list: `getFirst`, `getLast`, `removeFirst`, `removeLast`, `pop`, `element`, `remove()` throw
  `NoSuchElementException` with a `null` message; `peek*`/`poll*`/`peek`/`poll` return `null`.
- `offer*` return `true`; `push` = `addFirst`; `pop` = `removeFirst`; `add`/`offer` append at the tail.
- LinkedList allows `null` elements; `removeFirstOccurrence(null)` removes the first null.

## Steps

1. Do this first. Edit the existing test
   `javaw-bytecode/src/test/java/one/dastec/javaw/bytecode/BytecodeLinkedListTest.java` (allowed exception, see
   Goal). Its method `rejectsUnusedUnlinkedDequeMembers` asserts that Deque/Queue calls are rejected; after this
   task they are accepted. Delete exactly these two lines and keep the `reversed` line:

        rejectsUnused("java/util/Deque","addFirst","(Ljava/lang/Object;)V",INVOKEINTERFACE,true);
        rejectsUnused("java/util/Queue","offer","(Ljava/lang/Object;)Z",INVOKEINTERFACE,true);

   Also rename the method to `rejectsUnusedUnlinkedSequencedMembers`. Change nothing else in that file.
   Until this edit is made, `kit regress` and `kit finish` fail in `BytecodeLinkedListTest`
   (`rejectsUnusedUnlinkedDequeMembers`: "Expected java.lang.IllegalArgumentException to be thrown, but nothing
   was thrown"). Only this edit fixes that; never "fix" it by changing LinkedList or Deque.
2. Create `javaw-bytecode/src/main/resources/exception-runtime/java/util/Queue.java` (package `java.util`).
3. Create `.../java/util/Deque.java` the same way.
4. Register both. In `javaw-bytecode/src/main/java/one/dastec/javaw/bytecode/BytecodeExceptionRuntime.java`, in the
   `List.of("Iterator","ListIterator","Collection","List",...)` line, insert `"Queue","Deque",` right after
   `"List",` (Queue must stay before Deque; both after Collection).
5. Edit the LinkedList class declaration as shown above and update its one-line comment (it currently says the
   Deque/Queue interfaces are not linked).
6. `javaw-agent-kit/bin/kit check T04` until PASS.
7. In `docs/bytecode-linked-list.md`, the bullet that starts "`java.util.Queue` and `java.util.Deque` are not
   linked" has three sentences. Replace only the first two with: "LinkedList implements `java.util.Deque` (and so
   `Queue`) since revision `queue-deque`. Newer members such as `reversed()` are still rejected before WAT
   generation." Keep the Spliterators sentence. In the Evidence line change "`Deque`/`Queue`/`reversed()` calls" to
   "`reversed()` calls". Then write `docs/bytecode-queue-deque.md` (skeleton below).

## Hints

- An interface file is just `package java.util;` plus the declaration; copy the style of `java/util/List.java`.
- Re-declaring Collection methods (`add`, `remove(Object)`, `contains`, `size`, `iterator`, `addAll`) in Deque is
  what JDK 17 does. javac then emits `invokeinterface java/util/Deque.size` for `deque.size()`, so the runtime
  Deque must declare them too.
- `isEmpty()` and `clear()` are NOT declared in Deque; they come from Collection. Do not add them.
- No default methods, no `reversed()`, no `SequencedCollection`: any of these makes the rejection test fail
  (`Admission accepted a call to java/util/Deque.reversed...`).
- If every bytecode test breaks with "Cannot build bytecode exception runtime", the javac error follows (usually a
  typo in a generic signature; no imports are needed, everything is in `java.util`).
- Before the change the test fails with `link a complete acyclic type hierarchy: java/util/Queue`: that just means
  Queue is missing or not registered.
- Calibration: the reference passed on the first `kit check`. The whole task is ~20 lines; do not over-build it.
- `kit regress`/`kit finish` include `BytecodeLinkedListTest`; they fail until the step 1 test edit is done.

## Docs

Create `docs/bytecode-queue-deque.md`:

    # Bytecode Queue and Deque

    Revision `queue-deque-vN` links `java.util.Queue` and `java.util.Deque` (JDK 17 members, no defaults) in the
    bundled exception runtime, and `java.util.LinkedList` now implements `Deque`.

    Contract:

    - Calls through `Queue`/`Deque` dispatch to LinkedList's existing members and to user classes that implement
      the interfaces. Empty-deque failures are `NoSuchElementException`; `peek*`/`poll*` return null.
    - `reversed()` (JDK 21 SequencedCollection) is not linked.

    Evidence: `BytecodeQueueDequeTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
    fresh instances). `Deque.reversed()` and `LinkedList.reversed()` are rejected before WAT generation.
