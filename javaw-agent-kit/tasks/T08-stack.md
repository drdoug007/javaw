# T08 Stack

```kit
id: T08
slug: stack
title: link java.util.Stack
tests: BytecodeStackTest
depends: T07
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/Stack.java
allow: docs/bytecode-stack.md
```

## Goal

Add `java.util.Stack<E> extends Vector<E>` to the bundled runtime. Logback, Tomcat's digester, ANTLR and
others use `push`/`pop`/`peek`/`empty` and the inherited Vector members (`add`, `addAll`, `clear`, `clone`,
`elementAt`, `elements`, `get`, `isEmpty`, `iterator`, `lastElement`). T07 already added `Vector` and
`EmptyStackException`; this task adds one small class.

## Members to implement

`public class Stack<E> extends Vector<E>` with exactly:

- `public Stack()`
- `public E push(E item)`
- `public E pop()`
- `public E peek()`
- `public boolean empty()`
- `public int search(Object o)`

Everything else (`add`, `get`, `elementAt`, `elements`, `iterator`, `clone`, `toString`, `equals`, ...) is
inherited from the runtime `Vector` and must not be rewritten.

## Members to leave out

No other constructors (`Stack(int)` does not exist in the JDK) and no `setSize` (it is not in the runtime
Vector either). The test checks both are rejected. Do not edit `Vector.java`.

## JDK behaviour to match

- The top of the stack is the **end** of the vector (`size()-1`). Iteration, `toString` and `get(0)` go from
  bottom to top: after `push("a"); push("b"); push("c")`, `toString()` is `[a, b, c]` and `pop()` returns `c`.
- `push(item)`: `addElement(item)`, then return `item` (also when it is `null`).
- `peek()`: if `size()==0` throw `new EmptyStackException()`; otherwise return `elementAt(size()-1)`.
- `pop()`: `E top = peek();` then `removeElementAt(size()-1)`; return `top`. Empty -> `EmptyStackException`
  (thrown by `peek`, message `null`).
- `empty()`: `size() == 0`.
- `search(o)`: `int i = lastIndexOf(o); return i >= 0 ? size() - i : -1;` so the top element is distance 1,
  duplicates report the topmost copy, `null` works, absent gives -1.
- `clone()` is inherited; `super.clone()` in Vector already returns a `Stack`.

## Steps

1. Create `javaw-bytecode/src/main/resources/exception-runtime/java/util/Stack.java` with the six members.
2. Register it: in `BytecodeExceptionRuntime.java`, on the `List.of(...)` line that contains `"Vector"`,
   insert `"Stack",` right after `"Vector",`.
3. `javaw-agent-kit/bin/kit check T08` until PASS.
4. Write `docs/bytecode-stack.md` (skeleton below).

## Hints

- The whole class is about 15 lines. Call the inherited `addElement`, `elementAt`, `removeElementAt`,
  `lastIndexOf` and `size()`; do not touch `elementData` directly.
- `EmptyStackException` is in `java.util` already (T07); no import is needed.
- `pop` must go through `peek` (or check `size()==0` itself) so an empty stack throws `EmptyStackException`,
  not the `ArrayIndexOutOfBoundsException` from `removeElementAt(-1)`.
- If `kit check` says `java/util/Stack` is not linked, the registration edit is missing or misspelled.
- Do not override `clone`, `toString`, `equals` or `iterator`; the Vector versions already give the JDK results
  (the test checks that `clone()` returns a `Stack` and that iteration runs bottom to top).
- If `cloneIsStack` fails (a `ClassCastException`, or `getClass()==Stack.class` is false), T07's `Vector.clone`
  does not use `super.clone()`. Do not add `clone` to `Stack` and do not edit `Vector.java`: stop and report it.
- Do not add `synchronized` (the JDK has it on `pop`, `peek`, `search`); guard rejects the keyword.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
stackOperations, search, inheritedVectorMembers: if FIX THE RUNTIME says `link the descriptor class/interface java/util/Stack` (or another link message naming java/util/Stack), Stack is not registered: in BytecodeExceptionRuntime.java (do not edit that file by hand: run `javaw-agent-kit/bin/kit register <path under exception-runtime>` for each new file, which makes exactly this edit), on the List.of(...) line that contains "Vector", insert "Stack", right after "Vector",. If it says `link a matching instance method in the declaring hierarchy` or `link a matching special declaration`, one of public Stack(), push, pop, peek, empty, search is missing, not public, or misspelled.
emptyFailures, drainedFailures, failureIsCatchable: an empty stack must throw EmptyStackException ("EmptyStackException: null"), not ArrayIndexOutOfBoundsException. peek starts with `if (size() == 0) throw new EmptyStackException();` and pop starts with `E top = peek();` before calling removeElementAt(size() - 1).
popOrder, peekDoesNotRemove, pushAfterPop, listView, pushReturnsItem: the top of the stack is the END of the vector. push is `addElement(item); return item;` (return the argument, even null), peek returns `elementAt(size() - 1)` without removing, pop removes with `removeElementAt(size() - 1)`. Never insert at or remove index 0.
distances, duplicatesUseTopmost, nulls, emptySearch, usesEquals: search is 1-based from the top and uses the topmost match: `int i = lastIndexOf(o); return i >= 0 ? size() - i : -1;` (after pushing a,b,c: c=1, b=2, a=3, absent=-1). Vector.lastIndexOf already handles null and equals; do not use == or indexOf.
manyPushes, iteratesBottomToTop, failFast, positionalFailures, equalsVector, clearAndReuse: Stack must have no fields and no overrides beyond the six members: delete any own array, size counter, iterator, toString, equals or clear and call the inherited addElement, elementAt, removeElementAt, lastIndexOf and size(), so capacity (10, 20, 40), modCount and the exception messages come from Vector. If Stack already has only the six members and this still fails, the bug is in T07's Vector.java, which you must not edit: run kit block T08 with the failing fixture name.
cloneIsStack: do not add clone to Stack and do not edit Vector.java. If this fails with ClassCastException or getClass()==Stack.class is false, T07's Vector.clone does not use super.clone(): stop and run kit block T08 "Vector.clone does not use super.clone()".
```

## Docs

Create `docs/bytecode-stack.md` with this skeleton (fill in the brackets):

    # Bytecode Stack

    Revision `stack-vN` links `java.util.Stack` on top of the monitor-free `Vector` in the bundled exception
    runtime.

    Contract:

    - <push/pop/peek/empty/search behaviour, EmptyStackException, inherited Vector members>

    Evidence: `BytecodeStackTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two fresh
    instances). `Stack.setSize` and `Stack(int)` are rejected before WAT generation.
